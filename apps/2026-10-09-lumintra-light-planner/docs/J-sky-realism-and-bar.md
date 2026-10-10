# J — Sky realism techniques and the "stunning" bar

Research for a **paid Android light-and-sky session planner** whose hero screen is a **scrubbable sky
scene**: the user's drawn horizon silhouette, the real sky for a location/date, a time slider that
animates sunset → twilight → astronomical darkness → dawn, and the moon, rising deep-sky targets and
satellite passes drawn in it. The owner's bar: *"this must not look like a form with a button"* — it
must be the most visually striking app the factory has produced, and it should reuse what is already
solved rather than reinvent it.

**Scope of this document.** It answers seven questions and ends with a checkable bar. It implements
nothing, adds no dependency and writes no file outside `.kiln/scratch/`.

**Label discipline** follows `.cursor/rules/grounded-claims.mdc`: `MEASURED` (I ran a command, read a
file, or read a page in this run), `ON RECORD` (a named source states it and I read the source),
`INFERRED`, `UNVERIFIED` (with the exact check that would settle it). Every technique attributed to a
paper names the paper. The full ledger is §9.

### The one legal principle this whole document rests on

**Reimplementing a published algorithm is not copying code.** A paper that describes an equation, its
parameters and its fitting procedure publishes a *method*; writing our own Kotlin/Compose/RenderScript
(or a build-time Python baker) that evaluates that equation is independent implementation, exactly as
writing our own quicksort from the algorithm is not copying a quicksort. This is the same distinction
the factory already draws for icon licences in
`docs/research/2026-10-02-ui-icon-libraries-and-licensing.md` §1.2: the licence attaches to the
**expression** (source, assets), not to the idea or the formula.

Two consequences, stated because they bound what follows:

