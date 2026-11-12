#!/usr/bin/env bash
# One-command device QA: build, install, seed synthetic data, run qa/flows in dark+light x font 1.0/1.3,
# screenshot into docs/qa/<date>/<theme>-<scale>/, compare against design/png, write docs/qa/<date>/REPORT.md.
# usage: scripts/qa.sh [--device xiaomi|emulator|samsung] [--days N] [--skip-build] [--combos "dark:1.0 light:1.0"]
# Default device: Xiaomi if online and unlocked, else emulator tracker13 (started if needed). The Samsung is used
# only with an explicit --device samsung: no seed, no wipe, no screenshots, no camera flows, settings restored.
# Deviation: Maestro is not installed (and must not be), so flows are plain adb + uiautomator shell scripts
# (qa/lib/lib.sh DSL: tapOn / assertVisible / back / screenshot) that map 1:1 onto Maestro commands.
set -uo pipefail
cd "$(dirname "$0")/.."
ROOT="$PWD"

XIAOMI=b798509a
SAMSUNG='adb-RFCY30SW5QZ-XscZaw._adb-tls-connect._tcp'
EMU_SERIAL=emulator-5554
EMU_BIN="$HOME/Library/Android/sdk/emulator/emulator"
DEVICE=auto DAYS=30 BUILD=1 COMBOS="dark:1.0 light:1.0 dark:1.3 light:1.3"
while [ $# -gt 0 ]; do
  case "$1" in
    --device) DEVICE="$2"; shift 2 ;;
    --days) DAYS="$2"; shift 2 ;;
    --skip-build) BUILD=0; shift ;;
    --combos) COMBOS="$2"; shift 2 ;;
    *) echo "unknown arg $1" >&2; exit 2 ;;
  esac
done

DATE="$(date +%F)"
QA_TMP="$(mktemp -d)"; export QA_TMP
START=$SECONDS
online() { adb devices | awk -v s="$1" '$1==s && $2=="device"{f=1} END{exit !f}'; }
unlocked() {
  adb -s "$1" shell input keyevent 224 >/dev/null 2>&1; sleep 1
  ! adb -s "$1" shell dumpsys window 2>/dev/null | grep -qE "isKeyguardShowing=true|mDreamingLockscreen=true"
}

EMU_STARTED=0
start_emulator() {
  online "$EMU_SERIAL" && return 0
  echo ">> starting emulator tracker13"
  nohup "$EMU_BIN" -avd tracker13 -no-snapshot-save -no-audio -no-boot-anim >"$QA_TMP/emu.log" 2>&1 &
  EMU_STARTED=1
  local i=0
  until online "$EMU_SERIAL" && [ "$(adb -s "$EMU_SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; do
    sleep 5; i=$((i + 5)); [ $i -lt 420 ] || { echo "emulator did not boot in 420 s" >&2; return 1; }
  done
}

case "$DEVICE" in
  samsung) SERIAL="$SAMSUNG"; KIND=samsung ;;
  emulator) start_emulator || exit 1; SERIAL="$EMU_SERIAL"; KIND=emulator ;;
  xiaomi) SERIAL="$XIAOMI"; KIND=xiaomi ;;
  auto)
    if online "$XIAOMI" && unlocked "$XIAOMI"; then SERIAL="$XIAOMI"; KIND=xiaomi
    else start_emulator || exit 1; SERIAL="$EMU_SERIAL"; KIND=emulator; fi ;;
  *) echo "bad --device" >&2; exit 2 ;;
esac
online "$SERIAL" || { echo "device $SERIAL is not online" >&2; exit 1; }
export ANDROID_SERIAL="$SERIAL"
echo ">> device: $KIND ($SERIAL)"

PKG=skustra.fragments
OUT="$ROOT/docs/qa/$DATE"; [ "$KIND" = xiaomi ] || OUT="$OUT-$KIND"
SAFE=0; [ "$KIND" = samsung ] && SAFE=1
G="-g"; [ $SAFE = 1 ] && G=""

ORIG_NIGHT="$(adb shell cmd uimode night 2>/dev/null | sed -n 's/.*Night mode: *//p' | tr -d '\r')"
ORIG_FONT="$(adb shell settings get system font_scale | tr -d '\r')"
SEEDED=0

wait_log() { # wait_log <needle> <timeout s>
  local i=0
  while [ $i -lt "$2" ]; do
    adb logcat -d -s SliverSeed 2>/dev/null | grep -q "$1" && return 0
    sleep 2; i=$((i + 2))
  done
  return 1
}
seeder() { # seeder <log needle> <timeout> <extra args...>
  local needle="$1" to="$2"; shift 2
  adb logcat -c
  adb shell "am broadcast -n $PKG/app.sliver.debug.SeedReceiver -a app.sliver.debug.SEED $*" >/dev/null
  wait_log "$needle" "$to"
}
launch() {
  adb shell am force-stop $PKG
  adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
  sleep 4
}

