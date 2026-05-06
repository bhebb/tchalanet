#!/usr/bin/env python3
from __future__ import annotations

from collections import defaultdict
import hashlib
import json
import os
import re
from pathlib import Path
from typing import Any

ROOT = Path.cwd()

EXCLUDED_DIRS = {
    ".git",
    "node_modules",
    "dist",
    "build",
    "target",
    ".angular",
    ".nx",
    ".venv",
    "venv",
    "__pycache__",
    ".idea",
    ".vscode",
    "coverage",
}

AI_DIRS = {".claude", ".codex", ".copilot", ".agents"}
AI_NAMES = {"AGENTS.md", "CLAUDE.md", "CODEX.md", "COPILOT.md"}

OWNER_RULES = [
    ("apps/tchalanet-web/", "tchalanet-web"),
    ("libs/", "tchalanet-web"),
    ("apps/tchalanet-mobile/", "tchalanet-mobile"),
    ("tchalanet-mobile/", "tchalanet-mobile"),
    ("tchalanet-server/", "tchalanet-server"),
    ("tchalanet-edge-service/", "tchalanet-edge-service"),
    ("tchalanet-infra/", "tchalanet-infra"),
    ("tchalanet-docs/", "tchalanet-docs"),
    ("openspec/", "openspec"),
]

CANONICAL_FILES = {
    "AGENTS.md": "Global AI-agent router and repository policy.",
    "tchalanet-server/AGENTS.md": "Backend component router.",
    "apps/tchalanet-web/AGENTS.md": "Web component router.",
    "tchalanet-mobile/AGENTS.md": "Mobile component router.",
    "tchalanet-edge-service/AGENTS.md": "Edge-service component router.",
    "tchalanet-infra/AGENTS.md": "Infra component router.",
    "tchalanet-docs/AGENTS.md": "Documentation component router.",
}

STALE_PATTERNS = [
    (re.compile(r"\bionic\b", re.IGNORECASE), "mentions legacy Ionic mobile guidance"),
    (re.compile(r"\bcordova\b", re.IGNORECASE), "mentions legacy Cordova guidance"),
    (re.compile(r"\.specify/", re.IGNORECASE), "mentions legacy .specify workflow"),
    (re.compile(r"30-backend\.md|10-non-negociables", re.IGNORECASE), "mentions obsolete OpenSpec pack path/name"),
]

CONFLICT_PATTERNS = [
    (re.compile(r"direct implementation without proposal is forbidden", re.IGNORECASE), "must align with OpenSpec workflow"),
    (re.compile(r"do not create changes in `openspec/`", re.IGNORECASE), "component OpenSpec scoping rule"),
]

STATUS_ORDER = {
    "CANONICAL": 0,
    "TOOL_ROUTER": 1,
    "COMPONENT_SPECIFIC": 2,
    "DUPLICATE": 3,
    "OBSOLETE": 4,
    "ARCHIVE": 5,
    "UNKNOWN": 6,
}

COMPONENT_AGENTS = [
    ("root/global", "AGENTS.md", "Global router, versions, OpenSpec routing, component map."),
    ("backend", "tchalanet-server/AGENTS.md", "Backend commands, docs, OpenSpec workspace, validation."),
    ("web", "apps/tchalanet-web/AGENTS.md", "Angular/Nx commands, docs, OpenSpec workspace, validation."),
    ("mobile", "tchalanet-mobile/AGENTS.md", "Flutter commands, docs, OpenSpec workspace, validation."),
    ("edge", "tchalanet-edge-service/AGENTS.md", "Edge-service commands, docs, OpenSpec workspace, validation."),
    ("infra", "tchalanet-infra/AGENTS.md", "Infra compose/env commands, docs, OpenSpec workspace, validation."),
    ("docs", "tchalanet-docs/AGENTS.md", "MkDocs commands, docs, OpenSpec workspace, validation."),
]


def is_excluded(path: Path) -> bool:
    return any(part in EXCLUDED_DIRS for part in path.parts)


def is_ai_file(path: Path) -> bool:
    if path.name in AI_NAMES:
        return True
    if any(part in AI_DIRS for part in path.parts):
        return True
    return path.name.endswith(".prompt.md")


