# W67/W69 remote debug API review

**Status:** BLOCKED before build and installation. The debug API health route is
reachable, but the integrity challenge still returns `503 NOT_CONFIGURED`.

## Client configuration

MEASURED: the debug variant defaults to
`https://pikupo.com/plinth-debug/v1`, while the release variant retains
`https://pikupo.com/plinth/v1`
(`apps/2026-10-15-plinth-product-photos/app/build.gradle.kts:37,106`).
The English and Russian debug wallet messages both identify the remote
`pikupo.com/plinth-debug` API
(`app/src/main/res/values/strings_session.xml:8`,
`app/src/main/res/values-ru/strings_session.xml:8`).
The phase-1 README describes remote debug sessions and says no local server or
`adb reverse` is required (`apps/2026-10-15-plinth-product-photos/README.md:22-24`).

## Deployment gate

MEASURED at `2026-10-17T16:05:02+03:00`: `GET
https://pikupo.com/plinth-debug/v1/health` returned HTTP 200 with
`{"ok":true,"service":"plinth-product-photos-server","version":"1.0.0","databaseReachable":true}`.
The correctly formed `POST
https://pikupo.com/plinth-debug/v1/integrity/challenge` returned HTTP 503:
`NOT_CONFIGURED` / “Play Integrity verification is not configured.”
Earlier polling samples from 15:57:12 through 16:04:08 +03:00 returned the same
503 challenge response.

W68 finished `DONE` as a worker task but reported its deployment itself blocked:
the staged `npm ci` failed with `EACCES`, and SSH could not read the required key
or connect to the VPS. Its report says no remote state changed:
`apps/2026-10-15-plinth-product-photos/docs/redesign/remote-debug-deploy-w68.md`.
The live challenge probe independently confirms the debug API is not configured.

MEASURED: the A40 was available as serial `R58MB3SGZVL`;
`adb -s R58MB3SGZVL shell getprop ro.product.model` returned `SM-A405FM`.
Because the required challenge remained 503 after the approximately ten-minute
wait, the debug APK was not built or installed and no on-device UI or
device-verification record was updated. No local server was started and no
`adb reverse` was used.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Debug defaults to the remote debug API and release retains the production API URL. | MEASURED | `apps/2026-10-15-plinth-product-photos/app/build.gradle.kts:37,106` |
| English and Russian debug network messages name the remote API. | MEASURED | `app/src/main/res/values/strings_session.xml:8`; `app/src/main/res/values-ru/strings_session.xml:8` |
| The documented phase-1 path needs neither a local server nor `adb reverse`. | MEASURED | `apps/2026-10-15-plinth-product-photos/README.md:22-24` |
| Health is reachable, but challenge creation is not configured. | MEASURED | `Invoke-WebRequest` GET health and POST challenge at `2026-10-17T16:05:02+03:00`: HTTP 200 and HTTP 503 `NOT_CONFIGURED` |
| W68 could not deploy from its worker environment. | MEASURED | `apps/2026-10-15-plinth-product-photos/docs/redesign/remote-debug-deploy-w68.md` |
| The attached review device is the requested A40. | MEASURED | `adb -s R58MB3SGZVL shell getprop ro.product.model` returned `SM-A405FM` |
| Studio and Credits state in the newly installed build. | UNVERIFIED | No install/build was attempted after the challenge gate remained 503; retry after W68 deployment, then inspect the app on `R58MB3SGZVL`. |

**Elapsed time:** approximately 9 minutes from the W69 request timestamp
(`15:56:01+03:00`) to the final challenge probe at `16:05:02+03:00`.
