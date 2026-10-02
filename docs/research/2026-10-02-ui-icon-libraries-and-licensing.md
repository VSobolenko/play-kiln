# UI icon libraries and licensing

Research for the owner's instruction, recorded 2026-10-02, that the factory stop hand-drawing
glyphs and use a **vetted professional icon set** instead. Translated from Russian: *"Modern apps
do not do that — they download ready-made icons from the internet… Let it have internet access,
download, and paste into our app. That only raises quality."*

This document is the **evidence layer** that makes that instruction safe and mechanical. It does
not implement anything, adds no dependency and downloads no asset. Its job is to answer, with
sources, three questions the factory cannot answer by taste:

1. **What may legally be bundled into a paid, closed-source Play app** — the licence table in §1.
2. **How an Android slot in this pinned toolchain should actually take an icon set in** — §3.
3. **Which parts of an app's identity a stock set is still wrong for** — §6, and how that reconciles
   with the existing rule.

Labels are the factory's four: **MEASURED** (read in the source itself), **ON RECORD**,
**INFERRED**, **UNVERIFIED**. For a licence, `MEASURED` means the linked licence text was fetched
and read in this run; §1.5 names every fact that was **not**.

Pinned toolchain this research assumes, from `AGENTS.md` and `scaffold/android-compose/`:
Gradle **9.7.1**, AGP **9.4.1** (built-in Kotlin), Kotlin **2.3.21**, Compose BOM **2026.09.00**
(Material 3 1.4.0), `compileSdk 37`, `targetSdk 36`, `minSdk 26` (**MEASURED**,
`scaffold/android-compose/app/build.gradle.kts:29-37`). R8 full mode, release minification and
resource shrinking are factory defaults.

---

## 0. What the owner's instruction changes — and what it does not

