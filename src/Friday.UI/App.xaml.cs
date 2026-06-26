using System.IO;
using System.Windows;
using Friday.Application.Interfaces;
using Friday.Application.Services;
using Friday.Infrastructure.Persistence;
using Friday.Infrastructure.Persistence.Repositories;
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

        // Services
        services.AddScoped<SubjectService>();
        services.AddScoped<KnowledgeTreeService>();

        // ViewModels
        services.AddTransient<MainViewModel>();
        services.AddTransient<SubjectListViewModel>();

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

        var mainWindow = _serviceProvider.GetRequiredService<MainWindow>();
        mainWindow.Show();
    }

    protected override void OnExit(ExitEventArgs e)
    {
        Log.CloseAndFlush();
        base.OnExit(e);
    }
}
