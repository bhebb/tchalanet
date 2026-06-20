#!/usr/bin/env zsh
set -euo pipefail

SCRIPT_DIR_PHASE1="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="${ENV_FILE:-$SCRIPT_DIR_PHASE1/.env}"
if [[ -f "$ENV_FILE" ]]; then
  set -a
  source "$ENV_FILE"
  set +a
fi

BASE_URL="${TCH_BASE_URL:-http://localhost:8083/api/v1}"
SUPER_ADMIN_TOKEN="${TCH_SUPER_ADMIN_TOKEN:-}"
SELLER_TOKEN="${TCH_SELLER_TOKEN:-}"
AUTH_TOKEN_URL="${TCH_AUTH_TOKEN_URL:-}"
AUTH_CLIENT_ID="${TCH_AUTH_CLIENT_ID:-}"
AUTH_CLIENT_SECRET="${TCH_AUTH_CLIENT_SECRET:-}"
SUPER_ADMIN_USERNAME="${TCH_SUPER_ADMIN_USERNAME:-}"
SUPER_ADMIN_PASSWORD="${TCH_SUPER_ADMIN_PASSWORD:-}"
SELLER_USERNAME="${TCH_SELLER_USERNAME:-${TCH_BUYER_USERNAME:-}}"
SELLER_PASSWORD="${TCH_SELLER_PASSWORD:-${TCH_BUYER_PASSWORD:-}}"
TENANT_ID="${TCH_TENANT_ID:-00000000-0000-0000-0000-000000000003}"
OUTLET_ID="${TCH_OUTLET_ID:-00000000-0000-0000-0000-000000003001}"
TERMINAL_ID="${TCH_TERMINAL_ID:-00000000-0000-0000-0000-000000003101}"
OPENING_FLOAT="${TCH_OPENING_FLOAT:-100.00}"
DRAWS_PAGE_SIZE="${TCH_DRAWS_PAGE_SIZE:-100}"
GENERATE_DAYS="${TCH_GENERATE_DAYS:-7}"

today_utc_date() {
  python3 - <<'PY'
from datetime import datetime, timezone
print(datetime.now(timezone.utc).date().isoformat())
PY
}

date_plus_days() {
  local date_value="$1"
  local days="$2"
  python3 - "$date_value" "$days" <<'PY'
from datetime import date, timedelta
import sys

base = date.fromisoformat(sys.argv[1])
offset = int(sys.argv[2])
print((base + timedelta(days=offset)).isoformat())
PY
}

TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/tch-e2e-phase1.XXXXXX")"
trap 'rm -rf "$TMP_DIR"' EXIT

require_env() {
  local name="$1"
  local value="$2"
  if [[ -z "$value" ]]; then
    print -u2 -- "[ERREUR] Variable obligatoire manquante: $name"
    exit 1
  fi
}

json_get() {
  local file_path="$1"
  local dotted_path="$2"
  python3 - "$file_path" "$dotted_path" <<'PY'
import json
import sys
from pathlib import Path

file_path = Path(sys.argv[1])
path = [part for part in sys.argv[2].split('.') if part]
text = file_path.read_text()
if not text.strip():
    raise SystemExit(2)
current = json.loads(text)
for part in path:
    if isinstance(current, list):
        current = current[int(part)]
    else:
        current = current[part]
if current is None:
    raise SystemExit(3)
if isinstance(current, (dict, list)):
    print(json.dumps(current))
else:
    print(current)
PY
}

json_try_get() {
  local file_path="$1"
  local dotted_path="$2"
  local fallback="${3:-}"
  local value=""
  if value="$(json_get "$file_path" "$dotted_path" 2>/dev/null)"; then
    print -r -- "$value"
  else
    print -r -- "$fallback"
  fi
}

extract_access_token() {
  local source_file="$1"
  python3 - "$source_file" <<'PY'
import json
import sys
from pathlib import Path

payload = json.loads(Path(sys.argv[1]).read_text() or "{}")
token = payload.get("access_token")
if not token:
    raise SystemExit(1)
print(token)
PY
}

fetch_oauth_token() {
  local username="$1"
  local password="$2"
  local role_label="$3"
  local output_file="$TMP_DIR/token-${role_label}.json"
  local http_code=''
  local -a args
  args=(
    -sS
    -o "$output_file"
    -w '%{http_code}'
    -X POST
    "$AUTH_TOKEN_URL"
    -H 'Accept: application/json'
    -H 'Content-Type: application/x-www-form-urlencoded'
    --data-urlencode 'grant_type=password'
    --data-urlencode "client_id=$AUTH_CLIENT_ID"
    --data-urlencode "username=$username"
    --data-urlencode "password=$password"
  )
  if [[ -n "$AUTH_CLIENT_SECRET" ]]; then
    args+=( --data-urlencode "client_secret=$AUTH_CLIENT_SECRET" )
  fi

  if ! http_code="$(curl "${args[@]}")"; then
    print -u2 -- "[ERREUR] Impossible de récupérer le token OAuth2 ($role_label)"
    exit 1
  fi
  if [[ "$http_code" != "200" ]]; then
    fail_with_response "Échec récupération token OAuth2 ($role_label). Vérifier TCH_AUTH_TOKEN_URL, TCH_AUTH_CLIENT_ID et TCH_AUTH_CLIENT_SECRET si requis." "$http_code" "$output_file"
  fi

  extract_access_token "$output_file"
}

