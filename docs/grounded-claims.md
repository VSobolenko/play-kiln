# The grounded-claims duty

Every factual sentence the factory produces carries the evidence that makes it true, and every
recommendation names the alternative it rejected. The normative rule is
`.cursor/rules/grounded-claims.mdc`; this document is the human-facing contract for it — why it exists,
what the labels demand, how a claim ledger is carried and rejected, and, stated plainly, what the one
sensor can and cannot check.

## Why it exists

The owner's report, recorded 2026-10-02, after reading the work of nearly two slots: a scout said a
market pocket was good, the UI/UX work said the design was good, and **neither answered with facts**.
Each had answered from memory and presented the memory as a finding. That is the defect this rule
stops, and it is the factory's most expensive one precisely because it is invisible — a confident
sentence and a measured one look identical in a report, and only one of them is worth acting on.

The vocabulary was already here: `docs/ideation/README.md` prices a demand reading as `MEASURED`,
`ON RECORD`, `INFERRED` or `UNVERIFIED`, `docs/slot-differentiators.md` types a promise's evidence the
same way, and `docs/research/measured-facts.json` records when each external rule was measured. What
was missing was the requirement itself — that the label travels with the claim, in every report, from
every worker, and that an orchestrator rejects one that does not. That requirement is
`.cursor/rules/grounded-claims.mdc`. The labels themselves are defined **there** and are not restated
here, because `verification-discipline.mdc`'s "one source of truth, imported — never restated" governs
documents too.

## The four labels, and what each demands

The definitions are the rule's; this table is the shape a reader and an orchestrator hold in mind, not
a second rule.

| Label | You may use it only when | What travels with it |
|---|---|---|
| `MEASURED` | you ran a command, read a file, or observed the artifact **in this session** | the command and its observed output, or the `path:line` |
| `ON RECORD` | a named source states it and you read that source | the source, and the number or quote it gave |
| `INFERRED` | it follows from other labelled facts | the facts it follows from, and the step between them |
| `UNVERIFIED` | you did not check it | the exact command or file that would decide it, and who can run it |

`UNVERIFIED` is a result, not a failure. It is the honest answer to a question that could not be
settled, and it is the label that makes a gap visible instead of filling it with a plausible default.
The rule's **What is banned** section is the source for the failures the labels exist to stop — an
invented fact, a gap filled with the plausible default, a document paraphrased as if cited, a verdict
on one's own artifact, a recommendation with no rejected alternative — and none of them is reproduced
here.

## How a claim is carried, and how it is rejected

A worker's session is not visible to the orchestrator, so the evidence travels with the claim. Every
worker report ends with a **claim ledger**: one row per factual claim, carrying its label and, for a
`MEASURED` row, the command that produced it or the `path:line` it was read from.

```text
CLAIM                                        LABEL       EVIDENCE
the Scaffold discards its innerPadding       MEASURED    Surface.kt:24
predictive back is declared                  MEASURED    `rg enableOnBackInvokedCallback app/src/main/AndroidManifest.xml`
the wedge will convert                       UNVERIFIED  no priced comparable found; needs one to decide
```

An orchestrator **rejects** a report whose rows carry no label, whose `MEASURED` row names no command
and no `path:line`, or whose conclusion rests on a row labelled `UNVERIFIED`. A conclusion is only as
strong as its weakest load-bearing row, so a confident paragraph whose load-bearing fact is unlabelled
is not a report: it is sent back for its ledger rather than acted on. The dispatch side of this lives
in `.cursor/rules/parallel-execution.mdc` and `harness/dual-agent.md` — every brief states the duty,
because a subagent never sees the orchestrator's conversation and cannot infer it.

## Where the duty is enforced

The rule states that it is behavioural, and so does its sensor. There is exactly one program:
**`harness.groundedClaims`** in `scripts/core/harness.mjs`, run by `node scripts/verify.mjs` in the
`invariants` group. It is a **declared-duty sensor**, and it checks three declarations:

- that `.cursor/rules/grounded-claims.mdc` loads — it declares `alwaysApply: true`, so Cursor attaches
  it to context rather than leaving it reachable only by an agent that already knows to ask;
- that every role card under `harness/roles/` names the rule, so a dispatched role is told to follow
  it; and
- that every agent card under `.cursor/agents/`, `.codex/agents/` and `.claude/agents/` names the rule
  or delegates to a role card that does.

Its inline fixture suite is `harness.groundedClaimsFixtures`, which drives the sensor over synthetic
card sets whose verdict is known — a card that drops the rule must fail — so the sensor cannot quietly
stop being able to fail.

## What the sensor cannot check, stated rather than implied

It cannot check whether one particular sentence is true. It proves the duty is **declared** — that the
rule loads and that the cards which dispatch work bind it — and nothing more. A fabricated ledger, a
`MEASURED` row whose command was never run, and an observation that does not describe what was on the
screen all pass it, because no program reads a sentence and decides whether it is honest. That
judgement stays with the reader, which is exactly why the ledger is written down: **a false claim
beside its label is a claim a later reader can contradict**, and the defect this rule was written from
was invisible only because nothing was written down.

## Which cards bind it

Every card a worker is dispatched in names the rule, and the sensor requires it:

- `harness/roles/builder.md`, `harness/roles/critic.md`, `harness/roles/scout.md`,
  `harness/roles/money-owner.md` and `harness/roles/verifier.md` — the five roles a worker runs as.
- the agent mirror cards under `.cursor/agents/`, `.codex/agents/` and `.claude/agents/` — each names
  the rule or delegates to a role card.
- `.cursor/rules/parallel-execution.mdc` and `harness/dual-agent.md` — the dispatch contract, where a
  brief states the duty and a report is rejected without its ledger.

## Sources / related

- `.cursor/rules/grounded-claims.mdc` — the normative rule: the labels, what is banned, the ledger.
- `harness/dual-agent.md` — the worker lifecycle and acceptance gate the ledger is read at.
- `.cursor/rules/parallel-execution.mdc` — the dispatch contract that states the duty in every brief.
- `docs/research/measured-facts.json` — the measured-fact ledger: what the factory believes about the
  world, when it was measured, and the program that enforces it.
- `docs/ideation/README.md` — where the labels are used to price an idea's demand evidence.
