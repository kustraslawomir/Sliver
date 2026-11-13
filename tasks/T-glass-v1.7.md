# T-glass-v1.7 — glass + new controls

Boards: `design/png/GlassKit.png` (the spec), `HubV17`, `CameraV17`, `BuilderV17`, `PlayerV17`, `SettingsV17` (sources in `design/src/*.dc.html`).
The owner hates the current chips, buttons, switches, toolbar icons and font sizes. This task replaces all of them. Do it in order, commit + push after each item.

## 1. Type scale
- `ui/theme/Type.kt`: `ReelogType.display 40/44 600 −0.02em`, `title 28/32 600 −0.02em`, `headline 22/28 600 −0.01em`, `body 17/24 400`, `label 15/20 500`, `caption 13/18 400`. Geist.
- Nothing under 13 sp anywhere. List rows 17 sp. Times/counts use tabular figures (`fontFeatureSettings = "tnum"`).
- Lint/detekt rule: no raw `fontSize =` outside Type.kt.
- Must survive system font 1.3× (repeat the fs13 screenshots): nothing clipped, no fixed heights on text containers.

## 2. Kit — own composables in `ui/kit/`, no Material defaults
Remove usage of M3 `Switch`, `FilterChip`/`AssistChip`, `SegmentedButton`, `TopAppBar` styles.
- `ReelogButton` primary / secondary / tertiary / destructive, 56 dp (small 44 dp, 15/500). Primary = #EDEDE8 fill, #0A0A0B text (light: #141413 / #F6F4EF). Press: scale 0.97, 100 ms. "Share film…", "Make film", "Set reminder" all use primary.
- `ReelogChip` 40 dp, radius 20, padding 16, 15/500, optional 18 dp leading icon. Off: white 6 % + 1 dp white 12 %. On: #EDEDE8 fill, dark text.
- `ReelogSegmented` 44 dp track (white 6 % + hairline), thumb #EDEDE8 inset 3 dp, labels 15/500, thumb slides 220 ms. Used for Month/Year, Sound, Date style, Video/Photo.
- Day toggles (reminders): 44 dp circles, on = #EDEDE8 fill, off = 1 dp #3A3A40 ring.
- `ReelogSwitch` 52×32: off track #2C2C30 + hairline, knob 26 dp #8E8E88; on track #EDEDE8, knob #0A0A0B. Light theme: on #141413/#F6F4EF, off #DCDAD3/white. Whole row toggles.
- `ReelogIconButton`: Lucide icons (`com.composables:icons-lucide`, KMP), 24 dp, stroke 1.75. 48 dp hit area. Plain screens: no background. Over video/camera: 44 dp regular glass circle.
- **No orange anywhere.** Switch tracks, today ring, Share button, Record today disc, "Change" link — all go. Today cell = 2 dp #EDEDE8 ring. The only colour in the UI is record red #FF3B30.

## 3. Glass
- Add Haze (`dev.chrisbanes.haze`, latest stable 1.x that supports our Compose MP version). `hazeSource` on the content behind, `hazeEffect` on the glass.
- `Glass.regular`: blur 24 dp, tint white 8 %, saturation 1.6, noise 0.08. `Glass.strong`: blur 32 dp, tint white 12 %, saturation 1.8. Both: 1 dp border, vertical gradient white 30 % → 6 %, shadow (regular 0/10/30 black 35 %, strong 0/16/40 black 45 %).
- Over bright content (camera sky, light video) add black 22 % under the white tint so white text stays readable — see CameraV17 top bar.
- Light theme: tint white 62 %, white border, shadow 0/8/24 at 12 %.
- Fallback API < 31 and battery saver: solid #1C1C1F at 92 % (light #FFFFFF 92 %), same border, no blur.
- Where: camera (record ring, top icons, date chip, Video/Photo), player and builder controls over video, day screen icons, Record today dock, undo bar, bottom sheets. NOT on settings rows, lists, plain backgrounds.

