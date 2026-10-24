# MemorialDays

完全离线、无广告、零运行时权限的多事件纪念日 / 倒数日 Android 应用。Kotlin + Jetpack Compose + Room + Jetpack Glance，单 module。

- 包名：`com.example.memorialdays`
- `minSdk 26`（Android 8.0），`targetSdk 36`，`compileSdk 36`
- 无网络、无账号、无第三方 SDK（除用户指定的农历库）、无统计、无广告
- 数据只存在应用私有 Room 数据库

## 功能

- 单一排序列表：第一项渲染为 220dp Hero Card，其余渲染为 80dp 紧凑列表卡；拖到第一位自动展开
- Hero / 列表卡均为纯色卡面（浅色纯白、深色 `#1E1E20`），无半透明、无阴影
- 背景纵向渐变叠加 20% 主题色，壁纸色彩更明显但文字仍在纯色卡面上
- 公历 / 农历双模式；农历事件每年按农历月日推算
- 闰月（负月份）自动回退普通月；日数不足时 clamp 到当月最后一天（覆盖除夕 29/30 日）
- 每年重复（公历）；生日 / 纪念 / 相识等类型根据标题关键词自动识别
- 生日 / 相识循环事件显示“已出生 / 已相伴 N 天”，并附“距下次生日/纪念日还有 N 天”
- 长按 300ms 浮起，拖动排序；原地松手弹出编辑 / 分享 / 删除 BottomSheet
- 长按顶部 `MEMORIALDAYS` 打开备份面板，SAF 导入 / 导出 JSON，零存储权限
- 1080×1440 海报离屏渲染（IO 线程）、FileProvider 分享、60 秒后清理缓存
- 2×2 单事件、4×2 多事件 Glance 小组件，每日一次系统更新
- 全面屏 edge-to-edge、动态取色（Android 12+）、300ms 颜色过渡、空状态呼吸动画

## 两套卡片排版

### 非置顶列表卡（约 80dp）

```text
┌──────────────────────────────────────────────┐
│ 已出生  示例事件                   XXXX 天  │
│ 公历 1990.01.01 · 农历腊月初八              │
└──────────────────────────────────────────────┘
```

- 左侧：第一行状态（`已出生` / `已相伴` / `还有` / `已过` / `就是今天`）+ 标题，标题单行省略；第二行 `公历 1990.01.01 · 农历腊月初八`
- 右侧：`displayDays` 大数字 + 小号“天”，数字纯黑/纯白
- 卡面纯白 / `#1E1E20`，无半透明、无阴影，1px 极淡描边

### 置顶 Hero Card（约 220dp）

```text
              已出生
             示例事件
             XXXX 天
     1990.01.01 · 农历腊月初八
       距下次生日还有 XX 天
```

- 自上而下：状态小字、自定义标题（最多两行）、核心数字 + 小号“天”、日期（10sp 灰字）、距下次事件（主题色）
- 数字物理尺寸约为卡片宽度的 30%，按 `BoxWithConstraints` 计算，并用 `fontScale` 修正，避免 2K 屏/大字体溢出
- 置顶的卡片固定为 Hero Card，不参与拖拽；取消置顶后回落为列表卡
- 生日示例：出生日 `1990.01.01`、今天 `2026.10.23` → 已出生 `13444` 天，距下次生日 `70` 天（单元测试覆盖）

## 项目结构

