# T31 — Widget + Quick Settings tile

Milestone: M5 App  
Depends on: T13

## Goal
Home-screen shortcut to today's second.

## Do
- Glance 2×2: waiting (slit + 'Today's second is waiting') / captured (thumb + 'TODAY ✓'); tap → Camera. Update on save and at midnight.
- TileService 'SLIVER' → opens Camera (startActivityAndCollapse with PendingIntent on API 34+).

## Done when
- [ ] Widget updates after a capture
- [ ] Tile opens camera from lock screen pull-down
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
