# Design: pipe-sizing-user-capacity-table

## Context

See `proposal.md` §Why and §Licence basis; neither is restated here.

The slot today is a pure-arithmetic JVM core plus Compose screens:

- `sizing/SizingDomain.kt` holds `SizingCase` (material, pressure class, method, units, loads,
  developed lengths, fitting allowances, a `CapacityEntry`, and three optional physical readings),
  `UserProvenance` (`UserSupplied` | `CodeCitation`), `SizingEvaluator.evaluate(case)`, an
  exhaustive `SizingEvaluation` (`Computed` | `Refused`) and `RefusalKind`
  (`MISSING_INPUT`, `OUT_OF_RANGE`, `INCONSISTENT_INPUT`, `MISSING_PROVENANCE`). `Computed` cannot
  be constructed without a non-null `capacityProvenance`, so a result without a citation is
  unrepresentable.
- Storage is AndroidX `preferencesDataStore` — twice (`sizing_case`, `saved_cases`) — each holding
  a `stringPreferencesKey` whose value is encoded text, not JSON, read through a tolerant
  line codec (`SavedCaseCodec`) that escapes the four structural characters, drops an unreadable
  record instead of throwing, and is regression-tested both ways in `SavedCaseTest`.
- `CasesRepository` is `interface` + `InMemory` (tests, previews) + `DataStore…`, and
  `CasesViewModel` is a thin `stateIn` wrapper. That triple is the pattern to imitate.
- UI is one `Scaffold` in `ui/PlayKilnApp.kt` with a three-tab bottom bar
  (Calculator / Cases / Guide) over `NavDisplay`, and every screen is content-only so it can be
  previewed and tested without an Activity. `ui/home/HomeScreen.kt` holds a two-option
  `SingleChoiceSegmentedButtonRow` for the method and a `SectionCard` vocabulary.
- `ui/home/ResultLines.kt` builds the result as an ordered list of `(stringRes, args)` so the
  *set and order* of result lines is asserted in a JVM test, not read off a screenshot.

Constraints that shape everything below: no new Gradle dependency (the slot is offline and
dependency additions need a reason), no server, no Room, and every capacity-like value in the
build must be one the user typed.

## Goals / Non-Goals

**Goals**

- Let the user record their own table once and reuse it across cases.
- Return a capacity for a size and a length, and return the smallest carrying size for a load and
  a length, **from the user's recorded rows only**.
- Apply the source procedure's documented rounding rules **in the conservative direction only**,
  and make every substitution visible.
- Make a returned value impossible to mistake for an arithmetic comparison, for the code's value,
  or for a value the app verified.
- Keep the table on the device, with no way to move it off.

**Non-Goals**

- No distribution surface for the table (no export, share, import, picker, clipboard, network).
- No embedded, bundled, downloaded, defaulted, interpolated, averaged or curve-fitted value.
- No equation route, no manufacturer-system data, no per-row citation, no compliance claim.
- No change to `differentiators.json`, `store/studio.json`, `store/journey.json` or
  `store/listing.json` — the claims those carry move against implemented code and real proofs.
- No new Gradle dependency and no new persistence technology.

## Decisions

### D1 — The entity, and why its applicability is mandatory and self-declared

```kotlin
// sizing/CapacityTable.kt  (new)
const val USER_RECORDED_MARKER: String = "user-recorded"

data class TableApplicability(
    val fuel: String,                    // gas / fuel
    val materialOrSystem: String,        // material or system
    val inletPressureClass: String,      // inlet-pressure class
    val permittedPressureDrop: String,   // recorded as read, like the case's pressure-drop field
    val gasSpecificGravity: String,      // the user's own reading, parsed for comparison
)

data class TableCitation(
    val codeAndEdition: String,
    val tableIdentifier: String,
)

data class TableRow(
    val nominalSize: String,
    val insideDiameter: Double?,         // optional: only when the user's own table provides it
    val lengthFeet: Double,
    val capacityMbh: Double,
)

data class UserCapacityTable(
    val id: String,                      // stable identity for a case to reference
    val name: String,
    val applicability: TableApplicability,
    val citation: TableCitation,
    val marker: String,                  // must equal USER_RECORDED_MARKER
    val rows: List<TableRow>,            // order is meaningful: see D4
)
```

Every applicability field is required. The app **must never assume** an envelope, because an
assumed envelope is the app deciding which readings the user's table may be applied to — which is
the failure this whole change exists to prevent. The alternative (make applicability optional and
warn) was rejected: `proposal.md` §Non-Goals and the promise *"Nothing gets assumed"* already
recorded that posture for the existing case, and a warnable envelope is exactly a silently applied
one.

