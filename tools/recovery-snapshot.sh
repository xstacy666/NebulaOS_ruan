#!/bin/sh
# Wait for the tablet in recovery, then collect all evidence in ONE adb shell
# session (the device cycles recovery -> fastboot, so many short calls fail).
set -eu
cd "$(dirname "$0")/.."
mkdir -p logs
OUT="logs/recovery-snapshot-$(date +%Y%m%d-%H%M%S).txt"

CMD='
echo "=== id ==="; id
echo "=== props ==="; getprop | grep -iE "ro.recovery|ro.build|ro.product|bootloader|adb.secure" | head -80
echo "=== recovery.log tail ==="; tail -400 /tmp/recovery.log 2>/dev/null
echo "=== saved OFox logs ==="; ls -laR /data/media/0/OrangeFox/logs 2>/dev/null; ls -laR /sdcard/OrangeFox/logs 2>/dev/null
echo "=== cache recovery logs ==="; ls -la /cache/recovery 2>/dev/null; tail -200 /cache/recovery/last_log 2>/dev/null
echo "=== mounts ==="; mount
echo "=== pstore ==="; ls -la /sys/fs/pstore 2>/dev/null; for f in /sys/fs/pstore/*; do [ -f "$f" ] && { echo "--- $f"; tail -200 "$f"; }; done
echo "=== fstabs ==="; for f in /fstab.* /vendor/etc/fstab.* /odm/etc/fstab.* /system/etc/fstab.* /system_root/etc/fstab.*; do [ -f "$f" ] && { echo "--- $f"; cat "$f"; }; done
echo "=== lpdump ==="; lpdump 2>/dev/null || echo unavailable
echo "=== by-name ==="; ls -l /dev/block/by-name 2>/dev/null
echo "=== blkid ==="; for p in system system_ext product vendor odm; do echo "--- $p"; blkid /dev/block/by-name/$p 2>/dev/null || echo no-node; done
'

i=0
while [ "$i" -lt 60 ]; do
    state=$(./tools/root-usb.sh adb get-state 2>/dev/null || true)
    if [ "$state" = "device" ]; then
        ./tools/root-usb.sh adb shell "$CMD" > "$OUT" 2>&1 || true
        if grep -q '=== id ===' "$OUT"; then
            echo "Captured on attempt $i"
            break
        fi
    fi
    i=$((i + 1))
    sleep 1
done

echo "Saved $OUT"
wc -l "$OUT"
head -20 "$OUT"
