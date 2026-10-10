# Night scene

## Purpose

Lumintra's hero surface: one scrubbable scene that answers, at a glance, what the sky will actually
look like at a chosen spot on a chosen night — the real twilight colour above the user's own drawn
horizon, the stars and the Milky Way where they really lie, the Moon in its real phase, a satellite
about to cross the lens, and whether the chosen target fits the frame. It is computed on the device,
it needs no server, and it is readable in the dark without destroying the night vision a photographer
went outside to use.

## ADDED Requirements

### Requirement: The scene is one computed frame of the real sky

The Night screen SHALL render from a single `SceneFrame` (`domain/SceneState.kt`) that describes
exactly one instant — the observing spot, the absolute UTC instant, the Sun's altitude and azimuth,
the Moon's state, the chosen lens's field of view, the selected target (or none), and the satellites
above the horizon — and SHALL draw only from that value. Two renders of the same `SceneFrame` at the
same viewport size SHALL produce identical pixels. The scene SHALL NOT read a wall clock, the device
location, or network state inside its draw, and SHALL NOT draw anything the `SceneFrame` does not
carry.

The scene SHALL use the horizontal coordinate convention **azimuth 0°…360°, measured clockwise from
geographic north, increasing to the right across the screen; altitude −90°…+90°, positive above the
horizon**. Screen `x` SHALL increase with azimuth and SHALL NOT be mirrored by layout direction.

The scene rect SHALL show the lens frame's field of view with a **context margin of 50 % of the lens
FOV on each side** — i.e. the canvas horizontal field is `2.0 ×` the lens horizontal FOV and the
canvas vertical field is `2.0 ×` the lens vertical FOV. With the default lens preset (24 mm,
73° × 53°, 30 s exposure) the canvas SHALL show **146° × 106°**. The projection SHALL be
equirectangular within the canvas window:

```
x(az, alt) = W · ( wrap180(az − A_c) + F_h / 2 ) / F_h
y(az, alt) = H · ( A_top − alt ) / ( A_top − A_bottom )
A_top      = min( H_c + F_v / 2 , 90° )
A_bottom   = H_c − F_v / 2
```

with `A_c` the canvas centre azimuth, `H_c` the canvas centre altitude, `F_h`/`F_v` the canvas fields,
and every projected point clamped to the scene rect. `wrap180()` maps an azimuth difference into
`[−180°, +180°)`, so a body behind the viewer never projects onto the screen.

The canvas SHALL be centred as follows. With **no target selected**, the frame centre altitude SHALL
be `H_c = F_v / 2 − 4°`, so the horizon falls **4° above the scene rect's lower edge**, and the centre
azimuth SHALL be **180° (south) north of the equator and 0° (north) south of it**, so the antisolar
meridian is centred at local midnight. With **a target selected**, the frame centre SHALL move to that
target's instantaneous `(azimuth, altitude)`, clamped so the canvas window stays inside
`altitude ∈ [−10°, +90°]`, and a named control SHALL return the frame to the horizon-centred default.
The scene's viewing azimuth SHALL be **stated on screen**, never inferred from layout direction.

Layers SHALL be drawn in exactly this order, each into one Compose `Canvas`; the table is also the
statement of each layer's input:

| Order | Layer | Drawn by | Input |
|---|---|---|---|
| 1 | Sky field | the LUT row strip (fallback) or the AGSL shader (API 33+) | solar altitude `ε`, view altitude, view azimuth, the twilight LUT |
| 2 | Night floor (airglow, zodiacal light, integrated starlight) | one additive vertical gradient, or a shader addend | `ε`, view altitude |
| 3 | Milky Way composite | `drawVertices` over a 48 × 24 quad mesh with an `ImageShader` | the baked galactic map, the galactic→screen rotation, `ε` |
| 4 | Star field | batched `drawPoints` with `BlendMode.Plus`, **at most 8 calls** | the star table, the sky rotation, extinction per star |
| 5 | Deep-sky targets and constellation figures | one `Path` per object | the catalogue, the sky rotation |
| 6 | Moon | `drawImage` of the Moon texture clipped to the disc `Path`, plus a phase `Path` and a radial-gradient glow | the ephemeris, the Moon texture |
| 7 | Satellite trail | one `Path` with a fading tapered stroke, split at horizon crossings | the pass's position history |
| 8 | Earth-shadow wedge | **not an overlay** — applied inside layer 1 | `α_s(ε)`, view azimuth |
| 9 | Alpenglow band | one additive radial gradient, drawn **before** layer 10 | `ε`, the Sun's azimuth, the ridge line |
| 10 | Horizon silhouette | one opaque `Path` built from the user's `HorizonProfile` | the profile |
| 11 | Lens FOV frame | a hairline rectangle and four corner ticks | the lens FOV, the frame centre |
| 12 | Target marker | the marker `Path` drawn **before layer 10**, and the label drawn **after** it | the target's position |
| 13 | UI chrome | Compose `MaterialTheme` surfaces **outside** the scene rect | the scene's computed facts |

The Earth-shadow wedge SHALL NOT be drawn above the star field. The Earth's shadow dims scattered
sunlight and does not dim starlight; a translucent wedge painted over the stars would extinguish
exactly the darkest sky the user came for. The approved mockup paints the wedge after the stars and is
**wrong** for real rendering.

The scene's colours SHALL come from the committed twilight LUT and the slot's own
`ui/theme/Color.kt`; the mockup's hand-authored sky stop list SHALL NOT ship (E86). The scene's darkness
SHALL be described with `domain/SkyGrade.kt`'s `SkyGrade` and SHALL NOT be reported as a Bortle class,
because no light-pollution raster ships in this slot.

#### Scenario: The same frame renders the same pixels

- **WHEN** the same `SceneFrame` is rendered twice at the same viewport size and both results are compared pixel by pixel
- **THEN** every channel of every pixel is identical, because the scene is a pure function of the frame and the viewport

#### Scenario: Nothing but the frame is read inside the draw

- **WHEN** the scene's draw path is read for clock, location or network access
- **THEN** it contains no `System.currentTimeMillis()`, no `Location`, and no network call, so moving the device or changing the system clock cannot change a rendered frame

#### Scenario: Azimuth increases to the right and is not mirrored

- **WHEN** the scene is rendered with the system layout direction set to a right-to-left locale
- **THEN** the horizon silhouette, the stars and the FOV frame occupy the same screen positions as in a left-to-right locale, because the horizontal axis is an azimuth axis and mirroring it would put the user's western ridge on the wrong side (E88)

#### Scenario: The lens frame's angular width is the lens's own

- **WHEN** the rendered field-of-view rectangle is measured in degrees on the default 24 mm preset
- **THEN** its horizontal angular width is 73° within 2 %, its vertical width is 53° within 2 %, and the canvas rect around it spans 146° × 106°

#### Scenario: The wedge is not painted over the stars

- **WHEN** the scene is rendered at `ε = −4°` with the Earth-shadow edge above the antisolar horizon
- **THEN** the star sprites inside the shadowed region are unchanged in brightness relative to a render at the same `ε` with `α_s` forced to the horizon, because the wedge modulates layer 1 rather than covering layer 4

#### Scenario: The horizon line is four degrees above the lower edge by default

- **WHEN** the scene is opened with no target selected and the default lens
- **THEN** the silhouette's average line sits 4° above the scene rect's lower edge, and the readout states the viewing azimuth

#### Scenario: Selecting a target centres the frame on it

- **WHEN** a target at azimuth 84° and altitude 43° is selected
- **THEN** the canvas centre moves to that direction, the target marker renders at the scene rect's centre within 1 px, and the named "back to the horizon" control restores the default centring

#### Scenario: A body behind the viewer never appears on screen

- **WHEN** a target at azimuth 350° is rendered while the canvas is centred on azimuth 180°
- **THEN** no marker and no label for it appears anywhere in the scene rect

---

### Requirement: The sky is evaluated from the twilight LUT, never a hand-picked gradient

The sky SHALL be evaluated from the two committed twilight LUTs at render time: **61 solar altitudes
(+10.0° → −20.0°, step 0.5°) × 91 view elevations (0.0° → 90.0°, step 1.0°), three float16 linear-light
channels per cell, stored relative to a per-column exposure, ≈ 67 KB for both tables**. Interpolation
SHALL be bilinear in `(ε, h)` in linear light, performed once, with the azimuth blend between the
antisolar and solar tables applied after both samples and before tone mapping. **No interpolation
SHALL be performed on a gamma-encoded value, and no 8-bit value SHALL exist anywhere in the pipeline
before the tone map.** The LUT SHALL NOT be computed at runtime and SHALL NOT be computed on first
launch.

**A Sun below the table's darkest band SHALL be clamped, never extrapolated (E94).** The table's
solar-altitude range is **+10.0° → −20.0°** at **step 0.5°** — **61 rows, index 0 … 60**, the darkest
band being row 60 at **−20.0°**. For any computed solar altitude at or below **−20.0°** — which a deep
polar night reaches — the row index SHALL be clamped into **[0, 60]** and the row fraction pinned to
row 60, so no negative index, no index past row 60 and no zero-denominator division is reachable. A
unit test SHALL drive the sampler at `ε = −20.5°`, `−30°`, `−60°` and `−90°` and assert that each
resolves to row 60 with a finite, non-negative weight and no thrown exception. The rendered result at
such an instant SHALL be the darkest sky the table can express — pixel-identical to a render at
**ε = −20.0°** — and SHALL NOT be a black screen, a truncated sky or an invented gradient.

