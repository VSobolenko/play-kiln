# H — Rendering options for the scrubbable sky scene

**Question.** What is the right rendering technology for the hero screen of a `$9.99`, closed-source,
no-ads, no-server Android light-and-sky session planner — a user-drawn horizon silhouette with the real
sky above it for a chosen place and time, animated by a time slider (twilight colour, moon arc, deep-sky
targets rising and setting, a satellite streak, a lens-FOV overlay)? And what already exists off the
shelf rather than needing invention?

**Method.** Repository rules and licensing research read on disk; vendor documentation and library
licence files fetched live in this pass (`RuntimeShader` reference, AGSL overview, AGSL quick reference,
Filament `LICENSE`, SceneView `LICENSE`, Filament Android platform doc, catalogue licence pages).
Nothing here is quoted from memory. Every row of §13 carries a label and its source.

**Constraints taken as given.** `minSdk 26`, `targetSdk 36`, APK budget ~20 MB (current astronomy core
~4.18 MB), factory test device **Samsung Galaxy A40 (`SM-A405FM`, serial `R58MB3SGZVL`), Android 11**.
The factory's asset licence policy is the allowlist/denylist in
`docs/research/2026-10-02-ui-icon-libraries-and-licensing.md`.

---

## 0. Verdict in five lines

1. **No turnkey, licence-clean library exists** that renders an astronomical sky (starfield by
   magnitude + horizon + twilight + moon phase + satellite track) on Android. The planetarium engines
   are GPL/AGPL and cannot ship in a paid closed-source app; the permissively licensed ones are
   general-purpose graphics/3D runtimes, not sky renderers. **This is an answer, not a failure: the
   sky is ours to build, and it is a bounded amount of work.**
2. **Do not use a 3D engine.** The scene has no perspective, no meshes, no lighting and no occlusion —
   it is a set of 2-D angular positions on a screen rectangle. Filament/SceneView would be used for its
   shader plumbing while every feature it is famous for goes unused, and it ships native code for ABIs
   a 2-D scene does not need.
3. **Recommended: a layered 2-D renderer** — a **colour-LUT gradient** for the sky (sampled as a
   texture), **Compose `Canvas` geometry** for everything with a shape (stars, moon, DSO markers,
   satellite streak, horizon silhouette, FOV frame), and **AGSL `RuntimeShader` as an optional
   API 33+ enhancement** on top of the same LUT, never as the only path.
4. **The AGSL floor is API 33, and the factory's own test phone is Android 11 (API 30).** The
   designated verification device therefore exercises *only* the fallback. Any AGSL win is
   unverifiable on factory hardware until a second, API 33+ device is authorised. **This is the top
   risk — procedural as much as technical.**
5. **The star catalogue, not the renderer, is the licence trap.** HYG is `CC BY-SA 4.0`; the factory's
   denylist forbids `CC-BY-SA-*`. Whatever renderer is chosen, the *data* needs an owner/legal
   decision before it ships.

---

## 1. Decompose the scene before choosing the technology

The single most useful step: the "sky scene" is not one thing. It is six layers with completely
different rendering characteristics, and only one of them is a shader problem.

| Layer | What it is mathematically | Natural renderer | Per-pixel cost |
|---|---|---|---|
| **A. Sky field** | Colour as a function of (altitude, azimuth, sun altitude, moon position) — a **smooth, low-frequency field** | **Fragment shader**, or a **1-D LUT texture** sampled by altitude | Full-screen, but *cheap*: one texture fetch or a handful of ALU ops |
| **B. Stars** | ~1 k–10 k sparse points, sized by magnitude, positions = a rotation of a catalogue | **Geometry**: `drawPoints` batched by magnitude, or one cached raster | ~zero per pixel; CPU-side point throughput |
| **C. Deep-sky targets** | A few dozen rectangles/circles + labels | Canvas geometry | negligible |
| **D. Moon** | One disc, phase-shaped, with a small glow | Canvas geometry (`Path` + radial gradient) | negligible |
| **E. Satellite streak** | One polyline with a fading tail | Canvas `Path` with a gradient stroke | negligible |
| **F. Horizon silhouette + FOV overlay** | One user-authored `Path`, plus a frame | Canvas geometry | negligible |

**The decisive observation:** only layer **A** is a per-pixel shader problem, and it is the *easiest*
per-pixel problem there is (a smooth gradient field). Layers **B–F** are geometry, and geometry is what
a 2-D `Canvas` does natively and a fragment shader does not do at all. Any plan that puts stars or
satellite tracks into a full-screen shader mistakes the scene's shape.

---

## 2. The options, with their licences

