# W94d — credits: one place (top bar), debug balance restored to 10

## E8 — removed copy

| Removed copy | Where it rendered | path:line before | Status |
|---|---|---|---|
| "Used %1$d credits" / "Использовано кредитов: %1$d" (`studio_live_charge`) | last item of the Studio result list, under the result actions | render: `ui/studio/StudioRoute.kt:388-390` (`state.charged?.let { item { Text(stringResource(R.string.studio_live_charge, charged)) } }`); strings: `res/values/strings_studio_live.xml:25`, `res/values-ru/strings_studio_live.xml:25` | **NOT removed by me — blocked** |

`StudioRoute.kt` and the `strings_studio_live.xml` files are outside my owned list (`ui/studio/**` is
forbidden), so I did not edit them. The integrator/studio owner must delete `StudioRoute.kt:388-390`
and both `studio_live_charge` strings (then `state.charged` becomes unused outside tests; keep or drop it).
No other post-result "used/spent credits" copy exists: grep of `app/src/main` for
`Использовано|used %|credits_used|creditsUsed|spent` found only this string; the `session_*` "No credits
have been spent" lines are error messages, not post-result balance lines. Per-button prices untouched.

## Balance refresh mechanism (E8 / W85 drift)

Top bar shows `chipBalance` (`PlayKilnApp.kt:272`, `walletViewModel.chipBalance`) into `PlinthTopBar`
→ `BalanceChip`. The wallet value is persisted in `WalletSessionStore.balance`
(`data/session/WalletSessionStore.kt:23`), a `DataStore` flow with `distinctUntilChanged`; every
`saveSession`/balance write updates `BALANCE_CACHE`, so any screen that writes the cache after a spend
moves the chip without per-screen refresh. Limit: the chip is only as fresh as the last write; it does
not poll the server. I did not change code (no change in my owned files was needed).

## E10 — credit restore

Mechanism: W82 `server/scripts/debug-set-balance.mjs` (already deployed in `current-debug`), debug
service only.

```
ssh root@78.111.89.46 "set -a; . /etc/plinth-product-photos/server-debug.env; set +a; cd /opt/plinth-product-photos/current-debug; node debug-set-balance.mjs --debug --latest --balance 10"
```

Observed:
`{"walletId":"e777a037-4763-4b7a-b385-ae5491447709","before":{"balance":1,"refundDebt":0,"purchaseHold":0},"after":{"balance":10,"refundDebt":0,"purchaseHold":0},"ledgerEntry":"not-recorded-schema-does-not-allow-DEBUG_TOPUP"}`

Health afterwards: `HTTP 200`, `ok=true`, `databaseReachable=true`. Resulting server balance: 10.
Note: `--latest` picked wallet `e777a037…` (not W82's `e6f7e2dd…`); if the owner's phone uses another
wallet, rerun with `--wallet <uuid>`. The app's cached chip updates on its next wallet fetch.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Only one post-result credit line exists | MEASURED | grep `Использовано\|used %\|credits_used\|creditsUsed\|spent` over `app/src/main` → only `studio_live_charge` plus "No credits have been spent" errors |
| It renders at `StudioRoute.kt:388-390` | MEASURED | view of file lines 370-400 |
| Line not removed | MEASURED | outside owned files; unchanged |
| Debug wallet balance 1 → 10 | MEASURED | SSH output above, 2026-10-18 |
| Debug health OK | MEASURED | `curl.exe` → HTTP 200, ok=true |
| Chip reads a single DataStore flow | MEASURED | `PlayKilnApp.kt:272`, `WalletSessionStore.kt:23` |
| Chip cannot go stale after a spend | UNVERIFIED | depends on every spend path writing `BALANCE_CACHE`; not traced end to end, no Gradle run |
