#!/usr/bin/env python3
"""
Extract Spring controller endpoint mapping for Tchalanet.

Usage from repository root:
  python3 scripts/extract_tchalanet_controller_mapping.py \
    --root . \
    --out-md docs/generated/backend-controller-endpoints.md \
    --out-csv docs/generated/backend-controller-endpoints.csv

The script is regex-based on purpose: fast, no Maven build needed.
It extracts:
- module
- package
- controller class
- @Tag
- class @RequestMapping base path
- method HTTP verb/path
- Java method name
- @PreAuthorize
- @RequiredFeature
- source file/line
"""
from __future__ import annotations

import argparse
import csv
import re
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Iterable

HTTP_BY_MAPPING = {
    "GetMapping": "GET",
    "PostMapping": "POST",
    "PutMapping": "PUT",
    "PatchMapping": "PATCH",
    "DeleteMapping": "DELETE",
}
MAPPING_ANNOTATIONS = tuple(HTTP_BY_MAPPING.keys()) + ("RequestMapping",)

@dataclass
class EndpointRow:
    module: str
    package: str
    controller: str
    tag: str
    class_base_path: str
    http_method: str
    method_path: str
    full_path: str
    java_method: str
    pre_authorize: str
    required_feature: str
    source: str
    line: int


def strip_comments(s: str) -> str:
    s = re.sub(r"/\*.*?\*/", "", s, flags=re.S)
    s = re.sub(r"//.*", "", s)
    return s


def compact(s: str) -> str:
    return re.sub(r"\s+", " ", s or "").strip()


def module_name(path: Path) -> str:
    for p in path.parts:
        if p.startswith("tchalanet-"):
            return p
    return ""


def join_paths(base: str, sub: str) -> str:
    base = (base or "").strip()
    sub = (sub or "").strip()
    if not base and not sub:
        return "/"
    if not base:
        out = sub
    elif not sub:
        out = base
    else:
        out = base.rstrip("/") + "/" + sub.lstrip("/")
    if not out.startswith("/"):
        out = "/" + out
    out = re.sub(r"//+", "/", out)
    return out


def split_top_level_csv(s: str) -> list[str]:
    vals, cur, depth, quote = [], [], 0, None
    i = 0
    while i < len(s):
        ch = s[i]
        if quote:
            cur.append(ch)
            if ch == quote and (i == 0 or s[i - 1] != "\\"):
                quote = None
        else:
            if ch in ('"', "'"):
                quote = ch
                cur.append(ch)
            elif ch in "({[":
                depth += 1
                cur.append(ch)
            elif ch in ")}]":
                depth -= 1
                cur.append(ch)
            elif ch == "," and depth == 0:
                vals.append("".join(cur).strip())
                cur = []
            else:
                cur.append(ch)
        i += 1
    if cur:
        vals.append("".join(cur).strip())
    return vals


def unquote_java_string(s: str) -> str:
    s = s.strip()
    if s.startswith('"') and s.endswith('"'):
        return s[1:-1]
    return s


def annotation_arg(ann_text: str, key: str | None = None) -> str:
    """Return annotation argument. key=None means first positional string / value."""
    m = re.search(r"\((.*)\)\s*$", ann_text, flags=re.S)
    if not m:
        return ""
    body = compact(m.group(1))
    if not body:
        return ""

    if key:
        # key = "..." or key = {"..."}
        km = re.search(rf"\b{re.escape(key)}\s*=\s*(\{{[^}}]*\}}|\"[^\"]*\"|[^,)]*)", body)
        if km:
            val = km.group(1).strip()
            if val.startswith("{") and val.endswith("}"):
                vals = [unquote_java_string(v) for v in split_top_level_csv(val[1:-1])]
                return ", ".join(vals)
            return unquote_java_string(val)
        return ""

    # explicit value/path wins
    for k in ("value", "path"):
        val = annotation_arg(ann_text, k)
        if val:
            return val

    # first positional string or array
    if body.startswith("{") and body.endswith("}"):
        vals = [unquote_java_string(v) for v in split_top_level_csv(body[1:-1])]
        return ", ".join(vals)
    first = split_top_level_csv(body)[0] if body else ""
    if first.startswith('"'):
        return unquote_java_string(first)
    return ""


