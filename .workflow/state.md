- [x] T01
- [x] T02
- [x] T03
- [x] T04
- [x] T05
- [x] T06
- [x] T07
- [x] T08
- [x] T09
- [x] T10
- [x] T11
- [x] T12
- [x] T13
- [x] T15 (LUT .cube files not yet generated: python execution denied; run tools/gen_luts.py)
- [x] T16 (camera preview look deferred, see HANDOFF)
- [x] T17
- [x] T18
- [x] T19
- [x] T20
- [x] T21
- [x] T22
- [x] T23 (built-in music assets missing, see HANDOFF)
- [x] T14
- [x] T24 (not device-tested, see HANDOFF)
- [x] T25 (not device-tested, see HANDOFF)
- [x] T26
- [x] T27 (Google step hidden until T33)
- [x] T28 (alarms not device-tested, see HANDOFF)
- [x] T29 (Account/Backup rows route to Backup placeholder until T33)
- [x] T30 (splash overlay + v31 splash theme, not device-tested)
- [x] T31 (RemoteViews widget, not device-tested, see HANDOFF)
- [x] T32 (semantics added; manual TalkBack pass not done, see HANDOFF)
- [x] T33 (fake auth unless GOOGLE_WEB_CLIENT_ID set, see HANDOFF)
- [x] T34 (Ktor RestDriveClient + InMemoryDriveClient when auth is fake)
- [x] T35 (BackupEngine + WorkManager worker, 7-state machine, Backup screen, zip export, 24h problem notification)
- [x] T36 (RestoreEngine + RestoreWorker, Restore screen, local day wins; not device-tested)
- [x] T37 (sign out + delete account sheet; Drive folder trashed, not device-tested)
 - [x] F04 (projects sheet, repo rename/format/archive/delete)
- [x] F05 (no tab bar, AppNavigator, flow origin + toast on destination, discard sheet, SavedStateHandle, deep-link stacks; not device-verified)
- [x] pixel-perfect pack (8): byte-identical to applied design/tools/brand/android-res; no new tasks (checked 2026-11-12)
- [x] Camera zoom stops (0.6/1/2/3 + pinch) + 4K/bitrate/stabilization + 16 Mbps day encode (Samsung not device-tested, see HANDOFF)
- [x] T-design-v1.3 (code done; reminder firing not verified on Xiaomi (MIUI alarm deferral), filled-day/project-cover bug not reproduced; QA shots not committed, see HANDOFF)

- T-design-v1.4-mono: DONE except leftovers (seeder-dependent bugs, reminder emulator test); see HANDOFF.md.
- T-auto-1: DONE (seeder, thumbnail self-heal, memory card checked, reminder emulator test; see HANDOFF.md section T-auto-1)
- T-auto-3: DONE (scripts/design-lint.sh in check; see HANDOFF.md section T-auto-3)
- [x] T-auto-6 (tokens.json codegen, see HANDOFF)
- T-auto-4: DONE (Roborazzi goldens for 16 screens + tools/design-report.py; see HANDOFF.md section T-auto-4)
- T-film-whole-project: see HANDOFF.md section T-film-whole-project
- T-auto-2: DONE (scripts/qa.sh + qa/flows, no Maestro; Xiaomi run green; see HANDOFF.md section T-auto-2)
- T-auto-5: DONE (tools/design/render.sh + dcrender.mjs + pngdiff.mjs, Chrome headless; 71/79 boards >=95%; see HANDOFF.md section T-auto-5)
- T-looks-film: DONE (film-level filters, shader pass, 6 presets, 40-candidate pool + sheets; on-device perf TODO, see HANDOFF.md)
- T-auto-import-drafts: DONE (see HANDOFF.md section T-auto-import-drafts)
- T-sync-two-devices: DONE in code/tests; device check TODO (see HANDOFF.md section T-sync-two-devices)
- T-media-any-source: DONE except on-device HDR tone-map check (emulator lacks Main10 decoder); see HANDOFF.md section T-media-any-source
- T-auto-7: DONE (builder workflow, gated by BUILDER_AUTORUN, inert until enabled; see HANDOFF.md section T-auto-7)
- T-autofill-review: DONE (see HANDOFF.md section T-autofill-review)

## 2026-11-13 — T-rename-reelog + T-design-v1.5 done
- Reelog name (token), icon, splash, store text; v1.5 look pass items 1–12 (settings, project screen, undo bar, camera,
  From this day, check-a-day editor, Fill the calendar, Day detail, Film builder, Film player, onboarding, no mono caps).
- OOM fix (player buffers) b9337ed. QA: docs/qa/reelog/, docs/qa/v1.5/. Open items in HANDOFF "Reelog rename + design v1.5".

## T-glass-v1.7 (2026-11-13, done inline — builder workflow skipped twice)
- 1 type scale (ReelogType, core/theme/Type.kt, lint) · 2 kit (core/kit/Kit.kt) · 3 glass (Haze 1.6.10, core/kit/Glass.kt; blur only with a sibling source: app root for the undo bar, project screen for dock + project sheet; media glass = tint, SurfaceViews can't be blurred) · 4+7 record button v2 (Canvas) · 5 dock (idle CPU 0 %) · 8 day screen · 9 project sheet · 6 Settings, Hub header, filter tiles, player ambient.
- Owner overrides: player keeps share/edit/delete icons + Save; "Sound in film" removed from the day screen.
- Open: Xiaomi screenrecord of the record button and Xiaomi CPU number (Xiaomi not connected); light-theme + 1.3x shots of all 5 boards (only hub 1.3x done); camera/builder board pixel pass.

## T-glass-v1.7 item 10 (13 Nov)
- Change moment + kept camera originals + launch-time rescue from tmp (before cleanTmp; failed rescue = no clean). DB v11.
- Camera: video quality fallback any lower than FHD/UHD (emulator crashed with "Unable to find selected quality").
- Verified on emulator tracker13: record → original kept after undo window; Change moment → start 0 → 2580 ms, same original file; Cancel; leftover rec_ in tmp rescued on launch. QA: qa/v1.7/day-change-moment.png
- Old builds delete the recording 6 s after Save, so a day saved with an old build has a recording to rescue only if the app was killed inside that window.
- Pull-to-sync removed from the calendar (owner).

## 13 Nov evening (owner on Samsung)
- Recordings never lost: removing/replacing a day moves its kept recording to Movies/Reelog first (app copy deleted only after the gallery has it). "Also save full recordings to Photos" now works (was never implemented). RecordingSaver (Android MediaStore).
- Project start = earliest clip: DB 11→12 pulls existing projects back (Lena 13 Nov → 1 Apr), and any saved clip earlier than the start moves it (DayDao.pullProjectStart). Fill the calendar counts from the earliest clip.
- Fill the calendar runs as a foreground job (notification 9203): Samsung killed the background scan for "excessive binder traffic during cached".
- Samsung: "Lena iOS" folder (8710 iPhone files) had no metadata in MediaStore; rescanned. Recovered 13 Nov recording to Movies/Reelog/Reelog_2026-11-13_1459.mp4.
