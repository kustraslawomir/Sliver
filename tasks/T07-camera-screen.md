# T07 — Camera screen

Milestone: M1 Capture  
Depends on: T05, T06

## Goal
Home camera: preview, record any length to tmp, controls.

## Do
- CameraX preview + VideoCapture (1080p30) recording to `tmp/rec_<ts>.mp4`.
- Record: tap start/stop; long-press > 300 ms records while held. Running mono timer. Shutter haptic.
- Flip, flash (torch for video), grid overlay, self-timer 0/3/10 s.
- PHOTO / VIDEO segmented (photo capture → ImageCapture to tmp, then PhotoTrim route).
- Gallery thumb bottom-left (latest MediaStore item) → Gallery route.
- Persist mode, lens, flash, grid (not timer) in settings; restore on open.
- After stop → navigate to Trim with the tmp file.

## Don't
- No looks yet (T16).
- No format band yet (T14).

## Done when
- [ ] Cold start to ready-to-record < 2 s on a mid device
- [ ] Settings restored after app restart; timer off
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
