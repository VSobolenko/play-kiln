# Fuel-gas pipe sizing

> Delta for change `pipe-sizing-user-capacity-table`.
>
> **Base-spec note.** `openspec/specs/fuel-gas-pipe-sizing/` does not exist; the base requirements
> these `MODIFIED` blocks target live only inside the still-active change
> `openspec/changes/app-pipe-sizing/specs/fuel-gas-pipe-sizing/spec.md`. `openspec validate` accepts
> this file today, but `opsx-sync`/`archive` resolve MODIFIED against the main spec and, with none
> present, would take the create-new-main-spec path and copy **`ADDED` requirements only** — losing
> these three blocks silently. Sync the base delta (`opsx-sync app-pipe-sizing`, or archive that
> change) before applying or archiving this one, then confirm the three `### Requirement:` headers
> below match the main spec verbatim. See `../README.md` and `../design.md`.

## MODIFIED Requirements

### Requirement: The app computes arithmetic, not code capacities

For a complete, internally consistent case, the app SHALL compute load/MBH sums, developed-length
sums, user-supplied fitting-allowance addition, unit conversion, and comparison of computed
required load with user-confirmed capacity. It SHALL NOT embed, bundle, download, reproduce,
publish, infer, or otherwise supply ICC, NFPA, AGA, or other code capacity data, clause text, table
rows, or table-derived constants, and it SHALL NOT carry any such value in its resources, assets,
source, or network traffic.

The one further act this requirement permits is a lookup that RETURNS a capacity the user recorded
in the user's own device-local table: reading a row the user entered, applying the documented
rounding rules in the conservative direction only, and showing the row it used. That permitted act
SHALL remain distinguishable from the app supplying a value — the returned value SHALL be
attributable to a named user-recorded row, SHALL NOT be produced when no such row exists, and SHALL
NOT be described as the app's own value or as the code's own value.

#### Scenario: A complete user-confirmed case is evaluated

- **WHEN** the user submits a complete, internally consistent case with confirmed capacity and citation
- **THEN** the app displays the arithmetic outcome and whether the user-selected option's confirmed capacity supports the computed required load

#### Scenario: A capacity is returned from the user's own recorded table

- **WHEN** the case selects a return-from-user-table method and the user's table records a row that the case's declared applicability, nominal size and total length identify
- **THEN** the app displays the capacity recorded in that row, names the row it used, and displays the table's recorded citation and the calculation-aid disclaimer with it

#### Scenario: No value is returned when the user's own table cannot answer

- **WHEN** the case selects a return-from-user-table method and no row of the user's table covers it
- **THEN** the app displays no capacity value at all and names the reason it could not answer, and displays no partial number and no estimate

#### Scenario: Returning a value grants no code-compliance claim

- **WHEN** the app displays a capacity returned from the user's table
- **THEN** the display states that the value is the user's own recorded value, that the app neither transcribed nor verified it, and makes no code-compliance, code-approved or standards-certified claim

### Requirement: No capacity value is fabricated or silently reused

The app SHALL never fabricate, default, interpolate, estimate, average, curve-fit, scale, or carry
over a capacity value or other required capacity-like value from another row, case, or table. It
SHALL never return a value that is not one of the rows the user recorded. When a required input is
missing, outside the user-selected method's stated range, or inconsistent with another entered
value, it SHALL display no result, identify the affected input, and require revision.

Reading a value out of a row the user recorded is not fabrication: a value returned from a
user-recorded row, under the documented rounding rules, with the substituted row shown, is the
user's own reading shown back to the user. Every other production of a capacity-like value remains
forbidden, including a value synthesised between, beyond, or from two recorded rows; a value taken
from a row the case's declared applicability does not cover; and a value reused from a previous
row, a previous case, or a previous table.

#### Scenario: A capacity is missing

- **WHEN** a user submits a case without a required capacity value
- **THEN** the app displays no result, requests that value, and does not fill it from a default or another row

#### Scenario: A long or high-demand case is incomplete or out of range

- **WHEN** a submitted length or demand requires a missing value or is outside the selected method's stated range
- **THEN** the app displays no result, identifies the missing value or range issue, and does not substitute a value, method, or row

#### Scenario: Inputs are inconsistent

