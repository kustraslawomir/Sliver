# H01 — Hardening pass (from the code audit)

Do in order, one commit each, build+tests+lint green before each. Never ask questions.

## 1. Auto-pick speed
FrameSampler scores every frame at 10 fps (hundreds of `getScaledFrameAtTime` calls). Use a coarse pass (2-3 fps) for clips over ~10 s, refine around the winner, cap total frames, add a timeout with a sensible fallback (middle of the clip). Unit-test the sampling plan. Measure on the Samsung with a SliverDemo clip and put numbers in HANDOFF.md.

## 2. Save path
- `trimAudioToSecond`: no swallowed exceptions; check `renameTo`; verify the duration after trim; log failures; delete partial output on failure (also `runTransformer` throwing and `.hold.jpg`).
- Wrap the Transformer in `withTimeout`; on timeout show a Retry state.
- `SaveUndo.remove`: surface failure to the user, delete the day even if parking files fails; reclaim parked files on next launch.
- Replace `println("SliverSave failed")` with the DebugLog helper.
- Extract the duplicated finalize logic in AndroidClipExporter.

## 3. Backup + migration
- Backup manifest gets projectId and project metadata (name, format, start date); restore recreates/targets the right project, not the active one. Drive folder `SLIVER/<project name>/`. Keep restoring old manifests (no projectId -> active project).
- `Migration2To3`: if no project row exists but days do, insert a default project first. Test with a fixture that has days and an empty project table.

## 4. Picker access + errors
- Copy picked media into app storage at pick time (or take persistable access) so Fill empty days survives process death; stream, do not read whole files into memory (`extractMotionVideo` readBytes).
- Show "could not read N items" and an error state for failed adds (AddMedia, Backfill describe) instead of silent no-ops.

## 5. Quick hygiene (small, safe)
- Gate Log.d in CameraEngine/FrameSampler behind BuildConfig.DEBUG.
- Remove `SCHEDULE_EXACT_ALARM` and `POST_PROMOTED_NOTIFICATIONS` if the code does not strictly need them (use inexact reminders; keep the behaviour); add `dataExtractionRules`.
- Finish the internal rename `second` -> `clip` in identifiers and the `year-in-seconds` route; delete unreachable routes/keys left from dropped features (Filters `camera.look` key stays until the Filters decision is final: leave stored data).
- App-level CoroutineScopes: one app scope with an exception handler.

## Not in scope (needs the owner)
R8 + release signing (needs a keystore), real fonts/music.

## Done when
Each item fixed or recorded in HANDOFF.md with the reason; build + tests + lint green; section 'Hardening' in HANDOFF.md.
