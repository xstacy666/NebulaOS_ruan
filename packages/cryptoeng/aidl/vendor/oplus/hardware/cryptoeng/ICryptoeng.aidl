package vendor.oplus.hardware.cryptoeng;

@VintfStability
interface ICryptoeng {
    byte[] cryptoeng_invoke_command(in byte[] request);
}