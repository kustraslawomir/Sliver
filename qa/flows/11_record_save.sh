#!/usr/bin/env bash
# flow: record 2 s, edit clip, save into the seeded project (assert only, no screenshots: camera content). Starts and ends on the project home.
set -euo pipefail
. "$(dirname "$0")/../lib/lib.sh"
assertVisible "QA Seed"
tapOn "Record today"
assertVisible "Viewfinder"
tapOn "Record"
assertVisible "Stop recording"
wait_s 2
tapOn "Stop recording"
assertVisible "Trim position"
assertVisible "Save clip"
assertVisible "Retake"
tapOn "Save clip"
assertVisible "Previous month"
assertVisible "Done for today"
