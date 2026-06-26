---
phase: 01-foundation
plan: 03
subsystem: ui
tags: [wpf-shell, sidebar-navigation, cartoon-theme, chinese-ui, export-import, auto-backup, mvvm]
dependency_graph:
  requires:
    - phase: 01-foundation
      plan: 01
      provides: [Friday.Domain, Friday.Application, Friday.Infrastructure, Friday.UI, domain-entities, repository-interfaces]
    - phase: 01-foundation
      plan: 02
      provides: [SubjectService, KnowledgeTreeService, SubjectManagementViewModel, KnowledgeTreeViewModel, QuestionListViewModel, AddQuestionViewModel]
  provides:
    - NavigationService for page-based navigation
    - WPF Fluent theme with cartoon overlay (colors, fonts, rounded corners)
    - Chinese-language interface throughout
    - ExportImportService (JSON/CSV export, JSON import with version validation)
    - BackupService (auto-backup, 7-backup retention, AppData storage)
    - HomeView dashboard with stat cards
    - SettingsView with data management access
    - ExportImportView with file dialog UI
  affects: [all-subsequent-plans]
tech_stack:
  added: []
  patterns: [Navigation Service Pattern, ResourceDictionary Theming, File Dialog Pattern, VACUUM INTO Backup]
key_files:
  created:
    - src/Friday.UI/Services/NavigationService.cs
    - src/Friday.UI/ViewModels/HomeViewModel.cs
    - src/Friday.UI/ViewModels/SettingsViewModel.cs
    - src/Friday.UI/ViewModels/ExportImportViewModel.cs
    - src/Friday.UI/Views/HomeView.xaml
    - src/Friday.UI/Views/HomeView.xaml.cs
    - src/Friday.UI/Views/SettingsView.xaml
    - src/Friday.UI/Views/SettingsView.xaml.cs
    - src/Friday.UI/Views/ExportImportView.xaml
    - src/Friday.UI/Views/ExportImportView.xaml.cs
    - src/Friday.UI/Converters/BoolToVisibilityConverter.cs
    - src/Friday.UI/Resources/Themes/CartoonTheme.xaml
    - src/Friday.UI/Resources/Styles/ButtonStyles.xaml
    - src/Friday.UI/Resources/Styles/DataGridStyles.xaml
    - src/Friday.UI/Resources/Styles/WindowStyles.xaml
    - src/Friday.Application/Interfaces/IExportImportService.cs
    - src/Friday.Application/Interfaces/IBackupService.cs
    - src/Friday.Infrastructure/Services/ExportImportService.cs
    - src/Friday.Infrastructure/Services/BackupService.cs
    - tests/Friday.Tests/Infrastructure/ExportImportServiceTests.cs
    - tests/Friday.Tests/Infrastructure/BackupServiceTests.cs
  modified:
    - src/Friday.UI/MainWindow.xaml
    - src/Friday.UI/MainWindow.xaml.cs
    - src/Friday.UI/ViewModels/MainViewModel.cs
    - src/Friday.UI/App.xaml
    - src/Friday.UI/App.xaml.cs
decisions:
  - "Used NavigationService with ViewModelChanged event for page switching instead of code-behind navigation"
  - "Used RadioButton with GroupName for sidebar nav selection state"
  - "Used SQLite VACUUM INTO for atomic backup creation"
  - "Added 100MB file size limit on JSON import to mitigate DoS (T-03-03)"
  - "Used System.Text.Json with JsonDocument for import parsing (no polymorphic deserialization)"
  - "ExportImportView accessible from Settings page via navigation button"
metrics:
  duration_seconds: 900
  completed_at: "2026-06-26T08:30:00Z"
  tasks_completed: 2
  files_created: 21
  tests_passing: 31
---

# Phase 1 Plan 03: WPF Shell, Cartoon Theme, Export/Import, and Auto-Backup Summary

**WPF shell with sidebar navigation, WPF UI Fluent theme with cartoon overlay, Chinese-language interface, data export/import (JSON/CSV), and auto-backup on application close.**

## Tasks Completed

| Task | Name | Commit | Status |
|------|------|--------|--------|
| 1 | Navigation service, main window shell, WPF UI theme, and cartoon overlay | 3ad9c93 | Done |
| 2 | Data export/import (JSON/CSV) and auto-backup system | f453643 | Done |

## What Was Built

### WPF Shell with Sidebar Navigation (Task 1)
- **NavigationService**: Resolves ViewModels from DI, fires ViewModelChanged event for page switching
- **MainViewModel**: 5 navigation commands (Home, Subjects, Questions, Review, Settings) with Chinese page titles
- **MainWindow.xaml**: 240px sidebar with Segoe Fluent Icons + Chinese labels, ContentControl for page switching, page title bar
- **HomeView**: Welcome message, 3 stat cards (total subjects, total questions, due reviews), disabled quick-action buttons
- **SettingsView**: App info (version, data path), AI config placeholder, data management navigation
- **BoolToVisibilityConverter**: Standard converter with Invert parameter support

