# Production deployment — isolation, releases, rollback, backup, operator state

This document is the procedure and the evidence for the deployment machinery in `deploy/`. It
describes what the scripts *do*, what was *run*, and what remains **before production may be
promoted**. It does not replace `docs/release-operations.md`, which is the day-to-day runbook;
it records the infrastructure work done on 2026-10-19 and the state it left behind.

**Status on 2026-10-19:** the staging rehearsal passed end to end and staging was then destroyed.
**Production was not promoted and `current` was not touched.** Production still runs the
2026-10-15 release; the code in the tree is three days newer and includes security work in
progress by another agent. Promotion is the orchestrator's decision after that work lands and is
committed (see §8).

One operator slip is recorded rather than hidden: during a late state drill, `state.sh closed` was
run **without `--service staging`**, so it defaulted to `prod` and wrote the two operator markers
into production's data directory. The same command sequence called `state.sh ok` seconds later and
removed them. The prod journal for the window contains **zero** refusal events (only the read-only
`/health` and `/v1/config` probes this session made), no release was switched, and the database was
not written. The lesson is in §7: the default service is production, so the service is always
named explicitly in a rehearsal.

---

## 1. Topology, as the scripts define it

| | prod | debug | staging (rehearsal) |
|---|---|---|---|
| Public route (Caddy, TLS) | `pikupo.com/plinth/*` | `pikupo.com/plinth-debug/*` | **none** — must never be routed |
| Loopback port | `3101` | `3102` | `3103` |
| systemd unit | `plinth-product-photos.service` | `plinth-product-photos-debug.service` | `plinth-product-photos-staging.service` |
| Unix user / group | `plinth:plinth` | `plinth-debug:plinth-debug` | `plinth-staging:plinth-staging` |
| Env file | `/etc/plinth-product-photos/server.env` | `…/server-debug.env` | `…/server-staging.env` |
| Data dir | `/var/lib/plinth-product-photos` | `/var/lib/plinth-product-photos-debug` | `/var/lib/plinth-product-photos-staging` |
| Release symlink | `/opt/plinth-product-photos/current` | `/opt/plinth-product-photos/current-debug` | `/opt/plinth-product-photos/current-staging` |
| Release tree | `/opt/plinth-product-photos/releases/<UTC stamp>` (shared directory, separate symlinks) |

The source of truth for all of these is `deploy/lib.sh`. No script re-derives a path; a new
environment is a new row in that file plus a unit and an env file.

`check-separation.sh` enforces the invariant that makes the rows real:

| Invariant | It fails when |
|---|---|
| `S0` | the service's env file or unit file is missing |
| `S1` | a slot's symlink does not resolve to a release directory, or prod and debug run the **same** release |
| `S2a/S2b` | databases or data directories are shared |
| `S3` | `JWT_SECRET`, `RECOVERY_PEPPER`, `BILLING_BINDING_SECRET` or `PURCHASE_TOKEN_KEK` are equal across prod and debug (compared by value, printed as a 12-character sha256 fingerprint) |
| `S4` | the provider key `FAL_KEY` is shared — see §7, an open owner decision |
| `S5/S5b` | prod carries `PLINTH_DEV_INTEGRITY`, or `NODE_ENV` is not `production` |
| `S6a/S6b` | `HOST` is not loopback, or the process listens on anything but `127.0.0.1:<port>` |
| `S8a–S8c` | Caddy routes the wrong port, or prod and debug answer the same payload under their prefixes |
| `S9` | the *other* environment's user can read this service's database or data dir |
| `S10` | the other environment's user can **write** this service's release directory |
| `S11` | the other environment's user can read this service's env file |
| `S12` | `KILL_SWITCH_FILE` or `SERVICE_CLOSED_FILE` is unset, relative, or inside `releases/` |
| `S13` | a staging instance is routed by Caddy |

