# Play publishing playbook

This is the ordered handoff for taking a verified PlayKiln slot live. **Factory**
means automated by the harness; **Human** means a Play Console or legal action that
the factory must not perform silently.

## 1. Confirm account prerequisites — Human

1. Complete developer verification and use the intended personal or organization
   developer account.
2. If the personal account was created after **2023-11-13**, recruit at least **12**
   testers and keep them opted into a closed test continuously for **14 days** before
   applying for production access. Plan about three weeks for the first launch.
3. Confirm the account can register the intended package name.

The closed-test rule above is verified only for new personal accounts. Organization-
account-specific prerequisites are **NEEDS-VERIFICATION** because `FACTS.md` does not
state them.

Sources: **NEEDS-VERIFICATION** — `FACTS.md` supplies no official account, verification, or closed-test URLs.

## 2. Ship and verify the slot — Factory, with two human checkpoints

The slot has two phases and the owner stands between them.

**Phase 1 — `/ship <idea>`.** After plan approval the factory creates the slot, implements it,
tests it and produces a **debug** build to review. It does **not** sign, render store assets or
capture screenshots; that work is wasted if you want the app changed.

**Human checkpoint — approve the release.** Install the debug APK
(`app/build/outputs/apk/debug/app-debug.apk`), check the UI and behaviour yourself, and state
the approval. Only then is it recorded:

```bash
node scripts/approve-release.mjs --slot <slug> --note "<your own words>"
```

The record is bound to a digest of the app's sources, so editing the app after approving it
invalidates the approval and the release tools refuse until you approve the revision you
actually reviewed (`.cursor/rules/release-gate.mdc`).

**Phase 2 — `/release <slug>`.** Now the factory signs the releases, composes the store pack,
captures screenshots, verifies the slot and runs `/eval`.

Required handoff evidence:

```text
release-approval.json (the recorded approval this phase ran against)
dist/<slug>-<version>-<code>-release.apk
dist/<slug>-<version>-<code>-release.aab
dist/release-manifest.json
store/listing.json and listing.md
store/assets/icon-512.png
store/assets/feature-graphic-1024x500.png
6 phone + 4 seven-inch + 4 ten-inch screenshots per locale
green verify.mjs --build
accepted /eval
```

The human reads the final summary and diff. A worker's “done” is not evidence.

Sources: factory contract in `AGENTS.md`; no external citation required.

## 3. Create the Play app — Human

Create the app in Play Console and register the exact `applicationId` from
`release-manifest.json`. Do not change a slot's package name during upload.

The Play Developer API cannot create a new app, so this cannot be automated.

Sources: <https://developers.google.com/android-publisher/edits> ("You can only use
this API to make changes to an existing app (that has at least one APK uploaded)").
Until the first manual release exists, a draft app also accepts only draft releases
through the API — see <https://developers.google.com/android-publisher/tracks>.

## 4. Establish signing — Factory + Human

**The runbook is [`docs/signing.md`](signing.md)**, and the binding boundary is
`.cursor/rules/signing-keys.mdc`: where the key lives and why outside the repository, what
`keystore.properties` holds, how a key is created and the approval that gates it, and the backup. This
section keeps only what belongs to the publishing handoff — the fingerprint comparison and the rotation
reference.

**Factory:** `scripts/sign-release.mjs` configures or inspects the local upload key,
keeps the keystore outside Git, reads ignored `keystore.properties`, and reports the
upload-certificate SHA-256 fingerprint. `scripts/prepare-release.mjs` builds the
release APK/AAB, verifies the APK signature, and records the fingerprint in
`dist/release-manifest.json` as `signing.fingerprintSha256`.

**Human:** enroll the app in Play App Signing during the Console flow and compare the
expected upload certificate with the factory's recorded fingerprint before upload.

Factory upload-key material and Play App Signing are separate control surfaces. The
exact external distinction between the upload key and Google's app-signing key is
**NEEDS-VERIFICATION** because `FACTS.md` provides no signing documentation; `docs/signing.md`
records the same label for the same reason rather than resolving it by assertion.

For key rotation or reset — reference only, the procedure is not invented here:

1. Stop release automation; do not replace a key silently.
2. Follow the current Play Console key rotation/reset flow. The exact eligible flow,
   waiting periods, and Console labels are **NEEDS-VERIFICATION**.
