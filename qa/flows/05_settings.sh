#!/usr/bin/env bash
# flow: settings and reminders. Starts and ends on the project home.
set -euo pipefail
. "$(dirname "$0")/../lib/lib.sh"
tapOn "Settings"
assertVisible "Appearance"
screenshot Settings
scrollDown
tapOn "Daily reminder"
assertVisible "Remind me to film"
screenshot Reminders
back
scrollUp
assertVisible "Appearance"
back
assertVisible "Previous month"
