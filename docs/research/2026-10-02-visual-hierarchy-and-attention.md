# Visual hierarchy and attention — where the eye lands first, and how to engineer it

**Measured 2026-10-02.** Every external claim carries one of four labels: **MEASURED** (read on the
cited source, URL given), **ON RECORD** (asserted by a named published source but not this document's
own measurement), **INFERRED** (this document's reasoning from the cited facts), **UNVERIFIED** (could
not confirm). Where a claim is a **convention** — widely repeated, not empirically established — it
says so in the row, because a convention dressed as a measurement is a defect in this repository in
its own right.

## What this document is

The owner's instruction, recorded 2026-10-02, names the factory's biggest weakness as the thing a
professional does that the factory cannot: *"pick the focus, tune the gamut, decide where the eye goes
first, what the palette should be."* This is the second half of that instruction. A separate stream
owns colour science and icon design; **this document owns the ordering of attention** — which element
the eye should reach first, which second, which never, and how that ordering is measured rather than
felt.

It is grounded in one real slot read without editing it:
`apps/2026-10-01-lawn-round-log/`. Its `RoundScreen.kt` is the product — a hero band with a gradient
and two stat tiles, a two-item section strip, an ordered `LazyColumn` of customer rows, and a pinned
bottom action bar — and its declared palette lives in the same slot's `store/studio.json`. Its type
ramp is `ui/theme/Type.kt`. Every example below refers to that screen, so each rule can be read
against a real tree.

It is written to **add to** `scripts/core/uiux.mjs`, which another session is editing, and to
duplicate none of it. The boundary is in §10.

---

## 1. Pre-attentive attributes: the channels you actually get

### The finding

**MEASURED**, Treisman & Gelade (1980), *A feature-integration theory of attention*, found two
qualitatively different visual search modes:

- a target defined by **one** separable feature (a colour, an orientation, a size) is detected in
  parallel — reaction time is nearly flat in the number of distractors, with slopes averaging
  **3.1 ms/item**;
- a target defined by a **conjunction** of two or more features (red *and* vertical) requires serial,
  self-terminating attention, with slopes of about **60 ms/item** — roughly 19× the per-item cost.

— <https://www.cs.princeton.edu/courses/archive/spring08/cos598B/Readings/TreismanGelade1980.pdf> ·
<https://en.wikipedia.org/wiki/Feature_integration_theory>

**ON RECORD**, NN/g's dashboard guidance names the pre-attentive set it relies on — *"length, 2D
position, area, angle, and color"* — and separates the two jobs: length and 2D position map naturally
onto **quantity**, while colour and shape carry **categorical** membership and *"do not easily
communicate quantitative relationships."*
— <https://www.nngroup.com/articles/dashboards-preattentive/>

**ON RECORD**, the SFU lecture notes on visual attention list the pre-attentive categories as
**form** (line orientation, length, width, size, curvature, spatial grouping, blur, numerosity),
**colour** (hue, intensity), **motion**, and **spatial position** (2D location), and state the
practical consequence for design: *"Detectable differences on one channel pop out; a target that needs
two features does not."* They also record the honest limit — *"The model of preattention has been
evolved into a gamut rather than a threshold"*, i.e. pop-out is a gradient, not a switch.
— <http://www.sfu.ca/siatclass/IAT814/Spring2009/Lectures/IAT814-week5A.pdf> (**ON RECORD**)

### The practical consequence

