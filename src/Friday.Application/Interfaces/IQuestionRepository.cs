using Friday.Domain.Entities;
using Friday.Domain.Enums;

namespace Friday.Application.Interfaces;

public interface IQuestionRepository
{
    Task<List<Question>> GetAllAsync();
    Task<Question?> GetByIdAsync(Guid id);
    Task<List<Question>> GetBySubjectAsync(Guid subjectId);
    Task<List<Question>> GetByKnowledgePointAsync(Guid knowledgePointId);
    Task<List<Question>> GetFilteredAsync(Guid? subjectId = null, DateTime? dateFrom = null, DateTime? dateTo = null, ErrorType? errorType = null, string? sortBy = null);
    Task<Question> AddAsync(Question question);
    Task<Question> UpdateAsync(Question question);
    Task DeleteAsync(Guid id);
}
