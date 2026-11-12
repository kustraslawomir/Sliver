# T-film-whole-project: a film is always the whole project (owner decision, 2026-11-12)

A project already is the time span ("My 2026", "Lisbon trip"), so choosing Week / Month / Year / Custom
in the film builder makes no sense. The owner decided: **a film renders the whole project**.
This overrides the RANGE section in `design/src/FilmBuilder.dc.html` and docs/SPEC.md §Films.

## Do
- Film builder: remove the RANGE section (chips + custom date sheet). The preview, runtime and
  "Make film · mm:ss" use every filled day of the active project. Open days are still skipped.
  Close the gap the section leaves per the board's spacing (no empty slot).
- Film model: films no longer store a range. Keep the DB columns `rangeType/rangeFrom/rangeTo`
  readable for old rows (no destructive migration needed); new films write the whole-project
  values or nulls. Old films with a range keep playing their existing rendered file; re-render
  uses the whole project.
- Poster / library / premiere subtitle: replace the range text with the project span,
  e.g. "10 Jan – 12 Nov 2026 · 129 clips" (Geist, tabular figures; no uppercase, no "seconds").
- "LIVE / collecting" films: a collecting film is simply the project's film that grows as clips
  are added; keep that behaviour.
- Backup/restore: keep reading old `rangeType` fields; don't write new ones (or write nulls).
- Seeder (debug) and tests: update to the new model; remove range-only tests, add a test that a
  film includes every filled day of its project.
- docs/SPEC.md: update the Builder and Library/Premiere lines. design/INDEX.md: add a short
  "v1.4.1 owner decisions" note: film = whole project (no RANGE), camera opens on Video / back /
  1× / light off every time (mode, lens, light not remembered).

## Done when
Build + tests + lint + designLint green; film builder screenshot (dark + light, seeded synthetic
data, Xiaomi or emulator only) in docs/qa/film-whole-project/; a seeded project's film includes
all its days.

## Also: delete a film (owner request, 2026-11-12)
`FilmRepository.delete` exists but no UI reaches it. Add it per tasks/U12-undo-not-confirm.md
("remove film" = act now + undo toast, no confirm dialog) and reuse the undo pattern already used
for removing a clip in `DayDetailScreen` (same toast, same 6 s window).
- Film player: in the `⋯` / secondary actions, "Delete film" as **red text only** (v1.4: destructive
  = red text, never a filled red button). Deleting closes the player, returns to the project/library
  and shows "Film deleted · Undo".
- Library/Project screen: long-press a film poster → small sheet with "Delete film" (red text).
- After the undo window: delete the rendered file and poster from app storage, remove the DB row,
  and let the next backup remove it from Drive (check BackupEngine handles a deleted film; fix if
  not). Clips are never touched.
- A collecting (LIVE) film can be deleted too; it simply stops existing until the user makes a new one.
- Unit test: delete + undo restores the film; delete without undo removes row and files.
- If design/src has no board for this, keep it minimal in existing components (DECISION comment).
