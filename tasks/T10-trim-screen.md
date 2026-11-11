# T10 — Trim & Pick screen

Milestone: M1 Capture  
Depends on: T07, T08, T09

## Goal
The hero screen, per SPEC.

## Do
- Looping preview of the selected second (ExoPlayer with clipping, repeat).
- Filmstrip of thumbnails; fixed 1 s SliverFrame in the center; strip drags under it; dim outside; haptic tick every 0.25 s; lock-in pulse + impact on release.
- Badge AUTO-PICKED → CUSTOM after drag; initial position from auto-pick.
- Minimap with tap-to-jump for clips > 60 s.
- Audio toggle, speed 1× / 0.5×, FOR <day> sheet (last 7 days, marks days that already have a second).
- Use this second → ClipExporter → save → navigate back with UndoToast; tmp deleted after the undo window. Retake/close deletes tmp.
- Short clip mode (no strip): Slow to fit / Loop / Hold last frame chips.
- One-time coach mark.

## Don't
- No looks UI yet (button navigates to placeholder).

## Done when
- [ ] Whole flow record → trim → saved works end to end
- [ ] Undo restores the previous state
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
