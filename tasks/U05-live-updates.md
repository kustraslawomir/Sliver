# U05 — Live progress notifications

Depends on: —

## Goal
Long work is visible and calm.

## Do
- One helper `ProgressNotifier` used by film render, backup, restore and fill empty days.
- API 36+: `Notification.ProgressStyle` — film render shows one segment per month (ember for done, gray for pending) and a point at the current month; backup/restore one segment. Tracker icon = slit. Follow the promoted-notification field requirements from the docs.
- Below API 36: standard ongoing notification with determinate progress.
- Tap opens the relevant screen (Export / Backup). Finished → replaced by the 'ready' notification.

## Done when
- [ ] Progress updates at most every 1 s, never stalls silently
- [ ] Build passes

Refs: CLAUDE.md (+ v1.2 addendum), docs/UX-2026.md, v1.1 docs
