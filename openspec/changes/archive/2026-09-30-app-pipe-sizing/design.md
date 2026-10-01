# Design: app-pipe-sizing

## Product boundary

The `pipe-sizing` slot is an offline US fuel-gas sizing workflow and calculation aid. It has no account, server, cloud sync, analytics, camera, ML, or AI surface. It does not provide engineering advice or a code-compliance determination. It helps users organize inputs and arithmetic after they consult their applicable adopted code, and shows an unavailable state whenever a required input or provenance is absent.

## Calculation contract

The calculation screen collects editable user inputs: material or system, pressure class, method, unit system, loads, developed lengths, and user-confirmed capacity value or values read from the user's source. It also collects user-supplied fitting equivalent lengths, gas specific gravity, pressure-drop allowance, and pipe inside diameter when the selected method needs them. Every capacity-like input is visibly labelled user-supplied or user-confirmed, with this affordance: “Read this value from your applicable code book or free online code; this app does not provide code-table data.”

For a complete, internally consistent case, the app computes only load/MBH sums, developed-length sums, user-supplied fitting-allowance addition, unit conversion, and comparison of required load with user-confirmed capacity. It may report whether a user-selected nominal pipe-size option supports that load; it must not select a size from a bundled or downloaded table. Imperial presentation uses ft/in and MBH; a metric display/entry toggle must not alter the physical value, material, pressure class, method, or provenance.

## Code data and provenance

No ICC, NFPA, AGA, or other code capacity data, clause text, table row, or table-derived constant is embedded, bundled, downloaded, reproduced, or faked. The owner decision rests on settled ICC 2024 IFGC front-matter facts: free viewing at `codes.iccsafe.org` is not redistribution permission; ICC requires separate written permission for reproduction; IFGS material is AGA-copyrighted.

Every capacity-like quantity is user-supplied or user-confirmed. Each result displays an attached user citation—code/edition, table, row, or the literal label “user-supplied”—and never appears without it. Saved work retains that citation; the app never replaces it with an invented source or silently reuses it for another row.

| Quantity or value class | Product treatment |
|---|---|
| Capacity values and capacity-like limits | User-supplied or user-confirmed, with source affordance and attached citation. |
| Fitting equivalent lengths | User-supplied when required; no built-in table or default. |
| Gas specific gravity | User-supplied when required; no code-derived default. |
| Pressure-drop allowances | User-supplied when required; no code-derived default. |
| Pipe inside diameters | User-supplied when required; no dimension schedule or code-table default. |
| Unit conversions and pure arithmetic identities | Computed by the app; no code-table constant. |

Missing, out-of-range, or inconsistent required input produces no result. The app identifies the correction required and does not estimate, interpolate, default, or carry a value from another row.

## Disclaimer and result presentation

On first run and every result view, the app displays: “This is a calculation aid, not a code-compliance determination. You are responsible for verifying this work against the applicable adopted code.” The app, listing, store copy, and `review-notes.md` make no safety-critical, code-compliance, code-approved, or standards-certified claim. Every result view shows editable inputs, arithmetic outcome, attached user citation, and this disclaimer. Missing citation means no result, not a result with a warning.

## Billing

The paid surface is a one-time, non-consumable Play Billing unlock. It shows the real price and workflows before purchase, acknowledges and restores entitlement, and never converts to a subscription. Ads are absent in v1; no ad may overlay an input. A billing failure or pending purchase leaves local work readable and does not fabricate entitlement.

## Validation and release strategy

There is no external oracle for code capacities because v1 supplies none. Tests verify only the app's arithmetic against independently computed expectations, never expectations produced by the implementation: developed-length sums, user-supplied fitting-allowance addition, unit-conversion round trips, MBH/load summation, method selection, and missing/out-of-range/inconsistent-input handling. Tests also prove no result without a user citation or where a capacity would be fabricated, defaulted, interpolated, or carried over.

Release evidence ends with the truthful store pack, signed APK/AAB and release manifest, `node scripts/verify-slot.mjs --slot pipe-sizing --strict-store --build`, a complete diff read, and `/eval`. The human alone completes legal declarations, first upload, and publication.
