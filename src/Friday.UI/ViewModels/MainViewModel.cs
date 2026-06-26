using CommunityToolkit.Mvvm.ComponentModel;
using Friday.UI.Services;

namespace Friday.UI.ViewModels;

public partial class MainViewModel : ObservableObject
{
    private readonly NavigationService _navigationService;

    [ObservableProperty]
    private ObservableObject? _currentViewModel;

    [ObservableProperty]
    private string _currentPageTitle = "仪表盘";

    [ObservableProperty]
    private int _selectedNavigationIndex;

    public MainViewModel(NavigationService navigationService)
    {
        _navigationService = navigationService;
        _navigationService.ViewModelChanged += OnViewModelChanged;

        // Navigate to Home on init
        _navigationService.NavigateTo<HomeViewModel>("仪表盘");
        SelectedNavigationIndex = 0;
    }

    private void OnViewModelChanged(object? sender, (ObservableObject ViewModel, string Title) e)
    {
        CurrentViewModel = e.ViewModel;
        CurrentPageTitle = e.Title;
    }

    public void NavigateToHome()
    {
        _navigationService.NavigateTo<HomeViewModel>("仪表盘");
        SelectedNavigationIndex = 0;
    }

    public void NavigateToSubjects()
    {
        _navigationService.NavigateTo<SubjectManagementViewModel>("科目管理");
        SelectedNavigationIndex = 1;
    }

    public void NavigateToQuestions()
    {
        _navigationService.NavigateTo<QuestionListViewModel>("错题列表");
        SelectedNavigationIndex = 2;
    }

    public void NavigateToReview()
    {
        // Phase 3: Review engine
        SelectedNavigationIndex = 3;
    }

    public void NavigateToSettings()
    {
        _navigationService.NavigateTo<SettingsViewModel>("设置");
        SelectedNavigationIndex = 4;
    }

    public void NavigateToExportImport()
    {
        _navigationService.NavigateTo<ExportImportViewModel>("数据管理");
        SelectedNavigationIndex = 4; // Keep settings highlighted
    }
}
