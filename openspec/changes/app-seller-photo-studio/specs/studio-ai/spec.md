## Purpose

Offers generative image tools that go beyond a cut-out and a new background, while keeping the seller
honest with buyers: every result is checked against the original item, labelled as AI-processed, and never
charged when it fails. Detail: `annex/E-pipeline-and-operations.md` (O-08…O-14, guard metrics, series
pricing), screen S-14, edge cases E49–E55. Provenance labelling and in-app report also live in
`provenance-and-compliance`.

## ADDED Requirements

### Requirement: Generative tools

The app SHALL offer, as Enhanced-mode operations, relighting, canvas expansion, object and blemish removal,
upscaling, recolouring of the item, scene generation around the item, and for garments a ghost-mannequin
view and a virtual-model view. Each operation SHALL state its credit cost before it runs.

#### Scenario: Cost is stated first

- **WHEN** the seller selects an Enhanced operation
- **THEN** its credit cost is shown before the seller confirms it

#### Scenario: Operations are unavailable where the marketplace forbids them

- **WHEN** the selected marketplace preset is original-only
- **THEN** Enhanced operations are unavailable and the reason is shown

### Requirement: Fidelity guard

After every Enhanced result the system SHALL compare the item in the result with the item in the source,
using colour difference, structural similarity and, where the item carries text, a comparison of that
text, and SHALL accept the result only if it is within thresholds set from measurement. A result that
fails SHALL be retried once automatically; if the retry also fails the system SHALL reject the result,
return the credits, and tell the seller why, offering Original mode instead.

#### Scenario: A drifting result is rejected

- **WHEN** an Enhanced result alters the item's colour beyond the threshold and the retry does too
- **THEN** no result is delivered, the credits are returned and the seller is told the item did not stay faithful

#### Scenario: A faithful result is delivered

- **WHEN** an Enhanced result keeps the item within every threshold
- **THEN** it is delivered and the credits are charged once

#### Scenario: Label text is checked

- **WHEN** the item shows a printed label and the result changes the label's text
- **THEN** the guard rejects the result

### Requirement: A failure is never charged

The system SHALL return the credits for any operation that ends in an error, a timeout, an empty result or
a guard rejection, automatically and without the seller's action, and SHALL show the seller that the
credits were returned.

#### Scenario: Timeout refunds

- **WHEN** an operation times out
- **THEN** the credits reserved for it are returned and the balance equals the balance before the operation

#### Scenario: Refund is idempotent

- **WHEN** the same failed operation is reported twice
- **THEN** the credits are returned once

### Requirement: AI-content disclosure and reporting

The app SHALL label every Enhanced result as AI-processed, SHALL mark exported Enhanced files in their
metadata, and SHALL let the seller report an offensive or wrong result from the result screen. A report
SHALL reach the system and SHALL be recorded with the operation it concerns.

#### Scenario: Reporting a result

- **WHEN** the seller reports a result
- **THEN** a report is recorded with the operation's identifier and the seller sees a confirmation

### Requirement: Variations are charged once per series

When the seller asks for several variations of one scene in a single request, the system SHALL reserve the
full series cost once, SHALL settle per delivered variant, SHALL release the unused remainder, and SHALL
state that price before the request runs (annex E §Series).

#### Scenario: Series price

- **WHEN** the seller requests three variations
- **THEN** the stated price is for the series and the balance falls by that price only
