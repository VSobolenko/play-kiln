# Store asset contract audit — 2026-09-23

**Scope:** verify that the store assets `scripts/store-kit.py` actually generated for
`apps/2026-09-22-smoke/` satisfy the byte-level contract the factory states in
`AGENTS.md`, and independently check those assets against Play's own asset ranges.

**Verdict in one line: the smoke pack passes every dimensional and size limit, but the
icon violates the factory's own contract in two measured ways — it contains a drawn
rounded mask, and it is not tagged sRGB — and the factory's validator reports PASS on
both. The factory's `AGENTS.md` contract table is *wrong* about the 7-inch and 10-inch
screenshot ranges.**

| | |
|---|---|
| Slot audited | `apps/2026-09-22-smoke/` (read-only; nothing in the repo was modified) |
| Assets measured | 34 files (2 root + 16 × `en-US`/`ru-RU`) |
| Tools used | `python 3.13.7`; a dependency-free `struct`/`zlib` PNG parser; Pillow cross-check; `scripts/store-kit.py --validate-only` |
| Gradle | **not run** (per task constraint) |
| Assets generated during this audit | **none** — every required asset was already on disk |

---

## 1. The contract, quoted from the repo

`AGENTS.md` lines 83–115, `## Store-pack contract`. The required file set:

```text
store/listing.json
store/listing.md
store/privacy.md
store/data-safety.md
store/review-notes.md
store/graphics.md
store/checklist.md
store/assets/icon-512.png
store/assets/feature-graphic-1024x500.png
store/assets/<locale>/icon-512.png
store/assets/<locale>/feature-graphic-1024x500.png
store/assets/<locale>/phone-01.png ... phone-06.png
store/assets/<locale>/tablet-7-01.png ... tablet-7-04.png
store/assets/<locale>/tablet-10-01.png ... tablet-10-04.png
```

The per-item contract table:

| Item | Contract (quoted verbatim from `AGENTS.md`) |
|---|---|
| Listing text | Title ≤ 30, short description ≤ 80, full description ≤ 4000 characters |
| App icon | 512×512, 32-bit PNG with alpha, sRGB, ≤ 1024 KB; no drawn rounded mask or shadow |
| Feature graphic | Exactly 1024×500, JPEG or 24-bit PNG without alpha |
| Phone set | Exactly 6 at 1080×1920; Play permits 2–8, no alpha, ≤ 8 MB each, sides 320–3840, long side ≤ 2× short side |
| 7-inch set | Exactly 4 at 1200×1920; Play permits 4–8, short edge ≥ 1080, long edge ≤ 7680 |
| 10-inch set | Exactly 4 at 1600×2560; Play permits 4–8, short edge ≥ 1080, long edge ≤ 7680 |

And the synthetic-capture rule, lines 113–115:

> Default locale is `en-US`; supported additional locale is `ru-RU`. […] Claims and
> screenshots must describe the shipped app; synthetic captures must be identified as
> synthetic.

The same icon wording is restated in `docs/research/2026-09-22-android-play-requirements.md`
lines 144–145 ("do not draw Google's rounded mask or shadow") and in every slot's
`store/README.md` line 24, so this is a three-place contract, not a typo.

---

## 2. Exact commands run, with exit codes

All commands were run from `d:\Python\PlayKiln`. The parser script was written to
`%TEMP%\kiln-audit\inspect_assets.py` — **outside the repository** — and the throwaway
slot copy in §2.4 was written to `%TEMP%\kiln-audit\slot-smoke`, also outside the repo.

| # | Command | Exit | Purpose |
|---|---|---:|---|
| 2.1 | `python scripts/store-kit.py --help` | 0 | Read the generator's surface before using it |
| 2.2 | `python %TEMP%\kiln-audit\inspect_assets.py apps\2026-09-22-smoke\store\assets` | 0 | Measure every PNG's bytes (IHDR + full chunk list + decoded alpha) |
| 2.3 | `python scripts/store-kit.py --slot smoke --validate-only` | **1** | Run the factory's own gate against the real slot |
| 2.4 | *(temp copy, outside repo)* `Copy-Item` slot's `app/build.gradle.kts` + `store/`; `[System.IO.File]::WriteAllText("$tmp\capabilities.json", '{"schemaVersion": 1, "enabled": []}')` (no BOM); then `python scripts/store-kit.py --slot-dir $tmp --validate-only` | 0 | Run the factory's own gate on an identical asset tree so the asset checks are actually reached |
| 2.5 | `python -c` Pillow cross-check of icon/feature/phone (`getbbox`, `getextrema`, `histogram`, corner pixels, `info`) | 0 | Independent second measurement, different library |
| 2.6 | `python -c` `len()` over `store/listing.json` strings + `Get-FileHash -Algorithm SHA256` over all 34 assets | 0 | Listing limits and duplicate detection |

