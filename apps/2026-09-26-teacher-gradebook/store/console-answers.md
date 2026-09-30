# Play Console answers — Mark Book for Teachers

<!-- kiln:generated console answers - regenerated on every store-kit.py write; edit the pack, not this file -->

Every screen the Play Console asks for on a first upload, in console order, with the exact string to put in each field and the reason behind it. Regenerated from this slot's own pack on every `store-kit.py` run: editing this file changes nothing, so fix the pack and regenerate.

`docs/play-console-walkthrough.md` is the same walk in Russian, rendered from the same model (`scripts/lib/console-form.json`), so the two cannot disagree about the form.

- Console labels are the Russian ones you will see in the console; the values are what to type. Review instructions are English because the console demands English for them.
- A line marked `TODO-HUMAN` is not pasteable yet: it names what the pack is missing.

## How to tell that a declaration is complete

The owner read the `Шаг 1 из 5 … Шаг 5 из 5` strip as a completion counter — one item closed out of five — and concluded the declaration was unfinished, although it had already been saved. Completion is read off the console's own counters, listed below as they were observed.

- **`App content` -> the counter in its header** → `Требуется действие (N)` · `Требуется действие в отношении N декларации` · `Требуются действия (N)` — The number of declarations still waiting for an answer, and the console inflects the wording with that number. A saved declaration is not counted here: it leaves the count at the moment it is saved, so the number never reports it as unfinished.
- **`App content` -> «Обработано» (the list of processed declarations)** → `N обработанных деклараций` · `Последнее изменение: <дата>` — The list of saved declarations. Your row being in it, carrying a `Последнее изменение` date, is the record that the declaration is done — and the date moves when it is saved again.
- **Dashboard -> the app-setup row** → `Выполнено: N из M` — The dashboard's own summary. It is the only `N of M` counter on these screens, and it counts dashboard items rather than wizard steps.
- **`Publishing overview` -> «Changes not yet sent for review»** → `Целевая аудитория и возрастные ограничения — Изменение целевой аудитории и информации о контенте. Целевая возрастная группа: 18 и старше.` — The saved declaration sits here waiting to be submitted. Saving and sending for review are two different actions, and the second one is the owner's: the row in this list means saved, not undone.
- **The declaration's own save button** → `Сохранить` — A disabled `Сохранить` says there is nothing pending, not that the save failed.

`N` and `M` in this table are the shapes the console prints, not this slot's values. The 2026-09-29 session read them on one app (`9 обработанных деклараций`, `Выполнено: 5 из 11`), and printing them as another slot's values sends a human looking for a number that app does not have. Your own numbers are only ever read off the screen: the sheet prints the shape so the counter can be recognised.

The publishing-overview row prints the age group in its own branch's words (`Целевая возрастная группа: 18 и старше.` on the `adults-only` branch), so this table carries the `{audienceStoreRow}` substitution: the generator reads the wording off the branch named by `listing.contentRating.targetAudience` — the field `targetAudience.audiences.<branch>.storeRowAgeRu`. On a branch whose row nobody has read the generator prints `TODO-HUMAN` and names the branch instead of borrowing another branch's words, because another branch's wording is a string that slot does not have on its screen.

**The `Шаг N из M` strip is a map of the wizard, not a completion counter. `Шаг 1 из 5` reads like `one of five done`, but no `N of 5` counter exists anywhere on the page, and the only `из 5` strings there are the step labels themselves. A step's tick survives only until the page is reloaded: it is session state rather than saved data, so its absence says nothing about the declaration.**

Source: observed-console-2026-09-29 · `Требуется действие (1)` / `Требуется действие в отношении 1 декларации`; `9 обработанных деклараций` с `Последнее изменение: 29 сент. 2026 г.`; `Выполнено: 5 из 11`; запись `Целевая аудитория и возрастные ограничения — Изменение целевой аудитории и информации о контенте. Целевая возрастная группа: 18 и старше.` в «Обзор публикации»; погашенная `Сохранить` на сводке; поиск по DOM строки `N из 5` результата не дал, галочка шага сбрасывается перезагрузкой.

### 1. Состояние приложения — App status

Console path: `Play Console → страница приложения`

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| Состояние приложения (App status) | — | the state the console shows on this page: Draft before the first rollout, Published after it | While the status is Draft the opt-in link is not displayed, testers cannot install, and the store page answers 404. |
| Требуются действия (Required actions (the counter)) | — | the console shows one unfinished row per App-content declaration; on a fresh app that is the 10 this guide walks, and the count drops as each of them is answered | This is a work list, not an error: the counter equals the number of declarations this guide walks, and an app with every declaration answered shows zero. |

NEEDS-VERIFICATION: the console's own wording here was not observed; confirm it on the page before submitting.

связка «незавершённые действия на `Контент приложения` → блокируется ли раскат» наблюдалась как счётчик, но не проверялась попыткой раската.

### 2. Основная информация о магазине — Main store listing

Console path: `Магазин → Основная информация о магазине`

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| Название приложения (App name) — en-US | 30 | Mark Book for Teachers | Shown on the store page and in search; capped at 30 characters. 22/30 characters |
| Краткое описание (Short description) — en-US | 80 | Offline gradebook you own: marks, attendance, notes, reports. No account. | Shown in search results beside the title; capped at 80 characters. 73/80 characters |
| Полное описание (Full description) — en-US | 4000 | paste the block below | The only place the app itself is explained; capped at 4000 characters. 3012/4000 characters |
| Контактный адрес эл. почты (Contact email) | — | v.sobolenko.vs@gmail.com | Play publishes this address on the listing; user mail arrives there. (from `listing.contactEmail`) |
| URL политики конфиденциальности (Privacy policy URL) | — | https://telegra.ph/Mark-Book-for-Teachers--privacy-policy-09-26 | Play requires the policy in two places: a public HTTPS URL and inside the app; this is the first. (from `listing.privacyPolicyUrl`) |

