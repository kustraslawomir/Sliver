# SLIVER — product spec (v1)

One second of video per day → a calendar of seconds → films (week / month / year / custom).
Free, offline-first, private. Account only for optional backup to the user's own Google Drive.

## Core rules
- **One day = one 1.000 s clip.** Sources: in-app recording, gallery video, gallery photo (becomes 1 s).
- **Day = local calendar date** of the capture (device time zone at capture time). Gallery items use their taken date.
- **No streaks.** Empty days are neutral. Archive header shows "N SECONDS · SINCE <first day>".
- **Date stamp**: global setting, default ON. Shown on previews; burned in only at export; each film can turn it off before rendering.
- **Formats**: each film is Cinema 16:9 (default) or Story 9:16. Camera suggests the orientation of the *active film*. Clips that don't match are center-cropped (or blurred fill) at render. Never block, never nag.
- **Camera settings persist** between launches (look, grid, active film). Mode (Video), lens (back), zoom (1×), self-timer and the light (flash) are NOT remembered: the camera always opens on those defaults.

## Screens

### Launch
Black → glowing vertical slit grows → becomes the "I" in SL|VER → 1.4 s → cross-fade to Camera. Reduced motion: static logo.

### Onboarding (7 steps, first launch only)
1. "One second. Every day." (concept)
2. "We find the best second. You have the final say." (auto-pick concept)
3. "Watch your year add up." + format choice CINEMA 16:9 / STORY 9:16 (sets default film format)
4. Camera + mic pre-prompt → system dialog
5. Photos pre-prompt → system dialog (limited access is fine)
6. Reminder: Fixed time (chips 09:00 / 13:00 / 19:30 / 21:30) or Random window, with live notification preview → notification permission
7. Optional "Save your seconds" — Continue with Google, or "Skip — start as guest"
Steps 1–3 have Skip. No backfill step (Fill the past is never suggested).

### Camera (home, app opens here)
- Full-bleed preview. Top: date chip; "FOR: <film> · <format> ▾" chip (switch active film / new film).
- Right rail: grid, self-timer (off/3/10 s). Top right: flash.
- Bottom: looks strip (Natural + 8), PHOTO / VIDEO segmented, gallery thumb (bottom-left), record button, flip.
- Record: tap start/stop, or hold > 300 ms = record while held. Running timer. Any length.
- Swipe left/right on preview = change look; look name flashes.
- Cinema film + phone upright: dim outside a 16:9 band, chip "Turn sideways for the full frame" for 3.5 s.
- Landscape: UI stays portrait-locked, icons rotate; record button ends up under the thumb.
- Today already captured: card "Today's second is in" + Replace.

### Trim & Pick (hero)
- Header: close, "FOR <day> ▾" (opens Save-to-which-day sheet, last 7 days), audio toggle.
- Big looping preview of the chosen second with current look + date stamp. Badge "✦ AUTO-PICKED" → "CUSTOM" after a drag.
- Filmstrip with a **fixed 1 s window in the center**; the strip moves under it. Haptic tick every 0.25 s, impact on release, 400 ms glow pulse ("lock-in").
- Long clips (> 60 s): minimap above the strip, tap to jump.
- Options: speed 1× / 0.5× (0.5 s of source stretched to 1 s).
- CTA "Use this second". Secondary: Retake, Looks, Replace day.
- Saved: clip animates into today's cell, toast "Saved to <day> · Undo" (6 s).
- One-time coach mark "Drag to choose your second."
- Clip < 1 s: no strip; choose Slow to fit / Loop / Hold last frame.
- Portrait clip in a Cinema film: draggable 16:9 band over the preview (default centered), saved as crop offset.

### Gallery picker (custom)
Recents / Favorites, filter All / Videos / Photos, "FOR <date>" chip. Video → Trim. Photo (single select) → Photo → 1 s.

### Photo → 1 s
Looping preview, motion chips Still (default) / Slow zoom (opt-in, 1.00 → 1.08, eased). Use this second / Retake / Looks.

