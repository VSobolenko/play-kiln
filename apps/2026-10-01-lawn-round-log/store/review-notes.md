# Reviewer access and notes — Lawn Round Log

## No account is needed

The app creates no account, has no sign-in and requests no permission. A reviewer installs it and is
presented with an empty round; every screen is reachable without credentials.

## How to exercise the app

1. **Install and open.** The app starts on the round's day list with an empty state that asks for the
   first customer. No login, no permissions prompt, no network prompt.
2. **Add customers.** Add a few customers by name. Nothing asks for an appointment, a time or a date.
3. **Reorder the round.** Move a customer up or down; the order is the order you drive.
4. **Record visits.** Tap the mowed, skipped or rained-off control on a row. Each control has a visible
   label, a state written as a word, and a target of at least 48 dp.
5. **Push the round.** From a day with customers due, use the push-the-day action. Every customer due
   that day moves forward exactly one day — each one once.
6. **See who owes what.** Mowed visits build a per-customer balance; a skipped or rained-off visit adds
   nothing. Record a payment and watch the balance fall; remove it and it returns.
7. **Export.** Export the stored record to CSV through the system file picker. The app reports how many
   rows it wrote, and writes nothing to a location the reviewer did not choose.
8. **Restart.** Close the app and reopen it. The round, the visits and every balance are identical.

## The one purchase

The free tier is **eight customers**, fully usable with no time limit and no trial clock. The single
in-app product is a **non-consumable unlock** that removes the customer limit and opens the complete
visit history and the CSV export. It is offered when the ninth customer is added and when the history
or the export is first used, states one price, and offers no trial, renewal or subscription. It can be
restored after a reinstall. There is no second paywall and no advertising anywhere in the app.

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

- **The privacy policy URL is a placeholder.** `store/listing.json` carries
  `https://lawn-round-log.invalid/privacy` (reserved `.invalid` TLD, resolves nowhere). The owner must
  host the text of `store/privacy.md` at an HTTPS URL he controls and replace the placeholder in both
  the listing and `store/listing.json`. Until then the listing is incomplete, not merely undecided.
- **The in-app privacy policy entry point has not been verified from this file's author.** Play
  requires the policy to be reachable *inside* the app as well as at the listing URL; privacy prose in
  an About screen is not the policy. Confirm the shipped build links the hosted policy before upload.
- **The in-app product does not exist in Play Console yet.** See the console requirement above; the
  reviewer's price sheet depends on it.
- **No screenshots or store graphics are produced in this phase.** `store/assets/**` is a release-phase
  output; any capture made for review must come from the signed build and be labelled synthetic if it
  was not taken on a device.
- **The launcher icon and the store identity are declared elsewhere.** `store/studio.json` is authored
  by the visual-identity work, not by this document; the values here are the listing copy only.
- **`store/data-safety.md` describes one collected row (Financial info → Purchase history)**, from the
  `billing` capability's own declaration. If a future revision adds any SDK that transmits, the Data
  safety form must be updated with it; under-declaring a bundled SDK is a policy defect, not an
  oversight.
