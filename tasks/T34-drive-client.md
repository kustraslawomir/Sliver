# T34 — Drive REST client

Milestone: M6 Backup  
Depends on: T33

## Goal
Minimal Drive v3 client in commonMain (Ktor).

## Do
- Find-or-create folder 'SLIVER' (store id). Resumable upload (start session, PUT file, resume on failure) with name + appProperties. List folder files with appProperties. Download. Trash. Read/write manifest.json.
- Error mapping to BackupError (DRIVE_FULL, REVOKED, OFFLINE, OTHER).
- Tests with Ktor MockEngine for each call + error mapping.

## Don't
- Only drive.file endpoints/behaviour; no other Google APIs.

## Done when
- [ ] Tests pass
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
