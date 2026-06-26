using Friday.Application.Interfaces;
using Friday.Application.Services;
using Friday.Domain.Entities;
using FluentAssertions;
using Moq;
using Xunit;

namespace Friday.Tests.Application;

public class SubjectServiceTests
{
    private readonly Mock<ISubjectRepository> _mockRepository;
    private readonly SubjectService _service;

    public SubjectServiceTests()
    {
        _mockRepository = new Mock<ISubjectRepository>();
        _service = new SubjectService(_mockRepository.Object);
    }

    [Fact]
    public async Task CreateAsync_ShouldRejectDuplicateNames()
    {
        _mockRepository.Setup(r => r.ExistsByNameAsync("数学"))
            .ReturnsAsync(true);

        var act = () => _service.CreateAsync("数学", "🔢", "#7ED321");

        await act.Should().ThrowAsync<InvalidOperationException>()
            .WithMessage("*already exists*");
    }

    [Fact]
    public async Task CreateAsync_ShouldCreateNonPresetSubject()
    {
        _mockRepository.Setup(r => r.ExistsByNameAsync("物理"))
            .ReturnsAsync(false);
        _mockRepository.Setup(r => r.GetAllAsync())
            .ReturnsAsync(new List<Subject>
            {
                new() { SortOrder = 1, IsPreset = true },
                new() { SortOrder = 2, IsPreset = true },
                new() { SortOrder = 3, IsPreset = true }
            });
        _mockRepository.Setup(r => r.AddAsync(It.IsAny<Subject>()))
            .ReturnsAsync((Subject s) => s);

        var result = await _service.CreateAsync("物理", "⚡", "#9B59B6");

        result.Name.Should().Be("物理");
        result.IsPreset.Should().BeFalse();
        result.SortOrder.Should().Be(4);
    }

    [Fact]
    public async Task DeleteAsync_ShouldRejectPresetSubjects()
    {
        var presetSubject = new Subject
        {
            Id = Guid.NewGuid(),
            Name = "数学",
            IsPreset = true
        };

        _mockRepository.Setup(r => r.GetByIdAsync(presetSubject.Id))
            .ReturnsAsync(presetSubject);

        var act = () => _service.DeleteAsync(presetSubject.Id);

        await act.Should().ThrowAsync<InvalidOperationException>()
            .WithMessage("*Cannot delete preset*");
    }

    [Fact]
    public async Task DeleteAsync_ShouldDeleteCustomSubject()
    {
        var customSubject = new Subject
        {
            Id = Guid.NewGuid(),
            Name = "物理",
            IsPreset = false
        };

        _mockRepository.Setup(r => r.GetByIdAsync(customSubject.Id))
            .ReturnsAsync(customSubject);

        await _service.DeleteAsync(customSubject.Id);

        _mockRepository.Verify(r => r.DeleteAsync(customSubject.Id), Times.Once);
    }

    [Fact]
    public async Task SeedPresetsAsync_ShouldCreateMissingPresets()
    {
        _mockRepository.Setup(r => r.GetPresetAsync())
            .ReturnsAsync(new List<Subject>
            {
                new() { Name = "语文", IsPreset = true }
            });

        await _service.SeedPresetsAsync();

        _mockRepository.Verify(r => r.AddAsync(
            It.Is<Subject>(s => s.Name == "数学" && s.IsPreset)), Times.Once);
        _mockRepository.Verify(r => r.AddAsync(
            It.Is<Subject>(s => s.Name == "英语" && s.IsPreset)), Times.Once);
    }

    [Fact]
    public async Task SeedPresetsAsync_ShouldBeIdempotent()
    {
        _mockRepository.Setup(r => r.GetPresetAsync())
            .ReturnsAsync(new List<Subject>
            {
                new() { Name = "语文", IsPreset = true },
                new() { Name = "数学", IsPreset = true },
                new() { Name = "英语", IsPreset = true }
            });

        await _service.SeedPresetsAsync();

        _mockRepository.Verify(r => r.AddAsync(It.IsAny<Subject>()), Times.Never);
    }

    [Fact]
    public async Task UpdateAsync_ShouldRejectPresetSubjects()
    {
        var presetSubject = new Subject
        {
            Id = Guid.NewGuid(),
            Name = "数学",
            IsPreset = true
        };

        _mockRepository.Setup(r => r.GetByIdAsync(presetSubject.Id))
            .ReturnsAsync(presetSubject);

        var act = () => _service.UpdateAsync(presetSubject.Id, "新数学", "🔢", "#7ED321");

        await act.Should().ThrowAsync<InvalidOperationException>()
            .WithMessage("*Cannot modify preset*");
    }
}
