#!/system/bin/sh
# Runs early, before the kernel loads vendor modules.
# KernelSU v3 needs a metamodule for file overlays, so bind-mount instead.
MODDIR=${0%/*}

mount_module() {
    src="$1"
    dst="$2"
    if [ -f "$src" ] && [ -e "$dst" ]; then
        mount --bind "$src" "$dst" 2>/dev/null
        echo "dt2w-fix: bind-mounted $(basename "$dst")" > /dev/kmsg
    else
        echo "dt2w-fix: missing $src or $dst" > /dev/kmsg
    fi
}

# 1) Creates /proc/tp_gesture (stock kernel never calls init_lct_tp_gesture)
mount_module "$MODDIR/files/lct_tp.ko" /vendor_dlkm/lib/modules/lct_tp.ko

# 2) Makes the driver report the detected double-click gesture as a wake event
#    even when its db_wakeup flag is 0 (the stock flag never gets set here).
mount_module "$MODDIR/files/nt36532_spi.ko" /vendor_dlkm/lib/modules/nt36532_spi.ko
