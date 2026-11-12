# SLIVER — architecture

## Project
Standard KMP layout (JetBrains wizard), one app module:
```
composeApp/
  src/commonMain/kotlin/app/sliver/
    core/        theme, components, navigation, util (dates, formatting)
    domain/      models, pure logic (auto-pick scoring, film plan, reminder rules, backfill matching, backup state machine)
    data/        Room DB, DAOs, DataStore settings, repositories, Drive REST client (Ktor)
    platform/    interfaces implemented per platform (see below)
    feature/     onboarding, camera, trim, gallery, photo, looks, archive, day, backfill, films, builder, export, player, backup, account, notifications, settings
  src/androidMain/kotlin/app/sliver/platform/   Android implementations
  src/iosMain/kotlin/app/sliver/               iOS actuals; media features are placeholders (see HANDOFF)
iosApp/                                        SwiftUI host for MainViewController (XcodeGen project.yml)
  src/commonTest/
```
minSdk 29, targetSdk latest stable. Use the newest stable versions of every library; put them in `gradle/libs.versions.toml`.

## Libraries
Common: Compose Multiplatform, Navigation Compose (multiplatform), Koin, kotlinx-coroutines, kotlinx-datetime, kotlinx-serialization, Room KMP + bundled SQLite driver, DataStore Preferences (KMP), Ktor client (OkHttp engine on Android), Coil 3 (thumbnails).
Android only: CameraX (camera-core, camera2, lifecycle, video, view), Media3 (exoplayer, transformer, effect, ui), WorkManager, Glance appwidget, Credential Manager + googleid, play-services-auth (AuthorizationClient for the Drive scope), androidx.core (notifications).

## Platform interfaces (commonMain → androidMain)
| Interface | Android implementation |
|---|---|
| `FileStore` — paths, write/delete, free space | app filesDir / cacheDir |
| `CameraController` — preview, record to tmp, flip, flash, look effect | CameraX VideoCapture + Recorder |
| `ClipExporter` — trim to 1 s, photo → 1 s, apply crop, export with look/date | Media3 Transformer |
| `FrameSampler` — luminance frames at N fps for auto-pick | MediaMetadataRetriever.getScaledFrameAtTime |
| `LookEffects` — build effect list from LookParams | Media3 effects (LUT, contrast, HSL) + small custom GL shader |
| `FilmRenderer` — render a FilmPlan to mp4 with progress | Media3 Transformer Composition in a WorkManager job |
| `MediaLibrary` — query gallery, taken dates, favorites, access level | MediaStore |
| `ReminderScheduler` / `Notifier` | AlarmManager + NotificationManager channels |
| `BackupScheduler` | WorkManager |
| `GoogleAuth` — sign in, get Drive access token, revoke | Credential Manager + AuthorizationClient |
| `Haptics`, `Share`, `SystemSettings` (open app settings) | Android APIs |

Wire with Koin: `platformModule` per source set.

## Testing
Unit tests in commonTest for every pure function in `domain/` and for repositories with an in-memory DB. Drive client with Ktor MockEngine. No iOS tests. No instrumented tests unless a task asks.

## Navigation
Start destination: Onboarding if not done, else Camera. Bottom bar on Archive and Films only (Camera is the center button).