def extract_annotations_block(text: str, start: int, end: int) -> str:
    before = text[start:end]
    # collect contiguous annotation lines before class/method signature
    lines = before.splitlines()
    picked: list[str] = []
    paren_balance = 0
    for line in reversed(lines):
        stripped = line.strip()
        if not stripped and not picked:
            continue
        if not picked and not stripped.startswith("@") and paren_balance == 0:
            continue
        picked.append(line)
        paren_balance += stripped.count(")") - stripped.count("(")
        if stripped.startswith("@") and paren_balance >= 0:
            # continue upward only if previous line is another annotation
            continue
        if picked and not stripped.startswith("@") and paren_balance <= 0:
            break
    return "\n".join(reversed(picked))


def find_annotation(block: str, name: str) -> str:
    # Supports @Name(...) possibly multiline until balanced closing paren, or @Name
    idx = block.find("@" + name)
    if idx < 0:
        return ""
    i = idx + len(name) + 1
    while i < len(block) and block[i].isspace():
        i += 1
    if i >= len(block) or block[i] != "(":
        return "@" + name
    depth = 0
    j = i
    quote = None
    while j < len(block):
        ch = block[j]
        if quote:
            if ch == quote and block[j - 1] != "\\":
                quote = None
        else:
            if ch in ('"', "'"):
                quote = ch
            elif ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
                if depth == 0:
                    return block[idx:j + 1]
        j += 1
    return block[idx:]


def http_for_request_mapping(ann: str) -> str:
    m = re.search(r"RequestMethod\.([A-Z]+)", ann)
    return m.group(1) if m else "ANY"


def paths_from_annotation(ann: str) -> list[str]:
    val = annotation_arg(ann)
    if not val:
        return [""]
    return [v.strip() for v in val.split(",") if v.strip()]


def extract_file(path: Path, root: Path) -> list[EndpointRow]:
    raw = path.read_text(encoding="utf-8", errors="ignore")
    text = strip_comments(raw)
    if "@RestController" not in text and "@Controller" not in text:
        return []

    pkg = re.search(r"\bpackage\s+([\w.]+)\s*;", text)
    package = pkg.group(1) if pkg else ""

    cls = re.search(r"(?P<ann>(?:\s*@\w+(?:\([^;]*?\))?\s*)*)\bpublic\s+(?:final\s+)?class\s+(?P<name>\w+)", text, flags=re.S)
    if not cls:
        return []
    controller = cls.group("name")
    class_anns = cls.group("ann")

    base_ann = find_annotation(class_anns, "RequestMapping")
    base_paths = paths_from_annotation(base_ann) if base_ann else [""]
    tag_ann = find_annotation(class_anns, "Tag")
    tag = annotation_arg(tag_ann, "name") if tag_ann else ""
    class_auth_ann = find_annotation(class_anns, "PreAuthorize")
    class_auth = annotation_arg(class_auth_ann) if class_auth_ann else ""
    class_feat_ann = find_annotation(class_anns, "RequiredFeature")
    class_feature = annotation_arg(class_feat_ann) if class_feat_ann else ""

    rows: list[EndpointRow] = []
    method_re = re.compile(
        r"(?P<ann>(?:\s*@\w+(?:\([^;{}]*?\))?\s*)+)\s*"
        r"public\s+(?:<[^>]+>\s+)?[\w<>?,\s\.]+\s+(?P<name>\w+)\s*\(",
        flags=re.S,
    )
    for mm in method_re.finditer(text):
        anns = mm.group("ann")
        java_method = mm.group("name")
        mapping_name = None
        mapping_ann = ""
        for mn in MAPPING_ANNOTATIONS:
            ann = find_annotation(anns, mn)
            if ann:
                mapping_name = mn
                mapping_ann = ann
                break
        if not mapping_name:
            continue
        method = HTTP_BY_MAPPING.get(mapping_name, http_for_request_mapping(mapping_ann))
        method_paths = paths_from_annotation(mapping_ann)

        meth_auth_ann = find_annotation(anns, "PreAuthorize")
        meth_auth = annotation_arg(meth_auth_ann) if meth_auth_ann else ""
        meth_feat_ann = find_annotation(anns, "RequiredFeature")
        meth_feature = annotation_arg(meth_feat_ann) if meth_feat_ann else ""

        pre_auth = meth_auth or class_auth
        required_feature = meth_feature or class_feature
        line = text.count("\n", 0, mm.start()) + 1

        for base in base_paths:
            for mp in method_paths:
                rows.append(EndpointRow(
                    module=module_name(path),
                    package=package,
                    controller=controller,
                    tag=tag,
                    class_base_path=base,
                    http_method=method,
                    method_path=mp,
                    full_path=join_paths(base, mp),
                    java_method=java_method,
                    pre_authorize=pre_auth,
                    required_feature=required_feature,
                    source=str(path.relative_to(root)),
                    line=line,
                ))
    return rows


