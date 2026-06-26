# Project Research Summary

**Project:** Smart Wrong Answer Book
**Domain:** EdTech - Desktop Learning Application for Elementary Students
**Researched:** 2026-06-26
**Confidence:** MEDIUM-HIGH

## Executive Summary

The Smart Wrong Answer Book is a Windows-only desktop application that helps parents of elementary students (ages 6-12) capture, organize, and review wrong answers from homework and tests. The core value proposition is a photo-to-review loop: photograph a wrong answer, OCR extracts the text, AI identifies the knowledge point, and a spaced repetition algorithm schedules scientifically-timed reviews. Research indicates this should be built as a local-first WPF application with a cute cartoon UI for children and a clean management interface for parents.

The recommended stack is .NET 10 LTS + WPF + WPF UI (Lepoco) for the UI layer, CommunityToolkit.Mvvm for MVVM, EF Core + SQLite for persistence, OpenCvSharp4 for camera capture, and HttpClient + Polly for AI API integration. This stack is well-suited because WPF is the mature standard for Windows desktop apps, the user XAML experience transfers directly, and WPF UI brings modern Fluent design without leaving the WPF ecosystem. The architecture follows Clean Architecture + MVVM with clear layer separation: Domain (entities, business rules), Application (use cases, interfaces), Infrastructure (SQLite, AI APIs, camera), and Presentation (WPF views/viewmodels).

The single biggest risk is OCR accuracy for handwritten Chinese characters and math notation from student homework. If OCR is unreliable, the entire core value proposition collapses. This must be mitigated by designing the correction UI as a first-class feature from day one, not an afterthought. Other critical pitfalls include spaced repetition algorithm bugs (study Anki implementation), UI thread blocking during AI calls (async-first pattern), and database migration strategy (implement before first table is created). The project should ship as single-user, local-first, with cloud sync and multi-user deferred to future versions.
