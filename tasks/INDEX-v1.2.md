# v1.2 tasks

U01 first (policy). U03, U05, U10 have no dependencies and can run in parallel.

| Task | Title | Depends on | Why |
|---|---|---|---|
| [U01](U01-system-photo-picker.md) | System Photo Picker (policy fix) | — | Play policy |
| [U02](U02-adaptive-layouts.md) | Adaptive layouts (≥ 600 dp) | U01 | Android 16 ≥600dp |
| [U03](U03-edge-to-edge-predictive-back.md) | Edge-to-edge + predictive back | — | Android 16 defaults |
| [U04](U04-first-clip-30s.md) | First clip in under 30 seconds | U03 | Moonlitt-style onboarding |
| [U05](U05-live-updates.md) | Live progress notifications | — | Android 16 Live Updates |
| [U06](U06-signature-motion.md) | Signature motion, nothing more | U03 | Tide Guide-style signature motion |
| [U07](U07-quick-capture.md) | Quick capture everywhere | U01 | competitor complaint |
| [U08](U08-motion-photos.md) | Motion Photos → clips | U01 | competitor's best-loved feature |
| [U09](U09-accessibility-2026.md) | Accessibility 2026 | U06 | Guitar Wiz-style inclusivity |
| [U10](U10-frame-exact-audio-sync.md) | Frame-exact clips + audio sync | — | competitor bugs |
| [U11](U11-performance.md) | Startup + smoothness | U02 | quality bar |
| [U12](U12-undo-not-confirm.md) | Undo instead of confirm | U06 | modern pattern |
