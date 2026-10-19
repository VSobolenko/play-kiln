# W10 - Theme completion

## Scope and evidence

Only `ui/theme/Color.kt`, `ui/theme/Theme.kt`, this report and repository-local
scratch artifacts were changed by W10. Existing uncommitted `DividerLight`,
`DividerDark`, `outlineVariant` and `surfaceTint` assignments were retained.
No screens, layouts, store identity, typography, approval records or Git index
were edited. No commit was made. **Gradle was not run; I did not compile.**

Measurements below came from these commands, run from the repository root:

```powershell
node .kiln\scratch\plinth-role-gap.mjs
python .kiln\scratch\plinth-contrast.py
python .kiln\scratch\w10-theme-audit.py
node scripts\verify.mjs --slot plinth-product-photos
```

The first scanner reports the intersection of UI references and theme
assignments, not the total number of assigned scheme roles. Its initial
**13 defined roles** means 13 of the 17 referenced roles. The four missing roles
accounted for **67 reference sites**, not 67 distinct roles.

The W10 audit reads the actual Kotlin literals and both scheme blocks, checks
each scheme separately, checks identity against `store/studio.json`, recomputes
the derived constants and measures WCAG sRGB contrast. It does not inspect
rendered pixels, alpha-modified colours or Material You's opt-in dynamic scheme.

## Role gap: before and after

Before, observed output (location details omitted):

```text
=== ROLES THE UI USES: 17 ===
--- DEFINED by the theme (13) ---
background, onBackground, onPrimary, onSecondaryContainer, onSurface, onSurfaceVariant, outlineVariant, primary, primaryContainer, secondaryContainer, surface, surfaceVariant, tertiary

--- *** USED BUT NOT DEFINED *** (4) ---
  error  (54 site(s))
  surfaceContainer  (7 site(s))
  surfaceContainerHigh  (4 site(s))
  surfaceContainerHighest  (2 site(s))
```

After, observed output:

```text
=== ROLES THE UI USES: 17 ===
--- DEFINED by the theme (17) ---
background, error, onBackground, onPrimary, onSecondaryContainer, onSurface, onSurfaceVariant, outlineVariant, primary, primaryContainer, secondaryContainer, surface, surfaceContainer, surfaceContainerHigh, surfaceContainerHighest, surfaceVariant, tertiary

--- *** USED BUT NOT DEFINED *** (0) ---
```

The independent per-scheme audit observed **36 explicit assignments in light and
36 in dark**, with identical role sets and no unresolved constants. This closes
every currently scanned UI role, not a claim that every future Material API
role (for example the unused fixed-accent family) has been overridden.

## Before/after assignment and contrast table

All ratios are `:1`, rounded to three decimals only for display. Assertions use
the unrounded value. L/D means light/dark. "Vs surface" compares the row's colour
to `surface`: `Paper #E8E1D5` in light, `InkSoft #303637` in dark. It measures
separation, not a requirement that a surface fill contrast 3:1 with another fill.
`surfaceContainer*`, `surfaceDim` and `surfaceBright` share `onSurface`; there is
no separate M3 `onSurfaceContainer*` role.

| Role | Before L / D | Constant and value L | Constant and value D | On/paired role | Pair contrast L / D | Vs surface L / D |
|---|---|---|---|---|---|---|
| `error` | unset / unset | `Ink #212627` | `Paper #E8E1D5` | `onError`: Paper / Ink | 11.787 / 11.787 | 11.787 / 9.460 |
| `onError` | unset / unset | `Paper #E8E1D5` | `Ink #212627` | `error` | 11.787 / 11.787 | 1.000 / 1.246 |
| `errorContainer` | unset / unset | `Ink #212627` | `Paper #E8E1D5` | `onErrorContainer`: Paper / Ink | 11.787 / 11.787 | 11.787 / 9.460 |
| `onErrorContainer` | unset / unset | `Paper #E8E1D5` | `Ink #212627` | `errorContainer` | 11.787 / 11.787 | 1.000 / 1.246 |
| `surfaceContainerLowest` | unset / unset | `Paper #E8E1D5` | `Ink #212627` | `onSurface`: Ink / Paper | 11.787 / 11.787 | 1.000 / 1.246 |
| `surfaceContainerLow` | unset / unset | `Paper #E8E1D5` | `ContainerLowDark #363C3D` | `onSurface` | 11.787 / 8.640 | 1.000 / 1.095 |
| `surfaceContainer` | unset / unset | `BenchLight #E4D4C6` | `ContainerDark #3C4243` | `onSurface` | 10.598 / 7.874 | 1.112 / 1.201 |
| `surfaceContainerHigh` | unset / unset | `BenchLight #E4D4C6` | `InkLift #424848` | `onSurface` | 10.598 / 7.176 | 1.112 / 1.318 |
| `surfaceContainerHighest` | unset / unset | `ClayPale #E0C8B9` | `RaisedDark #575B58` | `onSurface` | 9.577 / 5.312 | 1.231 / 1.781 |
| `surfaceDim` | unset / unset | `ClayPale #E0C8B9` | `Ink #212627` | `onSurface` | 9.577 / 11.787 | 1.231 / 1.246 |
| `surfaceBright` | unset / unset | `Paper #E8E1D5` | `RaisedDark #575B58` | `onSurface` | 11.787 / 5.312 | 1.000 / 1.781 |
| `surfaceVariant` | Paper / InkLift | `BenchLight #E4D4C6` | `InkLift #424848` | `onSurfaceVariant`: SecondaryTextLight / Paper | 6.139 / 7.176 | 1.112 / 1.318 |
| `onSurfaceVariant` | Muted / Paper | `SecondaryTextLight #4B4A46` | `Paper #E8E1D5` | `surfaceVariant` | 6.139 / 7.176 | 6.828 / 9.460 |
| `outline` | Muted / BezelRim | `Muted #625E56` | `BoundaryDark #B4B0A5` | no on role; boundary vs surface | 4.966 / 5.674 | 4.966 / 5.674 |
| `inversePrimary` | unset / unset | `ClayPale #E0C8B9` | `VerdigrisDeep #315B54` | `inverseSurface`: Ink / Paper | 9.577 / 5.866 | 1.231 / 1.613 |
| `scrim` | unset / unset | `Bezel #080B0C` | `Bezel #080B0C` | no on role | n/a | 15.197 / 1.606 |

