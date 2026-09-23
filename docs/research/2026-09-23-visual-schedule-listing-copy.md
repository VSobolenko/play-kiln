# Visual-schedule slot — draft listing copy (en-US, ru-RU)

**Status: draft pack for review, not final copy.** This file is a *compliance artifact*:
the store listing for this slot is the thing most likely to pull a scheduling utility into
Google Play's health policy, so the copy is written to be defensible against
[`2026-09-23-autism-app-policy.md`](./2026-09-23-autism-app-policy.md) §2.4 before it is needed.

- **Authority for every compliance statement here:** `docs/research/2026-09-23-autism-app-policy.md`
  (sections 2.4, 4, and 8 were read in full; §1, §5, §7 are referenced where they decide
  something). Where a statement is *my* application of those rules rather than a quoted rule,
  it says so. Anything I could not trace is marked **UNVERIFIED**.
- **Limits** (title ≤ 30, short description ≤ 80, full description ≤ 4000, default `en-US`,
  additional `ru-RU`): `AGENTS.md` → `## Store-pack contract`. The factory validator applies
  them with Python `len()` at `scripts/store-kit.py:463`. Every count below is measured, not
  estimated (§11).
- **What this file is not:** not legal advice, not a final decision on the target audience, and
  not an instruction to publish. The health-declaration fork in §8 is presented with its
  consequence, not decided.

---

## 1. Summary for the reviewer

| Item | Result |
|---|---|
| Recommended title | `Visual Schedule: Step Cards` — **27/30**, no condition word, no audience word, no effect verb |
| Maximum-neutrality alternative | `Picture Routine Steps` — **21/30** |
| Claim-free titles proposed | 5 of 7 (EN), 5 of 7 (RU); no title has a §2.4 claim |
| `en-US` short description | **78/80** |
| `en-US` full description | **1346/4000** |
| `ru-RU` short description | **73/80** |
| `ru-RU` full description | **1381/4000** |
| Sentences my own audit flagged as §2.4 borderline or must-remove | **5** — 3 cut from the recommended copy, 2 survive only as clearly-labelled condition-naming alternatives |
| Remaining §2.4 borderline/must-remove sentences in the recommended copy | **0** |
| Health declaration the recommended copy supports | **"My app doesn't provide any health features"** — *conditional* on the shipped functionality also being claim-free and condition-free (§8) |
| Health declaration a condition-naming title forces you to expect | In-scope declaration + the "not a medical device…" disclaimer + the "consult a healthcare professional" reminder (§8) |
| `ru-RU` confidence | **High** for natural, non-transliterated Russian; a native reviewer should still sign off before upload (§10) |
| Cost of the claim-free route | The highest-intent search term (`autism`) is given up (§9) |

**A compliant listing is possible without any disclaimer.** No sentence in the recommended copy
needed one; the disclaimer enters only if the factory chooses to name a condition. That is a
product decision with a price, and §8 states both branches.

---

## 2. The two axes (do not collapse them)

Two independent declarations are in play, and a listing can be clean on one and dirty on the other:

| Axis | Question | Where it is decided | Consequence |
|---|---|---|---|
| **Claims (§2.4)** | Does the copy claim a benefit for a condition, a person's development, or an outcome? | This file | Condition claim → health policy scope, disclaimer, Health Misinformation risk |
| **Audience (§1)** | Who is the declared user — the carer, or the child? | Play Console Target audience, reviewable and overridable by imagery/terminology (§1.2) | Children in the audience → Families Policy Requirements bind |

This file decides the first axis and only observes the second. A title such as
`Visual Schedule for Kids` makes **no §2.4 claim** and is therefore *permitted on the claims
axis*; it selects the **child-audience** axis, and its consequence is in §1, not §2.4. I state it
that way rather than calling it "borderline", because a false verdict in either direction is the
defect this factory keeps repeating.

---

## 3. Title candidates (`en-US`) — measured

Every count below is the real length of the real string; method and reproducibility in §11.
The compliance position quotes the §2.4 table.

#### EN-1 · `Picture Routine Steps` — 21/30
Claim-free. No condition word, no audience word, no effect verb. Position: **permitted** — a literal
description of a picture-based sequence (§2.4 permitted row).

#### EN-2 · `Visual Routine Cards` — 20/30
Claim-free. `visual` + `routine` are feature words; the §2.4 table attaches "borderline" to the
phrase *"A visual schedule for autistic children"* because of the **audience naming**, not because
of the feature words, so a bare feature phrase stays in the permitted row.

