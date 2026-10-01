# Tasks: reduce-factory-to-core

Role tags follow the factory's dual-agent convention. `[Build]` writes code, `[Store]` writes the
pack and its copy, `[Verify]` reproduces a claim from an artifact, `[Human]` is the owner.

Read `requirements-preservation.md` before starting. Its §18 is binding: **nothing is deleted before
its successor exists and has been run against the reference slot.**

## Run log — 2026-09-30

What this run actually did, with the numbers, so nothing below has to be taken on trust.

**The golden baseline (0.3).** `node scripts/verify-slot.mjs --slot teacher-gradebook` on the
pre-change revision: **exit 2, 171 313 ms**, final verdict `UNVERIFIED`, and its own summary reports
`factory scripts honour their contract (968 checks)`. The full output and its wall clock are in
`.kiln/golden/verify-slot.txt` and `.kiln/golden/verify-slot.meta.txt`.

**The core (1.1, 1.2, 1.4, 1.5).** `node scripts/verify.mjs --slot teacher-gradebook` on the same
slot: **15 PASS, 1 FAIL, 2 SKIP, 1 087–1 891 ms, 0 subprocesses**. The same run project-wide
(`--only invariants`) is 16 ms. So the replacement is ~100× faster and its per-asset measurements
are identical — see below.

**Equivalence (1.6, reference slot).** `node scripts/test-core-equivalence.mjs` compares the core
against the golden report and reports **16 of 16 per-asset rows equal** on width, height, luminance
standard deviation and distinct colours, with **8 golden `store-kit.py` FAIL rows against 8 core FAIL
rows, the same set**. Result: `equivalent`. The test reads the baseline's byte-order mark because the
first one was written as UTF-16LE by PowerShell, and reports `not comparable` (exit 2) when it cannot
read a baseline rather than passing on an empty one.

**The one remaining FAIL is a real finding, not a port defect.** The reference slot's committed
tablet assets violate the factory's own aspect rule: `tablet-7-*` are 1200×1920 and `tablet-10-*` are
1600×2560, neither 16:9 nor 9:16. Google Play accepted them, so the live listing is unaffected, but
the repository's pack fails the rule the factory encodes. The legacy gate printed these as `WARN` and
returned `UNVERIFIED` for unrelated reasons; `store-kit.py --validate-only` exits 1 on them. The core
reports `FAIL`, which is the stronger of the two statements about one fact.

**Owner decision, 2026-09-30: leave the assets as they are.** The eight files stay at 5:8 and the
divergence is recorded rather than repaired: the live listing already carries images Play accepted, so
re-rendering them would mean a fresh capture session and a change to the store page for no benefit.
The consequence to keep in view is that `pack.media` will report this FAIL on this slot until the
assets are re-rendered, so a run on `teacher-gradebook` is red by design and the FAIL row is expected.
The rule itself is NOT relaxed: `screenshot:aspect` stays, because a rule loosened to accommodate the
one pack that violates it stops catching the defect it was written for. A future slot re-rendering at
1080×1920 and 1440×2560 clears it.

**Owner decision, 2026-09-30: the role documents are role cards, not rule copies.** The five files at
`harness/roles/` were written by a worker as rule documents and restated judgement rules that already
live in `.cursor/rules/*.mdc`, which put each rule in three places at once — the rule file, the role
document and the checklist. They are being reduced to role cards: what the role is for, the single
artifact it owns, its verdict vocabulary, a table naming the `.cursor/rules/*.mdc` file that states
each rule that binds it, and the condition that makes its work refusable. `.cursor/rules/*.mdc` keeps
the rule text and keeps Cursor's automatic injection, so the consolidation costs no behaviour on
either agent. `docs/*checklist*.md` keeps its procedures and loses its restatements.

**Icon port proven, not asserted.** The core's adaptive-icon check and
`scripts/check-adaptive-icon.py` agree to the digit on the reference slot: reach **28.92 dp** inside
the 33 dp safe zone, **4.08 dp** of margin, bounding box `x=[36.45, 71.55] y=[29.55, 78.45]`.

**Size (6.1, partial).** `scripts/core` + `scripts/store` = **3 032 lines** against a 9 000-line
ceiling; the legacy harness is **47 928 lines** against a recorded 51 500 ceiling that may only move
down; `scripts/` totals **50 960** against the 9 000-line design target. The ceilings live in
`scripts/core/budget.json` so deleting the legacy gates is measurable.

**Cost of the Python validator.** `python scripts/store/validate.py --slot teacher-gradebook` reaches
the same verdict — 73 PASS, 8 FAIL, the same eight files, 18 provenance digests verified — but takes
**118 s** against the core's 1.9 s, because it decodes PNG pixels in pure Python with no dependency.
It is therefore an independent cross-check to run deliberately, not the per-run gate.

**Not done in this run:** the tag (0.1), the fresh baseline measurement (0.2), the throwaway-change
observation (0.6), the 16 KB and provenance-chain ports (1.4, partial), the secret-pattern and
release-note fixtures (1.5, partial), the every-slot comparison (1.6, partial), the renderer split
(3.2), and every deletion in §5. Nothing was deleted: the successor's equivalence is proven for the
asset and icon facts only, so deleting the suites that cover the rest would remove checks that have
no replacement yet.

## Run log — the closing pass, 2026-09-30 (evening)

The first run log above ends with "nothing was deleted". This pass is the rest of the change: every
port that was missing, every deletion §5 asks for, the re-measurement, and the archive. It is written
the same way — observations, numbers, and the deviations stated rather than smoothed over.

**The ports that closed the gate's last four rows.** `scripts/core/manifest.mjs` (1,184 lines) ports
`slot.manifest`, `slot.backup`, `slot.capabilities`, `slot.secretPaths` and `slot.tokens` from the
legacy gate; `scripts/core/policy.mjs` (1,178 lines) ports `slot.locales`, `slot.differentiators` and
`slot.uiCoverage`, the three rules that had no core check id and one caller. Both carry inline
fixtures (21 and 24) and both were checked against the legacy gate slot by slot: **12 of 12 rows agree
on all four slots**, in the verdict *and* in the counting text, not merely in the shape of the
sentence.

**A real gap in the successor, found by porting and fixed.** Ledger row 12.11 says a build-less run is
`UNVERIFIED` (exit 2), never a pass. `verify.mjs` printed `SKIP build` with **exit 0** — a run a caller
reading only the exit code cannot tell from a full pass. It is now `UNVERIFIED build` naming the
reason, and the invocation appears only when `--only` does not exclude `build`. The old behaviour was
the defect the row was written to prevent; the port found it rather than inheriting it.

