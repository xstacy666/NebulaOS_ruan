#!/system/bin/sh
# Lock-screen double tap to sleep (DT2S) detector for ruan / NT36532.

RUNDIR=/data/vendor/dt2w
mkdir -p "$RUNDIR" 2>/dev/null
LOG="$RUNDIR/dt2w.log"
EN="$RUNDIR/dt2s_enabled"
LASTWAKE="$RUNDIR/last_wake"

log() { echo "$(date '+%Y-%m-%d %H:%M:%S') $*" >> "$LOG"; }

EV=""
for f in /sys/class/input/event*/device/name; do
    [ -r "$f" ] || continue
    grep -q 'NVTCapacitiveTouchScreen' "$f" 2>/dev/null || continue
    EV="/dev/input/$(basename "$(dirname "$(dirname "$f")")")"
    break
done
[ -n "$EV" ] || exit 1

prev=0
lasty=0
getevent -ql "$EV" 2>/dev/null | while IFS= read -r line; do
    case "$line" in
        *ABS_MT_POSITION_Y*)
            yv="$(echo "$line" | awk '{print $NF}')"
            [ -z "$yv" ] && continue
            case "$yv" in
                *[!0-9a-fA-F]*) ;;
                *) lasty=$((0x$yv)) ;;
            esac
            ;;
        *BTN_TOUCH*DOWN*)
            [ "$lasty" -le 15359 ] || continue
            [ "$(cat "$EN" 2>/dev/null)" = 1 ] || continue
            focus="$(dumpsys window 2>/dev/null | grep -m1 'mCurrentFocus=')"
            case "$focus" in
                *NotificationShade*) ;;
                *) continue ;;
            esac
            dumpsys window 2>/dev/null | grep -m1 -q 'isKeyguardShowing=true' || continue
            dumpsys power 2>/dev/null | grep -m1 -q 'mWakefulness=Awake' || continue

            now="$(date +%s%3N)"
            lw="$(cat "$LASTWAKE" 2>/dev/null)"
            [ -n "$lw" ] || lw=0
            [ $((now - lw)) -ge 1500 ] || continue

            if [ "$prev" -gt 0 ] && [ $((now - prev)) -le 450 ]; then
                log "DT2S: double tap on lock screen -> sleep"
                /system/bin/input keyevent 223
                prev=0
                sleep 1
            else
                prev=$now
            fi
            ;;
    esac
done
