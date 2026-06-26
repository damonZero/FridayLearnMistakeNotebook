using Friday.Application.Interfaces;
using Friday.Domain.Entities;
using Microsoft.EntityFrameworkCore;

namespace Friday.Infrastructure.Persistence.Repositories;

public class SubjectRepository : ISubjectRepository
{
    private readonly AppDbContext _context;

    public SubjectRepository(AppDbContext context)
    {
        _context = context;
    }

    public async Task<List<Subject>> GetAllAsync()
    {
        return await _context.Subjects
            .OrderBy(s => s.SortOrder)
            .ToListAsync();
    }

    public async Task<Subject?> GetByIdAsync(Guid id)
    {
        return await _context.Subjects.FindAsync(id);
    }

    public async Task<List<Subject>> GetPresetAsync()
    {
        return await _context.Subjects
            .Where(s => s.IsPreset)
            .OrderBy(s => s.SortOrder)
            .ToListAsync();
    }

    public async Task<Subject> AddAsync(Subject subject)
    {
        _context.Subjects.Add(subject);
        await _context.SaveChangesAsync();
        return subject;
    }

    public async Task<Subject> UpdateAsync(Subject subject)
    {
        subject.UpdatedAt = DateTime.UtcNow;
        _context.Subjects.Update(subject);
        await _context.SaveChangesAsync();
        return subject;
    }

    public async Task DeleteAsync(Guid id)
    {
        var subject = await _context.Subjects.FindAsync(id);
        if (subject != null)
        {
            _context.Subjects.Remove(subject);
            await _context.SaveChangesAsync();
        }
    }

    public async Task<bool> ExistsByNameAsync(string name)
    {
        return await _context.Subjects.AnyAsync(s => s.Name == name);
    }

    public static async Task SeedPresetSubjectsAsync(AppDbContext context)
    {
        if (await context.Subjects.AnyAsync())
            return;

        var presets = new List<Subject>
        {
            new Subject
            {
                Id = Guid.NewGuid(),
                Name = "语文",
                Icon = "📖",
                Color = "#4A90D9",
                SortOrder = 1,
                IsPreset = true,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            },
            new Subject
            {
                Id = Guid.NewGuid(),
                Name = "数学",
                Icon = "🔢",
                Color = "#7ED321",
                SortOrder = 2,
                IsPreset = true,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            },
            new Subject
            {
                Id = Guid.NewGuid(),
                Name = "英语",
                Icon = "🔤",
                Color = "#F5A623",
                SortOrder = 3,
                IsPreset = true,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            }
        };

        context.Subjects.AddRange(presets);
        await context.SaveChangesAsync();
    }
}
