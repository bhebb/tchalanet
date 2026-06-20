#!/usr/bin/env zsh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PHASE1_SCRIPT="$SCRIPT_DIR/e2e-phase1-bootstrap.sh"

ENV_FILE="${ENV_FILE:-$SCRIPT_DIR/.env}"
if [[ -f "$ENV_FILE" ]]; then
  set -a
  source "$ENV_FILE"
  set +a
fi

# Mappage des variables d'environnement
# Accepte TCH_BASE_URL ou TCH_E2E_API_BASE_URL, etc.
BASE_URL="${TCH_BASE_URL:-${TCH_E2E_API_BASE_URL:-http://localhost:8080/api/v1}}"
SELLER_TOKEN="${TCH_SELLER_TOKEN:-}"
SUPER_ADMIN_TOKEN="${TCH_SUPER_ADMIN_TOKEN:-}"
AUTH_TOKEN_URL="${TCH_AUTH_TOKEN_URL:-}"
AUTH_CLIENT_ID="${TCH_AUTH_CLIENT_ID:-}"
AUTH_CLIENT_SECRET="${TCH_AUTH_CLIENT_SECRET:-}"
SUPER_ADMIN_USERNAME="${TCH_SUPER_ADMIN_USERNAME:-}"
SUPER_ADMIN_PASSWORD="${TCH_SUPER_ADMIN_PASSWORD:-}"
SELLER_USERNAME="${TCH_SELLER_USERNAME:-}"
SELLER_PASSWORD="${TCH_SELLER_PASSWORD:-}"
TENANT_ID="${TCH_TENANT_ID:-00000000-0000-0000-0000-000000000003}"
OUTLET_ID="${TCH_OUTLET_ID:-00000000-0000-0000-0000-000000003001}"
TERMINAL_ID="${TCH_TERMINAL_ID:-00000000-0000-0000-0000-000000003101}"
SESSION_ID="${TCH_SALES_SESSION_ID:-${TCH_SESSION_ID:-}}"
DRAW_LIMIT="${TCH_DRAW_LIMIT:-20}"
CURRENCY="${TCH_CURRENCY:-CAD}"

# Conversion centimes vers décimal si TCH_STAKE_CENTS fourni
STAKE_CENTS="${TCH_STAKE_CENTS:-}"
if [[ -n "$STAKE_CENTS" && "$STAKE_CENTS" =~ ^[0-9]+$ ]]; then
  STAKE="$(awk "BEGIN {printf \"%.2f\", $STAKE_CENTS / 100}")"
else
  STAKE="${TCH_STAKE:-1.00}"
fi

GAME_CODE="${TCH_GAME_CODE:-HT_BOLET}"
BET_TYPE="${TCH_BET_TYPE:-MATCH_1_2D}"
GAME_PROFILES_RAW="${TCH_GAME_PROFILES:-}"
SELECTION_PLAN_RAW="${TCH_SELECTION_PLAN:-}"
SELECTIONS_PER_TICKET="${TCH_SELECTIONS_PER_TICKET:-3}"
ARTIFACT_DIR="${TCH_ARTIFACT_DIR:-${TCH_E2E_OUTPUT_DIR:-$ROOT_DIR/target/e2e/tickets}}"
SELL_MODE_RAW="${TCH_SELL_MODE:-ONE_TICKET_PER_GAME}"

TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/tch-e2e-phase2.XXXXXX")"
trap 'rm -rf "$TMP_DIR"' EXIT
mkdir -p "$ARTIFACT_DIR"

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
  local value=''
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

file_uri() {
  local file_path="$1"
  python3 - "$file_path" <<'PY'
from pathlib import Path
import sys

print(Path(sys.argv[1]).resolve().as_uri())
PY
}

