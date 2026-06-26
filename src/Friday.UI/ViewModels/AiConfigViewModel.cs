using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Friday.Application.Interfaces;
using Friday.Domain.Entities;
using Friday.UI.Services;

namespace Friday.UI.ViewModels;

public partial class AiConfigViewModel : ObservableObject
{
    private readonly IAiConfigService _aiConfigService;
    private readonly IAiService _aiService;
    private readonly NavigationService _navigationService;

    [ObservableProperty]
    private ObservableCollection<AiConfigItemViewModel> _configs = new();

    [ObservableProperty]
    private AiConfigItemViewModel? _selectedConfig;

    [ObservableProperty]
    private TokenUsageSummary? _usageSummary;

    [ObservableProperty]
    private DateTime? _fromDate;

    [ObservableProperty]
    private DateTime? _toDate;

    [ObservableProperty]
    private bool _isLoading;

    [ObservableProperty]
    private string _statusMessage = string.Empty;

    public AiConfigViewModel(IAiConfigService aiConfigService, IAiService aiService, NavigationService navigationService)
    {
        _aiConfigService = aiConfigService;
        _aiService = aiService;
        _navigationService = navigationService;
        _ = LoadConfigsAsync();
    }

    [RelayCommand]
    private async Task LoadConfigsAsync()
    {
        IsLoading = true;
        try
        {
            var configs = await _aiConfigService.GetAllConfigsAsync();
            Configs.Clear();

            // Add default configs if not exists
            var defaultConfigs = GetDefaultConfigs();
            foreach (var defaultConfig in defaultConfigs)
            {
                var existing = configs.FirstOrDefault(c => c.TaskType == defaultConfig.TaskType);
                if (existing == null)
                {
                    Configs.Add(new AiConfigItemViewModel(defaultConfig, this));
                }
                else
                {
                    Configs.Add(new AiConfigItemViewModel(existing, this));
                }
            }

            // Load usage summary
            await LoadUsageSummaryAsync();
        }
        catch (Exception ex)
        {
            StatusMessage = $"加载配置失败: {ex.Message}";
        }
        finally
        {
            IsLoading = false;
        }
    }

    [RelayCommand]
    private async Task LoadUsageSummaryAsync()
    {
        try
        {
            UsageSummary = await _aiConfigService.GetUsageSummaryAsync(FromDate, ToDate);
        }
        catch (Exception ex)
        {
            StatusMessage = $"加载使用统计失败: {ex.Message}";
        }
    }

    [RelayCommand]
    private async Task SaveConfigAsync(AiConfigItemViewModel configViewModel)
    {
        try
        {
            var config = configViewModel.ToEntity();
            await _aiConfigService.SaveConfigAsync(config);
            StatusMessage = "配置已保存";
            await LoadConfigsAsync();
        }
        catch (Exception ex)
        {
            StatusMessage = $"保存配置失败: {ex.Message}";
        }
    }

    [RelayCommand]
    private async Task TestConnectionAsync(AiConfigItemViewModel configViewModel)
    {
        try
        {
            configViewModel.IsTesting = true;
            configViewModel.TestResult = "测试中...";

            var config = configViewModel.ToEntity();
            await _aiConfigService.SaveConfigAsync(config);

            var success = await _aiService.TestConnectionAsync(config.Provider);
            configViewModel.TestResult = success ? "✓ 连接成功" : "✗ 连接失败";
            configViewModel.TestResultColor = success ? "#7ED321" : "#FF4444";
        }
        catch (Exception ex)
        {
            configViewModel.TestResult = $"✗ 错误: {ex.Message}";
            configViewModel.TestResultColor = "#FF4444";
        }
        finally
        {
            configViewModel.IsTesting = false;
        }
    }

    [RelayCommand]
    private void NavigateBack()
    {
        _navigationService.NavigateTo<SettingsViewModel>("设置");
    }

