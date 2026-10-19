# W87i — Owner complaints audit (2026-10-17)

Read-only audit of the current slot tree. Verdicts describe the source observed
in this audit; screenshots/device observations were not available. The quote in
each ID cell preserves the complaint wording supplied by the owner.

| ID | Verdict | Evidence path:line | Owner file(s), if open | One phone observation that proves it fixed |
|---|---|---|---|---|
| C1 — Studio under the hero image: "одна кнопка расположена на весь экран" — tools must be a real catalogue (grid), each with a clear "what it does" line, not one full-width button. | **FIXED** — Home and live Studio both render multi-card tool grids with purpose text. | `app/src/main/java/com/gexetr/plinthproductphotos/ui/home/HomeScreen.kt:149-165,384-432,572-615`; `app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioRoute.kt:225,424-480,744-781` | — | With a photo selected, open Home and Studio; observe multiple equal two-column tool cards and a purpose line, with no single full-screen choose-tool button. |
| C2 — Buttons scattered across the screen; layout unreadable. | **PARTIALLY FIXED** — controls are grouped into a catalogue and a vertical tool form, but narrow-phone spacing/readability is not measured on device. | `HomeScreen.kt:149-165,202-239`; `StudioRoute.kt:180-195,225-240,278-330,526-671` | If the device check reproduces it: `HomeScreen.kt` for Home; `StudioRoute.kt` for Studio. | On a narrow phone, open one tool and observe the title, photo, controls, and Generate action in a readable sequence without overlapping or orphaned buttons. |
| C3 — Expected flow: I choose a picture, then choose a tool. Photo must NOT be re-requested inside tools. | **FIXED for the latest Home photo** — Studio selects the first recent photo and Cutout auto-opens the newest imported photo when one exists; neither asks again in that flow. The tool-first empty-state path remains a separate first-time usability concern (C15). | `HomeScreen.kt:244-260`; `CutoutViewModel.kt:238-248,271`; `StudioRoute.kt:122-127,130-151,278-290,560-584` | — | Import a photo, tap Cutout and then a Studio tool; observe that both use the already-imported photo without opening another picker. |
| C4 — Tool cards must state what each tool does ("где плюс еще каждом инструменте будет понятно, что он делает"). | **FIXED** — Home cards and Studio rows pair a title with a purpose resource. | `HomeScreen.kt:384-432,572-615`; `StudioRoute.kt:424-480,744-781`; `app/src/main/res/values-ru/strings_home.xml:17-28` | — | Read each visible tool card on the phone; its purpose is visible without opening it. |
| C5 — "Вырезай товар": still asks "выбрать фото" though a photo was chosen; photo shown at half screen. | **PARTIALLY FIXED** — Cutout now auto-opens the newest imported photo, so the repeated chooser is removed when a photo exists. The source-image stage still has a 300 dp minimum height; whether it occupies half the usable phone screen needs device measurement. | `CutoutViewModel.kt:238-248,271`; `CutoutScreen.kt:237-244,340-348,571-588` | If the phone confirms the image is still too large: `app/src/main/java/com/gexetr/plinthproductphotos/ui/cutout/CutoutScreen.kt`. | Open Cutout with an imported product photo; observe the photo is reused and the source preview is compact enough to keep Generate and key controls visible. |
| C6 — Client-facing IDs / job ids shown to the user anywhere in the cutout/studio flow. | **STILL PRESENT** — Studio/Cutout internals keep IDs in their view models, and the Wallet reachable from those flows interpolates raw job IDs into user-visible outcomes. | `app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioViewModel.kt:196,293-307`; `app/src/main/java/com/gexetr/plinthproductphotos/ui/cutout/CutoutViewModel.kt:506,556-587`; `app/src/main/java/com/gexetr/plinthproductphotos/ui/wallet/WalletScreen.kt:379-398`; `app/src/main/res/values-ru/strings_wallet.xml:33-36` | `ui/wallet/WalletScreen.kt`, `res/values/strings_wallet.xml`, `res/values-ru/strings_wallet.xml`. | Open Wallet from a tool flow with a recent job; observe no UUID or raw job ID in any user-visible status. |
| C7 — "Обновить стоимость" / refresh-cost button exists — must not. | **PARTIALLY FIXED** — the visible Cutout recovery action is “Retry” and the old `cutout_refresh_wallet` resource is not referenced by UI, but the no-session report still tells users to press the removed “Обновить стоимость и баланс” action. | `CutoutScreen.kt:407-418,427`; `app/src/main/res/values-ru/strings_cutout.xml:6,19`; `app/src/main/res/values-ru/strings_report.xml:3`; `CutoutScreen.kt:337` | `res/values/strings_report.xml`, `res/values-ru/strings_report.xml`; remove or reconcile the stale Cutout refresh label in `res/values/strings_cutout.xml` and `res/values-ru/strings_cutout.xml` if retained. | Trigger the no-session report state in Cutout; observe there is no refresh-cost button and no instruction naming one. |
| C8 — "Сгенерировать фон сцены": UI shows only Scene 1 / Scene 2 / Scene 3 (or two selectable backgrounds); cannot request "фон моря" / "лес для палатки" in free words. | **FIXED** — Scene has a required free-text prompt; sea, forest, and campsite are suggestion chips that fill that prompt, not exclusive scene selections. | `StudioRoute.kt:587-636`; `app/src/main/res/values-ru/strings_studio_live.xml:52-57`; `StudioViewModel.kt:180-184` | — | In Scene, type “фон моря” (or “лес для палатки”), tap Generate, and observe the typed prompt remains the submitted direction rather than a fixed scene choice. |
| C9 — "Изменить освещение" (Relight): screen shows "инструменты студии", "вернуться в студию", re-asks photo, shows previously generated pictures as a picker, and a label wraps onto three lines obscuring content ("доступно два / для всех два кредитов"). | **PARTIALLY FIXED** — the active tool title comes from the Relight row, the existing photo is bound with only an optional Change action, and the result view is a before/after of the current run rather than a history picker. Russian credit amounts use plural resources; actual wrapping/readability still needs a phone check. | `StudioRoute.kt:180-195,278-320,560-584,744-758`; `app/src/main/res/values-ru/strings_studio_live.xml:37-42,47-50`; `PlayKilnApp.kt:766-772` | If the device check reproduces clipping: `StudioRoute.kt`, `app/src/main/res/values/strings_studio_live.xml`, `app/src/main/res/values-ru/strings_studio_live.xml` (after W87h has stopped writing them). | Open Relight with a photo selected; observe a Relight title, a compact already-selected photo, no history picker, and all credit text readable without obscuring the form. |
| C10 — "Расширить холст" (Expand/Outpaint): re-asks to choose a photo though one was chosen. | **FIXED for photo selection** — Expand row/form reuses the already-selected photo. It is currently non-runnable because checked-in server config disables O-09; see the operation mismatch below. | `StudioViewModel.kt:180-185,572-587`; `StudioRoute.kt:130-151,278-290,560-584,764-768`; `server/src/registry.ts:59-65` | — for the photo complaint. To make Generate runnable, own `StudioViewModel.kt` plus `server/src/registry.ts` and `server/src/providers/fal.ts`, or stop advertising it in Home. | Select a photo and tap Expand; observe the same photo in its tool form without a picker. |
| C11 — "Увеличение разрешения" (Upscale): screen title says "инструменты студии" — wrong identity. | **FIXED for title** — active screen title resolves from the selected row; Upscale maps to its dedicated title resource. The row is non-runnable under checked-in server config (O-11 disabled); see the operation mismatch below. | `StudioRoute.kt:180-188,727,770-774`; `app/src/main/res/values-ru/strings_studio_live.xml:46`; `server/src/registry.ts:67-72` | — for the title complaint. To make Generate runnable, own `StudioViewModel.kt`, `server/src/registry.ts`, and `server/src/providers/fal.ts`, or stop advertising it in Home. | Tap Upscale and observe the title “Увеличение разрешения” above its tool form. |
| C12 — "Другие способы завершить работу" footer at the bottom — unexplained, first time seen; must not exist or must be self-explanatory. | **FIXED in current Home wording/layout** — the old “other ways to finish” wording is absent; Home has a separate “Другие инструменты и экспорт” section and explicitly named actions. | `HomeScreen.kt:202-239`; `app/src/main/res/values-ru/strings_home.xml:53-56`; `rg -n "Другие способы завершить работу|Other ways to finish|другие способы|other ways" app/src/main` returned no matches. | — | Scroll to the bottom of Home and observe separately named workflows under “Другие инструменты и экспорт”, not an unexplained finish-work footer. |
| C13 — Landing starts with a big picture + "Создать ИИ-фон / сгенерируйте новое окружение" — implies the app is only AI background; user may want another tool. | **FIXED** — hero copy is neutral (“Фото вашего товара” / “Выберите ниже инструмент для обработки”); scene generation is one card among the catalogue. | `HomeScreen.kt:248-260,149-165`; `app/src/main/res/values-ru/strings_home.xml:4-5,20-28` | — | Cold-open Home and observe a neutral product-photo hero followed by several different tools, not an AI-background-only headline. |
| C14 — "Текст объявления": shows Amazon / Wildberries / Ozon marketplace advertising inside a generative image app — must not be presented as a generative tool. | **FIXED for Studio/Home catalogue** — Listing text is a separately labelled workflow action and is absent from the image-tool rows. | `HomeScreen.kt:202-232,572-615`; `app/src/main/res/values-ru/strings_home.xml:53-56`; `StudioRoute.kt:744-781` | — | Open the Studio catalogue and observe that Listing text / marketplace copy is not presented as an image-generation card; find it only under the separate workflow section. |
| C15 — Studio as a whole: a first-time user cannot tell how it works ("непонятно, как работает"). | **PARTIALLY FIXED** — Home tells users to choose a tool and cards explain purpose/cost, but cards are rendered even with no selected photo, and the photo→tool→Generate sequence is not presented as a single guide. The localized guide strings exist but no UI code references them. Home also advertises Expand, Upscale, and Erase although the checked-in server config disables them. | `HomeScreen.kt:145-165,248-260,384-432,572-615`; `app/src/main/res/values-ru/strings_home.xml:39-43`; `StudioViewModel.kt:572-574`; `StudioRoute.kt:134-146,744-781`; `server/src/registry.ts:59-77`; `rg -n "home_guide_title|home_guide_style|home_guide_import|home_guide_export" app/src/main/java/com/gexetr/plinthproductphotos/ui` returned no matches. | `HomeScreen.kt`, `app/src/main/res/values/strings_home.xml`, `app/src/main/res/values-ru/strings_home.xml`; if the three disabled cards remain advertised, also `StudioViewModel.kt`, `server/src/registry.ts`, and `server/src/providers/fal.ts`. | Cold-open with no photo; observe a clear photo→tool→Generate sequence, and verify each advertised image tool is enabled and can complete rather than ending at a disabled Generate button. |

