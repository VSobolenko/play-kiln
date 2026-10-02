# Monetization strategy for the PlayKiln factory — 2026-09-23

Goal: earn money by shipping a portfolio of apps and measuring which ones deserve
more investment. This document fixes the verified constraints, the **spectrum of
things an app can sell** (§3), the rule for choosing among them per app (§3.6), and
the revenue **and cost** arithmetic (§2) — because all of these shape app design rather than
being bolted on afterwards.

Monetization is **not** a single factory-wide default. It is chosen per app from the
spectrum in §3, and **purchases are emphasised over ads**: ads stay in the toolbox,
but they lead only for apps that have nothing to sell. §0 records why.

## 0. Decisions taken (2026-09-23)

Recorded so later sessions do not re-litigate them.

| Decision | Choice | Consequence |
|---|---|---|
| Monetization model | **Per app, chosen from a spectrum** (§3), never a fixed default. **No model is emphasised over another** — purchase, consumable, ads and hybrid are equally first-class | Every slot's plan must name its archetype, show which alternatives it considered, and justify the choice in revenue terms (§3.6, `docs/slot-economics.md`). A slot that leads with ads must also answer "what does a rewarded view unlock" *before* its UI is designed |
| Why there is no house default | The owner's past commercial success came from apps **without ads but with in-app purchases of content and elements**, and that history was written down as "purchase-led is the starting hypothesis" — which then **became** the default it was never meant to be: every proposal, recipe and plan in this repository chose `purchase_led_single_unlock` | The history is evidence that purchases *can* work, not a prior that they *will*. It is one data point about one owner's catalogue, not a finding about a new app's audience. State a preference only when the app's own evidence produces it |
| Purchase kinds in scope | **One-time unlocks** and **spendable consumables (token/credit packs)** — an open-ended in-app store, not only "remove ads"; ads as a parallel stream | Packs are consumables, never entitlements (§3.2); "remove ads" is a one-time unlock (§3.1) |
| Subscriptions | **Banned** (owner, 2026-10-01) | Out of scope: recurring entitlement needs server-side validation plus a renewal/refund/cancellation/grace-period lifecycle, which makes the slot a server project. `subscription` is not in `ARCHETYPES`, so a plan naming it fails `check-economics.mjs` (§3.3) |
| Child-directed apps | **No ads by default; paid app, IAP, or policy-compliant ads** | The archetype cannot be a factory-wide constant: a child-directed ads configuration must satisfy the Families ads regime, while no ads remains the low-risk product choice (§3.5) |
| Remote configuration | **Firebase Remote Config** | Ad unit ids, feature flags, pack grants, and experiment arms change without a release; needs a Firebase project and `google-services.json` per slot (both gitignored) |
| App verification | **On a real device/emulator, after an AdMob account exists** | The ads and billing adapters are compiled and unit-tested but **not** yet verified against live services |
| Next work | **Niche and demand analysis first**, code second | No slot is scaffolded until an idea passes the distinctness and demand checks (`docs/niche-selection.md`) |

Blocking human actions, in order:

1. **Confirm production access** in Play Console (see §1.1) — it decides whether a slot can
   earn in 7 days or must wait ≥ 14 days with 12 opted-in testers.
2. **Create/locate the AdMob account** and produce per-slot ad unit ids. Ad unit ids are
   public identifiers, not secrets, but they are per-app and cannot be invented. Until then
   every ad path is exercised against `FakeAdsController` with Google's test unit ids — and,
   per the decision above, on-device verification of ads *and billing* is sequenced after
   this account exists.
3. **Create the Firebase project** and add each slot's `google-services.json` locally.
4. **Decide the slot's archetype** (§3.6) while planning it. A purchase-led slot that sells
   money-costing credit also needs a server decision before its paywall is designed (§3.7).

## 1. Two hard constraints that decide the strategy

### 1.1 Production access gate (account-scoped question, must be confirmed)

Personal Play Console accounts created **after 2023-11-13** must run a closed test
with **at least 12 testers opted in continuously for 14 days** before production
access is granted. Internal testing does not count. Testers who drop out and return
restart their own 14-day clock. Production review adds up to ~7 days.

Consequences, if this applies to our account:

- each app costs **≥ 14 days before it can earn anything**;
- the 14-day windows for several apps can **overlap**, so the correct move is to
  batch app creation rather than serialize it;
- tester recruitment and engagement become a real operational cost, not a formality.

If the account predates 2023-11-13, or production access was already granted, this
gate does not apply per app. **This is unconfirmed and must be checked in Play
Console before planning a batch.**

Sources: <https://support.google.com/googleplay/android-developer/answer/14151465>,
<https://support.google.com/googleplay/android-developer/answer/9845334>.

