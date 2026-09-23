# Google Play automation surface and MCP landscape — 2026-09-23

Purpose: establish, with citations, exactly what PlayKiln can automate on Google
Play, what stays manual, and which MCP options actually exist. Read together with
[`2026-09-22-android-play-requirements.md`](./2026-09-22-android-play-requirements.md).

## Verdict

1. **Google ships no official MCP server for Google Play.** Its official MCP
   catalogue covers Android Management API, Maps, Pay & Wallet, Workspace, and
   others — Play is absent.
2. **Play MCP servers that exist are community-maintained.** Four are published in
   public registries; see §3.
3. **Our own `scripts/play-upload.mjs` already covers the critical release path**
   with no third-party dependency. That stays the source of truth.
4. **More can be automated than the factory currently does**: the Data safety
   declaration, the full monetization catalogue, the R8 mapping file, and internal
   app sharing are all real endpoints.
5. **The hard wall is unchanged**: no API can create a new Play app or fill the
   legal consents. A human does that once per slot.

## 1. The hard boundary (verified)

From the official Edits documentation:

> You can only use this API to make changes to an existing app (that has at least
> one APK uploaded); thus, you will have to upload at least one APK through the
> Play Console before you can use this API. Furthermore, you cannot use this API to
> change an app's state from "published" to "unpublished", or to fill out the legal
> consents required for publishing. To publish the app, you have to use the Play
> Console.

Practically, per slot, a human must:

- create the app and reserve the package name,
- upload the **first** AAB manually,
- complete the legal consents and the content rating questionnaire,
- press publish.

Everything after that first upload is automatable. A draft app additionally accepts
**only** draft releases through the API until the first manual release exists.

Sources:
- <https://developers.google.com/android-publisher/edits>
- <https://developers.google.com/android-publisher/tracks>

## 2. What the Play Developer API v3 actually exposes

The official REST reference lists these resource groups (plus `applications.dataSafety`,
which is documented on its own page and missing from the index):

| Area | Resource groups | Factory use |
|---|---|---|
| Release | `edits`, `edits.apks`, `edits.bundles`, `edits.tracks`, `edits.countryavailability` | already used by `play-upload.mjs` |
| Store listing | `edits.listings`, `edits.images`, `edits.details` | already used for text and graphics |
| Testing | `edits.testers`, `internalappsharingartifacts` | testers per track; instant install links without a track |
| Publishing hygiene | `edits.deobfuscationfiles` | upload the R8 mapping file so crash reports deobfuscate |
| Monetization | `monetization.onetimeproducts` (+`.purchaseOptions`, `.offers`), `monetization.subscriptions` (+`.basePlans`, `.offers`), `inappproducts` (legacy) | create the product catalogue from code |
| Purchase verification | `purchases.productsv2`, `purchases.subscriptionsv2`, `purchases.voidedpurchases`, `orders`, `externaltransactions` | server-side entitlement checks |
| Feedback | `reviews` (`list`, `get`, `reply`) | read and answer reviews |
| Signing | `appsigning`, `generatedapks`, `systemapks.variants` | inspect signing state; fetch Play-generated APKs |
| Account | `users`, `grants` | manage Console users and permissions |
| App config | `applications`, `applications.deviceTierConfigs`, `apprecovery`, `appstorecatalog.recentAppViews`, `recentUpdateEvents` | device tiers, app recovery, appearance analytics |

Sources:
- <https://developers.google.com/android-publisher/api-ref/rest>
- <https://developers.google.com/android-publisher/api-ref/rest/v3/edits.details>

### 2.1 `edits.details` is narrower than expected

The resource exposes only four fields: `defaultLanguage`, `contactWebsite`,
`contactEmail`, `contactPhone` (doc updated 2026-01-14). **There is no
`privacyPolicy` field**, so the privacy-policy URL in the store listing cannot be
set through this endpoint. Treat it as a Console step.

Source: <https://developers.google.com/android-publisher/api-ref/rest/v3/edits.details>

### 2.2 Data safety *can* be automated

```http
POST https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{packageName}/dataSafety
{ "safetyLabels": "<contents of the Data safety CSV>" }
```

