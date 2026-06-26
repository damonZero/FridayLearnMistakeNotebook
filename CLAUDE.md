<!-- GSD:project-start source:PROJECT.md -->

## Project

**Smart Wrong Answer Book (智能错题本)**

A desktop learning assistant for elementary school students (grades 1-6), designed to help parents record and manage their child's wrong answers through photo capture. The app uses AI to recognize questions from photos, analyze knowledge gaps, and create personalized review schedules based on proven learning theories. Currently covers Chinese, Math, and English with dynamic subject management.

**Core Value:** Accurate photo-based wrong answer capture with AI-powered knowledge analysis that turns mistakes into targeted learning opportunities through scientifically-backed spaced repetition.

### Constraints

- **Tech Stack**: C# — user's primary language, from Unity background
- **Platform**: Windows desktop first, single-machine local version
- **Data**: Local storage only for v1; cloud sync planned for future
- **AI**: API-based (not local models) — requires internet for AI features
- **UI**: Must be child-friendly (cute cartoon style) but functional for parents

<!-- GSD:project-end -->

<!-- GSD:stack-start source:research/STACK.md -->

## Technology Stack

## Recommended Stack

### Core Runtime

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| .NET | 10.0 LTS (SDK 10.0.301) | Application runtime | Latest LTS release with 3 years of support. Even-numbered .NET releases are LTS. Avoid .NET 9 (STS, EOL). |
| C# | 13 | Language | Ships with .NET 10. Source generators, file-scoped namespaces, primary constructors. |

### UI Framework

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| WPF | Built into .NET | UI framework | Windows-only matches requirement. Mature, stable, massive ecosystem. XAML data binding is excellent. VS designer support. User's Unity/XAML experience transfers directly. |
| WPF UI (Lepoco) | 4.3.0 | Modern Fluent controls | Adds Fluent Design System to WPF: modern navigation, themes (Light/Dark), acrylic/mica effects. Without this, WPF looks dated out of the box. |

- Project is Windows-only. Avalonia's cross-platform advantage is irrelevant.
- WPF has VS designer support; Avalonia does not (AvaloniaRider only for JetBrains Rider).
- WPF's ecosystem of third-party controls, tutorials, and StackOverflow answers is 10x larger.
- WPF UI (Lepoco) brings modern Fluent design without leaving WPF.
- Camera capture via Windows-native APIs (MediaCapture, DirectShow) integrates more naturally with WPF than Avalonia's cross-platform abstraction layer.
- Avalonia 12.0.5 is excellent for cross-platform, but adds unnecessary abstraction for a Windows-only app.
- Smaller ecosystem, fewer third-party controls for the "cute cartoon" UI work.
- No VS designer -- slower iteration for XAML-heavy UI work.
- WinUI 3 (Windows App SDK) is Microsoft's "modern" framework but has a smaller ecosystem than WPF.
- Rough edges, still evolving, less mature tooling.
- WPF + WPF UI gets you 90% of WinUI 3's look with 10x the community resources.
- .NET MAUI targets mobile + desktop but has well-documented Windows desktop issues.
- Its strength is cross-platform mobile; for desktop-only WPF is superior.

### MVVM Architecture

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| CommunityToolkit.Mvvm | 8.4.2 | MVVM framework | Microsoft's official MVVM toolkit. Source generators for `[ObservableProperty]`, `[RelayCommand]`. No boilerplate. MIT licensed. Lightweight -- no framework lock-in. |

- CommunityToolkit.Mvvm is Microsoft-maintained, the "official" recommendation.
- Source generators eliminate 80% of MVVM boilerplate (`INotifyPropertyChanged`, `ICommand`).
- Works with any UI framework (WPF, Avalonia, MAUI) -- no vendor lock-in.
- Prism is heavier (DI container, navigation, modules) -- overkill for a single-window desktop app.
- ReactiveUI has a steeper learning curve (reactive extensions) and is less mainstream.

### Database

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| EF Core + SQLite | 10.0.9 | ORM + local database | EF Core is the standard .NET ORM. SQLite is perfect for local-first desktop apps: single file, zero config, embedded. EF Core handles migrations, LINQ queries, change tracking. |
| SQLitePCLRaw.bundle_e_sqlite3 | 2.1.10+ | SQLite native bindings | Required by EF Core SQLite provider. Bundles the native SQLite engine. |

