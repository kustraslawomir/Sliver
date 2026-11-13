# Builder automation

`.github/workflows/builder.yml` runs on the self-hosted runner (`~/actions-runner`).

What it does: on a push to `auto/build` that adds or changes `tasks/*.md` (or a manual
`workflow_dispatch` with a task file), it runs `scripts/design-lint.sh`, then headless
`claude -p` with the body of `.claude/agents/builder.md` plus "Implement <task file>"
(`--max-turns 150`, job timeout 180 min), pushes `auto/build` (no force; one
`pull --rebase` retry), and, if the Xiaomi (`b798509a`) or an emulator is online (never the
Samsung), runs `scripts/qa.sh --skip-build` and appends `docs/qa/<date>/REPORT.md` to the run
summary. One run at a time; never on `main`. Auth comes from the runner's environment only.

Enable (owner): `gh variable set BUILDER_AUTORUN --body true`
Disable: `gh variable set BUILDER_AUTORUN --body false` (or `gh variable delete BUILDER_AUTORUN`).
Skip one push: put `[skip builder]` in the head commit message.

Warning: while an interactive Claude session is also committing to `auto/build`, the two can
collide (rebase conflicts, duplicated work). Disable the variable first.
