# Deletions, with the successor stated first

Task 5 of `openspec/changes/reduce-factory-to-core`, executed 2026-09-30. This file is the audit the
task asks for: **every deleted file, the ledger row that permits the deletion, the successor that must
exist and run before the source goes, and the search that found no other consumer.**

`requirements-preservation.md` §18.1 is the binding rule — *do not delete a file before its ledger
rows are honoured, and the successor must exist and run before the source is removed* — so the successor
for each file was written, run, and read before the file was removed. Nothing here was deleted on the
argument that it looked unused.

## 0. What was measured

| | before | after |
|---|---:|---:|
| files under `scripts/` (excluding `__pycache__`) | 101 | 94 |
| lines, `(Get-Content <file>).Count` (blank lines included) | 47 422 | 36 226 |
| lines, `Measure-Object -Line` (blank lines excluded) | — | 32 787 |

**Removed from `scripts/` by this change, 17 707 lines** (`Measure-Object -Line` over each file as it
stood in `git show HEAD:<path>`), and **added back 3 055 lines** (`scripts/store/{model,render,validate}.py`,
created by the store port) — a net **14 652 lines** by that method, **11 196** by the
blank-line-including one. Three workstreams are in this tree at once, so the reduction is attributed:

| Removed by | Files | Lines (`Measure-Object -Line`) |
|---|---:|---:|
| **this session** — the meta-runner chain (task 5.1) | `scripts/selftest.mjs` (3 506 by `.Count`, **327 764 bytes**) | 3 244 |
| **a concurrent pass on the same change** — the two ledger gates (task 5.4) | `scripts/check-freshness.mjs`, `scripts/lib/freshness.mjs`, `scripts/check-lessons.mjs`, `scripts/lib/lessons.mjs`, `scripts/test-freshness.mjs` | 1 382 |
| **the store/console port** (another workstream, do-not-touch here) | `scripts/store-kit.py`, `scripts/console-walkthrough.mjs`, `scripts/test-console-answers.py`, `scripts/lib/console-form.json` | 13 081 |

The five ledger-gate files were already absent when this session's deletion pass ran, and the evidence
is in the index rather than in this file's prose: `git status --short scripts` reports them as
`D  …` — deletions **staged**, which is what `git rm` leaves — while `scripts/selftest.mjs`, the file
this session removed, shows as ` D …` (worktree only). Both outcomes are the ones this task asks for;
the attribution is recorded so a later reader does not credit one pass with another's files.

Measured with `Get-ChildItem scripts -File -Recurse -Force | Where-Object { $_.FullName -notmatch
'__pycache__' }` and `(Get-Content <file>).Count`, so blank lines are counted; PowerShell's
`Measure-Object -Line` does not count them and this repository is mostly blank lines between comment
paragraphs (the error `docs/factory-core.md` records at its own end). Every figure above names which
of the two it came from.

## 1. Task 5.4 — the two ledger gates

| Deleted file | Lines (`Measure-Object -Line`) | Ledger row that permits it | Successor, run before deletion |
|---|---:|---|---|
| `scripts/check-freshness.mjs` | 169 | 17.1 — `DATA` keeps the ledger, `DROP` the freshness *gate* | `checkFactLedger()` in `scripts/core/facts.mjs`, verdict id `facts.ledger` |
| `scripts/lib/freshness.mjs` | 366 | 17.1 | the same module: contract, loader and per-fact audit in one file |
| `scripts/check-lessons.mjs` | 68 | 17.2 — `DROP` the gate, `CHECKLIST`: the lessons file stays and is read by the builder | **no code successor, deliberately.** Row 17.2's reason: it "checks that a filename exists, which is not that a check works". `harness/lessons.md` stays as the readable record; the sensor bullet becomes a writing convention (§"harness/lessons.md" in `docs/self-learning.md`) read by the builder role card |
| `scripts/lib/lessons.mjs` | 246 | 17.2 | as above |
| `scripts/test-freshness.mjs` | 533 | 17.1 | `factFixtureCases()` — 18 inline fixtures in `scripts/core/facts.mjs` |

**Kept, as the ledger requires:** `docs/research/measured-facts.json` (the data, row 17.1) and
`harness/lessons.md` (the readable record, row 17.2). Both are still on disk; `docs/self-learning.md`
still names them, and `scripts/kiln-status.mjs` still requires them.

### 1.1 What the successor keeps, and what it deliberately drops