`permittedPressureDrop` and `gasSpecificGravity` are text, matching the case's existing treatment
of pressure drop and diameter as *"recorded exactly as the code book states them"*. Length and
capacity, by contrast, have unambiguous conversions, so rows are entered in the user's selected
unit system and stored canonically (feet, MBH) like the case's own readings — one physical value,
two presentations, no drift on toggle. Alternative considered: store the table exactly as typed and
never convert it, on the grounds that the table is the user's transcription. Rejected because it
would make the same physical row behave differently from the same physical case reading and would
break a unit switch mid-transcription.

### D2 — One citation for the whole table, plus a literal marker

A real capacity table is headed by its citation, and the user transcribes one table, so the entity
carries **one** citation (code and edition, table identifier) and one literal marker. The app SHALL
NOT prompt for, store or display a per-row source: a per-row citation would imply the app
distinguishes rows it did not read.

The marker is a constant, not free text, so the label cannot drift: the result view renders it from
`USER_RECORDED_MARKER` rather than from a hand-written string
(`.cursor/rules/verification-discipline.mdc` — one source of truth, imported).

### D3 — The forward lookup, and the direction of its rounding

`returnCapacityFromUserTable(case, table)`:

```
1. require table != null                                     else SIZE/NOT_APPLICABLE refusals (D6)
2. require applicability matches exactly                     else APPLICABILITY_MISMATCH (D7)
3. require table complete (citation, marker, rows, no row
   missing size/length/capacity, no duplicate (size,length)) else TABLE_INCOMPLETE (D6)
4. size  := case.tableSelection.nominalSize                  else NOMINAL_SIZE_MISSING
5. rows  := table.rows where row.nominalSize matches size
           (trimmed, case-folded comparison)                 else SIZE_NOT_IN_TABLE (names the size)
6. exact := rows.firstOrNull { it.lengthFeet == case.totalLengthFeet }
7. used  := exact
           ?: rows.filter { it.lengthFeet > case.totalLengthFeet }
                  .minByOrNull { it.lengthFeet }
           ?: return LENGTH_ABOVE_EVERY_ENTERED_ROW (names the longest entered length for that size)
8. return ReturnedFromUserTable(
       returnedCapacityMbh = used.capacityMbh,     // the recorded number, verbatim, unadjusted
       rowLabel            = used.label,
       returnedSize        = size,
       substitution        = if (exact == null) LengthSubstitution(case.totalLengthFeet, used.lengthFeet) else null,
       provenance          = RecordedTableCitation(table.id, citation.codeAndEdition, citation.tableIdentifier))
```

Step 7 is the rounding rule and its direction is the whole point: `lengthFeet > totalLengthFeet`
then `minBy` selects the **closest larger** recorded length, never the closest smaller one. Because
capacity falls as length rises, a larger length yields the lower — conservative — capacity.
Rounding **down** would return a capacity for a shorter run than the user is building, which is the
one direction that can produce an unsafe answer; it is forbidden in the spec, not merely avoided
here.

Step 6 uses exact equality on the canonical double. That is safe precisely because both sides are
canonical feet: the case's total length is a sum of canonical readings and the row's length is a
canonical reading of the same unit, so "the exact length is listed" means the same number, not a
tolerance the app invented. Alternative considered: an epsilon to absorb float noise. Rejected — an
epsilon is the app deciding how much of a mismatch is acceptable, which is a decision the source
procedure does not delegate to the app, and it would let a rounded-down row match.

### D4 — The reverse lookup, and where the size ordering comes from

`smallestSizeCarryingLoad(case, table)`:

```
1. steps 1–3 of D3 (applicability, completeness)
2. sizes := distinct nominal sizes in the order they first appear in table.rows
3. require sizes' order agrees with the inside diameters the table also records,
   where both are present                                       else SIZE_ORDER_INDETERMINATE
4. candidates := for each size in `sizes`:
       rows := rows for that size
       exact := rows.firstOrNull { it.lengthFeet == case.totalLengthFeet }
       used  := exact ?: rows.filter { it.lengthFeet > case.totalLengthFeet }.minByOrNull { it.lengthFeet }
       used == null  -> this size cannot answer (skip, do not refuse yet)
       else          -> (size, used)
5. require candidates is non-empty                             else LENGTH_ABOVE_EVERY_ENTERED_ROW
6. carrying := candidates.filter { it.used.capacityMbh >= case.requiredLoadMbh }
7. require carrying is non-empty
       else LOAD_ABOVE_EVERY_ENTERED_CAPACITY (names the largest recorded capacity and its size)
8. picked := carrying.first()      // `candidates` is already in the user's own size order
9. return ReturnedFromUserTable(returnedSize = picked.size, returnedCapacityMbh = picked.used.capacityMbh,
       rowLabel = picked.used.label, substitution = …, provenance = …)
```

