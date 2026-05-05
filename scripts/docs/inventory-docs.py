#!/usr/bin/env python3
from __future__ import annotations

from collections import defaultdict
import hashlib
import json
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
    ".gradle",
    ".mvn",
}

STATUS_ORDER = {
    "CANONICAL": 0,
    "SUMMARY": 1,
    "LINK_ONLY": 2,
    "DUPLICATE": 3,
    "ARCHIVE": 4,
    "OBSOLETE": 5,
    "DELETE_LATER": 6,
    "UNKNOWN": 7,
}

OBSOLETE_PATTERNS = [
    (re.compile(r"\bionic\b", re.IGNORECASE), "mentions legacy Ionic mobile workflow"),
    (re.compile(r"\bcordova\b", re.IGNORECASE), "mentions legacy Cordova workflow"),
    (re.compile(r"\bold[-_ ]?quick[-_ ]?start\b", re.IGNORECASE), "looks like an old quick-start doc"),
    (re.compile(r"\blegacy\b", re.IGNORECASE), "mentions legacy workflow"),
]

CANONICAL_PATHS = {
    "AGENTS.md": ("root/global", "agent-policy", "CANONICAL", "canonical source of truth for AI and contributor rules"),
    "VERSIONS.md": ("root/global", "versions", "CANONICAL", "canonical runtime and tool version registry"),
    "DOCUMENTATION.md": ("root/global", "documentation-hub", "CANONICAL", "canonical documentation hub"),
    "openspec/project.md": ("openspec", "openspec-project", "CANONICAL", "global OpenSpec router"),
    "openspec/context/00-index.md": ("openspec", "openspec-context", "CANONICAL", "global context-pack index"),
    "openspec/context/05-version-guard.md": ("openspec", "openspec-context", "CANONICAL", "version guard context pack"),
    "openspec/context/10-non-negotiables.md": ("openspec", "openspec-context", "CANONICAL", "global non-negotiables context pack"),
    "tchalanet-docs/docs/00-guidelines/doc-policy.md": ("tchalanet-docs", "doc-policy", "CANONICAL", "published documentation policy"),
    "tchalanet-docs/docs/03-adr/index.md": ("tchalanet-docs", "adr-index", "SUMMARY", "published ADR index"),
}

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

OWNERSHIP_MAP = [
    ("Runtime and tool versions", "VERSIONS.md", "Keep canonical at repository root; link from MkDocs."),
    ("Global AI/contributor policy", "AGENTS.md", "Keep canonical at repository root; link from MkDocs and agent indexes."),
    ("Documentation policy", "tchalanet-docs/docs/00-guidelines/doc-policy.md", "Keep published policy in MkDocs."),
    ("Global OpenSpec routing", "openspec/context/*.md", "Keep light; do not copy component details into global context."),
    ("Component OpenSpec workspaces", "<component>/openspec/", "Keep close to the component and link from MkDocs."),
    ("Backend architecture and conventions", "tchalanet-server/docs/", "Keep implementation detail near backend code; summarize/link from MkDocs."),
    ("Backend domain rules", "tchalanet-server/src/**/DOMAIN_*.md", "Keep domain truth near code; summarize/link from MkDocs."),
    ("Backend feature docs", "tchalanet-server/src/**/FEATURE_*.md", "Keep feature implementation detail near code; summarize/link from MkDocs."),
    ("Web app and library docs", "apps/tchalanet-web/ and libs/**/README.md", "Keep near frontend code; link from MkDocs app pages."),
    ("Mobile app docs", "tchalanet-mobile/ and apps/tchalanet-mobile/", "Keep near mobile code; link from MkDocs app pages."),
    ("Infra operational docs", "tchalanet-infra/docs/", "Keep operational detail near infra; link from MkDocs operations pages."),
    ("Edge service docs", "tchalanet-edge-service/docs/ and README.md", "Keep service detail near edge project; link from MkDocs app pages."),
    ("Published product and architecture portal", "tchalanet-docs/docs/", "Own curated summaries, indexes, links, ADRs, and generated reports."),
]


def is_excluded(path: Path) -> bool:
    return any(part in EXCLUDED_DIRS for part in path.parts)


def owner_for(path: Path) -> str:
    text = path.as_posix()
    for prefix, owner in OWNER_RULES:
        if text.startswith(prefix):
            return owner
    return "root/global"


def title_for(text: str) -> str:
    for line in text.splitlines():
        stripped = line.strip()
        if stripped.startswith("# "):
            return stripped[2:].strip()
    return ""