## 4. Record button — liquid glass (camera)
- Box 88 dp: existing `Modifier.liquidGlow` (unchanged) → strong glass ring, CircleShape → red core 66 dp, radialGradient #FF6F63 → #FF3B30 → #D9281F centred at (38 %, 30 %) → specular highlight: white 50 % → 0, 60 × 38 % ellipse at the top.
- Recording: core morphs to a 32 dp square, corner 9 dp (spring dampingRatio 0.7, stiffness 400), glow 1.0. Photo: white core #FFFFFF → #CFCFC8, white glow palette.
- Press: scale 0.94, 120 ms. Haptic CONTEXT_CLICK on start, CONFIRM on save.

## 5. Record today dock (project screen)
- Replaces the orange disc sitting over the film posters. Strong-glass capsule, 72 dp tall, 16 dp from the sides, 30 dp above the nav bar; calendar and films scroll under it (add bottom content padding so the last row can scroll clear).
- Left: 48 dp plain red core (no glow). Text "Record today" 17/600 + caption "Friday 13 Nov · 6 h left". Chevron right.
- Today recorded: thumbnail of today in the circle, "Today is in", "0:01 · tap to watch or replace" → day screen.
- **No animation on this screen.** Remove the Record today glow entirely — it is what burns ~60 % CPU on the idle calendar and the likely cause of the startup ANR. After the change measure on the Xiaomi: idle project screen CPU < 5 % (`adb shell top -d 2 -n 3 | grep skustra`).