Unchanged assignments and their paired measurements:

| Role / on role | Constants and values L | Constants and values D | Pair contrast L / D | Fill vs surface L / D |
|---|---|---|---|---|
| `primary` / `onPrimary` | VerdigrisDeep #315B54 / Paper #E8E1D5 | Verdigris #668A81 / Bezel #080B0C | 5.866 / 5.188 | 5.866 / 3.229 |
| `primaryContainer` / `onPrimaryContainer` | Verdigris #668A81 / Bezel #080B0C | VerdigrisDeep #315B54 / Paper #E8E1D5 | 5.188 / 5.866 | 2.929 / 1.613 |
| `secondary`, `tertiary` / their on roles | ClayDeep #62483D / Paper #E8E1D5 | ClayPale #E0C8B9 / Ink #212627 | 6.430 / 9.577 | 6.430 / 7.686 |
| `secondaryContainer`, `tertiaryContainer` / their on roles | ClayPale #E0C8B9 / Ink #212627 | ClayDeep #62483D / ClayPale #E0C8B9 | 9.577 / 5.224 | 1.231 / 1.471 |
| `background` / `onBackground` | Paper #E8E1D5 / Ink #212627 | Ink #212627 / Paper #E8E1D5 | 11.787 / 11.787 | 1.000 / 1.246 |
| `surface` / `onSurface` | Paper #E8E1D5 / Ink #212627 | InkSoft #303637 / Paper #E8E1D5 | 11.787 / 9.460 | 1.000 / 1.000 |
| `inverseSurface` / `inverseOnSurface` | Ink #212627 / Paper #E8E1D5 | Paper #E8E1D5 / Ink #212627 | 11.787 / 11.787 | 11.787 / 9.460 |
| `surfaceTint` (no on role) | Paper #E8E1D5 | InkSoft #303637 | n/a | 1.000 / 1.000 |
| `outlineVariant` (no on role; decorative divider only) | DividerLight #C2BCB1 | DividerDark #50524E | n/a | 1.453 / 1.556 |

The low contrast of a divider or fill is not a text contrast pass. Conversely,
an `onPrimary` colour is measured on `primary`, not as body text on `surface`.
The audit's raw table also prints the surface comparison for every on role.

## Derived tokens, not a new palette

All 12 `store/studio.json` identity colours remain byte-for-byte present in
`Color.kt`; the studio record was not edited. The six new constants are rounded
per-channel sRGB interpolations of those colours:

| Constant | Derivation | Result |
|---|---|---|
| `BenchLight` | Paper -> ClayPale, 0.52 | #E4D4C6 |
| `ContainerLowDark` | InkSoft -> InkLift, 0.34 | #363C3D |
| `ContainerDark` | InkSoft -> InkLift, 0.68 | #3C4243 |
| `RaisedDark` | InkLift -> BezelRim, 0.30 | #575B58 |
| `SecondaryTextLight` | Muted -> Ink, 0.35 | #4B4A46 |
| `BoundaryDark` | BezelRim -> Paper, 0.45 | #B4B0A5 |

The existing divider tokens also recomputed correctly: Muted -> Paper at 0.72
is #C2BCB1; BezelRim -> Ink at 0.55 is #50524E. Literal constants keep the
factory's static colour parser able to resolve every assignment; derivation
checks ensure the literals are not arbitrary extra palette choices.

## Whole-pass contrast and rejected alternatives

