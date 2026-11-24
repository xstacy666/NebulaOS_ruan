# Nebula OS Changelog — Redmi Pad Pro 5G (`ruan`) & POCO Pad (`dizi`)

## Build 2.0 (Latest - Zero KSU Core Architecture)
- **Zero KernelSU Dependency**: Completely removed all `/data/adb` and root dependencies. All features work out of the box on clean flash (`fastboot -w`).
- **Native DT2W Daemon**: Compiled native C ELF binary `dt2w_daemon` running in vendor HAL layer, continuously injecting `EV_SYN(0)/SYN_CONFIG(1)/WAKEUP_ON(5)` into Novatek NT36532 touch controller.
- **Native DT2S Listener**: Native touch listener monitoring lockscreen gestures and triggering screen sleep.
- **Xiaomi Smart Pen Integration**: Native `xiaomi-pen` & `xiaomi-pen-pressure` HALs with `init.pen.rc` property trigger listeners. Pre-installed `DiziPen` companion app.
- **Nebula OS Glassy Updater**: Custom cosmic squircle UI with iOS-style frosted glass capsule button, wired to `xstacy666/NebulaOS_ruan_ota`.
- **Display & Audio Tuning**: Permanent 120Hz refresh rate lock, instant PIN Quick Unlock, Qualcomm WCNSS Wi-Fi 6 optimizations, and `voice_call_bridge` in-call mic routing.
- **Privacy Engine**: Embedded `cryptoeng-hal-service` AIDL implementation and patched `Settings.apk` lifecycle abort in `LinkAccountActivity`.
- **Boot Animation**: Restored stock bootanimation binary and orientation values.

## Build 1.0 (Initial Port)
- Initial port of ColorOS 17 / Android 15 from OnePlus Pad 2 (`OPD2404`) to Redmi Pad Pro 5G (`ruan`) and POCO Pad Wi-Fi (`dizi`).
- Basic display, touch, audio, camera, and sensors bring-up.
