# AGENTS.md — slot: Plinth: Product Photos

This file is **nested** guidance for `apps/<this-slot>/` only. Root
`../../AGENTS.md` (factory) still applies and wins on conflicts about harness,
Play floors, and isolation.

## Slot identity

- **Slug:** `plinth-product-photos`
- **applicationId:** `com.gexetr.plinthproductphotos`
- **Idea:** Turn a seller's own product photos into per-marketplace listing cards: server cut-out, original-pixel composition, marketplace rule linter, export with provenance.

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
node scripts/verify.mjs --slot plinth-product-photos
```

Eval report: `.kiln/eval.md` (written by `/eval`).
