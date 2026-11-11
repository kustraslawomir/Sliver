#!/usr/bin/env bash
# If the zip already ships docs/ + tasks/ (+CLAUDE.md), adopt them verbatim. Exit 0 = adopted, 1 = needs distill.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; IN="$ROOT/.workflow/input"
SRC="$(find "$IN" -maxdepth 2 -type d -name tasks -exec dirname {} \; | head -1)"
[ -n "$SRC" ] && [ -d "$SRC/docs" ] || exit 1
mkdir -p "$ROOT/docs" "$ROOT/tasks"
cp -R "$SRC/docs/." "$ROOT/docs/"; cp -R "$SRC/tasks/." "$ROOT/tasks/"
if [ -f "$SRC/CLAUDE.md" ] && ! grep -q "rules for agents" "$ROOT/CLAUDE.md"; then
  { echo; echo "# ---- Product rules (from input plan) — override stack line above ----"; cat "$SRC/CLAUDE.md"; } >> "$ROOT/CLAUDE.md"
fi
echo "adopted: $(ls "$ROOT/tasks" | grep -c '^T[0-9]') tasks"
