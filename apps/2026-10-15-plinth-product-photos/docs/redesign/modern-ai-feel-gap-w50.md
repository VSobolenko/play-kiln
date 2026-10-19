# W50 - Why Plinth still feels dated: research and a structural apply wave

**Date:** 2026-10-17. **Status:** research and proposed work, not implementation.
**ON RECORD - owner brief:** usable is not enough; Photoroom and Picsart are the feel targets, and the gap may require large changes rather than bugfix polish. This assignment explicitly forbids Checkpoint 1.5 / release approval.

**Verdict - INFERRED:** the owner is right about a remaining structural gap. Plinth has acquired modern components, but its principal creation journeys still ask the seller to operate a sequence of forms. The next wave should replace that interaction grammar with **visible outcomes -> one persistent product canvas -> inspect and choose -> hand off**. Another palette, corner-radius or nav-height pass would leave the measured causes below intact.

## 1. Evidence boundary and prior work

`MEASURED` means a repository file or command read/run in this W50 session; `ON RECORD` means a named source says it; `INFERRED` is a conclusion from specified evidence; `UNVERIFIED` names an observation still required. Proposed requirements below are recommendations (`INFERRED`), not descriptions of shipped behavior.

**MEASURED:** this is a source spot-check, not a live-device inspection. No competitor app or Plinth APK was installed or operated in W50. No competitor screenshot was visually inspected or measured. HTML image tags and marketing copy are not screenshot evidence. All claims about current Android pixel density, colors, type sizes, transition smoothness, blur or comparative perceived quality remain **UNVERIFIED**. The orchestrator can decide those by recording comparable launch -> select photo -> create -> inspect -> save journeys on an attached device.

