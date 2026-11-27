# Exhaustive Bug Tracker & Root Cause Analysis

This document details every major bug encountered during the porting and bring-up process, its underlying technical root cause, and the permanent fix.

---

### Bug 1: Reboot Loop at POCO Boot Logo (`PS_HOLD` Reset)
* **Symptom**: Tablet powers on, displays the POCO logo for 5–10 seconds, then immediately reboots or shuts down. PMIC log: `Reset Trigger: PS_HOLD, Reset Type: HARD_RESET`.
* **Root Cause**:
  1. *Permission Lockout*: Staged donor APKs and `SystemUI.apk` had mode `0600`/`0660`. Android's `PackageManagerService` threw `SecurityException: Permission denied` during SystemServer initialization.
  2. *LpMetadata Extent Shift*: Custom repacker packed partition extents without the 4-byte `attributes` uint32, mapping `system_a` into Extent 1 (`system_ext_a`) and leaving `system_a` completely unmapped.
* **Fix**:
  1. Run `tools/fix_permissions.sh` to enforce `0755` dirs and `0644` files owned by `0:0`.
  2. Rebuild `super.img` with `tools/build_clean_super.sh` using official `lpmake --sparse`.

---

### Bug 2: OrangeFox `E: Unable to unmap dynamic partitions`
* **Symptom**: In OrangeFox Recovery, wiping partitions or executing Format Data fails with: `E: Unable to unmap dynamic partitions`.
* **Root Cause**: OrangeFox's device-mapper mapper logic mounted logical partitions with duplicate slot suffixes (e.g. `product_a_a`), which failed the internal `DeleteDevice` call during `Unmap_Super_Devices()`.
* **Fix**: Do not rely on OrangeFox's internal unmap script. Run native formatting directly over root ADB using `tools/format_f2fs_userdata.sh`.

---

### Bug 3: Ext4 Userdata Boot Failure
* **Symptom**: Tablet boots past kernel, enters early userspace init, then immediately reboots into recovery.
* **Root Cause**: Earlier `fastboot -w` created standard `ext4` userdata. ColorOS `fstab.qcom` strictly requires F2FS with metadata encryption:
  `latemount,wait,check,formattable,fileencryption=aes-256-xts:aes-256-cts:v2+inlinecrypt_optimized+wrappedkey_v0,metadata_encryption=aes-256-xts:wrappedkey_v0`.
* **Fix**: Format with native `make_f2fs -f -g android -O encrypt,extra_attr,project_quota,quota,casefold -C utf8 /dev/block/by-name/userdata`.

---

### Bug 4: Fastboot Freezing / Timeout on USB Hubs (`reap_as` Hang)
* **Symptom**: `fastboot devices` lists serial `328358c`, but any real command (`flash`, `getvar`) hangs forever in kernel `reap_as` (ioctl `USBDEVFS_REAPURB`).
* **Root Cause**:
  1. Xiaomi's ABL fastboot stack fails to handle USB split-transaction bulk transfers on USB 2.0 hubs.
  2. USB port flapping on multi-port hubs caused Transaction Translator lockups.
* **Fix**:
  1. Use direct Type-C connection without USB hub, OR
  2. Reboot to `fastbootd` in OrangeFox (**Menu → Reboot → Fastboot**). `fastbootd` uses the Linux kernel USB gadget driver and responds in 0.008 seconds without hangs.

---

### Bug 5: 120Hz Jitter & Touch Idle Pause (OTI Bug)
* **Symptom**: Display stutters at 60Hz or drops frames; after screen-off, touch idle remains paused and locks refresh rate.
* **Root Cause**:
  1. `SmoothTransitionController.disableAdfrOtiPwAndAutoResume` in `SystemUI` sends keyguard pause to `oplus_vrr_service` on sleep and fails to resume properly on LCD panels without AOD.
  2. SurfaceFlinger phase offsets and VRR props were misconfigured for tablet panel timing.
* **Fix**:
  1. Patch `SystemUI.apk` using `tools/patch_systemui_oti.py` (changes `power_off` goto jump to `nop nop`).
  2. Inject `configs/performance.prop` into `system/build.prop`:
     `persist.oplus.display.vrr=0`, `debug.sf.late.app.duration=16666666`, `debug.sf.late.sf.duration=16666666`, `set_idle_timer_ms=1100`.

