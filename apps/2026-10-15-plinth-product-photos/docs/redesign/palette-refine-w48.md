# W48 - muted teal palette refinement

## Diagnosis and decision

ON RECORD: the W48 owner brief rejects W46's signal-lime identity as
eye-searing ("вырви глаз зелёный"). This is not Checkpoint 1.5 approval.
The rejected action and lane colours were #B9E84B and #96C93A, with
#E7F3BC as their pale container.

INFERRED design intent: one muted teal family over the existing cool graphite
and paper lets product photography supply the strongest colour. A soft sky
alternative was not selected: teal follows the existing pine companion without
introducing another accent family. This is a design choice, not an observed
owner acceptance or an on-device visual verdict.

## Whole-theme changes

| Role | Before | After |
|---|---|---|
| amber / primary action | #B9E84B | #63B7AA |
| amber_deep / tertiary and dark containers | #354A12 | #346E63 |
| amber_pale / pale container | #E7F3BC | #D9EDE7 |
| lane / dark primary and selected state | #96C93A | #4E9F92 |
| accent_deep / pine companion | #21463D | #21463D (unchanged) |
| paper / light field | #F4F7F2 | #F4F7F2 (unchanged) |
| ink / dark field and action text | #111816 | #111816 (unchanged) |

The shared roles are updated together in `store\studio.json` and
`app\src\main\java\com\gexetr\plinthproductphotos\ui\theme\Color.kt`.
Semantic reds, product-photo backdrop colours and neutral surface roles are
unchanged. `Theme.kt` keeps its existing role mapping. The foreground launcher
only replaces pale and lane fills; its four Lightfold paths are unchanged.

## Contrast and verification evidence

MEASURED with `contrastRatio` and `hctOf` from `scripts\lib\hct.mjs`:

| Pair | WCAG contrast |
|---|---:|
| action teal / ink | 7.619:1 |
| lane teal / ink | 5.744:1 |
| deep teal / paper | 5.464:1 |
| pale teal / ink | 14.766:1 |
| muted text / paper | 6.591:1 |
| action teal / paper | 2.187:1 |

The action colour is a fill with ink text, not normal-size text on paper;
the last pair does not meet AA text contrast. The existing light secondary
and tertiary roles use deeper colours instead. No new role mapping makes
light teal normal text on paper. The primary accent measures H185.0,
C36.2, T69.1; lane measures H184.2, C35.3, T60.3.

The initial deep candidate #28554D failed `palette.duplicate` against
`muted` #4E5B54 (1.180:1 contrast, 14.4 degrees of hue, C13.6 difference).
#346E63 was selected instead after checking separation against all declared
roles and its paper contrast. This preserves neutral text rather than
changing it to accommodate the accent.

Verification command (the registered group is singular `icon`):

```powershell
node scripts\verify.mjs --slot plinth-product-photos --only palette,icon
```

MEASURED final result: exit 0, 17 PASS, 0 FAIL, 0 subprocesses.
All nine `palette.*` rows, all seven `icons.*` rows and `slot.adaptiveIcon`
passed. `palette.contrast` checked 24 declared on-X/X pairs across both
schemes; the weakest pair was light onError/error at 5.03:1.

The inline active-identity assertion failed before the change in studio,
Color.kt and the launcher foreground. After the retint it passed, and all
15 shared studio/Kotlin hex roles matched exactly. The launcher foreground's
pathData SHA-256 before and after is
`2dae3d2d862db8ee6406d9b657d088032b9d352f9402d904489593af08fe0ee5`.

## Explicit boundary and remaining match

MEASURED: `app\src\main\res\values\colors.xml:5` still declares
`playkiln_seed_dark` as #96C93A. W48's explicit disjoint file list does not
permit editing that file; it was left untouched. Consequently the requested
whole-slot forbidden-colour scan is not clean: historical documentation is
allowed, but this XML resource is a nonhistorical remaining match. Its retint
to lane #4E9F92 requires the worker owning that file; the scoped palette
refinement must not be mistaken for a complete slot-wide lime purge.

No Gradle build, install or device visual review is claimed. No release
approval was recorded; owner acceptance remains UNVERIFIED.

## W49a - XML seed follow-up

MEASURED: after reading `app\src\main\res\values\themes.xml` and
`ui\theme\Theme.kt`, changed `app\src\main\res\values\colors.xml:5`
to `playkiln_seed_dark = #4E9F92`, matching the muted teal lane.
The XML theme references only the window background; Compose maps dark
primary to `PlinthPalette.Lane`. The existing pine seed #21463D is not lime
and remains unchanged; neither window background nor the night resource changed.
This resolves the remaining XML match recorded above.

MEASURED on 2026-10-17:

```powershell
rg -n "B9E84B|96C93A|C6FF00" apps\2026-10-15-plinth-product-photos --glob '!**/build/**' --glob '!**/docs/**'
node scripts\verify.mjs --slot plinth-product-photos --only palette,icon
```

The scan emitted no matches (rg exit 1). Verification exited 0 with
17 PASS, 0 FAIL and 0 subprocesses. A parsed-XML assertion independently
confirmed the dark seed is #4E9F92. No build, device or release action was run.
