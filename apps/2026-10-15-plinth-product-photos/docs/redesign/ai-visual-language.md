# Plinth: Product Photos — the visual language of a modern AI app

**Status:** specification, not an implementation. Nothing in this document has been built.
**Scope:** the visual language of `apps/2026-10-15-plinth-product-photos` — surfaces, shape, type,
spacing, imagery density, motion, the navigation shell as *chrome*, the one-photo→one-image flow as
*form*, and the anti-patterns that make the current screens read as a notepad.
**Owner of the decision:** the owner, at checkpoint 1.5. This document exists so that the decision he
was asked to make on 2026-10-16 has a written standard behind it.
**Deliverable:** this file and nothing else. No file under `app/` was touched; `store/studio.json` and
`ui/theme/Color.kt` were read, not edited; Gradle was not run.

The required final section, **"Applied to Plinth: before → after"**, is last, after the claim ledger
and the sources.

---

## How to read the labels

| Label | Means | What must accompany it |
|---|---|---|
| `MEASURED` | a file in this repo read in this session, or a command run in it | the `path:line`, or the command and its output |
| `ON RECORD` | an external source that **a document in this repo records having read** | `<in-repo doc> → <page>`. The claim is as strong as the recorded read, and no stronger |
| `INFERRED` | derived from labelled facts | the facts, and the step between them |
| `UNVERIFIED` | not checked | the exact page or command that would decide it |

Two consequences, stated rather than implied. **I did not fetch any external page in this session**, so
every external claim is `ON RECORD` *through this repository's own research*, and the primary page is
named so a reader can go and check it. **I did not see the app on a device**, so every claim about
pixels, animation and perceived quality is `UNVERIFIED` and names the run that would settle it.

---

## 0. The measured starting position

The owner's report is short and it is about form, not function:
«Кнопки, текст, кнопки, текст, кнопки, текст… Какое-то просто доисторическое.»
(`docs/owner-review-2026-10-16.md`, verbatim). Six measurements explain it exactly.

**0.1 The app's own palette never reaches the screen.** `Color.kt` defines the palette and
`Theme.kt` sets **24 colour roles** (`ui/theme/Theme.kt:13-36` light, `:37-58` dark). The slot's own
code refers to the `surfaceContainer*` family **18 times across 7 files**, and **neither `Color.kt`
nor `Theme.kt` sets any of them** — `rg -n surfaceContainer ui/theme/Color.kt` returns **no matches**,
and `Theme.kt` is absent from the grep below
(`rg -c 'surfaceContainer(Lowest|Low|High|Highest)?'` → `HomeScreen.kt:5`, `AutoCardScreen.kt:3`,
`FormingCardPreview.kt:3`, `WalletScreen.kt:3`, `SelectedMediaPreview.kt:2`, `StudioToolsScreen.kt:1`,
`PlinthStage.kt:1`).
All 18 therefore resolve to the Compose framework's baseline scheme — a purple-seeded, violet-tinted
neutral that belongs to no Plinth decision. **The app's signature component is a gradient between the
framework's palette and its own:** `PlinthStage` interpolates
`surfaceContainerHigh → secondaryContainer → surface` (`ui/components/PlinthStage.kt:28-31`), i.e.
*framework → ClayPale → Paper*.

> **Two of these call sites landed after this document was first drafted and are worth calling out,
> because they are exactly §4.3's recommendation arriving half-built.**
> `ui/autocard/FormingCardPreview.kt` (now wired at `AutoCardScreen.kt:499-501`) already gets the hard
> half right: a `fillMaxWidth().aspectRatio(aspectRatio)` bound, `clip(shapes.extraLarge)`,
> `contentAlignment = Alignment.Center` — **the off-centre-spinner fix** — a
> `surfaceContainerHigh → surfaceContainerHighest` sweep drawn with `drawWithCache`/`drawRect`
> (`:81-101`), a `liveRegion = LiveRegionMode.Polite` announcement (`:107`), and a
> `animateFloatAsState(tween(350))` fade-out once the bitmap exists (`:47-51`). It is the right
> component. **But its two shimmer stops (`:80-81`) are the two unset roles above**, so the sweep
> currently interpolates between two framework defaults and the skeleton renders in the framework's
> violet-grey rather than in the bench tone — §5's `H3`/`H19` retone is what makes that component
> correct, and until then the newest, most modern-looking screen in the app is the one furthest from
> the palette. This is the clearest illustration of why §0.1 is the first fix, not a cosmetic one.

**0.2 There is no imagery.** `res/` contains **21 drawables and every one is a vector XML**; there are
**zero PNG/WebP/JPEG files anywhere under `res/`** (`Get-ChildItem -Recurse -File … | Where Extension
-in '.png','.webp','.jpg'` → 0; `res/drawable` → 21). For an app whose whole job is producing images,
no image is shipped.

**0.3 The text-to-image ratio, by source reference.** `rg -c '\bText\('` against
`rg -c 'Image\(|AsyncImage|painterResource|BitmapPainter|asImageBitmap'`, per UI file:

| UI file | `Text(` | image refs | ratio | what the screen is about |
|---|---:|---:|---:|---|
| `studio/StudioRoute.kt` | 46 | 3 | **15.3** | the studio's tool bench |
| `batch/BatchScreens.kt` | 43 | **0** | **∞** | processing *many photos* |
| `coach/CoachScreen.kt` | 40 | 2 | 20.0 | shooting advice, *about the photo* |
| `settings/SettingsScreen.kt` | 38 | 0 | ∞ | app settings |
| `wallet/WalletScreen.kt` | 38 | 0 | ∞ | buying credits for image work |
| `studio/StudioToolsScreen.kt` | 36 | 1 | 36.0 | the AI operation catalogue |
| `listing/ListingTextScreen.kt` | 31 | **0** | ∞ | listing text (legitimately textual) |
| `library/LibraryScreen.kt` | 26 | 2 | **13.0** | the project gallery |
| `group/GroupScreen.kt` | 25 | **0** | ∞ | SKU grouping of photos |
| `slide/SlideKitScreen.kt` | 24 | 3 | 8.0 | building a slide series |
| `cutout/CutoutScreen.kt` | 21 | 8 | 2.6 | the cut-out |
| `home/HomeScreen.kt` | 21 | 10 | 2.1 | the first screen |
| `autocard/AutoCardScreen.kt` | 33 | 3 | 11.0 | **the AI card** |
| `editor/EditorScreen.kt` | 13 | 3 | 4.3 | the editor |

**This is a proxy, not a rendered measurement** — one `Text(` may sit in an unreachable branch and one
`Image(` may be reused in a loop. Its honest limit is stated; what it establishes is direction, and the
direction is unambiguous: the screens whose job *is* photographic are the most textual.

**0.4 The framework decides the shape, too.** There are **10 distinct corner radii** in use — 30 dp
(`PlinthStage.kt:26,35`), 26 dp (`HomeScreen.kt:506`), 24 dp (`HomeScreen.kt:418`), 22 dp
(`HomeScreen.kt:386`, `EditorScreen.kt:123`), 20 dp (`HomeScreen.kt:427,461,491`), 18 dp
(`StudioTryout.kt:110`, `PlinthStage.kt:64`), 14 dp (`HomeScreen.kt:514`), 5 dp (`EditorScreen.kt:301`),
plus `MaterialTheme.shapes.extraLarge` (28 dp) and `shapes.large` (16 dp) — i.e. **three decisions per
radii from two different scales**, neither of which is written down anywhere.

**0.5 The type scale declares 8 of the 15 styles it uses.** `ui/theme/Type.kt` defines
`displaySmall, headlineMedium, titleLarge, titleMedium, bodyLarge, bodyMedium, labelLarge`. Three
styles used by the slot are **not** declared and fall to the framework defaults with different
tracking and weight: `labelSmall` (`AutoCardScreen.kt:586`), `labelMedium`
(`AutoCardScreen.kt:609`, `PlinthStage.kt:69`), `titleSmall` (`AutoCardScreen.kt:599`,
`WalletScreen.kt:193`).

**0.6 Three real destinations have no screen file.** `AutoCardResultScreen` (`PlayKilnApp.kt:609`),
`ProjectWorkspaceScreen` (`:787`) and `BatchDestinationRoute` (`:826`) are **private composables inside
the navigation module**, while every other screen lives in its own `ui/<area>/` package. The screen the
app's central promise lands on is the one with no home of its own.

**0.7 The shell has already been built, and nothing calls it.** This is the most actionable
measurement in the document, and it was not true when the first draft was written — the files landed
while this document was being edited:

| Component | Defined at | Called from outside its own file |
|---|---|---|
| `PlinthBottomNavBar` (tabs `STUDIO / LIBRARY / MORE`) | `ui/components/PlinthNavBar.kt:31` | **none** |
| `PlinthNavigationRail` (the same three, for wide windows) | `ui/components/PlinthNavBar.kt:85` | **none** |
| `PlinthTopBar` with `PlinthSection` | `ui/components/PlinthTopBar.kt:18,26` | **none** |
| `BalanceChip` | `ui/components/PlinthTopBar.kt:53` | **none** |
| `PlinthCostChip` | `ui/components/PlinthCostChip.kt:15` | **none** |
| `PlinthToolCard` | `ui/components/PlinthToolCard.kt:28` | **none** |

`rg -n 'PlinthBottomNavBar|PlinthNavigationRail|BalanceChip|PlinthCostChip|PlinthToolCard|PlinthTopBar|PlinthSection' …/plinthproductphotos`
returns **only the definitions and their own internal references** — there is no call site anywhere in
the app. `PlayKilnApp.kt` imports exactly one of the new components (`PlinthScaffold`, `:88`) and
applies it at `:329,338,362,597,660,793`; **`NavigationSuiteScaffold` appears nowhere in the slot**.

Three consequences, and they change what §3 and §9 are asking for:

1. **§3 is a wiring task, not a design task.** The three-tab set the parallel work chose
   (`Studio / Library / More`) is the same conclusion §3.1 reaches independently from the competitor
   convergence in `competitor-teardown.md` §2.1 — two derivations agreeing is the strongest signal in
   this document. What is missing is a host that switches `PlinthBottomNavBar` ↔ `PlinthNavigationRail`
   on the window size class and puts `BalanceChip` in the chrome.
2. **This document must not propose a second component under a different name.** Where §9 previously
   said "`CreditPill.kt` (new)" read **`PlinthCostChip`**; where it said "`BalanceChip.kt` (new)" read
   **`BalanceChip` in `PlinthTopBar.kt`**; where it said "`PlinthShell.kt` (new)" read **wire the
   existing `PlinthNavBar`/`PlinthTopBar`**. Creating a synonym beside a landed component is how the
   app acquires two cost chips and two balances — the exact drift `.cursor/rules/visual-identity.mdc`
   forbids for palettes, applied to components.
3. **The unwired components still carry the §0.1 defect**, because they are card and chrome surfaces:
   they need the §5.1 retone before they are adopted, not after. Adopting them as they are would put the
   framework's violet-grey into the shell on all twenty destinations at once.

**The mechanism, in one sentence:** *the app has a real identity in `studio.json`, a real identity in
`Color.kt`, a real shell in `ui/components/` — and no mechanism that carries any of the three into the
~17 surfaces, 10 radii and 20 destinations that actually decide what a screen looks like, so the
framework fills the gap with defaults and the result reads as a generic form.* That is what "notepad"
means, and §0.7 is its sharpest form: the identity is not missing, it is **unreferenced**.

---

## 1. What makes an app read as an AI app in 2025–2026

### 1.1 The governing principle

**The output is the interface.** A form over an API asks the user to describe the result; an AI app
shows the result and lets the user steer it. Every device below is a consequence of that one sentence.

### 1.2 The eight devices