resolve_token_if_missing() {
  local token_name="$1"
  local username="$2"
  local password="$3"
  local role_label="$4"
  local current_token="$5"
  if [[ -n "$current_token" ]]; then
    print -r -- "$current_token"
    return
  fi
  if [[ -z "$username" || -z "$password" ]]; then
    print -r -- ""
    return
  fi
  if [[ -z "$AUTH_TOKEN_URL" || -z "$AUTH_CLIENT_ID" ]]; then
    print -u2 -- "[ERREUR] $token_name absent. Fournir le token directement ou définir TCH_AUTH_TOKEN_URL et TCH_AUTH_CLIENT_ID."
    print -r -- ""
    return
  fi

  print -u2 -- "==> Récupération automatique token $token_name via OAuth2"
  fetch_oauth_token "$username" "$password" "$role_label"
}

http_request() {
  local method="$1"
  local url="$2"
  local token="$3"
  local output_file="$4"
  local payload="${5:-}"
  local http_code=''

  local -a args
  args=(
    -sS
    -o "$output_file"
    -w '%{http_code}'
    -X "$method"
    "$url"
    -H "Authorization: Bearer $token"
    -H 'Accept: application/json'
  )

  if [[ -n "$payload" ]]; then
    args+=(
      -H 'Content-Type: application/json'
      --data "$payload"
    )
  fi

  if ! http_code="$(curl "${args[@]}")"; then
    print -u2 -- "[ERREUR] Appel réseau échoué: $method $url"
    exit 1
  fi

  print -r -- "$http_code"
}

fail_with_response() {
  local message="$1"
  local http_code="$2"
  local output_file="$3"

  print -u2 -- "[ERREUR] $message (HTTP $http_code)"
  if [[ -s "$output_file" ]]; then
    print -u2 -- "----- réponse -----"
    cat "$output_file" >&2
    print -u2 -- ""
    print -u2 -- "-------------------"
  fi
  exit 1
}

print_open_draws() {
  local file_path="$1"
  python3 - "$file_path" <<'PY'
import json
import sys
from pathlib import Path

payload = json.loads(Path(sys.argv[1]).read_text())
data_payload = payload.get("data") or {}
content = data_payload.get("items") or data_payload.get("content") or []
open_rows = [
    row for row in content
    if row.get("status") == "OPEN"
]
print(len(content))
print(len(open_rows))
for row in open_rows:
    channel = (row.get("channel") or {}).get("code") or "?"
    draw_id = row.get("id") or "?"
    scheduled = row.get("scheduledAt") or "?"
    print(f"{channel}\t{draw_id}\t{scheduled}")
PY
}

SUPER_ADMIN_TOKEN="$(resolve_token_if_missing 'TCH_SUPER_ADMIN_TOKEN' "$SUPER_ADMIN_USERNAME" "$SUPER_ADMIN_PASSWORD" 'super-admin' "$SUPER_ADMIN_TOKEN")"
SELLER_TOKEN="$(resolve_token_if_missing 'TCH_SELLER_TOKEN' "$SELLER_USERNAME" "$SELLER_PASSWORD" 'seller' "$SELLER_TOKEN")"

require_env 'TCH_SUPER_ADMIN_TOKEN (ou TCH_SUPER_ADMIN_USERNAME/TCH_SUPER_ADMIN_PASSWORD)' "$SUPER_ADMIN_TOKEN"
require_env 'TCH_SELLER_TOKEN (ou TCH_SELLER_USERNAME/TCH_SELLER_PASSWORD)' "$SELLER_TOKEN"

print -- "==> Phase 1 · génération des tirages (${GENERATE_DAYS} jours)"
generate_from="$(today_utc_date)"
generate_to="$(date_plus_days "$generate_from" "$((GENERATE_DAYS - 1))")"
generate_file="$TMP_DIR/generate-draws.json"
generate_status="$(http_request \
  POST \
  "$BASE_URL/platform/ops/draws/generate" \
  "$SUPER_ADMIN_TOKEN" \
  "$generate_file" \
  "{\"tenantId\":\"$TENANT_ID\",\"from\":\"$generate_from\",\"to\":\"$generate_to\",\"dryRun\":false,\"force\":false,\"reason\":\"e2e phase1 bootstrap\"}")"
if [[ "$generate_status" != "200" ]]; then
  fail_with_response "Impossible de générer les tirages ($generate_from → $generate_to)" "$generate_status" "$generate_file"
