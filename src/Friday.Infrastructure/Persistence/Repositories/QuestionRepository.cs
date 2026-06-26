using Friday.Application.Interfaces;
using Friday.Domain.Entities;
using Friday.Domain.Enums;
using Microsoft.EntityFrameworkCore;

namespace Friday.Infrastructure.Persistence.Repositories;

public class QuestionRepository : IQuestionRepository
{
    private readonly AppDbContext _context;

    public QuestionRepository(AppDbContext context)
    {
        _context = context;
    }

    public async Task<List<Question>> GetAllAsync()
    {
        return await _context.Questions
            .Include(q => q.Subject)
            .OrderByDescending(q => q.CreatedAt)
            .ToListAsync();
    }

    public async Task<Question?> GetByIdAsync(Guid id)
    {
        return await _context.Questions
            .Include(q => q.Subject)
            .Include(q => q.Chapter)
            .Include(q => q.KnowledgePoint)
            .FirstOrDefaultAsync(q => q.Id == id);
    }

    public async Task<List<Question>> GetBySubjectAsync(Guid subjectId)
    {
        return await _context.Questions
            .Where(q => q.SubjectId == subjectId)
            .OrderByDescending(q => q.CreatedAt)
            .ToListAsync();
    }

    public async Task<List<Question>> GetByKnowledgePointAsync(Guid knowledgePointId)
    {
        return await _context.Questions
            .Where(q => q.KnowledgePointId == knowledgePointId)
            .OrderByDescending(q => q.CreatedAt)
            .ToListAsync();
    }

    public async Task<List<Question>> GetFilteredAsync(
        Guid? subjectId = null,
        DateTime? dateFrom = null,
        DateTime? dateTo = null,
        ErrorType? errorType = null,
        string? sortBy = null)
    {
        var query = _context.Questions
            .Include(q => q.Subject)
            .AsQueryable();

        if (subjectId.HasValue)
            query = query.Where(q => q.SubjectId == subjectId.Value);

        if (dateFrom.HasValue)
            query = query.Where(q => q.CreatedAt >= dateFrom.Value);

        if (dateTo.HasValue)
            query = query.Where(q => q.CreatedAt <= dateTo.Value);

        if (errorType.HasValue)
            query = query.Where(q => q.ErrorType == errorType.Value);

        query = sortBy?.ToLower() switch
        {
            "reviewcount" => query.OrderByDescending(q => q.Streak),
            "mastery" => query.OrderByDescending(q => q.LeitnerBox),
            _ => query.OrderByDescending(q => q.CreatedAt)
        };

        return await query.ToListAsync();
    }

    public async Task<Question> AddAsync(Question question)
    {
        _context.Questions.Add(question);
        await _context.SaveChangesAsync();
        return question;
    }

    public async Task<Question> UpdateAsync(Question question)
    {
        question.UpdatedAt = DateTime.UtcNow;
        _context.Questions.Update(question);
        await _context.SaveChangesAsync();
        return question;
    }

    public async Task DeleteAsync(Guid id)
    {
        var question = await _context.Questions.FindAsync(id);
        if (question != null)
        {
            _context.Questions.Remove(question);
            await _context.SaveChangesAsync();
        }
    }
}
