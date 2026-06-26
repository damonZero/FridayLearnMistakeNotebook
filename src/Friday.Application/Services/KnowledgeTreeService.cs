using Friday.Application.Interfaces;
using Friday.Domain.Entities;

namespace Friday.Application.Services;

public class KnowledgeTreeService
{
    private readonly IChapterRepository _chapterRepository;
    private readonly IKnowledgePointRepository _knowledgePointRepository;

    public KnowledgeTreeService(
        IChapterRepository chapterRepository,
        IKnowledgePointRepository knowledgePointRepository)
    {
        _chapterRepository = chapterRepository;
        _knowledgePointRepository = knowledgePointRepository;
    }

    public async Task<List<Chapter>> GetBySubjectAsync(Guid subjectId)
    {
        return await _chapterRepository.GetBySubjectAsync(subjectId);
    }

    public async Task<List<KnowledgePoint>> GetKnowledgePointsByChapterAsync(Guid chapterId)
    {
        return await _knowledgePointRepository.GetByChapterAsync(chapterId);
    }

    public async Task<Chapter> AddChapterAsync(Guid subjectId, string name, int sortOrder)
    {
        if (string.IsNullOrWhiteSpace(name))
            throw new ArgumentException("Chapter name cannot be empty.", nameof(name));

        var chapter = new Chapter
        {
            SubjectId = subjectId,
            Name = name.Trim(),
            SortOrder = sortOrder,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        return await _chapterRepository.AddAsync(chapter);
    }

    public async Task<Chapter> UpdateChapterAsync(Guid id, string name, int sortOrder)
    {
        var chapter = await _chapterRepository.GetByIdAsync(id)
            ?? throw new InvalidOperationException($"Chapter with id '{id}' not found.");

        if (string.IsNullOrWhiteSpace(name))
            throw new ArgumentException("Chapter name cannot be empty.", nameof(name));

        chapter.Name = name.Trim();
        chapter.SortOrder = sortOrder;
        chapter.UpdatedAt = DateTime.UtcNow;

        return await _chapterRepository.UpdateAsync(chapter);
    }

    public async Task DeleteChapterAsync(Guid id)
    {
        var chapter = await _chapterRepository.GetByIdAsync(id)
            ?? throw new InvalidOperationException($"Chapter with id '{id}' not found.");

        // Delete all knowledge points under this chapter first
        var knowledgePoints = await _knowledgePointRepository.GetByChapterAsync(id);
        foreach (var kp in knowledgePoints)
        {
            await _knowledgePointRepository.DeleteAsync(kp.Id);
        }

        await _chapterRepository.DeleteAsync(id);
    }

    public async Task<KnowledgePoint> AddKnowledgePointAsync(
        Guid chapterId, string name, string description, int sortOrder)
    {
        if (string.IsNullOrWhiteSpace(name))
            throw new ArgumentException("Knowledge point name cannot be empty.", nameof(name));

        var knowledgePoint = new KnowledgePoint
        {
            ChapterId = chapterId,
            Name = name.Trim(),
            Description = description?.Trim() ?? string.Empty,
            SortOrder = sortOrder,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        return await _knowledgePointRepository.AddAsync(knowledgePoint);
    }

    public async Task<KnowledgePoint> UpdateKnowledgePointAsync(
        Guid id, string name, string description, int sortOrder)
    {
        var knowledgePoint = await _knowledgePointRepository.GetByIdAsync(id)
            ?? throw new InvalidOperationException($"Knowledge point with id '{id}' not found.");

        if (string.IsNullOrWhiteSpace(name))
            throw new ArgumentException("Knowledge point name cannot be empty.", nameof(name));

        knowledgePoint.Name = name.Trim();
        knowledgePoint.Description = description?.Trim() ?? string.Empty;
        knowledgePoint.SortOrder = sortOrder;
        knowledgePoint.UpdatedAt = DateTime.UtcNow;

        return await _knowledgePointRepository.UpdateAsync(knowledgePoint);
    }

    public async Task DeleteKnowledgePointAsync(Guid id)
    {
        await _knowledgePointRepository.DeleteAsync(id);
    }
}