### Cartoon Theme (Task 1)
- **CartoonTheme.xaml**: Color palette (Primary=#4A90D9, Secondary=#7ED321, Accent=#F5A623), CornerRadius 12px default, Microsoft YaHei font family, page fade animations (200ms)
- **ButtonStyles.xaml**: Cartoon primary/secondary/accent/danger button styles with rounded corners, hover/pressed/disabled states
- **DataGridStyles.xaml**: Alternating row colors, cartoon-friendly spacing, styled headers
- **WindowStyles.xaml**: Card, stat card, section title, subsection title, body text, secondary text, input styles

### Data Export/Import (Task 2)
- **IExportImportService**: ExportToJsonAsync, ExportToCsvAsync, ImportFromJsonAsync, GetExportSummaryAsync
- **ExportImportService**: JSON export with version=1 field, CSV export with semicolon separator, import with version validation before clearing data
- **ExportImportViewModel**: File dialog integration, Chinese status messages, overwrite warning
- **ExportImportView**: Export JSON/CSV buttons, import JSON button with warning, summary display

### Auto-Backup System (Task 2)
- **IBackupService**: BackupAsync, GetBackupListAsync, RestoreFromBackupAsync, CleanupOldBackupsAsync
- **BackupService**: SQLite VACUUM INTO for atomic backups, 7-backup retention (D-16), AppData storage (D-17)
- **Auto-backup on exit**: App.xaml.cs OnExit triggers backup + cleanup (D-15)
- **Backup directory**: Created on startup at %AppData%/SmartWrongAnswerBook/Backups/

### Tests (10 new, 31 total)
- **ExportImportServiceTests** (6 tests): JSON format with version field, export all entities, reject missing version, reject unsupported version, round-trip preservation, no data modification on invalid version
- **BackupServiceTests** (4 tests): backup file creation, cleanup keeps 7, ordered list, cleanup no-op when <7

## Decisions Made

- **NavigationService pattern**: ViewModelChanged event-based navigation instead of code-behind switching. Cleaner separation, reusable across pages.
- **RadioButton sidebar nav**: Used RadioButton with GroupName="SidebarNav" for mutual exclusion, custom template for cartoon look.
- **VACUUM INTO for backups**: SQLite's atomic backup command ensures clean, consistent copies without locking the main database.
- **100MB import limit**: File size validation before JSON parsing to mitigate DoS attacks (T-03-03).
- **ExportImport in Settings**: Accessible via "Data Management" button in Settings page rather than as a top-level nav item.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Missing `using Xunit;` in test files**
- **Found during:** Task 2 build
- **Issue:** Both ExportImportServiceTests.cs and BackupServiceTests.cs missing `using Xunit;` import
- **Fix:** Added `using Xunit;` to both files
- **Files modified:** tests/Friday.Tests/Infrastructure/ExportImportServiceTests.cs, tests/Friday.Tests/Infrastructure/BackupServiceTests.cs
- **Commit:** Included in f453643

**2. [Rule 1 - Bug] ErrorType enum deserialized as number, not string**
- **Found during:** Task 2 test run (RoundTrip test failed)
- **Issue:** System.Text.Json serializes enums as integers by default. ImportFromJsonAsync used GetString() on ErrorType which is a number in JSON.
- **Fix:** Added ValueKind check -- if string, use Enum.Parse; if number, cast from int
- **Files modified:** src/Friday.Infrastructure/Services/ExportImportService.cs
- **Commit:** Included in f453643

## Known Stubs

None - all planned functionality is implemented and working.

## Threat Flags

| Flag | File | Description |
|------|------|-------------|
| threat_flag: T-03-01 | ExportImportService.cs | JSON import validates version field before processing (mitigated per D-19) |
| threat_flag: T-03-03 | ExportImportViewModel.cs | 100MB file size limit on import to prevent DoS (mitigated) |
| threat_flag: T-03-04 | BackupService.cs | VACUUM INTO creates atomic backup copy; file existence and size > 0 verified (mitigated) |

## Self-Check

- All 21 created files exist in repository
- All 2 task commits (3ad9c93, f453643) verified in git log
- Solution builds with zero errors and zero warnings
- All 31 unit tests pass
- Sidebar navigation with 5 Chinese-labeled items works
- Cartoon theme: Primary=#4A90D9, Secondary=#7ED321, Accent=#F5A623, CornerRadius=12px, Microsoft YaHei font
- Export JSON produces version=1 field with all entities
- Import validates version before modifying data
- Auto-backup on app close with 7-backup retention
- All visible UI strings in Chinese

## Next Phase Readiness

- Phase 1 complete: all 3 plans delivered (walking skeleton, subject/KP/question management, shell + data safety)
- Ready for Phase 2: Camera capture and image processing (OpenCvSharp4)
- All DATA-02 through DATA-05, UI-01, UI-02, UI-05 requirements satisfied

---
*Phase: 01-foundation*
*Completed: 2026-06-26*
