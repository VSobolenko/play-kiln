# W38 — Studio home polish

## What changed

Studio now leads with the seller's selected product photo before the heading and
explanatory copy, with the selected item's name carried in a badge over the image.
The import action remains the prominent way to start when no photo is selected.
Existing photos are browsable as an image rail; Studio operations remain
horizontal, image-led cards using that same source photo as context, with the
operation icon, purpose, current cost and existing availability details. The
current generated result now appears directly below the hero, ahead of the photo,
prompt and operation galleries, preserving its existing apply/save actions.
Home's tool cards use the latest local product photo when one exists and retain a
palette-backed icon fallback when it does not; Home is not changed in this Studio
task.

Costs are not estimates: Home still uses `StudioOperationCatalog.costFor`, and
the live Studio shows the server's current operation quote in the existing
`PlinthCostChip` beside each operation name. The action button no longer repeats
that price and the shared balance; confirmation still verifies the quote and
balance before starting. The top-bar balance and wallet action are unchanged.
Coil and the existing Compose/Material surfaces are reused; no dependency,
navigation, wallet, settings, or app-shell change was made.

## Scene/category visuals

No second scene picker was added to live Studio. Home already has a category
strip whose thumbnails are derived from each category's first verified
`PROCEDURAL_SCENE` recipe and its actual palette values
(`ui/home/HomeSceneStrip.kt:152-169,259-284`). The live Studio request path accepts
an operation and prompt, not a category or recipe id
(`ui/studio/StudioViewModel.kt:161-186`); showing category tiles there as if they
controlled generation would imply a capability the request cannot honor.
Instead, the Studio route uses the user's real product image for its visual
density and operation cards. Recipe-backed category navigation remains on Home.

## Verification

- `node scripts/check-locales.mjs --slot plinth-product-photos` — PASS; 1,022
  entries in English and Russian with matching format arguments and plurals.
- `node scripts/verify.mjs --slot plinth-product-photos` — 122 PASS, 3
  UNVERIFIED, 4 SKIP, 1 FAIL; `slot.device` reports that its recorded digest
  (`a9cb7f6d…`) does not match the current source digest (`7df2e761…`). No device
  record was refreshed. The build row remains UNVERIFIED because this task
  explicitly did not run Gradle.
- `git diff --check` on `StudioRoute.kt` — PASS.

Compilation and rendered appearance are **UNVERIFIED**; no Gradle or device run
was performed.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| Home tool cards use the latest local product photo when available and keep the existing catalog-derived cost. | MEASURED | `ui/home/HomeScreen.kt:125,172-192`; `ui/components/PlinthToolCard.kt:31-72` |
| Studio's hero starts with the actual selected photo, overlays its selected-name badge, and keeps the existing picker action. | MEASURED | `ui/studio/StudioRoute.kt:162-206` |
| The delivered result is now immediately after the hero and preserves its existing apply/save actions. | MEASURED | `ui/studio/StudioRoute.kt:208-256` |
| The photo rail displays actual project-photo files and retains the existing selection callback. | MEASURED | `ui/studio/StudioRoute.kt:264-309` |
| Saved output cards load recorded output paths and preserve the existing open-variant action. | MEASURED | `ui/studio/StudioRoute.kt:311-352`; `ui/studio/StudioViewModel.kt:448-455` |
| Each operation card shows `PlinthCostChip` from the current server quote; the action has no duplicate price/balance chip. | MEASURED | `ui/studio/StudioRoute.kt:369-472`; `state.config?.ops` / `quote?.credits` at `:372-373`, chip at `:419`, button at `:440-448` |
| Confirmation still compares the displayed price with the refreshed quote and gates the start action. | MEASURED | `ui/studio/StudioRoute.kt:487-545`; `ui/studio/StudioViewModel.kt:100-112,161-186`; `ui/studio/StudioConfirmationPreviewTest.kt` |
| Home category thumbnails use verified recipe metadata; they are palette-derived scene thumbnails, not photographs of generated outputs. | MEASURED | `ui/home/HomeSceneStrip.kt:152-169,259-284` |
| Recipe-category cards would be misleading in live Studio because its request accepts no category/recipe selection. | INFERRED | Studio request shape at `ui/studio/StudioViewModel.kt:161-186`; compared with category callback at `ui/home/HomeSceneStrip.kt:139` |
| English and Russian resources are complete and format-compatible. | MEASURED | `node scripts/check-locales.mjs --slot plinth-product-photos`: PASS, 1,022 entries |
| The current Compose code compiles and the screen looks polished on-device. | UNVERIFIED | No Gradle build or device review was run, per task constraints. |
