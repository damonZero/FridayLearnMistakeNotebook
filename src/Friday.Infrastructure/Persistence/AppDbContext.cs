using Friday.Domain.Entities;
using Microsoft.EntityFrameworkCore;

namespace Friday.Infrastructure.Persistence;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options)
    {
    }

    public DbSet<Subject> Subjects => Set<Subject>();
    public DbSet<Chapter> Chapters => Set<Chapter>();
    public DbSet<KnowledgePoint> KnowledgePoints => Set<KnowledgePoint>();
    public DbSet<Question> Questions => Set<Question>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        modelBuilder.Entity<Subject>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.Property(e => e.Name).IsRequired().HasMaxLength(100);
            entity.Property(e => e.Icon).HasMaxLength(50);
            entity.Property(e => e.Color).HasMaxLength(20);
            entity.HasMany(e => e.Chapters)
                  .WithOne(e => e.Subject)
                  .HasForeignKey(e => e.SubjectId)
                  .OnDelete(DeleteBehavior.Cascade);
        });

        modelBuilder.Entity<Chapter>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.Property(e => e.Name).IsRequired().HasMaxLength(200);
            entity.HasOne(e => e.Subject)
                  .WithMany(e => e.Chapters)
                  .HasForeignKey(e => e.SubjectId)
                  .OnDelete(DeleteBehavior.Cascade);
            entity.HasMany(e => e.KnowledgePoints)
                  .WithOne(e => e.Chapter)
                  .HasForeignKey(e => e.ChapterId)
                  .OnDelete(DeleteBehavior.Cascade);
        });

        modelBuilder.Entity<KnowledgePoint>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.Property(e => e.Name).IsRequired().HasMaxLength(200);
            entity.Property(e => e.Description).HasMaxLength(1000);
            entity.HasOne(e => e.Chapter)
                  .WithMany(e => e.KnowledgePoints)
                  .HasForeignKey(e => e.ChapterId)
                  .OnDelete(DeleteBehavior.Cascade);
            entity.HasMany(e => e.Questions)
                  .WithOne(e => e.KnowledgePoint)
                  .HasForeignKey(e => e.KnowledgePointId)
                  .OnDelete(DeleteBehavior.SetNull);
        });

        modelBuilder.Entity<Question>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.Property(e => e.Content).IsRequired();
            entity.Property(e => e.Answer).HasMaxLength(2000);
            entity.Property(e => e.UserAnswer).HasMaxLength(2000);
            entity.Property(e => e.ImagePath).HasMaxLength(500);
            entity.Property(e => e.Notes).HasMaxLength(2000);
            entity.HasOne(e => e.Subject)
                  .WithMany()
                  .HasForeignKey(e => e.SubjectId)
                  .OnDelete(DeleteBehavior.SetNull);
            entity.HasOne(e => e.Chapter)
                  .WithMany()
                  .HasForeignKey(e => e.ChapterId)
                  .OnDelete(DeleteBehavior.SetNull);
            entity.HasOne(e => e.KnowledgePoint)
                  .WithMany(e => e.Questions)
                  .HasForeignKey(e => e.KnowledgePointId)
                  .OnDelete(DeleteBehavior.SetNull);
        });
    }

    public void ConfigureSqlite()
    {
        Database.ExecuteSqlRaw("PRAGMA journal_mode=WAL;");
        Database.ExecuteSqlRaw("PRAGMA foreign_keys=ON;");
    }
}