| # | Option | What it actually gives you | SPDX / licence | Paid closed-source OK? | Verdict for this scene |
|---|---|---|---|---|---|
| 1 | **Compose `Canvas` / `DrawScope`** (`drawWithCache`, `drawPoints`, `Path`, `Brush` gradients), `Modifier.drawWithContent` | Geometry + gradients inside the existing Compose render pass. No new context, no new dependency, no ABI split | Part of Compose/AndroidX — `Apache-2.0` | **Yes** | **Core of the recommendation.** Carries layers B–F, and layer A via a many-stop gradient or LUT |
| 2 | **AGSL / `android.graphics.RuntimeShader`** | A per-pixel `vec4 main(vec2)` contributed as a `Shader` to a `Canvas`/`RenderNode` paint operation. Ideal for layer A | Part of the platform, no licence | **Yes** | **Optional API 33+ enhancement on layer A only.** Not available below API 33 (§3) |
| 3 | **Google Filament** | Full PBR 3-D renderer: glTF, IBL, materials, skinning, Vulkan/GLES backends, native `.so` | `Apache-2.0` (read: `google/filament` `LICENSE`) | **Yes** | **Rejected.** §4 |
| 4 | **SceneView** | Compose wrappers over Filament (3-D model viewer, AR) | `Apache-2.0` (read: `sceneview-android` `LICENSE`) | **Yes** | **Rejected with #3** — it is a convenience layer over #3, so it inherits #3's mismatch |
| 5 | **`GLSurfaceView` / raw OpenGL ES 2.0–3.2** | Direct GPU control; universal on API 26+; what the planetariums use | Platform API, no licence | **Yes** | **Held as the performance fallback**, not the first choice (§7.3) |
| 6 | **Vulkan** (AVP) | Explicit GPU control | Platform API, no licence | **Yes** | **Rejected.** Needs NDK/C++ (no Kotlin bindings); the only thing it buys is control this scene does not need |
| 7 | **WebView + Three.js** | WebGL in a web view | `MIT` (Three.js; version not re-read this pass) | Yes | **Rejected.** Extra process + IPC + scrub jank; see §4.4 |
| 8 | **Compose `graphicsLayer` layering** | RenderNode-backed compositing of independent layers | Platform/Compose | **Yes** | **Not an alternative — a technique.** It is how the recommended renderer invalidates only what changed (§7.1) |
| 9 | **Skiko** (Kotlin bindings to Skia) | Direct Skia API from Kotlin, Android **API 24+** | `Apache-2.0` (read: `JetBrains/skiko` README) | Yes | **Not needed.** Compose already draws through Skia; adding Skiko duplicates the layer |
| 10 | **Lottie / Rive / pre-baked animation** | Playback of an authored animation | Lottie `Apache-2.0`; Rive runtime per its own licence | Yes | **Rejected.** The scene is a *function* of (place, date, time, user horizon); it cannot be pre-baked |
| 11 | **A planetarium engine** (Stellarium, Sky Map, Gaia Sky, KStars…) | A finished sky | **GPL / AGPL / MPL — see §6** | **No** (GPL/AGPL); MPL is not on the allowlist | **Forbidden as code.** Its *published techniques* are fair to reimplement |

**Allowlist/denylist applied.** The factory's allowlist is the exact strings `MIT`, `ISC`,
`Apache-2.0`, `OFL-1.1`, `CC-BY-4.0`, `MIT-0`, `BSD-2-Clause`, `BSD-3-Clause`
(`docs/research/2026-10-02-ui-icon-libraries-and-licensing.md:634`); the denylist is
`CC-BY-NC-*`, `CC-BY-NC-SA-*`, `CC-BY-SA-*`, `GPL-*`, `AGPL-*`, `LGPL-*`, `CC-BY-ND-*`
(`:642-643`), with the reasoning at `:197`: *"Share-alike / copyleft: distribution can oblige release
under the same terms — incompatible with a paid closed-source app"*. Filament and SceneView are
`Apache-2.0` and pass. Every planetarium engine fails.

---

## 3. AGSL / `RuntimeShader` — exactly what degrades below API 33

### 3.1 The floor

> *"Android Graphics Shading Language (AGSL) is used by **Android 13 and above** to define the behavior
> of programmable `RuntimeShader` objects."*
> — `https://developer.android.com/develop/ui/views/graphics/agsl` (fetched this pass)

**Therefore: on API 26–32 there is no AGSL at all.** Not a reduced version, not a software fallback —
the class does not exist. With `minSdk 26`, that is the API 26, 27, 28, 29, 30, 31, 32 range. **The
factory's own test phone is Android 11 — API 30 — inside that range.**

`RuntimeShader` is guarded the ordinary way (`Build.VERSION.SDK_INT >= 33`) and the app must ship a
second, non-AGSL path for layer A that is good in its own right, or the hero screen is a flat rectangle
on the device the factory actually tests on.

### 3.2 What AGSL can and cannot do (read from the two vendor pages this pass)

**What it is:**

> *"A `RuntimeShader` calculates a **per-pixel color** based on the output of a user defined Android
> Graphics Shading Language (AGSL) function."*
> — `https://developer.android.com/reference/android/graphics/RuntimeShader`

> *"Your AGSL effect contributes **a function** (or functions) to the GPU's fragment shader."*
> — AGSL overview. So AGSL is a stage of the `Canvas`/`RenderNode` pipeline, **not** a whole GPU
> pipeline program.

**Capabilities that matter here:**

- Input is `vec4 main(vec2 canvas_coordinates)`; **the coordinate origin is the upper-left**, unlike
  GLSL (`RuntimeShader` ref). Every screen↔sky transform must be written in Canvas coordinates.
- Output is **premultiplied** (`[R*A, G*A, B*A, A]`); layer A is opaque so this is only a concern if
  the sky layer is composited with alpha.
- Colour-managed: the shader's working space is the destination's. `toLinearSrgb()` / `fromLinearSrgb()`
  are provided *"to get physically accurate lighting"* — which is exactly what an atmospheric-scattering
  model wants, and exactly the trap if you mix linear and sRGB halves in one expression.
- `layout(color)` on a `vec4` uniform makes Android convert it into the working space — the clean way to
  pass palette stops in.
- Uniform surface: `float`, `vec2/3/4`, `int`, `ivec2/3/4`, `mat2/3/4`, **`float[]`, `int[]`**, colour
  uniforms, **input shaders** (`setInputShader`) and **raw data buffers** (`setInputBuffer`). So a
  palette or a small table *can* be handed to the shader as data, not just as constants.

**Capabilities that bite:**

