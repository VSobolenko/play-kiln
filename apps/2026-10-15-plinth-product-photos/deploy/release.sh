#!/usr/bin/env bash
# release.sh - deploy one Plinth service from a named, immutable snapshot.
#
# The script never builds the working tree. It takes a tarball produced by
# deploy/snapshot.sh, which is `git archive <tree> apps/<slot>/server` and carries
# a sidecar naming the tree object and the archive's sha256. A release is
# therefore attributable to bytes, not to a directory somebody edited.
#
#   release.sh staging --tarball /opt/plinth-product-photos/incoming/x.tgz
#   release.sh prod    --tarball ... --promote        # production promotion
#   release.sh prod    --tarball ... --promote --allow-shared-provider-key
#                                                    # the owner's S4 decision, logged as a warning
#   release.sh debug   --tarball ... --skip-backup
#
# Steps: extract -> install -> build -> backup -> migrate -> separation check ->
# atomic switch -> restart -> health gate. A failed health gate rolls the symlink
# back automatically and exits non-zero. There is no path that prints success
# while the service is dead.

set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
. "$HERE/lib.sh"

SERVICE=""
TARBALL=""
TREE=""
PROMOTE=0
DO_BACKUP=1
DO_PRUNE=1
KEEP=10
SEP_EXTRA=()

usage() {
  sed -n '2,18p' "$0" | sed 's/^# \{0,1\}//'
  exit 0
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --tarball) TARBALL="${2:-}"; shift 2 ;;
    --tree) TREE="${2:-}"; shift 2 ;;
    --promote) PROMOTE=1; shift ;;
    --allow-shared-provider-key)
      # Demotes S4 (one provider key across envs) to a warning. An owner
      # decision carried by the promotion command, never a default.
      SEP_EXTRA+=(--allow-shared-provider-key); shift ;;
    --skip-backup) DO_BACKUP=0; shift ;;
    --no-prune) DO_PRUNE=0; shift ;;
    --keep) KEEP="${2:-}"; shift 2 ;;
    -h|--help) usage ;;
    -*) die "unknown flag: $1 (try --help)" ;;
    *) [ -z "$SERVICE" ] || die "one service per invocation (got '$SERVICE' and '$1')"; SERVICE="$1"; shift ;;
  esac
done

[ -n "$SERVICE" ] || die "usage: release.sh <prod|debug|staging> --tarball <path> [--tree <id>] [--promote]"
svc_known "$SERVICE" || die "unknown service: $SERVICE"
[ -n "$TARBALL" ] || die "--tarball is required (a clean snapshot, never the working tree)"
[ -f "$TARBALL" ] || die "tarball not found: $TARBALL"
require_root
require_cmd tar
require_cmd node
require_cmd npm
require_cmd curl
require_cmd flock

# One deploy per host at a time. Two concurrent releases share npm's cache and
# the release symlink, and the second can switch a slot the first is still
# health-checking. The lock is released when the process exits, including on a
# failure.
exec 9>"$LOCK"
flock -n 9 || die "another deploy holds $LOCK - wait for it or inspect the holder"

UNIT="$(svc_unit "$SERVICE")"
SLOT="$(svc_slot "$SERVICE")"
ENV_FILE="$(svc_env_file "$SERVICE")"
SVC_USER="$(svc_user "$SERVICE")"
SVC_PORT="$(svc_port "$SERVICE")"
DATA_DIR="$(svc_data_dir "$SERVICE")"

[ -f "$UNIT_DIR/$UNIT" ] || die "unit not installed: $UNIT_DIR/$UNIT (staging is created by deploy/setup-staging.sh)"
[ -f "$ENV_FILE" ] || die "env file not installed: $ENV_FILE"
id "$SVC_USER" >/dev/null 2>&1 || die "service user does not exist: $SVC_USER"

# Production is the one promotion that needs a second, explicit word. The
# orchestrator runs this after the owner's release approval; an accidental
# `release.sh prod` is a production change nobody asked for.
if [ "$SERVICE" = "prod" ] && [ "$PROMOTE" -ne 1 ]; then
  die "refusing to promote production without --promote (this is the deliberate second word)"
fi

