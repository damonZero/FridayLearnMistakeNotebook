# Requirements: Smart Wrong Answer Book (智能错题本)

**Defined:** 2026-06-26
**Core Value:** Accurate photo-based wrong answer capture with AI-powered knowledge analysis that turns mistakes into targeted learning opportunities through scientifically-backed spaced repetition.

## v1 Requirements

### Photo Capture & OCR (拍照与识别)

- [ ] **PHOTO-01**: User can capture photos using in-app camera
- [ ] **PHOTO-02**: User can crop and rotate captured images before processing
- [ ] **PHOTO-03**: System recognizes printed Chinese text from photos via OCR
- [ ] **PHOTO-04**: System recognizes handwritten Chinese text from photos via OCR
- [ ] **PHOTO-05**: System recognizes math expressions (fractions, exponents, equations) from photos
- [ ] **PHOTO-06**: System recognizes English text from photos via OCR
- [ ] **PHOTO-07**: User can manually edit OCR-recognized text to correct errors
- [ ] **PHOTO-08**: User can import multiple photos from a single test paper, auto-cropping individual questions
- [ ] **PHOTO-09**: User can import PDF test papers and extract questions directly

### Subject & Question Management (科目与题目管理)

- [x] **SUBJ-01**: System presets Chinese, Math, and English as default subjects
- [x] **SUBJ-02**: User can add custom subjects dynamically
- [x] **SUBJ-03**: User can delete subjects (with confirmation)
- [x] **SUBJ-04**: System supports hierarchical knowledge tree: Subject > Chapter > Knowledge Point
- [x] **SUBJ-05**: User can browse wrong answer list filtered by subject
- [x] **SUBJ-06**: User can search wrong answers by date range, knowledge point, and error type
- [x] **SUBJ-07**: User can sort wrong answers by date, review count, and mastery level

### Knowledge Analysis (知识点分析)

- [ ] **KNOW-01**: AI automatically identifies knowledge points from wrong answers
- [ ] **KNOW-02**: User can manually correct AI-identified knowledge points
- [ ] **KNOW-03**: AI analyzes error patterns across multiple wrong answers to identify root causes
- [ ] **KNOW-04**: System classifies errors into types: Careless, Conceptual, Method, Calculation
- [ ] **KNOW-05**: User can view knowledge weakness analysis showing common error patterns

### Review & Learning (复习与学习)

- [ ] **REV-01**: System schedules reviews using Ebbinghaus forgetting curve intervals
- [ ] **REV-02**: System implements Leitner box system for progressive difficulty
- [ ] **REV-03**: System combines spaced repetition with progressive difficulty for optimal review timing
- [ ] **REV-04**: System generates daily review task list ("Today's N questions to review")
- [ ] **REV-05**: User can mark review result as "Got it" or "Still wrong"
- [ ] **REV-06**: System updates Leitner box position based on review results
- [ ] **REV-07**: User can review any subset of wrong answers on demand (by subject, date, etc.)
- [ ] **REV-08**: AI generates similar practice questions targeting the same knowledge point
- [ ] **REV-09**: User can practice with AI-generated similar questions

### AI Model Configuration (AI 模型配置)

- [ ] **AICFG-01**: User can configure API key for AI services
- [ ] **AICFG-02**: User can assign different AI models to different tasks (OCR, analysis, question generation)
- [ ] **AICFG-03**: System provides sensible defaults for AI model configuration
- [ ] **AICFG-04**: System validates API key connectivity before saving

### Data Management (数据管理)

- [ ] **DATA-01**: All data stored locally in SQLite database
- [x] **DATA-02**: System auto-backups data to local file on schedule
- [x] **DATA-03**: User can export data to JSON format for backup
- [x] **DATA-04**: User can export data to CSV format for external analysis
- [x] **DATA-05**: User can import data from JSON backup file
- [ ] **DATA-06**: System sends Windows notification for daily review reminders
- [ ] **DATA-07**: User can configure review reminder time
- [ ] **DATA-08**: User can generate printable review sheet from due questions as PDF

### Analytics & Gamification (数据分析与激励)

- [ ] **ANA-01**: User can view learning progress dashboard with error trends over time
- [ ] **ANA-02**: User can view subject breakdown of errors
- [ ] **ANA-03**: User can view knowledge point mastery percentage
- [ ] **ANA-04**: User can view review completion rate
- [ ] **ANA-05**: System displays knowledge mastery map with color-coded status (Red=weak, Yellow=learning, Green=mastered)
- [ ] **ANA-06**: System tracks consecutive review days (streak)
- [ ] **ANA-07**: System awards badges and stars for achievements (e.g., 7-day streak)
- [ ] **ANA-08**: User can view achievement collection

### User Interface (用户界面)

- [x] **UI-01**: Application uses cute cartoon style suitable for elementary students (ages 6-12)
- [x] **UI-02**: Application provides parent management mode with clean interface
- [ ] **UI-03**: Application provides child review mode with playful interface
- [ ] **UI-04**: UI elements use large buttons and simple language appropriate for children
- [x] **UI-05**: Application supports Chinese language interface

