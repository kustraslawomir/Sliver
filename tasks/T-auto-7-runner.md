# T-auto-7: start the builder automatically on new tasks

There is a self-hosted GitHub Actions runner on the Mac (`~/actions-runner`).

- Workflow `.github/workflows/builder.yml`: on push to `auto/build` that adds or changes
  `tasks/*.md`, run on the self-hosted runner: `claude -p` (headless) with the builder prompt
  from `.workflow/` and the changed task file, max turns/budget set in the workflow.
- Before it starts: `scripts/design-lint.sh`, and if the Xiaomi or an emulator is available, after it finishes (never the Samsung):
  `scripts/qa.sh` (T-auto-2) and attach `docs/qa/<date>/REPORT.md` to the run summary.
- One run at a time (concurrency group), never on `main`, and it never force-pushes.
- Secrets only from the runner's environment, nothing in the repo.
Done when: pushing a new task file starts the builder without anyone typing "go".
