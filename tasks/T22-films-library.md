# T22 — Films: model + library

Milestone: M4 Films  
Depends on: T04, T03

## Goal
Film CRUD and the library screen.

## Do
- Default live film '2026' (year of first day, format from settings) created on first save.
- Library: Year in Seconds teaser (shown from 1 Dec, unlock 26 Dec local), poster grid (name, format, runtime = number of seconds in range, range text, LIVE badge), '+ New film'.
- Pure `FilmPlan` builder in commonMain: list of day files in range, skipping open days, + runtime; tests.

## Done when
- [ ] Plan tests pass
- [ ] Library shows real counts
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
