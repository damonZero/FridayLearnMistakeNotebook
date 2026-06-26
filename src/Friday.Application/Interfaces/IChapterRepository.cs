using Friday.Domain.Entities;

namespace Friday.Application.Interfaces;

public interface IChapterRepository
{
    Task<List<Chapter>> GetAllAsync();
    Task<Chapter?> GetByIdAsync(Guid id);
    Task<List<Chapter>> GetBySubjectAsync(Guid subjectId);
    Task<Chapter> AddAsync(Chapter chapter);
    Task<Chapter> UpdateAsync(Chapter chapter);
    Task DeleteAsync(Guid id);
}