**Size ordering is the one place the app could be tempted to supply its own data.** A trade size
ladder (`1/2 < 3/4 < 1 < 1-1/4 …`) is exactly the kind of table-derived constant the memo forbids,
so the app does not hold one. Instead:

- the order is the order in which the user's own rows first present each nominal size — the user is
  transcribing a table that lists sizes in ascending order, so the table's own order is the
  user's transcription, not the app's knowledge;
- where the table also records inside diameters, the app **cross-checks** the two and refuses with
  `SIZE_ORDER_INDETERMINATE` when they disagree, rather than silently preferring one. A
  contradiction means the transcription is wrong somewhere, and returning a size from a table whose
  own two orderings disagree is confidently returning a value from a table the app can see is
  inconsistent.

Alternatives considered: (a) a hard-coded trade-size ladder — forbidden, it is app-supplied code
data; (b) order by inside diameter only, requiring the user to enter a diameter for every row —
rejected because a real table need not print inside diameters, and D1 makes the diameter optional;
(c) let the user declare a rank per size — rejected as a second transcription surface the user must
get right for no gain over the row order.

Note the shape of step 4: the forward rounding of D3 is applied per size first, so by the time a
size is compared against the load there is exactly **one** candidate row per size and the
comparison is against a recorded capacity — never against an interpolated one. That is how the
procedure's *"closest larger capacity"* clause is honoured without the app ever producing a value
that is not a row: the chosen row is the closest larger capacity among the rows that cover the
case's length, and `carrying.first()` is the smallest of those in the user's own size order.

### D5 — No interpolation, and byte-identity of the returned value

The returned capacity is `used.capacityMbh` — the stored `Double` of a row the user entered, moved
without arithmetic. A unit test asserts exact equality (`0.0` tolerance) against the recorded row,
not a converted or rounded value, so an implementation that scaled, averaged or curve-fit a value
would fail rather than pass close. No capacity-like quantity in the new paths is computed: the only
arithmetic the new method performs is the case's own required-load and total-length sums, which the
existing evaluator already produces and which `SizingDomain.kt` already owns.

### D6 — Refusal taxonomy

The existing `RefusalKind` is an enum in `SizingDomain.kt`; the new kinds are added to it so there
is one refusal vocabulary and one `SizingEvaluation.Refused` carrier, rather than a parallel
mechanism. Every kind names the correction in `correctionRequired`, built from the check itself,
never from a hand-written message that restates the rule.

| `RefusalKind` | Fires when | `correctionRequired` names |
|---|---|---|
| `NO_TABLE_SELECTED` | the table method is selected and the case names no table | "select the table you recorded" |
| `TABLE_INCOMPLETE` | the table is missing its citation, marker, a row, a required cell, or has a duplicate `(size, length)` | the table, the row and the missing or duplicated thing |
| `APPLICABILITY_MISMATCH` | any applicability field differs (D7) | each differing field with both values |
| `SIZE_NOT_IN_TABLE` | the case's nominal size is not recorded | the size, and the sizes the table does record |
| `NOMINAL_SIZE_MISSING` | the forward method is selected and no size is entered | "enter the nominal size you are reading" |
| `LENGTH_ABOVE_EVERY_ENTERED_ROW` | no recorded length for any candidate size reaches the total length | the longest recorded length |
| `LOAD_ABOVE_EVERY_ENTERED_CAPACITY` | every candidate capacity is below the required load | the largest recorded capacity and the size it belongs to |
| `SIZE_ORDER_INDETERMINATE` | the declared size order contradicts the recorded diameters | the sizes and diameters that disagree |
| `TABLE_CHANGED` | the case was saved against a table whose recorded contents have since changed (D9) | that the table changed and must be re-read |

`TABLE_INCOMPLETE` and `SIZE_NOT_IN_TABLE` are separate because the first says *your table cannot
be used* and the second says *your table can be used but not for this size*; collapsing them would
make an incomplete table look like a legitimate out-of-scope answer.

### D7 — Applicability comparison, with no tolerance and no aliasing

**Amended 2026-09-27 to match the artifact (`tasks.md` §2.3.2).** An earlier revision of this
decision listed `fuel` and `permittedPressureDrop` among the compared fields. `SizingCase` carries
neither, so neither comparison can fire; the set below is what the code actually performs, and the
two omissions are recorded here rather than left implicit.

