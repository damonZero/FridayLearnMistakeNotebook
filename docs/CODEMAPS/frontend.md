<!-- Generated: 2026-09-30 | Files scanned: 34 | Token estimate: ~900 -->

# UI 层（Jetpack Compose, Material3，学习优先导航）

宿主：MainActivity → FridayNotebookNavHost
**底部导航与启动页：复习(启动) → 首页 → 科目 → 设置**

```
review(启动页)   ReviewScreen    — 会话快照；内容区滚动+固定答题按钮；返回确认；防双击
                                   看答案后【举一反三·练相似题】入口
home             HomeScreen      — 欢迎卡(点击直达复习,实时待复习数) + 统计卡 + 快捷操作
subjects         SubjectsScreen  — 科目卡(点击→概况, ›指示, 删除保护)
subject/{id}     SubjectDetailScreen — 掌握进度条 + 今日待复习/新题/学习中/已掌握四格
                                       + 最近错题(点进详情) + 全部错题/添加错题
settings         SettingsScreen  — 分区列表（未实现项点击提示"开发中"）
  ├─ add_question?subjectId   AddQuestionScreen — 三字段+知识点 AI 预填(空值不覆盖)
  │                             LazyRow chips、字数 2000、imePadding
  ├─ camera                   CameraScreen   — AI 识题(结果卡内滚,按钮固定底部)
  │                             预览有结果时收缩 180dp；权限引导
  ├─ question_list?subjectId  QuestionListScreen — 科目筛选+搜索(防抖)
  │                             排序[最新/最早/待复习优先] 分组[掌握/错误类型/日期/知识点]
  │                             多选模式: 底栏 全选/分享/打印(生成完成自动触发)
  │                             卡片: 错误类型 chip + 📚考点 chip
  ├─ question/{id}            QuestionDetailScreen — 原图/复习状态/AI 分析(落库)/
  │                             🖨 生成纸质练习卷(缓存优先)/相似题入口/删除
  ├─ practice/{id}            PracticeScreen — 缓存秒开/顶栏刷新(覆盖保存)/梯度标签
  │                             (🟩同型巩固 🟨情境变换 🟦逆向综合)/再练一组=重放
  ├─ stats                    StatsScreen   — 真实三档分布(合计100%)/待复习口径
  ├─ ai_config                AiConfigScreen — DeepSeek/火山模板一键预填；
  │                             chip 选中=appliedTemplate；Key 遮蔽；真实连接测试
  ├─ ai_usage                 AiUsageScreen — token/费用日志(全任务类型)
  ├─ backup                   BackupScreen  — SAF 导出/导入二次确认
  └─ achievement              AchievementScreen — LazyVerticalGrid 真实 streak
```

## ViewModel ↔ Screen（@HiltViewModel + StateFlow<UiState>，13 个）

| ViewModel | 要点 |
|---|---|
| ReviewViewModel | 会话快照+isSubmitting 防抖；三态(加载/空/完成) |
| CameraViewModel | startCapture 单飞(seq+取消)；IO 解码一次复用；AI 失败分类回退 |
| AddQuestionViewModel | 预填空值不覆盖；2000 字校验；防重复提交 |
| QuestionListViewModel | 排序/分组引擎(sections)、多选、批量生成(缓存优先+进度) |
| QuestionDetailViewModel | 分析落库(updateAnalysis 含考点)；练习卷生成(缓存优先) |
| PracticeViewModel | 缓存优先+refreshQuestions+restart 分离；自评仅本地 |
| SubjectDetailViewModel | combine 四流单科统计 |
| AiConfigViewModel | 模板 appliedTemplate/modelCustomized 脏标记 |
| Home/Stats/Achievement/AiUsage/Backup/SubjectViewModel | 统计/streak/备份等 |

## 状态传递约定

相机→录入：savedStateHandle `ocr_result` / `ocr_image_path` / `ocr_answer` / `ocr_user_answer` / `ocr_knowledge_point`
（读本页 backStackEntry，不读 currentBackStackEntry——转场期会串页）

## 主题

Theme.kt 深浅两套（primaryContainer 低饱和配对色）；页面取色统一 colorScheme
