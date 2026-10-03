# Recon evidence — a real-time tuner for the saz / bağlama (and oud), 2026-10-02

This file holds the measurements behind the recipe proposed in
`docs/ideation/2026-10-02-niche-real-time-tools.md`. It is the `evidenceRef` of
`docs/ideation/plans/rast-saz-oud-tuner.economics.json` and the `evidence.source` of every
promise in `docs/ideation/plans/rast-saz-oud-tuner.differentiators.json`.

Labels follow `.cursor/rules/grounded-claims.mdc`: **MEASURED** is a number or a quote this
session read from a command it ran; **INFERRED** follows from labelled facts; **UNVERIFIED**
is not checked, with the command that would decide it named.

## 1. Method and sensor calibration

Signed-out Play search through the factory's own sensors, run from `d:\Python\PlayKiln`. The two
calibration controls were run **in the same invocation** as every lane batch, as required:

```bash
node scripts/scan-market.mjs --concurrency 4 --query "habit tracker" --query "bjj training log" \
  --query "bagpipe tuner" --query "chanter tuner" --query "pedal steel tuner" --query "oud tuner" \
  --query "sitar tuner" --query "tanpura app" --query "shruti box" --query "tabla metronome" \
  --query "lehra app" --query "mandolin tuner" --query "banjo tuner" --query "accordion tuner" \
  --query "harmonica tuner" --query "polyrhythm metronome" --query "odd time signature metronome" \
  --query "drumline metronome" --query "hangboard timer" --query "climbing training timer" \
  --query "rowing stroke rate" --query "swim tempo trainer" --query "running cadence trainer" \
  --query "golf tempo trainer"
```

| Control | Required verdict | Observed verdict | Result |
|---|---|---|---|
| `habit tracker` | `fortress` | `fortress` | **calibrated** |
| `bjj training log` | `dead-zone` | `dead-zone` | **calibrated** |

Both controls held in the later name-screen and instrument-screen invocations too, so the lane
verdicts below are not sensor drift.

## 2. The lane screened (MEASURED)

Verdict, guard relevance and weak-incumbent count, from the invocation above.

| Query | Verdict | Guard | Rated | Weak | Median |
|---|---|---|---|---|---|
| bagpipe tuner | fortress | ok | 14/16 | 2 | 4.60 |
| chanter tuner | mixed | partial | 12/14 | 4 | 4.35 |
| pedal steel tuner | mixed | ok | 23/29 | 9 | 4.30 |
| oud tuner | mixed | ok | 19/30 | 7 | 4.50 |
| sitar tuner | fortress | ok | 25/30 | 5 | 4.50 |
| tanpura app | mixed | partial | 16/20 | 4 | 4.30 |
| shruti box | mixed | POLLUTED | 18/30 | 4 | 4.35 |
| tabla metronome | fortress | ok | 20/25 | 4 | 4.60 |
| harmonica tuner | candidate | ok | 15/30 | 9 | 4.10 |
| polyrhythm metronome | candidate | ok | 4/9 | 2 | 4.15 |
| odd time signature metronome | fortress | partial | 10/12 | 2 | 4.65 |
| drumline metronome | fortress | partial | 8/11 | 2 | 4.60 |
| hangboard timer | fortress | ok | 19/30 | 3 | 4.60 |
| climbing training timer | fortress | partial | 8/9 | 0 | 4.80 |
| rowing stroke rate | mixed | ok | 7/12 | 3 | 4.20 |
| swim tempo trainer | mixed | ok | 14/30 | 6 | 4.25 |
| running cadence trainer | mixed | ok | 17/29 | 4 | 4.50 |
| golf tempo trainer | candidate | ok | 16/29 | 8 | 4.15 |
| baglama tuner | mixed | ok | 24/30 | 5 | 4.45 |
| saz tuner | mixed | ok | 21/30 | 5 | 4.50 |
| microtonal tuner | mixed | ok | 7/9 | 2 | 4.60 |
| quarter tone tuner | mixed | partial | 29/30 | 7 | 4.40 |
| maqam tuner | fortress | ok | 7/14 | 1 | 4.70 |
| ney tuner | fortress | partial | 10/12 | 1 | 4.55 |

## 3. The pocket measured — the saz / bağlama (and oud) tuner shelf (MEASURED)

