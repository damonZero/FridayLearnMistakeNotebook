# Architecture Patterns

**Domain:** C# Desktop Learning Application (Wrong Answer Book)
**Researched:** 2026-06-26

## Recommended Architecture

**Clean Architecture + MVVM** is the standard pattern for C# desktop applications with complex business logic. This provides clear separation of concerns, testability, and flexibility to swap infrastructure components (database, AI providers, UI framework).

### High-Level Layer Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      Presentation Layer                          │
│  (WPF/WinUI 3 - Views, ViewModels, UI Services)                │
├─────────────────────────────────────────────────────────────────┤
│                      Application Layer                           │
│  (Use Cases, Services, DTOs, Interfaces)                        │
├─────────────────────────────────────────────────────────────────┤
│                        Domain Layer                              │
│  (Entities, Value Objects, Domain Services, Business Rules)     │
├─────────────────────────────────────────────────────────────────┤
│                     Infrastructure Layer                         │
│  (SQLite, AI APIs, Camera, File System, External Services)      │
└─────────────────────────────────────────────────────────────────┘
```

**Dependency Rule:** Dependencies point inward. Domain knows nothing about other layers. Infrastructure implements interfaces defined in Application/Domain.

## Component Boundaries

### 1. Domain Layer (Core)

**Responsibility:** Business entities, value objects, domain services, and business rules. Zero external dependencies.

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| `WrongQuestion` Entity | Core entity: question text, images, subject, knowledge points, difficulty | Application Layer |
| `KnowledgePoint` Entity | Knowledge point hierarchy, subject categorization | Application Layer |
| `ReviewSchedule` Entity | Leitner box level, next review date, review history | Application Layer |
| `LearningAlgorithm` Domain Service | Ebbinghaus forgetting curve, Leitner box logic, spaced repetition calculations | Application Layer |
| `Subject` Entity | Subject management (Chinese, Math, English, custom) | Application Layer |

**Key Domain Models:**
```csharp
public class WrongQuestion
{
    public Guid Id { get; private set; }
    public string QuestionText { get; private set; }
    public byte[] OriginalImage { get; private set; }
    public byte[] ProcessedImage { get; private set; }
    public Guid SubjectId { get; private set; }
    public List<KnowledgePoint> KnowledgePoints { get; private set; }
    public DifficultyLevel Difficulty { get; private set; }
    public ReviewSchedule ReviewSchedule { get; private set; }
    public DateTime CreatedAt { get; private set; }
    public DateTime? LastReviewedAt { get; private set; }
}

public class ReviewSchedule
{
    public int LeitnerBox { get; private set; }  // 1-5
    public DateTime NextReviewDate { get; private set; }
    public int CorrectCount { get; private set; }
    public int IncorrectCount { get; private set; }
    public List<ReviewRecord> History { get; private set; }
}

public class KnowledgePoint
{
    public Guid Id { get; private set; }
    public string Name { get; private set; }
    public Guid SubjectId { get; private set; }
    public Guid? ParentId { get; private set; }  // Hierarchy support
    public List<KnowledgePoint> Children { get; private set; }
}
```

### 2. Application Layer (Use Cases)

**Responsibility:** Orchestrate domain objects, define interfaces for infrastructure, implement use cases.

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| `IQuestionRepository` | Interface for question persistence | Infrastructure |
| `IKnowledgePointRepository` | Interface for knowledge point persistence | Infrastructure |
| `IAIService` | Interface for AI operations (OCR, analysis, generation) | Infrastructure |
| `ICameraService` | Interface for camera capture | Infrastructure |
| `IImageProcessor` | Interface for image processing | Infrastructure |
| `ILearningAlgorithm` | Interface for spaced repetition calculations | Domain |
| `QuestionService` | Use case: capture, process, store questions | Domain, Infrastructure |
| `ReviewService` | Use case: schedule reviews, track progress | Domain |
| `SubjectService` | Use case: manage subjects | Domain |
| `AIService` (implementation) | Orchestrate AI calls, handle responses | Infrastructure |

**Key Application Services:**
```csharp
public interface IQuestionService
{
    Task<WrongQuestion> CaptureAndProcessAsync(byte[] imageData, Guid subjectId);
    Task<WrongQuestion> AddManualAsync(string questionText, Guid subjectId, byte[]? image = null);
    Task<List<WrongQuestion>> GetQuestionsBySubjectAsync(Guid subjectId);
    Task<List<WrongQuestion>> GetQuestionsByKnowledgePointAsync(Guid knowledgePointId);
    Task DeleteAsync(Guid questionId);
}

