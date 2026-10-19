# W46 — Plinth visual modernization

**Scope:** visual identity and shell treatment only; no pricing, release approval, signing or device pass.
**Mark:** Lightfold geometry is unchanged. Its launcher colours now follow the new identity.

## Research — what reads as a current creative product

**ON RECORD:** the Photoroom Play listing describes turning one product photo into lifestyle, flat-lay,
on-model and batch outputs while preserving product details. Canva's Play listing foregrounds templates
and multiple visual formats. Pixelcut's official site calls itself a creative workspace; its queried Play
listing was unavailable during this pass, so no Play-listing-specific Pixelcut claim is made. The prior
slot teardown records Photoroom's tool-grid/Start-from-Photo entry and Pixelcut's image-rich tool cards
(`docs/redesign/competitor-teardown.md`, §§1.1–1.2).

**INFERRED, not screenshot measurement:** the useful common thread is to make the work product the
largest element, let visual choices carry more meaning than explanatory text, group tools as image-led
cards, and keep the persistent navigation/actions quiet but unmistakable. A strong, high-contrast action
shape can anchor a mostly neutral workspace without making every surface compete. Store copy and the
prior teardown do **not** establish the competitors' exact type sizes, palette, button radii or Android
chrome heights; those details remain **UNVERIFIED** here. No install/download figures are used.

