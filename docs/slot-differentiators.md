# Slot differentiator contract — `differentiators.json`

Every slot states **why this app is worth installing**, one promise at a time, in a file a program
reads. This document is the specification; it is enforced by
`scripts/check-differentiators.mjs`, `scripts/new-slot.mjs` refuses to create a slot without a
valid contract, and `scripts/verify.mjs` fails a slot whose promises are unresolved. All four
facts are stated here because a contract nobody reads is a convention, and a convention is what
this file replaced.

## Why this file exists

Recon does not only answer "can this earn". It answers "why would anyone install this". The answer
is a list: a complaint every incumbent shares, a job nobody serves, a number in a review, a
workflow the category forces users to work around. That list *is* the product's plan, and it is the
first thing implementation loses. A builder starts with twelve reasons and finishes with seven,
because four were hard and one needed an API it did not want to add, and then every gate returns
PASS — the APK builds, the store pack is complete, the economics plan parses — and the slot is
called done.

The factory has no gate that can see that, because every existing gate measures **form**. None of
them knows what the app promised to be. This file is what the app promised to be, and the gate is
what makes the promise expensive to break.

The failure is not dishonesty. It is the ordinary gravitational pull of a build: the easy promises
ship first, the hard ones get a `// TODO`, and the end of the work is declared by whoever is
tired. `deferred` is the state that makes that visible instead of invisible, and it is why a slot
whose promises are unfinished **cannot** pass its own gate.

## The contract

`differentiators.json` lives at the slot root. It is written by `scripts/new-slot.mjs` from a plan
under `docs/ideation/plans/`, so it exists before the first line of UI — the same rule
`economics.json` follows, for the same reason: a list written after the build only contains what
was built.

```json
{
  "schemaVersion": 1,
  "slot": "<the slot's kebab-case slug>",
  "reconRef": "docs/research/<file>.md#<section>",
  "jobCoverage": {
    "before": { "refusal": { "reason": "<why this job has no before stage>", "decidedBy": "scout", "recordedAt": "<YYYY-MM-DD>" } },
    "during": { "claimId": "long-high-demand-runs" },
    "after":  { "claimId": "exports-the-runs" }
  },
  "claims": [
    {
      "id": "long-high-demand-runs",
      "weight": "core",
      "jobStage": "during",
      "depthFamily": "core_operation",
      "promise": "<what the user gets here that the incumbents do not give them>",
      "evidence": {
        "strength": "MEASURED",
        "source": "docs/research/<file>.md#<section>",
        "finding": "<what recon actually observed, with the number or the quote>"
      },
      "status": "shipped",
      "proof": {
        "type": "test",
        "ref": "app/src/test/java/<package>/<Test>.kt",
        "symbol": "longRun",
        "assertion": "<what a reader runs, and what they see happen>"
      }
    },
    {
      "id": "exports-the-runs",
      "weight": "supporting",
      "jobStage": "after",
      "depthFamily": "data_out",
      "promise": "<the handoff: the result leaves the device in a form the user's other tool reads>",
      "comparison": {
        "incumbent": "<package or title>",
        "limitation": "<what that app does not do>",
        "source": "docs/research/<file>.md#<section>"
      },
      "evidence": { "...": "as above" },
      "status": "shipped",
      "proof": {
        "type": "code",
        "ref": "app/src/main/java/<package>/<File>.kt",
        "symbol": "shareCsv",
        "assertion": "<what a reader runs, and what they see happen>"
      }
    }
  ]
}
```

| Field | Rule |
|---|---|
| `schemaVersion` | Exactly `1`. |
| `slot` | The slot's kebab-case slug. The gate checks it against the directory name, so a contract copied from another slot fails instead of passing as this slot's recon. |
| `reconRef` | `docs/research/<file>.md` or `docs/ideation/<file>.md`, optionally with `#section`. The file must exist. |
| `jobCoverage` | Binds each of the job's three stages — `before`, `during`, `after` — to a claim id or a written refusal. `during` is never a refusal, and `after` is refusable only by the owner. See "The job's shape" below. |
| `claims` | At least `3` promises, at least `2` of them `core`. |
| `claims[].id` | Kebab-case, unique. It is named in gate output, so a drop is reported by name. |
| `claims[].weight` | `core` or `supporting`. A `core` promise is one of the reasons the app was selected; losing it changes what the product is. |
| `claims[].jobStage` | `before`, `during` or `after`. Which part of the user's job this promise serves. |
| `claims[].depthFamily` | One family from the closed set in "The job's shape". A *connective* family is a handoff that counts toward the floor. |
| `claims[].promise` | One sentence: what the user gets here that the existing apps in this category do not give them. |
| `claims[].comparison` | Optional. `{ incumbent, limitation, source }` — the app this promise beats and what that app does not do, with the recon source. Printed for a human, never auto-passed. |
| `claims[].evidence.strength` | `MEASURED`, `ON RECORD`, `INFERRED` or `UNVERIFIED` — the labels `.cursor/rules/ideation-gate.mdc` already requires. |
| `claims[].evidence.source` | The recon document the claim came from. The file must exist, so deleting the proposal that justifies a promise breaks that promise's gate. |
| `claims[].evidence.finding` | What recon observed. A `MEASURED` finding must contain a number — a measurement nobody took is not a measurement. |
| `claims[].status` | `shipped`, `narrowed`, `dropped` or `deferred`. |

