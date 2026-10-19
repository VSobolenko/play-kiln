# Plinth: Product Photos — proposal (2026-10-12)

One universal seller studio for marketplace listing photos: cut-out, backgrounds, marketplace rules
check, batch in one saved look, slide kit with seller-controlled text, listing text, export packs, shoot
coach, and an optional labelled Enhanced studio. Not a photo-in / photo-out generator.

**OpenSpec change:** `openspec/changes/app-seller-photo-studio` (implementation annex under
`annex/`). **Contracts:** `docs/ideation/plans/plinth-product-photos.{economics,differentiators,naming}.json`.
**Evidence:** `docs/research/2026-10-12-seller-platform-evidence.md`.

## Category and job

Category: photo / commerce tools for marketplace sellers. Job: take or import item photos, produce
cards that meet each marketplace's image rules, apply one look across a batch and a slide series, write
listing text, and get a pack off the phone.

## Wedge (one sentence)

Plinth keeps the item's own pixels untouched in Original mode and styles everything around it, checks
every image against the rules of the chosen marketplace, and sets a whole batch and an infographic
series in one saved look with seller-chosen text typeset on the device — which the marketplace editors
and the generic AI photo apps do not do together.

## Paying user, price, moment of payment

Paying user: a marketplace seller who lists items weekly. What is bought: consumable credit packs (90 /
230 / 600) plus one non-consumable unlock. Price hypothesis for checkpoint 1: $7.29 / $18.49 / $47.99
and unlock $28.99 (store-localised at runtime). Moment of payment: the first server operation after the
10 starter credits, with the cost shown before every paid action. Archetype:
`purchase_led_repeat_sales` (needs `billing`). Subscriptions are banned.

## Archetypes considered and rejected (revenue reasons)

| Archetype | Verdict | Revenue reason |
|---|---|---|
| `purchase_led_repeat_sales` | chosen | Provider cost is per use; revenue must scale with use |
| `purchase_led_single_unlock` | rejected | One-time price against recurring provider cost is an unbounded loss on heavy sellers |
| `ads_led_utility` | rejected | Sellers doing commerce work are ad-hostile; ads next to their goods damage trust; ad income cannot fund ~$0.08/credit |
| `purchase_only_no_ads` | rejected | Same shape as single unlock; cannot fund per-use cost |
| subscription | banned | Owner rule |

## Demand evidence

Hand-classified Play shelf (MEASURED, 2026-10-12): "product photos" search shows on-topic cards
(`Product Plus: AI Product Photo` 4.6★, and others); relevance guard 25/30 on-topic. Occupied shelf =
demand evidence, not a refusal (`.cursor/rules/ideation-gate.mdc`).

Incumbents as evidence (ON RECORD, evidence doc §Market evidence): Photoroom Pro ~$90/yr, Pixelcut Pro
~$96/yr; WB Jam from 22 990 ₽/30 days with Photo studio generations; WildCard from 299 ₽/month, refunds
only technical failures, regenerates background per slide. The unserved combination (INFERRED) is item
pixels preserved + background generated + one saved look across a batch + marketplace rules checked
before upload.

## Promise contract

Fifteen promises in `plans/plinth-product-photos.differentiators.json`, all `deferred` until proof.
Core: item pixels untouched; failed result never charged; auto-card by item; marketplace rules linter;
batch with saved Look; export pack per marketplace; slide kit series. Supporting: multi-import, shoot
coach, SKU grouping and scale reading, listing text in six languages, library search, batch-done
reminder, Look backup and resume, Enhanced studio tools. Job coverage: before = multi-import, during =
auto-card, after = export pack (connective `data_out`).

## Name

**Plinth: Product Photos** — slug `plinth-product-photos`, `applicationId` `com.gexetr.plinthproductphotos`.
Derived from a scored field of six candidates over three archetypes; rejected Vitrine, Shelfie and
Stagely for Play collisions (MEASURED scan 2026-10-12). Negative control: `Product Photo Editor`. Full
record: `plans/plinth-product-photos.naming.json`. A clean Play search is not trademark clearance.

## Revenue arithmetic (division shown)

Net factor **0.80** of price (Play fee + estimated tax haircut; **UNVERIFIED** until money-owner re-reads
`docs/monetization-strategy.md` §2.2 at spike S5). Provider ceiling **$0.025** per credit-unit.

