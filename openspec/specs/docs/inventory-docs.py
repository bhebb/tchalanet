#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import os
import re
from pathlib import Path
from typing import Iterable

ROOT = Path.cwd()

EXCLUDED_DIRS = {
    ".git", "node_modules", "dist", "build", "target", ".angular", ".nx",
    ".venv", "venv", "__pycache__", ".idea", ".vscode"
}

COMPONENT_PREFIXES = [
    "openspec",
    "tchalanet-server",
    "tchalanet-web",
    "tchalanet-mobile",
    "tchalanet-edge-service",
    "tchalanet-infra",
    "tchalanet-docs",
]

def is_excluded(path: Path) -> bool:
    return any(part in EXCLUDED_DIRS for part in path.parts)

def owner_for(path: Path) -> str:
    parts = path.parts
    if not parts:
        return "root"
    for prefix in COMPONENT_PREFIXES:
        if parts[0] == prefix:
            return prefix
    return "root"

def title_for(text: str) -> str:
    for line in text.splitlines():
        if line.startswith("# "):
            return line[2:].strip()
    return ""

def doc_type_for(path: Path) -> str:
    name = path.name.lower()
    s = str(path).lower()
    if "openspec" in s:
        return "openspec"
    if "domain_" in name or name.startswith("domain-"):
        return "domain"
    if "agent" in name or ".claude" in s or ".codex" in s or ".copilot" in s:
        return "agent"
    if "readme" == name:
        return "readme"
    if "decision" in s or "adr" in s:
        return "decision"
    if "audit" in s:
        return "audit"
    return "doc"

def main() -> None:
    rows = []
    for path in sorted(ROOT.rglob("*.md")):
        rel = path.relative_to(ROOT)
        if is_excluded(rel):
            continue
        try:
            text = path.read_text(encoding="utf-8", errors="ignore")
        except Exception:
            text = ""
        h = hashlib.sha256(text.encode("utf-8")).hexdigest()[:16]
        rows.append({
            "path": str(rel),
            "owner": owner_for(rel),
            "doc_type": doc_type_for(rel),
            "title": title_for(text),
            "lines": text.count("\n") + (1 if text else 0),
            "sha256_16": h,
            "status": "UNKNOWN",
            "recommended_action": "REVIEW",
        })

    out_dir = ROOT / "build"
    out_dir.mkdir(exist_ok=True)
    (out_dir / "docs-inventory.json").write_text(json.dumps(rows, indent=2), encoding="utf-8")

    counts = {}
    for r in rows:
        counts[r["owner"]] = counts.get(r["owner"], 0) + 1

    md = ["# Docs Inventory", "", "## Counts by owner", ""]
    md.append("| Owner | Markdown files |")
    md.append("| --- | ---: |")
    for owner, count in sorted(counts.items()):
        md.append(f"| `{owner}` | {count} |")

    md.extend(["", "## Files", ""])
    md.append("| Path | Owner | Type | Title | Lines | Status | Action |")
    md.append("| --- | --- | --- | --- | ---: | --- | --- |")
    for r in rows:
        title = r["title"].replace("|", "\\|")
        md.append(f"| `{r['path']}` | {r['owner']} | {r['doc_type']} | {title} | {r['lines']} | {r['status']} | {r['recommended_action']} |")

    target = ROOT / "tchalanet-docs" / "docs" / "99-reference"
    target.mkdir(parents=True, exist_ok=True)
    (target / "docs-inventory.md").write_text("\n".join(md) + "\n", encoding="utf-8")

if __name__ == "__main__":
    main()
