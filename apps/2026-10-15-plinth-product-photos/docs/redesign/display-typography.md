# W33: opt-in display typography

## Scope and adoption

`app/src/main/java/com/gexetr/plinthproductphotos/ui/theme/Type.kt` adds bundled
Noto Serif Display SemiBold to `displayLarge` (57/64 sp) and `headlineLarge`
(32/40 sp). Both use zero letter spacing and natural, proportional numerals.
These styles are for editorial hero/result prose, not prices or counters.
All existing styles, including `displaySmall` and `headlineMedium`, are unchanged;
body, titles, labels and existing tabular-numeric settings remain sans-serif.
No palette roles, dependencies or runtime font providers change.

HomeScreen already had uncommitted changes when this worker started, so the
authorised Type.kt/assets-only fallback is used. No screen adopts the new styles
in W33. This intentionally prepares typography rather than claiming a visible
redesign.

Later adoption, after the relevant screen owner reviews the layout:

- Home's `StudioHero`: use `headlineLarge` for a future editorial hero title,
  not the import/continue button labels. The current hero has no title Text.
- A selected redesigned result screen: use `headlineLarge` for its result prose
  heading, or `displayLarge` only where a large hero has room. Keep balance,
  price, dimensions, progress and other numeric/UI text in current sans styles.

Do not change the global `PlinthTopBar` title or shared `headlineMedium`/
`headlineSmall` styles as a shortcut: that would restyle unrelated destinations.
Keep wrapping/font scaling; review English and Russian on small screens before
adopting either display style. Runtime layout and appearance remain UNVERIFIED.

## Font provenance and licence

Source: Google Fonts `google/fonts`, pinned commit
`47d54ca1ebb14fd864695f1d39dc734f39017c33`, family version 2.003.

- [Source TTF](https://raw.githubusercontent.com/google/fonts/47d54ca1ebb14fd864695f1d39dc734f39017c33/ofl/notoserifdisplay/NotoSerifDisplay%5Bwdth,wght%5D.ttf)
- [Family OFL](https://raw.githubusercontent.com/google/fonts/47d54ca1ebb14fd864695f1d39dc734f39017c33/ofl/notoserifdisplay/OFL.txt)
- Source SHA-256: `4b661adcf6a5d38bf5befdc4d976e8a7238a4aeb57e62c1c1a67775380a09df9`
- Bundled `app/src/main/res/font/noto_serif_display_semibold.ttf`: **435,940 bytes**.
- Bundled SHA-256: `29ac4b132be2385284e4996708118ec68fd472b1e3904f5697bc39e5da650644`

The asset is a derived static instance at `wght=600`, `wdth=100`, without
subsetting. This avoids variable-axis configuration and uses one real weight
rather than synthesizing bold. FontTools instantiates the source; naming records
identify SemiBold explicitly because this source has no STAT axis values for
automatic name updating. Outlines, character coverage and layout tables come
from the pinned source. No Maven dependency was added.

Licence: SIL Open Font License 1.1. The exact family licence is retained at
`docs/NotoSerifDisplay-OFL.txt`. Its entire text, including the copyright notice,
is also embedded in the bundled TTF's name-ID 13 metadata, so the licence travels
inside the font under `res/font/` and into the app, rather than relying on a
docs-only notice or an unsupported plain-text font resource. It can be read with
FontTools (`TTFont(path)["name"].getDebugName(13)`). The original name-ID 0
copyright (2016 Google Inc.) is preserved; the upstream licence separately
states 2012 Google Inc. Neither notice is replaced. The family OFL names no
Reserved Font Name.

## Observed checks and limits

The scratch inspection command, from the repository root:

```powershell
$env:PYTHONPATH = (Resolve-Path .kiln\scratch\w33-typography\tools).Path
$env:PYTHONDONTWRITEBYTECODE = '1'
python .kiln\scratch\w33-typography\check_font.py
```

The inspector checks the actual bundled font's cmap for printable ASCII,
the complete Russian alphabet including both forms of Yo, and Cyrillic
characters found in `values-ru*/strings.xml`. It also checks static 600/normal
width metadata, the complete embedded OFL, source copyright preservation and
the standalone licence copy. Snapshot comparisons protect Color.kt, Theme.kt,
HomeScreen.kt and every previously declared typography style.
The missing-font check failed before the asset was generated.

No Gradle, Android compilation, emulator/device run or screenshot capture was
performed. **Comp UNVERIFIED.** The orchestrator must compile and review the
eventual adopted headings on-device; font coverage alone does not prove shaping,
wrapping, accessibility or visual quality.
