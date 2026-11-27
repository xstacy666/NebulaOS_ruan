#!/usr/bin/env python3
"""
repair_super_metadata.py
Repairs Android LpMetadata partition descriptor structs in super.img.

CRITICAL DISCOVERY:
The Android C-struct LpMetadataPartition is laid out as:
    struct LpMetadataPartition {
        char name[36];              // offset 0..36
        uint32_t attributes;        // offset 36..40  <-- 4 bytes!
        uint32_t first_extent_index;// offset 40..44
        uint32_t num_extents;       // offset 44..48
        uint32_t group_index;       // offset 48..52
    };

Many custom unpackers/repackers mistakenly unpack offset 36 as '<III', omitting
'attributes' and shifting 'first_extent_index' into offset 40 with a value of 1.
This causes 'system_a' to map into Extent 1 (system_ext) instead of Extent 0,
leaving system_a completely unmapped and triggering a bootloop!

This script inspects and repairs all 6 metadata slots.
"""

import sys, os, struct

def repair_metadata(file_path):
    with open(file_path, "r+b") as f:
        data = f.read(1024 * 1024) # Read first 1MB metadata block
        idx = 0
        fixed = 0
        while True:
            pos = data.find(b"0PLA", idx)
            if pos == -1:
                break
            
            hdr = data[pos:pos+128]
            magic, maj, min_v, hdr_size = struct.unpack("<4sHHI", hdr[:12])
            part_off, num_parts, part_size = struct.unpack("<III", hdr[80:92])
            ext_off, num_exts, ext_size = struct.unpack("<III", hdr[92:104])
            
            print(f"[*] Found LP header at offset 0x{pos:x}: parts={num_parts}, exts={num_exts}")
            
            # Verify and fix partition table
            p_base = pos + hdr_size + part_off
            for i in range(num_parts):
                p_pos = p_base + i * part_size
                pdata = data[p_pos:p_pos+part_size]
                name = pdata[:36].split(b"\x00")[0].decode("utf-8", errors="ignore")
                attrs, first_ext, n_ext, grp = struct.unpack("<IIII", pdata[36:52])
                
                # Check for known partition mapping
                expected_ext = None
                if name == "system_a": expected_ext = 0
                elif name == "system_ext_a": expected_ext = 1
                elif name == "product_a": expected_ext = 2
                elif name == "vendor_a": expected_ext = 3
                elif name == "odm_a": expected_ext = 4
                elif name == "vendor_dlkm_a": expected_ext = 5
                
                if expected_ext is not None and first_ext != expected_ext:
                    print(f"    [!] Fixing {name}: first_ext changed from {first_ext} -> {expected_ext}")
                    new_bytes = struct.pack("<IIII", attrs, expected_ext, n_ext, grp)
                    data = data[:p_pos+36] + new_bytes + data[p_pos+52:]
                    fixed += 1
            idx = pos + 4
        
        if fixed > 0:
            f.seek(0)
            f.write(data)
            print(f"[+] Successfully repaired {fixed} partition descriptor entries!")
        else:
            print("[+] All partition descriptor extents are already correct.")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 repair_super_metadata.py <super.img_or_block_device>")
        sys.exit(1)
    repair_metadata(sys.argv[1])
