# Domain Pitfalls

**Domain:** C# Desktop Learning App (Wrong Answer Book)
**Researched:** 2026-06-26

## Critical Pitfalls

Mistakes that cause rewrites, data loss, or fundamental product failure.

### Pitfall 1: OCR Accuracy Assumptions for Handwritten Content

**What goes wrong:** The app assumes AI-powered OCR will reliably recognize handwritten Chinese characters, math equations, and English text from phone photos. Real-world accuracy for handwritten elementary student writing is significantly lower than printed text benchmarks. Parents become frustrated when they must constantly correct OCR results, abandoning the photo workflow entirely.

**Why it happens:** AI API providers showcase accuracy on clean, printed documents. Handwritten Chinese characters have thousands of classes with structural similarity (e.g., 己/已/巳). Math notation is inherently two-dimensional (superscripts, fractions, spatial relationships). Student handwriting varies wildly. Phone photos add blur, skew, uneven lighting, and background noise.

**Consequences:** If OCR is unreliable, the entire core value proposition collapses. Parents revert to manual entry, questioning why they need the app at all. Trust in AI features erodes.

**Prevention:**
- Design the OCR step as "AI-assisted recognition + human verification" from day one, not "automatic recognition"
- Build a fast correction UI as a first-class feature, not an afterthought
- Implement image preprocessing (deskew, binarization, contrast enhancement) before sending to OCR
- Allow manual fallback at every OCR touchpoint
- Test with real handwritten samples from target age group early (Phase 1-2)
- Consider offering both "quick capture" (skip OCR, manual entry) and "smart capture" (OCR + verify) modes

**Detection:** If correction time exceeds the time saved by OCR, the workflow is broken.

**Phase to address:** Phase 1 (Foundation) -- the correction UX must be designed alongside the capture flow, not bolted on later.

---

### Pitfall 2: Spaced Repetition Algorithm Implementation Bugs

**What goes wrong:** The Ebbinghaus/Leitner/spaced repetition algorithm has subtle implementation bugs that cause reviews to appear at wrong intervals. Cards pile up after skipped days. Ease factors drift to extremes. The learning schedule becomes meaningless or actively harmful to retention.

**Why it happens:** The SM-2 algorithm (basis for most spaced repetition) has specific edge cases: hardcoded first intervals (1 day, 6 days), ease factor floor of 1.3, quality scale mapping (0-5). Common bugs include: off-by-one errors ("days from now" vs "the nth day"), not resetting intervals on lapse, incorrect ease factor calculations, time zone/day boundary issues, and not handling multi-day absences gracefully.

**Consequences:** If reviews appear too late, students forget material. If too early, they waste time on already-learned content. Parents lose trust in the "smart scheduling" promise. The scientific credibility of the app is undermined.

**Prevention:**
- Study Anki's open-source implementation as a reference -- it has years of edge-case fixes
- Implement the algorithm as a pure, testable function with no UI dependencies
- Write comprehensive unit tests for: first review, lapse handling, ease factor bounds, multi-day gaps, midnight boundaries
- Add a "preview schedule" debug view showing when each card will next appear
- Validate against known SM-2 expected outputs before integrating with UI
- Log all scheduling decisions for post-hoc analysis

**Detection:** Create test cards, answer them with known patterns, verify the schedule matches expected SM-2 output. If a card answered "wrong" appears 30 days later instead of 1 day, something is broken.

**Phase to address:** Phase 3 (Learning Engine) -- implement and thoroughly test the algorithm before connecting it to the review UI.

---

### Pitfall 3: Blocking the UI Thread with AI API Calls

**What goes wrong:** OCR, knowledge analysis, or similar question generation calls are made synchronously on the UI thread. The entire application freezes for 5-30 seconds during each AI operation. Parents think the app has crashed. On slower connections or API timeouts, the freeze can last minutes.

