# W54 — visible AI presence

**Implemented:** 2026-10-17. **Scope:** the existing Compose shell, Studio hero, and result-forming surface. This pass does not change provider behavior, credit arithmetic, palette source data, or release inputs.

## Decision

Plinth now treats the output as the interface. Studio opens on one large, aspect-bound visual surface: the current generation while it is running, the delivered preview when available, the last saved result otherwise, then the selected product photo. Import and result actions stay attached to that surface. The operation catalogue follows it as a secondary horizontal tool shelf.

The generating state is not a spinner. `FormingCardPreview` draws a bounded tonal mesh, an aspect-sensitive morphing product silhouette, and a diagonal shimmer. The field stops when the result arrives and crossfades to the bitmap; a failed state stays in the same result-shaped container. When Android's animator-duration scale is zero, the infinite transitions are not composed, the arrival transition snaps, and the same generative field remains visible at a deliberate static phase.

Credit copy remains factual. Each tool reads its price from the current server configuration, pairs it with the current shared balance when known, and refuses commitment when the quote is absent or stale. The existing confirmation still rechecks the displayed quote before submission and states that reservation settles only after verified delivery.

## Stack choice

No Haze, blur, Lottie, or other visual dependency was added. The measured catalogue already supplies Compose animation, drawing, Material 3 tonal roles, and Navigation 3. The current redesign research explicitly recommends tone plus translucency as Plinth's glass language and records real blur performance as unmeasured on the target device. Adding Haze would therefore add a rendering dependency without resolving a demonstrated product gap.

Chrome and stage depth use only `MaterialTheme.colorScheme` roles. The existing teal identity values in `store/studio.json` and `Color.kt` remain unchanged. Compact bars and the wide rail share one `plinthTonalGlass` treatment: a 0.74-alpha base, palette-derived diagonal tonal lift and teal field, an edge-local scrim, plus paired boundary/highlight lines. Their Material containers are transparent so the layered field is the chrome rather than a near-opaque color painted over it. Selected navigation uses the existing primary-container role. `PlinthStage` uses a bounded palette-derived field and no shadow.

## Reduced motion

Compose's public `MotionDurationScale` is a coroutine-context element used by animation machinery; the pinned API does not expose a public composition-local getter that can directly gate this custom infinite drawing branch. `FormingCardPreview` therefore observes Android's public `Settings.Global.ANIMATOR_DURATION_SCALE` URI. A zero value removes both infinite transitions and selects `snap()` for the finite fade/crossfade. Nonzero values retain the normal Compose specs, which continue to receive platform duration scaling. The observer is disposed with the composition.

## Evidence and limits

| Claim | Label | Evidence |
|---|---|---|
| The generation hero contains no `CircularProgressIndicator` or `LinearProgressIndicator` | MEASURED | `rg -n "CircularProgressIndicator|LinearProgressIndicator" app/src/main/java/com/gexetr/plinthproductphotos/ui/autocard/FormingCardPreview.kt app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioRoute.kt` |
| The mesh and shimmer are bound to the requested output aspect | MEASURED | `FormingCardPreview.kt`: `aspectRatio(aspectRatio)`, computed canvas `aspect`/`portraitWeight`, radial fields, morph, and sweep |
| Zero animator scale produces a static generative frame rather than an infinite transition | MEASURED | `FormingCardPreview.kt`: `rememberMotionEnabled`, `ANIMATOR_DURATION_SCALE`, `animated = ... && motionEnabled`, static `rememberUpdatedState` phases, and `snap()` arrival specs |
| Compact and wide chrome use the same layered tonal-glass drawing | MEASURED | `PlinthNavBar.kt`: `plinthTonalGlass` with `TOP`/`BOTTOM`/`END`; `PlinthTopBar.kt` and `NavigationRail` use transparent Material containers |
| Studio presents the visual result before photo selection, prompt, and operation tools | MEASURED | `StudioRoute.kt`: first `LazyColumn` item is the `PlinthStage` hero; the operation shelf follows the item and prompt sections |
| Tool costs come from the live configuration and unknown prices remain non-committable | MEASURED | `StudioRoute.kt`: `state.config?.ops?.get(...)`, `studioConfirmationPreview`, `studio_live_price_unknown`, and quote-match confirmation |
| The new motion is smooth and the chrome reads as premium on the target phone | UNVERIFIED | Requires the repository's later build and on-device review; this worker was explicitly limited to static verification |