#### EN-3 · `What Happens Next? Cards` — 24/30
Claim-free, and the closest echo of the §2.4 permitted wording ("Shows what happens next, as
pictures"). Position: **permitted**.

#### EN-4 · `Picture Schedule Steps` — 22/30
Claim-free. Position: **permitted**.

#### EN-5 · `Visual Schedule: Step Cards` — 27/30 · **recommended**
Claim-free and keeps the searchable feature term `visual schedule`, which the §2.4 table treats as
a feature, not a condition. Position: **permitted**. Margin is 3 characters — do not append a
locale suffix to this one.

#### EN-6 · `Visual Schedule for Kids` — 24/30
Names an audience of children, names no condition, claims nothing. Position: **permitted on the
§2.4 claims axis**; **audience-axis consequence** under §1 — "kids"/"children" terminology is the
kind of thing §1.2 says Play may use to override a declared target audience, so pairing this title
with an adult-only Target audience declaration is the misdeclaration risk in §7.4 item 1.

#### EN-7 · `Visual Schedule: Autism` — 23/30
Names a condition and no benefit. Position: **§2.4 borderline** — the exact verdict of the
*"visual schedule for autistic children"* row: "Names no benefit, but introduces condition-related
information and moves the app toward health-policy scope (§2.3). Expect the Health apps
declaration question." Not a claim, and not a violation by any text in the policy doc — but it is
the wording that removes the honest option in §8.

There is no title among the seven with a §2.4 claim (the prohibited row — "supports autistic
children", "helps with transitions", "reduces meltdowns", "improves communication", "therapy",
"treatment", "clinically proven", "recommended by therapists"). I deliberately did not write one;
if a future reviewer wants one, it needs the §8 branch-B declaration, not this file.

### `ru-RU` titles — measured

#### RU-1 · `Карточки распорядка` — 19/30
Claim-free. Literal: "cards of the routine". Position: **permitted**.

#### RU-2 · `Расписание из карточек` — 22/30
Claim-free. Literal: "a schedule made of cards". Position: **permitted**.

#### RU-3 · `Визуальное расписание` — 21/30
Claim-free. The standard Russian term for a visual schedule. Position: **permitted**.

#### RU-4 · `Визуальное расписание: шаги` — 27/30 · **recommended for ru-RU**
Claim-free, mirrors EN-5. Position: **permitted**.

#### RU-5 · `Что дальше: карточки` — 20/30
Claim-free, mirrors EN-3 ("what happens next: cards"). Position: **permitted**.

#### RU-6 · `Визуальное расписание: дети` — 27/30
Audience-naming, no condition, no claim. Position: **permitted on the §2.4 claims axis**;
**audience-axis consequence** under §1, same as EN-6.

#### RU-7 · `Визуальное расписание: аутизм` — 29/30
Condition-naming. Position: **§2.4 borderline**, same as EN-7. 29/30 leaves 1 character of margin;
this title is not a candidate for any suffix.

---

## 4. `en-US` copy pack

#### Short description — en-US · 78/80

```text
Picture steps in order: a full-screen next step, a timer, and an editing view.
```

#### Full description — en-US · 1346/4000

```text
Picture Routine Steps

Picture Routine Steps is a picture-based schedule. You build a sequence of picture steps, and the app shows what happens next, as pictures, in the sequence you choose. It is a scheduling utility: you decide the steps, and the app presents them.

What you can do
• Create a routine and add picture steps to it
• Give each step a picture and a short label
• Put the steps in the order you want, and change the order later
• Present the routine full screen, one step at a time
• Run a timer for the current step
• Edit any routine in the editing view
• Keep several routines and switch between them

How the two views work
In the presentation view the screen shows one large picture and one step at a time. Routines are created and changed in the editing view.

Privacy
• Works offline. Nothing is sent off the device.
• No account and no sign-in
• No ads
• No analytics and no tracking
• Pictures are chosen with the Android system photo picker and stay on your device

Optional purchases
Optional picture packs can be bought through Google Play billing. There is no subscription. The price is shown before you confirm, and a purchase does not block routines you have already built.

Accessibility
• Large touch targets and picture-first cards
• Every card has a label for screen readers
• Nothing is conveyed by colour alone
```

#### Short description (variant B, audience-naming) — en-US · 62/80

```text
Picture routine cards for kids: full-screen steps and a timer.
```

Use only if the audience axis of §1 is being answered "children". It makes no §2.4 claim; it does
put child-audience terminology into metadata, which §1.2 says Play may weigh.

---

## 5. `ru-RU` copy pack

A genuine translation, not a transliteration: the Russian reads as Russian product copy
(«составляете последовательность шагов», not «визуальный график для детей» calqued word for word),
and the sentence structure is not the English one carried across.

#### Short description — ru-RU · 73/80

```text
Картинки-шаги в нужном порядке: полноэкранный шаг, таймер и режим правки.
```

Measured note: the more textbook phrase `режим редактирования` (editing mode) does **not** fit —
substituting it into this sentence yields **81/80** (§11). `режим правки` is correct, idiomatic
Russian and is what fits.

#### Full description — ru-RU · 1381/4000

```text
Карточки распорядка

«Карточки распорядка» — это расписание из картинок. Вы сами составляете последовательность шагов с картинками, а приложение показывает, что будет дальше, — в том порядке, который выбрали вы. Это планировщик: шаги задаёте вы, приложение их показывает.

Что можно делать
• Создать распорядок и добавить в него шаги с картинками
• Дать каждому шагу картинку и короткую подпись
• Расставить шаги в нужном порядке и позже его изменить
• Показывать распорядок на весь экран, по одному шагу за раз
• Запускать таймер для текущего шага
• Изменять любой распорядок в режиме правки
• Хранить несколько распорядков и переключаться между ними

Как устроены два экрана
В режиме показа на экране одна крупная картинка и один шаг за раз. Распорядки создаются и меняются в режиме правки.

Приватность
• Работает без интернета. Данные не отправляются с устройства.
• Без аккаунта и без входа
• Без рекламы
• Без аналитики и отслеживания
• Картинки выбираются через системный выбор фото Android и остаются на вашем устройстве

Дополнительные покупки
Дополнительные наборы картинок можно купить через оплату Google Play. Подписки нет. Цена видна до подтверждения, и покупка не блокирует уже созданные распорядки.

Доступность
• Крупные элементы и карточки с картинками
• У каждой карточки есть подпись для программ чтения с экрана
• Ни один элемент не обозначается только цветом.
```

#### Short description (variant B, audience-naming) — ru-RU · 59/80

```text
Карточки распорядка для детей: полноэкранные шаги и таймер.
```

---

## 6. Claim audit — adversarial against my own copy

Scope: every sentence in §4 and §5 that could be read as implying a benefit, plus the rows I cut
while auditing. Verdict column quotes the §2.4 table.

### 6.1 Cut during the audit (they were in my drafts)

| Sentence (as drafted) | Read as | §2.4 verdict | Action |
|---|---|---|---|
| "A sequence for leaving the house" | A named friction scenario, i.e. the transition use case that §2.4's prohibited row is about | **borderline** | **Cut.** Naming a scenario adds no feature information and sits one verb away from "helps with transitions". Remaining bullets name only what the app does |
| "The presentation view is built to be simple and readable" | UI quality presented as a comprehension benefit | **borderline** | **Cut**, replaced with the structural "the screen shows one large picture and one step at a time" |
| "If you want to see the app before you commit, the first routine you build is free." | A monetisation promise that cannot be verified before the slot is built | **must-remove as written** — §2.1: "Apps must provide an accurate disclosure, description and images/video of their functionality in all parts of the metadata" | **Cut.** Not a health claim; an accuracy defect |

### 6.2 In the recommended copy — flagged and cleared

| Sentence | Read as | §2.4 verdict |
|---|---|---|
| "Picture Routine Steps is a picture-based schedule." | Self-classification as a scheduler | **permitted** — literal functionality |
| "You build a sequence of picture steps, and the app shows what happens next, as pictures, in the sequence you choose." | The §2.4 permitted row almost verbatim | **permitted** |
| "It is a scheduling utility: you decide the steps, and the app presents them." | Category statement | **permitted** — and it helps a reviewer read the app correctly |
| "In the presentation view the screen shows one large picture and one step at a time." | Layout description | **permitted** |
| "Optional picture packs can be bought through Google Play billing. There is no subscription." | Monetisation disclosure | **permitted** — and required for truthful metadata; §4.2 asks for non-coercive, clearly-priced disclosure. No manipulation, no virtual-currency blurring |
| "The price is shown before you confirm, and a purchase does not block routines you have already built." | Non-coercion assurance | **permitted** — but it is a behaviour promise; see §7 |
| "«Карточки распорядка» — это расписание из картинок." / "Это планировщик: шаги задаёте вы, приложение их показывает." | Literal description (RU) | **permitted** |
| "В режиме показа на экране одна крупная картинка и один шаг за раз." | Layout description (RU) | **permitted** |

### 6.3 Audit result, counted

- **Flagged as §2.4 borderline or must-remove: 5.** Three were cut from the recommended copy
  (§6.1) and two survive only as clearly-labelled alternatives: `Visual Schedule: Autism` (EN-7)
  and `Визуальное расписание: аутизм` (RU-7), both **borderline**.
- **Flagged and remaining inside the recommended copy: 0** (0 borderline, 0 must-remove).
- **Not counted as §2.4 claims** (they are audience-axis items of §1, listed so they are not
  mistaken for clean): `Visual Schedule for Kids` (EN-6), `Визуальное расписание: дети` (RU-6),
  and the two variant-B short descriptions.
- **No verb of effect appears anywhere in the recommended copy.** No "supports", "helps",
  "improves", "reduces", "calms", "develops", "progress", "therapy", "treatment", "clinically
  proven", "recommended by", "special needs", "sensory", "meltdown", "anxiety". I looked for each
  of them as a literal string in both locales.

The honest limitation: §2.4 carries the doc's own **UNVERIFIED** note that no Play policy
enumerates permitted or prohibited phrasings for autism-adjacent apps. These verdicts are the
application of the quoted rules, not quoted rules. A reviewer who disagrees with a "permitted"
row is disagreeing with my application of §2.4, not with a policy sentence — and §6.1 exists
precisely so that the disagreements happen here rather than at upload.

---

## 7. Functionality claims that need build verification (not health claims)

These are permitted *only if true of the shipped build*. They are not §2.4 benefit claims, but a
false one is still a Misleading Claims defect (§2.1) and, for the data rows, an inaccurate Data
safety answer (§5.1, §7.4 item 4). Verify each against the build and the final dependency list
before upload:

| Claim in the copy | Must be true of |
|---|---|
| "Works offline. Nothing is sent off the device." / "Работает без интернета. Данные не отправляются с устройства." | No upload path at all, **including any SDK** — §5.1: local-only processing is not "collection", but any SDK transmission becomes the developer's declaration |
| "No analytics and no tracking" / "Без аналитики и отслеживания" | The dependency list and the merged manifest; if any child age group is declared, §5.3 restricts SDKs separately |
| "No ads" | Must match the Play Console ads declaration (§7.2) |
| "No account and no sign-in" | The shipped auth surface |
| "Pictures are chosen with the Android system photo picker and stay on your device" | That the Photo Picker is used and no media permission beyond it is requested (§5.3) |
| "Large touch targets and picture-first cards" / "Крупные элементы и карточки с картинками" | ≥48dp targets as built (§6 guidance) |
| "Every card has a label for screen readers" | `contentDescription`/semantics coverage on every card |
| "Nothing is conveyed by colour alone." | The actual colour usage |
| "a purchase does not block routines you have already built" | The pack gating logic — if a pack can gate an already-built routine, this sentence is false and must change |

---

## 8. The health-declaration fork

The policy doc (§2.3) states the declaration form and the two honest answers, and §7.4 item 2
states what a wrong answer costs. Stated plainly:

### Branch A — "My app doesn't provide any health features."

The Console wording quoted in §2.3 is: *"If your app doesn't provide any health features or access
health data to support non-health features, you should declare: My app doesn't provide any health
features."*

**This answer is honest only when both conditions hold:**

1. **Shipped functionality is claim-free.** A picture-sequence scheduler with a timer, an editing
   view, and picture packs that are generic (morning, bedtime, leaving the house). No condition
   content, no symptom/behaviour tracking, no clinical vocabulary, no advice, no health data
   access. §2.3's trigger is about functionality and information, and one condition-branded
   content pack inside the app is functionality — copy cannot compensate for it.
2. **The listing makes no health claim.** That is exactly the copy in §4 and §5: no condition
   word, no audience-condition phrase, no verb of effect, no outcome, no health-feature category,
   and screenshot graphics that show the UI instead of asserting a benefit (§2.1 puts
   "description, title, icon, and screenshots" in scope).

**Which copy in this file supports branch A:** the recommended claim-free set —
titles EN-1…EN-5 / RU-1…RU-5, short descriptions `en-US`/`ru-RU` A, both full descriptions, and
the caption/bullet appendix in §12. Those are sufficient; nothing else about the app is needed.

**One inference, labelled as such:** branch B's disclaimer text ("not a medical device…", "consult
a healthcare professional") is quoted in §2.2 as an obligation *of apps in scope*. The policy doc
does not say "do not include the disclaimer when you declare no health features", but §2.3 presents
the two answers as the honest alternatives. My reading — an inference from §2.3, not a quoted
rule — is that the branch-A listing should **not** carry the disclaimer, because the disclaimer is
in-scope-app boilerplate and would contradict the declaration on the same page. If a reviewer
disagrees, the text to add is in branch B below.

### Branch B — in-scope declaration + disclaimer

The required wording, quoted in §2.2: a clear disclaimer *"indicating that the app is 'not a
medical device and does not diagnose, treat, cure, or prevent any medical condition'"*, plus the
obligation that *"Apps must also remind users to consult a healthcare professional for medical
advice, diagnosis, or treatment."* A Russian rendering, if this branch is chosen (translation mine,
not policy text):

