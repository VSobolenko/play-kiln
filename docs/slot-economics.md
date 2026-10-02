# Slot economics contract — `economics.json`

Every slot states **how it can earn** and **what would make us kill it**, in a file
that a program reads. This document is the specification; it is enforced by
`scripts/check-economics.mjs`, and `scripts/new-slot.mjs` refuses to create a slot
without a valid plan. Both facts are stated here because the plan is worthless if it
is only a convention.

## Why this file exists

`docs/monetization-strategy.md` §3.6 already required a slot to name its archetype
before its UI was designed, and §5 item 5 already required numeric kill criteria
before shipping. Both were prose. The factory's first product slot then shipped with
`capabilities.json = {"schemaVersion": 1, "enabled": []}` — no billing, no ads, no
revenue mechanism at all — and every existing gate returned PASS, because no check
read those two sections. The goal of the portfolio is revenue, so a slot with no
revenue mechanism is a defect, and a defect that no check can see is not a defect the
factory can fix.

`economics.json` is that prose made executable. The JSON is the plan; the capability
pairing below is the part that has teeth.

## The contract

`economics.json` lives at the slot root, beside `.kiln-state.json`. Its shape is
exactly:

```json
{
  "schemaVersion": 1,
  "archetype": "purchase_led_single_unlock",
  "payingUser": "<who pays, one sentence>",
  "paidUnit": "<what exactly is bought, one sentence>",
  "priceUsd": 4.99,
  "momentOfPayment": "<when in the user journey the money is taken>",
  "distinctnessSentence": "<what a user gets here that they do not get from the existing apps in this category>",
  "evidenceRef": "docs/research/<file>.md#<section>",
  "day1RevenuePath": "<how the first dollar arrives>",
  "killCriteria": { "windowDays": 30, "minInstalls": 500, "minRevenueUsd": 50 },
  "assumptionsUnverified": ["<at least one honest unknown>"]
}
```

| Field | Rule |
|---|---|
| `schemaVersion` | Exactly `1`. |
| `archetype` | One of the five values in the next section. |
| `payingUser` | Which person pays, in one sentence. At least 12 characters. |
| `paidUnit` | What exactly is bought, in one sentence. At least 12 characters. |
| `priceUsd` | The price in US dollars. A finite number `> 0` for every paid archetype; `ads_led_utility` may be `0`. |
| `momentOfPayment` | When in the journey the money is taken. At least 12 characters. |
| `distinctnessSentence` | The §1.2 distinct angle: what a user gets here that the existing apps in the category do not give them. At least 12 characters. |
| `evidenceRef` | `docs/research/<file>.md`, optionally with `#section`. The file must exist for `check-economics.mjs --slot`; the anchor is not verified. |
| `day1RevenuePath` | How the first dollar actually arrives. At least 12 characters. |
| `killCriteria` | Object with three finite integers: `windowDays` in `7..180`, `minInstalls >= 1`, `minRevenueUsd >= 1`. A kill criterion of `0` can never be missed, so it is rejected. |
| `assumptionsUnverified` | At least one non-empty string that is not a placeholder. An empty array claims a plan has no unknowns, which is not a plan. |

None of the free-text fields may be a placeholder. The validator rejects the literal
values `tbd`, `todo`, `n/a`, `none`, `unknown`, and `placeholder`; any text
containing `example.com`; angle-bracket text such as `<who pays>`; any text containing
`lorem`; and three or more consecutive `x` characters as a whole word.

## Archetypes and the capability each one requires

The four archetypes are the rows of `docs/monetization-strategy.md` §3.6's table other
than Subscription, which this factory does not ship (see below). This table is the teeth
of the gate: an archetype that names a way to earn is not credible while the capability
that collects the money is disabled.

| `archetype` | §3.6 row | Required capability at verify time |
|---|---|---|
| `purchase_led_single_unlock` | Purchase-led, single unlock | `billing` enabled |
| `purchase_led_repeat_sales` | Purchase-led, repeat sales | `billing` enabled |
| `ads_led_utility` | Ads-led utility | `ads` enabled |
| `purchase_only_no_ads` | Purchase-only, no ads | none — and `priceUsd > 0` |

`subscription` is deliberately **not** in this table and not in `ARCHETYPES`; it is banned, and
a plan naming it fails validation rather than being paired with a capability. The reason is below.

`purchase_only_no_ads` requires no capability because a paid app's price is set in
Play Console, not by an in-app capability; the gate's only demand of it is a price
above zero. Enabling extra capabilities beyond what the archetype needs is allowed,
because an ads-led utility legitimately also sells a one-time "remove ads" unlock
(§3.6).

## There is no default archetype — it is derived

**The factory has no house monetization.** All four archetypes are live options on every
proposal, and the revenue model is a *conclusion about this app and this audience*, not a
starting point. Choosing one because the last slot chose it, or because it is the easiest to
implement, is the defect this section exists to prevent.

The rule rests on the owner's own instruction, recorded 2026-10-01: *our goal is simply to earn.
Whether it is ads, a one-time purchase or a repeat purchase is another question — that depends on
the app, the audience, and the product.* There are no frames **except one**.

### Subscription is banned

The owner's instruction, same day: *we drop subscriptions immediately — that is the one thing we
will not do, there are too many nuances. So a subscription is forbidden. Everything else is fine.*

