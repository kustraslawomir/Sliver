# F10 — Edit clip: loading, sound, save

Depends on: F05, F09

## Goal
The edit screen is fast, clear and can't get stuck.

## Do
- Loading state for big files (screenshot edit-loading.jpg): skeleton preview + strip, 'Preparing video… 42%' (steps: open, thumbnails, auto-pick), file info ('4:32 · 812 MB'); strip fills in progressively; CTA shows 'Preparing…' disabled.
- Preview loop is **muted by default**; sound icon toggles it (this also sets keepAudio for the saved clip, default on → show 'Clip keeps its sound' hint once).
- Remove the 1× / 0.5× switch (and speed from the model usage).
- Primary 'Save clip': single-flight → 'Saving…' with progress → pop to flow origin with toast (F05). Never stays on the screen after saving.
- Header: close icon (x), date chip 'TODAY ▾' with chevron icon, sound icon. Secondary: Retake · Filters.

## Done when
- [ ] Big file (> 500 MB) shows progress within 100 ms
- [ ] Save works once per tap and returns to Camera
- [ ] Build passes

Screenshots: docs/screenshots/edit-rotated.jpg, docs/screenshots/edit-loading.jpg

Refs: CLAUDE.md, docs/NAVIGATION.md, docs/TERMS.md, docs/PROJECTS.md, docs/UI-RULES.md