The last stages SHALL be, in this order and exactly once: white balance in linear light, the ACES
filmic tone map, the sRGB piecewise transfer (IEC 61966-2-1, not a 2.2 power), a ±0.5/255 tiled
64 × 64 blue-noise dither added in the encoded domain, and 8-bit quantisation. The fallback path SHALL
materialise the LUT as a **one-pixel-wide strip whose row count equals the scene's device-pixel
height**, drawn 1:1; a stretched, filtered bitmap gradient SHALL NOT be used.

#### Scenario: No visible banding in the twilight

- **WHEN** a 1080 × 2340 capture is taken at `ε = −4°` and a vertical luminance scanline is read at the antisolar azimuth
- **THEN** no step between adjacent rows exceeds **2/255** ([O#1])

#### Scenario: Four distinguishable antisolar bands

- **WHEN** a reviewer looks at one screenshot at `ε = −4°`
- **THEN** a blue upper sky, a pink Belt of Venus, a blue band and a distinct horizon band are each pointable, and their CIELAB hue angles differ pairwise by **≥ 20°** ([O#3])

#### Scenario: Purple is present and the palette stays pale

- **WHEN** the colour at 15° elevation on the antisolar side is measured at `ε = −4°`
- **THEN** its CIELAB hue is in **[280°, 330°]** with chroma **C\* ≥ 6** ([O#5]); and every twilight band between 15° and 40° elevation has **C\* ≤ 25** ([O#6])

#### Scenario: Brightest band above the horizon

- **WHEN** the vertical luminance profile of the scene is scanned
- **THEN** it peaks between **5° and 15°** above the horizon and is **≥ 15 % dimmer at 1°** than at the peak ([O#2])

#### Scenario: The sweep has no keyframe

- **WHEN** the scrub is swept `0 → 1` at step 0.002 and consecutive frames are compared
- **THEN** no colour step larger than **3/255** appears anywhere in the range, at any phase boundary ([O#7])

#### Scenario: Astronomical darkness is not black

- **WHEN** the scene is rendered at `ε = −18°`
- **THEN** the zenith RGB lies between **(2, 3, 8)** and **(12, 14, 28)**, and **no sky pixel is `(0, 0, 0)`** ([O#27])

#### Scenario: A Sun below the darkest band clamps to the darkest sky (E94)

- **WHEN** the scene is rendered on a deep polar night at a solar altitude of `ε = −35°`, below the table's darkest band of **−20.0°**
- **THEN** the sampler resolves to row 60, the frame is pixel-identical to a render at `ε = −20.0°`, and no negative index, no index past row 60 and no division by zero occurs

#### Scenario: The darkest-band clamp is covered by a unit test (E94)

- **WHEN** the LUT sampler's unit test is read
- **THEN** it evaluates solar altitudes below the table's range — at least `−20.5°`, `−30°` and `−90°` — and asserts that each resolves to row 60 with a finite, non-negative weight and no thrown exception

#### Scenario: The dither is doing its job

- **WHEN** the standard deviation of the residual after subtracting a 32 × 32 box blur from a flat twilight area is measured
- **THEN** it is **≥ 0.3/255** with the dither enabled and **= 0** with it disabled ([O#40])

#### Scenario: No 8-bit value exists before the tone map

- **WHEN** the sky pipeline's types are inspected
- **THEN** the tone map takes a `FloatArray` and returns a `ByteArray`, and no intermediate buffer is a `ByteArray` ([O#41])

#### Scenario: The gradient is evaluated, not stretched

- **WHEN** the strip's row count is compared with the scene's device-pixel height
- **THEN** they are equal, so no filtering is applied to the sky, and no banding source is introduced by the transfer curve

#### Scenario: The app's palette is the LUT's palette

- **WHEN** every colour in `ui/theme/Color.kt` that names a sky role is compared with the corresponding LUT endpoint sample
- **THEN** each one agrees to **ΔE00 ≤ 2** ([O#32])

---

### Requirement: The scrub moves the sky one-to-one with the finger

The scrub SHALL be modelled as `SceneScrub` (`domain/SceneState.kt`) over a structural `SceneKey`
(spot id, epoch millis, lens FOV, target id, horizon id) plus a per-frame linear value
`scrub ∈ [0, 1]` where `s = 0` is solar altitude `ε = +6°` and `s = 1` is `ε = +6°` again. The
structural key SHALL live in a `mutableStateOf`; **`scrub` SHALL be read only inside
`Canvas { }` / `drawBehind`**, so a drag invalidates the draw phase only and SHALL NOT recompose or
relayout.

**The drag SHALL NOT be eased.** The value written by the drag SHALL be the value drawn in the same
frame; no `animateTo`, no `Animatable` and no interpolation SHALL sit between the pointer delta and
the drawn frame. Easing SHALL be applied **only on release** (a bounded settle) and on a tap-to-advance,
using `emphasized-decelerate` and a duration of at most 200 ms.

**No allocation SHALL happen inside the draw lambda.** Every `Path`, `Brush`, sprite bitmap and
reusable buffer SHALL be built in `drawWithCache` or pre-allocated and reused, and the scene SHALL
produce one frame per input change rather than one per display refresh.

Everything downstream of `scrub` SHALL be a pure function `SkySnapshot = Sky.ephemeris(key, scrub)`,
unit-testable without a GPU. `derivedStateOf` SHALL be used only to coarsen a label, never for the sky
itself.

**The scrub's span is bounded and stated (E68).** The scrub SHALL resolve only to instants inside the
chosen night — the span between the two `ε = +6°` crossings already stated above, and never more than
**24 hours** in total. Moving the thumb earlier or later from the current instant SHALL therefore be
bounded in **both** directions by that one span: no scrub position SHALL resolve to an instant outside
it, extend the night, wrap to the next one, or leave the slider free-running. The range SHALL be
**stated to the user** rather than left as an invisible limit — the scrub track SHALL label its start
and end with the night's first and last clock times in the spot's zone, and the readout SHALL name the
night being scrubbed. A scrub position that would resolve outside the span SHALL be clamped to the
span's end, in addition to the `[0, 1]` clamp stated below.

**A scrub frame is bounded work, never a full-ephemeris recomputation (E68).** The night's ephemeris
SHALL be computed **once per `SceneKey`** into a bounded set of instants, and a scrub frame SHALL reuse
those computed samples: the frame draws only its own instant from them, so the number of sky
evaluations per frame is bounded by a small constant and independent of the span's length. Recomputing
the whole night's ephemeris, its event list or the timeline strip inside the draw lambda — one full
night per frame, the failure an unbounded slider forces — SHALL be a defect.

**The event list over the span is complete (E68).** Every key time the brief states for the chosen
night SHALL be computed over the whole span and SHALL NOT be truncated by the scrub range. That list
has one home — the `session-brief` capability's *The twilight and key-times table states its exact
definitions and instants* requirement — which this requirement references rather than restating.

On a device under memory pressure the star geometry SHALL be cached in `drawWithCache` keyed by
(viewport, magnitude limit, rotation) and **dropped and rebuilt** on `onTrimMemory` rather than
thrashing, and the drawn star count SHALL be capped by a magnitude limit rather than reduced by
dropping frames. After process death the spot, date, scrub instant, lens, target and horizon SHALL be
restored from saveable state, and the scene SHALL return to the same frame.

#### Scenario: The thumb tracks the finger with no lag

- **WHEN** a drag moves the thumb by a fraction of the strip's width in one frame
- **THEN** the sky drawn in that same frame corresponds to the thumb's exact new position, with no intermediate value and no animation between the pointer delta and the pixels

#### Scenario: A drag does not recompose or relayout

- **WHEN** a scripted drag is run with recomposition counted
- **THEN** the count does not increase during the drag, because `scrub` is read only inside the draw phase

#### Scenario: No allocation inside the draw lambda

- **WHEN** the draw path is inspected for `Path(`, `Brush.`, `ImageBitmap` and array construction
- **THEN** every one of them is created in `drawWithCache` or pre-allocated, and the draw lambda allocates nothing

#### Scenario: The scene is reversible

- **WHEN** the scene is scrubbed `0 → 1 → 0` back to the same value
- **THEN** the sky pixels are bit-identical to the starting render, because the function carries no hidden state ([O#28])

#### Scenario: A release settles and stops

- **WHEN** the finger is lifted after a drag
- **THEN** a settle of at most 200 ms runs and then the scene stops drawing frames entirely

#### Scenario: A tap advance is eased, and the drag is not

- **WHEN** the user taps to advance the scrub rather than dragging
- **THEN** the advance uses the emphasized-decelerate curve over at most 200 ms, while a drag still writes its value directly

#### Scenario: Low memory rebuilds rather than thrashes

- **WHEN** the process reports memory pressure while the scene is on screen
- **THEN** the cached star geometry is dropped and rebuilt once, the drawn magnitude limit is the stated cap, and the scrub remains interactive without dropping frames (E84)

#### Scenario: A rotate does not restart anything

- **WHEN** the device is rotated with the scene on screen and a settle animation running
- **THEN** the same frame is drawn for the same `SceneFrame`, and no animation restarts (E82)

#### Scenario: Process death returns to the same frame

- **WHEN** the process is killed while the scene shows a given spot, date, instant and lens, and the app is reopened
- **THEN** the same spot, date, instant, lens and target are restored and the scene draws that frame rather than a first-launch state

#### Scenario: The scrub stays inside its range

- **WHEN** the drag continues past either end of the strip
- **THEN** the scrub value is clamped to `[0, 1]` and the strip never wraps to the other end of the night

#### Scenario: The span is bounded in both directions and stated on screen (E68)

- **WHEN** the Night screen is opened for a chosen night and the scrub is dragged to each end and then past it in both directions
- **THEN** the thumb stops at the night's first and last instants, those two ends are labelled with their clock times in the spot's zone, the readout names the night, and no position resolves to an instant outside the span

#### Scenario: A scrub frame does not recompute the night's ephemeris (E68)

- **WHEN** a scripted drag is run across the whole span and the sky evaluations per frame are counted
- **THEN** each frame evaluates only the instant it draws — a bounded constant, independent of the span's length — and no frame recomputes the whole night's ephemeris, its event list or the timeline strip

#### Scenario: The night's key times are complete over the span (E68)

- **WHEN** the chosen night's key times are compared with the brief's table
- **THEN** every key time the `session-brief` capability's key-times table requirement lists is present over the whole span, and no event is truncated by the scrub range

---

### Requirement: An idle scene draws no frames

The scene SHALL be driven by input, not by a frame clock: there SHALL be no `withFrameNanos` loop and
no `rememberInfiniteTransition`. A frame SHALL be produced only when the scrub changes, the
`SceneKey` changes, the viewport changes, or a bounded settle is running. A still scene SHALL therefore
produce **zero frames per second**, and twinkle SHALL exist only while frames are being produced.

#### Scenario: Ten seconds of no input produce no frames

- **WHEN** the scene is left untouched for 10 s and the frame counter is read before and after
- **THEN** the frame counter is unchanged ([O#29])

#### Scenario: A still scene has still stars

- **WHEN** the scene is left idle at a fixed instant
- **THEN** no star twinkles, because scintillation is a per-frame phenomenon and there is no frame clock

#### Scenario: The scene stops when it is not visible

- **WHEN** the Night screen is backgrounded or the display is turned off
- **THEN** the scene produces no frames and holds no wake lock (E96)

---

### Requirement: The frame budget on the named device, and the claim is UNVERIFIED until measured

The scrub SHALL be specified against the factory's test device: **Samsung Galaxy A40, `SM-A405FM`,
serial `R58MB3SGZVL`, Android 11 / API 30, 1080 × 2340 @ 480 dpi, Exynos 7904, Mali-G71 MP2, 60 Hz
panel**. The budget for one scrub frame SHALL be **≤ 12 ms** of CPU plus GPU, leaving headroom inside
the 16.67 ms refresh interval.

**The claim "the scrub is smooth on the A40" SHALL be written `UNVERIFIED` until it is measured, and no
document, screenshot, listing or handoff in this slot may state it as a fact before then.** No
frame-time, thermal or battery reading exists for this scene on this device. The measurement SHALL be
taken on the debug APK built from the revision under review, on serial `R58MB3SGZVL`, by running:

```text
adb -s R58MB3SGZVL shell getprop ro.product.model      # must read SM-A405FM
adb -s R58MB3SGZVL shell getprop ro.build.version.sdk  # must read 30, or the run is void
adb -s R58MB3SGZVL shell dumpsys gfxinfo -c com.gexetr.lumintralightplanner reset
adb -s R58MB3SGZVL shell dumpsys gfxinfo com.gexetr.lumintralightplanner framestats
```

against a scripted scrub — drag the strip `s = 0 → 1` and back at ≈ 2 s per pass for 30 s — and the
acceptance is **p95 frame time ≤ 16.7 ms, p99 ≤ 33 ms, and janky frames < 1 %**, with the idle frame
counter unchanged. The battery reading is a separate `UNVERIFIED` claim:
`adb -s R58MB3SGZVL shell dumpsys batterystats --charged com.gexetr.lumintralightplanner` before and
after a 5-minute scripted scrub and a 5-minute idle, accepted at **≤ 2.0 %** and **≤ 0.2 %** of the
3100 mAh cell respectively.

#### Scenario: The device is confirmed before a reading is trusted

- **WHEN** a performance run is started
- **THEN** `ro.product.model` reads `SM-A405FM` and `ro.build.version.sdk` reads `30`; if either differs, the run is void and its readings are discarded rather than reported

#### Scenario: The smoothness claim is published only with its measurement

- **WHEN** the handoff or the store copy is read for a statement about smoothness on the A40
- **THEN** the statement is either absent or accompanied by the `dumpsys gfxinfo` reading and the serial, because until that reading exists the claim is `UNVERIFIED`

#### Scenario: A run that misses the budget is reported as a miss

- **WHEN** a scripted drag produces p95 above 16.7 ms or a jank rate at or above 1 %
- **THEN** the reading is reported as a failure with its numbers, and the handoff does not round it into a pass

#### Scenario: The perf claim names the artifact and the revision

- **WHEN** a frame-time reading is printed
- **THEN** it names the serial, the artifact path and the revision it was taken from, because a reading against a different install is evidence about a different app

**Honest boundary, recorded rather than glossed.** This requirement fixes the numbers a reading must
clear; it cannot prove the numbers have been reached, and no such reading exists yet. What is proved
today is the budget, the device identity and the commands. What is not proved is smoothness, battery
cost or thermal behaviour on the A40 — all three remain `UNVERIFIED`.

---

### Requirement: The horizon line occludes the sky per object class

The user's own drawn silhouette SHALL be stored as a `HorizonProfile` (`domain/HorizonProfile.kt`)
carrying **exactly 360 altitude samples**, one per whole degree of azimuth from 0° to 359°, altitude
in degrees clamped to `[−10°, +90°]`, rendered from the same array the mathematics uses. The scene
SHALL occlude by draw order only — the silhouette `Path` is filled opaque over everything drawn before
it — and SHALL NOT use masking, a stencil, or a depth buffer for the ridge itself.

Occlusion SHALL be per object class:

| Object | Rule |
|---|---|
| A star | culled before it enters the batch when its altitude is below the profile at its azimuth (one array read) |
| A deep-sky target | the **marker** is drawn before the silhouette and is occluded; the **label** is drawn after it and is clamped, so a target below the ridge is labelled against the ridge line with a downward marker |
| The Moon | **cut, not culled**: when the Moon's disc crosses the ridge, the disc is drawn inside the inverse of the ridge clip path, so a partially set Moon is partly hidden |
| A satellite | the trail polyline is **split at every horizon crossing**; no segment is drawn below the ridge |
| The FOV frame | **never occluded** — a viewfinder that a hill cuts in half is a defect, not realism |
| The Milky Way mesh | quads whose four corners are all below the ridge are culled; the rest are drawn beneath the silhouette |

The silhouette fill SHALL be a two-stop vertical gradient, not a flat colour, and its optional tree
glyphs SHALL be part of the same filled path rather than a second colour.

#### Scenario: No sky below the silhouette

- **WHEN** the mean luminance two rows below the ridge is measured at `ε = −2°` and at `ε = −15°`
- **THEN** it is **≤ 6/255** in both cases and varies by **≤ 2/255** between them ([O#17])

#### Scenario: A star behind the ridge is not drawn

- **WHEN** a star whose azimuth's profile altitude is 25° sits at 12° altitude
- **THEN** it produces no drawn point and no sprite footprint

#### Scenario: A target that sets keeps a readable label

- **WHEN** the selected target falls below the drawn ridge
- **THEN** the marker is hidden by the silhouette and the label is anchored to the ridge line with the downward marker, still legible

#### Scenario: A partially set Moon is cut rather than removed

- **WHEN** the Moon's disc straddles the drawn ridge
- **THEN** the part above the ridge renders as a crescent segment of the disc and the part below is hidden, with no full-disc flash

#### Scenario: A satellite trail never crosses the ridge

- **WHEN** a pass polyline is rendered over a ridge that blocks 20° at the relevant azimuths
- **THEN** the drawn trail is split at each crossing and contains no segment below the silhouette

#### Scenario: The viewfinder is not cut in half

- **WHEN** the lens frame is drawn over a silhouette that rises above the frame's lower edge
- **THEN** the frame's rectangle and corner ticks remain fully drawn, because a viewfinder is an interface element rather than a scene element

---

### Requirement: The Earth-shadow wedge and the Belt of Venus are derived from the Sun's position

The Earth-shadow edge altitude `α_s(ε)` SHALL be derived, not authored, from
`α_s = ε + arcsin( (R / 2H) · ε_rad² )` with `R = 6371 km` and `H = 20 km`, floored at `ε` and
taken as the zenith when the argument exceeds unity, evaluated once per solar altitude and baked into
the LUT asset as **61 float32 values**. The wedge SHALL be applied **inside the sky layer** as a blue-
shifted darkening (the surviving light there is long-path Rayleigh-scattered blue), not as a neutral
overlay, and SHALL apply only on the antisolar half of the sky.

The Belt of Venus SHALL be baked into the antisolar LUT as a warm band immediately **above** `α_s`,
with a width shrinking linearly from **≈ 8°** to **≈ 0.9°** as `ε → 0°`, an amplitude proportional to
`max(0, (ε + 6°) / 4°)` capped at 1, and no presence below `ε = −6°`. The band's edge SHALL therefore
move with the Sun and SHALL NOT be a fixed screen-space band.

#### Scenario: The shadow edge rises far faster than the Sun

- **WHEN** the scrub is swept from `ε = −1°` to `ε = −4°` and the row where luminance drops 50 % toward the antisolar horizon is measured
- **THEN** that edge moves from **≤ 6°** to **≥ 50°** of altitude, a ratio of edge motion to Sun motion of **≥ 8** ([O#4], [O#18])

#### Scenario: The shadow band is blue, not grey

- **WHEN** the colour inside the Earth-shadow region is compared with the colour immediately above `α_s` at the same elevation
- **THEN** the shadowed sample is bluer and darker, and the sample above it is the warm Belt of Venus, so the two are distinguishable as bands rather than as a single dimming

#### Scenario: The band is absent while the sky is bright

- **WHEN** the scene is rendered at `ε = −2°`
- **THEN** no Belt of Venus is drawn, because the band begins at `ε = −6°`, and the sentence in the readout does not claim one

#### Scenario: The band's width follows the Sun

- **WHEN** the band's angular width is measured at `ε = −6°` and at `ε = −0.5°`
- **THEN** the first is within 1° of **8°** and the second is within 0.5° of **1°**, moving monotonically between them

#### Scenario: The wedge never covers the stars

- **WHEN** the scene at `ε = −4°` is compared with the same scene whose `α_s` is forced to the horizon
- **THEN** the star sprites are unchanged and only the sky field differs

---

### Requirement: The alpenglow band feathers over the ridge

The alpenglow SHALL be drawn as **one additive `Brush.radialGradient` rectangle, never a blur** —
`Modifier.blur` / `RenderEffect` is not the mechanism, both because its support on API 30 is
unverified and because a full-screen gaussian is a fill-rate trap on the device's GPU. Its intensity
SHALL follow `A_peak · f(ε) · (0.35 + 0.65 · (1 + cos(az − az_sun)) / 2) · K(y)` with
`f(ε) = exp( −((ε + 1.0°) / 4.0°)² )`, peaking at `ε = −1°` and at **≤ 25 % of peak by `ε = −8°`**, and
`K(y)` equal to 1 above the ridge and `(1 − d/8px)²` for `d` device pixels below the ridge top, so the
band is clipped only at `ridge − 8 px` and visibly feathers over the silhouette's edge. The band SHALL
be drawn before the silhouette and SHALL NOT be drawn twice with the Belt of Venus, which is the same
physical backscatter seen on the opposite side of the sky.

#### Scenario: Warm light hugs the ridge

- **WHEN** the scene is rendered at `ε = −1°` and the band above the local sky is measured
- **THEN** a warm band with CIELAB hue **20°–45°** reaches **≥ 12/255** above the local sky and extends **≤ 8 px** above the ridge ([O#19])

#### Scenario: The glow is gone by the time the sky is dark

- **WHEN** `ε` reaches `−8°`
- **THEN** the band has fallen to **≤ 25 %** of its peak ([O#19])

#### Scenario: The scene uses no runtime blur for the soft edges

- **WHEN** the scene's sources are read for `Modifier.blur`, `RenderEffect` or a blur mask filter
- **THEN** none is used, and every soft edge is a gradient stop pattern

#### Scenario: The glow is on the Sun's side

- **WHEN** the band's amplitude is compared between the solar and antisolar halves of the same frame
- **THEN** the solar half is at least 1.9× the antisolar half, because the term is `0.35 + 0.65 · (1 + cos)` 

---

### Requirement: A flat horizon and a horizon that blocks everything are legal and named

A `HorizonProfile` whose samples are all equal SHALL be a legal horizon and SHALL be used by every
calculation as the drawn value, including a flat 0° profile. A spot with **no** profile SHALL be a
distinct state (`hasProfile = false`) that uses 0° and says so once. A profile whose samples are all
+90° SHALL also be legal; every target, satellite and the Moon SHALL then be reported as behind the
horizon with the reason stated once, and the app SHALL offer one non-blocking confirmation, because an
all-90° profile is usually a mis-draw. In both degenerate cases the scene SHALL still render the sky
above the silhouette.

#### Scenario: A flat line is a horizon, not an absence

- **WHEN** a spot carries a profile with every sample at 0°
- **THEN** every calculation uses 0°, the scene draws a flat foreground, and the app does not report a missing profile

#### Scenario: No profile is its own state

- **WHEN** a spot carries no profile at all
- **THEN** the app states `No horizon profile for this spot — using a flat horizon.` once, and the scene draws a flat foreground rather than nothing

#### Scenario: A horizon that blocks everything says so

- **WHEN** a saved profile has every one of the 360 samples at +90°
- **THEN** the app states `Your horizon blocks the whole sky.`, excludes every target and pass from the scene, and offers one non-blocking confirmation to redraw

#### Scenario: A blocking horizon does not blank the sky

- **WHEN** the all-90° profile is loaded
- **THEN** the scene still draws the sky above the silhouette, so the user sees what they drew rather than an empty rect

---

### Requirement: The Milky Way is the real band, rotated to this time and place

The Milky Way SHALL be the ESO `eso0932a` panorama, **baked at build time into an equirectangular
galactic-coordinate map** with bright stars of `V < 3.0` masked and inpainted, a box blur applied to
the unresolved region, and mipmaps generated. It SHALL NOT be shipped raw.

Its placement SHALL be a real coordinate transform, not a decorative rotation:
`M_gal→screen = R_horizontal(φ, LST) · R_gal→ICRS`, where `R_gal→ICRS` is the IAU galactic-to-ICRS
matrix, **re-derived at build time from the published pole and position angle and asserted against the
tabulated matrix to 1 × 10⁻⁶ in a unit test**, and `R_horizontal(φ, LST)` is the **same** rotation the
star field uses. The band SHALL be drawn as a **48 × 24 quad mesh** through `drawVertices` with an
image shader, with quads culled by the scene rect and by the horizon and clipped in screen space at the
rect's edge.

The band's opacity SHALL follow the computed darkness:
`a_mw(ε) = smoothstep(−8°, −14°, −ε)` — **zero at `ε = −8°` and full from `ε = −14°`** — so the band
cannot leak into a sunset. The star layer SHALL be prevented from doubling on the band by two
mechanisms together: the bright stars are masked out of the texture at bake time, and the texture and
the sprites are rotated by the same matrix, so a masked star's sprite lands within **≤ 1 px** of the
hole it left. The faint 2MASS field beyond `V = 6.5` SHALL NOT be drawn by default.

#### Scenario: The band is where the galaxy is

- **WHEN** the scene is rendered for a given spot and instant and the band's centre line is compared with the galactic plane computed independently
- **THEN** the band's centre line lies on the galactic plane within **1°** at the frame centre

#### Scenario: The band is a structured band with a rift

- **WHEN** a luminance scan is taken across the band at the galactic-plane crossing
- **THEN** a local minimum of **≥ 25 % depth** appears between two maxima ([O#14])

#### Scenario: The band follows the darkness, at both ends

- **WHEN** the band's peak luminance above the local sky is measured at `ε ≥ −6°` and at `ε ≤ −14°`
- **THEN** it is **0** in the first case and **≥ 8/255** in the second ([O#16])

#### Scenario: No doubled bright stars

- **WHEN** a masked bright star's screen position is examined at `ε = −15°`
- **THEN** no pixel within **± 4 px** of it has luminance **≥ 30 %** of that star's own core ([O#15])

#### Scenario: The band follows the same rotation as the stars

- **WHEN** the scrub advances by one hour
- **THEN** the band and the star field both rotate by the same sidereal angle, so their relative geometry is unchanged

#### Scenario: The mesh is culled

- **WHEN** a scene with the horizon blocking half the sky is rendered
- **THEN** no quad entirely below the ridge is submitted, and no quad's vertex lies outside the scene rect

---

### Requirement: The ESO credit is visible wherever the band is

The Milky Way panorama's credit **`ESO/S. Brunier`** is mandatory and visible under CC BY 4.0 (source
`https://www.eso.org/public/images/eso0932a/`), and it SHALL travel with the image, unaltered and not
separated from it. It SHALL render:

1. on the **Night scene itself** — wherever the panorama is drawn, the credit **`ESO/S. Brunier`** SHALL
 be rendered **on the band or immediately beside it**, unaltered and not separated from the image, so
 that a credit living only in the `Credits` list does not satisfy this; and nothing on the scene SHALL
 imply an **ESO** endorsement;
2. on the **`Credits`** screen (`ui/settings/CreditsScreen.kt`), reached from `Settings`, naming the
 asset `eso0932a`, the credit `ESO/S. Brunier`, the licence `CC BY 4.0`, the source URL
 `https://www.eso.org/public/images/eso0932a/`, and what the app changed (the reprojection to galactic
 coordinates, the star removal at `V < 3.0`, the blur) — at a contrast ratio of **at least 4.5:1**
 against its background; and
3. on every **exported or shared image that contains the band** — the postcard produced by the `Share`
 capability SHALL carry a legible credit line naming `ESO/S. Brunier` and `CC BY 4.0`.

The credit SHALL be a string resource present in `values/strings.xml` and `values-ru/strings.xml`. The
band SHALL NOT be drawn on any surface that carries no credit.

#### Scenario: The credit is on the credits screen at the measured contrast

- **WHEN** the `Credits` screen is rendered and the credit line's contrast ratio is measured
- **THEN** it is **≥ 4.5:1** and the line reads `ESO/S. Brunier` ([O#36])

#### Scenario: The credit is on the scene, with the band

- **WHEN** the Night scene is rendered with the Milky Way band drawn
- **THEN** the credit `ESO/S. Brunier` is rendered on the band or immediately beside it, unaltered and not separated from the image, and nothing on the scene implies an ESO endorsement

#### Scenario: A shared postcard carries the credit

- **WHEN** a postcard containing the Milky Way band is exported or shared
- **THEN** the exported image contains a legible credit line naming `ESO/S. Brunier` and `CC BY 4.0`

#### Scenario: Every sky asset carries its licence

- **WHEN** the slot's provenance record is read
- **THEN** it names every sky asset with its licence, and the set is exactly ESO `eso0932a` (CC BY 4.0), NASA SVS Moon Kit (public domain), FK6 (CC0 1.0) with the hand-authored brightest-star supplement, 2MASS (public-domain style plus the verbatim acknowledgement), the IAU figures and names (CC BY 4.0) and Wikidata (CC0 1.0) — with **no planet texture** and so no Solar System Scope credit ([O#35])

#### Scenario: No copyleft asset is in the bundle

- **WHEN** the slot's provenance and bundled asset names are scanned
- **THEN** no `GPL`, `AGPL`, `CC BY-SA` or `CC BY-NC` entry is found ([O#37])

#### Scenario: The credit is translated too

- **WHEN** the app is switched to Russian
- **THEN** the credit line still names `ESO/S. Brunier` and `CC BY 4.0` verbatim, with only the surrounding prose translated

---

### Requirement: A planet is a disc scaled by its magnitude, never a texture

A planet SHALL be drawn in the same layer pipeline as any other target (the deep-sky target layer) as a
**disc whose radius is scaled by the planet's computed magnitude**, with its **label**, and SHALL NOT
be drawn from an image asset: **no planet texture SHALL ship**, and no texture credit is owed for one.
The disc SHALL be a `Path` like every other object in that layer, so it is occluded, labelled and
rotated by the same rules as the deep-sky targets; drawing a planet from the Solar System Scope
textures, or carrying any `assets/sky/planets/` image, SHALL be a defect. The Moon keeps its own layer
and its own NASA SVS image unchanged.

#### Scenario: A planet is a magnitude-scaled disc with a label

- **WHEN** a planet is rendered in the scene
- **THEN** it is a filled disc whose radius follows its computed magnitude and it carries its label, and no image texture is sampled for it

#### Scenario: No planet texture ships

- **WHEN** the slot's provenance record and bundled asset names are scanned
- **THEN** no Solar System Scope texture and no `assets/sky/planets/` image is present, and the Moon's NASA SVS asset is unchanged

---

### Requirement: The star set is FK6 plus a mandatory brightest-star supplement

The star table the scene draws SHALL be the **FK6** catalogue (**CC0 1.0**) **plus a hand-authored
supplement of the brightest stars**, merged into one shipped star set. FK6's live row count is
**4,151** and its V magnitude range is **−0.62 … 9.71**; FK6 **omits Sirius, Arcturus, Capella and
Betelgeuse** (its brightest row is `alpha Car` at `v = −0.62`), so the supplement SHALL be mandatory
and SHALL be merged into the shipped set — without it the scene omits the sky's most recognisable
stars. Each supplement row SHALL carry its own two-source verification, naming two independent
references and their angular agreement, exactly as the compiled deep-sky rows carry `srcA`, `srcB` and
`deltaArcmin`. The magnitude limits this capability already declares SHALL apply unchanged to the
merged set: the point field's drawn magnitude limit, and the star-trail mode's **`V ≤ 6.0`**.

#### Scenario: The supplement is merged, not optional

- **WHEN** the shipped star set is read for Sirius, Arcturus, Capella and Betelgeuse
- **THEN** all four are present and the set's row count is at least **4,151**

#### Scenario: A supplement row carries its own two-source verification

- **WHEN** a supplement row's verification record is read
- **THEN** it names two independent sources and their agreement, rather than a single source or an unverified magnitude

---

### Requirement: Star-trail mode draws the night's arcs

The scene SHALL offer a **star-trail mode** on the same timeline as the point mode. In trail mode the
scene SHALL draw, for every star brighter than the mode's magnitude limit (**`V ≤ 6.0`**), the **arc
it sweeps over a trail interval centred on the current scrub instant** — a polyline of at least **12
vertices** per star across that interval — instead of its point sprite. The trail interval SHALL
default to **30 minutes** and SHALL be selectable among **15 / 30 / 60 / 120 minutes**, and the
selected interval SHALL be stated in the readout.

Trail mode SHALL differ visually from point mode in three observable ways: the star field is arcs
rather than points; **no scintillation** is applied (a trail is an integrated exposure, so twinkle
would be a lie); and the satellite layer draws the whole pass's trail across the interval rather than
a short streak. Entering and leaving the mode SHALL be a named two-state control on the scene's
chrome, and leaving it SHALL restore point mode at the same scrub instant with no change to the spot,
date or lens. The night's geometry, the horizon, the Moon, the Milky Way and the LUT SHALL be
unchanged by the mode.

#### Scenario: Arcs replace points

- **WHEN** trail mode is entered at a fixed scrub instant
- **THEN** every drawn star is an arc over the stated interval and no point sprite remains from point mode

#### Scenario: The arcs span the stated interval

- **WHEN** the trail interval is set to 30 minutes and a star's arc's endpoints are converted back to time
- **THEN** they differ by 30 minutes within 1 minute

#### Scenario: Trails do not twinkle

- **WHEN** two consecutive frames of trail mode at the same scrub instant are compared
- **THEN** they are identical, because no scintillation term is applied in trail mode

#### Scenario: The interval cannot be set to zero

- **WHEN** the shortest offered interval is selected
- **THEN** the shortest offered value is 15 minutes and no zero-length or negative interval is reachable

#### Scenario: Leaving the mode restores the point field unchanged

- **WHEN** trail mode is entered and then left without moving the scrub
- **THEN** the point field is drawn at the same instant with the same star positions as before the mode was entered

---

### Requirement: Red night mode preserves dark adaptation

The scene SHALL have a **fourth theme state, "red night mode"**. In this mode a real observer's dark
adaptation is the product, so the constraint is on the emitted spectrum, not on taste.

**Palette discipline.** The red-mode palette SHALL use the red channel for its whole signal, with
**blue fixed at 0 and green at or below 8/255 on every declared colour**. The threshold is derived from
the CIE scotopic luminous efficiency `V′(λ)`: it peaks at 507 nm, has fallen to ≈ 0.0002 of peak at
620 nm, is ≈ 0.43 at the sRGB green primary near 550 nm and ≈ 0.04 at the blue primary near 450 nm. The
sRGB green primary is therefore the dominant scotopic leak, and `G ≤ 8/255` holds a full-strength
pixel's scotopic stimulus to roughly **3 %** of a white pixel's, while a red-only pixel contributes
about **0.04 %** — enough to read the screen and not enough to reset the eye. The derivation and the
threshold SHALL be recorded beside the palette in `ui/theme/NightVisionPalette.kt`.

**It is a palette, not a filter.** Red mode SHALL be its own declared palette in
`ui/theme/NightVisionPalette.kt`; it SHALL NOT be produced by a saturation, hue, colour-matrix or
`ColorFilter` transform applied to the default palette, and it SHALL NOT reuse the default sky colours
with an overlay. The sky's computed radiance SHALL be mapped through the red palette's own monotone
ramp, so the scene keeps its real structure — the twilight bands, the shadow edge, the stars and the
band — as luminance.

Red mode SHALL apply to **the scene and every surface of the app, including the home-screen widget**,
which SHALL read the same palette. It SHALL be toggled by a named control with a TalkBack role and a
touch target of at least **48 dp** on the Night screen **and** by a row in `Settings`, and the choice
SHALL be remembered across restarts and process death.

#### Scenario: No blue and no bright green anywhere in the mode

- **WHEN** every colour declared in `ui/theme/NightVisionPalette.kt` is read
- **THEN** each has blue `= 0` and green `≤ 8/255`, and a unit test fails if any declared colour does not

#### Scenario: The mode is a palette, not a filter

- **WHEN** the sources are read for a colour-matrix, saturation, hue-rotate or `ColorFilter` applied to the default palette to obtain red mode
- **THEN** none exists, and red mode's colours come from its own declared palette

#### Scenario: The scene keeps its structure in red

- **WHEN** the scene is rendered in red mode at `ε = −4°` and at `ε = −18°`
- **THEN** the twilight bands, the Earth-shadow edge and the star field remain distinguishable as differences in red-channel luminance, and the two instants are not the same picture

#### Scenario: The widget is red too

- **WHEN** red mode is on and the home-screen widget is rendered
- **THEN** the widget's colours come from the same declared red palette, with blue `= 0` and green `≤ 8/255`

#### Scenario: The toggle is remembered

- **WHEN** red mode is turned on, the app is closed and the process is recreated
- **THEN** the scene opens in red mode without asking again

#### Scenario: The toggle is reachable and named

- **WHEN** the red-mode control is measured and read by TalkBack
- **THEN** it is at least 48 dp in both dimensions, it has a switch role and a name, and a second, equivalent row in `Settings` changes the same preference

#### Scenario: The default theme is not red

- **WHEN** the app is installed and opened for the first time
- **THEN** red mode is off, so the default scene shows the true sky colours

---

### Requirement: The lens frame states whether the target fits

The scene SHALL draw the **angular frame of the user's own lens and sensor** as a rectangle with four
corner ticks, at the chosen `LensPreset`'s field of view (`domain/Optics.kt`), and SHALL state, in
words beside or beneath the frame, whether the currently selected target fits inside it. The
horizontal and vertical FOV SHALL be computed from **focal length and sensor dimensions in
millimetres, always** — the units preference SHALL convert temperature, wind and distance and SHALL
**never** convert focal length or sensor size. The default preset SHALL be **24 mm, 73° × 53°, 30 s**.

The fit verdict SHALL be a `TargetFit` value carrying exactly the **three** states the
**`target-catalogue`** capability defines — **fits**, **marginal** and **too large** — and this
capability SHALL NOT declare a second value set: the classification is defined once in
`target-catalogue`'s lens-fit requirement, which compares the target's major axis against the frame's
**shorter** side and owns the boundary values, and this requirement SHALL reference them rather than
restate different ones. The verdict SHALL be stated as a sentence naming the target's required
angular size and the frame's available size, e.g. `M31 needs 2.6° × 1.0°; your 300 mm frame is
4.6° × 3.1° — fits.` When the target's angular size is not in the compiled catalogue, the app SHALL
**refuse a verdict** and say that the size is unknown, rather than implying a fit it cannot compute.
The frame SHALL be fixed to the sky: redrawing the horizon SHALL NOT move it. The frame SHALL NOT be
occluded by the silhouette.

#### Scenario: A target that fits is stated to fit

- **WHEN** a target whose catalogue size is smaller than the frame's angular size is selected
- **THEN** the scene states that it fits and names both sizes in degrees

#### Scenario: A target that does not fit is stated with its state and the shortfall

- **WHEN** a selected target's major axis is past the `fits` band the `target-catalogue` capability defines, so it is in one of the two non-fitting states — the `marginal` band or beyond the frame's shorter side
- **THEN** the scene states which of the two it is — `marginal` or `too large` — and names the required and available sizes, rather than showing a clipped marker with no explanation

#### Scenario: An unknown size refuses a verdict

- **WHEN** the selected target has no angular size in the compiled catalogue
- **THEN** the app states that the size is unknown and offers no fits / marginal / too-large verdict

#### Scenario: The frame is fixed to the sky

- **WHEN** the user redraws the horizon line
- **THEN** the frame's screen position and angular size are unchanged, because the frame is a property of the view rather than of the silhouette

#### Scenario: Focal length is never unit-converted

- **WHEN** the units preference is set to imperial and the framing is computed
- **THEN** the focal length and sensor dimensions are the same millimetre values as in metric, and the fit verdict is identical

#### Scenario: The frame is not occluded

- **WHEN** a ridge rises across the frame's lower edge
- **THEN** the frame's rectangle and all four ticks remain fully drawn

---

### Requirement: A satellite pass is a tapered streak and a stale element set is named

A predicted visible pass SHALL be drawn as **one tapered streak** — a fading `Path` whose stroke is
widest at the head and narrows to the tail, built from **at least 8 vertices** over the pass's last
20 seconds of travel — and SHALL NOT be a dot or a full-orbit line. The streak SHALL advance
monotonically in azimuth while the scrub advances, SHALL be split at the horizon, and SHALL be drawn
only over the **observable** sub-interval (above the ridge ∧ satellite sunlit ∧ observer dark), so the
picture and the sentence agree. The number of streaks drawn at once SHALL be capped, and the cap SHALL
be stated in the layer legend rather than applied silently.

The scene SHALL name the element set's state rather than drawing a plausible pass from a stale set:
**age ≤ 24 h** normal; **24 h < age ≤ 72 h** drawn with an explicit staleness sentence; **age > 72 h**
no pass times at all, only an expired-set sentence; and **no element set at all** an unavailable
sentence naming the reason. A pass that occurs but is not observable SHALL be reported as occurring
and not visible with its reason, and SHALL be drawn as a dimmed streak rather than omitted.

#### Scenario: A pass is a trail, not a dot

- **WHEN** a visible pass is rendered and scrubbed through
- **THEN** it is a connected polyline of **≥ 8 vertices** present in **≥ 6 consecutive scrub frames**, advancing monotonically in azimuth, with **no segment** below the ridge ([O#24])

#### Scenario: The streak matches the stated window

- **WHEN** a pass is observable for only 40 seconds because the satellite leaves sunlight
- **THEN** the drawn streak spans that 40-second sub-interval and the readout states the same sub-interval

#### Scenario: A pass in Earth's shadow is reported, not dropped

- **WHEN** a pass occurs with the satellite in umbra
- **THEN** the app states that the pass occurs but the satellite is in Earth's shadow, and the streak is dimmed rather than absent

#### Scenario: A daylight pass is reported as not visible

- **WHEN** a pass occurs while the sky is brighter than solar altitude `−6°`
- **THEN** the app states that it occurs but the sky is too bright, and states it once rather than per pass when every pass in the night is in daylight

#### Scenario: A stale element set is named, and an expired one refuses times

- **WHEN** the cached element set is 30 hours old and, separately, 80 hours old
- **THEN** the first is drawn with the staleness sentence, and the second shows no pass times at all with the expired-set sentence

#### Scenario: No element set at all

- **WHEN** the app has never reached the element feed and is offline
- **THEN** the satellite layer renders an unavailable state naming the reason, and the rest of the scene and the brief are complete

#### Scenario: The drawn streak count is capped and stated

- **WHEN** more satellites are above the horizon than the cap allows
- **THEN** the cap is stated in the layer legend, the drawn set is the brightest passes, and the underlying pass list is complete

---

### Requirement: The timeline strip is the same sky function at 96 stops

The timeline strip below the scene SHALL take each of its colours from **the same sky function the
scene uses**, sampled at **96 stops `i = 0 … 95`** at a **fixed view elevation of 12°** on the
**antisolar meridian**, each stop being
`strip(i) = sRGB( tonemap( E(ε_i) · LUT_A( ε_i , 12° ) ) )` with the same tonemap, white balance,
exposure and transfer as the scene's top layer. The strip SHALL NOT be a second palette, a decorative
key, or a static gradient. A unit test SHALL assert that 20 sampled stops equal the scene's own sky
function evaluated at the same `(ε_i, 12°)` to **0/255** ([O#43]).

The strip SHALL be built once per (spot, date) into a 96 × 1 source image stretched horizontally, and
a scrub SHALL move only a thumb and update text, regenerating nothing.

The strip SHALL carry the night's three overlay facts.

The **dark-window band** SHALL span exactly the intervals the `session-brief` capability's *The dark
window is a computed interval with a typed outcome* requirement defines as the dark window — computed
from the Sun alone — and this requirement SHALL cite that definition as the authority for the band
rather than restating its solar threshold. The **moonless window** that the `session-brief`
capability's Moon requirement defines is a **separate** fact, not a synonym for the dark window: when
the strip marks it, it SHALL be drawn as its own mark, with a visual treatment distinct from the
dark-window band and its own label, so a dark window the Moon is up in is never drawn as a moonless
one, and this requirement SHALL cite that definition rather than restating the Moon's threshold. A
band that does not occur on the chosen night SHALL be hidden rather than drawn at zero width.

When the night has **no** dark window, the strip SHALL draw no dark-window band and SHALL name the
night's typed state rather than draw a zero-width band or substitute the moonless mark: the closed set
of states (`NO_ASTRO_DARK`, `NO_DARK`, `POLAR_DAY`) is owned by the `session-brief` capability's
dark-window requirement, and the sentence the readout reuses is owned by the requirement below, *The
scene renders a true sky when there is no darkness at all*. This requirement SHALL NOT introduce a
fourth state of its own.

The **Moon** SHALL be drawn as markers at its rise and set (and, where the drawn horizon masks the
Moon, marked as masked) with the illuminated fraction labelled; and the **cloud** as an adjacent row of
cells coloured by the cached forecast's total cloud-cover fraction, labelled `forecast` with its
retrieval time and rendering `no forecast` when there is none. The phase labels on the strip SHALL be
computed from the standard `−6° / −12° / −18°` thresholds and SHALL be labels, never keyframes.

**An instant exactly on a boundary belongs to the interval it opens (E95).** When the scrub instant
coincides exactly with a twilight threshold, the instant SHALL be attributed to the interval it
**opens**, consistent with the half-open `[start, end)` intervals this app uses: an instant equal to
the astronomical-dark boundary is the **start of astronomical dark**, not the end of the band before
it. The boundary's label SHALL be rendered **beside the slider thumb** — e.g. `23:04 — astronomical
dark begins` — naming that interval and its clock time in the spot's zone. The label SHALL NOT flicker
between the two sides of the boundary as the thumb is dragged across it: the attribution at the exact
instant SHALL be stable and fixed by the half-open rule, never chosen by the direction of travel.

#### Scenario: The strip's colours are the scene's function

- **WHEN** 20 sampled strip stops are compared with the sky function evaluated at the same solar altitudes and 12° elevation
- **THEN** every comparison agrees to **0/255** ([O#43])

#### Scenario: A scrub costs no strip work

- **WHEN** the strip is scrubbed across its whole range
- **THEN** the strip's cells are not regenerated and only the thumb and the text change

#### Scenario: The strip carries the dark window, the Moon and the cloud

- **WHEN** a night is loaded with a computed dark window, a moonrise inside it and a cached forecast
- **THEN** the strip shows the dark window as a bracketed region, the Moon's rise and set with its illuminated fraction, and an adjacent row of cloud cells labelled as a forecast with its retrieval time

#### Scenario: The dark-window band and the moonless mark are distinct marks

- **WHEN** a computed night has the Moon above the horizon for part of its dark window, so the moonless window is a strict sub-interval of the dark window
- **THEN** the dark-window band spans the whole dark window, the moonless mark spans only its narrower sub-interval, and the two are separate marks with distinct visual treatments and their own labels

#### Scenario: A night with no dark window carries no band

- **WHEN** the night's dark-window report is one of the `session-brief` capability's non-`DARK` typed states (`NO_ASTRO_DARK`, `NO_DARK`, `POLAR_DAY`), so the night has no dark window
- **THEN** the strip draws no dark-window band and names that typed state, and it neither draws a zero-width band nor substitutes the moonless mark, because the typed state is owned by the `session-brief` capability's dark-window requirement rather than invented here

#### Scenario: A band that does not occur is hidden, not zero-width

- **WHEN** the night has no astronomical darkness
- **THEN** the astronomical band is not drawn at zero width and is labelled as not occurring, rather than appearing as a hairline

#### Scenario: No forecast is stated, not faked

- **WHEN** the app has never fetched a forecast
- **THEN** the cloud row renders `no forecast` rather than an empty row or a fabricated value

#### Scenario: An instant exactly on a boundary is attributed to the band it opens (E95)

- **WHEN** the scrub sits exactly on the astronomical-dark boundary at 23:04
- **THEN** the instant is attributed to astronomical dark — the interval it opens — the label `23:04 — astronomical dark begins` is rendered beside the thumb, and dragging the thumb back and forth across 23:04 does not make the label flicker between `astronomical dark begins` and the previous band's end

---

### Requirement: The instrument readout states the night's facts, and no control covers what is being planned

Below the scene the screen SHALL present an instrument readout carrying, at all times, the **scrub
control, the clock, the light window, the dark window, the Moon, the selected target and the pass
warning**. The readout SHALL use the brand vocabulary — "session", "spot", "horizon line", "dark
window", "brief", "target", "pass", "frame", "Tonight" — and SHALL state each fact with a number and a
unit.

**No control SHALL cover the thing being planned.** Every interactive control SHALL be laid out
outside the scene rect; nothing in the chrome SHALL overlap the target marker, the lens frame, the
horizon silhouette or a pass streak. In portrait the scene rect SHALL occupy at least **45 %** of the
window height whenever the chrome is fully expanded, and the chrome SHALL be collapsible so the scene
can grow, never shrink below that floor. The scene SHALL extend edge-to-edge behind the system bars
while the chrome respects the insets.

Every interactive control SHALL be named, SHALL carry a TalkBack role, and SHALL have a touch target
of at least **48 dp** in both dimensions — including the scrub thumb, whose visible size may be
smaller than its touch target. Text in the chrome SHALL meet a contrast ratio of at least **4.5:1**,
and no fact SHALL be carried by colour alone: every colour-coded band, the locked/selected state, the
moonless-versus-moonlit state and the pass warning SHALL also carry a word or a shape.

The named controls are: the scrub track (slider role, value announced), the timeline strip (same
accessible slider), the **Play night** toggle (button role, label alternating between play and pause),
the **red night mode** toggle (switch role), the **star trails** toggle (switch role), the **target**
row (focusable, opens `Targets`), and the **pass warning** row (focusable, opens the pass detail).
The clock, the light window, the dark window, the Moon and the target altitude are readouts, not
controls, and are announced by the scene's semantic summary.

#### Scenario: Every control meets the target floor

- **WHEN** the Night screen's controls are measured
- **THEN** the scrub track, the strip, the play toggle, the red-mode toggle, the trails toggle, the target row and the pass row are each at least 48 dp in both dimensions

#### Scenario: The controls never cover the plan

- **WHEN** the scene is rendered with a target, a lens frame, a drawn ridge and a pass all present
- **THEN** no control's bounds intersect the scene rect, and the target marker, the frame, the ridge and the streak are each fully visible

#### Scenario: The scene keeps its height when the chrome is expanded

- **WHEN** the chrome is fully expanded in portrait on the A40
- **THEN** the scene rect is at least 45 % of the window height, and the chrome offers a control that grows the scene

#### Scenario: The readout names all seven facts

- **WHEN** the Night screen is rendered for a night with a dark window, a Moon, a target and a pass
- **THEN** the clock, the light window, the dark window, the Moon's state, the target, the pass warning and the scrub are all present with a number and a unit

#### Scenario: No fact is carried by colour alone

- **WHEN** the readout is rendered in greyscale
- **THEN** the moonless / moonlit distinction, the pass warning and the selected state remain readable from words or shapes

#### Scenario: The caption ink meets its contrast floor

- **WHEN** the chrome's text colours are measured against their backgrounds
- **THEN** every caption is at least **4.5:1** and no number is drawn in the scene's own sky colours

#### Scenario: The play toggle is bounded and produces no idle frames

- **WHEN** the play toggle is started and then stopped
- **THEN** the scene advances the scrub while it is running and returns to zero frames per second when it is stopped

---

### Requirement: The scene renders a true sky when there is no darkness at all

The scene SHALL render the true computed sky in every one of the `DarkWindowKind` states
(`domain/Twilight.kt`: `DARK`, `POLAR_DAY`, `POLAR_NIGHT`, `NO_ASTRO_DARK`, `NO_DARK`) **through the
same LUT path as every other night**, driven only by the Sun's altitude. There SHALL be no special-case
drawing for a night without darkness, no synthetic black sky with stars, and no "no data" screen. The
readout SHALL reuse one sentence per state, and the same sentence SHALL be used by the brief, the
widget, the share text and the reminder.

- **No astronomical darkness** (`NO_ASTRO_DARK`, e.g. midsummer at 52.5 °N and most of the year at
  Reine): state the deepest twilight actually reached and the instant it occurs.
- **No darkness of any kind** (`POLAR_DAY` / `NO_DARK`, midnight sun): the readout's first line
  becomes the **light** window, stating the Sun's minimum altitude and its time.
- **Polar night** (`POLAR_NIGHT`): the Sun does not rise and its maximum altitude stays between −18.0°
  and −0.8333°, so the night carries a dark window with its **midday twilight dip** named as a
  sub-interval, and the readout reports the Sun's **maximum** altitude and the instant it occurs as a
  positive fact; the Moon's and targets' events are computed normally. Which state a night is reported
  as is `session-brief`'s precedence decision (`The five outcomes are a partition of the possible
  nights, resolved by a fixed precedence`), and this capability SHALL NOT restate or re-derive it. When
  the Sun never rises **and** its maximum altitude is at or below −18.0°, the night is one whole-day
  dark interval and the reported kind is `DARK` (`wholeNightDark`), not `POLAR_NIGHT`; the strip then
  draws that single band rather than a dip, and the readout SHALL NOT claim the Sun does not rise.
- The dark-window strip SHALL grey and label the bands that do not occur rather than drawing them at
  zero width, and the reminder SHALL NOT be offered for a night with no dark window.

#### Scenario: Polar day still draws a sky

- **WHEN** the scene is opened at Reine on the June solstice, where the Sun's minimum altitude is +1.37°
- **THEN** the scene draws the sky computed from that Sun altitude through the ordinary LUT, with no black sky and no stars in a night that has none

#### Scenario: No astronomical darkness is a named state with a sentence

- **WHEN** the night reaches nautical twilight and no darker
- **THEN** the readout states the level that is reached and the instant the Sun is lowest, e.g. `No astronomical darkness at Lofoten tonight — the sky only reaches nautical twilight (Sun −14.6° at 01:04).`

#### Scenario: The midnight sun reports the light, not an empty dark panel

- **WHEN** the Sun stays above the horizon for the whole night
- **THEN** the readout's first line states the Sun's minimum altitude and its time, and no dark-window figure is fabricated

#### Scenario: Polar night reports the Sun's maximum as a fact

- **WHEN** the Sun never rises
- **THEN** the night window is the whole civil day, the readout states the Sun's maximum altitude and its time, and the Moon's and targets' events are still computed

#### Scenario: A band that does not occur is not drawn

- **WHEN** the strip is rendered for a night with no nautical twilight
- **THEN** that band is absent and labelled as not occurring, rather than drawn at zero width

#### Scenario: No reminder is offered without a dark window

- **WHEN** the night has no dark window
- **THEN** the "Remind me tonight" affordance is not offered, and the app does not schedule a dark-window reminder

---

### Requirement: The scene degrades by capability, never by truth, below the shader floor and under memory pressure

On a device **below the AGSL floor (API 26–32)**, which includes the factory's own test device, the
scene SHALL render through the LUT strip path, and the AGSL `RuntimeShader` path SHALL be selected
**only** on API 33+ by a `Build.VERSION.SDK_INT` branch. The shader SHALL be **additive**: the
fallback is the product on the A40 and the shader is an enhancement. There SHALL be no reflection, no
`Class.forName`, and no try/catch used as a version check, and a shader compile failure SHALL fall back
to the LUT path for the session rather than rethrowing. Deleting the shader source SHALL leave the
module compiling and the hero screen drawing the same scene structure ([O#39]) (E85).

On a device without enough memory the scene SHALL reduce the **drawn magnitude limit** and rebuild its
cached geometry on `onTrimMemory`; it SHALL NOT reduce the scene's truth — the horizon occlusion, the
LUT sky, the Moon's phase, the Earth-shadow edge and the band's rotation SHALL be identical to a
high-memory render at the same instant. The scene SHALL NOT crash, stall on the first frame, or show a
spinner.

The two paths SHALL sample the same LUT asset and SHALL declare the tone map, white balance, transfer
and dither once each; a debug golden-image test SHALL render the sky through both and fail if any
channel differs by more than **2/255** over 256 elevations × 61 solar altitudes ([O#34]). Colour drift
between the paths SHALL be a defect, not a device difference.

#### Scenario: The A40 runs the fallback with no shader reference

- **WHEN** the hero screen is run on the A40 and its logcat is read
- **THEN** the scene renders the LUT path and logcat contains **no** `ClassNotFoundException` or `NoClassDefFoundError` naming `RuntimeShader` ([O#38])

#### Scenario: The shader is additive

- **WHEN** the shader source file is deleted and the module is compiled
- **THEN** the module still compiles and the hero screen draws the same scene structure ([O#39])

#### Scenario: The two paths agree

- **WHEN** the debug golden-image test renders the sky through the CPU path and the shader for the same inputs
- **THEN** the maximum per-channel difference is **≤ 2/255** over 256 elevations × 61 solar altitudes ([O#34])

#### Scenario: Low memory changes the star count, not the sky

- **WHEN** the scene is rendered under memory pressure and at the same instant without it
- **THEN** the sky, the horizon occlusion, the Moon, the Earth-shadow edge and the band's orientation are identical, and only the drawn star count is reduced (E84)

#### Scenario: A missing sky asset is refused, not invented

- **WHEN** the LUT asset is unreadable
- **THEN** the scene states a typed unavailable condition rather than drawing a plausible gradient, and the rest of the screen remains usable

---

### Requirement: One screen in portrait, landscape, large fonts, TalkBack and RTL

The Night screen SHALL read the window size class and SHALL lay out in a single column in portrait and
in a side-by-side arrangement in landscape, with the scene rect keeping at least **45 %** of the shorter
axis's available space and the chrome reflowing rather than scrolling horizontally.

At a system font scale of **200 %**, every text surface SHALL reflow with no fixed-height container,
no clipped label and no truncated number. Labels drawn inside the scene rect SHALL abbreviate rather
than clip, and SHALL remain legible at the minimum size the type scale allows.

Because the scene is a graphic, it SHALL expose a **semantic summary** for TalkBack naming the spot,
the date, the Sun's altitude, the Moon's state, the dark window and the current instant, and the scrub
SHALL be an accessible **seekable** control whose value is announced as a time and a solar altitude
(e.g. `01:04, the Sun is 18° below the horizon`). A `Canvas` with no semantics SHALL be a defect.

The scene's horizontal axis SHALL NOT be mirrored by layout direction (see the first requirement), and
the surrounding chrome SHALL mirror normally in a right-to-left locale.

#### Scenario: The scene reads the window size class

- **WHEN** the screen is opened on a phone in portrait and then rotated to landscape
- **THEN** the layout changes between the single-column and side-by-side arrangements, and the scene rect is at least 45 % of the shorter axis's available space in both

#### Scenario: Two hundred percent font scale reflows

- **WHEN** the system font scale is set to 200 %
- **THEN** every readout, label and control reflows without clipping, and no number is truncated

#### Scenario: TalkBack hears the night

- **WHEN** TalkBack is enabled and the scene receives focus
- **THEN** a semantic summary names the spot, the date, the Sun's altitude, the Moon's state, the dark window and the current instant

#### Scenario: The scrub announces its value

- **WHEN** TalkBack is enabled and the scrub is adjusted
- **THEN** the announced value is a time and the Sun's altitude, e.g. `01:04, the Sun is 18° below the horizon`, and the control's role is seekable

#### Scenario: The scene is not mirrored in a right-to-left locale

- **WHEN** the locale is switched to a right-to-left language
- **THEN** the chrome mirrors and the scene does not, so the horizon's western ridge stays on the same side as the sky's western azimuth (E88)

#### Scenario: Insets are respected and back is predictive

- **WHEN** the screen is opened at the target SDK with predictive back enabled
- **THEN** the scene draws behind the system bars, the chrome respects the insets, and the back gesture is previewed rather than closing the app abruptly

---

### Requirement: The frame survives rotation and process death

The scene's `SceneKey` — the spot, the night's date, the scrub instant, the lens preset, the selected
target and the horizon profile — SHALL be held in state that survives both a configuration change and
process death (`SavedStateHandle` / `rememberSaveable`). A rotation SHALL recompute nothing that is not
already in the view model and SHALL NOT restart an animation or reset the scrub. After process death
the app SHALL reopen on the same frame. A stored scrub instant that no longer resolves SHALL be clamped
to the night's range rather than restoring a position outside it, and a stored reference to a target
that the compiled catalogue no longer contains SHALL be dropped with a note while the rest of the frame
survives.

#### Scenario: A rotation is instant and lossless

- **WHEN** the scene is rotated with a target selected and the scrub at an arbitrary position
- **THEN** the same frame is drawn, the scrub is unchanged, and no animation restarts (E82)

#### Scenario: Process death restores the frame

- **WHEN** the process is killed and the app reopened
- **THEN** the same spot, date, scrub instant, lens, target and horizon are restored

#### Scenario: A stored instant outside the night is clamped

- **WHEN** the restored scrub instant falls outside the restored night's range
- **THEN** it is clamped into the range and the scene draws rather than failing

#### Scenario: A vanished target costs the target, not the frame

- **WHEN** the restored target id is not in the compiled catalogue
- **THEN** the target is dropped with a note, the frame still opens for the same spot and instant, and the app does not crash

---

### Requirement: Every user-facing string is a resource in both English and Russian

Every user-facing string this capability introduces — the phase labels, the readout labels and values,
the state sentences, the refusal sentences, the layer legend, the credit lines and the TalkBack
announcements — SHALL be a string resource present in **both `app/src/main/res/values/strings.xml` and
`app/src/main/res/values-ru/strings.xml`**, with no user-facing prose as a literal in Kotlin. Numbers,
times and units SHALL be formatted with the locale's own formatters rather than by concatenation, and
the credit lines naming `ESO/S. Brunier` and `CC BY 4.0` SHALL keep those names verbatim in every
locale.

#### Scenario: Both locale files carry every scene string

- **WHEN** the keys used by the Night scene are compared against `values/strings.xml` and `values-ru/strings.xml`
- **THEN** every key exists in both files with a non-empty value, and no scene key is missing from either

#### Scenario: No literal prose in Kotlin

- **WHEN** the Night scene's Kotlin sources are scanned for user-facing string literals
- **THEN** none is found, and every string is resolved through a resource ([O#44])

#### Scenario: The credit survives translation

- **WHEN** the app is switched between English and Russian
- **THEN** `ESO/S. Brunier` and `CC BY 4.0` render identically in both, with only the surrounding prose translated

#### Scenario: Times and numbers follow the locale

- **WHEN** the locale is changed between the two declared locales
- **THEN** times and numeric readouts are rendered with the locale's own formatter rather than a hard-coded separator

**Honest boundary, recorded rather than glossed.** This capability's requirements are checkable only
against a real run, and several of them are not proved by any code that exists today. The acceptance
readings in this specification are the ones `O-sky-scene-rendering.md` §9 defines and are reproduced
here with their numbers so that a reviewer knows what to observe; the scene itself, the LUT asset, the
baked Milky Way map and the star table do not exist yet, so every one of those readings is
**`UNVERIFIED`** until the corresponding capture is taken on serial `R58MB3SGZVL`. In particular, the
frame-time and battery claims of the budget requirement are **`UNVERIFIED`**: no reading of this scene
on this device exists, and the only route to one is the `dumpsys gfxinfo` and `dumpsys batterystats`
commands named there. What this specification proves is the shape, the numbers and the refusals; what
it cannot prove is that a rendering yet meets them.

## MODIFIED Requirements

None. This change adds a new capability and modifies no existing spec.
