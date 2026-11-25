#!/usr/bin/env python3
"""Apply all port fixes to a root-owned extracted system tree."""
import os
import shutil
import tempfile

ROOT = "/data/user/0/ai.antigem/files/home/projects/opd4-pocopad-port"
TMP = tempfile.gettempdir()
SYS = os.path.join(ROOT, "work/sys2")
SEXT = os.path.join(ROOT, "work/se2")
DONOR_ROOT = "/storage/emulated/0/port/archive/x9ultra-17/trees/"

# 1. build.prop
p = os.path.join(SYS, "system/build.prop")
s = open(p).read().replace("ro.product.model=24074PCD2I", "ro.product.model=POCO Pad 5G")
extra = """
# --- port fixes ---
persist.oplus.display.vrr=0
debug.sf.defer_refresh_rate_when_off=1
debug.sf.late.app.duration=16666666
debug.sf.late.sf.duration=16666666
debug.sf.early.app.duration=16666666
debug.sf.early.sf.duration=16666666
debug.sf.earlyGl.app.duration=16666666
debug.sf.earlyGl.sf.duration=16666666
debug.sf.use_phase_offsets_as_durations=1
debug.sf.set_idle_timer_ms=1100
debug.sf.enable_hwc_vds=1
vendor.display.vds_allow_hwc=0
vendor.display.enable_rotator_ui=0
vendor.display.enable_optimize_refresh=0
vendor.display.enable_dpps_dynamic_fps=0
vendor.display.disable_mitigated_fps=1
ro.launcher.depth.overview=false
ro.launcher.blur.appLaunch=0
persist.sys.sf.disable_blur=1
persist.sys.locale=en-US
persist.sys.oplus.anim_level=3
pm.dexopt.first-boot=verify
pm.dexopt.boot=verify
pm.dexopt.install=speed-profile
pm.dexopt.bg-dexopt=speed-profile
pm.dexopt.inactive=verify
"""
if "port fixes ---" not in s:
    s = s.rstrip("\n") + "\n" + extra
open(p, "w").write(s)

# 2. stage OPPO apps
with open(os.path.join(ROOT, "work/audit/donor-packages.txt")) as f:
    donors = {}
    for line in f:
        pkg, path = line.rstrip("\n").split("\t", 1)
        donors.setdefault(pkg, path)
with open(os.path.join(ROOT, "work/oppo-features-list.txt")) as f:
    wanted = [l.strip() for l in f if l.strip() and not l.startswith("#")]
for pkg in wanted:
    apk = donors.get(pkg)
    if not apk or not os.path.exists(apk):
        continue
    rel = os.path.dirname(apk[len(DONOR_ROOT):])
    src, dst = os.path.dirname(apk), os.path.join(SYS, rel)
    os.makedirs(dst, exist_ok=True)
    for name in os.listdir(src):
        a, b = os.path.join(src, name), os.path.join(dst, name)
        if os.path.isdir(a):
            if os.path.exists(b):
                shutil.rmtree(b)
            shutil.copytree(a, b, copy_function=shutil.copy2)
        else:
            shutil.copy2(a, b)

# 3. fonts + overlay + Kaorios
fonts = os.path.join(SYS, "system/fonts")
names = []
for fn in ["Inter.ttf", "SpaceGrotesk.ttf"]:
    src = os.path.join(TMP, fn)
    if os.path.exists(src):
        shutil.copy2(src, os.path.join(fonts, fn))
        names.append(fn)
for fn in sorted(os.listdir(os.path.join(TMP, "fonts2"))):
    if fn.endswith(".ttf"):
        shutil.copy2(os.path.join(TMP, "fonts2", fn), os.path.join(fonts, fn))
        names.append(fn)
fxml = os.path.join(SYS, "system/etc/fonts.xml")
s = open(fxml).read()
fams = []
for fn in names:
    fam = "Custom" + fn.replace(".ttf", "").replace("[", "").replace("]", "").replace("-", "")
    fams.append('    <family name="%s">\n        <font weight="400" style="normal" postScriptName="%s">%s</font>\n    </family>\n' % (fam, fam, fn))
if fams and 'family name="CustomInter"' not in s:
    s = s.replace("</familyset>", "".join(fams) + "</familyset>")
open(fxml, "w").write(s)

ov = os.path.join(SYS, "system/overlay")
os.makedirs(ov, exist_ok=True)
shutil.copy2(os.path.join(TMP, "RuanCornerOverlay.apk"), os.path.join(ov, "RuanCornerOverlay.apk"))

shutil.copy2(os.path.join(TMP, "Kaorios_patched.apk"), os.path.join(SYS, "system/priv-app/KaoriosToolbox/KaoriosToolbox.apk"))

# 4. SystemUI patch + remove updater
shutil.copy2(os.path.join(TMP, "SystemUI_patched.apk"), os.path.join(SEXT, "priv-app/SystemUI/SystemUI.apk"))
oat = os.path.join(SEXT, "priv-app/SystemUI/oat")
if os.path.isdir(oat):
    shutil.rmtree(oat)
shutil.rmtree(os.path.join(SEXT, "priv-app/Updater"), ignore_errors=True)
for f in ["privapp-permissions-org.nebula.updater.xml", "privapp-permissions-updater.xml"]:
    fp = os.path.join(SEXT, "etc/permissions", f)
    if os.path.exists(fp):
        os.remove(fp)
ota = os.path.join(SYS, "my_product/etc/extension/feature_ota.xml")
if os.path.exists(ota):
    os.remove(ota)

print("all changes applied; fonts:", len(names))
