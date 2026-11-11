# T01 — Project setup

Milestone: M0 Foundation  
Depends on: —

## Goal
Empty KMP app that builds and launches on Android with a black screen.

## Do
- Create KMP project (composeApp, targets android + iosArm64/iosSimulatorArm64) per ARCHITECTURE.md, package `app.sliver`.
- Version catalog with all libraries from ARCHITECTURE.md (latest stable). Add only what this task compiles against; others commented as `# later`.
- Koin started in Application; empty `commonModule` + `platformModule`.
- Single activity, edge-to-edge, portrait-locked, dark system bars.
- iosMain: compile-only stub `MainViewController` — don't build it.

## Don't
- No screens yet.
- No CI setup.

## Done when
- [ ] `./gradlew :composeApp:assembleDebug` passes
- [ ] App launches to a #0A0A0B screen
- [ ] `testDebugUnitTest` runs (one trivial test)

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
