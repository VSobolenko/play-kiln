# Play market scans — 2026-10-16

Measured demand evidence accompanying `2026-10-16-money-ecosystem-ranking.md`.
This is a signed-out public Play listing sample, not revenue, conversion, retention,
or willingness-to-pay evidence.

## Method and limits

On 2026-10-16, each of the following queries was run in its own process using
`node scripts/enrich-market.mjs --query "<one query>" --concurrency 2 --top 8`.
Each command's unedited output is saved at the cited path under
`.kiln/scratch/recon-2026-10-16/`.

| Shelf | Query | Raw output |
|---|---|---|
| A — marketplace sellers | `ai product photo` | `.kiln/scratch/recon-2026-10-16/enrich-ai-product-photo.log` |
| A | `etsy listing maker` | `.kiln/scratch/recon-2026-10-16/enrich-etsy-listing-maker.log` |
| A | `product photo editor` | `.kiln/scratch/recon-2026-10-16/enrich-product-photo-editor.log` |
| A | `ebay listing tool` | `.kiln/scratch/recon-2026-10-16/enrich-ebay-listing-tool.log` |
| B — SMB marketing / print | `menu maker` | `.kiln/scratch/recon-2026-10-16/enrich-menu-maker.log` |
| B | `price card maker` | `.kiln/scratch/recon-2026-10-16/enrich-price-card-maker.log` |
| B | `flyer maker` | `.kiln/scratch/recon-2026-10-16/enrich-flyer-maker.log` |
| B | `poster maker` | `.kiln/scratch/recon-2026-10-16/enrich-poster-maker.log` |
| B | `social media post maker` | `.kiln/scratch/recon-2026-10-16/enrich-social-media-post-maker.log` |
| C — consumer personal | `ai headshot` | `.kiln/scratch/recon-2026-10-16/enrich-ai-headshot.log` |
| C | `pet portrait` | `.kiln/scratch/recon-2026-10-16/enrich-pet-portrait.log` |
| C | `interior design ai` | `.kiln/scratch/recon-2026-10-16/enrich-interior-design-ai.log` |
| C | `photo restorer` | `.kiln/scratch/recon-2026-10-16/enrich-photo-restorer.log` |
| C | `greeting card maker` | `.kiln/scratch/recon-2026-10-16/enrich-greeting-card-maker.log` |

The tool's source identifies the storefront as signed-out Google Play with
`hl=en`, `gl=US`, using `/store/search?c=apps` and `/store/apps/details`
(`scripts/enrich-market.mjs:540`; the query defaults are also
`scripts/lib/play-scan.mjs:400-401`). Search pages showed between 9 and 30
cards per query (the `search returned` lines in the 14 cited enrichment logs);
the enriched tables below contain only each query's first eight cards. Install
values are Play's rounded buckets, not exact installs.
Review counts are shown as printed: displayed count, with the schema.org exact
count in parentheses when supplied. `unknown` is not `false`.

All 14 enrichment commands exited 0. No query failed, and no exit-2 retry at
concurrency 1 was needed. The separate exact-reason reread called the same
`scanQuery()` used by the scanner and wrote one JSON line per query, in the
order below, to `.kiln/scratch/recon-2026-10-16/verdict-reasons.log`.
Its 14 verdict labels matched the labels printed by the enrichment runs. In
PowerShell, the exact extra command was:

```powershell
$code = 'import { scanQuery } from "./scripts/lib/play-scan.mjs"; const qs = ["ai product photo","etsy listing maker","product photo editor","ebay listing tool","menu maker","price card maker","flyer maker","poster maker","social media post maker","ai headshot","pet portrait","interior design ai","photo restorer","greeting card maker"]; for (const query of qs) { const s = await scanQuery(query); console.log(JSON.stringify({query, verdict:s.verdict ?? null, reason:s.reason ?? null, analysis:s.analysis ? {total:s.analysis.total,ratedCount:s.analysis.ratedCount,weakCount:s.analysis.weakCount,strongCount:s.analysis.strongCount,ratedShare:s.analysis.ratedShare,weakShare:s.analysis.weakShare,strongShare:s.analysis.strongShare}:null, relevance:s.relevance ? {level:s.relevance.level,onTopicCount:s.relevance.onTopicCount,total:s.relevance.total,suspect:s.relevance.suspect,warning:s.relevance.warning}:null})); }'
node --input-type=module -e $code 2>&1 | Tee-Object -FilePath '.kiln\scratch\recon-2026-10-16\verdict-reasons.log'
```

One search count differed between those reads: `etsy listing maker` returned
14 cards in the enrichment run (`enrich-etsy-listing-maker.log:L3`) and 9 in
the exact-reason reread (`verdict-reasons.log:L2`, `analysis.total`). Both
counts are retained; both readings returned `fortress`, and the cited reason
is from the 9-card reread.
The explicitly requested two-query title cross-check ran as:

```powershell
node scripts/scan-titles.mjs --query "price card maker" --query "menu maker" --on-job "\b(menu|price list|price card|flyer|poster|catalog|listing)\b" --off-job "\b(qr|pos|order|printer|barcode|label|sign|signature|video|wallpaper)\b"
```

It exited 0 and is captured in
`.kiln/scratch/recon-2026-10-16/scan-titles-price-card-menu.log`.

## Shelf readings

