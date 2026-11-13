# T-design-v1.5 — make the app match design v1.5

Read first: `docs/FUNCTIONAL.md` (behaviour), `design/INDEX.md` section "v1.5" (which board = which screen), `CLAUDE.md` "Test device".
Measure from `design/png/*.png` (@3x, px ÷ 3 = dp). Behaviour does not change unless written here — this is a look pass.

## Do, one commit per item
1. **App name as one string.** Every user-visible "Fragments"/"SLIVER" comes from `app_name` (strings + `Res.string`). Settings footer: plain text name + version, delete the slit wordmark composable usage there.
2. **Project screen** = `ProjectHub*`. Header icons (pencil, profile), review row + sheet, the 6 cell states exactly as INDEX says (draft = dashed #55555A, thumb 40 %, "Draft" top-left 10sp; empty = "+" 14dp #55555A; today = red 6dp dot). Films row: span + "N clips · length" under each poster. Dock disc has **no glow**.
3. **Undo bar** (one composable): 48dp, radius 14, inverted colours, 132dp from bottom on the project screen, above the buttons on Day detail. Used for Remove unchecked, Remove day, Remove clip, Delete film. Replace any other toast/snackbar.
4. **Camera**: zoom stops row per `CameraV2.png`; timer text "0:03 · Clip OK" in Geist 14/500 (no mono caps); "AE/AF lock" in Geist 12/600. Record button glow already matches `RecordGlow` — only verify sizes, don't rewrite it.
5. **From this day** = `FromThisDay*` (grid, badges, switch, Browse all photos, empty state).
6. **Check a day** = `EditClipDraft` / `EditClipDraftD` header + other-items strip + Keep this clip / Remove day.
7. **Fill the calendar** = `FillCalendar*` (setup big number, progress stats table, done mosaic + 4 actions).
8. **Day detail** = `DayDetail*`: remove share + "mark" option, two buttons only.
9. **Film builder** = `FilmBuilder`: no bundled music list (only the picked file + "Pick another audio file…"), Date on screen as a switch, filter thumbnails row, sliders only when a filter is on. Remove "Between days" if it still exists.
10. **Film player** = `FilmPlayer`: Save to Photos primary, Edit film, Delete film red text, file line 12sp.
11. **Onboarding** = `OnbV2`: red dot mark, "Restore from backup" under Start.
12. **No mono uppercase labels anywhere** (Geist Mono caps → Geist sentence case). `scripts/design-lint.sh`: add a rule that fails on `GeistMono` + `uppercase()`/all-caps string resources.

## Done when
- `./gradlew check` green, Roborazzi goldens re-recorded and diffed against `design/png` for the screens above (light + dark where an `L` board exists, font scale 1.3).
- `scripts/qa.sh` passes on the Xiaomi `b798509a` (or emulator `tracker13`). **Never the Samsung.**
- Screenshots in `docs/qa/v1.5/`, `HANDOFF.md` lists anything that still differs, one line each.
