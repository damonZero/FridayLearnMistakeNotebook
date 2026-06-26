# 智能错题本 - 产品需求文档 (PRD)

**版本：** 1.0
**日期：** 2026-06-26
**状态：** 待开发

---

## 1. 产品概述

### 1.1 产品定位
面向小学生的智能错题本 Android APP，通过拍照识别、AI 分析、科学复习三大核心能力，帮助家长和孩子高效管理错题，提升学习效果。

### 1.2 目标用户
- **主要用户：** 家长（录入、管理错题）
- **次要用户：** 小学生（复习、练习）
- **使用场景：** 家庭学习环境

### 1.3 核心价值
拍照自动录入错题 → AI 分析知识点薄弱点 → 科学安排复习计划 → 生成相似题巩固练习

---

## 2. 功能需求

### 2.1 拍照与识别 (OCR)

| 编号 | 功能 | 优先级 | 说明 |
|------|------|--------|------|
| PHOTO-01 | 应用内拍照 | P0 | 调用手机摄像头拍摄错题 |
| PHOTO-02 | 图片裁剪旋转 | P0 | 拍照后可裁剪、旋转、增强对比度 |
| PHOTO-03 | OCR 文字识别 | P0 | 识别印刷体中文、英文、数学公式 |
| PHOTO-04 | 手写体识别 | P1 | 识别手写中文、英文（准确率待验证） |
| PHOTO-05 | 数学公式识别 | P1 | 识别分数、指数、方程等数学表达式 |
| PHOTO-06 | 手动编辑修正 | P0 | OCR 出错时可手动修改识别结果 |
| PHOTO-07 | 多图批量导入 | P2 | 拍整张试卷，自动裁剪单题 |
| PHOTO-08 | PDF 试卷导入 | P2 | 导入数字 PDF 试卷提取题目 |

### 2.2 科目与题目管理

| 编号 | 功能 | 优先级 | 说明 |
|------|------|--------|------|
| SUBJ-01 | 预设科目 | P0 | 预设语文、数学、英语三科 |
| SUBJ-02 | 动态增删科目 | P0 | 用户可添加自定义科目 |
| SUBJ-03 | 删除科目保护 | P0 | 预设科目不可删除，自定义科目可删除 |
| SUBJ-04 | 知识点树 | P1 | 层级结构：科目 > 章节 > 知识点 |
| SUBJ-05 | 错题列表 | P0 | 按科目筛选查看错题 |
| SUBJ-06 | 搜索筛选 | P1 | 按日期、知识点、错误类型筛选 |
| SUBJ-07 | 排序功能 | P1 | 按日期、复习次数、掌握程度排序 |
| SUBJ-08 | 手动录入 | P0 | 支持手动输入题目（非拍照） |

### 2.3 知识点分析

| 编号 | 功能 | 优先级 | 说明 |
|------|------|--------|------|
| KNOW-01 | AI 知识点识别 | P1 | AI 自动分析错题涉及的知识点 |
| KNOW-02 | 手动修正知识点 | P1 | 用户可修正 AI 识别的知识点 |
| KNOW-03 | 错误模式分析 | P2 | 分析多道错题找出根本原因 |
| KNOW-04 | 错误类型分类 | P1 | 分类：粗心/概念错误/方法错误/计算错误 |
| KNOW-05 | 薄弱点分析报告 | P2 | 展示知识薄弱点分布 |

### 2.4 复习与学习

| 编号 | 功能 | 优先级 | 说明 |
|------|------|--------|------|
| REV-01 | 艾宾浩斯遗忘曲线 | P0 | 基于遗忘曲线安排复习时间 |
| REV-02 | 莱特纳卡片盒系统 | P0 | 5 个盒子，答对晋级，答错降级 |
| REV-03 | 间隔重复算法 | P0 | 结合遗忘曲线和莱特纳系统 |
| REV-04 | 每日复习任务 | P0 | 打开 APP 显示今日待复习题目 |
| REV-05 | 复习结果标记 | P0 | 标记"会了"/"还错"，更新掌握程度 |
| REV-06 | 按需复习模式 | P1 | 随时复习任意筛选的错题 |
| REV-07 | AI 生成相似题 | P1 | 基于错题生成类似练习题 |
| REV-08 | 练习模式 | P1 | 做 AI 生成的相似题巩固 |

### 2.5 AI 模型配置

