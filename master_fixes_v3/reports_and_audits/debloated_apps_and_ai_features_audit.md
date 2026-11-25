# Debloated Apps & Oppo AI Feature Restoration Audit

This audit compares the original **OnePlus Pad 2 stock firmware dump** (`OPD2404_17.0.0.102_20260924`) against the current live ported ROM (`ruan_p`). It categorizes all missing applications, on-device AI models, and services needed to restore the complete Oppo AI, Breeno, and Game Space ecosystems.

---

## 1. Summary of Changes

| Partition | Total in OnePlus Dump | Present in Current ROM | Debloated / Missing |
| :--- | :---: | :---: | :---: |
| **`my_stock`** | 126 APKs | 64 APKs | **62 APKs** |
| **`my_product`** | 28 APKs | 22 APKs | **7 APKs** |
| **`system_ext`** | 35 APKs | 30 APKs | **5 APKs** |
| **`system`** | 71 APKs | 71 APKs | **0 APKs** |
| **`product`** | 4 APKs | 4 APKs | **0 APKs** |
| **`my_preload`** | 1 APK (Netflix) | 0 APKs | **1 APK** |
| **Total** | **265 APKs** | **191 APKs** | **74 APKs** |

---

## 2. Oppo AI & AndesGPT Ecosystem

These applications and models form ColorOS 17's AI engine. All binaries and on-device weights are verified inside `/data/local/tmp/dump_my_stock` and `/data/local/tmp/romdump/mp`.

| Feature | App / Package Directory | Partition Path | Purpose |
| :--- | :--- | :--- | :--- |
| **Oppo AI Core Engine** | `AIUnit` | `my_stock/priv-app/AIUnit/` | Core system AI daemon required by all AI plugins |
| **AI Writer** | `AIWriter` | `my_stock/app/AIWriter/` | System text rewriting, formatting, polish, and tone shift |
| **AI Painter** | `AIPaint` | `my_product/app/AIPaint/` | Drawing assistant and image generation engine |
| **AI Translation** | `OppoTranslation` | `my_stock/del-app/OppoTranslation/` | Real-time text, voice, and full-screen screen translation |
| **AI Call Assistant** | `AICallAssistant` | `my_product/priv-app/AICallAssistant/` | Real-time call transcription, translation, and summary |
| **AI Memory** | `AIMemory` | `my_stock/app/AIMemory/` | Contextual memory system for personal habits |
| **AI Widgets** | `AIWidgets` | `my_stock/app/AIWidgets/` | Dynamic smart cards and desktop AI suggestions |
| **AI Notes (Smart Format)** | `OppoNote2` | `my_stock/del-app/OppoNote2/` | ColorOS Notes app with AI transcription & auto-summaries |
| **OCR Scanner Engine** | `BaseOcrScanner` / `OcrScanner` | `my_stock/app/` & `my_stock/del-app/` | Optical character recognition & document scan |
| **Sandbox Runtime** | `SandboxRuntime` | `my_stock/priv-app/SandboxRuntime/` | Sandboxed runtime container for AI models |

### On-Device AI Models & Subsystems
Located in `my_product/etc/aisubsystem/` and `my_stock/etc/aisubsystem/`:
- **NLP Text Embedding Model**: `oaa-oaa_nlp_text_embedding_domestic_sm8850` (44.6 MB)
- **Entity Extraction Plugin**: `plugin-cloud_entity_extraction` (2.3 MB)
- **PCR Code Plugin**: `plugin-pcr_code` (14.8 MB)
- **Configuration Profiles**: `OAA/`, `OAP/`, `Plugin/`, `config/`, `orange_config/`

---

## 3. Breeno & Speech Services

| Component | Directory | Partition Path | Functionality |
| :--- | :--- | :--- | :--- |
| **Breeno Voice Assistant** | `HeyTapSpeechAssist` | `my_stock/priv-app/HeyTapSpeechAssist/` | Primary voice assistant interface and wake engine |
| **Voice Framework Service** | `OVoiceManagerService` | `my_product/priv-app/OVoiceManagerService/` | Low-level IPC bridge between apps and speech engine |
| **Voice Skill Execution** | `OVoiceManagerSkillService` | `my_stock/app/OVoiceManagerSkillService/` | Executes voice macros, smart home, and system actions |
| **Breeno Feed / Quick Glance** | `AssistantScreen` | `my_stock/app/AssistantScreen/` | -1 Screen / Quick Glance feed |
| **Breeno Speech Audio Profiles**| `breenospeech2` | `my_product/etc/breenospeech2/` | Tuned audio models and acoustic configurations |