None of the free-text fields may be a placeholder. The validator rejects the literal values `tbd`,
`todo`, `n/a`, `none`, `unknown` and `placeholder`; any text containing `example.com`; angle-bracket
text such as `<who pays>`; any text containing `lorem`; and three or more consecutive `x`
characters as a whole word. The patterns are **imported** from `scripts/lib/slot-economics.mjs`
rather than restated, because a second copy of a rule is a second thing to drift.

## The job's shape — stages, families and the connective floor

A claim is not only a promise. It also says **where in the user's job** it sits and **what kind of work**
it does, so the list cannot be three ways to edit a record and nothing that finishes the job. The
normative rule is `.cursor/rules/app-depth.mdc`; this section is the field contract that agrees with it.

- **`jobStage`** is one of `before`, `during` and `after` — what the user brings or prepares, the act
  the app is named for, and what they do with the result.
- **`depthFamily`** is one family from a closed set: `core_operation` (the middle, never a handoff), the
  *connective* families `data_in`, `data_out`, `durability`, `reminder`, `widget`, `find` and `bulk`,
  and the reportable-only `insight`, `speed`, `fit`, `access`, `collaboration` and `trust`. The exact
  token a connective family's proof artifact must contain is in the rule, not repeated here —
  `data_in` brings the user's own content in through whichever door the job uses: the document picker or
  the clipboard, the gallery's `PickVisualMedia` picker, or a camera capture (`TakePicture` /
  `ACTION_IMAGE_CAPTURE`). Playing the user's own media is **not** a handoff, because nothing crosses the
  app's boundary; the camera is, because the captured bytes enter the app.
- **`jobCoverage`** binds each of the three stages to a `claimId`, or — where the job genuinely lacks
  that stage — to a `refusal` carrying a `reason`, a `decidedBy` and a `recordedAt`. **`during` is never
  refusable**: it is the act the app is named for, and refusing it means there is no app. `before` may be
  refused by the scout, with the reason it measured. **`after` may be refused only by the owner, in his
  own words** — the handoff is the most valuable capability the contract promises, so giving it up is a
  product decision recorded exactly as weakening a `core` promise is: `decidedBy` is `"owner"` and the
  refusal carries the same `ownerDecision { words, recordedAt }`. A scout-recorded `after` refusal, or a
  missing or empty owner record, is a failure naming the field. On any stage the `reason` is real text,
  not a token.
- **The connective floor.** At least one `shipped` claim must be a connective family whose proof
  artifact actually contains that family's API. Three ways to edit a record and no way to get it out is
  the defect the floor exists to stop.
- **`comparison`** names the incumbent a claim beats and what it does not do. It is printed for a human,
  never auto-passed, because its truth is a claim about a competitor that only recon can source.

Applicability is **derived, not hand-listed**: `scripts/core/baseline.mjs` reads the slot's creation
date from the registry, so the slots that predate this section carry their findings as a printed note
while every slot created after it is gated.

## Status is the whole mechanism

| `status` | What it means | What it requires |
|---|---|---|
| `deferred` | Not built yet. Every promise starts here. | `deferredReason` saying what is missing. **Fails the slot gate.** |
| `shipped` | Delivered. | `proof` that resolves inside the slot. |
| `narrowed` | Delivered in a weaker form than promised. | `proof` for the surviving part, plus `narrowedTo` and `lostCoverage`. On a `core` promise, also `ownerDecision`. |
| `dropped` | Not delivered. | `droppedReason`. On a `core` promise, also `ownerDecision`. |

