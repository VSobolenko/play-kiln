---
name: "/ship"
id: "ship"
category: "Workflow"
description: "PlayKiln phase 1: one idea → isolated slot → dual-agent development → an app the owner can review"
---

# /ship — PlayKiln development phase

The factory's front door when the idea is already in hand, and **only the first half of it**. One
invocation takes an idea to an app the agent has installed and exercised, which the owner then
judges as a product: isolated slot, implementation, tests, source-level verification. It then
**stops** and asks the owner to approve the release.

Use **`/forge`** instead when there is **no idea yet**: it runs recon to choose the topic by
measurement, and on the owner's approval continues into these same steps — then into `/release`.
Nothing below changes when the work arrives through `/forge`: same stages, same gates, same stops.

Building the signed artifacts, composing the store pack and capturing screenshots are release
work: they happen in `/release`, on the owner's explicit word. Read
`.cursor/rules/release-gate.mdc`.

Two modes share one phase:

| Mode | When | Difference |
|---|---|---|
| **New app** | the human brings an idea and no slot exists | creates the slot first (step 2) |
| **Continue** | a slot / OpenSpec change already exists | starts at step 3 |

Decide the mode in step 0. Never create a second slot for an idea that already has one —
extend the existing slot.

## Steps

### 0. Classify

- New idea with no slot → **New app** mode. Platform is fixed: Android / Kotlin / Compose.
- Existing work: `openspec status --change "<name>" --json`, or a slot already under
  `apps/` → **Continue** mode.

If the idea is underspecified, ask **one** blocking question and proceed. Do not stall on
polish.

### 1. Propose (OpenSpec + the economics plan)

For a new idea, run `/opsx-propose` for change name `app-<slug>` (slug = short kebab-case
derived from the idea). Read `AGENTS.md` and `openspec/specs/**` first.

The change is the specification the slot is built **inside**, and it is authored here, **before the
slot exists**: `proposal.md`, `tasks.md` and a delta spec under `specs/<capability>/`, confirmed by
`openspec validate app-<slug>`. Step 2's scaffold refuses to create the slot without it
(`.cursor/rules/spec-first.mdc`).

Before proposing anything, apply `.cursor/rules/ideation-gate.mdc`:

- Reject the topic if it is a calculator, a converter, or generic arithmetic over user-typed
  numbers. An example the owner gave is a shape, not a topic.
- Reject it if it duplicates a job another slot already does, or if it is on the killed list
  in `docs/research/2026-09-24-market-decision.md` §3.
- Demand must be measured with `scripts/scan-market.mjs` / `enrich-market.mjs` /
  `scan-titles.mjs`, and hand-classified — a `candidate` verdict alone is not evidence.

Write the economic plan and validate it:

```bash
node scripts/check-economics.mjs --economics docs/ideation/plans/<slug>.economics.json
```

The plan must name a real archetype, a paying user, what is bought, a price, the moment of
payment, a measured evidence reference, and numeric kill criteria. See
`docs/slot-economics.md`. **A plan whose honest answer to "what does the user buy?" is
"nothing, they pay up front" is usually not monetized** — say so and justify it.

Present the proposal, the tasks, and the economics plan.
**CHECKPOINT 1 — stop until the human says go.**

### 2. Create the slot (New app mode only)

```bash
node scripts/new-slot.mjs --slug <slug> --name "<Human Title>" \
  --idea "<one line>" \
  --economics docs/ideation/plans/<slug>.economics.json \
  --differentiators docs/ideation/plans/<slug>.differentiators.json \
  --naming docs/ideation/plans/<slug>.naming.json \
  --spec app-<slug>
```

Creates `apps/<YYYY-MM-DD>-<slug>/` from `scaffold/android-compose`, registers the attempt in
`registry/apps.json`, copies the validated plan to `economics.json` and the promise contract to
`differentiators.json`, and prints the
`applicationId` (`com.gexetr.<slug>` per `scripts/lib/namespaces.mjs`). The script refuses to
create the slot without a validated plan, a validated promise contract, a validated naming contract,
**and the specification authored in step 1** — it names the exact `openspec/changes/<spec>/` path it
expected and refuses before it writes anything, so a slot cannot be born outside its change — and
self-checks that
no build output was copied and
no template token or old package survived; if it reports FAIL, fix the scaffold before
implementing anything.

Every promise in `differentiators.json` starts `deferred`, and `verify.mjs` fails while any
promise is still deferred. Resolve each one as you build: `shipped` with a proof that exists in the
slot, or `narrowed`/`dropped` in writing — with the owner's own words when the promise was `core`.

### 3. Implement — dispatch, do not type

Follow `harness/dual-agent.md`, `.cursor/skills/pge-harness/SKILL.md`, and
`.agents/skills/subagent-driven-development/SKILL.md`.

**The orchestrator does not implement this step.** It partitions the work into bounded, disjoint file
lists and dispatches **one worker per list, together, in a single message** — UI, domain logic, unit
tests, resources, the store drafts. A queue of one worker awaited at a time is serial work with a
worker's name on it, and it is the default this step exists to break
(`.cursor/rules/parallel-execution.mdc`).

- **Plan** via the `planner` agent. It is read-only in this build: it reports the proposal and the
  orchestrator writes the file.
