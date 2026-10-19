# Service states and the client contract

The server can be in one of four states. `closed`, `maintenance` and `ok` are the operator's decision;
`update_required` is a property of the calling client. This document is the contract the Android
client is wired against, and the runbook an operator follows. The implementation is `server/src/service-state.ts`, `server/src/guards.ts` and
`server/src/versions.ts`.

---

## 1. The four states

| State | Meaning | Who causes it | New paid work | Reads and refunds |
|---|---|---|---|---|
| `ok` | normal operation | nothing | allowed | allowed |
| `maintenance` | temporary pause; the service will come back | `KILL_SWITCH_FILE` exists | refused `503 SERVICE_MAINTENANCE` | allowed |
| `closed` | final; the service was stopped on purpose | `SERVICE_CLOSED_FILE` exists | refused `503 SERVICE_CLOSED` | allowed |
| `update_required` | this build is below `MIN_APP_VERSION` | the caller's signed `ver` claim | refused `426 UPDATE_REQUIRED` | allowed |

**Precedence is fixed** (`service-state.ts:72-89`): `closed` outranks `maintenance`, which outranks
`update_required`, which outranks `ok`. A closed service reports `closed` even to a current build; a
paused service does not tell anyone to update, because an update would not help. The order is a
server-side rule, so the client never has to resolve a combination.

`maintenance` and `closed` are **two files, not one flag with two meanings**:

```text
SERVICE_CLOSED_FILE present  -> closed        (wins over maintenance)
KILL_SWITCH_FILE    present  -> maintenance
neither                      -> ok
```

`deploy/state.sh closed` writes both files, because a build that predates `SERVICE_CLOSED_FILE` only
knows `KILL_SWITCH_FILE` and would otherwise keep spending. `state.sh maintenance` removes the closed
marker, so a reopened service cannot be stuck in a state nobody can see.

---

## 2. Where each state is visible

### `GET /config` (also `/v1/config`)

```json
{
  "ops": { "O-02": { "enabled": false, "...": "..." } },
  "starterCredits": 0,
  "killSwitch": true,
  "minAppVersion": "1.4.0",
  "dailyCapReached": false,
  "serviceState": "update_required",
  "notice": {
    "code": "UPDATE_REQUIRED",
    "messageByLocale": { "en-US": "...", "ru-RU": "..." },
    "dismissible": false
  }
}
```

- `serviceState` is resolved from the operator files plus the optional `X-App-Version` request header.
- `ops.*.enabled` is `false` whenever the state is not `ok` **or** the daily provider cap is reached,
  so a client that only reads `ops` still refuses to start a job.
- `killSwitch` is kept as a compatibility boolean and is `true` for every non-`ok` state, including
  `update_required`. A build that predates `serviceState` will therefore disable spending, which is
  the safe direction. New clients switch on `serviceState`.
- `notice.messageByLocale` carries copy for `en-US` and `ru-RU`. The client may use its own resources
  keyed on `notice.code`; the server copy exists so a build without those strings still has wording.
- The response is sent with `Cache-Control: public, max-age=60`, so **a state change can take up to
  60 seconds to be seen through `/config`**. The authoritative refusal is the per-request guard on the
  spend routes, which reads the files on every call.

### `GET /health`

```json
{ "ok": true, "databaseReachable": true, "serviceState": "maintenance", "time": 1792428343930 }
```

`/health` reports the **operator** state only (`readOperatorState`), never `update_required`: a probe
has no client version, and a probe must not be told to update. It answers `503` and
`databaseReachable: false` when the database cannot be read. It is never rate limited.

### Spend routes

These carry `minimumVersion` and `paidWork` in their pre-handler chain
(`routes.ts:270`, `:288`, `:317`, `:331`, `:375`, `:452`):

| Route | Refused when |
|---|---|
| `POST /v1/purchases/intents` | paused, closed, outdated |
| `POST /v1/purchases/confirm` | paused, closed, outdated |
| `POST /v1/wallet/recovery-code` | paused, closed, outdated |
| `PUT /v1/uploads` | paused, closed, outdated |
| `POST /v1/recognize` | paused, closed, outdated |
| `POST /v1/jobs` | paused, closed, outdated |

The version check runs **before** the state check, so an outdated client that is also hitting a paused
service receives `426 UPDATE_REQUIRED` — the client action it can take — rather than a pause message it
cannot act on. Both guards run after `authenticate`, so the refusal names a verified device.

### Routes that stay open in every state

