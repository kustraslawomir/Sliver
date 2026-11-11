#!/usr/bin/env bash
# Device helper for screenshots. Usage: device.sh ready | install | launch | shot <name> | tap X Y | swipe X1 Y1 X2 Y2 | back | text "<label>" | size
set -euo pipefail
cd "$(dirname "$0")/.."
PKG=app.sliver
# Several devices may be attached: pin to the Samsung unless ANDROID_SERIAL is already set.
if [ -z "${ANDROID_SERIAL:-}" ]; then
  ANDROID_SERIAL="$(adb devices -l | awk '/model:SM_/{print $1; exit}')"
  [ -n "$ANDROID_SERIAL" ] || { echo "no Samsung device" >&2; echo no; exit 1; }
  export ANDROID_SERIAL
fi
case "${1:?cmd}" in
  ready)   adb get-state >/dev/null 2>&1 && echo yes || { echo no; exit 1; } ;;
  install) ./gradlew :composeApp:installDebug -q ;;
  launch)  adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null; sleep 3 ;;
  shot)    mkdir -p docs/screenshots; adb exec-out screencap -p > "docs/screenshots/${2:?name}.png"; echo "docs/screenshots/$2.png" ;;
  tap)     adb shell input tap "$2" "$3"; sleep 1 ;;
  swipe)   adb shell input swipe "$2" "$3" "$4" "$5" 300; sleep 1 ;;
  back)    adb shell input keyevent 4; sleep 1 ;;
  text)    adb shell uiautomator dump /sdcard/ui.xml >/dev/null; adb exec-out cat /sdcard/ui.xml | python3 -I -c '
import sys,re
x=sys.stdin.read(); t=sys.argv[1].lower()
for m in re.finditer(r"text=\"([^\"]*)\"[^>]*content-desc=\"([^\"]*)\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"",x):
    if t in (m[1]+" "+m[2]).lower():
        a,b,c,d=map(int,m.groups()[2:]); print((a+c)//2,(b+d)//2); break
else: sys.exit(1)' "$2" ;;
  size)    adb shell wm size ;;
esac
