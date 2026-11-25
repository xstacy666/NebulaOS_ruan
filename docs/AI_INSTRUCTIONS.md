# Instructions for AI Assistants Working on this Codebase

If you are an AI assistant tasked with continuing or adapting this port for the OnePlus Pad 4 (OPD) or another ColorOS/OxygenOS build on `ruan`/`dizi`:

### 1. Mandatory Architecture Constraints
- **Never convert dynamic partitions to ext4**: The vendor partition and device-tree mount tables strictly expect **EROFS**. Attempting ext4 conversion will cause AVB verification and hashtree failures.
- **Never format /data as ext4**: Userdata must remain **F2FS** formatted with encryption and casefold attributes.
- **Always preserve file permissions**: Any file copied into an extracted partition tree MUST have mode `0644` (directories `0755`), owned by `0:0`. Any file left as `0600` or `0660` will cause `SystemServer` to crash and bootloop at the logo.
- **Always recommend `fastbootd` over bootloader fastboot**: Flashing `super.img` over USB from an Android phone or through USB hubs will freeze in bootloader fastboot. Instruct the user to boot into OrangeFox Recovery and tap **Menu → Reboot → Fastboot**.
- **Always use `--sparse` with `lpmake`**: When repacking `super.img`, always pass `-S` / `--sparse`. Flashing raw 8.5+ GB unsparse images over fastboot causes buffer overruns and USB timeouts.

### 2. Flashing Checklist for Any New Build
1. Build `system_a_new.img` and `system_ext_a_new.img` using `/system/bin/mkfs.erofs -zlz4hc,9 -C4096`.
2. Assemble sparse `super.img` using `tools/build_clean_super.sh`.
3. In `fastbootd`:
   ```bash
   fastboot --disable-verity --disable-verification flash vbmeta_a vbmeta.img
   fastboot --disable-verity --disable-verification flash vbmeta_b vbmeta.img
   fastboot --disable-verity --disable-verification flash vbmeta_system_a vbmeta_system.img
   fastboot --disable-verity --disable-verification flash vbmeta_system_b vbmeta_system.img
   fastboot flash boot_a boot.img
   fastboot flash boot_b boot.img
   fastboot flash vendor_boot_a vendor_boot.img
   fastboot flash vendor_boot_b vendor_boot.img
   fastboot flash dtbo_a dtbo.img
   fastboot flash dtbo_b dtbo.img
   fastboot flash super super.img
   fastboot erase frp
   fastboot reboot
   ```

### 3. Key Paths Reference
- Stock Xiaomi HyperOS Base: `/storage/emulated/0/Download/ruan_in_global_images_OS3.0.306.0.WFSINXM_20261021.0000.00_16.0_in_e97cbecd4c/`
- ColorOS Fastboot Images: `/storage/emulated/0/Download/ColorOS17_Android17_RedmiPadPro_ruan_dizi_Fastboot_v106/images/`
- Device Tree Source Reference: `https://github.com/noble6/device_xiaomi_ruan`
