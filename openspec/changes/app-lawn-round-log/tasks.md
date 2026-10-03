# Tasks: app-lawn-round-log

## Recorded decisions

- [x] The slot is `apps/2026-10-01-lawn-round-log`; new and independent, built from its own plan,
  its own promise contract and the shared harness. No sibling slot's code is read
  (`.cursor/rules/ideation-gate.mdc` § "The factory is not the market").
- [x] **The app's own manifest requests no Android permission at all.** **Reason:** the promise that
  carries the slot is that the round does not wait on a login or on someone else's server being up,
  and the two loudest measured complaint classes are login failure and a server outage. The promise
  is therefore *no account and no network in the daily path*, which is what the launch copy says —
  **not** "the app is incapable of using the network". The only network-capable component is the
  Play purchase path from the `billing` capability, whose module contributes no permission and no
  component; the merged manifest is **measured from the built artifact** and this claim is corrected
  if it disagrees.
- [x] The commercial surface is **one** non-consumable Play Billing unlock at **$9.99**, offered at
  the ninth customer or the first CSV export. No ads, no subscription, no second paywall. The visit
  history and the owed balances are **never** withheld — the free tier keeps the whole record and the
  unlock only lifts the customer ceiling and adds the export, and the listing copy, the string
  resources and this contract were corrected to say exactly that on 2026-10-02.
- [x] **`pool-route-log` is deliberately left unbuilt.** **Reason:** it and this slot are one sentence
  — "a solo trade's round on the phone" — and two of our own slots that one sentence describes is the
  internal near-duplicate Play's Spam policy reaches, up to account termination.
- [x] **Persistence is one versioned JSON document written atomically, not a database.** **Reason:**
  the store, the CSV export and the unit-test fixtures become one shape, so "the record survives"
  is one code path; an operator's record is far below a megabyte.
- [ ] **[Human] Approve the product at checkpoint 1.5.** The agent installs and tests the debug APK
  on the connected device; the owner's decision is the product, not the installation. Nothing in
  phase 2 runs before that: `node scripts/approve-release.mjs --slot lawn-round-log --note "<the
  owner's own words>"`.

## 1. Domain core, store and their tests — worker 1

Base package `com.gexetr.lawnroundlog`.

- [ ] **[Build]** `domain/Model.kt` — `Customer(id, name, address, phone, notes, rateUsd, active,
  createdAt)`, `Visit(id, customerId, dateIso, status, amountUsd)`,
  `enum class VisitStatus { MOWED, SKIPPED, RAINED_OFF }`,
  `Payment(id, customerId, dateIso, amountUsd)`, `RoundState(customers, visits, payments)`.
- [ ] **[Build]** `domain/Balance.kt` — derived balances only; no stored balance field anywhere.
  `balanceFor(customerId)` = Σ mowed visits − Σ payments; `totalOwed()` across active customers.
- [ ] **[Build]** `domain/Round.kt` — pure functions `orderRound(...)`, `move(customerId, toIndex)`,
  `pushByOneDay(dateIso)`, `dueOn(dateIso)`, `mark(customerId, dateIso, status)`. The push MUST move
  each affected customer exactly once and MUST be idempotent-safe under a double invocation.
- [ ] **[Build]** `domain/Csv.kt` — `exportCsv(state)` returning the text plus per-table row counts.
- [ ] **[Build]** `data/RoundStore.kt` — JSON persistence in the slot's own private files directory,
  versioned document, atomic write (temp file + rename), tolerant read of a missing/corrupt file
  (returns an empty state rather than crashing).
- [ ] **[Test]** `app/src/test/java/com/gexetr/lawnroundlog/domain/` — the fixtures that make each
  promise falsifiable: balance arithmetic, restart identity, a skipped visit is not billable, a
  double push moves each customer exactly two days and duplicates nobody, CSV row counts, and the
  corrupt-file read.
- [ ] **[Gate]** `node scripts/run-slot-tests.mjs --slot lawn-round-log --rerun`.

## 2. Design system, shell and settings — worker 2

- [ ] **[Build]** `ui/theme/Color.kt`, `ui/theme/Shape.kt`, `ui/theme/Type.kt`, `ui/theme/Theme.kt` —
  the slot's own palette, taken from the slot's `store/studio.json` `palette` block so the store pack
  and the app cannot drift, with an explicit light and dark mapping and **no** dynamic colour and no
  default purple seed.
