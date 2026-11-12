# Project rules (read by every agent — keep short)

- **Stack is Kotlin Multiplatform (Compose Multiplatform), non-negotiable.** Never create a native-only Android app. The old Android template was moved out (backup: `../Flash-android-template-backup`); only the Gradle wrapper, `gradle.properties` and `local.properties` remain.
- T01 builds the KMP project BY HAND (no IDE wizard) exactly per `docs/ARCHITECTURE.md`: root `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, module `composeApp` with `commonMain`/`androidMain`/`iosMain`/`commonTest`, package `app.sliver`. Use the newest stable Kotlin, Compose Multiplatform, AGP and Gradle that are mutually compatible (check official compatibility tables; update the wrapper if needed). Shared logic and UI live in `commonMain`; `androidMain` holds only platform implementations; `iosMain` only `TODO("iOS later")` stubs, never built.
- Source of truth = `docs/SPEC.md` (requirements, IDs R1..Rn), `docs/UI.md` (screens/tokens), `docs/TASKS.md` (ordered slices). **Never re-read the original zip/`.workflow/input` after distillation** unless a doc says "see input: <path>".
- Progress state lives in `.workflow/state.md`. Update it after every task. On start, read it first and resume.
- NEVER stop to ask the user. Missing secret/decision → use the documented default, put a fake/emulator behind an interface, log it in `HANDOFF.md`, continue.
- Never print, commit, or log secrets. Secrets only in `.env.local` / `local.properties` / `google-services.json` (all gitignored).
- Small vertical slices, one commit each: `feat(Tn): ...`. Push ONLY `git push origin auto/build` (never force, never main). Never deploy, never touch anything outside this repo.
- Verify with real commands (the Build/Tests/Lint commands in the product rules below), not by assumption. Max 2 fix attempts per failure, then mark task BLOCKED in `HANDOFF.md` and move on.
- Code style: minimal, idiomatic, no speculative abstractions, no comments restating code. Match UI tokens from `docs/UI.md` exactly.

# ---- Product rules v1.1 (from fix pack) — override stack line above ----
# SLIVER — rules for agents (v1.1)

Product: one-clip-a-day video diary, organised in projects. Read before coding:
`docs/SPEC.md`, `docs/DESIGN.md`, `docs/DATA.md`, `docs/ARCHITECTURE.md`, and the v1.1 docs: `docs/NAVIGATION.md`, `docs/TERMS.md`, `docs/PROJECTS.md`, `docs/UI-RULES.md`. **When v1.1 docs and older docs disagree, v1.1 wins.**

## Scope discipline
- Implement exactly the task you were given. No extra features, no speculative abstractions.
- Spec silent → simplest option, `// DECISION: …` comment, list it in your summary.
- No new dependencies unless needed — say why in the summary.
- Never invent API names; check real docs/source.

## Platforms
- KMP. Android is the only target we build, run and test. iOS = `TODO("iOS later")` stubs, never built.
- Pure logic in `commonMain` with unit tests; platform work in `androidMain` behind interfaces.

## Commands
- Build `./gradlew :composeApp:assembleDebug` · Tests `./gradlew :composeApp:testDebugUnitTest` · Lint `./gradlew :composeApp:lintDebug`
- Done = build + tests pass.

## UI rules (see docs/UI-RULES.md)
- Icons are vector icons from `core/icons/SliverIcons` — never letters or words pretending to be icons ("X", "v", "SND", "GRID" in a circle).
- Fonts come from `SliverTheme.typography` only. No `FontFamily.Default`, no `MaterialTheme.typography` in screens.
- Geist Mono only for dates, timecodes and counters. Buttons, chips and menus use Geist.
- Every primary action is single-flight: disabled + progress while running, can't fire twice.
- Every screen that loads media shows a loading state within 100 ms.
- Words: follow `docs/TERMS.md` ("clip", never "second" as a noun in UI; "Filters", never "Looks").

## Product rules — never break
- Dark only, one accent ember `#FF8A3D` (fill only on the primary button and record dot).
- No streaks, no notes, no mood, no paywall, no ads, no analytics.
- Offline-first; network only for the user's own Google Drive backup.
- Gallery originals are read-only. In-app recordings live in `tmp/` and are deleted after the clip is saved (unless "Also save full recordings to Photos").
- Filters and date stamp are never burned into stored clips — only into exports.

# Add to CLAUDE.md (v1.2)

- targetSdk 36. Never set `windowOptOutEdgeToEdgeEnforcement`, never rely on `onBackPressed`.
- Never request READ_MEDIA_IMAGES / READ_MEDIA_VIDEO. Get media through the system Photo Picker (`PickVisualMedia` / `PickMultipleVisualMedia`).
- Every screen must work at any window size ≥ 600 dp and survive recreation (state in ViewModel / SavedStateHandle). Use window size classes, not device checks.
- Long work (render, backup, restore, fill empty days) shows a progress notification using `Notification.ProgressStyle` on API 36+, normal progress notification below.
- State is never shown by color alone: add an icon, label or shape.

## Test device
Preference order, decided by the owner: 1) the Xiaomi (serial `b798509a`, M2101K9G), 2) the Samsung (serial `adb-RFCY30SW5QZ-XscZaw._adb-tls-connect._tcp`, model SM_*), 3) the emulator AVD `tracker13` (`~/Library/Android/sdk/emulator/emulator -avd tracker13`, serial emulator-5554, slow). Check `adb devices`, pick the first that is online, and always pass it via `ANDROID_SERIAL`; never touch the others. On Samsung use `am start --user 0`. Do not try to unlock a locked phone; if the chosen device is locked, fall through to the next one. Privacy (mandatory on every device): before every adb tap/screencap verify `dumpsys activity activities | grep topResumedActivity` contains app.sliver; never screenshot or commit images of the live camera preview, the system Photo Picker showing real media, real clip thumbnails/covers, the launcher or other apps; mask or avoid them. Never import, pull or delete the owner's real media; install only the debug app. Only the SliverDemo folders (Movies/SliverDemo, Pictures/SliverDemo) are safe demo media.

## Design fidelity (non-negotiable)
- UI tasks follow `PROMPT.md` in the pixel-perfect pack: inventory → build → screenshot → compare → checklist.
- Design files in `design/src` are the spec: 1 CSS px = 1 dp at 390 dp width.
- A UI task is not done without `docs/qa/<screen>.png` and a ✓/✗ checklist in the summary.
- No stock Material visuals. No Roboto. No text pretending to be an icon.
- PROMPT.md and GLOW.md live in docs/pixel-perfect/. Design wins on visuals; tools/ and design/ are at the repo root. Android module is composeApp (not androidApp).
