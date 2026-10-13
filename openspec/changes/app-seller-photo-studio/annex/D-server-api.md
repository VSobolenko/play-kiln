# D — Server API contract

Base URL `https://<API_HOST>/v1` (the host exists only after task 2.1 / D9). JSON bodies unless stated.
Server: Node.js (current LTS at scaffold time) + TypeScript, Fastify, `better-sqlite3` (WAL), `sharp` for
image metadata and the fidelity guard, `undici` for provider calls; runs under systemd behind the
existing Caddy. `DECISION`. One worker process, bounded job concurrency `JOB_CONCURRENCY = 2` (the host has
1 vCPU and 954 MiB; ON RECORD from the session's earlier host read, re-measure at 2.1).

## Auth

`Authorization: Bearer <deviceToken>` on everything except API-01, API-12 and API-13. The token is an
HS256 JWT `{sub: deviceId, wid: walletId, iat, exp}` signed with `JWT_SECRET` (server environment only),
lifetime 30 days; the client re-calls API-01 when it expires. A blocked device receives 403 `DEVICE_BLOCKED`.

## Endpoints

| Id | Method + path | Request | Success | Notes |
|---|---|---|---|---|
| API-01 | `POST /session` | `{installId, appVersion, locale, integrityToken?}` | `200 {deviceToken, walletId, balance, starterGranted, config}` | creates device+wallet on first call; grants `STARTER_CREDITS` once per wallet; `installId` is 128-bit random generated on first run |
| API-02 | `GET /wallet` | — | `200 {balance, reserved, entries:[last 50 ledger rows without refs]}` | |
| API-03 | `POST /purchases/confirm` | `{productId, purchaseToken, orderId?}` | `200 {credited, balance, state}` | verifies with Google Play Developer API (`purchases.products.get`), credits once by `token_hash`, then consumes (packs) or acknowledges (unlock) through the API. Idempotent: a replay returns the first answer |
| API-04 | `POST /wallet/recover` | `{code}` | `200 {deviceToken, walletId, balance}` | rebinds this device to the wallet whose `recovery_hash` matches; 5 attempts/hour/IP and 20/day/device |
| API-05 | `POST /wallet/recovery-code` | — | `200 {code}` | shown once after the first purchase and from Settings; 16 base32 chars (80 bits); server stores a hash; creating a new code invalidates the old |
| API-06 | `PUT /uploads` | raw bytes, `Content-Type: image/jpeg\|png\|webp`, `X-Client-Sha256` | `201 {uploadId, expiresAt}` | ≤ 4 MiB; client downscales to ≤ 2048 px longest side first; kept ≤ 1 h; server re-checks magic bytes |
| API-07 | `POST /jobs` | `{clientRequestId, op, uploadIds[], params, marketplaceId?, estimateCredits}` | `202 {jobId, reserved, state}` | reserves credits in one transaction before any provider call; `estimateCredits` must equal the server's cost else `409 PRICE_CHANGED` with the current cost; same `clientRequestId` returns the existing job |
| API-08 | `GET /jobs/{id}` | — | `200 {state, progress?, result?:{downloadPath, expiresAt, width, height, provenance, guard?:{passed, metrics}}, charged, error?:{code,message}}` | poll with `Retry-After` hint of 1–3 s |
| API-09 | `GET /jobs/{id}/result` | — | `200` image bytes | authenticated; available until `expiresAt` (≤ 24 h) |
| API-10 | `POST /jobs/{id}/cancel` | — | `200 {state, released}` | releases the reservation if the provider call has not been made; otherwise the job completes and the answer is returned |
| API-11 | `POST /reports` | `{jobId?, category, note?}` | `201 {id}` | `category ∈ OFFENSIVE, WRONG_PRODUCT, UNSAFE, OTHER`; `note ≤ 500 chars`; satisfies the Play in-app-report rule |
| API-12 | `GET /health` | — | `200 {ok, version, time}` | public; used by deployment check |
| API-13 | `GET /config` | — | `200 {ops:{<O-id>:{credits, enabled}}, starterCredits, killSwitch, minAppVersion, dailyCapReached}` | public and cacheable 60 s; the client never hard-codes a credit cost |
| API-14 | `DELETE /data` | — | `204` | deletes uploads, results, reports and the device↔wallet link; the wallet balance stays recoverable only by recovery code; ledger kept de-identified |

## Error body

```json
{ "error": { "code": "INSUFFICIENT_CREDITS", "message": "…", "retryAfterSec": null, "details": {} } }
```

| Code | HTTP | Meaning | Client behaviour |
|---|---|---|---|
| `UNAUTHORIZED` | 401 | token missing/expired | re-session once, retry once |
| `DEVICE_BLOCKED` | 403 | device blocked | show support text; no retry |
| `INSUFFICIENT_CREDITS` | 402 | balance < cost | open the wallet sheet with the shortfall |
| `PRICE_CHANGED` | 409 | server cost ≠ estimate | show new cost, ask again |
| `PURCHASE_INVALID` | 400 | token unknown/forged | do not credit; show generic failure; no retry loop |
| `PURCHASE_PENDING` | 409 | Play reports pending | keep the item "Pending", poll on resume |
| `RATE_LIMITED` | 429 | too many requests | wait `retryAfterSec` |
| `CAP_REACHED` | 503 | daily spend cap reached | tell the user, offer Original mode, retry tomorrow |
| `KILL_SWITCH` | 503 | operator disabled paid ops | same as CAP_REACHED |
| `TOO_LARGE` | 413 | upload > 4 MiB | downscale and retry once |
| `UNSUPPORTED_MEDIA` | 415 | not jpeg/png/webp | convert and retry once |
| `VERSION_TOO_OLD` | 426 | `appVersion < minAppVersion` | block paid ops, link to the store |
| `PROVIDER_FAILED` | job.error | provider error or timeout | job FAILED, reservation released |
| `GUARD_REJECTED` | job.error | fidelity guard rejected twice | job FAILED, released |
| `MODERATION_BLOCKED` | job.error | provider content filter | job FAILED, released, message explains |

## Job lifecycle (the ledger contract)

```text
POST /jobs ──▶ RESERVE(−cost) ──▶ QUEUED ──▶ RUNNING ──▶ [guard] ──┬─ pass ──▶ SUCCEEDED
                                                                    │              │
                                                                    │              └─ GET /jobs/{id}/result (first byte OK) ──▶ SETTLE
                                                                    │              └─ expiresAt passes with 0 downloads     ──▶ RELEASE
                                                                    └─ fail ──▶ retry once ──▶ fail ──▶ FAILED ──▶ RELEASE(+cost)
Any provider error / timeout (120 s) / cancel-before-call / restart-recovery ─────────────────────────▶ RELEASE
```

**DECISION (matches E55):** settle at the first successful download of the result, not at `SUCCEEDED`.
`SUCCEEDED` means the bytes are ready; the seller is charged only when the client has them. If
`expiresAt` passes with zero downloads, RELEASE. On server start, every `RUNNING` job older than 180 s is
moved to `FAILED(PROVIDER_FAILED)` with RELEASE. `SETTLE` and `RELEASE` are idempotent by
`(wallet_id, kind, idempotency_key = jobId)`.

## Limits and protection

Per-device 30 requests/min; per-IP 120/min; concurrent open jobs per device 4; `STARTER_CREDITS = 10`;
global `DAILY_SPEND_CAP_USD = 25` and `STARTER_DAILY_BUDGET_USD = 10` (environment, owner-changeable); a
kill switch file `KILL_SWITCH_FILE` whose existence sets `killSwitch = true`. Logs hold request ids,
status codes, job ids and costs; never image bytes, prompts containing user text, tokens or keys.

## Provider seam (`server/providers`)

`interface Provider { run(op, inputs, params): Promise<ProviderResult> }`; `FalProvider` and
`FakeProvider` (tests; deterministic outputs and injectable failure/latency). The model registry
(`server/config/models.json`, annex E §Registry) maps `op → {endpointId, tier, maxCostUsd, timeoutMs}`; an op with
`endpointId: null` is refused with `enabled: false` in API-13. The fal key is read from `FAL_KEY` in the
server environment only.

## Security notes

TLS only (Caddy); HSTS; request body size limit 5 MiB; JSON schema validation on every body; no
user-supplied URL is ever fetched (uploads are bytes); purchase tokens stored as SHA-256 hashes; recovery
codes stored as SHA-256 with a server-side pepper; CORS disabled (native client). The service account that
reads Play purchases has only the Android Publisher scopes it needs and lives in the server environment.
