## Purpose

Encodes each marketplace's image rules as data, so the app can size, constrain and check a card for the
platform it will be listed on, and tell the seller what is wrong before the listing is rejected. Detail:
`annex/B-marketplace-presets.md`, `annex/marketplaces.seed.json` (M-<id>), `annex/L-linter-rules.md`
(L01–L20), screen S-09.

## ADDED Requirements

### Requirement: Presets are data with a source

Every marketplace preset SHALL be declared in a data file, not in code, and SHALL carry its canvas size
and aspect ratio, its permitted editing mode, its rules (text, frames, watermarks, background, minimum
resolution, file size and format), a source URL for those rules, the date they were last verified, and a
default locale. A preset without a source URL or a verification date SHALL be rejected by the project's
checks and SHALL NOT be offered to the seller.

#### Scenario: A preset without a source is refused

- **WHEN** the preset data contains an entry with no source URL
- **THEN** the project's check fails and names that entry

#### Scenario: Presets change without a code change

- **WHEN** a rule value in the preset data file changes
- **THEN** the linter's verdict for a matching image changes with no change to application code

### Requirement: Editing mode per marketplace

Each preset SHALL declare one editing mode: original-only, limited overlay or free. In original-only mode
the app SHALL disable every operation that alters the photo and SHALL offer shooting guidance and a rule
check. In limited-overlay mode it SHALL permit only the overlays the preset lists. In free mode it SHALL
permit all operations.

#### Scenario: Original-only disables editing

- **WHEN** the seller selects a preset whose mode is original-only
- **THEN** cut-out, background, text overlay and generative tools are unavailable and the reason is shown

#### Scenario: Limited overlay allows only the listed overlays

- **WHEN** the seller selects a limited-overlay preset that permits badges but not price text
- **THEN** a price text overlay is refused with the rule it would break

### Requirement: Rules linter

Before export the app SHALL evaluate the card against every rule of the selected preset and SHALL show
each rule as passed or failed with a plain-language explanation. The linter SHALL evaluate canvas size,
ratio, file format and size, detected text, frames, watermarks and background colour where the preset
declares a rule about them.

#### Scenario: A failed rule is explained

- **WHEN** a card has a coloured background and the preset forbids it
- **THEN** the linter marks that rule failed and states the rule and the offending property

#### Scenario: A passing card shows all rules passed

- **WHEN** a card satisfies every rule of the preset
- **THEN** every rule is shown as passed and export is offered without a warning

### Requirement: Fix for marketplace

The app SHALL offer an action that corrects every failed rule it can correct without changing the item
(size, ratio, margins, format, removal of a forbidden overlay), and SHALL list any rule it cannot correct.

#### Scenario: Automatic correction

- **WHEN** a card fails the size and margin rules and the seller taps fix for marketplace
- **THEN** both rules pass afterwards and the item's interior pixels are unchanged

#### Scenario: Uncorrectable rules remain listed

- **WHEN** a card fails a rule that needs a new photo
- **THEN** the action leaves that rule failed and says a new photo is needed

### Requirement: Marketplace coverage and locale ordering

The app SHALL ship presets for at least Wildberries, Ozon, Avito, Vinted, eBay, Etsy, Poshmark, Depop and
Mercari at 1.0, and SHALL order the presets shown to the seller by the interface locale's default
marketplaces. Presets for further marketplaces MAY follow in later releases.

#### Scenario: Locale decides the order

- **WHEN** the interface language is Russian and then German
- **THEN** the first presets offered differ and match each locale's declared defaults