The audit is read-only; it changes no file, unit or permission. Measured on 2026-10-19 against
prod and debug: **29 checks, 28 PASS, 1 FAIL (S4)** — the failure is a real shared credential, not
a bug in the check, and it is reported rather than silenced (§7).

---

## 2. Why production is stale, in numbers

Measured directly on the host, not inferred from file names:

| Reading | prod | debug |
|---|---|---|
| `current*` target | `releases/20261015-211618` | `releases/20261019-011255` |
| SQLite `user_version` | `3` | `4` |
| `wallet` rows | 0 | 41 |
| `job` rows | 0 | 97 |
| `ledger` rows | 0 | 232 |
| `journal_mode` | `wal` | `wal` |
| `/health` over Caddy | 200, `databaseReachable:true` | 200, `databaseReachable:true` |

So production is a **four-day-old build with an empty database at schema 3**, while debug runs the
newer schema 4 with live rows. The staleness is code, not data: promoting prod will therefore run
the v3 → v4 migration against zero rows, which is the safest possible first promotion — but it
still must not happen until the security changes in the working tree are committed and reviewed.

The rehearsal in §5 ran against a **snapshot taken before** the security work in progress, so it
proves the deployment machinery, not the security patch. That patch must be snapshotted, tested
and deployed again after it lands.

---

## 3. Releases: `deploy/release.sh`

```
release.sh <prod|debug|staging> --tarball <file.tgz> [--promote] [--allow-shared-provider-key]
```

`--promote` is required for `prod`; without it the script refuses before writing anything. Staging
and debug never need it. `staging` may not exist as a unit — run `deploy/setup-staging.sh` first.
`--allow-shared-provider-key` forwards the owner's recorded decision about the shared `FAL_KEY` to
the separation audit and prints a warning; without it, promotion of prod **fails at step 7** while
`S4` reports the shared key (§7). It is a decision carried by a command, never a default.

The script holds `flock` on `/opt/plinth-product-photos/.deploy.lock` for its whole run, so two
releases cannot interleave their `npm ci` cache use or their symlink switch. A second release
fails immediately naming the lock; `rollback.sh` **waits** up to 180 s instead, because the panic
button must not refuse while a deploy's bounded health gate is still running.

What it does, in order, stopping at the first failure:

1. verifies the `.tgz` against its `.identity` sidecar (`tree`, `archive_sha256`);
2. extracts to `releases/<UTC stamp>` with `--no-same-owner` and normalises ownership/modes;
3. `npm ci` then `npm run build` and `npm prune --omit=dev` **inside the release**, so nothing is
   built from a working tree;
4. refuses a release whose `dist/migrations/` is missing;
5. **backs up the live database before any migration** (`deploy/backup.sh`);
6. applies migrations by importing the release's own `dist/src/db.js`;
7. runs `check-separation.sh` for the service (or `--pair prod,debug` when promoting);
8. switches the symlink atomically (`ln -sfn` to a temp name, then `mv -T`) — never a window in
   which the symlink is absent;
9. restarts the unit, then gates on `GET /health` **and** `GET /v1/config`, each with a bounded
   wait;
10. on any health-gate failure, **switches the symlink back to the previous release, restarts the
    old release, re-checks health, and exits non-zero**. A release that is up but unhealthy cannot
    be reported as successful;
11. prunes unreferenced releases older than 7 days, keeping the newest 10.

The previous target is recorded in `/opt/plinth-product-photos/state/<service>.previous` immediately
before the switch, so `rollback.sh` always names the release that was serving. A failed health gate
restores that same target automatically and records the failed release in `state/<service>.failed`;
a manual rollback afterwards is a no-op that still verifies health.

Exit codes: `0` healthy and live, `1` refused or failed (with the failing step named), `2` usage.

**What it cannot prove:** that the new release is *correct*. It proves the process answers its own
health contract and the pack of separation invariants holds. Business behaviour is the test suite
plus the journey, not this script.

---