**Полное описание (Full description) — en-US — paste:**

```text
A private grade book that lives on your device and belongs to you, not to a school platform.

Every teacher has a second record running alongside whatever the school officially uses — a spreadsheet, a notebook, a folder of marks. Mark Book is that record, kept on your own device, with no account to create and no subscription to keep paying.

WHAT YOU CAN DO

• Classes — create, rename, edit and delete them. Deleting a class takes its roster, marks, attendance, notes and seating with it, and nothing comes back.
• Students — add a roster by hand or import a CSV, edit anyone, mark someone inactive when they leave, or delete them. An import never adds the same student twice.
• Marks — record a score, mark work missing, or exempt a student, per assignment. An empty cell is never written as a zero.
• Categories — weigh coursework, exams and homework however your department does, reorder them, and drop the lowest mark in a category.
• Averages that explain themselves — the overall grade shows the rounding rule, whether missing work counted as zero, and every category it left out and why.
• Attendance — mark a register per day, with one tap for "everyone present".
• Notes and parent contacts — participation notes and a log of who you contacted, when, and what about, with follow-up dates that come back to you.
• Seating plans — set a grid of desks and put students in them.
• Reports — export the grade sheet as CSV, or produce a printable PDF for the whole class or one student, with your rounding and missing-work rules printed on it.
• Backup — write your whole record to one file you keep, and restore it later. Restoring refuses a file it does not recognise instead of changing anything.

FREE, AND THEN ONCE

Free for as long as you like: one class, up to 20 students, one category, and unlimited marks, attendance, notes, contacts and seating.

A single one-time purchase adds as many classes as you teach, more than 20 students in a class, weighted categories and dropping the lowest mark, CSV export, printable PDF reports, and backup and restore. It is not a subscription, and Google Play shows the exact price in your currency before you confirm. The unlock can be restored on a new device.

PRIVACY

No account, no login, no ads, no analytics. Your record is written to this device and sent to us nowhere — we have no server to send it to and no way to see it. Android's own backup feature can copy your record to your personal Google Drive so a lost phone is not a lost term; that copy is in your account, not ours, and you can turn it off in Android settings. See the privacy policy for the two network permissions the Play billing library brings with it.

WHO IT IS FOR

Teachers who already keep their own marks beside the school system, and who want a tool they buy once rather than rent monthly. The app is a teacher's tool, not a pupil's, and is not directed at children.

If you want a record you can hand to the school's system as a file and keep as your own backup, this is it.
```

Press `Сохранить` (Save) to save this screen.

### 3. Политика конфиденциальности — Privacy policy

Console path: `Контент приложения → Политика конфиденциальности`

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| URL политики конфиденциальности (Privacy policy URL) | — | https://telegra.ph/Mark-Book-for-Teachers--privacy-policy-09-26 | The URL must serve the same text the pack ships: Play requires the policy both at a public HTTPS URL and inside the app. (from `listing.privacyPolicyUrl`) |

Press `Сохранить` (Save) to save this screen.

`listing.privacyPolicyUrl` — the same URL `store/privacy.md` is hosted at.

### 4. Реклама — Ads

Console path: `Контент приложения → Реклама` (`.../app-content/ads-declaration`)

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| Реклама (Ads) | — | Нет, мое приложение не содержит рекламу | The declaration has to match the shipped build: verify-slot.mjs --strict-store compares it with the enabled capabilities and fails when they disagree. (from `listing.ads.containsAds`) |

Press `Сохранить` (Save) to save this screen.

`listing.ads.containsAds` = false — Enabled capabilities declare no advertising SDKs.

### 5. Учетные данные — App access

Console path: `Контент приложения → Учетные данные` (`.../app-content/testing-credentials`)

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| Учетные данные (App access) | — | да | The «no» branch asserts that no sign-in and no purchase are required; a slot that sells an unlock cannot assert that. (from `derived.credentials`) |

The dialog carries a certification checkbox: the data given in this declaration provides full access to all features and content of the app, including paid content. Only tick it when the reviewer can actually reach every feature.

The dialog itself states what the reviewer cannot do: they cannot create accounts or use personal accounts to make purchases. Nothing will buy the paid content for them, so the only mechanism is a promo code inside the instructions.

Order for a slot that sells something:
- Создайте продукт и **активируйте** его вариант покупки.
- Создайте промокод.
- Вернитесь в «Учетные данные», выберите «да», откройте диалог «Добавить данные для входа» и вставьте инструкции с кодом, затем отметьте чекбокс сертификации.
- Добавьте аккаунт в «Тестирование лицензий» и погасите код сами.

Observation 2026-09-29: with the certification checkbox cleared, the dialog answered «Couldn't save changes». That is one session's observation, not a documented Google rule; the consequence is that the instructions and the checkbox are saved together, so a cleared box does not save the declaration.

Press `Сохранить` (Save) to save this screen.

Model rule «capability.billing»: The slot sells an unlock, so some content is behind a purchase. The console's «no» branch asserts that paying for content is not required, which this slot cannot assert truthfully; the reviewer therefore needs access given to them, which is a promo code in the instructions.

The dialog fields and the instructions block are in the next section.