### 1.2 Repetitive-content policy — the existential risk to a factory

The Spam policy states:

> **Repetitive Content** — We don't allow apps that merely provide the same
> experience as other apps already on Google Play. Apps should provide value to
> users through the creation of unique content or services.
> Examples of common violations: … Creating multiple apps with highly similar
> functionality, content, and user experience. If these apps are each small in
> content volume, developers should consider creating a single app that aggregates
> all the content.

Enforcement escalates: removals and suspensions count as **strikes**; multiple
strikes can **terminate** the account; on termination **every app in the catalog is
removed** and related accounts are permanently suspended.

This is the largest structural risk to a factory model, but it is narrower than "many apps is
bad". The policy targets two distinct things, and only one of them is about volume:

- **Copying other developers' apps** — shipping a slot that merely reproduces an app already
  on Play adds no value and is a violation on its own. This is the case a genuinely new design
  or a function nobody else has solves.
- **Internal duplication** — several of *our own* slots being near-identical to each other in
  functionality, content, and user experience. Five calculators with different icons is the
  example the policy names; a calculator, a calendar, and a flashlight are not.

So the operating rules are:

- A slot must state its **distinct angle in one sentence** before it is scaffolded: the thing a
  user gets from it that they do not get from the existing apps in that category. If that
  sentence cannot be written, the idea is not ready.
- **Within our own catalog, no two slots may be near-identical.** That is the check that
  matters for a factory, and it is enforceable.
- The factory must **retire** weak slots deliberately. A growing catalog of near-identical dead
  apps is exactly what the policy describes, and it is wasted maintenance as well.
- The strongest distinctness levers are functionality and content, not colour and naming. A
  rename plus a new palette is the pattern to avoid; a new capability is not.

Sources: <https://support.google.com/googleplay/android-developer/answer/9899034>,
<https://support.google.com/googleplay/android-developer/answer/9899234>,
<https://support.google.com/googleplay/android-developer/answer/9898783>.

## 2. Revenue arithmetic

Two streams reach an app and their arithmetic is not the same. **Ad** revenue per day is
`DAU × ARPDAU` and has no per-request cost to subtract. A **purchase** does have one, and its
price must recover that cost plus every deduction taken before the money reaches us — which is
why a multiple of unit cost is not a margin. Ad arithmetic first (§2.1), then the purchase
arithmetic that prices the packs in §3.2 (§2.2).

### 2.1 Ad revenue arithmetic

Revenue per day = `DAU × ARPDAU`. Published 2026 benchmarks by format (ads only, not our own
measurements):

| Format | Tier-1 eCPM | Global eCPM |
|---|---:|---:|
| Banner | $0.50–1.50 | $0.20–0.80 |
| Interstitial | $5–8 (video up to ~$14) | $2.50–5.00 |
| Rewarded video | $15–40 (US Android ≈ $16.49) | $8–18 |

Two multipliers matter as much as the format:

- **Mediation beats a single network** by roughly **40–60 %** of total ad revenue.
- **Rewarded video** adds roughly **40–68 %** to total ad revenue and does **not**
  measurably hurt in-app-purchase conversion, because it is opt-in.

**Illustrative example** (published figures, not a forecast for any slot): 64 % opt-in, 2.1
rewarded views per DAU at a $24 blended eCPM yields ≈ **$0.05 per DAU per day**, i.e. ≈
$2,580/day at ~51 k DAU.

Illustrative order-of-magnitude implications for **$1,000/month** (≈ $33/day), derived from
the benchmarks above:

| Monetization posture | Roughly required DAU |
|---|---:|
| Rewarded-centric | ~700–1,000 |
| Interstitial + banner | ~2,000–6,000 |
| Banner-only | ~11,000+ |

The strategic read: **rewarded video is worth roughly an order of magnitude more per
user than banners**, so an ads-led app must be *designed* with something worth unlocking
— and an app with nothing worth unlocking is a poor ads fit (§3.4). Retention is the
other multiplier, since `DAU = installs × retention`.

These are order-of-magnitude estimates from published publisher benchmarks, not
forecasts for a specific app.

Sources (ads): <https://www.playwire.com/blog/admob-ecpm-benchmarks-what-publishers-should-expect>,
<https://coinis.com/glossary/rewarded-video>,
<https://www.applixir.com/blog/web-rewarded-video-ad-performance-in-2026-the-mid-year-verdict/>.

### 2.2 Purchase arithmetic: a multiple of unit cost is not a margin