public interface IReviewService
{
    Task<List<WrongQuestion>> GetDueReviewsAsync();
    Task<List<WrongQuestion>> GetDueReviewsBySubjectAsync(Guid subjectId);
    Task SubmitReviewResultAsync(Guid questionId, bool isCorrect);
    Task<ReviewStatistics> GetStatisticsAsync();
    Task<ReviewStatistics> GetStatisticsBySubjectAsync(Guid subjectId);
}

public interface IAIService
{
    Task<string> PerformOCRAsync(byte[] image, AIModelConfig config);
    Task<KnowledgeAnalysisResult> AnalyzeKnowledgeAsync(string questionText, Guid subjectId, AIModelConfig config);
    Task<List<string>> GenerateSimilarQuestionsAsync(string questionText, int count, AIModelConfig config);
}

public class AIModelConfig
{
    public string Provider { get; set; }  // "OpenAI", "Baidu", "Custom"
    public string ApiKey { get; set; }
    public string ModelName { get; set; }
    public string BaseUrl { get; set; }  // For custom endpoints
}
```

### 3. Infrastructure Layer

**Responsibility:** Implement interfaces defined in Application layer. Handle external concerns.

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| `SQLiteDbContext` | EF Core DbContext, database operations | Application Layer |
| `QuestionRepository` | Implement IQuestionRepository | Application Layer |
| `KnowledgePointRepository` | Implement IKnowledgePointRepository | Application Layer |
| `OpenAIService` | Implement IAIService for OpenAI API | Application Layer |
| `BaiduAIService` | Implement IAIService for Baidu OCR/NLP | Application Layer |
| `CameraService` | Implement ICameraService using AForge/EmguCV | Application Layer |
| `ImageProcessor` | Implement IImageProcessor for image enhancement | Application Layer |
| `FileStorageService` | Handle image file storage | Application Layer |
| `BackupService` | Local backup management | Application Layer |

**Infrastructure Implementation Examples:**
```csharp
// Camera Service using AForge.NET
public class AForgeCameraService : ICameraService
{
    private VideoCaptureDevice _camera;
    private FilterInfoCollection _videoDevices;

    public async Task<byte[]> CaptureFrameAsync()
    {
        // Capture frame from camera
        // Convert to byte array
        // Return processed image
    }

    public List<CameraDevice> GetAvailableCameras()
    {
        _videoDevices = new FilterInfoCollection(FilterCategory.VideoInputDevice);
        return _videoDevices.Cast<FilterInfo>()
            .Select(f => new CameraDevice { Name = f.Name, MonikerString = f.MonikerString })
            .ToList();
    }
}

// AI Service with HttpClient
public class OpenAIService : IAIService
{
    private readonly HttpClient _httpClient;
    private readonly JsonSerializerOptions _jsonOptions;

    public OpenAIService(HttpClient httpClient)
    {
        _httpClient = httpClient;
        _jsonOptions = new JsonSerializerOptions { PropertyNamingPolicy = JsonNamingPolicy.CamelCase };
    }

