#!/usr/bin/env bash
# backup.sh - a consistent SQLite backup, verified by reading it back.
#
#   backup.sh                 # production, keep the newest 7
#   backup.sh staging --keep 3
#   backup.sh prod --out-dir /opt/plinth-product-photos/backups/prod
#
# A blind `cp` of a live SQLite file is not a backup: the main file and its WAL
# are copied at different instants. This uses better-sqlite3's online backup API
# (SQLite's own backup, one consistent snapshot), then opens the COPY read-only,
# runs PRAGMA integrity_check and counts rows in real tables. A file that fails
# either check is deleted and the script exits non-zero.
#
# Restore is deploy/restore.sh, and the drill in the docs restores into a scratch
# database - a backup nobody restored is not a backup.

set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
. "$HERE/lib.sh"

SERVICE="prod"
KEEP=7
OUT_DIR=""
REL_OVERRIDE=""

usage() {
  sed -n '2,15p' "$0" | sed 's/^# \{0,1\}//'
  exit 0
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --keep) KEEP="${2:-}"; shift 2 ;;
    --out-dir) OUT_DIR="${2:-}"; shift 2 ;;
    --release) REL_OVERRIDE="${2:-}"; shift 2 ;;
    -h|--help) usage ;;
    -*) die "unknown flag: $1" ;;
    *) SERVICE="$1"; shift ;;
  esac
done

svc_known "$SERVICE" || die "unknown service: $SERVICE"
require_root
require_cmd node

[ -n "${KEEP##*[!0-9]*}" ] && [ "$KEEP" -ge 1 ] || die "--keep must be a positive integer"
ENV_FILE="$(svc_env_file "$SERVICE")"
DB_PATH="$(env_value "$ENV_FILE" DATABASE_PATH)"
[ -n "$DB_PATH" ] || die "$ENV_FILE declares no DATABASE_PATH"
[ -f "$DB_PATH" ] || { warn "database does not exist yet: $DB_PATH (nothing to back up)"; exit 0; }

REL="${REL_OVERRIDE:-$(readlink -f "$ROOT/$(svc_slot "$SERVICE")" 2>/dev/null || true)}"
[ -n "$REL" ] && [ -d "$REL/node_modules/better-sqlite3" ] \
  || die "no release with better-sqlite3 installed for $SERVICE (pass --release <dir> on a first deploy)"

[ -n "$OUT_DIR" ] || OUT_DIR="$BACKUPS/$SERVICE"
mkdir -p "$OUT_DIR"
STAMP="$(date -u +%Y%m%d-%H%M%S)"
DEST="$OUT_DIR/$SERVICE-$STAMP.sqlite"

log "backing up $SERVICE"
log "  source $DB_PATH"
log "  dest   $DEST"

# The JS runs in the release directory so `better-sqlite3` resolves to the
# deployed native module. The database path never appears in argv.
(
  cd "$REL"
  SRC_DB="$DB_PATH" DEST_DB="$DEST" node --input-type=module -e '
    import Database from "better-sqlite3";
    const src = process.env.SRC_DB;
    const dest = process.env.DEST_DB;
    const db = new Database(src, { readonly: true, fileMustExist: true });
    await db.backup(dest);
    db.close();
    // The copy inherits WAL mode, so its newest committed frames can sit in a
    // `-wal` sidecar. restore.sh installs exactly one file, so fold the WAL back
    // in and switch to rollback-journal mode before verifying: a backup that
    // needs two files is a backup the restore procedure silently loses.
    const seal = new Database(dest, { fileMustExist: true });
    seal.pragma("wal_checkpoint(TRUNCATE)");
    seal.pragma("journal_mode = DELETE");
    seal.close();
    const copy = new Database(dest, { readonly: true, fileMustExist: true });
    const integrity = copy.pragma("integrity_check", { simple: true });
    const all = copy.prepare("SELECT name FROM sqlite_master").all().map((r) => r.name);
    const wanted = ["wallet", "device", "job", "ledger", "job_upload"];
    const tables = all.filter((n) => wanted.indexOf(n) !== -1);
    const counts = {};
    for (const t of tables) counts[t] = copy.prepare(`SELECT count(*) c FROM "${t}"`).get().c;
    const version = copy.pragma("user_version", { simple: true });
    copy.close();
    console.log(JSON.stringify({ integrity, user_version: version, counts }));
    if (integrity !== "ok") process.exit(1);
    if (tables.length === 0) process.exit(2);
  '
) || {
  rc=$?
  rm -f "$DEST"
  die "backup verification failed (exit $rc) - the copy was deleted"
}

chmod 640 "$DEST"
chown root:root "$DEST"

SIZE="$(stat -c '%s' "$DEST")"
SHA="$(sha256sum "$DEST" | cut -c1-16)"
printf '%s  %s  %s bytes\n' "$SHA" "$(basename "$DEST")" "$SIZE"

# Prune only backups this script made, newest first.
mapfile -t OLD < <(find "$OUT_DIR" -maxdepth 1 -type f -name "$SERVICE-*.sqlite" -printf '%f\n' | sort -r | tail -n +$((KEEP + 1)))
for f in "${OLD[@]:-}"; do
  [ -n "$f" ] || continue
  log "pruning $f"
  rm -f "$OUT_DIR/$f"
done

echo "BACKUP: OK"
echo "  file   $DEST"
echo "  sha256 ${SHA}..."
echo "  kept   $(find "$OUT_DIR" -maxdepth 1 -type f -name "$SERVICE-*.sqlite" | wc -l) (limit $KEEP)"
