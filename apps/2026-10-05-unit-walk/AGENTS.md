# AGENTS.md — slot: UnitWalk

This file is **nested** guidance for `apps/<this-slot>/` only. Root
`../../AGENTS.md` (factory) still applies and wins on conflicts about harness,
Play floors, and isolation.

## Slot identity

- **Slug:** `unit-walk`
- **applicationId:** `com.gexetr.unitwalk`
- **Idea:** UnitWalk — a crash-safe, no-account rental unit walk for self-managing landlords: room-by-room photo condition records that survive a process kill, move-in versus move-out compare per unit, and a signed PDF shared from the phone.

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
node scripts/verify.mjs --slot unit-walk
```

Eval report: `.kiln/eval.md` (written by `/eval`).
