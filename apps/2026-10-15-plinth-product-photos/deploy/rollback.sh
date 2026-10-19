#!/usr/bin/env bash
# rollback.sh - one command, back to the previous release.
#
#   rollback.sh              # production, no arguments: the operator's panic button
#   rollback.sh staging      # named service
#   rollback.sh prod --to 20261019-011255
#
# The target is the previous release recorded by release.sh in
# /opt/plinth-product-photos/state/<service>.previous. The script switches the
# symlink, restarts the unit and waits for a real health answer; if the target
# does not come up it switches back and exits non-zero, so a failed rollback
# cannot leave the slot pointing at a dead release.
#
# What it does NOT undo: a database migration. See
# docs/production-deployment.md § "Rollback does not undo a migration".

set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
. "$HERE/lib.sh"

SERVICE="prod"
TO=""

usage() {
  sed -n '2,17p' "$0" | sed 's/^# \{0,1\}//'
  exit 0
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --to) TO="${2:-}"; shift 2 ;;
    -h|--help) usage ;;
    -*) die "unknown flag: $1" ;;
    *) SERVICE="$1"; shift ;;
  esac
done

svc_known "$SERVICE" || die "unknown service: $SERVICE"
require_root
require_cmd curl
require_cmd flock

# The same lock as release.sh, but the panic button waits rather than refusing:
# a deploy's health gate is bounded at ~40 s, so a 180 s wait covers it. A lock
# held longer than that is a deploy that is not making progress.
exec 9>"$LOCK"
flock -w 180 9 || die "could not acquire $LOCK within 180s - a deploy is stuck, inspect it first"

UNIT="$(svc_unit "$SERVICE")"
SLOT="$(svc_slot "$SERVICE")"

CURRENT="$(readlink -f "$ROOT/$SLOT" 2>/dev/null || true)"
if [ -n "$TO" ]; then
  case "$TO" in
    /*) TARGET="$TO" ;;
    *)  TARGET="$RELEASES/$TO" ;;
  esac
else
  [ -f "$STATE/$SERVICE.previous" ] || die "no previous release recorded for $SERVICE in $STATE/$SERVICE.previous"
  TARGET="$(head -1 "$STATE/$SERVICE.previous")"
fi

[ -d "$TARGET" ] || die "rollback target does not exist: $TARGET"
[ "$TARGET" != "$CURRENT" ] || die "rollback target is the release already running: $TARGET"

log "rolling $SERVICE back"
log "  from $CURRENT"
log "  to   $TARGET"

if ! activate_service "$SERVICE" "$TARGET"; then
  warn "rollback target $TARGET failed its health gate"
  if [ -n "$CURRENT" ] && [ -d "$CURRENT" ]; then
    warn "restoring the release that was running before ($CURRENT)"
    if activate_service "$SERVICE" "$CURRENT"; then
      die "rollback failed; the previous release was restored and is healthy"
    fi
    die "rollback failed AND the previous release did not come back - manual attention required"
  fi
  die "rollback failed and there is no previous release to restore"
fi

# Remember the release we came from, so `rollback.sh` again returns forward. The
# pair is what makes a rollback reversible rather than a one-way trip.
if [ -n "$CURRENT" ]; then
  printf '%s\n' "$CURRENT" > "$STATE/$SERVICE.previous"
fi
printf '%s %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$TARGET" > "$STATE/$SERVICE.rolled-back"

echo
echo "ROLLBACK: OK"
echo "  service  $SERVICE"
echo "  running  $TARGET"
echo "  health   http://127.0.0.1:$(svc_port "$SERVICE")/health -> 200"
echo "  note     the database was NOT reverted; migrations are forward-only"
