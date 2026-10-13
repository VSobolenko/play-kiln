## Context

See `proposal.md` for the motivation. Constraints that shape the approach:

- The factory's rules apply unchanged: spec-first, economics / differentiators / naming contracts before
  scaffold, two-phase release, device tests on the A40 (`R58MB3SGZVL`), no secret in the client, no
  subscription, scratch files only under `.kiln/scratch/`.
- The seam `PurchaseAuthority` and the pack type `ConsumablePack` with `Fulfillment.SERVER_COST` exist in
  `capabilities/billing/` (**MEASURED**, read 2026-10-12) and have **no implementation**; this slot builds
  the first one, as a server.
- Server host: VPS `78.111.89.46`, Ubuntu 26.04, 1 vCPU, 954 MiB RAM, Caddy and WireGuard installed
  (**MEASURED** earlier in this session). A domain name and a TLS certificate for it are **UNVERIFIED**; Android
  refuses cleartext, so this is a start-of-build blocker (decision D9).
- The Google Play Developer API from the server: `voidedpurchases` answered 200 and `products.get`
  answered 400 for a fake token (**MEASURED** earlier in this session), so the service account works.
- fal.ai: a pixel-preserving cut-out was measured with `fal-ai/bria/background/remove` (exact RGB kept,
  **MEASURED**). The pixel preservation of every other model is **UNVERIFIED** until spike S2.
- Evidence about competitors and rules (Photoroom, Pixelcut, WB editor, WILDCARD, Obliq, FlowDrop, Vinted,
  Avito, eBay, Etsy and others) was read 2026-10-12 and is **ON RECORD**; exact prices and sizes are
  re-read at spike S5 and from the marketplace's own pages when `marketplaces.json` is written.

## Goals / Non-Goals

**Goals:** a finished, deep product (before, during and after the core act); item fidelity as a provable
property; an authoritative server ledger; every marketplace rule as sourced data; a contract set that the
factory's gates can verify.

**Non-Goals (design level):** a general photo editor competing with Photoshop; a model host (the server
calls providers, it does not run models); accounts and cross-device sync; any rule logic in Kotlin that
belongs in `marketplaces.json`.

## Decisions

**D1. Two modes, one invariant.** Original mode composites with an alpha mask and keeps the interior
byte-identical; Enhanced mode is the only place a generative model touches the item. The invariant is a
unit test over a fixed fixture and is the proof of the core promise. *Rejected:* a single "AI cards" mode
(cannot promise fidelity, and Vinted and Avito forbid it).

**D2. Cut-out engine is decided by measurement.** Spike S1 compares an on-device option (ML Kit Subject
Segmentation, a beta artifact last updated 2023, **ON RECORD**; or a bundled model) with the server's
Bria cut-out on about 30 hard photos. If on-device passes, the quick path is free and offline; if not,
every cut-out is a server operation. The beta status conflicts with "latest stable only"
(`dependency-freshness.mdc`); the spike records whether a stable alternative exists. *Rejected:*
choosing by reputation.

**D3. Server shape.** One small service (Node or Go), SQLite in WAL mode, a bounded worker pool, the fal
queue API for long jobs, results held on disk with a short TTL. The client downsizes uploads to a
long side of about 2048 px. Endpoints: device session (integrity-checked), purchase confirm, balance and
ledger, create/poll job, report, delete data. *Rejected:* Postgres and a message broker (954 MiB RAM);
direct client calls to fal (puts a key in the APK, forbidden by `client-secrets.mdc`).

**D4. Ledger discipline.** Reserve, then settle or release. Every movement carries an idempotency key.
Purchase confirm verifies the token with the store, acknowledges or consumes it, and credits in one
transaction. A failed, empty, timed-out or guard-rejected job releases its reservation automatically.

**D5. Fidelity guard.** After every Enhanced result: colour difference and structural similarity inside the
source mask (after alignment), plus text comparison for items with printed text. Thresholds come from
spike S2, not from taste. One automatic retry, then reject and refund.

**D6. Model routing is configuration.** A server-side registry (id, operation, price, mode, version)
decides which provider model serves which operation, and can change without an app release. Initial
routes: Bria cut-out; local composition for Original; scene and relight models for Enhanced; a vision
model for recognition, OCR and listing text. Per-model prices and pixel preservation are **UNVERIFIED**
until S2 and S5 (the order of magnitude read so far is roughly $0.02–$0.04 for a standard card and up to
$0.08–$0.15 for premium generation).

**D7. Marketplace data, not code.** `marketplaces.json` in the slot (seeded from
`annex/marketplaces.seed.json`, schema in `annex/B-marketplace-presets.md`): canvas, ratio, mode, rules,
`sources`, `verifiedAt`. A project check fails any shown entry without `sources` or `verifiedAt`.
**Queue 1 (ships M1, rules seeded as secondary):** Wildberries, Ozon, Avito, Vinted, eBay, Etsy.
**Queue 2 (ships M2, rules seeded as secondary):** Depop, Amazon, Shopify.
**Unread stubs (never shown until a first-party or secondary read adds sources):** Poshmark, Mercari,
Mercado Libre, Shopee, Wallapop, Leboncoin, Kleinanzeigen, Facebook Marketplace, Yandex Market, Merchant
Center. Spike S5 raises `secondary` → `first-party` where a marketplace's own help is read.

