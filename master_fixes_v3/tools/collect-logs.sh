#!/bin/sh
# Capture logcat / dmesg / dumpsys snapshots into logs/<timestamp>-<name>/.
set -eu

cd "$(dirname "$0")/.."
ADB="./tools/root-usb.sh adb"

NAME="${1:-snapshot}"
SAFE=$(printf '%s' "$NAME" | tr -c 'A-Za-z0-9_-' '_')
STAMP=$(date +%Y%m%d-%H%M%S)
OUT="logs/${STAMP}-${SAFE}"

STATE=$($ADB get-state 2>/dev/null || true)
if [ "$STATE" != "device" ]; then
    echo "No connected device. Run ./tools/root-usb.sh adb devices first." >&2
    exit 1
fi

mkdir -p "$OUT"
echo "Device: $($ADB get-serialno)"

$ADB shell getprop > "$OUT/getprop.txt"
$ADB logcat -d -v threadtime > "$OUT/logcat.txt"

# dmesg is root-restricted on many ROMs; try device-root, then fall back.
if $ADB shell 'su -c id' 2>/dev/null | grep -q 'uid=0'; then
    $ADB shell 'su -c dmesg' > "$OUT/dmesg.txt" 2>"$OUT/dmesg.err" || true
else
    $ADB shell dmesg > "$OUT/dmesg.txt" 2>"$OUT/dmesg.err" || true
fi

for svc in battery wifi display input window; do
    $ADB shell "dumpsys $svc" > "$OUT/dumpsys-${svc}.txt" 2>/dev/null || true
done

echo "Snapshot written to $OUT"
ls -lh "$OUT"
