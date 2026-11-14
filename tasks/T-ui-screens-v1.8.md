# T-ui-screens-v1.8 — 2nd iteration of the main screens (do after T-ui-audit-v1.8; where they differ, THIS wins)

Boards: `design/png/HubV18.png`, `DayV18.png`, `BuilderV18.png`, `SettingsV18.png` (sources `design/src/*V18.dc.html`). Placeholder art only.

## Project screen (HubV18)
- Header: name 28/600 + chevron, gear, ⋯. Subtitle one line: "155 clips · since 1 April · Story 9:16" + sync icon & word only while syncing.
- Row: Month/Year segmented 40 dp (168 dp wide) on the left, month arrows on the right — one row, not two.
- "October" 22/600 + caption "23 of 31 days". Cells 46 dp, radius 11, day number 12/500 bottom-left with a 1 dp text shadow. No "+30" badges, no "+" in empty cells (empty past day = #141416 tile with the number #6E6E68; future = number only #55555A).
- Films: "Films 3" + small filled "Make film". Posters 112×142 radius 16, title on the poster (20/600), small play circle top-right; caption under the poster 13 sp, 2 lines: range / length · look. Each film its own cover.
- Dock: 68 dp, uses the new record button (ring + disc) at 48 dp. Content bottom padding so the last poster caption sits above the dock.

## Day screen (DayV18)
- Three equal action tiles instead of 3 stacked pills: Moment (scissors) · Replace (swap) · Remove (trash, #E5675F). 72 dp tall, radius 20, white 10 % tint over the bottom scrim, icon 24 + label 14/500, 8 dp gaps. Remove still uses the undo bar.
- Title 24/600, "Day 184 · 21:07" 15 sp #EDEDE8, arrows plain (no circles) — the scrim carries them.
- Top: back + sound as 44 dp black 28 % circles, no border.

## Film builder (BuilderV18) — make it short, move detail into sheets
- Header centred: film name + "Story 9:16 · range · N clips".
- Preview 164×290 (9:16; 16:9 = full width × 197), 56 dp play circle, never the word "Paused".
- **Filmstrip scrubber** 36 dp of real thumbnails with a 3 dp white playhead, replaces the empty line + tick marks; month initials under it.
- "Look": 64×80 tiles with real names only.
- **Sound** and **Date on screen** become two rows with a value + chevron: Sound → opens the audio layers editor / music list (value "Original", "Music", "Original + 1 track", sub "Clair Suspense · 70 %"); Date on screen → small sheet: Off / Plain / Film / Camcorder (value shows the choice). No inline track list, no inline segmented controls.
- Sticky "Make film · 2:35" over a fade; never disabled — if Music is chosen with no track, tapping it opens the music list.

## Settings (SettingsV18)
- Groups: Account & backup / Camera / App. Copy as on the board (subtitles describe behaviour, never "On:"/"Off:"). Backup row shows last sync ("On · 2 min ago"). No "Fill the calendar" row.
