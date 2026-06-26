---
phase: 01-foundation
plan: 02
subsystem: ui
tags: [subject-crud, knowledge-tree, question-form, filter-search-sort, mvvm, wpf]
dependency_graph:
  requires:
    - phase: 01-foundation
      plan: 01
      provides: [Friday.Domain, Friday.Application, Friday.Infrastructure, Friday.UI, domain-entities, repository-interfaces]
  provides:
    - SubjectService with preset protection
    - KnowledgeTreeService for hierarchical operations
    - ChapterRepository and KnowledgePointRepository
    - QuestionRepository with filtering
    - SubjectManagementViewModel with CRUD
    - KnowledgeTreeViewModel with three-panel hierarchy
    - QuestionListViewModel with filter/search/sort
    - AddQuestionViewModel with form validation
  affects: [01-foundation-plan-03, 02-features]
tech_stack:
  added: [Moq 4.20.72]
  patterns: [Service Layer, Three-Panel Navigation, Filter Pattern, Form Validation]
key_files:
  created:
    - src/Friday.Application/Interfaces/IChapterRepository.cs
    - src/Friday.Application/Services/SubjectService.cs
    - src/Friday.Application/Services/KnowledgeTreeService.cs
    - src/Friday.Infrastructure/Persistence/Repositories/ChapterRepository.cs
    - src/Friday.Infrastructure/Persistence/Repositories/KnowledgePointRepository.cs
    - src/Friday.Infrastructure/Persistence/Repositories/QuestionRepository.cs
    - src/Friday.UI/ViewModels/SubjectManagementViewModel.cs
    - src/Friday.UI/ViewModels/KnowledgeTreeViewModel.cs
    - src/Friday.UI/ViewModels/QuestionListViewModel.cs
    - src/Friday.UI/ViewModels/AddQuestionViewModel.cs
    - src/Friday.UI/Views/SubjectManagementView.xaml
    - src/Friday.UI/Views/SubjectManagementView.xaml.cs
    - src/Friday.UI/Views/KnowledgeTreeView.xaml
    - src/Friday.UI/Views/KnowledgeTreeView.xaml.cs
    - src/Friday.UI/Views/QuestionListView.xaml
    - src/Friday.UI/Views/QuestionListView.xaml.cs
    - src/Friday.UI/Views/AddQuestionView.xaml
    - src/Friday.UI/Views/AddQuestionView.xaml.cs
    - tests/Friday.Tests/Application/SubjectServiceTests.cs
    - tests/Friday.Tests/Infrastructure/KnowledgePointRepositoryTests.cs
  modified:
    - src/Friday.Application/Interfaces/IKnowledgePointRepository.cs
    - src/Friday.Application/Interfaces/IQuestionRepository.cs
    - src/Friday.UI/App.xaml
    - src/Friday.UI/App.xaml.cs
    - src/Friday.UI/MainWindow.xaml
    - src/Friday.UI/MainWindow.xaml.cs
    - tests/Friday.Tests/Friday.Tests.csproj
key_decisions:
  - "Used service layer (SubjectService, KnowledgeTreeService) for business logic instead of putting it in ViewModels"
  - "Added ErrorType? parameter to IQuestionRepository.GetFilteredAsync for SUBJ-06 compliance"
  - "Used inline edit pattern for SubjectManagement instead of dialog popups"
  - "Used Moq for service-layer unit tests (SubjectServiceTests)"
requirements_completed: [SUBJ-01, SUBJ-02, SUBJ-03, SUBJ-04, SUBJ-05, SUBJ-06, SUBJ-07]

duration: 12min
completed: 2026-06-26
---

# Phase 1 Plan 02: Subject CRUD, Knowledge Tree, Question Form, and Filter/Search/Sort Summary

**Subject CRUD with preset protection, hierarchical knowledge tree browser, Add Question form with ErrorType/LeitnerBox defaults, and question list with subject/date/error-type filters and sort commands**

## Performance

- **Duration:** ~12 min
- **Started:** 2026-06-26
- **Completed:** 2026-06-26
- **Tasks:** 3
- **Files created/modified:** 27

## Accomplishments

- SubjectService with preset protection (Chinese/Math/English cannot be deleted or modified)
- KnowledgeTreeService for hierarchical Subject > Chapter > Knowledge Point operations
- ChapterRepository and KnowledgePointRepository with ordered queries
- QuestionRepository with filtered query supporting subject, date range, ErrorType, and sort
- SubjectManagementViewModel with add/edit/delete commands and preset guard
- KnowledgeTreeViewModel with three-panel hierarchy navigation (auto-loads on selection)
- QuestionListViewModel with filter bar (subject, date range, ErrorType) and sort (date, review count, mastery)
- AddQuestionViewModel with form validation, ErrorType enum population, and LeitnerBox defaults
- All 21 unit tests passing (9 original + 7 SubjectService + 5 KnowledgePointRepository)

## Task Commits

Each task was committed atomically:

1. **Task 1: Application services, Chapter repository, and knowledge tree service** - `17cedf3` (feat)
2. **Task 2: Knowledge tree ViewModels, question list with filter/search/sort, and UI wiring** - `544989a` (feat)
3. **Task 3: Add Question form with ErrorType and LeitnerBox defaults** - `a147820` (feat)

## Files Created/Modified

