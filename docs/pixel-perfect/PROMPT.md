# Prompt — paste at the start of every UI task

You are implementing SLIVER screens to match the design **1:1**.

**Source of truth:** `design/src/*.dc.html` (and `design/png/*.png` when present). Use `design/INDEX.md` to find the file for your screen. Each file is one screen drawn at **390 × 844 CSS px. 1 CSS px = 1 dp** on a 390 dp-wide phone. Every position, size, radius, color, border, glow, font family/size/weight/line-height/letter-spacing, icon and piece of copy in those files is a requirement, not a suggestion.

For the task you were given:

1. **Inventory.** Open the design file(s). List every element on the screen with: frame (x, y, w, h in dp), style (font family, size, weight, line height, letter spacing, color, radius, border, glow) and exact copy. Inline `style="…"` holds the values; `{{holes}}` are computed in the `<script>` at the bottom — read it to get the real values per state.
2. **Build** in Compose with `SliverTheme` tokens. A value the design uses that isn't a token yet → add it to the theme, never hardcode it in the screen.
3. **Layout.** Same structure, order and spacing. Absolute `top` values assume an 844 dp screen: keep the anchoring instead — top bar measured from the status-bar inset, bottom controls from the navigation-bar inset, centered blocks centered. Widths match exactly at 390 dp; on wider screens keep the same margins (20 dp gutters).
4. **Only these ingredients:** bundled Geist / Geist Mono (Instrument Serif was removed in v1.2.2, so delete it from the project); icons from `core/icons` built from the **same SVG path data** that's in the design files (copy the `d=` paths); colors from `design/tokens.json`. Material components may be used for behaviour only — never their default look (no stock Button, Chip, Switch, Slider, TopAppBar, Snackbar visuals).
5. **Verify — mandatory.**
   - Run on an emulator with a 1080 × 2340 px, 420 dpi profile (≈ 411 dp wide; close enough — compare relative to 390 dp, the 20 dp gutters must match).
   - `tools/screenshot.sh <screen-name>` → `docs/qa/<screen-name>.png`.
   - If `design/png/<file>.png` exists: `python3 tools/compare.py docs/qa/<screen-name>.png design/png/<file>.png docs/qa/<screen-name>-compare.png` and look at the side-by-side + overlay.
   - Go through your inventory: position ±2 dp, size ±2 dp, colors exact hex, font family/size/weight exact, copy exact, icon shape exact. Fix and re-check until everything passes.
   - In your summary: the checklist with ✓/✗ per element and the screenshot paths.
6. **Never change the design.** If something can't be built exactly (platform limit), mark it ✗, explain why and propose the closest option — no silent approximations.

**Not part of the design (ignore):** fake status bars and phone frames, gradient blobs standing in for video/photos, sample data, the prototype JS that fakes playback/progress, and anything labelled "SYSTEM UI".

**Where boards and docs disagree:** v1.1/v1.2 docs win on behaviour and words (no tab bar, "clip" not "second", "Filters" not "Looks", no streaks). Everything visual stays 1:1 with the board.

**Glow is mandatory.** Read GLOW.md. Every ember glow in the design is implemented with `Modifier.glow()` and proven with crops in docs/qa/glow/. A missing glow = a failed task.
