using Friday.Domain.Entities;
using Friday.Domain.Enums;
using FluentAssertions;
using Xunit;

namespace Friday.Tests.Domain;

public class SubjectTests
{
    [Fact]
    public void Subject_ShouldInitializeWithDefaultValues()
    {
        var subject = new Subject();

        subject.Id.Should().NotBeEmpty();
        subject.Name.Should().BeEmpty();
        subject.Icon.Should().BeEmpty();
        subject.Color.Should().BeEmpty();
        subject.SortOrder.Should().Be(0);
        subject.IsPreset.Should().BeFalse();
        subject.CreatedAt.Should().BeCloseTo(DateTime.UtcNow, TimeSpan.FromSeconds(5));
        subject.UpdatedAt.Should().BeCloseTo(DateTime.UtcNow, TimeSpan.FromSeconds(5));
        subject.Chapters.Should().BeEmpty();
    }

    [Fact]
    public void Subject_ShouldAllowSettingProperties()
    {
        var subject = new Subject
        {
            Name = "数学",
            Icon = "🔢",
            Color = "#7ED321",
            SortOrder = 1,
            IsPreset = true
        };

        subject.Name.Should().Be("数学");
        subject.Icon.Should().Be("🔢");
        subject.Color.Should().Be("#7ED321");
        subject.SortOrder.Should().Be(1);
        subject.IsPreset.Should().BeTrue();
    }

    [Fact]
    public void Question_ShouldDefaultLeitnerBoxToOne()
    {
        var question = new Question();

        question.LeitnerBox.Should().Be(1);
        question.EaseFactor.Should().Be(2.5);
        question.IntervalDays.Should().Be(1);
        question.Streak.Should().Be(0);
    }

    [Fact]
    public void ErrorType_ShouldHaveFiveValues()
    {
        var values = Enum.GetValues<ErrorType>();

        values.Should().HaveCount(5);
        values.Should().Contain(ErrorType.Careless);
        values.Should().Contain(ErrorType.Conceptual);
        values.Should().Contain(ErrorType.Method);
        values.Should().Contain(ErrorType.Calculation);
        values.Should().Contain(ErrorType.Unknown);
    }
}