## Home/Studio operation mismatch

`StudioViewModel.REQUEST_OPERATIONS` accepts six operations:
`GENERATED_SCENE`, `RELIGHT`, `RECOLOUR`, `EXPAND`, `UPSCALE`, `ERASE`
(`StudioViewModel.kt:572-587`). The Studio catalogue has the same six rows
(`StudioRoute.kt:744-781`), but an allowlist entry does not mean the server can
run it. The checked-in server registry enables O-07 (Scene), O-08 (Relight),
and O-10 (Recolour), and disables O-09, O-11, and O-12 (`server/src/registry.ts:33-77`).
`StudioRoute.availableRows` hides rows with disabled server quotes
(`StudioRoute.kt:134-146`). Thus the three enabled Studio operations are the
only ones that can run through an enabled Generate action under the
checked-in config; the deployed `/config` response is **UNVERIFIED**.

Home currently advertises seven cards (`HomeScreen.kt:572-615`): Cutout,
Generated Scene, Relight, Expand Canvas, Recolour, Erase Region, and Upscale.

- **Advertised but disabled in the checked-in server:** Expand (O-09) requires
  `canvas_size`, which the client does not send (`server/src/registry.ts:59-65`;
  `StudioViewModel.kt:188-201`); Upscale (O-11) exceeds the premium price cap
  and lacks provider support (`server/src/registry.ts:67-72`); Erase (O-12)
  requires a `mask_url`, which the client does not send
  (`server/src/registry.ts:73-77`; `StudioViewModel.kt:188-201`). Home still
  shows all three cards; the Studio catalogue filters their disabled rows,
  while a direct Home launch can still open a form whose Generate action is
  disabled. These are advertised-but-not-runnable defects, despite their
  presence in the request allowlist.
