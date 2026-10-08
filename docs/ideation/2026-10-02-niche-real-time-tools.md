# Rast — a microtonal tuner for the saz, bağlama and oud

Recon lane: **niche real-time tools — sound, timing and tempo**. One recipe only, proposed for
the owner's decision at human checkpoint 1. Written 2026-10-02.

- **Slug:** `rast-saz-oud-tuner`
- **Title:** Rast: Saz & Oud Tuner
- **applicationId:** `com.gexetr.rastsazoudtuner`
- **Price:** $4.99 one-time unlock
- **Archetype:** `purchase_led_single_unlock` (capability: `billing`)
- **Server:** none. Fully offline; the detector runs on the device's microphone.

The measurements behind every number below are in
`docs/research/2026-10-02-niche-real-time-tools-evidence.md`. Labels follow
`.cursor/rules/grounded-claims.mdc`: **MEASURED** / **ON RECORD** / **INFERRED** / **UNVERIFIED**.

## 1. Category and the job

**Real-time pitch measurement for a microtonal string instrument.** The app listens to the
instrument through the microphone and tells the player, in cents, how far each course is from the
target pitch of a named makam tuning — a live measurement, not a stored list. Its output is a
needle and a reading, not a table the user typed into.

This is inside the lane's hard rule: the primary value is a computational job performed in the
moment (listening and measuring), using the device's own audio hardware, with no server and no
record book, log, ledger, counter or checklist anywhere in the product.

## 2. The paying user, the unit, the moment

- **Paying user:** a saz/bağlama or oud player who already uses a free ad-funded niche tuner and is
  tired of two things — full-screen ads cutting a tuning session short, and the microtonal makam
  intervals (komma, perde) being rounded away by equal-tempered software.
- **What is bought:** one in-app purchase that unlocks every traditional tuning (Kara Düzen,
  Bağlama Düzeni, Misket, oud and makam sets), the perde/komma fret reference, the sustained drone
  tone generator, and removes all advertising for the life of the install.
- **Price:** $4.99 one-time.
- **Moment of payment:** the first time the player reaches for something the free tier does not
  read — a non-equal temperament, the drone, or simply an ad-free tuning session.

## 3. Archetype, and the archetypes rejected (each with a revenue reason)

**Chosen: `purchase_led_single_unlock`** → capability `billing`.

- **`ads_led_utility` — rejected.** The shelf is already ad-funded and free, and the loudest,
  most uniform complaint in the pocket *is* the ads (7 of 13 critical baglama-tuner reviews, 54%).
  Matching that model competes on the one axis users hate and monetizes at CPM rates that a
  microtonal instrument audience — small and geographically concentrated — cannot carry to $200.
- **`purchase_led_repeat_sales` — rejected.** There is nothing to re-sell. A player tunes with the
  same instrument for years; the tunings, the reference and the drone are static content, so a
  second and third purchase has no honest object.
- **`purchase_only_no_ads` — rejected.** A price set in Play Console with no discovery is a weak
  lever (`docs/monetization-strategy.md` §4), and a paid-up-front microtonal tuner would be
  out-competed by the free 1M+ incumbents before a single review exists. A visible free tier that
  reaches the microtonal tuning is the discovery path; the unlock is the conversion.
- **`subscription` — banned outright** (owner, 2026-10-01) and not considered.

## 4. Reasons this app can win (the promise contract)

Full list in `docs/ideation/plans/rast-saz-oud-tuner.differentiators.json` — 6 promises, 3 of them
`core`, every one anchored to a complaint a named user actually wrote:

1. `quarter-tone-accuracy` (**core**) — reads komma/perde intervals in cents and holds a lock on
   the low courses instead of reporting silence or freezing.
2. `no-ads-during-tuning` (**core**) — bought once, so no ad can ever cover the tuner.
3. `all-tunings-one-unlock` (**core**) — every traditional tuning inside the one purchase.
4. `offline-no-account` — works with no signal and no sign-in.
5. `drone-and-perde-guide` — a sustained reference drone and a microtonal fret reference.
6. `reads-low-courses` — a low-frequency detection mode for the thick saz and oud strings.

## 5. The wedge, in one sentence

