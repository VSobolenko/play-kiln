# Tasks: app-seller-photo-studio

One build policy: Gradle is serial per slot (`.cursor/rules/build-speed.mdc`); workers never run it, the
orchestrator runs one invocation. Temporary files go to `.kiln/scratch/` only. Slot-specific documents go
to `apps/<slot>/docs/` after scaffold (`.cursor/rules/slot-docs.mdc`). Phase 2 items are blocked until the
owner's recorded approval (`.cursor/rules/release-gate.mdc`).

**Implementation reference:** `annex/README.md`. Every task below cites the annex file that defines the
numbers, schemas and edge cases. Do not invent a value that the annex already states. Spikes that decide
UNVERIFIED values are `annex/M-spikes-and-reads.md`. Name/slug decided: `plinth-product-photos` /
`com.gexetr.plinthproductphotos` (annex J).

## 1. Contracts (before scaffold; harness-side authoring)

- [x] 1.1 Write `docs/research/2026-10-12-seller-platform-evidence.md` with the competitor, complaint, marketplace-rule and price evidence, each claim labelled MEASURED / ON RECORD / INFERRED / UNVERIFIED, and verify every `evidenceRef` below resolves to it (`rg` finds the section anchors).
- [x] 1.2 Write `docs/ideation/plans/plinth-product-photos.economics.json` (`purchase_led_repeat_sales`, packs and unlock, kill criteria `windowDays 90`, `minInstalls 1000`, `minRevenueUsd 200`) and verify `node scripts/check-economics.mjs --economics <path>` prints no BLOCKED. Detail: annex J.
- [x] 1.3 Write `docs/ideation/plans/plinth-product-photos.differentiators.json` with at most 15 promises, all `deferred`, each with `jobStage` and `depthFamily`, `jobCoverage` for before, during and after, and at least one connective claim; verify `node scripts/check-differentiators.mjs --differentiators <path> --allow-deferred` passes its schema checks.
- [x] 1.4 Derive the name: write `docs/ideation/plans/plinth-product-photos.naming.json` from at least five candidates in at least three archetypes, with one rejected alternative, a Play title scan, the 30-character cap measured in all six locales with five characters of slack; verify `node scripts/check-naming.mjs --naming <path>` passes. Detail: annex J.
- [x] 1.5 Write the proposal-queue document `docs/ideation/2026-10-12-plinth-product-photos.md` summarising the idea for the owner (wedge in one sentence, revenue arithmetic with the division, incumbents as evidence, build estimate labelled an estimate, `## Device capabilities` with twelve used/not-used lines) and verify it states every section `docs/ideation/README.md` requires.
- [x] 1.6 Author the implementation annex (`annex/A`…`annex/M`) and the capability specs including `slide-kit` and `provenance-and-compliance`; run `openspec validate app-seller-photo-studio --strict` and verify it reports the change valid.

## 2. Spikes M0 (measure before building; scratch only) — `annex/M-spikes-and-reads.md`

- [ ] 2.1 S0 Read the VPS: Caddy configuration, hostnames, certificates, disk, free RAM, SSH access path for deployment; record the findings and verify a hostname with a valid TLS certificate answers `GET /health` over HTTPS, or raise the single owner question through `AskQuestion`.
- [ ] 2.2 S1 cut-out benchmark: about 30 hard photos through the on-device candidate and the server cut-out; verify a table of results with 100% crops and the interior-RGB invariant check exists under `.kiln/scratch/`, and record the decision (free local path or not) in the evidence document. Pass numbers: annex M §S1.
- [ ] 2.3 S2 fidelity benchmark: the same photos through each Enhanced candidate; measure colour difference, structural similarity and label-text comparison inside the source mask; verify thresholds for the guard are recorded with the numbers that set them (annex E §Guard).
- [ ] 2.4 S3 vision benchmark: about 50 items across categories for category accuracy and scale, SKU and ruler reading, with cost per call; verify accuracy figures are recorded.
- [ ] 2.5 S4 walking skeleton: one request from the phone over HTTPS to the server, one purchase-token verification against the store interface, one ledger debit and one model call; verify with a request log on the server and a screenshot from the phone, and no key present in any client file (`node scripts/check-client-secrets.mjs` on the skeleton).
- [ ] 2.6 S5 re-read prices and first-party marketplace pages: Photoroom, Pixelcut, provider model pages, Ozon/Avito/eBay/Etsy/Depop/Amazon/Shopify help, then Poshmark/Mercari; verify each price and raised preset has a source and a read date in the evidence document, then fix pack prices with the money-owner role (annex J, annex B).
- [ ] 2.7 S6 policy read by `play-auditor`: AI-generated content, Data safety for photos sent to a server and to a processing provider, IPTC DigitalSourceType URIs, C2PA read-only, virtual-model verdict; verify the auditor's written findings are saved in the slot docs (annex H).

