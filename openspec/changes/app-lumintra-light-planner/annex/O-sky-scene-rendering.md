# O — The sky-scene rendering contract

**Worker:** spec worker `O`, sky-scene rendering.
**Date:** 2026-10-08.
**Product:** **Lumintra**, `lumintra-light-planner`, `com.gexetr.lumintralightplanner` — a **$9.99
one-time**, closed-source, no-ads, no-server Android light-and-sky session planner. Hero screen: a
scrubbable Night scene. **Fixed by the brief; not relitigated here.**
**Deliverable:** an implementable rendering contract. It specifies; it does not re-argue the
architecture and it writes no application code.

**Inputs read in full this session** (all `MEASURED` by `Read`):
`.kiln/scratch/positioning-2026-10-08/H-rendering-options.md` (the layered-2-D decision, the AGSL
floor, the A40 specification) ·
`.kiln/scratch/positioning-2026-10-08/J-sky-realism-and-bar.md` (the twilight models, the star
techniques, the horizon treatment, the B1–B20 bar) ·
`.kiln/scratch/positioning-2026-10-08/I-assets-and-data.md` (every asset licence) ·
`C:\Users\vsobo\.cursor\projects\d-Python-PlayKiln\canvases\lumintra-night-scene.canvas.tsx`
(the approved visual target).

**Label discipline** follows `.cursor/rules/grounded-claims.mdc`: `MEASURED` (a command run or a file
read in this session), `ON RECORD` (a named source states it and the source was read), `INFERRED`
(it follows from labelled facts, and the step is shown), `UNVERIFIED` (not checked; the exact check
that would settle it is named). The ledger is §11.

**Two things this contract deliberately does not contain**, stated first so nothing below implies
them: (a) **no frame-time, thermal or battery measurement exists for this scene on this phone** — §10
names the commands that produce them, and until they are run, "smooth on the A40" is `UNVERIFIED`;
(b) **no code and no assets were downloaded, copied, or ported.** Every algorithm below is
reimplemented from its published description, and §2 says exactly where each one comes from.

---

## 0. Notation and the two constants everything is measured against

| Symbol | Meaning | Value |
|---|---|---|
| `ε` | **solar altitude**, degrees, negative below the horizon. The scene's single independent variable. | slider range **+6° … −20°** |
| `h` | **view elevation** above the horizon, degrees | `0° … 90°` |
| `Δaz` | view azimuth measured **from the solar meridian**, degrees | `0° … 180°` |
| `s` | **scrub position** in the night, `0 … 1` | `s = 0` at ε = +6°, `s = 1` at ε back to +6° |
| `R` | Earth radius | `6371 km` |
| `H` | effective top of the optically significant atmosphere (the boundary the Earth-shadow formula uses) | `20 km` |
| `Φ(θ)` | **relative optical air mass** along a line of sight at altitude `h` | Kasten–Young, §2.5 |
| device | the factory's test phone | **`SM-A405FM`, serial `R58MB3SGZVL`, Android 11 = API 30**, 1080×2340 @ 480 dpi, Exynos 7904, **Mali-G71 MP2 ×2 @ 770 MHz**, 3100 mAh |

The device row is `MEASURED` — `.cursor/rules/device-testing.mdc:30` (serial, model, Android 11) and
`docs/research/2026-10-08-light-and-sky-session-planner-market.md:506-507` (`SM-A405FM`, Android 11);
GPU / SoC / battery are `ON RECORD` from H §9.1. `adb -s R58MB3SGZVL shell getprop
ro.build.version.sdk` must read `30` before any reading in §10 is trusted.

**The one architectural fact this contract is built around** (`ON RECORD`, H §0.4 and §3.1): *AGSL is
Android 13+ (API 33+)*, so the A40 — API 30 — **never executes the shader**. The fallback is not a
degraded mode; on the factory's own test phone it is the product. Every choice below is made so the
fallback is the beautiful path and the shader is an addition.

---

## 1. The layer stack

### 1.1 Draw order, owner of pixels, renderer, input

Drawn into one Compose `Canvas` inside a `Box`, bottom of the list painted first. "Owns which pixels"
is exact: it is the invariant each layer is responsible for, and §9/V-checks test it.

| # | Layer | Renderer | Owns | Input data |
|---|---|---|---|---|
| **L1** | **Sky field** | **fallback:** one `ImageBitmap` strip, `1 × Hpx`, drawn 1:1 into the scene rect · **API 33+:** AGSL `RuntimeShader` over the scene rect | every pixel of the scene rect, initially | `ε`, `h(px)`, `Δaz`, the twilight LUT (L1a/L1b), `E(ε)` |
| **L2** | **Night floor** (airglow + zodiacal + integrated starlight) | additive `Brush.verticalGradient` (fallback) / an addend term in the shader | added to L1, **only where the sky is dark** | `ε` (amplitude), `h` (profile) |
| **L3** | **Milky Way composite** | `drawVertices` over a 48×24 quad mesh with an `ImageShader` (bitmap) | the band and only the band | the baked equirectangular MW map, `R_h·R_ge`, `ε` (opacity) |
| **L4** | **Star field** | ≤ 8 batched `drawPoints` calls, `BlendMode.Plus`, one pre-rendered sprite per (magnitude bin × colour bucket) | the sprite footprints only | FK6 + 2MASS catalogue, `R_h`, `ε` (visibility), `Φ(h)` |
| **L5** | **Deep-sky targets + constellation figures** | `Path` per object; IAU figure polylines | marker strokes only | Wikidata/HEASARC curated list, IAU `CC BY 4.0` figures, `R_h` |
| **L6** | **Moon** | `drawImage` of `lroc_color_2k.jpg` clipped to its disc `Path`, plus a phase `Path` for the dark limb, plus a radial-gradient glow | the moon disc + glow radius | ephemeris (alt/az, phase fraction, bright-limb position angle, libration), NASA SVS texture |
| **L7** | **Satellite trail** | one `Path` with a fading stroke, split at horizon crossings | the trail's own stroke | SGP4 position history (12 vertices over the last 20 s) |
| **L8** | **Earth-shadow wedge** | **not an overlay** — a modulation applied inside L1's evaluation (`L1` samples `LUT_A` only below `α_s(ε)`) | it changes L1's value; it never draws on top of L4 | `α_s(ε)`, `Δaz` |
| **L9** | **Alpenglow band** | one additive `Brush.radialGradient` rectangle, clipped to `h > α_ridge − 8 px` | the warm backscatter over the ridge | `ε`, the sun's azimuth, the ridge polyline |
| **L10** | **Horizon silhouette** | one `Path` (the ridge) + optional tree glyphs | everything below the ridge; **opaque** | the user's `HorizonProfile` |
| **L11** | **Lens FOV frame** | 4 corner ticks + a hairline rect, `Path` | its own strokes | chosen lens (h,v) FOV, scene centre |
| **L12** | **Target marker + label** | marker `Path` (occluded by L10) + label drawn in the overlay plane | marker + label | target position, `ε` |
| **L13** | **UI chrome** | Compose `MaterialTheme` surfaces: clock, phase, moon %, target altitude, pass warning, info panel, the timeline strip, the scrub thumb | its own bounds | the pure `SkySnapshot` of L1–L12 |

**Three consequences that are load-bearing.**

1. **L8 is *inside* L1, not above it.** The Earth's shadow dims *scattered sunlight*; it does not dim
   starlight. A translucent dark wedge drawn over L4 would extinguish the stars in the antisolar sky —
   exactly the region where the darkest, best star field is. This is the one place where the mockup's
   literal SVG order (`sky → MW → stars → wedge → …`) is **wrong for real rendering** and is corrected
   here. `MEASURED` — read in the canvas file, the wedge `<path>` is painted after the star `<g>` at
   `lumintra-night-scene.canvas.tsx:260-268`.
2. **L10 occludes by draw order and nothing else.** No masking, no stencil, no depth. The ridge
   `Path` is filled opaque over everything drawn before it. This is cheap and exact.
3. **L12 splits.** The marker is drawn *before* L10 (so the ridge occludes it, correctly); the label is
   drawn *after* L10 with a clamping rule (§5.4), so a target that sets still has a readable label.

### 1.2 Where the sky becomes a bitmap and where it stays procedural

| Stage | Form | When |
|---|---|---|
| **Twilight radiance** | a **committed binary LUT asset** (`sky_lut.bin`, §2) — **never computed at runtime and never computed on first launch** | build time, `scripts/bake-sky-lut.py` |
| **Runtime, fallback path** | the LUT is materialised as **one 1-pixel-wide `ImageBitmap` strip of `Hpx` rows**, drawn 1:1 into the scene rect | a strip rebuild per scrub frame (§7.3) |
| **Runtime, API 33+ path** | the LUT stays a **texture** (`setInputShader`/`setInputBuffer`) sampled per pixel by AGSL — the gradient remains procedural | per frame, on the GPU |
| **Everything else** | procedural geometry (Compose `Path`, `drawPoints`, `drawVertices`) — never a bitmap except L3 and L6, which are **bitmap assets by nature** | per frame |

**The strip is 1:1 with the scene's device-pixel height, not a 256-row image stretched.** A stretched
strip has to be filtered, and filtering an sRGB-encoded bitmap interpolates in the wrong space —
which is the exact "visible banding" defect B1 forbids. Drawing one row per device row means the
gradient is *evaluated* at every output row and no filtering exists at all. Cost is bounded and
small: the scene is ≈ 1900 device px tall, so a rebuild is ≈ 1900 bilinear LUT samples ≈ 8 k texture
fetches, i.e. microseconds (§10 R5 measures it).

**The asset budget of the whole renderer** (`INFERRED` from the itemised sizes below):
`sky_lut.bin` ≈ **66 KB** + star catalogue ≈ **120 KB** + MW map ≈ **310 KB** + Moon map **447 KB** +
constellation figures ≈ **60 KB** ≈ **≈ 1.0 MB**. This is the answer to H §11's "the build-size
question becomes a non-question": the renderer is **code**, and its data is ~1 MB inside a 20 MB budget.

---

## 2. The twilight LUT

### 2.1 The two axes, exactly

| Axis | Quantity | Range | Step | Samples |
|---|---|---|---|---|
| **A1** | solar altitude `ε` | **+10.0° … −20.0°** | **0.5°** | **61** |
| **A2** | view elevation `h` above the horizon | **0.0° … 90.0°** | **1.0°** | **91** |

Values outside A1 clamp to the end rows; values below `h = 0°` are never sampled (the ground occludes
them, L10). The slider's own range is `+6° … −20°` (§0), narrower than A1's, deliberately: the LUT
carries a **4° overhang** at each end so bilinear interpolation at the extremes has real neighbours
instead of a clamped edge.

**Why two LUTs and not one.** The twilight's most striking structure — the antisolar bands of B3 and
the Earth-shadow wedge of B4 — is *azimuth-dependent*, and a single (ε, h) table has no azimuth axis.
Rather than add a third axis (which would triple the asset and make the baker's output harder to
inspect), the baker emits **a pair of two-axis LUTs in the two vertical planes that bound the
phenomenon**:

* **`LUT_A` — the antisolar meridian**, `Δaz = 180°`. This is where the Belt of Venus, the blue band
  and the Earth's shadow live. **It is the fallback path's entire sky.**
* **`LUT_B` — the solar meridian**, `Δaz = 0°`. This is where the sun's own glow and the brightest
  twilight live. **It is used only on the API 33+ path**, to blend.

At render time the two are blended by azimuth in linear light:

```
w(Δaz)      = (1 + cos Δaz) / 2                 // 0 at the antisolar meridian, 1 at the solar one
L_rel(ε,h)  = lerp( LUT_A(ε,h), LUT_B(ε,h), w )  // bilinear on each LUT, then lerp
sky_linear  = E(ε) · L_rel                       // absolute linear radiance
sky_sRGB    = transfer( tonemap( whitebalance( sky_linear ) ) )
```

The **fallback uses `LUT_A` alone** (`w = 0`) and adds the sun's own glow as a separate additive
radial gradient (L9's sibling, §5.5) — which is precisely what the approved mockup already does: its
sky is a vertical gradient with no azimuthal term at all
(`MEASURED` — `lumintra-night-scene.canvas.tsx:224-228`, a single `linearGradient` with four stops).
The AGSL path gets the extra realism of the azimuthal blend for free. Both paths sample the same two
LUTs, which is the mechanism that prevents colour drift (§8).

### 2.2 The value stored per cell: **linear light, float16, exposure-normalised**

Twilight radiance spans **5–6 orders of magnitude** from daylit sky to astronomical darkness. A
single float16 texture cannot hold that range (float16's normal minimum is ≈ 6.1 × 10⁻⁵), and a
single **uint8 sRGB** texture would band — the defect B1 exists to catch. The contract is therefore:

1. **Per cell, store `L_rel = L_abs / E(ε)`**, where `E(ε)` is the **zenith radiance at that solar
   altitude, in absolute linear units**. Within one column the cell values span roughly 10⁻³…3, which
   float16 holds at ~3 decimal digits — enough, because the eye cannot see 0.1 % differences in a
   smooth ramp, and the dither (§2.6) covers the quantisation of the final bytes.
2. **Store `E(ε)` beside the table** as a **61-entry `float32` array** (`E[0]` is the daylit zenith,
   `E[60]` the −20° zenith).
3. **Store the RGB triple as three `float16` values, linear light, D65 white, premultiplied = no**
   (the sky is opaque; alpha is a wasted channel and is not stored).
4. **The reconstruction is `L_abs = L_rel × E(ε)`.** Nothing else in the pipeline knows about `E`.

**Colour-space discipline, from computation to display — one sentence per stage, no stage skipped:**

| Stage | Space | Where |
|---|---|---|
| Bruneton–Neyret integration (the baker) | **linear spectral → linear CIE XYZ (D65)** | `scripts/bake-sky-lut.py`, build time |
| LUT cells on disk | **linear sRGB primaries, relative to `E(ε)`, float16** | `sky_lut.bin` |
| Interpolation at render (bilinear on A1/A2, then the azimuth `lerp`) | **linear** | the only interpolation in the app that runs on sky colour |
| White balance | **linear**, one fixed 3×3 von Kries matrix, D65 → the app's chosen display white | both paths, one matrix, one constant |
| Tonemap | **linear → display-referred**, the Narkowicz (2015) ACES filmic approximation `L' = L(2.51L + 0.03) / (L(2.43L + 0.59) + 0.14)` | both paths |
| Transfer to code values | **the sRGB piecewise transfer (IEC 61966-2-1)**, not a 2.2 power | both paths |
| Dither | ±0.5/255 tiled blue noise, **added in the encoded domain immediately before quantisation** | both paths |
| Quantisation | **the only 8-bit step in the entire pipeline** | strip `IntArray` / shader output |

On the AGSL path the working space is the **destination's** (Compose's canvas is sRGB), so the shader
must call `toLinearSrgb()` on its inputs and `fromLinearSrgb()` on its result — `ON RECORD`, H §3.2
quoting the `RuntimeShader` reference. Mixing a linear half and an sRGB half inside one AGSL
expression is named there as the trap, and the checklist item #44 asserts the Kotlin half of the same
rule.

### 2.3 How it is produced, and the asset it produces

**Produced by a script, at build time, and committed.** `scripts/bake-sky-lut.py`:

* reimplements **Bruneton & Neyret 2008** — *Precomputed Atmospheric Scattering*, Computer Graphics
  Forum 27(4) 1079–1086, EGSR 2008, DOI 10.1111/j.1467-8659.2008.01245.x — in NumPy: Rayleigh + Mie
  multiple scattering, **plus the ozone (Chappuis-band) absorption term** without which twilight reads
  as "orange-and-grey apocalypse" (`MEASURED`, J §1.3). The published reference implementation is
  **BSD-3-Clause** (`MEASURED`, J's ledger) — which means it *could* be read as documentation, but the
  contract is to work from **the paper**, so the licence question does not arise at all.
* evaluates the model over A1 × A2, in the **antisolar** and **solar** vertical planes;
* writes `apps/<slot>/app/src/main/assets/sky_lut.bin`;
* **writes only inside the repository** — never `%TEMP%`/`%TMP%`/`$TMPDIR`
  (`.cursor/rules/workspace-hygiene.mdc`). The script lives under `scripts/`, so `build.scratch` in
  the `invariants` group already reads it and would fail a `tmpdir()` named as a write target.
* is **reproducible**: the committed `.bin` carries a header with (schema version, A1/A2 bounds and
  steps, the model parameters, the SHA-256 of the generator's own source). `verify.mjs` recomputes
  the expected hash and **FAILs when the committed asset and its generator disagree** — so the
  artifact cannot silently drift from the code that produced it. This is the `verification-discipline`
  rule ("one source of truth, imported — never restated") applied to a binary asset.

**Asset size, computed from §2.1 exactly:**

```
cells per LUT          = 61 × 91               = 5 551
bytes per LUT          = 5 551 × 3 × 2 (f16)   = 33 306
two LUTs                                       = 66 612
E(ε)  61 × float32                             =    244
α_s(ε) 61 × float32 (§5.3)                     =    244
header (version, ranges, hashes)               =     64
──────────────────────────────────────────────────────
sky_lut.bin                                    ≈ 67.2 KB
```

**Never computed on first launch.** A first-launch precompute is refused for two reasons: it costs
seconds on the Exynos 7904 (Bruneton's own precompute was **2.0 s on a 2012 desktop GPU** —
`MEASURED`, J §1.2 / Müller Table 1), and it would make the picture a function of the device, which
is the same defect class the visual-identity rule fails a drifted palette for.

### 2.4 Interpolation at render time

* **Bilinear** in (`ε`, `h`) on each LUT, in float, in linear light. Not nearest, not bicubic.
* **`ε` is interpolated, `h` is evaluated.** The strip path does not interpolate `h` at all (§1.2)
  because it samples one row per device row.
* **The azimuth blend is computed after both LUT samples and before the tonemap**, in linear light.
* **No interpolation is ever done on a gamma-encoded value** — the anti-pattern J §5.3 lists.

### 2.5 The atmosphere's own two functions, tabulated with the LUT

Two scalars are needed per `ε` and are baked into the same file so no runtime computation is needed:

* **`α_s(ε)` — the Earth-shadow edge altitude** (§5.3). 61 floats.
* **`E(ε)` — the exposure** (§2.2). 61 floats.

`Φ(θ)`, the air mass, is **not** tabulated: it is evaluated per star (a handful of thousands of times
per frame, not per pixel), from **Kasten & Young (1989)**, *Revised optical air mass tables and
approximation formula*, Applied Optics **28**(22), 4735–4738:

```
Φ(h) = 1 / ( cos(z) + 0.50572 · (96.07995° − z)^(−1.6364) ),     z = 90° − h
```

valid to `z = 90°`, i.e. down to the horizon — which is where the app needs it, because extinction
and scintillation both peak there.

### 2.6 Banding: the dither is part of the contract, not a polish step

Two independent mechanisms are required, and neither alone suffices:

1. **The gradient is evaluated per output row** (§1.2), so no stretched-bitmap stair-stepping exists.
2. **±0.5/255 tiled blue noise added immediately before 8-bit quantisation.** A **64×64 blue-noise
   tile** generated once at build time, tiled in screen space (so it never crawls during a scrub) and
   added in the encoded domain. `ON RECORD`, J §5.2 / §7 item 8: blue noise is preferred over Bayer,
   and a screen-space-tied pattern avoids motion aliasing.
   **Bayer/ordered dithering is rejected** because its cross pattern is visible in a smooth twilight
   gradient at exactly the scales a full-screen sky produces.

---

## 3. The star renderer

Everything in this section is **reimplemented from Müller, Engel & Döllner (2012)**, *Single-Pass
Rendering of Day and Night Sky Phenomena*, VMV 2012, pp. 55–62, DOI 10.2312/PE/VMV/VMV12/055-062,
read in full in this task (J §1.2, §2.1). Where a constant is *ours* rather than the paper's, it says
so and says why.

### 3.1 What is drawn — and what is not

**Stars are viewpoint-aligned billboards carrying a Point Spread Function, batched by magnitude and
colour. They are never `GL_POINTS`, never `drawCircle` per star, and never a shader loop.** The paper
states point sprites are *"not applicable"* because they are *"unaffected by camera distortion, which
leads to star clustering in the center when using larger FoVs"* (`MEASURED`, quoted in J). The 2025
DLR paper adds the hardware reason (`MEASURED`, J §2.1): hardware point smoothing is vendor-specific,
so the luminance a smooth point contributes is unknowable, and *"small primitives tend to stall the
GPU pipeline"*.

**Compose realisation:** a **separate pre-rendered `ImageBitmap` sprite per (magnitude bin × colour
bucket)**, drawn by **batched `drawPoints` with `BlendMode.Plus`** so overlapping glows add — the HDR
behaviour the paper relies on. Concretely: **8 magnitude bins × 6 colour buckets = 48 sprites**, and
**at most 8 `drawPoints` calls per frame** by batching along the magnitude axis and letting each call
take the colour from the bucket whose sprite it uses… which would need 48 calls. The measured
compromise, stated so it is a decision and not an accident: **6 colour buckets, and within a bucket
all magnitude bins are drawn with one call using per-point `PointMode.Points` and a single sprite** —
because a sprite's *brightness* must vary per star, and `drawPoints` cannot do that with one paint.
The resolution is: **draw the whole star field as ≤ 8 calls, one per magnitude bin, each with its own
alpha and point size, using a neutral-white sprite; and supply the per-star colour by drawing each
*colour* bucket's stars into a small offscreen `ImageBitmap` once per sky-rotation change, then
compositing.** This is the raster-cache of H §7.1 #3, and it is the mechanism that keeps the scrub
cheap. §10 R2 is the risk that the batching is still too expensive, and names the measurement.

### 3.2 Magnitude → brightness (one channel)

Flux ratio to the control magnitude `m_a` (the paper's own relation, `MEASURED`, J §2.1(b)):

```
Δm(m)  = 2.512 ^ (m_a − m)                 // = 10^(−0.4 (m − m_a))
```

`m_a = 4.0` — the paper's calibrated control magnitude. Stars **fainter than** `m_a` carry their
brightness entirely in the core's amplitude; stars **brighter than** `m_a` additionally grow a glare
halo (§3.4).

The star's final screen intensity:

```
I(m, h, t) = c_q · Δm(m) · O(h) · S(h, t) · a_dark(ε)
```

| Term | Formula | Origin |
|---|---|---|
| `c_q` | calibration constant, chosen so a `V = 6.5` star at `h = 60°` at ε = −15° renders at 1/255 above the local sky — i.e. **at the edge of visibility**, which is what `V = 6.5` means for a dark-adapted eye | **ours** (`INFERRED`); the paper's `c_q ≈ 4 × 10⁻⁷` is in a different radiometric unit and is not transferable |
| `O(h)` | extinction, §3.5 | paper's out-scattering factor |
| `S(h,t)` | scintillation, §3.6 | paper's scintillation term |
| `a_dark(ε)` | `smoothstep(−2°, −9°, −ε)` — 0 while the sky is brighter than ε = −2°, 1 from ε = −9° | **ours**; the paper blends by sun altitude, this is the concrete curve |
| colour | §3.7 | paper's pipeline |

### 3.3 Magnitude → radius (the second, deliberately small channel)

```
r_core(m) = max( sqrt(2) px , 0.5 dp )                     // the paper's minimum: q ≥ √2 px, "to eliminate flickering due to aliasing"
```

**The core radius is a constant for every star.** On the A40 (480 dpi = 3 device px per dp) this
resolves to `max(1.414, 1.5) = 1.5` device px; on a 1× device, 1.414 px. This is the design that
makes B7 true — *"a few bright with glow, many faint sub-pixel"* — and it is why the *brightness*
channel above carries the magnitude, not the size. Uniformly scaling size with magnitude is the
documented failure mode (`ON RECORD`, J §2.1(c)): *"uniformly scaling the size makes the faintest
stars too bulky"*.

**The glare halo is the only place a radius grows**, and it is gated on the control magnitude:

```
I_G        = max( 0 , Δm(m) − 1 )                // zero exactly at m = m_a
r_glare(m) = max( r_core , c_k · sqrt(I_G) )     // the paper's k = max(q, c_k·√I_G) form
c_k        = 0.50 px                              // OURS (§3.4)
```

### 3.4 The PSF, and the halo threshold

The rendered radial profile is the sum of two normalised profiles, evaluated on the sprite:

```
P(l) = A_core · T(l)  +  A_glare · G(l)          // l = distance / billboard radius ∈ [0,1]
T(l) = 2 l³ − 3 l² + 1      for l ∈ [0,1], 0 outside     // the paper's intensity PSF, volume V_T = 1.167
G(l) = (1 − l²)²                                          // OURS — see below
```

* `T` is **the paper's published intensity PSF**, used verbatim (`MEASURED`, J §2.1(b)).
* `G` is **ours**. The paper's glare profile is printed ambiguously in the OCR (`G(l) = √64·l`) and the
  source review left the exact constant `UNVERIFIED` (J §9). A profile that *increases* with distance
  from the centre is not a PSF, so the shape as transcribed cannot be right, and inventing a constant
  for a mis-read equation would be the "plausible default wearing a fact's clothes" the grounded-claims
  rule bans. `G(l) = (1 − l²)²` is chosen instead because it is band-limited (its value and first
  derivative vanish at `l = 1`, so a sprite has no visible edge), which is the property that matters
  for a halo, and it is **labelled as our substitute rather than as the paper's formula**.
* `c_k = 0.50 px` is **calibrated, not quoted**: `I_G` for Sirius (`V = −1.46`) is
  `2.512^{5.46} − 1 ≈ 151`; `√151 ≈ 12.3`; `c_k = 0.5` gives **≈ 6 px**, which is the halo radius a
  1080-px-wide 73° field should show for the brightest star in the sky. The calibration is falsifiable
  and checklist item #8 states it.

### 3.5 Extinction — dimmer *and* redder toward the horizon

Per channel, using the paper's Rayleigh out-scattering coefficients **β_r = (0.16, 0.37, 0.91)** for
sea level (`MEASURED`, J §2.1(e)) and Kasten–Young air mass (§2.5):

```
O_c(h) = exp( −β_r[c] · ( Φ(h) − 1 ) )          c ∈ {R, G, B}
```

The `− 1` makes `O = 1` at the zenith (air mass 1) so extinction is *relative to overhead*, which is
what the eye compares. At the horizon (`Φ ≈ 38`):
`O = (3.5 × 10⁻³, 3.1 × 10⁻⁶, 3.3 × 10⁻¹⁵)` — the blue channel is annihilated and the red survives
150× better than the green, which is the reddening B9 asks for. **The effect is capped at `h ≥ 1°`** so
a star exactly on the ridge does not produce a denormal range.

### 3.6 Scintillation — twinkle that grows toward the horizon

The paper: scintillation is proportional to the optical path length, which *"strongly increase[s]
towards the horizon"* (`MEASURED`, J §2.1(d)). The contract:

```
σ(h) = s_0 · ( Φ(h) − 1 )^1.5            // amplitude; exponent OURS, s_0 OURS
S(h,t) = 1 + σ(h) · n_i(t)               // per-star multiplicative brightness
n_i(t) = (1 − a) · n_i(t−Δt) + a · u      // a 1-pole low-pass, a = Δt/(Δt + τ), τ = 60 ms, u ∈ N(0,1)
```

* `s_0 = 0.12`, calibrated so the amplitude at `h = 5°` (`Φ ≈ 10.3`) is **≥ 3×** the amplitude at
  `h = 60°` (`Φ ≈ 1.15`) — the falsifiable criterion of checklist item #10.
* The 60 ms correlation is what makes it read as *twinkle* rather than television static; a
  zero-correlation per-frame noise at 60 Hz looks like sensor noise, not atmosphere.
* **A positional jitter of ±0.15 px** at the same rate accompanies it — a real twinkle wobbles.
* **Twinkle is a time phenomenon and therefore only exists while frames are being produced.** In an
  idle scene there is no frame clock (§7.4), so a still scene has still stars. This is correct and it
  is a battery decision, stated here so nobody later "fixes" it by adding a clock.

### 3.7 Colour from temperature

The paper's chain, reimplemented (`MEASURED` for the chain's shape, J §2.1(f)):

```
B−V  →  T_eff  →  Planckian locus (x,y)  →  CIE xyY  →  linear sRGB (D65)
```

with **Ballesteros (2012)**, *New insights into black bodies*, EPL **97**(3) 34008, for the first step:

```
T_eff = 4600 K · ( 1 / (0.92·(B−V) + 1.7)  +  1 / (0.92·(B−V) + 0.62) )
```

and the locus from **Krystek (1985)**, *An algorithm to calculate correlated colour temperature*,
Color Research & Application **10**(1) 38–40 (the paper's own citation), with the **Kim et al. (2002)**
cubic as the fallback if the `xy` fit is not monotone at the range's ends. Only **6 colour buckets**
ship (the sprite atlas of §3.1), so the continuous `T_eff` is quantised at bake time into 6
linear-sRGB values — one per bucket — and the bucket edges are chosen by population, not by equal
temperature.

**Two honest limits.** First, **FK6's photometric columns were not read in this session** — whether it
carries `B−V` for all 4,150 stars is `UNVERIFIED` (check: read the GAVO cone-service response header
for `FK6` and count non-null `B−V`). Second, the 2MASS faint field carries **J/H/K, not V**, so its
colour must be **modelled** from the IR colours and its magnitude **mapped** to an approximate V —
which I §1.3 already flags as a step the builder must own and label in-app as an approximation, not as
measured V.

### 3.8 A realistic star count, without a per-pixel catalogue evaluation

**The catalogue is never evaluated per pixel — it is never evaluated per *pixel* at all.** The chain:

1. **Build time.** Merge **FK6 (CC0 1.0)** for the bright, high-accuracy end with **2MASS** (public-domain
   style, acknowledgement required) for the faint field. Bin every star into **0.5-mag bins from
   `V = −1.5` to `6.5` → 16 bins**. Write a binary catalogue into
   `app/src/main/assets/stars.bin`: per star **RA, Dec (2 × float32 = 8 B)**, **V (float16, 2 B)**,
   **colour-bucket index (uint8, 1 B)** = **11 B/star**. At ~9,000 stars that is **≈ 99 KB**;
   **proper motion is deliberately not carried** — over one night it is far below one pixel.
   *Licence:* FK6 is `CC0 1.0` (no attribution required), 2MASS requires one verbatim acknowledgement
   string, both per I §1.2; **HYG, Hipparcos, Tycho, Gaia and APASS are forbidden** (`CC BY-SA 4.0` /
   `CC BY-NC 3.0 IGO`, I §1.1) and the build must not reach for them.
2. **Per frame, CPU.** One 3×3 rotation `M = R_horizontal · R_ICRS→horizontal` for the frame's
   `(place, time)` — the paper's Eq. 7 form `R_s = R_y(δ − π/2)·R_z(−LMST)·P` (`MEASURED`, J §2.2).
   Then per star: `v_screen = project(M · v_icrs)`, **cull by horizon** (one array lookup, §5.4),
   **cull by the scene rect**. This is ≈ 9,000 × ~20 flops ≈ 0.2 Mflop, which is nothing; the cost
   that matters is the *draw*, and that is what the ≤ 8 batched `drawPoints` calls bound.
3. **Density.** The default 73° × 53° field is `1.18 sr` of `4π sr`, i.e. **9.4 % of the sky**, so a
   full-sky count of ~9,000 to `V = 6.5` renders as **≈ 850 stars** — the honest naked-eye figure, and
   the range checklist item #13 asserts (700–1,100). The 2MASS-supplied faint field beyond 6.5 is
   **not drawn by default**: it appears only when the user narrows the lens FOV, which is the
   "densify on zoom" step. `INFERRED` from the two counts and the field's solid angle.
4. **Draw.** ≤ 8 batched `drawPoints` (§3.1), plus the offscreen colour-bucket raster of H §7.1 #3,
   rebuilt only when the sky has rotated past a threshold.

---

## 4. The Milky Way composite

### 4.1 The asset, and its licence

**`ESO eso0932a`, "The Milky Way panorama"** — **CC BY 4.0**, credit **`ESO/S. Brunier`, mandatory and
visible** (`MEASURED`, I §5: *"Crediting this image with the full credit line, in a visible way is
MANDATORY"*). The source is 6000 × 3000. The mockup already names it, and correctly
(`MEASURED` — `lumintra-night-scene.canvas.tsx:495`, `"ESO eso0932a"` / `"CC BY 4.0 · кредит
обязателен"`).

**Bake, do not ship raw.** A build-time step produces `milkyway.webp` (2048 × 1024, **≈ 310 KB**) with:

* the source **reprojected to equirectangular galactic coordinates `(l, b)`**. The source's own
  projection must be confirmed before the resample (the widely reused ESO panorama is a
  **Hammer–Aitoff**-derived all-sky image, and the kam-ui implementation this recon already read
  describes a *"Hammer → equirectangular reprojection"* — `ON RECORD`, J §3(a)). **Which projection
  `eso0932a` itself uses is `UNVERIFIED`** — check: open the ESO page's own technical note or compare
  the image's frame against a known constellation, and record the answer beside the asset.
* **bright stars removed and inpainted.** Every catalogue star with `V < 3.0` (≈ 200 stars) is masked
  in the equirectangular map and inpainted from its neighbourhood. This is the paper's own instruction
  (`MEASURED`, J §3.2, quoted: *"bright stars should be removed though, to avoid star doubling"*), and
  the checklist item #15 is the test.
* a **fixed 3 × 3 box blur** applied only to the region below `V ≈ 8` equivalent — enough to make the
  unresolved starlight read as a band. Not a Gaussian: a box keeps the Rift's edges crisp at the one
  scale the phone shows. (J §3.1 measures that at 1080 × 2340 the band is 150–400 px tall, so
  large-scale structure dominates and per-star grain is below the eye's resolution.)
* mipmaps generated for the mesh's LOD selection.

### 4.2 Placement — this is a real coordinate transform, specified exactly

The texture is a map in **galactic** coordinates. To place it, three rotations compose, and the app
already has the mathematics for the last two:

```
M_gal→screen = R_horizontal(φ, LST)  ·  R_gal→ICRS
```

**(a) `R_gal→ICRS`** is the fixed matrix from the **IAU galactic coordinate system**, whose pole and
origin are defined by **Blaauw, Gum, Kapteyn, Weigelt et al. (1960)**, *The new I.A.U. system of
galactic coordinates*, MNRAS **121**, 123, and whose standard numerical form is published in the
**Hipparcos and Tycho Catalogues (ESA 1997), Vol. 1, §1.5.3**:

```
R_gal→ICRS = [ −0.0548755604  −0.8734370902  −0.4838350155 ]
             [  0.4941094279  −0.4448296300   0.7469822445 ]
             [ −0.8676661490  −0.1980763734   0.4559837762 ]
```

The build **re-derives** this matrix from the published pole `(α = 192.85948°, δ = +27.12825°)` and
the galactic-centre position angle `l_Ω = 32.93192°` and **asserts agreement with the tabulated matrix
to 1 × 10⁻⁶** in a unit test — so the constant in the source is a checked claim, not a copied number.

**(b) `R_horizontal(φ, LST)`** is the paper's own `R_s = R_y(δ − π/2) · R_z(−LMST) · P` form, with `φ`
the observer's latitude and `LST` the local sidereal time the ephemeris core already computes.

**(c) The screen projection** is the same function the stars use, so the Milky Way and the star field
cannot disagree about where the sky is. That shared function is the entire reason the band lines up
with the real galactic plane.

### 4.3 Drawing it: a coarse textured mesh, not a per-pixel sphere lookup

The transformation is **not affine** — a single rotated `drawImage` would distort the band by several
degrees across a 73° field, which is visible on the Rift. The contract is a **coarse mesh**:

* Split the equirectangular texture into a **48 × 24 grid of quads** (1,152 quads).
* Compute each quad's four corners in screen space through §4.2's exact transform.
* Draw with **`drawVertices`** and an `ImageShader` on the paint — bilinear interpolation across each
  quad, which at 48 × 24 is far finer than the band's own smoothing.
* **Cull quads whose corners are all outside the scene rect** and quads entirely below the horizon
  (§5.4), so the mesh's fill cost is proportional to what is on screen, not to the sphere.
* Where the mesh crosses the scene's edge the vertices are **clipped in screen space** before drawing,
  so a quad spanning the boundary cannot get a degenerate corner.

### 4.4 Opacity follows the darkness

The Milky Way is **invisible in twilight** — the sky background is far brighter than the band. So it
fades in with a curve on the same variable as everything else:

```
a_mw(ε) = smoothstep( −8° , −14° , −ε )          // 0 at ε = −8°, 1 from ε = −14°
```

At ε = −8° (nautical twilight) it is zero; at −14° (approaching astronomical darkness) it is full.
`a_mw` multiplies the mesh's paint alpha. **Item #16 of the checklist asserts both endpoints**, so the
band cannot silently leak into a sunset.

### 4.5 Why the stars do not double on top of it

Two mechanisms, and **both are required**:

1. **The bright stars are not in the texture** (§4.1) — masked and inpainted at bake time.
2. **The texture and the sprites are rotated by the same matrix.** The same `R_horizontal` builds the
   star positions (§3.8 step 2) and the mesh corners (§4.2), so a masked-out star's sprite lands
   within **≤ 1 px** of the hole it left. A mismatch would be a positional doubling, which is precisely
   the failure the paper names.

The faint stars *are* in both — the texture's blurred unresolved starlight and the catalogue's
`V > 6.5` field are not drawn by default (§3.8 step 3), so there is no second population to double.

---

## 5. The horizon

### 5.1 The data format of the user's drawn silhouette

**A per-azimuth altitude array, not a free polyline.** The user draws by dragging the ridge in the
scene, which emits **one altitude per azimuth** — because the scene's projection is single-valued in
azimuth by construction, the editor cannot produce a folded or self-intersecting silhouette. That
choice is what makes occlusion an `O(1)` array lookup instead of a scan.

```kotlin
@Entity
data class HorizonProfile(
    @PrimaryKey val id: Long,
    val name: String,
    val latitudeDeg: Double,        // the place it was drawn at, for orientation
    val longitudeDeg: Double,
    val altitudeDeg: FloatArray,    // 360 samples, azimuth 0..359° (geographic north, clockwise),
                                    // altitude in degrees, range −10.0 … +90.0
    val createdAt: Long,
)
```

**Storage:** the array is **`ByteArray(720)`** — 360 × `int16` in **0.01°** units — in a Room `BLOB`.
0.01° is 36 arc-seconds, which at a 73° / 1080 px field is **0.03 px**: finer than the display can
resolve, and it halves the storage versus `float32`. One profile is 720 B; a hundred are 72 KB.
The mockup's own ridge is a 13-vertex polyline (`MEASURED` —
`lumintra-night-scene.canvas.tsx:203`), which is exactly the kind of authoring input that resamples
into this array; the array, not the polyline, is what ships.

### 5.2 Rendering the silhouette

1. For `az = 0 … 359`, compute `screen_i = project(az_i, altitudeDeg[i])`.
2. Build one `Path`: the 360 projected points, then two corners at the scene's bottom edge, closed.
3. Fill **opaque**. The fill is a **vertical gradient from `#04060C` at the ridge to `#01030A` at the
   bottom**, not a flat colour — the ground is not a paper cut-out, and the gradient is 2 stops, so it
   costs nothing.
4. Optional tree glyphs are **part of the silhouette path** (drawn in the same fill), never a second
   colour. The mockup's triangles (`MEASURED`, `:305-314`) become a build-time or draw-time
   perturbation of the ridge array, which keeps occlusion a single array.

### 5.3 Occlusion, per object class

| Object | Mechanism | Cost |
|---|---|---|
| **A star** (L4) | `if (h_star < altitudeDeg[round(az_star)] ) cull` — **before** it enters the batch | one array read per star |
| **A deep-sky target** (L5, L12) | same test. The **marker is drawn before L10** (occluded); the **label is drawn after L10** and is *clamped*: if the target is below the ridge the label is anchored to the ridge line and prefixed with `↓` | one array read |
| **The Moon** (L6) | a partially-set Moon must be *cut*, not culled: L6's disc is drawn inside `clipPath(ridgeInverse)` when `h_moon − r_moon < altitude[az_moon]` | one clip layer, only when low |
| **The satellite** (L7) | the trail polyline is **split at every horizon crossing**: for each segment, solve for the `t` at which `h(t) = altitude[az(t)]` (bisection, ≤ 8 iterations, ≤ 12 segments) and emit only the above-ridge pieces | ≤ 12 solves per frame |
| **The FOV frame** (L11) | **not occluded.** It is a viewfinder; a viewfinder that a hill cuts in half is a bug, not realism | — |
| **The Milky Way mesh** (L3) | quads whose four corners are all below the ridge are culled; the rest are drawn under L10 and the ridge occludes them anyway | corner test per quad |

### 5.4 The Earth-shadow wedge — derived, geometry-driven, and it moves ~8× the sun

**The physical object.** The Earth's shadow is the region where the line of sight's air is in the
Earth's own shadow. Its lower edge is not a direction but a **distance**: along a ray at elevation
`α`, in the antisolar vertical plane, the air is shadowed from the observer out to

```
t₂(α, ε) = R · (1 − cos ε) / sin(α − ε)          for α > ε        (derived, §5.4 note)
```

so the *visible* shadow edge is the elevation at which that shadow depth falls to the top of the
optically significant atmosphere, `H = 20 km`:

```
              ⎧ 90°                                                    if (R/(2H))·ε_rad² ≥ 1
α_s(ε)  =     ⎨ ε + arcsin( (R / (2H)) · ε_rad² )                      otherwise
              ⎩ ε                                                      (floor — the edge never sinks below ε)
```

**The derivation, in three lines, so it can be checked rather than trusted** (`INFERRED` — derived in
this session from the geometry; the *qualitative* behaviour is `ON RECORD` from the antitwilight paper):

1. With the observer at the surface, the antisolar direction at elevation `α`, the Earth's centre `R`
   below, and the Sun's rays parallel at depression `ε`, the ray–sphere tangency condition
   `dist²(C, ray) = R²` reduces to `t² sin²(α−ε) + 2Rt cos ε sin(α−ε) − R² sin²ε = 0`, whose positive
   root is `t₂ = R(1 − cos ε)/sin(α − ε)`. The shadow therefore exists only for `α > ε`, and its depth
   `t₂` grows without bound as `α → ε⁺`.
2. Setting `t₂ = H` gives `sin(α_s − ε) = R(1 − cos ε)/H`; with `1 − cos ε ≈ ε²/2`, that is
   `α_s ≈ ε + arcsin( (R/2H)·ε² )`.
3. **Numerically, with `R = 6371 km`, `H = 20 km`:** `α_s = 4.0°` at `ε = −1°`, `13.2°` at `−2°`,
   `28.9°` at `−3°`, `54.9°` at `−4°`, **above the zenith** from `ε ≈ −5°` on. Over the `−1° → −4°`
   window the edge rises ≈ 17° per 1° of Sun motion — the *"increases much faster than ε and approaches
   the zenith while ε is still in single digits"* that the antitwilight paper states
   (`MEASURED` as a quote, J §4.2), with B4's "≈ 8×" satisfied and exceeded.

**How it is applied — and it is not an overlay (§1.1).** `α_s(ε)` is baked into `sky_lut.bin`
(61 floats, §2.5) and used at render time only inside L1's evaluation: for a view direction with
`Δaz > 90°` (the antisolar half) and `h < α_s(ε)`, the sample is taken from a **shadow column of
`LUT_A`** — i.e. the baker emits the antisolar LUT with the shadow already applied, and the shadow's
effect is a **blue-shifted darkening** (the "blue band" of B3), not a neutral darkening, because the
surviving light there is longer-path rayleigh-scattered blue.

**The Belt of Venus** sits immediately **above** `α_s` and moves with it. It is a warm band:

* **present from ε ≈ −6°**, redder than blue from ε ≈ −2° (both `MEASURED`, J §1.4);
* **width** `w(ε)` shrinking from ≈ 8° to ≈ 0.9° as ε → 0 — the paper's own measured numbers, and the
  contract takes them as the band's width endpoints, interpolated linearly in ε;
* **amplitude** ∝ `max(0, (ε + 6°)/4°)`, capped at 1;
* **implemented as part of the bake** for `LUT_A` (it is a *meridian profile* feature, so a two-axis
  LUT can hold it), **not** as a runtime overlay.

### 5.5 Alpenglow — the warm backscatter that hugs the ridge

The Belt of Venus **is** twilight alpenglow (`ON RECORD`, J §4.3 quoting Wikipedia: *"the Belt of Venus
is actually alpenglow visible near the horizon during twilight… the backscatter of reddened
sunlight"*), so the two share a source and must not be drawn twice. The division of labour is
geometric, not aesthetic:

* **The Belt of Venus** is the band in the **antisolar** sky, baked into `LUT_A` (§5.4).
* **Alpenglow over the ridge** is the same backscatter seen **toward the Sun**, where it is brightest,
  and it must **feather over the silhouette's top edge** because the light scatters in the air in front
  of and behind the ridge (J §4.3).

```
A_alp(ε, az, y) = A_peak · f(ε) · (0.35 + 0.65 · (1 + cos(az − az_sun))/2) · K(y)
f(ε)            = exp( −((ε + 1.0°) / 4.0°)² )        // peaks at ε = −1°, ≤ 25 % of peak by ε = −8°
K(y)            = 1 for y above the ridge, and (1 − d/8px)² for d px **below** the ridge top
```

`K` is what makes it *feather over* the ridge rather than stop at it: the band is drawn **before** L10
and is allowed to be overdrawn by nothing, because it is clipped only at `ridge − 8 px` in screen
space. Its colour is a warm red-pink, drawn **additively** on top of whatever L1 produced.

**It is a `Brush.radialGradient`, deliberately — not a blur.** The approved mockup draws it as an
ellipse through an SVG `feGaussianBlur` (`MEASURED` — `lumintra-night-scene.canvas.tsx:274-281` and
the `blur-` filter at `:229-231`). **The app must not**, for two reasons: (a) `Modifier.blur` /
`RenderEffect` is documented for **API 31+**, and the A40 is API 30 — this is `UNVERIFIED` against
the pinned Compose BOM (`compose.ui` 1.12.0 / Material3 1.4.0) here, and the check is to run a blur
on the A40 and observe; (b) a full-screen gaussian is exactly the fill-rate trap §10 R1 names on a
2-core Mali. A `Brush.radialGradient` whose stops encode the same falloff is **exact, allocation-free
and cheap**, and it produces the same picture. This substitution is a contract, not a preference: the
scene's soft edges are gradients, never blurs.

---

## 6. The timeline strip

### 6.1 What it is

The strip below the scene, 268 × 7 dp in the mockup (`MEASURED` —
`lumintra-night-scene.canvas.tsx:415-431`), is **not a decorative key and not a second palette**. Its
colour at any position is **the computed sky at that moment**, which is the mockup's own stated idea
(`MEASURED`, `:559-561`: *"Цвет каждой плитки — посчитанное небо в этот момент"*). The palette **is**
the light, and that is the product's visual-identity claim
(`.cursor/rules/visual-identity.mdc`: *"the palette is one source of truth, shared with the app"*).

### 6.2 Sampling, exactly

```
strip(i) = sRGB( tonemap( E(ε_i) · LUT_A( ε_i , h = 12° ) ) )       i = 0 … 95
ε_i      = the solar altitude at scrub position  i / 95
```

* **96 stops**, `i = 0 … 95` — the mockup uses 60 (`MEASURED`, `:419`), and 96 is chosen because the
  strip is 268 dp ≈ 804 device px wide on the A40, so 96 stops can be drawn 1:1 with no visible stair
  at the dawn end where the colour changes fastest. (Both numbers are `INFERRED` from the widths; a
  measured capture can settle it, and it is a one-constant change.)
* **`h = 12°`**, a single fixed elevation, and it is the *antisolar* meridian — i.e. `LUT_A`. That is
  the band where the four twilight bands of B3 are most legible, so the strip reads as *"the sky
  telling you what time it is"*. Using the zenith would give a nearly monochrome strip; using `h = 0°`
  would give the horizon band of B2, which is the *dimmest* part and would read as muddy.
* **The same tonemap, the same white balance, the same `E(ε)`, the same transfer** as L1. There is
  exactly one sky-colour function in the app and the strip is a second call site of it — not a copy.

### 6.3 How the scrub updates it

* **The strip is baked once per (place, date)** — 96 cells, drawn into a **96 × 1 `ImageBitmap`** at
  `ColorSpace.Srgb`, then stretched horizontally with `FilterQuality.Medium` (a *horizontal* stretch
  of a 1-row image is a 1-D interpolation along the time axis, which is the correct space for it
  because the time axis is perceptual, not photometric — this is the one place where filtering an
  sRGB bitmap is the right thing, and it is stated so it does not look like an oversight).
* **The scrub does not regenerate the strip.** It moves a **thumb** drawn as a separate overlay
  (`drawCircle` at `x = 16 + s·268`, the mockup's own construction at `:431-432`) and updates the
  clock/phase text. So a drag costs **zero strip work**.
* **When `place` or `date` changes** the night's `ε(s)` curve changes, and the strip is regenerated
  once. That regeneration is ≤ 96 `LUT_A` samples plus the tonemap: **≈ 3 µs**, which is why it can
  happen on the main thread without a visible cost.
* **The phase labels are text, not colour.** `Закат / Гражданские / Навигационные / Астрономические /
  Полная тьма` are computed from the standard `−6° / −12° / −18°` thresholds
  (`ON RECORD`, J §1.4) as *labels*, never as keyframes — J §5.2's rule that a discontinuity at a
  stage boundary is the exact "form with a button" defect. The mockup's own `phaseLabel` already does
  this correctly (`MEASURED`, `:90-97`).

---

## 7. The scrub interaction

### 7.1 The state model

```kotlin
// rare, structural: changing any of these rebuilds everything
@Immutable data class SceneKey(
    val placeId: Long, val epochMillis: Long, val fovDeg: Float, val targetId: String?,
    val horizonId: Long,
)

// per-frame, linear: READ ONLY INSIDE THE DRAW LAMBDA
val scrub: MutableFloatState = mutableFloatStateOf(0.5f)
```

* `SceneKey` lives in a `mutableStateOf` — a change recomposes and relayouts, which is correct because
  the scene's *structure* changed.
* **`scrub` is a `MutableFloatState` read only inside `Canvas { … }` / `drawBehind`**, so a drag
  invalidates **only the draw phase**: no recomposition, no relayout. This is the documented Compose
  phases behaviour (`MEASURED`, J §5.1, quoting developer.android.com: *"State reads during drawing
  code affect the drawing phase… Compose UI runs only the draw phase"*), and it is the difference
  between the published 238-recomposition case study and a 60 fps drag.
* Everything downstream of `scrub` is a **pure function**: `SkySnapshot = Sky.ephemeris(key, scrub)`.
  It returns positions, `ε`, `Φ`, the moon's phase and the sun's azimuth, and it is **unit-testable
  without a GPU** — the "new, small, testable surface" H §11 asks for. `derivedStateOf` is used **only
  to coarsen** (`which phase label`), never for the sky itself (J §5.1).
* All heavy drawing objects — `Path`s, `Brush`es, the sprite atlas, the `ImageBitmap`s — are built in
  **`drawWithCache`**, which keeps them alive while the size is unchanged (`MEASURED`, J §5.1). **No
  allocation happens inside the draw lambda.** This is the single most important jank rule in the
  contract.

### 7.2 Why the drag must not be eased

Material's own guidance for a scrubber is that the thumb tracks the finger 1:1; easing is for
**release, programmatic change and state feedback** (`MEASURED`, J §5.2, quoting the motion guidance:
*"do not animate the drag itself"*). The concrete realisation:

```kotlin
Modifier.pointerInput(widthPx) {
  detectDragGestures(
    onDrag = { change, delta -> change.consume(); scrub.value = (scrub.value + delta.x / widthPx).coerceIn(0f, 1f) }
  )
}
```

No `animateTo`, no `Animatable` in the drag path — the value written is the value drawn **in the same
frame**. Easing is applied **only** on release (a **settle** of ≤ 200 ms) and on a **tap-to-advance**
(`short4`/`medium2`, `emphasized-decelerate` `cubic-bezier(0.05, 0.7, 0.1, 1)`, the M3 expressive
scheme's tokens — `MEASURED`, J §5.2). The reason it matters beyond taste: an eased drag makes the sky
lag the finger, and a lagging sky stops feeling like a physical thing the user is moving — which is
the whole premise of the hero screen.

### 7.3 What is per-frame versus imperative

| Work | Frequency | Why |
|---|---|---|
| `Sky.ephemeris(key, scrub)` | **every frame of a drag** | it is the input; it is ≈ 50 µs of trigonometry |
| Rotate + cull the star catalogue | **every frame** | ≈ 9,000 × 20 flops; cheaper than tracking a "did it move enough" predicate |
| Rebuild the 1 × `Hpx` sky strip | **every frame** | ≈ 1,900 LUT samples, no allocation (`ImageBitmap.writePixels` into a pre-allocated bitmap) |
| Rebuild the star colour-bucket raster | **only when the sky has rotated > 0.25°** | the expensive one; ≈ 0.25° corresponds to ≈ 1 minute of rotation, so a full-night drag rebuilds it ≈ 570 times rather than 3,600 |
| Rebuild the Milky Way mesh vertices | end of drag + rotation > 0.25° | 1,152 corners × 2 projections |
| Rebuild the timeline strip | **on `place`/`date` change only** | §6.3 |
| Moon texture upload | **never at runtime** — one bitmap, decoded once | — |
| Any `Brush`/`Path` construction | **never at runtime** — `drawWithCache` | §7.1 |

### 7.4 The repaint trigger, and the battery consequence

**The trigger is the input, not a frame clock.** There is **no `withFrameNanos` loop** and no
`rememberInfiniteTransition`. A frame is produced when (a) `scrub` changes, (b) `SceneKey` changes,
(c) the orientation changes, or (d) a bounded settle animation is running. J §7.1 decision 4 states
this and it is the largest battery decision in the app; this contract makes it concrete:

* **An idle scene produces zero frames.** The checklist item #30 asserts it against
  `dumpsys gfxinfo`'s frame counter, not against the source.
* **A drag produces ≈ 60 frames/s on the A40's 60 Hz panel**, each ≈ 10–12 ms.
* **A settle produces ≤ 200 ms of frames** and then stops.

**The battery consequence, split into what is reasoned and what is measured:**

| Statement | Label |
|---|---|
| A continuously animating full-screen composited scene is the most expensive mode available, and a 3100 mAh cell makes it visible | `ON RECORD` (H §9.2, from the measured capacity) |
| The scene is predominantly dark and the A40's panel is Super AMOLED, where near-black pixels draw near-zero power | `ON RECORD` (device specification) + `INFERRED` (OLED emission) — **not measured on this device for this app** |
| Costing most of the 2.53 M pixels at 60 Hz, even in a 25 %-lit scene, will be measurable in `batterystats` | `INFERRED` |
| The **numbers** — %/hour for a scrub vs. %/hour idle — | **`UNVERIFIED`**. §10 R8 names the exact command and the acceptance threshold. |

### 7.5 The frame budget, named against the device

| Quantity | Value | Why |
|---|---|---|
| Panel refresh on the A40 | **60 Hz → 16.67 ms** | device specification (`ON RECORD`, H §9.1) |
| **Budget for a scrub frame** | **≤ 12 ms** (CPU + GPU together) | leaves 4.7 ms of headroom for Compose's own draw and the OS; a frame that consumes the whole interval has no room for a hiccup |
| **Target p95 frame time** | **≤ 16.7 ms** | ≥ 95 % of frames land in a refresh interval |
| **Target p99 frame time** | **≤ 33 ms** | at most one dropped frame, rarely |
| **Target jank rate** | **< 1 %** of frames over 16.7 ms in a 30 s scripted drag | this is the number `dumpsys gfxinfo` prints as "Janky frames" |
| **Idle frame production** | **0 frames/s** | §7.4 |
| **First-frame latency after a drag begins** | **≤ 33 ms** | the hand must not perceive a lag between touching and moving |

**These are targets, not measurements.** No frame-time reading exists for this scene on this phone
(H §9.3: *"no frame-time, GPU-utilisation, thermal or battery measurement for a full-screen composited
sky scene on this device"*), the checklist items #30–#32 are the gates, and §10 names the commands.

---

## 8. The AGSL enhancement and the fallback

### 8.1 The exact relationship

| | Fallback (API 26–32, **the A40's path**) | Enhancement (API 33+) |
|---|---|---|
| Choice | `Build.VERSION.SDK_INT < 33` | `>= 33` |
| L1 implementation | 1 × `Hpx` `ImageBitmap` strip, CPU-built from the LUT, drawn 1:1 | `RuntimeShader` over the scene rect, sampling the same LUTs via a uniform shader |
| Azimuth | `w = 0` (`LUT_A` only) + a separate additive sun-glow gradient | `w(Δaz)` blends `LUT_A`/`LUT_B` per pixel |
| Tonemap / white balance / transfer / dither | Kotlin, once | AGSL, once — **the same three functions, transcribed** |
| Everything in L2–L13 | **identical**, the same code | **identical**, the same code |

**AGSL is additive, never load-bearing** (H §7.1 decision 2). The concrete acceptance is checklist
item #40: deleting the shader source leaves the app compiling and the screen unchanged in structure.

**AGSL never touches the stars.** It cannot: loops must be unrollable with a constant bound, so a
catalogue cannot be evaluated per pixel, and arrays are 1-D, fixed-size and indexable only by a
constant or a loop variable (`MEASURED`, H §3.2, quoting the AGSL quick reference). The shader's whole
job is the sky field.

### 8.2 How colour drift between the two is prevented

Three mechanisms, in increasing strength:

1. **One source of truth.** Both paths sample `sky_lut.bin`. Neither has its own palette, its own
   stops, or its own tonemap curve. The mockup's hand-authored `SKY` stop list
   (`MEASURED`, `lumintra-night-scene.canvas.tsx:31-41`) is a *mockup* device and is **not** shipped —
   the shipped palette is the LUT, and the app's `Color.kt` holds the LUT's endpoint samples (§checklist
   #33).
2. **One implementation of the last three stages.** `tonemapACES`, `whiteBalance`, `srgbEncode`,
   `dither` are declared once in Kotlin and once in AGSL, and a **unit test transcribes both to a
   single reference and compares** — the AGSL string is the literal source of the shader, so the test
   reads the shipped text, not a copy of it.
3. **A golden-image test in a debug build.** The debug build renders the sky **twice for the same
   input** — once through the CPU path (evaluated at every row of a test grid) and once through the
   shader, into two offscreen bitmaps — and **fails if any channel differs by more than 2/255** over
   256 elevations × 61 sun altitudes. This is checklist item #35, it runs on an API 33+ device or an
   emulator in CI only, and it is the mechanism that makes "the two paths agree" a *checkable* claim
   rather than a promise.
4. **And the independent one:** `python scripts/visual-qa.py --slot <slug>`'s palette and contrast
   sections read the shipped pixels of whatever device produced them
   (`ON RECORD`, `.cursor/rules/visual-identity.mdc`, "How it is verified"), so a drifted path is
   visible in the pack as well as in the debug diff.

### 8.3 What the app does below API 33

* It **selects the fallback at construction** and never references `RuntimeShader` on the fallback
  path, so no `NoClassDefFoundError` can occur. Checklist item #39 asserts it on the A40's logcat.
* **The shader is not shipped as a "maybe".** The API-33 branch is one `when` over `SDK_INT`; there is
  no reflection, no `Class.forName`, and no try/catch used as a version check.
* **The A40 is the verification target for the fallback and for everything else.**
  A second, API 33+ device is authorised **only** to check the shader path, and only on the owner's word
  (H §0.4 and §10 item 1). Every observation in the handoff names its serial, and the fallback's
  observations are the ones the owner's own phone produced.

---

## 9. The acceptance checklist

Falsifiable, and each one names a number, a visible feature or a measurable property. `[Bn]` maps back
to the bar of `J-sky-realism-and-bar.md` §8. The `V` column is the verification route:
**Z** = the A40, zero-tooling (look at the screen, scrub it); **S** = a screenshot + a measurement
script; **U** = a unit test; **D** = `adb` on `R58MB3SGZVL`; **P** = `visual-qa.py`.

1. **No visible banding.** `[B1]` **S** — at `ε = −4°`, in a 1080 × 2340 capture, a vertical luminance
   scanline at the antisolar azimuth has no step larger than **2/255** between adjacent rows.
2. **Brightest band above the horizon.** `[B2]` **S** — the vertical luminance profile peaks between
   **5° and 15°** above the horizon and is **≥ 15 % dimmer at 1°** than at the peak.
3. **Four antisolar bands.** `[B3]` **Z** — at `ε = −4°` a reviewer can point at, in one screenshot:
   a blue upper sky, a pink Belt of Venus, a blue band, and a distinct horizon band. Their hue angles
   (CIELAB) differ pairwise by **≥ 20°**.
4. **The shadow rises ≈ 8× the Sun.** `[B4]` **S** — scrubbing `ε = −1° → −4°` moves the measured
   shadow edge (the row where luminance drops 50 % toward the antisolar horizon) from **≤ 6°** to
   **≥ 50°**. The ratio of edge motion to Sun motion is **≥ 8**.
5. **Purple is present.** `[B5]` **S** — at `ε = −4°`, the colour at 15° elevation on the antisolar side
   has CIELAB hue in **[280°, 330°]** with chroma **C\* ≥ 6**.
6. **Pale, not a cartoon.** `[B6]` **S** — every twilight band between 15° and 40° elevation has
   **C\* ≤ 25** in CIELAB.
7. **Continuous.** `[B17]` **S** — a `0 → 1` sweep at step `0.002` shows **no colour step larger than
   3/255** between consecutive frames anywhere in the range. No stage boundary is a keyframe.
8. **Stars, not dots.** `[B7]` **S** — in one astronomical-darkness capture, **no core is smaller than
   1.414 px**, **≥ 30 and ≤ 120** stars carry a visible halo, and the halo radius of the brightest star
   in frame is **≥ 4 px** and **≤ 9 px**.
9. **The minimum radius is enforced.** `[B7]` **U** — the radius function returns `≥ √2 px` for every
   `V ∈ [−1.5, 6.5]`.
10. **Twinkle grows toward the horizon.** `[B8]` **S** — a two-frame diff at Δt = 80 ms in a still sky
    shows the RMS brightness change of stars at `h < 15°` is **≥ 3×** that of stars at `h > 60°`.
11. **Extinction reddens and dims.** `[B9]` **S** — Vega rendered at `h = 75°` and at `h = 5°`: the
    low star's **B/R ratio falls by ≥ 0.25** and its total intensity falls by **≥ 1.5 magnitudes**
    (≥ 75 % dimmer).
12. **Colour from `B−V`.** `[B7]` **U** — an `B−V = 1.6` star renders with `R − B ≥ 40/255`; a
    `B−V = −0.1` star renders with `|R − B| ≤ 12/255`.
13. **A realistic star count.** `[B7]` **S** — at `ε = −15°` with the default 73° × 53° field, the
    number of rendered stars is **700–1,100**.
14. **The Milky Way is a structured band with a rift.** `[B10]` **S** — a luminance scan across the
    band at the galactic-plane crossing shows a local minimum of **≥ 25 % depth** between two maxima.
15. **No doubled bright stars.** `[B11]` **S** — no pixel within ±4 px of a masked bright star's
    screen position has luminance **≥ 30 %** of that star's own core.
16. **The band follows the darkness.** `[B16 partial]` **S** — the band's peak luminance above the
    local sky is **0 at `ε ≥ −6°`** and **≥ 8/255 at `ε ≤ −14°`**.
17. **Hard occlusion.** `[B12]` **S** — no sky-gradient value appears below the silhouette line: the
    mean luminance two rows below the ridge is **≤ 6/255** and varies by **≤ 2/255** between
    `ε = −2°` and `ε = −15°`.
18. **The Earth-shadow wedge is geometry-driven.** `[B4]` **S** — the same measurement as item 4.
19. **Warm alpenglow over the ridge.** `[B13]` **S** — at `ε = −1°` a warm band (CIELAB hue
    **20°–45°**) reaches **≥ 12/255** above the local sky, extends **≤ 8 px** above the ridge, and by
    `ε = −8°` has fallen to **≤ 25 %** of its peak.
20. **Refraction lifts objects at the horizon.** `[B14 partial]` **U** — a star whose geometric
    altitude is `0.00°` renders with its centre **≥ 0.50°** above the silhouette line.
21. **The Moon has a phase and earthshine.** `[B14]` **S** — a 25 %-lit Moon's terminator is an ellipse
    whose semi-minor axis is **0.5 ± 0.08** of the semi-major, and the dark limb's luminance is
    **4–12/255** against the sky.
22. **The Moon glows.** `[B14]` **S** — the pixel three core-radii from the Moon's centre is **≥ 1.5×**
    the local sky luminance.
23. **The Moon is deliberately enlarged, not accidentally.** `[B14]` **S** — the rendered disc
    diameter is **2.0×–3.0×** the true 0.518°, i.e. **1.04°–1.55°**, and the app's licences/notes screen
    says so.
24. **Satellites are trails, not dots.** `[B15]` **Z** — a pass renders as a connected polyline of
    **≥ 8 vertices**, present in **≥ 6 consecutive scrub frames**, advancing monotonically in azimuth,
    with **no segment** below the ridge.
25. **Targets are where the ephemeris says.** `[B15 partial]` **U** — the target's screen position
    agrees with an independent ephemeris computation to within **0.25°** at 6 sampled scrub positions.
26. **The FOV frame is a frame, and it is fixed to the sky.** **S** — the rectangle's
    angular width equals the selected lens's horizontal FOV to within **2 %**, and redrawing the ridge
    does not move it.
27. **Astronomical darkness is not black.** `[B16]` **S** — at `ε = −18°` the zenith RGB is between
    **(2, 3, 8)** and **(12, 14, 28)**, and **no sky pixel is `(0,0,0)`**.
28. **Reversible.** `[B19]` **S** — scrubbing `0 → 1 → 0` back to the same value yields **bit-identical**
    sky pixels (the function is pure; no hidden state).
29. **Idle produces zero frames.** **[new, not in B1–B20]** **D** — with no input for 10 s, `dumpsys gfxinfo`'s
    frame counter is **unchanged**.
30. **The scrub is smooth on the A40.** `[B18]` **D** — over a 30 s scripted drag:
    **p95 ≤ 16.7 ms**, **p99 ≤ 33 ms**, **janky frames < 1 %**.
31. **A scrub costs little battery.** **[new, not in B1–B20]** **D** — a 5-minute scripted scrub consumes
    **≤ 2.0 %** of the 3100 mAh capacity; 5 minutes idle consumes **≤ 0.2 %**.
32. **The app's palette is the LUT's palette.** `[B20]` **U** — every colour in the slot's
    `ui/theme/Color.kt` that names a sky role equals the corresponding LUT endpoint sample to
    **ΔE00 ≤ 2**.
33. **The pack's palette is the app's palette.** `[B20]` **P** — `visual-qa.py`'s palette section passes
    on the shipped hero screenshot.
34. **The two sky paths agree.** **U/D** — the debug golden-image test reports **max |Δ| ≤ 2/255** per
    channel over 256 elevations × 61 sun altitudes.
35. **Every asset carries its licence.** **U** — the slot's provenance record names every sky asset with
    its licence, and the set is exactly: ESO `eso0932a` (**CC BY 4.0**), NASA SVS Moon Kit
    (**public domain**), FK6 (**CC0 1.0**), 2MASS (**PD-style + the verbatim IRSA acknowledgement**),
    IAU figures and WGSN names (**CC BY 4.0**), Wikidata (**CC0 1.0**), Solar System Scope if planets
    ship (**CC BY 4.0**).
36. **The mandatory ESO credit is visible.** **Z/P** — `ESO/S. Brunier` renders in the app's licences
    screen at contrast **≥ 4.5:1** against its background.
37. **No copyleft asset is in the bundle.** **U** — a scan of the slot's provenance and the bundled
    asset names finds **no** `GPL`, `AGPL`, `CC BY-SA` or `CC BY-NC` entry. (The catalogue allowlist is
    FK6 / 2MASS / Wikidata / IAU / NASA / ESO / Solar System Scope and nothing else — I §10.)
38. **Below API 33 nothing is missing.** **D** — on the A40 the hero screen renders the LUT path, and
    logcat contains **no** `ClassNotFoundException` / `NoClassDefFoundError` mentioning `RuntimeShader`.
39. **The shader is additive.** **U** — deleting the shader source file leaves the module compiling and
    the hero screen drawing the same scene structure.
40. **The dither is doing its job.** **S** — the standard deviation of the residual after subtracting a
    32 × 32 box blur from a flat twilight area is **≥ 0.3/255** with dither on and **= 0** with it off.
41. **No 8-bit value exists before the tonemap.** **U** — the pipeline's types: the tonemap takes
    `FloatArray` and returns `ByteArray`; no intermediate is `ByteArray`.
42. **The gradient is evaluated, not stretched.** **U** — the strip's row count equals the scene's
    device-pixel height, so no filtering is applied to the sky.
43. **The strip's colours are the same function.** **U** — `strip(i)` for 20 sampled `i` equals the L1
    function evaluated at `(ε_i, 12°)` to **0/255**.
44. **Localisation survives the hero screen.** **U** — every phase label, unit and note exists in
    `values/` and `values-ru/`, and no user-facing string is a literal in Kotlin.

**Count: 44 items.** Their distribution over the bar is: **B1**→1 · **B2**→2 · **B3**→3 · **B4**→4, 18 ·
**B5**→5 · **B6**→6 · **B7**→8, 9, 12, 13 · **B8**→10 · **B9**→11 · **B10**→14 · **B11**→15 ·
**B12**→17 · **B13**→19 · **B14**→20, 21, 22, 23 · **B15**→24, 25 · **B16**→16, 27 · **B17**→7 ·
**B18**→29, 30 · **B19**→28 · **B20**→32, 33. Items 26 and 31–44 are this contract's own additions —
the FOV frame, the battery reading, the licence set, the two-path agreement, the pipeline's colour
discipline and the localisation of the hero screen — none of which the bar covers.

---

## 10. The performance and battery risk register

Nothing in this table is a measurement of this scene on this phone. It is a ranked list of what can
break the frame budget, what the mitigation is, and **which command produces the reading** that would
settle it.

| # | Risk | Why it bites on the A40 specifically | Mitigation in this contract | The command that settles it |
|---|---|---|---|---|
| **R1** | **The scene never runs the shader on this device — so the A40's cost is CPU + Skia, not GPU ALU.** The risk is not "the shader is slow"; it is that every claim about the shader is unverifiable here, and the fallback carries 100 % of the load | API 30 < the AGSL floor of 33 | Make AGSL additive (§8.1); drive the shader only on an API 33+ device on the owner's word; every handoff names the path the A40 observed | `adb -s R58MB3SGZVL shell getprop ro.build.version.sdk` → expect `30` |
| **R2** | **Overdraw.** L1, L3, L4, L9 and L10 each cover a large fraction of 2.53 M pixels; a naive implementation composites 4–5 full-screen layers | a 2-core 2019 GPU on a 1080 × 2340 panel | clip each layer to its own bounding box; the star batch is ≤ 8 calls; the MW mesh is culled to the visible band; the horizon *replaces* pixels rather than adding | `adb -s R58MB3SGZVL shell dumpsys gfxinfo com.gexetr.lumintralightplanner framestats` |
| **R3** | **Per-frame allocation → GC pauses → jank.** A `Brush`, `Path`, `ImageBitmap` or a `FloatArray` allocated inside the draw lambda is the classic Compose scrub failure | the Exynos 7904's cores are slow enough that a GC pause crosses a frame boundary | everything is built in `drawWithCache`; the star position buffer, the vertex buffer and the strip `IntArray` are pre-allocated and reused | `adb -s R58MB3SGZVL shell dumpsys gfxinfo com.gexetr.lumintralightplanner framestats` and `adb -s R58MB3SGZVL logcat -s Choreographer` |
| **R4** | **The 1 × `Hpx` strip rebuild every frame.** ≈ 1,900 LUT samples, a `writePixels` upload and a bitmap draw | a software LUT sample is cheap, but `writePixels` is a CPU→GPU copy | if the measurement shows it, the fallback to a **256-row strip rebuilt every 4 rows of scrub motion and drawn with `FilterQuality.Medium`** is a one-constant change (and checklist item #42 becomes `≥ 256`) | `dumpsys gfxinfo … framestats` with the strip row count as the only variable |
| **R5** | **The star layer is CPU-bound, not GPU-bound.** `drawPoints` with antialiased point sprites is Skia-side work, and the paper's own caution is that *"small primitives tend to stall the GPU pipeline"* | a 2019 mid-range SoC, and the default field draws ≈ 850 stars | ≤ 8 batched calls; a colour-bucket offscreen raster rebuilt only past a 0.25° rotation; a hard cap on the drawn count | `dumpsys gfxinfo …` + the frame time with the catalogue size as the only variable |
| **R6** | **The Milky Way mesh.** 1,152 textured quads through `drawVertices` with an `ImageShader` | fill-rate + per-quad overhead on a 2-core GPU | cull by horizon and by rect; clip at the scene edge; consider a 0.5× offscreen composite if the measurement shows the fill dominates | `dumpsys gfxinfo … framestats`, and a capture at `a_mw = 0` vs `a_mw = 1` to isolate its cost |
| **R7** | **Blur as a soft edge.** The mockup feathers the alpenglow with `feGaussianBlur`; a runtime blur is (a) `RenderEffect`, documented for **API 31+** — `UNVERIFIED` against the pinned BOM — and (b) a full-screen fill trap | API 30 and a weak GPU | **gradients, never blurs** (§5.5); every soft edge is a `Brush` stop pattern | run a `Modifier.blur` on the A40 and observe, once — then never depend on it |
| **R8** | **Battery.** An animated full-screen composited scene on a 3100 mAh cell | not measured — the AMOLED advantage is reasoned, not read | on-demand rendering (§7.4): an idle scene produces zero frames | `adb -s R58MB3SGZVL shell dumpsys batterystats --charged com.gexetr.lumintralightplanner`, before/after a 5-minute scripted scrub and a 5-minute idle |
| **R9** | **Thermal throttling during a long scrub.** Sustained fill on a small chassis | `ON RECORD` (H §9.2) as low–medium | a scrub is a burst, and on-demand rendering means there is no sustained fill | `adb -s R58MB3SGZVL shell dumpsys thermalservice` across a 10-minute scripted scrub |
| **R10** | **Colour drift between the two paths** (the two-device defect) | the A40 can never see the other path | §8.2's four mechanisms, of which the golden-image test is the gate | checklist item #34 |

### 10.1 The claim gate: when "it is smooth on the A40" is allowed to be said

The sentence is permitted **only** when all four of the following have been run on serial
`R58MB3SGZVL`, against the **debug APK built from the revision under review** (the device-testing
rule's artifact rule, `.cursor/rules/device-testing.mdc`), and the readings are printed with the claim:

```text
# 1. which device and which API level — the run is void if this is not 30 / SM-A405FM
adb -s R58MB3SGZVL shell getprop ro.product.model      # expect SM-A405FM
adb -s R58MB3SGZVL shell getprop ro.build.version.sdk  # expect 30

# 2. frame timing during a scripted scrub (see below), then read the percentiles
adb -s R58MB3SGZVL shell dumpsys gfxinfo -c com.gexetr.lumintralightplanner reset
adb -s R58MB3SGZVL shell dumpsys gfxinfo com.gexetr.lumintralightplanner framestats

# 3. the idle case: the frame counter must not move
adb -s R58MB3SGZVL shell dumpsys gfxinfo com.gexetr.lumintralightplanner | Select-String "Total frames rendered"

# 4. battery, scrub vs idle
adb -s R58MB3SGZVL shell dumpsys batterystats --charged com.gexetr.lumintralightplanner
```

**Acceptance:** p95 ≤ 16.7 ms, p99 ≤ 33 ms, janky frames < 1 %, and the idle counter unchanged
(checklist items #29–#31). The scripted scrub is the same journey every time — drag the strip from
`s = 0` to `s = 1` and back at a constant ≈ 2 s per pass for 30 s — so two runs are comparable.

**And the honest statement that must accompany any such reading:** it is a debug build on one phone at
one temperature with one battery level. It is evidence about *that revision on that device*, which is
exactly what `.cursor/rules/device-testing.mdc` calls the artifact under test — not a claim that the
scene is fast in general.

---

## 11. Claim / label / evidence ledger

```text
CLAIM / LABEL / EVIDENCE

--- Inputs read ---
The layered-2-D architecture, the AGSL floor and the A40's specification are as stated in this contract / MEASURED / Read .kiln/scratch/positioning-2026-10-08/H-rendering-options.md in full this session (its §0, §3.1, §7.1, §9.1)
The twilight model comparison, the star techniques, the horizon treatment and the B1–B20 bar are as stated / MEASURED / Read .kiln/scratch/positioning-2026-10-08/J-sky-realism-and-bar.md in full this session (its §1.1–§1.4, §2.1, §3, §4, §8)
Every asset licence named in §9 item 35 / MEASURED / Read .kiln/scratch/positioning-2026-10-08/I-assets-and-data.md in full this session (§1.2, §4.2, §5, §7)
The approved visual target is the canvas at the path given in the brief / MEASURED / Read C:\Users\vsobo\.cursor\projects\d-Python-PlayKiln\canvases\lumintra-night-scene.canvas.tsx

--- The mockup's own choices, read from its source ---
The mockup's sky is a single vertical linearGradient with four stops and no azimuthal term / MEASURED / lumintra-night-scene.canvas.tsx:224-228
The mockup paints the Earth-shadow wedge AFTER the star group / MEASURED / lumintra-night-scene.canvas.tsx:260-268 (wedge path follows the star <g>)
The mockup's phase label is computed from thresholds, not keyframes / MEASURED / lumintra-night-scene.canvas.tsx:90-97
The mockup's timeline strip uses 60 samples / MEASURED / lumintra-night-scene.canvas.tsx:419-420
The mockup's ridge is a 13-vertex polyline / MEASURED / lumintra-night-scene.canvas.tsx:203
The mockup feathers the alpenglow and the wedge with feGaussianBlur / MEASURED / lumintra-night-scene.canvas.tsx:229-231 and :274-281
The mockup names the ESO asset and its licence / MEASURED / lumintra-night-scene.canvas.tsx:495-496

--- Device and toolchain ---
The test phone is SM-A405FM, serial R58MB3SGZVL, Android 11 / MEASURED / .cursor/rules/device-testing.mdc:30
The A40's SoC is Exynos 7904, GPU Mali-G71 MP2 (2 cores, 770 MHz), 1080x2340 AMOLED, 3100 mAh / ON RECORD / H §9.1 citing GSMArena and devicespecifications; reproduced here without re-fetching
The device has run this project's SGP4 core successfully / MEASURED / docs/research/2026-10-08-light-and-sky-session-planner-market.md:506-509 and :735
The factory's test device is API 30, below the AGSL floor of API 33 / MEASURED (device) + ON RECORD (floor) / device-testing.mdc:30; H §3.1 quoting https://developer.android.com/develop/ui/views/graphics/agsl
No frame-time, thermal or battery measurement exists for this scene on this device / UNVERIFIED / H §9.3 says so; settled by the §10.1 commands on R58MB3SGZVL

--- The LUT ---
Bruneton & Neyret 2008 is Precomputed Atmospheric Scattering, CGF 27(4) 1079–1086, EGSR 2008, DOI 10.1111/j.1467-8659.2008.01245.x, with O(1) render cost and support for twilight and Earth shadowing / ON RECORD / J §1.1 table and §1.2, read this session from that document's own citations
Bruneton's published reference implementation is BSD-3-Clause / MEASURED / J's ledger (read this session), quoting github.com/ebruneton/precomputed_atmospheric_scattering ; this contract reimplements from the paper regardless, so the licence does not arise
Ozone (Chappuis bands) is why twilight is purple, and Bruneton 2017 adds it / MEASURED / J §1.3 quoting Lynch et al. §5.A and ebruneton.github.io/precomputed_atmospheric_scattering
Bruneton's own precompute took ~2.0 s on a 2012 desktop GPU / MEASURED / J §1.2 quoting Müller et al. 2012 Table 1
A float16 cell cannot hold a 5–6 order-of-magnitude radiance range, so the LUT is stored relative to a per-column exposure / INFERRED / float16's normal minimum is 6.1e-5 per IEEE 754 binary16; the storage scheme in §2.2 follows
The sRGB transfer function is the piecewise IEC 61966-2-1 curve, not a 2.2 power / ON RECORD / J §7 item 8 and the general standard; the exact curve is IEC 61966-2-1:1999
Blue-noise dithering at ±0.5/255 removes twilight banding and is preferred over Bayer / ON RECORD / J §5.2 and §8 B1, citing Reassembly, Sundog Software, Wronski and the Unreal thread
Kasten & Young (1989), Applied Optics 28(22) 4735–4738, is the air-mass approximation Φ(h) = 1/(cos z + 0.50572(96.07995°−z)^−1.6364) / ON RECORD / the formula's standard citation; NOT re-fetched this session — the builder must re-read the paper before transcribing the constant

--- Stars ---
Müller, Engel & Döllner 2012, Single-Pass Rendering of Day and Night Sky Phenomena, VMV 2012 pp. 55–62, DOI 10.2312/PE/VMV/VMV12/055-062 is the composition blueprint / MEASURED / read in full in J's session and quoted there
The intensity PSF is T(l)=2l³−3l²+1, l∈[0,1], with V_T=1.167 / MEASURED / J §2.1(b) quoting the paper §4.2
The minimum billboard radius is q ≥ √2 px, to eliminate aliasing flicker / MEASURED / J §2.1(b) quoting the paper §4.2
Δm(m) = 2.512^(m_a − m), with m_a = 4 the control magnitude, and glare appearing above it / MEASURED / J §2.1(b) quoting the paper §4.2
The paper's c_q ≈ 4×10⁻⁷ is calibrated for its own radiometric units and is not transferable / INFERRED / a unit-dependent constant cannot be reused without its unit; §3.2 therefore fixes c_q by the V=6.5 visibility criterion instead
The paper's glare profile G(l)=√64·l is ambiguous in the OCR and its constant is unverified / MEASURED (the ambiguity) + UNVERIFIED (the constant) / J §2.1(b) and J's ledger, last rows; §3.4 substitutes a band-limited falloff and says so
Point sprites are unusable at wide FoV because they cluster at the centre / MEASURED / J §2.1(a) quoting the paper §4 verbatim
Hardware point smoothing is vendor-specific and small primitives stall the pipeline / MEASURED / J §2.1(a) quoting elib.dlr.de/215930
Scintillation is proportional to optical path length and grows toward the horizon / MEASURED / J §2.1(d) quoting the paper §4.3.2
Extinction uses Rayleigh coefficients β_r = (0.16, 0.37, 0.91) for sea level / MEASURED / J §2.1(e) quoting the paper
Star colour runs B−V → temperature → Planckian locus → CIE → sRGB, using Olson (1998) and Krystek (1985) / MEASURED / J §2.1(f) quoting the paper §4.3
Ballesteros (2012), EPL 97(3) 34008, gives T_eff = 4600 K (1/(0.92(B−V)+1.7) + 1/(0.92(B−V)+0.62)) / ON RECORD / the formula's standard citation; NOT re-fetched this session — the builder must re-read it before transcribing
An 11-byte-per-star binary and 0.5-mag bins give ~99 KB for ~9,000 stars / INFERRED / arithmetic in §3.8 from the field sizes named there
The default 73°×53° field holds ~9.4 % of the sky, so ~9,000 stars to V=6.5 render as ~850 / INFERRED / solid-angle arithmetic in §3.8 step 3
FK6 carries B−V for all 4,150 stars / UNVERIFIED / not read this session; check the GAVO FK6 cone-service column list and count non-null B−V before relying on it

--- Milky Way ---
ESO eso0932a is CC BY 4.0 with a MANDATORY visible credit and is 6000×3000 / MEASURED / I §5, quoting eso.org/public/images/eso0932a/ and eso.org/public/outreach/copyright/
The ESO source's own map projection is Hammer–Aitoff-derived / UNVERIFIED / the kam-ui note says "Hammer → equirectangular reprojection" but eso0932a's own page was not read for its projection; the check is stated in §4.1
Bright stars must be removed from the star-map texture to avoid doubling / MEASURED / J §3.2 quoting Müller et al. §4.4
At 1080×2340 the Milky Way band is ~150–400 px tall, so large-scale structure dominates / INFERRED / J §3.1's own inference from the pixel geometry
The galactic→ICRS matrix quoted in §4.2 is the standard form published in the Hipparcos and Tycho Catalogues (ESA 1997) Vol. 1 §1.5.3, deriving from Blaauw et al. (1960), MNRAS 121, 123 / ON RECORD / the matrix's standard citation; NOT re-fetched this session — §4.2 requires the build to re-derive it and assert agreement to 1e-6, which is the check
The Milky Way is invisible in twilight / INFERRED / the sky background outshines the band at ε > −8°; the fade curve of §4.4 encodes it and checklist item #16 is the measurement

--- Horizon ---
The antitwilight bands are upper sky / Belt of Venus / blue band (Earth's shadow) / horizon band, from Lynch, Dearborn & Richtsmeier (2017), Applied Optics 56(19) G156–G168, DOI 10.1364/AO.56.00G156 / MEASURED / J §1.4 quoting the paper
The Belt of Venus moves ≈8× the Sun's altitude; BV/BB transition width 8°→0.9° as ε→0; BV appears at ε≈−6° and reddens at ε≈−2° / MEASURED / J §1.4 quoting Lynch §4/§6
The shadow edge rises far faster than ε and approaches the zenith while ε is in single digits / MEASURED / J §4.2 quoting Lynch et al. Eqs. 1–7 and its own prose
α_s(ε) = ε + arcsin((R/2H)·ε²) with R=6371 km, H=20 km, and the t₂(α,ε)=R(1−cos ε)/sin(α−ε) intermediate / INFERRED / derived in §5.4 this session from the ray–sphere tangency condition; its numeric behaviour (4.0° at −1°, 13.2° at −2°, 28.9° at −3°, 54.9° at −4°, above the zenith from −5°) is consistent with the ON RECORD qualitative statement above and is falsified by checklist item #4
The sky is brightest above the horizon and dimmer in the last few degrees / MEASURED / J §1.4 quoting Lynch §4.A/§5 (optical depth unity ~15° typical, ~5.3° very clear)
Refraction lifts the Sun and Moon ≈0.58° at the horizon (Bennett) / MEASURED / J §1.4 quoting Lynch §7
Twilight colours are pale and unsaturated, near the achromatic point in CIE / MEASURED / J §1.4 quoting Lynch §4.B citing Lee
The Belt of Venus is the twilight form of alpenglow / ON RECORD / J §4.3 quoting Wikipedia "Belt of Venus"
Müller uses c_d ∈ [2,3] to enlarge the Moon beyond its true apparent size / MEASURED / J §4.5 quoting the paper
Modifier.blur / RenderEffect is documented for API 31+ / UNVERIFIED / a fetch of developer.android.com's Compose draw reference timed out this session; the check is to run a blur on the A40 and observe, and §5.5 forbids depending on it either way

--- Interaction ---
Reading State<Float> in drawBehind/drawWithCache invalidates only the draw phase; passing a plain Float recomposed 238× in the published case study / MEASURED / J §5.1 quoting developer.android.com's Compose phases guide and a case study
drawWithCache caches Brushes/Paths while size is unchanged / MEASURED / J §5.1 quoting the reference
Material guidance is that the thumb tracks the finger 1:1 and the drag itself is not eased / MEASURED / J §5.2 quoting the motion guidance
M3 emphasized-decelerate = cubic-bezier(0.05,0.7,0.1,1); short4=200ms, medium2=300ms / MEASURED / J §5.2 quoting material-web _md-sys-motion.scss and Flutter motion.dart
The M3 expressive scheme is the recommended one for prominent/hero interactions / MEASURED / J §5.2 quoting the MotionScheme reference

--- Agents, licences, and what was not done ---
AGSL is API 33+ and contributes a fragment-stage function to the Canvas/RenderNode pipeline / ON RECORD / H §3.1–§3.2 quoting developer.android.com; the A40 cannot run it
AGSL loops must be unrollable with constant bounds and arrays are indexable only by a constant or a loop variable, so a star catalogue cannot be evaluated per pixel / ON RECORD / H §3.2 quoting the AGSL quick reference
No permissive, licence-clean Android library renders an astronomical sky / ON RECORD / H §5 and its ledger
HYG (CC BY-SA 4.0), Hipparcos/Tycho/Gaia (CC BY-NC 3.0 IGO), APASS (permission required) and BSC5 (unlicensed by default) are all unusable or unverified for this slot / MEASURED / I §1.1, I's ledger quoting each source's own page
FK6 is CC0 1.0 / MEASURED / I §1.2 quoting dc.g-vo.org/fk6/q/cone/form ("waived all copyright ... CC0 1.0")
2MASS requires a verbatim acknowledgement and states no commercial restriction / MEASURED / I §1.2 and §9 quoting irsa.ipac.caltech.edu/Missions/2mass.html
NASA SVS content is public domain unless noted; the NASA insignia is not, and no endorsement may be implied / MEASURED / I §4.1 quoting svs.gsfc.nasa.gov/help/ and nasa.gov/nasa-brand-center/images-and-media/
IAU constellation figures and WGSN star names are CC BY 4.0 / MEASURED / I §3.2 quoting iau.org and the IAU Copyright page
Wikidata structured data is CC0 1.0 / MEASURED / I §1.2 quoting wikidata.org/wiki/Wikidata:Licensing
Solar System Scope textures are CC BY 4.0 with commercial use explicitly permitted / MEASURED / I §4.4 quoting solarsystemscope.com/textures/
No code was copied, no shader was pasted, no asset was downloaded and no file outside .kiln/scratch/ was written in this session / MEASURED / this report is the only file written; all algorithm sources are papers named above and all asset licences are I's readings
The frame-budget numbers in §7.5 are targets, not measurements / UNVERIFIED / no frame-time reading exists for this scene on this device; settled by the §10.1 commands
```

**The one honest limit of this report.** It is a **contract**, and a contract is not a rendering. Three
of its load-bearing numbers are *derived or chosen here*, not quoted, and each is labelled in place:
the exposure-normalised LUT storage (§2.2), the glare calibration `c_k = 0.50 px` and profile
`G(l) = (1 − l²)²` (§3.4), and the Earth-shadow edge formula `α_s(ε)` (§5.4). All three are falsifiable
through the checklist — item #34 for the two paths, item #8 for the halo radii, item #4 for the shadow
motion — and none of them is presented as a measurement it is not. And the thing this report most
emphatically cannot say is the one the owner will ask first: **whether it is smooth on his own phone.
No reading of that kind exists for this scene yet, §10.1 is the only route to it, and until it is run
the answer is `UNVERIFIED`.**
