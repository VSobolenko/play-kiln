# M0 S4 — Plinth server walking skeleton

Observed 2026-10-15. Scope: server-side integrity/session checks, Play one-time
purchase fulfilment, ownership, retention, reporting, deletion receipts, and
deployment readiness. The local server implementation and its offline checks
are evidenced below; production deployment is **blocked**, not complete.

## Implemented boundary

- API-00/API-01 consume a stored, expiring, single-use challenge and verify the
  request hash, package, version, signing certificate, Play recognition,
  licensing, and device verdict server-side. `installId` is only a correlation
  hash. Device Recall has no configured implementation, so this server grants
  no starter credits.
- Purchase confirmation is checked against Play before wallet credit is
  applied. The Play Publisher API discovery schema was inspected while fixing
  the adapter: `ProductPurchaseV2` has no quantity or consumption-state field;
  the package-scoped `purchases.products.get` response (`ProductPurchase`)
  carries `quantity`, `consumptionState`, and `purchaseState`. The adapter uses
  that endpoint, binds the request to the configured package path, and checks
  an explicitly returned product id when present. Fulfilment is idempotent;
  purchase tokens remain envelope-encrypted only while server-side
  consumption is pending. RTDN is authenticated and deduplicated, and a
  scheduled authoritative voided-purchase sweep claws back credits into
  refund debt.
- Reported generated outputs are linked to moderation cases by result
  fingerprint; three distinct wallet reports escalate a case and report
  owners can read its state. Upload and result expiration is swept, including
  when the paid provider is not configured. Deletion receipts describe
  controlled-system deletion separately from the fal retention window; they
  do not claim an external deletion the service cannot perform.
- `server/README.md` documents the intended dedicated Caddy route,
  `systemd` unit, storage and log locations, health check, and rollback. No
  VPS/Caddy/service changes were made: the deployment SSH invocation was
  blocked by the non-interactive tool permission boundary. The public
  `/plinth/health` path still returns the existing site's HTML SPA response,
  not Plinth health JSON.
- The worker process environment reported `FAL_KEY=UNSET` and
  `PLAY_SERVICE_ACCOUNT_JSON=UNSET`. This is **not** a reading of the VPS
  service environment; remote provider-key presence remains unknown. No paid
  provider call was made, and tests use fake providers.

## Verification observations

The full server test run passed 37 tests in six files. `npx tsc --noEmit`
completed without diagnostics (exit 0). The client-secrets scan passed for
172 shipped-client/build files. The deployed health probe returned HTTP 200
with `Content-Type: text/html; charset=utf-8` and an HTML document beginning
`<!DOCTYPE html>`; it is therefore not a health pass. SSH deployment remains
blocked as described above.

## Claim ledger

CLAIM LABEL EVIDENCE
The server suite passes all 37 tests in six files. MEASURED `npm test` — `Test Files 6 passed (6); Tests 37 passed (37); Duration 2.93s`.
The server TypeScript sources and tests type-check. MEASURED `npx tsc --noEmit` — exit 0, no diagnostics.
The shipped Android client has no detected client-side paid-provider secret. MEASURED `node scripts/check-client-secrets.mjs --slot plinth-product-photos` — `PASS scanned 172 client file(s); VERDICT: PASS`.
The public Plinth path is not deployed and currently resolves to the site's SPA. MEASURED `curl.exe -sS -D - --max-time 20 https://pikupo.com/plinth/health` — `HTTP/1.1 200 OK`, `Content-Type: text/html; charset=utf-8`, 35,236 bytes, body begins `<!DOCTYPE html>`.
Deployment could not be attempted through the configured SSH identity in this non-interactive run. MEASURED `ssh -o BatchMode=yes -o ConnectTimeout=8 main true` — tool denied execution because interactive approval was unavailable.
The worker process has no FAL or Play service-account credential. MEASURED environment presence check — `FAL_KEY=UNSET`; `PLAY_SERVICE_ACCOUNT_JSON=UNSET`; values were not printed.
The VPS service's FAL credential presence is unknown. UNVERIFIED Requires authorized SSH access to inspect the service manager's environment without printing its values.
The intended route is path-scoped and preserves the existing site. ON RECORD `server/README.md` §Deployment; host and TLS observations are in `M0-S0-host-and-tls.json`.
The fal provider documents media retention up to 30 days; this implementation does not claim provider-side deletion. ON RECORD `server/README.md` §API and configuration status; `M0-S6-play-policy.md:23`.
The Play v2 product response schema omits quantity and consumption state, while the legacy product response exposes them. MEASURED `Invoke-RestMethod -Uri 'https://androidpublisher.googleapis.com/$discovery/rest?version=v3'` — `ProductPurchaseV2` properties omit both; `ProductPurchase` includes `quantity`, `consumptionState`, and `purchaseState`.
