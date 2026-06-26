namespace Friday.Mobile.Services;

public class NavigationService
{
    private INavigation? _navigation;

    public void Initialize(INavigation navigation)
    {
        _navigation = navigation;
    }

    public async Task NavigateToAsync<TPage>() where TPage : Page
    {
        if (_navigation == null)
            return;

        var page = GetPage<TPage>();
        await _navigation.PushAsync(page);
    }

    public async Task NavigateBackAsync()
    {
        if (_navigation == null)
            return;

        await _navigation.PopAsync();
    }

    public async Task NavigateToRootAsync()
    {
        if (_navigation == null)
            return;

        await _navigation.PopToRootAsync();
    }

    private static Page GetPage<TPage>() where TPage : Page
    {
        // This will be resolved from DI in the actual implementation
        return (TPage)Activator.CreateInstance(typeof(TPage))!;
    }
}
