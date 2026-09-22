---
name: pge-harness
description: Planner–Generator–Evaluator loop for PlayKiln slots. Use on non-trivial /ship and /ship work so generation and grading stay separated.
---

# Planner–Generator–Evaluator

Modern long-running agent pattern (stronger than self-review):

| Role | Who | Output |
|------|-----|--------|
| **Planner** | Controller after human idea | OpenSpec proposal/tasks + sprint contract |
| **Generator** | Cursor and/or Codex Build/Store | Code + store pack + ledger |
| **Evaluator** | `/eval` + `/review` + other peer | `.kiln/eval.md` scores; no product edits |

## Rules

1. Planner does not implement.
2. Generator does not grade itself as final.
3. Evaluator never patches — only findings.
4. Failed eval → Generator fix loop (≤5) → re-eval.
5. Context resets between roles when possible (fresh subagent / Codex thread).

See `harness/ambition.md` and `/eval`.
