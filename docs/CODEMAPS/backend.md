<!-- Generated: 2026-09-30 | Files scanned: 45 | Token estimate: ~850 -->

# 数据与业务层（domain + data 层）

## 远程服务（AiChatService，OpenAI 兼容 /chat/completions）

```
chat(taskType, prompt)                    文本对话（分析/出题）
chatWithImage(taskType, base64, prompt)   图文混合（识题）
内部：resolveConfig(SIMILAR_QUESTION 兼容旧 GENERATE) → 互斥单飞重试×2
     → extractContent（兼容分段数组；正文空→报错，不读思考文本）
     → AiUsageLogger.log（token/费用按模型名估算，供应商名不参与计价）
max_tokens=8192（思考型模型推理 + 多小问长 JSON）

analyzeQuestionImage(image) → QuestionExtraction{content, userAnswer, answer, knowledgePoint}
  多小问大题：content 完整保留 (1)(2) 编号；userAnswer/answer 按小问分组
analyzeKnowledge(...)      → KnowledgeAnalysis{knowledgePoints, errorTypeGuess, analysis}
generateSimilarQuestions() → List<GeneratedQuestion>{content, answer, variation}
  梯度：①同型巩固 ②情境变换 ③逆向综合；多小问原题优先针对核心小问
SimilarQuestionCodec       ↔ questions.similarQuestions JSON 列编解码
```

## 用例 → 仓库 → DAO（主要链路）

| UseCase | Repository | DAO |
|---|---|---|
| AddQuestionUseCase(+knowledgePoint) | addQuestion | insertQuestion |
| GetQuestionsForReviewUseCase | getQuestionsForReview | getQuestionsForReview(dueUntil=明日0点, 分钟时钟) |
| ProcessReviewResultUseCase | processReviewResult | updateQuestion（经算法） |
| Subject 三件套 | SubjectRepository | SubjectDao（isPreset 删除保护） |

直接方法（_repository 直连）：getBoxCounts[/BySubject]、getTodayDueCount[BySubject]、
updateAnalysis(id, aiAnalysis, knowledgePoint)、updateSimilarQuestions(id, json)、searchQuestions

## 算法 — SpacedRepetitionAlgorithm

score 钳制 1..5；答错→盒1固定次日；答对→盒+1(≤5)、间隔=round(基础[1,2,4,7,15]×EF)、EF∈[1.3,2.5]

## 备份 — BackupManager（v2 全 6 表）

导出（事务内读，apiKey 置空）/ 导入（FK 序 upsert，v2 校验）/ autoBackup(onStop) / SAF 导出

## 纸质练习卷 — print/PracticeSheetPdfGenerator

```
generate(items: List<SheetItem>, includeSimilar, title) : SheetFiles{练习卷, 答案卷}  [Mutex 单飞]
  Writer：A4 595×842pt，StaticLayout 按行界分页（不截行），页脚每页绘制
  练习卷：题干+原图(compress采样)+纯空白作答区+签名栏
  答案卷：正确答案 → 举一反三答案 → AI 分析 → 孩子上次记录 + 记录栏
shareSheetFile(ctx, file)   单文件 ACTION_SEND（微信文件通道；多文件 SEND_MULTIPLE 微信不收）
printExerciseSheet(ctx, f)  PrintManager + 文件拷贝 Adapter（ISO_A4）
文件名分钟戳 + exists 碰撞追加序号；writeTo use + try/finally
```

## DI（`di/`）

DatabaseModule（v4 + 3 个 Migration）/ RepositoryModule（@Binds）/ AppModule、GsonModule
