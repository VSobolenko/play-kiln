# Generation-capability inventory — 2026-10-16

**Scope.** A read-only inventory of what the factory's own AI-generation capability actually is today, so a
planned family of Android apps extends a real capability rather than an imagined one. The one existing
AI-generation slot is `apps/2026-10-15-plinth-product-photos/` (app + `server/`); its backend is the only
generation surface the factory owns.

**Method.** Every claim below is grounded per `.cursor/rules/grounded-claims.mdc`. All source was read, not run;
no Gradle command was executed (another process owns this slot's daemon). The only command run was
`node scripts/check-client-secrets.mjs --slot plinth-product-photos`. The claim ledger is at the end.

**What was read.** `server/src/{registry,routes,queue,ledger,app,auth,purchases,retention,integrity,moderation,google-play,config,db,provider-concurrency}.ts`,
`server/migrations/001..003*.sql`, `server/schema/api.schema.json`, `server/README.md`, and the server test
suites (`registry`, `queue`, `guard`, `ledger`, `recognition`, `error-handler`, `purchases`); the app's
`differentiators.json`, `capabilities.json`, `app/build.gradle.kts`, `app/src/main/java/**/data/api/*`,
`data/wallet/*`, `data/outbox/*`, and `ui/**`; `capabilities/*/build.gradle.kts` and sources;
`openspec/changes/app-seller-photo-studio/**`.

---

## Part 1 — The model surface

### 1a. The operation registry

All eleven operations live in one literal, `MODEL_REGISTRY` in
`apps/2026-10-15-plinth-product-photos/server/src/registry.ts:39-85`. Every row's `endpointId` is a fal.ai
model id; the adapter POSTs to `https://queue.fal.run/<endpointId>` (`registry.ts:138`, `:163`).

| op | fal endpoint | credits | real `costMicroUsd` (≈USD) | unit / reference | enabled | what it does | I/O shape |
|---|---|---|---:|---|---|---|---|
| **O-01** `recognize` | `fal-ai/any-llm/vision` | 0 | `10_000` ($0.010) | requests / 1 request | **yes** | Vision LLM names the item kind, category, attributes and a scale hint | **JPEG in → JSON out.** One raw JPEG (client requires JPEG magic bytes, `ApiClient.kt:76-81`); strict JSON-schema response |
| **O-02** `cutout` | `fal-ai/bria/background/remove` | 1 | `18_000` ($0.018) | generations / 1 generation | **yes** | Remove background (cut-out) | **image in → image out.** Prompt-free (`registry.ts:159` rejects a `prompt`); exactly one upload (`routes.ts:422`) |
| **O-07** background scene | `fal-ai/flux/dev` | 2 | `50_000` ($0.050) | megapixels / 2 MP | **yes** | Text-to-image **background** generation (`// $0.025/MP at the 2 MP reference output`, `registry.ts:49`) | **image + prompt params in → image out** |
| **O-08** relight | `fal-ai/iclight-v2` | 5 | `104_858` ($0.1048) | megapixels / 1024×1024 | **yes** | Relight the item (`// $0.10/MP at 1024x1024 (1.048576 MP)`, `registry.ts:54`) | image in → image out |
| **O-09** expand canvas | `fal-ai/bria/expand` | 2 | `40_000` ($0.040) | generations / 1 generation | **yes** | Outpaint / expand the frame | image in → image out |
| **O-10** recolour / edit | `fal-ai/flux-pro/kontext` | 2 | `40_000` ($0.040) | images / 1 output image | **yes** | Instruction-based image edit (recolour) | image + prompt in → image out |
| **O-11** upscale | `fal-ai/clarity-upscaler` | 1 | `125_829` ($0.125829) | megapixels / 2048×2048 | **no** | Upscale | image in → image out — **DISABLED (see §1b)** |
| **O-12** erase region | `fal-ai/bria/eraser` | 2 | `40_000` ($0.040) | generations / 1 generation | **yes** | Erase a region | image in → image out. **No mask is plumbed** (see §4) |
| **O-13** ghost mannequin | **`null` — no route** | 5 | **`null`** | — | **no** | Ghost-mannequin garment shot | — |
| **O-14** virtual model | `fal-ai/idm-vton` | 5 | `75_000` ($0.075) | compute seconds / 60 s | **yes** | Virtual try-on / on-model render | image in → image out |
| **O-15** listing text | **`null` — no route** | 1 | **`null`** | — | **no** | Generate listing copy | — (text in/out would be needed) |

`operationConfig()` (`registry.ts:88-94`) reports `enabled: model.enabled && Boolean(config.falKey) && !killSwitch`
— so with **no `FAL_KEY`**, every operation is reported disabled regardless of the literal (`registry.ts:92`).

**Two operations carry a real, recorded `costMicroUsd` but are switched off in code:** O-11 and O-13.

### 1b. Disabled and absent operations, with the reason verbatim

- **O-11 (upscale) — disabled on cost grounds.** The registry comment is verbatim
  (`registry.ts:67`):
  > `// Disabled: light cap is $0.025; $0.03/MP exceeds it at 1024x1024 ($0.031), and 2048x2048 costs $0.125829 (> premium $0.125).`
  So O-11 is a **priced, measured decision**: the modelled price does not fit the "light" credit tier and
  exceeds the "premium" floor at its reference output.
- **O-13 (ghost mannequin) — no fal route exists.** `endpointId: null`, `costMicroUsd: null` (`registry.ts:75`).
  There is no comment in `registry.ts`; the README states it plainly (see §1g).
- **O-15 (listing text) — no route, unresolved.** `endpointId: null`, `costMicroUsd: null` (`registry.ts:83`).
  No text-generation operation exists anywhere in the registry: **every operation that has a route is
  image-in/image-out or image-in/JSON-out.** There is no text-to-text operation.

### 1c. Auth model

- **The client holds no secret.** The only host the client names is a single constant,
  `API_BASE_URL = "https://pikupo.com/plinth/v1"` (`app/src/main/java/**/data/api/ApiConfig.kt:3`). No fal host,
  no `fal` string and no key-shaped literal appears under `app/src/main/` — confirmed by
  `node scripts/check-client-secrets.mjs --slot plinth-product-photos` → **PASS**, `scanned 204 client file(s) …
  no key-shaped literal, paid-provider SDK, paid-provider host or keyed google-services.json`.
- **The fal key is server-only.** `FAL_KEY` is read from the server environment and used as
  `Authorization: Key ${key}` on the fal request (`registry.ts:131`, `:139`, `:165`). The README (line ~31) states
  `FAL_KEY must remain unset until an owner authorizes provider spending`.
- **Client authentication = a Play Integrity–minted device token.** The flow is
  `POST /v1/integrity/challenge` → `POST /v1/session` (Play Integrity standard token + `installId` + a
  `sessionRequestHash` binding the annex fields and locale) → a signed HS256 JWT device token (`JWT_SECRET`,
  ≥32 chars). Every authenticated call sends `Authorization: Bearer <deviceToken>` (`ApiClient.kt:199`). The
  server validates the token **and** re-checks the device row on every request — `token_generation`, `blocked`,
  `deleted_at` (`server/src/auth.ts:12-45`). A stale generation → 401 `UNAUTHORIZED`; a blocked device → 403
  `DEVICE_BLOCKED`.
- Session/purchase/RTDN endpoints are **only enabled when the corresponding server credentials and expected app
  identity settings exist** (`PLAY_SERVICE_ACCOUNT_JSON`, `PLAY_VERSION_CODE`, `PLAY_SIGNING_CERT_SHA256`,
  `BILLING_BINDING_SECRET`, `PLAY_CREDIT_PRODUCTS`, `PUBSUB_AUDIENCE`, …) — README lines ~93-100.

### 1d. Wallet / credits model — server-authoritative

The credit ledger is **server-authoritative**, and this is the decisive fact for reuse: a client-side balance edit
cannot create spendable credit. Evidence:

- The authoritative ledger is a SQLite table with a **non-negative balance CHECK** and a ledger of typed
  movements (`server/migrations/001_init.sql`): `wallet(balance … CHECK(balance >= 0), refund_debt, purchase_hold,
  starter_granted)`; `ledger(kind IN ('STARTER','PURCHASE','RESERVE','SETTLE','RELEASE','REFUND_ADJUST'), delta,
  idempotency_key, UNIQUE(wallet_id, kind, idempotency_key))`.
- **Two-phase reserve/settle/release** in `server/src/ledger.ts`: `reserve()` (:18) refuses when
  `refund_debt > 0 || purchase_hold > 0 || balance < credits` (:39); `settle()` (:52) converts a reservation into
  a charge; `release()` (:126) returns it. Every movement is idempotent by key.
- **Purchase flow is server-side.** `POST /v1/purchases/intents` returns HMAC-derived obfuscated account/profile
  ids for `BillingFlowParams` (`routes.ts:220`); `POST /v1/purchases/confirm` **re-reads the purchase from the
  Google Play Developer API**, fulfils it in a durable state machine, then consumes it on the server
  (`routes.ts:238`; states `RECEIVED→VERIFIED→FULFILLED→PLAY_FINALIZING→CONSUMED`, `001_init.sql`). Purchase tokens
  are AES-256-GCM envelope-encrypted until Play finalization. Voided purchases are clawed back via Play's
  authoritative API and become `refund_debt`, which blocks reservations until repaid (README ~78-82).
- **The client reuses the factory's `PurchaseAuthority` seam** but the spend truth is the server. The slot's
  implementation is `WalletPurchaseAuthority` (`app/.../data/wallet/WalletPurchaseCoordinator.kt:41`), implementing
  `com.gexetr.capability.billing.PurchaseAuthority`; `confirmSpend()` (coordinator `:86`) is the interface the
  capability documents as required "before any local debit". `WalletPurchaseApi.confirmPurchase()` (`:24`, `:95`)
  talks to the server.
- **Client-side is a cache/outbox, not the ledger.** `data/outbox/PurchaseOutbox.kt` and
  `PurchaseFulfilmentWorker.kt` are a retry queue for server confirmation (the worker calls
  `api.confirmPurchase`, `PurchaseFulfilmentWorker.kt:28`); `EncryptedBillingStateStore`/`AndroidKeystoreVault`
  store billing state encrypted at rest. The wallet is **deliberately excluded from cloud backup**
  (`differentiators.json` claim `look-backup-and-resume`).

**Reuse verdict:** the *credit-spend* mechanism is server-authoritative and therefore reusable in principle — but
it is not packaged as a shared component (see Part 3): the ledger, purchases, integrity and routes all live under
this slot's `server/`.

### 1e. Cost accounting and the operation-price guard

- **The real cost of every call is recorded.** Each `job` row stores `cost_micro_usd` and `provider_endpoint`
  (`001_init.sql` job table; written in `queue.ts:100-102`). Daily spend is accumulated in `spend_day(day,
  cost_micro_usd, starter_cost_micro_usd)` (`001_init.sql`), incremented by `reserveDailyBudget()`
  (`queue.ts:242-255`).
- **There IS a spend guard, but it is a *daily cap*, not a *per-operation price ceiling*.**
  `reserveDailyBudget` throws `CAP_REACHED` when `today + costMicroUsd > DAILY_SPEND_CAP_USD` (default **$25**,
  `queue.ts:243`, README `DAILY_SPEND_CAP_USD`). The same cap disables paid ops in the config response and
  `/v1/health` (`routes.ts:118`, `:179`, `:183`). It is reserved **before** the provider call (`queue.ts:115-120`),
  and the reservation is released if the cap is hit.
- There is **no code guard that refuses an operation whose price exceeds its credit tier** — that guard is prose
  only, applied by hand (O-11 was disabled by hand; see §1b). A second app that priced a fal call no credit tier
  could carry would not be stopped by any program. (This is the "annex E tier-ceiling" claim; it is **not**
  implemented — see §4.)

### 1f. Error and retry surface

- **One API envelope for every error** (`routes.ts` + `app.ts` error handler):
  `{ error: { code, message, retryAfterSec, details } }`. Typed codes observed in source: `INVALID_REQUEST`,
  `UNAUTHORIZED`, `DEVICE_BLOCKED`, `INTERNAL`, `TOO_LARGE`, `KILL_SWITCH`, `OPERATION_DISABLED`,
  `INSUFFICIENT_CREDITS`, `CAP_REACHED`, `GUARD_REJECTED`, `MODERATION_BLOCKED`, `PROVIDER_FAILED`,
  `PROVIDER_RESULT_HASH_MISMATCH`. `retryAfterSec` is `null` in every `replyError` path
  (`server/test/error-handler.test.ts` asserts this for 400/401/…/499 and 500/TOO_LARGE/INVALID_REQUEST).
- **`GET /v1/jobs/:id` sets `Retry-After: 2`** (`routes.ts:472`) — the client's polling cadence for a running job.
- **Failure means no charge.** On any provider error or a guard rejection the queue calls
  `this.ledger.release(walletId, jobId)` (`queue.ts:233`), so the reservation returns to the wallet; a failed job
  is `charged = 0`. `recoverInterruptedJobs()` also releases jobs stuck `QUEUED` or `RUNNING` > 180 s
  (`queue.ts:148-157`). This is exactly the shipped promise `failed-result-never-charged`.
- **Retry policy**: exactly **one automatic retry** with a fresh random seed when the provider reports a
  fidelity-guard failure (`queue.ts:193-199`); if the retry also fails the guard → `GUARD_REJECTED` and release.
  The retry's own provider cost is reserved against the daily cap (`queue.ts:196`). A provider HTTP/hash failure is
  **not** retried — it becomes `PROVIDER_FAILED` / `PROVIDER_RESULT_HASH_MISMATCH` and releases.
- **Provider result integrity**: the queue recomputes `sha256(output.bytes)` and throws
  `PROVIDER_RESULT_HASH_MISMATCH` on disagreement (`queue.ts:201-202`); the client independently verifies the
  downloaded bytes against `Content-Length`, the `Digest` header and the declared sha256 (`ApiClient.kt:146-162`).
- **Duplicate-submission safety**: a job is unique on `(wallet_id, client_request_id)` (`001_init.sql`), so a
  retried client request returns the existing job rather than double-reserving (`queue.ts:92-94`).

### 1g. Registry-vs-README drift (read the code, not the prose)

`server/README.md:59-61` claims **"O-07 through O-12 have the measured M0 endpoint selections but remain disabled
until exact cost ceilings are verified. O-13 has no route; O-14 remains disabled; O-15 remains unresolved."**
`registry.ts` contradicts this: **O-07, O-08, O-09, O-10, O-12 and O-14 are `enabled: true`**; only O-11 (cost),
O-13 and O-15 (no route) are `false`. **`registry.ts` is the source of truth** — it is what `enqueue()` and the
config response read (`queue.ts:89`, `registry.ts:92`). The README is stale; treat its status paragraph as
unreliable.

### 1h. What `/v1/recognize` and `/v1/jobs` actually expose

- **`POST /v1/recognize`** (`routes.ts:321`) — raw JPEG body + `X-Client-Sha256`, absorbed at **zero wallet
  credits**, guarded by the daily cost cap (`routes.ts:362`), returns item JSON. Used live by the client
  (`AutoCardScreen.kt:132`).
- **`POST /v1/jobs`** (`routes.ts:390`) — body `{ clientRequestId, op, uploadIds[], params, estimateCredits }`.
  Uploads first go through `PUT /v1/uploads` (raw bytes, `X-Client-Sha256`). The handler requires
  `estimateCredits === model.credits` exactly (`routes.ts:404`) and, **for every operation, reads only
  `orderedUploads[0]` as *the* image** (`routes.ts:436`) — multiple `uploadIds` are accepted but only the first
  contributes pixels; anything else must travel inside `params`. Returns **202** `{ jobId, reserved, state }`.
  Poll `GET /v1/jobs/:id`; cancel `POST /v1/jobs/:id/cancel`; download `GET /v1/jobs/:id/result` with
  `X-Result-Token`; confirm delivery `POST /v1/jobs/:id/delivery-receipts`.

---

## Part 2 — What the existing app already ships

### 2a. The promise contract (`differentiators.json`) — 15 claims

Job coverage: `before` = `multi-import-pick`, `during` = `auto-card-by-item`, `after` = `export-pack-per-marketplace`
(`differentiators.json:7-23`). Statuses: **12 shipped, 3 narrowed, 0 dropped.**

| id | weight | jobStage | depthFamily | status |
|---|---|---|---|---|
| `item-pixels-untouched` | core | during | core_operation | shipped |
| `failed-result-never-charged` | core | during | trust | shipped |
| `auto-card-by-item` | core | during | core_operation | shipped |
| `marketplace-rules-linter` | core | after | fit | shipped |
| `batch-with-saved-look` | core | during | bulk | shipped |
| `export-pack-per-marketplace` | core | after | data_out | shipped |
| `slide-kit-series` | core | after | speed | shipped |
| `multi-import-pick` | supporting | before | data_in | **narrowed** (≤5 photos; no duplicate flagging) |
| `shoot-coach` | supporting | before | data_in | shipped |
| `sku-grouping-scale-reading` | supporting | before | fit | **narrowed** (grouping shipped; ruler/label scale-reading dropped) |
| `listing-text-six-languages` | supporting | after | fit | shipped |
| `library-search` | supporting | after | find | shipped |
| `batch-done-reminder` | supporting | after | reminder | shipped |
| `look-backup-and-resume` | supporting | after | durability | shipped |
| `studio-tools-enhanced` | supporting | during | core_operation | **narrowed** (ops are named & priced but "not generally available or guaranteed to execute") |

The only connective floor claim honoured with a real API is `export-pack-per-marketplace` (proof symbol
`ACTION_SEND`, `ExportDelivery.kt`) — plus `shoot-coach`/`multi-import-pick` for `data_in`.

### 2b. Every screen, with its route, in the order a user meets them

Destinations are declared in `app/.../ui/PlayKilnApp.kt:98-155` and wired in `entryProvider` (`:171-468`).

| # | Screen (function) | Nav destination (`PlayKilnApp.kt`) | Reached from |
|---:|---|---|---|
| 1 | `HomeRoute` / `HomeScreen` | `HomeDestination` (:98) | app start |
| 2 | `AboutScreen` | `AboutDestination` (:100) | Home |
| 3 | `SettingsRoute` | `SettingsDestination` (:103) | Home |
| 4 | `ImportRoute` | `ImportDestination` (:106) | Home |
| 5 | `CardRoute` | `CardDestination(photoId)` (:109) | Home / Import |
| 6 | `EditorRoute` | `EditorDestination(photoId)` (:112) | Card |
| 7 | `CutoutRoute` | `CutoutDestination` (:115) | Home |
| 8 | `AutoCardRoute` | `AutoCardDestination(photoId)` (:118) | Home (calls **O-01** here) |
| 9 | `AutoCardResultScreen` | `AutoCardResultDestination(photoId)` (:121) | AutoCard |
| 10 | `ExportScreen` (+ linter panel) | `ExportDestination` (:124) | Home / Project |
| 11 | `LibraryScreen` | `LibraryDestination` (:127) | Home |
| 12 | `ProjectWorkspaceScreen` | `ProjectDestination(projectId)` (:131) | Library |
| 13 | `GroupScreen` | `GroupDestination(projectId)` (:133) | Project |
| 14 | `LooksScreen` | `LooksDestination(projectId)` (:136) | Project |
| 15 | `BatchScreen` | `BatchDestination(projectId, lookId)` (:139) | Looks |
| 16 | `SlideKitScreen` | `SlideKitDestination(photoId)` (:142) | Home |
| 17 | `ListingTextScreen` | `ListingTextDestination` (:145) | Home |
| 18 | `StudioToolsScreen` | `StudioToolsDestination` (:148) | Home |
| 19 | `WalletRoute` | `WalletDestination` (:152) | Home / Settings / StudioTools |
| 20 | `CoachRoute` | `CoachDestination(projectId)` (:155) | Home |

**Which screens actually call the server:** only **two** generation calls exist in the client —
`recognizePhoto` (O-01) in `AutoCardScreen.kt:132`, and `submitCutout` (O-02) in `CutoutViewModel.kt:458`
(via `ApiClient.submitCutout`, `ApiClient.kt:102-115`). Everything else is on-device. In particular
**`StudioToolsScreen` is wired to a Toast, not to the server** — its `onSelectOperation` shows
`home_nav_operation_unavailable` and it is constructed with `balanceCredits = 0`
(`PlayKilnApp.kt:439-450`). So O-07…O-14 are served-and-enabled by the backend but **unreachable from the
shipped UI**.

### 2c. The job this app owns — one sentence

> **Turn a seller's own product photos into a marketplace-ready photo pack for one chosen marketplace —
> import, cut out, compose on a background, auto-card, lint the marketplace's image rules, type listing text,
> and export — with the paid AI steps metered against a server-authoritative credit wallet.**

Any new app whose sentence is the same is a duplicate; a new app that borrows the *server* but does a different
job is not.

---

## Part 3 — What is reusable across a family of apps

### 3a. Already factored, and safely reusable (the `capabilities/` modules)

Five versioned Android library modules, published under the group `com.gexetr.capability` version `1.0.0`
(`capabilities/gradle.properties:12,16`) and consumed as Maven coordinates
(`app/gradle/libs.versions.toml:77-78` → `com.gexetr.capability:core`, `:billing`). Each is a separate Gradle
project with `maven-publish` and its own test suite.

| Module | Provides (source root `capabilities/<m>/src/main/kotlin/com/gexetr/capability/<m>/`) | Depends on |
|---|---|---|
| **core** | `PlayKilnResult` (typed success/failure), `Clock`, `CapabilityState`, `CapabilityDiagnostics` — contracts only, coroutines-only, no Android/SDK types (`capabilities/core/build.gradle.kts:42-45`) | — |
| **billing** | `BillingController`/`PlayBillingController`, `ProductId`, `ConsumablePack`, `Wallet`, `Entitlements`, `PurchaseLedger`, `BillingStateStore`, `BillingFlowParamsFactory`, and the **`PurchaseAuthority` seam** (`capabilities/billing/src/main/kotlin/.../PurchaseAuthority.kt:30`) | core + official Play Billing |
| **ads** | `AdsController`/`AdsConfig`/`AdMobAdsController` + consent gate; SDK types kept private (`capabilities/ads/build.gradle.kts:5-7,46-48`) | core + GMS Ads |
| **telemetry** | `Telemetry` (no-op default) + `Redaction` — zero tracking unless opted in (`capabilities/telemetry/build.gradle.kts:1-4`) | core |
| **remoteconfig** | `RemoteConfigController` + safe defaults + a fake; Firebase kept private | core + Firebase |

`PurchaseAuthority` is the interface a second app would reimplement; the factory deliberately ships **no default
implementation** (`PurchaseAuthority.kt:27-31`), and `PlayBillingController` refuses to serve a catalog that needs
an authority without one. This slot is its **first implementation** (`WalletPurchaseAuthority`,
`WalletPurchaseCoordinator.kt:41`). That interface — plus billing's connection retry, offer caching,
acknowledgement, pending-purchase resolution and a deterministic fake (the module doc comment,
`capabilities/billing/build.gradle.kts:1-4`) — is genuinely reusable platform code.

The slot enables only `core` + `billing` (`capabilities.json` → `{"schemaVersion":1,"enabled":["billing"]}`).

### 3b. Welded to this slot — would have to be rewritten per app

These are **not** capabilities and live under the slot; a second app cannot import them.

| Piece | Where | Why it cannot be reused as-is |
|---|---|---|
| **The operation registry** | `server/src/registry.ts:39-85` (`MODEL_REGISTRY`) | Operation ids `O-01…O-15` are this app's photo jobs; the ids, credits and endpoints encode its palette of operations. A new app's jobs do not exist here. |
| **The HTTP surface** | `server/src/routes.ts` | Paths, response shapes, upload semantics (single-image, `routes.ts:436`) and the recognition route are Plinth-shaped; the client (`ApiClient.kt`) is written against them. |
| **The generation pipeline / fal adapter** | `server/src/registry.ts:130-216` (`createFalProvider`) | Single-image `image_url` base64 data-URI + verbatim `params`, sync mode, `{images:[{url}]}` response. **No mask, no multi-image, no text-out plumbing** — every new I/O shape is new code here. |
| **The ledger / purchases / integrity / retention server** | `server/src/{ledger,purchases,integrity,retention,moderation,google-play,queue}.ts`, `migrations/*` | Server-authoritative and well-built, but it is a slot-local service, not a published module; product ids, package name (`PLAY_PACKAGE_NAME`) and routes bind it to Plinth. A second app either reuses *this* deployment's API or copies the server. |
| **The client API layer** | `app/.../data/api/*` | `ApiConfig.kt` hardcodes `https://pikupo.com/plinth/v1`; `ApiClient` methods are Plinth endpoints. |
| **The client wallet/outbox** | `data/wallet/*`, `data/outbox/*` | `WalletPurchaseAuthority`, product ids and the outbox are written against the Plinth server's purchase API. |
| **Brand + store identity** | `store/studio.json`, theme, mark, `ui/components` | Per-slot by rule (`.cursor/rules/visual-identity.mdc`); must not be shared. |

**Bottom line for reuse:** what a second app can *reuse without writing code* is the five `capabilities/`
modules and the `PurchaseAuthority` interface. What it must *build or copy* is the whole generation path —
registry, adapter, routes, server — because none of that is factored out of the slot today.

---

## Part 4 — The honest gaps

What the backend **cannot** do today:

1. **No text-generation operation at all.** O-15 (listing text) has no route and is disabled; every routed
   operation is image in / image (or JSON) out. A new app whose job is "write the listing / the reply / the email"
   needs a **new fal endpoint and a text I/O path** in the adapter — today the adapter only handles
   `{images:[{url}]}`. Cost: **UNVERIFIED** — decide by reading the chosen endpoint's page on
   `fal.ai/models/<endpointId>`.
2. **No fidelity guard is actually computed.** The `guard` field on `ProviderOutput` (`registry.ts:17`) is
   optional and **never set** by either `createFakeProvider` or `createFalProvider`. So the retry branch
   (`queue.ts:193-199`) and the `GUARD_REJECTED` path are **unreachable in production today**, and `guard_json`
   in `result_metadata` is always NULL. The prompt-only guard is a seam with no implementation. A family app that
   promises "AI output that keeps the product faithful" must implement the guard itself.
3. **No mask / region plumbing.** O-12 (eraser) is "image in → image out" in code; there is no mask upload path
   (`graphql`-free, no mask field in `params` handling). Masked edits (erase, selective inpaint) need new adapter
   and route work.
4. **Single-image only.** `routes.ts:436` reads `orderedUploads[0]`; a job that genuinely needs two images
   (e.g. a garment + a model for a second virtual-try-on) has no first-class input — it must smuggle the second
   image through `params`, and the adapter does not send it.
5. **No inbound-facing generation ops for non-photo domains.** Video, audio, 3-D, upscaling
   (O-11 disabled), ghost mannequin (O-13 no route) and any other job have **no operation**. Each needs a new
   `MODEL_REGISTRY` entry **and a real fal endpoint whose price has been measured**. Prices for such endpoints
   are **UNVERIFIED** here — the deciding source is the endpoint's own page at `fal.ai/models/<endpointId>`.
6. **No per-operation price-ceiling guard.** A daily spend cap exists ($25 default), but nothing refuses an
   operation whose real cost exceeds the credit tier it is sold under. Pricing a new op correctly is not enforced;
   O-11 was disabled by hand. A family that adds ops at scale is one arithmetic slip from serving a call for less
   than it costs.
7. **`endpointId: null` ops (O-13, O-15) are decisions, not TODOs.** Adding them is product work (choose an
   endpoint, measure its cost, set credits), not config.

Published prices for **new** endpoints are not invented here. Every `costMicroUsd` in this document is the value
**recorded in `registry.ts`**, which is the factory's own measurement; whether it still matches fal's current
published price on 2026-10-16 is **UNVERIFIED** (would be decided by re-reading the endpoint page).

---

## Part 5 — The spec the slot was written inside

`openspec/changes/app-seller-photo-studio/**` (note: change name `app-seller-photo-studio`, slug
`plinth-product-photos`).

**What the change says the app is for** (`proposal.md`): a server-backed Android app that does the *whole seller
photo job* — "one universal platform, not a photo-in photo-out generator": cut out, place on backgrounds with
shadows, compose to the marketplace canvas, edit by hand, one-tap auto-card, marketplace presets + rules linter,
batch/saved Looks/SKU grouping/scale-SKU-ruler reading/listing text in six languages/shoot coach/library/
reminders/backup, a generative studio, and the factory's **first `PurchaseAuthority`** server that confirms Play
purchases, holds the credit ledger, calls fal.ai with a server-only key, and enforces cost caps and retention.
Monetization: `purchase_led_repeat_sales` (consumable credit packs + one non-consumable unlock), no subscription,
no ads. New capabilities declared: `card-pipeline`, `marketplace-presets`, `batch-and-library`, `shoot-coach`,
`studio-ai`, `credits-wallet-server`, `listing-and-export`, `slide-kit`, `provenance-and-compliance`.

**Agreement vs drift (proposal → shipped promises):**

- **Agrees:** the cut-out/compose/card path, the marketplace linter, batch + saved Looks + retry, export pack,
  slide kit, shoot coach, library/search, reminder, backup, and the credit/wallet server are all `shipped`.
- **Drift 1 — the generative studio was descoped.** The proposal sells a *generative studio* (relight, expand,
  eraser, upscale, recolour, ghost mannequin, virtual model). The shipped claim `studio-tools-enhanced` is
  **`narrowed`**: the operations are "named and priced" but "**not generally available or guaranteed to
  execute**", and the shipped UI wires `StudioToolsDestination` to an "unavailable" Toast
  (`PlayKilnApp.kt:443-450`). The server *does* enable O-07…O-10/O-12/O-14, but no screen calls them.
- **Drift 2 — scale/ruler reading was dropped.** The proposal's `batch-and-library` includes "scale/SKU/ruler
  reading"; the claim `sku-grouping-scale-reading` is `narrowed` — grouping ships, scale-reading does not.
- **Drift 3 — multi-import was narrowed.** The proposal's `data_in` promise (many photos, duplicates flagged,
  chunked big batches) shipped as `multi-import-pick`, narrowed to ≤5 sequential photos with no duplicate flagging.
