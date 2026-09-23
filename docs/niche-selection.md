# Niche selection for the PlayKiln factory

Purpose: the repeatable procedure for deciding whether an idea earns a slot, and the evidence
gathered so far. Monetization is covered in `docs/monetization-strategy.md`; this document is
about **whether anyone wants the app at all**.

The factory's bottleneck is not building apps, it is picking the right ones. A slot costs days;
picking wrong costs the same days plus the policy exposure of a dead app in the catalog. Every
idea therefore passes this screen *before* `scripts/new-slot.mjs` runs.

## 1. The strategic constraint: one idea, one distinct product

Google Play's spam/repetitive-content policy (see `docs/monetization-strategy.md` §1.2) targets
**internal duplication** — several of our own apps that are near-identical in functionality,
content, and user experience. That rules out the obvious factory play:

> **Do not** ship five apps that are the same engine with a new palette and name.

Variety has to come from **different categories and genuinely different jobs**, not from
re-skinning one. Each slot must be able to state its distinct angle in one sentence before it is
scaffolded.

## 2. What the store actually signals (measured 2026-09-23)

Method: signed-out Google Play search, `hl=en&gl=US`, `?c=apps`. Seven queries were run and the
visible result set read directly. Query, then what the incumbents look like:

| Query | Visible results | Incumbent signal | Reading |
|---|---:|---|---|
| `electrician calculator` | ~30 | Leader: **5M+ installs, 49K reviews, 4.5★, "Contains ads · In-app purchases"**. Mid-tier weak: 3.5★ ($19.99), 3.8★ ($3.99), 3.5★, 4.0★ | **Candidate** |
| `visual schedule autism` | ~35 | **No institutional incumbent.** Ratings exist but many are poor: 2.0★, 2.5★, 2.9★, 3.4★, 3.7★, 3.8★, 3.9★; best is 4.8★; one paid app at $4.99 | **Candidate, with a monetization constraint** |
| `habit tracker` | ~28 | Top 20 nearly all 4.3–4.9★, from Google, TickTick, Leap Fitness, HabitRPG | Fortress |
| `shift work calendar` | ~30 | Top 14 almost all **4.6–5.0★** from dedicated indie shops; only outliers at 3.7★ and 2.8★ | Fortress |
| `fasting tracker` | ~19 | Mostly 4.4–4.9★; weak outliers at 2.7★, 3.7★, 3.8★; incumbents are subscription-funded | Fortress, needs subscriptions |
| `first aid guide` | ~27 | American Red Cross 4.8★, IFRC 4.4★, UN 4.5★, National Safety Council 4.3★ | Fortress (institutions) |
| `bjj training log` | ~29 | **About two thirds of top results show no rating at all** (pando, RollJournal, My Jiu-Jitsu, BJJ College, Strap, Fluyd, BetterBJJ, Roll IQ, …) | **Dead zone** |

### The signal that matters

**A rating on an incumbent is proof that the niche supports an app.** An app Play shows without a
rating has too few installs to compute one. So:

- Many unrated competitors + no clear leader = **many people tried and nobody got traction**. That
  is not "low competition", it is a category that does not sustain an app. `bjj training log` is
  the example: the naive "low competition = opportunity" heuristic would have picked it.
- A 5M-install leader with 49K reviews = the category is worth money, and the question becomes
  whether any *specific* pocket of it is reachable.

So the screen is **not** "few competitors". It is:

```text
Is demand proven?      Do incumbents carry substantial review counts?
   no   -> dead zone. Reject. (bjj training log)
   yes  -> Is the top reachable?  Do some mid-tier incumbents sit below ~4.2 stars,
                                  or is a sub-query unserved?
             no  -> fortress. Reject. (habit tracker)
             yes -> candidate. Screen for fit and distinctness. (electrician calculator)
```

Two secondary filters:

- **Monetization fit** — if the category is subscription-driven and we have deferred subscriptions,
  skip it for now. This is why `fasting tracker` is out despite being a real market.
- **Buildability** — the slot must be reachable by a Kotlin/Compose template with no backend.

## 3. The screening procedure

Run this for every candidate before scaffolding a slot.

1. **Seed the terms.** Type the idea into Play search and record what autocomplete offers; those
   are real queries, not guesses. Prefer feature and use-case phrasings over broad categories.
2. **Read the top ~30 results.** For each: rating, developer identity, price, and whether the
   listing shows ads or IAP.
3. **Count rated vs unrated incumbents.** If most are unrated, stop — dead zone.
4. **Open the top two or three listings.** Record install count and review count, and check the
   "Contains ads · In-app purchases" line. This is where proven demand is actually confirmed.
5. **Look for the weak pocket.** Mid-tier incumbents below ~4.2★, or a closely related sub-query
   whose results are all unrated while its parent category is proven.
6. **Write the distinctness sentence**: what a user gets here that they do not get from the
   existing apps. If it cannot be written, the idea is not ready.
7. **Check fit**: which capability does it need, and does it need subscriptions (deferred)?

### Traps this procedure exists to avoid

- **Low competition is not opportunity.** Unrated competitors mean failure, not vacancy.
- **Google Keyword Planner is web search, not Play search.** Its volumes do not describe store
  search demand and must not be used for it.
- **Result counts are display-capped.** ~30 visible results is a page limit, not a market size.
  Do not report it as a competitor count.
- **The store is personalised and geo-dependent.** A signed-out `en-US` session is a spot check.
  Our actual reachable market depends on the account's target countries.

