# Plinth: Product Photos — M0 S5 money-owner findings

**Observed:** 2026-10-14  
**Scope:** Annex M §S5 only: first-party marketplace reads, provider/app pricing, billing-library release notes, and the arithmetic in `docs/monetization-strategy.md` §§2.2 and 3.7. This is a research finding and checkpoint recommendation, not approval of the plan or permission to scaffold.

## S5 disposition

Direct reads confirmed usable first-party material for Shopify and Poshmark. Direct access to Ozon, Avito, eBay, Etsy, Depop, Amazon Seller Central and Mercari did not produce enough page content to promote the existing secondary facts to first-party verified. The blockers are recorded below; an indexed search result or a secondary quotation is not treated as a direct read.

One selected provider operation now has an official current price: fal’s `fal-ai/bria/background/remove` page states $0.018 per inference without a prompt and $0.03 with a prompt. That is below the light-operation ceiling only for the no-prompt route. The older `fal-ai/any-llm/vision` route listed for recognition is shown as no longer supported; its former approximate cost is not a current price anchor.

The plan’s 0.80 net factor is a conservative approximation of the source assumptions, not a measured settlement factor. Applying the server-confirmed fraud assumption and all deductions listed in §2.2 gives 0.8081766 before the target margin. With §2.2’s 60% target margin, §3.7’s server-authority case requires K = 3.0933833. At the annex E ceiling of $0.025 per credit, the minimum price is $0.0773346 per credit under those assumptions. The existing packs’ calculated prices per credit range from $0.0799833 to $0.081. This is conditional arithmetic, not observed profitability.

## First-party marketplace reads

| Marketplace | S5 result on 2026-10-14 | What may / may not be carried forward |
|---|---|---|
| Ozon | **Inaccessible.** The recorded official media-requirements URL repeatedly redirected with HTTP 307 until the fetch tool hit its redirect limit. | No Ozon rule is promoted to first-party verified in this report. The existing evidence’s Ozon measurements and the `secondary` status in the seed remain as recorded; do not remove the verification warning based on this attempt. |
| Avito | **Inaccessible.** Search indexing surfaced an Avito support result for photo-upload problems, but the surfaced requirements article returned 404 on direct fetch; a second help URL returned only a generic support label. | No dimensions, count, formats or content rules are promoted to first-party verified here. The existing secondary source remains secondary. |
| eBay | **Blocked.** The official picture-requirements URL redirected to an eBay verification/CAPTCHA page. | The seed’s existing source row is not a successful S5 re-read. Do not treat its recorded numbers as newly re-verified. |
| Etsy | **Blocked.** The official Etsy Help article returned HTTP 403 on direct fetch. Search indexing identified the official article title and URL but did not substitute for reading it. | The seed’s existing source record and earlier evidence are not promoted by this access attempt. Rules remain first-party-unconfirmed for this S5 pass. |
| Depop | **Blocked.** Search indexing identified the official Help Centre article “How to list an item”; direct fetch returned HTTP 403. | No image count, dimensions or editing rule is newly verified. Existing third-party claims (including the square-format claim) remain secondary/unverified. |
| Amazon | **Blocked.** Seller Central redirected to a sign-in flow; a separate seller-help page returned only a blank brand response. | The secondary guide already in the evidence is not a first-party S5 read. No Amazon requirements are promoted here. |
| Shopify | **First-party content read.** Shopify’s Product media types page states that product/collection images can be up to 5000×5000 px or 25 megapixels, must be under 20 MB, and lists accepted image types. It says PNG is best for most product images, followed by JPEG; TIFF, BMP, GIF, SVG, HEIC and WebP are also accepted. It says 2048×2048 px usually displays best for square product images and that display depends on the store theme. | These are current source statements, not additional assumptions about image count or editing rules; the page section read does not establish those. |
| Poshmark (feasible read) | **First-party content read.** Poshmark’s 2026 portrait-photo article says listing photos now appear in a 3:4 portrait frame, old square photos remain intact, and sellers need not update them. It recommends centering the item, filling the frame while leaving a small margin, bright even light, and clean backgrounds. Its 2020 listing-photography guide says a listing can include up to 16 photos and 1 video and recommends a white/simple background; the older square-crop advice is superseded for the cover display by the newer article. | The newer platform statement is the relevant display guidance. The older count was read from Poshmark’s own blog, but confirm in the app if the exact current listing limit is operationally important. |
| Mercari (feasible attempt) | **Inaccessible for rules.** The official Help Center opened only a personalized-support/login page; a guessed article URL returned 404. | No photo-spec claim is promoted. The seed’s unread stub stays unread; no third-party result is used to fill it. |

