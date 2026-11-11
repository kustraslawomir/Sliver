# T12 — Photo → 1 s

Milestone: M1 Capture  
Depends on: T08, T11

## Goal
Make a 1 s clip from a still.

## Do
- `ClipExporter.photoToSecond(uri, motion)`: Media3 image input (duration 1 s, 30 fps); ZOOM = time-based MatrixTransformation scale 1.00 → 1.08 eased.
- Screen: looping preview, chips Still / Slow zoom, Use this second / Retake / Looks. Saves with source PHOTO.

## Done when
- [ ] Output 1.000 s, smooth zoom
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
