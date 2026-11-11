# T15 — Look presets + LUT files

Milestone: M2 Looks  
Depends on: T01

## Goal
Define the 8 looks and produce LUT assets.

## Do
- `tools/gen_luts.py` (numpy only) that writes 33³ `.cube` files from simple recipes: grayscale, contrast, brightness, saturation, sepia, hue shift, soft-light tint. Recipes:
  Noir: grayscale 1, contrast 1.35, brightness 0.92 · Blackout: contrast 1.6, brightness 0.7, sat 0.75 · Night Neon: sat 1.7, contrast 1.2, brightness 0.88, hue −12°, tint #7A2AC8 @45%
  Cold Steel: sat 0.45, contrast 1.18, tint #3A8AC8 @55% · Ember: sepia 0.3, sat 1.35, contrast 1.08, tint #FF7A2A @45% · VHS 94: sat 1.35, contrast 0.85, brightness 1.05, tint #B04AC8 @20%
  Hard Light: contrast 1.65, brightness 1.06, sat 1.12 · Ghost: grayscale 0.65, brightness 1.15, contrast 0.72
- Commit generated files to compose resources. `LookPreset` enum with name, lut file, and extra defaults (grain, vignette, fade, sharp): Noir grain 35 vig 50 sharp 30; Blackout vig 65 sharp 40; Night Neon vig 40 grain 20; Cold Steel sharp 60 vig 25; Ember grain 22 vig 30; VHS 94 fade 35 grain 55; Hard Light sharp 90 vig 20; Ghost fade 55 grain 30.

## Done when
- [ ] 9 presets (Natural = identity) load
- [ ] Script is re-runnable
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
