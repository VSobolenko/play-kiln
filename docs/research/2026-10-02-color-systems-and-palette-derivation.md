# Colour systems and palette derivation

**Status:** research note, written 2026-10-02. Not a gate. Not a rule. It is the evidence base a gate
could be built from, and the Encodable rules section is the only part written to be turned into one.

**Why this document exists.** Every PlayKiln slot ships a palette, declared twice and kept identical:
`store/studio.json` (`palette`) and the app's own `app/src/main/java/<pkg>/ui/theme/Color.kt`. The
existing gates check three things about it — that the two copies agree, that the pack's pixels match
the app's Kotlin palette (`scripts/visual-qa.py`, section `palette`), and that text/background pairs
reach their contrast floors. **Nothing checks that the palette was derived rather than picked.**
`.cursor/rules/visual-identity.mdc` says the identity must be "a build input, not a thing that gets
decorated on at the end", but declares no method for producing one, so a slot is free to answer
"where does this palette come from?" with a metaphor.

That is the exact defect this document is written from, and it was measured, not supposed.

**Evidence labels.** Every claim below carries one of the factory's four labels, per
`.cursor/rules/ideation-gate.mdc`:

| Label | Means |
|---|---|
| `MEASURED` | the number was read at its source, or computed by this document's own scripts over a real artifact |
| `ON RECORD` | a citable source states it, but it was not independently reproduced here |
| `INFERRED` | this document's reasoning over two or more of the above; the reasoning is shown |
| `UNVERIFIED` | stated for completeness, with no source found |

**Reproduction.** The numeric tables in §0.3, §2.4 and §5.5 come from three scripts written for this
document, all of which read only and write nothing:

- `.kiln/scratch/lawn-full-palette.mjs` — HCT, OKLCH and WCAG contrast for every role of the
  motivating slot, plus the Material 3 derivation from the same seed.
- `.kiln/scratch/oklch-headroom.mjs` — OKLCH gamut headroom per role, by bisection over the
  OKLab→sRGB inverse, with pure red/green/magenta as the 100 %-self-check.
- `.kiln/scratch/lawn-pixel-share.py` — per-pixel OKLCH and nearest-role classification over real
  1080×2340 device screenshots under `.kiln/scratch/device-lawn-round-log/`.
- `.kiln/scratch/palette-research2.mjs` — gamut ceilings, the M3 tone-contrast table, and the neutral
  chroma Material 3 produces for the same seed.

They run against `@material/material-color-utilities` (the reference implementation of Material 3's
colour science, installed under `.kiln/scratch/mcu/`, version **0.4.0**), so the "M3 would have done X"
claims are computed by Google's own algorithm rather than quoted from a blog.

**Re-running them needs one flag, and this is written down because omitting it fails misleadingly.**
The published package is built for bundlers and uses extensionless relative imports
(`scheme/scheme_content.js` imports `dynamiccolor/dynamic_scheme`), which Node's ESM resolver refuses.
`.kiln/scratch/hook-register.mjs` installs a resolve hook (`.kiln/scratch/resolve-hook.mjs`) that
retries a failed relative specifier with `.js` appended, and every script must be run through it:

```bash
cd .kiln/scratch
node --import ./hook-register.mjs lawn-full-palette.mjs # without --import: ERR_MODULE_NOT_FOUND
node --import ./hook-register.mjs oklch-headroom.mjs # pure red/green must print exactly 100.0%
```

Both scripts are read-only and re-derivable from a clean `.kiln/scratch/mcu/`; the numbers below were
produced by them and were re-run on 2026-10-02 to confirm the document's `MEASURED` values reproduce
(the accent/M3-primaryContainer ratio of 1.88 in §2.3 and rule 15, and the 100.0 % self-check for the
pure primaries in §2.1). A builder writing the gate should re-derive rather than copy: the whole point
of rule 19 is that the derivation is declared, and the arithmetic in this document is only evidence
that the derivation was computed, not a substitute for computing it again.

---

## 0. The motivating defect, stated in numbers

### 0.1 What the slot says the palette is

`apps/2026-10-01-lawn-round-log/app/src/main/java/com/gexetr/lawnroundlog/ui/theme/Color.kt`:

> *"The ground is a cut lawn: a deep turf green field, chalk-off-white ink, and exactly one lime
> reserved for work that has just been done."*
>
> *"`amber` — the cut accent: the colour of a stripe that has just been mown."*

`store/studio.json` repeats the same justification in `_palette`. The accent is `#C9F04A`. The
justification is a **narrative about a lawn**, not a statement about hue, chroma, tone, area or
contrast. Nothing in the pipeline could disagree with it, and the owner's reaction on seeing it was
that a green app with this accent reads as unaesthetic.

This is the failure class in one sentence: **a palette justified by a story rather than by a method.**

### 0.2 Why the existing gates cannot see it

`MEASURED`, by running the slot's own contrast pairs:

| Pair the slot actually uses | WCAG 2.x ratio | Verdict |
|---|---:|---|
| `paper` #F6F4E4 on `ink` #12301C (body text) | **12.95:1** | passes 1.4.3 (4.5) and 1.4.6 (7) |
| `muted` #BBD0B2 on `ink` (secondary text) | **8.72:1** | passes |
| `amber` #C9F04A on `ink` (accent text) | **10.95:1** | passes |
| `ink` on `amber` (accent ink) | **10.95:1** | passes |
| `bezel_rim` #7C9A82 on `bezel` #0A1A10 | **5.82:1** | passes |
| `paper` on `accent_deep` #1E6E6A | **5.43:1** | passes |
| `muted` on `ink_lift` #275B33 | **4.86:1** | passes, 0.36 margin |
| `amber_deep` #6E9A22 on `paper` | **3.01:1** | **fails 4.5:1 for body text**; only the 3:1 large-text floor |
| `amber` on `paper` | **1.18:1** | invisible by luminance; separated by chroma alone |
| `paper` vs `amber_pale` #EAF9C6 | **1.01:1** | two declared roles, one luminance |

The palette's **contrast story is strong** — deliberately, and the code comment says so ("the
contrast is deliberately far above the Material baseline"). A gate keyed only to contrast returns a
clean sheet for a palette whose structure is the thing that failed. The two rows that *do* fail
(`amber_deep` on `paper` at 3.01:1; `amber` on `paper` at 1.18:1) are real and worth a gate of their
own, but they are a side effect, not the defect. **The defect is in the hue/chroma geometry, and
nothing currently measures hue or chroma at all.**

### 0.3 The geometry, measured

`MEASURED` — HCT is (hue, chroma, tone) after Material 3; OKLCH after Ottosson. Both were computed
from the declared hex values, not read from the comments.

| Role | Hex | HCT H | HCT C | HCT T | OKLCH L | OKLCH C | OKLCH gamut headroom |
|---|---|---:|---:|---:|---:|---:|---:|
| `ink` (field, dark end) | `#12301C` | 155.3 | 22.8 | 17.1 | 0.280 | 0.052 | 71.1 % |
| `ink_soft` | `#1B4126` | 153.7 | 27.6 | 24.2 | 0.339 | 0.064 | 70.6 % |
| `ink_lift` (field, light end) | `#275B33` | 151.5 | 35.5 | 34.3 | 0.424 | 0.086 | 72.1 % |
| `amber` (**the accent**) | `#C9F04A` | 123.4 | **68.0** | 89.5 | 0.898 | 0.190 | **87.5 %** |
| `amber_deep` | `#6E9A22` | 129.4 | 58.8 | 58.5 | 0.632 | 0.152 | **91.5 %** |
| `amber_pale` | `#EAF9C6` | 127.1 | 23.7 | 95.7 | 0.958 | 0.068 | 52.5 % |
| `accent_deep` (the teal) | `#1E6E6A` | 192.0 | 32.7 | 42.0 | 0.491 | 0.075 | **88.4 %** |
| `paper` | `#F6F4E4` | 115.9 | 7.2 | 96.0 | 0.964 | 0.021 | 25.5 % |
| `muted` | `#BBD0B2` | 143.6 | 18.7 | 81.2 | 0.834 | 0.047 | 18.8 % |
| `bezel` | `#0A1A10` | 158.4 | 14.1 | 7.6 | 0.200 | 0.030 | 60.5 % |
| `bezel_rim` | `#7C9A82` | 156.3 | 21.0 | 60.8 | 0.656 | 0.049 | 27.5 % |

*OKLCH gamut headroom* = the role's OKLCH chroma divided by the largest OKLCH chroma that stays inside
sRGB at the role's own lightness and hue, computed by bisection over the OKLab→sRGB inverse (`MEASURED`,
`.kiln/scratch/oklch-headroom.mjs`; pure red, green and magenta come back at exactly 100.0 %, which is
the self-check). §2.1 explains why this metric is a **report** and not a rule.

And the supporting tones declared below the fold in the same file — `TurfWash` 143.2 / 14.5 / 91.4,
`ChalkDim` 122.8 / 8.7 / 91.5, `ChalkLine` 151.1 / 21.6 / 55.1, `SlateWash` 186.7 / 14.3 / 89.8,
`SlateDeep` 192.6 / 22.7 / 21.5, `SlateLight` 189.9 / 35.9 / 79.3, `OliveDeep` 130.2 / **45.7** / 36.2,
`TurfSurface` 152.8 / 25.9 / 20.4, `TurfSurfaceLift` 151.4 / 31.7 / 28.1, `TurfLine` 153.6 / 26.5 / 45.0,
`Mist` 139.9 / 17.0 / 91.5, `ClayMist` 35.3 / 10.3 / 88.8, `EmberInk` 30.7 / 37.5 / 11.1,
`Copper` 28.8 / 60.8 / 37.8, `CopperLight` 30.6 / 29.6 / 79.9.

Seven facts fall out of that table.

1. **The accent is the same hue as Material 3's own primary would be.** The seed's hue is 123.4°;
   Material 3's `primary` for that seed is H123.0 (`MEASURED`, §0.5). The accent is not a *different*
   colour from the field's family — literally the same hue Material 3 calls "primary".
2. **The accent's hue is one analogous step from the field.** Δhue(`amber`, `ink`) = **31.9°**;
   Δhue(`amber`, `ink_lift`) = **28.1°**. Material 3's own `TemperatureCache.analogous()` divides the
   wheel into **12 sections** — 30° per step — and calls that set "analogous" (`MEASURED`, quoted in
   §3.3). The accent sits inside one analogous step of the field it is supposed to stand out from.
3. **The field is chromatic, not neutral.** A background at HCT chroma 22.8–35.5 is 2.7–4.3× the
   chroma Material 3 gives its own neutrals (C5.6–8.3, `MEASURED`, §5.3) — **for this very seed**.
   Because the 60 % of the screen is already saturated, saturation has no headroom left to mean
   anything. Only *lightness* and *hue* can carry the accent, and fact 2 removes hue.
