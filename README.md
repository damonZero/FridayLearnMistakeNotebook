# 周周错题本 (FridayLearnMistakeNotebook)

面向小学生的智能错题本 Android 应用：家长拍照录入错题，AI 识别文字并分析知识点，孩子按科学的间隔复习计划巩固练习。

[![Android CI](https://github.com/damonZero/FridayLearnMistakeNotebook/actions/workflows/android-ci.yml/badge.svg)](https://github.com/damonZero/FridayLearnMistakeNotebook/actions/workflows/android-ci.yml)

## 核心功能

- **拍照识别录入** — CameraX 拍照 / 相册选图，自动压缩与 EXIF 方向矫正，调用视觉大模型 OCR 识别题目文字，识别结果可编辑后入库，原图随错题留存
- **错题管理** — 预设语文/数学/英语 + 自定义科目（重名校验、预设保护）、错题列表（科目筛选/关键字搜索）、错题详情页（原图/复习状态/AI 分析/相似题入口）
- **科学复习** — 艾宾浩斯遗忘曲线 + 莱特纳 5 盒系统 + SM-2 难度系数（答错次日必现，难度自适应 [1.3, 2.5]），每日待复习任务、复习结果即时落库
- **AI 智能** — 知识点分析（按错题给出薄弱知识点与错因分析）、相似题生成与练习模式（练习不计入复习计划）
- **AI 配置** — 用户自填 API Key（OpenAI 兼容协议，默认适配火山方舟/豆包），按任务（OCR/分析/出题）分配不同模型，真实连接测试，Key 遮蔽显示，导出备份自动脱敏
- **用量统计** — 每次调用的 token 消耗与费用估算，按时间范围查看，旧日志清理
- **数据安全** — Room 本地存储、JSON 全量备份（6 表）/恢复（导入前二次确认）、SAF 导出到任意目录、退出自动备份
- **学习激励** — 掌握度分布统计、连续打卡 streak、9 个成就徽章

## 技术栈

| 层 | 技术 |
|---|---|
| 语言/UI | Kotlin 1.9 + Jetpack Compose (Material3) + Navigation Compose |
| 架构 | MVVM + Clean Architecture（ui → domain ← data，Hilt 依赖注入） |
| 数据 | Room（v2，显式 Migration）+ DataStore + Gson |
| 网络 | OkHttp（OpenAI 兼容 chat/completions） |
| 相机/图片 | CameraX + Coil + ExifInterface |
| 测试 | JUnit4 + Mockito（30+ 单元测试）+ GitHub Actions CI |

架构详情见 [docs/CODEMAPS](docs/CODEMAPS/)（architecture / backend / frontend / data / dependencies 五份精简地图），产品需求见 [REQUIREMENTS.md](REQUIREMENTS.md)。

## 构建运行

要求：**JDK 17+**、Android SDK 34（`local.properties` 配置 `sdk.dir`）。

```bash
# 单元测试
./gradlew :app:testDebugUnitTest

# 构建 Debug APK（输出 app/build/outputs/apk/debug/）
./gradlew :app:assembleDebug
```

Windows 下若 `JAVA_HOME` 指向其他 JDK，可显式指定：
```powershell
.\gradlew.bat :app:assembleDebug "-Dorg.gradle.java.home=<JDK17+路径>"
```

安装后进入「设置 → AI 配置」填写视觉模型的 baseUrl / API Key / 模型名即可开始使用（数据全部本地存储，不上传云端）。

## 项目结构

```
app/src/main/java/com/friday/mistakenotebook/
├── data/          # Room 实体/DAO、仓库实现、OCR 与 AI 服务、备份
├── domain/        # 领域模型、仓库接口、用例、间隔复习算法
├── ui/            # Compose 页面（home/subject/review/questiondetail/practice/...）
├── di/            # Hilt 模块
└── util/          # 图片处理
```

## License

MIT — 见 [LICENSE](LICENSE)
