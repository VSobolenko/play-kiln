# W67 remote debug API client and device review

**Status:** COMPLETE — after repeated `503 NOT_CONFIGURED` responses, the valid remote integrity challenge returned `200` at 16:10:49 +03:00. The debug APK was built, installed on the specified A40, and launched.

## Client configuration

- `app/build.gradle.kts` defaults debug builds to `https://pikupo.com/plinth-debug/v1`; HTTP is restricted to an explicit loopback override.
- Release builds remain pointed at `https://pikupo.com/plinth/v1`.
- `session_network_debug` in both `values/strings_session.xml` and `values-ru/strings_session.xml` identifies the remote debug API. Neither message refers to localhost.
- The phase-1 section of `README.md` documents the remote debug API and says no local server or `adb reverse` is needed.

## Remote readiness gate

The target phone is `R58MB3SGZVL` (`SM-A405FM`), and ADB reported it in `device` state. Health returned `200` with `databaseReachable: true`. A valid `POST /plinth-debug/v1/integrity/challenge` request (`installId=w69-remote-debug-probe`, `appVersion=1.0.0`, `locale=en-US`) returned `503 NOT_CONFIGURED` from 16:01:39 through 16:09:49. The tenth poll returned a valid challenge with status `200` at 16:10:49; only then did the build and install proceed.

The W68 deployment record at `docs/redesign/remote-debug-deploy-w68.md` reports that staging dependency installation and SSH access were denied before any VPS changes could be made. This worker did not start a local server or use `adb reverse`.

## Build and device observation

`assembleDebug` passed through `scripts/lib/gradle.mjs` with JDK 17. An initial direct helper call supplied a relative slot path and failed before Gradle could start; the corrected invocation used `path.resolve(...)` and succeeded in 6,389 ms. The APK was installed with `adb -s R58MB3SGZVL install -r` after rechecking model `SM-A405FM`. Its SHA-256 and the installed `base.apk` SHA-256 both equal `110963524aeabc93493a8d8f3a51e12af7d1845be33c45ec76c1d88b97d7aac2`.

The first Studio view showed the “Get credits” action. Opening the wallet initially showed “Balance unavailable” and the offline guidance, with no localhost text; a wallet refresh did not change it. After relaunch, Studio rendered `Credits · 10`. The final screenshot is `.kiln/scratch/plinth-a40-w69-20261017/studio-final.png`; the initial view and wallet state are recorded in the same scratch directory. No purchase was attempted.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Debug defaults to the remote debug API and release still targets the production API. | MEASURED | `app/build.gradle.kts:35-48, 101-106` |
| English and Russian network soft-gate strings name the remote API, not localhost. | MEASURED | `app/src/main/res/values/strings_session.xml:8`; `app/src/main/res/values-ru/strings_session.xml:8` |
| Phase-1 README documents remote debug without a local server or `adb reverse`. | MEASURED | `README.md:22-28` |
| The target phone is the specified SM-A405FM and is connected over ADB. | MEASURED | `adb -s R58MB3SGZVL shell getprop ro.product.model`; `adb -s R58MB3SGZVL get-state` |
| The schema-valid challenge was repeatedly 503 and then returned 200 at 16:10:49. | MEASURED | Ten-minute `Invoke-WebRequest` poll; final response contained a challenge and expiration |
| `assembleDebug` passed under JDK 17 in 6,389 ms. | MEASURED | `runGradleTasks(path.resolve('apps', '2026-10-15-plinth-product-photos'), ['assembleDebug'])`, exit 0 |
| The requested package was installed on serial `R58MB3SGZVL`, model `SM-A405FM`, and its installed APK matches the build SHA-256. | MEASURED | `adb -s R58MB3SGZVL install -r`; pulled installed `base.apk`; compared SHA-256 |
| Studio displayed `Credits · 10` after relaunch. | MEASURED | `.kiln/scratch/plinth-a40-w69-20261017/studio-final.png`, visually inspected |
| Wallet briefly displayed `Balance unavailable` and offline guidance before the relaunch; no localhost copy was visible. | MEASURED | `.kiln/scratch/plinth-a40-w69-20261017/wallet.png` and `wallet-refreshed.png`, visually inspected |
| W68 did not modify VPS state. | INFERRED | `docs/redesign/remote-debug-deploy-w68.md` says staging and SSH failed before remote commands could run |