Kept: a fact naming an enforcing program that does not exist is a `FAIL` naming the fact; an empty
ledger is broken (vacuously green otherwise); a fact naming no program is reported `UNENFORCED` with
its `gap`; a ledger in which *nothing* is enforced is a `FAIL`; the window semantics ("inside on day N,
stale on day N+1"); a stale fact is reported as `UNVERIFIED` in the row's detail and is **not** a
failure.

Dropped: the `0/1/2` exit-code vocabulary and the standalone CLI. One function returns one verdict,
and `scripts/verify.mjs` already carries `PASS`/`FAIL`/`UNVERIFIED`. The stale state survives as the
`UNVERIFIED` token in the detail — the string the old gate's readers actually read.

### 1.2 The search that found the consumers

```
rg -n "check-freshness|lib/freshness|check-lessons|lib/lessons|test-freshness" .
```

| Consumer found | Action |
|---|---|
| `scripts/verify-slot.mjs` (a freshness stage that spawned the deleted script, and a self-test stage that spawned the deleted `selftest.mjs`) | **repaired** — both stages removed, `--no-selftest` kept as an accepted no-op so a recorded command line still runs (§4) |
| `scripts/kiln-status.mjs` (required-harness-files list) | **repaired** — the four deleted paths removed; `scripts/verify.mjs`, `scripts/core/index.mjs` and `scripts/core/facts.mjs` added |
| `docs/self-learning.md` | **rewritten** — the check is `facts.ledger` in `scripts/core/facts.mjs`; the lessons gate is gone and the journal is a reader-based `CHECKLIST` item |
| `scripts/check-outcomes.mjs`, `scripts/lib/outcomes.mjs` (comments citing the deleted script) | **repaired** — the comment now cites the fact-ledger check |
| `AGENTS.md` (`Self-learning loop` table, layout, verification list) | **left alone** — `AGENTS.md` is on this change's do-not-touch list (it is the L1 contract for both agents, 27 lines of it describe the retired gates). Recorded here as a follow-up for the file's owner |
| `harness/lessons.md`, `docs/reviews/2026-09-29-factory-review-decisions.md` | **left alone** — dated records of runs that happened; rewriting them would falsify evidence, which is the same reason `docs/self-learning.md` grandfathers the 60 pre-contract lesson entries |
| `docs/factory-core.md` | **annotated** — the owner-facing reduction plan is a dated snapshot; a status line was added so its C-section rows are not read as current inventory |

### 1.3 The successor was run before the deletion

```
17 of 18 fact(s) name an enforcement that exists — 0 by a core check id, 17 by a surviving script;
1 UNENFORCED, never a pass: play-auto-string-translation-overrides (...)
```
`PASS`, exit 0 — and `facts.fixtures`: `PASS — 18 fixtures`.

## 2. Task 5.1 — the meta-runner chain

| Deleted file | Lines | Ledger row that permits it | Successor, run before deletion |
|---|---:|---|---|
| `scripts/selftest.mjs` | 3 506 (`.Count`) / 3 244 (`Measure-Object -Line`) | task 5.1 (the meta-runner chain); `docs/factory-core.md` §C row 1 names it as the largest check-the-checks file, run twice per verification | `scripts/verify.mjs` (one process, one row per check) plus the inline fixture sets in `scripts/core/{assets,apk,config,icon,project,facts}.mjs`; the *knowledge* — every surviving check and suite with its exact command — is `docs/factory-checks.md` |

The suite runners `selftest.mjs` used to spawn are **not** deleted as a group. Section 3 records the
search for each one; the ones that survive do so because a record names them, a surviving script
invokes them, or their sensor has not been ported.

## 3. Task 5.5 — every remaining check, and the search for its motivation

Method: for each file, `rg -l --hidden -g '!node_modules' -g '!apps/**' -g '!.kiln/**' --fixed-strings
"<name>" .` and the hits were read to see whether any is a *motivation* (a recorded incident, an L1
contract, a surviving consumer) or only a *proposal to delete* (`docs/factory-core.md`) or a
self-reference.

**Nothing was deleted on this pass.** Every candidate either has a `CORE`/`DATA` row in
`requirements-preservation.md`, is named by `AGENTS.md`, is invoked by a surviving script, or is the
recorded sensor of a defect in `harness/lessons.md`. The task says to delete only after the search
fails to find motivation; the search found it. The table is the audit.

| File | Lines | Search result | Decision |
|---|---:|---|---|
| `scripts/test-icon-design.py` | 559 | ledger row **5.11** — `CORE`: "the agreement check is one of the few suites that stays" | keep |
| `scripts/lib/icon_design.py`, `scripts/lib/icon_qa.py` | 1 388 | rows 5.1–5.15 (the five primitives, rendered twice); required by row 5.11 | keep |
| `scripts/test-apk-provenance.mjs` | 349 | ledger row **13.5** — `CORE` ("this regression stays"); named by `AGENTS.md` | keep |
| `scripts/test-client-secrets.mjs` | 404 | ledger row **11.9** — the negative and positive controls; named by `AGENTS.md` and `.cursor/rules/client-secrets.mdc`. Task 1.5 records that the negative controls still live only here | keep |
| `scripts/test-locale-dedup.mjs` | 138 | ledger row **7.6** — `CORE`; named by `AGENTS.md` | keep |
| `scripts/test-release-notes.py` | 184 | ledger row **7.3** — `CORE` | keep (also a do-not-touch file) |
| `scripts/test-slot-lock.mjs` | 386 | task 5.6 keeps the lease and its regression; ledger row **3.8** `CORE` | keep (§4.2) |
| `scripts/test-core-equivalence.mjs` | 195 | this change's own equivalence evidence (`tasks.md` 1.6, `measurements.md`, `package.json` `kiln:equivalence`) | keep |
| `scripts/lib/slot-lock.mjs` | 376 | ledger row **3.8** — `CORE`: the lease that names the holder | keep (§4.2) |
| `scripts/test-in-app-policy.mjs` | 279 | invoked by `verify-slot.mjs` (which FAILs if the policy sensor is missing); ledger row **9.1** — `CORE` | keep |
| `scripts/check-in-app-policy.mjs`, `scripts/lib/in-app-policy.mjs` | 332 | row 9.1 `CORE`; `AGENTS.md` layout; `verify-slot.mjs` invokes the CLI; `scripts/core/project.mjs` imports the evaluator | keep |
| `scripts/test-capture-publish.mjs` | 274 | `AGENTS.md` verification list; the sensor of a recorded lesson; guards the surviving `lib/capture-publish.mjs` | keep |
| `scripts/test-capture-assertions.mjs` | 213 | `media-capture.mjs` is written to be driven by it (its CLI guard names the suite); guards a surviving module | keep |
| `scripts/test-capture-locale.mjs` | 113 | `harness/lessons.md` 2026-09-30 — a recorded defect (a capture in the wrong locale) with this suite as its sensor | keep |
| `scripts/test-codex-signals.mjs` | 210 | `harness/lessons.md` (the classifier that reported finished work as blocked); guards `lib/codex-signals.mjs`, used by the documented Codex channel in `AGENTS.md` | keep |
| `scripts/test-kill-path.mjs` | 116 | `docs/reviews/2026-09-29-factory-review-decisions.md` (19/19 against a real mirror); `AGENTS.md` stage 11 documents `kiln-status --kill` | keep |
| `scripts/test-outcomes.mjs` | 397 | `AGENTS.md` verification list; guards the earn stage | keep |
| `scripts/test-pack-approval.mjs` | 285 | `AGENTS.md` verification list and the approval-boundary section | keep |
| `scripts/test-play-reviews.mjs` | 180 | `AGENTS.md` verification list | keep |
| `scripts/test-play-scan.mjs` | 325 | `harness/lessons.md` 2026-09-23 — the relevance guard and the WHOOP markup defect, with this suite as the synthetic sensor; `docs/reviews/2026-09-23-play-scan-audit.md` | keep |
| `scripts/test-stop-gate.mjs` | 199 | `harness/lessons.md` — the quiet-turn defect and the false-exit-code defect; guards the live `.cursor/hooks/stop-gate.cjs` | keep |
| `scripts/test-visual-qa-discovery.py` | 165 | `AGENTS.md` verification list; guards the surviving `visual-qa.py` theme reader | keep |
| `scripts/test-checklist-products.py` | 274 | `harness/lessons.md` — "sensor: walks the generated checklist of a billing slot"; adjacent to the console workstream that owns the generator | keep |
| `scripts/validate-play-scan.mjs`, `scripts/verify-play-scan-defects.mjs` | 253 | `harness/lessons.md` 2026-09-23 (calibration and the live re-check); `scripts/market-queries.json` cites the calibration as the reason its control queries must not drift | keep |
| `scripts/verify-reviews-sensor.mjs` | 227 | `AGENTS.md` verification list | keep |
| `scripts/check-ui-coverage.mjs` | 215 | invoked by `verify-slot.mjs`, which FAILs if it is missing; `harness/lessons.md` — the defect where it printed `PASS` while checking nothing, fixed and re-verified on real slots; `docs/reviews/2026-09-29-factory-review-decisions.md` D6 "Accepted, shipped" | keep — and its consumer is a file this change must not edit |
| `scripts/harness-inventory.mjs` | 137 | `.cursor/commands/{diagnose,improve-harness,kiln-status}.md` and `harness/README.md` invoke it | keep |
| `scripts/setup-mcp.mjs` | 62 | `README_RU.md` and `.cursor/skills/run-playkiln/SKILL.md` | keep |
| `scripts/publish-privacy.mjs` | 243 | `docs/research/2026-09-22-android-play-requirements.md` — written to satisfy the public privacy-policy URL requirement | keep |
| `scripts/scan-*.mjs`, `scripts/enrich-market.mjs`, `scripts/scan-titles.mjs`, `scripts/lib/play-*.mjs` | 3 845 | the scout's demand-measurement instruments, not gates (this change's do-not-delete list) | keep |
| `scripts/codex-run.mjs`, `scripts/lib/codex-ledger.mjs`, `scripts/lib/codex-signals.mjs` | 744 | `AGENTS.md` "Agent model" documents the runner and its verdict vocabulary; `harness/codex-workers.md` | keep |
| `scripts/store-kit.py`, `scripts/store/**`, `scripts/console-walkthrough.mjs`, `scripts/lib/console-form.json`, `scripts/test-console-answers.py`, `scripts/test-release-notes.py`, `scripts/test-store-studio-completeness.py` | 10 000+ | owned by another workstream (do-not-touch). **That port has since deleted** `store-kit.py`, `console-walkthrough.mjs` (with `docs/play-console-walkthrough.md`), `test-console-answers.py` and `lib/console-form.json`, keeping `test-release-notes.py`, `test-store-studio-completeness.py` and the new `scripts/store/{model,render,validate}.py` | untouched **by this task**; its consequences for the surface are in §6 and §7 |
| `scripts/check-16k.py`, `scripts/check-adaptive-icon.py`, `scripts/check-locales.mjs`, `scripts/check-economics.mjs`, `scripts/check-differentiators.mjs`, `scripts/check-outcomes.mjs`, `scripts/check-in-app-policy.mjs`, `scripts/check-client-secrets.mjs`, `scripts/run-slot-tests.mjs` | ~2 000 | each is a `CORE` row or is invoked by `verify-slot.mjs` | keep |

