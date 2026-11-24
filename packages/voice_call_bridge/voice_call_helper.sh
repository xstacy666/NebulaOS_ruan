#!/system/bin/sh
# Native Voice Call Audio Bridge for ColorOS on Redmi Pad Pro 5G (SM7435)

BRIDGE="/system_ext/bin/voice_call_bridge"
if [ ! -x "$BRIDGE" ]; then
    BRIDGE="/data/adb/modules/bugfix_opd2404/files/voice_call_bridge"
fi

case "$1" in
    2|on|active)
        exec "$BRIDGE" 2
        ;;
    1|off|idle)
        exec "$BRIDGE" 1
        ;;
    *)
        exec "$BRIDGE" daemon
        ;;
esac
