# Dual-agent protocol (Cursor ∥ Codex)

Cursor and Codex are **peers**. Neither is permanently "the writer" or "the
reviewer". The controller (the agent the human is talking to) owns orchestration,
the ledger, and objective gates.

## Role cards (assign per task)

| Role | Responsibility |
|------|----------------|
| **Build** | Implement code, tests, Gradle wiring in the slot |
| **Store** | Play listing, privacy text, icons, screenshots, store/ |
| **Verify** | Independent check: diff read, verify-slot, challenge design |

Typical first split for a new app:

1. Cursor **Build** core screens + Codex **Store** listing/assets in parallel
2. Swap: Codex **Verify** (or Build remaining tasks) + Cursor **Verify**/fix
3. Both must agree the objective gate is green before claiming done

## Concurrency

| Situation | Rule |
|-----------|------|
| Two agents editing the same slot sources | Separate git worktrees; controller merges |
| One writes, one reviews | Same tree OK if reviewer is read-only |
| Codex MCP write | Prefer worktree; remember `secret-guard` does not see Codex writes — run `verify-slot` + secret scan after |
| Codex session continuity | Reuse `threadId` via `codex-reply` |

## Ledger

Path: `.superpowers/sdd/<slug>/progress.md`

Every dispatch and verdict gets a line. After context loss, trust the ledger.

## Accountability

1. **Prevent** — conventions in `AGENTS.md`; task constraints in Codex
   `developer-instructions`.
2. **Contain** — worktrees for risky writes.
3. **Verify** — `node scripts/verify-slot.mjs` + human-readable diff.
4. **Reject** — send concrete failures back on the same Codex thread or resume
   the Cursor implementer; do not silently patch in the controller session when
   SDD says otherwise.

## When they disagree

Adjudicate against `openspec/` + `AGENTS.md`. Record a **Ruling** in the ledger
with the cost if wrong. Prefer the more testable claim.