- `materialOrSystem` and `inletPressureClass`: the case's recorded text is compared against the
  table's declared text after trimming and case-folding, and any difference is a mismatch. The app
  does **not** normalise synonyms, trade aliases, or unit spellings — recognising that
  `Schedule 40` and `Sch. 40` are the same system is app-supplied domain knowledge, and it is
  exactly the kind of knowledge the app must not hold.
- `gasSpecificGravity`: both sides are parsed to `Double` and compared with exact equality.
  **No epsilon.** A tolerance is the app deciding how large a mismatch it may apply the table
  across, and `proposal.md` §Non-Goals says the app never applies a table outside its declared
  envelope. An unparseable value on either side is a `TABLE_INCOMPLETE` or `OUT_OF_RANGE` refusal,
  not a silent mismatch, so the user is told which entry is unusable rather than which two numbers
  "differ".
- **`fuel` is NOT compared.** `SizingCase` carries no fuel field, so there is no case reading to
  compare against the table's `applicability.fuel`. The table still *requires* the field
  (`validationRefusal` refuses a blank one), but requiring a declaration is not the same as
  checking it against the case.
- **`permittedPressureDrop` is NOT compared.** The table records it as text
  (`permittedPressureDrop: String`, *"recorded as read"* per D1) while the case's pressure-drop
  input is a numeric allowance (`pressureDropAllowance: SourcedValue`). They are not the same
  quantity, and comparing them would mean the app inventing the conversion between a transcribed
  pressure-drop phrase and a computed allowance — app-supplied domain knowledge again.

**Residual consequence, written down rather than hidden.** Because those two fields are not
compared, a case declared as propane can be run against a table declared for natural gas, and the
mismatch is **not detected**. The material/system, inlet-pressure-class and gas-specific-gravity
checks are the whole of the applicability check; anything a case does not carry is a difference the
app cannot see. `spec.md`'s applicability requirement states the same limitation in its own words,
and no scenario in that spec asserts a refusal for either field, because no such refusal exists
(`tasks.md` §2.3.2).

**Rejected alternative: add `fuel` and `permittedPressureDrop` to `SizingCase`.** The decision is
recorded and this change does not take it. Widening the case's input surface is rejected: the
slot's whole licence position is that the app owns no applicability knowledge, and the case's
declared text is the only envelope there is. Two more user-entered fields would lengthen the form
before any sizing happens without making the comparison stronger — the app would still be comparing
two strings the user typed, with no ability to tell a wrong fuel from a differently worded one.
The honest answer is to say which comparisons exist, not to add fields that make the list look
complete.

### D8 — Persistence: the same mechanism, not a second one

The table is stored with the app's existing device-local technology, imitating `CasesRepository`
exactly:

```kotlin
// ui/tables/TablePersistence.kt  (new — placed beside the surface that owns it, like
// ui/cases/CasesRepository.kt, because it is the only Android-context-dependent part)
private val Context.capacityTablesStore by preferencesDataStore(name = "capacity_tables")

interface TablesRepository {
    val tables: Flow<List<UserCapacityTable>>
    suspend fun upsert(table: UserCapacityTable)
    suspend fun delete(id: String)
    suspend fun get(id: String): UserCapacityTable?
    suspend fun clear()
    class InMemory(...) : TablesRepository        // tests and previews, never a production backend
}

class DataStoreTablesRepository(context: Context) : TablesRepository   // one stringPreferencesKey("tables")
```

No Room, no JSON dependency, no new Gradle dependency, no server: the storage *kind* is the one the
slot already uses, applied to a second key space, wrapped in the same `interface` + `InMemory` +
`DataStore` triple so the ViewModel is testable exactly as `CasesViewModel` is.

The codec reuses the tolerant line format's design — four escaped structural characters, one record
per line, an unreadable record dropped rather than throwing, because a killed write must not cost
the user every table. **It must not restate the escaping rule**: the contract step extracts
`escapeField`/`unescapeField` and the separator constants into one new shared helper
(`ui/storage/LineCodec.kt`), both codecs import it, and `SavedCaseTest`'s existing
escaping-both-ways check is the regression sensor for pulling `SavedCaseCodec` onto it. A second
hand-written escape implementation is the defect class
`.cursor/rules/verification-discipline.mdc` names under *"One source of truth, imported — never
restated"*.

The alternative — storing rows inside the existing `saved_cases` blob — was rejected: a table has a
different lifetime from a case (it outlives the cases that use it, and deleting a case must never
touch a table), and entangling them would make one blob's corruption cost both.

