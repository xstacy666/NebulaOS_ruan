#!/system/bin/sh
# Manual re-arm (KernelSU action button)
MODDIR=${0%/*}
settings put secure double_tap_to_wake 1 2>/dev/null
settings put system double_wake_status 1 2>/dev/null
if [ -e /proc/tp_gesture ]; then
    echo 0 > /proc/tp_gesture 2>/dev/null
    sleep 1
    echo 1 > /proc/tp_gesture 2>/dev/null
    "$MODDIR/files/dt2wctl" 0 24 1 >/dev/null 2>&1
    echo "DT2W re-armed"
else
    echo "Error: /proc/tp_gesture missing (reboot after installing the module)"
fi
