using CommunityToolkit.Mvvm.ComponentModel;

namespace Friday.Mobile.ViewModels;

public partial class QuestionListViewModel : ObservableObject
{
    [ObservableProperty]
    private string _title = "错题列表";
}