### D9 — A saved case binds to the revision of the table it was read against

A table is editable, and a case's returned value is a claim about a specific set of recorded rows.
If the user edits a capacity and later loads a case saved before the edit, showing the old number
would be a confident answer for a table that no longer exists, and recomputing silently would
change the answer without saying so. Decision:

- the saved case stores the table's `id` **and a SHA-256 digest of the table's encoded contents**
  (`java.security.MessageDigest` — no new dependency), alongside the nominal size it was read for;
- on load, if the table is absent or its digest differs, the case's readings load unchanged and the
  app refuses at evaluation with `TABLE_CHANGED`, naming that the table moved;
- the digest is never displayed as a value or a code; it is only a change sensor.

This is the same posture the existing code already takes twice — the saved case stores no
evaluation (`"a stored result would otherwise be shown for inputs the user cannot see"`) and any
edit clears the shown result. The alternative (snapshot the used row into the saved case) was
rejected: it would put a copy of the user's code reading into a second place and make the case, not
the table, the thing of record.

### D10 — A returned value gets its own representation

`SizingEvaluation` gains a third variant and `UserProvenance` gains a third case:

```kotlin
sealed interface UserProvenance {
    data object UserSupplied : UserProvenance
    data class CodeCitation(val codeAndEdition: String, val table: String, val row: String) : UserProvenance
    /** A value the app read out of the user's own recorded table; not a code reading. */
    data class RecordedTableCitation(
        val tableId: String,
        val codeAndEdition: String,
        val tableIdentifier: String,
    ) : UserProvenance
}

sealed interface SizingEvaluation {
    data class Computed internal constructor(...) : SizingEvaluation
    data class ReturnedFromUserTable internal constructor(
        val requiredLoadMbh: Double,
        val totalLengthFeet: Double,
        val returnedCapacityMbh: Double,          // the recorded row's value, unadjusted
        val returnedSize: String,
        val rowLabel: String,
        val substitution: LengthSubstitution?,    // (enteredLengthFeet, usedLengthFeet) when substituted
        val capacityProvenance: UserProvenance.RecordedTableCitation,
    ) : SizingEvaluation
    data class Refused(val kind: RefusalKind, val correctionRequired: String) : SizingEvaluation
}

data class LengthSubstitution(val enteredLengthFeet: Double, val usedLengthFeet: Double)
```

Reusing `Computed` and stuffing the table citation into its `capacityProvenance` was rejected
outright: `Computed` means *the app compared the user's number against the load*, and a returned
value means *the app showed the user their own row*. Sharing one type would let the result card —
and the shared case report — render a returned value with the arithmetic comparison's wording. A
distinct variant makes the two impossible to confuse, and because `HomeScreen.ResultCard` and
`ResultLines` switch exhaustively over the sealed type, the Kotlin compiler forces the UI to handle
the new case; no `else -> error(...)` may be added, which would remove that check.

`RecordedTableCitation` implements `UserProvenance` so the invariant that a result cannot exist
without provenance keeps holding for the new variant without a second rule.

### D11 — UI shape

The slot has three tabs and the Calculator owns the method selector. Consequences:

- **A fourth tab, `Tables`** — the entry surface. A list of the user's tables (name, citation,
  applicability summary, row count, a change/delete affordance), then one table open for editing:
  the applicability header, the single citation, the marker confirmation, and the row list with
  per-field entry and add/remove. Because the row order is the size order (D4), the screen shows
  the derived size order read-only, so the user can see what the app will use and correct a
  mis-ordered transcription before a case depends on it.
- **The Calculator's method selector** grows from two options to four. The existing
  `SingleChoiceSegmentedButtonRow` will not hold four labels once the longest locale's words are in
  them (`.cursor/rules/visual-identity.mdc` — a layout whose style depends on English string
  lengths is a defect that only appears after localisation), so the method selector becomes a
  vertical list of choices inside the existing `SectionCard`. Selecting a table-returning method
  reveals a table picker, plus a nominal-size field for the forward direction.
- **Per-field entry, not paste.** v1 records rows through the field form. Multi-line paste is
  deferred: it is a second parsing surface (delimiters, column order, unit ambiguity) on a feature
  whose whole risk is transcription error, and the measured complaint is answered without it.
- **The result view** adds, in the existing line vocabulary: the table's citation, the row used,
  the size returned, and — only when a substitution happened — the entered length and the length
  used. It then states the literal user-recorded marker and the app's neither-transcribed-nor-verified
  statement, and keeps the existing always-visible calculation-aid disclaimer. Nothing about the
  new method may shorten or reorder the existing context-first lines: `ResultLinesTest` asserts that
  a figure is never the first thing read.
