# BottomSheet SPM 使用指南

## 架构概览

本项目实现了一个 Compose Multiplatform 的 BottomSheet 组件，采用 `expect/actual` 模式：

```
commonMain    → expect fun BottomSheet(...)     # 统一 API
androidMain   → actual fun BottomSheet(...)     # Android: ComponentDialog + 动画
iosMain       → actual fun BottomSheet(...)     # iOS: Kotlin 桥接 → Swift Package (UIKit)
```

iOS 端没有使用 Kotlin/ObjC interop 直接操作 UIKit，而是通过 **本地 Swift Package** (`bottomsheet-spm/`) 实现原生行为，Kotlin 端仅保留约 70 行桥接代码。

### 为什么用 Swift Package 而不是纯 Kotlin interop？

Compose Multiplatform 在 iOS 上不通过 UIKit 视图层次渲染，导致：
- `boundsInWindow()` 返回 Compose 内部坐标系，无法可靠转换为 UIKit 窗口坐标
- 自定义转场动画、手势交互依赖 UIKit API，用 Kotlin interop 编写复杂且容易出错

解决方案：将 UIKit 逻辑写在 Swift 中编译为静态库，Kotlin 通过 SwiftPM import 调用。

---

## 模块结构

```
compose-bottom-sheet/
├── bottomsheet-spm/                          # Swift Package（原生 UIKit 实现）
│   ├── Package.swift                         # SPM 清单，产出静态库 BottomSheetSPM
│   └── Sources/BottomSheetSPM/
│       └── BottomSheetController.swift       # UIKit 弹出控制器 + 转场动画
│
├── bottomsheet-mpp/                          # Kotlin Multiplatform 库
│   ├── build.gradle.kts                      # 配置 swiftPMDependencies 导入本地 Swift Package
│   └── src/
│       ├── commonMain/.../BottomSheet.kt     # expect 声明 + BottomSheetProperties
│       ├── androidMain/.../BottomSheet.android.kt  # Android actual（ComponentDialog）
│       └── iosMain/.../BottomSheet.ios.kt    # iOS actual（~70 行 Kotlin 桥接）
│
└── iosApp/                                   # iOS 宿主应用
    ├── iosApp/ContentView.swift              # SwiftUI 包装 Compose UIViewController
    └── KotlinMultiplatformLinkedPackage/     # Xcode 合成包，链接 Swift 依赖
        └── Package.swift                     # 声明对 bottomsheet-spm 的依赖
```

---

## 分层详解

### 1. Common 层 — 统一 API

`bottomsheet-mpp/src/commonMain/.../BottomSheet.kt`

```kotlin
@Composable
expect fun BottomSheet(
    onDismissRequest: () -> Unit,
    properties: BottomSheetProperties = BottomSheetProperties.Default,
    content: @Composable ColumnScope.() -> Unit,
)
```

`BottomSheetProperties` 控制行为：
- `dismissOnBackPress` — 是否响应返回键（Android）/ 手势返回
- `dismissOnClickOutside` — 点击外部区域是否关闭

### 2. iOS Swift Package — 原生 UIKit 实现

`bottomsheet-spm/Sources/BottomSheetSPM/BottomSheetController.swift`

核心类：

| 类 | 职责 |
|---|------|
| `BottomSheetController` | 容器 VC，持有内容 VC，管理截图替换逻辑 |
| `BottomSheetPresentationController` | 控制遮罩层（dimming view）和布局（屏幕下半部分） |
| `BottomSheetTransitioningDelegate` | 分配 presentation controller 和自定义 animator |
| `BottomSheetAnimator` | 入场/出场动画（缩放 + 透明度，底部锚点） |

关键行为：
- **入场动画**: 从底部缩放 (0.8) + 淡入 → 原尺寸，0.15s EaseOut
- **出场动画**: 截图替换 → 缩放 (0.8) + 淡出，0.15s EaseIn
- **截图替换**: dismiss 时先 `drawHierarchy` 截图，用 `UIImageView` 替代 Compose 内容，避免动画过程中重组
- **点击外部关闭**: 遮罩层添加 `UITapGestureRecognizer`

### 3. iOS Kotlin 桥接 — 极简连接层

`bottomsheet-mpp/src/iosMain/.../BottomSheet.ios.kt`

```kotlin
import swiftPMImport.BottomSheet.bottomsheet.mpp.BottomSheetController

@Composable
actual fun BottomSheet(...) {
    val uiViewController = LocalUIViewController.current

    val controller = remember {
        val contentVC = ComposeUIViewController { /* 内容 */ }
        BottomSheetController(contentViewController = contentVC)
    }

    // 设置关闭回调
    controller.onDismissHandler = { onDismissRequest() }

    // 生命周期管理
    DisposableEffect(controller) {
        uiViewController.presentViewController(controller, animated = true)
        onDispose {
            controller.dismissViewControllerAnimated(true)
        }
    }
}
```

桥接层只做三件事：
1. 创建 Compose 内容 VC 并传给 Swift 层
2. 绑定 `onDismissHandler` 回调
3. 管理 present/dismiss 生命周期

### 4. Gradle 配置 — SwiftPM 集成

`bottomsheet-mpp/build.gradle.kts`

```kotlin
kotlin {
    swiftPMDependencies {
        iosMinimumDeploymentTarget.set("16.0")
        localSwiftPackage(
            directory = project.layout.projectDirectory.dir("../bottomsheet-spm"),
            products = listOf("BottomSheetSPM")
        )
    }
}
```

要求：**Kotlin 2.4.0-Beta2+** 才支持 `localSwiftPackage`。

### 5. iOS 宿主应用集成

`iosApp/KotlinMultiplatformLinkedPackage/Package.swift`

Xcode 通过这个合成包解析 Swift 依赖链：

```swift
dependencies: [
    .package(path: "../../bottomsheet-spm")
],
targets: [
    .target(name: "KotlinMultiplatformLinkedPackage", dependencies: [
        .product(name: "BottomSheetSPM", package: "bottomsheet-spm")
    ])
]
```

`ContentView.swift` 中用 `UIViewControllerRepresentable` 包装 Compose 的 `MainViewController()`。

---

## 使用方式

在 Compose Multiplatform 共享代码中直接调用：

```kotlin
@Composable
fun MyScreen() {
    var showSheet by remember { mutableStateOf(false) }

    Button(onClick = { showSheet = true }) {
        Text("Show BottomSheet")
    }

    if (showSheet) {
        BottomSheet(
            onDismissRequest = { showSheet = false },
            properties = BottomSheetProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
        ) {
            // BottomSheet 内容（Compose 代码）
            Text("Hello from BottomSheet")
            Button(onClick = { showSheet = false }) {
                Text("Close")
            }
        }
    }
}
```

---

## 构建

```bash
# Android
./gradlew :bottomsheet-mpp:assembleDebug

# iOS Framework 生成
./gradlew :bottomsheet-mpp:linkDebugFrameworkIosArm64
./gradlew :bottomsheet-mpp:linkDebugFrameworkIosSimulatorArm64

# 首次添加/修改 SwiftPM 依赖后，需重新生成 Xcode 集成包
./gradlew :bottomsheet-mpp:integrateLinkagePackage
```

---

## 注意事项

- Swift 类需要 `@objcMembers` 注解才能被 Kotlin/Native 可见
- import 路径格式: `swiftPMImport.<group>.<project>.<ModuleName>.<ClassName>`
- `ComposeUIViewController` 必须在 `@Composable` 上下文中调用，用 `remember {}` 创建
- `bottomsheet-spm/.build/` 和 `bottomsheet-mpp/build/` 已通过 `.gitignore` 排除
