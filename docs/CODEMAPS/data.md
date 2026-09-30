<!-- Generated: 2026-09-27 | Files scanned: 12 | Token estimate: ~600 -->

# 数据库 — Room（MistakeNotebookDatabase, **v2**, exportSchema=true → schemas/，Migration 显式声明于 DatabaseModule）

位置：`data/local/`；类型转换：`Converters.kt`（enum ↔ name 等隐式转换）
迁移历史：`MIGRATION_1_2`（questions 加 aiAnalysis TEXT）；schema 基线 1.json/2.json 已入库

## 实体与关系

```
subjects (SubjectEntity) 1 ──── n chapters (ChapterEntity) 1 ──── n knowledge_points (KnowledgePointEntity)
    │                                                          │
    └──────────────── n questions (QuestionEntity) ────────────┘ (FK, 均可空)
独立表：ai_configs / ai_usage_logs
```

| 表 | 关键字段 | 备注 |
|---|---|---|
| `subjects` | name, icon, color, isPreset | 预设语文/数学/英语，删除保护靠 isPreset |
| `chapters` | subjectId FK, name | 无管理 UI |
| `knowledge_points` | chapterId FK, name, description | 无管理 UI |
| `questions` | content, answer, userAnswer, errorType, imagePath, **nextReviewDate, leitnerBox(1-5), easeFactor, streak, reviewCount, aiAnalysis(AI分析摘要)** | 核心；错误类型枚举 UNKNOWN/CARELESS/CONCEPTUAL/METHOD/CALCULATION |
| `ai_configs` | provider, apiKey, baseUrl, modelName, taskType(AiTaskType), isEnabled | **apiKey 明文存储**（需求要求加密） |
| `ai_usage_logs` | provider, taskType, modelName, inputTokens, outputTokens, estimatedCost | 费用统计来源 |

## DAO 概览（`data/local/dao/`）

- **QuestionDao**（19 方法，最重）：`getQuestionsForReview(dueUntil)`（传"明天0点"，当天到期即算）/ `getTodayReviewCount` / `getMasteredCount`（盒5=已掌握）/ `getBoxCounts()`（盒子分布，统计页用）/ `searchQuestions(query)` / 常规 CRUD；读均为 `Flow`，写为 `suspend`
- **SubjectDao**：CRUD + `getSubjectCount()` + 按名查重
- **ChapterDao / KnowledgePointDao**：仅 CRUD，无上层消费者
- **AiConfigDao**：`getEnabledConfigByTaskType(taskType)` — OCR 运行时取配置
- **AiUsageLogDao**：`getTotalCost()` / `getCostByTimeRange` / `deleteOldLogs`

## 迁移历史

v1 起，无 Migration（版本未升过）。改实体需同步加 Migration 或 fallbackToDestructiveMigration（当前均未配置）。

## 备份数据结构（BackupData, JSON v2）

强类型 6 表：`subjects[] / chapters[] / knowledgePoints[] / questions[] / aiConfigs[]（apiKey 置空）/ aiUsageLogs[]`。导入按 FK 顺序逐表 upsert（同 id 覆盖），v1 旧格式被显式拒绝。
