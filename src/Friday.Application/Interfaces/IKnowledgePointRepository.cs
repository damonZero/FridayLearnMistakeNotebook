using Friday.Domain.Entities;

namespace Friday.Application.Interfaces;

public interface IKnowledgePointRepository
{
    Task<List<KnowledgePoint>> GetAllAsync();
    Task<List<KnowledgePoint>> GetByChapterAsync(Guid chapterId);
    Task<KnowledgePoint?> GetByIdAsync(Guid id);
    Task<List<KnowledgePoint>> GetBySubjectAsync(Guid subjectId);
    Task<KnowledgePoint> AddAsync(KnowledgePoint knowledgePoint);
    Task<KnowledgePoint> UpdateAsync(KnowledgePoint knowledgePoint);
    Task DeleteAsync(Guid id);
}
