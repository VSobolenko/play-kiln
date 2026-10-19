#!/usr/bin/env bash
# setup-staging.sh - create the rehearsal instance (port 3103) that proves the
# release path before anything touches production.
#
#   setup-staging.sh            # create user, data dir, env file and unit
#   setup-staging.sh --force    # recreate the env file and unit
#
# Staging is a real service with its own user, its own env file, its own data
# directory and its own unit - that is the point: a rehearsal that shared
# production's identity would prove nothing. It is NOT enabled at boot and it is
# NOT routed by Caddy, so it can never be reached from the public internet.
#
# Generated secrets are written to the env file and never printed. The provider
# key is a deliberate dummy: staging must never spend the production provider
# account, and the rehearsal's generation call is refused by the kill switch
# before a provider would be contacted.

set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
. "$HERE/lib.sh"

FORCE=0
while [ "$#" -gt 0 ]; do
  case "$1" in
    --force) FORCE=1; shift ;;
    -h|--help) sed -n '2,18p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) die "unknown argument: $1" ;;
  esac
done

SERVICE="staging"
require_root
require_cmd systemctl
require_cmd node

UNIT="$(svc_unit "$SERVICE")"
ENV_FILE="$(svc_env_file "$SERVICE")"
SVC_USER="$(svc_user "$SERVICE")"
DATA_DIR="$(svc_data_dir "$SERVICE")"
PORT="$(svc_port "$SERVICE")"

if [ -f "$UNIT_DIR/$UNIT" ] && [ "$FORCE" -ne 1 ]; then
  die "$UNIT_DIR/$UNIT already exists (use --force to rewrite it)"
fi

log "creating system user $SVC_USER"
id "$SVC_USER" >/dev/null 2>&1 || useradd --system --home "$DATA_DIR" --shell /usr/sbin/nologin "$SVC_USER"

log "creating directories"
mkdir -p "$DATA_DIR" "$ROOT/incoming" "$STATE" "$BACKUPS" "$ROOT/.npm-cache"
chown "$SVC_USER:$SVC_USER" "$DATA_DIR"
chmod 750 "$DATA_DIR"

rand() { head -c 48 /dev/urandom | base64 | tr -d '=\n' | cut -c1-48; }
kek()  { head -c 32 /dev/urandom | base64 | tr -d '=\n'; }

if [ -f "$ENV_FILE" ] && [ "$FORCE" -ne 1 ]; then
  log "keeping existing env file $ENV_FILE"
else
  log "writing $ENV_FILE (values are not printed)"
  umask 077
  cat > "$ENV_FILE" <<EOF
NODE_ENV=staging
HOST=127.0.0.1
PORT=$PORT
JWT_SECRET=$(rand)
RECOVERY_PEPPER=$(rand)
BILLING_BINDING_SECRET=$(rand)
PURCHASE_TOKEN_KEK=$(kek)
FAL_KEY=staging-dummy-provider-key-$(head -c 12 /dev/urandom | base64 | tr -d '=\n')
PLINTH_DEV_INTEGRITY=1
PLAY_PACKAGE_NAME=com.gexetr.plinthproductphotos
DATABASE_PATH=$DATA_DIR/server.sqlite
DATA_DIR=$DATA_DIR
KILL_SWITCH_FILE=$DATA_DIR/KILL_SWITCH
SERVICE_CLOSED_FILE=$DATA_DIR/SERVICE_CLOSED
MIN_APP_VERSION=0
STARTER_CREDITS=10
STARTER_DAILY_BUDGET_USD=1
DAILY_SPEND_CAP_USD=1
JOB_CONCURRENCY=1
EOF
  umask 022
  chown "root:$SVC_USER" "$ENV_FILE"
  chmod 640 "$ENV_FILE"
fi

log "writing $UNIT_DIR/$UNIT"
cat > "$UNIT_DIR/$UNIT" <<EOF
[Unit]
Description=Plinth Product Photos STAGING API server (rehearsal, not public)
Documentation=file:$ROOT/current-staging/README.md
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=$SVC_USER
Group=$SVC_USER
WorkingDirectory=$ROOT/current-staging
EnvironmentFile=$ENV_FILE
ExecStart=/usr/bin/node dist/src/index.js
Restart=on-failure
RestartSec=5s
NoNewPrivileges=true
PrivateTmp=true
PrivateDevices=true
ProtectSystem=strict
ProtectHome=true
ProtectKernelTunables=true
ProtectKernelModules=true
ProtectControlGroups=true
RestrictSUIDSGID=true
RestrictNamespaces=true
LockPersonality=true
MemoryDenyWriteExecute=false
ReadWritePaths=$DATA_DIR

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
# Deliberately not enabled: a reboot must not bring the rehearsal instance back.
systemctl disable "$UNIT" >/dev/null 2>&1 || true

echo "STAGING: READY (not started, not enabled, not proxied)"
echo "  user    $SVC_USER"
echo "  unit    $UNIT"
echo "  env     $ENV_FILE (mode 640 root:$SVC_USER; values not printed)"
echo "  data    $DATA_DIR"
echo "  port    $PORT (loopback only)"
echo
echo "next: $HERE/release.sh staging --tarball $ROOT/incoming/<snapshot>.tgz"
