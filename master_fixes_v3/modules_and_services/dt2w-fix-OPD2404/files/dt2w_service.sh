#!/system/bin/sh
# Native DT2W + DT2S + Quick Unlock + Dynamic Device Identity Service
# For POCO Pad 5G (ruan) & Redmi Pad Pro (dizi) / Novatek NT36532
# Zero KernelSU dependency - Built into ColorOS 17 system_ext

# Wait for system_server and SettingsProvider to be ready
until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 2
done

# Dynamic PIN / Password Auto-Unlock Daemon
# Dynamically synchronizes locksettings.db, PASSWORD_LENGTH, and keyguard_quick_unlock
setprop persist.sys.ls.ui_type 0

run_dynamic_unlocker() {
    SQLITE_BIN="/system_ext/bin/sqlite_query"
    [ -x "$SQLITE_BIN" ] || SQLITE_BIN="/system/system_ext/bin/sqlite_query"
    [ -x "$SQLITE_BIN" ] || SQLITE_BIN="/data/vendor/dt2w/sqlite_query"
    [ -x "$SQLITE_BIN" ] || SQLITE_BIN="/data/local/tmp/sqlite_query"

    LAST_TYPE=""
    LAST_DB_MTIME=""

    while true; do
        if [ -f /data/system/locksettings.db ] && [ -x "$SQLITE_BIN" ]; then
            DB_MTIME="$(stat -c %Y /data/system/locksettings.db 2>/dev/null)"
            if [ "$DB_MTIME" != "$LAST_DB_MTIME" ]; then
                LAST_DB_MTIME="$DB_MTIME"
                TYPE="$("$SQLITE_BIN" /data/system/locksettings.db "SELECT value FROM locksettings WHERE name='lockscreen.qu.ui.type' AND user=0 LIMIT 1;" 2>/dev/null)"
                if [ -n "$TYPE" ] && [ "$TYPE" != "$LAST_TYPE" ]; then
                    LAST_TYPE="$TYPE"
                    setprop persist.sys.ls.ui_type 0
                    if [ "$TYPE" = "4" ] || [ "$TYPE" = "6" ]; then
                        settings put system PASSWORD_LENGTH "$TYPE" 2>/dev/null || true
                        settings put secure lockscreen.pin_length "$TYPE" 2>/dev/null || true
                        settings put secure keyguard_quick_unlock 1 2>/dev/null || true
                        settings put system keyguard_quick_unlock 1 2>/dev/null || true
                        settings put secure keyguard_disable_quick_unlock 0 2>/dev/null || true
                        settings put secure lockscreen.pin_auto_confirm 1 2>/dev/null || true
                        settings put secure pin_auto_confirm 1 2>/dev/null || true
                        "$SQLITE_BIN" /data/system/locksettings.db "DELETE FROM locksettings WHERE name='lockscreen.pin_password_length' AND user=0; INSERT INTO locksettings (name, user, value) VALUES ('lockscreen.pin_password_length', 0, '$TYPE');" 2>/dev/null || true
                    else
                        settings put system PASSWORD_LENGTH 0 2>/dev/null || true
                        settings put secure lockscreen.pin_length 0 2>/dev/null || true
                        settings put secure keyguard_quick_unlock 0 2>/dev/null || true
                        settings put system keyguard_quick_unlock 0 2>/dev/null || true
                        settings put secure lockscreen.pin_auto_confirm 0 2>/dev/null || true
                        settings put secure pin_auto_confirm 0 2>/dev/null || true
                    fi
                fi
            fi
        fi
        sleep 2
    done
}
run_dynamic_unlocker &


# Initialize display refresh rate & smooth animations
settings put system min_refresh_rate 120.0 2>/dev/null || true
settings put system peak_refresh_rate 120.0 2>/dev/null || true
settings put global window_animation_scale 1.0 2>/dev/null || true
settings put global transition_animation_scale 1.0 2>/dev/null || true
settings put global animator_duration_scale 1.0 2>/dev/null || true