Shelf verdict is a compact summary of its query verdicts, not an additional
tool-generated score. The query-level verdict and verbatim reason are recorded
in each table.

### Shelf A — marketplace sellers

**Shelf reading: mixed** — three query screens returned `mixed`; `etsy listing
maker` returned `fortress`.

| Query | Tool verdict | Tool reason (verbatim) | Reason evidence |
|---|---|---|---|
| `ai product photo` | `mixed` | “demand proven, 9 weak incumbent(s), but 0.57 of rated are strong — enter only with a specific angle” | `verdict-reasons.log:L1` |
| `etsy listing maker` | `fortress` | “3/4 rated incumbents are >= 4.5, and only 1/4 rated incumbents are below 4.2 (0.25 < 0.3) — entrenched” | `verdict-reasons.log:L2` |
| `product photo editor` | `mixed` | “demand proven, 10 weak incumbent(s), but 0.57 of rated are strong — enter only with a specific angle” | `verdict-reasons.log:L3` |
| `ebay listing tool` | `mixed` | “demand proven, 9 weak incumbent(s), but 0.41 of rated are strong — enter only with a specific angle” | `verdict-reasons.log:L4` |

| App (package) | Installs | Rating | Reviews (displayed; exact) | containsAds | inAppPurchases | Listing price | Evidence |
|---|---:|---:|---:|---|---|---|---|
| Photoroom: AI Photo Editor (`com.photoroom.app`) | 100M+ | 4.7 | 4,020,000 (4,024,098) | false | true | free | `enrich-ai-product-photo.log:L7` |
| Lensa: photo editor & AI art (`com.lensa.app`) | 10M+ | 4.1 | 196,000 (195,534) | false | true | free | `enrich-ai-product-photo.log:L8` |
| Canva: AI Photo & Video Editor (`com.canva.editor`) | 500M+ | 4.8 | 27,400,000 (27,403,039) | false | true | free | `enrich-ai-product-photo.log:L9` |
| PhotoDirector: AI Photo Editor (`com.cyberlink.photodirector`) | 50M+ | 4.2 | 944,000 (943,563) | true | true | free | `enrich-ai-product-photo.log:L10` |
| Picsart AI Photo Editor, Video (`com.picsart.studio`) | 1B+ | 3.9 | 12,100,000 (12,125,534) | true | true | free | `enrich-ai-product-photo.log:L11` |
| Adobe Express: AI Photo, Video (`com.adobe.spark.post`) | 100M+ | 4.6 | 866,000 (865,770) | false | true | free | `enrich-ai-product-photo.log:L12` |
| Product Plus: AI Product Photo (`com.gizastudios.productplus`) | 5K+ | 4.6 | 381 (381) | false | true | free | `enrich-ai-product-photo.log:L13` |
| Fotor–AI Photo Editor & Filter (`com.everimaging.photoeffectstudio`) | 10M+ | 4.3 | 732,000 (731,568) | true | true | free | `enrich-ai-product-photo.log:L14` |
| Etsy Seller: Manage Your Shop (`com.etsy.butter.sell_on_etsy`) | 5M+ | 4.6 | 55,900 (55,938) | false | false | free | `enrich-etsy-listing-maker.log:L8` |
| Etsy: Shop from Real People (`com.etsy.android`) | 50M+ | 4.8 | 2,200,000 (2,197,848) | false | false | free | `enrich-etsy-listing-maker.log:L9` |
| Seller SEO: Etsy Tags & AI (`com.sellerseo.app`) | 100+ | unrated | unknown | false | true | free | `enrich-etsy-listing-maker.log:L10` |
| Etsy SEO Visibility Booster (`com.shb.etsyoptimizer`) | 5+ | unrated | unknown | false | false | paid ($9.99) | `enrich-etsy-listing-maker.log:L11` |
| eBay online shopping & selling (`com.ebay.mobile`) | 100M+ | 4.5 | 5,020,000 (5,023,860) | true | false | free | `enrich-etsy-listing-maker.log:L12` |
| Tier List Maker: Build & Share (`jp.co.updeito.tierlistmaker`) | 10K+ | 4.9 | 119 (119) | true | true | free | `enrich-etsy-listing-maker.log:L13` |
| List Maker: Create any list (`info.camposha.list_maker`) | 100+ | unrated | unknown | false | true | free | `enrich-etsy-listing-maker.log:L14` |
| Listing it! - Checklist (`com.listing_it`) | 50K+ | 4.6 | 824 (824) | false | false | free | `enrich-etsy-listing-maker.log:L15` |
| Photoroom: AI Photo Editor (`com.photoroom.app`) | 100M+ | 4.7 | 4,020,000 (4,024,098) | false | true | free | `enrich-product-photo-editor.log:L7` |
| Picsart AI Photo Editor, Video (`com.picsart.studio`) | 1B+ | 3.9 | 12,100,000 (12,125,534) | true | true | free | `enrich-product-photo-editor.log:L8` |
| Canva: AI Photo & Video Editor (`com.canva.editor`) | 500M+ | 4.8 | 27,400,000 (27,403,038) | false | true | free | `enrich-product-photo-editor.log:L9` |
| Lightroom Photo & Video Editor (`com.adobe.lrmobile`) | 100M+ | 4.5 | 3,680,000 (3,678,136) | false | true | free | `enrich-product-photo-editor.log:L10` |
| Photoshop Express Photo Editor (`com.adobe.psmobile`) | 100M+ | 4.5 | 2,600,000 (2,596,625) | false | true | free | `enrich-product-photo-editor.log:L11` |
| Product Plus: AI Product Photo (`com.gizastudios.productplus`) | 5K+ | 4.6 | 381 (381) | false | true | free | `enrich-product-photo-editor.log:L12` |
| Pixelcut AI Photo Editor (`com.circular.pixels`) | 10M+ | 4.2 | 245,000 (244,686) | false | true | free | `enrich-product-photo-editor.log:L13` |
| Bazaart AI Photo Editor Design (`me.bazaart.app`) | 5M+ | 4.6 | 71,600 (71,558) | false | true | free | `enrich-product-photo-editor.log:L14` |
| eBay online shopping & selling (`com.ebay.mobile`) | 100M+ | 4.5 | 5,020,000 (5,023,860) | true | false | free | `enrich-ebay-listing-tool.log:L7` |
| Vendit - eBay Listing Tool (`com.venditmobile.vendit`) | 1K+ | 3.5 | 15 (15) | false | true | free | `enrich-ebay-listing-tool.log:L8` |
| eBay Motors: Parts, Cars, more (`com.ebay.motorsapp`) | 1M+ | 4.0 | 6,490 (6,494) | false | false | free | `enrich-ebay-listing-tool.log:L9` |
| Pricing Tool for eBay Sellers (`com.zeedub.eBayPriceTool`) | 1K+ | 3.7 | 23 (23) | false | true | free | `enrich-ebay-listing-tool.log:L10` |
| eProfit - Sell on ebay with AI (`com.simplysoftsolutions.eprofit`) | 50K+ | 4.8 | 2,160 (2,158) | true | true | free | `enrich-ebay-listing-tool.log:L11` |
| ListIt: AI Listing for Ebay (`com.listitapp.app`) | 100+ | unrated | unknown | true | true | free | `enrich-ebay-listing-tool.log:L12` |
| Vendlists: AI eBay Listing App (`com.vendlists.app`) | 50+ | unrated | unknown | false | false | free | `enrich-ebay-listing-tool.log:L13` |
| Listman - eBay Listing Manager (`com.listman.twa`) | 500+ | unrated | unknown | false | false | free | `enrich-ebay-listing-tool.log:L14` |

