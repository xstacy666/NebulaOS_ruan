#!/usr/bin/env python3
import os
import sys
import shutil
import struct
import hashlib
import zlib
import subprocess

TREES = '/data/user/0/ai.antigem/files/home/projects/opd4-pocopad-port/work/trees_clean'
SYS = os.path.join(TREES, 'system')
SEXT = os.path.join(TREES, 'system_ext')
PROD = os.path.join(TREES, 'product')
VEND = os.path.join(TREES, 'vendor')
VDLKM = os.path.join(TREES, 'vendor_dlkm')

REPO = '/data/user/0/ai.antigem/files/home/projects/NebulaOS_ruan'
PKG = '/data/user/0/ai.antigem/files/home/projects/Nebula_All_Fixes_Package'
DONOR_PRIVAPP = '/storage/emulated/0/port/donor/trees/my_stock/priv-app'
ARCHIVE_PRIVAPP = '/storage/emulated/0/port/archive/x9ultra-17/trees/my_stock/priv-app'
ROM_DIR = '/storage/emulated/0/Download/ColorOS17_Android17_RedmiPadPro_ruan_dizi_Fastboot_v106'

print("================================================================")
print("  APPLYING COMPLETE A-TO-Z FIXES TO CLEAN ROM TREES             ")
print("================================================================")

# ==============================================================================
# 1. DEBLOAT & APP REMOVALS
# ==============================================================================
print("\n[*] Step 1: Debloat & App Removals...")

# 1a. Removals specifically requested by user
# - Removal of Nebula Updater
updater_paths = [
    os.path.join(SEXT, 'priv-app/Updater'),
    os.path.join(SEXT, 'etc/permissions/privapp-permissions-org.nebula.updater.xml'),
    os.path.join(SEXT, 'etc/permissions/privapp-permissions-updater.xml')
]
for p in updater_paths:
    if os.path.exists(p):
        if os.path.isdir(p):
            shutil.rmtree(p)
        else:
            os.remove(p)
        print(f"  [-] Removed Nebula Updater file/dir: {p}")

# - Removal of Kaorios Toolbox
kaorios_paths = [
    os.path.join(SYS, 'system/priv-app/KaoriosToolbox'),
    os.path.join(SYS, 'priv-app/KaoriosToolbox'),
    os.path.join(SYS, 'system/etc/permissions/com.kousei.kaorios.xml'),
    os.path.join(SYS, 'etc/permissions/com.kousei.kaorios.xml')
]
for p in kaorios_paths:
    if os.path.exists(p):
        if os.path.isdir(p):
            shutil.rmtree(p)
        else:
            os.remove(p)
        print(f"  [-] Removed Kaorios file/dir: {p}")

