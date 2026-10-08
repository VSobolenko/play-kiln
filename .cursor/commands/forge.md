---
name: "/forge"
id: "forge"
category: "Workflow"
description: "PlayKiln one-command entry: recon picks the topic by measurement, then the app is built and released, stopping only for the owner's two approvals"
---

# /forge — the whole factory from one command

`/ship` starts from an idea the owner already has. `/forge` starts from **nothing**: it finds the
topic by measurement, presents it, and then builds it through to a releasable app — stopping only
where the factory has always stopped.

**One command. Two stops. Both of them are the owner's.** `/forge` is a *chain of the existing
stages*, not a shortcut past them: it runs the same recon, the same gates and the same checkpoints
that `/ship` and `/release` run. What it removes is the owner having to open a second door.

```
/forge [<brief>]
      │
      ├─ A. Recon ──────────── topic chosen by measurement, not by taste
      │
      ├─ B. CHECKPOINT 1 ───── STOP. The owner reads the proposal and says go.
      │
      ├─ C. Build ──────────── slot, code, tests, debug APK   (`.cursor/commands/ship.md`)
      │
      ├─ D. CHECKPOINT 1.5 ─── STOP. The agent has installed and tested the app on the phone; the owner confirms it is the product.
      │
      └─ E. Release ────────── signed artifacts, store pack, screenshots  (`.cursor/commands/release.md`)
```

## The two stops are not negotiable

A single command must not become a single decision. The two checkpoints are the factory's safety,
and neither may be skipped, inferred, or recorded by the agent
(`.cursor/rules/release-gate.mdc`):

- **Checkpoint 1** is the owner choosing what gets built and at what price. Without it there is no
  slot: `scripts/new-slot.mjs` refuses to scaffold without a validated plan.
- **Checkpoint 1.5** is the owner having *looked at the app*. Without it nothing is signed, packed
  or photographed, because all of that work is wasted if the app still changes.

If the owner says "just do everything", the correct reading is **"do not ask me about the parts I
delegated"** — never "skip the two places where I decide". The agent stops at each checkpoint, states
what it is waiting for, and ends the turn.

## Optional brief

`/forge` accepts a brief the way `/ship` does, with one extra rule from
`.cursor/rules/ideation-gate.mdc`: **a brief is a shape, never a topic.** "Something for musicians,
small and usable offline" may name a user benefit, and recon still has to measure a shelf before
proposing anything on it; it must not treat offline/no-backend as an infrastructure criterion. A
brief that names a specific app the owner wants built is the one case that goes to
`/ship` instead, because there is nothing left to recon.

## A. Recon

Run the **scout** role (`harness/roles/scout.md`) exactly as it is written, and follow
`docs/ideation/README.md`. The scout's boundary is absolute: it writes only under `docs/ideation/`,
starts no slot, and records no approval.

**Dispatch before you read.** Recon is several subagents in **one** message, not a solo search:
**two or more scouts** over different query sets or shelves, **plus a `money-owner`** that starts
pricing the first candidate to come back. The orchestrator searches nothing and reads nothing itself
until those are running, then adjudicates what they return. One scout, awaited, is serial work with a
worker's name on it (`.cursor/rules/parallel-execution.mdc`).

**Put the read boundary in the dispatch brief itself.** A subagent does not see this conversation, so
this is not a reminder — it is the only place the scope is communicated:

- Read the **market** (`scripts/scan-market.mjs`, `scan-titles.mjs`, `enrich-market.mjs`,
  `scan-complaints.mjs`) and the killed lists in `docs/research/` and `docs/niche-selection.md`.
- To avoid a duplicate job, read **only** `id`, `name` and `idea` from `registry/apps.json`.
- **Never open `apps/**`**, never inspect a slot, and never report on a slot's completeness or stage.
  Those are experiments, not market evidence, and a report about one is a report about this
  repository wearing the costume of a report about demand
  (`.cursor/rules/ideation-gate.mdc` — "The factory is not the market").

