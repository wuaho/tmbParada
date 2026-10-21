#!/usr/bin/env bash
# Smoke test on an emulator: walks through the main flows and saves screenshots.
set -u
live_failed=
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
  echo "Focused window: $(adb shell dumpsys window | grep -m1 mCurrentFocus)"
  echo "On screen: $(ui | grep -o -E '(text|content-desc)="[^"]+"' | head -25 | tr '\n' ' ')"
  return 1
}

adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
# -g grants runtime permissions, such as notifications, so no dialog covers the screen.
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS || true
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

# After pinning, the app opens the stop picker; if it does not, open it from the widget.
if ui | grep -q "Choose a bus stop"; then
  echo "Stop picker opened after pinning"
else
  echo "Stop picker NOT opened after pinning; opening it from the widget"
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
sleep 4
# Choosing a stop opens the widget settings; pick the transparent style to exercise it.
shot 05b-settings
tap_text "V29"
tap_text "5 min"
adb shell input swipe 540 1800 540 700 300
sleep 1
tap_text "Transparent"
tap_text "Done"
sleep 4
# Done returns to the home screen, on the page where the launcher placed the widget.
# Pressing Home again would jump to the launcher's default page, so the test does not.
# Configuring does not start a live session: the widget waits idle with its start button.
if ui | grep -q 'text="See arrivals"'; then
  echo "Returned to the home screen showing the idle widget"
else
  echo "Idle widget NOT visible after returning home"
  echo "Focused window: $(adb shell dumpsys window | grep -m1 mCurrentFocus)"
  live_failed=1
fi
if adb shell dumpsys activity services "$PKG" | grep -q "LiveSessionService"; then
  echo "Live session started by configuring the widget"
  live_failed=1
fi
shot 06-widget

tap_text "See arrivals"
sleep 1
shot 07-widget-refreshing
sleep 6
shot 08-widget-after-refresh

# The live session runs as a foreground service with an ongoing notification.
if adb shell dumpsys activity services "$PKG" | grep -q "LiveSessionService"; then
  echo "Live session service running"
else
  echo "Live session service NOT running"
  live_failed=1
fi
if adb shell dumpsys notification --noredact | grep -q "pkg=$PKG"; then
  echo "Live notification posted"
else
  echo "Live notification NOT posted"
  live_failed=1
fi
adb shell cmd statusbar expand-notifications
sleep 2
shot 08b-notification-shade
adb shell cmd statusbar collapse
adb shell input keyevent KEYCODE_SLEEP
sleep 2
adb shell input keyevent KEYCODE_WAKEUP
sleep 2
shot 08c-lock-screen
adb shell wm dismiss-keyguard
sleep 2

# Ending the session from the notification turns the widget into its idle "start" view.
adb shell cmd statusbar expand-notifications
sleep 2
tap_text "Stop|STOP"
adb shell cmd statusbar collapse
sleep 4
shot 08d-widget-idle
if ui | grep -q 'text="See arrivals"'; then
  echo "Idle widget shown"
else
  echo "Idle widget NOT shown"
  live_failed=1
fi

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
if [ -n "${live_failed:-}" ]; then
  echo "Live session checks failed"
  exit 1
fi
