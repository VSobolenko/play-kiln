# A — Competitor parity matrix

Source rows: `docs/research/2026-10-12-seller-platform-evidence.md` §Market evidence and §Competitors.
"Ours" names the capability spec and the milestone. `NON-GOAL` rows carry the reason so no later session
re-derives them. Labels: ON RECORD unless stated.

## Who was analysed

| Competitor | Kind | Evidence |
|---|---|---|
| Photoroom | general editor + marketplace templates, mobile-first | Photoroom blog (competitor-authored), Morphed comparison |
| Pixelcut | creative toolkit, one image at a time | Photoroom blog (competitor-authored about Pixelcut) |
| Pebblely | themed backgrounds | Morphed comparison |
| Flair.ai, Claid.ai, ProductShot, Scenay, Morphed | on-model, catalogue, budget, scene generation | Morphed comparison |
| WB Partners built-in editor and Jam AI | marketplace-native | WB help "Фоторедактор" (2026-03-02), "Тарифы «Джема»" (2026-05-28) |
| Ozon Seller built-in AI editor | marketplace-native | third-party blog 2026-09-22 (secondary) |
| WildCard (Telegram bot) | Russian card generator | WildCard's own editorial page (competitor-authored, disclosed) |
| Nightjar | fidelity-focused catalogue generator | Nightjar's own blog (vendor) |

## Matrix

| Id | Capability | Who has it | Ours | Stage |
|---|---|---|---|---|
| F-01 | Cut-out / background removal | all | `card-pipeline` Requirement: Cut-out | M1 |
| F-02 | Manual refine, eraser, undo | WB (eraser, undo/redo), Photoroom | `card-pipeline`: Manual refinement | M1 |
| F-03 | Solid, gradient, studio backgrounds | all | `card-pipeline`: Backgrounds (O-06) | M1 |
| F-04 | Generated scene background | Pebblely (40+ themes + text), WB composition (Jam), Photoroom | `card-pipeline`: Scene backgrounds generated without the item (O-07) | M1 presets, M3 free-text |
| F-05 | Contact shadow / grounding | Photoroom, Pebblely | `card-pipeline`: Shadow synthesised on device (E-pipeline §Shadow) | M1 |
| F-06 | Item recognition → fitting scene | WildCard (recognises the photo), WB composition | `card-pipeline`: Auto-card — **core** `auto-card-by-item` | M1 |
| F-07 | Marketplace size templates | Photoroom (1000+ for Amazon/Shopify/Etsy) | `marketplace-presets` presets are data (annex B) | M1 |
| F-08 | Pre-upload rules check against the marketplace's own list | none documented in any source read (INFERRED: absence in sources, not proof of absence) | `marketplace-presets`: Rules linter — **core** `marketplace-rules-linter` | M1 |
| F-09 | Batch with the same settings | Photoroom (up to 50), Pixelcut (limited) | `batch-and-library`: Batch — **core** `batch-with-saved-look` | M2 |
| F-10 | Saved look / brand kit | Photoroom (team brand kits) | `batch-and-library`: Look | M2 |
| F-11 | Slide / infographic series, seller-chosen text | WB editor text+templates (per card, manual), WildCard (one slide per run) | `slide-kit` (annex I) | M2 |
| F-12 | Slide text checked against the marketplace's prohibited list | none; WildCard documents the opposite (draws "скидка 30%" if typed) | `slide-kit`: Prohibited text refused before render | M2 |
| F-13 | Listing title/description within limits | WB Jam description generation, WildCard SEO pack | `listing-and-export`: Listing text in six languages | M2 |
| F-14 | Keyword frequency (Wordstat) | WildCard | **NON-GOAL v1** — Yandex Wordstat access needs a keyed server integration whose terms and cost are unread; revisit after S5 | — |
| F-15 | Ghost mannequin | Photoroom | `studio-ai`: Garment views (O-13) | M3 |
| F-16 | Virtual model / try-on | Photoroom, Flair.ai, WB try-on (20 per month in Jam) | `studio-ai`: Garment views (O-14) | M3 |
| F-17 | Recolour | Photoroom, Nightjar | `studio-ai`: Recolour (O-10) | M3 |
| F-18 | Upscale / enhance | WB "improve quality", Ozon enhance, Nightjar 2K/4K | `studio-ai`: Upscale (O-11) | M3 |
| F-19 | Relight, canvas expansion | not asserted for any competitor | `studio-ai`: Relight, Expansion (O-08, O-09) | M3 |
| F-20 | Video cards | WildCard (8 s, 4 credits), WB free "video cover" built from ≥ 2 photos | **NON-GOAL** — WB builds a video cover for free, so a paid one has no pricing room (INFERRED); revisit only on a measured ask | — |
| F-21 | Shoot guidance | none documented; WB publishes written shooting recommendations | `shoot-coach` | M2 |
| F-22 | SKU grouping, scale/label reading | none documented | `batch-and-library`: Grouping — supporting | M2 |
| F-23 | Library with search | none documented for the mobile competitors | `batch-and-library`: Library | M2 |
| F-24 | Look backup / restore | none documented | `batch-and-library`: Backup | M2 |
| F-25 | Resume a queue after restart | none documented | `batch-and-library`: Queue durability | M2 |
| F-26 | Failed result not charged | WildCard refunds only "technical" failures; Nightjar retries free after its own review | `credits-wallet-server`: Failure never charged — **core** `failure-never-charged` | M1 |
| F-27 | Free tier | WildCard 5 then 2/week, Photoroom 250 exports/month with watermark, Pebblely 40/month no watermark | integrity-bound starter grant when supported; free exports watermarked and batches capped at 5; no refill (D10/D18) | M1 |
| F-28 | Pricing | Photoroom Pro $90/yr, Pixelcut Pro $96/yr, WildCard from 299 ₽/month, WB Jam from 22 990 ₽/30 days | credit packs + one-time unlock (annex J) | M1 |
| F-29 | One look, many marketplaces | Ozon editor's stated gap (secondary) | `listing-and-export`: Export pack per marketplace — **core** `export-pack-per-marketplace` | M1 |
| F-30 | Item pixels untouched | none; every generative competitor redraws, Nightjar reviews afterwards | `card-pipeline`: Original mode — **core** `item-pixels-untouched` | M1 |
| F-31 | A/B main-photo variants | WB A/B service (5/10/unlimited by tariff) | export pack may carry a labelled second main variant (`ExportPackItem.variantOf`) | M2 |
| F-32 | Publish through marketplace Seller API | third-party tools | **NON-GOAL** — accounts and marketplace API keys on the client are a secret-handling surface; the owner banned accounts | — |
| F-33 | Import from a competitor's export | none seen | **NON-GOAL v1** | — |
| F-34 | Text drawn by the generative model | WildCard | **REFUSED by design** — all text typeset on device (D13); WB penalises distorted characters | — |

## What the matrix says about the wedge (INFERRED)

The only rows no competitor source documents are F-08, F-12, F-21–F-25 and F-30. F-30 is the product's
constitution; F-08/F-12 are the cheapest to build and the most visible to a seller whose card was blocked.
The features every competitor has (F-01–F-07, F-09, F-10, F-15–F-18) are table stakes and are built to
parity, not beyond it.
