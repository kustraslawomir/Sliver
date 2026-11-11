# F07 — Camera layout v2

Depends on: F01, F04

## Goal
Controls in thumb reach, clearer actions. Canvas page '10 · v1.1 fixes' shows it.

## Do
- Top: project chip (left, opens Projects) and calendar chip 'TODAY' / 'TODAY ✓' (right, opens Project; ✓ when today has a clip). Remove the 'Today's second is in · Replace' card (screenshot camera-replay-card.jpg) — Replace lives in Clip detail.
- Tools row just above the mode switch: Grid, Timer (off/3/10 shows value), Light (torch), Filter (opens quick filter strip). Icon + small label.
- Mode switch PHOTO / VIDEO (compact).
- Bottom row: Add media (latest gallery thumb with ember + badge, label 'ADD'), Record, Switch camera (icon rotates 180° on tap).
- Rotation: icons rotate upright with OrientationEventListener (animated 240 ms); layout stays put.
- Keep the 16:9 band + 'Turn sideways' hint for Cinema projects held upright.

## Done when
- [ ] Matches canvas CameraV2 / CameraV2Land
- [ ] Every control reachable with the right thumb on a 6.5\" phone
- [ ] Build passes

Screenshots: docs/screenshots/camera-before.jpg, docs/screenshots/camera-replay-card.jpg

Refs: CLAUDE.md, docs/NAVIGATION.md, docs/TERMS.md, docs/PROJECTS.md, docs/UI-RULES.md
