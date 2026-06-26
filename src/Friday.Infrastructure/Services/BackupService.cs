using Friday.Application.Interfaces;
using Friday.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace Friday.Infrastructure.Services;

public class BackupService : IBackupService
{
    private readonly AppDbContext _context;
    private readonly string _backupPath;
    private const int MaxBackups = 7;

    public BackupService(AppDbContext context, string backupPath)
    {
        _context = context;
        _backupPath = backupPath;

        if (!Directory.Exists(_backupPath))
            Directory.CreateDirectory(_backupPath);
    }

    public async Task<string> BackupAsync()
    {
        var timestamp = DateTime.UtcNow.ToString("yyyyMMdd_HHmmss");
        var backupFile = Path.Combine(_backupPath, $"backup_{timestamp}.db");

        // Use SQLite VACUUM INTO for atomic backup per D-17
        var connection = _context.Database.GetDbConnection();
        await connection.OpenAsync();
        using (var command = connection.CreateCommand())
        {
            command.CommandText = $"VACUUM INTO '{backupFile.Replace("'", "''")}'";
            await command.ExecuteNonQueryAsync();
        }
        await connection.CloseAsync();

        return backupFile;
    }

    public Task<IReadOnlyList<BackupInfo>> GetBackupListAsync()
    {
        if (!Directory.Exists(_backupPath))
            return Task.FromResult<IReadOnlyList<BackupInfo>>(Array.Empty<BackupInfo>());

        var backups = Directory.GetFiles(_backupPath, "backup_*.db")
            .Select(f => new FileInfo(f))
            .OrderByDescending(f => f.Name)
            .Select(f => new BackupInfo(f.FullName, f.CreationTimeUtc, f.Length))
            .ToList();

        return Task.FromResult<IReadOnlyList<BackupInfo>>(backups);
    }

    public async Task RestoreFromBackupAsync(string backupPath)
    {
        if (!File.Exists(backupPath))
            throw new FileNotFoundException("Backup file not found", backupPath);

        // Get current database path from connection string
        var connectionString = _context.Database.GetConnectionString();
        var dataSourcePrefix = "Data Source=";
        if (connectionString == null || !connectionString.Contains(dataSourcePrefix))
            throw new InvalidOperationException("Could not determine database path from connection string");

        var dbPath = connectionString
            .Split(';', StringSplitOptions.RemoveEmptyEntries)
            .FirstOrDefault(s => s.Trim().StartsWith(dataSourcePrefix, StringComparison.OrdinalIgnoreCase))
            ?.Substring(dataSourcePrefix.Length).Trim();

        if (string.IsNullOrEmpty(dbPath))
            throw new InvalidOperationException("Could not parse database path from connection string");

        // Close current connection
        await _context.Database.CloseConnectionAsync();

        // Copy backup over current database
        File.Copy(backupPath, dbPath, overwrite: true);
    }

    public Task CleanupOldBackupsAsync()
    {
        if (!Directory.Exists(_backupPath))
            return Task.CompletedTask;

        var backupFiles = Directory.GetFiles(_backupPath, "backup_*.db")
            .OrderBy(f => f) // filenames contain timestamp, so alphabetical = chronological
            .ToList();

        // Keep newest MaxBackups, delete the rest per D-16
        if (backupFiles.Count > MaxBackups)
        {
            var filesToDelete = backupFiles.Take(backupFiles.Count - MaxBackups);
            foreach (var file in filesToDelete)
            {
                try
                {
                    File.Delete(file);
                }
                catch
                {
                    // Best effort - don't fail if a file can't be deleted
                }
            }
        }

        return Task.CompletedTask;
    }
}
