# Reviewer access — Laneset: Swim Set Builder

Everything a Play reviewer needs to reach the part of the app that is sold, and to confirm the free
part works with nothing redeemed.

## App access declaration (`Контент приложения → Учетные данные`)

**Answer: `Да` (restricted sections exist), on the `Платежи…` line.**

The radio question is `В вашем приложении есть закрытые разделы?`. The console's `Да` branch lists what
it counts as a gate, including `Платежи, например за контент для однократных покупок, подписки или
уровни доступа.` Laneset sells a **one-time in-app purchase** (`laneset_unlock`), and four capabilities
are behind it, so the `Нет` branch — which asserts that no sign-in **and no purchase** is required —
cannot be answered truthfully here.

The reviewer cannot buy the product themselves: the console's own dialog states
`Мы не можем создавать новые аккаунты, использовать личные аккаунты для совершения покупок`. The only
mechanism Play offers for handing over paid content is a **promo code**, entered in the credentials
dialog's free-form field (below). That is the access route this file describes.

## Gated features

Read from the shipped code. The single source of the entitlement is `BillingManager.isUnlocked`
(`app/src/main/java/com/gexetr/laneset/data/BillingManager.kt:98`), seeded `false` and made
authoritative by Play in `restoreOwnedPurchases()`
(`app/src/main/java/com/gexetr/laneset/data/BillingManager.kt:360`). Every gate below is one read of
that flow.

| Feature (the name the user sees) | Where the gate is read |
|---|---|
| **Every other focus: Sprint, Threshold, Technique and Recovery** — only Endurance is free | `ui/home/HomeViewModel.kt:180` (`selectFocus` routes a non-free focus to `HomeNavigation.Unlock`); `ui/home/HomeUiState.kt:68` (`FREE_FOCUS = Focus.ENDURANCE`); label `unlock_benefit_focuses` in `res/values/strings_unlock.xml:19` |
| **The guided 400 m and 200 m pace test** | `ui/home/HomeViewModel.kt:154` (`PaceTestRequested -> navigateIfUnlocked(...)`); the lock itself is `ui/home/HomeViewModel.kt:334` (`navigateIfUnlocked`); label `unlock_benefit_pace_test` in `res/values/strings_unlock.xml:20` |
| **Coach-workout import** (paste a workout a coach wrote) | `ui/home/HomeViewModel.kt:153` (`ImportRequested -> navigateIfUnlocked(...)`); same lock at `ui/home/HomeViewModel.kt:334`; label `unlock_benefit_import` in `res/values/strings_unlock.xml:21` |
| **Share and print a finished set** (the handoff) | `ui/home/HomeViewModel.kt:151` (`ExportRequested -> requestHandoff { ... }`); the lock itself is `ui/home/HomeViewModel.kt:323` (`requestHandoff`); label `unlock_benefit_handoff` in `res/values/strings_unlock.xml:22` |

The four features are also named inside the app on the unlock screen
(`ui/unlock/UnlockScreen.kt:226-229`) and, while locked, on Home itself: the focus strip marks each
paid focus `… · by purchase` (`home_focus_chip_locked`), and the handoff and import sections carry
`home_handoff_locked` / `home_import_locked` with a `See what the unlock gives` button
(`res/values/strings.xml:99`, `:102`).

## Reviewer access route — a Play promo code

