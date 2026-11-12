#!/usr/bin/env bash
# flow: home (month grid). Starts and ends on the project home.
set -euo pipefail
. "$(dirname "$0")/../lib/lib.sh"
assertVisible "Month"
assertVisible "Switch project"
screenshot ProjectHub