---

### Bug 6: Privacy Password & Account Linking Greyed Out
* **Symptom**: Settings → Privacy Password options are greyed out or tapping "Link Account" immediately closes the window without linking.
* **Root Cause**:
  1. ColorOS delegates keystore/credentials to an internal TEE HAL: `vendor.oplus.hardware.cryptoeng@1.0`. On non-OPPO hardware, this TEE service is absent.
  2. Settings checks `judgeLinkEntranceType`. If false, account link activities fail silently.
* **Fix**:
  1. Patch Settings using `tools/patch_settings_privacy.py` (`const/4 v0, 1` in `judgeLinkEntranceType`).
  2. Inject `reference/cryptoeng_source/` shim service to emulate `ICryptoeng` TEE responses without needing KernelSU.

---

### Bug 7: Panel Corner Double-Rounding
* **Symptom**: Screen corners have software black rounded corner cutouts that double-round the physical LCD display bezel.
* **Root Cause**: Software default sets `rounded_corner_radius` based on phone curves.
* **Fix**: Install `overlays/RuanCornerOverlay.apk` in `/system/overlay/` to set software `rounded_corner_radius=0px`.

---

### Bug 8: Auto-Unlock 4-Digit PIN Lockout & 6-Digit Discrepancy (Dynamic Unlocker)
* **Symptom**: Setting a 4-digit PIN still shows 6 dots on lockscreen; upon typing the 4 digits, auto-unlock never fires and no Enter/OK button is shown, locking out the user.
* **Root Cause**:
  1. `SystemUI.apk` (`classes3.dex` offset `0x42f1f4`) had a hardcoded bytecode override returning `6` (`12 60 0f 00`) unconditionally in `KeyguardPasswordUtils.getNumericPwdLength()`.
  2. `OplusKeyguardSecurityModel.getDeviceSecurityMode()` selects `SecurityMode.PINSix` whenever `getNumericPwdLength() == 6`, hiding the Enter key.
  3. ColorOS Settings updates `lockscreen.qu.ui.type` inside `/data/system/locksettings.db`, but fails to update `Settings.System.PASSWORD_LENGTH`.
  4. On ported devices without `OplusAuthCheck` daemon IPC, `persist.sys.ls.ui_type=1` throws a `SecurityException: return code: 1002`, failing to determine PIN length natively.
* **Fix**:
  1. Restored `SystemUI.apk`'s `classes3.dex` to 100% stock OEM bytecode:
     - At `0x42f1f4`: `71 10 7c f7 00 00` (`invoke-static SystemClock.elapsedRealtime()`).
     - At `0x4208f4`: `62 00 f1 8f` (`sget-object MultiUserContentHelper.Companion`).
     - Recalculated DEX Adler-32 and SHA-1 headers and APK Central Directory CRC-32.
  2. Added standalone lightweight C helper `sqlite_query` (`tools/sqlite_query.c`) that directly queries SQLite via `dlopen("libsqlite.so")` with zero dependencies.
  3. Embedded **Dynamic Unlocker Daemon** inside `/system_ext/bin/dt2w_service.sh`:
     - Monitors `/data/system/locksettings.db` mtime.
     - When `lockscreen.qu.ui.type == 4`: sets `PASSWORD_LENGTH=4`, `keyguard_quick_unlock=1`, and writes `lockscreen.pin_password_length=4`. Keyguard presents 4 dots and instantly unlocks on the 4th digit.
     - When `lockscreen.qu.ui.type == 6`: sets `PASSWORD_LENGTH=6`, `keyguard_quick_unlock=1`, and writes `lockscreen.pin_password_length=6`. Keyguard presents 6 dots and instantly unlocks on the 6th digit.
     - When an alphanumeric or custom-length PIN is set: sets `keyguard_quick_unlock=0`, restoring the standard numeric/alphanumeric keyboard with the Enter/OK button.
  4. Injected `persist.sys.ls.ui_type=0` into `build.prop` so Keyguard reads `PASSWORD_LENGTH` from the first zygote bootstrap.

