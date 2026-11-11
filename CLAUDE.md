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

# ---- Product rules (from input plan) — override stack line above ----
# SLIVER — rules for agents

Product: one-second-a-day video diary. Read before coding:
`docs/SPEC.md` (behavior), `docs/DESIGN.md` (look), `docs/DATA.md` (data + file rules), `docs/ARCHITECTURE.md` (code layout).

## Scope discipline
- Implement exactly the task you were given. No extra features, no speculative abstractions, no "for later" layers.
- If the spec is silent, pick the simplest option, leave a `// DECISION: …` comment and list it in your summary.
- Don't add dependencies that aren't in ARCHITECTURE.md unless you must — then say why in the summary.
- Don't refactor code outside the task unless it blocks you.
- Unsure about a library API? Check the real docs/source. Never invent API names.

## Platforms
- KMP project. **Android is the only target we build, run and test.**
- iOS: `iosMain` gets `actual` stubs that `TODO("iOS later")`. Never try to build or test iOS.
- Pure logic (dates, scoring, scheduling rules, film plans, Drive REST calls, state machines) goes in `commonMain` with unit tests.
- Platform work (CameraX, Media3, WorkManager, notifications, Glance, MediaStore, Google auth, haptics) goes in `androidMain` behind the small interfaces listed in ARCHITECTURE.md.

## Commands
- Build: `./gradlew :composeApp:assembleDebug`
- Tests: `./gradlew :composeApp:testDebugUnitTest`
- Lint: `./gradlew :composeApp:lintDebug` — fix warnings you introduced.
A task is done only when build + tests pass.

## Code style
- Kotlin, Compose Multiplatform, Koin, coroutines/Flow, kotlinx-datetime, kotlinx-serialization.
- Per feature: `feature/<name>/` with `XScreen.kt` (Composable), `XViewModel.kt` (StateFlow<XUiState>), plus repository use. Keep it flat.
- No hardcoded colors/dimens in screens — use `SliverTheme` tokens.
- User-facing strings in English, in compose resources `strings.xml`.
- Tests: `kotlin.test` in `commonTest` for logic. No UI/instrumented tests unless the task says so.
- Small files, clear names, no comments that restate the code.

## Product rules — never break these
- Dark only. One accent: ember `#FF8A3D`. Ember is a fill only on the primary button and the record dot.
- No streaks, no guilt copy, no day notes, no mood tags, no paywall, no ads, no analytics, no tracking.
- Works fully offline. Network is used only for the user's own Google Drive backup.
- We never upload video anywhere except the user's own Drive.
- Gallery originals are read-only: never modify or delete them.
- In-app recordings live in `tmp/` and are deleted after the chosen second is saved (unless "Also save full recordings to Photos" is on).
- Look and date stamp are never burned into day files — only into exports.