Paths below use these aliases, all under `apps\2026-10-15-plinth-product-photos\`:

| Alias | Base |
|---|---|
| `R\` | `docs\redesign\` |
| `U\` | `app\src\main\java\com\gexetr\plinthproductphotos\ui\` |
| `D\` | `app\src\main\java\com\gexetr\plinthproductphotos\data\` |
| `A\` | `app\src\main\assets\` |
| `V\`, `RU\` | `app\src\main\res\values\`, `app\src\main\res\values-ru\` |
| `IT\` | `app\src\androidTest\java\com\gexetr\plinthproductphotos\ui\` |
| `JT\` | `app\src\test\java\com\gexetr\plinthproductphotos\` |

**MEASURED - prior research read before this diagnosis:**

| Existing document | What W50 carries forward / corrects |
|---|---|
| `R\competitor-teardown.md:276-334` | Keep its hero/scene/result ambition. Re-audit implementation instead of repeating its historical "no shell" findings. |
| `R\ai-visual-language.md:165-197,439-481` | Keep "output is the interface" and one-photo flow. Its historical missing theme/wiring findings are not today's source state. Its "costs nothing" statement about translucency is not a measured rendering-cost result and is not adopted here. |
| `R\production-android-stack-2026.md:28,38,65-80` | Retain its caution about public `MotionScheme` availability and Haze. Its "no shared transitions", Nav3 upgrade and "Telephoto new dependency" recommendations have been overtaken by current source. |
| `R\modern-ai-android-2026.md:30-72` | Use existing Compose/Coil first. Its assertion that pinned M3 exposes public `MotionScheme` conflicts with the later version-specific research at `production-android-stack-2026.md:28`; do not base the wave on it. |
| `R\visual-modern-w46.md:84-101` | Hero ordering, larger artwork and tonal surfaces already received attention; do not sell them as new structural work. |
| `R\palette-refine-w48.md:3-13,89-108` | Lime was owner-rejected; muted teal is the current direction, not an owner acceptance. Keep it fixed during this wave. |

## 2. External modernity signals, not invented pixels

### Sources actually read on 2026-10-17

| ID | Source and evidence obtained |
|---|---|
| E1 | [Photoroom Android Play listing](https://play.google.com/store/apps/details?id=com.photoroom.app&hl=en_US). **ON RECORD:** describes single-photo lifestyle, flat-lay and on-model outputs, preserving product details, batch editing, marketplace resizing and prompt edits. These are vendor claims, not independently verified fidelity or speed. |
| E2 | [Photoroom: Quick guide to the UI (Android)](https://help.photoroom.com/en/articles/13672978-quick-guide-to-the-photoroom-ui-android). **ON RECORD:** article HTML body, not only its title, was read via raw fetch at character offsets 30000 and 47000. Head metadata says modified 2026-02-17. Home documents template/tool/design search, suggested features, templates, photo upload and recent work. Main editor documents an image, editing options and settings for the selected layer. AI editors document generated previews, per-image context menus with variants, and a prompt/next-generation arrow. |
| E3 | [Photoroom: Create a variation (Android)](https://help.photoroom.com/en/articles/15457998-create-a-variation-of-an-ai-image-android). **ON RECORD:** raw article body read at offset 30000; metadata says modified 2026-07-05. Select Variation at the bottom of an image or in its context menu; it generates another version; select image -> Save. The article says AI credits are required and variation is random. |
| E4 | [Picsart Android Play listing](https://play.google.com/store/apps/details?id=com.picsart.studio&hl=en_US). **ON RECORD:** describes templates, background/object removal, AI Playground, Gen AI Studio, and Aura's create/edit/refine conversation. This establishes the advertised creation/refinement vocabulary, not an observed Android launch layout. |
| E5 | [Picsart: AI Background](https://picsart.com/ai-background/). **ON RECORD:** public web product page says "Upload an image and describe the background you want", offers background styles/pre-generated options, contextual suggestions without typing, and iterating styles for the same product. It contains example-image sections for product listings and consistent styles. This is a web surface, not proof of Android control placement. |
| E6 | [Android: shared elements with navigation](https://developer.android.com/develop/ui/compose/animation/shared-elements/navigation). **ON RECORD:** Nav3 setup wraps `NavDisplay` in `SharedTransitionLayout` and applies matching keyed `sharedElement` modifiers at both endpoints using the animated-content scope. A host wrapper alone is insufficient. |

**MEASURED - retrieval limitations:** simplified Photoroom help fetches returned titles only; raw HTML exposed the article bodies above. Picsart help requests for AI Background, navigation and Remove BG returned HTTP 403. Search synthesis supplied inconsistent article URLs; those unconfirmed synthesized quotations are not load-bearing evidence here. An attempted Picsart general tutorial redirected to a 2014 gradient article, which is excluded as contemporary evidence. No optional Pixelcut claim is needed to reach the verdict.

### Pattern-to-decision matrix

| Signal | What the evidence really supports | Plinth decision / alternative rejected |
|---|---|---|
| Outcome-rich entry and media density | **ON RECORD:** E2 names templates, suggestions, upload and recents; E5 presents example-led scene use cases. **UNVERIFIED:** current Android image-area percentage, number of visible tiles or exact carousel geometry for either app. | **INFERRED:** show genuinely different attainable outcomes, not the same original repeated under different tool labels. Reject a claimed competitor density ratio without screenshots. |
| Create affordance | **ON RECORD:** E2 documents starting a design by upload. **UNVERIFIED:** Picsart's current central `+` placement; the help fetch was blocked. | **INFERRED:** central creation is a Plinth teardown requirement, not a verified Photoroom/Picsart clone. Make its route immediate and preserve the selected intent. |
| Scene gallery and refinement | **ON RECORD:** E5 explicitly supports pre-generated styles, suggestions and switching styles on the same product; E2/E3 attach further generation to an existing image. | **INFERRED:** presets must depict a result and lead into a persistent product workspace. Reject an unlabelled swatch masquerading as a generated scene preview. |
| Result stage and variants | **ON RECORD:** E2 has previews and per-image variant menus; E3 has variation -> review -> Save. E4 advertises create/edit/refine. | **INFERRED:** creation does not end at "request completed"; give the output inspection, sibling choices and a handoff. Reject copying all of Picsart's unrelated social/video functionality. |
| Quiet chrome and typography | **ON RECORD:** E2 separates the image, editing tools and selected-layer settings. **UNVERIFIED:** competitor typefaces, sp scale, nav heights, radii and amount of visible chrome. | **INFERRED:** keep one canvas and reveal relevant tools contextually. Do not claim that serif/sans choice itself dates Plinth. |
| Forming preview and motion | **UNVERIFIED:** neither app's live loading choreography, shared-element implementation or animation durations was observed. E6 establishes an Android implementation mechanism, not a competitor's internals. | **INFERRED:** continuity of the same photo matters more than decorative motion; real phases should resolve in the same bounds. Reject fake intermediate generated images, fake percentages and minimum-wait animations. |
| Glass / blur | **UNVERIFIED:** no evidence here establishes either competitor's native backdrop blur or performance. | **INFERRED:** do not make glass the diagnosis or a dependency requirement; tonal overlays can support a canvas without proving blur is necessary. |
| Empty states | **ON RECORD:** E2's templates and E5's examples demonstrate ways to start from something visible; they do not establish the apps' current no-project screens. | **INFERRED:** Plinth should teach with a rights-cleared, labelled example outcome and a direct own-photo action. Reject claiming that either competitor has a particular observed empty state. |

## 3. Current source audit against the old backlog

**MEASURED:** source anchors below were read in this session. Numbers are source dp/sp/ratios, not rendered pixel measurements. The worktree contains pre-existing staged/unstaged work; these are local-source observations, not claims about an installed APK or HEAD.

| Teardown ambition | Current observation and evidence | Remaining gap / disposition |
|---|---|---|
| Hero not buried in prose (`R\competitor-teardown.md:305-313`) | `U\home\HomeScreen.kt:142-160` puts hero first, then `CreditsExplanation`, then scenes. `:275-342` shows a source-photo hero or a drawn empty plinth, with actions. `U\components\CreditsExplanation.kt:28-55` has a dismissible explanation with two text actions. | **INFERRED:** old "paragraph before first image" complaint is closed on Home. But the large hero sells the input, not an attainable transformed result; the explainer still interrupts the hero-to-gallery sequence. Move education out of the main visual discovery path without hiding the quote. |
| Scene strip | `U\home\HomeSceneStrip.kt:124-173` has a snapping `LazyRow`, animated items and one procedural recipe per category. `:247-297` draws a gradient, horizon and rounded platform, not product photography. `HomeScreen.kt:157-160` passes a category, not a selected recipe ID. | **INFERRED:** present but semantically incomplete. It is a category door with decorative recipe artwork, not "choose this look for my product". Preserve recipe intent through import. |
| Image-led tool gallery | `HomeScreen.kt:134-140` is already a two-column grid. `:200-204` passes `latestPhotoFile` to every tool. `U\components\PlinthToolCard.kt:48-78` has artwork at 1.15 ratio, title/cost and a two-line description; `:93-128` uses an icon for null/error or the supplied image. | **INFERRED:** repeated input images make the cards visually different labels for the same object, not demonstrations of distinct operations. More image area alone cannot fix content that does not show the difference. |
| Shared elements and motion | `U\PlayKilnApp.kt:316-346` now wraps NavDisplay with `SharedTransitionLayout`, passes its scope and sets forward/pop/predictive specs. `U\theme\PlinthMotion.kt:8-34` supplies 160/180 ms tokens and a photo-key helper. Exact UI-tree search `rg -n --glob '*.kt' 'sharedElement\|sharedBounds\|plinthPhotoSharedElementKey\|SharedTransition' ...\ui` returned the host wrapper/import/opt-in and helper only, no image endpoint modifiers. | **INFERRED:** engine provisioned, photo continuity not connected. Do not upgrade Nav3 again or rewrite transition timing as the main bet. **UNVERIFIED:** whether runtime navigation is smooth. |
| Spinner -> forming preview | `U\autocard\FormingCardPreview.kt:57-99,110-163` bounds the skeleton to result ratio, fades it and crossfades a ready bitmap. `AutoCardScreen.kt:538-558` and `PlayKilnApp.kt:1062-1069` use it. | **MEASURED:** old "AutoCard is a bare spinner" claim is obsolete. **INFERRED:** improve the subject continuity, not install another shimmer. The skeleton's generic rounded silhouette is not the seller's photo. |
| Actual server generation progress | `U\studio\StudioRoute.kt:156-207` keeps the selected source in the first stage; result is an inline later card at `:208-271`. Prompt and tool row are further down at `:356-455`; server phase and a linear indicator are last at `:458-488`. | **INFERRED:** the live AI path did not inherit the AutoCard forming-preview language. The user has to relate source, instructions, pending state and delivered image across scroll positions. This is a major restructuring target. |
| Centre Create / immediate import | `U\components\PlinthNavBar.kt:49-72` draws three tabs then appends a 56 dp FAB on the right. `U\PlayKilnApp.kt:279-282` pushes Import; `U\import\ImportScreen.kt:184-214` then asks the empty user to press the picker button. | **MEASURED:** there is a create affordance, but not the teardown's centre action or direct picker entry. **INFERRED:** the unnecessary landing/choice breaks initiation; centring alone would be a nit if the route stayed unchanged. |
| Dedicated result and handoff | `U\PlayKilnApp.kt:484-497,842-1084` has a real result destination, persistence, error states, before/after chips, Share and document save. `D\project\ProjectOutputRepository.kt:35-72` persists outputs and reloads latest AutoCard. | **MEASURED:** "no result screen / cannot save" is false today. **INFERRED:** it is still a scrolling result form, not a workbench with sibling outputs and contextual next actions. |
| Before/after | `AutoCardScreen.kt:523-537` and `PlayKilnApp.kt:1044-1060` use two `FilterChip`s. `U\editor\EditorScreen.kt:116-174` already has a constrained/adaptive preview-and-tools workspace. | **INFERRED:** comparison exists; replace chip-switching on the main creation/result path with direct inspection, not a claim that comparison is absent. Retain button/semantics alternatives to any drag gesture. Do not rebuild the local Editor merely to imitate this proposal. |
| Variants | `U\studio\StudioRoute.kt:311-351` already lists saved generated results with `openVariant`. `U\studio\StudioViewModel.kt:55-78` holds saved variants/selected output. `U\autocard\AutoCardResultsViewModel.kt:8-15` holds one pixel buffer per photo; `PlayKilnApp.kt:842-1084` renders one active output, no siblings rail. | **INFERRED:** variants exist in live Studio but are disconnected from the AutoCard result workbench. Do not invent multi-output provider support or call local preset previews paid generated variations. |
| Palette/type "still default" | `U\theme\Color.kt:5-31` declares teal and neutral ladders; `Theme.kt:23-101` maps both schemes' containers. `Type.kt:11-31` bundles Noto Serif Display for displayLarge/headlineLarge; `:49-115` supplies title/body/label roles. Tool-card titles use titleSmall, not those display roles (`PlinthToolCard.kt:65-69`). | **MEASURED:** missing theme wiring/default-font diagnosis is stale. **INFERRED:** hierarchy and image meaning, not another accent or blanket font replacement, are the useful next variables. |

## 4. Honest diagnosis: five structural causes

These are **INFERRED** causes of the owner's impression, grounded in section 3, not a timed comparative usability study:

1. **The gallery depicts inputs rather than possibilities.** Identical source imagery in tool cards, abstract scene stands and the empty plinth do not answer "what will my product become?" E2/E5 establish the contrasting template/example vocabulary. Changing teal or adding elevation does not change that information.
2. **The product is not a persistent workspace.** AutoCard stacks source preview, change-source action, comparison controls, result preview, category/attributes and scenes (`AutoCardScreen.kt:508-640`). Live Studio separately stacks source, result, item strip, saved variants, prompt, operations and status. Every feature may work while the whole still feels like a wizard assembled from panels.
3. **Creation intent gets diluted across doors.** Home scenes carry category only; Create reaches an empty Import screen before the picker; several operation cards lead to the general tools destination (`HomeScreen.kt:205-212`). E2/E3 instead document an image/tool/refinement relationship. The issue is losing context, not simply too many screens.
4. **Waiting and finishing use different visual grammars.** AutoCard has a forming preview, live Studio has bottom-of-list server status, and the result destination is another scroll form. The photo has no paired shared-element endpoints. A canvas should carry the transition from choice through waiting to inspection, without pretending the service streams pixels.
5. **A generated image is treated as a completion message instead of a decision object.** Comparison is chip-based; saved variants are elsewhere; the AutoCard result is singular. E2/E3 support image-anchored refinement and variant selection. Plinth needs a result workbench, not more success copy.

**Boundary - INFERRED:** preserve Plinth's seller-specific marketplace rules, original preservation, provenance and charging truth. E1's marketing fidelity claim is not permission to label Plinth's whole-image generation as pixel-preserving: `V\strings_studio_live.xml:4,39-41` explicitly warns that generated results can change the item and are kept as labelled EXTRA outputs. A polished UI must make that distinction easier, never erase it.

## 5. Big-bet apply wave: exactly five ranked work packages

**All five are proposed / INFERRED.** Rank is owner-visible impact, not dependency order. Effort S/M/L denotes scope, not a measured duration. These are substantial composition/interaction changes, not a claim that one cosmetic iteration will deliver competitor parity.

**File ownership rule:** lists are disjoint. Only WP5 edits `PlayKilnApp.kt`; WP4 builds its extracted destination in a new file and WP5 removes the old private implementation when integrating it. Shared resources get package-specific new XML files in both locales; no worker edits a generic shared `strings.xml`. New paths below are proposals, not claims they already exist. Unlisted files require a revised dispatch, not opportunistic edits.

### 1. Outcome-led Studio home, not a tool catalogue - L

**Change in feel:** opening Plinth becomes browsing believable product-photo outcomes and continuing real work, rather than reading labels beneath copies of the input photo.

**Own files:** `U\home\HomeScreen.kt`; `U\home\HomeSceneStrip.kt`; `U\home\HomeUiState.kt`; `U\home\HomeViewModel.kt`; `U\components\PlinthToolCard.kt`; new `V\strings_w50_discovery.xml` and `RU\strings_w50_discovery.xml`; new `A\w50-examples\manifest.json`, `source.webp`, `clean.webp`, `studio.webp`, `lifestyle.webp`; `IT\home\HomeScreenTest.kt`.

**Deliver:** an outcome-led first surface with own-photo create and resume; a compact scene gallery with meaningful names and result imagery; tool examples that differ by operation; actual saved output for resume instead of assuming the newest source is the finished work. Use original/rights-cleared sample photography and locally produced illustrative composites, recording rights, recipe IDs and whether each example is illustrative in the manifest. Never redistribute competitor imagery or imply a local illustration is a guaranteed AI result. Keep unavailable scenes visibly unavailable. Move the dismissible credit tutorial behind help/on-demand disclosure; preserve action-local cost and global balance.

**Libraries:** existing Coil, Foundation lazy/snap and M3; no new dependency. **MEASURED support:** grid and loader already exist (`HomeScreen.kt:134-204`, `PlinthToolCard.kt:93-128`). New work is media and hierarchy, not acquiring a carousel framework.

**Interface contract:** expose optional scene-ID intent alongside category on the existing Home callbacks, with null/default compatibility until WP5 wires it; the shared example manifest keys previews by stable recipe ID, never array position.

**Acceptance proposal:** first-run has distinct labelled example outcomes and own-photo action without a credit tutorial between them; returning state distinguishes source from finished output; tapping an example preserves its recipe after import; failed artwork has an explicit non-success state. Inspect EN/RU and large text rather than enforcing an invented competitor card count.

**Reject:** a second palette pass, another hero aspect-ratio tweak, bigger icons on repeated input photos, and stock images with no operation/recipe correspondence. Those leave cause 1 intact.

### 2. AutoCard becomes one product canvas with a scene tray - L

**Change in feel:** the seller steers the same visible product through scene selection, preparation and review instead of scrolling between two large photos and category forms.

**Own files:** `U\autocard\AutoCardScreen.kt`; `U\autocard\FormingCardPreview.kt`; new `U\autocard\AutoCardSceneTray.kt`; new `V\strings_w50_canvas.xml` and `RU\strings_w50_canvas.xml`; new `IT\autocard\AutoCardCanvasTest.kt`.

**Deliver:** one persistent preview plane; scene tray below it; source/category/attributes as contextual sheets; next action docked to the workspace. Render actual local recipe previews using the product and recipe, with bounded thumbnail work, not flat swatches. If no generated asset exists, show an unavailable/illustrative state, never a counterfeit output. Retain category correction, original-only marketplace policy, unavailable-scene handling and quote gates. During recognition/composition preserve source context with a truthful busy overlay, then resolve into the returned composition.

**Libraries:** existing Compose layout/animation, Foundation and bitmap/domain composition; no new dependency. Extend the existing `FormingCardPreview`, not a second skeleton engine (`:57-99`). Lottie is not needed to hold a canvas or show an actual state.

**Interface contract:** accept optional initial scene ID and optional photo-continuity modifier from WP5; validate the ID against the available category recipes. Export reusable result-preview behavior without touching WP4's files. Consume WP1's example manifest only for labelled examples, never as a substitute for the user's composed output.

**Acceptance proposal:** selected scene remains visible while work runs; category correction remains reachable without a separate full-height source preview; unavailable generated scenes cannot proceed as ready; original-only mode cannot create a modified card; failures never animate into success. At large text/short windows, allow an adaptive scroll fallback instead of shrinking targets.

**Reject:** just snapping the existing gradient swatches, adding a glow to a generic silhouette, hiding the category override, or auto-running a quoted paid recognition call.

### 3. Rebuild live generative Studio as an image-anchored edit session - L

**Change in feel:** a generated scene/edit feels like a studio session around the selected product, not a developer console with a prompt and remote-job status at the bottom.

**Own files:** `U\studio\StudioRoute.kt`; `U\studio\StudioViewModel.kt`; new `U\studio\StudioSessionCanvas.kt`; new `V\strings_w50_session.xml` and `RU\strings_w50_session.xml`; new `IT\studio\StudioSessionTest.kt`; new `JT\ui\studio\StudioSessionStateTest.kt`.

**Deliver:** one selected item canvas; relevant operation tray; concise instruction composer and priced Generate; stage-specific queued/running/downloading/confirming overlay within the canvas; result inspection and saved-variant filmstrip bound to that item. Put technical transport diagnostics in an expandable detail surface, retaining actionable errors and accessible status. Keep the generated/not-original warning and charge/keep/discard meaning visible at the relevant decision.

**Libraries:** existing Compose/Coil and observable Studio state. Reuse WP2's forming-preview interface after it lands, passing source context and real phases; no new Lottie/Haze dependency. **MEASURED support:** `StudioViewModel.kt:55-78,86-117` already models pending/phase/result/variants and confirmation eligibility.

**Interface contract:** preserve the current `start(operation, prompt, displayedCredits)` and resume-existing-job semantics (`StudioViewModel.kt:161-185`); expose optional entry photo/operation intent for WP5 without starting anything automatically. View state must remain a projection of actual job/delivery state, not a second payment state machine.

**Acceptance proposal:** simulate queued, running, received-but-unconfirmed, confirmed, lost connection, changed quote, discard and saved-variant reopen. The active image remains the anchor throughout; unknown delivery never becomes "free retry"; each explicitly confirmed paid action submits once; saved generated outputs remain labelled and do not replace MAIN/source.

**Reject:** silently enabling blocked tools, pretending unavailable recognition/series work, decorative progress percentages, collapsing delivery confirmation into a timed animation, or hiding the distinction between a generated whole image and preserved product pixels. Presentation is not an adapter/backend repair.

### 4. A result workbench: inspect, choose siblings, hand off - L

**Change in feel:** finishing creation lands on a photograph the seller can judge and use, with related outputs at hand, rather than a "ready" message above chips and download buttons.

**Own files:** new `U\autocard\AutoCardResultScreen.kt`; `U\autocard\AutoCardResultsViewModel.kt`; new `U\components\BeforeAfterComparison.kt`; `D\project\ProjectOutputRepository.kt`; `U\export\ExportPackSource.kt`; `U\export\ExportScreen.kt`; new `V\strings_w50_result.xml` and `RU\strings_w50_result.xml`; new `IT\autocard\AutoCardResultWorkbenchTest.kt`; new `JT\data\project\ResultSelectionTest.kt`.

**Deliver:** extract current save/share/reload/error behavior into a canvas-led result destination; direct hold/slider comparison with a labelled button alternative; filmstrip of actual persisted sibling outputs for the same item; active-output-specific Share / Save / Export-pack handoff and a truthful next-edit action. Decode active/full inspection separately from thumbnails. Keep aligned comparison explicit: original and composition may differ in dimensions/framing, so fit them in documented coordinate frames; do not suggest a wipe proves pixel identity.

**Libraries:** Compose gestures/semantics and existing Coil. Telephoto is already declared and included (`gradle\libs.versions.toml:30,78`, `app\build.gradle.kts:180`); evaluate it for the result inspection surface only, not every tile. No new dependency. Use E3 as evidence for image-anchored iteration, not as evidence that this provider returns four results per request.

**Interface contract:** supply a public route composable taking photo ID, optional incoming pixels and back/regenerate/export callbacks for WP5. Retain current persistence/recovery (`ProjectOutputRepository.kt:35-72`) while adding metadata-first sibling enumeration and active-output loading. Expose selected output ID explicitly. Add an explicit selected-output input to the export source: its current default loads library items and the latest Look (`ExportPackSource.kt:80-110`), not a result selection. Preserve that default for existing whole-library export; WP5 must not send a generic "latest pack" that silently exports a different sibling. Keep generated-output eligibility and provenance checks in both routes.

**Acceptance proposal:** switching siblings changes preview AND bytes shared/saved/exported; process recreation reloads the chosen stored output or gives a truthful missing state; saving is idempotent for destination recreation, not a duplicate variant on every entry; single-output state has no fake variants; comparison remains usable with TalkBack. Distinguish local compositions from generated outputs using recorded provenance, not appearance.

**Reject:** merely renaming Save, another success banner, a fake four-tile grid, or charging again for browsing already saved outputs. Do not upgrade Telephoto as if it were absent.

### 5. Creation continuity and a quiet contextual shell - M

**Change in feel:** one unmistakable create entry and continuous photo context make the five packages feel like one app rather than separate redesigned screens.

**Own files:** `U\PlayKilnApp.kt`; `U\components\PlinthNavBar.kt`; `U\components\PlinthTopBar.kt`; `U\import\ImportScreen.kt`; `U\import\SelectedMediaPreview.kt`; `U\theme\PlinthMotion.kt`; new `U\components\PlinthPhotoContinuity.kt`; new `V\strings_w50_shell.xml` and `RU\strings_w50_shell.xml`; `IT\NavigationMotionTest.kt`; new `IT\CreationContinuityTest.kt`.

**Deliver:** integrate WP1-4 contracts; make compact Create genuinely central with accessible tab order, but more importantly launch the picker from explicit create intent once, return to the right origin on cancellation, and preserve category/recipe/tool intent into the selected-photo workspace. Keep a deliberate rail on wide windows. Replace the embedded private result composable with WP4's route and wire the selected output into export, preserving any marketplace eligibility checks. While actively editing, suppress redundant root navigation chrome in favor of workspace back/next controls without trapping the user or hiding cost.

**Libraries:** existing Nav3 **1.2.0**, Compose shared transitions and adaptive scene strategy, no new dependency (`gradle\libs.versions.toml:11`; `PlayKilnApp.kt:316-346`). Connect matching shared-image endpoints for the same photo identity, using a scoped optional modifier supplied to WP2 and the import preview. Existing photo-key helper at `PlinthMotion.kt:34` is a starting point, not proof of connected endpoints. Never morph the original photo into a different generated output under the same identity.

**Integration contract:** WP5 alone changes host signatures/callback wiring and removes the old private result implementation after WP4 lands. Other packages retain old callable entry signatures with optional defaults until integration. Shared transition scope/modifiers flow from the host; screens must render normally when no matching source exists. Picker launch is keyed to an explicit navigation event, not recomposition or process restore. Declining/cancelling never launches it repeatedly.

**Acceptance proposal:** EN/RU, compact/wide, cancel/reopen picker, activity recreation, rapid back, predictive-back cancel and complete; stable photo identity across a selected-photo transition; no stuck overlay, duplicate picker or paid submission. Preserve the existing 220 ms navigation-settling expectation in `IT\NavigationMotionTest.kt:20-63,98-109`; no ceremonial animation delaying a ready action. Device smoothness remains a separate measurement.

**Reject:** shrinking nav height as the whole task, switching navigation libraries, globally slowing all transitions, or drawing blur over every scrolling list. Centre placement without a direct intent-preserving route does not close cause 3.

## 6. Dependencies, integration and library decisions

**Proposed execution order - INFERRED:** begin WP1, WP2 and WP4 in disjoint files with agreed optional contracts; WP3 consumes WP2's preview API; WP5 integrates after those contracts land. WP2 can initially operate from existing recipes, then use WP1's manifest for examples. WP4's comparison component need not be forced onto every other screen in this wave. Rank and scheduling are intentionally different. If a contract needs to change, route it through its owner rather than letting two workers edit the host.

| Technology | Decision and evidence |
|---|---|
| Haze | **INFERRED: reject for this wave.** `R\production-android-stack-2026.md:77-85` records 2.0.1 Maven release versus README beta-state ambiguity and rejects adoption. This is **ON RECORD through prior research**, not a fresh W50 version check. No load-bearing gap above requires backdrop sampling. **UNVERIFIED:** GPU/frame cost on Plinth; a future experiment needs a bounded overlay and same-device frame comparison with a nonblur fallback, not an assertion that blur is always slow. |
| Lottie | **INFERRED: defer.** Prior research records an optional 6.7.1 dependency (`R\modern-ai-android-2026.md:46`); current catalog/build do not declare it. The existing native forming preview already exists. Add only for an authored, rights-cleared animation with a distinct informational job after the structural flow is in place; no new coordinate or unverified version pin is required by this plan. |
| SharedTransition | **MEASURED:** host and photo-key helper exist, endpoint search is empty as described in section 3. **ON RECORD:** E6 requires endpoints. **INFERRED:** finish one same-photo journey within existing Compose; do not add a third-party transition package or mistake a wrapper for delivery. |
| MotionScheme | **ON RECORD:** version-specific prior research at `production-android-stack-2026.md:28` says the pinned M3 1.4.0 implementation is internal. **INFERRED:** retain public Compose specs and existing motion tokens; do not add alpha/beta artifacts to follow unversioned docs. W50 did not re-download the versioned JAR. |
| Coil / Telephoto / Foundation / Nav3 | **MEASURED:** declared/included in the current catalog/build; Nav3 is already 1.2.0 and Telephoto already 0.19.0. **INFERRED:** use these instead of another image loader, pager, zoom or navigation engine. Library presence alone proves neither UX nor performance. |

**Proposed design constants - INFERRED:** hold muted teal and the current typography tokens while changing visual composition; use a small number of contextual headings instead of adding prose to fill space; maintain target sizes, resource localization, keyboard reachability and large-text fallbacks. Quiet chrome means fewer competing panels, not illegible labels or removing safeguards.

## 7. Falsifiable completion criteria for the later apply wave

These are **proposed acceptance observations / INFERRED**, not W50 test results:

- A first-time seller sees distinct attainable outcomes, chooses an example, selects their photo and reaches that intended scene without reselecting category/recipe. Cancelling import leaves the original entry usable.
- A returning seller can identify and resume a finished output versus its source; missing artwork never looks like a completed transformation.
- The selected product remains on a canvas during the real job phases, with failures/retry/charge uncertainty still explicit; no predetermined animation manufactures progress.
- Comparison and real sibling selection are usable without gestures; the exact active sibling reaches Share/Save/Export. Whole-image generation stays labelled, and original-only marketplace restrictions remain effective.
- Existing fast back/navigation behavior survives; paired photo transitions have no duplicate/stuck layer and gracefully degrade when their counterpart is absent.

**UNVERIFIED - who and how:** the orchestrator/independent verifier must exercise those journeys in a debug build, including EN/RU, dark/light, large text, short/expanded windows, network interruption and recreation. Run `node scripts\run-slot-tests.mjs --slot plinth-product-photos --rerun` and `node scripts\verify.mjs --slot plinth-product-photos --build`, plus the packages' actual Compose instrumentation tests through the repository Gradle route. Record artifact/source revision, serial and observed screen behavior. A source-only PASS cannot settle the owner-feel verdict; a code symbol cannot prove a working gesture or a beautiful screen.

**No release action is part of this plan.** W50 does not record approval, sign, produce a store pack, capture store media or publish. A later debug visual review is evidence gathering, never inferred Checkpoint 1.5 authorization.

## CLAIM / LABEL / EVIDENCE ledger

| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| Photoroom documents template-led entry, generated previews and per-image variant actions on Android | ON RECORD | E2 raw article body, Home / Main editor / AI Tools editors sections; E3 Steps (Android). |
| Picsart advertises generate/edit/refine and documents same-subject background suggestions/styles on its web page | ON RECORD | E4 Play description; E5 "Get instant background suggestions" and "Generate AI backgrounds that go well with your branding". Web is not Android UI evidence. |
| Home is already hero-first, but its tool cards repeat the latest source photo | MEASURED | `U\home\HomeScreen.kt:142-160,200-204`; `U\components\PlinthToolCard.kt:93-128`. |
| Scene browsing exists but passes category rather than the displayed recipe; AutoCard choices remain swatches | MEASURED | `U\home\HomeSceneStrip.kt:124-173,247-297`; `HomeScreen.kt:157-160`; `U\autocard\AutoCardScreen.kt:676-744`. |
| AutoCard forming previews, saved Studio variants and a dedicated save/share result destination already exist | MEASURED | `U\autocard\FormingCardPreview.kt:57-99`; `U\studio\StudioRoute.kt:311-351`; `U\PlayKilnApp.kt:484-497,842-1084`. |
| Shared-transition host plumbing exists but no UI image endpoint modifiers were found | MEASURED | Exact `rg` search recorded in section 3: host/import/opt-in and photo-key definition only; `U\PlayKilnApp.kt:316-346`, `U\theme\PlinthMotion.kt:34`. |
| Live AI still separates source, result, prompt/tools and server progress down one list | MEASURED | `U\studio\StudioRoute.kt:156-271,356-488`. |
| The remaining dated feel is plausibly structural, not solved by another palette or elevation pass | INFERRED | Five causes in section 4 derived from the measured audit and E2-E5; this is a design diagnosis, not runtime comparative proof. |
| Exactly five disjoint apply packages address discovery, canvas, live AI session, result workbench and creation continuity | INFERRED | Section 5 ownership lists and section 6 integration order; proposed scope, not implemented behavior. |
| Competitor native colors, motion, blur, precise media density and Plinth's post-wave perceived quality | UNVERIFIED | No app/screenshot observation in W50; independent verifier must record comparable device journeys and owner product feedback, as section 7 specifies. |