#### Диалог «Добавить данные для входа» — The credentials dialog

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| Имя (Name) | 60 | any name you will recognise in the list, e.g. `Google Play review` | The only mandatory field in the dialog; it carries the console's own asterisk. |
| Имя пользователя, электронный адрес или номер телефона (Username, email, or phone number) | 100 | leave empty - this slot has no sign-in | Optional: only the Name field carries the console's mandatory asterisk. |
| Пароль (Password) | 100 | leave empty - this slot has no sign-in | Optional: an empty field does not block saving the declaration. |
| Другие данные для доступа (Other access data) | 500 | paste the block below | The console requires them in **English** and caps the field at 500 characters; measured: **333/500** |

**Другие данные для доступа (Other access data) — paste:**

```text
Mark Book for Teachers: the paid unlock is the only restricted content. The reviewer cannot buy it, so redeem promo code TODO-HUMAN-SET-THE-REAL-PROMO-CODE-XX at https://play.google.com/redeem?code=TODO-HUMAN-SET-THE-REAL-PROMO-CODE-XX with the account that installed the app, then reopen the app. The paid features open immediately.
```

The checkbox in the console's own words: Учетные данные, указанные в этой декларации, обеспечивают полный доступ ко всем функциям и контенту приложения, включая платный

Observation 2026-09-29: with the certification checkbox cleared, the dialog answered «Couldn't save changes». That is one session's observation, not a documented Google rule; the consequence is that the instructions and the checkbox are saved together, so a cleared box does not save the declaration.

Press `Сохранить` (Save) to save this screen.

Replace `TODO-HUMAN-SET-THE-REAL-PROMO-CODE-XX` with one code from the batch before pasting. The batch comes off the promotion page (`Download codes`), lives beside the slot as `promotion_codes.csv`, and is gitignored - a code is a free unlock of what the slot sells, so one never belongs in a commit. The measured length above uses a placeholder 38 characters long, which is longer than the code this console issues, so a real code can only shorten the text. This paragraph is guidance and is deliberately outside the block: everything inside the block is read by Google's reviewer, so nothing inside it may address you.

### 6. Возрастные ограничения — Content rating

Console path: `Контент приложения → Возрастные ограничения`

Screen steps: `1 Категория`, `2 Анкета`, `3 Сводка`.

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| Электронный адрес (Email address) | — | v.sobolenko.vs@gmail.com | The console states the address is used to contact you about content ratings and may be shared with rating bodies and IARC. |
| Категория (Category) | — | Все другие типы приложений | The branch decides it rather than a human, because the category and the ten answers of step 2 are one decision. The observed `notes` branch takes «All other app types» — neither a game nor a social or messaging app — and another branch takes its own radio. (from `listing.contentRating.questionnaire`) |
| Я принимаю условия использования Международной коалиции возрастной классификации (IARC) (I accept the International Age Rating Coalition (IARC) terms of use) | — | tick it | The questionnaire cannot proceed without it: the console will not advance to step 2. |

Press `Далее` (Next) to save this screen.

`listing.contentRating.questionnaire` = 'notes' → the branch's own `category`, and the console's label «Все другие типы приложений» is read off this field. Nothing here is a value written for another app.

NEEDS-VERIFICATION: the console's own wording here was not observed; confirm it on the page before submitting.

шаг 3 не наблюдался; источником помечен его подэкран ниже. Шаг 2 прочитан в консоли 2026-09-29 и наблюдался.

#### Шаг 2 Анкета — Step 2 Questionnaire