1. **The channel budget is small and shared.** Orientation, size, hue, motion and 2D position are
   processed before attention; everything else (a word, a border, an icon's meaning) is processed
   after it. A screen therefore has on the order of a handful of genuinely pre-attentive levers, and
   they are spent by the first few elements that use them.
2. **Two channels for one emphasis is stronger than one.** A "big red" element is more robust than a
   "big" one and than a "red" one, for a concrete reason and not a stylistic one: it survives the
   loss of either channel — greyscale removes hue, a small screen removes scale, colour-vision
   deficiency removes hue for ~8 % of men. This is the same claim as WCAG 1.4.1 *Use of Color*
   (**MEASURED**, <https://www.w3.org/TR/2023/REC-WCAG22-20231005/>) applied to hierarchy.
3. **Two elements on the same channel destroy each other.** If the biggest thing is A and the biggest
   thing is also B, neither pops out; the visual system has to serial-search. This is the mechanism
   behind "everything is shouting" (§2) and it is why emphasis is a *relative* property (§5).
4. **A conjunction emphasis is a search cost, not a pop-out.** An element that is only distinguishable
   from its neighbours by the *combination* of two features (e.g. "the primary-coloured filled pill"
   among outlined pills and primary-coloured links) is found by serial search. The fix is to give the
   focal element a feature it alone possesses.
5. **Motion outranks everything and is therefore expensive.** **ON RECORD**, NN/g: *"Fast animations
   are more likely to attract attention when they happen outside the user's focus of attention"*, and
   *"Animated user interface elements are tempting and powerful tools, yet they can easily waste a
   precious currency: users' attention and time."*
   — <https://www.nngroup.com/articles/animation-usability/>

### The interaction with change blindness

**ON RECORD**, NN/g defines change blindness as *"people's tendency to ignore changes in a scene when
they occur in a region that is far away from their focus of attention"*, and names the exact failure
this section is about: a carousel whose many regions change at once, or an "Out of stock" label on a
button that *"doesn't stand out when the rest of the display stays the same"*. The recommended
mitigations are the hierarchy rules of this document applied to a change: *"Use appropriate visual
emphasis for significant new elements (such as contrast, size, and padding)"* and *"place important
new visual elements close to the point where the users' eyes are already focused."*
— <https://www.nngroup.com/articles/change-blindness-definition/> ·
<https://www.nngroup.com/articles/change-blindness/>

**INFERRED:** for a field app whose whole job is "one tap records a visit", this is the single most
load-bearing perception fact in the document. The status change after a tap must land where the thumb
just was (the row), with a real emphasis change (fill + glyph + word), because a change elsewhere on
the screen — a hero number updating, a balance recalculating — will compete with it and one of the two
will be missed.

---

## 2. How many levels of hierarchy a screen can carry

This is the most numerically answerable question in the document, because NN/g publishes counts.

**ON RECORD**, *Visual Hierarchy in UX: Definition* — <https://www.nngroup.com/articles/visual-hierarchy-ux-definition/>:

- **Scale:** *"Use no more than 3 sizes — small, medium, and large."*
- **Big elements:** *"Limit how many elements are big to a maximum of 2 so that they do stand out and
  reinforce the hierarchy."*
- **Contrast:** *"Use no more than 3 contrast variations for complex designs. If everything is
  contrasted, then nothing stands out. Effective designs often use different treatments for header,
  subheader, and body text."*
- **Colour:** *"In common, uncomplicated designs, limit your color use to 2 primary and 2 secondary
  colors."*

**ON RECORD**, *5 Principles of Visual Design in UX* — <https://www.nngroup.com/articles/principles-visual-design/>:
*"To create a clear visual hierarchy, use 2–3 typeface sizes to indicate to users what pieces of
content are most important."*

**ON RECORD**, Material 3 makes the same point from the other direction. Its type scale ships **15
baseline styles** (and 15 emphasized ones), and its own guidance says a product should use far fewer:
*"Your product likely will not need all 15 default styles from the Material Design type scale. In this
[example], five sizes are chosen for a reduced set while the rest are removed."* The same page states
the design requirement plainly: *"the rendered type scale should aim to provide impactful contrast
between sizes by avoiding small differences."*
— <https://m3.material.io/styles/typography/type-scale-tokens> (**MEASURED** as quoted)

### The failure mode

**ON RECORD**, NN/g's own worked example of the failure is a page where *"the elements on this page are
all relatively equal in size and color and lack breathing room. Because most text is in all caps, very
little of it stands out."* — same URL. The mechanism is §1: when every element carries an emphasis
channel, no channel carries information, and the eye falls back to serial reading of a screen that was
meant to be glanced at.

### The honest counting problem for a task screen

NN/g's 3-sizes rule is written for a **marketing/content page** with header, subheader and body.
A dense task screen legitimately needs more tiers: a title, a primary value, row names, body/metadata,
and a caption/label tier. The `lawn-round-log` Round screen renders **six distinct type sizes** —
23 sp (headlineSmall title), 18 sp (`TallyNumerals` stat value), 17 sp (titleMedium name), 15 sp
(bodyMedium/labelLarge), 13 sp (bodySmall/labelMedium), 12 sp (labelSmall) — and four emphasis tiers.

**INFERRED:** the honest position is that NN/g's **3** is the anchor for the *content* hierarchy
(what the page is about), and a task screen adds a **supporting tier** (labels/captions) and a
**numeral tier** (monospaced figures). That yields a defensible ceiling of **≤ 6 distinct sizes and
≤ 4 emphasis tiers**, with the top tier carrying exactly one element and the second tier carrying at
most two *instances* (a repeated list row counts once). The extension is this document's, not NN/g's,
and §11 marks it so.

**MEASURED (in-tree):** `ui/theme/Type.kt` in `lawn-round-log` deliberately loosens every line height
above the Material baseline — 23/31 (1.35), 17/24 (1.41), 15/22 (1.47) against Material's 24/32
(1.33), 16/24 (1.50), 14/20 (1.43). The slot's own comment gives the reason (read at arm's length,
outdoors, with the mower running), and the consequence for hierarchy is real: looser leading makes
size differences *less* salient, so a looser ramp needs slightly larger size steps than a tight one to
read as the same number of tiers.

---

## 3. Scanning patterns — what they measure, and what transfers to a phone at a kerb

### What the patterns actually are

**ON RECORD**, NN/g's eye-tracking taxonomy — <https://www.nngroup.com/articles/text-scanning-patterns-eyetracking/>:

- **F-pattern** — *"In the absence of subheadings and bullets, users tend to fixate on the words toward
  the beginning of lines and toward the top of the page."* Two horizontal sweeps then a vertical
  sweep down the left.
- **Layer-cake** — fixations placed *"mostly on the page's headings and subheadings"*, few on the body
  between them, until a heading of interest is found.
- **Spotted** — irregular fixations, no strong shape.
- **Commitment** — the user reads essentially all of it.

**MEASURED (as a stated comparison)**, the layer-cake article's own table:
*"Unlike the F-pattern, which is usually not very effective at extracting the right information from
text, the layer-cake pattern can guide users to the right place on a page."* F-pattern **efficiency:
Low**; layer-cake **efficiency: High**. F-pattern characteristics: *"Columns of text with little text
that stands out"*; layer-cake: *"Pages with well-signaled headings and subheadings."*
— <https://www.nngroup.com/articles/layer-cake-pattern-scanning/>

**MEASURED**, the F-pattern is explicitly a **default**, not a goal: *"The F-pattern is the default
pattern when there are no strong cues."* The pattern is a symptom of missing structure.
— <https://www.nngroup.com/articles/f-shaped-pattern-reading-web-content/>

### The Z-pattern, honestly

**UNVERIFIED / convention.** NN/g does not publish a "Z-pattern". What it publishes is the opposite
finding, from an eye-tracking study of alternating image/text layouts: zigzag layouts produced
*"residual fixations"* — accidental fixations on whatever replaced what the eye was looking at after a
scroll — and made it *"more difficult to ignore decorative imagery and caused users to stumble over
these unhelpful images and immediately redirect their fixations"*, whereas **aligned** layouts let
users skip images entirely.
— <https://www.nngroup.com/articles/zigzag-page-layout/> ·
<https://www.nngroup.com/articles/gazeplots-zigzag/>

**INFERRED:** a "Z-pattern" for a hero/marketing layout is a convention, not an NN/g measurement, and
the only measured part of it is the weaker, more general fact underneath — *predictable alignment
scans faster than alternation*. This document treats "Z-pattern" as a marketing convention and derives
no rule from it.

### Right-rail and banner blindness

**ON RECORD**, NN/g: *"Almost no fixations within advertisements … the heatmaps also show how users
don't fixate within design elements that resemble ads, even if they aren't ads."* And the operative
sentence for this factory: *"anything that's overly large or colorful risks being ignored"* — a loud
band does not attract attention, it teaches users to skip that region. One participant *"looked at the
right rail just once, presumably deciding that it contained only ads"* and then never returned.
— <https://www.nngroup.com/articles/banner-blindness-original-eyetracking/> ·
<https://www.nngroup.com/articles/banner-blindness-old-and-new-findings/> ·
<https://www.nngroup.com/articles/fight-right-rail-blindness/>

**INFERRED:** this is a warning aimed squarely at the factory's house pattern of a **large full-bleed
gradient hero**. A hero earns its place when it is the *only* loud region and carries the one number
the user opened the app for; a hero that repeats what the list below already says is a region users
will learn to skip, and skipping is measured in fixations, not taste.

### Which patterns apply to a one-handed field app used at a kerb

**INFERRED**, from the sources above:

| Pattern | Applies to | Verdict for a kerb-side, one-handed, task app |
|---|---|---|
| **F-pattern** | columns of undifferentiated text | **Applies to the *list*** — the left edge of the customer rows is the scan axis. Keep the leading element (position chip, then name) at a constant left edge so the eye can run down it. |
| **Layer-cake** | well-signalled headings | **Applies weakly** — only where the screen genuinely chunks (section strip, group headers). *"Support the layer-cake pattern"* is NN/g's advice for content, and it is the same advice that makes a task list scannable. |
| **Spotted / commitment** | irregular / full reading | Spotted is the realistic mode for a row a user has stopped on; commitment is what a form demands. |
| **Z-pattern** | marketing hero layouts | **Does not apply.** Convention, not measurement (above). |
| **Right-rail blindness** | page right-hand columns | **Applies by analogy** to any screen region that is loud for its own sake: the eye learns to skip it. |
| **Fitts's law** (not a scan pattern but the governing law of thumb reach) | target acquisition | **Applies and dominates.** See §8. |

The honest general statement: **NN/g's patterns are measurements of *reading text*, and a task app is
not read, it is scanned for a target and then acted on.** The transferable content is (a) the top-left
bias, (b) that structure (headings, grouping) beats prose for scanning, and (c) that alignment beats
alternation. The non-transferable content is the specific letter shapes — nobody's eye traces an F on
a phone at a kerb, and a rule that asserts one does would be astrology.

---

## 4. Figure/ground and gestalt, applied to screens

### Figure/ground

**ON RECORD**, the gestalt principles explain *"how people decide whether several individual elements
are part of the same group"*; figure/ground is the same mechanism applied to what is an object at all.
NN/g's visual-hierarchy article gives the design consequence: *"Applying color to a design makes some
elements appear to advance and others to recede, and, thus, determines what grabs our attention."*
— <https://www.nngroup.com/articles/visual-hierarchy-ux-definition/>

**INFERRED:** a card, panel or hero is a *figure* only if it measurably separates from its ground —
by luminance, by tonal elevation, or by an edge. A `Surface` whose `color` equals the background is
not a group; it is markup. Material 3's own surface-container levels exist for exactly this:
*"Surface container is the default role, but the others are especially helpful for creating hierarchy
and nested containers in layouts for expanded screens"*, with the roles named by emphasis from
`surfaceContainerLowest` to `surfaceContainerHighest`.
— <https://m3.material.io/styles/color/roles> (**MEASURED** as quoted)

### The grouping principles, with the concrete UI consequence

**ON RECORD**, NN/g — <https://www.nngroup.com/articles/gestalt-proximity/>:

- **Proximity** — *"Items close together are likely to be perceived as part of the same group."* And
  the strength ordering that matters for the rules below: *"Proximity is one of the most important
  grouping principles and can overpower competing visual cues such as similarity of color or shape."*
  **Consequence:** group by **whitespace first**. The group is made by the *outer* gap, not the inner.
- **Similarity** — <https://www.nngroup.com/articles/gestalt-similarity/>: *"when items share some
  visual characteristic, they are assumed to be related"*; *"the shared characteristic can unite
  elements despite a distributed placement."* It is *"not necessarily the strongest grouping
  principle as it is often overpowered by proximity or common region"* but it is *"the most
  resilient."* **Consequence:** repeat a treatment (a type style, a pill shape, a status tone) for a
  category of thing across the screen, and it will hold together even when the items are far apart.
- **Common region** — <https://www.nngroup.com/articles/common-region/>: *"items within a boundary are
  perceived as a group"*; *"Creating a clear boundary is a strong visual cue that can overpower other
  grouping principles such as proximity or similarity."* And the cost: *"too many borders and colored
  boxes that exist purely for decoration add clutter to the interface."* **Consequence:** reach for a
  container when whitespace cannot be afforded (a dense list, a nested panel), and *only* then.
- **Closure / connectedness** — the same body of work: a rounded rectangle's implied boundary reads as
  an object even when it is only partially drawn. **Consequence:** a card's shape is a grouping
  decision, not decoration.

**ON RECORD**, the sharper form of the same rule for controls — NN/g, *Closeness of Actions and
Objects*: *"we frequently see users overlook features because layouts violate the proximity rule. When
buttons, drop-downs, checkboxes, or other actionable GUI elements are too far away from the objects
they act on, people don't see them. Often, users don't realize what they're missing and simply assume
the features aren't available."*
— <https://www.nngroup.com/articles/closeness-of-actions-and-objects-gui/>

**INFERRED:** this is the rule the `lawn-round-log` row violates least — three status icon buttons sit
directly under the customer they act on, inside the same `Surface` — and the rule a *bottom action
bar* most often violates, because the primary action's object (a new customer, a changed day) is not
the thing next to it. A pinned bar is legitimate when its action is global to the screen; it is a
proximity defect when its action belongs to one row.

### Why a divider is usually worse than spacing

**ON RECORD**, Fluent 2: *"Space is used to denote groups of associated information. Used correctly,
spacing creates logical sections of content on a page without having to use lines or other graphical
elements as a divider."* The same page adds the emphasis half: *"UI elements that have more spacing
around them draw more focus and tend to be perceived as higher in importance than elements that have
less space around them."*
— <https://fluent2.microsoft.design/layout>

**INFERRED:** a divider works by *closure* (an enclosure implies common region), so it groups the
items it encloses — the opposite of the intent when it is used to *separate* two items that are meant
to read as different. Spacing separates; a line usually joins.

---

## 5. Contrast hierarchy: the emphasis channels, and the rule that makes them relative

### The channels

| Channel | Mechanism | Notes |
|---|---|---|
| **Size** | pre-attentive (form) | the strongest and most reliable; NN/g caps it at 3 levels |
| **Weight** | weight of stroke; pre-attentive as line width | M3 ships a whole second set of "emphasized" styles that are *the same sizes with higher weight* |
| **Case** | all-caps | **not** a free emphasis channel — see below |
| **Letter-spacing** | tracking | M3 assigns per-style tracking; `lawn-round-log` sets it to 0 on purpose (§6) |
| **Colour / value** | pre-attentive | works by *contrast*, not by hue |
| **Opacity** | alpha on the `on*` role | M3: *"For disabled states in M3, it's still acceptable to use 'on-x' colors with alpha values."* |
| **Position** | pre-attentive (2D position) | top-left bias, reading gravity |
| **Enclosure** | common region | strongest grouping cue, costs clutter |

**MEASURED**, Material 3's emphasis doctrine — <https://developer.android.com/develop/ui/compose/designsystems/material3>:
*"Emphasis in M3 is provided using variations of color and its on-color combinations"* and *"Using
different font weights for text."* The two mechanisms are the same two this document's rules target.

**MEASURED**, the M3 emphasized styles are exactly a weight lever: *"They have a higher weight and
other minor adjustments compared to the baseline styles, and are best applied to bold, selection, and
other areas of emphasis."* — <https://m3.material.io/styles/typography/type-scale-tokens>

### "If everything is bold, nothing is bold" — the relative rule

This is the load-bearing claim of the section, and NN/g states it directly:

> **ON RECORD:** *"Use no more than 3 contrast variations for complex designs. If everything is
> contrasted, then nothing stands out."*
> — <https://www.nngroup.com/articles/visual-hierarchy-ux-definition/>

**INFERRED:** the reason is §1 — an emphasis is a *difference*, and perception has no absolute scale
for "important". A page where one element is bold and everything else is regular has one emphasis; a
page where everything is bold has none, and the reader loses the signal entirely. Formally: emphasis
is a property of the *distribution* of a channel across the screen, not of any element.

### WCAG is a floor for legibility, not a hierarchy target

**MEASURED**, WCAG 2.2 — <https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html> and
<https://www.w3.org/WAI/WCAG21/Understanding/non-text-contrast.html>:

- **SC 1.4.3 (Minimum, AA):** normal text **≥ 4.5:1**; **large text** (≥ 18 pt regular or ≥ 14 pt bold)
  **≥ 3:1**.
- **SC 1.4.6 (Enhanced, AAA):** **≥ 7:1** normal, **≥ 4.5:1** large.
- **SC 1.4.11 (Non-text Contrast, AA):** UI components and meaningful graphics **≥ 3:1** against
  adjacent colours; *"any visual information necessary to indicate state, such as whether a component
  is selected or focused must also ensure … a minimum 3:1 contrast ratio."*
- **SC 1.4.1 (Use of Color):** colour may not be the *only* visual means of conveying information.

**INFERRED:** these are **legibility floors**, and a hierarchy built by sitting every tier exactly on
4.5:1 has *no* hierarchy — the audit example below makes the point with numbers. The correct shape is
a **ladder** well above the floor: e.g. primary text 12:1, secondary 7:1, caption 4.5:1, disabled 3:1.
The floor exists so that the lowest rung is still readable, not so that every rung can be the same.

### All caps is a trap, and the research is genuinely split

**ON RECORD / MEASURED (mixed, and stated as mixed):**

- **MEASURED**, NN/g's glanceable-typography study: *"Lowercase lettering required 26% more time for
  accurate reading than uppercase"* — but only for *"a word or two in isolation"*, and the authors
  *"acknowledged that 'uppercase letters are more prone to crowding and letter confusion'."* NN/g's
  own summary adds the boundary: *"these findings don't necessarily apply to scanning or reading
  longer text. We generally don't recommend using all-caps text for longer passages."*
  — <https://www.nngroup.com/articles/glanceable-fonts/>
- **MEASURED**, the older typographic record: Paterson & Tinker found *"a sizable advantage (close to
  14%) in reading times for sentences written in lowercase compared with upper case text"*, explained
  by lowercase word-shapes permitting *"reading by word units, while all capitals tend to be read
  letter by letter."* The Wikipedia summary of that literature: *"all-capitals printing takes at least
  one-third more space than lower case"* and *"Scientific testing from the 20th century onward has
  generally indicated that all-caps text is less legible and readable than lower-case text."*
  — <https://www.uv.es/mperea/fournet_et_al> · <https://en.wikipedia.org/wiki/All_caps>
