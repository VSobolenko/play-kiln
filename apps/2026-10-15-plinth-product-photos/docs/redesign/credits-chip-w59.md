# W59 — Modern credits chip

## Result

The top-bar balance is now a standalone `BalanceChip` (`app/src/main/java/com/gexetr/plinthproductphotos/ui/components/BalanceChip.kt`). It is a quiet, borderless tonal pill with a tinted Lucide plus glyph, a `Credits · N` label, and a 48 dp minimum target. A missing balance reads “Get credits” rather than presenting an unknown value or implying zero. Both states retain the accessible top-up purpose and open the existing Wallet callback.

The English and Russian labels live in `app/src/main/res/values/strings_shell.xml` and `app/src/main/res/values-ru/strings_shell.xml`. `CreditsExplanation` stays on Home and keeps its dismiss/restore behavior; its surface now uses theme surface roles instead of the more prominent secondary container. Its copy is shorter and still points to Wallet.

No wallet pricing or purchase behavior changed. Wallet continues to list consumables from `billingCatalog().consumables`, render Play-provided localized prices, and use one sticky primary `Button` for the selected product (`ui/wallet/WalletScreen.kt:188-216,259-268`). The top-up affordance in the wallet header scrolls to the pack list; it is not a second purchase action.

## Evidence

| Claim | Label | Evidence |
|---|---|---|
| The count state displays a credit unit, is at least 48 dp tall, and opens Wallet on tap. | MEASURED | `app/src/androidTest/java/com/gexetr/plinthproductphotos/ui/home/CreditsClarityTest.kt:22-39`; device report `app/build/outputs/androidTest-results/connected/debug/TEST-SM-A405FM - 11.xml` records all four `CreditsClarityTest` cases passing. |
| The missing-balance state offers “Get credits” and opens Wallet without displaying zero. | MEASURED | `CreditsClarityTest.kt:41-52`; final connected test report above. |
| The Home explanation still opens Wallet and dismisses across saved-state restoration. | MEASURED | `CreditsClarityTest.kt:62-79`; final connected test report above. |
| Wallet retains live catalog packs, store-provided prices, and a single primary purchase button. | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/wallet/WalletScreen.kt:188-216,259-268`; no Wallet files or catalog values changed in W59. |
| The chip and explanation use existing theme roles; no palette hex values changed. | MEASURED | `ui/components/BalanceChip.kt:39-45`, `ui/components/CreditsExplanation.kt:32-35`; no theme or palette file changed. |

The focused device command was run through the shared Gradle route:

```powershell
node -e "import('./scripts/lib/gradle.mjs').then(({runGradleTasks})=>{const slotDir=process.cwd()+'\\apps\\2026-10-15-plinth-product-photos';const x=runGradleTasks(slotDir,['connectedDebugAndroidTest','-Pandroid.testInstrumentationRunnerArguments.class=com.gexetr.plinthproductphotos.ui.home.CreditsClarityTest']);const out=String(x.result.stdout||'')+String(x.result.stderr||'');console.log(out.split(/\r?\n/).slice(-80).join('\n'));process.exitCode=x.result.status??1;})"
```

The final run reported `BUILD SUCCESSFUL`. The slot JVM suite also passed: `node scripts/run-slot-tests.mjs --slot plinth-product-photos --rerun` reported 280 tests, 0 failures, 0 errors, and 0 skipped.
