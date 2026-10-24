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

- 顶部居中：状态小字 + 自定义标题（中等字号，最多两行）
- 中部居中：核心数字 + 小号“天”，数字物理尺寸约为卡片宽度的 30%，按 `BoxWithConstraints` 计算，并用 `fontScale` 修正，避免 2K 屏/大字体溢出
- 底部居中：状态描述、日期、距下次事件；所有元素用 Spacer 分层，保持留白
- 拖拽任意列表卡到第一位，它会在同一列表中变为 Hero Card；把 Hero 拖到后面会折叠回列表卡
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
./gradlew assembleRelease        # R8 + 资源压缩，未签名
./gradlew installDebug           # 已连接设备时安装
```

产物路径：

- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`（约 17 MB）
- Release APK（unsigned）：`app/build/outputs/apk/release/app-release-unsigned.apk`（约 1.8 MB）

Gradle wrapper 使用腾讯镜像并配置官方 SHA-256：
`distributionSha256Sum=20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78`。

## 数据模型与迁移

数据库版本 **3**。`EventEntity`：

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
```

`MIGRATION_2_3` 的具体 SQL：

```sql
ALTER TABLE events ADD COLUMN isYearly INTEGER NOT NULL DEFAULT 0;
ALTER TABLE events ADD COLUMN eventType TEXT NOT NULL DEFAULT 'GENERAL';
ALTER TABLE events ADD COLUMN isLunar INTEGER NOT NULL DEFAULT 0;
ALTER TABLE events ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0;
```

`MIGRATION_1_2` 仍保留（添加 `note`）。`addMigrations(MIGRATION_1_2, MIGRATION_2_3)` 显式注册，**没有** `fallbackToDestructiveMigration()`。

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

- 点击卡片：编辑
- 长按 300ms（通过 `LocalViewConfiguration` 覆盖 `longPressTimeoutMillis`）：浮起并触发触觉
- 拖动超过 `touchSlop`：拖拽排序；坐标交换触发 `SegmentTick`
- 拖到索引 0：该项自动展开为 Hero Card；Hero 拖到后面折叠为列表卡
- 原地松手：BottomSheet（编辑 / 分享海报 / 删除）
- 长按顶部 `MEMORIALDAYS`：备份 BottomSheet
- 保存 / 删除 / 备份：300ms 节流；拖拽排序松手立即批量写库
- 触觉：`Build.VERSION`、`View.isHapticFeedbackEnabled`、`Vibrator.hasVibrator()` 三重判断，200ms 节流，使用 `View.performHapticFeedback`（不需要 `VIBRATE` 权限）

## 备份与海报

- 备份：`ActivityResultContracts.CreateDocument` / `OpenDocument`（SAF），JSON 格式，无存储权限。导入会覆盖当前全部纪念日。
- 海报：`PosterRenderer` 在 `Dispatchers.IO` 用 Android Canvas 绘制 1080×1440 PNG。没有使用 ComposeView 截图，因为 ComposeView 的测量/绘制必须回到主线程，会违反“海报渲染不阻塞主线程”的硬约束。
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
adb install -r "<项目路径>/app/build/outputs/apk/debug/app-debug.apk"
```

或把 APK 传到手机点击安装。部分国产 ROM（如 ColorOS / MIUI）首次安装需：设置 → 应用 → 特殊应用权限 → 安装未知应用 → 允许文件管理器/浏览器。

小组件：长按桌面空白处 → 小组件 / 卡片 → MemorialDays → `下一个日子`（2×2）或 `纪念日列表`（4×2）。没有调用 `requestPinAppWidget`。

## 安全与轻量自查表

### A. 安全与隐私