- **Drift 4 — the fidelity guard is prose, not code.** The proposal's "constitution" is a `GuardRejected` on
  unfaithful output; per §4, no guard is computed server-side today, so the mechanism behind
  `failed-result-never-charged`'s guard half (as opposed to its release-on-failure half) is unimplemented.

The `tasks.md` and the delta specs were not read line-by-line for this inventory (they are large annex-driven
documents); the drift above is measured against `differentiators.json` and the shipped UI, which is the surface a
planner needs.

---

## Claim ledger

| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| All eleven operations are defined in one literal `MODEL_REGISTRY` | MEASURED | `registry.ts:39-85` |
| O-01 = `fal-ai/any-llm/vision`, 0 credits, cost 10000 µUSD, enabled | MEASURED | `registry.ts:40-41` |
| O-02 = `fal-ai/bria/background/remove`, 1 credit, 18000 µUSD, enabled, prompt-free | MEASURED | `registry.ts:44-45`; `registry.ts:159` |
| O-07 = `fal-ai/flux/dev`, 2 credits, 50000 µUSD, enabled | MEASURED | `registry.ts:48-50` |
| O-08 = `fal-ai/iclight-v2`, 5 credits, 104858 µUSD, enabled | MEASURED | `registry.ts:53-55` |
| O-09 = `fal-ai/bria/expand`, 2 credits, 40000 µUSD, enabled | MEASURED | `registry.ts:58-59` |
| O-10 = `fal-ai/flux-pro/kontext`, 2 credits, 40000 µUSD, enabled | MEASURED | `registry.ts:62-63` |
| O-11 = `fal-ai/clarity-upscaler`, **disabled**, 125829 µUSD | MEASURED | `registry.ts:66-68` |
| O-11's verbatim disable reason | MEASURED | `registry.ts:67` |
| O-12 = `fal-ai/bria/eraser`, 2 credits, 40000 µUSD, enabled | MEASURED | `registry.ts:71-72` |
| O-13 = no endpoint (`null`), disabled, cost `null` | MEASURED | `registry.ts:75-76` |
| O-14 = `fal-ai/idm-vton`, 5 credits, 75000 µUSD, enabled | MEASURED | `registry.ts:79-80` |
| O-15 = no endpoint (`null`), disabled, cost `null` | MEASURED | `registry.ts:83-84` |
| Ops are disabled when no `FAL_KEY` | MEASURED | `registry.ts:92`; `registry.ts:131` |
| fal is called at `https://queue.fal.run/<endpointId>` with `Authorization: Key` | MEASURED | `registry.ts:138-139`; `:163-165` |
| fal adapter sends one image as a base64 data-URI + verbatim `params`, sync mode | MEASURED | `registry.ts:166-170` |
| fal adapter expects `{images:[{url}]}` and fetches the URL for bytes | MEASURED | `registry.ts:174-186` |
| The client's only host is `https://pikupo.com/plinth/v1` | MEASURED | `app/.../data/api/ApiConfig.kt:3` |
| No client secret / no fal host under `app/src/main/` | MEASURED | `node scripts/check-client-secrets.mjs --slot plinth-product-photos` → PASS, 204 files |
| Server auth = HS256 JWT device token via Play Integrity session | MEASURED | `server/src/auth.ts:12-45`; `ApiClient.kt:29-40`; README ~66-70 |
| Auth re-checks token generation, blocked and deleted on every call | MEASURED | `auth.ts:30-45` |
| Wallet balance is non-negative and ledger movements are typed | MEASURED | `migrations/001_init.sql` (wallet, ledger tables) |
| Reserve refuses when refund_debt/purchase_hold > 0 or balance < credits | MEASURED | `ledger.ts:39` |
| Reserve/settle/release exist | MEASURED | `ledger.ts:18`, `:52`, `:126` |
| Credit spend is server-authoritative (client cannot create spendable credit) | MEASURED | `ledger.ts`; `PurchaseAuthority.kt:5-28`; `WalletPurchaseCoordinator.kt:86` |
| Purchases are re-read from Play and fulfilled server-side | MEASURED | `routes.ts:220`, `:238`; `migrations/001_init.sql` purchase state machine; README ~78-82 |
| The slot implements the `PurchaseAuthority` seam (`WalletPurchaseAuthority`) | MEASURED | `app/.../data/wallet/WalletPurchaseCoordinator.kt:41,46` |
| Client outbox/workers only retry server confirmation | MEASURED | `data/outbox/PurchaseOutbox.kt`; `PurchaseFulfilmentWorker.kt:28` |
| Each job records `cost_micro_usd` and `provider_endpoint` | MEASURED | `migrations/001_init.sql`; `queue.ts:100-102` |
| Daily spend is tracked and capped (default $25) | MEASURED | `queue.ts:242-255`; README `DAILY_SPEND_CAP_USD`; `routes.ts:118,179,183` |
| **No** per-operation price-ceiling guard is implemented | MEASURED | no such check in `queue.ts`/`routes.ts`; only `reserveDailyBudget` (`queue.ts:242`) |
| Errors use one envelope with `retryAfterSec` null in reply paths | MEASURED | `server/test/error-handler.test.ts:31-40,66-70` |
| `GET /v1/jobs/:id` sets `Retry-After: 2` | MEASURED | `routes.ts:472` |
| A failed/guard-rejected job releases its reservation (no charge) | MEASURED | `queue.ts:229-234`; `queue.ts:193-199` |
| Interrupted jobs are released after 180 s | MEASURED | `queue.ts:148-157` |
| Exactly one automatic re-run on a guard failure with a new seed | MEASURED | `queue.ts:193-199` |
| Provider bytes are hash-checked against the declared sha256 | MEASURED | `queue.ts:201-202` |
| Client re-verifies length + Digest + sha256 on download | MEASURED | `ApiClient.kt:146-162` |
| Jobs are unique on `(wallet_id, client_request_id)` (idempotent submit) | MEASURED | `migrations/001_init.sql`; `queue.ts:92-94` |
| **No fidelity guard is computed** — `guard` is never set by either provider | MEASURED | `registry.ts:17` (optional); `createFakeProvider` `registry.ts:104-124` and `createFalProvider` `registry.ts:130-193` set none; only reader is `queue.ts:193,199` |
| O-12 (eraser) has **no mask plumbing** | MEASURED | adapter sends only `image_url` + `params` (`registry.ts:166-170`); no mask field anywhere |
| A job uses only the **first** upload as its image | MEASURED | `routes.ts:436` |
| `estimateCredits` must equal `model.credits` | MEASURED | `routes.ts:404` |
| Registry (source of truth) disagrees with README's disabled-list | MEASURED | `registry.ts:48-80` (enabled true) vs `server/README.md:59-61` |
| 15 promises; 12 shipped, 3 narrowed | MEASURED | `differentiators.json` (all `status` fields) |
| The three narrowed promises are `multi-import-pick`, `sku-grouping-scale-reading`, `studio-tools-enhanced` | MEASURED | `differentiators.json` (`status` fields) |
| `export-pack-per-marketplace` is proved by `ACTION_SEND` | MEASURED | `differentiators.json` proof block (`ExportDelivery.kt`) |
| 20 nav destinations exist, named as in §2b | MEASURED | `PlayKilnApp.kt:98-155` |
| Only O-01 and O-02 are called from the client | MEASURED | `AutoCardScreen.kt:132`; `CutoutViewModel.kt:458`; `ApiClient.kt:94-115` |
| StudioTools is wired to a Toast, `balanceCredits = 0` | MEASURED | `PlayKilnApp.kt:439-450` |
| `capabilities/core` is contracts-only over coroutines | MEASURED | `capabilities/core/build.gradle.kts:44` |
| `capabilities/billing` ships the `PurchaseAuthority` seam and a refusal without one | MEASURED | `PurchaseAuthority.kt:27-31`; `PlayBillingController` per module doc `capabilities/billing/build.gradle.kts:1-4` |
| Capabilities publish as `com.gexetr.capability` v`1.0.0` | MEASURED | `capabilities/gradle.properties:12,16` |
| The slot consumes only `core` + `billing` | MEASURED | `capabilities.json`; `app/build.gradle.kts:155-158`; `app/gradle/libs.versions.toml:77-78` |
| The generation registry/pipeline/routes/server are slot-local, not a capability | MEASURED | glob `capabilities/**/*.kt` has no generation source; `server/` is under the slot |
| The spec change is `app-seller-photo-studio` for slug `plinth-product-photos` | MEASURED | `openspec/changes/app-seller-photo-studio/proposal.md` (`--spec app-seller-photo-studio`) |
| The spec proposes a full generative studio incl. ghost mannequin & virtual model | MEASURED | `proposal.md` §What Changes ("Generative studio …") |
| The studio drifted to `narrowed` in the shipped contract | MEASURED | `differentiators.json#studio-tools-enhanced` (`status: narrowed`) |
| The spec's scale/ruler reading drifted to narrowed | MEASURED | `differentiators.json#sku-grouping-scale-reading` vs `proposal.md` ("scale/SKU/ruler reading") |
| **Every** recorded `costMicroUsd` is the factory's own value, not a live fal price read today | UNVERIFIED | would be decided by re-reading each `fal.ai/models/<endpointId>` page |
| The price of any endpoint **not** in the registry (video/text/upscale replacements, etc.) | UNVERIFIED | would be decided by the chosen endpoint's page at `fal.ai/models/<endpointId>` |
| The live server behaviour (does it actually serve O-07…O-14 today) | UNVERIFIED | read-only inventory; not deployed/run here. Would be decided by a live `GET https://pikupo.com/plinth/v1/config` with a valid session, or by the server's own test run |
| `tasks.md`/delta-spec line-by-line agreement with the app | UNVERIFIED | not read line-by-line this pass; the drift above is measured against `differentiators.json` + shipped UI |
