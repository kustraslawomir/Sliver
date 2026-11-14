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

## Runtime (debug build of bfc8d6e, emulator tracker13 sdk_gphone64_arm64, 14 Nov)

The Xiaomi was not connected; every number below is from the emulator, which renders in software (most frames count as "janky" there whatever the app does). Use them to compare before/after on the same emulator, not as phone numbers.

| measure | value |
|---|---|
| cold start (am start -W, TotalTime) | 1835 / 1827 / 1911 ms (avg 1858) |
| idle CPU, project screen (top -d 2 -n 5) | 0.0 % ×5 |

Recompositions = debug RecomposeProbe at the top of each composable (counts whole-function recompositions; inner lambda scopes that read state are not counted). Frames = dumpsys gfxinfo after reset.

| flow | recompositions | frames · janky · p90 · p99 |
|---|---|---|
| hub_idle_10s | 0 | frames: 0 janky: 0 (0.00%) 90th: 4950ms 99th: 4950ms |
| hub_scroll_calendar | 0 | frames: 106 janky: 105 (99.06%) 90th: 150ms 99th: 150ms |
| hub_scroll_films | 0 | frames: 0 janky: 0 (0.00%) 90th: 4950ms 99th: 4950ms |
| day_playing_6s | 0 | frames: 0 janky: 0 (0.00%) 90th: 4950ms 99th: 4950ms |
| camera_idle_10s | 0 | frames: 146 janky: 141 (96.58%) 90th: 85ms 99th: 150ms |
| camera_recording_3s | 0 | frames: 151 janky: 148 (98.01%) 90th: 129ms 99th: 200ms |
| builder_playing_6s | 0 | frames: 11 janky: 11 (100.00%) 90th: 200ms 99th: 200ms |
| builder_scrubbing | 0 | frames: 44 janky: 42 (95.45%) 90th: 150ms 99th: 200ms |
| builder_pick_look | 0 | frames: 112 janky: 109 (97.32%) 90th: 350ms 99th: 450ms |
| sound_sheet_open_5s | 0 | frames: 5 janky: 5 (100.00%) 90th: 150ms 99th: 150ms |
| player_playing_6s | 0 | frames: 8 janky: 6 (75.00%) 90th: 300ms 99th: 300ms |


Notes:
- On the emulator the builder preview and the player do not advance (the video stays on one frame), so "playing" flows show almost no frames; they need the Xiaomi.
- Camera frames are the camera preview (PreviewView COMPATIBLE = TextureView, so each camera frame is an app frame), not UI work.
- Hub idle: 0 recompositions and 0 frames in 10 s; hub scroll: the hub, month and cells do not recompose.
