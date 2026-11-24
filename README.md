# Nebula OS (ColorOS 17) for Redmi Pad Pro 5G & POCO Pad

<p align="center">
  <img src="packages/Updater/res/drawable-nodpi-v4/nebula_logo.jpg" width="180" height="180" alt="Nebula OS Logo" style="border-radius: 36px;"/>
  <br/>
  <b>Powered by ColorOS 17 (Android 17)</b>
  <br/>
  <i>Crafted for Redmi Pad Pro 5G (<code>ruan</code>) & Redmi Pad Pro / POCO Pad Wi-Fi (<code>dizi</code>)</i>
</p>

---

## 🌟 Maintainers & Credits

- **Lead Maintainer**: [Øpium | Team Nebula](https://github.com/xstacy666) (`xstacy666`)
- **Core Contributors & Testing**: 
  - `@Noble_actual117`
  - `@GAURAV_140110`
  - `@NotYoursAditya`
  - `@Gignigga`
  - `Team Nebula`

---

## 📱 Device Specifications

| Attribute | Specification |
|:---|:---|
| **SoC** | Qualcomm Snapdragon 7s Gen 2 (`SM7435-AB`) |
| **GPU** | Adreno 710 |
| **Display** | 12.1" 2.5K LCD (1600 x 2560), 120Hz Refresh Rate |
| **Touch IC** | Novatek NT36532 (`NVTCapacitiveTouchScreen`) |
| **Stylus** | Xiaomi Smart Pen (Novatek Stylus / `NVTCapacitivePen` + `mipp_pen0`) |
| **Partitions** | Dynamic Partitions (Virtual A/B, EROFS super image) |

---

## 🛠 Complete Fixes & Port Architecture

Every single feature in this port has been engineered directly into the **read-only core system partitions** (`system`, `system_ext`, `vendor`), guaranteeing **zero dependency on KernelSU, Magisk, or `/data/adb` modules**. Clean flashing (`fastboot -w`) preserves all functionality out-of-the-box.

### 1. 📶 Wi-Fi 6 & Bluetooth Stack
- **Vendor Configurations**: Full Qualcomm WCNSS overlay configs and firmware links in `configs/wifi/`.
- **Bluetooth Stack**: Bluetooth HCI and SSR logging configs, audio routing for SBC, AAC, aptX, LDAC, and LHDC.

### 2. 📞 Voice Calls & Microphone Bridge
- **`voice_call_bridge`**: Native low-level daemon intercepting voice call state changes via `sys.oplus.call_exist`.
- **Routing**: Automatically reconfigures Qualcomm audio paths and mixer controls between cellular baseband and microphone/earpiece/speakers.
- Source code in [`packages/voice_call_bridge/`](packages/voice_call_bridge/).

### 3. 👆 Double Tap to Wake (DT2W) & Double Tap to Sleep (DT2S)
- **Native C Daemon (`dt2w_daemon`)**: Novatek NT36532 driver ignores standard ioctl; `dt2w_daemon` directly injects `EV_SYN(0) / SYN_CONFIG(1) / WAKEUP_ON(5)` into `/dev/input/eventX` and re-arms periodically.
- **DT2S Listener (`dt2s.sh`)**: Background touch event monitor tracking double taps on the lockscreen and dispatches sleep events.
- **Service Integration**: Defined in `dt2w.rc` (vendor) and `init.dt2w.rc` (system_ext) with clean root domain labels.
- Source code in [`packages/dt2w/`](packages/dt2w/).

### 4. 🖊 Xiaomi Smart Pen & Pressure HAL
- **Native HAL Binaries**: `xiaomi-pen` and `xiaomi-pen-pressure` handling pen connection modes and pressure translation to `/dev/uinput`.
- **Init Triggers**: `init.pen.rc` listens for `vendor.pen.state=connected|disconnected|reset` dispatched by the framework.
- **DiziPen Privapp**: Companion system service (`org.lineageos.dizipen`) managing Bluetooth pen handshakes.
- Source code in [`packages/pen/`](packages/pen/).

### 5. ⚡ Screen-On Fast Charging & Thermals
- **Unthrottled Profiles**: Custom `thermal-*.conf` files eliminating aggressive ColorOS tablet thermal throttling.
- **Full Speed Charging**: Uncapped fast charging while the display is active and during heavy gaming.
- Configs in [`configs/thermal/`](configs/thermal/).

### 6. 🔒 Privacy Engine & App Lock (`cryptoeng`)
- **CryptoEng AIDL**: Complete native AIDL implementation (`vendor.oplus.hardware.cryptoeng.ICryptoeng`) written in Rust, satisfying ColorOS privacy and keystore requirements.
- **Settings Lifecycle Fix**: Resolved premature activity termination in `LinkAccountActivity` and `PrivacyBindAccountController`, enabling full HeyTap Account linking, App Lock, and Private Safe.
- Source code in [`packages/cryptoeng/`](packages/cryptoeng/).

### 7. 🚀 Display & Performance Tuning
- **120Hz Lock**: Enforced 120Hz refresh rate (`min_refresh_rate` and `peak_refresh_rate` = 120.0).
- **Quick Unlock**: Zero-delay lockscreen PIN unlock (no Enter / OK key required).
- **Stock Boot Animation**: Restored pristine stock boot animation with correct orientation.

### 8. 🔄 Nebula OS Glassy OTA Updater
- **System Privapp**: Pre-installed in `/system_ext/priv-app/Updater/`.
- **Modern UI**: Cosmic squircle Nebula logo, frosted glass capsule pill button, fluid animations.
- **OTA Infrastructure**: Connected directly to the live GitHub releases feed [`xstacy666/NebulaOS_ruan_ota`](https://github.com/xstacy666/NebulaOS_ruan_ota) with direct Google Drive cloud package downloads.
- Source code in [`packages/Updater/`](packages/Updater/).

---

## 💻 Flashing Instructions

1. Download the latest Fastboot ROM package: `ColorOS17_RedmiPadPro_ruan_dizi_Fastboot.7z`.
2. Extract the archive completely.
3. Reboot your tablet to Fastboot mode:
   ```bash
   adb reboot bootloader
   ```
4. Run the flashing script:
   - **Windows**: Double-click `flash.bat`
   - **Linux / macOS**:
     ```bash
     chmod +x flash.sh
     ./flash.sh
     ```
5. The script flashes all partitions, wipes userdata for a clean boot, and reboots into Nebula OS.

---

## 📄 License & Attribution

- Portions copyright (C) The Android Open Source Project.
- Portions copyright (C) LineageOS Project.
- Portions copyright (C) OnePlus / OPPO Mobile Telecommunications Corp., Ltd.
- Nebula OS modifications and fixes maintained by **Team Nebula** (`xstacy666`).