## 4. Where the opportunity actually sits

### The pattern across all seven measurements

The fortresses and the openings separate along one line that is not obvious:

- **Fortresses** — habit tracker, shift calendar, fasting tracker — are categories where the
  *user is the kind of person who builds apps*: mass-market lifestyle tools with a huge audience
  and therefore heavy indie attention. Quality is high, so there is nothing to displace.
- **Openings** — electrician calculators, visual schedules for autistic children — are jobs whose
  *users are not developers*: tradespeople, parents, carers. Fewer people build for them, so the
  incumbents are amateurs with poor ratings despite proven demand.

**Heuristic: look for a job where the person doing the work would never build the app.** That is
where proven demand and weak supply coexist, which is the only combination worth a slot.

### Candidate A — specialised calculation tools for a trade or profession

- The leader — `Electrical Calculations` (Ettore Gallina) — shows **5M+ installs, 49K reviews,
  4.5★, "Contains ads · In-app purchases"**. Free-with-ads-plus-IAP is exactly the hybrid model
  in `docs/monetization-strategy.md`, and it is the market leader here.
- Its developer ships a **portfolio of adjacent, non-identical tools**: Photovoltaic
  Calculations, Lighting Calculations, Electrical Cost, Computer Science Calculations,
  ArduController. Different jobs, one developer, one code lineage. This is the strongest existing
  proof that the factory's model works on Play — and it is also the model's policy ceiling, since
  each app must stay genuinely distinct.
- Weak mid-tier incumbents exist in the same result set (3.5★ at $19.99, 3.8★ at $3.99, 3.5★),
  which is the reachable pocket rather than a head-on attack on the leader.

**Do not attack `electrical calculations` directly** — the leader has 49K reviews. The opportunity
is a specific adjacent profession, country, or sub-workflow where demand is proven and the
incumbents are weak.

Monetization fit: good. Ads plus a one-time unlock, no backend needed.

### Candidate B — assistive tools, with a monetization constraint

`visual schedule autism` is the weakest incumbent field measured: ratings exist all over the top
35, and many are poor (2.0★, 2.5★, 2.9★, 3.4★, 3.7★, 3.8★, 3.9★) with no institutional leader.
Demand is proven — the ratings exist — and the supply is visibly inadequate.

The constraint is the mandatory **Families Policy Requirements** when any declared target age
group includes children; **Designed for Families** is a separate opt-in programme. A
parents/carers-only declaration is not a safe shortcut where the app or listing uses
child-directed imagery or terminology: Play can override it, and misdeclaration risks removal or
suspension. The policy read now resolves the earlier uncertainty: **no ads is the low-risk product
choice, not a prohibition**. Ads remain possible only under the Families ads regime (including a
self-certified Ads SDK, child-appropriate non-interest-based ads, and the required formats).
IAP is also not prohibited on policy grounds, but it must use Play Billing and avoid emotionally
manipulative prompts, inadvertent taps, blurred virtual/real-money distinctions, and overly
aggressive commercial tactics; purchase consent remains a legal question. The current no-ads,
purchase-led shape is therefore unchanged as a product decision, not because ads are unavailable.
This remains a different, compliance-heavy slot shape; do not treat it as a quick experiment.
See [`docs/research/2026-09-23-autism-app-policy.md`](research/2026-09-23-autism-app-policy.md)
§§1, 3, and 4, which records the source links, including
<https://support.google.com/googleplay/android-developer/answer/9893335> and
<https://support.google.com/googleplay/android-developer/answer/9867159>.

Risks to respect:

- **Correctness is the product.** A calculation tool that is wrong is worthless and, in
  electrical or medical use, dangerous. Every formula must come from a published standard or an
  authoritative source, and cite it.
- **Policy ceiling.** Specialised tools from one account is exactly the shape the
  repetitive-content policy watches. Adjacent categories, genuinely different jobs, and a
  distinctness sentence per slot.
- **Weak incumbents at high prices cut both ways.** They may be weak because the market will not
  pay that much, not because they are badly built.
- **Verified absence of a fortress is not proof of revenue.** The autism field has no measurable
  money on display at all, unlike Candidate A where the leader's 5M+ installs are visible.

## 5. Status

This is a **standing workstream**, not a one-off decision. The factory will need it every time a
new idea is considered, and the plan is deliberately to try a varied set of apps rather than
many variations of one.

- **Both previously unverified leads are now tested.** Accessibility/assistive tools cleared the
  screen (Candidate B above). Offline reference did not: `first aid guide` is an institutional
  fortress. That is one instance of the archetype rather than the whole archetype — other
  offline-reference domains remain unmeasured. Candidate B's child-audience policy question is
  now sourced in `docs/research/2026-09-23-autism-app-policy.md`; its no-ads position is a
  low-risk product choice, not a Families-policy prohibition.
- Seven queries have been read. Two produced candidates, four were fortresses, one was a dead
  zone. **A hit rate of roughly one candidate in four is the current baseline expectation**, and
  is itself a reason to screen cheaply and in bulk.
- The screen is **manual**, by decision: automating it should wait until a live slot proves which
  parts of it actually predict anything.
- Not established: install-count thresholds, review-sentiment reading (only ratings were read),
  geo-specific demand, and any revenue estimate for a specific niche. Ratings were read from the
  result list; install and review counts were confirmed for the one leader opened in full.
