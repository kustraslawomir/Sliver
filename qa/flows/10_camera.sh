#!/usr/bin/env bash
# flow: camera (assert only, never screenshot: live preview). Starts and ends on the project home.
set -euo pipefail
. "$(dirname "$0")/../lib/lib.sh"
tapOn "Record today"
assertVisible "Viewfinder"
assertVisible "Zoom 1"
assertVisible "Close camera"
tapOn "Close camera"
assertVisible "Previous month"
