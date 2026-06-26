using System.Text;
using System.Text.Json;
using Friday.Application.Interfaces;
using Friday.Domain.Entities;
using Friday.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace Friday.Infrastructure.Services;

public class ExportImportService : IExportImportService
{
    private readonly AppDbContext _context;

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        WriteIndented = true,
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase
    };

    public ExportImportService(AppDbContext context)
    {
        _context = context;
    }

    public async Task<string> ExportToJsonAsync()
    {
        var subjects = await _context.Subjects.AsNoTracking().ToListAsync();
        var chapters = await _context.Chapters.AsNoTracking().ToListAsync();
        var knowledgePoints = await _context.KnowledgePoints.AsNoTracking().ToListAsync();
        var questions = await _context.Questions.AsNoTracking().ToListAsync();

        var export = new
        {
            version = 1,
            exportedAt = DateTime.UtcNow.ToString("O"),
            subjects,
            chapters,
            knowledgePoints,
            questions
        };

        return JsonSerializer.Serialize(export, JsonOptions);
    }

    public async Task<string> ExportToCsvAsync()
    {
        var questions = await _context.Questions
            .AsNoTracking()
            .Include(q => q.Subject)
            .ToListAsync();

        var sb = new StringBuilder();

        // Header row
        sb.AppendLine("Id;SubjectName;Content;Answer;UserAnswer;ErrorType;CreatedAt;ReviewDate;LeitnerBox");

        // Data rows
        foreach (var q in questions)
        {
            sb.AppendLine(string.Join(";",
                q.Id,
                EscapeCsv(q.Subject?.Name ?? ""),
                EscapeCsv(q.Content),
                EscapeCsv(q.Answer),
                EscapeCsv(q.UserAnswer),
                q.ErrorType,
                q.CreatedAt.ToString("O"),
                q.ReviewDate.ToString("O"),
                q.LeitnerBox));
        }

        return sb.ToString();
    }

    public async Task<ExportSummary> ImportFromJsonAsync(string json)
    {
        using var doc = JsonDocument.Parse(json);
        var root = doc.RootElement;

        // Validate version field per D-19
        if (!root.TryGetProperty("version", out var versionElement))
        {
            throw new InvalidOperationException("Unsupported export version");
        }

        var version = versionElement.GetInt32();
        if (version != 1)
        {
            throw new InvalidOperationException("Unsupported export version");
        }

        // Clear existing data
        _context.Questions.RemoveRange(_context.Questions);
        _context.KnowledgePoints.RemoveRange(_context.KnowledgePoints);
        _context.Chapters.RemoveRange(_context.Chapters);
        _context.Subjects.RemoveRange(_context.Subjects);
        await _context.SaveChangesAsync();

        int subjectCount = 0, chapterCount = 0, kpCount = 0, questionCount = 0;

        // Import subjects
        if (root.TryGetProperty("subjects", out var subjectsElement))
        {
            foreach (var item in subjectsElement.EnumerateArray())
            {
                var subject = new Subject
                {
                    Id = Guid.Parse(item.GetProperty("id").GetString()!),
                    Name = item.GetProperty("name").GetString() ?? "",
                    Icon = item.TryGetProperty("icon", out var icon) ? icon.GetString() ?? "" : "",
                    Color = item.TryGetProperty("color", out var color) ? color.GetString() ?? "" : "",
                    SortOrder = item.TryGetProperty("sortOrder", out var so) ? so.GetInt32() : 0,
                    IsPreset = item.TryGetProperty("isPreset", out var ip) && ip.GetBoolean(),
                    CreatedAt = item.TryGetProperty("createdAt", out var ca) ? ca.GetDateTime() : DateTime.UtcNow,
                    UpdatedAt = item.TryGetProperty("updatedAt", out var ua) ? ua.GetDateTime() : DateTime.UtcNow
                };
                _context.Subjects.Add(subject);
                subjectCount++;
            }
        }

        // Import chapters
        if (root.TryGetProperty("chapters", out var chaptersElement))
        {
            foreach (var item in chaptersElement.EnumerateArray())
            {
                var chapter = new Chapter
                {
                    Id = Guid.Parse(item.GetProperty("id").GetString()!),
                    SubjectId = Guid.Parse(item.GetProperty("subjectId").GetString()!),
                    Name = item.GetProperty("name").GetString() ?? "",
                    SortOrder = item.TryGetProperty("sortOrder", out var so) ? so.GetInt32() : 0,
                    CreatedAt = item.TryGetProperty("createdAt", out var ca) ? ca.GetDateTime() : DateTime.UtcNow,
                    UpdatedAt = item.TryGetProperty("updatedAt", out var ua) ? ua.GetDateTime() : DateTime.UtcNow
                };
                _context.Chapters.Add(chapter);
                chapterCount++;
            }
        }

        // Import knowledge points
        if (root.TryGetProperty("knowledgePoints", out var kpsElement))
        {
            foreach (var item in kpsElement.EnumerateArray())
            {
                var kp = new KnowledgePoint
                {
                    Id = Guid.Parse(item.GetProperty("id").GetString()!),
                    ChapterId = Guid.Parse(item.GetProperty("chapterId").GetString()!),
                    Name = item.GetProperty("name").GetString() ?? "",
                    Description = item.TryGetProperty("description", out var desc) ? desc.GetString() ?? "" : "",
                    SortOrder = item.TryGetProperty("sortOrder", out var so) ? so.GetInt32() : 0,
                    CreatedAt = item.TryGetProperty("createdAt", out var ca) ? ca.GetDateTime() : DateTime.UtcNow,
                    UpdatedAt = item.TryGetProperty("updatedAt", out var ua) ? ua.GetDateTime() : DateTime.UtcNow
                };
                _context.KnowledgePoints.Add(kp);
                kpCount++;
            }
        }

        // Import questions
        if (root.TryGetProperty("questions", out var questionsElement))
        {
            foreach (var item in questionsElement.EnumerateArray())
            {
                var question = new Question
                {
                    Id = Guid.Parse(item.GetProperty("id").GetString()!),
                    SubjectId = item.TryGetProperty("subjectId", out var sid) && sid.ValueKind == JsonValueKind.String
                        ? Guid.Parse(sid.GetString()!) : null,
                    ChapterId = item.TryGetProperty("chapterId", out var cid) && cid.ValueKind == JsonValueKind.String
                        ? Guid.Parse(cid.GetString()!) : null,
                    KnowledgePointId = item.TryGetProperty("knowledgePointId", out var kpid) && kpid.ValueKind == JsonValueKind.String
                        ? Guid.Parse(kpid.GetString()!) : null,
                    Content = item.GetProperty("content").GetString() ?? "",
                    Answer = item.TryGetProperty("answer", out var ans) ? ans.GetString() ?? "" : "",
                    UserAnswer = item.TryGetProperty("userAnswer", out var ua2) ? ua2.GetString() ?? "" : "",
                    ErrorType = item.TryGetProperty("errorType", out var et)
                        ? (et.ValueKind == JsonValueKind.String
                            ? Enum.Parse<Friday.Domain.Enums.ErrorType>(et.GetString()!)
                            : (Friday.Domain.Enums.ErrorType)et.GetInt32())
                        : Friday.Domain.Enums.ErrorType.Unknown,
                    ImagePath = item.TryGetProperty("imagePath", out var ip2) ? ip2.GetString() ?? "" : "",
                    Notes = item.TryGetProperty("notes", out var notes) ? notes.GetString() ?? "" : "",
                    ReviewDate = item.TryGetProperty("reviewDate", out var rd) ? rd.GetDateTime() : DateTime.UtcNow,
                    LeitnerBox = item.TryGetProperty("leitnerBox", out var lb) ? lb.GetInt32() : 1,
                    EaseFactor = item.TryGetProperty("easeFactor", out var ef) ? ef.GetDouble() : 2.5,
                    IntervalDays = item.TryGetProperty("intervalDays", out var id2) ? id2.GetInt32() : 1,
                    Streak = item.TryGetProperty("streak", out var streak) ? streak.GetInt32() : 0,
                    CreatedAt = item.TryGetProperty("createdAt", out var ca) ? ca.GetDateTime() : DateTime.UtcNow,
                    UpdatedAt = item.TryGetProperty("updatedAt", out var ua3) ? ua3.GetDateTime() : DateTime.UtcNow
                };
                _context.Questions.Add(question);
                questionCount++;
            }
        }

        await _context.SaveChangesAsync();

        return new ExportSummary(subjectCount, chapterCount, kpCount, questionCount);
    }

    public async Task<ExportSummary> GetExportSummaryAsync()
    {
        var subjectCount = await _context.Subjects.CountAsync();
        var chapterCount = await _context.Chapters.CountAsync();
        var kpCount = await _context.KnowledgePoints.CountAsync();
        var questionCount = await _context.Questions.CountAsync();

        return new ExportSummary(subjectCount, chapterCount, kpCount, questionCount);
    }

    private static string EscapeCsv(string value)
    {
        if (value.Contains(';') || value.Contains('"') || value.Contains('\n'))
        {
            return $"\"{value.Replace("\"", "\"\"")}\"";
        }
        return value;
    }
}