### Looks editor
Preview (hold to compare with original), tabs LOOKS / ADJUST.
- Looks: Natural, Noir, Blackout, Night Neon, Cold Steel, Ember, VHS 94, Hard Light, Ghost — thumbnails of the current clip; intensity slider per look.
- Adjust: chips Sharpness (first, default selected), Contrast, Grain, Vignette, Fade, Saturation; one big slider; Reset per tool + double-tap reset; "Reset all".
- "Set as default look" switch. "Apply look".

### Archive
- Header: "N SECONDS · SINCE …", settings gear.
- MONTH / YEAR switch.
- Month grid (Mon-first): cells filled (thumb) / empty past (dim dot, tap → Add sheet) / today (ember ring → Camera) / future / before first day. Backfilled days show a small gallery glyph.
- Year: 12 rows × 31 tiny cells colored by each clip; tap a month → month grid. "Auto-import from..." in the project menu. "Make your <year> film so far".
- Memory card: "ONE MONTH AGO · <date>" (rules: 1 month / 100 days / 1 year ago; year once available).
- Add sheet for an empty day: Video from gallery / Photo → 1 second (gallery opens for that date), Leave it open.

### Day detail
Full-bleed loop (look + date stamp), prev/next day, sound toggle. Weekday, DAY n, time. Chips: look (→ editor), place (from file location; name cached when online). Replace / Change look / Remove (confirm; original in gallery untouched; Drive copy trashed).

### Auto-import drafts (replaces "Fill the past"; Project `...` menu and Settings)
- Entry: "Auto-import from..." opens a page: a "From" date (default = project start, any day up to today, own month-grid picker), "N empty days since <date>" live from the calendar, "Import drafts". Photo/video permission (READ_MEDIA_IMAGES + READ_MEDIA_VIDEO + VISUAL_USER_SELECTED) is asked only here; partial access imports what is visible and shows a "Limited access / Allow all" banner.
- Scan: MediaStore by DATE_TAKEN (device zone), only days with no row. Per gap day the default is the longest video (a motion photo counts as a video), else a photo; 1 s window: motion photo around its presentation timestamp, video in the middle, photo Still. Created through the normal save path as `draft` days with up to 30 candidates (videos by length, then photos). WorkManager, sequential, resumable (re-scan skips days that already have a row), cancelable, progress notification. "Undo all" removes only unconfirmed drafts of the run.
- Drafts are calendar-only: dashed outline, dimmed thumbnail, "Draft" label, a11y "<date>, draft, review"; header chip "N drafts to review". They are excluded from counts, films, year/light, memory card, reminders, project stats, backup and restore until kept.
- Draft editor (Edit clip / Photo clip): "Draft - i of N" with previous/next draft, strip "Other items from this day" (swap in place, no picker), "Video | Photo" for motion photos (also after the Photo Picker), "Keep this clip" (confirms, no 6 s undo bar, jumps to the next draft) and "Skip day" (deletes the draft, undo toast).

### Films
- Library: Year in Seconds teaser card, posters (title, format, runtime, project span "10 Jan – 12 Nov 2026 · 129 clips"; a collecting film is the project film that grows with new clips). Long-press a poster for "Delete film" (red text, undo toast, no confirm). "+ New film".
- Builder (one screen): live preview (tap = pause), scrubber with day/month ticks (a film is always the whole project, no RANGE), SOUND Original / Music / Mix (Music and Mix need a file picked with "Pick a file"; copied into app storage, shown with Replace / Remove), DATE ON SCREEN On / Off, FILTERS (None, Kiosk, Afterhours, Silver, Sunday, Tape, Halo: thumbnails rendered from a frame of the film, Intensity and Grain sliders; saved on the film; Kiosk shows an orange LED date stamp, Tape a camcorder stamp). CTA "Save film" (runtime shown under the scrubber). Every filled day of the project is used; open days are skipped.
- Export: progress ("made on your phone"), continues in background → Premiere: slit opens into the frame, title card "A film by you / <name> / project span · N clips · runtime" → Play film / Save to Photos / Share… / Done.
- Player: play/pause, scrubber, mute, fullscreen (rotate), Share film… (system share sheet → YouTube, Instagram, …), Save to Photos, Edit film.
- Year in Seconds: unlocks 26 Dec (local). Stats: seconds kept, films made, most filmed month(s), "Watch the premiere".

