# Play Console runbook — Plinth AI: Product Photos

**Plinth AI: Product Photos** · `com.gexetr.plinthproductphotos` · **0.1.1 (2)**

_Turn a seller's own product photos into per-marketplace listing cards: server cut-out, original-pixel composition, marketplace rule linter, export with provenance._

This is this app's own submission document: the console controls to press, in order, with the answer already derived from the slot's declarations. It is not a copy of the pack — the pack's prose lives in `store/listing.md`, `store/data-safety.md` and `store/review-notes.md`. The global reference for every field and option is `docs/play-console-checklist.md`; this file does not restate its caps, and every character cap it names is resolved in `scripts/store/model.py` / `scripts/lib/play-assets.json`.

- Enabled capabilities: `billing` (`capabilities.json`)
- Reward path: `purchase_led_repeat_sales` (`economics.json`)
- Recorded name decision: `Plinth AI: Product Photos` (`naming.json`)
- Page markers: `[console]` a console action, `[verify]` a read-only check, `[human]` the owner's own act.

---

## 1. Preconditions — clear these or the submit control stays disabled

While any dashboard task is open, the publishing overview's only control stays disabled and prints `Чтобы отправить изменения на рассмотрение, выполните необходимые действия на панели управления приложением.`

- [ ] `[human]` **The bundle file is in the app's library.** `Выпуски` → the release form's `Наборы App Bundle` → the neighbouring `Загрузить` control takes the AAB file itself, so if this version is not yet listed, the owner uploads it once — see §6.
- [ ] `[console]` **The two first-publication declarations.** `Панель управления` → the `Заявки` dialog → tick `Подтвердить, что приложение соответствует Правилам программы для разработчиков` and `Принять экспортное законодательство США`. The agent ticks them **only when the slot carries a recorded release approval**, because the approval *is* the human checkpoint and the attestation is the owner's, not the click (`docs/play-console-checklist.md` §1.A — the owner's decision, recorded 2026-10-03). Accepted once per app; nothing in the pack carries them.
- [ ] `[human]` **The five store-graphic fields.** `Магазин → Основная информация о магазине` → upload the icon, the feature graphic and the phone / 7-inch / 10-inch screenshot sets — see §3. The sandboxed browser cannot set a `file` input, so this upload is the owner's.
- [ ] `[verify]` **The privacy-policy URL is real and reachable.** The listing carries `https://telegra.ph/Plinth-Product-Photos--privacy-policy-10-15` (`store/listing.json.privacyPolicyUrl`); the same URL must open from inside the app (`scripts/check-in-app-policy.mjs`).
- [ ] `[verify]` The dashboard counter reads `Выполнено: N из 11` — nine `Контент приложения` rows plus the two store rows (`Выберите категорию приложения и укажите контактную информацию`, `Настройте страницу приложения`), and it deliberately omits `Рекламный идентификатор`. It is **not** the declaration count (§2).

---

## 2. App-content declarations — `Контент приложения → Обзор`

Two tabs: `Требуются действия` (default) and `Обработано`, whose heading prints `N обработанных деклараций`. Ten declarations, each row's action `Настроить`, listed below in the console's own screen order.

### Безопасность данных — `Контент приложения → Безопасность данных`

| Form field | Answer | Source / reason |
|---|---|---|
| `Сбор и передача данных` (`collectsOrShares`) | **Да** | `store/listing.json.dataSafety.collectsData` — Derived from the enabled capabilities and their bundled SDK declarations. |
| `Шифрование при передаче` (`encryptedInTransit`) | **Да** | `store/listing.json.dataSafety.encryptedInTransit` |
| `Аккаунты` (`accountCreation`) | **Нет** | `store/review-notes.md` — the app creates no account and has no sign-in |
| `Удаление данных` (`dataDeletion`) | **Механизм не требуется** | There is no app-side account to delete; stored records are editable and deletable in the app |
| `Эти данные обрабатываются временно?` (`temporaryProcessing`) | **Нет** | The entitlement is kept on the device so the unlock survives a reinstall (`store/data-safety.md`) |

`Категории данных` (`dataCategories`) — one row per declared type:

