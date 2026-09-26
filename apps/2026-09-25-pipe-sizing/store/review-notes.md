# Reviewer notes — Gas Pipe Sizing Workbook

No account, login, invitation, or demo credentials are required. The app is free to use and
every screen in the review journey below is reachable without paying; the Pro unlock is an
optional one-time purchase and nothing a reviewer needs to see is behind it. There is no
subscription, no free trial, and no consumable.

## Review journey

1. Launch the app. It opens on the Calculator tab; "Cases" and "Guide" are the other two tabs.
2. Enter a material or system, a pressure class, a load, and a developed length, then a fitting
   quantity and the equivalent length per fitting that your own code book gives you.
3. Choose whether the capacity-like reading is user-supplied or comes from a code citation, and if
   cited, record the edition, table, and row you read it from.
4. Press "Confirm this capacity for the selected method", then "Evaluate sizing case" to review the
   arithmetic, the comparison against the required load, the recorded citation, and the
   calculation-aid disclaimer.
5. Evaluate with a required input missing to see the refusal. The shortest path: switch the method
   to "Capacity + physical inputs" and evaluate without recording a gas specific gravity. The app
   names the missing reading instead of showing a partial number, and shows no result at all.
6. Scrolling the calculator shows all three monetization surfaces:
   - **Interstitial**: on the free tier, one may appear after every **third completed evaluation**
     of a session. It never appears on launch, never after a refusal, and never for a Pro owner.
   - **Rewarded video**: "Share report" on the free tier offers a rewarded video. Watching one
     unlocks report sharing **for the rest of the session**; declining closes the offer and leaves
     the app fully usable.
   - **Paywall**: "Unlock Pro" opens a sheet listing the benefits, one purchase button and a
     "Restore a purchase" action. It prints no price of its own; Google Play shows the exact price
     and currency before payment.

The app performs arithmetic and bookkeeping only. It supplies no code-table data and does not
determine compliance or certify work. It restores the current case after the process ends by
reading it back from local storage, and keeps no history of previous cases.

## Permissions, data, and media

Two permissions are declared:

- `android.permission.INTERNET` — used by the ads and billing SDKs to reach Google's servers.
- `com.google.android.gms.permission.AD_ID` — declared by the Google Mobile Ads SDK to serve and
  measure ads.

Ads are served by Google AdMob and are requested **only after the ad-choices form has resolved**.
If consent is refused, no ad is requested and the app remains fully usable without ads. The data
types the app and its bundled SDKs collect or share are enumerated in `store/data-safety.md`:
App activity and Device or other IDs for advertising, App info and performance for SDK analytics,
and Financial info (the product IDs purchased and their entitlement state) for app functionality.
Payment instruments are handled entirely by Google Play and never reach the app. The case the user
builds is stored on the device and is never uploaded; there is no account and no server, so no
account deletion mechanism applies. Clearing app storage or uninstalling removes all local data.

The phone screenshots are real captures driven by `store/journey.json` on a phone AVD, and each
one is asserted against the live view hierarchy before and after the shutter. `scripts/media-capture.mjs`
refuses to publish them unless the installed APK is byte-identical to the release artifact recorded in
`dist/release-manifest.json`, and records that binding, including the sha256 of each composed asset, in
`store/provenance.json`. The tablet screenshots are that same phone capture framed at tablet dimensions
rather than captures taken on a tablet device, so the large-screen eligibility they support should be
re-confirmed on real large-screen hardware before upload.

## Capability compliance

- Enabled: ads, billing
- Consent required: **Yes** — the EEA/UK ad-choices form (Google User Messaging Platform)
- Purchase authority: Not required — no SERVER_COST consumables are declared.

## In-app product required before review

One non-consumable product, id `pro_unlock`, must exist and be active in the Play Console for
`com.gexetr.pipesizing` before the app is reviewed: a purchase flow that references an unknown
product fails in the reviewer's session. It grants no consumable credit and is not a subscription.

## Human checks required before upload

- TODO-HUMAN: Replace `support@example.com` with a monitored support address if it is still a placeholder.
- TODO-HUMAN: Host `store/privacy.md` publicly and replace `https://example.com/privacy` with its final HTTPS URL.
- TODO-HUMAN: Confirm the data-safety answers and permission list against the release AAB and all bundled SDKs.
- TODO-HUMAN: Replace the phone-derived tablet screenshots with captures from a real large-screen device.
- TODO-HUMAN: Create the `pro_unlock` in-app product in Play Console, then complete the legal declarations and first-app setup.

Version reviewed: `0.2.0` (code `2`)
