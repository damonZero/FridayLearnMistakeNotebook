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
        var subjectManagementViewModel = _serviceProvider.GetRequiredService<SubjectManagementViewModel>();
        _viewModel.CurrentViewModel = subjectManagementViewModel;
    }

    private void KnowledgeTree_Click(object sender, RoutedEventArgs e)
    {
        var knowledgeTreeViewModel = _serviceProvider.GetRequiredService<KnowledgeTreeViewModel>();
        _viewModel.CurrentViewModel = knowledgeTreeViewModel;
    }

    private void Questions_Click(object sender, RoutedEventArgs e)
    {
        var questionListViewModel = _serviceProvider.GetRequiredService<QuestionListViewModel>();
        _viewModel.CurrentViewModel = questionListViewModel;
    }

    private void AddQuestion_Click(object sender, RoutedEventArgs e)
    {
        var addQuestionViewModel = _serviceProvider.GetRequiredService<AddQuestionViewModel>();
        _viewModel.CurrentViewModel = addQuestionViewModel;
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