| 编号 | 功能 | 优先级 | 说明 |
|------|------|--------|------|
| AICFG-01 | API Key 管理 | P0 | 用户填写自己的 API Key |
| AICFG-02 | 按任务分配模型 | P1 | OCR/分析/出题可配置不同模型 |
| AICFG-03 | 默认配置 | P0 | 提供默认模型配置 |
| AICFG-04 | 连接测试 | P1 | 测试 API Key 是否有效 |
| AICFG-05 | Token 消耗统计 | P1 | 记录每次调用的 Token 数量 |
| AICFG-06 | 费用预估 | P1 | 根据 Token 单价计算费用 |

### 2.6 数据管理

| 编号 | 功能 | 优先级 | 说明 |
|------|------|--------|------|
| DATA-01 | 本地存储 | P0 | SQLite 数据库本地存储 |
| DATA-02 | 自动备份 | P0 | APP 关闭时自动备份 |
| DATA-03 | JSON 导出 | P1 | 导出数据为 JSON 格式 |
| DATA-04 | CSV 导出 | P2 | 导出数据为 CSV 格式 |
| DATA-05 | JSON 导入 | P1 | 从 JSON 备份文件恢复 |
| DATA-06 | 复习提醒通知 | P2 | Android 系统通知提醒复习 |
| DATA-07 | 提醒时间配置 | P2 | 用户配置提醒时间 |
| DATA-08 | 打印复习单 | P3 | 生成可打印的复习题单 |

### 2.7 数据分析与激励

| 编号 | 功能 | 优先级 | 说明 |
|------|------|--------|------|
| ANA-01 | 学习进度仪表盘 | P1 | 错题趋势、科目分布 |
| ANA-02 | 知识点掌握率 | P1 | 各知识点掌握百分比 |
| ANA-03 | 复习完成率 | P1 | 每日/每周复习完成情况 |
| ANA-04 | 知识点掌握地图 | P2 | 红/黄/绿标记掌握程度 |
| ANA-05 | 连续打卡 | P2 | 连续复习天数统计 |
| ANA-06 | 成就徽章 | P3 | 7 天打卡、100 道题等成就 |
| ANA-07 | 星星奖励 | P3 | 完成复习获得星星 |

### 2.8 用户界面

| 编号 | 功能 | 优先级 | 说明 |
|------|------|--------|------|
| UI-01 | 卡通风格 | P0 | 可爱卡通界面，适合小学生 |
| UI-02 | 家长模式 | P1 | 简洁清晰的管理界面 |
| UI-03 | 儿童模式 | P2 | 大按钮、简单语言、活泼动画 |
| UI-04 | 中文界面 | P0 | 所有界面使用中文 |
| UI-05 | 横屏适配 | P1 | 支持手机横屏使用 |

---

## 3. AI 模型配置方案

### 3.1 推荐模型组合

| 任务 | 推荐模型 | 备选模型 | 说明 |
|------|----------|----------|------|
| **OCR 识别** | 火山方舟 (豆包 Vision) | 百度 OCR、GPT-4 Vision | 中文手写识别最强 |
| **错题分析** | DeepSeek | Claude、GPT-4 | 性价比高，中文好 |
| **生成相似题** | DeepSeek | Claude、GPT-4 | 同上 |
| **复习排期** | 本地算法 | — | 艾宾浩斯+莱特纳，不需要 AI |

### 3.2 Token 价格参考（每 1000 tokens）

| 模型 | 输入价格 | 输出价格 |
|------|----------|----------|
| 火山方舟 (豆包 Vision) | ¥0.008 | ¥0.02 |
| DeepSeek Chat | ¥0.001 | ¥0.002 |
| DeepSeek Reasoner | ¥0.004 | ¥0.016 |
| GPT-4o | ¥0.04 | ¥0.12 |
| GPT-4 | ¥0.2 | ¥0.6 |
| Claude 3 Sonnet | ¥0.024 | ¥0.12 |
| Claude 3 Opus | ¥0.12 | ¥0.6 |

---

## 4. 技术架构

### 4.1 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| **移动端** | .NET MAUI (Android) | 跨平台框架，C# 开发 |
| **UI 框架** | MAUI XAML | 原生 Android UI |
| **MVVM** | CommunityToolkit.Mvvm | 简化 MVVM 开发 |
| **数据库** | EF Core + SQLite | 本地数据存储 |
| **AI 集成** | HttpClient + API | 火山方舟、DeepSeek |
| **日志** | Serilog | 文件日志 |
| **测试** | xUnit | 单元测试 |

### 4.2 项目结构

