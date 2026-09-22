# Android and Google Play requirements — 2026-09-22

This brief supersedes `2026-09-20-android-play-ai-agents.md`; the older file remains
historical. Values below come from `.kiln/codex/prompts/FACTS.md`, verified there on
2026-09-22.

## Citation status

`FACTS.md` contains the requirements but contains no official documentation URLs
for them. Its only URL is the Android Publisher OAuth scope, which is not a
supporting documentation page. The required official citations are therefore marked
`NEEDS-VERIFICATION` instead of being invented.

## 1. Toolchain

The factory baseline is Gradle **9.7.1**, AGP **9.4.1**, Kotlin and the Compose
Compiler plugin **2.3.21**, Compose BOM **2026.09.00**, Compose UI **1.12.0**,
Material 3 **1.4.0**, `compileSdk = 37`, `targetSdk = 36`, `minSdk = 26`, JDK
**17 minimum**, and SDK Build Tools **36.0.0**. Gradle 9.7.1 runs on JDK 17–26;
AGP 9.4.1 supports API 37; Compose 1.12 requires compileSdk 37 and AGP 9.

Sources: **NEEDS-VERIFICATION** — no official toolchain URLs are present in `FACTS.md`.

AGP 9 has built-in Kotlin. Applying `org.jetbrains.kotlin.android` is an error;
`android.kotlinOptions {}` is removed; `jvmTarget` derives from Java compile options.
`getDefaultProguardFile()` accepts only `proguard-android-optimize.txt`, and
`applicationVariants` is replaced by `androidComponents.onVariants`.

Sources: **NEEDS-VERIFICATION** — no official AGP 9 URL is present in `FACTS.md`.

AGP 9 defaults include R8 full mode, optimized resource shrinking, strict full-mode
keep rules, AndroidX, the default AndroidX test runner, tested-build-type-only unit
tests, and failure on missing ProGuard files. Do not set the obsolete flags listed
in `AGENTS.md`.

Sources: **NEEDS-VERIFICATION** — no official AGP default-settings URL is present in `FACTS.md`.

## 2. Platform behavior changes

### Target SDK

New apps and updates have a Play target floor of API 36 since **2026-08-31**.
The factory compiles against API 37 while targeting API 36.

Sources: **NEEDS-VERIFICATION** — no official target-SDK URL is present in `FACTS.md`.

### Edge-to-edge

Edge-to-edge is enforced for apps targeting API 35+ on Android 15+. At API 36
there is no opt-out on Android 16. Use `enableEdgeToEdge()` and consume system
insets with Material 3 `Scaffold`.

Sources: **NEEDS-VERIFICATION** — no official edge-to-edge URL is present in `FACTS.md`.

### Predictive back

Predictive back is on by default at API 36. The manifest contract is
`enableOnBackInvokedCallback="true"`.

Sources: **NEEDS-VERIFICATION** — no official predictive-back URL is present in `FACTS.md`.

### Backup and device transfer

Declare `android:allowBackup="true"` or record an explicit decision. For API 31+
provide `android:dataExtractionRules`; for older devices also provide
`android:fullBackupContent`. Rules must separate cloud backup from device transfer
and exclude secrets.

Sources: **NEEDS-VERIFICATION** — no official backup URL is present in `FACTS.md`.

### Permissions

Minimize permissions, prefer Photo Picker to broad media access, declare foreground-
service types, and treat exact alarms as restricted.

Sources: **NEEDS-VERIFICATION** — no official permissions URL is present in `FACTS.md`.

## 3. Performance and quality

### R8

Release builds enable minification and resource shrinking in R8 full mode, use
`proguard-android-optimize.txt`, and keep rules must be precise. Under strict full
mode, keeping a class no longer implicitly keeps its constructor. A **≥25%** DEX
shrinking/optimization/obfuscation expectation applies to 2027 releases.

Sources: **NEEDS-VERIFICATION** — no official R8/app-quality URL is present in `FACTS.md`.

### Baseline and startup profiles

Use `androidx.baselineprofile` **1.5.0**,
`androidx.profileinstaller:profileinstaller:1.4.1`, and
`androidx.benchmark:benchmark-macro-junit4:1.4.1`. Generation requires a device or
emulator through a `com.android.test` module. The verified guidance attributes
roughly **30% faster startup** to profiles.

Sources: **NEEDS-VERIFICATION** — no official baseline-profile URL is present in `FACTS.md`.

### Android vitals thresholds

Keep user-perceived crash rate below **1.09% overall** and **8% per device model**;
user-perceived ANR rate below **0.47% overall** and **8% per device model**; and
excessive partial wake locks below **5%**. These bad-behavior thresholds can affect
Play visibility. Vitals also tracks p90 anonymous RSS plus swap, bitmap memory, and
jank. Avoid leaks and unbounded caches as Android 17 tightens app memory limits.

Sources: **NEEDS-VERIFICATION** — no official Android vitals URL is present in `FACTS.md`.

## 4. Packaging and signing

### Release artifacts

