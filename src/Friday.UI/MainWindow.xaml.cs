using System.Windows;
using Friday.UI.ViewModels;

namespace Friday.UI;

public partial class MainWindow : Window
{
    private readonly MainViewModel _viewModel;

    public MainWindow(MainViewModel viewModel)
    {
        InitializeComponent();
        _viewModel = viewModel;
        DataContext = _viewModel;
    }

    private void NavHome_Click(object sender, RoutedEventArgs e)
    {
        _viewModel.NavigateToHome();
    }

    private void NavSubjects_Click(object sender, RoutedEventArgs e)
    {
        _viewModel.NavigateToSubjects();
    }

    private void NavQuestions_Click(object sender, RoutedEventArgs e)
    {
        _viewModel.NavigateToQuestions();
    }

    private void NavReview_Click(object sender, RoutedEventArgs e)
    {
        _viewModel.NavigateToReview();
    }

    private void NavSettings_Click(object sender, RoutedEventArgs e)
    {
        _viewModel.NavigateToSettings();
    }
}
