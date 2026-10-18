#!/usr/bin/env bash
# Smoke test on an emulator: walks through the main flows and saves screenshots.
set -u
OUT=screenshots
mkdir -p "$OUT"
PKG=com.example.tmbparada

shot() { adb exec-out screencap -p > "$OUT/$1.png"; echo "screenshot $1"; }

ui() {
  adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
  adb shell cat /sdcard/ui.xml
}

# Taps the center of the first node whose text or content-desc matches the regex $1.
tap_text() {
  for _ in $(seq 1 20); do
    point=$(ui | python3 -c '
import re, sys
xml, pattern = sys.stdin.read(), sys.argv[1]
for node in re.finditer(r"<node [^>]*>", xml):
    n = node.group(0)
    label = re.search(r"(?:text|content-desc)=\"(?:%s)\"" % pattern, n)
    bounds = re.search(r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", n)
    if label and bounds:
        x1, y1, x2, y2 = map(int, bounds.groups())
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
' "$1")
    if [ -n "$point" ]; then
      echo "tap '$1' at $point"
      adb shell input tap $point
      return 0
    fi
    sleep 1
  done
  echo "NOT FOUND: $1"
  ui > "$OUT/ui-not-found-$(date +%s).xml"
  return 1
}

adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -c

adb shell am start -W -n "$PKG/.MainActivity"
sleep 3
shot 01-home

tap_text "Add widget"
sleep 3
shot 02-pin-dialog
tap_text "Add to home screen|ADD TO HOME SCREEN|Add automatically|ADD AUTOMATICALLY|Add|ADD"
sleep 4
shot 03-after-pin

# The launcher opens the stop picker; if it does not, open it from the widget.
if ! ui | grep -q "Choose a bus stop"; then
  adb shell input keyevent KEYCODE_HOME
  sleep 2
  tap_text "Choose a stop"
  sleep 3
fi
shot 04-picker
adb shell input text "2903"
sleep 3
shot 05-search-results
tap_text "Stop 2903"
sleep 6

adb shell input keyevent KEYCODE_HOME
sleep 4
shot 06-widget

tap_text "Refresh arrivals"
sleep 1
shot 07-widget-refreshing
sleep 6
shot 08-widget-after-refresh

adb shell cmd uimode night yes
sleep 5
shot 09-widget-dark
adb shell cmd uimode night no

adb shell am start -W -n "$PKG/.MainActivity"
sleep 3
shot 10-home-with-widget

adb logcat -d > "$OUT/logcat.txt"
adb logcat -d -b crash > "$OUT/crash.txt"
if grep -q "$PKG" "$OUT/crash.txt"; then
  echo "App crashed:"
  cat "$OUT/crash.txt"
  exit 1
fi
echo "No crashes"
