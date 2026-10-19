#!/usr/bin/env bash
# harden-identity.sh - give each Plinth environment its own Unix identity and
# take write permission away from everybody else.
#
# This is the host change that check-separation.sh's S9/S10/S11 rows measure.
# It is idempotent: run it as often as you like. It does not touch a release's
# contents, does not promote a slot, and does not restart the production unit -
# production's data directory is re-owned in place (the running process already
# runs as `plinth`, so no restart is needed).
#
#   harden-identity.sh [--dry-run]
#
# What it changes:
#   * creates the `plinth-debug` system user if absent
#   * /var/lib/plinth-product-photos*          -> owned by that env's user, 750
#   * /etc/plinth-product-photos/<svc>.env     -> root:<env-group> 640
#   * /opt/plinth-product-photos/releases/**   -> root:<env-group>, go-w, 755/644
#   * debug unit gets a drop-in naming User/Group=plinth-debug (then restarted)
#
# Exit: 0 applied (or nothing to do), 1 a precondition failed.

set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
. "$HERE/lib.sh"

DRY=0
[ "${1:-}" = "--dry-run" ] && DRY=1

require_root

run() {
  if [ "$DRY" -eq 1 ]; then printf 'would: %s\n' "$*"; else "$@"; fi
}

ensure_user() { # <name> <home>
  local name="$1" home="$2"
  if id "$name" >/dev/null 2>&1; then
    log "user $name already exists"
    return 0
  fi
  command -v useradd >/dev/null || die "useradd not found"
  log "creating system user $name (home $home)"
  run useradd --system --home-dir "$home" --shell /usr/sbin/nologin "$name"
  run install -d -o "$name" -g "$name" -m 750 "$home"
}

# A data directory is readable only by its own service user and root. SQLite's
# sidecars are chmod'ed the same way: a 644 -wal file leaks the same rows as a
# 644 database.
tighten_data_dir() { # <dir> <user>
  local dir="$1" user="$2"
  [ -d "$dir" ] || { warn "data dir absent: $dir"; return 0; }
  run chown -R "$user:$user" "$dir"
  run find "$dir" -type d -exec chmod 750 {} +
  run find "$dir" -type f -exec chmod 640 {} +
}

tighten_env() { # <file> <group>
  local file="$1" group="$2"
  [ -f "$file" ] || { warn "env file absent: $file"; return 0; }
  run chown "root:$group" "$file"
  run chmod 640 "$file"
}

# The state files must live outside the release tree or the next deploy replaces
# them. A service whose env predates SERVICE_CLOSED_FILE gets the path added; the
# value is not a secret, and an existing line is never rewritten.
ensure_env_key() { # <file> <key> <value>
  local file="$1" key="$2" value="$3"
  [ -f "$file" ] || return 0
  if grep -qE "^${key}=" "$file"; then
    return 0
  fi
  log "adding $key to $(basename "$file")"
  if [ "$DRY" -eq 1 ]; then
    printf 'would: append %s=%s to %s\n' "$key" "$value" "$file"
  else
    printf '%s=%s\n' "$key" "$value" >> "$file"
  fi
}

# Releases are immutable code. Root owns them; the service's group reads them.
# A 777 release (measured on 20261019-011255) lets the *other* environment
# rewrite the code that runs as this one, which is the S10 row.
tighten_release_tree() { # <dir> <group>
  local dir="$1" group="$2"
  [ -d "$dir" ] || return 0
  run chown -R "root:$group" "$dir"
  run chmod -R go-w "$dir"
  run find "$dir" -type d -exec chmod 755 {} +
  run find "$dir" -type f -exec chmod 644 {} +
}

# ---------------------------------------------------------------- users
ensure_user plinth-debug /var/lib/plinth-product-photos-debug
id plinth >/dev/null 2>&1 || die "production user plinth is missing"

# ---------------------------------------------------------------- data + env
tighten_data_dir /var/lib/plinth-product-photos        plinth
tighten_data_dir /var/lib/plinth-product-photos-debug  plinth-debug
tighten_env /etc/plinth-product-photos/server.env         plinth
tighten_env /etc/plinth-product-photos/server-debug.env   plinth-debug
ensure_env_key /etc/plinth-product-photos/server.env       SERVICE_CLOSED_FILE /var/lib/plinth-product-photos/SERVICE_CLOSED
ensure_env_key /etc/plinth-product-photos/server-debug.env SERVICE_CLOSED_FILE /var/lib/plinth-product-photos-debug/SERVICE_CLOSED

# ---------------------------------------------------------------- releases
# Ownership follows the slot a release tree is *served by*: the two live slots
# are current and current-debug. Every other tree only loses world-write, which
# nothing needs on a past release.
prod_rel="$(readlink -f "$ROOT/current" 2>/dev/null || true)"
debug_rel="$(readlink -f "$ROOT/current-debug" 2>/dev/null || true)"
[ -n "$prod_rel" ] && tighten_release_tree "$prod_rel" plinth
[ -n "$debug_rel" ] && tighten_release_tree "$debug_rel" plinth-debug
# Every other tree loses world-write; nothing needs to modify a past release.
for rel in "$RELEASES"/*/; do
  [ -d "$rel" ] || continue
  run chmod -R go-w "$rel"
done

# ---------------------------------------------------------------- debug unit
DROPIN_DIR="$UNIT_DIR/plinth-product-photos-debug.service.d"
if [ "$DRY" -eq 0 ]; then
  install -d -m 755 "$DROPIN_DIR"
  cat > "$DROPIN_DIR/20-identity.conf" <<'EOF'
# Environment identity: the debug instance must not be able to read production
# data, and production must not be able to rewrite the code debug runs.
[Service]
User=plinth-debug
Group=plinth-debug
EOF
  systemctl daemon-reload
else
  printf 'would: write %s/20-identity.conf and daemon-reload\n' "$DROPIN_DIR"
fi

if [ "$DRY" -eq 0 ]; then
  if systemctl is-active --quiet plinth-product-photos-debug.service; then
    log "restarting debug to pick up its new identity"
    systemctl restart plinth-product-photos-debug.service
    wait_health debug 40 || die "debug did not come back after the identity change"
    read_endpoint_ok 3102 || die "debug answers /health but not /v1/config after the identity change"
  else
    warn "debug unit is not active; identity applies on its next start"
  fi
fi

echo "HARDEN: OK (run deploy/check-separation.sh --pair prod,debug to measure it)"
