# T08 — Clip exporter (1 s)

Milestone: M1 Capture  
Depends on: T05

## Goal
Turn any source + start time into the day file.

## Do
- `ClipExporter.exportSecond(source, startMs, speed, cropY?, format)` with Media3 Transformer: ClippingConfiguration, re-encode 1080p H.264/AAC, output exactly 1.000 s. Speed 0.5: take 500 ms and stretch.
- Short source (< 1 s): modes SLOW_TO_FIT, LOOP, HOLD_LAST.
- Thumbnail (320px JPEG) from the exported file.
- `DayRepository.saveSecond(...)` writes file + row atomically (temp name, then rename).

## Don't
- No look/date burned in.

## Done when
- [ ] Exported files are 1.000 s ±1 frame (check with MediaMetadataRetriever in a small debug check)
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