> One paid tuner that reads the makam intervals in cents and keeps a stable lock on the low saz and
> oud courses while never showing an ad, with every traditional tuning included in a single one-time
> $4.99 unlock instead of split across a $15/week subscription or a full-screen ad every thirty
> seconds.

Every clause answers a recorded complaint: **against ads** (CherrySoft's oud tuner hit "with full
screen ads every 30 seconds"; 54% of critical baglama-tuner reviews), **against a weekly
subscription** (Perfect Tuner "ambushes you with a $15 a week subscription"), **against locked
tunings** ("Only 2 available tunings for free, the rest is paid"), **against accuracy loss**
("Accuracy is very bad"), and **against missed low strings** ("This app freez and dont hear my
saz"). It is a difference in **functionality and content**, never in naming or colour.

## 6. Name derivation

The field, the scores and the rejected alternatives are in
`docs/ideation/plans/rast-saz-oud-tuner.naming.json`: six candidates spanning five archetypes
(carrier_job_noun, borrowed_foreign_word, coined, descriptive, eponymous), the bare category noun
`Tuner` carried as the negative control at 9/100, and three rejected alternatives with reasons.
`Rast` is the foundational makam — specific to this music and ownable — followed by the real job
noun users type. `node scripts/scan-market.mjs --query "rast tuner"` returned `mixed` with **no app
titled Rast** on the visible page. **UNVERIFIED:** a class 9 trademark screen was not run.

## 7. Demand evidence, measured

`node scripts/scan-market.mjs` (controls `habit tracker` → **fortress**, `bjj training log` →
**dead-zone**, both holding) plus `enrich-market.mjs`:

| App (package) | Rating | Exact reviews | Installs | Ads | IAP | Price |
|---|---:|---:|---|---|---|---|
| `com.visne.baglamatuner` | 4.4★ | 8,020 | 1M+ | yes | yes | free |
| `com.baglamatuner` | 4.2★ | 2,560 | 1M+ | yes | no | free |
| `org.umitates.baglamatuner` | unrated | — | 100K+ | yes | yes | free |
| `com.kme.baglamatuner` | unrated | — | 50K+ | yes | yes | free |
| `com.saztunes.app` | unrated | — | 50K+ | no | yes | free |
| `com.visne.oudtuner` | 4.1★ | 2,970 | 500K+ | yes | yes | free |
| `com.kme.oudtuner` | 4.0★ | 158 | 10K+ | yes | yes | free |
| `tr.com.alyaka.alper.professionaloud` | 2.9★ | 456 | 100K+ | yes | no | free |

**Reading (MEASURED).** Demand is proven *on the niche-instrument apps themselves*, not only on
generic tuners: two saz/bağlama tuners at **1M+ installs each** and an oud tuner at **500K+ / 2,970
reviews**, all three at 4.1–4.4★ — below the 4.6–4.9★ the generic fortress holds. CherrySoft ships
both the 1M+ baglama tuner and the 500K+ oud tuner, free with ads and IAP, so this exact recipe is
already monetised. **Hand-classification** (`scan-titles.mjs`): the guard was **over-confident** on
`baglama tuner` (0.97 guard against **0.53** hand job-share) and `saz tuner` (1.00 against **0.67**)
— generic guitar tuners echoing the token "tuner" pad a third to a half of the shelf, so the
shelf's verdict is anchored to the enriched per-app facts above, never to a guard alone.

**Complaint corpus (MEASURED, `scan-complaints.mjs`).** The class every incumbent shares is
**ads**; then accuracy, forced accounts, offline failure and locked tunings. Quotes in §5. The
**unrated entrants at 50K–100K+** are a caution rather than the headline: several have tried this
niche, and the two rated leaders are still only 4.2–4.4★, which is where the wedge lives.

## 8. The arithmetic, with the division shown

| Step | Value |
|---|---|
| Price of the one unlock | **$4.99** |
| Play service fee 15% | −$0.7485 → $4.2415 |
| Refund allowance 2% | −$0.0848 → $4.1567 |
| Currency allowance 2% | −$0.0831 → **$4.0735 ≈ $4.07 net per sale** |
| Net target over the 90-day window | **$200** |
| Unlocks needed | 200 ÷ 4.07 = **49.1 → 50** |
| Installs at 2% install→unlock | 49.1 ÷ 0.02 = **2,457 → ≈2,500** |
| Installs per day over 90 days | 2,500 ÷ 90 = **≈28/day** |

The 15% / 2% / 2% deductions are plan assumptions from `docs/monetization-strategy.md` §2.2, **not
measurements of this account**; the 2% conversion is an assumption no slot here has ever measured.

## 9. Build, permissions, capabilities, server

- **Build estimate:** 4–6 agent hours (a single-module Compose app: a YIN/autocorrelation pitch
  detector, a tuning/temperament table, a ToneGenerator drone, one billing product).
- **Permissions:** `RECORD_AUDIO` only. No network, no storage, no location.
- **Capabilities:** `billing` (the archetype's requirement). Ads deliberately **not** enabled.
- **Server:** **none** — nothing is transmitted, and the app is offline by construction.
- **Kill criteria:** 90-day window, `< 2,500 installs` or `< $200` net revenue.

## 10. Visual direction

A single dark instrument-tuner surface, not a Material demo: a large live cents needle drawn by the
slot, a makam/perde strip that names the interval being played, per-course string tiles that light
as each is locked, and one amber accent reserved for "out of tune" — an identity declared in
`store/studio.json` at stage 1 and shared with the app's own `Color.kt`, per
`.cursor/rules/visual-identity.mdc`.

## 11. Honest unknowns

- Conversion (2%), retention and revenue are unmeasured; the store page shows a badge, not a P&L.
- Play install counts are rounded buckets; the review sample is Play's own three-per-page choice.
- Only the first eight cards per query were enriched; a larger incumbent may sit below that cut.
- `Rast` has not been trademark-screened in class 9.

## 12. Candidates refused, with the revenue reason

| Candidate | Verdict (MEASURED) | Why refused — revenue reason |
|---|---|---|
| bagpipe / chanter tuner | **fortress** (median 4.60) | Demand is real but the shelf is dominated by 4.6★ apps with thousands of reviews; no reachable weak mid-tier establishes a paying wedge. |
| sitar tuner | **fortress** (25/30 rated, median 4.50) | Same shape as bagpipes: high-rated incumbents, the wedge would be naming or colour, not functionality. |
| tabla metronome / odd-time metronome / drumline metronome | **fortress** | Metronome jobs are owned by 4.6★ incumbents; a rhythm trainer here has no functional wedge. |
| hangboard timer / climbing training timer | **fortress** (4.60 / 4.80) | The timing job is saturated by 4.6–4.8★ apps; no complaint pocket to answer. |
| swim tempo / rowing stroke rate / running cadence | **mixed**, thin | Pockets exist but the rated leaders are 4.2–4.5★ in very small shelves with fewer exact reviews than the saz shelf's single leader — weaker demand reading than the chosen pocket. |
| golf tempo trainer | **candidate**, but `com.golfbpm.golfbpmapp` is 3.5★ and forces an account | Demand unproven: the weakest incumbent is unrated/3.5★ and the shelf is 4.1★ median, so there is no priced, rated proof anyone pays. |
| pedal steel / harmonica / accordion tuner | **mixed**, 9 weak incumbents | Genuine pockets, but the enriched leaders carry fewer exact reviews than the saz/oud shelf and no single shared complaint as uniform as the saz ads complaint; kept as the runner-up, not proposed. |
| shruti box / tanpura app | **mixed**, guard POLLUTED | The drone job is real but the query is polluted by unrelated apps, so the demand reading cannot be trusted without a longer screen. |
| any generic guitar/violin/piano tuner, any HIIT interval timer | not screened as a target | Explicitly excluded by the lane: giant fortresses; a new entrant cannot buy share against 10M–50M+ installs. |
| step counters, jump-rope counters | excluded by the owner's brief | The "counter" shape the owner rejected. |

No candidate was refused for being on a populated shelf. Every refusal above names a demand,
price, wedge or build-cost reason.

## 13. What the owner is asked to decide

Approve the plan and the $4.99 price at human checkpoint 1. Until then no slot is scaffolded and
no code exists; the three contracts beside this file are the plan.