# Initialize Quick Unlock (no Enter/OK needed on lockscreen)
settings put secure keyguard_quick_unlock 1 2>/dev/null || true
settings put system keyguard_quick_unlock 1 2>/dev/null || true
settings put secure keyguard_disable_quick_unlock 0 2>/dev/null || true
settings put secure lockscreen.pin_auto_confirm 1 2>/dev/null || true
settings put secure pin_auto_confirm 1 2>/dev/null || true

# Initialize Double Tap to Wake & Double Tap to Sleep
settings put secure double_tap_to_wake 1 2>/dev/null || true
settings put system dt2s_enabled 1 2>/dev/null || true

# Dynamic Device Identification:
#   SIM variant (ruan): POCO Pad 5G / 24074PCD2I
#   Wi-Fi variant (dizi): Redmi Pad Pro / 2405CRPFDL
HWNAME="$(getprop ro.boot.hwname)"
NORIL="$(getprop ro.radio.noril)"
BASEBAND="$(getprop gsm.version.baseband)"

if [ "$NORIL" = "yes" ] || [ "$HWNAME" = "dizi" ] || [ -z "$BASEBAND" ] || [ "$BASEBAND" = "null" ]; then
    DEV_NAME="Redmi Pad Pro"
    DEV_MODEL="2405CRPFDL"
    DEV_MARKET="Redmi Pad Pro"
else
    DEV_NAME="Poco Pad 5G"
    DEV_MODEL="24074PCD2I"
    DEV_MARKET="Poco Pad 5G"
fi

# Apply systemwide device names
settings put global device_name "$DEV_NAME" 2>/dev/null || true
settings put global revise_device_name "$DEV_NAME" 2>/dev/null || true
settings put system device_name "$DEV_NAME" 2>/dev/null || true
settings put system oplus_customize_device_name "$DEV_NAME" 2>/dev/null || true
settings put secure bluetooth_name "$DEV_NAME" 2>/dev/null || true
settings put secure device_tether_name "$DEV_NAME" 2>/dev/null || true

# Apply persistent and runtime properties
setprop persist.sys.device_name "$DEV_NAME" 2>/dev/null || true
setprop bluetooth.device.default_name "$DEV_NAME" 2>/dev/null || true
setprop ro.config.device_name "$DEV_NAME" 2>/dev/null || true

# Ensure thermal config is active
echo 0 > /sys/class/thermal/thermal_message/sconfig 2>/dev/null || true

RUNDIR=/data/vendor/dt2w
mkdir -p "$RUNDIR" 2>/dev/null
chmod 0777 "$RUNDIR" 2>/dev/null
LOG="$RUNDIR/dt2w.log"
APPLIED="$RUNDIR/dt2w_applied"
DT2S_EN="$RUNDIR/dt2s_enabled"
LASTWAKE="$RUNDIR/last_wake"

log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') $*" >> "$LOG"
}


# Preload offline Flux Themes & Wallpapers if not present
WP_DIR="/data/data/com.oplus.wallpapers"
if [ -d "$WP_DIR" ] && [ ! -d "$WP_DIR/files/online/theme-OS17-5000-photo_frame_clock-tablet" ]; then
    PRELOAD_SRC="/system/system_ext/etc/wallpaper_preload"
    [ -d "$PRELOAD_SRC" ] || PRELOAD_SRC="/system_ext/etc/wallpaper_preload"
    if [ -d "$PRELOAD_SRC/online" ]; then
        mkdir -p "$WP_DIR/files/online" "$WP_DIR/databases"
        cp -rn "$PRELOAD_SRC/online/"* "$WP_DIR/files/online/" 2>/dev/null || true
        if [ ! -f "$WP_DIR/databases/InspirationTheme-db" ] && [ -f "$PRELOAD_SRC/InspirationTheme-db" ]; then
            cp "$PRELOAD_SRC/InspirationTheme-db"* "$WP_DIR/databases/" 2>/dev/null || true
        fi
        WP_UID="$(stat -c '%u' "$WP_DIR" 2>/dev/null)"
        [ -n "$WP_UID" ] && chown -R "$WP_UID:$WP_UID" "$WP_DIR/files/online" "$WP_DIR/databases" 2>/dev/null || true
        chmod -R 775 "$WP_DIR/files/online" 2>/dev/null || true
    fi