The factory release contract is a signed installable APK, a signed AAB for Play,
and `dist/release-manifest.json`. `prepare-release.mjs` verifies the APK signature,
records the upload-certificate SHA-256 fingerprint, inventories artifacts, and
records 16 KB readiness.

Sources: **NEEDS-VERIFICATION** — `FACTS.md` gives no official AAB/signing URL; the factory behavior is repository-local.

### 16 KB page size

Pure Kotlin/Java apps are ready. Apps with native code require AGP **8.5.1+** and
NDK **r28+**. The hard update requirement begins **2027-02-01**. Never set
`jniLibs.useLegacyPackaging = true`.

Sources: **NEEDS-VERIFICATION** — no official 16 KB page-size URL is present in `FACTS.md`.

### Signing boundary

Keystores and passwords remain outside Git. The factory configures a local upload
key and produces signed artifacts; the human controls Play App Signing and the first
Console upload. Exact Play App Signing key semantics and rotation/reset steps are not
specified in `FACTS.md`.

Sources: **NEEDS-VERIFICATION** — no official Play App Signing or key-rotation URL is present in `FACTS.md`.

## 5. Play Console requirements

### Listing text and graphics

- Title: at most **30** characters.
- Short description: at most **80** characters.
- Full description: at most **4000** characters.
- Icon: 512×512, 32-bit PNG with alpha, sRGB, at most 1024 KB; do not draw Google's
  rounded mask or shadow.
- Feature graphic: exactly 1024×500, JPEG or 24-bit PNG without alpha.
- Phone screenshots: 2–8, JPEG or 24-bit PNG without alpha, at most 8 MB, each side
  320–3840 px, long side no more than twice the short side; 1080×1920 is recommended.
- Large-format eligibility: at least four screenshots with at least 1080 px on the
  short side.
- 7-inch and 10-inch tablet screenshots: 4–8 each, short edge at least 1080 px,
  long edge at most 7680 px.
- Gated apps require review notes and usable demo access.

Sources: **NEEDS-VERIFICATION** — `FACTS.md` names Play Console Help article 9866151 but provides no official URL.

PlayKiln's stricter output contract is 6 phone, 4 seven-inch tablet, and 4 ten-inch
tablet screenshots per locale, plus the icon and feature graphic.

Sources: **NEEDS-VERIFICATION** — this is a repository contract, not an external Play rule.

### Data safety and privacy

The Data safety form is mandatory even when the app collects no data. It must match
the privacy policy and every third-party SDK. The privacy policy needs a public URL.

Sources: **NEEDS-VERIFICATION** — no official Data safety or privacy-policy URL is present in `FACTS.md`.

### Account deletion

If the app has accounts, account deletion must be available both in-app and through
an external web resource, and the Data safety form must disclose it.

Sources: **NEEDS-VERIFICATION** — no official account-deletion URL is present in `FACTS.md`.

### Developer verification and package registration

Play apps register package names. Developer-verification protections go live for
users in Brazil, Indonesia, Singapore, and Thailand on **2026-09-30**.

Sources: **NEEDS-VERIFICATION** — no official developer-verification URL is present in `FACTS.md`.

### Closed testing for new personal accounts

Personal developer accounts created after **2023-11-13** must run a closed test with
at least **12 testers opted in continuously for 14 days** before applying for
production access. Plan about three weeks for the first launch.

Sources: **NEEDS-VERIFICATION** — no official closed-testing URL is present in `FACTS.md`.

### Declarations

Ads must be declared truthfully. Content rating and target-audience declarations are
required before publish.

Sources: **NEEDS-VERIFICATION** — no official declaration URLs are present in `FACTS.md`.

### AI-generated content

An app that generates AI content must provide in-app reporting or flagging, must not
generate prohibited content, and should not be a thin API wrapper.

Sources: **NEEDS-VERIFICATION** — no official AI-generated-content policy URL is present in `FACTS.md`.

## 6. Automation boundaries

The Android Publisher v3 edit flow is:
`edits.insert` → `edits.bundles.upload` → `edits.listings.patch` →
`edits.images.upload` → `edits.tracks.update` → `edits.commit`. Valid image types
include `icon`, `featureGraphic`, `phoneScreenshots`, `sevenInchScreenshots`, and
`tenInchScreenshots`.

Sources: **NEEDS-VERIFICATION** — no official Android Publisher API reference URL is present in `FACTS.md`.

API access uses the `https://www.googleapis.com/auth/androidpublisher` scope and a
service account configured in Google Cloud and invited in Play Console with the
required store-listing and release permissions.

Sources: `FACTS.md` contains the OAuth scope URL but no official authentication documentation URL; **NEEDS-VERIFICATION**.

The API cannot create a new Play app. A human must create the app, perform the first
AAB upload, and complete all legal declarations once in Play Console. Factory upload
automation is for updates, not creation; no upload or commit happens without human
approval.

Sources: **NEEDS-VERIFICATION** — no official API limitation URL is present in `FACTS.md`.
