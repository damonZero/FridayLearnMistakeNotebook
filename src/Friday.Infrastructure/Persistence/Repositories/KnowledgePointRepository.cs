using Friday.Application.Interfaces;
using Friday.Domain.Entities;
using Microsoft.EntityFrameworkCore;

namespace Friday.Infrastructure.Persistence.Repositories;

public class KnowledgePointRepository : IKnowledgePointRepository
{
    private readonly AppDbContext _context;

    public KnowledgePointRepository(AppDbContext context)
    {
        _context = context;
    }

    public async Task<List<KnowledgePoint>> GetAllAsync()
    {
        return await _context.KnowledgePoints
            .Include(kp => kp.Chapter)
            .OrderBy(kp => kp.SortOrder)
            .ToListAsync();
    }

    public async Task<List<KnowledgePoint>> GetByChapterAsync(Guid chapterId)
    {
        return await _context.KnowledgePoints
            .Where(kp => kp.ChapterId == chapterId)
            .OrderBy(kp => kp.SortOrder)
            .ToListAsync();
    }

    public async Task<KnowledgePoint?> GetByIdAsync(Guid id)
    {
        return await _context.KnowledgePoints
            .Include(kp => kp.Chapter)
            .FirstOrDefaultAsync(kp => kp.Id == id);
    }

    public async Task<List<KnowledgePoint>> GetBySubjectAsync(Guid subjectId)
    {
        return await _context.KnowledgePoints
            .Include(kp => kp.Chapter)
            .Where(kp => kp.Chapter.SubjectId == subjectId)
            .OrderBy(kp => kp.Chapter.SortOrder)
            .ThenBy(kp => kp.SortOrder)
            .ToListAsync();
    }

    public async Task<KnowledgePoint> AddAsync(KnowledgePoint knowledgePoint)
    {
        _context.KnowledgePoints.Add(knowledgePoint);
        await _context.SaveChangesAsync();
        return knowledgePoint;
    }

    public async Task<KnowledgePoint> UpdateAsync(KnowledgePoint knowledgePoint)
    {
        knowledgePoint.UpdatedAt = DateTime.UtcNow;
        _context.KnowledgePoints.Update(knowledgePoint);
        await _context.SaveChangesAsync();
        return knowledgePoint;
    }

    public async Task DeleteAsync(Guid id)
    {
        var knowledgePoint = await _context.KnowledgePoints.FindAsync(id);
        if (knowledgePoint != null)
        {
            _context.KnowledgePoints.Remove(knowledgePoint);
            await _context.SaveChangesAsync();
        }
    }
}
