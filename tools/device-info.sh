#!/bin/sh
# Collect read-only device facts into logs/device-info-<timestamp>.txt.
set -eu

cd "$(dirname "$0")/.."
mkdir -p logs

STAMP=$(date +%Y%m%d-%H%M%S)
OUT="logs/device-info-${STAMP}.txt"
ADB="./tools/root-usb.sh adb"

STATE=$($ADB get-state 2>/dev/null || true)
if [ "$STATE" != "device" ]; then
    echo "No connected device. Run ./tools/root-usb.sh adb devices first." >&2
    exit 1
fi

{
    echo "=== date (UTC) ==="; date -u
    echo "=== serial ==="; $ADB get-serialno
    echo "=== getprop ==="; $ADB shell getprop
    echo "=== kernel version ==="; $ADB shell cat /proc/version
    echo "=== cpuinfo ==="; $ADB shell cat /proc/cpuinfo
    echo "=== meminfo ==="; $ADB shell cat /proc/meminfo
    echo "=== wm size ==="; $ADB shell wm size
    echo "=== wm density ==="; $ADB shell wm density
    echo "=== df -h ==="; $ADB shell df -h
    echo "=== adb shell id ==="; $ADB shell id
    echo "=== root check ==="; $ADB shell 'su -c id' 2>&1 || true
    echo "=== block devices ==="; $ADB shell 'ls -l /dev/block/by-name' 2>&1 || true
} > "$OUT"

echo "Saved $OUT"
grep -iE 'ro.product.(model|name|device|board)|ro.build.version.release|ro.build.display.id|ro.build.fingerprint' "$OUT" | head -30 || true
