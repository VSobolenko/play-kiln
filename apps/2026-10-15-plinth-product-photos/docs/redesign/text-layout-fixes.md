# W13: text layout fixes

Scope: `PlinthCostChip.kt`, `PlinthNavBar.kt`, and `HomeSceneStrip.kt`.
Standard: `docs/ui-ux-standards.md:70-106`, "Text must be able to wrap and to grow".
All Kotlin paths below are relative to
`app/src/main/java/com/gexetr/plinthproductphotos/ui/`.

## Findings and changes

| Finding (original line) | Before | After (current line) | Extent / visual intent |
|---|---|---|---|
| `components/PlinthCostChip.kt:44` | `maxLines = 1,` without overflow | `maxLines = 1, overflow = TextOverflow.Ellipsis,` at 45-46 | Keeps the compact, single-line chip. Existing padding and natural text measurement remain; a constrained label signals truncation instead of cutting a glyph. |
| `components/PlinthNavBar.kt:146` | `.height(48.dp)` | `.heightIn(min = 48.dp)` at 149 | Keeps the 48 dp touch-target floor without capping the icon/label stack. |
| `components/PlinthNavBar.kt:161` | `maxLines = 1,` without overflow | `maxLines = 1, overflow = TextOverflow.Ellipsis,` at 164-165 | Keeps equal-width, single-line navigation labels and makes any truncation visible. Typography and selectable semantics are unchanged. |
| `home/HomeSceneStrip.kt:202` | `Modifier.size(width = 148.dp, height = 176.dp)` | `.widthIn(min = 148.dp, max = maxCardWidth).heightIn(min = 176.dp)` at 207-208; `maxCardWidth = 148.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)` at 202 | Preserves 148 dp width at font scale 1.0, increases the available width with the font scale, and makes 176 dp a height floor. Keeps the thumbnail's existing 1.25 aspect ratio and the title's two-line ellipsis. |

The nav item's enclosing Row also used `.height(48.dp)` and its Box used
`.height(104.dp)`. Leaving either would retain an ancestor cap. They now use
`.heightIn(min = 48.dp)` and `.heightIn(min = 104.dp)` respectively
(`PlinthNavBar.kt:52-58`). The Row reserves the existing 56 dp FAB band using
`.padding(top = 56.dp)`: default geometry remains 56 + 48 = 104 dp, while a
taller label stack can increase the shell height without entering the FAB band.
The decorative 1 dp divider and 56 dp icon-only FAB remain unchanged.

A bare width floor was rejected for the scene card: LazyRow measures horizontally
without a finite maximum, while the thumbnail uses `fillMaxWidth().aspectRatio(...)`.
A finite, font-scale-dependent maximum preserves a deliberate card size and gives
that image a bounded width. A constant maximum was rejected because it would
reintroduce the same large-font cap. No font shrinking, `softWrap = false`, colour,
resource, or behavior changes were made.

## Russian locale at 1.3x: source review, not a rendered measurement

Read `res/values-ru/strings_shell.xml:3-5`: "Студия", "Библиотека", "Ещё";
`res/values-ru/strings_home.xml:43,46-51` includes `%1$d кр.`, the composed
`%1$s · %2$s` card title, and "Сгенерированная сцена".
The chip remains naturally measured; the navigation labels remain weighted with
explicit ellipsis and no fixed vertical cap. At 1.3x, the card's width range is
148-192.4 dp, not a hard 148 dp, and its height can exceed 176 dp as its thumbnail
and two title lines need space. Existing title wrapping remains enabled; a long
category/kind combination still has the intentional two-line ellipsis.

`ui/theme/Type.kt` declares 16 sp line heights for labelSmall/labelMedium and
20 sp for labelLarge. The linear 1.3x sanity calculation gives 20.8 sp-equivalent
navigation line height plus its 20 dp icon, below the 48 dp floor; this is only
layout reasoning, not Android font/glyph measurement. Actual Russian label fit,
font metrics, and screenshot appearance at 1.3x remain UNVERIFIED. The orchestrator
must compile and render the current sources on a device or preview to decide them.

## Source-only verification

Command, before and after:

```text
node scripts/verify.mjs --slot plinth-product-photos
```

Before row (verbatim):

```text
  FAIL       uiux.text-layout                apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/components/PlinthCostChip.kt:44: Text clamps to `maxLines = 1` with no `overflow`, which is `TextOverflow.Clip`, so a truncated label shows a cut glyph with no indication that there is more (see docs/ui-ux-standards.md, "Text must be able to wrap and to grow"); apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/components/PlinthNavBar.kt:146: Column sets a fixed extent (height(48.dp)) on an element that holds text, so at a large font scale the text can only wrap inside it — use a flexible extent and let the typography scale (see docs/ui-ux-standards.md, "Text must be able to wrap and to grow"); apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/components/PlinthNavBar.kt:161: Text clamps to `maxLines = 1` with no `overflow`, which is `TextOverflow.Clip`, so a truncated label shows a cut glyph with no indication that there is more (see docs/ui-ux-standards.md, "Text must be able to wrap and to grow"); apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/home/HomeSceneStrip.kt:202: Surface sets a fixed extent (size(width = 148.dp)) on an element that holds text, so at a large font scale the text can only wrap inside it — use a flexible extent and let the typography scale (see docs/ui-ux-standards.md, "Text must be able to wrap and to grow")
```

After row (verbatim):

```text
  PASS       uiux.text-layout                no fixed extent on a text-bearing element and no maxLines clamp without an overflow in 150 Kotlin file(s)
```

Before: `120 PASS, 3 UNVERIFIED, 4 SKIP, 3 FAIL`, exit 1.
After: `122 PASS, 3 UNVERIFIED, 4 SKIP, 1 FAIL`, exit 1.
No previously passing check became failing. The pre-existing `slot.device` failure
remains: the recorded build digest is stale. The baseline `uiux.a11y` failure in
`StudioRoute.kt` also disappeared during concurrent work; it is not credited to W13.
The three UNVERIFIED rows remain `pack.inventory`, `pack.media`, and `build`.

Captured outputs:

- Before: `.kiln/scratch/copilot-plinth-w13-text-layout/1792185465355-copilot-tool-output-46732-595a6598-0fdf-49ab-a979-4b520d2dc7dc.txt`
- After: `.kiln/scratch/copilot-plinth-w13-text-layout/verify-after.txt`

No Gradle command was run. I did not compile. No commit or index mutation was made.

## Claim ledger

| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| Four reported text-layout findings no longer appear | MEASURED | `node scripts/verify.mjs --slot plinth-product-photos`: after row quoted above |
| Chip and bottom-nav clamps explicitly use ellipsis | MEASURED | `components/PlinthCostChip.kt:45-46`; `components/PlinthNavBar.kt:164-165` |
| Navigation's item and enclosing extents are flexible | MEASURED | `components/PlinthNavBar.kt:52-58,149` |
| Card width responds to font scale and height is a floor | MEASURED | `home/HomeSceneStrip.kt:202-208` |
| Overall source-only report has fewer failures, no new failing row | MEASURED | Same verification command: 120 PASS / 3 FAIL before; 122 PASS / 1 FAIL after |
| Russian 1.3x runtime appearance is acceptable | UNVERIFIED | Orchestrator must render current sources at Russian locale / font scale 1.3 on device or preview |
| Current Kotlin sources compile | UNVERIFIED | Orchestrator owns `node scripts/verify.mjs --slot plinth-product-photos --build`; not run by W13 |
