# Plinth Product Photos server

Fastify + TypeScript service using `better-sqlite3` in WAL mode. Startup applies
`migrations/001_init.sql` automatically. `GET /health` (also `/v1/health`)
returns the service/version/time and confirms a SQLite query completed; it does
not claim that Play Integrity, purchases, or a model provider are configured.

## Run locally

Use Node.js 22.9+ (Node 24 recommended). From this directory:

```powershell
Copy-Item .env.example .env
# Replace JWT_SECRET and RECOVERY_PEPPER in .env with distinct random values.
npm install --cache ..\..\..\.kiln\scratch\plinth-w1-server\npm-cache
npm run dev
```

The example contains placeholders only. `.env` is ignored by Git and is read by
Node's `--env-file-if-exists`; production secrets belong in the service manager's
environment/secret store, never in source, the Android client, or logs. `JWT_SECRET`
is required and must contain at least 32 characters. `RECOVERY_PEPPER` is optional
until recovery-code support is needed. `FAL_KEY` must remain unset until an owner
authorizes provider spending; tests inject only `fakeProvider`.

Available environment settings:

| Variable | Default | Purpose |
|---|---:|---|
| `JWT_SECRET` | required | HS256 device-token signing |
| `FAL_KEY` | unset | Server-only fal credential; constructs the paid adapter only when set |
| `RECOVERY_PEPPER` | unset | Server-only HMAC pepper for recovery-code hashes |
| `DATABASE_PATH` | `data/server.sqlite` | SQLite database file |
| `DATA_DIR` | `data` | Upload/result storage root |
| `JOB_CONCURRENCY` | `2` | Maximum active provider jobs |
| `DAILY_SPEND_CAP_USD` | `25` | UTC-day provider spend ceiling |
| `STARTER_DAILY_BUDGET_USD` | `10` | UTC-day starter liability cap, reserved at $0.025 per granted credit |
| `STARTER_CREDITS` | `10` | Conditional grant after core/device verdicts and Device Recall bit-1 write succeed |
| `PLINTH_DEV_INTEGRITY` | `0` | Only `1` enables local Google attestation/recall simulation; forbidden with `NODE_ENV=production` |
| `MIN_APP_VERSION` | `0` | Minimum client version enforced server-side on every mutating route (`426 UPDATE_REQUIRED`); `0` disables the gate |
| `KILL_SWITCH_FILE` | `KILL_SWITCH` | Presence pauses new paid work (`maintenance`); reads and refunds stay open |
| `SERVICE_CLOSED_FILE` | `SERVICE_CLOSED` | Presence stops new paid work permanently (`closed`); outranks the kill switch |
| `TRUST_PROXY` | `loopback` | The only trusted reverse-proxy hop; anything wider lets a caller forge `X-Forwarded-For` |
| `PORT` / `HOST` | `3000` / `127.0.0.1` | Listener address |
| `PLAY_PACKAGE_NAME` | `com.gexetr.plinthproductphotos` | Expected package returned by Play |
| `PLAY_VERSION_CODE` | unset | Exact version code accepted by the integrity verifier |
| `PLAY_SIGNING_CERT_SHA256` | unset | Accepted Play signing certificate digest, hex |
| `PLAY_SERVICE_ACCOUNT_JSON` | unset | Server-only Android Publisher / Play Integrity service-account JSON |
| `BILLING_BINDING_SECRET` | unset | HMAC key for wallet- and intent-bound Billing obfuscation ids |
| `PURCHASE_TOKEN_KEK` | unset | Base64 encoding of a random 32-byte purchase-token wrapping key |
| `PLAY_CREDIT_PRODUCTS` | `{}` | JSON mapping of one-time product ids to granted credit amounts |
| `PUBSUB_AUDIENCE` | unset | Exact OIDC audience expected on RTDN push requests |
| `PUBSUB_SERVICE_ACCOUNT_EMAIL` | unset | Exact Pub/Sub push identity allowed on RTDN |

### Explicit local session mode

For phase-1 development only, set `$env:PLINTH_DEV_INTEGRITY = '1'` before
`npm run dev` (or set it in the ignored local `.env`). No Google credentials,
Play version, or signing certificate are required. With `NODE_ENV=production`
and `PLINTH_DEV_INTEGRITY=1`, startup throws
`PLINTH_DEV_INTEGRITY cannot be enabled in production`; it does not warn and continue.
The active mode emits this warning banner at startup:

```text
*** PLINTH DEV INTEGRITY ACTIVE: Google attestation and Device Recall are SIMULATED; no device/license verification; challenge/hash/replay/expiry and starter budget remain enforced. NOT FOR PRODUCTION. ***
```

