# U08 — Motion Photos → clips

Depends on: U01

## Goal
Android's version of the competitor's best-loved feature (Live Photos).

## Do
- Detect Motion Photo 1.0 via XMP (`Camera:MotionPhoto` = 1, `Container:Directory` item with `Item:Semantic` = MotionPhoto and `Item:Length`); confirm the video bytes actually exist.
- JPEG: video = last `Length` bytes. HEIC/AVIF: top-level `mpvd` box payload. Ignore legacy MicroVideo tags.
- Write the embedded MP4 to tmp and open Edit clip with it (default window centered on `MotionPhotoPresentationTimestampUs`, else the midpoint). Fall back to Photo clip if no video.
- Unit tests for the parser with fixture files.

## Done when
- [ ] Pixel and Samsung motion photos become moving clips
- [ ] Parser tests pass
- [ ] Build passes

Refs: CLAUDE.md (+ v1.2 addendum), docs/UX-2026.md, v1.1 docs
