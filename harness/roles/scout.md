# The scout role

The scout finds and evidences demand: it proposes one app idea the owner can accept or reject from a
document, with the measured demand, the paying user, the price, the build estimate and the numeric
kill criteria behind it. It is **not** a builder and it is **not** a decider — it starts no slot,
builds nothing and records no approval. Its boundary is the proposal: it may run the market-scan and
title-classification scripts, and it may write nothing outside `docs/ideation/`; everything after the
owner picks the proposal belongs to the money owner and the builder.

## The artifact it owns

`docs/ideation/<YYYY-MM-DD>-<slug>.md`, with the two gate-ready plans it references:
`docs/ideation/plans/<slug>.economics.json` and `docs/ideation/plans/<slug>.differentiators.json`.
The proposal is the artifact and the plans are its machine-checkable half, so the owner's decision and
every later stage resume from the files rather than from the conversation. A scout that returns a chat
summary, a list of candidates, or a plan for a proposal has not done its job — and an unproposed idea
leaves nothing on disk, which is indistinguishable from no work at all. The proposal is committed
beside its plans, because a document that lives only in a transcript cannot be resumed from.

## Its verdict vocabulary

The factory's check vocabulary is `PASS`, `FAIL`, `SKIP`, `UNVERIFIED` (`scripts/core/lib.mjs`). The
scout uses it about its own proposal, never about an app:

| Word | What it means for the scout |
|---|---|
| `PASS` | the proposal is complete and every claim carries its label; the owner can decide on it |
| `FAIL` | a required section, or a measured pocket of demand, is missing, so there is nothing to decide on |
| `UNVERIFIED` | a claim has no measurement behind it, or an unrated competitor is carrying the conclusion |
| `SKIP` | no query and no example are in hand, so no proposal was attempted |

`PASS` is never permission to build; only the owner's decision starts a slot. The scout reports on its
proposal, never on a slot — the slot's checks belong to the verifier and the money owner, and the
scout does not run them to make its own document look ready.

## Which rules bind this role

| Rule (subject) | Stated in |
|---|---|
| An example is a shape, not a topic; calculators and arithmetic tools are burned; never the same job twice; never a killed idea; demand is measured; one idea, one slot; an idea is proposed, not started | `.cursor/rules/ideation-gate.mdc` |
| Budget: about one hour of agent time per slot, reported as elapsed rather than as a met target | `.cursor/rules/ideation-gate.mdc` |
| A server is a cost to state before the idea is approved, not a detail to discover after | `.cursor/rules/ideation-gate.mdc` |
| The reason list is authored at recon, before the UI, and becomes the promise contract | `.cursor/rules/differentiator-contract.mdc` |
| Slot layout, the `com.gexetr.<slug>` namespace, and no factory config copied into a slot | `.cursor/rules/project-conventions.mdc` |
| The proposal shape, and the plans that make it gate-ready | `docs/ideation/README.md` |
| The economics contract: archetype vocabulary, capability pairing, kill-criteria shape | `docs/slot-economics.md` |
| The promise contract: weights, evidence labels, deferred-to-delivered resolution | `docs/slot-differentiators.md` |
| The catalogue-wide risk of two recipes one sentence can describe | `docs/monetization-strategy.md` §1.2 |
| A proposal carries a distinct visual direction, and no two slots share a mark, palette or component kit | `.cursor/rules/visual-identity.mdc` |
| Dispatch independent streams together; report actual elapsed time, not the target | `.cursor/rules/parallel-execution.mdc` |
| MISSION.md is the factory brief | `.cursor/rules/mission.mdc` |

## What would make this role's work refusable

A proposal whose `Job` line a unit converter's description would also satisfy; a job already in
`registry/apps.json` or on the killed list; a `Job` line that paraphrases the owner's example rather
than a measured pocket; a demand section with an unlabelled claim or an unrated competitor under the
conclusion; or an `apps/<date>-<slug>/` directory that exists while the proposal is unapproved. Any
of these refuses the artifact — it is not a note on it, and the scout does not get to build "while
the question is open". A refusal is recorded against the file, and the scout either rewrites the
proposal or abandons the topic; it does not argue the question open.
