# AGENTS.md — slot: Coverkit: Social Studio

This file is **nested** guidance for `apps/<this-slot>/` only. Root
`../../AGENTS.md` (factory) still applies and wins on conflicts about harness,
Play floors, and isolation.

## Slot identity

- **Slug:** `coverkit`
- **applicationId:** `com.gexetr.coverkit`
- **Idea:** Offline multi-network social cover studio: design once, export sharp PNG packs for YouTube Instagram TikTok WhatsApp, one-time unlock, no AI keys.

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
node scripts/verify.mjs --slot coverkit
```

Eval report: `.kiln/eval.md` (written by `/eval`).
