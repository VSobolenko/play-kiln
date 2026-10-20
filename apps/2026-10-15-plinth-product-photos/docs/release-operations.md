# Release operations

This is the runbook for the Plinth server behind `https://pikupo.com/plinth/v1/`. It describes the
scripts that exist in `deploy/` and the host layout those scripts read, not an intended design. Every
path below is either defined in `deploy/lib.sh` or read from the service's environment file.

Scope: the TypeScript server, its SQLite database, its systemd units and the operator controls. The
Android release is covered by the slot's store pack and Play Console checklist, not here.

---

## 1. The host, as the scripts define it

`deploy/lib.sh` is the single source of these paths. A script sources it; nothing restates a path.

| Item | Value |
|---|---|
| Root | `/opt/plinth-product-photos` |
| Immutable releases | `/opt/plinth-product-photos/releases/<UTC stamp>` |
| Live symlinks | `current` (prod), `current-debug` (debug), `current-staging` (staging) |
| Incoming tarballs | `/opt/plinth-product-photos/incoming` |
| Backups | `/opt/plinth-product-photos/backups/<service>` |
| Deploy state | `/opt/plinth-product-photos/state` |
| Env files | `/etc/plinth-product-photos/server.env`, `server-debug.env`, `server-staging.env` |
| systemd units | `plinth-product-photos.service`, `plinth-product-photos-debug.service`, `plinth-product-photos-staging.service` |
| Loopback ports | prod `3101`, debug `3102`, staging `3103` |
| Data directories | `/var/lib/plinth-product-photos`, `…-debug`, `…-staging` |
| Public prefixes | `/plinth/`, `/plinth-debug/`, `/plinth-staging/` |

A release directory **is** the server package: `deploy/snapshot.sh` archives
`apps/2026-10-15-plinth-product-photos/server` and `deploy/release.sh` extracts it with three path
components stripped, so `package.json` sits at the release root. The systemd units run with
`WorkingDirectory=/opt/plinth-product-photos/current-debug` (or `current`) and
`node dist/src/index.js`.

The two production facts that are not in this repository and cannot be verified from it:

- The Caddy site (`pikupo.com`) proxies `handle_path /plinth/*` to `127.0.0.1:3101` and must keep its
  SPA fallback after that handler.
- `check-separation.sh` reads `/etc/caddy/Caddyfile`, `/etc/caddy/caddy.env`, systemd units and env
  files on the host. It is the authority on what is installed; run it before every promotion.

---

## 2. Deploy a release

The rule: **a release is built from a named tree, never from the working tree.** `snapshot.sh` uses
`git archive`, so uncommitted Kotlin or TypeScript cannot leak into a deploy.

### 2.1 Build and verify the snapshot (from the repository root)

The revision must be a **commit** (or a tag), never `git write-tree`: the index is a shared file and
captures whatever another agent has staged, so with parallel workers in this tree it is not a clean
input. `snapshot.sh` accepts any tree-ish and records the resolved tree object in the sidecar.

```bash
REV="$(git rev-parse HEAD)"   # or the commit the task names
SLOT=apps/2026-10-15-plinth-product-photos

# Test the revision's bytes, not the working tree. This is the only place tests run:
# release.sh builds and health-checks a release but does not run the test suite.
rm -rf .kiln/scratch/verify && mkdir -p .kiln/scratch/verify
git archive "$REV" "$SLOT/server" | tar -x -C .kiln/scratch/verify
( cd ".kiln/scratch/verify/$SLOT/server" && npm ci && npm test && npx tsc --noEmit )

deploy/snapshot.sh "$REV" --out .kiln/scratch/releases/plinth.tgz
```

The slot's `deploy/snapshot.sh` lives under the slot, so run it from
`apps/2026-10-15-plinth-product-photos` (or give its full path).

`npm run build` happens inside the release on the host; `dist/migrations/` comes from
`scripts/copy-migrations.mjs`, and `release.sh` refuses a release without it. Running a build in the
working tree proves nothing about the snapshot.

On Windows the same script runs under Git Bash/WSL. The output is a `.tgz` plus a `.tgz.identity`
sidecar naming `tree` and `archive_sha256`; `release.sh` refuses a sidecar whose hash disagrees with
the archive.

