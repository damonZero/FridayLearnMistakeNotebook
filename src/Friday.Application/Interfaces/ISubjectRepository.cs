using Friday.Domain.Entities;

namespace Friday.Application.Interfaces;

public interface ISubjectRepository
{
    Task<List<Subject>> GetAllAsync();
    Task<Subject?> GetByIdAsync(Guid id);
    Task<List<Subject>> GetPresetAsync();
    Task<Subject> AddAsync(Subject subject);
    Task<Subject> UpdateAsync(Subject subject);
    Task DeleteAsync(Guid id);
    Task<bool> ExistsByNameAsync(string name);
}
