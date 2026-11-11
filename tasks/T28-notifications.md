# T28 — Local notifications

Milestone: M5 App  
Depends on: T04, T03

## Goal
Reminders and system events, all local.

## Do
- Channels: Reminders, Films, Backup, Memories. POST_NOTIFICATIONS handling.
- Pure rules in commonMain (tests): next fire times for FIXED (chosen weekdays) and RANDOM (random minute inside window, one per day for 7 days), skip days that already have a second.
- Android scheduler: exact alarm if SCHEDULE_EXACT_ALARM granted, else setWindow ±10 min; reschedule on app open, boot and settings change. Saving today's second cancels today's reminder + Running late.
- Notifications with copy from SPEC; tap opens Camera / film / backup / day.
- Settings screen + denied banner.

## Done when
- [ ] Rule tests pass
- [ ] Reminder fires on device and is skipped after capture
- [ ] Build passes

Refs: CLAUDE.md, docs/SPEC.md, docs/DESIGN.md, docs/DATA.md, docs/ARCHITECTURE.md
