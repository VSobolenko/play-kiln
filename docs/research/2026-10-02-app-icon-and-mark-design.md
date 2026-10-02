# App icon and mark design: what the professionals actually encode

**Date:** 2026-10-02
**Author:** research pass for the PlayKiln factory (Cursor)
**Status:** research document; no code, no rule file, no slot was changed.
**Written because:** the factory's icon work has an objective half and a missing half. The objective
half exists and is good — `scripts/lib/icon_design.py` owns a construction grid, a declared stroke
scale and a primitive set; `scripts/lib/icon_qa.py` measures the raster against thirteen gated
legs; `scripts/check-adaptive-icon.py` measures the shipped vector against the platform masks. The
missing half is the **design** half. The owner's complaint, recorded 2026-10-02, is that the factory
is weak at exactly this: *there are professionals who generate intelligent icons — you cannot just
write the letter M, you have to find the focal point, tune the gamut, decide where the eye lands
first, and what the palette should be.* His standard is explicit: **do not invent a method — study
the professionals' established rules and encode those.**

This document therefore does two things: it states the established rules with primary sources, and
it turns the ones that are actually computable into numbered, machine-checkable assertions in the
**Encodable rules** section at the end. It also cross-checks every platform number against what
`scripts/lib/icon_design.py`, `scripts/lib/icon_qa.py`, `scripts/core/icon.mjs` and
`scripts/check-adaptive-icon.py` currently implement, and reports the disagreements it found as
findings — §5 and §9.

**Labels.** Four, as the factory uses them: **MEASURED** (a number observed in this tree or in a
run, with the file it was read from), **ON RECORD** (stated by a named source; the URL is given),
**INFERRED** (this document's reasoning, which could be wrong), **UNVERIFIED** (claimed somewhere
but not established here). A rule that is really taste wearing a number is called out as a
**convention** and is never dressed as a measurement.

The one-line summary of the honest position: **the platform publishes the geometry, and it does not
publish the design.** Every hard number below — 108, 72, 66, 512, 30 %, 1024 KB — is a fact someone
measured. Nearly every *design* rule — one idea, colour-only-is-a-defect, silhouette-first — is a
professional convention that the factory can and should encode as a threshold, but the threshold is
the factory's decision, not Google's law. This document says which is which, per rule.

---

## 1. What an app icon is for, mechanically

### 1.1 The surfaces it appears on

