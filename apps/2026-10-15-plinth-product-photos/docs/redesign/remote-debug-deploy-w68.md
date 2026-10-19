# W68 remote DEBUG deployment — 2026-10-17

## Result

`plinth-product-photos-debug.service` now runs the separate release
`/opt/plinth-product-photos/releases/20261017-160849` through the dedicated
`/opt/plinth-product-photos/current-debug` symlink.  Production was left on
`/opt/plinth-product-photos/releases/20261015-211618` through its unchanged
`current` symlink.

The debug-only systemd drop-in is
`/etc/systemd/system/plinth-product-photos-debug.service.d/10-current-debug.conf`:

```ini
[Service]
WorkingDirectory=/opt/plinth-product-photos/current-debug
```

No production environment file was changed. In particular, this deployment did
not enable `PLINTH_DEV_INTEGRITY` for `plinth-product-photos.service`.

## Build and release evidence

- **MEASURED** — From `server/`, `npm ci --cache
  D:\Python\PlayKiln\.kiln\scratch\w68-remote-debug\npm-cache`, `npm run build`, and
  `npm test` exited 0. Build copied migrations `001`–`003`; Vitest reported
  12 passing files and 140 passing tests.
- **MEASURED** — The staged archive SHA-256 was
  `1AF83E42E675913A2CAB1AD7D5A8C9415EAE56F54FBA405A395791CA9075C61F`.
  It contained the built `dist/`, `schema/`, `migrations/`, `package.json`,
  `package-lock.json`, and `README.md`.
- **MEASURED** — On the VPS, `npm ci --omit=dev --ignore-scripts=false` exited
  0 in the new release. Presence checks for `dist/src/device-recall.js`,
  `node_modules`, `schema/api.schema.json`, and
  `migrations/002_integrity_purchase_deletion.sql` all passed.
- **MEASURED** — `systemctl show` reported debug
  `WorkingDirectory=/opt/plinth-product-photos/current-debug`, `ActiveState=active`,
  and `SubState=running`. The production service reported `active` and retained
  its original `current` target.

## Remote verification from this machine

- **MEASURED** — `curl.exe https://pikupo.com/plinth-debug/health` returned
  HTTP 200 with `ok=true` and `databaseReachable=true`.
- **MEASURED** — `POST https://pikupo.com/plinth-debug/v1/integrity/challenge`
  with `{ "installId": "w68-remote-debug-proof-20261017b", "appVersion": "1",
  "locale": "en-US" }` returned HTTP 200 with `challengeId`, `challenge`, and
  `expiresAt`; `NOT_CONFIGURED` was absent.
- **MEASURED** — `curl.exe https://pikupo.com/plinth/health` returned HTTP 200
  with `ok=true` after the debug restart.
- **MEASURED** — `journalctl -u plinth-product-photos-debug.service -n 40`
  contained the `PLINTH DEV INTEGRITY ACTIVE` banner at 2026-10-17 13:10:02
  (VPS journal time), confirming that the debug process selected the DEV
  Device Recall/integrity verifier.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| The deployed debug tree includes Device Recall code. | MEASURED | VPS `test -f dist/src/device-recall.js` exited 0. |
| Debug is isolated from production release selection. | MEASURED | `readlink -f current-debug` → `20261017-160849`; `readlink -f current` → `20261015-211618`. |
| DEV integrity is active only in the debug process. | MEASURED | Debug journal banner plus the debug-only systemd drop-in; production service target remained unchanged. |
| Debug challenge is configured remotely. | MEASURED | Fresh HTTPS POST returned 200 and challenge fields, not 503 `NOT_CONFIGURED`. |
| Production remained available after the debug deploy. | MEASURED | Fresh `https://pikupo.com/plinth/health` response: HTTP 200 and `ok=true`. |
