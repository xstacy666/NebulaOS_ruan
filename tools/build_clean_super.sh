#!/bin/bash
set -euo pipefail

PROJECT="/data/user/0/ai.antigem/files/home/projects/opd4-pocopad-port"
SYS="$PROJECT/work/sys2"
SEXT="$PROJECT/work/se2"
PARTS="$PROJECT/work/stockparts"
OUT_DIR="/storage/emulated/0/Download/ColorOS17_Android17_RedmiPadPro_ruan_dizi_Fastboot_v106/images"
LPMAKE="/data/user/0/ai.antigem/files/usr/bin/lpmake"
MKFS_EROFS="/system/bin/mkfs.erofs"

DEVICE_SIZE=9126805504
GROUP_A_NAME="qti_dynamic_partitions_a"
GROUP_B_NAME="qti_dynamic_partitions_b"

echo "=== Step 1: Checking/Building system_ext_a_new.img ==="
if [ ! -f "$PROJECT/work/system_ext_a_new.img" ]; then
    echo "Building system_ext_a_new.img..."
    $MKFS_EROFS -zlz4hc,9 -C4096 --all-root --mount-point=/system_ext \
        --file-contexts="$SEXT/etc/selinux/system_ext_file_contexts" \
        "$PROJECT/work/system_ext_a_new.img" "$SEXT"
fi
echo "system_ext_a_new.img ready: $(ls -lh "$PROJECT/work/system_ext_a_new.img")"

echo "=== Step 2: Building system_a_new.img ==="
rm -f "$PROJECT/work/system_a_new.img"
echo "Running mkfs.erofs on sys2..."
$MKFS_EROFS -zlz4hc,9 -C4096 --all-root --mount-point=/ \
    --file-contexts="$SYS/system/etc/selinux/plat_file_contexts" \
    "$PROJECT/work/system_a_new.img" "$SYS"
echo "system_a_new.img ready: $(ls -lh "$PROJECT/work/system_a_new.img")"

echo "=== Step 3: Packing clean sparse super.img ==="
size() { stat -c %s "$1"; }

SYSTEM=$(size "$PROJECT/work/system_a_new.img")
SEXT=$(size "$PROJECT/work/system_ext_a_new.img")
ODM=$(size "$PARTS/odm_a.img")
PRODUCT=$(size "$PARTS/product_a.img")
VENDOR=$(size "$PARTS/vendor_a.img")
VDLKM=$(size "$PARTS/vendor_dlkm_a.img")

FINAL_SUPER="$OUT_DIR/super_fixed_sparse.img"
rm -f "$FINAL_SUPER"

$LPMAKE \
  --metadata-size 65536 \
  --super-name super \
  --metadata-slots 3 \
  --virtual-ab \
  --sparse \
  --device "super:$DEVICE_SIZE" \
  --group "$GROUP_A_NAME:9126805504" \
  --group "$GROUP_B_NAME:9126805504" \
  --partition "system_a:readonly:$SYSTEM:$GROUP_A_NAME" \
  --image "system_a=$PROJECT/work/system_a_new.img" \
  --partition "system_ext_a:readonly:$SEXT:$GROUP_A_NAME" \
  --image "system_ext_a=$PROJECT/work/system_ext_a_new.img" \
  --partition "product_a:readonly:$PRODUCT:$GROUP_A_NAME" \
  --image "product_a=$PARTS/product_a.img" \
  --partition "vendor_a:readonly:$VENDOR:$GROUP_A_NAME" \
  --image "vendor_a=$PARTS/vendor_a.img" \
  --partition "odm_a:readonly:$ODM:$GROUP_A_NAME" \
  --image "odm_a=$PARTS/odm_a.img" \
  --partition "vendor_dlkm_a:readonly:$VDLKM:$GROUP_A_NAME" \
  --image "vendor_dlkm_a=$PARTS/vendor_dlkm_a.img" \
  --partition "system_b:readonly:0:$GROUP_B_NAME" \
  --partition "system_ext_b:readonly:0:$GROUP_B_NAME" \
  --partition "product_b:readonly:0:$GROUP_B_NAME" \
  --partition "vendor_b:readonly:0:$GROUP_B_NAME" \
  --partition "odm_b:readonly:0:$GROUP_B_NAME" \
  --partition "vendor_dlkm_b:readonly:0:$GROUP_B_NAME" \
  --output "$FINAL_SUPER"

echo "=== BUILD COMPLETE! ==="
ls -lh "$FINAL_SUPER"
