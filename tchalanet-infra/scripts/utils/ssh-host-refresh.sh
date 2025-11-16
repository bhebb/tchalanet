#!/usr/bin/env bash
set -euo pipefail
# ssh-host-refresh.sh <ip_or_host> [alias]
# Supprime les anciennes empreintes et ajoute la nouvelle dans known_hosts.
IP_OR_HOST="${1:?ip_or_host required}"
ALIAS="${2:-}"
KNOWN_HOSTS="$HOME/.ssh/known_hosts"

log(){ echo "[ssh-refresh] $*"; }

if [ -f "$KNOWN_HOSTS" ]; then
  log "Removing existing host key entries for $IP_OR_HOST" || true
  ssh-keygen -R "$IP_OR_HOST" >/dev/null 2>&1 || true
  if [ -n "$ALIAS" ]; then
    log "Removing existing host key entries for alias $ALIAS" || true
    ssh-keygen -R "$ALIAS" >/dev/null 2>&1 || true
  fi
fi

log "Fetching fresh host key for $IP_OR_HOST"
if ssh-keyscan -H "$IP_OR_HOST" >> "$KNOWN_HOSTS" 2>/dev/null; then
  log "Host key appended to $KNOWN_HOSTS"
else
  echo "[ssh-refresh] ERROR: failed to fetch host key for $IP_OR_HOST" >&2
  exit 1
fi

if [ -n "$ALIAS" ]; then
  log "(Optional) Add alias entry via ssh config: Host $ALIAS -> HostName $IP_OR_HOST"
fi

log "Done."
