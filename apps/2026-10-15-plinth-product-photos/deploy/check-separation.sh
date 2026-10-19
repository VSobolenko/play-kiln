#!/usr/bin/env bash
# check-separation.sh - prove that two Plinth environments cannot contaminate
# each other, with output, and exit non-zero naming the invariant that broke.
#
# Designed to run before every deploy. Read-only: it changes no file, no unit and
# no permission. When it fails, the remedy is a change to the host, never a flag
# that silences the row.
#
#   check-separation.sh                      # audit every service that exists
#   check-separation.sh --pair prod,debug    # audit one pair
#   check-separation.sh --service staging    # audit one service on its own
#   check-separation.sh --allow-shared-provider-key
#                                            # demote S4 (shared FAL_KEY) to a
#                                            # warning; the owner's decision, not
#                                            # a convenience
#
# Exit: 0 all invariants hold, 1 at least one failed.

set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
. "$HERE/lib.sh"

PAIR=""
ONLY_SERVICE=""
ALLOW_SHARED_PROVIDER_KEY=0
FAILURES=0
CHECKS=0

usage() {
  sed -n '2,20p' "$0" | sed 's/^# \{0,1\}//'
  exit 0
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --pair) PAIR="${2:-}"; shift 2 ;;
    --service) ONLY_SERVICE="${2:-}"; shift 2 ;;
    --allow-shared-provider-key) ALLOW_SHARED_PROVIDER_KEY=1; shift ;;
    -h|--help) usage ;;
    *) die "unknown argument: $1 (try --help)" ;;
  esac
done

require_root
require_cmd curl
require_cmd ss

pass() { CHECKS=$((CHECKS + 1)); printf 'PASS  %-3s %-22s %s\n' "$1" "$2" "$3"; }
fail() { CHECKS=$((CHECKS + 1)); FAILURES=$((FAILURES + 1)); printf 'FAIL  %-3s %-22s %s\n' "$1" "$2" "$3"; }
skip() { printf 'SKIP  %-3s %-22s %s\n' "$1" "$2" "$3"; }

# A service exists when its unit file exists. Staging is transient by design.
service_exists() { [ -f "$UNIT_DIR/$(svc_unit "$1")" ]; }

