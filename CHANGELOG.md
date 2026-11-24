# Nebula OS Changelog — POCO Pad 5G (`ruan`) & Redmi Pad Pro (`dizi`)

## Build 2.1 (Production Pro Edition — Pure Core Architecture)
- **Dynamic Hardware Device Identity**:
  - Automatically identifies variant at boot via modem/RIL & SKU detection:
    - **SIM Variant (`ruan`)**: Branded systemwide as **POCO Pad 5G** (Model: `24074PCD2I`).
    - **Wi-Fi Only (`dizi`)**: Branded systemwide as **Redmi Pad Pro** (Model: `2405CRPFDL`).
  - Synchronizes `Settings.Global.DEVICE_NAME`, `Settings.System.DEVICE_NAME`, `oplus_customize_device_name`, `persist.sys.device_name`, and `bluetooth.device.default_name` dynamically on boot.
- **Complete Clean-up of About Device Info**:
  - Eliminated all legacy OnePlus (`OPD2404`, `一加平板 Pro`) strings and Chinese branding across all partition `build.prop` files (`my_manifest`, `my_product`, `system`).
  - Corrected Processor specification to **Snapdragon® 7s Gen 2 Mobile Platform** (`SM7435`).
  - Updated Software Version to **NebulaOS 17.0 (Android 15)**.
- **Privacy Engine & App Lock Integration**:
  - Bytecode-patched `Settings.apk` (`classes8.dex`) resolving premature `RESULT_CANCELED` abort in `LinkAccountActivity.onStop()` and `PrivacyBindAccountController`. App Lock, Private Safe, and Privacy Password work out of the box.
  - Native Rust AIDL `cryptoeng-hal-service` running under `system:system` with context `u:object_r:hal_gatekeeper_default_exec:s0` (matching `vendor_file_contexts:1125`).
- **Zero KernelSU & Pure Core System**:
  - Completely purged all `/data/adb` and root dependencies from all helper scripts.
  - Native C binary `dt2w_daemon` (`u:r:hal_touchfeature_xiaomi_default:s0`) injecting NT36532 wakeup events.
  - Native lockscreen DT2S detector and in-call mic `voice_call_bridge` running natively under `u:r:debuglog:s0`.
  - Xiaomi Smart Pen & DiziPen companion app fully integrated into vendor and system_ext partitions.
  - Restored stock bootanimation binary and orientation values.
  - Nebula OS Glassy Updater pre-installed in `/system_ext/priv-app/Updater/`.

## Build 2.0 (Zero KSU Architecture Bring-up)
- Compiled native C ELF binary `dt2w_daemon` running in vendor HAL layer.
- Native DT2S listener monitoring lockscreen gestures.
- Custom cosmic squircle UI OTA updater with frosted glass capsule button.
- Thermal engine screen-on fast charging profiles (33W/67W).
- Permanent 120Hz refresh rate lock and instant PIN Quick Unlock.

## Build 1.0 (Initial Bring-up)
- Initial port of ColorOS 17 / Android 15 from OnePlus Pad 2 (`OPD2404`) to SM7435-AB.
