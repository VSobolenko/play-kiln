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
4. Whether the VPS has a domain and TLS (first blocker, design D9).
5. The segmentation engine decision (spike S1) and the fidelity thresholds (spike S2).