### Shelf B — SMB marketing / print

**Shelf reading: mixed** — `menu maker`, `price card maker`, and
`social media post maker` returned `mixed`; `flyer maker` and `poster maker`
returned `fortress`.

| Query | Tool verdict | Tool reason (verbatim) | Reason evidence |
|---|---|---|---|
| `menu maker` | `mixed` | “demand proven, 1 weak incumbent(s), but 0.5 of rated are strong — enter only with a specific angle” | `verdict-reasons.log:L5` |
| `price card maker` | `mixed` | “demand proven, 5 weak incumbent(s), but 0.5 of rated are strong — enter only with a specific angle” | `verdict-reasons.log:L6` |
| `flyer maker` | `fortress` | “14/23 rated incumbents are >= 4.5, and only 3/23 rated incumbents are below 4.2 (0.13 < 0.3) — entrenched” | `verdict-reasons.log:L7` |
| `poster maker` | `fortress` | “13/19 rated incumbents are >= 4.5, and only 1/19 rated incumbents are below 4.2 (0.05 < 0.3) — entrenched” | `verdict-reasons.log:L8` |
| `social media post maker` | `mixed` | “demand proven, 8 weak incumbent(s), but 0.42 of rated are strong — enter only with a specific angle” | `verdict-reasons.log:L9` |

