#!/usr/bin/env python3
import json
import os
import re

LOG_FILE = '/data/user/0/ai.antigem/files/home/.gemini/antigravity-cli/brain/7598b521-2782-40ea-a37d-fb289e9b5705/.system_generated/logs/transcript.jsonl'
OUT_MD = '/data/user/0/ai.antigem/files/home/projects/NebulaOS_ruan/docs/FULL_CONVERSATION_HISTORY.md'
os.makedirs(os.path.dirname(OUT_MD), exist_ok=True)

print(f"Parsing transcript from {LOG_FILE}...")

user_turns = 0
agent_turns = 0

with open(LOG_FILE, 'r', encoding='utf-8', errors='ignore') as f, open(OUT_MD, 'w', encoding='utf-8') as out:
    out.write('# Nebula OS (ColorOS 17) Port - Complete Conversation & Engineering History\n\n')
    out.write('**Repository**: [xstacy666/NebulaOS_ruan](https://github.com/xstacy666/NebulaOS_ruan)\n\n')
    out.write('**Target Devices**: Redmi Pad Pro 5G (`ruan`) & Redmi Pad Pro Wi-Fi (`dizi`) / Snapdragon® 7s Gen 2 (SM7435-AB)\n\n')
    out.write('**Maintainers**: Øpium | Team Nebula (@Noble_actual117, @GAURA_140110, @NotYoursAditya, @Gignigga)\n\n')
    out.write('**Archived At**: 2026-11-25\n\n')
    out.write('---\n\n')

    for line in f:
        line = line.strip()
        if not line:
            continue
        try:
            entry = json.loads(line)
        except Exception:
            continue
        
        src = entry.get('source', '')
        content = entry.get('content', '')
        ts = entry.get('created_at', '')

        if src.startswith('USER'):
            user_turns += 1
            clean = re.sub(r'</?USER_REQUEST>', '', content).strip()
            if '<CONTEXT_SUMMARY>' in clean:
                clean = clean.split('</CONTEXT_SUMMARY>')[-1].strip()
            clean = re.sub(r'<ADDITIONAL_METADATA>[\s\S]*?</ADDITIONAL_METADATA>', '', clean).strip()
            if clean:
                out.write(f'## 👤 User (Request #{user_turns} • {ts})\n\n> {clean}\n\n')

        elif src == 'MODEL' and content and content.strip():
            # Filter out internal tool execution stdout lines
            if content.startswith('Created At:') or content.startswith('Command exited with code') or content.startswith('Tool is running as a background task'):
                continue
            if 'The tool\'s output was truncated' in content:
                continue
            agent_turns += 1
            out.write(f'## 🤖 Antigravity AI (Turn #{agent_turns} • {ts})\n\n{content.strip()}\n\n---\n\n')

print(f"Successfully generated {OUT_MD} with {user_turns} user turns and {agent_turns} assistant responses!")
