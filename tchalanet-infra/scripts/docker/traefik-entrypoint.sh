#!/bin/sh
set -e

# Traefik entrypoint script
# Ensures acme.json has correct permissions before starting Traefik

ACME_FILE="/acme.json"

if [ -f "$ACME_FILE" ]; then
  echo "→ Setting acme.json permissions to 600"
  chmod 600 "$ACME_FILE"
else
  echo "→ Creating acme.json with permissioFns 600"
  touch "$ACME_FILE"
  chmod 600 "$ACME_FILE"
fi

echo "→ Starting Traefik..."
exec /entrypoint.sh "$@"