- **WHEN** values entered for the selected method are inconsistent
- **THEN** the app displays no result and identifies the inputs requiring correction

#### Scenario: The app does not interpolate between two recorded rows

- **WHEN** the required load falls between two capacities the user recorded
- **THEN** the app returns the recorded row whose capacity is the closest larger one, or displays no result, and never displays a capacity that lies between the two recorded values

#### Scenario: Rounding a length down is forbidden

- **WHEN** the case's total length falls between two lengths the user recorded for the selected nominal size
- **THEN** the app uses the closest larger recorded length and never displays the capacity recorded at the shorter length, and never offers the shorter length's value as a choice

#### Scenario: A value is not carried over from another row

- **WHEN** the user's table records a row with a higher capacity that also covers the case's length
- **THEN** the app returns only the row that the case's nominal size and the substituted length identify, and does not carry the higher capacity across

#### Scenario: An added longer row does not change a case that already had its exact length

- **WHEN** the user adds a row for a length longer than the case's total length to a table that already records the case's exact length
- **THEN** the app still returns the exact row's recorded capacity and names that exact row, and does not substitute the newly added longer row

### Requirement: Every result has user-recorded provenance

The app SHALL display evaluated inputs and the user's citation with every result, including a
capacity returned from the user's recorded table. A citation SHALL record code/edition, table, row,
or the literal label “user-supplied”, and for a value returned from the user's table it SHALL
additionally record that table's own single citation and the row the app used, and SHALL state
literally that the values are user-recorded. The app SHALL NOT display a result without that label,
SHALL NOT claim it supplied or verified the cited source, and SHALL NOT present a value returned
from the user's table as the code's value or as the app's own value.

#### Scenario: Provenance is missing

- **WHEN** a capacity value lacks the required user citation
- **THEN** the app displays no result and identifies provenance as the missing required input

#### Scenario: A returned value carries the table's citation and the row used

- **WHEN** the app displays a capacity returned from the user's table
- **THEN** the display names the table's recorded code and edition, the table's recorded identifier, the row the app used, and the literal user-recorded marker

#### Scenario: A table with no citation returns no value

- **WHEN** the selected table has no code and edition or no table identifier recorded
- **THEN** the app displays no result and names the missing citation, and does not treat the table as usable

#### Scenario: A returned value is never attributed to the code or to the app

- **WHEN** the result of a return-from-user-table method is read
- **THEN** it states that the value is the user's recorded value and that the app neither transcribed nor verified it, and it does not present the value as supplied, checked, or approved by the app or by any code body

## ADDED Requirements

### Requirement: The user records their own capacity table, with its own declared applicability and one citation

The app SHALL let the user create a device-local capacity table and SHALL require, before the table
can be used, all of: a declared fuel or gas, material or system, inlet-pressure class, permitted
pressure drop, and gas specific gravity; one citation for the whole table comprising the code and
edition and the table identifier; and a literal marker stating that the values are user-recorded.
It SHALL let the user record rows of nominal size, inside diameter where their own table provides
it, length, and capacity. It SHALL NOT supply, prefill, default, suggest, or validate any of these
values or any row content against a value of its own.

#### Scenario: A table is created without its declaration

- **WHEN** the user attempts to use a table whose declared applicability, citation, or marker is incomplete
- **THEN** the app returns no value and names the missing declaration, and does not assume the missing part

#### Scenario: The citation belongs to the whole table

- **WHEN** the app reports the source of a value it read from a row
- **THEN** it reports the table's single recorded citation and the row label, and never prompts for, stores, or displays a per-row citation

#### Scenario: The app supplies no table content

- **WHEN** the table entry surface is opened with nothing recorded
- **THEN** every applicability field, the citation, the marker and the row list are empty, and no nominal size, inside diameter, length, capacity or pressure value is prefilled

### Requirement: The app returns a capacity from the user's recorded table and names the row it used

When the case selects the return-from-user-table method, the app SHALL return the capacity recorded
in the row that the case's declared applicability, nominal size and total length identify, SHALL
return no other value, SHALL name the row it used, and SHALL show the recorded length it used beside
the case's length whenever it substituted one.

#### Scenario: An exact recorded row answers the case

- **WHEN** the case's nominal size and total length are both recorded for the selected table
- **THEN** the app displays that row's recorded capacity and names that row

