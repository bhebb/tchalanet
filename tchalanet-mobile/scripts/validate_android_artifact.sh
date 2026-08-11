#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat >&2 <<'USAGE'
Usage:
  validate_android_artifact.sh --apk PATH --expected-package PACKAGE --api-base-url URL

Validates a distributable Android APK before it can leave CI.
USAGE
}

apk_path=""
expected_package=""
api_base_url=""

while [ "$#" -gt 0 ]; do
  case "$1" in
    --apk)
      apk_path="${2:-}"
      shift 2
      ;;
    --expected-package)
      expected_package="${2:-}"
      shift 2
      ;;
    --api-base-url)
      api_base_url="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 2
      ;;
  esac
done

[ -n "$apk_path" ] || { echo "Missing --apk" >&2; exit 2; }
[ -n "$expected_package" ] || { echo "Missing --expected-package" >&2; exit 2; }
[ -n "$api_base_url" ] || { echo "Missing --api-base-url" >&2; exit 2; }

if [ ! -s "$apk_path" ]; then
  echo "APK does not exist or is empty: $apk_path" >&2
  exit 1
fi

case "$api_base_url" in
  http://localhost*|https://localhost*|http://127.*|https://127.*|http://10.0.2.2*|https://10.0.2.2*)
    echo "Refusing to distribute a mobile build pointed at a local API: $api_base_url" >&2
    exit 1
    ;;
esac

if ! zip -T "$apk_path" >/dev/null; then
  echo "APK zip integrity check failed: $apk_path" >&2
  exit 1
fi

find_build_tool() {
  local name="$1"
  if command -v "$name" >/dev/null 2>&1; then
    command -v "$name"
    return
  fi
  if [ -n "${ANDROID_HOME:-}" ]; then
    find "$ANDROID_HOME/build-tools" -type f -name "$name" 2>/dev/null | sort -V | tail -n 1
  fi
}

aapt_bin="$(find_build_tool aapt)"
apksigner_bin="$(find_build_tool apksigner)"

[ -n "$aapt_bin" ] || { echo "Android build tool 'aapt' was not found" >&2; exit 1; }
[ -n "$apksigner_bin" ] || { echo "Android build tool 'apksigner' was not found" >&2; exit 1; }

"$apksigner_bin" verify --verbose --print-certs "$apk_path" >/tmp/tchalanet-apksigner.txt

if ! grep -q "Verified using v" /tmp/tchalanet-apksigner.txt; then
  echo "APK signature verification did not report a verified signing scheme" >&2
  cat /tmp/tchalanet-apksigner.txt >&2
  exit 1
fi

badging="$("$aapt_bin" dump badging "$apk_path")"
package_name="$(printf '%s\n' "$badging" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")"
version_code="$(printf '%s\n' "$badging" | sed -n "s/^package: .*versionCode='\([^']*\)'.*/\1/p")"
version_name="$(printf '%s\n' "$badging" | sed -n "s/^package: .*versionName='\([^']*\)'.*/\1/p")"

if [ "$package_name" != "$expected_package" ]; then
  echo "Unexpected package name. expected=$expected_package actual=$package_name" >&2
  exit 1
fi

if ! printf '%s' "$version_code" | grep -Eq '^[1-9][0-9]*$'; then
  echo "Invalid Android versionCode: $version_code" >&2
  exit 1
fi

if [ -z "$version_name" ]; then
  echo "Missing Android versionName" >&2
  exit 1
fi

if ! printf '%s\n' "$badging" | grep -q "^launchable-activity:"; then
  echo "APK has no launchable activity" >&2
  exit 1
fi

echo "Android artifact guard passed: package=$package_name versionName=$version_name versionCode=$version_code"
