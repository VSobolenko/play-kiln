# Design: reduce-factory-to-core

## Context

See `proposal.md` — Why, for the measured baseline (88 files / 47,358 lines; 22 suites / 10,641;
2 meta-runners / 5,181) and for the two independent defects. Read
`requirements-preservation.md` before any deletion: it enumerates every rule the harness encodes
today, with its values, its source, and its required disposition after this change.

Constraints that shape the approach:

- The four existing slots under `apps/` must keep building. No slot is rebuilt by this change, but
  the reduced core must be able to verify each of them, because that is how the reduction is proven.
- The owner's release approval, the two first-publication declarations and the console graphics
  upload are the only human steps that may remain. Everything else is automatable and therefore must
  not be presented as human work.
- The repository's own rules — *a check that cannot fail is not a check*; *one source of truth,
  imported*; *never claim a script works until it ran* — apply to the refactor itself, so the
  refactor's result is measured, not asserted.
- `openspec/config.yaml` is invalid YAML today and is silently ignored by the CLI. Whatever is built
  here must not repeat that shape: a contract that no program reads and no person checks is already
  broken.

## Goals / Non-Goals

**Goals**

- One **program core**: deterministic facts only, one command per fact, one entry point that runs
  them all, a measured size budget, and no runner that runs another runner's suite.
- One **place per judgement rule**: a role prompt (or a checklist) that a human can edit without a
  code change and without a test suite to re-derive.
- **Provable losslessness**: every row of `requirements-preservation.md` has a named successor that
  exists and runs before the source is deleted.
- A **measured result**: the same walk that produced the baseline table produces the after table, and
  the one verify command's wall clock is recorded before and after.

**Non-Goals**

- Not a rewrite of the good fact-checkers. `lib/play-assets.mjs`, `lib/client-secrets.mjs`,
  `lib/apk-provenance.mjs`, `lib/icon_qa.py`, `check-16k.py`, `lib/java.mjs`, `lib/gradle.mjs`,
  `lib/namespaces.mjs` are already one-fact modules with a real failure mode. They are kept, and
  where possible they are *the* implementation, not re-implemented.
- Not a change to Play policy, to any numeric window, or to any slot's product behaviour.
- Not the removal of the two-phase approval boundary, of the registry, or of the kill decision.
- Not an attempt to make judgement measurable. The trade is deliberately accepted: a judgement rule
  becomes a prompt plus a human checkpoint, and the loss of enforcement is bought back by the
  owner's two checkpoints and the verifier role.

## Decisions

### D1. Split the harness by *measurability*, not by feature area

The current layout groups by area (`store-kit.py` renders, validates, writes console sheets and
parses console observations), which is why a change to one rule touches a 6,192-line file. The
target layout groups by the question *can a machine settle this without reading prose?*

- `scripts/core/` — fact checks. Pure functions plus a thin CLI. Importable, testable with inline
  fixtures, no subprocesses.
- `scripts/lib/` — the single sources of truth the checks read (namespaces, java home, gradle
  route, play asset constants, secret patterns, approval digests).
- `scripts/store/` — the store pack: one writer, one validator, one shared model.
- `harness/roles/` — the judgement rules, one file per role.
- `docs/` — the checklists the roles and the owner read.

*Alternative rejected:* keep the file structure and only delete code. It leaves the 6,192-line
module intact, and the module is the concrete thing that makes a rule change expensive.

### D2. One verify entry point, and no gate runs another gate's suite

`scripts/verify.mjs` imports every fact check in `scripts/core/` and runs them in one process, in
order, printing one row per fact with a real verdict (`PASS` / `FAIL` / `UNVERIFIED` / `SKIP`) and a
non-zero exit on any `FAIL`. It spawns exactly one subprocess: Gradle, when `--build` is passed.

- A gate is a function with a name, a source-of-truth reference, and an incident. Not a script that
  shells out to another script that shells out to a suite.
- `--build` compiles the slot once, through `lib/gradle.mjs`, with the pinned JDK.
- `UNVERIFIED` (exit 2) is a first-class verdict, and it is never printed as `PASS` — an unrun check
  and a passing check must not look alike.

*Why:* the measured defect is a chain of runners. `verify-slot.mjs` runs `selftest.mjs`, which
discovers and runs 22 suites, some of which assert that other scripts call `runGradleTasks`. The
chain is what makes a one-line fix cost an hour and what makes a green run unreadable.

*Alternative rejected:* keep the chain but make it faster. It cannot be made readable — the reader
still cannot tell which of 1,000 assertions covers the thing they just changed.

### D3. Judgement lives in one prompt file per role, and the rule is stated exactly once

Roles: **scout** (demand), **money owner** (plan, price, KPI, kill), **builder** (the app, the pack,
the store copy), **verifier** (independent reproduction from the artifact on disk), **critic**
(continuous adversary against the most recent landing).

- The canonical text lives in `harness/roles/<role>.md`. `.cursor/agents/*.md` and
  `.codex/agents/*.md` become thin adapters that point at the canonical file, because two copies of a
  judgement rule is the drift this change removes.
