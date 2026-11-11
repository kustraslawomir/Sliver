# P-project — Project screen 1:1

Follow PROMPT.md. Design source of truth: design/src/ProjectHub.dc.html (month view, menu, add-to-empty-day sheet) and ProjectHubYear.dc.html (year view + daily-nudge card). Archive*.dc.html and Films.dc.html are old v1.0: do NOT use them.

ENTRY / NAV: opened from the calendar chip on Camera; no tab bar. Chevron-down and system/predictive back return to Camera. Project name + chevron -> project switcher (Projects sheet). Gear -> Settings. Menu -> dropdown anchored top-right, scrim closes it.

CONTENT (scrollable under a fixed header)
1. Header: name Instrument Serif 30; mono line "{N} CLIPS · SINCE {d MMM} · {FORMAT}".
2. Month/Year segmented control. Month view prev/next limited to project start -> current month (disabled = #3A3A40).
3. Nudge card: only when no reminder is set AND not dismissed (DataStore flag). "Set reminder" -> Notifications settings. "No thanks" -> set flag, hide for good.
4. Month grid, 7 columns, Monday first. filled -> clip thumbnail + day number -> Clip detail; empty past day after project start -> dark cell + dot -> add sheet; today without clip -> 2dp ember ring -> Camera; future / before start -> dimmed number, not tappable.
5. Year view: 12 rows x up to 31 cells. Tapping a month with days opens it in month view.
6. Memory card: clip from exactly 1 month ago; hidden if none. Tap -> Clip detail.
7. Films: "Make film" -> Film builder. Posters 132x198 horizontal row, tap -> Player. No films: only header + button.

ADD SHEET (empty day): mono date label, "Add a clip for this day", "Choose from Photos" -> SYSTEM Photo Picker (PickVisualMedia, no READ_MEDIA_*), then Edit clip with target date = that day. "Leave it empty" closes.

MENU: Rename, Change shape (both -> existing project edit form), Fill empty days -> Backfill (opt-in only, never suggest elsewhere), Archive project, Delete project (red #E5675F). The delete confirmation is NOT in the design: do not invent a new one; keep the existing F04 confirm dialog unchanged and list under "Needs from you" in HANDOFF.md that the owner must supply the design.

DATA: Room queries per project (clips by month as Flow, count, first clip date, films). No new tables unless strictly necessary; say which and why.

DONE WHEN: tools/screenshot.sh + tools/compare.py for month view, year view with nudge card, menu open, add sheet open, within ±2 dp, exact colors/fonts/copy/icons; comparison PNGs in docs/qa/project/ (use emulator-5554, ANDROID_SERIAL; design/png has no PNGs, so render the HTML boards via a headless browser if available, otherwise measure from HTML and say so). No Material defaults visible. Report what could not be matched and why.
