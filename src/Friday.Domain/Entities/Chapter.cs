namespace Friday.Domain.Entities;

public class Chapter
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid SubjectId { get; set; }
    public string Name { get; set; } = string.Empty;
    public int SortOrder { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    public Subject Subject { get; set; } = null!;
    public List<KnowledgePoint> KnowledgePoints { get; set; } = new();
}