build_ticket_lines_json() {
  local ticket_index="$1"
  local game_code="$2"
  local bet_type="$3"
  local selection_count="${4:-$SELECTIONS_PER_TICKET}"
  local bet_option="${5:-}"
  local -a lines=()
  local line_number=1
  local selection=''
  local bet_option_json=''
  if [[ -n "$bet_option" ]]; then
    bet_option_json=",\"betOption\":$bet_option"
  fi
  while (( line_number <= selection_count )); do
    selection="$(build_random_selection_for_bet_type "$ticket_index" "$line_number" "$bet_type")"
    lines+=("{\"gameCode\":\"$game_code\",\"selection\":\"$selection\",\"stake\":$STAKE,\"betType\":\"$bet_type\"$bet_option_json}")
    (( line_number += 1 ))
  done
  print -r -- "${(j:,:)lines}"
}

random_2d() {
  print -r -- "$(printf '%02d' "$((RANDOM % 100))")"
}

build_random_selection_for_bet_type() {
  local ticket_index="$1"
  local line_number="$2"
  local bet_type="$3"
  local left=''
  local right=''
  local value=''
  case "$bet_type" in
    MATCH_1_2D)
      random_2d
      ;;
    MARRIAGE_2D2D)
      left="$(random_2d)"
      right="$(random_2d)"
      print -r -- "$left-$right"
      ;;
    LOTTO3_3D)
      # Respecte la demande 00..99 en l'encodant sur 3 digits attendus par LOTTO3.
      print -r -- "0$(random_2d)"
      ;;
    LOTTO4_PATTERN)
      print -r -- "$(printf '%02d%02d' "$((RANDOM % 100))" "$((RANDOM % 100))")"
      ;;
    LOTTO5_PATTERN)
      print -r -- "$(printf '%03d%02d' "$((RANDOM % 1000))" "$((RANDOM % 100))")"
      ;;
    *)
      value=$(( ((ticket_index - 1) * SELECTIONS_PER_TICKET + line_number) % 90 + 10 ))
      print -r -- "$(printf '%02d' "$value")"
      ;;
  esac
}

resolve_game_profile() {
  local raw="$1"
  local normalized="${raw:u}"
  normalized="${normalized//[[:space:]]/}"
  case "$normalized" in
    BOLET|HT_BOLET)
      print -r -- 'HT_BOLET:MATCH_1_2D'
      ;;
    MARYAJ|HT_MARYAJ)
      print -r -- 'HT_MARYAJ:MARRIAGE_2D2D'
      ;;
    2X2|MARYAJ2X2|MARIAGE|MARRIAGE)
      print -r -- 'HT_MARYAJ:MARRIAGE_2D2D'
      ;;
    LOTO|HT_LOTO|HT_LOTO3)
      print -r -- 'HT_LOTO3:LOTTO3_3D'
      ;;
    LOTO3)
      print -r -- 'HT_LOTO3:LOTTO3_3D'
      ;;
    LOTO4|HT_LOTO4)
      print -r -- 'HT_LOTO4:LOTTO4_PATTERN:1'
      ;;
    LOTO5|HT_LOTO5)
      print -r -- 'HT_LOTO5:LOTTO5_PATTERN:1'
      ;;
    *:*)
      print -r -- "$normalized"
      ;;
    *)
      print -u2 -- "[ERREUR] Profil jeu non supporté: $raw"
      print -u2 -- "         Utiliser BOLET, MARYAJ/2X2, LOTO3, LOTO4, LOTO5 ou GAME:BET_TYPE[:BET_OPTION]"
      exit 1
      ;;
  esac
}

resolve_sell_mode() {
  local raw="$1"
  local normalized="${raw:u}"
  normalized="${normalized//-/_}"
  normalized="${normalized//[[:space:]]/}"
  case "$normalized" in
    ONE_TICKET_PER_GAME)
      print -r -- 'ONE_TICKET_PER_GAME'
      ;;
    SINGLE_TICKET_MULTI_GAME)
      print -r -- 'SINGLE_TICKET_MULTI_GAME'
      ;;
    *)
      print -u2 -- "[ERREUR] TCH_SELL_MODE non supporté: $raw"
      print -u2 -- "         Valeurs supportées: ONE_TICKET_PER_GAME, SINGLE_TICKET_MULTI_GAME"
      exit 1
      ;;
  esac
}

