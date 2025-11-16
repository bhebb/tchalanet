#!/usr/bin/env bash
set -euo pipefail

# create-secrets-from-env.sh
# Usage: ./scripts/doppler/create-secrets-from-env.sh envs/staging/.env --project tchalanet --config staging [--dry-run] [--all] [--include A,B] [--exclude X,Y]
# Reads a local .env file and creates/updates secrets in Doppler via `doppler secrets set`.
# By default the script only pushes keys that look like secrets (PASSWORD|PASS|TOKEN|KEY|SECRET|ADMIN|DSN|JWT).
# WARNING: This writes secrets to Doppler. Use with care.

usage(){
  cat <<EOF
Usage: $0 <env-file> --project <project> --config <config> [--dry-run] [--all] [--include KEY1,KEY2] [--exclude KEY3,KEY4]
Example: $0 envs/staging/.env --project tch-api --config staging --dry-run
Note: by default only keys matching PASSWORD|PASS|TOKEN|KEY|SECRET|ADMIN|DSN|JWT are pushed. Use --all to push every key.
EOF
  exit 1
}

if [ $# -lt 4 ]; then usage; fi

ENV_FILE="$1"; shift
PROJECT=""; CONFIG=""; DRY=0; SECRETS_ONLY=1; INCLUDE_LIST=(); EXCLUDE_LIST=()
while [ $# -gt 0 ]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --config) CONFIG="$2"; shift 2;;
    --dry-run) DRY=1; shift;;
    --all) SECRETS_ONLY=0; shift;;
    --include) ICSV="$2"; shift 2; IFS=',' read -r -a INCLUDE_LIST <<< "$ICSV";;
    --exclude) ECSV="$2"; shift 2; IFS=',' read -r -a EXCLUDE_LIST <<< "$ECSV";;
    --help|-h) usage;;
    *) echo "Unknown arg: $1"; usage;;
  esac
done

if [ ! -f "$ENV_FILE" ]; then echo "Env file not found: $ENV_FILE" >&2; exit 2; fi
if [ -z "$PROJECT" ] || [ -z "$CONFIG" ]; then echo "--project and --config are required" >&2; usage; fi

# Ensure doppler CLI present
if ! command -v doppler >/dev/null 2>&1; then
  echo "doppler CLI not found. Install it (brew install doppler) or run via docker image dopplerhq/cli" >&2
  exit 3
fi

# helper: trim spaces
trim() { echo "$1" | sed -e 's/^\s*//' -e 's/\s*$//'; }

# helper: determine if key should be pushed
is_secret_key() {
  local k="$1"
  # exact include takes precedence
  for inc in "${INCLUDE_LIST[@]:-}"; do
    [ -z "$inc" ] && continue
    if [ "$k" = "$inc" ]; then return 0; fi
  done
  # explicit exclude
  for ex in "${EXCLUDE_LIST[@]:-}"; do
    [ -z "$ex" ] && continue
    if [ "$k" = "$ex" ]; then return 1; fi
  done
  # if --all requested, accept all
  if [ "$SECRETS_ONLY" -eq 0 ]; then
    return 0
  fi
  # pattern matching (case-insensitive)
  if echo "$k" | grep -Eiq 'PASSWORD|PASS|TOKEN|KEY|SECRET|ADMIN|DSN|JWT'; then
    return 0
  fi
  return 1
}

# Read env file and create secrets
TMPFILE=$(mktemp /tmp/doppler-XXXXXX.env)
trap 'rm -f "$TMPFILE"' EXIT

grep -v '^\s*$' "$ENV_FILE" | grep -v '^\s*#' > "$TMPFILE" || true

set +u
while IFS='=' read -r key rest; do
  # Keep value as-is including '=' signs
  [ -z "$key" ] && continue
  key=$(trim "$key")
  # value may contain '=' - reconstruct
  val="${rest}"
  # If rest begins empty, try read the rest of the line (handles values containing =)
  # Remove surrounding quotes if present
  val=$(echo "$val" | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")
  # skip empty values
  if [ -z "$val" ]; then
    echo "Skipping $key (empty value)"
    continue
  fi
  if is_secret_key "$key"; then
    if [ "$DRY" -eq 1 ]; then
      echo "DRY RUN: doppler secrets set --project $PROJECT --config $CONFIG $key=***REDACTED***"
    else
      echo "Setting secret: $key"
      doppler secrets set --project "$PROJECT" --config "$CONFIG" "$key=$val"
    fi
  else
    echo "Skipping non-secret key: $key"
  fi
done < "$TMPFILE"
set -u

echo "Done. $( [ $DRY -eq 1 ] && echo '(dry-run)' )"
