using CommunityToolkit.Mvvm.ComponentModel;
using Microsoft.Extensions.DependencyInjection;

namespace Friday.UI.Services;

public class NavigationService
{
    private readonly IServiceProvider _serviceProvider;

    public event EventHandler<(ObservableObject ViewModel, string Title)>? ViewModelChanged;

    public NavigationService(IServiceProvider serviceProvider)
    {
        _serviceProvider = serviceProvider;
    }

    public void NavigateTo<TViewModel>(string title) where TViewModel : ObservableObject
    {
        var viewModel = _serviceProvider.GetRequiredService<TViewModel>();
        ViewModelChanged?.Invoke(this, (viewModel, title));
    }
}
