# L — Linter rules

A rule is a pure function `(ImageFacts, MarketplacePreset, SlotRole) -> List<Finding>` in
`domain/linter/`. `ImageFacts` is computed once per output file by `FactsReader` (width, height, bytes,
format, jpeg quality estimate, has-alpha, border-pixel statistics, item-fill ratio from the mask, EXIF
GPS present, OCR text blocks, barcode regions). `SlotRole` is `MAIN`, `EXTRA` or `SLIDE`.

`Finding(ruleId, severity, message, fixAction?, evidence)`; severity is `ERROR` (blocks the marketplace
export unless overridden), `WARNING`, `ADVICE`. Messages are resources in six languages and name the
number from the preset and its `verifiedAt`.

| Id | Rule | Predicate (fails when) | Sev | Fix action | Applies to |
|---|---|---|---|---|---|
| L01 | format | `facts.format ∉ preset.image.formats` | ERROR | `ConvertFormat(to=first allowed, jpg preferred)` | all |
| L02 | file size | `facts.bytes > preset.image.maxBytes` | ERROR | `Recompress(quality search, floor preset.minJpegQuality or 70)`; if floor still too big → `Downscale` | all |
| L03 | minimum size | `w < minWidth` or `h < minHeight` or `longSide < minLongSide` | ERROR | none from pixels alone: offers `Upscale` (O-11) and says it costs credits | all |
| L04 | maximum size | `w > maxSide` or `longSide > maxLongSide` or `w+h > maxSidesSum` | ERROR | `Downscale` | all |
| L05 | aspect | `aspect != null` and `|w/h − aspect| > 0.01` | WARNING (ERROR when the marketplace crops) | `PadToAspect` or `CropToAspect` (item kept inside safe margin) | all |
| L06 | count | pack items `> maxCount` | ERROR | none; lists which items exceed | pack |
| L07 | white main | `backgroundPolicy = white-255` and any of the 4 border bands has a pixel with a channel `< 255` outside the item mask | ERROR | `SetBackgroundWhite255` (Original mode composites on `#FFFFFF`) | MAIN |
| L08 | fill ratio | `minFillRatio` set and `itemArea/canvasArea < minFillRatio` | WARNING | `FillToRatio` (scale item within safe margin) | MAIN |
| L09 | neutral-only background | `backgroundPolicy = neutral-only` and mean chroma of background `> 12 (Lab C*)` | ERROR | `SetBackgroundNeutral` | MAIN, EXTRA |
| L10 | text on main | OCR finds ≥ 1 block with ≥ 3 characters outside the item mask and `textOnMain ∈ {forbidden, penalised-by-size}` | `forbidden` → ERROR; `penalised-by-size` → WARNING with the block's height share | none; shows the box | MAIN |
| L11 | slide text categories | any `SlideText` string matches a category in `preset.text.prohibited` (annex I §Prohibited text) | ERROR (the slide is not rendered) | edit text | SLIDE |
| L12 | slide contrast | WCAG ratio of text colour against its plate `< contrastMin.small` (text < 24 px at 900-wide canvas scale, not bold) or `< contrastMin.large` | ERROR for WB, WARNING otherwise | `AutoTextColour` (choose black/white by ratio) | SLIDE |
| L13 | transparency | `noTransparentPng` and `facts.hasAlpha` | ERROR | `FlattenOnBackground` | all |
| L14 | contacts in image | OCR/barcode finds a phone, e-mail, URL, handle, or a QR region, and `contacts_links` or `qr_code ∈ prohibited` | ERROR | none; shows the box | all |
| L15 | original-only gate | `editPolicy = original-only` and the file was produced with a non-allowed operation | ERROR | `RevertToOriginal` | all |
| L16 | location data | EXIF GPS present in output | WARNING | `StripExif` (default on at export: E22) | all |
| L17 | order hint | WB: accessory/complectation not second after main (only when the seller tagged the role) | ADVICE | reorder | pack |
| L18 | watermark/logo guess | OCR text over `watermark` heuristics (repeated diagonal text, corner brand text) | ADVICE | none; the engine cannot prove absence | all |
| L19 | listing text limits | title/description longer than `listing.titleMax` / `descriptionMax` | ERROR for export of text | `Trim` suggestion | text |
| L20 | JPEG quality floor | estimated quality `< minJpegQuality` | ERROR | `Recompress` | WB |

## Honest limits (carried into the UI wording)

OCR and barcode detection can miss text; L10/L14/L18 therefore say "no text found" never "no text".
L18 is advice only. A marketplace's moderation can reject an image the linter passes; the app says
"meets the rules we read" (annex B rule 6).

## Fixtures (the project check `linter.fixtures`, task 5.5)

For every rule: one image fact set that passes, one that fails, and for every first-queue marketplace one
case that is accepted by the preset's own limits. Concrete cases: WB 699×933 fails L03, 700×933 passes;
WB 10 485 761 bytes fails L02, 10 485 760 passes; Avito HEIC fails L01 with `ConvertFormat(jpg)`; Amazon
MAIN border pixel `#FEFEFE` fails L07, `#FFFFFF` passes; Etsy PNG with alpha fails L13; Vinted pack with a
cut-out fails L15; WB slide containing "Скидка 30%" fails L11 in ru and "-30%" fails in every language;
slide text "Хит продаж" fails L11 (evaluative_claim); text `#777777` on `#FFFFFF` (ratio 4.48) fails L12 at
small size, `#767676` (4.54) passes.