A purchase carries a cost an ad impression does not: someone must be paid to serve the thing we
sold. For any product whose fulfilment costs money per unit — a credit pack, a generation, a
metered API call — the price must recover that cost **and** every deduction taken before the
money reaches us. **Margin is what survives those deductions**; a multiplier on unit cost says
nothing about it on its own. This is not AI-specific: it holds for any metered backend, and
`C_unit` is the only provider-specific input.

The relationship is a divisor, not a multiplier:

```
                 C_unit
P_min = ─────────────────────────────────────────────
        (1−F_fraud)(1−F_billing)(1−F_refund)(1−F_fx)(1−M_target)
```

- `C_unit` — provider cost to fulfil **one** unit of credit, in USD.
- `F_fraud` — share of **granted** credit for which no payment is ever collected.
- `F_billing` — the Play service fee (plus the separate billing fee).
- `F_refund` — refunds and chargebacks on paid transactions.
- `F_fx` — FX and payout rounding between the listed price and what we receive.
- `M_target` — margin we want on what actually arrives.

Each `(1−F…)` is a share of the sale we never keep, and `M_target` is taken *after* all of them.
The required multiple is `K = 1 / [the divisor]`, so `P_min = K × C_unit`; quote `K` beside any
pack price, because `K` — not a round 2× or 3× — is what the assumptions actually produce.
`P_min → ∞` as any factor approaches 1, so a hopeful input does not lower the price; it makes
the business disappear.

**A 2× markup is roughly a 25 % margin, not 50 %.** Set the price at twice `C_unit` and the
formula gives the margin that price implies:

```
non-margin factors = (1−0.20)(1−0.15)(1−0.02)(1−0.02)
                   = 0.80 × 0.85 × 0.98 × 0.98 = 0.653

K = 2  ⇒  1 / (0.653 × (1−M)) = 2  ⇒  1−M = 0.766  ⇒  M ≈ 23 %
```

The verified research states this as **~25 %**; either way the headline is the same. Pricing at
twice unit cost keeps about a quarter, because the fee, refunds, FX and fraud take the rest
before the margin is even counted.

*Assumption set used below — every value is a **decision to re-check**, never a measured fact.*
Taken from `docs/research/2026-09-23-deepseek-api.md` §6.4, where each is named and argued
rather than observed:

| Symbol | Assumption | Value | Status |
|---|---|---:|---|
| `F_fraud` | share of granted credit never paid for when the client-reported purchase token is trusted | 0.20 | **ASSUMPTION** — decide per slot; see §3.7 |
| `F_fraud` | same, when the purchase token is confirmed server-side | 0.01 | **ASSUMPTION** — a residual, not zero |
| `F_billing` | Play service fee + billing fee | 0.15 | **ASSUMPTION.** The fee table is sourced (research §6.3, read 2026-09-23), but 0.15 is the *chosen default*, valid only under the first-$1M annual tier and/or the new-install case; above $1M, or for existing installs in the EEA/UK/US at standard rates, the fee rises to 20–30 % plus the 5 % billing fee. Whether the fee is computed on a tax-inclusive price is **UNVERIFIED** (research §8.7) — re-verify before pricing a real product |
| `F_refund` | refunds / chargebacks | 0.02 | **ASSUMPTION** — decide per slot |
| `F_fx` | FX + payout rounding | 0.02 | **ASSUMPTION** — decide per slot; Play prices are set per currency |
| `M_target` | target gross margin on net revenue | 0.60 | **ASSUMPTION** — decide per slot |

`F_fraud` repays the most attention: it is not a property of the market, it is the share the app
*allows* by granting credit on the client's word (§3.7).

**Worked example — one 1,000,000-token pack, arithmetic shown.** `C_unit = $2.112` per 1M tokens
is the research's conservative peak figure (70 % input / 30 % output, all input cache-miss, so
caching can only improve it). Assume `F_fraud = 0.20`, `F_billing = 0.15`, `F_refund = 0.02`,
`F_fx = 0.02`, `M_target = 0.60`:

```
C_unit   = $2.112

divisor  = (1−0.20)(1−0.15)(1−0.02)(1−0.02)(1−0.60)
         = 0.80 × 0.85 × 0.98 × 0.98 × 0.40
         = 0.261

K        = 1 / 0.261 = 3.83                  ← required multiple of unit cost
P_min    = 3.83 × $2.112 = $8.09             ← cheapest honest price for this pack

naive 2× = 2 × $2.112 = $4.22                ← under-prices by (8.09−4.22)/8.09 ≈ 48 %
```

Two further readings of the same arithmetic:

- **Break-even** (`M_target = 0`) on the same four deductions needs `K = 1 / 0.653 = 1.53×`.
  Even a zero-margin pack cannot be sold at cost.
