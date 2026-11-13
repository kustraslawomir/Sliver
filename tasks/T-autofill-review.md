# T-autofill-review: auto-filled days in the calendar, pick/cut per day, abort the rest (owner request, 2026-11-13)

Owner: "After Fill all, switch to the calendar. In the calendar each auto-filled day has a frame (draft-like
marker) and the number of media from that day. If I click it, I can choose exactly which media, cut it, or
remove/drop the day. Of course with an option to abort it for the items I didn't pick."

## Model (no DB migration — reuse existing columns)
- An **auto day** = a day saved by an auto-import run in "Keep without review" mode: `draft = false`,
  `importRunId != null`, `draftCandidates` = that day's gallery items (already stored by the executor).
  Auto days count everywhere (films, year chart, memory, reminders, sync) — they are real days.
- **Reviewed** = the user touched it (picked/cut/kept) → `importRunId = null`, `draftCandidates = null`
  (keep candidates if the editor needs them while open; clear on save).
- Drafts (keep mode off) keep working as today.

## 1. After Fill all
When a keep-mode run finishes, the Auto-import screen navigates to the Project calendar automatically
(month of the first filled day) and shows a toast "N days filled · Check them in the calendar".

## 2. Calendar marking
Auto day cell: thumbnail at full opacity + dashed 1.5 dp outline (same dash as drafts, but no dimming) +
a small count badge bottom-right with a stack icon and the number of media of that day (e.g. "4"), hidden when 1.
Never colour alone (outline + badge). Accessibility: "<date>, auto-filled, 4 items, review".
Project header chip: "N auto-filled days to check" (replaces/joins the drafts chip; one chip listing both kinds).

## 3. Tap an auto day (or a draft) → Day media sheet
Bottom sheet "Tue 10 Nov · 4 items":
- Grid (3 columns) of all candidates of that day: video thumbnails with duration badge, photos, motion photos with
  a motion badge; the current pick has a check + outline. Use the thumbnails already used by the draft editor strip.
- Tap an item → opens Edit clip / Photo clip with it (existing DraftActions.open with that uri) to cut the 1 s;
  saving marks the day reviewed.
- Buttons: **Keep** (marks reviewed, no edit), **Remove day** (red text; deletes with the existing undo toast).
- Close/back = nothing changes.

## 4. Abort the ones I didn't pick
Chip sheet "N auto-filled days to check": **Keep all** (mark all reviewed), **Remove unchecked** (red text:
deletes every still-auto day of the project — only `importRunId != null`, never reviewed or camera days — with
one undo toast "N days removed · Undo"), **Check one by one** (opens the first auto day's sheet; after an action
it moves to the next).
Also on the Auto-import run-finished screen: "Keep all" / "Remove unchecked".

## Tests
Unit: auto-day predicate; Keep all / Remove unchecked touch only auto days of the active project; undo restores;
reviewed day leaves the set after edit-save; count badge text. Roborazzi: calendar with auto days (synthetic),
the day media sheet, the chip sheet — dark + light.

## Device check (Xiaomi or emulator, NEVER the Samsung; synthetic media in SliverDemo only)
Fill a seeded test project in keep mode → lands on calendar with marked days and counts → open one, pick a
different item, cut, save → marker gone; Remove unchecked → only unreviewed auto days removed; Undo works.