`node scripts/enrich-market.mjs --top 8 --concurrency 3 --query "saz tuner"`, and the same
command for `oud tuner`:

| App (package) | Title | Rating | Exact reviews | Installs | Ads | IAP | Price |
|---|---|---:|---:|---|---|---|---|
| `com.visne.baglamatuner` | Baglama Tuner: Bağlama & Saz (CherrySoft) | 4.4★ | 8,020 | 1M+ | yes | yes | free |
| `com.baglamatuner` | Baglama Tuner (Morfozz) | 4.2★ | 2,560 | 1M+ | yes | no | free |
| `org.umitates.baglamatuner` | Saz Tuner & Bağlama Lessons | unrated | — | 100K+ | yes | yes | free |
| `com.kme.baglamatuner` | Baglama Tuner - Bağlama / Saz | unrated | — | 50K+ | yes | yes | free |
| `com.saztunes.app` | SazTunes - Learn to play the saz | unrated | — | 50K+ | no | yes | free |
| `com.visne.oudtuner` | Ud Tuner - Oud Tuner (CherrySoft) | 4.1★ | 2,970 | 500K+ | yes | yes | free |
| `com.kme.oudtuner` | Oud Tuner - Tuner for Ud | 4.0★ | 158 | 10K+ | yes | yes | free |
| `tr.com.alyaka.alper.professionaloud` | Professional Oud | 2.9★ | 456 | 100K+ | yes | no | free |
| `com.tuningApps.oudTuner` | Oud Tuner | unrated | — | 100+ | no | no | paid $2.99 |

**Reading.** Demand is proven on the *niche-instrument* apps themselves, not only on generic
tuners: two saz/bağlama tuners hold **1M+ installs each** (8,020 and 2,560 exact reviews) and a
third oud tuner holds **500K+ at 2,970 reviews** — all three 4.1–4.4★, i.e. below the 4.6–4.9★
that the generic tuners hold. The same developer (CherrySoft) ships the 1M+ baglama tuner **and**
the 500K+ oud tuner, both free with ads and in-app purchases: the recipe of a niche-instrument
tuner is already monetised in this exact family. The shelf is not a fortress (its leaders are
4.2–4.4) and not a dead-zone (its leaders carry thousands of rated reviews).

**Hand-classification** (`node scripts/scan-titles.mjs`, the discipline the relevance guard
cannot replace — `docs/research/2026-09-24-market-decision.md` §2.1):

| Query | Guard on-topic | Hand job-share | Verdict |
|---|---|---|---|
| `baglama tuner` | 29/30 (0.97) | **16/30 (0.53)** | guard **over-confident**; half the shelf is generic guitar tuners echoing the token `tuner` |
| `saz tuner` | 30/30 (1.00) | **20/30 (0.67)** | saz-specific apps are the majority but generic tuners pad the rest |

So the *specific* shelf is roughly a third to two thirds of the visible cards; the rest are the
generic-tuner fortress (Soundcorset 50M+, DaTuner 10M+, Fender 5M+, Perfect Tuner 10M+,
Pro Guitar Tuner 10M+) that appears for any tuner query. The saz-specific field is
**two rated 1M+ apps, one rated 500K+ app, and five unrated entrants at 50K–100K+** — proven
demand with a genuine weak mid-tier, plus the caution that several have already tried.

## 4. The complaint corpus (MEASURED)

`node scripts/scan-complaints.mjs` over the shelf and directly over packages. The complaint they
**all** share is ads blocking the tuner; accuracy, forced accounts, offline failure and locked
tunings follow.

**Ads — the top class across the whole tuner category.**

- `com.jrinnovation.proguitartuner` (Pro Guitar Tuner, 4.1★, 143,610 exact reviews, 10M+): *"Ads are terrible on this app. It used to be that you could tune one or two strings before interruptions by mandatory ads"* · *"Good tuner, BUT … within a matter of seconds, full page ads pop up, blocking the tuner."*
- `com.visne.oudtuner` (Ud Tuner - Oud Tuner, 4.1★, 2,970 reviews, 500K+): *"Not working consistently. Cant use the app continuously because i ak hit with full screen ads every 30 seconds that are not only annoying, but also cau[sing]…"*
- `com.bork.dsp.datuna` (DaTuner, 4.1★, 455,190 reviews, 10M+): *"nothing but ads for ANYTHING you try to do."*
- Complaint histogram for `baglama tuner`: **7 of 13 critical reviews were ads (54%)**, with further classes broken 2, crash 2, paywall 2, regression 2, account 1, accuracy 1, offline 1, value 1.

