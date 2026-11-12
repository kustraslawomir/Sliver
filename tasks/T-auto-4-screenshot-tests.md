# T-auto-4: screenshot tests against the design

- Roborazzi (JVM, no device) for every screen in `design/INDEX.md`, dark + light, with seeded
  fake data (same content as the boards: "My 2026", 129 clips, Tue 10 Nov…).
- Recorded goldens live in `composeApp/src/test/snapshots/`. Separately, a report task
  compares each golden with `design/png/<Screen>.png` (scaled to the same width) and prints a
  similarity score; below the threshold (start at 0.85, tune later) = warning in the report,
  not a failure, because fonts render a bit differently.
- A golden diff vs the previous recording **is** a failure (catches agents silently changing UI).
Done when: `./gradlew verifyRoborazziDebug` runs in CI and the design-similarity report is generated.