The S5 first-party read failures mean marketplace preset facts should remain visibly unverified where they were already marked secondary/unread. No marketplace seed, evidence record, or preset was edited in this spike.

## Pricing pages and selected provider routes

### Model/provider pricing

- **O-02, selected route:** fal’s page for `fal-ai/bria/background/remove` names Bria RMBG 2.0 and states $0.018 per inference without a prompt; prompt requests are priced at $0.03 and routed to Guided Remove Background. The no-prompt price fits the $0.025 light-call ceiling; a prompt costs $0.005 above that ceiling. This finding depends on keeping the O-02 request prompt-free.
- **O-01, route listed in annex E:** the current fal page for `fal-ai/any-llm/vision` says, “This model is no longer supported.” The earlier ≈$0.01/call record in annex E is not a current price for a supported endpoint. The replacement model and its price are unresolved; do not treat the absorbed-recognition allowance as re-confirmed by S5.
- **O-15:** annex E names only the generic `any-llm`, not a fully selected, current model endpoint. No price is assigned to that generic label by this read.
- **O-07–O-14:** annex E’s model registry leaves the endpoint IDs null for these operations pending the S2 benchmark. They are not selected endpoints with prices that S5 can confirm. The S2 decision must use provider-owned model pages and compare each price to its tier ceiling.

### Photoroom and Pixelcut

- The current Photoroom pricing page was reachable, but the fetched page content exposed its plan-comparison introduction rather than numerical plan prices. Its official blog comparison page was also reachable, but the current readable capture did not expose its comparison-table contents. The prior evidence record cites the blog’s Photoroom Pro $90/year and Pixelcut Pro $96/year figures as fetched 2026-10-12; in this S5 report those amounts remain **ON RECORD (older competitor-authored comparison), not currently re-verified prices**.
- Pixelcut’s current pricing page was readable. It describes a free version and Pro features, and says annual billing saves 20%, but the fetched page did not expose a dollar amount. The exact current Pixelcut Pro price therefore remains unverified from the first-party pricing page.
- These subscription comparisons are not equivalent to Plinth’s consumable-credit packs plus separate non-consumable unlock. They cannot substantiate a “market price minus a couple percent” calculation without a comparable unit and billing period.

## Play Billing and version notes

The official Google Play Billing Library release notes list 9.1.0 dated 2026-06-18 as the newest release shown on the page read on 2026-10-14. This is the version reference for a future pin; no Plinth module exists yet and no dependency was pinned by this spike.

The official ML Kit release notes still show `play-services-mlkit-subject-segmentation` 16.0.0-beta1 (last updated 2023-11-07); it is not a stable segmentation candidate. The AndroidX stable-channel page reports last updated 2026-10-12, but no Plinth AndroidX versions have been selected or used yet, so this read does not certify a future dependency set. Re-read the specific release notes when the actual stable artifacts are chosen.

**Orchestrator decision (2026-10-15, not a measurement):** These are research notes, not a client
dependency approval. The unmeasured S3 spike owns the recognition-route decision; no ML Kit SDK is
carried on the client classpath pending that spike (see `M0-S3-vision-and-ocr.md`).

## Net factor re-derived from §§2.2 and 3.7

