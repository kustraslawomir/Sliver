# T-ui-audit-v1.8 — owner's phone review, 14 Nov

Owner reviewed the current build on his phone. Verdict: the kit is in, but whole components were never designed (dialogs, date/time pickers, music list, media picker, camera options) and several screens have bugs. Board for the new components: `design/png/ComponentsV18.png` (source `design/src/ComponentsV18.dc.html`). Record button: `tasks/T-record-button-v3.md` is approved by the owner — do it if not done.
Work top to bottom, commit + push after each section. Tests: Xiaomi b798509a / emulator tracker13 only, never the Samsung. QA screenshots with placeholder clips only, never the owner's media.

## 0. Glass — tone it down, don't add more
Glass isn't overused; it reads as **grey outlined circles** because of the 1 dp hairline on everything, which looks like a wireframe.
- Glass keeps its edge ONLY on: sheets, dialogs, the Record today dock. Icon buttons over video/camera: no border, tint only (black 22 % + blur where possible).
- Secondary buttons on plain screens (Make film, Browse all photos, Remove on the day screen): filled #1C1C1F, **no outline**. Light theme #E9E7E1.
- No new glass anywhere else.

## 1. Rule for every switch subtitle (systemic bug, 3rd time)
"Also save full recordings to Photos" is ON while the subtitle says "Off: only the chosen clip is kept…" (SettingsScreen.kt:102). Same bug was on the day screen.
- A subtitle describes **what the switch does**, never one state. E.g. "Keeps the whole recording in Photos, not just the 1 s clip." / "Press a volume key on the camera to start and stop."
- If a subtitle must depend on the state, it is computed from the state. Add a unit test that renders every settings switch in both states and checks no subtitle starts with "On:"/"Off:" contradicting the state.

## 2. Project screen (hub)
1. "Syncing" takes a whole row + ~100 dp of empty space before the month. Move sync status into the subtitle line as a small icon + word ("155 clips · since 1 April · Story 9:16 · ⟳ Syncing"), hide it when idle. Month header 24 dp under the subtitle.
2. Hierarchy is upside down: "October" (≈40 sp) is bigger than the project name. Month = ReelogType.headline 22 sp, project name = title 28 sp.
3. "+30 / +24 / +25" badges on cells: nobody knows what they mean. If it's "more items that day", show it only in the day screen, not on the calendar. Remove the badge from cells.
4. Films: three posters are **identical** (same cover frame, title "Lena", same range). Poster = the film's own cover frame (frame at 10 % of the film, or the chosen filter applied), title = the film's name, caption = range · length · filter. If they really are 3 renders of the same range, show them as versions: "Lena · v3 · Kiosk".
5. The film captions ("1 Apr – 13 Nov 2026") are cut by the nav bar and covered by the dock: add bottom content padding = dock height + 16 + nav inset.
6. Month/Year toggle from HubV17 is missing — put it back (segmented, 44 dp) or tell the owner why it was dropped in the commit message.

## 3. Camera
1. Record button → T-record-button-v3 (approved).
2. Remove the "⋯" sheet that holds only "Grid". Grid becomes a toggle icon (#) in the top bar next to flash (board, last phone). One tap, state shown by a filled vs outline icon, remembered as now.
3. Add / Flip: drop the text labels under the icons (nothing else in the app labels icon buttons); 52 dp glass circles.
4. Zoom chips: all show the "×" (0.6× 1× 2× 3×) or none; 15/500.

## 4. Film builder
1. Filter names: tiles say "1 Kiosk", "2 Kiosk", "3 Plus", "4", "5" — numbers and missing names. Use the LookPresets names only (Natural/None, Kiosk, Afterhours, Silver, Sunday, Tape, Halo). Never a number, never an empty label.
2. Paused state: plain "Paused" text over a face. Replace with a 56 dp glass pause/play circle in the centre that fades out 600 ms after play.
3. Tick marks row above the scrubber has no meaning to the user. Either label them (month initials) or remove them.
4. Make film turns into a grey disabled button with no reason when Music has no track picked. Keep it enabled-looking but show the hint above it: "Pick a track or switch to Original" and scroll to the list on tap.
5. Date style segmented (Film/Plain/Camcorder) is visible while "Date on screen" is off — hide it (animate height) when off.
6. Make film must never be cut by the nav bar: sticky bottom bar with the button, content scrolls under it.

## 5. Music list (board phone 4) — align with the audio layers you built in 4d39861
The current list is raw file names ("sub_clair-suspense-628719 _1_.mp3") with a speaker icon and no selection state. This list is the picker for a track/layer:
- Row 64 dp: 40 dp play/pause preview button (plays the track from its start, stops when another starts or the sheet closes), cleaned title, caption "2:31 · fills the whole film" / "1:58 · loops 2×" computed against the film length, trailing radio (22 dp).
- Title cleaning: drop the extension, replace _ and - with spaces, drop trailing ids/numbers and "(1)"/"_1_", title-case. Owner can rename (hold row → Rename / Remove).
- Selected row: background white 6 %, radius 16, expands with the volume slider.
- Last row: "+ Add music from files". Empty state: one line "No music yet" + that row.
- Audio layers editor (your 4d39861) keeps its timeline; adding a layer opens this list.

## 6. Pick media for a day ("From this day", board phone 5)
1. 2 big tiles per row with a huge empty screen → 3 columns, 3:4 tiles, radius 12.
2. Two identical "19:50" tiles: a Live/motion photo shown twice. Show one tile with a "Live" badge.
3. Videos get a duration badge ("0:12"), top-right.
4. Section "Recorded in Reelog" first (full recordings kept by T-glass item 10), then "From your camera · N".
5. Bottom bar: "Show saved and received too" switch + "Browse all photos" filled secondary button.

## 7. Dialogs, pickers, text input — no system Material dialogs
- **Dialog** (board phone 1): strong glass card, radius 28, 24 dp padding, title 22/600, body 15 #B9B9B3, buttons stacked full width: destructive filled #E5675F with dark text, Cancel as text button. Scrim black 55 %. Use for: delete project, remove day (if not undo-able), sign out, delete film, discard edits.
- **Date picker** (board phone 2): bottom sheet with the app's own month grid (44 dp day circles, selected = paper fill, future/out-of-range #55555A and disabled), month arrows, quick chips, primary button that names the result ("Fill from 21 October"). Replace DatePickerSheet styling with this; reuse for project start date.
- **Time picker** (board phone 3): replace `SystemTimePickerDialog` with a sheet: two wheels (hours, minutes in 5-min steps; long-press for 1-min), selected row band white 7 %, 40/600 tabular; preset chips (Golden hour, 08:00, 20:00); primary "Set 15:55". Respects 24 h setting.
- **Text input** (rename project, rename track): sheet with a 56 dp field, radius 16, #111113 fill, 1 dp #3A3A40 (focused #EDEDE8), clear button, primary Save; keyboard opens immediately.
- **Overflow menu** (⋯ on the hub) is fine. Rename "Change orientation" → "Film format". "Fill the calendar" stays in the menu and is removed from Settings (one place).

## 8. Settings
- Fix subtitles per §1.
- Remove "Fill the calendar" row (lives in the project menu).
- "Daily reminder · Golden hour, every day" fine.

## Done
Screenshots (placeholder media) of: hub, camera, builder (music on/off, date on/off), music list, media picker, each dialog/picker, settings — dark + light — in `qa/v1.8/`. Commit message lists any item you skipped and why.