| App (package) | Installs | Rating | Reviews (displayed; exact) | containsAds | inAppPurchases | Listing price | Evidence |
|---|---:|---:|---:|---|---|---|---|
| Menu Maker, Price List Maker (`com.menu.maker`) | 1M+ | 4.9 | 42,300 (42,293) | true | true | free | `enrich-menu-maker.log:L7` |
| Price List & Menu Maker (`com.desygner.menus`) | 500K+ | 4.6 | 24,200 (24,181) | true | true | free | `enrich-menu-maker.log:L8` |
| Menu Maker: Menu Design (`menudesign.stockalert.site`) | 1K+ | unrated | unknown | false | true | free | `enrich-menu-maker.log:L9` |
| BrandBite Menu Card Maker App (`com.brandbite.android`) | 10K+ | unrated | unknown | false | true | free | `enrich-menu-maker.log:L10` |
| QR Menu Maker: Digital Menu (`com.sardemsoftware.qrmenu`) | 5K+ | 4.8 | 95 (95) | false | true | free | `enrich-menu-maker.log:L11` |
| Menusa: online QR menu maker (`com.menusa.android`) | 10K+ | 4.3 | 147 (147) | false | false | free | `enrich-menu-maker.log:L12` |
| e-menus (`com.emenus.android`) | 10K+ | unrated | unknown | false | false | free | `enrich-menu-maker.log:L13` |
| SaveMenu: QR Menu & Ordering (`com.savemenu.app`) | 5K+ | unrated | unknown | false | true | free | `enrich-menu-maker.log:L14` |
| Price List Maker Pro (`com.erfouris.grocerypricelist`) | 1K+ | unrated | unknown | false | true | free | `enrich-price-card-maker.log:L7` |
| Price List Maker: Quick & Easy (`com.appplanex.pricelistmaker`) | 1K+ | unrated | unknown | false | true | free | `enrich-price-card-maker.log:L8` |
| Price List Maker (`com.micrasol.pricelistmaker`) | 50K+ | 2.3 | 148 (148) | false | true | free | `enrich-price-card-maker.log:L9` |
| Price List Maker - Fast & Easy (`com.pricelistmaker`) | 100K+ | 4.1 | 3,730 (3,727) | true | true | free | `enrich-price-card-maker.log:L10` |
| PriceSnap - Make Price List (`com.skepticlabwk96.pricesnap`) | 10K+ | 4.2 | 159 (159) | true | true | free | `enrich-price-card-maker.log:L11` |
| Price List Maker global (`ir.mlggrand.priceList2`) | 1K+ | unrated | unknown | false | true | free | `enrich-price-card-maker.log:L12` |
| Price List Pro - Manage Prices (`com.price.list.pro`) | 100+ | unrated | unknown | false | true | free | `enrich-price-card-maker.log:L13` |
| Menu Maker, Price List Maker (`com.menu.maker`) | 1M+ | 4.9 | 42,300 (42,293) | true | true | free | `enrich-price-card-maker.log:L14` |
| Flyer Maker, Poster Maker (`com.bg.flyermaker`) | 10M+ | 4.8 | 385,000 (385,313) | true | true | free | `enrich-flyer-maker.log:L7` |
| Flyer Maker, Poster Design (`com.postermaker.flyermaker.tools`) | 500K+ | 4.3 | 12,100 (12,063) | true | true | free | `enrich-flyer-maker.log:L8` |
| Poster Maker, Flyer Maker (`com.nra.flyermaker`) | 5M+ | 4.7 | 167,000 (167,200) | true | true | free | `enrich-flyer-maker.log:L9` |
| Canva: AI Photo & Video Editor (`com.canva.editor`) | 500M+ | 4.8 | 27,400,000 (27,403,039) | false | true | free | `enrich-flyer-maker.log:L10` |
| AI Poster Maker - Flyer Maker (`com.thmobile.postermaker`) | 5M+ | 4.3 | 59,000 (59,021) | true | true | free | `enrich-flyer-maker.log:L11` |
| Poster Maker - Flyer maker (`splendid.postermaker.designer`) | 10M+ | 4.7 | 240,000 (240,363) | true | true | free | `enrich-flyer-maker.log:L12` |
| Flyer Maker, Poster Maker (`com.lefpro.nameart`) | 1M+ | 4.7 | 24,400 (24,369) | true | true | free | `enrich-flyer-maker.log:L13` |
| Flyers & Posters (`com.desygner.flyers`) | 1M+ | 4.6 | 16,000 (16,015) | true | true | free | `enrich-flyer-maker.log:L14` |
| Canva: AI Photo & Video Editor (`com.canva.editor`) | 500M+ | 4.8 | 27,400,000 (27,403,039) | false | true | free | `enrich-poster-maker.log:L7` |
| Poster Maker - Flyer maker (`splendid.postermaker.designer`) | 10M+ | 4.7 | 240,000 (240,363) | true | true | free | `enrich-poster-maker.log:L8` |
| Poster Maker, Flyer Maker (`com.nra.flyermaker`) | 5M+ | 4.7 | 167,000 (167,200) | true | true | free | `enrich-poster-maker.log:L9` |
| Flyer Maker, Poster Maker (`com.bg.flyermaker`) | 10M+ | 4.8 | 385,000 (385,313) | true | true | free | `enrich-poster-maker.log:L10` |
| Poster Maker – Flyers & Posts (`com.coolapps.postermaker`) | 10M+ | 4.8 | 319,000 (318,796) | true | true | free | `enrich-poster-maker.log:L11` |
| PosterMyWall: Design & Promote (`com.app.postermywall12`) | 1M+ | 4.5 | 2,600 (2,599) | false | false | free | `enrich-poster-maker.log:L12` |
| Poster Maker - AI Flyer Editor (`cn.leminet.poster`) | 100K+ | 4.8 | 1,810 (1,812) | true | true | free | `enrich-poster-maker.log:L13` |
| AI Poster Maker - Flyer Maker (`com.thmobile.postermaker`) | 5M+ | 4.3 | 59,000 (59,021) | true | true | free | `enrich-poster-maker.log:L14` |
| Social Media Post Maker (`com.bg.socialcardmaker`) | 1M+ | 4.8 | 38,600 (38,557) | true | true | free | `enrich-social-media-post-maker.log:L7` |
| Post Maker for Social Media (`com.post.maker.for.social.media.photo.editor.postplus`) | 100K+ | 4.5 | 999 (999) | true | true | free | `enrich-social-media-post-maker.log:L8` |
| Canva: AI Photo & Video Editor (`com.canva.editor`) | 500M+ | 4.8 | 27,400,000 (27,403,040) | false | true | free | `enrich-social-media-post-maker.log:L9` |
| Social Post Maker & Design (`com.desygner.socialposts`) | 1M+ | 4.4 | 28,200 (28,209) | true | true | free | `enrich-social-media-post-maker.log:L10` |
| Adobe Express: AI Photo, Video (`com.adobe.spark.post`) | 100M+ | 4.6 | 866,000 (865,770) | false | true | free | `enrich-social-media-post-maker.log:L11` |
| Picsart AI Photo Editor, Video (`com.picsart.studio`) | 1B+ | 3.9 | 12,100,000 (12,125,534) | true | true | free | `enrich-social-media-post-maker.log:L12` |
| Mysocial: AI Creator Manager (`io.mysocial.creators`) | 100K+ | 4.1 | 1,080 (1,084) | false | true | free | `enrich-social-media-post-maker.log:L13` |
| Own. - Post. Go Viral. Repeat (`com.nemo.nemoapp`) | 10K+ | 4.1 | 366 (366) | false | true | free | `enrich-social-media-post-maker.log:L14` |