- [ ] **[Build]** `ui/components/Brand.kt` — the slot's own mark glyph drawn in Compose, plus the
  reusable vocabulary: a status rail/pill that states a visit status in a word **and** a shape, a
  balance row, a stat tile, an empty state.
- [ ] **[Build]** `ui/PlayKilnApp.kt`, `MainActivity.kt` — the destination host and its own back
  stack, edge-to-edge, window size class read for the wide layout.
- [ ] **[Build]** `data/SettingsStore.kt` + `ui/settings/SettingsScreen.kt` +
  `ui/settings/SettingsViewModel.kt` — a settings surface reachable from the main screen where
  **every** preference changes observable behaviour (candidate set: currency symbol, default visit
  amount, first day of the round, confirm-before-delete), persisted, no dead setting.
- [ ] **[Build]** `res/values/strings.xml` + `res/values-ru/strings.xml` — every user-facing string
  is a resource from the first screen; **no** hardcoded prose in Kotlin.
- [ ] **[Build]** `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml` — cloud backup and
  device transfer separated; the record is the operator's own data and no secret is stored.

## 3. Screens — worker 3

- [ ] **[Build]** `ui/round/RoundScreen.kt` + `RoundViewModel.kt` — the ordered day list, reorder,
  one-tap mowed/skipped/rained-off, the single-action push-the-round, and the total owed. Every
  control ≥ 48 dp; nothing carries meaning by colour alone.
- [ ] **[Build]** `ui/customer/CustomerScreen.kt` + `CustomerViewModel.kt` — add, edit, delete a
  customer; per-customer balance; payment recorded in one tap and removable; delete requires a
  confirmation that names the customer.
- [ ] **[Build]** `ui/history/HistoryScreen.kt` + `HistoryViewModel.kt` — visit and payment history
  per customer and across the round, editable and deletable, with the CSV export behind the purchase.
- [ ] **[Build]** `ui/unlock/UnlockScreen.kt` + `UnlockViewModel.kt` — one sheet, one price, no trial
  clock, no subscription wording, restore path.
- [ ] **[Gate]** no screen renders a hardcoded user-facing string.

## 4. Platform, capabilities and store drafts

- [ ] **[Build]** `app/build.gradle.kts` and `app/src/main/AndroidManifest.xml` — billing wired for
  one non-consumable product; **no** `uses-permission` element in the manifest at all.
- [ ] **[Build]** `capabilities.json` — `billing` enabled, and only `billing`.
- [ ] **[Build]** `store/studio.json` — the slot's declared identity: `mark`, the full `palette`
  (the same colours as `ui/theme/Color.kt`), per-shot `headlines`/`kickers`, `featureTagline`,
  `featurePill`. Written **before** the UI is styled.
- [ ] **[Build]** `store/listing.json`, `store/listing.md`, `store/privacy.md`,
  `store/data-safety.md`, `store/review-notes.md`, `store/release-notes.md` — drafts. Data safety
  declares **no** collection, because the operator's record never leaves the device and the app's
  own code neither reads nor writes the network. The claim is about **the slot's code, not the merged
  manifest**: `billing-ktx` 9.1.0 pulls `com.google.android.datatransport` 3.1.8, which contributes
  `android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE` and an internal
  `TransportBackendDiscovery` service, and Google uses it to upload billing events — so the merged
  release manifest declares four permissions and only the app's own manifest declares none. Both the
  privacy policy and the data-safety sheet state this in the same words, and the merged manifest is
  read from the built artifact rather than asserted (measured 2026-10-02 by `aapt2 dump permissions`
  against the signed release APK).

## 5. Verification and the stop

- [ ] **[Gate]** `node scripts/run-slot-tests.mjs --slot lawn-round-log --rerun`
- [ ] **[Gate]** `node scripts/verify.mjs --slot lawn-round-log --build` — compiles `assembleDebug`
  and does not sign.
- [ ] **[Gate]** every promise in `differentiators.json` resolved — `shipped` with a proof that
  resolves on disk, or `narrowed`/`dropped` in writing. **`verify.mjs` fails while any is
  `deferred`**, and the six promises are the reason this slot was chosen.
- [ ] **[Human] CHECKPOINT 1.5** — the agent has installed and tested
  `app/build/outputs/apk/debug/app-debug.apk` on the connected device (serial, artifact hash and
  observed screens in the handoff); the owner approves the product, or asks for changes.

**Nothing in phase 2 runs here.** `prepare-release.mjs`, `scripts/store/render.py` in its rendering
modes and `media-capture.mjs` all refuse without a recorded approval, and working around that refusal
is the failure this phase exists to prevent.
