# F05 — Navigation model

Depends on: F03

## Goal
Implement docs/NAVIGATION.md exactly.

## Do
- Remove the bottom tab bar. Camera is root. Routes per the map.
- Flow origin + pop-to-origin after Save clip with a one-shot result → toast on the destination.
- Single-flight helper used by all primary actions; de-duplicate navigation events.
- Deep links from notifications, widget and QS tile with the stacks from the table.
- Discard sheet for in-app recordings; SavedStateHandle for Edit clip restore.

## Done when
- [ ] Every row of the NAVIGATION.md table verified by hand on device
- [ ] Double-tapping any primary button triggers it once
- [ ] Build passes

Screenshots: docs/screenshots/edit-loading.jpg

Refs: CLAUDE.md, docs/NAVIGATION.md, docs/TERMS.md, docs/PROJECTS.md, docs/UI-RULES.md