def collect(root: Path) -> list[EndpointRow]:
    rows: list[EndpointRow] = []
    server = root / "tchalanet-server"
    search_root = server if server.exists() else root
    for path in search_root.rglob("*.java"):
        if "/target/" in path.as_posix() or "/build/" in path.as_posix():
            continue
        rows.extend(extract_file(path, root))
    rows.sort(key=lambda r: (r.full_path, r.http_method, r.controller, r.java_method))
    return rows


def write_csv(rows: list[EndpointRow], out: Path) -> None:
    out.parent.mkdir(parents=True, exist_ok=True)
    with out.open("w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(asdict(rows[0]).keys()) if rows else ["module"])
        w.writeheader()
        for r in rows:
            w.writerow(asdict(r))


def write_md(rows: list[EndpointRow], out: Path) -> None:
    out.parent.mkdir(parents=True, exist_ok=True)
    by_surface: dict[str, list[EndpointRow]] = {}
    for r in rows:
        surface = r.full_path.split("/")[1] if r.full_path != "/" and len(r.full_path.split("/")) > 1 else "root"
        by_surface.setdefault(surface, []).append(r)

    with out.open("w", encoding="utf-8") as f:
        f.write("# Backend controller endpoint mapping\n\n")
        f.write("Generated by `extract_tchalanet_controller_mapping.py`.\n\n")
        f.write(f"Total endpoints: **{len(rows)}**\n\n")
        f.write("## Summary by surface\n\n")
        f.write("| Surface | Count |\n|---|---:|\n")
        for s, rs in sorted(by_surface.items()):
            f.write(f"| `{s}` | {len(rs)} |\n")
        f.write("\n")
        for s, rs in sorted(by_surface.items()):
            f.write(f"## `{s}`\n\n")
            f.write("| Method | Path | Controller.method | Module | Security | Feature | Source |\n")
            f.write("|---|---|---|---|---|---|---|\n")
            for r in rs:
                source = f"`{r.source}:{r.line}`"
                sec = compact(r.pre_authorize).replace("|", "\\|")
                feat = compact(r.required_feature).replace("|", "\\|")
                f.write(
                    f"| `{r.http_method}` | `{r.full_path}` | `{r.controller}.{r.java_method}` "
                    f"| `{r.module}` | `{sec}` | `{feat}` | {source} |\n"
                )
            f.write("\n")


def print_duplicates(rows: list[EndpointRow]) -> None:
    seen: dict[tuple[str, str], list[EndpointRow]] = {}
    for r in rows:
        seen.setdefault((r.http_method, r.full_path), []).append(r)
    dups = {k: v for k, v in seen.items() if len(v) > 1}
    if not dups:
        print("No duplicate method+path mappings found.")
        return
    print("Duplicate method+path mappings:")
    for (method, path), rs in sorted(dups.items()):
        print(f"- {method} {path}")
        for r in rs:
            print(f"  - {r.controller}.{r.java_method} ({r.source}:{r.line})")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=".", help="Repository root")
    ap.add_argument("--out-md", default="backend-controller-endpoints.md")
    ap.add_argument("--out-csv", default="backend-controller-endpoints.csv")
    ap.add_argument("--duplicates", action="store_true", help="Print duplicate method+path mappings")
    args = ap.parse_args()

    root = Path(args.root).resolve()
    rows = collect(root)
    write_md(rows, Path(args.out_md))
    write_csv(rows, Path(args.out_csv))
    print(f"Extracted {len(rows)} endpoints")
    print(f"Markdown: {args.out_md}")
    print(f"CSV:      {args.out_csv}")
    if args.duplicates:
        print_duplicates(rows)


if __name__ == "__main__":
    main()