#### Scenario: A substituted length is named

- **WHEN** the case's total length is not a recorded length for the selected nominal size but a larger recorded length exists
- **THEN** the app displays the capacity recorded at that larger length and displays both the case's length and the recorded length it used

#### Scenario: A nominal size the table does not record

- **WHEN** the case names a nominal size the selected table does not record
- **THEN** the app displays no value and names the size and the sizes the table does record

### Requirement: The app returns the smallest entered size whose recorded capacity carries the required load

When the case selects the reverse method, the app SHALL return the smallest entered nominal size
whose recorded capacity at the case's total length is at least the computed required load, SHALL
name the row it used and the size it returned, and SHALL order entered sizes only by an ordering the
user's own table establishes. It SHALL refuse rather than choose an ordering when the table's own
recorded content does not establish one.

#### Scenario: The smallest carrying size is returned

- **WHEN** two entered nominal sizes both record a capacity at least the required load
- **THEN** the app displays the smaller of the two sizes, the row it used, and that row's recorded capacity

#### Scenario: The load is larger than every recorded capacity

- **WHEN** the required load exceeds every recorded capacity of every entered size
- **THEN** the app displays no size and names the largest recorded capacity and the size it belongs to

#### Scenario: The table's recorded diameters contradict its own size order

- **WHEN** the inside diameters the table records are not in the same order as the sizes' order in the table
- **THEN** the app displays no size and reports the sizes and diameters that disagree instead of choosing an order

### Requirement: The app applies the documented rounding rules, upward only

The app SHALL apply the rounding rules the source procedure documents: use the closest larger
recorded length when the exact length is not recorded, and the closest larger recorded capacity when
the exact capacity is not recorded. It SHALL NOT round a length or a capacity down. It SHALL NOT
substitute silently: whenever it substitutes either, it SHALL display the value the user supplied and
the recorded value it used.

#### Scenario: A length is rounded up only

- **WHEN** the case's total length falls between two lengths the user recorded
- **THEN** the app uses the larger recorded length and displays both the case's length and the recorded length it used

#### Scenario: A capacity is rounded up only

- **WHEN** the required load falls between two capacities the user recorded
- **THEN** the app uses the larger recorded capacity and displays both the required load and the recorded capacity it used

#### Scenario: Rounding down is never the answer

- **WHEN** the exact length is not recorded and a shorter recorded length would also have carried the load
- **THEN** the app does not return the value recorded at the shorter length and does not offer it as a choice

### Requirement: The app interpolates nothing

The app SHALL return only values that exist as rows the user recorded. It SHALL NOT estimate,
average, curve-fit, scale, or otherwise compute a capacity between, beyond, or from recorded rows,
and SHALL NOT convert a recorded capacity to a different length, size, or pressure class.

#### Scenario: No value is produced between two recorded rows

- **WHEN** a query falls between two recorded rows
- **THEN** the app returns one of the two recorded rows or displays no value, and displays no value that is not a recorded row's capacity

#### Scenario: The displayed value equals the recorded row exactly

- **WHEN** the app displays a returned capacity
- **THEN** its value equals, with no adjustment and no rounding, the capacity the user recorded in the row the app named

### Requirement: A table is refused outside its declared applicability

The app SHALL refuse to apply a table when the case's declared material or system, inlet-pressure
class, or gas specific gravity differs from the table's declared applicability in any field, SHALL
name each differing field with both values, and SHALL NOT return any value from that table. The app
SHALL NOT normalise, alias, or tolerate a difference between the two declarations.

Those three comparisons are the whole of the applicability check. The app SHALL NOT compare the
case's fuel or gas against the table's declared fuel, and SHALL NOT compare the case's permitted
pressure drop against the table's declared permitted pressure drop: the case carries neither
reading, so there is nothing to compare and no refusal to raise. A case whose fuel or permitted
pressure drop would differ from a table's declaration is therefore **not** refused for that
difference alone, and the difference goes undetected whenever the material/system,
inlet-pressure-class and gas-specific-gravity readings agree. The table still SHALL require both
declarations before it can be used (see *The user records their own capacity table…*), but a
required declaration is not a comparison, and this requirement records that distinction rather
than implying a check that does not exist.

