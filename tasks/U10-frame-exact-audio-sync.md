# U10 — Frame-exact clips + audio sync

Depends on: —

## Goal
Never the 'random part' or drifting audio the competitor is criticised for.

## Do
- Automated test (instrumented, Android only) that exports clips from known test videos and checks first-frame timestamp = chosen start ± 1 frame and duration 1.000 s ± 1 frame.
- Render a 365-clip film in a test and check audio/video end-time drift < 40 ms; fix by trimming audio to video duration per clip and resampling to one rate before mixing.
- Log a warning (debug) when a source has a variable frame rate; normalise to 30 fps on export.

## Done when
- [ ] Both tests pass on a device
- [ ] Build passes

Refs: CLAUDE.md (+ v1.2 addendum), docs/UX-2026.md, v1.1 docs
