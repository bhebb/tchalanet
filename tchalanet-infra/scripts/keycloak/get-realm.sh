#!/usr/bin/env bash
set -euo pipefail

# get-realm.sh <env>
# Génère un realm JSON à partir d'un template (realm.base.json)
# Usage: ./get-realm.sh staging

ENV="${1:-staging}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Paths
ENV_DIR="$ROOT_DIR/envs/$ENV"
TEMPLATE="${TEMPLATE:-$ROOT_DIR/keycloak/realms/templates/realm.base.json}"
OUT_DIR="${OUT_DIR:-$ROOT_DIR/keycloak/realms}"
OVERLAY_FILE="$ROOT_DIR/keycloak/realms/overlays/${ENV}.json"

# Resolve overlay candidates: prefer exact env file, but accept prod/production aliases
# Also support MERGE_OVERLAYS=1 to merge multiple overlays (applied in candidate order)
OVERLAY_DIR="$ROOT_DIR/keycloak/realms/overlays"
OVERLAY_CANDIDATES=()
# exact name first
OVERLAY_CANDIDATES+=("$OVERLAY_DIR/${ENV}.json")
# common aliases for production
if [ "$ENV" = "production" ] || [ "$ENV" = "prod" ]; then
  # prefer prod.json over production.json when both exist
  OVERLAY_CANDIDATES+=("$OVERLAY_DIR/prod.json")
fi

# Find existing overlays in candidate order
FOUND_OVERLAYS=()
for c in "${OVERLAY_CANDIDATES[@]}"; do
  if [ -f "$c" ]; then
    FOUND_OVERLAYS+=("$c")
  fi
done

if [ ${#FOUND_OVERLAYS[@]} -eq 0 ]; then
  OVERLAY_FILE=""
elif [ ${#FOUND_OVERLAYS[@]} -eq 1 ]; then
  OVERLAY_FILE="${FOUND_OVERLAYS[0]}"
else
  # multiple overlays found
  if [ "${MERGE_OVERLAYS:-0}" = "1" ]; then
    tmp_merge=$(mktemp_safe /tmp/tch-get-realm.overlay.merge.XXXXXX.json)
    add_tmp "$tmp_merge"
    # merge all found overlays in order: left-to-right overlay wins
    jq -s 'reduce .[] as $i ({}; . * $i)' "${FOUND_OVERLAYS[@]}" > "$tmp_merge"
    OVERLAY_FILE="$tmp_merge"
    echo "→ MERGE_OVERLAYS=1: merged overlays into $OVERLAY_FILE" >&2
  else
    # choose preferred overlay: prefer prod.json (explicit) over production.json
    preferred="${FOUND_OVERLAYS[0]}"
    # if prod.json present, select it; else if production.json present, select it; else keep first
    for f in "${FOUND_OVERLAYS[@]}"; do
      case "$(basename "$f")" in
        prod.json)
          preferred="$f"; break;;
        production.json)
          # select production.json only if prod.json wasn't found earlier
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

# portable mktemp helper: try GNU mktemp, macOS style, then python fallback
mktemp_safe() {
  local template="$1"
  local tmp
  # try mktemp with template (Linux/GNU)
  if tmp=$(mktemp "$template" 2>/dev/null); then
    echo "$tmp"
    return 0
  fi
  # try macOS-style mktemp -t prefix
  local prefix
  prefix=$(basename "${template%%.*}")
  if tmp=$(mktemp -t "$prefix" 2>/dev/null); then
    echo "$tmp"
    return 0
  fi
  # try python3 fallback
  if command -v python3 >/dev/null 2>&1; then
    python3 - <<'PY'
import tempfile
f=tempfile.NamedTemporaryFile(delete=False)
print(f.name)
PY
    return 0
  fi
  echo "Unable to create temporary file (mktemp/python missing)" >&2
  return 1
}

# Manage tmp files list and cleanup
TMP_FILES=()
add_tmp() { TMP_FILES+=("$1"); }
cleanup_tmp() { rm -f "${TMP_FILES[@]:-}" 2>/dev/null || true; }
trap cleanup_tmp EXIT

# Load env files from envs/common and envs/<ENV>
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

# Defaults
# Set KC_REALM based on ENV with sensible defaults, can be overridden by KC_REALM env var
if [ -z "${KC_REALM+x}" ]; then
  case "$ENV" in
    staging)
      KC_REALM="tchalanet-staging"
      ;;
    production)
      KC_REALM="tchalanet-production"
      ;;
    *)
      KC_REALM="tchalanet"
      ;;
  esac
else
  KC_REALM="$KC_REALM"