> Приложение не является медицинским изделием и не предназначено для диагностики, лечения или
> профилактики каких-либо заболеваний. По вопросам здоровья, диагностики и лечения
> проконсультируйтесь с врачом.

**Wording that forces branch B** — any one of these is enough:

| Trigger | Source |
|---|---|
| A condition named anywhere in title, short description, full description, or screenshot text: `autism`, `autistic`, `ASD`, `ADHD`, `neurodivergent`, `special needs` | §2.3: a listing that says "for autistic children" is "health-related *information* about a condition, and the reviewer can reasonably treat the app as in scope"; §2.4 borderline row |
| Any effect on a person or condition — the §2.4 prohibited row, verbatim: "supports autistic children", "helps with transitions", "reduces meltdowns", "improves communication", "therapy", "treatment", "clinically proven", "recommended by therapists" | §2.4 |
| An outcome or development claim in any phrasing ("calmer days", "fewer meltdowns", "better transitions", "makes progress", "builds independence") | §2.4 dividing line: "claims about a condition, a person's development, or an outcome" |
| A health-feature category: condition management, mental/behavioural health support, rehabilitation, therapy exercises, appointment reminders | §2.3 declarable categories |
| A declared in-app feature that manages or tracks a condition | §2.3 |

**What branch B costs, in the doc's own terms:** the app enters Health Content and Services scope
(§2.1); the description must carry the disclaimer and the consult-a-professional reminder (§2.2);
any benefit claim that is unsupported additionally risks Health Misinformation enforcement — *"We
don't allow apps containing misleading health claims that contradict existing medical consensus,
or can cause harm to users"* (§2.1) — and the disclaimer does not launder such a claim. The
in-scope declaration must also stay consistent with Data safety and the content-rating answers
(§7.4 items 2 and 6).

