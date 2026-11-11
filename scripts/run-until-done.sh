#!/usr/bin/env bash
# Retry wrapper: re-launches /build-app until every task in .workflow/state.md is ticked.
# Usage: ./scripts/run-until-done.sh <zip> [usd_per_attempt=20] [max_attempts=8] [wait_min=30]
# Waits between attempts (covers usage-limit windows). Safe to Ctrl-C and re-run: state is in files.
set -uo pipefail
ZIP="$(cd "$(dirname "${1:?usage}")" && pwd)/$(basename "$1")"
USD="${2:-20}"; MAX="${3:-8}"; WAIT="${4:-30}"
cd "$(dirname "$0")/.."; mkdir -p .workflow
pending() { [ -f .workflow/state.md ] && grep -q '^- \[ \]' .workflow/state.md; }
for n in $(seq 1 "$MAX"); do
  if [ -f .workflow/state.md ] && ! pending; then echo "All tasks done."; exit 0; fi
  echo "=== attempt $n/$MAX $(date '+%H:%M') ===" | tee -a .workflow/run.log
  claude -p "/build-app $ZIP" --model sonnet --permission-mode acceptEdits --max-budget-usd "$USD" </dev/null 2>&1 | tee -a .workflow/run.log
  if [ -f .workflow/state.md ] && ! pending; then echo "All tasks done."; exit 0; fi
  echo "Not finished. Waiting ${WAIT} min before retry..." | tee -a .workflow/run.log
  sleep $((WAIT*60))
done
echo "Gave up after $MAX attempts. Re-run to continue."
