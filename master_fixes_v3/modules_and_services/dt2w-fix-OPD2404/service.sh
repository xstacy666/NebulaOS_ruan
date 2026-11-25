#!/system/bin/sh
# Runs as root in late_start. Keeps DT2W armed.
MODDIR=${0%/*}

settings put secure double_tap_to_wake 1 2>/dev/null
settings put system double_wake_status 1 2>/dev/null

# Wait for the patched module to create the gesture node
i=0
while [ ! -e /proc/tp_gesture ] && [ $i -lt 60 ]; do
    sleep 1
    i=$((i + 1))
done

if [ ! -e /proc/tp_gesture ]; then
    echo "dt2w-fix: /proc/tp_gesture missing" > /dev/kmsg
    exit 1
fi

echo "dt2w-fix: gesture node ready, arming DT2W" > /dev/kmsg

# db_wakeup can be cleared on resume, so re-apply both flags periodically.
while true; do
    echo 1 > /proc/tp_gesture 2>/dev/null
    "$MODDIR/files/dt2wctl" 0 24 1 >/dev/null 2>&1
    sleep 2
done
