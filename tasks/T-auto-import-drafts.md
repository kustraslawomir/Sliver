# T-auto-import-drafts: fill gaps from the gallery as drafts (owner request, 2026-11-12, due next morning)

Owner: "Auto-import into the calendar gaps, e.g. import 300 drafts; the user can edit them or swap for another
item from that day inside the app (not the gallery). Photos too when the day has no video. In the editor both
are available. Mark them as drafts; the user must go through them in the editor. Let the user pick the
'from' date."

Owner answers (binding):
- Full gallery access: READ_MEDIA_IMAGES + READ_MEDIA_VIDEO (+ READ_MEDIA_VISUAL_USER_SELECTED). Asked only
  when the user starts auto-import. CLAUDE.md is updated. Partial access → import only what's visible and show
  "Allow all" banner (reuse the Backfill limited-access banner).
- Default draft per day: the longest video of that day; motion photo counts as video; no video → a photo.
  1 s window: motion photo → centred on its presentation timestamp; video → middle; photo → Still.
- Drafts do NOT count until confirmed: excluded from films, the year chart/light of the year, memory card,
  reminder skip-if-done, streak-free stats, backup/sync (sync later can carry them, but not now).
- Run only when the user starts it (no nightly job).

## Entry
Project screen `⋯` menu → "Auto-import from…" (and Settings → the existing "Fill empty days" row, which this
replaces/extends). Sheet: date picker "From" (default = project start; can't be after today), shows
"N empty days since <date>" live, button "Import drafts". Uses the Material3 DatePicker styled with our tokens
(no stock Material look, Geist, mono accent).

## Scan + import (background)
- MediaStore query (images + video) by DATE_TAKEN in [from, yesterday] for the active project; group by local
  date (device zone, same rules as CaptureDate; ignore DATE_MODIFIED-only items per T-media-any-source §5).
- Only days with NO day row (filled or draft) get a draft. Never touch filled days.
- For each gap day: pick the default (rules above), create the 1 s clip through the SAME save path as a real
  save (DayRepository.saveClip / photoToClip) so thumbs, luma/tone work, and store `draft = true` plus the
  list of that day's candidate MediaStore ids (`candidates`, capped at 30, ordered: videos by length, then photos).
- WorkManager job, progress notification (`Notification.ProgressStyle` on API 36+, normal below), resumable,
  cancelable; ~300 days must finish without OOM (process sequentially, release decoders).
- "Undo all" for one run (removes only drafts created by that run).

## Data
Room migration 4→5: `draft` (Boolean, default false), `draftCandidates` (String JSON, nullable),
`importRunId` (String, nullable). Migration test. Backup/restore and sync ignore drafts (skip rows with draft=true).

## Calendar marking
Draft cell = thumbnail at reduced opacity + dashed 1.5 dp outline + small "Draft" label (Geist 9–10 sp,
not uppercase) — never colour alone. Project header shows "12 drafts to review" chip → opens the first draft.
Accessibility: "10 November, draft, review".

## Editor (Edit clip / Photo clip) for a draft
- Top: "Draft · 3 of 12" with Next/Previous draft navigation.
- A horizontal strip "Other items from this day" with all candidates (video thumbnails with duration badge,
  photos, motion photos with a motion badge). Tapping one swaps the source in place (no gallery app, no picker).
- Video ↔ Photo: for a motion photo or when the day has both, the "Video · Photo" control from
  T-media-any-source §1 (implement it here if that task hasn't landed).
- Primary button "Keep this clip" (confirms: draft=false, normal day) — single-flight; secondary "Skip day"
  (deletes the draft, leaves the day empty, with undo toast). After confirming, jump to the next draft.
- Drafts never show the 6 s save-undo bar per item; "Undo all" lives on the run.

## Tests
Grouping by date across zones; default pick rules; gap-only (filled days untouched); draft excluded from
films/year/memory/reminder; confirm/skip; undo run; migration 4→5.

## Device check (Xiaomi or emulator only; NEVER the Samsung)
Only with SliverDemo media: put synthetic videos/photos/motion photos with EXIF dates spread over 2–3 weeks into
`Movies/SliverDemo` / `Pictures/SliverDemo`, run auto-import from a date in a SEEDED test project, review two
drafts (swap one candidate, keep one, skip one), Undo all. Screenshots (dark + light) of the sheet, the calendar
with drafts, the draft editor — only synthetic media in frame. Never import the owner's real media during tests:
the permission grant on the Xiaomi must be revoked afterwards (`pm revoke`) and seeded/test data wiped.

## Done when
Build + tests + lint + designLint + verifyRoborazziDebug green (re-record affected goldens), device check done,
HANDOFF section "T-auto-import-drafts" with limits, docs/SPEC.md updated (replace "Fill the past" with this).
