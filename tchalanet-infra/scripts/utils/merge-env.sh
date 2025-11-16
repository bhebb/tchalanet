#!/usr/bin/env bash
set -euo pipefail
ENV="${1:-dev}"

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/envs/$ENV/.env.merged"
mkdir -p "$ROOT/envs/$ENV"

echo "→ Generating $OUT"

# Temp file
tmp=$(mktemp)
trap 'rm -f "$tmp"' EXIT

# collect files in order: all *.env from common then all *.env from env-specific
# Exclude any file named compose.env and do NOT include any .secrets file in the merged output
files=()
# include common/.env if present (explicit)
[ -f "$ROOT/envs/common/.env" ] && files+=("$ROOT/envs/common/.env")
# add all common/*.env but skip compose.env
for f in "$ROOT/envs/common"/*.env; do
  [ -f "$f" ] || continue
  [ "$(basename "$f")" = "compose.env" ] && continue
  files+=("$f")
done
# include env-specific .env if present
[ -f "$ROOT/envs/$ENV/.env" ] && files+=("$ROOT/envs/$ENV/.env")
# add all env-specific *.env but skip compose.env and don't add any .secrets
for f in "$ROOT/envs/$ENV"/*.env; do
  [ -f "$f" ] || continue
  b=$(basename "$f")
  [ "$b" = "compose.env" ] && continue
  files+=("$f")
done

# concatenate key=value lines into tmp, prefixed by source comment
for f in "${files[@]}"; do
  echo "# --- from: $f" >> "$tmp"
  # only keep valid KEY=... lines
  grep -E '^[A-Za-z_][A-Za-z0-9_]*=' "$f" >> "$tmp" || true
  echo >> "$tmp"
done

# AWK: keep only the last occurrence for each key, preserve order of last occurrences
awk '
function trim(s){ sub(/^[ \t\r\n]+/,"",s); sub(/[ \t\r\n]+$/,"",s); return s }
{
  if ($0 ~ /^[[:space:]]*[A-Za-z_][A-Za-z0-9_]*[[:space:]]*=/) {
    split($0, a, "=")
    key = trim(a[1])
    val = substr($0, index($0, "=")+1)
    last_pos[key] = NR
    last_val[key] = val
    line_key[NR] = key
    lines[NR] = $0
  } else {
    lines[NR] = $0
  }
}
END{
  for(i=1;i<=NR;i++){
    if (line_key[i] == "") continue
    k = line_key[i]
    if (last_pos[k] == i) print k "=" last_val[k]
  }
}
' "$tmp" > "$OUT"

chmod 600 "$OUT"

echo "✔ Wrote $(wc -l < "$OUT") lines to $OUT"