fi
generated_created="$(json_try_get "$generate_file" 'data.created' '?')"
generated_existing="$(json_try_get "$generate_file" 'data.alreadyExists' '?')"
generated_skipped="$(json_try_get "$generate_file" 'data.skipped' '?')"
print -- "    Tirages ($generate_from → $generate_to): créés=$generated_created déjà_existants=$generated_existing skipped=$generated_skipped"

print -- "==> Phase 1 · ouverture des tirages du jour"
open_today_status="$((0))"
open_today_file="$TMP_DIR/open-today.json"
open_today_status="$(http_request \
  POST \
  "$BASE_URL/platform/ops/draws/open-today" \
  "$SUPER_ADMIN_TOKEN" \
  "$open_today_file" \
  '{"dryRun":false}')"
if [[ "$open_today_status" != "200" ]]; then
  fail_with_response 'Impossible d’ouvrir les tirages du jour' "$open_today_status" "$open_today_file"
fi
opened_count="$(json_try_get "$open_today_file" 'data.opened' '?')"
opened_skipped="$(json_try_get "$open_today_file" 'data.skippedLocked' '?')"
opened_late="$(json_try_get "$open_today_file" 'data.skippedTooLateOrCutoffPassed' '?')"
print -- "    Tirages ouverts: $opened_count (déjà verrouillés=$opened_skipped, trop tard=$opened_late)"

print -- "==> Vérification de la session POS courante"
current_session_file="$TMP_DIR/current-session.json"
current_session_status="$(http_request \
  GET \
  "$BASE_URL/tenant/sessions/current?terminalId=$TERMINAL_ID" \
  "$SELLER_TOKEN" \
  "$current_session_file")"

SESSION_ID=''
case "$current_session_status" in
  200)
    SESSION_ID="$(json_try_get "$current_session_file" 'data.id')"
    if [[ -z "$SESSION_ID" ]]; then
      fail_with_response 'Session courante introuvable dans la réponse JSON' "$current_session_status" "$current_session_file"
    fi
    print -- "    Session déjà ouverte: $SESSION_ID"
    ;;
  204)
    print -- "    Aucune session ouverte, création en cours"
    open_session_file="$TMP_DIR/open-session.json"
    open_session_payload=$(cat <<JSON
{"outletId":"$OUTLET_ID","terminalId":"$TERMINAL_ID","openingFloat":$OPENING_FLOAT}
JSON
)
    open_session_status="$(http_request \
      POST \
      "$BASE_URL/tenant/sessions/open" \
      "$SELLER_TOKEN" \
      "$open_session_file" \
      "$open_session_payload")"
    if [[ "$open_session_status" != "201" ]]; then
      fail_with_response 'Impossible d’ouvrir la session POS' "$open_session_status" "$open_session_file"
    fi
    SESSION_ID="$(json_try_get "$open_session_file" 'data.sessionId')"
    if [[ -z "$SESSION_ID" ]]; then
      fail_with_response 'Session créée mais sessionId absent de la réponse' "$open_session_status" "$open_session_file"
    fi
    print -- "    Session créée: $SESSION_ID"
    ;;
  *)
    fail_with_response 'Échec de lecture de la session POS courante' "$current_session_status" "$current_session_file"
    ;;
esac

print -- "==> Listing des tirages du jour"
draws_today_file="$TMP_DIR/draws-today.json"
draws_today_status="$(http_request \
  GET \
  "$BASE_URL/admin/draws/today?page=0&size=$DRAWS_PAGE_SIZE&sort=scheduledAt,asc" \
  "$SUPER_ADMIN_TOKEN" \
  "$draws_today_file")"
if [[ "$draws_today_status" != "200" ]]; then
  fail_with_response 'Impossible de lister les tirages du jour' "$draws_today_status" "$draws_today_file"
fi

mapfile=("${(@f)$(print_open_draws "$draws_today_file")}")
total_draws="${mapfile[1]:-0}"
open_draws="${mapfile[2]:-0}"
print -- "    Total tirages du jour: $total_draws"
print -- "    Tirages OPEN: $open_draws"
if (( open_draws > 0 )); then
  local_index=3
  while (( local_index <= ${#mapfile} )); do
    IFS=$'\t' read -r channel_code draw_id scheduled_at <<< "${mapfile[$local_index]}"
    print -- "      - $channel_code | $draw_id | $scheduled_at"
    (( local_index++ ))
  done
else
  print -- "      (aucun tirage OPEN dans la page retournée)"
fi

print -- ""
print -- "==> Exports pour la phase 2"
print -- "export TCH_BASE_URL=\"$BASE_URL\""
print -- "export TCH_TENANT_ID=\"$TENANT_ID\""
print -- "export TCH_OUTLET_ID=\"$OUTLET_ID\""
print -- "export TCH_TERMINAL_ID=\"$TERMINAL_ID\""
print -- "export TCH_SESSION_ID=\"$SESSION_ID\""