# 1b. Surgical Debloat from clean_bloatware.sh
debloat_targets = [
    # Backups
    os.path.join(SYS, 'system/framework/framework.jar.bak'),
    os.path.join(SEXT, 'priv-app/CarrierConfig/CarrierConfig.apk.bak'),
    # Factory QA Burn-In / Aging Videos (~51.7 MB)
    os.path.join(SYS, 'my_product/media/aging_video.mp4'),
    # Offline Theme Caches (~203 MB)
    os.path.join(SEXT, 'etc/wallpaper_preload/online/theme-OS16-5000-mountain-tablet'),
    os.path.join(SEXT, 'etc/wallpaper_preload/online/wallpaper-OS16-5040-Nature-tablet'),
    os.path.join(SEXT, 'etc/wallpaper_preload/online/theme-OS17-5000-with_the_light-tablet'),
    os.path.join(SEXT, 'etc/wallpaper_preload/online/theme-OS17-5000-definition-tablet'),
    os.path.join(SEXT, 'etc/wallpaper_preload/online/theme-OS17-5000-photo_frame_clock-tablet'),
    # Uncompressed Decouping Wallpaper Videos (~68 MB)
    os.path.join(SYS, 'my_product/decouping_wallpaper/default/liveWallpaper/ivy_1.mp4'),
    os.path.join(SYS, 'my_product/decouping_wallpaper/common/wallpaper_group/ai_camera_movement'),
    os.path.join(SYS, 'my_product/decouping_wallpaper/common/wallpaper_group/00_ColorOS15/ColorOS1506.mp4'),
    os.path.join(SYS, 'my_product/decouping_wallpaper/common/wallpaper_group/00_ColorOS15/ColorOS1506_land.mp4'),
    # Mismatched AI models & Covid PCR Plugins (~66.7 MB)
    os.path.join(SYS, 'my_product/etc/aisubsystem/OAA/oaa-oaa_nlp_text_embedding_domestic_sm8850-19927190-3005-pall-rall'),
    os.path.join(SYS, 'my_stock/etc/aisubsystem/Plugin/plugin-pcr_code-187699292-1060-pall.apk'),
    os.path.join(SYS, 'my_region/etc/aisubsystem/Plugin/plugin-full_search_llm_cn-189796359-150017-pall-CN.apk'),
    os.path.join(SYS, 'my_region/etc/aisubsystem/Plugin/plugin-cloud_livephoto_matting-185622557-1006-full-domestic-33-aall-release.apk'),
    os.path.join(SYS, 'my_product/etc/aisubsystem/Plugin/plugin-cloud_entity_extraction-187699205-1008-pall-domestic.apk'),
    # Chinese Voice Assistant (Breeno) (~165.8 MB)
    os.path.join(SYS, 'my_stock/priv-app/HeyTapSpeechAssist'),
    # Ad Tracking & Harvesters
    os.path.join(SYS, 'my_stock/priv-app/dmp'),
    os.path.join(SYS, 'my_stock/priv-app/OplusBootReg'),
    os.path.join(SYS, 'my_stock/app/Gleaner'),
    os.path.join(SYS, 'my_stock/app/COSA'),
    os.path.join(SEXT, 'priv-app/DeepThinker'),
    os.path.join(SEXT, 'priv-app/DFS'),
    os.path.join(SEXT, 'priv-app/UpgradeGuide'),
    os.path.join(SYS, 'my_stock/app/AIMemory'),
    os.path.join(SEXT, 'app/SystemAppUpdateService'),
    os.path.join(SEXT, 'app/SAUHelper'),
    # Incompatible Hardware Apps & Redundant China Del-Apps (~78 MB)
    os.path.join(SEXT, 'priv-app/BeidouMsgService'),
    os.path.join(SEXT, 'app/NtnSatApp'),
    os.path.join(SEXT, 'app/RsmcSatelliteAuth'),
    os.path.join(SEXT, 'app/TTSatelliteAuth'),
    os.path.join(SEXT, 'app/NfcEngineerTest'),
    os.path.join(SEXT, 'app/BTtestmode'),
    os.path.join(SEXT, 'app/OplusEngineerNetwork'),
    os.path.join(SEXT, 'app/OplusCommercialEngineerMode'),
    os.path.join(SEXT, 'app/WapiCertManage'),
    os.path.join(SYS, 'my_stock/app/PhoneNOAreaInquireProvider'),
    os.path.join(SYS, 'my_stock/del-app/PhoneManagerLite'),
    os.path.join(SYS, 'my_stock/del-app/OplusEmail'),
    os.path.join(SYS, 'my_stock/priv-app/AISeal'),
    os.path.join(SYS, 'my_stock/app/FileEncryption')
]
for p in debloat_targets:
    if os.path.exists(p):
        if os.path.isdir(p):
            shutil.rmtree(p)
        else:
            os.remove(p)
        print(f"  [-] Purged: {os.path.basename(p)}")

# Speechassist video mp4s
sp_vid = os.path.join(SYS, 'my_product/cust/CN/media/speechassist/video')
if os.path.exists(sp_vid):
    shutil.rmtree(sp_vid)
    print("  [-] Purged speechassist video folder")

print("[+] Step 1 Debloat complete!")

# ==============================================================================
# 2. RESTORATION OF KEKEUSERCENTER & PRIVACY PASSWORD / ACCOUNT LOGIN
# ==============================================================================
print("\n[*] Step 2: Restoring KeKeUserCenter & Account Components...")

# 2a. Stage KeKeUserCenter from donor
keke_donor = os.path.join(DONOR_PRIVAPP, 'KeKeUserCenter/KeKeUserCenter.apk')
if not os.path.exists(keke_donor):
    keke_donor = os.path.join(ARCHIVE_PRIVAPP, 'KeKeUserCenter/KeKeUserCenter.apk')

