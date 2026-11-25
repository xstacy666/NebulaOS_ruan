#!/usr/bin/env python3
"""Rebuild super.img from lpdump metadata + a directory of partition images.

Usage: rebuild_super.py <lpdump.txt> <parts_dir> <output.img>

Parses `lpdump` output, keeps every partition name, flag and size, and emits
an lpmake command that reproduces the original dynamic-partition layout while
using the (possibly converted ext4) images from <parts_dir>.
"""
import os
import re
import subprocess
import sys

lpdump_file, parts_dir, output = sys.argv[1:4]
text = open(lpdump_file, errors="replace").read()

metadata_size = re.search(r"Metadata size:\s*(\d+)", text)
metadata_slots = re.search(r"Metadata slots:\s*(\d+)", text)
super_name = re.search(r"Super name:\s*(\S+)", text)

parts = []
# Typical line:
#   name: system, group: qti_dynamic_partitions_a, attributes: 0, first extent: 0,
#   last extent: 1, size: 3221225472, ...
for m in re.finditer(
    r"name:\s*([A-Za-z0-9_.\-]+).*?group:\s*([A-Za-z0-9_.\-]+).*?"
    r"(?:attributes:\s*(\d+).*?)?size:\s*(\d+)",
    text,
    re.S,
):
    name, group, attrs, size = m.group(1), m.group(2), m.group(3) or "0", m.group(4)
    parts.append((name, group, attrs, int(size)))

# de-duplicate by name, keep the largest size seen
seen = {}
for name, group, attrs, size in parts:
    if name not in seen or size > seen[name][2]:
        seen[name] = (group, attrs, size)
parts = [(n, g, a, s) for n, (g, a, s) in seen.items()]

if not parts:
    print("ERROR: could not parse any partitions from lpdump output", file=sys.stderr)
    sys.exit(1)

groups = {}
for name, group, attrs, size in parts:
    groups[group] = groups.get(group, 0) + size

cmd = [
    "lpmake",
    "--metadata-size", str(int(metadata_size.group(1)) if metadata_size else 65536),
    "--super-name", super_name.group(1) if super_name else "super",
    "--metadata-slots", str(int(metadata_slots.group(1)) if metadata_slots else 2),
    "--device", "%s:%d" % (
        super_name.group(1) if super_name else "super",
        sum(s for _, _, _, s in parts) + (int(metadata_size.group(1)) if metadata_size else 65536) * 2 + 1048576,
    ),
]
for group, size in groups.items():
    cmd += ["--group", "%s:%d" % (group, size)]
for name, group, attrs, size in parts:
    cmd += ["--partition", "%s:%s:%d" % (name, "readonly" if attrs == "0" else "readwrite", size)]
cmd += ["--sparse", "-o", output]
for name, group, attrs, size in parts:
    f = os.path.join(parts_dir, name + ".img")
    if os.path.exists(f):
        cmd += ["--image", "%s=%s" % (name, f)]

print("[*] " + " ".join(cmd))
subprocess.run(cmd, check=True)
print("[+] wrote " + output)
