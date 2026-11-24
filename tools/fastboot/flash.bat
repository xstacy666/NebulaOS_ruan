@echo off
echo ========================================================
echo   Nebula OS (ColorOS 17) Fastboot ROM
echo   Target: Redmi Pad Pro 5G (ruan) ^& Wi-Fi (dizi)
echo   Maintainers: Opium ^| Team Nebula
echo   Credits: @Noble_actual117, @GAURA_140110, @NotYoursAditya, @Gignigga
echo ========================================================
echo.

for %%p in (boot vendor_boot init_boot dtbo) do (
    if exist images\%%p.img (
        echo Flashing %%p...
        fastboot flash %%p_a images\%%p.img
        fastboot flash %%p_b images\%%p.img
    )
)

for %%v in (vbmeta vbmeta_system vbmeta_vendor) do (
    if exist images\%%v.img (
        echo Flashing %%v with disabled verity...
        fastboot --disable-verity --disable-verification flash %%v_a images\%%v.img
        fastboot --disable-verity --disable-verification flash %%v_b images\%%v.img
    )
)

if exist images\super.img (
    echo Flashing super partition...
    fastboot flash super images\super.img
)

echo Wiping userdata for clean boot...
fastboot -w
echo Rebooting system to Nebula OS...
fastboot reboot
echo.
echo Flashing completed successfully! Welcome to Nebula OS!
pause