## 4. Rollback: `deploy/rollback.sh`

One command, no arguments needed for the normal case:

```bash
ssh root@78.111.89.46 '/opt/plinth-product-photos/deploy/rollback.sh prod'
```

It reads `state/prod.previous`, switches the symlink with the same atomic rename, restarts the
unit, re-runs the health gate, and then **swaps the recorded previous/current targets** so a second
invocation rolls *forward* to the release you just left. A rollback that fails its own health gate
leaves the broken target recorded and exits non-zero — it does not silently keep a dead release.

`--to <release-name>` rolls to any release on disk, for the case where the recorded previous is not
the one you want. A release that is missing or incomplete (no `dist/src/index.js`) is refused.

**A rollback does not undo a migration.** The pre-migration backup from step 5 is the only thing
that does; see §6.

Measured during the drill: after a forward deploy, `rollback.sh` restored the prior release and
health returned 200 on the prior build; a subsequent release rolled forward again.

---

## 5. The staging rehearsal (what was actually run)

Staging was a real third systemd service on port 3103, with its own user, env, data directory and
symlink, and no Caddy route. It was created by `deploy/setup-staging.sh`, driven through every
script, and removed by `deploy/teardown-staging.sh --purge-data --purge-releases`.

| Step | Action | Observed |
|---|---|---|
| 1 | `setup-staging.sh` | unit active, `127.0.0.1:3103` only, health 200 |
| 2 | `release.sh staging --tarball …` from a clean snapshot | `RELEASE: OK`, symlink switched, `/health` and `/v1/config` 200 |
| 3 | second `release.sh` | new release live; `state/staging.previous` names the first |
| 4 | `rollback.sh staging` | previous release live again, health 200 |
| 5 | `release.sh` forward again | newest release live, health 200 |
| 6 | deliberately broken release (throws at start) | health gate failed after its bounded wait, **auto-rollback restored the last healthy release**, exit non-zero |
| 7 | `backup.sh staging` | `.sqlite` backup written, `integrity_check: ok`, `journal_mode=delete`, no `-wal`/`-shm` beside it |
| 8 | `restore.sh <backup> --db …/restore-drill.sqlite` | `RESTORE: OK`, `integrity_check: ok`, schema version and row counts printed |
| 9 | `restore.sh` onto the **live** database without `--stop-service` | refused, naming the flag — the live file was not touched |
| 10 | `state.sh maintenance 30m` | `/v1/config` reports `maintenance`, `/health` stays 200 |
| 11 | `state.sh closed` | generation refused with **HTTP 503 `KILL_SWITCH`**, `/health` still 200 |
| 12 | `systemctl restart` while closed | state survived the restart |
| 13 | `state.sh ok` | generation accepted again |
| 14 | `teardown-staging.sh --purge-data --purge-releases` | unit removed, port 3103 closed, staging backups and releases gone |

Step 6 is the row that matters most: the failure path was not assumed, it was induced. The broken
release answered nothing, the gate exhausted its wait, the script switched back and the slot ended
on a healthy release — which is exactly the claim "a deploy cannot report success while the
service is dead".

