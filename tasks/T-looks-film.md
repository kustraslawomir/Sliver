# T-looks-film: un-park filters, add a "Film" family led by a Disposable-camera look (owner request, 2026-11-12)

Owner likes the grainy vibe of disposable cameras and dark/black film looks (apps like Prequel). Build OUR OWN
looks with the techniques below. Never copy LUTs, textures or assets from other apps.

Product rules still apply: filters are never burned into stored clips, only into previews and exports
(CLAUDE.md). Wording: "Filters", never "Looks" (docs/TERMS.md). Mono UI, red only for recording.

## 0. Un-park
`LookEffects.PARKED` → false. Check performance on the Xiaomi: preview ≥ 30 fps with the heaviest filter at
1080p; export time per 1 s clip measured and noted in HANDOFF.

## 1. Shader upgrades (`LookShader.kt`, one GLSL pass where possible)
- **Film grain**: per-frame seed; grain size ~1.5–2 px at 1080p (scale with output height); luminance-weighted
  (strongest in shadows/mids, weak in highlights); colour grain with chroma amount param (0 for B&W).
- **Halation**: threshold highlights → blur (separable, reduced-resolution pass OK) → add back with red-orange tint.
- **Flash falloff** (disposable): radial exposure boost in the centre + falloff to edges, warm tint in the
  boosted area; amount param.
- **Soft plastic lens**: edge blur ramp + slight barrel distortion + chromatic aberration at the edges.
- **Gate weave**: sub-pixel random offset per frame (±0.5 px), plus optional exposure flicker ±1.5 %.
- **Light leak**: procedural soft orange/red gradient blob drifting slowly from one edge, low probability
  per clip (deterministic per clip id so preview = export).
- All params deterministic from (clip id, frame time) so preview and export match.

## 2. New presets (LUT + shader params), names in docs/TERMS.md style
- **Disposable**: Kodak-Gold-like warm LUT (warm yellows/reds, cyan-green shadows, lifted blacks 6–8 %),
  flash falloff, heavy colour grain, soft lens, strong vignette, occasional light leak; date stamp default
  style "LED" (orange segmented digits, bottom-right) when the date stamp is on.
- **Dark**: low-key; crushed blacks, mids −0.3 EV, cool green-teal shadows, saturation −30, heavy vignette,
  dense grain.
- **Black**: B&W, channel mix R 0.5 / G 0.4 / B 0.1, high contrast S-curve with deep blacks, coarse
  mono grain (Tri-X feel), light halation.
- **Portra**: warm, soft contrast, gentle skin tones, fine grain.
- **Night 800**: tungsten-balanced, blue shadows, strong halation around lights, medium grain.
Keep the existing presets that still make sense; drop or rename any that duplicate these (note in HANDOFF).
Generate LUTs with the Kotlin LUT generator (commit 0c90b44) from documented curves/matrices, 33³ `.cube`.

## 3. Where filters live: the FILM, not single clips (owner decision, 2026-11-12)
- Filters are chosen only in the **Film builder** and apply to the whole film (preview + render/export).
  The builder's existing single-look option becomes the "Filter" section: horizontal row of thumbnails
  rendered from a frame of the project's film with each filter (cached), intensity slider, Grain slider.
  Disposable first. Saved in the film definition (synced later with T-sync-two-devices).
- **Remove filters from single clips**: no filter control in Edit clip / Photo clip / Day detail, and no
  "Change look"/filter entry on a day. Clips play and save clean. Existing per-day `look` values are ignored
  for playback and render (keep the DB column readable; migration not needed); the film's filter is the only
  one applied. Remove the camera filter swipe if it still exists.
- Single-clip export/share (if any) stays unfiltered.
- docs/SPEC.md and docs/TERMS.md: update the Filters/Films sections accordingly; design/INDEX.md: add to
  "v1.4.1 owner decisions": filters are film-level only.

## 4. Tests + checks
Unit tests: LUT generator output for known colours, deterministic shader params per clip id. Roborazzi goldens
for the filter picker (synthetic frames only). Device check on Xiaomi/emulator with seeded synthetic clips:
screenshots of each filter (dark UI) in docs/qa/looks-film/. Never use the owner's real media.

## Done when
Build + tests + lint + designLint + verifyRoborazziDebug green; perf numbers in HANDOFF; SPEC updated.

## 5. Tuning sheet for the owner (no references from other apps)
Owner won't make reference pairs, and we must not sample other apps' filters. Instead:
- `tools/looks/sheet.py` (or a Gradle/JVM test task) renders a contact sheet: 4 synthetic test frames
  (warm skin-tone gradient portrait stand-in, green foliage pattern, blue sky gradient, night lights with
  bright points) × each filter in 3 labelled variants (A/B/C differing in temperature, black level and grain).
- Output `docs/qa/looks-film/sheet.png` (one image, readable on a phone). The owner answers e.g.
  "Dark B, more grain, Disposable warmer"; the chosen variant's params become the preset.
