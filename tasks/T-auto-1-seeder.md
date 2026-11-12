# T-auto-1: debug data seeder

Problem: bugs like "filled day shows as an empty box" can't be reproduced on an empty app.

- Debug builds only (`BuildConfig.DEBUG`, never in release). Entry: Settings → long-press the
  version row → "Seed data", plus an adb hook:
  `adb shell am broadcast -a app.sliver.debug.SEED --ei days 365 --es project "My 2026"`.
- Generates real 1 s MP4 clips (Media3 Transformer from 6 bundled 2 s sample videos in
  `debug/assets/seed/`, each with a different colour and brightness), one per day, with ~1 in 13
  days left empty, a second project "Lisbon trip" (9 days, Story 9:16) and one finished film.
- Fills `luma` / `tone` the same way a real save does. Goes through the same repository code
  as a real save (no shortcuts), so the bugs show up.
- `--es mode wipe` removes everything it created.

Done when: after seeding, Month, Year, Projects, Day and Films show thumbnails, and a test covers the seeder.
