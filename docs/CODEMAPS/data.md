<!-- Generated: 2026-09-30 | Files scanned: 15 | Token estimate: ~650 -->

# 数据库 — Room（MistakeNotebookDatabase, **v4**, exportSchema=true → schemas/1~4.json）

迁移（全部显式，DatabaseModule）：1→2 aiAnalysis ｜ 2→3 knowledgePoint ｜ 3→4 similarQuestions

## 实体与关系

```
subjects 1 ── n chapters 1 ── n knowledge_points
    └──────── n questions（FK 可空，CASCADE）
独立表：ai_configs / ai_usage_logs
```

| 表 | 关键字段 | 备注 |
|---|---|---|
| `subjects` | name, icon, color, isPreset | 预设语文/数学/英语 |
| `questions` | content, answer, userAnswer, errorType, imagePath, **nextReviewDate, leitnerBox(1-5), easeFactor, streak, reviewCount, aiAnalysis(摘要), knowledgePoint(考点标签), similarQuestions(举一反三 JSON)** | 核心表 |
| `ai_configs` | provider, apiKey, baseUrl, modelName, taskType, isEnabled | taskType: OCR/ANALYSIS/SIMILAR_QUESTION(+兼容 GENERATE)；Key 明文(导出脱敏) |
| `ai_usage_logs` | provider, taskType, modelName, inputTokens, outputTokens, estimatedCost | 全任务类型落账 |
| `chapters` / `knowledge_points` | FK 结构 | 暂无管理 UI |

## QuestionDao（22 方法）

- 复习：getQuestionsForReview(dueUntil=明日0点) / getTodayReviewCount / getQuestionsForReviewBySubject
- 统计：getTotalQuestionCount / getMasteredCount(盒5) / getBoxCounts / **getBoxCountsBySubject** / **getTodayDueCountBySubject**
- 局部更新（防整行回写）：**updateAnalysis(id, aiAnalysis, knowledgePoint)** / **updateSimilarQuestions(id, json)**
- CRUD / searchQuestions(content,answer LIKE)
- 所有 Flow 读；写 suspend。dueUntil 类查询由 Repository 分钟时钟驱动

## 备份（BackupData JSON v2，强类型 6 表）

subjects/chapters/knowledgePoints/questions/aiConfigs(apiKey置空)/aiUsageLogs
导入：FK 顺序逐表 upsert（同 id 覆盖，aiConfigs 空 key 保留本地）；v1 格式拒绝
