# F11 — Media picker fixes

Depends on: F01, F05

## Goal
A picker that looks and works like a real one.

## Do
- Header: title 'Add media' + close icon; 'FOR TODAY' / 'FOR <date>' chip when picking for a date.
- Filter row per UI-RULES.md: one LazyRow, two groups, 36 dp chips, maxLines 1 — fixes the vertical 'Photos' chip and the empty gap (screenshot gallery-filters.jpg).
- Day headers: Geist Mono 12 sp caps 'MON 9 NOV', not huge bold text.
- Video tiles: duration badge bottom-right with a small play icon; photos: nothing.
- Thumbnails load with placeholders; grid 3 columns, 2 dp gaps.

## Done when
- [ ] Matches UI-RULES, no wrapping at 200% font scale
- [ ] Build passes

Screenshots: docs/screenshots/gallery-filters.jpg

Refs: CLAUDE.md, docs/NAVIGATION.md, docs/TERMS.md, docs/PROJECTS.md, docs/UI-RULES.md
