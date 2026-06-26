using Friday.Application.Interfaces;
using Friday.Domain.Entities;
using Microsoft.EntityFrameworkCore;

namespace Friday.Infrastructure.Persistence.Repositories;

public class ChapterRepository : IChapterRepository
{
    private readonly AppDbContext _context;

    public ChapterRepository(AppDbContext context)
    {
        _context = context;
    }

    public async Task<List<Chapter>> GetAllAsync()
    {
        return await _context.Chapters
            .Include(c => c.Subject)
            .OrderBy(c => c.SortOrder)
            .ToListAsync();
    }

    public async Task<Chapter?> GetByIdAsync(Guid id)
    {
        return await _context.Chapters
            .Include(c => c.Subject)
            .FirstOrDefaultAsync(c => c.Id == id);
    }

    public async Task<List<Chapter>> GetBySubjectAsync(Guid subjectId)
    {
        return await _context.Chapters
            .Where(c => c.SubjectId == subjectId)
            .OrderBy(c => c.SortOrder)
            .ToListAsync();
    }

    public async Task<Chapter> AddAsync(Chapter chapter)
    {
        _context.Chapters.Add(chapter);
        await _context.SaveChangesAsync();
        return chapter;
    }

    public async Task<Chapter> UpdateAsync(Chapter chapter)
    {
        chapter.UpdatedAt = DateTime.UtcNow;
        _context.Chapters.Update(chapter);
        await _context.SaveChangesAsync();
        return chapter;
    }

    public async Task DeleteAsync(Guid id)
    {
        var chapter = await _context.Chapters.FindAsync(id);
        if (chapter != null)
        {
            _context.Chapters.Remove(chapter);
            await _context.SaveChangesAsync();
        }
    }
}
