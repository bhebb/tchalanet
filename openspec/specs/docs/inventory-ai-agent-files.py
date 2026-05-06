#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path.cwd()
EXCLUDED_DIRS = {".git", "node_modules", "dist", "build", "target", ".angular", ".nx", ".venv", "venv"}
AI_DIRS = {".claude", ".codex", ".copilot", ".agents"}
AI_NAMES = {"AGENTS.md", "CLAUDE.md", "CODEX.md", "COPILOT.md"}

def is_excluded(path: Path) -> bool:
    return any(part in EXCLUDED_DIRS for part in path.parts)

def is_ai_file(path: Path) -> bool:
    if path.name in AI_NAMES:
        return True
    if any(part in AI_DIRS for part in path.parts):
        return True
    if path.name.endswith(".prompt.md"):
        return True
    return False

def owner_for(path: Path) -> str:
    first = path.parts[0] if path.parts else "root"
    known = {
        "tchalanet-server", "tchalanet-web", "tchalanet-mobile",
        "tchalanet-edge-service", "tchalanet-infra", "tchalanet-docs",
        "openspec"
    }
    return first if first in known else "root"

def main() -> None:
    rows = []
    for path in sorted(ROOT.rglob("*")):
        if not path.is_file():
            continue
        rel = path.relative_to(ROOT)
        if is_excluded(rel):
            continue
        if not is_ai_file(rel):
            continue
        text = path.read_text(encoding="utf-8", errors="ignore")
        rows.append({
            "path": str(rel),
            "owner": owner_for(rel),
            "lines": text.count("\n") + (1 if text else 0),
            "classification": "UNKNOWN",
            "recommended_action": "REVIEW",
        })

    out_dir = ROOT / "build"
    out_dir.mkdir(exist_ok=True)
    (out_dir / "ai-agent-files-inventory.json").write_text(json.dumps(rows, indent=2), encoding="utf-8")

    md = ["# AI Agent Files Inventory", ""]
    md.append("| Path | Owner | Lines | Classification | Action |")
    md.append("| --- | --- | ---: | --- | --- |")
    for r in rows:
        md.append(f"| `{r['path']}` | {r['owner']} | {r['lines']} | {r['classification']} | {r['recommended_action']} |")

    target = ROOT / "tchalanet-docs" / "docs" / "99-reference"
    target.mkdir(parents=True, exist_ok=True)
    (target / "ai-agent-files-inventory.md").write_text("\n".join(md) + "\n", encoding="utf-8")

if __name__ == "__main__":
    main()
