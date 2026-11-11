# U09 — Accessibility 2026

Depends on: U06

## Goal
Inclusive by default.

## Do
- Font scale to 200% (nonlinear scaling) on every screen without clipping; long labels wrap or truncate with content descriptions.
- No color-only state: today cell has a ring *and* a dot; selected chips have a check icon; backup states have icons; disabled buttons have different shape/opacity *and* text.
- Contrast ≥ 4.5:1 for all text; check ember on dark for small text (use accent/core for text if needed).
- TalkBack: custom actions on calendar cells (Open, Replace, Remove); the trim strip is an adjustable slider with time announcements.
- Touch targets ≥ 48 dp on Android.

## Done when
- [ ] Accessibility Scanner shows no errors on main screens
- [ ] Manual TalkBack pass recorded in the summary
- [ ] Build passes

Refs: CLAUDE.md (+ v1.2 addendum), docs/UX-2026.md, v1.1 docs
