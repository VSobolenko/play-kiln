# Play Console data safety answers — Lumintra: Light Planner

These answers must match the shipped app, the merged manifest, every bundled SDK and the public
privacy policy (`store/privacy.md`). They are written from the app's own design
(`openspec/changes/app-lumintra-light-planner/specs/cloud-forecast/spec.md`,
`…/annex/M-play-integrations-compliance.md` §3–§4) and the shipped `AndroidManifest.xml`.

## Does the app collect or share any required user data?

**Yes** — exactly one row.

## The one collected row

| Field | Answer |
|---|---|
| Category | **Location** |
| Sub-category | **Approximate location** |
| Collected? | **Yes** — a saved spot's coordinate is transmitted off the device |
| Shared? | **No** — nothing is shared; a transmission to a data source the user's app calls is not "sharing" in Play's form |
| Purpose | **App functionality** |
| Optional? | **Yes** — the forecast is optional and the user can turn it off; the whole local product still works |
| Processed ephemerally? | **No** — the coordinate is not retained beyond the request |
| Encrypted in transit? | **Yes** — the request is HTTPS only |

### Why this row exists, exactly

A saved **spot's coordinate** is sent to the **MET Norway Locationforecast** service at
**`api.met.no`** to fetch the hourly **total cloud cover** for the session window. Play's definition of
*collect* is data **transmitted off the user's device**, *irrespective of destination and irrespective
of whether it reaches the developer*, so this is a collection and it is declared — even though no
server we run ever sees it, and no account is involved.

What leaves the device on this call, and nothing more:

- the saved spot's latitude and longitude, **truncated to 4 decimal places** (about 11 m),
- the spot's altitude in whole metres when the spot carries one,
- the request time, and
- an identifying `User-Agent` — `Lumintra/<versionName> (v.sobolenko.vs@gmail.com)`.

It is sent **only while the user is viewing that spot's forecast** — never in the background, never at
launch, never from the widget or the reminder. See `specs/cloud-forecast/spec.md` (requirements "The
request is conditional, cached per spot, and made only while the app is in use" and "The user is told a
coordinate leaves the device, and the forecast is optional").

## Permissions that are not a data-safety "collection" row

### `POST_NOTIFICATIONS` — opt-in local dark-window reminder

The app declares **`POST_NOTIFICATIONS`** (API 33+) solely for one **opt-in** local reminder that fires
when the astronomical dark window starts for a saved spot. The reminder is scheduled and delivered
**on the device**; it is not sold, not shared with any third party, and not used for advertising or
analytics. Denying the permission disables only that reminder — every other part of the product still
works. Play's data-safety form does not treat a local notification as a collected data type, so there
is no Notifications / Device-or-other-IDs collection row; the permission is documented here because
the factory's `slot.manifest` gate requires every gated permission to be named in this file.

### `INTERNET`

Required only for the two keyless HTTPS calls named above (`api.met.no` and CelesTrak). Not a
data-safety collection category by itself.

## What is deliberately NOT declared, and why

- **The CelesTrak element-set query carries no coordinates.** It requests orbital element sets for a
  named satellite (by NORAD catalogue number) and sends only the app's identifying `User-Agent`. No
  coordinate, no location and no user data leaves on that call, so it is not a Location row and not a
  data type of its own.
- **Share and export are user-initiated.** The text brief, the postcard and the export/import of spots
  and horizons go only where the user chooses (the Android share sheet, or a file the user saves). The
  app sends them nowhere by itself and runs no server, so they are not a collection or a share.
- **Everything else is local.** The sky scene, the drawn horizon, the twilight and dark-window
  geometry, the Moon, the ~250-object target catalogue, the satellite-pass propagation, the optics
  frame, the widget and the reminder all compute on the device and transmit nothing.
- **No photo, media, storage, contacts, files, personal-info, financial-info or health row exists.**
  There is no account, so no personal info is collected; there is no advertising SDK and no analytics
  SDK, so no advertising-ID or diagnostics row exists. Monetization is purchase-only (no ads); any
  Play Billing financial row, if introduced later, must be declared when that SDK is added — the
  current pack declares none.

## Third-party SDKs

None that collect or transmit data. The app declares no advertising SDK, no analytics SDK, no crash
reporter, no authentication SDK and no billing library. The only network destinations are the two
keyless public services named above (`api.met.no` and CelesTrak); both are reached with the app's own
HTTPS client and neither is a bundled SDK with a data-collection policy of its own.

## Security practices

- Data encrypted in transit: **Yes** — every request is HTTPS.
- Users can request deletion: **Not applicable** — there are no accounts. A user deletes all app data
  by clearing the app's storage (Android Settings → Apps → Lumintra → Storage → Clear data) or by
  uninstalling; nothing is held on any server of ours to delete.

## The stricter reading, if the auditor prefers it

The declared row is **Approximate location**. The coordinate the app sends is truncated to four decimal
places (about **11 m**) before the URL is built, which is the precision at which the request is made
and the reason the row is declared as *Approximate* rather than *Precise*. An over-declaration to
**Precise location** is available and is not wrong in the conservative direction: Play's own guidance
treats a coordinate the user can pick at map scale as potentially precise, and the app's coordinate
picker lets the user place a pin at finer than 11 m. If the auditor prefers the stricter reading, move
this single row from **Approximate** to **Precise** with the same purpose (**App functionality**),
optional flag (**optional**) and encryption answer; no other row changes.

## Agent note

The store pack must never say the app collects nothing: `store/listing.json.dataSafety.collectsData` is
`true`, this document names the one row, and `store/privacy.md` names `api.met.no` and MET Norway as a
recipient. Run `.agents/skills/android-official/play-play-policy-insights/` before marking the slot
ready, and read the **merged** manifest of the built artifact — a library can contribute a permission
the source manifest does not show (`store/checklist.md` names the two that must appear and the location
permissions that must not).
