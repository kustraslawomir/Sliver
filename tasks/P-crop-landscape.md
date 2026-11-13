# P-crop-landscape — choose the crop area for horizontal video in a vertical project

Owner request: "I create a project in vertical mode, using a horizontal video, I want an option to choose the area to crop."

## Today
`TrimScreen.kt`: `reframe = format == FilmFormat.CINEMA && clipAspect < 1f` shows `ReframeBand` (vertical drag, `cropY`) for portrait sources in a cinema project. `SaveClipRequest.cropY`, `AndroidClipExporter`, `AndroidFilmRender`, DB column, sync records and `CameraLogic` already carry `cropY`.

## Do
1. Generalise to the opposite case: `format == FilmFormat.STORY && clipAspect > 1f` (source wider than 9:16 target) shows a draggable 9:16 band over the video that moves LEFT/RIGHT (`cropX` 0..1, default 0.5 = centered). Same dimming outside the band, same accent border, 48dp touch-friendly, state survives recreation.
2. Persist `cropX` next to `cropY`: SaveClipRequest, clip row (additive Room migration with a test; old rows = centered), exporter (`AndroidClipExporter`), film render (`AndroidFilmRender`), backup/sync records (old manifests without it = centered). Export must match what the preview shows.
3. Same control in `PhotoTrimScreen` (photos) and wherever the draft/fill-empty-days flow shows Trim (it reuses the same screens). Auto-imported days (no user choice) stay centered.
4. Pure helper(s) in commonMain with unit tests: band fraction for a given source/target aspect, clamp, cropX to crop rect. Existing `cinemaBandFraction`/`topFromCropY` helpers are the pattern.
5. Device proof (Xiaomi first; CLAUDE.md privacy rules, no real media in screenshots): vertical project + a landscape SliverDemo clip -> band visible and draggable; saved clip crop matches (ffprobe dims + compare a frame computationally, do not view real media). Add a unit/ffprobe check where possible.

Build + tests + lint green, one commit, explicit paths, push auto/build only. Add a short HANDOFF.md note.
