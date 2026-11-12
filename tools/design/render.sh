#!/usr/bin/env bash
# usage: tools/design/render.sh <Board|all> [outDir]   (default outDir: design/png)
# Renders design/src/<Board>.dc.html with Node + headless Chrome. No network, no npm/pip deps.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
CHROME="${CHROME:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}"
OUT="${2:-$ROOT/design/png}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$OUT"

render() {
  local b="$1" dims w h s
  dims="$(node "$ROOT/tools/design/dcrender.mjs" "$b" "$WORK/$b.html")"
  read -r w h s <<<"$dims"
  "$CHROME" --headless=new --disable-gpu --hide-scrollbars --no-first-run --no-sandbox \
    --user-data-dir="$WORK/profile" --host-resolver-rules="MAP * ~NOTFOUND" \
    --force-device-scale-factor="$s" --window-size="$w,$h" --virtual-time-budget=2000 \
    --screenshot="$OUT/$b.png" "file://$WORK/$b.html" >/dev/null 2>&1
  echo "rendered $b ${w}x${h}@${s}x"
}

if [ "$1" = all ]; then
  for f in "$ROOT"/design/src/*.dc.html; do render "$(basename "$f" .dc.html)"; done
else
  render "$1"
fi