3. Store the approved replacement key outside Git and update the ignored local
   signing configuration.
4. Re-run `node scripts/sign-release.mjs --slot <slug>` and compare the new SHA-256
   fingerprint with the Console record.
5. Re-run release preparation and verification before any upload.

Sources: **NEEDS-VERIFICATION** — no official Play App Signing or key-rotation URL is present in `FACTS.md`.
The factory's own half of this section is measured in `docs/signing.md`; `FACTS.md` remains the reason
the external distinction stays labelled rather than stated.

## 5. Perform the first upload — Human

Upload the signed AAB manually in Play Console. Complete any first-upload setup the
Console requires and confirm that the package name and upload certificate are
accepted. The factory must not attempt `play-upload.mjs` before this succeeds.

Uploading a bundle is not creating a release, and a draft release is not a submission. The
measured ladder that carries a draft release from this upload to `Изменения на проверке` is
**§5.1** below; it supersedes any instruction that names a dashboard label as the control that
submits anything.

Sources: **NEEDS-VERIFICATION** — `FACTS.md` states the first AAB must be uploaded manually but provides no official URL.

### 5.1 The release-submission ladder — recorded 2026-09-29

Written after the factory's first real slot (`Mark Book for Teachers`, `com.gexetr.teachergradebook`,
app id `4973392289635183576`) was actually submitted — and after an earlier attempt to follow this
repository's own instructions failed at **every** control, because the instruction named labels that
are not controls. Everything below was read off the live RU console on 2026-09-29 and walked end to
end. It is written to be followed by a human who has never opened the console and to be parsed as a
procedure by a bot.