#### Manual title classification for the SMB overlap

The scan printed 30 titles for `price card maker` and 13 for `menu maker`.
Below, titles are grouped by the advertised title alone for the seller-facing
job of making printable/displayable menus or price lists. This is a human
classification, not a claim about unobserved app functionality. "Off-job"
includes ambiguous titles that do not advertise that job. Source ranges refer
to the raw title-scan output.

**Price card maker — on-job (19 titles):** Price List Maker Pro; Price List
Maker: Quick & Easy; Price List Maker (Micra); Price List Maker - Fast & Easy;
Price List Maker global; Price List Pro - Manage Prices; Menu Maker, Price
List Maker; Price List Maker (MSTok); Make Price List & Invoice; Price List &
Menu Maker; Easy Rate: Price List Maker; Price List Maker (Ruhi apps);
Catalog Maker: PDF Price List (FESTIVAL POST); Price List Maker (EASA);
Catalog Maker: PDF Price List (IM Core); Price List (ZakaSoft); Price List
Pro (ZakaSoft); Catalog Maker: PDF Price List (VIBE CODERS); Hero: Sell,
Price & List Fast. (`scan-titles-price-card-menu.log:L5-L22`,
`L28-L37`)

**Price card maker — off-job / title-ambiguous (11 titles):** Canva: AI Photo
& Video Editor; Price Label Maker; `info`
(`com.skepticlabwk96.pricesnap`); Card Prices; `info`
(`com.pricepadstudio.pricepad`); PokeFrame: Trading Card Maker; Business Card
Maker; Misprint: Buy & Sell Cards; Digital Business Card Maker; Artificer -
MTG Card Maker; ValueSnap: Sports Card & TCG. (`scan-titles-price-card-menu.log:L23-L36`)

There is a title-surface disagreement for `com.skepticlabwk96.pricesnap`:
the title scan printed `info`, while the enrichment listing printed
`PriceSnap - Make Price List` (`enrich-price-card-maker.log:L11`). Both
readings are preserved; its human classification above follows the title-scan
surface, so it is off/ambiguous there, and the enrichment title advertises the
price-list job.

**Menu maker — on-job (4 titles):** Menu Maker, Price List Maker; Price List
& Menu Maker; Menu Maker: Menu Design; BrandBite Menu Card Maker App.
(`scan-titles-price-card-menu.log:L42-L49`)

**Menu maker — off-job / title-ambiguous (9 titles):** Menu Translator AI:
MenuGuide; Weekly Menu - Meal Planner; My Simple Menu; Menu, please! Menu
Translator; QR Menu Maker: Digital Menu; Menusa: online QR menu maker;
SaveMenu: QR Menu & Ordering; SOMENU: POS Software & QR Menu; e-menus.
(`scan-titles-price-card-menu.log:L50-L56`)

The scan's lexical guard called both queries `on-topic`, but its own
hand-assistance output warns that the guard's title-token share does not equal
the job share: 18/30 for `price card maker` and 8/13 for `menu maker`
(`scan-titles-price-card-menu.log:L3-L3`, `L37-L40`, `L57-L59`). The hand
classification above is narrower than that lexical bucket; QR/POS/ordering,
translation, consumer meal-planning, and unrelated card-making titles are not
seller menu/price-card design.

### Shelf C — consumer personal

**Shelf reading: mixed** — four query screens returned `mixed`; `photo
restorer` returned `candidate`.

| Query | Tool verdict | Tool reason (verbatim) | Reason evidence |
|---|---|---|---|
| `ai headshot` | `mixed` | “demand proven, 6 weak incumbent(s), but 0.29 of rated are strong — enter only with a specific angle” | `verdict-reasons.log:L10` |
| `pet portrait` | `mixed` | “demand proven, 7 weak incumbent(s), but 0.39 of rated are strong — enter only with a specific angle” | `verdict-reasons.log:L11` |
| `interior design ai` | `mixed` | “demand proven, 6 weak incumbent(s), but 0.33 of rated are strong — enter only with a specific angle” | `verdict-reasons.log:L12` |
| `photo restorer` | `candidate` | “18/27 rated incumbents are below 4.2 — demand proven, supply inadequate” | `verdict-reasons.log:L13` |
| `greeting card maker` | `mixed` | “demand proven, 5 weak incumbent(s), but 0.5 of rated are strong — enter only with a specific angle” | `verdict-reasons.log:L14` |

