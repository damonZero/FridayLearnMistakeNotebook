using System.Collections.ObjectModel;
using System.Windows;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Friday.Application.Services;
using Friday.Domain.Entities;

namespace Friday.UI.ViewModels;

public partial class SubjectManagementViewModel : ObservableObject
{
    private readonly SubjectService _subjectService;

    [ObservableProperty]
    private ObservableCollection<Subject> _subjects = new();

    [ObservableProperty]
    private Subject? _selectedSubject;

    [ObservableProperty]
    private string _newSubjectName = string.Empty;

    [ObservableProperty]
    private string _editSubjectName = string.Empty;

    [ObservableProperty]
    private bool _isEditing;

    [ObservableProperty]
    private string _errorMessage = string.Empty;

    public SubjectManagementViewModel(SubjectService subjectService)
    {
        _subjectService = subjectService;
        _ = LoadSubjectsAsync();
    }

    [RelayCommand]
    private async Task LoadSubjectsAsync()
    {
        var subjects = await _subjectService.GetAllAsync();
        Subjects = new ObservableCollection<Subject>(subjects);
        IsEditing = false;
    }

    [RelayCommand]
    private async Task AddSubjectAsync()
    {
        ErrorMessage = string.Empty;

        if (string.IsNullOrWhiteSpace(NewSubjectName))
        {
            ErrorMessage = "请输入科目名称";
            return;
        }

        try
        {
            await _subjectService.CreateAsync(NewSubjectName, string.Empty, string.Empty);
            NewSubjectName = string.Empty;
            await LoadSubjectsAsync();
        }
        catch (InvalidOperationException ex)
        {
            ErrorMessage = ex.Message;
        }
    }

    [RelayCommand]
    private async Task DeleteSubjectAsync(Subject? subject)
    {
        if (subject == null) return;

        if (subject.IsPreset)
        {
            MessageBox.Show("预设科目不能删除", "提示", MessageBoxButton.OK, MessageBoxImage.Warning);
            return;
        }

        var result = MessageBox.Show(
            $"确定要删除科目 \"{subject.Name}\" 吗？",
            "确认删除",
            MessageBoxButton.YesNo,
            MessageBoxImage.Question);

        if (result == MessageBoxResult.Yes)
        {
            try
            {
                await _subjectService.DeleteAsync(subject.Id);
                await LoadSubjectsAsync();
            }
            catch (InvalidOperationException ex)
            {
                MessageBox.Show(ex.Message, "错误", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }
    }

    [RelayCommand]
    private void StartEditSubject(Subject? subject)
    {
        if (subject == null || subject.IsPreset) return;

        SelectedSubject = subject;
        EditSubjectName = subject.Name;
        IsEditing = true;
    }

    [RelayCommand]
    private async Task SaveEditSubjectAsync()
    {
        ErrorMessage = string.Empty;

        if (SelectedSubject == null) return;

        if (string.IsNullOrWhiteSpace(EditSubjectName))
        {
            ErrorMessage = "科目名称不能为空";
            return;
        }

        try
        {
            await _subjectService.UpdateAsync(
                SelectedSubject.Id, EditSubjectName, SelectedSubject.Icon, SelectedSubject.Color);
            IsEditing = false;
            await LoadSubjectsAsync();
        }
        catch (InvalidOperationException ex)
        {
            ErrorMessage = ex.Message;
        }
    }

    [RelayCommand]
    private void CancelEditSubject()
    {
        IsEditing = false;
        ErrorMessage = string.Empty;
    }
}
