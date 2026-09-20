---
name: "/ship"
id: "ship"
category: "Workflow"
description: "Autonomous orchestrator for an existing OpenSpec change: implement → verify → review → finish → archive"
---

# /ship — autonomous agent harness

Single entry point for end-to-end work on an **existing** OpenSpec change.
For a brand-new Play app idea, use `/forge` first (it creates the slot, then
continues with this pipeline).

Stops for the human only at: **plan approval** (if not already approved) and
**integration / Play decision**.

## Roles

| Role | Implementation |
|------|----------------|
| Orchestrator | this command — current agent |
| Contract | OpenSpec + `/opsx-*` |
| Writer | fresh implementer subagent **or** Codex peer (see dual-agent) |
| Task checker | task reviewer / peer Verify role |
| Final checkers | `bugbot` + `security-review` + Codex peer |
| Runtime UI | `/ui-verify` (Compose UI / emulator when available) |
| Policy | hooks `secret-guard`, `git-guard` |
| Evidence | `verification-before-completion` + `scripts/verify-slot.mjs` |
| Integration | `finishing-a-development-branch` |

## Steps

### 0. Classify

- Reuse existing change: `openspec status --change "<name>" --json`
- If none and the human wants a new app → hand off to `/forge`
- Present plan if not yet approved. **CHECKPOINT 1**

### 1. Implement (dual-agent + SDD)

Follow `.agents/skills/subagent-driven-development/SKILL.md` and
`harness/dual-agent.md`. Isolate with git worktrees when needed. Never implement
on `main` without consent. Ledger under `.superpowers/sdd/<plan>/progress.md`.

### 2. Verify

```bash
node scripts/verify-slot.mjs
# or --slot <slug> when known
```

Slot touched → Gradle `assembleDebug` (and `bundleRelease` when shipping).
Spec/planning → `openspec validate --strict "<change>"`.

### 3. Runtime UI verify

Run `/ui-verify` for Compose screens in the blast radius.

### 4. Review

Dispatch `/review`. Fix loop ≤5 rounds on blockers.

### 5. Finish

`finishing-a-development-branch` → **CHECKPOINT 2** (merge/PR/keep + Play upload
decision). Do not publish without explicit approval.

### 6. Archive

`/opsx-archive` when the change is done or deliberately abandoned.

## Guardrails

- Evidence before "done".
- Never accept Codex or Cursor self-reports without `verify-slot.mjs` + diff read.
- Never commit secrets; never force-push `main`/`master`.
- Keep slots isolated; improve the **root** harness when the same failure repeats.