**D8. Money.** Archetype `purchase_led_repeat_sales` (credit packs) plus one non-consumable unlock.
Credit-cost tiers: light 1 credit, standard 2, premium 5; a credit is budgeted at about $0.025 of
provider cost. Net per sale is about 0.80 of the price (15% store fee, refunds and currency about 5%;
**INFERRED**, replace with the exact §2.2 formula of `docs/monetization-strategy.md`). Starting
hypothesis for checkpoint 1: unlock $28.99; packs of 90 / 230 / 600 credits at $7.29 / $18.49 / $47.99;
10 starter credits per device. Arithmetic: a $7.29 pack nets about $5.83; with server-confirmed purchases
the price must be at least 3.09 times provider cost (§3.7), so the budget is $7.29 / 3.09 = about $2.36,
that is about 94 credits at $0.025, and the 90-credit pack fits. The price principle is the owner's: the
market's price minus a couple of percent. The anchors (Photoroom Pro about $7.50 per month billed yearly,
Pixelcut Pro about $8–10 per month, one-time desktop cut-out tools about $24–49) are **ON RECORD** from
earlier in this session and must be re-read at S5 before any price is fixed. *Rejected:* unlock only
(unbounded provider cost per heavy user); subscription (banned); ads (hurts trust with a seller's own
photos); unlimited server cut-out (every call costs money).

**D9. Domain and deployment are the first blocker.** Task 2.1 reads the Caddy configuration and the
available hostnames on the VPS. If there is no hostname with a certificate, that is the only question put
to the owner. How an agent deploys to the VPS (its own SSH access or a step done by the owner) is also
**UNVERIFIED** and is resolved in the same task.

**D10. Free-credit leakage.** Russian users can spend starter credits but cannot pay (billing is closed to
Russia, **ON RECORD**), so starter credits are few, bound to the integrity-verified device and capped by
the global daily limit. Russian presets are reach and a feature, not a revenue line.

**D11. Scope is staged, not shrunk.** M0 spikes, then M1 (end-to-end basic path plus server), M2 (seller
workflow), M3 (studio). One release after M3 unless the owner decides otherwise. Video cards,
marketplace-API publishing, accounts and sync are outside 1.0 because they need seller tokens or a
separate access model, not because of build time.

**D12. The contract stays deliverable.** The differentiators list is capped at 15 promises that each have a
proof (see `tasks.md` 1.3). A promise that cannot be proved is narrowed or dropped in writing. This
answers the factory's record of an over-large slot killed with a recorded reason
(`registry/apps.json`, `lumintra-light-planner`).

**D13. Slide text is never drawn by a generative model.** Every letter on a slide is typeset on the device
with bundled fonts (`annex/I-slide-kit.md`, capability `slide-kit`). This removes the "distorted
characters" failure WB names and that WildCard's own page documents. *Rejected:* letting the scene model
draw title plates.

**D14. Credits settle at first successful download.** A job reserves on create, releases on failure /
timeout / cancel / guard reject, and settles only when the client has received the first byte of the
result (`annex/D-server-api.md`, E55). A result that expires undelivered releases rather than settles.

## Implementation annex (read before coding)

The change's numbered decisions above are the *why*. The *what exactly* lives under `annex/` — screens,
API bodies, linter fixtures, edge cases, package layout, spike pass numbers. Read order:
`annex/README.md`. A builder who implements from `design.md` alone will invent numbers; a builder who
implements from the annex will not. Spikes that decide UNVERIFIED values are `annex/M-spikes-and-reads.md`.

## Risks / Trade-offs

- **Fidelity cannot be promised for generated results** → Original is the default, Enhanced is labelled and
  guarded, and the claim is limited to the guard's measured thresholds.
- **On-device cut-out may not be good enough** → D2 decides by measurement; the economics hold either way
  because the free allowance is then paid from starter credits.
- **A single small VPS is the single point of failure and a cost sink** → worker limit, queue, global
  spend cap and an emergency switch; a larger host is an operations decision, not a design change.
- **Provider prices and model behaviour change** → registry-driven routing; prices re-read at S5 and
  monitored by the spend cap.
- **Marketplace rules drift** → data with `verifiedAt`; a stale-preset report in the checks.
- **Play review of AI content and data handling** → in-app report and labels; Data safety declares photos
  as collected (transmitted off the device); `play-auditor` reviews before release. Whether a processing
  provider counts as "sharing" is **UNVERIFIED**.
- **Conversion is unknown** → no slot in this repository has a measured install-to-paid rate; the kill
  criteria are an off-switch, and the plan labels conversion **UNVERIFIED**.
- **Scale of the build** → the promise cap, the milestones and a vertical slice per promise inside its
  milestone keep every promise provable.

## Open questions that do not change the specs

- Exact pack prices and credit costs, fixed at checkpoint 1 with the money-owner role (hypothesis in
  annex J and `plans/plinth-product-photos.economics.json`).
- Retention maximum for server-side images; seed defaults are uploads ≤ 1 h and results ≤ 24 h
  (`annex/C-data-model.md`, `annex/H-compliance-and-provenance.md`); adjust after the VPS disk is read
  (S0).
- Server language is decided: Node + TypeScript + Fastify + better-sqlite3 + sharp (`annex/D-server-api.md`,
  `annex/K-package-and-test-plan.md`).