| 编号 | 实现与证据 |
| --- | --- |
| A1 | Manifest 无任何 `<uses-permission>`；APK 实际权限仅 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`（AndroidX 自动生成的 signature 权限）。无 INTERNET、存储、通讯录、定位、VIBRATE。SAF 不申请存储权限；海报使用 `cacheDir` + FileProvider。 |
| A2 | `allowBackup=false`、`fullBackupContent=false`、`usesCleartextTraffic=false`；`data_extraction_rules.xml` 排除 cloud-backup / device-transfer。 |
| A3 | 无硬编码密钥；全部依赖固定版本；无 Hilt/Retrofit；`release { isMinifyEnabled = true; isShrinkResources = true }`，R8 构建成功。农历库为本地 Java 库，无网络访问。 |

### B. 性能、防卡死与耗电

| 编号 | 实现与证据 |
| --- | --- |
| B1 | `viewModelScope` / Application 级 `SupervisorJob` / `rememberCoroutineScope`；非 UI 层统一 `applicationContext`；`collectAsStateWithLifecycle`，无手动 `registerReceiver`；`LaunchedEffect` 随组合取消。 |
| B2 | 农历推算在 `withContext(Dispatchers.Default)`；Room suspend/Flow + `Dispatchers.IO`；海报在 `Dispatchers.IO`；Composable 内无数据库调用。 |
| B3 | 小组件 `updatePeriodMillis=86400000`；自定义 `AppWidgetProvider` + `goAsync`；无 Service、无 AlarmManager、无 WorkManager、无保活；天数只在状态构建或小组件更新时计算。 |

### C. 数据与日期正确性

| 编号 | 实现与证据 |
| --- | --- |
| C1 | 天数使用 `ChronoUnit.DAYS.between`；生日/相识事件的“已出生/已相伴”同样使用 `ChronoUnit.DAYS.between(event.date, today)`；无毫秒差；`PAST/TODAY/FUTURE` 明确区分。 |
| C2 | 数据库 v3 + `MIGRATION_2_3` 四条 ALTER；无 destructive migration。 |
| C3 | 无 `!!`；标题 1–40、备注 0–80、日期 1900–2100 双重校验；列表标题 `maxLines=2` + `TextOverflow.Clip`（按新规范不省略），其他可变文本 `maxLines` + Ellipsis/Clip；空/加载/错误状态完整；海报/备份异常均捕获；Bad JSON/坏日期跳过。 |

### D. 适配与字符集

| 编号 | 实现与证据 |
| --- | --- |
| D1 | 2×2：110dp + targetCell 2×2；4×2：250dp + targetCell 4×2；旧系统回退 minWidth/minHeight；标准 AppWidgetProvider；无 pin。 |
| D2 | `-Dfile.encoding=UTF-8`；全部中文在 `strings.xml`；Kotlin 源码无中文字符串。 |

### 额外安全措施

- Widget receiver 对传入的 `appWidgetIds` 与本应用实际拥有的 ID 取交集，拒绝第三方伪造广播。
- `FileProvider` 不导出，只授予临时读权限。
- 备份 JSON 导入时校验 format、标题、日期范围、备注长度；空数据抛 `EmptyBackupException` 并提示。
- 海报文件只存在 cacheDir，按时间清理，避免 OOM/长期残留。

## 验证结果

| 检查 | 结果 |
| --- | --- |
| `./gradlew assembleDebug` | BUILD SUCCESSFUL |
| `./gradlew testDebugUnitTest` | 16/16 通过，0 failures / 0 errors（DayMath 7 + 农历/生日示例 5 + 类型 4） |
| `./gradlew lintDebug` | 0 errors，2 warnings（lunar 1.7.7 可升级提示；mipmap-anydpi-v26 在 minSdk 26 下多余），1 hint |
| `./gradlew assembleRelease` | BUILD SUCCESSFUL；R8 + 资源压缩；约 1.8 MB |
| APK 权限 | 仅签名级 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` |
| Debug APK | 约 17 MB，`app/build/outputs/apk/debug/app-debug.apk` |
| 依赖树 | `cn.6tail:lunar:1.7.4`；无 `androidx.work` |

## 已知限制

- 当前环境没有 Android 真机/模拟器，未进行运行时交互与截图级视觉验证；拖拽与点击/滚动的手势冲突、BottomSheet、SAF 分享链路需要在真机（ColorOS / MIUI 等）上实测。
- Material3 1.4 的标准 `DatePicker` 没有独立的“月网格”；本次恢复了标准日期网格 + 年份选择 + `showModeToggle` 输入模式，并把顶部 headline 改为 `FontWeight.Light`。没有编造不存在的三级网格 API。
- 海报使用 Android Canvas 而非 Compose 截图；原因是 ComposeView 必须在主线程测量/绘制，与“海报渲染在 IO”冲突。视觉语言仍复用主题色、字体层级和状态文案。
- 农历库固定 1.7.4；Lint 提示 1.7.7 可升级，未偏离用户指定版本。
- Room 为明文数据库（应用私有目录 + 禁止备份），未引入 SQLCipher。
- Release APK 未签名，侧载请使用 debug APK。
- “已出生 / 已相伴”由 `EventType.BIRTHDAY / ANNIVERSARY` 驱动；标题关键词自动识别，若标题被识别为“相识”则显示“已相伴”，需要“已出生”时请使用生日/出生类标题（例如“示例事件”）。
