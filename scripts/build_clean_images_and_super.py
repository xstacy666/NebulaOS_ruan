#!/usr/bin/env python3
import os
import sys
import subprocess
import time

TREES = '/data/user/0/ai.antigem/files/home/projects/opd4-pocopad-port/work/trees_clean'
PARTS = '/data/user/0/ai.antigem/files/home/projects/opd4-pocopad-port/work/stockparts'
OUT_DIR = '/data/user/0/ai.antigem/files/home/projects/opd4-pocopad-port/work/clean_output'
FINAL_ROM_DIR = '/storage/emulated/0/Download/ColorOS17_Android17_RedmiPadPro_ruan_dizi_Fastboot_v106/images'
FINAL_SUPER = os.path.join(FINAL_ROM_DIR, 'super.img')

os.makedirs(OUT_DIR, exist_ok=True)
os.makedirs(FINAL_ROM_DIR, exist_ok=True)

MKFS = '/data/user/0/ai.antigem/files/usr/bin/mkfs.erofs'
LPMAKE = '/data/user/0/ai.antigem/files/usr/bin/lpmake'

DEVICE_SIZE = 9126805504
GROUP_SIZE = 9122611200
GROUP_A = "qti_dynamic_partitions_a"
GROUP_B = "qti_dynamic_partitions_b"

jobs = [
    {
        'name': 'product_a',
        'src': os.path.join(TREES, 'product'),
        'out': os.path.join(OUT_DIR, 'product_a.img'),
        'mp': '/product',
        'fc': os.path.join(TREES, 'product/etc/selinux/product_file_contexts'),
    },
    {
        'name': 'vendor_dlkm_a',
        'src': os.path.join(TREES, 'vendor_dlkm'),
        'out': os.path.join(OUT_DIR, 'vendor_dlkm_a.img'),
        'mp': '/vendor_dlkm',
        'fc': None,
    },
    {
        'name': 'vendor_a',
        'src': os.path.join(TREES, 'vendor'),
        'out': os.path.join(OUT_DIR, 'vendor_a.img'),
        'mp': '/vendor',
        'fc': os.path.join(TREES, 'vendor/etc/selinux/vendor_file_contexts'),
    },
    {
        'name': 'system_ext_a',
        'src': os.path.join(TREES, 'system_ext'),
        'out': os.path.join(OUT_DIR, 'system_ext_a.img'),
        'mp': '/system_ext',
        'fc': os.path.join(TREES, 'system_ext/etc/selinux/system_ext_file_contexts'),
    },
    {
        'name': 'system_a',
        'src': os.path.join(TREES, 'system'),
        'out': os.path.join(OUT_DIR, 'system_a.img'),
        'mp': '/',
        'fc': os.path.join(TREES, 'system/system/etc/selinux/plat_file_contexts'),
    },
]

print("================================================================")
print("  STEP 1: COMPILING EROFS PARTITION IMAGES                      ")
print("================================================================")

total_start = time.time()

# Remove old super.img to free up storage space immediately
if os.path.exists(FINAL_SUPER):
    print(f"[*] Freeing space: removing old {FINAL_SUPER} ({os.path.getsize(FINAL_SUPER) / (1024*1024):.1f} MB)...")
    os.remove(FINAL_SUPER)

for job in jobs:
    name = job['name']
    out_img = job['out']
    src_dir = job['src']
    mp = job['mp']
    fc = job['fc']

    if name in ['product_a', 'vendor_a', 'vendor_dlkm_a'] and os.path.exists(out_img):
        print(f"[*] Skipping already compiled unchanged image: {name}.img ({os.path.getsize(out_img) / (1024*1024):.1f} MB)")
        continue

    print(f"\n[*] Compiling {name}.img from {src_dir} (mount-point: {mp})...")
    cmd = [
        MKFS,
        "-zlz4hc,9",
        "-C4096",
        "--all-root",
        f"--mount-point={mp}"
    ]
    if fc and os.path.exists(fc):
        cmd.append(f"--file-contexts={fc}")
    cmd.extend([out_img, src_dir])

    t0 = time.time()
    res = subprocess.run(cmd)
    t1 = time.time()
    if res.returncode != 0:
        print(f"[-] ERROR: Failed to build {name}.img! Code: {res.returncode}")
        sys.exit(res.returncode)
    
    sz_mb = os.path.getsize(out_img) / (1024 * 1024)
    print(f"[+] Built {name}.img: {sz_mb:.2f} MB in {t1 - t0:.1f}s")

