#!/bin/sh
# Convert a fastboot ROM's EROFS dynamic partitions to ext4.
#
# Usage:  sh convert_erofs_to_ext4.sh <workdir>
#
# <workdir> must contain images/super.img, images/boot.img and
# images/vendor_boot.img from an extracted fastboot ROM.  The converted
# ROM is written to <workdir>/out_ext4/.
#
# Pure system tooling: simg2img / lpunpack / fsck.erofs / mkfs.ext4 /
# lpmake / unpack_bootimg / mkbootimg.  No root, no KSU, no Magisk.

set -e
WD="${1:?usage: $0 <workdir>}"
IMG="$WD/images"
OUT="$WD/out_ext4"
TMP="$WD/tmp_convert"
mkdir -p "$OUT/images" "$TMP"

echo "[*] 1/7  Expand sparse super.img"
simg2img "$IMG/super.img" "$TMP/super_raw.img"

echo "[*] 2/7  Read dynamic partition layout"
lpdump "$TMP/super_raw.img" > "$TMP/lpdump.txt" 2>&1 || true
cat "$TMP/lpdump.txt"

echo "[*] 3/7  Unpack dynamic partitions"
mkdir -p "$TMP/parts"
lpunpack "$TMP/super_raw.img" "$TMP/parts"

echo "[*] 4/7  Convert each EROFS partition to ext4"
for f in "$TMP/parts/"*.img; do
    [ -f "$f" ] || continue
    part=$(basename "$f" .img)
    magic=$(dd if="$f" bs=1 skip=1024 count=4 2>/dev/null | od -An -tx1 | tr -d ' \n')
    # EROFS superblock magic (little endian) = 0x E0 F2 5D 24 ... check both
    if fsck.erofs -s "$f" >/dev/null 2>&1; then
        echo "    - $part: EROFS -> ext4"
        mkdir -p "$TMP/tree_$part"
        fsck.erofs --extract="$TMP/tree_$part" --overwrite --no-preserve "$f" >/dev/null 2>&1
        size=$(stat -c %s "$f")
        blocks=$((size / 4096))
        rm -f "$f"
        mkfs.ext4 -q -b 4096 -d "$TMP/tree_$part" -L "$part" "$f" "$blocks"
    else
        echo "    - $part: not EROFS, left untouched"
    fi
done

echo "[*] 5/7  Rebuild super.img"
# Partition sizes are read from lpdump; see rebuild_super.py for the exact
# lpmake invocation (sizes/groups are preserved from the original layout).
python3 "$(dirname "$0")/rebuild_super.py" "$TMP/lpdump.txt" "$TMP/parts" "$OUT/images/super.img"

echo "[*] 6/7  Patch fstab in vendor_boot ramdisk (erofs -> ext4)"
mkdir -p "$TMP/vb"
unpack_bootimg --boot_img "$IMG/vendor_boot.img" --out "$TMP/vb" >/dev/null 2>&1
if [ -f "$TMP/vb/ramdisk" ]; then
    mkdir -p "$TMP/vbram" && cd "$TMP/vbram"
    cpio -idmu < "$TMP/vb/ramdisk" 2>/dev/null || true
    for f in $(grep -rl 'erofs' . 2>/dev/null); do
        sed -i 's/ erofs / ext4 ro,barrier=1,discard /g' "$f"
    done
    find . | cpio -o -H newc > "$TMP/vb/ramdisk" 2>/dev/null || true
fi

echo "[*] 7/7  Repack vendor_boot.img and copy remaining images"
mkbootimg --vendor_boot "$TMP/vb" --out "$OUT/images/vendor_boot.img" 2>/dev/null || \
    cp "$IMG/vendor_boot.img" "$OUT/images/vendor_boot.img"
for f in boot.img dtbo.img vbmeta.img vbmeta_system.img flash.sh flash.bat; do
    [ -f "$IMG/$f" ] && cp "$IMG/$f" "$OUT/images/$f" 2>/dev/null || true
done
[ -f "$WD/flash.sh" ] && cp "$WD/flash.sh" "$OUT/"
[ -f "$WD/flash.bat" ] && cp "$WD/flash.bat" "$OUT/"

echo "[+] Done: $OUT"
