# AGENTS.md — slot: PlayKiln Pipe Proof

This file is **nested** guidance for `apps/<this-slot>/` only. Root
`../../AGENTS.md` (factory) still applies and wins on conflicts about harness,
Play floors, and isolation.

## Slot identity

- **Slug:** `pipe-proof`
- **applicationId:** `com.gexetr.pipeproof`
- **Idea:** Throwaway pipeline proof: scaffold to signed APK and AAB to store pack to verified gates, with no hand edits.

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
node scripts/verify-slot.mjs --slot pipe-proof
```

Eval report: `.kiln/eval.md` (written by `/eval`).
