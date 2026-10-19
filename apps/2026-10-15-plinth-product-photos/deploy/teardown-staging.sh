#!/usr/bin/env bash
# teardown-staging.sh - remove the rehearsal instance cleanly.
#
#   teardown-staging.sh                  # stop, disable, remove unit + env + symlink
#   teardown-staging.sh --purge-data     # also delete the staging database and data dir
#   teardown-staging.sh --purge-data --purge-releases
#                                        # also delete the release directories staging used
#
# Leaving a fourth service running on the owner's host is not acceptable, so the
# default removes the unit itself rather than only stopping it. Production and
# debug are never touched: every path is resolved through the staging row of the
# service table, and the script refuses if that row ever names another service.

set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
. "$HERE/lib.sh"

PURGE_DATA=0
PURGE_RELEASES=0
while [ "$#" -gt 0 ]; do
  case "$1" in
    --purge-data) PURGE_DATA=1; shift ;;
    --purge-releases) PURGE_RELEASES=1; shift ;;
    -h|--help) sed -n '2,14p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) die "unknown argument: $1" ;;
  esac
done

SERVICE="staging"
[ "$(svc_slot "$SERVICE")" = "current-staging" ] || die "refusing: the staging row does not name current-staging"
[ "$(svc_unit "$SERVICE")" = "plinth-product-photos-staging.service" ] || die "refusing: unexpected staging unit"

require_root
require_cmd systemctl

UNIT="$(svc_unit "$SERVICE")"
ENV_FILE="$(svc_env_file "$SERVICE")"
SVC_USER="$(svc_user "$SERVICE")"
DATA_DIR="$(svc_data_dir "$SERVICE")"
SLOT_LINK="$ROOT/$(svc_slot "$SERVICE")"

log "stopping $UNIT"
systemctl stop "$UNIT" 2>/dev/null || true
log "disabling $UNIT"
systemctl disable "$UNIT" 2>/dev/null || true

if [ -f "$UNIT_DIR/$UNIT" ]; then
  log "removing $UNIT_DIR/$UNIT"
  rm -f "$UNIT_DIR/$UNIT"
fi
rm -rf "$UNIT_DIR/$UNIT.d"
systemctl daemon-reload
systemctl reset-failed "$UNIT" 2>/dev/null || true

if [ -e "$SLOT_LINK" ]; then
  log "removing slot symlink $SLOT_LINK"
  rm -f "$SLOT_LINK"
fi

if [ -f "$ENV_FILE" ]; then
  log "removing $ENV_FILE (values never printed)"
  rm -f "$ENV_FILE"
fi

rm -f "$STATE/$SERVICE.previous" "$STATE/$SERVICE.release" "$STATE/$SERVICE.rolled-back" "$STATE/$SERVICE.failed"

if [ "$PURGE_RELEASES" -eq 1 ] && [ -f "$STATE/$SERVICE.releases" ]; then
  while IFS= read -r rel; do
    [ -n "$rel" ] || continue
    case "$rel" in
      "$RELEASES"/*) log "removing staging release $rel"; rm -rf --one-file-system "$rel" ;;
      *) warn "refusing to remove '$rel' (not under $RELEASES)" ;;
    esac
  done < "$STATE/$SERVICE.releases"
  rm -f "$STATE/$SERVICE.releases"
fi

# Second source: a release directory that carries BUILD_INFO service=staging was
# built for this rehearsal even if it never reached a slot (a failed build or a
# failed health gate). Remove it too, but never a release another slot runs.
if [ "$PURGE_RELEASES" -eq 1 ]; then
  prod_rel="$(readlink -f "$ROOT/current" 2>/dev/null || true)"
  debug_rel="$(readlink -f "$ROOT/current-debug" 2>/dev/null || true)"
  for d in "$RELEASES"/*/; do
    [ -d "$d" ] || continue
    [ -f "$d/BUILD_INFO" ] || continue
    grep -qx 'service=staging' "$d/BUILD_INFO" || continue
    resolved="$(readlink -f "$d")"
    [ "$resolved" = "$prod_rel" ] && continue
    [ "$resolved" = "$debug_rel" ] && continue
    log "removing staging-built release $(basename "$d")"
    rm -rf --one-file-system "$d"
  done
fi

if [ "$PURGE_DATA" -eq 1 ]; then
  log "removing staging data $DATA_DIR"
  rm -rf --one-file-system "$DATA_DIR"
  if [ -d "$BACKUPS/staging" ]; then
    log "removing staging backups $BACKUPS/staging (their source database is gone)"
    rm -rf --one-file-system "$BACKUPS/staging"
  fi
  if id "$SVC_USER" >/dev/null 2>&1; then
    log "removing system user $SVC_USER"
    userdel "$SVC_USER" 2>/dev/null || warn "could not remove user $SVC_USER (process still running?)"
  fi
fi

echo
echo "STAGING: TORN DOWN"
echo "  unit      removed"
echo "  env       removed"
echo "  symlink   removed"
echo "  data      $([ "$PURGE_DATA" -eq 1 ] && echo removed || echo "kept at $DATA_DIR (--purge-data to delete)")"
echo "  releases  $([ "$PURGE_RELEASES" -eq 1 ] && echo "removed (recorded in state/$SERVICE.releases)" || echo kept)"
echo
echo "verify nothing is left:"
echo "  systemctl list-units --all | grep -c plinth-product-photos-staging   # must be 0"
echo "  ss -ltn | grep ':3103' || echo 'port 3103 closed'"
