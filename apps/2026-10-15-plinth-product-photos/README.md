# Plinth AI: Product Photos

**Slug:** `plinth-product-photos`  
**applicationId:** `com.gexetr.plinthproductphotos`  
**Idea:** Turn a seller's own product photos into per-marketplace listing cards: server cut-out, original-pixel composition, marketplace rule linter, export with provenance.

PlayKiln slot — isolated Android attempt. Factory harness lives at the repo root.

## Build

```bash
# from this directory (JDK 17+ and Android SDK required)
./gradlew assembleDebug
./gradlew bundleRelease
```

Windows: `.\gradlew.bat assembleDebug`

## Phase-1 debug sessions (remote)

Debug builds use a simulated integrity token (`dev:<requestHash>`) and talk to the
**remote debug API** at `https://pikupo.com/plinth-debug/v1` (systemd unit
`plinth-product-photos-debug` on the VPS, `PLINTH_DEV_INTEGRITY=1`). No local
server and no `adb reverse` are required for normal review.

Override only when needed with `-PPLINTH_DEBUG_API_BASE_URL=<url>` or
`PLINTH_DEBUG_API_BASE_URL`. HTTP is permitted only for `127.0.0.1` /
`localhost`; everything else must be HTTPS.

Marketplace-pack preparation logs failures with tag `PlinthExport`, including
the preparation step and item IDs (or marketplace ID during rendering). Inspect
them with `adb logcat -s PlinthExport:E`. Imported photos with unset order zero
export as photo 1; existing positive orders are preserved. Original-only and
limited-overlay presets receive source pixels without the saved Look, rather
than bypassing their editing-policy checks.

Release builds always use `https://pikupo.com/plinth/v1` and the real Play
Integrity provider. The dev provider is guarded by `BuildConfig.DEBUG` both
at activity selection and at construction; it is not a release attestation
fallback. The loopback cleartext policy exists only in the debug source set.

If the Gradle wrapper JAR is missing, from a machine with Gradle installed:

```bash
gradle wrapper --gradle-version 8.11.1
```

## Store pack

See `store/` for Play listing drafts.