## v2 Requirements

(No v2 requirements defined yet — all features scoped to v1)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Multi-user/multi-child support | V1 is single-child only; adds auth and data isolation complexity |
| Cloud sync / server deployment | Local-first strategy; server version planned for future |
| Social features (sharing, leaderboards) | Privacy concerns for children's data; unnecessary complexity |
| Full LMS features (assignments, grading) | Wrong-answer book is not a school LMS; scope creep |
| Video/homework recording | Different product category; adds storage/bandwidth complexity |
| Real-time collaboration | Not needed for single-child parent-assisted workflow |
| Complex user authentication | Single-machine, single-child; no need for login system |
| In-app browsing/search engine | Focus on user's OWN wrong answers; web search is different product |
| Handwriting practice/drawing | Different product; mark answers correct/incorrect, don't add drawing input |
| Multi-language UI | V1 is Chinese-only for Chinese market; i18n in future version |
| AI chatbot tutor | Scope creep; AI generates similar questions, not interactive tutoring |
| Complex analytics/reporting | Simple metrics only; not a BI tool |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| PHOTO-01 | Phase 2 | Pending |
| PHOTO-02 | Phase 2 | Pending |
| PHOTO-03 | Phase 2 | Pending |
| PHOTO-04 | Phase 2 | Pending |
| PHOTO-05 | Phase 2 | Pending |
| PHOTO-06 | Phase 2 | Pending |
| PHOTO-07 | Phase 2 | Pending |
| PHOTO-08 | Phase 2 | Pending |
| PHOTO-09 | Phase 2 | Pending |
| SUBJ-01 | Phase 1 | Completed (01-02) |
| SUBJ-02 | Phase 1 | Completed (01-02) |
| SUBJ-03 | Phase 1 | Completed (01-02) |
| SUBJ-04 | Phase 1 | Completed (01-02) |
| SUBJ-05 | Phase 1 | Completed (01-02) |
| SUBJ-06 | Phase 1 | Completed (01-02) |
| SUBJ-07 | Phase 1 | Completed (01-02) |
| KNOW-01 | Phase 4 | Pending |
| KNOW-02 | Phase 4 | Pending |
| KNOW-03 | Phase 4 | Pending |
| KNOW-04 | Phase 4 | Pending |
| KNOW-05 | Phase 4 | Pending |
| REV-01 | Phase 3 | Pending |
| REV-02 | Phase 3 | Pending |
| REV-03 | Phase 3 | Pending |
| REV-04 | Phase 3 | Pending |
| REV-05 | Phase 3 | Pending |
| REV-06 | Phase 3 | Pending |
| REV-07 | Phase 3 | Pending |
| REV-08 | Phase 3 | Pending |
| REV-09 | Phase 3 | Pending |
| AICFG-01 | Phase 4 | Pending |
| AICFG-02 | Phase 4 | Pending |
| AICFG-03 | Phase 4 | Pending |
| AICFG-04 | Phase 4 | Pending |
| DATA-01 | Phase 1 | Pending |
| DATA-02 | Phase 1 | Done |
| DATA-03 | Phase 1 | Done |
| DATA-04 | Phase 1 | Done |
| DATA-05 | Phase 1 | Done |
| DATA-06 | Phase 5 | Pending |
| DATA-07 | Phase 5 | Pending |
| DATA-08 | Phase 5 | Pending |
| ANA-01 | Phase 5 | Pending |
| ANA-02 | Phase 5 | Pending |
| ANA-03 | Phase 5 | Pending |
| ANA-04 | Phase 5 | Pending |
| ANA-05 | Phase 5 | Pending |
| ANA-06 | Phase 5 | Pending |
| ANA-07 | Phase 5 | Pending |
| ANA-08 | Phase 5 | Pending |
| UI-01 | Phase 1 | Done |
| UI-02 | Phase 1 | Done |
| UI-03 | Phase 5 | Pending |
| UI-04 | Phase 5 | Pending |
| UI-05 | Phase 1 | Done |

**Coverage:**
- v1 requirements: 55 total (mapped from 8 categories: PHOTO, SUBJ, KNOW, REV, AICFG, DATA, ANA, UI)
- Mapped to phases: 55
- Unmapped: 0

| Phase | Requirements | Count |
|-------|-------------|-------|
| Phase 1: Foundation | DATA-01..05, SUBJ-01..07, UI-01, UI-02, UI-05 | 15 |
| Phase 2: Capture Pipeline | PHOTO-01..09 | 9 |
| Phase 3: Review Engine | REV-01..09 | 9 |
| Phase 4: AI Intelligence | KNOW-01..05, AICFG-01..04 | 9 |
| Phase 5: Polish & Engagement | ANA-01..08, DATA-06..08, UI-03, UI-04 | 13 |

---
*Requirements defined: 2026-06-26*
*Last updated: 2026-06-26 after roadmap creation*
