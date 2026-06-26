using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Friday.Application.Interfaces;
using Friday.Infrastructure.Persistence;

namespace Friday.Mobile.ViewModels;

public partial class HomeViewModel : ObservableObject
{
    private readonly AppDbContext _dbContext;

    [ObservableProperty]
    private int _totalSubjects;

    [ObservableProperty]
    private int _totalQuestions;

    [ObservableProperty]
    private int _dueReviews;

    [ObservableProperty]
    private string _greeting = string.Empty;

    public HomeViewModel(AppDbContext dbContext)
    {
        _dbContext = dbContext;
        UpdateGreeting();
    }

    [RelayCommand]
    private async Task LoadDataAsync()
    {
        TotalSubjects = _dbContext.Subjects.Count();
        TotalQuestions = _dbContext.Questions.Count();
        DueReviews = _dbContext.Questions.Count(q => q.ReviewDate <= DateTime.UtcNow);
    }

    private void UpdateGreeting()
    {
        var hour = DateTime.Now.Hour;
        Greeting = hour switch
        {
            < 6 => "🌙 夜深了，注意休息",
            < 12 => "🌅 早上好，开始学习吧",
            < 14 => "☀️ 中午好，适当休息",
            < 18 => "🌤️ 下午好，继续加油",
            < 22 => "🌆 晚上好，复习时间到",
            _ => "🌙 夜深了，注意休息"
        };
    }
}