| Surface | Secondary text L | Secondary text D | Outline L | Outline D |
|---|---:|---:|---:|---:|
| `background` | 6.828 | 11.787 | 4.966 | 7.070 |
| `surface` | 6.828 | 9.460 | 4.966 | 5.674 |
| `surfaceContainerLowest` | 6.828 | 11.787 | 4.966 | 7.070 |
| `surfaceContainerLow` | 6.828 | 8.640 | 4.966 | 5.182 |
| `surfaceContainer` | 6.139 | 7.874 | 4.465 | 4.723 |
| `surfaceContainerHigh`, `surfaceVariant` | 6.139 | 7.176 | 4.465 | 4.304 |
| `surfaceContainerHighest` | 5.548 | 5.312 | 4.035 | 3.186 |
| `surfaceDim` | 5.548 | 11.787 | 4.035 | 7.070 |
| `surfaceBright` | 6.828 | 5.312 | 4.966 | 3.186 |

Decisions against `ai-visual-language.md` section 5:

- Rejected setting only the surface ladder. The research tool reproduced
  Muted/Paper **4.966**, Muted/bench **4.465**, and Muted/ClayPale **4.035**.
  Set the global light `onSurfaceVariant` to the proposed raised ink
  #4B4A46, rather than adding a custom token requiring screen edits.
  That measures **6.828 / 6.139 / 5.548** on page/bench/raised.
- Rejected dark secondary text #BDB8AE for this complete ladder. The proposed
  colour measures only **3.494** against #575B58. Retained existing `Paper`
  globally: its minimum on the ladder is **5.312**.
- Rejected the proposed dark outline #A6A298: **3.660** on InkLift, but only
  **2.709** on RaisedDark. Interpolating BezelRim -> Paper at 0.45 instead
  measures **4.304** on InkLift and **3.186** on RaisedDark.
- Rejected new red literals and framework error defaults: W10 expressly
  requires palette-owned errors in both schemes, overriding the research
  document's framework-error proposal. Rejected clay and verdigris alarms
  because the studio record reserves them for credits and proof respectively.
  Neutral Ink/Paper errors preserve those reservations, meet **11.787**
  on their paired fill and at least **9.577 light / 5.312 dark** when the
  error role is foreground on the surface ladder. They intentionally do not
  provide a distinct red alarm hue. Alarm recognition on-device is unverified.
- Retained low-contrast `outlineVariant` for decorative dividers, not required
  edges; `outline` meets 3:1 across the measured surface ladder.
- Did not change primary/secondary/tertiary semantics, layouts or call sites.
  The unchanged dark primary measures **3.229** against surface, so these
  measurements are not a blanket guarantee for arbitrary role combinations.
  No visual call-site defect was established without running the app.

## Observed verification and limits

`python .kiln\scratch\w10-theme-audit.py` exited 0 and reported:

```text
DERIVATION: 8 rounded sRGB interpolations match
IDENTITY: all 12 studio colours retained; additions are derived only
LIGHT: 36 explicit roles; 17 UI roles; missing 0
DARK: 36 explicit roles; 17 UI roles; missing 0
PASS: both schemes cover every scanned UI role; text >=4.5 and outline >=3 on surface ladder
```

This text assertion covers `onSurface`, `onSurfaceVariant` and `error`
against each surface-ladder role, plus all 12 ordinary on-X/X pairs and
`inversePrimary`/`inverseSurface`. It does not claim every accent can be
used as body text on any surface.

`node scripts\verify.mjs --slot plinth-product-photos` exited **1**:
**122 PASS, 3 UNVERIFIED, 4 SKIP, 1 FAIL**. All nine `palette.*` checks passed:
agreement matched 12 declared colours against 20 constants; contrast checked
24 on-X/X pairs across both schemes, weakest **5.19:1**; field chroma max
**C9.9**, below C12. Its full observed report is retained at
`.kiln/scratch/w10-source-verify.txt`.

The failure was `slot.device`: the stored device-verification build digest no
longer matches the current sources. It was not edited or bypassed.
`pack.inventory`, `pack.media` and `build` were UNVERIFIED. **I did not compile,
install, exercise screens, capture screenshots or perform release work.** The
orchestrator owns the next build/device run. The existing explicit
`dynamicColor` opt-in path remains unchanged and can intentionally replace
these static schemes; its runtime colours are outside this measurement.

## Claim ledger

| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| Four missing UI roles at 67 sites became zero missing UI roles | MEASURED | `node .kiln\scratch\plinth-role-gap.mjs`, before/after output above |
| Each scheme separately covers all 17 scanned roles, with 36 explicit assignments | MEASURED | `python .kiln\scratch\w10-theme-audit.py` |
| All 12 identity colours retained; six additions and two existing dividers are derived | MEASURED | `python .kiln\scratch\w10-theme-audit.py`, identity and eight derivation assertions |
| Secondary text minimum is 5.548 light / 5.312 dark across the measured surface ladder | MEASURED | `python .kiln\scratch\w10-theme-audit.py`, SURFACE rows |
| Neutral errors avoid spending reserved proof/credit hues | INFERRED | studio palette reservations and Ink/Paper assignments; alarm recognition is not measured |
| All nine palette checks passed; overall source verification did not pass | MEASURED | `node scripts\verify.mjs --slot plinth-product-photos`, exit 1, report above |
| Compilation and rendered/device behaviour | UNVERIFIED | No Gradle/device command run; orchestrator must run the authorised build and device journey |