- EF Core's migration system is critical -- schema will evolve as features are added (subjects, knowledge points, review schedules).
- LINQ queries are natural for the complex data model (questions, subjects, knowledge points, review history).
- Change tracking simplifies save/update logic.
- Dapper is faster but requires hand-written SQL for every query -- maintenance burden for a solo developer.
- sqlite-net is too lightweight -- no migrations, no navigation properties.
- Use WAL journal mode: `PRAGMA journal_mode=WAL;` (better read concurrency)
- Enable foreign keys: `PRAGMA foreign_keys=ON;`
- Store DB in AppData: `Environment.SpecialFolder.ApplicationData`
- Use `VACUUM INTO` for backup functionality
- Connection string: `Data Source={appData}\SmartWrongAnswerBook\app.db`

### Camera Capture

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| OpenCvSharp4 | 4.13.0.20260602 | Camera capture + image preprocessing | Wraps OpenCV 4.13. Mature, well-documented. Handles webcam capture, frame grabbing, basic image operations (resize, crop, rotate, denoise). |
| OpenCvSharp4.runtime.win | 4.13.0.20260602 | Windows native runtime | Required Windows-specific native binaries for OpenCvSharp. |

- OpenCvSharp is the most popular OpenCV wrapper for .NET (28K+ downloads on NuGet).
- Actively maintained, tracks OpenCV releases closely.
- Cleaner API than Emgu CV (which requires separate `Emgu.CV.runtime.{platform}` packages).
- AForge.NET is effectively abandoned (last update years ago).
- Use `VideoCapture` class for webcam access.
- Capture frames in a background thread, push to UI via `BitmapImage`.
- Pre-capture: auto-focus, white balance (if supported by webcam).
- Post-capture: crop region selection via draggable overlay on the captured frame.

### Image Processing

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| SixLabors.ImageSharp | 4.0.0 | Image manipulation (crop, enhance, resize) | Fully managed, cross-platform, no native dependencies. Excellent for post-capture processing: crop, brightness/contrast adjustment, sharpening, format conversion. |
| SkiaSharp | 4.148.0 | 2D graphics rendering | Skia-based rendering for custom cartoon UI elements, SVG rendering, Lottie animations. Powers the "cute" visual style. |

- ImageSharp is fully managed (no GDI+ dependency, no native DLLs).
- `System.Drawing` is Windows-only and has known memory leak issues.
- Magick.NET (ImageMagick) is powerful but heavy -- overkill for crop/enhance.
- ImageSharp has an excellent fluent API: `image.Mutate(x => x.Resize(...).Grayscale())`.
- Essential for the cartoon UI: custom-drawn controls, SVG character assets, smooth animations.
- Avalonia uses Skia internally; even in WPF, SkiaSharp gives you the same rendering power.
- Can render SVG files directly (via Svg.Skia if needed).

### AI/OCR Integration

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| System.Net.Http (HttpClient) | Built into .NET | HTTP client for AI API calls | Built-in, no additional dependency. Use `IHttpClientFactory` via `Microsoft.Extensions.Http` for proper lifecycle management. |
| Microsoft.Extensions.Http | 10.0.9 | HttpClient factory | Manages `HttpClient` lifetimes, prevents socket exhaustion. Register named/typed clients per AI provider. |
| System.Text.Json | 10.0.9 | JSON serialization | Built into .NET, fastest JSON serializer for .NET. No need for Newtonsoft.Json. |
| Polly | 8.7.0 | Resilience/retry policies | AI APIs are unreliable (rate limits, timeouts, transient failures). Polly provides retry with exponential backoff, circuit breaker, timeout policies. |

- Each AI task (OCR, analysis, question generation) is a separate service implementing a common `IAiService` interface.
- Per-task model configuration: each task stores its own API endpoint, model name, and API key.
- Use `IHttpClientFactory` with typed clients: `OcrClient`, `AnalysisClient`, `QuestionGenClient`.
- Polly wraps each client with retry (3 attempts, exponential backoff) and timeout (30s).
- The app must support multiple AI providers (not just OpenAI).
- Raw HTTP + typed clients give maximum flexibility.
- AI provider APIs are simple REST endpoints -- no SDK complexity needed.