- **No GPL/AGPL implementation is ever proposed for shipping.** Where a permissive reference
 implementation exists we may *read it as documentation*, and where its licence is permissive we may
 even port table values — but the recommendation throughout is to work from the **paper**, so the
 licence question does not arise at all. Copyleft-licensed ports (many Shadertoy/Unity WebGL ports are
 unlicensed or GPL) are on the denylist by the same reasoning as the icon denylist: an unknown licence
 is not a permissive one (`.cursor/rules/…` and the icon doc's §1.3, cited).
- **Assets are a different question from algorithms and must be checked separately.** A published
 *equation* has no licence; a published *sky panorama texture* does. §3.3 flags the one that would have
 bitten us.

---

## 1. The twilight-colour problem — published sky models, compared

A naive two-colour `Brush.verticalGradient(top=blue, bottom=orange)` looks cheap for a specific,
measurable reason: real twilight is **not a two-colour vertical gradient**. Between sunset and dark,
the sky's vertical profile has *four* roughly horizontal bands (measured, not asserted — see §1.4), the
colour is a *function of scattering geometry*, and brightness falls by 5–6 orders of magnitude in about
an hour. The models below are the published ways to compute that function.

### 1.1 The models, what each produces, and its cost

Complexity notation is from the evaluation paper's own Table II (label `ON RECORD`, source below),
where `n` is samples per dimension and `n_λ` wavelengths. **RMSE** is that paper's summed spectral
error against measured/libRadtran ground truth at 0.816/0.0384/0.704 atmosphere parameters — lower is
more accurate.

| Model | Paper / reference | What it produces | Sun below horizon? | Precompute | Render | RMSE | Sunset realism |
|---|---|---|---|---|---|---|---|
| **Preetham** 1999 | Preetham, Shirley, Smits, *A Practical Analytic Model for Daylight*, SIGGRAPH '99, pp. 91–100, DOI 10.1145/311535.311545 | Analytic sky luminance + chromaticity from turbidity; the classic cheap daylight sky | **No** (daylight only) | none | `O(1)` | **88.1** (overestimates ~2×) | **weak** — its known weakness |
| **Hošek–Wilkie** 2012 | Hošek & Wilkie, *An analytic model for full spectral sky-dome radiance*, SIGGRAPH 2012, ACM TOG 31(4) pp. 95–104, DOI 10.1145/2185520.2185591 | Physically-based analytic **spectral** sky; ground albedo; explicitly improves sunsets & high turbidity; a **drop-in replacement for Preetham** | **No** (daytime, sun above horizon) | none | `O(1)` | **41.5** (overestimates overall, underestimates near sun) | **much better than Preetham** |
| **Nishita93** | Nishita, Sirai, Tadamura, Nakamae, *Display of the Earth taking into account atmospheric scattering*, SIGGRAPH '93 | Numeric single-scattering integration; all viewpoints; aerial perspective; supports sun below horizon | yes | `O(n³)` time / `O(n²)` mem | `O(n)` | **26.6** (underestimates ~1/3) | limited — **no multiple scattering** |
| **Haber** 2005 | Haber, Magnor, Seidel, *Physically-based simulation of twilight phenomena*, ACM TOG 24(4), 2005 | Multiple scattering + **ozone absorption** + curved light paths; ground-only | yes | `O(n⁶)` / `O(n³)` | `O(n²)` | **14.7** | good, but re-precomputes per sun angle |
| **Bruneton–Neyret** 2008 | Bruneton & Neyret, *Precomputed Atmospheric Scattering*, Computer Graphics Forum 27(4) pp. 1079–1086, EGSR 2008, DOI 10.1111/j.1467-8659.2008.01245.x | Precomputed **Rayleigh + Mie multiple scattering**; "the daylight and **twilight** sky colour", aerial perspective, Earth/mountain shadows and light shafts; all viewpoints ground→space | **yes** (precomputes all sun zenith angles incl. below horizon) | `O(n⁶)` / `O(n⁴)` | **`O(1)`** | **11.3** | **best of the classic set** |
| **Elek** 2010 | Elek & Kmoch, *Real-time spectral scattering in large-scale natural participating media*, SCCG 2010 | Bruneton run on more wavelengths then converted to RGB; same complexity | yes | `O(n⁶)` / `O(n⁴)` | `O(1)` | **11.3** (spectral) | best, spectral |
| **Perez** 1993 | Perez, Seals, Michalsky, *All-weather model for sky luminance distribution*, Solar Energy 50(3), 1993 | The luminance-distribution basis Preetham fits to; diffuse-sky luminance vs sky position | sun-above | none | `O(1)` | (not in the CG comparison) | daytime diffuse sky only |

Sources for this table: the **comparison paper** is Šárka, Hosek, and others' *A Qualitative and
Quantitative Evaluation of 8 Clear Sky Models*, arXiv 1612.04336 (`MEASURED` — I read Table II and the
per-model sections in this run), and each model's own citation is in its row. The claim that
Hošek–Wilkie is *"more accurate than Preetham"* in the sunset/turbidity case is Hošek–Wilkie's own
abstract (`MEASURED`, SIGGRAPH History Archive summary of the paper) and is corroborated by the
comparison paper's ranking (`ON RECORD`).

### 1.2 The single most relevant paper for this product

The models above describe *sky colour*. The product is a **composed night-to-day scene**. The paper
that already solves the composition — and that we should treat as the blueprint — is:

> **Daniel Müller, Juri Engel, Jürgen Döllner, *Single-Pass Rendering of Day and Night Sky
> Phenomena*, Vision, Modeling, and Visualization (VMV) 2012, pp. 55–62,
> DOI 10.2312/PE/VMV/VMV12/055-062.**
> (`MEASURED` — I read the full paper text in this run.)

What it does, verbatim from the paper (`MEASURED`):

- **Atmosphere**: uses Bruneton & Neyret for the atmosphere ("features a good match to CIE Standard
  General Sky, accounts for earth shadowing, and supports single as well as multiple scattering").
- **Stars**: "GPU generated, viewpoint-aligned billboards are used to render stars with approximated
  color, brightness, and scintillations" — and explicitly *"Utilizing point-sprites is also not
  applicable. They are unaffected by camera distortion, which leads to star clustering in the center
  when using larger FoVs."*
- **Moon**: a GPU viewpoint-oriented billboard simulating a virtual moon-sphere, with phase, **Hapke–
  Lommel–Seeliger BRDF** reflectance, **earthshine**, and lunar-eclipse colouring.
- **Milky Way**: "For correct background illumination, faint stars that indicate the Milky Way, are
  added by a textured cube."
- **Day-night blend**: star intensity scaled by a smooth function of the sun's altitude so stars fade
  to zero just after sunrise. Measured performance: **cloudless day-night sky < 4 % frame cost**, and
  atmosphere precompute ≈ **2.0 s** on a 2012 desktop GPU (`MEASURED` — its Table 1).

That is, in one paper: exactly the components our hero screen needs, composed in one pass, at a cost
that fits a phone. This is where the factory should point its reuse, not at a single sky model.

### 1.3 What this means for the Android implementation — three concrete options

Cost is what makes the choice; the paper's own complexity column is the input.

- **Option A — analytic only (cheapest, weakest at twilight).** Evaluate **Hošek–Wilkie** per vertex into
  a **~32-stop vertical gradient**, interpolated in **linear light** then gamma-encoded, plus a separate
  azimuth term for the sun's glow. Day and sunset look good. **But Hošek–Wilkie is a daytime model**; it
  does not render the sun-below-horizon twilight the app is named for. It would have to be blended into
  a hand-authored night gradient — a seam, and the seam is where "cheap" shows.
- **Option B — bake Bruneton offline, sample at runtime (recommended).** Run Bruneton's precomputation
  **at build time** in a build-side tool (Python/Node), emitting a small **sky LUT** committed as an
  asset: an equirectangular (or a few vertical-column) radiance texture parameterised by **sun
  altitude** (say 64 steps from +90° to −18°) × view elevation, optionally × view azimuth near the sun.
  At runtime the shader is a `O(1)` LUT fetch + interpolation — which is precisely Bruneton's `O(1)`
  render term. Benefits: honours the app's whole range including **twilight and the Earth's shadow**,
  needs no `O(n⁶)` precompute on the phone, and the baker is *our own code evaluating a published
  method*. Bake cost is a build-time minute, paid once, off-device. This is the option the evidence
  supports.
- **Option C — full Bruneton on-device.** Possible on modern GPUs but wasteful for a slider whose inputs
  (location/date) change rarely; the precompute would have to be redone when the date changes unless
  the LUT is a function of sun altitude only, which Option B already exploits.

**The ozone term is not optional for a striking app.** Ozone (Chappuis-band) absorption is *why twilight
is purple*, not merely orange→black: the antitwilight paper notes "some of its color is the result of
ozone absorption in the Chappius bands" (`MEASURED`, Lynch et al., §5.A). Bruneton's 2017 reference
implementation explicitly "adds support for the ozone layer" (`MEASURED`,
`ebruneton.github.io/precomputed_atmospheric_scattering`). A model without ozone will produce an
orange-and-grey dusk that reads as apocalypse, not beauty.

**Tone-mapping and white balance are part of realism, not decoration.** The comparison paper measured
that participants found a *less* physically accurate rendering **more** realistic when it happened to
have a "white-balance" effect, and concludes "it is also important to correctly simulate perceptual
effects (such as tone mapping and white balance)" (`MEASURED`, arXiv 1612.04336, analysis of Fig. 10 /
Table I). So the bar includes a deliberate tonemap and a fixed (not auto) white balance, and the
transition must be smooth in *perceptual*, not linear, space.

### 1.4 The twilight structure the model must reproduce — measured, not guessed

This is the anatomy a "two-colour gradient" cannot fake, and it is the concrete checklist for the
twilight look. From **Lynch, Dearborn, Richtsmeier, *Antitwilight I: structure and optics*, Applied
Optics 56(19), 2017, pp. G156–G168, DOI 10.1364/AO.56.00G156** (`MEASURED` — read in full):

- Twilight is sunlight scattered while the Sun is between **0° and −18°** below the horizon; with very
  turbid air it can be seen to ≈ −23°.
- **Four anti-solar bands**, top to bottom: **upper sky (US, blue)**, **Belt of Venus (BV, pink)**,
  **blue band (BB — the Earth's shadow)**, **horizon band (HB, anchored to the horizon)**.
- The **Belt of Venus moves ≈ 8× faster than the Sun's altitude**. Measured: the BV/BB boundary drops
  from ~8° wide transition at ε≈−3° to ~0.9° at ε≈0°. This *rapid motion* is a signature a scrubber can
  show and a static gradient cannot.
- Brightness is **not monotonically decreasing to the horizon**: "the altitude of maximum sky
  brightness increases ~linearly downward toward the horizon until it reaches a maximum, then decreases
  toward the horizon." The maximum sits well above the horizon (for the paper's clear coastal air,
  optical depth unity — i.e. the top of the HB — is ≈ **15°** above the horizon; in much clearer air,
  ≈ **5.3°**). The lowest few degrees are *dimmer* (Lee's horizon dimming; the "dark segment").
- The **BV first appears at ε ≈ −6°**, is clearly redder than blue only from **ε ≈ −2°**, and becomes
  "the brightest part of the antitwilight." The horizon band turns **blue** when the BB reaches it and
  **pink** as the BV sets onto it near sunrise.
- At **ε ≈ −6° to −3°** blue is the brightest RGB component at every altitude; the "pink" BV actually
  carries more blue than red and *looks* pink by contrast — so an app that paints the BV as literal
  pink over a dark sky gets it wrong.
- Twilight colours are **pale and unsaturated**, clustered near the achromatic point in CIE (Lee, cited
  in the paper). A saturated orange is the cartoon, not the sky.
- Refraction lifts the Sun/Moon ≈ **0.58°** at the horizon (Bennett, cited in the paper) — they are seen
  touching the horizon while geometrically below it.

The **naked-eye twilight-stage boundaries** the slider must label are the standard ones: civil ends at
Sun altitude **−6°**, nautical at **−12°**, astronomical at **−18°** (`ON RECORD`, Sky & Telescope
"Tiptoe Into The Twilight Zone"; consistent with the antitwilight paper's 0…−18° range).

**What makes a rendered sky read as *real* rather than a gradient — the answer, in one paragraph:** it
is a *physically parameterised function of view direction and sun altitude* (Rayleigh λ⁻⁴ + Mie + ozone
+ multiple scattering), not a value the artist picks; it contains the **antisolar band structure** and
the **rising Earth-shadow wedge**; it is **brightest above the horizon, dimmer at it**; it is
**desaturated**; it is **continuous in sun altitude** (never stepped between "stages"); it is
**dither-quantised** so no banding is visible; and it is **tonemapped and white-balanced** deliberately.
Every one of those is a checkable statement (§8).

---

## 2. Stars — how thousands of them read as stars, not dots

### 2.1 The techniques, named and sourced

**(a) Screen-space `GL_POINTS` is the trap.** The Müller 2012 paper states point sprites are *"not
applicable"* because they "are unaffected by camera distortion, which leads to star clustering in the
center when using larger FoVs" (`MEASURED`). The 2025 DLR *Rendering Millions of Stars in Real-Time*
paper adds the second failure: hardware point smoothing "is vendor specific, so we cannot know how much
luminance a smooth point will actually contribute", and "small primitives tend to stall the GPU
pipeline" (`MEASURED`, elib.dlr.de/215930). So: **do not draw stars as `GL_POINTS`/hardware points.**

**(b) The accepted technique: viewpoint-aligned billboards + a Point Spread Function.**
- Müller 2012: "point light sources are rendered with viewpoint aligned billboards that adapt to the
  actual output resolution"; each star gets a per-star **intensity PSF** plus a **glare PSF** "to
  overcome physical intensity constraints and provide a larger range of apparent brightness as common
  in HDRR."
- **Intensity PSF**, exactly as published: `T(l) = 2l³ − 3l² + 1`, with `l ∈ [0;1]` the normalised
  distance from the billboard centre (`MEASURED`). Its disc-integrated volume `V_T = 1.167`.
- **Glare PSF** for bright stars, on an enlarged billboard (`MEASURED`): the paper gives
  `G(l) = √64·l` (printed ambiguously in the OCR; treat the *shape* — a sqrt falloff — as the finding,
  the exact constant as `UNVERIFIED` pending a clean read of §4.2), with billboard radius
  `k = max(q, c_k√I_G)`.
- **Minimum radius** `q ≥ √2` px (to "eliminate flickering due to aliasing"), computed from the vertical
  FOV `γ_v` and resolution `res_v`.
- **Brightness ↔ magnitude**: `Δm(m) = 2.512^(m_a − m)`; the glare kicks in for stars brighter than the
  control magnitude `m_a`; calibrated constant `c_q ≈ 4×10⁻⁷` for `m_a = 4` (`MEASURED`).
- Gaia Sky does the same family of thing: convert apparent magnitude → absolute magnitude
  `M = m − 5(log₁₀ d − 1)`, then to a "pseudo-size" via luminosity `L = L₀·10^(−0.4·M_o)`, apply a
  constant and a square root; render on billboard quads whose angular size is `α = atan(p/d)`, with a
  "brightness power" that "artificially widens the difference between bright and faint stars"
  (`MEASURED`, Gaia Sky 3.4 docs, Star rendering).
- **Why not just circles** (the general optics argument, `ON RECORD`, tiffnix "Rendering star fields in
  3D" and the CG StackExchange thread): a real star is an unresolved point spread by the optical system
  into an **Airy/PSF pattern with glow (and for some optics, diffraction spikes)**; a flat circle of
  fixed size is either too small (aliases) or too big (bulky), and distance falloff must be
  inverse-square. So the *shape* of the star is the finding: a bright core + a glow halo, not a disc.

**(c) Magnitude → size AND brightness (two channels, not one).** Müller and Gaia Sky both use size *and*
intensity, and both note that uniformly scaling the size makes the faintest stars too bulky, which is
why the *glare* carries the brightness cue on an enlarged billboard while the core stays near its true
(pixel-scale) radius. On a phone the practical rule: **faint stars are sub-pixel and near their true
colour with no size inflation; only magnitude-bright stars grow a halo.**

**(d) Twinkle / scintillation, and why it grows toward the horizon.** Müller 2012 models scintillation
as proportional to the optical path length `Φ(θ)` through the atmosphere, which "strongly increase[s]
towards the horizon", multiplying star brightness by `S = c_s·β_r·Φ^N` with a per-frame random basis
(`MEASURED`). This is the physically-correct reason **stars near the horizon twinkle more and are dimmer
and redder** — the same `Φ(θ)` drives extinction (§2.1(e)). Twinkle is a *time* phenomenon: a per-frame
fluctuation, not a static pattern.

**(e) Extinction near the horizon.** Both Müller 2012 and Gaia Sky apply atmospheric extinction: Gaia Sky
"correct[s] it using extinction data" before mapping magnitude to size (`MEASURED`); Müller attenuates
colour and intensity by an out-scattering factor `O(θ)` built from the optical path length, using
Rayleigh coefficients `β_r = (0.16, 0.37, 0.91)` for sea level (`MEASURED`). Consequence: a star low in
the sky is **dimmer and redder** than the same star overhead — a checkable per-star property.

**(f) Colour from temperature.** Stars are not white. Müller 2012: B−V (Johnson–Morgan) → approximate
temperature → Planckian locus (polynomial fit) → CIE tristimulus → sRGB, using Olson (1998) and Krystek
(1985) (`MEASURED`). This is the published pipeline; there is also a standard Johnson B−V → RGB
approximation used by amateur software.

### 2.2 Android-specific note

None of the star techniques are GPU-compute-dependent. On Android the pattern that fits the factory's
stack is: compute each visible star's screen position, size and brightness **once per time-change** (a
rotation matrix from the star's equatorial coordinates to horizontal, `R_s = R_y(δ−π/2)·R_z(−LMST)·P`
per Müller Eq. 7 — `MEASURED`), then in Compose draw the star layer as **pre-shaped sprites** (a
radial-gradient glow bitmap, or a `drawCircle` core + a `Brush.radialGradient` halo) with additive
blending (`BlendMode.Plus`) so overlapping glows sum, which is the HDR behaviour the paper relies on.
The stars' *positions* rotate with time; their *shape* does not change — so the expensive part is
per-frame re-rotation, not re-shaping.

---

## 3. The Milky Way and the background

Three published approaches, with the trade-off named.

**(a) Photographic panorama.** A real all-sky image reprojected to equirectangular and sampled on a sky
dome. Example (`MEASURED`, kam-ui "Milky Way" component): "ESA's Gaia space observatory mapped ~1.7
billion stars. The colour panorama (Hammer → equirectangular reprojection) is textured onto a sky dome
hemisphere… ~945 KB texture asset." Two independent implementations use this: Stellarium ships a
"realistic Milky Way", and TPE desktops a "3D Celestial Sphere" (`ON RECORD`).
- **Visual result**: the best realism, because it *is* real data — the bulge, the Great Rift and the dust
  lanes are correct, not approximated.
- **Cost**: one texture (sub-MB is achievable), zero per-frame maths.
- **The trap**: **licence.** The commonly-shared Gaia DR2 colour panorama is **CC BY-SA 3.0 IGO**
  (`MEASURED` — read verbatim in the kam-ui credit line "Star data credit: ESA/Gaia/DPAC (CC BY-SA 3.0
  IGO)"). CC BY-**SA** is **forbidden** by the factory's own licence rule (share-alike is incompatible
  with a paid closed-source APK — `docs/research/2026-10-02-ui-icon-libraries-and-licensing.md` §1.2,
  cited). So this path requires a **public-domain or attribution-only** source (NASA/ESO CC BY 4.0, or a
  NASA "Astronomy Picture of the Day" that is PD), recorded in the slot's provenance, or a **self-baked**
  texture.

**(b) Procedural band.** Model galactic latitude as a **narrow exponential/Gaussian falloff**, displace
the coordinate axis with Perlin noise, subtract a second gradient for the central dust rift, subtract a
Voronoi field for grain, add colour with more noise, and layer scaled-up Voronoi points as stars
(`ON RECORD`, "Procedural Milky Ways", artsnscience.eu — the node-graph explainer states the method
explicitly). Gaia Sky's procedural galaxies use the same primitive set — "FBM Noise", additive particles
for stars/gas, subtractive for dust lanes (`MEASURED`, Gaia Sky procedural-galaxies docs).
- **Visual result**: consistent with the app's own art style and *parameterisable* (you can change the
  dust contrast). But it looks *generic* unless a lot of tuning goes in, and the rift is a noise field,
  not the real Great Rift.
- **Cost**: pure shader/maths; no asset.

**(c) Baked density/radiance texture from a real catalogue (recommended middle path).** Bin a real star
catalogue into an equirectangular luminance map, blur to smear unresolved starlight, tint from weighted
mean B−V, and multiply in dust extinction `exp(−k·τ)` and a continuous radiance map. Measured example
(`ON RECORD`, SharpAstro/tianwen `generate_milkyway.cs` + `bake_milkyway.ps1`): "Bins real Tycho-2
photometry (~2.5M stars, V mag + B-V) into a galactic luminance map, applies a separable Gaussian blur
to smear unresolved starlight, and tints each pixel from the weighted mean B-V of the stars that fell in
it. The galactic plane, bulge, and Magellanic clouds all appear naturally because they exist in the
catalog." Its tuning constants are recorded (K=11.8, BlurSigma=0.30, MinMag=10.0, Saturation=3.2,
Brightness=0.478, LuminanceMix=0.566…), and it was optimised against a Stellarium reference.
- **Visual result**: real structure (bulge, dust lanes, LMC/SMC) without a share-alike image licence; you
  own the baked asset.