One item is worth naming as a **finding rather than a deletion**: while this audit ran, `scripts/store-kit.py
--slot teacher-gradebook --validate-only` exited 1 with `missing scripts/lib/console-form.json`, and
that script is now gone altogether — the port deleted it in the same window. The port's own
`scripts/store/validate.py` answers the same question and is what `docs/factory-checks.md` lists. The
one loose end it left is `scripts/core/config.mjs`, which still reads the deleted
`scripts/lib/console-form.json` and is not a file this task may edit, so `node scripts/verify.mjs
--only invariants` reports `UNVERIFIED config.parse` until its owner lands the replacement.

## 4. Repairs this change had to make to surviving files

Deleting a file is only half of it; the consumers have to still work. These edits are exactly the
removal of a call to a file this change deletes, or the retargeting of a comment to the successor.
Nothing else in these files changed.

### 4.1 `scripts/verify-slot.mjs`

The gate spawned both deleted scripts and treated their absence as `FAIL`, so leaving it untouched
would have turned the legacy gate permanently red with a message about a file that no longer exists.

- the factory **self-test stage** removed (the flag `--no-selftest` is still accepted, and its help
  line now says the stage was retired, so a recorded command line still runs);
- the **freshness stage** (`verifyMeasuredFacts`) and its call site removed — the fact-ledger check
  owns this requirement now;
