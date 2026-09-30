# Three-model factory review — decisions and dispositions

> **Date:** 2026-09-29. **Inputs:** three independent reviews of the PlayKiln factory, by three
> different models, plus the external prior-art research in
> [`../research/2026-09-29-app-factory-prior-art.md`](../research/2026-09-29-app-factory-prior-art.md)
> the owner commissioned in parallel. **This document is the decision record**: what was accepted and
> shipped, what was refused and why, and what is honestly still open. The reviewers were reviewers —
> none of them changed a file, and every disposition below is the orchestrator's.

**Why it exists.** The owner's instruction was explicit: three models review the factory and the
industrial practice around it, each in its own context, and then *one* head decides. A review whose
findings are not dispositioned is a document nobody reads, which is the failure this factory has
already paid for twice (§3.5 of the prior-art research names it: *"the console notice a human has to
read sat on no screen, so both documents were silent about it"*).

## 1. Who reviewed what, and what each was told

| Reviewer | Channel | Emphasis |
|---|---|---|
| **DeepSeek** | Cursor background subagent | verification integrity and the self-learning loop: gate design that resists gaming, whether a lesson is connected to a check, and the smallest design that would close the loop. Read-only, network denied. |
| **Codex CLI** | `node scripts/codex-run.mjs run --id review-factory-2 --sandbox danger-full-access` | engineering and harness design: the worker ledger, the approval boundary, JDK/build resolution, and single-source-of-truth duplication across languages. Read-only source inspection; no Gradle. |
| **GitHub Copilot CLI** | `copilot -s --allow-all-tools` | current external state: Android toolchain versions, Play policy deadlines and forms, modern agent-harness practice, 2026 monetization for small offline Android apps. |
| **Prior-art research** | the owner's own commission | every published AI app factory, what is good in their version, and which of their problems this factory is about to pay for again. |

Each was given the same rules of engagement: label every claim `MEASURED` / `ON RECORD` / `INFERRED` /
`UNVERIFIED`; state the cheapest demonstration of each defect; be adversarial; and state what could not
be determined. All three obeyed the labelling rule, which is what made cross-checking possible.

## 2. The findings two reviewers reached independently

Independent agreement is the strongest signal in this set, because the two contexts shared no
methodology — one read the repository's gates, the other read its engineering structure.

| Finding | DeepSeek | Codex | Disposition |
|---|---|---|---|
| The approval digest does not cover the store pack's authored inputs (`listing.json` explains the app; `studio.json` is the identity; `journey.json` is what the shots assert) | **D1** | — | **Accepted, shipped** |
| `play-upload.mjs` — the only script that talks to Google Play — never checks the release approval | — | **P0 #1** | **Accepted, shipped** |
| Nothing evaluates `killCriteria` after launch; the earn stage has no sensor | **D5** | — (stage-contract table agrees: "runtime installs/revenue evaluation has no program") | **Accepted, shipped** |
| The worker ledger's `DONE` proves an exit code, not an artifact; a reused worker id can report another run's completion | **D1/D4 adjacent** | **P1 #3, P1 #4** | **Refused for worker ids**, accepted in part — see §4 |
| No generic per-slot lock; serialization is a human duty, and a second Gradle consumer *dies* rather than waiting | — | **P0 #2** | **Accepted, shipped** |
| `runGradleTasks` fails **open** when no supported JDK resolves, silently re-creating the documented 70-second cold-daemon path | — | **P1 #5** | **Accepted, shipped** |
| A check that cannot fail is not a check — `check-ui-coverage.mjs` printed PASS for every slot but one | **D6** | — | **Accepted, shipped** |
| `visual-qa.py` cannot run from the repository alone and nothing runs it | **D8** | — (single-source table: "visual QA only reports") | **Accepted in part** — see §3 |
| Client-side paid-API credentials are the signature defect of this era and nothing scanned the shipped APK | — (prior art §3.8 = **B1**) | — | **Accepted, shipped** — the highest-value finding in the whole set |
| The self-test never ran six suites that exist and are cited as release evidence | **§3.1 (nothing connects a lesson to a check)** | — (single-source table: "no source-level check compares") | **Accepted, shipped** |

## 3. Accepted and shipped, with the evidence

Every item below was observed, not read. The command and its result are the claim.

1. **The approval now has two halves.** `scripts/lib/pack-approval.mjs` digests the authored pack inputs
   (`store/listing.json`, `store/journey.json`, `store/studio.json`, `store/review-notes.md`) and
   explicitly excludes pipeline outputs (`store/assets/**`, `provenance.json`, `console-answers.md`),
   because a digest over the pipeline's own output would void its own approval every time the pipeline
   ran — the trap `release-approval.mjs` already documented for the adaptive-icon vectors it writes into
   `app/src/main/res/**`. `play-upload.mjs` refuses before reading credentials and before printing a
   `--dry-run` plan. **Evidence:** `node scripts/test-pack-approval.mjs` → 30/30 on the fixed tree, and
   `node .kiln/mutate-upload-gate.mjs` removes `checkReleaseApproval` from the uploader and re-runs the
   suite → **26/30**, with the four FAIL rows all being the unapproved-slot legs (the gate deleted, the
   uploader printed a plan for an unapproved slot). The harness logs `mutation exit: 1`, `restored
   byte-identical: true`, and the verdict `the suite fails against the ungated implementation (the
   sensor can fail)`. The suite is expected to fail against the implementation it replaced.
2. **The earn stage is a sensor.** `apps/<slot>/store/outcomes.json` carries dated readings;
   `scripts/check-outcomes.mjs` judges them against the thresholds the slot declared *before* it was
   built. **Evidence:** `node scripts/test-outcomes.mjs` → 70/70, including the boundary cases (a window
   that has not elapsed is `TOO-EARLY`, not `MISSED`; a window that has elapsed with no readings is
   `NO-DATA` exit 2, not `MISSED`).
3. **The kill rule fires.** `node scripts/kiln-status.mjs --kill <slug> --reason "<why>"` records the
   decision in the registry and prints the archive command; it refuses a kill with no reason, refuses an
   unknown slug, and refuses a slot that meets its own criteria unless `--force`. **Evidence:**
   `node scripts/test-kill-path.mjs` → 19/19 against a real mirror of the repository in a temp
   directory, including the byte-identical-registry assertion for the refusal path. The kill path also
   prints the same command `check-outcomes` prints, from one constant, so the two cannot drift.
4. **A per-slot build lease.** `scripts/lib/slot-lock.mjs`, acquired by `runGradleTasks` — the one Gradle
   route — so two consumers of one slot produce an explicit `SLOT_LOCK_HELD` naming the holder instead of
   a dead worker and a half-written Kotlin file. **Evidence:** `node scripts/test-slot-lock.mjs`.
5. **Gradle fails closed on JDK resolution** instead of inheriting whatever is on `PATH`.
6. **`check-client-secrets.mjs`** scans the shipped client for key-shaped literals, paid-provider SDKs,
   direct calls to paid hosts and a keyed `google-services.json`, with an explicit slot-owned exemption
   file whose entries each need a reason and a decider, and which prints every exemption it applied.
   **Evidence:** `node scripts/test-client-secrets.mjs` (including negative controls: a `YOUR_API_KEY`
   placeholder, the word `secret` in prose, and an offline slot must all pass), plus a clean run on both
   real slots — 144 files and 51 files scanned, exit 0.
7. **`check-ui-coverage.mjs` can now fail.** The two ways of checking nothing are `UNVERIFIED` (exit 2)
   rather than `PASS`. **Evidence:** `--slot teacher-gradebook` → `PASS … (40 reachable)`;
   `--slot pipe-sizing` → `UNVERIFIED … none of them is named by REQUIRED`, where the previous
   implementation printed `PASS … (0 reachable, 0 reported as notes)`.
8. **The self-test runs every suite it finds.** Six suites — `test-apk-provenance`,
   `test-capture-publish`, `test-capture-assertions`, `test-codex-signals`, `test-play-scan`,
   `test-play-reviews` — existed, were cited in `AGENTS.md` as evidence a release rests on, and were
   never executed by the one command a session runs. `selftest.mjs` now discovers and runs every
   `scripts/test-*` file except those with a dedicated section or a named reason, and fails when a suite
   is neither run nor given a reason. **Evidence:** all seven unregistered suites were run first and
   passed (67 ms – 7.9 s); the runner and its coverage check are wired in.
9. **The self-learning loop is closed at the three points it was open.** `measured-facts.json` gains a
   truthful `enforcedBy` per fact (the script *and* the probe), `check-freshness.mjs` fails when a named
   program or probe does not exist and reports `UNENFORCED` (exit 2) when a fact has no program at all,
   and `check-lessons.mjs` fails when a lesson names a sensor that is gone. **Evidence:**
   `node scripts/check-freshness.mjs` → exit 2 with the two real gaps named;
   `node scripts/check-lessons.mjs` → exit 0; `node scripts/test-freshness.mjs` → 102/102, and 116/116
   with `--prove` (7 mutation proofs). Each new rule was shown failing and restored — quoted in
   `docs/self-learning.md`.

## 4. Refused, deferred, or accepted only in part

A refusal with a reason is the point of this section. Three of these refusals are load-bearing.

| Recommendation | Disposition | Reason |
|---|---|---|
| **Widen the approval digest to `scripts/**` and `.cursor/rules/**`** (prior art **A1**/§2.3, Consort's "controls the agent cannot edit") | **Refused** | Every harness improvement would void every slot's approval, and this factory's whole method is that the harness changes on every slot — the owner said so directly ("interruption and change are normal and correct"). `scripts/test-apk-provenance.mjs` also asserts the opposite on purpose: *"a change under `scripts/` does not demand a rebuild."* The boundary is instead made **auditable** — one command reports exactly which bytes the owner approved — and both remaining holes are now stated in `AGENTS.md` rather than implied. Recorded, not closed. |
| **Holdout scenarios the builder cannot read** (prior art **B2**, StrongDM's dark factory) | **Deferred** | Genuinely the strongest idea in the prior art and it costs little, but it needs a *second context* to hold the answer, and this factory's default loop still has one head writing and judging. Adding the artifact without the independent holder would produce a file that looks like a holdout and is graded by its author — worse than not having it. It goes first in the next round of work. |
| **Human-override rate as a promotion gate** (prior art **B3**: 20 runs, >90% pass, <5% false positive, **<10% override**) | **Deferred, with the honest reason** | The metric is cheap to record and meaningless today: the factory has completed **one** real slot, and the threshold is defined over twenty. Recording a rate over `n = 1` would be a number that looks like evidence. What is *not* deferred is the honesty rule it implies, now stated: a recorded approval is the owner's words, `approvedBy` is a constant, and nothing distinguishes the owner's words from an agent's. |
| **Versioned worker run identity + artifact receipts** (Codex P1 #3/#4) | **Partially accepted** | The diagnosis is right and the ledger's `DONE` is too weak. But this factory's worker verdicts are read alongside the *tree* by rule ("check the deliverable on disk before trusting a label"), and the receipts the review proposes are a real subsystem. What was accepted now is the part with immediate payoff: the sandbox classifier (`SANDBOX_UNAVAILABLE`) that stopped a dead worker reporting `DONE`, and the `LOST`-is-unverified rule. The receipts stay open and are recorded below. |
| **One schema-validated data contract replacing three copies of the asset policy** (Codex P1 #6/#7) | **Deferred** | Correct on the merits: `store-kit.py`, `play-assets.mjs`, `play-upload.mjs` and the regex readers in `verify-slot.mjs`/`visual-qa.py` each restate screenshot targets, caps and locales, and the values agree today only by attention. Replacing the regex readers needs a canonical data file plus adapters in both languages — a harness change larger than this round, and the four copies are currently in agreement, which the review measured. |
| **`portfolio_overview` and `play_halt_rollout`** (prior art **B6**) | **Deferred** | Both need live Play credentials to be anything but a stub, and neither can be verified offline. The dry-run half already exists. |
| **Head-term registry and collision check** (prior art **B5**) | **Deferred** | Medium cost, real value for ASO *and* as machine-readable evidence in a Repetitive Content investigation. Nothing collides today: the two shipped slots are a pipe-sizing workbook and a classroom mark book. It becomes urgent at the third. |
| **Per-slot platform-deadline register** (prior art **B7**) | **Deferred** | The *factory*-level version shipped (`measured-facts.json` with `enforcedBy`). The per-slot version is the right shape at ten slots and is not yet worth the artifact at two. |
| **Make the promise gate prove behaviour, not symbols** (DeepSeek **D4**: `test` proofs resolved against executed `<testcase>` names, `code` proofs rejected inside comments, and the contract's commit ordered before the first `app/src/main/` commit) | **Accepted as a defect, not yet fixed** | Every part of the diagnosis is confirmed and it includes the most uncomfortable single fact in the whole review: **the first real slot's `differentiators.json` was committed 12 minutes after the app it was supposed to constrain.** The fix touches `lib/slot-differentiators.mjs`, its gate, its fixtures and the one existing slot's proofs, and it needs an executed test run to resolve against — it is the second item of the next round, after the holdout set. |
| **No gate requires a single test to execute** (DeepSeek **D7**) | **Accepted as a defect, deferred by the build-speed rule** | `verify-slot.mjs --build` compiles but never runs the unit tests, and `run-slot-tests.mjs` reports `NO TESTS` with exit 0. The fix must add `testDebugUnitTest` to the **same** Gradle invocation as `assembleDebug` — a second invocation would violate `.cursor/rules/build-speed.mdc`, which measured 6.3 s → 2.3 s for exactly that mistake. That is a deliberate change to the gate's build step, not a line, and it is scheduled rather than rushed. |
| **`visual-qa.py` cannot run from the repository alone** (DeepSeek **D8**) | **Accepted as a defect, not yet fixed** | Confirmed: it dereferences the first raw capture without a guard, and `store/captures/` is gitignored, so a clean checkout raises `AttributeError` instead of reporting `UNMEASURED`. The rule's own text already says it is a report and not a gate, so the defect is the traceback and the silence, not the absence of a gate. |
| **The console's automatic string translation** (prior art, `enforcedBy: null`) | **Accepted as a recorded gap** | No program can assert it: the state lives in Play Console, not in the repository. It is now reported as `UNENFORCED` by `check-freshness.mjs` rather than implied to be checked — which is the honest fix available offline. |
| **The single-account blast radius** (prior art **B9**: one forced 30-day sweep terminated an account with 2,500 apps; Google's white-label guidance says decentralise) | **Recorded, owner's decision** | It conflicts directly with the namespace rule (`com.gexetr.<slug>`, an applicationId is permanent after first upload), which is the owner's to change. Not an agent's call, and not urgent at two apps. Raised, not resolved. |

## 5. What could not be determined

Stated so the next session does not mistake a reading for a measurement.

- **No reviewer executed a gate.** All three read code, git history and files on disk. Every
  "MEASURED" in their reports means "I read the artifact", not "I ran it". The dispositions above
  therefore required a *run* before acceptance, and each shipped item in §3 has one.
- **Whether any recorded approval was genuinely the owner's words.** Unanswerable from the repository:
  no transcript of owner speech is stored, and `approvedBy: 'owner'` is a constant. See §4.
- **Whether the current `store/provenance.json` is genuine.** `dist/` is absent, so the artifact link is
  by design "not checked this run" and a real record cannot be told from a plausible one offline.
- **Copilot's external figures are industry commentary, not vendor data.** Its own report says so: the
  monetization benchmarks are SEO/blog sources, and it flags that the official Play Billing deprecation
  page is self-inconsistent (a banner naming version 8 by 2026-08-31 against a table naming version 7),
  that the 16 KB enforcement date has two published variants, and that Kotlin 2.3.21 was only visible as
  an `-RC2` tag in its search. None of those was allowed to become a fact in
  `docs/research/measured-facts.json` without a source and a re-check date.
- **Codex's Windows sandbox failed** (`CreateProcessAsUserW` → Win32 1920, the `pwsh.exe` execution alias
  under `WindowsApps`). It ran its review with `--sandbox danger-full-access` after a stash snapshot. The
  classifier gap that let a sandbox failure record `DONE` was itself a finding, and is fixed.

## 6. The factory's own answer to "is it self-learning now?"

Partly, and the split matters more than the label.

**Automated, enforced, and able to fail:** a rule that goes stale is `UNVERIFIED`; a fact that names an
enforcing program which no longer exists is a failure; a lesson whose named sensor was deleted is a
failure; a regression suite that exists but is not run is a failure. Those four closes are the loop's
teeth, and each was demonstrated failing before it was trusted.

**Not automated:** noticing that Google Play published a new requirement. No robot browses the console;
a human or an agent reads the page, and `docs/research/measured-facts.json` is re-dated. The honest
consequence is stated in `docs/self-learning.md` rather than hidden: every fact in that ledger is true
**as of the date recorded with it**, and the ledger's job is to make a stale one visible, not to be
right about the future.

**The operator's loop, one line:** after any external reading, re-date the fact and name what enforces
it; after any defect, add the sensor *and* name it in the lesson. Two commands check the result —
`node scripts/check-freshness.mjs` and `node scripts/check-lessons.mjs` — and `node scripts/selftest.mjs`
runs every suite the factory owns.