def doc_type_for(path: Path) -> str:
    name = path.name.lower()
    text = path.as_posix().lower()
    if path.as_posix() in CANONICAL_PATHS:
        return CANONICAL_PATHS[path.as_posix()][1]
    if "openspec" in text:
        return "openspec"
    if name.startswith("domain_") or name.startswith("domain-") or "/domain_" in text:
        return "domain"
    if name.startswith("feature_") or name.startswith("feature-") or "/feature_" in text:
        return "feature"
    if "agent" in name or ".claude" in text or ".codex" in text or ".copilot" in text or ".agents" in text:
        return "agent"
    if name == "readme.md":
        return "readme"
    if "decision" in text or "adr" in text:
        return "decision"
    if "audit" in text:
        return "audit"
    if "convention" in text:
        return "convention"
    if "quick" in name or "start" in name or "setup" in name:
        return "quickstart"
    if "policy" in name:
        return "policy"
    return "doc"


def canonical_source_for(path: Path, owner: str, doc_type: str) -> str:
    text = path.as_posix()
    if text in CANONICAL_PATHS:
        return text
    if doc_type == "domain":
        return "tchalanet-server/src/**/DOMAIN_*.md"
    if doc_type == "feature":
        return "tchalanet-server/src/**/FEATURE_*.md"
    if doc_type == "decision":
        return "tchalanet-docs/docs/03-adr/"
    if doc_type == "convention" and owner == "tchalanet-server":
        return "tchalanet-server/docs/conventions/"
    if doc_type == "openspec":
        return "component/global openspec workspace owning the change"
    if doc_type == "agent":
        return "AGENTS.md plus scoped agent files"
    if owner == "tchalanet-docs":
        return "tchalanet-docs/docs/"
    if owner == "tchalanet-infra":
        return "tchalanet-infra/docs/"
    if owner == "tchalanet-edge-service":
        return "tchalanet-edge-service/"
    if owner == "tchalanet-web":
        return "apps/tchalanet-web/ and libs/**/README.md"
    if owner == "tchalanet-mobile":
        return "tchalanet-mobile/ and apps/tchalanet-mobile/"
    if owner == "tchalanet-server":
        return "tchalanet-server/docs/ or near-code docs"
    return "UNKNOWN"


def classify(path: Path, owner: str, doc_type: str) -> tuple[str, str]:
    text = path.as_posix()
    if text in CANONICAL_PATHS:
        return CANONICAL_PATHS[text][2], CANONICAL_PATHS[text][3]
    if "/archived/" in text or "/archive/" in text:
        return "ARCHIVE", "keep archived; review before deletion"
    if doc_type in {"domain", "feature", "convention"}:
        return "CANONICAL", "keep near code and link from MkDocs"
    if doc_type == "openspec":
        if "/changes/" in text:
            return "LINK_ONLY", "active/change documentation; link from OpenSpec map"
        return "SUMMARY", "keep global/component OpenSpec context light"
    if owner == "tchalanet-docs":
        return "SUMMARY", "published portal page; keep curated and avoid copying component docs"
    if doc_type in {"readme", "quickstart"}:
        return "LINK_ONLY", "component entrypoint; link from MkDocs"
    return "UNKNOWN", "review ownership before moving or deleting"


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
        counts[row["status"]] = counts.get(row["status"], 0) + 1
    return dict(sorted(counts.items(), key=lambda item: STATUS_ORDER.get(item[0], 99)))