**Why it happens:** C# developers from Unity backgrounds are accustomed to coroutines and async patterns but may not know WPF's dispatcher model. Making HTTP calls with `HttpClient` without `async/await` blocks the calling thread. Even with async, forgetting to use `ConfigureAwait(false)` or dispatching back to UI thread incorrectly causes deadlocks.

**Consequences:** App feels broken and unresponsive. Users force-quit during operations, potentially corrupting in-progress data. The "cute cartoon UI" becomes a frozen screenshot.

**Prevention:**
- Establish a strict rule: no network I/O on the UI thread, ever
- Use `IAsyncRelayCommand` (from CommunityToolkit.Mvvm) for all async operations
- Show animated loading indicators during AI calls (progress spinners, cartoon character animations)
- Implement cancellation tokens for all API calls so users can abort
- Set reasonable timeouts (30-60s for OCR, 15-30s for analysis)
- Queue multiple AI operations instead of firing them all simultaneously

**Detection:** Any operation that makes the window title show "Not Responding" is a blocking call on the UI thread.

**Phase to address:** Phase 1 (Foundation) -- establish the async pattern in the project skeleton. Retrofitting async later is painful and error-prone.

---

### Pitfall 4: SQLite Database Without Migration Strategy

**What goes wrong:** The local SQLite database schema evolves as features are added, but there is no migration system. Early users (or the developer during testing) must delete their database and start over with each schema change. Accumulated wrong answers and review history are lost.

**Why it happens:** SQLite is simple to set up -- just create tables on first run. But there is no built-in migration framework. Developers add columns or tables ad-hoc, forgetting that existing databases need `ALTER TABLE` statements. Backups of the `.db` file without WAL/SHM files lose recent transactions. Concurrent access from multiple threads without `busy_timeout` causes "database is locked" errors.

**Consequences:** Developer loses test data repeatedly, slowing iteration. If shipped without migrations, any schema update in a new version destroys user data. Parents who spent weeks entering wrong answers lose everything on an update.

**Prevention:**
- Implement a versioned migration system from day one (e.g., a `SchemaVersion` table with sequential migration scripts)
- Use a migration library or roll a simple one: check version on startup, run pending migrations in order
- Always run migrations inside transactions
- Enable WAL mode for better concurrent read/write performance
- Use `PRAGMA integrity_check` after migrations
- Implement automatic database backup before each migration
- Use `VACUUM INTO` or the SQLite backup API for safe backups (not file-level copy)

**Detection:** If you have ever said "just delete the database and re-run," you need migrations.

**Phase to address:** Phase 1 (Foundation) -- the migration system must exist before the first table is created.

---

### Pitfall 5: API Key Security and Configuration Complexity

**What goes wrong:** API keys for AI providers are stored in plain text in config files, hardcoded in source code, or exposed in error messages. The per-task AI model configuration (different providers for OCR vs analysis vs generation) creates a complex configuration surface that confuses parents.

**Why it happens:** Developers store keys in `appsettings.json` or `config.xml` for convenience. The per-task model feature means potentially 3+ API keys across different providers. Parents are not developers -- asking them to obtain, configure, and manage multiple API keys is a significant UX barrier. Keys in plain text files are trivially extractable.

**Consequences:** Security risk if config files are shared or backed up to cloud storage. Parents cannot configure the app, leading to support burden or abandonment. If a key is exposed, the parent's API account may be abused.

**Prevention:**
- Use Windows Credential Manager or DPAPI for API key storage, never plain text files
- Provide a guided setup wizard that walks parents through API key entry with copy-paste from provider dashboards
- Offer a "test connection" button for each configured provider
- Consider a default/pre-configured option (e.g., ship with a single recommended provider, allow advanced users to customize)
- Mask API keys in the UI (show only last 4 characters)
- Never log API keys in error messages or diagnostic output

**Detection:** If `cat config.json` reveals all API keys, they are not secured.

