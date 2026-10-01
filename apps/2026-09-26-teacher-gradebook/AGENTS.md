# AGENTS.md — slot: Mark Book for Teachers

This file is **nested** guidance for `apps/<this-slot>/` only. Root
`../../AGENTS.md` (factory) still applies and wins on conflicts about harness,
Play floors, and isolation.

## Slot identity

- **Slug:** `teacher-gradebook`
- **applicationId:** `com.gexetr.teachergradebook`
- **Idea:** Private offline classroom record book a teacher owns: create, edit and delete every class, student, assignment, grade, attendance mark, note, parent contact and seating position without an account or a server, see an explainable running average, export CSV and PDF, and keep a restorable backup file. Free for one class, unlocked once for unlimited classes and export.

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
node scripts/verify.mjs --slot teacher-gradebook
```

Eval report: `.kiln/eval.md` (written by `/eval`).
