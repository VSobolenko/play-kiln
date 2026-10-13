# I — Slide kit (infographic series with seller-controlled text)

**Why it exists (ON RECORD, evidence §Market evidence).** WB's and Ozon's built-in editors work one image
per run, platform-written slide text and no series in one style; WildCard draws one slide per run with a
regenerated background ("the same background across a series will not exist") and draws prohibited text if
the seller types it. The slide kit is the app's answer: a **series** of slides in **one saved style**, text
chosen by the **seller**, **typeset on the device**, **checked against the marketplace's prohibited list
before it is rendered**.

## What a slide is

`Slide(id, itemGroupId, templateId, role, blocks[], lookId)` rendered at the target canvas
(default 900×1200). A slide is composed from layers: background (from the Look), the item (Original-mode
composite), shapes (plates, lines, arrows), icons (from the one licensed set) and **text blocks**.

`TextBlock(id, slot, text, styleToken)`; `slot ∈ TITLE, SPEC_1..SPEC_3, BADGE, CAPTION`. Text is set with
real fonts bundled with the app (an open-licence family covering Latin and Cyrillic, recorded in
`docs/font-provenance.md`; **no text is ever drawn by a generative model** — design D13).

## Templates (`assets/slide-templates.json`, DECISION: 8 at M2)

| Id | Layout | Slots | For |
|---|---|---|---|
| `hero` | item large centre, title top | TITLE, BADGE | main-adjacent first slide |
| `spec3` | item left, three spec plates right | TITLE, SPEC_1..3 | key properties |
| `size` | item with dimension lines and arrows | TITLE, SPEC_1..2 (dimensions) | clothing/furniture sizing |
| `detail` | zoomed crop of the item + caption | CAPTION | material/texture close-up |
| `kit` | grid of included parts (items from the group) | TITLE, CAPTION | what is in the box |
| `use` | item on a generated or procedural scene | CAPTION | in-use context |
| `variants` | colour/size chips row | TITLE, SPEC_1 | variants of one model |
| `clean` | item only, nothing else | — | MAIN-safe slide for `white-255` marketplaces |

Slide order for a pack follows the preset: for WB the app suggests `clean` first, accessories second, then
`spec3`, `size`, `detail`, `use`, `variants` (the order is WB's three documented sequence rules plus the
cited blog's working scheme, labelled "our suggestion" in the UI).

## Sizes (legibility on the phone preview) — derived values, not marketplace rules

For a 900×1200 canvas: title ≥ 60 px, spec/caption ≥ 40 px, safe margin ≥ 60 px, ≤ 3 captions per slide.
Source: the cited WB-infographic blog derived these from the 246×328 catalogue preview (a 60 px letter
becomes 16 px). They are DECISION defaults, labelled "derived", and the editor shows the slide at 246 px
width as a live check (S-10).

## Prohibited text (L11) — deterministic, per language

For each marketplace `text.prohibited` category the app has a **matcher** per UI/text language
(`assets/prohibited-text.json`), built from the marketplace's own listed phrases and common forms:

| Category | Examples that must match (WB, from help-quoting sources) |
|---|---|
| `price_discount` | digits with `₽`/`руб`/`$`/`€`, `%` with a minus or "скидка/sale/off", "распродажа", "акция", "дёшево" |
| `contacts_links` | phone-number patterns, e-mail, `http(s)://`, `www.`, `@handle`, messenger names with an id |
| `qr_code` | detected by the barcode detector on the rendered slide, not a text match |
| `evaluative_claim` | "хит", "топ", "лучший", "лидер продаж", "best", "#1", "bestseller" |
| `review_reward` | "отзыв за", "кешбэк за", "review for", "cashback for" |
| `sold_count` | "продано", "sold" next to a number |
| `call_to_action` | "купи", "купите", "звоните", "закажи", "buy now", "call", "order" |
| `delivery_returns` | "доставка", "возврат", "free shipping", "returns" |

Matching is case-insensitive, Unicode-normalised, ignores diacritics and treats look-alike Cyrillic/Latin
letters as equal (a seller may type `Хит` with Latin `X`). A match marks the text block red with the
category; the slide **does not render** until edited, or until the seller chooses "Render anyway" which is
available only for `WARNING`-severity marketplaces and is recorded in the manifest. Marketplaces whose
`text.prohibited` is empty (not stated) show no matcher findings, and the UI says "this marketplace's
rules for slide text were not read".

Honest limit: the matcher is a **list**, not a language model. It catches the categories the marketplaces
publish; it cannot judge every misleading claim. The UI says "no listed phrases found", never "text is
compliant". Phrase lists for de, fr, es and pt-BR are authored from the marketplace's published categories
translated by the builder and are **UNVERIFIED by a native speaker** until a native review; ru and en are
the seed lists grounded in the cited WB help quotations.

## Contrast (L12)

Text colour against its plate or the background behind the block: WCAG ratio, with WB's thresholds
(≥ 4.5 small, ≥ 3 large/bold) as the default for every marketplace. `AutoTextColour` picks black or white
by the higher ratio; the avoid-pairs table of WB (green-red, …) produces an ADVICE finding.

## Series consistency

A `Look.slideStyleId` binds colours, fonts, plate shapes and spacing; changing the style updates every
slide of the project. Backgrounds across the series are the **same asset** (a generated scene is reused,
never regenerated per slide). That is the explicit wedge over WildCard's per-slide regeneration.

## Credits

Slides are local and free (O-17). A *generated* scene used by slides costs O-07 once per Look.

## Tests (`SlideKitTest`)

Prohibited-text fixtures per category in ru and en (including look-alike letters); contrast fixtures;
series background identity (same asset hash across slides); the 246 px preview legibility check
(rendered title height ≥ 14 px at 246 px width); render determinism (same inputs → same bytes).
