# W98b — Cutout and Create fixes

Scope: the six reported defects from the W97c A40 pass and W94 owner complaints. No Gradle build
or post-change device run was performed, as required. References marked “before fix” identify the
root-cause lines in the starting source.

| Defect | Measured evidence | Root cause (path:line) | Fix | How a user now sees it |
|---|---|---|---|---|
| E8: charge confirmation leaked into cut-out review | W97c `device-pass-w97c.md:17` measured “Delivered and confirmed. 1 credits were charged once…” below the result; W94 `owner-complaints-canonical-w94.md:73` says the balance belongs in the top bar. | Before fix, `ui/cutout/CutoutScreen.kt:227` selected the settled-charge message and `:286-288` rendered it in review. | Removed that message, the charge-timing lines, and the refund/shortfall amount lines from the cut-out UI. Reworded remaining error/status copy without charge, spending, credit, or balance assertions. The action price remains. | Result/review has no charge, credit or balance line. The wallet balance stays in the top bar; the action button shows the price. |
| “1 credits” grammar | Same measured sentence, W97c `device-pass-w97c.md:17`. | Before fix, `res/values/strings_cutout.xml:16` and its Russian peer formatted a variable credit count as an ordinary string. | Removed the sentence and all other variable refund/shortfall counts. The only remaining displayed counts are the action price and mask revision; both use locale-aware plurals in both languages. | Result/review status has no credit count; the action price uses its locale-aware plural (“1 credit” / “1 кредит”). |
| Green mask tint instead of the product cut-out | W97c `device-pass-w97c.md:17` records the green-grey mug; screenshot `.kiln/scratch/device-w97c/s16-cutres.png`. | Before fix, `ui/cutout/CutoutScreen.kt:471-477` drew `selectionOverlay` over the review bitmap. The overlay is intentionally teal (`ui/cutout/CutoutViewModel.kt:1214-1233`, `:1330`). | Removed the overlay from `CutoutReviewPreview`; the mask tint remains on the editing surface only. The review now draws the bitmap from `renderCutoutPreview`, which keeps each source pixel’s RGB and multiplies its alpha by the mask (`CutoutViewModel.kt:1095-1119`). | The checkerboard shows the original-colour kept object, with pixels outside the mask transparent. |
| E4: painting was mistaken for scrolling | W97c `device-pass-w97c.md:19-23` measured 4,062 and 9,910 changed pixels on photo drags, exact undo (0 px diff), and scrolling only when a drag began below the surface. W94 `owner-complaints-canonical-w94.md:34-42` requires a dedicated editor and a safe scroll gesture. | Before fix, `ui/editor/EditorScreen.kt:178-190` split the surface and a scrollable controls column; `:288-300` handled a surface drag as a brush stroke. | Kept the one-finger brush meaning; no two-finger gesture was added. The canvas now carries a persistent “drag here to paint / scroll below the photo” hint and a coloured REMOVE/RESTORE mode badge. Matching mode buttons use distinct selected colours. The canvas and compact controls share the screen; a “Done editing” action is fixed in the scaffold bottom bar. Undo/history code is unchanged. | The canvas says exactly what dragging does and where scrolling starts; the active mode is named and coloured on the canvas edge. Brush size, modes and undo/redo are arranged in the same screen, with Done always available. |
| E1: duplicate Create action; save destination was vague | W97c `device-pass-w97c.md:11-13` records Create on the selected-photo screen, another identical Create on the next screen, and no saved location after export. W94 `owner-complaints-canonical-w94.md:6-18` requires one obvious next step and a known destination. | Before fix, `ui/card/CardScreen.kt:131` made the second screen wait for another Create tap; `ui/card/CardViewModel.kt:48-61` loaded only the source photo. The old saved-state string did not name a destination (`res/values/strings_card.xml:16`). | Loading the confirmed photo now starts local composition (`CardViewModel.kt:51-72`); the card screen exposes Save only after the result exists, or Retry on composition failure. After export, the view model derives the selected folder from the returned document URI and stores it in UI state. | The first Create action produces the card without another Create prompt. After Save image, the result says “Saved to [selected folder]”; Downloads and storage-root names are localized. |
| Clipped caption and cutout action under navigation bar | W97c `device-pass-w97c.md:13` measured the marketplace caption at 9 px high; `:15` measured the cut-out action as a sliver until scrolling. | Before fix, `ui/card/CardScreen.kt:224-227` gave the caption no minimum line count/height. The cut-out action was inside the scrollable control column (`ui/cutout/CutoutScreen.kt:385-398`). | The caption now uses `minLines = 2` and a 36 dp minimum height. The cut-out action moved to `PlinthScaffold.bottomBar`; the editor Done action is there too. `PlinthScaffold.kt:25-33` applies `WindowInsets.safeDrawing` and navigation-bar padding to the bottom bar. | The marketplace note reserves two lines; the primary cut-out and editor actions remain above system navigation without scrolling. |