fi

find_touch_event() {
    for f in /sys/class/input/event*/device/name; do
        [ -r "$f" ] || continue
        grep -q 'NVTCapacitiveTouchScreen' "$f" 2>/dev/null || continue
        echo "/dev/input/$(basename "$(dirname "$(dirname "$f")")")"
        return 0
    done
    return 1
}


# Safe On-Screen Fast Charging (regulated at ~15W-18W / 3200mA, avoids 500mA throttle)
regulate_charging() {
    # Ensure cooling device 36 does not drop into deep throttle state (> 2) while screen is awake
    if [ -d /sys/class/thermal/cooling_device36 ]; then
        cur_cdev="$(cat /sys/class/thermal/cooling_device36/cur_state 2>/dev/null)"
        if [ "$cur_cdev" -gt 2 ] 2>/dev/null; then
            echo 2 > /sys/class/thermal/cooling_device36/cur_state 2>/dev/null || true
        fi
    fi
    # Set safe constant charge limit
    if [ -w /sys/class/power_supply/battery/charge_control_limit ]; then
        echo 3200000 > /sys/class/power_supply/battery/charge_control_limit 2>/dev/null || true
    fi
}

screen_on() {
    st="$(dumpsys power 2>/dev/null | grep -m1 'mWakefulness=')"
    case "$st" in *Awake*) ;; *) return 1 ;; esac
    for b in /sys/class/backlight/*/brightness; do
        [ -r "$b" ] || continue
        v="$(cat "$b" 2>/dev/null)"
        case "$v" in ''|*[!0-9]*) continue ;; esac
        [ "$v" -gt 0 ] 2>/dev/null && return 0
    done
    return 1
}

set_gesture() {
    ev="$(find_touch_event)" || return 1
    if [ "$1" = 1 ]; then
        /system/bin/sendevent "$ev" 0 1 5 2>/dev/null
    else
        /system/bin/sendevent "$ev" 0 1 4 2>/dev/null
    fi
}

# Wait for touch input event device
ev=""
i=0
while [ "$i" -lt 60 ]; do
    ev="$(find_touch_event)" && [ -n "$ev" ] && break
    i=$((i + 1))
    regulate_charging
        sleep 1
done

set_gesture 1
echo 1 > "$APPLIED"
log "boot: armed DT2W=1 on $ev"

# Start the lock-screen double-tap-to-sleep detector
if [ -x /system/system_ext/bin/dt2s.sh ] || [ -x /system_ext/bin/dt2s.sh ]; then
    DT2S_BIN="/system_ext/bin/dt2s.sh"
    [ -x "$DT2S_BIN" ] || DT2S_BIN="/system/system_ext/bin/dt2s.sh"
    /system/bin/sh "$DT2S_BIN" >/dev/null 2>&1 &
fi

prev=1
n=0

while :; do
    sleep 4

    on=0
    screen_on && on=1

    setting="$(settings get secure double_tap_to_wake 2>/dev/null)"
    [ "$setting" = 0 ] || setting=1
    dt2s="$(settings get system dt2s_enabled 2>/dev/null)"
    [ "$dt2s" = 0 ] || dt2s=1

    echo "$dt2s" > "$DT2S_EN"

    if [ "$on" = 1 ]; then
        if [ "$prev" = 0 ]; then
            date +%s%3N > "$LASTWAKE"
            set_gesture "$setting"
            echo "$setting" > "$APPLIED"
            log "screen on: re-armed DT2W=$setting"
            n=0
        elif [ "$(cat "$APPLIED" 2>/dev/null)" != "$setting" ]; then
            set_gesture "$setting"
            echo "$setting" > "$APPLIED"
            log "Settings toggle: DT2W=$setting"
            n=0
        fi
    else
        # Screen is off - ensure DT2W gesture is armed in touch IC
        if [ "$setting" = 1 ]; then
            set_gesture 1
        fi
    fi
    prev=$on
done