- **Cost**: a build-time bake from a public catalogue; then the same one-texture runtime cost as (a).

### 3.1 Which looks better on a phone at 1080×2340

**`INFERRED`** (my reasoning from the sources above, not a measurement): At 1080×2340 the Milky Way arc
occupies perhaps 150–400 px of height depending on framing. At that scale the *large-scale structure*
(the bright bulge, the dark rift) dominates perception and per-star grain is below the eye's
resolution — which is exactly what the bake's own note says: "Low BlurSigma keeps the radiance channel
sharp (LMC/SMC rims, dust lanes around rho Oph)" and the paper's star-map guidance says the map
"resolution should be rather low, just indicating the Milky Way instead of further individual stars"
(`MEASURED`, Müller §4.4). So a **photograph or a catalogue-baked luminance+colour texture beats
procedural noise** on a phone, because noise cannot reproduce the real *rift* at the one scale the user
sees. The choice between (a) and (c) is therefore a **licence** decision, not a visual one; the visual
answer is "use real data", and (c) is the way to use real data without the CC BY-SA trap.

### 3.2 The double-star trap

Müller 2012, verbatim: "Especially for high resolutions, bright stars should be removed though, to avoid
star doubling caused by positional differences to individual rendered stars." (`MEASURED`). If the
Milky Way texture contains bright stars *and* the scene renders those stars as sprites, any positional
mismatch makes them appear twice. **The baked Milky Way texture must be a background-only layer with
bright stars removed.**

