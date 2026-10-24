# MemorialDays

Offline, zero-permission Android app for tracking multiple anniversaries and countdowns. Kotlin + Jetpack Compose + Room + Glance, single module.

## Project

- Module: `:app` — package `com.example.memorialdays`, minSdk 26, targetSdk 36, compileSdk 36.
- Entry point: `MainActivity` -> `HomeScreen`.
- Widgets: `SingleEventWidget` (2x2) and `MultiEventWidget` (4x2), hosted by `BaseGlanceWidgetReceiver` subclasses.
- No network, no accounts, no third-party SDKs except the user-pinned `cn.6tail:lunar:1.7.4`, no runtime permissions.
- Room database v3 (`memorial_days.db`) with `MIGRATION_1_2` and `MIGRATION_2_3`.

## Commands

- `./gradlew assembleDebug` — debug APK at `app/build/outputs/apk/debug/app-debug.apk`.
- `./gradlew testDebugUnitTest` — JVM tests under `app/src/test`.
- `./gradlew lintDebug` — report at `app/build/reports/lint-results-debug.html`.
- `./gradlew assembleRelease` — R8/shrunk, unsigned release APK.
- `./gradlew installDebug` — install on a connected device via adb.
- Requires JDK 17+ and Android SDK platform 36 / build-tools 36.0.0. `local.properties` (untracked) points at the local SDK.

## Architecture

- `data/` — `EventEntity` (isYearly/eventType/isLunar/sortOrder), `EventDao`, `MemorialDatabase` v3, `EventRepository` (transactions/reorder), `BackupRepository` (SAF JSON).
- `domain/` — `DayMath` (`ChronoUnit`), `EventDateResolver` (lunar/solar recurrence), `EventType` (keyword detection), `EventSections`, `DisplayFormat`.
- `ui/` — `EventViewModel` (StateFlow, 300ms throttles, lunar work on `Dispatchers.Default`, elapsed/display day split), `HomeScreen` (single sortable list + SAF launchers), `HeroCard` (first item, 220dp), `EventCard` (compact 80dp), `DayNumber` (AnimatedContent), `EventEditorDialog`, `EventActionSheet`, `BackupSheet`, `SafeHaptics`, theme.
- `share/` — `PosterRenderer` (1080x1440 Canvas on `Dispatchers.IO`), `PosterData/PosterStyle`.
- `widget/` — `WidgetData` (resolves recurrences on IO), `SingleEventWidget`, `MultiEventWidget`, `BaseGlanceWidgetReceiver`, `WidgetUpdater`.
- `MemorialApplication` owns `AppContainer` (database, repositories, poster renderer, application-scoped widget updater).

## Conventions

- Never add permissions, INTERNET, accounts, Hilt, Retrofit, Lottie, animation/drag libraries, or WorkManager. Only `cn.6tail:lunar` is allowed as the third-party calendar engine.
- Day counts only through `java.time` / `ChronoUnit`; never millisecond division.
- Lunar conversion uses `Lunar.fromDate(Date)` and `Lunar.fromYmd(year, month, day).solar`; `LunarMonth.getDay(int)` does not exist in 1.7.4. Negative month = leap month; fall back to the ordinary month and clamp the day for New Year's Eve.
- Room only through suspend DAO calls or Flow; never query a database inside a composable. Lunar/poster work stays on `Default`/`IO`.
- All user-facing strings live in `res/values/strings.xml`; Kotlin sources contain no Chinese literals.
- Avoid `!!`; validate title (1..40), note (0..80), date (1900..2100); title may wrap to 2 lines with `TextOverflow.Clip`; other long text uses `maxLines`.
- Widgets must not use WorkManager, Services, AlarmManager or high-frequency `updatePeriodMillis`. The custom receiver uses `goAsync` + `GlanceAppWidget.update` and validates incoming appWidgetIds.
- The home list is a single sortable list. Index 0 renders `HeroCard`; all other indices render compact `EventCard`. Dragging an item to index 0 must expand it into the hero; dragging the hero down must collapse it. Do not reintroduce a separate date-selected hero.
- Cards are opaque: pure white in light mode, `#1E1E20` in dark mode. Never add `alpha` to the card surface and never add shadows. The root background is a vertical gradient with 20% of the theme accent.
- List card: about 80dp, first row status + single-line title, second row `公历 … · 农历…`, right-aligned large numeral + small 天.
- Hero card: about 220dp, centered status/title, numeral physically 30% of card width (divide by `LocalDensity.fontScale`), bottom centered status/date/next-occurrence copy.
- `EventType.BIRTHDAY` and `ANNIVERSARY` use elapsed days for `displayDays` (`ChronoUnit.DAYS.between(event.date, today)`) and show 已出生 / 已相伴; recurring ones also show 距下次生日/纪念日. Do not use the next-occurrence count as the large number for these types.
- Motion: `AnimatedContent` with direction-aware spring; all geometry/list motion uses `spring(dampingRatio = 0.75f, stiffness = 400f)`. The only `tween` is the explicit 300ms color transition in `Theme.kt`. Empty-state breathing uses `keyframes` + `infiniteRepeatable`.
- Haptics only through `SafeHaptics` (vibrator + SDK + 200ms throttle); never request `VIBRATE`.
- Posters: `cacheDir/posters`, `FileProvider` (`exported=false`, `grantUriPermissions=true`), clean after sharing; never request storage permission.
- Release builds keep `isMinifyEnabled` and `isShrinkResources`.

## Notes

- The Gradle wrapper points at the Tencent mirror for faster downloads in mainland China and pins the official SHA-256 checksum. Switch `distributionUrl` in `gradle/wrapper/gradle-wrapper.properties` back to `services.gradle.org` if preferred; keep the checksum.
- Glance's WorkManager transmission is excluded; do not replace `BaseGlanceWidgetReceiver` with `GlanceAppWidgetReceiver` without re-checking the merged manifest permissions.
- The Room database is plaintext by design; privacy relies on app-private storage and disabled backups.
- `cn.6tail:lunar` is pinned to 1.7.4 by request. Lint reports 1.7.7 as available; do not upgrade without an explicit instruction.