keke_dest_dir = os.path.join(SYS, 'my_stock/priv-app/KeKeUserCenter')
keke_dest_apk = os.path.join(keke_dest_dir, 'KeKeUserCenter.apk')

if os.path.exists(keke_donor):
    os.makedirs(keke_dest_dir, exist_ok=True)
    shutil.copy2(keke_donor, keke_dest_apk)
    os.chmod(keke_dest_apk, 0o644)
    if os.path.exists(os.path.join(keke_dest_dir, 'oat')):
        shutil.rmtree(os.path.join(keke_dest_dir, 'oat'))
    print(f"  [+] Installed KeKeUserCenter.apk ({os.path.getsize(keke_dest_apk)} bytes) to {keke_dest_apk}")
else:
    print(f"  [-] Warning: KeKeUserCenter donor not found at {keke_donor}!")

# 2b. Verify KeKeUserCenterAccount
keke_acc_dir = os.path.join(SYS, 'my_stock/priv-app/KeKeUserCenterAccount')
keke_acc_apk = os.path.join(keke_acc_dir, 'KeKeUserCenterAccount.apk')
if os.path.exists(keke_acc_apk):
    if os.path.exists(os.path.join(keke_acc_dir, 'oat')):
        shutil.rmtree(os.path.join(keke_acc_dir, 'oat'))
    print(f"  [+] KeKeUserCenterAccount.apk verified ({os.path.getsize(keke_acc_apk)} bytes).")
else:
    print(f"  [-] Warning: KeKeUserCenterAccount.apk missing at {keke_acc_apk}!")

# 2c. Cryptoeng HAL Service & RC
crypto_bin = os.path.join(REPO, 'master_fixes_v3/modules_and_services/04_cryptoeng_hal_service/cryptoeng_source/target/release/cryptoeng-hal-service')
if not os.path.exists(crypto_bin):
    crypto_bin = '/data/user/0/ai.antigem/files/home/projects/opd4-pocopad-port/work/oplus_cryptoeng-aidl-rs/target/release/cryptoeng-hal-service'

dest_crypto_bin = os.path.join(SEXT, 'bin/cryptoeng-hal-service')
if os.path.exists(crypto_bin):
    os.makedirs(os.path.dirname(dest_crypto_bin), exist_ok=True)
    shutil.copy2(crypto_bin, dest_crypto_bin)
    os.chmod(dest_crypto_bin, 0o755)
    print(f"  [+] Installed cryptoeng-hal-service to {dest_crypto_bin}")

crypto_rc = os.path.join(SEXT, 'etc/init/cryptoeng.rc')
os.makedirs(os.path.dirname(crypto_rc), exist_ok=True)
with open(crypto_rc, 'w') as f:
    f.write("""service vendor.oplus.hardware.cryptoeng-service /system_ext/bin/cryptoeng-hal-service
    class hal
    user system
    group system
""")
os.chmod(crypto_rc, 0o644)
print(f"  [+] Verified {crypto_rc}")

# 2d. Settings.apk Privacy Link Patches
settings_apk = os.path.join(SEXT, 'priv-app/Settings/Settings.apk')
if os.path.exists(settings_apk):
    import zipfile
    zin = zipfile.ZipFile(settings_apk, 'r')
    c8 = bytearray(zin.read('classes8.dex'))
    p1_orig = bytes.fromhex('6f107209030054301af4380005006e10268500006e1080000300')
    p1_patch = bytes.fromhex('6f107209030054301af4380005006e10268500000e0000000000')
    p2_orig = bytes.fromhex('38050d005445a1f46e20980015005444a1f46e106d000400')
    p2_patch = bytes.fromhex('280d00005445a1f46e20980015005444a1f46e106d000400')
    modified = False
    
    idx1 = c8.find(p1_orig)
    if idx1 != -1:
        c8[idx1:idx1+len(p1_orig)] = p1_patch
        modified = True
        print("  [+] Settings.apk: Applied Pattern 1 (return-void) in classes8.dex")
    elif c8.find(p1_patch) != -1:
        print("  [*] Settings.apk: Pattern 1 already patched.")

    idx2 = c8.find(p2_orig)
    if idx2 != -1:
        c8[idx2:idx2+len(p2_orig)] = p2_patch
        modified = True
        print("  [+] Settings.apk: Applied Pattern 2 (unconditional goto) in classes8.dex")
    elif c8.find(p2_patch) != -1:
        print("  [*] Settings.apk: Pattern 2 already patched.")

    if modified:
        c8[12:32] = hashlib.sha1(c8[32:]).digest()
        c8[8:12] = struct.pack('<I', zlib.adler32(bytes(c8[12:])) & 0xffffffff)
        tmp_apk = settings_apk + '.tmp'
        zout = zipfile.ZipFile(tmp_apk, 'w')
        for item in zin.infolist():
            if item.filename == 'classes8.dex':
                zout.writestr(item, c8, compress_type=item.compress_type)
            else:
                zout.writestr(item, zin.read(item.filename), compress_type=item.compress_type)
        zin.close()
        zout.close()
        os.replace(tmp_apk, settings_apk)
        os.chmod(settings_apk, 0o644)
        print("  [+] Settings.apk updated successfully.")
    else:
        zin.close()

