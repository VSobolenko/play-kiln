# I — Sky assets and data: licences, shippability, and the OpenNGC replacement

**Worker:** assets-and-data research (dispatch `I`).
**Date:** 2026-10-08.
**Product judged against:** a **paid ($9.99), closed-source, no-ads** Android light-and-sky session
planner (`purchase_only_no_ads`) whose hero screen is a scrubbable sky scene.
**Scope:** star catalogue, deep-sky catalogue, constellation lines/names, Moon + planet imagery,
photographic Milky Way backdrop, and any other licence-clean asset that would make the scene better.

**Method:** every licence below was **read from the source's own page in this session** with the
command recorded in the ledger, unless the row says otherwise. Where a prior measurement exists in
this task's own earlier session or in the sibling adjudication file
(`docs/research/2026-10-08-light-and-sky-session-planner-market.md`), the row says so and names it.
Nothing here is a remembered licence.

---

## 0. The bar this report judges against

The factory's own licensing research puts these on the **denylist for a paid closed-source app**:

> *"ShareAlike — you must distribute your contributions under the same license; incompatible with a
> paid closed-source app."*
> — `docs/research/2026-10-02-ui-icon-libraries-and-licensing.md:147`, `docs/icon-provenance.md:174`

Denylist as applied here: **`CC-BY-SA-*`, `GPL-*`/`LGPL-*`/`AGPL-*`, `CC-BY-NC-*`, `CC-BY-ND-*`,
proprietary/"all rights reserved" terms.** Allowlist: **`MIT`, `ISC`, `Apache-2.0`, `OFL-1.1`,
`CC-BY-4.0`, `CC0`**. The difference that decides almost every row below is between **`CC BY 4.0`**
(shippable with attribution) and **`CC BY-SA 4.0`** (share-alike, the thing we are escaping), and
between both of those and **`CC BY-NC 3.0 IGO`** (non-commercial — unusable at any price).

Three sources are **explicitly not "public domain" just because they look open**, and each is
measured below: NASA (**the NASA insignia is *not* public domain**), HEASARC (freely usable, but the
Messier table is compiled from a copyrighted Sky Publishing book), and Wikimedia (**Wikipedia text is
CC BY-SA 4.0 — only *Wikidata structured data* is CC0**).

---

## 1. Star catalogue (positions + magnitudes + proper motion)

### 1.1 The trap — every famous bright-star catalogue is encumbered

| Catalogue | Exact licence (measured this session unless noted) | Shippable? |
|---|---|---|
| **HYG v4.4** (`astronexus/HYG-Database`, now at Codeberg) | **CC BY-SA 4.0** — the repo's `LICENSE` reads verbatim: *"This work is licensed under a Creative Commons Attribution-ShareAlike 4.0 International License"* | ❌ **denylist (share-alike)** |
| **Hipparcos / Tycho** (ESA) | **CC BY-NC 3.0 IGO** — ESA states: *"The Hipparcos and Tycho Catalogues are distributed under the CC BY-NC 3.0 IGO licence and the Terms and Conditions for the use of these data require users to acknowledge ESA using 'Credit: ESA'."* | ❌ **non-commercial** |
| **Gaia DR3** (ESA) | **CC BY-NC 3.0 IGO** (same ESA licence family; the ESA acknowledgements page did not load this session — see ledger) | ❌ **non-commercial** |
| **AAVSO APASS DR10** | AAVSO data policy: *"free for your own personal use and for use in publications"*; the older policy block reads *"All rights reserved. No part of these data may be reproduced, transmitted, distributed, published … without the express written permission of the AAVSO."* | ❌ without written permission |
| **Yale Bright Star Catalogue** (BSC5, VizieR `V/50`; HEASARC mirror `bsc5p`) | **No SPDX licence.** The CDS `V/50/ReadMe` carries **no copyright notice** (measured — grep for `copyright`/`licence`/`commercial` returned nothing). HEASARC's copy says *"HEASARC materials are all available freely for your use"* and asks for an acknowledgement, and records that it was *"created by the HEASARC in 1995 based upon a file obtained from either the ADC or the CDS"*. The underlying catalogue is © Hoffleit/Warren (Yale/SAO). | ⚠️ **UNVERIFIED** — no affirmative commercial grant; see §1.3 |
| **HYG / AT-HYG derived sets, most "star databases" on GitHub** | inherit CC BY-SA from HYG | ❌ |

**Consequence, stated plainly:** the catalogue the app would naturally reach for (Hipparcos/HYG/BSC5)
is either **CC BY-SA**, **CC BY-NC**, or **unlicensed-by-default**. There is no widely-used
"modern permissive bright-star database" that is unambiguously `CC0`/`CC BY 4.0`.

### 1.2 What *is* clean

| Source | Exact licence (measured this session) | What it gives | Shippable? |
|---|---|---|---|
| **FK6** (Sixth Fundamental Catalogue), GAVO Data Centre | **CC0 1.0** — the GAVO service metadata "Rights" block reads verbatim: *"To the extent possible under law, the publisher has waived all copyright and related or neighboring rights to the FK6 data. For details, see the Creative Commons CC0 1.0 Public Domain dedication."* (`dc.g-vo.org/fk6/q/cone/form`) | **4,150 fundamental stars** with very high-precision positions **and proper motions** (i.e. exactly the bright stars a naked-eye scene draws) | ✅ **yes — no attribution even required** |
| **2MASS All-Sky Point Source Catalog** (IRSA) | Acknowledgement requested, not a licence restriction; IRSA states the exact required string (see §9). No commercial prohibition found. *(An explicit "public domain" sentence was **not** found on the pages read — see §1.3.)* | Full-sky positions + J/H/K magnitudes, ~470 M sources — **fills the faint star field** between the FK6 bright stars | ✅ with the acknowledgement (J/H/K are IR magnitudes — a **visual-magnitude mapping is required**, not a straight copy) |
| **Wikidata** | **CC0 1.0** — *"All structured data … is released into the public domain under Creative Commons Zero"*; *"We offer the data in Wikidata freely and with no requirement for attribution under CC-0"* (`wikidata.org/wiki/Wikidata:Licensing`, `…:Data_access`) | star items exist but coverage is sparse | ✅ but **thin** — not a catalogue |

### 1.3 Verdict on stars

