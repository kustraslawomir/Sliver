# T-record-button-v3 — record button: graphite metal, not plastic

Owner: the v2 button (`RecordButton.mp4`) looks plastic. Cause: big white gloss ellipse, orange subsurface blob at the bottom, saturated smooth gradient = candy. Replace the **look** of the button; keep its behaviour from T-glass-v1.7 item 7 (glow strength 0.6/1.0, press scale, morph to square, progress arc, timer pill, saved flash, haptics, Canvas, paused when the camera isn't RESUMED).

Reference: `design/png/RecordButtonV3.png` (graphite = the 2nd and 4th tile), layer values in `design/motion/RecordButtonV3.html` (classes `.B`).

Layers, 92 dp box, bottom to top:
1. Glow: much quieter — one radial, #FF3B30 at 30 % → 10 % at 45 % → 0, 150 dp; × 0.7 idle, × 1.0 recording. Drop the 3 drifting coloured blobs (they were part of the toy look). Keep a slow breathing of ±8 % alpha on a 4 s sine while the camera is open.
2. **Graphite bezel** 92 dp disc: sweepGradient #4A4A4F, #8D8D93 (10 %), #3A3A3F (22 %), #76767C (35 %), #2F2F34 (50 %), #83838A (63 %), #3D3D42 (76 %), #9A9AA0 (88 %), back to #4A4A4F — anodised metal with light catching in 4 spots. Fine concentric brushing: repeating rings 0.6 dp dark at 18 % every 1.6 dp, multiply, 35 % opacity. 0.75 dp inner edge white 40 %. Shadow 0/6/14 black 40 %.
3. Seat: 74 dp disc #141416, inner shadow 0/1.5/2 black 90 %, 0.75 dp highlight at the bottom edge white 55 % (machined lip).
4. Core 64 dp, **matte**: radial at (50 %, 40 %) #F5463A → #E2301F (60 %) → #C3200F; inner top highlight 1 dp white 22 %, inner shadow 0/−4/8 #460000 35 %. **No gloss ellipse, no subsurface glow.** Film grain on the core and bezel: fractal noise, overlay, 22–30 % (precompute a 128 px noise ImageBitmap once, tile it).
5. Recording: core → 32 dp square, corner 8 dp, seat stays — the dark seat around the small square is what makes it read as a real stop button. Progress arc on the bezel, r 41.5 dp, 2.5 dp white, glow 3 dp.
6. Photo mode: core #EDEDE8 → #CFCFC8 matte, same bezel.
7. Pressed: core moves down 1 dp and darkens 6 %, bezel unchanged (a real button sinks into the seat, it doesn't shrink).

Done = Xiaomi/emulator screenrecord of idle → record 1 s → saved on a bright and on a dark scene, next to `RecordButtonV3.png`, in `qa/v1.7/`. Never the Samsung.
