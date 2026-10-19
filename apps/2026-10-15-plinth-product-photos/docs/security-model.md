# Security model

This describes what the Plinth server protects, where its trust boundaries are, which controls exist
today, and what is still undefended. It is written against the code in `server/src/` and the deploy
scripts in `deploy/`; a control is listed only if it is implemented and executed by a test or a
script. The residual-risk section is deliberately blunt.

The product: an Android app that sells AI product-photo edits. Credits live on the server and are
spent on jobs that call a paid image provider. The provider credential never leaves the server.

---

## 1. Assets, in order of loss

| Asset | What losing it means |
|---|---|
| Provider spend (`FAL_KEY` balance) | Real money leaves with no matching revenue. The primary asset. |
| Wallet balances and the credit ledger | Users lose paid credits, or an attacker mints them. Both are money. |
| Purchase records and Play purchase tokens | A forged or replayed purchase grants credits; a leaked token is a usable receipt. |
| `JWT_SECRET`, `BILLING_BINDING_SECRET`, `PURCHASE_TOKEN_KEK`, `RECOVERY_PEPPER`, Play service account | Full impersonation, entitlement forgery, or decryption of stored purchase tokens. |
| User images and results | Private product photos; deletion and retention promises are part of the store listing. |
| Database and backups | All of the above in one file. A backup on the same host does not survive the host losing it. |
| Availability | A paused shop earns nothing; a closed shop loses the installed base. |

Non-assets: the APK is public and inspectable. Nothing secret may be inside it.

---

## 2. Trust boundaries

```
Android app (untrusted)
   │  HTTPS, bearer JWT (30 days, device-scoped)
   ▼
Caddy  (TLS termination, /plinth/* path route)
   │  loopback 127.0.0.1:3101, X-Forwarded-For from the trusted hop only
   ▼
Fastify  ──────────────► SQLite (WAL)          ── server-local, single writer
   │                         └── backups under /opt/plinth-product-photos/backups
   ├──────────────► Google Play (integrity, purchases, RTDN)   ── service account
   └──────────────► fal.ai image provider                       ── FAL_KEY
```

Everything the app sends is untrusted: headers, path ids, JSON bodies, image bytes, the `appVersion`
string. The only claims the server trusts are the ones it signed itself, or Google signed and the
server verified.

---

## 3. Controls that exist

### 3.1 Identity and session

- A device session is created only after a Play Integrity challenge/response
  (`server/src/integrity.ts:104`). The challenge is single-use (`:119-120`), expires, and the request
  hash covers `challengeId`, challenge, installId, **app version** and locale (`:70-80`). Replaying a
  challenge fails; changing the claimed app version after the challenge fails (`:121-125`).
- The JWT carries `sub` (device), `wid` (wallet), `gen` (token generation) and `ver` (attested app
  version). `authenticate` re-reads the device and wallet on **every** request and rejects a deleted
  device, a deleted wallet, a stale generation or a blocked device
  (`server/src/auth.ts:15-50`). A token is therefore revocable server-side without waiting for expiry.
- The JWT is signed with `JWT_SECRET`, which `loadConfig` refuses to start without and requires to be
  at least 32 characters (`server/src/config.ts:3-8`, `:103`).
- `PLINTH_DEV_INTEGRITY=1` is refused when `NODE_ENV=production` (`config.ts:74-76`), and
  `check-separation.sh` S5 fails a production env that carries it at all.

### 3.2 Credit conservation

The ledger is the money. Its invariants are enforced in one class (`server/src/ledger.ts`), and every
mutation runs in `db.transaction(...).immediate()` so two callers cannot interleave a read and a
write:

| Invariant | Where |
|---|---|
| A reservation exists before a job can run; the job row must belong to the wallet | `ledger.ts:19-50` |
| One `RESERVE` per job (`idempotency_key = jobId`), and a conflicting amount is an error | `ledger.ts:21-27` |
| A refund is one `RELEASE` per job, and a settled job cannot be released | `ledger.ts:141-164` |
| A charge is one `SETTLE` per job, and a released job cannot be settled | `ledger.ts:126-139` |
| A wallet with `refund_debt` or `purchase_hold` cannot reserve (chargeback protection) | `ledger.ts:39-41` |
| A delivery receipt is bound to one attempt, one wallet, one result and exact bytes/sha256 | `ledger.ts:52-110` |
| A failed job releases its reservation; a crash-recovered job does too | `queue.ts:169-177` |