The inputs below are **assumptions in the strategy document**, not observations from Play settlement: server-confirmed `F_fraud = 0.01`, `F_billing = 0.15`, `F_refund = 0.02`, `F_fx = 0.02`, and target margin `M_target = 0.60`. The document also warns that the billing-fee assumption is tier/market dependent and whether the fee is computed on a tax-inclusive price is unverified.

Using §2.2’s formula and §3.7’s server-authority case:

```text
net proceeds factor before target margin
 = (1 − 0.01)(1 − 0.15)(1 − 0.02)(1 − 0.02)
 = 0.99 × 0.85 × 0.98 × 0.98
 = 0.8081766

required K at 60% target margin
 = 1 / (0.8081766 × (1 − 0.60))
 = 3.0933833

minimum listed price per credit at $0.025 provider cost
 = $0.025 × 3.0933833
 = $0.0773346
```

The plan’s 0.80 is lower than the formula’s 0.8081766 and is therefore conservative under these exact assumptions; it is not evidence that sellers’ actual payouts net 80%. Using the plan’s listed packs and the $0.025 per-credit cost ceiling, their price/cost multiples are: Starter $7.29 / 90 = $0.081 per credit, or 3.24×; Studio $18.49 / 230 = $0.0803913 per credit, or 3.2156522×; Pro $47.99 / 600 = $0.0799833 per credit, or 3.1993333×. All exceed the derived 3.0933833× floor, narrowly at the largest pack, **only if** the cost ceiling and assumed deduction rates hold.

Sensitivity that could reverse the decision: if the effective billing deduction were 25% rather than 15%, keeping the other strategy assumptions fixed, the derived net factor would be 0.7130970, the required K 3.5058344, and the minimum price at the $0.025 ceiling $0.0876459 per credit. All three current pack prices per credit are below that stress-case minimum. This is a scenario, not a claim that the actual fee is 25%.

## Checkpoint price hypotheses (recommendation only)

**Recommendation:** carry the existing ladder to the owner’s checkpoint as a provisional experiment, not as a market-validated price and not as owner approval:

| Product hypothesis | Proposed checkpoint value |
|---|---:|
| Starter pack | $7.29 for 90 credits |
| Studio pack | $18.49 for 230 credits |
| Pro pack | $47.99 for 600 credits |
| Non-consumable unlock | $28.99 |

The amounts and pack sizes are on record in annex J and the economics plan; this S5 read did not establish a directly comparable competitor price. Keep “market price minus a couple of percent” out of factual store copy unless a current, first-party comparable is obtained. Also keep unlock and credit purchases visibly distinct: the unlock removes the stated watermark/batch restriction; it does not fund provider calls. If the owner wants to preserve the 60% target margin in markets where the effective billing deduction exceeds roughly 17.8% (other assumptions unchanged), the current largest pack’s price/cost ratio is insufficient at the $0.025 ceiling; price, margin target, eligible regions or provider-cost ceiling would need an explicit owner decision rather than silent arithmetic drift.

This recommendation rejects **declaring a new exact price from the current competitor pages**: current official price pages did not return usable dollar figures, the only stated comparable values are an older competitor-authored annual-subscription comparison, and a subscription is not the same unit as a Plinth credit pack. It also rejects treating the 0.80 factor as observed; the documents provide assumptions, not payout data.

## Falsifiable objections to the existing ladder

