import zlib
import hashlib
import struct
import zipfile
import sys
import os

def patch_classes8(dex_bytes: bytearray) -> bytearray:
    # Pattern 1: LinkAccountActivity.onStop() aborting with RESULT_CANCELED
    # invoke-super onStop (6f 10 72 09 03 00)
    # iget-object (54 30 1a f4)
    # if-eqz (38 00 05 00)
    # invoke-virtual detachRegister (6e 10 26 85 00 00)
    # invoke-virtual isChangingConfigurations (6e 10 80 00 03 00) -> replace with return-void (0e 00 00 00 00 00)
    p1_orig = bytes.fromhex('6f107209030054301af4380005006e10268500006e1080000300')
    p1_patch = bytes.fromhex('6f107209030054301af4380005006e10268500000e0000000000')

    idx1 = dex_bytes.find(p1_orig)
    if idx1 != -1:
        print(f"[+] Found Pattern 1 at 0x{idx1:x}, applying return-void patch...")
        dex_bytes[idx1:idx1+len(p1_orig)] = p1_patch
    elif dex_bytes.find(p1_patch) != -1:
        print("[*] Pattern 1 already patched!")
    else:
        print("[-] Warning: Pattern 1 not found in dex!")

    # Pattern 2: PrivacyBindAccountController premature finish
    # if-eqz v5, 005a: 38 05 0d 00 -> replace with goto +000d (28 0d 00 00)
    # iget-object mActivity: 54 45 a1 f4
    # setResult: 6e 20 98 00 15 00
    # iget-object: 54 44 a1 f4
    # finish: 6e 10 6d 00 04 00
    p2_orig = bytes.fromhex('38050d005445a1f46e20980015005444a1f46e106d000400')
    p2_patch = bytes.fromhex('280d00005445a1f46e20980015005444a1f46e106d000400')

    idx2 = dex_bytes.find(p2_orig)
    if idx2 != -1:
        print(f"[+] Found Pattern 2 at 0x{idx2:x}, applying unconditional goto...")
        dex_bytes[idx2:idx2+len(p2_orig)] = p2_patch
    elif dex_bytes.find(p2_patch) != -1:
        print("[*] Pattern 2 already patched!")
    else:
        print("[-] Warning: Pattern 2 not found in dex!")

    # Recompute SHA-1 checksum (offset 12..32)
    sha1 = hashlib.sha1(dex_bytes[32:]).digest()
    dex_bytes[12:32] = sha1

    # Recompute Adler-32 checksum (offset 8..12)
    adler = zlib.adler32(dex_bytes[12:]) & 0xffffffff
    dex_bytes[8:12] = struct.pack('<I', adler)
    print(f"[+] Dex checksums updated (Adler32: 0x{adler:08x})")

    return dex_bytes

def patch_settings_apk(in_apk: str, out_apk: str):
    print(f"[*] Patching Settings APK: {in_apk} -> {out_apk}")
    with zipfile.ZipFile(in_apk, 'r') as zin:
        with zipfile.ZipFile(out_apk, 'w') as zout:
            for item in zin.infolist():
                data = zin.read(item.filename)
                if item.filename == 'classes8.dex':
                    print("[*] Processing classes8.dex...")
                    data = patch_classes8(bytearray(data))
                zout.writestr(item, data, compress_type=item.compress_type)
    print("[+] Done! Patched APK created successfully.")

if __name__ == '__main__':
    if len(sys.argv) < 3:
        print(f"Usage: {sys.argv[0]} <input_Settings.apk> <output_Settings.apk>")
        sys.exit(1)
    patch_settings_apk(sys.argv[1], sys.argv[2])
