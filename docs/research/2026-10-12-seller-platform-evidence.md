# Seller photo platform — evidence record (2026-10-12)

Scope: demand, competitor, marketplace-rule, policy and price facts behind the change
`openspec/changes/app-seller-photo-studio`. Every sentence carries a label from
`.cursor/rules/grounded-claims.mdc`.

**How the sources were read.** Pages were fetched on 2026-10-12 into the harness fetch cache (outside the
repository, not committed). A source is cited by its printed title, site and printed date. Where a page is a
third-party summary of a marketplace's own help centre, the claim is labelled `ON RECORD (secondary)` and the
item is on the spike list (`annex/M-spikes-and-reads.md`) for a first-party re-read before the preset ships.
A vendor describing its own product is `ON RECORD (vendor)`; a vendor describing a competitor is
`ON RECORD (competitor-authored)` and is never the only source for a price the app prices against.

## Market evidence

- ON RECORD (Play Developer Program Policy, effective 2026-08-26, fetched 2026-10-12): apps that generate
  images with AI "must contain in-app user reporting or flagging features that allow users to report or flag
  offensive content to developers without needing to exit the app." The same section requires such apps to
  prevent the generation of Restricted Content and Deceptive Behavior content.
- MEASURED (`node scripts/scan-titles.mjs --query "vitrine product photos" …`, 2026-10-12, output kept at
  `.kiln/scratch/name-scan.txt`): the Play shelf for "product photos" shows on-topic cards such as
  `Product Plus: AI Product Photo` (4.6, Giza Studios), `AI Product PhotoStudio-SKUShot` (unrated),
  `AI Product Photo Editor` (unrated) and `Product photo editor AI mockup` (3.3). The relevance guard called
  it 25/30 on-topic; the hand read agrees that these ranked cards are on the job. The visible cap is 30
  cards, which is a display cap and not a competitor count.
