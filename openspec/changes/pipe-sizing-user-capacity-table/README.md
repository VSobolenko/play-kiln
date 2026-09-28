# pipe-sizing-user-capacity-table

Let the user record their own capacity table once — its own declared applicability, one citation
for the whole table, and rows of nominal size, inside diameter, length and capacity — and have the
`pipe-sizing` slot return values from it: the recorded capacity for a size and a length (naming the
row used), and the smallest entered size that carries a required load at a total length. The app
applies the code's documented rounding rules **upward only** and shows every substitution; it never
interpolates, estimates, defaults or carries a value from another row, never applies a table
outside its declared applicability, and never claims a returned value is the code's value or that
the app transcribed or verified it. The table stays on the device with no export, share or import
surface, so the app is not a channel for redistributing code data.

The whole reason this route is legal is that the app does not reproduce or distribute the code's
data: the user keys their own reading into their own device. `docs/research/2026-09-25-pipe-sizing-code-source.md`
§5 records it as the low-distribution-risk route and its decision table forbids embedding
(*"No embedded numbers until a right is documented"*); it also records counsel's confirmation as
`UNVERIFIED`, which this change does not pretend to have obtained.

## OpenSpec structure note — the base spec must be synced first

**`openspec/specs/fuel-gas-pipe-sizing/` does not exist.** The requirements this change's
`## MODIFIED Requirements` blocks target live only inside the still-active change
`openspec/changes/app-pipe-sizing/specs/fuel-gas-pipe-sizing/spec.md`. Two consequences, both
verified against `openspec` 1.10.0 on this machine rather than assumed:

1. `openspec validate pipe-sizing-user-capacity-table` **passes today**. That is not approval:
   `validate` does not check a MODIFIED block against a main spec.
2. `opsx-sync` / `archive` **do** resolve MODIFIED by looking the requirement up in the main spec.
   With no main spec, the sync takes its *create-new-main-spec* path, which copies **`ADDED`
   requirements only** — so the three MODIFIED blocks would be dropped silently and the
   contradiction they exist to close would survive into the spec that gets created.

Therefore, **before this change is applied or archived**, sync the base delta into the main specs:

```bash
opsx-sync app-pipe-sizing          # or: openspec archive app-pipe-sizing
# then re-read openspec/specs/fuel-gas-pipe-sizing/spec.md and confirm these three titles
# are present verbatim, because a MODIFIED header must match the main spec exactly:
#   The app computes arithmetic, not code capacities
#   No capacity value is fabricated or silently reused
#   Every result has user-recorded provenance
```

If the orchestrator prefers not to sync, the alternative is to author this delta's superseding text
as `## ADDED Requirements` under new requirement titles and remove the three MODIFIED blocks — but
then the two forbidden-as-written requirements stay in the base spec and an implementer has to
interpret the contradiction, which is the outcome this change was asked to prevent.

Two pre-existing OpenSpec conditions found while checking the above, neither caused by this change
and neither fixed here: `openspec validate --specs` already fails `android-compose-baseline` and
`play-compliance` (and `capabilities` carries 0 requirements), and `openspec/config.yaml` does not
parse (`Implicit keys need to be on a single line at line 13, column 7`), so the artifact rules it
declares are silently ignored by the CLI.

## Documents

| File | Contents |
|---|---|
| `proposal.md` | Why, the licence basis for the user-owned route, what changes, non-goals, what this does not claim, impact |
| `design.md` | The table entity, the two lookup algorithms with the rounding rules and their direction, the refusal taxonomy, persistence reuse, the UI shape, open questions, deferred store/release work, and the `differentiators.json` claims this change is expected to move |
| `tasks.md` | Ordered, checkable work items with disjoint file lists, the two-worker parallelism plan, and the no-embedded-data and unit-test requirements |
| `specs/fuel-gas-pipe-sizing/spec.md` | The delta: three `MODIFIED` requirements plus ten `ADDED` requirements, each with falsifiable scenarios |
