#!/usr/bin/env bash
set -euo pipefail

# get-realm.sh <env>
# Génère un realm JSON à partir d'un template (realm.base.json)
# Usage: ./get-realm.sh dev|local|staging|tg|prod

ENV="${1:-dev}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

TEMPLATE="${TEMPLATE:-$ROOT_DIR/keycloak/realms/templates/realm.base.json}"
OUT_DIR="${OUT_DIR:-$ROOT_DIR/keycloak/realms}"
OVERLAY_DIR="$ROOT_DIR/keycloak/realms/overlays"

# ---------- helpers ----------

mktemp_safe() {
  local template="$1"
  local tmp
  if tmp=$(mktemp "$template" 2>/dev/null); then
    if [ -f "$tmp" ]; then
      echo "$tmp"; return 0
    fi
  fi
  local prefix
  prefix=$(basename "${template%%.*}")
  if tmp=$(mktemp -t "$prefix" 2>/dev/null); then
    echo "$tmp"; return 0
  fi
  if command -v python3 >/dev/null 2>&1; then
    python3 - <<'PY'
import tempfile
f = tempfile.NamedTemporaryFile(delete=False)
print(f.name)
PY
    return 0
  fi
  echo "Unable to create temporary file (mktemp/python missing)" >&2
  return 1
}

TMP_FILES=()
add_tmp() { TMP_FILES+=("$1"); }
cleanup_tmp() { rm -f "${TMP_FILES[@]:-}" 2>/dev/null || true; }
trap cleanup_tmp EXIT

command -v jq >/dev/null 2>&1 || { echo "✖ jq required" >&2; exit 1; }
[ -f "$TEMPLATE" ] || { echo "✖ Template not found: $TEMPLATE" >&2; exit 1; }

# ---------- overlay resolution ----------

