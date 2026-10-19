# W94 — How product-photo / marketplace-card generators are actually built

Date: 2026-10-18. Research only; no code touched, no Gradle run.
Owner's instruction: measure the conventions, do not invent them.

## 0. Read this first: what was and was not opened

| Source | Result |
|---|---|
| WB official instruction `seller.wildberries.ru/instructions/.../photo-generation-using-a-neural-network-rb` | **Opened, but returned only the legal-agreement navigation list** (page is client-rendered). Zero flow content. The WB flow below therefore comes from a **secondary** article that states it was checked against that instruction on 2026-09-30. |
| `selsup.ru/blog/fotostudiya-wildberries-nejroset/` | **Opened, read ~28k chars of it** (screen captions, steps, limits). Secondary, and the vendor sells a competing tool. |
| `pixyn.ai/blog/ai-foto-dlya-wildberries-poshagovo-2026` | Opened (first 8k chars). Secondary, vendor blog. |
| `fahimai.com/how-to-use-photoroom` | Opened (~15k chars). Third-party tutorial, not Photoroom. |
| Photoroom help (`help.photoroom.com/...13743688...`, `...13003543...`) | **Loaded titles only, no body.** |
| `photoroom.com/tools/background-generator` | 404. `photoroom.com/blog/background-remover-mistakes` loaded one marketing line only. |
| `pebblely.com/`, `pebblely.com/faq/`, `claid.ai/` | Loaded a testimonial / refund snippet / one sentence. **No flow content.** |
| `canva.com/features/background-remover/` | 403. |
| `seller-edu.ozon.ru/.../redaktor-foto-ai` | Redirect loop (307), not loaded. |
| `web_search` answer summaries (Photoroom, Pebblely, Ozon, Photoroom Edit Cutout) | Search-engine paraphrases. Treated as **search-summary only**, never as "observed in the product". |

Consequence: nothing here was seen inside a running product. Every "observed flow" is a documented flow, and the
weaker the source, the weaker the label. Canva, Claid and Ozon flows are **UNVERIFIED** in full.

Labels: `MEASURED-DOC` = read in a loaded page; `SEARCH-SUMMARY` = only in a search-engine paraphrase;
`UNVERIFIED` = not established; `INFERENCE` = my engineering conclusion, not a fact about any product.

## 1. Pattern table

