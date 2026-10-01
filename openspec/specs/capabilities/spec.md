# Factory capability: shared capabilities

## Purpose

Reused functionality must be implemented **once** and consumed by many slots, so
the factory does not rediscover the same integration bugs and Play-policy answers in
every app. Capabilities are versioned artifacts, never shared source trees.

## Requirements

### Requirement: Capabilities are versioned published artifacts

Shared functionality SHALL live in the standalone `capabilities/` Gradle build,
publish as `com.gexetr.capability:<module>:<version>`, and be consumed by slots
       19|through a pinned version. A slot SHALL NOT depend on another slot and SHALL NOT
include the capabilities source tree.

#### Scenario: Slot consumes a capability

- **WHEN** a slot needs ads
- **THEN** it adds `implementation("com.gexetr.capability:ads:<pinned>")` and changes
  no other slot

#### Scenario: Two slots need different versions

- **WHEN** one slot stays on `1.0.0` and another adopts `1.1.0`
  - **THEN** both build, because the version is pinned per slot

### Requirement: Capabilities wrap official SDKs, never replace them

A capability SHALL own only the integration of an official SDK (initialisation
order, consent sequencing, entitlement state, lifecycle, test doubles) and SHALL
NOT reimplement SDK features. SDK types SHALL NOT appear in a capability's public
API; official SDKs SHALL be `implementation` dependencies.

#### Scenario: AdMob stays behind the interface

- **WHEN** a slot reads the ads API
  - **THEN** no `com.google.android.gms.ads` type appears in the public signatures

### Requirement: Capabilities declare their compliance footprint

Every capability module SHALL ship a `capability.json` validating against
`capabilities/schema/capability.schema.json`, listing its SDK dependencies,
permissions with a written justification, collected data types with purpose and
sharing, whether consent is required before network work, whether ads are present,
and its test doubles. `./gradlew checkCapabilityManifests` SHALL fail on a missing
or dishonest manifest.

#### Scenario: Data safety is generated, not guessed

- **WHEN** the store pack is produced for a slot
  - **THEN** the Data safety answers are derived from the `capability.json` files of
  the capabilities that slot includes

#### Scenario: Networked capability lacks a test double

- **WHEN** a capability sets `networkRequired: true` with an empty `testDoubles`
  - **THEN** `checkCapabilityManifests` fails

### Requirement: Capabilities are optional per slot unless declared otherwise

A slot SHALL include only the capabilities it needs. A capability with
`optionalForSlots: false` (for example `core`) SHALL be present in every slot.

#### Scenario: Offline app stays data-free

- **WHEN** a slot omits the ads capability
  - **THEN** its merged manifest contains no advertising-ID permission and its Data
  safety answer stays "no data collected"

### Requirement: Consent precedes data collection

A capability with `requiresConsent: true` SHALL NOT perform network work or collect
data before the user resolves the consent form.

#### Scenario: No ad request before consent

- **WHEN** consent has not been resolved
  - **THEN** `canRequestAds()` is false and no ad request is made

### Requirement: Promotion is justified

Code SHALL be promoted into a capability only when at least two slots need it, or it
carries Play-policy/security risk that must be solved identically everywhere, or the
integration is non-obvious and easy to get wrong. Otherwise the code stays in the slot.

#### Scenario: Single-app feature stays local

- **WHEN** only one slot needs a behaviour and it carries no policy risk
  - **THEN** it is implemented in that slot and not promoted

### Requirement: Capability versions are immutable

Published capability versions SHALL be additive within a major version; a breaking
change SHALL take a new major version and leave the previous version published and
buildable.

#### Scenario: Breaking change

- **WHEN** a public capability API must change incompatibly
  - **THEN** the version major is increased and slots migrate deliberately