fi
DEFAULT_LOCALE="${DEFAULT_LOCALE:-fr}"
SUPPORTED_LOCALES_CSV="${SUPPORTED_LOCALES:-}"
TEST_USER_PASSWORD="${TEST_USER_PASSWORD:-changeme}"
#THEME_NAME="${KC_LOGIN_THEME:-${THEME_NAME:-tchalanet}}"

mkdir -p "$OUT_DIR"
# Always write to a single file name (user requested): tchalanet-realm.json
# The internal realm name (KC_REALM) is still set in the JSON content.
OUT_FILE="${OUT_FILE:-$OUT_DIR/tchalanet-realm.json}"

# sanity
command -v jq >/dev/null 2>&1 || { echo "✖ jq required" >&2; exit 1; }
[ -f "$TEMPLATE" ] || { echo "✖ Template not found: $TEMPLATE" >&2; exit 1; }

echo "→ Generating realm from template: $TEMPLATE" >&2

# Prepare supportedLocales JSON array
if [ -n "$SUPPORTED_LOCALES_CSV" ]; then
  # split by comma and dedupe preserving order
  IFS=',' read -r -a _locales <<< "$SUPPORTED_LOCALES_CSV"
  seen=()
  locales_json="[]"
  for l in "${_locales[@]}"; do
    # trim
    l=$(echo "$l" | xargs)
    # skip empty
    [ -z "$l" ] && continue
    # append if not seen
    if ! printf "%s\n" "${seen[@]}" | grep -qx "$l"; then
      seen+=("$l")
      locales_json=$(echo "$locales_json" | jq --arg v "$l" '. + [$v]')
    fi
  done
else
  # fallback to template's supportedLocales
  locales_json="null"
fi

# Compute client origins/redirects
APP_WEB_ORIGIN="${APP_WEB_ORIGIN:-${APP_HOST:-http://localhost:4200}}"
APP_API_HOST="${APP_API_HOST:-${API_HOST:-http://localhost:8081}}"

# helper to produce localhost variants for staging
_local_redirects() {
  local base="$1"
  # ensure base without trailing /*
  echo "$base" | sed -E 's/\/*$$//'
}

# function to build users array based on roles in template
# We'll read roles from template and create a test user per role

# Start with the template JSON, then transform with jq
jq_filter='.'

# set realm name
jq_filter="$jq_filter | .realm = \"$KC_REALM\""
# set defaultLocale
jq_filter="$jq_filter | .defaultLocale = \"$DEFAULT_LOCALE\""
# set internationalization enabled (keep template default)

# set supportedLocales if provided
if [ "$locales_json" != "null" ]; then
  locales_payload=$(echo "$locales_json")
  jq_filter="$jq_filter | .supportedLocales = $locales_payload"
fi

# Update clients: tchalanet-web and tchalanet-swagger
# For web client, set redirectUris and webOrigins to include APP_WEB_ORIGIN and localhost variants for staging
# For swagger, include swagger redirect samples

