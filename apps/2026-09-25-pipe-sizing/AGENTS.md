# AGENTS.md — slot: Gas Pipe Sizing Workbook

This file is **nested** guidance for `apps/<this-slot>/` only. Root
`../../AGENTS.md` (factory) still applies and wins on conflicts about harness,
Play floors, and isolation.

## Slot identity

- **Slug:** `pipe-sizing`
- **applicationId:** `com.gexetr.pipesizing`
- **Idea:** Offline gas pipe sizing arithmetic using user-entered loads, lengths, allowances, units, and source citations; paid once.

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
node scripts/verify.mjs --slot pipe-sizing
```

Eval report: `.kiln/eval.md` (written by `/eval`).