| Product | Price | Credits | Net (×0.80) | Provider ceiling | Margin |
|---|---:|---:|---:|---:|---:|
| Starter pack | $7.29 | 90 | $5.83 | $2.25 | $3.58 |
| Studio pack | $18.49 | 230 | $14.79 | $5.75 | $9.04 |
| Pro pack | $47.99 | 600 | $38.39 | $15.00 | $23.39 |
| Unlock | $28.99 | — | $23.19 | — | — |

Price per credit on the starter pack: $7.29 / 90 = **$0.081**. Price/cost = 0.081 / 0.025 = **3.24 ≥
3.09** (§3.7 floor). Kill criteria: `windowDays 90`, `minInstalls 1000`, `minRevenueUsd 200` — an
off-switch, not a target (≈ 35 starter-pack sales in 90 days). Server cost: owner's existing VPS
(78.111.89.46) plus provider spend capped by `DAILY_SPEND_CAP_USD` and an emergency switch.

## Build estimate

**Estimate only, never a criterion** (owner 2026-10-08): several hundred agent-hours across spikes,
server, client, assets and verification. Milestones M0→M3 in `tasks.md`; one release after M3.

## Permissions and capabilities

Capabilities required: `billing`. Deliberately not enabled: `ads`. Server required: yes (wallet,
purchase confirm, model calls, fidelity guard, reports, delete). Permissions: `INTERNET` (server and
purchases); `CAMERA` (shoot coach, at point of use); `POST_NOTIFICATIONS` (batch-done, at point of use).
Photo Picker needs none.

## Device capabilities

Each capability is answered `used` or `not used`; a `used` line names the API and what the job loses
without it.

- camera: used — CameraX preview and capture for the shoot coach (`S-16`); without it the seller can
  only import existing photos and loses live framing, level and shot-list guidance. Needs
  `android.permission.CAMERA`.
- photo picker / gallery: used — `ActivityResultContracts.PickVisualMedia` (no runtime permission);
  without it the job has no way to bring existing item photos into a project.
- media playback or capture: not used — the job captures still photos; it does not play or record audio
  or video.
- microphone: not used
- location: not used — EXIF GPS is stripped from outputs; the app never reads location.
- motion sensors: used — `SensorManager` with `Sensor.TYPE_GRAVITY` for the coach level overlay; without it the framing
  guide still works but the seller loses the level cue. No extra permission beyond sensors already
  available to the app.
- contacts / calendar: not used
- notifications / reminder: used — `NotificationChannel` + WorkManager for batch-finished;
  `android.permission.POST_NOTIFICATIONS`; without it the seller must keep the app open to know a long
  batch is done.
- home-screen widget: not used
- share / export: used — `ACTION_SEND`, `FileProvider`, gallery save; without it the pack cannot leave
  the phone.
- Bluetooth / USB: not used
- files / SAF: used — `ACTION_CREATE_DOCUMENT` for ZIP export of a marketplace pack; without it the
  seller can still share single files but cannot save a full pack as a ZIP.

## Visual direction

A gallery plinth under soft top light: deep graphite fields, warm travertine surfaces, one verdigris
accent reserved for "item guaranteed" (original pixels / rules pass), one clay accent reserved for
credits. Mark: plinth silhouette under a lifted rounded square. Slot-owned `ProofRing`, `PlinthStage`,
glyph-only strip. Full direction: annex J.

## Kill criteria

`windowDays: 90`, `minInstalls: 1000`, `minRevenueUsd: 200`. Read by `node scripts/check-outcomes.mjs`.

## Honest unknowns

1. Whether sellers adopt a mobile app versus the marketplace's own editor.
2. Provider prices per Enhanced call (spike S2).
3. First-party marketplace rules for secondary presets (spike S5).
4. Whether the VPS has a domain and TLS (spike S0 / D9) — the only start-of-build owner question if missing.
5. On-device cut-out quality (spike S1) and fidelity thresholds (spike S2).
6. Exact net factor 0.80 and §3.7 floor (money owner at S5).
7. IPTC DigitalSourceType URIs and C2PA (spike S6) — labelled, not claimed from memory.

## Scaffold command (after owner approval at checkpoint 1)

```bash
node scripts/new-slot.mjs \
  --economics docs/ideation/plans/plinth-product-photos.economics.json \
  --differentiators docs/ideation/plans/plinth-product-photos.differentiators.json \
  --naming docs/ideation/plans/plinth-product-photos.naming.json \
  --spec app-seller-photo-studio
```

Then move this proposal and `plans/plinth-product-photos.*` into `apps/<slot>/docs/`
(`.cursor/rules/slot-docs.mdc`).