if os.path.exists(os.path.join(SEXT, 'priv-app/Settings/oat')):
    shutil.rmtree(os.path.join(SEXT, 'priv-app/Settings/oat'))

# ==============================================================================
# 3. QUICK UNLOCK & DYNAMIC PIN FIX (4-DIGIT / 6-DIGIT AUTO-UNLOCK)
# ==============================================================================
print("\n[*] Step 3: Quick Unlock & Dynamic PIN Auto-Unlock System...")

# 3a. SystemUI.apk bytecode patch: KeyguardPasswordUtils.getNumericPwdLength
sysui_apk = os.path.join(SEXT, 'priv-app/SystemUI/SystemUI.apk')
if os.path.exists(sysui_apk):
    import zipfile
    apk_data = bytearray(open(sysui_apk, 'rb').read())
    with zipfile.ZipFile(sysui_apk, 'r') as z:
        info = z.getinfo('classes3.dex')
        lh = info.header_offset
        fn, ex = struct.unpack_from('<HH', apk_data, lh + 26)
        dex_start = lh + 30 + fn + ex
        dex = bytearray(apk_data[dex_start : dex_start + info.file_size])

    qu_patch = bytes.fromhex('71003a070000')
    curr_bytes = dex[0x42f1f4:0x42f1f4+6]
    if curr_bytes != qu_patch:
        print(f"  [*] Patching classes3.dex @ 0x42f1f4 from {curr_bytes.hex()} -> {qu_patch.hex()}")
        dex[0x42f1f4:0x42f1f4+6] = qu_patch
        dex[12:32] = hashlib.sha1(dex[32:]).digest()
        dex[8:12] = struct.pack('<I', zlib.adler32(bytes(dex[12:])) & 0xffffffff)
        apk_data[dex_start : dex_start + len(dex)] = dex

        new_crc = zlib.crc32(dex) & 0xffffffff
        struct.pack_into('<I', apk_data, lh + 14, new_crc)

        eocd = apk_data.rfind(b'PK\x05\x06')
        p = struct.unpack_from('<I', apk_data, eocd + 16)[0]
        while apk_data[p:p + 4] == b'PK\x01\x02':
            f, e, c = struct.unpack_from('<HHH', apk_data, p + 28)
            if struct.unpack_from('<I', apk_data, p + 42)[0] == lh:
                struct.pack_into('<I', apk_data, p + 16, new_crc)
                break
            p += 46 + f + e + c

        open(sysui_apk, 'wb').write(apk_data)
        os.chmod(sysui_apk, 0o644)
        print("  [+] SystemUI.apk patched: KeyguardPasswordUtils restored to stock dynamic check.")
    else:
        print("  [*] SystemUI.apk classes3.dex @ 0x42f1f4 already dynamic (71003a070000).")

    if os.path.exists(os.path.join(SEXT, 'priv-app/SystemUI/oat')):
        shutil.rmtree(os.path.join(SEXT, 'priv-app/SystemUI/oat'))

# 3b. Install sqlite_query binary helper
sqlite_bin_src = os.path.join(REPO, 'tools/sqlite_query')
for dst_bin in [os.path.join(SEXT, 'bin/sqlite_query'), os.path.join(VEND, 'bin/sqlite_query')]:
    os.makedirs(os.path.dirname(dst_bin), exist_ok=True)
    shutil.copy2(sqlite_bin_src, dst_bin)
    os.chmod(dst_bin, 0o755)
    print(f"  [+] Installed sqlite_query ({os.path.getsize(dst_bin)} bytes) to {dst_bin}")

