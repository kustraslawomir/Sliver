# T23 — Film builder

Milestone: M4 Films  
Depends on: T22, T16

## Goal
One-screen builder with live preview.

## Do
- Preview: ExoPlayer playlist of day files (1 s each) with the film look + date overlay + fade; tap = pause; scrubber with day ticks (≤ 40) or month ticks; seek.
- Options: Range (Week/Month/Year/Custom date range), Sound (Original/Music/Mix + 4 built-in tracks + pick file via SAF), Between days (Cut/Fade), Date on screen (On/Off, default from setting), One look (None/preset). Save to Film.
- Built-in tracks: placeholders (silent 3 min AAC) under composeResources/files/music until real tracks are added.

## Done when
- [ ] Preview reflects every option
- [ ] Film options persisted
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