    private List<AiConfig> GetDefaultConfigs()
    {
        return new List<AiConfig>
        {
            new AiConfig
            {
                TaskType = "ocr",
                Provider = "volcano_ark",
                ModelName = "doubao-1.5-vision-pro-32k",
                IsEnabled = true
            },
            new AiConfig
            {
                TaskType = "analysis",
                Provider = "deepseek",
                ModelName = "deepseek-chat",
                IsEnabled = true
            },
            new AiConfig
            {
                TaskType = "generation",
                Provider = "deepseek",
                ModelName = "deepseek-chat",
                IsEnabled = true
            }
        };
    }
}

public partial class AiConfigItemViewModel : ObservableObject
{
    private readonly AiConfigViewModel _parentViewModel;

    [ObservableProperty]
    private Guid _id;

    [ObservableProperty]
    private string _provider = string.Empty;

    [ObservableProperty]
    private string _apiKey = string.Empty;

    [ObservableProperty]
    private string? _baseUrl;

    [ObservableProperty]
    private string? _modelName;

    [ObservableProperty]
    private string _taskType = string.Empty;

    [ObservableProperty]
    private bool _isEnabled = true;

    [ObservableProperty]
    private bool _isTesting;

    [ObservableProperty]
    private string _testResult = string.Empty;

    [ObservableProperty]
    private string _testResultColor = "#666666";

    public string TaskTypeDisplay => TaskType switch
    {
        "ocr" => "🔍 OCR 识别（视觉）",
        "analysis" => "📊 错题分析",
        "generation" => "✍️ 生成相似题",
        _ => TaskType
    };

    public string ProviderDisplay => Provider switch
    {
        "volcano_ark" => "🌋 火山方舟 (Ark)",
        "deepseek" => "🐋 DeepSeek",
        "openai" => "🤖 OpenAI",
        "claude" => "🧠 Claude",
        _ => Provider
    };

    public List<string> Providers => new() { "volcano_ark", "deepseek", "openai", "claude" };

    public List<string> ProviderDisplays => new()
    {
        "🌋 火山方舟 (Ark)",
        "🐋 DeepSeek",
        "🤖 OpenAI",
        "🧠 Claude"
    };

    public int SelectedProviderIndex
    {
        get => Providers.IndexOf(Provider);
        set
        {
            if (value >= 0 && value < Providers.Count)
            {
                Provider = Providers[value];
                OnPropertyChanged();
                OnPropertyChanged(nameof(ProviderDisplay));
                UpdateModelName();
            }
        }
    }

    public AiConfigItemViewModel(AiConfig config, AiConfigViewModel parentViewModel)
    {
        _parentViewModel = parentViewModel;
        Id = config.Id;
        Provider = config.Provider;
        ApiKey = config.ApiKey;
        BaseUrl = config.BaseUrl;
        ModelName = config.ModelName;
        TaskType = config.TaskType;
        IsEnabled = config.IsEnabled;
    }

    [RelayCommand]
    private async Task SaveAsync()
    {
        await _parentViewModel.SaveConfigCommand.ExecuteAsync(this);
    }

    [RelayCommand]
    private async Task TestConnectionAsync()
    {
        await _parentViewModel.TestConnectionCommand.ExecuteAsync(this);
    }

    public AiConfig ToEntity()
    {
        return new AiConfig
        {
            Id = Id,
            Provider = Provider,
            ApiKey = ApiKey,
            BaseUrl = BaseUrl,
            ModelName = ModelName,
            TaskType = TaskType,
            IsEnabled = IsEnabled
        };
    }

    private void UpdateModelName()
    {
        ModelName = Provider switch
        {
            "volcano_ark" => TaskType == "ocr" ? "doubao-1.5-vision-pro-32k" : "doubao-1.5-pro-32k",
            "deepseek" => "deepseek-chat",
            "openai" => "gpt-4o",
            "claude" => "claude-3-sonnet-20240229",
            _ => ModelName
        };
        OnPropertyChanged(nameof(ModelName));
    }
}
