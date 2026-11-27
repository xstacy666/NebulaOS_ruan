#!/data/user/0/ai.antigem/files/usr/bin/python3
"""Find the tablet on the USB tether link and connect adb to its TCP port."""
import re
import subprocess
import sys

PORT = "5555"
target = sys.argv[1] if len(sys.argv) > 1 else None

out = subprocess.run(["ifconfig", "-a"], capture_output=True, text=True).stdout

iface = None
found = []
for line in out.splitlines():
    m = re.match(r"^([A-Za-z0-9_-]+):", line)
    if m:
        iface = m.group(1)
    m = re.search(r"inet\s+(?:addr:)?(\d+\.\d+\.\d+\.\d+)", line)
    if m and iface and re.match(r"^(usb|rndis|eth|en)", iface):
        found.append((iface, m.group(1)))

print("USB tether interfaces visible to this shell:")
for name, ip in found:
    print(f"  {name}: {ip}")
if not found:
    print("  none — enable USB tethering on the tablet first")
    sys.exit(1)

candidates = []
if target:
    candidates.append(target)
for _, ip in found:
    parts = [int(p) for p in ip.split(".")]
    subnet = ".".join(str(p) for p in parts[:3])
    for last in (1, parts[3] - 1, parts[3] + 1):
        if 0 < last < 255:
            candidates.append(f"{subnet}.{last}")

for host in dict.fromkeys(candidates):
    addr = f"{host}:{PORT}"
    print(f"trying {addr}")
    r = subprocess.run(["adb", "connect", addr], capture_output=True, text=True)
    print((r.stdout + r.stderr).strip())
    state = subprocess.run(
        ["adb", "get-state"], capture_output=True, text=True
    ).stdout.strip()
    if state == "device":
        print(f"CONNECTED: {addr}")
        break
else:
    print("No connection. Pass the tablet IP explicitly, e.g.:")
    print("  ./tools/usb-adb-connect.py 192.168.42.1")
    sys.exit(1)