```
Friday/
├── Friday.Domain/           # 领域层（实体、枚举、业务规则）
│   ├── Entities/
│   │   ├── Subject.cs
│   │   ├── Chapter.cs
│   │   ├── KnowledgePoint.cs
│   │   ├── Question.cs
│   │   ├── AiConfig.cs
│   │   └── AiUsageLog.cs
│   └── Enums/
│       └── ErrorType.cs
│
├── Friday.Application/      # 应用层（接口、服务、业务逻辑）
│   ├── Interfaces/
│   │   ├── ISubjectRepository.cs
│   │   ├── IChapterRepository.cs
│   │   ├── IKnowledgePointRepository.cs
│   │   ├── IQuestionRepository.cs
│   │   ├── IAiService.cs
│   │   ├── IAiConfigService.cs
│   │   ├── IExportImportService.cs
│   │   └── IBackupService.cs
│   └── Services/
│       ├── SubjectService.cs
│       └── KnowledgeTreeService.cs
│
├── Friday.Infrastructure/   # 基础设施层（数据库、外部服务）
│   ├── Persistence/
│   │   ├── AppDbContext.cs
│   │   └── Repositories/
│   └── Services/
│       ├── AiService.cs
│       ├── AiConfigService.cs
│       ├── ExportImportService.cs
│       └── BackupService.cs
│
├── Friday.Mobile/           # MAUI Android 应用
│   ├── Pages/
│   ├── ViewModels/
│   ├── Services/
│   ├── Converters/
│   └── Platforms/Android/
│
└── Friday.Tests/            # 单元测试
```

### 4.3 数据模型

#### Subject（科目）
```csharp
public class Subject
{
    public Guid Id { get; set; }
    public string Name { get; set; }           // 科目名称
    public string? Icon { get; set; }           // 图标
    public string? Color { get; set; }          // 颜色
    public int SortOrder { get; set; }          // 排序
    public bool IsPreset { get; set; }          // 是否预设
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    public List<Chapter> Chapters { get; set; }
}
```

#### Chapter（章节）
```csharp
public class Chapter
{
    public Guid Id { get; set; }
    public Guid SubjectId { get; set; }
    public string Name { get; set; }
    public int SortOrder { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    public Subject Subject { get; set; }
    public List<KnowledgePoint> KnowledgePoints { get; set; }
}
```

#### KnowledgePoint（知识点）
```csharp
public class KnowledgePoint
{
    public Guid Id { get; set; }
    public Guid ChapterId { get; set; }
    public string Name { get; set; }
    public string? Description { get; set; }
    public int SortOrder { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    public Chapter Chapter { get; set; }
    public List<Question> Questions { get; set; }
}
```

#### Question（错题）
```csharp
public class Question
{
    public Guid Id { get; set; }
    public Guid? SubjectId { get; set; }
    public Guid? ChapterId { get; set; }
    public Guid? KnowledgePointId { get; set; }
    public string Content { get; set; }          // 题目内容
    public string? Answer { get; set; }           // 正确答案
    public string? UserAnswer { get; set; }       // 用户答案
    public ErrorType ErrorType { get; set; }      // 错误类型
    public string? ImagePath { get; set; }        // 图片路径
    public string? Notes { get; set; }            // 备注
    public DateTime? ReviewDate { get; set; }     // 下次复习时间
    public int LeitnerBox { get; set; }           // 莱特纳盒子 (1-5)
    public double EaseFactor { get; set; }        // 难度系数
    public int IntervalDays { get; set; }         // 复习间隔天数
    public int Streak { get; set; }               // 连续答对次数
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    public Subject? Subject { get; set; }
    public Chapter? Chapter { get; set; }
    public KnowledgePoint? KnowledgePoint { get; set; }
}
```

#### ErrorType（错误类型枚举）
```csharp
public enum ErrorType
{
    Unknown = 0,      // 未知
    Careless = 1,     // 粗心
    Conceptual = 2,   // 概念错误
    Method = 3,       // 方法错误
    Calculation = 4   // 计算错误
}
```

#### AiConfig（AI 配置）
```csharp
public class AiConfig
{
    public Guid Id { get; set; }
    public string Provider { get; set; }         // 提供商: volcano_ark, deepseek, openai, claude
    public string ApiKey { get; set; }           // API Key
    public string? BaseUrl { get; set; }         // 自定义 Base URL
    public string? ModelName { get; set; }       // 模型名称
    public string TaskType { get; set; }         // 任务类型: ocr, analysis, generation
    public bool IsEnabled { get; set; }          // 是否启用
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}
```