| Route | Why it stays open |
|---|---|
| `GET /v1/wallet` | the user must see what is left |
| `GET /v1/jobs/:id` | status of work already paid for |
| `GET /v1/jobs/:id/result` | collect bytes already paid for, while the token is valid |
| `POST /v1/jobs/:id/delivery-receipts` | settle a delivery that already happened |
| `POST /v1/jobs/:id/cancel` | refund a queued job |
| `POST /v1/reports` and `GET /v1/reports/:id` | report a bad result |
| `DELETE /v1/data` | privacy deletion must never be blocked |
| `GET /v1/data/deletions/:token` | check a deletion |
| `POST /v1/wallet/recover` | redemption path (currently a `503 NOT_CONFIGURED` stub) |
| `GET /config`, `GET /health` | state discovery |

A paused or closed service **must not trap credits**. Nothing in the closed path deletes a wallet, a
result or a delivery token; retention removes payloads on their own expiry schedule
(`server/src/retention.ts:18-38`).

---

## 3. HTTP contract

### `426 UPDATE_REQUIRED`

```json
{
  "error": {
    "code": "UPDATE_REQUIRED",
    "message": "This build is below the minimum supported version. Update the app before spending credits.",
    "details": { "minAppVersion": "1.4.0" }
  }
}
```

- The comparison is numeric, part by part: `1.9.9 < 1.10.0`, `1.10.0 == 1.10.0` (`versions.ts:11-34`).
- A version that cannot be parsed is **below any enforceable minimum** (`versions.ts:49-55`), and so
  is a token with no `ver` claim at all. Failing closed is the only safe direction where money is
  involved.
- A minimum of `0`, `0.0.0`, empty or unparseable means "no gate" — the machine default. `MIN_APP_VERSION`
  defaults to `0` (`config.ts:113`), so a deployment that never sets it never refuses on version.

### `503 SERVICE_MAINTENANCE` / `503 SERVICE_CLOSED`

```json
{
  "error": {
    "code": "SERVICE_MAINTENANCE",
    "message": "Plinth is paused for maintenance: new jobs, purchases and recognition are not accepted. Nothing was charged.",
    "details": {}
  }
}
```

The two codes are distinct on purpose: a client treats "come back later" differently from "this
service is over". The queue re-checks the same files inside `enqueue` (`queue.ts:98-104`) and throws
the same `SERVICE_MAINTENANCE` code, so a pause that lands between the HTTP guard and the queue cannot
start a job. The queue returns an existing job unchanged for a replayed `clientRequestId`
(`queue.ts:91-99`), but the HTTP spend route still refuses that replay while paused because its
`paidWork` guard runs first; the client learns the outcome from `GET /v1/jobs/:id`, which is never
gated.

### `429 RATE_LIMITED`

Every limited route answers `429` with `Retry-After` in seconds and
`error.code = "RATE_LIMITED"`. Limits are per source address, per device **and per route**, so one
endpoint's polling cannot spend another's allowance; the table is `security-model.md` §3.3. A client
that sees `429` must wait `Retry-After` and must not clear its session.

---

## 4. The client contract

The client is not wired yet; this section is what the follow-up dispatch implements. The rules are
ordered by what goes wrong if they are ignored.

### 4.1 The `ver` claim is bound to the session, not to the installed APK

The version the server compares is inside the signed JWT, written at session creation from the
attested `appVersion` (`integrity.ts:70-80`, `:112-125`). Updating the app **does not change an
existing token**: `SessionBootstrap.connect` returns the stored session and never re-creates one while
it exists (`data/session/SessionBootstrap.kt:61`). A user who updates from `1.3.0` to `1.4.0` still
carries `ver: "1.3.0"` until the token expires (30 days), so the server keeps answering `426`.

Therefore, on `426 UPDATE_REQUIRED` the client **must**:

1. Stop the failing request; do not retry it with the same token.
2. `WalletSessionStore.clearSession()` — the token, wallet id and cached balance.
3. Run `SessionBootstrap.connect(appVersion = BuildConfig.VERSION_NAME, ...)` once. The new session
   carries the installed version, because the integrity request hash binds it.
4. Retry the original request once.
5. If it still answers `426`, the installed build really is below the server's minimum: show the
   blocking update screen and open the Play listing. Do not loop.

A client that only shows the update screen and opens Play will leave every updated user blocked until
their token expires, because the app has no other path that mints a session. This is the single most
important rule in this document.

### 4.2 Rendering matrix

| Signal | Screen | Blocking? | Actions |
|---|---|---|---|
| `serviceState: "ok"` | normal | no | everything |
| `serviceState: "maintenance"` or `503 SERVICE_MAINTENANCE` | banner/panel over the current screen: "Paused for maintenance. Nothing was charged." | no — reads and results still work | retry after 30 s with backoff; **do not** clear the session; keep cached balance visible |
| `serviceState: "closed"` or `503 SERVICE_CLOSED` | one read-only screen: "Plinth is closed. Existing results can still be collected." | yes for spending, **no** for collection | open results, open wallet, export/delete data; no retry loop |
| `serviceState: "update_required"` or `426 UPDATE_REQUIRED` | blocking update screen with the Play listing button | yes | the four steps in §4.1 |
| `dailyCapReached: true` | same panel as maintenance, different copy ("daily limit reached") | no | retry tomorrow, reads work |
| `429 RATE_LIMITED` | inline message on the action | no | wait `Retry-After`, do not clear the session |