### What the fork is not

It is **not** the audience fork. Answering "no health features" does not settle §1: whether the
declared user is the carer or the child is a separate decision with its own consequences (§1.3),
and §1.2 warns that a wrong answer there is itself actionable. Choosing branch A does not license
`Visual Schedule for Kids`.

**The honest bottom line:** a compliant listing is possible without any disclaimer, and this file
contains it. The fork's price is distribution, not risk: branch A gives up the word `autism`, which
is the phrase the market scan shows people search. That trade belongs to the human, not to me.

---

## 9. Keywords without claims

Play has no keyword field; the keywords are the words in the metadata above. The policy doc gives
three different weights to the words in this niche, and they should not be treated alike:

| Keyword | Weight | Where it comes from | Recommendation |
|---|---|---|---|
| `visual schedule` | Neutral **feature** word | §2.4 attaches "borderline" to *"A visual schedule for autistic children"* because of the audience naming, not the feature words; the permitted row describes exactly this feature | **Use** |
| `picture schedule`, `picture cards`, `steps`, `sequence`, `timer`, `full screen`, `offline`, `no ads`, `daily routine`, `morning routine`, `bedtime routine` | Neutral feature words | §2.4 permitted row (literal description of the functionality) | **Use** |
| `routine` | Neutral | Not a condition word and not a claim word | **Use** |
| `transition` | **Caution** | "helps with transitions" is in the §2.4 prohibited list. The bare noun is not itself prohibited, but every natural collocation ("transition support", "eases transitions", "transition helper") lands inside the prohibited framing | **Avoid in the metadata.** Prefer `what happens next` / `next step`, which the permitted row already sanctions |
| `autism`, `ASD`, `ADHD`, `neurodivergent`, `special needs` | **Condition words — advise against** | §2.3/§2.4 borderline row: condition-related information, "expect the Health apps declaration question". §2.3 also records the absence of any rule that a diagnosis word by itself is a declaration | **Avoid in the branch-A set.** Including one is the single change that moves this file to branch B |
| `therapy`, `treatment`, `ABA`, `speech`, `communication`, `development`, `milestones`, `sensory`, `calm`, `anxiety`, `meltdown` | **Claim words — advise against** | §2.4 prohibited row and the dividing line (development/outcome claims) | **Never use** |
| `kids`, `children`, `for families` | Audience words, **not** claims | §1.2: imagery and terminology can make Play override the declared target audience | **Use only if the child audience is the declared one**; never as an afterthought |

