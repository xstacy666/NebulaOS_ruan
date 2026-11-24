#!/bin/bash
set -e
echo "========================================================"
echo "  Nebula OS (ColorOS 17) Fastboot ROM"
echo "  Target: Redmi Pad Pro 5G (ruan) & Wi-Fi (dizi)"
echo "  Maintainers: Øpium | Team Nebula"
echo "  Credits: @Noble_actual117, @GAURA_140110, @NotYoursAditya, @Gignigga"
echo "========================================================"
echo ""

for part in boot vendor_boot init_boot dtbo; do
    if [ -f "images/${part}.img" ]; then
        echo "Flashing ${part}..."
        fastboot flash ${part}_a "images/${part}.img"
        fastboot flash ${part}_b "images/${part}.img"
    fi
done

for vb in vbmeta vbmeta_system vbmeta_vendor; do
    if [ -f "images/${vb}.img" ]; then
        echo "Flashing ${vb} with disabled verity..."
        fastboot --disable-verity --disable-verification flash ${vb}_a "images/${vb}.img"
        fastboot --disable-verity --disable-verification flash ${vb}_b "images/${vb}.img"
    fi
done

if [ -f "images/super.img" ]; then
    echo "Flashing super partition..."
    fastboot flash super "images/super.img"
fi

echo "Wiping userdata..."
fastboot -w
echo "Rebooting to Nebula OS..."
fastboot reboot
echo "Flashing completed successfully!"