## 3. Checkpoint 1 and scaffold

- [ ] 3.1 Present price ladder, kill criteria, name, promise list and spike results to the owner and record the decision in his words in the evidence document; verify the recorded decision exists before scaffolding.
- [ ] 3.2 Scaffold with `node scripts/new-slot.mjs --economics docs/ideation/plans/plinth-product-photos.economics.json --differentiators docs/ideation/plans/plinth-product-photos.differentiators.json --naming docs/ideation/plans/plinth-product-photos.naming.json --spec app-seller-photo-studio`; verify the command succeeds and the slot appears in `registry/apps.json`.
- [ ] 3.3 Move the proposal and `plans/plinth-product-photos.*` into `apps/<slot>/docs/` and verify `ideas.orphanPlans` passes in `node scripts/verify.mjs`.
- [ ] 3.4 Write `capabilities.json` enabling `billing`; verify `node scripts/check-economics.mjs --slot <slug>` passes.
- [ ] 3.5 Declare identity in `store/studio.json` (mark, full palette, headlines, kickers, feature tagline, pill) from annex J before any UI styling; write the slot's own `Color.kt` from it and verify the `palette` group passes. Package layout: annex K.

## 4. Server (`apps/<slot>/server/`, one writer) — `annex/C-data-model.md`, `annex/D-server-api.md`

- [ ] 4.1 Implement Play Integrity Standard requests for session creation (API-00/API-01): bind the single-use challenge and canonical request fields into `requestHash`, verify package/version/certificate, `PLAY_RECOGNIZED`, `LICENSED` and device verdict server-side, and use Device Recall bit 1 only if S4 proves it available; never use `installId` as anti-reinstall proof and grant zero starter credits otherwise; verify altered-hash, replay, unevaluated-verdict, unavailable-recall and reinstall/reset cases.
- [ ] 4.2 Implement purchase confirmation plus encrypted client Room outbox/unique WorkManager retry; set and verify the wallet-derived `obfuscatedAccountId`, validate package/product/state/token/quantity/ownership before idempotent fulfillment, then consume/acknowledge server-side; implement authenticated Pub/Sub RTDN deduplication plus the Voided Purchases reconciliation sweep; verify forged, mismatch, duplicate-message, replay, process-death, pending, cancellation and voided/clawback cases.
- [ ] 4.3 Implement the ledger (reserve, settle only after API-09R accepts the idempotent receipt for an atomically stored result with matching declared length/digest, release, idempotency keys; D14 / E55); verify tests: insufficient balance refused before any model call, concurrent spends cannot overspend, disconnect/short-write/missing-or-wrong receipt does not settle, lost receipt retries safely, release on failure is idempotent, undelivered expiry releases.
- [ ] 4.4 Implement the model registry and job queue with bounded concurrency and the fal queue API behind a fake provider for tests (API-07…API-10, annex E registry); verify a contract test passes against the fake and one real call passes in scratch.
- [ ] 4.5 Implement the fidelity guard with thresholds from task 2.3 (annex E); verify tests: a drifting result is rejected and refunded, a faithful one is delivered and charged once, a changed label is rejected, identity and swap controls (annex M §S2).
- [ ] 4.6 Implement resource ownership, opaque result tokens and durable delivery receipts, partial series ledger, caps/refund debt, moderation cases and truthful revoking deletion; verify cross-wallet denial, disconnect/no-receipt-no-charge, partial series, old-token denial, report threshold, deletion receipt/provider status and expiry.
- [ ] 4.7 Deploy to the VPS by the path found in task 2.1 and verify HTTPS health plus one end-to-end job from the phone; secrets exist only in the server environment.

