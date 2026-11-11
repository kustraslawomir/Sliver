# T21 — Fill the past

Milestone: M3 Archive  
Depends on: T11, T12, T09

## Goal
Opt-in backfill from the gallery by taken date.

## Do
- Intro + range (Since I started / This year / Everything) + limited-access banner.
- Scan MediaStore DATE_TAKEN (fallback DATE_ADDED), group by local date; candidate per empty day: longest video, else first photo (only if 'Use photos too').
- Results mosaic (year rows), toggle months, 'Fill N days' → WorkManager batch: auto-pick + export (source BACKFILL), progress, 'Undo all' (deletes that batch).
- Pure matching logic in commonMain with tests.

## Don't
- Never touch days that already have a second.
- Never suggest this feature anywhere else.

## Done when
- [ ] Batch survives app close
- [ ] Undo removes exactly the batch
- [ ] Tests + build pass

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