The body is the raw CSV that Play Console exports, not a JSON model, so the
factory's job is to generate that CSV from the slot's real SDK surface. This is the
one Play Console "App content" form with a documented write API.

Sources:
- <https://developers.google.com/android-publisher/api-ref/rest/v3/applications/dataSafety>
- <https://support.google.com/googleplay/android-developer/answer/10787469>

## 3. MCP landscape (verified in registries)

**No official Google Play MCP server.** Google's official catalogue lists Android
Management API, Design MCP, Developer Knowledge API, Google Home Developer, Maps,
Pay & Wallet, Stitch, and Workspace — Play is not among them, and the official
announcement of MCP support never included it.

Sources:
- <https://docs.cloud.google.com/mcp/supported-products>
- <https://cloud.google.com/blog/products/ai-machine-learning/announcing-official-mcp-support-for-google-services>

Community options, with the version actually published in the registry:

| Package | Registry | Latest | Notes |
|---|---|---|---|
| `google-play-developer-mcp` | npm | **0.1.3** | README advertises "v1.0.0 / 150 tools" — the published version does not match, so treat capability claims as unverified |
| `@w3wide/play-console-mcp` | npm | **2.0.0** | TypeScript, MCP SDK; uses `playdeveloperreporting:v1alpha1` while Google's current surface is v1beta1 |
| `playstore-mcp` | npm | **1.1.1** | Wraps Android Publisher: app details, tracks, reviews, listings, products, testers |
| `google-play-mcp` | PyPI | **0.6.0** (MIT) | Release-lifecycle oriented: artifacts, testers, rollouts, Android Vitals |

**Recommendation: do not put a community Play MCP on the critical release path.**
They add a supply-chain dependency, require the same service-account credentials we
already handle, and duplicate logic we have verified. Use them as an optional
read-only convenience only, and never let an agent publish through an unaudited
third-party server.

## 4. What this means for the factory

Tiered, cheapest first:

1. **Extend `play-upload.mjs`** (no new dependency) with:
   - `applications.dataSafety` upload from a generated CSV,
   - `edits.deobfuscationfiles` upload of the R8 mapping file,
   - `edits.details` for contact website/email/phone and default language,
   - an internal-app-sharing artifact for instant human testing.
2. **Catalog automation**: generate one-time products and subscriptions from
   `capabilities/billing` configuration. New work must use
   `monetization.onetimeproducts` and `monetization.subscriptions`; the legacy
   `inappproducts` endpoint must not be used for new subscriptions.
3. **Feedback loop**: read Android Vitals and reviews to feed `/diagnose`.
4. **First-party MCP** (optional): wrap our own verified script and the Play API in
   a small local MCP server so agents get Play tools without third-party code.

### Sources for the monetization model

- <https://developer.android.com/google/play/billing/manage-catalog>
- <https://developers.google.com/android-publisher/api-ref/rest/v3/monetization.onetimeproducts/patch>
- <https://developers.google.com/android-publisher/api-ref/rest/v3/monetization.subscriptions/create>
- <https://android-developers.googleblog.com/2023/06/changes-to-google-play-developer-api-june-2023.html>

## 5. Correction: the "App Store Review API" is not for us

`androidpublisher/v3/appstore/{appStorePackageName}/apps/{packageName}` accepts a
`policyDeclarations` array covering Privacy Policy, Target audience and content,
Advertising ID, Ads, Sign in details, Health, Finance and more. It is tempting to
read this as automation for a normal developer's "App content" page.

It is not. The official guide states the API is for **third-party app stores
registered on Google Play through the Third-party app store on Play program**, to
submit apps hosted on *their* store. A normal PlayKiln slot cannot use it, so the
App content declarations remain a human Console step (apart from Data safety, §2.2).

Sources:
- <https://developers.google.com/android-publisher/app-store-review>
- <https://developers.google.com/android-publisher/app-store-review/policy-declarations>

## 6. Reporting API, for reference

Android Vitals live in a separate service, the Play Developer Reporting API
(`v1beta1`), reached on its own host rather than under `androidpublisher`.
Community servers that claim "full Reporting API coverage" but target `v1alpha1`
are behind the current surface.

Source: <https://developers.google.com/android-publisher/api-ref/rest>
