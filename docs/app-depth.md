# The depth contract

Every app the factory ships carries the user's **whole job** — what they bring or prepare, the act the
app is named for, and what they do with the result — not only the record-keeping middle. The normative
rule is `.cursor/rules/app-depth.mdc`; this document is the human-facing contract for it: why it
exists, the three stages, the depth families, what a program checks and what a human audits, and,
stated plainly, what no program can prove. The rule's **What is banned** section is not reproduced
here, because `verification-discipline.mdc`'s "one source of truth, imported — never restated" governs
documents too.

## Why it exists

The owner's report, recorded 2026-10-02, after nearly two slots: **"we get a raw app that can do only
one thing."** His examples are exact — a teacher's gradebook creates, edits and deletes students and
nothing else; a calculator calculates and nothing else. **"Where is the saving, where is the search,
where are the engineering functions?"** His standard is explicit: *"I want quality apps, and quality
matters to me more than the feature count."*

The defect has a name: **partial flow** — the middle of the user's job is implemented and the handoffs
are not, so the app is usable and cannot be *finished* with. The measured cost of the missing half is in
`docs/research/2026-10-02-user-review-depth-signals.md`: **15.13 %** of low-star reviews are *feature
requests* (Khalid et al. 2014), and across independent corpora **17–23 %** of all reviews are feature
requests — the second-most-frequent low-star reason in the best-measured dataset (**MEASURED**,
§1.1–1.2). The ranked extras users name are led by **export / backup / data portability** — the only one
that is itself a paid product in four commercial apps and a named upgrade reason in a paying app's 5★
reviews (**MEASURED**, §3) — then sync, search/filter, reminders, widgets, competitor import, bulk
operations, statistics and templates.

Why the factory kept producing it is mechanical, and measured in this repository. The scope of a slot
**is its promise list**: `differentiators.json` is what the builder is obliged to deliver, and a
contract that names only the middle of the job produces exactly an app that does only the middle —
while every gate reports PASS. Two instances:

- `apps/2026-09-26-teacher-gradebook` spends a `core` promise on `edit-every-record`, proved by
  `deleteAssignment` — a `core` promise spent on the record-keeping middle.
- `apps/2026-10-01-lawn-round-log` builds `domain/Csv.kt` with **no `ACTION_SEND`, no
  `ACTION_CREATE_DOCUMENT` and no `FileProvider`** anywhere in its `app/src/main`, so the CSV can be
  built and never leaves the phone.

## The three stages

Every slot decomposes the user's job into what happens **before**, **during** and **after** the core
act, and the contract must cover all three. The job is not the act; the job is everything the act sits
inside.

| Stage | What it is | For the gradebook, for the calculator |
|---|---|---|
| **before** | what the user brings or prepares — last term's data, a list they already have, a template, defaults | import a class list; remember the last expression type |
| **during** | the central act the app is named for | record a grade; evaluate an expression |
| **after** | what the user does with the result — keeps it, sends it, files it, is reminded of it, reviews it | export the term a parent can read; save/share the result |

`during` is never refusable — it is the act the app is named for, and refusing it means there is no
app. `before` may be refused by the scout, with the reason it measured. **`after` may be refused only by
the owner, in his own words.** The handoff is the most valuable capability the contract promises — it is
what makes the job finishable — so giving it up is a product decision, exactly as weakening a `core`
promise is, and it is recorded the same way: `decidedBy` must be `"owner"` and the refusal carries the
same `ownerDecision { words, recordedAt }`. A missing or empty owner record is a failure naming the
field, and a scout-recorded `after` refusal is refused by the gate.

## The depth families

Each claim declares its `depthFamily` from a closed set. The **connective** families are the handoffs:
they are what makes a job finishable, and they are the ones a proof can be checked against.

| `depthFamily` | What it is | Counts toward the handoff floor? |
|---|---|---|
| `core_operation` | the app's central act | **no** — this is the middle, not a differentiator |
| `data_in` | bring the user's own data or media in — a document, a gallery pick (`PickVisualMedia`) or a camera capture (`TakePicture`, `ACTION_IMAGE_CAPTURE`) — or an incumbent's export | yes |
| `data_out` | get the result off the device usable | yes |
| `durability` | the user cannot lose the work | yes |
| `reminder` | the app reaches the user | yes |
| `widget` | capture without opening the app | yes |
| `find` | find it again later | yes |
| `bulk` | act on many at once | yes |
| `insight` | see the whole picture | **no** — the research rates this presence-only |
| `speed`, `fit`, `access`, `collaboration`, `trust` | templates, personalisation, accessibility, sharing, no lock-in | **no** — reportable, never floored |