### 2.2 Upload and deploy

```bash
scp plinth.tgz plinth.tgz.identity root@78.111.89.46:/opt/plinth-product-photos/incoming/

ssh root@78.111.89.46 \
  '/opt/plinth-product-photos/deploy/release.sh staging --tarball /opt/plinth-product-photos/incoming/plinth.tgz'
```

`release.sh <service>` runs nine steps and stops at the first failure:

1. verifies the snapshot sidecar,
2. extracts to `releases/<UTC stamp>`,
3. `npm ci` in the release,
4. `npm run build` and `npm prune --omit=dev`,
5. **backup before migration** (`deploy/backup.sh`),
6. applies migrations with `db.ts` (`PRAGMA user_version`),
7. runs `check-separation.sh` for the service (or `--pair prod,debug` when promoting),
8. atomically swaps the symlink and restarts the unit,
9. prunes unreferenced releases older than 7 days, keeping the newest 10.

The health gate is not a process check. `activate_service` requires all of:

- `systemctl is-active` is true,
- `/health` answers `200` within 40 s,
- `/v1/config` answers `200` and contains `"killSwitch"`,
- `/proc/<MainPID>/cwd` resolves to the release just switched.

If any of those fail, the script re-points the symlink at the previous release and exits non-zero. It
never prints `RELEASE: OK` for a service that is not serving.

Promote to production with the deliberate second word:

```bash
ssh root@78.111.89.46 \
  '/opt/plinth-product-photos/deploy/release.sh prod --tarball /opt/plinth-product-photos/incoming/plinth.tgz --promote'
```

`release.sh prod` without `--promote` refuses. That is intentional: production promotion follows the
owner's release approval and is never a side effect of deploying staging.

### 2.3 Verify after deploy

```bash
curl -fsS https://pikupo.com/plinth/health
curl -fsS https://pikupo.com/plinth/v1/config | head -c 600
ssh root@78.111.89.46 'systemctl is-active plinth-product-photos.service; readlink -f /opt/plinth-product-photos/current'
```

A `200 text/html` from `/plinth/health` is Caddy's SPA fallback, not the server. Accept only JSON with
`"service":"plinth-product-photos-server"` and `"databaseReachable":true`.

---

## 3. Roll back

```bash
ssh root@78.111.89.46 '/opt/plinth-product-photos/deploy/rollback.sh'
ssh root@78.111.89.46 '/opt/plinth-product-photos/deploy/rollback.sh debug'
ssh root@78.111.89.46 '/opt/plinth-product-photos/deploy/rollback.sh prod --to 20261019-011255'
```

`rollback.sh` reads `state/<service>.previous`, switches the symlink, restarts, and re-runs the same
health gate. If the target does not come up it restores the release that was running and exits
non-zero. It records the release it left in `state/<service>.previous`, so running it twice returns
forward.

**A rollback does not undo a migration.** `release.sh` backs up *before* migrating precisely because
the schema is forward-only. If a new release wrote data under a new schema and the old code cannot read
it, rolling the symlink back is not enough: restore the pre-migration backup (section 6) or write a
forward migration that makes the old shape readable. Never edit `user_version` by hand.

---

## 4. Pause, close and reopen

Generation is refused by reading files on disk **per request**, so no restart is needed and no
in-flight read is interrupted. The precedence is decided by `server/src/service-state.ts:72-89`:

| State | Control | Meaning |
|---|---|---|
| `closed` | `SERVICE_CLOSED_FILE` exists | We stopped running the service. Final. Outranks everything. |
| `maintenance` | `KILL_SWITCH_FILE` exists (and the closed file does not) | Temporary pause. |
| `update_required` | Client version below `MIN_APP_VERSION` | That build must not spend. |
| `ok` | neither operator file exists | Normal. |

Use the operator script; it resolves both paths the way the server does, writes the intent and
timestamp into the file, verifies `/v1/config` reports the state, and leaves `/health` answering:

```bash
ssh root@78.111.89.46 '/opt/plinth-product-photos/deploy/state.sh maintenance 30m'
ssh root@78.111.89.46 '/opt/plinth-product-photos/deploy/state.sh closed'
ssh root@78.111.89.46 '/opt/plinth-product-photos/deploy/state.sh ok'
ssh root@78.111.89.46 '/opt/plinth-product-photos/deploy/state.sh --check'
```