| Surface | Size it is seen at | Source | Label |
|---|---|---|---|
| Launcher cell, legacy / Android ≤ 7.1 | **48 × 48 dp** (mdpi 48 px, hdpi 72 px, xhdpi 96 px, xxhdpi 144 px, xxxhdpi 192 px) | Android iconography: *"Launcher icons on a mobile device must be 48x48 dp"* ([googlesource](https://android.googlesource.com/platform/frameworks/base/+/f19e4b420de65306f09c199829e7e06fe210e21d/docs/html/design/style/iconography.jd)); density table in [Support different pixel densities](https://developer.android.com/training/multiscreen/screendensities) | ON RECORD |
| Launcher cell, adaptive (Android 8+) | 108 × 108 dp layers, of which the **inner 72 × 72 dp is the masked viewport** | [Adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive); [AdaptiveIconDrawable](https://developer.android.com/reference/android/graphics/drawable/AdaptiveIconDrawable) | ON RECORD |
| Themed icon (Android 13+) | same cell, tinted by the system from one monochromatic layer | [Features and APIs 13](https://developer.android.com/about/versions/13/features), [Adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive) | ON RECORD |
| Play store icon | **512 × 512 px**, shown in list rows, search cards and the app page, dynamically rounded and shadowed by Play | [Google Play icon design specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications) | ON RECORD |
| In-app / system UI icons | Material's system-icon grid is **24 dp** | [Material 3 — Designing icons](https://m3.material.io/styles/icons/designing-icons) | ON RECORD |

**The smallest size that the *mark* must read at is the launcher cell and the 512 px store icon
rendered small — both in the 40–48 px range on screen.** Two readings of "48 dp" circulate and they
differ by a factor of 2/3, so the factory must say which it means:

- **Store reading (conservative).** The 512 px asset is the whole 108 dp canvas; a 512→48 px
  downscale puts the entire canvas into 48 px. Under this reading a 24 px-at-512 stroke becomes
  **2.25 px**, and a 16 px stroke **1.5 px** — which is exactly the arithmetic in
  `scripts/lib/icon_qa.py` (`min_at_48 = stroke * 48 / 512`) and in `studio.json`'s prose. **MEASURED**
  (read from `icon_qa.py:774`, `min_feature_at_48dp_px`).
- **Launcher reading.** The adaptive drawable's intrinsic size is the **72 dp viewport**
  (`AdaptiveIconDrawable.getIntrinsicWidth() = getMaxIntrinsicWidth() * DEFAULT_VIEW_PORT_SCALE`,
  with `DEFAULT_VIEW_PORT_SCALE = 1/(1+2×0.25) = 2/3` and `EXTRA_INSET_PERCENTAGE = 1/4` — so the
  child layer is 108 dp when the bounds are 72 dp; [AOSP source](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/graphics/java/android/graphics/drawable/AdaptiveIconDrawable.java),
  [API reference](https://developer.android.com/reference/android/graphics/drawable/AdaptiveIconDrawable)).
  If a launcher draws the viewport into a 48 px cell, the same 24 px stroke becomes **3.375 px** —
  1.5× *larger*, i.e. more legible. **INFERRED** (the 48 px cell is the platform's legacy icon size;
  the scale factor is arithmetic on the two, and Google does not publish an "adaptive icon at 48 dp"
  sentence).

So the factory's existing 1.5 px floor is the **worst case**, not the only case, and the two
readings bracket it. This matters for a rule: a threshold stated at the store reading is safe for
both.

### 1.2 The detail budget

Three facts set the budget, and they all point the same way:

- The masked viewport is 91.7 % of the layer (`SAFEZONE_SCALE = 66f/72f`, AOSP source above), so
  **at most ~44 px of a 48 px launcher cell is visible** — an adaptive icon loses about 8 % of its
  cell to the mask even at the widest.
- A stroke below **1.5 px** in the cell "reads as a grey smear rather than a line" — the factory's
  own measurement (`icon_qa.py` `MIN_FEATURE` comment and `thinnest_stroke_px`), and it is the
  narrowest published floor found in this pass. **ON RECORD (factory)**, no platform analogue.
- Professional practice is blunter than the arithmetic: the *single most predictive pre-export
  check* is a **silhouette test at ~40 px in greyscale**, because at that size interior detail is
  already gone and only the outer contour is doing any work ([App Icon Design: a developer's
  platform-first guide](https://apptenium.com/guides/app-icon-design), practice/secondary).
  Apple's own words are the same instruction without the number: *"An icon with fine visual features
  might look busy… details may be hard to discern at smaller sizes"*
  ([Apple HIG — App icons](https://developer.apple.com/design/human-interface-guidelines/app-icons)).

**Inference the factory can use (INFERRED):** the detail budget is **one idea, expressed in
≤ 3 masses and 1 stroke weight family**, with every feature ≥ 1.5 px at the store reading of 48 px.
Anything that needs a second read is below the resolution at which the icon is chosen.

---

## 2. The focal point

### 2.1 The established basis for an "optical anchor"

`scripts/lib/icon_design.py` speaks of an `optical_anchor` (default `(0.5, 0.5)`,
`OPTICAL_TOLERANCE = 0.02`) and `icon_qa.silhouette_stats()` measures the silhouette centroid against
it. The established basis for that idea, from sources designers cite:

- **Geometric vs optical alignment.** *"Most of the time mathematical centering will do just fine…
  sometimes it will look off. That is when you have to place the element slightly off its true
  center."* The canonical example is a play triangle: *"the weight or mass in the triangle lies in
  its left-hand side… we have to manually nudge the triangle until it appears central"*
  ([Rails Designer](https://railsdesigner.com/mathematical-optical-alignment-design/),
  [Marvel](https://marvelapp.com/blog/optical-adjustment-logic-vs-designers/)). **ON RECORD**
  (practice).
- **A formula exists for the symmetric case.** For a triangle, *"aligning the triangle centroid to
  the center of the circle… looks right"*; and a circle next to a square of equal width must be
  scaled **112.84 %** to match area (the convex-hull area is the better weight proxy for shapes with
  holes) ([bjango — Formulas for optical adjustments](https://bjango.com/articles/opticaladjustments/)).
  **ON RECORD** (practice, with arithmetic).
- **Visual weight is not bounding-box size.** It is the relationship between filled area, stroke
  weight, internal spacing, contrast and surrounding whitespace, and *"alignment creates structure,
  but weight creates balance"* ([Dutchicon — Optical weight in icons](https://dutchicon.com/optical-weight-icons/)).
  **ON RECORD** (practice).

**What this means for `optical_anchor` (INFERRED):** the factory's rule — *the arithmetic centre is
the optical centre only for centrally symmetric silhouettes, and an asymmetric mark must declare its
own anchor* — is **correct and under-used**. It is stated in the module's comment
(`icon_design.py:114-122`) and is true: for a centred circle, square, ellipse or rectangle the area
centroid is the geometric centre; for a triangle it is ⅓ of the height up from the base, which is
the centroid bjango's article names. But `OPTICAL_ANCHOR` is **empty** in the current tree
(**MEASURED**, `icon_design.py:122`), and the two gates that measure reach (`check-adaptive-icon.py`,
`scripts/core/icon.mjs`) measure from the *geometric viewport centre* instead — see §5.3.

### 2.2 How many competing shapes before legibility collapses

No primary source publishes a number, and this document will not invent one.

- Apple: *"Find a concept or element that captures the essence of your app or game, make it the core
  idea of your icon, and express it in a simple, unique way with a **minimal number of shapes**"*
  ([Apple HIG](https://developer.apple.com/design/human-interface-guidelines/app-icons)). **ON RECORD.**
- Material: *"Simplify icons for greater clarity and legibility"*, *"Make icons graphic and bold"*,
  *"Don't be overly literal. Avoid complex icons"* ([M3 — Designing icons](https://m3.material.io/styles/icons/designing-icons),
  [M1 — Style: icons](https://m1.material.io/style/icons.html)). **ON RECORD.**
- Silhouette/blur tests show *why*: heavily blurred logos that share a common form *"all begin to
  look alike"*, and circular forms collapse together first
  ([Jim Nielsen — Logo integrity brought into focus](https://blog.jim-nielsen.com/2014/logo-integrity/)).
  **ON RECORD** (practice experiment).

**A computable proxy, labelled honestly (INFERRED → convention):** a mark should have **≤ 3 visually
separate masses**, where a "mass" is a connected ink component after closing gaps narrower than one
`regular` stroke, and the two largest masses may not be within 10 % of each other in area *and*
differently coloured *and* separated — i.e. the icon reads as one gestalt or one dominant + one
support. The threshold is taste, not law.

**A warning that matters for this factory, and it is a finding.** A naive "one dominant mass"
rule would **fail the factory's own `mark-stripes` mark**, which is four strokes of equal weight by
design (two chalk, two accent). It is *one idea* — a mown-stripe fan — expressed as four parallel
marks, and that is legitimate. Equally, a naive "no parallel strokes / no letterform" detector would
**false-positive on `mark-stripes`** (four parallel strokes converging) while doing nothing about a
real letter-as-logo, which is a different defect (§8.1). Any rule in this area must be a **report
or a declared-family rule**, never a hard gate on component count. **MEASURED** (the geometry is in
`studio.json`, `_mark`).

---

## 3. Silhouette and shape language

### 3.1 The outline must read before the interior

This is the most consistently repeated rule across every source, and it is the one the icon is
usually judged by:

- *"A production-ready app icon is a single high-contrast silhouette"*; run the **40 px silhouette
  test in greyscale** — *"This single test predicts most real-world failures before you touch an
  export dialog"* ([apptenium](https://apptenium.com/guides/app-icon-design), practice/secondary).
  **ON RECORD** (practice).
- Blur/squint tests exist to prove that *"the logo's primary silhouette and internal hierarchy are
  strong enough to survive degradation"*; the documented failure modes are **over-detail** (features
  compete), **collapsed interior gaps**, and **line weight too light** (Cursa — *Scalability and
  legibility checks*, [cursa.app](https://cursa.app/en/page/logo-design-foundations-scalability-and-legibility-checks),
  practice). **ON RECORD** (practice).
- Jim Nielsen's experiment removes colour and applies increasing Gaussian blur, precisely to test
  *"how well the mark can stand on its own"* as shape alone. **ON RECORD** (practice).

### 3.2 Corner and edge treatment

- Material's system grid: *"Corner radii are 2dp by default"*, *"For the outlined style symbols,
  interior corners are square, not rounded. For shapes 2dp wide or less, stroke corners shouldn't be
  rounded"*; recommended stroke weight is **2 dp** and stroke weights and terminals must be
  consistent across a set ([M3](https://m3.material.io/styles/icons/designing-icons),
  [M1](https://m1.material.io/style/icons.html)). **ON RECORD.**
- The factory already does this at icon scale: `BOOK_CORNER = STROKE["regular"]` and the `plate`
  primitive's corner treatment is a declared parameter (`icon_design.py:177-178, 223`). **MEASURED.**
- Play applies the outer corner treatment dynamically — *"Google Play dynamically applies corner
  radius… Radius will be equivalent to 30 % of icon size"*, and *"Don't round the edges of your
  final asset"* ([Play icon design specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications)).
  **ON RECORD.** The practical consequence: **an icon's own corners must not carry meaning**, because
  Play will cut them and every OEM mask will cut more.

### 3.3 Recognised geometric families and what they connote

There is no published connotation table that is not marketing. What *is* published is the **keyline
family** and the instruction to build on it:

- Material's product-icon grid uses **square (152 dp), circle (176 dp diameter), vertical rectangle
  (128 × 176), horizontal rectangle (176 × 128)** keylines, so that differently-shaped icons
  *"maintain a consistent visual proportion"* ([M1](https://m1.material.io/style/icons.html)).
  **ON RECORD.**
- The factory's own second keyline is the **3:4 portrait rectangle inscribed in the circle
  keyline** (`icon_design.py:91,95`, `GRID["portrait"] = (0.344444, 0.459259)`), generalising Material's
  two rectangles into one rule. This is a **good, defensible invention** and agrees with Material's
  method. **MEASURED** (factory) / **ON RECORD** (Material).
- *"A unique outline focuses attention on the object and makes it easy to recognize"* (Apple's macOS
  HIG as archived: [web.archive.org](https://web.archive.org/web/20181206014649/https:/developer.apple.com/design/human-interface-guidelines/macos/icons-and-images/app-icon/)).
  **ON RECORD.**
- Claims of the form "circles mean friendliness, squares mean stability" are **UNVERIFIED** in this
  pass and should not be encoded. The two conventions worth keeping are weaker and testable:
  **a circular silhouette is the *least* distinctive form** under blur (Nielsen, above), and
  **a filled form reads before an outlined one** (Cursa's "stroke-to-fill conversion… to avoid
  hairline loss"). Both are **ON RECORD** (practice) and both are already implicit in the factory's
  `MIN_FEATURE` floor.

### 3.4 How to test a silhouette (the test procedures professionals use)

| Test | Procedure | What it catches | Source | Label |
|---|---|---|---|---|
| Squint | Unfocus the eyes / heavy blur until shape only remains | competing ideas, blurry gestalt | Nielsen, Cursa | ON RECORD (practice) |
| Blur ladder | Incremental Gaussian blur, colour removed | which marks survive as shape; circles collapse together | [Nielsen](https://blog.jim-nielsen.com/2014/logo-integrity/) | ON RECORD (practice) |
| 40 px greyscale | scale to 40 px, desaturate, look | the single most predictive failure test | [apptenium](https://apptenium.com/guides/app-icon-design) | ON RECORD (practice) |
| Silhouette-only | keep outer contour, drop all interior detail | whether the outline alone is identifiable | Cursa | ON RECORD (practice) |
| Counter keep-one | keep only the most distinctive interior counter | over-detailed interiors | Cursa | ON RECORD (practice) |
| Context row | place beside 20 real icons of the category | "does it read as distinct" | Apple WWDC17 *App Icon Design* ([developer.apple.com/videos](https://developer.apple.com/videos/play/wwdc2017/822/)) | ON RECORD |
| Greyscale / CVD | 0 % saturation, then a simulator (Coblis, Color Oracle) | colour-dependence | [Coblis](https://www.color-blindness.com/coblis-color-blindness-simulator/), [Accessibility Labs](https://accessibility-labs.com/colorblindness/) | ON RECORD (practice) |

**INFERRED, and the useful part for a factory:** of these, exactly **two are cheaply computable**
and should be encoded as report legs, not as taste: the **40 px greyscale silhouette render** (a
raster the pipeline already produces in `icon_qa.downscale_contrast`) and the **context row** (a
composite the store renderer already has every asset for). Blur and squint are the same measurement
with a different kernel; **squint ≈ Gaussian blur with σ ≈ 1–2 px at 48 px**, which is the factory's
`field_high_frequency_rms` sigma of 2.0 (`icon_qa.py:516`) applied to the mark instead of the field.

---

## 4. Colour in an icon

### 4.1 Contrast against the *host* background

- The **launcher** paints the icon's own background layer, so the mark's contrast problem is against
  **its own field**, not the wallpaper — but the legacy docs are explicit that the icon must survive
  an arbitrary wallpaper: *"Since the user can change the Home screen's wallpaper, make sure that
  your launcher icon is clearly visible on any type of background"*
  ([Android iconography](https://android.googlesource.com/platform/frameworks/base/+/f19e4b420de65306f09c199829e7e06fe210e21d/docs/html/design/style/iconography.jd)).
  The adaptive icon solves this by owning its background layer; Play does **not** — *"Transparent
  assets will display the background color of Google Play UI"*, and Play's recommendation is to
  *"pick a background color for your asset that's appropriate for your brand and doesn't include any
  transparency"* ([Play icon design specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications)).
  **ON RECORD.**
- The measurable floor is **3:1** for a graphical object: WCAG SC 1.4.11 Non-text Contrast (Level AA)
  and technique **G207** — *"if the icons are required to understand the content, then the icons need
  to have a contrast ratio of at least 3:1"*, and for a gradient *"identify the color with the least
  contrast to the foreground color"*
  ([WCAG21 Understanding 1.4.11](https://www.w3.org/WAI/WCAG21/Understanding/non-text-contrast.html),
  [G207](https://www.w3.org/WAI/WCAG21/Techniques/general/G207.html)). **ON RECORD.**
- IBM's Carbon design system states the same for its icon set — *"Icons are always a solid,
  monochromatic color and need to pass the same color contrast ratio as typography (**4.5:1**)"*
  ([Carbon — Icons/Usage](https://carbondesignsystem.com/elements/icons/usage/)). **ON RECORD.**

**Reconciliation with the factory:** `icon_qa.ICON_GATES["downscale_contrast_48px"]` requires
**≥ 4.5:1** at a 48 px render — **stricter than WCAG's 3:1** and equal to Carbon's typography rule.
That is a **house convention, not a platform requirement**, and it should be labelled as such in the
rule it eventually becomes. **MEASURED** (read from `icon_qa.py:175-186`).

### 4.2 One dominant colour plus one accent

- Apple: *"Prefer a simple background, such as a solid color or gradient, that puts the emphasis on
  your primary design — you don't need to fill the entire icon canvas with content"*
  ([Apple HIG](https://developer.apple.com/design/human-interface-guidelines/app-icons)). **ON RECORD.**
- Play: *"use similar or complementary elements, styles, and color themes across your store listing
  graphics"* ([Best practices for your store listing](https://support.google.com/googleplay/android-developer/answer/13393723)).
  **ON RECORD.**
- The factory's own reference slot uses exactly this shape: a deep turf-green field, chalk ink, and
  **one** lime reserved for "work that has just been done" (`studio.json` `_palette`, **MEASURED**).
  That is the pattern, and it is the pattern the design sources describe.

### 4.3 What goes wrong with gradients or many hues at small size

- A **texture** in the field is invisible-but-dirty at launcher size: the factory measured the
  rejected icon's 32 px drafting grid at **1.40 RMS** of high-frequency luma against a clean ramp's
  **0.10**, and a bloom-laden field at **2.49 RMS** residual against a best-fit plane versus
  **0.22** for one ramp (`icon_qa.py` `ICON_GATES`, **MEASURED**). Gates: `field_high_frequency_rms
  ≤ 0.5`, `field_linear_ramp_rms ≤ 2.0`.
- Gradients **are** allowed and even recommended — Apple names "solid color or gradient" as the
  preferred simple background, and Play's own icon system renders a dynamically applied shadow, so
  lighting inside the artwork is expected. The defect is not a gradient; it is **more than one tonal
  structure** in the same field. **ON RECORD / MEASURED** (above).
- **Hue count at 48 px is the real failure.** With ~44 px of a 48 px cell visible, two hues that
  differ only in hue and not in lightness stop being two things. WCAG's own wording legalises the
  distinction: hue differences *plus* a lightness difference *"count as an additional visual
  distinction… as long as the difference in relative luminance between the colors leads to a
  contrast ratio of 3:1 or greater"*
  ([WCAG 1.4.1 Use of Color](https://www.w3.org/WAI/WCAG21/Understanding/use-of-color.html)).
  **ON RECORD.**

### 4.4 The greyscale / colour-blind check

- **SC 1.4.1 Use of Color (Level A)**: *"Color is not used as the only visual means of conveying
  information, indicating an action, prompting a response, or distinguishing a visual element"*
  ([W3C](https://www.w3.org/WAI/WCAG21/Understanding/use-of-color.html)). **ON RECORD.**
- Practice sources converge on the same procedure: design in **grayscale first**, then check the
  result in a simulator (Coblis, Color Oracle, Stark) across protanopia/deuteranopia/tritanopia
  ([Accessibility Labs](https://accessibility-labs.com/colorblindness/), [EULE](https://euleinstitute.com/en/blog/color-blindness-ux-design/)).
  **ON RECORD** (practice).
- For an app icon the criterion bites twice: **on the launcher**, where the mark must be identifiable
  as a shape in one colour (see §6, themed icons), and **in the store**, where a screenshot-sized
  icon may be seen by a user who cannot separate the accent from the field.

---

## 5. Adaptive icons and masks, measured

### 5.1 The platform numbers, from the platform

| Quantity | Value | Source | Label |
|---|---|---|---|
| Layer canvas | **108 × 108 dp** | [Adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive), [AdaptiveIconDrawable](https://developer.android.com/reference/android/graphics/drawable/AdaptiveIconDrawable) | ON RECORD |
| Masked viewport (visible square) | **72 × 72 dp** (inner) | same | ON RECORD |
| Reserved band, each side | **18 dp** (108 − 2×18 = 72) | same | ON RECORD |
| Guaranteed-visible safe zone | **66 × 66 dp** (*"the inner 66x66 dp of the icon appears within the masked viewport"*; never clipped by an OEM mask) | [Adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive) | ON RECORD |
| Logo size inside the layer | *"at least 48x48 dp"* and *"must not exceed 66x66 dp"* | same | ON RECORD |
| Mask geometry | *"This path should be convex and should respect the safezone (66/71 = 91 %) within the view bounds. This is enforced in one of the CTS tests."* | [AOSP — Implement adaptive icons](https://source.android.com/docs/core/display/adaptive-icons) | ON RECORD |
| Mask coordinate system | [0, 100] with 25 % padding all round | same | ON RECORD |
| Safe-zone scale constant | `SAFEZONE_SCALE = 66f/72f` → **0.916 67** | [AOSP AdaptiveIconDrawable.java](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/graphics/java/android/graphics/drawable/AdaptiveIconDrawable.java) | ON RECORD |
| Legacy launcher icon | **48 × 48 dp** | [Android iconography](https://android.googlesource.com/platform/frameworks/base/+/f19e4b420de65306f09c199829e7e06fe210e21d/docs/html/design/style/iconography.jd) | ON RECORD |
| No masks or shadows in the layer | *"The layers must not have masks or background shadows around the outline of the icon."* | [Adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive) | ON RECORD |
| Mask shapes an OEM may choose | circle, **squircle**, rounded square, square | [Create app icons](https://developer.android.com/studio/write/create-app-icons) | ON RECORD |

**A discrepancy inside Google's own documentation, reported rather than smoothed over:**
`source.android.com` writes the safe zone as **"66/71 = 91 %"**, while `AdaptiveIconDrawable.java`
defines `SAFEZONE_SCALE = 66f/72f` (91.67 %) and `developer.android.com` says 66 dp inside the 72 dp
viewport. 66/71 is 92.96 %, so the prose "91 %" is inconsistent with its own fraction either way.
The AOSP constant is the only one a device actually applies, and the factory uses **66/72 = 33 dp
radius**, which is the correct one. **ON RECORD** (both sources) / **INFERRED** (which to trust) —
this is a *finding* about the platform docs, not a defect in the factory.

### 5.2 What happens outside the safe zone, under each mask

- **72 dp circle**: the worst realistic mask (it is what `config_useRoundIcon = true` selects). A mark
  whose outermost ink is at radius ≤ 33 dp is inside; ink between 33 and 36 dp is clipped *only
  where the circle's diagonal is*, and ink beyond 36 dp is clipped by every mask.
- **72 dp squircle / rounded square**: clips the corners, so a mark whose *bounding box* corners are
  empty (a ring, a disc, a diagonal stroke) survives further out than a solid square of the same box
  would. The factory's `primitive_reach()` measures the actual outline for exactly this reason
  (`icon_design.py:378-407`, comment: *"a ring's box corner is empty, and measuring it would shrink a
  round mark by 30 % to protect a corner that holds no ink"*) — **MEASURED**, and this is the right
  call.
- **Square 72 dp**: clips only beyond the 36 dp half-extent; never clips inside 33 dp.
- **Play's own 512 px rendering** adds a **30 % corner radius** and a drop shadow
  ([Play icon design specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications)),
  so the store image loses more of the corners than the launcher does. A mark built inside the 62 dp
  keyline circle (0.574 × 108 dp) is inside both. **MEASURED** (geometry) / **ON RECORD** (Play's rule).

### 5.3 Cross-check against what the factory implements — **and the disagreements**

Read from the shipped files (all **MEASURED**):

| Platform / design quantity | Factory value | Where | Agreement |
|---|---|---|---|
| 108 dp viewport | `ADAPTIVE_VIEWPORT_DP = 108.0` | `icon_design.py:59`, `icon_qa` derives from it, `check-adaptive-icon.py:35` | ✅ |
| 72 dp visible | `ADAPTIVE_VISIBLE_DP = 72.0` → 0.666667 | `icon_design.py:60,101`, `GRID["visible"]` | ✅ |
| 66 dp safe | `ADAPTIVE_SAFE_DP = 66.0` → 0.611111; radius 33 dp | `icon_design.py:61-62,102`, `check-adaptive-icon.py:37` | ✅ |
| 18 dp margin each side | `GRID["margin"] = dp(18)` = 0.166667 | `icon_design.py:100` | ✅ |
| 48 dp logo floor / 66 dp ceiling | no direct floor; `MIN_REACH_RATIO = 0.75 × 31 dp = 23.25 dp` reach ⇒ ≈ **46.5 dp across**, against the platform's *"at least 48x48 dp"* | `icon_design.py:127` | ⚠️ **mild disagreement**, below |
| Circular mask, no clipping | gate: reach > 33 dp ⇒ FAIL | `check-adaptive-icon.py:281-288`, `core/icon.mjs:299` | ✅ |
| 512 px store icon | `CANVAS = 512` | `icon_design.py:58` | ✅ |

**Finding 5.3.1 — the reach floor is ~1.5 dp below the platform's stated minimum logo size
(DISAGREEMENT).** `MIN_REACH_RATIO = 0.75` of the 31 dp keyline gives a minimum reach of **23.25 dp**,
i.e. a mark as small as 46.5 dp across — just under the *"at least 48x48 dp"* the adaptive-icon page
states. The floor is correctly chosen for *"a mark that looks lost in its cell"*, but it is not the
platform's number. Either raise the ratio to **48/62 = 0.774** (so the floor *is* the platform floor),
or record in the module that 0.75 is a deliberate house floor and why. **ON RECORD** (platform
wording) / **MEASURED** (factory constant) / **INFERRED** (the reconciliation).

**Finding 5.3.2 — reach is measured from two different origins (DISAGREEMENT, latent).**
`icon_design.mark_reach()` and `icon_qa.silhouette_stats()` measure from the **declared optical
anchor**; `check-adaptive-icon.py` (`centre = VIEWPORT_DP/2`) and its Node port
`scripts/core/icon.mjs` (`const half = geometry.adaptiveViewportDp / 2`) measure from the
**geometric viewport centre**. For the two registered marks — and for anything else centrally
symmetric — the two coincide, so this is invisible today. For the first asymmetric mark that
declares an `optical_anchor` (the case `icon_design.py:114-122` explicitly anticipates), the shipped
vector can measure *inside* 33 dp from the geometric centre while `icon_qa` reports a different dp
figure, and the two gates will disagree about the same file. **MEASURED** (both code paths) /
**INFERRED** (the consequence).

**Finding 5.3.3 — two ceilings for one quantity.** `icon_qa.ICON_GATES["mark_reach_dp"]["max"]` is
`31.0 + 0.211 dp = 31.211 dp` (the keyline plus one canvas pixel), while `check-adaptive-icon.py`
allows anything up to **33 dp**. So the *render-time* gate is ~1.8 dp stricter than the
*shipped-artifact* gate. Both are inside the platform safe zone, both are intentional, but a mark
that passes the shipped check is not necessarily a mark `icon_design.assert_mark_on_grid()` would
let render — and `icon_design.py`'s own `assert_mark_on_grid` (lines 437-507) would refuse it first.
**MEASURED.** The fix is a shared constant, not a new threshold (§9, "one source of truth").

**Finding 5.3.4 — the declared mark of the one live slot is not in the registry (DISAGREEMENT with
the slot).** `apps/2026-10-01-lawn-round-log/store/studio.json` declares
`"mark": "mark-stripes"`, but `MARK_REGISTRY` in `scripts/lib/icon_design.py:262-267` contains only
`pipe` and `mark-book`. `resolve_mark()` calls `icon_design.mark_primitives(mark)`, which raises
`SystemExit("BLOCKED: unknown studio mark…")` for anything not registered
(`icon_design.py:271-276`), so as this revision stands the studio would refuse to render the slot's
own identity — fail-closed, which is the right failure mode, but it means the mark is currently
**unrenderable**. The `_mark` prose describes the entry as *"mechanical"* to add (four `stroke`
primitives at `bold`, round caps), so this is very likely a concurrent-edit gap rather than a design
problem. **MEASURED** (both files read; `rg "mark-stripes"` matches only the slot, not `scripts/`).
Flagged, not fixed, per this pass's constraints.

**Resolved 2026-10-02.** `mark-stripes` is registered: `MARK_REGISTRY` in `scripts/lib/icon_design.py`
now holds three marks (`pipe`, `mark-book`, `mark-stripes`, from four `stroke` primitives at `bold`),
and `scripts/core/icons.mjs`'s `icons.mark` leg reads the registry rather than trusting the slot —
it reports `PASS … 3 registered: pipe, mark-book, mark-stripes` on the live slot, and it is the leg
that would have failed the unrenderable state this finding measured. The paragraph above is left as
the measurement it was; the studio no longer refuses this slot.

**Finding 5.3.5 — the store icon's opacity rule is stricter than Play's.** `icon_qa` gates
`alpha_min >= 255` (every pixel opaque, including the four corners). Play requires a **32-bit PNG
with alpha** but only *recommends* opacity (transparency falls back to the Play UI background). The
factory's rule is a **house convention** that makes the icon host-independent — defensible, but it is
not a Play rule and must not be written as one. **MEASURED** (factory) / **ON RECORD** (Play).

---

## 6. Monochrome / themed icons

- **Android 13+ (API 33) themed icons.** The app *"must provide both an adaptive icon and a
  monochromatic app icon"*; the system derives a tint from the user's wallpaper/theme and applies it
  to the monochromatic layer ([Android 13 features](https://developer.android.com/about/versions/13/features)).
  The layer is declared as `<monochrome android:drawable="…"/>` inside the adaptive icon XML, and
  *"you can create separate drawables for each layer if needed"*
  ([Adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)).
  **ON RECORD.**
- **The home screen falls back** to the colour adaptive icon when the user has not enabled themed
  icons, when the app ships no monochrome layer, or when the launcher does not support them — unless
  the device is **Android 16 QPR 2 or later, where Android automatically themes icons for apps that
  don't provide their own** ([Adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)).
  **ON RECORD.** That last clause is the reason a colour-only mark is no longer merely un-themed: on
  current platforms it will be auto-tinted whether or not the app asked.
- **The Apps Experience Program requires it** for qualifying apps: *"enable your launcher icon to
  dynamically adjust its tint by including a drawable layer within the adaptive icon XML"*
  ([AEP — Themed App Icons](https://developer.android.com/distribute/aep/aep-req-theme-app-icons)).
  **ON RECORD.**
- The two-layer split is fine because the **background is supplied by the system** in themed mode:
  *"only a monochromatic foreground icon asset needs to be provided, the system will take care of the
  background and color both with the extracted color scheme"*
  ([Codelab — Design and preview your app icons](https://codelabs.developers.google.com/design-android-launcher)).
  **ON RECORD.**

**Why a mark that only works in colour is a defect (INFERRED, from the above):** under a themed icon
the **entire mark is one flat colour**. Every distinction the mark makes must therefore survive as
**shape**: the four stripes of `mark-stripes` survive as four stripes (good), while the two-inner-
lime/two-outer-chalk contrast does **not** (that is not a defect either — hue was decorative there,
not load-bearing). A mark where the *only* difference between two elements is hue loses an element
entirely under theming. The test is mechanical: **flatten the mark to a single ink colour over the
themed background and re-run the reach / stroke / counter / coverage gates.** If the mark passes in
one colour, themeable; if it stops reading, or if any two masses become one, hue was carrying
structure.

---

## 7. Play's own rules, as rules

| Rule | Value / wording | Source | Label |
|---|---|---|---|
| Store icon size | **512 × 512 px** | [Play icon design specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications), [Add preview assets](https://support.google.com/googleplay/android-developer/answer/9866151) | ON RECORD |
| Format | **32-bit PNG (with alpha)**, **sRGB** | same | ON RECORD |
| Size cap | **1024 KB (1 MB)** | same | ON RECORD |
| Shape | **Full square** — *"Google Play dynamically handles masking. Radius will be equivalent to 30 % of icon size"*; *"Don't round the edges of your final asset"* | [Play icon design specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications) | ON RECORD |
| Shadow | **None baked in** — *"Google Play dynamically handles shadows"*; *"Don't add drop shadows to your final asset"* | same | ON RECORD |
| Why the mask is forbidden | *"Uniformed shapes are visually more appealing and easier to digest. They help users focus on the artwork, as opposed to the shape. They fix alignment issues caused by random open space to better present surrounding information, such as the title, rating, and price."* | same | ON RECORD |
| Transparency | *"Transparent assets will display the background color of Google Play UI"* — play a background colour when possible | same | ON RECORD |
| Placement | Use the **keyline grid** when shapes matter; fill the asset when they do not; *"Don't force drastic brand & artwork transformation to full bleed"* | same | ON RECORD |
| Composition | *"keep important elements toward the center of graphics"* | [Best practices for your store listing](https://support.google.com/googleplay/android-developer/answer/13393723) | ON RECORD |
| No misleading graphics | *"Misleading symbols in app icons are not allowed, for example: new message dot indicator when there are no new messages and download/install symbols when the app is not related to downloading content"*; no ranking/price/promotional text or imagery; no emoji, emoticons or repeated special characters; ALL CAPS only as part of the brand | [Metadata policy](https://support.google.com/googleplay/android-developer/answer/9898842) | ON RECORD |
| No impersonation | *"don't use icons that could mislead users… such as icons that are similar to those of existing products or services"* | [Best practices](https://support.google.com/googleplay/android-developer/answer/13393723) | ON RECORD |

**Why Google forbids the baked mask (ON RECORD, and it is stated by Google).** Play's own rationale
is three-part: uniform shapes are easier to digest, they let artwork be the focus rather than the
shape, and they fix alignment against *"random open space"* in a list of icons — so that the title,
rating and price beside an icon line up the same way for every app. A pre-rounded asset reintroduces
exactly the inconsistency Play removes. **MEASURED** (the factory already enforces the mask-free
half: `icon_qa`'s `alpha_min` gate plus the studio's flattened full-square render — but note the
factory has **no leg that detects a *drawn* rounded mask or shadow**, only a leg that the pixels are
opaque; see §9 R20).

---

## 8. Common professional failure modes, named and explained

### 8.1 Letters-as-logos
Writing the initial letter of the app name is the most common shortcut and the weakest mark.
Apple permits it as a *possible exception* — *"displaying a mnemonic like the first letter of your
app's name can help people recognize your app"* — while warning that text *"doesn't support
accessibility or localization, is often too small to read easily, and can make an icon appear
cluttered"* ([Apple HIG](https://developer.apple.com/design/human-interface-guidelines/app-icons)).
Two mechanical reasons: a letterform is a **high-frequency, thin-feature** shape (it violates the
stroke floor in most designs), and it carries **no metaphor** — Apple's first principle in WWDC17 is
*"metaphor, finding an easily identifiable metaphor"*
([WWDC17 822](https://developer.apple.com/videos/play/wwdc2017/822/)). **ON RECORD.** This is a
**rejection rule for recon**, not a computable pixel test — §9 R1 states the weak proxy and says so.

### 8.2 Too much detail
Interior features below the resolution limit. Measured failure: the smallest feature that survives a
launcher cell is ~1.5 px (factory); practice drops interior detail entirely before the store
thumbnail (silhouette-only test, Cursa; 40 px greyscale test, apptenium). **ON RECORD.** Computable
as `thinnest_stroke_px` and `smallest_counter_px` — **already gated** (`≥ 16 px @ 512`).

### 8.3 Multiple competing ideas
Two metaphors in one mark. Apple: *"make it the core idea of your icon, and express it in a simple,
unique way"* (singular). Material: *"Avoid complex icons."* Nielsen's blur experiment shows that two
common forms blur into one another and the mark stops being identifiable. **ON RECORD.** The
computable proxy is weak and must be a *report* (§2.2, §9 R2) — the factory's own `mark-stripes`
would trip a naive version of it.

### 8.4 Low contrast
Either mark-to-field, or field-to-host. The measurable floor for a graphical object is **3:1**
(WCAG 1.4.11 / G207); the factory gates **4.5:1** at 48 px (house). **ON RECORD / MEASURED.**

### 8.5 Dependent on colour
*"Color is not used as the only visual means…"* (WCAG 1.4.1). Under Android 13+ theming the mark is
flattened to one tint (§6), so a colour-dependent mark loses information twice. **ON RECORD.**

### 8.6 Off-centre weight
The mark is arithmetically centred and looks crooked — the *first word* the owner used about the
rejected icon. The established fix is optical alignment: nudge **away from** the visual weight, or
align the **centroid** for a triangle-like shape (bjango, Marvel, Rails Designer; §2.1). The factory
measures it as `optical_offset_px ≤ 0.02 × 512 = 10.24 px`. **ON RECORD / MEASURED.**

### 8.7 Inconsistent stroke weights
Material: *"Do use consistent stroke weights and squared stroke terminals. Don't use inconsistent
stroke weights or rounded stroke terminals"*, and recommended stroke is **2 dp**
([M1](https://m1.material.io/style/icons.html)). The factory enforces this structurally with a
two-value `STROKE` scale (`regular` 16 px, `bold` 24 px) and rejects anything else at render time —
**the strongest single design invariant in the module**, and worth keeping exactly as it is.
**MEASURED.**

### 8.8 Clipart
A stock glyph or emoji as the mark. Neither Play's own guidance nor the platform rules mention it by
name, but Play's impersonation rule and its *"Don't use text or graphic elements that can mislead
users"* clause cover the misleading half, and `visual-identity.mdc` already bans *"emoji, clipart, or
a stock icon as the app's mark"*. **ON RECORD** (Play) / **ON RECORD** (factory rule).

### 8.9 Circular sameness (a ninth, and the one the factory should worry about most)
Nielsen's blur experiment found that **circle-based marks collapse together first** — *"Circular
logical forms… when heavily blurred, all of these logos begin to look alike."* Combined with the
platform's circle keyline and the factory's `MIN_REACH_RATIO`, there is a real drift risk toward a
"disc with something inside" family across slots, which is the same defect as two slots sharing a
palette (`visual-identity.mdc`). **ON RECORD** (practice) / **INFERRED** (the portfolio risk).

---

## 9. What to change, and where (recommendations, not actions taken)

No file was modified except this document. The reconciliation in §5.3 and the rules above point at
four small, bounded changes, in dependency order:

1. **Register or remove `mark-stripes`** in `MARK_REGISTRY` (`icon_design.py:262`) so the live slot's
   declared identity is renderable — Finding 5.3.4. This is the only item that blocks a render today.
2. **One source of truth for the reach ceiling and the reach origin** — Findings 5.3.2 and 5.3.3.
   `check-adaptive-icon.py`, `scripts/core/icon.mjs` and `icon_qa.ICON_GATES["mark_reach_dp"]` all
   encode a bound on the same quantity and currently disagree (33 dp, 33 dp, 31.211 dp) and measure
   it from two different origins. The pattern to follow is already in the tree:
   `play-assets.json → iconGeometry` is what `scripts/core/icon.mjs` reads, and
   `.cursor/rules/verification-discipline.mdc` states the rule (*"A value used by two scripts lives in
   one module and is imported"*). **MEASURED.**
3. **Add the one missing Play leg** — R20, "no drawn mask or shadow". `icon_qa.outer_band()` already
   computes the only region of the canvas a grid-built mark may not occupy, so the sensor is a
   few lines against pixels the pipeline already has. **MEASURED / ON RECORD.** *(This is the one
   rule the factory asserts in prose — `visual-identity.mdc` §"What is banned", `AGENTS.md` — and
   does not measure.)*
4. **Add the two computable design legs as reports, never as gates** — R16/R17 (silhouette
   distinctiveness and the 48 px greyscale render) and R24 (cross-slot identity collision). Every
   bound in those three is a **convention**; gating on them would fail legitimate marks (R2's false
   positive on `mark-stripes` is the proof), and a noisy gate is read past — which is the outcome
   this document exists to avoid.

**The honest limit, stated plainly.** Nothing in this document can make an icon beautiful, and no
program can decide where the eye lands first. What the rules above can do is make the **common,
measured failures unshippable** — a thread too thin to see, a mark outside its mask, a hue doing
work that shape should do, a field of texture where tone belongs, a corner Play will cut — and force
the judgement that remains (the focal point, the metaphor, the palette's character) to the place it
already belongs in this factory: the owner, at the review, with something worth looking at.

---

## References

Primary (platform and standards):

- Google Play icon design specifications — <https://developer.android.com/distribute/google-play/resources/icon-design-specifications>
- Add preview assets to showcase your app (Play Console Help) — <https://support.google.com/googleplay/android-developer/answer/9866151>
- Metadata policy (Play Console Help) — <https://support.google.com/googleplay/android-developer/answer/9898842>
- Best practices for your store listing (Play Console Help) — <https://support.google.com/googleplay/android-developer/answer/13393723>
- Adaptive icons (Jetpack Compose) — <https://developer.android.com/develop/ui/compose/system/icon_design_adaptive>
- `AdaptiveIconDrawable` (API reference) — <https://developer.android.com/reference/android/graphics/drawable/AdaptiveIconDrawable>
- `AdaptiveIconDrawable.java` (AOSP, `SAFEZONE_SCALE = 66f/72f`, `EXTRA_INSET_PERCENTAGE`) — <https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/graphics/java/android/graphics/drawable/AdaptiveIconDrawable.java>
- Implement adaptive icons (AOSP: mask convexity, 66/71 safe zone, [0,100] mask space) — <https://source.android.com/docs/core/display/adaptive-icons>
- Create app icons (mask shapes: circle, squircle, rounded square, square) — <https://developer.android.com/studio/write/create-app-icons>
- Features and APIs, Android 13 (themed app icons) — <https://developer.android.com/about/versions/13/features>
- AEP guideline: Themed App Icons — <https://developer.android.com/distribute/aep/aep-req-theme-app-icons>
- Codelab: Design and preview your app icons — <https://codelabs.developers.google.com/design-android-launcher>
- Support different pixel densities (density table, mipmap rule) — <https://developer.android.com/training/multiscreen/screendensities>
- Android iconography (48×48 dp launcher icon; visible on any wallpaper) — <https://android.googlesource.com/platform/frameworks/base/+/f19e4b420de65306f09c199829e7e06fe210e21d/docs/html/design/style/iconography.jd>
- Legacy launcher icon design doc (density sizes, padding) — <https://android.googlesource.com/platform/frameworks/base/+/dc828acd5fadb266b13cce459b1cacfad8ef7aef/docs/html/guide/practices/ui_guidelines/icon_design_launcher.jd>
- Material 3 — Designing icons (simplify, bold, keylines, 2 dp corners/stroke, optical corrections) — <https://m3.material.io/styles/icons/designing-icons>
- Material 1 — Style: icons (keyline shapes and sizes) — <https://m1.material.io/style/icons.html>
- Apple HIG — App icons — <https://developer.apple.com/design/human-interface-guidelines/app-icons>
- Apple WWDC17 session 822, *App Icon Design* — <https://developer.apple.com/videos/play/wwdc2017/822/>
- Apple macOS HIG — App Icon (archived; unique outline) — <https://web.archive.org/web/20181206014649/https:/developer.apple.com/design/human-interface-guidelines/macos/icons-and-images/app-icon/>
- Carbon Design System — Icons/Usage (4.5:1, sizes, alignment) — <https://carbondesignsystem.com/elements/icons/usage/>
- WCAG 2.1 SC 1.4.1 Use of Color — <https://www.w3.org/WAI/WCAG21/Understanding/use-of-color.html>
- WCAG 2.1 SC 1.4.11 Non-text Contrast — <https://www.w3.org/WAI/WCAG21/Understanding/non-text-contrast.html>
- W3C technique G207 (3:1 for icons) — <https://www.w3.org/WAI/WCAG21/Techniques/general/G207.html>

Design practice (secondary, cited as practice):

- bjango — Formulas for optical adjustments (centroid alignment; 112.84 % area scale) — <https://bjango.com/articles/opticaladjustments/>
- Rails Designer — Mathematical and optical alignment — <https://railsdesigner.com/mathematical-optical-alignment-design/>
- Marvel — Optical adjustment: logic vs designers — <https://marvelapp.com/blog/optical-adjustment-logic-vs-designers/>
- Dutchicon — Optical weight in icons — <https://dutchicon.com/optical-weight-icons/>
- Jim Nielsen — Logo integrity brought into focus (blur + greyscale test) — <https://blog.jim-nielsen.com/2014/logo-integrity/>
- Cursa — Logo design foundations: scalability and legibility checks — <https://cursa.app/en/page/logo-design-foundations-scalability-and-legibility-checks>
- apptenium — App icon design: a developer's platform-first guide (40 px silhouette test) — <https://apptenium.com/guides/app-icon-design>
- Coblis colour-blindness simulator — <https://www.color-blindness.com/coblis-color-blindness-simulator/>
- Accessibility Labs — Colourblindness and inclusive design — <https://accessibility-labs.com/colorblindness/>
- EULE Institute — Colour blindness & UX design — <https://euleinstitute.com/en/blog/color-blindness-ux-design/>

Factory sources read for this document (all **MEASURED**):

- `scripts/lib/icon_design.py` — the grid, the stroke scale, the primitives, `MARK_REGISTRY`
- `scripts/lib/icon_qa.py` — `ICON_GATES`, the thirteen gated legs, `mask_survival`, `outer_band`
- `scripts/check-adaptive-icon.py` and `scripts/core/icon.mjs` — the shipped-vector reach check
- `scripts/core/assets.mjs`, `scripts/lib/play-assets.json` — icon asset windows and `iconGeometry`
- `apps/2026-10-01-lawn-round-log/store/studio.json` — the `mark-stripes` declaration and palette
- `.cursor/rules/visual-identity.mdc`, `AGENTS.md`, `.cursor/rules/verification-discipline.mdc`

---

## Encodable rules

Each entry is **(a)** the assertion, **(b)** the numeric threshold or bound, **(c)** the source.
A rule a program can evaluate over the declared `mark` primitives (fractions of the 108 dp
viewport), the declared `palette`, and the rendered 512 px raster is marked **computable**; the ones
that need a human are marked **recon rule** and are not dressed as checks. Labels: MEASURED / ON
RECORD / INFERRED / UNVERIFIED. **Convention** marks a threshold that is the factory's choice rather
than a platform law.

The letter `K` below is the circle keyline = `33 dp − 2 dp clearance = 31 dp = 0.287037` of the
108 dp canvas (already `KEYLINE_CIRCLE` in `icon_design.py:91`).

---

**R1 — The mark is not a letterform or a stock glyph. (recon rule, not computable)**
(a) The `mark` declares a metaphor/keyline family, and the studio refuses marks whose primitives
form a Latin capital letter, a binary glyph, or a borrowed icon set. (b) No numeric threshold; the
weak proxy a scanner *can* compute — *a single ink component whose filled-area/bounding-box IoU is
≥ 0.7 and whose bbox is within 5 % of a capital-letter aspect range* — is **UNVERIFIED** as a
detector and would false-positive on `mark-stripes` (see R2), so it must not gate. (c) Apple HIG
(letters permitted but warned against), Play Metadata policy (misleading symbols), factory
`visual-identity.mdc` (no clipart). **ON RECORD** + **UNVERIFIED** (the proxy).

**R2 — One idea: bounded primitive budget, reported not gated.**
(a) A mark declares ≤ 8 primitives and ≤ 3 palette roles, and the number of visually separate masses
(connected components after closing gaps < 1 `regular` stroke) is reported. (b) `primitives ≤ 8`,
`roles ≤ 3`, `masses` **report-only**; **no hard limit on masses**, because `mark-stripes` is
legitimately four equal masses and any "one dominant mass ≥ 50 % of ink" rule fails it. (c) Apple
HIG *"minimal number of shapes"*, Material *"Simplify"*, Nielsen blur test. **INFERRED** —
**convention**.

**R3 — Silhouette extent inside the guaranteed zone.**
(a) The longest side of the silhouette bounding box lies inside the 66 dp safe square. (b)
`0.30 ≤ extent ≤ 0.6111` (canvas fractions; the ceiling is 66/108). (c) Factory
`icon_qa.ICON_GATES["silhouette_extent"]`; platform safe zone
([Adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)).
**MEASURED** (factory) / **ON RECORD** (platform).

**R4 — Reach from the declared optical anchor, inside the circle keyline.**
(a) Outermost ink ≥ 75 % of and ≤ 100 % of the circle keyline from the mark's declared anchor. (b)
`0.215278 ≤ reach ≤ 0.287037` fractions of the canvas, i.e. **23.25–31.0 dp**, plus one antialiased
canvas pixel of tolerance at the ceiling (31.211 dp). (c) Factory `icon_design.mark_reach()`,
`icon_qa` `mark_reach_dp`. **MEASURED.** **Convention** (the 75 % floor; note R4.1).

**R4.1 — If the aim is the platform's own logo floor, the ratio is 0.774, not 0.75.**
(a) The platform says a logo is *"at least 48x48 dp"* and 48 dp across = 24 dp reach from centre;
24/31 = **0.774**. (b) Either raise `MIN_REACH_RATIO` to 0.774 or record 0.75 as a deliberate house
floor. (c) [Adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive).
**ON RECORD** (platform) / **INFERRED** (reconciliation).

**R5 — No ink outside the 66 dp safe circle.**
(a) Zero mark pixels outside the 33 dp-radius circle. (b) `mark_outside_safe_circle_px = 0`. (c)
Factory `icon_qa`; AOSP `SAFEZONE_SCALE = 66f/72f` and the CTS-enforced safe-zone rule
([source.android.com](https://source.android.com/docs/core/display/adaptive-icons), [AOSP
source](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/graphics/java/android/graphics/drawable/AdaptiveIconDrawable.java)).
**MEASURED** / **ON RECORD**.

**R6 — Optical centring.**
(a) The silhouette centroid is within tolerance of the mark's declared optical anchor (the geometric
centre for centrally symmetric silhouettes; the centroid, not the bbox centre, for triangles and
other asymmetric forms). (b) `optical_offset ≤ 0.02` canvas = **10.24 px @ 512**. (c) bjango
(centroid alignment; circle-vs-square area scale 112.84 %), Marvel, Rails Designer; factory
`icon_qa` `optical_offset_px`. **ON RECORD** (practice) / **MEASURED** (factory) — **convention**
(the 2 % bound).

**R6.1 — Every gate that measures reach must measure from the same anchor.**
(a) `check-adaptive-icon.py` and `scripts/core/icon.mjs` must use `icon_design.optical_anchor(mark)`,
not `viewport/2`. (b) Numeric equality between the two reported reach figures for the same mark.
(c) This document §5.3, Finding 5.3.2. **MEASURED** (the disagreement exists) — **finding**.

**R7 — Thinnest stroke survives the smallest real render.**
(a) Every drawn stroke and contour ≥ the `regular` weight. (b) `≥ 16 px @ 512` (0.03125 canvas),
which is **1.5 px at the conservative 48 px reading** and 2.25 px at the launcher reading. (c)
Factory `MIN_FEATURE`; the 1.5 px rationale is the factory's own measurement. **MEASURED** —
**convention** (no platform number exists).

**R8 — Smallest enclosed counter clears the same floor.**
(a) Every enclosed negative-space region's thinnest width ≥ `regular`. (b) `≥ 16 px @ 512`. (c)
Factory `MIN_COUNTER`; Cursa's "interior gaps collapse" failure mode. **MEASURED** / **ON RECORD**
(practice) — **convention**.

**R9 — One stroke scale.**
(a) Every stroked primitive uses a declared weight, and the max/min weight ratio is bounded. (b)
`weight ∈ {regular = 0.03125, bold = 0.046875}`; ratio **≤ 1.5**; nothing between or outside. (c)
Material *"Do use consistent stroke weights"*; factory `STROKE`. **ON RECORD** / **MEASURED**.

**R10 — Mark-to-field contrast at launcher size.**
(a) Luminance contrast between mark ink and its local field in a 48 px render. (b) **≥ 4.5:1**
(factory, house) — the platform/WCAG floor is **≥ 3:1** (WCAG SC 1.4.11 + technique G207; Carbon
states 4.5:1 for icons). (c) Factory
`icon_qa.ICON_GATES["downscale_contrast_48px"]`; [WCAG 1.4.11](https://www.w3.org/WAI/WCAG21/Understanding/non-text-contrast.html),
[G207](https://www.w3.org/WAI/WCAG21/Techniques/general/G207.html), [Carbon](https://carbondesignsystem.com/elements/icons/usage/).
**MEASURED** (factory) / **ON RECORD** (3:1) — the 4.5:1 is **convention**.

**R11 — Colour is not load-bearing: the mark passes in one colour.**
(a) Re-render the mark with a single ink colour over the themed/launcher background and re-run R3,
R4, R5, R7, R8 and R12. Every one must still pass, and flattening must not merge two declared
masses that were separate in colour. (b) Boolean per leg; no new number except the legs it reuses.
(c) WCAG SC 1.4.1 *"Color is not used as the only visual means…"*; Android 13+ monochrome layer and
Android 16 QPR 2 auto-theming. **ON RECORD** / **INFERRED** (the test construction).

**R12 — Ink coverage window (figure/ground).**
(a) The fraction of the 66 dp safe square covered by mark ink. (b) `0.15 ≤ coverage ≤ 0.85`. (c)
Factory `icon_qa` (the rejected pack measured 0.99). **MEASURED** — **convention** (no platform
source).

**R13 — Field is one tonal plane, not a texture.**
(a) High-frequency RMS on non-mark pixels and residual against a best-fit linear ramp. (b)
`field_high_frequency_rms ≤ 0.5` luma; `field_linear_ramp_rms ≤ 2.0` luma; `field_grid_periodicity`
reported only. (c) Factory `icon_qa` `ICON_GATES`. **MEASURED** — **convention** (thresholds), the
*defect* is ON RECORD (Play/Apple allow gradients; the factory rejects texture).

**R14 — Distinct hues are bounded, and adjacent roles are separable.**
(a) The mark uses ≤ 3 palette roles; for every pair of roles that are spatially adjacent (bbox gap
< 1 `regular` stroke), the pair differs by **either** ≥ 3:1 luminance contrast **or** a shape cue.
(b) `roles ≤ 3`; `Δlum_contrast ≥ 3:1` for adjacent pairs. (c) WCAG 1.4.1 note (*"a contrast ratio of
3:1 or greater"* counts as a lightness distinction); Material *"Don't rely on colour"*;
`visual-identity.mdc` (one colour + one accent). **ON RECORD** (3:1) / **INFERRED** (adjacency test)
— **convention** (the role cap).

**R15 — Greyscale survival.**
(a) Convert the rendered icon to greyscale (0 % saturation) and re-measure R10. (b) mark-vs-field
contrast in greyscale **≥ 3:1** (WCAG), factory target 4.5:1. (c) [WCAG 1.4.1](https://www.w3.org/WAI/WCAG21/Understanding/use-of-color.html),
[Accessibility Labs](https://accessibility-labs.com/colorblindness/) (grayscale-first method).
**ON RECORD** / **MEASURED**.

**R16 — The silhouette is distinctive, not a blob (report leg).**
(a) Intersection-over-union of the filled silhouette with (i) its convex hull, (ii) the circle of
equal area, (iii) the filled bounding box. (b) Report the three IoUs; a mark with
`IoU(convex hull) > 0.95` **or** `IoU(circle) > 0.90` **or** `IoU(bbox) > 0.90` is flagged
**non-distinctive** for a human read, never auto-failed. (c) Nielsen (circular forms collapse under
blur), Apple (*"unique shape"*), Material (keyline families imply most marks are circle/square-like).
**ON RECORD** (the risk) / **INFERRED** (the metric) — **convention** (the bounds). Reported because
a hard gate here would punish a legitimate disc or page mark.

**R17 — 48 px greyscale silhouette render, kept as an artifact.**
(a) Emit a 48 px greyscale render of the flattened store icon beside the store pack and report its
silhouette IoU with the 512 px silhouette. (b) Report `IoU(48 px silhouette, 512 px silhouette
downscaled)`; flag `< 0.85` as "the shape changed at launcher size". (c) apptenium (40 px silhouette
test), Apple (test on Home screen / in a folder), Nielsen (blur/grayscale). **ON RECORD**
(practice) / **INFERRED** (the 0.85 proxy) — **convention**.

**R18 — Mask survival at every shape the platform uses.**
(a) Fraction of mark ink clipped by each of: 72 dp circle, 72 dp squircle (corner radius = side/4),
72 dp square, 66 dp circle. (b) **0 px** for the 66 dp circle (gates); report the other three.
(c) Factory `icon_qa.mask_survival`; [Create app icons](https://developer.android.com/studio/write/create-app-icons)
(circle/squircle/rounded-square/square previews), [Adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive).
**MEASURED** / **ON RECORD**.

**R19 — Alpha is fully opaque, and corners are ink.**
(a) Minimum alpha anywhere, including the four corners, on the 512 px store render. (b)
`alpha_min = 255`. (c) Play *recommends* an opaque background because *"Transparent assets will
display the background color of Google Play UI"*
([Play icon design specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications));
the factory's `255` is **stricter than required**. **MEASURED** (factory) — **convention**.

**R20 — No drawn rounded mask or shadow. (currently UNENFORCED — the rule the factory claims)**
(a) The store icon contains no baked-in corner mask and no drop shadow: the corner pixels of the
512 px square are the field (not transparent, not black), and there is no luminance trough along the
expected shadow direction outside the silhouette. (b) Proposal: `alpha == 255` at all four corners
(exists: `alpha_min`) **and** the outer 5 % band's luminance standard deviation ≤ the field's, i.e.
no shadow gradient; a shadow is a directional gradient in a band no mark built on the grid can
occupy (`icon_qa.outer_band()` already computes exactly this band). (c) Play: *"Shape: Full square…
Radius will be equivalent to 30 % of icon size"*, *"Shadow: None… Don't add drop shadows to your
final asset"*, *"Don't round the edges of your final asset"*
([Play icon design specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications)).
**ON RECORD** (Play) / **MEASURED** (the missing leg) — this is the **only Play icon rule with no
sensor in this tree**; `alpha_min` catches transparency but not a drawn mask or shadow.

**R21 — The mask the platform applies is the only mask: no self-masking.**
(a) The adaptive foreground's outermost ink must be the mark, not a rectangle/disc drawn to
simulate a mask; the silhouette bbox must not be a plain square within 2 % of the 66 dp zone on all
four sides. (b) Boolean; report. (c) Platform *"The layers must not have masks or background shadows
around the outline of the icon"* ([Adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)).
**ON RECORD** / **INFERRED** (the detector) — **convention** (2 %).

**R22 — Play store geometry.**
(a) 512 × 512, 32-bit PNG with alpha, sRGB, ≤ 1024 KB, full square. (b) exact; `≤ 1 048 576` bytes.
(c) Play; factory already enforces size and byte cap in `scripts/core/assets.mjs:288-291` and
`play-assets.json` `icon`. **ON RECORD** / **MEASURED**.

**R23 — Colour count at small size is bounded by the palette, not by the bitmap.**
(a) The rendered icon's distinct-colour count is not bounded by a number (a gradient legitimately
produces thousands), so the check is on **mark roles**, not pixels: `roles ≤ 3` (R14) and no role
covers `< 3 %` of mark ink unless it is a declared accent. (b) `roles ≤ 3`; any role `< 3 %` of ink
is reported as "detail that will disappear". (c) Apple (*"minimal number of shapes"*, simple
background), Material (*"make icons graphic and bold"*), factory `apply_palette` role model.
**ON RECORD** / **INFERRED** — **convention** (3 %).

**R24 — Two slots never ship the same identity.**
(a) Across the catalogue, compare every pair of rendered 512 px icons and every pair of declared
palettes; report the most similar pair. (b) Report `IoU(greyscale silhouettes)` and the palette's
per-role ΔE; flag `IoU > 0.6` **or** ≥ 2 palette roles within ΔE ≤ 10 as a **portfolio collision**.
(c) Play Spam policy and repetitive-content risk (`AGENTS.md`, `docs/monetization-strategy.md` §1.2),
factory `visual-identity.mdc` (*"Two slots must not share a mark, a palette, or a component kit"*).
**ON RECORD** (policy) / **INFERRED** (the metric and bounds) — **convention**. *Pairwise similarity
of the six phone shots already exists as `visual-qa.py`'s `composition` leg; the cross-slot
comparison does not.*

**R25 — Every icon rule is a report with a named threshold, and the threshold is labelled.**
(a) Any leg added for the rules above states, in its own output, whether its bound is a **platform
requirement** or a **factory convention** — the same distinction `docs/self-learning.md` draws for
facts. (b) Boolean: the leg's emitted text contains the word for its class. (c)
`.cursor/rules/verification-discipline.mdc` (*"a check that cannot fail is not a check"*, and its
corollary: a check whose bound is taste must say so). **ON RECORD** (factory) — this rule exists
because R4, R6, R9, R12, R13, R14, R16, R17, R19, R23 and R24 all contain at least one **convention**
threshold, and a convention presented as a measurement is the failure this repository keeps
recording.