- one comment that cited `SUITE_EXCLUSIONS` in the deleted `selftest.mjs` retargeted to
  `docs/factory-checks.md`.

### 4.2 `scripts/lib/slot-lock.mjs` (task 5.6) — inspected, nothing deleted

Confirmed a lease that **names the holder** and refuses the second consumer **by name**, not by a
toolchain error: `node scripts/test-slot-lock.mjs` → **49/49 checks passed, exit 0**, including "the
refusal is a SlotLockHeldError", "the refusal carries the stable `SLOT_LOCK_HELD` code", "the refusal
names the holder pid / purpose / remedy", and "the refusal is thrown before the slot lease is taken".

The task also permits deleting any part that substitutes for *scheduling*. There is none: the wait is
bounded (120 s) and ends in a refusal that names the holder, the slot is scoped per slot directory so
two slots never contend, and the only way to skip acquisition is the documented fixture escape hatch
`PLAYKILN_SLOT_LOCK=off`, which is itself named a hazard in the module's own docblock and asserted by
six legs of the suite this task keeps. `scripts/lib/slot-lock.mjs` is unchanged.

### 4.3 Others

| File | Edit | Why |
|---|---|---|
| `scripts/kiln-status.mjs` | the "Factory self-test" section now runs `node scripts/verify.mjs` and reports its exit code and any `FAIL`/`UNVERIFIED` row by name; the required-file list lost the five deleted paths and gained `scripts/verify.mjs`, `scripts/core/index.mjs`, `scripts/core/facts.mjs` and `scripts/store/{model,render,validate}.py` | the section spawned `selftest.mjs` and printed `FAIL scripts/selftest.mjs is missing` once it was gone — a health report about a deleted file instead of about the factory |
| `scripts/new-slot.mjs` | two comments now name the `build.copyFilter` check (`scripts/core/build.mjs`) as the consumer of the exported exclusion predicate, and record that the sandbox slot run has no successor | the export's only consumer is that check; the comment claimed a deleted suite |
| `scripts/lib/gradle.mjs` | three comments now name `scripts/test-slot-lock.mjs` as the suite that drives the injectable spawner, the `javaHome: null` skip and the one-invocation contract | it is the surviving suite at those seams (`test-slot-lock.mjs` asserts "an injected spawner is the harness path and still runs one invocation") |
| `scripts/lib/release-approval.mjs` | the paragraph that ended "`scripts/selftest.mjs` fails a name whose reason is not in the set…" now says those three assertions have no successor and points at §7 | leaving it would have been a document claiming a check that no longer exists |
| `scripts/lib/in-app-policy.mjs` | the docblock now says the Python half reads `placeholderHosts` from `play-assets.json` (`scripts/store/model.py`) and that nothing asserts the agreement any more, pointing at §7 | same reason |
| `README.md` | the "factory tests itself" block ran `node scripts/selftest.mjs`; it now runs `node scripts/verify.mjs` and points at `docs/factory-checks.md` | a live instruction to a deleted file |
| `docs/self-learning.md` | rewritten | the check is `facts.ledger`; the lessons gate is gone and its journal is a reader-based `CHECKLIST` item |
| `docs/factory-core.md` | status line added | it is a dated reduction plan, not current inventory |
| `docs/factory-checks.md` | new | the successor of `selftest.mjs`'s one useful job: which checks exist and the exact command that runs each |

## 5. Evidence

### 5.1 The fact-ledger check, red and then green

`node .kiln/facts-demo.mjs` (throwaway, gitignored) drives **copies** of the real ledger, so the
repository's own record was never left broken. Observed output, 2026-09-30 19:5x +03:00:

```text
A. The registry reader: which check ids may a fact name?
  scripts/core/index.mjs declares: invariants, assets, icon, apk, build, project
  RED   as the anchored reader saw the registry (ids: ['invariants']): FAIL
        play-listing-text-caps: names the check id 'assets', which is not declared by scripts/core/index.mjs (declared: invariants)
  GREEN with the reader fixed: PASS — 12 of 18 fact(s) name an enforcement that exists — 8 by a core check id, 4 by a surviving script

B. A fact that names an enforcement: does a broken name fail?

RED   one fact names a check id the registry does not declare
  verdict: FAIL
  detail:  play-listing-text-caps: names the check id 'no-such-check', which is not declared by scripts/core/index.mjs (declared: invariants, assets, icon, apk, build, project)

GREEN the same fact names a declared check id
  verdict: PASS
  detail:  12 of 18 fact(s) name an enforcement that exists — 8 by a core check id, 4 by a surviving script; 6 UNENFORCED …

RED   one fact names an enforcing script that is not on disk
  verdict: FAIL
  detail:  play-icon-asset-spec: names the enforcing script scripts/no-such-enforcing-program.py, which is not on disk — the fact claims a machine check that is gone

GREEN the same fact names a script that exists
  verdict: PASS
  detail:  12 of 18 fact(s) name an enforcement that exists — 7 by a core check id, 5 by a surviving script

C. A fact past its re-check window: reported, never fatal.

GREEN a fact last re-measured in 2020
  verdict: PASS
  detail:  1 past the re-check window today, so UNVERIFIED rather than FAIL — re-open the source and re-date the entry: play-feature-graphic-spec (re-checked 2020-01-01, 180-day window)

GREEN the same fact re-dated today
  verdict: PASS

D. A fact that names no enforcing program: an honest hole needs its sentence.

RED   a fact stripped of its enforcement and of its gap
  verdict: FAIL
  detail:  play-screenshot-aspect-rule: names no enforcing program and carries no gap — UNENFORCED is the honest answer only when it says in one sentence what is unenforced

GREEN the same fact carrying the gap that says what nothing reads
  verdict: PASS
  detail:  7 UNENFORCED, never a pass: play-screenshot-aspect-rule (nothing can read a console toggle, so the position is prose)

DEMO PASS — 9/9 cases behaved as stated
```

