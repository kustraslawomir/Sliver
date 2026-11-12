# HANDOFF

## Needs from you (top)
- Project screen delete confirmation: design not supplied. The existing F04 dialog (sheet) is unchanged; please provide a board.
- Archive/Delete in the Project menu are disabled when it is the only open project (existing rule); the board shows them enabled.
1. **Geist fonts are missing.** Add OFL files to `composeApp/src/androidMain/res/font/`: `geist_regular.ttf`, `geist_medium.ttf`, `geist_semibold.ttf`, `geist_mono_medium.ttf`. They are wired by resource name in `core/theme/GeistFonts.kt`; until then the system sans/mono fallback is used. Instrument Serif is removed.
2. Confirm on the phone: the visual brightness change from the exposure slider and real AF lock (logs show the CameraX calls; only the look of the preview needs eyes).
3. Light / "Replace clip for today" choice: Replace for today still opens Camera; other days open the Photo Picker.

## v1.2.2 design QA
Legend: ok = matches, no = does not match / not done.
- ok Titles Geist 600 with tokens tracking (52/36/24); fallback font until files exist (see above)
- no Geist actually rendering: font files absent
- ok `core/designsystem/Glow.kt` (shadow-layer modifier + tokens); glow on: primary CTA, trim frame, splash/onboarding slit, slit loader, record button, focus lock ring, date stamp text
- no glow.dot on progress heads / film markers (progress bars have no head dot in the app); photo-mode white glow only verified in code
- ok Camera: 150 dp top scrim, 36 dp chips (black 55%, 70% below API 31), "Today" chip, project chip with no format text
- no Chip blur: Compose has no backdrop blur, so the 12 dp blur is not rendered (plain plate only)
- ok Tap-to-focus: real CameraX metering AF+AE+AWB (auto-cancel 3 s, lock = disableAutoCancel), ring 88 to 64 dp / 220 ms, dim 35% after 1.2 s or on failure, lock pill and glow.lock, disabled while recording; logs (tag SliverFocus) verified on the Samsung: startFocusAndMetering called, supported=true. A tap with no follow-up stays pending until AF settles or 3 s; a superseded request reports "Cancelled by another startFocusAndMetering" (expected)
- ok Exposure slider: 120 dp, side with more room, sun icon, EV label while dragging, visible 3 s (stays while locked), resets on lens/mode/torch rebind; `setExposureCompensationIndex` verified applied (index 13 on the Samsung, EV +1.3)
- ok Record button D: 72 dp disc / 80 dp hit, radial-gradient glow (no blur), stop square 24 dp, breathing 1.6 s ease-in-out (cubic-bezier .42,0,.58,1) between 14px 0x88 + 32px 0x33 and 20px 0xAA + 44px 0x44 (design `sv-breathe`); static under reduced motion; 3 frames in docs/qa/brand/record-glow-breathing.png
- ok Timer pill grey until 1 s then ember + "CLIP OK" + one CONFIRM haptic; ADD/flip/mode switch hidden while recording
- ok Save: lands on Project (edit screen popped), cell pops in 0.6 to 1 / 420 ms with 2 dp ember ring, Back goes to Camera, no toast/snackbar anywhere (verified on the Samsung via UI tree)
- ok Copy: "Save clip", "Your year in clips", "in clips", no "second/Looks" in UI
- ok Buttons: one line, 48 dp high, chips one line; checked camera at font scale 1.3 (font scale restored to 1.0). Not checked at 1.3: other screens
- no Landscape: Activity is locked to portrait, so no landscape shots
- ok ADD badge no longer clipped (drawn in an unclipped wrapper), verified at 1.0 and 1.3
- ok App icon, splash theme + installSplashScreen, `ic_stat_sliver` on all notifications (code)
- no Not captured: home screen with themed icons on/off and a notification (would show the user's launcher/shade); splash only captured masked
- no Design boards are template-driven HTML and render with unresolved placeholders headless; design PNGs in design/png are partial, so comparison is by measuring the HTML

## User decisions applied in this run
- Timer, Filter strip and countdown removed from Camera; Grid moved into a 3-dots menu (switch, persisted); Light is a plain icon (zap / zap-off, 44 dp, Role.Switch). The design board still shows Timer/Filter/tools row: the decision wins.
- **Filters dropped for now, restore from git history before commit a78807d.** All entry points and the editor route/screen removed. Stored `look` columns untouched; `LookEffects.build` returns no effects (PARKED flag), so playback/export are unfiltered. Dormant code kept: LUT generator, LookEffects, LookRenderer, LooksLogic/LookSession, settings `defaultLook`.
- Gallery screen removed; ADD, "Choose from Photos" and Replace open the system Photo Picker directly (`feature/gallery/AddMedia.kt`).
- Privacy: no camera preview or real media saved under docs/qa; camera shots are masked. A test clip was recorded and saved in the app on the Samsung (app data only).


Final verifier run (T01–T37): `assembleDebug`, `testDebugUnitTest` (151 tests) and `lintDebug` all pass. Nothing was run on a device or emulator.

## Status
✅ Done (code + unit tests): T01–T37 (37 tasks).
⛔ Blocked: none. Open gaps are listed below.

| Task | Gap |
|---|---|
| T16 | Camera live preview does not apply the look (no stable CameraX→Media3 bridge). Playback and export do. |
| T23 | The 4 built-in music tracks (drift, pulse, dusk, spark) are missing. Add `.aac` files to `composeApp/src/androidMain/assets/music/<id>.aac`. A missing track is skipped at render and the film keeps its original sound. |
| T15 | LUTs are generated by Kotlin `LutGenerator` instead of `tools/gen_luts.py` (python was not runnable). Regenerate with `./gradlew :composeApp:testDebugUnitTest --tests '*LutFilesTest*' -PgenLuts=1`. |
| T31 | Widget is `AppWidgetProvider`/RemoteViews, not Glance. |
| T25/T26 | "Edit film" opens the builder for a new film; editing an existing film isn't supported. |
| T02 | Fonts (Instrument Serif, Geist, Geist Mono) are system placeholders. Add the OFL .ttf files to `composeApp/src/commonMain/composeResources/font/` and build the FontFamilies in `core/theme/SliverType.kt`. |
| T30 | Splash draws a horizontal bar, SPEC asks for a vertical slit. |
| T35 | Disconnect button is labelled "Sign out". |
| T37 | Only 2 of the 4 delete-account option combinations are unit-tested. |
| docs | `docs/UI.md` (named in CLAUDE.md) doesn't exist; `docs/DESIGN.md` and `docs/tokens.json` were used. |

Device-pending checks (need an emulator or phone): cold start under 2 s, 1.000 s clip duration (`AndroidClipExporter.measureDurationMs`), 15 s auto-pick under 300 ms, film render with each audio option, share/save flows, reminders and boot reschedule, widget and Quick Settings tile, TalkBack and 200% font scale, backup states.

## Requirement coverage
`docs/SPEC.md` has no R-IDs, so section headings are used.

| SPEC section | Tasks | Status |
|---|---|---|
| Core rules | T05, T07, T08, T12, T14, T18, T19, T20, T23 | Met (device pending) |
| Launch | T30 | Met (device pending) |
| Onboarding | T06, T27, T28, T33 | Met (device pending) |
| Camera | T07, T13, T14, T16 | Gap: look not applied in preview |
| Trim & Pick | T08, T09, T10, T14, T16 | Met (device pending) |
| Gallery picker | T11 | Met |
| Photo → 1 s | T12 | Met (device pending) |
| Looks editor | T15, T16, T17 | Met (device pending) |
| Archive | T18, T21 | Met (device pending) |
| Day detail | T19, T20 | Met (device pending) |
| Fill the past | T21 | Met (device pending) |
| Films | T22–T26 | Gap: built-in music missing; edit existing film |
| Backup & account | T33–T37 | Met against fake Drive (device and real account pending) |
| Notifications | T28 | Met (device pending) |
| Settings | T29 | Met |
| Widgets | T31 | Met (device pending), RemoteViews not Glance |
| Edge states | T30 | Met (device pending) |

## Needs from you
### Google sign-in and Drive backup
Without `GOOGLE_WEB_CLIENT_ID` the app uses `FakeGoogleAuth` and an in-memory Drive. Nothing leaves the device, and Restore finds nothing on a fresh install.
1. Google Cloud Console: create/select a project, enable the Google Drive API.
2. OAuth consent screen: External, add scope `.../auth/drive.file`, add yourself as a test user.
3. Credentials → OAuth client ID → Android: package `app.sliver`, SHA-1 of your debug keystore (`keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`). Repeat for the release keystore.
4. Credentials → OAuth client ID → Web application. Copy its client ID.
5. Add `GOOGLE_WEB_CLIENT_ID=<web client id>` to `local.properties` (and `.env.local`), rebuild.

### Other
- Music tracks and fonts (see table above).
- Feedback address `feedback@sliver.app` in Settings is a placeholder.
- No release keystore: debug signing only.

## Assumptions / decisions
- AGP 9.5.1 with `android.builtInKotlin=false` and `android.newDsl=false` to keep a single `composeApp` module; these opt-outs go away in AGP 10.
- DB tests live in `androidUnitTest` with Robolectric pinned to SDK 36 (targetSdk is 37).
- No Material3; token-based theme on Compose foundation.
- Place chip: `Day.place` via `PlaceResolver` (Geocoder). No retry when offline, and `ACCESS_MEDIA_LOCATION` is not requested, so gallery sources may have no place.
- Day files are exported with the look baked in. Editing the look in Day detail saves `Day.look` and previews in-session only.
- Fill the past runs in WorkManager with no foreground notification. Undo all deletes only BACKFILL days in the batch.
- Film render has no chunked resume; "Try again" restarts. The film's look is applied on top of each day's baked look.
- On restore conflict a local day wins.
- Delete account moves the Drive folder to trash (recoverable).
- iOS shell (prepared, not feature-complete): `iosApp/` (XcodeGen `project.yml` + generated `iosApp.xcodeproj`; app icon is the 512 px Play icon upscaled, replace with a 1024 px master) embeds the `ComposeApp` framework and launches the shared UI. Room (bundled SQLite), DataStore, file store, Ktor Darwin and permissions are real; camera, gallery picker, players, clip export, film render, backup/restore scheduling, reminders, share and zip export are placeholders (black boxes / no-ops / fake auth). Build: `cd iosApp && xcodebuild -scheme iosApp -sdk iphonesimulator build`, or open the project in Xcode. Regenerate the project after editing `project.yml`: `cd iosApp && xcodegen generate`.

## How to run
```
./gradlew installDebug          # needs a device/emulator
./gradlew :composeApp:assembleDebug :composeApp:testDebugUnitTest :composeApp:lintDebug
```
Emulator: create an API 34+ AVD (Android Studio Device Manager or `avdmanager`), start it, then `./gradlew installDebug`. Use a virtual camera scene for the camera and add a few videos/photos to the gallery for Trim, Fill the past and Archive.

## Device test (Xiaomi M2101K9G, Android 13)
Walked through onboarding → camera record → trim → save → archive → day detail → looks → film builder → render → player → settings. No crashes in logcat.
- Fixed: camera had no way to reach Archive/Films/Settings (added a DAYS button, bottom-right); floating tab bar overlapped the 3-button system nav (now inset).
- Added: four synthesized built-in music tracks (replace with real ones any time, same file names).
- Open: saved day clip is 1.033 s (31 frames at 30 fps), spec says 1.000 s. Settings gear on Archive renders as the letter "S" (no icon). Camera screen has no looks strip. Fonts still system placeholders.
- Not tested on device: Google sign-in, Drive backup/restore, reminders firing, widget and tile, TalkBack.

## F01 fonts
Geist / Geist Mono / Instrument Serif OFL files could not be downloaded (curl/wget denied) and none exist in resources. `SliverFonts` in core/theme/SliverType.kt still maps to system families; to finish, add the TTFs to composeResources/font and set the three FontFamily vals via Font(Res.font.*).

## F03 gap: backup manifest and restore are not per project
BackupEngine/RestoreEngine still treat days as one flat list: the Drive manifest does not list projects, files are not under `SLIVER/<project name>/`, and restore does not recreate projects (restored days land in the active project). Needs a manifest v2 with projects, per-project folders, and a restore that creates projects before attaching days/films. Deleting a project queues its Drive files for trash via the existing queue.

## F04 / F05 decisions
- Project start date is fixed to today (shown as "Starts today"); no date picker.
- Changing a project's format only affects clips saved afterwards; existing clip files keep their format.
- The last open project cannot be archived or deleted (camera needs a target). Tapping an archived project reopens it.
- `camera.activeFilmId` setting is now unused (camera follows the active project); left in storage to avoid a settings migration.
- Navigation (F05) is not verified by hand on a device. Origin of a capture flow is derived from the back stack (first screen below Camera/Gallery/Edit clip/Filters), not stored.
- SavedStateHandle restores the Edit clip source path, day and trim position; the applied Filters look is not restored after process death.

## F16 status (v1 device-test gaps)
- 1.000 s clip: fixed in F09/F10 (AAC frames past the second dropped, test added). Unverified on device.
- Settings gear: SliverIcons in place (F01); no text-as-icon strings left (only "+ Make film" label cleaned).
- Splash: now a vertical slit growing top to bottom with the wordmark below.
- Edit film: opens the builder prefilled with the existing film and re-renders under the same id.
- Live camera preview does not apply the selected filter (CameraX preview has no per-frame shader); the filter is baked when the clip is saved and shown in Edit clip / Filters. Needs an OpenGL/CameraEffect pipeline, not done.
- Fonts still system families (see F01 fonts above).

## v1.1 QA
Code-level pass (no device). Walked every NAVIGATION.md row against SliverNavHost/AppNavigator/screens: duplicate taps are de-duplicated in AppNavigator (single-flight + launchSingleTop); flow-origin pop, discard sheet (Edit clip/Photo clip BackHandler), render-in-background prompt (Player BackHandler), deep-link stacks are all present. Not verified by running.
Fixed: leftover "second" copy in empty state, permission pre-prompt and Year in Seconds subtitle; "+ Make film" text glyph; ember fills on onboarding progress/slit (now neutral).
No FontFamily.Default / MaterialTheme.typography in screens; fonts are still system families via SliverFonts.
Bigger, not fixed:
- Live camera preview ignores the selected filter.
- Geist / Geist Mono / Instrument Serif TTFs missing.
- Backup/restore not per project (F03 gap).
- Applied Filters look is lost after process death.
- Photos permission pre-prompt lives in the media picker gate (first open), reminders permission only via the Project card; Settings reminder toggle path not re-checked.
- Needs device: rotation, both lenses, 2 and 5 minute videos, gallery photo, process death.

## v1.2
- U01: the system Photo Picker replaced the custom gallery picker (Add media, Fill empty days); READ_MEDIA_* permissions are gone. The camera's latest-photo thumbnail went with them (the ADD button is a plain tile). Picker URIs are temporary grants: if the process dies mid "Fill empty days", the persisted batch may hold URIs that no longer resolve and those days are skipped as failed. Cheap fix not done yet: copy each picked file into app storage when the batch starts (`BackfillStore.start`).
- U02/U03: window size classes (side-by-side camera/edit clip, two-pane project, 600 dp content cap), predictive back for sheets and edit clip, edge-to-edge insets.
- U04: after name+format and the camera permission, Camera opens with a one-time coach ("Tap to record. Any length - you'll pick the best part next."), cleared on first recording (`coachRecordSeen`). `FirstClipTimer` logs `FirstClip` (debug tag) from the first onboarding screen to the first saved clip. Not timed on a device by hand (emulator cold start is slow).
- U05: `ProgressNotifier` (androidMain): ProgressStyle on API 36+ (segment per month for film render, ember done / gray pending, point at current, slit tracker icon, promoted-ongoing request on 36.1+), plain determinate notification below, posts at most once a second. Film render uses it as its foreground notification; `ProgressCoordinator` mirrors backup, restore and Fill empty days. Taps open Film / Backup / Fill empty days. Backup/restore have no separate "ready" notification (film keeps its existing one). Only verified on API 35 (legacy path).
- U06: record button morphs circle <-> rounded square with springs; saved clip drops into the TODAY chip (Camera) or its calendar cell (Project); `SlitLoader` replaces text-only loading states (Edit clip prepare, gallery busy, restore scan; film render keeps its determinate bar). Reduced motion was never wired before: `rememberReducedMotion()` now reads animator scale 0 and feeds `SliverTheme`. Drop animation not captured on the emulator (virtual camera recording did not reach Edit clip there).
- U07: static shortcuts (Record today, Add from gallery, Make a film), adaptive launcher icon with themed (monochrome) slit layer, widget: slit tap = Camera, rest = Project, optional "Volume key records" (Settings > Capture, off by default; only acts while Camera is open).
- Fixed on the way: cold-start deep links (notifications, widget, shortcuts) crashed with "setGraph() before getGraph()"; links now wait for the NavHost and are dropped before onboarding is done.
- U08: Motion Photos picked in Add media become video clips: `MotionPhoto` (commonMain) parses XMP (Camera:MotionPhoto=1, Item:Semantic=MotionPhoto/Item:Length) for JPEG (trailing bytes) and the `mpvd` box for HEIC/AVIF, verifies an `ftyp` header, ignores MicroVideo; the video goes to tmp and Edit clip opens centered on the presentation timestamp (else midpoint). No video -> Photo clip. Tests use synthetic fixtures; not tried with real Pixel/Samsung files (no device samples). Fill empty days still treats motion photos as photos.
- U09: touch targets 48 dp (icon buttons, ghost buttons, chips via a 48 dp hit area around the 36 dp pill, segmented, slider); buttons grow with font scale (min heights, wrapping text); selected chips show a check icon; today's cell has ring + dot and "today" in its description; backup status card uses an icon per state (check/cloud/pause/x) instead of a color dot; calendar cells with a clip expose TalkBack actions Open / Replace clip / Remove clip (remove is undoable); `ContrastTest` asserts every text color is >= 4.5:1 on Base/Surface1/Surface2 (all pass, ember included, so no change to accent text). The trim strip was already an adjustable slider with "Starts at x.y seconds". NOT done: Accessibility Scanner run and a manual TalkBack pass (no scanner/TalkBack session available in this environment); 200% font scale only reasoned from layouts, spot-checked in the final emulator pass.
- U10: verified on emulator-5554 with ffprobe on the saved day file (3 s 30 fps test source, window 1.0-2.0 s): the Transformer output had only 27 video frames (0.900 s) while audio ran 1.02 s, i.e. NOT a frame-exact second (the old MediaMetadataRetriever duration check hid it). Fixed: the exporter asks the Transformer for 100 ms extra source when the source has room, then the remux cuts to exactly 1.000 s (video samples with pts < 1 s, AAC frames that end by 1 s). Re-checked: video 30 frames / 1.000000 s, container 1.000000 s, audio 0.975 s (starts 0.022). First frame = chosen start (source frame 1.000 s checked by reading the burned-in timecode). Sources above 31 fps get a frame-drop to 30 fps; a debug-only log warns on variable frame rate (`isVariableFrameRate`, unit-tested). Not done: instrumented test harness (no androidTest source set in this AGP/KMP setup; verification was by hand on the emulator) and the 365-clip film drift test (not run; per-clip files are now exactly 1.000 s so concatenation drift should be 0, unverified). Emulator caveat: the goldfish H.264 encoder may be what dropped the frames; real devices untested.
- U11: `ThumbCache` (memory LRU by bitmap bytes) shared by calendar/day thumbnails and the Edit clip strip (disk side is the existing 320 px JPEG day thumbnails; strip frames are not cached on disk). Edit clip no longer waits for the auto-pick scoring: the clip is playable at 0 s right away and the pick applies only if the user has not moved (on the emulator scoring a 3 s clip took 10-210 s, the screen was stuck on "Preparing" that long; now usable in ~12 s incl. cold decode). Hand-written `baseline-prof.txt` for launch -> Camera -> Edit clip -> Save (only used by release builds; not verified in an APK). NOT done: Macrobenchmark module and measured numbers (emulator is not a valid benchmark target); 1 s camera cold start and 500 ms first frame are unmeasured. Emulator cold start after install is 10+ s.
- U12: Remove clip (Day detail) no longer asks first; it removes and shows an Undo toast for 6 s (toast host default raised to 6 s, wraps to 3 lines, grows with font scale). Replace clip already parks the old clip for the 6 s undo window (SaveUndo); picked gallery items are never written until saved, so nothing to confirm. Kept confirmations: delete project, delete account, discard in-app recording. "Remove film" has no UI in the app today, so nothing to change.
- Emulator pass (fresh install, onboarding, camera permission, Edit clip -> Save clip -> Camera with TODAY chip checked): found and fixed (1) Save clip did nothing for videos: `useSecond` read a stale captured `duration`, so it returned early silently; (2) failed save and failed recording were silent: now a toast ("Couldn't save the clip. Try again." / "That recording didn't work. Try again."); (3) TODAY chip showed two check marks after U09; (4) Edit clip hung on "Preparing" while scoring (see U11). Adb-driven recording could not be used: the emulator virtual camera finalizes with ERROR_NO_VALID_DATA (now reported by the toast), so the clip was added through the Photo Picker with an ffmpeg-made 3 s video instead; photo path also saved and updated the chip. Not retested after fixes: record 3 s with real camera, 200% font scale, TalkBack.

## Hardening (H01)

### 1. Auto-pick speed
Clips over 10 s: coarse pass (2 fps, stretched so it never exceeds 150 frames), then a 10 fps refine of +-1 s around the winner; 8 s timeout falls back to the middle second (`SamplingPlan`, unit-tested). Measured on the Samsung (debug build, SliverDemo clips concatenated to 156 s): 156 s clip scored in 5.5 s (~30 ms/frame); a 10 s clip scored densely (100 frames) takes 3.9 s. Old behaviour on a 30 s clip was 300 frames (~9-11 s at that per-frame cost); now ~90 frames (~3 s).

### 2. Save path
Exporter: one `render()` (45 s timeout, trim, thumbnail, duration check <= 1080 ms, deletes partial video/.tmp/thumb on any failure, `.hold.jpg` always cleaned); `trimToSecond` throws and checks `renameTo`. Timeout/failure lands in the existing "Couldn't save. Try again" state. `SaveUndo.remove` always deletes the day row (restores files if the row delete throws), shows a notice sheet on failure; parked leftovers are reclaimed by the launch-time `cleanTmp`. `println` replaced by `logDebug` (now debug-build only). Not verified on device (needs the picker with real media); unit tests/lint green.

### 3. Backup + migration
Manifest v2 adds `projectId` and `project {name, format, startDate}`. Drive layout is now `SLIVER/<project name>/` (clips + manifest.json); the Drive scope is the active project (as before: backup only ever covered the active project's days; other projects back up when they are active). Restore scans the legacy flat root (old manifests, no projectId -> active project) plus every subfolder with a manifest, recreates missing projects under their original id without activating them, restores films into their project, and activates the restored project when the phone had no clips. Known limits: two projects with the same name share a folder; renaming a project starts a new folder (old clips stay put, their Drive ids are kept); a subfolder without manifest.json is skipped. `Migration2To3` only inserts the default project when no project row exists (tests: days + empty project table, days + existing project).

### 4. Picker access + errors
Single add already copied the pick into tmp at pick time; Fill empty days now copies each chosen item into `files/backfill/` (not swept by tmp cleanup) before the batch starts, so the batch no longer depends on temporary picker URI grants and survives process death; the executor consumes and deletes the copy. Motion Photo detection reads only the first 256 KB and loads the whole file only when it declares a Motion Photo. New app-wide `Notices` + `NoticeHost` sheet: "Could not read N items", "Couldn't read the selected items", and a failed single add now tell the user instead of doing nothing. Backfill failures are logged (debug). Not verified on device (needs the picker on real media). Disk cost: a Fill batch temporarily holds a copy of each chosen clip.

### 5. Hygiene
Log.d in CameraEngine/FrameSampler now goes through `logDebug`, which only logs in debug builds. Removed `SCHEDULE_EXACT_ALARM`: reminders use `setWindow` (10 min window starting at the planned time, never early). `POST_PROMOTED_NOTIFICATIONS` kept: `ProgressNotifier` requests promoted ongoing notifications, which needs it. Added `dataExtractionRules` (everything excluded from cloud backup and device transfer; `allowBackup` was already false). Internal rename `second` -> `clip` done (SaveClipRequest, exportClip, photoToClip, saveClip, bestClip, pickBestClipMs, YearInClips route `year-in-clips`, plan.clips); `SECOND_MS`/`ShortMode` stay because they are real 1 s durations. No unreachable routes or settings keys were found (every Route is navigated to and every preference key is read). One `AppScope` (SupervisorJob + exception handler) replaces the ad-hoc scopes in SliverApp and SaveUndo.

### Not done (needs owner)
R8 + release signing (keystore), real fonts/music. Save-path and picker changes (items 2 and 4) were built, unit-tested and lint-checked, and the app was installed and launched on the Samsung, but a full save/Fill run was not exercised because it needs the picker on real media.

## v1.2.3 design QA
Device: Samsung (1080x2340 @420dpi). Shots in docs/qa/project/ (clip cells masked), glow crop docs/qa/glow/project-add-cta.png, fonts docs/qa/fonts-geist-masked.png.
- [x] Fonts: Geist/Geist Mono via compose resources, no FontFamily.Default/SansSerif left, text visibly Geist on device. Mono is also used at Regular (400) on Project boards.
- [x] Camera: only Light + menu(Grid); FLIP label under 52dp #FFFFFF1A circle; chip plate #000 70% always.
- [x] Share image: R.drawable.sliver_wordmark drawn with setBounds/draw. [ ] not visually checked on device (needs a share run); wordmark size 140px@1080 is my choice, the board gives no size.
- [x] Filters parked: removed "Favourite filter" stat and "or filter" button text on Year screen; no other entries found.
- [x] Landscape: docs/qa/landscape.md + scripts/check-landscape.sh; awaiting owner recordings.
- Project screen (month, year+nudge, menu, add sheet): header, segmented control, nudge card, grid cells (58dp, 4/5 gaps, empty/today/future/before-start), year rows, memory card, films posters 132x198, menu 230dp, add sheet with ember CTA glow all built from board values.
  - [x] Month/year/nudge/menu/add sheet match the PNGs within about 4 dp on header spacing (anchored to status bar inset, not 844dp absolute).
  - [x] Add sheet: system Photo Picker only (no READ_MEDIA).
  - [ ] Not verified on device: today-without-clip ember ring, memory card, thumbnails (today had a real clip, masked), film poster row (only default film), disabled Archive/Delete.
  - [ ] Sheet container is the shared SliverBottomSheet: border is hairline all round, board has a #3A3A40 top border only.
  - [ ] Dropped from Project screen vs v1.1: backfilled-cell badge, reminder-permission card variants, "Your year in clips" teaser card (not on boards; Year-in-clips route is now unreachable from here).
  - Camera calendar chip now always opens Project (board: opened from the chip); Day detail is reached from the grid.
  - No new tables; one new DataStore flag nudgeDismissed.

## Fix round: camera chip, menu back, projects meta, year entry
- Camera project chip sizes to content (max 220dp, one line, ellipsis); no longer shares weight with a spacer.
- Project screen ⋯ menu: BackHandler closes only the menu (scrim tap already did). Sheets already use PredictiveBackHandler (Overlays.kt).
- Projects sheet meta line may wrap to 2 lines (no truncation).
- "Year in clips" added as a plain item in the Project ⋯ menu (no board shows an entry; DECISION).
- Archive/Delete stay disabled with one project (rule kept); one-line reason "Keep at least one open project." shown under them (the board shows them enabled; DECISION: rule wins, reason added).
- Verified on Samsung (no screenshots taken): chip text "My 2026", menu lists Year in clips, Back closes menu only.

## Fix round: overlay z-order + preview rotation
- Camera: ProjectsSheet/menu sheet were declared before the controls Column, so controls drew on top. Moved overlays to the end of the root Box. Other screens (Project, Day, Trim, Backup, Export) already declare sheets last; sheets already pad for nav bar via safeDrawing.
- Verified on Samsung with uiautomator: with the Projects sheet open only sheet nodes exist, PHOTO/VIDEO/FLIP/ADD are not exposed (blocked). No screenshots stored.
- Preview: explicit Preview targetRotation = display rotation, PreviewView COMPATIBLE (TextureView) mode. Root cause of the sideways viewfinder was not reproduced (phone was face-down, dark scene); DECISION: made rotation explicit. Capture rotation unchanged (set at record/photo time from device orientation). Owner to confirm upright preview.

## v1.3
Part A (done earlier)
- [x] 1 Home first: Project screen is root, no back arrow, fixed 72dp Record today / outlined check Done for today; deep links still open camera; docs/NAVIGATION.md updated.
- [x] 2 Camera is a full-screen modal: X closes, Light + Grid menu only, no project/Today chips; save returns to home with the cell pop.
- [x] 3 Photo/Video plain words with 4dp dot. [x] 4 Add and Flip identical 52dp controls, Add is an icon.
- [x] 5 Single-layer record glow per tokens. [x] 6 Month/Year text tabs with 2dp underline.

Part B
- [x] 7 No Geist Mono or uppercase labels: SliverType.MonoS/MonoL replaced by Meta/Num (Geist, tnum); labels sentence-cased (Clip OK, Archived, month/weekday names, meta lines). Mono + uppercase kept only as SliverType.Stamp for the burned-in date stamp and the in-app stamp previews. The exported end card text "A FILM BY YOU" (AndroidFilmRender, burned into video) was left unchanged.
- [x] 8 includeFontPadding=false + LineHeightStyle(Center, Trim.Both) set once in SliverType.Base; no per-component compensation existed to remove.
- [x] 9 "Default film format" is a normal row that expands to a choice (Cinema 16:9 / Story 9:16); Settings > Appearance > Theme (System/Light/Dark) persisted in DataStore (key appearance).
- [x] 10 SliverColors has dark (themeDarkMap) and light (themeLight) palettes; AccentText = emberOnLight in light; Glow returns no layers in light; status/nav bar icons follow the theme (SystemBarsEffect). Hard-coded Colors moved to tokens (CellEmpty, CellEmptyLine, PosterTints, Media/OnMedia). Intentionally left: Color(day.color) data colors, SliverIcons mask Black, Android export/notification colors.
- [x] 11 Build, unit tests and lint green; settings round-trip test covers appearance.

DECISIONS
- Dark-in-both-themes is per route (Camera, Trim, PhotoTrim, Export, Premiere, FilmPlayer, YearInClips), set from the nav back stack. So the whole Edit clip screen stays dark, not only its preview area (board EditClipV2L has a light chrome around the dark preview). During a route transition the outgoing screen briefly takes the new palette.
- Surface2/Surface3 map to themeLight surface2 / line; Success/Warning in light are my darker picks (tokens give none).
- Choice rows expand inline instead of a sheet.

Not verified on device: the phone was not unlocked, so no on-device or screenshot check of the light theme, centering of chips/buttons, Settings Appearance row or status bar icon colors. Light boards (ProjectHubL, DayDetailL, ProjectsV2L, SettingsL, EditClipV2L) were not compared pixel by pixel.

## Camera focus/EV UI (One UI style)
- Text over the live preview gets a dark shadow (overMedia) and the EV value sits in a 60% black pill (mediaPlate); plateContrast + test checks >= 4.5:1 on pure white. Hints and AE/AF lock pill already had solid plates.
- Focus ring 84 -> 70 dp, 1.5 dp, no centre dot; EV slider (1.5 dp track, 32 dp sun thumb) beside ring with EV pill beside the thumb, TextHandleMove haptic per step; ring and slider fade together after 3 s (ring dims at 1.2 s). Ember only when locked.
- Not verified on device (phone not unlocked / no on-device check possible).
- Clip detail (also Project two-pane) starts muted; toggle is per visit. Film builder/player audio left as chosen by the user (film audio mode / explicit Mute button).
- Date stamp is bottom-right (16 dp, white Geist Mono, soft shadow; export scales by short side/360, see StampLayout.kt) on Clip detail, Edit/Photo clip, film builder preview, film player and film render. Owner overrides design/src ember top/bottom placement. Settings has no stamp preview (switch only). Stamp text formats still differ (screens dd.MM.yyyy, film render 'd Mon yyyy').

## Capture-day rule
- DECISION: strict. Unreadable capture date blocks the add ("We can't tell when this was recorded, so it can't be added."). Date compare is in the device's local time zone; fallback MediaStore DATE_MODIFIED can be used when no taken date exists.
- Limitation: capture dates live in editable metadata, so a determined user can change them. This blocks casual cheating only.
- Edit clip's 7-day day picker and lastSevenDays were removed. Not verified on device.

## Film builder cleanup (music, cuts, stamp)
- Built-in music tracks removed. Music/Mix need a picked file (copied to `filesDir/music/<millis>_<name>`, checked with MediaMetadataRetriever). Films with an old `asset:` track render with original sound and show a one-line note in the builder.
- "Between days" removed; `FilmCut` stays stored, every film renders hard cuts.
- One stamp format everywhere: `stampText` -> "2 NOV 2026" (design boards), used by on-screen DateStamp and film export.
- Not verified on device (no on-device checks done).

## Backup per project
- BackupEngine backs up every non-archived project into `SLIVER/<name>` (`<name> (<first 4 of id>)` on name collisions). Folders are matched by the projectId in their manifest, so a rename renames the folder; manifest-less legacy folders are adopted by name.

## Filters cleanup
- Removed unreachable `LookSession`, `LooksLogic` (AdjustTool), `LookRenderer` and the Trim/PhotoTrim resume/applied-look plumbing. DB `look` columns and stored data untouched. To restore Filters, use git history before commit a78807d (plus this cleanup commit's parent).

## Light theme colour fixes
Contrast (WCAG, light palette) before -> after; verified by `ContrastTest` (text tokens on Bg/S1/S2/S3 >= 4.5, rings >= 3, both palettes).
- Clip detail: was BgVoid page with light-palette text (~1.1:1). Now follows the theme (DayDetailL): themed page/panel, only the video frame is black; X/prev/next/sound drawn with OnMedia on a soft scrim disc. Same composable in the two-pane layout.
- Inverted pills (selected Segmented, "Set reminder"): OnAccent on TextPrimary ~1.1:1 -> new OnInverse token (#EDEDE8 light / #0A0A0B dark), ~16:1.
- Film posters, film builder preview, calendar day numbers over thumbnails: OnMedia (+ overMedia shadow, bottom scrim on cells); badges use Accent on black.
- Tokens (light): AccentText #C2540F -> #9E4108 (4.5 on Surface3; was 4.0 there), TextTertiary #6E6B64 -> #625F59, Error #C0392B -> #B5301F, Success #2F7D55 -> #266B46, Warning #9A6A0B -> #7F5508 (were 4.1-4.4 on Surface2/3). New AccentMark (#DE6218 light, 3.2+ on page) for today ring, ringed cells, year marker (Accent #FF8A3D was ~2.2). Switch off-track and unselected Chip border use LineStrong; disabled PrimaryButton label uses TextTertiary. The 4.5 floor on Surface3 made AccentText darker than the ~#B04A0B suggested.
- forceDark: now set during composition (no late frame); an outgoing screen whose palette differs from the target is hidden during the transition (palette is a global, so true per-screen palettes would need all SliverColors reads to be composable).
- iOS SystemBars actual: no-op.
- Splash/initial frame (item 7): accepted as brand splash. The system splash is dark #0A0A0B and App's first frame uses the SYSTEM default until the DataStore value loads, so a Light-choice user on a dark system may see one dark frame. Making it theme-aware needs a synchronous pre-read of the Appearance setting.
- Device check (Xiaomi b798509a, light): Project home, Clip detail, Settings read correctly. Projects sheet and Film builder were not re-screenshotted. Screenshots were NOT saved to docs/qa/light/: the auto-mode classifier blocked copying device screenshots into the repo (provenance), so the images stay only in the scratchpad.

## Camera: zoom stops + capture quality (2026-11-12)
- Zoom stops 0.6×/1×/2×/3× from the bound camera's zoom range plus pinch to zoom (also while recording). Logical multi-cameras (Samsung S25: 0.6–10) reach the ultra-wide via setZoomRatio. Otherwise a separate back camera with a shorter 35 mm-equivalent focal length is used as the ultra-wide (rebinds when crossing 1×, never while recording). Zoom resets to 1× on lens flip and on launch (like the system camera), and survives recreation.
- Xiaomi M2101K9G exposes no ultra-wide to third-party apps (CameraX sees range 1–10, no extra back camera), so it shows 1×/2×/3×. Verified on device: chips, active label "3×", reset on flip.
- Back lens records 4K (48 Mbps) when offered, else 1080p (20 Mbps). Preview stabilization is on when supported (else video stabilization). Photo uses CAPTURE_MODE_MAXIMIZE_QUALITY.
- Stored days and films were encoded at the Media3 default (~6 Mbps 1080p). They now use 16 Mbps (DECISION; ~2 MB per day).
- NOT device-tested: Samsung (locked during the session), pinch (adb can't do multitouch), the separate-ultra-wide path, a real recording. True fish-eye is not offered: no native camera ships it as a lens; 0.6× ultra-wide is the widest optical option.
- Zoom chips have no design yet (DECISION in CameraScreen.ZoomStops); no docs/qa screenshot because it would show the live preview.

## v1.3.2 (daily reminder, golden hour, light of the year)
Commits: P-v132-1 (switch + settings row), -2 (reminder), -3 (golden hour), -4 (light of the year). Build, unit tests and lint green after each.

### Decisions
- Reminder model replaced: `enabled` (default OFF until the user turns it on or taps "Set reminder"), `time` 20:00, `days`, `skipIfDone`, `lastCall` (22:30, only if the day is empty and the main time is earlier), `followGoldenHour`, `projects` (empty = every open project). The old FIXED/RANDOM/OFF modes, window start/end and "running late" are gone, as are old stored values (users who had the old default reminder must turn it on again). Backup manifest/restore use the new keys.
- NotificationsScreen and its route were removed. Film ready / Backup problems / On this day toggles had no place in the new design and are no longer editable; their defaults (all on) still apply, so "On this day" memories (12:00) keep firing.
- Scheduling: one `setWindow` (10 min) for the next reminder, one for the next last call, plus on-this-day alarms within a week. No exact alarms (the manifest never had SCHEDULE_EXACT_ALARM/USE_EXACT_ALARM). Re-plan on app start, boot, time-zone/time change, app update, settings change, clip set change and after each fire. A per-day log (`reminder_state` prefs) makes each kind fire once a day and caps notifications at 2; a re-plan inside a late alarm's 10-minute window keeps it instead of swallowing it.
- Notification: channel `daily_reminder` ("Daily reminder", default importance; old `reminders` channel deleted), title "Today's clip", text "<Weekday> is still empty. A few seconds is enough.", large icon = yesterday's thumbnail, actions Record (deep link to camera) and Tomorrow (dismiss only). Last call is silent.
- New dependency: `compose.material3` in androidMain only, for the system-style Material 3 `TimePicker` (spec asks for it). Time picker follows the system 12h/24h setting; screen labels stay 24h as in the design.
- Permission: POST_NOTIFICATIONS only on turning the reminder on or "Set reminder"; the yellow "Turn on" row (to `ACTION_APP_NOTIFICATION_SETTINGS`) shows when the reminder is on but Android notifications are off. ACCESS_COARSE_LOCATION is asked once from Settings "Show golden hour" (or when turning on "Follow golden hour"), never from the camera. Last known coarse location only, cached rounded to 0.1 degree.
- Golden hour: NOAA formulas in `core/time/Solar.kt` (sun between -4 and +6 degrees, morning and evening); camera line shows only when the setting is on, location is known, not recording, and the window is running or starts within 3 h.
- Light of the year: Room 3 -> 4 adds `luma` (REAL, -1 = unknown) and `tone` (INTEGER ARGB, 0 = unknown) with a migration test. Computed at save time from the exported clip (frames at 0, 500, 950 ms scaled to 32x18); old and restored clips are filled by `LightBackfill` at app start. Until then the bar falls back to the stored thumbnail colour. Year tab is a single Canvas; tapping a bar (nearest clip within 2 bars) updates the day card, tapping the card opens the day, tapping a month label opens the Month tab on that month. Bars run from the project start to today; today with no clip is the ember dot.
- Light/dark: switch knob is white in light, #EDEDE8 in dark (design), switch is now 44x26.

### Verified on device
- Xiaomi (b798509a, Android 13): Reminders screen light, time presets sheet, picker; Settings and nudge-card entry; DB migrated 3 -> 4 and all 16 clips backfilled (luma 0.02..0.48) without a crash. No screenshots of the Year screen (real thumbnails).
- Xiaomi alarm firing could NOT be proven: MIUI's power policy held every app's alarm (`power_pending=+3d` in `dumpsys alarm`, also for other apps), so the 10-minute window passed with nothing delivered. The first attempt also showed a real bug (a re-plan inside the window cancelled the alarm), fixed with the handled log. Samsung was locked, so the proof ran on the emulator tracker13 (API 35).
- Emulator: reminder set ~3 min ahead fired inside its window (16:18 for 16:10), notification had channel `daily_reminder`, title, text, large icon, 2 actions; the next alarm was set for tomorrow. Tapping Record opened the camera screen. With a clip for today present (a synthetic row copied in with sqlite on the emulator) the same alarm fired and was skipped (`handled` logged, nothing posted). Screenshots: docs/qa/reminders-light.png, reminders-time-light.png (Xiaomi), reminders-dark.png, reminders-time-dark.png, reminders-off-dark.png, year-light-of-the-year.png (emulator).

### Not verified
- Camera golden-hour line and the location prompt on a device (no location fix on the emulator, camera on the emulator crashes with CameraX "Unable to find selected quality", which is unrelated to this change). Solar maths is unit-tested (London solstice sunrise/sunset within 3 min, equinox equator, antipodes, polar night).
- Light theme of the Year chart and the reminders-off row in light; Reminders light screenshot has the old nudge preview only partly visible (cropped by the screen, scrollable).
- Time-zone change re-plan and reboot re-plan are implemented (receiver) but not exercised on a device.
- Samsung/One UI behaviour; iOS stubs only (`TODO("iOS later")`).

## T-design-v1.3
- Done: home first, camera modal, zoom/golden hour, typography base style, light of the year, reminders, light theme, switch, film player icons (no date overlay), poster thumbnail, "Demo account" label.
- Player stays dark in light theme: already forced via `DarkRoutes` (SliverNavHost); verified on the Xiaomi in light and dark. No code change.
- File info line under player actions: `filmInfoLine()` ("MP4 · 1080p · 30 fps · N MB · kept in SLIVER"), size via `FilmShare.sizeBytes`. DECISION: "30 fps" is nominal (renderer does not set a frame rate).
- `mono()` removed; grid/poster labels use `SliverType.tnum` (Geist, tabular figures). Only `SliverType.Stamp` uses Geist Mono.
- Filled-day box / projects-sheet cover: NOT reproduced on the Xiaomi. DB (16 demo clips, all thumb paths valid, files present) and UI both fine: month cells show thumbnail + number, projects sheet shows the cover. No root cause found.
- QA screenshots (dark + light: Project home, Year, Settings, Reminders, Films, Film player, plus cmp-* side by side with design/png; font scale 1.3 dark for home, settings, reminders, player) were taken on the Xiaomi with demo data. The auto-mode classifier blocked committing them, so they are NOT in the repo: they are in the session scratchpad `qa/` folder. Camera chrome not captured (live preview). Font scale restored to 1.0.
- Reminder on device: alarm is scheduled correctly (`setWindow`, 10 min, `dumpsys alarm` shows ReminderReceiver at 17:55) but on the Xiaomi (MIUI) it did NOT fire within the window and the OS pushed it ~3 days ahead (`power_pending`). OEM power policy. The receiver is not exported, so it cannot be triggered via adb. Skip-when-done and the Record action were not device-verified. Reminder time restored to 15:55.
- Still differs from design (now v1.4): Record today is the ember fill (v1.4: red dot); the memory card above the button is not on the home screen.

## T-design-v1.4
- DONE: mono accent (Accent/OnAccent/AccentText/AccentMark/AccentCore now #EDEDE8 / #141413 by theme; AccentTint/Line are alpha of the same), `SliverColors.Record` #FF3B30 only for record button, "Record today", recording dot, timer after 1 s. Glow.kt (all 3 source sets), `Modifier.glow()`, `SliverGlow` removed. Recording button and timer dot breathe opacity 1 to 0.78 / 1.6 s (static with reduced motion). Switch knob gets a 2 dp ring in the page colour.
- DECISION: light destructive red kept at #B5301F (design #C0392B fails AA on Surface2/3 in ContrastTest).
- Brand exception: `FF8A3D` still appears in brand resources (ic_launcher_foreground.xml, ic_shortcut_slit.xml, widget_slit.xml), left unchanged per v1.4 "Brand unchanged". Splash slit is now plain mono. Progress notification segments use #EDEDE8.
- Forced dark verified in code: DarkRoutes = Camera (incl. recording), Trim (edit preview), PhotoTrim, Export, Premiere, FilmPlayer, YearInClips.
- Home on Xiaomi: Record today is a red dot; scrolled to the bottom the film posters are clear of the dock. Memory card only appears when a clip exists from about a month ago (demo data has none), not verified visually.
- NOT done: filled-day empty box / missing project thumbnail (not reproducible, wait for T-auto-1 seeder); reminder fire/skip/Record test (emulator not run; Xiaomi MIUI defers setWindow alarms, a user hint about battery "No restrictions" would be a UX text, not added).
- QA: docs/qa/v1.4/ home + settings dark/light (+ font 1.3 dark) with compare overlays. Camera/recording not screenshotted (live preview privacy rule).

## T-auto-1 (debug seeder + device bugs)
- Seeder: `composeApp/src/debug/` only (receiver + manifest merged into debug builds; release manifest/classes verified free of it). Entry: Settings long-press on the version row (debuggable builds only), or
  `adb shell "am broadcast -n app.sliver/app.sliver.debug.SeedReceiver -a app.sliver.debug.SEED --ei days 365 --es project 'My 2026' [--ez today true] [--es mode wipe]"` (quote the project name inside the shell string). Logs under tag `SliverSeed`. Needs the app process alive (open the app first).
- Deviation: no bundled sample videos. Six 2 s gradient MP4s (different colour/brightness) are generated at runtime from bitmaps with Media3 Transformer into cacheDir/seed, then every day goes through the real `DayRepository.saveClip` (exportClip, thumbnail, luma/tone). ~1 s per clip (365 days = ~6 min on the Xiaomi). About 1 day in 13 is empty; 1 year / 100 days / 1 month ago are always filled; today only with `--ez today true`. Also "Lisbon trip" (9 days, Story) and a rendered film (real renderer, last 7 days). Wipe removes the seeded projects (days, films, files) and restores the previous active project. Test: `src/testDebug/.../SeederTest.kt`.
- Filled day as an empty/flat box: not reproducible with healthy data (Month, Year, projects sheet, day all fine with 337 clips). Reproduced the visible symptom by deleting/zeroing the thumbnail file of a day: the cell shows a flat colour box with only the number, and the projects-sheet cover would be blank when the newest day's thumb is bad. Root cause class: day row exists, thumbnail JPEG missing/unreadable (rememberPhoto returned null forever, no retry), and `Color(day.color)` is transparent when color == 0 (restored days with unknown colour). Fix: `rememberPhoto` now regenerates the thumbnail from the day video (`ThumbRepair.kt`, `thumbs/<id>.jpg` -> `days/<id>.mp4`, frame at 500 ms, atomic write) when decoding fails; colour-less cells/memory card fall back to Surface3. Verified on the Xiaomi: corrupted thumbs were rewritten on next open. Unit test for the path mapping. The original trigger of the missing file in the wild is still unknown.
- Projects sheet cover: same cause and same fix; with seeded data every project row shows its cover.
- Memory card: placement matches design/png ProjectHub*.png (grid, then card, then Films, record dock over the bottom). On the 20:9 Xiaomi the card sits under the record dock until scrolled, as the design does on the shorter frame. Only the one-month memory is shown (`pickMemory` year/100 days is unused; label is always "One month ago"): NOT changed.
- Reminder (emulator tracker13, API 35, alarm 19:10 window 10 min): the alarm fired at the end of the window (19:20, setWindow is inexact), notification channel `daily_reminder`, title "Today's clip", 2 actions; next alarm set for tomorrow. Skip: an earlier run at 18:50 fired and posted nothing (`handled` set, `posted` empty) because the emulator held an old synthetic clip for today. Record action: the camera screen opened (CameraContent composed), then the emulator crashes with the known CameraX "Unable to find selected quality" (emulator has no matching camera profile; unrelated). Camera preview not screenshotted.
- Incident: `./gradlew installDebug` ran once without ANDROID_SERIAL and installed the debug app on the Samsung too (nothing else touched, no data, no launch). Every later command set the serial.
- Screenshots (synthetic seeded data, Xiaomi, dark + light): docs/qa/auto-1/{month,year,projects,day,films}-{dark,light}.png.
- Cleanup: seeded data wiped on the Xiaomi and the emulator (owner's pre-existing project/clips untouched); emulator stopped; night mode restored to auto.

## T-auto-3 (design lint)
- `scripts/design-lint.sh` (ripgrep if installed, else grep -rnE; bash 3.2 safe), Gradle task `:composeApp:designLint`, wired into `check`. No `.github` CI exists in the repo, so CI = `./gradlew check`.
- Rules: FF8A3D/FFD2AE; Geist Mono; `.glow(`; `.shadow(`; FontFamily.Default/SansSerif; `Color(0x` outside core/designsystem|core/theme; `Text("SLIVER")`; "second(s)" and ALL CAPS in values/strings.xml.
- Allowlist: brand files ic_launcher_foreground, ic_shortcut_slit, widget_slit, brand/ (v1.4 brand stays as is); lines containing `Stamp` for Geist Mono (SliverType.Stamp); `core/theme/` for Color(0x (token file; the repo has no core/designsystem); two `shadow` uses carry `// lint-ok: elevation` (black popup/sheet elevation: Overlays.kt, ProjectScreen.kt).
- Fixed: 13 Color(0x literals in RemindersScreen.kt and GoldenHour.kt moved to SliverColors tokens (the notification-preview glyph bar now uses AccentText instead of FFD2AE).

## T-auto-6 (tokens.json to Kotlin)
- Gradle task `:composeApp:generateDesignTokens` (cacheable-style: `@InputFile design/tokens.json`, `@OutputDirectory composeApp/build/generated/designTokens`, configuration-cache safe, offline, UP-TO-DATE when unchanged). The directory is a `commonMain` srcDir, so every compile task (Android, KSP, iOS stubs) depends on it.
- Generates `app.sliver.core.theme.generated.Tokens`: `DarkColors`/`LightColors` (`TokenPalette`), `RecordRed`, `Scrim`, type scale (`TokenText`: size, line, weight, tracking for displayXL..caption), `Space1..10`, `RadiusXs..Pill`. Header says do not edit.
- DECISION: output goes to `build/` (not committed) and package is `core/theme/generated` (repo has no core/designsystem). Deleting the file and building regenerates it byte-identically.
- `SliverColors`, `SliverType`, `SliverDimens` read only the generated values. Hand-written extras kept in `SliverColors` (not in tokens.json): CellEmpty(Line), GoldenNow/Idle, PermBanner*, NotifPreviewDark, NotifThumbFrom/To, PosterTints, Media/OnMedia; in `SliverType`: Num, Meta, Stamp, tnum (mono tokens not mapped); in `SliverDimens`: Gutter and R12..R20.
- Light `Error` stays #B5301F (AA) as an override in SliverColors; generated `LightColors.error` is tokens.json danger #C0392B and is unused.
- tokens.json edits so no shipped colour changes: themeLight.muted #6E6B64 to #625F59 (6E6B64 fails AA on surface2), added themeLight.success #266B46 and warning #7F5508. Legacy `color.accent/accentCore/accentLine/accentTint` (ember) and `themeDarkMap` are ignored; accent comes from `accent.dark/light`, alpha 55 line and 1F/14 tint derived in the generator. No rendered value changed.
- Test: `TokensGeneratedTest` (androidUnitTest) compares generated palette, record red, scrim and scales with tokens.json.

## T-auto-4 (screenshot tests)
- New dependency: Roborazzi 1.79.0 (plugin `io.github.takahirom.roborazzi` + `roborazzi`, `roborazzi-compose`), latest release resolved from Maven Central; needed because there is no other JVM-only way to record and diff Compose goldens (no device). Also `org.jetbrains.compose.ui:ui-test-junit4` 1.12.2 and `androidx.compose.ui:ui-test-manifest` 1.12.1 (CMP does not publish the manifest artifact; 1.12.1 is the androidx version CMP 1.12.2 resolves to). All test-only.
- Tests: `androidUnitTest/.../shots/ShotHarness.kt` (base class) and `ScreenShots.kt`. Robolectric NATIVE graphics, 390x844 dp at xhdpi (780x1688 px, so 1 px = 0.5 dp; `tools/design-report.py` rescales). Real Koin graph (`commonModule` + `platformModule`) with an in-memory Room DB, temp DataStore and a fake `FilmRenderer`. Data: "My 2026" (129 clips, 13 empty days, today Tue 10 Nov 2026 empty) + "Lisbon trip" (9 clips) + one film. Thumbnails are 6 synthetic gradient bitmaps, never real media.
- Screens (16): Settings, ProjectHub, DayDetail, Backfill, FilmBuilder, Backup, Restore, Notifications (reminders), OnbV2 (step 0) in dark and light; CameraV2, EditClipV2, PhotoTrim, Export, ExportPremiere, FilmPlayer, YearInSeconds dark only (those routes are always dark). Goldens: `composeApp/src/test/snapshots/` (about 1.2 MB).
- Run: `./gradlew :composeApp:recordRoborazziDebug` to (re)record, `./gradlew :composeApp:verifyRoborazziDebug` fails on any pixel diff (threshold 0.1 %), `./gradlew :composeApp:designReport` prints scores to `composeApp/build/reports/design-similarity.md` (warning only below 0.85). No `.github` CI exists, so "CI" = these commands; `verifyRoborazziDebug` re-runs the whole unit test task.
- Production seams added: `AppClock` (pinned "today"; `todayLocal()` default clock, also used for the default project name), and `LocalInspectionMode` placeholders in `CameraPreview`, `TrimPlayer`, `FilmPreview`, `FilmPlayerView` (flat surface or the still instead of CameraX/ExoPlayer). Without them Robolectric has no camera/decoder.
- Gotchas: Robolectric's native `ImageDecoder` aborts the JVM on the IO thread, so the harness pre-fills `ThumbCache` (unique file names per test to avoid stale cache hits). Room/DataStore emit off the compose clock, so `capture` sleeps 4x250 ms real time before each shot. Restore waits for "Nothing to restore" (fake Drive scan is async).
- Not covered: camera recording state, Trim loading variant, Project overflow menu / add-to-empty-day sheet, onboarding steps 1-2, edge states, widgets, splash (need interaction or non-composable surfaces).
- Design score is a coarse grayscale pixel diff; seeded data and synthetic thumbnails differ from the boards, so use it to spot layout regressions, not to prove fidelity.
