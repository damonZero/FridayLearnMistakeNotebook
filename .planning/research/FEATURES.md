# Feature Landscape

**Domain:** Smart Wrong-Answer Book (错题本) for Elementary Students
**Researched:** 2026-06-26
**Overall Confidence:** MEDIUM (based on domain knowledge of Chinese EdTech market + learning theory research)

## Table Stakes

Features users expect. Missing any of these = product feels incomplete or broken.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| **Photo capture + crop** | Core workflow: parent photographs wrong answer from test/homework | Medium | In-app camera, crop tool, image rotation. Must handle various paper sizes and lighting. |
| **OCR question recognition** | Converts photo to editable text; without this, no better than a photo album | High | Must handle printed AND handwritten Chinese/English/Math. Accuracy is the #1 user pain point. |
| **Subject organization** | Parents expect to filter by Chinese/Math/English | Low | Preset subjects + dynamic add/delete. Hierarchical: Subject > Chapter > Knowledge Point. |
| **Wrong answer list + search** | Browse and find previously recorded mistakes | Low | Filter by subject, date range, error type, knowledge point. Sort by date/review count. |
| **Manual text editing** | OCR will make mistakes; users must correct | Low | Edit question text, answer, notes. Rich text or structured fields. |
| **Knowledge point tagging** | Parents/teachers categorize by what concept was wrong | Medium | Auto-suggest tags from AI analysis + manual override. Hierarchical knowledge tree. |
| **Review scheduling (spaced repetition)** | Core value proposition: scientifically-backed review timing | High | Ebbinghaus curve + Leitner box system. Daily review queue with due/not-due status. |
| **Daily review task list** | Child opens app, sees what to review today | Medium | Push-style: "Today's 5 questions to review." Clear, actionable, age-appropriate. |
| **On-demand review mode** | Child/parent can review any subset on demand | Low | "Review all Math errors from last week" type filtering. |
| **Mark review result** | After reviewing, mark as "Got it" / "Still wrong" | Low | Binary or 3-level (Easy/OK/Hard). Updates Leitner box position. |
| **Local data storage** | All data on local machine, no cloud dependency | Medium | SQLite or similar. Must survive app restarts. Auto-backup to local file. |
| **Data export/backup** | Parents want to backup, transfer, or print | Low | Export to PDF (for printing), JSON/CSV (for backup). Import from backup file. |
| **Child-friendly UI** | Target audience is 6-12 year olds | Medium | Cute cartoon style, large buttons, simple language. But parent mode for management. |

## Differentiators

Features that set the product apart. Not expected, but create competitive advantage.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **AI knowledge gap analysis** | Beyond tagging: AI identifies ROOT CAUSE of errors (e.g., "doesn't understand fractions, not just 'made calculation error'") | High | Analyze error patterns across multiple wrong answers. Show knowledge graph with weak nodes highlighted. |
| **Similar question generation** | AI generates new practice questions targeting the same knowledge point | High | "You got fraction addition wrong, here are 3 similar problems." Requires LLM API integration. |
| **Progress analytics dashboard** | Visual learning progress for parents | Medium | Charts: errors over time, subject breakdown, knowledge mastery %, review completion rate. |
| **Per-task AI model config** | Let parent choose which AI model for OCR vs analysis vs generation | Medium | Power feature: different models excel at different tasks. API key management. |
| **Multi-image batch import** | Photograph entire test paper, auto-crop individual questions | High | Significant UX improvement: photograph once, get 10 questions. Requires image segmentation. |
| **Error type classification** | Categorize errors: Careless / Conceptual / Method / Calculation | Medium | AI-assisted classification + manual correction. Enables pattern analysis ("80% of errors are careless"). |
| **Review session timer** | Track how long child spends on each review | Low | Gamification element: "You reviewed 5 questions in 8 minutes!" Helps parents monitor engagement. |
| **Achievement/streak system** | Motivate consistent review with streaks, badges, stars | Medium | "7-day review streak!" with visual rewards. Age-appropriate gamification for 6-12 year olds. |
| **PDF test paper import** | Import digital test papers (not just photos) | Medium | Some schools provide PDF test papers. Extract questions directly. |
| **Knowledge point mastery map** | Visual tree/graph showing mastery level per knowledge point | Medium | Color-coded: Red (weak) / Yellow (learning) / Green (mastered). Helps parents see the big picture. |
| **Print-friendly review sheet** | Generate a printable review sheet from due questions | Low | Some parents prefer physical flashcards. Export today's review as formatted PDF. |
| **Review reminder notification** | Windows system notification: "Time to review! 5 questions due today" | Low | Simple but effective for building habit. Configurable time (e.g., 7pm daily). |

## Anti-Features

