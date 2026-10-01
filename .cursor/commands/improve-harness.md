---
name: "/improve-harness"
id: "improve-harness"
category: "Workflow"
description: "Turn repeated agent failures into root skills/rules/scripts — factory self-improvement"
---

# /improve-harness

OpenAI/Cursor harness practice: when agents struggle, upgrade the **environment**,
not only the one-off fix.

## When

- Same mistake across ≥2 slots (or ≥2 sessions)
- verify/eval catches a class of bugs agents keep reintroducing
- New Play Console email/policy appears

## Steps

1. Write a short failure note in `harness/lessons.md` (symptom → root cause → fix class)
2. Prefer, in order: update `AGENTS.md` (Codex sees it) → new/extended skill →
   computational sensor in `verify.mjs` → rule `.mdc` → OpenSpec factory spec
3. Add a regression check (script assertion or checklist item)
4. Run `node scripts/harness-inventory.mjs` and note the delta
5. Do **not** fork harness into an app slot