1. **The “market price minus a couple percent” premise has no current comparable-price evidence in this read.** It is falsified as a usable anchor if current first-party Photoroom/Pixelcut pages or store offers show no comparable price for a similar seller job and unit; it is supported only if an owner can identify current, region-specific, directly comparable offers and their billing units. The current captured pricing pages did not show dollar amounts.
2. **The 0.80 payout factor may be wrong for the target markets.** It is falsified if a sourced Play settlement sample, with tax treatment, refunds, FX and fee tier separated, yields a net factor below 0.80; it is also not measured merely because the formula’s assumed factor is 0.8081766. Obtain the actual regional fee/tax treatment and early payout records before calling it a realized margin.
3. **The pack floor is sensitive to billing deductions.** Holding the source’s other assumptions constant, a billing deduction above approximately 17.8% makes the 600-credit pack fall below the 60%-margin K floor at the $0.025 provider ceiling. A confirmed 25% deduction gives a required $0.0876459/credit, above every listed pack’s $/credit. This objection is falsified by evidence that applicable deductions are low enough or by a deliberate owner-approved margin/cost change.
4. **The provider ceiling is not yet validated for all paid operations.** O-02 at $0.018 without a prompt is below the light ceiling, but a prompt route at $0.03 is above it; other enhanced routes remain unselected pending S2. Any enabled paid route whose official per-call cost exceeds its tier ceiling falsifies the present pack-to-cost arithmetic until credit pricing or routing changes.
5. **The absorbed recognition budget is not currently tied to a supported endpoint.** The previously recorded vision model now reports that it is no longer supported. A replacement’s official endpoint price and the O-01 call budget can falsify the “free/absorbed” cost assumption if they exceed the budget.
6. **The unlock price has no first-party comparable anchor in this read.** Evidence that sellers will not buy watermark removal/batch access at $28.99, or that a comparable one-time unlock is priced materially differently, would falsify this specific hypothesis. Credits do not eliminate that uncertainty because the unlock buys a different entitlement.
7. **The revenue gate is not price validation.** The existing 90-day install/revenue thresholds are criteria recorded in the plan, not evidence that enough sellers will purchase either pack or unlock. A properly sourced console reading that misses either threshold at the decision window would falsify the launch hypothesis; an early reading, missing data, or a price-page comparison cannot establish conversion.

This pass made no writes to the price, criteria, evidence record, economics JSON, seed, contract, or product files; the only write action was creation of this scratch report. No paid provider call, scaffold, product-code work, build, deployment, Git-mutating operation, release or publish was performed. Final read-only `git status --short` showed `M docs/research/2026-10-12-seller-platform-evidence.md`; I did not edit that file during this pass, and because no initial status was captured, I cannot attribute its modified state or claim it was clean beforehand. This S5 report does not authorize scaffolding; the owner’s checkpoint remains required.

## Claim ledger

All web reads below were made on **2026-10-14** unless an older read date is explicitly identified. “Inaccessible” means the page could not be read in this pass; it is not evidence that the content is absent.