Rules that follow from the table:

- **Never clear the session for maintenance, closed, a cap or a 429.** The token is the only key to
  paid results; clearing it on a pause loses access to work the user owns.
- **`update_required` is the only state that clears the session**, and only once, to re-attest.
- **A pause must not hide the wallet or the results.** The balance is fetched by
  `GET /v1/wallet`, which is not gated.
- **A closed service is not an error to retry.** Schedule nothing; the state does not change by
  itself.
- Copy comes from the client's own `values/strings.xml` keyed on `notice.code`; the server's
  `messageByLocale` is a fallback for a build without those keys.

### 4.3 State is advisory until a spend route refuses

`/config` is cached for 60 seconds and is a read. A client may pre-hide the "Generate" button from it,
but it must still handle a `503`/`426` from the spend route itself. A pre-emptive hide is a courtesy;
the guard is the enforcement.

### 4.4 Polling

`/config` must not be polled faster than once per 60 seconds. The spend routes carry the authoritative
answer, and a fast config poll is pure load. On returning from background, one refresh is enough.

---

## 5. Operator runbook

All commands run as root on the host. The scripts live outside the releases tree, at
`/opt/plinth-product-photos/deploy`; run them by absolute path, because the current directory is not
guaranteed after an SSH command.

```bash
# current state, no change
/opt/plinth-product-photos/deploy/state.sh --check

# temporary pause: generation, purchases, recognition and uploads are refused; reads keep working
/opt/plinth-product-photos/deploy/state.sh maintenance 30m

# reopen
/opt/plinth-product-photos/deploy/state.sh ok

# final closure: refuses new work for good, keeps /health answering and results collectable
/opt/plinth-product-photos/deploy/state.sh closed
```

`state.sh` writes the files, verifies `/health` and then polls `/v1/config` until it reports the state
it just wrote (`state.sh:108-157`, `:198-204`). It does **not** restart the service: state is read per
request, and reopening must not bounce a process that is serving downloads.

### Raising `MIN_APP_VERSION`

```bash
# in the service env file (e.g. /etc/plinth-product-photos/server.env)
MIN_APP_VERSION=1.4.0

# config.ts reads it at boot, so this one needs a restart
systemctl restart plinth-product-photos.service

curl -s http://127.0.0.1:3101/config -H 'X-App-Version: 1.3.0' | jq .serviceState
# "update_required"
```

Order of operations when shipping a release that requires the new build:

1. Publish the new build and let it reach devices.
2. Set `MIN_APP_VERSION` to the new `versionName` and restart.
3. Watch the `UPDATE_REQUIRED` rate in the logs (`guards.ts:80-81` logs the client version). A spike
   that does not decay means clients are not re-attesting; the client contract in §4.1 is not in the
   shipped build.

Do **not** raise the minimum above the version currently rolling out: that refuses the update itself.
The minimum can only ever name a build users can already install.

### What each state does to money

| State | Credits can be spent | Provider is called | Purchases can complete |
|---|---|---|---|
| `ok` | yes | yes | yes |
| `maintenance` | no | no | no |
| `closed` | no | no | no |
| `update_required` | no | no | no |

An in-flight job is not killed by a pause: the job already holds a reservation and a provider call may
already be paid for. Let it finish or fail normally; the queue releases the reservation on failure.
The daily provider cap (`DAILY_SPEND_CAP_USD`, default 25) is independent of the state and applies in
`ok`.

---

## 6. Verification

```bash
cd apps/2026-10-15-plinth-product-photos/server

# 26 cases: limiter, version arithmetic, four states, payloads, wallet isolation, idempotency
npx vitest run test/security-hardening.test.ts

# the state machine's unit-level truth
npx vitest run test/security-hardening.test.ts -t "service states"
```

The cases that pin this document:

- `reports maintenance, disables every operation and keeps reads open`
- `lets closed outrank maintenance and report it in /health`
- `answers update_required for a stated old build and ok for a current one`
- `refuses below the minimum and accepts a request exactly at the minimum`
- `refuses a new job from an outdated signed session and does not reserve credits`
- `refuses a new job while closed, with a distinct code`
- `names a pause SERVICE_MAINTENANCE on the job route, not the old KILL_SWITCH`
- `lets the queue refuse a pause with the same code the HTTP guard uses`
- `keeps job status and refunds available to an outdated build`
- `binds the attested version into the session token and reports the state in the session config`
- `fails /health when the database is unreachable`

A state change is verified on a real deployment by `./state.sh <state>` itself: it fails unless
`/v1/config` reports the state it wrote.