- Changing a judgement rule is a one-file edit with no test suite to update. That is the property
  the owner asked for in his own words: *"if it were subagents, they'd do it in their head and we'd
  fix one prompt."*
- A role prompt that states a rule must state the **falsifiable consequence** with it: which
  observation flips the verdict, and the exact command that produces it. A verdict no evidence could
  contradict is not a verdict.

*Alternative rejected:* encode judgement as a "soft" gate that warns. A warning nobody can fail is
the same as no check, but it costs code and implies coverage that does not exist.

### D4. The economics gate shrinks to the facts; the plan becomes an owner-approved document

What stays machine-checked (each is a fact about a file): the archetype is one of the five;
`capabilities.json` enables the capability the archetype needs; the kill criteria's shape
(`windowDays` 7..180, `minInstalls` ≥ 1, `minRevenueUsd` ≥ 1 — a threshold nothing can miss is not a
criterion); the plan file exists and carries the owner's approval.

What leaves the machine: whether the demand evidence is real, whether the price is right, whether the
`evidenceRef` prose labels its claims honestly. Those become the money owner's job and a row the
owner reads at checkpoint 1.

*Why:* the failure the gate caught — a purchase-led plan on a slot with no `billing` capability — is
the fact part, and it is kept. The `evidenceRef` file-existence check was never the thing that
caught a bad market; it only caught a missing filename.

### D5. `store-kit.py` becomes three programs and one model

| New | Responsibility | Constraint |
|---|---|---|
| `scripts/store/model.py` | the pack's data model and every numeric limit, read from `lib/play-assets` values | one definition; no duplicate constants |
| `scripts/store/render.py` | writes the pack: listing, documents, icon/feature assets, compositions | requires the recorded approval; `--skip-provenance` reports `UNVERIFIED` (exit 2) |
| `scripts/store/validate.py` | reads the pack and the provenance record; prints the pack verdict | never writes; the only thing `verify.mjs` calls |

*Why:* a program that writes a pack and grades it is grading itself. The 6,192-line file also
carried the console-sheet generator and the console-observation parser, both of which are removed
(see §14.7 and §14 of the ledger).

*Alternative rejected:* split by output type (icons / screenshots / text). It keeps the
write-and-grade overlap for every kind.

### D6. The console knowledge becomes a checklist; the transcript is archived

- The submission ladder, the ten declarations, the field caps and the page paths become
  `docs/play-console-checklist.md` — read by the builder at phase 2 and by the owner at checkpoint 2.
- `scripts/lib/console-form.json` (3,164 lines of live console transcript) is **moved, not deleted**,
  to `docs/play-console-observations.json`, marked at the top as *an archived reading; no program
  reads this file*. Re-measuring that ladder costs a live console session, so the transcript is
  worth keeping and is not worth parsing.
- `scripts/console-walkthrough.mjs`, `scripts/console-walkthrough` drift check and
  `scripts/test-console-answers.py` are deleted with the sheet generator.

*Why:* the sheet was a copy of what the pack already said, kept in sync by a 4,158-line test. The
owner answered that upload by hand once and the answers are in the transcript; a checklist preserves
the knowledge, and the generator preserved a document nobody has needed twice.

### D7. The two ledgers stay readable and stop being gates

`harness/lessons.md` and `docs/research/measured-facts.json` stay. `check-lessons.mjs` is removed
(it measured that a filename exists — not that a check works, which is a different claim), and
`check-freshness.mjs` is removed (a date only a human can re-measure, printed as a verdict, is a
false signal).

The requirement behind them — *a check that exists and is never run cannot be told apart from a check
that does not exist* — is kept in a stronger form by D2: every check that exists is a function the
one verify command calls, and a fact file's `enforcedBy` field names a function in `scripts/core/`
that `verify.mjs` runs. A fact naming a function that does not exist fails the verify run; a fact
naming no function is reported as `UNENFORCED`, as it is today.

### D8. The registry gains a lifecycle

`registry/apps.json` gains `published` (a release exists on Play) and `earning` (a dated reading
exists), keeps `scaffolded` and `killed`, and the kill decision binds to a state rather than to a
directory's existence. This is what makes "the factory's product is revenue" representable at all —
today no state means shipped and no state means earned.

### D9. Losslessness is proven by a golden-slot equivalence run

Before any deletion, the current harness is run against the reference slot
(`apps/2026-09-26-teacher-gradebook`) and its output is captured as the **golden run**. After each
migration stage, the reduced core is run against the same slot, on the same revision, and the two
verdict sets are compared: every `FAIL` in the golden run must still be a `FAIL` in the new run, and
every new `FAIL` must be explained by a check that no longer exists and whose ledger row says so.

*Why:* the alternative — trusting that the port preserved the checks — is exactly the failure
`.cursor/rules/verification-discipline.mdc` was written against.

### D10. Freeze, add, then delete — never delete first

