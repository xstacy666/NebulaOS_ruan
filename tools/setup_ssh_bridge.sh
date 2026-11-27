#!/bin/bash
# AntiGravity <-> Tablet Auto-Connect SSH Bridge
set -e

echo "=== 1. Checking SSH keys on AntiGravity host ==="
SSH_DIR="$HOME/.ssh"
mkdir -p "$SSH_DIR"
chmod 700 "$SSH_DIR"

if [ ! -f "$SSH_DIR/id_ed25519" ]; then
    ssh-keygen -t ed25519 -N "" -f "$SSH_DIR/id_ed25519" -C "antigravity-bridge"
fi

PUB_KEY=$(cat "$SSH_DIR/id_ed25519.pub")
echo "Host public key: $PUB_KEY"

echo "=== 2. Creating persistent auto-reconnect daemon script for Tablet ==="
cat << 'DAEMON_EOF' > /data/user/0/ai.antigem/files/home/projects/tab_ssh_agent.sh
#!/system/bin/sh
# Persistent reverse tunnel / ADB daemon on Tablet
HOST_IP="192.168.31.1" # gateway/host fallback
PORT=8022

while true; do
    # Ensure adbd listens on static tcp port 5555
    CURRENT_PORT=$(getprop service.adb.tcp.port)
    if [ "$CURRENT_PORT" != "5555" ]; then
        setprop service.adb.tcp.port 5555
        stop adbd
        start adbd
    fi

    # Check dropbear / sshd or ping host
    sleep 10
done
DAEMON_EOF

echo "Bridge script template created."