- **Cutout is not a StudioViewModel operation:** it has its own route and
  request flow; the server registry enables O-02 (`PlayKilnApp.kt:407,490`;
  `CutoutViewModel.kt:238-248`; `server/src/registry.ts:46-49`).
- All rows also require a positive enabled quote, no kill switch/daily cap,
  provider readiness, and a selected photo (`StudioRoute.kt:134-146`; readiness
  constant `:79`).

## W87a–h attribution

No W87-labelled commit exists in the repository history observed for this
audit (`git --no-pager log --all --oneline --grep="W87"` returned no rows;
`HEAD` is `bc14ac3`). The W87 implementation is present as staged/unstaged
working-tree content, so there is no commit hash or isolated commit diff to
attribute. The report files below are uncommitted working-tree records.

- **W87a — directly recorded:** `docs/redesign/studio-shell-w87a.md:7-10`
  records the compact photo strip, two-column grid, separate tool mode, bound
  photo, freeform scene direction, and live-allowlist scope. Current code
  confirms the catalogue and tool mode at `StudioRoute.kt:180-240,424-480`.
  These changes address the Studio shell portions of C1/C2/C4/C8/C9/C12;
  C3/C5's latest-photo behavior is recorded under W87h.
- **W87d — directly recorded:** `docs/redesign/home-toolbox-w87d.md:3-15`
  records the neutral product-photo hero, eight purpose/cost cards, and
  separately placed Listing text / slide-kit workflows. Current code/resources
  confirm this at `HomeScreen.kt:149-165,202-235,248-260,572-615` and
  `app/src/main/res/values-ru/strings_home.xml:4-5,53-56`. This addresses C13/C14 and the
  catalogue portion of C1/C4.
