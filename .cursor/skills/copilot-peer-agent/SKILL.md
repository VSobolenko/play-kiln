---
name: copilot-peer-agent
description: Delegate work to GitHub Copilot CLI as a second peer worker channel beside Codex, via node scripts/copilot-run.mjs. Use for model diversity or when Codex is rate-limited.
disable-model-invocation: true
---

# Copilot peer agent

GitHub Copilot CLI is a full agent harness on a second peer model family, invoked as a durable
worker through `node scripts/copilot-run.mjs`. In PlayKiln it is a **second worker channel, not a
replacement for Codex**: Codex remains the co-lead.

## Must read

- Root `AGENTS.md`
- `harness/copilot-workers.md`
- `harness/dual-agent.md`

## Commands

```text
node scripts/copilot-run.mjs run    --id <id> --prompt-file <path> [options]
node scripts/copilot-run.mjs status [--id <id>]
node scripts/copilot-run.mjs list
node scripts/copilot-run.mjs last   --id <id> [--chars 4000]
node scripts/copilot-run.mjs log    --id <id> [--lines 60]
node scripts/copilot-run.mjs resume --id <id> --prompt-file <path>
node scripts/copilot-run.mjs probe  [--model <name>]
```

`run` needs `--id` and `--prompt-file`; `resume` continues **that worker's** recorded session.
Options: `--cwd`, `--model`, `--effort`, `--permissions tools|tools+paths`, `--timeout-min`,
`--label`, `--agent`.

## Accountability (mandatory)

1. **Prevent** — durable rules in `AGENTS.md`; task-local rules in the prompt file.
2. **Contain** — disjoint file lists, or a git worktree when parallel with Cursor.
3. **Verify** — `node scripts/verify.mjs` + read the diff. Self-report ≠ evidence.
4. **Reject** — resume the same worker id with concrete failures.

## Ledger verdicts

The same four as Codex, because they are the same module: `DONE` / `FAILED(<code>)` /
`BLOCKED(<signal>)` / `LOST`.

## The ladder

`Codex worker → GitHub Copilot CLI worker → Cursor \`generator\` subagent → orchestrator`.
Copilot is the **first fallback rung** for the Codex-class failures; its own `RATE_LIMITED` /
`AUTH_ERROR` falls through to `generator`.

## Rules that are easy to break

- **Judge success by the exit code, never by "stderr is non-empty"** — on Windows the PowerShell
  shim writes a `NativeCommandError` block to stderr even on a successful run.
- **Never raise cost**: do not pass `--model` or `--effort` unless the caller asked.
- **No `copilot` MCP server**: nothing shows Copilot exposing an MCP server mode; do not add one to
  `.cursor/mcp.json`.

## Concurrency hazard

Do not let Copilot and Cursor write the same files in one worktree.
`secret-guard` does not intercept Copilot writes — scan afterwards.
