# PlayKiln harness ambition — surpass, don't thin-copy

**Verdict (2026-09-20):** PlayKiln's *methodology* core (superpowers + OpenSpec +
`/ship` + dual Codex peers) was already comparable to the food-delivery
reference. It *felt* thinner because that repo carries ~9 Expo domain skills plus
product-specific GraphQL/i18n skills — volume that looks like "richness" but is
**product specialization**, not a better factory.

**Goal:** PlayKiln must be a **better general factory** than the reference: more
modern harness patterns, real Android agent tooling, stronger computational
sensors, and a Planner→Generator→Evaluator loop that the reference only partially
implements.

## Honest gap map (before this upgrade)

| Area | Reference (food-delivery) | PlayKiln before | Target |
|------|---------------------------|-----------------|--------|
| Superpowers SDD/TDD/review | Yes | Yes (+ diagnosing) | Keep |
| Domain skill *count* | High (Expo+product) | Lower (correctly) | Depth > fake count |
| Dual-model peers | Codex reviewer-ish | Peers declared | PGE + peers |
| Android agent MCP | None | None | **android-skills + DroidAgentKit** |
| Computational sensors | verify-changes | verify-slot (Play gates) | + inventory, doctor, eval rubric |
| Context layering L1/L2/L3 | Implicit | Weak | Explicit skill |
| Harness self-improve | Informal | Informal | `/improve-harness` |
| Nested AGENTS per app | No | No | Slot `AGENTS.md` |

## Modern patterns we adopt (sources)

- Harness engineering (Böckeler / Osmani / arXiv 2609.00006): guides + sensors + loops
- Planner–Generator–Evaluator (Anthropic long-running / agent patterns catalog)
- Ralph loop for stubborn grind with filesystem ledger
- Google `android/skills` + skydoves `android-skills-mcp`
- iVamsi `droid-agent-kit` — allowlisted Gradle/adb MCP (not raw shell)
- hwdavr `android-agent-harness` — L1/L2/L3 context + role separation

## Non-goals

- Do not clone Expo skills into an Android factory.
- Do not invent fake MCP servers.
- Do not measure quality by skill folder count alone.
