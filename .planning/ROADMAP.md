# Roadmap: Smart Wrong Answer Book (智能错题本)

## Overview

The Smart Wrong Answer Book builds a photo-to-review learning loop for elementary students. Phase 1 establishes the data foundation and app shell. Phase 2 delivers the capture pipeline (camera, OCR, correction) -- the highest-risk area. Phase 3 implements the spaced repetition review engine that turns captured mistakes into scheduled learning. Phase 4 layers AI intelligence for knowledge analysis and question generation. Phase 5 polishes the experience with analytics, gamification, and engagement features.

## Phases

**Phase Numbering:**

- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Foundation** - Domain model, database, subject management, WPF shell with base UI
- [ ] **Phase 2: Capture Pipeline** - Camera capture, image processing, OCR with correction, batch import
- [ ] **Phase 3: Review Engine** - Spaced repetition algorithm, daily review queue, review session UI
- [ ] **Phase 4: AI Intelligence** - Knowledge analysis, error classification, AI model configuration
- [ ] **Phase 5: Polish & Engagement** - Analytics dashboard, gamification, child mode, notifications, PDF export

## Phase Details

### Phase 1: Foundation

**Goal**: User can launch the app, manage subjects and knowledge points, and navigate a working Chinese-language interface
**Mode:** mvp
**Depends on**: Nothing (first phase)
**Requirements**: DATA-01, DATA-02, DATA-03, DATA-04, DATA-05, SUBJ-01, SUBJ-02, SUBJ-03, SUBJ-04, SUBJ-05, SUBJ-06, SUBJ-07, UI-01, UI-02, UI-05
**Success Criteria** (what must be TRUE):

  1. User can launch the app and see a Chinese-language interface with cartoon-styled theme
  2. User can create, edit, and delete subjects (with Chinese, Math, English pre-populated)
  3. User can browse a hierarchical knowledge tree (Subject > Chapter > Knowledge Point)
  4. User can search and filter the question list by subject, date range, and sort criteria
  5. User can export data to JSON/CSV and import from a JSON backup file

**Plans**: 3 plans
Plans:
**Wave 1**

- [x] 01-01: Project skeleton -- .NET 10 solution, Clean Architecture layers, DI container, EF Core + SQLite with migration system, domain entities (completed 2026-06-26)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 01-02: Subject, knowledge & question management -- Subject CRUD with presets, hierarchical knowledge tree, Add Question form (exercises Question entity, ErrorType, LeitnerBox), search/filter/sort (completed 2026-06-26)

**Wave 3** *(blocked on Wave 2 completion)*

- [ ] 01-03: WPF shell & base UI -- Main window with navigation, WPF UI Fluent theme + cartoon overlay, parent mode layout, Chinese strings, data export/import, auto-backup

### Phase 2: Capture Pipeline

**Goal**: User can photograph a wrong answer, get AI-recognized text, correct errors, and save the question to the database
**Mode:** mvp
**Depends on**: Phase 1
**Requirements**: PHOTO-01, PHOTO-02, PHOTO-03, PHOTO-04, PHOTO-05, PHOTO-06, PHOTO-07, PHOTO-08, PHOTO-09
**Success Criteria** (what must be TRUE):

  1. User can take a photo of a wrong answer using the in-app camera
  2. User can crop, rotate, and enhance the captured image before processing
  3. System extracts question text from photos (printed Chinese, handwritten Chinese, math expressions, English) with editable results
  4. User can correct OCR errors by editing the recognized text directly
  5. User can import multiple photos from a test paper or a PDF, with individual questions extracted

**Plans**: 3 plans

Plans:

- [ ] 02-01: Camera & image processing -- In-app camera via OpenCvSharp4, crop/rotate/enhance via ImageSharp, image storage on disk
- [ ] 02-02: OCR pipeline -- AI-powered OCR for all text types, correction UI, async loading indicators, timeout handling
- [ ] 02-03: Batch import -- Multi-photo import with auto-cropping, PDF import, question creation flow to database

