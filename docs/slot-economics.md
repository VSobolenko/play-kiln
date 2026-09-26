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

The five archetypes are exactly the rows of `docs/monetization-strategy.md` §3.6's
table. This table is the teeth of the gate: an archetype that names a way to earn is
not credible while the capability that collects the money is disabled.

| `archetype` | §3.6 row | Required capability at verify time |
|---|---|---|
| `purchase_led_single_unlock` | Purchase-led, single unlock | `billing` enabled |
| `purchase_led_repeat_sales` | Purchase-led, repeat sales | `billing` enabled |
| `ads_led_utility` | Ads-led utility | `ads` enabled |
| `purchase_only_no_ads` | Purchase-only, no ads | none — and `priceUsd > 0` |
| `subscription` | Subscription | `billing` enabled |

`purchase_only_no_ads` requires no capability because a paid app's price is set in
Play Console, not by an in-app capability; the gate's only demand of it is a price
above zero. Enabling extra capabilities beyond what the archetype needs is allowed,
because an ads-led utility legitimately also sells a one-time "remove ads" unlock
(§3.6).

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