# ---------------------------------------------------------------- per-service
check_service() { # <service>
  local svc="$1" env unit user slot rel host port db data bind group mode owner
  env="$(svc_env_file "$svc")"
  unit="$(svc_unit "$svc")"
  user="$(svc_user "$svc")"
  slot="$(svc_slot "$svc")"
  port="$(svc_port "$svc")"

  [ -f "$env" ] || { fail S0 env-file "$svc: missing $env"; return; }
  [ -f "$UNIT_DIR/$unit" ] || { fail S0 unit "$svc: missing $UNIT_DIR/$unit"; return; }

  rel="$(readlink -f "$ROOT/$slot" 2>/dev/null || true)"
  if [ -n "$rel" ] && [ -d "$rel" ]; then
    pass S1 release-path "$svc $slot -> $rel"
  elif ! systemctl is-active --quiet "$unit"; then
    # First deploy: the unit exists and is down, so there is no current release
    # to separate yet. The health gate after the switch is what proves the slot.
    skip S1 release-path "$svc has no $slot yet and the unit is not active (first deploy)"
  else
    fail S1 release-path "$svc $slot does not resolve to a release directory"
  fi

  # S5 - production must never carry the dev integrity bypass. NODE_ENV is
  # checked too: config.ts only refuses the pair (dev=1 AND production), so a
  # production env with NODE_ENV unset would accept the bypass.
  if [ "$svc" = "prod" ]; then
    if env_has "$env" PLINTH_DEV_INTEGRITY; then
      fail S5 dev-integrity "prod carries PLINTH_DEV_INTEGRITY - a dev bypass in production is a total entitlement bypass"
    else
      pass S5 dev-integrity "prod carries no PLINTH_DEV_INTEGRITY"
    fi
    if [ "$(env_value "$env" NODE_ENV)" = "production" ]; then
      pass S5b node-env "prod NODE_ENV=production"
    else
      fail S5b node-env "prod NODE_ENV is '$(env_value "$env" NODE_ENV)' (must be production: it is the second half of the dev-integrity guard)"
    fi
  fi

  # S6 - loopback only.
  host="$(env_value "$env" HOST)"
  case "$host" in
    127.0.0.1|::1|localhost) pass S6a host "$svc HOST=$host" ;;
    *) fail S6a host "$svc HOST='${host}' is not a loopback address" ;;
  esac
  bind="$(ss -ltnH "sport = :$port" 2>/dev/null | awk '{print $4}' | head -1)"
  case "$bind" in
    127.0.0.1:"$port"|"[::1]:$port") pass S6b listen "$svc listens on $bind only" ;;
    "")
      if systemctl is-active --quiet "$unit"; then
        fail S6b listen "$svc has no listener on port $port (unit is active, so this is a real crash)"
      else
        skip S6b listen "$svc is not running yet (no listener to measure; the health gate checks it after the switch)"
      fi ;;
    *) fail S6b listen "$svc listens on $bind - reachable off-host, not loopback-only" ;;
  esac

  # S9/S10/S11 - identity and permissions, measured as the *other* service's
  # user, not read off the mode bits alone.
  for other in $(service_names); do
    [ "$other" = "$svc" ] && continue
    service_exists "$other" || continue
    local ouser
    ouser="$(svc_user "$other")"
    id "$ouser" >/dev/null 2>&1 || { fail S9 db-cross-read "$ouser does not exist - $other cannot be separated from $svc by identity"; continue; }
    db="$(env_value "$env" DATABASE_PATH)"
    data="$(env_value "$env" DATA_DIR)"
    local ok=1
    runuser -u "$ouser" -- test -r "$db" 2>/dev/null && ok=0
    runuser -u "$ouser" -- test -r "$data" 2>/dev/null && ok=0
    if [ "$ok" -eq 1 ]; then
      pass S9 db-cross-read "$ouser cannot read $svc database or data dir ($db)"
    else
      fail S9 db-cross-read "$ouser CAN read $svc data ($db / $data) - ownership and mode do not separate the environments"
    fi
    if runuser -u "$ouser" -- test -r "$env" 2>/dev/null; then
      fail S11 env-perms "$ouser can read $svc env file $env"
    else
      pass S11 env-perms "$ouser cannot read $svc env file $env"
    fi
    if [ -n "$rel" ] && [ -d "$rel" ]; then
      if runuser -u "$ouser" -- test -w "$rel" 2>/dev/null; then
        fail S10 release-writable "$ouser can WRITE $svc release $rel - a different environment can rewrite the code that runs as $user"
      else
        pass S10 release-writable "$ouser cannot write $svc release $rel"
      fi
    fi
  done

  # S12 - operator state must survive a release switch, so it cannot live inside
  # a release directory that the next deploy replaces. Both state files are
  # checked: a closed marker inside the release would vanish on the next deploy.
  local ks name
  for name in KILL_SWITCH_FILE SERVICE_CLOSED_FILE; do
    ks="$(env_value "$env" "$name")"
    if [ -z "$ks" ]; then
      fail S12 state-path "$svc has no $name - state.sh would write inside the release and lose it on deploy"
    elif [ "${ks#/}" = "$ks" ]; then
      fail S12 state-path "$svc $name='$ks' is relative (resolved against the release directory)"
    elif [ "${ks#"$RELEASES"/}" != "$ks" ]; then
      fail S12 state-path "$svc $name=$ks is inside the releases tree"
    else
      pass S12 state-path "$svc $name at $ks (survives deploy)"
    fi
  done

  # S13 - staging is a rehearsal instance and must never be exposed publicly.
  if [ "$svc" = "staging" ]; then
    if grep -q "plinth-staging" /etc/caddy/Caddyfile 2>/dev/null; then
      fail S13 not-public "staging appears in the Caddyfile - a rehearsal instance is on the public internet"
    else
      pass S13 not-public "staging is not routed by Caddy"
    fi
  fi
}

