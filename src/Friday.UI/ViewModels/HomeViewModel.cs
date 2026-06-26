using CommunityToolkit.Mvvm.ComponentModel;
using Friday.Application.Interfaces;

namespace Friday.UI.ViewModels;

public partial class HomeViewModel : ObservableObject
{
    private readonly ISubjectRepository _subjectRepository;
    private readonly IQuestionRepository _questionRepository;

    [ObservableProperty]
    private int _totalSubjects;

    [ObservableProperty]
    private int _totalQuestions;

    [ObservableProperty]
    private int _dueReviews;

    public HomeViewModel(ISubjectRepository subjectRepository, IQuestionRepository questionRepository)
    {
        _subjectRepository = subjectRepository;
        _questionRepository = questionRepository;
        _ = LoadStatsAsync();
    }

    private async Task LoadStatsAsync()
    {
        var subjects = await _subjectRepository.GetAllAsync();
        TotalSubjects = subjects.Count();

        var questions = await _questionRepository.GetAllAsync();
        TotalQuestions = questions.Count();

        // Count questions due for review (ReviewDate <= today)
        DueReviews = questions.Count(q => q.ReviewDate.Date <= DateTime.UtcNow.Date);
    }
}
