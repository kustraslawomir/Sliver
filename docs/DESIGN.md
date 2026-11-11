# SLIVER — design

Mood: A24 title cards, Halide, Leica. Dark room, one light on. The signature is a thin vertical slit of warm light.

## Tokens (also in tokens.json)
Colors
- bg/void #050505 (camera, premiere) · bg/base #0A0A0B (app)
- surface/1 #111113 (cards, cells) · surface/2 #18181B (sheets, toasts) · surface/3 #202024 (pressed, disabled)
- line/hairline #26262A · line/strong #3A3A40
- text/primary #EDEDE8 · secondary #B9B9B3 · tertiary #8E8E88 · disabled #55555A (never for information)
- accent #FF8A3D · accent/core #FFD2AE (slit center) · accent/line #FF8A3D55 · accent/tint #FF8A3D1F · on-accent #0A0A0B
- success #8FC7A4 · warning #E8C27A · error #E5675F · scrim #000000B3
Type (bundle the fonts, all OFL)
- Instrument Serif 400: displayXL 64/64, displayL 44/48, displayM 30/34 — moments, titles of sheets/films. Never on controls.
- Geist: title 22/28 600 · headline 17/24 500–600 (buttons) · body 17/26 · bodySmall 15/22 · caption 13/18
- Geist Mono 500: monoL 15/20 +6% · monoS 12/16 caps +12% — dates, timecodes, counts, labels
Spacing 4pt grid (4 8 12 16 20 24 32 40 56 72), screen gutter 20. Primary actions in the bottom 40% of the screen.
Radii 4 · 10 (sliver frame, calendar cells) · 16 · 24 (cards, preview) · 32 (sheet top) · pill
Elevation = surface step + 1px hairline. Only sheets/toasts get a shadow.
Glow (ember only): small 0 0 6px · medium 0 0 14px + 0 0 36px @55% · CTA 0 8px 32px @27%.

## Components
- PrimaryButton: 56 high, pill, ember fill, on-accent text, CTA glow. One per screen.
- SecondaryButton: 52–56, pill, 1px line/strong, text primary.
- GhostButton: 44 high, text secondary. Destructive = text error.
- IconButton: 44×44 hit area, over camera: #050505 at 40–60%.
- Record button: 80 outer ring 3px bone, inner 62 ember dot. Recording: ring ember + 28 rounded square. Hold: grows 1.2×. Photo: inner bone.
- Chip: 32–40 high pill, 1px hairline; selected = ember line + tint. Badge mono caps.
- Segmented: track surface/1 + hairline, selected pill bone with on-accent text.
- Switch row: 58–64 high, title + caption, track ember when on.
- Slider: 2px track, ember fill, thin slit thumb (3–4×20–28). Native semantics.
- Bottom sheet: surface/2, top radius 32, grabber 36×4, scrim 70%.
- Toast: 56 high pill, surface/2, Undo in ember text.
- Calendar cell 46×58–62, radius 10: filled (thumb, mono day bottom-left) · empty (surface/1 + 4px dot) · today (2px ember ring) · future (text disabled) · backfilled (small gallery glyph).
- **Sliver frame** (trim window): exactly 1 s of strip wide, 2.5px ember border, radius 10, medium glow. Dragging: scale 1.04, core color border. Lock-in: 3px + 400 ms pulse. Outside the frame the strip is dimmed 66%.
- Tab bar: floating pill 72 high, Archive · [Camera = bone ring with ember slit] · Films. Hidden on Camera, Trim, editors.
- Icons: 24 grid, 1.5 stroke, round caps, no fills (Lucide-compatible geometry).

## Motion
Curves: standard (0.2,0,0,1) · emphasized (0.3,0,0,1) · exit (0.4,0,1,1). Springs: lock-in stiffness 420 damping 28; drop 260/22.
Durations: instant 100 · quick 160 · base 240 · slow 400 · drop 650 · cinematic 1600.
Key moments: lock-in pulse 400 ms; save → clip flies into today's cell 650 ms; sheets 400 ms; premiere slit opens 1000 ms ease-out cubic, title fades 600 ms from 1.4 s; splash 1.4 s + 240 ms cross-fade.
Haptics: tick (strip every 0.25 s, slider steps) · snap/lock-in (medium impact) · shutter (record start/stop) · success (saved, film ready) · warning (failures).
Reduced motion: fly-ins → cross-fades, pulses → static brighten, premiere/splash jump to last frame. Haptics stay.

## Grain & texture
UI grain 3–5% static, only on background layers. Film grain only during playback. Blur only over the live camera.

## Copy tone
Short, warm, calm. Never guilt, never hype. "Open day", not "missed day". English UI.
