# D — Server API contract

Base URL `https://<API_HOST>/v1` (the host exists only after task 2.1 / D9). JSON bodies unless stated.
Server: Node.js (current LTS at scaffold time) + TypeScript, Fastify, `better-sqlite3` (WAL), `sharp` for
image metadata and the fidelity guard, `undici` for provider calls; runs under systemd behind the
existing Caddy. `DECISION`. One worker process, bounded job concurrency `JOB_CONCURRENCY = 2` (the host has
1 vCPU and 954 MiB; ON RECORD from the session's earlier host read, re-measure at 2.1).

## Auth

`Authorization: Bearer <deviceToken>` on everything except API-00, API-01, API-12, API-13, API-14S and API-15. The token is an
HS256 JWT `{sub: deviceId, wid: walletId, gen: tokenGeneration, iat, exp}` signed with `JWT_SECRET`
(server environment only), lifetime 30 days; the client re-calls API-01 when it expires. Every request
compares `gen` with the device row, so deletion/revocation invalidates prior tokens immediately. A blocked
or deleted device receives 403 `DEVICE_BLOCKED`.

## Endpoints

| Id | Method + path | Request | Success | Notes |
|---|---|---|---|---|
| API-00 | `POST /integrity/challenge` | `{installId, appVersion}` | `200 {challengeId, challenge, expiresAt}` | single use, 5 minute TTL; stored hashed; rate limited like API-01 |
| API-01 | `POST /session` | `{installId, appVersion, locale, integrityToken, challengeId}` | `200 {deviceToken, walletId, balance, starterGranted, config}` | Standard token `requestHash = base64url(SHA-256(UTF8("session\\n" + challengeId + "\\n" + challenge + "\\n" + installId + "\\n" + appVersion)))`; verifies exact hash, package/version/certificate, `PLAY_RECOGNIZED`, `LICENSED` and `MEETS_DEVICE_INTEGRITY`; `installId` is correlation only; starter grant follows Device Recall below |
| API-02 | `GET /wallet` | — | `200 {balance, reserved, refundDebt, purchaseHold, entries:[last 50 ledger rows without refs]}` | |
| API-02P | `POST /purchases/intents` | `{productId}` | `201 {intentId, obfuscatedAccountId, obfuscatedProfileId, expiresAt}` | validates catalogue product; persists a 15-minute single-use intent bound to wallet/product; values are passed to BillingFlowParams |
| API-03 | `POST /purchases/confirm` | `{intentId, productId, purchaseToken}` | `200 {credited, balance, state}` | reads `ProductPurchaseV2`; verifies the live intent, package, exactly one expected line item/quantity, catalogue product, PURCHASED state, token and both obfuscated ids; runs the durable fulfillment saga below. Replay returns the terminal answer; `orderId` is response metadata, never identity |
| API-04 | `POST /wallet/recover` | `{code}` | `200 {deviceToken, walletId, balance}` | rebinds this device to the wallet whose `recovery_hash` matches; 5 attempts/hour/IP and 20/day/device |
| API-05 | `POST /wallet/recovery-code` | — | `200 {code}` | shown once after the first purchase and from Settings; 16 base32 chars (80 bits); server stores a hash; creating a new code invalidates the old |
| API-06 | `PUT /uploads` | raw bytes, `Content-Type: image/jpeg\|png\|webp`, `X-Client-Sha256` | `201 {uploadId, expiresAt}` | ≤ 4 MiB; client downscales to ≤ 2048 px longest side first; kept ≤ 1 h; server re-checks magic bytes |
| API-07 | `POST /jobs` | `{clientRequestId, op, uploadIds[], params, marketplaceId?, estimateCredits}` | `202 {jobId, reserved, state}` | reserves credits in one transaction before any provider call; `estimateCredits` must equal the server's cost else `409 PRICE_CHANGED` with the current cost; same `clientRequestId` returns the existing job |
| API-08 | `GET /jobs/{id}` | — | `200 {state, progress?, result?:{resultToken, expiresAt, width, height, bytes, sha256, provenance, guard?:{passed, metrics}}, charged, error?:{code,message}}` | opaque short-lived token, never a path; poll with `Retry-After` hint of 1–3 s |
| API-09 | `GET /jobs/{id}/result` | bearer device auth + `X-Result-Token` | `200` image bytes with `Content-Length`, `Digest` and `X-Delivery-Attempt` | verifies result/job/wallet ownership; creates/reuses a delivery attempt but never settles; token remains usable until receipt or `expiresAt` (≤ 24 h) |
| API-09R | `POST /jobs/{id}/delivery-receipts` | `{deliveryAttemptId, bytes, sha256}` | `200 {charged, state}` | after atomic local write, validates attempt/result/wallet and exact declared length/digest, then SETTLE idempotently; duplicate valid receipt returns the same answer |
| API-10 | `POST /jobs/{id}/cancel` | — | `200 {state, released}` | releases the reservation if the provider call has not been made; otherwise the job completes and the answer is returned |
| API-11 | `POST /reports` | `{jobId, category, note?}` | `201 {id,status:"OPEN"}` | `category ∈ OFFENSIVE, WRONG_PRODUCT, UNSAFE, RIGHTS, OTHER`; `note ≤ 500 chars`; stores op/provider/model/fingerprint for review; satisfies the Play in-app-report rule |
| API-12 | `GET /health` | — | `200 {ok, version, time}` | public; used by deployment check |
| API-13 | `GET /config` | — | `200 {ops:{<O-id>:{credits, enabled}}, starterCredits, killSwitch, minAppVersion, dailyCapReached}` | public and cacheable 60 s; the client never hard-codes a credit cost |
| API-14 | `DELETE /data` | recent bearer auth + explicit confirmation | `202 {receiptToken,status}` or terminal `200 {receiptToken,status:"COMPLETE",providerRetentionUntil?}` | revokes sessions, delivery tokens and recovery; deletes resources/reports/links; tombstones wallet; retained accounting/replay rows are de-identified and cannot restore balance; receipt token is random 256-bit and shown once; status never overclaims provider deletion |
| API-14S | `GET /data/deletions/{receiptToken}` | — | `200 {status,controlledSystemsComplete,providerRetentionUntil?,completedAt?}` | bearer-free because API-14 revoked it; stores only SHA-256 of the token, contains no identity/data, expires after 7 days, rate limited |
| API-15 | `POST /play/rtdn` | Google Cloud Pub/Sub push envelope | `204` | verifies the Pub/Sub OIDC JWT audience/issuer/signature/service-account email, deduplicates `message.messageId`, decodes only this package, then calls the Developer API for authoritative state; never trusts notification data alone |

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
                                                                    │              └─ GET result → verify+fsync+rename → valid API-09R receipt ──▶ SETTLE
                                                                    │              └─ expiresAt passes with no valid receipt                    ──▶ RELEASE
                                                                    └─ fail ──▶ retry once ──▶ fail ──▶ FAILED ──▶ RELEASE(+cost)
Any provider error / timeout (120 s) / cancel-before-call / restart-recovery ─────────────────────────▶ RELEASE
```

**DECISION (matches E55):** settle only after API-09R accepts the client's delivery receipt, not when an
HTTP write completes. The client writes to a temporary app-private file, verifies byte count and SHA-256,
fsyncs where supported, atomically renames, then durably queues the receipt. Missing, wrong or lost receipts
do not charge and may be retried idempotently. If `expiresAt` passes with no valid receipt, RELEASE. On server start, every `RUNNING` job older than 180 s is
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

## Protocol details

- API-00/API-01 use a single-use short-lived challenge and Play Integrity Standard `requestHash` content
  binding. The server decodes the token with Google and compares every expected field; empty/`UNEVALUATED`
  core verdicts fail closed for the starter grant. `installId` is correlation only. If S4 proves Device
  Recall available, bit 1 means "starter already granted": check false, grant transactionally, write true,
  and return success only after the write succeeds. Propagation races are contained by the database grant
  row and per-challenge serialization. Without supported Device Recall the grant is zero.
- API-02P derives `obfuscatedAccountId = base64url(HMAC-SHA256(BILLING_BINDING_SECRET, "wallet:" + walletId))`
  and `obfuscatedProfileId = base64url(HMAC-SHA256(BILLING_BINDING_SECRET, "intent:" + intentId))`; the
  client passes both to BillingFlowParams. API-03 requires the exact values returned by ProductPurchaseV2.
  Purchase processing is a durable saga: persist the raw token envelope-encrypted, verify Play, commit the
  credit/entitlement once, move to `PLAY_FINALIZING`, retry server-side consume/acknowledge to terminal, and
  only then return `CREDITED`/`ENTITLED` and erase ciphertext. A crash at every boundary resumes from state.
- API-15 handles RTDN when the app is absent. `ONE_TIME_PRODUCT_PURCHASED` triggers the same idempotent
  verification path; cancellation/void notifications and the nightly Voided Purchases sweep use the
  authoritative Developer API result. Duplicate Pub/Sub message ids and purchase tokens are no-ops. The
  sweep persists pagination state, overlaps the prior successful interval, and never assumes events older
  than Play's current API window are queryable. A full or quantity-based partial void applies one
  `REFUND_ADJUST`: available credits fall to zero first and the remainder becomes `refundDebt`; paid model
  jobs remain blocked until future purchased credits repay it. A voided unlock removes entitlement.
- API-06 creates a wallet-owned upload row; API-07 accepts only unexpired uploads owned by that wallet.
- API-08 returns a short-lived opaque `resultToken`, never a filesystem path. API-09 verifies
  result/job/wallet ownership and records a delivery attempt without charging. API-09R alone settles after
  the client confirms an atomic file write with exact declared length/digest. Disconnect, short write,
  wrong receipt or missing receipt does not settle; receipt retry is idempotent.
- API-14 requires recent re-authentication and confirmation, increments token generation, revokes sessions
  and recovery, deletes resources/reports/links, tombstones the wallet and de-identifies accounting rows.
  It returns a deletion receipt through API-14S until terminal. Provider-side
  deletion is requested when supported; otherwise the receipt and privacy text name the provider's measured
  retention window and mark only systems under our control complete—never claim third-party deletion.

Series use a parent reservation and child variants. Each fully delivered child creates one partial SETTLE;
each failed or expired child creates a partial RELEASE, unique by `(seriesId, variantId, movementKind)`.
At terminal state `reserved = settled children + released children`.

## Security notes

TLS only (Caddy); HSTS; request body size limit 5 MiB; JSON schema validation on every body; no
user-supplied URL is ever fetched (uploads are bytes); a raw purchase token is never logged and is stored
server-side only as envelope-encrypted saga ciphertext until Play consume/acknowledge reaches a terminal
state; its SHA-256 replay key remains after ciphertext deletion. The client keeps a pending raw token encrypted in
its private Room outbox until terminal confirmation, then erases it. Pub/Sub push auth is restricted to
  the configured service-account email and audience; `rtdn_message.message_id` is unique. Recovery
codes stored as SHA-256 with a server-side pepper; CORS disabled (native client). The service account that
reads Play purchases has only the Android Publisher scopes it needs and lives in the server environment.