| # | Pattern | Which products do it | Why it exists (user problem) | Label |
|---|---|---|---|---|
| P1 | **One primary action per step** — a single generate button after choices are made | WB Фотостудия: «Сгенерировать» after model/background chosen; Pebblely: «Create»; Photoroom: "Generate" in each AI tool's own editor | A seller wants a finished photo, not a toolbox; one obvious next step removes "what do I press" | MEASURED-DOC (WB, Photoroom via fahimai); SEARCH-SUMMARY (Pebblely) |
| P2 | **Scenario picked first, before any editing** — "put garment on model" vs "replace model/background" | WB Фотостудия (two scenarios chosen by what the source photo contains) | Different inputs need different requirements; choosing first prevents a wasted paid attempt | MEASURED-DOC (selsup) |
| P3 | **Region/subject chosen before the generation** — «Модель / Фон / Модель и фон» with a green highlight over what will change | WB scenario 2 | Shows what will be replaced *before* spending a generation. «Не выбрано» → random replacement, so the preview is the safeguard | MEASURED-DOC (selsup) |
| P4 | **Automatic cutout, manual refinement afterwards** — cutout runs on import, then Erase / Restore brushes for misses | Photoroom (cutout automatic, then erase/restore) | Zero effort for the 90% case; brush only for the exception | MEASURED-DOC (fahimai: "cutout runs automatically… erase and restore brushes"); SEARCH-SUMMARY (Edit Cutout details: size, assisted mode, never creates pixels) |
| P5 | **Dedicated editor per AI tool**, not inline editing on the gallery | Photoroom ("each AI Tool has its own editor"); WB opens Фотостудия from the card, with a second entry from the photo editor side panel | A tool with its own canvas can own gestures, zoom and a result row without fighting a list | MEASURED-DOC |
| P6 | **Catalogue pickers, not free text, for the scene** — «Все модели» (sex/age/build/type filters, «Применить»), «Все фоны» (city, flat, nature, studio, sport) | WB; Pebblely (40+ themes + optional custom prompt, SEARCH-SUMMARY); Photoroom (preset scene or own prompt, fahimai) | Sellers do not write prompts; presets are the default, text is the escape hatch | MEASURED-DOC (WB, Photoroom); SEARCH-SUMMARY (Pebblely) |
| P7 | **Unchosen scene stays as the source** — no background chosen ⇒ original background is kept | WB scenario 1 | Predictable default; nothing changes that the user did not choose | MEASURED-DOC (selsup) |
| P8 | **Several variants, user picks one** — WB scenario 2 returns two variants; Pixyn returns 4; Photoroom tip "generate four, view side by side" | WB, Pixyn, Photoroom | Generation is non-deterministic; choice is the quality control | MEASURED-DOC |
| P9 | **Before/after disclosure by tapping the result** — "tap the result to open the before/after comparison" | WB scenario 2 | Lets the seller verify the product was not altered (colour, print, label) — WB moderation compares with the real item | MEASURED-DOC (selsup); moderation claim is vendor opinion (pixyn) |
| P10 | **Async generation, user may leave** — status «Генерируем изображение»; "you can leave the page, processing continues" | WB | Generation takes long; blocking the screen is a trap | MEASURED-DOC |
| P11 | **Quota visible inside the tool; cost before the press** — remaining attempts shown in the interface; «Купить ещё» button in both scenarios; Pixyn "shows the charge **before** launch" | WB, Pixyn; Pebblely per-image credits (1, or 2 in high-res) | Money/quota surprise is the main trust failure; failed technical runs are not charged, completed-but-disliked runs are | MEASURED-DOC (WB limits, Pixyn); SEARCH-SUMMARY (Pebblely) |
| P12 | **Result offers: download, add to card, regenerate** — regenerate = "another variant with the same settings" | WB | The next step is where the photo is *used*; regenerate keeps settings so one tap retries | MEASURED-DOC (selsup) |
| P13 | **Input requirements stated up front with good/bad examples** — JPG/PNG ≤ 10 MB, 700×900…2800×3600 px, front view, no mannequin, uniform neutral background; mismatch ⇒ "upload another image" | WB | An unsuitable photo burns a paid attempt; showing bad examples before upload is cheaper than an error after | MEASURED-DOC (selsup) |
| P14 | **Output spec of the destination is a first-class constraint** — WB card 3:4, 900×1200 min, white/light background | WB marketplace (via pixyn, secondary) | The result is judged by the marketplace, not by taste | MEASURED-DOC (pixyn, secondary only) |
| P15 | **Scroll vs. erase conflict resolution** | none documented | — | **UNVERIFIED** (no loaded source describes gesture handling). Rules below that touch this are INFERENCE. |
| P16 | **Subject selection before cutout** as an explicit tap/box step | none documented. Photoroom cutout is *automatic*; WB highlights an auto-detected region. "Assisted mode: tap an area" exists only in a search summary | — | **UNVERIFIED** as an explicit pre-step. Observed convention is the opposite: auto-detect, then correct. |

## 2. The Wildberries flow, step by step

Source for every step: `selsup.ru/blog/fotostudiya-wildberries-nejroset/` (secondary, dated 2026-09-30, states it follows the WB
instruction updated 2026-09-17). Primary WB page did not render. All steps are therefore **MEASURED-DOC (secondary)** unless marked.

**Entry**
1. Seller cabinet → «Товары и цены → Карточка товара».
2. Pick a **published** product (pencil or row → «Редактировать карточку» in the right panel). Not available while creating a new card.
3. Tap «Фотостудия» inside the card. Second entry: «Открыть редактор» → «Фотостудия» in the photo-editor side panel.
4. Access requires the «Джем» subscription (selsup + search summary; exact eligibility rules UNVERIFIED — decide at the WB instruction URL above).

**Choose scenario** (screen 1)
5. Two options: put clothing on a virtual model (clothes only, no person/mannequin; categories «Одежда», «Спортивная одежда») or replace model/background (a photo with exactly one real person with a distinguishable face; all categories).

**Scenario A — clothes on a model**
6. Upload JPG/PNG ≤ 10 MB, 700×900–2800×3600 px, front view, whole item, neutral light background.
7. Left panel: pick a model; «Все модели» → filters (sex, age, build, appearance type) → «Применить».
8. «Все фоны» → city / flat / nature / studio / sport → «Применить». Skipped ⇒ background stays as in source.
9. «Сгенерировать». Status «Генерируем изображение»; user may leave.
10. Result variants: download or add to the card. Regenerate ⇒ a different variant, same settings.