- **The shared case report** gains the same citation/row/substitution lines and the
  no-verification statement, and must **not** contain any other row of the table. The report is a
  text `Intent`; the moment it carries the row inventory the app becomes a redistribution channel,
  which is the one thing this change's licence basis cannot survive (D12).

### D12 — Licence basis, and the alternatives it rejects

Restating the deciding constraint as a design input rather than as background:
`docs/research/2026-09-25-pipe-sizing-code-source.md` §5 ranks the routes and its decision table
requires *"a written NFPA or ICC licence explicitly allowing app reproduction; chosen edition/TIAs;
a reviewer-signed transcription and diff"* before a cell may be embedded, and states *"No embedded
numbers until a right is documented"*. Embedding is therefore rejected, the equation route is
rejected (its copyright status is left `UNVERIFIED` there), licensed manufacturer data is rejected
(no written permission), and the user-entered lookup is taken because the app *"does not
redistribute the table"*.

That single fact decides D1 (the applicability the user declares is the app's only envelope
knowledge), D4 (no trade-size ladder), D5 (no computed value) and D11 (no export surface). Any
future proposal to add a share/export surface for the table is a proposal to change the licence
answer, not a UI change.

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| **Wrongly recorded, confidently returned.** A mistyped capacity is returned as a fact. | This is the risk the route accepts. Every result view and the shared report label the value as the user's own, name the citation and the row, and state that the app neither transcribed nor verified it; the calculation-aid disclaimer stays visible. The app cannot do better without reading the code, which it may not do. |
| The user's table order is used as the size order, so a mis-ordered transcription yields a wrong "smallest carrying size". | The recorded inside diameters are cross-checked against the order and a contradiction refuses (D4); the derived order is shown read-only on the table screen (D11). |
| Exact float equality on `gasSpecificGravity` refuses a case the user considers identical (`0.62` vs `0.620`). | Both sides are parsed to `Double` first, so `0.62` and `0.620` are equal; only genuinely different readings refuse. A tolerance would be the app choosing an envelope (D7). This will occasionally read as fussy; the alternative reads as applying a table outside its declaration. |
| The method selector's growth changes an existing screen's layout and the store screenshots that photograph it. | The change is additive in the result lines (asserted by `ResultLinesTest`) but the method control's shape changes; `store/journey.json`'s coordinates are re-measured at release, which is already the slot's recorded practice for a layout change. See the deferred list below. |
| A digest-bound saved case refuses after any table edit, including a harmless rename. | Deliberate: a rename is a change to the table of record, and refusing names it. The alternative is silently recomputing. |
| `RefusalKind` grows from four to thirteen members, and some existing exhaustive `when` blocks over it must be extended. | The compiler enforces it, and the repo's rule forbids an `else` branch that would hide a new member. |

## Migration Plan

Additive and forward-only, with no migration of existing data:

1. New files and new enum/sealed members land first; the existing `evaluate(case)` keeps its
   behaviour for the two existing methods, and `restoredFrom` keeps its `?:` defaults, so a store
   written by the shipped build restores unchanged.
2. New saved-case keys (`TABLE_ID`, `TABLE_DIGEST`, `TABLE_SIZE`) are read with defaults, so a case
   saved before this change loads with no table selected and refuses only if the user selects a
   table method.
3. No schema version bump and no destructive step. A removed table degrades its cases to
   `TABLE_CHANGED` refusals rather than losing them.
4. Rollback is deleting the new files and the added members; nothing existing is reshaped, so an
   older build reading the store sees only keys it ignores.

## Owner decisions recorded 2026-09-26

Both questions were put to the owner and answered. The analysis is kept because it is what the
answer rests on; the answers bind this change.

1. **The table surface is free.** The user-owned table and the values it returns are available
   without purchase. The existing one-time Pro unlock keeps exactly the boundary it had, and this
   change adds no new purchase surface, no new ad surface and no paywall copy. Consequence for
   `/release`: the listing may describe the table as a free capability, and the free/paid boundary
   `economics.json` describes (`ads_led_utility`, priceUsd 4.99, capabilities `ads` + `billing`) is
   unchanged and must not be re-cut for this feature.
2. **Several named tables ship in v1.** One table cannot serve two pressure classes or materials,
   and a case names the one it was read against, so the full design applies: the table list, table
   identity on the case, and name/citation collision handling are all in scope. The single-table
   simplification is rejected.

## Deferred: store copy and journey review at `/release` (do not edit those files in this change)

