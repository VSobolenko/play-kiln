# W87d — Home toolbox

The Home screen now starts with the selected product photo and a neutral
“Studio tools” catalogue. Equal-size image-tool cards show their purpose and
credit cost; listing text, marketplace cards, and slide kits are labeled as
separate work and kept out of that catalogue.

## Claim ledger

| Claim | Status | Evidence |
|---|---|---|
| The hero presents the product photo rather than selling AI backgrounds. | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/home/HomeScreen.kt:249-260`; `app/src/main/res/values/strings_home.xml:4-5`; `app/src/main/res/values-ru/strings_home.xml:4-5` |
| The toolbox contains eight image operations, each with a title, purpose, and displayed catalog cost. | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/home/HomeScreen.kt:361-440,576-620`; `app/src/test/java/com/gexetr/plinthproductphotos/ui/home/HomeToolboxTest.kt:10-37` |
| Listing text and slide kits are separate from image generation and editing. | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/home/HomeScreen.kt:210-235,451-485`; `app/src/main/res/values/strings_home.xml:51-53`; `app/src/main/res/values-ru/strings_home.xml:53-55` |
| The Scene strip is no longer part of Home. | MEASURED | `HomeSceneStrip.kt` removed; `rg -n 'HomeSceneStrip|home_scene_strip_title|home_scene_load_failed' apps/2026-10-15-plinth-product-photos/app/src/main` returns no matches |
| Regression cases cover the image-tool inventory and base credit costs. | MEASURED | `app/src/test/java/com/gexetr/plinthproductphotos/ui/home/HomeToolboxTest.kt:10-37` |
| Kotlin unit-test execution and compilation are verified. | UNVERIFIED | Gradle was not run per task instruction. |