Recommended keyword set for the branch-A listing: **visual schedule, picture schedule, routine,
daily routine, morning routine, bedtime routine, picture cards, step by step, timer, offline, no
ads.** That set is honest, describes shipped functionality, and manufactures no claim.

---

## 10. `ru-RU` confidence

**High.** The Russian above is written as Russian product copy, not translated word-by-word: it
uses «расписание из картинок», «шаги с картинками», «режим правки», «подпись» where a literal
translation would have produced calques, and it keeps the same claim-free constraint in the
Russian (no «поддерживает», «помогает», «улучшает», «развивает», «терапия», «лечение»,
«аутизм» — I checked each of those as literal strings).

**Residual caveat, stated honestly:** I am confident in the language, not in the reviewer's taste.
The store pack is a compliance artifact, so a native speaker should sign off on the `ru-RU`
locale before upload. Nothing here is machine translation, and the two count-driven edits
(`режим правки` instead of `режим редактирования`) are documented in §5.

---

## 11. Measurement method (so the numbers can be re-derived)

- Counts are **Unicode code points**, the same unit Play's limits and the factory validator use:
  `scripts/store-kit.py:463` checks `("title", 30), ("shortDescription", 80), ("fullDescription", 4000)`.
- The measurement was a real computation over the real string (`String.Length` in a Windows
  PowerShell session whose strings are UTF-16; every string here is **BMP-only — verified:
  0 surrogate-pair code units in every measured string** — so UTF-16 code units == code points).
