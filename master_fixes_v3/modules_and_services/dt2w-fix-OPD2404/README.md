# DT2W fix for OnePlus OPD2404 (caramel kernel)

## Root cause

Two independent defects in the stock kernel broke double-tap-to-wake:

1. `/vendor/bin/dt2w_daemon` sends ioctl mode **14** (`{0,14,1}`) every second.
   On this kernel mode 14 is a game-buffer write. The real DT2W gesture mode is
   **24** (`nvt_set_gesture_mode()` → `gesture_mode = 2`, `Enabled touch wakeup gesture`).

2. `lct_tp.ko` contains `init_lct_tp_gesture()` and a `/proc/tp_gesture` write
   handler, but `lct_tp_create_procfs()` never calls it. Also the touch driver's
   `db_wakeup` flag is never set, so even though the touch IC reports
   `gesture_id = 15 / Gesture : Double Click.`, the driver logs
   `Gesture : Double Click Not Enable.` and never wakes the device.

The framework settings (`secure double_tap_to_wake`, `system double_wake_status`)
were already 1 and were not the problem.

## Fix

KernelSU v3 needs a metamodule to mount files from a module's `system/` folder,
so this module **bind-mounts** patched driver files from `post-fs-data.sh`
before Android loads the vendor modules:

- `files/lct_tp.ko` — `init_module()` now calls `init_lct_tp_gesture()`, creating
  `/proc/tp_gesture` at boot.
- `files/nt36532_spi.ko` — `nvt_ts_wakeup_gesture_report()`: the
  `cbnz db_wakeup` check is changed to an unconditional branch, so the detected
  double-click gesture is always reported as a wake key event.
- `files/dt2wctl` — tiny ioctl helper.
- `service.sh` — sends ioctl mode 24 every 2 s so the touch IC keeps detecting
  double taps, and writes `1` to `/proc/tp_gesture`.
- `action.sh` — manual re-arm button.

## Install

Flash the zip in KernelSU → Modules → Install from storage, reboot, then test
double-tap-to-wake. No system partition is modified (bind mounts are undone on
reboot; removing the module restores stock behavior).

Verify after reboot (root shell):

```
ls -l /proc/tp_gesture
dmesg | grep -iE "dt2w-fix|Enabled touch wakeup gesture|Gesture : Double Click"
dmesg | grep "mode:24, value:1"
```