- **W87g — directly recorded:** `docs/redesign/studio-ops-expand-upscale-w87g.md:1-5`
  records adding Expand, Upscale, and Erase to the Studio allowlist and rows;
  current code confirms `StudioViewModel.kt:572-587` and
  `StudioRoute.kt:764-781`. This addresses the live Studio parts of C10/C11.
- **W87h — directly recorded:** `docs/redesign/studio-rewrite-closeout-w87h.md:3-22,32-38`
  records removal of Virtual Model from Home, auto-opening the latest imported
  Cutout photo, six Studio rows, freeform Scene, server-disabled O-09/O-11/O-12,
  and no device run. Current source confirms the seven-card Home list at
  `HomeScreen.kt:572-615` and Cutout auto-selection at
  `CutoutViewModel.kt:238-248,271`. Its closeout explicitly says Expand,
  Upscale, and Erase are not end-to-end runnable (`:7,24-26`).
- **W87b, W87c, W87e — attribution UNVERIFIED by individual worker:** the
  owner brief lists their assignments at
  `docs/redesign/studio-rewrite-owner-w87.md:90-93`, and W87h records the final
  aggregate outcome, but no individual commit/report isolates their edits.
- **W87f — attribution UNVERIFIED:** no W87f-specific change record was found
  in this slot's `docs/redesign/` or Git history. Do not infer ownership from
  current source. W87h's closeout report is present; this audit did not edit
  any `ui/studio/**` file.

## Fix partition (open items only)