- Full descriptions are measured **LF-only, with no trailing newline**, matching what
  `listing.json` will store (`\n` separators). Verified: 0 `\r` characters.
- Reported counts, in order:

| Item | Count | Limit |
|---|---:|---:|
| EN-1 `Picture Routine Steps` | 21 | 30 |
| EN-2 `Visual Routine Cards` | 20 | 30 |
| EN-3 `What Happens Next? Cards` | 24 | 30 |
| EN-4 `Picture Schedule Steps` | 22 | 30 |
| EN-5 `Visual Schedule: Step Cards` | 27 | 30 |
| EN-6 `Visual Schedule for Kids` | 24 | 30 |
| EN-7 `Visual Schedule: Autism` | 23 | 30 |
| RU-1 `Карточки распорядка` | 19 | 30 |
| RU-2 `Расписание из карточек` | 22 | 30 |
| RU-3 `Визуальное расписание` | 21 | 30 |
| RU-4 `Визуальное расписание: шаги` | 27 | 30 |
| RU-5 `Что дальше: карточки` | 20 | 30 |
| RU-6 `Визуальное расписание: дети` | 27 | 30 |
| RU-7 `Визуальное расписание: аутизм` | 29 | 30 |
| Short A `en-US` | 78 | 80 |
| Short variant B `en-US` | 62 | 80 |
| Short A `ru-RU` | 73 | 80 |
| Short variant B `ru-RU` | 59 | 80 |
| Full `en-US` | 1346 | 4000 |
| Full `ru-RU` | 1381 | 4000 |
| Rejected variant: `ru-RU` short A with `режим редактирования` | 81 | 80 — over by 1, hence the §5 wording |

