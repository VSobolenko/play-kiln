# Tasks: reduce-factory-to-core

Role tags follow the factory's dual-agent convention. `[Build]` writes code, `[Store]` writes the
pack and its copy, `[Verify]` reproduces a claim from an artifact, `[Human]` is the owner.

Read `requirements-preservation.md` before starting. Its §18 is binding: **nothing is deleted before
its successor exists and has been run against the reference slot.**

## 0. Freeze, measure, and repair the config

- [ ] 0.1 Tag the current revision as the pre-change state and confirm the tag is readable.
  **Verify:** `git tag` lists the tag and `git show <tag> --stat` prints a tree.
- [ ] 0.2 Record the baseline measurement with the command that produced the table in
  `proposal.md`: a walk of `scripts/` (excluding `__pycache__`) that prints file count, total lines,
  the `test-*` line total, and the two meta-runners' line total. Save the output into the change.
  **Verify:** the saved output reproduces the numbers `88 / 47,358 / 10,641 / 5,181`.
- [ ] 0.3 Record the **golden run**: run the current verification and store-pack validation against
  `apps/2026-09-26-teacher-gradebook` on this revision and save the complete output, including the
  wall clock.
  **Verify:** the saved output names each check and its verdict, and states an elapsed time.
- [ ] 0.4 **[Build]** Repair `openspec/config.yaml` so it parses: quote or restructure every scalar
  that contains a colon followed by a space, and keep the existing `context`, `proposal` and `tasks`
  rules unchanged in meaning.
  **Verify:** `python -c "import yaml; yaml.safe_load(open('openspec/config.yaml',encoding='utf-8'))"`
  exits 0, and `openspec status --change reduce-factory-to-core` prints no parse warning.
- [ ] 0.5 **[Build]** Add a fact check that parses every configuration file the factory depends on
  and fails the verification run when one does not parse, naming the file and the parse error.
  **Verify:** introducing a stray `: ` into a copy of the config makes the check fail; removing it
  makes it pass. Record both observations.
- [ ] 0.6 **[Verify]** Confirm the repaired config actually reaches the CLI by creating a throwaway
  change and observing that the project rules appear in the artifact instructions.
  **Verify:** the throwaway change's instructions JSON contains the factory's proposal rules; delete
  the throwaway change afterwards.

## 1. Core skeleton and one entry point

- [ ] 1.1 **[Build]** Create the core's structure (`scripts/core/`, `scripts/store/`,
  `scripts/lib/`) with the fact checks as importable functions in `scripts/core/`, each function
  returning a named verdict and each taking its constants from one module.
  **Verify:** importing the core in a node one-liner and calling one check returns a verdict object
  without spawning any process.
- [ ] 1.2 **[Build]** Implement the single verification entry point: it imports and runs every core
  check in one process, prints one row per check with `PASS` / `FAIL` / `SKIP` / `UNVERIFIED`,
  exits non-zero on any `FAIL`, prints the elapsed duration and the subprocess count, and spawns
  only the build tool when a build is requested.
  **Verify:** running it without the build flag prints the rows, the elapsed time and a subprocess
  count of 1 or fewer; a deliberately broken fact makes it exit non-zero.
- [ ] 1.3 **[Build]** Route the build through the existing single Gradle module and the existing
  single Java-home module; do not write a second discovery routine.
  **Verify:** the entry point prints the resolved JDK and the build duration, and `--no-daemon`
  makes the build route throw rather than proceed.
- [ ] 1.4 **[Build]** Port the existing one-fact modules into the core as imported libraries rather
  than subprocess scripts: the Play asset constants, the client-secret sensor, the APK provenance
  chain, the adaptive-icon geometry check and the 16 KB page-size measurement.
  **Verify:** each ported check produces the same verdict on the reference slot as the golden run
  records.
