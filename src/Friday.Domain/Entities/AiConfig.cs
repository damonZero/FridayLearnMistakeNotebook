namespace Friday.Domain.Entities;

public class AiConfig
{
    public Guid Id { get; set; }
    public string Provider { get; set; } = string.Empty; // "volcano_ark", "deepseek", "openai", "claude"
    public string ApiKey { get; set; } = string.Empty;
    public string? BaseUrl { get; set; }
    public string? ModelName { get; set; }
    public string TaskType { get; set; } = string.Empty; // "ocr", "analysis", "generation"
    public bool IsEnabled { get; set; } = true;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}

public class AiUsageLog
{
    public Guid Id { get; set; }
    public string Provider { get; set; } = string.Empty;
    public string TaskType { get; set; } = string.Empty;
    public string ModelName { get; set; } = string.Empty;
    public int InputTokens { get; set; }
    public int OutputTokens { get; set; }
    public int TotalTokens { get; set; }
    public decimal EstimatedCost { get; set; } // in CNY
    public string? RequestId { get; set; }
    public bool IsSuccess { get; set; }
    public string? ErrorMessage { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
