# AGENTS.md — slot: PlayKiln Smoke

This file is **nested** guidance for `apps/<this-slot>/` only. Root
`../../AGENTS.md` (factory) still applies and wins on conflicts about harness,
Play floors, and isolation.

## Slot identity

- **Slug:** `smoke`
- **applicationId:** `com.gexetr.smoke`
- **Idea:** Pipeline smoke test: proves new-slot -> signed APK + AAB -> store pack -> verify PASS.

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
node scripts/verify-slot.mjs --slot smoke
```

Eval report: `.kiln/eval.md` (written by `/eval`).
