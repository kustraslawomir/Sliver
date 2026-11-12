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

## 3. Picker UX
Filter row in Edit clip / Film builder: thumbnails rendered from the current clip frame per filter (cached),
intensity slider, Grain amount slider. Disposable shown first.

## 4. Tests + checks
Unit tests: LUT generator output for known colours, deterministic shader params per clip id. Roborazzi goldens
for the filter picker (synthetic frames only). Device check on Xiaomi/emulator with seeded synthetic clips:
screenshots of each filter (dark UI) in docs/qa/looks-film/. Never use the owner's real media.

## Done when
Build + tests + lint + designLint + verifyRoborazziDebug green; perf numbers in HANDOFF; SPEC updated.