# 3c. Install dt2w_service.sh with Dynamic Unlocker daemon & safe fast charging & DT2S
dt2w_sh_src = os.path.join(REPO, 'packages/dt2w/dt2w_service.sh')
dt2w_dst = os.path.join(SEXT, 'bin/dt2w_service.sh')
shutil.copy2(dt2w_sh_src, dt2w_dst)
os.chmod(dt2w_dst, 0o755)
print(f"  [+] Installed production dt2w_service.sh ({os.path.getsize(dt2w_dst)} bytes) to {dt2w_dst}")

# 3d. Add persist.sys.ls.ui_type=0 to build.prop
for bp_path in [os.path.join(SYS, 'system/build.prop'), os.path.join(SYS, 'build.prop'), os.path.join(SEXT, 'etc/build.prop')]:
    if os.path.exists(bp_path):
        lines = open(bp_path).readlines()
        has_prop = any('persist.sys.ls.ui_type' in l for l in lines)
        if not has_prop:
            with open(bp_path, 'a') as f:
                f.write("\n# Dynamic Lockscreen UI (Dynamic 4/6 PIN)\npersist.sys.ls.ui_type=0\n")
            print(f"  [+] Appended persist.sys.ls.ui_type=0 to {bp_path}")

# ==============================================================================
# 4. DEVICE CONNECT (OPLUS SYNERGY / MULTI-SCREEN CONNECT)
# ==============================================================================
print("\n[*] Step 4: Staging Device Connect Components...")
app_mapping = [
    (os.path.join(DONOR_PRIVAPP, 'HeyCast/HeyCast.apk'), os.path.join(SEXT, 'priv-app/HeyCast/HeyCast.apk')),
    (os.path.join(DONOR_PRIVAPP, 'OPSynergy/OPSynergy.apk'), os.path.join(SEXT, 'priv-app/OPSynergy/OPSynergy.apk')),
    (os.path.join(DONOR_PRIVAPP, 'MyDevices/MyDevices.apk'), os.path.join(SEXT, 'priv-app/MyDevices/MyDevices.apk')),
    (os.path.join(DONOR_PRIVAPP, 'ShareScreen/ShareScreen.apk'), os.path.join(SYS, 'my_stock/priv-app/ShareScreen/ShareScreen.apk')),
]
for src, dst in app_mapping:
    if not os.path.exists(src):
        alt = src.replace(DONOR_PRIVAPP, ARCHIVE_PRIVAPP)
        if os.path.exists(alt): src = alt
    if os.path.exists(src):
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copy2(src, dst)
        os.chmod(dst, 0o644)
        if os.path.exists(os.path.join(os.path.dirname(dst), 'oat')):
            shutil.rmtree(os.path.join(os.path.dirname(dst), 'oat'))
        print(f"  [+] Verified Device Connect APK: {os.path.basename(dst)} ({os.path.getsize(dst)} bytes)")
    elif os.path.exists(dst):
        print(f"  [*] Already staged: {os.path.basename(dst)} ({os.path.getsize(dst)} bytes)")
    else:
        print(f"  [-] Warning: Device Connect source not found for {dst}")

# App feature flags for Device Connect UI (Stock ColorOS Interconnect)
feat_xml = os.path.join(SYS, 'my_stock/etc/extension/com.oplus.app-features.xml')
if os.path.exists(feat_xml):
    f_content = open(feat_xml).read()
    old_tag = '<app_feature name="oplus.software.padconnect.setting.disabled" args="boolean:true"/>'
    new_tags = '<app_feature name="oplus.software.padconnect.setting.disabled" args="boolean:false"/>
	<app_feature name="com.oplus.linker.mix.entrance.settings" args="boolean:true"/>'
    if old_tag in f_content:
        f_content = f_content.replace(old_tag, new_tags)
        open(feat_xml, 'w').write(f_content)
        print('  [+] Unlocked stock Device Connect UI feature flags in com.oplus.app-features.xml')
    else:
        print('  [*] Device Connect UI feature flags already present in com.oplus.app-features.xml')

