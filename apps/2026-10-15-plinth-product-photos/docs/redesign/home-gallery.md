# W14: category-first home gallery

## Card count and scene selection

The home strip now maps `AutoCategory.entries` once, producing one card for each
category rather than flattening every recipe. Each thumbnail uses the category's
first verified `PROCEDURAL_SCENE` recipe with a non-null `colorArgb`; a missing
canonical recipe fails through the existing bounded error handling rather than
silently omitting a category.

Run from the repository root:

```powershell
node .kiln\scratch\copilot-plinth-w14-home-gallery\check-gallery.mjs
```

Observed output:

```text
BEFORE: 123 recipe cards
AFTER: 21 category cards; exactly 1 per category
Canonical palette pairs: 11 unique (asset palettes are reused)
SOURCE CONTRACT: PASS (not Kotlin compilation or runtime evidence)
```

The script reads the asset and category enum, counts the old per-recipe expansion,
checks one canonical scene per category, prints each category's count and colours,
and asserts the new Kotlin source contract. It exited 1 against the old `flatMap`
implementation and 0 after the edit. This is a source/data check, not execution of
the Kotlin implementation.

The thumbnail uses the canonical recipe's real `colorArgb` and `color2Argb`,
falling back to the primary colour only if the second is absent. Its gradient
projects across the canvas along `angleDeg`, defaulting to 90 degrees like the
engine's procedural renderer (`domain/autocard/AutoCardEngine.kt:386-387`).
No fabricated alpha tint, image asset, network request, permission, or backend was
added. **The existing asset does not provide 21 unique palettes:** several
categories share colours. The asset is outside this task's write scope and was
not changed to manufacture uniqueness.

## Title vocabulary and resources

The source read at task start used `%1$s · %2$s`: a category name followed by
`Studio field`, `Soft gradient`, `Studio scene`, `Generated scene`, or
`Generated %1$d`. Russian suffixes were `Фон студии`, `Мягкий градиент`,
`Сцена студии`, `Сгенерированная сцена`, or `Сцена %1$d`.
These are the actual resource values observed, rather than the symbolic kind
names quoted in the task brief.

Now the entire card title is `categoryLabel(category)`, with no kind or numbered
variant suffix (`ui/home/HomeSceneStrip.kt:205,254-276`). English titles are:
Top, Bottoms, Dress, Outerwear, Shoes, Bag, Accessory, Jewelry, Watch, Cosmetics,
Skin care, Packaged food, Beverage, Small electronics, Home decor, Kitchenware,
Toy, Book, Small furniture, Tool, Other. Their existing Russian translations
remain in use, unchanged (`res/values-ru/strings_autocard.xml:33-53`).

Removed from both `values/strings_home.xml` and `values-ru/strings_home.xml`:

- `home_scene_card_title`
- `home_scene_solid`
- `home_scene_gradient`
- `home_scene_procedural`
- `home_scene_generated`
- `home_scene_generated_variant`

The pre-edit search found these keys only in the strip and their two resource
declarations; the same search after editing found no references:

```powershell
rg -n -g '*.{kt,xml}' 'home_scene_(solid|gradient|procedural|generated|card_title)' apps\2026-10-15-plinth-product-photos\app\src
```

No user-visible strings were added. The loading skeleton, failed state,
cancellation rethrow, bounded parsing catches, and existing font-scaled
`widthIn`/minimum-height sizing were preserved.

## Tap and variant reachability

The strip still calls `onOpenScene(preview.category)` at
`ui/home/HomeSceneStrip.kt:122`; no route or callback signature changed.

There is already a post-category variant chooser:
`domain/autocard/AutoCardFlowState.kt:19-20` obtains that category's recipes;
`ui/autocard/AutoCardScreen.kt:242,261` supplies scene IDs and records selection;
`AutoCardScreen.kt:604-619` renders a `LazyRow` of choices calling
`onChooseScene(sceneId)`.

Reachability qualifications, not newly introduced routes:

- The existing `ui/home/HomeScreen.kt:144-146` ignores the tapped category and
  imports a photo or opens AutoCard for the latest photo. Thus the strip emits the
  category, but the current parent does not preselect it. Fixing category transfer
  requires coordinating the parent/navigation contract; that contract is unchanged.
- `AutoCardScreen.kt:143-145` suppresses recipes for white-main marketplace rules.
  `AutoCardScreen.kt:272-275,613` marks generated scenes unavailable when their
  backgrounds are absent. A chooser exists, but this is not evidence that all six
  variants can be selected in every context. No generated scenes or routes were
  invented to hide that gap.

## Allowed verification

`node scripts\check-locales.mjs --slot plinth-product-photos` exited 0:
900 keys in each locale, with matching format arguments and plural categories.

`node scripts\verify.mjs --slot plinth-product-photos` exited 1 both before and
after the implementation. The check-verdict comparison found **0 changed
verdicts**: 122 PASS, 3 UNVERIFIED, 4 SKIP, 1 FAIL. The failure remains
`slot.device` (a stale device-verification build digest); missing store media and
the deliberately omitted build remain UNVERIFIED. `uiux.text-layout` remained
PASS and did not name `HomeSceneStrip.kt`. Logs are under
`.kiln/scratch/copilot-plinth-w14-home-gallery/verify-before.txt` and
`verify-after.txt`.

No checks were cleared, disabled, or weakened. No Gradle command, compilation,
device run, commit, staging, signing, release, or publication was performed.
Compilation and rendered appearance remain **UNVERIFIED** for the orchestrator's
single build/device slot.