**Accuracy / quarter-tone reading.**

- `com.visne.oudtuner`: *"Accuracy is very bad. Only 2 available tunings for free, the rest is paid and probably with the same mediocre accuracy."*
- `tr.com.alyaka.alper.professionaloud` (Professional Oud, 2.9★, 456 reviews, 100K+): *"E tone is wrong, this is not an E tone, please check, and try to let sound has frequencies, not just solid tone."*
- `com.tortato.tunerwear` (Tuner Wear, 3.8★, 100K+): *"The accuracy was honestly the worst."*

**Locked tunings / paywall.**

- `com.visne.oudtuner`: *"Only 2 available tunings for free, the rest is paid."*
- `pl.netigen.toolstuner` (Perfect Tuner, 4.1★, 87,501 reviews, 10M+): *"This app ambushes you with a $15 a week subscription every time you try to use it."*

**Detection failure — the instrument is not heard.**

- `com.baglamatuner` (Baglama Tuner, 4.2★, 2,560 reviews, 1M+): *"Not detecting any sounds"* · *"This app freez and dont hear my saz."*

**Forced account / offline.**

- `com.golfbpm.golfbpmapp` (3.5★, 34 reviews, 10K+): *"Makes you create an account and doesn't even offer a free version."*
- `com.jrinnovation.proguitartuner`: an `offline` complaint (needs a connection).

## 5. Revenue arithmetic (INFERRED from the measured shelf + repo plan assumptions)

The division, shown:

| Step | Value |
|---|---|
| Price of the one unlock | **$4.99** |
| Play service fee, 15% | −$0.7485 → $4.2415 |
| Refund allowance, 2% | −$0.0848 → $4.1567 |
| Currency-conversion allowance, 2% | −$0.0831 → **$4.0735 ≈ $4.07 net per sale** |
| Net target over the kill window | **$200** (`minRevenueUsd`) |
| Unlocks needed | 200 ÷ 4.07 = **49.1** (50) |
| Installs needed at **2%** install→unlock | 49.1 ÷ 0.02 = **2,457 ≈ 2,500** |
| Installs per day, 90-day window | 2,500 ÷ 90 = **≈28/day** |

The 15% / 2% / 2% deductions are the plan assumptions carried from `docs/monetization-strategy.md`
§2.2, not measurements of this account. The 2% install→unlock conversion is an assumption; no
slot in this repository has ever measured a real conversion rate.

## 6. Name derivation (MEASURED screen, UNVERIFIED clearance)

- `node scripts/scan-market.mjs --query "rast tuner"` returned `mixed`, guard `partial` — generic
  tuners, **no app titled "Rast"** on the visible page.
- The other candidate carriers were screened and rejected: `perde tuner` (mixed, but "Perde"
  transliterates to a vulgar stem in ru), `tembra` (`node scripts/scan-titles.mjs --query "tembra"`
  returned near-collisions `Temba`, `Tembera`, `Teambra`), `koma tuner` (mixed; "Koma" is "coma"
  in ru), `makam tuner` (fortress and homonym-polluted — Play returns Quran-recitation "maqam"
  apps).
- **UNVERIFIED:** a trademark screen of "Rast" in class 9 was not run; the deciding commands are
  USPTO / EUIPO eSearch plus / WIPO Global Brand Database searches for `Rast` in class 9. A clean
  Play search is not clearance.

## 7. Honest limits

- Install counts are Play's rounded buckets (`1M+` spans 1,000,000–9,999,999).
- The review sample is Play's choice of three per page; the complaint histogram describes the
  *vocabulary* of the pocket, not complaint frequency in its population.
- Only the first eight cards per query were enriched; a larger incumbent may sit below that cut.
- The relevance guard was **over-confident on `baglama tuner` (0.97 guard against 0.53 hand
  job-share)**; every shelf verdict here is anchored to enriched per-app facts or review text, not
  to a verdict alone.
- Nothing here measures conversion, retention or revenue; the store page shows a badge, not a P&L.