The exact token each connective family's proof artifact must contain (`ACTION_SEND`,
`ACTION_CREATE_DOCUMENT`, `FileProvider`, `PdfDocument`, …) is in `.cursor/rules/app-depth.mdc`; this
table is the shape, not a second copy of the rule. The camera is the `data_in` handoff the rule's
closed set previously did not name: the owner's decision of 2026-10-02 made the phone's free, local
capabilities — the picker, the gallery, the user's own media and the camera — legitimate, and a camera
capture brings the user's own content in exactly as a gallery pick does, so it carries its own tokens
(`TakePicture`, `ACTION_IMAGE_CAPTURE`). Playing the user's own media and the motion sensors are not
handoffs, because nothing crosses the app's boundary; they feed the act rather than carry it anywhere.

**At least one `shipped` claim must be a connective family whose proof artifact actually contains its
API.** Three ways to edit a record and no way to get it out is the defect the floor exists to stop.

Depth is the job done completely, not "add everything". The same research measures the opposite
failure: an unused feature charges maintenance rent — **under ~5 % engagement for a month** is a
removal candidate (**MEASURED**, `2026-10-02-user-review-depth-signals.md` §6) — and a requested
data-safety extra shipped badly (a cloud-sync race that wiped saves and dropped a top-3 game to #13)
did more damage than its absence. The handoffs the job actually has, never a settings screen full of
switches.

## What is machine-checked, and what a human audits

**Machine-checked** — `slot.differentiators`, with its fixture suite in `scripts/core/policy.mjs`: the
schema of every new field; that `jobStage` is one of the three and every stage resolves to a claim or a
refusal; that `during` is never a refusal; that a `before` refusal carries a reason, a decider and a date
while an `after` refusal is the owner's, in his own words; that at least one `shipped` connective claim
exists; and that its proof artifact contains the named token for its family. A refusal's `reason` is
rejected when it is a placeholder or a bare token, which is the most a predicate can honestly do — it
cannot read sincerity, so a human still audits every refusal.

**Human-audited, printed and never auto-passed** — the CRUD report (claims whose text reads as a bare
record operation), every refusal, and every `comparison`. The CRUD predicate is a **report, not a
gate**: `docs/research/2026-10-02-depth-encoding-and-detectability.md` measured that the repository's
own worst case (`edit-every-record`) escapes a lexical predicate because it carries a contrast clause,
so failing it would be a false positive on correct prose. The owner reads these at checkpoint 1.

## The honest limit

**It cannot prove the app is good, wanted or worth paying for** — and it is gameable: three invented
claims plus one `ACTION_SEND` satisfies it
(`docs/research/2026-10-02-depth-encoding-and-detectability.md` §4). What it removes is the *unnoticed*
partial flow: the slot whose contract never mentioned the handoff, so nobody ever asked whether the job
could be finished. It cannot prove a `comparison` is true (only that a source was named), cannot prove a
`proof` artifact *means* the promise, and cannot prove a refusal was justified. The judgement that
separates a rich app from a padded one stays with the owner at checkpoint 1.

In the factory's own terms: the normative text is `.cursor/rules/app-depth.mdc`, and the enforcement is
a program — a rule that only a document asserts is a rule nobody follows.

## Sources / related

- `.cursor/rules/app-depth.mdc` — the normative rule: the stages, the families, the floor, what is
  banned.
- `docs/research/2026-10-02-app-depth-anatomy.md` — the feature families and the "cut scope, not the
  job" reading.
- `docs/research/2026-10-02-user-review-depth-signals.md` — the review frequencies and the paid-extra
  evidence.
- `docs/research/2026-10-02-depth-encoding-and-detectability.md` — what a static scan can and cannot
  check, and the gaming limit.
- `docs/slot-differentiators.md` — the field contract (`jobStage`, `depthFamily`, `comparison`,
  `jobCoverage`).
- `.cursor/rules/differentiator-contract.mdc` — the rule that the promise list is a contract.
- `.cursor/rules/ideation-gate.mdc` — the budget section, corrected so the surface narrows and the job
  does not.
