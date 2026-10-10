# AGENTS.md — slot: Lumintra: Light Planner

This file is **nested** guidance for `apps/<this-slot>/` only. Root
`../../AGENTS.md` (factory) still applies and wins on conflicts about harness,
Play floors, and isolation.

## Slot identity

- **Slug:** `lumintra-light-planner`
- **applicationId:** `com.gexetr.lumintralightplanner`
- **Idea:** Offline-first light and night-sky session planner: a scrubbable night scene with your own horizon, dark window, moon, targets, satellite passes and cloud forecast for a saved spot

## Commands (from this directory)

```bash
./gradlew assembleDebug
./gradlew bundleRelease
./gradlew lint
```

## Boundaries

- Do not edit factory `.cursor/` or `.agents/` from slot work — open a harness
  change at repo root instead (`/improve-harness`).
- Do not depend on other `apps/*` modules.
- Secrets only in local `.env` / keystore paths.

## Verification

From repo root:

```bash
node scripts/verify.mjs --slot lumintra-light-planner
```

Eval report: `.kiln/eval.md` (written by `/eval`).