---

## 4. Gaming Engine & Game Space

| Component | Directory | Partition Path | Functionality |
| :--- | :--- | :--- | :--- |
| **OnePlus Games (Game Space)**| `OplusGames` | `my_stock/priv-app/OplusGames/` | Floating gaming sidebar, performance modes, FPS counter |
| **Game Center** | `Gamecenter` | `my_stock/del-app/Gamecenter/` | Game hub, cloud gaming, game account manager |
| **Game Manager Profiles** | `sys_game_manager_config.json` | `my_product/etc/extension/` | Frame stabilization and touch sensitivity configs |
| **GPU Control Panel** | `sys_graphics_game_gpu_control_panel.json` | `my_product/etc/extension/` | Per-game graphics, MSAA, and refresh rate overrides |
| **Game Eco Policy** | `oplus.product.features_gameeco_*.xml` | `my_product/etc/permissions/` | Feature flags unlocking 120Hz/90Hz gaming modes |

---

## 5. Tablet Interconnection & Productivity

| Component | Directory | Partition Path | Functionality |
| :--- | :--- | :--- | :--- |
| **Pad Multi-Screen Connect** | `PadConnect` | `my_stock/priv-app/PadConnect/` | Screen mirror, drag-and-drop, phone relay on tablet |
| **Cross-Device Synergy** | `OPSynergy` | `my_stock/priv-app/OPSynergy/` | Shared clipboard, call relay, unified notification hub |
| **Smart Sidebar Dock** | `ContentPortal` | `my_stock/priv-app/ContentPortal/` | Drag-and-drop file dock and sidebar shelf |
| **Screen Cast Engine** | `HeyCast` | `my_stock/priv-app/HeyCast/` | Low-latency display streaming to TVs and monitors |
| **Assistive Ball** | `FloatAssistant` | `my_stock/app/FloatAssistant/` | Floating navigation button for tablet gestures |
| **Screen Sharing** | `ShareScreen` | `my_stock/app/ShareScreen/` | Remote screen assistance and collaborative viewing |
| **Office & Documents** | `OWork` & `OplusDocumentsReader` | `my_stock/app/` & `my_stock/del-app/` | Built-in viewer for Office documents and PDFs |

---

## 6. Official App Stores & Cloud Services

| Component | Directory | Partition Path | Status |
| :--- | :--- | :--- | :--- |
| **App Market** | `KeKeMarket` | `my_stock/priv-app/KeKeMarket/` | Official ColorOS app store |
| **Theme Store** | `KeKeOplusThemeStore-CN` | `my_stock/priv-app/KeKeOplusThemeStore-CN/` | Official fonts, themes, AOD, and icon packs |
| **Theme Space** | `KeKeThemeSpace` | `my_stock/del-app/KeKeThemeSpace/` | Theme customization engine |
| **HeyTap Cloud** | `CloudService` | `my_stock/app/CloudService/` | Cloud backup, contact sync, notes backup |
| **ColorOS Browser** | `Browser` & `BrowserVideo` | `my_stock/app/` & `del-app/` | Stock browser with video player integration |
| **HeyTap Pay** | `KeKePay` & `SecurePay` | `my_stock/app/` | NFC/payment security daemon |

---

## 7. Telegram Channel Updates (@Coloros17App)

From `https://t.me/Coloros17App`, the following updated core package builds are available:

- `UXDesign_17.0.20.apk`
- `UMS_17.22.8.apk`
- `QuickGlance_17.0.54.apk`
- `WallpaperChooser_17.0.27.apk`
- `WirelessSettings_17.6.5.apk`
- `Wallpapers_17.4.0061.apk`

---

## 8. Restoration Strategy

1. **System-Level Integration**:
   Copy the missing applications and model files directly into `/data/media/0/Fastboot_ROM/extracted_partitions/` (`my_stock` and `my_product` paths).
2. **SELinux Permissions & Ownership**:
   Set `chmod -R 644` on APKs and `chmod 755` on directories with context `u:object_r:system_file:s0`.
3. **Privileged Permissions**:
   Ensure `etc/permissions/privapp-permissions-*.xml` entries exist for `AIUnit`, `HeyTapSpeechAssist`, and `OplusGames` so Android does not fail startup validation.
4. **Partition Placement**:
   Verify size boundaries before running `mkfs.erofs` and `lpmake` to ensure the dynamic partitions fit cleanly within `super.img`.
