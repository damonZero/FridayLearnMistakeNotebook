namespace Friday.Application.Interfaces;

public record BackupInfo(string FilePath, DateTime CreatedAt, long FileSize);

public interface IBackupService
{
    Task<string> BackupAsync();
    Task<IReadOnlyList<BackupInfo>> GetBackupListAsync();
    Task RestoreFromBackupAsync(string backupPath);
    Task CleanupOldBackupsAsync();
}
