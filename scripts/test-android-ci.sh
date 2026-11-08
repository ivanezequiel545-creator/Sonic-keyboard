#!/usr/bin/env bash
set -euo pipefail
# New emulator versions can leave a QEMU child holding the action's stdout pipe
# after `adb emu kill`. Always reap it, including when Gradle reports a failure.
cleanup() {
  adb -s "${ANDROID_SERIAL:-emulator-5554}" emu kill >/dev/null 2>&1 || true
  pkill -9 -f '[q]emu-system-x86_64' || true
}
trap cleanup EXIT
cd android
bash ../scripts/gradle-ci.sh connectedDebugAndroidTest --no-daemon -PvelocityBuildNumber="${GITHUB_RUN_NUMBER:-0}"
