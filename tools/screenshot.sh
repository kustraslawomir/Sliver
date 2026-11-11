#!/usr/bin/env bash
# usage: tools/screenshot.sh <screen-name>   (device/emulator connected via adb)
set -euo pipefail
mkdir -p docs/qa
adb exec-out screencap -p > "docs/qa/$1.png"
echo "docs/qa/$1.png"