| Claim | Label | Evidence |
|---|---|---|
| The S5 pass did not write the research evidence file; final status nevertheless showed that tracked file modified, and its state before this pass is unknown. | MEASURED (command output / action record) | Final read-only command `git status --short` — 2026-10-14; output `M docs/research/2026-10-12-seller-platform-evidence.md`. This pass's sole write action was `.kiln/scratch/plinth-m0-s5/report.md`; no initial status was captured. |
| Annex M assigns these first-party marketplace/pricing/release-note reads to S5 and requires recording prices with source and read date. | MEASURED | `openspec/changes/app-seller-photo-studio/annex/M-spikes-and-reads.md` §S5; read 2026-10-14. |
| The proposed pack prices, credit counts, unlock price, 0.80 factor, and “market minus a couple percent” wording are existing hypotheses, not S5-approved values. | MEASURED | `openspec/changes/app-seller-photo-studio/annex/J-identity-and-money.md` §Money; `docs/ideation/plans/plinth-product-photos.economics.json`; read 2026-10-14. |
| The economics contract says price correctness is not established by its validator and the archetype/price decision belongs to the owner. | MEASURED | `docs/slot-economics.md`; `harness/roles/money-owner.md`; read 2026-10-14. |
| Claim labels require sources and missing facts remain UNVERIFIED; reports end in a claim ledger. | MEASURED | `.cursor/rules/grounded-claims.mdc`; read 2026-10-14. |
| Shopify accepts the listed image formats, limits product images to 5000×5000 px / 25 MP and under 20 MB, says 2048×2048 usually displays best for square images, and theme controls presentation. | MEASURED (first-party page read) | Shopify, “Product media types”: https://help.shopify.com/en/manual/products/product-media/product-media-types — accessed 2026-10-14. |
| Poshmark currently frames listing photos in a 3:4 portrait display, preserves old square photos, and recommends centered composition with margins and clean backgrounds. | MEASURED (first-party page read) | Poshmark Blog, “A New Look for Listing Photos”: https://blog.poshmark.com/2026/03/25/your-guide-to-portrait-photos/ — accessed 2026-10-14. |
| Poshmark’s older guide states up to 16 photos and 1 video and recommends a plain/white background; its square-cover advice predates the current portrait display article. | MEASURED (first-party page read) | Poshmark Blog, “The Poshmark Listing Photography Guide”: https://blog.poshmark.com/2020/01/16/the-poshmark-listing-photography-guide/ — accessed 2026-10-14; current display cross-check at the 2026 article above. |
| Ozon first-party media page did not load because of repeated 307 redirects; no Ozon rule was newly verified. | MEASURED (access outcome) | https://docs.ozon.ru/global/products/requirements/media — attempted 2026-10-14; redirect-limit failure. |
| Avito first-party requirements content was not readable; the surfaced requirements article returned 404. | MEASURED (access outcome) | https://support.avito.ru/articles/200808408 — attempted 2026-10-14; HTTP 404. Generic help result: https://support.avito.ru/articles/2223 — accessed 2026-10-14. |
| eBay’s official picture requirements page redirected to a CAPTCHA; no requirement was independently confirmed in this pass. | MEASURED (access outcome) | https://www.ebay.com/help/selling/listings/pictures-videos/picture-requirements?id=4148 — attempted 2026-10-14; CAPTCHA redirect. |
| Etsy’s official image guide returned HTTP 403; no rule was independently confirmed in this pass. | MEASURED (access outcome) | https://help.etsy.com/hc/en-us/articles/115015663347-Requirements-and-Best-Practices-for-Images-in-Your-Etsy-Shop — attempted 2026-10-14; HTTP 403. |
| Depop’s Help Centre listing article was identified but returned HTTP 403 on direct fetch; no image rule was independently confirmed. | MEASURED (access outcome) | Depop Help Centre, “How to list an item”: https://depophelp.zendesk.com/hc/en-gb/articles/360032716413-How-to-list-an-item — attempted 2026-10-14; HTTP 403. |
| Amazon Seller Central image-help page redirected to sign-in and another seller-help URL returned no readable text; no Amazon rule was independently confirmed. | MEASURED (access outcome) | https://sellercentral.amazon.com/help/hub/reference/G200986900 — attempted 2026-10-14; sign-in redirect. https://sellercentral.amazon.com/help/hub/reference/external/G1881?locale=en-US — attempted 2026-10-14; empty response. |
| Mercari’s official help center required personalized sign-in and the attempted article URL returned 404; no Mercari image rule was newly verified. | MEASURED (access outcome) | https://www.mercari.com/us/help_center/ — accessed 2026-10-14; login-only response. https://www.mercari.com/us/help_center/article/4 — attempted 2026-10-14; HTTP 404. |
| fal lists $0.018 per no-prompt inference and $0.03 for a prompt request on the selected BRIA background-removal page. | MEASURED (first-party model page read) | fal.ai, `fal-ai/bria/background/remove`: https://fal.ai/models/fal-ai/bria/background/remove — accessed 2026-10-14. |
| The no-prompt BRIA price is under the light-call ceiling; the prompted price is $0.005 above it. | INFERRED | fal.ai price above; annex E light ceiling $0.025, `openspec/changes/app-seller-photo-studio/annex/E-pipeline-and-operations.md` — read 2026-10-14. |
| The fal `any-llm/vision` page says the model is no longer supported. | MEASURED (first-party model page read) | https://fal.ai/models/fal-ai/any-llm/vision — accessed 2026-10-14. |
| The earlier ≈$0.01 vision estimate is not a current supported-endpoint price. | INFERRED | Annex E’s earlier estimate and current unsupported-model page above; `annex/E-pipeline-and-operations.md` — read 2026-10-14. |
| The current Photoroom pricing page did not expose a numerical price in its fetched content; its comparison blog page was also reachable without its table content in the returned capture. | MEASURED (access/content outcome) | https://www.photoroom.com/pricing and https://www.photoroom.com/blog/photoroom-vs-pixelcut — accessed 2026-10-14. |
| Pixelcut’s accessible pricing content says annual Pro billing saves 20%, but shows no dollar amount in the returned page text. | MEASURED (first-party page read) | https://www.pixelcut.ai/pricing — accessed 2026-10-14. |
| The earlier $90/year Photoroom Pro and $96/year Pixelcut Pro figures are present in the existing record, but were not re-verified from current first-party price displays in S5. | ON RECORD (older competitor-authored comparison) | `docs/research/2026-10-12-seller-platform-evidence.md` §Market evidence, read 2026-10-14; cited source is Photoroom’s comparison blog above. |
| The release notes list Play Billing Library 9.1.0 dated 2026-06-18 as newest shown. | MEASURED (first-party release-notes read) | https://developer.android.com/google/play/billing/release-notes — accessed 2026-10-14. |
| ML Kit’s subject-segmentation artifact remains beta1 in the current listed version table, dated 2023-11-07. | MEASURED (first-party release-notes read) | https://developers.google.com/ml-kit/release-notes — accessed 2026-10-14. |
| The AndroidX stable-channel page was last updated 2026-10-12; no Plinth-specific stable version is established because no dependency has been selected. | MEASURED (first-party page read) | https://developer.android.com/jetpack/androidx/versions/stable-channel — accessed 2026-10-14; no version claim made. |
| The strategy’s server-confirmed net factor under its stated deduction assumptions is 0.8081766, and K at 60% target margin is 3.0933833. | INFERRED (arithmetic over assumptions) | `docs/monetization-strategy.md` §2.2 (F_fraud 0.01, F_billing 0.15, F_refund 0.02, F_fx 0.02, M_target 0.60) and §3.7 (server confirmation case); read 2026-10-14. These inputs are assumptions, not measured settlement data. |
| The current pack prices per credit are $0.081, $0.0803913 and $0.0799833, and their multiples at the $0.025 ceiling are 3.24×, 3.2156522× and 3.1993333×. | INFERRED (arithmetic over plan values) | `docs/ideation/plans/plinth-product-photos.economics.json`; `annex/J-identity-and-money.md`; annex E ceiling; read 2026-10-14. |
| If F_billing were 0.25 with the other assumptions unchanged, K would be 3.5058344 and the price floor $0.0876459 per credit. | INFERRED (sensitivity scenario, not actual fee) | Formula in `docs/monetization-strategy.md` §2.2; scenario changes only F_billing from 0.15 to 0.25; other input assumptions as listed above. |
| The current ladder is a price hypothesis and not evidence of adoption, margin realization or conversion. | INFERRED | Existing plan/evidence records in `docs/ideation/plans/plinth-product-photos.economics.json`, `annex/J-identity-and-money.md`, and `docs/research/2026-10-12-seller-platform-evidence.md` §Money; no Play Console readings exist in this S5 scope. |
| Whether sellers accept the pack/unlock prices, what the actual regional net factor is, the current exact Photoroom/Pixelcut paid prices, and replacement O-01 provider cost are unresolved. | UNVERIFIED | Decide with current first-party regional price offers, verified Play fee/tax treatment plus sourced settlement records, a supported O-01 endpoint’s provider page, and later sourced Play Console readings; owner decides at checkpoint. |
