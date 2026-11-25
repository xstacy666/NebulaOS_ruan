#!/bin/sh
# Pair / connect / inspect a wireless ADB connection to the tablet.
set -eu

usage() {
    cat <<'EOF'
Usage:
  ./tools/adb-connect.sh pair <pair-ip> <pair-port> <6-digit-code>
  ./tools/adb-connect.sh <ip> <port>
  ./tools/adb-connect.sh status
  ./tools/adb-connect.sh disconnect
EOF
    exit 2
}

command -v adb >/dev/null 2>&1 || { echo "adb not found on PATH" >&2; exit 1; }

case "${1:-}" in
    pair)
        [ "$#" -eq 4 ] || usage
        echo "Pairing with $2:$3 ..."
        adb pair "$2:$3" "$4"
        ;;
    status)
        [ "$#" -eq 1 ] || usage
        adb devices -l
        ;;
    disconnect)
        [ "$#" -eq 1 ] || usage
        adb disconnect
        ;;
    "")
        usage
        ;;
    *)
        [ "$#" -eq 2 ] || usage
        echo "Connecting to $1:$2 ..."
        adb connect "$1:$2"
        adb devices -l
        ;;
esac
