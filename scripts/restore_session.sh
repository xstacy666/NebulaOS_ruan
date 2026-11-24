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
echo "  Nebula OS • Antigravity AI Cross-Device Session Sync"
echo "  Conversation ID: $CONV_ID"
echo "  Target App Directory: $CLI_DIR"
echo "=========================================================="
echo ""

BRAIN_TAR="$BACKUP_DIR/brain_session_backup.tar.gz"
CONV_TAR="$BACKUP_DIR/conversation_db_backup.tar.gz"
META_JSON="$BACKUP_DIR/summary_meta.json"

if [ ! -f "$BRAIN_TAR" ]; then
    echo "ERROR: Brain backup archive not found at $BRAIN_TAR"
    exit 1
fi

echo "-> 1. Restoring Brain session memory (transcripts, artifacts, steps)..."
mkdir -p "$CLI_DIR/brain/$CONV_ID"
tar -xzf "$BRAIN_TAR" -C "$CLI_DIR/brain/$CONV_ID"

if [ -f "$CONV_TAR" ]; then
    echo "-> 2. Restoring Conversation Database into app data..."
    mkdir -p "$CLI_DIR/conversations"
    tar -xzf "$CONV_TAR" -C "$CLI_DIR/conversations"
fi

if [ -f "$META_JSON" ]; then
    echo "-> 3. Registering conversation into sidebar index..."
    python3 -c "
import sqlite3, json, os

db_path = os.path.join('$CLI_DIR', 'conversation_summaries.db')
if not os.path.exists(db_path):
    conn = sqlite3.connect(db_path)
    conn.execute('''CREATE TABLE IF NOT EXISTS conversation_summaries (
        conversation_id TEXT PRIMARY KEY,
        title TEXT,
        preview TEXT,
        step_count INTEGER,
        last_modified_time datetime,
        workspace_uris TEXT,
        status TEXT,
        source TEXT DEFAULT '',
        project_id TEXT DEFAULT '',
        agent_name TEXT DEFAULT '',
        parent_conversation_id TEXT DEFAULT '',
        nesting_depth INTEGER DEFAULT 0,
        battle_id TEXT DEFAULT '',
        winning_conversation_id TEXT DEFAULT '',
        not_fully_idle NUMERIC DEFAULT 0,
        killed NUMERIC DEFAULT 0,
        last_user_input_time datetime,
        last_user_input_step_index INTEGER DEFAULT -1,
        app_data_dir TEXT DEFAULT 'antigravity-cli',
        raw_summary BLOB,
        group_id TEXT DEFAULT ''
    )''')
else:
    conn = sqlite3.connect(db_path)

with open('$META_JSON', 'r') as f:
    meta = json.load(f)

conn.execute('''INSERT OR REPLACE INTO conversation_summaries 
    (conversation_id, title, preview, step_count, last_modified_time, workspace_uris, status, app_data_dir)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)''', (
    meta['conversation_id'],
    meta['title'],
    meta['preview'],
    meta['step_count'],
    meta['last_modified_time'],
    meta['workspace_uris'],
    meta['status'],
    meta['app_data_dir']
))
conn.commit()
conn.close()
print('Registered successfully in conversation_summaries.db')
" 2>/dev/null || echo "Note: Python index registration skipped (conversation will still load directly)."
fi

echo "-> 4. Applying secure permissions..."
chmod -R 700 "$CLI_DIR/brain/$CONV_ID"
[ -f "$CLI_DIR/conversations/$CONV_ID.db" ] && chmod 600 "$CLI_DIR/conversations/$CONV_ID.db"

echo ""
echo "=========================================================="
echo "SUCCESS: AntiGem AI session fully synced to this device!"
echo "Open AntiGem now — 'Gain ADB And Root Access' will be right"
echo "in your chats, with every thought, artifact, and message!"
echo "=========================================================="
