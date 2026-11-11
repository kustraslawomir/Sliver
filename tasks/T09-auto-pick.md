# T09 — Auto-pick scoring

Milestone: M1 Capture  
Depends on: T01

## Goal
Pick the best second automatically.

## Do
- commonMain: pure function `bestSecond(frames: List<FloatArray>, fps): Long` — mean absolute luminance diff between consecutive frames, pick the 1 s window with the highest sum, ties → closer to the middle.
- androidMain `FrameSampler`: 10 fps at ~160px width, luminance only; for clips > 60 s sample at 2 fps then refine ±5 s at 10 fps.
- Unit tests with synthetic frames (static, single spike, two spikes).

## Don't
- No ML, no faces, no audio analysis.

## Done when
- [ ] Tests pass
- [ ] 15 s clip scored in < 300 ms on device (log it)
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
