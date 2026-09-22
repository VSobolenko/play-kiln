# AGENTS.md — PlayKiln capabilities

Rules for anything living under `capabilities/`. Read the root `AGENTS.md` first.

## What this directory is

A standalone Gradle build that publishes **versioned Android library artifacts** shared by
PlayKiln slots. Group `com.gexetr.capability`, one version for all modules
(`playkiln.capabilityVersion` in `gradle.properties`).

A slot consumes a capability by pinning a version. A slot never depends on another slot and
never on this build's source tree — that keeps slots isolated (root rule) while removing
repeated integration work.

## Golden rule: wrap, never replace

Official SDKs (Play Billing, Google Mobile Ads, UMP consent) already solve their own problems.
A capability exists only to own the **integration** that every app otherwise re-invents:

- correct initialisation order and lifecycle;
- consent before any data collection or ad request;
- entitlement/purchase state that survives process death and works offline;
- a deterministic test double so slot tests never touch the network;
- a truthful `capability.json` so the store pack is generated, not hand-written.

Do **not** put these here:

- app UI, Compose code, navigation, theming (slots own their UI);
- app business logic that only one app uses;
- a reimplementation of an SDK feature;
- an abstraction layer with no second consumer in sight;
- anything that hides a policy obligation without declaring it in `capability.json`.

## Promotion rule (when code becomes a capability)

Promote only when **at least one** is true:

1. two or more shipped/planned slots need it, or
2. it carries Play-policy or security risk that must be solved identically everywhere, or
3. the integration is genuinely non-obvious (retry/reconnect, acknowledgement, consent,
   state restoration) and getting it wrong is easy.

Otherwise write it in the slot. Extracting later is cheap; a wrong abstraction is expensive.

## Mandatory contract for every module

1. `capability.json` at the module root, validated against `schema/capability.schema.json`
   (`./gradlew checkCapabilityManifests`). It must be truthful — it feeds the Data safety form.
2. A `Fake…` test double whenever `networkRequired` is true. The fake is part of the public API.
3. Unit tests (`src/test`) that run without a device and without network.
4. No SDK types in the public API. Third-party dependencies stay `implementation` so a slot
   cannot accidentally couple to AdMob/Billing classes; the documented escape hatch is for a
   slot to add the raw SDK itself and drop the capability.
5. No permissions beyond what `capability.json` declares, and every dangerous permission needs
   a written `why`.
6. No PII in logs. Telemetry must be no-op unless a slot explicitly enables it.
7. API stability: once a version is published, only additive changes in the same major.

## Versioning and release

1. Change code in a module.
2. Bump `playkiln.capabilityVersion` (or the artifact's `capability.json` version — they must
   match; the check task enforces it).
3. `./gradlew checkCapabilityManifests test`
4. `./gradlew publishToMavenLocal`
5. Update the pinned version in the slot that wants it. Slots on older versions keep working.

Breaking change → new major version, and the old version stays published.

## Build

```bash
cd capabilities
./gradlew checkCapabilityManifests   # manifest contract
./gradlew test                       # unit tests, no device needed
./gradlew assembleRelease            # all AARs
./gradlew publishToMavenLocal        # make artifacts available to slots
./gradlew :ads:publishToMavenLocal   # one module
```

## What a slot sees

```kotlin
implementation("com.gexetr.capability:ads:1.0.0")
implementation("com.gexetr.capability:billing:1.0.0")
```

Consumption requires `mavenLocal()` in the slot's repositories, which the scaffold configures.
