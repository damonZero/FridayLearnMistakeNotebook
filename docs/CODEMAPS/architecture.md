<!-- Generated: 2026-09-30 | Files scanned: 90+ | Token estimate: ~750 -->

# 系统架构 — 周周错题本 (FridayLearnMistakeNotebook)

小学生智能错题本 Android APP。MVVM + Clean Architecture，数据全本地。
AI 全家桶由用户自配 DeepSeek（默认）或火山方舟，识图/分析/出题按任务分配模型。
学习闭环：拍照 AI 识题 → 录入预填 → 科学复习 → 举一反三 → 纸质练习卷（护眼）→ 家长回录。

## 分层图

```
┌──────────────────────────────────────────────────────────────┐
│ UI (Jetpack Compose, Material3)                 ui/*          │
│  导航：复习(启动页) → 首页 → 科目 → 设置，13 个 Screen          │
│  多选打印底栏 / 拍照结果卡内滚 / 科目概况四格统计                 │
├──────────────────────────────────────────────────────────────┤
│ Domain (纯 Kotlin)                              domain/*      │
│  UseCases ×6 ｜ SpacedRepetitionAlgorithm（盒1-5+SM-2）        │
│  Repository 接口：Question(20+ 方法) / Subject                  │
├──────────────────────────────────────────────────────────────┤
│ Data                                            data/*        │
│  Room v4（6 实体，Migration 1→4 显式）｜ BackupManager(v2 JSON) │
│  VolcanoOcrService(纯OCR) ｜ AiChatService(识题/分析/举一反三)  │
│  print/PracticeSheetPdfGenerator（题卷/答案卷 A4 PDF）          │
├──────────────────────────────────────────────────────────────┤
│ DI (Hilt, SingletonComponent)：App/Database/Repository/Gson    │
└──────────────────────────────────────────────────────────────┘
```

## 入口

- `FridayNotebookApp` — @HiltAndroidApp + 启动时清扫孤儿图片
- `MainActivity` — 单 Activity；onStop 触发自动备份

## 核心数据流

**拍照录入（AI 一次识题）**：
```
CameraScreen → CameraViewModel
  → AiChatService.analyzeQuestionImage(base64)   [多模态单调用]
      {题干(多小问完整) + 学生手写作答 + 多解法答案 + 考点标签}
  → 失败且为解析错误 → VolcanoOcrService 回退纯 OCR
  → savedStateHandle 原子回传 → AddQuestion 三字段+知识点预填（空值不覆盖）
```

**复习循环**：
```
ReviewScreen(启动页) → 会话快照 → 看答案 → 举一反三入口
  → SpacedRepetitionAlgorithm → Room
```

**举一反三**：
```
PracticeScreen(缓存秒开) / 详情页 / 列表批量
  → AiChatService.generateSimilarQuestions（同型巩固→情境变换→逆向综合梯度）
  → 落库 questions.similarQuestions(JSON) → 刷新按钮覆盖重生成
```

**纸质练习卷**：
```
详情页(单题含举一反三) / 列表多选底栏(批量，缓存优先)
  → PracticeSheetPdfGenerator → 题卷.pdf + 答案卷.pdf
  → 分享面板(微信文件通道) / PrintManager 直印
```

## 关键设计决策

- 复习列表会话快照化；AI 预填"空值不覆盖"；AI 失败按类型分类回退
- 生成类 AI 调用 max_tokens=8192（思考型模型推理+多小问长 JSON）
- 知识点标签/举一反三/原图均随错题持久化；备份含全部 6 表且 Key 脱敏
- Room 禁止破坏式迁移；启动清扫孤儿图片
