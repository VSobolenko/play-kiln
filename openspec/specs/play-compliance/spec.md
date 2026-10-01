# Capability: Google Play compliance baseline

## Purpose

Every PlayKiln slot that aims at Google Play MUST meet current Play Console and
platform requirements documented in
`docs/research/2026-09-20-android-play-ai-agents.md` (refresh when Google emails
or Console warnings change).

## Requirements

### Requirement: Target API floor

Phone/tablet slots SHALL set `compileSdk` and `targetSdk` to **36** (Android 16)
or higher unless the human explicitly scopes a non-Play / Wear / TV form factor.

#### Scenario: New slot scaffold

- **WHEN** `scripts/new-slot.mjs` creates a slot
- **THEN** the generated `app/build.gradle.kts` uses compileSdk/targetSdk ≥ 36

### Requirement: Backup and device transfer rules

Slots SHALL declare `android:dataExtractionRules` and provide XML rules separating
cloud backup vs device-to-device transfer. Sensitive tokens/credentials SHALL be
excluded. Legacy `fullBackupContent` SHOULD remain for API ≤ 30 users when
`minSdk` allows older devices.

#### Scenario: Manifest audit

- **WHEN** `node scripts/verify.mjs --slot <slug>` runs on a slot
- **THEN** `slot.backup` FAILs if `dataExtractionRules` or the referenced XML is missing

### Requirement: 16 KB page size awareness

Slots SHALL prefer pure Kotlin/Java. If any native `.so` is introduced (direct or
via SDK), agents MUST use AGP ≥ 8.5.1 / NDK r28+ guidance and record a Store/Verify
task to confirm Play Console 16 KB checks before `ready`.

#### Scenario: Adding a native SDK

- **WHEN** a dependency ships JNI libraries
- **THEN** OpenSpec tasks include a 16 KB compatibility verification step

### Requirement: Edge-to-edge and predictive back

Slots targeting API 36 SHALL support edge-to-edge (no opt-out on Android 16) and
prefer predictive-back compatible navigation (`enableOnBackInvokedCallback` true
unless a documented temporary opt-out exists in the change design).

#### Scenario: Platform behavior audit

- **WHEN** `node scripts/verify.mjs --slot <slug>` runs on a targetSdk-36 slot
- **THEN** `slot.manifest` reports `enableOnBackInvokedCallback true` as part of the
  manifest rows, and a documented temporary opt-out is the only accepted deviation

### Requirement: Play Console disclosures

Before status `ready` / publish, the slot's `store/` pack SHALL include privacy
policy draft, Data safety answers, review/demo notes, and ads declaration. Agents
SHOULD run Google's `play-policy-insights` skill audit when available.

#### Scenario: Pack completeness

- **WHEN** a slot's `store/` pack is validated
- **THEN** `pack.listing`, `pack.releaseNotes`, `pack.placeholders` and
  `slot.inAppPolicy` report rows, and the privacy policy is reachable from inside the app

### Requirement: No low-value AI wrappers

If the product idea is generative AI, the design MUST add differentiated workflow
(not a bare chat API shell) and in-app content reporting per Play AI-Generated
Content policy.

#### Scenario: AI slot is designed

- **WHEN** a proposed slot's differentiator is AI output
- **THEN** its `differentiators.json` names the workflow that is not a chat shell, and
  the content-reporting obligation is written before the UI exists