- **Build** via `generator` subagents and/or Codex workers (`node scripts/codex-run.mjs`, several
  backgrounded at once). Codex is a co-lead engineer, not a reviewer.
- Ledger: `.superpowers/sdd/<slug>/progress.md`.
- **Partition before dispatching.** Two writers on one file corrupt each other, and the corruption
  surfaces later as a mystery defect. Disjoint files or separate worktrees, decided up front.
- **Keep a verifier's context separate** from the builder's: the context that wrote the code is the
  least able to judge it.
- Only one Gradle consumer per slot.
- Stubborn stalls → `ralph-loop` (capped). Fix loops ≤ 5.
- Evidence over self-reports.

Scope the app so it can be reviewed on a device quickly. Cut screens and narrow the job
**now**, before the build, never by skipping verification later
(`.cursor/rules/parallel-execution.mdc`).

**Publish the privacy policy here, not in `/release`.** The in-app policy is a separate Play
requirement, so the URL the app itself must open has to exist while the app is still being built:

```bash
node scripts/publish-privacy.mjs --slot <slug>
```

It renders `store/privacy.md` to a real public HTTPS page and records the URL in
`store/listing.json`; mirror the same URL into a `translatable="false"` string in `res/values/` and
open it from the app (`slot.inAppPolicy`, `scripts/check-in-app-policy.mjs`). Publishing rewrites the
listing, which is part of the pack the owner approves, so doing it now is what stops it from voiding
the approval later. **The owner is never asked for a host** — the factory publishes
(`docs/privacy-policy-hosting.md`, `.cursor/rules/privacy-policy.mdc`).

### 4. Development verification

This is the verification a revision gets **before** the owner sees it. It is deliberately
cheap and reversible:

```bash
node scripts/run-slot-tests.mjs --slot <slug>          # unit tests
node scripts/verify.mjs --slot <slug> --build     # source gates + assembleDebug
```

`--build` compiles with `assembleDebug` only; it does not sign or produce a release artifact.
Prefer DroidAgentKit MCP for lint / doctor / runtime checks when connected. `UNVERIFIED` is
not PASS — read the diff by hand.

A debug APK the agent installs on the connected phone and drives itself is the expected output
(`app/build/outputs/apk/debug/app-debug.apk`). What the owner judges is the product, on the evidence
the agent observed there.

**Do not run any release step here.** `prepare-release.mjs`, `scripts/store/render.py` rendering and
`media-capture.mjs` all refuse until the owner approves, and trying to work around that
refusal is the failure this phase exists to prevent. `scripts/store/render.py --init` (draft copy) and
`scripts/store/validate.py` stay open and are fine to use.

### 5. Stop and request the owner's approval

**CHECKPOINT 1.5 — the human decides the product.** The agent has already installed the debug
artifact on the attached phone and driven every screen itself; this report is the evidence of that
run, and asking the owner to install, launch or verify the app is the defect this checkpoint exists
to prevent. Report, then wait:

- the agent ran the app on the connected device and what it observed there,
- the exact screens exercised,
- the device serial and the hash of the installed artifact, because the on-device run is evidenced by
  them plus what was observed on screen — "it compiles" and "the tests are green" are not a
  substitute (`.cursor/rules/device-testing.mdc`),
- the verification output from step 4,
- that the release (build, signing, store pack, screenshots) has **not** run.

The owner then either requests changes (return to step 3) or approves the release. Only on his
explicit approval in the current conversation:

```bash
node scripts/approve-release.mjs --slot <slug> --note "<the owner's own words>"
```

Then hand off to **`/release`**. Never record an approval without his words, and never treat
your own confidence as approval.

## Guardrails

- Evidence before "done": never accept Cursor or Codex self-reports without `verify.mjs`
  output plus a read diff.
- Never run the release phase inside `/ship`. If the owner asks for a publish-ready build,
  that is `/release`.
- Never commit secrets, never force-push `main`, never publish on the human's behalf.
- Never `git commit`, `git push`, `git tag`, or create branches without an explicit
  instruction in the current conversation. Read-only git inspection is always fine.
- Never generate or rotate a signing key on your own initiative.
- Never scaffold a slot without a validated `economics.json`; the money stage is not optional
  and a green slot that cannot earn is a failed slot.
- Never carry an example the owner gave forward as the topic.
- Keep slots isolated; improve the **root** harness when the same failure repeats
  (`.cursor/rules/verification-discipline.mdc`).
- Fixing a factory defect means adding a check to the inline fixture set of the module that
  owns the rule (`scripts/core/*.mjs`, run by `node scripts/verify.mjs`) and confirming it
  fails before the fix.
- Report the development phase's actual elapsed wall clock for transparency; it is never an idea or
  kill criterion.

## Output

```
## /ship complete — <title> (development)

**Mode:** new app | continue
**Slot:** apps/<dir>
**applicationId:** com.gexetr.<slug>
**Change:** app-<slug>
**Debug build:** app/build/outputs/apk/debug/app-debug.apk
**Verification:** <stdout evidence>
**Release:** NOT started — build, signing, store pack and screenshots wait for approval
**Awaiting:** owner review, then `node scripts/approve-release.mjs --slot <slug> --note "..."` and `/release`
```