# Build jq fragment to update clients
clients_update=$(cat <<'JQ'
| .clients = (
  .clients | map(
    if .clientId == "tchalanet-web" then
      .redirectUris = [ $APP_WEB_REDIRECT ]
      | .webOrigins = [ $APP_WEB_ORIGIN ]
      | (.redirectUris += ($EXTRA_WEB_REDIRECTS // []))
      | (.webOrigins += ($EXTRA_WEB_ORIGINS // []))
    elif .clientId == "tchalanet-swagger" then
      .redirectUris = ($SWAGGER_REDIRECTS)
      | .webOrigins = ($SWAGGER_ORIGINS)
    else . end
  )
)
JQ
)

# Prepare variables for jq invocation
# Compute APP_WEB_REDIRECT (with /*)
APP_WEB_REDIRECT_VAL="${APP_WEB_ORIGIN%/}/*"
APP_WEB_ORIGIN_VAL="${APP_WEB_ORIGIN%/}"

# Extra localhost variants for staging
EXTRA_WEB_REDIRECTS='[]'
EXTRA_WEB_ORIGINS='[]'
if [ "$ENV" = "staging" ]; then
  EXTRA_WEB_REDIRECTS='["http://localhost/*","http://127.0.0.1/*"]'
  EXTRA_WEB_ORIGINS='["http://localhost","http://localhost:4200","http://127.0.0.1"]'
fi

# Swagger redirects/origins defaults
SWAGGER_REDIRECTS='["http://localhost:8081/swagger-ui/oauth2-redirect.html"]'
SWAGGER_ORIGINS='["http://localhost:8081"]'

# Now build users based on roles present in template
# We will extract realm roles and generate users with username <role>-test@local and password TEST_USER_PASSWORD
roles_json=$(jq -r '.roles.realm | map(.name) | @json' "$TEMPLATE")
# roles_json is a JSON array like ["SUPER_ADMIN","TENANT_ADMIN",...]

# Build users array JSON
users_json='[]'
for role in $(echo "$roles_json" | jq -r '.[]'); do
  # lowercase portable
  lc_role=$(echo "$role" | tr '[:upper:]' '[:lower:]')
  uname="$lc_role"
  email="$uname@local"
  pwd="$TEST_USER_PASSWORD"
  locale_attr="$DEFAULT_LOCALE"
  user_obj=$(jq -n --arg username "$uname" --arg email "$email" --arg pwd "$pwd" --arg role "$role" --arg locale "$locale_attr" '
    {username:$username, enabled:true, email:$email, emailVerified:true, credentials:[{type:"password", value:$pwd, temporary:false}], realmRoles:[$role], attributes:{locale:[$locale]}}
  ')
  users_json=$(echo "$users_json" | jq --argjson u "$user_obj" '. + [$u]')
done

## Safer jq pipeline: 3 steps
# 1) set realm, defaultLocale, theme, i18n
tmp1=$(mktemp_safe /tmp/tch-get-realm.XXXXXX.json)
add_tmp "$tmp1"
if [ "$locales_json" != "null" ]; then
  jq --arg realm "$KC_REALM" \
     --arg defaultLocale "$DEFAULT_LOCALE" \
     --argjson locales "$locales_json" \
     '.realm = $realm | .defaultLocale = $defaultLocale | .internationalizationEnabled = true | .supportedLocales = $locales' "$TEMPLATE" > "$tmp1"
else
  jq --arg realm "$KC_REALM" \
     --arg defaultLocale "$DEFAULT_LOCALE" \
     '.realm = $realm | .defaultLocale = $defaultLocale | .internationalizationEnabled = true' "$TEMPLATE" > "$tmp1"
fi

# 2) update clients (uses APP_WEB vars and extras)
tmp2=$(mktemp_safe /tmp/tch-get-realm.XXXXXX.json)
add_tmp "$tmp2"
# write json arrays to temp files to avoid shell quoting with --argjson
tmp_extra_redirects=$(mktemp_safe /tmp/tch-extra-redirects.XXXXXX.json)
add_tmp "$tmp_extra_redirects"
tmp_extra_origins=$(mktemp_safe /tmp/tch-extra-origins.XXXXXX.json)
add_tmp "$tmp_extra_origins"
tmp_swagger_redirects=$(mktemp_safe /tmp/tch-swagger-redirects.XXXXXX.json)
add_tmp "$tmp_swagger_redirects"
tmp_swagger_origins=$(mktemp_safe /tmp/tch-swagger-origins.XXXXXX.json)
add_tmp "$tmp_swagger_origins"
tmp_users=$(mktemp_safe /tmp/tch-users.XXXXXX.json)
add_tmp "$tmp_users"

echo "$EXTRA_WEB_REDIRECTS" > "$tmp_extra_redirects"
echo "$EXTRA_WEB_ORIGINS" > "$tmp_extra_origins"
echo "$SWAGGER_REDIRECTS" > "$tmp_swagger_redirects"
echo "$SWAGGER_ORIGINS" > "$tmp_swagger_origins"
echo "$users_json" > "$tmp_users"

jq_err2=$(mktemp_safe /tmp/tch-jq2.err.XXXXXX)
add_tmp "$jq_err2"
if ! jq --arg APP_WEB_REDIRECT "$APP_WEB_REDIRECT_VAL" \
   --arg APP_WEB_ORIGIN "$APP_WEB_ORIGIN_VAL" \
   --slurpfile EXTRA_WEB_REDIRECTS "$tmp_extra_redirects" \
   --slurpfile EXTRA_WEB_ORIGINS "$tmp_extra_origins" \
   --slurpfile SWAGGER_REDIRECTS "$tmp_swagger_redirects" \
   --slurpfile SWAGGER_ORIGINS "$tmp_swagger_origins" \
   '.clients = (.clients | map( if .clientId == "tchalanet-web" then (.redirectUris = [ $APP_WEB_REDIRECT ] | .webOrigins = [ $APP_WEB_ORIGIN ] | (.redirectUris += ($EXTRA_WEB_REDIRECTS[0] // [])) | (.webOrigins += ($EXTRA_WEB_ORIGINS[0] // []))) elif .clientId == "tchalanet-swagger" then (.redirectUris = $SWAGGER_REDIRECTS[0] | .webOrigins = $SWAGGER_ORIGINS[0]) else . end ))' "$tmp1" > "$tmp2" 2> "$jq_err2"; then
  echo "✖ jq (update clients) failed" >&2
  echo "--- jq stderr ---" >&2
  sed -n '1,200p' "$jq_err2" >&2 || true
  echo "--- tmp1 preview ---" >&2
  sed -n '1,120p' "$tmp1" >&2 || true
  echo "--- tmp_extra_redirects ---" >&2
  sed -n '1,120p' "$tmp_extra_redirects" >&2 || true
  exit 1
fi
rm -f "$jq_err2" || true

# 3) attach users
jq_err3=$(mktemp_safe /tmp/tch-jq3.err.XXXXXX)
add_tmp "$jq_err3"
if ! jq --slurpfile USERS "$tmp_users" '.users = $USERS[0]' "$tmp2" > "$OUT_FILE" 2> "$jq_err3"; then
  echo "✖ jq (attach users) failed" >&2
  echo "--- jq stderr ---" >&2
  sed -n '1,200p' "$jq_err3" >&2 || true
  echo "--- tmp2 preview ---" >&2
  sed -n '1,120p' "$tmp2" >&2 || true
  echo "--- tmp_users ---" >&2
  sed -n '1,120p' "$tmp_users" >&2 || true
  exit 1
fi
rm -f "$jq_err3" || true

# 4) apply overlay if exists (shallow merge: overlay wins)
if [ -f "$OVERLAY_FILE" ]; then
  tmp_overlay=$(mktemp_safe /tmp/tch-get-realm.overlay.XXXXXX.json)
  add_tmp "$tmp_overlay"
  # Merge strategy:
  # - shallow merge other top-level keys (overlay wins)
  # - for .clients, merge by clientId: take template client object and overlay client object and combine (overlay fields override, missing fields preserved)
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

# Ensure realm remains enabled by default (protect against overlays that might disable it)
# You can opt-out by setting ALLOW_DISABLED_REALM=1 in environment when running this script.
if [ "${ALLOW_DISABLED_REALM:-0}" != "1" ]; then
  tmp_force=$(mktemp_safe /tmp/tch-get-realm.force.XXXXXX.json)
  add_tmp "$tmp_force"
  jq '.enabled = true' "$OUT_FILE" > "$tmp_force"
  mv "$tmp_force" "$OUT_FILE"
  echo "→ Forced .enabled = true on $OUT_FILE (set ALLOW_DISABLED_REALM=1 to skip)" >&2
fi

# 5) ensure client scope for custom mapper and attach to clients
TCH_SCOPE_NAME="${TCH_SCOPE_NAME:-tch}"
tmp_scope=$(mktemp_safe /tmp/tch-scope.XXXXXX.json)
add_tmp "$tmp_scope"
jq -n --arg name "$TCH_SCOPE_NAME" --arg mapperId "tch-json-claim-mapper" '
{
  name: $name,
  protocol: "openid-connect",
  attributes: {
    "include.in.token.scope": "true",
    "display.on.consent.screen": "false",
    "consent.screen.text": ""
  },
  protocolMappers: [
    {
      name: "tch-json-claim",
      protocol: "openid-connect",
      protocolMapper: $mapperId,
      config: {
        "jsonType.label": "JSON",
        "id.token.claim": "true",
        "access.token.claim": "true",
        "userinfo.token.claim": "true",
        "claim.name": "tch"
      }
    }
  ]
}
' > "$tmp_scope"

tmp3=$(mktemp_safe /tmp/tch-get-realm.XXXXXX.json)
add_tmp "$tmp3"
if ! jq --slurpfile SCOPE "$tmp_scope" --arg scopeName "$TCH_SCOPE_NAME" '
  .clientScopes = (.clientScopes // [])
  | ( if (.clientScopes | map(.name) | index($scopeName)) == null then .clientScopes += $SCOPE else . end )
  | .clients = (.clients | map( .defaultClientScopes = (((.defaultClientScopes // []) + [$scopeName]) | unique) ))
' "$OUT_FILE" > "$tmp3"; then
  echo "✖ jq (attach client scope) failed" >&2; exit 1;
fi
mv "$tmp3" "$OUT_FILE"

if [ "${DEBUG:-0}" = "1" ]; then
  echo "--- DEBUG: generated OUT_FILE ---" >&2
  sed -n '1,200p' "$OUT_FILE" >&2 || true
fi

echo "✔ Realm generated to: $OUT_FILE" >&2
# Print a short summary of test users
printf "Users created per role with password '%s':\n" "$TEST_USER_PASSWORD" >&2
printf " - %s\n" $(echo "$roles_json" | jq -r '.[]' | tr '[:upper:]' '[:lower:]') >&2

exit 0
