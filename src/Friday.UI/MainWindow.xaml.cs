using System.Windows;
using Friday.UI.ViewModels;
using Microsoft.Extensions.DependencyInjection;

namespace Friday.UI;

public partial class MainWindow : Window
{
    private readonly MainViewModel _viewModel;
    private readonly IServiceProvider _serviceProvider;

    public MainWindow(MainViewModel viewModel, IServiceProvider serviceProvider)
    {
        InitializeComponent();
        _viewModel = viewModel;
        _serviceProvider = serviceProvider;
        DataContext = _viewModel;
    }

    private void Home_Click(object sender, RoutedEventArgs e)
    {
        // Coming soon
    }

    private void Subjects_Click(object sender, RoutedEventArgs e)
    {
        var subjectListViewModel = _serviceProvider.GetRequiredService<SubjectListViewModel>();
        _viewModel.CurrentViewModel = subjectListViewModel;
    }

    private void Questions_Click(object sender, RoutedEventArgs e)
    {
        // Coming soon
    }

    private void Review_Click(object sender, RoutedEventArgs e)
    {
        // Coming soon
    }

    private void Settings_Click(object sender, RoutedEventArgs e)
    {
        // Coming soon
    }
}
