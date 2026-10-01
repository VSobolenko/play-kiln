# Proposal: pipe-sizing-user-capacity-table

> **KILLED 2026-09-30 — the slot it extended is dead.** This change was 34/36 complete and its code is
> on disk in `apps/2026-09-25-pipe-sizing` (`TablesScreen`, `TableCodec`, `CapacityTableTest`), but
> the two remaining tasks were verification of a slot that `registry/apps.json` now records as
> `killed`, so they can never be closed. The work is not in flight; the branch is over. See
> `proposal.md` of `app-pipe-sizing` for the kill reason and `docs/factory-checks.md` for the
> stage-11 instrument.
>
> **Archived without merging its delta spec**, for the same reason as `app-pipe-sizing`: a capability
> that ships nowhere must not be advertised in `openspec/specs/`, which `/ship` tells an agent to read
> first.

## Why

The slot is a calculation aid that only checks arithmetic on a capacity the user already looked
up. The measured complaint it was selected to answer says the opposite: a reviewer of a 100K+
incumbent writes *"We know\knew the length and pipe diameter. What I am looking for is to enter
those and get the others based on industry standards"*, and a paid incumbent's review says it
*"would not return a value for a long length high demand run"*
(`docs/research/2026-09-24-market-decision.md` §2.2). Today the app cannot return anything,
because it owns no values, so `differentiators.json` records the recon clause
`returns-the-missing-values` as **dropped** and `records-the-citation` as **narrowed**.

The owner was offered three routes and chose the user-owned one: the app stores a capacity table
**the user enters from their own code book**, and returns values from that table. That is the route
`docs/research/2026-09-25-pipe-sizing-code-source.md` §5 prices as *"Lower because the app does not
redistribute the table"* while the embed/equation routes are *"High until written permission"*.

## Licence basis (the governing constraint)

This memo's rule is *"No embedded numbers until a right is documented"*, and its decision table
requires, before a cell may be embedded, **a written NFPA or ICC licence permitting app
reproduction, a frozen edition with its TIAs, and a reviewer-signed transcription** by a qualified
fuel-gas code professional; it states that *"a developer self-check is not credible safety
evidence"*. The memo also settles that free viewing at `codes.iccsafe.org` is not redistribution
permission and that IFGS material carries an additional AGA copyright.

A table of the user's own readings **keyed in by the user, stored on the user's own device, and
never distributed by the app** reproduces nothing: the app still ships zero code-table data and is
not a channel for the code's data. That is the entire legal basis of this change, and this change
does not weaken it.

Honest residual, quoted from the same memo rather than glossed: the user-entered-lookup route
carries *"**UNVERIFIED:** obtain counsel's confirmation"*. This change proceeds on the memo's own
ranking of the routes, and the residual is not a gap this change may close by assertion.

## What Changes

