# T-sync-two-devices: one account, two phones, same projects (owner request, 2026-11-12)

## Today (why this task exists)
Backup is push-only. With the same Google account on two phones:
- both upload their days into the same project folder but never pull; each phone only sees its own
  clips (the other phone's clips arrive only via Restore on an empty app);
- `manifest.json` per project is rewritten by whichever phone backs up last, so films and project
  settings from the other phone are lost;
- the same date recorded on both phones gives two `<date>.mp4` files in one folder; Restore picks one.
Drive `drive.file` access works across devices (files created by the same Cloud project/user).

## Model
- Drive stays the shared store; no server, no Firebase (product rule: network only for the user's own Drive).
- Each install has a stable `deviceId` (UUID, DataStore, never synced) and a monotonic `updatedAt` per record.
- Records and their Drive form:
  - **Project** (id, name, format, start, archived, updatedAt) → `projects/<projectId>.json` in the root folder.
  - **Day** (projectId, date, clip, thumb, look, motion, keepAudio, source, luma, tone, updatedAt, deviceId,
    contentHash) → `<project folder>/<date>.mp4` with all fields in `appProperties`. One file per (project, date).
  - **Film definition** (id, projectId, name, options, updatedAt) → `<project folder>/films/<filmId>.json`.
    Renders are never uploaded; each phone renders locally (already so).
  - **Tombstones** for deletes: `<project folder>/deleted/<date>.json` or `films/<id>.deleted.json`
    with `deletedAt`; kept 90 days, then garbage-collected.
- One file per record = no shared manifest to overwrite = no lost updates. Remove the per-project
  `manifest.json` writer (keep reading it once to migrate old backups).
- Device-local, never synced: camera state, reminders, appearance, golden hour, onboarding flags.

## Sync cycle (`SyncEngine`, replaces BackupEngine.run)
1. **Pull**: Drive `changes.list` with a saved `startPageToken` (first run: full listing of the root
   folder). For each changed record: if remote `updatedAt` > local `updatedAt` → apply (download clip
   to `days/`, regenerate thumb if needed, compute luma/tone via the existing path). Tombstone newer
   than local → delete locally (same code as a user delete, no undo toast).
2. **Merge rule**: last writer wins per record by `updatedAt`, tie-break by `deviceId`. A day replaced
   by the other phone shows a one-time notice on next open: "10 Nov was changed on another phone".
   The losing clip is not destroyed: move it to Drive `<project>/replaced/<date>-<deviceId>.mp4` (30 days).
3. **Push**: local records with `updatedAt` > last pushed → upload / update / write tombstone.
4. Save the new page token and `lastSyncAt` only after both steps succeed (idempotent re-run).
- Same project created separately on both phones (different ids, same name): never auto-merge;
  show both, user can merge later (out of scope; note it).

## Triggers
App start (if signed in, Wi-Fi or "use mobile data" setting), after a save/delete (debounced 30 s),
WorkManager periodic 1 h with network constraint, pull-to-refresh on the Project screen.
No push notifications (no server), so the other phone updates within the next trigger.

## UI
- Settings → Backup becomes "Backup & sync": status line "Synced · 2 min ago · 2 phones"
  (phones = distinct deviceIds seen in the last 30 days), Sync now, errors as today.
- Restore on a fresh install = first pull.
- Signed-in second phone with local data: first sync merges by the rules above (no wipe).
- Long syncs show the progress notification per CLAUDE.md (ProgressStyle on API 36+).

## Tests (commonTest, two in-memory devices sharing one InMemoryDriveClient)
Record on A → appears on B; same date on both offline → newer wins, loser in replaced/;
delete on A → gone on B; rename project on B → renamed on A; film created on A → definition on B,
rendered locally; interrupted pull/push resumes without duplicates; old manifest-based backup migrates.

## Done when
Build + tests + lint + designLint + verifyRoborazziDebug green; device check with two devices
(Xiaomi + emulator signed in to the same test account) shows a clip recorded on one appearing on the
other after "Sync now"; HANDOFF section "T-sync-two-devices" lists limits (no instant push, conflict rule).
