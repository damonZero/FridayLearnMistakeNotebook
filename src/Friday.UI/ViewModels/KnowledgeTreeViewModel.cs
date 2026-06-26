using System.Collections.ObjectModel;
using System.Windows;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Friday.Application.Services;
using Friday.Domain.Entities;

namespace Friday.UI.ViewModels;

public partial class KnowledgeTreeViewModel : ObservableObject
{
    private readonly KnowledgeTreeService _knowledgeTreeService;
    private readonly SubjectService _subjectService;

    [ObservableProperty]
    private ObservableCollection<Subject> _subjects = new();

    [ObservableProperty]
    private Subject? _selectedSubject;

    [ObservableProperty]
    private ObservableCollection<Chapter> _chapters = new();

    [ObservableProperty]
    private Chapter? _selectedChapter;

    [ObservableProperty]
    private ObservableCollection<KnowledgePoint> _knowledgePoints = new();

    [ObservableProperty]
    private string _newChapterName = string.Empty;

    [ObservableProperty]
    private string _newKnowledgePointName = string.Empty;

    [ObservableProperty]
    private string _newKnowledgePointDescription = string.Empty;

    [ObservableProperty]
    private string _errorMessage = string.Empty;

    public KnowledgeTreeViewModel(
        KnowledgeTreeService knowledgeTreeService,
        SubjectService subjectService)
    {
        _knowledgeTreeService = knowledgeTreeService;
        _subjectService = subjectService;
        _ = LoadSubjectsAsync();
    }

    partial void OnSelectedSubjectChanged(Subject? value)
    {
        if (value != null)
        {
            _ = LoadChaptersAsync(value.Id);
        }
        else
        {
            Chapters.Clear();
            KnowledgePoints.Clear();
        }
    }

    partial void OnSelectedChapterChanged(Chapter? value)
    {
        if (value != null)
        {
            _ = LoadKnowledgePointsAsync(value.Id);
        }
        else
        {
            KnowledgePoints.Clear();
        }
    }

    [RelayCommand]
    private async Task LoadSubjectsAsync()
    {
        var subjects = await _subjectService.GetAllAsync();
        Subjects = new ObservableCollection<Subject>(subjects);
    }

    private async Task LoadChaptersAsync(Guid subjectId)
    {
        var chapters = await _knowledgeTreeService.GetBySubjectAsync(subjectId);
        Chapters = new ObservableCollection<Chapter>(chapters);
        KnowledgePoints.Clear();
    }

    private async Task LoadKnowledgePointsAsync(Guid chapterId)
    {
        var kps = await _knowledgeTreeService.GetKnowledgePointsByChapterAsync(chapterId);
        KnowledgePoints = new ObservableCollection<KnowledgePoint>(kps);
    }

    [RelayCommand]
    private async Task AddChapterAsync()
    {
        ErrorMessage = string.Empty;

        if (SelectedSubject == null)
        {
            ErrorMessage = "请先选择科目";
            return;
        }

        if (string.IsNullOrWhiteSpace(NewChapterName))
        {
            ErrorMessage = "请输入章节名称";
            return;
        }

        try
        {
            var nextOrder = Chapters.Count > 0 ? Chapters.Max(c => c.SortOrder) + 1 : 1;
            await _knowledgeTreeService.AddChapterAsync(SelectedSubject.Id, NewChapterName, nextOrder);
            NewChapterName = string.Empty;
            await LoadChaptersAsync(SelectedSubject.Id);
        }
        catch (Exception ex)
        {
            ErrorMessage = ex.Message;
        }
    }

    [RelayCommand]
    private async Task DeleteChapterAsync(Chapter? chapter)
    {
        if (chapter == null || SelectedSubject == null) return;

        var result = MessageBox.Show(
            $"确定要删除章节 \"{chapter.Name}\" 及其所有知识点吗？",
            "确认删除",
            MessageBoxButton.YesNo,
            MessageBoxImage.Question);

        if (result == MessageBoxResult.Yes)
        {
            await _knowledgeTreeService.DeleteChapterAsync(chapter.Id);
            await LoadChaptersAsync(SelectedSubject.Id);
        }
    }

    [RelayCommand]
    private async Task AddKnowledgePointAsync()
    {
        ErrorMessage = string.Empty;

        if (SelectedChapter == null)
        {
            ErrorMessage = "请先选择章节";
            return;
        }

        if (string.IsNullOrWhiteSpace(NewKnowledgePointName))
        {
            ErrorMessage = "请输入知识点名称";
            return;
        }

        try
        {
            var nextOrder = KnowledgePoints.Count > 0
                ? KnowledgePoints.Max(kp => kp.SortOrder) + 1 : 1;
            await _knowledgeTreeService.AddKnowledgePointAsync(
                SelectedChapter.Id, NewKnowledgePointName, NewKnowledgePointDescription, nextOrder);
            NewKnowledgePointName = string.Empty;
            NewKnowledgePointDescription = string.Empty;
            await LoadKnowledgePointsAsync(SelectedChapter.Id);
        }
        catch (Exception ex)
        {
            ErrorMessage = ex.Message;
        }
    }

    [RelayCommand]
    private async Task DeleteKnowledgePointAsync(KnowledgePoint? knowledgePoint)
    {
        if (knowledgePoint == null || SelectedChapter == null) return;

        var result = MessageBox.Show(
            $"确定要删除知识点 \"{knowledgePoint.Name}\" 吗？",
            "确认删除",
            MessageBoxButton.YesNo,
            MessageBoxImage.Question);

        if (result == MessageBoxResult.Yes)
        {
            await _knowledgeTreeService.DeleteKnowledgePointAsync(knowledgePoint.Id);
            await LoadKnowledgePointsAsync(SelectedChapter.Id);
        }
    }
}