A job is enqueued with reservations disabled only after the queue's `enqueue` checks for an existing
`client_request_id` and returns the existing job instead of reserving twice (`queue.ts:91-99`). The
test `test/security-hardening.test.ts` ("returns the existing job for a replayed clientRequestId")
pins it, and "releases a reservation once" pins the double-refund case.

Provider spend has one fiat ceiling per UTC day: `spend_day.cost_micro_usd` is compared to
`DAILY_SPEND_CAP_USD` and incremented in the same `.immediate()` transaction before a job is
accepted (`queue.ts:271-283`). The check in `/v1/jobs` (`routes.ts:479-481`) is an early refusal, not
the enforcement point; the queue transaction is what two concurrent requests cannot both pass. The
wallet's credit balance is the per-wallet ceiling — a reservation is required before a job can run —
and there is no separate per-wallet fiat/day limit; that gap is listed in §5.

### 3.3 Rate limiting

One in-memory token bucket per Fastify process (`server/src/rate-limit.ts`). A token bucket is used
rather than a fixed window so a caller cannot spend two windows' allowance back-to-back.

Two keys, because they defeat different attacks, and both name the route:

- **Network key `ip:<address-or-/64>:<route>`** bounds an unauthenticated flood. Behind Caddy the
  address is the real client only because `TRUST_PROXY` defaults to `loopback` (`config.ts:27-34`,
  `app.ts:38`); a wider setting lets a caller forge `X-Forwarded-For` and escape its bucket. An IPv6
  address is counted as its **/64** (`rate-limit.ts:354-376`), because an ISP delegates a whole
  prefix and keying the full address would hand one caller 2^64 buckets; an IPv4-mapped address
  (`::ffff:a.b.c.d`) shares its IPv4 bucket for the same reason.
- **Device key `dev:<route>:<sub>`** is the signed JWT subject. A caller cannot choose it, and it
  survives source-address rotation. It is applied after `authenticate`, so an unauthenticated caller
  never reaches it (`rate-limit.ts:400-418`).
- **The route is part of both keys.** One shared bucket made a wallet poll spend the allowance a
  recovery code or a purchase confirmation needed, so an innocent client saw `429` on a different
  endpoint. Each route now has its own budget; the integration tests named below pin both keys.

The bucket map is capped at `MAX_BUCKETS = 20_000` and evicts oldest-first
(`rate-limit.ts:67`, `:125-140`), so a flood from many addresses costs bounded memory. Eviction can
drop an honest caller's bucket under churn; that trade is listed in §5 rather than presented as a
perfect defence.

| Route class | Network bucket | Device bucket |
|---|---|---|
| `GET /config` | 120/min | — |
| `POST /v1/integrity/challenge` | 12 per 5 min | — |
| `POST /v1/session` | 12 per 5 min | — |
| `POST /v1/play/rtdn` | 600/min | — |
| `GET /v1/data/deletions/:token` | 30/min | — |
| `GET /v1/wallet` | 600/min flood guard | 60/min |
| `POST /v1/jobs` | 600/min flood guard | 20/min |
| `POST /v1/recognize` | 600/min | 30/min |
| `PUT /v1/uploads` | 600/min | 30/min |
| `POST /v1/purchases/intents` and `/confirm` | 600/min | 10 per 10 min |
| `POST /v1/wallet/recovery-code` | 600/min | 3/hour |
| `DELETE /v1/data` | 600/min | 3/hour |
| Job read / result / receipt / cancel / reports | 600/min | 30–120/min |

Values are in `rate-limit.ts:148-173`. A refusal is `429`, code `RATE_LIMITED`, with `Retry-After` in
seconds (`:339-343`). `/health` and `/healthz` are deliberately unlimited so a probe is never starved.

### 3.4 Payload limits