- [ ] 1.5 **[Build]** Write the inline fixture tests for the checks whose sensors moved out of the
  deleted suites: image dimension parsing, aspect-ratio arithmetic, the secret patterns with their
  negative controls, the icon path parser's relative-coordinate semantics, and the release-note
  length measurement. Each fixture set must fail against at least one wrong implementation.
  **Verify:** each fixture set is shown to fail when the behaviour it guards is inverted, and that
  observation is recorded.
- [ ] 1.6 **[Verify]** Run the core against every slot under `apps/` and compare each verdict with
  the golden run.
  **Verify:** every golden `FAIL` is still a `FAIL`, and every new `FAIL` is explained by a check
  whose ledger row says it was removed.

## 2. Judgement rules into prompts and checklists

- [ ] 2.1 **[Build]** Create `harness/roles/` with one canonical document per role — scout, money
  owner, builder, verifier, critic — moving the judgement rules from
  `requirements-preservation.md`'s `PROMPT` rows, each stated once and each with the observation
  that settles it.
  **Verify:** every `PROMPT` row in the ledger is findable in exactly one role document, and a
  search across the repository finds no second statement of it.
- [ ] 2.2 **[Build]** Reduce `.cursor/agents/*.md` and `.codex/agents/*.md` to adapters that point
  at the canonical role documents.
  **Verify:** the adapter files contain no judgement rule text, and the canonical file is named in
  each.
- [ ] 2.3 **[Build]** Author the phase-1 and phase-2 checklists in `docs/`, carrying the `CHECKLIST`
  rows: the submission ladder, the ten declarations, the release and in-app-product form details,
  the EU geo-blocking position, the automatic-translation caution, the two human steps with their
  reasons, the two stated honest limits, and the ideation and money rules the owner reads.
  **Verify:** every `CHECKLIST` row in the ledger has a line in a named checklist document, and each
  line states who reads it.
- [ ] 2.4 **[Verify]** Confirm the factory is usable end to end from the prompts and checklists
  alone by walking one slot's phase-1 review against them and recording which line decided each
  answer.
  **Verify:** the walk produces an answer for every phase-1 question, and no answer requires reading
  a program.

## 3. Split the store pack

- [ ] 3.1 **[Store]** Extract the pack's data model and every numeric limit into one module, and
  prove no limit is defined twice by making the validator and the renderer import the same value.
  **Verify:** changing one limit in the model changes both the renderer's output and the validator's
  message, and a search finds no second literal copy of that limit.
- [ ] 3.2 **[Store]** Extract the write path into a renderer that requires the recorded approval,
  reports `UNVERIFIED` when the provenance record describes a previous run, and never validates its
  own output.
  **Verify:** rendering without an approval refuses; rendering with a stale provenance record
  reports `UNVERIFIED` and exits with the unverified status.
- [ ] 3.3 **[Store]** Extract the read path into a validator that never writes, and make the
  verification entry point call it.
  **Verify:** the validator on the reference slot reproduces the golden run's pack verdict, and it
  creates no file in the slot.
- [ ] 3.4 **[Verify]** Reproduce the gold run's store verdicts through the new validator, including
  a deliberate failure for each asset rule class: a wrong dimension, a wrong byte cap, a wrong
  aspect ratio, a wrong count, a solid-colour image and an edited asset that no longer matches the
  provenance record.
  **Verify:** each injected failure is reported with the image named and the rule named.

## 4. Move the data items

- [ ] 4.1 **[Store]** Move the live console transcript to its archived location, mark it at the top
  as an archived reading that no program reads, and verify it is byte-identical to the original.
  **Verify:** a hash comparison of the two files is equal, and the only difference is the added
  header.
- [ ] 4.2 **[Store]** Move the release-note format contract and the per-language limit into the pack
  model, so the measured string and the pasted string remain the same characters.
  **Verify:** a note one character over the limit fails, a note at the limit passes, and a note for
  an undeclared language is still measured.
- [ ] 4.3 **[Store]** Keep the studio identity schema and the generated-document markers where the
  renderer reads them, and record the reference palette's role names so a future slot starts from a
  named vocabulary rather than a blank file.
  **Verify:** the renderer reads the reference slot's identity unchanged and produces the golden
  run's assets.
