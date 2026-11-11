# T05 — FileStore + date rules

Milestone: M0 Foundation  
Depends on: T04

## Goal
One place that knows where files live and what 'today' is.

## Do
- `FileStore` interface + Android impl: paths for days/thumbs/films/tmp, create/delete, free space, tmp cleanup (> 24 h) called on app start.
- `Clock` + `todayLocal()` + helpers: local date of an Instant in the device zone, week (Mon-first), month grid builder (leading blanks, 6 rows).
- Unit tests for date helpers incl. month starting on Sunday and DST days.

## Done when
- [ ] Cleanup runs on launch
- [ ] Date tests pass
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