def owner_for(path: Path) -> str:
    text = path.as_posix()
    for prefix, owner in OWNER_RULES:
        if text.startswith(prefix):
            return owner
    return "root/global"


def kind_for(path: Path) -> str:
    text = path.as_posix()
    if text in CANONICAL_FILES:
        return "agents-router"
    if "/.claude/skills/" in text or text.startswith(".claude/skills/") or text.startswith(".agents/skills/"):
        return "skill"
    if text.startswith(".claude/commands/"):
        return "claude-command"
    if text.startswith(".claude/agents/"):
        return "claude-agent"
    if text.startswith(".github/prompts/") or text.endswith(".prompt.md"):
        return "copilot-prompt"
    if "/CLAUDE.md" in text or text == "CLAUDE.md":
        return "claude-router"
    if text.startswith(".codex/"):
        return "codex-router"
    if text.startswith(".agents/"):
        if text == ".agents/README.md" or re.match(r"^\.agents/[^/]+\.md$", text):
            return "agent-router"
        return "generic-agent"
    return "agent-doc"


def classify(path: Path, owner: str, kind: str) -> tuple[str, str]:
    text = path.as_posix()
    if text in CANONICAL_FILES:
        return "CANONICAL", CANONICAL_FILES[text]
    if "/archive/" in text or "/archived/" in text:
        return "ARCHIVE", "Already archived; keep discoverable until reviewed."
    if owner != "root/global" and path.name in {"AGENTS.md", "CLAUDE.md"}:
        return "COMPONENT_SPECIFIC", "Keep near component; link from global router."
    if kind in {"agent-router", "claude-router", "codex-router", "copilot-prompt", "claude-command", "claude-agent", "skill"}:
        return "TOOL_ROUTER", "Keep tool-specific and short; link to canonical rules."
    return "UNKNOWN", "Review ownership before archiving or converting."


def title_for(text: str) -> str:
    for line in text.splitlines():
        stripped = line.strip()
        if stripped.startswith("# "):
            return stripped[2:].strip()
    return ""


def normalize_words(text: str) -> set[str]:
    words = re.findall(r"[a-z0-9][a-z0-9_-]{3,}", text.lower())
    stopwords = {"this", "that", "with", "from", "pour", "dans", "avec", "shall", "when", "then"}
    return {word for word in words if word not in stopwords}


def overlap_score(left: set[str], right: set[str]) -> float:
    if not left or not right:
        return 0.0
    return len(left & right) / min(len(left), len(right))


def markdown_escape(value: Any) -> str:
    return str(value).replace("|", "\\|").replace("\n", " ")


