using CommunityToolkit.Mvvm.ComponentModel;

namespace Friday.Mobile.ViewModels;

public partial class KnowledgeTreeViewModel : ObservableObject
{
    [ObservableProperty]
    private string _title = "知识树";
}