`state.sh` removes the other marker when returning to `ok`. For `closed` it writes **both** the closed
marker and the kill switch, because an older build reads only the kill switch (`docs/service-states.md`).
Both paths must be configured in the service's env file, and `check-separation.sh` invariant **S12**
fails a service that leaves either one unset. Production's env carried only `KILL_SWITCH_FILE` until
2026-10-19; `SERVICE_CLOSED_FILE` was added then, so `state.sh closed` can now actually close prod.

A manual check, without the script:

```bash
ssh root@78.111.89.46 '
  set -a; . /etc/plinth-product-photos/server.env; set +a
  echo "closed: ${SERVICE_CLOSED_FILE:-unset} exists=$([ -e "${SERVICE_CLOSED_FILE:-/nonexistent}" ] && echo yes || echo no)"
  echo "pause:  ${KILL_SWITCH_FILE:-unset} exists=$([ -e "${KILL_SWITCH_FILE:-/nonexistent}" ] && echo yes || echo no)"
'
curl -fsS https://pikupo.com/plinth/v1/config | python3 -c "import json,sys; c=json.load(sys.stdin); print(c['serviceState'], c['notice']['code'] if c['notice'] else None)"
```

What stays open in `maintenance` and `closed`: `GET /v1/wallet`, `GET /v1/jobs/:id`,
`GET /v1/jobs/:id/result`, `POST /v1/jobs/:id/cancel` (the refund), delivery receipts, reports and
account deletion. What is refused: new jobs, recognition, uploads, purchase intents/confirmation and
recovery-code generation. The full matrix is in `docs/service-states.md`.

---

## 5. Raise the minimum app version

`MIN_APP_VERSION` is read from the environment **at process start** (`server/src/config.ts:113`), so
changing it needs a restart, unlike the state files.

```bash
ssh root@78.111.89.46 '
  cp -a /etc/plinth-product-photos/server.env /etc/plinth-product-photos/server.env.bak-$(date -u +%Y%m%d-%H%M%S)
  sed -i "s/^MIN_APP_VERSION=.*/MIN_APP_VERSION=1.4.0/" /etc/plinth-product-photos/server.env
  systemctl restart plinth-product-photos.service
'
curl -fsS https://pikupo.com/plinth/v1/config | python3 -c "import json,sys; print(json.load(sys.stdin)['minAppVersion'])"
```

Rules that matter:

- The version compared is the one **signed into the device token** at session creation
  (`server/src/routes.ts:220-223`), not the `X-App-Version` header of a later call. A caller cannot
  escape the gate by editing a header.
- A token with no `ver` claim is treated as below any enforceable minimum (`versions.ts:48-55`). That
  is deliberate fail-closed behaviour for money, and it has a transition cost: sessions issued by a
  server older than this release carry no `ver`, so after raising the minimum those users are refused
  until the app starts a new session. Raise the minimum after the client handles `426` by
  re-sessioning, or accept one restart per user.
- `MIN_APP_VERSION=0` (or an unparseable value) means "no gate". Do not set it to a value you have not
  shipped: raising it above the current store build locks everyone out of spending.
- `update_required` is reported by `GET /config` only when the caller sends `X-App-Version`. The
  session response computes it from the attested version, so a client that never calls `/config` still
  learns the state when it creates its session. The mutating routes refuse regardless of what any
  client renders.

---

## 6. Backup and restore

### 6.1 Taking a backup

```bash
ssh root@78.111.89.46 '/opt/plinth-product-photos/deploy/backup.sh prod'
ssh root@78.111.89.46 '/opt/plinth-product-photos/deploy/backup.sh prod --keep 14'
```

`deploy/backup.sh` uses SQLite's online backup API through `better-sqlite3`, so the copy is
transactionally consistent while the service keeps serving. It then:

1. checkpoints the copy and switches it to rollback-journal mode, so the backup is **one file**
   (a WAL pair copied as one file loses the newest committed frames),
