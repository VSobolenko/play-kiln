# M — Play integrations, permissions, Data safety, attribution, revenue levers

**Slot:** `lumintra-light-planner` (Lumintra, `com.gexetr.lumintralightplanner`)
**Product:** paid **$9.99 one-time**, `purchase_only_no_ads`, closed-source, Android light-and-sky session planner
**Date:** 2026-10-08 · **Author:** research worker · **Status:** live documentation read in this session

Every claim is labelled `MEASURED` (read/ran in this session, with the URL or `path:line`),
`ON RECORD` (a named source states it), `INFERRED`, or `UNVERIFIED` (with the check that would settle it).
All URLs below were fetched in this session unless stated otherwise.

Sources fetched (and what each settled):

| # | URL | Date shown on page | Settled |
|---|---|---|---|
| S1 | https://developer.android.com/guide/playcore/in-app-review | Last updated **2026-01-30** | device requirements, quotas, "don't ask questions first", design rules, Data safety table |
| S2 | https://developer.android.com/guide/playcore/in-app-review/kotlin-java | Last updated **2026-10-01** | artifact, version, ReviewManager/ReviewInfo/launchReviewFlow code shape |
| S3 | https://developer.android.com/guide/playcore | Last updated **2026-10-01** | the Play Core split, per-feature coordinates, Play Core SDK ToS |
| S4 | https://developer.android.com/guide/playcore/in-app-updates | Last updated **2026-01-30** | in-app updates scope + its Data safety row |
| S5 | https://developer.android.com/distribute/marketing-tools/linking-to-google-play | Last updated **2026-08-07** | the fallback store-listing intent |
| S6 | https://support.google.com/googleplay/android-developer/answer/9898684 | live | *User Ratings, Reviews, and Installs* policy |
| S7 | https://support.google.com/googleplay/android-developer/answer/10787469 | live | the Data safety form, and Play's definition of "collect" |
| S8 | https://api.met.no/doc/TermsOfService | live | MET Norway ToS: User-Agent, rate, caching, coordinates, privacy |
| S9 | https://api.met.no/doc/License | live | MET Norway CC BY 4.0 + the credit wording |
| S10 | https://www.eso.org/public/outreach/copyright/ | live | ESO credit is mandatory, full and visible |
| S11 | NASA media guidelines (https://www.nasa.gov/nasa-brand-center/images-and-media/) | live via search result | acknowledgement, no-endorsement |
| S12 | IAU constellation charts pages + IAU Copyright page | live | CC BY 4.0, full credit, no IAU logo |
| S13 | https://www.solarsystemscope.com/textures/ (+ Qt attribution page) | live | CC BY 4.0 + the working attribution string |
| S14 | FK6 / GAVO data-centre pages | live | **CC0 1.0** |
| S15 | Google Maven `maven-metadata.xml` for `review`, `review-ktx`, `app-update` | — | **the current versions, from Google's own repo** |
| S16 | The AARs `review-2.0.2.aar`, `review-ktx-2.0.2.aar` downloaded from `dl.google.com` | — | the merged manifest, the KTX signatures, the bundled licence |
| S17 | https://developer.android.com/topic/performance/vitals (direct fetch timed out twice; content read via search result quoting the page) | — | core vitals affect visibility |

---

## 1. Play In-App Review — the exact contract

### 1.1 Answer to the owner's request, up front

**He is right that the API exists, and it is the current one.** He is **wrong about one design
detail, and it matters**: the flow must **not** be fired from a "Rate us" button. Google's own page
says so in as many words (S1, §Quotas):

> "you should not have a call-to-action option (such as a button) to trigger the API, as a user might
> have already hit their quota and the flow won't be shown, presenting a broken experience to the
> user. For this use case, redirect the user to the Play Store instead."

So the correct build is **two separate things**:

1. **Settings → "Rate Lumintra"** is a plain **open-the-Play-listing** link (§1.9). It always works,
   it can never silently do nothing, and it is what a user who wants to rate will use.
2. **The in-app review flow** (the API) is fired **automatically at a moment the user has just had a
   good experience** (e.g. a session brief was produced or shared, after N briefs), with the store
   listing as the fallback path on the same screen path but never as the trigger of the API.

### 1.2 Artifact, version, licence, repository

| Item | Value | Label / evidence |
|---|---|---|
| Artifact | `com.google.android.play:review` (`implementation`), plus `com.google.android.play:review-ktx` for Kotlin | MEASURED — S2 and S3 both print both coordinates |
| **Current stable version** | **`2.0.2`** (both artifacts) | MEASURED — S15 `maven-metadata.xml`: `<latest>2.0.2</latest><release>2.0.2</release>`, `lastUpdated 20241018`; and S2 (page updated 2026-10-01) prints `2.0.2` |
| Repository | **Google's Maven repository** (`google()`), not Maven Central | MEASURED — S2: *"This dependency is downloaded from the Google's Maven repository. So, make sure you also include that repository in your project's build.gradle file."* |
| Licence | **Play Core Software Development Kit Terms of Service** (last modified 2020-09-24) — *not* an OSS licence like Apache-2.0 | MEASURED — the ToS text is printed on S3 and shipped inside the AAR as `LICENSE` (S16) |
| ToS terms that bind the build | You may copy/distribute Redistributable Code **"solely for inclusion as part of your API Client"**; **"You will not modify, translate, or create derivative works of Redistributable Code."** | MEASURED — S3 §"Play Core Software Development Kit Terms of Service" ¶4 |
| Does it need the old monolithic `play-core`? | **No — `play-core` is gone.** | MEASURED — S3: *"The Google Play Core Java and Kotlin library have been split into multiple separate libraries, one for each feature. Update to the new libraries…"*, followed by the per-feature migration table |
| What the old import became | `com.google.android.play.core.tasks.*` → `com.google.android.gms.tasks.*`; class names unchanged | MEASURED — S3 §"Common migration steps" |
| minSdk of the library | 21 | MEASURED — S16, the AAR manifest declares `android:minSdkVersion="21"` (compatible with the slot's minSdk 26) |

**So: exactly `com.google.android.play:review:2.0.2` (+ `review-ktx:2.0.2`).** Do **not** add
`com.google.android.play:core` — it does not exist in the split world.

### 1.3 API shape

**Java/Kotlin interface (the shape the docs show):**

```kotlin
val manager: ReviewManager = ReviewManagerFactory.create(context)      // context, not activity
val request: Task<ReviewInfo> = manager.requestReviewFlow()            // Task API (GMS tasks)
request.addOnCompleteListener { task ->
    if (task.isSuccessful) {
        val reviewInfo: ReviewInfo = task.result
        manager.launchReviewFlow(activity, reviewInfo)                 // Activity REQUIRED
    } else {
        // @ReviewErrorCode val code = (task.exception as ReviewException).errorCode
    }
}
```

MEASURED — S2 §"Create the ReviewManager" / §"Request a ReviewInfo object" / §"Launch the in-app review flow".
Two things the doc states that decide the shape:

- **`ReviewInfo` is perishable**: *"The `ReviewInfo` object is only valid for a limited amount of time.
  Your app should request a `ReviewInfo` object ahead of time (pre-cache) but only once you are certain
  that your app will launch the in-app review flow."* (S2). So: request and launch **in the same
  user-visible moment**, not minutes apart.
- **`launchReviewFlow` takes an `Activity`, not a `Context`** (S2). In Compose the hosting Activity
  must be obtained (`LocalContext.current as ComponentActivity`, or `LocalActivity.current` on newer
  `activity-compose`). The slot's `activity-compose` version is not read here — see the ledger.

**The Kotlin-extensions shape (MEASURED — read out of the shipped jar, S16):**

The `review-ktx:2.0.2` AAR's `classes.jar` contains exactly one file,
`com/google/android/play/core/ktx/ReviewManagerKtxKt.class`, whose constant pool carries two
extension functions on `ReviewManager`:

```text
requestReview : (ReviewManager, Continuation) -> Object          // suspend fun ReviewManager.requestReview(): ReviewInfo
launchReview  : (ReviewManager, Activity, ReviewInfo, Continuation) -> Object
                                                                 // suspend fun ReviewManager.launchReview(activity: Activity, reviewInfo: ReviewInfo)
```

Note the KTX names are **`requestReview()` / `launchReview()`**, *not* `requestReviewFlow()` /
`launchReviewFlow()`. Nothing else is in the artifact (no manifest components, no permissions).

**Proposed Compose shape (INFERRED from S2 + S16 — not yet compiled):**

```kotlin
// In the Activity, not in a Composable that owns no Activity:
val manager = ReviewManagerFactory.create(activity)
val reviewInfo = manager.requestReview()          // ktx suspend, returns ReviewInfo
manager.launchReview(activity, reviewInfo)        // ktx suspend; returns when the flow ends
// Regardless of outcome: continue the app's normal flow. Never branch on the result.
```

### 1.4 The constraints that decide the design (each with its source)

| # | Constraint | Source (quoted) | What it means for Lumintra |
|---|---|---|---|
| C1 | **A time-bound quota exists, and its value is secret and changeable** | S1 §Quotas: *"Google Play enforces a time-bound quota on how often a user can be shown the review dialog. Because of this quota, calling the `launchReviewFlow` method more than once during a short period of time (for example, less than a month) might not always display a dialog."* + *"The specific value of the quota is an implementation detail, and it can be changed by Google Play without any notice."* | One call per user, at one earned moment. Never a per-screen or per-session call. The window is *"less than a month"* at minimum. |
| C2 | **There is no call-to-action button that triggers the API** | S1 §Quotas: *"you should not have a call-to-action option (such as a button) to trigger the API… For this use case, redirect the user to the Play Store instead."* | **Kills the Settings-button design.** Settings row → Play listing intent (§1.9). |
| C3 | **The dialog is not guaranteed, and the caller is told nothing** | S1/S2: *"The flow has finished. The API does not indicate whether the user reviewed or not, or even whether the review dialog was shown. Thus, no matter the result, we continue our app flow."* and S2: *"If an error occurs during the in-app review flow, do not inform the user or change your app's normal user flow."* | Quota exhausted ⇒ **the coroutine just returns**. No toast, no "thanks for rating", no retry, no UI change. The app must look identical whether the card appeared or not. |
| C4 | **No question may precede or accompany the prompt** | S1 §When to request: *"Your app shouldn't ask the user any questions before or while presenting the rating button or card, including questions about their opinion (such as 'Do you like the app?') or predictive questions (such as 'Would you rate this app 5 stars')."* | **No "Are you enjoying Lumintra?" gate, no NPS screen, no two-step "yes/no then rate".** This is also why a filtered prompt ("only ask happy users") is not just tacky — it is exactly the pattern the sentence names. |
| C5 | **No gating on sentiment, no incentive, no manipulation** | S6 §Full Policy: *"Developers must not attempt to manipulate the placement of any apps on Google Play… including… inflating product ratings, reviews, or install counts by illegitimate means, such as fraudulent or incentivized reviews and ratings"*; Key Considerations: *"Don't offer rewards or incentives for ratings or reviews"*, *"Don't use deceptive pop-ups or force users to rate your app."* | No reward, no unlock, no "rate 5★ to remove ads", no repeated nagging. Leaving a review must be worth **zero** in app currency. |
| C6 | **The card must not be tampered with** | S1 §Design guidelines: *"Surface the card as-is, without tampering or modifying the existing design in any way, including size, opacity, shape, or other properties."*, *"Don't add any overlay on top of the card or around the card."*, *"The card and the card's background should be on the topmost layer. Once the card has surfaced, don't programmatically remove the card."* | No custom theming of the card, no scrim, no dim, no `Dialog` drawn over it, no programmatic dismissal, no auto-dismiss timer. |
| C7 | **Trigger only after the user has got real value** | S1 §When to request: *"Trigger the in-app review flow after a user has experienced enough of your app or game to provide useful feedback."* and *"Don't prompt the user excessively."* | The earned moment is **after a session brief exists / after a share** — not on first launch, not on install, not on the paywall. |
| C8 | **Needs the Play Store app present** | S1 §Device requirements: *"In-app reviews only work on the following devices: Android devices (phones, tablets, and TVs with Google TV) running Android 5.0 (API level 21) or higher that have the Google Play Store installed. ChromeOS devices that have the Google Play Store installed."* | A device with no Play Store ⇒ the request fails. INFERRED (from C3 + C8, not stated in the doc): the failure surfaces as an unsuccessful `Task`/thrown `ReviewException` in `requestReviewFlow` — which the app must swallow silently. |
| C9 | **The flow is asynchronous; wait for it before advancing** | S2: *"Wait until the user has completed the in-app review flow before your app continues its normal user flow (such as advancing to the next level)."* | Don't start navigation/animation that would tear down the Activity while the card is up. |

**No permission.** MEASURED — S16: the merged `AndroidManifest.xml` of `com.google.android.play:review:2.0.2`
(the AAR ships the *merged* manifest) is:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.google.android.play.review" android:versionCode="20002" android:versionName="2.0.2">
    <uses-sdk android:minSdkVersion="21" android:targetSdkVersion="23" />
    <application />
</manifest>
```

No `<uses-permission>`, no `<activity>`, no `<provider>`, no `<service>`, no `<receiver>`.
`review-ktx:2.0.2` is identical. **The review library adds nothing to the slot's permission surface and
no manifest component.**

### 1.5 Data safety effect of In-App Review

MEASURED — S1 §Data Safety prints a per-feature table and frames it:

> "The Play Core libraries are your app's runtime interface with the Google Play Store. As such, when
> you use Play Core in your app, the Play Store runs its own processes, which include handling data as
> governed by the Google Play Terms of Service."

| Data collected on usage | User-entered data (rating and free-text review) |
|---|---|
| Purpose | *"The data collected is used to leave a review on the Play Store."* |
| Encryption | *"Data is encrypted."* |
| Sharing | *"…used in a public review on the Play Store or, if the app is in a closed test track, the data collected is shared privately with the app developer."* |
| Deletion | *"Users can delete their reviews in their Google Play Store account or Google Account."* |

And: *"you are solely responsible for deciding how to respond to Google Play's data safety section form."*

**INFERRED, and stated as a judgement rather than a fact:** this is the Play Store's own review
surface, driven by the Play Store app, entered by the user inside Play's UI, and published as a Play
review — it is not data Lumintra transmits to its own server. The defensible position is **no new
Data safety row for the review flow**, with the note that the rating/comment a user submits is handled
under Google's Terms of Service as shown above. The conservative alternative — declaring "App activity
→ User-generated content: Reviews" — is available if the reviewer of this spec prefers to over-declare.
Either way it **changes nothing about permissions**.

### 1.6 Play In-App **Updates** — is it worth it for this app?

| Fact | Source |
|---|---|
| Supported Android 5.0 (API 21)+; mobile/tablet/ChromeOS only | S4 |
| Two flows: **flexible** (background, user keeps using the app) and **immediate** (fullscreen, blocks until updated) | S4 |
| Not compatible with `.obb` expansion files | S4 |
| Its Data safety row: *"Device metadata / Application version / List of modules and asset packs installed"* — purpose *"to determine if an update is available and what the size of the update is expected to be"*, *"Data is not transferred to any third parties"*, *"Data is deleted following a fixed retention period."* | S4 |
| Artifact/version | `com.google.android.play:app-update:2.1.0` (+`app-update-ktx`) — MEASURED, S15 `maven-metadata.xml`: `<latest>2.1.0</latest>`, `lastUpdated 20230522` |

**Recommendation: do not use it for v1.** INFERRED, from three facts read above rather than from taste:
(a) Play already updates apps in the background on unmetered connections (S4 opens by saying exactly
that), so the benefit is a reminder, not a capability; (b) the app is a plan-before-you-drive tool —
being *forced* into an immediate-update screen before a session is the opposite of the job; (c) it adds
a second Play Core artifact **and a Data safety row** about device metadata for a benefit Play already
delivers. A flexible update prompt inside Settings ("check for updates") would be the low-risk version
if it is ever wanted — it is not needed for a first release.
`UNVERIFIED`: no measurement exists here of the install-rate effect of in-app updates on a paid
planning app; the check that would settle it is a shipped A/B reading in Play Console.

### 1.7 Play Core library split — which artifact exactly

MEASURED — S3 prints the split table verbatim. The relevant rows:

| Feature | Artifact (Kotlin extension artifact alongside) |
|---|---|
| In-app review | `com.google.android.play:review` (`:review-ktx`) |
| In-app updates | `com.google.android.play:app-update` (`:app-update-ktx`) |
| Asset delivery | `com.google.android.play:asset-delivery` (`-ktx`) |
| Feature delivery | `com.google.android.play:feature-delivery` (`-ktx`) |

**Lumintra needs exactly one: `com.google.android.play:review` (+`-ktx`).** The old
`com.google.android.play:core` is the pre-split monolith and must not appear.

### 1.8 Policy pitfalls that could get an app removed

All five are read from the two sources above; each is a **strike-bearing** risk, because the Play
Developer Program Policies escalate rejection → removal → suspension → account termination, and every
app in the catalogue is removed with the account.

1. **Incentivized ratings/reviews** (S6). *"Developers must not attempt to manipulate the placement of
   any apps on Google Play… such as fraudulent or incentivized reviews and ratings."* Offering a
   discount, an unlock, extra targets, a free horizon pack, or anything of equivalent value for a
   rating is the named violation, and the policy's own illustration is a discount-for-high-rating
   notification.
2. **Deceptive or forced review prompts** (S6). *"Don't use deceptive pop-ups or force users to rate
   your app."* A prompt that blocks the app, repeats until answered, or pretends a rating is required
   is this one.
3. **Opinion-gating the prompt** (S1 + S6). Asking "do you like the app?" first, then routing only the
   happy users to the API, is both (a) the question S1 forbids and (b) manipulation under S6 —
   it is a filtered fiction of the app's real rating.
4. **Customising the review card** (S1 §Design guidelines). Tampering with the card, overlaying it, or
   programmatically dismissing it is the third S1 rule set and the easiest to break by accident in
   Compose (a scrim, a themed `Dialog`, a `LaunchedEffect` timer).
5. **Review text that carries a coupon, game code, email address or link** (S6, and the factory's own
   `store/release-notes.md` / In-App Review scope does not create it). Relevant here only if the app
   ever *encourages* a user to put promo content in a review — the Share/postcard feature must not ask
   for that.

### 1.9 The fallback: a plain "open the Play listing" link

**Yes — the app should have one, and it should be the Settings "Rate us" row.** S5 (Last updated
2026-08-07) gives the exact form and the exact intent:

```kotlin
val intent = Intent(Intent.ACTION_VIEW).apply {
    data = Uri.parse("https://play.google.com/store/apps/details?id=com.gexetr.lumintralightplanner")
    setPackage("com.android.vending")
}
startActivity(intent)
```

MEASURED — S5 §"Linking from an Android App": *"create an `Intent` that opens a URL. As you configure
this intent, pass `"com.android.vending"` into `Intent.setPackage()` so that users see your app's
details in the Google Play Store app instead of a chooser."*
The URI family is `https://play.google.com/store/apps/details?id=<package_name>` (S5 summary table).
Note `setPackage("com.android.vending")` is what removes the app-chooser; on a device without the Play
Store the `ActivityNotFoundException` must be swallowed (the row should then do nothing visible, or
open the web URL — INFERRED, a design choice, not a documented requirement).

---

## 2. Permissions and Data safety — the honest bill

### 2.1 The permissions table

| Permission | Why it would be there | What breaks without it | Compliance cost |
|---|---|---|---|
| `android.permission.INTERNET` | Fetch the hourly **total cloud-cover** forecast from `api.met.no`, and fetch/no. the satellite **orbital elements** | Nothing in the *planned* feature list except the forecast and the passes: the sky scene, the drawn horizon, the twilight/lunar geometry, the ~250-object target catalogue, scouting spots, the postcard and the session brief are all local. **Recommendation: make "works fully offline except the cloud forecast and satellite passes" a stated property, and never claim the opposite.** | Protection level **normal** — no runtime prompt, no user-visible grant. It is not itself a Data safety row; it is the pipe the coordinate travels down (see §3). |
| `android.permission.ACCESS_COARSE_LOCATION` | **Optional** convenience only: "use my current location" when creating a spot | **Nothing.** With a saved spot the user picks a place and names it; a manual coordinate entry is also possible; the app's own coordinate picker needs `INTERNET`, not location. Without the permission the job loses *only* auto-detecting where the user is standing. | Protection level **dangerous** (runtime prompt). Play's location policy expects location to be core functionality with a prominent disclosure; declaring a permission the job does not need is a trust and review-surface cost with no money line. **A keyless map/place search or a manual lat/long entry replaces it entirely** — and `Geocoder` (reverse geocoding a chosen coordinate into a place name) requires **no** location permission. MEASURED — S-extra: `https://developer.android.com/reference/android/location/Geocoder` documents no `@RequiresPermission` on any `getFromLocation`/`getFromLocationName` overload; it only warns that *"This API may hit the network"*. |
| `android.permission.POST_NOTIFICATIONS` | Only if a **reminder** or a **widget** is added ("dark window starts at 23:10 — leave now") | A reminder / widget cannot notify. | Protection level **dangerous**, added in **Android 13 (API level 33)**. MEASURED — S-extra: `https://developer.android.com/about/versions/13/behavior-changes-all` §"Runtime permission for notifications": *"Android 13 (API level 33) introduces a runtime notification permission: `POST_NOTIFICATIONS`."* and *"`POST_NOTIFICATIONS` … Protection level: dangerous"* (`Manifest.permission` reference). The user is prompted; on upgrade to 13+ an eligible existing app can be **pre-granted** if it already had a notification channel. **It is not in the current screen list — do not declare it until a reminder actually ships.** |
| Storage (`READ_/WRITE_EXTERNAL_STORAGE`, `MANAGE_EXTERNAL_STORAGE`) | — | — | **None needed. Confirmed: the share/postcard handoff must use none of them.** The modern correct route is `Intent.ACTION_SEND` + `FileProvider` with `FLAG_GRANT_READ_URI_PERMISSION` for a share sheet, or `ACTION_CREATE_DOCUMENT` (SAF) to write a file the user picks. Both grant a single-URI permission and need no storage permission. A `Bitmap`/`PdfDocument` written into `cacheDir`/`filesDir` and shared through `FileProvider` is the same shape. |
| Camera / microphone / Bluetooth / `QUERY_ALL_PACKAGES` / `com.android.vending.BILLING` | — | — | **None.** No camera job, no audio, no device pairing, no in-app product (see §5.6 — billing must not be added). |

**Bottom line:** the honest permission set is **`INTERNET` alone**, plus `POST_NOTIFICATIONS` **only if**
a reminder/widget ships, plus `ACCESS_COARSE_LOCATION` **only if** an "auto-locate me" convenience is
deliberately added — and that last one is a choice, not a requirement.

### 2.2 Does the app *need* a runtime location permission?

**No.** MEASURED/INFERRED chain:
- MEASURED (S8): `api.met.no` forecasts are requested **by coordinate**, with no device-location
  concept — the API has no idea where the phone is.
- MEASURED (S-extra, Geocoder): turning a chosen coordinate into a human place name needs no
  location permission — no geocoding overload carries `@RequiresPermission`. It is nevertheless
  **network-dependent, API-branched and optional**: the reference warns *"This API may hit the network,
  and may block for excessive amounts of time"* and returns *"null or empty list … if there is no
  backend service available"*; `Geocoder.isPresent()` (**API 9**) is the documented presence check and
  the asynchronous `GeocodeListener` overload is **API 33** against a `minSdk` of **26**, so API 26–32
  uses the deprecated overload on a background thread. It stays keyless enrichment that degrades to
  coordinates, never a required path, and **no third-party geocoding API with a credential may replace
  it** (`.cursor/rules/client-secrets.mdc`).
- MEASURED (S5): a map/place search is a URL/HTTP concern, not a location one.
- INFERRED: therefore every planned feature — saved spots, drawn horizons, the sky scene, the brief,
  the postcard — is reachable with a user-chosen coordinate and `INTERNET` alone.

**If** the "coarse-location convenience" is offered, the honest description of what it buys is one
sentence: *"the app can start a new spot at wherever you are standing instead of you tapping the map"* —
and what the job loses without it is exactly that one tap.

### 2.3 Odds and ends the factory's rules require stating

- **`api.met.no` requires an identifying `User-Agent`.** MEASURED (S8) §Identification: *"All requests
  must (if possible) include an identifying User-Agent-string (UA) in the request with the
  application/domain name, optionally version number… If we cannot contact you in case of problems, you
  risk being blocked without warning."* Valid example given: `"AcmeWeatherApp/0.9 github.com/acmeweatherapp"`.
  The slot must set a `User-Agent` header (Lumintra + a contact URL/address).
- **Coordinates must be truncated to 4 decimals.** MEASURED (S8) §Traffic 5: *"When using requests with
  latitude/longitude, truncate all coordinates to max 4 decimals… For new products, requests with 5+
  decimals will return a 403 Forbidden."*
- **Cache, do not poll; never poll from the background.** MEASURED (S8) §Traffic 1–2, 6: respect
  `Expires`, use `If-Modified-Since`, *"Applications on mobile devices must not retrieve new data as
  long as the application is not in use."* This is a design constraint that pushes **away** from a
  reminder-driven background fetch.
- **HTTP is not allowed.** MEASURED (S8) §Protocols: *"We only support encrypted HTTPS… Unencrypted
  HTTP requests are redirected to HTTPS… If you don't fix it after a reasonable interval we reserve the
  right to block such traffic."* — which also makes "encrypted in transit = Yes" true in Data safety.
- **MET recommends a proxy, and warns it logs IP + coordinates.** MEASURED (S8) §Personal data:
  *"If you call the API directly from an app or browser, the user's IP address will be stored in our
  logs, along with any possible geocoordinates used in requests. To guarantee anonymity we recommend
  using a proxy gateway…"* and §Direct client-to-API connections: *"Browsers and mobile apps should not
  contact the API directly, but instead use a local proxy (backend for frontend) server…"* — followed
  by the allowance: *"Low-volume websites and mobile apps may use simple cross-origin Javascript
  requests and direct image links, provided the sites identifies itself as described above."*
  **This matters for §3 and for the recurring-cost line in the plan** (a proxy is a cost the
  `purchase_only_no_ads` price cannot carry per user; direct access is permitted for a low-volume app
  and is the shape the economics assumes).
- **Trademark/naming restriction.** MEASURED (S8) §Trademarks and naming restrictions: *"you may not
  use the word 'Yr' as a part of your service name, or attach the Yr logo to your service."* — the
  listing and the credits screen must not say "Yr".
- **Orbital elements source.** The recon proposes **fetching** them at runtime (not bundling). The
  licence of the specific feed is **not** read in this session → `UNVERIFIED`; the check is to name the
  feed and read its terms before the spec freezes (see ledger).

---

## 3. Data safety — the exact declarations

### 3.1 Play's own definition of "collect"

MEASURED (S7) §"Data collection":

> **"Collect" means transmitting data from your app off a user's device.**

and immediately, the line the factory's rule quotes:

> *"Libraries and SDKs: This includes user data transmitted off device from your app by libraries
> and/or SDKs used in your app, irrespective of whether data is transmitted to you or a third-party
> server."*

So **yes**: sending a saved coordinate to `api.met.no` is a transmission of user data off the device.
It is a **"collect"** under Play's definition, even though the destination is not the developer and no
account exists.

### 3.2 The ephemeral-processing exception, and why it is the crux here

MEASURED (S7), same section:

> *"Ephemeral processing: User data transmitted off device that is processed ephemerally needs to be
> included in your form response, but if it meets the standard below, it will not be disclosed in your
> app's Data safety section on Google Play."*
> *"Processing data 'ephemerally' means accessing and using it while the data is only stored in memory
> and retained for no longer than necessary to service the specific request in real-time."*
> **Play's own worked example is a weather app:** *"a weather app that transmits user location off the
> device to fetch the current weather at the user's location but only uses location data in memory and
> does not store that data once the request has been fulfilled, can treat its transient use of location
> as ephemeral."*

Lumintra is literally the example Play wrote. **But one measured fact cuts against the strongest form
of the claim:** MET Norway says **it** stores what it receives — *"the user's IP address will be stored
in our logs, along with any possible geocoordinates used in requests"* (S8). Play's standard is about
the data being *"retained for no longer than necessary to service the specific request"*, and the
receiver is retaining it.

**Recommended, defensible position (INFERRED from the two measured texts, and flagged as a judgement,
not a documented ruling):** declare the row normally rather than claiming the ephemeral exemption.

| Data type | Collected? | Shared? | Purpose | Required/Optional | Other |
|---|---|---|---|---|---|
| **Location → Approximate location** (the saved spot's coordinates, truncated to 4 decimals as MET requires) | **Yes — collected** | **No** (MET is a data source the app queries on the user's behalf; the transfer is app → API, not re-transfer to a third party by the developer) | **App functionality** (fetch the cloud-cover forecast) | **Optional** — the user chooses a spot, can refuse the forecast, and the app works without it | Encrypted in transit: **Yes** (HTTPS only). Retention: **the app does not store it server-side; the saved spot lives on the device.** |

Notes that must appear in the pack's `store/data-safety.md` so the declaration is auditable:

- **This is the only row.** No personal info, no financial info, no health, no messages, no photos,
  no audio, no files, no contacts, no calendar, no app activity, no web browsing, no device IDs —
  none is transmitted by the app.
- **`INTERNET` alone produces no row**, per Play's "collect" definition: the definition is about data,
  not about the capability.
- **A deliberate over-declaration is available** and is what a cautious auditor would choose: add
  "Precise location" instead of "Approximate", and/or a note that the coordinate is user-chosen rather
  than device-derived. Play's guidance on optional vs required is judgment-based (S7 §optional/required),
  so both readings are legal; the stricter row costs nothing but a sentence.
- **The alternative, if the ephemeral exemption is preferred:** keep the same row but mark it
  *ephemeral* in the form (it then will not appear in the store-listing Data safety section), and be
  ready to defend it against MET's own logging statement. This report recommends **not** taking it.
- **If a proxy server is ever added**, the declaration changes materially: the coordinate would pass
  through infrastructure the developer runs, which is a collection by the developer and raises the
  retention/deletion questions the current shape avoids.

### 3.3 Is a privacy policy required?

**Yes, and unconditionally.** MEASURED (S7):

> *"Even developers with apps that do not collect any user data must complete this form and provide a
> link to their privacy policy. In this case, the completed form and privacy policy can indicate that
> no user data is collected or shared."*

> *"Ensure that you've added a privacy policy; this is required to complete the Data safety form and
> have your data safety information shown to users."*

Two placements are required, and the factory already owns both:
`store/listing.json.privacyPolicyUrl` must be a real public HTTPS URL (published by
`scripts/publish-privacy.mjs`, never a placeholder — `.cursor/rules/privacy-policy.mdc`), **and** the
same policy must be openable **from inside the app** (`slot.inAppPolicy`,
`scripts/check-in-app-policy.mjs`).

**The policy must now describe the forecast call**, because the app transmits the chosen spot's
coordinates off the device: name `api.met.no`, the fact that the coordinate (and the requesting IP) is
sent, why, and that nothing is stored by the developer. A policy that says "we collect nothing" would
be **false** under Play's definition once the forecast ships.

---

## 4. Attribution strings that must ship

These are the exact strings for the **About / Credits** screen (and, where the licence says "clear and
visible", they must be readable **inside the app**, not only in a web page nobody opens).

| Source | Licence | Required credit (exact) |
|---|---|---|
| **MET Norway** (`api.met.no`), cloud-cover forecast | **CC BY 4.0** + NLOD 2.0 | MEASURED (S9): *"Credit should be given to The Norwegian Meteorological Institute, shortened MET Norway, as the source of data. Some suggestions: «Data from MET Norway», «Based on data from MET Norway». A link to the source where the data can be downloaded would also be appreciated."* → ship: **`Data from MET Norway (met.no) — CC BY 4.0`** with the links `https://www.met.no` / `https://api.met.no` and `https://creativecommons.org/licenses/by/4.0/`. **Never use the word "Yr"** (S8). |
| **ESO Milky Way panorama** (the `eso0932a` panorama by Serge Brunier) | CC BY 4.0, with a **mandatory, unaltered, clearly visible** credit | MEASURED (S10): *"the full image or footage credit must be presented in a clear and readable manner to all users, with the wording unaltered (for example: "ESO/José Francisco (josefrancisco.org)"). The credit should not be hidden or disassociated from the image footage."* → ship the credit **beside the image**, as **`ESO/S. Brunier`**, plus `https://www.eso.org/public/images/eso0932a/`. If the image is altered, *"you must clearly indicate if you have used AI to modify our content"* (S10). ESO's own note: *"ESO materials… may not be used to state or imply the endorsement by ESO… of a commercial product or service"* (S10). |
| **NASA imagery** (if any NASA-derived pixels ship) | Generally not copyrighted / public domain, with conditions | MEASURED (S11): *"NASA should be acknowledged as the source of the material."* and *"If the NASA material is to be used for commercial purposes, including advertisements, it must not explicitly or implicitly convey NASA's endorsement of commercial goods or services."* Also: the NASA insignia/logo/seal (the "meatball") **must not** be used — it is *"strictly prohibited"* to use NASA emblems in a way implying endorsement, and the insignia may not be reproduced. → ship e.g. **`NASA/JPL-Caltech`** (or the specific instrument credit for the image used), and **no NASA logo anywhere in the app or the listing**. Never write "official", "NASA-approved", or similar. |
| **IAU constellation figures** | CC BY 4.0 | MEASURED (S12): *"The charts below were produced in collaboration with Sky & Telescope magazine (Roger Sinnott and Rick Fienberg). Alan MacRobert's constellation patterns… The images are released under the Creative Commons Attribution 4.0 International license."* and the IAU Copyright page: full credit, clearly visible, reproduce **in full**, and **do not overlay the IAU logo**. → ship: **`Constellation figures: IAU and Sky & Telescope (Roger Sinnott, Rick Fienberg; patterns by Alan MacRobert) — CC BY 4.0`** with `https://www.iau.org` and the CC BY link. If the figures are redrawn/adapted, say so and credit the adaptation. |
| **Solar System Scope** (planet/moon/Sun textures) | CC BY 4.0 | MEASURED (S13), a working string used in the wild and consistent with the publisher's page: **`Planet, Moon and Sun surface maps: Solar System Scope, based on NASA elevation and imagery data. CC BY 4.0. https://www.solarsystemscope.com/textures/`** plus `https://creativecommons.org/licenses/by/4.0/`. Their own page discloses the modifications a re-user should pass on: *"Some parts of the planets remain to be mapped. These 'gaps' are filled with fictional terrain"*, *"The colors are slightly more saturated"* (S13). |
| **FK6** (fundamental star catalogue) | **CC0 1.0** — public domain dedication | MEASURED (S14): *"To the extent possible under law, the publisher has waived all copyright and related or neighboring rights to the FK6 data. For details, see the Creative Commons CC0 1.0 Public Domain dedication. Of course, you should still give proper credit when using this data as required by good scientific practice."* → CC0 requires **no** credit; ship one anyway as good practice: **`Star positions: The Sixth Fundamental Catalogue (FK6), GAVO Data Centre — CC0 1.0`**. |

**One screen, one list.** A single "About / Credits" destination carrying all six rows above (only the
rows that actually ship), each with its licence name and a live link, satisfies every one of these
licences at once — and is also what `docs/icon-provenance.md`-style provenance discipline expects for
any *data* asset. The ESO row is the one with the strictest wording (unaltered, visible, beside the
image), so the Milky Way image, wherever it appears in the scene or on the postcard, should carry
`ESO/S. Brunier` **on or next to it**, not only in the credits list.

---

## 5. What actually moves revenue here — ranked, and honest about what is unmeasured

### 5.0 The frame, from this slot's own plan

MEASURED (`docs/ideation/plans/lumintra-light-planner.economics.json`):
`archetype: purchase_only_no_ads`, `priceUsd: 9.99`, `net = 9.99 × 0.85 × 0.92 = $7.81`,
`64 sales/month` to clear the $500-net target, `killCriteria` = 150 installs / $1,000 net by day 120.

MEASURED (`.kiln/scratch/positioning-2026-10-08/E-money-repositioning.md` §4): the shelf's paid anchors
are **PhotoPills $10.99 / 100K+ / 4.8 (8,220)**, **Sun Surveyor $9.99 / 100K+ / 4.8 (5,140)**,
**Sun Position PRO $9.99 / 50K+ / 4.9 (2,730)** — standalone purchase-only listings, zero ads.
The same doc records the shelf as the only one with standalone paid anchors at six-figure installs.

**The structural fact that ranks everything below:** this app has **no free funnel**. It is bought or
it is not. So the only two quantities that exist are **impressions → install(≈purchase) conversion**
and **the volume of impressions the listing can earn**. Every lever below acts on one of those two.

### 5.1 The ranking

| Rank | Lever | Why it is where it is (evidence) | Factory status |
|---|---|---|---|
| **1** | **Store-listing conversion: icon, screenshots, and the first two lines** | MEASURED — https://play.google.com/console/about/store-listing-experiments/ §Best practices: *"Get the biggest impact by testing icons, videos, and screenshots"*; experiments report *"acquisition and 1-day retention rates for each version"* and Play recommends *"testing a single asset at a time"* for *"at least a week"*. For a purchase-only app with no free tier, the listing **is** the product's first screen and the only thing standing between an impression and $9.99. | **Partly covered.** The factory renders a slot-owned icon, feature graphic and a 6-shot phone set + tablet sets from `store/studio.json`, photographs **real** screens (`scripts/media-capture.mjs`) and measures the pixels (`scripts/visual-qa.py`). That is a *strong starting point*, not a measured one. **GAP: nothing ever A/B-tests an asset** — the first listing ships unexamined, and Play's experiments need traffic that only exists after launch. |
| **2** | **Rating and review volume** | MEASURED — the shelf's winners carry large review counts at 4.8 (PhotoPills 8,220; Sun Surveyor 5,140; Sun Position PRO 2,730 — positioning doc §1.4), and MEASURED in `docs/research/2026-10-08-light-and-sky-session-planner-market.md`: the shelf's **weakest** ratings are the clear-sky/astronomy-weather apps (Good To Stargaze 3.7; Clear Outside 3.5/3.8; Ouranos 3.9). A paid app with 12 reviews and 4.6 is a materially weaker purchase proposition than the same app with 800. | **GAP — and it is exactly the owner's request.** Today the slot has **no review surface at all**: nothing asks, and Settings has no rating row. §1 fixes that. **Also a gap: no crash/quality telemetry of our own** — Android vitals is Play-side (see rank 4), so the developer learns of crashes only after Play does. |
| **3** | **Positioning, price and the one-sentence wedge** | MEASURED — the light shelf's ceiling is $10.99 and $9.99 is its modal price (positioning §4); $17.99 exists but only as an **in-funnel Pro unlock of a free 5M+ app** and does not transfer to a standalone listing (positioning §2). The name/title band and the query set were measured with `scan-titles.mjs`. | **Covered.** `economics.json` + `differentiators.json` + `naming.json` are required before scaffold, and the store pack is rendered from the same identity. This is the factory's strongest-existing half. |
| **4** | **Technical quality (crash/ANR rate) → discoverability** | MEASURED — https://developer.android.com/topic/performance/vitals §bad behaviour thresholds: user-perceived **crash rate 1.09 %** overall / 8 % per phone model, **ANR rate 0.47 %** overall / 8 % per model, evaluated on a 28-day average; *"If your app or game exceeds a bad behavior threshold, Play may reduce the visibility of your title. Play may also show users a warning on your store listing."* (Direct fetch of that URL timed out twice in this session; the quoted text came from the search tool returning the page.) | **Covered by construction, not by a check.** Android vitals is collected by the OS/Play — no SDK, no Firebase, nothing to add. The slot's own defences are unit tests + the offline-first design (a flight-mode app cannot ANR on a dead network call). **GAP: the kill criterion "≥50 % of buyers generate a brief within 30 days" has no in-app measurement** (see rank 7). |
| **5** | **ASO: title, short description, keywords** | MEASURED (ON RECORD, repo) — `naming.json` records a signed-out Play search: the carrier query returned 29 cards and **no card is titled Lumintra**, the descriptor query `light planner` returned 24/30 on-job, and two incumbents already use `Light Planner`. | **Covered** by the naming contract + `scan-titles.mjs` + the Play title rules. The remaining ASO work (short-description wording, screenshot captions, localised variants) is written by `scripts/store/render.py` from the declarations. |
| **6** | **Localisation** | INFERRED from Play's own store-listing help: the title is a per-language field and *"You can add one localized name per language"*, and listing text/experiments are localisable (S5, store-listing experiments page: *"localized text"*). `en` + `ru` are the slot's declared locales. | **Covered by rule** (`.cursor/rules/localization.mdc`, born-multilingual) but **not measured for revenue** — no per-locale conversion reading exists. |
| **7** | **Update cadence / "freshness"** | **UNVERIFIED.** I found no Google page stating that release frequency is a discoverability factor. What *is* measured is that fixing vitals restores visibility (rank 4) and that Play auto-updates anyway (S4 opening sentence). The honest statement: cadence matters **through** quality and feature evidence, not as a ranking input anyone has documented. | **GAP by design** — a one-time-purchase app has no forced update loop, and adding one to "look fresh" is not an evidenced lever. |
| **8** | **A free/paid boundary** | **N/A.** `purchase_only_no_ads` has no free tier, and `economics.json` rejects `purchase_led_single_unlock` on an **unmeasured install→purchase conversion** (positioning §5). | Correctly absent. Do not invent one. |

### 5.2 What does **not** move revenue here (and must not be added)

- **An in-app product.** MEASURED — `economics.json`: `"capabilities": { "required": [],
  "deliberatelyNotEnabled": ["billing", "ads"] }` with the note *"purchase_only_no_ads needs no
  capability because the price is set in Play Console; billing is not enabled (no in-app store)"*.
  **There is nothing to sell in-app: the price is a Play Console listing attribute, not an IAP.** The
  spec must therefore **not** add `com.android.billingclient`, a `billing` capability, a paywall
  screen, or a `PurchaseAuthority`. Confirming this is important precisely because a "rate us"-shaped
  Settings screen is the kind of place an implementer invents an in-app purchase.
- **Ads.** Not enabled, and every paid row on the shelf declares none (positioning §1.4).
- **A subscription.** Banned factory-wide and absent from `ARCHETYPES`.
- **Incentivized or gated reviews.** Forbidden (S6, S1) — see §1.8. A review is worth exactly $0 in
  app currency.
- **Keyword stuffing in the title/description.** A named Play violation (positioning §6).

### 5.3 The top gap, stated plainly

**The app has no rating/review loop and no listing measurement.** Its conversion is a single
uninstrumented step from a Play impression to a $9.99 purchase, on a shelf whose winners are carried by
8,000-review, 4.8★ listings, and the factory ships the listing artwork but never tests a variant of it.
The owner's own request (§1) is therefore the highest-value single addition available — **provided it is
built the way Google requires** (automatic at an earned moment; Settings row opens the Play listing;
never a button-triggered `launchReviewFlow`, never sentiment-gated). The first variant to test after
launch is the icon and the first screenshot, per Play's own "biggest impact" guidance.

---

## 6. Claim ledger

| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| The review artifact is `com.google.android.play:review` (+`review-ktx`) | MEASURED | https://developer.android.com/guide/playcore/in-app-review/kotlin-java (updated 2026-10-01), §"Set up your development environment" |
| Current stable version is **2.0.2** for both | MEASURED | `https://dl.google.com/android/maven2/com/google/android/play/review/maven-metadata.xml` → `<latest>2.0.2</latest>`, `lastUpdated 20241018`; same for `review-ktx` |
| It is on Google's Maven repo | MEASURED | same page: *"This dependency is downloaded from the Google's Maven repository."*; the artifact downloaded from `dl.google.com` |
| Licence is the Play Core SDK ToS, not an OSS licence; derivative works forbidden | MEASURED | `guide/playcore` §"Play Core Software Development Kit Terms of Service"; and the `LICENSE` file inside `review-2.0.2.aar` (extracted in this session) |
| The old monolithic `play-core` is split per feature | MEASURED | `https://developer.android.com/guide/playcore` §"Migration from the Play Core Java and Kotlin Library" |
| API shape: `ReviewManagerFactory.create(context)`, `requestReviewFlow(): Task<ReviewInfo>`, `launchReviewFlow(activity, reviewInfo)` | MEASURED | the kotlin-java page, all three code blocks |
| The KTX extensions are `requestReview()` and `launchReview(activity, reviewInfo)`, both suspend | MEASURED | `javap`-free constant-pool read of `ReviewManagerKtxKt.class` extracted from `review-ktx-2.0.2.aar`: `requestReview (ReviewManager, Continuation) -> Object`, `launchReview (ReviewManager, Activity, ReviewInfo, Continuation) -> Object` |
| The review library declares **no permission** and no manifest component | MEASURED | the merged `AndroidManifest.xml` inside `review-2.0.2.aar` (and `review-ktx-2.0.2.aar`): `<uses-sdk minSdk 21/>`, empty `<application/>`, no `uses-permission` |
| A time-bound quota exists; exceeding it (e.g. a call within less than a month) may show no dialog; its value is secret and may change without notice | MEASURED | in-app-review page §Quotas, quoted in §1.4 C1 |
| A button must not trigger the API; a CTA should redirect to the Play Store instead | MEASURED | in-app-review page §Quotas, quoted in §1.4 C2 |
| The caller is told nothing — not even whether the dialog was shown; do not change app flow or inform the user | MEASURED | kotlin-java page §"Launch the in-app review flow" and its Important note |
| No question may precede or accompany the prompt ("Do you like the app?", "Would you rate 5 stars") | MEASURED | in-app-review page §"When to request an in-app review" |
| The card must be surfaced as-is: no tampering, no overlay, topmost, no programmatic removal | MEASURED | in-app-review page §"Design guidelines" |
| Review flow requires the Play Store app to be installed | MEASURED | in-app-review page §"Device requirements" |
| On a device without the Play Store the request fails as an unsuccessful task | INFERRED | from the device-requirements sentence + the error-code branch in the kotlin-java sample; the exact failure mode is not stated in the docs |
| The app should also offer a plain Play-listing link | MEASURED | in-app-review §Quotas (*"redirect the user to the Play Store instead"*) plus `distribute/marketing-tools/linking-to-google-play` (updated 2026-08-07) |
| The listing intent is `ACTION_VIEW` + `https://play.google.com/store/apps/details?id=<pkg>` + `setPackage("com.android.vending")` | MEASURED | linking-to-google-play §"Linking from an Android App" |
| In-app reviews put "user-entered data (rating and free-text review)" into Play Store processes governed by Play's ToS | MEASURED | in-app-review page §"Data Safety" table |
| Whether that requires a Data safety row is the developer's decision | MEASURED | in-app-review page: *"you are solely responsible for deciding how to respond to Google Play's data safety section form"* |
| Recommendation: no new Data safety row for the review flow | INFERRED | the data goes to the Play Store app's own review UI, not to a Lumintra server; the conservative alternative is stated in §1.5 |
| Incentivized/deceptive/forced ratings are policy violations | MEASURED | https://support.google.com/googleplay/android-developer/answer/9898684, §Full Policy + Key Considerations |
| In-app updates: API 21+, flexible/immediate, `.obb`-incompatible, Data-safety row for device metadata/app version/modules | MEASURED | https://developer.android.com/guide/playcore/in-app-updates (updated 2026-01-30) |
| `app-update` current version is 2.1.0 | MEASURED | Google Maven `app-update/maven-metadata.xml` |
| Recommendation: do not ship in-app updates for v1 | INFERRED | from (a) Play auto-updates already (S4 opening), (b) it adds a Data safety row, (c) a forced-update screen fights the pre-drive job |
| Null hypothesis not tested: the install-rate benefit of in-app updates on a paid planner | UNVERIFIED | needs a shipped A/B read in Play Console |
| "Collect" means transmitting data from the app off the user's device; libraries/SDKs count; destination (you or a third party) is irrelevant | MEASURED | https://support.google.com/googleplay/android-developer/answer/10787469 §"Data collection", quoted in §3.1 |
| **Sending a saved coordinate to `api.met.no` is therefore a collection** | MEASURED (definition) + INFERRED (application) | the transmission is stated in the plan (S8 requests carry lat/long) and the definition above |
| Ephemeral processing: included in the form response, not disclosed in the store listing, if the data is memory-only for the request; Play's own example is a weather app sending user location | MEASURED | the same page, §"Ephemeral processing", quoted in §3.2 |
| MET Norway **stores** the requesting IP and any geocoordinates in its access logs | MEASURED | https://api.met.no/doc/TermsOfService §"Personal data" |
| Recommendation: declare the row normally rather than claiming ephemerality | INFERRED | the two quotes above collide; §3.2 states both readings and the alternative |
| Privacy policy is required even for an app that collects nothing, and must be linked from the store listing | MEASURED | support.google.com/…/10787469 §"Which developers need to complete the Data safety form" |
| A policy saying "we collect nothing" would be false once the forecast ships | INFERRED | from the "collect = transmit" definition plus the forecast call |
| MET Norway data is CC BY 4.0 + NLOD 2.0; credit "Data from MET Norway" | MEASURED | https://api.met.no/doc/License, quoted in §4 |
| MET Norway requires an identifying User-Agent, forbids the word "Yr", requires ≤4-decimal coordinates, forbids background polling, HTTPS only, and prefers a proxy | MEASURED | api.met.no TermsOfService §§Identification, Trademarks, Traffic, Protocols, Direct client-to-API connections |
| ESO credit is mandatory, unaltered, clear and visible, not separated from the image; "ESO/S. Brunier" is the panorama credit | MEASURED | https://www.eso.org/public/outreach/copyright/ §Conditions and the Usage-rights Q&A |
| NASA must be acknowledged as the source, must not imply endorsement, and its emblems must not be used | MEASURED | NASA media guidelines (page read via search result; the direct fetch timed out) |
| IAU constellation charts are CC BY 4.0, credited to IAU + Sky & Telescope (Sinnott, Fienberg; MacRobert patterns), reproduced in full, no IAU logo | MEASURED | iau.org constellation pages + iau.org/copyright |
| Solar System Scope textures are CC BY 4.0 with the "based on NASA elevation and imagery data" string | MEASURED | solarsystemscope.com/textures + the Qt attribution page |
| FK6 is CC0 1.0 with credit recommended but not required | MEASURED | GAVO data-centre FK6 pages and table info |
| `Geocoder` requires no location permission | MEASURED | https://developer.android.com/reference/android/location/Geocoder — no `@RequiresPermission` on any geocoding overload; only *"This API may hit the network"* |
| `Geocoder` needs no permission but is network-dependent and API-branched: the async overload is API 33 against a `minSdk` of 26, and `isPresent()` is API 9; "may hit the network, and may block for excessive amounts of time" | MEASURED | https://developer.android.com/reference/android/location/Geocoder, read 2026-10-08 |
| `POST_NOTIFICATIONS` is a dangerous runtime permission introduced in Android 13 (API 33); upgrades may be pre-granted | MEASURED | developer.android.com/about/versions/13/behavior-changes-all §"Runtime permission for notifications"; the Compose notification-permission guide; `Manifest.permission` reference |
| The app needs no storage permission for the share/postcard handoff | ON RECORD | factory rule `.cursor/rules/app-depth.mdc` names `ACTION_SEND`/`ACTION_CREATE_DOCUMENT`/`FileProvider` as the `data_out` tokens; the slot's promise `share-the-session-brief` commits to `ACTION_SEND` (`docs/ideation/plans/lumintra-light-planner.differentiators.json:145`) |
| Core vitals affect Play visibility; thresholds 1.09 % crash / 0.47 % ANR overall, 8 % per model, 28-day average; a store-listing warning may appear | MEASURED | https://developer.android.com/topic/performance/vitals (the direct fetch timed out twice; the text was returned by the search tool quoting that URL) |
| Store listing experiments: biggest impact from icon/video/screenshots; one asset at a time; ≥1 week | MEASURED | https://play.google.com/console/about/store-listing-experiments/ §Best practices |
| The shelf's paid anchors and the $10.99 ceiling | MEASURED | `.kiln/scratch/positioning-2026-10-08/E-money-repositioning.md` §§1.4, 4 (values re-verified there by `enrich-market.mjs`) |
| The shelf's weakest ratings are the clear-sky/astronomy-weather apps | MEASURED | `docs/research/2026-10-08-light-and-sky-session-planner-market.md` (grep hit on rating rows) |
| The slot has no in-app product; the price is a Play Console attribute; billing capability is deliberately not enabled | MEASURED | `docs/ideation/plans/lumintra-light-planner.economics.json` `capabilities` block |
| The slot has **no** review surface today (nothing asks, no rating row in Settings) | INFERRED | the fixed screen list in this brief (Settings = units, language, rate us) is the plan, not the artifact — no slot exists yet; the check is `rg -i review apps/lumintra-light-planner/app/src/main` after scaffold |
| Update cadence is a discoverability factor | UNVERIFIED | no Google page found stating it; the check is a Play Console Help search for a "freshness" ranking statement |
| The licence of the orbital-elements feed the app plans to fetch | UNVERIFIED | the feed is not named in the economics plan's verified set; the check is to name the feed and read its terms before the spec freezes |
| `LocalActivity` availability in the slot's `activity-compose` version (for obtaining the Activity in Compose) | UNVERIFIED | the check is the resolved `androidx.activity:activity-compose` version in the slot's version catalog, or the compiled alternative `LocalContext.current as ComponentActivity` |
| Whether the in-app review card appears for an app in internal testing / installed from a non-Play source | UNVERIFIED | not stated on either Android page; the check is a device run on `R58MB3SGZVL` after the first closed-track release |

---

## 7. One-paragraph answer for the spec

Lumintra ships **`com.google.android.play:review:2.0.2`** (plus `review-ktx:2.0.2`) from Google's Maven
repo under the Play Core SDK ToS, called as `requestReview()` → `launchReview(activity, info)` **once, at
an earned moment, from the hosting Activity**, with the result ignored and the app's flow unchanged —
and the **Settings "Rate us" row opens the Play listing** through
`ACTION_VIEW` + `setPackage("com.android.vending")`, never the API. It declares **`INTERNET` only**
(`POST_NOTIFICATIONS` if a reminder ships, `ACCESS_COARSE_LOCATION` only if an optional auto-locate
convenience is added — the manual-coordinate path needs no location permission at all), transmits the
user's chosen coordinate and IP to `api.met.no`, and therefore declares **Location → Approximate
location, collected, app functionality, optional, encrypted in transit** in the Data safety form with a
privacy policy that says so. It carries no in-app product, no billing dependency, no ads and no
subscription, and its highest-value unfinished lever is the rating/review loop the owner asked for,
built Google's way.
