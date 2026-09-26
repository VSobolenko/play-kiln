---
name: run-playkiln
description: How to run the PlayKiln factory and a slot's Android build. Use when setting up the environment or assembling an app.
---

# Run PlayKiln

## Factory

```bash
node scripts/setup-mcp.mjs
node scripts/check-economics.mjs --slot <slug>          # money gate: plan vs enabled capabilities
node scripts/verify-slot.mjs
```

Creating a slot requires an approved economics plan **and** a promise contract, and both come first:

```bash
node scripts/check-economics.mjs --economics docs/ideation/plans/<slug>.economics.json
node scripts/check-differentiators.mjs --differentiators docs/ideation/plans/<slug>.differentiators.json
node scripts/new-slot.mjs --slug <slug> --name "<Title>" --idea "<one line>" \
  --economics docs/ideation/plans/<slug>.economics.json \
  --differentiators docs/ideation/plans/<slug>.differentiators.json
```

Do not invent a slug and a title to "see if it works". A slot costs the owner real
time and a Play listing; ideas are proposed in `docs/ideation/` from measured demand and
approved by the owner first. Calculators are a closed topic, and the example an owner
gives is a shape rather than a topic — see `.cursor/rules/ideation-gate.mdc`.

## Slot build (requires Android SDK / JDK 17+)

Development builds the debug variant only. Release tasks, signing, store assets and screenshots
are phase 2 and refuse until the owner approves
(`node scripts/approve-release.mjs --check --slot <slug>`):

```bash
cd apps/<date>-<slug>
./gradlew assembleDebug
```

On Windows PowerShell:

```powershell
cd apps\<date>-<slug>
.\gradlew.bat assembleDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk` — that is what the owner installs
to review the app. Do not run `assembleRelease`/`bundleRelease` by hand during development; the
release pipeline (`node scripts/prepare-release.mjs --slot <slug>`) owns them, runs them in one
Gradle invocation, and records the signed artifacts plus the release manifest.

## Prereqs

- JDK 17+
- Android SDK (platform + build-tools matching the slot's `compileSdk`)
- Cursor MCP: context7, playwright, codex (ChatGPT/Codex extension), optional firebase
