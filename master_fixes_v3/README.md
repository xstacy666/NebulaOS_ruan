# OnePlus Pad (OPD) / ColorOS 17 ROM Port to Redmi Pad Pro & POCO Pad (ruan/dizi)
### Master Fixes, Architecture Documentation & Engineering Guide (v3.0)

This repository contains the complete engineering knowledge base, tools, patches, and configurations for porting **OnePlus Pad / ColorOS 17 / OxygenOS 15 (Android 15)** to the **Redmi Pad Pro / POCO Pad** (codenames `ruan` [5G] and `dizi` [Wi-Fi], Qualcomm Snapdragon 7s Gen 2 / SM7435-AB "parrot" platform).

---

## Package Directory Structure

```text
OnePlus_Pad_Port_Master_Fixes_v3/
├── README.md                  # This file
├── MASTER_PORTING_GUIDE.md    # Step-by-step master reproduction guide
├── BUGS_AND_SOLUTIONS.md      # Exhaustive root-cause & fix analysis of all 10+ port bugs
├── AI_INSTRUCTIONS.md         # Exact guidelines and rules for another AI assistant
├── tools/
│   ├── build_clean_super.sh   # Rebuilds pure EROFS system/system_ext and sparse super.img
│   ├── flash.sh               # Universal Fastboot / Fastbootd flashing script
│   ├── fix_permissions.sh     # Fixes 0644/0755 permissions on extracted trees
│   ├── format_f2fs_userdata.sh# Formats userdata with ColorOS-required F2FS encryption flags
│   ├── repair_super_metadata.py # Fixes LpMetadata 4-byte struct alignment bug
│   ├── patch_systemui_oti.py  # Bytecode patch for 120Hz touch idle pause on screen-off
│   └── patch_settings_privacy.py # Bytecode patch for Settings privacy password & account link
├── configs/
│   ├── performance.prop       # 120Hz display refresh, SurfaceFlinger phase offsets, animation
│   └── oppo-features-list.txt # Audit list of 37 ported ColorOS/OPPO donor applications
├── overlays/
│   └── RuanCornerOverlay.apk  # Zero-radius corner overlay to prevent double-rounded panel corners
└── reference/
    ├── cryptoeng_source/      # Rust source code for the ICryptoeng TEE HAL shim
    └── donor-packages.txt     # Complete path mappings for donor apps
```

---

## Quick Reference Commands

### To Rebuild the Super Image:
```bash
su -c '/data/user/0/ai.antigem/files/usr/bin/bash tools/build_clean_super.sh'
```

### To Flash in `fastbootd` (OrangeFox -> Reboot -> Fastboot):
```bash
su -c 'sh tools/flash.sh'
```

### To Format Userdata with Required F2FS Encryption:
```bash
su -c 'sh tools/format_f2fs_userdata.sh'
```
