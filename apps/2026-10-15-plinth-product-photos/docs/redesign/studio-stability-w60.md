# Studio stability — W60

## Root cause

`StudioViewModel.task()` sets `busy` for every task, including passive refreshes and local
import/save/apply work. `StudioRoute` previously used that broad flag to choose the hero's
content subtree. An `ON_RESUME` refresh therefore disposed the current Coil-backed artwork and
temporarily replaced it with `FormingCardPreview`, even though no image was being formed.

`FormingCardPreview` had the same identity problem internally: its bitmap subtree existed only
while `ready` was true. A `busy` transition after delivery removed and recreated the image. Its
skeleton also ran two independent infinite animations; the mesh animation invalidated all radial
gradients and silhouette geometry on every frame while the containing `LazyColumn` could scroll.

## Fix

- The Studio hero now enters forming mode only for a pending request without a result. Passive
  `busy` transitions leave the current photo/result subtree in place.
- A delivered bitmap stays composed across `busy` and failure overlay changes. The skeleton and
  image cross-fade through complementary alpha without replacing the bitmap node.
- The expensive mesh geometry is fixed. One low-frequency sweep remains as the progress cue, so
  scrolling no longer competes with two independent full-preview animations.
- Palette and navigation components are unchanged.

## Static verification

Gradle was intentionally not run; W60 assigns builds to the orchestrator. The following checks
were run from the repository root after the edit:

```powershell
rg -n "isForming|Keep the delivered bitmap|Keep the mesh geometry" `
  apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioRoute.kt `
  apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/autocard/FormingCardPreview.kt

rg -n "if \(state\.busy \|\| state\.result|Crossfade|forming-card-morph|forming-card-mesh" `
  apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioRoute.kt `
  apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/autocard/FormingCardPreview.kt
```

Expected evidence: the first command finds the three stabilization points. The second command
has no matches, proving the broad `busy` identity switch, conditional Crossfade, and second
infinite mesh animation are absent.
