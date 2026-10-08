#!/usr/bin/env bash
#
# AmazMod dev helper: build, install and fix device state in one command.
#
# Usage:
#   scripts/dev.sh service   # build + install the watch (service) APK
#   scripts/dev.sh app       # build + install the phone (app) APK, then rebind the listener
#   scripts/dev.sh preview   # seed fake notifications on the watch and open the list
#
# Requires the device(s) to be connected via adb (USB or tcp). The watch and phone
# are detected by model. The debug key is shared between builds, so updates use
# `adb install -r` and keep the app UID (and the watch widget) intact.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-$HOME/jdk11}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

TARGET="${1:-service}"

find_watch() { adb devices -l | awk '/qogirUS|Amazfit_Verge/{print $1; exit}'; }
find_phone() { adb devices -l | awk '/SM_G780G|r8qxx/{print $1; exit}'; }

gradle_build() { (cd "$ROOT" && ./gradlew -q "$1" --no-daemon); }

install_latest() { # $1=serial  $2=apk dir
  local serial="$1" dir="$2" apk
  apk="$(ls -t "$dir"/*.apk 2>/dev/null | head -1)"
  [ -n "$apk" ] || { echo "No APK in $dir"; exit 1; }
  echo "Installing $(basename "$apk") on $serial"
  adb -s "$serial" install -r "$apk"
}

rebind_listener() { # $1=serial
  local c="com.edotassi.amazmod/com.edotassi.amazmod.notification.NotificationService"
  adb -s "$1" shell cmd notification disallow_listener $c || true
  sleep 1
  adb -s "$1" shell cmd notification allow_listener $c || true
  adb -s "$1" shell monkey -p com.edotassi.amazmod -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
}

case "$TARGET" in
  service)
    gradle_build ":service:assembleDebug"
    W="$(find_watch)"; [ -n "$W" ] || { echo "Watch not found via adb"; exit 1; }
    install_latest "$W" "$ROOT/service/build/outputs/apk/debug"
    adb -s "$W" shell am start -n com.amazmod.service/.springboard.LauncherWearGridActivity >/dev/null 2>&1 || true
    ;;
  app)
    gradle_build ":app:assembleDebug"
    P="$(find_phone)"; [ -n "$P" ] || { echo "Phone not found via adb"; exit 1; }
    install_latest "$P" "$ROOT/app/build/outputs/apk/debug"
    rebind_listener "$P"
    ;;
  preview)
    W="$(find_watch)"; [ -n "$W" ] || { echo "Watch not found via adb"; exit 1; }
    adb -s "$W" shell am start -n com.amazmod.service/.ui.UIPreviewActivity >/dev/null 2>&1 || true
    ;;
  *)
    echo "Usage: $0 [service|app|preview]"; exit 1;;
esac

echo "Done ($TARGET)."
