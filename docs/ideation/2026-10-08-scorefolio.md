# Scorefolio — photographed sheet music to rehearsal PDF

## Recommendation

**MEASURED — `node scripts/scan-market.mjs` across 14 requested queries returned six candidate pockets; `sheet music scanner app` is one of them.** **MEASURED — `node scripts/scan-titles.mjs --query 'sheet music scanner app'` hand-classified 11 of 13 visible cards as on-job (0.85), alongside the guard's on-topic verdict.** Propose **Scorefolio**, a paid, local Android tool for a musician to capture or select photographed sheet-music pages, review crop/rotation/perspective cleanup and order, then export one print-ready rehearsal PDF.

**INFERRED — the paying user is a choir, band, orchestra or piano student preparing borrowed or marked-up paper music, because the measured shelf contains scanner and reader apps rather than a generic camera shelf.** **MEASURED — the first paid incumbent, `com.xemsoft.sheetmusicscanner`, is $19.99 with 50K+ installs and 2,920 displayed reviews.** Scorefolio sells a $9.99 one-time, no-ad unlock at the first multi-page PDF export; it is not a subscription.

## Market read

**MEASURED — `node scripts/enrich-market.mjs --query 'sheet music scanner app' --top 8 --concurrency 3` recorded these incumbents:** Sheet Music Scanner & Reader is paid $19.99, 4.0 stars, 2,920 reviews and 50K+ installs; its free sibling Sheet Music Scanner Pro is 2.3 stars, 207 reviews and 50K+ installs with IAP; Sheet Music Reader Halbestunde is 3.1 stars, 584 reviews and 100K+ installs with IAP; PlayScore2 is 3.6 stars and 500K+ installs from the complaint read; seven of eight enriched cards declare IAP and one declares ads.

**MEASURED — the weak mid-tier pocket is Sheet Music Scanner Pro (2.3 stars, 50K+) and Sheet Music Reader Halbestunde (3.1 stars, 100K+), not the 10M+ general Musescore catalogue.** **MEASURED — `node scripts/scan-complaints.mjs --query 'sheet music scanner app' --top 6 --concurrency 2` classified 8 of 12 critical reviews as paywall, 3 as broken, and 1 as accuracy; this is a three-review-per-page vocabulary sample, not a frequency estimate.**

## Complaints read

**MEASURED — `node scripts/scan-complaints.mjs --package de.halbestundeuben.app` printed this Play-rendered critical-review excerpt: “I used the free version. It was okay, but too limited. Take the picture of one page of the music sheet and that's it. Your privilege to try another pa”.** **MEASURED — the same command printed this excerpt: “doesn't work at all with scanning music sheet. i took a picture of a beautiful music page good lighting condition, straight direct view it output an e”.**

**MEASURED — `node scripts/scan-complaints.mjs --package com.organum.playscore` printed this excerpt: “so I love the idea of this app and for the most part it's fine. It is still very flawed. It will get notes and rhythms wrong constantly. It doesn't re”.** **INFERRED — Scorefolio should not promise optical music recognition, because the observed accuracy failure makes recogniser correctness a high-risk value proposition.**

## The wedge and transformation

**INFERRED from the measured one-page restriction and recognition-accuracy complaint — Scorefolio’s wedge is: it converts a batch of photographed score pages into a reviewable, cropped, deskewed, ordered, print-ready PDF that can leave the phone, without claiming to read or rewrite notes.**

**INFERRED — the concrete on-device pipeline is `ActivityResultContracts.PickMultipleVisualMedia` or CameraX capture → OpenCV-style document-edge detection, perspective transform, rotation and adaptive contrast → a per-page human crop review → `PdfDocument` render → `ACTION_CREATE_DOCUMENT` or `ACTION_SEND` through a `FileProvider`.** **UNVERIFIED — OpenCV dependency size and reliable staff-line preservation need a build spike with real score fixtures; that measurement decides whether the cleanup quality is good enough.**

**INFERRED — this is substantial rather than a file archive: capture/import, batch queue, per-page review, reorder/replace, output settings, PDF rendering and share/save are distinct flows, and the external PDF finishes the job.** **INFERRED — the estimate is 10–14 agent hours, dominated by image-quality fixtures and Android PDF/export verification.**

## Revenue plan

**INFERRED from the measured $19.99 paid incumbent — use the `purchase_only_no_ads` archetype at $9.99, because the buyer buys a durable export tool and a single purchase avoids the measured paywall hostility and the owner's subscription ban.** Rejected archetypes: `purchase_led_single_unlock` is rejected because this is a complete export tool and an in-app split risks recreating the paywall complaint; `ads_led_utility` is rejected because a rehearsal-export session is short and ads interrupt the job; `purchase_led_repeat_sales` is rejected because repeated paid packs do not improve the capture transformation; subscription is banned.

