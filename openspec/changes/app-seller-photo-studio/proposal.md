## Why

Sellers on marketplaces (WB, Ozon, Avito, Vinted, eBay, Etsy, Poshmark, Depop, Mercari and others) need
every item photographed, cut out, set on a fitting background, checked against the marketplace's own
image rules and exported, many times a day. The leaders (Photoroom, Pixelcut, the WB editor) sell this as
a monthly subscription, and their own users complain about three things the factory can answer: no plain
edits (rotate, crop, exposure), a "look" that cannot be reapplied weeks later, and a credit charged when
the result is empty (**ON RECORD**, Reddit threads read 2026-10-12). Resellers add a second job the
leaders do not serve end to end: group the photos per item, read the scale, SKU and ruler, write the
listing in the marketplace's language, and export a pack per marketplace (**ON RECORD**, FlowDrop and
similar pages read 2026-10-12). The owner chose this slot on 2026-10-12: one universal platform, not a
photo-in photo-out generator.

## What Changes

A new slot, a server-backed Android app (`com.gexetr.plinthproductphotos`; name **Plinth: Product Photos**,
slug `plinth-product-photos`, derived in `docs/ideation/plans/plinth-product-photos.naming.json`), that
does the whole seller photo job:

- **The constitution:** the item is sacred. *Original* mode never changes a pixel inside the item's mask;
  *Enhanced* mode may generate around and on the item, is labelled as AI, and passes a fidelity guard. A
  failed result never costs a credit.
- Cut out (free on-device if a measured spike proves it, otherwise server), place on backgrounds with
  shadows, compose to the marketplace's canvas, edit by hand, and export.
- One-tap **auto-card**: a vision model names the item and its category, picks a scene, shadow and
  composition, checks the marketplace rules and exports.
- **Marketplace presets and a rules linter** driven by data (`marketplaces.json`), each preset with a
  source URL and a verification date.
- **Batch, saved Looks, SKU grouping, scale/SKU/ruler reading, listing text in six languages, a shoot
  coach, a searchable library, reminders, backup.**
- **Generative studio** (relight, expand, eraser, upscale, recolour, ghost mannequin, virtual model).
- **Server:** the factory's first `PurchaseAuthority`. The server confirms Play purchases, holds the
  authoritative credit ledger, calls fal.ai with a key that exists only on the server, and enforces cost
  caps and image retention limits.
- Six interface languages: en, ru, de, fr, es, pt-BR.

**Monetization (decided; prices are a starting hypothesis for checkpoint 1):** `purchase_led_repeat_sales`
with consumable credit packs (`Fulfillment.SERVER_COST`) plus one non-consumable unlock. No subscription,
no ads. Paying user: a reseller or small seller listing items weekly. Price principle (owner): the
market's price, a couple of percent lower. Moment of payment: the first server operation after the free
starter credits. Kill criteria: `windowDays 90`, `minInstalls 1000`, `minRevenueUsd 200`, an off-switch and
not a selection criterion.

**Non-goals for 1.0:** video cards, publishing through marketplace APIs, accounts, multi-device sync, a
web version, ads, subscriptions, and any client-side secret.

**Contract clarifications:** Original fidelity applies only while item-region pixels receive no tonal or
generative edit; local tonal edits are labelled manually adjusted, procedural backgrounds are not called
AI-generated, and `original-only` follows each preset's sourced allow-list. Quick cut-out ships only if its
spike selects a stable local engine. Free entitlement is a watermarked export with a 5-item batch limit;
the unlock removes those limits while credits continue to fund server work. Purchases, result delivery,
starter grant and deletion use the durable/revoking contracts in design D17–D18.

## Capabilities

### New Capabilities
- `card-pipeline`: import, cut-out, backgrounds, shadows, composition, the Original/Enhanced fidelity
  modes, the manual editor and the one-tap auto-card.
- `marketplace-presets`: data-driven marketplace presets, the rules linter and the "fix for marketplace"
  action.
- `batch-and-library`: batch queue, saved Looks, SKU grouping and reading, library, search, reminders and
  backup.
- `shoot-coach`: camera capture with live framing, blur and exposure guidance and per-item shot lists.
- `studio-ai`: generative tools, the fidelity guard and the AI-content disclosure and reporting.
- `credits-wallet-server`: credit packs, the unlock, the purchase authority server, the ledger, cost
  caps, privacy and deletion.
- `listing-and-export`: listing text in six languages, export formats, ZIP per marketplace, share and
  clipboard handoffs.
- `slide-kit`: infographic series in one saved style, seller-controlled text typeset on the device,
  prohibited-text check before render (`annex/I-slide-kit.md`).
- `provenance-and-compliance`: provenance badges and metadata, in-app AI report, privacy facts, gated
  virtual model (`annex/H-compliance-and-provenance.md`).

### Modified Capabilities

(none)

## Impact

- New slot `apps/<date>-plinth-product-photos/` (app plus `server/`), scaffolded by
  `scripts/new-slot.mjs --economics … --differentiators … --naming … --spec app-seller-photo-studio`
  after checkpoint 1. Implementation detail is in `annex/` (read `annex/README.md` first).
- The `billing` capability is enabled and its seam `PurchaseAuthority` gets its first implementation, in
  the slot's `server/`. Nothing under `capabilities/` is edited by this change.
- External services: fal.ai (server side only), the Google Play Developer API (server side only). Service
  cost is in the revenue arithmetic of `design.md`.
- Permissions: Photo Picker none, `CAMERA` for the coach, `POST_NOTIFICATIONS` only when a batch is
  started.
- Evidence labels follow `.cursor/rules/grounded-claims.mdc`; every UNVERIFIED item is listed in
  `design.md` with the spike that decides it.