def status_counts(rows: list[dict[str, Any]]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for row in rows:
        counts[row["classification"]] = counts.get(row["classification"], 0) + 1
    return dict(sorted(counts.items(), key=lambda item: STATUS_ORDER.get(item[0], 99)))


def main() -> None:
    rows: list[dict[str, Any]] = []
    words_by_path: dict[str, set[str]] = {}

    candidate_paths: list[Path] = []
    for dirpath, dirnames, filenames in os.walk(ROOT):
        dirnames[:] = sorted(name for name in dirnames if name not in EXCLUDED_DIRS)
        base = Path(dirpath)
        for filename in sorted(filenames):
            candidate_paths.append(base / filename)

    for path in sorted(candidate_paths):
        rel = path.relative_to(ROOT)
        if is_excluded(rel) or not is_ai_file(rel):
            continue
        text = path.read_text(encoding="utf-8", errors="ignore")
        digest = hashlib.sha256(text.encode("utf-8")).hexdigest()
        owner = owner_for(rel)
        kind = kind_for(rel)
        classification, action = classify(rel, owner, kind)
        stale_hits = [reason for pattern, reason in STALE_PATTERNS if pattern.search(text)]
        conflict_hits = [reason for pattern, reason in CONFLICT_PATTERNS if pattern.search(text)]
        if stale_hits and classification == "UNKNOWN":
            classification = "OBSOLETE"
            action = "archive after review: " + "; ".join(sorted(set(stale_hits)))
        elif stale_hits:
            action += " Review stale mentions: " + "; ".join(sorted(set(stale_hits)))

        row = {
            "path": rel.as_posix(),
            "owner": owner,
            "kind": kind,
            "title": title_for(text),
            "lines": text.count("\n") + (1 if text else 0),
            "sha256": digest,
            "sha256_16": digest[:16],
            "classification": classification,
            "recommended_action": action,
            "stale_mentions": sorted(set(stale_hits)),
            "conflict_mentions": sorted(set(conflict_hits)),
        }
        rows.append(row)
        words_by_path[row["path"]] = normalize_words(text)

    hash_groups: dict[str, list[dict[str, Any]]] = defaultdict(list)
    title_groups: dict[str, list[dict[str, Any]]] = defaultdict(list)
    basename_groups: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in rows:
        hash_groups[row["sha256"]].append(row)
        if row["title"]:
            title_groups[row["title"].lower()].append(row)
        basename_groups[Path(row["path"]).name.lower()].append(row)

    duplicate_findings: list[dict[str, Any]] = []
    duplicate_paths: set[str] = set()

    def add_finding(kind: str, key: str, grouped_rows: list[dict[str, Any]], score: float | None = None, mark: bool = False) -> None:
        if len(grouped_rows) < 2:
            return
        duplicate_findings.append({
            "kind": kind,
            "key": key,
            "score": score,
            "paths": [row["path"] for row in grouped_rows],
        })
        if mark:
            ranked = sorted(grouped_rows, key=lambda row: STATUS_ORDER.get(row["classification"], 99))
            for row in ranked[1:]:
                duplicate_paths.add(row["path"])

    for digest, grouped_rows in hash_groups.items():
        if len(grouped_rows) > 1:
            add_finding("identical content", digest[:16], grouped_rows, 1.0, True)
    for title, grouped_rows in title_groups.items():
        if len(grouped_rows) > 1 and title not in {"tasks"}:
            add_finding("same H1 title", title, grouped_rows)
    for basename, grouped_rows in basename_groups.items():
        if len(grouped_rows) > 1 and basename not in {"agents.md", "claude.md", "skill.md"}:
            add_finding("same filename", basename, grouped_rows)

    comparable = [row for row in rows if row["lines"] >= 25]
    for index, left in enumerate(comparable):
        for right in comparable[index + 1:]:
            if left["sha256"] == right["sha256"]:
                continue
            score = overlap_score(words_by_path[left["path"]], words_by_path[right["path"]])
            if score >= 0.86:
                add_finding("high textual overlap", f"{left['path']} <> {right['path']}", [left, right], score, True)

    for row in rows:
        if row["path"] in duplicate_paths and row["classification"] not in {"CANONICAL", "ARCHIVE"}:
            row["classification"] = "DUPLICATE"
            row["recommended_action"] = "convert to short router or archive after canonical source is confirmed"

    rows.sort(key=lambda row: (row["owner"], row["path"]))

    out_dir = ROOT / "build"
    out_dir.mkdir(exist_ok=True)
    target = ROOT / "tchalanet-docs" / "docs" / "99-reference"
    target.mkdir(parents=True, exist_ok=True)

    payload = {
        "generated_by": "scripts/docs/inventory-ai-agent-files.py",
        "excluded_dirs": sorted(EXCLUDED_DIRS),
        "total_ai_agent_files": len(rows),
        "counts_by_owner": dict(sorted((owner, sum(1 for row in rows if row["owner"] == owner)) for owner in {row["owner"] for row in rows})),
        "counts_by_classification": status_counts(rows),
        "component_agents": [
            {"component": component, "path": path, "purpose": purpose}
            for component, path, purpose in COMPONENT_AGENTS
        ],
        "duplicates": duplicate_findings,
        "files": rows,
    }
    (out_dir / "ai-agent-files-inventory.json").write_text(json.dumps(payload, indent=2), encoding="utf-8")

    md = [
        "# AI-Agent Files Inventory",
        "",
        "> Generated by `scripts/docs/inventory-ai-agent-files.py`. Do not edit generated tables by hand.",
        "",
        "This inventory scans AI-agent guidance files and excludes generated/vendor directories.",
        "",
        "## Counts by classification",
        "",
        "| Classification | Files |",
        "| --- | ---: |",
    ]
    for classification, count in status_counts(rows).items():
        md.append(f"| `{classification}` | {count} |")
    md.extend(["", "## Files", "", "| Path | Owner | Kind | Title | Lines | Classification | Action |", "| --- | --- | --- | --- | ---: | --- | --- |"])
    for row in rows:
        md.append(
            f"| `{row['path']}` | {row['owner']} | {row['kind']} | {markdown_escape(row['title'])} | "
            f"{row['lines']} | {row['classification']} | {markdown_escape(row['recommended_action'])} |"
        )
    (target / "ai-agent-files-inventory.md").write_text("\n".join(md) + "\n", encoding="utf-8")

    dup_md = [
        "# AI-Agent Duplicate And Obsolete Report",
        "",
        "This report is heuristic. It identifies candidates for review; it does not authorize deletion.",
        "",
        "## Stale Or Conflicting Mentions",
        "",
        "| Path | Classification | Mentions |",
        "| --- | --- | --- |",
    ]
    for row in rows:
        mentions = row["stale_mentions"] + row["conflict_mentions"]
        if mentions:
            dup_md.append(f"| `{row['path']}` | {row['classification']} | {markdown_escape('; '.join(mentions))} |")
    dup_md.extend(["", "## Duplicate Candidates", "", f"Total findings: {len(duplicate_findings)}", ""])
    if duplicate_findings:
        dup_md.append("| Kind | Key / Score | Files |")
        dup_md.append("| --- | --- | --- |")
        for finding in duplicate_findings:
            score = "" if finding["score"] is None else f" score={finding['score']:.2f}"
            files = "<br>".join(f"`{markdown_escape(path)}`" for path in finding["paths"])
            dup_md.append(f"| {markdown_escape(finding['kind'])} | `{markdown_escape(finding['key'])}`{score} | {files} |")
    else:
        dup_md.append("No duplicate candidates were detected.")
    (target / "ai-agent-duplicates.md").write_text("\n".join(dup_md) + "\n", encoding="utf-8")

    map_md = [
        "# Component AGENTS.md Map",
        "",
        "Component-specific details belong in component agent files. Root `AGENTS.md` should stay a short router.",
        "",
        "| Component | Agent file | Purpose |",
        "| --- | --- | --- |",
    ]
    for component, path, purpose in COMPONENT_AGENTS:
        map_md.append(f"| {component} | `{path}` | {purpose} |")
    (target / "component-agents-map.md").write_text("\n".join(map_md) + "\n", encoding="utf-8")

    cleanup_md = [
        "# AI-Agent Cleanup And Archival Plan",
        "",
        "No AI-agent file should be deleted directly. Archive first, then delete only in a follow-up reviewed change.",
        "",
        "| Action | Classification | Meaning | Count |",
        "| --- | --- | --- | ---: |",
    ]
    plan_rows = [
        ("keep", "CANONICAL", "Canonical router or source of truth."),
        ("keep short", "TOOL_ROUTER", "Tool-specific entrypoint; link to canonical rules."),
        ("keep near component", "COMPONENT_SPECIFIC", "Component-owned detail."),
        ("convert or archive after review", "DUPLICATE", "Likely duplicate content."),
        ("archive after review", "OBSOLETE", "Stale instruction candidate."),
        ("keep archived", "ARCHIVE", "Already archived."),
        ("review", "UNKNOWN", "Ownership/action unclear."),
    ]
    counted = status_counts(rows)
    for action, classification, meaning in plan_rows:
        cleanup_md.append(f"| {action} | `{classification}` | {meaning} | {counted.get(classification, 0)} |")
    cleanup_md.extend(["", "## Review Queue", "", "| Path | Classification | Recommended action |", "| --- | --- | --- |"])
    for row in rows:
        if row["classification"] in {"DUPLICATE", "OBSOLETE", "UNKNOWN"}:
            cleanup_md.append(f"| `{row['path']}` | {row['classification']} | {markdown_escape(row['recommended_action'])} |")
    (target / "ai-agent-cleanup-plan.md").write_text("\n".join(cleanup_md) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
