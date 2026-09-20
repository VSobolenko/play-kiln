# Factory capability: Android Compose baseline

## Purpose

All new PlayKiln apps share a modern Android baseline so agents do not invent
conflicting stacks.

## Requirements

### Requirement: Compose-first Kotlin apps

New slots SHALL be Kotlin projects using Jetpack Compose and Material 3, built
with Gradle Kotlin DSL and a version catalog.

#### Scenario: Scaffold

- **WHEN** `scripts/new-slot.mjs` runs
- **THEN** the slot contains an `:app` module with Compose enabled and a
  placeholder `MainActivity`

### Requirement: Play-oriented packaging

Release distribution for Google Play SHALL prefer Android App Bundle (`bundleRelease`).

#### Scenario: Pre-publish verify

- **WHEN** a slot is marked ready for Play
- **THEN** agents attempt `bundleRelease` (or record BLOCKED if SDK missing)
  before claiming ship-ready
