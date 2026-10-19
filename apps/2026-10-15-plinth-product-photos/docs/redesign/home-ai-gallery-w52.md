# W52 — Home as AI gallery

## Structural change

Before W52, the observed working-tree home sequence in `HomeScreen.kt` was hero → credits explanation → scene strip (pre-change lines 147–162), so the explanatory card interrupted the visual path. The tool area followed as a two-column grid, and each `PlinthToolCard` ended with a visible description paragraph.

After W52, the sequence is hero → scene strip → tools → credits explanation: `HomeScreen.kt:147–154` places the hero before scenes, `HomeScreen.kt:174–212` renders the tool section, and `HomeScreen.kt:213–218` moves credits below the tools. Scene selection and tool callbacks remain unchanged. The cost labels still come from `StudioOperationCatalog` in `HomeScreen.kt:176–207`; no prices were introduced.

Scene cards now take 62% of the available strip width (`HomeSceneStrip.kt:109–110`) and retain start snapping (`HomeSceneStrip.kt:126–133`). Their media plane is 156 dp high and the category label sits over a bottom gradient (`HomeSceneStrip.kt:238–267`) instead of appearing in a caption stack below a smaller thumbnail.

Tool cards use their artwork as the card face, with title and the existing cost label overlaid at the bottom (`PlinthToolCard.kt:33–89`). The description is excluded from visible card text and included in a localized accessibility label alongside title and cost (`PlinthToolCard.kt:39–56`; `res/values/strings_home.xml` and `res/values-ru/strings_home.xml:14`, `home_tool_accessibility_description`). The wallet action remains available through the moved `CreditsExplanation` (`HomeScreen.kt:213–218`; `CreditsExplanation.kt:24–51`).

## Verification

- `node scripts/verify.mjs --slot plinth-product-photos --only ui` — `uiux.text-layout` and `locale.hardcoded` pass.
- A focused structural assertion confirmed the hero/scenes/tools/credits order, 62% scene width, 156 dp media height, category overlay, and that tool descriptions are not rendered as card-face text.
- `node scripts/verify.mjs --slot plinth-product-photos --build` — `assembleDebug` passed in 69,163 ms. The overall verifier exited 1 because `slot.device`'s recorded build digest predates this source change; `pack.inventory` and `pack.media` are UNVERIFIED because no `store/assets` exist yet. No device record or release approval was created or changed.