| # | Device | What it looks like | Evidence | Plinth today |
|---|---|---|---|---|
| 1 | **Generated content first, controls second** | the composed image occupies the screen; tools are secondary chrome | `ON RECORD` — `docs/redesign/competitor-teardown.md` §2.3: Photoroom's onboarding recommendation literally says *"watch the background vanish"*; Pixelcut's home is *"a horizontally scrolling list of large, visually rich cards"* where *"the imagery is the menu"* | The result is a `Box` with a `fillMaxWidth().aspectRatio(1.2f)` fill and `contentAlignment = Center` (`AutoCardScreen.kt:390-405`) sitting in a scrolling column of 33 `Text(` references. `PlinthStage`, the slot's only stage component, is **not imported by `AutoCardScreen.kt`** — the AI screen is the least styled screen in the app |
| 2 | **A scene/preset gallery, not sliders** | named, *visible* scene cards with rendered thumbnails | `ON RECORD` — teardown §2.5.2: Pebblely's **40+ named templates** (Cafe, Studio, Kitchen, Water, Nature, Bathroom); Photoroom's named AI tools (*Recolor*, *Product Beautifier*, *Virtual Model*, *Product Staging*, *Replace Background*). The teardown's own conclusion: *"Parameter sliders are how a photo editor looks; scene cards are how an AI app looks."* | **The data exists and the UI throws it away.** `assets/autocard/scene-recipes.json` (63 KB) declares **124 scene ids** (`rg -o '"sceneId"'`), each carrying `color`, `color2`, `angleDeg`, shadow opacity/blur/offset, `fillTarget` and `marginPct`. `SceneChoice` (`AutoCardScreen.kt:553-613`) renders those recipes as a **flat `linearGradient` swatch plus a text label**, and for a `GENERATED` recipe with no palette it renders the string `w3_scene_no_preview` ("No scene preview", `res/values{,-ru}/strings_import_smartcard.xml:19`). The owner saw «просто три точки и то какие-то цифры» — the label is literally `"Solid 1"`, `"Gradient 1"`, `"Studio 1"` (`AutoCardScreen.kt:589-601`) |
| 3 | **Before/after, because the promise is "the pixels are untouched"** | a comparison control, not a toggle chip | **`MEASURED` in this slot's own contract**: `differentiators.json` carries a **`core`** promise `item-pixels-untouched` (weight `core`, family `core_operation`, `jobStage` `during`) — the app has *signed an obligation* to prove the item's pixels survived. The comparison is the visible form of that obligation, so it is not a trend to copy | The app **already has a before/after toggle on two screens** — `AutoCardScreen.kt:283-289` and `EditorScreen.kt:146-157`, both rendered as a pair of `FilterChip`s — plus a hint string `w3_swipe_back`. So the interaction exists twice, in two idioms, on two screens, and neither is the *component* the promise needs |
| 4 | **The result forms in front of the user** | a shimmer bounded to the result's own shape, fading as the image arrives | `ON RECORD` — teardown §2.5.4: Apple's Intelligence guidelines define a shimmer as a **process indicator** — a modulated glow that conveys ongoing activity and then **fades as the result becomes ready** (`developer.apple.com/design/human-interface-guidelines/generative-ai`). §4.3 below is the Android mechanism | A bare `CircularProgressIndicator` — see §4.3 and §6 |
| 5 | **Confidence and provenance shown in the open** | the model's own read, with its certainty, and where the bytes came from | `MEASURED` — the slot already does the confidence half: `AutoCardScreen` renders recognition progress and the suggested category with its confidence (`AutoCardScreen.kt:264-310,367-380` per `docs/ui-critique-2026-10-16.md` §5). `ui/provenance/ProvenanceBadge.kt` exists; `ProofRing` (`ui/components/ProofRing.kt`) draws a verdigris ring as a provenance affordance | `ProofRing` is used **exactly once**, at `CardScreen.kt:99` — on the *non-AI* path. The AI path has no proof affordance at all |
| 6 | **A "magic" affordance on the primary action** | the create action is unmistakable, named and one tap away | `ON RECORD` — teardown §4.1: Photoroom's *Start from Photo* and Canva's *Create* tab both exist because the create path must never be more than one tap. (`ON RECORD`, `canva.com/help` *Understanding the Canva mobile app tabs*.) | The primary action is a plain full-width `Button` with the text label `plinth_make_ai_card`, inside a `Surface(shadowElevation = 6.dp)` (`ImportScreen.kt:117-121`, `AutoCardScreen.kt:328-331`). The owner's words: «А, вот кнопка, она оказывается в самом низу была» |
| 7 | **Imagery-dense chrome instead of one card per idea** | ~2.5 gallery cards visible at once, not one full-width row | `ON RECORD` — teardown §2.7 (convergent: Pixelcut's dashboard of large visual cards). The slot's own `SceneChoice` already sets `widthIn(min = 164.dp, max = 220.dp)` (`AutoCardScreen.kt:566`) — so the *density convention is already in the code*, only the thumbnails are missing | See §5.6 |
| 8 | **A process language that belongs to the app, and ends** | the generating surface itself is branded, then resolves | `ON RECORD` — teardown §2.5.4 (Apple shimmer). **Not** to be confused with decoration | Today: three shapes of progress in one app — a 24 dp spinner in a 40 dp box (`AutoCardScreen.kt:318`), a full-width bar (`StudioRoute.kt:210`), and a bare centred-by-accident spinner (`LibraryScreen.kt:165`) |

### 1.3 The glass/blur device: declined, with the reason

The brief asks about glass/blur. **I decline real blur for this app, and this is a design decision
rather than an omission.**

- **There is no first-party Material 3 "glass" component.** M3 expresses depth with *tone*
  (`ON RECORD` — `docs/research/2026-10-02-color-systems-and-palette-derivation.md` §5.4, citing
  <https://m3.material.io/blog/tone-based-surface-color-m3>, which is the page `H13` also cites for the
  surface-container emphasis ladder). A blur is not a Material surface; it is a `RenderEffect` with a
  GPU cost on every frame it covers.
- **The cost claim is `UNVERIFIED`.** I did not measure `Modifier.blur` or `graphicsLayer { renderEffect }`
  on the test device. What would decide it: a Macrobenchmark or a frame-timing capture on
  `SM-A405FM` (serial `R58MB3SGZVL`) while a blurred layer is visible.
  The brief in `.cursor/rules/device-testing.mdc` is the run.
- **The app already has the cheaper idiom, and it is the right one.** `PlinthStage.kt:64` renders
  `surface.copy(alpha = 0.72f)` over its own gradient — a translucent lift that costs nothing and is
  derived from the palette. **That is Plinth's "glass": a tone step plus one alpha, never a blur.**

The three devices I would decline elsewhere for the same reason — a glow that is not a state, a
gradient used as a background rather than as a *field*, and any shadow — are handled by §2.5.

### 1.4 What the platform requires you to add alongside

An app that generates imagery is not only a design problem. `docs/research/M0-S6-play-policy.md` records
(`ON RECORD`, read 2026-10-14) that Google requires **reporting/flagging for offensive AI-generated
content** (`support.google.com/googleplay/android-developer/answer/14094294`) and references
**C2PA / Content Credentials** (`c2pa.org/conformance/`) as the provenance standard for generated media.
This is not a visual-language requirement, and it is named here because it is the reason device #5
(provenance) is worth spending a component on: it is simultaneously a differentiator and a policy
surface. **The implementation of the reporting requirement is `UNVERIFIED`** in that same record.

---

## 2. What Material 3 and Android actually constrain

### 2.1 The pin is a boundary: M3 Expressive is out of bounds

**`MEASURED` in this repo** — `.cursor/rules/ui-ux-baseline.mdc` records, citing
`docs/research/2026-10-02-modern-android-platform-surface.md` §6: *Material 3 Expressive and the physics
`MotionScheme` live in `material3 1.5.0-alpha*`; this factory's pinned BOM (`2026.09.00`) ships stable
`material3 1.4.0` and contains neither*, and `uiux.platform` in `scripts/core/uiux.mjs` **fails** a build
file that declares an Expressive/`MotionScheme` alpha artifact the pin does not carry.

**So: adopt the Expressive *ideas* — tone-based surfaces, one tokenised shape scale, spring motion — and
reach for none of its *artifacts*.** The gestures are all available on the pin
(`androidx.compose.animation` springs, `Modifier.animateItem`, `AnimatedContent`, shared elements);
the `MotionScheme` object is not. A design that needs a 1.5.0-alpha artifact to look modern is a design
that is betting the factory's pinned toolchain on a version bump.

### 2.2 Surfaces: this palette carries two visible levels, not five

Material 3 defines `surfaceContainerLowest / Low / default / High / Highest` — five steps
(`ON RECORD` — `m3.material.io/blog/tone-based-surface-color-m3`, cited at `H13` in
`docs/research/2026-10-02-visual-hierarchy-and-attention.md:863`). **Plinth cannot express five**, and
the arithmetic says so.

The warm axis is `Paper #E8E1D5` → `ClayPale #E0C8B9`. Every step between them is a `lerp` of two
*declared* colours, so no new hue enters the identity. Computed with the WCAG relative-luminance formula
as a command (`python .kiln/scratch/plinth-contrast.py`, run in this session):

| level | role(s) | value | luminance | vs the page |
|---|---|---|---:|---:|
| **page** | `surface`, `background`, `surfaceContainerLowest`, `surfaceContainerLow` | `Paper #E8E1D5` | 0.75810 | 1.000:1 |
| **bench** | `surfaceContainer`, `surfaceContainerHigh` | `#E4D4C6` (lerp 0.52) | 0.67658 | **1.112:1** |
| **raised** | `surfaceContainerHighest`, `secondaryContainer` | `#E0C8B9` = `ClayPale` | 0.60659 | **1.231:1** |

**The whole warm axis spans 1.231:1.** Divided across four nominal steps that is 1.054:1 per step — and
`H13` (`…visual-hierarchy-and-attention.md:863`) requires a group container to separate from its parent
by **≥ 1.05:1**. So a fifth step would be *at* the floor and invisible in practice. **The correct design
is two steps and a hairline, not five steps of tone.** Anything finer must be carried by spacing (H11,
H12), by an edge, or by the accent — never by tone.

Dark mode has more room and it is asymmetric — this is a `MEASURED` surprise worth stating:

| level | value | vs the page | step |
|---|---|---|---:|---:|
| page | `Ink #212627` | 1.000:1 | — |
| | `#363C3D` | 1.364:1 | 1.364 |
| | `#3C4243` | 1.497:1 | **1.097** |
| | `#424848` = `InkLift` | 1.642:1 | **1.097** |
| raised | `#575B58` | 2.219:1 | 1.351 |

**Dark carries three practically visible levels; light carries two.** A design rule that says "the dark
scheme mirrors the light one" is arithmetically false here, and `§5.5` of
`docs/research/2026-10-02-color-systems-and-palette-derivation.md` names the failure it causes
(*"a light scheme and a dark scheme chosen independently are two free choices in the same space"*).

### 2.3 Elevation is tone. Not shadow.

`ON RECORD` — same colour doc, §5.4 (*"Elevation is tone, not shadow"*): M3 expresses depth through
surface-container tone and *"reserves shadow for components that genuinely float over arbitrary
content"*, the stated reason being legibility under any contrast setting, because *"a tonal step is a
measured separation … whereas a shadow is an uncontrolled soft edge"*.

Plinth currently does the opposite in the two places that matter: `PlinthStage.kt:26` applies
`.shadow(10.dp, RoundedCornerShape(30.dp))` and the primary-action bars use
`Surface(shadowElevation = 6.dp)` with **no `color`** (`ImportScreen.kt:106`, `AutoCardScreen.kt:305`).
A `Surface` with no colour is `surface` — and in light mode `surface == surfaceVariant == Paper`
(`Theme.kt:28,30`). **So the app's pinned action bar is Paper on Paper, separated only by a neutral grey
shadow over a warm page — which is why the owner's eye reads it as "hidden".** That is not a hierarchy
opinion; it is a contrast of 1.000:1 with the page (computed, table in §5.1, row `surface`/`surfaceVariant`).

### 2.4 Shape: one scale, adopted, replacing ten radii

M3 ships a tokenised shape scale and the point of a scale is that a component's radius is a *lookup*
(`ON RECORD` — `m3.material.io/styles/shape/overview`; the current step names and dp values are
`UNVERIFIED` here — the page would settle them, and `.cursor/rules/visual-identity.mdc` already makes the
slot's own shapes part of its identity). What is not in doubt is the requirement: **the app must stop
carrying 10 ad-hoc radii (§0.4).** The concrete scale is §5.3.

### 2.5 Gradients and glow: the official position, and the mechanism that replaces them

There is no M3 page that says "gradients are banned", and I will not invent one. What follows is
`INFERRED` from two `MEASURED` facts already in this repo:

1. **M3's depth mechanism is tone** (colour doc §5.4). A gradient is therefore not competing with a
   shadow — it is competing with *the thing M3 actually uses*. Every gradient Plinth draws should be a
   **tonal field between two declared palette colours**, which is what `PlinthStage` half-does today and
   fails only because its first stop is `surfaceContainerHigh`, an unset role.
2. **A gradient's worst stop is the contrast case** (`ON RECORD` — colour doc's *contrast battery*
   method, §7: *"every declared text/background pair, at the gradient's worst stop, in both schemes"*).
   A gradient therefore has a hard obligation: **every ink placed over it must clear its WCAG floor at
   the gradient's worst stop**, which must be checked numerically at authoring time.

**The rule that follows.** A gradient is allowed only (a) between two declared palette colours, (b) over
a bounded region — a stage, a pill, a thumbnail plate, never a whole screen background, and (c) with its
worst stop's contrast measured. **Glow is allowed only as a *state*, never as decoration**: it marks
"this is running" and it ends when the run ends (§4.3). A glowing thing that is not running is a lie
about the app's state.

### 2.6 Type: the ramp, and the ceiling on tiers

Two `ON RECORD` anchors, both already in the repo's corpus
(`docs/research/2026-10-02-visual-hierarchy-and-attention.md` §2 and §6):

- NN/g: *"Use no more than 3 sizes"*, *"Limit how many elements are big to a maximum of 2"*
  (`nngroup.com/articles/visual-hierarchy-ux-definition/`).
- M3's own type guidance: *"Your product likely will not need all 15 default styles from the Material
  Design type scale"*, and importantly *"the rendered type scale should aim to provide impactful contrast
  between sizes by avoiding small differences"* (`m3.material.io/styles/typography/type-scale-tokens`).

The repo's own extension, `INFERRED` and labelled as such there, is `H3`: **≤ 6 distinct sizes and ≤ 4
content tiers** on one screen (`…visual-hierarchy-and-attention.md:777-783`). §5.4 adopts `H3`.

One measured consequence specific to Plinth: `Type.kt` sets `fontFeatureSettings = "tnum"` on every
style it declares and `letterSpacing = 0.sp` on all but `titleMedium`. The three styles it does **not**
declare (`labelSmall`, `labelMedium`, `titleSmall`, §0.5) therefore render with the framework's
tracking **and without tabular numerals** — so a credit count is tabular in one label and proportional
in the next. That is a visible defect on the wallet and the tool catalogue.

### 2.7 Spacing

`MEASURED`, both quoted in `…visual-hierarchy-and-attention.md` §7:
Android — *"Android UI utilizes an 8 dp grid … Smaller elements such as icons, type, and some elements
within components are best aligned to a 4 dp grid"*
(`developer.android.com/design/ui/mobile/guides/layout-and-content/grids-and-units`); M3 —
*"space100 is 8dp. The main spacing units are multiples of 8dp"* (`m3.material.io/styles/spacing/tokens`).
Encoded as `H12` (`…visual-hierarchy-and-attention.md:852`): 100 % on the 4 dp grid, ≥ 80 % on the 8 dp.

Grouping is a `CONVENTION`, and the repo says so rather than pretending otherwise: `H11`
(*between-group gap ≥ 2× within-group gap*, floor 2.0, target 3–4×) is derived from published practitioner
rules of thumb, with Apple's ~8 pt / ~32 pt settings list as the only measured data point
(`…visual-hierarchy-and-attention.md:841-850`). **Adopt its floor, keep its label.**

### 2.8 Imagery-first: what the platform says, and what it does not

Material 3 has no rule that says "show an image before you show a button", and I am not going to
attribute one to it. The constraint from the platform is narrower and harder: **the first screen must
still satisfy `H15`** (the focal element is in the top third or is the first text emitted,
`…visual-hierarchy-and-attention.md:875`) **and `H1`** (exactly one rank-1 element,
`:764`). Imagery-first is therefore not "put a picture at the top and hope"; it is **a decision about
which element is rank-1**, and the answer for this app is the *image*, which is a promise the contract
already made (`item-pixels-untouched`, §1.2 device 3).

---

## 3. The shell

### 3.1 What is decided elsewhere, said out loud so it is not re-decided here

The tab set, the tab count and the raised create action are **specified in
`docs/redesign/competitor-teardown.md` §4.1–4.2**, with the competitor evidence for each. That document
is another worker's deliverable and **this section does not restate or overturn the decision.** What
this section owns is the *chrome*: what the bar is made of, how it behaves when the window is not
compact, and where the balance, settings and back affordance live.

The one structural fact that must be recorded because both documents depend on it: **there is no shell
wired today** — and, as of a measurement taken while this document was being edited, the shell is
**built and unwired** rather than absent (see §0.7, which is the corrected version of this paragraph).
`NavDisplay(` is called once (`ui/PlayKilnApp.kt:182`); the M3 vocabulary is unused — no
`NavigationBar`, no `NavigationRail`, no `NavigationSuiteScaffold` anywhere in the slot, where `rg` over
`app/src/main/java/com/gexetr/plinthproductphotos` returns only `bottomBar =` inside per-screen
`Scaffold`s — while the slot's **own** `PlinthBottomNavBar`, `PlinthNavigationRail` and `PlinthTopBar`
sit in `ui/components/` with **zero call sites**. Every one of the 20 destinations resolves as
`metadata = ListDetailSceneStrategy.detailPane()` (`PlayKilnApp.kt:210-491`) with `listPane` used
**only** for `HomeDestination` (`:189`). So the adaptive vocabulary is *imported and inert*, and the
slot's own substitute is *written and unreferenced*: on a tablet the app shows one stretched pane and,
where nothing is open, the sentence `home_pick_section` (`DetailPlaceholder`, `PlayKilnApp.kt:886-892`).

### 3.2 The bar

- **Tonal, not shadowed.** The bar's container is `surface`-family with a top hairline in `outlineVariant`
  (§5.1). It does **not** use `shadowElevation`, for the reason in §2.3.
- **The bar is chrome, and `H9` bounds it** (`…visual-hierarchy-and-attention.md:827`): ≤ 1 primary and
  ≤ 1 secondary affordance, ≤ 25 % of window height. Plinth's tab bar plus create action is 1 create
  affordance plus icon-only destinations — inside the bound.
- **Text in the bar is `labelMedium` at 12 sp** and is the app's only all-caps-free short label tier;
  `H23` caps all-caps at `labelLarge`/14 sp and 30 characters (`:934`).
- **One accent in the bar, and it is the indicator.** `H6` (`:802`) allows ≤ 2 `primary`-filled surfaces
  per screen; the bar's selected indicator and the create action are those two, and they are the *only*
  verdigris surfaces on any screen that shows the bar.
- **The balance is a chip, not a tab** (teardown §4.2 owns the placement): `Clay`-rimmed pill,
  `ClayDeep` ink, tabular numerals, top-trailing. `studio.json` reserves clay for charged credits, and
  this is the one place in the chrome where credits are stated.

### 3.3 Where the account, settings and back affordance go

- **Settings folds the account surface in.** `AboutDestination` (`PlayKilnApp.kt:116`, `:210`) is reached
  from Home and is a destination of its own today; under this spec it becomes a row inside Settings.
  Justification is the convergent competitor rule (`ON RECORD` — teardown §2.2: Canva moved Profile into
  *More*; Pebblely puts billing under *Profile*; Photoroom keeps plan in the header) and `docs/settings-ia.md`
  owns the internal structure.
- **Back is an app bar action, never a floating button.** Today `WithShellBack`
  (`PlayKilnApp.kt:867-882`) renders a **`TextButton` with a text label** inside a `Column` padded by
  `statusBarsPadding()`, sitting *above* the content of Cutout, Studio, SlideKit, Library and Group.
  That is the owner's «я не могу нажать на вернуться в студию, так как вверху плашка уведомления мне
  перекрывает» (`docs/owner-review-2026-10-16.md`, class 1). The spec fixes the affordance, not just the
  inset: **a real `TopAppBar` with an icon-only navigation action whose `contentDescription` is
  `plinth_back`**, and no text label. `.cursor/rules/ui-ux-baseline.mdc` owns the inset mechanism and it
  is not restated here.
- **Predictive back is opted in** at `<application>` with `android:enableOnBackInvokedCallback="true"`,
  and back handling stays with the host's back stack, not `MainActivity`
  (`MEASURED`, `.cursor/rules/ui-ux-baseline.mdc` §"The modern platform", citing
  `docs/research/2026-10-02-modern-android-platform-surface.md` §2). A push/pop is animated
  (`NavDisplay`'s own transition), because "an instant swap has nothing for predictive back to preview".

### 3.4 Non-compact windows

The adaptive floor the factory already measures is `uiux.adaptive`: *"A list that opens a detail
destination must state its two-pane policy."* Plinth declares `detailPane()` for 20 destinations and a
`listPane` for one, so the policy is *declared* and *unimplemented*.

The spec: **`NavigationSuiteScaffold` is the one host**, and it switches the bar's form by window class
(`ON RECORD` — teardown §4.1 already recommends the three-tab shell; the switch itself is Android's
adaptive guidance, `developer.android.com/develop/ui/compose/layouts/adaptive`, recorded as the page
`uiux.adaptive` points at). Concretely:

- **compact** → bottom navigation,
- **medium + expanded** → navigation rail, so the gallery gets the vertical space it needs,
- **and the Library/Project pair becomes a real list-detail** — `LibraryDestination` is `listPane`,
  `ProjectDestination` is `detailPane`. That single change turns `DetailPlaceholder` from a sentence into
  a correct two-pane view, and it is the only structural change that makes the imported
  `ListDetailSceneStrategy` do anything.

**`UNVERIFIED`:** whether `material3 1.4.0` on the pinned BOM exposes `NavigationSuiteScaffold` in a
stable form, and whether the slot's `androidx.compose.material3.adaptive.navigation3` imports are stable
at this version. What decides it: `./gradlew :app:dependencies` — **not to be run by hand while another
worker holds the slot** (`.cursor/rules/build-speed.mdc`), and never as part of this document's work.

---

## 4. One photo → one finished image

### 4.1 It is one flow, and the reason is in the owner's own words

«Здесь одна фото целая студия, дальше какие-то категории, потом опять мне что-то предлагается. Ничего
неинтуитивно, непонятно…» (`docs/owner-review-2026-10-16.md`, defect class 5). He is describing **a
set of separate tool screens**, which is what the app is: `Import → (choose one of two paths) →
AutoCard or Card → Editor | Cutout | SlideKit | ListingText | Export`, with the second path entered from
Home and its result landing on yet another destination. `docs/ui-critique-2026-10-16.md` finding 2
measured the worst part of it: **the main "Make card" action leads to `CardDestination`, the *non-AI*
path** (`ImportScreen.kt:150-154` → `PlayKilnApp.kt:203-215`, `CardScreen.kt:89-93`), while the AI path
is a different door.

So the framing is not a style preference. **Framing it as one guided flow is the fix for a measured
navigation defect.**

### 4.2 The bench, and its five stations

Name the thing a bench, because the identity is a plinth and the shell is a studio (the teardown's
phrase is *"a studio with four benches"*, §3 of that file). One destination, five stations, **one
persistent primary action whose label is the next station's name**:

| # | Station | The user does | Primary action |
|---|---|---|---|
| 1 | **Bring in** | pick the photo (or shoot it) | `Continue` → 2 |
| 2 | **Set the scene** | choose a **rendered** scene card | `Make the card` → 3 |
| 3 | **Making** | watches the card form (§4.3) | — (no action; the station is the progress) |
| 4 | **Keep the pixels** | compares before/after, adjusts | `Export` → 5 |
| 5 | **Ship it** | picks the marketplace pack | `Save the pack` |

Rules that make it one flow rather than five screens:

- **The station owns the screen, and the primary action never moves.** It is the same slot in the same
  place at every station, so muscle memory survives the transition — and its label is the *next* station,
  which is how the user knows where they are without a stepper.
- **One destination in the back stack per station.** A station is a `detailPane()` entry, so back goes to
  the previous station and never to the Home grid.
- **No station is reachable only by scrolling.** `H8` (`…visual-hierarchy-and-attention.md:815`) puts the
  primary action in the bottom third; `H9` bounds what else shares that bar.
- **A station that costs credits says so on the action**, in a clay pill beside the label (teardown §2.6
  and §4.2 own the balance; `docs/credits-model.md` owns the arithmetic). Cost attached to the choice is
  the measured competitor convention (`ON RECORD` — teardown §2.6: Pixelcut shows the credit cost *in the
  model picker, before generating*).

### 4.3 The progress state: a bounded shimmer, and the measured cause of the off-centre spinner

The owner saw «ползунок загрузки, он крутится не относительно центра. И меня вообще выбросило»
(`docs/owner-review-2026-10-16.md`). Two separate defects, both structural.

**The off-centre cause is measurable in the source.** `Column`'s default
`horizontalAlignment` is `Start`, so a `CircularProgressIndicator()` placed as a direct child of a
`Column` renders at the **leading edge** of the column, not the centre. That is exactly the shape of
`LibraryScreen.kt:165` and `:198`, `CardScreen.kt:92` and `:126`, `EditorScreen.kt:110`,
`SlideKitScreen.kt:308` and `PlayKilnApp.kt:532`. Where the author *did* remember, they wrapped it —
`Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(…) }`
(`AutoCardScreen.kt:318`, `ImportScreen.kt:239`, `SelectedMediaPreview.kt:51`). **So the app has both
idioms, chosen per call site by nothing.** A spinner is a two-line defect that a rule can end: it is
never a bare child of a layout; it is *always inside a bounded container whose shape is the result's*.

**The replacement**, in order of what it fixes:

1. **The shimmer is the result's own silhouette.** A skeleton box at the **exact aspect ratio the output
   will have**, with the placeholder in the `bench` tone and a `raised`-tone sweep — not a spinner in
   space. This is the `ON RECORD` Apple guidance (teardown §2.5.4) and it directly answers
   «не относительно центра»: there is nothing to be off-centre *in*.
2. **Progress is named in the app's own words, per step.** The app already has the strings
   (`autocard_recognition_progress`, `w3_composing`), and the prose is not the problem — the problem is
   that today they sit beside a 24 dp spinner in the action bar while the image area is empty.
3. **Stepped, not indefinite, where the steps are real.** Recognise → compose → shadow → done. A step
   that cannot be measured is a lie and must not be shown; `UNVERIFIED` which of the four are actually
   reportable by the pipeline — the run that decides it is a device run of the AutoCard flow with
   `android_logcat_capture` while `AutoCardScreen` is busy.
4. **Glow is the state boundary**, per §2.5: the sweep runs while the work runs and stops when the
   result arrives.

### 4.4 Never throw the user out

«И меня вообще выбросило, короче... Я не смог ничего сгенерировать.» The measured cause is one line:
`AutoCardDestination` is **popped and re-pushed** to regenerate —
`backStack.pop(); val step = AutoCardDestination(...); if (backStack.lastOrNull() != step)
backStack.push(step)` (`PlayKilnApp.kt:280-285`, reached from `onRegenerate`). A pop-then-push is a
navigation reset: the station's state is destroyed, the user's place in it is destroyed, and on a
non-compact window the panes re-animate.

**The rule:** a failed or cancelled generation is an *in-screen state of the station it happened in* —
never a navigation event. The station keeps the user's scene selection, their category, and their
comparison, and offers `Try again` in place. This is `H22` applied to an action
(`…visual-hierarchy-and-attention.md:926` — *"a per-item action is inside or immediately adjacent to its
item's container"*).

### 4.5 The result must be a destination with a handoff

Two `MEASURED` defects, both already named in `docs/ui-critique-2026-10-16.md`:

- the result screen tells the user *"It has not been saved as a project output"* and offers no save or
  send (`PlayKilnApp.kt:609-700`; `res/values/strings_home_nav.xml:31-34`), and
- the screen has **no package of its own** (§0.6).

Under this spec the result is **station 4–5 of the bench**, it is a real file
(`ui/result/`), and its handoff is `data_out` — the promise `export-pack-per-marketplace` is already in
the contract with family `data_out`, so the visual language's job is to make the handoff *visible on the
result screen* rather than one destination away.

---

## 5. The Plinth token set

Every value below is either a **declared** palette colour or a `lerp` of two declared colours, computed
by `python .kiln/scratch/plinth-contrast.py`. **No new hue is introduced**, so `Color.kt` and
`store/studio.json` remain the single source of truth as `.cursor/rules/visual-identity.mdc` requires.

### 5.1 The colour contract — one change, or none of it

**Adopt this table as a whole.** Implementing §2.3 alone would set `surfaceContainerHigh` to the warm
bench tone and drop `onSurfaceVariant` from **4.966:1** (on `Paper`) to **4.465:1** — under the 4.5:1
floor — which would ship an AA failure. That is §5.5 of the colour doc's point made concrete, and it is
why the surface ladder and the ink roles are one decision.

Contrast battery, `python .kiln/scratch/plinth-contrast.py`, WCAG 2.2 AA floors (**4.5** text / **3.0**
non-text, `H19`):

| pair | ratio | verdict |
|---|---:|---|
| `Ink` on `Paper` (page) | **11.787:1** | PASS |
| `Muted` on `Paper` (page) — `onSurfaceVariant` today | **4.966:1** | PASS |
| `Muted` on bench `#E4D4C6` | **4.465:1** | **FAIL** |
| `Muted` on raised `ClayPale` | **4.035:1** | **FAIL** |
| **derived** `#4B4A46` on bench | **6.139:1** | PASS |
| **derived** `#4B4A46` on raised | **5.548:1** | PASS |
| `VerdigrisDeep` on `Paper`; and `Paper` on `VerdigrisDeep` | **5.866:1** | PASS |
| `Verdigris` on `Paper` | **2.929:1** | **FAIL** — so `Verdigris` is **never** light-mode text, and not even a 3:1 boundary |
| `Clay` on `Paper` / on bench | **3.440:1** / **3.093:1** | **FAIL** as text; PASS as a 3:1 rim |
| `ClayDeep` on `Paper` | **6.430:1** | PASS — this is the credit ink |
| `Ink` on `ClayPale` — `onSecondaryContainer` | **9.577:1** | PASS |
| dark: `Paper` on `InkSoft` | **9.460:1** | PASS |
| dark: **derived** `#BDB8AE` on `InkSoft` | **6.222:1** | PASS |
| dark: `Bezel` on `Verdigris` (primary fill) | **5.188:1** | PASS |
| dark: `Verdigris` on `Ink` | **4.024:1** | **FAIL** as text (PASS 3.229:1 as a ring on `InkSoft`) |
| dark: `ClayPale` on `ClayDeep` | **5.224:1** | PASS |
| dark: `BezelRim` on `InkLift` — `outline` today | **2.589:1** | **FAIL** (SC 1.4.11) |
| dark: **derived** `#A6A298` on `InkLift` | **3.660:1** | PASS |

**Two corrections this table forced on my own first draft, reported rather than hidden.** (a) A
`lerp(Muted, Paper, 0.72)` "hairline" is `#C2BCB1`, which measures **1.307:1** on the bench — too faint
for a component boundary, so it is a **divider tone only** and a perceivable edge uses `outline`
(`Muted`, 4.465:1 on the bench). (b) The dark `outline` must be **lightened**, not left as `BezelRim`.

Roles to set, in `ui/theme/Theme.kt`:

| role | light | dark | why |
|---|---|---|---|
| `surfaceContainerLowest`, `surfaceContainerLow` | `Paper` | `Ink` / `#363C3D` | the page |
| `surfaceContainer`, `surfaceContainerHigh` | `#E4D4C6` | `#3C4243` / `InkLift` | the bench — where a card or a stage sits |
| `surfaceContainerHighest`, `secondaryContainer` | `ClayPale` | `#575B58` / `ClayDeep` | the raised field; already the `PlinthStage` stop |
| `surfaceVariant` | `#E4D4C6` (**not** `Paper`) | `InkLift` | today `surface == surfaceVariant` (§2.3) |
| `onSurfaceVariant` | `Muted` **only on the page** | `#BDB8AE` | 4.966:1 on the page is correct; 4.465:1 on a card is not |
| **`onSurfaceVariantRaised`** (slot token, not an M3 role) | `#4B4A46` | `#BDB8AE` | the ink for any text over bench/raised. `PlinthStage.kt:69` needs it today |
| `outline` | `Muted` | `#A6A298` | component boundaries ≥ 3:1 |
| `outlineVariant` | `#C2BCB1` | `#50524E` | dividers only — **never** a component boundary |
| `surfaceTint` | `surface` (i.e. off) | `surface` | see below |
| `error`, `onError`, `errorContainer`, `onErrorContainer` | framework baseline | framework baseline | see below |

**`surfaceTint` must be set to `surface`, and this is not cosmetic.** M3's `surfaceTint` defaults to
`primary` — here `VerdigrisDeep`, a green — and it is what elevation-based surface colour helpers blend
in. Left unset, every component that applies tonal elevation shifts the warm travertine page **toward
green**. Setting it to `surface` makes "no tint" the decision and leaves tone to the palette's own warm
ladder, which is the whole of §2.2. **`UNVERIFIED`:** the exact `surfaceTint` default is read from
`lightColorScheme()`'s parameter default rather than from the artifact in this session.

**`error` stays the framework baseline, and the reason is a bound `studio.json` already set.** Its
palette semantics reserve verdigris for *"guarantees about the item or passing rules"* and clay for
*"charged credits"*. An alarm colour is neither, and inventing a red would break either the reservation
or the floor. But the four error roles are currently **unset**, and `docs/research/2026-10-02-color-systems-and-palette-derivation.md`
§6.5 records why that matters: M3's `error` *"ships with `onError`, `errorContainer` and
`onErrorContainer` because it needs the same fill/ink/container structure as any other family"*. **Set
all four explicitly to the framework baseline values** (resolved from the scheme, so they are a decision
on the record rather than an accident), and check the two pairings against 4.5:1. This is the one place
the palette is deliberately *not* extended: it is a semantic alarm, not an identity colour.

### 5.2 Where the accent is spent

Two accents, and the semantics are `MEASURED` from `store/studio.json` (palette semantics: *"Verdigris
is reserved for guarantees about the item or passing rules; clay is reserved for charged credits"*).

| accent | spent on | never spent on |
|---|---|---|
| **Verdigris** (`VerdigrisDeep` fill, `Paper` ink) | the one primary action of a screen; provenance/proof affordances (`ProofRing`); a **passing** marketplace rule; the selected-state indicator | text in light mode (2.929:1, §5.1); a decorative field; a cost |
| **Verdigris in dark** (`Verdigris` fill, `Bezel` ink) | fills and ≥ 3:1 rings (`ProofRing` on `InkSoft`, 3.229:1) | body text (4.024:1 on `Ink`) |
| **Clay** (`ClayPale` fill, `Ink` ink; `Clay` rim; `ClayDeep` ink) | the balance chip, credit cost pills, the reserve/settle ledger state, a **failing** marketplace rule | the primary action; an error; decoration |

`Clay` itself measures 3.440:1 on `Paper` — **it is a rim and a pill fill, never a text ink**;
`ClayDeep` at 6.430:1 is the ink. **`Clay` is currently used in exactly one place in the slot**
(`ui/settings/StudioTryout.kt:180`) and appears in `Theme.kt` nowhere — so the credit accent that
`studio.json` reserves is, today, absent from the app. That is the measured gap that made the owner ask
for *one balance* (`docs/owner-review-2026-10-16.md`, class 4; `docs/credits-model.md` owns the fix).

And `H6` bounds the accent: **≤ 2 `primary`-filled surfaces per screen** (`…visual-hierarchy-and-attention.md:802`).
With the chrome spending one on the create action, a screen has **one** verdigris fill left for its own
primary action. A screen with two green buttons has none.

### 5.3 Shape

One scale, adopted. `MaterialTheme.shapes` is the single source; a literal
`RoundedCornerShape(n.dp)` in a screen file is a finding (§0.4 lists the ten that exist today).

| token | dp | used for | today's ad-hoc equivalent it replaces |
|---|---:|---|---|
| `extraSmall` | 4 | a rim on a swatch, a rule | `EditorScreen.kt:301` (5 dp) |
| `small` | 8 | a chip, a colour dot | — |
| `medium` | 12 | an input, a small tile | `HomeScreen.kt:514` (14 dp) |
| `large` | 16 | a **base** card, a scene card, a list row | `PlinthStage.kt:64` (18 dp), `HomeScreen.kt:427,461,491` (20 dp) |
| `largeIncreased` | 24 | a station panel, a photo tile | `HomeScreen.kt:386,418` (22/24 dp) |
| `extraLarge` | 28 | the stage | `HomeScreen.kt:506` (26 dp) |
| `extraLargeIncreased` | 32 | the bench stage only — one per screen | `PlinthStage.kt:26,35` (30 dp) |
| `full` | 50 % | the balance chip, the create action, the selected indicator | — |

**The rule that matters more than the table:** the *larger* radius means the *larger* region. A pill and a
30 dp panel are not two ways to say "card"; they are two statements about size. The app's current mix has
a 30 dp stage next to a 26 dp card next to a 5 dp swatch, chosen per call site, which is a vocabulary with
no grammar.

### 5.4 Type

Adopt `H3`: **≤ 6 distinct sizes and ≤ 4 content tiers per screen**
(`docs/research/2026-10-02-visual-hierarchy-and-attention.md:777`).

Declare all twelve styles the app uses, so §0.5's three undeclared styles stop falling through — and
keep `tnum` on every one of them, because every one of them can carry a credit count, a pixel dimension
or a file size.

| style | size/lh | weight | tracking | tier |
|---|---|---|---|---|
| `displaySmall` | 36/44 | SemiBold | 0 | focal, once |
| `headlineMedium` | 28/36 | SemiBold | 0 | focal, once |
| `headlineSmall` | 24/32 | SemiBold | 0 | **add** — a station title |
| `titleLarge` | 22/28 | SemiBold | 0 | content |
| `titleMedium` | 16/24 | Medium | 0.15 | content |
| `titleSmall` | 14/20 | Medium | 0.10 | **add** — used at `AutoCardScreen.kt:599` |
| `bodyLarge` | 16/24 | Normal | 0.5 | body |
| `bodyMedium` | 14/20 | Normal | 0.25 | body |
| `bodySmall` | 12/16 | Normal | 0.4 | **add** — a caption |
| `labelLarge` | 14/20 | SemiBold | 0.10 | label |
| `labelMedium` | 12/16 | Medium | 0.5 | **add** — used at `AutoCardScreen.kt:609`, `PlinthStage.kt:69` |
| `labelSmall` | 11/16 | Medium | 0.5 | **add** — used at `AutoCardScreen.kt:586` |

**Family: keep `FontFamily.SansSerif`.** Identity comes from weight, tracking and tabular numerals, not
from a font file — and a vendored font would add a licence and provenance obligation
(`.cursor/rules/visual-identity.mdc`, `docs/icon-provenance.md`'s regime) for a gain this document cannot
demonstrate. `H4` still applies: two sizes in the same family on one screen must differ by **≥ 5 %**
(16 vs 14 is 1.143 — fine; 12 vs 11 is 1.09 — fine).

### 5.5 Spacing

4 dp base, 8 dp for layout gaps (`H12`, `…visual-hierarchy-and-attention.md:852`). Named steps, all
multiples of 4: **4 · 8 · 12 · 16 · 20 · 24 · 32 · 48**. Screen gutter **16**, station panel padding
**20**, within-group gap **8**, between-group gap **≥ 16** (`H11`'s 2× floor, kept labelled as
`CONVENTION`), a list's last item reachable above the bar via the **scrollable's own `contentPadding`**
— never `padding(bottom = 48.dp)` guessing a bar height
(`…visual-hierarchy-and-attention.md:606-620` and `.cursor/rules/ui-ux-baseline.mdc`).

Dividers: **0 where the adjacent gap is already ≥ 16 dp** (`H10`, `:833`), because
*"spacing creates logical sections of content on a page without having to use lines"* (Fluent 2,
`fluent2.microsoft.design/layout`, `ON RECORD` through that doc).

### 5.6 The imagery-density rule

Three countable rules. They are `INFERRED` from the measured defects in §0 and from the convergent
competitor pattern (`docs/redesign/competitor-teardown.md` §2.3, §2.7); **no program measures them yet**
(§7 states that limit and names the check that could).

1. **The word budget before the first image.** At most **12 words of static prose** may appear above the
   first image-bearing surface, which must be **within the first 320 dp** of the content area. "Image-
   bearing" means a composable that paints the user's own pixels or a scene/preset **rendered from its
   own recipe** — a vector glyph does **not** qualify and neither does a colour swatch.
2. **At least one image-bearing surface visible without scrolling** on every screen whose subject is a
   photo, a scene, a pack or a project: Bring in, Set the scene, Making, Keep the pixels, Ship it,
   Library, Project, Group, Looks, Batch, Cutout.
3. **Gallery density: ≥ 2 cards visible at once** in any horizontal preset/scene strip. This one is
   already satisfied in the code and only needs its thumbnails — `SceneChoice` sets
   `widthIn(min = 164.dp, max = 220.dp)` (`AutoCardScreen.kt:566`).

**The single highest-leverage application, measured.** The scene gallery's data already contains
everything needed to draw a faithful miniature: `color`, `color2`, `angleDeg`, `shadow.opacity`,
`shadow.blurPct`, `shadow.offsetPct`, `fillTarget`, `marginPct` — 124 recipes, read from
`assets/autocard/scene-recipes.json`. Rendering a scene card as **that field plus a neutral silhouette
at `fillTarget`** (a `Canvas` or a `drawBehind`) converts the current swatch+label into the imagery the
competitor idiom requires, **with no new asset and no new data**. For a `GENERATED` recipe there is no
palette and today's answer is the string `w3_scene_no_preview`; the fix is to render the *category's*
field with a sparkle-state marker, or to stop offering unrenderable recipes in a *visual* gallery —
either way the user must never be shown a card whose content is the sentence "no preview".

### 5.7 Motion

Available on the pin: `androidx.compose.animation.core` springs, `AnimatedContent`, `AnimatedVisibility`,
`Crossfade`, `animateContentSize`, `Modifier.animateItem`, `SharedTransitionLayout`, `Animatable`,
`updateTransition` — the list `uiux.motion` already accepts (`.cursor/rules/ui-ux-baseline.mdc`). **Not
available:** M3 Expressive's `MotionScheme` (§2.1).

| moment | vocabulary | duration/feel |
|---|---|---|
| station → station | `NavDisplay`'s transition, shared element where a thumbnail becomes the station's hero | spring, no linear |
| the image arriving | the shimmer fades out **as** the result fades in, cross-phased, never an instant swap | one beat |
| a scene card selected | the rim fills; the indicator scales ~1.0 → 1.06 | spring, ~150 ms |
| the balance changing | the number animates **and the state is named** (reserved → settled → refunded) | ~250 ms |
| a failed run | the station does not move (§4.4); the failure appears in place | — |
| anything else | **no motion** | — |

`H24`/`H25` still bind the money: a purchase surface is never the first destination
(`…visual-hierarchy-and-attention.md:944-957`), and *"the job's primary action outranks the purchase"*.

### 5.8 Component vocabulary

The rule from `.cursor/rules/visual-identity.mdc`: a component earns its place by encoding a decision the
app makes **repeatedly**. Plinth has three today — `PlinthScaffold` (a correct edge-to-edge scaffold,
used by 12 screens), `PlinthStage` (the gradient stage) and `ProofRing` (used once). It needs these:

| component | encodes | replaces |
|---|---|---|
| `PlinthStage` (fix §0.1) | "this is the bench" | its own framework-default gradient stop |
| **`SceneCard`** | a scene recipe as an image | `SceneChoice`'s swatch + `"Solid 1"` label (`AutoCardScreen.kt:553-613`) |
| **`CompareSlider`** | *the item's pixels are untouched* (§1.2 device 3) | two `FilterChip` pairs on two screens (`AutoCardScreen.kt:283-289`, `EditorScreen.kt:146-157`) |
| **`ResultSkeleton`** | the result's own shape while it is made (§4.3) | 10 bare/mis-centred progress indicators (§6). **Half-built already**: `ui/autocard/FormingCardPreview.kt` has the shape, the centring and the fade — it needs to be **retone** (its two shimmer stops are unset framework roles, §0.1) and **generalised** to the other stations rather than rewritten |
| **`ProofRing`** (promote) | provenance on the AI path | used once, at `CardScreen.kt:99` |
| **`CreditPill`** | a cost, before the commitment | per-row cost text (`StudioToolsScreen.kt`'s `StudioOperationRow`) |
| **`BalanceChip`** | the one balance | one row per catalog consumable plus one for the unlock, each carrying its own localized price (`WalletScreen.kt:259-286`) |
| **`PrimaryBar`** | the persistent action, the *next station's* name | `Surface(shadowElevation = 6.dp)` Paper-on-Paper (`ImportScreen.kt:106`, `AutoCardScreen.kt:305`) |

### 5.9 Iconography

Unchanged from the factory rule, restated only so a builder does not have to guess: every
general-purpose glyph — back, plus, bin, gear, empty state — comes from **one** licence-cleared set,
vendored with its licence text and a per-icon provenance record (`docs/icon-provenance.md`), tinted from
the theme, carrying no colour of its own. `res/drawable` already holds 21 vectors
(`ic_lucide_*` plus the three launcher layers and `plinth_mark`), so the family is in place; the mark
stays slot-owned as `.cursor/rules/visual-identity.mdc` requires. **The one defect this spec adds:** the
Home screen's wallet action is drawn with the **plus** glyph —
`IconButton(onClick = onOpenWallet) { Icon(painterResource(R.drawable.ic_lucide_plus), …) }`
(`ui/home/HomeScreen.kt:144-148`). A `+` means *create*; the balance is not a create action, and it is
the strongest single affordance mismatch in the app.

---

## 6. Anti-patterns, with the screen where each one occurs

Each row names the *mechanism*, because a rule an engineer cannot act on is an opinion.

| # | Anti-pattern | Where, measured | Why it reads as a notepad | The replacement |
|---|---|---|---|---|
| A1 | **A page of label/button/label/button** | `StudioRoute.kt` 46 `Text(` / 3 image refs; `StudioToolsScreen.kt` 36/1; `WalletScreen.kt` 38/**0**; `BatchScreens.kt` 43/**0**; `GroupScreen.kt` 25/**0** | every element is the same weight and the same shape, so nothing is rank-1 (`H1`) and the eye serial-reads a screen that was meant to be glanced at | §5.6's density rule + one rank-1 image-bearing surface + `H3`'s tier ceiling |
| A2 | **Text-only selection** | the scene gallery: `SceneChoice` (`AutoCardScreen.kt:553-613`) renders a flat gradient swatch of the recipe's own colour and the label `w3_scene_solid/gradient/studio/generated` + index — i.e. **"Gradient 1"**; an unrenderable recipe shows `w3_scene_no_preview` (`AutoCardScreen.kt:586`) | the user is asked to choose an *image* by reading a *word*, which is exactly «Абсолютно ничего не видно» | render the recipe's own field + silhouette from data that already exists (§5.6) |
| A3 | **A bare `TopAppBar` title with no back affordance, or a back affordance that is not in the bar** | `WithShellBack` (`PlayKilnApp.kt:867-882`) is a **text `TextButton`** in a `Column` padded by `statusBarsPadding()`, floating above the content of Cutout (`:236`), Studio (`:481`), SlideKit (`:399`), Library (`:310`) and Group (`:347`); `StudioRoute.kt:91`, `StudioToolsScreen.kt:108`, `SlideKitScreen.kt:148`, `CutoutScreen.kt:164` declare `TopAppBar(title = { … })` with **no `navigationIcon`** | a text link where the platform puts an arrow, drawn in the region the status bar occupies — the owner's «плашка уведомления мне перекрывает» | a real `TopAppBar` with an icon-only nav action (§3.3); insets stay `ui-ux-baseline.mdc`'s |
| A4 | **A bare `CircularProgressIndicator` with no context, and off-centre** | **10 sites**: `LibraryScreen.kt:165,198`; `CardScreen.kt:92,126`; `EditorScreen.kt:110`; `SlideKitScreen.kt:308`; `PlayKilnApp.kt:532,682`, `MarketplaceLinterPanel.kt:71`, `ReportOutputAction.kt:169`; plus `LinearProgressIndicator` at `StudioRoute.kt:210` and `BatchScreens.kt:159` | a bare child of a `Column` inherits `horizontalAlignment = Start` → **renders at the leading edge**; and a spinner says *something is happening* without saying *what is forming* | `ResultSkeleton` bounded to the result's aspect ratio (§4.3); `H22`: the indicator sits inside the thing it is about |
| A5 | **The primary action reachable only by scrolling, or sitting on a Paper-on-Paper bar** | `Surface(shadowElevation = 6.dp)` with **no `color`** (`ImportScreen.kt:106`, `AutoCardScreen.kt:305`) → in light mode `surface == surfaceVariant == Paper`, contrast vs the page **1.000:1**; the owner found the button by accident: «А, вот кнопка, она оказывается в самом низу была» | a bar separated from the page by a grey shadow over warm paper is invisible on a bright screen | `PrimaryBar` with the bench tone + a top hairline (§3.2, §5.8) |
| A6 | **The AI result is a preview with no handoff** | `PlayKilnApp.kt:609-700` (`AutoCardResultScreen`, a private composable in the nav module) + `res/values/strings_home_nav.xml:31-34`, measured in `docs/ui-critique-2026-10-16.md` finding 1 | the flow teaches the user that the app cannot finish | station 4–5 of the bench, in `ui/result/`, ending in `data_out` (§4.5) |
| A7 | **The main path leads to the non-AI screen** | `ImportScreen.kt:150-154` → `PlayKilnApp.kt:203-215` → `CardScreen.kt:89-93`, measured in `docs/ui-critique-2026-10-16.md` finding 2 | «Понятно, что это AI приложение» — and the app's own front door is not it | the bench's station 2 (§4.2) |
| A8 | **Raw platform list/`FilterChip` grid as the AI's chooser** | the category picker: a `Surface` of two `Text` lines (`AutoCardScreen.kt:474-488`) opening a `ModalBottomSheet` of `FilterChip`s grouped by headings (`AutoCardScreen.kt:536-546`) | the owner named it exactly: «Банальный самый встроенный какой-то механизм Android» | a visual preset/stage chooser; the sheet may remain as the overflow |
| A9 | **A tool offered as text about itself** | `StudioOperationRow` (`StudioToolsScreen.kt:314-345`) is a title plus a fixed stack of prose — a provenance line (`:334`), a cost line (`:335`), an availability line (`:337`) — and the detail pane is four more `Text`s in a `verticalScroll` Column (`:204-208`); the cost line (`costLineResource`, `:447`) renders the cost **and both balances as a sentence** | an AI operation is sold by showing what it does to *this* product. A row that describes provenance and prints an arithmetic sentence asks the user to trust a paragraph; nothing on the screen renders the operation's output | the operation's human name + a rendered example of its output on the user's own photo |
| A10 | **The signature component is a gradient between the framework and the app** | `PlinthStage.kt:28-31` interpolates `surfaceContainerHigh` (**unset**, §0.1) → `secondaryContainer` → `surface`; `.shadow(10.dp, …)` at `:26` | a hero that fades from violet-grey to travertine reads as unstyled even when the palette is deliberate | §5.1, §2.3 |
| A11 | **A wide window shows a sentence** | `DetailPlaceholder` → `R.string.home_pick_section` (`PlayKilnApp.kt:886-892`), the only `listPane` in the app | an adaptive layout that adapts to nothing | Library/Project become a real list-detail (§3.4) |
| A12 | **Ten radii from two scales** | §0.4 | `H1`'s failure applied to shape: when every corner is a different size, no corner means anything | §5.3 |
| A13 | **Three undeclared text styles carrying `tnum`-less numerals** | §0.5 — `labelSmall`, `labelMedium`, `titleSmall` | a credit count is tabular in one label and proportional in the next, on the same screen | §5.4 |
| A14 | **A `+` glyph on the wallet** | `HomeScreen.kt:144-148` | the strongest affordance in the app promises to create something and sells credits | §5.9 |

---

## 7. Honest limits of this document

Stated plainly rather than implied, because `AGENTS.md`'s standard is that a rule claiming more than it
delivers is the defect.

- **Nothing here was seen on a device.** Every claim about pixels, contrast *as rendered*, animation
  quality and perceived modernity is `UNVERIFIED`. The run that decides it is the one
  `.cursor/rules/device-testing.mdc` already requires: install the debug APK on the test phone, drive
  the bench end to end, and photograph it. **The serial in that rule is `R58MB3SGZVL`**
  (`SM-A405FM`, the A40) — and §1.3 of this document mis-typed it once before self-correcting, which is
  a reminder that the serial must be read from `adb devices`, not remembered.
- **No program measures this spec.** `.cursor/rules/ui-ux-baseline.mdc`'s `uiux.*` checks read Kotlin and
  XML for *causes*; the imagery-density rule (§5.6), the word budget, the accent budget and the tier
  ceiling have **no sensor**. The check that could exist is closest to
  `scripts/core/uiux.mjs`'s shape — a source scan that counts image-bearing composables per screen and
  reports the first one's position — and the honest form of it is a **report, not a gate**, for the
  reason `docs/research/2026-10-02-depth-encoding-and-detectability.md` records: a lexical predicate on
  layout intent produces false positives and a noisy gate is read past.
- **The performance claims in §1.2 device 8 and the shimmer are unmeasured** on this app. The previous
  run's measurements are in `docs/owner-review-2026-10-16.md` ("What this review does not say") and they
  are about pack preparation and a main-thread freeze, not about rendering a skeleton.
- **One source in this redesign's own corpus is broken, and I do not rely on it.** The competitor teardown
  cites `docs/design/midjourney-DESIGN.md` for Midjourney's shimmer placeholder
  (`docs/redesign/competitor-teardown.md` §2.5.4). **`docs/design/` does not exist in this tree and no
  file whose name contains `midjourney` exists anywhere in the repository** (`Test-Path docs/design` →
  `False`; `Get-ChildItem -Recurse -Filter '*midjourney*'` → nothing). Every Midjourney-specific claim is
  therefore excluded from this document, and the shimmer argument rests only on the Apple guidance.
- **The `ON RECORD` chain is a chain.** Where this document says a page says something, the reader's
  guarantee is *this repository recorded reading it*, not that I fetched it. The pages are named so the
  next reader can shorten the chain.
- **This document does not decide the tab set, the balance arithmetic, the settings structure, the import
  flow or the competitor analysis.** Those are `docs/redesign/competitor-teardown.md`,
  `docs/credits-model.md`, `docs/settings-ia.md` and `docs/import-smartcard-ux.md`.

### Claim ledger

| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| `Theme.kt` sets 24 colour roles and no `surfaceContainer*` role | MEASURED | `ui/theme/Theme.kt:13-58` |
| The slot's code references `surfaceContainer*` 18 times across 7 files | MEASURED | `rg -c 'surfaceContainer(Lowest\|Low\|High\|Highest)?' app/src/main/java/…/ui` → `HomeScreen.kt:5`, `AutoCardScreen.kt:3`, `FormingCardPreview.kt:3`, `WalletScreen.kt:3`, `SelectedMediaPreview.kt:2`, `StudioToolsScreen.kt:1`, `PlinthStage.kt:1` |
| `Color.kt` assigns **no** `surfaceContainer*` role, and `Theme.kt` appears in none of the counts above — so all 18 sites resolve to the Compose framework's baseline scheme | MEASURED | `rg -n surfaceContainer ui/theme/Color.kt` → no matches; `Theme.kt` absent from the `rg -c` file list |
| `ui/autocard/FormingCardPreview.kt` already implements §4.3's bounded-skeleton shape (aspect-ratio bound, `shapes.extraLarge` clip, centred content, `liveRegion` announcement, 350 ms fade) and is wired at `AutoCardScreen.kt:499-501` | MEASURED | `ui/autocard/FormingCardPreview.kt:43-51,56-58,80-81,107`; `AutoCardScreen.kt:499-501` |
| That new component's shimmer interpolates between the two unset roles, so the sweep is framework-toned rather than bench-toned | MEASURED | `ui/autocard/FormingCardPreview.kt:80-81` (`surfaceContainerHigh` → `surfaceContainerHighest`) against the `Color.kt` non-assignment row above |
| A three-tab shell (`STUDIO / LIBRARY / MORE`) with a bottom bar and a rail **is already implemented** | MEASURED | `ui/components/PlinthNavBar.kt:31` (`PlinthBottomNavBar`), `:54-56` (the three sections), `:85` (`PlinthNavigationRail`), `:109-111` |
| `BalanceChip` and `PlinthCostChip` already exist, so this document must not propose parallel components | MEASURED | `ui/components/PlinthTopBar.kt:53`; `ui/components/PlinthCostChip.kt:15` |
| **None** of those components has a call site outside its own file — the shell is unwired | MEASURED | `rg -n 'PlinthBottomNavBar\|PlinthNavigationRail\|BalanceChip\|PlinthCostChip\|PlinthToolCard\|PlinthTopBar\|PlinthSection' app/src/main/java/…/plinthproductphotos` → definitions and self-references only |
| `PlayKilnApp.kt` imports one new component (`PlinthScaffold`) and `NavigationSuiteScaffold` appears nowhere in the slot | MEASURED | `ui/PlayKilnApp.kt:88` (import), `:329,338,362,597,660,793` (uses); `rg -n NavigationSuiteScaffold app/src/main` → no matches |
| The `H1`–`H25` hierarchy rules this document cites exist with numeric thresholds | MEASURED | `docs/research/2026-10-02-visual-hierarchy-and-attention.md`: H1 `:764`, H3 `:777`, H15 `:875`, H19 `:904`, H22 `:926`, H25 `:953` — all read in this session |
| `competitor-teardown.md` (the source for every external `ON RECORD` row above) exists and its §1.1/§2.1/§2.3/§2.5 carry the cited readings | MEASURED | `docs/redesign/competitor-teardown.md` (58 933 bytes at time of reading). **Cited by section, not by line**, because another worker edited this file during this session (mtime 23:50) — section numbers survive an edit, line numbers do not |
| Their unset values fall to the framework baseline, which is purple-seeded | ON RECORD | `m3.material.io/styles/color/roles`; the exact baseline hex for `material3 1.4.0` was not read — UNVERIFIED |
| `PlinthStage` interpolates `surfaceContainerHigh → secondaryContainer → surface` with a 10 dp shadow and a 30 dp radius | MEASURED | `ui/components/PlinthStage.kt:26,28-31,35` |
| `res/` holds 21 drawables, all vector XML, and zero raster images | MEASURED | `Get-ChildItem -Recurse -File res \| Where Extension -in '.png','.webp','.jpg'` → 0; `res/drawable` → 21 |
| Text-to-image reference ratios per UI file (table §0.3) | MEASURED (proxy) | `rg -c '\bText\('` and `rg -c 'Image(\|AsyncImage\|painterResource\|BitmapPainter\|asImageBitmap'` over `app/src/main/java/…/ui`; a count is a proxy for a render, not a render |
| Ten distinct corner radii are in use, from two scales | MEASURED | `rg -n 'RoundedCornerShape\([0-9]+'` and `rg -n 'shapes\.(large\|extraLarge)'` |
| `Type.kt` declares 8 styles; `labelSmall`, `labelMedium`, `titleSmall` are used but undeclared | MEASURED | `ui/theme/Type.kt:9-63`; uses at `AutoCardScreen.kt:586,599,609`, `PlinthStage.kt:69`, `WalletScreen.kt:193` |
| `AutoCardResultScreen`, `ProjectWorkspaceScreen`, `BatchDestinationRoute` are private composables in `PlayKilnApp.kt` | MEASURED | `PlayKilnApp.kt:609,787,826` |
| There is no `NavigationBar` / `NavigationRail` / `NavigationSuiteScaffold` in the slot | MEASURED | `rg 'NavigationBar\|NavigationRail\|NavigationSuiteScaffold'` → no match |
| 19 of 20 destinations are `detailPane()` and only Home is `listPane` | MEASURED | `PlayKilnApp.kt:189,210-491` |
| A `CircularProgressIndicator()` as a direct `Column` child renders at the leading edge | INFERRED | `Column`'s default `horizontalAlignment` is `Start` (Compose `Column` API); the shape recurs at `LibraryScreen.kt:165,198`, `CardScreen.kt:92,126`, `EditorScreen.kt:110`, `SlideKitScreen.kt:308`, `PlayKilnApp.kt:532,682` |
| `await` — the app's action bars use `Surface(shadowElevation = 6.dp)` with no `color`, so `surface` on `surface` | MEASURED | `ui/import/ImportScreen.kt:106`, `ui/autocard/AutoCardScreen.kt:305`; `Theme.kt:28,30` sets both to `Paper` |
| The light warm axis spans 1.231:1 and is two practically visible steps | MEASURED | `python .kiln/scratch/plinth-contrast.py` → page `#E8E1D5` 0.75810, bench `#E4D4C6` 1.112:1, raised `#E0C8B9` 1.231:1 |
| Dark mode carries three visible steps (1.364 / 1.097 / 1.097 / 1.351) | MEASURED | same command |
| `Muted` fails 4.5:1 on a raised surface (4.465 / 4.035) and `#4B4A46` fixes it (6.139 / 5.548) | MEASURED | same command |
| Light `Verdigris` on `Paper` is 2.929:1, so it is never light-mode text | MEASURED | same command |
| `Clay` is 3.440:1 on `Paper`; `ClayDeep` is 6.430:1 | MEASURED | same command |
| Dark `outline` (`BezelRim`) on `InkLift` is 2.589:1, below SC 1.4.11's 3:1; `#A6A298` is 3.660:1 | MEASURED | same command; floor from `H19` (`…visual-hierarchy-and-attention.md:907`) |
| A `lerp(Muted, Paper, 0.72)` "hairline" is only 1.307:1 on the bench | MEASURED | same command — reported because it refuted this document's first draft |
| `surfaceTint` defaults to `primary`, so unset elevation tints the warm page green | ON RECORD + UNVERIFIED | M3 `ColorScheme.surfaceTint` is documented as the tint applied to elevated surfaces and `lightColorScheme()` defaults it to `primary`; the resolved default was not read from the artifact this session |
| Clay is used exactly once in the slot and never in `Theme.kt` | MEASURED | `rg -n 'PlinthPalette\.Clay\b'` → `ui/settings/StudioTryout.kt:180` only |
| The wallet affordance on Home is the `+` glyph | MEASURED | `ui/home/HomeScreen.kt:144-148` |
| The studio operation row is prose about the operation, not an example of its output | MEASURED | `ui/studio/StudioToolsScreen.kt:314-345` (title + provenance `:334` + cost `:335` + availability `:337`); detail pane `:204-208` |
| `wireValue` is **not** rendered as a visible row label — my earlier draft said it was, and that was wrong | MEASURED | `rg -n wireValue ui/studio/StudioToolsScreen.kt` → `:166` (a list key), `:391`, `:443` (both `error(...)` messages) — no UI call site |
| 124 scene recipes exist, each with colour/angle/shadow/fill/margin | MEASURED | `Select-String '"sceneId"' app/src/main/assets/autocard/scene-recipes.json` → 124; first records read in full |
| The scene gallery renders a swatch and a kind+index label, and `w3_scene_no_preview` for generated recipes | MEASURED | `AutoCardScreen.kt:553-613`, `:586`, `:589-601`; `res/values{,-ru}/strings_import_smartcard.xml:19` |
| A before/after toggle exists on two screens as `FilterChip` pairs | MEASURED | `AutoCardScreen.kt:283-289`; `EditorScreen.kt:146-157` |
| `item-pixels-untouched` is a `core` promise in this slot's contract | MEASURED | `docs/plinth-product-photos.differentiators.json:12-15` |
| Regenerate pops and re-pushes the destination | MEASURED | `PlayKilnApp.kt:280-285` |
| The main "Make card" action leads to `CardDestination`, not the AI path | MEASURED | `docs/ui-critique-2026-10-16.md` finding 2, from `ImportScreen.kt:150-154`, `PlayKilnApp.kt:203-215`, `CardScreen.kt:89-93` |
| `ProofRing` is used exactly once, on the non-AI path | MEASURED | `rg -n 'ProofRing\('` → `CardScreen.kt:99` and its declaration |
| M3 Expressive and `MotionScheme` are `material3 1.5.0-alpha*`, absent from the pinned BOM, and `uiux.platform` fails on them | ON RECORD | `.cursor/rules/ui-ux-baseline.mdc` §"The modern platform", citing `docs/research/2026-10-02-modern-android-platform-surface.md` §6 |
| Elevation is tone, not shadow, in M3 | ON RECORD | `docs/research/2026-10-02-color-systems-and-palette-derivation.md` §5.4 → `m3.material.io/blog/tone-based-surface-color-m3` |
| M3 defines five surface-container levels | ON RECORD | same page, cited at `docs/research/2026-10-02-visual-hierarchy-and-attention.md:863` |
| NN/g: ≤ 3 sizes, ≤ 2 big elements, ≤ 3 contrast variations | ON RECORD | `nngroup.com/articles/visual-hierarchy-ux-definition/`, quoted at `…visual-hierarchy-and-attention.md:110-118` |
| M3: products need far fewer than 15 type styles; avoid small differences | ON RECORD | `m3.material.io/styles/typography/type-scale-tokens`, quoted at `…visual-hierarchy-and-attention.md:126-130` |
| The tier ceiling of ≤ 6 sizes / ≤ 4 content tiers is this repo's extension, not NN/g's | ON RECORD (labelled) | `…visual-hierarchy-and-attention.md:146-152` |
| 4 dp / 8 dp grids; grouping ratio is a convention with Apple's 8/32 as the only measured point | ON RECORD | `developer.android.com/design/ui/mobile/…/grids-and-units`, `m3.material.io/styles/spacing/tokens`, `developer.apple.com/design/human-interface-guidelines/layout`, all quoted at `…visual-hierarchy-and-attention.md:492-523` |
| Our own accordion of hierarchy rules `H1`–`H25` exists with numeric thresholds and sources | MEASURED | `docs/research/2026-10-02-visual-hierarchy-and-attention.md:753-957` |
| Apple defines the shimmer as a process indicator that fades as the result arrives | ON RECORD (secondary) | `docs/redesign/competitor-teardown.md` §2.5.4 → `developer.apple.com/design/human-interface-guidelines/generative-ai`, read 2026-10-16 by that worker |
| Photoroom/Pixelcut/Pebblely/Canva patterns (named tools, templates, credit-in-picker, one place for account) | ON RECORD (secondary) | `docs/redesign/competitor-teardown.md` §1.1–1.7, §2.1–2.7, with its own source list |
| Play requires flagging for offensive AI output and references C2PA | ON RECORD | `docs/research/M0-S6-play-policy.md:26,32` → `support.google.com/…/answer/14094294`, `c2pa.org/conformance/`, read 2026-10-14; implementation UNVERIFIED |
| The competitor teardown's `docs/design/midjourney-DESIGN.md` citation is broken | MEASURED | `Test-Path docs/design` → `False`; `Get-ChildItem -Recurse -Filter '*midjourney*'` → no results |
| Anything about how this looks or feels on a phone | UNVERIFIED | needs the device run of `.cursor/rules/device-testing.mdc`; `python scripts/visual-qa.py --slot plinth-product-photos` is currently blocked on a missing `store/journey.json` (recorded in `docs/ui-critique-2026-10-16.md`) |
| Whether `NavigationSuiteScaffold` and the `material3.adaptive.navigation3` imports are stable on the pin | UNVERIFIED | `./gradlew :app:dependencies` on this slot, run once and not concurrently with another worker |

---

## 8. Sources

**In this repository, read this session**

- `apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/ui/`
  — `PlayKilnApp.kt`, `theme/Color.kt`, `theme/Theme.kt`, `theme/Type.kt`,
  `components/{PlinthStage,PlinthScaffold,ProofRing}.kt`, `home/HomeScreen.kt`,
  `autocard/AutoCardScreen.kt`, `studio/{StudioRoute,StudioToolsScreen}.kt`, `wallet/WalletScreen.kt`,
  `import/{ImportScreen,SelectedMediaPreview}.kt`, `library/LibraryScreen.kt`, `editor/EditorScreen.kt`,
  `cutout/CutoutScreen.kt`, `slide/SlideKitScreen.kt`, `card/CardScreen.kt`, `export/ExportScreen.kt`,
  `settings/StudioTryout.kt`, `listing/ListingTextScreen.kt`
- `…/app/src/main/res/` — `drawable/*` (21 vectors), `values/strings_import_smartcard.xml:19`,
  `values-ru/strings_import_smartcard.xml:19`
- `…/app/src/main/assets/autocard/scene-recipes.json` (63 165 bytes, 124 `sceneId`)
- `…/store/studio.json` (mark `mark-plinth`; palette semantics: verdigris = guarantees/rules, clay = charged credits)
- `…/docs/owner-review-2026-10-16.md` (the owner's verbatim words and the five extracted defect classes)
- `…/docs/ui-critique-2026-10-16.md` (source-only audit; its §5 and findings 1–2 are adopted here)
- `…/docs/plinth-product-photos.differentiators.json` (the promise contract)
- `…/docs/redesign/competitor-teardown.md` (the shell decision and the competitor evidence — another
  worker's deliverable, cited, not restated)
- `.cursor/rules/{ui-ux-baseline,visual-identity,app-depth,grounded-claims,device-testing}.mdc`
- `docs/research/2026-10-02-visual-hierarchy-and-attention.md` (§2, §6, §7, §8, §11 — `H1`–`H25`)
- `docs/research/2026-10-02-color-systems-and-palette-derivation.md` (§4.1, §5.2–5.5, §6, §7)
- `docs/research/M0-S6-play-policy.md` (AI-content policy and C2PA)
- `.kiln/scratch/plinth-contrast.py` (written and run in this session; every ratio in §5.1)

**Named primary pages, reached through the sources above**

- <https://m3.material.io/blog/tone-based-surface-color-m3> · <https://m3.material.io/styles/color/roles>
  · <https://m3.material.io/styles/spacing/tokens> · <https://m3.material.io/styles/typography/type-scale-tokens>
  · <https://m3.material.io/components/extended-fab/guidelines>
- <https://developer.android.com/design/ui/mobile/guides/layout-and-content/grids-and-units>
  · <https://developer.android.com/develop/ui/compose/layouts/adaptive> (the adaptive switch)
- <https://www.nngroup.com/articles/visual-hierarchy-ux-definition/>
  · <https://www.nngroup.com/articles/principles-visual-design/>
  · <https://www.nngroup.com/articles/fitts-law/> · <https://www.nngroup.com/articles/touch-target-size/>
  · <https://www.nngroup.com/articles/closeness-of-actions-and-objects-gui/>
- <https://developer.apple.com/design/human-interface-guidelines/generative-ai>
  · <https://developer.apple.com/design/human-interface-guidelines/layout>
  · <https://developer.apple.com/design/human-interface-guidelines/components/menus-and-actions/buttons>
- <https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html> · <https://www.w3.org/WAI/WCAG22/Understanding/use-of-color.html>
- <https://fluent2.microsoft.design/layout> · <https://helpx.adobe.com/lightroom/mobile/…/import-photos-from-your-device.html>
- <https://support.google.com/googleplay/android-developer/answer/14094294> · <https://c2pa.org/conformance/>
- Competitor help centres as recorded in the teardown: `help.photoroom.com`, `help.pixelcut.app`,
  `canva.com/help` (*Understanding the Canva mobile app tabs*), Pebblely help centre.

---

## 9. Applied to Plinth: before → after

Every destination declared in `ui/PlayKilnApp.kt`, from the file's own list
(`:113-170`, entries `:188-500`). **"Carries the change" names the file**, because a specification that
does not name a file is not adoptable. Where the row says *new*, the file does not exist yet — the
reason is in §0.6.

| Destination (`PlayKilnApp.kt`) | What it is today | What it becomes | Carries the change |
|---|---|---|---|
| `HomeDestination` `:113`, entry `:188` | "Studio" home: a `PlinthStage` hero, a 3-step instruction, an `Adaptive(260.dp)` toolbox grid of vector-glyph cards, a `+`-glyph wallet button (`HomeScreen.kt:139-148,341-474`); 21 `Text(` / 10 image refs; on an empty library it contains **no image-bearing surface at all** | **Tab 1 — the Studio.** Rank-1 element is the last thing the user made (or the picker, when there is nothing); the hero becomes a real `PlinthStage` with a palette-derived field (§5.1, §2.2); the toolbox becomes a **gallery** rather than a grid of glyphs; the balance becomes the existing `BalanceChip`; the create action is the shell's raised action, not a `+` on the wallet | `ui/home/HomeScreen.kt`, `ui/theme/Theme.kt`, `ui/components/{PlinthStage,PlinthToolCard}.kt` (**both exist** — retone `PlinthStage`, wire `PlinthToolCard` for the gallery) |
| `ImportDestination` `:122`, entry `:219` | a `TopAppBar` + a `bottomBar` whose primary button is `plinth_make_ai_card` on a `Surface(shadowElevation = 6.dp)` with no colour (§6 A5); leads to the **non-AI** `CardDestination` (`:203-215`) | **Bench station 1 — Bring in.** The picker lands here; the photo is the rank-1 element immediately; one `PrimaryBar` whose label is the next station's name, on the bench tone with a hairline | `ui/import/ImportScreen.kt`, `ui/import/SelectedMediaPreview.kt`, `ui/components/PrimaryBar.kt` (new) |
| `CardDestination(photoId)` `:125`, entry `:226` | the **non-AI** local composition, entered from the main path; carries `ProofRing` (its only use) and warns that no cut-out is performed | **The "exact placement" station**, reached from the bench as a *choice*, never as the default door; keeps `ProofRing` and becomes that component's reference implementation | `ui/card/CardScreen.kt`, `ui/components/ProofRing.kt` |
| `AutoCardDestination(photoId)` `:134`, entry `:241` | the AI flow: a 1.2-aspect preview `Box` with no `PlinthStage`, a `FilterChip` before/after pair, a `LazyRow` of **gradient swatches** labelled "Solid 1", a category `Surface` opening a `FilterChip` `ModalBottomSheet`, a `bottomBar` whose primary action is hidden until `preview != null`; regenerate **pops and re-pushes** the destination (`:280-285`) | **Bench station 2–3 — Set the scene / Making.** `SceneCard`s rendered from the recipes' own `color`/`angle`/`shadow`/`fillTarget`; the category picker becomes a visual chooser (the sheet survives as overflow); the primary action is persistent and never scrolls away; failure is an in-screen state and **never** a navigation event | `ui/autocard/AutoCardScreen.kt`, `ui/autocard/FormingCardPreview.kt` (**exists** — retone its two shimmer stops), `ui/components/{SceneCard,CompareSlider,PrimaryBar}.kt` (new), `ui/PlayKilnApp.kt` (`onRegenerate`, `:280-285`) |
| `AutoCardResultDestination` `:137`, entry `:275` | **a private composable inside the navigation module** (`AutoCardResultScreen`, `PlayKilnApp.kt:609`), a bare `CircularProgressIndicator` at `:682`, and a string telling the user the result *"has not been saved as a project output"* | **Bench stations 4–5 — Keep the pixels / Ship it**, in a file of its own, ending in the `data_out` handoff the contract already promises; the `CompareSlider` is the screen's rank-1 control | `ui/result/AutoCardResultScreen.kt` (new), `ui/PlayKilnApp.kt` |
| `EditorDestination` `:128`, entry `:233` | five sliders + background chips + a `FilterChip` before/after pair (`EditorScreen.kt:110,123,146-157,301`); 13 `Text(` / 3 image refs | **Station 4's detail view.** Sliders demoted behind the visual scene/preset gallery; the comparison promoted to `CompareSlider`; the 22 dp and 5 dp literals folded into `MaterialTheme.shapes` (§5.3) | `ui/editor/EditorScreen.kt` |
| `CutoutDestination` `:131`, entry `:236` | wrapped in `WithShellBack`; `TopAppBar` with no `navigationIcon` (`CutoutScreen.kt:164`); **the most image-led screen in the app** (21 `Text(` / 8 image refs) and the two biggest radii literals (22 dp) | **a station** (a cut-out is a step, not a destination), with a real app bar; its imagery-first proportion becomes the **pattern every other station copies** | `ui/cutout/CutoutScreen.kt`, `ui/components/PlinthStage.kt` |
| `ExportDestination` `:140`, entry `:287` | a card per pack printing `entry.path` (`ExportScreen.kt:209`) then one summary line carrying width, height, format, quality, a processing label **and `bytes.size`** (`:210-221`); 13 `Text(` / **1** image ref; "кривой" in the owner's words | **Station 5 — Ship it.** Each pack is a **rendered thumbnail** of the actual output with its marketplace named; the byte size and the processing label move behind a disclosure; the share/save path is the screen's primary action | `ui/export/ExportScreen.kt`, `ui/components/PackCard.kt` (new) |
| `LibraryDestination` `:143`, entry `:310` | `WithShellBack` + `PlinthScaffold` with **no topBar**, then a screen with **five `OutlinedTextField`s used as filters** (search, category, marketplace, "updated after", "updated before") and 2 image refs | **a real list pane** in a Library/Project list-detail (§3.4); filters become chips and a search field, not a form of five text boxes; the grid is imagery-first with ≥ 2 cards visible | `ui/library/LibraryScreen.kt`, `ui/PlayKilnApp.kt` |
| `ProjectDestination(projectId)` `:146`, entry `:337` | `ProjectWorkspaceScreen` — **a private composable in the navigation module** (`PlayKilnApp.kt:787`) | **the detail pane**, in its own file, sharing the 16 dp gutter and the shape scale | `ui/project/ProjectWorkspaceScreen.kt` (new), `ui/PlayKilnApp.kt` |
| `GroupDestination(projectId)` `:149`, entry `:347` | `GroupScreen` behind `WithShellBack`; 25 `Text(` / **0** image refs — SKU grouping, a fully photographic job, renders no photo | **a working surface**: each group is a plate of its own photos; the 2-pane policy stated | `ui/group/GroupScreen.kt`, `ui/components/PlinthStage.kt` |
| `LooksDestination(projectId)` `:152`, entry `:367` | `LooksScreen` behind `WithLoadedDestination`; the saved-look picker that gates the batch | **a scene gallery like station 2** — the same `SceneCard`, so a look is chosen once and recognised everywhere | `ui/batch/LooksScreen.kt`, `ui/components/SceneCard.kt` (new) |
| `BatchDestination(projectId, lookId)` `:155`, entry `:392` | `BatchDestinationRoute` — **a private composable in the navigation module** (`PlayKilnApp.kt:826`); `BatchScreens.kt` is 43 `Text(` / **0** image refs with a `LinearProgressIndicator` (`:159`) | **a filmstrip**: the run is a row of `ResultSkeleton`s that resolve one by one, so the batch is *visible* as it progresses; the `POST_NOTIFICATIONS` reminder path is `batch-done-reminder`'s, not a visual decision | `ui/batch/BatchScreen.kt` (new, split out of `BatchScreens.kt`), `ui/PlayKilnApp.kt` |
| `SlideKitDestination(photoId)` `:158`, entry `:399` | `SlideKitScreen` behind `WithShellBack`; `TopAppBar` with no nav icon (`:148`); a bare spinner at `:308`; a `Toast` on success (`PlayKilnApp.kt:436-442`) | **a station with a board**: the slide series is the rank-1 element, one slide per visible card, the success state in place instead of a `Toast` | `ui/slide/SlideKitScreen.kt`, `ui/autocard/FormingCardPreview.kt` (**exists** — generalise, do not duplicate) |
| `ListingTextDestination` `:161`, entry `:449` | `ListingTextScreen`; 31 `Text(` / **0** image refs — and this one *should* be textual, which is why it is the control case: the rule in §5.6 excludes it explicitly | stays textual by right; gains **one** image-bearing surface (the item it describes) so the text is anchored to what it is about | `ui/listing/ListingTextScreen.kt` |
| `StudioToolsDestination` `:164`, entry `:481` | `StudioRoute` behind `WithShellBack`; 46 `Text(` / 3 image refs; a `LinearProgressIndicator` at `:210`; `StudioToolsScreen` rows that are prose about provenance, cost and availability rather than an example of the operation (`:314-345`), and a detail pane of four `Text` lines (`:204-208`) | **the bench's tool drawer**, one balance, one `CreditPill` per row showing cost *before* the choice, human names instead of wire enums, and a visible result area | `ui/studio/StudioToolsScreen.kt`, `ui/studio/StudioRoute.kt`, `ui/components/PlinthCostChip.kt` (**exists** — wire it; do not add a second cost chip) |
| `WalletDestination` `:167`, entry `:488` | `WalletRoute`; 38 `Text(` / **0** image refs; four product cards each with title, arithmetic, price and a `Button` (`WalletScreen.kt:259-286`); a commit bar on `surfaceContainer` (**unset → framework grey**) | **one balance, then top-ups**: the balance is the rank-1 element, the products become a labelled list under it, the commit bar uses the bench tone and the clay accent the model reserves for credits | `ui/wallet/WalletScreen.kt`, `ui/wallet/WalletRoute.kt`, `ui/components/PlinthTopBar.kt` (`BalanceChip` — **exists**, wire it) |
| `SettingsDestination` `:119`, entry `:213` | `SettingsRoute` → `SettingsScreen` + `StudioTryout`; the owner's report is that the settings *"смешались"* | structure is `docs/settings-ia.md`'s; this document only fixes the **chrome**: one scroll axis, spacing-made groups instead of dividers (`H10`), `MaterialTheme.shapes`, and the declared type scale | `ui/settings/SettingsScreen.kt`, `ui/settings/StudioTryout.kt` |
| `AboutDestination` `:116`, entry `:210` | a top-level destination reached from Home | **a row inside Settings** (§3.3), not a destination of its own | `ui/settings/SettingsScreen.kt`, `ui/about/AboutScreen.kt`, `ui/PlayKilnApp.kt` |
| `CoachDestination(projectId)` `:170`, entry `:491` | `CoachRoute` behind `WithLoadedDestination`; 40 `Text(` / 2 image refs — a shooting coach, and it shows almost no photography | **the `before` stage of the job**: the coach's advice is illustrated with the shot it is describing, and it hands off into station 1 | `ui/coach/CoachScreen.kt` |
| — (the shell itself) | **the shell components exist and nothing calls them** (§0.7): `PlinthBottomNavBar`/`PlinthNavigationRail` (`:31`/`:85`), `PlinthTopBar` + `BalanceChip` (`:26`/`:53`), `PlinthCostChip`, `PlinthToolCard` — all with **zero call sites**; `NavDisplay` at `:182`; 19 of 20 destinations are `detailPane()`; a wide window shows the sentence `home_pick_section` (`:886-892`) | **wire what is already built, then host it**: a `NavigationSuiteScaffold` in `PlayKilnApp.kt` that switches `PlinthBottomNavBar` ↔ `PlinthNavigationRail` on the window size class, `PlinthTopBar`'s `BalanceChip` in the chrome on every section, Library+Project as the list-detail pair, predictive back opted in — and the §5.1 retone applied to those components **before** they are adopted, not after (§0.7 point 3) | `ui/PlayKilnApp.kt` (wire), `ui/components/PlinthNavBar.kt` + `PlinthTopBar.kt` (retone) |

**The order I would build it in, and why**

1. **`ui/theme/Theme.kt` + `Type.kt` + the shape scale (§5.1, §5.3, §5.4) — one change, first.**
   The eighteen framework-role references §0.1 measures and three undeclared type styles are the
   mechanism behind §0.1 and §0.5; they
   are the cheapest edits in the document and they change every screen at once. **§5.1 must land whole,
   or the secondary-text contrast regresses** (4.966:1 → 4.465:1).
2. **`SceneCard` from `scene-recipes.json` (§5.6) — the highest visual return per hour.** The data, the
   geometry and the density convention (`widthIn(164,220)`) are already in the code; the app is 124
   rendered scene thumbnails away from looking like the category it is in, with no new asset and no new
   backend.
3. **The shell (§3) — and it is the cheapest of the three, because the components already exist.**
   §0.7's table is the whole job: `PlinthBottomNavBar`, `PlinthNavigationRail`, `PlinthTopBar` and
   `BalanceChip` have zero call sites, so this is a **`PlayKilnApp.kt` wiring change**, not a design or
   a build. It is the navigation defect the owner described — one photo, then a whole studio, then
   categories, then something else offered again — and it is the change that makes the other two
   visible on all twenty destinations at once. The bench's persistent primary action is the one part of
   it that is genuinely new.

Everything else in the table is a consequence of those three.
