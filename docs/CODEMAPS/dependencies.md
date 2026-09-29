<!-- Generated: 2026-09-27 | Files scanned: 3 (build.gradle.kts x2, manifest) | Token estimate: ~450 -->

# 依赖清单

## 技术栈（`app/build.gradle.kts`）

| 类别 | 依赖 | 版本 |
|---|---|---|
| 构建 | Kotlin + AGP, compileSdk/targetSdk 34, minSdk 26, JDK 17 | — |
| UI | Compose BOM + Material3 + icons-extended + animation | 2024.02.00 |
| 架构 | Navigation-Compose / Lifecycle-ViewModel-Compose | 2.7.6 / 2.7.0 |
| DI | Hilt Android + hilt-navigation-compose (KSP) | 2.50 / 1.1.0 |
| 数据 | Room runtime/ktx/compiler (KSP) | 2.6.1 |
| 异步 | kotlinx-coroutines-android | 1.7.3 |
| 偏好 | DataStore-Preferences | 1.0.0 |
| 序列化 | Gson | 2.10.1 |
| 相机 | CameraX core/camera2/lifecycle/view | 1.3.1 |
| 图片 | Coil-Compose | 2.5.0 |
| 网络 | OkHttp + logging-interceptor | 4.12.0 |
| 测试 | JUnit4, mockito-kotlin, coroutines-test; androidTest: Espresso + Compose UI test | — |

编译期处理：KSP（Room + Hilt 编译器）；Compose 编译器扩展 1.5.8。

## 外部服务

- **火山方舟（豆包 Vision）** — OCR，OpenAI 兼容 `/chat/completions`，BaseUrl/Key/Model 全部由用户在 ai_configs 表配置，无内置默认端点
- 无其他第三方服务；无 Analytics / Crash 上报

## Android 权限（AndroidManifest.xml）

- `CAMERA`（uses-feature required=false）
- `READ_MEDIA_IMAGES`；`READ/WRITE_EXTERNAL_STORAGE`（限 SDK ≤32 / ≤28）
- `INTERNET` + `ACCESS_NETWORK_STATE`

## 组件（Manifest）

- `FridayNotebookApp`（@HiltAndroidApp）+ `MainActivity`（LAUNCHER，单 Activity）
- FileProvider（authorities `${applicationId}.fileprovider`，paths 配置于 `res/xml/file_paths.xml`）— 拍照照片 URI 共享

## 版本控制注意

仓库含 `周周错题本-debug.apk`（19MB，已提交在历史中）与 `app/build/` 构建产物；`.gitignore` 已加 `*.apk`。
