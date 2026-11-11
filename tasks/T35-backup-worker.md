# T35 — Backup worker + screen

Milestone: M6 Backup  
Depends on: T34

## Goal
Automatic backup and the Backup screen.

## Do
- WorkManager unique queue: upload Days with backup=PENDING (UNMETERED when Wi-Fi only), exponential backoff, trash replaced/removed files, update manifest.
- Backup state machine in commonMain (tests) → 7 UI states from SPEC.
- Backup screen: status card, folder, counts, Drive space used (about.get storageQuota), Auto-backup, Wi-Fi only, Back up now, Export everything (.zip via SAF), Disconnect (confirm).
- Backup problem notification only after 24 h of failures.

## Done when
- [ ] Airplane mode / Wi-Fi only / full Drive show the right states
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