4. **Many roles sit near the sRGB boundary — but that alone is not the defect.** Eight of the
   twenty-six declared colours are at ≥ 75 % of the OKLCH gamut ceiling for their own lightness and
   hue: `amber` **87.5 %**, `amber_deep` **91.5 %**, `accent_deep` **88.4 %**, `SlateDeep` 88.9 %,
   `OliveDeep` **94.2 %**, `EmberInk` 90.7 %, `Copper` 80.0 %, `CopperLight` **100.0 %**. The three
   field roles sit just under at 70.6–72.1 %. Material 3's default primary from a standard seed sits
   at **34.3 %**, under half — but Material 3's own `primaryContainer` `#e9ddff` sits at **100.0 %**,
   because the algorithm *clamps* it there. Headroom is therefore a report and not a rule (§2.1, §9):
   two legitimate systems occupy both ends of it.
5. **Material 3 would have allowed roughly half this accent's chroma at the same tone.** For this
   seed, Material 3's `primaryContainer` is `#d8eb9a`, H123.2 **C36.1 T90.0**. The slot's accent is
   H123.4 **C68.0 T89.5** — the same hue, the same tone, **1.88× the chroma** (`MEASURED`, §0.5).
6. **The palette is one analogous sweep, not a scheme.** Merging chromatic roles (C ≥ 12) at a 30°
   threshold leaves exactly **2 families**: the error family at 28.8°–30.7° (3 roles, 1.9° spread)
   and **one family spanning 123.4°–192.6° containing 20 roles** (`MEASURED`). Hue is used as a
   gradient, not as structure.
7. **Two of the eleven declared roles are the same colour.** `paper` `#F6F4E4` and `amber_pale`
   `#EAF9C6` are **1.01:1** apart in contrast, 11.2° apart in hue, 16.5 apart in chroma and 0.3 apart
   in tone (`MEASURED`). A consumer of the palette cannot tell which one the designer meant.

### 0.4 Why *lime-on-green specifically* fails

The user-visible adjectives are "unpleasant", "unaesthetic", "reads as a warning colour". Here is the
mechanism, in the measured quantities.

The failure is **not** legibility, and it is **not** vibration. The accent's contrast against the field
is 10.95:1 and its tone gap is 72.4 — nothing flickers and nothing is unreadable. Honesty on this
point matters, because a gate built on the vibration story would pass this palette.

The failure is **hierarchy under a saturated field**. A UI separates an element from its background by
exactly four levers: **hue difference**, **chroma difference**, **lightness difference**, and **area**.
This palette spends all of its budget on lightness and none on the other two:

- **Hue**: 31.9° from the field — inside Material 3's own analogous step. The accent is *the field's
  hue*, turned toward yellow.
- **Chroma**: 68.0 against the field's 22.8–35.5. That is a ratio of 1.9–3.0, but the field is *not*
  neutral, so the eye reads "more of the same green" rather than "a different substance". Material 3
  answers the same problem structurally: it puts the primary at C36 on a C6 neutral (§5.3), a ratio
  of 6.0, and rotates the tertiary to +60° for the third family (§3.3).
- **Lightness**: 10.95:1. This is the only lever fully spent, and it is why the app is legible and
  still disliked.

Then the specific yellow-green problem compounds it:

- **The accent and the light paper are the same hue neighbourhood.** `paper` H115.9 C7.2 T96.0 and
  `amber` H123.4 C68.0 T89.5 differ by **7.5° hue and 6.5 tone**, at a chroma ratio of 9.4. Against
  `paper` the accent measures **1.18:1** — invisible by luminance, held apart by chroma alone. In the
  light scheme, or in any store composition where the accent is drawn on the chalk surface, the
  accent does not exist as a shape; it exists as a colour.
- **A high-chroma yellow-green is the hue band the human visual system is most sensitive to.** Material
  3 encodes this as a named exclusion in its own algorithm (§2.4): hues 90–111 at chroma > 16 below
  tone 65 are treated as *universally disliked* and forcibly lightened. The lime escapes that band by
  12° of hue — narrowly. That is precisely why a rule built only on Material 3's dislike guard would
  not catch this palette, and why the rules in the Encodable rules section are relational rather than
  absolute.

**One sentence for the record:** `#C9F04A` on `#12301C` fails because it is the same hue as its field,
one analogous step away, at 87.5 % of its OKLCH gamut ceiling while the field sits at 71–72 %, in a
palette whose entire chromatic structure is a single 69° analogous sweep — so "special" has to be
carried by lightness alone, and lightness is the one thing the user was not asked to look at.

### 0.5 What Material 3 does with the identical seed

This is the single most concrete piece of evidence in the document, because it is **the method being
available and not being used**. Feeding HCT (123.4, 68.0, 89.5) — the accent itself — into Google's
own `SchemeTonalSpot`, `MEASURED`:

| Material 3 role | Derived hex | HCT H | HCT C | HCT T |
|---|---|---:|---:|---:|
| `primary` | `#556423` | 123.0 | **35.8** | 39.9 |
| `secondary` | `#5b6146` | 123.3 | **16.1** | 39.9 |
| `tertiary` | `#3a665e` | **183.6** | 23.8 | 39.9 |
| `error` | `#ba1a1a` | 25.1 | 84.3 | 40.0 |
| `surface` | `#fbfaed` | 121.8 | **6.1** | 98.0 |
| `onSurface` | `#1b1c15` | 120.9 | 5.6 | 9.9 |
| `primaryContainer` | `#d8eb9a` | 123.2 | 36.1 | 90.0 |
| `onPrimaryContainer` | `#3e4c0c` | 123.0 | 35.9 | 30.0 |

Read the `tertiary` row. Material 3's third family for this seed is **H183.6 C23.8** — a teal. The
slot's own `accent_deep` is **H192.0 C32.7**. The slot derived the +60° hue, declared it, and then
used a super-saturated primary-hue lime as the accent instead. The method was present in the palette
and was overridden by the metaphor.

---

## 1. Colour spaces for UI palette work

### 1.1 HSL and HSV are not design spaces

`ON RECORD` — CSS Color 4, §7:

> "The hue angle in HSL is not perceptually uniform; colors appear bunched up in some areas and widely
> spaced in others."
> — <https://www.w3.org/TR/css-color-4/>

The consequence is not subtle. In HSL, `hsl(60 100% 50%)` (yellow) and `hsl(240 100% 50%)` (blue)
both claim **L = 50 %**. Their WCAG relative luminances are **0.9278** and **0.0722** — a factor of
**12.85** — and their contrast ratio against each other is **8.00:1**, between two colours the space
calls equally light. An agent that reasons "L = 50 % is a mid colour, so it must have enough contrast
against both white and black" is reasoning about a number that does not mean what it says.