| Restriction (verbatim in substance from the AGSL quick reference) | Consequence for this scene |
|---|---|
| *"'for' loops are quite limited; the compiler must be able to **unroll** the loop. The initializer, the test condition, and the `next` statement must use **constants**."* `next` limited to `++ -- += -=` | The loop bound cannot be a uniform. You must choose a **fixed maximum** at shader-compile time, and the compiler inlines every iteration. **A real star catalogue cannot be evaluated per pixel.** A handful of procedural stars is fine; a few thousand is a gigantic shader that will not compile or will run at a fraction of the frame budget |
| No recursion; `discard` **not allowed** | A "draw stars only where they are" approach via `discard` is out; you blend instead |
| Arrays: **1-D only, fixed size**, cannot be returned/copied/assigned/compared, and *"can only be indexed using a **constant or a loop variable**"* | Star tables can be read, but only by a constant index or the unrolled loop index — reinforcing the point above |
| No `sampler2D`; sampling is `uniform shader` + `.eval(coord)`, or a **raw buffer** via `setInputBuffer` | A pre-baked star/atmosphere texture *can* be sampled. This is the bridge in §7.1 |
| `attribute`, `varying`, `invariant` unsupported; declarations must be inside an explicit braced scope | No vertex-stage customisation; it is a fragment-stage language only |

### 3.3 The honest conclusion on AGSL

**AGSL is the right tool for layer A and the wrong tool for layers B–F.** Use it for the twilight /
atmospheric gradient on API 33+, where it buys physically modelled scattering instead of an
interpolated ramp — and keep the LUT path that produces the same picture on API 26–32, which is the
path the A40 will run.

---

## 4. The heavier options, and why each loses

### 4.1 Filament / SceneView (the "3-D engine" answer)

Filament is a genuine piece of engineering and it is licence-clean: `Apache-2.0`, read at
`raw.githubusercontent.com/google/filament/main/LICENSE` (`"Copyright 2023 The Android Open Source
Project"`). Its Android requirements are modest on paper — **`minSdk 21`, OpenGL ES 3.0+, Vulkan 1.0+
with the backend auto-selected** (Filament Android platform doc; `android/build.gradle` in the repo
declares `minSdk 21` and the ABI list `arm64-v8a, armeabi-v7a, x86_64, x86`) — and the artifact is a
normal Maven dependency (`com.google.android.filament:filament-android:1.72.0`).

It still loses, for a reason that is about the scene and not about quality:

- **The scene has no 3-D content.** Every element is an angular position projected onto a rectangle.
  There are no meshes, no normals, no lights, no materials, no occlusion, no depth. The moon is a disc,
  not a sphere with a phase terminator you would shade; the horizon is a 2-D `Path`, not terrain.
- **Its value is in the parts that go unused.** Filament's identity is PBR, glTF, IBL and material
  authoring. Fleet-fingered as they are, none of them is on the list of things this screen needs.
- **It does not remove the shader work.** Layer A still has to be written as a custom material/shader —
  i.e. the *same* AGSL-or-GLSL work — and now inside a native engine's material system instead of
  inside Compose.
- **It adds native code and a second render context** for ABIs (x86/x86_64) a shipped Play app does not
  need, plus a surface lifecycle to manage from Compose via `AndroidView`.

> **A measured caveat, stated rather than implied:** whether *this* device's Mali-G71 MP2 driver is on
> Filament's practical supported list is **UNVERIFIED** — it would take an on-device run. The
> recommendation does not depend on it, because the argument above is structural.

**SceneView** is a `Apache-2.0` Compose wrapper over Filament (LICENSE read at
`raw.githubusercontent.com/SceneView/sceneview-android/main/LICENSE`). It is a convenience layer, so it
inherits Filament's mismatch and adds one more dependency above it.

### 4.2 `GLSurfaceView` / raw OpenGL ES — held, not chosen

This is what the planetariums actually do (§6), and it has one real advantage: **ES 2.0/3.0 is
universal on API 26+, so there is no API-33 split at all.** One renderer, one code path, every device.

It loses to Compose+AGSL on *effort*, not on capability: you hand-write shader programs, manage EGL and
surface lifecycle from Compose through `AndroidView`, and — the expensive part — every star, label,
silhouette and FOV frame that Compose would draw for free becomes hand-written GL geometry and text.
It is strictly *more invention* for a scene that needs no depth buffer and no custom vertex stage.

**It stays on the table as the measured fallback (§7.3):** if the Compose path cannot hold the frame
budget on the A40 during a scrub, layer A moves into a `GLSurfaceView` behind a Compose UI, and nothing
above it changes.

### 4.3 Vulkan

Correct, powerful, and irrelevant here: it needs NDK/C++ because there are no first-party Kotlin
bindings, and its only advantages (explicit synchronisation, multi-threaded command recording,
advanced compute) address a scene this simple does not have. Rejected.

### 4.4 WebView + Three.js

`MIT`, and it is genuinely capable — but it puts the scene inside a browser engine: a second process
(`WebContent`) with its own memory floor, an IPC bridge for every slider value, and a scrub that is
exactly the worst-case interaction for that bridge (a value changing every frame). Rejected.

### 4.5 `graphicsLayer` layering, and Skiko

Neither is an alternative to the above — `graphicsLayer` is a **technique** the recommended renderer
uses to make scrubbing cheap (§7.1), and Skiko (`Apache-2.0`, Android API 24+) is a direct-Skia API that
Compose already sits on, so adding it would duplicate a layer rather than add one.

---

## 5. Does a turnkey, licence-clean library exist? — **No.**

Stated plainly, because it is the useful answer:

**There is no library you can add to `build.gradle.kts` that renders an astronomical sky — starfield by
magnitude, horizon, twilight gradient, moon phase, satellite track — under a licence this factory
permits.**

The two halves of the space are each empty:

- **Sky-domain code** is copyleft. Stellarium is `GPL-2.0`; Sky Map (`sky-map-team/stardroid`) is a
  **combined `GPL-3.0` distribution** with `Apache-2.0` legacy files and *"All Rights Reserved"*
  artwork; `ffalt/starfield` is `GPL-3.0`; the archived Stellarium-mobile forks are `GPL-2.0`. None can
  ship in a paid closed-source APK.
