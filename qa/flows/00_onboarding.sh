#!/usr/bin/env bash
# flow: first-run onboarding on a fresh install (only run by scripts/qa.sh when the start screen is showing).
set -euo pipefail
. "$(dirname "$0")/../lib/lib.sh"
assertVisible "Restore from backup"
tapOn "Start"
assertVisible "Create project"
tapOn "Create project"
assertVisible "Allow camera"
tapOn "Allow camera"
assertVisible "Previous month"
