# The self-learning loop

This factory encodes rules it measured once: Play's field limits, the console's own inline sentences,
a vendor's guidance, a pinned toolchain version, and the lessons a defect taught. Every one of those
readings is true **as of a date**, and every one of them can stop being true without anything in this
repository changing. This document is the contract for noticing that, and the honest list of what is
still a person's job.

The loop is three questions, asked of two journals:

| Question | Asked by | Journal |
|---|---|---|
| Is every measured fact still inside its window, and does the program it names still exist? | `node scripts/check-freshness.mjs` | `docs/research/measured-facts.json` |
| Does every lesson still name a sensor that is on disk? | `node scripts/check-lessons.mjs` | `harness/lessons.md` |
| Does a hand-recorded console transcription still match the console model? | `python scripts/store-kit.py --observations` | `.kiln/console-observations/*.json` |

Each returns a repository exit code, so each can be wired into a gate. None of them fetches anything
from Google — see [What the loop cannot do](#what-the-loop-cannot-do).

## Why it exists

Three holes were measured in the loop before this contract existed, and every one of them was silent:

1. **Nothing connected a lesson to a check.** `harness/lessons.md` is the factory's memory: a dated
   entry per defect with `Symptom / Root cause / Fix class / Change made`. The rule it lives under —
   `.cursor/rules/verification-discipline.mdc`, "every repeated defect earns a regression check" — was
   itself only prose in the journal. An entry whose fix named no check read exactly like an entry that
   had closed the defect for good, and a check that was later deleted or renamed left the lesson that
   demanded it silently green.
2. **Nothing noticed Google Play changing.** The rules the factory follows live in
   `docs/research/measured-facts.json` as structured entries carrying a source, a `verifiedAt` date and
   an expiry window. A fact past its window was reported — but a fact said *what* the rule is and
   *when* it was read, not **which program enforces it**, so nothing could say which gate was now
   resting on a stale reading.
3. **There was no entry point.** The trigger for re-checking a rule was "an agent happened to notice".
   There was no command that produced the work order, so the work order was whatever somebody
   remembered.

## Inputs

### `docs/research/measured-facts.json` — what the factory believes about the world

One entry per fact. The shape is validated by `scripts/lib/freshness.mjs` before anything is judged,
and an entry that fails the shape is a broken record (exit 1) rather than a fact nobody checked.

```json
{
  "id": "play-icon-asset-spec",
  "claim": "<the rule, as measured>",
  "source": "<the URL or document to re-open>",
  "sourceKind": "play-console",
  "verifiedAt": "2026-09-29",
  "verifiedBy": "live-console",
  "expiresAfterDays": 180,
  "whereUsed": ["docs/research/2026-09-22-android-play-requirements.md"],
  "enforcedBy": { "script": "scripts/store-kit.py", "probe": "--validate-only" },
  "gap": "<present only when the enforcement covers part of the claim>"
}
```

| Field | Rule |
|---|---|
| `id` | lower-case kebab-case, unique; a stable handle a work order and a lesson can both name |
| `sourceKind` | one of `play-console`, `vendor-docs`, `eu-legislation`, `repo-measurement` |
| `verifiedBy` | one of `live-console`, `vendor-page`, `primary-legislation`, `measured-in-repo` — *how* to re-measure |
| `verifiedAt` | ISO date, never in the future (a future date is how a stale entry hides) |
| `expiresAfterDays` | a positive integer; **180** for a moving target (Play, a vendor's current guidance), **365** for a long-lived one (legislation, a toolchain pin). "Expires after N days" means the entry is still inside its window **on** day N and stale on day N+1 |
| `whereUsed` | the repository paths that depend on the fact; each must exist, because a blast radius that is gone is a record that has stopped being true about the repository |
| `enforcedBy` | **required**: `{ script, probe }` or `null` (see below) |
| `gap` | required when `enforcedBy` is `null`, allowed beside a real `enforcedBy` when the check covers only part of the claim |

`enforcedBy` is the field this loop was missing. It names the artifact that turns the fact into a
machine check — `script` is a repository-relative path, `probe` is the exact CLI argument or literal
the script carries, and `null` means the fact is enforced by a whole-file rule rather than a probe.
Two verdicts fall out of it:

- **A named program that is not on disk, or a named probe that is not in that program's source, is
  BROKEN (exit 1).** The fact claims a machine check that is gone, which is exactly the silent drift
  this field exists to catch.
- **`null` is UNENFORCED (exit 2), never a pass.** It is *the* honest answer for a rule the factory
  follows on prose alone, and it has to carry a `gap` in one sentence saying what is unenforced. An
  unenforced fact is the most valuable row in the ledger: it is a rule nothing can defend.

`null` is not a licence to dodge work. Inventing an `enforcedBy` that does not really enforce the
claim is worse than `null`, because it converts a visible hole into a green row.

### `harness/lessons.md` — what a defect cost, and what it left behind

Append-only and in chronological order. The template is `Symptom / Root cause / Fix class / Change
made`, plus a bullet the sensor contract added:

```
### YYYY-MM-DD - short title
- Symptom:
- Root cause:
- Fix class: (AGENTS.md | skill | sensor | rule | spec)
- Change made:
- Sensor: <what fails today if this defect comes back>
```

Three shapes are honest, and `scripts/lib/lessons.mjs` accepts exactly those:

1. `` - Sensor: `scripts/test-x.mjs` `` — one or more backticked repository paths that **exist**, at
   least one outside `.kiln/`. A sensor may also be a *command* naming the script it runs
   (`` `python scripts/store-kit.py --observations` ``): the file that must exist is the script.
2. `- Sensor measured before it was trusted: ...` — the form the 2026-09-29 entries already use,
   because it states the path **and** how it was shown to fail. The path is what the check reads; the
   prose beside it is what a reader reads.
3. `` - Sensor: none - <reason> `` — an explicit "no sensor is possible", with a reason of at least 40
   characters. A one-off console sequence or a UI observation legitimately leaves nothing to run, and
   saying so is a decision a reader can challenge. Silence is not. The bullet is read as a
   *declaration*: it has to open with `- Sensor: none`, because testing for the word `none` anywhere
   in the bullet was too loose to be a check — a long sensor bullet that quoted a FAIL row containing
   the word ("carries no screen with that id and none whose console label is …") was counted as an
   explicit "no sensor is possible", which silently turned a real 431-leg suite into a `none`.

A `.kiln/` path does not satisfy the contract on its own, because `.kiln/` is disposable local state
by design (`AGENTS.md`) and a sensor that lives only there disappears with the next cleanup. Citing a
throwaway injection harness beside a durable check is legitimate and is reported as a note.

The contract binds entries dated **2026-09-29 or later** (`SENSOR_CONTRACT_FROM`). The 60 earlier
entries are counted and their missing sensor lines printed; they are **not** rewritten, because the
journal's history is evidence rather than a style guide.

### `.kiln/console-observations/*.json` — what a human read in the live console

The Play Console is behind a login, so the only honest way to measure it is that somebody reads it and
writes down what it said. One file per screen, one schema (`OBSERVATION_SCHEMA_VERSION = 1`), each
carrying `schemaVersion`, `screenId`, `consoleNameRu`, `observedAt`, its citation and the reader's
notes. Two shapes exist, and the shape is decided by the finding rather than by the transcriber's
convenience:

- a screen that **was** found reports `actions`, `saveObserved` and `persisted`;
- a screen that **was not** found carries `foundOnAnyReachableScreen: false` and must name
  `searchedScreens` and `blockerRu` — where it looked and what could not be read. An honest negative
  is evidence only when it says where it searched.

`python scripts/store-kit.py --observations` validates each file's shape against
`scripts/lib/console-form.json` (the console model), refuses a paraphrase of a console label, refuses
a field id belonging to another screen, and then prints the model edits the file implies. It writes
nothing without `--apply-observations`, and only what the model itself declares foldable
(`observationFold`) — a silently applied observation is how a guide becomes a copy of one session.

This primitive is deliberately **not** re-implemented by the freshness gate. The freshness gate is
the trigger and the coverage check; this is the transcription mechanism.

### The support files the loop reads

| File | Role |
|---|---|
| `scripts/lib/freshness.mjs` | the ledger contract, the loader, and the per-fact audit — one source of truth; `check-freshness.mjs` is only the CLI and the exit-code policy |
| `scripts/check-freshness.mjs` | the freshness/coverage gate: exit 0 / 1 / 2 |
| `scripts/lib/lessons.mjs` | the sensor contract and the pure evaluator |
| `scripts/check-lessons.mjs` | the lessons gate: exit 0 / 1 / 2 |
| `scripts/lib/console-form.json` | the console model the observations are compared against |
| `scripts/store-kit.py` | owns the observations validator (`--observations`) |

## Outputs

Every run produces a **work order**, not a score.

| Output | Exit | What it means | What to do |
|---|---|---|---|
| `FAIL` / `BROKEN` | 1 | the record is unreadable, or a named enforcing script or probe is gone | fix the record, or restore/rename the check |
| `STALE` | 2 | a fact is past its window | re-open its `source`, re-measure, then update `verifiedAt` and every `whereUsed` path |
| `UNENFORCED` | 2 | a fact names no enforcing program | write the check the rule is only prose about, then set `enforcedBy` |
| `NOTE` (partial) | — | a real sensor covers only part of the claim | read it; a claim larger than its check is how a partial sensor becomes a false one |
| `FRESH` / `PASS` | 0 | every fact inside its window and enforced; every bound lesson names a sensor that exists | nothing |

The stale block prints the claim, the source, the `verifiedBy` method and the `whereUsed` paths, so
the output *is* the work order: "re-open this page, then update these files". The unenforced block
prints the fact and its `gap`, so the missing check is a work order too.

An empty ledger is broken (exit 1) on purpose: "every entry is fresh" is vacuously true for zero
entries, so deleting the facts would otherwise be the cheapest way to pass. For the same reason a
`whereUsed` path that no longer exists is a record defect, not a note.

## Cadence

| When | Run | Why then |
|---|---|---|
| Before a slot's UI exists (stage 0.5/0.6) | `node scripts/check-freshness.mjs` | a plan built on a stale rule is a plan built on a rule Play may no longer have |
| At each `/ship` and `/release` boundary | `node scripts/check-freshness.mjs` and `node scripts/check-lessons.mjs` | both are cheap, both are source-only, neither needs a device or a build |
| After every defect a slot cost you | `node scripts/check-lessons.mjs` | the entry is being written anyway; the gate is what stops it shipping without a sensor |
| Whenever the console was read for a release | `python scripts/store-kit.py --observations` | the transcription is only evidence after it has been checked against the model |
| Monthly, or whenever a fact's window elapses | the stale block above | the window is the schedule; the fact's own `expiresAfterDays` decides when it comes due |

The windows are the clock, and they are deliberately two: **180 days** for a moving target and
**365 days** for a long-lived one. `scripts/test-freshness.mjs` asserts that every window in the
repository ledger is one of those two, so a fact cannot quietly buy itself a decade.

## What the loop cannot do

Written down because a watchdog that overstates itself is worse than no watchdog.

- **It cannot browse Google Play by itself, and it does not try.** Nothing here fetches a page. The
  re-measure step — open the `source`, read it, decide whether the rule moved — is a human or an agent
  command, and it is the part of the loop that is **discipline, not automation**. What is automated is
  noticing *that* the reading is due and *which* files depend on it.
- **It detects an expired reading, not a changed world.** A fact inside its window is not a fact that
  is still true; it is a fact nobody has disproved. Google changing a page on a Tuesday moves nothing
  here until the window elapses or somebody reads it.
- **`probe` is a literal-string search, not a semantic check.** A probe that appears in the named
  script satisfies the gate, even in a comment. It proves the probe was not renamed or deleted; it
  does not prove it still checks what the fact claims.
- **`whereUsed` is asserted by existence, not by dependence.** The gate proves the paths a fact names
  are on disk. It cannot prove the fact is still *used* there.
- **Some facts have no forcing program at all, and that is a finding rather than a bug.** As of
  2026-09-29 one of the eighteen facts is unenforced, and one more is only partly enforced; both are
  listed under [The current gaps](#the-current-gaps).
- **A false `persisted: true` in a console transcription is not caught.** The validator refuses
  `persisted` on a screen the console never confirmed, and it compares the reading's `observedAt`
  against the model's provenance — but the transcription itself is a human statement about a page a
  program cannot see.
- **The lessons contract is retroactive only in the forward direction.** 60 entries predate
  2026-09-29 and are counted, not checked.
- **Nothing runs on a timer.** The cadence above is a rule for whoever runs the next command; no
  scheduler or hook invokes these gates on a schedule. What is wired is the gate:
  `scripts/verify-slot.mjs` calls `scripts/check-freshness.mjs` on every verification run and turns
  exit 2 into `UNVERIFIED` — never PASS, and deliberately not FAIL, because a Play requirement
  changing is not a code defect and blocking a slot on it would teach people to silence the check.
  `scripts/check-lessons.mjs` is not wired into it yet, so it runs when an operator (or an agent
  following the cadence) runs it.

## The current gaps

This is a reading of `docs/research/measured-facts.json` on 2026-09-29, and it is the honest headline
of the whole loop: a rule the factory follows with nothing to defend it.

| Fact | State | What is unenforced |
|---|---|---|
| `play-auto-string-translation-overrides` | `enforcedBy: null` | nothing asserts the factory position. Declining Play Console automatic string translation for a language a slot already translates lives only in `AGENTS.md` and `docs/research/2026-09-22-android-play-requirements.md` prose, so nothing fails if a slot enables the feature and lets Console machine output override a git-tracked `values-<locale>/` translation. A check would have to read the console state — which the repository does not hold — or be a review note, so it stays an honest `null` rather than an invented script. |
| `factory-toolchain-pin` | partial (`gap`) | `scripts/kiln-status.mjs` probes the Gradle wrapper 9.7.1, `compileSdk` 37, `targetSdk` 36 and Build Tools 36.0.0, and the JDK 17 floor is `MIN_MAJOR_SUPPORTED` in `scripts/lib/java.mjs` (asserted by `scripts/selftest.mjs`). The **AGP 9.4.1, Kotlin/Compose Compiler 2.3.21 and Compose BOM 2026.09.00** pins are asserted by no program. |

Adding a check for either is a work item, not a documentation fix. Until then the gate reports both
on every run, at exit 2, so neither can be mistaken for green.

## Closing the loop — the operator commands

```bash
# 1. Is any measured fact due? The output is the work order.
node scripts/check-freshness.mjs

# ...the same, machine-readable (counts, gap ids, exit code, every entry's enforcement row)
node scripts/check-freshness.mjs --json

# ...for a loop that must not block: prints the problem, exits 0. It NEVER excuses a broken record.
node scripts/check-freshness.mjs --warn-only

# 2. Re-measure what it named: open the entry's `source`, confirm or correct the claim,
#    then update `verifiedAt`, `expiresAfterDays` and every `whereUsed` path in
#    docs/research/measured-facts.json. If the rule moved, fix the code that encodes it -
#    and if the check itself moved, update `enforcedBy.script` / `enforcedBy.probe` too.

# 3. Does every lesson still name a sensor that exists?
node scripts/check-lessons.mjs
node scripts/check-lessons.mjs --since 2026-09-29     # move the cut-off; older entries are counted only
node scripts/check-lessons.mjs --json

# 4. Read the console, write one file per screen under .kiln/console-observations/, then check it.
python scripts/store-kit.py --observations            # validate + print the model edits it implies
python scripts/store-kit.py --observations --apply-observations   # write the foldable ones back

# 5. Prove the gates can still fail (a check that cannot fail is not a check).
node scripts/test-freshness.mjs            # 102 offline checks against synthetic ledgers
node scripts/test-freshness.mjs --prove    # + 7 mutation proofs: each mutant is shown to be caught
```

### Automated today, and what is not

| Part of the loop | Automated | Discipline |
|---|---|---|
| Noticing a fact is due | yes — `check-freshness.mjs` computes the age and prints the work order | |
| Noticing a named check is gone | yes — exit 1 names the missing script or probe | |
| Noticing a rule has no check at all | yes — exit 2 names the fact and its `gap` | |
| Noticing a lesson's sensor was deleted | yes — `check-lessons.mjs` names the lesson and the missing path | |
| Validating a console transcription against the model | yes — `store-kit.py --observations` | |
| **Fetching the source** | | **discipline** — a person or an agent opens the page; nothing here does |
| **Deciding the rule moved** | | **discipline** — "still true" versus "changed" is a judgement |
| **Writing the check a `gap` names** | | **discipline** — the gate names the hole; someone has to write the sensor |
| **Running the loop at all** | partly — `scripts/verify-slot.mjs` calls the freshness gate on every verification run and turns its exit 2 into `UNVERIFIED` | **discipline** — nothing schedules either gate, and the lessons gate is not wired into `verify-slot.mjs` at all, so it runs when somebody runs it |

## Proof that the gates can fail

A check that cannot fail is not a check, so every rule this contract added was shown failing before it
was trusted, and then restored. Everything quoted below is the observed output of a real run — not a
description of one — and each injection was made on a copy or reverted immediately, so no gate was
ever left red in the repository.

**A fact whose `enforcedBy.script` is not on disk** — one entry's `script` was pointed at
`scripts/no-such-enforcing-program.py` in a copy of the ledger, and the gate was run against that copy
(`--ledger`) so the repository ledger was never left broken:

```
=== measured-facts freshness — ledger .kiln/freshness-demo-missing-script.json ===
  FAIL        ENFORCE-SCRIPT-MISSING: play-icon-asset-spec names enforcing script scripts/no-such-enforcing-program.py, which does not exist — the fact claims a machine check that is gone

---
VERDICT: BROKEN — 1 problem(s); the record must be readable, and the program it names must exist, before any fact can be trusted.
```

exit 1.

**A fact whose `probe` is absent from the named script's source** — the same entry's `probe` was set
to `--no-such-probe-xyz`:

```
=== measured-facts freshness — ledger .kiln/freshness-demo-missing-probe.json ===
  FAIL        ENFORCE-PROBE-ABSENT: play-icon-asset-spec names probe "--no-such-probe-xyz" in scripts/store-kit.py, and that literal is not in the file — the probe was renamed or removed

---
VERDICT: BROKEN — 1 problem(s); the record must be readable, and the program it names must exist, before any fact can be trusted.
```

exit 1.

**A lesson whose named sensor is not on disk** — one 2026-09-29 entry's `Sensor` bullet was pointed at
`scripts/no-such-sensor.mjs`:

```
  FAIL        "2026-09-29 - the console notice a human has to read sat on no screen, so both documents were silent about it" names a sensor that is not there: scripts/no-such-sensor.mjs

21 entries bound by the sensor contract (from 2026-09-29); 20 declare one (1 as `none`, 0 in prose without a file name).
60 earlier entries grandfathered, 60 of them without a sensor line.

VERDICT: FAIL
```

exit 1 — and restored, `21 declare one (1 as none, 0 in prose without a file name)`, `VERDICT: PASS`,
exit 0. (At that moment one entry was still being counted as `none`; the tightening described below is
what turned that count into the honest `0 as none`.)

**A `Sensor: none` claim with no reason** — the same entry's bullet was written as
`` - Sensor: none - nothing to run ``:

```
  FAIL        "2026-09-29 - the console notice a human has to read sat on no screen, so both documents were silent about it" declares `Sensor: none` without a reason: a none-claim has to say why nothing can be run, in 40+ characters, or name the check it drives in backticks

21 entries bound by the sensor contract (from 2026-09-29); 20 declare one (0 as `none`, 0 in prose without a file name).
```

exit 1; and the same bullet with a sentence after it — `` - Sensor: none - the reading was a single
live console session that no script reproduces, so there is nothing here to run `` — is accepted,
`21 declare one (1 as none, ...)`, `VERDICT: PASS`, exit 0. Both directions were checked because
tightening the `none` shape without a positive control is how a rule becomes impossible to satisfy.

Tightening that shape also caught a real false green: one entry's `Sensor` bullet quoted a FAIL row
containing the word "none", so it was counted as an explicit "no sensor is possible" while the entry
in fact had a 431-leg suite (`scripts/test-console-answers.py`). The bullet now names that suite, and
the same bullet would fail today if the suite were deleted.

`scripts/test-freshness.mjs --prove` performs the same kind of check automatically and permanently.
Each of its seven mutants — `stale-is-ignored`, `exit-code-pinned-to-zero`, `duplicate-ids-allowed`,
`blast-radius-not-checked`, `missing-enforcing-script-allowed`, `probe-not-checked` and
`unenforced-counted-as-a-pass` — is re-run against a deliberately broken copy of the library, and the
proof fails if the mutant is *not* caught:

```
PASS  missing-enforcing-script-allowed: the real script produces the expected observation (1)
PASS  missing-enforcing-script-allowed: the mutant produces the wrong observation (0), so T8 "a named enforcing program that is gone exits 1" can fail
```

A rule that only has a hand-recorded demonstration is a rule that can drift back; the mutation proofs
are the ones that cannot.

