using System.IO;
using System.Windows;
using Friday.Application.Interfaces;
using Friday.Application.Services;
using Friday.Infrastructure.Persistence;
using Friday.Infrastructure.Persistence.Repositories;
using Friday.Infrastructure.Services;
using Friday.UI.Services;
using Friday.UI.ViewModels;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Serilog;

namespace Friday.UI;

public partial class App : System.Windows.Application
{
    private readonly ServiceProvider _serviceProvider;

    public App()
    {
        var services = new ServiceCollection();
        ConfigureServices(services);
        _serviceProvider = services.BuildServiceProvider();
    }

    private void ConfigureServices(IServiceCollection services)
    {
        // Configure Serilog
        var logPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "SmartWrongAnswerBook", "logs", "log-.txt");

        Log.Logger = new LoggerConfiguration()
            .WriteTo.File(logPath, rollingInterval: RollingInterval.Day)
            .CreateLogger();

        // Database
        var dbPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "SmartWrongAnswerBook", "app.db");

        var dbDir = Path.GetDirectoryName(dbPath)!;
        if (!Directory.Exists(dbDir))
            Directory.CreateDirectory(dbDir);

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
            var backupPath = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                "SmartWrongAnswerBook", "Backups");
            return new BackupService(sp.GetRequiredService<AppDbContext>(), backupPath);
        });

        // Navigation Service
        services.AddSingleton<NavigationService>();

        // ViewModels
        services.AddTransient<MainViewModel>();
        services.AddTransient<HomeViewModel>();
        services.AddTransient<SubjectListViewModel>();
        services.AddTransient<SubjectManagementViewModel>();
        services.AddTransient<KnowledgeTreeViewModel>();
        services.AddTransient<QuestionListViewModel>();
        services.AddTransient<AddQuestionViewModel>();
        services.AddTransient<SettingsViewModel>();
        services.AddTransient<ExportImportViewModel>();

        // Main Window
        services.AddSingleton<MainWindow>();
    }

    protected override async void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);

        // Ensure database is created and seeded
        using var scope = _serviceProvider.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<AppDbContext>();
        await context.Database.EnsureCreatedAsync();
        context.ConfigureSqlite();
        await SubjectRepository.SeedPresetSubjectsAsync(context);

        // Ensure backup directory exists
        var backupPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "SmartWrongAnswerBook", "Backups");
        if (!Directory.Exists(backupPath))
            Directory.CreateDirectory(backupPath);

        var mainWindow = _serviceProvider.GetRequiredService<MainWindow>();
        mainWindow.Show();
    }

    protected override async void OnExit(ExitEventArgs e)
    {
        // Auto-backup on app close per D-15
        try
        {
            using var scope = _serviceProvider.CreateScope();
            var backupService = scope.ServiceProvider.GetRequiredService<IBackupService>();
            await backupService.BackupAsync();
            await backupService.CleanupOldBackupsAsync();
        }
        catch (Exception ex)
        {
            Log.Error(ex, "Failed to create backup on app exit");
        }

        Log.CloseAndFlush();
        base.OnExit(e);
    }
}