- **Confirming the purchase server-side** moves `F_fraud` from 0.20 to 0.01 and the multiple
  from **3.83× to 3.09×** — worth **$0.74 per $1.00** of unit cost, or **$1.56** on this pack.
  That is the quantified return on the authority in §3.7, independent of any fraud incident.

Then round **up** to a price the store can actually set: the listed price, not `P_min`, is what
the user pays. **UNVERIFIED:** the set of purchasable Play price points (research §8.8) — a Play
Console step, not something this document can establish.

Re-read the provider's price page and re-date every number before a pricing decision; the
research itself warns that provider prices may change at any time.

Sources (purchase arithmetic): `docs/research/2026-09-23-deepseek-api.md` §2.1 (published
per-token prices), §6.1–§6.5 (formula, assumption set, worked packs), §6.3 (Play service fee,
read from Play's own page), §8 (the UNVERIFIED register).

## 3. The purchase spectrum: what an app can sell, and how Play Billing implements it

Monetization is not one lever. Everything an app can sell maps to a **different Play
Billing product type, a different Play call, and a different question about where the
truth lives**. Collapsing them into "a list of product ids" is what produces the classic
bugs: a token pack that can be bought exactly once, a purchase Play refunds after three
days, a balance that multiplies itself on every cold start.

| What the user pays for | Play product type | The Play call that matters | What the user actually gets | Where the truth lives |
|---|---|---|---|---|
| Remove ads, pro feature, one module | One-time (non-consumable) | acknowledge | A permanent entitlement | Play — `restore()` recovers it after reinstall |
| Token / credit / generation packs | One-time (consumable) | **consume** | A spendable balance, held until spent | Our server for `SERVER_COST` packs; the device cache only for `LOCAL` packs (§3.7) |
| ~~Recurring access~~ | ~~Subscription~~ — **out of scope** (§3.3) | — | Not sold by any slot; the capability models the lifecycle and no slot may ship it | — |
| Watching a rewarded video | Not billing | — | A one-time in-app reward | Local, and deliberately disposable |
| Paid app (no ads, no IAP) | Store listing price | — | The app itself | Play |

### 3.1 One-time non-consumable unlocks — "remove ads", "pro"

Something the user **owns forever**. This is the only product kind whose recovery story
is complete: Play remembers the purchase, and `BillingController.restore()` is the single
source of truth for `entitlements`, so a reinstall or a new device recovers it instead of
charging again.

- **Acknowledge it.** An unacknowledged purchase is refunded by Play after three days,
  which reads in the reports as the app losing a sale — which is why the capability
  requires `acknowledgePendingPurchases()` on cold start, not only after a purchase
  (`capabilities/billing/capability.json`).
- **Never consume it.** Consuming a non-consumable erases the ownership the user paid for;
  consume is the consumable path only (§3.2).
- Typical products: `remove_ads`, `pro_unlock`, `unlock_export`. `BillingCatalog.utility(...)`
  is the shape a utility slot starts from, and it exists precisely because unlocks (not
  packs) are what that archetype sells.

### 3.2 Consumables — token and credit packs that are **spent**

This is the mechanic behind the owner's "token pack" idea: a voice assistant for learning
English where the user buys a pack of tokens to interact with an AI assistant.

The user does **not** own the pack. They buy a quantity; the quantity is credited to a
**balance**; the purchase is then **consumed**, and consuming is what makes the same
product purchasable a second time. What the user holds afterwards is a balance, not
ownership.

- **Authorize, then grant, then consume.** For a `SERVER_COST` pack the authority must confirm
  the token *before* any credit exists (`PurchaseLedger` authorizes first, then credits). The
  credit itself is `PurchaseLedger` crediting `Wallet` and returning the token in
  `Settlement.consume`, which the caller then performs. `BillingCatalog` models a pack as
  `ConsumablePack(id, grants, unit, fulfillment)` — the grant amount is a promise shown on
  the paywall ("100 tokens"), so store UI, grant accounting, and validation read one
  declaration rather than three copies of a number.
- **A consumable is deliberately not an entitlement.** Crediting `owned += tokens_100`
  instead of `wallet += 100 tokens` is the bug that makes a pack buyable exactly once,
  forever (`Wallet` and `BillingCatalog` KDocs).
- **`restore()` cannot recover a balance.** Play tracks *purchases*, not balances; a wallet
  rebuilt from Play on a fresh install is always empty, and only our own record of what was
  spent survives (`BillingController` KDoc). For local-value credit that is acceptable if
  the slot says so; for money-costing credit it is the reason the authority in §3.7 exists.
- **Settlement is idempotent per token.** Play re-reports an unconsumed purchase on every
  cold start, so crediting on each report multiplies the user's balance for one payment
  (`PurchaseLedger` rule 3, `store.isSettled(token)`, reported as `AlreadySettled`).
- **Pending is not paid.** Only `PURCHASED` grants; `PENDING` grants nothing and is reported
  as `AwaitingPayment` (`PurchaseLedger` rule 1).
- **Units are explicit and may be plural.** One app can sell several kinds of credit at once;
  `Wallet` is keyed by a unit string and `BillingCatalog.consumableUnits()` exists so the
  balance UI can be seeded before anything is bought. Inferring the unit from the product
  name is how two packs silently collide (`ConsumablePack` KDoc).
- **A failed purchase mutates nothing.** No credit, no entitlement, no consume, so it retries
  later — and a `SERVER_COST` pack the authority will not confirm is `NotAuthorized`, with
  no credit granted.

### 3.3 Subscriptions — banned

**Out of scope: the owner banned subscriptions on 2026-10-01.** *We drop subscriptions immediately —
that is the one thing we will not do, there are too many nuances. So a subscription is forbidden.
Everything else is fine.*

This supersedes the earlier "deferred" position, and supersedes everything below that described when
a subscription "becomes correct": the mechanism is not waiting for a slot to prove recurring value,
it is not available to propose at all. `subscription` was removed from `ARCHETYPES` in
`scripts/lib/slot-economics.mjs`, so a plan naming it fails `parseEconomics`,
`requiredCapabilitiesFor` and `node scripts/check-economics.mjs`, with no exemption path. The
contract is `docs/slot-economics.md`.

The reason is cost, not taste. A subscription is *recurring* revenue, so the entitlement has to be
re-established continuously: server-side validation, plus a renewal, refund, cancellation,
grace-period and failed-payment lifecycle honoured on a schedule the client does not control. The
billing capability already models that lifecycle — `SubscriptionStatus.ACTIVE`, `GRACE_PERIOD`,
`ON_HOLD`, `PAUSED`, `CANCELED`, `EXPIRED`, with `GRACE_PERIOD` counted as active on purpose so a
paying user is not cut off during a payment retry, and `Entitlements` treating an active subscription
as ownership — and that is exactly the point: the model is complete, and completing it in production
is a server project with a billing backend. This factory builds slots, not servers, and the
one-hour budget has no room for one.

The three conditions below are kept as the record of why the position moved from *deferred* to *out
of scope*, not as a gate that may be satisfied:

- the value genuinely **recurs** — fresh content each period, an ongoing service, storage,
  live data;
- the user's need does **not** end once a one-time purchase is made (if it does, a one-time
  unlock plus consumable packs is simpler and earns the same);
