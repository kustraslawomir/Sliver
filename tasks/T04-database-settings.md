# T04 — Database, settings, repositories

Milestone: M0 Foundation  
Depends on: T01

## Goal
Persistence layer from DATA.md.

## Do
- Room KMP: Day, Film entities with type converters (LocalDate, Instant, LookParams JSON, enums). DAOs with Flow queries: days in range, day by date, count, films.
- DataStore settings with a typed `SettingsRepository` (all keys from DATA.md, defaults as listed).
- `DayRepository`, `FilmRepository` interfaces + implementations.
- Unit tests with in-memory DB: insert/replace/delete day, range query, settings defaults.

## Don't
- No Drive fields logic (just columns).

## Done when
- [ ] Tests cover CRUD + defaults
- [ ] Build + tests pass

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
