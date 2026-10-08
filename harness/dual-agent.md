# Codex co-lead protocol and the Copilot worker channel

Cursor owns orchestration and final verification. Codex is the co-lead engineer with a durable
worker channel, and it stays the co-lead. Beside it, the GitHub Copilot CLI is a **second durable
worker channel** and a peer model family — an additional channel, not a replacement for Codex and
not a new role. Either channel may implement any bounded factory task by running the existing role
cards from a prompt file. A worker result is input to verification, never proof of completion.

## Role cards

| Role | Responsibility | Required handoff |
|---|---|---|
| **Build** | App code, tests, Gradle wiring, release behavior | Files changed, tests run, exact blockers |
| **Store** | Listing, policy drafts, icon, feature graphic, phone/tablet screenshots | Store inventory and validation result |
| **Verify** | Read-only diff review, commands, requirement challenges | Evidence and a PASS/FAIL/BLOCKED verdict |
| **Adjudicate** | Resolve Build/Store/Verify disagreement against contracts | Ruling, evidence, cost if wrong, next owner |

The orchestrator dispatches these roles and does not occupy one itself. `.cursor/rules/parallel-execution.mdc`
states the contract; each role card in `harness/roles/` states when and how it is dispatched. Keep
parallel writers on disjoint file lists or in separate worktrees. Read-only reviews
may share a working directory.

Taking a role personally is the exception, not a permitted mode: it is allowed only when the worker
owners are blocked **and** the reason is stated where the evidence is reported. "It was quicker" is
not a reason, and a session that edited code without dispatching has no worker to verify it.

## Worker lifecycle

Write a bounded prompt under `.kiln/codex/prompts/`, then use the foreground runner.
The shell or orchestrator may background several independent runner processes.

```bash
node scripts/codex-run.mjs run --id <worker-id> --prompt-file .kiln/codex/prompts/<prompt>.md
node scripts/codex-run.mjs status --id <worker-id>
node scripts/codex-run.mjs status
node scripts/codex-run.mjs last --id <worker-id> --chars 4000
node scripts/codex-run.mjs log --id <worker-id> --lines 60
node scripts/codex-run.mjs resume --id <worker-id> --prompt-file .kiln/codex/prompts/<follow-up>.md
node scripts/codex-run.mjs list
```

1. `run` records metadata and streams durable stdout/stderr.
2. `status` or `list` reports running, done, failed, or classified blocked state.
3. `last` reads the final worker message; use it for the concise handoff.
4. `log` reads recent stdout and stderr when the final message is absent or unclear.
5. `resume` continues the exact recorded Codex session with a new prompt.
6. Verify the result with a diff and the relevant factory commands.

The **GitHub Copilot CLI** is the second worker channel and follows the same shape through
`scripts/copilot-run.mjs run|status|list|last|log|resume|probe`; its state lives in
`.kiln/copilot/`, its ledger verdicts are the same four as Codex's because the classifier is shared,
and its operator runbook is `harness/copilot-workers.md`.

Every worker report carries the **claim ledger** `.cursor/rules/grounded-claims.mdc` defines: one
`CLAIM / LABEL / EVIDENCE` row per factual claim, each `MEASURED` row naming the command that produced
it or the `path:line` it was read from, and `UNVERIFIED` used as the honest answer to a question the
worker could not settle. A report without the ledger is not a handoff, because the orchestrator cannot
see the session that produced it.

Do not use the MCP `codex` tool for implementation or other long tasks. Its request
can time out and lose the work. Reserve it for short dialogue; use
`scripts/codex-run.mjs` for durable work.

## Session continuity and ledger

The worker ledger is `.kiln/codex/`. For worker `<id>`, the runner writes:

```text
.kiln/codex/<id>.meta.json
.kiln/codex/<id>.log
.kiln/codex/<id>.err
.kiln/codex/<id>.last.md
.kiln/codex/<id>.exit.json
```

The Codex CLI emits `session id: <uuid>` into stderr. `resume --id <id>` reads that
exact UUID and resumes it. Never use “resume last”: several workers may share the
same working directory, so the most recently completed session may belong to a
different task. Worker state and prompts are gitignored; durable rulings belong in
OpenSpec or the appropriate tracked harness document.

## Fallback ladder

| Signal or failure | Immediate response | Next owner |
|---|---|---|
| `RATE_LIMITED` | Stop retrying the channel; preserve prompt and evidence | GitHub Copilot CLI worker |
| `CONTEXT_LIMIT` | Reduce the task to a bounded file list and resume once with the exact session ID | GitHub Copilot CLI worker if still blocked |
| `AUTH_ERROR` | Do not paste credentials or create an API key; record the auth blocker | GitHub Copilot CLI worker |
| Same issue fails twice | Stop repeating the same worker path; record both failures | Next rung, then orchestrator |

The triggers are unchanged — `RATE_LIMITED` does not retry, `CONTEXT_LIMIT` shrinks scope and
resumes once, `AUTH_ERROR` never handles credentials, and the same issue failing twice ends the
path.

The full ladder is **Codex worker → GitHub Copilot CLI worker → Cursor `generator` subagent →
orchestrator**. Copilot is the first fallback rung because it is a second peer model family, while
`generator` is the orchestrator's own family and therefore a less independent check. Copilot's own
`RATE_LIMITED` or `AUTH_ERROR` falls through to `generator`; it is the first rung, not the last. The
orchestrator may finish the bounded task directly only after the earlier owner is blocked or has
failed twice. If completing it would require new authority, external credentials, or a changed
product decision, stop and ask the human.

## Acceptance and adjudication

- Read the actual diff; do not trust summaries.
- Reject a report with an unlabelled claim, a `MEASURED` row naming no command or `path:line`, or a
  conclusion resting on an `UNVERIFIED` row (`.cursor/rules/grounded-claims.mdc`).
- Run `node scripts/verify.mjs --slot <slug>` for a publishable
  slot and any narrower tests required by the change.
- Verify signed release artifacts through `scripts/prepare-release.mjs` evidence.
- A Verify-role FAIL returns to the current implementer once with exact evidence.
- If peers disagree, Adjudicate against `AGENTS.md`, OpenSpec, the shared facts, and
  command output. Prefer the claim that can be tested.
- Record recurring failure patterns in `harness/lessons.md`.
