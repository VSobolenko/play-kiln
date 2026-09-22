---
name: "/diagnose"
id: "diagnose"
category: "Workflow"
description: "Diagnose harness or slot failures using diagnosing-superpowers + kiln-status + DroidAgentKit doctor"
---

# /diagnose

When builds, agents, or MCP misbehave — do not randomly edit.

1. Run `node scripts/kiln-status.mjs` and `node scripts/harness-inventory.mjs`
2. Follow `.agents/skills/diagnosing-superpowers/SKILL.md`
3. If Android toolchain suspected: DroidAgentKit `android_doctor` / `npx -y @droidagentkit/launcher doctor`
4. Record root cause in `.superpowers/sdd/<slug>/diagnose.md` or `harness/lessons.md`
5. If the same failure will recur → `/improve-harness` (feed the factory)
