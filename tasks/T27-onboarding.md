# T27 — Onboarding

Milestone: M5 App  
Depends on: T06, T28

## Goal
First-launch flow, 7 steps from SPEC.

## Do
- Slit progress indicator, Back/Skip rules, step 3 format choice (sets newFilmFormat), steps 4–6 reuse PermissionGate, step 6 reminder setup with live notification preview, step 7 Google (if T33 done, else hidden) / Skip.
- Sets onboardingDone; next launch goes to Camera.

## Don't
- No backfill step.

## Done when
- [ ] Fresh install walks through and lands on Camera
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
