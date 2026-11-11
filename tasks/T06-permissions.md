# T06 — Permission pre-prompts

Milestone: M1 Capture  
Depends on: T03

## Goal
Friendly pre-prompt screens before system dialogs + denied states.

## Do
- Reusable `PermissionGate(kind)` composable: camera+mic, photos (full/partial), notifications (Android 13+).
- Pre-prompt copy from SPEC (onboarding steps 4–6 reuse it).
- Denied state screen for camera (SPEC edge states) with Open Settings + Add from gallery.

## Don't
- Don't request permissions at app start without a pre-prompt.

## Done when
- [ ] Grant/deny paths work on a real device
- [ ] Partial photo access detected and exposed as state
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
