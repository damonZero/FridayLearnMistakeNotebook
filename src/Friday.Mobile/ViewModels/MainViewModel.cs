using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;

namespace Friday.Mobile.ViewModels;

public partial class MainViewModel : ObservableObject
{
    [ObservableProperty]
    private string _title = "智能错题本";

    [ObservableProperty]
    private int _selectedTabIndex;

    [RelayCommand]
    private async Task NavigateToHome()
    {
        // Navigation handled by Shell
    }

    [RelayCommand]
    private async Task NavigateToSubjects()
    {
        // Navigation handled by Shell
    }

    [RelayCommand]
    private async Task NavigateToQuestions()
    {
        // Navigation handled by Shell
    }

    [RelayCommand]
    private async Task NavigateToSettings()
    {
        // Navigation handled by Shell
    }
}
