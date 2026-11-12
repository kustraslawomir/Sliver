# T-auto-2: one-command device QA (Maestro)

Goal: "check it on the phone" = one command, no hands.

- Add Maestro flows in `qa/flows/`: home, camera, recording (2 s), edit clip, save, day,
  projects sheet, year view, reminders, settings, film player.
- `scripts/qa.sh [device]`: build + install debug → seed (T-auto-1) → run the flows in
  **dark** and **light** (`adb shell cmd uimode night yes|no`) and at font scale **1.0 and 1.3**
  (`adb shell settings put system font_scale 1.3`) → screenshots into `docs/qa/<date>/<theme>-<scale>/`
  → restore the device settings.
- Then run `tools/compare.py` for each screenshot against `design/png/<Screen>.png` and write
  `docs/qa/<date>/REPORT.md` (side-by-side links + mismatch score per screen).
- Default device: the **Xiaomi** over USB, otherwise an **emulator** (Pixel, API 36), picked automatically by `scripts/qa.sh`. The Samsung is used only with an explicit `scripts/qa.sh --device samsung` for the final pre-release check, never by default, and the script must never wipe it or leave its settings changed.

Done when: `scripts/qa.sh` runs end to end on the Xiaomi and on the emulator and produces the report.