Stages are ordered so that **nothing is deleted until its successor exists and has been run on the
golden slot.** The deletion stage is last and is a separate, reviewable diff per file group.

### D11. Budgets are measured, not intended

| Budget | Value | Measured by |
|---|---|---|
| Program core, total | ≤ **9,000** lines of `scripts/**` | the walk from `proposal.md` |
| Test surface inside the core | ≤ **25 %** of the core | the walk, filtering `test-*` and `*_test` |
| Subprocess spawns in one verify run | **1** (Gradle, only with `--build`) | `verify.mjs` prints its spawns |
| Runners that run other runners | **0** | `verify.mjs` imports functions, never scripts |
| Wall clock, `verify.mjs --slot <slug>` without `--build` | recorded before and after; the after value is the target | `elapsedMs`, printed |

*Why a budget and not a rule:* the owner's problem was not that a specific file was too long; it was
that the growth had no ceiling and nobody measured it. A number that a command prints is the
ceiling.

## Risks / Trade-offs

- **[A deleted gate catches something months later]** → The ledger's §18 forbids deleting before the
  successor runs; the golden-slot run (D9) is the mechanical check; and the fact checks that stay are
  pure functions with inline fixtures, so their sensors survive the removal of the suites that
  happened to exercise them.
- **[A judgement rule now has no enforcement]** → Accepted deliberately. Bought back by: the rule
  living in exactly one prompt, the verifier role reproducing claims from artifacts, and the owner's
  two checkpoints. The alternative — a regex over prose — does not enforce the rule either; it
  enforces a proxy, and it produces `PASS` rows that read like coverage.
- **[The prompt becomes the new bloat]** → Bounded by the same discipline as code: one file per
  role, one statement per rule, no rule stated twice, and the checklists in `docs/` are read by a
  named role instead of being written for nobody. The size budget in D11 covers `scripts/**`; a
  separate, explicit line count for `harness/roles/**` is recorded with it.
- **[`openspec/config.yaml` breaks again silently]** → A `verify.mjs` fact check parses the file and
  fails when it does not parse, and the repair quotes every scalar containing `: `. The general
  lesson is recorded: a config file is code, and the only thing that keeps it honest is something
  that reads it.
- **[Deleting `test-console-answers.py` loses the console model]** → The transcript is archived
  (D6), the checklist carries the ladder and the caps, and the task list requires the checklist to be
  reviewed against the transcript line by line before the JSON moves.
- **[The equivalence run cannot see a check that *should* have fired]** → Stated plainly: D9 proves
  that the port did not *lose* behaviour on the reference slot; it cannot prove a check that never
  fired on that slot still works. Those checks are covered by their own inline fixtures, which are
  required to fail against the pre-port behaviour for at least one case each.
- **[The owner's time is consumed by the refactor instead of by a slot]** → The stage order is
  chosen so the factory is usable from the end of stage 3 onward; the remaining stages remove code
  and cannot leave the factory in a worse state than the golden run.

## Migration Plan

Stage order is a contract, not a suggestion. Each stage ends with the golden-slot run (D9) and is an
independently revertible commit. Details and verification commands are in `tasks.md`.

1. **Freeze and measure.** Record the golden run, the baseline walk and the verify wall clock on the
   reference slot. Repair `openspec/config.yaml` and add the parse check, because every later stage
   is planned by an agent reading this repository's own rules.
2. **Build the core's skeleton and the single entry point.** No deletions. `verify.mjs` runs a small
   number of newly written fact checks and the existing fact-checker modules, imported rather than
   spawned.
3. **Move the judgement rules into `harness/roles/` and the checklists into `docs/`.** Still no
   deletions. The factory is fully usable from here.
4. **Split the store pack** into model / render / validate, and switch `verify.mjs` to the new
   validator.
5. **Move the ledger items that are pure data** (the console transcript, the release-notes format,
   the studio schema) to their new homes, verifying byte-for-byte equality where the item is a copy.
6. **Delete, in file groups, each group only after `requirements-preservation.md` shows no remaining
   `CORE`/`DATA` row pointing at it.** The meta-runner chain first, then the 22 suites except the
   sensors that moved into the core, then the console sheet generator and its test, then
   `store-kit.py`'s remaining body, then the two ledger gates.
7. **Re-measure and record.** The after-walk, the after wall clock, and the golden-run comparison
   are written into the change's own record before the change is archived.

**Rollback.** Stages 1–6 are separate commits; the pre-change state is tagged before stage 2.
Rolling back a stage means reverting that commit, and the golden run is re-executed to confirm the
reverted state still passes it. Because deletion is last and file-grouped, the expensive-to-undo part
is always the smallest commit.

## Open Questions

None that would change the specs or the task breakdown. Two items are deliberately deferred and
recorded in the ledger rather than decided here: the exact prose of the role prompts (authored during
stage 3, reviewed by the owner, not specified in this change) and whether a future slot needs the
console-answers sheet regenerated (the transcript and the model are archived, so it can be rebuilt
from them).
