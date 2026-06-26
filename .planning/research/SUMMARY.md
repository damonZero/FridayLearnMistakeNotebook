# Project Research Summary

**Project:** Smart Wrong Answer Book
**Domain:** EdTech - Desktop Learning Application for Elementary Students
**Researched:** 2026-06-26
**Confidence:** MEDIUM-HIGH

## Executive Summary

The Smart Wrong Answer Book is a Windows-only desktop application that helps parents of elementary students (ages 6-12) capture, organize, and review wrong answers from homework and tests. The core value proposition is a photo-to-review loop: photograph a wrong answer, OCR extracts the text, AI identifies the knowledge point, and a spaced repetition algorithm schedules scientifically-timed reviews. Research indicates this should be built as a local-first WPF application with a cute cartoon UI for children and a clean management interface for parents.

The recommended stack is .NET 10 LTS + WPF + WPF UI (Lepoco) for the UI layer, CommunityToolkit.Mvvm for MVVM, EF Core + SQLite for persistence, OpenCvSharp4 for camera capture, and HttpClient + Polly for AI API integration. This stack is well-suited because WPF is the mature standard for Windows desktop apps, the user XAML experience transfers directly, and WPF UI brings modern Fluent design without leaving the WPF ecosystem. The architecture follows Clean Architecture + MVVM with clear layer separation: Domain (entities, business rules), Application (use cases, interfaces), Infrastructure (SQLite, AI APIs, camera), and Presentation (WPF views/viewmodels).

The single biggest risk is OCR accuracy for handwritten Chinese characters and math notation from student homework. If OCR is unreliable, the entire core value proposition collapses. This must be mitigated by designing the correction UI as a first-class feature from day one, not an afterthought. Other critical pitfalls include spaced repetition algorithm bugs (study Anki implementation), UI thread blocking during AI calls (async-first pattern), and database migration strategy (implement before first table is created). The project should ship as single-user, local-first, with cloud sync and multi-user deferred to future versions.

## Key Findings

### Recommended Stack

