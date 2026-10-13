# K — Package layout, dependencies, tests and worker partition

Slot root after scaffold: `apps/<date>-plinth-product-photos/` (`applicationId`
`com.gexetr.plinthproductphotos`). Client Kotlin package root: `com.gexetr.plinthproductphotos`; below it
`<pkg>` means that root. Server lives at `apps/<slot>/server/`.

## Client layout (`app/src/main/`)

```
java/<pkg>/
  MainActivity.kt, App.kt                      # edge-to-edge, predictive back, Navigation 3 host
  nav/        Destinations.kt, AppNav.kt       # S-01..S-18 routes (annex F)
  domain/
    model/    Provenance.kt SlotRole.kt Item.kt Output.kt Look.kt BackgroundSpec.kt ShadowSpec.kt JobState.kt   # annex C
    pipeline/ Compositor.kt Mask.kt MaskRefiner.kt Fill.kt Shadow.kt SceneProcedural.kt Pipeline.kt            # annex E
    linter/   Rules.kt Linter.kt FixActions.kt                                                                # annex L
    presets/  MarketplacePreset.kt PresetRepository.kt PresetStaleness.kt                                    # annex B
    slides/   Slide.kt TemplateEngine.kt ProhibitedText.kt Contrast.kt                                        # annex I
    export/   Exporter.kt FileNaming.kt XmpWriter.kt ZipWriter.kt Manifest.kt                                 # annex H
    group/    Grouping.kt SkuPattern.kt
    coach/    ShotLists.kt FrameQuality.kt Level.kt
    text/     ListingTextRequest.kt Limits.kt
  data/
    db/       AppDatabase.kt Entities.kt Daos.kt                                                              # annex C
    files/    FileStore.kt Sources.kt Outputs.kt
    prefs/    SettingsStore.kt WalletStore.kt
    net/      ApiClient.kt ApiModels.kt Auth.kt JobPoller.kt                                                  # annex D
    billing/  BillingClientWrapper.kt PurchaseCoordinator.kt                                                  # capabilities/billing
    work/     BatchWorker.kt ResumeScanner.kt Notifier.kt
  ui/
    theme/    Color.kt Type.kt Shape.kt Theme.kt
    components/ PlinthStage.kt ProofRing.kt ItemTile.kt StatusPill.kt SectionStrip.kt CostLine.kt EmptyState.kt MarkGlyph.kt
    screens/  one package per S-nn (annex F), each with Screen.kt and ViewModel.kt
res/  values/ and values-ru/ values-de/ values-fr/ values-es/ values-pt-rBR/ ; drawable/ ; xml/ (backup, data extraction, locales_config)
assets/ marketplaces.json slide-templates.json prohibited-text.json scene-recipes.json categories.json fonts/
```

## Dependencies (each needs a reason; versions are the latest stable read from release notes at scaffold
time through the version catalog; none is a beta/alpha — `dependency-freshness.mdc`)

| Dependency | Reason |
|---|---|
| Compose BOM 2026.09.00, Material 3, Navigation 3 | UI, back stack (factory toolchain) |
| Room + KSP | library, jobs, items (no `kapt`) |
| DataStore Preferences | settings, wallet cache |
| WorkManager | batch and resume, constraints (Wi-Fi only, charging) |
| CameraX (core, camera2, lifecycle, view) | shoot coach |
| `kotlinx-serialization-json` | API models, presets, templates |
| OkHttp | API client with pinned timeouts, multipart upload (stable, mainstream) |
| Coil (compose) | thumbnails and large preview decoding |
| `com.android.billingclient:billing-ktx` (latest stable; 9.1.0 listed at 2026-06-18) | purchases; with the Fragment constraint at or above `scripts/lib/play-dependency-floors.json` |
| ML Kit text recognition and barcode scanning (Google Play services variants; S3 decides) | on-device OCR and QR detection for linter and SKU reading; no beta artifacts |
| a vendored licence-cleared icon set | UI glyphs (`icons/provenance.json`) |
| bundled open-licence fonts (Latin + Cyrillic) | slide text (`docs/font-provenance.md`) |

Banned here: any provider SDK (fal, OpenAI, Anthropic, Stripe …), any key in `app/src/main`.

