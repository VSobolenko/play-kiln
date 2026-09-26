---
name: android-compose-slot
description: Build or modify a PlayKiln Android app slot with Kotlin, Jetpack Compose, Material 3, and modern Gradle. Use when working under apps/*/ or creating UI/domain code for a slot.
---

# Android Compose slot

## Stack

- Kotlin, Jetpack Compose, Material 3
- Gradle Kotlin DSL + `gradle/libs.versions.toml`
- Kotlin Compose Compiler plugin (Kotlin 2.x) — not the old separate compiler artifact
- coroutines / Flow; ViewModel `UiState`
- Navigation Compose
- KSP if annotation processing is required

## Layout (single-module starter)

```
apps/<slot>/
  settings.gradle.kts
  build.gradle.kts
  gradle/libs.versions.toml
  app/src/main/java/.../MainActivity.kt
  app/src/main/java/.../ui/theme/
  app/src/main/AndroidManifest.xml
  store/          # Play listing pack
  .env.example
  README.md
```

Grow into feature modules only when the OpenSpec design says so.

## Do

- Unique `applicationId` = `com.gexetr.<slug>`
- `compileSdk` / `targetSdk` ≥ **36**
- `dataExtractionRules` + backup XML; `enableEdgeToEdge`
- Theme via `MaterialTheme`; dynamic color optional
- Previews for non-trivial composables
- `assembleDebug` before claiming UI works; `bundleRelease` happens in `/release` after the
  owner's approval, via `scripts/prepare-release.mjs`
- Run `.cursor/skills/play-compliance/` before status `ready`

## Don't

- Flutter / RN / Expo / XML-first new screens
- Commit keystores or real `google-services.json`
- Copy the factory harness into the slot
- Ship without Data safety / privacy drafts in `store/`
