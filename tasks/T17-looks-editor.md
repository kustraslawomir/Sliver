# T17 — Looks editor screen

Milestone: M2 Looks  
Depends on: T16, T10

## Goal
Editor reachable from Trim, Photo → 1 s, Day detail.

## Do
- Preview (press and hold = original), LOOKS tab with 9 live thumbnails (cheap: still frame + LUT) and intensity per look; ADJUST tab: chips Sharpness (default), Contrast, Grain, Vignette, Fade, Saturation, one big slider, Reset per tool, double-tap reset, Reset all.
- 'Set as default look' writes settings.defaultLook. 'Apply look' returns params to the caller.

## Done when
- [ ] Changes visible live
- [ ] Default look used by new captures
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