- [ ] 4.4 **[Verify]** Confirm every `DATA` row in the ledger resolves to a file that exists and is
  read by a named check or role.
  **Verify:** a listing of all `DATA` rows with their resolved paths contains no missing target.

## 5. Delete, in file groups

Each task deletes only after the ledger shows no remaining `CORE` or `DATA` row pointing at it.

- [ ] 5.1 Delete the meta-runner chain: the self-test's suite discovery and execution, and the
  verification script's invocation of it. Keep the suites whose sensors were ported in task 1.5 as
  the inline fixtures, and delete the rest.
  **Verify:** a search finds no verification or test script that invokes another verification or
  test script, and the entry point still reports the same verdicts as task 1.6.
- [ ] 5.2 Delete the console-answers sheet generator and its regression suite, the walkthrough
  renderer and its drift check, after task 2.3's checklist has been reviewed against the archived
  transcript line by line.
  **Verify:** every console field, page path and cap that the deleted files contained is present in
  the checklist, shown by a side-by-side review whose result is recorded.
- [ ] 5.3 Delete the remaining body of the monolithic store program, after tasks 3.1–3.3 have
  replaced its three responsibilities.
  **Verify:** a search for the deleted module finds no import, and the verification run's verdicts
  are unchanged.
- [ ] 5.4 Delete the two ledger gates (the freshness gate and the lesson-sensor gate) while keeping
  the ledgers themselves readable, and move the requirement they expressed into the fact-ledger
  check that fails when a fact names an enforcing function that does not exist or that the entry
  point does not run.
  **Verify:** a fact naming a non-existent enforcing function makes the run fail; a fact naming
  nothing is reported as unenforced; the two ledger documents still exist.
- [ ] 5.5 Delete the remaining unmotivated checks — each one only after its motivation has been
  searched for in the repository's records and not found — and record the search in the change.
  **Verify:** the change's record lists every deleted check with the search that found no incident.
- [ ] 5.6 Delete the per-slot lock's role as a substitute for scheduling, keeping it as a lease that
  names the holder, and confirm a second build consumer is refused by name rather than by a
  toolchain error.
  **Verify:** a second concurrent build attempt fails immediately and prints the holder's identity.

## 6. Re-measure, record, and prove the result

- [ ] 6.1 **[Verify]** Re-run the baseline measurement of task 0.2 and record the after values next
  to the before values in the change.
  **Verify:** both measurements were produced by the same command, and the line count is at or below
  the budget stated in the design.
- [ ] 6.2 **[Verify]** Re-run the golden run's command set and compare verdict by verdict, then
  record the comparison and the new wall clock.
  **Verify:** the comparison explains every difference, and each explanation names a ledger row.
- [ ] 6.3 **[Verify]** Rebuild every slot under `apps/` with the release build route in a single
  Gradle invocation per slot, and record the build time and the resolved JDK for each.
  **Verify:** every slot's debug build succeeds in one Gradle invocation, and each slot's reported
  JDK is the same pinned major.
- [ ] 6.4 **[Verify]** Confirm each existing slot's commercial plan still pairs with its enabled
  capabilities after the economics change, so no shipped slot's plan becomes unenforceable.
  **Verify:** the pairing check reports a verdict for every slot that has a plan, and any slot whose
  plan names a capability its build does not enable is reported as failing or as unenforced.
- [ ] 6.5 **[Verify]** Confirm the verification run's own cost: one process, one subprocess at most,
  and the elapsed duration recorded.
  **Verify:** the run's output states the subprocess count as one or fewer, and the duration is
  written into the change's record.
- [ ] 6.6 **[Human]** The owner reviews the reduced command surface — the one verification command,
  the two checkpoints, the checklists — and states whether it is the shape he asked for, in his own
  words.
  **Verify:** the owner's words are recorded in the change, and no approval is recorded without them.
