# The self-learning loop

This factory encodes rules it measured once: Play's field limits, the console's own inline sentences,
a vendor's guidance, a pinned toolchain version, and the lessons a defect taught. Every one of those
readings is true **as of a date**, and every one of them can stop being true without anything in this
repository changing. This document is the contract for noticing that, and the honest list of what is
still a person's job.

The loop is two questions, asked of two journals:

| Question | Asked by | Journal |
|---|---|---|
| Is every measured fact still inside its window, and does the program it names still exist? | `node scripts/verify.mjs` — the `facts.ledger` row, implemented by `scripts/core/facts.mjs` | `docs/research/measured-facts.json` |
| Does a hand-recorded console transcription still match the console model? | `python scripts/store-kit.py --observations` | `.kiln/console-observations/*.json` |

`scripts/core/facts.mjs` returns one verdict per question the core can decide
(`facts.ledger`, `facts.fixtures`), so it is read as part of the one verification command rather than
as a command of its own. Neither journal is fetched from Google — see
[What the loop cannot do](#what-the-loop-cannot-do).

**The lesson journal is read, not gated.** `harness/lessons.md` stays as the factory's memory of what
a defect cost — an entry per defect with `Symptom / Root cause / Fix class / Change made` and the
sensor that stops it recurring. The gate that used to check it (`check-lessons.mjs` over
`lib/lessons.mjs`) was removed on 2026-09-30 with the rest of the meta-runner layer, and the reason is
recorded in `requirements-preservation.md` row 17.2: it checked that a *filename* exists, which is not
that a check works, so a passing run said nothing a reader could not read for themselves. The journal
is now a `CHECKLIST` item — the builder reads it and cites the sensor — instead of a verdict.

## Why it exists

Three holes were measured in the loop before this contract existed, and every one of them was silent:

1. **Nothing connected a lesson to a check.** The rule `harness/lessons.md` lives under —
   `.cursor/rules/verification-discipline.mdc`, "every repeated defect earns a regression check" — was
   itself only prose in the journal. An entry whose fix named no check read exactly like an entry that
   had closed the defect for good. That is now a reading habit rather than a program, deliberately and
   with the reason stated above.
2. **Nothing noticed Google Play changing.** The rules the factory follows live in
   `docs/research/measured-facts.json` as structured entries carrying a source, a `verifiedAt` date and
   an expiry window. A fact past its window was reported — but a fact said *what* the rule is and
   *when* it was read, not **which program enforces it**, so nothing could say which gate was now
   resting on a stale reading. `enforcedBy` is that field, and it is checked on every run.
3. **There was no entry point.** The trigger for re-checking a rule was "an agent happened to notice".
   There was no command that produced the work order, so the work order was whatever somebody
   remembered. The entry point is now the one verification command, which prints the ledger's state on
   every run instead of waiting to be asked.

## Inputs

### `docs/research/measured-facts.json` — what the factory believes about the world

One entry per fact. The shape is read and judged by `scripts/core/facts.mjs`
(`checkFactLedger` → `auditFactLedger`), and an entry that does not describe a window is a broken
record (`FAIL`) rather than a fact nobody checked.

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
| `whereUsed` | the repository paths that depend on the fact; each one that is gone is reported as a note, because a blast radius that has moved is a record worth re-reading |
| `enforcedBy` | **required**: a core check id, `{ script, probe }`, or `null` (see below) |
| `gap` | required when `enforcedBy` is `null`, allowed beside a real `enforcedBy` when the check covers only part of the claim |

`enforcedBy` is the field the loop was missing. It takes two forms, and the run's detail counts them
separately so the migration between them is visible rather than implied:

- **a core check id** — `"enforcedBy": "assets"`. The id must be one the registry in
  `scripts/core/index.mjs` declares, and it may only be declared there: the reader parses that file
  rather than importing `CHECK_IDS` from it, because the day the registry imports this module the
  import would close a cycle and hand back a half-initialised module — a landmine that appears only
  in the configuration that matters. Naming an id nobody declares, or a check the one verify command
  does not run, is a `FAIL` naming the fact.
- **a legacy pair** — `{ "script": "scripts/store-kit.py", "probe": "--validate-only" }`. The script
  must exist on disk and, when `probe` is a string, that literal must be in the script's own source.
  This is the shape the deleted gate carried, and the facts whose enforcement is a surviving script
  rather than a core check still use it.

Two verdicts fall out of it:

- **A named program that is not on disk, or a named probe that is not in that program's source, is a
  failure.** The fact claims a machine check that is gone, which is exactly the silent drift this
  field exists to catch.
- **`null` is `UNENFORCED`, never a pass.** It is *the* honest answer for a rule the factory follows
  on prose alone, and it has to carry a `gap` in one sentence saying what is unenforced. An unenforced
  fact is the most valuable row in the ledger: it is a rule nothing can defend.

`null` is not a licence to dodge work. Inventing an `enforcedBy` that does not really enforce the
claim is worse than `null`, because it converts a visible hole into a green row.

### `harness/lessons.md` — what a defect cost, and what it left behind

Append-only and in chronological order, with no gate in front of it any more. The template is
`Symptom / Root cause / Fix class / Change made`, plus the sensor bullet:

```
### YYYY-MM-DD - short title
- Symptom:
- Root cause:
- Fix class: (AGENTS.md | skill | sensor | rule | spec)
- Change made:
- Sensor: <what fails today if this defect comes back>
```

Three shapes are honest, and they are still the three the writer should choose between — they are a
writing convention now, not an input to a program:

1. `` - Sensor: `scripts/test-x.mjs` `` — one or more backticked repository paths that **exist**, at
   least one outside `.kiln/`. A sensor may also be a *command* naming the script it runs
   (`` `python scripts/store-kit.py --observations` ``): the file that must exist is the script.
2. `- Sensor measured before it was trusted: ...` — states the path **and** how it was shown to fail.
   The path is what a reader follows; the prose beside it is what a reader reads.
3. `` - Sensor: none - <reason> `` — an explicit "no sensor is possible", with a reason of at least 40
   characters. A one-off console sequence or a UI observation legitimately leaves nothing to run, and
   saying so is a decision a reader can challenge. Silence is not. The bullet is read as a
   *declaration*: it has to open with `- Sensor: none`. The distinction was paid for — testing for the
   word `none` anywhere in the bullet counted a long sensor bullet quoting a FAIL row ("carries no
   screen with that id and none whose console label is …") as "no sensor is possible", which silently
   turned a real 431-leg suite into a `none`.

A `.kiln/` path does not satisfy the convention on its own, because `.kiln/` is disposable local state
by design (`AGENTS.md`) and a sensor that lives only there disappears with the next cleanup. Citing a
throwaway injection harness beside a durable check is legitimate and is reported as a note.

Entries dated before **2026-09-29** predate the sensor bullet entirely; 60 of them carry no sensor
line. They are the journal's history and are not rewritten, which is also why the journal keeps a
reader instead of a gate: a program that checked the 2026-09-29 contract could only ever report on
part of the file, and a partial verdict about a file that is 80 % history reads as a verdict about the
whole file.

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

This primitive is deliberately **not** re-implemented by the fact-ledger check. The fact-ledger check
is the trigger and the coverage check for the *facts*; this is the transcription mechanism for the
*console*, and it is the one part of the loop whose subject is a page no program can open.

## Outputs

Every run produces a **work order**, not a score.

| Output | Verdict | What it means | What to do |
|---|---|---|---|
| a fact names an enforcing program that is not there | `FAIL` | the record names a machine check that is gone, or does not parse | fix the record, or restore/rename the check |
| a fact past its window | `PASS`, with `UNVERIFIED` in the row's detail | the reading is old; nothing is broken | re-open its `source`, re-measure, then update `verifiedAt` and every `whereUsed` path |
| a fact that names no enforcing program | `PASS`, with `UNENFORCED` and its `gap` in the detail | the rule is prose | write the check the rule is only prose about, then set `enforcedBy` |
| a partial enforcement (`gap` beside a real `enforcedBy`) | `PASS`, with the gap in the detail | a real sensor covers only part of the claim | read it; a claim larger than its check is how a partial sensor becomes a false one |
| everything in window and enforced | `PASS` | — | nothing |

A stale fact is deliberately **not** a failure and deliberately not silent: a date only a human can
re-measure is not a code defect, and a run that went red on it would teach people to silence the
check, which is the failure mode the ledger exists to prevent. The `UNVERIFIED` token is in the
detail, so the state survives in the string a reader actually reads.

An empty ledger is a failure on purpose: "every entry is fresh" is vacuously true for zero entries, so
deleting the facts would otherwise be the cheapest way to pass. So is a ledger in which **no** entry
names an enforcement that exists — a ledger that asserts nothing is not a green ledger.

## Cadence

| When | Run | Why then |
|---|---|---|
| Before a slot's UI exists (stage 0.5/0.6) | `node scripts/verify.mjs` | a plan built on a stale rule is a plan built on a rule Play may no longer have |
| At each `/ship` and `/release` boundary | `node scripts/verify.mjs` (`--slot <slug>`) | source-only, no device, no build, and it is the same command as everything else |
| Whenever a fact's window elapses | the `facts.ledger` row's own detail | the window is the schedule; the fact's own `expiresAfterDays` decides when it comes due |
| After every defect a slot cost you | read `harness/lessons.md` and cite the sensor | the entry is being written anyway; naming the sensor is what stops the defect returning unowned |
| Whenever the console was read for a release | `python scripts/store-kit.py --observations` | the transcription is only evidence after it has been checked against the model |

The windows are the clock, and they are deliberately two: **180 days** for a moving target and
**365 days** for a long-lived one. A window that does not describe one — a `verifiedAt` that does not
parse, or an `expiresAfterDays` that is not a positive integer — is a broken record rather than a fact
nobody can re-measure, because an entry with no readable window can never come due.

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
  script satisfies the check, even in a comment. It proves the probe was not renamed or deleted; it
  does not prove it still checks what the fact claims.
- **`whereUsed` is asserted by existence, not by dependence.** The paths a fact names that are still
  on disk prove nothing about dependence; the ones that are gone are printed, and that is a prompt to
  read, not proof that the fact is used where it says.
- **Some facts have no forcing program at all, and that is a finding rather than a bug.** As of
  2026-09-30 one of the eighteen facts is unenforced, and one more is only partly enforced; both are
  listed under [The current gaps](#the-current-gaps).
- **A false `persisted: true` in a console transcription is not caught.** The validator refuses
  `persisted` on a screen the console never confirmed, and it compares the reading's `observedAt`
  against the model's provenance — but the transcription itself is a human statement about a page a
  program cannot see.
- **The lesson journal has no backstop.** Nothing fails when an entry names a sensor that has since
  been deleted, and nothing fails when an entry names none. That is the price of removing the gate
  (row 17.2), and it is stated here rather than implied by silence.
- **Nothing runs on a timer.** The cadence above is a rule for whoever runs the next command; no
  scheduler or hook invokes these checks on a schedule. What is wired is the check: the fact-ledger
  verdict is part of the run that `node scripts/verify.mjs` performs, and the verification entry point
  is the only thing an operator has to remember.

## The current gaps

This is a reading of `docs/research/measured-facts.json` on 2026-09-30, and it is the honest headline
of the whole loop: a rule the factory follows with nothing to defend it.

| Fact | State | What is unenforced |
|---|---|---|
| `play-auto-string-translation-overrides` | `enforcedBy: null` | nothing asserts the factory position. Declining Play Console automatic string translation for a language a slot already translates lives only in `AGENTS.md` and `docs/research/2026-09-22-android-play-requirements.md` prose, so nothing fails if a slot enables the feature and lets Console machine output override a git-tracked `values-<locale>/` translation. A check would have to read the console state — which the repository does not hold — or be a review note, so it stays an honest `null` rather than an invented script. |
| `factory-toolchain-pin` | partial (`gap`) | `scripts/kiln-status.mjs` probes the Gradle wrapper 9.7.1, `compileSdk` 37, `targetSdk` 36 and Build Tools 36.0.0, and the JDK 17 floor is `MIN_MAJOR_SUPPORTED` in `scripts/lib/java.mjs`. The **AGP 9.4.1, Kotlin/Compose Compiler 2.3.21 and Compose BOM 2026.09.00** pins are asserted by no program. |

Adding a check for either is a work item, not a documentation fix. Until then the run reports both
as `UNENFORCED` in the ledger row's detail, so neither can be mistaken for green.

## Closing the loop — the operator commands

```bash
# 1. Is any measured fact due, enforced, or unenforced? The row's detail is the work order.
node scripts/verify.mjs                       # every project-wide invariant, including facts.ledger
node scripts/verify.mjs --only invariants     # the same invariants alone, ~20 ms
node scripts/verify.mjs --json                # the same report, machine-readable

# 2. Re-measure what it named: open the entry's `source`, confirm or correct the claim,
#    then update `verifiedAt`, `expiresAfterDays` and every `whereUsed` path in
#    docs/research/measured-facts.json. If the rule moved, fix the code that encodes it -
#    and if the check itself moved, update `enforcedBy` too.

# 3. Read the console, write one file per screen under .kiln/console-observations/, then check it.
python scripts/store-kit.py --observations            # validate + print the model edits it implies
python scripts/store-kit.py --observations --apply-observations   # write the foldable ones back
```

`facts.fixtures` — the inline fixture set in `scripts/core/facts.mjs` — is what makes the ledger check
trustworthy: it is run by the same command, and it is the check's own proof that it can fail. See
[Proof that the check can fail](#proof-that-the-check-can-fail).

### Automated today, and what is not

| Part of the loop | Automated | Discipline |
|---|---|---|
| Noticing a fact is due | yes — the `facts.ledger` row computes the age and prints it | |
| Noticing a named check is gone | yes — a `FAIL` names the fact and the missing script or probe | |
| Noticing a rule has no check at all | yes — the row names the fact, `UNENFORCED` and its `gap` | |
| Noticing a lesson's sensor was deleted | | **discipline** — the journal has a reader, not a gate (row 17.2) |
| Validating a console transcription against the model | yes — `store-kit.py --observations` | |
| **Fetching the source** | | **discipline** — a person or an agent opens the page; nothing here does |
| **Deciding the rule moved** | | **discipline** — "still true" versus "changed" is a judgement |
| **Writing the check a `gap` names** | | **discipline** — the row names the hole; someone has to write the sensor |
| **Running the loop at all** | partly — the ledger row runs with the one verification command | **discipline** — nothing schedules it, so it is read when the command is run |

## Proof that the check can fail

A check that cannot fail is not a check. The proof is now part of the check: `facts.fixtures` runs
`auditFactLedger` against synthetic ledgers and asserts both directions of every rule, and it is a row
in the same report as the ledger it guards. It carried **18 fixtures** on 2026-09-30, including the
negative controls that keep it honest — a fact naming an id that does not exist (red) and the same
fact naming a declared one (green), a fact whose window has elapsed (reported, never fatal) and the
same fact re-dated (silent), a `null` enforcement with a gap (an honest hole) and without one (a
broken record), a registry reader that must not mistake a longer key such as `valid:` for an id, and
the real registry, read from disk, still declaring every id its checks answer to.

Every fixture was observed red before green, and the whole set was driven against **copies** of the
repository's own ledger so no run ever left the real record broken. The injection harness is
`.kiln/facts-demo.mjs` (throwaway, gitignored) and its observed output is recorded in
`openspec/changes/reduce-factory-to-core/deletions.md`. Two of its rounds are quoted here, because
they are the ones a reader can check against the ledger on disk:

```
A. The registry reader: which check ids may a fact name?
  scripts/core/index.mjs declares: invariants, assets, icon, apk, project
  RED   as the anchored reader saw the registry (ids: ['invariants']): FAIL
        play-listing-text-caps: names the check id 'assets', which is not declared by scripts/core/index.mjs (declared: invariants)
  GREEN with the reader fixed: PASS — 17 of 18 fact(s) name an enforcement that exists — 1 by a core check id, 16 by a surviving script

C. A fact past its re-check window: reported, never fatal.

GREEN a fact last re-measured in 2020
  verdict: PASS
  detail:  1 past the re-check window today, so UNVERIFIED rather than FAIL — re-open the source and re-date the entry: play-feature-graphic-spec (re-checked 2020-01-01, 180-day window)
```

The first round is the reason the registry is read by parsing rather than anchored to the start of a
line: the registry writes one entry per line and three of its five ids follow `{ ` on the same line as
their object, so an anchored reader found one id out of five and would have reported three real checks
as undeclared — turning the three facts that name them into phantom failures.

`facts.fixtures` performs the same kind of proof permanently and automatically, which is the part that
cannot drift back.
