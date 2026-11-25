#!/system/bin/sh
# Late boot: recover if something reset the prop or adbd started USB-only.
setprop service.adb.tcp.port 5555
stop adbd 2>/dev/null
start adbd 2>/dev/null