**Scenario B — replace model/background**
11. «Загрузить» the photo. Unsuitable (no person, several people, face merges with background) ⇒ service asks for another image.
12. Right side: choose «Модель», «Фон», or «Модель и фон». **Green highlight shows the area that will change.** Option «Не выбрано» in a highlighted area ⇒ random substitution.
13. Left panel: set model / background as in 7–8; «Сгенерировать».
14. Two variants appear; **tap a result to open before/after**. Regenerate if unsuitable.

**Money / quota**
15. Monthly limits by plan: 50 / 100 (standard / advanced), 65 / 130 (as tariff-constructor option). Resets on the subscription renewal date.
16. Extra generations 50 ₽ each, packs of 20 / 50 / 100 / 200; «Купить ещё» button in both scenarios; extras are spent after the monthly limit; unused paid remainder carries over.
17. A successfully produced image consumes a generation **even if disliked**; a technical error does not.

**UNVERIFIED (not established by anything I loaded)**
- Exact on-screen position of the remaining-attempts counter — decide at `seller.wildberries.ru/instructions/ru/ru/material/photo-generation-using-a-neural-network-rb`.
- Whether a *cutout* / erase brush exists anywhere in WB Фотостудия — nothing found; decide at the same URL.
- Whether a mobile (app) version of the flow exists — not found; same URL.
- Image captions from the secondary article were read as text; **I did not view the screenshots**.

## 3. Ten layout rules (for a Kotlin/Compose developer)

Each rule is self-contained. Rules from a `MEASURED-DOC` pattern are conventions; those marked INFERENCE are mine and must not be
presented as "how everyone does it".

1. **Pick the job first (P2, P13).** The first screen after import asks what the photo contains/what the user wants
   (e.g. "clean cutout on white", "place in a scene"), as large selectable cards, each with one good and one bad example thumbnail.
   Validate the image against the chosen job *before* any paid step; on mismatch show "choose another photo", not an error dialog.
2. **One primary button per screen (P1).** Exactly one filled, full-width button pinned to the bottom (`Button` in a bottom bar with
   `navigationBarsPadding()`); everything else is `TextButton`/`OutlinedButton`. The label is the verb of the result: «Generate», not «Next».
3. **Cutout is automatic; the user corrects, not selects (P4, P16).** Run the cutout on import and show the result immediately.
   Provide Erase and Restore as a two-state toggle in a toolbar, with brush-size slider and undo. Do not add a "select the subject" step.
