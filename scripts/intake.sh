#!/usr/bin/env bash
# Safe, non-interactive intake: unzip into a fresh dir + environment report.
set -euo pipefail
ZIP="${1:?usage: intake.sh <file.zip>}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
IN="$ROOT/.workflow/input"
REPORT="$ROOT/.workflow/intake.md"

mkdir -p "$ROOT/.workflow"
rm -rf "$IN"; mkdir -p "$IN"
unzip -q -o "$ZIP" -d "$IN" -x '__MACOSX/*' '*.DS_Store' 2>/dev/null || [ $? -le 1 ]
# zip-slip guard: nothing may resolve outside $IN
if find "$IN" -type l | grep -q .; then find "$IN" -type l -delete; fi

have() { command -v "$1" >/dev/null 2>&1 && echo "yes" || echo "no"; }
present() { [ -e "$1" ] && echo "present" || echo "missing"; }

{
  echo "# Intake report"
  echo "## Files ($(find "$IN" -type f | wc -l | tr -d ' '))"
  (cd "$IN" && find . -type f -exec wc -c {} + | sort -k2 | sed 's|\./||' | head -300)
  echo
  echo "## Tooling"
  echo "java: $(have java) | gradlew: $(present "$ROOT/gradlew") | adb: $(have adb) | node: $(have node) | firebase-cli: $(have firebase) | flutter: $(have flutter)"
  echo
  echo "## Credentials (presence only, values never shown)"
  echo ".env.local: $(present "$ROOT/.env.local")"
  echo "app/google-services.json: $(present "$ROOT/app/google-services.json")"
  echo "local.properties: $(present "$ROOT/local.properties")"
  if [ "$(have firebase)" = yes ]; then
    echo "firebase login: $(firebase login:list 2>/dev/null | grep -qi '@' && echo logged-in || echo not-logged-in)"
  fi
} > "$REPORT"
cat "$REPORT"
