# T-design-v1.4-mono: accent = mono + red dot, plus v1.3 leftovers

Read `design/INDEX.md` section **v1.4** (it overrides every ember/orange/glow rule above it)
and `design/tokens.json → accent`. Measure from `design/png/`.

## 1. Mono + red dot
- Remove the ember accent everywhere. Primary fill / selected / switch-on / tabs / today ring:
  dark `#EDEDE8` on `#0A0A0B` text, light `#141413` with `#F6F4EF` text.
- Red `#FF3B30` ONLY for: record button, "Record today", recording dot, timer after 1 s.
- Destructive actions: red text only, never a filled red button.
- Delete `Modifier.glow()` usages and the glow tokens. Recording breathes with opacity
  1 → 0.78 / 1.6 s (static when animations are off).
- Brand (icon, splash, logo) stays as is.

## 2. Leftovers from T-design-v1.3 (see HANDOFF.md)
- Player, camera, recording, edit preview: **forced dark** in both themes (not `BgBase`).
- Player: the one-line file info under the buttons.
- On the Xiaomi or the emulator (NOT the Samsung, see "Test devices" below): filled day rendering as an empty box, and the missing project thumbnail
  in the projects sheet. Reproduce with real clips (record 2 days, or use the debug seeder),
  fix, and screenshot.
- Reminder: test on the device: fires at the set time, skips when today has a clip, Record opens the camera.

## Test devices
- Day-to-day testing: the **Xiaomi** over USB, or an **emulator** (Pixel, API 36). Pick whichever is attached.
- The **Samsung** is only for the final check before a release, and only when the owner asks. Never install on it, wipe it or change its settings during normal work.

## Done when
- `docs/qa/v1.4/`: every changed screen, dark AND light, next to `design/png` (tools/compare.py),
  plus font scale 1.3.
- `grep -rn "FF8A3D\|glow(" composeApp/` returns nothing.
- Tests and lint green. List anything that still doesn't match: don't hide it.
