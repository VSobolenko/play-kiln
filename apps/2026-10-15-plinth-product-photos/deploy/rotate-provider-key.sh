#!/usr/bin/env bash
# rotate-provider-key.sh <debug|prod> [--dry-run]
# Rotates FAL_KEY through a no-echo prompt; a failed health gate restores it.

set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
. "$HERE/lib.sh"

validate_key() { # deliberately describes violations without displaying a secret
  local key="$1" id secret
  [ -n "$key" ] || die "FAL_KEY must not be empty"
  [[ "$key" != *[[:space:]]* ]] || die "FAL_KEY must not contain whitespace"
  [[ "$key" != *\"* && "$key" != *\'* ]] || die "FAL_KEY must not contain quotes"
  [ "${#key}" -ge 32 ] && [ "${#key}" -le 200 ] || die "FAL_KEY length must be 32-200 characters"
  [[ "$key" == *:* ]] || die "FAL_KEY must look like <id>:<secret>"
  id="${key%%:*}"; secret="${key#*:}"
  [[ "$secret" != *:* ]] && [ "${#id}" -ge 16 ] && [ "${#secret}" -ge 16 ] \
    || die "FAL_KEY must contain one colon with at least 16 characters on each side"
}

ensure_distinct_key() { # <new-key> <other-service> <other-env-file>
  local other_key
  other_key="$(env_value "$3" FAL_KEY)"
  [ -n "$other_key" ] || die "$3 does not declare FAL_KEY"
  [ "$(sha12 "$1")" != "$(sha12 "$other_key")" ] \
    || die "refusing shared FAL_KEY: fingerprint matches $2"
}

# This guarded source mode exposes the two pure checks for offline tests only;
# it is not a command-line path and cannot rotate a key.
if [ "${ROTATE_PROVIDER_KEY_LIB_ONLY:-0}" = 1 ] && [ "${BASH_SOURCE[0]}" != "$0" ]; then return 0; fi

SERVICE=""
DRY_RUN=0
KEY_FILE=""

usage() {
  printf '%s\n' "usage: rotate-provider-key.sh <debug|prod> [--dry-run] [--key-file <path>] [--help]"
  exit 0
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --dry-run) DRY_RUN=1; shift ;;
    --key-file) KEY_FILE="${2:-}"; [ -n "$KEY_FILE" ] || die "--key-file needs a path"; shift 2 ;;
    --help) usage ;;
    -*) die "unknown flag: $1 (try --help)" ;;
    *) [ -z "$SERVICE" ] || die "one service per invocation (got '$SERVICE' and '$1')"
       SERVICE="$1"; shift ;;
  esac
done

[ -n "$SERVICE" ] || die "usage: rotate-provider-key.sh <debug|prod> [--dry-run] [--key-file <path>]"
[ "$SERVICE" != staging ] || die "staging has no instance; FAL_KEY cannot be rotated there"
require_root
svc_known "$SERVICE" || die "unknown service: $SERVICE"

ENV_FILE="$(svc_env_file "$SERVICE")"
UNIT="$(svc_unit "$SERVICE")"
OTHER=debug
[ "$SERVICE" = debug ] && OTHER=prod
OTHER_ENV="$(svc_env_file "$OTHER")"
[ -f "$ENV_FILE" ] || die "env file not installed: $ENV_FILE"
[ -f "$OTHER_ENV" ] || die "other service env file not installed: $OTHER_ENV"

# The lock file is ordinary and stays on disk after a deploy; what matters is
# whether a live process holds the flock on it. Take that lock for the whole
# rotation instead of testing for the file, so a deploy cannot start while the
# service is being restarted, and a leftover file cannot block a rotation.
require_cmd flock
exec 9>"$LOCK"
flock -n 9 || die "a deployment holds $LOCK - wait for it to finish"

# Two ways in, and neither prints the key: a no-echo prompt for a human at a
# terminal, or --key-file for a key the owner already stores outside git. The
# file path is what appears in the process list and the shell history, never the
# value; the file is read once and never copied anywhere but the env file.
if [ -n "$KEY_FILE" ]; then
  [ -r "$KEY_FILE" ] || die "key file is not readable: $KEY_FILE"
  NEW_KEY="$(tr -d '\r\n' < "$KEY_FILE")"
  if [ "$(wc -l < "$KEY_FILE")" -gt 1 ]; then
    die "key file must hold exactly one line: $KEY_FILE"
  fi
