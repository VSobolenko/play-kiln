# W63 - Static Home scene thumbnails

## Change

`HomeSceneStrip.kt` now uses a `Box` background with a remembered vertical
gradient made from the selected recipe's `colorArgb` and `color2Argb`.
When the second color is absent, both stops use the primary color.
The gradient intentionally omits the recipe's angle, horizon line and platform
geometry: these cards are category swatches, not full scene renderings.

Removed item placement/fade animations, press-scale animation, animated container
color and the explicit graphics layer. The clickable `Surface` retains its
standard interaction feedback. The category label still overlays a scrim whose
brush is now remembered by theme scrim color.

Start snapping, stable category keys, localized overlay labels and
`onOpenScene(preview.category)` are retained. Catalogue loading remains in
`LaunchedEffect(context)` with remembered state and IO parsing, including its
existing explicit error handling.

Chosen: cached recipe-color fills. Rejected: retaining procedural geometry or
rasterizing it into a new thumbnail cache. The former leaves the requested draw
work in the strip; the latter adds bitmap allocation/cache management when the
brief permits simple color swatches.

## Observed checks

The source-contract check below failed before the edit with:

```text
FAIL static-thumbnail contract: animateColorAsState, animateFloatAsState, animateItem, Canvas, graphicsLayer
```

After the edit it reported `PASS static-thumbnail contract`.
This checks the requested absence of rendering/animation APIs, not frame timing.

```powershell
$path = 'apps\2026-10-15-plinth-product-photos\app\src\main\java\com\gexetr\plinthproductphotos\ui\home\HomeSceneStrip.kt'
$source = Get-Content -Raw $path
$forbidden = [regex]::Matches($source, '\b(Canvas|animateItem|graphicsLayer|animateFloatAsState|animateColorAsState)\b') |
    ForEach-Object Value | Sort-Object -Unique
if ($forbidden) {
    Write-Output ('FAIL static-thumbnail contract: ' + ($forbidden -join ', '))
    exit 1
}
Write-Output 'PASS static-thumbnail contract'
```

A separate source-preservation check reported
`PASS preserved snap/category/click/cache source contracts`; it checked the
existing snap position, stable key, callback, category text, remembered state,
effect, IO dispatcher and the new remembered background brush.

No Gradle invocation, device run or frame-time measurement was performed.
The W62 ledger was read with `node scripts/codex-run.mjs list` and reported
`DONE plinth-w62-studio-scroll`; that is a ledger observation, not evidence that
W63 compiles. Compilation is left to the integrated worker handoff rather than
starting an optional build from this bounded edit.
`HomeScreen.kt`, palette and release-approval files were not edited by W63.

## Claim ledger

Paths below are relative to this slot; `HomeSceneStrip.kt` is under
`app\src\main\java\com\gexetr\plinthproductphotos\ui\home\`.

| Claim | Label | Evidence |
|---|---|---|
| No thumbnail Canvas, item animation, explicit graphics layer or animated press state remains | MEASURED | Source-contract command above: failed before, passed after |
| Recipe colors feed a remembered Box background | MEASURED | `HomeSceneStrip.kt:234-240` |
| Snap-to-start and scene callback remain | MEASURED | `HomeSceneStrip.kt:108-125` |
| Category overlay remains; scrim brush is cached | MEASURED | `HomeSceneStrip.kt:187-224` |
| Parsed catalogue state/effect remain outside the item renderer | MEASURED | `HomeSceneStrip.kt:70-92` |
| W63 compiles and preserves device interactions | UNVERIFIED | Integrator: `node scripts/verify.mjs --slot plinth-product-photos --build`, then exercise Home scrolling and scene selection on device |
| Scroll frame times improve | UNVERIFIED | Compare frame timing on the same device/build scenario before and after; source removal alone is not a benchmark |
