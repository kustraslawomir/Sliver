# T25 — Export, premiere, player, share

Milestone: M4 Films  
Depends on: T24

## Goal
From 'Make film' to watching and sharing.

## Do
- Export screen with progress (observes the job).
- Premiere: black, slit grows to the film frame (1000 ms ease-out cubic), title card fades in at 1.4 s; reduced motion = final frame.
- Player: ExoPlayer, play/pause, scrubber with day ticks, mute, fullscreen landscape, current date label, Share film… (ACTION_SEND video/mp4 via FileProvider → YouTube etc.), Save to Photos (MediaStore Movies), Edit film.
- Library posters open the player for rendered films.

## Done when
- [ ] Share to YouTube app opens its upload flow
- [ ] Saved file appears in Photos
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
