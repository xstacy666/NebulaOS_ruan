#!/system/bin/sh
# format_f2fs_userdata.sh
# Formats /data and /metadata with the exact flags required by ColorOS / OxygenOS fstab.qcom.
# Standard "fastboot -w" formats data as ext4, which triggers boot failure because
# ColorOS strictly requires F2FS with metadata encryption and casefolding.

set -eu

USERDATA="/dev/block/by-name/userdata"
METADATA="/dev/block/by-name/metadata"
MISC="/dev/block/by-name/misc"

echo "=== 1. Checking partitions ==="
if [ ! -b "$USERDATA" ]; then
    echo "Error: $USERDATA block device not found. Are you running this in recovery?"
    exit 1
fi

echo "=== 2. Formatting userdata with native F2FS ==="
/system/bin/make_f2fs -f -g android -O encrypt,extra_attr,project_quota,quota,casefold -C utf8 "$USERDATA"

echo "=== 3. Formatting metadata with ext4 ==="
if [ -b "$METADATA" ]; then
    /system/bin/mke2fs -t ext4 -b 4096 "$METADATA"
fi

echo "=== 4. Clearing misc bootloader message (BCB) ==="
if [ -b "$MISC" ]; then
    dd if=/dev/zero of="$MISC" bs=4096 count=1
fi

echo "=== Success! Userdata and metadata are cleanly prepared. ==="
