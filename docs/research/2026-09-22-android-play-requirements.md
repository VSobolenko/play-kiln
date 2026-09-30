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

### Play App Signing "enhancements" — the three items on the upload screen (recorded 2026-09-28)

Enrolling in Play App Signing unlocks a family of `.aab` **enhancements**, and Console presents them
together on the upload screen. Verbatim:

> "By letting Google sign the optimized distribution APKs generated from your app bundles, you gain
> access to valuable services like automatic protection and automatic strings translation using
> Gemini models."

Source: <https://support.google.com/googleplay/android-developer/answer/9842756?hl=en>

So a first upload shows the bundle as **signed by Google Play**, **automatic protection** enabled,
and **automatic strings translation** not enabled — three rows of one list, not three findings about
the app. The third is an **optional feature being offered**, not a defect in the app, its manifest or
its listing. It is opt-in, reversible, and its absence blocks nothing.

**Decision for a slot that ships its own translations: decline it.** Play's translation page states
the consequence verbatim:

> "You can select multiple languages to translate to. Play will automatically manage all translations
> for languages you select. **Any existing translations for these languages will be overridden.** To
> keep existing translations or to manage them yourself, do not select these languages."

Source: <https://support.google.com/googleplay/android-developer/answer/9844778?hl=en>

A PlayKiln slot that has `values-<locale>/` directories has reviewable, diffable, git-tracked
translations. Turning the console feature on for those languages replaces them with machine output
held only in Console. The factory's position is therefore:

- **Decline it** for any language the slot already translates. `values-<locale>/` in the repository is
  the source of truth, because a translation in source survives every release, is reviewable in a
  diff, and cannot be wiped by a future upload.
- Enabling it is only coherent **as a bootstrap** for a language the slot does not translate at all,
  followed by reviewing the output and freezing it into `values-<locale>/` — never as the permanent
  home of a translation.
- Console-side edits are fragile: they are replaced by the next build and lost if the feature is
  turned off, so they cannot be the record.

**Ruled out, so nobody "fixes" them later:** Play auto-translates the *store listing* by default and
that is unrelated to in-app strings; **no Play requirement links the app's resource languages to the
languages declared on the listing**, so a single-language listing over a multi-language app is
legitimate and adding listing languages to "clear" such a message would be wasted work; and the only
*blocking* language error in this area is a bundle declaring unrecognized language codes, which a
slot using real `values-<locale>/` codes cannot trigger. A slot's own `android:localeConfig` is a
separate matter already covered by `scripts/check-locales.mjs`.

## 5. Play Console requirements

### Listing text and graphics

- Title: at most **30** characters.
- Short description: at most **80** characters.
- Full description: at most **4000** characters.
- Icon: 512×512, 32-bit PNG with alpha, sRGB, at most 1024 KB; do not draw Google's
  rounded mask or shadow.