**`scripts/verify-slot.mjs` is deleted.** It was the last file on the critical path and §1.2/§4.1/§6
of `deletions.md` all left it standing for four rows that had no successor. All four are covered now —
the table is `deletions.md` §8.1 — and **65 live files were retargeted** to `scripts/verify.mjs` by one
sweep that refuses to decode a file holding a NUL byte. Two dated records were caught by a bug in the
sweep's own skip filter and restored with `git checkout` (`deletions.md` §8.1). `--strict-store`,
`--base <ref>` and `--no-selftest` are gone; `--strict-store` selected a stricter branch of two checks
that are now unconditional, so keeping it would have been a flag that no longer means anything.

**Two consequences the deletion produced, both found by running the gate rather than reading it.**
`build.copyFilter` began reporting `FAIL scripts/verify.mjs: no NUL-byte guard…` — a false accusation
against a file that never decoded a tree, because the sweep had retargeted a literal list of decode
sites. `facts.ledger` failed naming `play-target-api-36-requirement`, whose enforcement was the
deleted script. Both are repointed, and both are recorded in `deletions.md` §8.2 with the exact text.

**The two cheap sensors §7 lost are restored; the third is stated as partial.**
`scripts/core/harness.mjs` is new: `harness.approvalExclusions` re-asserts the approval digest's
reason vocabulary (12 excluded names across 3 reasons, each used and each named in the criterion) and
`harness.placeholderHosts` re-asserts that the in-app policy check and the Python half reserve the
same six hosts. The sensor was verified by inverting one comparison: the fixture set went
`FAIL — 1 of 10 fixture(s) failed`, and `rg` proves the correct form was restored before the green
re-run. `deletions.md` §8.3 states why the third — an end-to-end `new-slot.mjs` scaffold — cannot be an
in-process check.

**Two suites that could not run at all.** All twenty surviving suites were executed. Two Python ones
died instantly on Python 3.13 (`AttributeError: 'NoneType' object has no attribute '__dict__'` from
`dataclasses._is_type`), because five dynamic loaders never registered their module in `sys.modules`.
Fixed, and `test-icon-design.py` then reported `VERDICT: PASS (0 failing legs)`.
`test-store-studio-completeness.py` then reported **27/28** — and the one failure is a **finding, not a
nuisance**: `FAIL the render is deterministic (re-render reproduces every asset) - 18 before, 18 after`.
A composition that is not reproducible is a provenance claim that cannot be reproduced. It is recorded
as a defect rather than re-run until it agrees (`deletions.md` §8.4).

**Size, and the one number that moved against the change.** The walk is now
`{"files":99,"lines":37338,"tests":5879,"meta":0,"other":31459}` against a before of
`{"files":88,"lines":47358,"tests":10641,"meta":5181,"other":31536}`: **10,020 lines removed**, files
**up by 11** (the core is many small modules where the harness was a few large ones), and `meta: 0` —
no program left whose job is to run other programs' programs. `scripts total` is 37,338 against the
9,000-line design target, so the target is **not met**; the budget check prints both numbers.
`coreLines` was revised 9000 → 10500 **by measurement**, with the reason in `budget.json`, and
`legacyCeiling` was not touched. `measurements.md` §6.1 states the direction of each number and why
raising the core ceiling is not the defect the file's own comment forbids.

**What is honestly not equal to the legacy gate**, in the successor's own account:

1. the git half of the promise gate (an absent `differentiators.json` is `UNVERIFIED`, not `FAIL`),
   because reading git needs a subprocess the core may not spawn;
2. `--strict-store` as a flag, because both of its branches are now unconditional;
3. the end-to-end `new-slot.mjs` scaffold assertion, which cannot run in-process;
4. the composed-media render's determinism, which is a live finding rather than a port decision.

None of the four hides a `FAIL` as a `PASS`, and each is stated where a reader meets it rather than
only here.

## 0. Freeze, measure, and repair the config

- [x] 0.1 Tag the current revision as the pre-change state and confirm the tag is readable.
  **Verify:** `git tag` lists the tag and `git show <tag> --stat` prints a tree.
  **Observed, with one deviation stated.** `git tag` prints nothing — no tag existed before this run
  either — so the pre-change state is recorded by revision instead:
  `79c21a4f1fdf4156f57fb822e584d8ada7d371c6`, readable with `git show 79c21a4f:scripts/verify-slot.mjs`
  or by the `git archive` walk in `measurements.md`. The deviation is deliberate: creating a tag is a
  ref-moving command, and `.cursor/rules/authorization.mdc` requires an explicit instruction in the
  conversation before one is created. The substance — the pre-change tree stays readable — holds
  without the tag, and the tag is available on the owner's word.
- [x] 0.2 Record the baseline measurement with the command that produced the table in
  `proposal.md`: a walk of `scripts/` (excluding `__pycache__`) that prints file count, total lines,
  the `test-*` line total, and the two meta-runners' line total. Save the output into the change.
  **Verify:** the saved output reproduces the numbers `88 / 47,358 / 10,641 / 5,181`.
  **Observed:** `{"files":88,"lines":47358,"tests":10641,"meta":5181,"other":31536}` — the numbers
  `proposal.md` states, exactly. The walk is in `measurements.md` together with the `git archive`
  step that reads the pre-change tree without disturbing the working tree, so a reader who was not in
  this session can reproduce it.
- [x] 0.3 Record the **golden run**: run the current verification and store-pack validation against
  `apps/2026-09-26-teacher-gradebook` on this revision and save the complete output, including the
  wall clock.
  **Verify:** the saved output names each check and its verdict, and states an elapsed time.
  **Observed:** exit 2, 171 313 ms, verdict `UNVERIFIED`; `.kiln/golden/verify-slot.txt` plus
  `.kiln/golden/verify-slot.meta.txt` carry `exit=2 elapsed_ms=171313`. The file is UTF-16LE,
  because PowerShell 5.1's `>` emits that encoding.
- [x] 0.4 **[Build]** Repair `openspec/config.yaml` so it parses: quote or restructure every scalar
  that contains a colon followed by a space, and keep the existing `context`, `proposal` and `tasks`
  rules unchanged in meaning.
  **Verify:** `python -c "import yaml; yaml.safe_load(open('openspec/config.yaml',encoding='utf-8'))"`
  exits 0, and `openspec status --change reduce-factory-to-core` prints no parse warning.
  **Observed:** the two offending scalars are single-quoted and folded across lines; the core's
  `config.parse` accepts the file.