    public async Task<string> PerformOCRAsync(byte[] image, AIModelConfig config)
    {
        var base64Image = Convert.ToBase64String(image);
        var request = new
        {
            model = config.ModelName,
            messages = new[]
            {
                new { role = "user", content = new object[]
                {
                    new { type = "text", text = "Extract all text from this image. Return only the text content." },
                    new { type = "image_url", image_url = new { url = $"data:image/jpeg;base64,{base64Image}" } }
                }}
            }
        };

        _httpClient.DefaultRequestHeaders.Authorization =
            new AuthenticationHeaderValue("Bearer", config.ApiKey);

        var response = await _httpClient.PostAsJsonAsync(
            $"{config.BaseUrl}/chat/completions", request, _jsonOptions);

        response.EnsureSuccessStatusCode();
        var result = await response.Content.ReadFromJsonAsync<OpenAIResponse>(_jsonOptions);
        return result.Choices[0].Message.Content;
    }
}
```

### 4. Presentation Layer (UI)

**Responsibility:** WPF/WinUI 3 views, view models, UI services, navigation.

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| `MainWindow` | Shell window, navigation frame | ViewModels |
| `MainViewModel` | Root ViewModel, navigation state | Application Layer |
| `HomeViewModel` | Dashboard, quick actions | Application Layer |
| `CaptureViewModel` | Camera capture, image preview | Application Layer |
| `QuestionListViewModel` | Browse, filter, search questions | Application Layer |
| `ReviewViewModel` | Review session, answer submission | Application Layer |
| `SubjectManagementViewModel` | Add/edit/delete subjects | Application Layer |
| `SettingsViewModel` | AI model configuration, app settings | Application Layer |
| `INavigationService` | View navigation abstraction | ViewModels |
| `IDialogService` | Dialog/message box abstraction | ViewModels |

**MVVM Pattern Implementation:**
```csharp
// MainViewModel using CommunityToolkit.Mvvm
public partial class MainViewModel : ObservableObject
{
    private readonly INavigationService _navigationService;

    [ObservableProperty]
    private ObservableObject _currentViewModel;

    [ObservableProperty]
    private string _currentPageTitle;

    public MainViewModel(INavigationService navigationService)
    {
        _navigationService = navigationService;
        _navigationService.ViewModelChanged += OnViewModelChanged;
        NavigateToHome();
    }

    [RelayCommand]
    private void NavigateToHome()
    {
        _navigationService.NavigateTo<HomeViewModel>();
    }

    [RelayCommand]
    private void NavigateToCapture()
    {
        _navigationService.NavigateTo<CaptureViewModel>();
    }

    [RelayCommand]
    private void NavigateToReview()
    {
        _navigationService.NavigateTo<ReviewViewModel>();
    }

    private void OnViewModelChanged(object sender, ViewModelChangedEventArgs e)
    {
        CurrentViewModel = e.ViewModel;
        CurrentPageTitle = e.Title;
    }
}

// CaptureViewModel - Camera capture workflow
public partial class CaptureViewModel : ObservableObject
{
    private readonly ICameraService _cameraService;
    private readonly IImageProcessor _imageProcessor;
    private readonly IQuestionService _questionService;
    private readonly IAIService _aiService;
    private readonly ISubjectRepository _subjectRepository;

    [ObservableProperty]
    private BitmapSource _cameraPreview;

    [ObservableProperty]
    private BitmapSource _capturedImage;

    [ObservableProperty]
    private bool _isCameraActive;

    [ObservableProperty]
    private ObservableCollection<Subject> _subjects;

    [ObservableProperty]
    private Subject _selectedSubject;

    [ObservableProperty]
    private string _recognizedText;

    [ObservableProperty]
    private bool _isProcessing;

    public CaptureViewModel(
        ICameraService cameraService,
        IImageProcessor imageProcessor,
        IQuestionService questionService,
        IAIService aiService,
        ISubjectRepository subjectRepository)
    {
        _cameraService = cameraService;
        _imageProcessor = imageProcessor;
        _questionService = questionService;
        _aiService = aiService;
        _subjectRepository = subjectRepository;

        LoadSubjectsAsync();
    }

    [RelayCommand]
    private async Task StartCameraAsync()
    {
        IsCameraActive = true;
        await _cameraService.StartAsync();
        // Subscribe to frame updates for preview
    }

    [RelayCommand]
    private async Task CaptureAsync()
    {
        var imageBytes = await _cameraService.CaptureFrameAsync();
        CapturedImage = ByteArrayToBitmapSource(imageBytes);

        // Process image (crop, enhance, etc.)
        var processedImage = await _imageProcessor.EnhanceAsync(imageBytes);

        // Perform OCR
        IsProcessing = true;
        RecognizedText = await _aiService.PerformOCRAsync(processedImage, GetOCRConfig());
        IsProcessing = false;
    }

