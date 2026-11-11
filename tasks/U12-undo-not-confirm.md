# U12 — Undo instead of confirm

Depends on: U06

## Goal
Modern pattern: act now, undo for a few seconds.

## Do
- Replace confirm dialogs with undo toasts for: remove clip, replace clip, discard picked gallery item, remove film.
- Keep explicit confirmation only for: delete project, delete account, discard an in-app recording (it exists nowhere else).

## Done when
- [ ] Each listed action is undoable within 6 s
- [ ] Build passes

Refs: CLAUDE.md (+ v1.2 addendum), docs/UX-2026.md, v1.1 docs