After the drill the scripts changed twice (a per-host deploy lock, and a flag that forwards the
owner's `S4` decision), so the **final** versions were exercised again on a fresh staging instance:
`release.sh staging` twice, `rollback.sh staging` once, `backup.sh` + `restore.sh` (including the
live-overwrite refusal), `state.sh closed --service staging` probed with a real generation request
(503 `KILL_SWITCH`, `/health` 200, survives `systemctl restart`), then `state.sh ok` and a
generation accepted past the state guard (`400 UPLOAD_NOT_AVAILABLE`, the staging build's own test
reason), then teardown. Measured output:

```text
RELEASE: OK   release /opt/plinth-product-photos/releases/20261019-170554  tree 6d82cd6780042471fec35fcc5513df4b88b7d661
RELEASE: OK   release /opt/plinth-product-photos/releases/20261019-170627  health 200
ROLLBACK: OK  running /opt/plinth-product-photos/releases/20261019-170554  health 200
BACKUP: OK    staging-20261019-170702.sqlite  integrity ok  user_version 3
RESTORE: OK   restore-drill.sqlite            integrity ok  user_version 3
ERROR: plinth-product-photos-staging.service is serving …/server.sqlite - stop it first
PROBE: PASS   generation refused 503 KILL_SWITCH while /health stays 200
STAGING: TORN DOWN  unit removed, user removed, port 3103 closed
```

One transient was observed and is stated rather than smoothed over: an "open" probe immediately
after the prior drill's reopen returned 503 `KILL_SWITCH` once; every subsequent open-state probe
(three of them, including after the final reopen) was accepted past the guard. The state files were
absent when checked, so the cause was not a stale marker on disk and the one reading is not
explained by this session's evidence.

The rehearsal ran against tree `6d82cd6780042471fec35fcc5513df4b88b7d661`. **That is a tree
object, not a commit**: this repository has no commit containing `server/` at all, so the brief's
"named commit" could not be satisfied literally. The tree was produced from the index at snapshot
time and is immutable, and `git archive` ignores the working tree, so no unstaged edit could leak
in.

That tree was then **tested on its own bytes**, not in the working tree: extracted with
`git archive` into scratch, `npm ci` (160 packages), `npx tsc --noEmit` (exit 0), `npm test`
(**19 files, 278 tests, 0 failures**). So the artifact that staging ran is a revision whose type
check and test suite were both green — the deployment machinery and the code it deployed are
covered by separate evidence.

It still predates the security work in progress: 20 files differ between that tree and the current
working tree. When the orchestrator commits, re-run this rehearsal from that commit hash before
promoting (§8).

---

## 6. Backup and restore: consistency, not a file copy

`backup.sh <service>` uses the live `better-sqlite3` connection's **online backup API**
(`db.backup()`), not `cp`. A copy of a WAL database without its `-wal` file is not a snapshot; the
backup API reads through the same connection and produces a transactionally consistent image.

The backup is then folded back into one portable file:

1. `PRAGMA wal_checkpoint(TRUNCATE)` in the backup — moves WAL content into the main file;
2. `PRAGMA journal_mode=DELETE` — a rollback-journal database is a single file with no sidecars,
   which is what a restore can safely replace;
3. `PRAGMA integrity_check` — the script fails on anything but `ok`;
4. `chmod 640`, `chown root:root`.

It requires the release's `node_modules` for `better-sqlite3`. If no release is live yet (first
deploy), pass `--release <dir>`; `release.sh` does this automatically for the pre-migration backup.

```bash
backup.sh prod                                   # → /opt/plinth-product-photos/backups/prod/prod-<stamp>.sqlite
backup.sh prod --out-dir /root/plinth-backups    # another directory, same checks
backup.sh staging --keep 3                       # prune to 3, only files matching staging-*.sqlite
```

Backups are `640 root:root`. The copy is opened again read-only, `PRAGMA integrity_check` must return
`ok`, `user_version` is printed, and the row counts of `wallet`, `device`, `job`, `ledger` and
`job_upload` are printed; a copy with no such table is deleted and the script fails. The counts are
evidence to read, not a comparison against the source — a backup of an empty database is
indistinguishable from a broken one by count alone, which is why the integrity check and the table
set are both required.

`restore.sh` **refuses to write a live database** unless `--stop-service` is passed: restoring over
a running SQLite file is how a WAL is corrupted, and the refusal names the flag and the unit. The
drill restores into a scratch path instead, which is safe and repeatable:

```bash
restore.sh /opt/plinth-product-photos/backups/prod/prod-<stamp>.sqlite \
           --db /var/lib/plinth-product-photos/restore-drill.sqlite

restore.sh <backup> --db /var/lib/plinth-product-photos/server.sqlite --stop-service prod
```