Two findings from running it twice, an hour apart, rather than reasoning about it. The first is why the
registry is read by delimiter rather than anchored to the start of a line: the anchored reader matched
**one** id out of six, so three real checks would have been reported as undeclared and the three facts
naming them turned into phantom failures. The second is that a demonstration asserting a *count* of
facts ("1 by a core check id") broke the moment the store port retargeted nine facts to `assets` —
the demo's own lesson, that a check which quotes a moving number is a check on the number; it was
made count-agnostic and re-run clean.

`facts.fixtures` is the durable half — 18 inline fixtures in `scripts/core/facts.mjs`, run by the same
command as the check they guard (`PASS — 18 fixtures`). The demo above is the throwaway half, and it
is cited as such: a `.kiln/` harness is not a sensor (it disappears with the next cleanup), which is
exactly why the fixtures exist.

### 5.2 The commands

| Command | Observed 2026-09-30 (PowerShell, no Gradle) |
|---|---|
| `node scripts/verify.mjs --only invariants` | **16 PASS, 1 UNVERIFIED, exit 2** — the row is `config.parse: not evaluated: scripts/lib/console-form.json: absent`, the concurrent console port's in-flight state (§6). `facts.ledger` PASSes and `facts.fixtures` PASSes (18 fixtures) |
| `node scripts/verify.mjs --slot teacher-gradebook` | **29 PASS, 1 UNVERIFIED, 1 FAIL, 2 SKIP** — `FAIL pack.media` is the owner-accepted 5:8 divergence of the shipped tablet assets, `UNVERIFIED config.parse` as above, `SKIP slot.outcomes` is `NO-DATA`, `SKIP build` needs `--build` |
| `node scripts/kiln-status.mjs` | `VERDICT: NOT READY`, exit 1, and it names the single `UNVERIFIED config.parse` row under "Factory checks" after the repair in §4.3 |
| `node scripts/test-core-equivalence.mjs` | `RESULT: equivalent` |
| `node scripts/test-slot-lock.mjs` | 49/49 |
| every surviving suite and per-slot check | the table in `docs/factory-checks.md`, each row a real run dated 2026-09-30 |

The `facts.ledger` row is the one this task added, and its first real run was a **red** one: nine facts
still named the deleted `scripts/store-kit.py` as their enforcing script, and the check failed naming
each fact. The store port retargeted them to the `assets` core check a minute later and the same
command returned PASS. That is the sensor demonstrated on the repository's own data, not on a fixture.

## 6. Left alone, with the reason

| Left alone | Why |
|---|---|
| `AGENTS.md` references to `selftest.mjs`, `check-freshness.mjs`, `check-lessons.mjs`, `lib/freshness.mjs`, `lib/lessons.mjs` (~8 places) | `AGENTS.md` is on the do-not-touch list: it is the L1 contract for both agents, and its owner is another workstream. The stale references are a follow-up for that owner, named here rather than silently edited |
| `scripts/core/index.mjs` | on the do-not-touch list. It now imports `checkFactLedger` and `factFixtures` from `./facts.mjs` and runs both under `invariants`, so this deliverable is reachable from the single entry point — verified, not assumed: `node scripts/verify.mjs --only invariants` prints `facts.ledger` and `facts.fixtures` rows |
| comments in `scripts/verify.mjs` and `scripts/core/index.mjs` that name `selftest.mjs` as history | both files are on the do-not-touch list; each comment explains why the core is not a chain, which is the record of the change itself |
| `harness/lessons.md`, `docs/reviews/**`, `docs/research/measured-facts.json`, `openspec/changes/**` | dated records and data; the ledger keeps the JSON, and rewriting a historical record falsifies evidence |
| `docs/research/measured-facts.json` while the store port was retargeting it | it is the ledger this check reads, and a second writer was mid-edit (mtime 19:37:32). Editing it from here would have raced their write; the check's own red row was the useful contribution, and it went green on their edit |
| `scripts/store-kit.py`, `scripts/store/**`, `scripts/console-walkthrough.mjs`, `scripts/lib/console-form.json`, `scripts/test-console-answers.py` and the suites around them | explicitly owned by another workstream. They are now **deleted** by that port — `scripts/store/{model,render,validate}.py` are the survivors — and one consequence is still open for its owner: `scripts/core/config.mjs` (also not mine) still reads the deleted `scripts/lib/console-form.json`, so `config.parse` reports `UNVERIFIED` |
| `scripts/verify-slot.mjs`'s remaining references to `scripts/lib/console-form.json` and `scripts/store-kit.py` | the legacy gate now exits 1 on the reference slot (`FAIL store pack inventory — scripts/lib/console-form.json could not be read`, `WARN provenance unverified — scripts\store-kit.py is missing`) because the console/store port deleted both while that gate still names them. This task's own edits to the file were only the removal of its self-test and freshness stages (§4.1); these two dependencies are the port's to repoint, and `node scripts/verify.mjs --slot <slug>` is the command the pipeline now uses |
| `package.json` `"kiln:assets": "node scripts/store-kit.mjs"` | a pre-existing dangling npm script (`scripts/store-kit.mjs` never existed; the program was `scripts/store-kit.py`, now itself deleted by the port). Not created by this change, and `package.json` is not mine to repoint — observed and recorded |

## 7. Assertions that lost their sensor, and are recorded instead of trusted