### Dependency Injection & Configuration

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Microsoft.Extensions.DependencyInjection | 10.0.9 | DI container | Built-in .NET DI. Lightweight, sufficient for desktop app scope. |
| Microsoft.Extensions.Configuration | 10.0.9 | App configuration | JSON + user secrets for API keys. Supports `appsettings.json` with environment overrides. |
| Microsoft.Extensions.Logging | 10.0.9 | Logging abstractions | Standard logging interface. |
| Serilog | 4.3.1 | Structured logging | File + console sinks. Structured logging for debugging AI calls, review scheduling, etc. |
| Serilog.Sinks.File | 6.0.0+ | File sink | Log to `%AppData%\SmartWrongAnswerBook\logs\`. |

### Data Backup

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| System.IO.Compression | Built into .NET | ZIP backup archives | Bundle SQLite DB + exported data into a ZIP for backup/restore. |
| (No additional library needed) | | | SQLite's `VACUUM INTO` creates a clean DB copy; ZIP it for portability. |

### Testing

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| xUnit | 2.9.0+ | Unit testing | Standard .NET test framework. |
| Moq | 4.20.0+ | Mocking | Mock AI services, database, camera for unit tests. |
| FluentAssertions | 7.0.0+ | Assertion library | Readable test assertions. |
| Microsoft.EntityFrameworkCore.Sqlite (in-memory) | 10.0.9 | In-memory DB for tests | Use SQLite in-memory mode for integration tests. |

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| UI Framework | WPF + WPF UI | Avalonia 12.0.5 | Cross-platform unnecessary. WPF has larger ecosystem, VS designer, better Windows camera integration. |
| UI Framework | WPF + WPF UI | WinUI 3 | Smaller ecosystem, rough edges, less mature tooling. WPF + WPF UI achieves similar modern look. |
| UI Framework | WPF + WPF UI | .NET MAUI | MAUI's Windows desktop support has known issues. MAUI targets mobile+desktop; desktop-only WPF is superior. |
| MVVM | CommunityToolkit.Mvvm | Prism | Prism is heavier (DI, navigation, modules) -- overkill for single-window desktop app. |
| MVVM | CommunityToolkit.Mvvm | ReactiveUI | Steeper learning curve (Rx extensions), less mainstream. |
| ORM | EF Core | Dapper | Dapper requires hand-written SQL everywhere. EF Core's migrations and LINQ are worth the slight overhead. |
| ORM | EF Core | sqlite-net | Too lightweight -- no migrations, no navigation properties. |
| Camera | OpenCvSharp4 | Emgu CV | Emgu CV requires separate runtime packages per platform. OpenCvSharp has cleaner API. |
| Camera | OpenCvSharp4 | AForge.NET | Effectively abandoned. |
| Image Processing | ImageSharp | System.Drawing | System.Drawing has memory leaks, Windows-only, GDI+ dependency. |
| Image Processing | ImageSharp | Magick.NET | Heavy native dependency. Overkill for crop/enhance. |
| JSON | System.Text.Json | Newtonsoft.Json | System.Text.Json is built-in, faster, and sufficient. No need for Newtonsoft. |
| HTTP | HttpClient + Polly | RestSharp | RestSharp is a wrapper around HttpClient. Direct HttpClient + Polly is cleaner, fewer dependencies. |
| Logging | Serilog | NLog | Both are fine. Serilog has better structured logging and is more popular in modern .NET. |

## Installation

# Create project

# Core packages

# Camera & Image Processing

# AI Integration

# DI, Config, Logging

# Dev dependencies

## NuGet Package Summary

| Package | Version | License | Confidence |
|---------|---------|---------|------------|
| WPF-UI | 4.3.0 | MIT | HIGH |
| CommunityToolkit.Mvvm | 8.4.2 | MIT | HIGH |
| Microsoft.EntityFrameworkCore.Sqlite | 10.0.9 | MIT | HIGH |
| OpenCvSharp4 | 4.13.0.20260602 | Apache-2.0 | HIGH |
| OpenCvSharp4.runtime.win | 4.13.0.20260602 | Apache-2.0 | HIGH |
| SixLabors.ImageSharp | 4.0.0 | Six Labors Split License | HIGH |
| SkiaSharp | 4.148.0 | MIT | HIGH |
| Microsoft.Extensions.Http | 10.0.9 | MIT | HIGH |
| Polly | 8.7.0 | BSD-3-Clause | HIGH |
| Serilog | 4.3.1 | Apache-2.0 | HIGH |

## Sources

- NuGet.org -- package versions verified directly from NuGet pages (accessed 2026-06-26)
- Microsoft .NET Download page -- .NET 10.0 LTS confirmed as current (accessed 2026-06-26)
- Context7 documentation for WPF, Avalonia, ImageSharp, CommunityToolkit.Mvvm, OpenCvSharp, SkiaSharp
- Avalonia docs (docs.avaloniaui.net) -- platform support and features confirmed
- WPF UI docs (lepoco/wpfui GitHub) -- theming and setup confirmed

<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->

## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->

## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->

## Project Skills

No project skills found. Add skills to any of: `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, or `.codex/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->

## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:

- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->

<!-- GSD:profile-start -->

## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