def main() -> None:
    rows: list[dict[str, Any]] = []
    words_by_path: dict[str, set[str]] = {}

    for path in sorted(ROOT.rglob("*.md")):
        rel = path.relative_to(ROOT)
        if is_excluded(rel):
            continue
        try:
            text = path.read_text(encoding="utf-8", errors="ignore")
        except Exception:
            text = ""

        digest = hashlib.sha256(text.encode("utf-8")).hexdigest()
        owner = owner_for(rel)
        doc_type = doc_type_for(rel)
        status, action = classify(rel, owner, doc_type)
        obsolete_hits = [reason for pattern, reason in OBSOLETE_PATTERNS if pattern.search(text)]
        if obsolete_hits and status == "UNKNOWN":
            action = "review: " + "; ".join(sorted(set(obsolete_hits)))

        row = {
            "path": rel.as_posix(),
            "owner": owner,
            "doc_type": doc_type,
            "title": title_for(text),
            "lines": text.count("\n") + (1 if text else 0),
            "sha256": digest,
            "sha256_16": digest[:16],
            "status": status,
            "canonical_source": canonical_source_for(rel, owner, doc_type),
            "recommended_action": action,
            "obsolete_mentions": sorted(set(obsolete_hits)),
        }
        rows.append(row)
        words_by_path[row["path"]] = normalize_words(text)

    filename_groups: dict[str, list[dict[str, Any]]] = defaultdict(list)
    title_groups: dict[str, list[dict[str, Any]]] = defaultdict(list)
    hash_groups: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in rows:
        filename_groups[Path(row["path"]).name.lower()].append(row)
        if row["title"]:
            title_groups[row["title"].strip().lower()].append(row)
        hash_groups[row["sha256"]].append(row)

    duplicate_paths: set[str] = set()
    duplicate_findings: list[dict[str, Any]] = []

    def add_group(
        kind: str,
        key: str,
        grouped_rows: list[dict[str, Any]],
        score: float | None = None,
        mark_duplicate: bool = False,
    ) -> None:
        if len(grouped_rows) < 2:
            return
        paths = [row["path"] for row in grouped_rows]
        duplicate_findings.append({"kind": kind, "key": key, "score": score, "paths": paths})
        if mark_duplicate:
            ranked = sorted(grouped_rows, key=lambda row: STATUS_ORDER.get(row["status"], 99))
            for row in ranked[1:]:
                duplicate_paths.add(row["path"])

    ignored_names = {"readme.md", "index.md", "tasks.md", "proposal.md", "design.md", "spec.md", "skill.md"}
    for filename, grouped_rows in filename_groups.items():
        if len(grouped_rows) > 1 and filename not in ignored_names:
            add_group("same filename", filename, grouped_rows)
    ignored_titles = {"tasks", "index", "readme"}
    for title, grouped_rows in title_groups.items():
        if len(grouped_rows) > 1 and title not in ignored_titles:
            add_group("same H1 title", title, grouped_rows)
    for digest, grouped_rows in hash_groups.items():
        if len(grouped_rows) > 1 and any(row["lines"] > 0 for row in grouped_rows):
            add_group("identical content", digest[:16], grouped_rows, 1.0, mark_duplicate=True)

    comparable = [row for row in rows if row["lines"] >= 25]
    for index, left in enumerate(comparable):
        for right in comparable[index + 1:]:
            if left["sha256"] == right["sha256"]:
                continue
            score = overlap_score(words_by_path[left["path"]], words_by_path[right["path"]])
            if score >= 0.82:
                add_group(
                    "high textual overlap",
                    f"{left['path']} <> {right['path']}",
                    [left, right],
                    score,
                    mark_duplicate=True,
                )

    for row in rows:
        if row["path"] in duplicate_paths and row["status"] not in {"CANONICAL", "ARCHIVE"}:
            row["status"] = "DUPLICATE"
            row["recommended_action"] = "review duplicate group; keep/link canonical source before archive"

    rows.sort(key=lambda row: (row["owner"], row["path"]))

    out_dir = ROOT / "build"
    out_dir.mkdir(exist_ok=True)
    counts_by_owner = dict(sorted((owner, sum(1 for row in rows if row["owner"] == owner)) for owner in {row["owner"] for row in rows}))
    inventory_payload = {
        "generated_by": "scripts/docs/inventory-docs.py",
        "excluded_dirs": sorted(EXCLUDED_DIRS),
        "total_markdown_files": len(rows),
        "counts_by_owner": counts_by_owner,
        "counts_by_status": status_counts(rows),
        "ownership_map": [
            {"category": category, "canonical_location": location, "strategy": strategy}
            for category, location, strategy in OWNERSHIP_MAP
        ],
        "duplicates": duplicate_findings,
        "documents": rows,
    }
    (out_dir / "docs-inventory.json").write_text(json.dumps(inventory_payload, indent=2), encoding="utf-8")

    md = [
        "# Documentation Inventory",
        "",
        "> Generated by `scripts/docs/inventory-docs.py`. Do not edit the generated tables by hand.",
        "",
        "This inventory scans Markdown files while excluding generated/vendor directories: "
        + ", ".join(f"`{name}/`" for name in sorted(EXCLUDED_DIRS))
        + ".",
        "",
        "## Counts by owner",
        "",
        "| Owner | Markdown files |",
        "| --- | ---: |",
    ]
    for owner, count in counts_by_owner.items():
        md.append(f"| `{owner}` | {count} |")

    md.extend(["", "## Counts by status", "", "| Status | Markdown files |", "| --- | ---: |"])
    for status, count in status_counts(rows).items():
        md.append(f"| `{status}` | {count} |")

    md.extend(["", "## Canonical Ownership Map", "", "| Documentation category | Canonical location | MkDocs strategy |", "| --- | --- | --- |"])
    for category, location, strategy in OWNERSHIP_MAP:
        md.append(f"| {markdown_escape(category)} | `{markdown_escape(location)}` | {markdown_escape(strategy)} |")

    md.extend(["", "## Files", "", "| Path | Owner | Type | Title | Lines | Status | Canonical source | Action |", "| --- | --- | --- | --- | ---: | --- | --- | --- |"])
    for row in rows:
        md.append(
            f"| `{row['path']}` | {row['owner']} | {row['doc_type']} | {markdown_escape(row['title'])} | "
            f"{row['lines']} | {row['status']} | `{markdown_escape(row['canonical_source'])}` | "
            f"{markdown_escape(row['recommended_action'])} |"
        )

    target = ROOT / "tchalanet-docs" / "docs" / "99-reference"
    target.mkdir(parents=True, exist_ok=True)
    (target / "docs-inventory.md").write_text("\n".join(md) + "\n", encoding="utf-8")

    duplicate_md = [
        "# Duplicate Documentation Report",
        "",
        "> Generated by `scripts/docs/inventory-docs.py`. This is a heuristic report, not an instruction to delete files.",
        "",
        "Rules for cleanup:",
        "",
        "- Do not delete docs directly.",
        "- Keep component implementation docs near code.",
        "- If ownership is unclear, mark the document `UNKNOWN` and review it.",
        "- Archive or convert duplicates to links only after the canonical source is confirmed.",
        "",
        f"Total duplicate findings: {len(duplicate_findings)}",
        "",
    ]
    if duplicate_findings:
        duplicate_md.append("| Kind | Key / Score | Files |")
        duplicate_md.append("| --- | --- | --- |")
        for finding in duplicate_findings:
            score = "" if finding["score"] is None else f" score={finding['score']:.2f}"
            files = "<br>".join(f"`{markdown_escape(path)}`" for path in finding["paths"])
            duplicate_md.append(f"| {markdown_escape(finding['kind'])} | `{markdown_escape(finding['key'])}`{score} | {files} |")
    else:
        duplicate_md.append("No duplicate candidates were detected.")
    duplicate_text = "\n".join(duplicate_md) + "\n"
    (out_dir / "docs-duplicates.md").write_text(duplicate_text, encoding="utf-8")
    (target / "docs-duplicates.md").write_text(duplicate_text, encoding="utf-8")

    cleanup_md = [
        "# Documentation Cleanup Plan",
        "",
        "This plan is generated from the inventory. It proposes review actions only; no document is deleted by the script.",
        "",
        "| Action | Status source | Meaning | Count |",
        "| --- | --- | --- | ---: |",
    ]
    cleanup_rows = [
        ("keep", "CANONICAL", "Canonical source; keep in place and link from MkDocs."),
        ("keep summary", "SUMMARY", "Curated portal/context summary; keep concise."),
        ("link only", "LINK_ONLY", "Keep near owner and expose via MkDocs links."),
        ("merge or archive after review", "DUPLICATE", "Confirm canonical source before changing anything."),
        ("archive", "ARCHIVE", "Already archived or archive candidate; keep discoverable."),
        ("delete later", "DELETE_LATER", "Deletion requires a follow-up reviewed change."),
        ("review", "UNKNOWN", "Ownership/action unclear; do not move or delete yet."),
    ]
    counted_statuses = status_counts(rows)
    for action, status, meaning in cleanup_rows:
        cleanup_md.append(f"| {action} | `{status}` | {meaning} | {counted_statuses.get(status, 0)} |")
    cleanup_md.extend(["", "## Review Queue", "", "| Path | Owner | Status | Recommended action |", "| --- | --- | --- | --- |"])
    for row in rows:
        if row["status"] in {"DUPLICATE", "OBSOLETE", "DELETE_LATER", "UNKNOWN"}:
            cleanup_md.append(f"| `{row['path']}` | {row['owner']} | {row['status']} | {markdown_escape(row['recommended_action'])} |")
    (target / "docs-cleanup-plan.md").write_text("\n".join(cleanup_md) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