Features to explicitly NOT build. These add complexity without proportional value.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| **Multi-user/multi-child support** | Adds auth, data isolation, UI complexity. V1 is single-child. | Use profile name + avatar for personalization. Multi-child in future version. |
| **Cloud sync / server** | Adds server costs, deployment complexity, privacy concerns. Local-first is the strategy. | Local SQLite + file-based backup/restore. Cloud sync in future. |
| **Social features (sharing, leaderboards)** | Privacy concerns for children's data. Unnecessary complexity. | Focus on individual progress tracking. No sharing of error data. |
| **Full LMS features (assignments, grading)** | Wrong-answer book is not a school LMS. Scope creep. | Stay focused: capture errors, analyze, review. Don't become a classroom tool. |
| **Video/homework recording** | Different product category. Adds storage/bandwidth complexity. | Stick to photo + text. Video is for tutoring apps, not error tracking. |
| **Real-time collaboration** | Not needed for single-child parent-assisted workflow. | Async is fine. Parent records, child reviews separately. |
| **Complex user authentication** | Single-machine, single-child. No need for login system. | Simple local profile selection if needed. No passwords, no accounts. |
| **In-app browsing/search engine** | Tempting to add "search for answer" but scope creep. | Focus on the user's OWN wrong answers. Web search is a different product. |
| **Handwriting practice/drawing** | Different product. Adds canvas/drawing complexity. | Mark answers as correct/incorrect. Don't add drawing input. |
| **Multi-language UI** | V1 is Chinese-only for Chinese market. | Chinese UI with English subject content. i18n in future version. |
| **AI chatbot tutor** | Scope creep. Different product category (AI tutor vs error tracker). | AI generates similar questions, not interactive tutoring. Keep it focused. |
| **Complex analytics/reporting** | Parents don't need dashboards with 20 charts. | Simple, clear metrics: total errors, review completion rate, mastery %. Not a BI tool. |

## Feature Dependencies

```
Photo Capture → OCR Recognition → Manual Edit → Knowledge Point Tagging
                                                    ↓
                                            Review Scheduling
                                                    ↓
                                            Daily Review List → Review Session → Mark Result
                                                    ↓
                                            Progress Analytics

AI Knowledge Analysis ← requires → Knowledge Point Tagging + multiple recorded errors
Similar Question Generation ← requires → AI Knowledge Analysis + AI model config
Batch Import ← requires → Photo Capture + OCR + image segmentation
Print Export ← requires → Review Scheduling + question content
```

## MVP Recommendation

**Prioritize (Phase 1 - Core Loop):**
1. Photo capture + crop + OCR recognition
2. Subject organization (preset 3 subjects)
3. Wrong answer list + manual edit
4. Knowledge point tagging (manual first, AI later)
5. Review scheduling (Ebbinghaus + Leitner)
6. Daily review task list + mark result
7. Local storage + auto-backup
8. Child-friendly UI

**Why this order:** The core loop is "Capture error -> Review error -> Mark progress." Without this loop working end-to-end, nothing else matters. OCR accuracy is the #1 risk, so build it early and iterate.

**Phase 2 (Intelligence Layer):**
1. AI knowledge gap analysis
2. Similar question generation
3. Error type classification
4. Per-task AI model configuration
5. Progress analytics dashboard

**Why:** AI features differentiate but depend on having enough data (Phase 1 errors) to be useful. Build the intelligence layer after the core data loop is stable.

**Phase 3 (Polish & Scale):**
1. Multi-image batch import
2. Knowledge point mastery map
3. Achievement/streak system
4. Print-friendly review sheet
5. Review reminder notification
6. PDF test paper import

**Why:** These are UX improvements and engagement features that make the product delightful, but aren't required for the core value proposition.

**Defer indefinitely:**
- Multi-user/multi-child (future version)
- Cloud sync (future version)
- Social features (never for this product)
- LMS features (never for this product)

## Key Risk: OCR Accuracy

The single biggest risk to the entire product is OCR accuracy. If OCR can't reliably recognize:
- **Printed Chinese text** (complex characters, various fonts)
- **Handwritten Chinese text** (child handwriting varies wildly)
- **Math expressions** (fractions, exponents, equations)
- **English text** (mixed with Chinese, various handwriting styles)

...then the product fails. This must be the first technical spike. Consider:
- Multiple OCR providers (Tesseract, Baidu OCR, Google Vision, Azure)
- Fallback: manual entry with OCR as "nice to have" suggestion
- User correction loop: corrections improve future recognition

## Sources

- Chinese EdTech market analysis: 错题帮, 橙果错题本, 小猿搜题, 蜜蜂试卷, 试卷宝, 夸克学习
- Learning theory: Ebbinghaus forgetting curve, Leitner box system, spaced repetition research
- Educational app patterns: Anki, Quizlet, Kahoot!, ClassDojo, Prodigy Math
- User pain point research: Chinese education forums, app store reviews