- **MEASURED**, NN/g's visual-hierarchy example names all-caps as a *cause* of a failed hierarchy:
  *"Because most text is in all caps, very little of it stands out."* — same URL as the relative rule.

**INFERRED:** all-caps is a *weak* emphasis channel that costs legibility for prose and buys a little
legibility for one- or two-word labels. The honest rule is narrow: all-caps for short labels only, and
never as the primary way a tier is distinguished.

### Icon and colour blindness

**MEASURED**, WCAG 1.4.1 (same TR URL): *"Color is not used as the only visual means of conveying
information, indicating an action, prompting a response, or distinguishing a visual element."*
**INFERRED:** for `lawn-round-log` this is already honoured — `VisitStatusPill` carries the word as
well as the tone — and it is the rule that makes the greyscale test in §9 meaningful rather than
decorative.

---

## 6. Type scale: ratios with numbers, and how a ramp is built from one base

### Modular scales

A modular scale multiplies a base size by a fixed ratio per step. With a **16 px** base
(**ON RECORD**, calculator and guide sources agree on the arithmetic; the exact px are computed from
16 × ratioⁿ):

| Ratio | Name | Step 1 | Step 2 | Step 3 | Step 4 |
|---|---|---|---|---|---|
| 1.125 | Major Second | 18.0 | 20.2 | 22.8 | 25.6 |
| 1.200 | Minor Third | 19.2 | 23.0 | 27.6 | 33.2 |
| 1.250 | Major Third | 20.0 | 25.0 | 31.2 | 39.1 |
| 1.333 | Perfect Fourth | 21.3 | 28.4 | 37.9 | 50.5 |
| 1.500 | Perfect Fifth | 24.0 | 36.0 | 54.0 | 81.0 |
| 1.618 | Golden Ratio | 25.9 | 41.9 | 67.8 | 109.7 |

— <https://quirkulator.com/calculator/modular-type-scale-calculator> ·
<https://www.css-architecture.com/design-system-token-fundamentals-naming-conventions/typography-scale-systems/building-a-modular-type-scale-with-custom-properties/> ·
<https://www.modularscale.com/>

**ON RECORD** on which ratio to choose: *"Major Third (1.25) produces a gentler ramp suitable for
dense data UIs; Perfect Fourth (1.333) creates clear hierarchy faster, ideal for marketing or
editorial layouts"*, and *"A 1.125 (major second) and a 1.333 (perfect fourth) both look reasonable
for a subtitle one step above body text — 18px vs 21px — but by the fifth step above a 16px base they
diverge to 29px vs 67px."*
— <https://web-font-optimization.com/typography-fundamentals-system-architecture/type-scale-modular-grids/choosing-a-modular-scale-ratio/> ·
<https://www.typographymaster.com/guide/type-scale-systems>

**MEASURED:** Material 3's own type-scale guidance names its default: it refers to *"the Major Second
type [scale]"* as the basis for adjusting a style — i.e. M3's ramp is anchored at ~**1.125–1.2**,
the dense-UI end of the table, which is consistent with M3 shipping 15 closely-spaced steps and then
telling products to delete most of them.
— <https://m3.material.io/styles/typography/type-scale-tokens>

### The Material 3 baseline ramp, in numbers

**MEASURED** (Android platform sizes and line heights) — the M3 baseline scale, `sp`:

| Style | Size | Line height | Ratio (lh/size) | Weight |
|---|---:|---:|---:|---|
| displayLarge | 57 | 64 | 1.12 | Regular |
| displayMedium | 45 | 52 | 1.16 | Regular |
| displaySmall | 36 | 44 | 1.22 | Regular |
| headlineLarge | 32 | 40 | 1.25 | Regular |
| headlineMedium | 28 | 36 | 1.29 | Regular |
| headlineSmall | 24 | 32 | 1.33 | Regular |
| titleLarge | 22 | 28 | 1.27 | Regular |
| titleMedium | 16 | 24 | 1.50 | Medium |
| titleSmall | 14 | 20 | 1.43 | Medium |
| bodyLarge | 16 | 24 | 1.50 | Regular |
| bodyMedium | 14 | 20 | 1.43 | Regular |
| bodySmall | 12 | 16 | 1.33 | Regular |
| labelLarge | 14 | 20 | 1.43 | Medium |
| labelMedium | 12 | 16 | 1.33 | Medium |
| labelSmall | 11 | 16 | 1.45 | Medium |

