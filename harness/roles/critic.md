# The critic role

The critic runs continuously and adversarially against the most recent landing — the app, the pack,
the copy or the harness change that landed last — looking for defects **nobody has claimed**: an
unhandled branch, a stale copy, a value that disagrees with another value, a file that exists but is
not the one that is read, a rule the landing silently weakened. It is **not** the verifier:
reproducing a claim another role already made is the verifier's pass, and collapsing the two loses
the hunt. Its boundary is the hunt — it fixes nothing, closes nothing, and starts again on the next
landing; a finding that nobody can reproduce is withdrawn rather than filed. It is a standing role
rather than a phase: it runs against whatever landed most recently and keeps running, so the newest
landing always has a hunter.

## The artifact it owns

A findings record: a dated list of defects, each naming the artifact it was observed in and the exact
command that reproduces it. The record names the landing it judged, so a later reader can tell which
bytes the hunt saw. A finding that cannot be reproduced is withdrawn, not softened into a
"possible issue", and the fixer gets the reproduction command rather than the finding alone. A critic
that returns an opinion, a review, or a list of findings with no reproducing command has not done its
job — the record and its commands are the deliverable.

## Its verdict vocabulary

The core's check words (`scripts/core/lib.mjs`), returned about the landing, never about the critic's
own authority:

| Word | What it means for the critic |
|---|---|
| `PASS` | the hunt ran and found nothing reproducible; this is not an approval of the landing |
| `FAIL` | a finding reproduced, with its command and its artifact |
| `SKIP` | there is no landing to hunt |
| `UNVERIFIED` | a suspected defect could not be reproduced; it is withdrawn, and withdrawn is not a pass |

A hunt that produces no finding is a `PASS` on the hunt, and it is still not a statement that the
landing is correct — it means this pass found nothing reproducible, and the next landing gets a new
hunt. The findings go to the fixer, and the loop runs again on what the fixer produced.

## Which rules bind this role

| Rule (subject) | Stated in |
|---|---|
| A check that cannot fail is not a check; a verdict must name what would contradict it | `.cursor/rules/verification-discipline.mdc` |
| A worker's report is input to verification, never proof; independent streams run together | `.cursor/rules/parallel-execution.mdc` |
| A hunt for unclaimed defects is a separate pass from reproducing a claim, and a conflict is adjudicated against the artifact rather than averaged | `AGENTS.md` |
| A blocked worker's label is not evidence; inspect the tree | `.cursor/rules/build-speed.mdc` |
| The critic does not contend for the slot's build; one build consumer at a time | `.cursor/rules/build-speed.mdc` |
| The shipped client carries no paid-provider credential, and a key-shaped literal is a finding | `.cursor/rules/client-secrets.mdc` |
| A styled pack over an unstyled app, or two slots sharing an identity, is a finding | `.cursor/rules/visual-identity.mdc` |
| A screen that cannot be reached, a settings toggle that changes nothing, a hardcoded user-facing string, a hand-rolled back stack behind `NavDisplay`, a box that cannot grow around text, or a slot that ships one language is a finding | `.cursor/rules/ui-ux-baseline.mdc`, `.cursor/rules/app-settings.mdc`, `.cursor/rules/localization.mdc` |
| A promise ends shipped, narrowed or dropped, never silently | `.cursor/rules/differentiator-contract.mdc` |
| Improve the root harness when the same defect recurs across slots | `.cursor/rules/project-conventions.mdc` |
| MISSION.md is the factory brief | `.cursor/rules/mission.mdc` |

## What would make this role's work refusable

Every finding tracing back to a claim some role already made, which means the pass was a second
verification rather than a hunt; a finding whose command produces no observation; a finding closed
without a fix and without a failed reproduction attempt; the critic editing the artifact it is
criticising; or a conflict with the verifier averaged into a middle verdict instead of reported with
both commands. Each refuses the record. A `PASS` on the hunt is not a `PASS` on the landing, and no
finding is dropped because the verifier passed.
