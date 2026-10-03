# Play Console submission checklist

This is the runbook for taking an **approved** revision of a slot through Google Play Console to the
terminal state `Изменения на проверке`. It is read by the builder at phase 2 and by the owner at
checkpoint 2. It carries the exact console strings, field caps and page names recorded from the live
RU console on **2026-09-29**, so a human who has never opened the console can follow it and an agent
can be checked against it. Nothing here is a substitute for the owner's approval, and nothing here
is a program: the console ladder is a recorded runbook, not code.

**Markers.** `[human]` — an action only the owner may take. `[console]` — a console action a person
or an agent performs. `[verify]` — a read-only check. Exactly one step is `[human]` — the
store-graphics upload; it is listed again in §10.

**Sources.** `docs/play-publishing-playbook.md` §5.1 (the live walk), the archived console model
`docs/play-console-observations.json` — the frozen body of the deleted `scripts/lib/console-form.json`,
marked *an archived reading; no program reads this file* —
`requirements-preservation.md` §14, and the Play articles cited inline.

---

## 1. Preconditions — what must be true before the ladder can be walked

While any dashboard task is open, nothing in the submission flow is reachable and the submit button
stays disabled. Clearing these three is what enables it.

### A. The two first-publication declarations `[console]`

The agent ticks these two **only when the slot carries a recorded release approval**. The approval is
the human checkpoint — the owner reviews the plan and the policies before the initial publication and
says so in his own words — and the clicks are not themselves the checkpoint. The owner's decision,
recorded 2026-10-03: «В нашем заводе есть правило, что перед публикацией необходимо одобрение
владельца. Собственно говоря, в то время и я проверяю проверку всех вот этих политик, и только после
моего одобрения я говорю разрешить публикацию. Поэтому, если я разрешил публикацию, значит эти
галочки можно уже автоматически нажимать. Потому что все проверки необходимые для публикации
проводятся до начальной публикации всего приложения.»

- [ ] `Панель управления` (`.../app-dashboard`) → dialog `Заявки`: tick
      `Подтвердить, что приложение соответствует Правилам программы для разработчиков`.
- [ ] Tick `Принять экспортное законодательство США`.
- [ ] These are accepted **once per app** rather than per release, and the agent performs them only
      under the recorded approval above. No field of the store pack carries them; do not infer them
      from a slot's capabilities or its pack.
- [ ] In the observed run both were already ticked and the dialog's primary button was disabled —
      that disabled button is the evidence that nothing remained to save there.

### B. The five store-graphic fields `[human]`

- [ ] `Магазин → Основная информация о магазине` (`/main-store-listing`): upload the icon, the
      feature graphic, and the phone / 7-inch / 10-inch screenshot sets — five required image fields,
      recorded in the console model as `icon`, `featureGraphic`, `phoneShots`, `tablet7Shots` and
      `tablet10Shots`.
- [ ] The pack already renders every one of them and `scripts/media-capture.mjs` produces the
      screenshots; the upload itself cannot be done by the sandboxed browser the factory drives (see
      §10).
- [ ] While the dashboard row `Настройте страницу приложения` is open, the submit button in §2
      stays disabled. The observed session cleared it exactly this way.
- [ ] The same page carries the listing's text and identity fields an agent can paste:
      `Название приложения *` (`title`), `Краткое описание *` (`shortDescription`),
      `Полное описание *` (`fullDescription`), `Категория приложения` (`category`) and
      `Контактный адрес электронной почты` (`contactEmail`, the developer account's monitored
      address). Their character caps are measured by the pack's own limit resolver
      (`scripts/lib/play-assets.json`) and printed by `store/listing.md`; they are not re-typed
      here, because a second copy of a cap is a second answer to one question.
- [ ] On the same page the optional media fields stay empty: `URL проморолика на YouTube для
      приложения` (`video`), `Скриншоты на компьютере` (`desktopShots`), `Скриншоты для Android
      XR` (`androidXrShots`), `Пространственное видео для Android XR` (`androidXrSpatialVideo`)
      and `Непространственное видео для Android XR` (`androidXrNonSpatialVideo`). See §8.

### C. The privacy policy exists in two places, not one `[verify]`

- [ ] `Контент приложения → Политика конфиденциальности` (`.../app-content/privacy-policy`), field
      `URL политики конфиденциальности`, carries the slot's `listing.privacyPolicyUrl`.
- [ ] The URL is **published by the factory, not obtained from the owner**:
      `node scripts/publish-privacy.mjs --slot <slug>` renders `store/privacy.md` to a real public
      HTTPS page during development and records it — `docs/privacy-policy-hosting.md`,
      `.cursor/rules/privacy-policy.mdc`. Which hosts count as unfinished placeholders is stated once
      in `.cursor/rules/project-conventions.mdc`.
- [ ] The same policy must be reachable **from inside the app**. The console's own prose on that
      page reads: `Следует разместить ссылку на политику конфиденциальности на странице
      приложения в Google Play и в самом приложении, даже если оно не предназначено для детей и не
      получает...` The public URL satisfies one half; the app satisfies the other, and privacy prose
      in an About screen is **not** the policy.

