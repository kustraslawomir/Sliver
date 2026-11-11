# T11 — Gallery picker

Milestone: M1 Capture  
Depends on: T06, T10

## Goal
Custom picker that feeds Trim / Photo → 1 s.

## Do
- MediaStore query: videos + images, newest first, grouped by day header; Recents / Favorites (IS_FAVORITE); filter All / Videos / Photos.
- `forDate` argument shown as a chip; Trim saves to that date.
- Video tap → Trim with content URI. Photo tap (single select) → Photo → 1 s.
- Partial access: banner with 'Select more' / 'Allow all'.

## Don't
- No multi-select.

## Done when
- [ ] Picks open the right screens with the right date
- [ ] Original files untouched
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
