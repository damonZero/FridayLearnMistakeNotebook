using Friday.Domain.Entities;
using Friday.Infrastructure.Persistence;
using Friday.Infrastructure.Persistence.Repositories;
using FluentAssertions;
using Microsoft.EntityFrameworkCore;
using Xunit;

namespace Friday.Tests.Infrastructure;

public class SubjectRepositoryTests : IDisposable
{
    private readonly AppDbContext _context;
    private readonly SubjectRepository _repository;

    public SubjectRepositoryTests()
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseSqlite("DataSource=:memory:")
            .Options;

        _context = new AppDbContext(options);
        _context.Database.OpenConnection();
        _context.Database.EnsureCreated();
        _context.ConfigureSqlite();

        _repository = new SubjectRepository(_context);
    }

    [Fact]
    public async Task SeedPresetSubjectsAsync_ShouldCreateThreePresets()
    {
        await SubjectRepository.SeedPresetSubjectsAsync(_context);

        var subjects = await _repository.GetAllAsync();

        subjects.Should().HaveCount(3);
        subjects.Should().AllSatisfy(s => s.IsPreset.Should().BeTrue());
        subjects.Select(s => s.Name).Should().Contain(new[] { "语文", "数学", "英语" });
    }

    [Fact]
    public async Task AddAsync_ShouldPersistSubject()
    {
        await SubjectRepository.SeedPresetSubjectsAsync(_context);

        var newSubject = new Subject
        {
            Name = "物理",
            Icon = "⚡",
            Color = "#9B59B6",
            SortOrder = 4,
            IsPreset = false
        };

        var result = await _repository.AddAsync(newSubject);

        result.Id.Should().NotBeEmpty();
        var all = await _repository.GetAllAsync();
        all.Should().HaveCount(4);
    }

    [Fact]
    public async Task DeleteAsync_ShouldRemoveSubject()
    {
        await SubjectRepository.SeedPresetSubjectsAsync(_context);

        var subjects = await _repository.GetAllAsync();
        var toDelete = subjects.First();

        await _repository.DeleteAsync(toDelete.Id);

        var remaining = await _repository.GetAllAsync();
        remaining.Should().HaveCount(2);
        remaining.Should().NotContain(s => s.Id == toDelete.Id);
    }

    [Fact]
    public async Task ExistsByNameAsync_ShouldReturnTrueForExistingName()
    {
        await SubjectRepository.SeedPresetSubjectsAsync(_context);

        var exists = await _repository.ExistsByNameAsync("数学");
        var notExists = await _repository.ExistsByNameAsync("物理");

        exists.Should().BeTrue();
        notExists.Should().BeFalse();
    }

    [Fact]
    public async Task GetPresetAsync_ShouldReturnOnlyPresets()
    {
        await SubjectRepository.SeedPresetSubjectsAsync(_context);

        var nonPreset = new Subject { Name = "Custom", IsPreset = false };
        await _repository.AddAsync(nonPreset);

        var presets = await _repository.GetPresetAsync();

        presets.Should().HaveCount(3);
        presets.Should().AllSatisfy(s => s.IsPreset.Should().BeTrue());
    }

    public void Dispose()
    {
        _context.Database.CloseConnection();
        _context.Dispose();
    }
}
