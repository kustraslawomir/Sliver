# U06 — Signature motion, nothing more

Depends on: U03

## Goal
Three memorable animations, consistent physics everywhere else.

## Do
- Use Compose spring specs from DESIGN.md (or Material 3 MotionScheme if your Compose version has it) — no tween soup.
- Signature 1 — record button: circle ⇄ rounded square shape morph on start/stop (spring), ring turns ember.
- Signature 2 — saved clip shrinks into the TODAY ✓ chip on Camera / into its calendar cell on Project.
- Signature 3 — slit loader: thin ember line sweeping, used for every loading state instead of spinners.
- Respect 'Remove animations' / animator scale 0: jump to end states.

## Don't
- No parallax, no confetti, no idle animations.

## Done when
- [ ] All three work at 120 Hz without jank
- [ ] Reduced-motion path verified
- [ ] Build passes

Refs: CLAUDE.md (+ v1.2 addendum), docs/UX-2026.md, v1.1 docs