**This code does not exist yet.** It is created in Play Console by the account owner, and the console
creates it only **after** the one-time product exists — the page's info block reads `Создавать
промоакции можно только для подписок, контента для продажи или платных приложений`, and with an empty
product list the page shows no create button — so it can never exist before the product does. It does
become active for an app with **no reviewed release**, so the code is ready to paste as soon as the
product exists, before the first submission. What follows is the plan the owner executes, not a live
code. Do not look for a code value in this repository.

**What the owner does (Play Console):**

1. Create/finish the app in Play Console so `com.gexetr.laneset` is registered
   (`docs/play-console-checklist.md` §9).
2. Create the one-time product under `Монетизация с помощью Play → Встроенные продукты`
   (`…/one-time-products`). Its **product ID must be exactly** `laneset_unlock` — the string the app
   hands to Play Billing (`data/BillingManager.kt:134`, `UNLOCK_PRODUCT_ID`). The product carries
   **exactly one purchase option**, and it must be **`Активировать`**ed, not left a draft — a draft
   option is not returned to the app and the purchase fails in the reviewer's session
   (`docs/play-console-checklist.md` §5).
3. Create the promo code under `Монетизация с помощью Play → Promotions` (`…/promotions`). The page
   only offers create once the product exists. A promotion lasts at most **365 days** end-to-end; the
   code count is final after saving. Download the codes promptly (`Скачать коды`); the CSV is never
   committed, because a code is a free unlock of what the app sells (`docs/play-console-checklist.md`
   §5).
4. In the App access dialog (`Контент приложения → Учетные данные` → the `Да` branch → the credentials
   entry), paste one code into the field
   **`Другие данные, которые требуются для входа`** (the `instructions` field, cap 500 characters).
   This is the free-form field — the code does **not** go in `Имя пользователя…` or `Пароль`, which
   this app has none of. Tick the certification checkbox
   `Учетные данные, указанные в этой декларации, обеспечивают полный доступ ко всем функциям и
   контенту приложения, включая платный` together with the instructions; with the box clear the
   dialog refuses to save (`Не удалось сохранить изменения`). The code does give full access including
   the paid content, so the certification is true.

**What the reviewer does:**

1. Redeem the code with the Google account signed into the test device (Play Store's redeem flow, or
   the promo link the owner supplies). Redeeming a one-time-product code grants the `laneset_unlock`
   entitlement to that account at no charge.
2. Open Laneset. The billing manager reads owned purchases back from Play on connect
   (`data/BillingManager.kt:291`, `restoreOwnedPurchases()` declared at `:360`), so the unlock appears on its own; if it
   does not, open **Unlock** from Home (`See what the unlock gives`) and tap **`Restore a purchase`**
   (`res/values/strings_unlock.xml:33`). The unlock screen then shows its **Unlocked** state.
3. With the unlock owned, all four gated features above are reachable: the focus strip offers Sprint,
   Threshold, Technique and Recovery; the trials button opens the guided 400 m / 200 m pace test; the
   `Open a coach workout` button opens the import screen; and `Share or print the set` opens the
   handoff sheet.

## What works with no payment and nothing redeemed (free surface)

A reviewer who redeems nothing can still confirm the app works end to end, because the free tier
generates a complete, target-paced session:

- **Home** (`ui/home/HomeScreen.kt`): choose a pool length (25 m or 50 m), the minutes available
  (20/30/45/60/75/90), the strokes to leave out, and the gear on the deck — all free.
- **The Endurance focus is free** and is the default (`ui/home/HomeUiState.kt:68`; its chip reads
  `Endurance · free`). Building a set computes a full warm-up / main set / cool-down with a target
  time on every interval, using the app's declared default pace of 110 s/100 m
  (`ui/home/HomeUiState.kt:35`, `DEFAULT_FREE_PACE_PER_100_SECONDS`); the app says the pace is its own
  default until the trials are swum.
- **The whole set is usable free**: the interval hero, the totals, the runner (`Back` / `Next`) and the
  full set list with targets and rest (`ui/home/HomeScreen.kt`), plus `Discard this set`.
- **About** (`ui/about/AboutScreen.kt`) opens, including the published privacy policy link.
- **Settings** (`ui/settings/SettingsScreen.kt`) opens: theme, wallpaper colours, app language and
  vibration feedback.
- **The Unlock screen itself** opens for free (from `See what the unlock gives`, or by tapping any
  locked control), and **Restore a purchase** works without owning anything.

## Other review notes

- **No account and no sign-in.** There is no login, no email/password, no Google sign-in and no 2FA;
  the demo-account fields of the App access dialog do not apply. The Google account the reviewer uses
  is only the account Play Billing charges/grants against, not an app account.
- **No ads.** The app carries no advertising SDK and declares no ads (`store/listing.json`, `ads`).
- **Offline by construction.** The app's own code opens no network connection — the programming rules
  and the pace arithmetic ship in the APK and a full session is generated in airplane mode. The only
  network user is **Google Play Billing** for the one-time unlock, which merges
  `android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE` into the packaged
  manifest (`app/src/main/AndroidManifest.xml:4-11`).
- **One purchase, not a subscription.** The single product is a non-consumable one-time unlock; there
  is no subscription, trial, renewal or consumable (`store/listing.json`; `data/BillingManager.kt`).
- **No content is gated behind a code other than the purchase.** There is no referral, invitation or
  PIN gate.
- **The tablet screenshots are framed phone captures.** The `tablet-7-*` and `tablet-10-*` images
  under `store/assets/<locale>/` are composed from the same phone captures as `phone-01..06`, not
  captured on a tablet device, because no tablet device is attached. The app is responsive and its
  large-screen layout is a real layout, but these store images are a framing of the phone session
  rather than a tablet capture, so they are presented as phone captures rather than passed off as
  tablet evidence.