- `src/Friday.Application/Interfaces/IChapterRepository.cs` - 6-method chapter repository interface
- `src/Friday.Application/Services/SubjectService.cs` - Business logic: preset protection, name validation, idempotent seeding
- `src/Friday.Application/Services/KnowledgeTreeService.cs` - Hierarchical operations: chapter/KP CRUD with cascade delete
- `src/Friday.Infrastructure/Persistence/Repositories/ChapterRepository.cs` - EF Core implementation with SortOrder ordering
- `src/Friday.Infrastructure/Persistence/Repositories/KnowledgePointRepository.cs` - With GetBySubjectAsync traversal
- `src/Friday.Infrastructure/Persistence/Repositories/QuestionRepository.cs` - GetFilteredAsync with ErrorType/sort support
- `src/Friday.UI/ViewModels/SubjectManagementViewModel.cs` - CRUD with preset guard, inline edit
- `src/Friday.UI/ViewModels/KnowledgeTreeViewModel.cs` - Three-panel hierarchy with auto-load on selection
- `src/Friday.UI/ViewModels/QuestionListViewModel.cs` - Filter/search/sort with ApplyFiltersAsync
- `src/Friday.UI/ViewModels/AddQuestionViewModel.cs` - Form with validation, ErrorType enum, LeitnerBox defaults
- `src/Friday.UI/Views/SubjectManagementView.xaml` - Add/edit/delete DataGrid UI
- `src/Friday.UI/Views/KnowledgeTreeView.xaml` - Three-panel layout (Subject/Chapter/KP)
- `src/Friday.UI/Views/QuestionListView.xaml` - Filter bar with dropdowns and date pickers
- `src/Friday.UI/Views/AddQuestionView.xaml` - Form with all Chinese labels
- `src/Friday.Application/Interfaces/IKnowledgePointRepository.cs` - Added GetBySubjectAsync method
- `src/Friday.Application/Interfaces/IQuestionRepository.cs` - Added ErrorType? parameter to GetFilteredAsync
- `src/Friday.UI/App.xaml` - Added DataTemplates for new ViewModels
- `src/Friday.UI/App.xaml.cs` - Registered new services, repositories, and ViewModels
- `src/Friday.UI/MainWindow.xaml` - Added Knowledge Tree and Add Question navigation buttons
- `src/Friday.UI/MainWindow.xaml.cs` - Added click handlers for new navigation
- `tests/Friday.Tests/Application/SubjectServiceTests.cs` - 7 tests for SubjectService
- `tests/Friday.Tests/Infrastructure/KnowledgePointRepositoryTests.cs` - 5 tests for KnowledgePointRepository
- `tests/Friday.Tests/Friday.Tests.csproj` - Added Moq package reference

## Decisions Made

- **Service layer pattern:** Business logic (preset protection, name validation, cascade delete) lives in services, not ViewModels. ViewModels are thin wrappers that call services.
- **ErrorType filtering:** Added ErrorType? parameter to IQuestionRepository.GetFilteredAsync to satisfy SUBJ-06 requirement for error type search.
- **Inline edit for subjects:** Used toggle-based inline editing (IsEditing flag + EditSubjectName) instead of dialog popups to keep the UI simple.
- **Moq for service tests:** Added Moq to test project for mocking repository interfaces in SubjectServiceTests.
- **Auto-load on selection:** KnowledgeTreeViewModel uses partial void OnSelectedSubjectChanged/OnSelectedChapterChanged to auto-load child collections.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Missing QuestionRepository implementation**
- **Found during:** Task 2 (QuestionListViewModel creation)
- **Issue:** IQuestionRepository existed but no implementation was created in Plan 01-01
- **Fix:** Created QuestionRepository with full CRUD + GetFilteredAsync supporting subject, date range, ErrorType, and sort
- **Files modified:** src/Friday.Infrastructure/Persistence/Repositories/QuestionRepository.cs
- **Verification:** Build succeeds, all tests pass
- **Committed in:** 544989a (Task 2 commit)

**2. [Rule 3 - Blocking] Missing Moq package for service tests**
- **Found during:** Task 1 (SubjectServiceTests creation)
- **Issue:** SubjectServiceTests uses Moq.Mock but Moq was not in Friday.Tests.csproj
- **Fix:** Added Moq 4.20.72 package reference to Friday.Tests.csproj
- **Files modified:** tests/Friday.Tests/Friday.Tests.csproj
- **Verification:** Tests compile and pass
- **Committed in:** 17cedf3 (Task 1 commit)

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Both were prerequisites for planned functionality. No scope creep.

## Issues Encountered

None - plan executed smoothly after resolving the two blocking issues above.

## Known Stubs

None - all planned functionality is implemented and working.

## Threat Flags

| Flag | File | Description |
|------|------|-------------|
| threat_flag: T-02-01 | SubjectService.cs, KnowledgeTreeService.cs | Input validation added: non-empty names, max length 200 chars (via EF Core fluent config) |
| threat_flag: T-02-02 | SubjectService.cs, SubjectManagementViewModel.cs | Preset protection enforced in both service layer and UI (defense in depth) |

## Self-Check

- All 20 created files exist in repository
- All 3 task commits (17cedf3, 544989a, a147820) verified in git log
- Solution builds with zero errors
- All 21 unit tests pass
- Subject CRUD works with preset protection
- Knowledge tree browses Subject > Chapter > Knowledge Point
- Add Question form creates Question with ErrorType and LeitnerBox=1
- Question list filters by subject, date range, error type
- Question list sorts by date, review count, mastery

## Next Phase Readiness

- Subject management, knowledge tree, and question entry are fully functional
- Ready for Plan 03 (cartoon UI theming) and Phase 2 (photo capture, AI integration)
- All SUBJ-01 through SUBJ-07 requirements satisfied

---
*Phase: 01-foundation*
*Completed: 2026-06-26*
