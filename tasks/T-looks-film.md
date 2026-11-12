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

## 2. New presets — our own family (names and concepts are original; owner approved the direction)
Inspiration is only the general genre (grainy analog / disposable / dark / B&W). Order in the picker:
- **Kiosk** (disposable camera from a kiosk): warm cheap-film LUT (warm yellows/reds, cyan-green shadows,
  lifted blacks 6–8 %), flash falloff, heavy colour grain, soft plastic lens, strong vignette, occasional
  light leak; date stamp default style "LED" (orange segmented digits, bottom-right) when the stamp is on.
- **Afterhours** (dark, night, city): low-key; crushed blacks, mids −0.3 EV, teal-green shadows,
  saturation −30, halation around lights, dense grain, heavy vignette.
- **Silver** (B&W silver film): channel mix R 0.5 / G 0.4 / B 0.1, high-contrast S-curve, deep blacks,
  coarse mono grain, light halation.
- **Sunday** (soft warm memory): faded blacks, warm highlights, gentle skin, fine grain, low contrast.
- **Tape** (our VHS, evolves the existing VHS 94): chroma bleed, faint scanlines, slight weave,
  date in a camcorder-style stamp variant.
- **Halo** (dreamy night lights): bloom + halation, cool shadows, soft image, medium grain.
Keep the existing presets that still make sense; drop or rename any that duplicate these (note in HANDOFF).
Generate LUTs with the Kotlin LUT generator (commit 0c90b44) from documented curves/matrices, 33³ `.cube`.

## 3. Where filters live: the FILM, not single clips (owner decision, 2026-11-12)
- Filters are chosen only in the **Film builder** and apply to the whole film (preview + render/export).
  The builder's existing single-look option becomes the "Filter" section: horizontal row of thumbnails
  rendered from a frame of the project's film with each filter (cached), intensity slider, Grain slider.
  Kiosk first. Saved in the film definition (synced later with T-sync-two-devices).
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

## 6. Big candidate pool (owner: "plan a lot, I'll throw away ~99%")
Build a parametric look model (LUT generated from: white balance shift, per-channel tone curves, black lift,
shoulder roll-off, saturation per hue band, shadow/highlight tint, B&W channel mix) + shader params (grain
size/amount/chroma, halation, bloom, vignette, flash falloff, softness, weave, leak probability). Each
candidate is just a parameter set in `looks/candidates.json`, so adding/removing one costs nothing.
Names in the app are OUR OWN; film-stock names below are only internal notes describing the general,
publicly known character of that kind of film (never shown in UI, never used as filter names).

Colour negative, consumer (cheap, warm, punchy):
1. Kiosk 400 — consumer 400 colour neg: warm yellow-orange, punchy reds, cyan shadows, visible grain.
2. Kiosk 200 — consumer 200 "gold" style: warmer, golden skin, softer grain.
3. Plus 200 — budget 200: lower saturation, slightly muddy greens, warm cast, coarse grain.
4. Superstore — Japanese consumer 400 feel: cooler greens, magenta-ish skin, green-cyan shadows.
5. Corner 100 — budget 100/200 with green-teal shadows and pastel highlights.
Colour negative, pro (clean, skin-friendly):
6. Sunday 160 — soft portrait neg: low contrast, gentle peach skin, pastel.
7. Sunday 400 — portrait 400: a bit more contrast and warmth, fine grain.
8. Vivid 100 — fine-grain landscape neg: saturated, crisp, deep blues.
9. Studio 400 — cool-neutral pro neg: minty greens, soft magenta.
Cinema / tungsten:
10. Afterhours — tungsten night stock: blue shadows, red halation around lights, medium grain.
11. Afterhours Warm — same, white balance corrected warmer for streetlights.
12. Reel 250 — daylight cinema neg: low contrast, wide latitude, filmic roll-off.
13. Bleach — bleach-bypass: desaturated, high contrast, silvery highlights.
Slide film (reversal):
14. Slide 100 — vivid transparency: high saturation, deep blacks, punchy blues.
15. Cross — cross-processed slide: yellow-green highlights, cyan shadows, high contrast.
16. Slide Fade — expired slide: magenta shift, faded blacks.
Expired / lo-fi:
17. Expired 03 — expired colour neg: colour shifts toward magenta/brown, raised blacks, heavy grain.
18. Attic — very old print: yellowed, low contrast, soft.
19. Leak — strong light leaks + warm shift.
20. Kiosk Flash — disposable with strong on-camera flash falloff and red-eye-warm skin.
21. Kiosk Night — disposable at night without flash: underexposed, green cast, huge grain.
22. Toy — plastic toy camera: heavy vignette, soft edges, saturated, colour fringing.
Black & white:
23. Silver 400 — classic grainy B&W 400: punchy contrast, coarse grain.
24. Silver 100 — fine-grain B&W: smooth mid-tones, gentle contrast.
25. Silver 3200 — pushed high-ISO B&W: crushed blacks, huge grain.
26. Silver Soft — matte B&W with lifted blacks.
27. Sepia — warm toned B&W.
28. Cyan — cyanotype-like blue toned B&W.
Video eras:
29. Tape — VHS camcorder (evolves VHS 94).
30. Hi8 — sharper camcorder, cool, interlace-ish weave.
31. DV 2004 — early digital camcorder: over-sharpened, cool, clipped highlights.
32. Webcam — low-light early webcam: noisy, green-yellow.
Moods:
33. Afterhours Dark — low-key city night, teal-green shadows, crushed blacks.
34. Halo — dreamy bloom on lights.
35. Fog — milky haze, low contrast, cool.
36. Golden — warm late-sun push, orange highlights.
37. Winter — cold blue, desaturated, bright.
38. Noir Tint — B&W with deep teal shadows.
39. Neon — boosted magentas/cyans for night city lights.
40. Plain — Natural: no colour change, grain only.

## 7. Overnight deliverable for the owner
- All 40 candidates as parameter sets, rendered by `tools/looks/sheet` (JVM test or Gradle task, no Python
  deps — numpy/PIL are not installed) on synthetic test frames AND on seeded demo clips: one contact sheet
  per family + one overview sheet, in `docs/qa/looks-film/` (scratchpad if the classifier blocks images).
- Each tile labelled with its number + our name, so the owner can answer "keep 1, 10, 23, 33".
- Only the kept ones become presets in the Film builder; the rest stay in candidates.json (not shown).
