# T02 — Theme + base components

Milestone: M0 Foundation  
Depends on: T01

## Goal
`SliverTheme` with all tokens and the reusable components every screen will use.

## Do
- Bundle fonts (Instrument Serif, Geist, Geist Mono) via compose resources.
- Tokens from docs/tokens.json as Kotlin objects: colors, typography, spacing, radii, motion specs.
- Components: PrimaryButton, SecondaryButton, GhostButton, IconButton44, Chip, Badge, Segmented, SwitchRow, SliverSlider, SliverBottomSheet, UndoToast (host + state), SliverFrame (static visual of the trim window with states idle/dragging/locked).
- A debug-only `ComponentGallery` screen showing all of them.

## Don't
- No Material color scheme leaking into screens (map Material3 colors to tokens once, inside the theme).

## Done when
- [ ] Gallery screen renders every component
- [ ] No hex colors outside theme files
- [ ] Build + tests pass

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
