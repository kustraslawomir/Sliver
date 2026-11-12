#!/usr/bin/env bash
# flow: projects sheet (screenshot only when no real project is listed: covers are real thumbnails). Starts and ends on the project home.
set -euo pipefail
. "$(dirname "$0")/../lib/lib.sh"
tapOn "Switch project"
assertVisible "New project"
assertVisible "QA Seed"
if projects_only_seeded; then screenshot ProjectsV2; else echo "  skip screenshot ProjectsV2 (real projects on device)"; fi
back
assertVisible "Previous month"
