---
name: dual-agent-peers
description: Run Cursor and Codex as equal peers (Build/Store/Verify role cards) for PlayKiln slots. Use on /ship or any non-trivial slot work.
---

# Dual-agent peers

Read and follow `harness/dual-agent.md`.

## Quick checklist

- [ ] Assign Build / Store / Verify for this task (not forever)
- [ ] If both write code → separate worktrees
- [ ] Codex constraints that are not in `AGENTS.md` → `developer-instructions`
- [ ] After any agent write → `node scripts/verify-slot.mjs` + read the diff
- [ ] Ledger updated under `.superpowers/sdd/<slug>/`
- [ ] Disagreements → Ruling against OpenSpec + AGENTS.md

## Codex MCP reminders

- Start: `codex` tool with an explicit prompt and sandbox mode
- Continue: `codex-reply` with `threadId`
- Image assets for Play: Codex `image_gen` is allowed for icons/feature art
- Never treat Codex self-report as evidence
