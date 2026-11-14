# Perf baseline (before STEP 1) — 14 Nov 2026, commit 5c06e1a + LeakCanary/camera fixes

Kotlin 2.4.21, Compose Multiplatform 1.12.2, AGP 9.4.1. Strong skipping is the compiler default (featureFlags: StrongSkipping, IntrinsicRemember, OptimizeNonSkippingGroups, PausableComposition all on).

## Compose compiler (release, composeApp)

| metric | value |
|---|---|
| total composables | 683 |
| restartable | 652 |
| skippable | 278 (43 %) |
| known unstable arguments | 78 of 7061 |
| unknown-stable arguments | 257 |
| classes: stable / unstable / uncertain | 139 / 67 / 26 |

UI-relevant unstable classes: `data.db.Day`, `data.db.Film`, `data.db.Project`, `data.settings.AppSettings`, `ReminderSettings`, `feature.films.MixClip`, `feature.archive.Memory`, `feature.gallery.CaptureDate.*`, `DeepLinkTarget.Day`. The rest are services/workers/DAOs that never reach a composable.
Cause in almost every case: `kotlinx.datetime.LocalDate` / `kotlin.time.Instant` fields (types from another module the compiler can't infer).

Worst composables by unstable parameters (the per-function report covers 209 composables; the module totals above cover all 683):

| composable | unstable params |
|---|---|
| autoimport.DatePickerSheet | 3 (LocalDate ×3) |
| archive.DayCell | 3 (LocalDate ×2, Day) |
| archive.YearView | 2 |
| archive.MonthGrid | 2 |
| archive.MiniMonth | 2 |
| trim.TrimScreen | 1 |
| notifications.TimeWheelSheet | 1 (LocalTime) |
| notifications.PresetTile | 1 |
| films.rememberMusicLibrary | 1 (non-skippable) |
| camera.FocusOverlay | 1 |
| backup.Mosaic | 1 |
| autoimport.DateCell | 1 |
| archive.AddSheet | 1 |

Release APK (unsigned): 48.75 MB.
