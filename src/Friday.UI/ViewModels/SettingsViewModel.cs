using System.Reflection;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Friday.UI.Services;

namespace Friday.UI.ViewModels;

public partial class SettingsViewModel : ObservableObject
{
    private readonly NavigationService _navigationService;

    [ObservableProperty]
    private string _appVersion = string.Empty;

    [ObservableProperty]
    private string _dataPath = string.Empty;

    public SettingsViewModel(NavigationService navigationService)
    {
        _navigationService = navigationService;
        AppVersion = Assembly.GetExecutingAssembly().GetName().Version?.ToString() ?? "1.0.0";
        DataPath = System.IO.Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "SmartWrongAnswerBook");
    }

    [RelayCommand]
    private void NavigateToExportImport()
    {
        _navigationService.NavigateTo<ExportImportViewModel>("数据管理");
    }
}
