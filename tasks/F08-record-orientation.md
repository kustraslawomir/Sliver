# F08 — Record in the real orientation

Depends on: F07

## Goal
A clip filmed sideways is a landscape clip.

## Do
- Bug: with the activity locked to portrait, CameraX keeps targetRotation = portrait, so sideways recordings are stored rotated (screenshot edit-rotated.jpg).
- OrientationEventListener → map to Surface.ROTATION_* → set `videoCapture.targetRotation` and `imageCapture.targetRotation` before each recording/photo.
- Verify the saved file: MediaMetadataRetriever width/height/rotation gives a landscape display size for sideways recordings.

## Done when
- [ ] Sideways recording plays as landscape in the system gallery
- [ ] Build passes

Screenshots: docs/screenshots/edit-rotated.jpg

Refs: CLAUDE.md, docs/NAVIGATION.md, docs/TERMS.md, docs/PROJECTS.md, docs/UI-RULES.md