# ---------------------------------------------------------------- 1. snapshot
log "[1/9] verifying snapshot identity"
ARCHIVE_SHA="$(sha256sum "$TARBALL" | awk '{print $1}')"
IDENTITY="$TARBALL.identity"
if [ -f "$IDENTITY" ]; then
  SIDE_TREE="$(grep -E '^tree=' "$IDENTITY" | cut -d= -f2- | tr -d '\r' || true)"
  SIDE_SHA="$(grep -E '^archive_sha256=' "$IDENTITY" | cut -d= -f2- | tr -d '\r' || true)"
  [ -n "$SIDE_SHA" ] && [ "$SIDE_SHA" = "$ARCHIVE_SHA" ] || \
    die "snapshot sidecar does not match the archive: sidecar=$SIDE_SHA archive=$ARCHIVE_SHA"
  [ -n "$SIDE_TREE" ] || die "snapshot sidecar names no tree: $IDENTITY"
  if [ -n "$TREE" ] && [ "$TREE" != "$SIDE_TREE" ]; then
    die "--tree $TREE disagrees with the snapshot's tree $SIDE_TREE"
  fi
  TREE="$SIDE_TREE"
  log "      tree=$TREE archive_sha256=$ARCHIVE_SHA (sidecar verified)"
else
  [ -n "$TREE" ] || die "no $IDENTITY and no --tree: a release must name the revision it was built from"
  warn "no snapshot sidecar; trusting --tree $TREE and archive sha256 $ARCHIVE_SHA"
fi

# ---------------------------------------------------------------- 2. extract
STAMP="$(date -u +%Y%m%d-%H%M%S)"
REL="$RELEASES/$STAMP"
[ ! -e "$REL" ] || die "release directory already exists: $REL (two deploys in the same second)"
log "[2/9] extracting snapshot into $REL"
mkdir -p "$REL"
# The archive path is apps/<slot>/server; three components are stripped so the
# release root is the server package itself.
tar -xzf "$TARBALL" -C "$REL" --strip-components=3
[ -f "$REL/package.json" ] || die "snapshot extraction produced no package.json at $REL (wrong archive layout)"
[ -f "$REL/package-lock.json" ] || die "snapshot has no package-lock.json - npm ci cannot be reproducible"
# Record the release before the build, so a build or health failure still leaves
# a trace that teardown can remove. A release directory that exists and is not
# recorded is a directory nobody cleans up.
mkdir -p "$STATE"
printf '%s\n' "$REL" >> "$STATE/$SERVICE.releases"
chown -R "root:$SVC_USER" "$REL"
chmod -R go-w "$REL"

cat > "$REL/BUILD_INFO" <<EOF
service=$SERVICE
tree=$TREE
archive_sha256=$ARCHIVE_SHA
archive=$(basename "$TARBALL")
created_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
built_on=$(hostname)
EOF

# ---------------------------------------------------------------- 3. install
log "[3/9] npm ci (all dependencies, needed to compile TypeScript)"
(
  cd "$REL"
  export npm_config_cache="$ROOT/.npm-cache"
  export npm_config_fund=false npm_config_audit=false
  if ! npm ci --no-audit --no-fund >"$REL/.npm-ci.log" 2>&1; then
    tail -30 "$REL/.npm-ci.log" >&2
    die "npm ci failed in $REL"
  fi
  log "[4/9] npm run build"
  if ! npm run build >"$REL/.npm-build.log" 2>&1; then
    tail -40 "$REL/.npm-build.log" >&2
    die "npm run build failed in $REL"
  fi
  npm prune --omit=dev >"$REL/.npm-prune.log" 2>&1 || die "npm prune failed in $REL"
)
[ -f "$REL/dist/src/index.js" ] || die "build produced no dist/src/index.js"
[ -d "$REL/dist/migrations" ] || die "build produced no dist/migrations (copy-migrations did not run)"
chown -R "root:$SVC_USER" "$REL"
chmod -R go-w "$REL"
log "      built $(find "$REL/dist" -type f | wc -l) files under dist/"

# ---------------------------------------------------------------- 4. backup
# The backup happens BEFORE migrations: a backup taken after a migration cannot
# undo it. A first deploy of an empty database is the only case where it is
# trivially empty, and the script still takes it.
if [ "$DO_BACKUP" -eq 1 ]; then
  log "[5/9] consistent backup of the $SERVICE database (pre-migration)"
  "$HERE/backup.sh" "$SERVICE" --release "$REL" || die "backup failed - refusing to migrate or switch"
else
  warn "[5/9] backup skipped (--skip-backup); a failed migration has no restore point"
fi