## Server layout (`server/`)

```
package.json tsconfig.json .env.example (placeholders only) Caddyfile.example deploy/ (systemd unit, deploy script)
src/ index.ts config.ts db.ts auth.ts ledger.ts purchases.ts jobs.ts providers/{provider.ts,fal.ts,fake.ts}
     guard/{metrics.ts,guard.ts} compose/{validate.ts} routes/*.ts sweep.ts spend.ts
test/ *.test.ts  fixtures/
```

Stack: Node LTS + TypeScript + Fastify + better-sqlite3 + sharp + undici; tests with `node:test` or
`vitest`, whichever is the latest stable at scaffold time (record the choice).

## Tests (named, each asserts a thing the specs state)

Client unit tests (`app/src/test/`):
`CompositorInvariantTest` (core bytes identical at scale 1; one resample otherwise; fill ratios 0.80±0.02),
`FillAndMarginTest`, `MaskRefinerUndoTest`, `ShadowTest`, `LinterRulesTest` (every L-rule pass and fail
with the annex fixtures, e.g. 699×933 fails / 700×933 passes), `PresetSourceTest` (every entry has source
and `verifiedAt`; fails on a copy without), `PresetStalenessTest`, `FixActionsTest`, `FileNamingTest`,
`XmpWriterTest` (round trip, merge, decode still works), `ZipWriterTest`, `ManifestTest`,
`GroupingTest`, `SkuPatternTest`, `ShotListsTest`, `FrameQualityTest`, `LimitsTest`,
`SlideKitTest` (annex I), `LedgerClientTest` (CostLine shown before charge, idempotency keys),
`NoSubscriptionTest` (only `INAPP` queried), `BatchResumeTest`, `RetryFailedOnlyTest`,
`SettingsActTest` (each setting changes a behaviour), `LocalizationParityTest` (all six locales have every
key; the six `values-*` dirs exist), `ProofRingStateTest`, `BackupRulesTest` (wallet excluded, Looks
included).

Server tests: `ledger.test` (insufficient balance refused before any provider call; concurrent spends
cannot overspend; release idempotent), `purchases.test` (forged token refused; replay credits once; voided
purchase claws back; unknown product refused), `session.test` (reinstall does not refill; integrity failure
caps starter credits), `jobs.test` (concurrency bound; timeout releases; cancel releases),
`guard.test` (drift rejected and refunded; faithful delivered and charged once; changed label rejected;
identity control passes; swap control rejected), `spend.test` (daily cap refusal, emergency switch),
`retention.test` (expiry sweep deletes), `reports.test`, `delete.test`, `noimagelog.test` (no image bytes
or URLs in the log), `contract.test` (client models vs server schemas generated from one JSON schema file
`server/schema/api.schema.json`, which both sides import).

Device run (task 8.4) on `R58MB3SGZVL` follows annex F first-run journey plus: purchase of test credits,
batch of 6, coach in airplane mode, forced Enhanced failure shows refund, Original export file hash equals
the interior pixel hash recorded by the pipeline.

## Worker partition (disjoint file lists; Gradle is never run by workers)

| Worker | Owns | Needs from others |
|---|---|---|
| W-server | `server/**` | annex D, E (guard, registry), C (DDL) |
| W-domain | `app/src/main/java/<pkg>/domain/**` and its tests | annex C, E, L, B, I, H |
| W-data | `data/**`, `nav/**`, `capabilities/billing` wiring in `app/build.gradle.kts` | annex C, D |
| W-ui | `ui/**` | annex F, J; domain interfaces as written in W-domain's `model/` |
| W-assets | `res/**`, `assets/**`, `docs/**` of the slot, `icons/**`, `store/studio.json` | annex F strings list (derived by W-ui), I, J, B |
| W-tests | `app/src/test/**` that no other worker owns, `server/test/**` additions | all |

Interfaces are agreed **through files**: W-domain commits `domain/model/*.kt` first (they are annex C
transcribed), W-data and W-ui read them from disk. The orchestrator runs the single Gradle invocation after
the writers finish: `node scripts/verify.mjs --slot <slug> --build`.
