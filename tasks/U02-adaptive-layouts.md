# U02 — Adaptive layouts (≥ 600 dp)

Depends on: U01

## Goal
Tablets, foldables and desktop windows get a real layout because the orientation lock is ignored there.

## Do
- Compute window size class (compact / medium / expanded) in commonMain from window size.
- Compact: current phone layouts, portrait lock stays (`screenOrientation` only matters < 600 dp).
- Medium/expanded: Camera with controls along the side nearest the bottom/right; Edit clip with preview left, strip + actions right; Project screen two panes (calendar | clip detail or films).
- Max content width 600 dp for lists/sheets; viewfinder handles sensor vs window aspect and rotation.
- Verify state survives rotation/resizing (no lost tmp recording, no lost edit position).

## Done when
- [ ] Works on a foldable/tablet emulator in both orientations and in split-screen
- [ ] No state loss on resize
- [ ] Build passes

Refs: CLAUDE.md (+ v1.2 addendum), docs/UX-2026.md, v1.1 docs
