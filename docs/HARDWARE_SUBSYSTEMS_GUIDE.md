# Complete Hardware Subsystems & Fixes Guide: ruan/dizi (SM7435)

This document provides exhaustive documentation on how every single hardware subsystem on the Redmi Pad Pro / POCO Pad (`ruan` [5G] and `dizi` [Wi-Fi]) is configured, integrated, and patched for the OnePlus Pad (OPD) / ColorOS 17 ROM port.

---

## 1. Audio, Microphones & Voice Calls Subsystem

### Quad-Speaker & Dual-Microphone Architecture
* **SoC Audio Platform**: Qualcomm SM7435 ("parrot") with integrated WCD937x / Qualcomm audio codec.
* **Speaker Layout**: 4 symmetrical stereo speakers (2 Left, 2 Right in landscape mode) with Dolby Atmos spatial sound.
* **Microphone Layout**: 2 digital microphones (top and frame edge) for beamforming noise cancellation during voice calls and video recordings.

### Voice Call & Mic Fix (Resolving "No Sound / No Mic in Calls")
* **Root Cause on Port**: On custom ROM ports, telephony audio routing failed because the RIL/telecom daemon tried to route voice stream through earpiece (`AUDIO_DEVICE_OUT_EARPIECE`), which tablets do not physically possess.
* **The Fix**:
  1. Set Voice Call volume steps:
     ```properties
     ro.config.vc_call_vol_steps=11
     ```
  2. Telephony routing overrides: In `audio_policy_configuration.xml` and `vendor.prop`, reroute `AUDIO_DEVICE_OUT_TELEPHONY_TX` and `AUDIO_STREAM_VOICE_CALL` directly to Speaker (`AUDIO_DEVICE_OUT_SPEAKER`) and digital microphone (`AUDIO_DEVICE_IN_BUILTIN_MIC`).
  3. Ensure `audio.primary.parrot.so` and `libaudioroute.so` are mounted from `/vendor/lib64/hw/`.
  4. Mixer Paths: Located at `device_tree_ruan/configs/audio/mixer_paths_parrot.xml`.

---

## 2. Wi-Fi & Hotspot Subsystem (Qualcomm WCN3998 / WCN6750)

* **Architecture**: Wi-Fi 6 (802.11ax) dual-band (2.4 GHz & 5 GHz) 2x2 MIMO.
* **Firmware Mount**: Mounted from dedicated UFS partition `/dev/block/bootdevice/by-name/modem` into `/vendor/firmware_mnt`.
* **Configuration Files**:
  - `WCNSS_qcom_cfg.ini`: Located in `device_tree_ruan/configs/wifi/`. Configures regulatory domain (FCC/ETSI/MII), bandwidth (80 MHz channels), and power save modes.
  - `p2p_supplicant_overlay.conf` & `wpa_supplicant_overlay.conf`: Wi-Fi Direct and WPA3-Personal authentication parameters.
* **Critical Rule**: Never delete or unmount `/vendor/firmware_mnt`. If the modem partition is unmounted, the WLAN MAC address returns `02:00:00:00:00:00` and Wi-Fi cannot turn on.

---

## 3. Bluetooth Subsystem & Audio Codecs

* **Architecture**: Bluetooth 5.2 with support for SBC, AAC, aptX, aptX HD, aptX Adaptive, and LDAC.
* **Firmware Mount**: `/dev/block/bootdevice/by-name/bluetooth` mounted to `/vendor/bt_firmware`.
* **Config File**: `device_tree_ruan/configs/bluetooth/bt_vendor.conf`.
  - UART transport speed: `3000000` bps.
  - Firmware path: `/vendor/bt_firmware/image/`.
* **Properties**:
  ```properties
  vendor.qcom.bluetooth.soc=cherokee
  persist.vendor.bt.a2dp.aac_whitelist=false
  persist.vendor.qcom.bluetooth.enable.splita2dp=true
  ```

---

## 4. Touchscreen, Stylus & Double-Tap-to-Wake (DT2W)

### Active Stylus / Pen Support
* The touch IC supports both the official **Xiaomi Smart Pen (2nd Gen)** and active capacitive pens (e.g., **Lenovo Precision Pen**).
* To keep active pens working without Bluetooth pairing:
  `pipa-service.sh` / `device_tree_ruan/pen/` keeps pen touch scanning armed in the touch controller (`sec_touchscreen` driver node).

### Double-Tap-to-Wake (DT2W) Persistent Fix
* In `modules_and_services/dt2w-fix-OPD2404/`:
  The post-fs script writes `1` to the touch panel kernel wake node:
  ```bash
  for node in /proc/touchpanel/double_tap_enable /sys/touchpanel/double_tap_enable /sys/devices/platform/soc/*.i2c/i2c-*/double_tap; do
      [ -f "$node" ] && echo 1 > "$node"
  done
  ```

---

## 5. Battery, Thermal & Fast Charging (33W / 67W)

* **Architecture**: Dual-cell battery topology (10,000 mAh total).
* **PMIC / Charger IC**: Qualcomm PM6150 / SMB1396 fast charge pump.
* **Charger Firmware Fstab Bug Fix**:
  Xiaomi's `init.target.rc` looks for `/vendor/etc/charger_fw_fstab.qti` during off-mode and fast charging. If missing, battery readings display "?" and the tablet powers off. Ensure this fstab is installed in `/vendor/etc/`.
* **Fast Charging Protocol**: Standard USB-PD 3.0 (PPS) and Xiaomi Turbo Charge.

---

## 6. Live Capsule / Dynamic Island Feature

* Staged donor APK: `com.oplus.pantanal.ums` (Universal Media Service) and `com.oplus.linker` (OPSynergy).
* Enables ColorOS / OxygenOS dynamic island notifications in the status bar for timers, media playback, food delivery, and charging status.

---

## 7. Camera HAL & Torch Strength Control

* **Camera Hardware**: 8 MP Rear, 8 MP Front.
* **Torch Control Patch**: On `ruan`/`dizi`, the torch current can be adjusted in 12 levels of 20 mA (from 120 mA stock up to 240 mA boost) by writing directly to `/sys/class/leds/led:torch_0/brightness` and `/sys/class/leds/led:torch_1/brightness`.