---

## 2. The submission ladder, in order

This is the sequence `requirements-preservation.md` §14.1 records. Follow it literally; the one
control that submits lives here and nowhere else.

- [ ] **Step 1 — `Сохранить` on the release `[console]`.** On
      `.../tracks/<trackId>/releases/<releaseId>/review`, heading
      `Ошибки, предупреждения и сообщения`, press `Сохранить`. A banner reading
      `warning Обнаружены проблемы с выпуском` is the generic wording over a non-blocking warning —
      Play counts `Ошибка` and `предупреждение` separately and only `Ошибка` blocks `Сохранить`.
      If the count reads an error, stop and fix it.
- [ ] **Step 2 — the modal `Перейти на страницу "Обзор публикации"?` `[console]`.** The save has
      **already happened** by the time this overlay is on screen. Its body is
      `Изменение сохранено. Чтобы отправить его на проверку, перейдите в "Обзор публикации".`;
      the buttons are `Не сейчас` and `Перейти к обзору`. There is no second save to look for.
      Choose `Перейти к обзору`.
- [ ] **Step 3 — the publishing overview `[console]`.** `.../publishing`, heading
      `Обзор публикации`. Its one enabled control is `Отправить N изменений на проверку`; `N` is
      the count of pending changes and varies per app (it read `12` on the observed app). If the
      page instead prints `Чтобы отправить изменения на рассмотрение, выполните необходимые
      действия на панели управления приложением.`, a required dashboard task is still outstanding:
      close it from §1 and come back. Do not press anything else and do not hunt for a hidden second
      submit control — there is none.
- [ ] **Step 4 — the `Проверка на наличие распространенных проблем` block `[verify]`.** The block
      carries the line `Осталось: максимум N минут` (it read `Осталось: максимум 13 минут` in the
      observed run) and the promise
      `Если на этом этапе не будет найдено проблем, изменения будут отправлены сразу.`
      Read this literally: **when the check finds nothing, the changes go to review immediately —
      there is no second confirm button to look for.** `Осталось: максимум N минут` is the
      console's own estimate, not a wait the factory schedules.

      > **Measured order, recorded rather than resolved.** The live walk of 2026-09-29
      > (`docs/play-publishing-playbook.md` §5.1, steps 7 then 8) recorded the confirm dialog of
      > step 5 opening immediately on the submit press, with the pre-review check running **after**
      > it. `requirements-preservation.md` §14.1 lists the check before the dialog, and this
      > checklist follows the ledger. Both texts and both controls exist; only their order differs.

- [ ] **Step 5 — the confirm dialog `Отправить N изменений на проверку?` `[console]`.** Title
      `Отправить N изменений на проверку?`, body
      `Изменения будут отправлены на проверку в Google. Обычно она занимает до семи дней, но
      иногда может потребоваться больше времени.`, buttons `Отмена` and
      `Отправка изменений на проверку`. **There are no checkboxes of any kind and no certifications
      in this dialog** — the two first-publication declarations live in the dashboard's `Заявки`
      dialog (§1.A) and are not re-asked. Press `Отправка изменений на проверку`.
- [ ] **Step 6 — the terminal state `Изменения на проверке` `[verify]`.** On `.../publishing`:
      `Изменения на проверке` and
      `Изменения находятся на рассмотрении. Если в приложении есть другие проблемы, мы обнаружим их
      на этом этапе.`, plus the one remaining control `Удалить изменения`, which withdraws the
      submission. On the account app list (`/app-list`) the app's `Статус обновления` column reads
      `На рассмотрении`.
- [ ] That is the end of the ladder. Nothing further is pressed until Google's verdict; the review
      can take a few hours or up to seven days, and no control accelerates it.

---

## 3. The ten App-content declarations

`Контент приложения → Обзор` (`.../app-content/overview`) has two tabs. `Требуются действия` is the
tab open by default and holds what still waits for an answer; `Обработано` is the list of saved
declarations, whose heading prints `N обработанных деклараций`. The dashboard's own counter
(`Выполнено: N из 11`) counts a different set and is not the declaration count. The ten rows, each
with the action `Настроить`:

- [ ] Безопасность данных (Data safety). Complete even when nothing is collected, and reconciled with
      every bundled SDK and with the public privacy policy. The definition of "collect", and the
      under-declaration trap a bundled SDK can create, is stated once in
      `.cursor/rules/play-compliance.mdc`.       The form's own sections are `Сбор и передача данных`
      (`collectsOrShares`), `Шифрование при передаче` (`encryptedInTransit`), `Аккаунты`
      (`accountCreation`), `Удаление данных` (`dataDeletion`), `Категории данных`
      (`dataCategories`) and `Эти данные обрабатываются временно?` (`temporaryProcessing`); every
      answer comes from the slot's `store/data-safety.md`.
- [ ] Приложения для здоровья (Health apps)
- [ ] Финансовые функции (Financial features)
- [ ] Приложения государственных учреждений (Government apps)
- [ ] Рекламный идентификатор (Advertising ID)
- [ ] Целевая аудитория и возрастные ограничения (Target audience and content)
- [ ] Возрастные ограничения (Content rating)
- [ ] Учетные данные (App access)
- [ ] Реклама (Ads)
- [ ] Политика конфиденциальности (Privacy policy)

