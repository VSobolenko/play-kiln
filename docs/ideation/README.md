# `docs/ideation/` — proposals the owner chooses between

This directory is where an idea exists **before** it is allowed to become a slot. It is not
documentation for an existing app. A file here is a proposal, and the owner is the reader.

## What lives here

| Path | What it is |
|---|---|
| `README.md` | This file: the rules for proposing an idea. |
| `<date>-<topic>.md` | A human-readable proposal set: what was measured, one or more recipes in full, a comparison, a recommendation, and the refusals. |
| `plans/<slug>.economics.json` | One gate-ready economics plan per recipe, in the exact schema of `docs/slot-economics.md`. |
| `plans/<slug>.differentiators.json` | One gate-ready promise contract per recipe, in the exact schema of `docs/slot-differentiators.md`: the measured reasons this app can win, each weighted `core` or `supporting`. |
| `plans/<slug>.naming.json` | One gate-ready naming contract per recipe, in the exact schema of `docs/app-naming.md`: the scored candidates the name was chosen from, the chosen name, and the alternatives that were rejected with their reasons. |

## The rule: a proposal must pass the economics gate before a slot exists

`scripts/new-slot.mjs` requires `--economics <path>` and validates it with
`parseEconomics` from `scripts/lib/slot-economics.mjs`. A slot whose plan is missing, incomplete,
or paired with an archetype whose capability cannot collect the money is **never scaffolded**.
`scripts/check-economics.mjs` re-checks the same plan at verify time against the slot's
`capabilities.json`.

So the order is fixed and is not negotiable:

1. **Propose.** Write the proposal and its `plans/<slug>.economics.json` under this directory.
2. **Prove the plan parses and is complete.**
   `node scripts/check-economics.mjs --economics docs/ideation/plans/<slug>.economics.json`
   This prints `SKIP` for the capability pairing, because the capabilities are not enabled until
   the slot exists. The gate's schema checks are not skipped, and a `BLOCKED` verdict means the
   recipe is not proposable yet.
3. **The owner picks one.** Only the owner chooses; the factory does not start an idea on its own.
4. **Scaffold.** Run the `new-slot.mjs` command the proposal records. It writes the validated plan
   verbatim to the slot root as `economics.json`, and the slot's `capabilities.json` must enable
   what the archetype needs (`billing` for the `purchase_led_*` archetypes, `ads` for
   `ads_led_utility`). It also writes `differentiators.json` from the promise contract, with every
   promise `deferred` — see the next section.
5. **Promote the evidence.** The gate's `evidenceRef` pattern accepts only
   `docs/research/<file>.md`. Before a slot is scaffolded from a plan whose measured evidence lives
   here, that evidence must be promoted into `docs/research/`, or the `evidenceRef` must be
   re-pointed at a research file that genuinely supports the claim. See the factory-improvement
   section of `2026-09-26-five-recipes.md` for why this is a defect rather than a nuisance.

## The rule: a proposal must also state why the app can win

The economics gate answers "can this earn". It cannot answer "why would anyone install it", and an
app can pass every money check while being a worse app than the one the recon selected. So a recipe
ships a second plan beside its economics plan: `plans/<slug>.differentiators.json`, in the exact
schema of `docs/slot-differentiators.md`.

Each promise is one sentence — what a user gets here that the incumbents do not give them — with the
recon evidence behind it, labelled `MEASURED`, `ON RECORD`, `INFERRED` or `UNVERIFIED`, and weighted
`core` or `supporting`. A `core` promise is one of the reasons the recipe was chosen: losing it
changes what the product is, which is why dropping one later needs the owner's own words.

```bash
node scripts/check-differentiators.mjs --differentiators docs/ideation/plans/<slug>.differentiators.json
```

This prints `SKIP` for the artifact resolution, because there is no slot to resolve against yet; the
schema checks are not skipped. A `BLOCKED` verdict means the reason list is not proposable.

Every promise is written `deferred`, and it stays deferred until the build makes it true. That is the
mechanism: `scripts/verify.mjs` fails while any promise is deferred, so the slot cannot be
called done with the easy ten of twenty delivered and the rest quietly abandoned.

> The five recipes recorded in `2026-09-26-five-recipes.md` and the proposals beside them predate
> this contract: each records only `--economics`, so `new-slot.mjs` will refuse them. Before any of
> them is scaffolded, its promise contract and its naming contract have to be written from the recon
> already in the proposal.

## The rule: a proposal must also name the app

The name is the third recorded decision, beside the price and the reasons. It is derived from a
scored field of candidates rather than invented as the thought before the first line of code, because
a title that is the bare category noun spends Play's strongest ranking field on the head term the
incumbent already owns and gives a user nothing to prefer. `docs/app-naming.md` is the contract and
`plans/<slug>.naming.json` is the artifact.

```bash
node scripts/check-naming.mjs --naming docs/ideation/plans/<slug>.naming.json --validate-only
```

This prints `SKIP` for the pack cross-check, because a proposal has no slot and no `store/listing.json`
yet; the schema checks are not skipped, and `--validate-only` skips only the evidence file resolving on
disk. A `BLOCKED` verdict means the name is not proposable: the contract must carry at least five
scored candidates spanning at least three archetypes, the chosen candidate at 70/100 or better, at
least one rejected alternative with the reason it lost, and a title inside Play's 30-character cap in
every declared locale.

## The rule: a proposal must also say which device capabilities the job uses