### Phase 3: Review Engine

**Goal**: User sees a daily review queue, reviews questions with spaced repetition scheduling, and can practice with similar questions
**Mode:** mvp
**Depends on**: Phase 2
**Requirements**: REV-01, REV-02, REV-03, REV-04, REV-05, REV-06, REV-07, REV-08, REV-09
**Success Criteria** (what must be TRUE):

  1. User sees "Today's N questions to review" when opening the review section
  2. User can mark a reviewed question as "Got it" or "Still wrong"
  3. System adjusts review scheduling based on results (Leitner box position updates, next review date recalculated)
  4. User can start an on-demand review session filtered by subject, date range, or knowledge point
  5. User can practice with AI-generated similar questions targeting the same knowledge point

**Plans**: 3 plans

Plans:

- [ ] 03-01: Spaced repetition algorithm -- Ebbinghaus intervals, Leitner box system, combined scheduling, unit tests
- [ ] 03-02: Review session UI -- Daily task list generation, review screen, Got it/Still wrong buttons, Leitner updates
- [ ] 03-03: Review flexibility -- On-demand review filters, AI similar question generation, practice mode

### Phase 4: AI Intelligence

**Goal**: System automatically identifies knowledge points and error patterns from wrong answers, with user-configurable AI models
**Mode:** mvp
**Depends on**: Phase 3
**Requirements**: KNOW-01, KNOW-02, KNOW-03, KNOW-04, KNOW-05, AICFG-01, AICFG-02, AICFG-03, AICFG-04
**Success Criteria** (what must be TRUE):

  1. User can configure API keys and assign different AI models to OCR, analysis, and question generation tasks
  2. System automatically identifies knowledge points from wrong answers after capture
  3. User can manually correct AI-identified knowledge points
  4. System classifies errors into types (Careless, Conceptual, Method, Calculation)
  5. User can view a knowledge weakness analysis showing common error patterns across questions

**Plans**: 3 plans

Plans:

- [ ] 04-01: AI configuration -- API key management with connectivity validation, per-task model assignment, sensible defaults
- [ ] 04-02: Knowledge analysis -- AI knowledge point identification, manual correction UI, error type classification
- [ ] 04-03: Pattern analysis -- Cross-question error pattern analysis, root cause identification, weakness dashboard

### Phase 5: Polish & Engagement

**Goal**: User can view learning analytics, child earns achievements for consistent review, and the app delivers daily reminders and printable review sheets
**Mode:** mvp
**Depends on**: Phase 4
**Requirements**: ANA-01, ANA-02, ANA-03, ANA-04, ANA-05, ANA-06, ANA-07, ANA-08, DATA-06, DATA-07, DATA-08, UI-03, UI-04
**Success Criteria** (what must be TRUE):

  1. User can view a learning progress dashboard with error trends, subject breakdown, and mastery percentages
  2. User sees a color-coded knowledge mastery map (Red=weak, Yellow=learning, Green=mastered)
  3. System tracks consecutive review days (streak) and awards badges for achievements
  4. User receives Windows notifications for daily review reminders at a configured time
  5. User can generate a printable PDF review sheet; child mode shows large buttons and simple language

**Plans**: 3 plans

Plans:

- [ ] 05-01: Analytics dashboard -- Progress trends, subject breakdown, mastery percentage, completion rate, mastery map
- [ ] 05-02: Gamification & child mode -- Streak tracking, badge/achievement system, child review mode with playful UI
- [ ] 05-03: Engagement polish -- Windows notification reminders, configurable reminder time, printable PDF review sheets

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation | 2/3 | In progress | - |
| 2. Capture Pipeline | 0/3 | Not started | - |
| 3. Review Engine | 0/3 | Not started | - |
| 4. AI Intelligence | 0/3 | Not started | - |
| 5. Polish & Engagement | 0/3 | Not started | - |
