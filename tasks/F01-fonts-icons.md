# F01 — Fonts + icon set

Depends on: —

## Goal
Fonts render as designed and every icon is a real vector icon.

## Do
- Verify Geist / Geist Mono / Instrument Serif load from compose resources and are wired into SliverTheme typography; remove any FontFamily.Default / MaterialTheme.typography use in screens.
- Create `core/icons/SliverIcons` with the Lucide icons listed in UI-RULES.md as committed ImageVectors.
- Replace every text-as-icon in the app (X, v, SND, GRID/TIMER/FLASH/FLIP circles, DAYS box) with icons + optional small Geist label.
- Mono font only for dates, timecodes, counts.

## Don't
- No layout redesign here (F07, F11 do that).

## Done when
- [ ] Grep finds no `Text("X")`, `"v"`, `"SND"` style icons
- [ ] Screens render Geist, not Roboto (compare with screenshots)
- [ ] Build passes

Screenshots: docs/screenshots/camera-before.jpg, docs/screenshots/edit-rotated.jpg, docs/screenshots/gallery-filters.jpg

Refs: CLAUDE.md, docs/NAVIGATION.md, docs/TERMS.md, docs/PROJECTS.md, docs/UI-RULES.md