    [RelayCommand]
    private async Task SaveQuestionAsync()
    {
        var question = await _questionService.CaptureAndProcessAsync(
            BitmapSourceToByteArray(CapturedImage),
            SelectedSubject.Id);

        // Navigate to question detail or back to home
    }
}
```

## Data Flow

### 1. Question Capture Flow

```
User clicks "Capture"
    ↓
CameraService.CaptureFrameAsync()
    ↓ (returns byte[])
ImageProcessor.EnhanceAsync(image)
    ↓ (returns enhanced byte[])
AIService.PerformOCRAsync(image, config)
    ↓ (returns string)
AIService.AnalyzeKnowledgeAsync(text, subjectId, config)
    ↓ (returns KnowledgeAnalysisResult)
QuestionService.CaptureAndProcessAsync(image, subjectId)
    ↓ (creates WrongQuestion entity)
QuestionRepository.AddAsync(question)
    ↓ (persists to SQLite)
UI updates with success message
```

### 2. Review Session Flow

```
User clicks "Start Review"
    ↓
ReviewService.GetDueReviewsAsync()
    ↓ (queries ReviewSchedule where NextReviewDate <= now)
LearningAlgorithm.SortByPriority(questions)
    ↓ (orders by Leitner box, difficulty, last review)
UI displays question
    ↓
User answers (correct/incorrect)
    ↓
ReviewService.SubmitReviewResultAsync(questionId, isCorrect)
    ↓
LearningAlgorithm.CalculateNextReview(question, isCorrect)
    ↓ (applies Ebbinghaus + Leitner + spaced repetition)
ReviewSchedule updated
    ↓ (new LeitnerBox, NextReviewDate)
QuestionRepository.UpdateAsync(question)
    ↓
UI moves to next question
```

### 3. Knowledge Analysis Flow

```
User captures question image
    ↓
AIService.PerformOCRAsync(image)
    ↓ (extracts text)
AIService.AnalyzeKnowledgeAsync(text, subjectId)
    ↓ (AI identifies knowledge points)
KnowledgePointRepository.GetOrCreateAsync(knowledgePoints)
    ↓ (matches existing or creates new)
Question.KnowledgePoints updated
    ↓
UI shows suggested knowledge points
    ↓
User confirms/edits knowledge points
    ↓
QuestionRepository.UpdateAsync(question)
```

## Patterns to Follow

### Pattern 1: Repository Pattern
**What:** Abstract data access behind interfaces
**When:** All database operations
**Example:**
```csharp
public interface IQuestionRepository
{
    Task<WrongQuestion> GetByIdAsync(Guid id);
    Task<List<WrongQuestion>> GetAllAsync();
    Task<List<WrongQuestion>> GetBySubjectAsync(Guid subjectId);
    Task<List<WrongQuestion>> GetByKnowledgePointAsync(Guid knowledgePointId);
    Task<List<WrongQuestion>> GetDueForReviewAsync();
    Task AddAsync(WrongQuestion question);
    Task UpdateAsync(WrongQuestion question);
    Task DeleteAsync(Guid id);
}

public class QuestionRepository : IQuestionRepository
{
    private readonly AppDbContext _context;

    public QuestionRepository(AppDbContext context)
    {
        _context = context;
    }

    public async Task<List<WrongQuestion>> GetDueForReviewAsync()
    {
        return await _context.Questions
            .Include(q => q.ReviewSchedule)
            .Include(q => q.KnowledgePoints)
            .Where(q => q.ReviewSchedule.NextReviewDate <= DateTime.Now)
            .OrderBy(q => q.ReviewSchedule.LeitnerBox)
            .ToListAsync();
    }
}
```

### Pattern 2: Service Pattern with DI
**What:** Business logic in services, injected via DI container
**When:** All application logic
**Example:**
```csharp
// App.xaml.cs
public partial class App : Application
{
    private readonly IHost _host;

