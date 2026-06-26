using System.Net.Http.Json;
using System.Text;
using System.Text.Json;
using Friday.Application.Interfaces;
using Friday.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;

namespace Friday.Infrastructure.Services;

public class AiService : IAiService
{
    private readonly HttpClient _httpClient;
    private readonly AppDbContext _dbContext;
    private readonly ILogger<AiService> _logger;

    // Token pricing (CNY per 1K tokens)
    private static readonly Dictionary<string, (decimal Input, decimal Output)> TokenPricing = new()
    {
        // 火山方舟
        ["volcano_ark"] = (0.008m, 0.02m),
        // DeepSeek
        ["deepseek-chat"] = (0.001m, 0.002m),
        ["deepseek-reasoner"] = (0.004m, 0.016m),
        // OpenAI
        ["gpt-4"] = (0.2m, 0.6m),
        ["gpt-4o"] = (0.04m, 0.12m),
        // Claude
        ["claude-3-opus"] = (0.12m, 0.6m),
        ["claude-3-sonnet"] = (0.024m, 0.12m),
    };

    public AiService(HttpClient httpClient, AppDbContext dbContext, ILogger<AiService> logger)
    {
        _httpClient = httpClient;
        _dbContext = dbContext;
        _logger = logger;
    }

    public async Task<AiResponse> ExecuteAsync(AiRequest request, CancellationToken cancellationToken = default)
    {
        var config = await _dbContext.AiConfigs
            .FirstOrDefaultAsync(c => c.TaskType == request.TaskType && c.IsEnabled, cancellationToken);

        if (config == null)
        {
            return new AiResponse
            {
                IsSuccess = false,
                ErrorMessage = $"未配置 {request.TaskType} 任务的 AI 模型"
            };
        }

        try
        {
            return config.Provider switch
            {
                "volcano_ark" => await CallVolcanoArkAsync(config, request, cancellationToken),
                "deepseek" => await CallDeepSeekAsync(config, request, cancellationToken),
                "openai" => await CallOpenAiAsync(config, request, cancellationToken),
                "claude" => await CallClaudeAsync(config, request, cancellationToken),
                _ => new AiResponse { IsSuccess = false, ErrorMessage = $"不支持的 Provider: {config.Provider}" }
            };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "AI service call failed for provider {Provider}", config.Provider);
            return new AiResponse
            {
                IsSuccess = false,
                ErrorMessage = ex.Message
            };
        }
    }

    public async Task<bool> TestConnectionAsync(string provider, CancellationToken cancellationToken = default)
    {
        var config = await _dbContext.AiConfigs
            .FirstOrDefaultAsync(c => c.Provider == provider && c.IsEnabled, cancellationToken);

        if (config == null) return false;

        try
        {
            var request = new AiRequest
            {
                TaskType = config.TaskType,
                Prompt = "Hello, this is a connection test. Please respond with 'OK'.",
                MaxTokens = 10
            };

            var response = await ExecuteAsync(request, cancellationToken);
            return response.IsSuccess;
        }
        catch
        {
            return false;
        }
    }

    private async Task<AiResponse> CallVolcanoArkAsync(Domain.Entities.AiConfig config, AiRequest request, CancellationToken cancellationToken)
    {
        var baseUrl = config.BaseUrl ?? "https://ark.cn-beijing.volces.com/api/v3";
        var endpoint = request.TaskType == "ocr" ? "/chat/completions" : "/chat/completions";

        var messages = new List<object>();

        if (!string.IsNullOrEmpty(request.SystemPrompt))
        {
            messages.Add(new { role = "system", content = request.SystemPrompt });
        }

        if (request.TaskType == "ocr" && !string.IsNullOrEmpty(request.ImageBase64))
        {
            messages.Add(new
            {
                role = "user",
                content = new object[]
                {
                    new { type = "text", text = request.Prompt ?? "请识别图片中的文字内容，包括题目、选项和答案。" },
                    new { type = "image_url", image_url = new { url = $"data:image/jpeg;base64,{request.ImageBase64}" } }
                }
            });
        }
        else
        {
            messages.Add(new { role = "user", content = request.Prompt ?? "" });
        }

        var payload = new
        {
            model = config.ModelName ?? "doubao-1.5-vision-pro-32k",
            messages,
            max_tokens = request.MaxTokens,
            temperature = request.Temperature
        };

        _httpClient.DefaultRequestHeaders.Clear();
        _httpClient.DefaultRequestHeaders.Add("Authorization", $"Bearer {config.ApiKey}");

        var response = await _httpClient.PostAsJsonAsync($"{baseUrl}{endpoint}", payload, cancellationToken);
        var json = await response.Content.ReadAsStringAsync(cancellationToken);

        if (!response.IsSuccessStatusCode)
        {
            return new AiResponse
            {
                IsSuccess = false,
                ErrorMessage = $"火山方舟 API 错误: {response.StatusCode} - {json}"
            };
        }

        var result = JsonSerializer.Deserialize<JsonElement>(json);
        var content = result.GetProperty("choices")[0].GetProperty("message").GetProperty("content").GetString();
        var usage = result.GetProperty("usage");
        var inputTokens = usage.GetProperty("prompt_tokens").GetInt32();
        var outputTokens = usage.GetProperty("completion_tokens").GetInt32();
        var totalTokens = inputTokens + outputTokens;
        var cost = CalculateCost("volcano_ark", config.ModelName ?? "doubao-1.5-vision-pro-32k", inputTokens, outputTokens);

        return new AiResponse
        {
            IsSuccess = true,
            Content = content,
            InputTokens = inputTokens,
            OutputTokens = outputTokens,
            TotalTokens = totalTokens,
            EstimatedCost = cost,
            RequestId = result.TryGetProperty("id", out var id) ? id.GetString() : null
        };
    }

    private async Task<AiResponse> CallDeepSeekAsync(Domain.Entities.AiConfig config, AiRequest request, CancellationToken cancellationToken)
    {
        var baseUrl = config.BaseUrl ?? "https://api.deepseek.com";
        var endpoint = "/chat/completions";

        var messages = new List<object>();

        if (!string.IsNullOrEmpty(request.SystemPrompt))
        {
            messages.Add(new { role = "system", content = request.SystemPrompt });
        }

        messages.Add(new { role = "user", content = request.Prompt ?? "" });

        var payload = new
        {
            model = config.ModelName ?? "deepseek-chat",
            messages,
            max_tokens = request.MaxTokens,
            temperature = request.Temperature
        };

        _httpClient.DefaultRequestHeaders.Clear();
        _httpClient.DefaultRequestHeaders.Add("Authorization", $"Bearer {config.ApiKey}");

        var response = await _httpClient.PostAsJsonAsync($"{baseUrl}{endpoint}", payload, cancellationToken);
        var json = await response.Content.ReadAsStringAsync(cancellationToken);

        if (!response.IsSuccessStatusCode)
        {
            return new AiResponse
            {
                IsSuccess = false,
                ErrorMessage = $"DeepSeek API 错误: {response.StatusCode} - {json}"
            };
        }

        var result = JsonSerializer.Deserialize<JsonElement>(json);
        var content = result.GetProperty("choices")[0].GetProperty("message").GetProperty("content").GetString();
        var usage = result.GetProperty("usage");
        var inputTokens = usage.GetProperty("prompt_tokens").GetInt32();
        var outputTokens = usage.GetProperty("completion_tokens").GetInt32();
        var totalTokens = inputTokens + outputTokens;
        var cost = CalculateCost("deepseek", config.ModelName ?? "deepseek-chat", inputTokens, outputTokens);

        return new AiResponse
        {
            IsSuccess = true,
            Content = content,
            InputTokens = inputTokens,
            OutputTokens = outputTokens,
            TotalTokens = totalTokens,
            EstimatedCost = cost,
            RequestId = result.TryGetProperty("id", out var id) ? id.GetString() : null
        };
    }

    private async Task<AiResponse> CallOpenAiAsync(Domain.Entities.AiConfig config, AiRequest request, CancellationToken cancellationToken)
    {
        var baseUrl = config.BaseUrl ?? "https://api.openai.com/v1";
        var endpoint = "/chat/completions";

        var messages = new List<object>();

        if (!string.IsNullOrEmpty(request.SystemPrompt))
        {
            messages.Add(new { role = "system", content = request.SystemPrompt });
        }

        messages.Add(new { role = "user", content = request.Prompt ?? "" });

        var payload = new
        {
            model = config.ModelName ?? "gpt-4o",
            messages,
            max_tokens = request.MaxTokens,
            temperature = request.Temperature
        };

        _httpClient.DefaultRequestHeaders.Clear();
        _httpClient.DefaultRequestHeaders.Add("Authorization", $"Bearer {config.ApiKey}");

        var response = await _httpClient.PostAsJsonAsync($"{baseUrl}{endpoint}", payload, cancellationToken);
        var json = await response.Content.ReadAsStringAsync(cancellationToken);

        if (!response.IsSuccessStatusCode)
        {
            return new AiResponse
            {
                IsSuccess = false,
                ErrorMessage = $"OpenAI API 错误: {response.StatusCode} - {json}"
            };
        }

        var result = JsonSerializer.Deserialize<JsonElement>(json);
        var content = result.GetProperty("choices")[0].GetProperty("message").GetProperty("content").GetString();
        var usage = result.GetProperty("usage");
        var inputTokens = usage.GetProperty("prompt_tokens").GetInt32();
        var outputTokens = usage.GetProperty("completion_tokens").GetInt32();
        var totalTokens = inputTokens + outputTokens;
        var cost = CalculateCost("openai", config.ModelName ?? "gpt-4o", inputTokens, outputTokens);

        return new AiResponse
        {
            IsSuccess = true,
            Content = content,
            InputTokens = inputTokens,
            OutputTokens = outputTokens,
            TotalTokens = totalTokens,
            EstimatedCost = cost,
            RequestId = result.TryGetProperty("id", out var id) ? id.GetString() : null
        };
    }

    private async Task<AiResponse> CallClaudeAsync(Domain.Entities.AiConfig config, AiRequest request, CancellationToken cancellationToken)
    {
        var baseUrl = config.BaseUrl ?? "https://api.anthropic.com";
        var endpoint = "/v1/messages";

        var messages = new List<object>();

        if (request.TaskType == "ocr" && !string.IsNullOrEmpty(request.ImageBase64))
        {
            messages.Add(new
            {
                role = "user",
                content = new object[]
                {
                    new { type = "image", source = new { type = "base64", media_type = "image/jpeg", data = request.ImageBase64 } },
                    new { type = "text", text = request.Prompt ?? "请识别图片中的文字内容，包括题目、选项和答案。" }
                }
            });
        }
        else
        {
            messages.Add(new { role = "user", content = request.Prompt ?? "" });
        }

        var payload = new
        {
            model = config.ModelName ?? "claude-3-sonnet-20240229",
            max_tokens = request.MaxTokens,
            messages,
            system = request.SystemPrompt
        };

        _httpClient.DefaultRequestHeaders.Clear();
        _httpClient.DefaultRequestHeaders.Add("x-api-key", config.ApiKey);
        _httpClient.DefaultRequestHeaders.Add("anthropic-version", "2023-06-01");

        var response = await _httpClient.PostAsJsonAsync($"{baseUrl}{endpoint}", payload, cancellationToken);
        var json = await response.Content.ReadAsStringAsync(cancellationToken);

        if (!response.IsSuccessStatusCode)
        {
            return new AiResponse
            {
                IsSuccess = false,
                ErrorMessage = $"Claude API 错误: {response.StatusCode} - {json}"
            };
        }

        var result = JsonSerializer.Deserialize<JsonElement>(json);
        var content = result.GetProperty("content")[0].GetProperty("text").GetString();
        var usage = result.GetProperty("usage");
        var inputTokens = usage.GetProperty("input_tokens").GetInt32();
        var outputTokens = usage.GetProperty("output_tokens").GetInt32();
        var totalTokens = inputTokens + outputTokens;
        var cost = CalculateCost("claude", config.ModelName ?? "claude-3-sonnet-20240229", inputTokens, outputTokens);

        return new AiResponse
        {
            IsSuccess = true,
            Content = content,
            InputTokens = inputTokens,
            OutputTokens = outputTokens,
            TotalTokens = totalTokens,
            EstimatedCost = cost,
            RequestId = result.TryGetProperty("id", out var id) ? id.GetString() : null
        };
    }

    private decimal CalculateCost(string provider, string model, int inputTokens, int outputTokens)
    {
        var key = model.ToLower() switch
        {
            var m when m.Contains("doubao") => "volcano_ark",
            var m when m.Contains("deepseek-chat") => "deepseek-chat",
            var m when m.Contains("deepseek-reasoner") => "deepseek-reasoner",
            var m when m.Contains("gpt-4o") => "gpt-4o",
            var m when m.Contains("gpt-4") => "gpt-4",
            var m when m.Contains("claude-3-opus") => "claude-3-opus",
            var m when m.Contains("claude-3-sonnet") => "claude-3-sonnet",
            _ => provider
        };

        if (TokenPricing.TryGetValue(key, out var pricing))
        {
            return (inputTokens * pricing.Input + outputTokens * pricing.Output) / 1000m;
        }

        // Default pricing if not found
        return (inputTokens * 0.01m + outputTokens * 0.03m) / 1000m;
    }
}