# ---------------------------------------------------------------- per-pair
check_pair() { # <a> <b>
  local a="$1" b="$2" ea eb db_a db_b da da2
  ea="$(svc_env_file "$a")"; eb="$(svc_env_file "$b")"

  # S1 - different release trees.
  local ra rb
  ra="$(readlink -f "$ROOT/$(svc_slot "$a")" 2>/dev/null || true)"
  rb="$(readlink -f "$ROOT/$(svc_slot "$b")" 2>/dev/null || true)"
  if [ -n "$ra" ] && [ -n "$rb" ] && [ "$ra" != "$rb" ]; then
    pass S1 release-path "$a and $b run different releases"
  else
    fail S1 release-path "$a and $b resolve to the same release ('$ra' / '$rb')"
  fi

  # S2 - different database and data directory, and not nested.
  db_a="$(env_value "$ea" DATABASE_PATH)"; db_b="$(env_value "$eb" DATABASE_PATH)"
  da="$(env_value "$ea" DATA_DIR)"; da2="$(env_value "$eb" DATA_DIR)"
  if [ "$db_a" = "$db_b" ]; then
    fail S2a database-path "$a and $b share DATABASE_PATH=$db_a"
  else
    pass S2a database-path "$a=$db_a"
  fi
  if [ "$da" = "$da2" ]; then
    fail S2b data-dir "$a and $b share DATA_DIR=$da"
  else
    pass S2b data-dir "$a=$da / $b=$da2"
  fi
  case "$da/" in "$da2"/*) fail S2c data-nested "$a DATA_DIR is inside $b DATA_DIR" ;; esac
  case "$da2/" in "$da"/*) fail S2c data-nested "$b DATA_DIR is inside $a DATA_DIR" ;; esac

  # S3 - the identity secrets that let one environment mint the other's
  # credentials must differ. Values are never printed, only sha256 prefixes.
  local k va vb shared=0
  for k in JWT_SECRET RECOVERY_PEPPER BILLING_BINDING_SECRET PURCHASE_TOKEN_KEK; do
    va="$(env_value "$ea" "$k")"; vb="$(env_value "$eb" "$k")"
    if [ -z "$va" ] || [ -z "$vb" ]; then
      fail S3 secret-identity "$k missing in $a or $b"
      shared=1
    elif [ "$(sha12 "$va")" = "$(sha12 "$vb")" ]; then
      fail S3 secret-identity "$k is IDENTICAL in $a and $b (sha256:$(sha12 "$va")) - one environment can forge the other's tokens/entitlements"
      shared=1
    else
      pass S3 secret-identity "$k differs ($a $(sha12 "$va") / $b $(sha12 "$vb"))"
    fi
  done

  # S4 - the provider credential. A shared FAL_KEY means a debug run spends the
  # production provider budget and a debug leak burns the production key. It
  # cannot be separated without a second provider account, which is the owner's
  # decision - so this row fails and the flag records that decision.
  va="$(env_value "$ea" FAL_KEY)"; vb="$(env_value "$eb" FAL_KEY)"
  if [ -z "$va" ] || [ -z "$vb" ]; then
    pass S4 secret-provider "FAL_KEY not configured in both (no shared provider credential)"
  elif [ "$(sha12 "$va")" != "$(sha12 "$vb")" ]; then
    pass S4 secret-provider "FAL_KEY differs ($a $(sha12 "$va") / $b $(sha12 "$vb"))"
  elif [ "$ALLOW_SHARED_PROVIDER_KEY" = "1" ]; then
    CHECKS=$((CHECKS + 1))
    printf 'WARN  %-3s %-22s %s\n' "S4" "secret-provider" \
      "FAL_KEY is shared between $a and $b (sha256:$(sha12 "$va")); accepted by --allow-shared-provider-key, owner decision pending a second provider key"
  else
    fail S4 secret-provider "FAL_KEY is SHARED between $a and $b (sha256:$(sha12 "$va")) - debug spend hits the production provider account; provision a separate key or record the owner's decision and pass --allow-shared-provider-key"
  fi

  # S8 - the proxy prefix must be one-way. Caddy's `handle_path /plinth/*` does
  # not match /plinth-debug/*, but that is a fact about the config file and is
  # measured rather than assumed: the two public paths must answer with
  # different payloads.
  local site caddy_a caddy_b
  site="$(grep -E '^SITE_ADDRESS=' /etc/caddy/caddy.env 2>/dev/null | cut -d= -f2- | tr -d '"' || true)"
  [ -n "$site" ] || site="pikupo.com"
  # SITE_ADDRESS is a bare host in caddy.env; curl needs a scheme. Without this
  # the check measured its own malformed URL and reported the proxy as broken.
  case "$site" in
    http://*|https://*) ;;
    *) site="https://$site" ;;
  esac
  caddy_a="$(awk -v p="$(svc_prefix "$a")" '$1=="handle_path" && $2==p"*" {getline; print}' /etc/caddy/Caddyfile)"
  caddy_b="$(awk -v p="$(svc_prefix "$b")" '$1=="handle_path" && $2==p"*" {getline; print}' /etc/caddy/Caddyfile)"
  if printf '%s' "$caddy_a" | grep -q ":$(svc_port "$a")"; then
    pass S8a caddy-route "Caddy $(svc_prefix "$a")* -> $(svc_port "$a")"
  else
    fail S8a caddy-route "Caddy has no route $(svc_prefix "$a")* -> $(svc_port "$a")"
  fi
  if printf '%s' "$caddy_b" | grep -q ":$(svc_port "$b")"; then
    pass S8b caddy-route "Caddy $(svc_prefix "$b")* -> $(svc_port "$b")"
  else
    fail S8b caddy-route "Caddy has no route $(svc_prefix "$b")* -> $(svc_port "$b")"
  fi
  if [ "$a" = "prod" ] && [ "$b" = "debug" ]; then
    local body_a body_b
    body_a="$(curl -fsS -m 10 "$site$(svc_prefix "$a")v1/config" 2>/dev/null || true)"
    body_b="$(curl -fsS -m 10 "$site$(svc_prefix "$b")v1/config" 2>/dev/null || true)"
    if [ -z "$body_a" ] || [ -z "$body_b" ]; then
      fail S8c prefix-isolation "could not read both public prefixes from $site"
    elif [ "$body_a" = "$body_b" ]; then
      fail S8c prefix-isolation "prod and debug prefixes returned the SAME payload - the split cannot be proven"
    else
      pass S8c prefix-isolation "prod and debug prefixes answer with different payloads (release build's /plinth/v1 reaches prod only)"
    fi
  fi
}

# ------------------------------------------------------------------- run
SERVICES=()
for s in $(service_names); do service_exists "$s" && SERVICES+=("$s"); done
[ "${#SERVICES[@]}" -gt 0 ] || die "no Plinth services found under $UNIT_DIR"

if [ -n "$ONLY_SERVICE" ]; then
  svc_known "$ONLY_SERVICE" || die "unknown service: $ONLY_SERVICE"
  service_exists "$ONLY_SERVICE" || die "service not installed: $ONLY_SERVICE"
  echo "== separation audit: $ONLY_SERVICE =="
  check_service "$ONLY_SERVICE"
elif [ -n "$PAIR" ]; then
  a="${PAIR%%,*}"; b="${PAIR##*,}"
  svc_known "$a" && svc_known "$b" || die "unknown service in --pair: $PAIR"
  service_exists "$a" && service_exists "$b" || die "both services must be installed for --pair"
  echo "== separation audit: $a vs $b =="
  check_service "$a"
  check_service "$b"
  check_pair "$a" "$b"
else
  echo "== separation audit: all services (${SERVICES[*]}) =="
  for s in "${SERVICES[@]}"; do check_service "$s"; done
  for ((i = 0; i < ${#SERVICES[@]}; i++)); do
    for ((j = i + 1; j < ${#SERVICES[@]}; j++)); do
      check_pair "${SERVICES[i]}" "${SERVICES[j]}"
    done
  done
fi

echo
echo "== summary: $CHECKS checks, $FAILURES failed =="
if [ "$FAILURES" -ne 0 ]; then
  echo "SEPARATION: FAIL"
  exit 1
fi
echo "SEPARATION: PASS"