**INFERRED arithmetic — at $9.99, a 15% Play service fee is $1.4985 and leaves $8.4915 before tax/refunds; $250 monthly net needs $250 ÷ $8.4915 = 29.44, rounded up to 30 sales/month.** **UNVERIFIED — buyer-jurisdiction tax, refunds and the account’s actual Play fee tier have not been measured; this is why the proposal does not claim a post-tax net.**

**INFERRED — day one revenue arrives when a user reaches the multi-page PDF export after previewing their cleaned pages, sees the one-time $9.99 price, buys it, and receives the export with no renewal.** **MEASURED — the economics plan fixes three falsifiable criteria: by day 90, at least 500 installs and $250 revenue; failure to hit either number is a kill condition.**

## Promise contract

**MEASURED — `plans/scorefolio.differentiators.json` records four promises, including three core promises, all deferred pending a slot.** **MEASURED — its coverage explicitly binds before (batch import/order), during (reviewable cleanup) and after (PDF export), and its after claim is the `data_out` connective handoff family with planned `PdfDocument`, `ACTION_CREATE_DOCUMENT` and `ACTION_SEND` proof APIs.**

## Device capabilities

- camera: used — CameraX / `ACTION_IMAGE_CAPTURE`; without it a musician cannot capture paper music that is not already in the gallery; manifest permission: `android.permission.CAMERA`.
- photo picker / gallery: used — `ActivityResultContracts.PickMultipleVisualMedia`; without it existing score photos must be re-shot; no runtime permission.
- media playback or capture: not used — the job creates a document rather than playing or recording media.
- microphone: not used — the transformation is image-to-PDF, not audio recognition.
- location: not used — location does not improve a score image.
- motion sensors: not used — manual crop review is more reliable than sensor inference for this job.
- contacts / calendar: not used — the export goes through Android sharing rather than an address-book integration.
- notifications / reminder: not used — no deferred alert is required to produce the PDF.
- home-screen widget: not used — the flow starts with a capture/import session, not glanceable state.
- share / export: used — `ACTION_SEND`, `FileProvider`, `PdfDocument`; without it the transformed PDF cannot reach a printer, conductor or another rehearsal device; no manifest permission.
- Bluetooth / USB: not used — Android’s selected print/share target handles downstream connectivity.
- files / SAF: used — `ACTION_CREATE_DOCUMENT`; without it the user cannot choose a durable location for the finished PDF; no manifest permission.

## Permissions, server and visual direction

**INFERRED — CAMERA is the only runtime permission, justified by direct paper capture; the photo picker, share sheet and Storage Access Framework need no storage permission.** **INFERRED — no server, account, paid API or keyed AI service is needed; all pixels are processed on device.**

**INFERRED — visual direction: an ivory paper field, charcoal notation lines and a single vermilion page-order marker create the feeling of a rehearsal folio; page cards visibly curl/fan when reordered rather than behaving like stock file tiles.**

## Name derivation

**INFERRED — Scorefolio (10 characters) is the selected coined name because it combines score with folio, signals an assembled music document and avoids the shelf’s generic scanner noun.** **MEASURED — the live measured shelf contains the phrases Sheet Music Scanner & Reader and Sheet Music Scanner Pro, so `Music Scanner` is rejected as a category collision.** **UNVERIFIED — no class-9 trademark-register search or dedicated Scorefolio Play collision screen has been performed, so this is not clearance.** The scored five-name field, rejected alternatives and locale checks live in `plans/scorefolio.naming.json`.

## Honest unknowns

- **UNVERIFIED — real-score fixtures must show whether edge detection and contrast preserve fine staff lines, annotations and faint photocopies; a build spike decides it.**
- **UNVERIFIED — the scan did not measure willingness to pay $9.99 or conversion; the $19.99 incumbent proves only that a paid listing exists.**
- **UNVERIFIED — the market reads Play’s rounded install buckets and three review texts per app page, so it cannot establish exact market size or complaint prevalence.**
- **UNVERIFIED — rights to reproduce/share copyrighted scores remain the user’s responsibility; the app must not market itself as acquiring or redistributing music.**

## Falsifier

**The proposal is falsified if a fixture-based Android spike shows that automatic correction makes ordinary photographed notation less legible than the input or cannot emit a reliable ordered PDF, or if the recorded 90-day result is below 500 installs or $250 revenue.**