4. **Highlight what will change before spending (P3).** Before generation, draw the affected region as a translucent overlay
   (`drawWithContent`, 35–40% alpha accent) over the preview, with a three-way segmented control «Subject / Background / Both».
   An unset choice must be explicit ("keep as is"), never silently random (WB's «Не выбрано» is the cautionary case).
5. **Editor is its own full-screen destination (P5).** Tapping a photo in the gallery opens a dedicated editor route that owns
   gestures. No brush gestures on a scrolling gallery row.
6. **Scroll and erase never share a pointer (P15 — INFERENCE; no source documents this).** In editor mode the canvas has two explicit
   modes: *Move* (pinch-zoom + two-finger pan, one finger pans only when zoomed) and *Erase/Restore* (one finger paints, two fingers
   still pinch/pan). The mode is visible in the toolbar. The editor body is never inside a `verticalScroll`; any panel below it
   (scenes, settings) is a separate bottom sheet. Consume pointer events only while a brush mode is active.
7. **Scenes are a catalogue, text is the escape hatch (P6, P7).** A horizontal `LazyRow` of preset scene tiles (grouped chips:
   studio / home / nature / city), plus an optional "describe it" field. If nothing is chosen, the original background is kept.
8. **Show the price and remaining quota on the button's own screen (P11).** The generate button reads e.g. «Generate · 1 credit»;
   remaining credits sit in the top bar of the same screen. Buying more is a text button on that screen, not a settings detour.
   Say once, near the button, that a finished generation is charged and a failed one is not (WB's stated rule) — only if the app's
   own billing actually does that.
9. **Variants, then before/after on tap (P8, P9, P12).** Show 2–4 results in a pager/row; tapping one opens a full-screen
   comparison with a draggable divider over the original (`Modifier.pointerInput` + `clipRect` on the top layer). Generation is a
   background job with a visible status that survives leaving the screen (P10).
10. **Result screen offers the use, not more editing (P12, P14).** Primary: «Save / Share» (and, where relevant, "export for the
    marketplace" at its aspect, e.g. 3:4, ≥ 900×1200). Secondary: «Regenerate» (same settings) and «Edit cutout». No promotional or
    unrelated destinations on this screen.

## 4. Claim ledger

| CLAIM | LABEL | EVIDENCE |
|---|---|---|
| WB «Фотостудия» exists inside the seller cabinet card editor, for published cards only | MEASURED-DOC (secondary) | https://selsup.ru/blog/fotostudiya-wildberries-nejroset/ |
| WB has two scenarios: clothes on virtual model; replace model/background | MEASURED-DOC (secondary) | same |
| WB scenario 2 highlights the changing area in green; «Не выбрано» ⇒ random substitution | MEASURED-DOC (secondary) | same |
| WB: tapping a scenario-2 result opens before/after | MEASURED-DOC (secondary) | same |
| WB: generation continues if the user leaves the page; results downloadable or addable to the card; regenerate = another variant, same settings | MEASURED-DOC (secondary) | same |
| WB limits 50/100/65/130; extra 50 ₽, packs 20/50/100/200; «Купить ещё»; completed result is charged, technical error is not | MEASURED-DOC (secondary) | same |
| WB input: JPG/PNG ≤ 10 MB, 700×900–2800×3600 px | MEASURED-DOC (secondary) | same |
| The same facts as stated in WB's own instruction | UNVERIFIED | could not load: https://seller.wildberries.ru/instructions/ru/ru/material/photo-generation-using-a-neural-network-rb (nav list only) |
| Access requires «Джем» subscription | SEARCH-SUMMARY + selsup text | same selsup URL; official page could not be loaded |
| WB card spec 3:4, min 900×1200, JPG ≤ 10 MB, white/light background; moderation compares with the real item | MEASURED-DOC (secondary, vendor opinion on moderation) | https://pixyn.ai/blog/ai-foto-dlya-wildberries-poshagovo-2026 ; official WB spec UNVERIFIED |
| Pixyn shows price before a run; 4 variants | MEASURED-DOC (vendor blog) | same pixyn URL |
| Photoroom: cutout runs automatically, then erase/restore brushes | MEASURED-DOC (third-party tutorial) | https://www.fahimai.com/how-to-use-photoroom |
| Photoroom: each AI tool has its own editor; scene = preset or short prompt; generate 4 versions and compare; virtual model flow | MEASURED-DOC (third-party tutorial) | same |
| Photoroom Edit Cutout: assisted tap mode, never creates new pixels, Retouch generates content | SEARCH-SUMMARY only | could not load body: https://help.photoroom.com/en/articles/13743688-how-to-use-edit-cutout |
| Photoroom's own help flow for AI Background | UNVERIFIED | could not load body: https://help.photoroom.com/en/articles/13003543-upload-an-image-and-apply-an-ai-background-web-app |
| Pebblely: upload → 40+ themes or custom prompt → size → Create; 1 credit/image (2 high-res); 40 free/month | SEARCH-SUMMARY only | search answer citing selleraihub.com / wizcommerce.com; could not load content: https://www.pebblely.com/ , https://pebblely.com/faq/ |
| Claid.ai flow | UNVERIFIED | could not load flow content: https://claid.ai/ |
| Canva Background Remover / Magic Media flow | UNVERIFIED | could not load: https://www.canva.com/features/background-remover/ (403) |
| Ozon AI photo editor (remove background, extend to 3:4, enhance; free) | SEARCH-SUMMARY only | could not load: https://seller-edu.ozon.ru/work-with-goods/zagruzka-tovarov/zagruzka-media/redaktor-foto-ai (redirect loop) |
| Any product documents how it resolves scroll-vs-erase gestures | UNVERIFIED | no loaded source covers it; rule 6 is INFERENCE |
| Any product has an explicit "select subject" step before cutout | UNVERIFIED | none found; observed convention is auto-detect then correct |
| I viewed WB/Photoroom screenshots | **FALSE** — only captions/text were read | — |

## 5. What would settle the gaps
- Open the WB instruction in a real browser (JS-rendered): URL in §2. Settles auto-detect vs brush, counter placement, mobile.
- Install Photoroom and Pebblely and walk one photo each on the attached phone; record screenshots. Settles P4/P15/P16 as *observed*.
