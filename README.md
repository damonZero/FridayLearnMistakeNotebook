# 周周错题本 (FridayLearnMistakeNotebook)

面向小学生的智能错题本 Android 应用：家长拍照录入错题，AI 一次完成"识别题干 + 提取学生作答 + 多解法解答 + 考点标签"，孩子按科学的间隔复习并做"举一反三"巩固练习，支持导出纸质练习卷（题卷/答案卷分离）护眼刷题，家长在 App 上一键回录。

[![Android CI](https://github.com/damonZero/FridayLearnMistakeNotebook/actions/workflows/android-ci.yml/badge.svg)](https://github.com/damonZero/FridayLearnMistakeNotebook/actions/workflows/android-ci.yml)

## 核心功能

### 录入：AI 识题一次到位
- **拍照/相册** → 自动压缩 + EXIF 矫正 + 极端长宽比增强（竖长小票也能识别）
- **一次多模态调用**同时返回：题干（大题多小问完整保留编号）、学生手写作答、多解法参考答案、考点标签——四个字段自动预填录入表单，空值不覆盖手输内容
- 识题异常自动回退纯 OCR，原图随错题留存

### 管理：多维度整理
- 科目管理（预设保护/重名校验）+ **科目概况页**（掌握进度、今日待复习、四格统计、最近错题）
- 错题列表：科目筛选、防抖搜索、**排序**（最新/最早/待复习优先）、**分组**（掌握状态/错误类型/录入日期/知识点）
- 错题卡片带错误类型与 📚 考点标签；详情页汇总原图/答案/复习状态/AI 分析

### 复习：科学排期
- 艾宾浩斯 + 莱特纳 5 盒 + SM-2 难度系数（答错次日必现，难度自适应 [1.3, 2.5]）
- 复习页为启动首屏；会话快照防崩溃、防双击、返回确认
- 看答案后一键**举一反三**：按"同型巩固 → 情境变换 → 逆向综合"梯度出题，同一考点多角度考察；题目持久化（缓存秒开），支持一键刷新

### 纸质练习卷（护眼打印）
- 错题详情页单题生成，或**列表多选批量**生成（每周 1~2 次的节奏）
- **题卷（孩子做）与答案卷（家长留存）分离**，答案卷含逐题答案与 AI 分析、家长记录栏
- A4 排版：长文本自动分页、错题原图嵌入、作答区纯留白、页脚签名栏
- 分享到微信（单文件文件通道）发电脑/打印店，或系统打印服务直印

### AI 配置与用量
- DeepSeek 官方（默认模板，`deepseek-flash` 一模型通吃识图+推理）/ 火山方舟备选模板
- 真实连接测试、Key 遮蔽、按任务分配模型；每次调用 token 与费用估算落库

### 数据与激励
- Room 本地存储（v4，显式 Migration + schema 基线）、JSON 全量备份/恢复/SAF 导出/自动备份
- 掌握度统计、连续打卡 streak、9 个成就

## 技术栈

| 层 | 技术 |
|---|---|
| 语言/UI | Kotlin 1.9 + Jetpack Compose (Material3) + Navigation Compose |
| 架构 | MVVM + Clean Architecture（ui → domain ← data，Hilt 注入） |
| 数据 | Room v4（显式 Migration + schemaLocation 基线）+ DataStore + Gson |
| 网络/AI | OkHttp（OpenAI 兼容多模态 chat/completions） |
| 相机/图片 | CameraX + Coil + ExifInterface |
| 文档 | PdfDocument + StaticLayout（系统内置 PDF 排版） |
| 测试 | JUnit4 + Mockito（30+ 单元测试）+ GitHub Actions CI |

架构详情见 [docs/CODEMAPS](docs/CODEMAPS/)（architecture / backend / frontend / data / dependencies），产品需求见 [REQUIREMENTS.md](REQUIREMENTS.md)。

## 构建运行

要求：**JDK 17+**、Android SDK 34（`local.properties` 配置 `sdk.dir`）。

```bash
./gradlew :app:testDebugUnitTest   # 单元测试
./gradlew :app:assembleDebug       # Debug APK
```

Windows 下若 `JAVA_HOME` 指向其他 JDK：
```powershell
.\gradlew.bat :app:assembleDebug "-Dorg.gradle.java.home=<JDK17+路径>"
```

安装后进入「设置 → AI 配置」选择 DeepSeek 模板、粘贴 API Key 即可使用（数据全部本地存储，不上传云端）。

## 项目结构

```
app/src/main/java/com/friday/mistakenotebook/
├── data/          # Room 实体/DAO、仓库实现、AI 服务、备份
├── domain/        # 领域模型、仓库接口、用例、间隔复习算法
├── print/         # 纸质练习卷 PDF 生成器与分享/打印
├── ui/            # Compose 页面（review/home/subject/subjectdetail/
│                  #   questionlist/questiondetail/practice/camera/...）
├── di/            # Hilt 模块
└── util/          # 图片处理（降采样/EXIF/极端长宽比）
```

## License

MIT — 见 [LICENSE](LICENSE)
