# Icon provenance and licence contract

The owner's instruction, recorded 2026-10-02: hand-drawing UI glyphs is amateurish, so an agent
goes online, downloads a ready-made, well-chosen, licence-cleared icon set, and pastes it into the
app. This document is the **contract** that makes that instruction safe and auditable. It is the
binding specification behind `.cursor/rules/visual-identity.mdc`; it is not a tutorial, and it
replaces no existing rule — `workspace-hygiene.mdc` still governs where temporary files go, and
`localization.mdc` still governs user-facing strings.

The evidence layer is `docs/research/2026-10-02-ui-icon-libraries-and-licensing.md`. Every licence
claim below is derived from it, and the subset this pass re-read from the primary licence texts is
recorded in §"Verification status" at the end. Where a licence was **not** read in this pass, this
document says so rather than asserting it.

**The one-line split that is the whole point.**

- **UI iconography** (a back arrow, a plus, a bin, a settings gear, an empty-state symbol) comes
  from a **licence-cleared professional set**, vendored into the slot, with provenance. This is the
  new requirement.
- **The app's mark** (the launcher icon, the adaptive-icon foreground, the monochrome/themed layer,
  the in-app brand glyph) is **slot-owned and derived from `store/studio.json`'s `mark`**. It is
  never a library icon. This half of the old rule is unchanged, and it is not a taste preference —
  see rule 10.

The test: if the icon would be the same in a competitor's app, it is UI iconography and belongs to
the library; if it would be the same in no other app, it is identity and belongs to the slot.
(**ON RECORD**, research §6.)

Labels are the factory's four: **MEASURED** (read in the source or a run), **ON RECORD** (stated by
a named source), **INFERRED** (this document's reasoning), **UNVERIFIED** (claimed but not
established).

---

## 1. The record: `apps/<slot>/icons/provenance.json`

The record lives at a fixed path so a future check can find it without guessing:

```text
apps/<slot>/icons/
  provenance.json          # the machine-readable record (schema in §1.2)
  LICENSES/
    <SPDX>.txt             # verbatim licence text of every set used, one file per distinct spdx
```

**Rule 1 — the path is fixed.** `apps/<slot>/icons/provenance.json` and
`apps/<slot>/icons/LICENSES/<SPDX>.txt`. It mirrors the factory's existing `store/provenance.json`
(a committed record binding committed bytes to a source) and the `economics.json` /
`differentiators.json` / `naming.json` convention that a decision is a build input with a schema.
(**ON RECORD**, research §5.1 and factory convention; the exact shape is a recommendation in the
research, adopted here as the contract.)

### 1.2 Schema

```json
{
  "schemaVersion": 1,
  "sets": [
    {
      "library": "Material Symbols",
      "version": "2.966",
      "spdx": "Apache-2.0",
      "sourceUrl": "https://github.com/google/material-design-icons",
      "licenceFile": "apps/<slot>/icons/LICENSES/Apache-2.0.txt",
      "attribution": null,
      "fetchedAt": "2026-10-02",
      "fetchedBy": "agent",
      "icons": [
        {
          "name": "add",
          "file": "app/src/main/res/drawable/ic_add.xml",
          "sha256": "<64 lowercase hex characters>",
          "sourcePath": "symbols/android/material_symbols/add/materialsymbolsoutlined/add_24px.svg",
          "attribution": null
        }
      ]
    }
  ]
}
```

Field contract:

| Field | Level | Type | Required | Meaning |
|---|---|---|---|---|
| `schemaVersion` | root | integer | yes | Must be `1`. A future shape increments it. |
| `sets` | root | array | yes | One entry per library set vendored. Non-empty. |
| `library` | set | string | yes | The set's name (`Material Symbols`, `Lucide`, …). |
| `version` | set | string | yes | The pinned release, tag, or dated snapshot. Never `latest` or a branch name. |
| `spdx` | set | string | yes | An SPDX identifier from the allowlist (§2), or the conditional `LicenseRef-Remix-Icon-1.0`. |
| `sourceUrl` | set | string | yes | HTTPS URL of the canonical repository or release the bytes came from. |
| `licenceFile` | set | string | yes | Repo-relative path to the committed licence text, under `apps/<slot>/icons/LICENSES/`. |
| `attribution` | set | object \| null | yes (may be `null`) | `null` when the licence needs only the committed text; otherwise `{ "text": string, "url": string, "inApp": boolean }`. See rule 9. |
| `fetchedAt` | set | string | yes | ISO date (`YYYY-MM-DD`) the bytes were fetched. |
| `fetchedBy` | set | string | yes | `"agent"` or a person's name. |
| `icons` | set | array | yes | One entry per committed icon from that set. Non-empty. |
| `name` | icon | string | yes | The icon's slug as used in the set. |
| `file` | icon | string | yes | Repo-relative path of the committed artifact: a `res/drawable/*.xml` `VectorDrawable` or an `ImageVector` Kotlin file. |
| `sha256` | icon | string | yes | SHA-256 of the committed file, 64 lowercase hex characters. |
| `sourcePath` | icon | string | yes | The path inside the upstream repository/release the file was derived from. |
| `attribution` | icon | object \| null | no | Per-icon override when one icon in a multi-licence set differs (e.g. Lucide's Feather-derived list). |

**Rule 2 — every used icon is listed, and every listed icon exists.** A committed
`res/drawable/ic_*.xml` or `ImageVector` that is not in some `sets[].icons[].file` is an **orphan**;
a listed file that does not exist is a failure; a `sha256` that does not match the committed bytes
is a failure. (**INFERRED** from research encodable rule 3; the check does not exist yet — see
"Honest limit".)

---

## 2. The licence allowlist

**Rule 3 — a vendored set's `spdx` must be on the allowlist.** Exact strings:

```text
MIT
ISC
Apache-2.0
OFL-1.1
CC-BY-4.0
MIT-0
BSD-2-Clause
BSD-3-Clause
```

- `MIT` / `ISC` — permissive; the condition is that the copyright notice and permission text travel
  with the copies, satisfied by the committed licence file. Does **not** require an on-screen
  credit. (**MEASURED** — Tabler `LICENSE` and Phosphor `LICENSE` read; Lucide `LICENSE` read, ISC
  for the set plus a named Feather-derived list kept under MIT.)
- `Apache-2.0` — permissive with a patent grant. §4(a): recipients get a copy of the licence.
  §4(d): if upstream ships a `NOTICE`, reproduce its attribution notices — `google/material-design-icons`
  has no `NOTICE`, so this adds nothing for Material specifically, while §4(a) still applies.
  (**MEASURED** — canonical text at <https://www.apache.org/licenses/LICENSE-2.0> and Google's
  repo `LICENSE` read; the absent `NOTICE` is **ON RECORD** from research §1.2/§1.5.)
- `OFL-1.1` — for icon fonts: may be bundled, embedded, redistributed and sold with any software,
  provided the reserved font name is not reused and the OFL text travels. (**MEASURED** — read in
  full inside Font Awesome Free's `LICENSE.txt`.)
- `CC-BY-4.0` — commercial use permitted, **attribution required** ("give appropriate credit,
  provide a link to the license, and indicate if changes were made"). (**MEASURED** — the deed at
  <https://creativecommons.org/licenses/by/4.0/> read; Font Awesome Free's `LICENSE.txt` states the
  icons are CC BY 4.0.)
- `MIT-0`, `BSD-2-Clause`, `BSD-3-Clause` — permissive; listed for completeness. **UNVERIFIED** —
  no such set is used today and no licence text for them was read in this pass.

**A file with several licences passes only if every part is allowed and each is recorded** — for
example Lucide (`ISC` + `MIT`) and Font Awesome Free (`CC-BY-4.0` + `OFL-1.1` + `MIT`).
(**MEASURED**, the same licence texts.)

### 2.1 The one conditional entry

**Rule 4 — Remix Icon is allowed for UI iconography only, and never as the mark.** Remix Icon has
**no SPDX identifier**; it is governed by a custom "Remix Icon License v1.0" (January 2026), and it
must be recorded as the literal string `LicenseRef-Remix-Icon-1.0`, never as `Apache-2.0` — the set
moved off Apache-2.0, so any source calling it Apache-2.0 is outdated and wrong. Its §2 permits
commercial use in mobile applications and its §2.4 makes attribution optional, so the set is
bundleable for UI usage; its §3.3 forbids brand use (rule 10). (**MEASURED** — the `License` file
read in full: §1 grant, §2.1 commercial use, §2.4 optional attribution, §3.3 brand prohibition.)

This is the one place this contract reconciles two parts of the research: research §1.1 and §6.1
say the Remix **set** is bundleable for UI while the **mark** use is not, and research encodable
rule 1's exact-string list happens not to name Remix's licence. Because the licence text itself is
what was measured, the conditional entry above is the record, and the brand prohibition is carried
by rule 10. (**INFERRED** — the reconciliation; the licence facts are **MEASURED**.)

---

## 3. The licence denylist

**Rule 5 — a vendored set must not carry a denied identifier or term.**

| Never bundle | Why | Label |
|---|---|---|
| Any `CC-BY-NC-*` (any version), `CC-BY-NC-SA-*` | *"NonCommercial — You may not use the material for commercial purposes"*; every slot is sold or ad-supported, and a commercial use is one *"primarily intended for commercial advantage or monetary compensation"* | MEASURED (deed) |
| Any `CC-BY-SA-*` | *"ShareAlike — you must distribute your contributions under the same license"*; incompatible with a paid closed-source app | MEASURED (deed) |
| Any `GPL-*`, `AGPL-*`, `LGPL-*` | Copyleft: distribution can oblige release of the covered work under the same terms | MEASURED (GPL-3.0 preamble via the OSI mirror; `gnu.org` returns 403) |
| Any `CC-BY-ND-*` | NoDerivatives: a converted `VectorDrawable` is a derivative | ON RECORD (research §8 rule 2) |
| **Font Awesome Pro** icons without a purchased plan/seat | Proprietary contract, per-seat, no open licence | MEASURED (partial — the full contract was not read; treated as proprietary, which is the safe direction) |
| **Streamline Premium**, beyond the free/one-time allowance | Paid licence; a **100-icons-per-project** limit; embedding for end users needs an extended licence | ON RECORD (research §1.3–§1.4) |
| **Streamline** "free but not open-source" sets, without attribution | *"you can't embed a full set into your app"* and attribution is required | ON RECORD (research §1.3) |
| **Flaticon** free icons, without attribution | Free tier is attribution-only and the complete Terms of Use prevail over the free grant; some "free" icons have unclear upstream provenance | ON RECORD (research §1.3–§1.4) |
| **Icons8** free assets, without a visible clickable link | Free-account terms require a visible, clickable link and forbid derivative copies — a paid app cannot honour that cleanly | ON RECORD (research §1.3) |
| **Any set with no licence file, or whose "free" page contradicts its repository** | An unknown licence is not a permissive one | INFERRED |
| **Any "free tier" that is free-as-in-beer only** (download-site tokens such as Iconscout-style sites) | The grant is a service ToU, revocable and contingent on a link; not an open licence | ON RECORD (research §1.3) |

**`NOASSERTION`, an absent `spdx`, or a premium/attribution service term is a failure, not a
note.** A permissive-looking download button is not a licence file. (**MEASURED** for the deeds and
for Font Awesome's statement; the *legal effect of copyleft on an APK* is **UNVERIFIED** — a counsel
question — which is exactly why the rule fails closed rather than reasoning about it.)

Two cautions that are **not** denials: **Feather** and **Font Awesome Free** are bundleable but
frozen/superseded, and **Material Icons** (the classic set) is `Apache-2.0` and bundleable but no
longer updated — prefer **Material Symbols**. (**ON RECORD**, research §1.3.)

---

## 4. Fetch once, during development, and commit

**Rule 6 — icons are fetched once, during development, and committed. They are never fetched at
build time and never at runtime.** (**ON RECORD** — `AGENTS.md` "a build must not depend on the
network" and `.cursor/rules/workspace-hygiene.mdc`.)

Concretely:

- The SVG (or subset font) is downloaded **by hand or by an agent during development**, converted to
  a `res/drawable/ic_<name>.xml` `VectorDrawable` (or committed as an `ImageVector`), and the
  result is committed. The conversion is build-time-only in the sense that it happens once, by hand;
  `gradlew` performs no icon download.
- The **variable-font** path commits the subset `.ttf` into `res/font/`, never a Google Fonts URL.
- No `@font-face` from a remote host, no `Downloadable Fonts` provider fetch for icon glyphs, no
  `androidx.compose.ui.text.googlefonts` download at runtime — network at runtime is the same defect
  as network at build time here. (**ON RECORD**, research §4.)
- **Failure shapes** (research encodable rule 4): an icon artifact declared in
  `gradle/libs.versions.toml` or any `build.gradle.kts`; a Gradle plugin or task that downloads
  icons; an npm/fontsource-style package consumed by a build step; a `googlefonts` URL in shipped
  sources. Vendored bytes under `res/` or in the slot's `ui/` are the pass shape. (**INFERRED** as
  a failure list; no check enforces it yet.)
- Temporary files used while fetching or converting (a raw SVG, a PNG preview, a download log) go
  under `.kiln/scratch/`, **never** `%TEMP%`/`%TMP%`/`$TMPDIR` and never anywhere outside the
  repository. The committed output lives under the slot, not in scratch. (**ON RECORD**,
  `workspace-hygiene.mdc`.)

**Rule 7 — a committed UI icon is an Android-consumable vector, not a raw SVG.** The committed
artifact is a `VectorDrawable` (`res/drawable/*.xml`) or a Compose `ImageVector`; no `.svg` ships
under `app/src/main/`, and no runtime SVG-rendering dependency is added solely for icons.
(**MEASURED** — `VectorDrawable` supports Tiny SVG 1.2 except text; the SVG import is a build-time
conversion. Research §3c.)

### 4.1 The deprecated Material icon artifacts are not allowed

**Rule 8 — the slot does not depend on `androidx.compose.material:material-icons-core` or
`material-icons-extended`** (nor their `org.jetbrains.compose.material` equivalents). Both were
removed from current Material 3 and are intentionally pinned at `1.7.3`, so they cannot resolve from
the factory's pinned Compose BOM **2026.09.00**; Google's own recommendation is to download Vector
Drawable XML from the Material Symbols "Android" tab instead. Failure shape: either coordinate in
`gradle/libs.versions.toml` or any `build.gradle.kts`. (**MEASURED** — developer.android.com
package-summary and images guide, AOSP commit, JetBrains commit, all cited in research §2.2.)

---

## 5. Licence text travels with the set

**Rule 9 — for every distinct `spdx` in `provenance.json`, a verbatim licence file exists in the
repository** at `apps/<slot>/icons/LICENSES/<SPDX>.txt`, is a regular non-empty file **named for that
`spdx`**, and is referenced by every row using it. For Apache-2.0 the file is the Apache-2.0 text; if
the upstream ships a `NOTICE`, that too. (**MEASURED** — Apache-2.0 §4(a) and §4(d), and MIT/ISC/OFL's
notice clauses, read; the `LICENSES/` path shape is **ON RECORD** from research §5.1.)

The name is part of the rule, not a convention: a `licenceFile` that exists but is named
`Apache-2.0.txt` while the set declares `MIT` is a committed text that does not discharge the
obligation it is filed under, and `icons.provenance` fails it (**INFERRED**, audit 2026-10-02).

The licence file is the durable half of the obligation for the permissive sets, and it is what makes
the obligation auditable later. It is **not in the APK**: the store pack and the About/Licences
surface are where a recipient of the built artifact is told, per rule 11 below.

---

## 6. Tint from the theme; no colour of its own

**Rule 10 — a UI icon draws in the theme's content/role colour and carries no hard-coded fill.**

- The tint comes from `LocalContentColor.current` or an explicit `MaterialTheme.colorScheme.<role>`,
  never a literal.
- Failure shapes: an `android:fillColor="#RRGGBB"` literal inside a vendored `VectorDrawable`
  (other than a mask), a `<gradient>` with literal stops, a literal colour inside an `ImageVector`,
  or an `Icon`/`Image` call that omits `tint` or sets it to a literal instead of the theme.
- **What the sensor reads of that list, stated rather than implied** (audit 2026-10-02, F8): it
  matches `android:fillColor` only — any namespace prefix, either quote style, comments blanked —
  because that is the failure shape named above. The bare `android:color` of a `<ripple>` is not a
  path fill and is not read; gradient stops and `strokeColor` are not read either; and a
  `Color(0xFF…)` literal in a Compose `Icon(...)` call is a Kotlin scan this sensor does not make.
- **The line between identity and vocabulary is the palette, not the file name.** The launcher
  vectors (`ic_launcher_*.xml`) are excluded by design and are the subject of `icons.launcher`. Any
  other drawable — including a slot-owned in-app brand mark — is UI iconography, and a literal fill
  there is *identity* (reported as a note naming the file) when it **is a role of the slot's own
  declared palette**, and a *failure* when it is not. A slot that declares no palette has no identity
  to appeal to, so every literal fill fails — the conservative direction.
- This is the same rule as the app's own palette being one source of truth
  (`.cursor/rules/visual-identity.mdc`), applied to a set the slot did not draw. A stock SVG with a
  baked fill breaks it. (**ON RECORD**, visual-identity rule; the failure shapes are **MEASURED**
  from research encodable rule 6, which itself read the grounding slot's own statement.)

Accessibility is a **call-site** decision, not a new rule: an optional decorative icon that repeats
an adjacent label gets `contentDescription = null`; an icon that is the only content of a control
needs an accessible name; an interactive target is at least 48 dp. This routes to the existing
`uiux.a11y` finding and is not duplicated here. (**ON RECORD**, `.cursor/rules/ui-ux-baseline.mdc`,
research encodable rule 12.)

---

## 7. One family per slot, plus the slot's own mark

**Rule 11 — all library UI icons in a slot come from one `library`, plus the slot's own mark and any
slot-drawn domain glyph.** Mixing two sets of different stroke weights in one screen is the most
visible way iconography looks assembled. Choose one stroke weight / corner family and stay with it
(Material Symbols ships Outlined / Rounded / Sharp for this reason). (**INFERRED** — the "looks
assembled" failure, research §7.1 and §7.9; **ON RECORD** — visual-identity's per-slot identity.)

Two documented exceptions:

- **A glyph whose meaning the app defines** (a domain noun with no honest library match) is drawn
  slot-owned, in the same stroke weight and on the same grid as the library, rather than bending a
  library icon to mean something it does not. (**ON RECORD**, research §6.3.)
- **A second `library` entry may exist only with a written justification field** in the record;
  without it, two or more library sets for UI iconography is a failure. (**INFERRED**, research
  encodable rule 9.)

---

## 8. Attribution, and where the text lives

**Rule 12 — if any vendored set requires attribution, the credit is reachable from inside the app,
and the record says so.** Set `attribution: { "text": …, "url": …, "inApp": true }` on that set;
`null` means the committed licence text is the whole obligation.

- The obligation is real for `CC-BY-4.0`: credit, a link to the licence, and an indication of
  changes. (**MEASURED** — the CC BY 4.0 deed and Font Awesome Free's `LICENSE.txt`.)
- `MIT` / `ISC` / `Apache-2.0` require **no** on-screen credit; the committed licence text is the
  obligation. An on-screen credit is courteous, not required. (**MEASURED**, the licence texts.)
- **Where the text lives:** the app's **About / Settings → "Licences"** surface — the same
  destination Google's `oss-licenses` activity would populate — beside the pack's own copy where the
  licence asks for a link. (**ON RECORD**, research §5.2; the `oss-licenses` tooling as the
  recommended home is **ON RECORD** at <https://developers.google.com/android/guides/opensource>.)
- Attribution strings are user-facing, so they belong in `values/` resources and are declared
  non-translatable where they must not be translated (`.cursor/rules/localization.mdc`).
  (**ON RECORD**, research §5.2.)

---

## 9. The mark is never a library icon

**Rule 13 — the launcher icon, the adaptive-icon foreground, the monochrome/themed layer, and any
in-app brand mark are derived from `store/studio.json`'s `mark`, never from a library icon.** A
library glyph as the mark fails this contract twice over:

- **Design and policy.** The store icon must be a distinct, simple silhouette that reads at small
  sizes and must not be *"similar to those of existing products or services"*; if two apps ship the
  same glyph as their launcher icon, a user cannot tell them apart and the near-duplicate evidence
  Play's Repetitive Content policy acts on is exactly that. (**ON RECORD**, Play icon design
  specifications and metadata policy, research §6.1.)
- **Licence.** Some sets explicitly forbid brand use. Remix Icon License v1.0 §3.3: *"You may NOT
  use the Icons, whether modified or unmodified, as … a logo, trademark, or brand identifier … a
  brand mark, app icon, or identity symbol where the Icon itself represents the brand."*
  (**MEASURED** — the `License` file read in full, including the rationale.) Font Awesome separately
  restricts its brand logos to representing the brand they depict. (**MEASURED** — its `LICENSE.txt`.)

The Android 13+ monochrome layer is the mark **in one colour**, tinted by the system: a generic
stock glyph there produces a tile identical to every other app that used it. Test it mechanically by
flattening the mark to a single ink colour and re-running the adaptive-icon reach/stroke gates — if
two masses that were separate in colour become one, hue was carrying structure.
(**ON RECORD**, Android themed-icon requirements, research §6.2.)

---

## How it is verified

`scripts/core/icons.mjs` is the sensor. It runs in the `icon` group beside the adaptive-icon check,
so `node scripts/verify.mjs --slot <slug>` runs it. Its `reports` list is written out literally rather
than derived, because the fact ledger reads the registry as *text* and can only name an id that
appears there as a literal `id:` — a load-time assertion fails if the list drifts from the sensor's
own `ICON_IDS`, so a fact can name any leg below.

| Leg | What it decides |
|---|---|
| `icons.mark` | the mark `store/studio.json` declares resolves in the Python-owned `MARK_REGISTRY`, so a pack cannot declare an identity the studio cannot render |
| `icons.launcher` | the launcher vectors are the slot's own rather than the scaffold's. Two criteria, both measured: the **geometry** (the `pathData` set, read with any namespace prefix and either quote style, comments blanked, so renaming `android:` to `a:` or switching to single quotes does not evade it) is not the scaffold's; and every literal fill of the slot's own vector **is a role of the declared `store/studio.json` palette** (an eight-digit literal read as its six-digit colour). The palette half is skipped only when the slot declares no palette at all — a killed slot with no identity has nothing to be off. So recolouring a copied vector does not evade it, and neither does new geometry wearing foreign colours |
| `icons.provenance` | `apps/<slot>/icons/provenance.json` resolves: the record parses, schemaVersion is 1, the §2 allowlist and §3 denylist are consulted, every named file is a regular committed file with a matching `sha256`, and `licenceFile` is a regular non-empty file **named for the set's own `spdx`**. A record that cannot be evaluated degrades **locally**: only `icons.provenance` (and `icons.licence`, which depends on it) becomes `FAIL`/`UNVERIFIED`, and the other five legs keep their real verdicts |
| `icons.licence` | each declared `spdx` has committed, non-empty `icons/LICENSES/<SPDX>.txt` text |
| `icons.offline` | no icon artifact is fetched at build or run time. The shipped-source shapes are a **superset** of the build-file shapes, so `https://api.iconify.design/mdi.json?icons=plus` — a real runtime icon fetch with no `.svg` extension — is caught in Kotlin as well as in a build file. Comments are blanked before matching: a URL a slot records as its icon's provenance is documentation, not a fetch, while the same URL in a real dependency line still fails |
| `icons.deprecated` | no deprecated `material-icons-core`/`-extended` coordinate in a real dependency line; a comment naming the artifact a slot removed is not a coordinate |
| `icons.tint` | UI iconography carries no literal `android:fillColor`; the mark (`ic_launcher_*`) is excluded by design, and a literal fill that is a role of the slot's own declared palette on another slot-owned drawable is reported as a note rather than failed (§6 above) |

The mark's **geometry** is still measured independently — `python scripts/check-adaptive-icon.py`,
`scripts/core/icon.mjs` (ink outside the 66dp safe zone, beyond the circle keyline) and
`python scripts/visual-qa.py --slot <slug>` (mask clipping, among its pixel measurements) — and those
cover rule 13's geometry rather than its provenance.

Falsifiability was observed rather than assumed: `icons.fixtures` carries **23** inline fixtures in the
`invariants` group, and neutering the `icons.launcher` leg so it returned no problems turned that
suite `FAIL` (`the scaffold copy is a finding: expected 1, got 0`), after which the module was
restored byte-identically. On 2026-10-02 an adversarial audit of the sensor produced nine repairs,
and each one was mutation-checked the same way: the fix was neutered, `icons.fixtures` was confirmed
to go `FAIL` naming the fixture that owns the rule, and the module was restored byte-identically
(sha256 `b07468e7…` before and after). Two readings the sensor makes today, so the record is not a
promise: the live slot `apps/2026-10-01-lawn-round-log` fails `icons.launcher` on all three vectors,
and no slot carries an `icons/provenance.json` yet, so every slot reports `icons.provenance` and
`icons.licence` as `UNVERIFIED` — never as a pass, because nothing scanned must not read as nothing
found.

### The 2026-10-02 audit repairs

An adversarial pass over the sensor produced twelve findings; ten were repaired in
`scripts/core/icons.mjs` (F1–F10 below) and two were judged acceptable under-approximations that are
stated under "The honest limit" rather than papered over.

- **A malformed record degrades locally, never globally.** `icons[].file` resolving to a directory
  used to make `sha256Of` throw out of the sensor, which reported *all seven* legs `UNVERIFIED` and
  made the flat finding list throw instead of producing the `icons.launcher` `FAIL` it had already
  computed. The record legs are now wrapped and each file is inspected before it is read: an
  unreadable input is a stated `UNVERIFIED`, never an exception. The same pass made `licenceFile` a
  regular non-empty file tied to its `spdx` by name, rather than an `existsSync` that a directory
  satisfied.
- **The two evasion holes in `icons.launcher` are closed.** A scaffold copy previously passed by
  renaming the namespace prefix (`xmlns:a=…`, `a:pathData`) or by single-quoting `pathData`; both now
  fail exactly as the byte-identical copy does.
- **Comments are documentation, not code.** `icons.offline` no longer fires on a commented source URL
  and `icons.deprecated` no longer fires on a comment naming a removed artifact, while a real
  `implementation(...)` coordinate still fails. The shipped-source fetch shapes were also brought up
  to the build-file list's strength, so an icon CDN URL with no `.svg` extension is caught in Kotlin
  too.
- **The tint leg reads what rule 10 names.** `android:color` on a `<ripple>` is no longer read as a
  fill, and a palette-role literal on a slot-owned drawable is a note rather than a failure (see §6).
- **Two documentation claims were made true rather than softened.** The module header no longer says
  it is unregistered, and the launcher leg now really is judged by the palette as the table above
  says.

The two accepted limits are unchanged and are stated above: a copy whose *geometry was edited* is not
detected, and a non-product (grandfathered or killed) slot carries its findings as a printed note
rather than a red row.

### The honest limit

This contract proves that a licence was **recorded, on an allowlist, with its text committed** — it
cannot prove the bytes came from where the record says, cannot prove an icon is beautiful or
well-chosen, and cannot decide whether a given glyph is identity or vocabulary. That last judgement
is the owner's, at the phase-1 review, exactly as it is for the palette and the mark. What the rules
here can do is make the common, measured failure unshippable: a
non-commercial licence, a copyleft licence, a library glyph promoted to the mark, a set fetched at
build time, an icon that carries its own colour.

---

## Verification status

| Claim | How it was established | Label |
|---|---|---|
| Tabler Icons — MIT | `LICENSE` fetched and read | MEASURED |
| Phosphor Icons — MIT | `LICENSE` fetched and read | MEASURED |
| Lucide — ISC, with a named Feather-derived list under MIT | `LICENSE` fetched and read (both texts) | MEASURED |
| Remix Icon License v1.0 — commercial use permitted, attribution optional, §3.3 forbids brand/app-icon use, no SPDX id | `License` fetched and read in full | MEASURED |
| Font Awesome Free — icons CC BY 4.0, fonts OFL 1.1, code MIT; brand-logo restriction | `LICENSE.txt` fetched and read | MEASURED |
| Apache-2.0 — §4(a) licence to recipients, §4(d) NOTICE, §6 no trademark rights | Canonical text read, and Google's repo `LICENSE` read | MEASURED |
| CC BY 4.0 — attribution + link + indicate changes | Deed read | MEASURED |
| CC BY-NC 4.0 — NonCommercial | Deed read | MEASURED |
| CC BY-SA 4.0 — ShareAlike | Deed read | MEASURED |
| `google/material-design-icons` has no `NOTICE` file | **Not re-read in this pass** — taken from research §1.2/§1.5 | ON RECORD |
| Heroicons, Feather, Bootstrap Icons, Ionicons, Iconoir, Majesticons — MIT | **Not read in this pass** — taken from research §1.1 | ON RECORD |
| Font Awesome **Pro** full contract | **Not read** — treated as proprietary, the safe direction | UNVERIFIED |
| Streamline / Flaticon / Icons8 free-tier terms | **Not read in this pass** — taken from research §1.3–§1.4 | ON RECORD |
| GPL-3.0 copyleft | **Not read in this pass**; the research read the preamble via the OSI mirror (`gnu.org` 403) | ON RECORD |
| Legal effect of a copyleft icon inside an APK | A counsel question; the rule fails closed instead | UNVERIFIED |
| `material-icons-*` deprecation, pin `1.7.3`, removal | **Not re-read in this pass** — taken from research §2.2 | ON RECORD |
| The `icons/provenance.json` shape and rules 1–13 as **code** | Written as a contract here, and read by a program since 2026-10-02: `scripts/core/icons.mjs` resolves the record, checks the allowlist and denylist, verifies every `sha256`, and reports `icons.provenance` / `icons.licence` | IMPLEMENTED — `icons.fixtures` drives it, 23 fixtures, each 2026-10-02 repair mutation-checked |

The licence texts this pass read are: Tabler, Phosphor, Lucide, Remix Icon, Font Awesome Free,
Apache-2.0 (canonical and Google's copy), and the CC BY 4.0 / CC BY-NC 4.0 / CC BY-SA 4.0 deeds.
Everything not in that list is carried from
`docs/research/2026-10-02-ui-icon-libraries-and-licensing.md` and is labelled `ON RECORD` or
`UNVERIFIED` above, never `MEASURED`.