### 3.3 Licence note for background assets

Per the icon-licensing research this factory already holds (`MEASURED` for that doc's method), a
texture is an **asset** and its licence governs bundling. Safe default for a paid closed-source app:
**CC0 / public domain** (NASA, ESA where stated, US-government works), or a **self-baked** texture whose
only input catalogue is itself permissively licensed or public. **CC BY-SA and CC BY-NC are out**
(exact licence families and their quoted terms are in the icon doc §1.2–1.3, cited rather than
restated). The provenance record must name the source and licence, as `store/provenance.json` already
does for pack media.

---

## 4. The horizon — how the drawn silhouette should meet the sky

The horizon is where "a sky render" becomes "a *place*". Five concrete, implementable treatments, each
traceable to a source above.

1. **Hard occlusion, then darken the ground.** Draw the sky first (full screen / dome), then the user's
   silhouette as an opaque path on top. Everything below the silhouette line is **ground**, not sky: it
   should be a dark, desaturated fill (or the user's own ground texture), never the sky gradient showing
   through. This is the base correctness — a silhouette that lets sky bleed through reads as a sticker.

2. **The antisolar Earth-shadow wedge, geometry-driven.** The single most authentic twilight cue is the
   **blue band** — the Earth's own shadow rising opposite the sun. Lynch et al. give the construction
   (`MEASURED`, Eqs. 1–7): the shadow line is the ray tangent to the Earth separating sunlit from shaded
   air, its slope `m = tan ε`, tangent point at `x_p = −R·sinε, y_p = R·cosε` (for earth radius `R`,
   solar altitude `ε`), intersecting the top of atmosphere at altitude `α(ε)` which "increases much
   faster than ε" and approaches the zenith while ε is still in single digits. **Implementation**: for
   the current sun altitude compute `α`, darken/blue the sky below that altitude on the *antisolar*
   side, and let it sink toward the horizon as the sun rises — the ~8×-faster motion is the magic the
   slider reveals.

3. **Warm glow behind/above the ridge at sunset — "alpenglow".** The Belt of Venus is *literally* the
   twilight analogue of alpenglow (Wikipedia, `ON RECORD`: "the Belt of Venus is actually alpenglow
   visible near the horizon during twilight… the backscatter of reddened sunlight"). So at sunset the
   treatment is not just "sky behind the hill" but a **warm backscatter band above the ridge**: a soft,
   additive warm-red/pink gradient that hugs the silhouette's upper edge and is brightest where the sun
   is. It should *bleed slightly over* the ridge top (blur/feather the silhouette's top edge by a few
   px), because in reality the light scatters in the air in front of and behind the ridge.

4. **Brightest sky is above the horizon, dimmer at it.** As §1.4 measured, the sky's luminance peaks
   several-to-many degrees above the horizon and *decreases* into the last few degrees. So: **do not let
   the ground line meet the brightest part of the gradient.** Put the max-brightness stop at ~10–15°
   altitude (clear-air) or ~5° (very clear), and a dimmer "horizon band" just above the silhouette.

5. **Refraction and scale near the horizon.** Lift celestial objects ≈ 0.58° at the horizon (Bennett /
   Lynch §7) and, per the DLR/Müller star work, redden and dim them there. The moon near the ridge should
   be allowed to look slightly larger (Müller's `c_d` factor acknowledges a purely true apparent size
   "yield[s] a subjectively small moon", and uses `c_d ∈ [2,3]` — `MEASURED`) — a deliberate artistic
   exaggeration that reads as a photo, not an error (label it as such if it ships).

A sixth, optional cue: **aerial perspective** — distant terrain desaturates and hazes toward the sky
colour with distance. Bruneton provides it natively (`ON RECORD`) and the composition paper cites it as
one of the reasons to use Bruneton over an analytic model.

---

## 5. The time-scrub interaction — from "moving a slider" to "magical"

### 5.1 Make the sky a pure function, then read the time only in the draw phase

The scene is a function of `(lat, lon, date, time)`. Scrub changes one `Float`. The jank-free Compose
pattern is documented and measured:

- **Pass the animated value as `State<Float>`, and read it inside `drawBehind` / `drawWithCache` /
  `Canvas`.** Android's own phases guide: "State reads during drawing code affect the drawing phase…
  When the state's `value` changes, Compose UI runs only the draw phase." Passing a `Float` recomposes
  238 times in the published case study; passing a `State<Float>` lets Compose **skip composition and
  layout** on every frame of the animation (`MEASURED`, developer.android.com Compose phases;
  proandroiddev case study).
- **Cache heavy draw objects** (`Brush`, `Path`, `Shader`) in `drawWithCache`, which persists them while
  size is unchanged (`MEASURED`, `drawWithCache` reference). The sky gradient brush and the star sprite
  shapes belong here.
- **`derivedStateOf`** is for *coarsening* a rapidly changing value (e.g. "which twilight stage are we
  in" as an enum for the label), **not** for the per-frame sky itself (`MEASURED`, Compose best
  practices).

### 5.2 What makes it feel magical (and the published guidance for each)

- **Continuous colour, discrete labels.** The sky must be a continuous function of sun altitude; the
  "civil/nautical/astronomical" text is a *label* computed from thresholds (−6/−12/−18°), never a
  keyframe. A discontinuity at a stage boundary is the exact "form with a button" defect.
- **Continuous object motion.** Sun, moon, stars and satellites move along their arcs at a rate set by
  sidereal time; as the slider moves, everything moves consistently. This is what "scrubbing the sky"
  means, and it is free once positions are a function of the same time value.
- **Do not ease the drag.** Material's own guidance for a scrubber is that the thumb tracks the finger
  1:1; easing is for **release / programmatic change / state feedback** (`MEASURED`, Material motion
  synthesis: "Keep frequent interaction feedback short and responsive… do not animate the drag itself").
  Apply easing to a *tap-to-advance* or a *release-settle*, not to the live drag.
- **Easing tokens, concrete.** For settling and for auto-advance: Material 3 **emphasized-decelerate =
  `cubic-bezier(0.05, 0.7, 0.1, 1)`**, durations **short4 = 200 ms**, **medium2 = 300 ms**,
  **medium4 = 400 ms** (`MEASURED`, material-web motion tokens `_md-sys-motion.scss` and Flutter
  `motion.dart`). Material 3 calls the **expressive** motion scheme "the recommended motion scheme for
  prominent UI elements and hero interactions" (`MEASURED`, `MotionScheme` API reference) — this hero
  screen *is* the prominent/hero case, so the expressive scheme is the correct one to use here, with the
  caveat above that the drag itself stays linear.
- **Sub-second, and short for feedback.** Material's classic guidance: adjust duration to distance
  travelled; keep frequent-interaction feedback short (`ON RECORD`, Material duration & easing).
- **Motion blur is not the right tool.** The papers do not use it, and introducing blur before a
  framebuffer that is re-quantised worsens banding (the Unreal blue-noise thread notes noise before TAA
  can smear). Prefer *temporal coherence* (a stable, screen-space dither that does not crawl — Sundog's
  `noise` is "tied to screen space to avoid motion aliasing", `MEASURED`) over per-frame blur.
- **Starfield precompute.** Star screen positions are a rotation of a fixed catalogue; recompute the
  rotation each frame but never re-fetch or re-sort the catalogue. Satellites are a two-line-element
  propagation per frame, cheap; draw passes as **short trail segments**, not dots.

### 5.3 Anti-patterns to ban

- A slider whose value recomposes the whole screen (the 238-recomposition failure above).
- A "stage" enum driving the sky colour (popping at boundaries).
- Redrawing the Milky Way texture or the star catalogue every frame.
- Easing the drag.
- Animating colour in gamma space (visible hue drift); interpolate in linear light (the comparison
  paper's spectrum figures show the perceptual difference is real, `MEASURED`).

---

## 6. Reference apps — what specifically makes each look premium

Five apps, and the *specific* mechanism, not "it looks nice". Where the claim is about their own
marketing words I quote them and label `ON RECORD` (read from the listing/site this run); where the
claim is about what the pixels do I have not personally opened, I label `INFERRED (screenshots/listing)`
and say so, per the brief.

**1. PhotoPills** — *photography planning app, paid, Android+iOS.* What it does that reads premium
(`MEASURED`, its own Play listing and user guide, read this run): it is **"The First 2D Map-Centric
Planner: Sun, Moon, Milky Way"** and it has **"3D Augmented Reality: Sun, Moon, Milky Way, Celestial
Equator, Polaris, DoF, FoV"**. Its Milky Way editing is a dedicated **"Night AR"** mode and a **2D
Milky Way layer** with a three-state sun/moon/Milky-Way map toggle; the guide walks a user through
placing an observer pin and reading the Galactic Center's position, path and arch orientation. The
premium cue is **planner legibility** — clean panels, alignment finders, a *purpose-built* Milky Way
planner — not a photoreal sky. Note the app's own caveat: AR "make[s] use of your device's compass.
Some devices may not have a compass" (`MEASURED`). *What its rendered sky does internally is
`UNVERIFIED`.* **Lesson for us:** PhotoPills wins on *clarity of the plan*; our wedge is a *rendered
scene*, so we must not copy its overlay-only AR and call that hero.

**2. The Photographer's Ephemeris (TPE)** — *light-planning app.* `MEASURED` from its site: it
advertises **"a 3D visualization of the band of the Milky Way"**, "3D maps and AR", "**TPE 3D** … a three
dimensional map of the landscape lit by **realistic directional light and shadows**", and on desktop "a
**3D Celestial Sphere** with visual search". Premium cue: **terrain-lit 3D with real shadows and
sightlines**, i.e. *the horizon is a real 3D thing*, plus a celestial sphere you can search. This is the
closest incumbent to "your own horizon + real sky", but it is a *map/3D* rendering, not a hand-drawn
silhouette. **Lesson:** our drawn-horizon is a genuine differentiator — nobody we found renders *the
user's own silhouette* as the scene's foreground.

**3. Star Walk 2** (Vito Technology) — *stargazing app.* `MEASURED` from its App Store/Play copy: it
markets **"All-new stunning design"**, **"New interface in vibrant colors for retina displays"**,
**"Atmospheric visual and sound effects, stirring soundtrack"**, **"Exclusive handcrafted artwork for
constellations"**, and **"Incredible 3D models of celestial bodies"**. Its predecessor **Star Walk won
an Apple Design Award (2010)** and Star Walk for iPad was a **2013 Webby nominee for Best Visual
Design** (`ON RECORD`, Webby Awards page read this run; Apple Design Award via the app's own copy and
press). Premium cue: **handcrafted constellation art as the visual language**, plus atmospheric layers
and sound. **Lesson:** a *bespoke illustrated layer* (here: the user's own horizon + a distinctly
rendered sky) is what separates a premium sky app from a star atlas. *Internal rendering: `UNVERIFIED`.*

**4. Sky Guide** (Fifth Star Labs) — *stargazing app.* `ON RECORD` (GeekWire, visitseattle blog, Fifth
Star Labs site, all read this run): it **won a 2014 Apple Design Award**; reviews cite its **"gorgeous
look"**; founder Nick Risinger's **real astrophotography** is the backdrop ("Risinger's superb
photography"); it supports **"magical AR on iPhone 16 Pro"** and **"ultra-responsive 120 FPS deep sky
browsing"** (`MEASURED` — its own site). Premium cue: **real photographic sky + 120 fps smoothness**,
and time controls that "rewind or fast-forward the sky" with day stepping (`ON RECORD`, its support
page). **Lesson:** two of the premium cues are literally on our list — *use real sky data* (a licensed
photographic/baked backdrop) and *be smooth* (120 Hz-capable rendering, no jank).

**5. Stellarium / Stellarium Mobile** — *planetarium.* `MEASURED` from stellarium.org features list:
**"very realistic atmosphere, sunrise and sunset"**, **"realistic Milky Way"**, **"star twinkling"**,
plus refraction and 3D scenes. Stellarium Mobile's copy: **"Simulate landscape and atmosphere with
realistic sunrise, sunset and atmosphere refraction"** (`MEASURED`, Play listing). Premium cue:
**physically-based atmosphere + realistic Milky Way + twinkle + refraction** — i.e. it does exactly the
numerate-things we listed, and it is the desktop reference the Milky-Way bake was optimised against.
**Lesson:** everything we want to do is *demonstrably shippable* — Stellarium has shipped it for years;
our differentiator is the **scrubbable, drawn-horizon, mobile-native** framing, not the physics.

**Also seen, not in the five:** **Sun Surveyor** (`MEASURED`, its site) markets a **"Live View —
Augmented reality puts you in the center of a sun, moon and milky way path simulator"** and a **"3D
Compass"**; **ISS Real-Time Tracker 3D** markets a **"SkyDome 3D overlay"** and a photoreal globe
(`ON RECORD`). Both reinforce that *the 3D sky view is the premium surface* in this category.

**Cross-cutting read (`INFERRED`):** the incumbents split into (i) **planner-first** apps (PhotoPills,
TPE, Sun Surveyor) that win on information design and an overlay, and (ii) **scene-first** apps
(Star Walk 2, Sky Guide, Stellarium) that win on a beautiful rendered/photographic sky. **Our hero
screen is a scene-first idea** — so the visual bar is set by group (ii), and the differentiator nobody
in either group has is a **rendered scene whose foreground is the user's own drawn horizon, scrubbed
through real twilight.**

---

## 7. Synthesis — the concrete recipe the evidence points to

1. **Sky radiance**: bake **Bruneton 2008** (Rayleigh + Mie multiple scattering + **ozone**) at build
   time into a small **sun-altitude × view-elevation LUT**; sample it in the shader. Fall back to
   **Hošek–Wilkie 2012** for the daylit portion if the LUT is too coarse. Reimplement from the papers.
2. **Blend the day↔night transition by sun altitude** with a smooth function (Müller's exact approach),
   never a stage switch.
3. **Stars**: viewpoint-aligned billboard sprites, magnitude → size *and* brightness, intensity PSF
   `T(l)=2l³−3l²+1`, a glare halo only above a control magnitude, minimum radius √2 px, extinction and
   scintillation as functions of optical path length, colour from B−V → temperature → sRGB.
4. **Milky Way**: a **self-baked** luminance+colour texture from a permissively-licensed catalogue
   (bake, don't ship CC BY-SA), **bright stars removed**, blurred so it reads as a structured band with a
   real rift.
5. **Moon**: a viewpoint billboard virtual sphere with phase, a Lunar-Lambert/Hapke–Lommel–Seeliger
   shading term and faint earthshine on the dark limb, plus a glare when bright.
6. **Horizon**: hard occlusion; ground darkened; **antisolar Earth-shadow wedge** from Lynch's geometry;
   warm alpenglow band feathering over the ridge at sunset.
7. **Antisolar band structure** at twilight: US/BV/BB/HB, with the BV↔BB boundary moving ~8× the sun's
   altitude and brightest sky *above* the horizon.
8. **Colour pipeline**: linear-light interpolation, deliberate tonemap + white balance, and **blue-noise
   (or interleaved-gradient) dither at ±0.5/255** to kill twilight banding.
9. **Interaction**: `State<Float>` read only in the draw phase; M3 expressive settling
   (`cubic-bezier(0.05,0.7,0.1,1)`, ~200–400 ms) on release only; drag stays 1:1; catalogue rotated, not
   re-fetched.

---

## 8. The "stunning" bar — checkable, so a reviewer can point at it

Each row is something a reviewer can point at **in a single screenshot or a short scrub**, not an
adjective. Sources are the sections above.

| # | The scene must… | How a reviewer checks it | Source |
|---|---|---|---|
| B1 | show **no visible colour banding** at twilight | zoom a twilight screenshot; no flat stair-step bands across the gradient | §1.3 (dither), §1.1 |
| B2 | put its **brightest band above the horizon**, with the last few degrees *dimmer* | vertical brightness scan: peak at ~5–15° altitude, drop at the horizon | §1.4 (Lynch, Lee) |
| B3 | show **four antisolar twilight bands** at sun ≈ −4° | one screenshot, point to blue upper sky, pink Belt of Venus, blue band, horizon band | §1.4 |
| B4 | have the **Earth-shadow (blue) band rise ≈8× faster than the sun altitude** | scrub −6°→−3°; the blue/pink boundary drops dozens of degrees | §1.4 |
| B5 | carry a **purple/violet** component at twilight, not only orange→black | colour picker in the twilight sky shows a magenta-leaning hue | §1.1 (ozone) |
| B6 | be **pale and desaturated**, not a saturated cartoon | sample CIE/saturation of the bands; they sit near achromatic | §1.4 |
| B7 | render **stars, not dots**: a few bright with glow, many faint sub-pixel | one screenshot shows a magnitude hierarchy and halos on the brightest | §2.1 |
| B8 | **twinkle more near the horizon** than at the zenith | two-frame diff shows larger amplitude at low altitude | §2.1(d) |
| B9 | show **reddening + extinction** near the horizon | a bright star low in the sky is dimmer and redder than the same star high | §2.1(e) |
| B10 | show the **Milky Way as a structured band with a dark rift** | point at the dust lane / Great Rift, not a uniform smear | §3 |
| B11 | have **no doubled bright stars** in the band | no bright star appears twice at mismatched positions | §3.2 |
| B12 | **occlude the sky with the silhouette** hard, and darken the ground | no sky gradient bleeds through the silhouette; below it is ground | §4.1 |
| B13 | show a **warm alpenglow band over the ridge** at sunset | warm backscatter visible above the ridge, feathering slightly over its top | §4.3 |
| B14 | render the **moon with phase + earthshine + a glow**, and a **sun disc with a halo** | a crescent moon shows a faint dark-limb glow; the sun is not a hard circle | §1.2, §2.1 |
| B15 | draw **satellite passes as moving trails** above the stars | a pass is a line segment that advances as the slider moves | §5.2 |
| B16 | keep **astronomical darkness not pure black** at sun = −18° | at −18° the sky retains airglow/zodiacal/starlight, not #000000 | §1.4 (US/HB visible all night) |
| B17 | be **continuous** across the whole range | a full day→night sweep shows no colour pop or stage jump | §5.2 |
| B18 | **scrub with no jank** | 60 fps while dragging; the value changes with the finger | §5.1 |
| B19 | be **reversible** | scrubbing backwards reproduces the same colours exactly | §5 (pure function) |
| B20 | look **consistent between the app and the store pack** | a screenshot matches the shipped palette and scene | factory rule `visual-identity.mdc` |

B1, B2, B5, B7, B10, B12, B16 and B17 are the ones that separate "a sky" from "a *rendered* sky" and
should be the first-pass review focus. None of them is a matter of taste; each is a thing on a
screenshot.

---

## 9. CLAIM / LABEL / EVIDENCE ledger

| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| Preetham 1999 = SIGGRAPH '99 pp. 91–100, DOI 10.1145/311535.311545 | ON RECORD | `dl.acm.org/doi/10.1145/311535.311545` read this run |
| Hošek–Wilkie 2012 = SIGGRAPH 2012, ACM TOG 31(4) pp. 95–104, DOI 10.1145/2185520.2185591 | ON RECORD | SIGGRAPH History Archive + cgg.mff.cuni.cz preprint + OpenAlex, read this run |
| Hošek–Wilkie improves sunsets/high turbidity and is a drop-in replacement for Preetham | ON RECORD | Hošek–Wilkie abstract, quoted verbatim, read this run |
| Bruneton–Neyret 2008 = CGF 27(4) pp. 1079–1086, EGSR 2008, DOI 10.1111/j.1467-8659.2008.01245.x | ON RECORD | ACM DL + INRIA HAL PDF + EG digital library, read this run |
| Bruneton supports twilight sky colour, multiple scattering, aerial perspective, Earth shadows, all viewpoints | ON RECORD | abstract, read this run |
| Bruneton reference implementation licence is BSD-3-Clause | MEASURED | `github.com/ebruneton/precomputed_atmospheric_scattering` page showed "License: BSD 3-Clause" |
| Bruneton 2017 reimplementation adds ozone support | MEASURED | `ebruneton.github.io/precomputed_atmospheric_scattering` page text |
| Complexity/RMSE table (Preetham 88.1, Hošek 41.5, Nishita93 26.6, Haber 14.7, Bruneton 11.3, render `O(1)`) | MEASURED | arXiv 1612.04336 Table II + per-model text, grepped in this run |
| The comparison paper concludes less-approximated models are perceived more realistic, and tonemap/white balance matter | MEASURED | arXiv 1612.04336 Table I discussion, read in this run |
| Müller/Engel/Döllner 2012 = VMV 2012 pp. 55–62, DOI 10.2312/PE/VMV/VMV12/055-062, single-pass day-night sky with stars/moon/Milky Way/clouds | MEASURED | full paper text fetched and read this run |
| Müller: point sprites cluster at centre at wide FoV; use viewpoint-aligned billboards | MEASURED | paper §4, quoted verbatim |
| Müller intensity PSF `T(l)=2l³−3l²+1`, `l∈[0,1]`, `V_T=1.167`, `Δm=2.512^(m_a−m)`, `q≥√2` px, `c_q≈4×10⁻⁷` | MEASURED | paper §4.2, read verbatim |
| Müller glare PSF exact constant `G(l)=√64·l` | UNVERIFIED | OCR of §4.2 ambiguous; re-read a clean PDF copy of the paper to settle |
| Müller: <4 % frame cost cloudless day/night, 2.0 s atmosphere precompute (2012 desktop GPU) | MEASURED | paper Table 1 + §6.1 |
| Müller: remove bright stars from the star-map texture to avoid doubling | MEASURED | paper §4.4, quoted verbatim |
| Müller star-colour pipeline B−V→temperature→Planckian→CIE→sRGB (Olson 1998, Krystek 1985) | MEASURED | paper §4.3 |
| Müller scintillation ∝ optical path length, grows toward horizon | MEASURED | paper §4.3.2 |
| Gaia Sky computes star pseudo-size from apparent magnitude via luminosity `L=L₀·10^(−0.4·M_o)` + sqrt; billboards `α=atan(p/d)`; "brightness power" widens bright/faint | MEASURED | Gaia Sky 3.4 Star-rendering docs fetched this run |
| Gaia Sky applies extinction data before magnitude→size | MEASURED | same docs |
| DLR 2025: hardware point smoothing is vendor-specific; small primitives stall GPU; they use a compute rasteriser | MEASURED | `elib.dlr.de/215930` PDF fetched this run |
| A star is an unresolved PSF (Airy/bloom + spikes), so a flat circle is wrong; crop PSF by brightness, inverse-square falloff | ON RECORD | tiffnix "Rendering star fields in 3D" + CG StackExchange thread, read this run |
| Lynch et al. 2017 = Applied Optics 56(19) G156–G168, DOI 10.1364/AO.56.00G156; four antitwilight bands US/BV/BB/HB | MEASURED | paper PDF read in full this run |
| Belt of Venus moves ≈8× the sun's altitude; BV/BB transition width 8°→0.9° as ε→0 | MEASURED | Lynch §4/§6, read verbatim |
| Brightness maximum above horizon, dimmer toward horizon (optical-depth unity ~15° typical, ~5.3° very clear) | MEASURED | Lynch §4.A/§5/Appendix A |
| Twilight colours are pale/unsaturated (near achromatic in CIE) | MEASURED | Lynch §4.B (citing Lee) |
| Refraction lifts sun/moon ≈0.58° at horizon | MEASURED | Lynch §7 (citing Bennett 1982) |
| Twilight range 0 to −18°, up to −23° in turbid air; US/HB faintly visible all night (starlight/moonlight/airglow) | MEASURED | Lynch §1, §5.C/§5 |
| Civil/nautical/astronomical twilight boundaries −6°/−12°/−18° | ON RECORD | Sky & Telescope "Tiptoe Into The Twilight Zone", read this run |
| Belt of Venus is the twilight form of alpenglow / backscattered reddened sunlight | ON RECORD | Wikipedia "Belt of Venus", read this run |
| Ozone (Chappuis bands) contributes to twilight colour | MEASURED | Lynch §5.A, quoted verbatim |
| Procedural Milky Way = exponential falloff + Perlin displacement + subtracted central shadow + Voronoi grain | ON RECORD | "Procedural Milky Ways" (artsnscience.eu), read this run |
| Gaia Sky procedural galaxies use FBM, additive star/gas and subtractive dust particles | MEASURED | Gaia Sky procedural-galaxies docs fetched this run |
| Catalogue-baked Milky Way: bin Tycho-2, Gaussian blur, B−V tint, Planck radiance, `exp(−k·τ)` extinction; constants recorded | ON RECORD | SharpAstro/tianwen `generate_milkyway.cs` + `bake_milkyway.ps1`, read this run |
| Gaia DR2 colour panorama commonly shared under CC BY-SA 3.0 IGO | MEASURED | kam-ui "Milky Way" credit line, read verbatim |
| CC BY-SA is forbidden for the factory's paid closed-source apps | ON RECORD | `docs/research/2026-10-02-ui-icon-libraries-and-licensing.md` §1.2–1.3 (read this run) |
| PhotoPills markets "First 2D Map-Centric Planner: Sun, Moon, Milky Way" and 3D AR incl. Milky Way | MEASURED | its Google Play listing + user guide, read this run |
| TPE markets a 3D Milky Way band, 3D maps with realistic directional light+w shadows, 3D Celestial Sphere | MEASURED | photoephemeris.com, read this run |
| Star Walk 2 markets "stunning design", "vibrant colors for retina", "atmospheric visual and sound effects", "handcrafted artwork"; Star Walk won Apple Design Award; iPad version 2013 Webby nominee Best Visual Design | MEASURED / ON RECORD | App Store + Play copy and Webby Awards page, read this run (award via the app's own copy + Webby page) |
| Sky Guide won a 2014 Apple Design Award; uses Risinger's real astrophotography; "magical AR", "120 FPS" | ON RECORD / MEASURED | GeekWire + visitseattle + fifthstarlabs.com, read this run |
| Stellarium features "very realistic atmosphere, sunrise and sunset", "realistic Milky Way", "star twinkling" | MEASURED | stellarium.org + Stellarium Mobile Play listing, read this run |
| Sun Surveyor markets a "Live View" AR "path simulator" and "3D Compass" | MEASURED | sunsurveyor.com, read this run |
| M3 emphasized-decelerate = `cubic-bezier(0.05,0.7,0.1,1)`; short4=200ms, medium2=300ms, medium4=400ms | MEASURED | material-web `_md-sys-motion.scss` + Flutter `motion.dart`, read this run |
| M3 "expressive" scheme is recommended for prominent/hero interactions | MEASURED | `MotionScheme` API reference, read this run |
| Material: adjust duration to distance; keep frequent-interaction feedback short; do not ease the drag | ON RECORD | Material duration/easing docs, read this run |
| Compose: reading `State<Float>` in `drawBehind`/`drawWithCache` invalidates only the draw phase; passing `Float` recomposed 238× in the case study | MEASURED | developer.android.com Compose phases + proandroiddev case study, read this run |
| `drawWithCache` caches Brushes/Paths while size unchanged | MEASURED | `drawWithCache` reference, read this run |
| Dithering twilight with ±0.5/255 screen-space noise removes banding; blue noise preferred over Bayer; blue noise before tonemap/TAA | ON RECORD | Reassembly blog + Sundog Software + Bart Wronski + Unreal thread, read this run |
| Which of these apps' *internals* match any technique | UNVERIFIED | only screenshots/listing copy were read; opening the apps is the check |
| Preetham/Hošek models as *shipped* in any competitor | UNVERIFIED | no competitor source inspected; not claimed |
| Which exact sky model produces the best phone result | INFERRED | follows from the RMSE/complexity table and the composition paper; would be settled by a bake-and-compare on the A40 |
| Müller glare-PSF constant | UNVERIFIED | re-read a clean PDF to settle |

**Honest limits of this document.** It is a literature and reference-app review, not an implementation
and not a measurement of any competitor's rendering. Every technique is labelled with the publication
that describes it; every app claim is either the app's own words (quoted) or explicitly marked as
inferred from screenshots. No code was copied, no dependency added, and no asset was downloaded. The one
place the recommendation leans on my judgement rather than a measured number — *which* model to bake —
is flagged `INFERRED` and is precisely the thing a phone-side bake-and-compare would settle.
