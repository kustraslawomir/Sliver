# T24 — Film render

Milestone: M4 Films  
Depends on: T23

## Goal
Render a FilmPlan to mp4 on the device.

## Do
- Media3 Transformer Composition: video sequence (one EditedMediaItem per day with look effects, TextOverlay date, crop/blur-fill to the film format), Fade = 120 ms dip to black at boundaries, end title card 2 s.
- Audio: ORIGINAL = clip audio; MUSIC = clips muted + track; MIX = clips 100% + music 30%.
- Run in WorkManager as a foreground job (mediaProcessing type on API 35+, dataSync below) with progress notification; output `films/<id>.mp4`, 1080p30 H.264/AAC.
- Failure → keep partial state, 'Try again' restarts (chunked resume is a later improvement — note as DECISION).

## Done when
- [ ] 30-day film renders correctly with each option
- [ ] Render survives leaving the app
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
