# U03 — Edge-to-edge + predictive back

Depends on: —

## Goal
Meet Android 16 defaults.

## Do
- Audit every screen with WindowInsets: camera preview full-bleed under status bar; bottom controls above the navigation/gesture inset; sheets respect IME + nav insets.
- Replace any onBackPressed / KEYCODE_BACK handling with BackHandler / PredictiveBackHandler. Sheets and Edit clip discard prompt follow the back gesture progress (scale/translate preview while swiping).
- Enable `android:enableOnBackInvokedCallback="true"`.

## Done when
- [ ] Back-to-home and in-app back animations visible on Android 15/16
- [ ] No content under the gesture bar
- [ ] Build passes

Refs: CLAUDE.md (+ v1.2 addendum), docs/UX-2026.md, v1.1 docs
