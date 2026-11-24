#!/bin/bash
set -e

CONV_ID="7598b521-2782-40ea-a37d-fb289e9b5705"
BASE_DIR="/data/user/0/ai.antigem/files/home/.gemini/antigravity-cli/brain/$CONV_ID"
BACKUP_ARCHIVE="/data/user/0/ai.antigem/files/home/projects/NebulaOS_ruan/docs/session_backup/brain_session_backup.tar.gz"

echo "=========================================================="
echo "  Nebula OS • Antigravity AI Session Restore Tool"
echo "  Conversation ID: $CONV_ID"
echo "=========================================================="
echo ""

if [ ! -f "$BACKUP_ARCHIVE" ]; then
    echo "ERROR: Backup archive not found at $BACKUP_ARCHIVE"
    exit 1
fi

echo "-> Creating target session directories..."
mkdir -p "$BASE_DIR"

echo "-> Restoring full conversation memory, transcripts and artifacts..."
tar -xzf "$BACKUP_ARCHIVE" -C "$BASE_DIR"

echo "-> Setting correct permissions..."
chmod -R 700 "$BASE_DIR"

echo ""
echo "SUCCESS: Antigravity AI conversation session has been restored!"
echo "Open AntiGem now and your complete conversation history will be right there waiting for you."
echo "=========================================================="