These are non-overlapping file sets. No product code was changed by this
audit. Do not dispatch the Studio set until sibling W87h has confirmed its
working files are stable.

| Worker | Open items | Exclusive files to own |
|---|---|---|
| A — Cutout layout and stale copy | C5 preview size; C7 stale refresh instruction | `app/src/main/java/com/gexetr/plinthproductphotos/ui/cutout/CutoutScreen.kt`; `app/src/main/res/values/strings_cutout.xml`, `strings_report.xml`; `app/src/main/res/values-ru/strings_cutout.xml`, `strings_report.xml` |
| B — User-visible job IDs | C6 | `app/src/main/java/com/gexetr/plinthproductphotos/ui/wallet/WalletScreen.kt`; `app/src/main/res/values/strings_wallet.xml`; `app/src/main/res/values-ru/strings_wallet.xml` |
| C — Catalogue truth and Studio device readability | C2, C9, C15; Home still advertises disabled O-09/O-11/O-12 | `app/src/main/java/com/gexetr/plinthproductphotos/ui/home/HomeScreen.kt`; `app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioRoute.kt`; `app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioViewModel.kt`; `app/src/main/res/values/strings_home.xml`, `strings_studio_live.xml`; `app/src/main/res/values-ru/strings_home.xml`, `strings_studio_live.xml`; `server/src/registry.ts`; `server/src/providers/fal.ts` |

## Claim ledger

| Claim | Label | Evidence |
|---|---|---|
| The live Studio request gate accepts six operations: Generated Scene, Relight, Recolour, Expand, Upscale, Erase. | MEASURED | `app/src/main/java/com/gexetr/plinthproductphotos/ui/studio/StudioViewModel.kt:180,572-587` |
| Studio has six request/catalogue rows; Home has seven image cards after W87h removes Virtual Model. | MEASURED | `StudioRoute.kt:744-781`; `HomeScreen.kt:572-615`; `docs/redesign/studio-rewrite-closeout-w87h.md:11` |
| Cutout opens the latest already-imported photo when one exists. | MEASURED | `CutoutViewModel.kt:238-248,271`; Home hero uses the first recent photo at `HomeScreen.kt:244-260`; `docs/redesign/studio-rewrite-closeout-w87h.md:35` |
| The checked-in server enables O-02, O-07, O-08, and O-10, but disables O-09, O-11, and O-12. | MEASURED | `server/src/registry.ts:40-77`; `docs/redesign/studio-rewrite-closeout-w87h.md:7,26,36` |
| Current Home hero wording is neutral and Listing text is outside the image-tool list. | MEASURED | `HomeScreen.kt:149-165,202-235,248-260,572-615`; `app/src/main/res/values-ru/strings_home.xml:4-5,53-56` |
| No W87 implementation commit is present in Git history. | MEASURED | `git --no-pager log --all --oneline --grep="W87"` returned no rows; `git rev-parse --short HEAD` returned `bc14ac3`; `git status --short` shows staged/unstaged W87 source and untracked redesign reports, including `studio-rewrite-closeout-w87h.md`. |
| `:app:compileDebugKotlin` and `:app:assembleDebug` completed successfully on the current tree using the factory Gradle helper. | MEASURED | `node --input-type=module -e "import path from 'node:path'; import { runGradleTasks } from './scripts/lib/gradle.mjs'; const d = path.resolve('apps/2026-10-15-plinth-product-photos'); const r = runGradleTasks(d, [':app:compileDebugKotlin', ':app:assembleDebug']); ..."`; exit `0`, JDK 17, `BUILD SUCCESSFUL in 3s`, 39 tasks `UP-TO-DATE`, helper elapsed `4315 ms`. |
| Current UI presentation, text wrapping, and first-time usability are fixed on a phone. | UNVERIFIED | No device observation was made. A device reviewer must run `adb install -r "D:\\Python\\PlayKiln\\apps\\2026-10-15-plinth-product-photos\\app\\build\\outputs\\apk\\debug\\app-debug.apk"` and perform the phone observations in C1-C15; record device serial, APK hash, and what appeared. |