**Phase to address:** Phase 2 (AI Integration) -- key management must be designed before the first API call is made.

---

## Moderate Pitfalls

### Pitfall 6: Cartoon UI That Sacrifices Usability

**What goes wrong:** The cute cartoon style prioritizes visual charm over functional clarity. Decorative elements compete with interactive ones (kids tap non-interactive characters). Low-contrast color choices in the playful palette fail readability standards. Text-heavy screens overwhelm young readers. Parents struggle to find functional controls buried under cute decorations.

**Why it happens:** Designing for two audiences (children who enjoy the aesthetic, parents who need efficiency) is inherently tension-filled. Cartoon illustrations can crowd the UI. Playful fonts sacrifice legibility. Bright, saturated colors can reduce text contrast below WCAG 4.5:1 requirements.

**Consequences:** Parents become frustrated navigating a child-oriented interface for management tasks. Children tap on decorative elements expecting interactivity. The app looks polished but is annoying to use.

**Prevention:**
- Separate "parent mode" (functional, efficient) from "child review mode" (playful, animated)
- Maintain WCAG 2.1 AA contrast ratios even with cartoon palette
- Make all interactive elements visually distinct from decorative ones (borders, hover states, cursor changes)
- Use large click targets (minimum 44x44px) for child-facing screens
- Test with actual children early -- what adults think is "cute" may be confusing to kids
- Keep parent-facing screens clean and functional; save the cartoon flair for the child review experience

**Detection:** Watch a child use the app for 5 minutes. If they tap non-interactive elements more than twice, the visual hierarchy is broken.

**Phase to address:** Phase 4 (UI/UX Polish) -- but design principles should be established in Phase 1.

---

### Pitfall 7: Per-Task AI Model Configuration Overwhelm

**What goes wrong:** The per-task AI model configuration (different provider/model for OCR, analysis, and question generation) is exposed as a prominent feature. Parents are confronted with dropdown menus for "OCR Provider," "Analysis Model," "Generation Engine" without understanding what these mean. Configuration becomes a barrier to usage.

**Why it happens:** The feature is designed for power users (the developer) but the primary user is a parent who just wants the app to work. Offering 3 independent configuration axes (task x provider x model) creates decision paralysis. Default values may not work well, forcing configuration before first use.

**Consequences:** Parents cannot get the app working without hand-holding. Support requests spike. The "smart" app feels dumb because it cannot configure itself.

**Prevention:**
- Ship with sensible defaults that work out of the box (one recommended provider handles all tasks)
- Hide per-task configuration behind an "Advanced" toggle
- Provide a single "quick setup" that configures all tasks with one API key
- Allow per-task overrides only for users who explicitly want them
- Show a "recommended" badge on the default configuration
- Never block first use on configuration -- allow basic functionality (manual entry) without any API key

**Detection:** If a parent needs more than 2 minutes from install to first use, the setup is too complex.

**Phase to address:** Phase 2 (AI Integration) -- design the configuration UX alongside the API integration.

---

### Pitfall 8: Image Storage and Disk Space Management

**What goes wrong:** Every captured photo is stored at full resolution. After months of use, the local database with embedded images grows to gigabytes. The app slows down. Backups become impractical. Parents' computers run low on disk space.

**Why it happens:** Developers store original photos for "maximum quality" without considering accumulation. A parent capturing 5-10 wrong answers per day generates 50-100MB of images per month (assuming 1-2MB per photo). Over a school year, that is 500MB-1GB just for images. SQLite databases with embedded BLOBs become slow and difficult to back up.

**Consequences:** App performance degrades over time. Backup/restore takes minutes. Parents blame the app for disk space issues. Database corruption risk increases with size.

**Prevention:**
- Compress and resize images on capture (store at display resolution, not camera resolution)
- Store images as files on disk, not as BLOBs in SQLite (reference by path)
- Implement a storage management screen showing usage and offering cleanup
- Consider auto-archiving old entries (older than 1 year) to compressed storage
- Set a configurable storage limit with warnings
- Use progressive image loading for the review interface