- the slot can carry the extra obligations: renewal-terms disclosure, cancellation, refund
  handling, and stricter review.

A slot meeting all three still ships with the server-side lifecycle above, and is therefore a server
project. It is not proposed. The `billing` capability stays available for one-time and consumable
purchases, which is what the factory uses it for.

### 3.4 Ads as a parallel revenue stream

Ads are not removed from the toolbox. They are one of several streams, and they are the
**wrong lead** for an app that has something to sell.

- The formats and their relative weight are in §2: banner, interstitial, rewarded video.
  **Rewarded video is worth roughly an order of magnitude more per user than banners**
  (figures and sources in §2), so the design question — *what does a rewarded view unlock?*
  — must be answered **before UI design**, not bolted on after.
- If the honest answer is "nothing worth unlocking", that is a signal the app is a poor ads
  fit — but a poor fit for ads is not evidence for a purchase. Ask the next question in §3.6
  rather than reaching for the purchase shape by default.
- Ads and purchases are not mutually exclusive inside one app, and the **order** between them
  is read off the app, not assumed: consumables lead where there is something worth buying
  repeatedly, ads lead for a utility with nothing to sell (a one-time "remove ads" unlock is
  the natural upsell), and neither has a standing claim on the other.
- Consent and declaration obligations apply as soon as the ads capability is bundled — even
  if no ad is ever shown (`capabilities/ads/capability.json`), see §6.

### 3.5 Apps directed at children — why "ads-first" is not a safe default

This is the case that proves monetization cannot be one fixed profile.

- The mandatory regime is the **Families Policy Requirements** when any declared target age group
  includes children. **Designed for Families** is a separate opt-in programme. A
  parents/carers-only declaration is not a safe shortcut where the app or listing uses
  child-directed imagery or terminology: Play can override it, and misdeclaration risks removal
  or suspension.
- **No ads is the factory's low-risk default, not a policy prohibition.** Ads may be used only
  under the Families ads regime: approved self-certified Ads SDKs, no interest-based advertising
  or remarketing to children or users of unknown age, child-appropriate content, and compliant ad
  formats. The factory's existing ads capability still must not be used as-is, because it lacks
  that verified child-directed configuration (`capabilities/ads/capability.json`, repo-verified).
