using Friday.Domain.Enums;

namespace Friday.Domain.Entities;

public class Question
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid? SubjectId { get; set; }
    public Guid? ChapterId { get; set; }
    public Guid? KnowledgePointId { get; set; }
    public string Content { get; set; } = string.Empty;
    public string Answer { get; set; } = string.Empty;
    public string UserAnswer { get; set; } = string.Empty;
    public ErrorType ErrorType { get; set; } = ErrorType.Unknown;
    public string ImagePath { get; set; } = string.Empty;
    public string Notes { get; set; } = string.Empty;
    public DateTime ReviewDate { get; set; } = DateTime.UtcNow;
    public int LeitnerBox { get; set; } = 1;
    public double EaseFactor { get; set; } = 2.5;
    public int IntervalDays { get; set; } = 1;
    public int Streak { get; set; } = 0;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    public Subject? Subject { get; set; }
    public Chapter? Chapter { get; set; }
    public KnowledgePoint? KnowledgePoint { get; set; }
}
