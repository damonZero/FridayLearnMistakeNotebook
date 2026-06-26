# Phase 1: Foundation - Context

**Gathered:** 2026-06-26
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 1 delivers the foundational infrastructure for the Smart Wrong Answer Book application:
- .NET 10 project skeleton with Clean Architecture layers (Domain, Application, Infrastructure, Presentation)
- EF Core + SQLite database with migration system
- Domain entities: Subject, Chapter, KnowledgePoint, Question with proper relationships
- Subject management with preset subjects (Chinese, Math, English) and dynamic CRUD
- Hierarchical knowledge tree (Subject > Chapter > KnowledgePoint)
- Search, filter, and sort for question list
- WPF shell with sidebar navigation and cartoon-themed UI
- Data export/import (JSON/CSV) and auto-backup
- Chinese language interface

This phase establishes the data foundation and app shell that all subsequent phases build upon.

</domain>

<decisions>
## Implementation Decisions

### Domain Entity Design
- **D-01:** Four core entities with hierarchical relationships:
  - `Subject` (科目): Id, Name, Icon, Color, SortOrder, IsPreset, CreatedAt, UpdatedAt
  - `Chapter` (章节): Id, SubjectId, Name, SortOrder, CreatedAt, UpdatedAt
  - `KnowledgePoint` (知识点): Id, ChapterId, Name, Description, SortOrder, CreatedAt, UpdatedAt
  - `Question` (错题): Id, SubjectId, ChapterId?, KnowledgePointId?, Content, Answer, UserAnswer, ErrorType, ImagePath, Notes, ReviewDate, LeitnerBox, EaseFactor, IntervalDays, Streak, CreatedAt, UpdatedAt
- **D-02:** Question can be linked to Subject, Chapter, or KnowledgePoint at any level (flexible association)
- **D-03:** ErrorType enum: Careless, Conceptual, Method, Calculation, Unknown
- **D-04:** Leitner box system: 5 boxes (1-5), default box 1 for new questions

### WPF Navigation Pattern
- **D-05:** Sidebar navigation with icon + text labels, fixed on left side
- **D-06:** Main navigation items: Home (仪表盘), Subjects (科目管理), Questions (错题列表), Review (复习), Settings (设置)
- **D-07:** Hamburger menu for settings and advanced features
- **D-08:** Content area fills remaining space with page-based navigation

### Cartoon UI Implementation
- **D-09:** Base theme: WPF UI (Lepoco) Fluent Design for modern look
- **D-10:** Cartoon overlay: soft color palette (blues, greens, pinks), large border radius (12-16px)
- **D-11:** Icons: Segoe Fluent Icons for standard icons + custom SVG for cartoon elements
- **D-12:** Font: Microsoft YaHei (微软雅黑) for body, playful font for titles
- **D-13:** Simple page transition animations and button feedback effects
- **D-14:** Color scheme: Primary=#4A90D9 (soft blue), Secondary=#7ED321 (green), Accent=#F5A623 (orange)

### Backup Strategy
- **D-15:** Auto-backup on application close (configurable in settings)
- **D-16:** Retention: keep last 7 backups, auto-delete older ones
- **D-17:** Backup location: `%AppData%/SmartWrongAnswerBook/Backups/`
- **D-18:** Export format: JSON with version field for forward compatibility
- **D-19:** Import: validate JSON version before restoring, show diff summary

### Claude's Discretion
- Database schema design and migration strategy
- DI container configuration (Microsoft.Extensions.DependencyInjection)
- Async/await patterns for all I/O operations
- Error handling and logging (Serilog)
- Unit test structure and coverage targets

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project Context
- `.planning/PROJECT.md` — Project vision, core value, constraints
- `.planning/REQUIREMENTS.md` — 55 v1 requirements with REQ-IDs
- `.planning/ROADMAP.md` — Phase structure and success criteria

### Research Findings
- `.planning/research/STACK.md` — Technology stack with versions and rationale
- `.planning/research/ARCHITECTURE.md` — Clean Architecture + MVVM patterns
- `.planning/research/FEATURES.md` — Feature landscape and MVP recommendations
- `.planning/research/PITFALLS.md` — 15 domain-specific pitfalls to avoid
- `.planning/research/SUMMARY.md` — Executive summary of all research

### Configuration
- `.planning/config.json` — Workflow preferences (mode, granularity, agents)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- None (greenfield project)

### Established Patterns
- Clean Architecture: Domain → Application → Infrastructure → Presentation
- MVVM with CommunityToolkit.Mvvm ([ObservableProperty], [RelayCommand])
- EF Core + SQLite with code-first migrations
- Async/await for all I/O operations

### Integration Points
- Domain layer has zero dependencies
- Application layer defines interfaces (repositories, services)
- Infrastructure implements interfaces (SQLite, file system, AI APIs)
- Presentation layer binds to ViewModels via DataTemplates

</code_context>

<specifics>
## Specific Ideas

- User has Unity/C# background, chose WPF for this project
- User prefers "you decide" for technical implementation details
- Cartoon UI should be cute but not childish (suitable for both parent and child)
- Knowledge tree should be intuitive to navigate (Subject > Chapter > KnowledgePoint)
- Export/import should be simple and reliable (parent will use this for backup)

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 1-Foundation*
*Context gathered: 2026-06-26*
