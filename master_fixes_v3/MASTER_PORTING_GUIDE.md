# Master Porting Guide: OnePlus Pad 4 (OPD) to Redmi Pad Pro / POCO Pad (ruan/dizi)

## 1. Hardware Architecture & Platform Specifications

| Component | Target Device (`ruan` / `dizi`) | Donor Device (OnePlus Pad / OPD) |
|---|---|---|
| **SoC** | Qualcomm SM7435-AB (Snapdragon 7s Gen 2) | MediaTek / Qualcomm Flagship Platform |
| **Codename** | `ruan` (5G / Cellular), `dizi` (Wi-Fi) | OPD2404 / OnePlus Pad |
| **Display** | 12.1" 2.5K LCD, 120Hz/144Hz | 11.61" 144Hz LCD |
| **Partitioning** | Virtual A/B with Retrofit Dynamic Partitions (`super`) | Virtual A/B Dynamic Partitions |
| **Dynamic Partitions** | `system`, `system_ext`, `product`, `vendor`, `odm`, `vendor_dlkm` | `system`, `system_ext`, `product`, `vendor`, `odm`, `my_*` |
| **Filesystem Types** | Logical: **EROFS** (`lz4hc`); Userdata: **F2FS** | Logical: **EROFS**; Userdata: **F2FS** |
| **Stock Kernel** | Linux 5.10.246-android12-9 (ab15159037) GKI | Linux 5.10 / 5.15 GKI |

---

## 2. The 7 Golden Rules of Porting to `ruan`/`dizi`

### Rule 1: Dynamic Partitions Must Stay Pure EROFS
Never convert `system_a` or `system_ext_a` to `ext4`. The device tree and `fstab.qcom` expect **EROFS** logical images. Rebuild images with:
```bash
/system/bin/mkfs.erofs -zlz4hc,9 -C4096 --all-root --mount-point=<mount> --file-contexts=<file_contexts> <out.img> <tree>
```

### Rule 2: Userdata Must Be Formatted as F2FS with Encryption Flags
Standard `fastboot -w` creates an `ext4` filesystem. ColorOS `fstab.qcom` strictly requires F2FS with:
`encrypt,extra_attr,project_quota,quota,casefold -C utf8`.
If formatted as ext4, `vold` cannot mount `/data`, triggering an immediate reboot loop into recovery.

### Rule 3: Strict File Permissions on Staged Apps (The 0644 Rule)
When staging apps, libraries, or overlays from donors or sdcards, they inherit `0660` or `0600` permissions.
- **Directories**: Must be `0755` (`drwxr-xr-x`, owned by `0:0`).
- **Files**: Must be `0644` (`-rw-r--r--`, owned by `0:0`).
- **Binaries**: Must be `0755` (`-rwxr-xr-x`, owned by `0:0`).
If an APK has `0600`, Android's `SystemServer` crashes on boot with `SecurityException`, causing the POCO logo bootloop.

### Rule 4: Match Kernel Symbol CRCs for Display (`msm_drm.ko`)
Xiaomi's continuous-splash handoff and display driver (`msm_drm.ko`) strictly check symbol CRCs. Use either:
1. The **stock Xiaomi HyperOS prebuilt GKI Image** (`5.10.246-android12-9`), OR
2. The LineageOS `sm7435` kernel (`noble6/device_xiaomi_ruan`) compiled against stock Module.symvers.

### Rule 5: Always Flash in `fastbootd`, Not Bootloader Fastboot
Xiaomi's ABL (Android Bootloader) fastboot has a hardware bug when connected over USB hubs or certain Type-C host controllers, stalling all bulk transfers.
Always boot into OrangeFox Recovery and tap **Menu → Reboot → Fastboot** (`fastbootd`).

### Rule 6: Respect the LpMetadata 4-Byte Struct Alignment
In `struct LpMetadataPartition`, offset 36 contains a 4-byte `attributes` field (`uint32_t`). When parsing or packing:
`attrs, first_ext, n_ext, grp = struct.unpack("<IIII", pdata[36:52])`
Omitting `attributes` shifts `first_ext` by 4 bytes, causing `system_a` to point to `system_ext_a`, completely unmapping the system partition!

### Rule 7: Verified Boot (AVB)
Always flash `vbmeta_a`, `vbmeta_b`, `vbmeta_system_a`, `vbmeta_system_b` with:
`--disable-verity --disable-verification`
to permit customized dynamic partition hashtrees.
