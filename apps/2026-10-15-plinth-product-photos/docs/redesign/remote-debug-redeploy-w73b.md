# W73b remote DEBUG redeploy — 2026-10-17

## Release

- **Release id:** `20261017-192724`
- **Debug release target:** `/opt/plinth-product-photos/releases/20261017-192724`
- **Deployed bundle SHA-256:** `fe409189f47cf490713184c1c3a03ab111d41c31736df89a99adb13ad83ef34d`
- **Scope:** only `/opt/plinth-product-photos/current-debug` and
  `plinth-product-photos-debug` were switched/restarted. The production
  `/opt/plinth-product-photos/current` symlink was not changed.

## Build and release evidence

- **MEASURED** — From `server/`, after a clean `npm ci`, `npm test`,
  `npx tsc --noEmit`, and `npm run build` all exited 0. Vitest reported
  **14 passed test files and 187 passed tests**; the build copied migrations
  `001`–`003`.
- **MEASURED** — The local release archive SHA-256 was
  `fe409189f47cf490713184c1c3a03ab111d41c31736df89a99adb13ad83ef34d`.
  The VPS computed the same checksum before extraction.
- **MEASURED** — On the VPS, `npm ci --omit=dev --ignore-scripts=false`
  completed in release `20261017-192724`; presence checks for
  `dist/src/registry.js` and `dist/src/providers/fal.js` passed before the
  debug symlink switch.
- **MEASURED** — After `systemctl restart plinth-product-photos-debug`,
  systemd reported `ActiveState=active`, `SubState=running`, and
  `WorkingDirectory=/opt/plinth-product-photos/current-debug`.

## W73 server prompt content

- **MEASURED** — The built registry and fal adapter at
  `dist/src/registry.js` and `dist/src/providers/fal.js` were packaged and
  deployed. Their remote SHA-256 values were respectively
  `a6051e887d8d61e80309a0543b13f8ad402eb5e7784bae482df645a03d490061` and
  `02caae8fc0a5cf7649c85c99b46b569d42c99e8b00b68519944b24963aca4572`.
  Local source inspection: `server/src/registry.ts:50-54` selects
  `fal-ai/flux-pro/kontext` for scene operation `O-07`; the server-owned
  tool prompt table is in `server/src/providers/fal.ts:138-146`.

## Fresh remote smoke

- **MEASURED** — `GET https://pikupo.com/plinth-debug/health` returned HTTP
  200 with `ok=true` and `databaseReachable=true`.
- **MEASURED** — `POST
  https://pikupo.com/plinth-debug/v1/integrity/challenge` with install id
  `w73b-smoke-20261017192835` returned HTTP 200 and non-empty `challengeId`,
  `challenge`, and `expiresAt` fields.
- **MEASURED** — The debug journal emitted the `PLINTH DEV INTEGRITY ACTIVE`
  banner at `Oct 17 16:28:03` (VPS journal time) after the restart.
- **MEASURED** — Production remained on
  `/opt/plinth-product-photos/releases/20261015-211618`; its service stayed
  `active`, and `GET https://pikupo.com/plinth/health` returned HTTP 200 with
  `databaseReachable=true`.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Debug serves the rebuilt W73b server release. | MEASURED | VPS `readlink -f /opt/plinth-product-photos/current-debug` → release `20261017-192724`; `systemctl show` returned active/running. |
| The debug release contains the scene-kontext registry and fal prompt adapter. | MEASURED | VPS presence checks plus remote SHA-256 for `dist/src/registry.js` and `dist/src/providers/fal.js`; source locations above identify the W73 behavior. |
| Debug integrity challenge is usable. | MEASURED | Fresh native HTTP POST returned 200 and the three expected non-empty fields. |
| Production `/plinth/v1` was not switched. | MEASURED | VPS `readlink -f /opt/plinth-product-photos/current` remained release `20261015-211618`; production health returned 200. |