- **Permissively licensed graphics code is general-purpose.** Filament and SceneView (`Apache-2.0`) are
  3-D engines; Skiko (`Apache-2.0`) is Skia bindings; Artemis (`Apache-2.0`) is a Compose-Desktop
  graphics playground. A live search for a permissive Android starfield/planetarium renderer returned
  only a Compose "warp-speed" demo and Skia/wrapper projects — **no sky renderer**.

**What *is* licence-clean and reusable is the mathematics, not the renderer** — and the factory's recon
has already mapped it: Astronomy Engine for Kotlin/JVM is `MIT` (`astronomy.kt:2-7`), and Orekit +
Hipparchus are `Apache-2.0`. Those give the *positions* (sun, moon, twilight, altitude/azimuth, DSO
rise/set, SGP4 satellite passes). **Nobody gives you the picture.** The picture is the slot's own work
— which, given §1, is a gradient plus geometry, not a renderer.

---

## 6. What real planetarium apps use — and where the line is

Read from project pages this pass:

| Project | Rendering | Licence (read) | Shippable here? |
|---|---|---|---|
| **Stellarium** (desktop) | *"renders realistic skies in real time with **OpenGL**"* | `GPL-2.0` | **No** |
| **Sky Map / Stardroid** (Android) | Android app, Kotlin/Java, its own `SkyRenderer` | **`GPL-3.0` combined**; legacy files `Apache-2.0`; artwork All Rights Reserved | **No** |
| **Stellarium Mobile** (store version) | Independent project; *"does not share code with Stellarium desktop"*; no longer open source | Proprietary | Not applicable |
| **Stellarium-mobile 1.10 / `chengxinlun/Stellarium-android`** (archived) | Maemo-optimised **OpenGL ES port** | `GPL-2.0` | **No** |
| **Stellarium Web Engine** | WebGL | `AGPL-3.0` *(read in an earlier pass of this recon; not re-read this pass — treat as ON RECORD/UNVERIFIED)* | **No** |
| **Gaia Sky** | Java/libGDX, desktop/VR | `MPL-2.0` — weak copyleft, **not on this factory's allowlist** | **No** |
| **KStars / Cartes du Ciel** | Desktop | `GPL-*` | **No** |

**The line, stated explicitly so no one crosses it by accident.**

- **A published technique may be reimplemented.** That the sky is drawn as an angular projection with
  stars sized by magnitude, that the atmosphere is precomputed into a small colour ramp and sampled,
  that the horizon is composited as a silhouette over it, that the view is rotated about the celestial
  pole — these are ideas, and ideas are not copyrightable. Reimplementing them from the *published
  description* is legitimate.
- **Code may not be lifted, and neither may assets.** No file may be copied from a `GPL`/`AGPL`/`MPL`
  repository into this slot, no shader may be pasted from one, and no star catalogue or texture taken
  as-is without a licence read (§8). Stellarium Mobile's own artwork carries an explicit
  *"All Rights Reserved"* notice even where the surrounding project is open.
- **The practical rule for this slot:** do not read the GPL sources at all while writing the renderer.
  Read the mathematics (already `MIT`/`Apache-2.0` in this repo) and design the picture from §1. That
  removes the question rather than managing it.

---

## 7. Recommendation

### 7.1 The recommended architecture — a layered 2-D renderer, no 3-D engine