## 5. M1 — end-to-end basic path (client) — `annex/C`, `E`, `F`, `L`, `B`

- [ ] 5.1 Implement import by the system picker and the pipeline domain: mask, Original composition, backgrounds, shadow, fill and margin (annex E invariant); verify the unit test that interior pixels are byte-identical and the fill-ratio test (`CompositorInvariantTest`, annex K). Screens S-03, S-04, S-05.
- [ ] 5.2 Implement the cut-out routes per the task 2.2 decision, mask refinement with undo; verify unit tests for refinement undo and an offline quick-cut test when the local path is chosen.
- [ ] 5.3 Implement the manual editor with undo and redo (S-05); verify tests that undo restores the earlier image and that no network call happens.
- [ ] 5.4 After S5 has promoted Poshmark and Mercari from unread stubs to complete queue-1 presets, copy `annex/marketplaces.seed.json` to `app/src/main/assets/marketplaces.json`; do not proceed while either required 1.0 preset remains unread. Verify fixture failures independently for missing/invalid source URL, `verifiedAt`, `defaultLocale`, `editPolicy`, `allowedOverlays` when limited, and each of the six `localeDefaults`; verify the real file passes (annex B).
- [ ] 5.5 Implement the rules linter and fix-for-marketplace (annex L L01–L20, S-09); verify fixture tests for every rule type, every first-queue marketplace, pass and fail (e.g. WB 699×933 fails / 700×933 passes).
- [ ] 5.6 Implement the auto-card (recognition, editable category, scene selection, marketplace mode; annex E §Auto-card, S-04); verify tests that different categories choose different scenes and that a no-editing marketplace returns the unaltered photo (E31).
- [ ] 5.7 Implement export (formats, size, SKU names, share, gallery, ZIP) and provenance labels (S-12, annex H); verify tests for naming, size and the metadata declaration.
- [ ] 5.8 Implement wallet/purchases, durable fulfillment, unlock restore and recovery-code transfer; enforce free watermark and 5-item batch limit; verify marketplace watermark blocking, no subscriptions, store prices and no wallet backup/transfer.
- [ ] 5.9 Implement settings (every listed preference changes behaviour, annex C keys), the in-app privacy policy and delete-my-data (S-17, S-18, API-14); verify `node scripts/check-in-app-policy.mjs --slot <slug>` and `node scripts/verify.mjs --slot <slug>` settings checks pass.
- [ ] 5.10 Write all strings in the six locales and lay out for the longest; verify `node scripts/verify.mjs --slot <slug>` localization checks pass and no `values-<locale>` is missing a key.
- [ ] 5.11 Build the slot-owned theme, components (`PlinthStage`, `ProofRing`, …) and iconography from one licensed icon set with `icons/provenance.json` (annex J, F); verify the `icon` and `palette` groups pass.
- [ ] 5.12 Publish the truthful privacy policy in phase 1 before any device/server photo transmission (`node scripts/publish-privacy.mjs --slot <slug>`), verify HTTPS reachability and S-17/S-18 link; this is not store-pack generation or console submission.

## 6. M2 — seller workflow — `annex/F`, `I`, `G`