```text
MemorialDays/
├── settings.gradle.kts / build.gradle.kts / gradle.properties
├── gradle/wrapper/  gradlew  gradlew.bat
├── AGENTS.md  README.md
└── app/
    ├── build.gradle.kts  proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/example/memorialdays/
        │   │   ├── MainActivity.kt
        │   │   ├── MemorialApplication.kt
        │   │   ├── data/
        │   │   │   ├── EventEntity.kt
        │   │   │   ├── EventDao.kt
        │   │   │   ├── MemorialDatabase.kt
        │   │   │   ├── EventRepository.kt
        │   │   │   └── BackupRepository.kt
        │   │   ├── domain/
        │   │   │   ├── DayMath.kt
        │   │   │   ├── DisplayFormat.kt
        │   │   │   ├── EventDateResolver.kt
        │   │   │   ├── EventSections.kt
        │   │   │   ├── EventType.kt
        │   │   │   └── MemorialEvent.kt
        │   │   ├── share/
        │   │   │   ├── PosterModels.kt
        │   │   │   └── PosterRenderer.kt
        │   │   ├── ui/
        │   │   │   ├── ActionThrottle.kt
        │   │   │   ├── BackupSheet.kt
        │   │   │   ├── DayNumber.kt
        │   │   │   ├── EventActionSheet.kt
        │   │   │   ├── EventCard.kt
        │   │   │   ├── EventCopy.kt
        │   │   │   ├── EventEditorDialog.kt
        │   │   │   ├── EventViewModel.kt
        │   │   │   ├── HeroCard.kt
        │   │   │   ├── HomeScreen.kt
        │   │   │   ├── SafeHaptics.kt
        │   │   │   ├── SheetComponents.kt
        │   │   │   └── theme/{Color,Theme,Type}.kt
        │   │   └── widget/
        │   │       ├── WidgetData.kt
        │   │       ├── WidgetColors.kt
        │   │       ├── SingleEventWidget.kt
        │   │       ├── MultiEventWidget.kt
        │   │       ├── BaseGlanceWidgetReceiver.kt
        │   │       ├── SingleEventWidgetReceiver.kt
        │   │       ├── MultiEventWidgetReceiver.kt
        │   │       └── WidgetUpdater.kt
        │   └── res/
        │       ├── drawable/ic_launcher_foreground.xml
        │       ├── drawable/ic_launcher_monochrome.xml
        │       ├── mipmap-anydpi-v26/ic_launcher.xml
        │       ├── mipmap-anydpi-v26/ic_launcher_round.xml
        │       ├── values/{strings,colors,themes}.xml
        │       ├── values-night/{colors,themes}.xml
        │       └── xml/{single,multi}_event_widget_info.xml,
        │                data_extraction_rules.xml, file_paths.xml
        └── test/java/com/example/memorialdays/domain/
            ├── DayMathTest.kt
            ├── EventDateResolverTest.kt
            └── EventTypeTest.kt
```

## 依赖清单（全部固定版本）

| 依赖 | 版本 | 用途 |
| --- | --- | --- |
| Android Gradle Plugin | 8.13.2 | 构建 |
| Gradle | 8.13 | 构建 |
| Kotlin | 2.2.20 | 语言 |
| KSP | 2.2.20-2.0.3 | Room 注解处理 |
| Compose BOM | 2025.10.01 | Compose 版本对齐 |
| Compose UI / Foundation | BOM 管理 | UI / 手势 / Overscroll |
| Material 3 | BOM 管理 | 组件、DatePicker、BottomSheet |
| Activity Compose | 1.11.0 | `setContent` / SAF launcher |
| Lifecycle Runtime Compose | 2.9.4 | `collectAsStateWithLifecycle` |
| Lifecycle ViewModel Compose | 2.9.4 | `viewModel()` |
| Core KTX | 1.17.0 | FileProvider / 系统兼容 |
| Room Runtime / KTX | 2.8.1 | 本地数据库 |
| Room Compiler (KSP) | 2.8.1 | 代码生成 |
| Glance AppWidget | 1.2.0 | 桌面小组件 |
| cn.6tail:lunar | 1.7.4 | 农历换算（用户指定） |
| JUnit | 4.13.2 | JVM 单元测试 |

未引入 Hilt、Retrofit、OkHttp、Lottie、任何动画库、任何拖拽库、WorkManager。Glance 的 WorkManager 传递依赖继续通过 `exclude(group = "androidx.work")` 排除；小组件使用自定义 `BaseGlanceWidgetReceiver`。

> 农历库固定为用户指定的 `1.7.4`。Lint 提示存在 `1.7.7`，本次未升级，避免偏离明确版本要求。

## 构建

前置：JDK 17+（本机使用 JDK 21），Android SDK Platform 36 与 Build-Tools 36.0.0。`local.properties` 中的 `sdk.dir` 指向本机 SDK（不纳入版本控制）。

