# Scene freeform (W87b) — claim ledger

Slot: `apps/2026-10-15-plinth-product-photos`. No Gradle was run; every claim is a `path:line`-style source reading, not an executed test.

| Claim | Status | Evidence |
|---|---|---|
| Generate works with prompt text alone (no category, no recipe) | UNVERIFIED (not run) | `AutoCardEngine.generationPrompt` returns non-null when guidance is non-blank; test `freeformPromptAloneBuildsAPromptWithoutCategoryOrRecipe` added, not executed |
| Recipe radio (`selectedSceneId`, Scene 1/2/3) removed from UI | MEASURED by reading | `GeneratedSceneRoute.kt`: no `selectedSceneId`; `generationPrompt(..., null, guidance)` |
| Chips only fill the prompt field | MEASURED by reading | `AssistChip(onClick = { guidance = text })`, strings `w87_chip_*` |
| Refresh price button deleted | MEASURED by reading | `w74_refresh_price` removed from code and both string files |
| Compact photo (72 dp) + Change when a photo is selected | MEASURED by reading | `Row`/`Box(size(72.dp))` + `w74_change_photo` |
| No job IDs/UUIDs shown | MEASURED by reading | none rendered in the route |
| Category dropdown and attributes field removed from this screen | MEASURED by reading | recognition still runs silently and enriches the prompt when present |
| Existing recipe-based prompt behaviour with blank guidance kept | UNVERIFIED | existing tests unchanged; `autoCategoryLabel` left as unused private helper |

Recipe-backed prompts: when guidance is non-blank and no recipe is explicitly chosen, the recipe's background text is omitted so the seller's words lead; shadow and fill defaults are kept.