— <https://github.com/material-components/material-components-android/blob/master/docs/theming/Typography.md> ·
<https://developer.android.com/develop/ui/compose/designsystems/material3>

**INFERRED:** two facts fall out that are worth stating because they are not obvious. First, M3's
**size** steps are tight (57→45 is 1.27; 16→14 is 1.14) and **line-height ratios run the other way** —
large sizes are set tighter (1.12–1.33) and small sizes looser (1.43–1.50). A ramp that increases
leading as it decreases size is *inverting the platform's own relationship*, which is why
`lawn-round-log`'s deliberately loose leading at every size is defensible for legibility but costs
hierarchy.

### Line length and line height

**ON RECORD (and explicitly flagged as convention):** the widely repeated rule is **45–75 characters
per line, ~66 the target** (Bringhurst), with **line height 1.5** as the default and 1.6–1.7 for long
lines. The source that publishes it says outright that it is not research-backed: *"these 60 to 80
characters are something we might be familiar with from books … this is not backed by research,
shorter lines do not seem to increase reading speed, as Mary Dyson points it out."*
— <https://pimpmytype.com/line-length-line-height/> · <https://www.uxpin.com/studio/blog/optimal-line-length-for-readability/>

**MEASURED**, and in tension with it: Dyson's review of the screen-reading literature reports that
Duchnicky & Kolers found *"blocks of text of 75–100 cpl were read more quickly than when the text was
presented in shorter lines"*, that Dyson & Haselgrove found no reading-speed effect between 55 and
100 cpl, and that the optimal printed length is often put at **52 characters** (Tinker via Rayner &
Pollatsek). It concludes that eye movements on screen are not different from print, and that long
lines are faster but need their return sweeps managed.
— <https://stu.westga.edu/~ssynan1/literacy/Dyson.pdf>

**INFERRED:** the honest statement is that **50–75 cpl is a convention with a real mechanism behind
it** (return-sweep difficulty on long lines), not a measured optimum for a phone. For an app, the
practical rule is not a character count at all: **a list row's primary text should be short enough to
scan, and body prose should be capped by a width bound rather than a character count.** The measurable
form is in §11 (H14): body text is bounded by a maximum width, and it is derived from the type size,
not hand-set.

---

## 7. Spacing systems

### The grid

**MEASURED**, Android: *"Android UI utilizes an 8 dp grid for layout, components, and spacing …
Smaller elements such as icons, type, and some elements within components are best aligned to a 4 dp
grid."* — <https://developer.android.com/design/ui/mobile/guides/layout-and-content/grids-and-units>

**MEASURED**, Material 3: *"The base unit of measurement md.sys.measurement.space100 is 8dp. The main
spacing units are multiples of 8dp"*, with *"Values other than multiples of 8 … like 2dp, 4dp, 6dp,
and 10dp"* as **nested** units. M3 defines three categories — *"Padding: Space inside an element",
"Gap: Space between elements", "Margins: Space outside an element"* — and the multiplier pattern
*"space225 = 18dp (8dp x 2.25)"* for extensions.
— <https://m3.material.io/styles/spacing/tokens> · <https://m3.material.io/styles/spacing>

### The proximity ratio — a convention, and labelled as one

**CONVENTION**, not a measurement. Multiple design-system and practitioner sources publish the same
rule of thumb: the space *between* groups must be substantially larger than the space *within* a
group, with **1:3 to 1:4** as the common target and **1:2 as the floor**.

- *"A ratio of approximately 1:3 to 1:4 between within-group spacing and between-group spacing
  produces reliable grouping perception. Ratios below 1:2 produce ambiguous grouping."* — source is a
  practitioner reference, **not** peer-reviewed: <https://deplaybook.com/pages/strategies/law-of-proximity>
- *"Padding around a group must be larger than spacing within the group"*; *"Internal spacing must be
  no more than 50% of external spacing"* — a design-engineering skill file, **not** a study:
  <https://github.com/oborchers/fractional-cto/blob/main/visual-design-principles/skills/whitespace-density/SKILL.md>
- The strongest single data point in the family is Apple's **settings list**, where within-group
  spacing is ~8 pt and between-group ~32 pt (a **4:1** ratio): <https://developer.apple.com/design/human-interface-guidelines/layout>
  — but HIG states the *values*, not a study that derived them.

**Honest label:** **CONVENTION / ON RECORD for the AI-generated claim**, **MEASURED only for Apple's
published values**. No controlled study was located that measures the detection threshold for grouping
as a function of the within/between spacing ratio. A rule built on it must say so.

**INFERRED:** the ratio is nevertheless *directional* and cheap to check, and the failure it catches is
real and common — a list whose row gap equals its section gap has no sections. The safe form of the
rule is a **floor of 2×** (conservative relative to every published recommendation) with a **target of
3–4×**, labelled convention.

### Spacing as emphasis

**ON RECORD**, Fluent 2: *"UI elements that have more spacing around them draw more focus and tend to
be perceived as higher in importance than elements that have less space around them."* And the
converse warning: *"UI elements that are set close to each other might be overlooked. People may
notice the grouping but not process each individual item."* — <https://fluent2.microsoft.design/layout>

**MEASURED (in-tree):** `lawn-round-log`'s Round screen uses **16 dp** horizontal content padding for
the list and the action bar, **10 dp** between rows, **4–14 dp** inside rows, and **32 dp** inside the
empty state. The row gap (10) is *less* than the screen margin (16) — correct, because the margin is
outside the whole list — but the row gap and the section-strip gap are the same order of magnitude,
which is exactly the ambiguity §4 warns about and §11 encodes.

---

## 8. Where the primary action belongs, and where money surfaces go

### Thumb reach, measured

**MEASURED**, Hoober's 2013 field study of **1,333** observed users (his own summary of the observation
set; the widely-quoted 780 is the subset that was *interacting*):

- **49 %** held one-handed, **36 %** cradled with one thumb, **15 %** two-handed.
- Of one-handed users, **67 %** used the right thumb, **33 %** the left.
- Later refinement: **75 %** of users *touch* the screen with only one thumb, but *"fewer than 50 % of
  users hold their phone with one hand"* — because cradling and finger-tapping are common; and
  *"people hold phones in multiple ways … and change their methods of grasping their phone without
  realizing it."*

— <https://www.uxmatters.com/mt/archives/2013/02/how-do-users-really-hold-mobile-devices.php> ·
<https://www.uxmatters.com/mt/archives/2017/05/design-for-fingers-touch-and-people-part-2.php> ·
<https://www.uxmatters.com/mt/archives/2017/03/design-for-fingers-touch-and-people-part-1.php>

**MEASURED**, the one-handed reach study: task performance *"deteriorated as the target distance
increased, especially at 45 mm or more"* from the natural thumb position, and the comfortable region
runs *"in the diagonal direction between the upper right and the lower left side of the screen."*
— <https://exa.ai/library/publication/z4kjxn0px8w>

**MEASURED**, Fitts's law: *"The movement time to a target depends on the size of the target and the
distance to the target"*, and — the part that matters for bottom placement — *"while the edge
placement offers an advantage in mouse- or trackball-driven UIs, it offers no advantage for
touchscreens. In fact … it actually takes longer to hit targets placed around the edges, possibly
because for touchscreens, it is possible to overshoot and hit beyond the edge of the screen."* NN/g's
concrete placement advice: *"Submit button in a form should be placed next to the last form field …
Mobile designs that place the Submit button at the very top of the page … result in suboptimal
movement time."*
— <https://www.nngroup.com/articles/fitts-law/>

**MEASURED**, NN/g touch targets: *"for users to quickly and accurately select a touch target, its
minimum size should be 1cm × 1cm (0.4in × 0.4in)"*, and — the sentence that governs a field app —
*"if an app … is to be used when the user is moving, [targets should be] bigger to allow for more
error."* — <https://www.nngroup.com/articles/touch-target-size/>

### The platform guidance

**MEASURED**, Material 3 FAB: *"For compact [< 600 dp] and medium breakpoints, the best place for the
FAB is the lower right corner of a screen"*, and *"In compact windows, place the FAB in the bottom
trailing edge."* — <https://m3.material.io/components/floating-action-button/accessibility>

**MEASURED**, Material 3 extended FAB: *"Use an extended FAB on screens with long, scrolling views that
require persistent access to an action"*; *"the extended FAB should be placed at the bottom of the
screen"*; and the explicit negative — *"Don't place the extended FAB in the upper half of a mobile
screen, as it disrupts the reading of the UI."*
— <https://m3.material.io/components/extended-fab/guidelines>

**MEASURED**, Compose's `FabPosition` offers `Center`, `End`, `Start`, `EndOverlay` — all *"at the
bottom of the screen"* — which is the platform's own confirmation that the primary-action zone is the
bottom. — <https://developer.android.com/reference/kotlin/androidx/compose/material3/FabPosition>

**ON RECORD**, Apple HIG: *"Use a filled button for the most likely action in a view … avoid using
too many filled buttons in a view. Too many filled buttons can increase cognitive load"*, and *"Keep
the number of prominent buttons to one or two per view."*
— <https://developer.apple.com/design/human-interface-guidelines/components/menus-and-actions/buttons>

