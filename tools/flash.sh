#!/system/bin/sh

# Universal Fastboot Flashing Script for Fairy Flasher & Termux

# Auto-detect fastboot binary
if command -v fastboot >/dev/null 2>&1; then
    FB="fastboot"
elif [ -x "/data/user/0/ai.antigem/files/usr/bin/fastboot" ]; then
    export HOME="/data/user/0/ai.antigem/files/home"
    export PATH="/data/user/0/ai.antigem/files/usr/bin:$PATH"
    FB="/data/user/0/ai.antigem/files/usr/bin/fastboot"
else
    FB="fastboot"
fi

DIR="/storage/emulated/0/Download/ColorOS17_Android17_RedmiPadPro_ruan_dizi_Fastboot_v106/images"

echo "=== 1. Checking Fastboot Devices ==="
$FB devices

echo "=== 2. Flashing VBMeta with Verity Disabled ==="
$FB --disable-verity --disable-verification flash vbmeta_a "$DIR/vbmeta.img"
$FB --disable-verity --disable-verification flash vbmeta_b "$DIR/vbmeta.img"
$FB --disable-verity --disable-verification flash vbmeta_system_a "$DIR/vbmeta_system.img"
$FB --disable-verity --disable-verification flash vbmeta_system_b "$DIR/vbmeta_system.img"

echo "=== 3. Flashing Boot, Vendor_Boot, DTBO ==="
$FB flash boot_a "$DIR/boot.img"
$FB flash boot_b "$DIR/boot.img"
$FB flash vendor_boot_a "$DIR/vendor_boot.img"
$FB flash vendor_boot_b "$DIR/vendor_boot.img"
$FB flash dtbo_a "$DIR/dtbo.img"
$FB flash dtbo_b "$DIR/dtbo.img"

echo "=== 4. Flashing Clean Sparse Super Image (7.4 GB) ==="
$FB flash super "$DIR/super.img"

echo "=== 5. Erasing Stale FRP ==="
$FB erase frp

echo "=== 6. Flashing Complete! Rebooting Tablet... ==="
$FB reboot