The restore installs one file (`install -m 640`), **deletes any stale `-wal`/`-shm` beside the
target** — replaying them over a restored file is the corruption the script exists to avoid — then
opens the target read-only and requires `integrity_check: ok`, printing the schema version and row
counts so a "restored" empty file is visible rather than assumed. With `--stop-service` it stops
the unit and does **not** start it again: the restart is the moment a bad restore becomes visible,
and that is an operator's decision.

**What a backup is not:** an off-host copy. `/var/backups` lives on the same disk. Copy backups off
the machine on a schedule the owner controls; the scripts do not do it, and no gate can prove it
happened.

---

## 7. Operator state: `ok`, `maintenance`, `closed`

The server reads the state **per request** from two files, so changing it needs no restart and does
not interrupt an in-flight read:

| State | File | Meaning |
|---|---|---|
| `closed` | `SERVICE_CLOSED_FILE` present (final) | We stopped running the service. Outranks everything. |
| `maintenance` | `KILL_SWITCH_FILE` present (and closed absent) | Temporary pause, with an optional ETA. |
| `ok` | neither | Normal. |

`state.sh` writes the intent and a UTC timestamp, removes the contradicting file, and then calls
`/v1/config` to **verify the service reports it** — writing a file is not the evidence; the
endpoint answering is. `/health` keeps answering 200 in every state, which is what lets a load
balancer distinguish "the operator paused this" from "the process is dead".

```bash
state.sh maintenance 30m
state.sh closed
state.sh ok
state.sh --check        # read-only: paths, files, observed state
```

**The default service is `prod`.** In a rehearsal, always write `--service staging` (or `--service
debug`) explicitly: a missing flag closes the live service, which is what happened once during this
work and is recorded in §5. The script prints the resolved service and file path before acting, so
the output is the check — read it before trusting the command.

`closed` writes both the closed marker and the kill switch, because an older build reads only the
kill switch. Both paths are configured in every env file since 2026-10-19; before that, production
had no `SERVICE_CLOSED_FILE`, so `closed` could not have been honoured there.

---

## 8. What is deliberately not done, and what must happen before promotion

Not done, on instruction and on evidence:

1. **No promotion.** `current` still points at `20261015-211618`. The tree is three days newer and
   another agent is editing `server/src/**`; deploying it would ship review-in-progress code.
2. **No commit was created.** The slot's `server/` has never been committed, and committing is the
   owner's authorisation, not this task's.
3. **No key, no secret was printed or changed.** Only variable names and 12-character fingerprints
   were read.
4. **No Kotlin, Gradle or adb command was run.**

Before prod is promoted:

- [ ] the security work lands and the orchestrator commits it; **record the commit hash**;
- [ ] run `npm ci && npm test && npx tsc --noEmit` on that commit's snapshot (§2.1 of
      `release-operations.md`), not in the working tree — the rehearsal tree `6d82cd6…` already
      passed all three (278 tests), but it does not contain the security patch;
- [ ] `snapshot.sh <commit>` and re-run the whole §5 rehearsal from that snapshot;
- [ ] resolve or record the `S4` shared-`FAL_KEY` decision: either provision a second provider key
      for debug, or the owner records the decision and the promotion command carries
      `--allow-shared-provider-key`. Until then the pair audit fails, correctly;
- [ ] take a fresh `backup.sh prod` (the database is empty, so this is a formality, but the
      pre-migration backup is a step, not a hope);
- [ ] `release.sh prod --tarball … --promote` and read its health gate;
- [ ] verify over Caddy: `/plinth/health`, `/plinth/v1/config`, and one real generation — a 200 from
      `/health` alone is not that.

`S4` in one sentence: sharing `FAL_KEY` means a debug-run generation is billed to the same provider
account and the same quota as production. It cannot corrupt prod data (the databases, releases and
tokens are separate), but it is not full credential isolation, and it is recorded as an open
decision rather than quietly demoted to a warning.