The stack is built on .NET 10 LTS (C# 13) with WPF as the UI framework. WPF was chosen over Avalonia (cross-platform unnecessary), WinUI 3 (smaller ecosystem, rough edges), and MAUI (Windows desktop issues). WPF UI (Lepoco 4.3.0) adds modern Fluent Design without leaving WPF. CommunityToolkit.Mvvm 8.4.2 provides source generators that eliminate 80% of MVVM boilerplate.

**Core technologies:**
- **.NET 10 LTS + C# 13**: Application runtime - latest LTS with 3 years of support
- **WPF + WPF UI (Lepoco 4.3.0)**: UI framework - mature ecosystem, VS designer, modern Fluent look
- **CommunityToolkit.Mvvm 8.4.2**: MVVM framework - Microsoft-maintained, source generators, no boilerplate
- **EF Core + SQLite 10.0.9**: ORM + local database - migrations, LINQ, zero-config embedded DB
- **OpenCvSharp4 4.13.0**: Camera capture - wraps OpenCV 4.13, active maintenance
- **SixLabors.ImageSharp 4.0.0**: Image processing - fully managed, fluent API for crop/enhance
- **SkiaSharp 4.148.0**: 2D rendering - powers cartoon UI, SVG rendering, animations
- **HttpClient + Polly 8.7.0**: AI integration - typed clients with retry/timeout policies
- **Serilog 4.3.1**: Structured logging - file sink for debugging AI calls

### Expected Features

**Must have (table stakes):**
- Photo capture + crop - core workflow: parent photographs wrong answer
- OCR question recognition - converts photo to editable text (accuracy is #1 risk)
- Subject organization - preset Chinese/Math/English + custom subjects
- Wrong answer list + search - browse and filter by subject, date, knowledge point
- Manual text editing - OCR correction is essential, not optional
- Knowledge point tagging - categorize by concept (manual first, AI later)
- Review scheduling (spaced repetition) - Ebbinghaus + Leitner box system
- Daily review task list - child opens app, sees what to review today
- Mark review result - Got it / Still wrong updates schedule
- Local data storage - SQLite, no cloud dependency
- Data export/backup - PDF for printing, JSON/CSV for backup
- Child-friendly UI - cute cartoon style for children, clean mode for parents

**Should have (differentiators):**
- AI knowledge gap analysis - identifies root cause of errors, not just symptoms
- Similar question generation - AI generates practice questions targeting weak points
- Progress analytics dashboard - visual learning progress for parents
- Per-task AI model config - power feature: different models for OCR vs analysis
- Multi-image batch import - photograph entire test, auto-crop individual questions
- Error type classification - Careless / Conceptual / Method / Calculation
- Achievement/streak system - gamification for 6-12 year olds
- Knowledge point mastery map - color-coded tree showing mastery levels

**Defer (v2+):**
- Multi-user/multi-child - adds auth, data isolation complexity
- Cloud sync - adds server costs, privacy concerns
- Social features - privacy concerns for children data
- LMS features - scope creep beyond error tracking
- Multi-language UI - V1 is Chinese-only

### Architecture Approach

Clean Architecture + MVVM with four layers: Domain (entities, business rules, zero dependencies), Application (use cases, service interfaces, DTOs), Infrastructure (SQLite repos, AI services, camera, file storage), and Presentation (WPF views, viewmodels, navigation). Dependencies point inward - Domain knows nothing about other layers. The project structure uses four assemblies: Friday.Domain, Friday.Application, Friday.Infrastructure, Friday.UI.

**Major components:**
1. **Domain Layer** - WrongQuestion, KnowledgePoint, ReviewSchedule, Subject entities; LearningAlgorithm domain service (Ebbinghaus/Leitner)
2. **Application Layer** - IQuestionService, IReviewService, IAIService interfaces; QuestionService, ReviewService use cases
3. **Infrastructure Layer** - AppDbContext (EF Core), QuestionRepository, OpenAIService, CameraService, BackupService, SettingsService
4. **Presentation Layer** - MainViewModel, CaptureViewModel, ReviewViewModel, QuestionListViewModel; NavigationService, DialogService

**Key patterns:**
- Repository pattern for all database operations
- Service pattern with DI for business logic
- DataTemplate-based navigation (WPF resolves Views for ViewModels)
- Async/await for all AI calls with loading indicators
- Configuration management via settings service (not hardcoded)

### Critical Pitfalls

1. **OCR accuracy for handwritten content** - Design as AI-assisted + human verification, not automatic. Build fast correction UI as first-class feature. Test with real handwritten samples from target age group early. Allow manual fallback at every OCR touchpoint.
2. **Spaced repetition algorithm bugs** - Study Anki open-source implementation. Implement as pure, testable function. Write comprehensive unit tests for edge cases (first review, lapse, multi-day gaps, midnight boundaries). Validate against known SM-2 outputs.
3. **UI thread blocking during AI calls** - Establish strict rule: no network I/O on UI thread. Use IAsyncRelayCommand from CommunityToolkit.Mvvm. Show animated loading indicators. Implement cancellation tokens. Set 30-60s timeouts for OCR.
4. **SQLite without migration strategy** - Implement versioned migration system before first table. Enable WAL mode. Use VACUUM INTO for backups (not file-level copy). Auto-backup before each migration.
5. **Image storage bloat** - Compress/resize on capture (display resolution, not camera resolution). Store images as files on disk, not BLOBs in SQLite. Implement storage management screen. Consider auto-archiving old entries.

## Implications for Roadmap

### Phase 1: Foundation (Domain + Database + Project Skeleton)
**Rationale:** Domain has zero dependencies and establishes the data model. Database and migration system must exist before features. Async patterns must be established in the skeleton - retrofitting later is painful.
**Delivers:** Working project with domain entities, SQLite database with migrations, DI container, basic WPF shell with navigation
**Addresses:** Local data storage, subject organization (data model), child-friendly UI (project structure)
**Avoids:** Pitfall 4 (no migration strategy), Pitfall 8 (image storage as BLOBs), Pitfall 9 (rigid subject schema)

### Phase 2: Capture Pipeline (Camera + OCR + Image Processing)
**Rationale:** OCR accuracy is the #1 risk - build it early and iterate. The capture workflow is the entry point for all data. Camera + OCR + correction UI form a tight dependency chain.
**Delivers:** Working photo capture, image enhancement, OCR with correction UI, question creation flow
**Addresses:** Photo capture + crop, OCR recognition, manual text editing, knowledge point tagging (manual)
**Avoids:** Pitfall 1 (OCR accuracy assumptions), Pitfall 3 (UI thread blocking), Pitfall 13 (camera fragility)
**Uses:** OpenCvSharp4, ImageSharp, HttpClient + Polly, AI service interfaces

### Phase 3: Review Engine (Spaced Repetition + Review UI)
**Rationale:** The learning algorithm is complex but isolated - can be developed and tested independently. Review is the core value proposition after capture. Depends on having questions in the database (Phase 2).
**Delivers:** Working spaced repetition scheduling, daily review queue, review session UI, progress tracking
**Addresses:** Review scheduling, daily review task list, on-demand review mode, mark review result
**Avoids:** Pitfall 2 (algorithm bugs), Pitfall 11 (midnight boundary issues)
**Implements:** LearningAlgorithm domain service, ReviewService, ReviewViewModel

### Phase 4: AI Intelligence Layer (Analysis + Generation + Configuration)
**Rationale:** AI features differentiate but depend on having enough data (Phase 1-3 questions) to be useful. Per-task model config needs secure key management. Question generation quality requires iterative prompt engineering.
**Delivers:** AI knowledge gap analysis, similar question generation, error type classification, per-task AI model configuration
**Addresses:** AI knowledge gap analysis (differentiator), similar question generation (differentiator), per-task AI config (differentiator)
**Avoids:** Pitfall 5 (API key security), Pitfall 7 (config overwhelm), Pitfall 10 (generation quality)
**Uses:** HttpClient + Polly, settings service, AI service factory pattern

### Phase 5: Polish and Engagement (UI + Analytics + Gamification)
**Rationale:** UX improvements and engagement features make the product delightful but are not required for core value. Parent/child mode separation needs both modes working. Analytics need review data from Phase 3.
**Delivers:** Polished cartoon UI, parent/child mode separation, progress analytics dashboard, achievement system, batch import, export/backup
**Addresses:** Progress analytics, achievement/streak system, multi-image batch import, print export, review reminders
**Avoids:** Pitfall 6 (cartoon UI sacrificing usability), Pitfall 14 (parent/child role confusion)

### Phase Ordering Rationale

- Domain first because it has zero dependencies and defines the data model everything else uses
- Capture pipeline second because OCR is the #1 risk - validate early, iterate fast
- Review engine third because it is the core value proposition and can be tested independently
- AI intelligence fourth because it differentiates but needs data from prior phases
- Polish last because it enhances but does not enable the core loop
- Each phase builds on the previous: Domain -> Capture -> Review -> AI -> Polish
- Migration system and async patterns established in Phase 1 prevent rework in all later phases

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 2:** OCR pipeline - needs research on specific AI provider APIs, image preprocessing techniques, Chinese handwriting recognition accuracy
- **Phase 3:** Spaced repetition - needs detailed study of Anki SM-2 implementation, edge case handling
- **Phase 4:** AI prompt engineering - needs research on effective prompts for knowledge analysis and question generation in Chinese education context

Phases with standard patterns (skip research-phase):
- **Phase 1:** Well-documented .NET patterns - Clean Architecture, EF Core, WPF MVVM are all standard
- **Phase 5:** Standard WPF theming, charting libraries, notification APIs - well-documented

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All packages verified on NuGet.org. .NET 10 LTS confirmed. WPF + WPF UI is the standard Windows desktop stack. |
| Features | MEDIUM | Based on Chinese EdTech market analysis and learning theory research. Would benefit from direct user interviews. |
| Architecture | HIGH | Clean Architecture + MVVM is the standard C# desktop pattern. All code examples verified against CommunityToolkit.Mvvm and EF Core docs. |
| Pitfalls | MEDIUM-HIGH | Based on domain expertise and known patterns. OCR accuracy numbers and spaced repetition edge cases would benefit from real-world testing. |

**Overall confidence:** MEDIUM-HIGH

### Gaps to Address

- **OCR accuracy benchmarks:** No real-world testing with handwritten Chinese from elementary students. Plan a Phase 2 spike to test multiple OCR providers with sample homework photos.
- **User validation:** Features list is based on competitor analysis, not direct parent/child interviews. Consider lightweight user research before finalizing Phase 4-5 scope.
- **SkiaSharp cartoon UI:** No specific cartoon design system or asset library identified. Theming approach needs validation - consider starting with WPF UI Fluent theme and layering cartoon elements incrementally.
- **Chinese AI provider evaluation:** Research identified generic patterns but did not evaluate specific Chinese OCR/AI providers (Baidu OCR, Tencent OCR, Alibaba OCR). Phase 2 should include provider comparison spike.

## Sources

### Primary (HIGH confidence)
- NuGet.org - all package versions verified (accessed 2026-06-26)
- Microsoft .NET Download page - .NET 10.0 LTS confirmed as current
- Context7 documentation - WPF, Avalonia, ImageSharp, CommunityToolkit.Mvvm, OpenCvSharp, SkiaSharp
- Microsoft Learn - EF Core SQLite, DI in .NET, WPF MVVM pattern
- CommunityToolkit.Mvvm GitHub - source generators, API reference

### Secondary (MEDIUM confidence)
- Chinese EdTech market analysis - competitor analysis of leading apps
- Learning theory - Ebbinghaus forgetting curve, Leitner box system, spaced repetition research
- Anki open-source implementation - reference for spaced repetition edge cases
- WCAG 2.1 guidelines - accessibility for cartoon UI design
- OCR research - CROHME (handwritten math), CASIA (Chinese handwriting) datasets

### Tertiary (LOW confidence)
- Handwritten Chinese OCR accuracy - no real-world testing with target age group; needs validation in Phase 2
- Cartoon UI approach - subjective design, no standard approach; needs iteration with actual children

---
*Research completed: 2026-06-26*
*Ready for roadmap: yes*