# ---------------------------------------------------------------- 5. migrate
DB_PATH="$(env_value "$ENV_FILE" DATABASE_PATH)"
[ -n "$DB_PATH" ] || die "$ENV_FILE declares no DATABASE_PATH"
[ "${DB_PATH#/}" != "$DB_PATH" ] || die "DATABASE_PATH must be absolute: $DB_PATH"
log "[6/9] applying migrations to $DB_PATH (as $SVC_USER)"
mkdir -p "$(dirname "$DB_PATH")"
chown "$SVC_USER:$SVC_USER" "$(dirname "$DB_PATH")"
(
  cd "$REL"
  runuser -u "$SVC_USER" -- env DATABASE_PATH="$DB_PATH" node --input-type=module -e \
    "import('./dist/src/db.js').then((m) => { const db = m.createDatabase(process.env.DATABASE_PATH); const v = db.pragma('user_version', { simple: true }); db.close(); console.log('database user_version=' + v); })" \
    || die "migration failed - the release stays unswitched and the previous one keeps serving"
)

# ---------------------------------------------------------------- 6. separation
log "[7/9] separation check before switching a live slot"
if [ "$SERVICE" = "prod" ]; then
  if [ "${#SEP_EXTRA[@]}" -gt 0 ]; then
    warn "S4 shared provider key allowed by an explicit flag - this is the owner's recorded decision, not a convenience"
  fi
  "$HERE/check-separation.sh" --pair prod,debug "${SEP_EXTRA[@]}" || die "separation invariants failed - refusing to promote"
else
  "$HERE/check-separation.sh" --service "$SERVICE" "${SEP_EXTRA[@]}" || die "separation invariants failed for $SERVICE"
fi

# ---------------------------------------------------------------- 7. switch
PREVIOUS="$(readlink -f "$ROOT/$SLOT" 2>/dev/null || true)"
mkdir -p "$STATE"
if [ -n "$PREVIOUS" ] && [ -d "$PREVIOUS" ]; then
  printf '%s\n' "$PREVIOUS" > "$STATE/$SERVICE.previous"
  log "      previous release recorded: $PREVIOUS"
else
  warn "no previous release for $SERVICE - this deployment cannot be rolled back"
fi

log "[8/9] switching $SLOT atomically and restarting $UNIT"
if ! activate_service "$SERVICE" "$REL"; then
  warn "HEALTH GATE FAILED for $REL"
  if [ -n "$PREVIOUS" ] && [ -d "$PREVIOUS" ]; then
    warn "rolling back to $PREVIOUS automatically"
    if activate_service "$SERVICE" "$PREVIOUS"; then
      printf '%s\n' "$REL" > "$STATE/$SERVICE.failed"
      echo "RELEASE: FAILED (rolled back to $(basename "$PREVIOUS"), service healthy)"
      exit 1
    fi
    echo "RELEASE: FAILED AND ROLLBACK FAILED - manual attention required" >&2
    exit 2
  fi
  echo "RELEASE: FAILED (no previous release to roll back to)" >&2
  exit 1
fi

printf '%s %s %s\n' "$STAMP" "$TREE" "$ARCHIVE_SHA" > "$STATE/$SERVICE.release"
printf '%s\n' "$REL" >> "$STATE/$SERVICE.releases"

# ---------------------------------------------------------------- 8. prune
if [ "$DO_PRUNE" -eq 1 ]; then
  log "[9/9] pruning unreferenced releases older than 7 days (keeping newest $KEEP)"
  REFERENCED="$(for s in $(service_names); do
      readlink -f "$ROOT/$(svc_slot "$s")" 2>/dev/null || true
      if [ -f "$STATE/$s.previous" ]; then cat "$STATE/$s.previous"; fi
    done | sort -u)" || true
  mapfile -t ALL < <(find "$RELEASES" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | sort -r)
  kept=0
  for name in "${ALL[@]}"; do
    dir="$RELEASES/$name"
    kept=$((kept + 1))
    if printf '%s\n' "$REFERENCED" | grep -qxF "$dir"; then continue; fi
    if [ "$kept" -le "$KEEP" ]; then continue; fi
    if [ -n "$(find "$dir" -maxdepth 0 -mtime -7)" ]; then continue; fi
    log "      removing old release $name"
    rm -rf --one-file-system "$dir"
  done
else
  log "[9/9] prune skipped"
fi

echo
echo "RELEASE: OK"
echo "  service   $SERVICE"
echo "  release   $REL"
echo "  tree      $TREE"
echo "  archive   sha256:$ARCHIVE_SHA"
echo "  health    http://127.0.0.1:$SVC_PORT/health -> 200"
echo "  rollback  $HERE/rollback.sh $SERVICE"
