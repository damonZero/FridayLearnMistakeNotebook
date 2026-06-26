using CommunityToolkit.Mvvm.ComponentModel;

namespace Friday.Mobile.ViewModels;

public partial class AddQuestionViewModel : ObservableObject
{
    [ObservableProperty]
    private string _title = "添加错题";
}