- IAP is not prohibited on policy grounds. Digital goods must use Play Billing; where children are
  in the audience, purchase presentation must avoid emotionally manipulative prompts,
  inadvertent taps, blurred virtual-currency/real-money distinctions, and overly aggressive
  commercial tactics. Purchase-consent mechanics remain a legal question, not a Play-policy ban.

Source: [`docs/research/2026-09-23-autism-app-policy.md`](research/2026-09-23-autism-app-policy.md)
§§1, 3, and 4; its primary links include
<https://support.google.com/googleplay/android-developer/answer/9893335> and
<https://support.google.com/googleplay/android-developer/answer/9867159>. The current no-ads,
purchase-led direction is retained as a product decision; this correction does not choose ads for
a child-directed slot.

### 3.6 The decision rule

Applied in order, before UI design:

1. **Is the app directed at children?** → apply the Families Policy Requirements (§3.5). Keep
   no ads as the low-risk default, or explicitly design and verify a Families-compliant ad
   configuration; paid app and IAP remain available options.
2. **Does the app have something worth selling repeatedly** — content, credits, generations,
   or a service call that costs real money to serve? → **repeat sales.** Model it as consumable
   packs (§3.2) plus one-time unlocks (§3.1). Ads, if used at all, are secondary.
3. **Is it a pure utility with nothing to sell repeatedly?** → **ads lead**, with a one-time
   "remove ads" / "pro" unlock as the upsell (§3.1). If there is nothing worth watching a
   rewarded video for, prefer the unlock over rewarded inventory. This is a finished product
   shape, not a failure to design a purchase.
4. **Is there genuine recurring value?** → **nothing to decide: subscriptions are banned**
   (§3.3). Recurring value is not a reason to build the app and may not be sold as one. Find
   the one-off shape, or do not build it.
5. **Write the archetype sentence into the slot plan.** Same discipline as §1.2's
   distinctness sentence: if the archetype cannot be stated in one sentence with a named
   product list, the monetization is not designed yet.

Resulting archetypes:

| Archetype | When it fits | Mechanics |
|---|---|---|
| Purchase-led, repeat sales | Something worth buying repeatedly (content, credits, generations) | Consumable packs + one-time unlocks; ads optional and secondary |
| Purchase-led, single unlock | Utility with one premium capability | One-time non-consumable |
| Ads-led utility | Nothing to sell; volume is plausible | Ads including rewarded, plus a "remove ads" unlock |
| Purchase-only, no ads | Child-directed or ad-hostile audience | A low-risk product choice: paid app or IAP (§3.5) |
| ~~Subscription~~ | **Banned** — on no row | Not available; a plan naming it fails the economics gate (§3.3) |

The purchase-led rows are not a guess about the market; they encode the owner's own
commercial history — apps **without ads but with in-app purchases of content and elements**.
That history was previously written here as "the factory's starting hypothesis", and writing it
that way is exactly how it stopped being a hypothesis: every plan, recipe and proposal this
repository produced chose `purchase_led_single_unlock`, because the doctrine told the agent that
was where to start. The history is now recorded as **evidence that purchases can work**, not as a
prior that they will — see `docs/slot-economics.md`, *"There is no default archetype"*. An app that
genuinely has nothing to sell is still an ads app, and §2's arithmetic is what tells us whether that
can reach $1,000/month.

### 3.7 The security asymmetry: who decides what the user owns

> *"Google Play reports a purchase, and the client is free to report anything else it likes.
> A patched build on a rooted device can invoke the purchase callback with a fabricated
> token, so an app that credits on the callback's word hands out credit that may cost real
> money to fulfil. Play's own guidance is to confirm the token against the Google Play
> Developer API before granting."* — `capabilities/billing/src/main/kotlin/com/gexetr/capability/billing/PurchaseAuthority.kt`, repo-verified.

- **A client-reported purchase token is attacker-controlled.** For credit that costs money
  to serve — an AI inference, a generated asset, a paid API call — the grant must be
  confirmed against a server using the Google Play Developer API **before** the user is
  credited. Credit granted ahead of that confirmation is drained by a patched build: the
  attacker pays nothing and spends everything, and the provider bill is still ours.
- **The same asymmetry applies to spending.** A balance kept only on the device is editable
  by the user and resets on reinstall, so it cannot be the only guard: for money-costing
  credit the authoritative debit happens server-side (`BillingController.spend` KDoc,
  `PurchaseAuthority.confirmSpend`).