`deferred` is not an answer, it is a queue. `scripts/check-differentiators.mjs --allow-deferred`
exists so a builder can read progress mid-flight; the slot gate does **not** pass that flag, so
`scripts/verify.mjs` fails while any promise is deferred, and the owner is never asked to
approve a revision that has not accounted for its own reason list.

### `ownerDecision`

Weakening or losing a `core` promise is a decision about what the product is, so it is the owner's
to make, and it is recorded in his words:

```json
"ownerDecision": { "words": "<the owner's own words>", "recordedAt": "2026-09-26T19:00:00.000Z" }
```

This is the same discipline as the release approval (`.cursor/rules/authorization.mdc`,
`scripts/lib/release-approval.mjs`): an approval exists only in the owner's own words, never
inferred from an agent's confidence. A `supporting` promise needs no owner decision — it needs a
reason in writing, because the point is that no promise disappears silently. The same record is what
a refusal of the `after` stage carries — the owner's decision to ship without the handoff — and it is
validated by the same helper, so the two paths cannot drift apart.

## Proof, and why it is typed

A `proof` is what makes "shipped" falsifiable. It is not a checkbox: it names a file inside the
slot and, for three of the four types, a symbol that must still be in that file. Deleting the code
that kept the promise fails the gate, which is what separates this from a status field an agent can
set.

| `proof.type` | `ref` must live in | `symbol` |
|---|---|---|
| `test` | `app/src/test/` or `app/src/androidTest/` | Required; must appear in the file. |
| `code` | `app/src/main/` | Required; must appear in the file. |
| `resource` | `app/src/main/res/` | Required; must appear as `name="<symbol>"` in the resource XML. |
| `capture` | `store/assets/` | Not required; the frame itself is the evidence. |

A `capture` proof points at a committed store asset, because `captures/` is not committed: a proof
that lives only on the machine that ran the capture is not a proof.

| Field | Rule |
|---|---|
| `claims[].proof.assertion` | The sentence a reader can check: what they run, and what they observe. |
| `claims[].proof.ref` | Slot-relative, must not escape the slot, must exist. |

## Enforcement

- **`scripts/new-slot.mjs` requires `--differentiators <path>`.** The plan is validated with
  `parseDifferentiators`, then written verbatim to the slot root as `differentiators.json`, and its
  promise count is recorded in `.kiln-state.json` and `registry/apps.json`. A slot cannot be
  scaffolded without one, exactly as it cannot be scaffolded without an economics plan.
- **`scripts/check-differentiators.mjs --slot <slug>`** is the gate: schema, then the promises
  resolved against the slot's own files, then the `deferred` rule. It prints `VERDICT: PASS` or
  `VERDICT: BLOCKED` and exits non-zero on failure.
- **`scripts/verify.mjs` invokes it for every slot**, and turns on a declaration rather than
  on a file being present — the file is looked for on disk *and* in git, so deleting
  `differentiators.json` in the working tree does not switch the check off. A slot with no contract
  at all predates this gate: it is reported `UNVERIFIED` (never PASS), on the same reasoning as
  `verifyEconomics`.
- **The job's shape is checked by the same gate.** `slot.differentiators` (in
  `scripts/core/policy.mjs`) verifies the three stages, that `during` is never a refusal, that a `before`
  refusal carries its reason and decider while an `after` refusal is the owner's in his own words, that
  at least one `shipped` claim's proof artifact contains its connective family's API, and that a
  `comparison` names its source. Its fixture suite proves each leg can fail, so the floor is a check
  rather than a hope.
- **The contract is a build input for the release approval.** It is not in
  `scripts/lib/release-approval.mjs`'s exclusion list, unlike `economics.json` and
  `capabilities.json`, and that is deliberate: the digest is of "the app the owner reviews", and
  the reason list is part of what he is approving. Editing a promise after approval lapses the
  approval, which is the correct outcome for changing what was promised.

## What this gate does not prove

It cannot prove a promise is *satisfied* — that the test which carries it asserts the right thing,
that the captured frame shows what the assertion claims, or that the promise is one users care
about. It proves every promise is either backed by an artifact that exists and still contains the
symbol the promise names, or has been explicitly given up in writing, with the owner's words when
the promise was load-bearing. The judgement that a promise was worth keeping stays with the owner,
and this gate's job is to make sure he is asked.

Like the money gate, it also cannot measure the list's **quality**. The floors — three promises,
two core, one number in every `MEASURED` finding — stop a degenerate file from passing. They cannot
stop an agent from writing three easy promises. That is why the list is authored at recon, before
the app exists, and reviewed by the owner at phase 1: the defence against a weak list is that a
human who wants the app to win is reading it.