- ON RECORD (competitor-authored: Photoroom's own blog, "Photoroom vs Pixelcut", fetched 2026-10-12):
  Photoroom Pro is $12.99/month or $90/year, Photoroom Max $34.99/month or $251.88/year, Ultra from
  $99/month or $990/year; Pixelcut Pro is $10/month or $96/year, Pro+ $30/$288, Max $60/$576; Photoroom's
  batch editor handles up to 50 images; Pixelcut is described as one image at a time with limited batch.
- ON RECORD (independent comparison: Morphed, "10 Best AI Product Photo Generators in 2026", 2026-04-08):
  Photoroom free tier exports up to 250 images/month with a watermark and no commercial use; Pro from
  $7.50/month billed annually (consistent with $90/year above); 1000+ templates for Amazon, Shopify and Etsy;
  virtual model, ghost mannequin, product staging, recolour, flat lay. Pebblely: free 40 images/month without
  watermark, $19/month for 500 images, 40+ themed backgrounds. Flair.ai on-model fashion, free / $10/month.
  Claid.ai $9/month; ProductShot $5/month; Scenay free 5/month, $9.99/month.
- ON RECORD (vendor): Wildberries' own Jam subscription tariff page "Тарифы «Джема»", updated 2026-05-28:
  Standard 22 990 ₽ / 30 days (Photo studio 50 generations, rich-content generations 50, on-photo try-on 20),
  Advanced 42 990 ₽ / 30 days (Photo studio 100 generations, rich 70, try-on 20), Premium from 375 000 ₽ /
  30 days (unlimited). These tariffs bundle analytics; the arithmetic never compares like for like and the
  proposal does not claim it does.
- ON RECORD (vendor): Wildberries help "Фоторедактор", updated 2026-03-02: the editor offers upload,
  templates, background, text, WB AI, shapes, icons, promo-mark zones, crop, "improve quality" (neural
  network), background removal, a manual eraser, collage. "Сгенерировать композицию" (generate composition)
  "works in test mode and is not yet available to all sellers", is available only under the Jam subscription,
  returns 2 variants in about a minute, and each press (including "generate again") reduces the studio limit
  by 1.
- ON RECORD (secondary, third-party blog dated 2026-09-22): Ozon Seller has its own AI editor (generate
  background, remove background, enhance). Its stated gaps, which this app targets: one image per run, a
  single marketplace, no series in one style, no cross-marketplace transfer, platform-written slide text.
- ON RECORD (competitor-authored: WildCard, Seller Blog, updated 2026-09-06): a Telegram bot; slide card
  1 credit, SEO pack 1 credit, video 4 credits; from 299 ₽/month; 5 free generations then 2 per week (5 with
  channel subscription); "an unsuccessful frame by your taste debits a credit just like a successful one;
  only technically failed generations are refunded automatically"; the background is regenerated for every
  slide ("the same background across a series will not exist"); no "change only the background" command; it
  draws slides with title, number-bearing specs and benefit plates, and no schemes, tables or arrows; it
  renders text through the generative model and its own page documents a hyphenation error in the output.
- ON RECORD (vendor: Nightjar, "Why AI Product Photos Don't Match the Real Product", updated 2026-09-26):
  separates **realism** (looks like a photograph) from **fidelity** (is the item the buyer receives);
  names four pipelines — text-to-image, image-conditioned generation, a conventional cut-out composite that
  "retains the captured product pixels", and a masked generative edit that "can restrict where new content is
  created"; its own mitigation is a post-generation visual review with free retries.
- ON RECORD (secondary, GEN202 "Карточка товара вб: требования…", checked 2026-08-26, quoting WB help of
  2026-07-27/08-21/08-28): WB lists "photo unsuccessfully generated by a neural network — looks unnatural and
  text has distorted characters" among errors that lower the card's rating.
- INFERRED: the unserved combination is **item pixels preserved + background generated + one saved look
  across a whole batch + marketplace rules checked before upload**. WildCard regenerates per slide and
  refunds only technical failure; WB's editor gives two variants per press behind a 22 990 ₽/30-day
  tariff; Photoroom is a general editor with marketplace templates. This is the wedge, labelled INFERRED
  because it is a comparison of documented behaviours, not a measured conversion difference.

## Marketplace rules

First-queue detail with source and label is in `annex/B-marketplace-presets.md` and the seed data
`annex/marketplaces.seed.json`. Highlights that drive requirements:

- ON RECORD (secondary, GEN202, quoting WB help): WB photos JPG/PNG/WEBP, ≥ 700×900 (help article of
  2026-07-27/08-28) or ≥ 700×933 (help article of 2026-08-21 / 2026-10-02), compression quality ≥ 65 %,
  file ≤ 10 MB in the seller cabinet (API: ≤ 32 MB), up to 30 photos, main photo "full, not cropped,
  without packaging", neutral clean even contrasting background (no "white background" requirement),
  gradients discouraged, text contrast ≥ 4.5:1 small / ≥ 3:1 large. The two minimums disagree on the
  platform's own pages; the app uses the larger (700×933).
- ON RECORD (secondary, Platform. "Инфографика для вб…", 2026-10-06, quoting WB help of 2026-10-02): the
  prohibited slide text list — prices and discounts, contacts and links, evaluative words ("hit", "top",
  "best"), review rewards, sold counts, calls to action; WB gives 72 hours to fix before blocking.
- ON RECORD (secondary, third-party page with "requirements checked 2026-08-26" quoting Ozon help): Ozon
  3:4 default, recommended 900×1200, 200–7680 px per side, ≤ 10 MB, ≤ 30 photos. Another third-party page
  ("draft") states 1:1, 1000×1000 and RGB 255 white; it contradicts the first, carries no help-centre
  quotations and is **rejected**; the Ozon preset stays unverified until a first-party read (task S5).
- ON RECORD (secondary, Platform. "Фото для Авито в 2026", 2026-09-25, quoting support.avito.ru): Avito
  accepts JPG/JPEG/PNG/GIF only (no HEIC, no WebP), ≤ 25 MB (16 MB through the mobile web), longest side
  ≤ 1920 px at the stated 1920×1080 ceiling, 10 photos for goods; forbidden: contacts, QR codes, discount
  text, third-party logos, coloured frames and backgrounds; Avito itself may reorder or cut such content out.
- ON RECORD (vendor: Vinted Catalogue Rules): item photos "have to represent the item as it is - no image
  editing is allowed" and "cannot be replaced with stock photos, photos from ads and watermarked images".
  Decision: Vinted is an **original-only** marketplace preset (cut-out and background replacement are
  disabled; only crop, rotate and exposure are allowed).
- ON RECORD (secondary, Mochify "Product Image Requirements for Every Major Marketplace", updated
  2026-10-01, checked 2026-06): Amazon MAIN pure white RGB 255,255,255, ~85 % fill, ≥ 1000 px for zoom, no
  text/logos; eBay min 500 px, 1600 recommended, ≤ 12 MB, up to 24 photos, no borders/text/watermarks;
  Etsy recommends 2000 px, min 635 px, up to 10 photos, JPEG/PNG/GIF only; Depop square 1:1;
  Vinted vertical 3:4 guidance without documented specs; Shopify up to 5000×5000 / 20 MB.

## Competitors

The parity matrix with per-feature ids is `annex/A-competitor-parity.md`.

- ON RECORD (secondary, ML Kit release notes, fetched 2026-10-12): `play-services-mlkit-subject-segmentation`
  is `16.0.0-beta1`, dated 2023-11-07. This conflicts with `.cursor/rules/dependency-freshness.mdc`
  ("latest stable"), so an on-device ML Kit cut-out is not a default.
- ON RECORD (Android Developers, Play Billing Library release notes, fetched 2026-10-12): the newest listed
  release is 9.1.0 (2026-06-18); 9.0.0 (2026-05-19). The slot pins the latest stable at scaffold time.
- MEASURED (earlier in the session, probe recorded in the session transcript): `fal-ai/bria/background/remove`
  returned the source RGB exactly under the opaque interior; other models' fidelity is UNVERIFIED.

## Fidelity and AI-output standards

- ON RECORD (Nightjar, above): realism and fidelity are independent axes; fidelity (shape, text, logo,
  colour, proportion) is the ecommerce bar.
- INFERRED: the strongest protection is structural, not statistical — composite the original pixels and
  generate only what surrounds them. That is the product's constitution (design D1) rather than a feature.
- INFERRED: all text on slides is typeset by the app with real fonts and never drawn by a generative model;
  this removes the "distorted characters" failure that WB names and WildCard's own page documents.
- UNVERIFIED: IPTC `DigitalSourceType` values `trainedAlgorithmicMedia` and
  `compositeWithTrainedAlgorithmicMedia` as the right declaration for Enhanced outputs. Recalled from the IPTC
  controlled vocabulary, not read in this session; decider: task S6 reads `cv.iptc.org` and records the
  exact URIs.
- UNVERIFIED: C2PA content credentials. Signing needs a certificate chain the factory does not have; v1 does
  not claim it. Decider: owner at a later checkpoint, after the spike reads the current C2PA conformance
  programme.
- UNVERIFIED: EU AI Act transparency duties for generated images and their dates. The app labels Enhanced
  results in-app and in metadata regardless; whether a legal duty applies is a lawyer's question.

## Money

- INFERRED (arithmetic, shown in the proposal §Revenue arithmetic): credit price ≈ $0.080 across the three
  packs; provider cost ceiling per credit ≈ $0.025 → price/cost ≈ 3.2 ≥ the 3.09 floor in
  `docs/monetization-strategy.md` §3.7 (decider to re-read: money-owner at S5).
- UNVERIFIED: the net-per-sale factor 0.80 (Play fee 15 % plus an estimated tax haircut); source
  `docs/monetization-strategy.md` §2.2 must be re-read when the money owner runs.
- UNVERIFIED: whether a seller pays $28.99 once for the unlock plus credits at the market's going rate; the
  anchor is "market price minus a couple of percent" (owner's instruction) and the comparison above is
  monthly while this app sells a one-time unlock plus consumables. The kill criteria are the experiment.

## Naming

- MEASURED (scan 2026-10-12): `plinth product photos` — no Play title containing "Plinth" among 30 visible
  cards. `vitrine product photos` — `Vitrine Shop` (com.company.vitrineshop, unrated) and `Vitrine Furniture`
  (com.smartvitrine.vitrine) are on the shelf. `stagely` — exact title `Stagely - Local Theatre Diary`
  (com.stagely) and `Stager AI Home Virtual Staging` (2.3★). `shelfie product photo` — five apps titled
  `Shelfie`. `lotshot` — no exact title, but `Lotlot: AI Art, Voice & Photo`. A clean Play result is not
  trademark clearance, and no register was queried.

## Promise contract

The promise list, its jobCoverage and its depth families are in
`docs/ideation/plans/plinth-product-photos.differentiators.json`. Evidence rows cite the sections above.

## Honest unknowns

1. Whether sellers adopt a mobile app for this versus the platform's own editor (WB's generate-composition
   is beta and Jam-gated, Ozon's is free inside the cabinet).
2. Provider prices per call beyond the one measured model.
3. First-party marketplace rules for Ozon, Avito, eBay, Etsy and the second queue.
4. Whether the Plinth service has an application-specific HTTPS health route; the existing `pikupo.com`
   certificate is valid, but its current `/health` response is the site's static SPA fallback (S0, 2026-10-14).
5. The segmentation engine decision (spike S1) and the fidelity thresholds (spike S2).

## M0 spikes — 2026-10-14

### S0 — VPS and host discovery

- MEASURED (read-only SSH to `78.111.89.46` using the locally configured deployment identity, 2026-10-14):
  host `main`; Ubuntu 26.04; 1 vCPU; 954 MiB RAM, 761 MiB available at observation; root filesystem
  49 GiB with 41 GiB available; Caddy active; uptime 10 days 4:16 and load average 0.00. SSH access
  succeeded. No credential material was read or copied.
- MEASURED (remote unauthenticated HTTPS requests, 2026-10-14): outbound TLS verification succeeded
  (`TLS_VERIFY=0`) for `queue.fal.run` and `androidpublisher.googleapis.com`; both returned HTTP 404
  for the unauthenticated probe URL. This proves network/TLS reachability only, not API authorization.
- MEASURED (Caddy host/certificate inventory, 2026-10-14): configured public names include
  `pikupo.com`, `www.pikupo.com`, `78-111-89-46.sslip.io` and `www.78-111-89-46.sslip.io`; certificate
  files exist for these names. The IP-based `sslip.io` names redirect to `https://pikupo.com`.
- MEASURED (HTTPS GET, 2026-10-14): `https://pikupo.com/health` returns HTTP 200 with
  `Content-Type: text/html` and the same 35,236-byte body and SHA-256 as `/` and `/api/health`.
  It is the static SPA fallback, **not** an application health handler. `plinth.pikupo.com` did not
  resolve, and `plinth.78-111-89-46.sslip.io` resolved to the VPS but did not complete TLS because
  Caddy has no route/certificate for that name.
- INFERRED: the already-certificate-backed `pikupo.com` host can carry the Plinth service behind a
  dedicated path route without creating DNS records. The server's deployment must add a truthful
  health handler/reverse-proxy route while preserving the existing site; no Caddy or site files were
  changed during this read.
- RESULT: VPS/SSH access, Caddy state, resources, and outbound TLS prerequisites are measured. The
  domain/TLS blocker in D9 is resolved by the existing `pikupo.com` host. A Plinth-specific health
  route does not exist yet and must be verified after the server is implemented/deployed (task 4.7);
  no hostname question is required.

### S3 — Vision and OCR

- MEASURED (2026-10-14): the existing Amazon Berkeley Objects corpus has 30 product images and was
  assembled for S1. It has no first-party crosswalk to Plinth's 21 categories, no verified scale/SKU/
  ruler/label ground truth, and no six-language listing references. Twelve Plinth categories have no
  representative. No S3 accuracy result can be honestly derived from it; a separate labelled corpus is
  required before those accuracy claims can be made. Details and claim ledger:
  `.kiln/scratch/plinth-m0-s3/report.md`.
- MEASURED (Google ML Kit release notes, fetched 2026-10-14): unbundled text recognition
  `play-services-mlkit-text-recognition:19.0.1` is listed stable; subject segmentation
  `play-services-mlkit-subject-segmentation:16.0.0-beta1` is not stable and cannot qualify as S1 local
  segmentation.
- MEASURED / UNVERIFIED (fal model pages, fetched 2026-10-14): machine-readable pages list
  `fal-ai/any-llm/vision` at $0.01/request standard ($0.03 premium, above O-01's $0.02 ceiling) and
  `fal-ai/any-llm` at $0.001/request standard ($0.01 premium). Their human-readable pages report the
  models are no longer supported; endpoint availability is unresolved and no inference call was made.
- RESULT: S3 is inconclusive pending a labelled dataset and confirmation of supported O-01/O-15 routes.
  The specified defaults remain: category editable; scale/SKU are seller-confirmed suggestions; do not
  claim measured accuracy. See `.kiln/scratch/plinth-m0-s3/report.md`.

### S1 — Cut-out benchmark

- MEASURED (2026-10-14): 29 of 29 eligible images in the prepared 30-photo CC BY 4.0 corpus returned
  a BRIA background-removal result. Estimated provider cost was $0.522 at the recorded $0.018 per
  success. Remote API response latency was median 2.4 s and nearest-rank p95 5.298 s; it excludes
  result download and is not phone/device latency.
- MEASURED: source/output dimensions and SHA-256 records matched for all 29; opaque-interior RGB and
  white/black composites had 0 differences across 49,084,957 alpha-255 pixels.
- UNVERIFIED: there is no eligible stable local segmentation candidate measured, no local-vs-reference
  IoU, no hand-corrected-mask IoU, and no A40 latency. The six review crops were manually selected
  challenges rather than objectively ranked worst masks. The corpus lacks an explicitly tagged
  transparent-bag case. Do not report this reference-only test as segmentation accuracy.
- RESULT: use server-only cut-out at 1 credit; make no free/offline cut-out claim. This is the
  specified default because no eligible local candidate was demonstrated, not proof that all possible
  custom models fail. Full results, table, crops and claim ledger:
  `.kiln/scratch/plinth-m0-s1/S1-report.md`.

### S2 — Enhanced fidelity controls and route research

- MEASURED (2026-10-14, local zero-cost controls): an identity re-encode proxy across 30 corpus images
  produced mean dE00 0.000000; this verifies only a no-op pixel path, not a provider round-trip.
  Reusing the 29 existing BRIA masks, 812 ordered cross-item mask pairs had maximum IoU 0.7939,
  below the provisional 0.95/0.97 guard thresholds. This tests only the mask-IoU arm; there are no
  Enhanced outputs, reviewer-labelled same-product results, or measured 95th-percentile thresholds.
- ON RECORD: first-party fal metadata identifies candidate schemas for O-07 `fal-ai/flux/dev`,
  O-08 `fal-ai/iclight-v2`, O-09 `fal-ai/bria/expand`, O-10 `fal-ai/flux-pro/kontext`,
  O-11 `fal-ai/clarity-upscaler`, O-12 `fal-ai/bria/eraser`, and O-14 virtual try-on candidates.
  No first-party O-13 ghost-mannequin endpoint was found in the bounded search.
- UNVERIFIED: exact prices for these candidates. fal's exact pricing route requires authentication;
  exact per-operation costs were not queried and no paid fidelity calls were made. Third-party price
  estimates are not used to accept routes. All remain disabled; candidate recommendations do not
  replace the specified defaults.
- RESULT: S2 is incomplete as an Enhanced fidelity benchmark. The provider route/retention conditions
  identified by S6 must be established before transmitting benchmark photos; then exact price and
  spend can be confirmed before any run. Full report and local controls:
  `.kiln/scratch/plinth-m0-s2/S2-findings-and-route-recommendation.md`,
  `.kiln/scratch/plinth-m0-s2/controls_local_results.json`.

### S5 — Marketplace, provider and pricing re-reads

- MEASURED (first-party pages, fetched 2026-10-14): Shopify states product images up to 5000×5000 px
  / 25 MP, under 20 MB, and 2048×2048 usually displays best for square images. Poshmark's current
  article describes a 3:4 portrait display; its older guide states up to 16 photos, which should be
  confirmed in-app if operationally critical.
- MEASURED (attempts, 2026-10-14): Ozon, Avito, eBay, Etsy, Depop, Amazon Seller Central and Mercari
  did not yield readable first-party photo-rule content in this pass. Their existing secondary/unread
  status is not promoted. Mercari's apparent search-result claims could not be verified by direct
  fetch, so no dimensions, formats or edit policy are adopted.
- MEASURED (fal Bria model page, fetched 2026-10-14): `fal-ai/bria/background/remove` costs $0.018
  per no-prompt inference; prompt requests cost $0.03 and exceed the light-operation $0.025 ceiling.
  The no-prompt route is required to fit that ceiling.
- MEASURED / UNVERIFIED: the fal `fal-ai/any-llm/vision` human-readable page reports unsupported; the
  old approximate O-01 price is not a current supported-route price. No selected O-15 price or S2
  Enhanced endpoint prices were confirmed.
- INFERRED (strategy assumptions, not observed Play settlement): server-confirmed deductions yield a
  net factor of 0.8081766 and required multiple K = 3.0933833 at a 60% target margin. At the
  $0.025/credit cost ceiling, the floor is $0.0773346/credit. Existing packs range from $0.0799833 to
  $0.081/credit and exceed this floor only under the stated assumptions. A 25% billing-fee sensitivity
  would require $0.0876459/credit; that fee is a scenario, not a measured rate.
- RESULT: the money owner recommends carrying the existing ladder ($7.29/90, $18.49/230,
  $47.99/600 and $28.99 non-consumable unlock) as checkpoint hypotheses, not validated market prices
  or approval. The 0.80 factor and “market price minus a couple percent” are not measured facts. Full
  source ledger and objections: `.kiln/scratch/plinth-m0-s5/report.md`.

### S6 — Play policy and metadata

- ON RECORD / UNVERIFIED: no Plinth app exists, so this is a design-only review, not a compliance
  verdict. Photos transmitted to the service are collected under Play's off-device definition.
  Proposed Plinth retention (uploads ≤1 h, results ≤24 h) does not prove provider retention; fal's
  documented default payload storage is 30 days and media/CDN expiration is separate. Confirm the
  selected model's terms, storage controls and processor/sharing treatment before real-photo
  transmissions or Data safety declarations.
- ON RECORD: fal's terms have an Excluded Models exception to the content-training restriction and
  state an 18-or-age-of-majority requirement for Customer Solution users; applicability must be
  checked against the selected endpoint. The actual audience and content rating must describe the
  shipped app.
- ON RECORD: the in-app generated-content report is required by the specification, but its flow and
  server moderation behavior remain unverified. O-14 virtual model remains disabled pending a selected
  route and safeguards. No C2PA conformance claim is supported.
- MEASURED: IPTC vocabulary URIs read 2026-10-14:
  `http://cv.iptc.org/newscodes/digitalsourcetype/trainedAlgorithmicMedia`,
  `http://cv.iptc.org/newscodes/digitalsourcetype/compositeWithTrainedAlgorithmicMedia`, and
  `http://cv.iptc.org/newscodes/digitalsourcetype/algorithmicallyEnhanced`. Select per actual output
  transformation, not a blanket Enhanced label.
- RESULT: policy audit findings and source ledger are preserved in
  `.kiln/scratch/plinth-m0-s6/report.md`; no declaration, endpoint or policy status was changed.

### M0 disposition — what is closed, what is still open

| Spike | State | Consequence carried into the build |
|---|---|---|
| S0 host/TLS | CLOSED | Use the certificate-backed `pikupo.com` host behind a dedicated path route; a truthful app health handler is added at task 4.7. No domain/TLS question for the owner. |
| S1 cut-out | CLOSED | Server-only cut-out at 1 credit; **no** free/offline cut-out claim. Reference route `fal-ai/bria/background/remove`, no prompt ($0.018/success). |
| S2 fidelity | OPEN | No paid fidelity calls; guard thresholds are not measured. All Enhanced operations stay disabled until the thresholds exist. |
| S3 vision/OCR | OPEN (inconclusive) | No labelled corpus and no supported O-01/O-15 route price. Category stays editable; scale/SKU stay seller-confirmed suggestions; no measured-accuracy claim. |
| S4 walking skeleton | NOT STARTED | Sequenced after the server exists (task 4.7), not before the scaffold. |
| S5 prices/presets | CLOSED as a read | Price ladder carried as **hypotheses**; Ozon/Avito/eBay/Etsy/Depop/Amazon/Mercari rules stay secondary/unread, so task 5.4 cannot complete the queue-1 presets. |
| S6 policy | CLOSED as a read | O-14 virtual model stays disabled; photos are collected under Play's off-device definition; IPTC URIs recorded; no C2PA claim. |

### Checkpoint 1 — owner decision

- **Presented:** the price ladder ($7.29 / 90 credits, $18.49 / 230, $47.99 / 600, $28.99 non-consumable
  unlock), kill criteria (90 days, 1 000 installs, $200), the derived name (*Plinth: Product Photos*),
  the 15-promise contract, and the M0 spike results including the two open spikes (S2, S3).
- **Owner's words (2026-10-15):** «Теперь приступай к реализации всего приложения. … Тестовый телефон
  тебе подключен. … Ты только оркестратор над копилотом. … будь внимательней. Другие чаты проработали
  спецификацию.»
- **Decision:** approved to implement. The plan, name, promise list, kill criteria and the price ladder
  as **hypotheses** are approved; the two open spikes are known and do not block the scaffold, the server
  or M1. The S2 fidelity benchmark and the S3 accuracy measurement remain open and are named in the
  handoff rather than silently dropped.

### Owner decisions during implementation — 2026-10-15

Two decisions the server cannot be built without. Both recorded here because the annex leaves them open (S1
names the provider cost; S0 names the host but no route) and neither is the builder's to take.

- **Cut-out provider — the owner's own words:** «ключ дал, он уже лежит тут: `.kiln\secrets\fal.key`». So the
  S1 reference route stands: server-only cut-out against `fal-ai/bria/background/remove`, no prompt,
  $0.018/success, 1 credit to the seller. The key is a **server-side** secret: it is read from the server's
  environment, it is never in the client and never in a file under `app/src/main/`, and it is never printed
  into a transcript, a prompt or a log. `.kiln/` is gitignored, so the key does not enter history.
  **Not measured here:** whether the key is valid or funded; the first real inference call is the decider
  (spike S4), and its result belongs in `.kiln/scratch/`, never in a committed file.
- **Deployment — authorised.** The owner authorised deploying the Plinth service to the VPS measured in S0
  (`78.111.89.46`, host `main`, Caddy active) as a **dedicated path route on `pikupo.com`**, preserving the
  existing site, with a truthful health handler for this service rather than the site's SPA fallback. The
  authorization was given by selecting that option in the implementation session rather than typed as a
  sentence, and it is recorded as such rather than dressed as prose he wrote.