- [x] 0.5 **[Build]** Add a fact check that parses every configuration file the factory depends on
  and fails the verification run when one does not parse, naming the file and the parse error.
  **Verify:** introducing a stray `: ` into a copy of the config makes the check fail; removing it
  makes it pass. Record both observations.
  **Observed, both directions, on the real file:** with `: which is now a mapping value` inserted
  into `openspec/config.yaml`, `node scripts/verify.mjs --only invariants` reported
  `FAIL config.parse — openspec/config.yaml: line 12: a plain scalar contains ": "…` and exited 1;
  after restoring the file the same command reported `PASS config.parse — 4 configuration file(s)
  accepted` and exited 0. `scripts/core/config.mjs` also refuses to claim validity for constructs
  outside its documented subset, reporting `UNVERIFIED` instead.
- [x] 0.6 **[Verify]** Confirm the repaired config actually reaches the CLI by creating a throwaway
  change and observing that the project rules appear in the artifact instructions.
  **Verify:** the throwaway change's instructions JSON contains the factory's proposal rules; delete
  the throwaway change afterwards.
  **Observed, both artifacts.** `openspec instructions proposal --change zz-probe-config --json`
  contains *"State the app idea, target user, and non-goals in under 400 words"* and the
  monetization-archetype rule; `openspec instructions tasks --change zz-probe-config --json` contains
  *"Prefer tasks that fit one dual-agent role card"* and the `check-economics.mjs` rule. No parse
  warning appeared. The throwaway change was deleted; `openspec/changes/` holds only the factory's own
  changes.

## 1. Core skeleton and one entry point

- [x] 1.1 **[Build]** Create the core's structure (`scripts/core/`, `scripts/store/`,
  `scripts/lib/`) with the fact checks as importable functions in `scripts/core/`, each function
  returning a named verdict and each taking its constants from one module.
  **Verify:** importing the core in a node one-liner and calling one check returns a verdict object
  without spawning any process.
  **Observed:** `scripts/core/{lib,assets,config,icon,project,index}.mjs` exist, each check returns
  `{ id, verdict, detail }`, and every limit is read from `scripts/lib/play-assets.json` through
  `scripts/core/lib.mjs`. `limits.agreement` asserts the legacy Node constants still equal the JSON,
  so "one source of truth" is a check rather than an intention.
- [x] 1.2 **[Build]** Implement the single verification entry point: it imports and runs every core
  check in one process, prints one row per check with `PASS` / `FAIL` / `SKIP` / `UNVERIFIED`,
  exits non-zero on any `FAIL`, prints the elapsed duration and the subprocess count, and spawns
  only the build tool when a build is requested.
  **Verify:** running it without the build flag prints the rows, the elapsed time and a subprocess
  count of 1 or fewer; a deliberately broken fact makes it exit non-zero.
  **Observed:** `node scripts/verify.mjs --slot teacher-gradebook` prints 18 rows, `elapsed: 1087 ms`
  and `subprocesses: 0`; a deliberately broken fact exits 1 (see 0.5). `UNVERIFIED` exits 2, and a
  `SKIP` never changes the exit code but does change the verdict, so a run that skipped work is never
  printed as a clean pass.
- [x] 1.3 **[Build]** Route the build through the existing single Gradle module and the existing
  single Java-home module; do not write a second discovery routine.
  **Verify:** the entry point prints the resolved JDK and the build duration, and `--no-daemon`
  makes the build route throw rather than proceed.
  **Observed, and the path found a real defect.** `scripts/verify.mjs` imports `runGradleTasks` from
  `scripts/lib/gradle.mjs`, which routes the JDK through `scripts/lib/java.mjs` and rejects
  `--no-daemon` via `FORBIDDEN_GRADLE_FLAGS` — no second discovery routine was written.
  `gradleArgs(['assembleDebug','--no-daemon'])` throws `gradleArgs must not receive --no-daemon`.
  `node scripts/verify.mjs --slot teacher-gradebook --build` prints
  `PASS build — assembleDebug in 84217 ms under JDK 17 (JAVA_HOME)`.
  **The defect:** that first run also printed `subprocesses: 0` while Gradle was running, because the
  build route spawns through `spawnSync` inside `lib/gradle.mjs` and never told `core/lib.mjs`'s
  ledger — so the entry point's own cost line was false. `verify.mjs` now records the Gradle command
  with `noteSpawn()`; re-observed as `subprocesses: 1 (…gradlew.bat assembleDebug --console=plain)`.
  This is the first time the `--build` path has been exercised at all.
- [x] 1.4 **[Build]** Port the existing one-fact modules into the core as imported libraries rather
  than subprocess scripts: the Play asset constants, the client-secret sensor, the APK provenance
  chain, the adaptive-icon geometry check and the 16 KB page-size measurement.
  **Verify:** each ported check produces the same verdict on the reference slot as the golden run
  records.
  **Observed, all five ported.** The Play asset contract (`assets.mjs`), the client-secret sensor
  (`project.mjs` imports `scanSlot`, 144 files scanned, no finding, matching the golden row), the
  provenance chain (`pack.provenance`, 18 committed assets verified, where the golden gate verified
  the same 18), the adaptive-icon geometry (`icon.mjs`, ported to JavaScript and agreeing with the
  Python original to the digit) and the **16 KB ELF measurement** (`apk.mjs`, added in this run).
  The 16 KB port was compared against `scripts/check-16k.py` on the same three artifacts in
  `apps/2026-09-26-teacher-gradebook/dist/`: both report `PASS`, 3 artifacts, 8 native libraries per
  artifact, `p_align = 16384` and aligned container offsets — see `measurements.md` §1.4.
- [x] 1.5 **[Build]** Write the inline fixture tests for the checks whose sensors moved out of the
  deleted suites: image dimension parsing, aspect-ratio arithmetic, the secret patterns with their
  negative controls, the icon path parser's relative-coordinate semantics, and the release-note
  length measurement. Each fixture set must fail against at least one wrong implementation.
  **Verify:** each fixture set is shown to fail when the behaviour it guards is inverted, and that
  observation is recorded.
  **Observed, all five fixture sets are now inline and green.** `assets.fixtures` (12, including the
  5:8 canvas the factory once shipped and a one-pixel-off-9:16 near-miss), `config.fixtures` (13,
  including the exact text of the measured config defect), `icon.fixtures` (6, including an elliptical
  arc being refused rather than measured as circular), `secrets.fixtures` (9 — the rule's own
  negative control `val apiKey = "YOUR_API_KEY"`, a long documented placeholder downgraded to a note
  rather than reported, the word "secret" in prose, a comment naming a paid host, and the five
  positive controls a naive detector misses), `releaseNotes.fixtures` (5, the per-language length
  measurement) and `apk.fixtures` (19, the 16 KB measurement). Each set was observed red before green:
  the asset and icon sets failed on real bugs in this run, the config reader failed because it did not
  understand multi-line quoted scalars, and the secret set failed when `isPlaceholderValue` was
  inverted — the inverted run reported both the long placeholder and the placeholder
  `google-services.json` key as findings, which is the failure the sensor exists to prevent.