`store/studio.json` and `store/journey.json` are **not** edited here. Their recorded claim bound
says the app *"performs sizing ARITHMETIC on readings the user supplies; it ships no code-table
data, **does not determine the pipe size itself**, and does not determine compliance"*. After this
change the middle clause is partly false: the app can return a size — from the user's own recorded
table, which it still does not determine. Two claims that mention the bound move:

| File | What must move | When |
|---|---|---|
| `store/studio.json` | `_claim_bounds` — "does not determine the pipe size itself" must be restated as "returns a size only from the table the user recorded", and `featurePill` (`OFFLINE · NO CODE TABLES`) stays true and should be re-affirmed | `/release`, against the built app |
| `store/studio.json` | `headlines`/`kickers` — a shot of the table surface and a shot of a returned value are the honest images for this feature; the list is one-per-journey-shot, so a new shot needs a new headline and kicker | `/release` |
| `store/journey.json` | the applied method now offers four options, so every `step` that taps the method control has stale coordinates; a table shot needs its own step list and `expect` readings, and a returned-capacity shot must assert the row named and the substitution, not just a number | `/release`, re-measured from a live `uiautomator` dump as the file's own comment requires |
| `store/journey.json` | shot-06's `Capacity provenance: CodeCitation(...)` expectation is about the *existing* method and stays valid; the new method must not be asserted with the old method's expected string | `/release` |
| `store/listing.json` | `fullDescription` says the app *"compares the capacity you confirm against the load you need"* and the feature bullets say the same; both become incomplete | `/release` |
| `app/src/main/res/values/strings.xml` | `guide_purpose_body` ("It sums the loads … and compares the capacity you confirmed") and `about_body` need the new method described truthfully | in this change, by the glue step — the Guide is user-visible copy the feature makes incomplete |
| visual identity | the new tab's nav iconography, and whether the slot's existing stock Material nav icons already conflict with `.cursor/rules/visual-identity.mdc` ("a stock icon as the app's mark or as UI iconography"), is a visual-identity review item | `/release` |

## `differentiators.json`: which claims this change is expected to move

`apps/2026-09-25-pipe-sizing/differentiators.json` is **not** edited in this change — a verdict must
follow implemented code with a real proof, not a plan. The two the brief names, and what each would
then need:

| Claim | Weight / status today | Where this change is expected to move it | Proof it would then need |
|---|---|---|---|
| `returns-the-missing-values` | `core`, **`dropped`** | To **`narrowed`** — not `shipped`, because the promise's clause *"from the standard"* is still not delivered: the app returns the **user's** recorded value, not the standard's. `narrowedTo`: "takes the length and the load a fitter has and returns the capacity, and the smallest entered size, from the table the fitter recorded, naming the row it used". `lostCoverage`: "the returned values are the fitter's transcription, not the standard's; the app neither transcribes nor verifies them, so a wrongly recorded row is returned confidently". Needs an **`ownerDecision`** in the owner's own words (it is a core promise being changed), and the decision is the one recorded for this change's route. | `test` — a unit test in the new lookup suite, e.g. `returnsTheSmallestEnteredSizeThatCarriesTheLoad` in `app/src/test/java/com/gexetr/pipesizing/sizing/CapacityTableLookupTest.kt` |
| `records-the-citation` | `core`, **`narrowed`** | Its `narrowedTo`/`lostCoverage` no longer describe the app: the citation can now point into a table the user owns and **the app names the row it used**, so the surviving part grows and the lost coverage narrows. Still `narrowed`, never `shipped` — the app records and prints the user's citation but still does not verify that the cited row supports the load. Needs an updated `ownerDecision` (core) alongside the existing one. | `code` — `UserProvenance.RecordedTableCitation`, in the file that declares it (`app/src/main/java/com/gexetr/pipesizing/sizing/SizingDomain.kt`) |

Two more that this change touches without changing their status, and which are **assertion-text
drift** rather than gate failures — the gate resolves a symbol, so it will keep passing while the
assertion stops being true:

| Claim | Drift | Action at `/eval` |
|---|---|---|
| `every-input-editable` | Its assertion says *"HomeField enumerates thirteen editable readings"*; this change adds fields, so the count becomes wrong while the symbol still resolves | re-word the assertion to name the fields rather than count them |
| `shows-its-arithmetic` | Its assertion enumerates the rendered result lines; the new method's lines are not in that list | extend the assertion with the citation, row-used and substitution lines |