Fetch `POST /v1/integrity/challenge` normally. Send `POST /v1/session` with
the original challenge and canonical fields; the local `integrityToken` must
be `dev:` followed by `sessionRequestHash(input)` (base64url SHA-256, no padding).
An arbitrary token is rejected. This hash is not an attestation or a secret;
the explicit environment gate is what restricts this substitute to development.
The Android debug token provider must supply this format; a production Google
token is not a local token. No Android source is changed by this server mode.

The same `StarterDeviceRecall` claim and transactional wallet/ledger/budget
path runs locally and in production. Locally only Google's services are
simulated: a false recall bit and a successful write, with existing DB grants
preventing another grant to the same install. **That local stand-in is one
grant per install, not per physical device**: reinstall, data deletion or a
new install ID can reset it. It is not production anti-reinstall protection.
The configured daily liability cap (default $10, $0.25 per ten-credit grant,
at most 40 such grants per UTC day) still applies. Use isolated, disposable
local data and do not expose this mode as a public production service.
Absent the flag, unconfigured challenge/session endpoints still return
`503 NOT_CONFIGURED`; no local fallback exists.

## API and configuration status

The shared wire contracts are in `schema/api.schema.json`; the service uses that
file for Fastify validation and the ledger/job payload contracts. The registry
is in `src/registry.ts`. O-01 recognition uses the annex E endpoint `any-llm/vision`,
is absorbed at zero wallet credits, and is enabled only when `FAL_KEY` exists.
O-02 uses the selected, price-measured route `fal-ai/bria/background/remove`,
prompt-free at $0.018. O-07 (Kontext scene edit), O-08 (IC-Light), and O-10 (Kontext recolour)
are enabled with a key. O-09 (Bria expand) validates its target canvas and source
placement, then restores the source pixels into that rectangle. O-12 (Bria eraser)
requires exactly two uploads (photo, then mask); white mask pixels erase and black
pixels must remain byte-identical to the photo. Both operations fail closed if
their fidelity guard cannot prove the result. O-14 needs separate `human_image_url`,
`garment_image_url`, and `description` and remains disabled.
O-11 remains disabled for its price ceiling; O-13 and O-15 have no route.
With no `FAL_KEY`, every model operation is reported disabled.

Health, public config, wallet reads, uploads, O-01 recognition (`POST /v1/recognize`,
one raw 1024 px JPEG with `X-Client-Sha256`), job submission/status/cancel,
result delivery/receipt, reports/status, recovery-code generation, and data
deletion are implemented. Play Integrity session and purchase/RTDN calls are
enabled only when their corresponding server credentials and expected app
identity settings exist. Session requests include the original `challenge` so
the server can verify its stored hash; the request hash binds the annex fields
and locale in a fixed newline-delimited order. `installId` is stored only as a
correlation hash and is never treated as anti-reinstall proof. The design requires
Google Play Integrity Device Recall, not an install-ID fallback
(`openspec/changes/app-seller-photo-studio/design.md`, D18, and annex D protocol
details; M0 S4 records that live availability was not established).
`PlayRecallIntegrityVerifier` decodes each token once, including
`deviceIntegrity.deviceRecall.values.bitFirst`, using the same
`PLAY_SERVICE_ACCOUNT_JSON` credential as the existing Google Play client.
After exact identity/hash checks and `PLAY_RECOGNIZED`, `LICENSED`,
`MEETS_DEVICE_INTEGRITY`, a false bit permits a call to
`POST https://playintegrity.googleapis.com/v1/{packageName}/deviceRecall:write`
with `{integrityToken, newValues:{bitFirst:true}}`. Only an acknowledged write
permits the database grant; true/missing bits and failed writes grant zero.
Other recall bits are never modified.

The independent integrity transport is intentional: `GooglePlayClient` keeps
its OAuth/API methods private and drops recall values from its decoded result.
Calling its decode method and then decoding again for recall is not safe:
Standard token replay protection can clear verdicts on a repeated decode.
Purchase/RTDN transport is unchanged. Google certificate digests are decoded
from base64url for comparison with the configured Play Console SHA-256 hex.

The write completes before committing the wallet credit so a write failure
cannot issue free credits. A process/DB failure after the Google write can lose
the free grant; no cross-service atomic transaction is claimed. Google's recall
values can take up to 30 seconds to propagate and Standard requests require
another warmup to refresh them. The DB rechecks same-install grants and the
UTC daily budget under a write lock, including after asynchronous recall;
it cannot identify concurrent *different* installs as the same physical device.
The starter liability budget bounds this exposure; it is not spent-provider
cost and does not replace `DAILY_SPEND_CAP_USD`.

`config.starterCredits` remains zero as the conservative advertised default
until live S4 capability evidence exists; `starterGranted` and `balance` in
the session response describe the actual award. The Android `SessionBootstrap`
stores `session.balance` and does not use `config.starterCredits`.

