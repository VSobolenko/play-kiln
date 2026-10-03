# Reviewer access and notes — Lawn Round Log

## No account is needed

The app creates no account, has no sign-in and asks for no runtime permission: the app's own manifest
declares no `uses-permission` element at all, so no permission prompt ever appears. The **merged
release manifest** of the built artifact is not permission-free — `aapt2 dump permissions` prints
four — and `store/data-safety.md` names each of them with the library that contributes it. A reviewer
installs it and is presented with an empty round; every screen is reachable without credentials.

## How to exercise the app

1. **Install and open.** The app starts on the round's day list with an empty state that asks for the
   first customer. No login, no permissions prompt, no network prompt.
2. **Add customers.** Add a few customers by name. Nothing asks for an appointment, a time or a date.
3. **Reorder the round.** Move a customer up or down; the order is the order you drive.
4. **Record visits.** Tap the mowed, skipped or rained-off control on a row. Each control announces its
   own name — the outcome and the customer's — beside a state written as a word, and every target is
   at least 48 dp.
5. **Push the round.** From a day with customers due, use the push-the-day action. Every customer due
   that day moves forward exactly one day — each one once.
6. **See who owes what.** Mowed visits build a per-customer balance; a skipped or rained-off visit adds
   nothing. Record a payment and watch the balance fall; remove it and it returns.
7. **Export.** Export the stored record to CSV through the system file picker. The app reports how many
   rows it wrote; Save writes only where the reviewer chose, and Share stages its own copy in the
   app's private cache before handing it on.
8. **Restart.** Close the app and reopen it. The round, the visits and every balance are identical.

## The one purchase

The free tier is **eight customers**, fully usable with no time limit and no trial clock, and it is
complete: every visit the operator logs and every balance it derives is his to keep, edit and delete
for as long as he likes. The single in-app product is a **non-consumable unlock** that adds exactly
two things — unlimited customers and the CSV export — and withholds nothing that was already free.
It is offered when the ninth customer is added and from the export action, states one price, and
offers no trial, renewal or subscription. It can be restored after a reinstall. There is no second
paywall and no advertising anywhere in the app.

**Console requirement:** the product must exist before review. Create the single non-consumable
in-app product in `Монетизация с помощью Play → Встроенные продукты` with an ID that matches the
app's own billing configuration, exactly **one** purchase option, then press `Активировать` — a
product left as `Сохранить как проект` is never returned to the app, and the reviewer sees a price
sheet that fails. The price is entered in the console's own form (the RU console takes a comma).

## App access declaration

This slot sells an unlock, so the console's App access question cannot take the "no restricted
sections" branch: that branch asserts no payment is required, which is false here. The declaration
therefore takes the `Платежи, например за контент для однократных покупок…` line, and the reviewer's
access to the paid tier is granted with a **promo code** in the credentials dialog's
`Другие данные, которые требуются для входа` field. The code itself is deliberately **not**
reproduced in this file: it is access handed out, not text the pack publishes, and it belongs in the
console declaration.

## Not finished yet — honest list

- **The privacy policy is published, and the published text is the pack's own.** `store/privacy.md`
  is rendered to `https://telegra.ph/Lawn-Round-Log--privacy-policy-10-02` by
  `scripts/publish-privacy.mjs`, which also writes the URL into `store/listing.json` and refuses to
  record it unless the page answers HTTP 200 from outside. Re-running the script after an edit to
  `store/privacy.md` updates that same URL rather than accumulating a new one.
- **The same URL is declared inside the app.** `R.string.about_privacy_url` (`translatable="false"`)
  carries it and `AboutScreen` opens it through `LocalUriHandler` — the in-app half Play requires, and
  the pair `scripts/check-in-app-policy.mjs` measures.
- **The permission reading is about the merged manifest, not the slot's own file.** The slot's
  `app/src/main/AndroidManifest.xml` declares no `uses-permission` element, but the artifact that
  ships is built from the **merged** manifest, and it carries four: `com.android.vending.BILLING`,
  the AndroidX-generated receiver permission, `android.permission.INTERNET` and
  `android.permission.ACCESS_NETWORK_STATE`. The two network permissions arrive with
  `com.google.android.datatransport`, a transitive dependency of `billing-ktx` 9.1.0, which uploads
  the purchase event through the Play Store. `store/data-safety.md` and `store/privacy.md` both state
  this in the same words, so the pack does not claim a permission-free artifact. A future revision
  that adds a transmitting SDK must update those two documents and the Data safety form together;
  the caution is written here rather than in the policy, because the policy is published verbatim and
  an internal note about factory bookkeeping does not belong on a page a reviewer reads.
- **The in-app product does not exist in Play Console yet.** See the console requirement above; the
  reviewer's price sheet depends on it.
- **The composed pack exists, and every frame in it is a device capture.** `store/assets/**` carries
  eighteen composed assets — the icon and the feature graphic in each of the two locations (the pack
  root and `en-US/`), six phone frames and eight tablet frames —
  built from six phone captures taken on the device against the signed release build, and
  `store/provenance.json` records the sha256 of each one.
- **The launcher icon and the store identity are declared elsewhere.** `store/studio.json` is authored
  by the visual-identity work, not by this document; the values here are the listing copy only.
- **`store/data-safety.md` describes one collected row (Financial info → Purchase history)**, from the
  `billing` capability's own declaration. If a future revision adds any SDK that transmits, the Data
  safety form must be updated with it; under-declaring a bundled SDK is a policy defect, not an
  oversight.
- **The tablet asset sets are composed from the six phone captures, not captured on a tablet.** All
  eighteen composed assets come from one device run: a physical Samsung Galaxy A40 (SM-A405FM),
  1080x2340, Android 11, against the signed release build `0.1.0 (1)`, commit `5a95279a815d`. No
  tablet device is attached to this factory, so the 7-inch and 10-inch sets re-frame the same six
  phone screenshots. Play treats a large-screen asset as a claim about the large-screen experience,
  and this pack does not make one: the frames show the phone layout, and no tablet layout was
  photographed or asserted. `store/journey.json` records the same limit in its preconditions.
- **The listing declares one language while the app ships two.** `store/listing.json` declares
  `en-US` only; the app itself ships an English and a Russian UI, and `store/release-notes.md`
  carries notes in both. The pack's asset sets are composed by `scripts/store-studio.py`, which
  draws its own single-language `headlines` and `kickers`, so a second declared listing locale
  cannot be given a valid asset set today; a single-language listing over a multi-language app is
  legitimate and needs no fix. The Russian listing copy that was drafted for this slot remains in
  git history rather than in the pack.