2. opens the copy read-only and runs `PRAGMA integrity_check`,
3. counts rows in `wallet`, `device`, `job`, `ledger`, `job_upload`,
4. deletes the copy and exits non-zero if either check fails,
5. prunes only files this script created, keeping the newest `--keep` (default 7).

The same mechanism is available from the server package for a local drill:

```bash
cd apps/2026-10-15-plinth-product-photos/server
npm run build
DATABASE_PATH=../../.kiln/scratch/infra/drill.sqlite \
  node scripts/backup-db.mjs --out ../../.kiln/scratch/infra/backups
node scripts/backup-db.mjs --verify ../../.kiln/scratch/infra/backups/plinth-<stamp>.sqlite
```

`server/src/backup.ts` writes `<destination>.partial`, verifies it, and only then renames it, so a
failed backup never carries a successful name. The unit test that pins this is
`server/test/backup.test.ts`.

Backups live on the same host, so they do **not** survive the host. Copy them off the machine on a
schedule the owner controls:

```bash
scp root@78.111.89.46:/opt/plinth-product-photos/backups/prod/prod-<stamp>.sqlite <off-host destination>
```

A backup that never left the host is not a disaster-recovery plan. The files contain wallet ids and
ledger history; treat them as personal data (mode `640`, root-owned, never in git).

### 6.2 Restore drill (safe, repeatable)

**Local drill, no host access.** `npm run drill:restore` builds a WAL database, takes a blind copy of
the live file (which must fail), runs the real `backupDatabase` seal, and reads the sealed snapshot
back on a separate connection. It writes only under `.kiln/scratch/infra/restore-drill/`.

```bash
cd apps/2026-10-15-plinth-product-photos/server
npm run build
npm run drill:restore
```

Measured on 2026-10-19:

```json
{
  "live": { "wallets": 2, "ledger": 25, "balance": 13 },
  "blindCopyError": "no such table: wallet",
  "backup": { "integrity": "ok", "tables": 4, "wallets": 2, "ledgerEntries": 25 },
  "journalModeAfterSeal": "delete",
  "sealedIsSingleFile": true,
  "restoredRows": { "wallets": 2, "ledger": 25, "balance": 13 },
  "matchesLive": true
}
```

The blind copy failing is the point: `restore.sh` installs one file, so the seal must be what makes
the snapshot readable. A clean exit 0 is the drill's own verdict; a mismatch names the row and exits 2.

**Host drill.** Restore into a scratch path first. `restore.sh` refuses to overwrite a live service's
database and verifies what landed, not what was copied.

```bash
ssh root@78.111.89.46 '
  /opt/plinth-product-photos/deploy/restore.sh \
    /opt/plinth-product-photos/backups/prod/prod-<stamp>.sqlite \
    --db /var/lib/plinth-product-photos/restore-drill.sqlite
'
```

Expected output ends with `RESTORE: OK` and a JSON line containing `"integrity":"ok"` and non-zero
counts for the tables that exist. Then remove the drill file:

```bash
ssh root@78.111.89.46 'rm -f /var/lib/plinth-product-photos/restore-drill.sqlite*'
```

### 6.3 Restore for real

```bash
ssh root@78.111.89.46 '
  /opt/plinth-product-photos/deploy/restore.sh \
    /opt/plinth-product-photos/backups/prod/prod-<stamp>.sqlite \
    --db /var/lib/plinth-product-photos/server.sqlite \
    --stop-service prod
  systemctl start plinth-product-photos.service
'
```

`restore.sh` stops the unit, deletes the stale `-wal`/`-shm` beside the target (replaying them over a
restored file is the corruption the script exists to avoid), installs the file, and verifies the
restored copy. It deliberately does not start the service: **the restart is the moment a bad restore
becomes visible.** Watch `journalctl -u plinth-product-photos.service` and `/health` after it.

A restored database is a point-in-time state. Credits spent or granted after the backup are gone; if
the backup is old, reconcile purchases against Play (`PurchaseService.reconcileVoided` runs daily)
before telling users the balance is authoritative.

---

## 7. Health, alerts and logs

`GET /health` (and `/healthz`) performs a real read of `sqlite_master` on every probe and returns:

- `200` with `{"ok":true,"databaseReachable":true,"serviceState":"…"}` when the process and database
  answer,
