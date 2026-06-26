using Friday.Application.Interfaces;
using Friday.Domain.Entities;
using Friday.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace Friday.Infrastructure.Services;

public class AiConfigService : IAiConfigService
{
    private readonly AppDbContext _dbContext;

    public AiConfigService(AppDbContext dbContext)
    {
        _dbContext = dbContext;
    }

    public async Task<List<AiConfig>> GetAllConfigsAsync()
    {
        return await _dbContext.AiConfigs
            .OrderBy(c => c.TaskType)
            .ThenBy(c => c.Provider)
            .ToListAsync();
    }

    public async Task<AiConfig?> GetConfigByTaskAsync(string taskType)
    {
        return await _dbContext.AiConfigs
            .FirstOrDefaultAsync(c => c.TaskType == taskType && c.IsEnabled);
    }

    public async Task SaveConfigAsync(AiConfig config)
    {
        var existing = await _dbContext.AiConfigs.FindAsync(config.Id);

        if (existing != null)
        {
            existing.Provider = config.Provider;
            existing.ApiKey = config.ApiKey;
            existing.BaseUrl = config.BaseUrl;
            existing.ModelName = config.ModelName;
            existing.TaskType = config.TaskType;
            existing.IsEnabled = config.IsEnabled;
            existing.UpdatedAt = DateTime.UtcNow;
        }
        else
        {
            config.Id = Guid.NewGuid();
            config.CreatedAt = DateTime.UtcNow;
            config.UpdatedAt = DateTime.UtcNow;
            _dbContext.AiConfigs.Add(config);
        }

        await _dbContext.SaveChangesAsync();
    }

    public async Task DeleteConfigAsync(Guid id)
    {
        var config = await _dbContext.AiConfigs.FindAsync(id);
        if (config != null)
        {
            _dbContext.AiConfigs.Remove(config);
            await _dbContext.SaveChangesAsync();
        }
    }

    public async Task<List<AiUsageLog>> GetUsageLogsAsync(DateTime? fromDate, DateTime? toDate)
    {
        var query = _dbContext.AiUsageLogs.AsQueryable();

        if (fromDate.HasValue)
            query = query.Where(l => l.CreatedAt >= fromDate.Value);

        if (toDate.HasValue)
            query = query.Where(l => l.CreatedAt <= toDate.Value);

        return await query
            .OrderByDescending(l => l.CreatedAt)
            .ToListAsync();
    }

    public async Task<TokenUsageSummary> GetUsageSummaryAsync(DateTime? fromDate, DateTime? toDate)
    {
        var logs = await GetUsageLogsAsync(fromDate, toDate);

        var summary = new TokenUsageSummary
        {
            TotalRequests = logs.Count,
            TotalInputTokens = logs.Sum(l => l.InputTokens),
            TotalOutputTokens = logs.Sum(l => l.OutputTokens),
            TotalTokens = logs.Sum(l => l.TotalTokens),
            TotalEstimatedCost = logs.Sum(l => l.EstimatedCost)
        };

        // Group by provider
        summary.ByProvider = logs
            .GroupBy(l => l.Provider)
            .ToDictionary(
                g => g.Key,
                g => new TokenUsageByProvider
                {
                    Provider = g.Key,
                    Requests = g.Count(),
                    Tokens = g.Sum(l => l.TotalTokens),
                    Cost = g.Sum(l => l.EstimatedCost)
                });

        // Group by task
        summary.ByTask = logs
            .GroupBy(l => l.TaskType)
            .ToDictionary(
                g => g.Key,
                g => new TokenUsageByTask
                {
                    TaskType = g.Key,
                    Requests = g.Count(),
                    Tokens = g.Sum(l => l.TotalTokens),
                    Cost = g.Sum(l => l.EstimatedCost)
                });

        return summary;
    }

    public async Task LogUsageAsync(string provider, string taskType, string modelName, int inputTokens, int outputTokens, decimal cost, bool isSuccess, string? requestId = null, string? errorMessage = null)
    {
        var log = new AiUsageLog
        {
            Id = Guid.NewGuid(),
            Provider = provider,
            TaskType = taskType,
            ModelName = modelName,
            InputTokens = inputTokens,
            OutputTokens = outputTokens,
            TotalTokens = inputTokens + outputTokens,
            EstimatedCost = cost,
            RequestId = requestId,
            IsSuccess = isSuccess,
            ErrorMessage = errorMessage,
            CreatedAt = DateTime.UtcNow
        };

        _dbContext.AiUsageLogs.Add(log);
        await _dbContext.SaveChangesAsync();
    }
}
