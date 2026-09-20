---
name: "/forge"
id: "forge"
category: "Workflow"
description: "PlayKiln entry point: idea → isolated Android slot → dual-agent build → verify → Play assets"
---

# /forge — APK kiln entry point

Use when the human brings a **new app idea**. This is the factory front door.
For continuing an existing OpenSpec change, prefer `/ship`.

## Steps

### 0. Clarify just enough

If the idea is underspecified, ask **one** blocking question (platform is fixed:
Android / Kotlin / Compose). Otherwise proceed. Do not stall on polish.

### 1. Propose (OpenSpec)

Run `/opsx-propose` for change name `app-<slug>` (slug = short kebab-case from
the idea). Read `AGENTS.md` and `openspec/specs/**` first.

Present proposal + tasks. **CHECKPOINT 1** — stop until the human says go.

### 2. Create the slot

```bash
node scripts/new-slot.mjs --slug <slug> --name "<Human Title>" --idea "<one line>"
```

This creates `apps/<YYYY-MM-DD>-<slug>/` from `scaffold/android-compose`, registers
the attempt in `registry/apps.json`, and prints the `applicationId`.

### 3. Dual-agent implement

Follow `harness/dual-agent.md` and
`.agents/skills/subagent-driven-development/SKILL.md` over the change `tasks.md`:

- Assign **Build** / **Store** / **Verify** roles to Cursor and Codex as **peers**
  (see `.cursor/skills/dual-agent-peers/SKILL.md`). Swap roles across tasks.
- Parallel code writes → separate git worktrees.
- Ledger: `.superpowers/sdd/<slug>/progress.md`
- Fix loops ≤5 rounds. Evidence over self-reports.

### 4. Verify

```bash
node scripts/verify-slot.mjs --slot <slug>
```

Plus Gradle evidence from the slot (`assembleDebug` at minimum). Capture stdout.

### 5. Play listing pack

Generate under `apps/<slot>/store/`:

- `listing.md` (title, short/full description, category)
- `privacy.md`
- icon + feature graphic prompts / assets (Codex `image_gen` OK)
- screenshot plan (Playwright or emulator captures when available)

### 6. Review

Run `/review` scoped to the slot. Then **CHECKPOINT 2** — publish / iterate /
archive. Do not upload to Play without human approval.

### 7. Registry

Update `registry/apps.json` status: `building` → `ready` | `published` |
`iterating` | `killed` | `archived`.

## Output

```
## /forge complete — <title>

**Slot:** apps/<dir>
**applicationId:** com.playkiln.<slug>
**Change:** app-<slug>
**Verification:** <evidence>
**Store pack:** apps/<dir>/store/
**Next:** awaiting publish / iterate / archive
```
