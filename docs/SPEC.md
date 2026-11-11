# SLIVER — product spec (v1)

One second of video per day → a calendar of seconds → films (week / month / year / custom).
Free, offline-first, private. Account only for optional backup to the user's own Google Drive.

## Core rules
- **One day = one 1.000 s clip.** Sources: in-app recording, gallery video, gallery photo (becomes 1 s).
- **Day = local calendar date** of the capture (device time zone at capture time). Gallery items use their taken date.
- **No streaks.** Empty days are neutral. Archive header shows "N SECONDS · SINCE <first day>".
- **Date stamp**: global setting, default ON. Shown on previews; burned in only at export; each film can turn it off before rendering.
- **Formats**: each film is Cinema 16:9 (default) or Story 9:16. Camera suggests the orientation of the *active film*. Clips that don't match are center-cropped (or blurred fill) at render. Never block, never nag.
- **Camera settings persist** between launches (mode, look, lens, flash, grid, active film). Self-timer always starts OFF.

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
- Year: 12 rows × 31 tiny cells colored by each clip; tap a month → month grid. "Fill the past from your gallery" link. "Make your <year> film so far".
- Memory card: "ONE MONTH AGO · <date>" (rules: 1 month / 100 days / 1 year ago; year once available).
- Add sheet for an empty day: Video from gallery / Photo → 1 second (gallery opens for that date), Leave it open.

### Day detail
Full-bleed loop (look + date stamp), prev/next day, sound toggle. Weekday, DAY n, time. Chips: look (→ editor), place (from file location; name cached when online). CTA "Share this second" (sheet: date stamp, original sound, SLIVER mark off by default → share intent). Replace / Change look / Remove (confirm; original in gallery untouched; Drive copy trashed).

### Fill the past (opt-in tool, Archive Year + Settings)
Intro (needs full photo access, nothing leaves the phone, filled days never touched) + range: Since I started / This year / Everything → scan by taken date → mosaic of empty days that can be filled → toggle months, "Use photos too" → "Fill N days" (background, progress) → Undo all. Limited access banner with "Allow all".

### Films
- Library: Year in Seconds teaser card, posters (title, format, runtime, range, LIVE badge for collecting films). "+ New film".
- Builder (one screen): live preview (tap = pause), scrubber with day/month ticks, RANGE Week / Month / Year / Custom, SOUND Original / Music / Mix + track list (4 built-in + "From your files…"), BETWEEN DAYS Cut / Fade, DATE ON SCREEN On / Off, ONE LOOK (None or a preset). CTA "Make film · mm:ss". Open days are skipped.
- Export: progress ("made on your phone"), continues in background → Premiere: slit opens into the frame, title card "A FILM BY YOU / <name> / range · N SECONDS · runtime" → Play film / Save to Photos / Share… / Done.
- Player: play/pause, scrubber, mute, fullscreen (rotate), Share film… (system share sheet → YouTube, Instagram, …), Save to Photos, Edit film.
- Year in Seconds: unlocks 26 Dec (local). Stats: seconds kept, films made, most filmed month(s), favourite look → "Watch the premiere".

### Backup & account (optional)
- "Save your seconds" sheet: Continue with Google / Not now.
- Before Google consent: plain words (one visible folder "SLIVER", only files SLIVER creates, we never store videos, ~2 MB per second).
- Backup screen: status card (7 states below), folder, backed up count, Drive space, Auto-backup, Wi-Fi only, Back up now, Export everything (.zip), Disconnect (confirm).
- States: backing up (progress, resumable) · up to date · paused (waiting for Wi-Fi) · failed (retry) · Drive full · access revoked (reconnect) · offline.
- Restore (fresh install): Welcome back → Continue with Google → "We found N seconds" → Restore all (progress, mosaic fills) → archive. Camera usable meanwhile.
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
Account · Backup · Default look · New film format · Date stamp (on) · Also save full recordings to Photos (off) · Notifications · Fill the past · What SLIVER stores · Send feedback (mailto) · version.

### Widgets
Android: Glance 2×2 (today waiting / today ✓ thumbnail, tap → camera) and Quick Settings tile "SLIVER" → camera.

### Edge states
Empty archive (first day), camera access off (→ Settings / add from gallery), export failed (resume), phone storage full.

## Cut from v1 (don't build)
Day notes, voice notes, mood tags, streaks, paywall, multi-photo import, film-strip archive view, Pan/Parallax, Shadows/Highlights/Temperature sliders, "Try another" suggestions, auto subject reframe, 4K, dual crops, Apple/email sign-in, Firebase, push server, quiet hours, quality setting, glitch/flash transitions, subtitles, recap image card, direct YouTube API, iOS.