`.cursor/rules/visual-identity.mdc` currently bans, in one sentence: *"Emoji, clipart, or **a stock
icon as the app's mark or as UI iconography**."* The grounding slot
`apps/2026-10-01-lawn-round-log/…/ui/components/Glyphs.kt` reads that ban literally and hand-draws
~15 glyphs as Compose `Canvas` primitives; its header comment says so: *"`.cursor/rules/
visual-identity.mdc` bans 'a stock icon as the app's mark or as UI iconography'… A
`material-icons-*` dependency would make the app legible as every other Compose app."*
(**MEASURED**, `Glyphs.kt:25-31`).

The owner's instruction is about the **second half only**. Read precisely, it says:

- **UI iconography** — the arrow, the bin, the confirmation tick, the toolbar glyph, the empty-state
  illustration — is a **solved, professional, standardised problem**, and hand-drawing it produces
  amateurish results. This half of the ban should be **replaced** by a requirement to use a vetted
  set, with provenance.
- **The app's mark** — the launcher icon, the in-app brand glyph, the monochrome/themed icon — is
  **not** solved by a stock set, for reasons in §6 that are both design and policy. This half of the
  ban **stays**.

So the two positions do not actually conflict once the single sentence is split. The precise
amendment recommended (prose only — the rule file is the owner's call and was not edited):

> Replace *"a stock icon as the app's mark or as UI iconography"* with
> *"a stock icon as the app's **mark**"*, and add a positive requirement:
> *"UI iconography comes from a vetted, licence-cleared set (allowlist in
> `docs/research/2026-10-02-ui-icon-libraries-and-licensing.md` §1), committed to the slot with its
> licence text and a provenance record, tinted from the theme — never hand-drawn as path
> primitives, and never carrying its own colour."*

The distinction the amendment encodes is the one Google itself draws: **Material 3's icon system
has two roles** — the *product logo / launcher icon* (which Google Play's own specifications say
must be a distinct, recognisable mark) and the *UI icon* (for which Google ships a standard set on
a 24 dp grid) (**ON RECORD**, Google Play icon design specifications and Material Symbols guide,
cited in §6). The factory was applying the launcher-icon rule to both roles.

---

## 1. The licence table — the single most important output

### 1.1 The table

"Commercial use" = may be used in an app the owner sells. "Attribution" = must credit the author,
and the form. "Bundle in a paid closed-source Play app" is the actual question this factory needs
answered: **can these bytes ship inside a signed APK/AAB that the user pays for, with no source
disclosure?**

| Set | SPDX identifier | Licence name | Commercial use? | Attribution required? | Bundle in paid closed-source app? | Canonical source | Licence file |
|---|---|---|---|---|---|---|---|
| **Material Symbols** (Google) | `Apache-2.0` | Apache License 2.0 | Yes | Not required (attribution "appreciated", per Google's README) | **Yes** | [github.com/google/material-design-icons](https://github.com/google/material-design-icons) | [LICENSE](https://raw.githubusercontent.com/google/material-design-icons/master/LICENSE) |
| **Material Icons** (Google, classic) | `Apache-2.0` | Apache License 2.0 | Yes | Not required | **Yes** (frozen — no new icons since 2022) | same repo | same LICENSE |
| **Lucide** | `ISC` (+ `MIT` for ~100 Feather-derived icons) | ISC License | Yes | Not required | **Yes** | [github.com/lucide-icons/lucide](https://github.com/lucide-icons/lucide) | [LICENSE](https://raw.githubusercontent.com/lucide-icons/lucide/main/LICENSE) |
| **Phosphor Icons** | `MIT` | MIT License | Yes | Not required | **Yes** | [github.com/phosphor-icons/core](https://github.com/phosphor-icons/core) | [LICENSE](https://raw.githubusercontent.com/phosphor-icons/core/main/LICENSE) |
| **Tabler Icons** | `MIT` | MIT License | Yes | Not required | **Yes** | [github.com/tabler/tabler-icons](https://github.com/tabler/tabler-icons) | [LICENSE](https://raw.githubusercontent.com/tabler/tabler-icons/main/LICENSE) |
| **Heroicons** | `MIT` | MIT License | Yes | Not required | **Yes** | [github.com/tailwindlabs/heroicons](https://github.com/tailwindlabs/heroicons) | [LICENSE](https://raw.githubusercontent.com/tailwindlabs/heroicons/master/LICENSE) |
| **Feather** | `MIT` | MIT License | Yes | Not required | **Yes** (frozen/unmaintained) | [github.com/feathericons/feather](https://github.com/feathericons/feather) | [LICENSE](https://raw.githubusercontent.com/feathericons/feather/main/LICENSE) |
| **Remix Icon** | **no SPDX id** — custom "Remix Icon License v1.0" | Remix Icon License v1.0 (proprietary-permissive) | Yes | Not required ("optional") | **Yes for UI icons — but the licence forbids using them as a logo, brand mark, or app icon** | [github.com/Remix-Design/RemixIcon](https://github.com/Remix-Design/RemixIcon) | [License](https://raw.githubusercontent.com/Remix-Design/RemixIcon/master/License) |
| **Bootstrap Icons** | `MIT` | MIT License | Yes | Not required | **Yes** | [github.com/twbs/icons](https://github.com/twbs/icons) | [LICENSE](https://raw.githubusercontent.com/twbs/icons/main/LICENSE) |
| **Ionicons** | `MIT` | MIT License | Yes | Not required | **Yes** | [github.com/ionic-team/ionicons](https://github.com/ionic-team/ionicons) | [LICENSE](https://raw.githubusercontent.com/ionic-team/ionicons/main/LICENSE) |
| **Font Awesome Free** | `CC-BY-4.0` (icons) / `OFL-1.1` (fonts) / `MIT` (code) | CC Attribution 4.0 + SIL OFL 1.1 + MIT | Yes | **Yes** — CC BY requires credit | **Yes, with attribution** | [github.com/FortAwesome/Font-Awesome](https://github.com/FortAwesome/Font-Awesome) | [LICENSE.txt](https://raw.githubusercontent.com/FortAwesome/Font-Awesome/6.x/LICENSE.txt) |
| **Font Awesome Pro** | `LicenseRef-Proprietary` | Font Awesome Pro commercial licence (per-seat, perpetual for paid version) | Only with a purchased Pro licence | N/A (contract) | **No without a paid licence** | [fontawesome.com/license](https://fontawesome.com/license) | same page (contract, not an open licence) |
| **Iconoir** | `MIT` | MIT License | Yes | Not required | **Yes** | [github.com/iconoir-icons/iconoir](https://github.com/iconoir-icons/iconoir) | [LICENSE](https://raw.githubusercontent.com/iconoir-icons/iconoir/main/LICENSE) |
| **Majesticons** | `MIT` | MIT License | Yes | Not required | **Yes** | [github.com/halfmage/majesticons](https://github.com/halfmage/majesticons) | [LICENSE](https://raw.githubusercontent.com/halfmage/majesticons/main/LICENSE) |
| **Streamline** | **no SPDX id** — mixed | Some free sets `CC-BY-4.0`; other free sets proprietary-with-attribution; premium commercial | Partly | Depends on set (see §1.4) | **Only premium, and only within the 100-icons-per-project allowance** | [streamlinehq.com](https://www.streamlinehq.com/) | [license tl;dr](https://help.streamlinehq.com/en/articles/6021970-license-tl-dr) |

The three `MIT`-and-`ISC` rows and the two `Apache-2.0` rows are the whole of the **safe default
allowlist**: permissive, commercial-use, no attribution obligation that has to surface in the UI.
The table's *"bundle?"* column is the one a builder should read.

### 1.2 The licence families, and what each one obliges

**`MIT` / `ISC` (permissive).** MEASURED from each licence text above. Permission is granted "to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies" (MIT wording; ISC is
equivalent in effect), on one condition: *"The above copyright notice and this permission notice
shall be included in all copies or substantial portions of the Software."* For a vendored icon set,
that is satisfied by committing the licence text beside the icons and keeping it in the repo — it
does **not** require an on-screen credit in the app. Tabler, Phosphor, Heroicons, Feather,
Bootstrap Icons, Ionicons, Iconoir and Majesticons are all plain MIT (**MEASURED**, their `LICENSE`
files, linked above). Lucide's `LICENSE` is ISC for the set as a whole, with a named list of ~100
Feather-derived icons kept under MIT; a vendored icon from that list should carry the MIT notice,
which the same file already contains (**MEASURED**,
[lucide LICENSE](https://raw.githubusercontent.com/lucide-icons/lucide/main/LICENSE)).

**`Apache-2.0` (permissive, with patent grant and a NOTICE rule).** MEASURED from the canonical
text ([apache.org/licenses/LICENSE-2.0](https://www.apache.org/licenses/LICENSE-2.0)). Two clauses
matter more than the headline grant:

- §4(a): *"You must give any other recipients of the Work or Derivative Works a copy of this
  License."* — a distributed APK/AAB containing the icons must be accompanied by the Apache-2.0
  text. In practice: commit the licence text in the repo (the store pack / About route then
  satisfies the "recipients" half).
- §4(d): if the upstream work ships a **`NOTICE` file**, derivative works must reproduce its
  attribution notices. Google's `material-design-icons` repository has **no `NOTICE` file**
  (**MEASURED** — `NOTICE` at the repo root returns 404), so §4(d) adds nothing for Material
  Symbols specifically; §4(a) still applies. This is the one obligation the "attribution not
  required" phrasing in Google's README does **not** remove: Google's sentence is about *credit in
  the UI*, not about §4(a).
- §6: no trademark rights are granted — relevant to §6 of this document.

**`CC-BY-4.0` (attribution required).** MEASURED for Font Awesome Free's icons, from its own
`LICENSE.txt`: *"Icons: CC BY 4.0 License… attribution is required by MIT, SIL OFL, and CC BY
licenses"*, and Font Awesome's own mitigation is that *"Downloaded Font Awesome Free files already
contain embedded comments with sufficient attribution, so you shouldn't need to do anything
additional when using these files normally"* (**MEASURED**,
[LICENSE.txt](https://raw.githubusercontent.com/FortAwesome/Font-Awesome/6.x/LICENSE.txt)). The
general CC BY obligation (read from the deed) is: *"give appropriate credit, provide a link to the
license, and indicate if changes were made"*
(https://creativecommons.org/licenses/by/4.0/). For a vendored FA Free icon, keeping the licence
file and the upstream comment satisfies this; if an icon is SVGO-optimised (which strips comments),
the credit must be restored somewhere durable.

**`CC-BY-NC-4.0` (non-commercial — FORBIDDEN in our apps).** MEASURED from the deed:
*"NonCommercial — You may not use the material for commercial purposes"*, where a *"commercial use
is one primarily intended for commercial advantage or monetary compensation"*
(https://creativecommons.org/licenses/by-nc/4.0/). Every slot this factory builds is either sold
(one-time unlock) or ad-supported — both are commercial by that definition. **An NC icon set may
not be bundled, full stop.**

**`CC-BY-SA` / `GPL` (copyleft — FORBIDDEN for a closed-source paid app).** Both were read:

- CC BY-SA 4.0 deed: *"ShareAlike — If you remix, transform, or build upon the material, you must
  distribute your contributions under the same license as the original"*
  (https://creativecommons.org/licenses/by-sa/4.0/).
- GPL-3.0 (the preamble, read via OSI's copy because `gnu.org` returned HTTP 403 in this run):
  *"if you distribute copies of such a program, whether gratis or for a fee, you must pass on to
  the recipients the same freedoms that you received. You must make sure that they, too, receive or
  can get the source code"* (https://opensource.org/license/gpl-3-0).

The consequence for this factory: bundling a CC-BY-SA or GPL-licensed asset can oblige the slot to
release the asset — and, on the GPL reading, potentially the surrounding work — under the same
terms, which is **incompatible with a paid, closed-source APK/AAB**. Whether an icon *inside* an app
makes the app itself a derivative is a legal question (**UNVERIFIED** — counsel question, exactly as
`docs/research/2026-09-23-on-device-plant-diagnosis.md:655` already flags for CC BY-SA), but the
factory does not need to resolve it: the risk is asymmetric and the fix is free (use a permissive
set), so copyleft sets go on the denylist rather than into a legal memo.

**`OFL-1.1` (fonts, including icon fonts).** MEASURED in full inside Font Awesome Free's
`LICENSE.txt`. Key clauses: fonts *"may be bundled, embedded, redistributed and/or sold with any
software provided that any reserved names are not used by derivative works"*; *"Neither the Font
Software nor any of its individual components… may be sold by itself"*; and *"Each copy contains
the above copyright notice and this license."* For a shipped app that embeds an icon font: the app
may be sold, the font may be embedded, no reserved-name (e.g. "Font Awesome") may be used for a
modified font, and the OFL text must travel with it. This is the licence family under which
**Material Symbols' variable font** *could* be shipped (though Material Symbols is Apache-2.0, not
OFL — see the table).

**Proprietary / commercial (per-seat or per-project).** Font Awesome **Pro** grants *"a perpetual
license to use the Pro version you paid for in commercial web, print, mobile, and desktop
projects"*, with "seats" for *"people who directly create content using Pro icons"*; a standard plan
is five seats (**ON RECORD**, [fontawesome.com/license](https://fontawesome.com/license) and
[the seat breakdown](https://blog.fontawesome.com/font-awesome-pro-seats/); the full contract text
was **not read in this run** — see §1.5). The legal form is a contract, not a public licence: there
is no "MIT" you can point at, and bundling Pro icons without a purchased plan or seat is a
copyright breach. Streamline is the same shape at a different price (§1.4).

### 1.3 The sets that must **never** be bundled — the denylist

This is the list that stops an agent from downloading the wrong thing. Each entry names the *reason*,
because "unclear" is not a reason and "crowded" is not one either.

| Never bundle | Why | Label |
|---|---|---|
| **Font Awesome Pro** icons, without a purchased plan/seat | Proprietary contract; no open licence; per-seat terms | MEASURED (partial, §1.5) |
| **Streamline Premium** assets, beyond the free/one-time allowance | Paid subscription or one-time licence; **100 icons per project** limit; embedding for end users needs an extended licence | MEASURED |
| **Streamline "free but not open-source"** sets, without attribution | *"All other free vector sets require attribution (link to our site)"*; and *"you can't embed a full set into your app"* | MEASURED |
| **Flaticon** free icons, without attribution | Free tier is attribution-only: *"must add a link to Flaticon and credit the author"*; some Flaticon "free" icons are of unclear upstream provenance | MEASURED |
| **Icons8** free assets, without a visible clickable link | Free-account terms require *"at least one visible and clickable link"* to icons8.com and forbid derivative copies; a paid app cannot honour that cleanly | MEASURED |
| **Any `CC-BY-NC` set** (any version) | NonCommercial clause forbids use "primarily intended for commercial advantage or monetary compensation" — every one of our slots is commercial | MEASURED (deed) |
| **Any `CC-BY-SA` / `GPL` / `AGPL` set** | Share-alike / copyleft: distribution can oblige release under the same terms — incompatible with a paid closed-source app | MEASURED (deeds + GPL preamble); legal effect on an APK UNVERIFIED |
| **Any set with no licence file, or whose "free" page contradicts its repository** | An unknown licence is not a permissive one; the factory cannot audit what it cannot cite | INFERRED |
| **Remix Icon used as the brand / launcher mark** (the *set* is bundleable; the *mark* use is not) | Licence §3.3: *"You may NOT use the Icons… as a logo, trademark, or brand identifier… app icon, or identity symbol"* | MEASURED |
| **Any "free tier" that is free-as-in-beer-only** (Flaticon, Icons8, Streamline free, Iconscout-style download sites) | The free grant is a service ToU, revocable and contingent on a link; it is not an open licence and often carries unclear provenance | MEASURED (Flaticon, Icons8, Streamline) |

Two more that are *not* exactly "never bundle" but belong on the caution list:

- **Feather** and **Font Awesome Free** are alive and MIT/CC-BY, but **Frozen / superseded**:
  Feather's last meaningful update is old and Lucide explicitly derives from it; Font Awesome Free
  is a subset of a paid product. Both are *bundleable*; neither is the best modern choice.
- **Material Icons** (the classic set) is `Apache-2.0` and bundleable but **no longer updated**
  (**MEASURED**, Google README: *"Material Icons… no longer updated"*). Prefer Material Symbols.

### 1.4 The confusion the table exists to prevent

- **"*Free* on the website" ≠ "MIT".** Flaticon's free grant is attribution-contingent, and its own
  licence certificate says the complete Terms of Use *"will prevail over the content of this
  document"* (**MEASURED**,
  [Flaticon licence certificate](https://media.flaticon.com/license/license.pdf)). A download button
  is not a licence file.
- **"*Apache-2.0*" ≠ "no obligation".** Apache-2.0 has §4(a) (ship the licence) and, if present,
  §4(d) (reproduce the `NOTICE`). Both survive a "no attribution required" marketing sentence.
- **"*MIT*" ≠ "public domain".** The copyright notice and permission text must travel.
- **A licence change is real and recent.** **Remix Icon moved off Apache-2.0** to a custom "Remix
  Icon License v1.0" dated January 2026 (**MEASURED**, its `License` file). An agent that "knows"
  Remix Icon is Apache-2.0 is now **wrong**, and — more dangerously — that licence **§3.3 forbids
  using the icons as an app icon**. Any older blog that calls Remix Icon "Apache-2.0" predates the
  change. Copy-paste guidance about icon licences is the failure mode this table exists to catch.

### 1.5 Licence facts not verified from the licence text in this run

Stated plainly, because a wrong licence claim is worse than an absent one:

| Fact | What was read | Status |
|---|---|---|
| Font Awesome **Pro** full contract | Only the licence page's opening clauses (the page rendered as ads first) and the seat/Medium summaries | **UNVERIFIED (full text)** — treated as proprietary, which is the safe direction |
| `material-icons-extended`'s own bundled licence notice | Not applicable — it is a Maven artifact, not a download page | n/a |
| CC BY 4.0 deed read? | FA's `LICENSE.txt` states the obligation; the CC BY **deed** itself was not fetched (only NC and SA deeds were) | **ON RECORD** (FA's own statement is MEASURED; the CC deed is linked but not read) |
| GPL from gnu.org | `gnu.org` returned **HTTP 403**; the GPL-3.0 preamble was read via OSI's mirror | **MEASURED (via OSI)** — canonical URL linked |
| Remix Icon being Apache-2.0 historically | No; the current `License` file is the custom one | Any claim it is Apache-2.0 is **UNVERIFIED/outdated** |
| `Apache-2.0` `NOTICE` file in `google/material-design-icons` | 404 at the repo root | MEASURED (does not exist) |

Everything else in the table was read from the linked licence file in this run.

---

## 2. What professional Android work actually uses

### 2.1 Google's own answer for a Material 3 app: Material Symbols, vendored

For a Material-3 Compose app, the current authoritative recommendation is **not** a Gradle icon
dependency at all. From Android's Compose-icons reference, updated **2026-08-06**:

> *"This library is no longer recommended for displaying Material Icons in Compose. Material Symbols
> are the new way forward, and we have stopped publishing updates to this library. **It has been
> removed from the latest Material 3 library release**, but you can still manually reference it if
> you cannot migrate yet. Instead, we recommend downloading **Vector Drawable XML files from the
> 'Android' tab on Google Fonts** to get the latest Material Symbols."*
> — https://developer.android.com/reference/kotlin/androidx/compose/material/icons/package-summary
> (**MEASURED**)

And from the Compose images guide, updated **2026-08-14**:

> *"Material Symbols (New) and Material Icons (`material-icons`)… this artifact is no longer
> maintained or recommended for use in your apps, as it contains an older look and feel and can also
> increase the build time of your apps significantly."*
> — https://developer.android.com/develop/ui/compose/graphics/images/material (**MEASURED**)

### 2.2 `material-icons-extended` / `material-icons-core`: deprecated, frozen, removed

The status, verified from primary sources rather than recalled:

- **Both artifacts stop shipping after Compose 1.7.0 stable.** AOSP's own change *"Remove
  material-icons-* projects"* (Bug 330712350) carries the release note: *"material-icons-core and
  material-icons-extended will stop shipping after Compose 1.7.0 stable. Developers should continue
  using 1.7.0 versions even after the rest of compose moves forward in versions"*
  (**MEASURED**,
  [alhia/androidx commit fda069f](https://github.com/alhia/androidx/commit/fda069fe217246933ccb671d5f34f4eff803f275)).
- **The last published version is 1.7.3, and it is pinned there.** Compose Multiplatform's
  deprecation-message fix states it directly: *"the `material-icons-extended` artifact is
  **intentionally pinned to version 1.7.3** and newer versions aren't published"*, and the `ReplaceWith`
  is `"org.jetbrains.compose.material:material-icons-extended:1.7.3"`
  (**MEASURED**,
  [JetBrains/compose-multiplatform commit c0eb527](https://github.com/JetBrains/compose-multiplatform/commit/c0eb5279ac7db74b1c1dcfcfbe049ef68d619c4f)).
- **It has been removed from the latest Material 3 library release** and is no longer published for
  current Compose (**MEASURED**, developer.android.com package-summary above).

**Consequence for this factory.** Our pinned BOM is **2026.09.00**, i.e. far past Compose 1.7.x, so
`material-icons-extended` **cannot be resolved from the current BOM** and would have to be pulled at
its frozen `1.7.3` — a stale artifact beside a current Compose runtime. **The recommended replacement
is Material Symbols vendored as vector resources**, exactly what Google's docs now say: *"download
Vector Drawable XML files from the 'Android' tab on Google Fonts"*, used through
`Icon(painter = painterResource(R.drawable.ic_add), …)` or generated `ImageVector`s
(**MEASURED**, package-summary; Compose images guide).

### 2.3 What appears in shipping apps

- **Material Symbols / Material Icons** dominate Android because they are the platform's own
  vocabulary and ship at 24 dp on a keyline grid; Google's Image Asset Studio even defaults to
  Material icons (**ON RECORD**,
  [developer.android.com/studio/write/create-app-icons](https://developer.android.com/studio/write/create-app-icons)).
- **Material Symbols is drawn as a variable font first** — a single file with `FILL`, `wght`,
  `GRAD`, `opsz` axes — with static SVG/vector-drawable and font outputs also published
  (**MEASURED**, [Material Symbols guide](https://developers.google.com/fonts/docs/material_symbols)
  and Google's README). It is the only listed set that is simultaneously a **font** and a
  **vector-drawable** set, which is why §3 treats it as two integration paths.
- The third-party permissive sets (Lucide, Phosphor, Tabler, Heroicons) are mostly **web-first**
  and are used on Android by vendoring their SVGs; none of them ships an Android `ImageVector`
  artifact as the primary distribution (**INFERRED** from their repositories, which publish SVG
  source, not Maven artifacts).

---

## 3. Technical integration paths, with trade-offs and APK-size cost

Four paths, ordered from the one this factory should **not** take to the one it should.

### 3a. `material-icons-extended` (or `material-icons-core`) as a Gradle dependency — **do not**

- **Status:** deprecated, frozen at `1.7.3`, removed from current Material 3 (§2.2). It is not a
  candidate in this factory's BOM.
- **Size:** famously large. The artifact was reported at **~18 MB AAR** as early as
  `1.0.0-alpha10`, and a transitive pull was measured adding **~20 MB** to an APK's size
  (**ON RECORD**,
  [SO: icon set / dependency size](https://stackoverflow.com/questions/65665563/android-jetpack-compose-icons-doesnt-contain-some-of-the-material-icons),
  [compose-material-dialogs issue #153](https://github.com/vanpra/compose-material-dialogs/issues/153)).
  Google's own note: *"Due to the very large size of this library, make sure to use R8/Proguard to
  strip unused icons… Alternatively you can make a local copy."*
- **R8 behaviour:** with `isMinifyEnabled = true` (R8 full mode is our default) unused icon classes
  are normally tree-shaken out of the **release** artifact; the debug artifact and build time still
  pay the cost (**ON RECORD**, same sources). But relying on the shrinker for a set we no longer
  want, at a version the toolchain no longer publishes, is worse than not adding it.
- **Verdict:** **not usable here.** It is not in the BOM, it is deprecated, and it is the exact
  "every Compose app looks the same" dependency the visual-identity rule was written against.

### 3b. Material Symbols as a **variable font**, drawn as text glyphs

- **How:** place the `.ttf` in `res/font/`, build a `FontFamily` with `FontVariation.Settings`
  (`FILL`, `opsz`, `GRAD`, `wght`), and render the icon **name** as a `Text`; the font's
  **ligatures** replace the word with the glyph. Compose supports this today:
  `FontVariation.Setting("FILL", 0f)` etc., and **variable fonts require API 26+** — which matches
  our `minSdk 26` exactly (**MEASURED**,
  [Compose fonts guide](https://developer.android.com/develop/ui/compose/text/fonts),
  [FontVariation](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/font/FontVariation),
  [Material Symbols guide](https://developers.google.com/fonts/docs/material_symbols)).
- **Size:** the full variable font is huge (**7.9 MB** for all axes, **295 KB** for the full static
  set, per Google's own payload table), but Google's recommended path **subsets** it: with
  `&icon_names=home,palette,settings` the payload is **1.7 KB** (**MEASURED**, Material Symbols
  guide). The subset is what a slot would commit (one font file with the ~20 icons it uses).
- **Accessibility / sizing caveats (the reason this is not the factory default):**
  - **The ligature name is the text.** A screen reader that does not apply the font substitution
    reads `arrow_forward` aloud unless the composable is marked decorative or given an explicit
    `contentDescription`. An icon font is more fragile than a `VectorDrawable` here, because the
    accessible name and the render are the same string.
  - **Icons scale with font scale.** A text glyph follows the user's font-size setting, so at 200 %
    an "icon" grows like text — which may be undesirable for a chrome glyph but is *correct* for an
    inline symbol. It must be a deliberate decision per call site.
  - **Baseline / clipping.** Glyphs sit on a text baseline with `lineHeight`; centring a glyph in a
    button needs explicit alignment and the `line-height: 1` trick the web guidance uses
    (**MEASURED**, Material Symbols guide's self-hosting CSS).
  - **`FILL` animation** is available (0→1) — genuinely useful for selected/unselected states, and
    a real advantage this path has over vendored vectors.
- **Verdict:** viable and attractive for a slot that genuinely wants `FILL` animation and matches
  glyphs to its typeface grade; **not** the safest default because of the accessibility coupling.

### 3c. Vendoring SVG → `VectorDrawable` (or `ImageVector`), committed to the slot — **the default**

- **How:** fetch a per-icon SVG once (Material Symbols "Android" tab, or a set's GitHub `svg/`
  folder), convert to `res/drawable/ic_<name>.xml` via Android Studio's **Vector Asset** importer,
  commit the XML, and consume it with `Icon(painter = painterResource(R.drawable.ic_…))`. For a
  Compose-native alternative, commit generated `ImageVector` Kotlin.
- **Conversion constraints, MEASURED** (`developer.android.com/studio/write/vector-asset-studio`):
  `VectorDrawable` supports "all features from Tiny SVG 1.2 **except for text**". **Not supported:**
  text, filter effects (drop shadows, blurs), pattern fills, `<defs>`, `<use>`, masks, dashed
  strokes, blend modes, SVG animation. Material Symbols' exported SVGs are path-only and convert
  cleanly; Feather/Lucide/Heroicons/Tabler are stroke paths and also convert, but a set that
  *relies* on `<defs>`/`<use>` for its shared geometry needs flattening first.
- **Size:** one `VectorDrawable` per used icon is a few hundred bytes to a few KB of XML; the APK
  cost is the sum of the icons actually used, with no unused-set ballast and no shrinker
  dependence. This is the smallest of the four paths for a known icon list. `VectorDrawable` also
  has a one-bitmap-cache-per-drawable nuance — *"if a VectorDrawable is used for different sizes, it
  is more efficient to create multiple VectorDrawables"* (**MEASURED**,
  [VectorDrawable reference](https://developer.android.com/reference/android/graphics/drawable/VectorDrawable)).
- **Verdict:** **the factory default.** It makes the identity audit-able (the bytes are in the
  repo), the licence obligable (the licence text travels with them), the build offline, and the
  provenance recordable per icon.

### 3d. Runtime SVG / vector rendering libraries — **not justified**

A runtime SVG parser/renderer adds a dependency, a runtime parse cost, a new attack surface, and no
capability the build-time `VectorDrawable` conversion lacks — while also making the licence and
provenance harder to trace (assets loaded at runtime are not in the APK to audit). `minSdk 26`
makes `VectorDrawable` universal, so there is nothing to shim. (**INFERRED** from the platform
capability in §3c; no such library is recommended.)

### 3e. Comparison summary

| Path | Deprecated? | APK cost | Offline build | Licence traceability | Verdict |
|---|---|---|---|---|---|
| `material-icons-extended` | **Yes** (frozen 1.7.3) | ~18 MB pre-shrink; shrunk in release | Gradle fetch | transitively via POM | **No** |
| Material Symbols variable font | Current | 1.7 KB if subset | if the `.ttf` is committed | repo | Viable, accessibility caveats |
| **Vendored SVG → VectorDrawable** | Current | per-icon KB | **yes** | **per-icon, in-repo** | **Default** |
| Runtime SVG library | n/a | dependency + runtime | no (assets may be remote) | poor | No |

---

## 4. Reproducibility and offline builds

**Principle (this repository's own rule, restated because it decides the mechanism):** a factory
process writes nothing outside the repository, and **a build must not depend on the network**
(`.cursor/rules/workspace-hygiene.mdc`; `AGENTS.md` "Working rules"). Applied to icons, the
mechanism is one sentence:

> **Icons are fetched once, during development, and committed — with their licence text and a
> provenance record. They are never fetched at build time and never at runtime.**

Concretely:

- The SVG is downloaded **by hand or by an agent during development**, converted, and the resulting
  `res/drawable/*.xml` is committed. The download is not part of `gradlew` or any script that runs
  in CI or on a fresh clone.
- The variable-font path commits the **subset `.ttf`** into `res/font/`, not a Google Fonts URL.
- No `@font-face` from a remote host, no `Downloadable Fonts` provider fetch for icon glyphs, no
  `androidx.compose.ui.text.googlefonts` download at runtime — those are **network at runtime** and
  violate the offline principle for the same reason a build-time fetch does.
- **What would silently fetch at build time, and must not be used:**
  - a **Gradle dependency** on an icon artifact (Maven resolution is a network fetch on a cold
    cache — normal for libraries, but it is exactly why the icon bytes should be in `res/`);
  - a **Gradle plugin or task** that downloads icons (e.g. an "icon generator" plugin);
  - an **npm/fontsource-style package** consumed by a build step;
  - a **`google-services`/provider SDK** or an icon API wrapped in a generated source task.
- **Dependencies are not the same as assets.** Pulling `androidx.compose.*` from `google()` is the
  factory's normal, cached route and is not what this rule targets; the rule targets **asset bytes**
  that would otherwise be non-reproducible. A vendored icon has a committed hash; a Gradle-resolved
  icon artifact does not (only a coordinate that can be re-published).
- **`.kiln/scratch/` only.** Any temporary file used while fetching/converting an icon (a raw SVG,
  a PNG preview, a download log) goes under `.kiln/scratch/`, never `%TEMP%`/`%TMP%`/`$TMPDIR`
  (`.cursor/rules/workspace-hygiene.mdc`). The committed output lives under the slot, not in
  scratch.

---

## 5. Governance and provenance

### 5.1 What a slot must record

So that the licence obligation is auditable and a later reader can honour attribution, a slot that
vendors icons records **which set, which version, which licence, and which individual icons** — as
a file, not a comment. Recommended shape (a proposal for the owner; nothing was created):

```text
apps/<slot>/icons/
  provenance.json          # the machine-readable record (below)
  LICENSES/
    Apache-2.0.txt         # verbatim licence text of every set used
    MIT.txt
    OFL-1.1.txt
    CC-BY-4.0.txt
```

`apps/<slot>/icons/provenance.json`:

```json
{
  "sets": [
    {
      "library": "Material Symbols",
      "version": "2.966",
      "spdx": "Apache-2.0",
      "sourceUrl": "https://github.com/google/material-design-icons",
      "licenceFile": "apps/<slot>/icons/LICENSES/Apache-2.0.txt",
      "fetchedAt": "2026-10-02",
      "fetchedBy": "agent",
      "icons": [
        {
          "name": "add",
          "file": "app/src/main/res/drawable/ic_add.xml",
          "sha256": "<hash of the committed XML>",
          "sourcePath": "symbols/android/material_symbols/add/materialsymbolsoutlined/add_24px.svg"
        }
      ]
    }
  ]
}
```

This mirrors the factory's existing `store/provenance.json` idea (a committed record binding
committed bytes to a source) and the `differentiators.json` / `economics.json` convention of
"the decision is a build input with a schema". A check could later assert: every
`res/drawable/ic_*.xml` appears in some `sets[].icons[].file`; every `spdx` is on the allowlist;
every `licenceFile` exists. (A sensor is **not** proposed here as code — this document is research.)

### 5.2 What Google Play requires or forbids about bundled licensed assets

- **Play does not require an open-source-licence screen** in the app, and does not police icon
  licences. It *does* police **metadata honesty and IP**: *"misleading symbols in app icons are not
  allowed"*, *"Don't use text or image… that suggests relations to existing Google Play programs"*,
  and the IP/Impersonation policy forbids graphics *"identical or so similar to existing products or
  services that users may be [misled]"* (**MEASURED**, Metadata policy
  https://support.google.com/googleplay/android-developer/answer/9898842; best practices
  https://support.google.com/googleplay/android-developer/answer/13393723). The practical read:
  **a stock icon in the store icon is a metadata risk** (§6); a stock icon *inside* the UI is not.
- **Google's own recommendation for open-source notices** is tooling, not a policy: the
  `oss-licenses-plugin` + `play-services-oss-licenses` library collects dependency licences and
  shows them in `OssLicensesMenuActivity` (**MEASURED/ON RECORD**,
  https://developers.google.com/android/guides/opensource). That plugin covers **Maven dependencies**
  it can read a POM for — it will **not** cover hand-vendored SVG files, which is precisely why the
  vendored path needs its own `provenance.json` + `LICENSES/` (§5.1) rather than leaning on the
  plugin.
- **Practical attribution-text location.** For the sets that *do* require attribution (CC BY and
  some proprietary free tiers), the credit belongs where users already expect licences: the app's
  **About / Settings → "Licences"** surface (the same destination the OSS-licenses activity would
  populate), and, where the licence asks for a link, beside the pack's own copy. For the
  permissive sets (MIT/ISC/Apache-2.0) the obligation is satisfied by the **licence text in the
  repository** and reproduced for recipients; an on-screen credit is courteous, not required.
  Non-translatable attribution strings still belong in resources, per
  `.cursor/rules/localization.mdc`.

---

## 6. What must stay hand-made

**The distinction is role, not pixels.** A **brand mark** is the app's identity — it must be unique
to the slot, recognisable at 48 dp, and survive the platform's masks. **UI iconography** is the
app's vocabulary — it should be conventional, because Convention is what makes a bin icon mean
"delete" to every user.

### 6.1 The launcher icon / brand mark — **must not be a stock library glyph**

Sources:

- Google Play's own icon specification: the store icon must be a distinct, simple silhouette that
  reads at small sizes, must not be "a cropped view of a larger image", and must not be "similar to
  those of existing products or services" (**MEASURED**,
  [Play icon design specs](https://developer.android.com/distribute/google-play/resources/icon-design-specifications),
  [Best practices](https://support.google.com/googleplay/android-developer/answer/13393723)).
- Android's launcher-icon guidance: *"launcher icons should differentiate your application from
  others"* and *"Icons should not be cropped. Use unique shapes where appropriate"* (**MEASURED**,
  AOSP `icon_design_launcher.jd`).
- The factory's own rule already says this: the mark is "generated from `studio.json`'s `mark`…
  drawn, not borrowed" (`.cursor/rules/visual-identity.mdc`).

**Why hundreds of apps sharing a stock mark is a problem:** if two apps ship the same glyph as their
launcher icon, a user cannot tell them apart on the home screen, Play cannot, and the "so similar
that users may be misled" clause of the metadata policy is in play. A stock UI icon is a *word in a
shared vocabulary*; a stock launcher icon is *the same face as everyone else*.

Some sets **explicitly forbid** this, which turns a design preference into a licence term: Remix
Icon §3.3, *"You may NOT use the Icons… as a logo, trademark, or brand identifier… app icon, or
identity symbol"* (**MEASURED**). Font Awesome separately warns that *"brand logos"* may only be
used *"to represent the company, product, or service to which they refer"* (**MEASURED**,
`LICENSE.txt`).

### 6.2 The Android 13+ monochrome / themed icon — **must be the slot's own mark**

Themed icons require an adaptive icon plus a `monochrome` layer; *"The layers must not have masks or
background shadows"*, *"Size all layers to 108×108 dp"*, the logo *"must not exceed 66×66 dp"*, and
it is tinted by the system from the user's wallpaper (**MEASURED**,
[Adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive),
[Android 13 features](https://developer.android.com/about/versions/13/features)). A generic stock
glyph here produces a monochrome "the same as every app that used that glyph" tile. This is the
slot's mark, in one colour — not a library icon.

### 6.3 Anything that must read as *this* app

- The **mark glyph** inside the app (the logo/hero mark) and any **hero/identity illustration** are
  the slot's own, because they *are* the identity the visual-identity rule protects.
- **Stat/hero panels, section strips, status pills, brand wordmarks** are slot components — a stock
  set cannot supply them, and §"the slot owns components" of the rule still applies.
- **A glyph whose meaning the app defines** ("mowed", "rained off", a domain noun) may have no
  library equivalent. If the set has no honest match, the correct outcome is a **slot-owned vector**
  in the same visual grammar as the library (same stroke weight, same grid) — not a library icon
  bent to mean something it does not.

**The one-line test:** if the icon would be the same in a competitor's app, it is *UI iconography*
and belongs to the library; if it would be the same in no other app, it is *identity* and belongs to
the slot.

---

## 7. Quality criteria for choosing and using a set

Drawn from the Material Symbols design axes and the platform's own icon grid; each is a check a
reviewer can apply, and each is the difference between "drawn by one hand" and "assembled".

1. **One set per slot.** Two sets with different stroke weights mixed in one screen is the most
   visible way an iconography looks assembled. Pick one (plus the slot's own mark and any
   domain-only glyphs drawn to match).
2. **Stroke-weight consistency.** A family must share one stroke weight and one cap style. Material
   Symbols exposes this as the `wght` axis (100–700) and `GRAD` (−50–200) for fine-tuning against
   the type; a vendored set is frozen at whatever the SVG carries, so *choose* the weight variant
   rather than mixing the `regular` and `bold` files (**MEASURED**,
   [Material Symbols guide](https://developers.google.com/fonts/docs/material_symbols)). The
   grounding slot already got this right for its hand-drawn set with a single `GlyphStrokeRatio`;
   a vendored set gets the same rule from the outside (**MEASURED**, `Glyphs.kt:52`).
3. **Optical sizing / stroke thickness at size.** Material Symbols' `opsz` axis (20–48 dp) exists
   because *"the stroke weight (thickness) changes as the icon size scales"*; a fixed vector scaled
   from 24 dp to 48 dp looks too light or too heavy. If the slot draws the same symbol at two very
   different sizes, prefer the variable-font path (or two vendored variants) over naive scaling
   (**MEASURED**, Material Symbols guide; `opsz` is a native `FontVariation.Setting` per the Compose
   fonts guide).
4. **Filled vs outlined is a state, not a decoration.** Material Symbols' `FILL` axis exists to
   convey selected/unselected; using filled and outlined versions of the same icon for two
   unrelated meanings is a vocabulary defect (**MEASURED**, Material Symbols guide).
5. **24 dp grid discipline.** The icon convention the rest of a Material 3 app is laid out to is
   24 dp with the standard keylines; Material Symbols' designs are 24 px baselines and *"only the 20
   and 24 px versions are designed with perfect pixel-grid alignment"* (**MEASURED**, Google
   README). 24 dp is also what the grounding slot's `GlyphSize` uses (**MEASURED**, `Glyphs.kt:46`).
6. **The icon takes the app's tint; it does not carry its own colour.** Colour must come from the
   theme (`LocalContentColor` / `tint = MaterialTheme.colorScheme…`), never a hard-coded colour in
   the vector. This is the rule `Glyphs.kt` already states: *"Colour never lives here… the set
   inherits the slot's own theme and cannot drift from the palette"* (**MEASURED**, `Glyphs.kt:37-39`).
   A stock SVG with a baked fill breaks it.
7. **Corner radius and terminal consistency.** A set's corners (rounded vs sharp) are a family
   trait; Material Symbols ships Outlined / Rounded / Sharp as separate families for this reason —
   choose one that matches the slot's own shape language (`ui/theme/Shape.kt`).
8. **Accessibility is a call-site decision.** A decorative icon that repeats an adjacent label gets
   `contentDescription = null`; an icon that is the only content of a control needs an accessible
   name. Both `Icon` and `Image` must pass an explicit value, and an interactive target is at least
   48 dp — this is `uiux.a11y`'s existing finding, not a new rule
   (`.cursor/rules/ui-ux-baseline.mdc`). The grounding slot states the same principle
   (**MEASURED**, `Glyphs.kt:41-43`).
9. **What makes a set look amateurish when mixed.** Different stroke weights; a mix of filled and
   outlined with no state logic; mismatched corner radii; icons at inconsistent optical size; icons
   that carry their own colour beside theme-tinted ones; freestanding glyphs that read as a
   truncated or mis-aligned shape at 24 dp. All are symptoms of *assembling* instead of *choosing*.

---

## 8. Encodable rules

Numbered, machine-checkable assertions. Format: **(a) assertion — (b) threshold / bound or exact
shape — (c) source, with label.** None of these is implemented as a check yet; they are the
specification a future sensor (and a future amendment to `visual-identity.mdc`) would encode.

1. **Allowlist of bundleable licences.**
   (a) A vendored icon set or icon dependency must carry an SPDX identifier on the allowlist.
   (b) Allowlist, exact strings: `MIT`, `ISC`, `Apache-2.0`, `OFL-1.1`, `CC-BY-4.0`, plus
   `MIT-0` and `BSD-2-Clause`/`BSD-3-Clause` if ever used. A file with several (e.g. Lucide's
   `ISC` + `MIT`, Font Awesome Free's `CC-BY-4.0` + `OFL-1.1` + `MIT`) passes if **every** part is
   allowed and each is recorded.
   (c) §1 table, licence texts linked there — **MEASURED** (each licence file read).

2. **Denylist of licences that must never be bundled.**
   (a) A vendored set must not carry a denied identifier or term.
   (b) Denylist, exact/pattern: `CC-BY-NC-*` (any version), `CC-BY-NC-SA-*`, `CC-BY-SA-*`,
   `GPL-*`, `AGPL-*`, `LGPL-*`, `CC-BY-ND-*`; and **no SPDX id + a commercial/premium/attribution
   service term** (Font Awesome Pro, Streamline premium, Flaticon free, Icons8 free) —
   `NOASSERTION`/absent SPDX with a paid tier is a **failure**, not a note.
   (c) NC and SA deeds read (https://creativecommons.org/licenses/by-nc/4.0/ and
   `/by-sa/4.0/`), GPL-3.0 preamble read, FA/Streamline/Flaticon/Icons8 terms read — **MEASURED**;
   the *legal effect on an APK* — **UNVERIFIED**, which is why the rule fails closed.

3. **Every icon dependency or vendored icon carries a provenance record.**
   (a) Every icon used by the slot resolves to exactly one row in
   `apps/<slot>/icons/provenance.json` naming `library`, `version`, `spdx`, `sourceUrl`,
   `licenceFile`, and for vendored files a `sha256`.
   (b) Exact shape: `sets[].icons[]` with the fields in §5.1; an `res/drawable/ic_*.xml` or an
   `ImageVector` in the slot that is not listed is a **failure** ("orphan icon"); a listed file
   that does not exist is a failure; a `sha256` that does not match the committed file is a
   failure.
   (c) §5.1 (this document) + factory precedent `store/provenance.json` — **ON RECORD /
   INFERRED** (the file shape is a recommendation, not yet a contract).

4. **No icon is fetched during a build.**
   (a) No build input resolves an icon/icon-font/icon-asset over the network.
   (b) Failure shapes: an icon artifact declared in `gradle/libs.versions.toml`/`build.gradle.kts`;
   a Gradle plugin/task that downloads icons; a script under `scripts/` that fetches an icon or
   font at build time; a `googlefonts` / `@font-face` URL in shipped sources. Vendored bytes under
   `res/`/`ui/` are the pass shape.
   (c) `.cursor/rules/workspace-hygiene.mdc` + `AGENTS.md` "a build must not depend on the network"
   — **ON RECORD**; enforced today by no icon-specific check.

5. **A vendored set's licence text is present in the repository.**
   (a) For every distinct `spdx` in `provenance.json`, a verbatim licence file exists in the
   repository and is referenced by every row using it.
   (b) Exact shape: `apps/<slot>/icons/LICENSES/<SPDX>.txt` exists and is non-empty; for Apache-2.0
   the file is the Apache-2.0 text (§4(a)); if the upstream ships a `NOTICE`, that too.
   (c) Apache-2.0 §4(a)/§4(d) read (https://www.apache.org/licenses/LICENSE-2.0) — **MEASURED**;
   `google/material-design-icons` has no `NOTICE` (404) — **MEASURED**.

6. **UI icons take their tint from the theme, never a hard-coded colour.**
   (a) A UI icon draws in the theme's content/role colour and carries no literal colour.
   (b) Failure shapes: a `Color(0x…)`/`Color.Red`/hex literal inside a vendored `VectorDrawable`
   (`android:fillColor="#RRGGBB"` other than `#FF000000`-as-mask, or `<gradient>` with literal
   stops) or inside an `ImageVector`; an `Icon`/`Image` call that omits `tint`/sets it to a literal
   instead of `LocalContentColor`/`MaterialTheme.colorScheme.*`.
   (c) `.cursor/rules/visual-identity.mdc` (palette is one source of truth) and the grounding slot's
   own statement, `Glyphs.kt:37-39` — **MEASURED**.

7. **The slot-owned mark is distinct from library UI icons.**
   (a) The launcher icon, the adaptive foreground, the monochrome layer and any in-app brand mark
   come from the slot's `studio.json` `mark`, **not** from a library icon.
   (b) Failure shapes: the launcher `mipmap`/adaptive foreground or the `<monochrome>` drawable is
   byte-identical (or path-identical) to an icon listed in `provenance.json`; the mark is a known
   library glyph; a set whose licence forbids brand use (Remix Icon §3.3) is used as the mark.
   (c) Play icon spec + AOSP launcher guidance + Android 13 themed-icon requirements — **MEASURED**
   (§6.1, §6.2); Remix Icon §3.3 — **MEASURED**.

8. **The deprecated Material icon artifacts are not used.**
   (a) The slot does not depend on `androidx.compose.material:material-icons-core` or
   `:material-icons-extended` (nor their `org.jetbrains.compose.material` equivalents).
   (b) Failure shape: either coordinate in `gradle/libs.versions.toml` or any `build.gradle.kts`.
   (c) Deprecated, frozen at `1.7.3`, removed from current Material 3 — **MEASURED** (§2.2).

9. **One icon family per slot (no mixed sets).**
   (a) All library UI icons in a slot come from one `library` (one set), plus the slot's own mark
   and any slot-drawn glyph.
   (b) Failure shape: `provenance.json` names **two or more** library sets for UI iconography
   without a written justification field.
   (c) §7.1 (this document), §7.9 — **INFERRED** from the "looks assembled" failure and the
   visual-identity rule's per-slot identity — **ON RECORD**.

10. **Attribution is honoured where the licence requires it.**
    (a) If any vendored set requires attribution (e.g. `CC-BY-4.0`, or a proprietary-with-
    attribution free tier, which rule 2 already denies), the required credit is reachable from
    inside the app.
    (b) Exact shape: a "Licences"/"About" destination that lists each set with its licence and,
    where required, a link; the credit strings live in `values/` resources
    (`.cursor/rules/localization.mdc`).
    (c) CC BY deed (attribution + link) — **ON RECORD** (FA's MEASURED statement links it); Google's
    `oss-licenses` tooling as the recommended home —
    https://developers.google.com/android/guides/opensource — **MEASURED**.

11. **Vendored icons are Android-consumable vectors, not raw SVG.**
    (a) A committed UI icon is a `VectorDrawable` (`res/drawable/*.xml`) or a Compose
    `ImageVector`; no `.svg` file is shipped or parsed at runtime.
    (b) Failure shape: a `*.svg` under `app/src/main/`; a runtime SVG/vector rendering dependency
    added solely for icons.
    (c) `VectorDrawable` supports Tiny SVG 1.2 minus text; SVG import is a build-time conversion —
    **MEASURED** (§3c).

12. **The app's own `Icon`/`Image` usage already obeys `uiux.a11y`.**
    (a) Every icon carries an explicit `contentDescription` (a string or `null`); every interactive
    target is ≥ 48 dp.
    (b) Existing check id `uiux.a11y` (do not duplicate it).
    (c) `.cursor/rules/ui-ux-baseline.mdc` — **MEASURED** (existing rule); noted here so the icon
    work routes to it rather than restating it.

---

## 9. Verification status of this document

| Claim class | Status |
|---|---|
| Licence texts for Material Symbols/Icons, Lucide, Phosphor, Tabler, Heroicons, Feather, Remix Icon, Bootstrap Icons, Ionicons, Font Awesome Free, Iconoir, Majesticons | **MEASURED** — each file fetched and read |
| CC BY-NC-4.0 and CC BY-SA-4.0 terms | **MEASURED** — deeds read |
| GPL-3.0 copyleft | **MEASURED via OSI mirror** (gnu.org 403) |
| Flaticon / Icons8 / Streamline terms | **MEASURED** — help-centre/licence pages read |
| Font Awesome **Pro** contract | **PARTIAL** — opening clauses + secondary summaries; full text not read |
| CC BY 4.0 deed itself | **ON RECORD** — FA's own licence states the obligation; the deed was not fetched |
| `material-icons-extended` deprecation, pin `1.7.3`, removal | **MEASURED** — AOSP commit, JetBrains commit, developer.android.com |
| APK-size numbers (~18 MB AAR, ~20 MB transitive) | **ON RECORD** — community measurements, not re-measured here |
| "Professional Android apps use set X" prevalence | **INFERRED / ON RECORD** — no controlled survey; not load-bearing |
| Legal effect of copyleft icons on an APK | **UNVERIFIED** — counsel question; rule 2 fails closed |
| The `icons/provenance.json` shape and rules 1–12 as *code* | **Proposed, not implemented** — this is research |

Nothing in this document was implemented, added as a dependency, or downloaded into the repository.
The only file written is this one; its note on `store/provenance.json` is a citation, not a change.
