# T-design-v1.3: apply design v1.3 → v1.3.3

Read `design/INDEX.md` sections **v1.3, v1.3.1, v1.3.2, v1.3.3** and `design/tokens.json`
(`themeLight`, `glow`). Measure from `design/png/`; the `.dc.html` files are the source.

## Do
1. **Home first.** The app opens on the Project screen. A fixed "Record today" button opens the
   camera as a full-screen modal with ✕. Remove the project and "Today" chips from the camera.
2. **Camera.** Photo/Video = two words with a dot (no pill). Add and Flip are identical controls.
   Record glow per `glow.record`. Golden hour line in the top bar (v1.3.1).
3. **Typography.** In the base TextStyle: `PlatformTextStyle(includeFontPadding = false)` and
   `LineHeightStyle(Center, Trim.Both)`. No Geist Mono and no uppercase labels anywhere
   (the date stamp burned into the video is the only exception).
4. **Year view = the light of your year** (v1.3.1). Store `luma` + `tone` per clip at save time.
   Draw with one Canvas.
5. **Daily reminder screen** (v1.3.2). Remove the old Notifications screen.
6. **Film player** (v1.3.3): icon buttons instead of "Play / Mute / Full screen" text,
   and remove the date overlay on the player (it doubles the burned-in stamp).
7. **Light theme:** two ColorSchemes from the tokens, Settings → Appearance (System/Light/Dark).
   Camera, recording, edit preview and the player stay dark. No glow in light.
8. **Bugs from the device:** filled day rendering as an empty box (needs thumbnail + number),
   empty film poster, missing project thumbnail in the projects sheet, `guest@example.com`.

## Done when
- Screenshots of every changed screen, **dark and light**, next to `design/png` via
  `tools/compare.py`, saved in `docs/qa/v1.3/`. Plus the same at font scale 1.3.
- The reminder fires at the set time on the device, skips when today has a clip,
  and the Record action opens the camera.
- Tests and lint green. A list of anything that still doesn't match: don't hide it.