## Removed and added strings

Removed for E8 from `app/src/main/res/values/strings_cutout.xml` and
`app/src/main/res/values-ru/strings_cutout.xml`:

| Resource | English removed | Russian removed |
|---|---|---|
| `cutout_charge_settled` | `Delivered and confirmed. %1$d credits were charged once for this cut-out.` | `Результат доставлен и подтверждён. За вырезание списано кредитов: %1$d.` |

The same cutout files also had the charge-timing strings `cutout_charge_after_delivery` and
`cutout_charge_pending` removed, plus the financial-status plurals `cutout_credits_returned_plural`
and `cutout_need_credits_plural`. `cutout_not_charged`, `cutout_failure_not_charged`,
`cutout_balance_unknown`, `cutout_release_unconfirmed`, `cutout_wallet_refreshing`, and
`cutout_wallet_refresh_failed` were removed or renamed to neutral status resources
(`cutout_cutout_failed`, `cutout_wallet_unavailable`, `cutout_cutout_not_completed`,
`cutout_request_refreshing`, and `cutout_request_refresh_failed`). Values of
`cutout_session_required`, `cutout_job_failed`, `cutout_moderation_blocked`, `cutout_result_expired`,
`cutout_integrity_failure`, `cutout_network_failure`, `cutout_unknown_failure`,
`cutout_mask_not_found`, and `cutout_photo_import_failed` were rewritten as status or retry
instructions. `CutoutScreen.kt` no longer renders charge-timing or refund-count copy. These changes
are in both locale files; only the action-price plural retains credit wording.

The redundant `card_make` (“Create product card” / “Создать карточку товара”) entry was also removed
from `app/src/main/res/values/strings_card.xml` and
`app/src/main/res/values-ru/strings_card.xml`. The Create action in the photo-confirmation flow
remains.

Added the mask-revision plural to **both** `values/strings_cutout.xml` and
`values-ru/strings_cutout.xml`:

| Resource | Count represented |
|---|---|
| `cutout_saved_mask_plural` | Mask revision number |

The existing `cutout_start_cost_label` plural remains the action-button price (“Cut out item · 1
credit” / “Вырезать товар · 1 кредит”). Refund and shortfall counts are no longer displayed; the
shortfall sheet directs the user to update the wallet without showing an amount, and the top bar
remains the balance surface.

Other new localized strings provide the editor’s REMOVE/RESTORE labels, drag/scroll hint and Done
action, plus the card destination names (Downloads, internal storage and external storage) and the
save-location result format.

## Compositing path

Before the fix, the review composable rendered the cut-out bitmap and then drew the editor’s
`selectionOverlay` on top (`CutoutScreen.kt:471-477` in the starting source). `makeMaskOverlay`
creates the teal mask visualization (`CutoutViewModel.kt:1214-1233`, colour `:1330`); that is useful
while painting but not the product preview. `renderCutoutPreview` already preserves the source RGB
and applies the mask to source alpha (`CutoutViewModel.kt:1095-1119`). The review now displays only
that rendered bitmap over its neutral checkerboard; the editor continues to display the tinted mask.

