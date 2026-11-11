# T30 — Splash + edge states

Milestone: M5 App  
Depends on: T02, T18

## Goal
Launch animation and the four edge screens.

## Do
- Android 12+ SplashScreen with black bg → Compose slit-to-wordmark animation (1.4 s) → cross-fade to start destination; reduced motion = static.
- Edge states: empty archive, camera access off, export failed (Try again), phone storage full (check before recording/export; < 200 MB).

## Done when
- [ ] No white flash on launch
- [ ] Edge states reachable
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