| App (package) | Installs | Rating | Reviews (displayed; exact) | containsAds | inAppPurchases | Listing price | Evidence |
|---|---:|---:|---:|---|---|---|---|
| GIO: AI Headshot Generator (`com.prequelapp.aistudio`) | 5M+ | 4.5 | 174,000 (174,437) | false | true | free | `enrich-ai-headshot.log:L7` |
| Headshot AI Pro: Perfect Photo (`com.mxy.photo.ai`) | 100K+ | 4.7 | 11,700 (11,743) | false | true | free | `enrich-ai-headshot.log:L8` |
| AI Photo Generator - ReCrea (`com.recrea.ai`) | 100K+ | 4.6 | 2,220 (2,219) | false | true | free | `enrich-ai-headshot.log:L9` |
| Headshot Plus: AI Headshots (`com.gizastudios.headshotplus`) | 1K+ | 4.5 | 103 (103) | false | true | free | `enrich-ai-headshot.log:L10` |
| Aragon.ai: Headshot Generator (`ai.aragon.dashboard`) | 1K+ | 3.1 | 12 (12) | false | true | free | `enrich-ai-headshot.log:L11` |
| AI Headshot & Photo Generator (`com.ai.headshot.generatorapp`) | 50K+ | 4.0 | 1,840 (1,836) | false | true | free | `enrich-ai-headshot.log:L12` |
| Dr. Headshot - AI Headshots (`com.drheadshot`) | 10K+ | 4.4 | 178 (178) | false | true | free | `enrich-ai-headshot.log:L13` |
| AI Headshot for CV & LinkedIn (`com.evolveralab.linkedin_profileapp`) | 1K+ | unrated | unknown | false | true | free | `enrich-ai-headshot.log:L14` |
| Pet Portrait: Pawtograph (`com.curlycorn.pawtograph`) | 5K+ | 4.6 | 235 (235) | false | true | free | `enrich-pet-portrait.log:L7` |
| AI Pet Portraits - Luks (`com.gsy.luks`) | 10K+ | 4.2 | 154 (154) | false | true | free | `enrich-pet-portrait.log:L8` |
| AI Pet Portraits – Echo Pets (`com.echopets.app`) | 1K+ | unrated | unknown | true | true | free | `enrich-pet-portrait.log:L9` |
| DreamPets: AI Pet Portrait App (`ai.dreampets.dreampets`) | 10K+ | 4.2 | 483 (483) | true | true | free | `enrich-pet-portrait.log:L10` |
| Petdraw: Pet Photo Art Editor (`com.yoya.petdraw`) | 1K+ | 4.0 | 26 (26) | false | true | free | `enrich-pet-portrait.log:L11` |
| PawCam: Pet Photo Camera (`com.jocoding.pawcam`) | 5+ | unrated | unknown | true | true | free | `enrich-pet-portrait.log:L12` |
| Talking Pet: Revive AI Photo (`tech.blowy.talkingpet`) | 100K+ | 4.5 | 12,300 (12,321) | false | true | free | `enrich-pet-portrait.log:L13` |
| DogCamera ‑ AI Pet Portraits (`com.bitsofwow.dogcamera`) | 1K+ | 4.8 | 88 (88) | false | false | free | `enrich-pet-portrait.log:L14` |
| AI Remodel - Interior Design (`pro.airemodel.android`) | 5M+ | 4.6 | 184,000 (184,307) | false | true | free | `enrich-interior-design-ai.log:L7` |
| AI Home Design: Interior DecAI (`ai.interior.design.home.renovation.app`) | 10M+ | 4.6 | 312,000 (312,423) | true | true | free | `enrich-interior-design-ai.log:L8` |
| Remodel AI - Home Design (`ai.reimage.remodel`) | 1M+ | 4.4 | 31,000 (30,954) | false | true | free | `enrich-interior-design-ai.log:L9` |
| Home AI - AI Interior Design (`interior.home.design.ai`) | 10M+ | 4.3 | 994,000 (994,289) | false | true | free | `enrich-interior-design-ai.log:L10` |
| Planner 5D: AI Home Design (`com.planner5d.planner5d`) | 50M+ | 4.4 | 586,000 (586,000) | false | true | free | `enrich-interior-design-ai.log:L11` |
| HomeGPT: AI Interior Design (`com.home.designer.aignite`) | 10K+ | 4.4 | 151 (151) | false | true | free | `enrich-interior-design-ai.log:L12` |
| Renovate AI : House Design (`app.renovateai`) | 100K+ | 4.5 | 4,490 (4,493) | false | true | free | `enrich-interior-design-ai.log:L13` |
| Home Planner AI: Room Interior (`com.icandesignapp.all`) | 10M+ | 4.0 | 241,000 (240,939) | false | true | free | `enrich-interior-design-ai.log:L14` |
| Renew - Old Photo Restoration (`com.renewphoto`) | 1M+ | 4.5 | 29,000 (28,996) | true | true | free | `enrich-photo-restorer.log:L7` |
| Relumi: Restore Old Photos AI (`com.repairitandroid.prd`) | 100K+ | 4.0 | 920 (920) | false | true | free | `enrich-photo-restorer.log:L8` |
| PhotoBoost - AI Photo Enhancer (`tap.photo.boost.restoration`) | 10M+ | 4.6 | 789,000 (789,015) | true | true | free | `enrich-photo-restorer.log:L9` |
| jpgHD: AI Photo Restore (`com.jpghd`) | 100K+ | 2.5 | 396 (396) | false | false | free | `enrich-photo-restorer.log:L10` |
| Restorer AI - Restore Photos (`com.middleton.imagecloneai`) | 1K+ | unrated | unknown | false | true | free | `enrich-photo-restorer.log:L11` |
| Data & Photo Recovery (`com.mobispeedy.recovery`) | 5M+ | 3.8 | 93,800 (93,792) | false | true | free | `enrich-photo-restorer.log:L12` |
| RestoreGo - Photo, Files (`com.restorego.photofiles`) | 1M+ | 4.8 | 5,630 (5,632) | true | false | free | `enrich-photo-restorer.log:L13` |
| DiskDigger photo/file recovery (`com.defianttech.diskdigger`) | 100M+ | 3.0 | 518,000 (518,406) | false | false | free | `enrich-photo-restorer.log:L14` |
| Invitation Maker: Cards & RSVP (`com.greetingsisland.sam`) | 10M+ | 4.9 | 192,000 (191,739) | true | true | free | `enrich-greeting-card-maker.log:L7` |
| Greeting & Birthday Card Maker (`com.desygner.greetings`) | 100K+ | 4.3 | 2,940 (2,937) | false | true | free | `enrich-greeting-card-maker.log:L8` |
| Invitation Maker Greeting Card (`vip.invit.anna`) | 1M+ | 4.3 | 5,300 (5,299) | false | true | free | `enrich-greeting-card-maker.log:L9` |
| All Invitation Card Maker App (`com.cardmaker.invitationcardmaker.birthdayinvitation.weddinginvitation`) | 5M+ | 4.2 | 3,040 (3,042) | false | true | free | `enrich-greeting-card-maker.log:L10` |
| Canva: AI Photo & Video Editor (`com.canva.editor`) | 500M+ | 4.8 | 27,400,000 (27,403,039) | false | true | free | `enrich-greeting-card-maker.log:L11` |
| Invitation Maker - Card Design (`com.textArtApp`) | 1M+ | 4.2 | 5,080 (5,081) | false | true | free | `enrich-greeting-card-maker.log:L12` |
| SimplyCards - postcards (`com.gresilab.simplycards`) | 500K+ | 4.4 | 16,500 (16,481) | false | false | free | `enrich-greeting-card-maker.log:L13` |
| American Greetings Ecards (`com.americangreetings.ecards`) | 50K+ | 4.2 | 2,020 (2,024) | false | true | free | `enrich-greeting-card-maker.log:L14` |

