# Smart Wrong Answer Book (智能错题本)

## What This Is

A desktop learning assistant for elementary school students (grades 1-6), designed to help parents record and manage their child's wrong answers through photo capture. The app uses AI to recognize questions from photos, analyze knowledge gaps, and create personalized review schedules based on proven learning theories. Currently covers Chinese, Math, and English with dynamic subject management.

## Core Value

Accurate photo-based wrong answer capture with AI-powered knowledge analysis that turns mistakes into targeted learning opportunities through scientifically-backed spaced repetition.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Photo capture with in-app camera, crop, and image enhancement
- [ ] AI-powered OCR for question recognition (printed and handwritten)
- [ ] Subject management (preset: Chinese, Math, English + dynamic add/delete)
- [ ] Knowledge point analysis and auto-categorization
- [ ] Similar question generation for practice
- [ ] Smart review scheduling (Ebbinghaus + Leitner + Spaced Repetition)
- [ ] Daily review task push + on-demand review mode
- [ ] Per-task AI model configuration (OCR/analysis/question generation)
- [ ] Local data storage with auto-backup
- [ ] Cute cartoon UI style suitable for elementary students

### Out of Scope

- Multi-user/multi-child support — single child only for v1
- Server/cloud deployment — local-first, server version planned for future
- Mobile app — desktop only for v1
- Social features — no sharing or collaboration

## Context

- **User**: Parent-assisted workflow; parent captures and manages, child reviews
- **Environment**: Home computer as primary device; most usage at home
- **Tech Stack**: C# desktop application (user has Unity/C# experience)
- **Learning Theories**: Ebbinghaus forgetting curve, Leitner box system, spaced repetition with progressive difficulty
- **AI Integration**: Multiple AI models via API keys, each task (OCR, analysis, question generation) can use different providers

## Constraints

- **Tech Stack**: C# — user's primary language, from Unity background
- **Platform**: Windows desktop first, single-machine local version
- **Data**: Local storage only for v1; cloud sync planned for future
- **AI**: API-based (not local models) — requires internet for AI features
- **UI**: Must be child-friendly (cute cartoon style) but functional for parents

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| C# desktop app | User expertise in C#/Unity, no web dev experience | — Pending |
| Local-first architecture | No server costs, simpler deployment, home-use scenario | — Pending |
| Per-task AI model config | Flexibility to use best model for each task (OCR vs analysis vs generation) | — Pending |
| Three learning theories combined | Comprehensive coverage: forgetting curve (timing), Leitner (progression), spaced repetition (method) | — Pending |
| AI auto-identify + manual correction for knowledge points | Balance between automation accuracy and user control | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-06-26 after initialization*
