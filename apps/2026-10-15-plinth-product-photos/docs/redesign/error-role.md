# Error role: visible failure text

## Finding

The falsifiable claim was **true** before this change. The pre-edit light scheme assigned `error = Ink` (`Theme.kt`, pre-edit line 45), while `onSurface` and `onBackground` were also `Ink` (`Theme.kt:30` and `Theme.kt:27`). `Ink` is `#212627` (`Color.kt:6`). The pre-edit dark scheme assigned `error = Paper` (pre-edit `Theme.kt:84`), the same `Paper` used by `onSurface` and `onBackground` (`Theme.kt:69` and `Theme.kt:66`); `Paper` is `#E8E1D5` (`Color.kt:13`). Thus, in each scheme, text styled only with `colorScheme.error` had exactly the same color as ordinary body text.

Eight representative uses across separate screens confirm that the role colors failure text, not merely decoration:

| Site | Classification |
|---|---|
| `app/src/main/java/com/gexetr/plinthproductphotos/ui/autocard/AutoCardScreen.kt:409` | User-facing recognition-disabled / insufficient-credits notice. |
| `app/src/main/java/com/gexetr/plinthproductphotos/ui/autocard/FormingCardPreview.kt:111` | Failure-state caption, selected when `failed` is true. |
| `app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioRoute.kt:153` | Backend-blocked message; generation is unavailable. |
| `app/src/main/java/com/gexetr/plinthproductphotos/ui/batch/BatchScreens.kt:176` | Batch action failure including the error detail. |
| `app/src/main/java/com/gexetr/plinthproductphotos/ui/listing/ListingTextScreen.kt:188` | Required item-name validation error. |
| `app/src/main/java/com/gexetr/plinthproductphotos/ui/coach/CoachScreen.kt:313` | Capture-failed message. |
| `app/src/main/java/com/gexetr/plinthproductphotos/ui/import/ImportScreen.kt:244` | Per-photo import failure and its reason. |
| `app/src/main/java/com/gexetr/plinthproductphotos/ui/library/LibraryScreen.kt:89` | Invalid-date validation error. |

Each is text the user needs to notice as a failure or validation state. The identical role values therefore made actual failure text visually indistinguishable by color from ordinary body text.

## Palette choice

The light-scheme candidate `Clay` (`#92705D`) was rejected: its measured contrast against `surface`, `surfaceContainer`, `surfaceContainerHigh`, and `surfaceContainerHighest` was respectively **3.440:1, 3.093:1, 3.093:1, and 2.795:1**, all below the 4.5:1 text floor. `ClayDeep` (`#62483D`) was selected for the light scheme because it clears the text floor at **6.430:1, 5.781:1, 5.781:1, and 5.224:1** on those same surfaces while differing from `Ink`.

In dark mode, `ClayDeep` was rejected as foreground text because its contrast on the four surfaces was **1.471:1, 1.225:1, 1.116:1, and 1.210:1**. `Paper` was rejected because it duplicates dark `onSurface` exactly. `ClayPale` was also considered, but its contrast on `surfaceContainerHighest` is only **4.316:1**. The chosen `ErrorClayDark` is **not a new identity color**: it is a derived utility tone, the rounded sRGB interpolation from declared `ClayPale` (`#E0C8B9`) to declared `Paper` (`#E8E1D5`) at `0.40`, producing `#E3D2C4`. Its measured dark-scheme contrast is **8.359:1** on `surface`, **6.958:1** on `surfaceContainer`, **6.341:1** on `surfaceContainerHigh`, and **4.694:1** on `surfaceContainerHighest`. Against `onSurface` it measures **1.132:1** with CIE76 **Delta E = 6.08**, so it remains a visibly warm tint rather than repeating the body-text color.

The final pairings are coherent and high contrast: light `Paper` on `ClayDeep` is **6.430:1**, with `Ink` on the `ClayPale` error container at **9.577:1**; dark `Ink` on `ErrorClayDark` is **10.416:1**, with `Paper` on the `ClayDeep` error container at **6.430:1**. The declared studio palette in `store/studio.json` is unchanged.

## Verification

`python .kiln\scratch\w16-error-contrast.py` reports all eight requested surface ratios and passes its role-pair and distinguishability checks. `python .kiln\scratch\w10-theme-audit.py` reports **DERIVATION: 9 rounded sRGB interpolations match**, **IDENTITY: all 12 studio colours retained; additions are derived only**, and preserves secondary-text contrast at **5.548:1 light / 5.312:1 dark** on the surface ladder.

`node scripts/verify.mjs --slot plinth-product-photos` was run before and after. Both runs reported **122 PASS, 1 FAIL, 3 UNVERIFIED**; all nine `palette.*` rows passed both times. The sole failure is the expected `slot.device` stale-build-digest finding. Compilation remains **UNVERIFIED**; Gradle was not run.
