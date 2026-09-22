---
name: "/ship"
id: "ship"
category: "Workflow"
description: "PlayKiln front door: one idea → isolated Android slot → dual-agent build → signed APK/AAB → store pack → verify"
---

# /ship — PlayKiln front door

The single entry point for the factory. One invocation takes an idea all the way to
a publish-ready slot: isolated project, signed release artifacts, complete store
pack, and objective verification.

Two modes share one pipeline:

| Mode | When | Difference |
|---|---|---|
| **New app** | the human brings an idea and no slot exists | creates the slot first (step 2) |
| **Continue** | a slot / OpenSpec change already exists | starts at step 3 |

Decide the mode in step 0. Never create a second slot for an idea that already has
one — extend the existing slot.

## Steps

### 0. Classify

- New idea with no slot → **New app** mode. Platform is fixed: Android / Kotlin /
  Compose.
- Existing work: `openspec status --change "<name>" --json`, or a slot already under
  `apps/` → **Continue** mode.

If the idea is underspecified, ask **one** blocking question and proceed. Do not
stall on polish.

### 1. Propose (OpenSpec)

For a new idea, run `/opsx-propose` for change name `app-<slug>`
(slug = short kebab-case derived from the idea). Read `AGENTS.md` and
`openspec/specs/**` first.

Present the proposal and tasks. **CHECKPOINT 1 — stop until the human says go.**

### 2. Create the slot (New app mode only)

```bash
node scripts/new-slot.mjs --slug <slug> --name "<Human Title>" --idea "<one line>"
```

Creates `apps/<YYYY-MM-DD>-<slug>/` from `scaffold/android-compose`, registers the
attempt in `registry/apps.json`, and prints the `applicationId`
(`com.gexetr.<slug>` per `scripts/lib/namespaces.mjs`). The script self-checks that
no build output was copied and no template token or old package survived; if it
reports FAIL, fix the scaffold before implementing anything.

### 3. Implement (dual-agent PGE)

Follow `harness/dual-agent.md`, `.cursor/skills/pge-harness/SKILL.md`, and
`.agents/skills/subagent-driven-development/SKILL.md`:

- **Planner** via the `planner` agent (step 1), **Build** / **Store** via the
  `generator` subagent and/or the Codex peer.
- Ledger: `.superpowers/sdd/<slug>/progress.md`.
- Parallel writers need disjoint files or separate worktrees.
- Stubborn stalls → `ralph-loop` (capped). Fix loops ≤ 5.
- Evidence over self-reports.

### 4. Store pack

```bash
python scripts/store-kit.py --slot <slug> --render-only            # synthetic screens
python scripts/store-kit.py --slot <slug> --capture                # real device journeys
```

Generates listing copy, privacy, data-safety, review notes, icon, feature graphic,
and the full screenshot sets for `en-US` and `ru-RU`. Prefer `--capture` when a
device is attached: real screenshots beat synthetic ones on a live listing, and any
synthetic capture must be labelled as such in `store/review-notes.md`.

### 5. Sign and release

```bash
node scripts/sign-release.mjs --slot <slug> --generate     # upload keystore, outside git
node scripts/prepare-release.mjs --slot <slug>             # signed APK + AAB + manifest
```

The keystore lives in `~/.playkiln/keystores/` and `keystore.properties` is
gitignored. An applicationId is permanent after the first Play upload.

### 6. Verify

```bash
node scripts/verify-slot.mjs --slot <slug> --strict-store
node scripts/kiln-status.mjs
```

`verify-slot.mjs` also runs `scripts/selftest.mjs`, the factory's own regression
sensor. Prefer DroidAgentKit MCP for lint / doctor / runtime checks when connected.
`UNVERIFIED` is not PASS — read the diff by hand.

### 7. Evaluate (independent)

Run **`/eval`** with the `evaluator` subagent, and `play-auditor` before marking the
slot ready, then **`/review`**. The generator never grades its own work.
On FIX → resume the generator → re-eval. On KILL → record `killed` in the registry.

### 8. Register and hand off

Update `registry/apps.json`. **CHECKPOINT 2 — the human uploads and publishes.**

```bash
node scripts/play-upload.mjs --slot <slug>   # only after the first manual upload
```

## Guardrails

- Evidence before "done": never accept Cursor or Codex self-reports without
  `verify-slot.mjs` output plus a read diff.
- Never commit secrets, never force-push `main`, never publish on the human's behalf.
- Keep slots isolated; improve the **root** harness when the same failure repeats
  (`.cursor/rules/verification-discipline.mdc`).
- Fixing a factory defect means adding a check to `scripts/selftest.mjs` and
  confirming it fails before the fix.

## Output

```
## /ship complete — <title>

**Mode:** new app | continue
**Slot:** apps/<dir>
**applicationId:** com.gexetr.<slug>
**Change:** app-<slug>
**Artifacts:** dist/ signed APK + AAB + release manifest
**Verification:** <stdout evidence>
**Store pack:** apps/<dir>/store/
**Next:** human checkpoint 2 — upload and publish
```