OVERLAY_FILE=""
if [ -d "$OVERLAY_DIR" ]; then
  OVERLAY_CANDIDATES=()
  OVERLAY_CANDIDATES+=("$OVERLAY_DIR/${ENV}.json")

  if [ "$ENV" = "production" ] || [ "$ENV" = "prod" ]; then
    OVERLAY_CANDIDATES+=("$OVERLAY_DIR/prod.json")
  fi

  FOUND_OVERLAYS=()
  for c in "${OVERLAY_CANDIDATES[@]}"; do
    [ -f "$c" ] && FOUND_OVERLAYS+=("$c")
  done

  if [ ${#FOUND_OVERLAYS[@]} -eq 0 ]; then
    OVERLAY_FILE=""
  elif [ ${#FOUND_OVERLAYS[@]} -eq 1 ]; then
    OVERLAY_FILE="${FOUND_OVERLAYS[0]}"
  else
    if [ "${MERGE_OVERLAYS:-0}" = "1" ]; then
      tmp_merge=$(mktemp_safe /tmp/tch-get-realm.overlay.merge.XXXXXX.json)
      add_tmp "$tmp_merge"
      jq -s 'reduce .[] as $i ({}; . * $i)' "${FOUND_OVERLAYS[@]}" > "$tmp_merge"
      OVERLAY_FILE="$tmp_merge"
      echo "→ MERGE_OVERLAYS=1: merged overlays into $OVERLAY_FILE" >&2
    else
      preferred="${FOUND_OVERLAYS[0]}"
      for f in "${FOUND_OVERLAYS[@]}"; do
        case "$(basename "$f")" in
          prod.json)       preferred="$f"; break ;;
          production.json)
            if [ "${preferred##*/}" != "prod.json" ]; then
              preferred="$f"
            fi
            ;;
        esac
      done
      echo "→ Multiple overlays found: using $preferred (others ignored). Set MERGE_OVERLAYS=1 to merge them." >&2
      OVERLAY_FILE="$preferred"
    fi
  fi
fi

# ---------- env, realm name, i18n ----------

ENV_DIR="$ROOT_DIR/envs/$ENV"

load_env_merged() {
  local d="$1"
  local f="$d/.env.merged"
  if [ -f "$f" ]; then
    echo "→ load env merged: $f" >&2
    set -a; . "$f"; set +a
  else
    echo "→ no .env.merged in: $d" >&2
  fi
}
load_env_merged "$ENV_DIR" || true

KC_REALM="${KC_REALM:-tchalanet}"
DEFAULT_LOCALE="${DEFAULT_LOCALE:-fr}"
SUPPORTED_LOCALES_CSV="${SUPPORTED_LOCALES:-}"

mkdir -p "$OUT_DIR"
OUT_FILE="${OUT_FILE:-$OUT_DIR/tchalanet-realm.json}"

echo "→ Generating realm from template: $TEMPLATE (env=$ENV, realm=$KC_REALM)" >&2

# supportedLocales JSON
if [ -n "$SUPPORTED_LOCALES_CSV" ]; then
  IFS=',' read -r -a _locales <<< "$SUPPORTED_LOCALES_CSV"
  seen=()
  locales_json="[]"
  for l in "${_locales[@]}"; do
    l=$(echo "$l" | xargs)
    [ -z "$l" ] && continue
    if ! printf "%s\n" "${seen[@]}" | grep -qx "$l"; then
      seen+=("$l")
      locales_json=$(echo "$locales_json" | jq --arg v "$l" '. + [$v]')
    fi
  done
else
  locales_json="null"
fi

tmp1=$(mktemp_safe /tmp/tch-get-realm.XXXXXX.json)
add_tmp "$tmp1"

if [ "$locales_json" != "null" ]; then
  jq --arg realm "$KC_REALM" \
     --arg defaultLocale "$DEFAULT_LOCALE" \
     --argjson locales "$locales_json" '
    .realm = $realm
    | .defaultLocale = $defaultLocale
    | .internationalizationEnabled = true
    | .supportedLocales = $locales
  ' "$TEMPLATE" > "$tmp1"
else
  jq --arg realm "$KC_REALM" \
     --arg defaultLocale "$DEFAULT_LOCALE" '
    .realm = $realm
    | .defaultLocale = $defaultLocale
    | .internationalizationEnabled = true
  ' "$TEMPLATE" > "$tmp1"
fi

cp "$tmp1" "$OUT_FILE"

# ---------- apply overlay (if any) ----------

if [ -n "${OVERLAY_FILE:-}" ] && [ -f "$OVERLAY_FILE" ]; then
  tmp_overlay=$(mktemp_safe /tmp/tch-get-realm.overlay.XXXXXX.json)
  add_tmp "$tmp_overlay"
  jq -s '
    .[0] as $base | .[1] as $overlay
    | ($base.clients // []) as $bclients
    | ($overlay.clients // []) as $oclients
    | ($bclients + $oclients) as $allclients
    | (reduce $allclients[] as $c ({}; .[$c.clientId] = ((.[$c.clientId] // {}) * $c))) as $clients_map
    | ($base * $overlay) | .clients = ([$clients_map[]])
  ' "$OUT_FILE" "$OVERLAY_FILE" > "$tmp_overlay"
  mv "$tmp_overlay" "$OUT_FILE"
  echo "→ Applied intelligent overlay merge: $OVERLAY_FILE" >&2
fi

# ---------- ensure realm enabled ----------

if [ "${ALLOW_DISABLED_REALM:-0}" != "1" ]; then
  tmp_force=$(mktemp_safe /tmp/tch-get-realm.force.XXXXXX.json)
  add_tmp "$tmp_force"
  jq '.enabled = true' "$OUT_FILE" > "$tmp_force"
  mv "$tmp_force" "$OUT_FILE"
  echo "→ Forced .enabled = true on $OUT_FILE (set ALLOW_DISABLED_REALM=1 to skip)" >&2
fi

# ---------- validate JSON ----------

jq empty "$OUT_FILE" || { echo "✖ Generated realm JSON is invalid: $OUT_FILE" >&2; exit 1; }

# ---------- refuse .users hors dev/local ----------

case "$ENV" in
  dev|local) ;;
  *)
    USER_COUNT=$(jq '(.users // []) | length' "$OUT_FILE")
    if [ "$USER_COUNT" -gt 0 ]; then
      echo "✖ Realm contains $USER_COUNT user(s) but ENV=$ENV — users are forbidden outside dev/local." >&2
      echo "  Remove .users from overlays/$ENV.json or from realm.base.json." >&2
      exit 1
    fi
    ;;
esac

echo "✔ Realm generated to: $OUT_FILE" >&2
exit 0
