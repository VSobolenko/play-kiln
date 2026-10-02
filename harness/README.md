# Harness notes

PlayKiln's agent runtime lives at the **repo root**. App slots under `apps/` are
products of the kiln, not copies of it.

Read **`ambition.md`** — the factory must surpass prior in-house harnesses.

## Canonical loop

Two phases, with the owner's approval between them
(`.cursor/rules/release-gate.mdc`).

**Phase 1 — development (`/forge` or `/ship`)**

1. **Plan** — OpenSpec; human checkpoint (`context-layers` L1/L2)
2. **Generate** — Cursor ∥ Codex peers (Build); SDD; optional Ralph
3. **Observe** — `verify.mjs --build`, `run-slot-tests`, DroidAgentKit, Gradle
4. **Approve** — the agent has installed the debug APK on the connected phone and exercised the app
   itself, and its observed evidence (device serial, artifact hash, what appeared) is the handoff; the
   owner decides the product and says yes; recorded by
   `approve-release.mjs --note` and bound to a digest of the app's sources

**Phase 2 — release (`/release`)**

5. **Build and sign** — `prepare-release.mjs`
6. **Store pack and screenshots** — `scripts/store/render.py`, `media-capture.mjs`
7. **Verify** — `verify.mjs --build`
8. **Evaluate** — `/eval` + `/review` (independent; no self-PASS)
9. **Repair** — Generator only; ≤5 rounds
10. **Integrate** — human Play/merge decision
11. **Improve** — `/improve-harness` → `lessons.md`

## Commands

`/forge` `/ship` `/release` `/eval` `/review` `/ui-verify` `/diagnose` `/improve-harness`
`/kiln-status` `/opsx-*`

## Health

```bash
node scripts/kiln-status.mjs
node scripts/harness-inventory.mjs
```