---

## 9. Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| prod runs `20261015-211618`, debug runs `20261019-011255` | MEASURED | `readlink -f /opt/plinth-product-photos/current{,-debug}` on 2026-10-19 |
| prod schema is 3 with 0 rows in every table, debug schema 4 with 41 wallets / 97 jobs / 232 ledger rows | MEASURED | `better-sqlite3` read-only query in `releases/20261019-011255` |
| prod and debug both answer `/health` 200 with `databaseReachable:true` | MEASURED | `curl` against `https://pikupo.com/plinth{,-debug}/health` |
| the pair audit returns 28 PASS / 1 FAIL, the failure being `S4` shared `FAL_KEY` | MEASURED | `check-separation.sh --pair prod,debug`, output in §1 |
| database tokens, JWT, recovery pepper, billing secret and purchase KEK differ; `FAL_KEY` does not | MEASURED | value fingerprints, `check-separation.sh` `S3`/`S4` rows |
| prod env is `root:plinth 640`, debug `root:plinth-debug 640` | MEASURED | `stat -c '%U:%G %a'` |
| debug service runs as `plinth-debug` | MEASURED | `systemctl show -p User plinth-product-photos-debug.service` |
| staging accepted a release, rolled back both ways, auto-rolled back a broken one, backed up and restored, enforced `closed` with 503, survived restart, and was torn down | MEASURED | drill output in this session, §5 table |
| the rehearsal artifact is tree `6d82cd6…`, not a commit, because no commit contains `server/` | MEASURED | `git cat-file -t 6d82cd678…` = `tree`; `git log --all -- …/server` empty; `git ls-tree HEAD … ` empty |
| 20 files differ between that tree and the current working tree | MEASURED | `git diff --stat 6d82cd678… -- …/server` |
| the rehearsal snapshot passes its own type check and tests | MEASURED | `npx tsc --noEmit` exit 0; `npm test` → `Test Files 19 passed (19)`, `Tests 278 passed (278)` in the extracted tree |
| `release.sh` refuses to report success while the service is dead | MEASURED | induced broken release, step 6: health gate failed, auto-rollback, exit non-zero |
| `rollback.sh` is one command and restores health | MEASURED | steps 4–5 |
| `closed` refuses generation while `/health` stays 200 | MEASURED | step 11: HTTP 503 `KILL_SWITCH` |
| concurrent deploys are blocked; a rollback waits rather than interleaving | MEASURED | lock held in another process → `rollback.sh` blocked (timeout exit 124); `release.sh` uses `flock -n` |
| the **final** scripts (after the lock and S4 flag) deploy, roll back, back up, restore, close and reopen a staging instance | MEASURED | second staging rehearsal, output in §5 (`RELEASE: OK` ×2, `ROLLBACK: OK`, `BACKUP/RESTORE: OK`, `PROBE: PASS`, torn down) |
| one open-state probe returned `503 KILL_SWITCH` once, unexplained; the next three returned `UPLOAD_NOT_AVAILABLE` with no state files on disk | MEASURED | probe output in §5; `state.sh --check` showed `state ok`, no markers |
| a mistaken `state.sh closed` (default service = prod) briefly wrote prod's markers and `state.sh ok` removed them | MEASURED | prod journal 17:06–17:10 contains 0 `SERVICE_CLOSED`/`KILL_SWITCH`/refusal events; `ls` of the data dir shows neither file; `current` unchanged |
| a release is never built from the working tree | MEASURED | `release.sh` extracts the verified tarball and runs `npm ci`/`npm run build` inside `releases/<stamp>` |
| the deployment machinery is correct for the security patch | UNVERIFIED | the patch was still being written; no snapshot of it was tested. Re-run §5 on its commit before promotion |
| a backup exists off this machine | UNVERIFIED | no off-host copy was made; `backup.sh` writes to local `/var/backups` |
