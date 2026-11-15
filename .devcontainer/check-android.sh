#!/usr/bin/env bash
set -euo pipefail

echo "== OpenMausBot Android workspace diagnostics =="
echo "Repository: $(git remote -v | head -n 1)"
echo "Branch: $(git branch --show-current)"
echo "Commit: $(git rev-parse --short HEAD)"
echo "JAVA_HOME: ${JAVA_HOME:-<unset>}"
java -version
echo "ANDROID_HOME: ${ANDROID_HOME:-<unset>}"
test -d "${ANDROID_HOME:?ANDROID_HOME is required}/platforms/android-37.0"
test -x "${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager"
"${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" --list_installed

echo "== Run Android v1.5 test suite and assemble preview APK =="
cd android
bash ./gradlew --no-daemon :core:test :app:testDebugUnitTest :app:assembleDebug :app:assemblePreview
echo "== APK =="
ls -lh app/build/outputs/apk/preview/app-preview.apk
