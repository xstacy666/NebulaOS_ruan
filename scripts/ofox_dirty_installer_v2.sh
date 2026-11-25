#!/bin/sh
# Nebula OS v107-ext4 - OrangeFox/TWRP dirty-flash installer (NO WIPE)
# Streams images straight from the ZIP to block devices: no /tmp extraction.
API="$1"; FD="$2"; ZIPFILE="$3"
for arg in "$@"; do case "$arg" in *.zip) ZIPFILE="$arg" ;; esac; done
ui_print() {
  if [ -n "$FD" ] && [ -e "/proc/self/fd/$FD" ]; then
    echo "ui_print $1" > "/proc/self/fd/$FD" 2>/dev/null || echo "$1"
    echo "ui_print " > "/proc/self/fd/$FD" 2>/dev/null || true
  else echo "$1"; fi
}
ui_print "=========================================="
ui_print " NEBULA OS v107-ext4  (DIRTY FLASH v2)"
ui_print " Data is NOT wiped. Raw super image."
ui_print "=========================================="
[ -f "$ZIPFILE" ] || { ui_print "[-] ZIP not found: $ZIPFILE"; exit 1; }
SLOT=$(getprop ro.boot.slot_suffix 2>/dev/null | tr -d '\r'); [ -z "$SLOT" ] && SLOT=_a
ui_print "-> Flashing super (raw)..."
unzip -p "$ZIPFILE" images/super.img | dd of=/dev/block/by-name/super bs=1048576 status=none || { ui_print "[-] super flash failed"; exit 1; }
for p in boot vendor_boot dtbo; do
  ui_print "-> Flashing $p$SLOT..."
  unzip -p "$ZIPFILE" "images/$p.img" | dd of="/dev/block/by-name/${p}${SLOT}" bs=1048576 status=none || ui_print "[!] $p failed"
done
for p in vbmeta vbmeta_system; do
  ui_print "-> Flashing $p$SLOT..."
  unzip -p "$ZIPFILE" "images/$p.img" | dd of="/dev/block/by-name/${p}${SLOT}" bs=4096 status=none || ui_print "[!] $p failed"
done
ui_print "[+] Done. Reboot. If bootloop: fastboot flash vbmeta --disable-verity."
exit 0