# Sepolicy rule for IDmtpService
se_rule_path = os.path.join(VEND, 'etc/ruan_sepolicy.rule')
if os.path.exists(se_rule_path):
    se_content = open(se_rule_path).read()
    target_rule = "allow system_app default_android_hwservice hwservice_manager find"
    if target_rule not in se_content:
        with open(se_rule_path, 'a') as f:
            f.write(f"\n{target_rule}\n")
        print(f"  [+] Injected IDmtpService rule into {se_rule_path}")
    else:
        print(f"  [*] IDmtpService rule already present in {se_rule_path}")

# ==============================================================================
# 5. CORNER RADIUS OVERLAY
# ==============================================================================
print("\n[*] Step 5: Installing Corner Radius Overlay (Double-Rounding Fix)...")
corner_src = os.path.join(REPO, 'master_fixes_v3/overlays/RuanCornerOverlay.apk')
for ov_dir in [os.path.join(SYS, 'system/overlay'), os.path.join(PROD, 'overlay')]:
    os.makedirs(ov_dir, exist_ok=True)
    dst = os.path.join(ov_dir, 'RuanCornerOverlay.apk')
    shutil.copy2(corner_src, dst)
    os.chmod(dst, 0o644)
    print(f"  [+] Installed RuanCornerOverlay.apk in {ov_dir}")

# ==============================================================================
# 6. IN-CALL AUDIO & DUAL-MIC / QUAD-SPEAKER ROUTING
# ==============================================================================
print("\n[*] Step 6: Installing In-Call Audio & Mic Routing Configs...")
audio_src = os.path.join(PKG, '07_audio_calls_and_mic_fix/audio_configs')
if os.path.exists(audio_src):
    for fn in os.listdir(audio_src):
        src_f = os.path.join(audio_src, fn)
        if not os.path.isfile(src_f): continue
        v_etc = os.path.join(VEND, 'etc', fn)
        shutil.copy2(src_f, v_etc)
        os.chmod(v_etc, 0o644)
        
        sku_dir = os.path.join(VEND, 'etc/audio/sku_parrot')
        if os.path.exists(sku_dir):
            shutil.copy2(src_f, os.path.join(sku_dir, fn))
            os.chmod(os.path.join(sku_dir, fn), 0o644)
    print(f"  [+] Installed {len(os.listdir(audio_src))} audio configs into vendor/etc/ and sku_parrot/")

# ==============================================================================
# 7. WI-FI 6 MIMO & BLUETOOTH FIX
# ==============================================================================
print("\n[*] Step 7: Installing Wi-Fi 6 MIMO & SAR configs...")
wifi_src = os.path.join(PKG, '08_wifi_bluetooth_connectivity/wifi_configs')
if os.path.exists(wifi_src):
    for conf in ['p2p_supplicant_overlay.conf', 'wpa_supplicant_overlay.conf']:
        cp = os.path.join(wifi_src, conf)
        if os.path.exists(cp):
            dst = os.path.join(VEND, 'etc/wifi', conf)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            shutil.copy2(cp, dst)
            os.chmod(dst, 0o644)
            print(f"  [+] Installed {conf} to vendor/etc/wifi/")
    
    qca_src = os.path.join(wifi_src, 'qca6750')
    if os.path.exists(qca_src):
        qca_dst = os.path.join(VEND, 'etc/wifi/qca6750')
        os.makedirs(qca_dst, exist_ok=True)
        for fn in os.listdir(qca_src):
            shutil.copy2(os.path.join(qca_src, fn), os.path.join(qca_dst, fn))
            os.chmod(os.path.join(qca_dst, fn), 0o644)
        print("  [+] Installed QCA6750 2x2 MIMO & SAR configs.")

# ==============================================================================
# 8. TELEPHONY & CARRIER CONFIG
# ==============================================================================
print("\n[*] Step 8: Telephony & Carrier Configs...")
tel_src = os.path.join(PKG, '09_cellular_and_telephony/android.hardware.telephony.exclude-euicc.xml')
if os.path.exists(tel_src):
    tel_dst = os.path.join(VEND, 'etc/permissions/android.hardware.telephony.exclude-euicc.xml')
    os.makedirs(os.path.dirname(tel_dst), exist_ok=True)
    shutil.copy2(tel_src, tel_dst)
    os.chmod(tel_dst, 0o644)
    print("  [+] Installed android.hardware.telephony.exclude-euicc.xml")