- **A user-owned capacity table entity.** The user creates it once and reuses it across cases. It
  carries (a) its own **declared applicability** — fuel/gas, material or system, inlet-pressure
  class, permitted pressure drop and gas specific gravity, mirroring the applicability headers a
  real capacity table carries — which the app never assumes; (b) rows of nominal size, inside
  diameter (where the user's table provides it), length and capacity; and (c) **one citation for
  the whole table** (code and edition, table identifier) plus a literal marker that the values are
  user-recorded.
- **A return-from-table sizing method.** Given the case's applicability, a nominal size and a
  total length, the app **returns** the recorded capacity and **names the row it used**.
- **The reverse direction.** Given the required load and the total length, the app returns the
  **smallest entered size whose recorded capacity carries the load**, or refuses by name.
- **The code's own documented rounding rules, upward only.** The closest **larger** recorded length
  when the exact length is not entered, and the closest **larger** recorded capacity when the exact
  capacity is not entered. Rounding **down is forbidden** — it would return a capacity for a shorter
  run than the user is building — and every substitution is **shown**.
- **Interpolation is forbidden.** The app never produces a value that is not one of the user's
  recorded rows: no estimate, average, curve-fit, scaling or conversion between rows.
- **A new refusal taxonomy** naming applicability mismatch, out-of-envelope length or load, empty
  or partial table, an unknown size, and an indeterminate size order.
- **The table stays device-local.** v1 has no export, share, import or file-picker surface for it,
  so the app never becomes a redistribution channel. See Non-Goals.
- **MODIFIED spec requirements.** Two existing requirements — *The app computes arithmetic, not
  code capacities* and *No capacity value is fabricated or silently reused* — as written forbid
  exactly the act this change legalises. They are restated to say which act is permitted (a
  user-recorded lookup under the documented rounding rules, shown to the user) and which remain
  forbidden (fabricating, interpolating, estimating, defaulting, and any app-owned or app-supplied
  code data). See *Capabilities*.

**BREAKING** for nothing shipped: no released artifact, listing claim or user data changes kind.
It is **claim-breaking** for the slot's own store copy and promise contract — see Impact.

## Capabilities

### New Capabilities

None. This change adds behaviour inside an existing capability.

### Modified Capabilities

- `fuel-gas-pipe-sizing`: three requirements change, because their current wording forbids the
  user-owned lookup this change introduces:
  - *The app computes arithmetic, not code capacities* — restated to permit returning a
    user-recorded row and to keep every app-supplied code value forbidden.
  - *No capacity value is fabricated or silently reused* — restated to draw the line between the
    permitted lookup and fabrication, interpolation, estimation, defaulting and reuse.
  - *Every result has user-recorded provenance* — restated so a returned value's provenance is the
    table's single citation plus the row used, with the literal user-recorded marker.

> **Structure note — read before applying.** `openspec/specs/fuel-gas-pipe-sizing/` **does not
> exist**. The base requirements these MODIFIED blocks target live only inside the still-active
> change `openspec/changes/app-pipe-sizing/specs/fuel-gas-pipe-sizing/spec.md`. `openspec validate`
> accepts this delta today (a MODIFIED block with no matching main spec validates clean), but
> `opsx-sync`/`archive` resolve MODIFIED by looking the requirement up in the main spec; with no
> main spec they take the *create-new-main-spec* path, which copies **ADDED requirements only** —
> so the three MODIFIED blocks would be dropped silently and the contradiction would survive into
> the spec that gets created. The base spec must be synced into `openspec/specs/` first:
> `/opsx-sync app-pipe-sizing` (or archive it), then re-check that the three requirement titles
> match exactly. See `README.md` and `design.md` §OpenSpec structure.

## Non-Goals

- **No distribution surface for the table in v1.** No export, share, send, import, file picker,
  copy-to-clipboard, or network path for the table or its rows. **Licence reason:** the moment the
  app can move table data off the device, it stops being the user's private reading and becomes a
  redistributable copy of code data, which is the exactly the right the memo says is not
  documented. A shareable *case report* may name the table's citation and the row used; it must not
  carry the table's rows.
- **No embedded, bundled, downloaded, or defaulted code data.** Unchanged from the current scope,
  and now also enforced against the new feature: no capacity, length, size, diameter, gravity,
  pressure-drop or size-ordering value may come from the app.
- **No interpolation, and no equation route.** The memo leaves the equation route's
  copyright status `UNVERIFIED`; it is not taken.
- **No compliance claim, and no new one.** The app still does not determine compliance; the
  reasoning is unchanged, and the `dropped` reasoning in `differentiators.json` for
  `returns-the-missing-values` still applies to the clause *"from the standard"*.
- **No per-row citation.** The user records one citation for the whole table, mirroring how a real
  table is headed.
- **No new purchase or ad surface.** The table feature is not a new thing to sell in this change;
  whether it belongs inside the existing one-time unlock is an open question for the owner
  (`design.md` §Open Questions).

## What this does NOT claim

- It does not claim the app supplies, knows, or verifies the code's capacity values.
- It does not claim the returned value is the code's value, or that a build made with it is
  compliant, approved or certified.
- It does not claim the user's transcription is correct. **Wrongly recorded, confidently returned
  is the risk this change accepts**, and every returned value must say so on the result view.
- It does not claim the rounding rules are the app's invention; it applies the rule the source
  procedure documents (*"the closest larger distance if the exact distance is not listed"*) and
  shows every substitution it makes.
- It does not change `differentiators.json`, `store/studio.json` or `store/journey.json` in this
  change. Their claims must move **with implemented code and real proofs**, not with a plan; the
  list of what is expected to move is in `design.md`.

## Impact

| Area | Effect |
|---|---|
| `app/src/main/java/.../sizing/` | New `CapacityTable.kt` (the entity) and `CapacityTableLookup.kt` (the two algorithms); `SizingDomain.kt` gains two methods, a provenance case, an evaluation variant and nine refusal kinds |
| `app/src/main/java/.../ui/tables/` | New `TablePersistence.kt` (DataStore repository + codec), `TablesUiState.kt`, `TablesViewModel.kt`, `TablesScreen.kt` |
| `app/src/main/java/.../ui/storage/` | New `LineCodec.kt` — the single copy of the record escaping that `SavedCaseCodec` and the table codec both import |
| `app/src/main/java/.../ui/home/`, `ui/PlayKilnApp.kt` | The new method's inputs and result lines; a fourth bottom-navigation destination for the table surface |
| `app/src/main/res/values/strings.xml` | New copy for the table surface, the new method, the substitution notice and the labels |
| Persistence | Reuses the existing `preferencesDataStore` + tolerant line-codec pattern; no new dependency, no server, no schema migration of existing stored cases |
| Tests | New JVM unit tests for the entity, the lookup, the refusals, the codec and the report; the existing suites must stay green |
| `store/studio.json`, `store/journey.json`, `store/listing.json` | **Untouched in this change.** Their recorded claim bounds say the app *"ships no code-table data, does not determine the pipe size itself"*, which this change makes partly false. Deferred to the release phase; the exact list is in `design.md` |
| `differentiators.json` | **Untouched in this change.** Expected to move `returns-the-missing-values` and `records-the-citation`, each needing an `ownerDecision` in the owner's own words. Note: `node scripts/check-differentiators.mjs --slot pipe-sizing` is **already `VERDICT: BLOCKED`** today, because three core claims lack `ownerDecision` — the slot cannot pass `verify-slot.mjs` until the owner records them |
| `economics.json` | Unchanged and still valid: archetype `ads_led_utility`, priceUsd 4.99, capabilities `ads` + `billing` enabled; this change enables no new capability and adds no new thing to sell |
