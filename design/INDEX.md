# Which design file is the spec for which screen

Newest board wins. Boards marked *superseded* are kept only for parts not redrawn yet.

| Screen | Design file(s) | Notes |
|---|---|---|
| Design system: tokens, components | `DesignSystem.dc.html`, `tokens.json` | buttons, chips, sliders, sheets, toasts, calendar cells, sliver frame |
| Camera | `CameraV2.dc.html`, `CameraV2Land.dc.html` | supersedes `Camera.dc.html`, `CameraLandscape.dc.html`, `CameraDone.dc.html` |
| Edit clip | `EditClipV2.dc.html` (`variant` ready / loading) | supersedes `Trim.dc.html`. Take ONLY the short-clip chips and the reframe band from `Trim.dc.html`, nothing else |
| Photo clip | `PhotoTrim.dc.html` | button text = "Save clip"; motion chips Still (default) / Slow zoom |
| Filters editor | `Looks.dc.html`, `LooksAdjust.dc.html`, `Frame.dc.html` | title "Filters"; sliders: Sharpness, Contrast, Grain, Vignette, Fade, Saturation |
| Projects sheet + new project | `ProjectsV2.dc.html` | |
| Onboarding | `OnbV2.dc.html` (`step` 0–2) | supersedes `Onboarding.dc.html` |
| Media picker | none — Android system Photo Picker | `PickerV2.dc.html` only shows context, don't build it |
| Project screen (calendar + films) | `ProjectHub.dc.html` (month, ⋯ menu, add-to-empty-day sheet) · `ProjectHubYear.dc.html` (year + daily-nudge card) | Opened from the calendar chip on the camera. Header: chevron-down back, project name → project switcher, settings, ⋯. No tab bar. Archive*/Films.dc.html are old v1.0 — do not use. |
| Clip detail + share | `DayDetail.dc.html`, `DayShare.dc.html` | no mood chips |
| Add to an empty day | the sheet in `ProjectHub.dc.html` (tap an empty day) | `ArchiveAdd.dc.html` is old, don't use it |
| Fill empty days | `Backfill.dc.html` | after picking in the system picker: results mosaic → filling → done. `BackfillLimited.dc.html` is obsolete (system picker has no 'limited access'), so don't build it |
| Film builder / export / player | `FilmBuilder.dc.html`, `Export.dc.html`, `ExportPremiere.dc.html`, `FilmPlayer.dc.html` | |
| Year in Seconds | `YearInSeconds.dc.html` | |
| Back up / Drive / restore / account | `SaveSheet.dc.html`, `DriveConnect.dc.html`, `Backup.dc.html`, `BackupStates.dc.html`, `Restore.dc.html`, `AccountSettings.dc.html` | Google only |
| Notifications settings + look | `Notifications.dc.html`, `NotifDenied.dc.html`, `NotifBoard.dc.html` | |
| Settings | `Settings.dc.html` | |
| Splash, edge states, widgets | `Splash.dc.html`, `EdgeStates.dc.html`, `Widgets.dc.html` | |
| Before/after explanation | `Changes.dc.html` | reading only |

`design/png/` contains rendered PNGs of every board. **Measure from these, not from the HTML.** Phone screens are @3x (1170×2532): px ÷ 3 = dp. Big boards are @1x. `tools/compare.py` takes these directly.

## Added in v1.2

| Item | Design | Notes |
|---|---|---|
| App icon (launcher, themed, splash, notification, Play) | `AppIcon.dc.html` | Ready resources in `android-res/`. Copy as described in `android-res/README.md`. Do not redraw. |
| Record button | **Option D "Ember"**: `CameraV2.dc.html` (idle, tap to record), `CameraV2Rec.dc.html` (recording) · options board `RecordButtons.dc.html` | Own composable `RecordButton(state)`. Video idle: 72dp ember disc in an 80dp hit area, soft glow (drawBehind radial gradient, no blur). Photo: white disc. Recording: same disc + 24dp dark stop square, slow 1.6 s breathing glow, OFF when animations are disabled in system settings. While recording, hide ADD, flip, tools and the mode switch. Timer pill at the top: grey until 1 s, then ember + "CLIP OK" and ONE haptic tick (HapticFeedbackConstants.CONFIRM). Tap again = stop → Edit clip. |
| Logo / wordmark | `Logo.dc.html` | Files in `brand/` (SVG + VectorDrawable). Not shown on app screens. If you need it anywhere, use `@drawable/sliver_wordmark`, never Text(). |

## v1.2.1 — save flow + buttons