```bash
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # JVM 单元测试
./gradlew lintDebug              # Lint 报告
./gradlew assembleRelease        # R8 + 资源压缩 + release 签名
./gradlew installRelease         # 已连接设备时安装正式版
```

产物：

- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`
- 正式版 APK：`app/build/outputs/apk/release/app-release.apk`，约 1.8 MB

`assembleRelease` 会从仓库外的 `keystore.properties` 读取签名信息，密钥库是 `app/release.jks`。两者都在 `.gitignore` 里，密码不会进入版本库；缺少 `keystore.properties` 时不会执行签名。

Gradle wrapper 使用腾讯镜像并配置官方 SHA-256：
`distributionSha256Sum=20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78`。

## 数据模型与迁移

数据库版本 **5**。`EventEntity`：

```kotlin
id: Long
title: String
targetDate: String        // ISO-8601 基准公历日期
note: String?
createdAt: Long
isYearly: Boolean         // 默认 false
eventType: String         // 默认 "GENERAL"
isLunar: Boolean          // 默认 false
sortOrder: Int            // 默认 0，拖拽排序后写入
isPinned: Boolean         // v4 新增，置顶
color: Int                // v5 新增，卡片颜色（0 表示无着色）
```

迁移链为 `MIGRATION_1_2`（补 `note`）→ `MIGRATION_2_3`（补 `isYearly / eventType / isLunar / sortOrder`）→ `MIGRATION_3_4`（补 `isPinned`）→ `MIGRATION_4_5`（补 `color`），全部显式注册，没有 `fallbackToDestructiveMigration()`。

## 农历推算

`EventDateResolver`：

- 公历转农历：`Lunar.fromDate(java.util.Date)`。
- 农历转公历：使用真实 API `Lunar.fromYmd(year, month, day).solar`。
  - 6tail lunar 1.7.4 的 `LunarMonth` **没有** `getDay(int)`，因此不能使用 `LunarYear.fromYear(...).getMonth(...).getDay(...)`；这是查证依赖后的修正。
- 负月份表示闰月。目标年份没有该闰月时，回退到普通月。
- 如果目标月只有 29 天而事件是“三十”，clamp 到 29 日；这同时覆盖除夕边界。
- 候选农历年从“今天所在农历年”开始，最多向后两年，选择第一个不早于今天的公历日期。
- 所有推算在 `EventViewModel` 的 `Dispatchers.Default` 中执行；小组件在 `WidgetData.load` 的 `Dispatchers.IO` 中执行。

`EventType` 关键词：生日/出生/诞生/生辰 → BIRTHDAY；忌/逝/离开/怀念/悼 → MEMORIAL；相识/在一起/结婚/周年/纪念日 → ANNIVERSARY；其余 GENERAL。今天的状态文案分别为“生日快乐 / 今天，想念 / 今天，纪念 / 就是今天”。

## 交互

- 轻点卡片：打开编辑（修改、颜色、保存）
- 长按原地松手：弹出选项（置顶 / 取消置顶、分享海报、删除）
- 长按后上下拖动：非置顶卡片排序；置顶卡片固定，不参与拖动
- 拖到置顶区上方或列表末尾时，按松手速度回落或回弹，都带弹簧动画
- 列表每页 5 个，左右滑动翻页，滑动时底部出现页码小点
- 长按顶部 `MEMORIALDAYS`：备份面板
- 保存 / 删除 / 备份有 300ms 节流；排序松手后批量写库
- 触觉：拖拽换位、卡片碰撞和长按都会触发反馈，使用 `View.performHapticFeedback`，不需要 `VIBRATE` 权限

## 备份与海报

- 备份：`ActivityResultContracts.CreateDocument` / `OpenDocument`（SAF），JSON 格式，无存储权限。导入会覆盖当前全部纪念日。
- 海报：`PosterRenderer` 在 `Dispatchers.IO` 用 Android Canvas 绘制 1080×1440 PNG。没有使用 ComposeView 截图，因为那会把测量和绘制拉回主线程。
- 文件：`cacheDir/posters/memorial-*.png`，通过 `FileProvider` 分享。
- 清理：分享启动后 60 秒删除当前文件；每次生成前清理超过 24 小时的旧文件；分享失败立即删除。
- `FileProvider`：`android:exported="false"`、`android:grantUriPermissions="true"`，meta-data 使用兼容旧版的 `android.support.FILE_PROVIDER_PATHS`。

## 数据存储位置

- `/data/data/com.example.memorialdays/databases/memorial_days.db`
- WAL 文件：`memorial_days.db-wal` / `memorial_days.db-shm`
- 应用私有目录，无需存储权限；卸载即删除
- `allowBackup=false`、`fullBackupContent=false`、`data_extraction_rules.xml` 排除全部备份域
- 调试查看：
  ```bash
  adb shell run-as com.example.memorialdays ls -l databases
  adb shell run-as com.example.memorialdays sqlite3 databases/memorial_days.db ".tables"
  ```

## 侧载安装

```bash
adb install -r "<项目路径>/app/build/outputs/apk/release/app-release.apk"
```

也可以把 APK 传到手机点击安装。部分国产 ROM（如 ColorOS / MIUI）首次安装需要到「设置 → 应用 → 特殊应用权限 → 安装未知应用」里放行文件管理器或浏览器。

小组件：长按桌面空白处 → 小组件 / 卡片 → MemorialDays → `下一个日子`（2×2）或 `纪念日列表`（4×2）。没有调用 `requestPinAppWidget`。

## 安全与隐私

- Manifest 没有声明任何权限，APK 里只有 AndroidX 自动生成的签名级权限。
- 关闭了备份和明文流量；`data_extraction_rules` 排除云备份与设备迁移。
- 没有硬编码密钥；签名信息放在 gitignore 的 `keystore.properties`。
- 小组件 receiver 会把系统传入的 ID 和本应用实际拥有的 ID 取交集，拒绝伪造广播。
- FileProvider 不导出，只授予临时读权限；海报只写 `cacheDir`，用完清理。
- 导入备份会校验格式、标题、日期范围和备注长度，坏数据跳过。

## 性能与耗电

- 农历换算放在 `Dispatchers.Default`，数据库和海报放在 `Dispatchers.IO`，Composable 里不直接查库。
- 协程跟随 `viewModelScope` / 应用级 `SupervisorJob` / `rememberCoroutineScope` 取消。
- 小组件用自定义 `AppWidgetProvider` + `goAsync`，没有 WorkManager、Service、AlarmManager，也不保活。
- 天数只在构建列表状态或刷新小组件时计算。

## 验证结果

| 检查 | 结果 |
| --- | --- |
| `./gradlew assembleDebug` | BUILD SUCCESSFUL |
| `./gradlew testDebugUnitTest` | 16/16 通过 |
| `./gradlew lintDebug` | 0 errors，2 warnings（lunar 版本提示、mipmap 多余） |
| `./gradlew assembleRelease` | BUILD SUCCESSFUL；签名后的 `app-release.apk`，约 1.8 MB |
| 真机测试 | OnePlus 13 / ColorOS 17 / 2K 屏，拖拽、回弹、置顶、分页和卡片排版均通过 |
| APK 权限 | 仅签名级 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` |

## 已知限制

- 已在 OnePlus 13（ColorOS 17，2K 屏）上完成实测，拖拽、回弹、置顶、分页和卡片排版都已验证；其他 ROM 的差异仍需自行确认。
- Material3 1.4 的标准 `DatePicker` 没有独立的“月网格”，这里用的是标准日期网格 + 年份选择 + 输入模式。
- 海报用 Android Canvas 绘制，没有用 ComposeView 截图，避免把测量/绘制拉回主线程。
- 农历库固定 1.7.4，未升级到 Lint 提示的 1.7.7。
- Room 为明文数据库（应用私有目录 + 禁止备份），未引入 SQLCipher。
- “已出生 / 已相伴”由 `EventType.BIRTHDAY / ANNIVERSARY` 驱动，标题关键词自动识别；需要“已出生”时用生日/出生类标题（例如“示例事件”）。