`MEASURED` on the six fully saturated HSL primaries, the failure is broader than luminance: all six
claim `S = 100 %` and `L = 50 %` (or the hue's equivalent), and their HCT chroma and tone are:

| HSL | Hex | HCT C | HCT T |
|---|---|---:|---:|
| `hsl(0 100% 50%)` red | `#FF0000` | 113.4 | 53.2 |
| `hsl(60 100% 50%)` yellow | `#FFFF00` | 75.5 | 97.1 |
| `hsl(120 100% 50%)` green | `#00FF00` | **108.4** | 87.7 |
| `hsl(240 100% 50%)` blue | `#0000FF` | 87.2 | 32.3 |

A **1.44× spread in chroma** and a **64.8-point spread in tone** between colours that HSL calls equally
saturated and equally light. HSL's S is a position on a piecewise-linear RGB line, not a quantity.

CSS Color 4 is explicit that the sRGB space underlying HSL "is neither linear-light nor perceptually
uniform", and that only Oklab (and to a lesser extent Lab) is designed for even perceptual spacing
(`ON RECORD`, §9.1, §13.2).

**Conclusion for the factory:** HSL/HSV is acceptable as an *authoring* notation and unacceptable as a
*measurement* space. No rule in the Encodable rules section may be stated in HSL/HSV terms.

### 1.2 HCT — Material 3's working space

HCT is Google's composite, and the point of it is that it recombines three quantities that no single
space gives you together.

- **Hue and Chroma are CAM16** — a colour *appearance* model, meaning it models the colour as a
  human sees it under an assumed viewing condition (adaptation white, surround, luminance), not as
  the display emits it. `ON RECORD` (material-color-utilities, `Hct.java`).
- **Tone is CIE L\***, the perceptual lightness from CIELAB, normalised so that **Tone 0 = black and
  Tone 100 = white**, with a linear relationship to WCAG relative luminance. `MEASURED`: the
  implementation's own `ColorUtils.luminanceToTone` / `toneToLuminance` are the exact WCAG
  `Y` transfer pair (<https://github.com/material-foundation/material-color-utilities/blob/main/java/utils/ColorUtils.java>).

The practical consequence of Tone being L\* is the property the whole of Material 3's contrast
guarantee rests on: **contrast between two colours is (almost) a function of their tones alone.**
`MEASURED`, from `Contrast.ratioOfTones`:

| Tone pair | Ratio |
|---|---:|
| 10 vs 98 | 16.31:1 |
| 10 vs 90 | 13.27:1 |
| 30 vs 90 | 7.24:1 |
| 40 vs 100 | 6.46:1 |
| 40 vs 90 | 5.00:1 |
| 60 vs 6 | 5.85:1 |
| 50 vs 98 | 4.27:1 |

That is why Material 3 can promise a contrast ratio *before* knowing which hue the user's wallpaper
produced: pick a tone pair, get a ratio. `ON RECORD`, Android Developers, *Material 3 in Compose*:
each key colour "relates to a tonal palette of **13 tones**", and the generated schemes guarantee
contrast (<https://developer.android.com/develop/ui/compose/designsystems/material3>).

**Chroma in HCT is bounded by the gamut, and that is a feature.** `TonalPalette.of(hue, chroma)`
constructs a palette that is *constant in hue and chroma and varies only in tone* — but a requested
chroma that is unreachable at some tone is clamped down to the boundary at that tone. `MEASURED` in
the derivation of §1.4: Material 3's primary for the lawn seed is H299-ish C36 at tone 40, but its
`primaryContainer` at tone 90 is C20.2, because at hue 299 / tone 90 the sRGB ceiling is 20.7. A
palette therefore stores an *intent* (hue, chroma) and realizes a *different chroma per tone*. Any
gate that reads chroma must read it **per tone**, never as one number for a family.

### 1.3 Oklab / OKLCH

`ON RECORD` — Ottosson, *A perceptual color space for image processing* (2020),
<https://bottosson.github.io/posts/oklab/>. Oklab is built by taking a cone-response matrix (a
modified LMS, the `M1` matrix), applying a **cube-root** non-linearity, then a second matrix (`M2`)
into (L, a, b). Reproduced in the document's own scripts as:

```
M1 = [0.4122214708  0.5363325363  0.0514459929
      0.2119034982  0.6806995451  0.1073969566
      0.0883024619  0.2817188376  0.6299787005]      # linear sRGB -> l, m, s
l', m', s' = cbrt(l), cbrt(m), cbrt(s)
M2 = [0.2104542553  0.7936177850 -0.0040720468
      1.9779984951 -2.4285922050  0.4505937099      # -> L, a, b
      0.0259040371  0.7827717662 -0.8086757660]
```

Its two selling points, both directly relevant here:

- **Hue linearity.** The cube root is the mechanism: it approximates the compressive response of the
  visual system, so a rotation in Oklab's `a`/`b` plane changes perceived hue roughly uniformly, and
  a change in L changes perceived lightness roughly uniformly. That is what HSL lacks and what makes
  OKLCH hue angles comparable across the wheel.
- **It is a metric space.** `ON RECORD` — CSS Color 4, §9.2: "Because Oklab is more perceptually
  uniform than CIE Lab, the color difference is a straightforward distance in 3D space (root sum of
  squares)." The document's pixel script uses exactly that: `ΔE_OK = sqrt(ΔL² + Δa² + Δb²)`.

**OKLCH hue angles are not HSL hue angles.** `ON RECORD` — CSS Color 4, §9.3: in OKLCH/LCH "0deg
points along the positive 'a' axis (toward purplish red) … 90deg points along the positive 'b' axis
(toward mustard yellow), 180deg points along the negative 'a' axis (toward greenish cyan), and 270deg
points along the negative 'b' axis (toward sky blue)"; and the spec notes that the same physical green
is 120° in HSL, **134.39° in LCH**, and a different angle again in each wide-gamut space
(`ON RECORD`, §4.3). `MEASURED` for the motivating slot: `amber` is **HCT 123.4°** and **OKLCH
121.9°**; `accent_deep` is **HCT 192.0°** and **OKLCH 190.1°**. The two spaces agree to within ~2°
for these colours, but they are not the same coordinate system and a gate must fix one.

**OKLCH's chroma scale.** `ON RECORD` — MDN, `<color>` / `oklch()`: C is "a `<number>` … 0 is … a
fully desaturated grey, 0.4 represents a highly saturated colour", and CSS Color 4 speaks of gamut
mapping by **"holding the hue constant, and reducing the chroma until the color falls in gamut"**
(MINDE, §14). `MEASURED` for the lawn palette: `amber` is OKLCH C = **0.190** — half of the 0.4 that
MDN calls "highly saturated", and still 84 % of the sRGB ceiling at its tone. Both statements are
true; the second is the one that matters, because it is the one that knows about the hue.

### 1.4 Why OKLCH is displacing HSL for palettes, in one measured fact

`MEASURED`, fetched from Tailwind's own colour documentation on 2026-10-02 and counted: the page
serves **575 `oklch(...)` values**, the first being `oklch(.13 .028 261.692)`. Tailwind CSS v4
publishes its entire palette — `slate-50` through `black` — in OKLCH, not hex or HSL
(<https://tailwindcss.com/docs/colors>). That is a production design system with one of the largest
installed bases in the world choosing OKLCH as its authoring space, and it is the clearest available
signal that OKLCH is now standard practice rather than an experiment.

### 1.5 Which space should a PlayKiln rule use?

Both HCT and OKLCH are defensible; they answer different questions.

| | HCT (Material 3) | OKLCH |
|---|---|---|
| Hue/chroma model | CAM16 (appearance model, viewing-condition aware) | Oklab (cube-root cone response) |
| Lightness | CIE L\*, tied to WCAG luminance by construction | Oklab L, perceptually uniform but **not** a WCAG function |
| Contrast arithmetic | `ratioOfTones(a, b)` answers directly | must convert L → sRGB → WCAG `Y` |
| Reference implementation | `@material/material-color-utilities` (Java/Kotlin/JS/Python) | none officially; ~20 lines of matrices |
| Where it is used | Material 3 dynamic colour, all M3-aware Android apps | CSS Color 4, Tailwind v4 |

**`INFERRED` recommendation for the factory:** state the rules in **HCT**, for two reasons that are
both practical rather than aesthetic. First, HCT's Tone *is* the WCAG luminance axis, so a single
space carries both the geometry rules and the contrast rules and no conversion bug can appear between
them. Second, the reference implementation is the one the Android app itself already sits next to, so
a gate and a Kotlin theme can be argued about in the same units. OKLCH is the right space for
*interpolation* (gradients, generated tints) and should be permitted there — CSS Color 4 §13.2 says
Oklab is the space for even perceptual spacing, which is what a gradient needs
(`ON RECORD`).

When a rule is stated in degrees of hue, it must say **which** degrees. The Encodable rules section
does: HCT.

---

## 2. Chroma discipline

### 2.1 What chroma does in a UI

Chroma is a *hierarchy* channel, and it is a **scarcer** one than lightness on a fixed-gamut display.
A colour can reach tone 100 or tone 0 anywhere; it can reach high chroma only in a band of hue/tone
space, and the ceiling collapses at both ends of the range. `MEASURED` — the maximum in-gamut OKLCH
chroma at the lime's own hue (121.9°), by lightness, found by bisection over the OKLab→sRGB inverse
(the CSS Color 4 §14 MINDE definition: hold hue and lightness, reduce chroma until in gamut):

| OKLCH L | 0.20 | 0.30 | 0.40 | 0.50 | 0.60 | 0.70 | 0.80 | 0.90 | 0.95 | 0.98 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| max C | 0.049 | 0.073 | 0.097 | 0.121 | 0.145 | 0.170 | 0.194 | **0.218** | 0.156 | 0.054 |

The ceiling peaks near L 0.90 and then **collapses**: a lime at L 0.98 can reach only a quarter of the
chroma available at L 0.90. This matters because the common instinct, "make the accent brighter *and*
more saturated", runs straight into that wall — it is exactly how `CopperLight` `#FFB4A4` ended up at
**100.0 %** of its own ceiling while `amber_pale` `#EAF9C6`, two hundredths of lightness away, sits at
52.5 % (`MEASURED`).

**A methodological warning a gate must not repeat.** The obvious way to measure "how much of the gamut
does this colour use" is Material 3's own `Hct.from(hue, 200, tone).chroma` — ask HCT for an impossible
chroma and read what survives. **That proxy is not trustworthy near the boundary**, and the self-check
shows why: it reports pure `#00FF00` at **189.2 %** of its own reported ceiling, which is nonsense, and
it reports the lawn field at 75–76 % where the OKLCH computation gives 70.6–72.1 %. The cause is that
HCT (hue, chroma, tone) is not a bijection onto sRGB near the gamut surface, so `Hct.from(hue, 200,
tone)` and `Hct.fromInt(hex)` disagree there. Every headroom figure in this document is therefore
computed in **OKLCH**, and the sanity check is that pure red, pure green and pure magenta all return
exactly **100.0 %** (`MEASURED`).

**And the headroom number is a report, not a rule.** `MEASURED` the counter-examples: Material 3's own
light `primaryContainer` `#e9ddff` is at **100.0 %** — the algorithm *clamps* it to the boundary on
purpose — and Tailwind's `lime-500` `#84cc16` sits at **97.6 %** as a deliberate palette choice. A rule
that failed either of those would be failing a real design system. The lawn palette's own headroom is
therefore not what makes it wrong; §0.4 and §2.3 name what is.

### 2.2 The accent is a small-area element, and the area is the mechanism

The accent's job is to be *the one thing that is different*. That works on a small area and fails on a
large one for a structural reason: the visual system adapts its own chroma sensitivity to the
prevailing field, so an accent that occupies a tenth of the screen stays a signal, while the same
colour at half the screen becomes the field and stops signalling. `ON RECORD`, Nielsen Norman Group's
guidance on colour in UI: chroma should be spent on the element you want noticed, and the widely
taught distribution is **60 % dominant, 30 % secondary, 10 % accent**
(<https://www.nngroup.com/articles/color-enhance-design/>). NN/g states the proportions as a working
rule of thumb; it cites no measurement, and §9 says so plainly.

`MEASURED` — the accent's real area share in `apps/2026-10-01-lawn-round-log`, measured by
classifying every pixel of real 1080×2340 device screenshots by nearest declared role (ΔE_OK) and by
an OKLCH chroma threshold:

| Screenshot | Pixels ≥ OKLCH C 0.10 | Within ΔE_OK 0.02 of `#C9F04A` | Nearest-role share of `amber` |
|---|---:|---:|---:|
| `round.png` | 5.25 % | 4.82 % | 4.87 % |
| `round-after-fix.png` | 5.93 % | 5.66 % | 5.76 % |
| `01-first-run.png` | 5.45 % | 5.17 % | 5.25 % |
| `05-mowed.png` | 2.70 % | 2.64 % | 2.66 % |
| `bottom.png` (1080×490 crop) | 16.08 % | 14.25 % | 14.41 % |

Two readings. **On a whole screen the accent stays inside the 10 % budget** — so the lawn app does not
fail the area rule, and a gate that only enforced "accent ≤ 10 %" would pass it. **But the accent
occupies 14–16 % of the pixels in a bottom-of-screen band**, which is where a "cut" pill and status
row live, and that is the region a reviewer's eye actually rests on. Area share is therefore worth
measuring *per region*, not only per screen.

The same measurement exposes the palette's real problem better than any single number:
**55–62 % of every screen has OKLCH chroma ≥ 0.06, and 69–75 % has chroma ≥ 0.04.** The "60 %"
dominant colour of this design is not a neutral. In a 60-30-10 reading that looks compliant; in a
hierarchy reading, the 60 % has consumed the chroma budget.

### 2.3 Bounding chroma: what real systems actually do

`MEASURED` — the chroma constants inside Material 3's `ColorSpec2021` (`primaryPalette` and friends),
read from the reference implementation:

| Scheme variant | `primaryPalette` chroma | `secondaryPalette` | `tertiaryPalette` | `neutralPalette` | `neutralVariantPalette` |
|---|---:|---:|---:|---:|---:|
| `TONAL_SPOT` (default) | **36** | 16 | 24 | 6 | 8 |
| `VIBRANT` | 200 | 24 | 32 | 6 | 8 |
| `EXPRESSIVE` | 40 | 24 | 32 | 6 | 8 |
| `CONTENT` | 0 | 0 | 0 | 6 | 8 |
| `NEUTRAL` | 12 | 8 | 16 | 6 | 8 |
| `RAINBOW` | 48 | 16 | 24 | 6 | 8 |
| `FRUIT_SALAD` | 48 | 16 | 24 | 6 | 8 |

Three things to read out of that table.

- **Material 3's default chromatic chroma is 36**, and even its loudest non-degenerate variant is 48.
  The only value above 48 is `VIBRANT`'s 200 — and 200 is not a target, it is a **deliberate request
  for the gamut clamp**, i.e. "take me to the boundary at every tone". A design that intends a
  restrained identity has no reason to ask for it.
- **The neutrals are 6 and 8.** Material 3 does not merely desaturate its surfaces, it holds them at
  a fixed, tiny chroma. `MEASURED` for the lawn light scheme as Material 3 would derive it from the
  lime seed: `surface` C6.1, `surfaceContainer` C5.8, `surfaceContainerHighest` C5.8, `surfaceVariant`
  C8.3, `outline` C7.8, `onSurface` C5.6.
- **IBM Carbon takes the opposite route to the same end.** `ON RECORD`, Carbon's colour overview:
  greys carry the interface, colour is used sparingly and semantically, and the guidance is explicit
  that "the use of color is a deliberate, intentional choice" with colour reserved for
  communication rather than decoration; Carbon's own palettes are grouped per-hue in ten steps and
  application in large areas is limited to the neutral families
  (<https://carbondesignsystem.com/elements/color/overview>). The mechanism differs — Carbon spends
  colour as *semantics* on small elements, M3 spends chroma as *structure* — but both keep the
  dominant surface low-chroma.

`INFERRED` from the two together: the defensible statement is not "a background must be grey", it is
**"the background's chroma must be low enough that the accent can exceed it by a wide margin."**
Material 3's margin for this seed is 36/6 = **6.0×**. The lawn palette's margin is 68/22.8 = **2.98×**
against the darkest field role, and 68/35.5 = **1.91×** against the lightest.

### 2.4 The one chroma rule a vendor actually encodes

Material 3 ships a hard-coded aesthetic judgement, and it is worth quoting because it proves the
category exists even though it does not cover this palette. `MEASURED`,
`@material/material-color-utilities/dislike/dislike_analyzer.js`:

```js
static isDisliked(hct) {
    const huePasses = Math.round(hct.hue) >= 90.0 && Math.round(hct.hue) <= 111.0;
    const chromaPasses = Math.round(hct.chroma) > 16.0;
    const tonePasses = Math.round(hct.tone) < 65.0;
    return huePasses && chromaPasses && tonePasses;
}
// and if disliked: Hct.from(hct.hue, hct.chroma, 70.0)   <- force tone 70
```

**Hue 90–111, chroma > 16, tone < 65 is disliked; Material 3 lightens it to tone 70.** This is the
only place in a major design system where a *specific hue band* is declared aesthetically
unacceptable, and it is the strongest available evidence that dark yellow-greens on saturated fields
are a known aesthetic hazard rather than a matter of taste.

`MEASURED` — the lawn palette does **not** trigger it: the field roles are at H151–158 (outside
90–111) and the accent at H123.4 (also outside). It escapes by 12° of hue. This is the load-bearing
observation for the Encodable rules section: **Material 3's own defence would pass this palette**, so
the rules must describe
the accent's *relationship to its field*, not the accent alone.

---

## 3. Harmony schemes actually used in software

### 3.1 The classical relationships, with numbers

These are dictionary definitions of angle relations on a colour wheel, not perceptual laws, and
they are only meaningful once a perceptually even wheel exists (see §1.3). Stated in degrees, with
the honest caveat that "the wheel" differs between HSL, HCT and OKLCH:

| Scheme | Hue relationship | Notes |
|---|---|---|
| Monochromatic | Δhue = 0, tone/chroma vary | One palette, all roles from it — **this is what Material 3's tonal palettes are** |
| Analogous | adjacent, ≈ 30° per step | Material 3's `TemperatureCache.analogous()` uses exactly 30° (§3.3) |
| Complementary | 180° | Maximum hue separation; both hues fight for equal attention |
| Split-complementary | ±150°–±30° around 180° (i.e. 150° and 210°) | softer than full complementary |
| Triadic | 120° / 240° | three-way balance; hard to keep one dominant |
| Tetradic / square | 90° steps | four-way; usually one colour must be demoted |

The `ON RECORD` justification for the numbers themselves is the colour-wheel definition used in every
design reference; there is no empirical study in this document's sources that establishes 180° as
optimum for UI. §9 flags the class.

### 3.2 Material 3: a monochromatic core with one fixed 60° rotation

Material 3 does not use complementary or triadic harmony. It builds **five monochromatic tonal
palettes from one hue** and then rotates exactly one of them.

`MEASURED`, from `ColorSpec2021` / `SchemeTonalSpot`:

| Palette | Hue | Chroma (default) |
|---|---|---|
| `primaryPalette` | source hue | 36 |
| `secondaryPalette` | source hue | 16 |
| `tertiaryPalette` | **source hue + 60°** | 24 |
| `neutralPalette` | source hue | 6 |
| `neutralVariantPalette` | source hue | (chroma / 8) + 4 |

The whole of Material 3's "harmony" is therefore: **one hue, four chroma levels, and a single +60°
third family.** A palette with no declared offsets at all — the lawn palette's case — has no
structure the algorithm can check, because the algorithm's structure *is* the offsets.

### 3.3 The two derivations Material 3 actually offers

`MEASURED`, from `SchemeTonalSpot.java` and `TemperatureCache.java`:

- **Tonal spot** (the default): the fixed offsets above.
- **From content**: `SchemeContent` extracts a source colour from an image and tunes hue/chroma per
  palette rather than using fixed constants.

And one harmony helper:

```
analogous(count = 5, divisions = 12)   // "a set of 5 colors on a color wheel divided into 12 sections"
complement()                           // approached by *temperature*, not by a fixed +180
```

`MEASURED`, read from the source comment: `MEASURED`, from `temperature_cache.js` —
"In art, this is usually described as a set of 5 colors on a color wheel divided into 12 sections."
**12 sections over 360° is 30° per step**, which is the number used in §0.4 and in rule B1.

Note what `complement()` does *not* do: it does not add 180° in hue. It finds the colour in the other
half of the wheel whose *temperature rank* is the inverse percentile of the input's. Material 3
therefore treats "complementary" as a perceptual-warmth relation, not a geometric one — worth knowing
before writing a rule that asserts 180°.

### 3.4 What the other systems do, in practice

- **Apple (`ON RECORD`).** HIG Color instructs using **semantic** colours rather than fixed ones, and
  provides light/dark and increased-contrast variants of each system colour
  (<https://developer.apple.com/design/human-interface-guidelines/color>). HIG Accessibility states
  the contrast floors: **4.5:1 for body text, 3:1 for large text, 3:1 for meaningful non-text**, and
  advises a **higher ratio — Apple names 7:1 — for text in dark mode and other low-light contexts**,
  with light-on-dark measurably thinner at the same ratio
  (<https://developer.apple.com/design/human-interface-guidelines/accessibility>). Apple's accent
  equivalent is the app **tint**, used for interactive elements only — a small-area role by
  construction.
- **IBM Carbon (`ON RECORD`).** Ten-step ramps per hue, greys carrying the interface, colour reserved
  for semantics; layering is by surface colour and shadow; contrast floors 4.5:1 text, 3:1 graphical
  objects, **3:1 focus indicators**, with disabled states explicitly exempt
  (<https://carbondesignsystem.com/elements/color/overview>).
- **Adobe Spectrum (`ON RECORD`).** Spectrum names colours by **semantic role plus a value on a
  brightness/contrast scale** (e.g. a *blue-500*), rather than by hue alone; the reasoning is that a
  name must express the role's intent, and the numeric value must be a stable handle for a step on a
  scale (<https://adobe.design/ideas/naming-colors-in-design-systems>). For this factory the
  transferable idea is that a role name should encode *purpose*, which is why `amber` — a hue name
  for a role described as "the accent" — is a naming defect independent of the colour.
- **Tailwind CSS v4 (`MEASURED`).** Eleven steps per hue (`50`–`950`), every value published in
  **OKLCH**; 575 `oklch()` values on the colours documentation page, first
  `oklch(.13 .028 261.692)` (<https://tailwindcss.com/docs/colors>). Tailwind does not prescribe a
  harmony; it publishes a wide field of hue ramps for the user to choose a *scheme* from.
- **Material Design 3 site (`ON RECORD`).** <https://m3.material.io/styles/color/system/overview>. Its
  prose is rendered client-side; the machine-readable equivalents of everything this document cites
  from it were taken from Google's own Android documentation and reference source instead, and are
  labelled accordingly.

---

## 4. Contrast as a measurable requirement

### 4.1 WCAG 2.x — the thresholds and where they apply

`ON RECORD`, <https://www.w3.org/TR/WCAG22/>.

| SC | Level | Requirement | Applies to |
|---|---|---|---|
| **1.4.3 Contrast (Minimum)** | AA | **4.5:1** normal text; **3:1** large text | Text and images of text. "Large" = **≥ 18 pt (≈ 24 px)**, or **≥ 14 pt (≈ 18.66 px) bold** |
| **1.4.6 Contrast (Enhanced)** | AAA | **7:1** normal; 4.5:1 large | Same |
| **1.4.11 Non-text Contrast** | AA | **3:1** | User-interface components (visual information needed to identify the control and its state) and **graphical objects** needed to understand content |
| **1.4.1 Use of Color** | A | — | Colour must not be the *only* visual means of conveying information, indicating an action, prompting a response, or distinguishing an element |
| **1.3.1 Info and Relationships** | A | — | Structure/relationships not conveyed by presentation alone |

The formula, `ON RECORD` (WCAG 2.2): `(L1 + 0.05) / (L2 + 0.05)` where `L` is relative luminance from
the sRGB channels linearised as `c/12.92` for `c ≤ 0.04045` and `((c+0.055)/1.055)^2.4` above it, with
weights **0.2126 R, 0.7152 G, 0.0722 B**.

Three properties of this metric decide what a gate can and cannot say.

- **It is hue-blind and chroma-blind.** Two colours with the same relative luminance have a ratio of
  1.0:1 no matter how different they look. `MEASURED`: the lawn's `amber` (relative luminance
  `Y` = 0.7523) and `paper` (`Y` = 0.8990) measure **1.18:1**, so WCAG treats a saturated lime on
  chalk as nearly indistinguishable while it is, in fact, vividly distinguishable. **A WCAG pass is
  not a legibility claim and a WCAG fail is not always a visibility failure.**
- **It measures background *role*, not pixel adjacency.** `ON RECORD`, Understanding 1.4.3: the ratio
  is between the text colour and the "background" colour; the technique notes that **if the text or
  the background is a gradient or image, the ratio must hold at the worst location**. The lawn field
  *is* a three-stop gradient (`ink` → `ink_soft` → `ink_lift`), so every text pair must be evaluated
  against the lightest stop, `ink_lift` at tone 34.3 — where `muted` measures **4.86:1** rather than
  the 8.72:1 it reaches on `ink`. Half of the apparent margin in §0.2 is not real.
- **The thresholds are floor, not goal.** `ON RECORD`, the same page: 4.5:1 was chosen as the level at
  which a *significant majority* of a normative sample with 20/40 vision or better could read the
  text, and the understanding document explicitly says that it "is not the intent that authors conform
  at exactly 4.5:1", and that higher ratios are better. It also states the ratio is **independent of
  font size assumption beyond the large-text exception**.

### 4.2 APCA — what changes

APCA (`Lc`) is a separate, newer readability metric by Andrew Somers, developed for the W3C's
Silver/inclusive-design work, and it is deliberately not a drop-in replacement for the WCAG ratio
(`ON RECORD`: <https://git.apcacontrast.com/documentation/APCAeasyIntro.html>). Four differences
matter to a palette gate.

1. **It is signed.** The output is `Lc`, whose sign encodes **polarity**: `Lc > 0` for dark text on a
   light background, `Lc < 0` for light text on a dark background. WCAG's ratio is polarity-blind,
   which means it cannot express the fact that a given ratio behaves differently in the two
   directions.
2. **It is polarity-sensitive.** The published guidance states that **dark-on-light needs a higher
   magnitude than light-on-dark for the same subjective readability**, and that the thresholds
   therefore differ by polarity (the practical rule of thumb is to aim higher for
   light-text-on-dark, and the APCA tooling applies different baselines per polarity).
3. **It is nonlinear in contrast.** Its lightness-difference exponent is tuned so that the *perceptual*
   step is even, rather than the *ratio* being even — which is why APCA does not produce a single
   number comparable to 4.5:1 and why its author states plainly that APCA and WCAG 2 ratios are not
   interchangeable.
4. **It has an explicit no-text floor.** `ON RECORD`, the APCA documentation and the Readability
   Consortium's ARC test methods (<https://www.readtech.org/ARC/tests/visual-readability-contrast/>):
   the `Lc` scale is used for **non-text** with a much lower floor than for text, precisely because a
   boundary or a large icon need only be *locatable*, not *readable*.

`UNVERIFIED` — this document could not reproduce APCA's numerics from a specification; the values
above are from the algorithm author's own documentation (ON RECORD), and the implementation is
published as source rather than as a specification. **A gate should not implement APCA from this
document.** If the factory wants `Lc`, it should add a dependency and pin its version, exactly as
`.cursor/rules/verification-discipline.mdc` requires for any shared constant.

### 4.3 The pitfall: passing contrast and still being unusable

This is the specific failure the factory's existing checks cannot see, and the lawn slot is the proof.

- **Contrast passes and hierarchy fails.** §0.2's table is almost entirely green: 12.95:1 body text,
  10.95:1 accent, 8.72:1 secondary. The owner's complaint was aesthetic, and no contrast number
  contradicts him.
- **Contrast passes on the wrong pair.** WCAG asks about *text/background*. It says nothing about two
  backgrounds, about an icon on a container, or about the accent's contrast against the *light*
  surface — which here is 1.18:1, and where SC 1.4.11's 3:1 would apply the moment the accent is a
  meaningful graphical object on chalk.
- **A big ratio can coexist with invisible content.** `ON RECORD`, Penn State Accessibility's
  vibrating-colours guidance: certain bright colour pairs "vibrate", and the page's key observation is
  that this is uncomfortable or unreadable **even when the pair passes a contrast measure** — the
  recommended remedy is to change the lightness relationship, not the hue
  (<https://accessibility.psu.edu/color/brightcolors/>). Pure complementary pairs at equal tone (red/
  green, blue/orange, and the classic magenta/green) are the named cases.
- **Chromostereopsis is the extreme form.** `ON RECORD` — the effect where saturated red/blue at
  similar luminance appears to sit at different depths and the eye cannot fuse them; documented with
  exactly the "saturated, similar-luminance" precondition (<https://en.wikipedia.org/wiki/Chromostereopsis>).
  This document could not find a peer-reviewed source with a numeric threshold it was able to open,
  and therefore states no threshold (§9).
- **Gradients and states hide failures.** Understanding 1.4.3's own technique requires the worst case;
  the lawn field's gradient means the *lightest* stop governs. A gate that samples one role per
  position will read 8.72:1 where the user sees 4.86:1.

**Conclusion:** contrast is necessary and cannot be sufficient. Rules 1–7 in the Encodable rules
section are the necessary half; the rules in Groups B, C and D are the half that the lawn slot fails
while passing all of them.

---

## 5. Tonal structure and dark mode

### 5.1 The tonal ladder

Material 3's unit of construction is the **tonal palette**: one (hue, chroma) with a colour available
at every **tone 0–100**. `ON RECORD`, Android Developers, *Material 3 in Compose*: each key colour
relates to a palette of **13 tones** (<https://developer.android.com/develop/ui/compose/designsystems/material3>).
The tones the reference implementation and the Android documentation use are 0, 10, 20, 30, 40, 50,
60, 70, 80, 90, 95, 99, 100 (`ON RECORD`), with intermediate tones for the surface ladder
(`MEASURED`, §5.3).

The consequence is that **a role is not a colour, it is a (hue, chroma, tone) triple**, and the whole
light scheme and the whole dark scheme are two selections of tones from the same palettes. This is the
mechanism that makes "derive light and dark from one seed" possible at all — and therefore the
mechanism whose absence produces drift.

### 5.2 What surface / on-surface / container are *for*

`ON RECORD`, MDC-Android `docs/theming/Color.md`
(<https://github.com/material-components/material-components-android/blob/master/docs/theming/Color.md>)
and Android Developers: the roles exist so a component can be described by **job** rather than by
colour.

| Role family | Job |
|---|---|
| `surface`, `background` | the base the app sits on |
| `surfaceContainer*`, `surfaceDim`, `surfaceBright` | the **depth ladder**: a container's level is its tone, not its shadow |
| `onSurface`, `onSurfaceVariant` | text/icons **on** the surface, at a tone chosen for contrast |
| `primary`, `secondary`, `tertiary` | the three chromatic families: the emphasis role, a quieter support role, and an offsetting accent |
| `onPrimary`, `primaryContainer`, `onPrimaryContainer` | the filled and containerised variants of the emphasis role, each with its own ink |
| `outline`, `outlineVariant` | boundaries — deliberately low-contrast, because a boundary is not text |
| `error`, `onError`, `errorContainer`, `onErrorContainer` | the one reserved semantic family |
| `*Fixed`, `*FixedDim` | roles whose tone does **not** flip between light and dark, for content that must match across modes |

The `on-X`/`X` naming is the load-bearing convention: **every fill has a declared ink.** A palette
that names fills without naming their inks has no way to check its own contrast, which is how a
consumer of a palette ends up pairing `amber_deep` with `paper` by eye.

### 5.3 The tone pairs, measured

`MEASURED` from Material 3's `ColorSpec2021`, the light→dark tone of each role:

| Role | Light tone | Dark tone |
|---|---:|---:|
| `primary` | 40 | 80 |
| `primaryContainer` | 90 | 30 |
| `onSurface` | 10 | 90 |
| `surface` / `background` | 98 | 6 |
| `surfaceContainerLowest` | 100 | 4 |
| `surfaceContainerLow` | 96 | 10 |
| `surfaceContainer` | 94 | 12 |
| `surfaceContainerHigh` | 92 | 17 |
| `surfaceContainerHighest` | 90 | 22 |
| `outline` | 50 | 60 |
| `outlineVariant` | 80 | 30 |
| `onSurfaceVariant` | 30 | 80 |

Two properties to notice, both of which a hand-picked dark scheme loses:

- **The dark counterpart is a function of the light tone, not a new choice.** `primary` 40 ↔ 80 is
  `100 − 40` doubled-ish; `primaryContainer` 90 ↔ 30 is exactly `100 − 90`; `onSurface` 10 ↔ 90 is
  exactly `100 − 10`. The relation is not one formula, but it is a fixed *mapping*, and being fixed is
  the point.
- **The surface ladder steps 2–6 tone at a time and never more.** `98/96/94/92/90` in light,
  `6/10/12/17/22` in dark. `MEASURED` the resulting contrasts: tone 40 vs 100 = 6.46:1, tone 10 vs 90
  = 13.27:1, tone 10 vs 98 = 16.31:1, tone 30 vs 90 = 7.24:1, tone 40 vs 90 = 5.00:1 — i.e. **every
  M3 on/fill pair clears 4.5:1 by construction**, and the pairs designers are told to use for body
  text clear 7:1.

### 5.4 Elevation is tone, not shadow

`ON RECORD`, Android Developers and MDC: Material 3 expresses depth through **surface container tone**
and reserves shadow for components that genuinely float over arbitrary content. The stated reason is
legibility under any contrast setting and any theme — a tonal step is a *measured* separation
(`MEASURED`: two tone steps ≈ 1.2–1.4:1), whereas a shadow is an uncontrolled soft edge. A dark theme
that expresses elevation with shadows alone has no separation at all when the user reduces contrast.

### 5.5 Why hand-picking both schemes is the usual source of drift

The mechanism, stated as arithmetic: a light scheme and a dark scheme chosen independently are **two
free choices in the same space**, so nothing guarantees the dark `container` is the light `container`'s
inverse, or that both reach their contrast floors, or that the two share a hue. Every one of those
becomes a per-role inspection that has to happen in a code review.

`MEASURED` on the lawn slot's own palettes, which is a case study in exactly this: its light and dark
declarations are consistent, and the *reason* is visible in the file — the supporting tones are
declared by hand ("A pale turf wash: containers on the light scheme", "The dark scheme's surface - the
field one step up from the background") and tuned against each other. That works, and it is expensive:
the slot carries **23 roles at HCT chroma ≥ 12** across 11 studio roles and 15 supporting tones, with
**8 of them at ≥ 75 % of the OKLCH gamut ceiling** and **4 chromatic hue families**
(lime 123–130, green 139–158, teal 187–193, rust 29–31) where Material 3 would carry 3 + error
(`MEASURED`).

The method that removes the drift is not "be careful". It is: **choose the seed once, derive both
schemes by a published mapping, and let every role be a lookup.** Material 3's mapping is in §5.3 and
it is the reason Google can ship a contrast guarantee it never inspects per app.

---

## 6. Semantic roles and colour count

### 6.1 Roles, not hues

`ON RECORD` — Adobe's naming guidance is that a colour's name must carry its **semantic role** and a
value expressing the step on a scale (<https://adobe.design/ideas/naming-colors-in-design-systems>).
The factory's own convention points the same way: `.cursor/rules/visual-identity.mdc` requires a role
vocabulary that "encodes a decision the app makes repeatedly". A palette like the lawn's, where the
accent role is named `amber` (a hue) and the second hue is named `accent_deep` (a role), has the two
naming schemes mixed, and the names then fight: the reader has to remember that the "accent" is the
one called `amber`, not the one called `accent_deep`.

### 6.2 60-30-10 and what it is worth

`ON RECORD`, NN/g (<https://www.nngroup.com/articles/color-enhance-design/>): a frequently used
starting point is **60 % a dominant/background colour, 30 % a secondary, 10 % an accent**, with the
accent reserved for the call to action and chroma spent where attention is wanted. NN/g presents this
as a working rule of thumb and **cites no measurement**; §9 records that.

The measured share for the lawn app is in §2.2. Two operational consequences: the rule is checkable
per-screen (rule E4) and is **not satisfied by a whole-screen average**, because the bottom band
measured 14–16 %.

### 6.3 How many hues a small utility should carry

`INFERRED` from §3.2 and §5.3: Material 3, the most widely deployed mobile palette system, gives a
small app **three chromatic families (hues) plus one error family**, with the three separated by
declared chroma and a single declared 60° rotation, and everything else built from one neutral family
at chroma 6–8. A utility screen does not have a job that needs a fourth hue; a fourth hue is almost
always a *decorative* decision, and decoration in a chromatic field is what removes the accent's
headroom.

### 6.4 Colour must never be the only signal

`ON RECORD`, WCAG 2.2 SC 1.4.1 (<https://www.w3.org/WAI/WCAG22/Understanding/use-of-color.html>):
colour must not be the only visual means of conveying information, indicating an action, prompting a
response, or distinguishing an element. The understanding document names the classic failures — "click
the green button", a required field marked only in red, a chart series identified only by hue — and the
standard remedy is a redundant non-colour cue (a label, an icon, a shape, a position, a pattern).

This is the one colour rule that is already universally enforced in the industry, and it has a direct
consequence for palettes: a role used *only* to show state (a "done" lime, a "missed" red) must be
paired with a glyph, a label or a shape, or the state is invisible to a user with a colour-vision
deficiency **and** to a user who has turned on a monochrome/accessibility mode. `MEASURED` context
for how much that matters: roughly 1 in 12 men and 1 in 200 women have a form of colour-vision
deficiency (`ON RECORD`, widely reported; the factory's own sources do not quantify it further, so it
is not used as a threshold here).

### 6.5 Error and state colour is a family, not a colour

`ON RECORD`, Material 3: `error` is present in *every* scheme at fixed hue ≈ 25 and chroma 84
(`MEASURED` for the lawn seed: `error` = H25.1 C84.3 T40.0 — the same hue and chroma as a purple
seed's error). It ships with `onError`, `errorContainer` and `onErrorContainer` because it needs the
same fill/ink/container structure as any other family. A palette that carries one red as a single
role cannot express an error *container*, and will end up reusing the primary ink on it by eye —
`MEASURED` the lawn palette's alternative: `Copper` H28.8 C60.8 T37.8 at **80.0 %** of the OKLCH gamut
ceiling with `CopperLight` at **100.0 %**.

---

## 7. How professionals evaluate

Each of these is a *named* method rather than a threshold, and §9 says so. They are the practices a
builder can run by hand when no rule catches the defect.

| Method | What it is | What it catches | Threshold |
|---|---|---|---|
| **Squint test** | defocus (squint or blur) the screen and see what still organises | whether hierarchy exists at all without reading | none |
| **Greyscale / luminance-only check** | reduce the palette to relative luminance and re-read the screen | whether the design survives on tone alone, and whether two roles collapse into one (`paper`/`amber_pale` at 1.01:1 is the lawn case) | none |
| **Hue-rotation check** | rotate every hue by a constant and re-run the contrast assertions | whether the design depends on a specific hue or on structure — contrast is hue-invariant, so a scheme that breaks under rotation was relying on hue to carry a relationship it should carry with tone | none |
| **1× screenshot check** | look at the palette on a real device screenshot at 100 %, never in a swatch grid or a mock-up | area share, gradient worst-case contrast, and the accents the eye actually rests on (the 14–16 % bottom band) | none |
| **Side-by-side against the derivation** | derive the scheme from the seed by the chosen algorithm and diff it against what was declared | whether each deviation is intentional and can be justified in writing | none |
| **Desaturation check** | desaturate the palette uniformly and see whether the accent is still the loudest element | `INFERRED` — whether hierarchy rests on chroma at all, or only on hue | none |
| **Sibling-slot check** | compare with every other slot's `store/studio.json` mark, palette and components | identity reuse, which `.cursor/rules/visual-identity.mdc` forbids and Play's Repetitive Content policy punishes | none |
| **Contrast battery** | every declared text/background pair, at the gradient's worst stop, in both schemes | WCAG 1.4.3 / 1.4.6 / 1.4.11 | 4.5 / 7 / 3:1 |
| **Non-colour redundancy audit** | every colour-coded state, checked for a second cue | WCAG 1.4.1 | none |

The two that are cheap and would have caught the motivating slot are the **greyscale check** (it
exposes `paper` = `amber_pale`) and the **side-by-side against the derivation** (it exposes the accent
at 1.88× the chroma Material 3 allows at that tone). Neither is currently performed by anything.

---

## Encodable rules

The list below is written to be turned into a program. Every rule is (a) one sentence, (b) a numeric
threshold, (c) a source. Where the threshold is this document's own, the status says so — a rule
marked `INFERRED` is a defensible convention, not a measurement, and the factory's convention is to
label rather than dress.

**Units and conventions used below, fixed once.**

- `HCT(h, c, t)` — hue in degrees 0–360, chroma 0–~130, tone 0–100; computed by
  `Hct.fromInt(argbFromHex(hex))` from `@material/material-color-utilities`.
- `headroom%(role)` — `C_OKLCH(role) ÷ C_max(role) × 100`, where `C_max` is the largest OKLCH chroma
  that stays inside sRGB when the role's own lightness and hue are held (found by bisection over the
  OKLab→sRGB inverse, the CSS Color 4 §14 MINDE definition). **Not** the HCT
  `Hct.from(hue, 200, tone).chroma` proxy — §2.1 shows that proxy reporting pure green at 189 % of its
  own ceiling.
- `WCAG(a, b)` — the WCAG 2.2 ratio, `(L1+0.05)/(L2+0.05)`.
- `Δhue` — the circular distance, `min(|a−b|, 360−|a−b|)`.
- `family` — a set of roles the palette declares as one palette, or which shares a hue within
  `HUE_FAMILY_TOLERANCE`.

Group A — contrast and legibility (the necessary half; the lawn slot passes almost all of it)

1. **Every role pair used as text on a background must reach 4.5:1, and every role pair used as text
   larger than 18 pt (or 14 pt bold) must reach 3:1.**
   *Threshold:* 4.5:1 / 3:1. *Source:* WCAG 2.2 SC 1.4.3 (`ON RECORD`, <https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html>).
2. **Every pair the app marks as important must reach 7:1.**
   *Threshold:* 7:1. *Source:* WCAG 2.2 SC 1.4.6 AAA, and Apple HIG Accessibility's advice to use a
   higher ratio in dark mode (`ON RECORD`).
3. **Every non-text role that carries meaning — icon, control boundary, focus ring, selected-state
   indicator, chart mark — must reach 3:1 against each adjacent colour it is drawn on.**
   *Threshold:* 3:1. *Source:* WCAG 2.2 SC 1.4.11 (`ON RECORD`); Carbon states the same for focus
   indicators (`ON RECORD`).
4. **Every `on-X` / `X` pair declared in the palette must reach 4.5:1, and every `X`-container pair
   must reach 3:1 against its own background.**
   *Threshold:* 4.5:1 / 3:1. *Source:* WCAG 1.4.3 / 1.4.11, plus the `on-X` naming convention of
   Material 3 (`MEASURED`; Material 3's own pairs measure 5.00:1–16.31:1 against tone
   pairs 40/90, 40/100, 10/90, 10/98, 30/90 — `MEASURED`).
5. **Where a background is a gradient, every text/background check must be run against the stop that
   is worst for that text, not against the declared "base" role.**
   *Threshold:* the same 4.5:1, evaluated at the worst stop. *Source:* WCAG 2.2 Understanding 1.4.3's
   own technique for gradients (`ON RECORD`).
6. **Two roles used as text on the same background, where the difference between them carries meaning
   (primary vs secondary text, or a link inside body text), must differ from each other by at least
   3:1.**
   *Threshold:* 3:1. *Source:* WCAG 2.2 Understanding 1.4.1, technique G183, which requires a link to
   be distinguishable from surrounding text by ≥ 3:1 (`ON RECORD`).
7. **No information may be encoded by colour alone: every element whose state is shown by colour must
   declare a second, non-colour cue.**
   *Threshold:* boolean, per element. *Source:* WCAG 2.2 SC 1.4.1 (`ON RECORD`,
   <https://www.w3.org/WAI/WCAG22/Understanding/use-of-color.html>).

Group B — hue structure (what the lawn slot actually fails)

8. **The accent's hue must differ from the dominant field's hue by at least 40°, unless the accent is
   declared as the same hue as the field, in which case its tone must differ by at least 40 and its
   chroma must be at least 2× the field's.**
   *Threshold:* Δhue ≥ 40°, or (Δtone ≥ 40 AND chroma ratio ≥ 2). *Source:* `INFERRED` from Material
   3's own tertiary offset of **+60°** (`MEASURED`, §3.2) and its 30° analogous step
   (`MEASURED`, §3.3); 40° is set below 60° to leave room for palettes that legitimately use a
   split-complementary offset. **The lawn accent is 31.9° from the field at a chroma ratio of 1.9–3.0
   and a tone gap of 72.4 — it passes only the Δtone leg, and fails the intent.**
9. **If a palette declares a complementary pair, |Δhue| between the two must be between 150° and
   210°.**
   *Threshold:* 150°–210°. *Source:* `INFERRED` from the colour-wheel definition of complementary;
   note Material 3's `complement()` is a temperature relation rather than a fixed 180° (`MEASURED`,
   §3.3), so this rule applies only to palettes that declare the scheme.
10. **Two roles that a user must tell apart, and whose tone difference is under 20, must differ in hue
    by at least 15°.**
    *Threshold:* if |Δtone| < 20 then Δhue ≥ 15°. *Source:* `INFERRED` — the lawn failure it is written
    for is `amber` (H123.4 T89.5) vs `paper` (H115.9 T96.0): Δhue **7.5°**, Δtone **6.5**,
    WCAG **1.18:1**. Nothing in the industry literature supplies 15°; it is set just above the 7.5°
    that failed.
11. **The palette must not contain more than three chromatic hue families plus one error family,
    where a family is a group of roles whose hues lie within the declared family tolerance of one
    another.**
    *Threshold:* ≤ 3 chromatic families at chroma ≥ 24, + 1 error family. *Source:* `INFERRED` from
    Material 3's structure — `primary`, `secondary`, `tertiary` at the source hue, source hue, and
    **source hue + 60°**, plus `error` at hue 25 (`MEASURED`).

Group C — chroma discipline (the strongest measured evidence in this document)

12. **REPORT ONLY — no pass/fail rule is proposed. The palette should not be *entirely* at the gamut
    boundary, and the accent's headroom should be reported.**
    *Threshold:* report `headroom%` per role and the count of roles at ≥ 75 %; **no bound**, because
    none survived scrutiny. *Source:* `UNVERIFIED`. This rule began as "no role above 75 % of the gamut
    ceiling" and could not be defended: `MEASURED` the two counter-examples that kill it — Material 3's
    own light `primaryContainer` `#e9ddff` is at **100.0 %** because the algorithm clamps it there, and
    Tailwind's `lime-500` `#84cc16` sits at **97.6 %** on purpose. The lawn palette has **8 of 26**
    colours at ≥ 75 % with `CopperLight` at 100.0 % and the accent at 87.5 %, and that is **not** what
    makes it wrong (§0.4). Recorded so a later gate does not mistake proximity to the boundary for a
    defect — which would fail Material 3 and Tailwind alike.
13. **Field, surface and background roles must have HCT chroma ≤ 12.**
    *Threshold:* C ≤ 12. *Source:* `ON RECORD` — Material 3's `neutralPalette` chroma is **6** and
    `neutralVariantPalette` is **8** in every scheme variant (`MEASURED`, §2.3) — and `MEASURED` for
    this seed, Material 3 produces `surface` C6.1, `surfaceContainer` C5.8, `surfaceVariant` C8.3.
    **The lawn field is C22.8–35.5, i.e. 2.7–4.3× the bound.**
14. **The highest-chroma primary/interactive role must have HCT chroma ≤ 48.**
    *Threshold:* C ≤ 48. *Source:* `MEASURED` — Material 3's `RAINBOW` and `FRUIT_SALAD` primaries are
    both 48, the default is 36, and the only higher value in the library (200, `VIBRANT`) is a
    deliberate unclamped request for the gamut boundary (§2.3).
15. **The accent's chroma must not exceed 1.5× what Material 3 derives for the primary container at
    the accent's own hue and tone.**
    *Threshold:* `C(accent) ≤ 1.5 × C(SchemeTonalSpot(seed=accent).primaryContainer)`. *Source:*
    `MEASURED` — for the lawn accent's own HCT (123.4, 68.0, 89.5), Material 3's `primaryContainer`
    is `#d8eb9a` H123.2 **C36.1 T90.0**, so the accent is at **1.88×** and fails. This is the
    single most directly damning rule, because it is the vendor's own algorithm disagreeing with the
    palette rather than this document's opinion.
16. **No role may satisfy `90 ≤ hue ≤ 111 AND chroma > 16 AND tone < 65`.**
    *Threshold:* the Material 3 dislike predicate. *Source:* `MEASURED` —
    `@material/material-color-utilities/dislike/dislike_analyzer.js` (§2.4). **The lawn palette
    passes this rule**; it is included because it is the only vendor-encoded aesthetic bound that
    exists and a gate should carry it, not because it catches this defect.
17. **The accent must be the highest-chroma role in the palette, by at least 1.5× the next highest
    non-error role.**
    *Threshold:* `C(accent) ≥ 1.5 × C(next highest non-error role)`. *Source:* `INFERRED` — chroma is
    the hierarchy channel (§2.1) and an accent that is not the most chromatic thing on screen has no
    claim to be the accent. The lawn palette **passes** (68.0 vs 35.5 = 1.9×), which is precisely why
    the rules above are needed alongside this one.
18. **No gradient or family may vary chroma by more than 8 while its hue varies by less than 5°.**
    *Threshold:* if Δhue < 5° then ΔC ≤ 8 across the family. *Source:* `INFERRED` from Material 3's
    tonal-palette contract — a palette is "constant in hue and chroma, but varies in tone"
    (`ON RECORD`, `TonalPalette`), with chroma falling only where the gamut clamps it (§1.2).
    **The lawn field fails**: `ink` C22.8 → `ink_lift` C35.5 is ΔC **12.7** over Δhue **3.8°**, and
    neither end is at the gamut ceiling (71.1 % and 72.1 %).**

Group D — derivation and palette integrity

19. **Every palette must declare a seed and the derivation used to get from it to each role.**
    *Threshold:* `seed` present; every role names either a Material 3 role name or an explicit
    `(Δhue, chroma, tone)` triple relative to the seed. *Source:* `INFERRED` from
    `.cursor/rules/visual-identity.mdc`'s "identity is a build input" plus Material 3's
    seed→`SchemeTonalSpot`/`SchemeContent` model (`ON RECORD`, Android Developers). This is the rule
    that makes the whole section enforceable rather than advisory: once the derivation is declared,
    every other rule can be computed *from the declaration* instead of from the final hex values.
20. **Within a declared family, all roles must share a hue within ±5°.**
    *Threshold:* hue spread ≤ 5° per family. *Source:* `ON RECORD` — Material 3's `TonalPalette` is
    constant-hue by construction; a family whose hue wanders is a gradient wearing a family's name.
21. **Two declared roles must not be closer than 1.2:1 in contrast while also being within 15° in hue
    and within 20 in chroma of each other.**
    *Threshold:* reject if `WCAG(a,b) < 1.2` AND `Δhue < 15` AND `|ΔC| < 20`. *Source:* `INFERRED`;
    the measured case is `paper` `#F6F4E4` vs `amber_pale` `#EAF9C6` at **1.01:1**, Δhue **11.2°**,
    ΔC **16.5** — two declared roles that a user cannot distinguish.
22. **A palette that deviates from its declared derivation must carry a written reason per deviation.**
    *Threshold:* every role whose computed value differs from the derivation by more than (Δhue 5°,
    ΔC 4, Δtone 3) requires a text justification. *Source:* `INFERRED` from
    `.cursor/rules/differentiator-contract.mdc`'s "never let a promise disappear silently" — the
    same principle applied to a colour role, and the mechanism that turns the lawn's metaphor into a
    claim that can be reviewed.

Group E — role count and area

23. **The accent's area share must not exceed 10 % of any screen, measured on a real screenshot, and
    not more than 10 % of any 20 %-height region of that screen.**
    *Threshold:* ≤ 10 % whole-screen, ≤ 10 % per region. *Source:* `ON RECORD` — NN/g's 60-30-10
    (`<https://www.nngroup.com/articles/color-enhance-design/>`), which is a convention with no
    measurement behind it. *Measured baseline:* the lawn app's accent occupies **2.64–5.76 %** of a
    full 1080×2340 screen (passes) and **14.25 %** of a 1080×490 bottom crop (fails the per-region
    leg) — `MEASURED`, §2.2.
24. **The dominant field's area share must be between 55 % and 65 %, and the secondary between 25 %
    and 35 %.**
    *Threshold:* as stated. *Source:* `ON RECORD`, NN/g 60-30-10 as above. *Caveat:* the design may
    legitimately differ; the rule is worth computing as a *report* rather than a hard fail (§9).
25. **No more than three chromatic hue families may be present at chroma ≥ 24, excluding the reserved
    error family.**
    *Threshold:* ≤ 3 + 1. *Source:* same as rule 11 (`MEASURED` Material 3 structure).

Group F — tonal structure and dark mode

26. **Every role must exist in both schemes, and the dark tone must be a declared function of the light
    tone rather than an independent choice.**
    *Threshold:* every light role has a dark counterpart, and `|darkTone − f(lightTone)| ≤ 3` for the
    declared mapping `f` (Material 3's: primary 40↔80, primaryContainer 90↔30, onSurface 10↔90,
    surface 98↔6, outline 50↔60). *Source:* `MEASURED` from Material 3's `ColorSpec2021` (§5.3).
27. **Adjacent surface levels must differ by between 2 and 8 tone.**
    *Threshold:* 2 ≤ Δtone ≤ 8 for consecutive members of the surface ladder. *Source:* `MEASURED` —
    Material 3's ladder steps `100/96/94/92/90` (light) and `4/10/12/17/22` (dark), i.e. 2–6.
28. **The base dark surface must have tone between 4 and 12, and must never be tone 0.**
    *Threshold:* 4 ≤ T ≤ 12. *Source:* `MEASURED` — Material 3's dark `surface` is tone **6** and
    `surfaceContainerLowest` is tone **4**; a pure-black surface destroys the tonal elevation ladder
    because there is no darker step to put anything on.
29. **Every scheme must reach its contrast floors on its own, not merely in one scheme.**
    *Threshold:* rules 1, 3 and 4 re-run per scheme. *Source:* WCAG 2.2 applies to the rendered app in
    whichever theme it is in; Apple HIG supplies the dark-mode-specific advice to go higher
    (`ON RECORD`).

Group G — evaluation (named methods, no machine threshold; the builder runs them, the gate cannot)

30. **Greyscale check:** with all hue and chroma removed, the intended hierarchy must still be legible
    and no two roles must collapse into the same grey.
    *Threshold:* none. *Source:* §7; `INFERRED`.
31. **Squint check at 1× on a real device screenshot:** the primary action must still be the first
    thing the eye finds.
    *Threshold:* none. *Source:* §7; `INFERRED`.
32. **Hue-rotation check:** rotate every role's hue by a constant and re-run Group A; the scheme must
    remain valid, because contrast is hue-invariant and a scheme that breaks under rotation was
    carrying a relationship with hue that tone should carry.
    *Threshold:* none. *Source:* `INFERRED` from the hue-independence of the WCAG luminance formula
    (`MEASURED`).
33. **Side-by-side against the derivation:** render the declared palette beside the scheme derived from
    its own seed and diff them; every deviation must have a written reason (rule 22).
    *Threshold:* none — it is rule 22's evidence. *Source:* `INFERRED`, §7.
34. **Sibling-slot check:** no two `store/studio.json` files may share a mark, a palette (as a set of
    roles within the tolerances of rule 21) or a component kit.
    *Threshold:* none numeric. *Source:* `ON RECORD` — `.cursor/rules/visual-identity.mdc`, justified
    there by Play's Repetitive Content policy.

---

## 9. What this document does not establish

State the limits rather than let the numeric furniture imply more than the evidence does.

**Conventions wearing numbers.** The following are in the Encodable rules section and are *not*
measurements:

- **Rule 8's 40° hue offset** is this document's number, sitting between Material 3's 30° analogous
  step and its 60° tertiary offset. No study says 40° is the perceptual optimum for an accent.
  Material 3's 60° is itself a design choice, not a finding.
- **Rule 10's 15° / rule 16's thresholds / rule 21's 1.2:1** are set just above the specific measured
  failures. They are diagnostic bounds derived from one artifact, not perceptual constants. Re-derive
  them if a second case contradicts them.
- **Rule 12 is explicitly not a rule.** It is a report, and it is in the list only so the honest
  version of the question appears somewhere. §2.1 and rule 12 give the measured counter-examples that
  defeated it — Material 3's own light `primaryContainer` at 100 % headroom (clamped there by the
  algorithm) and Tailwind's `lime-500` at 97.6 % (chosen there) — and the lawn palette's own headroom
  is not what is wrong with it. A gate that enforced a bound here would fail two real design systems.
- **Rule 13's chroma ≤ 12 and rule 14's ≤ 48** are Material 3's constants. Material 3 is a design
  system, not a perceptual study: the numbers are *choices* that produce its look. A different system —
  IBM Carbon, Tailwind — would produce different bounds.
- **Rule 23 and 24, the 60-30-10 shares.** NN/g presents them as a rule of thumb and cites no
  measurement; the sources give no study that validates the proportions. Treat them as a report.
- **Rule 17 ("the accent must be the most chromatic role")** is a convention of hierarchy, not a
  measurement, and the motivating palette satisfies it.
- **The chroma/area mechanism in §2.2** ("the eye adapts to the prevailing field") is stated here as
  an explanation consistent with the measurements. This document found no source that quantifies
  adaptation and sets a threshold from it. `UNVERIFIED`.

**What could not be sourced.**

- **`m3.material.io`** — the canonical Material 3 guidance site — serves a client-rendered shell; its
  static HTML contained no prose this document could quote. Everything normally cited from it is
  instead cited from `developer.android.com` and from Google's own reference implementation, and
  labelled accordingly (see §3.4's last row). This is a real substitution, not a formality: the
  prose on m3.material.io may state bounds this document does not carry.
- **APCA's numeric thresholds.** §4.2 quotes the algorithm author's documentation (`ON RECORD`) but
  this document could not obtain a specification of the `Lc` table, and the author states explicitly
  that APCA and WCAG 2 ratios are not interchangeable. **No APCA threshold is proposed in the
  Encodable rules section** — an implementation would need to pin a version of the algorithm.
- **A defensible numeric bound on chroma *magnitude*.** Material 3's constants (6 for neutrals, 36 for
  a default primary, 48 for its loudest non-degenerate variant) and Tailwind's near-boundary ramps are
  both *choices*, and they disagree about where good taste stops. This document found no source that
  measures a chroma magnitude against a usability or preference outcome, which is why rules 13–14 rest
  on Material 3's constants as a *design system's* numbers and rule 12 proposes no bound at all.
- **A reliable gamut-headroom measure inside HCT.** §2.1's proxy (`Hct.from(hue, 200, tone).chroma`)
  reports pure green at 189 % of its own ceiling, so this document measures headroom in OKLCH instead.
  The disagreement is a property of the reference implementation's solver near the gamut surface, and
  it is recorded rather than worked around, because a future gate would otherwise re-derive the proxy
  and trust it.
- **Chromostereopsis.** The phenomenon is documented and the precondition ("saturated, similar
  luminance") is consistent with the literature, but the peer-reviewed URL this document attempted to
  cite returned 404, so **no chromostereopsis threshold is proposed**. The nearest defensible source
  is Penn State's vibrating-colours guidance, which supplies the remedy (change the lightness
  relationship) rather than a number.
- **Colour-vision-deficiency prevalence.** §6.4 uses it qualitatively only; the factory's own sources
  do not quantify it and no threshold is derived from it.
- **Any study connecting palette chroma to user preference or installs.** The motivating defect is
  aesthetic. This document explains its *mechanism* in measurable quantities and **does not claim to
  measure beauty** — the same limit `.cursor/rules/visual-identity.mdc` already states: "None of these
  can say the app is beautiful."

**The honest summary.** Rules 12–15 and 18 are the ones that fail on the motivating palette for
structural reasons, and rule 15 is the strongest because it is a vendor algorithm disagreeing rather
than an opinion. Rules 8 and 11 are the ones that name the actual defect (the accent is the field's
own hue, one analogous step away, in a palette that uses hue as a gradient). Rules 1–7 are the part
that already works and must not be traded away for the rest. Rules 30–34 cannot be automated and
should not be dressed up as if they could — their value is that they are the methods that produced
this analysis in the first place.

---

## 10. Sources

All URLs were fetched or HEAD-checked on 2026-10-02 and returned HTTP 200 unless noted.

**Colour spaces**

1. W3C, *CSS Color Module Level 4*. <https://www.w3.org/TR/css-color-4/> — §7 (HSL hue "not
   perceptually uniform"), §9.1 (Lab, "perceptually uniform", L = 50 % is mid grey), §9.2 ("Oklab is
   more perceptually uniform than CIE Lab"), §9.3 (OKLCH hue angles: 90° mustard yellow, 180°
   greenish cyan, 270° sky blue), §13.2 (Oklab for perceptually even spacing in gradients), §14
   (gamut mapping by holding hue and reducing chroma), §4.3 (hue angles differ per space).
2. B. Ottosson, *A perceptual color space for image processing* (2020).
   <https://bottosson.github.io/posts/oklab/> — the `M1`/`M2` matrices and the cube-root
   non-linearity reproduced in §1.3.
3. MDN, *`<color>` / `oklch()`*. <https://developer.mozilla.org/en-US/docs/Web/CSS/color_value/oklch>
   — "0 is … a fully desaturated grey, 0.4 represents a highly saturated colour".
4. Google, *material-color-utilities* (reference implementation of HCT, tonal palettes and the
   Material 3 scheme algorithms). <https://github.com/material-foundation/material-color-utilities>
   — files read: `Hct.java`, `TonalPalette.java`, `ColorSpec2021.java`, `DislikeAnalyzer.java`,
   `TemperatureCache.java`, `SchemeTonalSpot.java`, `ColorUtils.java`, and the JavaScript build under
   `.kiln/scratch/mcu/` used for every computed number in this document.

**Design systems**

5. Android Developers, *Material Design 3 in Compose*.
   <https://developer.android.com/develop/ui/compose/designsystems/material3> — key colours, the
   **13 tones** per key colour, the contrast guarantee, dynamic colour.
6. Android Developers, *Custom design systems in Compose*.
   <https://developer.android.com/develop/ui/compose/designsystems/custom> — replaced and
   derived colour roles.
7. Material Components for Android, `docs/theming/Color.md`.
   <https://github.com/material-components/material-components-android/blob/master/docs/theming/Color.md>
   — the role table, surface container ladder, tonal elevation.
8. Material Design 3, *Color system*. <https://m3.material.io/styles/color/system/overview> — cited
   for existence of the guidance; its prose is client-rendered and could not be quoted (§9).
9. IBM, *Carbon Design System — Color*. <https://carbondesignsystem.com/elements/color/overview> —
   neutral-dominant palettes, semantic colour, layering, contrast and focus-indicator floors.
10. Apple, *Human Interface Guidelines — Color*.
    <https://developer.apple.com/design/human-interface-guidelines/color> — semantic colours,
    light/dark and increased-contrast variants, the tint as an interactive-element role.
11. Apple, *Human Interface Guidelines — Accessibility*.
    <https://developer.apple.com/design/human-interface-guidelines/accessibility> — 4.5:1 / 3:1 / 3:1,
    and the advice to go higher (7:1) in dark mode.
12. Apple, *App Store Connect Help — Manage accessibility*.
    <https://developer.apple.com/help/app-store-connect/manage-app-information/manage-accessibility>
13. Adobe, *Naming colors in design systems*.
    <https://adobe.design/ideas/naming-colors-in-design-systems> — semantic name plus a value on a
    brightness/contrast scale.
14. Tailwind Labs, *Colors*. <https://tailwindcss.com/docs/colors> — eleven steps per hue, published
    in **OKLCH** (575 `oklch()` values on the page as fetched, first `oklch(.13 .028 261.692)`).

**Contrast and evaluation**

15. W3C, *WCAG 2.2*. <https://www.w3.org/TR/WCAG22/> — SC 1.4.1, 1.4.3, 1.4.6, 1.4.11, 1.3.1.
16. W3C, *Understanding SC 1.4.3: Contrast (Minimum)*.
    <https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html> — the formula, the
    large-text definition, the gradient worst-case technique, and the statement that 4.5:1 is a floor
    rather than a goal.
17. W3C, *Understanding SC 1.4.1: Use of Color*.
    <https://www.w3.org/WAI/WCAG22/Understanding/use-of-color.html> — colour must not be the only
    visual means; technique G183's 3:1 for links in body text.
18. A. Somers, *APCA in a Nutshell / APCA Easy Intro*. <https://git.apcacontrast.com/documentation/APCAeasyIntro.html>
    — `Lc`, polarity, and the explicit statement that APCA is not interchangeable with WCAG 2 ratios.
19. The Readability Consortium, *Visual Readability Contrast (ARC) test methods*.
    <https://www.readtech.org/ARC/tests/visual-readability-contrast/>
20. Nielsen Norman Group, *Using Color to Enhance Your Design*.
    <https://www.nngroup.com/articles/color-enhance-design/> — 60-30-10, accent for the call to
    action. Stated as a convention; no measurement cited.
21. Penn State Accessibility, *Vibrating Colors*. <https://accessibility.psu.edu/color/brightcolors/>
    — bright colour pairs that vibrate despite a passing contrast measure; remedy is to change
    lightness.
22. Wikipedia, *Chromostereopsis*. <https://en.wikipedia.org/wiki/Chromostereopsis> — used only for
    the existence and precondition of the effect; no threshold is derived (§9).

**The measured artifact**

23. `apps/2026-10-01-lawn-round-log/store/studio.json` and
    `apps/2026-10-01-lawn-round-log/app/src/main/java/com/gexetr/lawnroundlog/ui/theme/Color.kt` —
    the eleven declared roles, the fifteen supporting tones, and the "stripe that has just been mown"
    justification this document was written to replace with a method.
24. `.kiln/scratch/lawn-full-palette.mjs`, `.kiln/scratch/lawn-pixel-share.py`,
    `.kiln/scratch/palette-research2.mjs` — the scripts behind every `MEASURED` number here; they read
    the repository and the device screenshots under `.kiln/scratch/device-lawn-round-log/` and write
    nothing.