Sources read 2026-10-17: [Photoroom on Google Play](https://play.google.com/store/apps/details?id=com.photoroom.app&hl=en-US),
[Canva on Google Play](https://play.google.com/store/apps/details?id=com.canva.editor&hl=en-US),
[Pixelcut official site](https://www.pixelcut.ai/). The app-page claims above are listing copy, not
independent install, screenshot or in-app observations.

## Diagnosis — measured before the pass

- `store/studio.json` originally declared graphite `#212627`, travertine paper `#E8E1D5`, clay
  `#92705D`/`#62483D`, and verdigris `#315B54`/`#668A81` (the palette object at the start of this pass,
  lines 9–23). This is the warm stone/aged-metal vocabulary the owner called dated.
- The shared hero `PlinthStage` used a 10 dp shadow and a three-stop gradient from
  `surfaceContainerHigh` through `secondaryContainer` to `surface` (baseline `PlinthStage.kt:26–33`).
  That made a large hero field look like a decorative card rather than a clean image plane.
- Before reordering, the home credit explainer occupied `HomeScreen.kt:142–148` and the photo hero
  followed at `:149–155`; the hero image used 1.35 at `:303`, and the two primary CTAs were ordinary
  Material `Button`s at `:288` and `:328`. Tool cards used 1.35 artwork at
  `PlinthToolCard.kt:56`, then stacked title, cost and description in the card body.
- The bottom bar had a 72 dp minimum row at `PlinthNavBar.kt:52` and a full-width 1 dp divider at
  `:43–48`; its selected tab was indicated only by icon/label tint. The balance chip and top bar sat on
  the page background rather than a distinct tonal shell (`PlinthTopBar.kt:46`, `TopAppBar` colors).

## Palette — before → after

| Studio role | Before | New identity |
|---|---|---|
| `ink` | `#212627` | `#111816` |
| `ink_soft` | `#303637` | `#1A2420` |
| `ink_lift` | `#424848` | `#2B3631` |
| `amber` | `#92705D` | `#B9E84B` |
| `amber_deep` | `#62483D` | `#354A12` |
| `amber_pale` | `#E0C8B9` | `#E7F3BC` |
| `accent_deep` | `#315B54` | `#21463D` |
| `paper` | `#E8E1D5` | `#F4F7F2` |
| `muted` | `#625E56` | `#4E5B54` |
| `bezel` | `#080B0C` | `#070C09` |
| `bezel_rim` | `#89877E` | `#98A49C` |
| `lane` | `#668A81` | `#96C93A` |
| `error` *(new)* | — | `#B3261E` |
| `error_deep` *(new)* | — | `#8C1D18` |
| `error_pale` *(new)* | — | `#F9DEDC` |

The direction is cool white/green-black with a signal-lime action and deep pine support: not purple/white
and not cream/terracotta. The three added error roles keep error semantics distinct from the brand accent.
The source-of-truth palette is `store/studio.json:9–24` and `ui/theme/Color.kt:5–32`; both light and
dark `Theme.kt` schemes use it.

### Contrast measurements

Measured from the actual `Color.kt` literals and `Theme.kt` scheme assignments using the WCAG sRGB
relative-luminance formula. The five `surfaceContainer*` steps were each checked, not sampled:

| Foreground | Light, minimum across five containers | Dark, minimum across five containers |
|---|---:|---:|
| `onSurface` | **12.49:1** | **6.41:1** |
| `onSurfaceVariant` | **4.94:1** | **6.41:1** |
| `outline` (non-text boundary) | **4.94:1** | **3.22:1** |

The factory palette check independently measures **24 on-X/X pairs** across both schemes; its weakest
pair is light `onError`/`error` at **5.03:1**. Signal-lime `#B9E84B` against ink `#111816` measures
**12.61:1**. Contrast is whole-pass; no secondary text role was left on an unmeasured lighter container.
These are source-color measurements, not rendered-pixel or dynamic-color claims.

## Applied

- Replaced the broad gradient/shadow hero stage with a single `surfaceContainerLow` tonal plane; the
  statistics inset steps up to `surfaceContainerHighest` (`PlinthStage.kt:18–60`). Recipe gradients
  remain bounded to scene thumbnails because they represent the selected recipe; the animated gradient
  remains only in the active image-forming state.
- Put the home photo hero ahead of the credit explainer (`HomeScreen.kt:142–155`), increased its image
  height by changing its ratio from 1.35 to 1.2, and made the primary home actions pill-shaped through
  the theme's `small = CircleShape` token (`Theme.kt:17–22,119`).
- Increased tool artwork height at a fixed card width by about **17%** (1.35 → 1.15 aspect ratio),
  tightened the title/cost into one row, and kept the description to a readable two-line secondary tier
  (`PlinthToolCard.kt:48–78`).
- Retoned the top bar and navigation rail/bar onto `surfaceContainerLow`. Removed the bottom hairline,
  added a tonal selected-tab capsule, raised the compact bar's minimum from 72 to 76 dp, and kept the
  56 dp create FAB on the high-contrast primary (`PlinthTopBar.kt:28–50`,
  `PlinthNavBar.kt:33–68,80–119`).
- Mapped all five `surfaceContainer*` levels, `surfaceDim`, `surfaceBright`, errors and inverse roles in
  both schemes (`Theme.kt:23–101`). The product's paper/graphite/travertine background options and
  sample-object colours were moved to `PlinthProductPalette`; their actual image-output colours remain
  unchanged.
- Kept the Lightfold paths intact and changed only launcher colours to declared palette roles. The
  monochrome mark follows the new paper tone; the in-app mark uses a theme attribute, not hard-coded UI
  icon fills.

## Claim ledger

| Claim | Label | Evidence / limit |
|---|---|---|
| Competitor listings put transformed imagery, templates and multiple output formats in the product story | `ON RECORD` | Official Play listing copy for Photoroom and Canva fetched 2026-10-17; Pixelcut source is its official site. No installs or screenshot pixels inferred. |
| Image-first hierarchy, tonal chrome and one vivid action should feel more like a creative workspace | `INFERRED` | Derived from the above positioning and the prior slot teardown; exact competitor radii, type scale and dp chrome heights are not established. |
| Previous Plinth palette and gradient hero treatment were dated and dominated the first visual surface | `MEASURED` | Starting `studio.json` palette and staged `PlinthStage.kt:26–33`; owner feedback in `docs/owner-review-2026-10-16.md`. |
| The new palette stays within WCAG AA for measured ordinary text pairs and the complete surface ladder | `MEASURED` | Python source parser/WCAG relative-luminance measurement above; `node scripts/verify.mjs --slot plinth-product-photos` reports `palette.contrast` PASS (24 pairs, minimum 5.03:1). |
| New shell, button shape and image hierarchy look contemporary on a real device | `UNVERIFIED` | Requires the separately assigned on-device review; source verification does not establish rendered appearance. |
| The modified Android sources compile | `UNVERIFIED` | No Gradle build was run; `verify.mjs` was run without `--build`. |
| The current source revision still matches the existing device-verification record | `FAIL` | `slot.device` reports the recorded build digest is stale. No device record was changed; device pass is out of scope. |

## Verification

`node scripts/verify.mjs --slot plinth-product-photos` completed source-only: **122 PASS, 3 UNVERIFIED,
4 SKIP, 1 FAIL**. All palette checks passed, including palette-to-theme agreement, contrast, hue
structure, duplicate colours and the disliked-colour volume. The sole failure is `slot.device`, because
the pre-existing `device-verification.json` digest does not match the current source tree. Store media
is absent and `assembleDebug` is **UNVERIFIED** because `--build` was not passed. No release action,
approval, commit or device pass was performed.
