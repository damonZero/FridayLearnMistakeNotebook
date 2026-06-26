namespace Friday.Application.Interfaces;

public class AiRequest
{
    public string TaskType { get; set; } = string.Empty; // "ocr", "analysis", "generation"
    public string? ImageBase64 { get; set; } // for OCR
    public string? Prompt { get; set; }
    public string? SystemPrompt { get; set; }
    public int MaxTokens { get; set; } = 2000;
    public double Temperature { get; set; } = 0.7;
}

public class AiResponse
{
    public bool IsSuccess { get; set; }
    public string? Content { get; set; }
    public int InputTokens { get; set; }
    public int OutputTokens { get; set; }
    public int TotalTokens { get; set; }
    public decimal EstimatedCost { get; set; }
    public string? RequestId { get; set; }
    public string? ErrorMessage { get; set; }
}

public class TokenUsageSummary
{
    public int TotalRequests { get; set; }
    public int TotalInputTokens { get; set; }
    public int TotalOutputTokens { get; set; }
    public int TotalTokens { get; set; }
    public decimal TotalEstimatedCost { get; set; }
    public Dictionary<string, TokenUsageByProvider> ByProvider { get; set; } = new();
    public Dictionary<string, TokenUsageByTask> ByTask { get; set; } = new();
}

public class TokenUsageByProvider
{
    public string Provider { get; set; } = string.Empty;
    public int Requests { get; set; }
    public int Tokens { get; set; }
    public decimal Cost { get; set; }
}

public class TokenUsageByTask
{
    public string TaskType { get; set; } = string.Empty;
    public int Requests { get; set; }
    public int Tokens { get; set; }
    public decimal Cost { get; set; }
}

public interface IAiService
{
    Task<AiResponse> ExecuteAsync(AiRequest request, CancellationToken cancellationToken = default);
    Task<bool> TestConnectionAsync(string provider, CancellationToken cancellationToken = default);
}

public interface IAiConfigService
{
    Task<List<Domain.Entities.AiConfig>> GetAllConfigsAsync();
    Task<Domain.Entities.AiConfig?> GetConfigByTaskAsync(string taskType);
    Task SaveConfigAsync(Domain.Entities.AiConfig config);
    Task DeleteConfigAsync(Guid id);
    Task<List<Domain.Entities.AiUsageLog>> GetUsageLogsAsync(DateTime? fromDate, DateTime? toDate);
    Task<TokenUsageSummary> GetUsageSummaryAsync(DateTime? fromDate, DateTime? toDate);
    Task LogUsageAsync(string provider, string taskType, string modelName, int inputTokens, int outputTokens, decimal cost, bool isSuccess, string? requestId = null, string? errorMessage = null);
}
