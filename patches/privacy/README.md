# Privacy Features & App Lock Fix — Nebula OS

## Problem Analysis
On ColorOS 17 ported from OnePlus Pad 2 (`OPD2404`) to Redmi Pad Pro 5G (`ruan`) and POCO Pad (`dizi`), two critical issues broke Privacy Password, App Lock, and Private Safe:

1. **Premature Activity Abort in `Settings.apk` (`classes8.dex`)**:
   - When entering "Password & Security -> Privacy Password", ColorOS launches `LinkAccountActivity` prompting the user to bind a HeyTap account.
   - In stock `LinkAccountActivity.onStop()`, if the activity loses focus or the user dismisses the dialog, it invoked `finish()` with `RESULT_CANCELED`.
   - The parent caller `ChooseAbstractPrivacy.onActivityResult()` checked `resultCode == RESULT_OK`. Receiving `RESULT_CANCELED`, it immediately called `finish()` without committing the user's entered PIN/password into Android Keystore/Gatekeeper.
   - Furthermore, `PrivacyBindAccountController` prematurely exited in `handlePreferenceTreeClick`.

2. **Proprietary Hardware Crypto Engine (`cryptoeng`) Dependency**:
   - ColorOS delegates App Lock file vault and Private Safe cryptographic key derivation to `vendor.oplus.hardware.cryptoeng.ICryptoeng`.
   - On non-Oppo/OnePlus Qualcomm hardware, the SoC lacks OnePlus's proprietary Trustlet Application (TA) in Qualcomm QSEE / TrustZone.
   - Consequently, the vendor HAL crashed or failed to bind, leaving the App Lock daemon without a valid crypto backend.

---

## Solutions Implemented

### 1. Bytecode Patches (`classes8.dex`)
- **Pattern 1 (`LinkAccountActivity.onStop()`)**:
  Replaces `invoke-virtual isChangingConfigurations` / abort path with an immediate `return-void` (`0e 00 00 00 00 00`), preventing premature cancellation so `ChooseAbstractPrivacy` successfully completes password creation.
- **Pattern 2 (`PrivacyBindAccountController`)**:
  Replaces conditional branch `if-eqz v5, +000d` (`38 05 0d 00`) with unconditional jump `goto +000d` (`28 0d 00 00`), allowing smooth progression past account link prompts without getting stuck.

### 2. Native CryptoEng AIDL Service
- Implemented a clean-room Rust AIDL service [`packages/cryptoeng`](../../packages/cryptoeng) registering `vendor.oplus.hardware.cryptoeng.ICryptoeng/default`.
- Hardware crypto operations are backed by Android Keystore standard AES-GCM and HMAC-SHA256.
- The service binary is installed to `/vendor/bin/hw/cryptoeng-hal-service` with context `u:object_r:hal_gatekeeper_default_exec:s0` (matching SELinux policy `vendor_file_contexts:1125`) and runs natively under `init` with zero root/KSU dependencies.

---

## Applying the Patch
Run the included python script to patch any stock `Settings.apk`:
```bash
python3 patch_settings_privacy.py /path/to/stock/Settings.apk /path/to/patched/Settings.apk
```