## 6. Screens
Rebuild with the kit, matching the boards: project screen (HubV17), camera (CameraV17), film builder (BuilderV17: filter tiles 64×84 radius 14, selected = 2 dp #EDEDE8 ring, labels 13 sp), player (PlayerV17: ambient blurred video colour behind, glass controls, 72 dp primary pause), settings (SettingsV17: grouped cards #141416 radius 18, rows 17 sp, values 15 sp #8E8E88). Then apply the same kit to every other screen (reminders, fill calendar, from this day, day detail, onboarding, backup) — no screen may keep old controls.

## Done when
- No M3 Switch/Chip/SegmentedButton imports, no orange hex in the code, no text under 13 sp (grep in the PR description).
- Screenshots of the 5 boarded screens dark + light + font 1.3×, side by side with the boards, in `qa/v1.7/`. Placeholder clips only, never the owner's media.
- Idle CPU number from item 5 in the commit message.
- Testing only on the Xiaomi `b798509a` or emulator `tracker13`. Never the Samsung.

## 7. Record button v2 — the app's calling card (overrides item 4 where they differ)
Watch `design/motion/RecordButton.mp4`; timings and every layer value are in `design/motion/RecordButton.html` (`frame(t)`); states in `design/png/RecordButton.png` (idle · press · recording 0.6 s · saved).
Layers, bottom to top, 92 dp box:
1. `liquidGlow` (unchanged), strength 0.6 idle → 1.0 recording.
2. Strong glass disc 92 dp (blur 20, white 10 %, saturation 1.8), shadow 0/14/34 black 45 %.
3. Outer rim 0.6 dp: sweepGradient — white 95 % at the top, white 15 % at the sides, warm red 55 % at the bottom (reflected core light). Rotates ±6° with the glow phase.
4. Gap ring: 72 dp disc, black 55 % with a 1 dp inner shadow — the machined shutter look. Inner rim 0.6 dp, white 45 % at top/bottom fading to 0 at the sides.
5. Core 64 dp, base #E3261C, clipped:
   - liquid: sweepGradient #FF2D55 → #FF3B30 → #FF6A3D → #E8261C → #FF4A6A, 160 % size, blur 7 dp, rotating one full turn per 5 s (only while the camera is open);
   - subsurface: radial at the bottom (50 %, 105 %), #FF965F 90 % → 0;
   - gloss: ellipse 68 × 44 % at the top (16 %, 5 %), white 80 % → 18 % → 0;
   - edges: inner shadow 0/−3/7 #6E0000 55 %, inner highlight 0/1.5/1.5 white 45 %.
6. Progress arc on the glass ring, r = 41 dp, 3 dp white, round caps, 4 dp white glow; 0→100 % over the first 1 s of a press, then stays full while held.
7. Timer pill 30 dp above the button: black 25 % glass, red 7 dp dot, "0.6 s" 14/500 tabular. Shows only while recording.
Motion: press = core ×0.92 + whole button ×0.96, 120 ms. Recording: core → 30 dp square, corner 9 dp, spring with a little overshoot (~420 ms). Release/saved: white ring flash 8 dp → 2 dp, 480 ms, haptic CONFIRM; core springs back to the disc (460 ms easeInOutCubic).
Render on a Canvas/DrawScope (no nested Boxes per layer), one `rememberInfiniteTransition` for phase, paused when the camera is not RESUMED. Done = screenrecord on the Xiaomi next to the mp4 in `qa/v1.7/record-button.mp4`.

## 8. Day screen fixes (owner's phone screenshot, 13 Nov) — do this FIRST, it's broken now
Reference: `design/png/DayDetail.png` + the kit above. Bugs seen on device:
1. **Date stamp overlaps the title.** The "13 NOV 2026" stamp is drawn as an overlay at the same height as "Friday 13 November" and covers the next-day arrow. On the day screen don't draw the stamp at all (the title already says the date); the stamp belongs only in the rendered film and in the builder/player preview.
2. **Video isn't full-bleed.** Black bands above and below, top buttons sit on black. Video fills the whole screen edge to edge (crop to fill, behind status and nav bars, `enableEdgeToEdge`), back and mute = 44 dp glass circles over the video.
3. **Text on bright video is unreadable.** Add the scrims from the board: top 150 dp black 70 % → 0, bottom 300 dp 0 → black 85 %. "Today · 17:46" 15 sp #EDEDE8, not grey.
4. **"Sound in film" lies.** Switch is ON while the subtitle says "Off: this clip is silent in every film." Subtitle must follow the state: on = "On: this clip's sound plays in films", off = the current text. Use ReelogSwitch, the whole row toggles. Row sits in a regular-glass card (radius 18) so it reads over any video.
5. **Remove is naked red text** floating on the video. Make it a 56 dp regular-glass pill with #E5675F text, same size as Replace, side by side with a 10 dp gap. Replace stays the primary pill.
6. Prev/next day: 48 dp hit areas, 24 dp chevrons in 44 dp glass circles, vertically centred on the title block, never covered by anything.
Done = new screenshot of the day screen on the Xiaomi/emulator with a placeholder clip (bright one, to prove the scrims), in `qa/v1.7/`.

## 9. Project switcher sheet (owner's emulator screenshot, 13 Nov)
1. **Sheet is see-through.** The calendar, "Films", "Make film" and the Record button show through it, and the record glow shines through "New project". Use strong glass (blur 32 dp) **plus** a scrim over the screen behind the sheet: black 55 % dark / black 30 % light. Nothing behind may be readable. Hide the Record today dock while any sheet is open.
2. **"New project" is floating text.** Make it a 56 dp secondary pill with a plus icon, full width, 16 dp above the nav bar inset.
3. **Thumbnails are empty grey/brown squares.** 56 dp, radius 14: newest clip of the project; empty project = #1C1C1F with a 24 dp Lucide film icon #55555A.
4. **Check next to the pencil reads as two buttons.** Current project = row background white 6 %, radius 16, plus a 20 dp check at the trailing edge. Edit = the pencil as a 44 dp icon button, shown on every row, always rightmost; check goes left of it with a 4 dp gap only on the current row. Tap row = switch project and close the sheet.
5. Sizes: sheet title 28/600 (ReelogType.title), name 17/500, caption 13. Row height 72, 20 dp side padding, handle 36×4 #3A3A40.
6. Captions use the project's real numbers ("0 clips · since 13 Nov" is fine for an empty one, but check My 2026 isn't wrongly reporting 0).
