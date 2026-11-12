# T-auto-5: render the design boards locally

Today the PNGs in `design/png` are made in the design session. Move that into the repo so
anyone (you, the builder, CI) can render a board.

- `tools/design/render.py <Board|all>` → `design/png/<Board>.png` (phone boards @3x = 1170 px
  wide, big boards @1x). Python + Playwright (Chromium). Fonts from `composeApp` resources
  (Geist, Geist Mono for old boards); block all network.
- The boards are `.dc.html`: a template with `{{dotted.lookups}}`, `<sc-for list as>`,
  `<sc-if value>`, `<dc-import name prop="…">` and a `class Component extends DCLogic`
  with `renderVals()`. Write a small static renderer (~150 lines of JS) that: parses the file,
  runs `renderVals()` with the defaults from `data-props`, expands sc-for/sc-if/dc-import,
  interpolates holes in text and attributes, drops `on*` handlers. Ask the design session for
  its reference implementation if you want it (`dcrender.js` + `export_png.py`).
- `scripts/design-png.sh` re-renders only boards whose `.dc.html` changed (git diff).
Done when: rendering all boards reproduces the committed PNGs within a small pixel tolerance.
