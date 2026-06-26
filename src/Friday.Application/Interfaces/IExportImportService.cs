namespace Friday.Application.Interfaces;

public record ExportSummary(int Subjects, int Chapters, int KnowledgePoints, int Questions);

public interface IExportImportService
{
    Task<string> ExportToJsonAsync();
    Task<string> ExportToCsvAsync();
    Task<ExportSummary> ImportFromJsonAsync(string json);
    Task<ExportSummary> GetExportSummaryAsync();
}
