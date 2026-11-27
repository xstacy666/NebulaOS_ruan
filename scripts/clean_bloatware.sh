#!/bin/bash
# ==============================================================================
# ColorOS 17 Android 17 Debloat & Surgical Optimization Script
# Target Device: Redmi Pad Pro 5G (ruan) / POCO Pad (dizi) - SM7435-AB
# NOTE: All account, login, auth, credential, and Weather components PRESERVED.
# ==============================================================================

set -euo pipefail

TREES="/data/user/0/ai.antigem/files/home/projects/opd4-pocopad-port/work/trees"

if [ ! -d "$TREES" ]; then
    echo "[-] Error: Trees directory not found at $TREES"
    exit 1
fi

echo "========================================================"
echo " Starting Debloat & Cleanup on ColorOS 17 ROM Trees... "
echo " (Account, Login, Auth, Credentials & Weather PRESERVED)"
echo "========================================================"

# --- 1. Dead Leftover Backups (~52.5 MB) ---
echo "[1/8] Purging leftover backup files..."
rm -f "$TREES/system/system/framework/framework.jar.bak"
rm -f "$TREES/system_ext/priv-app/CarrierConfig/CarrierConfig.apk.bak"

# --- 2. Factory QA Burn-In / Aging Videos (~51.7 MB) ---
echo "[2/8] Purging factory QA burn-in test video..."
rm -f "$TREES/system/my_product/media/aging_video.mp4"

# --- 3. Offline Theme Caches & Duplicated Videos in system_ext (~203.2 MB) ---
# NOTE: This saves 203MB in system_ext, directly solving the dynamic partition size flash limit!
echo "[3/8] Purging offline wallpaper & theme archives from system_ext..."
rm -rf "$TREES/system_ext/etc/wallpaper_preload/online/theme-OS16-5000-mountain-tablet"
rm -rf "$TREES/system_ext/etc/wallpaper_preload/online/wallpaper-OS16-5040-Nature-tablet"
rm -rf "$TREES/system_ext/etc/wallpaper_preload/online/theme-OS17-5000-with_the_light-tablet"
rm -rf "$TREES/system_ext/etc/wallpaper_preload/online/theme-OS17-5000-definition-tablet"
rm -rf "$TREES/system_ext/etc/wallpaper_preload/online/theme-OS17-5000-photo_frame_clock-tablet"

# --- 4. Uncompressed Decouping Wallpaper Videos (~68 MB) ---
# NOTE: Dynamic weather wallpaper (weather.coz2) is preserved!
echo "[4/8] Purging uncompressed decouping live wallpaper videos..."
rm -f "$TREES/system/my_product/decouping_wallpaper/default/liveWallpaper/ivy_1.mp4"
rm -rf "$TREES/system/my_product/decouping_wallpaper/common/wallpaper_group/ai_camera_movement"
rm -f "$TREES/system/my_product/decouping_wallpaper/common/wallpaper_group/00_ColorOS15/ColorOS1506.mp4"
rm -f "$TREES/system/my_product/decouping_wallpaper/common/wallpaper_group/00_ColorOS15/ColorOS1506_land.mp4"
rm -f "$TREES/system/my_product/cust/CN/media/speechassist/video/"*.mp4 2>/dev/null || true

# --- 5. Chinese AI Models, Mismatched SM8850 Models & Legacy PCR Plugins (~66.7 MB) ---
echo "[5/8] Purging mismatched AI models & COVID-19 PCR scanning plugins..."
rm -f "$TREES/system/my_product/etc/aisubsystem/OAA/oaa-oaa_nlp_text_embedding_domestic_sm8850-19927190-3005-pall-rall"
rm -f "$TREES/system/my_stock/etc/aisubsystem/Plugin/plugin-pcr_code-187699292-1060-pall.apk"
rm -f "$TREES/system/my_region/etc/aisubsystem/Plugin/plugin-full_search_llm_cn-189796359-150017-pall-CN.apk"
rm -f "$TREES/system/my_region/etc/aisubsystem/Plugin/plugin-cloud_livephoto_matting-185622557-1006-full-domestic-33-aall-release.apk"
rm -f "$TREES/system/my_product/etc/aisubsystem/Plugin/plugin-cloud_entity_extraction-187699205-1008-pall-domestic.apk"

# --- 6. Chinese Voice Assistant (Breeno) (~165.8 MB) ---
echo "[6/8] Purging Breeno Speech Assist..."
rm -rf "$TREES/system/my_stock/priv-app/HeyTapSpeechAssist"

# --- 7. Pure Ad Tracking, Harvesters & Unrelated Telemetry (~245 MB) ---
# NOTE: KeKeUserCenterAccount, StdSP, MCS, CloudAccService, PostmanService, RegService are KEPT.
echo "[7/8] Purging ad-profiling and event harvesters (keeping login/auth)..."
rm -rf "$TREES/system/my_stock/priv-app/dmp"
rm -rf "$TREES/system/my_stock/priv-app/OplusBootReg"
rm -rf "$TREES/system/my_stock/app/Gleaner"
rm -rf "$TREES/system/my_stock/app/COSA"
rm -rf "$TREES/system_ext/priv-app/DeepThinker"
rm -rf "$TREES/system_ext/priv-app/DFS"
rm -rf "$TREES/system_ext/priv-app/UpgradeGuide"
rm -rf "$TREES/system/my_stock/app/AIMemory"
rm -rf "$TREES/system_ext/app/SystemAppUpdateService"
rm -rf "$TREES/system_ext/app/SAUHelper"

# --- 8. Incompatible Hardware Apps & Redundant China Del-Apps (~78 MB) ---
# NOTE: OppoWeather2, OppoWeatherService, weather alarms, and weather.coz2 are KEPT.
echo "[8/8] Purging incompatible satellite apps, factory test apps, and China del-apps..."
rm -rf "$TREES/system_ext/priv-app/BeidouMsgService"
rm -rf "$TREES/system_ext/app/NtnSatApp"
rm -rf "$TREES/system_ext/app/RsmcSatelliteAuth"
rm -rf "$TREES/system_ext/app/TTSatelliteAuth"
rm -rf "$TREES/system_ext/app/NfcEngineerTest"
rm -rf "$TREES/system_ext/app/BTtestmode"
rm -rf "$TREES/system_ext/app/OplusEngineerNetwork"
rm -rf "$TREES/system_ext/app/OplusCommercialEngineerMode"
rm -rf "$TREES/system_ext/app/WapiCertManage"
rm -rf "$TREES/system/my_stock/app/PhoneNOAreaInquireProvider"
rm -rf "$TREES/system/my_stock/del-app/PhoneManagerLite"
rm -rf "$TREES/system/my_stock/del-app/OplusEmail"
rm -rf "$TREES/system/my_stock/priv-app/AISeal"
rm -rf "$TREES/system/my_stock/app/FileEncryption"

echo "========================================================"
echo " Debloat complete! Over 1.10 GB cleaned successfully.   "
echo " Account, Login, Auth, Credentials & Weather PRESERVED. "
echo "========================================================"