| Boundary | Limit | Where |
|---|---|---|
| Whole request body | 5 MiB | `app.ts:32` |
| JSON body (schema validation) | every field capped in `schema/api.schema.json` | `app.ts:68-73` |
| Image upload bytes | 4 MiB, non-empty | `routes.ts:339` |
| Image dimensions decoded | 40 megapixels (`sharp.limitInputPixels`) | `routes.ts:349`, `:411`, `:540` |
| Upload digest | `X-Client-SHA256` must equal the bytes | `routes.ts:343-347` |
| Operation `params` object | 8 KiB after serialisation | `routes.ts:493-495` |
| Prompt/instruction | 2 000 characters | `routes.ts:496-500` |
| Upload ids | exact count per operation, all owned by the wallet | `routes.ts:503-514` |
| Result bytes | 25 MiB, side 1 024–4 096 | `results.ts:3-5` |

The 8 KiB parameter ceiling and the prompt ceiling exist because a prompt is client-supplied text
forwarded to a **billed** provider call; an unbounded one is a direct cost lever.

### 3.5 Wallet isolation

Every wallet-touching query is scoped by the JWT `wid`, never by a client-supplied wallet id:

- job status: `WHERE id = ? AND wallet_id = ?` (`routes.ts:592-593`),
- result metadata: `WHERE r.job_id = ? AND r.wallet_id = ?` (`routes.ts:603-606`),
- result bytes: join on `delivery_token.wallet_id = result.wallet_id` and `r.wallet_id = ?`
  (`routes.ts:653-663`),
- uploads: `WHERE wallet_id = ? AND deleted_at IS NULL AND expires_at > ? AND id IN (...)`
  (`routes.ts:510-512`),
- deletion, receipts, ledgers and purchases: all scoped by `claims.wid`.

The test "does not let one wallet read another wallet job or result" drives a second wallet with a
stolen token and asserts `404 JOB_NOT_FOUND` / `404 RESULT_NOT_AVAILABLE` and no charge.

### 3.6 Injection, traversal and filesystem

- Every SQL statement in `server/src/` is a prepared statement with bound parameters. The only dynamic
  SQL is the `IN (${placeholders})` list in `routes.ts:510`, where the placeholders are generated from
  the array length and the values are bound; no identifier or value is concatenated.
- Result and upload paths come from the server's own `DATA_DIR` and a random id; the delivery route
  selects the path by job id and wallet id, not by a client path. `DELETE /v1/data` removes the paths
  the database holds for that wallet only.
- `docker`/shell execution is not used by the API. The deploy scripts read the env file without
  sourcing it (`deploy/lib.sh:104-108`), so secrets do not enter the shell environment or `/proc`.

### 3.7 Secrets and logs

- `FAL_KEY`, `RECOVERY_PEPPER`, `BILLING_BINDING_SECRET` and `PURCHASE_TOKEN_KEK` are server-side only.
  The client-secrets gate for the APK is `scripts/check-client-secrets.mjs`; this server never sends a
  credential to the app.
- Pino redacts `req.url`, `authorization`, `x-result-token`, `integrityToken`, `purchaseToken` and the
  report note (`app.ts:40-43`). No route returns a secret; error bodies carry a code, a fixed message
  and non-secret details.
- The recovery code is 16 base32 characters (80 bits) from `randomBytes(10)`, stored as an HMAC-SHA256
  with `RECOVERY_PEPPER` (`routes.ts:92-111`). The code is returned once and is not logged.
- Purchase tokens are encrypted at rest with a per-row data key wrapped by `PURCHASE_TOKEN_KEK`
  (AES-256-GCM, `purchases.ts:274-287`), and cleared when finalization completes.

### 3.8 Transport, headers and browser surface