- Feature graphic: exactly 1024×500, JPEG or 24-bit PNG without alpha, at most 15 MB.
- Screenshots — **measured live in the RU console on 2026-09-29**, on the app's
  `Стандартная страница приложения` page, from each field's own inline strings. These
  are the strings the console validates against, so they outrank the documentation
  below wherever the two disagree:
  - `Скриншоты для смартфона` (phone): 2–8 files, 16:9 or 9:16, each side 320–3840,
    ≤ 8 MB.
  - `Скриншоты для семидюймовых планшетов` (7-inch): up to 8 files, 16:9 or 9:16,
    each side 320–3840, ≤ 8 MB. The 1080 px floor is **not** this field's rule.
  - `Скриншоты для 10-дюймового планшета` (10-inch): up to 8 files, 16:9 or 9:16,
    each side 1080–7680, ≤ 8 MB.
  - `Скриншоты на компьютере` (Desktop) and `Скриншоты для Android XR`: **optional** —
    neither carries an asterisk. The factory deliberately does **not** produce them
    (owner's instruction, 2026-09-29): the pack is not required to fill them, so there
    is no target, asset or check for either.
- Play's own page gives two long-edge rules that genuinely conflict for tablets. Its
  general **Requirements** section says: "JPEG or 24-bit PNG (no alpha) Minimum
  dimension: 320px Maximum dimension: 3840px The maximum dimension of your
  screenshot can't be more than twice as long as the minimum dimension." Its
  device-specific **Large screens** section says: "Large screens: For Chromebook and
  tablets, you can add a minimum of 4 screenshots to demonstrate your in-app
  experience. Upload screenshots between 1,080 and 7,680px Use a 16:9 aspect ratio
  for landscape and a 9:16 aspect ratio for portrait." The console's own field
  strings above settle both questions for the factory: 16:9 or 9:16 is required in
  every screenshot field, and the per-field windows are 320–3840 (phone, 7-inch) and
  1080–7680 (10-inch). Source:
  <https://support.google.com/googleplay/android-developer/answer/9866151?hl=en>.
- Gated apps require review notes and usable demo access.

Sources: <https://support.google.com/googleplay/android-developer/answer/9866151?hl=en>.

PlayKiln's stricter output contract is 6 phone, 4 seven-inch tablet, and 4 ten-inch
tablet screenshots per locale — 9:16 in every set (`1080×1920` phone and 7-inch,
`1440×2560` 10-inch) — plus the icon and feature graphic.

Sources: **NEEDS-VERIFICATION** — this is a repository contract, not an external Play rule.

#### The release screen's two fields (recorded 2026-09-28, after the first real upload)

The release screen asks for a **release name** and **release notes**, and these are asked for at
*upload* time rather than at listing time. The pack covered every other listing field and neither of
these, so the first publish reached the screen with nothing prepared and no way to count the text —
`store/release-notes.md` now carries both, and `scripts/store-kit.py` measures them.

- **Release name** — an internal Play Console label. It is **not shown to any user**; it exists so
  the console's release list can tell one build from another, and Play prefills it from the bundle's
  `versionName`. The factory states it as `<versionName> (<versionCode>)` and checks it against
  `dist/release-manifest.json`, because a name that disagrees with the uploaded bundle makes the
  console list lie about which build is which — which is how the wrong rollout gets promoted.
- **Release notes / "What's new"** — **shown to users** on the store page, unlike the release name.
  Play puts every language in a **single text box** and separates them with a tag whose name is the
  language, the tags on their own lines: "Add the description for your app's release between the
  relevant language tags... make sure the language tags are on separate lines from the release
  notes" (<https://support.google.com/googleplay/android-developer/answer/9859348?hl=en>).
  `store/release-notes.md` therefore carries a fenced block in exactly that format, so the human
  pastes it whole rather than reassembling it — and the block *is* the note, not a rendering of it,
  so the string that is measured is the string that is pasted. Play caps each language at **500
  characters** and enforces the cap by **truncating the paste** rather than refusing it, so a
  too-long note ships as a half-sentence and nothing says so. The factory measures every note in the
  block, including one for a language the listing does not declare. A language may be left out of the
  block, in which case Play shows no note for it — but English text must never sit under a foreign
  tag, because that claims a translation the listing does not have.

The pack may still be complete without the file — its absence is reported as unfinished work rather
than as a false claim — but a file that *exists* is held to its own text: an over-long note, a
listing language with no note in the block, or a release name that disagrees with the bundle all fail.

### Data safety and privacy

The Data safety form is mandatory for **every** app on any track except internal testing, and is
required even when the app collects nothing — an app that collects no data still needs a form and a
privacy-policy link. It must match the privacy policy, the declared permissions, and every
third-party SDK. Data collection by a bundled SDK is the developer's to declare, not the SDK
vendor's, so a transitive dependency can create a declaration obligation nobody chose — which is
how Play Billing's `datatransport` dependency forced a **Financial info → Purchase history** row
into `teacher-gradebook`.

**"Collect" means transmitting data off the user's device.** Two consequences the factory must
apply deliberately, because guessing either way is a false declaration:

- **On-device access is not collection.** Data an app reads, writes and keeps only in local storage
  does not need to be disclosed. A slot whose whole record is a local file is therefore not
  collecting it, and must not over-declare it — `teacher-gradebook` holds class rosters, marks and
  staff notes entirely on the device and declares none of it.
- **But personal and sensitive data handled at all still carries the policy duties below**,
  including a privacy policy inside the app.

Sources: <https://support.google.com/googleplay/android-developer/answer/10787469> (definition of
collect, the on-device and ephemeral exclusions, the SDK responsibility).

### Privacy policy, and the part the factory kept missing

A privacy policy is required in **two** places, and only one of them was ever checked here:

1. **The store listing** needs a public HTTPS URL. Required for every app, including one that
   collects nothing.
2. **The app itself** must carry the policy. Play's User Data policy states the requirement for an
   app that handles personal or sensitive user data as: *"Have a valid privacy policy in both the
   app's store listing and within the app itself."*

The factory validated only the URL: `store/listing.json.privacyPolicyUrl` had to be a real,
reachable HTTPS page, and `scripts/publish-privacy.mjs` was written to make that true. Nothing
checked the second half, so a slot could ship a perfect hosted policy that no user could open from
the app — and `teacher-gradebook` did exactly that: its About screen explains how the app treats
data in prose, but carries no link, no URL and no way to reach the policy. The prose is not a
policy, and a user who wants the policy is told to leave and find it.

The rule for a new slot is therefore: **if the listing declares a `privacyPolicyUrl`, the app must
expose it.** A readable policy screen or a link that opens it satisfies this; a paragraph about
privacy does not.

### Prominent disclosure and consent — and when it is *not* required

An in-app disclosure shown before consent or a runtime permission is required when the app's
access, collection, use or sharing of personal and sensitive data **may not be within the user's
reasonable expectation** — the policy's own example is data collected in the background while the
user is not engaging with the app. When it applies, the disclosure must be inside the app, must not
live only in the policy, must appear in normal use rather than behind a settings menu, and consent
must be a distinct affirmative action (a tap or a tick) that navigating away does not grant.