#### Scenario: A pressure class differs

- **WHEN** the case's pressure class differs from the table's declared inlet-pressure class
- **THEN** the app returns no value and names the pressure class with both the case's value and the table's declared value

#### Scenario: A near miss is still a mismatch

- **WHEN** the case's gas specific gravity differs from the table's declared value by any amount
- **THEN** the app returns no value and names both values, and applies no tolerance

#### Scenario: A differently punctuated material is a mismatch

- **WHEN** the case's material or system and the table's declared material or system differ in wording or punctuation, and not only in letter case
- **THEN** the app returns no value and names both, and does not treat them as the same system

#### Scenario: A difference of letter case or surrounding whitespace is not a mismatch

- **WHEN** the case's material or system and the table's declared material or system differ only in letter case or in surrounding whitespace
- **THEN** the app returns a value and names the table's declaration as matching

### Requirement: A case no recorded row can answer is refused by the limit it hit

When the case's total length is longer than every length the user recorded, or the required load is
larger than every capacity the user recorded, the app SHALL return no value and SHALL name the limit
it hit and the largest value the user recorded.

#### Scenario: The length is longer than every recorded length

- **WHEN** the case's total length exceeds the longest length recorded for every entered size
- **THEN** the app returns no value and names the longest recorded length

#### Scenario: The required load exceeds every recorded capacity

- **WHEN** the required load exceeds the largest capacity recorded for every entered size
- **THEN** the app returns no value and names the largest recorded capacity and the row it belongs to

### Requirement: An empty or partial table is refused with the missing thing named

The app SHALL refuse to return any value when the selected table records no rows, or when a row the
lookup needs is missing a value the lookup requires — a nominal size, a length, or a capacity — or
when two recorded rows share the same nominal size and length. It SHALL name the missing or
duplicated thing and SHALL NOT fill it with a default, a zero, or a value from another row.

#### Scenario: The table records no rows

- **WHEN** the selected table records no rows
- **THEN** the app returns no value and names the table's missing rows

#### Scenario: A needed row is missing its capacity

- **WHEN** the row the lookup identifies records no capacity
- **THEN** the app returns no value and names that row and the missing capacity

#### Scenario: Two rows claim the same size and length

- **WHEN** the selected table records two rows with the same nominal size and the same length
- **THEN** the app returns no value and names the duplicated size and length instead of choosing one of the two

### Requirement: The user's table stays on the device and is not a distribution surface

The user's recorded table SHALL be stored only on the device and SHALL be usable only by the app.
The app SHALL provide no export, share, send, import, file-picker, clipboard or network surface for
the table or its rows, and a shareable case report SHALL NOT carry the table's rows: it SHALL name at
most the table's citation and the row the app used, and SHALL state that the value is the user's own
recorded value.

#### Scenario: The device is offline

- **WHEN** the device has no network connection
- **THEN** the user can create, edit and use a table and obtain a returned value, and no network call is made

#### Scenario: A shared case report does not carry the table

- **WHEN** a case whose value came from the user's table is shared as a report
- **THEN** the report contains the table's citation, the row the app used and the disclaimer, and contains no other row of the table

#### Scenario: No screen or intent sends the table out

- **WHEN** the app's screens and outgoing intents are inspected for a way to move the table or a row off the device
- **THEN** none exists, and no file the app writes contains the table's rows

### Requirement: A value returned from the user's table is labelled as the user's and unverified

Every value the app returns from the user's table SHALL be labelled as coming from the user's own
recorded table, SHALL name that table's citation, SHALL state literally that the app did not
transcribe and did not verify it, and SHALL be displayed with the calculation-aid disclaimer. The
app SHALL NOT describe such a value as the code's value, as a compliant or approved value, or as one
it checked, and SHALL NOT gain any code-compliance claim from returning it.

#### Scenario: A returned value is labelled

- **WHEN** the app displays a value returned from the user's table
- **THEN** that result view carries the user-recorded label, the table's citation, the statement that the app neither transcribed nor verified it, and the calculation-aid disclaimer

#### Scenario: A returned value survives the unit switch with its label

- **WHEN** the user switches the unit system while a returned value is displayed
- **THEN** the physical value, the table's citation, the row used and the user-recorded label are unchanged and the value is re-expressed only