```
┌─ Compose: Box with fixed-size layers, each its own draw pass / graphicsLayer ───────────┐
│                                                                                         │
│  L1  SKY FIELD        skyGradient(sunAlt, moonAlt, az)                                  │
│      ├ API 33+  : RuntimeShader (AGSL)  → atmospheric scattering, colour-managed        │
│      └ API 26-32: 1-D colour LUT (e.g. 1×256 ARGB bitmap) sampled & stretched,          │
│                    regenerated only when (place, date, time) changes                    │
│                                                                                         │
│  L2  STARS            cached raster (bitmap) OR batched drawPoints, by magnitude        │
│  L3  DEEP SKY         small Paths / circles + labels                                    │
│  L4  MOON             Path (phase) + radial-gradient glow                               │
│  L5  SATELLITE        Path with gradient stroke (fading tail)                           │
│  L6  HORIZON          user's Path silhouette, drawn over the sky                        │
│  L7  FOV OVERLAY      frame + cardinal ticks                                            │
│  L8  UI               slider, readouts                                                  │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

**Four decisions carry the whole thing.**

1. **One source of truth for the palette, in both paths.** The sky is defined as a short list of
   `(sunAltitude → colour)` stops. On API 26–32 those stops are rasterised into the LUT; on API 33+ the
   same stops are passed to AGSL as `layout(color)` uniforms and interpolated (or expanded into the
   scattering model). **The two paths must be derived from the same declared stops**, or the app looks
   like two different apps depending on the phone — the same defect class the visual-identity rule
   already fails for a store pack whose palette drifted from `Color.kt`.
2. **AGSL is additive, never load-bearing.** Delete the API 33+ branch and the screen must still be the
   product. That is what makes the A40 a real test rather than a second-class citizen.
3. **The star field is a *cached raster*, not 5 000 live draw calls.** Stars move slowly even when the
   slider moves fast (a night is one rotation of the sky), so L2 is baked into a bitmap and **redrawn
   only when the sky has rotated past a threshold** — or, for the narrow fields the lens-FOV overlay
   implies, rotated as a single `drawImage` with a matrix transform. This is the mechanism that keeps a
   scrub cheap.
4. **Render on demand, not on a frame clock.** The product is a *scrub*, not a live animation. There is
   no `withFrameNanos` loop; a frame is produced when the slider value, the target selection or the
   orientation changes, plus a short bounded ease. This is the single largest battery decision in the
   app (§9).

**What this avoids inventing.** No engine, no shader pipeline, no EGL, no ECS, no asset pipeline. What
must be invented is a projection function, a gradient, and a handful of `Path`s — all of which are
already expressible with the `MIT`/`Apache-2.0` mathematics the recon has verified.

### 7.2 The rejected alternative, and the fact that separates them

**Rejected: Filament (or SceneView over it).**

**The separating fact:** *the scene has no 3-D content to render.* Filament is, in the README's own
words, *"a real-time **physically based** rendering engine"* — its value is in meshes, materials,
image-based lighting and glTF scenes. Every element of this hero screen is a 2-D angular position:
there is no depth to sort, no normal to light, no mesh to project. Deciding between "2-D layered
renderer" and "3-D engine" was decided by asking which of Filament's features the scene consumes; the
answer is **none**, while the one operation the scene *does* need per-pixel — the sky gradient — still
has to be written by hand as a custom shader either way. A 3-D engine chosen here would be a cost
(native `.so`, a second render context, a material subsystem) paid for features that are not used.

That answer also survives the owner's instruction that the visual budget should buy **realism, not
polygons**: the recommended renderer spends its budget on the gradient and on magnitude-accurate star
sizes, which is where the realism lives; polygons are precisely what this scene has none of.

**Also rejected, in the same decision, with their own separating facts:**

| Rejected | Separating fact |
|---|---|
| `GLSurfaceView` + raw OpenGL ES | Universally available (no API-33 split) — but it re-introduces EGL/surface lifecycle, hand-written geometry **and hand-written text/labels** for everything Compose draws for free. More invention, same picture. **Held as the §7.3 fallback rather than discarded** |
| WebView + Three.js | A second process and an IPC bridge across exactly the interaction (continuous scrub) that punishes it |
| Vulkan | Needs NDK/C++; only buys control this scene has no use for |
| Lottie/Rive | The scene is a function of (place, date, time, user horizon) and cannot be pre-baked |

### 7.3 The fallback, named in advance so it is a decision and not a scramble

If the Compose path cannot hold its frame budget on the A40 while scrubbing, **move layer A only**
into a `GLSurfaceView` (ES 2.0, universally available) behind the Compose UI. Layers B–F stay in
Compose; the LUT palette becomes the shader's uniform. The architecture above does not change, which is
what makes this a fallback rather than a rewrite.

---

## 8. The licence trap that is not the renderer: the star catalogue

Rendering cleanly is necessary and not sufficient. **"Starfield by magnitude" requires star data, and
the common catalogues are `CC BY-SA`, which this factory denies.**

| Catalogue | Licence (read this pass) | Status here |
|---|---|---|
| **HYG database v4.0+** | **`CC BY-SA 4.0`** (v2/v3 were `CC BY-SA 2.5`), declared in the repo and its `LICENSE` | **Denied** by `docs/research/2026-10-02-ui-icon-libraries-and-licensing.md:197`, `:642-643` |
| **Yale Bright Star Catalogue**, 5th ed. | *"a published catalog, **not** public domain"*, made available for dissemination through the ADC | **UNVERIFIED** — needs its own licence reading, not an assumption |
| **Gaia DR3** | `CC BY-SA 3.0 IGO` (ESA) | **Denied** (`BY-SA`) |
| **OpenNGC** (deep-sky) | `CC BY-SA 4.0`, declared in the upstream README, **no `LICENSE` file** | Already flagged in the recon market doc §6.1 as *"a legal decision, not an evidence gap"* |

So the same question the recon already raised for the deep-sky catalogue applies to the **star**
catalogue, and it must be settled **before** the renderer is written around it, because the answer
changes what the star field even is.

**Routes that avoid the problem, named so the decision is a choice rather than a wall:**

- **Procedural / synthetic star field** — generate the stars from a deterministic function of
  (magnitude, position). Licence-clean by construction. The honest cost: it is *not* the real sky, so
  it cannot be used to identify a target, which is the app's job.
- **A public-domain / permissive source, licence read on its own page** — the only acceptable route for
  real stars, and it needs a named candidate with its licence read in this session before it is used.
- **Owner/legal decision**, exactly as the recon's §10.1 asks for OpenNGC — because whether a
  build-time-trimmed `CC BY-SA` subset may be bundled in a closed-source paid APK that carries the
  *scientific facts* of a catalogue is a legal question no program here settles.

**This is not a rendering blocker; it is a shipping blocker, and it is the one item in this report that
cannot be resolved by engineering.**

---

## 9. Performance and battery on the real test phone

### 9.1 The device, measured from public specification pages this pass

| Property | Value | Source |
|---|---|---|
| Model / serial | `SM-A405FM`, serial `R58MB3SGZVL` (the factory's designated test phone) | `docs/research/2026-10-08-light-and-sky-session-planner-market.md:479-482` |
| OS at test | **Android 11** → **API 30** | same |
| SoC | Exynos 7904 (14 nm) | GSMArena, devicespecifications |
| GPU | **ARM Mali-G71 MP2, 770 MHz, 2 cores** | devicespecifications |
| Display | 5.9", Super AMOLED, **1080 × 2340**, ~437 ppi | GSMArena |
| Battery | **3100 mAh** | GSMArena |

**The number that matters: 1080 × 2340 ≈ 2.53 M pixels, filled by a 2-core 2019 GPU.** Any full-screen
per-pixel shader is a fill-rate problem on this device, and a per-pixel *loop* is a catastrophe (§3.2).

### 9.2 Honest risk assessment

| Risk | Severity | Why | Mitigation |
|---|---|---|---|
| **The test phone cannot run AGSL at all** | **Highest — and procedural** | A40 = Android 11 = API 30 < 33. The designated verification device exercises only the LUT fallback, so the AGSL path is unverifiable on factory hardware | Make AGSL additive (§7.1 #2); drive the AGSL path on an API 33+ device only on the owner's word, and say plainly in the handoff which path the A40 observed |
| **Colour drift between the two layer-A paths** | High | AGSL is colour-managed, works in the destination space, and returns premultiplied values; the LUT is whatever bytes you wrote. Two paths = two chances to disagree | Derive both from the same declared stops; pass stops as `layout(color)` uniforms; diff the two paths' output — this is exactly what `scripts/visual-qa.py`'s palette and contrast sections exist to catch |
| **A per-pixel atmosphere model is too expensive on Mali-G71 MP2** | High | AGSL cannot loop over a dynamic range, and the unrolled version costs the loop's body × iteration count *per pixel* × 2.53 M pixels | **Precompute** the scattering into the LUT — on the CPU or once on the GPU — and have the shader sample it. The shader then does a handful of ALU ops and one fetch |
| **Star-field throughput** | Medium | Thousands of antialiased points per frame through Skia→GL is CPU-bound work on a 2019 mid-range SoC | Cache the star layer as a raster and rotate/redraw it (§7.1 #3); batch `drawPoints` by magnitude; cap the catalogue to the naked-eye limit for the default view and only densify on zoom |
| **Battery from an animated shader** | Medium | A continuously animating full-screen shader is the most expensive mode there is; a 3100 mAh cell makes it visible | **Render on demand.** No frame clock; a frame per input change. Cap any ease at ~30 Hz. An idle scene costs nothing |
| **Thermal/GPU throttling during long scrubs** | Low–Medium | Sustained fill on a small chassis | On-demand rendering means no sustained fill; a scrub is a burst |
| **False comfort from AMOLED** | — | The scene is predominantly dark and OLED black is near-zero power — genuinely favourable — but this was **not measured on this device** | Treat as an expectation, not a result; measure before claiming it |

### 9.3 What is verified today, and what is not

**Verified:** the device model, OS/API level and the fact that the factory's own SGP4 core already
built, installed, launched and ran correctly on this exact phone (the recon's `G-e-android-build-proof`
records `OREKITSMOKE_OK`, zero linkage/`FATAL` errors across 3,908 logcat lines). That tells us the
*A40 can run this app's compute*; it says nothing about frame timing.

**Not verified — and named so it is not implied:** there is no frame-time, GPU-utilisation, thermal or
battery measurement for a full-screen composited sky scene on this device. **No figure in §9.2 is a
measurement.** The check that would settle each is listed in §12.

---

## 10. Risk register (ranked, with the owner's decision where the answer is not ours)

| # | Risk | Kind | Who decides | Settled by |
|---|---|---|---|---|
| 1 | The designated test phone is below the AGSL floor, so the enhanced path is unverifiable on factory hardware | **Procedural + technical** | Owner (second device) | `adb devices` for an API 33+ serial; an on-device run of the AGSL path |
| 2 | Star/deep-sky catalogue is `CC BY-SA`, which the factory denies | **Legal** | **Owner** | A licence read per chosen source, or an owner decision on the OpenNGC/HYG question |
| 3 | Frame budget on Mali-G71 MP2 during a scrub | Technical | Builder | `gfxinfo` frame timings for the hero screen on `R58MB3SGZVL` |
| 4 | The two layer-A paths visibly disagree | Product quality | Builder | Side-by-side capture of the same instant on API 30 and API 33+; `visual-qa.py` palette/contrast |
| 5 | Atmosphere model too costly per pixel | Technical | Builder | A shader with the loop, timed on device — expect it to fail, and design for the LUT |
| 6 | Star catalogue size vs. the 20 MB budget | Product | Builder | Measured size of the trimmed catalogue asset (the deep-sky trim was already estimated at ~200–600 KB in the recon, `INFERRED`) |

Items 1 and 2 are the two that cannot be engineered away.

---

## 11. What this means for the slot, concretely

- **Dependencies added for rendering: none.** The recommendation is the platform (`Canvas`,
  `RuntimeShader`) plus the mathematics the recon already verified (`MIT` / `Apache-2.0`). A renderer
  that adds no dependency cannot add a licence problem, an ABI split, or a size regression.
- **The build-size question becomes a non-question.** A 2-D renderer is code, not an asset pack. The
  20 MB budget absorbs a star catalogue and a LUT (kilobytes), not an engine (megabytes of `.so`).
- **A new, small, testable surface** appears: one pure function `skyColour(u: Float, sunAlt: Float, …)
  → Color`, and one `project(alt, az, view) → Offset`. Both are unit-testable without a GPU, and both
  are the *same* function on both API paths. That is the design that keeps the two paths from drifting.
- **One clarification worth carrying into the spec:** the hero screen's realism budget is best spent on
  (a) the twilight gradient, (b) magnitude-accurate star sizes, (c) a physically plausible moon glow,
  and (d) a strictly correct limb/horizon composite. None of those four is a polygon.

---

## 12. The check that would settle each open question

```text
# 1. Is a second, API 33+ device available? (owner's word required to use it)
adb devices -l