Purchase intents return HMAC-derived obfuscated account/profile ids for
`BillingFlowParams`. Confirmations are re-read from Play, fulfilled in a durable
state transition, then consumed on the server. Purchase tokens are AES-256-GCM
envelope-encrypted until Play finalization completes. Voided-purchase polling
uses Play's authoritative API and records balance clawback plus refund debt;
debt blocks reservations and later purchase credits repay it first. RTDN push
requires Google OIDC signature, audience, issuer and service-account checks and
deduplicates message ids.

The retention worker runs even when no model provider is configured. Uploads
expire at one hour and result files/tokens at 24 hours; an undelivered job is
released if it expires without a durable receipt. Reports create per-output
moderation cases, escalated at three distinct wallet reports, and the report
owner can read its current case state. Deletion receipts prove removal from
controlled service storage only. The fal policy documents up to 30 days of
provider-side media retention; deletion stays `PENDING` while that window is
open and reports `RETENTION_UNCONFIRMED` after it has elapsed rather than
claiming a provider deletion the service cannot perform.

API-00/API-01 require `PLAY_SERVICE_ACCOUNT_JSON`, `PLAY_VERSION_CODE`, and
`PLAY_SIGNING_CERT_SHA256`. A missing/unevaluated verdict or unsupported Device
Recall never grants starter credits. Purchase/RTDN fulfillment additionally
requires `BILLING_BINDING_SECRET`, `PURCHASE_TOKEN_KEK`,
`PLAY_CREDIT_PRODUCTS`, and the service-account permissions above. With no
`FAL_KEY`, all paid model operations remain disabled; the fake provider is
test-only.

### Production integrity setup (not performed by local tests)

1. In Google Cloud Console, **APIs and services > Enable APIs and services**,
   enable **Play Integrity API** for the project's Cloud project. In Play Console,
   select Plinth, **Protected with Play > Play Integrity API > Get started/Manage >
   Link Cloud project**, and link that project. The Android Standard token
   provider needs its numeric **Project number**, shown on Google Cloud's
   project dashboard (**Project info**), not its text project ID. Supply that
   number as the Android build's `PLINTH_INTEGRITY_CLOUD_PROJECT_NUMBER`
   Gradle property/environment variable.
2. Supply the service's `PLAY_SERVICE_ACCOUNT_JSON` from that project's service
   account (**IAM & Admin > Service Accounts > [account] > Keys**), through the
   server secret store only. It must authorize the Play Integrity OAuth scope
   `https://www.googleapis.com/auth/playintegrity`. Do not paste or log the JSON.
   Account/key creation and console changes are owner/operator actions, not
   performed by this patch.
3. Set `PLAY_PACKAGE_NAME=com.gexetr.plinthproductphotos`,
   `PLAY_VERSION_CODE` to the uploaded build's exact version code (Play Console
   **Test and release > App bundle explorer**), and `PLAY_SIGNING_CERT_SHA256`
   to the **App signing key certificate > SHA-256 certificate fingerprint**
   under **Protected with Play > Play app signing** (App integrity in older
   navigation). This is the app-signing certificate, not the upload/debug key.
   These console-specific values must be supplied by the owner/operator.
4. Under **Protected with Play > Play Integrity API > Manage > Responses >
   Change responses**, turn **Device recall** on and **Save changes**. Access to
   the write API may require Google's early-access eligibility. A decoded
   false bit followed by a successful write and a fresh-warmup read of true
   must be measured for this app (S4); no live result is established here.
   Device Recall bits are shared across the developer account's apps, so bit 1
   must be reserved consistently across that account, not reused for another purpose.
5. Install the uploaded app through a Play test/release track with a licensed
   tester and an integrity-passing physical device, with current Play Store and
   Play services. Keep `PLINTH_DEV_INTEGRITY` unset/`0` and set `NODE_ENV=production`.
   Missing recall support still permits a verified session with zero starter credits.

API details: <https://developer.android.com/google/play/integrity/device-recall>,
<https://developer.android.com/google/play/integrity/setup>, and
<https://playintegrity.googleapis.com/$discovery/rest?version=v1>.

Run the service without setting `FAL_KEY` for health checks and local development.
The real fal adapter is exercised with mocked fetch responses; no test performs
a provider network call.

### fal transport contract

`src/providers/fal.ts::falQueueResult` submits once to `queue.fal.run`, validates
the acknowledgement's request id and fal-hosted tracking URLs, polls `status_url`
until `COMPLETED`, then reads `response_url`. Submission acknowledgements are
never model output. HTTP errors at any stage reject the adapter; the existing
`JobQueue` failure path releases the reservation. There are no automatic paid
resubmissions. Polling is bounded to 150 seconds and output download to 20 seconds,
below the queue's 180-second interrupted-job recovery threshold. This bounds a
local invocation, not provider execution: a timed-out remote inference can still
finish and incur provider cost. Request ids are not persisted for restart recovery.
Queue transport was chosen over synchronous `fal.run` because submission,
execution, and result retrieval have separate HTTP lifetimes.

