---
phase: 01-foundation
plan: 01
subsystem: foundation
tags: [solution-scaffold, domain-entities, ef-core, sqlite, wpf, mvvm]
dependency_graph:
  requires: []
  provides: [Friday.Domain, Friday.Application, Friday.Infrastructure, Friday.UI]
  affects: [all-subsequent-plans]
tech_stack:
  added: [.NET 10.0, C# 13, EF Core 10.0.9, SQLite, WPF, CommunityToolkit.Mvvm 8.4.2, Serilog 4.3.1, xUnit, FluentAssertions]
  patterns: [Clean Architecture, MVVM, Repository Pattern, DI Container]
key_files:
  created:
    - Friday.sln
    - src/Friday.Domain/Entities/Subject.cs
    - src/Friday.Domain/Entities/Chapter.cs
    - src/Friday.Domain/Entities/KnowledgePoint.cs
    - src/Friday.Domain/Entities/Question.cs
    - src/Friday.Domain/Enums/ErrorType.cs
    - src/Friday.Application/Interfaces/ISubjectRepository.cs
    - src/Friday.Application/Interfaces/IQuestionRepository.cs
    - src/Friday.Application/Interfaces/IKnowledgePointRepository.cs
    - src/Friday.Infrastructure/Persistence/AppDbContext.cs
    - src/Friday.Infrastructure/Persistence/Repositories/SubjectRepository.cs
    - src/Friday.UI/App.xaml.cs
    - src/Friday.UI/MainWindow.xaml
    - src/Friday.UI/ViewModels/MainViewModel.cs
    - src/Friday.UI/ViewModels/SubjectListViewModel.cs
    - src/Friday.UI/Views/SubjectListView.xaml
    - tests/Friday.Tests/Domain/SubjectTests.cs
    - tests/Friday.Tests/Infrastructure/SubjectRepositoryTests.cs
  modified: []
decisions:
  - "Installed .NET 10 SDK (10.0.301) via winget -- only .NET 9 was available"
  - "Used System.Windows.Application fully qualified to avoid namespace conflict with Friday.Application"
  - "Added .gitignore after first commit to prevent build artifacts in git"
metrics:
  duration_seconds: 992
  completed_at: "2026-06-26T06:52:39Z"
  tasks_completed: 2
  files_created: 19
  tests_passing: 9
---

# Phase 1 Plan 01: Foundation Walking Skeleton Summary

Walking skeleton proving Clean Architecture layers (Domain, Application, Infrastructure, UI) with EF Core + SQLite and WPF Subject management end-to-end.

## Tasks Completed

| Task | Name | Commit | Status |
|------|------|--------|--------|
| 1 | Solution scaffold, domain entities, application interfaces, and infrastructure | b5fa9b4 | Done |
| 2 | Minimal WPF app with Subject list -- proves full stack end-to-end | 492f9de | Done |

## What Was Built

### Domain Layer (Zero Dependencies)
- **Subject** entity: Id, Name, Icon, Color, SortOrder, IsPreset, CreatedAt, UpdatedAt with Chapters navigation
- **Chapter** entity: Id, SubjectId, Name, SortOrder, CreatedAt, UpdatedAt with Subject/KnowledgePoints navigation
- **KnowledgePoint** entity: Id, ChapterId, Name, Description, SortOrder, CreatedAt, UpdatedAt with Chapter/Questions navigation
- **Question** entity: Id, SubjectId?, ChapterId?, KnowledgePointId?, Content, Answer, UserAnswer, ErrorType, ImagePath, Notes, ReviewDate, LeitnerBox (default 1), EaseFactor (default 2.5), IntervalDays (default 1), Streak (default 0), CreatedAt, UpdatedAt
- **ErrorType** enum: Careless, Conceptual, Method, Calculation, Unknown

### Application Layer (Interfaces)
- **ISubjectRepository**: GetAllAsync, GetByIdAsync, GetPresetAsync, AddAsync, UpdateAsync, DeleteAsync, ExistsByNameAsync (7 methods)
- **IQuestionRepository**: GetAllAsync, GetByIdAsync, GetBySubjectAsync, GetByKnowledgePointAsync, GetFilteredAsync, AddAsync, UpdateAsync, DeleteAsync (8 methods)
- **IKnowledgePointRepository**: GetAllAsync, GetByChapterAsync, GetByIdAsync, AddAsync, UpdateAsync, DeleteAsync (6 methods)

### Infrastructure Layer (EF Core + SQLite)
- **AppDbContext**: DbSets for Subject, Chapter, KnowledgePoint, Question with Fluent API configuration
- **SubjectRepository**: Full CRUD implementation with seed data (语文, 数学, 英语)
- SQLite configured with WAL journal mode and foreign keys enabled

### UI Layer (WPF + MVVM)
- **MainWindow.xaml**: Sidebar navigation (200px) + ContentControl layout
- **MainViewModel**: ObservableObject with CurrentViewModel property
- **SubjectListViewModel**: ObservableCollection<Subject> with Add/Delete/Refresh RelayCommands
- **SubjectListView.xaml**: DataGrid with Name, Icon, Color, IsPreset, SortOrder columns + Add/Delete buttons
- **App.xaml.cs**: DI container setup with Serilog logging, database initialization on startup

### Tests (9 Passing)
- Subject entity initialization and default values
- Question LeitnerBox defaults to 1
- ErrorType enum has 5 values
- SubjectRepository CRUD operations (Add, Delete, GetAll, GetPreset, ExistsByName)

## Acceptance Criteria Verification

- [x] Friday.sln builds with zero errors and zero warnings
- [x] Friday.Domain has zero NuGet dependencies (pure domain)
- [x] Subject entity has all 8 properties per D-01
- [x] Question entity has LeitnerBox defaulting to 1 per D-04
- [x] ErrorType enum has 5 values per D-03
- [x] ISubjectRepository defines 7 methods
- [x] AppDbContext configures all 4 DbSets with proper foreign key relationships
- [x] SubjectRepository seeds 3 preset subjects on first run
- [x] SubjectTests pass (entity creation, defaults)
- [x] SubjectRepositoryTests pass (CRUD + seed verification)
- [x] Friday.UI.csproj references Friday.Application and Friday.Infrastructure
- [x] App.xaml.cs configures DI container with AppDbContext, ISubjectRepository, and ViewModels registered
- [x] App.xaml.cs runs database migration on startup (EnsureCreated)
- [x] MainWindow.xaml has sidebar + ContentControl layout
- [x] MainViewModel uses [ObservableProperty] for CurrentViewModel
- [x] SubjectListViewModel loads subjects from ISubjectRepository on init
- [x] SubjectListViewModel has AddSubjectAsync and DeleteSubjectAsync RelayCommands
- [x] SubjectListView.xaml has DataGrid bound to Subjects and Add/Delete buttons
- [x] `dotnet build Friday.sln` succeeds with zero errors
- [x] Unit tests verify SubjectRepository CRUD and seed behavior at runtime

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] .NET 10 SDK not installed**
- **Found during:** Task 1 start
- **Issue:** Only .NET 9 SDK (9.0.313) was available; plan requires .NET 10.0
- **Fix:** Installed .NET 10 SDK (10.0.301) via `winget install Microsoft.DotNet.SDK.10`
- **Files modified:** None (system-level change)
- **Commit:** N/A (prerequisite)

