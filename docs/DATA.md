# SLIVER — data & files

## Local database (Room KMP)
Day
- date: LocalDate (PK, local calendar date)
- file: String (relative path `days/2026-11-10.mp4`)
- thumb: String (`thumbs/2026-11-10.jpg`, 320px JPEG)
- source: CAMERA | GALLERY | PHOTO | BACKFILL
- look: LookParams (embedded JSON): preset (NATURAL…GHOST), intensity 0–100, sharp −100..100, contrast −100..100, grain 0..100, vignette 0..100, fade 0..100, saturation −100..100
- motion: STILL | ZOOM (photo days only)
- keepAudio: Boolean (default true) · speed: 1.0 | 0.5
- color: Int (average color of the thumbnail, for the year view)
- cropY: Float? (0..1, vertical offset of the 16:9 crop for portrait clips; null = center)
- takenAt: Instant · createdAt: Instant
- driveFileId: String? · backup: PENDING | DONE | FAILED

Film
- id: String (uuid) · name · format CINEMA | STORY · range: Week/Month/Year/Custom(from,to) · live: Boolean (keeps collecting)
- audio ORIGINAL | MUSIC | MIX · track: String? (asset id or content uri) · cut CUT | FADE · dateOn: Boolean · look: preset or NONE · mismatch CENTER | BLUR
- renderPath: String? (`films/<id>.mp4`) · renderedAt: Instant?

Settings (DataStore)
- defaultLook (NATURAL) · newFilmFormat (CINEMA) · dateStamp (true) · saveFullRecordings (false)
- reminder {mode FIXED|RANDOM|OFF, time 19:30, window 10:00–20:00, days [7×true]}
- notif {runningLate false, filmReady true, backupProblems true, onThisDay true}
- camera {mode VIDEO|PHOTO, look, lens BACK|FRONT, flash, grid, activeFilmId} — **timer is never stored**
- onboardingDone, coachTrimSeen, firstDay: LocalDate?
- account {signedIn, email?, driveFolderId?, autoBackup true, wifiOnly true}

## File layout (app-private storage)
```
files/days/YYYY-MM-DD.mp4     one per day, 1080p, 1.000 s, H.264/AAC, no look/date burned in
files/thumbs/YYYY-MM-DD.jpg
files/films/<filmId>.mp4      rendered films
cache/tmp/rec_<ts>.mp4        in-app recordings before trim
cache/proxies/                optional later (540p) — not in v1
```

## File rules
1. In-app recording → `tmp/` (never MediaStore) → Trim → export exact 1.000 s at 1080p → `days/` → thumb → delete tmp **after** the 6 s undo window. If `saveFullRecordings` → insert the tmp file into MediaStore Movies first.
2. Gallery video/photo → read only via content URI → export new 1 s file to `days/`. Never write to or delete the original.
3. Retake / leaving Trim without saving deletes the tmp file.
4. On app start: delete `tmp/*` older than 24 h.
5. Replace day: write new file, keep old until undo expires, then delete old + trash its Drive copy.
6. Remove day: same, no new file.
7. Trims re-encode (passthrough cuts snap to keyframes).

## Google Drive (backup, optional)
- Scope `https://www.googleapis.com/auth/drive.file` only.
- Folder "SLIVER" in My Drive (visible). Folder id stored in settings.
- One file per day: name `YYYY-MM-DD.mp4`, `appProperties: {sliverDate, look (json), source, motion}`.
- `manifest.json` in the folder: settings + films (no media). Overwritten on change.
- Restore: list files in folder (q = `'<folderId>' in parents and trashed = false`), rebuild Day rows from appProperties, download in background, regenerate thumbs, films re-render lazily.
- Error mapping: 403 `storageQuotaExceeded` → DRIVE_FULL · 401 / `invalid_grant` → REVOKED · no network → OFFLINE · Wi-Fi only + metered → PAUSED_WIFI · other → FAILED (retry 15 min, exponential).
- No Firebase, no own server. "Delete account" = revoke token + optional delete of the SLIVER folder + optional local wipe.
