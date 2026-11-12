#!/usr/bin/env bash
# flow: film player (seeded rendered film). Starts and ends on the project home.
set -euo pipefail
. "$(dirname "$0")/../lib/lib.sh"
scrollDown
tapOn "QA Seed, 0:"
assertVisible "Share film"
wait_s 1
screenshot FilmPlayer
tapOn "Close"
scrollUp
assertVisible "Previous month"