**2. [Rule 1 - Bug] Missing `using Xunit;` in test files**
- **Found during:** Task 1 build
- **Issue:** Test files referenced `[Fact]` attribute without importing Xunit namespace
- **Fix:** Added `using Xunit;` to SubjectTests.cs and SubjectRepositoryTests.cs
- **Files modified:** tests/Friday.Tests/Domain/SubjectTests.cs, tests/Friday.Tests/Infrastructure/SubjectRepositoryTests.cs
- **Commit:** Included in b5fa9b4

**3. [Rule 1 - Bug] Missing `using Friday.Domain.Enums;` in test file**
- **Found during:** Task 1 build
- **Issue:** SubjectTests.cs referenced ErrorType enum without importing the namespace
- **Fix:** Added `using Friday.Domain.Enums;` to SubjectTests.cs
- **Files modified:** tests/Friday.Tests/Domain/SubjectTests.cs
- **Commit:** Included in b5fa9b4

**4. [Rule 1 - Bug] Namespace conflict: Application class vs Friday.Application namespace**
- **Found during:** Task 2 build
- **Issue:** `public partial class App : Application` failed because `Application` resolved to `Friday.Application` namespace instead of `System.Windows.Application`
- **Fix:** Changed to `public partial class App : System.Windows.Application`
- **Files modified:** src/Friday.UI/App.xaml.cs
- **Commit:** Included in 492f9de

**5. [Rule 1 - Bug] Missing `using System.IO;` in App.xaml.cs**
- **Found during:** Task 2 build
- **Issue:** `Path` and `Directory` classes not found without System.IO import
- **Fix:** Added `using System.IO;` to App.xaml.cs
- **Files modified:** src/Friday.UI/App.xaml.cs
- **Commit:** Included in 492f9de

**6. [Rule 2 - Missing critical] Build artifacts committed to git**
- **Found during:** Task 2 commit
- **Issue:** bin/ and obj/ directories were committed (25,899 lines of build output)
- **Fix:** Added .gitignore with standard .NET patterns, removed build artifacts from tracking
- **Files modified:** .gitignore
- **Commit:** 8b2a3bb

## Known Stubs

None - all planned functionality is implemented and working.

## Threat Flags

| Flag | File | Description |
|------|------|-------------|
| threat_flag: T-01-SC | NuGet packages | All packages installed from NuGet.org (verified HIGH confidence per STACK.md) |

## Self-Check: PASSED

- All created files exist in repository
- All commits (b5fa9b4, 492f9de, 8b2a3bb) verified in git log
- Solution builds with zero errors
- All 9 unit tests pass
