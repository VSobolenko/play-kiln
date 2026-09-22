# PlayKiln harness design

**Date:** 2026-09-20  
**Status:** approved (monorepo factory)

## Problem

We need a single repository that acts as an APK production kiln: one shared agent
harness (Cursor ∥ Codex, OpenSpec, skills, MCP, `/ship`) and many isolated app
attempts under `apps/`. Improve the harness once; every future attempt inherits it.
Delete or iterate dead attempts without forking the factory.

## Decisions

| Decision | Choice |
|----------|--------|
| Layout | One git monorepo |
| Default stack | Kotlin + Jetpack Compose + Material 3 |
| Harness vs apps | Shared harness at repo root; apps never embed a copy |
| Dual agents | Cursor and Codex are **peers**, not author/reviewer-only |
| Templates | No per-app harness clones. A thin `scaffold/android-compose` seed is expanded by `scripts/new-slot.mjs` into a fresh folder under `apps/` |
| Out of scope | iOS / App Store, Flutter, React Native (for now) |

## Architecture

```
idea → /ship
     → OpenSpec change + registry entry
     → new isolated apps/<date>-<slug>/
     → dual-agent implement (Cursor ∥ Codex)
     → verify (Gradle) + Play listing assets
     → human: ship to Play / iterate / archive slot
```

## Isolation rules

- Unique `applicationId` per slot (`com.gexetr.<slug>`)
- Own OpenSpec change name `app-<slug>`
- Own signing secrets in env / local keystore path (never committed)
- Own ledger under `.superpowers/sdd/<slug>/`
- No shared source between slots unless explicitly extracted into harness later

## Dual-agent loop (peers)

Roles are assigned **per task**, not globally:

- **Build** — UI / domain / Gradle
- **Store** — listing copy, icons, screenshots, privacy text
- **Verify** — the other agent reviews or re-implements contested parts

Controller (whoever the human is talking to) owns the ledger, dispatches both,
runs objective gates (`scripts/verify-slot.mjs`), and never treats an agent
self-report as evidence.

## Success criteria for this harness milestone

1. Clone → open in Cursor → MCP + hooks + commands work
2. `/ship "simple calendar"` creates an isolated slot and runs the pipeline
3. Codex and Cursor can both write (via worktrees when parallel) and verify each other
4. No coupling to any other product codebase
