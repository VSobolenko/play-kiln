# W45: Credits clarity

The balance is a labelled credit balance, not an unexplained abbreviation:
`Credits: N` / `Кредиты: N`, including `Credits: —` / `Кредиты: —` when unknown.
Its accessibility description includes the displayed balance and says it opens
the wallet. An unknown balance is not presented as zero.

Studio opens with one non-blocking explanation strip above the hero. It explains
that server tools such as scene generation and background removal use credits,
that each tool shows its cost, and that Wallet holds the balance and pack
purchases. It has **Open wallet** and **Got it** actions in a wrapping action row.
There is no new price, fixed credits-per-photo promise or blocking modal.

Dismissal uses `rememberSaveable`, scoped to Studio's saved UI state. It is not an
installation-wide "seen forever" preference: a fresh Studio session without
restored state can show the explanation again. This keeps the optional onboarding
state out of the shared SettingsStore; it does not add a second settings store or
change settings behaviour. A permanent DataStore dismissal was deliberately not
added in this bounded copy/affordance change.

More and Wallet now both say **Credit wallet** / **Кошелёк кредитов**. Pack help
connects buying packs to spending credits on server tools; the existing Wallet
explanation still states that credits are charged only for a delivered result.
Neither the catalog nor billing SKU/grant/pricing code was edited for W45.

Cost chips remain in place. English uses `credit` / `credits`; Russian uses
`кредит` / `кредита` / `кредитов` through Android plurals, rather than `кр.`.
Both upscale choices include a credit unit. Tool-card titles and cost chips are
stacked so the longer, clearer cost cannot squeeze out the title. Cost text can
wrap instead of being ellipsized, with bounded rounded corners rather than an
oval clip on multi-line text. These are presentation changes: costs still
come from the existing `StudioOperationCatalog.costFor(...)` calls.

## Source-only verification

No Gradle, device/ADB action, commit or release command was run for W45, preserving
the W40 device/build lane.

- `node scripts\verify.mjs --slot plinth-product-photos --only ui`:
  exit 0, **23 PASS**, **0 subprocesses**; initial run 1977 ms, repeat 12739 ms.
- An inline Python `xml.etree.ElementTree` check read `strings_shell.xml`,
  `strings_wallet.xml`, `strings_wallet_w6.xml` and `strings_home.xml` in both
  `values` and `values-ru`: XML parsing, resource-type/key parity and numbered
  format-argument parity passed (16, 41, 12 and 45 entries respectively).
- `git diff --check` on the touched tracked UI paths reported no whitespace errors.
- Added four Compose instrumentation cases in `CreditsClarityTest`: known balance
  and wallet callback; unknown balance and wallet callback; visible action credit
  unit; tip wallet callback, dismissal and saved-state restoration. These cases
  were **not compiled or executed**. No red/green runtime result is claimed.

When W40 releases the lane, the orchestrator must build and run
`com.gexetr.plinthproductphotos.ui.home.CreditsClarityTest` through the factory's
locked `runGradleTasks` route (`connectedDebugAndroidTest` with the instrumentation
class filter), once with English and once with Russian resources. Review Studio,
Library, More and Wallet at phone width and enlarged font scale, including the
two upscale cost values. Runtime layout, TalkBack output and retained navigation
state remain unverified until then.

Observed wall-clock interval from task receipt to the second source-check
capture: 2026-10-17 02:21:46 to 02:27:29 (+03:00), 5 minutes 43 seconds;
documentation and final handoff followed.

## Claim ledger

Paths below are relative to this slot.

| Claim | Label | Evidence |
|---|---|---|
| The shared balance names credits and its accessibility description includes the value and wallet purpose. | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/components/PlinthTopBar.kt:55-71`; `app/src/main/res/values/strings_shell.xml:7-9` and `values-ru/strings_shell.xml:7-9`. |
| Studio includes a dismissible explanation with a wallet action; dismissal is saved UI state, not a permanent preference. | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/home/HomeScreen.kt:142-147`; `ui/components/CreditsExplanation.kt:24-55` under the same Java package root. |
| Both languages connect credit packs to server-tool spending and use the same wallet name in More. | MEASURED | `app/src/main/res/values/strings_shell.xml:10-14`, corresponding `values-ru` entries; `strings_wallet.xml:3` and `strings_wallet_w6.xml:8` in both locales. |
| Cost presentation uses pluralized units and preserves both catalog-derived upscale costs. | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/home/HomeScreen.kt:179-198`; `ui/components/PlinthCostChip.kt:15-20`; `ui/components/PlinthToolCard.kt:59-63`; `app/src/main/res/values/strings_home.xml:43-47` and `values-ru/strings_home.xml:43-49`. |
| The source-only UI checks found no failures in this run. | MEASURED | `node scripts\verify.mjs --slot plinth-product-photos --only ui`: exit 0, 23 PASS, 0 subprocesses. This is not build or device evidence. |
| The new instrumentation cases pass and the UI is clear at runtime in both languages. | UNVERIFIED | `app/src/androidTest/java/com/gexetr/plinthproductphotos/ui/home/CreditsClarityTest.kt`; requires the orchestrator's locked Gradle/device run after W40, plus the visual/TalkBack review above. |
