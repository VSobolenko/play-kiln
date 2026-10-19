# Plinth AI — product rewrite design (owner-approved 2026-10-17)

**Status:** approved to implement. Owner words: title **Plinth AI: Product Photos** / **Plinth AI: фото товаров**; remarks correctly captured; full rebuild not bugfix; no inventing — use patterns competitors already ship; under-the-hood system prompts so the tool does the magic.

**Authority:** owner review 2026-10-17 + confirmation same day. Defect ledger: `owner-review-rewrite-w70.md` (in flight). Competitor UX: `competitor-teardown.md` (ON RECORD) + `competitor-generative-ux-w71.md` (in flight).

---

## 1. Product promise (one sentence)

A marketplace seller picks a product photo, picks one named AI tool, taps Generate, and gets a real generative result that matches the tool — with cost on the button and almost no typing.

## 2. Non-negotiable principles

1. **Result over explanation.** Users do not care how it works. No generation IDs, no pipeline/job statuses, no walls of credit math before the tool.
2. **Tool magic.** Each tool carries a **server-side system prompt** (and defaults) that already imply the job. User instruction is optional short help, never required homework.
3. **Tool means tool.** Relight changes lighting on *this* photo. Recolour changes colour on *this* photo. Scene/background is **generative**, not letterbox/gradient padding. Wrong-image results (car / girl+dog) are P0 defects, not quirks.
4. **Happy path:** photo → tool → Generate (cost on button) → wait spinner → before/after → share/save.
5. **Do not invent chrome.** Navigation, editor icons (undo/redo/zoom), Profile for wallet/history, tool grid as imagery — follow Photoroom / Pixelcut / Pebblely patterns already measured in `competitor-teardown.md`.
6. **Layout:** stop inventing dense text UIs. Media-first, short labels, standard editor chrome.

## 3. Information architecture (target)

| Tab / entry | Role |
|---|---|
| **Home** | Visual tool gallery + Start from photo / Create |
| **Studio / AI Tools** | Same tools, requires a selected photo; one tool screen at a time |
| **Library / Projects** | User's results and drafts |
| **Profile** (replaces Pack) | Credits, wallet top-up, generation history, settings entry |
| **Center +** | Start Create → real AI product card / tool picker — **not** the fake pad-sides scene path |

Batch stays reachable from Home or Profile if still needed; it is not the identity of a main tab if it confuses the job.

## 4. Tool contract (client + server)

For every `StudioOperationId` that claims AI:

| Must | Must not |
|---|---|
| Bind the **currently selected product image** into the job payload | Fall back silently to an unrelated library/demo image |
| Apply a **per-tool system prompt** that forces the tool semantics | Treat user free-text as the only prompt |
| Return an image that is a edit/generation of that product | Return an unrelated generation |
| Show cost on Generate; confirm only if balance insufficient | Full-screen credit essay before every tool |
| Show a single waiting state | Enumerate developer job phases |

**AutoCard / “AI scene”:** must be generative composition (or honest rename if only local compose). Padding sides with gradient is **forbidden** as an “AI scene”.

**Cutout:** after cut, open a **standard mask editor** — icon undo / redo / zoom / brush — not invented text buttons.

**Viewer:** pinch/pan real zoom on the image, not “zoom center only”.

## 5. Naming (locked)

| Locale | Title |
|---|---|
| en | Plinth AI: Product Photos |
| ru | Plinth AI: фото товаров |
| other locales | Keep carrier `Plinth AI:` + existing job noun translations (fit ≤30 chars with slack) |

`applicationId` / slug stay `plinthproductphotos` / `plinth-product-photos` (permanent after any Play identity).

## 6. Implementation waves (Copilot/Codex only)

| Wave | Goal | Notes |
|---|---|---|
| **W72a** | Rename surfaces (naming.json, strings, listing, About) | No Gradle lock across writers |
| **W73** | P0 image binding + per-tool system prompts (server + client payload) | Prove relight/recolour use selected photo |
| **W74** | AutoCard / Create / center+: real generative path; kill fake pad-sides as AI | |
| **W75** | Studio shell UX: one tool, cost on Generate, kill IDs/statuses, optional short instruction | |
| **W76** | Cutout editor chrome + image viewer | Icons from licence-cleared set |
| **W77** | Pack → Profile (credits, wallet, history) | |
| **W78** | Copy/layout pass: remove jargon walls; media-first | |
| **W79** | Device pass on A40 against remote debug | Copilot drives phone |

W70/W71 docs land first and can refine file lists; waves must not invent new tools beyond owner asks.

## 7. Done when

Owner can: open app → see Plinth AI name → pick phone photo → Relight / Recolour / Scene / Cutout → get a result that matches the tool on **that** photo → share/save — without credit walls, IDs, or fake gradient AI. Device evidence recorded.

---

**Claim ledger**

| Claim | Label | Evidence |
|---|---|---|
| Owner approved title Plinth AI + rewrite principles + magic prompts | ON RECORD | Owner message 2026-10-17 |
| Competitor IA patterns (tool grid, Profile, cost before generate) | ON RECORD | `docs/redesign/competitor-teardown.md` §1.1–1.3 |
| Fake AI scene / wrong generations / Pack / IDs are in owner defect list | ON RECORD | Owner review 2026-10-17 |