### The trade-off when a pinned bar collides with the insets — the principle

The factory measured a real defect of exactly this kind on a **3-button-navigation** device: a bottom
bar whose content is drawn at the window's bottom edge lands under the ~48 dp scrimmed navigation bar,
so the control exists and cannot be tapped. At `targetSdk` 36 the bottom offset is disabled and this
is the *default*, not an edge case; the full mechanism, the inset types and their subsumption are
**MEASURED** in `docs/research/2026-10-02-android-window-insets.md` §1 and §5(c) and are not restated
here.

**The principle that governs it, in one sentence:** *the visual field may reach the window edge, but
the interactive target may not.* A full-bleed surface behind the bar is correct and deliberate; the
tappable content inside it must sit above the gesture pill or the 3-button bar, and the inset that
guarantees that belongs to the pinned element's own bottom padding (or to the scrollable's
`contentPadding`, so it may scroll behind the bar and still stop above it) — never to the window's
default behaviour, and never as a fixed `padding(bottom = 48.dp)` that guesses the bar's height.

**INFERRED:** the hierarchy consequence is separate from the inset mechanism: a pinned bar is
**chromium that competes with content for attention**. `lawn-round-log`'s bar carries a filled
`Button` (Add customer) and an `OutlinedButton` (Push day) plus four icon-text doors — eight
affordances in a region that swallows the bottom of the list. This is the correct place for the *job's*
primary action and the wrong place for the four rare doors, which are content-less by construction and
cost the list its last 120 dp. That judgement is a hierarchy rule, and §11 encodes the arithmetic
(≤ 1 primary + ≤ 1 secondary action in a pinned bar).

### Money surfaces

There is no controlled mobile study this document could locate that fixes "the paywall goes at the
bottom". What follows is **CONVENTION + INFERRED**, anchored on two MEASURED platform facts:

- Material's single-primary-action doctrine: the FAB is *"the screen's single primary action"*, and M3
  warns against competing floating elements
  (<https://m3.material.io/components/extended-fab/guidelines>: *"Avoid putting other floating
  components … on screen with the extended FAB"*).
- Apple's prominence rule: *"Keep the number of prominent buttons to one or two per view"*, and *"Use
  style — not size — to visually distinguish the preferred choice"*
  (<https://developer.apple.com/design/human-interface-guidelines/components/menus-and-actions/buttons>).

**INFERRED rules:**

1. **The job's primary action outranks the purchase.** On a screen whose purpose is the job, the
   purchase affordance must not be the filled button, must not be the largest type, and must not be
   the first thing on the screen. If a purchase is the only filled action a user sees, the app has
   told them the purchase *is* the job.
2. **One purchase surface, and it is named.** The offer is a distinct destination or a
   bottom sheet/dialog reached from a labelled affordance — not a spontaneous interstitial, and not a
   second competing surface.
3. **The purchase surface appears after the value, not before it.** The first screen after launch is
   the job, not the offer. (This is also the Play-compliance shape the factory already enforces: no
   launch-blocking gate unless the data handling actually triggers one.)
4. **`lawn-round-log` is the correct shape as a placement, not as an emphasis:** the unlock is a
   bottom-bar *door* with a word and a glyph, the hero number is the owed total and not the price.
   A door is reachable, labelled and ignorable — which is what a purchase affordance should be on a
   screen that is not about purchasing.

---

## 9. How professionals audit hierarchy

These are the published methods, and each maps to a machine-checkable test in §11.

### The squint test

**MEASURED**, LukeW, who named it: *"So how can you tell when there's the right amount of visual
hierarchy in a UI design? Just squint. This will blur the design just enough to quickly identify if
the important elements stand out."* And the strategic point that makes it a *hierarchy* test rather
than a beauty test: *"which elements are the important ones? … To know the answer, you need to
understand what product you are making, for whom, and why."*
— <https://www.lukew.com/ff/entry.asp?2013=>

**MEASURED**, NN/g's numeric form: it demonstrates the test with **blur radii of 5, 10 and 20 pixels**,
and its 20-px example reveals *"an unintended hierarchy, with one of the recently played items being
the most prominent element of the page due to its strong color"* — and the further finding that
*"it's not enough to design the template — you must also consider the content that will fill it."*
— <https://www.nngroup.com/articles/visual-hierarchy-ux-definition/>

**INFERRED:** the radii are the usable numbers. A 10-px blur removes text and leaves ink mass; a 20-px
blur removes shape and leaves colour mass. The two tests answer different questions: *what is biggest*
(10 px) and *what is loudest* (20 px). Both must agree with the declared focal element.

### The blur test

**ON RECORD**, the practitioner form: *"Blur the page until text is unreadable — what remains visible
represents what your visitor processes in the first second"*, with checks: headline remains visible,
CTA stands out and is not competing, decorative elements do not overpower functional ones, and the
mobile version passes too. A 10–15 px Gaussian blur is the published recipe.
— <https://www.blurtest.com/blog/usability-testing-checklist-what-to-test-before-your-website-goes-live> ·
<https://polypane.app/blog/debug-your-visual-hierarchy-with-the-squint-test/>

### The greyscale / contrast audit

**ON RECORD**, from a published hierarchy-audit method: elements that survive a desaturation have high
*value* contrast; the audit goal is *"not to maximize contrast everywhere"* but to keep the ladder
ordered — *"A body text contrast ratio of 7:1 and a caption contrast ratio of 4.5:1 is a correct
hierarchy. A body text ratio of 4.5:1 and a primary heading ratio of 4.5:1 is a hierarchy failure —
nothing distinguishes the heading from the body text except size."* The same method defines the
**primary-action weight test**: the CTA should have the highest or near-highest contrast, and *"if a
decorative badge, promotional banner, or navigation item has higher contrast, those elements are
drawing more visual weight than the primary action."*
— <https://dev.to/137foundry/how-to-audit-visual-hierarchy-in-an-existing-web-design-50aj> (**ON RECORD**)

### Counting emphasis levels, and the first-glance test

**ON RECORD**, the checklist form: *"One element dominates — something is clearly most important"*,
*"Size decreases from primary to tertiary"*, *"Spacing: section spacing larger than element spacing …
no arbitrary values (17px, 23px) … consistent scale used (4/8/16/24/32/48/64px)"*, and *"CTA color
never on non-clickable elements"*. It also publishes the symptom→cause table this document's rules
mechanise: *"Everything looks same → no size hierarchy"*, *"Can't find CTA → competing colors"*,
*"Feels chaotic → inconsistent spacing."*
— <https://mm.dreamineering.com/docs/products/product-design/visual-hierarchy-checklist> (**ON RECORD**)

**INTERNAL, ON RECORD (this factory):** blurring replaces text with solid blocks — Polypane's
*"Placeholdifier"* — which is the same test with the text-content variable removed; it is what makes
"the heading is still visible" a statement about layout rather than about wording.
— <https://polypane.app/blog/debug-your-visual-hierarchy-with-the-squint-test/>

---

## 10. What the existing sensor already measures — the boundary

`scripts/core/uiux.mjs` (read 2026-10-02, another session is editing it) registers these check ids:

| Check | What it measures |
|---|---|
| `uiux.navigation` | host-owned back stack (`NavDisplay` + `rememberNavBackStack`), not a hand-rolled list |
| `uiux.adaptive` | window size class / pane / adaptive-grid APIs present |
| `uiux.text-layout` | fixed extents around text, `maxLines = 1` with no `overflow` |
| `uiux.motion` | an animation API exists on a multi-screen slot |
| `uiux.a11y` | every interactive element named; every **declared** target ≥ 48 dp |
| `uiux.insets` | edge-to-edge opt-in, `Scaffold` `innerPadding` consumed, no `statusBars`-only screen, pinned bar without a bottom inset, IME handling |
| `uiux.platform` | predictive-back opt-in; M3-Expressive alpha dependency; product-conditional surfaces as notes |
| `uiux.scaffold`, `uiux.rule` | the factory's own half: the scaffold and the rule text |

`docs/ui-ux-standards.md` §"What this standard does not check" states the boundary this document
crosses: *"Whether the palette, hierarchy and depth are good — that is `DESIGN.md`,
`.cursor/rules/visual-identity.mdc`, `scripts/visual-qa.py` and the owner's judgement."*

**So the boundary is exact.** `uiux.*` measures *mechanism* — an API is present, an inset is
consumed, a target is big enough. It deliberately does **not** measure *ordering* — which of two
elements is meant to be seen first, whether the type ramp has tiers, whether groups separate. That
unmeasured half is precisely "where the eye goes first", and it is what §11 proposes to make readable.

Two things must not be duplicated: `uiux.a11y` owns 48 dp and accessible names, and `uiux.insets` owns
the inset API. §11's action-placement rules state *which* control is the primary action and *where on
the screen* it sits; they do not restate the target size or the inset mechanism.

---

## 11. Encodable rules

Each entry is **(a) the assertion, (b) the numeric threshold or bound, (c) the source** — and each
carries a label. Proposed family: `hierarchy.*`, in the slot `ui` group. "Source tree" means readable
from `app/src/main/**/*.kt` and the slot's `Type.kt`/`Color.kt`; "screenshot" means readable from a
rendered frame. **Scoping note that applies to all of them:** a *repeated list row* counts once — the
assertions are about the screen's chrome and its one-off content, not about multiplying a row by its
item count. This scoping is **INFERRED** and is the largest single judgement in the list.

---

**H1 — One focal point.** (a) Exactly one element on a screen occupies the highest emphasis tier.
(b) count = 1, never 0, never ≥ 2. (c) **ON RECORD** — NN/g: *"Make the most important element
biggest"*, *"Limit how many elements are big to a maximum of 2"*:
<https://www.nngroup.com/articles/visual-hierarchy-ux-definition/>. Source: screenshot (ink-mass rank)
+ source tree (which style is largest).

**H2 — One headline, and not in a row.** (a) A screen references at most one style from
`{displayLarge…displaySmall, headlineLarge…headlineSmall}` and it is not used inside a repeated row.
(b) ≤ 1 per screen; 0 allowed. (c) **ON RECORD** — NN/g *"use 2–3 typeface sizes"*
(<https://www.nngroup.com/articles/principles-visual-design/>); M3 *"five sizes are chosen for a
reduced set"* (<https://m3.material.io/styles/typography/type-scale-tokens>). Source: source tree
(`MaterialTheme.typography.<style>` occurrences per screen file).

**H3 — A screen's type-size count is bounded.** (a) The number of distinct `fontSize` values rendered
on one screen is ≤ 6, and no more than 4 of them are *content* tiers (the rest are labels/captions or
the numeral face). (b) ≤ 6 distinct sizes; ≤ 4 content tiers. (c) **ON RECORD** for the anchor (NN/g
*"no more than 3 sizes"*, same URL); **INFERRED** for the extension to a task screen (label tier +
numeral tier). Source: source tree, resolving each style through the slot's `Type.kt`. Reference slot
is at the bound: 6 sizes, 4 tiers.

**H4 — No accidental near-duplicates.** (a) Two distinct type sizes in the **same typeface family**
used on one screen differ by ≥ 5 %; equal sizes are permitted only when weight differs. (b) ≥ 1.05×,
target ≥ 1.10×. (c) **MEASURED** as the problem M3 names — *"avoiding small differences"*
(<https://m3.material.io/styles/typography/type-scale-tokens>) — and **INFERRED** for the numeric
floor (Material's own Major Second step is ~1.125; 1.05 is the "not a duplicate" floor). Source: source
tree. Reference slot: closest pair is 18/17 = 1.059, monospaced vs sans, so it passes on the family
exemption.

**H5 — The focal element is not distinguishable by hue alone.** (a) Desaturating the frame preserves
the identity of the rank-1 element. (b) The rank-1 element must differ from its neighbours on ≥ 2 of
{size, weight, value-contrast, position, enclosure}. (c) **MEASURED** for the requirement — WCAG 1.4.1
*"Color is not used as the only visual means of conveying information"*
(<https://www.w3.org/TR/2023/REC-WCAG22-20231005/>); **ON RECORD** for the *"make the most important
element distinct … on one visual channel, but avoid over-differentiation"* guidance
(<http://www.sfu.ca/siatclass/IAT814/Spring2009/Lectures/IAT814-week5A.pdf>). Source: screenshot
(desaturated rank comparison) + source tree.

**H6 — One accent fill.** (a) At most one element on a screen uses the `primary` role as an action
container, plus at most one selected-state fill. (b) ≤ 2 `primary`-filled surfaces per screen. (c) **ON
RECORD** — Apple: *"Refrain from adding color to the background of multiple controls"*
(<https://github.com/Prisma-Labs-Dev/apple-skills/blob/HEAD/skills/hig/color.md>); M3: *"Primary
container: Standout fill color against surface, for key components like FAB"*
(<https://m3.material.io/styles/color/roles>). Source: source tree (`color = …primary` on containers).
Reference slot: the two stat tiles use `primaryContainer`/`secondaryContainer` as a *gradient field*,
which is a container, not a control — the rule applies to controls and passes.

**H7 — One prominent button.** (a) A screen declares at most one filled `Button`; every other action
uses `OutlinedButton`, `TextButton` or a tonal/icon variant. (b) ≤ 1 filled; Apple allows ≤ 2 prominent
per view, so ≤ 2 is the stretched bound and 1 is the target. (c) **ON RECORD** — Apple HIG buttons
(*"Keep the number of prominent buttons to one or two per view"*):
<https://developer.apple.com/design/human-interface-guidelines/components/menus-and-actions/buttons>.
Source: source tree (`Button(` count). Reference slot: 1 filled + 1 outlined — passes.

**H8 — The primary action sits in the bottom zone.** (a) The filled button's vertical centre is in the
bottom third of the window, or it is the last action of a bottom-anchored container in source order.
(b) ≥ 2/3 of window height. (c) **MEASURED** — M3 FAB *"In compact windows, place the FAB in the bottom
trailing edge"* (<https://m3.material.io/components/floating-action-button/accessibility>); extended
FAB *"Don't … in the upper half of a mobile screen"*
(<https://m3.material.io/components/extended-fab/guidelines>); **MEASURED** Fitts/NN/g *"closer targets
are faster to acquire"* and *"Submit … should be placed next to the last form field"*
(<https://www.nngroup.com/articles/fitts-law/>). Source: screenshot. **Not** the inset — that stays
`uiux.insets`.

**H9 — A pinned action bar carries at most two actions.** (a) A bottom-anchored bar holds ≤ 1 primary
(filled) and ≤ 1 secondary action, plus icon-only or text doors that are *not* the job's actions.
(b) ≤ 2 non-icon affordances; bar height ≤ 25 % of window height. (c) **INFERRED** from the prominence
rule (H7) and from §8's measurement that the bar competes with the list for the bottom of the screen;
the 25 % bound is this document's derivation (**INFERRED**, no source). Source: source tree + screenshot.
Reference slot: 2 non-icon + 4 text doors + 120 dp — H9 would flag the four doors.

**H10 — Grouping is made by spacing, not by lines.** (a) A `HorizontalDivider`/`Divider` between two
list items whose vertical gap is already ≥ 16 dp is a finding; a screen's divider count must not exceed
the number of group boundaries spacing cannot express. (b) 0 dividers where the adjacent gap ≥ 16 dp;
≤ 1 per screen in the common case. (c) **ON RECORD** — Fluent: *"spacing creates logical sections of
content on a page without having to use lines"* (<https://fluent2.microsoft.design/layout>); NN/g:
*"too many borders and colored boxes that exist purely for decoration add clutter"*
(<https://www.nngroup.com/articles/common-region/>). Source: source tree.

**H11 — Between-group gap ≥ 2× within-group gap.** (a) For every pair of adjacent groups in a vertical
layout, the between-group gap is at least twice the within-group gap. (b) ratio ≥ 2.0 (floor), target
3.0–4.0. (c) **CONVENTION** — published rule of thumb 1:3–1:4, floor 1:2
(<https://deplaybook.com/pages/strategies/law-of-proximity>,
<https://github.com/oborchers/fractional-cto/blob/main/visual-design-principles/skills/whitespace-density/SKILL.md>);
**MEASURED only for Apple's values** (8 pt within / ~32 pt between in settings = 4:1,
<https://developer.apple.com/design/human-interface-guidelines/layout>). No controlled study located —
**this is a convention and must be labelled one**. Source: source tree (`Arrangement.spacedBy` /
`Spacer` / `padding` grouped by containment). Reference slot: row gap 10 dp vs list margins 16 dp — the
*section* strip gap is not measurably larger than the row gap, so H11 would flag it.

**H12 — Spacing comes from the grid.** (a) Every spacing literal under `app/src/main/` is a multiple of
4 dp; layout-level gaps and margins prefer multiples of 8 dp. (b) 100 % on the 4 dp grid, ≥ 80 % on the
8 dp grid. (c) **MEASURED** — Android *"If using a baseline grid, stick to measurements of 4 and 8"*
(<https://developer.android.com/design/ui/mobile/guides/layout-and-content/grids-and-units>); M3
*"space100 is 8dp … the main spacing units are multiples of 8dp"*
(<https://m3.material.io/styles/spacing/tokens>). Source: source tree. Reference slot: 16/10/14/12/8/4 —
10 and 14 are 4-grid but not 8-grid, so it passes the floor and misses the preference.

**H13 — A group container is a figure, or it is nothing.** (a) A `Surface`/`Card` used to express a
group differs from its parent by ≥ 1.05 luminance ratio or by a visible edge. (b) ratio ≥ 1.05 or a
border. (c) **INFERRED** from the figure/ground mechanism (§4) and M3's surface-container emphasis
ladder (<https://m3.material.io/blog/tone-based-surface-color-m3>, **MEASURED**). Source: screenshot.

**H14 — Body prose is width-bounded.** (a) A body-text block's measure is bounded by an explicit
maximum width or a layout constraint, not by the window. (b) bound present; no character-count
threshold is asserted, because the published 45–75 cpl range is **CONVENTION**
(<https://pimpmytype.com/line-length-line-height/>: *"this is not backed by research"*) and the screen
research is in tension with it
(<https://stu.westga.edu/~ssynan1/literacy/Dyson.pdf>). Source: source tree (`widthIn(max = …)` or a
`Box`/constraint on the prose). **Honest limit: this rule measures that a bound exists, not that it is
right.**

**H15 — Reading order matches emphasis order.** (a) The first non-chrome text element in the screen's
composition is the focal element, or the focal element is in the top third of the window. (b) focal
element y &lt; 1/3 window height, or focal element is the first text emitted. (c) **ON RECORD** —
Apple: *"place the most important items near the top and leading side"*
(<https://developer.apple.com/design/human-interface-guidelines/layout>); NN/g F-pattern top-left bias
(<https://www.nngroup.com/articles/text-scanning-patterns-eyetracking/>). Source: source tree order +
screenshot. Scoped to the content, not to a bottom-pinned action.

**H16 — The first glance names exactly one thing.** (a) With a 10 px blur, exactly one region is the
most salient; with a 20 px blur, the same region remains the most salient; both match the shot's
declared focal element. (b) blur radii 10 and 20 px; rank-1 count = 1 on each. (c) **MEASURED** for
the radii and the method — NN/g demonstrates 5/10/20 px
(<https://www.nngroup.com/articles/visual-hierarchy-ux-definition/>) and LukeW names the squint test
(<https://www.lukew.com/ff/entry.asp?2013=>); **INFERRED** for automating it as a local-ink-mass rank.
Source: screenshot.

**H17 — The hierarchy survives greyscale.** (a) Desaturating the frame leaves the emphasis rank order
unchanged. (b) rank order identical. (c) **ON RECORD** — the published audit method
(<https://dev.to/137foundry/how-to-audit-visual-hierarchy-in-an-existing-web-design-50aj>);
**MEASURED** for the underlying requirement, WCAG 1.4.1. Source: screenshot.

**H18 — Emphasis is a ladder, not a floor.** (a) Adjacent text tiers differ in computed contrast
against their background by ≥ 1.5× (e.g. 12:1 vs 7:1 vs 4.5:1), or in size by ≥ 10 %, or in weight.
(b) ≥ 1.5× contrast step, or ≥ 1.10× size step. (c) **ON RECORD** for the principle — *"If everything
is contrasted, then nothing stands out"*, *"no more than 3 contrast variations"*
(<https://www.nngroup.com/articles/visual-hierarchy-ux-definition/>), and the worked example *"body
7:1 and caption 4.5:1 is a correct hierarchy; body 4.5:1 and heading 4.5:1 is a failure"*
(same audit URL). **The 1.5× step is INFERRED.** Source: screenshot (pixel + declared palette).

**H19 — Contrast floors are met.** (a) Body text ≥ 4.5:1; large text (≥ 18 pt regular / ≥ 14 pt bold)
≥ 3:1; interactive component boundaries and states ≥ 3:1. (b) WCAG 2.2 AA. (c) **MEASURED** — SC 1.4.3
and SC 1.4.11 (<https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html>,
<https://www.w3.org/WAI/WCAG21/Understanding/non-text-contrast.html>). Source: screenshot +
`Color.kt`. Note this is the **floor**; H18 is the ladder.

**H20 — The primary action wins the contrast test.** (a) The primary action's contrast against its
background is among the top two contrast pairs on the screen, and no decorative element exceeds it.
(b) rank ≤ 2. (c) **ON RECORD** — the primary-action weight test: *"The primary CTA should have the
highest or near-highest contrast ratio on the page"*
(<https://dev.to/137foundry/how-to-audit-visual-hierarchy-in-an-existing-web-design-50aj>); **INFERRED**
for the rank-2 tolerance. Source: screenshot.

**H21 — One reading column.** (a) A screen's rows share a common leading text edge within a small
tolerance; the list's left edge is a straight vertical scan axis. (b) ± 4 dp across rows. (c) **ON
RECORD** — NN/g's zigzag study found aligned layouts scan better than alternating ones and that
alternation causes residual/wasted fixations
(<https://www.nngroup.com/articles/zigzag-page-layout/>); Apple HIG: *"Align components with one
another to make them easier to scan"* (<https://developer.apple.com/design/human-interface-guidelines/layout>).
Source: screenshot. Reference slot: the row's leading element is the position chip at a constant 14 dp
inset — passes.

**H22 — A control sits with the object it acts on.** (a) A per-item action is inside or immediately
adjacent to its item's container; a screen-level action is in the chrome, not inside a row. (b) action's
container = the item's container, or a screen-level bar. (c) **ON RECORD** — NN/g *Closeness of Actions
and Objects*: users *"don't see"* controls *"too far away from the objects they act on"*
(<https://www.nngroup.com/articles/closeness-of-actions-and-objects-gui/>). Source: source tree
(nesting). Reference slot: the three status buttons are inside the row's `Surface` — passes.

**H23 — All caps is label-sized only.** (a) No all-caps text at body scale or above; all-caps, if used,
is confined to short labels. (b) all-caps text size ≤ the slot's `labelLarge`/14 sp-equivalent and
length ≤ 30 characters. (c) **MEASURED, and the research is mixed and must be reported as mixed** —
NN/g's glanceable study found uppercase *faster* for isolated words but warns it does not apply to
longer text (<https://www.nngroup.com/articles/glanceable-fonts/>); the classic typographic record
finds all-caps ~14 % slower for sentences (<https://en.wikipedia.org/wiki/All_caps>); NN/g names all-caps
as a *cause* of a flattened hierarchy
(<https://www.nngroup.com/articles/visual-hierarchy-ux-definition/>). Source: source tree +
`Type.kt` + screenshot.

**H24 — A money surface never outranks the job.** (a) On a job screen, no purchase element is the
filled button, the largest text, or the first content element; at most one purchase affordance is
visible per screen. (b) 0 purchase elements at the top emphasis tier; ≤ 1 at the second. (c)
**INFERRED**, anchored on **MEASURED** platform doctrine — M3's single-primary-action rule and the
warning against competing floating components
(<https://m3.material.io/components/extended-fab/guidelines>), Apple's *"keep the number of prominent
buttons to one or two per view"* (<https://developer.apple.com/design/human-interface-guidelines/components/menus-and-actions/buttons>).
**No controlled study located that fixes paywall placement — this is a convention, and is labelled
one.** Source: source tree + screenshot.

**H25 — The first screen after launch is the job.** (a) No full-screen purchase surface is the first
destination reached on a cold start; the purchase affordance is reached from the job. (b) 1 job screen
before any purchase surface. (c) **INFERRED** from H24 and consistent with the factory's existing
no-launch-blocking-gate rule (`app-settings.mdc` / the Play compliance floor). Source: source tree
(navigation start destination).

---

### Which of these are convention rather than measurement

Stated plainly, because this repository treats a convention dressed as science as a defect:

| Rule | Status |
|---|---|
| **H11** (between-group ≥ 2× within-group) | **CONVENTION.** A rule of thumb with a real mechanism, published repeatedly at 1:3–1:4 and 1:2 floor; **no controlled study located**. Apple's 4:1 settings spacing is the only measured value in the family. |
| **H14** (body prose width-bounded) | **CONVENTION.** 45–75 cpl / 66 target is *explicitly* not research-backed by the source that publishes it, and the screen-reading literature (Dyson) is in tension with it. This rule therefore asserts only that a bound *exists*. |
| **H23** (all-caps labels only) | **MIXED.** Two bodies of measurement point opposite ways depending on string length; the rule takes the conservative side and says why. |
| **H24 / H25** (money surface placement) | **CONVENTION/INFERRED.** No controlled mobile study located. Anchored only on the platform's single-primary-action doctrine. |
| **H3**'s extension to 6 sizes / 4 tiers | **INFERRED** from NN/g's 3, not itself an NN/g measurement. |
| **H4, H9, H18**'s numeric steps (1.05, 25 %, 1.5×) | **INFERRED** thresholds, derived for checkability. |
| **H1, H2, H5, H6, H7, H8, H10, H12, H15, H16, H17, H19, H20, H21, H22** | Anchored in a **MEASURED** or **ON RECORD** source; the *threshold* is sometimes this document's rounding of that source. |

---

## Sources

Every URL fetched or returned with content for this document.

**Perception and attention**
- <https://www.cs.princeton.edu/courses/archive/spring08/cos598B/Readings/TreismanGelade1980.pdf> — Treisman & Gelade (1980), feature-integration theory; feature-search slope 3.1 ms/item, conjunction slope ~60 ms/item.
- <https://en.wikipedia.org/wiki/Feature_integration_theory> — secondary summary of feature vs conjunction search.
- <http://www.sfu.ca/siatclass/IAT814/Spring2009/Lectures/IAT814-week5A.pdf> — pre-attentive categories (form/colour/motion/position), pop-out as a gamut not a threshold.

**NN/g — hierarchy, grouping, scanning, attention**
- <https://www.nngroup.com/articles/visual-hierarchy-ux-definition/> — definition; ≤3 sizes; ≤2 big elements; ≤3 contrast variations; 2+2 colours; squint test at 5/10/20 px blur.
- <https://www.nngroup.com/articles/principles-visual-design/> — 2–3 typeface sizes; the five visual-design principles.
- <https://www.nngroup.com/articles/dashboards-preattentive/> — pre-attentive attributes for quantity vs category.
- <https://www.nngroup.com/articles/text-scanning-patterns-eyetracking/> — four scanning patterns.
- <https://www.nngroup.com/articles/f-shaped-pattern-reading-web-content/> — F as the default with no strong cues.
- <https://www.nngroup.com/articles/layer-cake-pattern-scanning/> — layer-cake efficiency table.
- <https://www.nngroup.com/articles/zigzag-page-layout/> — alternating layouts cause residual fixations.
- <https://www.nngroup.com/articles/gazeplots-zigzag/> — gaze plots behind the above.
- <https://www.nngroup.com/articles/gestalt-proximity/> — proximity overpowers similarity.
- <https://www.nngroup.com/articles/gestalt-similarity/> — similarity is the most resilient grouping cue.
- <https://www.nngroup.com/articles/common-region/> — boundaries are strong grouping cues and cost clutter.
- <https://www.nngroup.com/articles/closeness-of-actions-and-objects-gui/> — controls far from their object are missed.
- <https://www.nngroup.com/articles/banner-blindness-original-eyetracking/> — no fixations in ad-like regions; right-rail avoidance.
- <https://www.nngroup.com/articles/banner-blindness-old-and-new-findings/> — banner blindness 2020 revisit.
- <https://www.nngroup.com/articles/fight-right-rail-blindness/> — *"anything that's overly large or colorful risks being ignored."*
- <https://www.nngroup.com/articles/change-blindness-definition/> — definition and mitigations.
- <https://www.nngroup.com/articles/change-blindness/> — emphasis and placement reduce change blindness.
- <https://www.nngroup.com/articles/animation-usability/> — animation and the attention currency.
- <https://www.nngroup.com/articles/fitts-law/> — distance/size trade-off; screen edges do not help touch.
- <https://www.nngroup.com/articles/touch-target-size/> — 1 cm minimum; bigger when moving.
- <https://www.nngroup.com/articles/glanceable-fonts/> — uppercase faster for isolated words, not for prose.

**Material 3 / Android**
- <https://m3.material.io/styles/typography/type-scale-tokens> — 15+15 styles; *"avoiding small differences"*; reduced set of ~5; *"Major Second"*.
- <https://m3.material.io/styles/typography/overview> — five roles; 30 styles.
- <https://developer.android.com/develop/ui/compose/designsystems/material3> — M3 emphasis via colour pairs and font weight; alpha for disabled.
- <https://developer.android.com/reference/kotlin/androidx/compose/material3/Typography> — the type-scale reference and emphasized styles.
- <https://github.com/material-components/material-components-android/blob/master/docs/theming/Typography.md> — baseline `sp` sizes and weights.
- <https://m3.material.io/styles/color/roles> — surface container emphasis ladder; primary container as standout fill.
- <https://m3.material.io/blog/tone-based-surface-color-m3> — surface roles no longer tied to elevation.
- <https://m3.material.io/styles/spacing/tokens> · <https://m3.material.io/styles/spacing> · <https://m3.material.io/styles/spacing/applying-spacing> — space100 = 8 dp; padding/gap/margin; multiplier pattern.
- <https://developer.android.com/design/ui/mobile/guides/layout-and-content/grids-and-units> — 8 dp grid for layout, 4 dp for small elements.
- <https://m3.material.io/components/floating-action-button/accessibility> — FAB bottom trailing edge on compact.
- <https://m3.material.io/components/extended-fab/guidelines> — extended FAB at the bottom; not the upper half.
- <https://developer.android.com/reference/kotlin/androidx/compose/material3/FabPosition> — the four bottom positions.

**Apple**
- <https://developer.apple.com/design/human-interface-guidelines/layout> — importance near top/leading; alignment aids scanning.
- <https://developer.apple.com/design/human-interface-guidelines/components/menus-and-actions/buttons> — filled button for the most likely action; one or two prominent per view.
- <https://github.com/Prisma-Labs-Dev/apple-skills/blob/HEAD/skills/hig/color.md> — colour to the background of the primary control only; primary/secondary/tertiary background hierarchy.

**Accessibility**
- <https://www.w3.org/TR/2023/REC-WCAG22-20231005/> — SC 1.4.1, 1.4.3, 1.4.6, 1.4.11.
- <https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html> — 4.5:1 / 3:1 and the 3:1 ISO floor.
- <https://www.w3.org/WAI/WCAG21/Understanding/non-text-contrast.html> — 3:1 for components and states.

**Reach and ergonomics**
- <https://www.uxmatters.com/mt/archives/2013/02/how-do-users-really-hold-mobile-devices.php> — Hoober 2013: 49/36/15.
- <https://www.uxmatters.com/mt/archives/2017/05/design-for-fingers-touch-and-people-part-2.php> — 75 % one-thumb touch vs < 50 % one-hand hold.
- <https://www.uxmatters.com/mt/archives/2017/03/design-for-fingers-touch-and-people-part-1.php> — grip variability; thumb anatomy.
- <https://realites-paralleles.com/2013/12/en-mobile-interactions/> — independent replication of the hold/interact ratios.
- <https://exa.ai/library/publication/z4kjxn0px8w> — one-handed reach degrades beyond 45 mm.
- <https://dl.acm.org/doi/10.1145/2957265.2961835> — comfort-zone calibration; hard-to-reach targets.

**Type scale, line length, spacing**
- <https://quirkulator.com/calculator/modular-type-scale-calculator> — ratio table with px values.
- <https://www.css-architecture.com/design-system-token-fundamentals-naming-conventions/typography-scale-systems/building-a-modular-type-scale-with-custom-properties/> — Major Third vs Perfect Fourth for UI.
- <https://web-font-optimization.com/typography-fundamentals-system-architecture/type-scale-modular-grids/choosing-a-modular-scale-ratio/> — ratio character table; compounding divergence.
- <https://www.typographymaster.com/guide/type-scale-systems> — ratio → use-case mapping.
- <https://www.modularscale.com/> — modular scale construction and multi-strand caution.
- <https://stu.westga.edu/~ssynan1/literacy/Dyson.pdf> — Dyson, line length on screen; 52 cpl print optimum; 75–100 cpl read faster.
- <https://www.sciencedirect.com/science/article/abs/pii/S1071581901904586> — 55 cpl supports comprehension at normal and fast speeds.
- <https://pimpmytype.com/line-length-line-height/> — 60–80 cpl / 1.5, and the explicit *"not backed by research"*.
- <https://www.uxpin.com/studio/blog/optimal-line-length-for-readability/> — 50–75 cpl; 1.5 line height baseline.
- <https://deplaybook.com/pages/strategies/law-of-proximity> — **convention** 1:3–1:4 proximity ratio.
- <https://github.com/oborchers/fractional-cto/blob/main/visual-design-principles/skills/whitespace-density/SKILL.md> — **convention** internal ≤ 50 % of external.
- <https://www.humanstandards.org/code-design-tokens/spatial-rhythm-layout/> — five ordered spacing relationships.
- <https://fluent2.microsoft.design/layout> — spacing groups without dividers; space as emphasis.
- <https://en.wikipedia.org/wiki/All_caps> — Paterson/Tinker ~14 % lowercase advantage; all-caps costs a third more space.
- <https://www.uv.es/mperea/fournet_et_al> — letter-case effects are small for skilled readers; locus in early orthographic processing.

**Audit methods**
- <https://www.lukew.com/ff/entry.asp?2013=> — the squint test.
- <https://polypane.app/blog/debug-your-visual-hierarchy-with-the-squint-test/> — squint test questions; Placeholdifier.
- <https://www.blurtest.com/blog/usability-testing-checklist-what-to-test-before-your-website-goes-live> — blur test checklist.
- <https://dev.to/137foundry/how-to-audit-visual-hierarchy-in-an-existing-web-design-50aj> — greyscale ladder; primary-action weight test.
- <https://mm.dreamineering.com/docs/products/product-design/visual-hierarchy-checklist> — emphasis counting; symptom→cause table; 4/8/16/24/32/48/64 scale.

**Factory internals (read, not restated)**
- `apps/2026-10-01-lawn-round-log/app/src/main/java/com/gexetr/lawnroundlog/ui/round/RoundScreen.kt` — the grounded example.
- `apps/2026-10-01-lawn-round-log/app/src/main/java/com/gexetr/lawnroundlog/ui/theme/Type.kt` — the slot's type ramp.
- `apps/2026-10-01-lawn-round-log/app/src/main/java/com/gexetr/lawnroundlog/ui/components/Brand.kt` — `StatTile`, `SectionStrip`, `VisitStatusPill`.
- `apps/2026-10-01-lawn-round-log/store/studio.json` — the declared palette.
- `scripts/core/uiux.mjs` — the existing sensor and the boundary in §10.
- `docs/ui-ux-standards.md` §"What this standard does not check" — the declared gap this document fills.
- `docs/research/2026-10-02-android-window-insets.md` §1, §5(c) — the pinned-bar/inset mechanism, cited not restated.
