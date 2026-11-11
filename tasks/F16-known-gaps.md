# F16 — Known gaps from the v1 device test

Depends on: F01

## Goal
Close the open items from HANDOFF.md (v1 device test on Xiaomi M2101K9G).

## Do
- Saved clip must be exactly 1.000 s (currently 1.033 s): fix in ClipExporter / trim logic, add a test.
- Settings gear rendered as the letter "S": covered by F01 icons, verify.
- Splash: vertical slit, not a horizontal bar.
- "Edit film" must open the builder with the existing film, not a new one.
- Live camera preview should reflect the selected filter where feasible; otherwise note it in HANDOFF.md.
- Investigate similar defects in the same areas (text-as-icon, wrong default values, duplicated taps, unstyled fonts) and fix them.

## Done when
- [ ] Each item fixed or recorded in HANDOFF.md with a reason
- [ ] Build + tests pass

Refs: CLAUDE.md, HANDOFF.md
