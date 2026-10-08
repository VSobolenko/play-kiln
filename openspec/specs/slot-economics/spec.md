# Capability: slot economics

## Purpose

Every PlayKiln slot MUST state, before it is created, who pays, what they buy, at what
price, and what would prove it failed. This capability exists because the factory's first
real product slot shipped with `capabilities.json = {"schemaVersion": 1, "enabled": []}` —
no billing, no ads, no revenue mechanism at all — while every gate returned PASS. The
factory's product is revenue, so a slot that cannot earn is a failed slot no matter how
green its gates are. Post-mortem: `docs/reviews/2026-09-26-slot-1-economics-postmortem.md`.
Contract: `docs/slot-economics.md`.

## Requirements

### Requirement: A slot SHALL NOT be created without an approved economics plan

The slot creation step SHALL refuse to create a slot unless it is given a commercial plan that
passes validation, and the plan SHALL be approved by the owner before the app is built. Capitalised
`SHALL` is deliberate: a plan that is merely recommended is the state that produced the failed slot.
The plan is the money owner's document and the owner's approval is recorded in the owner's own
words; the plan's approval SHALL NOT be inferred from any agent's confidence.

#### Scenario: Missing plan

- **WHEN** the slot creation step runs with no plan
- **THEN** it prints `BLOCKED`, exits non-zero, and creates no directory under `apps/`

#### Scenario: Plan has no price

- **WHEN** the plan's archetype is a purchase-led archetype and its price is zero
- **THEN** the step refuses to create the slot and names the offending field

#### Scenario: An unapproved plan does not build an app

- **WHEN** a plan validates but carries no recorded owner approval
- **THEN** the build stage does not start, and the refusal names the approval step

#### Scenario: Valid plan

- **WHEN** the plan validates and the owner has approved it in his own words
- **THEN** the slot contains the approved plan, the slot's state carries its archetype and price,
  and the registry records its archetype

### Requirement: The plan SHALL declare an archetype from the fixed spectrum

`archetype` SHALL be one of `purchase_led_single_unlock`, `purchase_led_repeat_sales`,
`ads_led_utility`, or `purchase_only_no_ads`, matching the archetype table in
`docs/monetization-strategy.md` §3.6. A free-form string SHALL be rejected, because an
archetype that cannot be classified cannot be checked against the capabilities that
implement it.

No member of that spectrum SHALL be treated as a default, and a plan SHALL NOT be accepted
on the strength of having named an archetype: the proposal SHALL state the archetypes it
considered and the revenue reason each was rejected. This is a judgement at the owner's
checkpoint rather than a validated field; `docs/slot-economics.md` records why.

`subscription` SHALL be rejected. The owner banned recurring billing on 2026-10-01 because
re-establishing entitlement continuously requires server-side validation plus a renewal,
refund, cancellation and grace-period lifecycle the factory would have to operate permanently. This
is the owner's product decision, not an infrastructure prohibition: a server is admissible. The
ban is enforced by `subscription` being absent from `ARCHETYPES`, and SHALL NOT be
overridable by any exemption file.

#### Scenario: Unknown archetype

- **WHEN** the plan's `archetype` is not one of the four accepted values
- **THEN** validation fails and names the accepted values

#### Scenario: The banned subscription archetype

- **WHEN** the plan's `archetype` is `subscription`
- **THEN** `parseEconomics` fails naming the accepted values, `requiredCapabilitiesFor`
  throws, and `scripts/check-economics.mjs` exits non-zero, with no exemption path

### Requirement: A plan SHALL name a revenue mechanism the slot can actually implement

Validation SHALL pair the archetype with the enabled capabilities declared in the slot's
`capabilities.json`: the `purchase_led_*` archetypes require `billing`, `ads_led_utility`
requires `ads`, and `purchase_only_no_ads` requires neither because the price is set in Play
Console.

#### Scenario: Purchase-led plan on a slot with no billing

- **WHEN** `scripts/check-economics.mjs --slot <slug>` reads a purchase-led plan and
  `capabilities.json` is `{"schemaVersion": 1, "enabled": []}`
- **THEN** it reports `BLOCKED` naming the archetype, the missing `billing` capability,
  and the file to fix, and exits non-zero

#### Scenario: Scaffold-time plan

- **WHEN** `scripts/check-economics.mjs --economics <path>` validates a plan outside a slot
- **THEN** the pairing is reported as `SKIP` with the reason that it is enforced later
  against the created slot, and the plan's other rules are still enforced

### Requirement: A plan SHALL carry evidence and numeric kill criteria

The plan SHALL name the demand evidence it rests on and SHALL carry `killCriteria` with all three
of `windowDays` in 7..180, `minInstalls` at least 1, and `minRevenueUsd` at least 1. A kill
criterion of zero SHALL be rejected: a threshold nothing can miss is not a criterion. The plan
SHALL state what the user buys and at what moment, and where nothing is bought the plan SHALL say so
and justify it against the cost of a paid download with no discovery.

#### Scenario: Placeholder plan

- **WHEN** any required field is empty, shorter than a stated minimum, or a placeholder such as
  `TBD`, `N/A`, `none`, `unknown`, `placeholder`, an angle-bracketed token, or contains
  `example.com` or `lorem`
- **THEN** validation fails and names the field and the reason

#### Scenario: A threshold nothing can miss

- **WHEN** any kill criterion is zero
- **THEN** validation fails and names the criterion

#### Scenario: Unresolvable evidence

- **WHEN** a plan names evidence by reference and that reference cannot be resolved to a recorded
  document
- **THEN** the money owner reports `BLOCKED` naming the missing document, and the plan is not
  accepted on the strength of a citation that does not exist

#### Scenario: Nothing is bought

- **WHEN** the plan's honest answer to what the user buys is that nothing is bought
- **THEN** the plan states that explicitly and justifies it, rather than leaving the field thin

#### Scenario: Evidence is read by the money owner, not matched by a program

- **WHEN** a plan names evidence by reference
- **THEN** whether that evidence supports the plan is the money owner's judgement, recorded in the
  plan's own reasoning, and is not decided by a program

### Requirement: An owner example SHALL NOT become a slot topic

A slot's topic SHALL be selected from measured demand recorded under `docs/ideation/`, and
SHALL NOT be carried over from an example the owner gave to illustrate a shape.
Calculators, unit converters, and generic arithmetic tools SHALL NOT be proposed. Rationale
and the full rule: `.cursor/rules/ideation-gate.mdc`.

#### Scenario: Proposal review

- **WHEN** an idea is proposed for a slot
- **THEN** its proposal states the category, job, paying user, paid unit, price, demand
  evidence with its measured/on-record/inferred label, build estimate, and kill criteria,
  in a form the owner can accept or reject without reading code
