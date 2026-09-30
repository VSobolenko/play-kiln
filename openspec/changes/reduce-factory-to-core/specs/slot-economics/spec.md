# Capability: slot economics

## MODIFIED Requirements

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