    public App()
    {
        _host = Host.CreateDefaultBuilder()
            .ConfigureServices((context, services) =>
            {
                // Database
                services.AddDbContext<AppDbContext>(options =>
                    options.UseSqlite("Data Source=wronganswer.db"));

                // Repositories
                services.AddScoped<IQuestionRepository, QuestionRepository>();
                services.AddScoped<IKnowledgePointRepository, KnowledgePointRepository>();
                services.AddScoped<ISubjectRepository, SubjectRepository>();

                // Services
                services.AddScoped<IQuestionService, QuestionService>();
                services.AddScoped<IReviewService, ReviewService>();
                services.AddScoped<ILearningAlgorithm, LeitnerAlgorithm>();

                // AI Services
                services.AddHttpClient<IAIService, OpenAIService>();

                // Camera
                services.AddSingleton<ICameraService, AForgeCameraService>();
                services.AddSingleton<IImageProcessor, ImageProcessor>();

                // UI Services
                services.AddSingleton<INavigationService, NavigationService>();
                services.AddSingleton<IDialogService, DialogService>();

                // ViewModels
                services.AddTransient<MainViewModel>();
                services.AddTransient<HomeViewModel>();
                services.AddTransient<CaptureViewModel>();
                services.AddTransient<ReviewViewModel>();
                services.AddTransient<QuestionListViewModel>();
                services.AddTransient<SubjectManagementViewModel>();
                services.AddTransient<SettingsViewModel>();

                // Main Window
                services.AddSingleton<MainWindow>();
            })
            .Build();
    }

    protected override async void OnStartup(StartupEventArgs e)
    {
        await _host.StartAsync();

        // Ensure database is created
        using var scope = _host.Services.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<AppDbContext>();
        await context.Database.EnsureCreatedAsync();

        var mainWindow = _host.Services.GetRequiredService<MainWindow>();
        mainWindow.Show();
    }
}
```

### Pattern 3: DataTemplate-Based Navigation
**What:** WPF DataTemplates automatically resolve Views for ViewModels
**When:** All view navigation
**Example:**
```xml
<!-- MainWindow.xaml -->
<Window x:Class="WrongAnswerBook.MainWindow">
    <DockPanel>
        <!-- Navigation Menu -->
        <StackPanel DockPanel.Dock="Left" Width="200">
            <Button Content="Home" Command="{Binding NavigateToHomeCommand}" />
            <Button Content="Capture" Command="{Binding NavigateToCaptureCommand}" />
            <Button Content="Review" Command="{Binding NavigateToReviewCommand}" />
            <Button Content="Questions" Command="{Binding NavigateToQuestionsCommand}" />
            <Button Content="Subjects" Command="{Binding NavigateToSubjectsCommand}" />
            <Button Content="Settings" Command="{Binding NavigateToSettingsCommand}" />
        </StackPanel>

        <!-- Content Area -->
        <ContentControl Content="{Binding CurrentViewModel}" />
    </DockPanel>
</Window>

<!-- App.xaml - DataTemplates -->
<Application>
    <Application.Resources>
        <DataTemplate DataType="{x:Type vm:HomeViewModel}">
            <views:HomeView />
        </DataTemplate>
        <DataTemplate DataType="{x:Type vm:CaptureViewModel}">
            <views:CaptureView />
        </DataTemplate>
        <DataTemplate DataType="{x:Type vm:ReviewViewModel}">
            <views:ReviewView />
        </DataTemplate>
        <DataTemplate DataType="{x:Type vm:QuestionListViewModel}">
            <views:QuestionListView />
        </DataTemplate>
        <DataTemplate DataType="{x:Type vm:SubjectManagementViewModel}">
            <views:SubjectManagementView />
        </DataTemplate>
        <DataTemplate DataType="{x:Type vm:SettingsViewModel}">
            <views:SettingsView />
        </DataTemplate>
    </Application.Resources>
</Application>
```

### Pattern 4: Async/Award for Long Operations
**What:** Keep UI responsive during AI calls, database operations, camera capture
**When:** All I/O operations
**Example:**
```csharp
public partial class CaptureViewModel : ObservableObject
{
    [ObservableProperty]
    private bool _isProcessing;

    [ObservableProperty]
    private string _statusMessage;

