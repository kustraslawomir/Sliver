# T-ui-polish-v1.9 — owner's phone review #2 (14 Nov, light theme)

Big step up: hub hierarchy, day screen, settings and the short builder are right. What's left is below, worst first. Commit + push per section. Xiaomi/emulator only, never the Samsung. QA screenshots with placeholder media only.

## 1. Sound sheet — the weakest screen now
1. **Fake waveform** at the top (uniform grey bars) means nothing → replace with a real timeline: row 1 = clips as a thin filmstrip (same thumbnails as the builder), rows below = one bar per audio layer, **labelled inside** with the track name (13/500, ellipsis), each layer its own tint (layer 1 #6E8BFF, 2 #E59A4F, 3 #5FB58A at 70 %; light theme same hues at 85 %). Playhead line across all rows. Tap a bar = open that layer.
2. **Clip sound slider** has no thumb — just a line with a tick at the end, looks broken. Use the kit slider: 4 dp track, filled part #141413 (dark: #EDEDE8), 24 dp round thumb, value "100 %" right-aligned 15 sp tabular, speaker icon toggles mute.
3. **"Tap a clip to mute" chips** — 156 chips in a horizontal row doesn't scale. Remove. Per-clip mute = tap a thumbnail in the timeline filmstrip (muted ones get a small crossed-speaker badge) + one row "Muted clips · 3 ›" that opens a grid of all clips to toggle. Keep it out of the main sheet.
4. **"Add audio layer"** is bare indented text → full-width secondary button with a plus: "+ Add music". Opens the file picker; then the layer row appears.
5. Layer rows: left icon = 40 dp **play/pause preview** (not a speaker), title cleaned better ("Sub Clair Suspense" → strip leading "sub_"/"sub " prefixes too), caption "3:06 · from 0:00 · 100 %" ok. Swipe left or ⋯ → Remove.
6. Empty state (no layers): one line "Add music from your files. It plays over the clips." + the button. Footer: "You're responsible for music you add from your files."

## 2. Trailing "·" bug (2 places)
- Film poster caption "2:36 · v4 ·" and player subtitle "156 clips ·" end with a dangling dot when the look is None. Build captions with `listOfNotNull(...).joinToString(" · ")`. Add a unit test for empty parts.

## 3. Player
1. Fullscreen button sits on top of the date stamp "1 APR 2026" (bottom-right). Move fullscreen to the top-right of the video (or the header), keep the stamp clear. Rule: no control in the stamp corner.
2. Scrubber still has the meaningless tick marks → use the builder's filmstrip scrubber (36 dp thumbnails + white playhead) with month initials.
3. Duration mismatch: builder says 2:36, player −2:38. Either the builder counts the end card or not — make both show the same length.
4. Main action: Share is what people do with a finished film. Primary = "Share…", secondary = "Save to Photos" (after saving: "Saved ✓ · Movies/Reelog", disabled). Drop the duplicate share icon in the header.

## 4. Builder
1. Preview play button is a thin outline triangle on a faint circle — invisible on bright frames. 56 dp filled circle (white 85 %, dark icon), like the player.
2. "Make film" sticky bar covers the bottom of the "Date on screen" row. Scroll content needs bottom padding = bar height + 16; add a 24 dp fade above the bar.
3. Look tiles show a random other clip (field photo). Use the frame under the playhead so you see the look on YOUR film.
4. Selected tile ring: 2 dp, not 3 dp; "None" tile same fill as others (#E9E7E1 light / #1C1C1F dark), no heavy border.
5. "Date on screen" sheet: each option shows a small sample of that style next to the name (Plain "1 APR 2026" mono white, Film = orange LCD digits "'26 4 1", Camcorder = "APR 1 2026 / 09:44 AM" + REC dot). Selected = check, as now.

## 5. Project screen
1. Future weeks (16–30) take ~3 rows of empty space and push Films off screen. Future-only rows get height 28 dp (number only, no tile) — or collapse to the current week + next. Past/empty days unchanged.
2. Month/Year segmented is bigger than spec (≈56 dp tall, 220 wide). Spec: 40 dp tall, 168 wide, labels 15/500.
3. Dock when today is done: the full capsule over the films is noise once the job is done. Shrink to a 56 dp round thumbnail of today at bottom-right (tap → day screen); full capsule only while today is empty.
4. 4 films of the same range (v1–v4): show as one poster "Lena · 4 versions" with a stack edge; tap opens the newest, long-press lists versions. (If owner disagrees, keep separate posters.)
5. Content bottom padding so the last poster + caption can scroll fully above the dock.

## 6. Backup & sync
1. Doesn't match Settings: Folder / Clips / Drive and the account rows float on the background. Use the grouped cards (radius 18) like Settings: card 1 Status (Synced · 17 min ago · 2 phones), card 2 Folder / Clips / Drive, card 3 Sync automatically / Wi-Fi only, card 4 Account (email, Sign out), then Delete account.
2. "Delete account" is indented differently from everything else (left 100 vs 46). Same left edge as all rows.
3. Drive almost full: add what it means and what to do — "465 MB free · about 230 more clips" + "Manage Drive storage ›" (opens Google's storage page).
4. "Sync now" as a black primary when everything is already synced is too loud → secondary filled button; primary only when there's something waiting ("Sync 12 clips now").

## Done
Before/after screenshots of: sound sheet (empty, 2 layers), player, builder bottom, date sheet, hub (current month, today done), backup — light + dark — in `qa/v1.9/`.
