# T14 — Film format hints + reframe

Milestone: M4 Films  
Depends on: T10, T22

## Goal
Camera follows the active film's shape; portrait clips can be reframed.

## Do
- Camera chip 'FOR: <film> · <format> ▾' with menu of live films + New film. Persist activeFilmId.
- Cinema + device upright: dim outside a centered 16:9 band; chip 'Turn sideways for the full frame' for 3.5 s per session.
- OrientationEventListener: rotate icons; layout stays portrait-locked.
- Trim: if active film is Cinema and clip is portrait → draggable 16:9 band over preview, default center, saved as `cropY`.

## Don't
- Never block recording, no dialogs.

## Done when
- [ ] Band/hint follow the active film
- [ ] cropY persisted
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
