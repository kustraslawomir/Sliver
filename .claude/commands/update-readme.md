---
description: Refresh README.md with real screenshots and current feature status, then push auto/build
---
Goal: keep `README.md` a polished product page that matches the app as it actually is. Be cheap: no exploratory reading.

1. Read `README.md`, `.workflow/state.md`, and `HANDOFF.md` (if present). Feature rows for tasks that are BLOCKED/not done get a trailing "(coming)"; done ones none.
2. Privacy: the phone holds the owner's real photos. The ONLY safe media are in `Movies/SliverDemo` + `Pictures/SliverDemo` (synthetic, 28 clips + 5 photos, newest in Recents). Never capture a screen that shows other media: for the gallery picker open the SliverDemo album/folder first and capture only that; skip the camera preview unless it shows a blank wall; skip anything unclear. Review each image after capture and delete (don't commit) any that show anything personal.
   Screenshots (skip silently if `bash scripts/device.sh ready` prints `no`):
   - `bash scripts/device.sh install`, `launch`. Navigate with `device.sh text "<label>"` (prints tap coords) + `tap`, `back`, `swipe`. Never type credentials; use guest/skip paths.
   - Capture with `device.sh shot <name>` (portrait, one per screen): `camera`, `trim`, `looks`, `archive-month`, `archive-year`, `day`, `films`, `builder`, `premiere`, `settings` — only the ones reachable within ~60 actions total; don't loop on a screen you can't reach.
   - If the app has no seeded data, that is fine; capture what is real. Never fake or edit images.
3. Replace everything between `<!-- SCREENSHOTS:START -->` and `<!-- SCREENSHOTS:END -->` with a centered HTML table of the captured images (`width="200"` each, 4 per row, caption under each), relative paths `docs/screenshots/<name>.png`. If none were captured, restore the original note.
4. Commit `docs: update README` and run `git push origin auto/build`.
Reply in ≤5 lines: screenshots captured, README rows changed, push result.
