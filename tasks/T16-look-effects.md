# T16 — Look effects engine

Milestone: M2 Looks  
Depends on: T15

## Goal
Build Media3 effects from LookParams, usable in preview and export.

## Do
- `LookEffects.build(params, forExport)`: SingleColorLut from the preset (mixed by intensity — if Media3 can't mix, blend in the shader), Contrast, HslAdjustment (saturation), plus one custom GlShaderProgram for sharpen (unsharp), grain (animated only when playing), vignette, fade.
- Apply in ExoPlayer via setVideoEffects and in Transformer exports.
- Camera preview: CameraX + Media3 effect bridge if available in your CameraX version, otherwise apply only the LUT in a SurfaceProcessor — note the choice as DECISION.

## Done when
- [ ] Same params look the same in camera, playback and export
- [ ] No frame drops on a mid device in preview
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
