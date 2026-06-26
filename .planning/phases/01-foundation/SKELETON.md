# Walking Skeleton -- Smart Wrong Answer Book

**Phase:** 1
**Generated:** 2026-06-26

## Capability Proven End-to-End

A user can launch the WPF app, see a list of three preset subjects (Chinese, Math, English) loaded from a SQLite database, add a new custom subject via a button, and see it persist after restarting the app.

## Architectural Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Framework | .NET 10.0 LTS + WPF | User's C#/Unity background, Windows-only, mature ecosystem |
| UI controls | WPF UI (Lepoco) 4.3.0 Fluent theme | Modern Fluent Design without leaving WPF |
| MVVM | CommunityToolkit.Mvvm 8.4.2 | Microsoft official, source generators eliminate boilerplate |
| Data layer | EF Core 10.0.9 + SQLite | Code-first migrations, LINQ queries, single-file embedded DB |
| Architecture | Clean Architecture: Domain / Application / Infrastructure / UI (4 assemblies) | Dependency inversion, testability, layer isolation |
| Logging | Serilog 4.3.1 (file + console) | Structured logging, file sink for debugging |
| Testing | xUnit + FluentAssertions | Standard .NET test framework with readable assertions |
| Directory layout | `src/{Friday.Domain,Friday.Application,Friday.Infrastructure,Friday.UI}/` + `tests/Friday.Tests/` | Clean Architecture layer separation |

## Stack Touched in Phase 1

- [x] Project scaffold -- .NET 10 solution, 4 assemblies (Domain, Application, Infrastructure, UI), 1 test project, build, test runner
- [x] Routing -- MainWindow shell with sidebar navigation (Home, Subjects, Questions, Review, Settings), ContentControl-based page switching via DataTemplates
- [x] Database -- EF Core + SQLite with AppDbContext, Subject entity, versioned migration, CRUD operations (read preset subjects, write new subject)
- [x] UI -- SubjectListView with DataGrid bound to ViewModel, Add Subject button wired to RelayCommand, delete with confirmation dialog
- [x] Deployment -- `dotnet run --project src/Friday.UI` launches working app on local dev machine

## Out of Scope (Deferred to Later Slices)

- KnowledgePoint and Question entities (Plan 02)
- Chapter entity and hierarchical tree browsing (Plan 02)
- Search, filter, sort for question list (Plan 02)
- Cartoon overlay theming, soft color palette, custom border radius (Plan 03)
- Segoe Fluent Icons + custom SVG cartoon elements (Plan 03)
- Chinese-language interface strings (Plan 03)
- Data export/import (JSON/CSV) (Plan 03)
- Auto-backup on close (Plan 03)
- Page transition animations (Plan 03)
- OCR, camera, AI integration (Phase 2)
- Spaced repetition / review engine (Phase 3)

## Subsequent Slice Plan

Each later plan adds one vertical slice on top of this skeleton without altering its architectural decisions:

- Plan 02 (Phase 1): Subject & knowledge management -- full Subject CRUD, hierarchical knowledge tree (Chapter, KnowledgePoint), search/filter/sort for questions
- Plan 03 (Phase 1): WPF shell & base UI -- sidebar navigation with icons, WPF UI Fluent theme + cartoon overlay, Chinese-language strings, data export/import, auto-backup
- Phase 2: Capture pipeline -- camera, image processing, OCR with correction
- Phase 3: Review engine -- spaced repetition, daily review queue, review session UI
