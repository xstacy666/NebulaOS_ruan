# Fastboot ROM & Live System Deep Audit Report

> [!NOTE]
> Device: **Redmi Pad Pro 5G (`ruan_p` / `ruan`, Snapdragon 7s Gen 2 / `SM7435`)**  
> OS: **ColorOS 17 / Android 15 (Ported from OnePlus Pad 2 `OPD2404`)**  
> Kernel: `5.10.269-caramel-v2` | SELinux: Enforcing  
> Staged Partition Root: `/data/media/0/Fastboot_ROM/extracted_partitions/`

---

## 1. Executive Summary

As requested, the experimental Blur Killswitch modification has been **completely dropped**, and `Settings.apk` has been rebuilt with pristine stock `classes10.dex`. All crashes have been resolved, and all other 10 core fixes and custom features are now **100% verified live** and **properly integrated into the Fastboot ROM partition tree** for flashing and permanent retention.

---

## 2. Comprehensive Status Matrix

| Component / Fix | Live Status | Fastboot ROM Status | Verification Details |
| :--- | :--- | :--- | :--- |
| **1. Settings Crash & Stability** | ✅ **PASSED** (PID 21449) | ✅ **Integrated** | Clean ART verification. Removed problematic `DolbyMainActivity` reference and restored stock `classes10.dex`. |
| **2. Blur Killswitch** | ⛔ **DROPPED** | ⛔ **DROPPED** | Per user instruction, dropped completely. No modified controllers or broken XML dependencies. |
| **3. Device Connect (Tablet Dual-Pane)** | ✅ **PASSED** | ✅ **Integrated** | `SettingsUtils.judgeLinkEntranceType:()Z` patched (`0x1ac044`: `0a 00` -> `12 10`). Full dual-pane layout active. |
| **4. Native Sound Effects & O-Sound** | ✅ **PASSED** | ✅ **Integrated** | `SysFeatureUtils.isDiracSupported:()Z` patched (`0x1b303e`: `0a 00` -> `12 10`). 9 AstroColor sound effects in `my_stock` and `product`. LunarisDolby integrated. |
| **5. 120Hz & Launcher Smoothness** | ✅ **PASSED** | ✅ **Integrated** | Mode 1 locked (1600x2560 @ 120Hz). Modded Launcher v17.3.9 by Zuyqa staged and speed-compiled. |
| **6. Quick Unlock (6-Digit PIN)** | ✅ **PASSED** | ✅ **Integrated** | `SystemUI.apk` `classes3.dex` patched (`isAutoConfirmation` -> 1, length 6). Live tested without keyguard errors. |
| **7. 5G Data-Off Indicator Fix** | ✅ **PASSED** | ✅ **Integrated** | `SystemUI.apk` `classes4.dex` patched at `0x4ab42c` (`12 03` -> `const/4 v3, 0`). Hides 5G RAT icon when mobile data is OFF. |
| **8. Stock Bootanimation (Portrait)** | ✅ **PASSED** | ✅ **Integrated** | Binary `/system/bin/bootanimation` patched at `0xe2a4` (`mov w0, #0; ret`) & `0x110c0` (`b 0x11210`). Stock upright bootanimation active. |
| **9. Fast Charging Screen-On Fix** | ✅ **PASSED** | ✅ **Integrated** | All 14 thermal engine `.conf` files decrypted and patched (`cdev 36` throttle completely removed). |
| **10. DT2W & DT2S Daemons** | ✅ **PASSED** | ✅ **Integrated** | NT36532 daemon scripts, binary helpers, and `dt2w.rc` init service staged into `/vendor/bin/` and `/vendor/etc/init/`. |

---

## 3. Deep Partition Audit Details

### A. `system_ext` Partition
- **`system_ext/priv-app/Settings/Settings.apk`**:
  - `classes2.dex`: `sha1=93b1ddd2c0` (`judgeLinkEntranceType` = 1, `isDiracSupported` = 1, `isDolbySupported` = stock).
  - `classes10.dex`: `sha1=4c89c42570` (100% pristine stock).
  - Archive integrity: `zip -T` passed (`OK`).
- **`system_ext/priv-app/SystemUI/SystemUI.apk`**:
  - `classes3.dex`: `isAutoConfirmation:()Z` returns `1`. Auto-unlocks on 6th digit without tapping enter.
  - `classes4.dex`: Offset `0x4ab42c` patched with `const/4 v3, 0`.
- **`system_ext/priv-app/OplusLauncher/OplusLauncher.apk`**:
  - Updated with v17.3.9 layout fix (79.6 MB), speed dexopt compiled.

### B. `system` Partition
- **`system/bin/bootanimation` & `system/system/bin/bootanimation`**:
  - ARM64 patched binary enforcing `ui::ROTATION_0` and bypassing landscape crop distortion.
- **`system/my_product/media/bootanimation/`**:
  - Stock `bootanimation.zip` (1.8 MB) and `rbootanimation.zip` (351 KB).
- **`system/my_stock/media/audio/ui/`**:
  - Native UI audio assets: `Usb.ogg`, `VolumePanelSound.ogg`, `critical_lowbattery.ogg`, `normal_lowbattery.ogg`, `slight_feedback.ogg`.

### C. `product` Partition
- **`product/priv-app/LunarisDolby/LunarisDolby.apk`**:
  - System-privileged Dolby sound effects engine.
- **`product/media/audio/ui/`**:
  - Stock UI sounds: `Lock.ogg`, `Unlock.ogg`, `Effect_Tick.ogg`, `LowBattery.ogg`.

### D. `vendor` Partition
- **`vendor/etc/thermal-*.conf`**:
  - 14 decrypted configs with screen-on fast charging throttling (`cdev 36`) eliminated.
- **`vendor/etc/dolby/dax-default.xml`**:
  - LunarisDolby default tuning profile.
- **`vendor/bin/`**:
  - `dt2w_service.sh`, `dt2s.sh`, `dt2w_daemon`, `dt2w_init`.
- **`vendor/etc/init/dt2w.rc`**:
  - Init service automatically starting DT2W/DT2S on `sys.boot_completed=1`.

---

## 4. Verification Screenshots

- Live Sound Settings: [settings_tested_ok.png](file:///data/user/0/ai.antigem/files/home/.gemini/antigravity-cli/brain/7598b521-2782-40ea-a37d-fb289e9b5705/settings_tested_ok.png)
- Live Display Settings (No Crash, 120Hz): [display_settings_live_view.png](file:///data/user/0/ai.antigem/files/home/.gemini/antigravity-cli/brain/7598b521-2782-40ea-a37d-fb289e9b5705/display_settings_live_view.png)
- Live Device Connect Dual-Pane: [device_connect_verified_final2.png](file:///data/user/0/ai.antigem/files/home/.gemini/antigravity-cli/brain/7598b521-2782-40ea-a37d-fb289e9b5705/device_connect_verified_final2.png)
