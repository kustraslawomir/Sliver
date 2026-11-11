# T03 — Navigation shell

Milestone: M0 Foundation  
Depends on: T02

## Goal
App skeleton: routes, floating tab bar, placeholder screens.

## Do
- Routes for every feature in SPEC.md as placeholders (title text only).
- Floating tab bar (Archive · Camera slit button · Films) shown only on Archive and Films.
- Start destination: Onboarding if `onboardingDone` false, else Camera (use an in-memory flag until T05).
- Back handling standard; sheets via SliverBottomSheet.

## Don't
- No real screen logic.

## Done when
- [ ] Can tap through all placeholders
- [ ] Tab bar matches DESIGN.md
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
