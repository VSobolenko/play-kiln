# PlayKiln capabilities

Shared, versioned building blocks for PlayKiln slots.

```
com.gexetr.capability:core        1.0.0   contracts, availability, diagnostics, clock
com.gexetr.capability:telemetry   1.0.0   PII-safe logging/events, no-op by default
com.gexetr.capability:ads         1.0.0   consent-gated ads over Google Mobile Ads + UMP
com.gexetr.capability:billing     1.0.0   entitlement state over Google Play Billing
com.gexetr.capability:remoteconfig 1.0.0  offline-safe flags and parameters over Firebase Remote Config
```

## Why this exists (and what it is not)

The SDKs already exist. Play Billing, Google Mobile Ads and the UMP consent SDK are
maintained by Google and there is no reason to reimplement them.

What they do **not** give you is the *integration*, and that is where a factory repeats
itself and repeats its mistakes:

- consent must be resolved **before** any ad request in the EEA/UK;
- a purchase must be acknowledged, and pending purchases must be resolved on cold start;
- entitlement state must survive process death and work offline;
- tests must not hit the network, so every networked capability needs a deterministic double;
- the Play **Data safety** answers must match what the SDKs actually do — every time, for
  every app.

That last point is the real reason this directory exists. Code reuse saves a few hundred
lines; **compliance reuse** is what prevents an app from being rejected. Each capability
ships a `capability.json` describing its SDKs, permissions and collected data, and the store
pack is generated from those files instead of being re-answered by hand per app.

So: **wrap, never replace.** A capability owns the seams, not the features.

## When to add a capability

Only when at least one holds:

1. two or more slots need it;
2. it carries Play-policy or security risk that must be handled identically everywhere;
3. the integration is genuinely non-obvious and easy to get wrong.

Otherwise write it in the slot. Extracting later is cheap; a wrong abstraction is expensive.

## When not to use it

- A slot that needs a raw SDK feature adds the SDK itself and skips the capability.
  The capability must never become a ceiling.
- A slot that needs nothing from this list simply does not depend on it — including ads.
  An offline app stays data-free, and its Data safety answer stays "no data collected".

## Usage in a slot

```kotlin
dependencies {
    implementation("com.gexetr.capability:core:1.0.0")
    implementation("com.gexetr.capability:billing:1.0.0")
    implementation("com.gexetr.capability:ads:1.0.0")   // only if the slot shows ads
}
```

The slot's `settings.gradle.kts` resolves these from `mavenLocal()`; the scaffold already
configures it.

## Build

```bash
cd capabilities
./gradlew checkCapabilityManifests   # manifest contract for every module
./gradlew test                       # unit tests, no device, no network
./gradlew assembleRelease            # all AARs
./gradlew publishToMavenLocal        # publish for slots to consume
```

Authoring rules: [`AGENTS.md`](./AGENTS.md) · Manifest schema:
[`schema/capability.schema.json`](./schema/capability.schema.json) · Factory spec:
[`../openspec/specs/capabilities/spec.md`](../openspec/specs/capabilities/spec.md)
