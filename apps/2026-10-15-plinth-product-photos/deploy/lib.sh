#!/usr/bin/env bash
# Shared registry and helpers for the Plinth deploy scripts.
# One source of truth: every script sources this file and nothing restates a path.
#
# Services are named, never inferred from the caller's shell. `prod` is the only
# service whose symlink is `current`; every other service writes its own slot.

set -euo pipefail

ROOT="/opt/plinth-product-photos"
RELEASES="$ROOT/releases"
STATE="$ROOT/state"
INCOMING="$ROOT/incoming"
BACKUPS="$ROOT/backups"
LOCK="$ROOT/.deploy.lock"
ENV_DIR="/etc/plinth-product-photos"
UNIT_DIR="/etc/systemd/system"

# ---------------------------------------------------------------- service table
service_names() {
  printf '%s\n' prod debug staging
}

svc_known() {
  case "${1:-}" in prod|debug|staging) return 0 ;; *) return 1 ;; esac
}

svc_unit() {
  case "$1" in
    prod)    printf '%s' 'plinth-product-photos.service' ;;
    debug)   printf '%s' 'plinth-product-photos-debug.service' ;;
    staging) printf '%s' 'plinth-product-photos-staging.service' ;;
    *) die "unknown service: $1" ;;
  esac
}

svc_slot() {
  case "$1" in
    prod)    printf '%s' 'current' ;;
    debug)   printf '%s' 'current-debug' ;;
    staging) printf '%s' 'current-staging' ;;
    *) die "unknown service: $1" ;;
  esac
}

svc_env_file() {
  case "$1" in
    prod)    printf '%s' "$ENV_DIR/server.env" ;;
    debug)   printf '%s' "$ENV_DIR/server-debug.env" ;;
    staging) printf '%s' "$ENV_DIR/server-staging.env" ;;
    *) die "unknown service: $1" ;;
  esac
}

svc_user() {
  case "$1" in
    prod)    printf '%s' 'plinth' ;;
    debug)   printf '%s' 'plinth-debug' ;;
    staging) printf '%s' 'plinth-staging' ;;
    *) die "unknown service: $1" ;;
  esac
}

svc_port() {
  case "$1" in
    prod)    printf '%s' '3101' ;;
    debug)   printf '%s' '3102' ;;
    staging) printf '%s' '3103' ;;
    *) die "unknown service: $1" ;;
  esac
}

svc_data_dir() {
  case "$1" in
    prod)    printf '%s' '/var/lib/plinth-product-photos' ;;
    debug)   printf '%s' '/var/lib/plinth-product-photos-debug' ;;
    staging) printf '%s' '/var/lib/plinth-product-photos-staging' ;;
    *) die "unknown service: $1" ;;
  esac
}

svc_prefix() {
  case "$1" in
    prod)    printf '%s' '/plinth/' ;;
    debug)   printf '%s' '/plinth-debug/' ;;
    staging) printf '%s' '/plinth-staging/' ;;
    *) die "unknown service: $1" ;;
  esac
}

# ---------------------------------------------------------------- output helpers
log()  { printf '[%s] %s\n' "$(date -u +%H:%M:%S)" "$*"; }
warn() { printf '[%s] WARNING: %s\n' "$(date -u +%H:%M:%S)" "$*" >&2; }
die()  { printf '[%s] ERROR: %s\n' "$(date -u +%H:%M:%S)" "$*" >&2; exit 1; }

require_root() {
  [ "$(id -u)" -eq 0 ] || die "this script must run as root (it writes systemd units, env files and releases)"
  [ -d "$ROOT" ] || die "deploy root not found: $ROOT"
}

# ------------------------------------------------------------------ env reading
# Reads one key from an EnvironmentFile without sourcing it: sourcing would put
# every secret into this shell's environment and into any child process's /proc.
env_value() { # <env-file> <key>
  local file="$1" key="$2"
  [ -r "$file" ] || die "cannot read env file: $file"
  grep -E "^${key}=" "$file" | head -1 | cut -d= -f2- || true
}

env_has() { # <env-file> <key>
  local file="$1" key="$2"
  [ -r "$file" ] || die "cannot read env file: $file"
  grep -qE "^${key}=" "$file"
}

sha12() { printf '%s' "$1" | sha256sum | cut -c1-12; }

# --------------------------------------------------------------- runtime checks
health_ok() { # <port>
  curl -fsS -m 3 "http://127.0.0.1:$1/health" >/dev/null 2>&1
}

wait_health() { # <service> <attempts>
  local svc="$1" attempts="${2:-40}" port i
  port="$(svc_port "$svc")"
  for ((i = 1; i <= attempts; i++)); do
    if health_ok "$port"; then
      log "health 200 on 127.0.0.1:$port after ${i}s"
      return 0
    fi
    sleep 1
  done
  return 1
}

read_endpoint_ok() { # <port>
  # /v1/config is a public read endpoint and carries the operator state, so a
  # 200 here proves routes are registered, not only that the process is up.
  local body
  body="$(curl -fsS -m 3 "http://127.0.0.1:$1/v1/config" 2>/dev/null)" || return 1
  case "$body" in
    *'"killSwitch"'*) return 0 ;;
    *) return 1 ;;
  esac
}

running_release() { # <service> -> resolved release path of the running process
  local unit pid cwd
  unit="$(svc_unit "$1")"
  pid="$(systemctl show -p MainPID --value "$unit" 2>/dev/null)"
  [ -n "$pid" ] && [ "$pid" != "0" ] || return 1
  cwd="$(readlink -f "/proc/$pid/cwd" 2>/dev/null)" || return 1
  printf '%s' "$cwd"
}

switch_slot() { # <service> <release-dir>
  local svc="$1" rel="$2" slot link tmp
  slot="$(svc_slot "$svc")"
  link="$ROOT/$slot"
  tmp="$ROOT/.$slot.next"
  [ -d "$rel" ] || die "release directory does not exist: $rel"
  rm -f "$tmp"
  ln -sfn "$rel" "$tmp"
  mv -T "$tmp" "$link"
  log "symlink $link -> $rel"
}

# activate_service - point a slot at a release, restart, and prove it is alive.
# Returns non-zero when the process is up but wrong, or not up at all. Every
# caller treats that as a failed deployment and rolls back; none of them reports
# success on the strength of the symlink alone.
activate_service() { # <service> <release-dir>
  local svc="$1" rel="$2" unit running
  unit="$(svc_unit "$svc")"
  switch_slot "$svc" "$rel"
  systemctl restart "$unit"
  if ! systemctl is-active --quiet "$unit"; then
    warn "$unit is not active after restart"
    journalctl -u "$unit" -n 20 --no-pager || true
    return 1
  fi
  if ! wait_health "$svc" 40; then
    warn "$unit did not answer /health within 40s"
    journalctl -u "$unit" -n 30 --no-pager || true
    return 1
  fi
  if ! read_endpoint_ok "$(svc_port "$svc")"; then
    warn "$unit answers /health but not the public read endpoint /v1/config"
    return 1
  fi
  running="$(running_release "$svc" || true)"
  if [ "$running" != "$rel" ]; then
    warn "$unit is running from '$running', expected '$rel'"
    return 1
  fi
  log "$unit active from $rel (/health 200, /v1/config 200)"
  return 0
}

# -------------------------------------------------------------------- arg parse
# Small hand-rolled parser: no dependency to install, and an unknown flag is an
# error rather than a silently ignored typo.
parse_flag() { # <want> <flag>
  [ "${1:-}" = "$2" ]
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "required command not found: $1"
}

umask 022