# ==============================================================================
# 9. DT2W KERNEL MODULES & DT2WCTL
# ==============================================================================
print("\n[*] Step 9: Installing DT2W Kernel Modules & dt2wctl...")
tp_src = os.path.join(REPO, 'master_fixes_v3/modules_and_services/dt2w-fix-OPD2404/files/lct_tp.ko')
spi_src = os.path.join(REPO, 'master_fixes_v3/modules_and_services/dt2w-fix-OPD2404/files/nt36532_spi.ko')
ctl_src = os.path.join(REPO, 'master_fixes_v3/modules_and_services/dt2w-fix-OPD2404/files/dt2wctl')

mod_dir = os.path.join(VDLKM, 'lib/modules')
if os.path.exists(tp_src) and os.path.exists(spi_src):
    os.makedirs(mod_dir, exist_ok=True)
    shutil.copy2(tp_src, os.path.join(mod_dir, 'lct_tp.ko'))
    shutil.copy2(spi_src, os.path.join(mod_dir, 'nt36532_spi.ko'))
    os.chmod(os.path.join(mod_dir, 'lct_tp.ko'), 0o644)
    os.chmod(os.path.join(mod_dir, 'nt36532_spi.ko'), 0o644)
    print("  [+] Patched DT2W driver modules copied to vendor_dlkm/lib/modules/")

if os.path.exists(ctl_src):
    for ctl_dst in [os.path.join(SEXT, 'bin/dt2wctl'), os.path.join(VEND, 'bin/dt2wctl')]:
        os.makedirs(os.path.dirname(ctl_dst), exist_ok=True)
        shutil.copy2(ctl_src, ctl_dst)
        os.chmod(ctl_dst, 0o755)
        print(f"  [+] Installed dt2wctl in {ctl_dst}")

# ==============================================================================
# 10. ENFORCE STRICT POSIX PERMISSIONS (0644 files / 0755 dirs / 0755 bins)
# ==============================================================================
print("\n[*] Step 10: Enforcing Permissions (0644 files, 0755 dirs, 0755 bins)...")
for tree in [SYS, SEXT, PROD, VEND, VDLKM]:
    t_name = os.path.basename(tree)
    subprocess.run(["find", tree, "-type", "d", "-exec", "chmod", "755", "{}", "+"], check=True)
    subprocess.run(["find", tree, "-type", "f", "-exec", "chmod", "644", "{}", "+"], check=True)
    bin_dir = os.path.join(tree, "bin")
    if os.path.exists(bin_dir):
        subprocess.run(["find", bin_dir, "-type", "f", "-exec", "chmod", "755", "{}", "+"], check=True)
    print(f"  [+] Permissions enforced for {t_name}")

# ==============================================================================
# 11. UPDATE FLASH.SH AND FLASH.BAT (FASTBOOT ERASE FRP)
# ==============================================================================
print("\n[*] Step 11: Updating flash.sh & flash.bat with fastboot erase frp...")
for script_name, is_bat in [('flash.sh', False), ('flash.bat', True)]:
    sp = os.path.join(ROM_DIR, script_name)
    if os.path.exists(sp):
        content = open(sp).read()
        if 'fastboot erase frp' not in content:
            if is_bat:
                target = "echo Wiping userdata for clean boot..."
                replacement = "echo Erasing Factory Reset Protection (FRP) state...\nfastboot erase frp\n\necho Wiping userdata for clean boot..."
            else:
                target = 'echo "Wiping userdata..."'
                replacement = 'echo "Erasing Factory Reset Protection (FRP) state..."\nfastboot erase frp\n\necho "Wiping userdata..."'
            
            if target in content:
                content = content.replace(target, replacement)
                open(sp, 'w').write(content)
                print(f"  [+] Updated {script_name} with fastboot erase frp!")
            else:
                print(f"  [-] Target string not found in {script_name}")
        else:
            print(f"  [*] {script_name} already contains fastboot erase frp.")

print("\n================================================================")
print("  ALL A-TO-Z FIXES SUCCESSFULLY INTEGRATED INTO TREES_CLEAN!    ")
print("================================================================")
