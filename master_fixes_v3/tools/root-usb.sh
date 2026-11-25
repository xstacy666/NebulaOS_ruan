#!/bin/sh
# Run adb/fastboot as KernelSU root with a writable HOME for ADB keys.
# Root is needed to open /dev/bus/usb; the default root HOME is "/" which
# makes adb abort with "Cannot mkdir '//.android'".
set -eu

BIN=/data/user/0/ai.antigem/files/usr/bin
HOME_DIR=/data/user/0/ai.antigem/files/home

case "${1:-}" in
    adb|fastboot) TOOL=$1; shift ;;
    *)
        echo "Usage: $0 adb|fastboot <args...>" >&2
        exit 2
        ;;
esac

quote() { printf "'%s'" "$(printf '%s' "$1" | sed "s/'/'\\\\''/g")"; }

CMD="export HOME='$HOME_DIR'; exec '$BIN/$TOOL'"
for arg in "$@"; do
    CMD="$CMD $(quote "$arg")"
done

exec su -c "$CMD"