The economics gate answers "can this earn", the promise contract answers "why would anyone install
it", and the naming contract answers "what is it called". None of them asked what the app does with
the phone, and the measured result was a monoculture: of the proposals on record almost every one is
"the user types something and the app stores it" — camera 0, location 0, motion sensors 0,
home-screen widget 0, media playback 0, while the phone has all of them. The omission was
**structural, not a preference**: no recon contract had a field for a device capability, so a scout
had nowhere to write "this job needs the camera" even if it wanted to, and the answer arrived as a
silence that reads exactly like a decision. This is that field. It does not decide how far to open
the surface — that stays the owner's choice at checkpoint 1 — it makes the current answer *declared*.

Every proposal carries a **`## Device capabilities`** section answering `used` or `not used` for each
of twelve capabilities. It is a declaration, not a tally, and the load-bearing part is the
justification: a `used` line names the API the slot would call and what the job loses without it, so
a job that needs nothing from the phone has to say so twelve times rather than by omission. A
capability that needs a runtime permission must name that permission, because the proposal's own
permissions section is where the permission-minimisation bar is read. The platform fact that keeps
this a declaration problem rather than a cost problem: **the Android Photo Picker
(`ActivityResultContracts.PickVisualMedia`) needs no runtime permission at all**, so "uses the photo
picker" costs nothing against that bar, while a capability that does need one must say which. "No
camera" is allowed; what is no longer allowed is arriving at it by accident.

```markdown
## Device capabilities

Each capability is answered `used` or `not used`; a `used` line names the API and what the job
loses without it.

- camera: not used — the job is text entry; no step in it is a photograph.
- photo picker / gallery: used — `ActivityResultContracts.PickVisualMedia`, which asks for no
  runtime permission; without it the user retypes a reading the phone already holds.
- media playback or capture: not used
- microphone: not used
- location: not used
- motion sensors: not used
- contacts / calendar: not used
- notifications / reminder: used — `NotificationChannel`, `android.permission.POST_NOTIFICATIONS`;
  without it the round is forgotten between visits.
- home-screen widget: not used
- share / export: used — `ACTION_SEND`, `FileProvider`; without it the record cannot leave the phone.
- Bluetooth / USB: not used
- files / SAF: not used
```

Those twelve are the closed set: camera, photo picker / gallery, media playback or capture,
microphone, location, motion sensors, contacts / calendar, notifications / reminder, home-screen
widget, share / export, Bluetooth / USB, files / SAF. Adding one is a contract change; leaving one
unanswered is not allowed. `node scripts/verify.mjs` reads the section (`ideas.capabilities`): it
reports a proposal with no declaration, a capability left unanswered, a `used` line that names no API
or no loss, and a declaration that contradicts the document's own permissions prose or omits the
manifest permission a used capability needs. What it cannot read is in the module's own comment — it
cannot tell whether the named API is the right one, or whether the job would really miss it; that is
the owner's question at checkpoint 1.

## What a proposal must contain

An idea reaches the owner as a proposal, never as a started slot
(`.cursor/rules/ideation-gate.mdc`, "How an idea reaches the owner"). The minimum, per recipe:

- Category and job, stated as one sentence.
- The paying user, what exactly is bought, the price, and the moment of payment.
- The archetype, the capability it needs, and **which archetypes were considered and rejected, with the revenue reason for each**. No archetype is the factory's default, all four are first-class, and `subscription` is banned outright — see `.cursor/rules/ideation-gate.mdc` § "There is no house monetization, and subscriptions are banned".
- The reasons the app can win, as a promise contract: each one a sentence, with its recon evidence
  and strength label, and its weight (`core` or `supporting`). At least three promises, at least two
  of them `core`.
- The name, derived rather than invented: the candidates that were scored, the archetypes they span,
  and the alternatives that were rejected with the reason each lost — so the owner sees the field the
  name was chosen from, not only the name.
- Demand evidence with each claim labelled MEASURED, ON RECORD, INFERRED or UNVERIFIED, and the
  hand-classified job share alongside any relevance-guard verdict — a guard verdict alone is not
  evidence, because the guard is blind to homonym collisions.
- Unit arithmetic: the price, the deductions, the net per sale, and the number of sales per month
  needed to reach a stated target, with the division shown.
- Build estimate in agent hours, permissions required, capabilities required, and whether a server
  is required (and its cost if so).
- The device capabilities the job uses, declared one line per capability in a `## Device
  capabilities` section: twelve answers of `used` or `not used`, and for each `used` one the API the
  slot would call, what the job loses without it, and the manifest permission it needs. This is
  where a job that needs the camera, the microphone, the sensors, location or a widget says so — and
  where a job that needs none of them says so too, on the record, rather than by omission.
- The visual direction in one or two sentences — the mark, the palette's subject and the one thing
  the app's own components do that a stock Material screen does not. Two recipes must not propose
  the same direction, for the same reason they must not propose the same job
  (`.cursor/rules/visual-identity.mdc`).
- Kill criteria as three finite numbers.
- Honest unknowns, listed rather than omitted.

## What is not allowed here

- No calculators, unit converters, or generic arithmetic tools. That category is burned; see
  `.cursor/rules/ideation-gate.mdc`.
- Nothing on the binding killed list in `docs/research/2026-09-24-market-decision.md` §3 or the
  dead zones in `docs/niche-selection.md`.
- Two recipes that a single sentence can describe are one recipe. Google Play's Repetitive Content
  policy reaches the whole account, so the recipes must differ in category and job.
- No claim without a label, and no number that was not measured, quoted from a repository file, or
  explicitly marked as an assumption.