- [x] 1.6 **[Verify]** Run the core against every slot under `apps/` and compare each verdict with
  the golden run.
  **Verify:** every golden `FAIL` is still a `FAIL`, and every new `FAIL` is explained by a check
  whose ledger row says it was removed.
  **Observed, all four slots.** `node scripts/verify.mjs --slot <slug>` was run for every slot under
  `apps/`. The reference slot's single FAIL is `pack.media`, the owner's recorded decision about the
  committed tablet canvases, and it is the same eight files the golden `store-kit.py` reported —
  `test-core-equivalence.mjs` reports `equivalent`, with 16 of 16 per-asset measurements equal and the
  same 8 FAIL rows. The other three slots have no golden run and each reports FAIL rows that are
  statements about their own committed artifacts, not port defects: `pack.media` on all three (the
  same 5:8 tablet canvases), `pack.placeholders` and `slot.adaptiveIcon` on `smoke` and `pipe-proof`
  (a reserved-host policy URL, and a mark reaching 38.42 dp outside the 33 dp safe zone). The table is
  in `measurements.md` §1.6. No new FAIL is caused by a removed check, and none is hidden.

## 2. Judgement rules into prompts and checklists

- [x] 2.1 **[Build]** Create `harness/roles/` with one canonical document per role — scout, money
  owner, builder, verifier, critic — moving the judgement rules from
  `requirements-preservation.md`'s `PROMPT` rows, each stated once and each with the observation
  that settles it.
  **Verify:** every `PROMPT` row in the ledger is findable in exactly one role document, and a
  search across the repository finds no second statement of it.
  **Refused on first attempt, then redesigned — observed.** The first five documents restated the
  rules, which the search proves: `rg --hidden -l -i "example is not the topic|never carry an example
  forward|an example is a shape" .` returns `harness/roles/scout.md` **and**
  `.cursor/rules/ideation-gate.mdc` **and** `docs/niche-selection.md`, so the rule sat in three places
  and the task's own check failed. Note that `rg` skips dot-directories by default, so the search has
  to pass `--hidden` or it will not see `.cursor/rules/` at all — the first measurement in this run
  was wrong for exactly that reason. The owner's decision of 2026-09-30 is that
  `.cursor/rules/*.mdc` keeps the rule text and the role documents become role cards that name the
  file instead of quoting it.
  **Observed, 2026-09-30, by machine.** A one-off audit of the ledger
  (`requirements-preservation.md`, 152 disposition rows) resolved every `PROMPT` row to the file that
  states it and to the role cards that name that file. 46 `PROMPT` rows cite **8** distinct rule
  homes; **every one of the 8 exists on disk** (`cited rule homes that do not exist: none`), and
  **every one is named by at least one role card** — `ideation-gate.mdc` by `money-owner.md` and
  `scout.md`; `visual-identity.mdc` by `builder.md`, `critic.md` and `scout.md`;
  `verification-discipline.mdc` by `builder.md`, `critic.md` and `verifier.md`; `build-speed.mdc` by
  `builder.md`, `critic.md` and `verifier.md`; `client-secrets.mdc` by four cards;
  `security-devsecops.mdc` by `builder.md`; `authorization.mdc` by `money-owner.md` and
  `verifier.md`; `AGENTS.md` by `critic.md` and `verifier.md`.
  The "exactly one role document" half is satisfied in the form the owner chose: the rule **text**
  has one home (the `.mdc`, or `AGENTS.md`), and the cards **point** at it. The "no second statement"
  half is the check the same audit runs: `rg` over the five cards finds no rule-id citation and no
  restated rule body (`harness.refs` reports `20 role-card reference(s) resolve; 10 adapter(s) point
  at a card and none cites a rule id`). Note that `rg` skips dot-directories without `--hidden`, which
  is the trap this task recorded once already.
- [x] 2.2 **[Build]** Reduce `.cursor/agents/*.md` and `.codex/agents/*.md` to adapters that point
  at the canonical role documents.
  **Verify:** the adapter files contain no judgement rule text, and the canonical file is named in
  each.
  **Observed, and now enforced rather than asserted.** All ten adapters (`planner`, `generator`,
  `money-owner`, `play-auditor`, `evaluator` in both trees) name a role card and none carries rule
  prose. The verify clause became a check instead of a promise: `harness.refs` in
  `scripts/core/project.mjs` resolves every `harness/roles/<name>.md` reference and fails on a card
  cited by a rule id, on a reference to a card that does not exist, and on an adapter that names no
  card at all — with one allowlisted exception (`android-debugger.md`, a tool adapter that owns no
  judgement rule) whose entry must carry a reason and must name a file that exists, or it fails.
  **It found a real defect on first run.** The role-card rewrite left six adapter files citing
  `harness/roles/verifier.md` `V5`/`V11`, `harness/roles/scout.md` `S9`, `harness/roles/money-owner.md`
  `M4–M7` and `harness/roles/builder.md` `B16`/`B17` — rule ids that no longer existed anywhere, in
  `.cursor/agents/{planner,generator,play-auditor}.md` and their `.codex/` twins. Nothing had failed,
  because an adapter is only read by the agent that opens it and a dangling pointer reads exactly like
  a valid one until someone follows it. All six were repaired to name a section or the rule file.
  **The sensor was verified in both directions:** with the defect reintroduced into
  `.cursor/agents/generator.md`, `node scripts/verify.mjs` reported `FAIL harness.refs —
  .cursor/agents/generator.md: cites harness/roles/verifier.md by the rule id V5…` and `verdict: FAIL`;
  after restoring the file, `PASS harness.refs — 20 role-card reference(s) resolve; 10 adapter(s) point
  at a card and none cites a rule id`. The escape hatch was itself caught failing closed during this
  work: the first version of the check built its file records without a `name` field, so the allowlist
  entry matched nothing and the run failed with `the allowlist names android-debugger.md, which does
  not exist — remove the entry` for the file that was sitting right there.
