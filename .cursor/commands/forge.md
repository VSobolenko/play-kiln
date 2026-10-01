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
      ├─ D. CHECKPOINT 1.5 ─── STOP. The owner installs the APK and says it is good.
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
small and offline" narrows the *search*, and recon still has to measure a shelf before proposing
anything on it. A brief that names a specific app the owner wants built is the one case that goes to
`/ship` instead, because there is nothing left to recon.

## A. Recon

Run the **scout** role (`harness/roles/scout.md`) exactly as it is written, and follow
`docs/ideation/README.md`. The scout's boundary is absolute: it writes only under `docs/ideation/`,
starts no slot, and records no approval.

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

Report, in the owner's language and without code:

- the job, and who pays, and **what exactly is bought** — plus the price and the moment of payment;
- the demand evidence, with its labels, and the hand-classified job share;
- the arithmetic: the price, the deductions, the net per sale, and the sales per month to reach the
  target, with the division shown;
- the reasons this app wins (the promise contract), each with its weight;
- the build estimate in agent hours, the permissions, the capabilities, and whether a server is
  required;
- the three numeric kill criteria;
- the honest unknowns, listed rather than omitted.

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
OpenSpec proposal (`app-<slug>`) is created here, because it needs the slug the owner has just
approved; its checkpoint 1 is already satisfied by step B above, so do not ask it again. Everything
else runs unchanged — step 2's scaffold, the implementation, the tests, the verification. Do not
restate those steps here: that file is the single source of truth for phase 1, and a copy of it would
drift.

Two things are specific to arriving through `/forge`:

- **The slot inherits the recipe that was just approved.** `--slug`, `--name`, `--idea`, `--economics`
  and `--differentiators` come from the proposal in `docs/ideation/`, and every promise in
  `docs/ideation/plans/<slug>.differentiators.json` still starts `deferred`. Resolve each one as the
  app is built: `shipped` with a proof that exists in the slot, or `narrowed`/`dropped` in writing —
  with the owner's own words when the promise was `core`.
- **The recon that produced the proposal is fresher than the recipe file.** If the shelf has moved
  since it was measured, say so at checkpoint 1 rather than after the build.

## D. CHECKPOINT 1.5 — the owner looks at the app

`/ship` ends here on its own, and `/forge` must stop here too. Report what the app does, what to tap,
how to install it, and the verification output — then wait. Nothing is signed, packed or
photographed until the owner approves the revision with his own words, recorded only through:

```bash
node scripts/approve-release.mjs --slot <slug> --note "<the owner's own words>"
```

Never record that approval on the owner's behalf, never treat a "looks fine" from an earlier turn as
it, and never edit the record to get past a refusing script.

## E. Release — hand off to `/release`

After the recorded approval, follow **`.cursor/commands/release.md`**: signed APK and AAB, the store
pack, the captured screenshots, `verify.mjs --build`, and the `/eval` verdict. Then hand the Play
Console ladder back to the owner's observation
(`docs/play-publishing-playbook.md` §5.1). `/forge` ends when the slot is ready for Play — it does
not publish, and it never performs the owner's legal declarations.

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
- Report the actual elapsed wall clock. The one-hour target is a direction and is never cited as met.
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

**CHECKPOINT 1.5 — awaiting the owner's review of the installed app. Nothing is signed or packed.**
```
