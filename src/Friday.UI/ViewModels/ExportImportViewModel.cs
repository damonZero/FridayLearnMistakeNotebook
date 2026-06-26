using System.IO;
using System.Windows;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Friday.Application.Interfaces;
using Microsoft.Win32;

namespace Friday.UI.ViewModels;

public partial class ExportImportViewModel : ObservableObject
{
    private readonly IExportImportService _exportImportService;

    [ObservableProperty]
    private string _exportStatus = string.Empty;

    [ObservableProperty]
    private string _importStatus = string.Empty;

    [ObservableProperty]
    private ExportSummary? _summary;

    public ExportImportViewModel(IExportImportService exportImportService)
    {
        _exportImportService = exportImportService;
        _ = LoadSummaryAsync();
    }

    [RelayCommand]
    private async Task ExportToJsonAsync()
    {
        try
        {
            ExportStatus = "正在导出...";
            var json = await _exportImportService.ExportToJsonAsync();

            var dialog = new SaveFileDialog
            {
                Filter = "JSON 文件|*.json",
                FileName = $"错题本备份_{DateTime.UtcNow:yyyyMMdd_HHmmss}.json"
            };

            if (dialog.ShowDialog() == true)
            {
                await File.WriteAllTextAsync(dialog.FileName, json);
                ExportStatus = $"导出成功! 文件已保存到: {dialog.FileName}";
            }
            else
            {
                ExportStatus = "导出已取消";
            }
        }
        catch (Exception ex)
        {
            ExportStatus = $"导出失败: {ex.Message}";
        }
    }

    [RelayCommand]
    private async Task ExportToCsvAsync()
    {
        try
        {
            ExportStatus = "正在导出 CSV...";
            var csv = await _exportImportService.ExportToCsvAsync();

            var dialog = new SaveFileDialog
            {
                Filter = "CSV 文件|*.csv",
                FileName = $"错题本_{DateTime.UtcNow:yyyyMMdd_HHmmss}.csv"
            };

            if (dialog.ShowDialog() == true)
            {
                await File.WriteAllTextAsync(dialog.FileName, csv, System.Text.Encoding.UTF8);
                ExportStatus = $"导出成功! 文件已保存到: {dialog.FileName}";
            }
            else
            {
                ExportStatus = "导出已取消";
            }
        }
        catch (Exception ex)
        {
            ExportStatus = $"导出失败: {ex.Message}";
        }
    }

    [RelayCommand]
    private async Task ImportFromJsonAsync()
    {
        try
        {
            var warningResult = MessageBox.Show(
                "导入将覆盖现有数据，确定要继续吗？",
                "确认导入",
                MessageBoxButton.YesNo,
                MessageBoxImage.Warning);

            if (warningResult != MessageBoxResult.Yes)
            {
                ImportStatus = "导入已取消";
                return;
            }

            var dialog = new OpenFileDialog
            {
                Filter = "JSON 文件|*.json"
            };

            if (dialog.ShowDialog() != true)
            {
                ImportStatus = "导入已取消";
                return;
            }

            ImportStatus = "正在导入...";
            var json = await File.ReadAllTextAsync(dialog.FileName);

            // Validate file size (T-03-03: mitigate DoS from large files)
            if (json.Length > 100 * 1024 * 1024) // 100MB limit
            {
                ImportStatus = "导入失败: 文件过大 (超过 100MB)";
                return;
            }

            var result = await _exportImportService.ImportFromJsonAsync(json);
            ImportStatus = $"导入成功! 已导入: {result.Subjects} 科目, {result.Chapters} 章节, {result.KnowledgePoints} 知识点, {result.Questions} 错题";

            // Refresh summary
            await LoadSummaryAsync();
        }
        catch (InvalidOperationException ex)
        {
            ImportStatus = $"导入失败: {ex.Message}";
        }
        catch (Exception ex)
        {
            ImportStatus = $"导入失败: {ex.Message}";
        }
    }

    [RelayCommand]
    private async Task LoadSummaryAsync()
    {
        try
        {
            Summary = await _exportImportService.GetExportSummaryAsync();
        }
        catch
        {
            // Ignore errors on summary load
        }
    }
}
