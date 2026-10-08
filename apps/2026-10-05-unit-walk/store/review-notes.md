# Reviewer notes — UnitWalk

UnitWalk is an offline rental unit walk for a self-managing landlord: room-by-room condition
photos, crash-safe drafts, move-in versus move-out compare per unit, and a signed PDF shared from
the phone with no account and no vendor cloud.

## What to look at

1. **Start a walk with no account and no network.** Put the device in airplane mode first; the unit
   list, a walk, camera capture and the export must all work.
2. **Survive a kill mid-walk.** Photograph two or three rooms, force-stop the app from Android
   settings, reopen it: every completed room's photo and note must still be there.
3. **Compare move-in with move-out.** Open a linked move-out walk beside its move-in walk and check
   that the per-room differences are recorded.
4. **Export the report.** Share it with the system share sheet and save it with the file picker
   (SAF). The PDF must open and carry the unit, the rooms and the date.
5. **Check the unlock boundary.** Free is **one unit, fully usable** — unlimited walks on it, photos,
   the room-by-room move-in vs move-out comparison, and **PDF export, which carries a UnitWalk
   watermark**. The one-time **$9.99** unlock adds **unlimited units** and **watermark-free PDF
   export**. Nothing is a subscription and nothing renews. The free walk must be fully usable, not a
   demo: exporting is not locked, it is watermarked.

## Billing

- The archetype is `purchase_led_single_unlock` (`economics.json`); `capabilities.json` enables
  `billing` and only `billing`.
- **The product id is `unitwalk_unlock`.** Create it in Play Console → Monetize with Play → Products
  → In-app products under **that exact id**: a **one-time product** (never a subscription — the
  factory ships none), **active** (an option left in Draft is not returned by the Play Billing
  Library, so the app behaves as if the product does not exist), at the **$9.99** `economics.json`
  declares. It is a **non-consumable** permanent unlock, so never consume it — a consumed product
  cannot be re-bought and the buyer loses the perk. The app declares the id once, at
  `app/src/main/java/com/gexetr/unitwalk/ui/unlock/UnlockProducts.kt:20`
  (`val UNIT_WALK_UNLOCK: ProductId = ProductId("unitwalk_unlock")`); that line, not this document, is
  the source of truth, and a console id that differs from it is the defect.
- The test account must be added under Play Console → Settings → **License testing**; an account
  added only as an internal tester on a track is not a license tester, and its purchase is charged
  **real money**.
- Product authority: the unlock is a plain non-consumable, so no server-side `PurchaseAuthority` is
  required (`store/listing.json` → `capabilityCompliance.requiresPurchaseAuthority: false`).
- The unlock needs `com.gexetr.capability:billing:1.0.0` plus the Fragment constraint
  (`androidx.fragment:fragment` ≥ 1.9.1) that keeps the Billing → GMS-basement transitive off the
  Play-flagged 1.1.0.

## Permissions and data safety

- `CAMERA` is the app's only own runtime permission: used only for a walk photo the user takes. The
  Photo Picker needs none, and the report leaves the app through a `content://` URI, so there is no
  storage permission.
- `android.hardware.camera.any` is `required="false"` so tablets without a camera still get the app
  and can import from the gallery.
- Data safety declares Financial info → Purchase history only because of the bundled Play Billing
  SDK; the app's own code collects nothing and never touches the network. The Billing contribution
  (`INTERNET` / `ACCESS_NETWORK_STATE`, internal datatransport) is stated in both `store/privacy.md`
  and `store/data-safety.md` and must be re-measured from the merged manifest of the release AAB.

## Store assets

- The six phone screenshots are captured on the target phone (`SM-A405FM`, serial `R58MB3SGZVL`) by
  `scripts/media-capture.mjs` replaying `store/journey.json`, which asserts what each frame must
  show. The 7-inch and 10-inch sets are composed from those same six captures because no tablet
  device is attached; they are a phone-framed composition rather than a tablet capture, and the pack
  claims nothing more than that (`store/graphics.md`).
- The comparison frame (`store/assets/<locale>/phone-05.png`) photographs the app's own
  **no-baseline state**, and that is disclosed rather than hidden: the journey records exactly one
  walk, so the room card's prior side reads **No prior report** and the surface prints "no baseline
  has been invented". That is what a first finished walk really looks like, and the frame's headline
  and body were reworded on 2026-10-06 to state exactly that. A frame showing two recorded
  conditions would need a second finished walk for the same unit, which this journey does not
  create — see `store/journey.json` →
  `_why_the_comparison_frame_shows_the_no_baseline_state`. Note also that the comparison's column
  labels are the fixed strings `compare_column_prior` / `compare_column_current` (`Move-in` /
  `Move-out`) in `app/src/main/res/values/strings_compare.xml`, so a comparison opened from a
  move-in walk labels the current walk's column `Move-out`; that is the shipped behaviour of the
  frame a reviewer sees.

## Human checks required before upload

- TODO-HUMAN: Confirm the support email `v.sobolenko.vs@gmail.com` is monitored; Play publishes it on
  the listing.
- TODO-HUMAN: Open the live policy `https://telegra.ph/UnitWalk--privacy-policy-10-05` (the URL
  `store/listing.json` records) and confirm it still describes the shipped app. It is published from
  `store/privacy.md`; no reserved-host placeholder is involved.
- TODO-HUMAN: Confirm every claim in `store/listing.json` and the six screenshot captions against
  the release build. All five promises are `shipped`, none `deferred` (`node
  scripts/check-differentiators.mjs --slot unit-walk`): the crash-safe draft is proved by
  `WalkViewModelTest.aResumedDraftRestoresTheSamePositionNoteAndPhotosFromDisk`, and the PDF handoff
  by the `ACTION_SEND` / `ACTION_CREATE_DOCUMENT` path over a `FileProvider` URI in
  `ReportViewModel`.
- TODO-HUMAN: Confirm the data-safety answers and permission list against the release AAB and its
  merged manifest (the Billing contribution cannot be read from source alone).
- TODO-HUMAN: Inspect every captured screen for personal or test data before uploading.
- TODO-HUMAN: Confirm the release notes and the listing short/full descriptions use finished-product
  tone and invite Play reviews, never early/WIP/roadmap language (`.cursor/rules/store-copy.mdc`).
- TODO-HUMAN: Complete the Play Console legal declarations and first-app setup.

Version reviewed: `0.1.0` (code `1`), matching `store/release-notes.md`'s release name `0.1.0 (1)`.
