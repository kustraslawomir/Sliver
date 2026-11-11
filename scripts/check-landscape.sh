#!/usr/bin/env bash
# usage: scripts/check-landscape.sh <video-file>
# Prints width/height/rotation and fails if width <= height after applying rotation.
set -euo pipefail
f="${1:?usage: check-landscape.sh <file>}"
out=$(ffprobe -v error -select_streams v:0 \
  -show_entries stream=width,height:stream_side_data=rotation:stream_tags=rotate \
  -of default=nw=1 "$f")
echo "$out"
w=$(echo "$out" | sed -n 's/^width=//p' | head -1)
h=$(echo "$out" | sed -n 's/^height=//p' | head -1)
r=$(echo "$out" | sed -n 's/^rotation=//p;s/^TAG:rotate=//p' | head -1)
r=${r:-0}; r=${r#-}
if [ "$r" = 90 ] || [ "$r" = 270 ]; then t=$w; w=$h; h=$t; fi
echo "after rotation: ${w}x${h} (rotation ${r})"
if [ "$w" -le "$h" ]; then echo "FAIL: not landscape"; exit 1; fi
echo "OK: landscape"
