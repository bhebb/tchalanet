#!/usr/bin/env sh
set -eu

# tchl redis entrypoint: run redis with or without requirepass depending on REDIS_PASSWORD
# This script is mounted into the container and invoked as the service command.

# Defaults (can be overridden by env)
: ${REDIS_MAX_MEMORY:=256mb}
: ${REDIS_MAXMEMORY_POLICY:=allkeys-lru}
: ${REDIS_APPENDONLY:=yes}
: ${REDIS_APPENDFSYNC:=everysec}

if [ -n "${REDIS_PASSWORD-}" ]; then
  exec redis-server \
    --requirepass "$REDIS_PASSWORD" \
    --save 60 1 \
    --loglevel notice \
    --maxmemory "$REDIS_MAX_MEMORY" \
    --maxmemory-policy "$REDIS_MAXMEMORY_POLICY" \
    --appendonly "$REDIS_APPENDONLY" \
    --appendfsync "$REDIS_APPENDFSYNC"
else
  exec redis-server \
    --save 60 1 \
    --loglevel notice \
    --maxmemory "$REDIS_MAX_MEMORY" \
    --maxmemory-policy "$REDIS_MAXMEMORY_POLICY" \
    --appendonly "$REDIS_APPENDONLY" \
    --appendfsync "$REDIS_APPENDFSYNC"
fi
