#!/system/bin/sh
# fix_permissions.sh
# Fixes file and directory permissions on an extracted Android root/system/system_ext tree.
# CRITICAL: If files are copied from sdcard or extracted without root, they inherit 0660/0600
# permissions, causing SystemServer & SystemUI to crash with SecurityException at the boot logo!

set -eu

if [ "$#" -lt 1 ]; then
    echo "Usage: $0 <path_to_extracted_tree_1> [path_to_tree_2 ...]"
    exit 1
fi

for TREE in "$@"; do
    if [ ! -d "$TREE" ]; then
        echo "Error: Directory $TREE does not exist."
        continue
    fi

    echo "=== Fixing permissions on $TREE ==="
    chown -R 0:0 "$TREE"
    find "$TREE" -type d -exec chmod 755 {} +
    find "$TREE" -type f -exec chmod 644 {} +

    for b in "$TREE/bin" "$TREE/xbin" "$TREE/system/bin" "$TREE/system/xbin"; do
        if [ -d "$b" ]; then
            echo "  Setting executables in $b to 0755..."
            find "$b" -type f -exec chmod 755 {} +
        fi
    done
    echo "  Done for $TREE."
done

echo "All trees have been restored to strict Android production permissions (0755 / 0644)."
