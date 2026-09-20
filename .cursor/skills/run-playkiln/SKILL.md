---
name: run-playkiln
description: How to run the PlayKiln factory and a slot's Android build. Use when setting up the environment or assembling an app.
---

# Run PlayKiln

## Factory

```bash
node scripts/setup-mcp.mjs
node scripts/new-slot.mjs --slug demo-calendar --name "Demo Calendar" --idea "offline calendar"
node scripts/verify-slot.mjs
```

## Slot build (requires Android SDK / JDK 17+)

```bash
cd apps/<date>-<slug>
./gradlew assembleDebug
./gradlew bundleRelease
```

On Windows PowerShell:

```powershell
cd apps\<date>-<slug>
.\gradlew.bat assembleDebug
```

## Prereqs

- JDK 17+
- Android SDK (platform + build-tools matching the slot's `compileSdk`)
- Cursor MCP: context7, playwright, codex (ChatGPT/Codex extension), optional firebase
