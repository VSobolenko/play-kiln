# Harness notes

PlayKiln's agent runtime lives at the **repo root**. App slots under `apps/` are
products of the kiln, not copies of it.

Read **`ambition.md`** — the factory must surpass prior in-house harnesses.

## Canonical loop

1. **Plan** — OpenSpec; human checkpoint (`context-layers` L1/L2)
2. **Generate** — Cursor ∥ Codex peers (Build/Store); SDD; optional Ralph
3. **Observe** — `verify-slot`, DroidAgentKit, Gradle
4. **Evaluate** — `/eval` + `/review` (independent; no self-PASS)
5. **Repair** — Generator only; ≤5 rounds
6. **Integrate** — human Play/merge decision
7. **Improve** — `/improve-harness` → `lessons.md`

## Commands

`/ship` `/eval` `/review` `/ui-verify` `/diagnose` `/improve-harness`
`/kiln-status` `/opsx-*`

## Health

```bash
node scripts/kiln-status.mjs
node scripts/harness-inventory.mjs
```