Six rules decide whether this step is done, and they are the ones that have failed before:

1. **The topic is measured, not asserted.** Run `scripts/scan-market.mjs` over
   `scripts/market-queries.json`, then classify the visible titles by hand with
   `scripts/scan-titles.mjs`. A `candidate` verdict from the relevance guard **is not evidence on
   its own** — the guard is blind to homonym collisions and has already called a shelf of
   utility-billing apps `ok` for a pipe-sizing query.
2. **Read the weakest incumbents' reviews** (`scripts/scan-complaints.mjs`), because the complaint
   every competitor shares is the job nobody does, and that is what the app is for.
3. **Confirm installs, ads and purchases** behind the top cards with
   `scripts/enrich-market.mjs` — an idea's viability rests on the shelf's economics, not on its
   search ranking.
4. **Reject before proposing.** No calculators, converters or generic arithmetic over user-typed
   numbers; nothing on the killed list in `docs/research/2026-09-24-market-decision.md` §3 or in
   `docs/niche-selection.md`; nothing a single sentence cannot distinguish from an existing slot
   (Google Play's Repetitive Content policy reaches the whole account).
5. **Label every claim** `MEASURED` / `ON RECORD` / `INFERRED` / `UNVERIFIED`. A number with no
   label, or a label with no command behind it, is the defect this step exists to prevent.
6. **Produce files, not a chat summary.** A scout that returns a list of ideas has done no work:
   nothing on disk cannot be resumed from, and is indistinguishable from nothing. The deliverable is

```
docs/ideation/<YYYY-MM-DD>-<topic>.md
docs/ideation/plans/<slug>.economics.json          # gate-ready
docs/ideation/plans/<slug>.differentiators.json    # gate-ready
```

Validate both plans as they are written, because a plan that does not parse is not a proposal:

```bash
node scripts/check-economics.mjs      --economics      docs/ideation/plans/<slug>.economics.json
node scripts/check-differentiators.mjs --differentiators docs/ideation/plans/<slug>.differentiators.json
```

Both print `SKIP` for the artifact half — that is correct at this stage, because there is no slot to
resolve against yet. The schema checks are **not** skipped, and a `BLOCKED` verdict means the recipe
is not proposable.

Some evidence must be promoted before the slot can exist: the `evidenceRef` pattern accepts only
`docs/research/<file>.md`. If the measured evidence lives in the proposal, promote it into
`docs/research/` (or re-point `evidenceRef` at a research file that genuinely supports the claim)
**now**, not at scaffold time.

## B. CHECKPOINT 1 — present, then stop

Report, in the owner's language and without code. **The decision he is making is a revenue decision**
(`.cursor/rules/ideation-gate.mdc` § "The objective is revenue, not novelty"), so the report leads with
it and never asks him to approve a novelty:

- **the wedge in one sentence** — what a user gets here that the apps already on that shelf do not
  give them, in **functionality or content**, never a new name or palette;
- **the arithmetic, with the division shown** — the price, the deductions, the net per sale, and the
  sales per month needed to clear the target. This is the number the decision rests on;
- the job, who pays, and **what exactly is bought** — plus the price and the moment of payment;
- **the incumbents as evidence, not as a warning list**: who earns there and how (installs, price,
  ads, IAP). A populated shelf with money on display is a point in the proposal's favour, and the
  weak mid-tier is the pocket — say so plainly instead of treating the competition as a defect;
- the demand evidence, with its labels, and the hand-classified job share;
- the reasons this app wins (the promise contract), each with its weight;
- the build estimate in agent hours, the permissions, the capabilities, and any server/cloud/account/
  paid-API cost — whether it is per-user-per-month or one-off and how the price funds it;
- the three numeric kill criteria;
- the honest unknowns, listed rather than omitted.

If a candidate was **refused**, say why in revenue terms — an unrated demand reading, a price the
market will not pay, no statable wedge, or serving cost that defeats the shown revenue arithmetic. "This category is already
occupied" is not a refusal reason and must not appear as one.

If recon produced more than one recipe, present them and name which one it recommends and why. If
it produced exactly one, say so plainly and say what it refuses and why — a refusal with a reason is
part of the proposal.

**STOP. End the turn.** Do not create a slot, do not open OpenSpec, do not write Kotlin. Wait for
the owner's own words.

The owner may approve one recipe, reject all of them and ask for another search, or amend the terms
(price, scope, a promise). An approval is the owner's words, never the agent's confidence. This one
is stated in the conversation and drives step C; it is **not** the release approval, which is a
separate decision and is recorded at checkpoint 1.5 (step D).

## C. Build — hand off to `/ship`, do not reinvent it

On approval, follow **`.cursor/commands/ship.md` from step 1 onward**, in `New app` mode. Step 1's
OpenSpec change (`app-<slug>`) is authored here — `proposal.md`, `tasks.md` and a delta spec, before
the slot exists — because it needs the slug the owner has just approved, and it is the specification
step 2's scaffold refuses to create the slot without (`.cursor/rules/spec-first.mdc`); its checkpoint
1 is already satisfied by step B above, so do not ask it again. Everything
else runs unchanged — step 2's scaffold, the implementation, the tests, the verification. Do not
restate those steps here: that file is the single source of truth for phase 1, and a copy of it would
drift.

**Dispatch again here, not only at recon.** Implementation is **one worker per disjoint file list** —
UI, domain logic, unit tests, resources, the store drafts — dispatched together, never one worker
awaited in a queue. Partition the file lists *before* dispatching, because two writers on one file
corrupt each other, and remember that only one Gradle consumer may hold a slot at a time
(`.cursor/rules/build-speed.mdc`).

Two things are specific to arriving through `/forge`:

- **The slot inherits the recipe that was just approved.** `--slug`, `--name`, `--idea`, `--economics`,
  `--differentiators` and `--naming` come from the proposal in `docs/ideation/`, and `--spec app-<slug>`
  names the change authored in step 1; every promise in
  `docs/ideation/plans/<slug>.differentiators.json` still starts `deferred`. Resolve each one as the
  app is built: `shipped` with a proof that exists in the slot, or `narrowed`/`dropped` in writing —
  with the owner's own words when the promise was `core`.
- **The recon that produced the proposal is fresher than the recipe file.** If the shelf has moved
  since it was measured, say so at checkpoint 1 rather than after the build.

**The privacy policy is published in this phase, not in release.** The URL is a build input for the
app's own in-app policy, so `/ship` step 3 carries the command (`node scripts/publish-privacy.mjs
--slot <slug>`) and the app is built against the URL it returns; `docs/privacy-policy-hosting.md` is
the runbook and the owner is never asked for a host (`.cursor/rules/privacy-policy.mdc`). Publishing
rewrites `store/listing.json`, which the approval at step D binds, so it belongs before that approval
rather than after it.

## D. CHECKPOINT 1.5 — the owner decides the product

`/ship` ends here on its own, and `/forge` must stop here too. **Testing on the device is the
agent's own work, already done**: the debug APK has been installed on the attached phone and every
screen driven there by the agent before this report exists. What is reported is what was
**observed** — the device serial, the hash of the installed artifact, and what was tapped and what
appeared — not instructions for installing or launching anything. The owner's decision is a
**product** decision — is this the app we want to ship — never a test report the agent failed to
produce. Asking the owner to install, launch or verify the app is the defect this checkpoint exists
to prevent, not a way to satisfy it.

**What he is handed is the finished app, not a prototype.** The revision he reviews is the whole job:
every promise in `differentiators.json` resolved to `shipped`, `narrowed` or `dropped` — `verify.mjs`
fails while one is still `deferred` — and every behaviour exercised on the physical phone
(`.cursor/rules/device-testing.mdc`), never asserted from reading the code. That on-device run is
evidence, not a claim: it carries the device serial, the hash of the installed artifact and what was
observed on screen, because "it compiles" and "the tests are green" are not a substitute for it. A
demo with two working buttons is a failed checkpoint even when it compiles, because this is where he
decides the app is the app and he cannot decide that from a fragment.

**What is deliberately absent is everything the next step pays for.** No screenshot, no store pack, no
signing key, no release build — not as a shortcut, but because a concept he has not yet accepted makes
all of it waste, and the answer to "should I do the release work while I am here" is always no. "I will
just prepare the release in the meantime" is the exact sentence this checkpoint exists to refuse.

Nothing is signed, packed or photographed until the owner approves the revision with his own words,
recorded only through:

```bash
node scripts/approve-release.mjs --slot <slug> --note "<the owner's own words>"
```

Never record that approval on the owner's behalf, never treat a "looks fine" from an earlier turn as
it, and never edit the record to get past a refusing script.

## E. Release — hand off to `/release`

After the recorded approval, follow **`.cursor/commands/release.md`**: signed APK and AAB, the store
pack, the captured screenshots, `verify.mjs --build`, and the `/eval` verdict. Then **checkpoint 2**
— ask «may I proceed to publish?»; on yes, the agent walks
`docs/play-publishing-playbook.md` §5.1 end to end and publishes. `/forge` does not skip that ask,
and it does not hand the console labour to the owner: tooling assists (store graphics, the AAB
chooser, and the promo-code download) are the only places the owner's hand is needed. The first two
take a file the owner drops into an open chooser the agent prepared; the third is `Скачать коды` on
`.../promotions`, which the agent clicks **once** and then asks the owner to confirm the save window —
never repeatedly (`.cursor/rules/release-gate.mdc` § "Tooling assists"; owner's instruction, recorded
2026-10-07).
The two first-publication declarations are ticked by the agent once the recorded approval exists,
because the approval is the product checkpoint and not the click. The owner's decision, recorded
2026-10-03: «В нашем заводе есть правило, что перед публикацией необходимо одобрение владельца.
Собственно говоря, в то время и я проверяю проверку всех вот этих политик, и только после моего
одобрения я говорю разрешить публикацию. Поэтому, если я разрешил публикацию, значит эти галочки
можно уже автоматически нажимать. Потому что все проверки необходимые для публикации проводятся до
начальной публикации всего приложения.» And recorded 2026-10-04: the agent asks whether it may
proceed to publish; on yes, the agent itself opens Console, fills every form, uploads what tooling
allows, fixes what needs fixing, and publishes.

## Guardrails

- The two checkpoints are the command's reason to exist, not an inconvenience in it. An agent that
  "saves the owner a stop" has broken the thing it was asked to do.
- Recon is a stage, not a formality. Do not pick a topic and back-fill the evidence; the whole point
  of `/forge` over `/ship` is that the topic is chosen by measurement.
- `PASS` from any check is never permission to build, and no check can approve a release.
- Never record an approval, edit an approval record, or work around a refusing release script. Ask
  the owner instead.
- Never `git commit`, `git push`, `git tag` or create branches without an explicit instruction in the
  current conversation.
- Report actual elapsed wall clock for transparency; it is never a selection, ranking, refusal or kill criterion.
- If the owner ends the session mid-chain, the state is already on disk: the proposal and both plans
  are in `docs/ideation/`, and `/ship` or `/release` can be resumed from them directly. Say which
  door to reopen and why.

## Output

```
## /forge — <title>

**Recon:** docs/ideation/<date>-<topic>.md — <n> recipe(s) proposed by measurement
**Plans:** economics.json + differentiators.json — both validated
**Recommendation:** <slug>, because <the measured reason>

**CHECKPOINT 1 — awaiting the owner's decision. No slot has been created.**

— after approval, the same report becomes:

**Slot:** apps/<dir>
**applicationId:** com.gexetr.<slug>
**Debug build:** app/build/outputs/apk/debug/app-debug.apk
**Verification:** <stdout evidence>

**CHECKPOINT 1.5 — awaiting the owner's product decision, on an app the agent has already installed and exercised. Nothing is signed or packed.**
```