    [RelayCommand]
    private async Task ProcessImageAsync(byte[] imageBytes)
    {
        IsProcessing = true;
        StatusMessage = "Enhancing image...";

        try
        {
            var enhanced = await _imageProcessor.EnhanceAsync(imageBytes);

            StatusMessage = "Recognizing text...";
            var text = await _aiService.PerformOCRAsync(enhanced, GetOCRConfig());

            StatusMessage = "Analyzing knowledge points...";
            var analysis = await _aiService.AnalyzeKnowledgeAsync(text, SelectedSubject.Id, GetAnalysisConfig());

            RecognizedText = text;
            SuggestedKnowledgePoints = analysis.KnowledgePoints;

            StatusMessage = "Ready to save";
        }
        catch (Exception ex)
        {
            StatusMessage = $"Error: {ex.Message}";
            // Log error
        }
        finally
        {
            IsProcessing = false;
        }
    }
}
```

### Pattern 5: Configuration Management
**What:** Store AI model configs, app settings in local config file
**When:** AI model configuration, app preferences
**Example:**
```csharp
public class AppSettings
{
    public AIModelConfig OCRConfig { get; set; } = new();
    public AIModelConfig AnalysisConfig { get; set; } = new();
    public AIModelConfig GenerationConfig { get; set; } = new();
    public string BackupPath { get; set; } = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "WrongAnswerBook",
        "Backups");
    public bool AutoBackup { get; set; } = true;
    public int BackupRetentionDays { get; set; } = 30;
}

public class SettingsService : ISettingsService
{
    private readonly string _settingsPath;
    private AppSettings _settings;

