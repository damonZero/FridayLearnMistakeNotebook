<!-- Generated: 2026-09-30 | Files scanned: 5 | Token estimate: ~420 -->

# 依赖清单

## 技术栈（`app/build.gradle.kts`）

| 类别 | 依赖 | 版本 |
|---|---|---|
| 构建 | Kotlin + AGP, compileSdk/targetSdk 34, minSdk 26, JDK 17 | — |
| UI | Compose BOM + Material3 + icons-extended + animation | 2024.02.00 |
| 架构 | Navigation-Compose / Lifecycle-ViewModel-Compose | 2.7.6 / 2.7.0 |
| DI | Hilt (KSP) + hilt-navigation-compose | 2.50 / 1.1.0 |
| 数据 | Room runtime/ktx/compiler (KSP, schemaLocation=schemas/) | 2.6.1 |
| 异步 | kotlinx-coroutines-android | 1.7.3 |
| 序列化 | Gson（识题/举一反三 JSON 编解码亦用） | 2.10.1 |
| 相机/图片 | CameraX 1.3.1 / Coil-Compose 2.5.0 / exifinterface 1.3.7 | — |
| 网络 | OkHttp + logging-interceptor（AI 调用全走此处） | 4.12.0 |
| PDF | android.graphics.pdf.PdfDocument + StaticLayout（系统内置，零三方依赖） | — |
| 测试 | JUnit4, mockito-kotlin, coroutines-test; CI: GitHub Actions (Temurin 17) | — |

## 外部服务

- **DeepSeek 官方 API（默认模板）** — `deepseek-flash`（V4.1 原生多模态）识图+推理统一；baseUrl `https://api.deepseek.com`
- **火山方舟（备选模板）** — 豆包 Vision；同 OpenAI 兼容协议
- Key 由用户填写；计费估算按模型名匹配（供应商自由文本不参与）

## Android 权限

CAMERA（required=false）/ READ_MEDIA_IMAGES / 旧存储权限(≤32/≤28) / INTERNET + ACCESS_NETWORK_STATE

## 组件

- FridayNotebookApp（@HiltAndroidApp + 孤儿图片清扫）
- MainActivity（单 Activity + onStop 自动备份）
- FileProvider（cache / files/images / **files/exports**——练习卷 PDF 分享用）

## 版本控制注意

schemas/1~4.json 迁移基线入库；.gitignore 排除 .omo/.reports/*.apk
