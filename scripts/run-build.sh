#!/usr/bin/env bash
# One-shot launcher: ./scripts/run-build.sh ~/Downloads/app.zip [max_usd]
# Headless: anything not allow-listed in .claude/settings.json is denied (never prompts).
set -euo pipefail
ZIP="$(cd "$(dirname "${1:?usage: run-build.sh <zip> [max_usd]}")" && pwd)/$(basename "$1")"
BUDGET="${2:-60}"
cd "$(dirname "$0")/.."
mkdir -p .workflow
claude -p "/build-app $ZIP" \
  --model sonnet \
  --permission-mode acceptEdits \
  --max-budget-usd "$BUDGET" \
  2>&1 | tee .workflow/run.log
echo; echo "Done. Read HANDOFF.md"