This is deliberately **not** a startup checkbox for every app. A slot that is foreground-only, asks
for no runtime permission, and handles only what the user just typed into it does not trigger the
requirement, and adding a consent gate to one is unnecessary friction — a launch-blocking dialog is
also what Play's Disruptive Ads rules penalise when it is an ad. The trigger to record for future
slots is *unexpected or background handling*, not *any data at all*.

Sources: <https://support.google.com/googleplay/android-developer/answer/10144311> (User Data policy,
"Personal and Sensitive User Data" and "Prominent Disclosure & Consent Requirement").

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

### First-publication declarations (the two checkboxes)

Creating an app in Play Console ends with a **Declarations** box that must be acknowledged before
the app exists:

1. **"Developer Program Policies"** — an acknowledgement that the app and its store listing comply
   with *all* Developer Program Policies, not one document. It is an umbrella acceptance: the
   policies that actually bite a factory slot are named elsewhere in this file and in
   `AGENTS.md` — Repetitive Content, User Data, Data safety accuracy, Deceptive Behaviour, Payments,
   and Families.
2. **"US export laws"** — see below. The same screen also asks for acceptance of the Play App
   Signing Terms of Service.

Both are the owner's to accept; no factory script can or should press them, because the second one
is a certification about the developer's legal position, not a formality about the build.

Sources: <https://support.google.com/googleplay/android-developer/answer/9859152> ("In the
'Declarations' section: Acknowledge the 'Developer Program Policies' and 'US export laws'
declarations. Accept the Play App Signing Terms of Service.").

### Export compliance — why a Russian developer clicking the box is normal

The declaration reads: *"I understand that my application may be subject to United States export
laws, regardless of my location or nationality. I agree that I have complied with all such laws,
including any requirements for software with encryption functions. I hereby certify that my
application is authorized for export from the United States under these laws."*

Google explains the mechanism, in its own words: the app is hosted on Google's servers, Google is a
US company, and **the US government treats a download from those servers by someone outside the US
as an export** — so the laws can apply to a developer who is not American and is not in America.
Google blocks downloads to embargoed countries itself, so no factory action is needed for that part.

Three details the factory should know before treating the box as boilerplate:

- **"Encryption functions" does not mean the app wrote crypto code.** Google's page says the app may
  use encryption, and *"even if your application doesn't contain a crypto library, it may call up
  crypto functionality in another program."* A slot with no crypto import still uses HTTPS — through
  Play Billing, or through any SDK — and `teacher-gradebook` is exactly that case: a code search for
  `Cipher`, `KeyGenerator`, `EncryptedSharedPreferences`, `MessageDigest` and friends returns nothing,
  yet the billing SDK's transport makes network requests.
- **Accepting is the correct answer for a normal app.** Most commercial apps fall under the mass
  market category (ECCN `5D992.c`), exportable to most destinations under License Exception ENC. The
  box is not asking for a licence; it is asking the developer to certify compliance.
- **A duty may survive the click.** Mass-market encryption items that use the exception can carry an
  annual **self-classification report** to the US Bureau of Industry and Security. This is the one
  item here the factory cannot settle from Play's documentation, so it is recorded as an open
  obligation rather than a settled fact — see the label below.

For future slots this is a **per-account, once-per-app** step, not per-release: it is acknowledged
when the app is created. What changes per app is whether the app uses encryption at all, and the
honest answer for anything shipping on Play today is yes.

Sources: <https://support.google.com/googleplay/android-developer/answer/113770> (Play's export
compliance page, including the quote above and Google's own statement that it cannot give legal
advice). Classification and the annual-reporting obligation: **NEEDS-VERIFICATION** —
<https://www.bis.doc.gov/index.php/policy-guidance/encryption> is the authority to read, and it is
not in `FACTS.md`, so the factory records the possibility and does not assert the requirement.

### Developer Program Policies

Ads must be declared truthfully. Content rating and target-audience declarations are required
before publish.

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

Sources: <https://developers.google.com/android-publisher/api-ref/rest>,
<https://developers.google.com/android-publisher/edits>.

API access uses the `https://www.googleapis.com/auth/androidpublisher` scope and a
service account configured in Google Cloud and invited in Play Console with the
required store-listing and release permissions.

Sources: <https://developers.google.com/android-publisher/edits>.

The API cannot create a new Play app or fill the legal consents. A human must
create the app, perform the first AAB upload, complete the legal consents and the
content rating questionnaire, and press publish. Factory upload automation is for
updates, not creation; no upload or commit happens without human approval.

Most — but not all — of the "App content" page is automatable. The Data safety
declaration has a documented write endpoint
(`POST /androidpublisher/v3/applications/{packageName}/dataSafety`, body
`{"safetyLabels": "<CSV>"}`); the remaining declarations do not, so they stay a
Console step.

Sources: <https://developers.google.com/android-publisher/edits>,
<https://developers.google.com/android-publisher/api-ref/rest/v3/applications/dataSafety>.

The full verified automation surface, the monetization catalogue endpoints, and the
MCP landscape are mapped in
[`2026-09-23-google-play-automation-surface.md`](./2026-09-23-google-play-automation-surface.md).
