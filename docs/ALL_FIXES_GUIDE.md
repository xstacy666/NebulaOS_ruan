# Complete NebulaOS Hardware, Audio, Connectivity & Security Fixes Guide

**Target Devices**:
- Redmi Pad Pro 5G (`ruan` / `ruan_p`)
- Redmi Pad Pro & POCO Pad Wi-Fi (`dizi`)
**Base OS**: ColorOS 15 / 17 Port (Android 15)  
**SoC**: Qualcomm Snapdragon 7s Gen 2 (`SM7435-AB`)  
**Kernel**: 5.10.269-caramel-v2  

---

## Complete Modules & Fixes Index

1. [Fix 1: Privacy Password & HeyTap Account Linking](#fix-1-privacy-password--heytap-account-linking)
2. [Fix 2: Persistent Auto-Connecting ADB & SSH Bridge](#fix-2-persistent-auto-connecting-adb--ssh-bridge)
3. [Fix 3: Double Tap to Wake (DT2W) & Novatek Touchscreen](#fix-3-double-tap-to-wake-dt2w--novatek-touchscreen)
4. [Fix 4: Resolving Ext4 ROM Bootloop (EROFS vs Ext4)](#fix-4-resolving-ext4-rom-bootloop-erofs-vs-ext4)
5. [Fix 5: SELinux Enforcing Mode Policy](#fix-5-selinux-enforcing-mode-policy)
6. [Fix 6: Device Connect (OPlus Synergy / Cross-Device Link)](#fix-6-device-connect-oplus-synergy--cross-device-link)
7. [Fix 7: In-Call Audio & Microphone Fix (Cellular + VoIP)](#fix-7-in-call-audio--microphone-fix-cellular--voip)
8. [Fix 8: Wi-Fi (QCA6750) & Bluetooth Connectivity](#fix-8-wi-fi-qca6750--bluetooth-connectivity)
9. [Fix 9: Cellular / SIM & Mobile Network (Ruan 5G vs Dizi Wi-Fi)](#fix-9-cellular--sim--mobile-network-ruan-5g-vs-dizi-wi-fi)

---

### Fix 1: Privacy Password & HeyTap Account Linking
- **Issues**: Tapping "Link account" closes Settings; App Lock and Private Safe fail to set passwords.
- **Root Cause**:
  1. `Settings.apk` bytecode abort (`finish_privacy_chain=true`) in `LinkAccountActivity.onStop()`.
  2. Missing AIDL communication to `vendor.oplus.hardware.cryptoeng.ICryptoeng/default`.
  3. Missing HeyTap security intent provider `KeKeUserCenterAccount.apk` (`com.oplus.vip`).
- **Fix**:
  - Run `01_privacy_and_account_link/patch_settings_privacy.py` on `Settings.apk`.
  - Install `KeKeUserCenterAccount.apk` into `/my_stock/priv-app/`.
  - Injected `cryptoeng` HAL binder permissions into SELinux policy.

---

### Fix 2: Persistent Auto-Connecting ADB & SSH Bridge
- **Issues**: Tablet disconnects from debugging on every reboot; direct USB C-to-C cable requires physical host OTG.
- **Fix**:
  - Flash `02_adb_persistent_connection/adb-tcp-persistent.zip` in OrangeFox.
  - Adds `/data/adb/service.d/99-adbtcp.sh` to initialize `setprop service.adb.tcp.port 5555` automatically at boot.

---

### Fix 3: Double Tap to Wake (DT2W) & Novatek Touchscreen
- **Issues**: Double tap to wake does not wake the screen on the Novatek NT36532 display panel.
- **Fix**:
  - Install KernelSU module `03_dt2w_touchscreen_fix/dt2w-fix-OPD2404`.
  - Loads `lct_tp.ko` and `nt36532_spi.ko`, and runs `dt2wctl` listening on display wake events to toggle `/proc/touchpanel/double_tap_enable`.

---

### Fix 4: Resolving Ext4 ROM Bootloop (EROFS vs Ext4)
- **Issues**: Flashing converted ext4 dynamic partitions causes bootloop back to OrangeFox Recovery.
- **Root Cause**:
  - `ro.boot.veritymode=enforcing` with missing `dm-verity` hash tree blocks `first_stage_mount` of ext4 partitions.
  - ColorOS dynamic partitions uncompressed as ext4 exceed the super partition size ceiling.
- **Fix**:
  - Package images with EROFS (`mkfs.erofs -zlz4hc,9 -C 4096`).
  - If ext4 is mandatory, flash `vbmeta.img` with `fastboot flash --disable-verity --disable-verification vbmeta vbmeta.img`.

---

### Fix 5: SELinux Enforcing Mode Policy
- **Issues**: Services crash or fail binder transactions under Enforcing mode.
- **Fix**: Injected bidirectional Binder call and transfer rules between `hal_cryptoeng_client`, `system_app`, `system_server`, `priv_app` and `ksu` domain.

---

### Fix 6: Device Connect (OPlus Synergy / Cross-Device Link)
- **Issues**: "Device Connect" item was missing or failing to launch in Settings homepage.
- **Root Cause**:
  - `com.oplus.wirelesssettings.DEVICE_CONNECT` activity is gated by `com.oplus.subsys` binder connection to `IDmtpService` (`default_android_hwservice`).
  - Tablet layout fragment in `SettingsHomepageActivity` lacked direct intent forwarding.
- **Fix Included in `06_device_connect_and_synergy/`**:
  - Added SELinux permission allowing `system_app` access to `default_android_hwservice` (`IDmtpService`).
  - Enabled direct intent target `com.oplus.wirelesssettings/.DeviceConnectActivity` via `settings_feature_config.xml`.

---

### Fix 7: In-Call Audio & Microphone Fix (Cellular + VoIP)
- **Issues**: Complete silence (no speaker audio and no microphone input) during WhatsApp, Telegram, and SIM calls.
- **Root Cause**:
  - In Snapdragon 7s Gen 2 (`parrot` / `pineapple` audio HAL), the voice call use case calls `AudioVoice::RouteStream` with incorrect routing stream flags (`AudioVoice11RouteStream`).
  - The Xiaomi mixer paths (`mixer_paths_parrot_idp.xml`) and Qualcomm PAL audio resource manager (`resourcemanager_parrot_idp.xml`) routed VoIP and Cellular calls to an uninitialized earpiece handset device instead of the quad-speaker array and dual digital microphones.
- **Fix Included in `07_audio_calls_and_mic_fix/`**:
  - Replaced `/vendor/etc/audio/mixer_paths_parrot_idp.xml` and `/vendor/etc/audio/resourcemanager_parrot_idp.xml` with properly mapped voice streams routing `voice-call` and `voice-communication` to Speaker (`quad-spkr`) and top/bottom dual mics.
  - Updated `/vendor/etc/audio_policy_configuration.xml` to declare voice communication endpoints for both `primary` and `usb` audio routes.

---

### Fix 8: Wi-Fi (QCA6750) & Bluetooth Connectivity
- **Issues**: Wi-Fi drops, 5GHz band instability, and Bluetooth audio codec (LDAC/aptX) stuttering.
- **Root Cause**:
  - The Qualcomm QCA6750 Wi-Fi/BT combo chip expects exact regional SAR power limits (`n83_default_sar.xml`) and config parameters in `/vendor/etc/wifi/qca6750/WCNSS_qcom_cfg.ini`.
  - ColorOS stock configuration defaulted to single-antenna SISO mode (`gEnableMuBform=0`).
- **Fix Included in `08_wifi_bluetooth_connectivity/`**:
  - Tuned `WCNSS_qcom_cfg.ini` for dual-band 2x2 MIMO 802.11ax.
  - Overlay configurations for `wpa_supplicant` and Wi-Fi Direct P2P (`p2p_supplicant_overlay.conf`).
  - Synchronized Bluetooth HAL manifest entries in `manifest_ruan.xml`.

---

### Fix 9: Cellular / SIM & Mobile Network (Ruan 5G vs Dizi Wi-Fi)
- **Issues**:
  - On `ruan` (5G): Mobile data drops or no IMS/VoLTE registration.
  - On `dizi` (Wi-Fi only): System log spam from `RIL` trying to connect to non-existent baseband modem.
- **Fix Included in `09_cellular_and_telephony/`**:
  - `android.hardware.telephony.exclude-euicc.xml`: Clean carrier permissions.
  - Dynamic RIL detection: On `dizi`, telephony services gracefully sleep without battery drain or modem error panics.
  - Corrected `CarrierConfigOverlayRuan` APN and 5G SA/NSA band definitions.

---

## Package Directory Tree

```text
Nebula_All_Fixes_Package/
├── GUIDE.md                                    <-- This Guide
├── 01_privacy_and_account_link/
│   └── patch_settings_privacy.py               <-- Settings.apk bytecode patcher
├── 02_adb_persistent_connection/
│   ├── adb-tcp-persistent.zip                  <-- Flashable recovery zip (Port 5555 auto-boot)
│   └── setup_ssh_bridge.sh                     <-- Auto-reconnecting SSH bridge
├── 03_dt2w_touchscreen_fix/
│   └── dt2w-fix-OPD2404/                       <-- KernelSU module for DT2W & NT36532 TP
├── 04_cryptoeng_hal_service/
│   └── cryptoeng_source/                       <-- AIDL source & Rust daemon for ICryptoeng
├── 05_recovery_and_flashing_tools/
│   ├── adb-connect.sh                          <-- Automated ADB Wi-Fi connector
│   ├── collect-logs.sh                         <-- One-click dmesg & recovery log puller
│   ├── device-info.sh                          <-- Hardware & partition inspector
│   ├── recovery-diag.sh                        <-- Diagnostic script for OrangeFox
│   ├── recovery-snapshot.sh                    <-- Recovery state snapshot tool
│   ├── root-usb.sh                             <-- USB permissions script
│   └── usb-adb-connect.py                      <-- Raw USB packet connector for Android
├── 06_device_connect_and_synergy/              <-- OPlus Synergy & Cross-device link config
├── 07_audio_calls_and_mic_fix/
│   └── audio_configs/                          <-- Mixer paths, PAL audio & call mic XMLs
├── 08_wifi_bluetooth_connectivity/
│   └── wifi_configs/                           <-- QCA6750 MIMO configs, SAR & WCNSS_qcom_cfg.ini
└── 09_cellular_and_telephony/                  <-- Telephony permissions & RIL configuration
```