`app.ts:52-60` sets on every response: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`,
`Referrer-Policy: no-referrer`, `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`,
`Cross-Origin-Resource-Policy: same-origin`, `Permissions-Policy` denying geolocation/microphone/camera,
and `Strict-Transport-Security`. No CORS plugin is registered and no `Access-Control-Allow-Origin` is
emitted: there is no browser origin that needs access, and no cookie is used, so CSRF has no surface.

### 3.9 Error shapes

Validation errors become `400 INVALID_REQUEST`, oversize bodies `413 TOO_LARGE`, other 4xx a generic
`INVALID_REQUEST`, and every 5xx a generic `INTERNAL` with no stack, SQL or path (`app.ts:68-90`).
Provider and Play failures are logged by code, not by response body.

### 3.10 Service states and the version gate

- `server/src/service-state.ts` resolves four states with a fixed precedence: `closed` (file), then
  `maintenance` (file), then `update_required` (client below minimum), then `ok` (`:72-89`).
- New paid work is refused in `closed` and `maintenance` by a pre-handler that reads the files **per
  request** (`guards.ts:49-67`), and again at enqueue (`queue.ts:98-104`), so generation cannot start
  even if a client never fetches config.
- The minimum version is checked on every mutating route from the signed `ver` claim
  (`guards.ts:78-90`); a missing or unparseable version is below any enforceable minimum
  (`versions.ts:49-55`). The refusal is `426 UPDATE_REQUIRED`, not a generic 400.
- Reads, result downloads, refunds, delivery receipts, reports and account deletion stay open in every
  state, so a user can always collect what was paid for and always delete their data.

### 3.11 Retention and deletion

`server/src/retention.ts` deletes upload and result **payloads** when they expire, and deletes delivery
tokens and result metadata with them. It never deletes `ledger`, `wallet`, `job`, `purchase`,
`delivery_receipt` or `delivery_attempt` rows (`:18-38`): money records outlive images. A result that
expired without a receipt and without a charge releases its reservation before the row is removed
(`:22-30`). Account deletion replaces the wallet with an empty surrogate, moves the ledger to it,
scrubs purchase ciphertext and removes device links (`routes.ts:787-828`).

### 3.12 Backup

`server/src/backup.ts` uses SQLite's online backup API, seals the snapshot into rollback-journal mode
(one file, `:77-78`), verifies `integrity_check` and table counts on a separate read-only connection
(`:40-55`), and renames `<destination>.partial` only after verification (`:101-106`). `deploy/backup.sh`
implements the same sequence for the host (`deploy/backup.sh:82-86`) and deletes a copy that fails
verification. The seal matters even though the copy already holds every page: `db.backup` leaves the
copy in **WAL** mode (measured: `copy-journal wal rows 1`), so a later write would create a sidecar
that `restore.sh` never installs. A live WAL source copied blindly is worse — the measured drill read
`no such table: wallet` from the copy. The seal is what makes the file self-contained.

---

## 4. Audit findings

Each row is a line of code or a request that was observed, not a checklist item.

| # | Finding | Evidence | Status |
|---|---|---|---|
| 1 | No request limiting was installed on any route; session creation, purchases and recovery-code generation could be looped. | Prior state observed while auditing: `createApp` registered no limiter. Now `installNetworkLimiter`/`deviceLimit` are wired in `app.ts:62-63` and `rate-limit.ts`, and every bucket is driven by `test/security-hardening.test.ts`. | Fixed |
| 2 | `MIN_APP_VERSION` was defined in configuration but read by no route, so an outdated build could spend. | `server/src/config.ts:113` defines it; prior to this work no route referenced it. Now `requireMinimumVersion` is in `server/src/guards.ts:75` and on six spend-route pre-handler lists (`routes.ts:270`, `:288`, `:317`, `:331`, `:375`, `:452`). | Fixed |
| 3 | `KILL_SWITCH` was one boolean rendered as a generic failure; "closed" and "maintenance" were indistinguishable. | `server/src/queue.ts` threw one `KILL_SWITCH`; `/config` had one boolean. Now `service-state.ts` maps two files to four states and `/config` carries `serviceState` + `notice`. | Fixed |
| 4 | A crash-recovered job younger than three minutes kept its reservation forever. | Old `recoverInterruptedJobs` filtered by age; now every `QUEUED`/`RUNNING` job is released at boot (`queue.ts:169-177`). | Fixed |
| 5 | A polling client minted a new delivery token per status call; the table grew with poll rate. | `routes.ts:618-628` now keeps at most three live tokens per result; test "keeps at most three live delivery tokens per result". | Fixed |
| 6 | Operation `params` and `prompt` had no size ceiling, so a client could forward an unbounded billed prompt. | `routes.ts:493-500`; tests "refuses parameters past 8 KiB" and "refuses a prompt longer than 2000 characters". | Fixed |
| 7 | `/health` answered 200 while the database was unreachable, so a probe could not see a dead database. | `routes.ts:151` performs `SELECT count(*) FROM sqlite_master`; test "fails /health when the database is unreachable" asserts 503. | Fixed |
| 8 | A backup left in WAL mode can grow a `-wal` sidecar that `restore.sh` never installs; a blind `cp` of a live WAL database is unreadable. | Measured: `db.backup` produced a copy whose `journal_mode` is still `wal` (`copy-journal wal rows 1`), while the drill's blind live copy answered `no such table: wallet`. `deploy/backup.sh:82-86` checkpoints and switches the copy to `DELETE`, matching `server/src/backup.ts:77-78`; the seal was confirmed by the restore drill (`journalModeAfterSeal: delete`, `matchesLive: true`). | Fixed |
| 9 | `state.sh` wrote both "closed" and "maintenance" into one file, while the server now reads two; `closed` would have been reported as `maintenance`. | `deploy/state.sh` now writes `SERVICE_CLOSED_FILE` or `KILL_SWITCH_FILE`, removes the other, and verifies `serviceState` rather than `killSwitch`. `check-separation.sh` S12 checks both paths. | Fixed |
| 10 | Logs could carry a purchase token, an integrity token or a result token in a URL or body. | Pino `redact` list in `app.ts:40-43`. | Fixed |
| 11 | Every wallet-scoped read was already scoped by `wid`; this was verified rather than assumed. | `routes.ts:510-512`, `:592-593`, `:603-606`, `:653-663`; cross-wallet test. | Verified |
| 12 | SQL was already parameterised, including the dynamic `IN (...)`. | `routes.ts:510`; review of `server/src/*.ts`. | Verified |
| 13 | Limiter keys did not name the route, so a wallet poll could spend the recovery-code or purchase allowance; an IPv6 caller had 2^64 buckets; the bucket map grew without bound. | `rate-limit.ts:354-376` normalises IPv6 to /64, `:67` + `:125-140` cap the map, and both keys carry the route name (`:379-418`). Tests "keeps one network bucket per route", "keeps device buckets per route", "counts one IPv6 /64" and "caps the bucket map" fail on the previous keys. | Fixed |
| 14 | A delivery receipt was looked up by attempt and wallet only, so a receipt for one delivered job could be replayed against a different, undelivered job in the same wallet and be reported `SETTLED`. | `ledger.ts:60-74` joins `result` and requires `r.job_id = jobId` before returning a stored receipt; test "does not accept one job delivery receipt as proof for another job" fails without the join. | Fixed |

---

## 5. Residual risk

This is what is still not defended. Each item names the cost of defending it, because a risk with no
price is a risk nobody can decide about.

1. **The rate limiter is per process and in memory.** A restart clears every bucket, and a second
   replica would keep its own counters. One process per host is the current deployment, so today the
   only window is a restart. Defending it costs a shared store (Redis or a small SQLite counter table)
   plus the latency of a network round-trip on every request. Not implemented.
2. **Bucket eviction can be weaponised.** The map is now capped at 20 000 entries
   (`rate-limit.ts:67`) and evicts oldest-first, so memory is bounded, but an attacker who rotates
   source addresses inside a window can churn the map and evict an honest caller's bucket, granting
   that caller a fresh allowance. A strict cap with refusal instead of eviction would turn the same
   flood into a denial of service for every new address. Neither behaviour is free; the current
   choice bounds memory and accepts possible allowance churn. A shared store with per-key TTLs is the
   real fix and is not implemented.
3. **Public buckets are keyed by IP, and mobile users share carrier-grade NAT addresses.** Tight
   unauthenticated limits (12 per 5 minutes for challenge/session) protect the integrity quota but can
   refuse a legitimate launch when many users behind one address start at once. IPv6 is counted per
   /64 (`rate-limit.ts:354-376`), which closes address rotation inside one delegated prefix but also
   groups every user of that prefix. The honest mitigation is a second key derived from the request
   body (installId) with a generous IP ceiling; that lets an attacker rotate installId, so it trades
   one abuse path for another. This is why the current values are a judgement, not a proof.
4. **Starter credits are a farming surface.** A new wallet can receive a starter grant after integrity
   and Device Recall checks (`integrity.ts`), bounded by `STARTER_DAILY_BUDGET_USD` and
   `STARTER_CREDITS`. A determined operator with many Play accounts and devices can harvest up to the
   daily budget. Defending it further means payment-method binding or manual review, which is a
   product and privacy decision, not a code fix.
5. **The device bucket is per wallet, not per human.** Several wallets on one device each get their
   own allowance. There is no account-level identity to key on, by design (no login). Adding one costs
   a registration flow and a privacy surface.
6. **Purchase verification trusts Google's API responses.** A compromised Play service account, or a
   mis-scoped key, can mark purchases as valid. The defence is key management and `check-separation.sh`
   S3/S4, not application logic. The app is not allowed to grant credits locally.
7. **No WAF, bot management or volumetric DDoS protection.** Caddy terminates TLS and can absorb some
   abuse, but a large flood reaches the process. A provider-side shield is a recurring cost; it is not
   part of this repository.
8. **`TRUST_PROXY` is an operator setting.** If it is widened beyond the actual proxy, `X-Forwarded-For`
   becomes caller-controlled and the network limiter is bypassable. `check-separation.sh` measures the
   loopback listener but cannot prove the proxy is the only hop.
9. **Logs can still contain user content in provider error strings.** Codes and request ids are logged,
   not bodies, but a provider SDK error that embeds a prompt or an image URL would be written. Nothing
   sanitises third-party error text beyond the fields Pino redacts.
10. **`/health` is public and reports operator state.** It reveals whether the service is paused or
    closed. That is intentional for probes; if state disclosure is unwanted, the probe can move to a
    loopback-only route and Caddy can stop routing `/health`.
11. **The APK can be repackaged.** Play Integrity is the defence and the reason session creation needs
    it. A rooted device with a valid Play account can still pass some verdicts; the server treats the
    verdict as evidence, not as proof of a human.
12. **A live restore is a point-in-time rollback.** Credits and purchases made after the backup are
    lost, and Play reconciliation is required before balances are called authoritative. `restore.sh`
    refuses to overwrite a serving database and verifies what landed, but it cannot merge two histories.
13. **Backups stay on the same host by default.** A host loss loses the backup too. Copying them
    off-host is an owner-controlled operational step; no script does it on a schedule.
14. **No tamper-evident audit log.** Ledger rows are durable and idempotent, but there is no append-only
   hash chain and no external log sink, so an operator with database write access could rewrite
   history. Defending it costs an external append-only store.
15. **A delivered result can escape its charge if the process dies at the wrong instant.** The charge is
   recorded in `onCompletedSend` after the response finishes (`routes.ts:675-681`). If every byte
   reached the client and the process died before that callback ran, retention later sees a succeeded
   result with no receipt and no charge and releases the reservation, so the user keeps the bytes and
   the credits. The window is one callback wide, and charging at request start would charge failed or
   partial downloads. The robust fix is a durable delivery record written by the streaming layer (or a
   proxy) before the last byte, which is not implemented.
16. **There is no per-wallet fiat spend ceiling per day.** A wallet that legitimately buys a large
   credit balance can spend up to the global `DAILY_SPEND_CAP_USD`; the balance prevents overdraft but
   does not cap one wallet's provider cost below the global limit. Adding one needs a
   `wallet_spend_day` table (or an equivalent atomic counter keyed by wallet and UTC day), a
   migration, and a refusal code the client can render. The global cap, the 20 jobs/min/device limit
   and Play Integrity currently bound the exposure; the missing per-wallet ceiling is a deliberate
   deferral, not a claim that the global cap is sufficient.

---

## 6. How to check the controls

```bash
cd apps/2026-10-15-plinth-product-photos/server
npx vitest run test/security-hardening.test.ts   # 26 cases: limiter, versions, states, payloads, wallets
npx vitest run test/backup.test.ts               # backup, verify, single-file restore
npm run drill:restore                            # blind copy fails; sealed snapshot reads back
npx tsc --noEmit
npm test                                         # the whole suite
node ../../scripts/check-client-secrets.mjs --slot 2026-10-15-plinth-product-photos
```

The security tests are behavioural: they drive HTTP, the queue or a database and assert a status, a
code, a row count or a balance. A test that only asserted a guard "exists" would not be evidence.
