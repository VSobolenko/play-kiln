# Console checklist — Lumintra: Light Planner

Slot: `apps/2026-10-09-lumintra-light-planner/` · package `com.gexetr.lumintralightplanner` ·
one-time price **$9.99**, no in-app product, no ads, no subscription.

This is the walk for the agent after the owner's recorded release approval. Each row names the answer
this app gives and, where one exists, the program that measures it. Requirement briefs:
`docs/play-publishing-playbook.md` §5.1, `docs/play-console-checklist.md`,
`docs/research/2026-09-22-android-play-requirements.md`.

## Build and platform

- [ ] `compileSdk = 37`, `targetSdk = 36`, `minSdk = 26` (factory contract, `AGENTS.md`).
- [ ] `assembleDebug` passes; the release build produces the signed APK + AAB
      (`node scripts/prepare-release.mjs --slot lumintra-light-planner`, after approval).
- [ ] `android:dataExtractionRules` **and** `android:fullBackupContent` are both present
      (`app/src/main/AndroidManifest.xml` declares both; the spot store is what must survive a
      reinstall, and secrets are not in it). Files: `res/xml/data_extraction_rules.xml`,
      `res/xml/backup_rules.xml`.
- [ ] Edge-to-edge and predictive back hold at `targetSdk 36`
      (`android:enableOnBackInvokedCallback="true"` is declared; Navigation 3 hosts its own back stack).
- [ ] Cleartext traffic is off — `android:networkSecurityConfig="@xml/network_security_config"` is
      declared and both endpoints are HTTPS-only.
- [ ] **16 KB page-size readiness.** This slot ships **no native code** (pure Kotlin/Java), so the
      check is expected to pass trivially, but run it on the built artifacts anyway:
      `python scripts/check-16k.py apps/2026-10-09-lumintra-light-planner/dist/*.apk apps/2026-10-09-lumintra-light-planner/dist/*.aab`.
- [ ] Baseline profile present (`baselineprofile/` module) and the startup benchmark unaffected.

## The merged permission list to read after the build

**Read the merged manifest of the built artifact, not the source manifest** — a library can contribute a
permission the source does not show. The two permissions this app must declare, and nothing else:

| Permission | Why it is here |
|---|---|
| `android.permission.INTERNET` | The two keyless fetches: the `api.met.no` cloud forecast and the CelesTrak element-set download. Protection level *normal*; no runtime prompt. |
| `android.permission.POST_NOTIFICATIONS` | Android 13+ only. Requested **only when the reminder is switched on**. Denial disables the reminder and nothing else. Protection level *dangerous*, added in API 33. |

**Confirm that NO location permission of any kind appears.** None of these may be present:
`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`,
`ACCESS_MEDIA_LOCATION`, `FOREGROUND_SERVICE_LOCATION`, or any other `*LOCATION*` permission. The app
computes a sky from a **saved coordinate the user types or picks**; it never asks Android for the
device's position, and `Geocoder` reverse lookup needs no location permission.

Also confirm these are **absent**, because the design deliberately omits them:

- every storage/media permission (`READ_EXTERNAL_STORAGE`, `READ_MEDIA_*`) — the photo picker is not
  used and export/share go through `FileProvider` + the system document picker;
- `RECEIVE_BOOT_COMPLETED` — WorkManager reschedules its own work; the widget re-renders from
  persisted state;
- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` — the reminder is inexact by design;
- `CAMERA`, `BILLING`.

Where to read it: the merged manifest Gradle produces for the release variant, or the built APK itself
(`apkanalyzer manifest permissions <apk>` / `aapt2 dump permissions <apk>`; DroidAgentKit's
`android_apk_analyze` reads the same list). Record the observed list in the release report.

## Declarations and forms

- [ ] **Data safety** — **one** collected row: **Location → Approximate location**, purpose **App
      functionality**, **optional**, **encrypted in transit**, because the saved spot's coordinate is
      transmitted to `api.met.no`. Everything else is local. No data goes to the developer. Full answers:
      `store/data-safety.md`; run `play-auditor` before submitting. The over-declaration to **Precise
      location** is available if the auditor prefers the stricter reading.
- [ ] **Ads** — **No, the app does not contain ads.** No ad SDK ships (`store/listing.json.ads`).
- [ ] **Content rating** — questionnaire branch "all other app types"
      (`store/listing.json.contentRating.questionnaire = "notes"`); expected rating is 3+ / Everyone.
- [ ] **Target audience** — **adults** (`targetAudience = "adults-only"`); not directed at children, so
      the Families policy does not apply and the wizard skips `О приложении`, `Реклама` and
      `Страница приложения` on that branch.
- [ ] **Category** — `PHOTOGRAPHY` (`store/listing.json.category`, a console token).
- [ ] **Privacy policy URL** — see the placeholder row below.
- [ ] **Review notes / demo access** — paste `store/review-notes.md`. There is **no login gate**, so no
      credentials are needed; the notes state that explicitly and give the 60-second path to the core
      value.
- [ ] **Release name** — `1.0.0 (1)` (`store/release-notes.md`); Play prefills it from the bundle's
      `versionName`, and `scripts/store/validate.py` checks it against `dist/release-manifest.json`.
- [ ] **Release notes** — paste the whole `<locale>` block from `store/release-notes.md` (en-US 484 /
      ru-RU 486 code points, both inside Play's 500 cap and in finished-product tone).

## The two first-publication declarations

Both are ticked **by the agent**, once per app rather than per release, and **only after the owner's
release approval is recorded** (`.cursor/rules/release-gate.mdc` stage 5; owner's decision, recorded
2026-10-03: the approval is the product checkpoint and the click is not).

- [ ] **Developer Program Policies** — confirmed when the recorded approval exists.
- [ ] **US export laws** — an attestation about the encryption the app uses. This app uses HTTPS (the
      Android stack's own TLS) for both fetches, so the declaration is made on that basis. The annual BIS
      self-classification question remains an **open obligation** recorded by the factory, not settled
      law.

Confirm `node scripts/approve-release.mjs --check --slot lumintra-light-planner` reports the approval
**before** ticking either box. If it refuses, ask the owner — never edit or delete the record.

## In-app review: no incentive, no reward, no sentiment gate

- [ ] The review surface is Google's own **Play In-App Review** (`com.google.android.play:review-ktx`),
      not a custom dialog.
- [ ] It is triggered **once, at an earned moment** (after a session brief exists / after a share), never
      at launch, never during onboarding, never on the paywall. The app **does not ask a sentiment
      question** first and **does not gate** the prompt on a rating — no "Do you like the app?", no
      "Would you rate 5 stars", no "not now".
- [ ] **No incentive or reward attaches to the review.** Nothing is unlocked, granted, discounted or
      promised for leaving a rating or review, and no reward prompt exists anywhere in the app.
- [ ] The **`Rate us`** row in Settings opens the **Play listing**, never the in-app review flow, and the
      caller is told nothing about whether the review card appeared (that is the API's contract).
- [ ] The review flow is requested and launched in the same user gesture, and a quota-exhausted flow
      changes nothing visible in the app.

## No reserved-host placeholder reaches the release

- [ ] `store/listing.json.privacyPolicyUrl` is a **real public HTTPS URL**, published by the factory:
      `node scripts/publish-privacy.mjs --slot lumintra-light-planner`. It is **empty today** — the honest
      unfinished state — and is not to be replaced with a reserved host.
- [ ] **No** `example.com` / `example.org` / `example.net` / `example.edu` / `localhost` / `invalid` host,
      and no non-HTTPS scheme, appears anywhere in the pack. The check is `pack.placeholders`
      (`scripts/core/assets.mjs`) via `node scripts/verify.mjs --slot lumintra-light-planner`.
- [ ] The same URL is openable **from inside the app** (Credits → privacy policy), measured by
      `slot.inAppPolicy` (`node scripts/check-in-app-policy.mjs --slot lumintra-light-planner`).

## Signing and upload

- [ ] Upload keystore configured locally **outside** the repository
      (`~/.playkiln/keystores/com.gexetr.lumintralightplanner.jks`) with a gitignored
      `keystore.properties`; **nothing** key-shaped is committed.
- [ ] Play App Signing enrolled.
- [ ] **Do not enable Play Console's automatic string translation for `ru-RU`** — it would override the
      git-tracked `values-ru/` translations
      (`AGENTS.md` § "Store-pack contract"; owner's standing instruction).
- [ ] No client-side secret ships: `node scripts/check-client-secrets.mjs --slot lumintra-light-planner`.

## The three tooling assists (owner's hand only)

Exactly three console acts cannot be automated; at each the agent opens the page, names absolute paths,
asks, and waits:

1. **Store-graphic file inputs** — `…/store-listings/default/edit`, the five image fields, from
   `store/assets/`.
2. **AAB/APK chooser** — the release form's bundle upload, the artifact under `dist/`.
3. **Promo-code download** — `Монетизация с помощью Play → Promotions` (`.../promotions`): click
   **`Скачать коды` exactly once**, then ask the owner to confirm the save window and wait. **Never press
   it twice.**

While files are being chosen, do not reload or navigate the page — the chosen files live in the form
until the next save.

## Open items recorded rather than hidden

- **CelesTrak licence for a paid app** — the annex records the commercial-use question as not settled
  (`specs/satellite-passes/spec.md` requires it be settled before release by reading
  `https://celestrak.org/`'s terms and, if needed, writing to the address its policy publishes). **Set
  this before upload**; if the answer is no commercial grant, the pass feature must be narrowed to the
  cached/local path or dropped — which is an owner decision, not a builder's.
- **`privacyPolicyUrl` is empty** until `scripts/publish-privacy.mjs` runs. Publishing it is development
  work, not release work: rewriting the listing after an approval is recorded voids that approval.
- **`store/studio.json`'s palette vs the shipped `ui/theme/Color.kt`** — `scripts/core/palette.mjs`
  measures the two against each other in the `palette` group of `verify.mjs`; read its verdict, do not
  assume agreement.
