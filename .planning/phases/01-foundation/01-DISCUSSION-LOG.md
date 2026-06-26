# Phase 1: Foundation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-26
**Phase:** 1-Foundation
**Areas discussed:** UI Framework, Domain Entity Design, Navigation, Cartoon UI, Backup Strategy

---

## UI Framework

| Option | Description | Selected |
|--------|-------------|----------|
| WPF + WPF UI (Recommended) | Native Windows desktop, lightweight, good system integration | ✓ |
| Unity UGUI | User familiar with Unity, natural cartoon UI, but heavy and slow | |
| WPF first, Unity optional | Start with WPF, evaluate Unity for animations later | |

**User's choice:** WPF + WPF UI (Recommended)
**Notes:** User has Unity background but chose WPF for this project. User said "非Unity我就不熟悉了，你自己把控吧" (not familiar with non-Unity, you decide).

---

## Domain Entity Design

**User's choice:** Claude decided (user deferred)

**Decisions:**
- 4 core entities: Subject, Chapter, KnowledgePoint, Question
- Hierarchical: Subject > Chapter > KnowledgePoint
- Question flexible association (can link to any level)
- ErrorType enum: Careless, Conceptual, Method, Calculation, Unknown
- Leitner box system: 5 boxes, default box 1

---

## WPF Navigation

**User's choice:** Claude decided (user deferred)

**Decisions:**
- Sidebar navigation with icon + text labels
- Main items: Home, Subjects, Questions, Review, Settings
- Hamburger menu for settings
- Content area fills remaining space

---

## Cartoon UI

**User's choice:** Claude decided (user deferred)

**Decisions:**
- Base: WPF UI Fluent Design
- Cartoon overlay: soft colors, large border radius
- Icons: Segoe Fluent Icons + custom SVG
- Font: Microsoft YaHei
- Color scheme: Primary=#4A90D9, Secondary=#7ED321, Accent=#F5A623

---

## Backup Strategy

**User's choice:** Claude decided (user deferred)

**Decisions:**
- Auto-backup on app close
- Retention: last 7 backups
- Location: %AppData%/SmartWrongAnswerBook/Backups/
- Export: JSON with version field
- Import: validate version, show diff summary

---

## Claude's Discretion

- Database schema design and migration strategy
- DI container configuration
- Async/await patterns
- Error handling and logging
- Unit test structure

## Deferred Ideas

None — discussion stayed within phase scope
