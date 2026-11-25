#!/system/bin/sh
# Set the TCP port before init starts adbd, so it binds 0.0.0.0:5555 at boot.
setprop service.adb.tcp.port 5555