    public SettingsService()
    {
        _settingsPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "WrongAnswerBook",
            "settings.json");
        Load();
    }

    public AppSettings GetSettings() => _settings;

    public async Task SaveAsync()
    {
        var directory = Path.GetDirectoryName(_settingsPath);
        if (!Directory.Exists(directory))
            Directory.CreateDirectory(directory);

        var json = JsonSerializer.Serialize(_settings, new JsonSerializerOptions
        {
            WriteIndented = true
        });
        await File.WriteAllTextAsync(_settingsPath, json);
    }

    private void Load()
    {
        if (File.Exists(_settingsPath))
        {
            var json = File.ReadAllText(_settingsPath);
            _settings = JsonSerializer.Deserialize<AppSettings>(json) ?? new AppSettings();
        }
        else
        {
            _settings = new AppSettings();
        }
    }
}
```

## Anti-Patterns to Avoid

### Anti-Pattern 1: Code-Behind Logic
**What:** Putting business logic in XAML code-behind files
**Why bad:** Untestable, tight coupling, violates MVVM
**Instead:** All logic in ViewModels, bind to commands and properties

### Anti-Pattern 2: Direct Database Access from UI
**What:** Calling SQLite directly from ViewModels
**Why bad:** Violates separation of concerns, untestable
**Instead:** Use repository pattern, inject via DI

### Anti-Pattern 3: Synchronous AI Calls
**What:** Blocking UI thread during AI API calls
**Why bad:** UI freezes, poor user experience
**Instead:** Use async/await, show loading indicators

### Anti-Pattern 4: Hardcoded AI Configurations
**What:** Hardcoding API keys, model names in code
**Why bad:** Insecure, inflexible
**Instead:** Use settings service, per-task configuration

### Anti-Pattern 5: Monolithic ViewModel
**What:** One giant ViewModel with all logic
**Why bad:** Unmaintainable, hard to test
**Instead:** Separate ViewModels per feature, use composition

## Suggested Project Structure

```
Friday/
├── Friday.sln
├── src/
│   ├── Friday.Domain/                    # Core domain (no dependencies)
│   │   ├── Entities/
│   │   │   ├── WrongQuestion.cs
│   │   │   ├── KnowledgePoint.cs
│   │   │   ├── ReviewSchedule.cs
│   │   │   ├── Subject.cs
│   │   │   └── ReviewRecord.cs
│   │   ├── ValueObjects/
│   │   │   ├── DifficultyLevel.cs
│   │   │   └── AIModelConfig.cs
│   │   ├── Enums/
│   │   │   ├── LeitnerBox.cs
│   │   │   └── ReviewResult.cs
│   │   └── Interfaces/
│   │       └── ILearningAlgorithm.cs
│   │
│   ├── Friday.Application/               # Use cases, interfaces
│   │   ├── Interfaces/
│   │   │   ├── IQuestionRepository.cs
│   │   │   ├── IKnowledgePointRepository.cs
│   │   │   ├── ISubjectRepository.cs
│   │   │   ├── IAIService.cs
│   │   │   ├── ICameraService.cs
│   │   │   ├── IImageProcessor.cs
│   │   │   ├── ISettingsService.cs
│   │   │   └── IBackupService.cs
│   │   ├── Services/
│   │   │   ├── QuestionService.cs
│   │   │   ├── ReviewService.cs
│   │   │   └── SubjectService.cs
│   │   ├── DTOs/
│   │   │   ├── KnowledgeAnalysisResult.cs
│   │   │   ├── ReviewStatistics.cs
│   │   │   └── QuestionDto.cs
│   │   └── Mappings/
│   │       └── MappingProfile.cs
│   │
│   ├── Friday.Infrastructure/            # External concerns
│   │   ├── Persistence/
│   │   │   ├── AppDbContext.cs
│   │   │   ├── Repositories/
│   │   │   │   ├── QuestionRepository.cs
│   │   │   │   ├── KnowledgePointRepository.cs
│   │   │   │   └── SubjectRepository.cs
│   │   │   └── Migrations/
│   │   ├── AI/
│   │   │   ├── OpenAIService.cs
│   │   │   ├── BaiduAIService.cs
│   │   │   └── AIServiceFactory.cs
│   │   ├── Camera/
│   │   │   ├── AForgeCameraService.cs
│   │   │   └── ImageProcessor.cs
│   │   ├── Storage/
│   │   │   ├── FileStorageService.cs
│   │   │   └── BackupService.cs
│   │   └── Settings/
│   │       └── SettingsService.cs
│   │
│   └── Friday.UI/                        # WPF/WinUI 3 presentation
│       ├── App.xaml
│       ├── App.xaml.cs
│       ├── MainWindow.xaml
│       ├── MainWindow.xaml.cs
│       ├── ViewModels/
│       │   ├── MainViewModel.cs
│       │   ├── HomeViewModel.cs
│       │   ├── CaptureViewModel.cs
│       │   ├── ReviewViewModel.cs
│       │   ├── QuestionListViewModel.cs
│       │   ├── SubjectManagementViewModel.cs
│       │   └── SettingsViewModel.cs
│       ├── Views/
│       │   ├── HomeView.xaml
│       │   ├── CaptureView.xaml
│       │   ├── ReviewView.xaml
│       │   ├── QuestionListView.xaml
│       │   ├── SubjectManagementView.xaml
│       │   └── SettingsView.xaml
│       ├── Services/
│       │   ├── NavigationService.cs
│       │   └── DialogService.cs
│       ├── Converters/
│       │   └── ByteArrayToImageConverter.cs
│       ├── Resources/
│       │   ├── Styles/
│       │   ├── Themes/
│       │   └── Images/
│       └── Controls/
│           ├── CameraPreview.xaml
│           └── KnowledgePointSelector.xaml
│
├── tests/
│   ├── Friday.Domain.Tests/
│   ├── Friday.Application.Tests/
│   └── Friday.Infrastructure.Tests/
│
└── docs/
    └── architecture.md
