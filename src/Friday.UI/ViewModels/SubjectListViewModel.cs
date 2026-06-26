using System.Collections.ObjectModel;
using System.Windows;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Friday.Application.Interfaces;
using Friday.Domain.Entities;

namespace Friday.UI.ViewModels;

public partial class SubjectListViewModel : ObservableObject
{
    private readonly ISubjectRepository _subjectRepository;

    [ObservableProperty]
    private ObservableCollection<Subject> _subjects = new();

    [ObservableProperty]
    private string _newSubjectName = string.Empty;

    public SubjectListViewModel(ISubjectRepository subjectRepository)
    {
        _subjectRepository = subjectRepository;
        _ = LoadSubjectsAsync();
    }

    [RelayCommand]
    private async Task LoadSubjectsAsync()
    {
        var subjects = await _subjectRepository.GetAllAsync();
        Subjects = new ObservableCollection<Subject>(subjects);
    }

    [RelayCommand]
    private async Task AddSubjectAsync()
    {
        if (string.IsNullOrWhiteSpace(NewSubjectName))
        {
            MessageBox.Show("请输入科目名称", "提示", MessageBoxButton.OK, MessageBoxImage.Warning);
            return;
        }

        if (await _subjectRepository.ExistsByNameAsync(NewSubjectName))
        {
            MessageBox.Show("该科目已存在", "提示", MessageBoxButton.OK, MessageBoxImage.Warning);
            return;
        }

        var subject = new Subject
        {
            Name = NewSubjectName,
            IsPreset = false,
            SortOrder = Subjects.Count + 1
        };

        await _subjectRepository.AddAsync(subject);
        NewSubjectName = string.Empty;
        await LoadSubjectsAsync();
    }

    [RelayCommand]
    private async Task DeleteSubjectAsync(Subject subject)
    {
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
            await _subjectRepository.DeleteAsync(subject.Id);
            await LoadSubjectsAsync();
        }
    }

    [RelayCommand]
    private async Task RefreshAsync()
    {
        await LoadSubjectsAsync();
    }
}