# 2. Which API level is the test phone actually on?
adb -s R58MB3SGZVL shell getprop ro.build.version.sdk      # expect 30 -> AGSL absent

# 3. Does the hero screen hold a frame budget while scrubbing?
adb -s R58MB3SGZVL shell dumpsys gfxinfo com.gexetr.<slug> framestats

# 4. Is the scene GPU-bound or CPU-bound?
adb -s R58MB3SGZVL shell dumpsys gfxinfo com.gexetr.<slug>

# 5. Battery cost of a scrub vs. an idle scene
adb -s R58MB3SGZVL shell dumpsys batterystats --charged com.gexetr.<slug>

# 6. Do the two layer-A paths agree visually?
#    - screenshot the same instant on an API 30 device and an API 33+ device
#    - then: python scripts/visual-qa.py --slot <slug>

# 7. Does the AGSL program even compile for this scene (loop unrolling limit)?
#    - instantiate RuntimeShader with the intended program; a compile failure throws at construction

# 8. Licence of whatever catalogue is chosen
#    - read the source's own licence page in the session that adopts it (never from memory)
```

---

## 13. Claim ledger

```text
CLAIM / LABEL / EVIDENCE
AGSL is used by Android 13 and above to define programmable RuntimeShader behaviour / MEASURED / https://developer.android.com/develop/ui/views/graphics/agsl ("AGSL is used by Android 13 and above to define the behavior of programmable RuntimeShader objects")
RuntimeShader does not exist below API 33, so minSdk 26 leaves API 26-32 with no AGSL path at all / INFERRED / the MEASURED AGSL floor above, against the slot's minSdk 26; no software fallback is described on that page
The factory's designated test phone runs Android 11 => API 30, i.e. below the AGSL floor / MEASURED / docs/research/2026-10-08-light-and-sky-session-planner-market.md:479-482 ("Serial R58MB3SGZVL, ro.product.model = SM-A405FM (Galaxy A40), Android 11")
A RuntimeShader calculates a per-pixel colour / MEASURED / https://developer.android.com/reference/android/graphics/RuntimeShader ("A RuntimeShader calculates a per-pixel color based on the output of a user defined Android Graphics Shading Language (AGSL) function.")
AGSL contributes a function to the Canvas/RenderNode fragment shader, not a whole GPU pipeline program / MEASURED / https://developer.android.com/develop/ui/views/graphics/agsl ("Your AGSL effect contributes a function (or functions) to the GPU's fragment shader.")
AGSL's coordinate origin is upper-left (Canvas convention), unlike GLSL / MEASURED / RuntimeShader reference ("AGSL matches the screen coordinate system of the Android Canvas which has its origin as the upper left corner")
AGSL expects premultiplied alpha from main() / MEASURED / RuntimeShader reference ("In ASGL the color returned by the main function is expected to be premultiplied")
AGSL can take float[] and int[] uniforms, colour uniforms with layout(color), input shaders and raw input buffers / MEASURED / RuntimeShader reference (uniform table listing setFloatUniform(String,float[]) and setIntUniform(String,int[]); the layout(color) section; the AGSL quick reference's setInputBuffer / setInputShader rows)
AGSL for-loops must be unrollable, with constant initializer, condition and next / MEASURED / https://developer.android.com/develop/ui/views/graphics/agsl/agsl-quick-reference ("'for' loops are quite limited; the compiler must be able to unroll the loop. This means that the initializer, the test condition, and the 'next' statement must use constants")
AGSL arrays are 1-D, fixed-size, non-copyable, and indexable only by a constant or a loop variable / MEASURED / AGSL quick reference ("Arrays can only be indexed using a constant or a loop variable"; "Arrays cannot be returned from a function, copied, assigned or compared")
AGSL has no sampler2D; sampling is via a uniform shader's .eval() or a raw buffer via setInputBuffer / MEASURED / AGSL quick reference ("Sampler types aren't supported, but you can evaluate other shaders"; "you can use a BitmapShader as a generic raw buffer using RuntimeShader#setInputBuffer")
AGSL has no recursion and discard is not allowed / MEASURED / AGSL overview ("User defined functions are supported, without support for recursion of any kind"); AGSL quick reference ("Jump | break, continue, return (discard is not allowed)")
A real star catalogue cannot be evaluated per pixel in AGSL because the loop bound cannot be a uniform and the body is inlined per iteration / INFERRED / the two MEASURED loop/array restrictions above, applied to a catalogue of thousands of stars
Filament is Apache-2.0 / MEASURED / https://raw.githubusercontent.com/google/filament/main/LICENSE (Apache License 2.0, "Copyright 2023 The Android Open Source Project")
SceneView is Apache-2.0 / MEASURED / https://raw.githubusercontent.com/SceneView/sceneview-android/main/LICENSE (Apache License 2.0)
Filament on Android requires OpenGL ES 3.0+ (Vulkan 1.0+ also supported, backend auto-selected) and the Android library declares minSdk 21 / ON RECORD / https://google-filament.mintlify.app/platforms/android ("OpenGL ES 3.0+ - Wide device compatibility"); https://github.com/google/filament/blob/main/android/build.gradle ('minSdk': 21, abis arm64-v8a/armeabi-v7a/x86_64/x86)
Filament's current Maven artifact is com.google.android.filament:filament-android:1.72.0 / ON RECORD / https://github.com/google/filament/blob/main/README.md
Filament is described by its own README as a real-time physically based rendering engine / ON RECORD / https://github.com/google/filament/blob/main/README.md ("Filament is a real-time physically based rendering engine for Android, iOS, Linux, macOS, Windows, and WASM")
Whether the A40's Mali-G71 MP2 is on Filament's practical supported list is unverified / UNVERIFIED / would need an on-device Filament run; not settled by the MEASURED ES 3.0+ requirement
Sky Map (stardroid) is a combined GPL-3.0 distribution with Apache-2.0 legacy files and All Rights Reserved artwork / MEASURED / https://github.com/sky-map-team/stardroid ("the project as a combined distribution is compiled and distributed under the terms of the GPLv3"; "proprietary visual artwork ... Copyright (c) 2026 Penterakt LLC, All Rights Reserved")
Stellarium desktop renders with OpenGL and is GPL-2.0 / MEASURED / https://github.com/stellarium/stellarium ("a free GPL software which renders realistic skies in real time with OpenGL"; "GPL-2.0 license")
Stellarium Mobile (the store version) shares no code with Stellarium desktop and is no longer open source / ON RECORD / github.com/Stellarium/stellarium issues/discussions, e.g. issue #3850 and discussion #3252 ("it's a separate project ... sharing no code with Stellarium"; "The actual independent project seen in the app stores ... is no longer open-source")
ffalt/starfield (Android starfield live wallpaper) is GPL-3.0 / MEASURED / https://github.com/ffalt/starfield/ ("License: GNU General Public License v3.0")
Skiko (Kotlin bindings to Skia) is Apache-2.0 and supports Android from API 24 / MEASURED / https://github.com/JetBrains/skiko/ ("License: Apache License 2.0"; "Kotlin/JVM on Android(x86_64 and arm64), starting with API version 24")
Skiko is redundant here because Compose already draws through Skia / INFERRED / Compose's Canvas is documented as a Skia-backed drawing API; no repository check was run for this row
No permissive, licence-clean Android library renders an astronomical sky / MEASURED (search) + INFERRED (synthesis) / live search returned only Skiko (Apache-2.0, Skia bindings), Artemis (Apache-2.0, a Compose-Desktop graphics playground), stardroid (GPL-3.0), a Compose warp-speed demo, and ffalt/starfield (GPL-3.0) - no sky renderer; the two sky-domain candidates are the GPL projects above
The factory's licence allowlist is MIT/ISC/Apache-2.0/OFL-1.1/CC-BY-4.0 (+ MIT-0, BSD-2/3-Clause) and its denylist includes CC-BY-SA-*, GPL-*, AGPL-*, LGPL-* / MEASURED / docs/research/2026-10-02-ui-icon-libraries-and-licensing.md:634 (allowlist), :642-643 (denylist patterns), :197 (the CC-BY-SA/GPL/AGPL rationale)
The HYG stellar database (v4.0+) is CC BY-SA 4.0 / MEASURED / https://codeberg.org/astronexus/hyg ("This work is licensed under a Creative Commons Attribution-ShareAlike 4.0 International License"; earlier versions CC BY-SA 2.5) and https://raw.githubusercontent.com/astronexus/HYG-Database/main/LICENSE
The Yale Bright Star Catalogue is described as a published catalogue rather than public domain / ON RECORD / https://ui.adsabs.harvard.edu/abs/1995yCat.5050....0H/abstract ("The Yale Bright Star Catalogue is a published catalog, not public domain; its fifth edition was made available for dissemination through the Astronomical Data Center")
Gaia DR3 data is released under CC BY-SA 3.0 IGO / ON RECORD / https://www.cosmos.esa.int/web/gaia/dr3-non-single-stars ("Credits: ESA/Gaia/DPAC - CC BY-SA 3.0 IGO")
The OpenNGC deep-sky catalogue is CC-BY-SA-4.0 with no LICENSE file, already flagged as needing an owner/legal decision / MEASURED / docs/research/2026-10-08-light-and-sky-session-planner-market.md:336-370
Astronomy Engine for Kotlin/JVM is MIT and Orekit/Hipparchus are Apache-2.0, so the sky mathematics is licence-clean while the renderer is not supplied / MEASURED (Astronomy Engine) / docs/research/2026-10-08-light-and-sky-session-planner-market.md:424 ("Astronomy Engine for Kotlin/JVM - MIT. MEASURED - F §2.1, astronomy.kt:2-7"); the Orekit/Hipparchus position is ON RECORD from the same document's engine table
The Galaxy A40 is Exynos 7904 with a Mali-G71 MP2 (2 cores, 770 MHz), 1080x2340 Super AMOLED, 3100 mAh / ON RECORD / https://www.gsmarena.com/samsung_galaxy_a40-9642.php (Exynos 7904; 1080x2340; Li-Po 3100 mAh) and https://www.devicespecifications.com/en/model-cpu/b7cb4ffe ("GPU: ARM Mali-G71 MP2, 770 MHz, Cores: 2")
The factory's SGP4 core already built, installed and ran on this exact phone / MEASURED / docs/research/2026-10-08-light-and-sky-session-planner-market.md:479-482 and :708 ("installed, launched, printed OREKITSMOKE_OK; zero linkage/FATAL errors in 3,908 logcat lines")
No frame-time, GPU-utilisation, thermal or battery measurement exists for a full-screen composited sky scene on this device / UNVERIFIED / no such measurement is in this file or in the recon's six files; settled by the §12 gfxinfo/batterystats commands on serial R58MB3SGZVL
An on-demand (render-on-input) shader scene costs less battery than a continuously animating one on a 3100 mAh device / INFERRED / from the device's measured capacity and the general fill-rate cost of full-screen fragment shading; NOT measured on this device
The scene is predominantly dark, and OLED black is near-zero power, which is favourable / INFERRED / from the device's MEASURED Super AMOLED panel and OLED emission physics; not measured for this app
Three.js is MIT / ON RECORD (earlier pass of this recon) / not re-fetched in this pass; the version number is therefore UNVERIFIED here
Stellarium Web Engine is AGPL-3.0 / ON RECORD (earlier pass of this recon) / not re-fetched in this pass; treated as copyleft and therefore denied either way
```

**The one honest limit of this report.** It settles *which* rendering technologies are available, which
are licence-clean, and which fit the scene's shape. It does **not** contain a single frame-time or
battery measurement for this app on this phone — every performance statement in §9 is a risk judgement
from the device's measured specification, not a result. The §12 commands produce the results; until
they are run, "fast enough on the A40" is `UNVERIFIED`, and this report says so rather than implying it.