**Recommended clean set — `FK6 (CC0)` for the bright end + `2MASS (PD-style, acknowledgement)` for the
faint field.** This is fully shippable in a paid closed-source app with **zero share-alike risk** and
no non-commercial risk. Two honest costs are attached and must be stated in the build plan:

1. FK6 is **4,150 stars**, not a full naked-eye catalogue (~9,000 stars to mag ≈ 6.5). The scene gets
   its accuracy from FK6 and its density from 2MASS; faint-star photometry must be mapped from IR to
   an approximate visual magnitude (an **INFERRED** transform, which the builder must own and label
   in-app as an approximation, not as measured V magnitude).
2. There is **no permissive bright-star catalogue with a "true" V magnitude** that I could find.
   BSC5 is the classical answer and is **`UNVERIFIED`**: no explicit commercial grant exists, and the
   only affirmative statement I located is the ADC's historical "free to distribute **with
   acknowledgement**, do not alter the data", which is not a `CC0`/`CC BY` grant. *If the owner wants
   BSC5, that is a decision to take on an unlicensed-by-default US/ESA catalogue, documented — not a
   clean asset.*

**Do not** ship HYG, Hipparcos, Tycho, Gaia (including any Gaia-derived "AT-HYG" set), or APASS.

---

## 2. Deep-sky object catalogue — the OpenNGC replacement question

### 2.1 Why OpenNGC is a problem (already adjudicated, restated for the record)

`docs/research/2026-10-08-light-and-sky-session-planner-market.md` §6.1 settles it: the upstream
README line reads `… license limitations, OpenNGC is released under CC-BY-SA-4.0 license`, there is
**no `LICENSE` file** (`…/master/LICENSE` → **HTTP 404**), and GitHub's licence detector returns an
**empty `license.spdx_id`**. `NGC.csv` is live (3,877,591 bytes). The factory denylist therefore
bites: **`CC-BY-SA-4.0` is on the denylist for a paid closed-source app.**

### 2.2 Every replacement route I could find

| Candidate | Exact licence (measured this session unless noted) | Shippable? |
|---|---|---|
| **OpenNGC** | **CC BY-SA 4.0** (README; no LICENSE file) | ❌ denylist |
| **NGC 2000.0** (Sinnott, VizieR `VII/118`) | **Explicitly non-commercial** — read verbatim this session: *"Copyright Notice: This catalog is copyrighted by Sky Publishing Corporation … for scientific research purposes only. The data should not be used for commercial purposes without the explicit permission of Sky Publishing Corporation."* | ❌ **non-commercial** |
| **SAC Deep-Sky Database v8.1** (Saguaro Astronomy Club) | Freeware for private use; "may not be sold as a separate entity"; no explicit grant for bundling in paid software | ❌ / ⚠️ permission needed |
| **Stellarium DSO data** | GPL | ❌ |
| **HEASARC `MESSIER` table** (NASA/GSFC) | HEASARC policy: *"HEASARC materials are all available freely for your use"* + acknowledgement. **But** the page itself records that *"This version of the Messier Catalog was compiled primarily from Table III of Sky Catalogue 2000.0 … Sky Publishing Corporation"* — i.e. it is a US-government **distribution** of a third party's copyrighted compilation | ⚠️ risky |
| **VizieR / SIMBAD / NED-derived** | CDS: *"The copyrights of a catalogue depend on the data origin … refer to the ReadMe"*; NED's published policy regulates fair use/high-volume access and grants **no** commercial licence | ⚠️ |
| **Wikidata** (structured data) | **CC0 1.0** | ✅ **clean** — but it is a per-object database, not a catalogue, and its NGC coverage must be audited (see below) |

*(Also checked: d3-celestial ships deep-sky and constellation data under a **BSD-3-Clause** package
licence — measured via `api.github.com/repos/ofrohn/d3-celestial` → `"spdx_id":"BSD-3-Clause"` — but
the package licence covers the code, and the provenance of its compiled data files is **not**
established by that file; that is a gap, not a clearance.)*

### 2.3 Verdict: **No — no drop-in permissive replacement for OpenNGC exists.**

There is **no complete, permissively-licensed NGC/IC catalogue** I could find. Every full catalogue is
`CC BY-SA 4.0`, non-commercial, GPL, or unlicensed. What **does** exist is a **clean route to the
subset this app actually needs**:

> **A curated browse list — Messier (110) + the few hundred brightest NGC/IC objects — compiled from
> `Wikidata (CC0)`, cross-checked against `HEASARC` (freely usable, US-gov) and released with our own
> object descriptions and types.** Designations and positions of 18th/19th-century objects
> (Messier 1781, Dreyer 1888) are public-domain *facts by age*; Wikidata then supplies the CC0
> structured layer.

Two costs are attached, and neither can be hand-waved:

- **Coverage is not OpenNGC's, and the probes measured exactly how it falls short.** Three probes
  were run (all in §11):
  - The obvious class query (`?i wdt:P31 wd:Q14530`, *instance of "Messier object"*) returned **`0`** —
    the class exists but is **not** used as an `instance of` target, so that route is simply the wrong
    question.
  - The two global catalogue-code counts (`P528` starting `"NGC "` / `"IC "`) **did not answer**:
    **HTTP 504 (Gateway Timeout)** and **HTTP 429 (Too Many Requests)**. A **global** count is
    therefore still `UNVERIFIED`.
  - A **bounded** probe over five named objects (fast, HTTP 200) **did** answer, and this is the useful
    result. Four of five resolved by `P528` catalogue code — `M 31` → Q2469 *Andromeda Galaxy*,
    `NGC 7000` → Q265515 *North America Nebula*, `NGC 869` → Q482454 (item exists, **no English
    label**), `IC 434` → Q2355660. (`"Messier 42"` resolved nothing: the code form is `"M 42"`, so
    **the literal spelling of the code matters**.)

**Row-level completeness of the Wikidata route — measured on three objects:**

| Object | RA (`P6257`) | Dec (`P6258`) | Apparent magnitude (`P1215`) | Label |
|---|---|---|---|---|
| Q2469 *Andromeda Galaxy* (M 31) | ✅ `+10.6847083°` | ✅ `+41.2687500°` | ⚠️ **6 conflicting statements** (+4.86, +4.36, +3.44, +2.094, +1.283, +0.984) | ✅ |
| Q265515 *North America Nebula* (NGC 7000) | ✅ `+314.6958°` | ✅ `+44.3300°` | ⚠️ one value, `+4` (coarse) | ✅ |
| Q2355660 *IC 434* | ✅ `+85.25°` | ✅ `−2.50°` | ❌ **absent** | ✅ (label is the bare code) |

