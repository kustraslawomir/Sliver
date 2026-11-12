#!/usr/bin/env bash
# flow: day detail. Starts and ends on the project home.
set -euo pipefail
. "$(dirname "$0")/../lib/lib.sh"
tapOn ", has a clip"
assertVisible "Previous day"
screenshot DayDetail
tapOn "Close"
assertVisible "Previous month"
