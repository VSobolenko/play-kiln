# Wallet IDs — W88b

Owner complaint C6 requires zero job IDs, request IDs, terminal IDs, or UUID
substrings in user-visible strings (`owner-complaints-canonical.md`, C6).

## Changed display sites

| Site | Old text | New text | Evidence path:line |
|---|---|---|---|
| Released outcome (English) | `Job %1$s failed or was cancelled. It cost nothing; %2$d credits were returned.` | `This job failed or was cancelled. No charge was made; %1$d credits were returned.` | `WalletScreen.kt:382`; `values/strings_wallet.xml:33` |
| Released outcome (Russian) | `Задача %1$s завершилась с ошибкой или отменена. Списания не было; %2$d кредитов возвращено.` | `Задача завершилась с ошибкой или была отменена. Списания не было; возвращено кредитов: %1$d.` | `WalletScreen.kt:382`; `values-ru/strings_wallet.xml:33` |
| Settled outcome (English) | `Job %1$s was delivered. %2$d credits were charged after receipt.` | `Your result was delivered. %1$d credits were charged.` | `WalletScreen.kt:386`; `values/strings_wallet.xml:34` |
| Settled outcome (Russian) | `Задача %1$s выполнена. После подтверждения получения списано %2$d кредитов.` | `Результат доставлен. Списано кредитов: %1$d.` | `WalletScreen.kt:386`; `values-ru/strings_wallet.xml:34` |
| Receipt pending (English) | `Job %1$s is awaiting a valid delivery receipt. It has not been charged.` | `Your result is waiting for delivery confirmation. You have not been charged.` | `WalletScreen.kt:389`; `values/strings_wallet.xml:35` |
| Receipt pending (Russian) | `Ожидается корректное подтверждение получения задачи %1$s. Списания пока нет.` | `Ожидается подтверждение доставки результата. Списание пока не выполнено.` | `WalletScreen.kt:389`; `values-ru/strings_wallet.xml:35` |
| Release unconfirmed (English) | `Job %1$s ended, but its credit release is not confirmed yet.` | `This job ended, but its credit refund is still being confirmed.` | `WalletScreen.kt:390`; `values/strings_wallet.xml:36` |
| Release unconfirmed (Russian) | `Задача %1$s завершилась, но возврат кредитов ещё не подтверждён.` | `Задача завершилась, но возврат кредитов ещё проверяется.` | `WalletScreen.kt:390`; `values-ru/strings_wallet.xml:36` |

The two outcomes that still display a credit count now pass it as `%1$d`.
Pending and unconfirmed outcomes have no format arguments. All four resources
remain explicitly `translatable="true"` in both locales. `jobId` remains only
as wallet-screen model data and the lazy-list item key; it is not supplied to
display text.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| The wallet outcome card no longer passes `job.jobId` to a visible string. | MEASURED | Regression assertion run before editing failed with `FAIL: WalletJobOutcomeCard still passes a raw job id into a visible string.`; after editing it passed. Current call sites: `WalletScreen.kt:382-390`. |
| Both locale files retain the four outcome strings as translatable; released/settled retain only the credit-count argument and pending/unconfirmed have none. | MEASURED | PowerShell-piped Node assertion read both XML files and verified each resource's `translatable="true"` attribute and placeholder list; output: `PASS: no wallet outcome interpolates a job id; both locales keep translatable resources and only the passed credit-count placeholders.` Resource sites: `values/strings_wallet.xml:33-36`; `values-ru/strings_wallet.xml:33-36`. |
| The requested identifier terms were searched across app main sources; within the owned wallet UI/resources, the four visible interpolations were in `WalletJobOutcomeCard` and its four outcome strings, all changed above. | MEASURED | `rg "jobId|requestId|terminalJobId|resultJobId|\\.take\\(8\\)|\\.substring\\(0,\\s*8\\)|UUID" apps/2026-10-15-plinth-product-photos/app/src/main`; matching wallet locations initially appeared at `WalletScreen.kt:383,388,393,397` and `values[-ru]/strings_wallet.xml:33-36`. Other matches are outside these three owned files. |
| A Gradle build was run for this change. | UNVERIFIED | Not run as requested; sibling integrator owns the single compile. |