**Detection:** If the database file exceeds 500MB after 6 months of normal use, image storage needs optimization.

**Phase to address:** Phase 1 (Foundation) -- storage strategy must be decided before the first image is saved.

---

### Pitfall 9: Subject Management Schema Rigidity

**What goes wrong:** The preset subjects (Chinese, Math, English) are hardcoded with assumptions about knowledge point structure. When parents add custom subjects (e.g., Science, History), the knowledge point taxonomy, question format, and AI analysis prompts do not adapt. Custom subjects feel like second-class citizens.

**Why it happens:** The initial implementation bakes subject-specific logic into OCR prompts, knowledge point hierarchies, and UI layouts. Chinese has character recognition needs, Math has equation parsing, English has vocabulary patterns. A generic "custom subject" does not have these specialized behaviors.

**Consequences:** Parents who want to use the app for other subjects find it inadequate. The "dynamic subject management" promise is technically fulfilled but practically useless. Feature request backlog grows.

**Prevention:**
- Design the subject system as a pluggable configuration, not hardcoded branches
- Each subject should define: OCR prompt template, knowledge point taxonomy, question format hints
- Ship with well-tuned presets for Chinese/Math/English but allow full customization
- Use a subject configuration file or database table, not code-level switches
- Allow parents to clone and modify existing subject configurations

**Detection:** If adding a new subject requires code changes, the system is too rigid.

**Phase to address:** Phase 1 (Foundation) -- the data model must support extensible subjects from the start.

---

### Pitfall 10: Similar Question Generation Quality

**What goes wrong:** AI-generated "similar questions" are trivially different (same question with numbers changed) or wildly inappropriate (too hard, too easy, wrong topic). Parents lose trust in the practice feature. Questions contain errors or nonsensical content.

**Why it happens:** Generating pedagogically appropriate similar questions requires understanding the knowledge point, difficulty level, grade level, and common misconceptions. Generic LLM prompts produce generic results. Without structured output validation, the AI may return malformed questions. Without difficulty calibration, questions may be inappropriate for the student's level.

**Consequences:** Parents stop using the practice feature. If questions contain errors, parents lose trust in the entire app. The "AI-powered" label becomes a liability.

**Prevention:**
- Include grade level, subject, and knowledge point in the generation prompt
- Request structured output (JSON schema) and validate before displaying
- Allow parents to rate generated questions and use feedback to improve prompts
- Start with template-based generation (fill-in-the-blank variations) before full LLM generation
- Show a "preview and approve" step before presenting questions to the child
- Log generated questions for quality review

**Detection:** If more than 30% of generated questions are rejected by parents, the generation prompts need work.

**Phase to address:** Phase 3 (Learning Engine) -- question generation quality requires iterative prompt engineering.

---

## Minor Pitfalls

### Pitfall 11: Date/Time Handling Across Midnight

**What goes wrong:** Reviews scheduled for "tomorrow" appear at midnight instead of morning. A review done at 11:59 PM counts as today; the same card done at 12:01 AM counts as tomorrow. Parents reviewing before bedtime find the next-day schedule shifted.

**Prevention:** Define a "day boundary" (e.g., 4:00 AM) rather than using midnight. Store dates without times where possible. Test all scheduling logic across the midnight boundary.

**Phase to address:** Phase 3 (Learning Engine).

---

### Pitfall 12: WPF Data Binding Silent Failures

**What goes wrong:** WPF data bindings fail silently -- a misspelled property name or missing `INotifyPropertyChanged` implementation causes the UI to show stale or default data with no error. The developer does not notice because there is no exception.

**Prevention:** Enable WPF binding diagnostics in debug mode (`PresentationTraceSources.TraceLevel`). Use CommunityToolkit.Mvvm source generators to eliminate manual `INotifyPropertyChanged` boilerplate. Write UI tests that verify bound data appears on screen.

