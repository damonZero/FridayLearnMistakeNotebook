using Friday.Application.Interfaces;
using Friday.Domain.Entities;

namespace Friday.Application.Services;

public class SubjectService
{
    private readonly ISubjectRepository _subjectRepository;

    public SubjectService(ISubjectRepository subjectRepository)
    {
        _subjectRepository = subjectRepository;
    }

    public async Task<List<Subject>> GetAllAsync()
    {
        return await _subjectRepository.GetAllAsync();
    }

    public async Task<List<Subject>> GetPresetAsync()
    {
        return await _subjectRepository.GetPresetAsync();
    }

    public async Task<Subject> CreateAsync(string name, string icon, string color)
    {
        if (string.IsNullOrWhiteSpace(name))
            throw new ArgumentException("Subject name cannot be empty.", nameof(name));

        if (await _subjectRepository.ExistsByNameAsync(name))
            throw new InvalidOperationException($"Subject with name '{name}' already exists.");

        var allSubjects = await _subjectRepository.GetAllAsync();
        var maxSortOrder = allSubjects.Count > 0 ? allSubjects.Max(s => s.SortOrder) : 0;

        var subject = new Subject
        {
            Name = name.Trim(),
            Icon = icon ?? string.Empty,
            Color = color ?? string.Empty,
            IsPreset = false,
            SortOrder = maxSortOrder + 1,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        return await _subjectRepository.AddAsync(subject);
    }

    public async Task<Subject> UpdateAsync(Guid id, string name, string icon, string color)
    {
        var subject = await _subjectRepository.GetByIdAsync(id)
            ?? throw new InvalidOperationException($"Subject with id '{id}' not found.");

        if (subject.IsPreset)
            throw new InvalidOperationException("Cannot modify preset subjects.");

        if (string.IsNullOrWhiteSpace(name))
            throw new ArgumentException("Subject name cannot be empty.", nameof(name));

        subject.Name = name.Trim();
        subject.Icon = icon ?? string.Empty;
        subject.Color = color ?? string.Empty;
        subject.UpdatedAt = DateTime.UtcNow;

        return await _subjectRepository.UpdateAsync(subject);
    }

    public async Task DeleteAsync(Guid id)
    {
        var subject = await _subjectRepository.GetByIdAsync(id)
            ?? throw new InvalidOperationException($"Subject with id '{id}' not found.");

        if (subject.IsPreset)
            throw new InvalidOperationException("Cannot delete preset subjects.");

        await _subjectRepository.DeleteAsync(id);
    }

    public async Task SeedPresetsAsync()
    {
        var existing = await _subjectRepository.GetPresetAsync();
        var existingNames = existing.Select(s => s.Name).ToHashSet();

        var presets = new List<(string Name, string Icon, string Color, int SortOrder)>
        {
            ("语文", "📖", "#4A90D9", 1),
            ("数学", "🔢", "#7ED321", 2),
            ("英语", "🔤", "#F5A623", 3)
        };

        foreach (var (name, icon, color, sortOrder) in presets)
        {
            if (!existingNames.Contains(name))
            {
                await _subjectRepository.AddAsync(new Subject
                {
                    Name = name,
                    Icon = icon,
                    Color = color,
                    SortOrder = sortOrder,
                    IsPreset = true,
                    CreatedAt = DateTime.UtcNow,
                    UpdatedAt = DateTime.UtcNow
                });
            }
        }
    }
}
