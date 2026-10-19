# W12 - Studio route fixes

Source paths below are relative to this slot. `S` means
`app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioRoute.kt`.

## Smart-cast fix

Before (original line 265):

```kotlin
if (price != null && (state.balance == null || state.balance < price)) {
```

After (`S:267-268`):

```kotlin
val balance = state.balance
if (price != null && (balance == null || balance < price)) {
```

The balance is read once; null and insufficient-balance cases retain the same
comparison. Compilation is **UNVERIFIED**: no Gradle or compiler was run.

The whole file was read. Other nullable/delegated-property sites swept:

| Site | Observation |
|---|---|
| `S:80`, `S:338` | Delegated confirmation is consumed through `?.let` and destructured into local operation/price values. |
| `S:117-125` | Balance and reserved credits use `?.let` lambda values. |
| `S:205-220` | Quote/price are locals; delegated config reads use safe calls and nullable comparisons, not smart casts. |
| `S:238`, `S:241-250` | Row reason uses `?.let`; quote is local; price is only null-tested. |
| `S:262-263` | Captured price is a local `val` checked before constructing confirmation. |
| `S:278-301` | Last server state, terminal error, error code, retry delay, charge, and bitmap are captured using safe-call lambdas. |
| `S:308-331` | Boolean state properties and saved-variant null comparisons do not require smart casts. |
| `S:340-353` | Confirmation quote is local; state config uses safe calls. |
| `S:363-372` | Confirmation balance and remaining credits are lambda locals. |
| `S:447-455` | Action price/balance are function parameters, with price null-check and balance `?.let`. |

No other unsafe delegated-property smart-cast pattern was found by this source
inspection. Only an orchestrator build can establish compiler acceptance.

## Accessible button names

Both the primary `Button` (`S:421-427`) and secondary `OutlinedButton`
(`S:429-435`) now explicitly set `contentDescription = actionLabel` through
`Modifier.semantics`. The label is resolved once with
`stringResource(R.string.studio_live_generate)` at `S:419`.

Both controls use the existing name **Generate** / **Сгенерировать**:

- `app/src/main/res/values/strings_studio_live.xml:17`
- `app/src/main/res/values-ru/strings_studio_live.xml:17`

No resource keys were added and no XML files were changed. The action handlers,
enabled conditions, placement, visible labels, and cost chips were preserved.
The helper already contained visible `Text`; the static checker did not follow
that helper. The new explicit semantics remove both reported findings without
claiming an on-device TalkBack observation.

## Credit-flow inspection

| Requirement | Observed source |
|---|---|
| Fixed balance and top-up action | Present in the scaffold's `topBar` (`S:99`), with balance at `S:117-119` and wallet action at `S:128-130`, outside the scrolling `LazyColumn`. |
| Cost chip beside balance instead of per-card cost prose | Present as a combined cost/balance chip inside each operation action (`S:447-459`), using `studio_live_action_cost_balance` or its unknown-balance variant (`S:454-455`). This is an action-level chip, **not a cost chip in the top bar**. Known-price prose is only in confirmation (`S:362`); unknown-price warnings remain in cards (`S:250-254`). |
| Confirmation shows quote, current balance, projected remainder | Present at `S:362-366`. Missing balance and insufficient funds show explicit alternatives at `S:367-374`; quote mismatch is surfaced at `S:375-379`. |

These existing credit surfaces were preserved, not rebuilt.

## Observed checks

Before the edit, `node scripts/verify.mjs --slot plinth-product-photos`
exited 1 and reported both `StudioRoute.kt:417` and `StudioRoute.kt:421`
as unnamed buttons.

After the edit:

```text
node scripts/check-locales.mjs --slot plinth-product-photos
exit 0
PASS locales - 1 translated language(s) x 906 key(s) complete, plus the en default in values/: 906 entries, every format argument and plural category matching the default

node scripts/verify.mjs --slot plinth-product-photos
exit 1
PASS       uiux.a11y                       17 Icon/Image call(s) each with a contentDescription argument; 171 clickable element(s) each with an accessible name and no declared extent below 48 dp
122 PASS, 3 UNVERIFIED, 4 SKIP, 1 FAIL
```

The remaining failure is `slot.device`: its recorded build digest is stale
(`9fb7a3250207...` versus `f2d5b99f6a1c...` in this run). No device record was
modified. The build row explicitly remained `UNVERIFIED`, and the report recorded
`subprocesses: 0`. Full outputs are retained under
`.kiln/scratch/copilot-plinth-w12-studio-route/` at the repository root.

No Gradle, compile, commit, staging, or other git-state-changing command was run.
Elapsed wall-clock time was approximately four minutes.

## Claim ledger

| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| The unsafe balance comparison now uses one local read | MEASURED | `S:267-268` |
| Both buttons have explicit localized semantic names | MEASURED | `S:419-435`; both locale `strings_studio_live.xml:17` files |
| Existing credit UI includes fixed balance/top-up, action-level cost/balance chips, and confirmation arithmetic | MEASURED | `S:99-130`, `S:362-374`, `S:447-459` |
| Locale coverage and format arguments pass | MEASURED | `node scripts/check-locales.mjs --slot plinth-product-photos`: exit 0, 906 keys |
| The accessibility row no longer reports StudioRoute | MEASURED | `node scripts/verify.mjs --slot plinth-product-photos`: quoted `PASS uiux.a11y` row |
| Overall source verification is not green | MEASURED | Same command: exit 1, `slot.device` failure, 122 PASS / 3 UNVERIFIED / 4 SKIP / 1 FAIL |
| Kotlin compilation succeeds | UNVERIFIED | No compile run; orchestrator must run `node scripts/verify.mjs --slot plinth-product-photos --build` under its exclusive build slot |
| TalkBack behavior on device | UNVERIFIED | Requires orchestrator/device inspection of the current compiled artifact |
