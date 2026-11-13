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