`subscription` is therefore **not in `ARCHETYPES`**, and that absence is the enforcement: a plan
naming it is rejected by `parseEconomics`, by `requiredCapabilitiesFor`, and by the
`check-economics.mjs` gate, with no exemption path. It is a ban rather than a deferral for the same
reason the rest of this factory exists — a subscription is *recurring* revenue, so the entitlement
must be re-established continuously, which means server-side validation plus a renewal, refund,
cancellation, grace-period and failed-payment lifecycle honoured on a schedule the client does not
control. That is a server project with a billing backend, not a slot this factory builds, and the
one-hour budget has no room for it. §3.3's three tables remain the analysis of why it was previously
only deferred; the decision has moved from *deferred* to *out of scope*.

**The list is not closed.** The four archetypes are the shapes whose economics and mechanics this
factory has actually reasoned about, not the whole space of ways an app can earn. If recon finds a
mechanism that is neither a purchase nor ads, the correct move is to bring it as a proposal with its
arithmetic and its capability pairing and extend `ARCHETYPES` and this document together, rather than
to bend it into the nearest existing row. A new archetype is added by naming what it requires at
verify time, so the gate keeps its teeth.

**Why this had to be written down.** Every example in this repository — all six plans under
`docs/ideation/plans/`, all four recipes of `docs/ideation/2026-09-26-five-recipes.md`, the single
worked example that used to sit beside the table above, `scripts/test-outcomes.mjs`, the reference
slot, and both proposals of the 2026-10-01 recon — carried `purchase_led_single_unlock`. **A hundred
per cent of the evidence taught one answer**, and the factory learned it. Stating that there is no
default is not enough on its own; the examples have to stop being uniform, which is why every
proposal now has to show the comparison explicitly (below) and the resulting mix is expected to vary
by slot. Two slots sharing a monetization model for no stated reason is as much a smell as two
slots sharing a palette.

### Deriving it

Read the app and its audience, then follow whichever row the evidence supports. These are signals,
not a hierarchy — the ads row is not a fallback for the purchase rows, and none of them is a
default.

| Ask | If the answer is yes, it points to |
|---|---|
| Does the app produce something a user would buy **again** — credits, generations, content packs, exports, a service call that costs real money to serve? | `purchase_led_repeat_sales` — consumable packs, one-time unlocks secondary, ads optional |
| Is there **one** premium capability, and a user who would pay once to get it and be done? | `purchase_led_single_unlock` |
| Is it a **utility with nothing to sell repeatedly**, used in short frequent sessions, in a context where ads are tolerable? | `ads_led_utility` — ads including rewarded, with a "remove ads" unlock as the upsell |
| Is there **genuine recurring value** — ongoing content, sync, a live dataset? | **Banned.** A subscription cannot be shipped here (above). Either the app earns one-off, or the recurring value is not a reason to build it |
| Is the audience **ad-hostile**, or is the app directed at children (§3.5)? | `purchase_only_no_ads`, or a no-ads purchase-led shape |

Two corrections to how the older guidance has been read, each one a real narrowing:

- **Ads are a first-class model, not a consolation prize.** "Nothing to sell repeatedly" does not
  mean "we failed to design a purchase" — it is a real product shape, and the question is whether
  the audience tolerates ads. §3.4's arithmetic decides whether it can reach the target; that is a
  numbers question, not a taste one.
- **`purchase_only_no_ads` is a price lever with no discovery**, which §4 of
  `docs/monetization-strategy.md` already records as effectively dead outside ad-hostile audiences.
  It is a legitimate choice, and a plan that picks it must say why the ad-hostile case applies.

### The comparison is part of the plan

A proposal must show **which archetypes were considered and why the others were rejected, in revenue
terms** — "repeat sales ruled out because the user's records are static and there is nothing to
re-sell; ads ruled out because the app is used for ten seconds and a banner is worth nothing" is a
derivation. "We chose the unlock" alone is not. This is the mechanism that stops a default: a
defaulted choice has nothing to compare against and cannot produce the list. `subscription` needs no
entry in that list — it is banned outright (above), so it is not an option to weigh.

It is deliberately not a machine-checked field. Enforcing it would mean a new required key, which
`SCHEMA_VERSION` (exactly 1) would reject for every plan already written and for the reference slot —
and the budget contract forbids absorbing a new legacy *gate* into `legacyCeiling`. So this one is a
**judgement the owner makes at checkpoint 1**, and the report he reads names the alternatives and the
reasons. Stated here rather than implied, so nobody mistakes the absence of a check for the absence
of the rule.

## Enforcement

- **`scripts/new-slot.mjs`** takes a required `--economics <path>` argument. It
  validates the plan with `parseEconomics`, prints one `BLOCKED` line per problem,
  and refuses to copy the scaffold — so a slot without a valid plan is never created.
  On success the validated object is written verbatim to the slot root as
  `economics.json`, and its `archetype` and `priceUsd` are recorded in
  `.kiln-state.json` and `registry/apps.json`.
- **`scripts/check-economics.mjs --slot <slug>`** re-validates the plan at verify
  time, checks the `evidenceRef` file exists, and compares the archetype against the
  slot's `capabilities.json`. It prints `VERDICT: PASS` or `VERDICT: BLOCKED` and
  exits non-zero on failure.
- **`scripts/check-economics.mjs --economics <path>`** validates a standalone plan
  that is not yet in a slot. The capability pairing is **skipped** in this mode with a
  printed `SKIP` line, because the capabilities are not enabled until the slot
  exists; the pairing is enforced later against the created slot.

## What this gate does not prove

The gate proves the plan is complete, internally consistent, backed by a research
file that exists, and paired with a capability that can collect the money. It does
not prove the price is correct, the evidence actually supports the claim, the market
exists, or the first dollar will arrive. Those are judgements for the Evaluator and
the owner; the file makes them stateable and reviewable, not certain.
