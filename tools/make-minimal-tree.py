#!/usr/bin/env python3
"""Revert staged app/font/overlay/APK changes, keeping only build.prop fixes."""
import os
import shutil

ROOT = "/data/user/0/ai.antigem/files/home/projects/opd4-pocopad-port"
TREE = os.path.join(ROOT, "work/system_tree")
SEXT = os.path.join(ROOT, "work/systemext_tree")
DONOR_ROOT = "/storage/emulated/0/port/archive/x9ultra-17/trees/"

with open(os.path.join(ROOT, "work/audit/donor-packages.txt")) as f:
    donors = {}
    for line in f:
        pkg, path = line.rstrip("\n").split("\t", 1)
        donors.setdefault(pkg, path)
with open(os.path.join(ROOT, "work/oppo-features-list.txt")) as f:
    wanted = [l.strip() for l in f if l.strip() and not l.startswith("#")]

for pkg in wanted:
    apk = donors.get(pkg)
    if not apk:
        continue
    rel = os.path.dirname(apk[len(DONOR_ROOT):])
    d = os.path.join(TREE, rel)
    if os.path.isdir(d):
        shutil.rmtree(d)
        print("removed staged", rel)

for p in [
    "system/overlay/RuanCornerOverlay.apk",
    "system/fonts/Inter.ttf",
    "system/fonts/SpaceGrotesk.ttf",
    "system/fonts/Caveat[wght].ttf",
    "system/fonts/Pacifico-Regular.ttf",
    "system/fonts/IndieFlower-Regular.ttf",
    "system/fonts/PatrickHand-Regular.ttf",
    "system/fonts/Kalam-Regular.ttf",
    "system/fonts/ArchitectsDaughter-Regular.ttf",
]:
    fp = os.path.join(TREE, p)
    if os.path.exists(fp):
        os.remove(fp)

# revert fonts.xml additions
fp = os.path.join(TREE, "system/etc/fonts.xml")
s = open(fp).read()
lines = s.splitlines(True)
out, skip = [], False
for l in lines:
    if '<family name="Inter">' in l or '<family name="SpaceGrotesk">' in l or '<family name="Handwriting' in l:
        skip = True
    if skip:
        if '</family>' in l:
            skip = False
        continue
    out.append(l)
open(fp, "w").write("".join(out))

# restore original APKs
for src, dst in [
    (os.path.join(ROOT, "reference/systemui/SystemUI.apk"), os.path.join(SEXT, "priv-app/SystemUI/SystemUI.apk")),
    (os.path.join(ROOT, "reference/kaorios/Kaorios.apk"), os.path.join(TREE, "system/priv-app/KaoriosToolbox/KaoriosToolbox.apk")),
]:
    if os.path.exists(src):
        shutil.copy2(src, dst)
        print("restored", dst)
print("minimal tree ready")
