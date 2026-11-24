#!/bin/bash
set -e

CONV_ID="7598b521-2782-40ea-a37d-fb289e9b5705"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKUP_DIR="$REPO_ROOT/docs/session_backup"

# Dynamically locate antigravity CLI directory
if [ -d "$HOME/.gemini/antigravity-cli" ]; then
    CLI_DIR="$HOME/.gemini/antigravity-cli"
elif [ -d "/data/user/0/ai.antigem/files/home/.gemini/antigravity-cli" ]; then
    CLI_DIR="/data/user/0/ai.antigem/files/home/.gemini/antigravity-cli"
elif [ -d "/data/data/ai.antigem/files/home/.gemini/antigravity-cli" ]; then
    CLI_DIR="/data/data/ai.antigem/files/home/.gemini/antigravity-cli"
else
    CLI_DIR="$HOME/.gemini/antigravity-cli"
    mkdir -p "$CLI_DIR"
fi

echo "=========================================================="
echo "  Nebula OS • AntiGem AI Cross-Device Session Sync"
echo "  Conversation ID: $CONV_ID"
echo "  Target App Directory: $CLI_DIR"
echo "=========================================================="
echo ""

# Ensure workspace folder exists
mkdir -p "$HOME/projects"
mkdir -p "$CLI_DIR/brain/$CONV_ID"
mkdir -p "$CLI_DIR/conversations"

BRAIN_TAR="$BACKUP_DIR/brain_session_backup.tar.gz"
CONV_TAR="$BACKUP_DIR/conversation_db_backup.tar.gz"
SUM_DB="$BACKUP_DIR/conversation_summaries.db"
AGY_PB="$BACKUP_DIR/agyhub_summaries_proto.pb"
JET_PB="$BACKUP_DIR/jetbox_summaries_proto.pb"

if [ ! -f "$BRAIN_TAR" ]; then
    echo "ERROR: Brain backup archive not found at $BRAIN_TAR"
    exit 1
fi

echo "-> 1. Stopping background daemons to unlock databases..."
pkill -f "agy --hub" 2>/dev/null || true
pkill -f "agy_ide_bridge" 2>/dev/null || true

echo "-> 2. Restoring Brain session memory (transcripts, artifacts, steps)..."
tar -xzf "$BRAIN_TAR" -C "$CLI_DIR/brain/$CONV_ID"

if [ -f "$CONV_TAR" ]; then
    echo "-> 3. Restoring Conversation Database into app data..."
    tar -xzf "$CONV_TAR" -C "$CLI_DIR/conversations"
fi

if [ -f "$SUM_DB" ]; then
    echo "-> 4. Registering conversation into sidebar index (full protobuf BLOB)..."
    python3 -c "
import sqlite3, os
dest_db = os.path.join('$CLI_DIR', 'conversation_summaries.db')
src_db = '$SUM_DB'
conn = sqlite3.connect(dest_db)
conn.execute('ATTACH DATABASE ? AS src', (src_db,))
conn.execute('CREATE TABLE IF NOT EXISTS conversation_summaries AS SELECT * FROM src.conversation_summaries WHERE 1=0')
conn.execute('INSERT OR REPLACE INTO conversation_summaries SELECT * FROM src.conversation_summaries WHERE conversation_id=\"$CONV_ID\"')
conn.commit()
conn.close()
print('-> Success: conversation_summaries.db updated with full metadata.')
" 2>/dev/null || cp "$SUM_DB" "$CLI_DIR/conversation_summaries.db"
fi

echo "-> 5. Restoring summary proto buffers for instant launcher discovery..."
[ -f "$AGY_PB" ] && [ ! -f "$CLI_DIR/agyhub_summaries_proto.pb" ] && cp "$AGY_PB" "$CLI_DIR/"
[ -f "$JET_PB" ] && [ ! -f "$CLI_DIR/jetbox_summaries_proto.pb" ] && cp "$JET_PB" "$CLI_DIR/"
# Also copy if target has smaller size
if [ -f "$AGY_PB" ] && [ $(stat -c%s "$CLI_DIR/agyhub_summaries_proto.pb" 2>/dev/null || echo 0) -lt 500 ]; then
    cp "$AGY_PB" "$CLI_DIR/"
fi
if [ -f "$JET_PB" ] && [ $(stat -c%s "$CLI_DIR/jetbox_summaries_proto.pb" 2>/dev/null || echo 0) -lt 200 ]; then
    cp "$JET_PB" "$CLI_DIR/"
fi

echo "-> 6. Setting permissions..."
chmod -R 700 "$CLI_DIR/brain/$CONV_ID"
[ -f "$CLI_DIR/conversations/$CONV_ID.db" ] && chmod 600 "$CLI_DIR/conversations/$CONV_ID.db"
[ -f "$CLI_DIR/conversation_summaries.db" ] && chmod 600 "$CLI_DIR/conversation_summaries.db"

echo ""
echo "=========================================================="
echo "SUCCESS: AntiGem AI session fully synced to this device!"
echo "Now restart the AntiGem app on your phone."
echo "Your conversation 'Gain ADB And Root Access' will be right"
echo "there in your chats ready to continue!"
echo "=========================================================="
