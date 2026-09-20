---
name: "/review"
id: "review"
category: "Review"
description: "Peer review gate: bugbot + security-review + Codex (equal agent family)"
---

# /review — verification gate

The reviewing side does **not** rewrite code in this command — it verifies and
reports PASS / CHANGES REQUIRED. Fixes go back through the implementer / peer
Build role.

## Steps

1. **Scope** — prefer `Diff: uncommitted changes` mid-session; use
   `Diff: branch changes` for a finished slot branch. Confirm with
   `git status --short` and `git diff --stat`.

2. **Contract context** — read `AGENTS.md`, active OpenSpec change (if any),
   `openspec/specs/**`, and the slot's `README` / `store/listing.md` when present.

3. **Objective evidence**

   ```bash
   node scripts/verify-slot.mjs
   ```

   Plus Gradle for touched slots. Report real stdout. UNVERIFIED ≠ pass.

4. **Parallel checkers** — one `bugbot` and one `security-review` in a single
   message (`run_in_background: false`):

   - **bugbot** — description exactly `Bugbot`:
     ```
     Full Repository Path: <repo root>
     Diff: <uncommitted changes | branch changes>
     Change Description: <one line>
     Custom Instructions: Verify Android/Compose conventions from AGENTS.md,
     slot isolation (unique applicationId), no harness forks inside apps/,
     and OpenSpec conformance. Flag missing evidence, not just style.
     ```
   - **security-review** — description exactly `Security Review`:
     ```
     Full Repository Path: <repo root>
     Diff: <uncommitted changes | branch changes>
     Custom Instructions: No hardcoded secrets/keystores/Play credentials;
     exported components; WebView/URL handling; permission minimization.
     ```

4b. **Codex peer Verify** — via Codex MCP (`sandbox: read-only` unless writing
    in a worktree). Treat Codex as a **peer**, not a subordinate. Ask it to
    challenge architecture and Play-readiness. Record `threadId`.

5. **Verdict** — PASS only if objective gates are green and no blocking findings
   remain. Otherwise CHANGES REQUIRED with a concrete fix list for the Build role.
