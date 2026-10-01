---
name: codex-peer-agent
description: Delegate work to Codex as an equal peer agent (Build, Store, or Verify) via Codex MCP. Use for model diversity, Play asset generation, or parallel implementation in a worktree.
disable-model-invocation: true
---

# Codex peer agent

Codex is a full agent harness on a different model family. In PlayKiln it is a
**peer**, not a subordinate reviewer.

## Must read

- Root `AGENTS.md` (Codex auto-reads it)
- `harness/dual-agent.md`
- `.cursor/skills/dual-agent-peers/SKILL.md`

## Accountability (mandatory)

1. **Prevent** — durable rules in `AGENTS.md`; task-local rules in
   `developer-instructions`.
2. **Contain** — code writes in a git worktree when parallel with Cursor.
3. **Verify** — `node scripts/verify.mjs` + read the diff. Self-report ≠ evidence.
4. **Reject** — `codex-reply` on the same `threadId` with concrete failures.

## Tools

- `codex` — start session (`prompt`, optional `sandbox`, `developer-instructions`)
- `codex-reply` — continue with `threadId`

## Good peer splits

| Codex | Cursor |
|-------|--------|
| Store listing + icons | Build Compose screens |
| Build data layer in worktree | Build UI on main slot tree |
| Verify / red-team Play claims | Fix findings |

## Concurrency hazard

Do not let Codex and Cursor write the same files in one worktree.
`secret-guard` does not intercept Codex writes — scan afterwards.