### Реклама — both options read

- [ ] `.../app-content/ads-declaration`, radio `Реклама`:
      `Да, мое приложение содержит рекламу` / `Нет, мое приложение не содержит рекламу`.
- [ ] The answer must match what is actually built: `verify.mjs` compares it with
      the enabled capabilities and fails on a disagreement. A slot with no ads SDK answers `Нет`.

### Учетные данные — both branches

`.../app-content/testing-credentials`. The radio question is
`В вашем приложении есть закрытые разделы?` (`hasRestrictedSections`) with options `Да` / `Нет`.

- [ ] **The `Нет` branch** asserts that no sign-in and no purchase is required. A slot that sells an
      unlock cannot assert this.
- [ ] **The `Да` branch** prints, as an explanation, what the console counts as a gate:
      `Учетные данные (например, адрес электронной почты), имя пользователя, вход с аккаунтом
      Google или система единого входа (SSO).`; `Платежи, например за контент для однократных
      покупок, подписки или уровни доступа.`; `Реферальные коды или QR-коды.`; `Одноразовые
      PIN-коды или двухэтапная аутентификация.`; `Биометрическая аутентификация.`; `Действия,
      которые нужно выполнить на другом устройстве.` A selling slot belongs here on the
      `Платежи…` line.
- [ ] The `Да` branch carries an entry table (`credentials-entry`) (`Имя` / `Инструкции`). A row is
      opened with `Изменить: <имя записи>`; `Удалить: <имя записи>` is disabled while a single entry
      exists, so the console never lets the page be left without any access instruction.
- [ ] **The credentials dialog** (`Добавить данные для входа`) fields and caps: `Имя *` (33 of 60
      characters in the observed run); `Имя пользователя, электронный адрес или номер телефона`;
      `Пароль` (0 of 100); `Другие данные, которые требуются для входа` (440 of 500 — this is
      where a **promo code** goes, not the username/password fields); the certification checkbox
      `Учетные данные, указанные в этой декларации, обеспечивают полный доступ ко всем функциям и
      контенту приложения, включая платный`, ticked honestly only when the reviewer really reaches
      the paid content — which for a selling slot is only by the promo code.
- [ ] A login-gated app provides usable **demo access** (`reviewer-access-toggle`, the credentials
      dialog's `Добавить данные для входа`), recorded in `store/review-notes.md`. The
      console's own dialog states what the reviewer cannot do:
      `Мы не можем создавать новые аккаунты, использовать личные аккаунты для совершения покупок`
      — the purchase is never made for the reviewer, so the only way to hand over paid content is the
      promo code of §5.
- [ ] Instructions and the certification checkbox save **together**: with the checkbox cleared the
      dialog answered `Не удалось сохранить изменения` in the observed run.

### Возрастные ограничения — the questionnaire's step 1 of 3

The strip is `1 Категория` / `2 Анкета` / `3 Сводка`.

- [ ] Step 1 field `Электронный адрес` = `listing.contactEmail`.
- [ ] Three category radios: `Игра` / `Социальные сети или связь` / `Все другие типы приложений`.
      The slot's questionnaire branch decides which one.
- [ ] The IARC checkbox: `Я принимаю условия использования Международной коалиции возрастной
      классификации (IARC)`. Without it the console does not open step 2. It is a formality of the
      form, not a fact about the app.
- [ ] Press `Далее` to reach step 2 `Анкета`. Step 2 (`content-rating-step-2`) holds the IARC
      questionnaire's question rows (`q-downloaded-content`, `q-user-content-sharing`,
      `q-online-content`, `q-age-restricted-goods`, `q-precise-location`, `q-digital-goods`,
      `q-random-elements`, `q-monetary-rewards`, `q-browser-or-search`, `q-news-or-education`);
      step 3 (`content-rating-step-3`) is `Сводка`, which is what the slot's own `store/listing.json`
      `contentRating` answers are checked against.

### Целевая аудитория и возрастные ограничения — the five-step wizard

