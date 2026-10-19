# W32 - Scene and forming-preview rhythm

Implements backlog item 2 in `modern-ai-android-2026.md`, using the existing
Compose Foundation, animation and Material 3 stack. No Maven dependency,
Snapper, Lottie, animation asset, spinner or invented percentage was added.

## Motion contract

| Surface | Treatment | Boundary |
|---|---|---|
| Home category strip | Remembered lazy-list state, Foundation fling snapping at `SnapPosition.Start`; keyed item entry fades over 220 ms and placement uses a spring. | Native scroll bounds still clamp the final items. Scrolling does not choose a category or launch a flow. |
| Home category press | Non-bouncy spring from 1 to 0.97 scale and a 180 ms theme-container colour transition; the existing Surface ripple remains. | Feedback belongs to the press, not a persistent selection. Clicking immediately hands off that card's category, without waiting for motion. |
| Forming preview | The actual result aspect ratio contains a themed product silhouette, contact plane and shadow. A 2200 ms highlight sweep runs only while busy and not failed. Sweep state is read in drawing rather than rebuilding the layout each frame. | Idle/missing and failed states are static, with the existing localized status/error message. The silhouette is illustrative, not a claim to have recognized the product. |
| Forming to result | A 320 ms eased whole-placeholder fade accompanies image opacity and a restrained 0.985-to-1 scale settle. The outgoing status is retained during its fade. | Ready requires a bitmap, no busy operation and no failure. No timer delays the result or its actions. |
| Image changes | A 240 ms Crossfade between decoded bitmaps while ready softens Original/Card changes. | Busy or failed imagery is not presented as a ready result. A decode retry clears the previous result bitmap before decoding again. |

The placeholder and image share the same clipped container; there is no animated
layout-height change or competing scroll animation. Existing bring-into-view,
category selection, recognition, pricing and result-confirmation logic are
unchanged. `AutoCardScreen.kt` changes only the bitmap reset on the preview decode
path.

Foundation snapping was chosen over a pager because the existing strip displays
several variable-width cards, including font-scale-dependent widths. A pager would
replace that browsing layout. Snapper duplicates the native fling job, and Lottie
would introduce an unneeded dependency/asset for this pass.

## Evidence and limits

Paths below are relative to this slot's
`app/src/main/java/com/gexetr/plinthproductphotos/` unless stated otherwise.

| Claim | Label | Evidence |
|---|---|---|
| Native snap, keyed entry and immediate category callback are wired. | MEASURED | `ui/home/HomeSceneStrip.kt:125-145`. |
| Home still maps categories, not all recipes, to cards. | MEASURED | `ui/home/HomeSceneStrip.kt:158-179`; catalog JSON reading reported 21 unique categories, 123 recipes and 21 verified procedural preview candidates. |
| Press feedback does not add a persistent category selection. | MEASURED | `ui/home/HomeSceneStrip.kt:207-239`; interaction state is `collectIsPressedAsState()`. |
| Ready excludes busy, missing bitmap and failure; the placeholder fades as a whole. | MEASURED | `ui/autocard/FormingCardPreview.kt:57-103,123-164`. |
| Decode retry clears the previous bitmap. | MEASURED | `ui/autocard/AutoCardScreen.kt:369-378`. |
| Kotlin compilation/API compatibility. | UNVERIFIED | No Gradle command was run, as required by the worker boundary. The orchestrator must run the slot's authorized build route. |
| Snapping feel, image transition, accessibility and frame pacing on a device. | UNVERIFIED | Requires the runtime observations below; no device run or animation-clock test was performed. |

The catalog measurement parsed `app/src/main/assets/autocard/scene-recipes.json`
with Node and checked category count, unique IDs and verified procedural-preview
coverage. It measures data availability, not rendered-card behavior. Source reads
and whitespace checks are not runtime tests or a compilation PASS.

## Runtime acceptance still required

1. Fling the Home strip at normal and enlarged text sizes, in both layout
   directions. Confirm reachable cards settle without clipped labels. Tap first,
   middle and last categories: exactly one callback must carry the tapped category;
   scrolling alone must not navigate.
2. Exercise busy -> decoded image, busy -> failure, missing bitmap, and retry.
   The result-sized silhouette must remain truthful, failures must stop the sweep,
   and no previous image may masquerade as the retried result.
3. Change scenes rapidly while composing, then toggle Original/Card while ready.
   Confirm the final image is the latest result and confirmation remains immediate;
   the animation must not impose an extra wait.
4. Check TalkBack, large fonts and the system's disabled-animation setting.
   Status text must remain legible without a fake progress number; no motion may
   be required to understand state. Inspect frame pacing during the sweep.

**Worker status:** source changes delivered; **Comp UNVERIFIED**. No Gradle,
dependency edits, commits, signing or release action. Runtime acceptance belongs
to the orchestrator's integration pass.