**What that means in plain terms.** The field the sky scene **cannot work without — position — is
present** on all three tested objects, as `P6257`/`P6258` (degrees), *not* as `P625` (the property my
first probe asked for, which is why that probe returned no coordinates). The field needed to **size**
a deep-sky marker — magnitude — is **uneven and internally contradictory**: M31 carries six disagreeing
values, NGC 7000 a single coarse one, and IC 434 none at all. So the Wikidata route supplies
**positions reliably and magnitudes unreliably**, and the curated build must either take magnitudes
from a second source (`HEASARC`, freely usable, in §9) or size markers by **object type** instead of
by magnitude. An object-level audit along the `P528` path is **required work before any coverage
number is promised**, and it is the price of the clean licence.
- **The sky scene loses the "browse 13,000 deep-sky objects" claim.** If that claim is load-bearing
  for the product, then the honest options are (a) accept `CC BY-SA 4.0` on a separated derived
  catalogue **with legal review**, or (b) ship without it.

### 2.4 If we keep OpenNGC: exactly what obligation we would accept

State it plainly rather than pretending it is clean: **CC BY-SA 4.0 requires attribution *and*
share-alike on the adapted material.** A trimmed/massaged OpenNGC subset is an adaptation, so we
would have to license *that catalogue* (and its derivatives) under CC BY-SA 4.0, publish the
attribution, and indicate changes. Whether a *bundled data file inside a closed-source paid app* can
satisfy that without reaching the app's own code is **a legal question this research does not
settle** — it is not a licence-clean asset, and it should be treated as a legal-review item, not as
an engineering step. **The recommended answer is: do not ship OpenNGC; build the curated CC0 set.**

---

## 3. Constellation lines and names

### 3.1 The GPL trap (confirmed shape, not restated as fact about bytes)

Stellarium's default sky-culture / constellation data is distributed under **GPL-2.0+** terms;
**GPL code or data cannot ship in this closed-source paid app.** Flagged and moved past, as instructed.

### 3.2 Clean candidates

