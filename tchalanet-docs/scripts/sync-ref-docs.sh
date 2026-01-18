#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
DOCS_DIR="$ROOT_DIR/tchalanet-docs/docs"
REF_DIR="$DOCS_DIR/99-links/_ref"

rm -rf "$REF_DIR"
mkdir -p "$REF_DIR/web" "$REF_DIR/server" "$REF_DIR/infra" "$REF_DIR/openspec" "$REF_DIR/edge"

# --- Web app docs (near-code)
mkdir -p "$REF_DIR/web/app"
cp -f "$ROOT_DIR/apps/tchalanet-web/"*.md "$REF_DIR/web/app/" 2>/dev/null || true

# --- Web libs docs
mkdir -p "$REF_DIR/web/libs"
cp -f "$ROOT_DIR/libs/ui/styles/README.md" "$REF_DIR/web/libs/ui-styles.md" 2>/dev/null || true
cp -f "$ROOT_DIR/libs/ui/theme/README.md" "$REF_DIR/web/libs/ui-theme.md" 2>/dev/null || true
cp -f "$ROOT_DIR/libs/ui/widget-renderer/README.md" "$REF_DIR/web/libs/widget-renderer.md" 2>/dev/null || true
cp -f "$ROOT_DIR/libs/web/shell/README.md" "$REF_DIR/web/libs/web-shell.md" 2>/dev/null || true
cp -f "$ROOT_DIR/libs/web/widgets/README.md" "$REF_DIR/web/libs/web-widgets.md" 2>/dev/null || true

# --- OpenSpec context packs
mkdir -p "$REF_DIR/openspec/context"
cp -f "$ROOT_DIR/openspec/context/"*.md "$REF_DIR/openspec/context/" 2>/dev/null || true
cp -f "$ROOT_DIR/openspec/project.md" "$REF_DIR/openspec/project.md" 2>/dev/null || true
cp -f "$ROOT_DIR/openspec/AGENTS.md" "$REF_DIR/openspec/AGENTS.md" 2>/dev/null || true

# --- Backend docs (tchalanet-server/docs)
mkdir -p "$REF_DIR/server"
cp -rf "$ROOT_DIR/tchalanet-server/docs/"* "$REF_DIR/server/" 2>/dev/null || true

# --- Backend domain docs (DOMAIN_*.md from src)
DOMAINS_REF_DIR="$REF_DIR/server/domains"
mkdir -p "$DOMAINS_REF_DIR"
while IFS= read -r -d '' file; do
  rel="${file#"$ROOT_DIR/tchalanet-server/src/main/java/com/tchalanet/server/"}"
  # Ensure directory exists under server/domains/
  dst_dir="$DOMAINS_REF_DIR/$(dirname "$rel")"
  mkdir -p "$dst_dir"
  cp -f "$file" "$dst_dir/"
done < <(find "$ROOT_DIR/tchalanet-server/src/main/java/com/tchalanet/server" -name 'DOMAIN_*.md' -print0 2>/dev/null)

# --- Infra docs
mkdir -p "$REF_DIR/infra"
cp -rf "$ROOT_DIR/tchalanet-infra/docs/"* "$REF_DIR/infra/" 2>/dev/null || true

# --- Edge service docs
mkdir -p "$REF_DIR/edge"
cp -f "$ROOT_DIR/tchalanet-edge-service/README.md" "$REF_DIR/edge/README.md" 2>/dev/null || true

echo "✅ Synced reference docs into $REF_DIR"
