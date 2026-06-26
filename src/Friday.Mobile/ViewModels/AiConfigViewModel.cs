using CommunityToolkit.Mvvm.ComponentModel;

namespace Friday.Mobile.ViewModels;

public partial class AiConfigViewModel : ObservableObject
{
    [ObservableProperty]
    private string _title = "AI 配置";
}
