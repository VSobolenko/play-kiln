# W87f — HomeScreen.kt compile fix

## Claim ledger
- CLAIM: HomeScreen.kt syntax corruption was one unclosed `onClick = if … else {` argument in `HomeToolCard` (missing `}` and `,`), which cascaded to the end of the file. Evidence: `HomeScreen.kt` `HomeToolCard`, now a hoisted `val onToolClick: () -> Unit`. STATUS: MEASURED (compile).
- CLAIM: HomeScreen.kt now produces no compiler errors. Evidence: `runGradleTasks(<slot>, [':app:compileDebugKotlin'])` reports no `HomeScreen.kt` diagnostics. STATUS: MEASURED.
- CLAIM: Full module compile is NOT yet green. Remaining errors are outside this worker's ownership: `ui/PlayKilnApp.kt:225-231` (unresolved `GENERATED_SCENE`, `RELIGHT`, `RECOLOUR`, `EXPAND`, `ERASE`, `UPSCALE`, `VIRTUAL_MODEL`) and `ui/autocard/GeneratedSceneRoute.kt:104-105` (conflicting `guidance` declarations). STATUS: MEASURED, UNFIXED (out of scope).

Toolbox redesign (equal 2-column tool grid, neutral hero, secondary workflow rail) is unchanged.