- `503` with `{"ok":false,"databaseReachable":false,…}` when the database cannot be read.

`serviceState` in `/health` is the **operator** state (`ok|maintenance|closed`); it never says
`update_required`, because a machine probe has no client version.

What to page on:

- `/health` returns non-200 **twice in a row** (a single miss can be a deploy restart),
- `/v1/config` returns non-200 or its `serviceState` disagrees with the expected operator state,
- the process is active but `/health` answers HTML (Caddy route lost),
- disk usage on `/var/lib/plinth-product-photos` or `/opt/plinth-product-photos` above 85%,
- a provider error burst in the journal (`PROVIDER_FAILED`, `PURCHASE_UNAVAILABLE`) for more than
  five minutes,
- `PLAY_FINALIZING` rows older than one hour (a purchase that cannot be finalized is a paid user who
  has not received credits).

A minimal cron check, no vendor required:

```bash
* * * * * curl -fsS -m 5 https://pikupo.com/plinth/health | grep -q '"databaseReachable":true' \
  || logger -t plinth-health 'health check failed'
```

Logs:

```bash
journalctl -u plinth-product-photos.service -n 200 --no-pager
journalctl -u plinth-product-photos.service --since '30 min ago' | grep -E 'RATE_LIMITED|UPDATE_REQUIRED|SERVICE_'
```

The logger redacts `req.url`, `authorization`, `x-result-token`, `integrityToken`, `purchaseToken` and
the report note (`server/src/app.ts:40-43`). No route logs a secret. Do not print the env file, and do
not paste `journalctl` output containing a purchase token into a ticket.

---

## 8. Rotate a secret

All secrets live in `/etc/plinth-product-photos/server.env`, mode root-readable and service-readable
only. Never put one in a release, a doc, a test fixture or a log.

| Secret | Consequence of rotating | Procedure |
|---|---|---|
| `JWT_SECRET` | Every issued device token becomes invalid; all clients must create a new session. No data is lost. | Replace, restart, watch `/health`. Clients recover on next launch. |
| `RECOVERY_PEPPER` | Existing `wallet.recovery_hash` values no longer match, so old recovery codes stop working for users who wrote one down. | Rotate only with the owner's decision; announce that recovery codes must be regenerated in-app. |
| `BILLING_BINDING_SECRET` | In-flight purchase intents carry account/profile hashes computed with the old secret and will fail confirmation. Completed purchases are unaffected. | Drain `purchase_intent WHERE used_at IS NULL` (or wait out its lifetime), then rotate and restart. |
| `PURCHASE_TOKEN_KEK` | Rows in `purchase.state='PLAY_FINALIZING'` become undecryptable, so their finalization can never retry. | Confirm `SELECT count(*) FROM purchase WHERE state='PLAY_FINALIZING'` is 0, then rotate and restart. |
| `FAL_KEY` | Provider calls fail until the new key is in place; credits are refunded on failure by the ledger. | Run `/opt/plinth-product-photos/deploy/rotate-provider-key.sh <debug\|prod>`, then one real job with the owner's spending authorization. |
| `PLAY_SERVICE_ACCOUNT_JSON` | Purchase verification and RTDN identity checks fail closed (503), so nobody is charged. | Replace with a new key file path/JSON, restart, confirm a purchase intent can be created. |

Every rotation ends with the same proof:

```bash
ssh root@78.111.89.46 'systemctl restart plinth-product-photos.service'
curl -fsS https://pikupo.com/plinth/health
```

`check-separation.sh` compares `JWT_SECRET`, `RECOVERY_PEPPER`, `BILLING_BINDING_SECRET` and
`PURCHASE_TOKEN_KEK` between prod and debug by hash prefix and fails if they are shared. Run it after
any rotation.

### FAL_KEY provider-key rotation

Run `/opt/plinth-product-photos/deploy/rotate-provider-key.sh <debug|prod>`. The new key is typed twice
at a no-echo prompt, or read from a file the owner already keeps outside git with
`--key-file <path>`; it is never passed as an argument, so a key never reaches the process list or the
shell history. Two preconditions are enforced before anything is written: the value must look like a
provider key (`<id>:<secret>`, 32–200 characters, no whitespace or quotes), and its fingerprint must
differ from the *other* service's key — a shared key is the defect the run exists to remove, so it is a
hard refusal, not a warning.

