# Reviewer notes — Mark Book for Teachers

Written for the Play reviewer, and to be pasted into the Play Console **App access** and **Review
notes** fields. Claims here describe the shipped build only.

## App access

The slot sells an unlock, so some content is behind a purchase. The console's «no» branch asserts
that paying for content is not required, which this slot cannot assert truthfully; the reviewer
therefore needs access given to them, which is a promo code in the instructions.

The access itself is recorded in the console's App access declaration (`Контент приложения →
Учетные данные`), whose exact string to paste is generated into `store/console-answers.md`; the
credential is deliberately not reproduced here, because it is access the slot hands out rather than
text the pack publishes.

## What to do in two minutes

1. **Launch.** The class list is empty and explains itself. The free tier card states plainly what is
   free and what the one-time purchase adds.
2. **New class** → name it anything → **Create**. The class appears with its own default category.
3. **Open the class.** Tabs: Roster, Grades, Attendance, Notes, Seating, Reports, Setup.
4. **Roster → Add student** (up to 20 on the free tier; there is also **Import from CSV**).
5. **Grades → Add category / Add assignment**, then tap an assignment to mark the whole class from
   one sheet. Each cell takes a score, *missing*, or *exempt*.
6. **Averages show their working** — open a student to see the overall grade with the rounding rule,
   the missing-work rule and any excluded category named.
7. **Reports** → **Export the grade sheet** (CSV) or **Export the class report** (PDF).

## The one in-app purchase, and how to see it

The purchase is a **single non-consumable product**, `recordbook_pro` — one payment, not a
subscription. It unlocks unlimited classes and students, weighted categories with drop-lowest, CSV
export, PDF reports, and backup/restore.

A reviewer does **not** need to buy anything to see the app work, and the free tier is usable rather
than crippled. To see the purchase surface, use either of these, which are available **on the free
tier**:

- **Reports → Export the grade sheet** — because export is part of the unlock, this opens the unlock
  dialog with the feature list and Google Play's own confirmation sheet behind **Unlock**.
- **New class** a second time — the class limit opens the same dialog.

**I already paid → Restore** re-queries Play, so a purchase made with another account on this device
can be recovered without repurchasing. This path exists specifically because the reviews of
competing gradebooks describe lost purchases.

## Privacy, Data safety and permissions

- The app has **no server**; the developer receives nothing. Evidence: no HTTP client or URL exists
  in the app's own code, and `res/xml/network_security_config.xml` forbids cleartext traffic.
- The **Data safety** form declares **Financial info → Purchase history**, because the Google Play
  Billing library (and `com.google.android.datatransport`, a dependency it pulls in) handles the
  purchase. Nothing else is declared because nothing else is collected. Details and the source of
  each claim are in `data-safety.md`.
- The merged release manifest declares four permissions, all listed in `store/privacy.md`:
  `com.android.vending.BILLING`, `android.permission.INTERNET` and
  `android.permission.ACCESS_NETWORK_STATE` (the last two contributed by the billing library's own
  transport dependency, not requested by this app), plus a build-generated AndroidX permission. No
  dangerous permission is requested, and there is no storage permission — files are chosen through
  the system document picker.
- The app is **not directed at children**. It is a teacher's tool. A teacher may enter pupils' names
  and marks, which stay in the app's private storage on the device.
- Users can delete all data in-app (delete a class, or clear app storage, or uninstall). There is no
  account, so there is no account-deletion path to provide.
- Backups the user creates are written wherever the user chooses via the system picker; Android Auto
  Backup may also copy the record to the user's own Google Drive, which the privacy policy states.

## Ads, rating and target audience

- **Contains ads: No.** No advertising SDK is present.
- **Content rating questionnaire:** the app shows no user-generated content, no violence, no
  gambling, no ads. It handles a teacher's own student record offline.
- **Target audience:** adults; the app is a professional tool, not for children.

## Known limitations to be aware of while reviewing

- **The purchase price is not printed inside the app.** Google Play's confirmation sheet shows the
  exact localized price at the moment of purchase, and the store listing shows it on the product
  line. This was a deliberate choice over hard-coding a dollar figure that would be wrong outside
  the United States.
- The six phone screenshots are real captures from a physical device. `store/provenance.json` ties
  their raw capture hashes and the composed assets to the signed release build. The 7-inch and
  10-inch tablet sets are composed from those phone captures because no tablet device was attached;
  they are disclosed as phone-framed assets, not tablet captures.
- The app works entirely offline. The only network traffic is Google's own purchase handling, so a
  review device with no network can still exercise every feature except buying.