## Editor and insets verification

- **One-screen layout:** source inspection confirms the canvas/control allocation is `.78f` / `1.22f`
  of the remaining content (`EditorScreen.kt:193,234`); the controls therefore receive 61% of that
  area, with mode, brush size, undo/redo and select/reset in one compact column. The column retains
  scrolling only as an overflow fallback. This is a source-layout check, not a post-change A40
  screenshot measurement.
- **Gesture hint:** both locale resources provide the one-line instruction, placed on the canvas edge
  (`EditorScreen.kt:226`). The existing brush handler remains `detectDragGestures`; the hint directs
  the user to start page scrolling below the canvas, and no competing two-finger gesture exists.
- **Mode clarity:** a REMOVE or RESTORE label is rendered on the canvas edge (`EditorScreen.kt:202-218`)
  and the selected mode button uses a distinct error/tertiary container colour. Both are checked in
  the static regression assertions.
- **Sticky actions and navigation:** Cutout’s priced action and the editor’s Done action are Scaffold
  bottom bars. `PlinthScaffold.kt:25-33` wraps the bottom bar in navigation/IME insets and gives the
  content safe-drawing insets; neither action relies on a scrollable page footer. Each brush stroke,
  undo and redo persists through `CutoutViewModel.kt:382-427,988-1018`; Done is disabled while that
  save is in progress, then returns to the separate result screen.
- **Caption height:** `CardScreen.kt:226-231` sets `minLines = 2` and `heightIn(min = 36.dp)`. The
  card page remains scrollable while its Save action is in the scaffold bottom bar.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| The original W97c review displayed the settled-charge sentence and green-tinted result. | MEASURED | `docs/redesign/device-pass-w97c.md:17`; screenshot `s16-cutres.png`. |
| Photo drags painted 4,062/9,910 pixels; undo returned to a zero-pixel difference; off-canvas drag scrolled. | MEASURED | `docs/redesign/device-pass-w97c.md:19-23`. |
| The tint came from the overlay drawn above the review bitmap, not from the cut-out compositor. | MEASURED | Starting `CutoutScreen.kt:471-477`; `CutoutViewModel.kt:1095-1119,1214-1233`. |
| Changed EN/RU XML pairs parse and have matching resource keys; Kotlin resource references resolve. | MEASURED | PowerShell inline `python` using `xml.etree.ElementTree`, EN/RU key-set equality over all four resource pairs, and `R.string`/`R.plurals` reference checks printed `PASS: four EN/RU XML pairs parse and match; ... all Kotlin string/plural references resolve`. |
| No non-action cutout string mentions charges, spending, credits, or balance. | MEASURED | `rg -n "charged|spent|credit|кредит|списан|баланс" "apps\2026-10-15-plinth-product-photos\app\src\main\res\values\strings_cutout.xml" "apps\2026-10-15-plinth-product-photos\app\src\main\res\values-ru\strings_cutout.xml"` returned only `cutout_start_cost_label` entries. |
| The modified layout has the expected weights, persistent hint, distinct modes, sticky actions/insets, and two-line caption constraint. | MEASURED | PowerShell inline `python` source assertions verified weights `.78f`/`1.22f`, `cutout_editor_drag_hint`, REMOVE/RESTORE labels and distinct selected colours, caption `minLines = 2`, bottom bars and safe/navigation insets, with no charge message or tint overlay. Output: `PASS: .78f/.1.22f editor weights; one-line paint/scroll hint; named REMOVE/RESTORE badges with distinct colours; two-line card caption; sticky actions with safe/navigation insets; no review charge message or tint overlay.` These are source constraints, not rendered pixels. |
| Post-fix compilation and A40 visual/gesture behavior. | UNVERIFIED | The requested Gradle build/device replay was not run. The integrator can run `node scripts/verify.mjs --slot plinth-product-photos --build`, install the resulting debug APK, then repeat W97c S05-S16. |
