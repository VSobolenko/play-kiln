# Proposal: app-lawn-round-log

## Why this slot, why now

The owner selected the solo-trade round shelf on **2026-10-01** from the recon recorded in
`docs/ideation/2026-10-01-solo-trade-rounds.md`, and the owner's approval at checkpoint 1 is
recorded in the session that created this change. The evidence was independently re-read live by the
orchestrator on 2026-10-01 (`docs/ideation/2026-10-01-verification-of-evidence.md`); every
load-bearing row reproduced.

The slot is new and independent. Per `.cursor/rules/ideation-gate.mdc` § "The factory is not the
market" it is built from its own plan, its own promise contract and the shared harness only — no
sibling slot's code is read or reused.

## Demand evidence

Every claim is labelled. The full record is `docs/ideation/2026-10-01-solo-trade-rounds.md`
§2, and the independent re-read is `docs/ideation/2026-10-01-verification-of-evidence.md` §2.

- **MEASURED**: `node scripts/enrich-market.mjs --query "lawn care client tracker" --top 8` returns
  an **operator-side** audience with money on display — `com.lawnlove.pros` at **4.6★ over 1,230
  exact reviews (50K+ installs)** and `com.app.yardbook` at **4.6★ over 100K+ installs** — with a
  reachable weak mid-tier: `com.lawnguru` 3.5★/1,420, `com.lawnprosoftware.lawnproapp` 3.5★/10K+,
  `com.adamlyttle.mowbius` 3.8★/1K+.
- **MEASURED**: `node scripts/scan-complaints.mjs --package com.app.yardbook --package
  com.lawnprosoftware.lawnproapp --package com.adamlyttle.mowbius --package com.lawnbook.app` read
  18 reviews, 8 critical, **1 unclassified**. The three loudest classes are `broken` 3/8,
  `missing-feature` 3/8 and `paywall` 3/8 — and each is a property of a **cloud** product. Verbatim,
  re-confirmed live on 2026-10-01:
  - `com.app.yardbook` (4.6★, 100K+): *"Was good until it wasn't. Login hasn't worked in over a
    month. Always says incorrect password or Email, even when the exact same credentials work on
    b…"* and *"The app is missing some key features compared to the desktop/web version."*
  - `com.lawnprosoftware.lawnproapp` (3.5★, 10K+): *"They did a server upgrade and it wasn't done
    correctly, took everything down without warning for about 3 days"* and *"They deleted all my
    clients payment information."*
  - `com.lawnbook.app` (4.4★, 500+): *"80 dollars for a pretty-new app, for premium? that's kinda
    insane."*
- **COUNTER-EVIDENCE, recorded not buried**: six operator-shaped apps in the same shelf are
  **unrated** (`com.lawnsapp`, `io.lawn.app`, `app.mowmoney.com`, `lawn.care1`, the Mow Quote CRM
  package, `com.orangeforge.lawncarelog`). Whether that is a shelf that does not sustain a solo
  operator tool, or merely weak amateur supply, is **UNVERIFIED**. It is the largest single risk.
- The shelf's occupant count is **not** an argument against it: per
  `.cursor/rules/ideation-gate.mdc`, money on display is evidence of demand.

## Economics

- **Archetype:** `purchase_led_single_unlock`; the slot must enable the **`billing`** capability.
  The three rejected alternatives, each with its revenue reason, are in
  `docs/ideation/plans/lawn-round-log.economics.json` and restated in the recon §3.1.
- **Paying user:** a solo mower, or the owner of a two-to-three-person crew, running **20–150**
  recurring residential or small-commercial customers on a weekly or fortnightly round.
- **What is bought:** one permanent unlock — unlimited customers, the complete visit history, the
  per-customer owed balance, and CSV export.
- **Price:** **$9.99**. **Moment:** when the free tier fills up at **eight** customers, or the first
  time the history or the export is needed.
- **Arithmetic, with the division shown:**
  ```
  price                        $9.99
  divisor  (1−0.15)(1−0.98 …) = 0.85 × 0.98 × 0.98 = 0.81634
  net per unlock               $9.99 × 0.81634 = $8.16
  target                       $400 net / month
  unlocks / month              $400 ÷ $8.16 = 49
  installs / month @ 2 %       49 ÷ 0.02 = 2,450
  ```
- **Kill criteria:** `windowDays 90`, `minInstalls 2000`, `minRevenueUsd 300` — judged afterwards by
  `node scripts/check-outcomes.mjs --slot lawn-round-log`.
- **Honest ceiling, stated rather than implied:** a $1,000-net month needs **123 unlocks**, i.e.
  **≈6,150 installs/month** at the unmeasured 2 % conversion. The plan is not sold as a $1,000/month
  app.

## Distinctness

*The round is the operator's own offline record rather than a cloud account: the day's customers are
an ordered worklist he can reorder and push a day when it rains, one tap records mowed, skipped or
rained-off, a per-customer balance shows who still owes, and all of it opens and saves in airplane
mode with no login.*

Every clause answers a complaint a named user wrote, and each is a difference in **functionality and
content**, not in naming or colour (`docs/monetization-strategy.md` §1.2).

## Internal-catalogue check (Repetitive Content)

Checked against `registry/apps.json` **`id` / `name` / `idea` only**:

| Existing | Job | Verdict |
|---|---|---|
| `teacher-gradebook` (published) | private offline classroom record book | different category, different buyer, different data model, different report audience |
| `pipe-sizing` (killed) | gas pipe sizing workbook | burned trade-calculator direction; not touched |
| `smoke`, `pipe-proof` | throwaway pipeline proofs | not products |

No slot in the catalogue is a solo trade's round. **`pool-route-log` was screened in the same recon
and is deliberately NOT scaffolded with this one**: a pool service round and a mowing round are one
sentence — "a solo trade's round on the phone" — and two of our own slots that one sentence describes
are the exact internal near-duplicate the policy reaches. It stays unbuilt until this slot's outcome
is known.

## Non-goals

- No account, server, cloud sync, analytics, camera, location, ML or AI surface. The app's own
  manifest declares **no** `uses-permission`; the only network-capable component in the build is the
  Google Play purchase path, which the `billing` capability supplies and which contributes no
  permission of its own — measured from the built artifact rather than assumed.
- No ads, and no subscription at any point.
- No invoicing, no accounting suite, no accounting export beyond CSV; no invoice PDF.
- No map, routing or geocoding: a round is a list the operator orders, not a route on a map.
- No scheduling of new appointments — this is a recurring round, not a booking book.
