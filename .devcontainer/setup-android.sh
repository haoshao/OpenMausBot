#!/usr/bin/env bash
# Prepare a GitHub Codespace for OpenMausBot Android v1.5 development.
set -euo pipefail

SDK_ROOT="${ANDROID_HOME:-$HOME/android-sdk}"
TOOLS_ZIP="commandlinetools-linux-13114758_latest.zip"
TOOLS_URL="https://dl.google.com/android/repository/${TOOLS_ZIP}"

if ! command -v unzip >/dev/null 2>&1; then
  sudo apt-get update
  sudo apt-get install -y --no-install-recommends unzip
fi
if ! command -v curl >/dev/null 2>&1; then
  sudo apt-get update
  sudo apt-get install -y --no-install-recommends curl
fi

mkdir -p "$SDK_ROOT/cmdline-tools" "$SDK_ROOT/platform-tools"
if [ ! -x "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "Installing Android command-line tools..."
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' EXIT
  curl --fail --location --retry 3 "$TOOLS_URL" --output "$tmp/cmdline-tools.zip"
  unzip -q "$tmp/cmdline-tools.zip" -d "$tmp"
  mv "$tmp/cmdline-tools" "$SDK_ROOT/cmdline-tools/latest"
fi

export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"
export PATH="$SDK_ROOT/cmdline-tools/latest/bin:$SDK_ROOT/platform-tools:$PATH"

echo "Accepting Android SDK licenses..."
yes | sdkmanager --licenses >/dev/null || true

echo "Installing Android platform 37 and build tools..."
sdkmanager --install "platform-tools" "platforms;android-37" "build-tools;37.0.0"

# Gradle uses ANDROID_HOME. Do not commit local.properties, keystores, or secrets.
echo "Java version:"
java -version
echo "Android SDK:"
sdkmanager --list_installed
echo "Workspace ready. Run: bash .devcontainer/check-android.sh"