I did **not** run Gradle, I did **not** create a slot, I did **not** modify
`scripts/store-kit.py`, and I did **not** regenerate any asset.

### 2.3 — the factory's own gate fails on the real slot

```
$ python scripts/store-kit.py --slot smoke --validate-only
FAIL: missing apps\2026-09-22-smoke\capabilities.json; declare enabled capabilities (use an empty array when none are enabled)
EXIT:1
```

This is a real gap, not a typo. In `--validate-only` mode the script is forbidden from
creating the file:

```1112:1115:scripts/store-kit.py
    capability_config = slot / "capabilities.json"
    if not args.validate_only and not capability_config.is_file():
        write_json(capability_config, {"schemaVersion": 1, "enabled": []})
    compliance = capability_compliance(slot)
```

`capability_compliance()` shells out to `scripts/lib/capabilities.mjs`, which raises when
the file is absent (`scripts/lib/capabilities.mjs:109-112`). That call happens **before**
`validate_store()`:

```1149:1153:scripts/store-kit.py
    if args.validate_only:
        ok = validate_store(store, listing, locales)
        if ok:
            print(summary(slot, locales))
            return 0
        return 1
```

So on the real slot the validator aborts and **no asset is inspected at all**. Both
existing slots are affected: neither `apps/2026-09-22-smoke/capabilities.json` nor
`apps/2026-09-23-pipe-proof/capabilities.json` exists. The first item on
`store/checklist.md` ("Run `python scripts/store-kit.py --slot smoke --validate-only` and
resolve every FAIL") therefore cannot pass as written, and the failure message says
nothing about assets — an operator reads "capabilities" and never learns the icon was
never checked.

**Fix (described, not applied — the scripts tree is owned by another worker):** make
`--validate-only` report all failures instead of aborting on the first one, and treat a
missing `capabilities.json` during validation as a *recorded* failure that does not
prevent the asset checks from running (i.e. collect it into `failures` rather than
raising `StoreKitError` before `validate_store`). Alternatively create the empty
`capabilities.json` in `new-slot.mjs` so every slot has one from birth.

### 2.4 — the factory's own gate, once it actually runs

Against a byte-identical temp copy (byte counts match my parser exactly, which
cross-validates both tools):

```
PASS listing.json — schema=1 and Play character limits satisfied
PASS listing.md — 2513 bytes
PASS privacy.md — 904 bytes
PASS data-safety.md — 622 bytes
PASS review-notes.md — 1059 bytes
PASS graphics.md — 1020 bytes
PASS checklist.md — 1257 bytes
PASS assets/icon-512.png — 512x512 RGBA, 5413 bytes
PASS assets/feature-graphic-1024x500.png — 1024x500 RGB, 24471 bytes
PASS assets/en-US/icon-512.png — 512x512 RGBA, 5413 bytes
PASS assets/en-US/feature-graphic-1024x500.png — 1024x500 RGB, 24471 bytes
PASS assets/en-US/phone-01.png — 1080x1920 RGB, 84202 bytes
PASS assets/en-US/phone-02.png — 1080x1920 RGB, 83061 bytes
PASS assets/en-US/phone-03.png — 1080x1920 RGB, 84222 bytes
PASS assets/en-US/phone-04.png — 1080x1920 RGB, 85267 bytes
PASS assets/en-US/phone-05.png — 1080x1920 RGB, 83408 bytes
PASS assets/en-US/phone-06.png — 1080x1920 RGB, 77181 bytes
PASS assets/en-US/tablet-7-01.png — 1200x1920 RGB, 97247 bytes
PASS assets/en-US/tablet-7-02.png — 1200x1920 RGB, 94286 bytes
PASS assets/en-US/tablet-7-03.png — 1200x1920 RGB, 98114 bytes
PASS assets/en-US/tablet-7-04.png — 1200x1920 RGB, 98846 bytes
PASS assets/en-US/tablet-10-01.png — 1600x2560 RGB, 148196 bytes
PASS assets/en-US/tablet-10-02.png — 1600x2560 RGB, 147342 bytes
PASS assets/en-US/tablet-10-03.png — 1600x2560 RGB, 149057 bytes
PASS assets/en-US/tablet-10-04.png — 1600x2560 RGB, 153721 bytes
PASS assets/ru-RU/icon-512.png — 512x512 RGBA, 5413 bytes
PASS assets/ru-RU/feature-graphic-1024x500.png — 1024x500 RGB, 25064 bytes
PASS assets/ru-RU/phone-01.png — 1080x1920 RGB, 82611 bytes
PASS assets/ru-RU/phone-02.png — 1080x1920 RGB, 86479 bytes
PASS assets/ru-RU/phone-03.png — 1080x1920 RGB, 81426 bytes
PASS assets/ru-RU/phone-04.png — 1080x1920 RGB, 81378 bytes
PASS assets/ru-RU/phone-05.png — 1080x1920 RGB, 86219 bytes
PASS assets/ru-RU/phone-06.png — 1080x1920 RGB, 83055 bytes
PASS assets/ru-RU/tablet-7-01.png — 1200x1920 RGB, 96040 bytes
PASS assets/ru-RU/tablet-7-02.png — 1200x1920 RGB, 101130 bytes
PASS assets/ru-RU/tablet-7-03.png — 1200x1920 RGB, 94390 bytes
PASS assets/ru-RU/tablet-7-04.png — 1200x1920 RGB, 93555 bytes
PASS assets/ru-RU/tablet-10-01.png — 1600x2560 RGB, 140304 bytes
PASS assets/ru-RU/tablet-10-02.png — 1600x2560 RGB, 154078 bytes
PASS assets/ru-RU/tablet-10-03.png — 1600x2560 RGB, 143818 bytes
PASS assets/ru-RU/tablet-10-04.png — 1600x2560 RGB, 143062 bytes
STORE-KIT: OK apps/slot-smoke/store (icon=512x512 feature=1024x500 phone=6 tablet7=4 tablet10=4 locales=en-US,ru-RU)
EXIT:0
```

**The gate is green.** Sections 4–7 show what it does not look at.

---

## 3. Measured bytes — every PNG, parsed not inferred

`w×h` from IHDR; `colour type` is the PNG IHDR colour-type field (with its name);
`bpp` = bit depth × samples per pixel; `alpha ch.` = the pixel data carries an alpha
sample (types 4/6) or the palette declares `tRNS`. **No value below came from a filename
or from reading the generator's source.**

### Root assets (`store/assets/`)

| File | w×h | Colour type | bpp | alpha ch. | sRGB tag | Bytes |
|---|---|---:|---:|---|---|---:|
| `icon-512.png` | 512×512 | 6 (truecolor+alpha) | 32 | **yes** | **no** | 5 413 |
| `feature-graphic-1024x500.png` | 1024×500 | 2 (truecolor) | 24 | no | no | 24 471 |

### `en-US`

| File | w×h | Colour type | bpp | alpha ch. | Bytes |
|---|---|---:|---:|---|---:|
| `icon-512.png` | 512×512 | 6 (RGBA) | 32 | yes | 5 413 |
| `feature-graphic-1024x500.png` | 1024×500 | 2 (RGB) | 24 | no | 24 471 |
| `phone-01.png` | 1080×1920 | 2 (RGB) | 24 | no | 84 202 |
| `phone-02.png` | 1080×1920 | 2 (RGB) | 24 | no | 83 061 |
| `phone-03.png` | 1080×1920 | 2 (RGB) | 24 | no | 84 222 |
| `phone-04.png` | 1080×1920 | 2 (RGB) | 24 | no | 85 267 |
| `phone-05.png` | 1080×1920 | 2 (RGB) | 24 | no | 83 408 |
| `phone-06.png` | 1080×1920 | 2 (RGB) | 24 | no | 77 181 |
| `tablet-7-01.png` | 1200×1920 | 2 (RGB) | 24 | no | 97 247 |
| `tablet-7-02.png` | 1200×1920 | 2 (RGB) | 24 | no | 94 286 |
| `tablet-7-03.png` | 1200×1920 | 2 (RGB) | 24 | no | 98 114 |
| `tablet-7-04.png` | 1200×1920 | 2 (RGB) | 24 | no | 98 846 |
| `tablet-10-01.png` | 1600×2560 | 2 (RGB) | 24 | no | 148 196 |
| `tablet-10-02.png` | 1600×2560 | 2 (RGB) | 24 | no | 147 342 |
| `tablet-10-03.png` | 1600×2560 | 2 (RGB) | 24 | no | 149 057 |
| `tablet-10-04.png` | 1600×2560 | 2 (RGB) | 24 | no | 153 721 |

### `ru-RU`

| File | w×h | Colour type | bpp | alpha ch. | Bytes |
|---|---|---:|---:|---|---:|
| `icon-512.png` | 512×512 | 6 (RGBA) | 32 | yes | 5 413 |
| `feature-graphic-1024x500.png` | 1024×500 | 2 (RGB) | 24 | no | 25 064 |
| `phone-01.png` | 1080×1920 | 2 (RGB) | 24 | no | 82 611 |
| `phone-02.png` | 1080×1920 | 2 (RGB) | 24 | no | 86 479 |
| `phone-03.png` | 1080×1920 | 2 (RGB) | 24 | no | 81 426 |
| `phone-04.png` | 1080×1920 | 2 (RGB) | 24 | no | 81 378 |
| `phone-05.png` | 1080×1920 | 2 (RGB) | 24 | no | 86 219 |
| `phone-06.png` | 1080×1920 | 2 (RGB) | 24 | no | 83 055 |
| `tablet-7-01.png` | 1200×1920 | 2 (RGB) | 24 | no | 96 040 |
| `tablet-7-02.png` | 1200×1920 | 2 (RGB) | 24 | no | 101 130 |
| `tablet-7-03.png` | 1200×1920 | 2 (RGB) | 24 | no | 94 390 |
| `tablet-7-04.png` | 1200×1920 | 2 (RGB) | 24 | no | 93 555 |
| `tablet-10-01.png` | 1600×2560 | 2 (RGB) | 24 | no | 140 304 |
| `tablet-10-02.png` | 1600×2560 | 2 (RGB) | 24 | no | 154 078 |
| `tablet-10-03.png` | 1600×2560 | 2 (RGB) | 24 | no | 143 818 |
| `tablet-10-04.png` | 1600×2560 | 2 (RGB) | 24 | no | 143 062 |

**Chunk inventory:** every one of the 34 files contains exactly `IHDR`, one or more
`IDAT`, `IEND` — and nothing else. There is **no `sRGB`, no `iCCP`, and no `gAMA`
chunk in any file**, and Pillow reports an empty `info` dict for all three files it
opened (`info keys []`). No file carries an embedded colour profile.

**Provenance — the measured bytes are committed, not local:** `git ls-files
apps/2026-09-22-smoke` returns **89 tracked files, 34 of which are store assets**, and
`git check-ignore -v apps/2026-09-22-smoke/store/assets/icon-512.png` exits **1** (not
ignored; no `.gitignore` pattern matches `store`, `assets` or `png`). So every asset
measured here is reproducible from the repository, and the icon violations in §5 are a
**committed** defect rather than a stale local artefact. This also means a fresh clone
contains the exact bytes audited below.

**Duplicate detection (SHA-256, first 16 hex digits):** the root `icon-512.png`,
`en-US/icon-512.png` and `ru-RU/icon-512.png` are the *same file*
(`A617EDABD723B435`) — expected, since `store-kit.py` copies it
(`shutil.copyfile(icon, locale_dir / icon.name)` at line 1011). The root
`feature-graphic-1024x500.png` is byte-identical to the `en-US` one
(`D8087E38DB32E5C2`) but **differs** from the `ru-RU` one (`9629E00392F329E8`), which is
correct because the feature graphic carries localized title/tagline text. All 24
screenshots are distinct.

---

## 4. Measured vs contract — pass/fail

| Contract item | Required | Measured | Verdict |
|---|---|---|---|
| App icon — dimensions | 512×512 | 512×512 (both locales + root) | **PASS** |
| App icon — bit depth | 32-bit PNG | colour type 6, bit depth 8 = **32 bpp** | **PASS** |
| App icon — alpha | with alpha | alpha channel present; 41 683 px at α=0, 20 925 px partial, α range 0–255 | **PASS** |
| App icon — size | ≤ 1024 KB | 5 413 B (0.52 % of cap) | **PASS** |
| App icon — sRGB | sRGB | **no `sRGB`/`iCCP`/`gAMA` chunk; no colour profile** | **FAIL** |
| App icon — no drawn rounded mask or shadow | no drawn mask | **rounded mask baked into alpha**, bbox `(16, 16, 497, 497)` with ~112 px corner radius (see §5) | **FAIL** |
| Feature graphic — dimensions | exactly 1024×500 | 1024×500 (root, `en-US`, `ru-RU`) | **PASS** |
| Feature graphic — JPEG or 24-bit PNG without alpha | no alpha | colour type 2, 24 bpp, `tRNS` absent, no alpha channel | **PASS** |
| Phone set — count | exactly 6 | 6 per locale | **PASS** |
| Phone set — dimensions | 1080×1920 | 1080×1920 × 12 files | **PASS** |
| Phone set — no alpha | no alpha | colour type 2, `tRNS` absent | **PASS** |
| Phone set — size | ≤ 8 MB each | max 85 267 B (0.081 MB) | **PASS** |
| 7-inch set — count | exactly 4 | 4 per locale | **PASS** |
| 7-inch set — dimensions | 1200×1920 | 1200×1920 × 8 files | **PASS** |
| 10-inch set — count | exactly 4 | 4 per locale | **PASS** |
| 10-inch set — dimensions | 1600×2560 | 1600×2560 × 8 files | **PASS** |
| Per-locale sets for `en-US` and `ru-RU` | both complete | 16/16 files in each locale, all 8 root/locale docs present | **PASS** |
| Screenshot format | JPEG or 24-bit PNG without alpha | all 32 are 24-bit PNG, no alpha | **PASS** |

**Two failures, both on the icon.** Both are byte-level facts, and both are reported
**PASS** by the factory's own validator (§2.4).

---

## 5. The icon's drawn rounded mask — measured

The alpha channel is not a design artefact of the glyph; it is a mask applied to the
whole 512×512 canvas. Two independent parsers agree:

* my `zlib` decoder: `alpha bbox (non-zero region)` → `(16, 16, 497, 497)`; every
  pixel at `x=0` or `y=0` has α=0; the first fully opaque column is 16 and the last is
  496 for all rows from y=128 to y=384; the corner block (0..3 × 0..3) is entirely α=0;
* Pillow: `alpha bbox (16, 16, 497, 497)`, `alpha extrema (0, 255)`,
  `alpha==0 pixels 41683`, `alpha==255 pixels 199536`, `partial-alpha pixels 20925`,
  corners `(0,0,0,0)`.

A 32×32 alpha downsample of the file shows the mask boundary as a rounded rectangle
(`.` = opaque, `+` = partial, blank = transparent):

```
|                                |
|        .................       |
|     .......................    |
|    .........................   |
|   ...........................  |
|  ............................. |
|  ............................. |
| ...............................|
| ...............................|
| ............+++.+++............|
| ...........+.......+...........|
| ..........+.........+..........|
| .........+..+++++++..+.........|      <- the "focus" glyph, inside the mask
| .........+..+++++++..+.........|         (also partial alpha, not the issue)
| .........+..+++++++..+.........|
| ............+++++++............|
| .........+..+++++++..+.........|
| .........+..+++++++..+.........|      <- mask boundary continues to the edges
| .........+..+++++++..+.........|
| ..........+.........+..........|
| ...........+.......+...........|
| ............+++.+++............|
| ...............................|
|  ............................. |
|  ............................. |
|   ...........................  |
|    .........................   |
|     .......................    |
|        .................       |
|                                |
```

The geometry matches the generator exactly:

```559:562:scripts/store-kit.py
    mask = Image.new("L", base.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((16, 16, 496, 496), radius=112, fill=255)
    canvas = Image.new("RGBA", base.size, (0, 0, 0, 0))
    canvas.paste(base, (0, 0), mask)
```

a 16 px inset and a 112 px corner radius — precisely the bbox Pillow and my decoder both
measured. The contract says *"no drawn rounded mask or shadow"*, and the repo's own
requirements doc interprets that as *"do not draw Google's rounded mask or shadow"*
(`docs/research/2026-09-22-android-play-requirements.md:144-145`). This icon draws its own
rounded mask. That is the prohibited thing, and 15.9 % of the canvas (41 683 px) is
transparent because of it.

**Why the gate misses it:** the icon's only checks are

```1065:1068:scripts/store-kit.py
    specs: list[tuple[Path, tuple[int, int], str, bool]] = [
        (store / "assets" / "icon-512.png", (512, 512), "RGBA", False),
        (store / "assets" / "feature-graphic-1024x500.png", (1024, 500), "RGB", False),
    ]
```

and `inspect_image()` tests size, PIL mode, format, size-in-bytes, and — for screenshots —
the Play side/aspect rule. `RGBA` mode *requires* alpha, so an icon that is entirely
transparent would pass. There is no mask check and no colour-profile check anywhere in
`store-kit.py` (`grep` for `sRGB|srgb|iCCP` over the file returns nothing).

**Fix (described, not applied):** in `render_icon()`, drop the mask and paste the
gradient full-bleed (or make the mask a square `(0, 0, 512, 512)`), keeping the glyph's
own transparency; then add two assertions to `inspect_image()` — for `icon-512.png`,
(a) all four corner pixels must be `(r, g, b, 255)`, and (b) the file must contain an
`sRGB` chunk (`image.info.get("srgb") is not None`, or scan the chunk list). Both are
cheap and both would currently fail on this asset, which is the point.

---

## 6. Independent legality check against Play's ranges

The factory's table is not the authority for what Play rejects, so I applied the Play
range stated in the task — **each side 320–3840 px, long side ≤ 2× short side** — to the
measured numbers:

| Set | n | Sides | Min side | Max side | Aspect (long/short) | 320–3840? | ≤ 2×? | Would Play reject? |
|---|---:|---|---:|---:|---:|---|---|---|
| phone | 6 per locale | 1080×1920 | 1080 | 1920 | 1.778 | yes | yes | **no** |
| 7-inch | 4 per locale | 1200×1920 | 1200 | 1920 | 1.600 | yes | yes | **no** |
| 10-inch | 4 per locale | 1600×2560 | 1600 | 2560 | 1.600 | yes | yes | **no** |

**No screenshot set would be rejected by Play on dimensions or count.** Every count
(6/4/4) sits inside Play's permitted 2–8 (phone) and 4–8 (tablet) ranges, and no file
exceeds 0.15 MB against the 8 MB cap. This is the honest green part of the report: the
generator's raw screenshot output is genuinely Play-legal.

### But the factory's contract table is wrong

`AGENTS.md:110-111` says the 7-inch and 10-inch sets may have a **long edge ≤ 7680**.
Play's cap is **3840** — 7680 is exactly **2× the real limit**. A 1200×7680 screenshot
would satisfy `AGENTS.md` and be rejected by Play. It would also be rejected by the
factory's own script, which enforces the correct rule:

```1040:1043:scripts/store-kit.py
            if screenshot_rules:
                short, long = sorted(image.size)
                if short < 320 or long > 3840 or long > 2 * short:
                    failures.append("violates Play screenshot side/aspect limits")
```

So `AGENTS.md` **contradicts the factory's own validator**: the documented contract
permits assets the code rejects. The table is also misleading in the other direction —
`short edge ≥ 1080` is the *large-format eligibility* bar, not a screenshot-validity
bar. `AGENTS.md:109` gets the phone row right (it quotes 320–3840 and the 2× rule
verbatim) and omits both from the two tablet rows.

The same 7680 error is duplicated in
`docs/research/2026-09-22-android-play-requirements.md:151-152`, so fixing only
`AGENTS.md` would leave the error in place. Note also that the requirements doc marks
this whole section `NEEDS-VERIFICATION` with no official Play URL, which is likely how
the error survived.

**Fix (described, not applied — both files are owned by other workers):** change the
7-inch and 10-inch rows to *"Play permits 4–8, sides 320–3840, long side ≤ 2× short side;
at least 1080 px on the short side is required to count toward large-format
eligibility"*, in `AGENTS.md` and in the requirements doc. Better still, have
`store-kit.py` print the ranges it enforces so the prose and the code cannot drift apart
again.

### One more contract/validator mismatch

The contract says a feature graphic or screenshot may be *"JPEG or 24-bit PNG"*, but the
validator hard-requires PNG:

```1038:1039:scripts/store-kit.py
            if image.format != "PNG":
                failures.append(f"format {image.format}, expected PNG")
```

A perfectly legal JPEG would be reported `FAIL`. This is the validator being *stricter*
than the contract (harmless for the current all-PNG output), but it means the gate cannot
be used to certify a JPEG pack, and the word "JPEG" in the contract is currently
unenforceable. Worth aligning explicitly one way or the other.

---

## 7. Listing text limits — measured by string length

Measured with `len()` over the JSON, per locale:

| Field | Limit | `en-US` | `ru-RU` | Verdict |
|---|---:|---:|---:|---|
| `title` | 30 | **14** | **14** | PASS |
| `shortDescription` | 80 | **80** | **69** | PASS (at the cap) |
| `fullDescription` | 4000 | **397** | **367** | PASS |

Six `screenshotCaptions` per locale, lengths 21–27 chars each. No field exceeds its
limit, so nothing is rejected on length.

Two content findings that the length check cannot see:

1. **The `en-US` short description is truncated mid-word.** It is exactly 80/80
   characters and ends `...lives at the repo roo` + `…` — the word "root" was cut. It
   comes from `truncate_chars(meta["idea"], 80)`, which slices at a fixed byte offset:

   ```222:227:scripts/store-kit.py
   def truncate_chars(text: str, limit: int) -> str:
       text = re.sub(r"\s+", " ", text).strip()
       if len(text) <= limit:
           return text
       clipped = text[: max(1, limit - 1)].rstrip(" ,.;:-")
       return clipped + "…"
   ```

   `text[:79]` truncated "root" to "roo"; `rstrip(" ,.;:-")` cannot repair a cut word.
   The result is at the cap with no headroom, so any longer idea string yields a visibly
   broken store listing. **Fix:** truncate on a word boundary (cut at the last space
   before the limit) and assert the result is ≤ the limit.

2. **Factory scaffolding text is shipping in the listing.** The `en-US`
   `fullDescription` contains the literal sentence *"PlayKiln slot — isolated Android
   attempt. Factory harness lives at the repo root."* and the `ru-RU` description embeds
   the same English sentence verbatim. That is the slot's `idea` field, i.e. internal
   factory language, not user-facing marketing copy — and a Russian listing containing an
   untranslated English sentence about "the repo root" is a real store-quality problem.
   Both descriptions are far under the 4000-char cap, so no limit check will ever flag
   it. This is a "claims must describe the shipped app" issue (`AGENTS.md:114-115`).

---

## 8. Synthetic vs real — and is that disclosed?

**All 34 assets are synthetic.** Evidence, all from files on disk:

* there is **no** `apps/2026-09-22-smoke/store/captures/` directory anywhere in the slot,
  so `render_screenshots()` (`scripts/store-kit.py:678-679`) found no raw captures and
  fell through to `synthetic_screen()` for all six sources:

  ```687:688:scripts/store-kit.py
              raw = load_raw_capture(captures, index)
              sources.append(raw or synthetic_screen((720, 1280), index, captions[index - 1], listing))
  ```

* `render_screenshots()` returns `not has_all_raw`, and the generated documents say so in
  their own words. The disclosure strings are produced from that flag
  (`scripts/store-kit.py:851`, `:858`, `:880`), and the files on disk contain them:

| File | Line on disk |
|---|---|
| `store/graphics.md` | `Screenshot source status: **synthetic — replace with real captures before publish**.` |
| `store/review-notes.md` | `- TODO-HUMAN: Replace synthetic screenshots with real captures and regenerate the pack.` |
| `store/checklist.md` | `- [ ] Replace synthetic screenshots with real captures when \`graphics.md\` marks them synthetic.` |

So the synthetic-capture disclosure **required by `AGENTS.md` is present and truthful in
three documents** — this part of the contract is satisfied. What is *not* disclosed is
that the icon and feature graphic are also synthetic renders (gradient + drawn glyph),
and `graphics.md` describes the icon as *"RGBA Play store icon; transparent corners"* —
it documents the rounded mask as a feature rather than flagging it against the
"no drawn rounded mask" rule. `graphics.md` also labels the screenshots `RGB` and the
feature graphic 1024×500, both of which my measurements confirm.

The screenshots are synthetic *mock-ups of an app journey* (`synthetic_screen()` draws a
header bar, a caption card, a "Step N: a preview of the described journey." body line and
a coloured circle). They do not depict the real app UI. They must be replaced before
publish, and the pack's own docs say so.

---

## 9. Gaps between what the factory claims and what the bytes show

Ordered by severity.

| # | Claim | Bytes | Severity |
|---|---|---|---|
| 1 | `AGENTS.md:107` icon has **no drawn rounded mask** | Alpha mask at `(16,16,497,497)`, radius ≈112 — a drawn rounded mask; 41 683 transparent px. **Committed to git**, so it ships as-is | **High** — the asset itself violates the contract and is not fixed by any check |
| 2 | `AGENTS.md:110-111` tablets may be up to **7680** on the long edge | Play's cap is 3840; the factory's own script enforces 3840 | **High** — documented contract is 2× too permissive and contradicts the code |
| 3 | `AGENTS.md:107` icon is **sRGB** | No `sRGB`, `iCCP` or `gAMA` chunk in any of the 34 files; Pillow `info` empty | **Medium** — undeclared colour space; claim is unverifiable from the bytes |
| 4 | `store/checklist.md` item 1 — run `--validate-only` and resolve every FAIL | The command exits 1 on this slot before inspecting any asset (missing `capabilities.json`) | **Medium** — the factory's last gate is currently inoperative on both slots |
| 5 | Contract: feature graphic / screenshots may be **JPEG or 24-bit PNG** | Validator hard-fails any non-PNG | **Low** — validator stricter than contract; "JPEG" is unenforceable |
| 6 | `listing.md` presents the short description as store copy | `en-US` short description is 80/80 and truncated mid-word ("repo roo…") | **Low** — passes the limit, visibly broken copy |
| 7 | `AGENTS.md:114-115` claims must describe the shipped app | `fullDescription` contains factory scaffolding ("Factory harness lives at the repo root.") in *both* locales | **Low** — content quality, no automated check would catch it |

Everything else in the contract table — feature graphic dimensions and alpha, all three
screenshot sets' counts, dimensions, alpha absence and byte sizes, all listing character
limits, and the synthetic-capture disclosure — **matches the bytes exactly**.

---

## 10. Would Play reject this pack?

**On the dimensions, counts, aspect ratios, sizes and character limits: no.** Every set
is inside Play's ranges with wide margins, and I could not find a single asset that Play
would reject for a measurable geometric or size reason.

**On the icon: likely, and unverifiable from the repo.** The icon carries a baked-in
rounded mask with fully transparent corners, which is the specific thing the factory's
contract and the repo's requirements doc tell it not to do. Whether Play's upload
pipeline rejects or silently accepts it, I cannot confirm from the repository — every
source I could reach for the icon rule is either `AGENTS.md` itself or a research doc
that marks the section `NEEDS-VERIFICATION` with no official Play URL. I am not going to
convert that into a confident rejection claim. What is certain is the contract violation:
**the icon does not match the contract the factory published, and the factory's own gate
reports PASS anyway.**

---

## 11. What I could not verify

* **Play's actual enforcement** of "no drawn rounded mask" and the sRGB requirement. The
  repo's only sources are `AGENTS.md`, the slot `store/README.md` templates, and
  `docs/research/2026-09-22-android-play-requirements.md`, which marks the entire
  listing-asset section `NEEDS-VERIFICATION` and cites no official Play URL. I did not
  use web access, so the 320–3840 / 2× / 7680 analysis rests on the range the task
  supplied plus the rule `store-kit.py` itself enforces.
* **Whether the icon visually contains a shadow.** The task asks about shadow as well as
  mask. I measured the alpha mask (definitive), but a shadow could be drawn *inside* the
  opaque region and would be invisible to a chunk/alpha parser. I did not perform visual
  analysis of the pixel content, so I make no claim about shadows.
* **The rendered content of the screenshots.** I measured their geometry and format but
  did not assess whether the mock-up depicts the app truthfully; they are synthetic
  placeholders, and the pack documents say they must be replaced.
* **Other slots.** The task named the smoke slot. I confirmed only that
  `apps/2026-09-23-pipe-proof/` also lacks `capabilities.json`; I did not measure its
  assets, and I did not look for store assets anywhere else in the repo.
* **Whether `--validate-only` behaves the same on a slot that has `capabilities.json`.**
  No such slot exists in the repo right now, so the green run in §2.4 came from a temp
  copy with the file added by hand.
* **Android/Gradle-level evidence.** Not run, by instruction, so nothing here speaks to
  whether the app builds or whether the signed AAB matches these assets.

---

## 12. Reproducing this audit

The measurement script is dependency-free (`struct` + `zlib`) and lives outside the repo
at `%TEMP%\kiln-audit\inspect_assets.py`. Re-run with:

```bash
python "$TEMP\kiln-audit\inspect_assets.py" "apps\2026-09-22-smoke\store\assets"
```

The temp slot copy used for §2.4 is at `%TEMP%\kiln-audit\slot-smoke` and can be rebuilt
by copying the slot's `app/build.gradle.kts` and `store/` into a fresh directory and
writing `capabilities.json` as UTF-8 **without a BOM** — note that
`Set-Content -Encoding utf8` on PowerShell 5.1 emits a BOM, which
`capabilities.mjs` then rejects with *"is not valid JSON"*; use
`[System.IO.File]::WriteAllText(...)` instead.

No file in the repository was created, modified or deleted by this audit except this
report.