`requirements-preservation.md` §18 forbids deleting a file before its rows are honoured, and the
honest form of "honoured" includes naming what the deletion made unguarded. Three assertions lived
only in `scripts/selftest.mjs` and have **no successor** in the core: the surviving modules say so in
their own docblocks, and they are listed here so a reader of those comments finds the full account.

| Assertion that was in `selftest.mjs` | What it guarded | State now |
|---|---|---|
| a name in `EXCLUDED_FILE_REASONS` whose reason is not in `EXCLUSION_REASONS`, a reason no name uses, and an `EXCLUSION_CRITERION` that does not name every reason | the approval digest's exclusion list and the sentence that explains it (`scripts/lib/release-approval.mjs`) | **unguarded** — the set is a literal and a new name can cite a reason nobody defined without any program objecting |
| `PLACEHOLDER_HOSTS` in `scripts/lib/in-app-policy.mjs` agrees with the Python half | two holders of one list: a reserved-host list that stops matching is a pack shipping an unhosted privacy policy while both gates read `PASS` | **unguarded** — the Python half moved to `scripts/store/model.py`, which reads `placeholderHosts` from `scripts/lib/play-assets.json`, and nothing compares the two |
| `new-slot.mjs` run end to end against a real scaffold in a temp sandbox (`isExcludedScaffoldPath` plus the rewrite pass over a whole slot) | that a created slot is complete and that the copy filter does not drop what the slot needs to build | **partly guarded** — the predicate itself is swept and probed by `build.copyFilter`, but no check scaffolds a slot any more |

Each of those is a real loss and each is cheap to restore **inside the core**, as a fixture in the
module that owns the rule (`scripts/core/project.mjs` for the first two, `scripts/core/build.mjs` for
the third). They are not restored here because those modules are on this change's do-not-touch list —
they belong to the workers who own them, and this file is the handoff.

## 8. Closed by the orchestrator, 2026-09-30 (the change's own last pass)

Sections 6 and 7 were handoffs to owners who no longer exist: the change is one workstream being
finished, so the do-not-touch list is dissolved and every item left open above was either closed or
recorded as a standing fact. This section is what changed after §7 was written, and it is append-only
— nothing in §0–§7 was edited, so the earlier measurements remain as they were observed.

### 8.1 `scripts/verify-slot.mjs` — deleted, with every rule accounted for

The last file on the change's critical path. `deletions.md` §1.2, §4.1 and §6 all left it standing
because the successor did not yet cover four of its rows. They are covered now, and the coverage is
what permits the removal:

| Rule the legacy gate held | Successor now | Evidence |
|---|---|---|
| manifest, permissions, exported components, cleartext (`checkDangerousPermissions`) | `slot.manifest` in `scripts/core/manifest.mjs` | `PASS slot.manifest — 0 permission(s), none gated-and-undocumented…` on the reference slot |
| backup rules (`validateBackup`) | `slot.backup` | `PASS slot.backup — dataExtractionRules separates cloud-backup from device-transfer…` |
| capability compliance (`verifyCapabilityCompliance`) | `slot.capabilities` | `PASS slot.capabilities — capabilities.json enables billing; every enabled capability's SDK is declared` |
| in-slot secret paths (`checkSecrets`) | `slot.secretPaths` | `PASS slot.secretPaths — 2 secret path(s) present and each ignored by .gitignore` |
| unresolved placeholders (`checkUnresolvedTokens`) | `slot.tokens` | `PASS slot.tokens — no unresolved __TOKEN__ placeholder in 248 file(s)` |
| declared languages have resources (row 7.6) | `slot.locales` in `scripts/core/policy.mjs` | agrees with the legacy gate on all four slots, same sentence and same counts |
| the promise contract | `slot.differentiators` | `PASS — 7 promise(s), 5 core, 7 shipped, 0… deferred` on the reference slot; `FAIL` naming all three owner-decision claims on `pipe-sizing` |
| a listing claim has a control | `slot.uiCoverage` | agrees with the legacy gate on all four slots, including the two `UNVERIFIED` refusals |
| a build-less run is `UNVERIFIED`, exit 2 (row 12.11) | `scripts/verify.mjs` | `UNVERIFIED build — assembleDebug was not compiled because --build was not passed`; the row used to be `SKIP`, which exits **0** and is indistinguishable from a pass by exit code alone |

The two rows that were **not** one-for-one ports are stated rather than implied:

- **Row 12.11 was a genuine gap, not a port.** `verify.mjs` printed `SKIP build` with exit 0, so a
  caller reading only the exit code could not tell a source-only run from a full pass. That is the
  defect the row exists to prevent, and it was live in the successor until this pass.
