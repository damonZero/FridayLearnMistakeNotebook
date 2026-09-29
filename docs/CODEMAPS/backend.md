<!-- Generated: 2026-09-27 | Files scanned: 30 | Token estimate: ~750 -->

# 数据与业务层（Android 无服务器，本文档对应 domain + data 层）

## 远程服务（唯一外部调用）

```
VolcanoOcrService.recognizeText(imageBase64) : OcrResult
  → 读取 AiConfigDao.getEnabledConfigByTaskType(OCR)  // Key/BaseUrl/Model 用户配置
  → POST {baseUrl}/chat/completions  (OpenAI 兼容, Bearer 认证)
  → OcrResponseParser.parse() → OcrResult(text, confidence, textBlocks)
```

注意：**AI 分析知识点、相似题生成仅有 DB/DTO 脚手架，无实现**（见 ai_configs 表按任务类型预留，`AiTaskType` 目前实际使用仅 OCR）。

## 用例 → 仓库 → DAO 映射

| UseCase (`domain/usecase/`) | Repository 方法 | DAO |
|---|---|---|
| AddQuestionUseCase | QuestionRepository.addQuestion | QuestionDao.insertQuestion |
| GetQuestionsForReviewUseCase | getQuestionsForReview | QuestionDao.getQuestionsForReview(now) |
| ProcessReviewResultUseCase | processReviewResult | QuestionDao.updateQuestion（经算法计算） |
| GetAllSubjectsUseCase | SubjectRepository.getAllSubjects | SubjectDao.getAllSubjects |
| AddSubjectUseCase | addSubject | SubjectDao.insertSubject |
| DeleteSubjectUseCase | deleteSubject | SubjectDao.deleteSubjectById（含删除保护：预设科目拒绝） |

## 算法 — `domain/algorithm/SpacedRepetitionAlgorithm.kt`（object 纯函数）

- `calculateNextReview(question, score)` — score 先钳制 1..5
  - 答错（score<3）：盒→1，EF 公式下调（≥1.3），**间隔固定 1 天**（答错次日必现）
  - 答对（score≥3）：盒→min(box+1,5)，EF 夹在 [1.3, 2.5]，间隔=round(基础间隔×EF)
  - 基础间隔：盒1=1天 盒2=2天 盒3=4天 盒4=7天 盒5=15天
- `getBoxDescription(box)` / `getMasteryPercentage(box)`
- 待复习查询时间源：Repository 内每分钟重发 endOfToday（当天到期即算，跨午夜自动刷新）

## 备份 — `data/backup/BackupManager.kt`

- `BackupData v2` 强类型：全部 6 张表（subjects/chapters/knowledgePoints/questions/aiConfigs/aiUsageLogs），导出时 aiConfigs 的 apiKey 置空脱敏
- `exportToJson()/exportToFile()/exportToUri(uri)`（SAF 导出到用户选择位置，事务内读快照）
- `importFromJson/importFromFile` 真实现：version 校验 → 按 FK 顺序逐表 upsert（同 id 覆盖，aiConfigs 空 key 时保留本地 key）→ 返回含统计的 ImportResult
- `autoBackup()` — MainActivity.onStop 触发，返回 Boolean 如实记录；`getBackupFiles()/deleteBackup()`

## AI 连通性 — `data/remote/AiConnectivityTester.kt`

- `testConnection(baseUrl, apiKey, modelName)`：最小 chat/completions 探测（max_tokens=1），401/404/429/超时映射中文提示，供 AiConfig 连接测试真实调用

## 工具 — `util/ImageUtil.kt`（object）

`uriToBase64` / `bitmapToBase64` / `compressBitmap(1920×1080)` / `saveImageToLocal` / `loadLocalImage`

## DI 模块（`di/`，均 SingletonComponent）

- `DatabaseModule` — Room 实例 + 6 DAO 提供
- `RepositoryModule` — `@Binds` 接口→Impl
- `AppModule` / `GsonModule` — OkHttp、Gson、Context
