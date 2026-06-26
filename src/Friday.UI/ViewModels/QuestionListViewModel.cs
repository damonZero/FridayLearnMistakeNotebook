using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Friday.Application.Interfaces;
using Friday.Application.Services;
using Friday.Domain.Entities;
using Friday.Domain.Enums;

namespace Friday.UI.ViewModels;

public partial class QuestionListViewModel : ObservableObject
{
    private readonly IQuestionRepository _questionRepository;
    private readonly SubjectService _subjectService;

    [ObservableProperty]
    private ObservableCollection<Question> _questions = new();

    [ObservableProperty]
    private ObservableCollection<Subject> _subjects = new();

    [ObservableProperty]
    private Subject? _selectedSubjectFilter;

    [ObservableProperty]
    private DateTime? _dateFrom;

    [ObservableProperty]
    private DateTime? _dateTo;

    [ObservableProperty]
    private ObservableCollection<ErrorType> _errorTypes = new(Enum.GetValues<ErrorType>());

    [ObservableProperty]
    private ErrorType? _selectedErrorTypeFilter;

    [ObservableProperty]
    private string _sortBy = "Date";

    public QuestionListViewModel(
        IQuestionRepository questionRepository,
        SubjectService subjectService)
    {
        _questionRepository = questionRepository;
        _subjectService = subjectService;
        _ = InitializeAsync();
    }

    private async Task InitializeAsync()
    {
        var subjects = await _subjectService.GetAllAsync();
        Subjects = new ObservableCollection<Subject>(subjects);
        await LoadQuestionsAsync();
    }

    private async Task LoadQuestionsAsync()
    {
        var questions = await _questionRepository.GetFilteredAsync(
            SelectedSubjectFilter?.Id,
            DateFrom,
            DateTo,
            SelectedErrorTypeFilter,
            SortBy);
        Questions = new ObservableCollection<Question>(questions);
    }

    [RelayCommand]
    private async Task ApplyFiltersAsync()
    {
        await LoadQuestionsAsync();
    }

    [RelayCommand]
    private async Task ClearFiltersAsync()
    {
        SelectedSubjectFilter = null;
        DateFrom = null;
        DateTo = null;
        SelectedErrorTypeFilter = null;
        SortBy = "Date";
        await LoadQuestionsAsync();
    }

    [RelayCommand]
    private async Task SortByDateAsync()
    {
        SortBy = "Date";
        await LoadQuestionsAsync();
    }

    [RelayCommand]
    private async Task SortByReviewCountAsync()
    {
        SortBy = "ReviewCount";
        await LoadQuestionsAsync();
    }

    [RelayCommand]
    private async Task SortByMasteryAsync()
    {
        SortBy = "Mastery";
        await LoadQuestionsAsync();
    }
}
