#!/usr/bin/env bash
# Wrapper for run-compose.sh to call it from Make safely
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
HELPER="$SCRIPT_DIR/run-compose.sh"
if [ ! -x "$HELPER" ]; then
  echo "Missing helper $HELPER" >&2
  exit 2
fi

# forward args as-is
exec "$HELPER" "$@"
