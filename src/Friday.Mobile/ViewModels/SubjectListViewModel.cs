using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Friday.Application.Services;
using Friday.Domain.Entities;

namespace Friday.Mobile.ViewModels;

public partial class SubjectListViewModel : ObservableObject
{
    private readonly SubjectService _subjectService;

    [ObservableProperty]
    private ObservableCollection<Subject> _subjects = new();

    [ObservableProperty]
    private Subject? _selectedSubject;

    [ObservableProperty]
    private bool _isLoading;

    [ObservableProperty]
    private string _statusMessage = string.Empty;

    public SubjectListViewModel(SubjectService subjectService)
    {
        _subjectService = subjectService;
    }

    [RelayCommand]
    private async Task LoadSubjectsAsync()
    {
        IsLoading = true;
        try
        {
            var subjects = await _subjectService.GetAllAsync();
            Subjects = new ObservableCollection<Subject>(subjects);
        }
        catch (Exception ex)
        {
            StatusMessage = $"加载失败: {ex.Message}";
        }
        finally
        {
            IsLoading = false;
        }
    }

    [RelayCommand]
    private async Task AddSubjectAsync()
    {
        var result = await Microsoft.Maui.Controls.Application.Current!.MainPage!.DisplayPromptAsync(
            "添加科目", "请输入科目名称:", "确定", "取消");

        if (string.IsNullOrWhiteSpace(result))
            return;

        try
        {
            await _subjectService.CreateAsync(result, "#4A90D9", "📚");
            await LoadSubjectsAsync();
            StatusMessage = "添加成功";
        }
        catch (Exception ex)
        {
            StatusMessage = $"添加失败: {ex.Message}";
        }
    }

    [RelayCommand]
    private async Task DeleteSubjectAsync(Subject subject)
    {
        if (subject.IsPreset)
        {
            StatusMessage = "预设科目不能删除";
            return;
        }

        var confirmed = await Microsoft.Maui.Controls.Application.Current!.MainPage!.DisplayAlert(
            "确认删除", $"确定要删除 {subject.Name} 吗？", "删除", "取消");

        if (!confirmed)
            return;

        try
        {
            await _subjectService.DeleteAsync(subject.Id);
            await LoadSubjectsAsync();
            StatusMessage = "删除成功";
        }
        catch (Exception ex)
        {
            StatusMessage = $"删除失败: {ex.Message}";
        }
    }
}
