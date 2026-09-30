<!-- Generated: 2026-09-27 | Files scanned: 30 | Token estimate: ~800 -->

# UI 层（Jetpack Compose, Material3）

## 页面树与导航

宿主：`MainActivity` → `FridayNotebookNavHost`（`ui/navigation/`）
路由定义：`Screen.kt` sealed class；底部导航 4 Tab：首页 / 科目 / 复习 / 设置

```
home            HomeScreen      — 统计卡 + 今日待复习数 + 快捷入口
subjects        SubjectsScreen  — 科目卡片列表 / AddSubjectDialog / EmptySubjectsContent
review          ReviewScreen    — ReviewContent(答题卡) / EmptyReviewContent / ReviewCompleteContent
settings        SettingsScreen  — 设置分区列表（无独立 ViewModel）
  ├─ add_question?subjectId  AddQuestionScreen — 表单 + 相机/OCR结果回填(getErrorTypeName)
  ├─ camera                  CameraScreen    — CameraX 预览拍照 → OCR → 回传上一页
  ├─ question_list?subjectId QuestionListScreen — 筛选列表 / QuestionCard(点击进详情) / EmptyQuestionList
  ├─ question/{questionId}   QuestionDetailScreen — 原图/答案/复习状态/AI分析(存aiAnalysis)/相似题入口/删除
  ├─ practice/{questionId}   PracticeScreen — AI生成相似题 → 逐题自评 → 小结（不计入复习计划）
  ├─ stats                   StatsScreen     — MasteryLevelItem 掌握度分布
  ├─ ai_config               AiConfigScreen  — AiConfigCard / AiConfigDialog / 连接测试
  ├─ ai_usage                AiUsageScreen   — 调用日志 + 费用汇总 AiUsageLogCard
  ├─ backup                  BackupScreen    — BackupFileCard 备份列表
  └─ achievement             AchievementScreen — AchievementCard 9 成就
```

⚠️ 历史遗留的 4 个死路由（SubjectDetail/QuestionDetail/ReviewSession/DataBackup）已于 2026-09-27 清理。

## ViewModel ↔ Screen（均 `@HiltViewModel` + StateFlow<UiState>）

| ViewModel | UiState 关键字段 | 依赖 |
|---|---|---|
| HomeViewModel | todayReviewCount, totalQuestionCount | QuestionUseCases |
| SubjectViewModel | subjects, isLoading | SubjectUseCases |
| ReviewViewModel | questions, currentIndex | GetQuestionsForReview + ProcessReviewResult |
| AddQuestionViewModel | subjects, selectedSubjectId, ocrText | AddQuestionUseCase + OCR |
| CameraViewModel | capturedImageUri/Bitmap, ocrResult | VolcanoOcrService + ImageUtil |
| QuestionListViewModel | questions, subjects | QuestionRepository |
| StatsViewModel | totalQuestions, masteredQuestions | QuestionRepository Flow 统计 |
| AiConfigViewModel | configs(List\<AiConfigEntity\>), isLoading | AiConfigDao（直连，未过 domain） |
| AiUsageViewModel | logs, totalCost | AiUsageLogDao（直连，未过 domain） |
| BackupViewModel | backupFiles(List\<File\>) | BackupManager |
| AchievementViewModel | achievements(List\<Achievement\>), totalQuestions | 统计数据推导 |

## 状态回传约定

Camera → AddQuestion 通过 `navController.previousBackStackEntry.savedStateHandle["ocr_result"]` 传递识别文本。

## 主题 — `ui/theme/`：Color.kt / Theme.kt(FridayNotebookTheme) / Type.kt，卡通风格，中文文案硬编码在 Composable 中（strings.xml 仅 app_name）
