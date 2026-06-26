using Friday.Infrastructure.Persistence;
using Friday.Infrastructure.Services;
using Microsoft.EntityFrameworkCore;
using Xunit;

namespace Friday.Tests.Infrastructure;

public class BackupServiceTests : IDisposable
{
    private readonly string _dbPath;
    private readonly string _backupPath;
    private readonly AppDbContext _context;

    public BackupServiceTests()
    {
        _dbPath = Path.Combine(Path.GetTempPath(), $"test_{Guid.NewGuid():N}.db");
        _backupPath = Path.Combine(Path.GetTempPath(), $"backups_{Guid.NewGuid():N}");

        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseSqlite($"Data Source={_dbPath}")
            .Options;

        _context = new AppDbContext(options);
        _context.Database.EnsureCreated();
        _context.ConfigureSqlite();
    }

    [Fact]
    public async Task BackupAsync_CreatesBackupFile()
    {
        // Arrange
        var service = new BackupService(_context, _backupPath);

        // Act
        var backupFile = await service.BackupAsync();

        // Assert
        Assert.True(File.Exists(backupFile));
        Assert.True(new FileInfo(backupFile).Length > 0);
        Assert.Contains("backup_", Path.GetFileName(backupFile));
        Assert.EndsWith(".db", backupFile);
    }

    [Fact]
    public async Task CleanupOldBackupsAsync_KeepsOnly7Files()
    {
        // Arrange: create 10 fake backup files
        Directory.CreateDirectory(_backupPath);
        for (int i = 0; i < 10; i++)
        {
            var timestamp = DateTime.UtcNow.AddMinutes(-i).ToString("yyyyMMdd_HHmmss");
            var filePath = Path.Combine(_backupPath, $"backup_{timestamp}.db");
            await File.WriteAllTextAsync(filePath, $"backup {i}");
        }

        Assert.Equal(10, Directory.GetFiles(_backupPath, "backup_*.db").Length);

        var service = new BackupService(_context, _backupPath);

        // Act
        await service.CleanupOldBackupsAsync();

        // Assert: should have exactly 7 files
        var remaining = Directory.GetFiles(_backupPath, "backup_*.db");
        Assert.Equal(7, remaining.Length);
    }

    [Fact]
    public async Task GetBackupListAsync_ReturnsFilesOrderedByDateDescending()
    {
        // Arrange: create backup files
        Directory.CreateDirectory(_backupPath);
        for (int i = 0; i < 3; i++)
        {
            var timestamp = DateTime.UtcNow.AddMinutes(-i * 10).ToString("yyyyMMdd_HHmmss");
            var filePath = Path.Combine(_backupPath, $"backup_{timestamp}.db");
            await File.WriteAllTextAsync(filePath, $"backup {i}");
        }

        var service = new BackupService(_context, _backupPath);

        // Act
        var backups = await service.GetBackupListAsync();

        // Assert
        Assert.Equal(3, backups.Count);
        // Files should be ordered by name descending (newest timestamp first)
        Assert.True(string.Compare(backups[0].FilePath, backups[1].FilePath, StringComparison.Ordinal) > 0);
        Assert.True(string.Compare(backups[1].FilePath, backups[2].FilePath, StringComparison.Ordinal) > 0);
    }

    [Fact]
    public async Task CleanupOldBackupsAsync_DoesNothing_WhenLessThan7()
    {
        // Arrange: create 3 backup files
        Directory.CreateDirectory(_backupPath);
        for (int i = 0; i < 3; i++)
        {
            var timestamp = DateTime.UtcNow.AddMinutes(-i).ToString("yyyyMMdd_HHmmss");
            var filePath = Path.Combine(_backupPath, $"backup_{timestamp}.db");
            await File.WriteAllTextAsync(filePath, $"backup {i}");
        }

        var service = new BackupService(_context, _backupPath);

        // Act
        await service.CleanupOldBackupsAsync();

        // Assert: all 3 should remain
        Assert.Equal(3, Directory.GetFiles(_backupPath, "backup_*.db").Length);
    }

    public void Dispose()
    {
        _context.Database.CloseConnection();
        _context.Dispose();

        // Cleanup temp files
        try
        {
            if (File.Exists(_dbPath)) File.Delete(_dbPath);
            if (Directory.Exists(_backupPath)) Directory.Delete(_backupPath, true);
        }
        catch { /* best effort */ }
    }
}