- [ ] The wizard's strip runs to `Шаг 5 Сводка`; the model records the steps it walked as `Шаг 2
      О приложении` (`target-audience-step-2`), `Шаг 3 Реклама` (`target-audience-step-3`),
      `Шаг 4 Страница приложения` (`target-audience-step-4`), `Шаг 5 Сводка`
      (`target-audience-step-5`).
- [ ] `Какие целевые возрастные группы у вашего приложения?` (`ageGroups`) — tick the slot's own
      branch from `listing.contentRating.targetAudience`'s own dictionary, never another slot's
      branch: a substituted branch ticks age groups the app does not target.
- [ ] A group under 18 reveals the conditional minor-access checkbox (`minorGate`); the six groups
      are `до 5`, `6–8`, `9–12`, `13–15`, `16–17`, `18+`.
- [ ] On the `adults-only` branch **steps 2–4 are skipped** and `Сводка` carries the line about
      restricting access for minors. A run that walks four steps where the branch skips them has
      answered for a different app — the count of steps is itself a reading.

---

## 4. The release form

- [ ] While the release is `Черновик`, the release state reads `0.1.0 (1)`. The console prefills the
      release name from the bundle as `<versionName> (<versionCode>)`, with a length counter
      (`9/50` observed) — retyping it is not needed, and it then cannot disagree with the uploaded
      bundle. A release name that disagrees with the upload makes the console's release list lie
      about which build is which.
- [ ] `Заметки о выпуске` is **one field for all languages**: paste the fenced `<locale>` block of
      `store/release-notes.md` whole, tags on their own lines. After pasting, the console answers
      `Примечания к выпуску предоставлены на 2 языке`; `на 0 языке` means the `<locale>` tags are
      inline with the text.
- [ ] `Страны/регионы выпуска` is a required field: open `Все страны и регионы (N)` →
      `Изменить список стран/регионов` → tick `Выбрать все строки` → `Сохранить`. The field lives
      on the release's own tab, `.../tracks/<trackId>?tab=countryAvailability`. It is the only
      error the `Посмотреть и подтвердить` step raises when skipped:
      `Для этого типа версии не выбраны страны и регионы. Добавьте хотя бы одну страну или регион.`
      See §6 before narrowing the list.
- [ ] The bundle is not re-uploaded. `Наборы App Bundle` → `Добавить из библиотеки` → tick
      `1 (0.1.0)` → `Добавить в выпуск`. The neighbouring `Загрузить` is the path for a new file.
- [ ] The warning `Этот объект (App Bundle) содержит нативный код. Рекомендуем загрузить файл с
      отладочными символами` is not an error and does not block `Сохранить`; 16 KB readiness is
      measured separately from the built artifact.

---

## 5. The in-app product and the promo code

The product form has two steps: **1 Сведения о продукте** and **2 Доступность и цены**, reached
through `Монетизация с помощью Play` → `Встроенные продукты` (`.../one-time-products`) → the create
form at `.../one-time-products/create`. The form's observed field caps, which a slot's own IDs and
tags have to fit:

| Step | Field | Cap |
|---|---|---|
| 1 | `Идентификатор товара` (`productId`) | 141 |
| 1 | `Имя` (`name`) | 55 |
| 1 | `Описание` (`description`) | 200 |
| 1 and 2 | `Теги` (`tags`) | 20 |
| 2 | `Идентификатор способа покупки` (`offerId`) | 63 |
| 2 | `Имя` (`name`) | 55 |
| 2 | `Описание` (`description`) | 200 |

- [ ] Product ID comes from the slot's Kotlin config and must match the app exactly. Purchase option
      IDs (`offerId`, 63) may contain only digits, lowercase letters and hyphens; product IDs instead
      may contain underscores and dots, and reach 141 characters.
- [ ] `Тип покупки` — leave the default `Купить`. `Расширенные настройки` — leave collapsed.
      `Доступность` — leave all countries and regions.
- [ ] **The price is entered with a comma.** `economics.json` stores `priceUsd: 6.99` with a dot; the
      Russian console's price field takes `6,99`. Open the `Set prices` dropdown (the label stays
      untranslated in the RU console), enable bulk editing, tick the all-countries checkbox, and
      type the comma form. `economics.json` keeps its dot.
- [ ] **The product must carry exactly one purchase option.** A second option does not add a choice —
      it breaks the purchase: the repository's billing controller builds the flow without a
      `setOfferToken`, a form Play Billing Library accepts only when the product has a single
      purchase option.
- [ ] The button pair is `Активировать` / `Сохранить как проект` / `Назад`.
      **`Активировать`** publishes the option and makes it purchasable; **`Сохранить как проект`**
      leaves it a draft, and a draft is not returned to the app — the app then behaves as if the
      product did not exist and the cause gets hunted in the code. Press `Активировать`.
- [ ] The **certification checkbox** in the credentials dialog (§3) is the one that must be ticked
      together with the instructions; the promo code is created and then pasted into `Другие данные,
      которые требуются для входа`.
- [ ] **Promo codes:** `Монетизация с помощью Play → Promotions` (`.../promotions`). The page's info block reads
      `Создавать промоакции можно только для подписок, контента для продажи или платных приложений`
      — so the code is created **after** the product, never before. With an empty product list the
      page shows no create button.
- [ ] **The 365-day limit.** A promotion lasts at most one year, counted as at most **365** days
      between start and end: the console refuses with `Период между датой начала и датой окончания
      не должен превышать 365 дней.` With a 2026-09-29 start, an end of 2027-09-28 passes and
      2027-10-01 is refused (367 days).
- [ ] A promo code **is** created and becomes active for an app with no reviewed release — observed
      2026-09-29, questions closed. Redeem one yourself as a license tester before submitting, so the
      instruction handed to the reviewer is known to work.
- [ ] After saving, the code count and the promotion type cannot be changed, and the end date can
      only move forward; the count is a final decision. Download codes promptly with
      `Скачать коды` (`promotion_codes.csv`) — after the end date the page stops showing them. The
      file is **never committed**: a code is a free unlock of what the slot sells.
- [ ] License testers are an **account-level** list (`Настройки → Тестирование лицензий`), different
      from track testers. An account that is only an internal tester on a track is charged real
      money.

---

## 6. The EU position — Regulation (EU) 2018/302, art. 3(1)

The console notice, verbatim:

> **Регламент о необоснованной геоблокировке**
> Если вы распространяете приложения на территории Европейского союза, ознакомьтесь с Регламентом
> ЕС 2018/302.
> Link: `Подробнее` — Button: `Понятно`

- [ ] **The notice is neither a field nor an error.** It has no checkbox, it does not block
      `Сохранить`, and no field of the pack answers it, so "empty" is the whole answer and there is
      nothing for any slot to tick or paste. Do not look for a control it does not have.
- [ ] **The obligation rests on the country/region list**, not on the notice text. The factory's
      default is to leave **every** country and region ticked (§4).
- [ ] A list holding EU countries must not narrow access by a user's **nationality, place of
      residence or place of establishment**.
- [ ] The list narrows **only** under the regulation's own exception: where the main feature of the
      service is the provision of access to and use of copyright-protected works or other protected
      subject matter.
- [ ] A list holding **no** EU country is not distributed in the EU, and the notice is closed by
      itself — that empty answer is complete rather than unfinished.

Sources: Regulation (EU) 2018/302, art. 3 *Access to online interfaces*, <https://eur-lex.europa.eu/eli/reg/2018/302/oj/eng>;
<https://support.google.com/googleplay/android-developer/answer/6223646>.

---

## 7. Automatic string translation — never enable it for a language the slot already translates

A first upload shows three rows of one list — the bundle *signed by Google Play*, *automatic
protection* and *automatic strings translation* — because Play App Signing unlocks all three as
bundle enhancements. The third is a feature being offered, not a finding about the app, and declining
it blocks nothing.

- [ ] Do **not** enable it for a language the slot already translates. Why, and the console's own
      warning text, are stated once in `AGENTS.md` under "Store-pack contract".
- [ ] Using it as a **bootstrap** for a language the slot does not translate at all is coherent only
      if the output is reviewed and **frozen into `values-<locale>/`** afterwards.
- [ ] A single-language listing over a multi-language app is legitimate and needs no "fix".

---

## 8. Not produced on purpose

- [ ] The **Desktop** and **Android XR** screenshot fields stay empty. They carry no asterisk in the
      console — they are optional — and the owner's instruction on 2026-09-29 is that the pack does
      not fill them. No target, no asset and no check exists for them, and adding one is a product
      decision rather than a completeness fix. Do not "fix" this later.
- [ ] The three optional media fields stay empty for the same reason and are named here so nobody
      goes looking for them: `URL проморолика на YouTube для приложения` (`video`),
      `Пространственное видео для Android XR` (`androidXrSpatialVideo`) and `Непространственное
      видео для Android XR` (`androidXrNonSpatialVideo`). A promo video is a marketing asset the
      factory does not produce.

---

## 9. Play API boundaries and the first launch

- [ ] There is **no official Play MCP**. The Play Developer API **cannot create an app**, so the app
      is created in the console by hand and the exact `applicationId` from `release-manifest.json`
      is registered. `scripts/play-upload.mjs` updates an existing app **after** its first manual
      upload.
- [ ] Data safety **can** be written from a reviewed CSV
      (`POST /androidpublisher/v3/applications/{packageName}/dataSafety`), and the CSV is still
      authored from the slot's real SDK surface and approved by the human before it is sent; an
      unrealised CSV is a truthfulness risk.
- [ ] The privacy-policy URL is **not** settable through `edits.details` (which exposes only
      `defaultLanguage`, `contactWebsite`, `contactEmail`, `contactPhone`), so it stays a console
      step (§1.C).
- [ ] A new **personal** account created after **2023-11-13** may need at least **12** testers kept
      opted into a closed test continuously for **14 days** before production access. Plan about
      three weeks for the first launch.

---

## 10. What stays human — one step, and the declarations the agent now performs

Every other step of this ladder is console-mandatory and unattended: an agent may walk it when the
owner has directed it and is observing. What an agent may do on its own initiative — and what it may
never do without an instruction — is stated once in `.cursor/rules/authorization.mdc`.

1. **Uploading the five store-graphic fields (§1.B).** The field is a hidden `input[type=file]` that
   only appears after `Добавить объекты`, and the sandboxed browser the factory drives denies
   `DOM.setFileInputFiles`. This is an **unsolved tooling limitation, deliberately recorded as a
   limitation and not as a policy.** A human — or a driver that owns the file chooser — performs this
   step and nothing else.
2. **The two first-publication declarations (§1.A)** — `Правила программы для разработчиков` and
   `Экспортное законодательство США` — are performed by the **agent**, not the owner, once the slot
   carries a recorded release approval: the approval is the human checkpoint, and the click is not.
   They are accepted **once per app** rather than per release. The export declaration is a
   certification about encryption the app uses: every slot uses encryption whether or not it imports
   crypto code, and the **annual BIS self-classification question is an open obligation**, not
   settled law.

The release approval itself (checkpoint 2) precedes this ladder: the agent has already installed the
debug APK on the connected phone and exercised the app itself, the handoff carries its observed
evidence (device serial, artifact hash, what appeared on screen), and the owner's act is the
**product** decision — whether this is the app to ship — stated in his own words, which is recorded
with `node scripts/approve-release.mjs --slot <slug> --note "<the owner's own words>"`. The rules for
that record — the owner's own words only, never inferred, never recorded to unblock a step — are
stated once in `.cursor/rules/release-gate.mdc`.

---

## Stated limits, not defects

These are honest limits of the checks and records around this ladder. They are recorded so that
nobody "fixes" them with a check that cannot fail.

- **The approval digests do not cover `scripts/**`, the rules or `AGENTS.md`.** In principle an agent
  could weaken the check that judges it after approval. Widening the digest to `scripts/**` would
  void every slot's approval on every harness improvement, so the boundary is made **auditable** —
  one command reports exactly which bytes the owner approved — rather than tamper-proof. The real
  defence is that recorded approvals are read back and harness changes are reviewed as diffs.
- **`approvedBy: 'owner'` is a constant in the approval record.** Nothing in the record can
  distinguish the owner's words from an agent's. Only recording an approval from the owner's own
  words, never inferred, keeps that true.
- **The client-secret scan cannot claim an APK is secret-free.** A key fetched at launch, assembled
  from three variables, or hidden in a dependency's own resources is beyond a line-oriented scan.
  Its job is to make the common, measured defect unshippable, not to certify the artifact.
- **The self-learning loop does not browse Google Play by itself.** A policy change is noticed when
  an agent or the owner reads the page and re-dates the fact. Two positions are known to be
  **unenforced**: the automatic-string-translation toggle of §7 (no program can measure a console
  toggle), and the AGP / Kotlin / Compose BOM version pins.

---

## 11. The build the release is made from — read by the builder

Play's console pages cannot tell you whether the build behind the upload was made correctly, so the
factory's own build facts are recorded here rather than nowhere. None of them is a console field.

- [ ] **Baseline profiles are pinned and generated on hardware.** `androidx.baselineprofile` **1.5.0**,
      Profile Installer **1.4.1** and Macrobenchmark **1.4.1** live in the version catalog, and their
      generation needs a **device or emulator** plus a `com.android.test` module. It is therefore a
      phase-2 step on real hardware, not something a build script can do unattended — and Play does
      not require it, the factory does, so a release that skips it is one with no profile rather than
      one that failed a console rule.

---

## Every console id this checklist covers, and where

The archived reading (`docs/play-console-observations.json`) names these screen and field ids;
this index turns an id into the console label a reader can find on the page, so
"the pack mentions `saveButtons` — where is that?" has an answer here. The screen ids are the
sections this checklist walks in order; the groups after them are the forms inside those
screens.

- **declarationCompletion.counters** — `pending`, `processed`, `dashboard`, `publishing`, `save-button`
- **listing.fields** — `category` (Категория приложения), `contactEmail` (Контактный адрес электронной почты), `privacyPolicyUrl` (URL политики конфиденциальности), `title` (Название приложения *), `shortDescription` (Краткое описание *), `fullDescription` (Полное описание *), `icon` (Значок приложения *), `featureGraphic` (Картинка для описания *), `phoneShots` (Скриншоты для смартфона *), `tablet7Shots` (Скриншоты для семидюймовых планшетов *), `tablet10Shots` (Скриншоты для 10-дюймового планшета *), `desktopShots` (Скриншоты на компьютере), `androidXrShots` (Скриншоты для Android XR), `androidXrSpatialVideo` (Пространственное видео для Android XR), `androidXrNonSpatialVideo` (Непространственное видео для Android XR), `video` (URL проморолика на YouTube для приложения), `saveButtons` (Сохранить / Сохранить как проект / Не сохранять)
- **targetAudience.checkboxes** — `under5` (До 5 лет), `6-8` (6–8), `9-12` (9–12), `13-15` (13–15), `16-17` (16–17), `18-plus` (От 18 лет)
- **targetAudience.steps** — `step-1` (Возраст целевой группы), `step-2` (О приложении), `step-3` (Реклама), `step-4` (Страница приложения), `step-5` (Сводка)
- **credentials.dialogFields** — `name` (Имя *), `username` (Имя пользователя, электронный адрес или номер телефона), `password` (Пароль), `instructions` (Другие данные, которые требуются для входа), `full_access` (The credentials given in this declaration provide full access to all features and content of the app, including paid content), `google_devices_switch` (Allow these credentials to be used to test your app on Google devices and trusted partners' devices, and to collect feedback about it), `hint_other_data` (The console's own hint under «Other data required to sign in»)
- **billing.productForm.step1.fields** — `productId` (Идентификатор товара), `tags` (Теги), `name` (Имя), `description` (Описание), `icon` (Значок), `taxCategory` (Категория налогообложения товара), `ageRestriction` (Возрастное ограничение), `paymentRestriction` (Ограничение способов оплаты по местоположению)
- **billing.productForm.step2.fields** — `offerId` (Идентификатор способа покупки), `purchaseType` (Тип покупки), `tags` (Теги), `advanced` (Расширенные настройки), `availability` (Доступность)
- **screens** — `app-status` (Состояние приложения), `store-listing` (Основная информация о магазине), `app-content-overview` (Контент приложения), `declaration-privacy-policy` (Политика конфиденциальности), `declaration-ads` (Реклама), `declaration-app-access` (Учетные данные), `declaration-content-rating` (Возрастные ограничения), `declaration-target-audience` (Целевая аудитория и возрастные ограничения), `declaration-data-safety` (Безопасность данных), `declaration-advertising-id` (Рекламный идентификатор), `declaration-government-apps` (Приложения государственных учреждений), `declaration-financial-features` (Финансовые функции), `declaration-health-apps` (Приложения для здоровья), `release` (Создание выпуска), `publishing` (Обзор публикации), `billing-product` (Встроенные продукты), `billing-license` (Тестирование лицензий), `billing-promotions` (Promotions)
- **screens.fields** — `appStatus` (Состояние приложения), `requiredActions` (Требуются действия), `map-needs-action` (Требуются действия), `map-processed` (Обработано), `map-data-safety` (Безопасность данных), `map-health-apps` (Приложения для здоровья), `map-financial-features` (Финансовые функции), `map-government-apps` (Приложения государственных учреждений), `map-advertising-id` (Рекламный идентификатор), `map-target-audience` (Целевая аудитория и возрастные ограничения), `map-content-rating` (Возрастные ограничения), `map-app-access` (Учетные данные), `map-ads` (Реклама), `map-privacy-policy` (Политика конфиденциальности), `privacyPolicyUrl` (URL политики конфиденциальности), `ads` (Реклама), `hasRestrictedSections` (В вашем приложении есть закрытые разделы?), `yes-branch-access-types` (Что из перечисленного ниже требуется для доступа к приложению:), `credentials-entry` (Учетные данные), `reviewer-access-toggle` (Разрешить использовать эти учетные данные для тестирования вашего приложения на устройствах Google и надежных партнеров, а также получения отзывов о его работе), `no-branch-text` (Что из перечисленного ниже верно для вашего приложения:), `email` (Электронный адрес), `category` (Категория), `iarc` (Я принимаю условия использования Международной коалиции возрастной классификации (IARC)), `ageGroups` (Какие целевые возрастные группы у вашего приложения?), `minorGate`, `collectsOrShares` (Сбор и передача данных · собирает или передаёт ли приложение обязательные типы данных), `encryptedInTransit` (Шифрование при передаче · шифруются ли данные, которые приложение собирает), `accountCreation` (Аккаунты · может ли пользователь создать аккаунт в приложении), `dataDeletion` (Удаление данных · предусмотрен ли способ запросить удаление данных), `dataCategories` (Категории данных · какие типы данных собираются, обязательны ли они и с какой целью), `temporaryProcessing` (Эти данные обрабатываются временно?), `advertisingId` (Рекламный идентификатор), `government` (Приложения государственных учреждений), `financial` (Финансовые функции), `health` (Приложения для здоровья), `releaseName` (Имя выпуска), `releaseNotes` (Заметки о выпуске), `releaseCountries` (Страны/регионы выпуска), `submit_button` (Отправить N изменений на проверку), `lock_message` (Чтобы отправить изменения на рассмотрение, выполните необходимые действия на панели управления приложением.), `pending-changes` (Изменения, ещё не отправленные на проверку), `review-considered` (Изменения, учитываемые при проверке), `licenseTester` (Тестировщики лицензий), `startDate` (Дата начала), `endDate` (Дата окончания), `codeCount` (Количество кодов), `promoType` (Тип промоакции)
- **screens.subscreens** — `credentials-dialog` (Диалог «Добавить данные для входа»), `content-rating-step-2` (Шаг 2 Анкета), `content-rating-step-3` (Шаг 3 Сводка), `target-audience-step-5` (Шаг 5 Сводка), `target-audience-step-2` (Шаг 2 О приложении), `target-audience-step-3` (Шаг 3 Реклама), `target-audience-step-4` (Шаг 4 Страница приложения), `country-availability` (Страны и регионы — вкладка выпуска и уведомление, пересказанное владельцем), `release-saved-modal` (Модальное окно после «Сохранить»), `app-dashboard-goal` (Панель управления — цель «Настройте приложение» и её счётчик), `submit-dialog` (Диалог подтверждения отправки), `eu-geo-blocking-notice` (Уведомление «Регламент о необоснованной геоблокировке»), `post-submit-states` (После отправки: проверка перед отправкой и конечное состояние), `declarations-dialog` (Диалог «Заявки» — обе сертификации первого выпуска), `billing-product-step-1` (Шаг 1 Сведения о продукте), `billing-product-step-2` (Шаг 2 Доступность и цены), `billing-price` (Цена — выпадающая метка `Set prices`), `billing-tags` (Теги — поле шага 1 и шага 2)
- **screens.subscreens.fields** — `q-downloaded-content` (Скачанное приложение · содержимое с возрастными ограничениями (секс, насилие, грубые выражения, наркотики, азартные игры)), `q-user-content-sharing` (Общий доступ к пользовательскому контенту · обмен сообщениями, файлами, медиа между пользователями), `q-online-content` (Интернет-содержимое · продвижение или потоковая передача контента извне, который не входит в скачанный пакет), `q-age-restricted-goods` (Продвижение или продажа товаров и видов деятельности с возрастными ограничениями (алкоголь, табак, оружие, азартные игры)), `q-precise-location` (Другое · передача точного физического местоположения пользователя другим пользователям), `q-digital-goods` (Другое · покупка цифровых товаров внутри приложения), `q-random-elements` (Другое · покупки со случайными элементами (покупатель не знает заранее, что получит)), `q-monetary-rewards` (Другое · денежные вознаграждения, подарочные карты, конвертируемые вознаграждения, криптовалюта, NFT), `q-browser-or-search` (Другое · веб-браузер или поисковая система), `q-news-or-education` (Другое · приложение главным образом новостное или обучающее), `eu-geo-blocking-notice` (Уведомление о регламенте ЕС 2018/302 на вкладке «Страны/регионы»), `save-modal` (Перейти на страницу "Обзор публикации"?), `goal-store-category` (Выберите категорию приложения и укажите контактную информацию), `goal-store-listing` (Настройте страницу приложения), `dialog-title` (Отправить N изменений на проверку?), `dialog-body` (Изменения будут отправлены на проверку в Google. Обычно она занимает до семи дней, но иногда может потребоваться больше времени.), `dialog-buttons` (Отмена / Отправка изменений на проверку), `geoblocking-banner` (Регламент о необоснованной геоблокировке), `pre-review-check` (Проверка на наличие распространенных проблем), `no-second-confirmation` (Если на этом этапе не будет найдено проблем, изменения будут отправлены сразу.), `terminal-state` (Изменения на проверке), `developer-program-policies-row` (Правила программы для разработчиков), `us-export-laws-row` (Экспортное законодательство США), `dialog-buttons` (Сохранить / Не сохранять), `price` (Set prices), `tags` (Теги)

---

## Appendix: the console's own page paths, as recorded

Each line below is the breadcrumb the RU console showed on **2026-09-29**, quoted verbatim from the
archived reading (`docs/play-console-observations.json`) rather than paraphrased, so a reader can
match the route in the console and not only this document's description of it. The parenthesised
branch is the reading's own location for the path.

- `Контент приложения → Учетные данные` — recorded at `credentials`
- `Монетизация с помощью Play → Продукты → Встроенные продукты → Создать продукт` — recorded at `billing.productForm`
- `Монетизация с помощью Play → Promotions` — recorded at `billing.promotions`
- `Play Console → страница приложения` — recorded at `screens → app-status`
- `Магазин → Основная информация о магазине` — recorded at `screens → store-listing`
- `Контент приложения → Политика конфиденциальности` — recorded at `screens → declaration-privacy-policy`
- `Контент приложения → Реклама` — recorded at `screens → declaration-ads`
- `Контент приложения → Возрастные ограничения` — recorded at `screens → declaration-content-rating`
- `Контент приложения → Целевая аудитория и возрастные ограничения` — recorded at `screens → declaration-target-audience`
- `Контент приложения → Безопасность данных` — recorded at `screens → declaration-data-safety`
- `Контент приложения → Рекламный идентификатор` — recorded at `screens → declaration-advertising-id`
- `Контент приложения → Приложения государственных учреждений` — recorded at `screens → declaration-government-apps`
- `Контент приложения → Финансовые функции` — recorded at `screens → declaration-financial-features`
- `Контент приложения → Приложения для здоровья` — recorded at `screens → declaration-health-apps`
- `Тестирование → <нужный трек> → Создать выпуск; для публикации в магазине — Рабочая версия → Создать выпуск` — recorded at `screens → release`
- `Тестирование → <нужный трек> → вкладка «Страны/регионы»; для публикации в магазине — Рабочая версия → вкладка «Страны/регионы»` — recorded at `screens → release.subscreens → country-availability`
- `Тестирование → <нужный трек> → выпуск → «Посмотреть и подтвердить» → `Сохранить`; для публикации в магазине — Рабочая версия` — recorded at `screens → release.subscreens → release-saved-modal`
- `Обзор публикации → кнопка `Отправить N изменений на проверку`` — recorded at `screens → publishing.subscreens → submit-dialog`
- `Обзор публикации → уведомление рядом с кнопкой отправки` — recorded at `screens → publishing.subscreens → eu-geo-blocking-notice`
- `Обзор публикации → после `Отправка изменений на проверку`; список приложений (`/app-list`)` — recorded at `screens → publishing.subscreens → post-submit-states`
- `Панель управления → диалог «Заявки»` — recorded at `screens → publishing.subscreens → declarations-dialog`
- `Настройки → Тестирование лицензий` — recorded at `screens → billing-license`
