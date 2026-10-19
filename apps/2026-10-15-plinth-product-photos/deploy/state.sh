#!/usr/bin/env bash
# state.sh - the operator control: ok | maintenance | closed.
#
#   state.sh closed                 # stop generation for good, keep /health answering
#   state.sh maintenance 30m        # a temporary pause; the ETA is recorded in the file
#   state.sh ok                     # reopen
#   state.sh --check                # print the current state, change nothing
#   state.sh closed --service debug
#
# TWO files, because the server distinguishes two operator states and one flag
# must not have two meanings:
#
#   SERVICE_CLOSED_FILE present -> closed       (final; wins over maintenance)
#   KILL_SWITCH_FILE    present -> maintenance  (temporary)
#   neither                     -> ok
#
# The file's CONTENT records the operator's intent and timestamp; the server
# gates on existence. `closed` writes both files, because an older build reads
# only KILL_SWITCH_FILE and would otherwise ignore the final state; `maintenance`
# removes the closed marker so a reopened service is not stuck in a state nobody
# can see.
#
# Survives a restart and a deploy: the paths come from the env file and live
# outside the releases tree (checked by check-separation.sh).
#
# Both env names are resolved exactly as server/src/config.ts resolves them: an
# absolute value as-is, a relative one against the release the unit runs from.

set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
. "$HERE/lib.sh"

OPEN_ENV_VAR="KILL_SWITCH_FILE"
CLOSED_ENV_VAR="SERVICE_CLOSED_FILE"

SERVICE="prod"
ACTION=""
ETA=""
CHECK_ONLY=0

usage() {
  sed -n '2,20p' "$0" | sed 's/^# \{0,1\}//'
  exit 0
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --check) CHECK_ONLY=1; shift ;;
    --service) SERVICE="${2:-}"; shift 2 ;;
    -h|--help) usage ;;
    -*) die "unknown flag: $1" ;;
    *)
      if [ -z "$ACTION" ] && { [ "$1" = "ok" ] || [ "$1" = "maintenance" ] || [ "$1" = "closed" ]; }; then
        ACTION="$1"
      elif [ "$ACTION" = "maintenance" ] && [ -z "$ETA" ]; then
        ETA="$1"
      else
        die "unexpected argument: $1 (usage: state.sh <ok|maintenance|closed> [eta] [--service <svc>])"
      fi
      shift ;;
  esac
done

svc_known "$SERVICE" || die "unknown service: $SERVICE"
require_root
require_cmd curl

ENV_FILE="$(svc_env_file "$SERVICE")"
UNIT="$(svc_unit "$SERVICE")"
PORT="$(svc_port "$SERVICE")"
SVC_USER="$(svc_user "$SERVICE")"

[ -f "$ENV_FILE" ] || die "env file not installed: $ENV_FILE"

