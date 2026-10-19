#!/usr/bin/env bash
# restore.sh - put a backup file back, and prove the result is a database.
#
#   restore.sh <backup.sqlite> --db /path/to/scratch.sqlite
#   restore.sh <backup.sqlite> --db /var/lib/plinth-product-photos/server.sqlite --stop-service prod
#
# It refuses to write over the live database of a running service: restoring
# under a serving process corrupts both the process's view and the file. Say
# --stop-service <name> and the script stops the unit, restores, verifies and
# tells you to start it again - it does not start it for you, because the restart
# is the moment a bad restore becomes visible.
#
# Deleting a stale -wal/-shm beside the target is part of the restore: those
# files belong to the old database and replaying them over a restored file is
# exactly the corruption this script exists to avoid.

set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
. "$HERE/lib.sh"

BACKUP=""
TARGET_DB=""
STOP_SERVICE=""

usage() {
  sed -n '2,15p' "$0" | sed 's/^# \{0,1\}//'
  exit 0
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --db) TARGET_DB="${2:-}"; shift 2 ;;
    --stop-service) STOP_SERVICE="${2:-}"; shift 2 ;;
    -h|--help) usage ;;
    -*) die "unknown flag: $1" ;;
    *) [ -z "$BACKUP" ] || die "one backup per invocation"; BACKUP="$1"; shift ;;
  esac
done

[ -n "$BACKUP" ] || die "usage: restore.sh <backup.sqlite> --db <target.sqlite> [--stop-service <svc>]"
[ -f "$BACKUP" ] || die "backup not found: $BACKUP"
[ -n "$TARGET_DB" ] || die "--db is required (restoring to a scratch path is the recommended drill)"
[ "${TARGET_DB#/}" != "$TARGET_DB" ] || die "--db must be an absolute path"
require_root

REL_FOR_NODE=""
for s in $(service_names); do
  r="$(readlink -f "$ROOT/$(svc_slot "$s")" 2>/dev/null || true)"
  if [ -n "$r" ] && [ -d "$r/node_modules/better-sqlite3" ]; then REL_FOR_NODE="$r"; break; fi
done
[ -n "$REL_FOR_NODE" ] || die "no deployed release provides better-sqlite3 to verify with"

if [ -n "$STOP_SERVICE" ]; then
  svc_known "$STOP_SERVICE" || die "unknown service: $STOP_SERVICE"
  unit="$(svc_unit "$STOP_SERVICE")"
  log "stopping $unit before overwriting its database"
  systemctl stop "$unit"
fi

# Refuse while any live service owns this path, even if --stop-service named a
# different one. The comparison is on the resolved absolute path.
for s in $(service_names); do
  envf="$(svc_env_file "$s")"
  [ -f "$envf" ] || continue
  db="$(env_value "$envf" DATABASE_PATH)"
  [ "$db" = "$TARGET_DB" ] || continue
  unit="$(svc_unit "$s")"
  if systemctl is-active --quiet "$unit"; then
    die "$unit is serving $TARGET_DB - stop it first (restore.sh ... --stop-service $s)"
  fi
done

mkdir -p "$(dirname "$TARGET_DB")"
log "restoring $(basename "$BACKUP") -> $TARGET_DB"
install -m 640 "$BACKUP" "$TARGET_DB"
rm -f "$TARGET_DB-wal" "$TARGET_DB-shm"

# Verify the restored copy, not the backup: the point is that what landed is a
# readable database with rows in it.
(
  cd "$REL_FOR_NODE"
  TARGET_DB="$TARGET_DB" node --input-type=module -e '
    import Database from "better-sqlite3";
    const db = new Database(process.env.TARGET_DB, { readonly: true, fileMustExist: true });
    const integrity = db.pragma("integrity_check", { simple: true });
    const version = db.pragma("user_version", { simple: true });
    const all = db.prepare("SELECT name FROM sqlite_master").all().map((r) => r.name);
    const wanted = ["wallet", "device", "job", "ledger"];
    const counts = {};
    for (const t of all.filter((n) => wanted.indexOf(n) !== -1)) {
      counts[t] = db.prepare(`SELECT count(*) c FROM "${t}"`).get().c;
    }
    db.close();
    console.log(JSON.stringify({ integrity, user_version: version, counts }));
    if (integrity !== "ok") process.exit(1);
  '
) || die "the restored database failed integrity_check"

echo
echo "RESTORE: OK"
echo "  source  $BACKUP"
echo "  target  $TARGET_DB ($(stat -c '%s' "$TARGET_DB") bytes)"
if [ -n "$STOP_SERVICE" ]; then
  echo "  next    systemctl start $(svc_unit "$STOP_SERVICE"), then wait for /health to answer;"
  echo "          a service that does not start is the restore telling you it was the wrong file"
fi