- **The factory draws the line at fulfilment cost**, declared per pack as `Fulfillment`:
  - `LOCAL` — the spend costs us nothing beyond lost revenue (a theme, a filter, a local
    feature). A device balance is sufficient; cheating costs a sale, not cash.
  - `SERVER_COST` — each spend incurs a real cost. A `PurchaseAuthority` is **mandatory**,
    and `PlayBillingController` refuses to serve such a catalog without one, loudly, at
    connect time (`BillingCatalog.requiresPurchaseAuthority()`, repo-verified).
- **There is deliberately no default authority implementation.** The factory ships **no default
  `PurchaseAuthority`**; `PurchaseLedger` treats a `SERVER_COST` pack with none configured as
  unauthorized (`authorizePurchase` returns false → `NotAuthorized`, with no credit granted)
  rather than trusting it. A permissive default looks safe and is not, which is worse than an
  absent one (`PurchaseAuthority` KDoc).
- **The authority is worth money, not only safety.** Under the assumptions in §2.2, trusting
  the client-reported token (`F_fraud = 0.20`, `M_target = 0.60`) requires **3.83×** unit cost;
  confirming the token server-side (`F_fraud = 0.01`) requires **3.09×**. The gap is **$0.74 per
  $1.00** of unit cost — **$1.56** on the pack worked in §2.2.
- **Planning consequence:** a slot that sells money-costing credit is a **server project**,
  not just an app. That decision belongs in the plan beside the archetype (§3.6), because it
  changes the build.

> **Naming drift found while writing this, and since fixed (verified by search, not a policy
> claim).** The KDoc of `BillingController` and `Wallet` linked to a type named
> `PurchaseVerifier`; no such type exists anywhere under `capabilities/`. The implemented seam is
> `PurchaseAuthority` (`PurchaseAuthority.kt`). Both KDoc references now point at
> `PurchaseAuthority`, confirmed by a repo-wide search returning no remaining `PurchaseVerifier`
> match. Recorded here because it is the same defect class as the capability-group drift in
> `harness/lessons.md`: one name restated in two places, updated in one.

## 4. Model comparison

| Model | LTV potential | Fit | Main risk |
|---|---|---|---|
| Ads (banner/interstitial) | Low per user, needs scale | Weak on its own | Revenue too small without volume |
| Ads + rewarded video | Medium–high | Good **only when there is something worth unlocking** | Needs a real unlock; poor fit for a utility with none |
| Consumable packs (tokens, credits, generations) | High when the app has something worth selling repeatedly — but a high-LTV pack can still be a thin-margin one: the multiple of unit cost is not the margin (§2.2) | **Strong for content/service apps** — the owner's own proven shape | Fulfilment cost must be controlled; money-costing credit needs a server authority (§3.7) |
| One-time IAP ("remove ads", "pro") | Medium, one-off | Strong for any app with a premium capability; the natural ads upsell | Small converting share (~1–3 % — **UNVERIFIED**; no source read in this session) |
| Subscription | **Banned.** | — | Out of scope (owner, 2026-10-01): a server-side renewal, refund, cancellation and grace-period lifecycle makes the slot a server project (§3.3) |
| Affiliate / lead-gen | Can beat ads per user | Niche-dependent | Only works in specific verticals |
| Paid app | — | Viable as a no-ads product choice for child-directed or ad-hostile audiences (§3.5); otherwise effectively dead | No discovery |

Recommendation: **choose per app, and derive the choice from the app's own evidence.** Where the
app has something worth selling repeatedly — content, credits, generations, an AI assistant the user
pays to talk to — model it as consumable packs plus a one-time unlock, and treat ads as optional.
Where the app is a pure utility with nothing to sell, ads lead and a one-time "remove ads"
unlock is the upsell. Subscriptions are out of scope entirely (§3.3). Never
make any one of these the factory default: the "starting hypothesis" this section used to
state is precisely how a default formed without anyone choosing it, and the archetype mix is
expected to differ from slot to slot.

Affiliate deserves a deliberate look in verticals where it pays per action rather
than per impression (finance, travel, shopping, VPN), because there it can exceed ad
revenue by a large multiple for the same user.

## 5. What this means for the factory

**Enforcement status, added 2026-09-26.** Items 1, 5, 6 and 7 below were prose only, and the
factory's first real slot shipped without any of them: a calculator with
`capabilities.json = {"enabled": []}` passed every gate. They are now executable. Item 1 and
item 5 are enforced by `scripts/check-economics.mjs` against each slot's `economics.json`, and
`scripts/new-slot.mjs` refuses to create a slot without a validated plan. The contract is
`docs/slot-economics.md`; the spec is `openspec/specs/slot-economics/spec.md`; the post-mortem
is `docs/reviews/2026-09-26-slot-1-economics-postmortem.md`. Where the list below and the
enforced contract disagree, the contract wins, because it is the thing a program reads.

