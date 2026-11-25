#!/bin/sh
# Collect bootloop evidence from OrangeFox recovery into logs/recovery-diag-*/.
# Run while the tablet is booted into OrangeFox and connected by USB.
set -eu

cd "$(dirname "$0")/.."
mkdir -p logs
OUT="logs/recovery-diag-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT"

sh_() { ./tools/root-usb.sh adb shell "$@" 2>&1 || true; }

{
    echo "=== date ==="; date -u
    echo "=== devices ==="; ./tools/root-usb.sh adb devices 2>&1
    echo "=== recovery identity ==="; sh_ 'getprop ro.recovery.name; getprop ro.build.display.id'
    echo "=== recovery log (tail 500) ==="; sh_ 'tail -500 /tmp/recovery.log'
    echo "=== saved recovery logs ==="; sh_ 'ls -laR /data/media/0/OrangeFox/logs 2>/dev/null; ls -laR /sdcard/OrangeFox/logs 2>/dev/null'
    echo "=== mounts ==="; sh_ 'mount'
    echo "=== pstore / last kmsg ==="; sh_ 'ls -la /sys/fs/pstore 2>/dev/null; for f in /sys/fs/pstore/*; do [ -f "$f" ] && { echo "--- $f"; tail -300 "$f"; }; done'
    echo "=== fstab files ==="; sh_ 'for f in /fstab.* /vendor/etc/fstab.* /odm/etc/fstab.* /system/etc/fstab.* /system_root/etc/fstab.*; do [ -f "$f" ] && { echo "--- $f"; cat "$f"; }; done'
    echo "=== dynamic partitions ==="; sh_ 'lpdump 2>/dev/null || echo "lpdump unavailable"'
    echo "=== block devices ==="; sh_ 'ls -l /dev/block/by-name 2>/dev/null'
    echo "=== filesystem types ==="; sh_ 'for p in system system_ext product vendor odm; do echo "--- $p"; blkid /dev/block/by-name/$p 2>/dev/null || echo no-node; done'
} > "$OUT/report.txt"

echo "Saved $OUT/report.txt"
wc -l "$OUT/report.txt"