build_game_profiles() {
  local raw="$1"
  local -a profiles=()
  local -a input_parts=()
  local part=''
  if [[ -n "$raw" ]]; then
    input_parts=("${(@s:,:)raw}")
    for part in "${input_parts[@]}"; do
      [[ -z "${part//[[:space:]]/}" ]] && continue
      profiles+=("$(resolve_game_profile "$part")")
    done
  fi

  if (( ${#profiles[@]} == 0 )); then
    profiles+=("$(resolve_game_profile "$GAME_CODE:$BET_TYPE")")
  fi

  local profile=''
  for profile in "${profiles[@]}"; do
    print -r -- "$profile"
  done
}

build_selection_plan() {
  local raw="$1"
  local profile_raw=''
  local count_raw=''
  local resolved=''
  local total=0
  local -a input_parts=()

  if [[ -z "$raw" ]]; then
    return
  fi

  input_parts=("${(@s:,:)raw}")
  for part in "${input_parts[@]}"; do
    part="${part//[[:space:]]/}"
    [[ -z "$part" ]] && continue
    if [[ "$part" != *=* ]]; then
      print -u2 -- "[ERREUR] Entrée TCH_SELECTION_PLAN invalide: $part"
      print -u2 -- "         Format attendu: BOLET=2,MARYAJ=3,LOTO3=2,LOTO4=2,LOTO5=1"
      exit 1
    fi
    profile_raw="${part%%=*}"
    count_raw="${part#*=}"
    if [[ ! "$count_raw" =~ '^[0-9]+$' || "$count_raw" == "0" ]]; then
      print -u2 -- "[ERREUR] Nombre de sélections invalide dans TCH_SELECTION_PLAN: $part"
      exit 1
    fi
    resolved="$(resolve_game_profile "$profile_raw")"
    print -r -- "$resolved|$count_raw"
    total=$(( total + count_raw ))
  done

  if (( total != 10 )); then
    print -u2 -- "[ERREUR] TCH_SELECTION_PLAN doit totaliser 10 sélections; total actuel: $total"
    exit 1
  fi
}

http_request() {
  local method="$1"
  local url="$2"
  local token="$3"
  local output_file="$4"
  local payload="$5"
  shift 5
  local http_code=''
  local -a extra_headers=("$@")
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

  local header=''
  for header in "${extra_headers[@]}"; do
    args+=( -H "$header" )
  done

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

download_binary() {
  local url="$1"
  local token="$2"
  local output_file="$3"
  shift 3
  local http_code=''
  local -a extra_headers=("$@")
  local -a args
  args=(
    -sS
    -o "$output_file"
    -w '%{http_code}'
    "$url"
    -H "Authorization: Bearer $token"
  )

  local header=''
  for header in "${extra_headers[@]}"; do
    args+=( -H "$header" )
  done

  if ! http_code="$(curl "${args[@]}")"; then
    print -u2 -- "[ERREUR] Téléchargement échoué: $url"
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

write_base64_to_file() {
  local source_file="$1"
  local dotted_path="$2"
  local output_file="$3"
  python3 - "$source_file" "$dotted_path" "$output_file" <<'PY'
import base64
import json
import sys
from pathlib import Path

payload = json.loads(Path(sys.argv[1]).read_text())
path = [part for part in sys.argv[2].split('.') if part]
current = payload
for part in path:
    if isinstance(current, list):
        current = current[int(part)]
    else:
        current = current[part]
if not current:
    raise SystemExit(4)
Path(sys.argv[3]).write_bytes(base64.b64decode(current))
PY
}

parse_sellable_draws() {
  local source_file="$1"
  python3 - "$source_file" <<'PY'
import json
import sys
from pathlib import Path

payload = json.loads(Path(sys.argv[1]).read_text())
rows = payload.get("data") or []
print(len(rows))
for row in rows:
    print("\t".join([
        row.get("drawId") or "",
        row.get("channelCode") or "",
        row.get("channelLabel") or "",
        row.get("scheduledAt") or "",
    ]))
PY
}

generate_missing_draws_for_seven_days() {
  require_env 'TCH_SUPER_ADMIN_TOKEN (ou TCH_SUPER_ADMIN_USERNAME/TCH_SUPER_ADMIN_PASSWORD)' "$SUPER_ADMIN_TOKEN"
  require_env 'TCH_TENANT_ID' "$TENANT_ID"

  local from_date
  local to_date
  local generate_file
  local open_file
  local generate_status
  local open_status

  from_date="$(today_utc_date)"
  to_date="$(date_plus_days "$from_date" 6)"

  print -- "==> Aucun draw vendable: régénération des draws sur 7 jours ($from_date → $to_date)"

  generate_file="$TMP_DIR/generate-draws.json"
  generate_status="$(http_request \
    POST \
    "$BASE_URL/platform/ops/draws/generate" \
    "$SUPER_ADMIN_TOKEN" \
    "$generate_file" \
    "{\"tenantId\":\"$TENANT_ID\",\"from\":\"$from_date\",\"to\":\"$to_date\",\"dryRun\":false,\"force\":true,\"reason\":\"e2e phase2 fallback generation\"}")"
  if [[ "$generate_status" != "200" ]]; then
    fail_with_response 'Impossible de régénérer les draws sur 7 jours' "$generate_status" "$generate_file"
  fi

  open_file="$TMP_DIR/open-today-after-generate.json"
  open_status="$(http_request \
    POST \
    "$BASE_URL/platform/ops/draws/open-today" \
    "$SUPER_ADMIN_TOKEN" \
    "$open_file" \
    "{\"drawDate\":\"$from_date\",\"limit\":10000,\"dryRun\":false}")"
  if [[ "$open_status" != "200" ]]; then
    fail_with_response 'Impossible d’ouvrir les draws du jour après régénération' "$open_status" "$open_file"
  fi
}

bootstrap_if_needed() {
  if [[ -n "$SESSION_ID" ]]; then
    return
  fi

  require_env 'TCH_SUPER_ADMIN_TOKEN (ou TCH_SUPER_ADMIN_USERNAME/TCH_SUPER_ADMIN_PASSWORD)' "$SUPER_ADMIN_TOKEN"
  print -- "==> Phase 2 · bootstrap automatique via phase 1"
  local bootstrap_log="$TMP_DIR/phase1-bootstrap.log"
  zsh "$PHASE1_SCRIPT" > "$bootstrap_log"
  local exports=''
  exports="$(grep '^export TCH_' "$bootstrap_log" || true)"
  if [[ -z "$exports" ]]; then
    print -u2 -- "[ERREUR] Impossible de récupérer les exports de phase 1"
    cat "$bootstrap_log" >&2
    exit 1
  fi
  eval "$exports"
  BASE_URL="${TCH_BASE_URL:-$BASE_URL}"
  OUTLET_ID="${TCH_OUTLET_ID:-$OUTLET_ID}"
  TERMINAL_ID="${TCH_TERMINAL_ID:-$TERMINAL_ID}"
  SESSION_ID="${TCH_SESSION_ID:-$SESSION_ID}"
  print -- "    Session bootstrapée: $SESSION_ID"
}

SELLER_TOKEN="$(resolve_token_if_missing 'TCH_SELLER_TOKEN' "$SELLER_USERNAME" "$SELLER_PASSWORD" 'seller' "$SELLER_TOKEN")"
SUPER_ADMIN_TOKEN="$(resolve_token_if_missing 'TCH_SUPER_ADMIN_TOKEN' "$SUPER_ADMIN_USERNAME" "$SUPER_ADMIN_PASSWORD" 'super-admin' "$SUPER_ADMIN_TOKEN")"

require_env 'TCH_SELLER_TOKEN (ou TCH_SELLER_USERNAME/TCH_SELLER_PASSWORD)' "$SELLER_TOKEN"
bootstrap_if_needed
require_env 'TCH_SESSION_ID' "$SESSION_ID"
require_env 'TCH_TERMINAL_ID' "$TERMINAL_ID"
require_env 'TCH_OUTLET_ID' "$OUTLET_ID"
if (( SELECTIONS_PER_TICKET < 1 )); then
  print -u2 -- "[ERREUR] TCH_SELECTIONS_PER_TICKET doit être >= 1"
  exit 1
fi

if [[ -n "$SELECTION_PLAN_RAW" ]]; then
  game_profiles=("${(@f)$(build_selection_plan "$SELECTION_PLAN_RAW")}")
else
  game_profiles=("${(@f)$(build_game_profiles "$GAME_PROFILES_RAW")}")
fi
if (( ${#game_profiles[@]} == 0 )); then
  print -u2 -- "[ERREUR] Aucun profil jeu résolu"
  exit 1
fi

SELL_MODE="$(resolve_sell_mode "$SELL_MODE_RAW")"

print -- "==> Profils jeux utilisés"
for profile in "${game_profiles[@]}"; do
  if [[ "$profile" == *"|"* ]]; then
    profile_selection_count="${profile#*|}"
    profile="${profile%%|*}"
  else
    profile_selection_count="$SELECTIONS_PER_TICKET"
  fi
  profile_parts=("${(@s/:/)profile}")
  game_code="${profile_parts[1]}"
  bet_type="${profile_parts[2]}"
  bet_option="${profile_parts[3]:-}"
  selection_count="$profile_selection_count"
  if [[ -n "$bet_option" ]]; then
    print -- "    - $game_code:$bet_type betOption=$bet_option sélections=$selection_count"
  else
    print -- "    - $game_code:$bet_type sélections=$selection_count"
  fi
done
print -- "==> Mode de vente"
print -- "    - $SELL_MODE"

print -- "==> Récupération des draws vendables"
sellable_file="$TMP_DIR/sellable-draws.json"
sellable_status="$(http_request \
  GET \
  "$BASE_URL/tenant/cashier/draws/sellable?limit=$DRAW_LIMIT" \
  "$SELLER_TOKEN" \
  "$sellable_file" \
  '')"
if [[ "$sellable_status" != "200" ]]; then
  fail_with_response 'Impossible de lister les draws vendables cashier' "$sellable_status" "$sellable_file"
fi

sellable_lines=("${(@f)$(parse_sellable_draws "$sellable_file")}")
sellable_count="${sellable_lines[1]:-0}"
if (( sellable_count == 0 )); then
  generate_missing_draws_for_seven_days

  sellable_status="$(http_request \
    GET \
    "$BASE_URL/tenant/cashier/draws/sellable?limit=$DRAW_LIMIT" \
    "$SELLER_TOKEN" \
    "$sellable_file" \
    '')"
  if [[ "$sellable_status" != "200" ]]; then
    fail_with_response 'Impossible de relister les draws vendables cashier après régénération' "$sellable_status" "$sellable_file"
  fi

  sellable_lines=("${(@f)$(parse_sellable_draws "$sellable_file")}")
  sellable_count="${sellable_lines[1]:-0}"
  if (( sellable_count == 0 )); then
    print -u2 -- "[ERREUR] Aucun draw OPEN vendable après régénération sur 7 jours"
    exit 1
  fi
fi
print -- "    Draws vendables: $sellable_count"

integer index=0
integer sold_count=0
while (( index + 2 <= ${#sellable_lines} )); do
  (( index += 1 ))
  IFS=$'\t' read -r draw_id channel_code channel_label scheduled_at <<< "${sellable_lines[$((index + 1))]}"
  if [[ "$SELL_MODE" == "SINGLE_TICKET_MULTI_GAME" ]]; then
    combined_lines=()
    profiles_label=''
    for game_profile in "${game_profiles[@]}"; do
      if [[ "$game_profile" == *"|"* ]]; then
        selection_count="${game_profile#*|}"
        game_profile="${game_profile%%|*}"
      else
        selection_count="$SELECTIONS_PER_TICKET"
      fi
      profile_parts=("${(@s/:/)game_profile}")
      game_code="${profile_parts[1]}"
      bet_type="${profile_parts[2]}"
      bet_option="${profile_parts[3]:-}"
      ticket_lines_json="$(build_ticket_lines_json "$index" "$game_code" "$bet_type" "$selection_count" "$bet_option")"
      combined_lines+=("${ticket_lines_json}")
      if [[ -z "$profiles_label" ]]; then
        profiles_label="$game_code/$bet_type($selection_count)"
      else
        profiles_label="$profiles_label + $game_code/$bet_type($selection_count)"
      fi
    done

    sell_response_file="$TMP_DIR/sell-$index-multi-game.json"
    all_ticket_lines_json="${(j:,:)combined_lines}"
    sell_payload=$(cat <<JSON
{"terminalId":"$TERMINAL_ID","drawId":"$draw_id","currency":"$CURRENCY","lines":[${all_ticket_lines_json}],"printFormat":"PDF"}
JSON
)

    print -- "==> Vente draw $channel_code ($draw_id) · ticket unique multi-jeux"
    sell_status="$(http_request \
      POST \
      "$BASE_URL/tenant/cashier/sell" \
      "$SELLER_TOKEN" \
      "$sell_response_file" \
      "$sell_payload" \
      "X-Tch-Terminal-Id: $TERMINAL_ID" \
      "X-Tch-Outlet-Id: $OUTLET_ID" \
      "X-Tch-Sales-Session-Id: $SESSION_ID")"
    if [[ "$sell_status" != "201" ]]; then
      fail_with_response "Impossible de vendre un ticket multi-jeux pour $channel_code" "$sell_status" "$sell_response_file"
    fi

    outcome="$(json_try_get "$sell_response_file" 'data.outcome')"
    ticket_id="$(json_try_get "$sell_response_file" 'data.ticket.id')"
    ticket_code="$(json_try_get "$sell_response_file" 'data.ticket.ticketCode' "$channel_code-MULTI-$index")"
    if [[ "$outcome" == "PENDING_APPROVAL" ]]; then
      fail_with_response "Ticket multi-jeux en attente d’approbation pour $channel_code, PDF non généré" "$sell_status" "$sell_response_file"
    fi
    if [[ -z "$ticket_id" ]]; then
      fail_with_response "ticket.id manquant pour ticket multi-jeux $channel_code" "$sell_status" "$sell_response_file"
    fi

    response_pdf_path="$ARTIFACT_DIR/${ticket_code}.sell-response.pdf"
    write_base64_to_file "$sell_response_file" 'data.receipt.base64' "$response_pdf_path"
    if [[ ! -s "$response_pdf_path" ]]; then
      print -u2 -- "[ERREUR] PDF base64 vide pour $ticket_code"
      exit 1
    fi

    endpoint_pdf_path="$ARTIFACT_DIR/${ticket_code}.print.pdf"
    endpoint_pdf_uri="$(file_uri "$endpoint_pdf_path")"
    response_pdf_uri="$(file_uri "$response_pdf_path")"
    endpoint_status="$(download_binary \
      "$BASE_URL/tenant/tickets/$ticket_id/print.pdf" \
      "$SELLER_TOKEN" \
      "$endpoint_pdf_path" \
      'Accept: application/pdf')"
    if [[ "$endpoint_status" != "200" ]]; then
      fail_with_response "Impossible de télécharger le PDF print.pdf pour $ticket_code" "$endpoint_status" "$endpoint_pdf_path"
    fi
    if [[ ! -s "$endpoint_pdf_path" ]]; then
      print -u2 -- "[ERREUR] Fichier PDF vide depuis print.pdf pour $ticket_code"
      exit 1
    fi

    cp "$sell_response_file" "$ARTIFACT_DIR/${ticket_code}.sell.json"
    print -- "    Ticket vendu: $ticket_code"
    print -- "    Profils jeux: $profiles_label"
    print -- "    PDF sell response: $response_pdf_path"
    print -- "    PDF sell response link: $response_pdf_uri"
    print -- "    PDF print endpoint: $endpoint_pdf_path"
    print -- "    PDF print endpoint link: $BASE_URL/tenant/tickets/$ticket_id/print.pdf"
    print -- "    PDF print artifact link: $endpoint_pdf_uri"
    (( sold_count += 1 ))
  else
    for game_profile in "${game_profiles[@]}"; do
      if [[ "$game_profile" == *"|"* ]]; then
        selection_count="${game_profile#*|}"
        game_profile="${game_profile%%|*}"
      else
        selection_count="$SELECTIONS_PER_TICKET"
      fi
      profile_parts=("${(@s/:/)game_profile}")
      game_code="${profile_parts[1]}"
      bet_type="${profile_parts[2]}"
      bet_option="${profile_parts[3]:-}"

      sell_response_file="$TMP_DIR/sell-$index-$game_code.json"
      ticket_lines_json="$(build_ticket_lines_json "$index" "$game_code" "$bet_type" "$selection_count" "$bet_option")"
      sell_payload=$(cat <<JSON
{"terminalId":"$TERMINAL_ID","drawId":"$draw_id","currency":"$CURRENCY","lines":[${ticket_lines_json}],"printFormat":"PDF"}
JSON
)

      print -- "==> Vente draw $channel_code ($draw_id) · $game_code/$bet_type"
      sell_status="$(http_request \
        POST \
        "$BASE_URL/tenant/cashier/sell" \
        "$SELLER_TOKEN" \
        "$sell_response_file" \
        "$sell_payload" \
        "X-Tch-Terminal-Id: $TERMINAL_ID" \
        "X-Tch-Outlet-Id: $OUTLET_ID" \
        "X-Tch-Sales-Session-Id: $SESSION_ID")"
      if [[ "$sell_status" != "201" ]]; then
        fail_with_response "Impossible de vendre un ticket pour $channel_code ($game_code/$bet_type)" "$sell_status" "$sell_response_file"
      fi

      outcome="$(json_try_get "$sell_response_file" 'data.outcome')"
      ticket_id="$(json_try_get "$sell_response_file" 'data.ticket.id')"
      ticket_code="$(json_try_get "$sell_response_file" 'data.ticket.ticketCode' "$channel_code-$game_code-$index")"
      if [[ "$outcome" == "PENDING_APPROVAL" ]]; then
        fail_with_response "Ticket en attente d’approbation pour $channel_code ($game_code/$bet_type), PDF non généré" "$sell_status" "$sell_response_file"
      fi
      if [[ -z "$ticket_id" ]]; then
        fail_with_response "ticket.id manquant pour $channel_code ($game_code/$bet_type)" "$sell_status" "$sell_response_file"
      fi

      response_pdf_path="$ARTIFACT_DIR/${ticket_code}.sell-response.pdf"
      write_base64_to_file "$sell_response_file" 'data.receipt.base64' "$response_pdf_path"
      if [[ ! -s "$response_pdf_path" ]]; then
        print -u2 -- "[ERREUR] PDF base64 vide pour $ticket_code"
        exit 1
      fi

      endpoint_pdf_path="$ARTIFACT_DIR/${ticket_code}.print.pdf"
      endpoint_pdf_uri="$(file_uri "$endpoint_pdf_path")"
      response_pdf_uri="$(file_uri "$response_pdf_path")"
      endpoint_status="$(download_binary \
        "$BASE_URL/tenant/tickets/$ticket_id/print.pdf" \
        "$SELLER_TOKEN" \
        "$endpoint_pdf_path" \
        'Accept: application/pdf')"
      if [[ "$endpoint_status" != "200" ]]; then
        fail_with_response "Impossible de télécharger le PDF print.pdf pour $ticket_code" "$endpoint_status" "$endpoint_pdf_path"
      fi
      if [[ ! -s "$endpoint_pdf_path" ]]; then
        print -u2 -- "[ERREUR] Fichier PDF vide depuis print.pdf pour $ticket_code"
        exit 1
      fi

      cp "$sell_response_file" "$ARTIFACT_DIR/${ticket_code}.sell.json"
      print -- "    Ticket vendu: $ticket_code"
      print -- "    Profil jeu: $game_code / $bet_type"
      print -- "    PDF sell response: $response_pdf_path"
      print -- "    PDF sell response link: $response_pdf_uri"
      print -- "    PDF print endpoint: $endpoint_pdf_path"
      print -- "    PDF print endpoint link: $BASE_URL/tenant/tickets/$ticket_id/print.pdf"
      print -- "    PDF print artifact link: $endpoint_pdf_uri"
      (( sold_count += 1 ))
    done
  fi
done

print -- ""
print -- "==> Résumé"
print -- "    Tickets vendus: $sold_count"
print -- "    Dossier artefacts: $ARTIFACT_DIR"