else
  [ -t 0 ] && [ -r /dev/tty ] || die "FAL_KEY must be entered from an interactive terminal (or pass --key-file)"
  IFS= read -rsp 'FAL_KEY: ' NEW_KEY < /dev/tty
  IFS= read -rsp 'repeat: ' REPEAT_KEY < /dev/tty
  printf '\n' >&2
  [ "$NEW_KEY" = "$REPEAT_KEY" ] || die "FAL_KEY entries do not match"
  unset REPEAT_KEY
fi
validate_key "$NEW_KEY"

CURRENT_KEY="$(env_value "$ENV_FILE" FAL_KEY)"
[ "$(sha12 "$NEW_KEY")" != "$(sha12 "$CURRENT_KEY")" ] \
  || die "refusing no-op rotation: $SERVICE already holds this FAL_KEY"

OTHER_KEY="$(env_value "$OTHER_ENV" FAL_KEY)"
[ -n "$OTHER_KEY" ] || die "$OTHER_ENV does not declare FAL_KEY"
NEW_SHA="$(sha12 "$NEW_KEY")"
OTHER_SHA="$(sha12 "$OTHER_KEY")"
ensure_distinct_key "$NEW_KEY" "$OTHER" "$OTHER_ENV"

if [ "$DRY_RUN" -eq 1 ]; then
  log "dry run: would back up $ENV_FILE, atomically replace FAL_KEY, restart $UNIT, and verify /health and /v1/config"
  printf '  service      %s\n  new sha12    %s\n  %s sha12  %s\n' "$SERVICE" "$NEW_SHA" "$OTHER" "$OTHER_SHA"
  exit 0
fi

KEY_LINES="$(grep -c '^FAL_KEY=' "$ENV_FILE" || true)"
[ "$KEY_LINES" -le 1 ] || die "$ENV_FILE has more than one FAL_KEY line"
DIR="$(dirname "$ENV_FILE")"
BASE="$(basename "$ENV_FILE")"
STAMP="$(date -u +%Y%m%d-%H%M%S)"
BACKUP="$DIR/$BASE.bak-$STAMP"
[ ! -e "$BACKUP" ] || die "backup path already exists: $BACKUP"
TMP="$(mktemp "$DIR/.${BASE}.rotate.XXXXXX")"
trap 'rm -f "$TMP"' EXIT
install -m 0600 "$ENV_FILE" "$BACKUP"

if [ "$KEY_LINES" -eq 1 ]; then
  FAL_KEY_NEW="$NEW_KEY" awk '
    { old = $0; cr = (substr(old, length(old), 1) == "\r" ? "\r" : "") }
    /^FAL_KEY=/ { print "FAL_KEY=" ENVIRON["FAL_KEY_NEW"] cr; next }
    { print old }
  ' "$ENV_FILE" > "$TMP"
else
  cat "$ENV_FILE" > "$TMP"
  if [ -s "$ENV_FILE" ] && [ "$(tail -c 1 "$ENV_FILE" | wc -l)" -eq 0 ]; then printf '\n' >> "$TMP"; fi
  printf 'FAL_KEY=%s\n' "$NEW_KEY" >> "$TMP"
fi

# Redacting the key before cmp proves all other bytes (including line endings)
# are identical without ever rendering either key in terminal output.
cmp -s <(sed 's/^FAL_KEY=.*/FAL_KEY=<redacted>/' "$BACKUP") \
       <(sed 's/^FAL_KEY=.*/FAL_KEY=<redacted>/' "$TMP") \
  || die "refusing write: content outside FAL_KEY would change"
install -m 0640 -o root -g "$(svc_user "$SERVICE")" "$TMP" "$ENV_FILE"

restore() {
  warn "restoring $ENV_FILE from $BACKUP"
  install -m 0640 -o root -g "$(svc_user "$SERVICE")" "$BACKUP" "$ENV_FILE"
  systemctl restart "$UNIT" || true
}

if ! systemctl restart "$UNIT" || ! wait_health "$SERVICE" 40 || ! read_endpoint_ok "$(svc_port "$SERVICE")"; then
  restore
  journalctl -u "$UNIT" -n 30 --no-pager || true
  die "rotation failed health gate; previous FAL_KEY restored"
fi

echo
echo "ROTATION: OK"
printf '  service     %s\n  unit        %s\n  sha12       %s\n  %s sha12 %s\n' "$SERVICE" "$UNIT" "$NEW_SHA" "$OTHER" "$OTHER_SHA"
printf '  health      /health 200, /v1/config 200\n  backup      %s\n  rollback    install -m 0640 -o root -g %s %s %s && systemctl restart %s\n' \
  "$BACKUP" "$(svc_user "$SERVICE")" "$BACKUP" "$ENV_FILE" "$UNIT"
