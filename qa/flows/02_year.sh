#!/usr/bin/env bash
# flow: year view. Starts and ends on the project home.
set -euo pipefail
. "$(dirname "$0")/../lib/lib.sh"
tapOn "Year"
assertVisible "The light of your year"
screenshot ProjectHubYear
tapOn "Month"
assertVisible "Previous month"