print(f"\n[+] All 5 partition images compiled in {time.time() - total_start:.1f}s")

# ==============================================================================
# STEP 2: PACKING CLEAN SPARSE SUPER.IMG
# ==============================================================================
print("\n================================================================")
print("  STEP 2: PACKING CLEAN SPARSE SUPER.IMG                        ")
print("================================================================")

odm_img = os.path.join(PARTS, 'odm_a.img')
if not os.path.exists(odm_img):
    print(f"[-] ERROR: odm_a.img not found at {odm_img}")
    sys.exit(1)

def get_size(path):
    return os.path.getsize(path)

img_map = {
    'system_a': os.path.join(OUT_DIR, 'system_a.img'),
    'system_ext_a': os.path.join(OUT_DIR, 'system_ext_a.img'),
    'product_a': os.path.join(OUT_DIR, 'product_a.img'),
    'vendor_a': os.path.join(OUT_DIR, 'vendor_a.img'),
    'vendor_dlkm_a': os.path.join(OUT_DIR, 'vendor_dlkm_a.img'),
    'odm_a': odm_img,
}

sizes = {k: get_size(v) for k, v in img_map.items()}

print("\nPartition Sizes:")
total_dyn_bytes = 0
for k, sz in sizes.items():
    print(f"  - {k}: {sz} bytes ({sz / (1024*1024):.2f} MB)")
    total_dyn_bytes += sz

print(f"\nTotal dynamic partitions size: {total_dyn_bytes} bytes ({total_dyn_bytes / (1024*1024):.2f} MB)")
print(f"Group ceiling: {GROUP_SIZE} bytes ({GROUP_SIZE / (1024*1024):.2f} MB)")

if total_dyn_bytes > GROUP_SIZE:
    print(f"[-] ERROR: Total partitions size exceeds group size ceiling by {total_dyn_bytes - GROUP_SIZE} bytes!")
    sys.exit(1)

tmp_super = FINAL_SUPER + '.tmp'
if os.path.exists(tmp_super):
    os.remove(tmp_super)

lpmake_cmd = [
    LPMAKE,
    "--metadata-size", "65536",
    "--super-name", "super",
    "--metadata-slots", "3",
    "--virtual-ab",
    "--sparse",
    "--device", f"super:{DEVICE_SIZE}",
    "--group", f"{GROUP_A}:{GROUP_SIZE}",
    "--group", f"{GROUP_B}:{GROUP_SIZE}",
]

for part_name, img_path in img_map.items():
    sz = sizes[part_name]
    lpmake_cmd.extend([
        "--partition", f"{part_name}:readonly:{sz}:{GROUP_A}",
        "--image", f"{part_name}={img_path}",
    ])

# Empty B slot partitions
for b_part in ['system_b', 'system_ext_b', 'product_b', 'vendor_b', 'vendor_dlkm_b', 'odm_b']:
    lpmake_cmd.extend([
        "--partition", f"{b_part}:readonly:0:{GROUP_B}"
    ])

lpmake_cmd.extend(["--output", tmp_super])

print(f"\n[*] Executing lpmake to pack {FINAL_SUPER}...")
t0 = time.time()
res = subprocess.run(lpmake_cmd)
t1 = time.time()

if res.returncode != 0:
    print(f"[-] ERROR: lpmake failed with exit code {res.returncode}")
    sys.exit(res.returncode)

# Replace target super.img
os.replace(tmp_super, FINAL_SUPER)
final_sz = os.path.getsize(FINAL_SUPER)
print(f"[+] Sparse super.img successfully packed in {t1 - t0:.1f}s!")
print(f"[+] Target location: {FINAL_SUPER}")
print(f"[+] Final sparse size: {final_sz} bytes ({final_sz / (1024*1024):.2f} MB)")

print("\n================================================================")
print("  BUILD COMPLETE! READY FOR FASTBOOT FLASHING                   ")
print("================================================================")