| Item | Design | Notes |
|---|---|---|
| After "Save clip" | `ProjectHubSaved.dc.html` | Save → navigate to the Project screen (pop the edit screen, don't keep it in the back stack). Today's cell pops in once (scale 0.6→1, 420ms) with a 2dp ember ring; count +1. **No toast or snackbar anywhere in the save flow.** Back from here → Camera. Undo = tap today → Day detail → Replace/Delete. |
| Buttons & chips | all | Text is ONE line: `maxLines = 1`, `softWrap = false`, width = wrap content, height 48dp (chips 32–40dp). Never set a fixed width on a button with text. If buttons don't fit side by side (large font scale), stack them full-width in a Column. Don't shrink the text. Test at font scale 1.3. |

## v1.2.2: fonts, camera top, focus, glow, copy

| Item | Design | Notes |
|---|---|---|
| **Fonts** | all boards, `tokens.json` | **Instrument Serif is gone.** Every title uses Geist 600 with negative tracking (display XL 52/56 −3%, L 36/40 −2.5%, M 24/30 −2%). Remove the serif font file from the project. Labels stay Geist Mono. |
| Camera top | `CameraV2.dc.html`, `CameraV2Land.dc.html` | 150dp top scrim (black 70% → 0) so chips read on any video. Chips: 36dp pill, black 55% + 12dp blur behind (RenderEffect on API 31+, plain 70% black below that), Geist 15/500 white, no border. Project chip has no format text. Calendar chip says "Today". |
| Tap to focus | `CameraV2.dc.html` (tap the viewfinder) | Tap: 88dp ring shrinks to 64dp in 220ms with a center dot, then fades to 35% after 1.2 s. CameraX `FocusMeteringAction` (AF+AE, auto-cancel 3 s) from `PreviewView.meteringPointFactory`. Hold 600ms: AE/AF lock, ring + dot turn ember with `glow.lock`, "AE/AF LOCK" pill 54dp above, no auto-cancel until the next tap. Not while recording. |
| Glow | `GLOW.md` | Mandatory. See GLOW.md. |
| Copy | all | The word "second" no longer exists in the UI: it's "clip" ("Save clip", "Share this clip", "Your year in clips"). "Looks" → "Filters". If you find "second(s)" in UI copy anywhere, it's a bug (durations like "1 s" are fine). |
| No toasts | all | Removed from Trim/PhotoTrim/Camera. Nothing may float over the UI after an action. Show the result in place (ProjectHubSaved). |

## v1.2.3: after the first build review

| Item | Design | Notes |
|---|---|---|
| Fonts | `fonts/` | Geist + Geist Mono .ttf files are in the pack now. Follow fonts/README.md. A system-font fallback is a bug. |
| Camera tools | `CameraV2.dc.html`, `CameraV2Land.dc.html` | Matches what was built: only Light (icon) + ⋯ (menu → Grid on/off). No Timer, no Filter on the camera. The flip button label is "FLIP" ("BACK" read like navigation). |
| Chip blur | `CameraV2.dc.html` | The plain dark fallback is accepted: #000000 at **70%**, not lighter. Don't add a blur library. |
| Share image | `DayShare.dc.html` | The wordmark on the share image = `R.drawable.sliver_wordmark` drawn onto the bitmap (`drawable.setBounds(); drawable.draw(canvas)`). Never `drawText("SLIVER")`. |
| Landscape | `CameraV2Land.dc.html` | Not a screenshot task. The owner holds the phone sideways and records 2 s; then you pull the file (`adb pull`) and prove with `ffprobe` (or MediaMetadataRetriever) that width > height after rotation and the preview in Edit clip is 16:9. Post the ffprobe output in docs/qa/landscape.md. Do the same for upside-down landscape. |
| Filters | — | Parked in the app. Edit clip/Day detail boards still show "Filters": hide those entries while parked, don't leave dead buttons. |

## v1.3: home first, camera as a modal, light theme

| Item | Design | Notes |
|---|---|---|
| **Start screen** | `ProjectHub.dc.html` | The app opens on the Project screen, not on the camera. It's home: no back arrow. Project name + chevron = switch project. A 72dp ember **Record today** button sits fixed at the bottom center (label under it). When today has a clip, it's an outlined ✓ "Done for today" button that opens the day. No tab bar: two places don't need one. Notification / widget "record" deep links still open the camera directly. |
| Camera | `CameraV2.dc.html`, `CameraV2Rec.dc.html` | Full-screen modal over home. Top-left ✕ closes it (back gesture too). Top-right: Light + ⋯ (Grid). **No "My 2026" chip and no "Today" chip**: the camera only films, and project switching lives on home. Save → back to home with today's cell popping in (ProjectHubSaved). |
| Photo / Video | `CameraV2.dc.html` | Two plain words, no pill, no segmented control. Active = white + 4dp dot under it, inactive = white 60%. Geist 15/500. |
| Add & Flip | `CameraV2.dc.html` | Identical controls: 52dp circle, white 10% fill, 22dp icon, label under it in Geist 12/500, sentence case ("Add", "Flip"). Add is an icon (we have no library access to show a thumbnail). |
| Record glow | `tokens.json → glow.record` | Toned down: one layer, `0 0 12px #FF8A3D4D`. Breathing peaks at `0 0 18px #FF8A3D73`. |
| Month / Year | `ProjectHub.dc.html` | Text tabs with a 2dp underline. No pill. |
| Labels | all | No Geist Mono, no uppercase labels anywhere in the UI. Numbers use Geist with tabular figures (`fontFeatureSettings = "tnum"`). The only uppercase text left is the date stamp burned into the video. |
| Settings format | `Settings.dc.html` | "Default film format: Cinema 16:9 >" is a normal row that opens a choice. No segmented control. |
| **Text vertical offset on chips/buttons** | all | It's Android font padding, not a design margin. Set in the base TextStyle: `platformStyle = PlatformTextStyle(includeFontPadding = false)` and `lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)`. Apply it once in the Typography, not per component. |
| **Light theme** | `*L.dc.html` (ProjectHubL, DayDetailL, ProjectsV2L, SettingsL, EditClipV2L) + `tokens.json → themeLight` | Settings → Appearance: System (default) / Light / Dark. Implement as two `ColorScheme`s from the token maps, so no screen has a hard-coded color. Camera, recording, the Edit clip preview area and the player **stay dark in both themes**. No glow in light theme. Ember text on light uses `emberOnLight` #C2540F (contrast); ember fills keep #FF8A3D with dark text. |

## v1.3.1: light of the year + golden hour

| Item | Design | Notes |
|---|---|---|
| **The light of your year** | `ProjectHubYear.dc.html` (dark), `ProjectHubYearL.dc.html` (light) | Replaces the year heatmap in the Year tab. One bar per day from project start to today. **Height = average brightness (luma) of the clip, colour = its dominant colour.** Compute both ONCE at save time from 3 frames (start/middle/end, scaled to 32×18 px), store as two columns on the clip row (`luma: Float 0..1`, `tone: Int ARGB`). The chart then needs no video decoding. An empty day = a 2dp grey stub, today = an ember dot on the baseline. Tap a bar → the day card below updates (thumbnail, date, "The brightest day of <month>" if it is). Tap a month label → Month tab on that month. Draw it with one `Canvas`, not 365 composables. |
| **Golden hour** | `CameraV2.dc.html` (`golden` = soon), `CameraV2Golden.dc.html` (now) | Top bar of the camera, between ✕ and the tools: sun icon + "Golden hour in 38 min" (white 85%), or "Golden hour now · 24 min left" (#FFC58A) during it. Golden hour = the sun's elevation between −4° and +6°, morning and evening. Show the next one only if it starts within 3 h; otherwise show nothing. Computed offline (NOAA solar position formula, ~60 lines, no library) from the **coarse** location (`ACCESS_COARSE_LOCATION`, last known location only, no GPS fix). Ask for location once, from a row in Settings ("Show golden hour"), never on camera start. Denied/off = the line is simply absent. Hidden while recording. |

## v1.3.2: daily reminder settings

| Item | Design | Notes |
|---|---|---|
| Daily reminder | `Reminders.dc.html` (+ `RemindersTime`, `RemindersOff`, `RemindersL`) | Replaces the old `Notifications.dc.html` (don't build that one). Opened from Settings → "Daily reminder" and from the nudge card on the Project screen. |
| Settings stored (DataStore) | | `enabled` (default on after the user accepts the permission), `time` (default 20:00), `days` (7 booleans, default all), `skipIfDone` (default on), `lastCall` (default off, fixed at 22:30, only if the day is still empty), `followGoldenHour` (default off; time = 20 min before evening golden hour, from the same solar code as the camera), `projects` (open projects only; default: all open). |
| Scheduling | | One `AlarmManager.setWindow` per next reminder (10 min window). No exact alarms, so no `SCHEDULE_EXACT_ALARM`. Reschedule on boot, time-zone change, settings change, and after each fire. With `skipIfDone`, check at fire time whether today has a clip; if yes, post nothing. Max 2 notifications a day. |
| Permission | `RemindersOff.dc.html` | Ask for `POST_NOTIFICATIONS` only when the user turns the reminder on (or taps "Set reminder" on the nudge card), never at app start. If Android notifications are off: the yellow row with "Turn on" → `Settings.ACTION_APP_NOTIFICATION_SETTINGS`. |
| The notification | preview at the bottom of `Reminders` | Title "Today's clip". Text "<Weekday> is still empty. A few seconds is enough." Large icon = yesterday's clip thumbnail (if any). Actions: **Record** (opens the camera directly) and **Tomorrow** (just dismisses; nothing is counted, no guilt copy). Small icon `ic_stat_sliver`. Channel "Daily reminder", default importance. |
| Time presets sheet | `RemindersTime.dc.html` | 6 presets + "Other time…" → Material 3 `TimePicker` dialog, 24h/12h from the system setting. |

## v1.3.3: film player

| Item | Design | Notes |
|---|---|---|
| Player controls | `FilmPlayer.dc.html` | **Icons, not words.** Three round buttons in one row: sound (48dp, outline, speaker / speaker-off icon), play/pause (72dp, filled light, the main one), full screen (48dp, outline, corners icon). Labels go to `contentDescription` only. The words "Play / Mute / Full screen" must not be visible. |
| Date on the video | `FilmPlayer.dc.html` | **No date overlay on the player.** The date stamp is already burned into each clip, so drawing it again on top doubles it (bug seen on device: "11 Nov 2026" small + "11 NOV 2026" big). |
| Scrubber | `FilmPlayer.dc.html` | One tick per clip, ember progress line, light head 3×20dp. Times under it: Geist tabular, "0:12" and "-0:17". |
| Actions | `FilmPlayer.dc.html` | "Share film…" full width (ember). Under it, side by side: "Save to Photos" and "Edit film" (outline). A one-line file info at the bottom in muted text. |

## v1.4: accent = mono + red dot (supersedes every ember/orange rule above)

| Item | Design | Notes |
|---|---|---|
| Accent | all boards in `design/src`, `design/png`, `tokens.json → accent` | **No accent colour in the UI.** Primary buttons, selected states, switches, tabs, the today ring: dark theme = #EDEDE8 with #0A0A0B text, light theme = #141413 with #F6F4EF text. Colour on screen comes only from the clips (thumbnails, the light of the year). |
| Red = recording, nothing else | `CameraV2`, `CameraV2Rec`, `ProjectHub` | #FF3B30 for: the record button, "Record today", the recording dot and the timer after 1 s. Never for anything else. |
| Destructive | all | Red **text** only (dark #E5675F, light #C0392B). Never a filled red button, so delete can't look like record. |
| Glow | `tokens.json → glow` | Removed everywhere. `GLOW.md` and `Modifier.glow()` are superseded: delete the usages. Recording "breathes" with opacity 1 → 0.78 over 1.6 s (off when animations are disabled). |
| Switches | `Settings`, `Reminders` | On = filled track in the primary colour, knob with a 2dp ring in the background colour so it stays visible. |
| Brand | `brand/`, `android-res/` | Unchanged for now: the icon/logo slit stays warm. That's the one place colour stays. |
| Player in light theme | `FilmPlayer.dc.html` | Reminder from v1.3: the player, camera, recording and edit preview are dark in BOTH themes. Use a forced dark surface for these screens, not `BgBase`. |

## v1.4.1 owner decisions (2026-11-12)

| Item | Design | Notes |
|---|---|---|
| Film = whole project | `FilmBuilder.dc.html` | **No RANGE section** (Week / Month / Year / Custom and the custom date sheet are removed). A film always uses every filled day of its project. Poster, library and premiere subtitle show the project span: "10 Jan – 12 Nov 2026 · 129 clips". |
| Delete film | `FilmPlayer.dc.html` | "Delete film" as red text only, in the player and in a long-press sheet on a poster. Acts at once, toast "Film deleted · Undo" for 6 s, no confirm dialog. |
| Camera start state | `CameraV2.dc.html` | The camera opens on Video / back lens / 1x / light off every time. Mode, lens and light are not remembered. |
| Filters are film-level only | `FilmBuilder.dc.html` | Filters live only in the Film builder (None + Kiosk, Afterhours, Silver, Sunday, Tape, Halo, Intensity and Grain sliders). No filter control on clips, camera, Edit clip or Day detail. |
