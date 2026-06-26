using Friday.Domain.Entities;
using Friday.Infrastructure.Persistence;
using Friday.Infrastructure.Persistence.Repositories;
using FluentAssertions;
using Microsoft.EntityFrameworkCore;
using Xunit;

namespace Friday.Tests.Infrastructure;

public class KnowledgePointRepositoryTests : IDisposable
{
    private readonly AppDbContext _context;
    private readonly ChapterRepository _chapterRepository;
    private readonly KnowledgePointRepository _knowledgePointRepository;

    public KnowledgePointRepositoryTests()
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseSqlite("DataSource=:memory:")
            .Options;

        _context = new AppDbContext(options);
        _context.Database.OpenConnection();
        _context.Database.EnsureCreated();
        _context.ConfigureSqlite();

        _chapterRepository = new ChapterRepository(_context);
        _knowledgePointRepository = new KnowledgePointRepository(_context);
    }

    private async Task<Subject> CreateTestSubjectAsync()
    {
        var subject = new Subject
        {
            Name = "数学",
            Icon = "🔢",
            Color = "#7ED321",
            SortOrder = 1,
            IsPreset = true
        };
        _context.Subjects.Add(subject);
        await _context.SaveChangesAsync();
        return subject;
    }

    private async Task<Chapter> CreateTestChapterAsync(Guid subjectId, string name = "代数", int sortOrder = 1)
    {
        return await _chapterRepository.AddAsync(new Chapter
        {
            SubjectId = subjectId,
            Name = name,
            SortOrder = sortOrder
        });
    }

    [Fact]
    public async Task AddChapterAsync_ShouldPersistChapter()
    {
        var subject = await CreateTestSubjectAsync();

        var chapter = await CreateTestChapterAsync(subject.Id, "几何", 2);

        chapter.Id.Should().NotBeEmpty();
        chapter.Name.Should().Be("几何");
        chapter.SubjectId.Should().Be(subject.Id);

        var allChapters = await _chapterRepository.GetAllAsync();
        allChapters.Should().HaveCount(1);
    }

    [Fact]
    public async Task GetBySubjectAsync_ShouldReturnChaptersOrderedBySortOrder()
    {
        var subject = await CreateTestSubjectAsync();
        await CreateTestChapterAsync(subject.Id, "几何", 2);
        await CreateTestChapterAsync(subject.Id, "代数", 1);
        await CreateTestChapterAsync(subject.Id, "统计", 3);

        var chapters = await _chapterRepository.GetBySubjectAsync(subject.Id);

        chapters.Should().HaveCount(3);
        chapters[0].Name.Should().Be("代数");
        chapters[1].Name.Should().Be("几何");
        chapters[2].Name.Should().Be("统计");
    }

    [Fact]
    public async Task DeleteChapterAsync_ShouldCascadeToKnowledgePoints()
    {
        var subject = await CreateTestSubjectAsync();
        var chapter = await CreateTestChapterAsync(subject.Id);

        await _knowledgePointRepository.AddAsync(new KnowledgePoint
        {
            ChapterId = chapter.Id,
            Name = "一元二次方程",
            Description = "求解一元二次方程",
            SortOrder = 1
        });

        await _knowledgePointRepository.AddAsync(new KnowledgePoint
        {
            ChapterId = chapter.Id,
            Name = "因式分解",
            Description = "多项式因式分解",
            SortOrder = 2
        });

        var kps = await _knowledgePointRepository.GetByChapterAsync(chapter.Id);
        kps.Should().HaveCount(2);

        // Delete chapter - cascade should remove knowledge points
        await _chapterRepository.DeleteAsync(chapter.Id);

        var remainingKps = await _knowledgePointRepository.GetByChapterAsync(chapter.Id);
        remainingKps.Should().BeEmpty();
    }

    [Fact]
    public async Task GetBySubjectAsync_ShouldReturnKnowledgePointsForSubject()
    {
        var subject = await CreateTestSubjectAsync();
        var chapter1 = await CreateTestChapterAsync(subject.Id, "代数", 1);
        var chapter2 = await CreateTestChapterAsync(subject.Id, "几何", 2);

        await _knowledgePointRepository.AddAsync(new KnowledgePoint
        {
            ChapterId = chapter1.Id,
            Name = "方程求解",
            SortOrder = 1
        });
        await _knowledgePointRepository.AddAsync(new KnowledgePoint
        {
            ChapterId = chapter2.Id,
            Name = "三角形",
            SortOrder = 1
        });

        var kps = await _knowledgePointRepository.GetBySubjectAsync(subject.Id);

        kps.Should().HaveCount(2);
        kps[0].Name.Should().Be("方程求解");  // chapter1 first (SortOrder=1)
        kps[1].Name.Should().Be("三角形");    // chapter2 second (SortOrder=2)
    }

    [Fact]
    public async Task GetByChapterAsync_ShouldReturnKnowledgePointsOrderedBySortOrder()
    {
        var subject = await CreateTestSubjectAsync();
        var chapter = await CreateTestChapterAsync(subject.Id);

        await _knowledgePointRepository.AddAsync(new KnowledgePoint
        {
            ChapterId = chapter.Id,
            Name = "KP-3",
            SortOrder = 3
        });
        await _knowledgePointRepository.AddAsync(new KnowledgePoint
        {
            ChapterId = chapter.Id,
            Name = "KP-1",
            SortOrder = 1
        });
        await _knowledgePointRepository.AddAsync(new KnowledgePoint
        {
            ChapterId = chapter.Id,
            Name = "KP-2",
            SortOrder = 2
        });

        var kps = await _knowledgePointRepository.GetByChapterAsync(chapter.Id);

        kps.Should().HaveCount(3);
        kps[0].Name.Should().Be("KP-1");
        kps[1].Name.Should().Be("KP-2");
        kps[2].Name.Should().Be("KP-3");
    }

    public void Dispose()
    {
        _context.Database.CloseConnection();
        _context.Dispose();
    }
}
