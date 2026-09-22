# Factory capability: Android Compose baseline

## Purpose

All new PlayKiln apps share a modern Android baseline so agents do not invent
conflicting stacks.

## Requirements

### Requirement: Compose-first Kotlin apps

New slots SHALL be Kotlin projects using Jetpack Compose and Material 3, built
with Gradle Kotlin DSL and a version catalog, with `compileSdk`/`targetSdk` ≥ 36.

#### Scenario: Scaffold

- **WHEN** `scripts/new-slot.mjs` runs
- **THEN** the slot contains an `:app` module with Compose enabled, placeholder
  `MainActivity` using `enableEdgeToEdge`, and backup extraction rules XML

### Requirement: Play-oriented packaging

Release distribution for Google Play SHALL prefer Android App Bundle (`bundleRelease`).
Compliance details live in `play-compliance` spec.
