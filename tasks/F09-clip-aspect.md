# F09 — Previews use the clip's real aspect

Depends on: F08

## Goal
No more portrait boxes around landscape clips.

## Do
- Shared `ClipPlayer` composable: reads ExoPlayer VideoSize (incl. unappliedRotationDegrees) and sizes itself to the real display aspect (16:9 full width, 9:16 full height), centered on black.
- Use it in Edit clip, Photo clip, Clip detail, Filters editor and Player. Player: rotate the phone → fullscreen landscape.
- Filmstrip thumbnails keep the clip aspect too.

## Done when
- [ ] Landscape and portrait clips both look right everywhere
- [ ] Build passes

Screenshots: docs/screenshots/edit-rotated.jpg

Refs: CLAUDE.md, docs/NAVIGATION.md, docs/TERMS.md, docs/PROJECTS.md, docs/UI-RULES.md
