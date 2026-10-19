# W94a — Create / Card flow

## Before

| Screen element | What it did before this change | Why it was confusing |
|---|---|---|
| “ИИ-фон” / “Макет карточки” after photo selection | The first opened the AI-background route; the second opened the local card-composition screen (`ImportScreen.kt:106-117` before this change; `PlayKilnApp.kt:474-475`). | Two different jobs shared one moment, and “Макет карточки” did not say that it made a local image rather than an AI background. |
| “Готовая карточка” | Switched the preview to the locally composed bitmap held in `CardUiState`; it was not yet saved (`CardScreen.kt:210-216` before this change). | “Готовая” sounded like a saved/finished destination, but it was just one view of the current result. |
| “До” | Switched that same preview to the original imported photo (`CardScreen.kt:218-224` before this change). | It was a source/result comparison, not a second card or a second saved item. |
| “Черновик готов · проверьте результат” | Reported that the in-memory composition existed (`values-ru/strings_card.xml:7`; `CardScreen.kt:194-204` before this change). | It called an unsaved image a draft and did not explain whether or where it had been saved. |
| “Создать заново” | Called `makeCard` again with the same loaded photo/mask, 1200 × 1200 canvas, and current background (`CardScreen.kt:274-278` before this change; `CardViewModel.kt:71-88`). | It promised a new card but offered no new input or variation; it repeated the same composition flow. |
| “Сохранить черновик PNG” / “Черновик PNG сохранён” | Opened Android’s `CreateDocument` picker and wrote the PNG to the returned URI, not to the app Library (`CardScreen.kt:74-84` before this change; `CardViewModel.kt:97-115`; `values-ru/strings_card.xml:12-14` before this change). | “Сохранён” did not name the chosen destination, and “черновик” made the output sound like an in-app project. |

**What those two “cards” actually were:** “Готовая карточка” displayed the generated composition; “До” displayed the original photo. They were two preview states of one image flow, not two separate cards. “Черновик” was only copy for the unsaved composition/file, not a third destination.

## After

| Screen element | New copy / behaviour | Evidence |
|---|---|---|
| Create screen title and empty state | “Создать карточку товара” / “Начните с фото товара”; the empty state has one “Выбрать фото товара” action. | `ImportScreen.kt:91,185-195`; `values-ru/strings_import.xml:3-5`; `values/strings_import.xml:3-5` |
| Selected photo actions | The filled primary action is “Создать карточку товара” and opens the local card flow. The text action says “Или создать ИИ-фон” and opens the separate AI route. | `ImportScreen.kt:99-117`; `values-ru/strings_import.xml:6-8`; `values/strings_import.xml:6-8` |
| Selected photo preview | States that the photo is selected and ready for a card; additional imported photos say whether they are selected or can be tapped. | `ImportScreen.kt:130-165`; `values-ru/strings_import.xml:8-10`; `values/strings_import.xml:8-10` |
| Card result | Shows the composed image as “Ваша карточка товара”; the original/result chips are removed, so there is one result image. | `CardScreen.kt:109,172-225`; `values-ru/strings_card.xml:10`; `values/strings_card.xml:10` |
| Unsaved result and primary action | Says “Не сохранено в приложении. Кнопка «Сохранить изображение» откроет системный выбор места.” The single filled action is “Сохранить изображение”; it opens Android’s location picker. | `CardScreen.kt:127-148,210-225`; `values-ru/strings_card.xml:13-14`; `values/strings_card.xml:13-14` |
| Save result | After writing the PNG, says “Сохранено в выбранное в системном окне место, не в библиотеку приложения.” The primary action becomes “Готово” and returns from the card screen. | `CardScreen.kt:131-148,210-225`; `CardViewModel.kt:97-122`; `values-ru/strings_card.xml:17-18`; `values/strings_card.xml:17-18` |
| Re-render | “Создать заново” is removed from the screen, and `makeCard` refuses to replace an existing composition. | `CardScreen.kt:127-248`; `CardViewModel.kt:71-75` |

## First-time user questions

1. **What did I just make?** “Ваша карточка товара.”
2. **Is it saved?** “Не сохранено в приложении. Кнопка «Сохранить изображение» откроет системный выбор места.”
3. **What do I tap next?** “Сохранить изображение.”

After saving, the exact on-screen answer to “where?” is “Сохранено в выбранное в системном окне место, не в библиотеку приложения.” The result is not represented as a Library item.

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| The former “Готовая карточка” and “До” controls selected the composition and original photo respectively; neither was a saved-card destination. | MEASURED | Pre-edit `CardScreen.kt:210-224`; `values-ru/strings_card.xml:7-8`; source selection is `CardScreen.kt:109`. |
| Re-rendering used the same loaded source, mask, canvas dimensions, and background input; the UI exposed no new render inputs. | MEASURED | `CardViewModel.kt:71-88`; pre-edit `CardScreen.kt:274-278`. |
| PNG export went to the URI chosen through Android’s document picker, rather than the app Library. | MEASURED | Pre-edit `CardScreen.kt:74-84`; `CardViewModel.kt:97-122`; user-visible destination copy is in `values-ru/strings_card.xml:14,17`. |
| Create/Card copy exists in English and Russian with matching IDs; no “draft/черновик” wording remains in the card resources. | MEASURED | `values/strings_card.xml:3-20`; `values-ru/strings_card.xml:3-20`; `node scripts/check-locales.mjs --slot plinth-product-photos` reported 1,128 localized keys complete. |
| The targeted Create/Card assertions passed, including one filled `CardScreen` button, no result toggle/re-render affordance, localized resource parity, and the save/done states. | MEASURED | `CardScreen.kt:127-148,210-225`; `values/strings_card.xml:13-20`; `values-ru/strings_card.xml:13-20`; inline Python regression assertions in this session: 10/10 PASS. |
| Source-only slot verification found 122 PASS, 3 UNVERIFIED, 4 SKIP, and one FAIL: `slot.device` reports that its recorded build digest no longer matches the current inputs and requires a fresh on-device run. Build evidence is UNVERIFIED because `--build` was not run. | MEASURED | `node scripts/verify.mjs --slot plinth-product-photos`; `slot.device` asks for a fresh on-device run. Gradle was not run, as required. |