### Backup & account (optional)
- "Save your seconds" sheet: Continue with Google / Not now.
- Before Google consent: plain words (one visible folder "SLIVER", only files SLIVER creates, we never store videos, ~2 MB per second).
- Backup & sync screen: status line "Synced · 2 min ago · 2 phones", status card (7 states below), folder, backed up count, Drive space, Auto-backup, Wi-Fi only, Sync now, Export everything (.zip), Disconnect (confirm).
- States: backing up (progress, resumable) · up to date · paused (waiting for Wi-Fi) · failed (retry) · Drive full · access revoked (reconnect) · offline.
- Sync (T-sync-two-devices): one Google account on several phones keeps the same projects. Last writer wins per record (project, day, film definition) by `updatedAt`, ties by device id; a clip that loses is kept in Drive `replaced/` for 30 days and the other phone shows "10 Nov was changed on another phone" once. Deletes travel as tombstones (kept 90 days). Drafts, renders, camera/reminder/appearance settings never sync. Triggers: app start, 30 s after a save or delete, hourly (network), pull down on the project screen, Sync now. No instant push.
- Restore (fresh install) is the first pull of a sync: Welcome back → Continue with Google → "We found N seconds" → Restore all (progress, mosaic fills) → archive. Camera usable meanwhile.
- Account: signed in with Google, what we keep (videos: never), Sign out (confirm), Delete account (checkboxes: also delete SLIVER folder in Drive, also delete seconds from this phone).

### Notifications (all local)
Settings: Daily reminder Fixed / Random / Off, days of week; toggles Running late (21:00 if no second, **default off**), Film ready, Backup problems, On this day. Denied state: calm banner + Open Settings.
Copy:
- Reminder: "Your second for today" — "Whatever's in front of you counts."
- Random: "Right now." — "Whatever you're looking at — that's today."
- Running late: "Still time for today" — "Even the ceiling counts."
- Film ready: "“<film>” is ready" — "<N> seconds. Press play."
- Backup: "Backup paused" — "Your Drive is full. Your seconds are safe on this phone."
- On this day: "One month ago today" — "<Weekday>, <date>. Want to see it?"

### Settings
Account · Backup · New film format · Date stamp (on) · Also save full recordings to Photos (off) · Notifications · Auto-import from... · What SLIVER stores · Send feedback (mailto) · version.

### Widgets
Android: Glance 2×2 (today waiting / today ✓ thumbnail, tap → camera) and Quick Settings tile "SLIVER" → camera.

### Edge states
Empty archive (first day), camera access off (→ Settings / add from gallery), export failed (resume), phone storage full.

## Cut from v1 (don't build)
Day notes, voice notes, mood tags, streaks, paywall, multi-photo import, film-strip archive view, Pan/Parallax, Shadows/Highlights/Temperature sliders, "Try another" suggestions, auto subject reframe, 4K, dual crops, Apple/email sign-in, Firebase, push server, quiet hours, quality setting, glitch/flash transitions, subtitles, recap image card, direct YouTube API, iOS.

### Capture-day rule
- A clip can only be added to the day its media was recorded (EXIF DateTimeOriginal for photos and Motion Photos, video creation time for video, MediaStore DATE_TAKEN / modified as fallback; device local time zone). Mismatch or unreadable date: notice, nothing opens. In-app recordings are always today.
- Edit clip / Photo clip show the fixed day as non-interactive text; saving uses that day.
- Auto-import assigns each gallery item to its own capture date (DATE_TAKEN only).


## Filters (T-looks-film, 2026-11-12)
- Filters are FILM-LEVEL ONLY: chosen in the Film builder, applied to preview and render. Clips are never filtered: no filter control in camera (swipe removed), Edit clip, Photo clip or Day detail; stored clips stay clean; old per-day `look` values are ignored.
- One GLSL pass (grain with per-frame seed and luminance weighting, halation, bloom, flash falloff, soft plastic lens, chroma bleed, scanlines, gate weave, flicker, light leak) after a 33^3 LUT; all randomness is a pure function of (clip id, frame time).
- Parameter pool in `looks/candidates.json` (40 candidates); only kept ones ship as presets. Sheets: `./gradlew :composeApp:testDebugUnitTest --tests '*LookSheetsTest*' -PlookSheets=1` writes `docs/qa/looks-film/`.