- [ ] 6.1 Implement the batch queue with Looks, resume after restart and per-item retry (S-06, S-07, E27, E35); verify tests that a restart does not reprocess finished items and one failure does not stop the rest.
- [ ] 6.2 Implement grouping, SKU patterns, scale/SKU/ruler reading with confirmation and exclusion of reference photos (S-08, E60–E62); verify tests for regrouping, unique SKU numbering and reference-photo exclusion.
- [ ] 6.3 Implement listing text in six languages within marketplace limits, copy and share (S-11, E65); verify tests for language independence and limit enforcement.
- [ ] 6.4 Implement the shoot coach (camera, live guidance, shot lists, import quality check) with permissions only when needed (S-16, E63–E64); verify on the phone that guidance works in airplane mode.
- [ ] 6.5 Implement the library with search and filters, bulk operations and project deletion (S-13, E67–E68); verify tests for search by SKU and bulk marketplace change.
- [ ] 6.6 Implement batch-done notification and the backup rules including Looks (wallet excluded; annex C); verify the manifest and rules files contain the declarations and a restore test shows Looks present.
- [ ] 6.7 Write the second-queue presets (Depop, Amazon, Shopify already seeded; raise status after S5) and unread stubs as their rules are read; verify the source check passes for each added entry (annex B).
- [ ] 6.8 Implement the slide kit (S-10, capability `slide-kit`, annex I): templates, seller text, prohibited-text matchers, contrast, series background identity; verify `SlideKitTest` fixtures.

## 7. M3 — studio — `annex/E`, `H`, `G`

- [ ] 7.1 Implement relight, expansion, eraser, upscale, recolour and scene generation with stated costs and marketplace gating (S-14, O-08…O-13); verify tests that costs are shown first and original-only presets disable them (E31, E53).
- [ ] 7.2 Implement the garment ghost-mannequin and virtual-model views (O-14 gated by S6) and variation series pricing: reserve once for the series, settle per delivered variant, release the unused remainder (annex E §Series); verify the series ledger test.
- [ ] 7.3 Implement the AI label, metadata declaration (after S6 URIs) and in-app report (capability `provenance-and-compliance`, API-11, E54); verify a report reaches the server and is recorded with its operation.

## 8. Verification (independent contexts)

- [ ] 8.1 Run `node scripts/run-slot-tests.mjs --slot <slug> --rerun` and verify a PASS that is not STALE. Named tests: annex K.
- [ ] 8.2 Run `node scripts/verify.mjs --slot <slug>` and `node scripts/check-client-secrets.mjs --slot <slug>` and verify no FAIL; run `node scripts/check-differentiators.mjs --slot <slug>` only when no promise is deferred.
- [ ] 8.3 Resolve every promise to `shipped`, `narrowed` or `dropped` with a proof that contains the family token; verify the differentiators gate passes without `--allow-deferred`.
- [ ] 8.4 Device run on the A40: confirm `adb -s R58MB3SGZVL shell getprop ro.product.model` reads `SM-A405FM`, install the debug APK, drive the journey in annex F (import → Make card → Look → batch → S-09 → export), a purchase of test credits, the coach, and an Enhanced job with a forced failure to see the refund; write `device-verification.json` with serial, artifact hash and at least three observations.
- [ ] 8.5 Dispatch a bug hunter, a verifier and `play-auditor` and `security-review` in contexts separate from the authors; verify each report ends with a claim ledger and every reproduced defect is fixed or recorded.
- [ ] 8.6 Final authoritative build: `node scripts/verify.mjs --slot <slug> --build` compiles `assembleDebug`; verify exit 0 and that the debug APK exists at `app/build/outputs/apk/debug/app-debug.apk`.

## 9. Handoff and release (BLOCKED until the owner's recorded approval)

- [ ] 9.1 Hand the owner the checkpoint 1.5 report with device evidence; the approval is recorded only from his words with `node scripts/approve-release.mjs --slot <slug> --note "<owner's words>"`.
- [ ] 9.2 [BLOCKED] `node scripts/prepare-release.mjs --slot <slug>`, `python scripts/store/render.py --slot <slug>`, `node scripts/media-capture.mjs --slot <slug>`, store pack validation, 16 KB check.
- [ ] 9.3 [BLOCKED] On a separate checkpoint-2 publish yes, submit through the console per `docs/play-publishing-playbook.md` §5.1.
- [ ] 9.4 [BLOCKED] Update `registry/apps.json` and start the earn readings for `node scripts/check-outcomes.mjs --slot <slug>`.
