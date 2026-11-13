# Reelog — functional overview (current behaviour, 2026-11-13)

Snapshot of what the app does today on `auto/build` (updated for Reelog + design v1.5). Where `docs/SPEC.md` disagrees
(it still describes camera-as-home, a Looks editor, a Year view, 7-step onboarding), **this file wins**;
owner decisions are also listed in `design/INDEX.md` ("v1.4.1 owner decisions").

## Identity and platform
- App name **Reelog** (was Fragments, before that SLIVER), one value `appName` in `design/tokens.json` →
  `APP_NAME` (Kotlin) and `@string/app_name` (Android). Android applicationId / iOS bundle id stay `skustra.fragments`
  (OAuth client + existing backups depend on it); Kotlin namespace and classes stay `app.sliver.*`. KMP + Compose Multiplatform; Android only is built;
  iOS is a stub shell.
- One clip per day, organised in **projects** (e.g. "My 2026", "Lena"). Each day is a 1.000 s, 1080p
  H.264 clip stored in app storage (`days/<project>/<date>.mp4` + thumbnail). Originals in the gallery are
  never modified; deleting them does not affect the app.
- Offline-first. Network only for the user's own Google Drive (backup & sync). No analytics, ads, paywall, streaks.
- Visual language: v1.4 mono (light text on near-black / dark on paper), red only for recording and
  "Record today". Light + dark theme (Settings → Appearance); camera, editor, film player stay dark.

## Launch and onboarding
- Splash (cold start): system splash = red disc on #0A0A0B, then the disc shrinks into the "o" of "reel●g" with "One clip a day", and lands on the Record today button (plain fade before onboarding; reduced motion holds the word 600 ms).
- First run, 3 steps: **Welcome** ("One clip a day. One film a year." · Start / Restore from backup) →
  **Project** (name, default "My <year>", format Cinema 16:9 or Story 9:16 · Create project) →
  **Camera** permission pre-prompt. Then the Project screen.
- "Restore from backup" goes to Google sign-in, then the first sync pulls everything.

## Project screen (home)
- Header: project name with ▾ (project switcher sheet), edit pencil, profile icon; subtitle
  "N clips · since <first clip>" ("since" = earliest clip, even if before the project was created).
- Sync status line while syncing; pull down to sync.
- Optional daily-reminder nudge card ("Want a daily nudge?" · Set reminder / No thanks).
- Row "N auto-filled days to check" / "N drafts to review" when present → sheet with the days' thumbnails: **Keep all**,
  **Check one by one**, **Remove unchecked** (only unreviewed auto days, one undo bar).
- **Month calendar** (Year view and Month/Year tabs were removed): ‹ › reach back to the earliest clip/draft.
  Cells: filled (thumbnail + day number), auto-filled (dashed outline, full opacity, no counter), draft
  (dashed + dimmed + "Draft" label), empty past day ("+", tap → add), today (outlined, tap → camera),
  future (disabled). Tap filled day → Day detail. Tap auto-filled/draft day → straight into the editor.
- Films section with posters, "+ Make film". Fixed bottom dock: red **Record today** button.
- Projects sheet: list with covers, new project, edit pencil per row, hold to delete project (and account) with
  confirmation, archive.

## Camera
- Opens as a full-screen modal with ✕. Always starts on Video, back lens, 1×, light off (none of these are
  remembered); grid is remembered (⋯ menu).
- Photo · Video words with a dot; record button with a liquid glow; tap to start/stop or hold to record
  while held; timer turns red after 1 s ("Clip OK"). Volume key can record (setting).
- Zoom stops 0.6× / 1× / 2× / 3× from the lens' real range (ultra-wide via logical camera or a separate
  ultra-wide camera) + pinch to zoom, also while recording. Tap to focus, hold to lock AE/AF, exposure slider.
- Back lens records 4K at 48 Mbps when available (else 1080p 20 Mbps), stabilisation when supported.
- Cinema project + phone upright: band dimming outside 16:9 + "Turn sideways" hint.
- Golden-hour line in the top bar when enabled and within 3 h (coarse location, asked from Settings only).
- "Add" opens Add media (see below); flip camera.

## Edit clip (video) and Photo clip
- Fixed 1 s window over a filmstrip; drag the strip, live scrub shows the frame under the window
  (fast CLOSEST_SYNC scrub player), haptic ticks; minimap for long clips; sound toggle.
- 9:16 crop area can be chosen for landscape video/photos in a vertical (Story) project; 16:9 band for
  portrait clips in Cinema.
- Photo clip: Still / Slow zoom. Motion Photo / Live Photo: Video · Photo switch.
- Save → the day is written; undo toast. Clips are always saved **without filters**.
- Draft/auto-day mode: "Draft · i of N" header (prev/next only when walking one by one), strip
  "Other items from this day" (swap in place), **Keep this clip**, **Remove day**.

## Adding media to a day
- Past day with gallery access granted → **From this day** screen: all photos/videos taken that day,
  camera first, screenshots never, "Show saved and received too" switch, "Browse all photos" → system picker.
- Otherwise the system Photo Picker (multi-select so a Live Photo pair can be picked).
- Capture-date rule: a known capture date (EXIF / QuickTime) must match the day, else refused with a notice.
  File-name date (Telegram/WhatsApp/Signal/camera patterns) is a hint; no date → allowed on the chosen day
  with "Date unknown — added to …". DATE_MODIFIED/ADDED are never treated as capture dates.
