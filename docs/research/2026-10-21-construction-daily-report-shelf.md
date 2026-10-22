# Construction daily report shelf — measured 2026-10-21

Recon run for `/forge` on 2026-10-21. Query set: `.kiln/scratch/forge-2026-10-21-queries.json`.
Storefront: signed-out Play `hl=en`, `gl=US`.

## Ratings screen

**MEASURED** — `node scripts/scan-market.mjs --query-set .kiln/scratch/forge-2026-10-21-queries.json`:

| query | verdict | rated/total | weak | median |
| --- | --- | --- | --- | --- |
| construction daily report | **candidate** | 20/30 | 10 | 4.15 |

Why: 10/20 rated incumbents below 4.2 — demand proven, supply inadequate.

## Title hand-classification

**MEASURED** — `node scripts/scan-titles.mjs --query "construction daily report"` printed 30 visible cards (guard on-topic 24/30, share 0.80).

Hand buckets for the **daily site report / site diary / construction log** job (not estimating, not news, not payroll):

- **On-job (20/30, share 0.67):** Construction Daily Report (LEAN), Construction Daily Reports App, Construction Daily Log App, Site Diary, Daily Report: Construction AI, My Daily Report App, Raken, CDR Construction Daily Reports, ConstructionOnline, Site Report Expert, DailyReport, PlanRadar, Construction Forms & Templates, Site Daily Report Pro, Construction Manager, BuildReport Pro, DailyBuild, Daily Work Reports, Construction Daily Log, Construction Form Template PRO, Contractor Foreman, Onsite Construction App — counting conservatively as **20** after dropping pure estimators and news.
- **Off-job / adjacent (10/30):** Handoff Construction Estimator, Contractor+ Invoice, Construction Estimator App, Construction Expert A1 PRO, Construction News, Construction Material Tracker, Contractor Estimator, Miter Time & Pay, plus thin unrated trackers that are not daily reports.

Guard share 0.80 vs hand share **0.67** — both say the shelf is mostly the job; the hand read is the evidence used below.

## Economics behind the top cards

**MEASURED** — `node scripts/enrich-market.mjs --query "construction daily report" --top 8 --concurrency 3`:

| package | title | rating | reviews | installs | ads | IAP | price |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `com.lean.dailysitediary` | Construction Daily Report | unrated | — | 10+ | no | yes | free |
| `com.xamapps.docplan` | Construction Daily Reports App | 3.0 | 76 | 5K+ | no | yes | free |
| `com.snappii_corp.construction_daily_log_app` | Construction Daily Log App | 4.3 | 259 | 50K+ | **yes** | **yes** | free |
| `com.onebuild.handoff` | Handoff Construction Estimator | 4.8 | 655 | 10K+ | no | yes | free |
| `com.scriptandgo.sitediary` | Site Diary - Construction | 3.9 | 83 | 10K+ | no | no | free |
| `com.constructioncopilot.dailyreport` | Daily Report: Construction AI | unrated | — | 100+ | no | no | free |
| `com.snappii_corp.my_daily_report_app` | My Daily Report App | 3.3 | 109 | 50K+ | **yes** | **yes** | free |
| `com.raken` | Raken Construction Management | 4.2 | 2,630 | **100K+** | no | yes | free |

Money on display: Snappii pair at 50K+ with ads+IAP; Raken at 100K+ with IAP; weak mid-tier at 3.0★ / 3.3★ / 3.9★. Largest install bucket in the top eight: **100K+**.

## Complaints vocabulary

**MEASURED** — `node scripts/scan-complaints.mjs --query "construction daily report" --top 5` (Play renders ~3 reviews per page; vocabulary sample, not frequency):

Pocket: 15 reviews, 11 critical. Top classes: **paywall 45%**, missing-feature 27%, broken 18%, account 9%.

Verbatim load-bearing excerpts:

- `com.xamapps.docplan` 1★: *"This app costs $40/year, and there is no way to preview it before purchasing."*
- `com.xamapps.docplan` 2★: *"I have paid for yearly subscription and still after few days you have locked all the functions."*
- `com.snappii_corp.my_daily_report_app` 1★: multi-site pain — *"I go to 6 different locations and want to be able to do an activity log for each site"*
- `com.snappii_corp.my_daily_report_app` 3★: *"you can add photos but not captions… tag the locations for each entry."*
- `com.estimator.estimatorar` (adjacent): subscription cancel / billing complaints.

## Refusals from the same screen (not this recipe)

- **tattoo aftercare tracker** — candidate by ratings, but enrich showed mostly unrated 5+–100+ installs; titles flooded with AI tattoo generators. **Revenue refusal:** demand reading for *aftercare* is unproven; unrated thin shelf.
- **voice changer audio** — huge money (100M+ leader, ads+IAP on every top card) but fortress-grade ratings on leaders; entry needs a wedge stronger than “another voice FX app”. Deferred, not killed.
- **sheet music scanner** — still a strong pocket; already has an open proposal (`docs/ideation/2026-10-08-scorefolio.md`). Not re-proposed here.

## Honest limits

- Install buckets are Play’s rounded floors.
- Complaint histogram is a three-review sample per app.
- Raken / PlanRadar / Contractor Foreman lean enterprise; the reachable pocket is the weak mid-tier and subscription-hating solo / small-crew users, not a head-on attack on Raken.