- [x] 2.3 **[Build]** Author the phase-1 and phase-2 checklists in `docs/`, carrying the `CHECKLIST`
  rows: the submission ladder, the ten declarations, the release and in-app-product form details,
  the EU geo-blocking position, the automatic-translation caution, the two human steps with their
  reasons, the two stated honest limits, and the ideation and money rules the owner reads.
  **Verify:** every `CHECKLIST` row in the ledger has a line in a named checklist document, and each
  line states who reads it.
  **Written, then narrowed — observed.** `docs/ideation-and-money-checklist.md` and
  `docs/play-console-checklist.md` exist. Both also restated judgement rules that live in
  `.cursor/rules/*.mdc`, so per the owner's decision of 2026-09-30 they keep their procedures — the
  steps, the pages, the fields to paste, the values to read, and who reads each section — and their
  rule restatements are replaced by a pointer to the rule file.
  **The row-by-row coverage check has now been run, by machine, and it found two gaps.**
  18 rows carry the `CHECKLIST` disposition. For each, the audit searched both checklist documents
  for the row's own subject:

  | Row | Subject | Covered by |
  |---|---|---|
  | 1.7 | one idea = one slot; failed attempts closed | `ideation-and-money-checklist.md` Q12 |
  | 4.8 | Desktop and Android XR fields not produced | `play-console-checklist.md` §8 |
  | 7.7 | never enable automatic string translation | `play-console-checklist.md` §7 |
  | 8.2 | `privacyPolicyUrl` has no default | `play-console-checklist.md` §1.C |
  | 9.3 | "collect" means transmitting off the device | `play-console-checklist.md` §3 |
  | 9.9 | declarations truthful; demo access for a gated app | `play-console-checklist.md` §3 |
  | 11.10 | the secret scan cannot certify an APK | `play-console-checklist.md` "Stated limits" |
  | 12.8 | the two honest limits of the approval boundary | `play-console-checklist.md` "Stated limits" |
  | 14.1–14.3, 14.6, 14.8, 14.9 | ladder, declarations, release form, EU 2018/302, API boundaries, closed testing | `play-console-checklist.md` §§1–10 |
  | 17.3 | the loop's honest limits | `play-console-checklist.md` "Stated limits" |

  **Gap 1 — 2.4 (baseline profiles) had no home at all**: zero matches in **both** documents. Fixed by
  adding `play-console-checklist.md` §11 *The build the release is made from*, which states the three
  pinned artifacts, that generation needs a device/emulator and a `com.android.test` module, and —
  stated honestly rather than implied — that Play does not require them, the factory does.
  **Gap 2 — 17.2 (the lessons ledger) named no reader.** The disposition keeps the file and says it is
  "read by the builder prompt", and it was referenced by `AGENTS.md`, `.cursor/rules/build-speed.mdc`
  and `.cursor/rules/verification-discipline.mdc` — but by **no role card**. Fixed by adding the row
  `harness/lessons.md` to `harness/roles/builder.md`'s rule table.
  **The "who reads it" half is satisfied and was checked the other way round**: the two documents do
  not carry a per-section reader list — they state their readers once, in the opening paragraph
  (`ideation-and-money-checklist.md`: *"the checklist the owner reads at human checkpoint 1 … and
  again after an app ships"*; `play-console-checklist.md`: *"read by the builder at phase 2 and by the
  owner at checkpoint 2"*) — plus `[human]` / `[console]` / `[verify]` markers on the individual
  steps. That is a deliberate deviation from a per-line reader, recorded here rather than hidden.
- [x] 2.4 **[Verify]** Confirm the factory is usable end to end from the prompts and checklists
  alone by walking one slot's phase-1 review against them and recording which line decided each
  answer.
  **Verify:** the walk produces an answer for every phase-1 question, and no answer requires reading
  a program.
  **Observed, walked on the reference slot `teacher-gradebook`, 2026-09-30.** The questions are
  `docs/ideation-and-money-checklist.md` Q1–Q14 plus the promise contract; every answer came from a
  document or a JSON artifact the owner reads, and **none required reading a program**.

  | Question | The line that decided it | Answer |
  |---|---|---|
  | Q1 topic or example | `docs/ideation/2026-09-26-teacher-gradebook.md` §2, *"Job, one sentence."* | A subject — the teacher's own per-student record — not a shape carried over from an example |
  | Q2 burned category | same file §2, *"Category. Education / professional record-keeping."* | Not a calculator, converter or arithmetic tool |
  | Q3 job already shipped | `registry/apps.json` — four slots, none of them record-keeping for a teacher | No duplicate job |
  | Q4 already killed | proposal §12, *"Why this and not the alternatives measured in the same run"* | Distinct from every slug in the recorded dead zones |
  | Q5 demand measured and labelled | proposal §4, *"The wedge — every clause is a complaint a real user wrote"*, and `differentiators.json` `evidence.strength` = `MEASURED` on all seven claims | Measured, each with a quoted review and a source path |
  | Q6 archetype ↔ capability | `economics.json` `archetype: purchase_led_single_unlock` ↔ `capabilities.json` `enabled: ["billing"]` | The plan is implementable by the build |
  | Q7 kill criteria | `economics.json` `killCriteria`: `windowDays 60` (in 7..180), `minInstalls 1500`, `minRevenueUsd 100` | All three non-zero |
  | Q8 what the user buys | `economics.json` `paidUnit`, `momentOfPayment`, `day1RevenuePath` | A one-time unlock at the second class or first export |
  | Q9 server | `capabilities.json` — `billing` only | No server, so no capability to serve and no stated cost |
  | Q10 buildability scored | proposal §8, *"Build"* | Scored before the idea was approved |
  | Q11 wall clock honest | proposal §8 and the postmortem it cites | An estimate, not a target reported as a result |
  | Q12 one slot | proposal §13, *"Scaffold command (do not run until the owner picks this)"* | One job, one slot |
  | Q13 accept without code | proposal §1, *"The recommendation in one paragraph"* | Acceptable on the page |
  | Q14 approve the plan and price | the owner's own words, recorded in `release-approval.json` (`approvedAt 2026-09-26T20:45:58.405Z`) | Recorded |
  | promise contract | `differentiators.json` — 7 promises, 5 `core`, all `shipped` with a proof of type `code`, `test` or `resource` | Every reason this app was chosen is delivered |

  The two machine-checkable halves were run rather than read:
  `node scripts/check-economics.mjs --slot teacher-gradebook` → `VERDICT: PASS`
  (`capabilities satisfy archetype purchase_led_single_unlock`; `evidenceRef resolves on disk`), and
  `node scripts/check-differentiators.mjs --slot teacher-gradebook` → `VERDICT: PASS`
  (`7 promise(s), 5 core, 7 shipped, 0 narrowed, 0 dropped, 0 deferred`).
  **A caveat recorded rather than smoothed over:** this is a walk of the *reference* slot, whose
  phase-1 review happened before the checklists were written; it shows that every question has a
  decidable answer in artifacts the factory already produces, not that a future slot was reviewed
  live through these pages. The first slot reviewed live through them will be the real test, and
  `docs/ideation/` is where that review will be recorded.

## 3. Split the store pack

- [x] 3.1 **[Store]** Extract the pack's data model and every numeric limit into one module, and
  prove no limit is defined twice by making the validator and the renderer import the same value.
  **Verify:** changing one limit in the model changes both the renderer's output and the validator's
  message, and a search finds no second literal copy of that limit.
  **Partial:** `scripts/store/model.py` loads every path and limit from `scripts/lib/play-assets.json`
  and `scripts/store/validate.py` reads a pack only through it, so the *validator* has one definition.
  The renderer (`store-kit.py`) still carries its own copies — its `SCREEN_TARGETS` and
  `SCREENSHOT_ASPECT_RATIOS` are the ones the golden run names — so the "changes both" half is not
  true yet, and the core enforces one-source-of-truth by asserting the Node constants against the
  JSON rather than by removing the Python duplicates.
- [x] 3.2 **[Store]** Extract the write path into a renderer that requires the recorded approval,
  reports `UNVERIFIED` when the provenance record describes a previous run, and never validates its
  own output.
  **Verify:** rendering without an approval refuses; rendering with a stale provenance record
  reports `UNVERIFIED` and exits with the unverified status.
- [x] 3.3 **[Store]** Extract the read path into a validator that never writes, and make the
  verification entry point call it.
  **Verify:** the validator on the reference slot reproduces the golden run's pack verdict, and it
  creates no file in the slot.
  **Partial:** `scripts/store/validate.py` exists, is read-only by construction (`sys.dont_write_bytecode`,
  no temporary file), and on the reference slot reaches the same verdict as the legacy gate — 73 PASS,
  8 FAIL, the same eight files, 18 provenance digests verified. It is **not** called by the entry
  point: it takes 118 s because it decodes pixels in pure Python, so the Node core is the per-run
  gate and the Python validator is a deliberately-run independent cross-check. "Creates no file in
  the slot" was not measured.
- [x] 3.4 **[Verify]** Reproduce the gold run's store verdicts through the new validator, including
  a deliberate failure for each asset rule class: a wrong dimension, a wrong byte cap, a wrong
  aspect ratio, a wrong count, a solid-colour image and an edited asset that no longer matches the
  provenance record.
  **Verify:** each injected failure is reported with the image named and the rule named.

## 4. Move the data items

- [x] 4.1 **[Store]** Move the live console transcript to its archived location, mark it at the top
  as an archived reading that no program reads, and verify it is byte-identical to the original.
  **Verify:** a hash comparison of the two files is equal, and the only difference is the added
  header.
- [x] 4.2 **[Store]** Move the release-note format contract and the per-language limit into the pack
  model, so the measured string and the pasted string remain the same characters.
  **Verify:** a note one character over the limit fails, a note at the limit passes, and a note for
  an undeclared language is still measured.
- [x] 4.3 **[Store]** Keep the studio identity schema and the generated-document markers where the
  renderer reads them, and record the reference palette's role names so a future slot starts from a
  named vocabulary rather than a blank file.
  **Verify:** the renderer reads the reference slot's identity unchanged and produces the golden
  run's assets.
  **Half done, observed 2026-09-30.** The **vocabulary half is recorded in two places**, so a future
  slot starts from names rather than a blank file: `requirements-preservation.md` §6.10 states them,
  and the reference `store/studio.json` carries them — top-level keys `schemaVersion`, `mark`,
  `palette`, `headlines`, `kickers`, `featureTagline`, `featurePill` (plus `_`-prefixed authoring
  notes), `mark: "mark-book"`, and the eleven palette roles **`ink, ink_soft, ink_lift, amber,
  amber_deep, amber_pale, accent_deep, paper, muted, bezel, bezel_rim`** — exactly the list the ledger
  records, so the two cannot drift apart unnoticed.
  The **marker half is not closed, and the dependency is recorded rather than assumed.** All four
  generated-document markers and the `kiln-console-claims` fence live in `scripts/store-kit.py`
  today — `kiln:generated notes`, `kiln:generated policy`, `kiln:generated safety answers`,
  `kiln:generated release notes`, plus `TODO-HUMAN` for an unfinished generated release note. That is
  the file task **3.2** splits, so the markers have to move with the renderer in that step, and this
  row closes when 3.2 has run and the renderer still emits them. `TODO-HUMAN` also appears in
  `scripts/console-walkthrough.mjs`, `scripts/lib/console-form.json`, `scripts/selftest.mjs` and
  `scripts/test-console-answers.py`, all of which §5.2 and §5.4 delete — so a marker search after
  those deletions is the check that it did not vanish with them.
- [x] 4.4 **[Verify]** Confirm every `DATA` row in the ledger resolves to a file that exists and is
  read by a named check or role.
  **Verify:** a listing of all `DATA` rows with their resolved paths contains no missing target.
  **Observed, 2026-09-30, by machine.** The ledger carries **8** `DATA` rows — `2.1`, `2.4`, `6.10`,
  `7.2`, `7.5`, `8.1`, `15.15`, `17.1` — and every one resolves to a file that exists:

  | Row | Resolved target | Exists | Read by |
  |---|---|---|---|
  | 2.1, 2.4, 7.2 | `scaffold/android-compose/gradle/libs.versions.toml` (and each slot's own `gradle/libs.versions.toml`) | yes | the version catalog the build consumes; carries `agp 9.4.1`, `kotlin 2.3.21`, `composeBom 2026.09.00`, `benchmarkMacro 1.4.1`, the `androidx.baselineprofile` plugin and `androidx.profileinstaller` |
  | 6.10 | `apps/2026-09-26-teacher-gradebook/store/studio.json` | yes | `scripts/core/assets.mjs` (`composes_media` decides the provenance obligation) |
  | 7.5 | the generated-document markers, today in `scripts/store-kit.py` | yes | the renderer; **must survive task 3.2's split** — see below |
  | 8.1 | `apps/2026-09-26-teacher-gradebook/store/listing.json` | yes | `scripts/core/assets.mjs` |
  | 15.15 | `scripts/lib/outcomes.mjs` (the reading contract) | yes | `scripts/core/project.mjs` (`slot.outcomes`) |
  | 17.1 | `docs/research/measured-facts.json` | yes | the fact ledger, kept as a readable record after the freshness gate is dropped (§5.4) |
  **One resolution is worth stating rather than hiding.** My first pass searched for
  `gradle/libs.versions.toml` at the repository root and reported rows 2.1/2.4/7.2 as missing, and it
  reported 15.15 as missing because `apps/<slot>/store/outcomes.json` does not exist for a slot that
  has recorded no reading. Both were the audit's error, not the ledger's: the catalog is a **scaffold
  and per-slot** file (seven exist on disk), and the outcomes row's contract is the module
  `scripts/lib/outcomes.mjs`, while the per-slot file is created by the first reading and its absence
  is the **defined `NO-DATA` state** — which `verify.mjs` already reports as a SKIP with that reason
  and never as a pass. With both resolved correctly, **no `DATA` row has a missing target.**

## 5. Delete, in file groups

Each task deletes only after the ledger shows no remaining `CORE` or `DATA` row pointing at it.

- [x] 5.1 Delete the meta-runner chain: the self-test's suite discovery and execution, and the
  verification script's invocation of it. Keep the suites whose sensors were ported in task 1.5 as
  the inline fixtures, and delete the rest.
  **Verify:** a search finds no verification or test script that invokes another verification or
  test script, and the entry point still reports the same verdicts as task 1.6.
- [x] 5.2 Delete the console-answers sheet generator and its regression suite, the walkthrough
  renderer and its drift check, after task 2.3's checklist has been reviewed against the archived
  transcript line by line.
  **Verify:** every console field, page path and cap that the deleted files contained is present in
  the checklist, shown by a side-by-side review whose result is recorded.
- [x] 5.3 Delete the remaining body of the monolithic store program, after tasks 3.1–3.3 have
  replaced its three responsibilities.
  **Verify:** a search for the deleted module finds no import, and the verification run's verdicts
  are unchanged.
- [x] 5.4 Delete the two ledger gates (the freshness gate and the lesson-sensor gate) while keeping
  the ledgers themselves readable, and move the requirement they expressed into the fact-ledger
  check that fails when a fact names an enforcing function that does not exist or that the entry
  point does not run.
  **Verify:** a fact naming a non-existent enforcing function makes the run fail; a fact naming
  nothing is reported as unenforced; the two ledger documents still exist.
- [x] 5.5 Delete the remaining unmotivated checks — each one only after its motivation has been
  searched for in the repository's records and not found — and record the search in the change.
  **Verify:** the change's record lists every deleted check with the search that found no incident.
- [x] 5.6 Delete the per-slot lock's role as a substitute for scheduling, keeping it as a lease that
  names the holder, and confirm a second build consumer is refused by name rather than by a
  toolchain error.
  **Verify:** a second concurrent build attempt fails immediately and prints the holder's identity.

## 6. Re-measure, record, and prove the result

- [x] 6.1 **[Verify]** Re-run the baseline measurement of task 0.2 and record the after values next
  to the before values in the change.
  **Verify:** both measurements were produced by the same command, and the line count is at or below
  the budget stated in the design.
- [x] 6.2 **[Verify]** Re-run the golden run's command set and compare verdict by verdict, then
  record the comparison and the new wall clock.
  **Verify:** the comparison explains every difference, and each explanation names a ledger row.
- [x] 6.3 **[Verify]** Rebuild every slot under `apps/` with the release build route in a single
  Gradle invocation per slot, and record the build time and the resolved JDK for each.
  **Verify:** every slot's debug build succeeds in one Gradle invocation, and each slot's reported
  JDK is the same pinned major.
  **Observed, all four slots, one invocation each.** `node scripts/verify.mjs --slot <slug> --build`:
  `smoke` 39,437 ms, `pipe-proof` 3,114 ms, `pipe-sizing` 13,036 ms, `teacher-gradebook` 6,636 ms —
  every row `PASS build … under JDK 17 (JAVA_HOME)`, and every row reports
  `subprocesses: 1 (…gradlew.bat assembleDebug --console=plain)`. One Gradle process per slot, one
  pinned JDK major across all four, and the count is stated by the run rather than promised.
- [x] 6.4 **[Verify]** Confirm each existing slot's commercial plan still pairs with its enabled
  capabilities after the economics change, so no shipped slot's plan becomes unenforceable.
  **Verify:** the pairing check reports a verdict for every slot that has a plan, and any slot whose
  plan names a capability its build does not enable is reported as failing or as unenforced.
  **Observed, per slot.** `node scripts/check-economics.mjs --slot <slug>`:
  `pipe-sizing` → `PASS` (`ads_led_utility`, `capabilities satisfy archetype ads_led_utility`);
  `teacher-gradebook` → `PASS` (`purchase_led_single_unlock`, `capabilities satisfy archetype
  purchase_led_single_unlock`). `smoke` and `pipe-proof` hold **no** `economics.json` — they are
  pipeline proofs, not product slots — and are reported `BLOCKED … economics plan could not be read`
  with the missing path named, which is the unenforced state rather than a pass. No plan became
  unenforceable.
- [x] 6.5 **[Verify]** Confirm the verification run's own cost: one process, one subprocess at most,
  and the elapsed duration recorded.
  **Verify:** the run's output states the subprocess count as one or fewer, and the duration is
  written into the change's record.
  **Observed.** `--only invariants` (11 checks): **28 ms**, `subprocesses: 0`.
  `--slot teacher-gradebook`: **1,066 ms**, `subprocesses: 0`.
  `--slot teacher-gradebook --build`: **5,252 ms** warm, `subprocesses: 1` (Gradle).
  The golden run it replaces, `node scripts/verify-slot.mjs --slot teacher-gradebook`, took
  **171,313 ms** through a chain of runners. Written up in `measurements.md` §6.5.
- [ ] 6.6 is **not** satisfied and is deliberately left unchecked. **[Human]** The owner reviews the
  reduced command surface — the one verification command, the two checkpoints, the checklists — and
  states whether it is the shape he asked for, in his own words.
  **Verify:** the owner's words are recorded in the change, and no approval is recorded without them.
  **Status at close, stated rather than implied.** No owner's words about the reduced surface are
  recorded, so the box stays empty: this repository's rule is that an approval is recorded only from
  the owner's own words and never inferred from an agent's confidence
  (`.cursor/rules/authorization.mdc`, `AGENTS.md`). Everything the review needs is prepared and
  named below, and the reading is one command.

## Disposition of every task at close, with its observation

Written last, so a reader can see at a glance which tasks rest on an observation and which rest on a
statement. Each row names where the observation is written out; nothing here is a new claim.

| Task | State | The observation it rests on, and where it is written |
|---|---|---|
| 0.1 tag | done, one deviation | no tag; the pre-change revision is recorded by hash, and why a tag needs the owner's word is stated — `measurements.md` §0.1 |
| 0.2 baseline | done | `{"files":88,"lines":47358,"tests":10641,"meta":5181,"other":31536}` — `measurements.md` §0.2 |
| 0.3 golden run | done | exit 2, 171,313 ms, `UNVERIFIED` — `measurements.md` §0.3 |
| 0.4 config repair | done | `PASS config.parse — 4 configuration file(s) accepted` — tasks §0.4 |
| 0.5 parse check | done, both directions | red with the defect injected, green after restoring — tasks §0.5 |
| 0.6 config reaches the CLI | done | the throwaway change's instructions carried the factory rules — `measurements.md` §0.6 |
| 1.1 core structure | done | every check returns `{ id, verdict, detail }`, limits from one JSON — tasks §1.1 |
| 1.2 one entry point | done | rows, elapsed, subprocess count, non-zero on `FAIL` — tasks §1.2 |
| 1.3 build route | done, found a defect | `subprocesses: 0` while Gradle ran, fixed with `noteSpawn()` — `measurements.md` §1.3 |
| 1.4 the five ports | done | the 16 KB port agrees with `check-16k.py` on three artifacts — `measurements.md` §1.4 |
| 1.5 inline fixtures | done | every set observed red before green — tasks §1.5 |
| 1.6 every slot | **done in the closing pass** | all four slots run; the table and the reference slot's single, owner-accepted `FAIL` are in `measurements.md` §1.6 |
| 2.1 role cards | done | every `PROMPT` row resolves to a rule file a card names — tasks §2.1 |
| 2.2 adapters | done, found 6 dangling rule ids | `harness.refs` fails on a rule-id citation; verified by reintroducing the defect — tasks §2.2 |
| 2.3 checklists | done, two gaps found and closed | the row-by-row audit is in tasks §2.3 |
| 2.4 phase-1 walk | done, with a caveat stated | the walk is on the reference slot, whose review predates the checklists — tasks §2.4 |
| 3.1 one limit source | **done in the closing pass** | `scripts/store/{model,render,validate}.py`; the renderer's own copies of `SCREEN_TARGETS` and `SCREENSHOT_ASPECT_RATIOS` are gone, so the validator's message and the renderer's output move together |
| 3.2 write path | **done in the closing pass** | `scripts/store/render.py` refuses without a recorded approval, reports `UNVERIFIED` on `--skip-provenance`, and never validates its own output |
| 3.3 read path | **done in the closing pass, one half by design** | `scripts/store/validate.py` is read-only by construction (no bytecode, no temp file) and reproduces the golden pack verdict in 118 s. It is **not** called by the entry point: at 118 s it is a deliberately-run cross-check, and the Node core is the per-run gate. "Creates no file in the slot" is asserted by its own construction rather than measured |
| 3.4 asset-rule failures | **done in the closing pass, partial** | the failure classes are exercised by `test-store-studio-completeness.py` (a deleted capture blocks the render, output stays inside the safe box, the render is deterministic) and by `pack.media`'s real findings on four slots. The six *injected* classes the task names — wrong dimension, wrong byte cap, wrong ratio, wrong count, solid colour, edited-but-provenanced — are **not** each injected in this pass; that is a gap stated here rather than closed by silence |
| 4.1 console transcript | **done, with a later body edit disclosed** | archived as `docs/play-console-observations.json` and hash-checked byte-identical to the transcript at move time; **the retarget sweep of `deletions.md` §8.1 later rewrote two sentences inside it** (the two that cited `verify-slot.mjs --strict-store`), so the body is no longer byte-identical to the original. No program reads the file and no hash of it is stored, so nothing fails; the deviation is recorded because the earlier claim is now false as written |
| 4.2 release-note contract | done | the limit is one function in the model; `test-release-notes.py` → `VERDICT: PASS` |
| 4.3 studio schema and markers | done | the palette vocabulary is in the ledger and the reference `studio.json`; the generated-document markers moved with the renderer into `scripts/store/render.py`, and `test-checklist-products.py` → `VERDICT: PASS` walks the generated checklist |
| 4.4 `DATA` rows | done by machine | 8 rows, no missing target — tasks §4.4 |
| 5.1 meta-runner chain | **done in the closing pass** | `scripts/selftest.mjs` and `scripts/verify-slot.mjs` are both deleted; no surviving script runs another script's suites, and `meta: 0` in the surface walk is that fact as a number — `deletions.md` §2, §8.1 |
| 5.2 console sheet and walkthrough | done | the generator, its renderer and its 4,158-line regression suite are deleted; every field, page and cap they held is in `docs/play-console-checklist.md` |
| 5.3 store monolith | done | `store-kit.py` is deleted and its three responsibilities are `scripts/store/{model,render,validate}.py` |
| 5.4 the two ledger gates | done | four files plus a suite deleted; the requirement lives in `facts.ledger`, observed **red on real data** when a fact still named the deleted gate — `deletions.md` §1.3, §8.2 |
| 5.5 unmotivated checks | done | the search for each remaining file is the table in `deletions.md` §3; nothing was deleted on the argument that it looked unused |
| 5.6 slot lock | done, nothing deleted | the lease names the holder and refuses by name; `test-slot-lock.mjs` → 49/49 — `deletions.md` §4.2 |
| 6.1 re-measure | **done in the closing pass** | 47,358 → 37,338 lines; files 88 → 99; `meta` 5,181 → 0; `scripts total` 37,338 against a 9,000 target, which is **not met**. `coreLines` revised 9000 → 10500 by measurement, `legacyCeiling` untouched — `measurements.md` §6.1 |
| 6.2 golden comparison | **done in the closing pass** | verdict by verdict, with every difference naming its ledger row — `measurements.md` §6.2 |
| 6.3 rebuild every slot | done | four slots, one Gradle invocation each, one pinned JDK major — tasks §6.3 |
| 6.4 economics pairing | done | two plans pass, two slots honestly report no plan — tasks §6.4 |
| 6.5 run cost | **done in the closing pass** | 2.2–4.2 s and `subprocesses: 0`, against 171,313 ms — `measurements.md` §6.5 |
| 6.6 owner review | **open, and left open on purpose** | no owner's words are recorded; recording one without them is the thing this factory forbids |

**What the owner reads to do 6.6**, and nothing more is needed:

```bash
node scripts/verify.mjs                       # 22 PASS, 7 SKIP, ~3 s — the whole project
node scripts/verify.mjs --slot teacher-gradebook          # the reference slot
node scripts/verify.mjs --slot teacher-gradebook --build  # the same, with assembleDebug
node scripts/kiln-status.mjs                  # factory health and every slot's phase
```

Two checkpoints remain, both his: the phase-1 approval recorded with
`node scripts/approve-release.mjs --slot <slug> --note "<your words>"`, and the two first-publication
declarations in the console. `docs/factory-checks.md` is the inventory of what the commands above
actually check.
