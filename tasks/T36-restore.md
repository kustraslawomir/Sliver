# T36 — Restore on a new phone

Milestone: M6 Backup  
Depends on: T35

## Goal
Bring seconds back from Drive.

## Do
- Onboarding/Account entry 'Welcome back' → sign in → list folder → 'We found N seconds' → restore all: rebuild Day rows from appProperties, WorkManager downloads, regenerate thumbs/colors, films from manifest (re-render lazily).
- Progress UI with the month mosaic filling. Camera usable during restore; a day captured locally wins over a restored one (DECISION if conflict).

## Done when
- [ ] Fresh install restores a real backup
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