| Category | Collected | Shared | Required | Purpose |
|---|---|---|---|---|
| Financial info | Yes | No | Yes | App functionality |
| Photos and videos | Yes | Yes | No | App functionality |
| Personal info | Yes | Yes | No | App functionality |
| Device or other IDs | Yes | No | Yes | App functionality |
| Other user-generated content | Yes | Yes | No | App functionality |

The rows above are the bundled SDKs, not the app's own code: Google Play Billing Library 9.1.0. Under-declaring a bundled SDK is a policy defect, so this list and the built AAB must agree.

- [ ] `[console]` Answer the form sections above, then save the declaration.

### Приложения для здоровья — `Контент приложения → Приложения для здоровья`

- [ ] `[console]` Answer **Нет** — Factory slots are work tools: they measure no health data and handle no medical records, so the «no» is a property of the catalogue rather than of this app (the archived console reading's `health` declaration).

### Финансовые функции — `Контент приложения → Финансовые функции`

- [ ] `[console]` Answer **Нет** — A factory slot sells at most an in-app unlock and Google Play handles the payment itself, which does not make the app a financial product (the archived console reading's `financial` declaration).

### Приложения государственных учреждений — `Контент приложения → Приложения государственных учреждений`

- [ ] `[console]` Answer **Нет** — A «yes» asserts a government affiliation and moves the app into a different review path, and this factory ships no such app (the archived console reading's `government` declaration).

### Рекламный идентификатор — `Контент приложения → Рекламный идентификатор`

- [ ] `[console]` Answer **Нет** — `store/listing.json.ads.containsAds` is false and no enabled capability ships an advertising SDK. Answering «no» is a claim about the contents of the AAB: adding such an SDK later means changing this declaration with the build.

### Целевая аудитория и возрастные ограничения — `Контент приложения → Целевая аудитория`

- [ ] `[console]` `Какие целевые возрастные группы у вашего приложения?` → tick `От 18 лет`.
- [ ] `[console]` Tick the optional minor-access checkbox `Запретить доступ к моему приложению пользователям, которые указали в аккаунте Google, что они несовершеннолетние` — the branch ticks it because the declared audience is adults. It closes both the download and the in-app purchase to accounts marked as children.
- [ ] `[verify]` On this branch the wizard **skips** `О приложении`, `Реклама`, `Страница приложения`: `Далее` opens `Сводка` directly. Two screens are filled, not five — a run that walks four steps has answered for a different app.
  - Branch source: `store/listing.json.contentRating.targetAudience = "adults-only"`.
  - The app is not intended for children; entering Families pulls in a separate policy set.
- [ ] `[verify]` Step 1 field `Электронный адрес` = `v.sobolenko.vs@gmail.com` (`store/listing.json.contactEmail`).

### Возрастные ограничения — `Контент приложения → Возрастные ограничения`

Strip: `1 Категория` / `2 Анкета` / `3 Сводка`.

- [ ] `[console]` Step 1 `Электронный адрес` = `v.sobolenko.vs@gmail.com`; tick the IARC checkbox `Я принимаю условия использования Международной коалиции возрастной классификации (IARC)` — without it the console does not open step 2.
- [ ] `[console]` Step 1 category radio → **`Все другие типы приложений`** (the `notes` branch, `store/listing.json.contentRating.questionnaire`).
- [ ] `[console]` Step 2 `Анкета` — answer the ten questions:

  | Question | Answer | Reason |
  |---|---|---|
  | Скачанное приложение · содержимое с возрастными ограничениями | **Нет** | The package ships the interface and nothing else: no sex, violence, profanity, drugs or gambling content. |
  | Общий доступ к пользовательскому контенту · обмен сообщениями, файлами, медиа | **Нет** | The app has no accounts and no transport, so what the user types stays in local storage. |
  | Интернет-содержимое · продвижение или потоковая передача контента извне | **Нет** | Nothing is streamed or fetched: the app is offline by construction. |
  | Продвижение или продажа товаров и видов деятельности с возрастными ограничениями | **Нет** | The app neither sells nor promotes alcohol, tobacco, firearms or gambling; its only product is a digital feature unlock. |
  | Другое · передача точного физического местоположения пользователя | **Нет** | The app declares no location permission and has no recipient to share a location with. |
  | Другое · покупка цифровых товаров внутри приложения | **Да** | The `billing` capability sells a one-time unlock through Google Play Billing, which is a digital good, so the console requires «Да». |
  | Другое · покупки со случайными элементами (лутбоксы) | **Нет** | A purchase is deterministic: the buyer receives exactly what the purchase screen describes. |
  | Другое · денежные вознаграждения, подарочные карты, криптовалюта, NFT | **Нет** | The app pays the user nothing and issues no tokens, gift cards or digital assets. |
  | Другое · веб-браузер или поисковая система | **Нет** | The app opens no arbitrary address and searches nothing. |
  | Другое · приложение главным образом новостное или обучающее | **Нет** | It is a record-keeping tool rather than a source of knowledge or news. |

  - `q-random-elements` appears only after «Да» on the digital-goods row; answer it **Нет** — a purchase here is deterministic.
- [ ] `[verify]` Step 3 `Сводка` is what `store/listing.json.contentRating` is checked against; confirm it matches the table above before saving.

### Учетные данные — `Контент приложения → Учетные данные`

Radio `В вашем приложении есть закрытые разделы?` (`hasRestrictedSections`).
- [ ] `[console]` Choose **`Да`** and tick the line the credential dialog explains *this* app through — `Платежи, например за контент для однократных покупок, подписки или уровни доступа.` The «no» branch asserts no payment is required, which would be false here.
  - Rule (`CREDENTIALS_RULES`): The slot sells an unlock, so some content is behind a purchase. The console's «no» branch asserts that paying for content is not required, which this slot cannot assert truthfully; the reviewer therefore needs access given to them, which is a promo code in the instructions.
- [ ] `[console]` Open `Добавить данные для входа` and put the **promo code** in `Другие данные, которые требуются для входа` (not in the username/password fields). The code itself is harvested from the console, never from the pack.
- [ ] `[console]` Tick the certification checkbox `Учетные данные, указанные в этой декларации, обеспечивают полный доступ ко всем функциям и контенту приложения, включая платный` — instructions and the checkbox save **together**; without it the dialog answers `Не удалось сохранить изменения`.
- [ ] `[verify]` Create the promo code first (§4): `Монетизация с помощью Play → Promotions` refuses while the product list is empty.
- [ ] `[console]` The reviewer's own instructions are `store/review-notes.md` § "Access"; it is generated from the same rule, so the page and the pack state one answer.

### Реклама — `Контент приложения → Реклама`

- [ ] `[console]` Radio `Реклама` → **`Нет, мое приложение не содержит рекламу`** — `store/listing.json.ads.containsAds` is `false`; Enabled capabilities declare no advertising SDKs.

### Политика конфиденциальности — `Контент приложения → Политика конфиденциальности`

- [ ] `[console]` Field `URL политики конфиденциальности` = `https://telegra.ph/Plinth-Product-Photos--privacy-policy-10-15`.
- [ ] `[verify]` The same URL must be reachable from inside the app; the console's own page states the link is required in both places. `node scripts/check-in-app-policy.mjs --slot plinth-product-photos` measures the in-app half.

---

## 3. Store listing — `Магазин → Основная информация о магазине`

The text fields an agent pastes, with the value and where it comes from:

| Console field | Value for this app | Source |
|---|---|---|
| `Категория приложения` (`category`) | token `PHOTOGRAPHY` — **NEEDS-DECISION** (§7) | `store/listing.json.category` |
| `Контактный адрес электронной почты` (`contactEmail`) | `v.sobolenko.vs@gmail.com` | `store/listing.json.contactEmail` |
| `URL политики конфиденциальности` (`privacyPolicyUrl`) | `https://telegra.ph/Plinth-Product-Photos--privacy-policy-10-15` | `store/listing.json.privacyPolicyUrl` |
| `Название приложения *` (`title`) [en-US] | `Plinth AI: Product Photos` | `store/listing.json.locales.en-US.title`; measured against the cap in `store/listing.md` |
| `Краткое описание *` (`shortDescription`) [en-US] | Create product photos, check marketplace rules, and export listing-ready images. | `store/listing.json.locales.en-US.shortDescription` |
| `Полное описание *` (`fullDescription`) [en-US] | paste the `### Full description` block of `store/listing.md` | `store/listing.json.locales.en-US.fullDescription`; the cap is resolved in `scripts/store/model.py` |

**The five image fields are the owner's `[human]` uploads** (Preconditions §1); the pack already carries every file:

| Console field | Pack file |
|---|---|
| `Значок приложения *` (`icon`) | `store/assets/icon-512.png` and `store/assets/<locale>/icon-512.png` |
| `Картинка для описания *` (`featureGraphic`) | `store/assets/feature-graphic-1024x500.png` and its per-locale copy |
| `Скриншоты для смартфона *` (`phoneShots`) | `store/assets/<locale>/phone-01.png` … `phone-06.png` |
| `Скриншоты для семидюймовых планшетов *` (`tablet7Shots`) | `store/assets/<locale>/tablet-7-01.png` … `tablet-7-04.png` |
| `Скриншоты для 10-дюймового планшета *` (`tablet10Shots`) | `store/assets/<locale>/tablet-10-01.png` … `tablet-10-04.png` |

- [ ] `[verify]` The optional fields stay empty on purpose: `URL проморолика на YouTube`, `Скриншоты на компьютере`, `Скриншоты для Android XR` and both XR video fields (`docs/play-console-checklist.md` §8).

---

## 4. Monetization — the in-app product

The app's own billing code expects these product IDs — read from the shipped Kotlin by `declared_product_ids()`, not typed here: `plinth_pro_600`, `plinth_starter_90`, `plinth_studio_230`, `plinth_unlock`.

- [ ] `[console]` `Монетизация с помощью Play → Встроенные продукты` → create the product:

  | Step | Field | Value |
  |---|---|---|
  | 1 | `Идентификатор товара` (`productId`) | `plinth_pro_600`, `plinth_starter_90`, `plinth_studio_230`, `plinth_unlock` |
  | 1 | `Имя` (`name`) | `Plinth AI: Product Photos` |
  | 1 | `Тип покупки` | leave the default `Купить` |
  | 1 | `Теги` / `Значок` / `Категория налогообложения` | optional — leave or fill freely |
  | 2 | `Идентификатор способа покупки` (`offerId`) | exactly one option (digits, lowercase letters and hyphens) |
  | 2 | Price | `7,29` — enter it **with a comma** in the RU console; `economics.json.priceUsd` holds `7.29` with a dot |
  | 2 | `Доступность` | leave all countries and regions |

- [ ] `[console]` **Exactly one purchase option.** A second option breaks the purchase: the app's billing controller builds the flow without a `setOfferToken`, which Play Billing accepts only when the product has a single option.
- [ ] `[console]` Press **`Активировать`**, not `Сохранить как проект`. A draft option *is not returned by the Play Billing Library*, so the app behaves as if the product does not exist and the reviewer's price sheet fails — the failure mode `store/review-notes.md` names.
- [ ] `[console]` Add the purchase-testing account at `Настройки → Тестирование лицензий`. An account that is only an internal track tester is charged **real money**.
- [ ] `[console]` `Монетизация с помощью Play → Promotions` → create the promo code **after** the product exists (the page shows no create button while the product list is empty), then paste the code into the App-access declaration (§2 `Учетные данные`). The promo count and type are final once saved; the `promotion_codes.csv` file is never committed.
- [ ] `[assist]` **Download the codes — press `Скачать коды` exactly once, then stop and ask.** The button answers with the browser's own save window, which lives outside the page and cannot be accepted by the agent, so it presses once, asks the owner to confirm the save («нажмите „Скачать“ в открывшемся окне и подтвердите»), and **waits** — it never presses again and does not reload the page. Read `promotion_codes.csv` from the browser's default download folder (on Windows usually the user's `Downloads` folder) and never commit it.
- [ ] `[assist]` **If no window opened, hand the owner the manual path and wait.** The page is `.../promotions`, the control is `Скачать коды`, the file is `promotion_codes.csv`; when the owner reports where it landed, read that path and continue.

---

## 5. The submission ladder — press these, in this order

The release form first:

- `[console]` `Имя выпуска` (`releaseName`) — **`0.1.1 (2)`** (`store/release-notes.md`; Play prefills it from the bundle, so it must not be retyped).
- `[console]` `Заметки о выпуске` (`releaseNotes`) — paste the **whole** fenced `<locale>` block of `store/release-notes.md` into the single box, tags on their own lines. After pasting the console answers `Примечания к выпуску предоставлены на N языке`; `на 0 языке` means the tags are inline with the text.
  - Each language is capped at 500 characters and Play truncates silently, so the block is measured at pack validation.
- `[console]` `Страны/регионы выпуска` (`releaseCountries`) — open `Все страны и регионы` → `Изменить список стран/регионов` → tick `Выбрать все строки` → `Сохранить`. It is the one required field this step raises an error for when skipped.
- `[console]` `Наборы App Bundle` → `Добавить из библиотеки` → tick the version → `Добавить в выпуск`. Do not re-upload the file.

Then the ladder itself, in order:

1. `[console]` **Save.** On `.../tracks/<trackId>/releases/<releaseId>/review`, heading `Ошибки, предупреждения и сообщения`, press `Сохранить`. A banner `Обнаружены проблемы с выпуском` over a *warning* does not block — only an `Ошибка` does.
2. `[console]` **The save modal.** `Перейти на страницу "Обзор публикации"?` — the save has **already happened**. Press `Перейти к обзору`. There is no second save.
3. `[console]` **The publishing overview.** `.../publishing` → press the one enabled control `Отправить N изменений на проверку`. If it is disabled and the page prints the `...выполните необходимые действия на панели управления...` line, a §1 precondition is still open.
4. `[console]` **The confirm dialog.** `Отправить N изменений на проверку?` → press `Отправка изменений на проверку`. There are **no checkboxes and no certifications** in this dialog; the two first-publication declarations live in §1 and are not re-asked.
5. `[verify]` **The pre-review check.** `Проверка на наличие распространенных проблем` promises `Если на этом этапе не будет найдено проблем, изменения будут отправлены сразу.` — when it finds nothing the changes go to review immediately, with no second confirm control.
6. `[verify]` **The terminal state.** `.../publishing` reads `Изменения на проверке`, with the one remaining control `Удалить изменения`. That is the end of the ladder.

---

## 6. What stays human — the file inputs, and where the owner's words are

Everything else above is a console action an agent may perform when the owner has directed it. Two controls are file inputs, which the sandboxed browser cannot set, so the owner performs them (`docs/play-console-checklist.md` §10):

1. `[human]` **Uploading the bundle file.** `Выпуски` → `Загрузить` takes the AAB itself and is the one act that puts the version into the app's library for §5's `Добавить из библиотеки`. The release form itself never re-uploads the file.
2. `[human]` **Uploading the five store-graphic fields** (`Магазин → Основная информация о магазине`): the icon, the feature graphic and the phone / 7-inch / 10-inch screenshot sets. The field is a hidden `input[type=file]` that appears after `Добавить объекты`, and `DOM.setFileInputFiles` is denied — an unsolved tooling limitation, recorded as a limitation and not as a policy.

**The two first-publication declarations are not on that list.** They are the owner's *attestation* and the agent's click: `docs/play-console-checklist.md` §1.A records his decision of 2026-10-03 that the boxes may be ticked automatically once the slot carries the recorded approval, because «если я разрешил публикацию, значит эти галочки можно уже автоматически нажимать». The attestation is discharged by the approval, not by the click.

That approval precedes this runbook: the agent has already installed and exercised the app on the phone, and the owner's *product* decision is recorded with `node scripts/approve-release.mjs --slot plinth-product-photos --note "<the owner's own words>"`. The annual BIS self-classification question stays an open obligation.

---

## 7. Decisions this runbook could not derive

Questions for a human, not guesses: each names the declaration that would settle it. Until they are answered, the console field named in each is **NEEDS-DECISION**.

- `Категория приложения` — `listing.category` carries the token `PHOTOGRAPHY`, which `store/listing.json.categoryLabels` does not hold (it holds: none). Read the category's label in the console and record the pair.

---

**The global reference** for every field, option and cap is `docs/play-console-checklist.md` (longer narrative: `docs/play-publishing-playbook.md`). This file is the app-specific click sequence; the two are meant to be read together, and this one is the shorter path.

<!-- kiln:generated console runbook - delete this line to keep your own wording -->
