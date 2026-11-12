# Flow DSL on plain adb + uiautomator (Maestro-shaped: tapOn / assertVisible / back / screenshot).
# Sourced by scripts/qa.sh and every qa/flows/*.sh. Needs ANDROID_SERIAL, QA_TMP; optional QA_SHOT_DIR, QA_NO_SHOTS.
QA_UI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/ui.py"
PKG=skustra.fragments

die() { echo "FLOW FAIL: $*" >&2; exit 1; }

guard() {
  adb shell dumpsys activity activities 2>/dev/null | grep -m1 topResumedActivity | grep -q "$PKG" \
    || die "topResumedActivity is not $PKG, aborting"
}

dump() {
  local i
  for i in 1 2 3; do
    adb shell uiautomator dump /sdcard/qa-ui.xml >/dev/null 2>&1
    adb exec-out cat /sdcard/qa-ui.xml >"$QA_TMP/ui.xml" 2>/dev/null
    grep -q "</hierarchy>" "$QA_TMP/ui.xml" && return 0
    sleep 0.5
  done
  die "uiautomator dump failed"
}

# visible <needle>: true if text/content-desc contains it (current dump)
visible() { python3 -I "$QA_UI" has "$QA_TMP/ui.xml" "$1"; }

assertVisible() {
  local t=0
  while [ $t -lt 8 ]; do
    guard; dump
    visible "$1" && { echo "  ok  visible: $1"; return 0; }
    sleep 1; t=$((t + 1))
  done
  die "not visible: $1"
}

assertNotVisible() { guard; dump; visible "$1" && die "unexpectedly visible: $1"; echo "  ok  absent: $1"; }

# tapOn <needle> [exact|sub]: exact match first, then substring
tapOn() {
  local t=0 xy
  while [ $t -lt 8 ]; do
    guard; dump
    xy="$(python3 -I "$QA_UI" find "$QA_TMP/ui.xml" exact "$1" || python3 -I "$QA_UI" find "$QA_TMP/ui.xml" sub "$1")" || xy=""
    if [ -n "$xy" ]; then
      guard; adb shell input tap $xy; echo "  tap $1 @ $xy"; sleep 1.2; return 0
    fi
    sleep 1; t=$((t + 1))
  done
  die "cannot tap: $1"
}

back() { guard; adb shell input keyevent 4; sleep 1; }
scrollDown() { guard; adb shell input swipe 540 1700 540 600 300; sleep 1; }
scrollUp() { guard; adb shell input swipe 540 600 540 1900 300; sleep 1; }
wait_s() { sleep "$1"; }

# screenshot <Screen>: refuses on the camera or any non-app window; no-op in QA_NO_SHOTS mode
screenshot() {
  [ -n "${QA_NO_SHOTS:-}" ] && { echo "  skip screenshot $1 (no-shot mode)"; return 0; }
  guard; dump
  visible "Viewfinder" && die "privacy: refusing to screenshot the camera preview"
  visible "Stop recording" && die "privacy: refusing to screenshot a recording"
  mkdir -p "$QA_SHOT_DIR"
  adb exec-out screencap -p >"$QA_SHOT_DIR/$1.png"
  echo "  shot $1"
}

# projects_only_seeded: true when the projects sheet lists nothing but the seeded projects (covers are real otherwise)
projects_only_seeded() {
  local names
  guard; dump
  names="$(python3 -I "$QA_UI" projects "$QA_TMP/ui.xml")"
  [ -n "$names" ] || return 1
  ! printf '%s\n' "$names" | grep -qvxF -e "QA Seed" -e "Lisbon trip"
}
