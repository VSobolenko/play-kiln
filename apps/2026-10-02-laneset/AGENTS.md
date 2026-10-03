# AGENTS.md — slot: Laneset: Swim Set Builder

This file is **nested** guidance for `apps/<this-slot>/` only. Root
`../../AGENTS.md` (factory) still applies and wins on conflicts about harness,
Play floors, and isolation.

## Slot identity

- **Slug:** `laneset`
- **applicationId:** `com.gexetr.laneset`
- **Idea:** A swim set builder that computes each session from the swimmer's own constraints (minutes, pool length, focus, strokes to avoid, gear on deck) against authored programming rules, walks them through the two pace trials that put a target time on every interval, and prints or shares the set list; one-time unlock, offline, no account.

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
node scripts/verify.mjs --slot laneset
```

Eval report: `.kiln/eval.md` (written by `/eval`).