```

## Build Order Implications

Based on dependencies, build in this order:

### Phase 1: Foundation (Domain + Application Interfaces)
1. **Friday.Domain** - Entities, value objects, enums
2. **Friday.Application** - Interfaces, DTOs
3. **Friday.Domain.Tests** - Unit tests for domain logic

**Rationale:** Domain has zero dependencies. Application defines interfaces that Infrastructure will implement. This establishes the contract before implementation.

### Phase 2: Infrastructure Core
1. **Friday.Infrastructure/Persistence** - DbContext, repositories
2. **Friday.Infrastructure/Settings** - Configuration management
3. **Friday.Infrastructure.Tests** - Integration tests for repositories

**Rationale:** Database is foundational. All other components need to persist data. Settings service is needed early for AI configuration.

### Phase 3: UI Shell + Basic Views
1. **Friday.UI/App.xaml.cs** - DI container setup
2. **Friday.UI/MainWindow** - Shell with navigation
3. **Friday.UI/Services** - Navigation service
4. **Friday.UI/HomeViewModel** - Dashboard

**Rationale:** Establish the UI framework, navigation pattern, and DI wiring. This enables parallel development of features.

### Phase 4: Core Features
1. **Friday.Infrastructure/AI** - AI service implementations
2. **Friday.UI/CaptureViewModel** - Camera capture workflow
3. **Friday.UI/QuestionListViewModel** - Browse questions
4. **Friday.UI/SubjectManagementViewModel** - Subject management

**Rationale:** Camera and AI are the core value proposition. These depend on Infrastructure (Phase 2) and UI shell (Phase 3).

### Phase 5: Learning Algorithm + Review
1. **Friday.Domain/ILearningAlgorithm** - Algorithm implementation
2. **Friday.Application/ReviewService** - Review orchestration
3. **Friday.UI/ReviewViewModel** - Review session UI

**Rationale:** Learning algorithm is complex but isolated. Can be developed and tested independently. Review UI depends on algorithm + question data.

### Phase 6: Polish + Advanced Features
1. **Friday.Infrastructure/Storage** - Backup service
2. **Friday.UI/SettingsViewModel** - AI configuration UI
3. **Friday.UI/Resources** - Cute cartoon theme
4. **Friday.UI/Controls** - Custom controls (camera preview, knowledge selector)

**Rationale:** Backup, settings, and theming are important but not blocking. Can be done after core features work.

## Scalability Considerations

| Concern | At 100 Questions | At 1,000 Questions | At 10,000 Questions |
|---------|------------------|---------------------|----------------------|
| **SQLite Performance** | Excellent | Good | Needs indexing optimization |
| **AI API Costs** | Minimal | Moderate | Consider caching, batch processing |
| **UI Responsiveness** | Instant | Instant | Virtualization needed for lists |
| **Search Performance** | Fast | Fast | Full-text search indexing |
| **Backup Size** | Small (~10MB) | Moderate (~100MB) | Large (~1GB) - consider compression |

**Optimization Strategies:**
- **Database:** Add indexes on SubjectId, KnowledgePointId, NextReviewDate
- **UI:** Use virtualization for long lists (Questions, Review history)
- **AI:** Cache OCR results, batch similar requests
- **Images:** Store thumbnails separately, lazy load full images
- **Backup:** Incremental backups, compression

## Sources

- Microsoft Documentation: [WPF MVVM Pattern](https://learn.microsoft.com/en-us/dotnet/architecture/maui/mvvm)
- Microsoft Documentation: [Dependency Injection in .NET](https://learn.microsoft.com/en-us/dotnet/core/extensions/dependency-injection)
- Microsoft Documentation: [Entity Framework Core with SQLite](https://learn.microsoft.com/en-us/ef/core/providers/sqlite/?tabs=dotnet-core-cli)
- CommunityToolkit.Mvvm: [GitHub Repository](https://github.com/CommunityToolkit/MVVM)
- AForge.NET: [Computer Vision Framework](http://www.aforgenet.com/framework/)
- Clean Architecture: [Jason Taylor's Template](https://github.com/jasontaylordev/CleanArchitecture)
- Leitner System: [Wikipedia](https://en.wikipedia.org/wiki/Leitner_system)
- Spaced Repetition: [SuperMemo Algorithm](https://supermemo.guru/wiki/SuperMemo_Algorithm)

**Confidence Notes:**
- **HIGH confidence:** MVVM pattern, Repository pattern, DI setup - standard, well-documented patterns
- **MEDIUM confidence:** Camera capture (AForge) - library is older, may have compatibility issues with .NET 8
- **MEDIUM confidence:** AI service integration - depends on specific AI provider APIs, patterns are standard but implementation details vary
- **LOW confidence:** Cute cartoon UI - theming is subjective, no standard approach
