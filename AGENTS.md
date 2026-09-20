# AGENTS.md — PlayKiln

> Single source of truth for AI agents (Cursor **and** Codex). Codex auto-reads
> this file; `.cursor/rules/*.mdc` are Cursor-only. Put every convention an
> external agent must follow here.

## What this repo is

**PlayKiln** — a monorepo *factory* that produces many independent Android APK/AAB
attempts for Google Play. One shared agent harness; many app slots under `apps/`.
We try ideas (calendar, notes, courses, games, AI tutors…), publish or kill them,
and keep searching for a hit. Improving the harness once upgrades every future slot.

This is **not** a single product app. The product is the kiln.

## Layout

```
AGENTS.md                 # this file — agent contract
.cursor/                  # commands, rules, hooks, mcp, project skills
.agents/skills/           # installed methodology skills (superpowers)
openspec/                 # factory specs + per-slot changes
harness/                  # dual-agent protocol, role cards
apps/<date>-<slug>/       # isolated Android projects (one attempt each)
scaffold/android-compose/ # seed expanded by scripts/new-slot.mjs (not a harness fork)
registry/apps.json        # catalog of attempts + status
scripts/                  # new-slot, verify-slot, setup-mcp
```

## Default app stack (every new slot)

| Layer | Choice |
|-------|--------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | single `:app` module first; split features only when needed |
| Async | coroutines + Flow |
| DI | prefer constructor injection; Hilt only when complexity demands it |
| Persistence | DataStore / Room as needed |
| Build | Gradle Kotlin DSL + version catalog (`libs.versions.toml`) |
| Compose compiler | Kotlin Compose Compiler plugin (Kotlin 2.x) |
| minSdk | 26+ |
| targetSdk / compileSdk | current stable (keep current) |
| Packaging | release **AAB** for Play; APK for sideload/debug |

### Defaults to suppress (agents get these wrong)

| Do NOT use | Use instead |
|---|---|
| Flutter / Dart | Kotlin + Compose |
| React Native / Expo / Next.js | Kotlin + Compose |
| XML View layouts for new screens | Compose |
| Java for new code | Kotlin |
| kapt | KSP (when annotation processing is needed) |
| Hardcoded Material colors | `MaterialTheme.colorScheme` |
| Committing keystores / `google-services.json` secrets | env / local ignored paths |
| Sharing `applicationId` across slots | unique `com.playkiln.<slug>` per slot |
| Copying `.cursor/` into an app folder | shared root harness only |

## Factory workflow

1. Human brings an idea ("calendar with reminders", "English tutor", "simple puzzle").
2. Agent runs **`/forge <idea>`** (or `/ship` for an existing OpenSpec change).
3. Pipeline: propose → create isolated slot → dual-agent implement → verify →
   Play listing assets → human decides publish / iterate / archive.
4. Human checkpoints only: **plan approval** and **integration / Play upload**.

## Dual agents (Cursor ∥ Codex) — peers

Cursor and Codex are **equal peers**, not "author + rubber-stamp reviewer".

- Split work by **task type**: Build (code), Store (Play assets/copy), Verify (cross-check).
- Either agent may implement; the other must independently verify when the task is
  non-trivial. Swap roles across tasks so neither always writes or always reviews.
- Parallel **code** writes require separate git worktrees. Parallel read-only review
  on the same tree is fine.
- Never accept either agent's "done" without `node scripts/verify-slot.mjs` (and
  reading the diff). Self-reports are not evidence.
- Put durable conventions in **this file** so Codex sees them automatically.
  Use Codex `developer-instructions` only for task-local constraints.

Details: `harness/dual-agent.md` and `.cursor/skills/dual-agent-peers/SKILL.md`.

## OpenSpec

- Factory-level specs live in `openspec/specs/`.
- Each app attempt gets a change `app-<slug>` with proposal / design / tasks / delta specs.
- Archive completed or abandoned changes; dead apps may be deleted from `apps/`
  after registry status is set to `archived` or `killed`.

## Security

- Never commit secrets, API keys, keystore passwords, Play service-account JSON.
- Keystores and Play credentials live outside git (env vars / local paths listed in
  each slot's `.env.example`).
- Hooks: `secret-guard` on writes, `git-guard` on destructive git.

## Verification

```bash
node scripts/verify-slot.mjs                 # harness + touched slots (uncommitted)
node scripts/verify-slot.mjs --slot <slug>   # one app
node scripts/verify-slot.mjs --base main
```

For an Android slot the objective gate is Gradle (`assembleDebug` / `bundleRelease`
when requested). Harness JS scripts are syntax-checked. UNVERIFIED means read the
diff by hand — do not treat silence as a pass.

## MCP

See `.cursor/mcp.json`. Onboarding: `node scripts/setup-mcp.mjs`.

## Working rules

- One idea → one new folder under `apps/` unless the human asks to extend an existing slot.
- Do not couple slots to each other.
- Do not invent a second harness inside an app.
- Evidence before "done".
- Prefer improving root harness skills/rules when the same mistake repeats across slots.
