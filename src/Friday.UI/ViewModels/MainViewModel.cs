using CommunityToolkit.Mvvm.ComponentModel;

namespace Friday.UI.ViewModels;

public partial class MainViewModel : ObservableObject
{
    [ObservableProperty]
    private ObservableObject? _currentViewModel;
}
