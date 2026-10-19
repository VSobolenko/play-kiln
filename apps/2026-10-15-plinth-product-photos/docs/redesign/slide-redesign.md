# W26 - Image-led slide template browsing

Implements item 7 of `modern-ai-android-2026.md` section 3. Changes are limited to
`ui/slide/SlideKitScreen.kt`, English/Russian `strings_slide.xml`, and this note.
The pre-existing `PlinthScaffold` integration was preserved.

## Browsing and selection

The template picker is a keyed Foundation `LazyRow` of image-led cards, replacing
the full-width label-only buttons. Cards start at 144 dp wide and scale with the
system font size; titles wrap without line clamps. Foundation start snapping,
`animateItem()`, an animated selected border, and a selected-item scroll effect
provide bounded, user-driven motion. There is no auto-play or new dependency.
Snapper and a pager dependency were not added: native lazy-list snapping serves
this dense multi-card browser without changing the build.

Swiping only browses; tapping a card changes the current draft's template while
preserving its text map. The group exposes radio-button selection semantics and
localized selected/unselected descriptions. Switching slides reveals the chosen
template and scrolls the series preview toward that slide. The series preview
also uses a snapping keyed `LazyRow`; its cards start at 216 dp and scale with
font size. Preview labels use their rendered plans rather than newer draft data.

## Actual layout samples, not stock illustrations

Each composed template card calls the existing `SlideLayoutEngine.renderSeries`
and `SlidePixelRenderer` on `Dispatchers.Default`, using the seller's input
images, saved background, style, and effective marketplace rules. Localized
field labels stand in for sample text, filtered to the template's actual slots.
The UI explicitly says that sample text is not exported. Only the separate
existing draft-series rendering feeds the export callback.

Samples are reduced to 222 x 296 pixels before bitmap creation; their bitmaps
are recycled on disposal. Input-keyed composition resets sample state when the
images, background, style, rules, or localized labels change. Cancellation is
checked around rendering and is not caught as an error. Sample layout/text
failures appear as localized unavailable states that can grow vertically,
instead of becoming blank images or successful placeholders. A sample may be
unavailable when its illustrative field labels violate a marketplace limit or
its background requirement; the draft's actual text/render checks remain
independent, and a failed sample does not silently disable template selection.

Samples render on demand as lazy items enter composition. Re-entering a disposed
item can render it again; no persistent cache or performance claim is made.

## Observed validation and limits

`node scripts\check-locales.mjs --slot plinth-product-photos` exited 0:
954 keys in the default and Russian locale, matching format arguments and
plural categories in the final run (the earlier run had 921 keys; concurrent
slot work added other resources).

`node scripts\verify.mjs --slot plinth-product-photos --json` initially caught
the new series card's fixed `width(216.dp)` through `uiux.text-layout`. Changing
that to a font-scaled width removed that finding. The final source-only report
exited 1: **122 PASS, 1 FAIL, 3 UNVERIFIED, 4 SKIP**. The remaining failure is
`slot.device`: the recorded device run's build digest does not describe the
edited sources. `pack.inventory` and `pack.media` are UNVERIFIED because
`store/assets` is absent; `build` is UNVERIFIED because no build was requested.
No gate or approval record was edited to clear those states.

Reports: `.kiln\scratch\copilot-plinth-w26-slide\verify-after.json` (before the
text-width correction) and `verify-final.json` (after it). Scoped
`git diff --check` exited 0.

**Comp / Compose runtime: UNVERIFIED.** No Gradle, compilation, device exercise,
screenshots, staging, commit, signing, release, or publication was performed.
The orchestrator must run its authorized
`node scripts\verify.mjs --slot plinth-product-photos --build` and device pass
to decide compatibility and rendered behavior. Exercise all eight templates,
rapid flings/taps, slide switching/addition, long and prohibited draft text,
unavailable samples, export isolation from sample text, and large font scales.
Rendering latency, frame pacing, bitmap lifetime under rapid scrolling, and
visual density have not been measured on-device.

Elapsed wall-clock time: approximately 6 minutes, ending at 01:11 +03:00.

## Claim ledger

Paths below are relative to the slot's
`app/src/main/java/com/gexetr/plinthproductphotos/` unless a command is named.

| Claim | Label | Evidence |
|---|---|---|
| Label-only template buttons were replaced by selectable preview cards with native snapping and item animation | MEASURED | `ui/slide/SlideKitScreen.kt:235-293` |
| Selected-template and series-preview scrolling is explicit, not an auto-advance timer | MEASURED | `ui/slide/SlideKitScreen.kt:160-172` |
| Samples use the existing layout/pixel renderers and seller inputs, not invented template graphics | MEASURED | `ui/slide/SlideKitScreen.kt:438-465` |
| Sample text is isolated from the draft/export flow | MEASURED | Sample-only draft at `ui/slide/SlideKitScreen.kt:451`; existing series rendering at `:132` and export callback at `:422` |
| Sample errors are visible and allocated sample bitmaps have disposal cleanup | MEASURED | `ui/slide/SlideKitScreen.kt:467-496` |
| Locale resources agree | MEASURED | `node scripts\check-locales.mjs --slot plinth-product-photos`: exit 0, 954 keys in the final run |
| Source verification is not fully green | MEASURED | `verify-final.json`: exit 1; stale `slot.device`, build/media UNVERIFIED |
| Compilation, Compose compatibility, runtime appearance and performance | UNVERIFIED | Orchestrator's authorized build command and device cases above |
