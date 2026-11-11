---
description: Build the whole app from a zip of UI + business requirements, fully unattended
argument-hint: <path-to-zip>
---
You are the ORCHESTRATOR. Goal: turn the zip at `$ARGUMENTS` into a working, tested app in this repo, without ever asking the user anything or waiting. You (orchestrator) plan, delegate, and keep state; you do NOT write feature code yourself except trivial glue.

First: if `.workflow/state.md` exists, read it and RESUME at the first unfinished phase/task. Otherwise start at Phase 0.

## Phase 0 — Intake (cheap, scripted)
Run `bash scripts/intake.sh "$ARGUMENTS"`. It unzips to `.workflow/input/`, writes `.workflow/intake.md` (file tree, sizes, tool/credential availability). Read only that report.

## Phase 1 — Plan
Run `bash scripts/adopt.sh`. If it prints "adopted: N tasks", the input already ships a plan: use `docs/*`, `tasks/INDEX.md` and `tasks/Txx-*.md` verbatim as the task list (INDEX order; respect dependencies), skip distillation, and just write `.workflow/state.md` (checklist of task IDs) and `docs/NEEDS.md` (external deps: credentials, assets, with the fallback used). Read only `tasks/INDEX.md` and the docs headings needed for NEEDS.
If it exits 1 (no plan in the input), distill yourself ONCE: `docs/SPEC.md` (R1..Rn, MUST/SHOULD), `docs/UI.md`, `docs/TASKS.md` (8–20 vertical slices with testable acceptance criteria, every MUST mapped), `docs/NEEDS.md`. Ambiguity → conventional option, recorded under "Assumptions". Task files then live at `docs/TASKS.md`.
The stack is KMP (see CLAUDE.md); the old Android template is already moved out. `git init` if needed and commit a baseline; T01 creates the KMP project from scratch.

## Phase 2 — Credentials preflight (NON-BLOCKING)
Per `docs/NEEDS.md`, check what exists (`.env.local`, `google-services.json`, `firebase login:list`, local.properties) — never print values. For each missing item: generate/extend `.env.local.example` and a fake/emulator-backed implementation behind an interface; append to `HANDOFF.md` under "Needs from you" the exact step to plug in the real value. Continue immediately.

## Phase 3 — Build loop
Ensure git repo exists (`git init` if not) and work on branch `auto/build`. For each task in order:
1. Delegate to subagent `builder` with just the task ID (its file is `tasks/<ID>-*.md`, or its block in `docs/TASKS.md`) (plus a one-line hint if a prior task changed an API it depends on).
2. Update `.workflow/state.md` (DONE / BLOCKED).
3. After every 3 tasks and after the last, delegate to `verifier` with the finished IDs. On FAIL: send the failure line back to `builder` once (a "fix T<n>" task). Still failing → BLOCKED in `HANDOFF.md`, move on. Never loop more than that. After each verifier gate, run `git push -u origin auto/build` (if it fails, e.g. no credentials, note it once in `HANDOFF.md` and stop trying).
Do not run builders in parallel (shared files/Gradle lock). Do not re-read builder-written code; trust the verifier's table.

## Phase 4 — Close out
1. Delegate a final `verifier` run over ALL tasks.
2. Write `HANDOFF.md`: ✅ done tasks, ⛔ blocked tasks + why, requirement coverage (R-ID → task → status), "Needs from you" (credentials/steps), assumptions, how to run (`./gradlew installDebug`), emulator instructions.
3. Final commit. Print a ≤15-line summary. Stop.

## Cost rules
- Never paste file contents into delegations; pass IDs and doc paths.
- No exploratory reading of the whole repo. Grep, then read ranges.
- Keep your own replies short; state lives in files, not in conversation.
