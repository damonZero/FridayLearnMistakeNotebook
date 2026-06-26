using Friday.Application.Interfaces;
using Friday.Application.Services;
using Friday.Infrastructure.Persistence;
using Friday.Infrastructure.Persistence.Repositories;
using Friday.Infrastructure.Services;
using Friday.Mobile.Pages;
using Friday.Mobile.Services;
using Friday.Mobile.ViewModels;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Serilog;
using MauiApp = Microsoft.Maui.Controls.Application;

namespace Friday.Mobile;

public partial class App : MauiApp
{
    private readonly IServiceProvider _serviceProvider;

    public App()
    {
        var services = new ServiceCollection();
        ConfigureServices(services);
        _serviceProvider = services.BuildServiceProvider();

        InitializeComponent();
    }

    private void ConfigureServices(IServiceCollection services)
    {
        // Configure Serilog
        var logPath = Path.Combine(
            FileSystem.AppDataDirectory,
            "logs", "log-.txt");

        Log.Logger = new LoggerConfiguration()
            .WriteTo.File(logPath, rollingInterval: RollingInterval.Day)
            .CreateLogger();

        // Database
        var dbPath = Path.Combine(FileSystem.AppDataDirectory, "app.db");

        services.AddDbContext<AppDbContext>(options =>
            options.UseSqlite($"Data Source={dbPath}"));

        // Repositories
        services.AddScoped<ISubjectRepository, SubjectRepository>();
        services.AddScoped<IChapterRepository, ChapterRepository>();
        services.AddScoped<IKnowledgePointRepository, KnowledgePointRepository>();
        services.AddScoped<IQuestionRepository, QuestionRepository>();

        // Application Services
        services.AddScoped<SubjectService>();
        services.AddScoped<KnowledgeTreeService>();

        // Infrastructure Services
        services.AddScoped<IExportImportService, ExportImportService>();
        services.AddSingleton<IBackupService>(sp =>
        {
            var backupPath = Path.Combine(FileSystem.AppDataDirectory, "Backups");
            return new BackupService(sp.GetRequiredService<AppDbContext>(), backupPath);
        });
        services.AddScoped<IAiService, AiService>();
        services.AddScoped<IAiConfigService, AiConfigService>();

        // Navigation Service
        services.AddSingleton<NavigationService>();

        // ViewModels
        services.AddTransient<MainViewModel>();
        services.AddTransient<HomeViewModel>();
        services.AddTransient<SubjectListViewModel>();
        services.AddTransient<KnowledgeTreeViewModel>();
        services.AddTransient<QuestionListViewModel>();
        services.AddTransient<AddQuestionViewModel>();
        services.AddTransient<SettingsViewModel>();
        services.AddTransient<AiConfigViewModel>();

        // Pages
        services.AddTransient<MainPage>();
        services.AddTransient<HomePage>();
        services.AddTransient<SubjectListPage>();
        services.AddTransient<KnowledgeTreePage>();
        services.AddTransient<QuestionListPage>();
        services.AddTransient<AddQuestionPage>();
        services.AddTransient<SettingsPage>();
        services.AddTransient<AiConfigPage>();
    }

    protected override Window CreateWindow(IActivationState? activationState)
    {
        var mainPage = _serviceProvider.GetRequiredService<MainPage>();
        return new Window(new NavigationPage(mainPage));
    }

    public IServiceProvider Services => _serviceProvider;
}