## Weak mid-tier named in the enriched top-eight samples

The tool defines weak ratings as below 4.2; the lists below name sampled apps
with real install buckets and ratings around 2.5–4.2, including the 4.2
boundary examples requested by the approximate range (which are not counted
as weak by the tool). They are not a claim that all results on a shelf were
enriched. Broad or adjacent apps are marked.

- **Shelf A:** Lensa (4.1, 10M+), PhotoDirector (4.2, 50M+), and Picsart
  (3.9, 1B+) in `ai product photo` (`enrich-ai-product-photo.log:L8`,
  `L10-L11`); Picsart (3.9, 1B+) and Pixelcut (4.2, 10M+) in `product photo
  editor` (`enrich-product-photo-editor.log:L8`, `L13`); Vendit - eBay Listing
  Tool (3.5, 1K+) and Pricing Tool for eBay Sellers (3.7, 1K+) in `ebay
  listing tool` (`enrich-ebay-listing-tool.log:L8`, `L10`). eBay Motors
  (4.0, 1M+) is adjacent motors content, not an eBay listing editor
  (`enrich-ebay-listing-tool.log:L9`). No rated 2.5–4.2 Etsy listing-maker
  app appeared in that query's enriched top eight.
- **Shelf B:** Price List Maker by Micra Solution (2.3, 50K+), Price List Maker
  - Fast & Easy (4.1, 100K+), and PriceSnap - Make Price List (4.2, 10K+) in
  `price card maker` (`enrich-price-card-maker.log:L9-L11`). Micra is below
  the approximate 2.5 lower edge but is retained as a distinctly weak rated
  result. `social media post maker` yielded Picsart (3.9, 1B+), Mysocial
  (4.1, 100K+), and Own. (4.1, 10K+)
  (`enrich-social-media-post-maker.log:L12-L14`); Picsart is a broad editor.
  The enriched top eights for `menu maker`, `flyer maker`, and `poster maker`
  had no app in this range (their lowest rated entries were 4.3 or higher,
  or unrated).
- **Shelf C:** Aragon.ai: Headshot Generator (3.1, 1K+) and AI Headshot &
  Photo Generator (4.0, 50K+) (`enrich-ai-headshot.log:L11-L12`); AI Pet
  Portraits - Luks (4.2, 10K+), DreamPets (4.2, 10K+), and Petdraw (4.0,
  1K+) (`enrich-pet-portrait.log:L8`, `L10-L11`); Home Planner AI (4.0,
  10M+) (`enrich-interior-design-ai.log:L14`); Relumi (4.0, 100K+), jpgHD
  (2.5, 100K+), Data & Photo Recovery (3.8, 5M+), and DiskDigger (3.0,
  100M+) (`enrich-photo-restorer.log:L8`, `L10`, `L12`, `L14`). Data &
  Photo Recovery and DiskDigger are broad recovery tools rather than clearly
  photo-restoration generators. For `greeting card maker`, All Invitation
  Card Maker App (4.2, 5M+), Invitation Maker - Card Design (4.2, 1M+), and
  American Greetings Ecards (4.2, 50K+) are in range
  (`enrich-greeting-card-maker.log:L10`, `L12`, `L14`).