1. **Monetization is a design input and a per-slot decision** — now enforced before the slot
   exists rather than asserted after it. A slot's plan names its archetype (§3.6), its price,
   and its product list before UI is designed; if it uses ads, it also names the rewarded
   unlock.
2. **One config-driven façade per stream.** Ads behind something like
   `Monetization.showRewarded("unlock_export")` with placements, formats, and frequency caps
   in one per-slot file; billing behind a per-slot `BillingCatalog` declaring unlocks and
   consumable packs — so placement and pricing can be tuned without re-architecting
   app code. Subscriptions are out of scope and are not part of that catalog (§3.3).
3. **Remote configuration is mandatory** (Firebase Remote Config), otherwise every
   monetization experiment costs a full release cycle. This is the difference between
   measuring and guessing.
4. **Revenue must be attributed per slot**, or "which app works?" is unanswerable — and with
   purchase-led slots, credited credit versus spent credit must be attributable too.
5. **Define kill criteria before shipping** — now enforced: `economics.json` must carry
   `killCriteria` with a window, an install floor, and a revenue floor, all non-zero, before
   the slot can be created. The example "if after 30 days past production DAU < N and
   revenue < $X, the slot is archived" is the shape; the numbers are the plan's. This
   protects both time and account standing.
6. **Distinctness is a gate, not a preference**, per §1.2.
7. **Decide the fulfilment class before the paywall** (§3.7). A `SERVER_COST` pack means a
   server, a Play Developer API confirmation path, and an authoritative balance; the factory
   does not ship one today.
8. **Price each pack from the formula, not from a round multiple** (§2.2). A pack's price
   states its `C_unit`, the four assumed deduction rates, the target margin, and the resulting
   `K`, so a change in any assumption can be traced to a price instead of argued about.

## 6. Policy obligations implied by monetization

Declaring these late causes rework, so they belong in slot setup:

- **Ads declaration** in App content, and the **Advertising ID** declaration, which
  must reflect every bundled SDK.
- **Data safety** must match the ad and billing SDKs actually shipped; SDK manifests
  can merge `com.google.android.gms.permission.AD_ID` even when the app never
  declares it. Selling products adds purchase-history data to the declaration — the billing
  capability declares "Financial info — App functionality" for exactly this reason
  (`capabilities/billing/capability.json`).
- **User consent** (UMP or equivalent) is required before loading personalised ads in
  the EEA/UK; consent state must gate ad loading, not merely appear as a dialog.
- **Families policy**: when a declared target age group includes children, apply the Families
  Policy Requirements; no ads is the low-risk default, while any ads require the verified
  Families-compliant configuration in §3.5.
- **Disruptive Ads policy**: no full-screen ad on launch without warning, no ads that
  block the app's own function.
- **Billing** disclosure: price and renewal terms must be clear; purchase flows must
  acknowledge purchases, resolve pending purchases on cold start, and **consume
  consumables** — an unconsumed pack can be bought only once, and an unacknowledged purchase
  is refunded after three days.
- **Products must exist in Play Console before review**, because a purchase flow that
  references an unknown product fails in the reviewer's session
  (`capabilities/billing/capability.json`).
- **A spendable balance is ours to keep, and to lose.** Because Play does not track balances
  (§3.2), the slot must state what happens to unspent credit on reinstall, and — for
  `SERVER_COST` credit — the server is the record of truth (§3.7).

## 7. Open decisions

1. Does the §1.1 production gate apply to this account? (Decisive for batch size and
   timing.)
2. Which archetypes (§3.6) does the first batch use? Validate the decision rule against two
   or three real ideas from `docs/niche-selection.md` before scaffolding, rather than
   assuming one profile for the factory.
3. Which niche or vertical the first slots target — this decides whether affiliate is
   worth more than ads.
4. Do we need a server for `SERVER_COST` credit, and when? Any "buy tokens to use the AI"
   slot is blocked on this (§3.7).
5. Before planning a child-directed slot, apply the sourced Families analysis in §3.5 and
   `docs/research/2026-09-23-autism-app-policy.md`; re-read its live policy links before
   publishing.
6. Which values do the §2.2 assumptions take for the first slot — `F_fraud`, `F_billing`,
   `F_refund`, `F_fx`, `M_target`? They are decisions, not measurements, and each pack price
   inherits them. Re-check the Play fee tier in particular: 0.15 holds only under the
   first-$1M tier / new-install case, and whether the fee is computed on a tax-inclusive price
   is still **UNVERIFIED**.
