<!-- Generated: 2026-09-27 | Files scanned: 76 | Token estimate: ~700 -->

# 系统架构 — 周周错题本 (FridayLearnMistakeNotebook)

小学生智能错题本 Android APP。MVVM + Clean Architecture，数据全本地，AI 能力（OCR）由用户自配 API Key 调用火山方舟（OpenAI 兼容协议）。

## 分层图

```
┌─────────────────────────────────────────────────────────┐
│ UI (Jetpack Compose)          ui/*                      │
│  12 Screens ←→ 10 ViewModels (StateFlow<UiState>)       │
│  Navigation: FridayNotebookNavHost + Screen (sealed)    │
├─────────────────────────────────────────────────────────┤
│ Domain (纯 Kotlin)            domain/*                  │
│  UseCases: Add/GetQuestionsForReview/ProcessReviewResult│
│            AddSubject/DeleteSubject/GetAllSubjects      │
│  Algorithm: SpacedRepetitionAlgorithm (object, 纯函数)  │
│  Repository 接口: QuestionRepository, SubjectRepository │
├─────────────────────────────────────────────────────────┤
│ Data                          data/*                    │
│  RepositoryImpl ← Room DAOs (6) ← Room DB v1 (6 实体)   │
│  VolcanoOcrService → 火山方舟 /chat/completions (OCR)   │
│  BackupManager → JSON 导出/导入/自动备份                │
├─────────────────────────────────────────────────────────┤
│ DI (Hilt, SingletonComponent) di/*                       │
│  AppModule / DatabaseModule / RepositoryModule / Gson   │
└─────────────────────────────────────────────────────────┘
```

## 入口

- `FridayNotebookApp.kt` — `@HiltAndroidApp`，无额外初始化
- `MainActivity.kt` — 唯一 Activity，承载 Compose NavHost

## 核心数据流

**录入错题：**
```
CameraScreen ─拍照→ CameraViewModel ─ImageUtil.toBase64→ VolcanoOcrService
  → OcrResponseParser → savedStateHandle["ocr_result"] → AddQuestionScreen
  → AddQuestionViewModel → AddQuestionUseCase → QuestionRepositoryImpl → Room
```

**复习循环：**
```
ReviewScreen → ReviewViewModel → GetQuestionsForReviewUseCase
  → ReviewResult("会了"/"还错") → ProcessReviewResultUseCase
  → SpacedRepetitionAlgorithm.calculateNextReview(莱特纳盒子 + SM-2 难度系数)
  → QuestionRepositoryImpl → Room (nextReviewDate / leitnerBox)
```

## 关键设计决策

- 依赖倒置：domain 定义 Repository 接口，data 层实现，Hilt `@Binds` 绑定
- OCR 配置从 `ai_configs` 表按 `AiTaskType.OCR` 动态读取（用户自填 Key/BaseUrl/Model）
- 复习排期纯本地算法，不依赖 AI（见 `domain/algorithm/`）
- 主题：Material3 + 卡通风格，中文界面，minSdk 26 / target 34
