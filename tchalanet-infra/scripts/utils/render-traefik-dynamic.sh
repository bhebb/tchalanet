#!/usr/bin/env bash
# Render traefik/dynamic/ pour un seul environnement actif à la fois.
# Usage: ./render-traefik-dynamic.sh [dev|staging|prod]
set -euo pipefail

ENV="${1:-dev}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SRC="$ROOT/traefik/dynamic-src"
DEST="$ROOT/traefik/dynamic"

rm -rf "$DEST/env"
mkdir -p "$DEST/common" "$DEST/env"

cp -r "$SRC/common/." "$DEST/common/"

case "$ENV" in
  dev|local)
    cp -r "$SRC/local/." "$DEST/env/"
    ;;
  staging|stg)
    cp -r "$SRC/staging/." "$DEST/env/"
    ;;
  prod|production)
    cp -r "$SRC/prod/." "$DEST/env/"
    ;;
  *)
    echo "ENV inconnu: $ENV (dev|staging|prod attendu)" >&2
    exit 1
    ;;
esac

echo "→ Traefik dynamic rendu pour env=$ENV dans $DEST"