Schemas were read from fal's own
`https://fal.ai/api/openapi/queue/openapi.json?endpoint_id=<endpoint>` on
2026-10-16: background removal, expansion, and erasing return `image`;
FLUX dev, IC-Light v2, and Kontext return `images[]`. The current idm-vton schema
also returns singular `image` and `mask` (not `images[]`). Vision takes
`image_urls` and returns string `output`, with optional `partial` and `error`;
the recognition schema is included in the prompt because this endpoint does not
declare `response_format`. Invalid JSON is rejected, and the route still validates
the parsed recognition schema.

W73 replaces O-07's text-only FLUX dev with the existing `fal-ai/flux-pro/kontext`
image-editing route ($0.04/output, still two wallet credits). Every enabled image
tool requires exactly one upload and binds its bytes to `image_url`; there is no
library-image fallback. Relight, recolour, scene and the currently disabled expand
adapter prepend server-owned tool/identity constraints; `params.prompt` is optional
guidance only. These endpoints expose a single `prompt`, not a separate system role,
so the policy is composed into that supported field. Cut-out stays prompt-free on
its dedicated background-removal endpoint. Expand still requires canvas dimensions
and remains disabled; W73 does not enable incomplete tools. See
`../docs/redesign/studio-tool-binding-w73.md` for the trace and evidence limits.

Generated/relit/edited outputs are labelled
`ENHANCED`, never `ORIGINAL_PIXELS`; only prompt-free cut-out keeps the latter
label. Output dimensions and MIME format are read from the actual downloaded
image rather than reported as zero/octet-stream.

`test/fal-provider.test.ts` covers the queue lifecycle, endpoint-specific output
fixtures (including currently disabled models), invalid acknowledgements,
credential isolation, bounded polling, input validation, and reservation release
after a real-adapter failure. Live calls require separate spending authorization;
unit fixture coverage is not evidence of real-provider completion.

## Deployment

The measured server is Ubuntu behind the existing Caddy site at `pikupo.com`.
Plinth must remain a dedicated path route; do not replace the site or its SPA
fallback. The intended public API prefix is `https://pikupo.com/plinth/v1/`,
and the service health check is `https://pikupo.com/plinth/health`. Caddy strips
only `/plinth` and proxies to the loopback listener at `127.0.0.1:3101`, so
`/plinth/health` reaches this server's JSON handler instead of the site's
fallback.

Deployment procedure:

1. Build and verify in this directory with `npm ci`, `npm test`,
   `npx tsc --noEmit`, and `npm run build`. Do not put production credentials
   in the build tree.
2. Copy the built `dist/`, `migrations/`, `schema/`, `package.json`, and lockfile
   to a new versioned directory under `/opt/plinth-product-photos/releases/`.
   Install production dependencies there, then atomically repoint
   `/opt/plinth-product-photos/current` to that version.
3. Install/enable the dedicated systemd unit
   `plinth-product-photos.service` as the unprivileged `plinth` user. Its
   working directory is `/opt/plinth-product-photos/current/server`, command
   is `node --env-file=/etc/plinth-product-photos/server.env dist/src/index.js`,
   listener is `127.0.0.1:3101`, and persistent files/database belong under
   `/var/lib/plinth-product-photos/`. Keep `/etc/plinth-product-photos/server.env`
   root-readable and service-readable only; it is not part of the release.
4. Back up `/etc/caddy/Caddyfile` to a timestamped sibling. Add this handler
   inside the existing `pikupo.com` site block, **before** its SPA fallback:

   ```caddyfile
   handle_path /plinth/* {
       reverse_proxy 127.0.0.1:3101
   }
   ```

5. Run `sudo caddy validate --config /etc/caddy/Caddyfile`, reload Caddy, and
   restart the service. Confirm `systemctl is-active
   plinth-product-photos.service` and `journalctl -u
   plinth-product-photos.service` without printing environment values.
6. Probe `https://pikupo.com/plinth/health`; accept only JSON with
   `service=plinth-product-photos-server` and `databaseReachable=true`. A 200
   HTML response is the site's SPA fallback, not a healthy Plinth service.
7. Roll back by restoring the timestamped Caddy backup and reloading Caddy,
   atomically repointing `current` to the prior release, then restarting the
   service. Keep the failed release and its journal output for diagnosis.

The fal key is optional for health-only operation. Never invent or copy one
from another service: if absent from the service environment, leave paid
operations disabled until the owner supplies it.