**Markers.** `[console]` — a console action a bot performs. `[verify]` — a read-only check.
`[human]` — an action only the owner may take; today this family is **uploading the store
graphics**, named in precondition B below. The first-publication legal declarations are no longer in
it: the agent performs them once the slot carries a recorded release approval, because the approval
is the human checkpoint and not the click (owner's decision, recorded 2026-10-03). Everything else in
this ladder is console-mandatory and unattended. `NEEDS-VERIFICATION` marks a claim nobody has
measured.

**Field-level detail is deliberately not restated here.** Page paths, field names, character caps,
asset dimensions, the exact string to paste, and which slot value feeds which field are owned by the
human checklist [`docs/play-console-checklist.md`](play-console-checklist.md). This ladder
sequences those screens; it does not duplicate them. Numbers quoted here (`Выполнено: 2 из 5`,
`Отправить 12 изменений на проверку`) are console *forms* — another app prints other numbers.

#### Preconditions — what must be true before the ladder can be walked

**Precondition A — the two first-publication declarations. `[console]`, after the approval**

- *Where:* `Панель управления` (`.../app-dashboard`) → dialog `Заявки`.
- *What the agent does:* tick `Подтвердить, что приложение соответствует Правилам программы для
  разработчиков` and `Принять экспортное законодательство США`, **once the slot carries a recorded
  release approval**. These are accepted **once per app** rather than per release, and no field of the
  pack carries them — the factory transcribes the two rows and their captions and stops. In the
  observed run both were already ticked and the dialog's primary button was disabled, which is the
  evidence that nothing remained to save there.
- *Why it is agent-performed:* the approval is the human checkpoint — the owner reviews the plan and
  the policies before the initial publication and says so in his own words — and the click is not. The
  owner's decision, recorded 2026-10-03: «В нашем заводе есть правило, что перед публикацией
  необходимо одобрение владельца. Собственно говоря, в то время и я проверяю проверку всех вот этих
  политик, и только после моего одобрения я говорю разрешить публикацию. Поэтому, если я разрешил
  публикацию, значит эти галочки можно уже автоматически нажимать. Потому что все проверки
  необходимые для публикации проводятся до начальной публикации всего приложения.»
- *What is already automated:* nothing about the two marks themselves beyond this rule; do not infer
  them from a slot's capabilities or from its store pack.

**Precondition B — the store graphics. [human]**

- *Where:* `Магазин → Основная информация о магазине` (`/main-store-listing`), the five required
  image fields (icon, feature graphic, and the phone / 7-inch / 10-inch screenshot sets). Their
  names, dimensions, counts and caps are the checklist's *Магазин — тексты и контакт* section.
- *What the human does:* uploads the files. The pack already **renders** every one of them
  (`scripts/store/render.py`) and `scripts/media-capture.mjs` already **produces** the screenshots, but
  the upload itself cannot be done by the sandboxed browser the factory drives: the field is a
  hidden `input[type=file]` that only appears after `Добавить объекты`, and
  `DOM.setFileInputFiles` is denied by the browser sandbox. A human — or a driver that owns the
  file chooser — performs this step and nothing else.
- *Why it is a precondition:* while the dashboard row `Настройте страницу приложения` is open,
  nothing in the submission flow is reachable, and the submit button in step 6 stays disabled. The
  observed session cleared it exactly this way.

**Precondition C — the privacy policy must exist in two places, not one. [verify]**

- *Page:* `Контент приложения → Политика конфиденциальности` (`.../app-content/privacy-policy`),
  field `URL политики конфиденциальности`. In the observed run it read
  `https://telegra.ph/Mark-Book-for-Teachers--privacy-policy-09-26`.
- *The console's own prose on that page, verbatim:* `Следует разместить ссылку на политику
  конфиденциальности на странице приложения в Google Play и в самом приложении, даже если оно не
  предназначено для детей и не получает...` — the public URL satisfies one half of the
  requirement and the app satisfies the other. Hosting the URL is **the factory's** act, not the
  owner's: `node scripts/publish-privacy.mjs --slot <slug>` publishes `store/privacy.md` to a real
  public HTTPS page during development and records the URL (`docs/privacy-policy-hosting.md`,
  `.cursor/rules/privacy-policy.mdc`).
- **Defect this ladder surfaced — recorded, not fixed here.** The slot ships the policy **only** as
  a URL. `AboutScreen.kt` renders the support e-mail and the prose `about_privacy_body`, and no
  `Uri.parse`, `ACTION_VIEW` or `CustomTabsIntent` exists anywhere in the slot's Kotlin, so the
  in-app half is missing. No factory gate looked for it, so it passed every check.
- **Fix class:** the About screen opens the same URL through an openable link (`ACTION_VIEW` or
  `CustomTabsIntent`), plus a factory check that a slot declaring `listing.privacyPolicyUrl` has an
  openable-link call site in its own Kotlin. Not done in this change.

#### The ladder

**Step 1 — read the release goal; do not click its task labels.** `[verify]`

- *Page:* `Панель управления` (`.../app-dashboard`), goal card `Создайте и опубликуйте выпуск`,
  which counts **5 tasks**. Before the release existed the card read `Выполнено: 2 из 5` and its
  rows were, in order: `Выберите страны и регионы`, `Создайте и внедрите выпуск` (carrying the
  sub-rows `Создайте новый выпуск` and `Проверьте и подтвердите выпуск`), `Отправьте выпуск на
  проверку в Google`, `Опубликуйте приложение в Google Play`. The first two carried a check mark;
  the last three did not.
- *After:* you know how far the release is, and you have clicked nothing.
- **The trap that cost the owner his entire session — do not repeat it.** `Проверьте и подтвердите
  выпуск` and `Отправьте выпуск на проверку в Google` are **task labels inside a collapsed goal
  card**, not controls: they render as `span` inside `div.task-non-expandable`, they are neither
  links nor buttons, and clicking one only expands the card. The only clickable element in the card
  is a `div[role=button]` carrying `class="task-available"`, and in this session even that landed on
  `.../tracks/production?tab=releases` — the card is a *navigational shortcut*, not something that
  submits. **The one control that submits is in step 6, on `Обзор публикации`.** An instruction that
  says "нажмите «Отправьте выпуск на проверку в Google» на панели мониторинга" is precisely the
  instruction that failed, and it must not be written again.

**Step 2 — open the production release.** `[console]`

- *Page:* `.../tracks/production?tab=releases`, tab `Выпуски`.
- *Before:* the release exists.
- *Do:* nothing on this page except read it; the next click is `Изменить выпуск` (step 3).
- *After:* one row for the release, `1 (0.1.0)`, with status `Черновик`, and a button
  `Изменить выпуск` on that row.
- If no such row exists, the release has not been created yet: create it first. The create-release
  form (its fields, its `Далее`, and its one blocking error about countries and regions) is the
  walkthrough's *Создание выпуска* section.

**Step 3 — open the release form.** `[console]`

- *Do:* press `Изменить выпуск`.
- *Page:* `.../tracks/<trackId>/releases/<releaseId>/prepare` (the observed ids were
  `.../tracks/4699531965830041024/releases/1/prepare`), heading `Создание рабочего выпуска`, with a
  two-step header `1 Создать выпуск` / `2 Посмотреть и подтвердить`.
- *Before (this is the state the form must already be in, and it was):* release name `1 (0.1.0)`;
  the App Bundle attached, shown as version `1 (0.1.0)`, `26+`, target SDK 36; release notes present
  (483 characters in the observed run, starting from `<en-US>`); the side note
  `Включено улучшений: 2 из 3` under `Наборы App Bundle`.
- *After:* the bottom control `Далее` is **enabled**. If it is disabled, a required field of the
  step-1 form is empty — the field-level owner is the walkthrough's *Создание выпуска* section
  (release name and notes come from `store/release-notes.md`; the countries field is the one whose
  absence blocks the save). Nothing was typed by hand in the observed run: the form was already
  filled from the generated pack.

**Step 4 — review the release.** `[console]`

- *Do:* press `Далее`.
- *Page:* `.../tracks/<trackId>/releases/<releaseId>/review`, heading `Ошибки, предупреждения и
  сообщения`.
- *Before / what it says:* `1 предупреждение` — one warning, not an error — and the delivery table
  `Размер новых установок 1.68 МБ`, `Тип файла App bundle`, `Версия 1 (0.1.0)`, `Уровни API 26+`,
  `Целевой SDK 36`, `ABI 4`. Buttons `Назад` and `Сохранить`.
- **Do not read the banner as a blocker.** A banner reading `warning Обнаружены проблемы с выпуском`
  is the generic wording over that single non-blocking warning; the release saves into a draft. Play
  counts `Ошибка` and `предупреждение` separately and only the first blocks `Сохранить`.
- *After:* `Сохранить` is enabled. If the count reads an error instead, stop and fix it: the release
  does not save with one.

**Step 5 — save, and pass the dialog that hid the save last time.** `[console]`

- *Do:* press `Сохранить`.
- *After — and this is the part that must not be guessed:* an overlay dialog appears, titled
  `Перейти на страницу "Обзор публикации"?`, body `Изменение сохранено. Чтобы отправить его на
  проверку, перейдите в "Обзор публикации".`, with buttons `Не сейчас` and `Перейти к обзору`.
  **The save has already happened by the time this modal is on screen** — that modal is exactly what
  hid the save from the previous session, and there is no second save to look for. Choose
  `Перейти к обзору` to land on `.../publishing`.

**Step 6 — find the one control that submits, or read its lock.** `[console]`

- *Page:* `.../publishing`, heading `Обзор публикации`.
- *Before:* every dashboard task is closed (preconditions A and B, and the release goal's own
  `Выберите страны и регионы` / `Создайте и внедрите выпуск`).
- *After — the success state:* exactly **one enabled button, `Отправить 12 изменений на проверку`**.
  The number is the count of pending changes and varies per app; on the observed app it was `12`.
- *The lock state, and its exact symptom:* if the page prints
  `Чтобы отправить изменения на рассмотрение, выполните необходимые действия на панели управления
  приложением.` then a required task is still outstanding. That sentence is the symptom; the fix is
  to go close the outstanding dashboard task from step 1 and come back. Do not press anything, and
  do not hunt for a hidden second submit control — there is none.
- *Informational, nothing required:* the same page carries a collapsible notice
  `Регламент о необоснованной геоблокировке` / `Если вы распространяете приложения на территории
  Европейского союза, ознакомьтесь с Регламентом ЕС 2018/302.` with links `Подробнее` and a
  `Понятно` dismiss button. It asks for nothing; the console model owns its wording.

**Step 7 — confirm.** `[console]`

- *Do:* press `Отправить 12 изменений на проверку`.
- *After:* a confirm dialog, title `Отправить 12 изменений на проверку?`, body `Изменения будут
  отправлены на проверку в Google. Обычно она занимает до семи дней, но иногда может потребоваться
  больше времени.`, buttons `Отмена` and `Отправка изменений на проверку`. **There are no
  checkboxes and no certifications in this dialog** — the two first-publication declarations of
  precondition A live in the dashboard's `Заявки` dialog and are not re-asked here.
- *Do:* press `Отправка изменений на проверку`. The page returns to `Обзор публикации`.

**Step 8 — the pre-review check.** `[verify]`

- *After:* the page shows a section `Проверка на наличие распространенных проблем` with
  `Осталось: максимум 13 минут`, and the sentence `Такая проверка позволяет выявить определенные
  нарушения правил и проблемы с качеством приложения, чтобы вы могли устранить их, прежде чем
  отправить изменения на рассмотрение. Если на этом этапе не будет найдено проблем, изменения
  будут отправлены сразу.`
- *Read this literally:* **when the check finds nothing, the changes go to review immediately —
  there is no second confirm button to look for.** `Осталось: максимум 13 минут` is the console's
  own estimate and not a wait the factory has to schedule.

**Step 9 — the terminal state.** `[verify]`

- *After the check completes, on `.../publishing`:* `Изменения на проверке` and `Изменения
  находятся на рассмотрении. Если в приложении есть другие проблемы, мы обнаружим их на этом
  этапе.`, plus a button `Удалить изменения` (which is the only remaining control, and withdraws the
  submission).
- *On the account app list (`/app-list`):* the app's `Статус обновления` column reads
  `На рассмотрении`.
- That is the end of the ladder. Nothing further is pressed until Google's verdict; the review "can
  take a few hours or up to seven days (or longer in exceptional cases)" and no control accelerates
  it. Publishing after the review approves is §10–§11.

#### At a glance

| Step | What it is | Marker |
|---|---|---|
| A | First-publication declarations (`Правила программы…`, `Экспортное законодательство США`) | `[console]` (after the approval) |
| B | Upload the store graphics (five image fields) | `[human]` |
| C | Privacy policy URL in the listing **and** inside the app | `[verify]` (+ open defect) |
| 1 | Dashboard goal `Создайте и опубликуйте выпуск` — read only | `[verify]` |
| 2 | `.../tracks/production?tab=releases` → `Черновик` row | `[console]` |
| 3 | `Изменить выпуск` → `.../prepare` → `Далее` | `[console]` |
| 4 | `.../review` → `Сохранить` | `[console]` |
| 5 | Modal `Перейти на страницу "Обзор публикации"?` → `Перейти к обзору` | `[console]` |
| 6 | `Обзор публикации` → `Отправить N изменений на проверку` | `[console]` |
| 7 | Confirm dialog → `Отправка изменений на проверку` | `[console]` |
| 8 | `Проверка на наличие распространенных проблем` | `[verify]` |
| 9 | `Изменения на проверке` / `На рассмотрении` | `[verify]` |

**One contradiction to resolve, flagged rather than edited here.** The archived console model
(`docs/play-console-observations.json`, the frozen reading of the deleted `scripts/lib/console-form.json`)
still says of the submit button that "сама отправка остаётся
человеческим действием", and its `unreachable` entry `submit-dialog` records the confirm dialog as
never read. Both were true of the 2026-09-29 18:11 reading and were falsified by the same day's
later walk: the dialog was opened and read, and the press was performed from the console. The archive
is owned elsewhere; this ladder records what was measured so the two do not stay in conflict.

Sources: observed-console-2026-09-29 (the live RU walk of `com.gexetr.teachergradebook`: dashboard
goal `Создайте и опубликуйте выпуск` with `Выполнено: 2 из 5` and its five rows; the unreachable
task labels and the `div[role=button].task-available` shortcut that lands on
`.../tracks/production?tab=releases`; the `Черновик` row for `1 (0.1.0)`; the `prepare` and `review`
screens of `.../tracks/4699531965830041024/releases/1/`; the
`Перейти на страницу "Обзор публикации"?` modal; `.../publishing` with
`Отправить 12 изменений на проверку`, its confirm dialog, the
`Проверка на наличие распространенных проблем` section, and the terminal `Изменения на проверке`;
`/app-list` `Статус обновления` = `На рассмотрении`; `.../app-content/privacy-policy` field
`URL политики конфиденциальности` and the page's own two-places sentence); `AGENTS.md` §Play
compliance floor (the privacy policy in both places) and §Store-pack contract; `DESIGN.md`; the
generated [`docs/play-console-walkthrough.md`](play-console-walkthrough.md) for field-level detail.

## 6. Complete the store listing — Factory prepares, Human confirms

**Factory:** generates listing text within 30/80/4000-character limits, a 512×512
icon, a 1024×500 feature graphic, 6 phone screenshots, 4 seven-inch screenshots,
and 4 ten-inch screenshots per locale. It validates format, dimensions, alpha, byte
size, and count.

**Human:** confirm every claim and screenshot matches the uploaded build; review every
asset explicitly marked synthetic for truthful representation of shipped behavior;
provide demo credentials and review instructions when the app is gated.

Sources: **NEEDS-VERIFICATION** — `FACTS.md` names Play Console Help article 9866151 but provides no official URL.

## 7. Complete legal and audience declarations — Human, Data safety may be automated

Use the generated files as evidence, not as authorization to guess:

- Submit Data safety even if no data is collected; reconcile every SDK and the
  public privacy-policy URL.
- If accounts exist, verify deletion works in-app and through the declared external
  web resource.
- Complete the content-rating questionnaire.
- Declare whether the app contains ads.
- Complete target-audience declarations.
- Confirm permissions, foreground-service types, exact-alarm use, and reviewer access.

The human owns the truth and legal effect of these declarations. If code, policy,
and form disagree, stop and fix the app or the draft before submission.

### Capability-derived Data safety surface

Each slot records shared integrations in `apps/<date>-<slug>/capabilities.json`.
`enabled` is the explicit list of capability IDs. A billing slot also records its
consumable fulfilment (`LOCAL` or `SERVER_COST`); every `SERVER_COST` catalog must
describe `billing.purchaseAuthority`, the server that verifies purchase tokens and
owns the paid balance.

An empty `enabled` list is the explicit declaration that the slot uses none of the
shared capabilities — and since 2026-09-26 that is a **claim the economics gate checks**,
not a neutral default. A slot whose plan declares a purchase-led, subscription, or ads-led
archetype while `enabled` is empty is a plan the build cannot honour, and
`node scripts/check-economics.mjs --slot <slug>` reports `BLOCKED`. The failure this rule
exists to prevent is `apps/2026-09-25-pipe-sizing`, which shipped with `enabled: []`, a
listing that described a paid app, and no way to take money at all — every gate passed
(`docs/reviews/2026-09-26-slot-1-economics-postmortem.md`).

`scripts/lib/capabilities.mjs` reads that slot file and the selected
`capabilities/<id>/capability.json` manifests. It derives the permission and collected
data-type unions, bundled SDKs, ads presence, consent requirement, and purchase-authority
requirement. `scripts/store/render.py` writes those facts into `listing.json`, `data-safety.md`,
`privacy.md`, and `review-notes.md`; these fields are generated compliance evidence, not
free-form answers.

Before submission, run `node scripts/verify.mjs --slot <slug>
--build`. The strict gate compares the derived permissions with the build's merged
manifest, rejects ads or collected-data omissions in the store pack, and rejects a
`SERVER_COST` catalog without documented authority. Unknown capability IDs fail with
the valid ID list. The human still reviews the derived answers and owns their legal
submission; editing the generated prose to contradict the declarations cannot make the
gate pass.

Data safety is the one form with a documented write endpoint, so the factory may
upload it from a reviewed CSV instead of typing it by hand:
`POST /androidpublisher/v3/applications/{packageName}/dataSafety` with body
`{"safetyLabels": "<CSV contents>"}`. The CSV is still authored from the slot's real
SDK surface and approved by the human before it is sent; automatic upload of an
unreviewed CSV is a truthfulness risk and must not happen.

The privacy-policy URL is **not** settable through `edits.details` (which exposes
only `defaultLanguage`, `contactWebsite`, `contactEmail`, `contactPhone`), so it
stays a Console step.

Sources: <https://developers.google.com/android-publisher/api-ref/rest/v3/applications/dataSafety>,
<https://support.google.com/googleplay/android-developer/answer/10787469>,
<https://developers.google.com/android-publisher/api-ref/rest/v3/edits.details>.
Full surface: [`docs/research/2026-09-23-google-play-automation-surface.md`](research/2026-09-23-google-play-automation-surface.md).

## 8. Complete AI-content declarations when relevant — Factory + Human

**Factory:** verify that generative-AI features include in-app reporting or flagging,
do not produce prohibited content, and are more than a thin API wrapper.

**Human:** complete the applicable Console declarations and review the shipped safety
behavior. Do not publish if the reporting path cannot be demonstrated.

Sources: **NEEDS-VERIFICATION** — no official AI-generated-content policy URL is present in `FACTS.md`.

## 9. Test and request production access — Human

For an affected new personal account, maintain the 12-tester/14-day closed test and
then apply for production access. For other account types, follow the Console path
shown for that account; those account-specific rules are **NEEDS-VERIFICATION**.

Sources: **NEEDS-VERIFICATION** — no official testing or production-access URL is present in `FACTS.md`.

> **The ordered first-upload walkthrough is a human checklist, not a rendered document.**
> [`docs/play-console-checklist.md`](play-console-checklist.md) walks the console form
> in the order the console presents it — the dashboard state, the ten `Контент приложения`
> declarations, the credentials dialog, release creation, the publishing overview, and the billing
> steps — and names where each literal value comes from.
>
> The rendered document this replaced, `docs/play-console-walkthrough.md`, and its renderer
> `scripts/console-walkthrough.mjs` were deleted by `reduce-factory-to-core` §5.2; the console model
> they read survives only as the frozen reading
> [`docs/play-console-observations.json`](play-console-observations.json), which no program reads.
> The slot-level sheet `store/console-answers.md` was dropped by the same change (§14.7); the exact
> string to paste for a given slot now comes from the slot's own pack
> (`store/listing.json`, `store/listing.md`, `store/data-safety.md`, `store/release-notes.md`).

### 9.1 Internal testing, and the two links that are not the same link — recorded 2026-09-29

Written after a first launch stalled here: the owner uploaded the `.aab`, opened Internal testing,
added a tester email, clicked the opt-in link and got *"not found"*. **Uploading a bundle is not
creating a release.** They are separate objects in Console, and only the second makes a link exist.

**Rolling out is a distinct action.** Play's release flow is *Create a release* → *Prepare* →
*Review and roll out*, where the intermediate button is **Save as draft** (Russian: **«Сохранить как
проект»**) and the final one is **Start rollout** (Russian: **«Опубликовать выпуск»**). A draft is
not served to testers, so the link has nothing behind it. Source:
<https://support.google.com/googleplay/android-developer/answer/9859348?hl=en>.

**The opt-in link does not exist until the app is published.** Verbatim: *"The opt-in link displays
only when an app status is 'Published'. Apps in 'Draft' or 'Pending publication' status do not
display an opt-in link."* — <https://support.google.com/googleplay/android-developer/answer/9845334?hl=en>.
The status that matters is **App status**, shown under the app name; it is not the same as *Update
status* or *Item status* (<https://support.google.com/googleplay/android-developer/answer/9859751?hl=en>).
Read **App status** first — while it reads `Draft` or `No active releases`, nothing else can work.

**Four different Play URLs are easy to confuse, and only one opts a tester in:**

| URL shape | What it is |
|---|---|
| `play.google.com/apps/testing/<package>` | **The internal-testing opt-in link.** This is the one testers need |
| `play.google.com/apps/internaltest/<id>` | **Internal app sharing** — a different feature, needs developer mode, and fails when the app is not available to the user |
| App bundle explorer → *Copy shareable link* | Produces that same internal-sharing link |
| `play.google.com/store/apps/details?id=<package>` | The store listing; 404s until published and **enrols nobody** |

This matters inside this repository: `scripts/play-upload.mjs`'s standalone `internal-sharing` target
prints `artifact.downloadUrl`, which is the **internal app sharing** link, not the opt-in link. If a
tester is handed that URL the failure it produces is exactly this one. Check the path before sending
a link to anyone: `/apps/testing/` is correct, `/apps/internaltest/` is not.

**Expect a wait, not minutes.** After the first publish, *"the test link can take several hours to
become available to testers"* (9845334). And a brand-new **fully configured** app's first internal
rollout is itself reviewed: *"If your app's first release roll-out is on an Internal test track, the
submission must be reviewed before it can be published. Reviews can take a few hours or up to seven
days (or longer in exceptional cases)"* — <https://support.google.com/googleplay/android-developer/answer/9859654?hl=en>.
An app that is *not* fully configured rolls out immediately instead, with a temporary name for up to
48 hours. Either way the fix for a dead link is to wait or to roll out — never to resubmit, because a
resubmission restarts the queue.

### 9.2 Testing in-app purchases — a separate failure from the link — recorded 2026-09-29

The link and the purchase are gated by different things, and conflating them sends a developer
looking in the wrong place. The link depends on the **app's** publishing status; a purchase depends on
the **purchase option's** state and the **license tester** list.

1. **The product's ID must match the app exactly.** The slot declares it in code
   (`monetization/MonetizationConfig.kt`, e.g. `ProductId("recordbook_pro")`), and a purchase that
   references an unknown product fails in the reviewer's session. Create it under
   **Monetize with Play → Products → In-app products**.
2. **Activate the purchase option.** Play documents three states, and only one is purchasable:
   *Draft* — *"Purchase options in this state aren't returned by the Play Billing Library (PBL)"*;
   *Active* — *"returned by the PBL"*; *Inactive* — not returned. The state that matters in the
   current model is the **purchase option**, not the product.
   Source: <https://support.google.com/googleplay/android-developer/answer/16430488?hl=en>.
3. **License testers are account-level, not per-app, and are a different list from track testers.**
   **Settings → License testing** (Russian: **Настройки → Тестирование лицензий**). Verbatim:
   *"Testers need to pay for in-app purchases unless you add them to a license testers list"*
   (<https://support.google.com/googleplay/android-developer/answer/6062777?hl=en>). **An account that
   is only an internal tester on the track will be charged real money**, and the publisher's own
   account is always a license tester. Before running a purchase test, confirm the sheet shows a
   *test instrument*; a normal card means a real charge.
4. **A draft or internal-track app is under spend limits** — daily transaction count, per-order value
   and daily total, resetting every 24 hours; exceeding one blocks the transaction in the purchase
   cart. It stays that way until a production release ships.
5. **A test purchase must be acknowledged within three minutes** or it is refunded automatically
   (<https://developer.android.com/google/play/billing/test>).

**A never-published app can process test purchases.** Verbatim: *"An in-app product will be available
for purchase as long as it's active, even if its app is unpublished"*
(<https://support.google.com/googleplay/android-developer/answer/1153481>). The requirements are that
the app is uploaded and signed, the purchase option is active, the buyer is a license tester, and the
app is available in their region.

## 10. Roll out — Human approval, Factory may automate updates

A release reaches review through the ladder in **§5.1**; nothing can be rolled out before it has
been submitted there and approved. After approval, choose the production release in Play Console and
use a staged rollout when desired. The exact staged-rollout controls and percentages are
**NEEDS-VERIFICATION** because they are absent from `FACTS.md`.

After the first manual upload, `scripts/play-upload.mjs` may automate an approved
update through Android Publisher v3: create edit, upload bundle, patch listings,
upload images, update track, and commit. It uses service-account credentials from an
environment-provided local path. Never commit the JSON; never commit an edit without
explicit human approval.

Sources: **NEEDS-VERIFICATION** — no official edits/tracks/staged-rollout URL is present in `FACTS.md`.

## 11. Publish — Human

Review Console warnings, legal declarations, countries, price, and rollout choice.
Press **Publish** only when they match the approved release. This is the second human
checkpoint: the human presses Publish, and the action is never delegated to an agent.

Sources: human-control rule in `MISSION.md`; no external citation required.

## 12. Monitor after launch — Human + Factory

Monitor Android vitals and user feedback. Treat these as bad-behavior ceilings:

| Metric | Overall | Per device model |
|---|---:|---:|
| User-perceived crash rate | < 1.09% | < 8% |
| User-perceived ANR rate | < 0.47% | < 8% |
| Excessive partial wake locks | < 5% | — |

Also inspect p90 anonymous RSS plus swap, bitmap memory, and jank. Pause or repair a
rollout when evidence shows a release is unsafe; do not hide failures with listing
copy. Feed recurring faults back into `harness/lessons.md` and the root harness.

Sources: **NEEDS-VERIFICATION** — no official Android vitals URL is present in `FACTS.md`.