`--dry-run` performs every check and prints what it would do, writing and restarting nothing.

The write is atomic and reversible: the current env file is copied to `server[-debug].env.bak-<UTC>`
(mode 600), the replacement is built in a temporary file in the same directory, and a redacted diff
must show that the `FAL_KEY=` line is the only difference before it is installed. The service is then
restarted and must answer both `/health` and `/v1/config`; if either fails, the backup is restored and
the service restarted again, so a rotation that breaks the service leaves the previous key in place.

The rotator takes the same `flock` as `release.sh` for its duration. Note that the lock *file* is
ordinary and stays on disk after a deploy — what matters is whether a live process holds the lock, so
the rotator asks `flock -n`, not "does the path exist".

The proof is `/opt/plinth-product-photos/deploy/check-separation.sh --pair prod,debug` reporting `S4`
as `differs`. That proves the two services no longer share a credential; it does **not** prove the new
key is valid at the provider, because no auth-free endpoint distinguishes a good key from a bad one.
Only one real job does — run it with the owner's spending authorization, and watch for a
`PROVIDER`/`401` entry in `journalctl -u <unit>` before declaring the rotation complete.

A queue status request is not a valid probe: `GET https://queue.fal.run/.../requests/<uuid>` answers
`404 Request not found` for a valid key, a bogus key and no key at all (measured 2026-10-19), so it
distinguishes nothing.

### The provider key lives outside the repository

The owner keeps the production provider key in `.kiln/prod.key_fal` — inside the project, under the
gitignored `.kiln/` tree, never committed and never echoed. `--key-file` points the rotator at it. After
a rotation, delete any transient copy from the host (`shred -u`) rather than leaving one in `/root`.


---

## 9. Incident quick reference

| Symptom | First check | Likely action |
|---|---|---|
| `/health` 503, `databaseReachable:false` | `ls -l $DATABASE_PATH`, disk space, `journalctl` | restore a backup (6.3) or fix the mount; do not delete the database |
| `/health` 200 but app cannot start a session | Play Integrity config, `PLAY_PACKAGE_NAME`, package version code | fix config; sessions fail closed by design |
| 429 for real users | which bucket: `RATE_LIMITED` log carries `key=ip|dev` and the limit | raise the rule deliberately in `rate-limit.ts`, not by disabling it |
| 426 for real users | `MIN_APP_VERSION` against the installed build | lower it or ship the update; never remove the gate |
| Jobs queue but never finish | provider key, `CAP_REACHED`, job state `QUEUED` | check `DAILY_SPEND_CAP_USD`, provider status; refunds release reservations |
| Paid user has no credits | `purchase` state `PLAY_FINALIZING`, `last_error` | `retryFinalizations` runs every minute; check Play service-account access |
| Deploy failed | `release.sh` output names the failed step | fix and redeploy; the previous release stays live, or roll back explicitly |

---

## 10. What this runbook does not cover

- **Caddy and DNS.** A Caddy change is a host change; keep a timestamped `Caddyfile` backup and run
  `caddy validate` before reloading.
- **Host provisioning.** Users, directories, ownership, `harden-identity.sh` and the systemd unit
  contents are measured by `check-separation.sh`, not created here.
- **Monitoring beyond the cron probe.** No vendor is assumed and no dashboard is claimed.
- **A migration reversal tool.** Migrations are forward-only; the remedy is a backup or a new forward
  migration.
- **Disaster recovery when the host is gone.** `backup.sh` writes to the same machine; copying those
  files off-host is a separate, owner-controlled step.
- **The deploy scripts themselves were read, not executed, when this runbook was written.** The
  authoring machine is Windows with no `bash` on `PATH` (only the `C:\Windows\system32\bash.exe` WSL
  launcher, whose installed distro carries no bash), so `bash -n` could not run and **no syntax check
  is claimed for `deploy/*.sh`**. Before the first deploy from this revision, run it on the host:
  `bash -n /opt/plinth-product-photos/deploy/{lib,release,rollback,state,backup,restore,snapshot,check-separation}.sh`.
  A syntax error there costs nothing; the same error mid-deploy costs a rollback.