**The label column of this screen is not verbatim:** The label column of this screen is NOT the console's verbatim row: it holds the questionnaire section and a short gloss of the question joined with ` · `, not the line the console prints beside the controls. A gloss is a string that is not on the screen; read the row wording in the console before pasting, and replace it here.

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| Скачанное приложение · содержимое с возрастными ограничениями (секс, насилие, грубые выражения, наркотики, азартные игры) (Downloaded app · restricted content (sex, violence, profanity, drugs, gambling)) | — | Нет | The APK ships the interface and nothing else: no sex, violence, profanity, drugs or gambling content is downloaded with it. |
| Общий доступ к пользовательскому контенту · обмен сообщениями, файлами, медиа между пользователями (User content sharing · messages, files, media between users) | — | Нет | The app has no accounts and no transport: what the user types stays in local storage, so there is nothing to send to another user. |
| Интернет-содержимое · продвижение или потоковая передача контента извне, который не входит в скачанный пакет (Online content · promoting or streaming content outside the downloaded package) | — | Нет | Nothing is streamed or fetched: the app is offline by construction, so it promotes no content that is not inside the package. |
| Продвижение или продажа товаров и видов деятельности с возрастными ограничениями (алкоголь, табак, оружие, азартные игры) (Promotion or sale of age-restricted goods or activities (alcohol, tobacco, firearms, gambling)) | — | Нет | The app neither sells nor promotes alcohol, tobacco, firearms or gambling. Its only product, where it sells one, is a digital feature unlock. |
| Другое · передача точного физического местоположения пользователя другим пользователям (Other · sharing the user's precise physical location with others) | — | Нет | The app declares no location permission and has no recipient to share a location with. |
| Другое · покупка цифровых товаров внутри приложения (Other · in-app purchase of digital goods) | — | Да | The slot enables the `billing` capability and sells a one-time unlock through Google Play Billing, which is a digital good: the console requires «Да». |
| Другое · покупки со случайными элементами (покупатель не знает заранее, что получит) (Other · purchases with random elements (a loot box)) | — | Нет | A purchase here is deterministic — the buyer receives exactly what the purchase screen describes — so there is no loot box. The question itself appears only after «Да» on digital goods. |
| Другое · денежные вознаграждения, подарочные карты, конвертируемые вознаграждения, криптовалюта, NFT (Other · monetary rewards, gift cards, convertible rewards, crypto, NFTs) | — | Нет | The app pays the user nothing and issues no tokens, gift cards or digital assets. |
| Другое · веб-браузер или поисковая система (Other · web browser or search engine) | — | Нет | The app opens no arbitrary address and searches nothing: it is neither a browser nor a search engine. |
| Другое · приложение главным образом новостное или обучающее (Other · primarily news or educational) | — | Нет | Google's category is reference and learning material read for its own content. An app that keeps the user's own records is a record-keeping tool, not a source of knowledge. |

Press `Сохранить` (Save) to save this screen.

#### Шаг 3 Сводка — Step 3 Summary

Press `Отправить` (Submit) to save this screen.

NEEDS-VERIFICATION: the console's own wording here was not observed; confirm it on the page before submitting.

шаг не наблюдался: итоговые формулировки и кнопка отправки в консоли этой сессией не прочитаны. Ответ анкеты определяет, какие возрастные группы и территории разрешены, и через API не редактируется — это ручной шаг.

### 7. Целевая аудитория и возрастные ограничения — Target audience and content

Console path: `Контент приложения → Целевая аудитория и возрастные ограничения`

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| Какие целевые возрастные группы у вашего приложения? (Which target age groups does your app target?) | — | tick «От 18 лет» | The console asks it with six check boxes, and child age groups mean Families, a separate policy set. The adults-only branch ticks one group, and the wizard then skips steps 2-4. (from `listing.contentRating.targetAudience`) |
| Запретить доступ к моему приложению пользователям, которые указали в аккаунте Google, что они несовершеннолетние (необязательно) (Restrict access from users who declared a Google account age under 18 (optional)) | — | tick «Запретить доступ к моему приложению пользователям, которые указали в аккаунте Google, что они несовершеннолетние (необязательно)» | The branch named by listing.contentRating.targetAudience answers this box through its own `minorGate` field, and the adults branch ticks it. It closes both the download and the in-app purchase to accounts marked as children. |

Press `Далее` (Next) to save this screen.

`listing.contentRating.targetAudience` = 'adults-only'. The console's own label is «От 18 лет»; the value in `store/listing.json` is a token of `targetAudience.audiences`, not the label, so nothing here has to agree with the console by hand.

The console greys out «О приложении», «Реклама», «Страница приложения»: those tabs carry `aria-disabled="true"` and the class `is-disabled`, a click on one changes nothing, and `Далее` on step 1 opens `Шаг 5 из 5: Сводка` directly. The greying is the console branching on the audience, not a step you failed to complete — those steps are not filled in at all.

`adults-only` ticks it. The console's own wording: «Запретить доступ к моему приложению пользователям, которые указали в аккаунте Google, что они несовершеннолетние (необязательно)».

It closes both the download and the in-app purchase for accounts marked as children, and the console adds that Play may not apply the restriction to users who never declared an age under 18 — an availability limit, not a guarantee.

#### Шаг 5 Сводка — Step 5 Summary

Сводка показывает:
- `Доступ к вашему приложению ограничен для несовершеннолетних пользователей.` — появляется, только если отмечен чекбокс запрета.
- `Целевая возрастная группа вашего приложения: <отмеченные группы>` — та группа, которую вы отметили на шаге 1.
- `Если вы сохраните изменения, они будут добавлены на страницу "Обзор публикации" и готовы к отправке на проверку.` — куда уходит запись.

Press `Сохранить` (Save) to save this screen.

#### Шаг 2 О приложении — Step 2 About the app

NEEDS-VERIFICATION: the console's own wording here was not observed; confirm it on the page before submitting.

на ветке `adults-only` шаг пропускается (вкладка неактивна), поэтому его поля этой сессией не читались. Он существует для веток, которые включают детские или подростковые группы, — там визард идёт по всем пяти шагам, и заполнять его нужно по фактическому поведению приложения.

#### Шаг 3 Реклама — Step 3 Ads

NEEDS-VERIFICATION: the console's own wording here was not observed; confirm it on the page before submitting.

на ветке `adults-only` шаг пропускается (вкладка неактивна), поля не читались. Для остальных веток шаг существует; ответ выводится из `listing.ads.containsAds`, который заполняет `scripts/lib/capabilities.mjs`.

#### Шаг 4 Страница приложения — Step 4 Store listing

NEEDS-VERIFICATION: the console's own wording here was not observed; confirm it on the page before submitting.

на ветке `adults-only` шаг пропускается (вкладка неактивна), поля не читались. Для остальных веток он связывает возрастной рейтинг со страницей магазина.

### 8. Безопасность данных — Data safety

Console path: `Контент приложения → Безопасность данных`

**The label column of this screen is not verbatim:** The label column of this screen is NOT the console's verbatim row: the 2026-09-29 session read the wizard's answers rather than its questions. The field column carries a gloss of the question joined with ` · `, and only the row whose question was actually read — `Эти данные обрабатываются временно?` — carries the console's own wording. Read the row in the console before pasting and replace it here.

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| Сбор и передача данных · собирает или передаёт ли приложение обязательные типы данных (Data collection · does the app collect or share any of the required user data types) | — | Да | «Collect» means transmitting data off the device: a record that stays in a local file is not declared, but a bundled SDK that does transmit is declared even when the app itself does not — which is why a slot with `billing` enabled answers «Да» although the app has no network of its own. (from `listing.dataSafety.collectsData`) |
| Шифрование при передаче · шифруются ли данные, которые приложение собирает (Encryption in transit · is the data the app collects encrypted in transit) | — | Да | The pack sets this field only when there is something to transmit (the capabilities fill it from `dataCollected`), so the answer travels with the first question: an app that collects nothing cannot claim encryption for a transmission that does not exist. (from `listing.dataSafety.encryptedInTransit`) |
| Аккаунты · может ли пользователь создать аккаунт в приложении (Accounts · can a user create an account in the app) | — | В приложении нельзя создать аккаунт | The pack carries no separate 'has accounts' field: the console reads it from the deletion branch, and the pack's own document prints `Does the app support accounts? No` from the same field. The other direction (`hasAccountDeletion` = true) is a different branch of the wizard and was never observed, so the generator answers `TODO-HUMAN` rather than a second label. (from `listing.dataSafety.hasAccountDeletion`) |
| Удаление данных · предусмотрен ли способ запросить удаление данных (Data deletion · is there a way for users to request that their data is deleted) | — | Нет | This is the same pack answer as the account row, for the same reason: a deletion mechanism is needed only where accounts exist and the developer stores data. A factory slot has neither, and the pack's own document prints `Not applicable — no accounts` from this same field. (from `listing.dataSafety.hasAccountDeletion`) |
| Категории данных · какие типы данных собираются, обязательны ли они и с какой целью (Data categories · which data types are collected, whether they are required, and why) | — | Финансовые сведения → История покупок: «Собирает», «Сбор данных обязателен…», purpose «Функции приложения» | A category row is the bundled SDK rather than the app's own code: with `billing` enabled the Play Billing library creates it, and under-declaring it is a policy violation. The pack's type and purpose are English tokens while the screen shows Russian labels, so the model's vocabulary names the pair. (from `listing.dataSafety.dataTypes`) |
| Эти данные обрабатываются временно? (Is this data processed ephemerally?) | — | TODO-HUMAN: «Эти данные обрабатываются временно?» has no answer in this pack: `listing.dataSafety` describes what is collected, not how long it is kept or how it is processed, and «processed ephemerally» is a claim about how Google Play handles a purchase inside its own systems. Answering it from the pack would be asserting a retention rule nobody checked, so the row names itself instead. | The console asks about ephemeral processing, and «ephemeral» is a claim about how a purchase is processed inside Google Play. The pack describes neither a retention window nor the processing, so this row carries a `TODO-HUMAN` naming itself: a plausible default would be a claim nobody checked. (from `listing.dataSafety`) |

Press `Сохранить` (Save) to save this screen.

`listing.dataSafety.collectsData` = true — Derived from the enabled capabilities and their bundled SDK declarations.

The transmission that makes this «Да» comes from the bundled Google Play Billing Library, not from the app's own code; `listing.dataSafety.dataTypes` is the row it needs.

`listing.dataSafety.encryptedInTransit` = true — the same answer `store/data-safety.md` writes out as «Is collected data encrypted in transit?»

`listing.dataSafety.hasAccountDeletion` = false, and the pack's own `store/data-safety.md` prints «Does the app support accounts? No» from the same field: the sheet and the document cannot say different things about accounts.

`listing.dataSafety.hasAccountDeletion` = false — the same pack field as the account row, for the same reason: a deletion mechanism is needed only where accounts exist and the developer stores what they hold.

`Финансовые сведения` → `История покупок`: Purchase history: product IDs the user bought and their entitlement state. Payment instruments are handled entirely by Google Play and never reach the app.

«Эти данные обрабатываются временно?» has no answer in this pack: `listing.dataSafety` describes what is collected, not how long it is kept or how it is processed, and «processed ephemerally» is a claim about how Google Play handles a purchase inside its own systems. Answering it from the pack would be asserting a retention rule nobody checked, so the row names itself instead.

This is an open question for the owner rather than a missing reading: the row was observed, and it was recorded with no slot key because no pack field could answer it.

NEEDS-VERIFICATION: the console's own wording here was not observed; confirm it on the page before submitting.

дословных формулировок вопросов этой формы в модели нет: прочитаны ответы и метки вариантов на экране, а не текст самих вопросов (кроме строки `Эти данные обрабатываются временно?`). Дословные названия пяти шагов визарда тоже не выписаны. Перед вставкой сверьте подписи строк на экране и замените пересказ на прочитанный текст.

### 9. Рекламный идентификатор — Advertising ID

Console path: `Контент приложения → Рекламный идентификатор`

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| Рекламный идентификатор (Advertising ID) | — | No | Answering «no» is a claim about the contents of the AAB. Adding an SDK that reads the advertising ID later means changing this declaration together with the build. (from `capability.ads`) |

Press `Сохранить` (Save) to save this screen.

`capability.ads` is not enabled — no advertising or analytics SDK in the build reads the identifier, which is what `listing.ads.containsAds` already declares on the Ads screen.

NEEDS-VERIFICATION: the console's own wording here was not observed; confirm it on the page before submitting.

точные формулировки вариантов в консоли не наблюдались в этой сессии.

### 10. Приложения государственных учреждений — Government apps

Console path: `Контент приложения → Приложения государственных учреждений`

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| Приложения государственных учреждений (Government apps) | — | No | A «yes» asserts a government affiliation and moves the app into a different review path, and this factory ships no such app: the «no» is a property of the catalogue, not one slot's choice. |

Press `Сохранить` (Save) to save this screen.

`constant.no` — the answer is a property of the catalogue, not of this slot, so the model declares it once and «Приложения государственных учреждений» reads it from there. The console's own wording for this radio has not been read in any session: this screen carries NEEDS-VERIFICATION for that reason, and the cell prints the model's gloss of the token rather than a string pretending to be the console's label.

NEEDS-VERIFICATION: the console's own wording here was not observed; confirm it on the page before submitting.

точные формулировки вопроса и вариантов в консоли не наблюдались в этой сессии.

### 11. Финансовые функции — Financial features

Console path: `Контент приложения → Финансовые функции`

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| Финансовые функции (Financial features) | — | No | A factory slot sells at most an in-app unlock and Google Play handles the payment itself, which does not make the app a financial product: the «no» is a property of the catalogue. |

Press `Сохранить` (Save) to save this screen.

`constant.no` — the answer is a property of the catalogue, not of this slot, so the model declares it once and «Финансовые функции» reads it from there. The console's own wording for this radio has not been read in any session: this screen carries NEEDS-VERIFICATION for that reason, and the cell prints the model's gloss of the token rather than a string pretending to be the console's label.

NEEDS-VERIFICATION: the console's own wording here was not observed; confirm it on the page before submitting.

точные формулировки вопроса и вариантов в консоли не наблюдались в этой сессии; отдельно не проверено, считает ли Play встроенную покупку «финансовой функцией» в этой форме.

### 12. Приложения для здоровья — Health apps

Console path: `Контент приложения → Приложения для здоровья`

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| Приложения для здоровья (Health apps) | — | No | Factory slots are work tools: they measure no health data and handle no medical records, so the «no» is a property of the catalogue. |

Press `Сохранить` (Save) to save this screen.

`constant.no` — the answer is a property of the catalogue, not of this slot, so the model declares it once and «Приложения для здоровья» reads it from there. The console's own wording for this radio has not been read in any session: this screen carries NEEDS-VERIFICATION for that reason, and the cell prints the model's gloss of the token rather than a string pretending to be the console's label.

NEEDS-VERIFICATION: the console's own wording here was not observed; confirm it on the page before submitting.

точные формулировки вопроса и вариантов в консоли не наблюдались в этой сессии.

### 13. Создание выпуска — Create release

Console path: `Тестирование → Внутреннее тестирование → Создать выпуск`

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| Имя выпуска (Release name) | — | 0.1.0 (1) | An internal Play Console label, never shown to a user: it exists so the console's release list can tell one build from another. |
| Заметки о выпуске (Release notes) | — | paste the block below | Play shows these on the store page and truncates silently at 500 characters per language, so the block is measured before pasting. |

**Заметки о выпуске (Release notes) — paste:**

```text
<en-US>
First release. A private offline grade book: classes and rosters, marks per assignment with weights and dropped lowest, attendance, notes, parent contacts and seating — on your device, no account.

Every average shows its working: the rounding rule, whether missing work counted as zero, and each excluded category.

Export CSV or a printable PDF, and back up the whole record to a file you keep.

Free for one class up to 20 students; one purchase unlocks the rest.
</en-US>
<ru-RU>
Первый выпуск. Приватный офлайн-журнал оценок: классы и списки, оценки за работы с весами и отбрасыванием низшей, посещаемость, заметки, контакты с родителями и рассадка — на вашем устройстве, без аккаунта.

Каждое среднее показывает расчёт: правило округления, считалась ли пропущенная работа нулём и какие категории исключены.

Экспорт в CSV и PDF, резервная копия всей записи в файл у вас.

Бесплатно: один класс до 20 учеников; одна покупка открывает остальное.
</ru-RU>
```

Press `Опубликовать выпуск` (Start rollout) to save this screen.

### 14. Обзор публикации — Publishing overview

Console path: `Обзор публикации`

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| Обзор публикации (Publishing overview) | — | `Save`, then `Send app for review` | Two different buttons: Save only records the change, and the second one submits it; while only Save is pressed the console still lists the changes as unfinished. |

Press `Отправить приложение на проверку` (Send app for review) to save this screen.

### 15. Встроенные продукты — In-app products

Only for slots whose `billing` capability is enabled.

Console path: `Монетизация с помощью Play → Продукты → Встроенные продукты → Создать продукт` (`.../one-time-products → .../one-time-products/create`)

Screen steps: `1 Сведения о продукте`, `2 Доступность и цены`.

Where in the console (English): Monetize with Play -> Products -> In-app products

Why the product comes before the code:
- Create the product (step 1 «product details», step 2 «availability and prices») with exactly the ID the sheet prints.
- Press «Активировать» (Activate) - not «Сохранить как проект» (Save as draft).
- Create the promo code and return to the App access declaration with the code in the instructions.
- Add the license tester and make one test purchase yourself.

Step 2 has two save buttons and only one makes the option live. «Активировать» (Activate) publishes the option and makes it purchasable; «Сохранить как проект» (Save as draft) leaves it a draft, and a draft is not returned to the app - the app then behaves as if the product did not exist, and the cause gets hunted in the code. It is the same pair as «Сохранить как проект» versus «Опубликовать выпуск» on the release screen.

The product must carry exactly one purchase option. A second option breaks the purchase rather than adding a choice.

This repository's billing controller builds the flow as BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build() with no setOfferToken. The Play Billing Library accepts that form only when the product has a single purchase option; with two, the token is required and the controller never passes one.

The rule's reason in the shipped code: capabilities/billing/src/main/kotlin/com/gexetr/capability/billing/PlayBillingController.kt, launchPurchase, the BillingFlowParams builder around line 180

This slot gates some content, so the checkbox is only honest once the instructions carry a code that reaches it and you have redeemed that code yourself.

Activate the product's purchase option. Play documents three states and only one is purchasable: Draft — «Purchase options in this state aren't returned by the Play Billing Library (PBL)»; Active — returned; Inactive — not returned.

The page carries an info box: promo codes can only be created for subscriptions, in-app content, or paid apps - which is why the code is created after the product, never before it.

Press `Активировать` (Activate) to save this screen.

#### Шаг 1 Сведения о продукте — Step 1 Product details

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| Идентификатор товара (Product ID) | 141 | paste the block below | must start with a digit or a lowercase letter; may contain digits, lowercase letters, underscores and dots. Once the product exists, its ID can never be changed or reused. |
| Теги (Tags) | 20 | paste the block below | A tag starts with a lowercase letter and may contain digits (0-9), lowercase letters (a-z) and hyphens (-); the cap is 20 characters. |
| Имя (Name) | 55 | Mark Book for Teachers | shown in the cart, so the buyer reads this string at the moment of payment. |
| Описание (Description) | 200 | Offline gradebook you own: marks, attendance, notes, reports. No account. | shown to the buyer next to the price. |
| Значок (Icon) | — | store/assets/icon-512.png | The console's product-icon rule is a 32-bit PNG up to 8 MB, 1:1 aspect, each side 512-1080 px. The slot's own store icon already satisfies it, so the same file can be uploaded here. |
| Категория налогообложения товара (Product tax category) | — | leave the default, `Sale of digital applications` | defaults to the sale of digital applications; for this factory's slots that default is correct and needs no change. |
| Возрастное ограничение (Age restriction) | — | leave it empty | shown only in some countries (observed for the US); optional, leave it empty. |
| Ограничение способов оплаты по местоположению (Payment-method restriction by location) | — | leave it unrestricted | no restrictions by default; nothing to change. |

**Идентификатор товара (Product ID) — paste:**

```text
recordbook_pro
```

**Теги (Tags) — paste:**

```text
offline
productivity
grades
attendance
teacher-tools
```

Press `Далее` (Next) to save this screen.

'teacher tools' is entered as 'teacher-tools' to satisfy the console's tag rule.

#### Шаг 2 Доступность и цены — Step 2 Availability and prices

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| Идентификатор способа покупки (Purchase option ID) | 63 | paste the block below | must start with a digit or a lowercase letter and may contain only digits, lowercase letters and hyphens - so, unlike the product ID above, underscores and dots are not allowed here. |
| Тип покупки (Purchase type) | — | leave the default, `Buy` | a dropdown that defaults to «buy»; for a one-time unlock that default is correct. |
| Теги (Tags) | 20 | paste the same block as «Теги (Tags)» above | A tag starts with a lowercase letter and may contain digits (0-9), lowercase letters (a-z) and hyphens (-); the cap is 20 characters. |
| Расширенные настройки (Advanced settings) | — | leave collapsed | collapsed by default; nothing to change when the product has a single purchase option. |
| Доступность (Availability) | — | leave all countries and regions | all countries and regions are available by default; nothing to change. |

**Идентификатор способа покупки (Purchase option ID) — paste:**

```text
recordbook-pro
```

Press `Активировать` (Activate) to save this screen.

Product 'recordbook_pro' takes the purchase-option ID 'recordbook-pro': unlike a product ID, an option ID may not contain underscores or dots.

#### Цена — выпадающая метка `Set prices` — Price - the `Set prices` dropdown

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| Set prices | — | paste the block below | economics.json stores a dot-decimal, and the Russian console's price field accepts only the comma form; the `Set prices` label itself stays untranslated. |

**Set prices — paste:**

```text
6,99
```

In the Russian console this dropdown label stays untranslated, so the price field reads `Set prices`.

The order that worked:
- Open the `Set prices` dropdown.
- Choose the bulk-edit action for all prices.
- Tick the checkbox that selects every country and region.
- Type the price in the comma form (see the locale rule below).

economics.json stores the price as a dot-decimal (priceUsd 6.99). The price field in the Russian console accepts the comma form (6,99). The sheet prints the string to paste; the data file keeps its dot-decimal format and only the rendering changes.

Press `Сохранить` (Save) to save this screen.

#### Теги — поле шага 1 и шага 2 — Tags

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| Теги (Tags) | — | paste the same block as «Теги (Tags)» above | A tag starts with a lowercase letter and may contain digits (0-9), lowercase letters (a-z) and hyphens (-); the cap is 20 characters. |

The slot's tags are converted to the console's form deterministically: lowercased, with spaces and other disallowed characters turned into hyphens, truncated to the cap, and empties dropped. The sheet prints what changed next to the ready tags.

NEEDS-VERIFICATION: whether tags affect store search reach was not measured.

Press `Сохранить` (Save) to save this screen.

### 16. Тестирование лицензий — License testing

Only for slots whose `billing` capability is enabled.

Console path: `Настройки → Тестирование лицензий`

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| Тестировщики лицензий (License testers) | — | TODO-HUMAN: «Тестировщики лицензий» — no field of the pack carries it: a license tester is the owner's own account, and only the owner can name it | An account that is only an internal tester on a track is not a license tester, and a purchase made by it is charged real money. |

Where in the console (English): Settings -> License testing

Add the account that will test purchases to Play Console -> Settings -> License testing. An account that is only an internal tester on a track is not a license tester, and a purchase made by it is charged real money.

Press `Сохранить` (Save) to save this screen.

### 17. Promotions — Promotions

Only for slots whose `billing` capability is enabled.

Console path: `Монетизация с помощью Play → Promotions` (`.../promotions`)

| Console field (as shown) | Limit | What to type | Why |
|---|---:|---|---|
| Дата начала (Start date) | — | TODO-HUMAN: «Дата начала» — no field of the pack sets this date: it is the day you create the promotion | The window is counted from it: the console refuses a period longer than 365 days. |
| Дата окончания (End date) | — | no later than 365 days after the start | Documented ceiling: a promotion lasts at most one year; with a 2026-09-29 start 2027-09-28 passes while 2027-10-01 is refused with `The period between the start and end date must not exceed 365 days.` |
| Количество кодов (Number of codes) | — | TODO-HUMAN: «Количество кодов» — no field of this slot's pack carries it: the batch size is your decision and not a fact about the app, and a batch recorded for one app (the 2026-09-29 observation) would be that slot's answer printed as this one's. Take the number from what this promotion has to hand out, remembering that the console will not let you change it after «Сохранить». | Up to 500 promo codes per quarter across all non-subscription promotions; the count cannot be changed once saved. |
| Тип промоакции (Promotion type) | — | a one-time product, not a subscription | The type cannot be changed after saving, and codes exist only for subscriptions, in-app content or paid apps, which is why the product comes first. |

The page carries an info box: promo codes can only be created for subscriptions, in-app content, or paid apps - which is why the code is created after the product, never before it.

Observed: while the product list was empty the page showed no create button.

OBSERVATION 2026-09-29, question closed: a promo code **is** created and activated for an app with no reviewed release. The owner issued a batch of 100 codes while the app was still a draft and the App content declarations were unfinished. This entry used to read NEEDS-VERIFICATION; it no longer does.

Documented duration ceiling: a promotion lasts at most one year - *Promotions can last up to one year*. The console counts the days between the start and end dates and refuses the save with `Период между датой начала и датой окончания не должен превышать 365 дней.` (The period between the start and end date must not exceed 365 days.). With a 2026-09-29 start, an end of 2027-09-28 passes while 2027-10-01 is refused (367 days).

Documented quantity ceiling: up to 500 promo codes per quarter across all non-subscription promotions in an app (subscription codes do not count against it). Unused codes do not carry over to the next quarter, and on the first day of a quarter new codes can be unavailable for up to 24 hours.

Once saved, the code count and the promotion type cannot be changed, and an end date can only be moved to a future date. The code count is therefore a final decision, taken before pressing save.

Codes come off the promotion page's `Download codes` button as a file named `promotion_codes.csv` (this repository's convention for the same file is `promo-codes.csv`; both names are in `.gitignore`). Download them immediately: after the end date the page stops showing them - *After the promo code is expired, users can't redeem the code, and you can't view or update the code in Play Console*. The file is **never committed**: a code is a free unlock of whatever the slot sells, and a repository history is the wrong place for one.