A re-derivation script must read each measured string out of this file between its heading and its
fenced block; if the numbers in this table and the strings in §4/§5 ever disagree, the strings win
and the table is stale.

---

## 12. Appendix — screenshot captions and feature bullets (claim-free)

Included because §2.1 puts screenshots inside metadata accuracy, and §1.2 warns that listing
graphics can drive the audience assessment. The store-pack contract requires six captions and at
least three bullets per locale; no per-caption character limit is defined in `AGENTS.md`, so counts
are reported rather than asserted against a limit. Counts are in parentheses.

`en-US` captions (37, 38, 39, 32, 36, 35):
1. Picture steps in the order you choose (37)
2. Add a picture and a label to each step (38)
3. Present one step at a time, full screen (39)
4. Run a timer for the current step (32)
5. Change the order in the editing view (36)
6. Keep several routines on the device (35)

`en-US` feature bullets (40, 44, 26, 41, 36):
- Build picture step sequences you control (40)
- Full-screen presentation, one step at a time (44)
- Timer for the current step (26)
- Editing view for changing steps and order (41)
- Works offline; no account and no ads (36)

Graphics rule that follows from §2.4: screenshots show the app's own UI (a step list, a full-screen
step, the timer, the editing view). No screenshot states or implies a benefit, and no screenshot
text carries a condition word. If listing art is AI-generated, §7.2 requires the per-asset
AI-generated declaration — a Console flow item, not copy.

`ru-RU` captions (37, 35, 29, 24, 38, 35):
1. Шаги с картинками в выбранном порядке (37)
2. Картинка и подпись для каждого шага (35)
3. Один шаг за раз на весь экран (29)
4. Таймер для текущего шага (24)
5. Порядок шагов меняется в режиме правки (38)
6. Несколько распорядков на устройстве (35)

`ru-RU` feature bullets (33, 35, 24, 28, 46):
- Вы сами задаёте шаги с картинками (33)
- Показ на весь экран, по одному шагу (35)
- Таймер для текущего шага (24)
- Режим правки: шаги и порядок (28)
- Работает без интернета; без аккаунта и рекламы (46)

---

## 13. UNVERIFIED and open items

Carried from the policy doc unless noted; nothing below is a finding in either direction.

1. **UNVERIFIED (policy doc §2.3):** whether Play Console's Health Apps review classifies a
   claim-free schedule utility as health-related is a **Console review decision**, not something
   published policy resolves. So "branch A is available" is my reading of the doc's own two
   answers, and the Console decides in practice.
2. **UNVERIFIED (policy doc §2.4):** no policy was found that enumerates permitted or prohibited
   *phrasings* for autism-adjacent apps. Every verdict in §6 is the application of the quoted
   rules, not a quoted rule.
3. **UNVERIFIED (policy doc §2.2):** the claim that the health disclaimer must appear in the
   *first paragraph* of the description is secondary-source only. It does not affect this file
   unless branch B is chosen, and if it is, read the Play Console field when the app is created.
4. **Inference, not a quoted rule (mine, from §2.3):** that the branch-A listing should not carry
   the branch-B disclaimer text. Stated with its reasoning in §8 so it can be overruled.
5. **UNVERIFIED (mechanics, not policy):** whether Play's own field counter agrees exactly with
   the code-point count measured here. Play limits are in characters and every string here is
   BMP-only, so the counts should be identical, but the Console's live counter was not consulted;
   re-check the four copy fields against the Console input fields at upload time.
6. **UNVERIFIED (product, must be resolved by the build):** every row in §7. The listing text is
   only permitted if the shipped app makes it true. Until the slot is built and its dependency
   list is final, §7 rows are claims on credit.
7. **UNVERIFIED (product, resolves the fork):** whether any planned picture pack is
   condition-branded. §2.3's trigger is functionality and information, so one such pack moves the
   app to branch B no matter what the listing says. This file cannot see the pack list.
8. **UNVERIFIED (business, not policy):** the cost of the claim-free route. Giving up `autism` as
   a search term is a distribution decision, and I have no data on its size beyond the fact that
   the term is what this market searches for.
9. **Not verified here and out of scope:** legal questions (COPPA/GDPR purchase consent, EU
   accessibility legislation, medical-device classification), the Designed for Families page
   status, and the exact-alarm review decision — all flagged in the policy doc and none of them
   decidable from a listing file. The audience axis of §2 is a Console declaration and is not
   decided by this file.
