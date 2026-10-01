---
name: "/eval"
id: "eval"
category: "Review"
description: "Independent Evaluator role — grades a slot against a fixed rubric (never self-grade)"
---

# /eval — Planner–Generator–Evaluator: Evaluator

The **Evaluator** does not write product code. It grades artifacts against a fixed
rubric in a fresh mindset (prefer a different peer than who Built: Codex if Cursor
built, or bugbot+security-review).

It runs twice in a slot's life, against different evidence:

- **Phase 1 (`/ship`)** — source-level grading: the app builds as a debug APK, the code and
  tests are honest, the store *drafts* match the app. Signed artifacts, rendered assets and
  screenshots do not exist yet, so their absence is not a finding.
- **Phase 2 (`/release`)** — the same rubric against the release evidence: signed APK and AAB,
  release manifest, strict store-pack PASS, rendered assets and screenshots
  (`verify.mjs --slot <slug> --build`).

## Rubric (score each 0–5; ship bar = all ≥ 4)

1. **Builds** — `verify.mjs` PASS; `assembleDebug` evidence or BLOCKED with cause
2. **Architecture** — Kotlin/Compose conventions; no harness fork inside slot
3. **Play compliance** — targetSdk≥36, backup rules, store pack drafts
4. **UX** — edge-to-edge/insets; no blank primary screen
5. **Tests** — at least one meaningful unit or UI test for core logic (or explicit
   waiver in OpenSpec with human approval)
6. **Honesty** — listing/privacy/Data safety match the app
7. **Isolation** — unique applicationId; no cross-slot coupling

## Steps

1. Scope the slot (`apps/<dir>/`) and OpenSpec change.
2. Run `node scripts/verify.mjs --slot <slug>` — capture output.
3. Prefer DroidAgentKit MCP for lint/doctor when available; else Gradle.
4. Dispatch independent reviewers (`/review`) and/or Codex peer Verify read-only.
5. Write `apps/<slot>/.kiln/eval.md` with scores, findings, and **PASS / FIX / KILL**.
6. On FIX: hand findings to Generator only (no Evaluator patches). On KILL: update
   registry status.

Never show the Generator the Evaluator's chain-of-thought — only the findings list.