Documented redemption: `https://play.google.com/redeem?code=<CODE>` opens the Play Store with the code already filled in, or the code can be typed into the Play Store by hand. There is no in-app redemption screen and none is needed: the purchase is picked up on the next billing connection - `restore()` and `acknowledgePendingPurchases()` in capabilities/billing/src/main/kotlin/com/gexetr/capability/billing/PlayBillingController.kt. That is why the reviewer instruction ends with «reopen the app».

OPEN QUESTION, not a fact: Google states both *Users can redeem promo codes on the Google Play up until the promotion end date* and *Unused codes: If you don't use all of your promo codes in a quarter, you'll lose access to them*. The two do not settle whether the quarter spends the quota or expires the codes themselves. The only consequence is to redeem a code right after issuing it rather than at the end of the window.

Source: Documented: support.google.com/googleplay/android-developer/answer/6321495 (Create promotions) — the one-year ceiling, the 500-per-quarter limit, the CSV download, the immutability of a saved promotion, the redemption window; developer.android.com/google/play/billing/promo — the deep link `play.google.com/redeem?code=`, redemption from the Play Store, and the requirement that the app handle redemptions made outside it. Observed-console-2026-09-29: the empty-list info box, the 365-day refusal text, the promotion created while the app was a draft, and the promotion status chip.

Press `Сохранить` (Save) to save this screen.

## Machine-readable claims — do not paste

`store-kit.py --validate-only` reads this block and fails when it disagrees with `listing.json`, so a sheet left behind by an older pack cannot pass as current.

```kiln-console-claims
{
  "ads": false,
  "appName": "Mark Book for Teachers",
  "collectsData": true,
  "contactEmail": "v.sobolenko.vs@gmail.com",
  "contentRatingQuestionnaire": "notes",
  "credentialsAnswer": "yes",
  "dataTypes": [
    "Financial info"
  ],
  "locales": [
    "en-US"
  ],
  "privacyPolicyUrl": "https://telegra.ph/Mark-Book-for-Teachers--privacy-policy-09-26",
  "productIds": [
    "recordbook_pro"
  ],
  "productPriceDisplay": "6,99",
  "productTags": [
    "offline",
    "productivity",
    "grades",
    "attendance",
    "teacher-tools"
  ],
  "purchaseOptionIds": [
    "recordbook-pro"
  ],
  "releaseName": "0.1.0 (1)",
  "schemaVersion": 1,
  "slug": "teacher-gradebook",
  "targetAudience": "adults-only"
}
```