**Hard dependency, verified today:** `node scripts/check-differentiators.mjs --slot pipe-sizing`
currently returns **`VERDICT: BLOCKED`** with three findings — `returns-the-missing-values`,
`records-the-citation` and `shows-its-arithmetic` are core promises that were narrowed or dropped
without an `ownerDecision`. Because `scripts/verify-slot.mjs` invokes that gate, **this slot cannot
reach a green slot verification until the owner records his own words for those three**, whatever
this change does. That is not a blocker for building the feature; it is a blocker for calling the
slot done.

## Base spec / OpenSpec structure

`openspec/specs/fuel-gas-pipe-sizing/` does not exist; the base requirements this delta modifies
live only inside the still-active `openspec/changes/app-pipe-sizing/`. Both facts below were
verified against `openspec` 1.10.0 on this machine, not assumed:

- `openspec validate pipe-sizing-user-capacity-table` **passes** with MODIFIED blocks whose
  requirements exist nowhere in `openspec/specs/`. `validate` does not check them against a main
  spec, so a clean validate is not evidence that the delta can be applied.
- `opsx-sync` (and `archive`) resolve a MODIFIED block by finding the requirement in the main spec.
  With no main spec, `openspec-sync-specs` takes its *create-new-main-spec* path, which adds
  **`ADDED` requirements only** — so the three MODIFIED blocks would be dropped silently while the
  main spec is created from the ADDED ones, and the contradiction this change exists to close would
  survive into the spec of record.

Therefore the base delta must be synced into `openspec/specs/` before this change is applied or
archived: `opsx-sync app-pipe-sizing` (or archive it), then confirm the three MODIFIED headers match
the main spec verbatim, because a MODIFIED header must match exactly (whitespace-insensitive).

## Pinned interfaces for the parallel split (§`tasks.md`)

The contract step fixes these so the two parallel workers never edit the same file. `SizingDomain.kt`
is edited once, by the contract step, and the new method's algorithm lives entirely in
`CapacityTableLookup.kt`:

```kotlin
// SizingDomain.kt — the only line the contract step adds to SizingEvaluator, keeping every
// existing `evaluate(case)` call site compiling via the defaulted parameter:
fun evaluate(case: SizingCase, table: UserCapacityTable? = null): SizingEvaluation =
    if (case.method.returnsFromUserTable) {
        evaluateCaseAgainstUserTable(case, table)
    } else {
        evaluateArithmetically(case)          // the existing body, renamed, behaviour unchanged
    }

val SizingMethod.returnsFromUserTable: Boolean
    get() = this == SizingMethod.RETURN_FROM_USER_TABLE || this == SizingMethod.SMALLEST_SIZE_FOR_LOAD
```

```kotlin
// CapacityTableLookup.kt — implemented by the lookup worker; the contract step must not write it.
internal fun evaluateCaseAgainstUserTable(
    case: SizingCase,
    table: UserCapacityTable?,
): SizingEvaluation
```

---

## Addendum: findings that contradict or extend the brief

Recorded here so they are visible to the owner or orchestrator rather than lost in a transcript.

1. **`check-differentiators` is already `BLOCKED`** (three core claims without `ownerDecision`).
   The brief's framing implies the contract is settled; it is not, and it blocks the slot gate.
2. **The base spec is not in `openspec/specs/`**, and `opsx-sync`/`archive` would silently drop the
   MODIFIED blocks. The brief anticipated this ("say so explicitly"), and it is worse than a
   validity question: the CLI's own capability instruction says a Modified Capability must use
   *"the exact existing path under `openspec/specs/`"*, which does not exist here.
3. **`store/listing.json` carries reserved-host placeholders** — `contactEmail` is
   `support@example.com` and `privacyPolicyUrl` is `https://example.com/privacy`. `AGENTS.md` and
   `project-conventions.mdc` say the pack must not carry a reserved-host placeholder and that
   `DEFAULT_CONTACT_EMAIL` (`v.sobolenko.vs@gmail.com`) is applied as the default for every new
   slot. These are pre-existing, unrelated to this change, and block a release-phase `--strict-store`
   claim.
4. **`openspec/config.yaml` does not parse** (`Implicit keys need to be on a single line at line 13,
   column 7`), so every artifact rule it declares is silently ignored by the CLI. Pre-existing.
5. **`openspec validate --specs` already fails** `android-compose-baseline` and `play-compliance`,
   and `capabilities` carries 0 requirements. Pre-existing; not caused by, and not fixed by, this
   change.
6. **The brief lists two claims to move**; the file itself shows two more whose *assertion text*
   goes stale (`every-input-editable`'s "thirteen editable readings", `shows-its-arithmetic`'s line
   list). Both keep passing their gate while being untrue, which is the failure mode the promise
   contract's own §"What this gate does not prove" describes.