| Candidate | Exact licence | Shippable? |
|---|---|---|
| **IAU constellation charts** (`iau.org`, figures by **Alan MacRobert** drawn with **Sky & Telescope**'s Roger Sinnott & Rick Fienberg) | **CC BY 4.0** — the IAU page states verbatim: *"The images are released under the Creative Commons Attribution 4.0 International license."* The IAU `Copyright` page confirms: *"The images, videos and web texts on iau.org are released under the Creative Commons Attribution 4.0 International license and may on a non-exclusive basis be reproduced without fee…"* | ✅ **yes**, with credit in full (IAU: *"Please reproduce the credit in full… their names must not be removed"*) — **not** `CC BY-SA` |
| **`dcf21/constellation-stick-figures` → `constellation_lines_iau.dat`** | **Conflicting.** The file's own description says the modern IAU stick figures *"are released under a Creative Commons Attribution 4.0 International license"*; the **repository footer** says *"This data is distributed under the Gnu General Public License Version 3."* | ⚠️ **do not ship as-is** — see §10 |
| **`d3-celestial`** (package) | **BSD-3-Clause** (measured via the GitHub API) | ✅ for the *code*; ⚠️ its compiled constellation/dataset **provenance is not cleared by that licence** |
| **Constellation boundaries** | Ships inside the **same IAU CC BY 4.0 chart pack** (the page lists a per-constellation *"Constellation Boundary TXT (… KB)"*) | ✅ CC BY 4.0 |
| **IAU WGSN official star names** | iau.org site terms: **CC BY 4.0** | ✅ |

### 3.3 Verdict on constellation lines

**Licence-clean and adequate: the IAU's own `CC BY 4.0` material.** The recommended, lowest-risk route
is **derive the line segments from the IAU/Alan MacRobert figures ourselves** (the figures are
`CC BY 4.0` images, and the IAU explicitly permits reuse and adaptation) rather than redistributing a
third-party transcription whose own file header and repository footer **disagree about the licence**.
Redistributing `constellation_lines_iau.dat` means relying on a statement that the same repository
contradicts — that is exactly the "looks open, is not settled" case the brief told me to flag rather
than assume. If we *do* use it, the decision must be recorded, the file's own `CC BY 4.0` statement
quoted, and the discrepancy disclosed.

---

## 4. Moon imagery / texture, and planet discs

### 4.1 NASA (public domain — verified, with its limits)

NASA's Scientific Visualization Studio states, measured this session on `svs.gsfc.nasa.gov/help/`:

> *"All of our content is in the public domain (unless otherwise noted), meaning that it is free to
> download, use, and redistribute for whatever purposes you see fit. For more information, see NASA's
> media usage guidelines. Note: some of our visualizations feature licensed music, which is not in the
> public domain."*

The **limits matter for a commercial product** and are on NASA's own media-guidelines page
(`nasa.gov/nasa-brand-center/images-and-media/`, measured):

- *"The NASA Insignia, Logotype, identifiers, and imagery are **not** in the public domain. The use of
  the Insignia, Logotype and NASA identifiers is protected by law."*
- NASA content *"generally are not subject to copyright in the United States… For educational or
  informational purposes"*, and for commercial use: *"If the NASA material is to be used for
  commercial purposes, including advertisements, it must not explicitly or implicitly convey NASA's
  endorsement of commercial goods or services."*
- *"Media including Identifiable Persons … using the media for commercial purposes may infringe that
  person's right of privacy or publicity."*

**Consequence:** the Moon and planet *imagery* is shippable, **but** (a) no NASA logo/insignia, (b) no
wording implying NASA endorses the app, (c) avoid imagery containing identifiable persons.

### 4.2 NASA SVS "CGI Moon Kit" (`svs.gsfc.nasa.gov/4720`) — measured file inventory

The full list is on the page; the rows that matter for a mobile app:

| File | Size (measured) | Note |
|---|---|---|
| `lroc_color_2k.jpg` (2048×1024) | **447.2 KB** | the practical mobile colour map |
| `lroc_color_poles_1k.jpg` (1024×512) | **135.8 KB** | smallest colour map |
| `lroc_color_poles_2k.tif` (2048×1024) | **3.2 MB** | |
| `lroc_color_poles_4k.tif` (4096×2048) | **12.5 MB** | |
| `lroc_color_poles_8k.tif` (8192×4096) | **48.3 MB** | too big to bundle raw; would need resize |
| `lroc_color_poles_16k.tif` | 178.3 MB | not for a phone |
| `lroc_color_poles.tif` (27360×13680) | 494.1 MB | source only |
| `lroc_color_16bit_srgb_4k.tif` | 59.0 MB | |
| `lroc_color_16bit_srgb_8k.tif` | 232.0 MB | |
| `ldem_4.tif` (1440×720) | **4.0 MB** | displacement (for a real 3-D-lit terminator) |
| `ldem_16.tif` (5760×2880) | 63.3 MB | |

**Licence: public domain (NASA SVS statement above).** This is the clean route to a **Moon rendered at
its true phase with real imagery** — a 2K colour map (447 KB) plus the lit hemisphere gives a
phase-accurate disc at trivial APK cost.

### 4.3 NASA SVS "Moon Phase and Libration" + **Dial-A-Moon** API

Also on SVS (pages `5187` / `4851`, both HTTP 200 this session). Provides hourly Moon frames and a
**Dial-A-Moon API** returning the Moon's appearance/phase data per timestamp. **Public domain** under
the same SVS statement. *Suitability:* lets the app show the **real** Moon for any date rather than a
synthetic disc, without shipping imagery at all if the API is used (availability risk, not licence).

### 4.4 Planet discs — **Solar System Scope textures** (`solarsystemscope.com/textures/`)

Measured on the page: **"Distributed under Attribution 4.0 International license: You may use, adapt,
and share these textures for any purpose, even commercially."** → **CC BY 4.0, commercial use
explicitly permitted.** Covers the planet set (Mercury→Neptune) and moons in 2K/8K — i.e. **Jupiter,
Saturn (with rings), Mars, Venus** as real photographic-mosaic discs. *The page did not state a
required credit *string*, only the licence; a concrete string is proposed in §9.*

**Licence-clean alternatives for planet discs:** NASA/JPL mission imagery (public domain, same
insignia/endorsement limits).

### 4.5 Verdict on Moon and planets

**Clean and high quality.** Moon: **NASA SVS** (public domain) — `lroc_color_2k.jpg` 447 KB + the
displacement map if a real terminator is wanted. Planets: **Solar System Scope `CC BY 4.0`**
(commercial explicitly allowed). No share-alike, no non-commercial. The only traps are the NASA
**insignia** and the **no-endorsement** clause.

---

## 5. Photographic Milky Way panorama

**Chosen asset: ESO `eso0932a` — "The Milky Way panorama" (Serge Brunier / Frédéric Tapissier).**
Measured this session on `eso.org/public/images/eso0932a/`:

| Property | Measured value |
|---|---|
| Licence | **CC BY 4.0** — ESO `outreach/copyright` states: *"All images, videos, and music distributed on the public ESO website … are licensed under a Creative Commons Attribution 4.0 International License, and may on a non-exclusive basis be reproduced without fee provided the credit is clear and visible."* |
| Size (resolution) | *"The high resolution image provided here contains 18 million pixels"* → **6000×3000** |
| File sizes (measured) | **Fullsize Original 27.7 MB** · **Large JPEG 7.8 MB** · Publication TIFF 4K 12.9 MB · **Publication JPEG 4.6 MB** · **Screensize JPEG 152.2 KB** · wallpapers 1024×768 … 2048×1536 |
| Required credit | The page states *"Crediting this image with the full credit line, in a visible way is **MANDATORY**, if you want to use it without paying a fee."* The image credit is **`ESO/S. Brunier`** |

**Caveats ESO adds, both relevant to a paid app** (measured on the same page): *"ESO materials, images
and videos may not be used to state or imply the endorsement by ESO or any ESO employee of a
commercial product or service"*, and *"If an image includes a picture of an identifiable person, using
that image for commercial purposes is not permitted."* (Neither is a problem for a starscape.)

**Verdict:** ✅ **shippable in a paid closed-source app** with a **visible** `ESO/S. Brunier` credit.
For a phone, bundle the **Screensize 152 KB** or the **Publication JPEG 4.6 MB**; the 27.7 MB original
is unnecessary and would blow up the APK.

---

## 6. Anything else that makes the scene dramatically better — and is clean

| Asset | Why it helps | Licence | Shippable? |
|---|---|---|---|
| **NASA SVS "Deep Star Maps 2020"** (`svs.gsfc.nasa.gov/4851`, HTTP 200) | A **ready-rendered star-map texture** (8K/16K) that can replace or back-stop plotting thousands of stars — a photographic-looking sky for near-zero drawing work | **Public domain** (SVS statement §4.1) | ✅ |
| **IAU constellation boundary TXT** (part of the `CC BY 4.0` chart pack) | Optional constellation-boundary overlay; adds a "professional" layer | **CC BY 4.0** | ✅ |
| **IAU WGSN star names** | Real proper names (Vega, Betelgeuse…) on tap — the "alive" detail | **CC BY 4.0** | ✅ |
| **Other ESO panoramas** | Alternative/seasonal Milky Way backdrops; same licence and credit rule | **CC BY 4.0** | ✅ |
| **Horizon / atmosphere / light-pollution glow / twilight gradient** | The single biggest "this looks designed" lever | **No licence — render procedurally** (gradient + noise in Compose) | ✅ (own work) |
| **Dial-A-Moon API** | True Moon for any date without shipping per-date imagery | **Public domain** (SVS) | ✅ (availability risk only) |
| **Astronomy Engine (Kotlin/JVM)** — already adjudicated in the sibling research | Ephemeris/rise-set/phase maths, so the app needs no data for positions | **MIT** (© Don Cross) — `…session-planner-market.md` §6.4 | ✅ |

---

## 7. Master asset table

| # | Asset | Source | Exact licence | Shippable in a **paid closed-source** app? | Size | Quality / suitability |
|---|---|---|---|---|---|---|
| 1 | Bright-star positions + proper motion (4,150) | **FK6**, GAVO | **CC0 1.0** | ✅ **yes** (no attribution required) | ~0.2–0.5 MB trimmed | Excellent accuracy; **slice of the sky only** |
| 2 | Faint-star field (positions, J/H/K) | **2MASS** (IRSA) | PD-style, acknowledgement requested | ✅ with acknowledgement | ~0.5–3 MB for a bright cut | Full-sky density; IR→visual mag is a **modelling** step |
| 3 | Star item data (fallback/enrichment) | **Wikidata** | **CC0 1.0** | ✅ | ~10–100 KB | Sparse; not a catalogue |
| 4 | Star catalogue (classic) | **Yale BSC5 / HEASARC `bsc5p`** | none (© Hoffleit/Warren; ADC "free with acknowledgement") | ⚠️ **UNVERIFIED** | ~1 MB | The classical set — **needs an owner decision, not a clean asset** |
| 5 | Star catalogue | **HYG v4.4** | **CC BY-SA 4.0** | ❌ | ~1–2 MB | denylist (share-alike) |
| 6 | Star catalogue | **Hipparcos / Tycho** (ESA) | **CC BY-NC 3.0 IGO** | ❌ | n/a | non-commercial |
| 7 | Star catalogue | **Gaia DR3** (ESA) | **CC BY-NC 3.0 IGO** | ❌ | n/a | non-commercial |
| 8 | Star catalogue | **AAVSO APASS DR10** | all rights reserved; permission required | ❌ | n/a | needs written permission |
| 9 | Deep-sky catalogue | **OpenNGC** | **CC BY-SA 4.0** | ❌ | `NGC.csv` 3,877,591 B | **the problem being solved** |
| 10 | Deep-sky catalogue | **NGC 2000.0** (VizieR VII/118) | **explicitly non-commercial** | ❌ | ~1 MB | out |
| 11 | Deep-sky catalogue | **SAC v8.1** | freeware, no resale | ❌/⚠️ | ~1 MB | out / permission |
| 12 | Deep-sky browse list (subset) | **Wikidata + HEASARC**, own compilation | **CC0 / freely-usable** | ✅ licence; ⚠️ coverage partial | ~20–120 KB | **the recommended replacement route**; **positions present** (RA/Dec), **magnitudes uneven** — audit + a second magnitude source required |
| 13 | Constellation figures | **IAU / Alan MacRobert** charts | **CC BY 4.0** | ✅ | ~30–180 KB derived | the clean answer |
| 14 | Constellation lines file | `dcf21/constellation_lines_iau.dat` | file says **CC BY 4.0**, repo says **GPL v3** | ⚠️ **conflict — do not ship as-is** | ~30 KB | usable only with a recorded decision |
| 15 | Constellation data/code | **d3-celestial** | **BSD-3-Clause** (package) | ✅ code / ⚠️ data provenance | ~1 MB | provenance must be checked |
| 16 | Constellation figures | **Stellarium** | **GPL-2.0+** | ❌ | n/a | never |
| 17 | Moon colour map | **NASA SVS CGI Moon Kit** | **public domain** | ✅ (no insignia; no implied endorsement) | `lroc_color_2k.jpg` **447.2 KB**; 4k/8k TIFF 12.5/48.3 MB | excellent — real LRO imagery |
| 18 | Moon displacement map | **NASA SVS CGI Moon Kit** | **public domain** | ✅ | `ldem_4.tif` **4.0 MB** | enables a real lit terminator |
| 19 | Moon phases / libration + API | **NASA SVS + Dial-A-Moon** | **public domain** | ✅ | 0 (API) or per-frame | true phase for any date |
| 20 | Planet discs (Jupiter, Saturn, Mars, Venus…) | **Solar System Scope** | **CC BY 4.0** — *"even commercially"* | ✅ with attribution | 2K/8K textures | excellent |
| 21 | Milky Way backdrop | **ESO `eso0932a`** (Brunier) | **CC BY 4.0**, visible credit **mandatory** | ✅ | screensize **152.2 KB** / publication JPEG **4.6 MB** / original 27.7 MB; 6000×3000 | excellent full-screen backdrop |
| 22 | Rendered star-map texture | **NASA SVS "Deep Star Maps 2020"** | **public domain** | ✅ | 8K/16K (large; downsample) | strong back-stop for star plotting |
| 23 | Star proper names | **IAU WGSN** | **CC BY 4.0** | ✅ | tiny | nice detail |
| 24 | Horizon / atmosphere / twilight | **own procedural render** | no IP | ✅ | 0 KB | biggest design lever, zero licence risk |

---

## 8. Verdict on OpenNGC — the direct answer

**Does a permissive replacement for OpenNGC exist?**

**No — not as a like-for-like catalogue.** I found **no** complete, permissively-licensed NGC/IC
catalogue. Every full catalogue is `CC BY-SA 4.0` (OpenNGC, HYG), non-commercial (NGC 2000.0,
Hipparcos/Tycho/Gaia, APASS), GPL (Stellarium), or unlicensed (BSC5, SAC).

**Yes — for the subset this product needs.** A curated browse list of **Messier (110) + the few
hundred brightest NGC/IC objects** can be built from **`Wikidata` (CC0)** cross-checked against
**HEASARC** (freely usable) with our own types/descriptions, and that compilation is **clean for a
paid closed-source app**. The cost is an object-level coverage audit and a smaller "browse" claim.

**If OpenNGC is kept anyway,** the accepted obligation is exactly: **attribution + share-alike on the
adapted catalogue** (`CC BY-SA 4.0`), with the open question of whether a bundled data file inside a
closed-source paid app can satisfy share-alike — a **legal** question, not an engineering one. The
recommendation is not to ship it.

---

## 9. Exact attribution strings the app would ship

Copy these verbatim into the app's licences screen and the store listing where required.

| Asset | Required string |
|---|---|
| **ESO `eso0932a`** (mandatory, visible) | `ESO/S. Brunier` — plus the CC BY 4.0 link and a note if adapted. ESO: *"Crediting this image with the full credit line, in a visible way is MANDATORY."* |
| **Solar System Scope textures** | `Planet and Moon textures by Solar System Scope (solarsystemscope.com) — CC BY 4.0` *(the page states the licence but no exact string — this is a **proposed** string, marked INFERRED)* |
| **IAU constellation charts / WGSN names** | `Constellation figures: International Astronomical Union (IAU) and Sky & Telescope — figures by Alan MacRobert — CC BY 4.0`. IAU rules: reproduce the credit **in full**, **never** remove named contributors, and **do not** substitute the IAU logo for the credit. |
| **NASA SVS (Moon, star maps)** | `NASA's Goddard Space Flight Center Scientific Visualization Studio` (for CGI Moon Kit: `NASA / GSFC / Arizona State University`, per the page's own credit). NASA must be acknowledged as the source; **never** use the NASA insignia, and never imply NASA endorsement. |
| **NASA/JPL planetary imagery** | `NASA/JPL-Caltech` (as stated on the specific image). |
| **2MASS** (verbatim requirement) | `This publication makes use of data products from the Two Micron All Sky Survey, which is a joint project of the University of Massachusetts and the Infrared Processing and Analysis Center/California Institute of Technology, funded by the National Aeronautics and Space Administration and the National Science Foundation.` |
| **FK6 (CC0)** | none required. Recommended: `This work made use of the Sixth Fundamental Catalogue (FK6), GAVO/ARI — CC0 1.0`. |
| **Wikidata (CC0)** | none required. Recommended: `Data from Wikidata (CC0)`. |
| **HEASARC tables** | `This research has made use of data and/or software provided by the High Energy Astrophysics Science Archive Research Center (HEASARC), which is a service of the Astrophysics Science Division at NASA/GSFC.` |
| **AAVSO APASS** *(only if permission is obtained)* | `This research was made possible through the use of the AAVSO Photometric All-Sky Survey (APASS), funded by the Robert Martin Ayers Sciences Fund and NSF AST-1412587.` |
| **OpenNGC** *(only if we accept CC BY-SA)* | `Deep-sky data from OpenNGC (CC BY-SA 4.0) — https://github.com/mattiaverga/OpenNGC`, plus the share-alike statement on the derived catalogue. |

---

## 10. Top licence risk

**#1 — a share-alike work entering the paid closed-source bundle through a "harmless" trim.**
`CC BY-SA 4.0` is the factory's denylist precisely because the obligation follows the *derived*
data: OpenNGC (`CC BY-SA 4.0`), HYG (`CC BY-SA 4.0`, now the default "star database" on GitHub), and
**Wikipedia text** (`CC BY-SA 4.0` — note only *Wikidata structured data* is `CC0`) are each **one
copy-paste away** from a build that otherwise reads as licence-clean. The failure is invisible: no
build, test or gate in this factory can see a licence.

**#2 — an artifact whose own two licence statements disagree.** `dcf21/constellation-stick-figures`
declares **`CC BY 4.0`** in the description of `constellation_lines_iau.dat` and **`GPL v3`** in its
repository footer. Shipping it means relying on one statement the same repository contradicts. This is
the second face of the same risk and it is *new to this report*: it is not a denylist match, it is an
**unsettled** licence, which is worse because it looks clean.

**#3 (close behind) — the non-commercial family.** `Hipparcos`/`Tycho`/`Gaia`
(**`CC BY-NC 3.0 IGO`**), **APASS** (all rights reserved), and **NGC 2000.0** (*"should not be used for
commercial purposes"*) are all catalogues a developer downloads by reflex. A single CSV from the wrong
URL turns the app into a violation, and **nothing in the pipeline detects it**.

Mitigation that actually works: **record the licence per asset at the moment of download** (a
provenance file beside the data, as `docs/icon-provenance.md` already does for icons) and keep
`FK6 + 2MASS + Wikidata + IAU + NASA + Solar System Scope + ESO` as the only allowlisted sky sources.

---

## 11. Claim ledger

```text
CLAIM LABEL EVIDENCE

--- The bar ---
CC-BY-SA-* is on the factory denylist for a paid closed-source app | ON RECORD | docs/research/2026-10-02-ui-icon-libraries-and-licensing.md:147 ("ShareAlike — you must distribute your contributions under the same license; incompatible with a paid closed-source app"); docs/icon-provenance.md:174
OpenNGC is CC-BY-SA-4.0 with no LICENSE file | ON RECORD | docs/research/2026-10-08-light-and-sky-session-planner-market.md §6.1 (README line 15; PROBE openngc-LICENSE 404; api.github.com license.spdx_id empty); NGC.csv STATUS=200 BYTES=3877591

--- Stars ---
HYG v4.4 is CC BY-SA 4.0 | MEASURED | WebFetch/curl https://raw.githubusercontent.com/astronexus/HYG-Database/main/LICENSE → 200, 422 bytes, "This work is licensed under a Creative Commons Attribution-ShareAlike 4.0 International License"; same bytes at /master/. GitHub API repo: license.spdx_id = NOASSERTION
Hipparcos and Tycho are CC BY-NC 3.0 IGO | MEASURED | curl https://www.cosmos.esa.int/web/hipparcos/catalogues → 200, 86,684 B: "The Hipparcos and Tycho Catalogues are distributed under the CC BY-NC 3.0 IGO licence and the Terms and Conditions for the use of these data require users to acknowledge ESA using 'Credit: ESA'."
Gaia is CC BY-NC 3.0 IGO | ON RECORD (prior session of this task) | ESA Gaia terms; the settling page (https://www.cosmos.esa.int/web/gaia/data-acknowledgements) did NOT load this session (connection closed unexpectedly) → treat as ON RECORD, re-verify before shipping
AAVSO APASS requires permission for redistribution/commercial use | MEASURED | curl aavso.org policy pages: "free for your own personal use and for use in publications"; "All rights reserved. No part of these data may be reproduced, transmitted, distributed, published … without the express written permission of the AAVSO."
CDS ReadMe for the Bright Star Catalogue (V/50) carries NO copyright notice | MEASURED | curl https://cdsarc.cds.unistra.fr/ftp/V/50/ReadMe → 200, 11,571 B; grep for copyright|licence|license|commercial returned zero matches
HEASARC BSC5P is a 1995 HEASARC creation from an ADC/CDS file; HEASARC "materials are all available freely for your use" | MEASURED + ON RECORD | curl https://heasarc.gsfc.nasa.gov/W3Browse/star-catalog/bsc5p.html → 200, 16,113 B ("created by the HEASARC in 1995 based upon a file obtained from either the ADC or the CDS"); HEASARC data policy via search: https://heasarc.gsfc.nasa.gov/docs/heasarc/data_policy.html "HEASARC materials are all available freely for your use"
Whether BSC5 may be bundled in a paid closed-source app is UNVERIFIED | UNVERIFIED | no affirmative commercial/PD grant found; the deciding source would be a written statement from the BSC5 copyright holders (Hoffleit/Warren, Yale/SAO) or the ADC's current reuse terms at https://cdsarc.cds.unistra.fr/ (not read)
FK6 is CC0 1.0 | MEASURED | curl https://dc.g-vo.org/fk6/q/cone/form → 200, 15,237 B, "Rights: To the extent possible under law, the publisher has waived all copyright and related or neighboring rights to the FK6 data. For details, see the Creative Commons CC0 1.0 Public Domain dedication."
2MASS asks for a specific acknowledgement and states no commercial restriction | MEASURED | curl https://irsa.ipac.caltech.edu/Missions/2mass.html → 200, 6,593 B, acknowledgement text read verbatim (see §9)
An explicit "2MASS is in the public domain" statement | UNVERIFIED | curl https://www.ipac.caltech.edu/2mass/releases/allsky/doc/sec1_6b.html → 200, 97,653 B, but grep for domain|copyright|restrict|permission returned zero matches (page carries no such sentence in its served content); the settling page is the 2MASS Explanatory Supplement §1.6b or a written IRSA statement
Wikidata structured data is CC0 | MEASURED | WebSearch quoting https://www.wikidata.org/wiki/Wikidata:Licensing and …:Data_access: "All structured data … is released into the public domain under Creative Commons Zero"; "We offer the data in Wikidata freely and with no requirement for attribution under CC-0"
Wikidata class ids: "Messier object" = Q14530, "New General Catalogue" = Q14534 | MEASURED | curl https://www.wikidata.org/w/api.php?action=wbsearchentities&format=json&language=en&search=Messier%20object (and "New General Catalogue") → Q14530 "Messier object | astronomical objects catalogued by Charles Messier"; Q14534 "New General Catalogue | astronomical catalogue of deep sky objects". A search for "NGC object" returned no such class.
Wikidata carries ZERO items as `instance of Messier object` | MEASURED | curl "https://query.wikidata.org/sparql?format=json&query=SELECT (COUNT(?i) AS ?n) WHERE { ?i wdt:P31 wd:Q14530 }" → HTTP 200; decoded response `"values"…"bindings":[{"n":{…"value":"0"}}]` → n = 0. So the P31 route is the wrong query for deep-sky objects.
Wikidata's GLOBAL NGC/IC coverage counts | UNVERIFIED (both probes failed) | curl "https://query.wikidata.org/sparql?format=json&query=SELECT (COUNT(DISTINCT ?i) AS ?n) WHERE { ?i wdt:P528 ?c . FILTER(STRSTARTS(?c, \"NGC \")) }" → HTTP 504 Gateway Timeout; the `IC` variant → HTTP 429 Too Many Requests; an earlier attempt (first background probe) returned three consecutive `ERR The operation has timed out` at 60 s each. The settling step is a bounded re-run, not a global count.
Wikidata DOES carry named deep-sky objects resolvable by catalogue code | MEASURED | curl SPARQL with `VALUES ?c { "NGC 7000" "IC 434" "NGC 869" "Messier 42" "M 31" } ?i wdt:P528 ?c` → HTTP 200, 4 bindings: "M 31"→Q2469 (label "Andromeda Galaxy"), "NGC 7000"→Q265515 ("North America Nebula"), "NGC 869"→Q482454 (label fell back to the Q-id — NO English label), "IC 434"→Q2355660 (label "IC 434"). "Messier 42" matched NOTHING — the catalogue code literal is "M 42", so exact code spelling is required.
Wikidata deep-sky items carry RA/Dec as P6257/P6258, NOT P625 | MEASURED | curl https://www.wikidata.org/wiki/Special:EntityData/Q2469.json → P6257 {amount +10.6847083, unit Q28390 = degree}, P6258 {amount +41.2687500, unit Q28390}; Q265515 → P6257 +314.6958, P6258 +44.3300; Q2355660 → P6257 +85.25, P6258 −2.50. The earlier SPARQL asking for P625 returned NO coord binding, consistent with P625 being the wrong property for these items.
Wikidata deep-sky apparent MAGNITUDE (P1215) is uneven and internally contradictory | MEASURED | Q2469 claims.P1215 carries SIX statements with conflicting values (+4.86, +4.36, +3.44, +2.094, +1.283, +0.984); Q265515 has one (+4); Q2355660 has P1215 ABSENT entirely. Total claim counts read from Special:EntityData JSON: Q2469 = 77, Q265515 = 17.

--- Deep sky ---
NGC 2000.0 (VizieR VII/118) is non-commercial | MEASURED | curl https://cdsarc.cds.unistra.fr/ftp/VII/118/ReadMe → 200, 16,769 B: "Copyright Notice: This catalog is copyrighted by Sky Publishing Corporation … for scientific research purposes only. The data should not be used for commercial purposes without the explicit permission of Sky Publishing Corporation."
HEASARC's MESSIER table is compiled from a Sky Publishing book | MEASURED | curl https://heasarc.gsfc.nasa.gov/W3Browse/all/messier.html → 200, 10,175 B: "This version of the Messier Catalog was compiled primarily from Table III of Sky Catalog 2000.0: Volume 2 … Sky Publishing Corporation"
HEASARC materials are freely usable with an acknowledgement | MEASURED | search result/read of https://heasarc.gsfc.nasa.gov/docs/heasarc/data_policy.html: "HEASARC materials are all available freely for your use" + the acknowledgement string in §9
No complete permissive NGC/IC catalogue was found | INFERRED | from every row above: the only full catalogues located are CC-BY-SA (OpenNGC), non-commercial (NGC2000.0), GPL (Stellarium) or permission-only (SAC); no CC0/CC-BY-4.0 full NGC/IC catalogue appeared in any search run this session

--- Constellations ---
IAU constellation charts are CC BY 4.0 | MEASURED | the IAU constellations page text (captured by the search tool this session) reads: "The images are released under the Creative Commons Attribution 4.0 International license."; the IAU Copyright page states images/videos/web texts on iau.org are CC BY 4.0 "and may on a non-exclusive basis be reproduced without fee"
The IAU charts' figures come from Alan MacRobert / Sky & Telescope | MEASURED | same page: "The charts below were produced in collaboration with Sky & Telescope magazine (Roger Sinnott and Rick Fienberg). Alan MacRobert's constellation patterns, drawn in green on the charts…"
dcf21/constellation_lines_iau.dat declares CC BY 4.0 while its repository declares GPL v3 | ON RECORD | the repo README (captured this session): "The file constellation_lines_iau.dat lists the modern IAU stick figures, created by Alan MacRobert et al. They are released under a Creative Commons Attribution 4.0 International license." and "This data is distributed under the Gnu General Public License Version 3. It is © Dominic Ford 2015 - 2025."
d3-celestial's package licence is BSD-3-Clause | MEASURED | curl https://api.github.com/repos/ofrohn/d3-celestial → 200, 5,350 B: "license":{"key":"bsd-3-clause","name":"BSD 3-Clause \"New\" or \"Revised\" License","spdx_id":"BSD-3-Clause"}
Stellarium's default constellation data is GPL | ON RECORD (prior session of this task) | Stellarium licensing; treated as a flag per the brief, not shipped
IAU site material (incl. WGSN star names) is CC BY 4.0 | MEASURED | the IAU Copyright page text as above

--- Moon and planets ---
NASA SVS content is public domain unless noted | MEASURED | curl https://svs.gsfc.nasa.gov/help/ → 200, 70,292 B: "All of our content is in the public domain (unless otherwise noted), meaning that it is free to download, use, and redistribute for whatever purposes you see fit."
The NASA insignia is NOT public domain, and commercial use must not imply endorsement | MEASURED | curl https://www.nasa.gov/nasa-brand-center/images-and-media/ → 200, 317,535 B: "The NASA Insignia, Logotype, identifiers, and imagery are not in the public domain…" and "If the NASA material is to be used for commercial purposes, including advertisements, it must not explicitly or implicitly convey NASA's endorsement…"
NASA CGI Moon Kit file inventory and sizes | MEASURED | curl https://svs.gsfc.nasa.gov/4720 → 200, 54,938 B: lroc_color_2k.jpg (2048x1024) 447.2 KB; lroc_color_poles_1k.jpg 135.8 KB; lroc_color_poles_2k.tif 3.2 MB; lroc_color_poles_4k.tif 12.5 MB; lroc_color_poles_8k.tif 48.3 MB; lroc_color_poles_16k.tif 178.3 MB; lroc_color_poles.tif (27360x13680) 494.1 MB; lroc_color_16bit_srgb_4k.tif 59.0 MB; lroc_color_16bit_srgb_8k.tif 232.0 MB; ldem_4.tif (1440x720) 4.0 MB; ldem_16.tif (5760x2880) 63.3 MB
Solar System Scope textures are CC BY 4.0 and explicitly allow commercial use | MEASURED | curl https://www.solarsystemscope.com/textures/ → 200, 38,687 B: "Distributed under Attribution 4.0 International license: You may use, adapt, and share these textures for any purpose, even commercially."
Solar System Scope's exact required credit string | UNVERIFIED | the page states the licence but no string; the settling page is solarsystemscope.com/textures/ or their contact page — the string in §9 is a PROPOSED one (INFERRED)

--- Milky Way ---
ESO images are CC BY 4.0 with a mandatory visible credit | MEASURED | curl https://www.eso.org/public/outreach/copyright/ → 200, 83,147 B: "All images, videos, and music distributed on the public ESO website … are licensed under a Creative Commons Attribution 4.0 International License, and may on a non-exclusive basis be reproduced without fee provided the credit is clear and visible."
ESO eso0932a credit, resolution and file sizes | MEASURED | curl https://www.eso.org/public/images/eso0932a/ → 200, 82,452 B: "Credit: ESO/S. Brunier"; "The high resolution image provided here contains 18 million pixels"; "Fullsize Original 27.7 MB", "Large JPEG 7.8 MB", "Publication TIFF 4K 12.9 MB", "Publication JPEG 4.6 MB", "Screensize JPEG 152.2 KB"; "Crediting this image with the full credit line, in a visible way is MANDATORY"
ESO material must not imply endorsement | MEASURED | same ESO copyright page: "ESO materials, images and videos may not be used to state or imply the endorsement by ESO or any ESO employee of a commercial product or service"

--- Other clean assets ---
NASA SVS "Deep Star Maps 2020" page exists and is SVS content | MEASURED (existence) | curl https://svs.gsfc.nasa.gov/4851 → 200, 92,419 B; public-domain status is INFERRED from the SVS blanket statement, not from that page
Astronomy Engine (Kotlin/JVM) is MIT | ON RECORD | docs/research/2026-10-08-light-and-sky-session-planner-market.md §6.4 ("MIT (© 2019–2025 Don Cross)"), measured in that file's own session
```

---

## 12. Bottom line

- **Stars:** ship **FK6 (CC0)** + **2MASS (PD-style, acknowledgement)**. Do **not** ship HYG,
  Hipparcos/Tycho/Gaia or APASS. BSC5 is `UNVERIFIED` and needs an owner decision, not a build step.
- **Deep sky:** **no drop-in permissive replacement for OpenNGC exists**; the licence-clean route is a
  curated **Wikidata (CC0) + HEASARC** compilation of Messier + the brightest NGC/IC objects. What the
  probes measured: **positions (RA/Dec, `P6257`/`P6258`) are present** on every object tested, but
  **apparent magnitude (`P1215`) is uneven and contradictory** (M31 has six disagreeing values,
  IC 434 has none), and the **global** NGC/IC counts could not be obtained (504/429) — so an
  object-level audit plus a second magnitude source (`HEASARC`) is **prerequisite work**, not a
  formality. Keeping OpenNGC means accepting `CC BY-SA 4.0` share-alike, with a legal question
  attached — not recommended.
- **Constellations:** **IAU `CC BY 4.0`** charts/figures and boundary files; **never** Stellarium
  (GPL), and treat `constellation_lines_iau.dat` (CC BY 4.0 header vs GPL footer) as **unsettled**.
- **Moon:** **NASA SVS CGI Moon Kit — public domain**, `lroc_color_2k.jpg` is **447 KB**. **Planets:**
  **Solar System Scope CC BY 4.0**, commercial explicitly allowed.
- **Milky Way:** **ESO `eso0932a` — CC BY 4.0**, 6000×3000, visible credit **`ESO/S. Brunier`**
  (mandatory); ship the 152 KB or 4.6 MB version, not the 27.7 MB original.
- **Top risk:** `CC BY-SA` entering through a trimmed catalogue (OpenNGC, HYG, Wikipedia text), with
  an **unsettled** dual-licence file as the runner-up and the **non-commercial** catalogue family as
  the third — none of which any gate in this factory can see.