**Phase to address:** Phase 1 (Foundation) -- set up binding diagnostics in the project template.

---

### Pitfall 13: Camera Integration Fragility

**What goes wrong:** The in-app camera feature depends on specific webcam hardware and drivers. Some webcams have poor resolution, auto-focus issues, or driver conflicts. The app crashes or shows a black screen on certain hardware configurations.

**Prevention:** Test with multiple webcam models. Provide a fallback "import from file" option that always works. Handle camera access errors gracefully (permission denied, device in use, no device found). Consider using Windows.Media.Capture APIs rather than DirectShow for broader compatibility.

**Phase to address:** Phase 2 (Capture Feature).

---

### Pitfall 14: Parent/Child Role Confusion in Single-User Model

**What goes wrong:** The app is designed for a parent-child workflow but has a single-user model. There is no clear separation between "parent is managing content" and "child is reviewing." The child can accidentally delete entries, change settings, or access configuration screens. The parent cannot track what the child actually reviewed.

**Prevention:** Implement a simple mode switch (parent mode vs child mode) with different UI surfaces. Child mode should be locked to review screens only. Consider a simple PIN or click-pattern to exit child mode. Track review activity separately from management activity.

**Phase to address:** Phase 4 (UI/UX) -- but design the data model distinction in Phase 1.

---

### Pitfall 15: Backup That Does Not Actually Work

**What goes wrong:** The "auto-backup" feature copies the SQLite database file directly. If the database is in WAL mode, the backup is incomplete or corrupt. Restoring from backup silently loses data. Parents assume their data is safe until they need to restore.

**Prevention:** Use SQLite's `VACUUM INTO` or `sqlite3_backup` API for consistent backups. Verify backup integrity with `PRAGMA integrity_check` after creation. Test restore end-to-end (backup, delete original, restore, verify data). Store backups in a known, accessible location.

**Phase to address:** Phase 1 (Foundation) -- backup must work correctly from the start.

---

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| Project Skeleton (Phase 1) | Skipping migration system, storage strategy, async patterns | Establish these foundations before writing features |
| Database Schema (Phase 1) | Hardcoding subject structure, storing images as BLOBs | Design for extensibility, file-based image storage |
| AI Integration (Phase 2) | Blocking UI thread, plain-text API keys, complex configuration | Async-first, secure storage, sensible defaults |
| OCR Pipeline (Phase 2) | Assuming high accuracy, no correction workflow | Design correction UI as primary, OCR as assist |
| Learning Algorithm (Phase 3) | SM-2 edge cases, timezone bugs, skipped-day handling | Pure-function implementation, comprehensive unit tests |
| Question Generation (Phase 3) | Low-quality or incorrect generated content | Structured output, parent approval step, prompt iteration |
| UI Polish (Phase 4) | Cartoon style sacrificing usability, dual-audience tension | Separate parent/child modes, maintain contrast ratios |
| Testing (Phase 5) | No real handwritten samples, no child user testing | Early integration testing with real photos and real children |

---

## Sources

- SM-2 algorithm documentation (SuperMemo/Wozniak original papers)
- Anki open-source implementation (reference for spaced repetition edge cases)
- WPF documentation and CommunityToolkit.Mvvm guidance
- SQLite documentation (WAL mode, backup API, migration patterns)
- WCAG 2.1 accessibility guidelines
- OCR research: CROHME (handwritten math), CASIA (Chinese handwriting) datasets
- Microsoft guidance on resilient HTTP clients (Polly for .NET)
- Common WPF MVVM anti-patterns (community knowledge)

---

*Confidence: MEDIUM-HIGH -- Pitfalls are based on domain expertise and known patterns. Specific accuracy numbers for handwritten OCR would benefit from real-world testing with target age group.*
