#!/usr/bin/env bash
# Re-render design/png for boards whose .dc.html changed vs git (HEAD, plus untracked). Usage: scripts/design-png.sh [base-ref]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
BASE="${1:-HEAD}"
changed="$( { git diff --name-only "$BASE" -- design/src; git ls-files --others --exclude-standard design/src; } | sort -u | grep '\.dc\.html$' || true)"
[ -n "$changed" ] || { echo "no changed boards"; exit 0; }
for f in $changed; do
  [ -f "$f" ] && tools/design/render.sh "$(basename "$f" .dc.html)"
done