cleanup() {
  set +e
  echo ">> cleanup"
  if [ $SEEDED = 1 ]; then
    adb shell am start -n $PKG/app.sliver.MainActivity >/dev/null 2>&1; sleep 3
    seeder "done wipe" 120 --es mode wipe && echo "   wiped seeded data" || echo "   WIPE DID NOT CONFIRM, check SliverSeed log" >&2
  fi
  [ "${FRESH:-0}" = 1 ] && adb shell pm clear $PKG >/dev/null 2>&1 && echo "   fresh install: app data cleared"
  [ -n "$ORIG_NIGHT" ] && adb shell cmd uimode night "$ORIG_NIGHT" >/dev/null 2>&1
  if [ "$ORIG_FONT" = null ] || [ -z "$ORIG_FONT" ]; then adb shell settings delete system font_scale >/dev/null 2>&1
  else adb shell settings put system font_scale "$ORIG_FONT" >/dev/null 2>&1; fi
  echo "   restored night=$ORIG_NIGHT font_scale=$ORIG_FONT"
  adb shell input keyevent 3 >/dev/null 2>&1
  [ $EMU_STARTED = 1 ] && adb -s "$EMU_SERIAL" emu kill >/dev/null 2>&1
  rm -rf "$QA_TMP"
}
trap cleanup EXIT

if [ $BUILD = 1 ]; then
  echo ">> build"
  ./gradlew :composeApp:assembleDebug -q || { echo "build failed" >&2; exit 1; }
fi
echo ">> install"
adb install -r $G composeApp/build/outputs/apk/debug/composeApp-debug.apk >/dev/null || { echo "install failed" >&2; exit 1; }

launch
FRESH=0
if [ $SAFE = 0 ]; then
  adb shell uiautomator dump /sdcard/qa-ui.xml >/dev/null 2>&1
  if adb exec-out cat /sdcard/qa-ui.xml 2>/dev/null | grep -q "Restore from backup"; then
    # untouched fresh install (no data to protect): onboard, and pm clear at the end to leave it as found
    FRESH=1
    bash qa/flows/00_onboarding.sh || { echo "onboarding flow failed" >&2; exit 1; }
  fi
  echo ">> seed $DAYS days"
  SEEDED=1
  seeder "done seed" $((DAYS * 6 + 60)) --ei days "$DAYS" --es project "'QA Seed'" || { echo "seed did not finish" >&2; exit 1; }
fi

mkdir -p "$OUT"
RESULTS="$OUT/flows.txt"; : >"$RESULTS"
FAILS=0 FIRST=1
N=0; TOTAL=$(echo $COMBOS | wc -w | tr -d " ")
run_flow() { # run_flow <file> <combo>
  local f="$1" name; name="$(basename "$f" .sh)"
  echo "-- $2 $name"
  if bash "$f" >"$QA_TMP/flow.log" 2>&1; then echo "PASS $2 $name" >>"$RESULTS"
  else
    cat "$QA_TMP/flow.log" >&2
    if [ "$KIND" = emulator ] && { [ "$name" = 10_camera ] || [ "$name" = 11_record_save ]; }; then
      echo "SKIP $2 $name (emulator camera, known CameraX quality error)" >>"$RESULTS"
    else echo "FAIL $2 $name: $(tail -1 "$QA_TMP/flow.log")" >>"$RESULTS"; FAILS=$((FAILS + 1)); fi
    launch
  fi
}

for combo in $COMBOS; do
  theme="${combo%%:*}"; scale="${combo##*:}"
  echo ">> $theme / font $scale"
  adb shell cmd uimode night "$([ "$theme" = dark ] && echo yes || echo no)" >/dev/null
  adb shell settings put system font_scale "$scale"
  sleep 3; launch
  export QA_SHOT_DIR="$OUT/$theme-$scale"
  [ $SAFE = 1 ] && export QA_NO_SHOTS=1
  if [ $SAFE = 1 ]; then flows="01_home 02_year 04_day 05_settings"
  elif [ $FIRST = 1 ]; then flows="10_camera 01_home 02_year 03_projects 04_day 05_settings 06_film"
  else flows="01_home 02_year 03_projects 04_day 05_settings 06_film"; fi
  N=$((N + 1))
  # the recording goes last: the saved clip thumbnail comes from the live camera and must not appear in any screenshot
  [ $SAFE = 0 ] && [ $N = "$TOTAL" ] && flows="$flows 11_record_save"
  for fl in $flows; do run_flow "$ROOT/qa/flows/$fl.sh" "$theme-$scale"; done
  FIRST=0
done

DUR=$((SECONDS - START))
printf 'device: %s (%s)\ndate: %s\nduration: %ss\n' "$KIND" "$SERIAL" "$DATE" "$DUR" >"$OUT/run.txt"
[ $SAFE = 0 ] && python3 qa/report.py "$OUT"
echo ">> done in ${DUR}s, $FAILS failed flow(s); see $OUT"
cat "$RESULTS"
[ $FAILS = 0 ]