## Price read

The only non-free listing price printed by the 112 enriched app rows was Etsy
SEO Visibility Booster: `paid ($9.99)` (`enrich-etsy-listing-maker.log:L11`).
No billing period was printed, so none is inferred. All other rows printed
`free`; that does not mean they lack IAP, as shown by the separate
`inAppPurchases` flag.

## What is still missing

| Missing evidence | What this session establishes | Single command / action to settle |
|---|---|---|
| More than the first eight app details per query | These readings enrich only the first 8 of visible search cards. | `node scripts/enrich-market.mjs --query "<query>" --concurrency 1 --top 30` (one query per invocation; stays within the 30-card display cap). |
| Market variation by storefront | This pass is `hl=en`, `gl=US`; no other storefront was measured. The current enrichment CLI does not expose a storefront flag. | `node --input-type=module -e "import { scanQuery } from './scripts/lib/play-scan.mjs'; console.log(await scanQuery('ai product photo', { hl: 'en', gl: 'GB' }))"` reads the alternative storefront's search/rating surface; an equivalent locale-aware detail enrichment command does not currently exist. |
| Exact install counts | Public Play exposes rounded buckets in this output, not exact installs. | No command in this public-listing toolchain can settle exact competitor installs; this requires a first-party/authorized Play Console or market-data source for the named app. |
| Revenue, conversion, retention, or willingness to pay | Store listing flags and ratings do not report these outcomes. | After a candidate ships, record a dated reading in `apps/<slot>/store/outcomes.json`, then run `node scripts/check-outcomes.mjs --slot <slug>`. |
| Meaning and billing period of the `$9.99` price | The listing reader prints `paid ($9.99)` but not billing period or paywall terms. | Open the Etsy SEO Visibility Booster Play listing (`com.shb.etsyoptimizer`) and inspect its purchase flow; `enrich-market.mjs` alone cannot establish billing period. |
| Complaint-level reasons behind weak ratings | No review text or complaint taxonomy was run. | For an individual sampled package, run `node scripts/scan-complaints.mjs --package <package-id> --top 4`; this was deliberately not run in bulk. |
| Product capability or relevance beyond the advertised title | Titles and search order do not prove app behavior; the `etsy listing maker` results also carried a partial relevance warning in the reason reread. | Inspect the individual Play listing and app for the named package; there is no one CLI command here that verifies capabilities. |

No `scan-complaints.mjs` bulk run was made.

## Most important expectation change

The price-card shelf is `mixed`, not the unqualified candidate suggested by
the weaker earlier price-list sample (`2026-10-16-money-ecosystem-ranking.md:474`);
the tool's only `candidate` verdict in
these 14 queries is `photo restorer`. That query also surfaced a concrete
reachable weak tier—jpgHD at 2.5 with 100K+ installs and Relumi at 4.0 with
100K+—alongside high-install broad recovery apps
(`enrich-photo-restorer.log:L8`, `L10`, `L12`, `L14`; verdict reason
`verdict-reasons.log:L13`). Flyer and poster queries, by contrast, both
returned `fortress` (`verdict-reasons.log:L7-L8`). This is a ratings/install
screen, not a revenue conclusion.

## Claim ledger

```text
CLAIM | LABEL | EVIDENCE
All 14 enrichment query invocations exited 0 at concurrency 2; none needed a retry | MEASURED | Each `.kiln/scratch/recon-2026-10-16/enrich-<slug>.log` ends with `exit 0`; command: `node scripts/enrich-market.mjs --query "<one query>" --concurrency 2 --top 8`
The enrichment surface was signed-out Play hl=en, gl=US | MEASURED | `scripts/enrich-market.mjs:540`; the JSON reason reread output is `.kiln/scratch/recon-2026-10-16/verdict-reasons.log`
The 14 query verdict labels are those quoted in the query tables | MEASURED | `verdict-reasons.log:L1-L14`; each line records query and verdict; initial labels are also in the corresponding `enrich-<slug>.log`
The named app install, rating, review, ads, IAP, and price values are the readings in the tables | MEASURED | Each row cites its exact `enrich-<slug>.log:L<n>` output line
Flyer and poster screens returned fortress; photo restorer returned candidate | MEASURED | `verdict-reasons.log:L7-L8` and `L13`
The shelf-level mixed readings summarize the query labels, not an independent tool score | INFERRED | Shelf A: `verdict-reasons.log:L1-L4`; Shelf B: `L5-L9`; Shelf C: `L10-L14`; the aggregation rule is stated above the shelf tables
No sampled storefront outcome establishes revenue, conversion, or retention | MEASURED | These output records contain ratings, reviews, install buckets, ads/IAP and listing-price fields only; source format: `scripts/enrich-market.mjs:275-293`
Exact competitor installs and Etsy SEO Visibility Booster billing period remain unknown | UNVERIFIED | Exact installs need an authorized competitor data source; billing period needs inspection of `com.shb.etsyoptimizer` purchase flow
```
