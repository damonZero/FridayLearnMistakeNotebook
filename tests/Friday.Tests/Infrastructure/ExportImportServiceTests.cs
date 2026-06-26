using System.Text.Json;
using Friday.Domain.Entities;
using Friday.Domain.Enums;
using Friday.Infrastructure.Persistence;
using Friday.Infrastructure.Services;
using Microsoft.EntityFrameworkCore;
using Xunit;

namespace Friday.Tests.Infrastructure;

public class ExportImportServiceTests : IDisposable
{
    private readonly AppDbContext _context;
    private readonly ExportImportService _service;

    public ExportImportServiceTests()
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseSqlite("DataSource=:memory:")
            .Options;

        _context = new AppDbContext(options);
        _context.Database.OpenConnection();
        _context.Database.EnsureCreated();

        _service = new ExportImportService(_context);
    }

    [Fact]
    public async Task ExportToJsonAsync_ProducesValidJsonWithVersionField()
    {
        // Arrange
        var subject = new Subject { Name = "数学", Icon = "📐", Color = "#4A90D9" };
        _context.Subjects.Add(subject);
        await _context.SaveChangesAsync();

        // Act
        var json = await _service.ExportToJsonAsync();

        // Assert
        Assert.NotNull(json);
        using var doc = JsonDocument.Parse(json);
        var root = doc.RootElement;

        Assert.True(root.TryGetProperty("version", out var version));
        Assert.Equal(1, version.GetInt32());

        Assert.True(root.TryGetProperty("exportedAt", out _));
        Assert.True(root.TryGetProperty("subjects", out var subjects));
        Assert.Equal(1, subjects.GetArrayLength());
    }

    [Fact]
    public async Task ExportToJsonAsync_ExportsAllEntities()
    {
        // Arrange
        var subject = new Subject { Name = "数学" };
        _context.Subjects.Add(subject);

        var chapter = new Chapter { SubjectId = subject.Id, Name = "第一章" };
        _context.Chapters.Add(chapter);

        var kp = new KnowledgePoint { ChapterId = chapter.Id, Name = "加法" };
        _context.KnowledgePoints.Add(kp);

        var question = new Question
        {
            SubjectId = subject.Id,
            Content = "1+1=?",
            Answer = "2",
            UserAnswer = "3",
            ErrorType = ErrorType.Careless
        };
        _context.Questions.Add(question);
        await _context.SaveChangesAsync();

        // Act
        var json = await _service.ExportToJsonAsync();

        // Assert
        using var doc = JsonDocument.Parse(json);
        var root = doc.RootElement;

        Assert.Equal(1, root.GetProperty("subjects").GetArrayLength());
        Assert.Equal(1, root.GetProperty("chapters").GetArrayLength());
        Assert.Equal(1, root.GetProperty("knowledgePoints").GetArrayLength());
        Assert.Equal(1, root.GetProperty("questions").GetArrayLength());
    }

    [Fact]
    public async Task ImportFromJsonAsync_RejectsMissingVersion()
    {
        // Arrange
        var json = """{"subjects":[],"chapters":[],"knowledgePoints":[],"questions":[]}""";

        // Act & Assert
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.ImportFromJsonAsync(json));
        Assert.Contains("Unsupported export version", ex.Message);
    }

    [Fact]
    public async Task ImportFromJsonAsync_RejectsUnsupportedVersion()
    {
        // Arrange
        var json = """{"version":2,"subjects":[],"chapters":[],"knowledgePoints":[],"questions":[]}""";

        // Act & Assert
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.ImportFromJsonAsync(json));
        Assert.Contains("Unsupported export version", ex.Message);
    }

    [Fact]
    public async Task RoundTrip_ExportThenImport_PreservesEntityCounts()
    {
        // Arrange: populate database
        var subject = new Subject { Name = "数学", Icon = "📐", Color = "#4A90D9" };
        _context.Subjects.Add(subject);

        var chapter1 = new Chapter { SubjectId = subject.Id, Name = "第一章" };
        var chapter2 = new Chapter { SubjectId = subject.Id, Name = "第二章" };
        _context.Chapters.AddRange(chapter1, chapter2);

        var kp = new KnowledgePoint { ChapterId = chapter1.Id, Name = "加法" };
        _context.KnowledgePoints.Add(kp);

        var q1 = new Question { SubjectId = subject.Id, Content = "1+1=?", Answer = "2", ErrorType = ErrorType.Careless };
        var q2 = new Question { SubjectId = subject.Id, Content = "2+2=?", Answer = "4", ErrorType = ErrorType.Conceptual };
        var q3 = new Question { Content = "3+3=?", Answer = "6" };
        _context.Questions.AddRange(q1, q2, q3);
        await _context.SaveChangesAsync();

        // Act: export
        var json = await _service.ExportToJsonAsync();

        // Clear database
        _context.Questions.RemoveRange(_context.Questions);
        _context.KnowledgePoints.RemoveRange(_context.KnowledgePoints);
        _context.Chapters.RemoveRange(_context.Chapters);
        _context.Subjects.RemoveRange(_context.Subjects);
        await _context.SaveChangesAsync();

        // Verify empty
        Assert.Equal(0, await _context.Subjects.CountAsync());
        Assert.Equal(0, await _context.Questions.CountAsync());

        // Act: import
        var result = await _service.ImportFromJsonAsync(json);

        // Assert: counts match
        Assert.Equal(1, result.Subjects);
        Assert.Equal(2, result.Chapters);
        Assert.Equal(1, result.KnowledgePoints);
        Assert.Equal(3, result.Questions);

        Assert.Equal(1, await _context.Subjects.CountAsync());
        Assert.Equal(2, await _context.Chapters.CountAsync());
        Assert.Equal(1, await _context.KnowledgePoints.CountAsync());
        Assert.Equal(3, await _context.Questions.CountAsync());
    }

    [Fact]
    public async Task ImportFromJsonAsync_DoesNotModifyData_WhenVersionInvalid()
    {
        // Arrange: add initial data
        var subject = new Subject { Name = "数学" };
        _context.Subjects.Add(subject);
        await _context.SaveChangesAsync();

        var json = """{"version":99,"subjects":[],"chapters":[],"knowledgePoints":[],"questions":[]}""";

        // Act & Assert
        await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.ImportFromJsonAsync(json));

        // Original data should still exist
        Assert.Equal(1, await _context.Subjects.CountAsync());
        Assert.Equal("数学", (await _context.Subjects.FirstAsync()).Name);
    }

    public void Dispose()
    {
        _context.Database.CloseConnection();
        _context.Dispose();
    }
}
