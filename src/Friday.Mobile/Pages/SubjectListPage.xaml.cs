using Friday.Mobile.ViewModels;

namespace Friday.Mobile.Pages;

public partial class SubjectListPage : ContentPage
{
    private readonly SubjectListViewModel _viewModel;

    public SubjectListPage(SubjectListViewModel viewModel)
    {
        InitializeComponent();
        _viewModel = viewModel;
        BindingContext = _viewModel;
    }

    protected override async void OnAppearing()
    {
        base.OnAppearing();
        await _viewModel.LoadSubjectsCommand.ExecuteAsync(null);
    }
}
