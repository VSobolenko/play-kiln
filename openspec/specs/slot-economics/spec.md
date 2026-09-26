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

`scripts/new-slot.mjs` SHALL require an `--economics <path>` argument and SHALL refuse to
create the slot when the plan is missing, unparseable, or fails validation. Capitalised
`SHALL` is deliberate: a plan that is merely recommended is the state that produced the
failed slot.

#### Scenario: Missing plan

- **WHEN** `scripts/new-slot.mjs` runs without `--economics`
- **THEN** it prints `BLOCKED`, exits non-zero, and creates no directory under `apps/`

#### Scenario: Plan has no price

- **WHEN** the plan's `archetype` is a purchase-led archetype and `priceUsd` is `0`
- **THEN** the script refuses to create the slot and names the offending field

#### Scenario: Valid plan

- **WHEN** the plan validates
- **THEN** the slot contains the validated plan verbatim as `economics.json`, the slot's
  `.kiln-state.json` carries its `archetype` and `priceUsd`, and `registry/apps.json`
  records its `archetype`

### Requirement: The plan SHALL declare an archetype from the fixed spectrum

`archetype` SHALL be one of `purchase_led_single_unlock`, `purchase_led_repeat_sales`,
`ads_led_utility`, `purchase_only_no_ads`, or `subscription`, matching the archetype table
in `docs/monetization-strategy.md` §3.6. A free-form string SHALL be rejected, because an
archetype that cannot be classified cannot be checked against the capabilities that
implement it.

#### Scenario: Unknown archetype

- **WHEN** the plan's `archetype` is not one of the five values
- **THEN** validation fails and names the accepted values

### Requirement: A plan SHALL name a revenue mechanism the slot can actually implement

Validation SHALL pair the archetype with the enabled capabilities declared in the slot's
`capabilities.json`: purchase-led and subscription archetypes require `billing`,
`ads_led_utility` requires `ads`, and `purchase_only_no_ads` requires neither because the
price is set in Play Console.

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

The plan SHALL provide an `evidenceRef` matching `docs/research/<file>.md`, a
non-empty `assumptionsUnverified` list, and `killCriteria` with all three of
`windowDays` in 7..180, `minInstalls` at least 1, and `minRevenueUsd` at least 1. A kill
criterion of zero SHALL be rejected: a threshold nothing can miss is not a criterion.

#### Scenario: Placeholder plan

- **WHEN** any required field is empty, shorter than 12 characters, or a placeholder such
  as `TBD`, `N/A`, `none`, `unknown`, `placeholder`, `<angle-bracketed>`, or contains
  `example.com` or `lorem`
- **THEN** validation fails and names the field and the reason

#### Scenario: Unresolvable evidence

- **WHEN** `scripts/check-economics.mjs --slot <slug>` is run without `--validate-only`
  and `evidenceRef`'s file does not exist relative to the repository root
- **THEN** it reports `BLOCKED` naming the missing file

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
