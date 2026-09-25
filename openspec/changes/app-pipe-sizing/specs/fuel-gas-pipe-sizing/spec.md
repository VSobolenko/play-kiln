# Fuel-gas pipe sizing

## Purpose

Provide an offline, editable US fuel-gas sizing workflow and calculation aid. Users read capacity values from their applicable code book or free online code; the app performs arithmetic and bookkeeping, records the user's citation, and does not determine code compliance.

## ADDED Requirements

### Requirement: Users enter an explicit, editable sizing case

The app SHALL let users enter and edit material or system, pressure class, method, length, loads, user-confirmed capacity values, and every selected-method input, including fitting equivalent lengths, gas specific gravity, pressure-drop allowance, and pipe inside diameter when required. It SHALL identify every capacity-like input as user-supplied or user-confirmed and show an affordance telling users to read it from their applicable code book or free online code because the app provides no code-table data. It SHALL permit imperial or metric display/entry.

#### Scenario: A user edits a case

- **WHEN** the user changes an input used by the selected method
- **THEN** the displayed case reflects that change and no required input is silently locked to a conflicting value

#### Scenario: A user changes units

- **WHEN** the user selects metric or imperial units
- **THEN** the app displays ft/in and MBH in imperial mode without changing the physical value, method, or citation

### Requirement: The app computes arithmetic, not code capacities

For a complete, internally consistent case, the app SHALL compute load/MBH sums, developed-length sums, user-supplied fitting-allowance addition, unit conversion, and comparison of computed required load with user-confirmed capacity. It SHALL NOT embed, bundle, download, reproduce, infer, or otherwise supply ICC, NFPA, AGA, or other code capacity data, clause text, table rows, or table-derived constants.

#### Scenario: A complete user-confirmed case is evaluated

- **WHEN** the user submits a complete, internally consistent case with confirmed capacity and citation
- **THEN** the app displays the arithmetic outcome and whether the user-selected option's confirmed capacity supports the computed required load

### Requirement: Every result has user-recorded provenance

The app SHALL display evaluated inputs and the user's citation with every result. A citation SHALL record code/edition, table, row, or the literal label “user-supplied.” The app SHALL NOT display a result without that label or claim it supplied or verified the cited source.

#### Scenario: Provenance is missing

- **WHEN** a capacity value lacks the required user citation
- **THEN** the app displays no result and identifies provenance as the missing required input

### Requirement: No capacity value is fabricated or silently reused

The app SHALL never fabricate, default, interpolate, estimate, or carry over a capacity value or other required capacity-like value from another row. When a required input is missing, outside the user-selected method's stated range, or inconsistent with another entered value, it SHALL display no result, identify the affected input, and require revision.

#### Scenario: A capacity is missing

- **WHEN** a user submits a case without a required capacity value
- **THEN** the app displays no result, requests that value, and does not fill it from a default or another row

#### Scenario: A long or high-demand case is incomplete or out of range

- **WHEN** a submitted length or demand requires a missing value or is outside the selected method's stated range
- **THEN** the app displays no result, identifies the missing value or range issue, and does not substitute a value, method, or row

#### Scenario: Inputs are inconsistent

- **WHEN** values entered for the selected method are inconsistent
- **THEN** the app displays no result and identifies the inputs requiring correction

### Requirement: The calculation-aid disclaimer is visible at decision points

The app SHALL display this exact text on first run and every result view: “This is a calculation aid, not a code-compliance determination. You are responsible for verifying this work against the applicable adopted code.” It SHALL NOT make a code-compliance, code-approved, standards-certified, or other safety-critical claim in the app, listing, store copy, or review notes.

#### Scenario: A user first runs the app

- **WHEN** the user first runs the app
- **THEN** the app displays the required calculation-aid disclaimer

#### Scenario: A user views a result

- **WHEN** the app displays an arithmetic result
- **THEN** the same required disclaimer is visible with that result

### Requirement: The utility works offline without an account

Sizing entry, arithmetic, unit conversion, unavailable-result states, and locally stored work SHALL operate without network connectivity or an account. The app SHALL NOT expose cloud sync, server processing, camera/ML diagnosis, generated recommendations, or an AI chat/wrapper surface.

#### Scenario: The device is offline

- **WHEN** the device has no network connection
- **THEN** the user can enter and evaluate a complete user-confirmed case locally

### Requirement: The commercial surface is a one-time unlock

The initial release SHALL use Play Billing only for a one-time, non-consumable, restorable unlock if the human approves a paid boundary. It SHALL contain no subscription or advertisement, and no purchase/advertisement shall obscure an input or unavailable-result explanation.

#### Scenario: A purchase is restored

- **WHEN** a user restores a completed one-time purchase
- **THEN** the approved entitlement is restored without a subscription offer

#### Scenario: Billing is unavailable

- **WHEN** purchase or restoration is pending, cancelled, or fails
- **THEN** the app identifies that status, preserves existing local work, and does not report an unlock that was not granted
