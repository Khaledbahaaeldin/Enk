#!/usr/bin/env bash
# scripts/capture-screenshots.sh
# Captures standard 1.0 promotional and store screenshots via adb.
# Designed for Git Bash on Windows and Linux CI (MSYS_NO_PATHCONV=1 safe).

set -euo pipefail
export MSYS_NO_PATHCONV=1

OUTPUT_DIR="fastlane/metadata/android/en-US/images/phoneScreenshots"
mkdir -p "$OUTPUT_DIR"

DEVICE=${1:-}
ADB_CMD="adb"
if [ -n "$DEVICE" ]; then
    ADB_CMD="adb -s $DEVICE"
fi

echo "Verifying connected device..."
$ADB_CMD wait-for-device

capture_screen() {
    local name="$1"
    local dest="${OUTPUT_DIR}/${name}.png"
    echo "Capturing $name to $dest..."
    $ADB_CMD exec-out screencap -p > "$dest"
}

echo "=== Enk 1.0 Screenshot Capture Utility ==="
echo "Please navigate on device/emulator to each screen and press Enter to capture:"
echo ""

read -r -p "1. Day writing screen with text. Press Enter when ready..."
capture_screen "1_day_writer"

read -r -p "2. Night writing screen with lamp and control panel open. Press Enter when ready..."
capture_screen "2_night_writer"

read -r -p "3. The Box with dividers and note cards. Press Enter when ready..."
capture_screen "3_box"

read -r -p "4. Find screen showing a search query with ink matches. Press Enter when ready..."
capture_screen "4_find"

read -r -p "5. Setup screen showing appearance options with a non-default theme. Press Enter when ready..."
capture_screen "5_setup"

echo ""
echo "All 5 screenshots captured under $OUTPUT_DIR:"
ls -lh "$OUTPUT_DIR"
