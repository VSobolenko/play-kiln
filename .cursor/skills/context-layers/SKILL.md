---
name: context-layers
description: L1/L2/L3 context loading for PlayKiln — keep context under control. Use at session start and when switching ship stages.
---

# Context layers (L1 / L2 / L3)

Inspired by battle-tested Android agent harnesses. **Never dump every skill upfront.**

| Layer | When | Load |
|-------|------|------|
| **L1** | Every session | `AGENTS.md`, `DESIGN.md`, `harness/dual-agent.md`, `project-conventions` rule |
| **L2** | Active stage | Exactly the skills named by the stage (`/ship` propose → openspec-propose; Build → android-compose-slot + play-compliance; Eval → `/eval` + play-policy-insights) |
| **L3** | On demand | Research brief, Google android-official skill bodies, slot OpenSpec, DroidAgentKit reports |

If context > ~40% full: checkpoint ledger, reset, continue with L1+L2 only.