#### AiUsageLog（AI 使用日志）
```csharp
public class AiUsageLog
{
    public Guid Id { get; set; }
    public string Provider { get; set; }
    public string TaskType { get; set; }
    public string ModelName { get; set; }
    public int InputTokens { get; set; }
    public int OutputTokens { get; set; }
    public int TotalTokens { get; set; }
    public decimal EstimatedCost { get; set; }   // 预估费用（人民币）
    public string? RequestId { get; set; }
    public bool IsSuccess { get; set; }
    public string? ErrorMessage { get; set; }
    public DateTime CreatedAt { get; set; }
}
```

---

## 5. 学习算法

### 5.1 艾宾浩斯遗忘曲线

复习间隔：1天 → 2天 → 4天 → 7天 → 15天 → 30天

### 5.2 莱特纳卡片盒系统

- **盒子 1：** 新题 / 答错的题（每天复习）
- **盒子 2：** 答对 1 次（每 2 天复习）
- **盒子 3：** 答对 2 次（每 4 天复习）
- **盒子 4：** 答对 3 次（每 7 天复习）
- **盒子 5：** 已掌握（每 15 天复习）

**规则：**
- 答对 → 升级到下一个盒子
- 答错 → 降回盒子 1

### 5.3 综合算法

```
下次复习时间 = 当前时间 + 间隔天数
间隔天数 = 基础间隔 × 难度系数
难度系数 = max(1.3, 难度系数 + (0.1 - (5-得分) × (0.08 + (5-得分) × 0.02)))
```

---

## 6. 非功能需求

### 6.1 性能
- APP 启动时间 < 3 秒
- 页面切换动画流畅（60fps）
- OCR 识别响应时间 < 5 秒

### 6.2 兼容性
- Android 8.0+ (API 26+)
- 支持主流手机品牌（华为、小米、OPPO、vivo）
- 支持横屏和竖屏

### 6.3 数据安全
- 数据本地存储，不上传云端
- API Key 加密存储
- 自动备份防止数据丢失

### 6.4 用户体验
- 操作简单直观，家长 3 分钟上手
- 卡通风格界面，吸引孩子使用
- 中文界面，无语言障碍

---

## 7. 开发计划

### Phase 1：基础框架 ✅
- [x] 项目架构搭建
- [x] 数据库设计
- [x] 科目管理功能
- [x] 基础 UI 框架

### Phase 2：拍照识别
- [ ] 摄像头调用
- [ ] 图片裁剪增强
- [ ] OCR 集成（火山方舟）
- [ ] 识别结果编辑

### Phase 3：复习引擎
- [ ] 艾宾浩斯算法实现
- [ ] 莱特纳系统实现
- [ ] 每日复习任务
- [ ] 复习结果统计

### Phase 4：AI 智能
- [ ] AI 配置界面
- [ ] 知识点分析
- [ ] 相似题生成
- [ ] Token 统计

### Phase 5：数据管理
- [ ] 数据导出导入
- [ ] 自动备份
- [ ] 学习统计报表
- [ ] 成就系统

---

## 8. 验收标准

### 8.1 核心功能验收
- [ ] 能拍照并识别印刷体中文（准确率 > 90%）
- [ ] 能拍照并识别数学公式（准确率 > 80%）
- [ ] 能按科目管理错题
- [ ] 能根据遗忘曲线安排复习
- [ ] 能生成相似练习题

### 8.2 性能验收
- [ ] APP 启动 < 3 秒
- [ ] OCR 识别 < 5 秒
- [ ] 复习列表加载 < 1 秒

### 8.3 用户体验验收
- [ ] 家长能在 3 分钟内完成首次错题录入
- [ ] 孩子能独立完成每日复习
- [ ] 界面卡通可爱，孩子喜欢

---

## 9. 风险与限制

### 9.1 技术风险
- **OCR 准确率：** 手写体识别准确率可能不达标
  - 缓解方案：提供手动编辑功能
- **AI API 稳定性：** 外部 API 可能不稳定
  - 缓解方案：本地缓存，离线可用基础功能

### 9.2 成本风险
- **AI API 费用：** 高频使用可能产生较高费用
  - 缓解方案：Token 统计，费用预警

### 9.3 用户风险
- **学习曲线：** 家长可能不熟悉 APP 操作
  - 缓解方案：简洁 UI，新手引导

---

## 10. 附录

### 10.1 名词解释
- **OCR：** 光学字符识别
- **莱特纳系统：** 基于卡片盒的间隔重复学习法
- **艾宾浩斯曲线：** 描述遗忘规律的数学曲线
- **Token：** AI 模型处理文本的基本单位

### 10.2 参考资料
- 艾宾浩斯遗忘曲线研究
- 莱特纳卡片盒系统
- Anki 间隔重复算法
- SM-2 算法实现

---

**文档维护：** 本文档随项目迭代持续更新
**联系方式：** [待填写]
