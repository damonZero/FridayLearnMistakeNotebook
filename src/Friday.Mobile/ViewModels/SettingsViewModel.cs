using System.Reflection;
using CommunityToolkit.Mvvm.ComponentModel;

namespace Friday.Mobile.ViewModels;

public partial class SettingsViewModel : ObservableObject
{
    [ObservableProperty]
    private string _appVersion = string.Empty;

    [ObservableProperty]
    private string _dataPath = string.Empty;

    public SettingsViewModel()
    {
        AppVersion = Assembly.GetExecutingAssembly().GetName().Version?.ToString() ?? "1.0.0";
        DataPath = FileSystem.AppDataDirectory;
    }
}