- **The git half of the promise gate is dropped.** `verifyDifferentiators()` also read
  `differentiators.json` **in git**, so `rm`-ing the contract was a `FAIL` ("committed but missing
  from the working tree") rather than an `UNVERIFIED`. Reading git means spawning a subprocess, and
  the core may spawn only the build tool. The surviving protection is that an absent contract is
  `UNVERIFIED` naming the path, never a pass. No slot is in that state.

`--strict-store`, `--base <ref>` and `--no-selftest` are gone with it. `--strict-store` had become a
no-op flag that selected a stricter branch of two checks; both branches are now unconditional, so the
flag was dropped rather than kept as a lie.

**65 live files were retargeted** by one sweep (`.kiln/retarget-verify-slot.mjs`, throwaway): the
command name in 44 documents, and `--strict-store` / `--no-selftest` deleted wherever they appeared.
The sweep skips `.git`, `.kiln`, `openspec/`, `docs/reviews/`, `docs/research/`,
`docs/superpowers/`, `harness/lessons.md` and the dated recipes file, and it refuses to decode any
file holding a NUL byte (`.cursor/rules/verification-discipline.mdc` §16.2/§16.3). **It caught two
dated records by a bug in its own skip filter** — the filter was applied to directories only, so
`harness/lessons.md` and `docs/ideation/2026-09-26-five-recipes.md` were rewritten; both were restored
with `git checkout` and re-verified by `rg` before proceeding. Recorded because a sweep that reports
"65 files" and actually changed 67 is exactly the kind of claim this repository refuses.

**One live instruction the sweep could not finish by substitution**: `.cursor/skills/play-compliance`
§10 said "`--strict-store --build` for release evidence", which became a dangling flag reference. It
now states the whole command and the exit-2 rule.

### 8.2 The two consequences the deletion produced, both repointed

Both were found by running the gate the deletion belongs to, not by reading it.

| Check | What it said | Why | Fix |
|---|---|---|---|
| `build.copyFilter` | `FAIL scripts/verify.mjs: no NUL-byte guard before decoding bytes as text (checkUnresolvedTokens() scans every file of a slot as text)` | `NUL_GUARD_SITES` listed the three monoliths that each carried their own decode. The sweep retargeted `verify-slot.mjs` to `verify.mjs`, so the check began measuring a file that never decoded anything — a false accusation **and** a lost sensor | the list now names the real owners: `scripts/core/build.mjs` (`decodeSource()`, which every sweep imports) and `scripts/store/render_documents.py`. Listing a caller would measure nothing, since a caller that imported the predicate cannot skip it |
| `facts.ledger` | `FAIL play-target-api-36-requirement: names the enforcing script scripts/verify-slot.mjs, which is not on disk` | the fact named a deleted program as its enforcement | retargeted to `scripts/core/build.mjs`; the three `whereUsed` citations of the deleted gate retargeted to `scripts/core/assets.mjs` |

The second is worth keeping in view: the fact ledger is the one place in this repository where the
toolchain version pins claim a machine check, and the check failed the moment its subject was deleted.
That is the ledger doing its job.

### 8.3 §7's two cheap sensors, restored — and the third stated as partial

`scripts/core/harness.mjs` is new, and it carries the first two assertions `selftest.mjs` alone held:

| Check | What it now asserts | Observed |
|---|---|---|
| `harness.approvalExclusions` | every excluded name in the approval digest cites a reason the closed set defines, every defined reason is used by some name, and `EXCLUSION_CRITERION` names every reason — the sentence a human reads when deciding whether an approval covers a file | `PASS — 12 excluded name(s) across 3 reason(s), each reason used and named in the criterion` |
| `harness.placeholderHosts` | `PLACEHOLDER_HOSTS` in `scripts/lib/in-app-policy.mjs` and `placeholderHosts` in `scripts/lib/play-assets.json` are the same set, because a reserved host added to one half and not the other makes a pack whose policy URL is unhosted read as hosted by the check that decides whether the app can open a policy | `PASS — 6 reserved host(s), identical in both halves` |

`harness.fixtures` carries **10 fixtures** over both, and the sensor was verified by inverting one
comparison rather than asserted: with `if (!node.has(host))` disabled, the fixture set went
`FAIL — 1 of 10 fixture(s) failed: a host the Python half gained and the Node half lacks fails`, and
after restoring the file (`rg` proves the correct form is back) it returned `PASS — 10 fixtures`.

**§7's third row stays partial, and the reason is structural, not a shortage of time.** The end-to-end
assertion was "`new-slot.mjs` run against a real scaffold in a temp sandbox". `scripts/new-slot.mjs`
exports `isExcludedScaffoldPath`, `parseArgs` and `walkFiles` — the predicate and the walk, but not a
scaffolding entry point — so an in-process check cannot run it, and spawning `node new-slot.mjs` is a
second subprocess the core's own contract forbids. What survives is the half that can be measured
in-process: `build.copyFilter` sweeps and probes the exclusion predicate itself. Stated here so that
"restored" is not read as covering all three.

### 8.4 Two suites that were cited as evidence and could not run

Found by running every surviving suite rather than by reading the list — which is the whole reason
`docs/factory-checks.md` exists.

`scripts/test-icon-design.py` (559 lines) and `scripts/test-store-studio-completeness.py` (300 lines)
both died instantly on Python 3.13:

```text
File "<stdlib>/dataclasses.py", line 757, in _is_type
    ns = sys.modules.get(cls.__module__).__dict__
AttributeError: 'NoneType' object has no attribute '__dict__'
```

The cause is a dynamic loader that never registers its module: `dataclasses` resolves a field's type
through `sys.modules.get(cls.__module__)` for a `KW_ONLY` field, and a module absent from that mapping
returns `None`. Four loaders were affected — `scripts/store-studio.py` (`pack_model`),
`scripts/store/render.py` (`store_studio`), `scripts/test-store-studio-completeness.py` and
`scripts/test-release-notes.py` / `scripts/test-checklist-products.py` (both `store_render`) — and
`scripts/test-icon-design.py` already had the correct form, which is the precedent the fix follows.
Each now registers before `exec_module` and removes the entry if the load throws.

**The correction immediately found a real defect, which is the point.** With the loader fixed,
`test-icon-design.py` reports `VERDICT: PASS (0 failing legs)`, and
`test-store-studio-completeness.py` reports **27 of 28 checks passed**:

```text
FAIL the render is deterministic (re-render reproduces every asset) - 18 before, 18 after
```

Eighteen assets before, eighteen after, and at least one differs byte-for-byte between two renders of
the same input. A composed pack that is not reproducible is a provenance claim that cannot be
reproduced either, so this is a **finding, not a nuisance**, and it is recorded rather than re-run
until it agrees: the suite has been run twice this session with the same result, and the third run
above is the one quoted. It is also, on the evidence of the timestamp of the loader fix, a defect this
change did not introduce — it was invisible because the suite could not execute at all.

### 8.5 `AGENTS.md` — no longer left alone

§6 left eight stale references in `AGENTS.md` because it was another workstream's file. That owner is
gone, so the file was brought back into agreement with the tree, in the places where it would have
told a reader to run a program that does not exist:

| Region | Was | Now |
|---|---|---|
| stage table (stages 0.5, 0.6, 1, 2, 4, 5, 7, 11) | `scripts/verify-slot.mjs --build`, `--strict-store --build` | `scripts/verify.mjs` — one command, with the `UNVERIFIED`-without-`--build` rule stated where the stage is |
| store-pack contract table | `store-kit.py` checks the release name; `MAX_RELEASE_NOTE_CHARS` in `store-kit.py`; a full row describing the generated console sheet and `scripts/lib/console-form.json` | `scripts/store/validate.py`; `release_note_limit()` in `scripts/store/model.py`; and an explicit statement that `console-answers.md` is **no longer generated**, with the knowledge living in `docs/play-console-checklist.md` |
| self-learning ledger table | `check-freshness.mjs` and `check-lessons.mjs` as the readers | the `facts.ledger` core check for the facts; an explicit statement that the lessons file is a **record read by the builder**, not a gate, and why its gate was retired |
| layout list | `selftest.mjs`, `lib/freshness.mjs`, `lib/lessons.mjs`, `store-kit.py`, `console-form.json`, `console-walkthrough.mjs`, `test-console-answers.py` | `scripts/core/`, `scripts/core/index.mjs`, `scripts/verify.mjs`, `scripts/store/{model,render,validate}.py`, `docs/play-console-observations.json`, `docs/factory-checks.md` |
| verification block | nine commands that no longer exist, three dangling `verify-slot` forms, `store-kit.py`, stale comments ("runs the self-test too", "skip the self-test (rare)") | the real command set: the one entry point first, then the standalone money gates, the deliberate reports, the approval boundary, the release phase, and the offline suites |
| the self-test paragraph | presented `selftest.mjs` as the regression mechanism | the core's inline fixtures, with the manner of their failure recorded in `measurements.md` |

**And a standing correction to the change's own earlier claim.** Two regions of `AGENTS.md` used to
assert enforcement that no program provided: the self-learning table named the fact ledger's reader as
a script that is deleted, and §7 of this file recorded the loss. Both are now written as what is true —
the facts gate exists and is enforced, the lessons file is a record, and the fixtures are where a
sensor's ability to fail is demonstrated. §8.4 is the evidence for why "a suite exists" is not the same
claim as "a suite runs", and it was found by being run.

### 8.6 The instruction files, and two scratch files that were never meant to be in the tree

The sweep in §8.1 covered `AGENTS.md`, `README.md` and `docs/`, but the **rule files an agent loads
every session** were not in it, and they were still telling a reader to run programs this change
deleted. Found by the repo-wide search, not by reading:

| File | Was | Now |
|---|---|---|
| `.cursor/rules/verification-discipline.mdc` (×2) | "`node scripts/selftest.mjs` and `node scripts/verify.mjs` are evidence"; "add a check to `scripts/selftest.mjs`" | `node scripts/verify.mjs`, with the inline fixture set of the owning `scripts/core/*.mjs` module named as what makes a check falsifiable |
| `.cursor/commands/ship.md` | "adding a check to `scripts/selftest.mjs`" | the same retarget |
| `.cursor/rules/differentiator-contract.mdc` | "`node scripts/selftest.mjs` drives the real checker" | `node scripts/verify.mjs --slot <slug>` |
| `.cursor/rules/build-speed.mdc` | "`scripts/selftest.mjs` asserts this contract with a fake process" | `build.fixtures` in `scripts/core/build.mjs`, run by `verify.mjs` |
| `.cursor/rules/release-gate.mdc` (×3) | `python scripts/store-kit.py` in the rendering modes, in the approval-caller list, and in the release ladder | `python scripts/store/render.py` |
| `.cursor/rules/parallel-execution.mdc` | let `store-kit.py` compose the asset set | `scripts/store/render.py` |
| `.cursor/rules/visual-identity.mdc` | "`store-kit.py` refuses to render" | `scripts/store/render.py` |
| `.cursor/rules/project-conventions.mdc` (×2) | `DEFAULT_CONTACT_EMAIL` defined in `scripts/store-kit.py`; the reserved-host report from `scripts/store-kit.py` | `scripts/store/model.py` |
| `README.md` (×2) | the phase-2 pipeline row and the approval sentence | `store/render.py` |
| `scripts/core/build.mjs` | a comment naming the deleted `selftest.mjs` leftover-reference probe | the probe named generically |
| `scripts/test-checklist-products.py` | "and `scripts/selftest.mjs` reads that exit code" | the verifier reads it |

**Two files that were never supposed to be tracked.** `_tca.txt` (8,728 B) and `_obs.txt`
(23,904 B) sat at the repository root, committed as binaries by the pre-change revision `79c21a4`.
They are not factory files: `_tca.txt` is the stdout of `python scripts/test-console-answers.py`
redirected with `*> _tca.txt` (its own line 21 is that command), and `_obs.txt` is the dry-run output
of the console observation applier. Both name programs this change deleted, both were produced by a
shell redirect that should have pointed at `.kiln/` (gitignored), and neither is read by anything.
Deleted with `git rm`.

The class is the same one §8.1 named: a rename is not finished when the code is renamed, it is
finished when the search finds nothing that still claims otherwise. What is left after this sweep is
only **past-tense provenance** — "the retired `store-kit.py`", "the pre-split name raised", "since
`selftest.mjs` was deleted nothing asserts this" — which is a record of a deletion, not an
instruction to run something that is gone.