- HEIC → upright JPEG; HDR video tone-mapped to SDR at export; Live Photo pair (still + ≤6 s video, same
  name or ≤2 s apart) → video clip. Every pick is copied into app storage.

## Day detail
Full-bleed loop of the day, prev/next day, sound; **Replace** (same as Adding media for that day) and
**Remove** (undo). No per-day filters.

## Fill the calendar (auto-import)
- Entry: project ⋯ menu and Settings → "Fill the calendar". Page: "From" chips **Project start / This year /
  Last 30 days / Pick date…**, big "N empty days to fill", switches **Keep without review** (default ON) and
  **Include saved and received media** (default OFF = camera photos/videos only, DCIM). Screenshots and
  screen recordings are always skipped. Gallery permission (READ_MEDIA_*) is requested only here.
- Per empty day: best camera video (longest; motion photo counts as video), else a photo; 1 s window centred
  (motion photo: around its timestamp). Up to 30 candidates stored per day (camera first).
- Runs in background (WorkManager, resumable, cancelable, progress notification). Progress page shows:
  "x of y days", empty days, items found, days with media, videos · photos, couldn't read, current day, ETA,
  filled range, and "your gallery has nothing before <date>" when relevant.
- Keep-without-review ON → real days (count everywhere) marked as auto-filled until reviewed; OFF → drafts
  (excluded from films, counts, sync until kept). After a fill the app jumps to the calendar on the first
  filled month.
- Run page when finished: Keep all / Review one by one / Remove unchecked / Done / Undo all.
- A removed auto/draft day remembers its gallery items; the next import leaves it empty unless the gallery has
  **new** items for that day (then those come first, old ones as extra options). Undo forgets the removal.

## Films
- A film is always the **whole project** (no range). Library posters with project span and clip count;
  long-press → Delete film (undo).
- Film builder: live preview (tap = pause), scrubber, Sound Original / Music / Mix (pick an audio file), Date on
  screen on/off, **Filters** (film-level only): None, Kiosk, Afterhours, Silver, Sunday, Tape, Halo, with
  thumbnails from the film, Intensity and Grain sliders. 40-candidate pool exists in `looks/candidates.json`.
- Export renders 1080p (16 Mbps) in the background → Premiere → Play / Save to Photos / Share / Done.
- Film player: icon-only controls (mute, play/pause, fullscreen), scrubber with times, file info line
  "MP4 · 1080p · 30 fps · N MB · kept in Reelog", Save to Photos, Edit film, Delete film (red text, undo).

## Backup & sync (Google Drive, optional)
- Sign in: "Continue with Google" goes straight to Google (Credential Manager + `drive.file` consent; no
  explainer page). OAuth project `fragments-514519`, consent screen in Testing (owner is a test user).
- Backup & sync screen: chevron header, one status card (Synced · just now · N phones / Syncing / waiting for
  Wi-Fi / failed / Drive full / access removed / offline), Folder "Reelog" (an existing "Fragments" folder keeps being used), Clips line, Drive free space
  with ⚠ "Almost full" under 1 GB, Sync automatically, Wi-Fi only, Sync now, Export everything (.zip),
  account row (email · Sign out), Delete account.
- Two-way sync between phones on one account: one Drive file per record (project, day, film definition),
  last writer wins by `updatedAt`, losing clip kept in `replaced/` 30 days, tombstones for deletes, drafts
  never sync. Triggers: app start, 30 s after a save/delete, hourly, pull-down, Sync now. No instant push.
- Restore = first sync on a fresh install.

## Reminders and notifications (local only)
- Daily reminder screen: on/off, time presets + custom time, days of week, skip if today already has a clip,
  "last call" (22:30, only if empty), follow golden hour, which projects count; max 2 per day; inexact
  `setWindow` alarms (MIUI may delay them). Notification actions: Record (opens camera) / Tomorrow.
- POST_NOTIFICATIONS asked only when turning the reminder on. Progress notifications for render, backup,
  restore, auto-import (ProgressStyle on API 36+).

## Settings
Account · Backup & sync · Default film format · Appearance (System / Light / Dark) · Date stamp · Also save
full recordings to Photos · Volume key records · Show golden hour · Daily reminder · Fill the calendar ·
What Reelog stores · Send feedback · version.

## Other
- Undo: one bar for every undoable action (48dp, inverted colours, 5 s), above the Record dock; above the buttons on Day detail.
- Playback pauses everywhere when the screen locks or the app goes to the background.
- Widget (today waiting / today's thumbnail → camera) and Quick Settings tile → camera.
- Debug only: data seeder (Settings → long-press version, or adb broadcast to
  `skustra.fragments/app.sliver.debug.SeedReceiver`).

## Dev automation
`scripts/qa.sh` (device QA via adb/uiautomator), Roborazzi goldens (`recordRoborazziDebug` /
`verifyRoborazziDebug`), `scripts/design-lint.sh` in `check`, tokens generated from `design/tokens.json`,
`tools/design/render.sh` (render design boards), `.github/workflows/builder.yml` (inert until
`BUILDER_AUTORUN=true`). Test devices: Xiaomi `b798509a` / emulator `tracker13`; Samsung only on owner request.
