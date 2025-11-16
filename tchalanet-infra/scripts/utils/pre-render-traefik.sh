#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# Pre-render Traefik dynamic config
# Copies: server/traefik/dynamic/env/<env>.yaml -> server/traefik/dynamic/10-routers.yaml
# Usage:  ./scripts/utils/pre-render-traefik.sh [local.yaml|staging.yaml|prod.yaml]
# Default: local.yaml
# ---------------------------------------------------------------------------

# Resolve repo root from this script location: <repo>/scripts/utils/pre-render-traefik.sh
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"              # go up from scripts/utils -> repo root
TRAEFIK_DIR="$ROOT_DIR/traefik"

ENV_FILE="${1:-local.yaml}"
SRC="$TRAEFIK_DIR/dynamic/env/$ENV_FILE"
DST="$TRAEFIK_DIR/dynamic/10-routers.yaml"

if [ ! -f "$SRC" ]; then
  echo "✖ Environment file not found: $SRC" >&2
  echo "  Available:" >&2
  ls -1 "$TRAEFIK_DIR/dynamic/env" >&2 || true
  exit 1
fi

# Ensure dynamic dir exists
mkdir -p "$TRAEFIK_DIR/dynamic"

# Render
cp -f "$SRC" "$DST"

echo "✔ Rendered Traefik routers file"
echo "  → From: $SRC"
echo "  → To:   $DST"
