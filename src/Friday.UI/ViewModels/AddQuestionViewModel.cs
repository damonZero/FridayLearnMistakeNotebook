using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Friday.Application.Interfaces;
using Friday.Domain.Entities;
using Friday.Domain.Enums;

namespace Friday.UI.ViewModels;

public partial class AddQuestionViewModel : ObservableObject
{
    private readonly IQuestionRepository _questionRepository;
    private readonly ISubjectRepository _subjectRepository;

    [ObservableProperty]
    private string _content = string.Empty;

    [ObservableProperty]
    private string _answer = string.Empty;

    [ObservableProperty]
    private string _userAnswer = string.Empty;

    [ObservableProperty]
    private string _notes = string.Empty;

    [ObservableProperty]
    private ObservableCollection<Subject> _subjects = new();

    [ObservableProperty]
    private Subject? _selectedSubject;

    [ObservableProperty]
    private ObservableCollection<ErrorType> _errorTypes = new(Enum.GetValues<ErrorType>());

    [ObservableProperty]
    private ErrorType _selectedErrorType = ErrorType.Unknown;

    [ObservableProperty]
    private string _errorMessage = string.Empty;

    [ObservableProperty]
    private string _successMessage = string.Empty;

    public AddQuestionViewModel(
        IQuestionRepository questionRepository,
        ISubjectRepository subjectRepository)
    {
        _questionRepository = questionRepository;
        _subjectRepository = subjectRepository;
        _ = LoadSubjectsAsync();
    }

    private async Task LoadSubjectsAsync()
    {
        var subjects = await _subjectRepository.GetAllAsync();
        Subjects = new ObservableCollection<Subject>(subjects);
    }

    [RelayCommand]
    private async Task SaveQuestionAsync()
    {
        ErrorMessage = string.Empty;
        SuccessMessage = string.Empty;

        if (string.IsNullOrWhiteSpace(Content))
        {
            ErrorMessage = "请输入题目内容";
            return;
        }

        if (string.IsNullOrWhiteSpace(Answer))
        {
            ErrorMessage = "请输入正确答案";
            return;
        }

        if (SelectedSubject == null)
        {
            ErrorMessage = "请选择科目";
            return;
        }

        var question = new Question
        {
            Content = Content.Trim(),
            Answer = Answer.Trim(),
            UserAnswer = UserAnswer?.Trim() ?? string.Empty,
            Notes = Notes?.Trim() ?? string.Empty,
            ErrorType = SelectedErrorType,
            SubjectId = SelectedSubject.Id,
            LeitnerBox = 1,
            EaseFactor = 2.5,
            IntervalDays = 1,
            Streak = 0,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow,
            ReviewDate = DateTime.UtcNow
        };

        await _questionRepository.AddAsync(question);

        // Reset form
        Content = string.Empty;
        Answer = string.Empty;
        UserAnswer = string.Empty;
        Notes = string.Empty;
        SelectedErrorType = ErrorType.Unknown;
        ErrorMessage = string.Empty;
        SuccessMessage = "错题已保存";
    }

    [RelayCommand]
    private void ClearForm()
    {
        Content = string.Empty;
        Answer = string.Empty;
        UserAnswer = string.Empty;
        Notes = string.Empty;
        SelectedSubject = null;
        SelectedErrorType = ErrorType.Unknown;
        ErrorMessage = string.Empty;
        SuccessMessage = string.Empty;
    }
}