# Resolve both knobs exactly the way server/src/config.ts does: an absolute
# value is used as-is, a relative one is resolved against the process working
# directory, which is the release the unit runs from.
resolve_state_file() { # <env-var> -> path on stdout
  local name="$1" raw rel
  raw="$(env_value "$ENV_FILE" "$name")"
  rel="$(readlink -f "$ROOT/$(svc_slot "$SERVICE")" 2>/dev/null || true)"
  if [ -n "$raw" ]; then
    case "$raw" in
      /*) printf '%s' "$raw" ;;
      *)  [ -n "$rel" ] || die "cannot resolve relative $name without a live release for $SERVICE"
          printf '%s' "$rel/$raw" ;;
    esac
  else
    [ -n "$rel" ] || die "$ENV_FILE does not set $name and $SERVICE has no live release"
    warn "$ENV_FILE does not set $name; falling back to $rel/$name"
    printf '%s' "$rel/$name"
  fi
}

OPEN_FILE="$(resolve_state_file "$OPEN_ENV_VAR")"
CLOSED_FILE="$(resolve_state_file "$CLOSED_ENV_VAR")"

current_state() {
  if [ -e "$CLOSED_FILE" ]; then
    printf 'closed'
  elif [ -e "$OPEN_FILE" ]; then
    printf 'maintenance'
  else
    printf 'ok'
  fi
}

verify_api() { # <expected state>
  local want="$1" body i got kill
  for ((i = 1; i <= 15; i++)); do
    body="$(curl -fsS -m 3 "http://127.0.0.1:$PORT/v1/config" 2>/dev/null || true)"
    if [ -n "$body" ]; then
      got=''
      case "$body" in
        *'"serviceState":"ok"'*)          got='ok' ;;
        *'"serviceState":"maintenance"'*) got='maintenance' ;;
        *'"serviceState":"closed"'*)      got='closed' ;;
        *'"serviceState":"update_required"'*) got='update_required' ;;
      esac
      if [ -n "$got" ]; then
        [ "$got" = "$want" ] && return 0
      else
        # Compatibility with a build that predates SERVICE_CLOSED_FILE: it
        # exposes only `killSwitch`. Such a build cannot distinguish closed from
        # maintenance, but it must still refuse work; the state file is what
        # records the operator's intent, and the check below proves the refusal
        # is in force.
        kill=false
        case "$body" in *'"killSwitch":true'*) kill=true ;; esac
        case "$want" in
          ok)           [ "$kill" = false ] && return 0 ;;
          maintenance|closed) [ "$kill" = true ] && return 0 ;;
        esac
      fi
    fi
    sleep 1
  done
  return 1
}

check_health() {
  curl -fsS -m 3 "http://127.0.0.1:$PORT/health" >/dev/null 2>&1 \
    || die "/health is not answering on 127.0.0.1:$PORT - the service itself is down, this is not a state problem"
}

echo "service   $SERVICE"
echo "unit      $UNIT"
echo "state     $(current_state)"
echo "file      $OPEN_FILE (maintenance)"
echo "file      $CLOSED_FILE (closed)"

if [ "$CHECK_ONLY" -eq 1 ]; then
  check_health
  verify_api "$(current_state)" \
    || die "/v1/config serviceState disagrees with the state files ($OPEN_FILE, $CLOSED_FILE)"
  echo "observed  /health 200, /v1/config agrees with state=$(current_state) (serviceState when the build exposes it, killSwitch otherwise)"
  exit 0
fi

[ -n "$ACTION" ] || die "usage: state.sh <ok|maintenance|closed> [eta] [--service <svc>]"

case "$ACTION" in
  ok)
    for file in "$OPEN_FILE" "$CLOSED_FILE"; do
      if [ -e "$file" ]; then
        log "removing $file"
        rm -f "$file"
      fi
    done
    log "service is open"
    ;;
  maintenance)
    log "writing maintenance state to $OPEN_FILE${ETA:+ (eta $ETA)}"
    mkdir -p "$(dirname "$OPEN_FILE")"
    printf 'maintenance %s since %s\n' "${ETA:-unspecified}" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" > "$OPEN_FILE"
    # A pause is not the final state: remove the closed marker so the reported
    # state and the file on disk cannot disagree.
    rm -f "$CLOSED_FILE"
    ;;
  closed)
    log "writing closed state to $CLOSED_FILE"
    mkdir -p "$(dirname "$CLOSED_FILE")"
    printf 'closed since %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" > "$CLOSED_FILE"
    # Compatibility: a build that predates SERVICE_CLOSED_FILE reads only the
    # kill switch. Without this second file, `closed` would look open on that
    # build and the operator's final decision would have no effect at all.
    mkdir -p "$(dirname "$OPEN_FILE")"
    printf 'closed since %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" > "$OPEN_FILE"
    ;;
esac

for file in "$OPEN_FILE" "$CLOSED_FILE"; do
  if [ -e "$file" ]; then
    chown "$SVC_USER:$SVC_USER" "$file" 2>/dev/null || true
    chmod 644 "$file"
  fi
done

# The state is read per request, so no restart is needed - and no restart is
# performed: reopening must not bounce a process that is serving reads.
check_health
if ! verify_api "$ACTION"; then
  die "$UNIT does not report serviceState=$ACTION after state=$ACTION (files: $OPEN_FILE, $CLOSED_FILE)"
fi

case "$ACTION" in
  ok)          STATE_FILE="none" ;;
  maintenance) STATE_FILE="$OPEN_FILE" ;;
  closed)      STATE_FILE="$CLOSED_FILE" ;;
esac
echo
echo "STATE: $ACTION"
echo "  file        $STATE_FILE"
echo "  /health     200 (still answering)"
echo "  /v1/config  agrees with state=$ACTION (serviceState when the build exposes it, killSwitch otherwise)"
echo "  generation  $([ "$ACTION" = "ok" ] && echo "allowed (subject to credits and the daily cap)" || echo "refused, reads and refunds stay open")"
