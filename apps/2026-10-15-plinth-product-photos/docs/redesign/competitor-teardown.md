# Competitor teardown — AI product-photo & merchant-image apps

**Slot:** `apps/2026-10-15-plinth-product-photos` ("Plinth: Product Photos")
**Purpose:** the owner rejected the current build as «какой-то блокнот… с кнопками отправить в ИИ. Это не современное приложение», and asked for the competitor class of app. This document measures what that class actually does, names the patterns they converge on, and turns them into a shell proposal for Plinth that a Kotlin/Compose engineer can implement.
**Written:** 2026-10-16. **Author:** design research worker. **No application code was modified by this document.**

---

## 0. Method, and the honest limits

**How the sources were gathered.** Web search + fetch only, in this session. No app was installed and no screenshot was captured by this worker, so **nothing in this document is a claim about pixels this worker saw**. Where a store listing, a vendor help page or a dated review describes a control or a layout, the claim is `ON RECORD` against that source. Where nothing verifiable was found, the cell says `NOT VERIFIED`.

**The three limits, stated rather than implied:**

1. **Retailer help pages describe *screens and flows*, not *chrome*.** Photoroom's help centre names the screens "Home", "AI Tools", "Batch", "Content" but does not print the words "bottom navigation bar". The tab set is therefore `ON RECORD` (vendor screens + a search synthesis) while the exact control type is a strong inference, not a read screenshot.
2. **Several targets are web-only.** Flair.ai, Booth.ai, Claid.ai and SellerPic are browser tools with no verifiable native Android build. They are still useful — they show the product mechanics (scene presets, credit cost per operation, before/after) — but their *shell* cannot inform a Compose navigation bar, and the cells say so.
3. **Visual language (colours, radii, type scale) was not observable this session.** The per-app "visual language" rows are `NOT VERIFIED` for most apps; the *pattern-level* visual guidance in §2.7 is instead grounded in design systems that are read from committed documents (see the ledger). Inventing a screenshot is forbidden by this task and by `.cursor/rules/grounded-claims.mdc`.

**Where the app-under-redesign is cited**, the reference is a real `path:line` in this repository, read this session, and is `MEASURED`.

---

## 1. Per-app teardown

Legend — **MEASURED** = read from our repo this session · **ON RECORD** = read from the named external source · **UNVERIFIED** = could not be checked · **NOT VERIFIED** = no source exists.

---

### 1.1 Photoroom — the category leader

Sources read: Photoroom Help Center (GB) *Get started with Photoroom* (URL below), Photoroom Help Center *The AI Tools tab*, the Botsi "best free product-photo apps" teardown (2026), App Store listing for Photoroom (read 2026-10-16), and a web-search synthesis of Photoroom's own help articles on 2026-10-16.

| # | Dimension | Finding | Label / source |
|---|---|---|---|
| 1 | **Navigation shell** | Top-level screens are **Home**, **AI Tools**, **Content** and **Batch**; Home's answer says it is the place to *"Access AI Tools, Batch, and Content"*. A search synthesis of the help set states the app carries a **bottom navigation bar with Home, Content and AI Tools**; the help page itself names the screens but not the control. Settings/account/plan live in a **header area** with a **Get Pro** button, not as a tab. | `ON RECORD` — help.photoroom.com *Get started*; `ON RECORD (secondary)` — search synthesis 2026-10-16 |
| 2 | **First screen (hero)** | The Home screen drops the user onto a **search bar + a grid of tool cards + a `Start from Photo` CTA**, plus template imagery and a recent-designs area. The first thing on screen is *imagery*, not prose: it is a catalogue of what the app can do. | `ON RECORD` — Botsi teardown 2026: *"They drop you straight onto the home screen with a search bar, a tool grid, and a 'Start from Photo' CTA."* |
| 3 | **The user's own photo** | Import a photo → background removal runs → the item is shown on a template/background. Content holds saved designs; the AI Tools editor shows **multiple generated images in a preview grid**. | `ON RECORD` — help.photoroom.com *AI Tools*: *"Preview for generated images"*, *"Context menu for each generated image, with options for generating variants"* |
| 4 | **How AI-ness is communicated** | 1. **Named tools, not sliders** — the AI Tools hub lists *Recolor*, *Product Beautifier*, *Virtual Model*, *Product Staging*, *Replace Background*. 2. **Preview-first copy** — the teardown's own recommendation for newcomers is *"Head over to 'Start from Photo,' upload a picture of your product and **watch the background vanish**"*, i.e. the value lands in seconds and is shown, not described. 3. **Variants** are generated and offered in a context menu. | `ON RECORD` — help.photoroom.com *AI Tools*; Botsi teardown 2026 |
| 5 | **Value and cost** | A **"space sheet"** (account panel) carries **usage meters together in one place**: *AI images %*, *AI videos %*, **Exports 0/250**, with a **reset date**, and a **Get Pro** button in the corner. Separately, *View AI credits* appears in the Content screen's option menu. Cost is denominated in **exports** and **AI images**, not in abstract coins spent per button. | `ON RECORD` — help.photoroom.com search synthesis 2026-10-16 (space sheet / usage meters) |
| 6 | **Result screen** | Two result shapes: (a) the **main editor** with layers, where the composed image is the whole screen and tools are secondary; (b) the **AI tool editor**, which shows generated previews with per-image context menus (variants) and a download/share path. Export offers **platform resize presets**. | `ON RECORD` — help.photoroom.com *AI Tools* and export-resize articles |
| 7 | **Visual language** | Not observable in this session. | `NOT VERIFIED` |

**What Plinth should take from Photoroom:** the tool grid is a *gallery of images*, and the app's own name for its cost unit is a thing the user cares about (exports/images), not an internal token spent per button.

---

### 1.2 Pixelcut

Sources read: Pixelcut Help Center *Pixelcut Credits* and *Credit FAQ* (URLs below), the Lazyweb Pixelcut sign-in flow teardown (2026-10-16), the Apple App Store listing for Pixelcut (read 2026-10-16), and a search synthesis of Pixelcut blog/help pages.

| # | Dimension | Finding | Label / source |
|---|---|---|---|
| 1 | **Navigation shell** | **Bottom navigation bar** is present. Reported tab set: **Home, Ideas, Projects, Profile**; the library screen also carries a **settings icon** and a **Get Pro** button in the top area. A "Projects/workspace home" is reached by tapping the project name in the header. | `ON RECORD (secondary)` — Lazyweb teardown 2026-10-16: *"plus bottom navigation, projects list"*; search synthesis 2026-10-16 states tab names Home/Ideas/Projects/Profile. Tab *labels* are secondary-sourced, not vendor-documented. |
| 2 | **First screen (hero)** | The home dashboard is a **horizontally scrolling list of large, visually rich cards representing tools/features** — the imagery *is* the menu. The help centre describes the entry point as *"generate an image or upload an image to edit"*. | `ON RECORD` — screensdesign 2026-10-16 (dashboard of large visual tool cards); help.pixelcut.app |
| 3 | **The user's own photo** | Upload → edit; the editor is **non-destructive** (original recoverable) and carries toggles/sliders for effects and shadows. Edits are layered on the user's photo. | `ON RECORD` — help.pixelcut.app product-image-editor / shadow articles |
| 4 | **How AI-ness is communicated** | 1. **Showcase** — a feature that turns a plain shot into *photorealistic lifestyle scenes*, i.e. the AI's output is the pitch. 2. **Contextual suggested prompts** alongside upload. 3. **Per-model credit cost is shown in the model picker *before* the user commits** — the cost is attached to the choice, not discovered after. 4. **AI Try-On** for apparel. | `ON RECORD` — help.pixelcut.app *Pixelcut Credits*: *"the cost in credits for each model is displayed in the model picker, so you can see it before you generate"* |
| 5 | **Value and cost** | **One balance, top-up automatically via subscription**; tiers map to credit volumes, replenishing per billing cycle. Balance is checkable from inside the app, and **extra credits can be purchased on demand** (help centre names a 2000-credit purchase reachable from the **Settings** page). Apple listing: 3-day free trial then a yearly subscription; limited free trial for new users. | `ON RECORD` — help.pixelcut.app *Pixelcut Credits* / *Credit FAQ*; App Store listing read 2026-10-16 |
| 6 | **Result screen** | Results can be assembled into **collages/templates**, then saved/shared; batch edit exists. | `ON RECORD` — Apple listing and help centre collage/batch articles |
| 7 | **Visual language** | Home is *"covered in a horizontally scrolling list of large, visually rich cards"*. Colours/radii not observable. | `ON RECORD` (card density) — screensdesign 2026-10-16; rest `NOT VERIFIED` |

**Two things Plinth should take:** (a) the **cost is visible at the moment of choosing** a model — this is the opposite of Plinth's *"Credits per result: %1$d"* list scattered under a wall of tool cards; (b) the dashboard is a **horizontal strip of large visual cards**.

---

### 1.3 Pebblely

Sources read: Pebblely Help Center *Getting started*, *Manage billing*, *Viewing and downloading images*, *AI Product Photos* (URLs below), and a search synthesis of Pebblely blog/help pages.

| # | Dimension | Finding | Label / source |
|---|---|---|---|
| 1 | **Navigation shell** | Screens named by the vendor: **Home**, **Recent** (with *"View all"*), **Templates** / **Custom** (two tabs *inside* a product's generate screen), **Profile** (account/billing). No bottom bar is documented; the tabs documented are *within* the generation flow. | `ON RECORD` — help.pebblely.com *Getting started*: *"Two tabs – Templates and Custom"*; *Manage billing*: *"go to the Profile tab"*; *Viewing images*: *"access the Recent tab"* |
| 2 | **First screen (hero)** | Home carries **a set of sample products** to demonstrate the product, an **Add product** affordance, and a **Recent** section of thumbnails with "View all". The demo imagery precedes any explanation. | `ON RECORD` — help.pebblely.com *Getting started*: *"the Home screen supports a set of sample products"* |
| 3 | **The user's own photo** | Add product → upload → **crop to the product** (zoom + brush refine) → name it; the AI then *"extracts all visible text, logo or label"*. Uploads can be **bulk-selected** (multiple products at once). This is the most *photo-craft*-forward flow of the set: the app makes the user prepare the cutout well. | `ON RECORD` — help.pebblely.com *Getting started* |
| 4 | **How AI-ness is communicated** | 1. **Three steps**: upload → describe → **Create**. 2. **Named scenes, not sliders** — *"Templates"* is a **set of over 40 preset backgrounds** (the help page names Cafe, Studio, Kitchen, Water, Nature, Bathroom). 3. **`Custom`** is the free-prompt alternative. 4. **Generate 1, 2 or 4 at a time**; results in ~10 s; **no background removal needed**. | `ON RECORD` — help.pebblely.com *AI Product Photos* / *Getting started* |
| 5 | **Value and cost** | Subscription tiers *"up to 200 or 500 images per month"*; billing is managed under **Profile → Manage billing**; blog material states subscription credits **carry over**. A persistent, always-visible balance widget is not documented. | `ON RECORD` — help.pebblely.com *Manage billing*; `UNVERIFIED` — the on-screen placement of the balance |
| 6 | **Result screen** | **Recent Images** screen with a **filter by resolution** (All / HD / Standard), search, click a result to open it, then **download**; **bulk download via checkboxes**; **edit an existing result by describing the change**. | `ON RECORD` — help.pebblely.com *Viewing and downloading images* |
| 7 | **Visual language** | Named scene templates presented as visual choices. Colours/radii `NOT VERIFIED`. | `ON RECORD` (named scenes) — help.pebblely.com |

**What Plinth should take:** the **named-scene card grid** (Plinth already has a Look/scene concept) and the **`Templates` vs `Custom`** split — presets for speed, one free-text box for control.

---

### 1.4 Flair.ai

Sources read: Flair.ai product pages (flair.ai product-photography and fashion-photography pages) and the Shotkit hand-on review of Flair.ai (2026). **Web-based; no native Android app was found.**

| # | Dimension | Finding | Label / source |
|---|---|---|---|
| 1 | **Navigation shell** | Browser application. Documented surfaces: the **Drag & Drop canvas**, a **Custom Models** tab, an **Image Editor** tool suite. Not a mobile shell — cannot inform a Compose navigation bar. | `ON RECORD` — flair.ai product pages; Shotkit review 2026 |
| 2 | **First screen (hero)** | Upload/select your product (+ brand assets), then choose a **template from a gallery** (*"more than a dozen options"*) or write a custom prompt. The gallery is the hero. | `ON RECORD` — Shotkit review 2026 |
| 3 | **The user's own photo** | The product is placed on a **canvas** with props and a background; the product is the anchor and everything else is generated around it. Layers are editable. | `ON RECORD` — flair.ai; Shotkit review |
| 4 | **How AI-ness is communicated** | AI generates the scene *around* a locked product, and the **Image Editor** exposes post-generation AI moves as named verbs: **Magic Erase**, **variations**, **extend**, **upscale**, **regenerate product / human / clothing**, **change pose, angle, colour**. Generation is **bulk**. | `ON RECORD` — flair.ai product pages; Shotkit review |
| 5 | **Value and cost** | Paid plans. A credits-balance UI was not verifiable. | `UNVERIFIED` — no reachable pricing/credits page in this session |
| 6 | **Result screen** | Result is the canvas image; the Image Editor's named verbs act on it; bulk generation produces multiple outputs. | `ON RECORD` — Shotkit review |
| 7 | **Visual language** | Canvas/prop aesthetic. Colours/radii `NOT VERIFIED`. | `NOT VERIFIED` |

**What Plinth should take:** the **"lock the product, generate everything around it"** promise is exactly Plinth's own hero line — *"Keep the item's pixels. Style everything around it."* Flair proves the promise is the category's, and that its UI is a *scene gallery + one prompt box*.

---

### 1.5 Booth.ai

Sources read: Booth.ai product pages and the GeekDashboard review (2026). **Web app + a WhatsApp service.**

| # | Dimension | Finding | Label / source |
|---|---|---|---|
| 1 | **Navigation shell** | Web app with a **left-hand menu**: **Workflows, Products, AI Scene Generator, Gallery**. WhatsApp channel: send a photo in a chat. | `ON RECORD` — geekdashboard.com review 2026 |
| 2 | **First screen (hero)** | Left menu + a right-hand dashboard: the interface opens with **workflows presented per use case**, not a blank canvas. | `ON RECORD` — geekdashboard review |
| 3 | **The user's own photo** | Upload a **reference image of the product**, then describe the scene in text. | `ON RECORD` — geekdashboard review |
| 4 | **How AI-ness is communicated** | Workflow-per-use-case; **text-described scene → multiple generated images**; in WhatsApp the flow is photo → description → **HD result in ~30 s**. | `ON RECORD` — geekdashboard review |
| 5 | **Value and cost** | ~**$0.50 per photo**; **10 free photos** on signup. Credits UI `UNVERIFIED`. | `ON RECORD` — geekdashboard review |
| 6 | **Result screen** | Review/refine and download **4K** output. | `ON RECORD` — geekdashboard review |
| 7 | **Visual language** | `NOT VERIFIED` | `NOT VERIFIED` |

**Relevance:** Booth.ai is the clearest evidence that in this category the *unit of value is the finished photo*, and the price is stated per photo.

---

### 1.6 Claid.ai

Sources read: Claid.ai site/pricing pages (credit table). **Web/API product.**

| # | Dimension | Finding | Label / source |
|---|---|---|---|
| 1 | **Navigation shell** | Web app + REST API. Not a mobile shell. | `ON RECORD` — claid.ai |
| 2 | **First screen (hero)** | The public entry is a **tool/capability list with per-operation credit caps shown alongside**. | `ON RECORD` — claid.ai pricing |
| 3 | **The user's own photo** | An API request: send an image + a list of requested operations; receive processed output. | `ON RECORD` — claid.ai |
| 4 | **How AI-ness is communicated** | Capability names, not controls: *Upscale, Remove background, AI Photoshoot, AI Fashion, AI Video, AI Edit, Outpaint, AI Shadows*. | `ON RECORD` — claid.ai pricing |
| 5 | **Value and cost** | **Every operation is priced in credits, in one published table**: Remove background **1**, Upscale **2–8**, Outpaint **2**, AI Shadows **2**, AI Photoshoot **4**, AI Fashion **4**, AI Edit **4**, AI Video **60**. Monthly credit allowance; **50 free API credits** on trial. | `ON RECORD` — claid.ai pricing |
| 6 | **Result screen** | Output image/URL and formats; controlled by API params. | `ON RECORD` — claid.ai |
| 7 | **Visual language** | `NOT VERIFIED` | `NOT VERIFIED` |

**Relevance — and the sharpest lesson for Plinth.** Claid prices **every operation in one table in one unit** (credits), and the range is *2–60 credits across the whole catalogue*. Plinth shows *"Credits per result: 2"* under a tool, *"2K: N credits · 4K: M credits"* under another, and *"Local layout · AI scene priced separately"* under a third — a scattered price list — while a **balance is visible on exactly one screen in the whole app** (`MEASURED`, §3.2 below). Claid's table is the model for *"one balance, a small number of named price tiers"*.

---

### 1.7 Canva mobile — how a large creative tool organises its shell

Sources read: Canva Help Center GB *Understanding the Canva mobile app tabs* and the Canva Help Center GB *Canva mobile app* overview (URLs below).

| # | Dimension | Finding | Label / source |
|---|---|---|---|
| 1 | **Navigation shell** | **Five bottom tabs**: **Canva AI**, **Create**, **Templates**, **Your Designs**, **More**. Canva **removed the top-corner Profile** and moved account/settings into **More**. **Create replaced Home** and lands on **Quick Create** shortcuts rather than on recent designs. **Your Designs replaced Projects.** A **dropdown switcher sits next to the page title**. | `ON RECORD` — canva.com/help *Understanding the Canva mobile app tabs* |
| 2 | **First screen (hero)** | **Create** = *"quickly get started with Quick Create shortcuts"*. The AI surface is its own tab. | `ON RECORD` — Canva help centre |
| 3 | **The user's own photo** | Designs and uploads live in **Your Designs**; the canvas is the editor. | `ON RECORD` — Canva help centre |
| 4 | **How AI-ness is communicated** | **A whole tab is named for it and is the first tab** (*Canva AI*). AI is a *destination*, not a button inside another screen. | `ON RECORD` — Canva help centre |
| 5 | **Value and cost** | **Print Cart was moved into More** during the same restructure — i.e. a commercial surface was demoted into the overflow rather than given a tab. | `ON RECORD` — Canva help centre |
| 6 | **Result screen** | The editor; layouts/templates feed into it. | `ON RECORD` — Canva help centre |
| 7 | **Visual language** | Five equal-weight bottom tabs; dropdown switcher beside titles; consistent naming ("Your Designs", not "Projects"). | `ON RECORD` — Canva help centre |

**What Plinth should take:** (a) **exactly one place for account/settings** (Canva's *More*, Pixelcut's settings icon) — never a top-level page a user must find; (b) **the first tab is a job, and the AI surface gets its own tab**; (c) commercial surfaces can be demoted — Plinth's Wallet is currently the *only* place a balance exists, which is the inverse mistake.

---

### 1.8 SellerPic — a marketplace-seller-focused tool

Sources read: SellerPic marketing pages, an AppSumo customer review thread (2026), and Trustpilot/product-hunt material.

| # | Dimension | Finding | Label / source |
|---|---|---|---|
| 1 | **Navigation shell** | Web platform, reworked to a **"new layout"**; a reviewer's complaint is that *"navigation is worse than before on new layout"*. A native Android app was not verified. | `ON RECORD` — AppSumo review thread 2026; `UNVERIFIED` — mobile app existence |
| 2 | **First screen (hero)** | Marketed as a *"sleek and user-friendly"* suite of listing tools. Detailed layout not verifiable. | `ON RECORD` — sellerpic.ai marketing |
| 3 | **The user's own photo** | Product image upload → AI edit / model shots / try-on / image-to-video. | `ON RECORD` — sellerpic.ai |
| 4 | **How AI-ness is communicated** | *AI instruct edit*, *AI model shots*, *virtual try-on*, *image-to-video*. | `ON RECORD` — sellerpic.ai |
| 5 | **Value and cost** | **Credits**, and reviews complain specifically that *"credits are used before generating"* for wrong output sizing, with requests for **credit reimbursement**. The seller-facing grievance is about **credit spend on a failed result**. | `ON RECORD` — AppSumo review thread 2026 |
| 6 | **Result screen** | Platform-specific export sizes for marketplaces. | `ON RECORD` — sellerpic.ai |
| 7 | **Visual language** | `NOT VERIFIED` | `NOT VERIFIED` |

**This is the nearest peer to Plinth's audience, and its loudest complaint is a credits complaint.** Plinth's marketplaces are the same (Wildberries, Ozon, Avito, Etsy, eBay…). A seller who burns credits on a result they cannot use churns. That is an argument for the Wallet tab in §4 and for making a generation's cost legible *before* it runs.

---

## 2. The convergent pattern

Seven patterns recur across the set. Each is stated with the apps that demonstrate it; the shell proposal in §3 maps onto these one-for-one.

### 2.1 The shell is 3–5 persistent tabs, and "create" is one of them

- **Canva**: five bottom tabs — Canva AI, Create, Templates, Your Designs, More (`ON RECORD`).
- **Pixelcut**: bottom navigation bar with Home / Ideas / Projects / Profile (`ON RECORD`, secondary).
- **Photoroom**: Home / AI Tools / Content / Batch as top-level screens with a header plan button (`ON RECORD`).
- **Pebblely**: Home / Recent / Profile with tabs nested inside the generation flow (`ON RECORD`).

**Convergent rule:** top-level destinations are *jobs a user repeats*, not tools. The count is 3–5, and there is always **exactly one** place for account/billing/settings (Canva *More*; Pebblely *Profile*; Pixelcut's settings icon; Photoroom's header).

### 2.2 Account, billing and settings are *demoted*, never a top-level destination

Canva **moved Profile from the top corner into "More"** (`ON RECORD`). Pebblely puts billing under *Profile → Manage billing* (`ON RECORD`). Photoroom keeps plan and *Get Pro* in the header (`ON RECORD`).

**Inverse rule for Plinth:** the *balance* is not an account screen — it is a **status element** that lives in the chrome (§2.5) and *links* to a top-up screen.

### 2.3 The first screen is a gallery of imagery, not a list of buttons

- Photoroom Home = **tool grid + Start from Photo** (`ON RECORD`).
- Pixelcut Home = **horizontally scrolling large visual cards** (`ON RECORD`).
- Pebblely Home = **sample products** + Add product (`ON RECORD`).
- Flair = **template gallery** on entry (`ON RECORD`).
- Canva Create = **Quick Create shortcuts** (`ON RECORD`).

**Convergent rule:** the user's eye lands on an *image* before it lands on a sentence. A screen whose top item is a heading, a paragraph of explanation, and a column of text buttons is the shape the owner rejected.

### 2.4 The user's photo is treated as the hero object

Pebblely makes the user **crop to the product with zoom and brush** before anything is generated (`ON RECORD`). Flair **locks the product and generates around it** (`ON RECORD`). Photoroom removes the background and shows the item on a template (`ON RECORD`). Flair's and Plinth's own promise are the same sentence: *keep the item's pixels, style everything around it* (`MEASURED` — `res/values/strings_home.xml:3-4`).

### 2.5 AI-ness is signalled by four devices — none of which is a slider

1. **Generated preview first.** The value is *shown*, in seconds — Photoroom's own onboarding recommendation is "watch the background vanish" (`ON RECORD`).
2. **Named presets and scenes**, chosen visually — Pebblely's **40+ named templates** (Cafe, Studio, Kitchen, Water, Nature, Bathroom) (`ON RECORD`); Photoroom's named AI tools (`ON RECORD`); Claid's named operations (`ON RECORD`). Parameter sliders are how a *photo editor* looks; scene cards are how an *AI app* looks.
3. **Variant sets.** Photoroom offers **variants in a context menu** (`ON RECORD`); Pebblely generates **1/2/4 at once** (`ON RECORD`); Midjourney's own design system renders progress as a **shimmer placeholder** while the 2×2 grid of results materialises (`ON RECORD`, `docs/design/midjourney-DESIGN.md`).
4. **A shimmer / glow process language that ends when the result arrives.** Apple's Intelligence design guidelines define the shimmer as a **process indicator** — a modulated glow swept across the surface that *conveys ongoing activity* and then **fades as the result becomes ready** (`ON RECORD`, Apple HIG *Generative AI* / *Machine learning*). Midjourney's dark theme specifies it concretely: true-black canvas, and *"While rendering: shimmer placeholder (#1A1A1A → #2A2A2A sweep)"* (`ON RECORD`, `docs/design/midjourney-DESIGN.md`).

### 2.6 One balance, shown where the work happens, with cost attached to the choice

This is the pattern the owner asked about directly, and the evidence is unambiguous:

- **AI Studio Plus' own app-design doctrine**: a *"clean modern interface with a **always visible credit counter** in the header"*, where **tapping the counter opens credit details and purchase options** (`ON RECORD`).
- **Intangible.ai** puts the credit counter **top-right, in the toolbar row** (`ON RECORD`).
- **Photo AI** shows the balance **top-right**, and separates it into a **large number = subscription credits** and a **small number = additional credits**, with a **Buy more credits** action adjacent (`ON RECORD`).
- **Pixelcut** shows the **cost in credits inside the model picker, before generating** (`ON RECORD`).
- **Claid** publishes **every operation in one credit table** (`ON RECORD`).
- **SellerPic**'s top complaint is *credits spent on a result that did not fit*, with reimbursement requests (`ON RECORD`).

**Convergent rule:** *one* balance, always on screen in the chrome; the *cost of the next action* shown at the point of the action; top-up reached by tapping the balance; and per-action prices flattened into a small number of tiers rather than a unique price per button.

### 2.7 Visual language: what is measurable to copy

Because no competitor's pixels were observable this session, this sub-section uses the committed design references instead of invented screenshots. The owner's specific complaint — *"не относительно центра"* (a progress indicator that spins off-centre) — is precisely what the Apple shimmer guidance exists to fix: progress belongs **inside a bounded placeholder of the result's own shape**, not floating in the layout (`ON RECORD`, Apple HIG). The measurable conventions worth copying:

- **Bounded placeholder while generating** — a skeleton at the result's aspect ratio with the progress inside it; never a bare spinner in a stack.
- **True-dark card hierarchy with hairline borders** rather than grey fills — Midjourney's system uses `#000000` canvas, `#0E0E0E`/`#1A1A1A` surfaces and a `#2A2A2A` hairline (`ON RECORD`, `docs/design/midjourney-DESIGN.md`). Plinth's palette is its own (`store/studio.json`); the *principle* is "depth from hairlines and one accent", not these hex values.
- **One accent for the charged/guaranteed thing** — Plinth already reserves verdigris for "guarantee/pass" and clay for "charged credits" (`MEASURED`, `store/studio.json`). Competitors spend colour on the AI action and keep everything else neutral; Plinth should hold that line rather than introduce a third accent.
- **Dense imagery, thrifty words** — a gallery row of ~2.5 cards visible at once (the Pixelcut idiom), not a single full-width card per tool.

---

## 3. Plinth's current state, measured — what is actually wrong

Before proposing, the defect is named from the repository, not from taste.

### 3.1 There is no shell at all

| Finding | Evidence |
|---|---|
| `PlayKilnApp.kt` has **no bottom bar, no tab row, no navigation rail**. Only `NavDisplay`. | `MEASURED` — `ui/PlayKilnApp.kt:183` (`NavDisplay(`) |
| Every screen builds its **own** `Scaffold` + `TopAppBar` — so each destination is an island with its own chrome. | `MEASURED` — `ui/home/HomeScreen.kt:139`; `ui/wallet/WalletScreen.kt:117` |

Consequence: there is no persistent menu, no persistent status, and the app feels like a stack of full-page documents — the *"блокнот"* the owner named.

### 3.2 The balance is invisible almost everywhere, and the prices are everywhere

| Finding | Evidence |
|---|---|
| The wallet balance is rendered on **exactly one** working screen — the Cutout screen, and only when non-null. | `MEASURED` — `ui/cutout/CutoutScreen.kt:314` (`cutout_wallet_balance`); `res/values/strings_cutout.xml:30` |
| The Home toolbox prints a **per-tool price list** and tells the user to *"confirm the current quote before running a tool"*. | `MEASURED` — `res/values/strings_home.xml:15` (`home_toolbox_note`), `:16` (`home_tool_price` = *"Credits per result: %1$d"*), `:17` (`home_upscale_price` = *"2K: %1$d credits · 4K: %2$d credits"*), `:34` (`home_slide_price` = *"Local layout · AI scene priced separately"*) |
| The Wallet **did render the developer's pricing arithmetic into the shipped UI**: *"Price hypothesis: $7.29 ÷ 90 = $0.081 per credit"* — and *"$18.49 ÷ 230 = $0.0804"*, *"$47.99 ÷ 600 = $0.0800"*, *"$28.99 · removes the watermark and free batch limit"*. | `MEASURED` — `git diff` of `ui/wallet/WalletScreen.kt` shows four call sites passing `R.string.wallet_pack_starter_math` / `_studio_math` / `_pro_math` / `wallet_unlock_math` into a parameter literally named `arithmetic`; those keys were in `res/values/strings_wallet.xml` and `res/values-ru/strings_wallet.xml`. **CORRECTION TO THE RECORD (2026-10-16): an earlier revision of this row retracted the claim, because a grep run by the orchestrator found nothing. That grep was made *after* worker `plinth-w6-wallet-surface` had already deleted those four keys, so it measured the deletion and not the shipped app. The retraction was wrong and is withdrawn.** The strings are gone from the tree as of `plinth-w6-wallet-surface`; they were present in the revision the owner reviewed. | `MEASURED` — `git diff -- app/src/main/java/.../ui/wallet/WalletScreen.kt` (`-` lines for the four `arithmetic = stringResource(...)` call sites) and `git diff -- app/src/main/res/values/strings_wallet.xml` (`- wallet_pack_starter_math`, `- wallet_pack_studio_math`, `- wallet_pack_pro_math`, `- wallet_unlock_math`) |
| The Wallet is a **dead end** in some states: *"Wallet actions are not connected in this build."* | `MEASURED` — `res/values/strings_home_nav.xml:44` (`home_nav_wallet_unavailable`) |
| Several tool entries have **no execution route**: *"This enhanced operation has no execution route in this build."* | `MEASURED` — `res/values/strings_home_nav.xml:46` |

This is exactly the owner's complaint, and the mechanism is now on the record: **many per-item prices, one balance that is on two screens out of roughly twenty, the cost of an action stated as prose instead of as a number on the action itself — and, in the revision the owner reviewed, the developer's own pricing arithmetic printed where a competitor shows the balance that is left.** (One row in this section was retracted and then un-retracted; the audit trail is in the row itself. The conclusion did not change: the strings were real, and worker `plinth-w6-wallet-surface` removed them on 2026-10-16.)

### 3.3 The hero exists but is buried under a tool list

`HomeScreen` does have a hero (`StudioHero`, `MEASURED` — `ui/home/HomeScreen.kt:171,340`) and the copy is genuinely good — *"One photo.\nA whole studio."* (`res/values/strings_home.xml:3`). But below it the screen is a `LazyRow` of recent photos and then **seven `StudioToolCard`s** (`MEASURED` — `ui/home/HomeScreen.kt:199,244,263,272,281,295,304,313`) each with a paragraph of description. The hero is outnumbered by prose.

### 3.4 The result screen has no handoff

The AutoCard result ends at a preview; the app's own string says *"Your AI smart card is saved as a project output. Open it again from Studio tools."* (`MEASURED` — `res/values/strings_home_nav.xml:32`) — i.e. the finish line is "go and find it elsewhere", while competitors end at share/save/export (Photoroom, Pebblely, Flair, all `ON RECORD`).

---

## 4. Proposed shell for Plinth — a decision an engineer can implement

**Design stance.** Plinth's promise (*"One photo. A whole studio." / "Keep the item's pixels. Style everything around it."*) is Flair's promise and Photoroom's promise, and Plinth's voice is a gallery: a plinth, paper, graphite, verdigris. The shell should read as **a studio with four benches**, not as a menu with twenty entries.

### 4.1 The version-1 shell

**Three bottom-bar destinations, plus a persistent create action. Not four, not five.**

**Why the balance is *not* a tab** (this is a design decision derived from §2, not an owner instruction): every competitor measured in §2.6 puts the balance **in the chrome** and keeps top-up reachable *from the balance* — AI Studio Plus' header counter that opens purchase options, Intangible's toolbar counter, Photo AI's top-right number with *Buy more credits* beside it. None of them spends a tab on it. A `Wallet` tab would also be the weakest of the candidate tabs, because it is a *status plus a purchase*, both of which a header chip and a Library section carry at lower cost.

| # | Tab name | What it contains | Icon direction |
|---|---|---|---|
| 1 | **Studio** | The home. Hero = the user's current photo on the plinth + the single primary action *Bring in a product photo*. Below: a horizontal **Scene strip** of named Look cards (the 40-preset idiom), then the tool gallery as **image cards**, then the recent-projects rail. | the plinth mark |
| 2 | **Library** | Every project, output and finished image, with **receipt history** (what was charged, when) and **top-up packs** inside it. Grouping, batch, re-export. | stacked frames |
| 3 | **More** | Settings (language, theme, quality), marketplace rules, about, the policy link, account state. Singular. | the Lucide `settings` / `more-horizontal` glyph already vendored |

Plus:

- **A persistent create action**, rendered as a **raised centre action in the bottom bar** (`+` inside a filled plinth-coloured disc), which opens the **photo picker** and lands directly in the Smart-card flow. Justification: Photoroom's *Start from Photo* and Canva's *Create* tab both exist because the create path must never be more than one tap (`ON RECORD`).
- **Rationale for three, not four or five**: three is the floor at which every *job category* (make → keep → configure) has a home and none is a parking lot. Canva's five tabs are a suite of products; Plinth is one product, and a fourth tab would exist only to hold something a header chip or the Library can hold better. The create action carries the weight a fourth "Create" tab would.
- **Tabs 1 and 2 are jobs** (`ON RECORD` — Canva *Create* replaced *Home*; Pebblely's *Recent* is a job view). Tab 3 is Canva's *More* and Pebblely's *Profile* (`ON RECORD`).

### 4.2 The balance lives in the chrome on every tab

A **`BalanceChip` in the top app bar, on every top-level destination, at all times**: `verdigris disc · 120` — tap → opens the **Library** tab's top-up section (or a sheet). Justification: AI Studio Plus doctrine (*"always visible credit counter in the header"*, tap-to-top-up), Intangible (counter top-right), Photo AI (top-right, subscription vs extra credits). This single element answers the owner's *«Мы же договаривались, что какой-то один баланс какой-то будет у человека»*.

Rules that make it honest:

- **One balance.** No second balance anywhere. No per-item price list on the Studio screen.
- **Cost appears on the action card as a chip** (*"2", "4"*), not as a sentence. Fold Plinth's tiers into **three or four tiers** (the Claid pattern: a 2–60 range is a table, not a scatter) so a user learns prices.
- **Never render arithmetic.** The four *"Price hypothesis: …"* strings were removed from the shipped set by `plinth-w6-wallet-surface` on 2026-10-16 (`wallet_pack_starter_math`, `wallet_pack_studio_math`, `wallet_pack_pro_math`, `wallet_unlock_math`); `economics.json:50–51` stays internal. Do not let an internal derivation re-enter the user-facing string set.
- **The balance chip reads from one source** and is shown even when loading (`— credits`), so it never disappears the way `cutout_wallet_balance` can today.

### 4.3 The first screen (Studio) is imagery-first

1. **Hero tile** — the user's photo, full-width, ~4:3, on the plinth, with the promise caption. If no photo: an empty-plinth illustration and the words *Start with your own photo* (already the string `home_preview_empty`).
2. **One primary action** — *Bring in a product photo* (already `home_import_photo`).
3. **Scene strip** — a horizontal `LazyRow` of **Look/Scene cards**, each a rendered thumbnail with its name. This is the single highest-value adoption: it converts Plinth's existing scene concept from a list into the Pebblely/Photoroom idiom (`ON RECORD`).
4. **Tool gallery** — the seven `StudioToolCard`s become **compact image cards in a 2-column grid**, each with a glyph, a name and a cost chip; the descriptions become one short line, not a paragraph. Justification: Pixelcut's home is *"a horizontally scrolling list of large, visually rich cards"* (`ON RECORD`).
5. **Recent rail** — `RecentPhotoCard` stays, moved below the tools (or kept above if a project exists — "Pick up where you left off", `home_continue_title`).

**Word budget:** no more than one short line of prose before the first image; the current Home screen's page-length `home_toolbox_note` (`res/values/strings_home.xml:15`) is deleted from the UI.

### 4.4 The result screen is a destination with a handoff

After a generation completes, show a **dedicated result screen** (not an inline card inside the flow screen):

- **Full-bleed result image** on the canvas.
- **Before / After** toggle (or slider) comparing the source photo and the result — the Flair/Photoroom before-and-after idiom (`ON RECORD`, §1.4, §1.1).
- **Variants strip** if more than one was generated, with the active one selected — the Photoroom variant / Midjourney 2×2 idiom (`ON RECORD`).
- **Primary actions**: *Save to Library* (full width), then *Share* and *Export pack* as secondary — this is the handoff Plinth's AutoCard screen currently lacks (`MEASURED`, §3.4).
- **Cost line**, stated after the fact from the receipt: *"−4 credits · balance 116"* — this is the SellerPic lesson (a seller must be able to see what a result cost), and it is honest because the charge already happened.
- **During generation**, a **shimmer skeleton bounded to the result's exact aspect ratio** with the progress inside it, which fades as the result appears (Apple HIG; Midjourney's `#1A1A1A → #2A2A2A` placeholder). This directly fixes the owner's *"ползунок крутится не относительно центра"* and *"меня выбросило"*.

### 4.5 Where Settings sits

Settings is the **third tab (More)**, singular, holding app settings **only** (language, theme, quality, marketplace rules, about, policy). Justification: this is the Canva *More* shape, which exists in the product the owner cited as an example of a clear shell, and Canva's own restructure moved account functions there rather than leaving them in a top corner (`ON RECORD`).

A sibling task owns `docs/settings-ia.md` and is designing the *internals* of that screen — including how per-image composition defaults are reached. **This document does not read, edit or restate that file.** The shell above only gives it a home: the tab is the container; the internals are that document's decision.

### 4.6 Banned in this shell

- **A dedicated `Wallet` tab.** The balance lives in the chrome and top-up is reached from it (`ON RECORD` — AI Studio Plus, Intangible, Photo AI, all §2.6); receipt history sits inside Library. A status element does not deserve a destination.
- **Per-item prices rendered as prose.** Cost is a chip on an action card, and one balance is authoritative.
- **Any internal pricing arithmetic** in the user-facing set — the four *"Price hypothesis: …"* strings that shipped in the reviewed revision are the measured instance, removed 2026-10-16 — **or** *"not connected in this build"* / *"no execution route in this build"* text.
- **A screen whose top element is a heading plus a paragraph.** Every top-level screen opens on imagery or on a list of visual choices.
- **A floating spinner** during generation.

---

## 5. Adoption table — element → the file that carries it

All paths are relative to `apps/2026-10-15-plinth-product-photos/app/src/main/java/com/gexetr/plinthproductphotos/`. Every path below was read this session (`MEASURED`).

| # | Proposed element | File(s) that carry it | Change shape |
|---|---|---|---|
| 1 | Three-tab bottom bar + raised create action; single `Scaffold` hosting `NavDisplay` with `bottomBar` | `ui/PlayKilnApp.kt` (`NavDisplay` at `:183`) · `ui/components/PlinthScaffold.kt` (already exposes a `bottomBar` slot) | Wrap `NavDisplay` in one `PlinthScaffold`; add `ui/components/PlinthNavBar.kt`; remove per-screen `TopAppBar`s that duplicate chrome |
| 2 | Persistent `BalanceChip` in the top app bar of every tab | new `ui/components/PlinthTopBar.kt` · `ui/components/CostChip.kt` (chip used by tool cards) | New composables; feed from the wallet state below |
| 3 | Balance as shell-visible state (single source) | `ui/wallet/WalletScreen.kt` (`WalletUiState.balance`, `:79`) · `ui/wallet/WalletRoute.kt` · `ui/wallet/WalletViewModel.kt` | Hoist balance to app-level state the shell can read; keep one accessor |
| 4 | Library tab = projects + receipt history + top-up | `ui/library/LibraryScreen.kt` · `ui/wallet/WalletScreen.kt` | Library gains a receipts section and a top-up section; Wallet packs move in |
| 5 | Remove developer arithmetic + dead-end strings | `res/values/strings_wallet.xml:14,16,18` · `res/values/strings_home.xml:15-17,34` · `res/values/strings_home_nav.xml:45-48` · their `values-ru/` mirrors | Delete/replace; add plain price-tier strings |
| 6 | Studio hero as the first element | `ui/home/HomeScreen.kt` (`StudioHero`, `:340`, invoked `:171`) · `ui/home/HomeUiState.kt` · `ui/home/HomeViewModel.kt` | Keep the hero; move it to the top; delete the prose note |
| 7 | Scene strip of named Look cards | new `ui/components/SceneCard.kt` · `data/look/LookRepository` · assets `assets/autocard/scene-recipes.json` | Render the existing Look/scene catalogue as a `LazyRow` of thumbnails |
| 8 | Tool gallery as compact image cards with cost chips | `ui/home/HomeScreen.kt` (`StudioToolCard`, `:495`; call sites `:244-313`) · `ui/studio/StudioToolsScreen.kt` (`:50`) · `ui/components/CostChip.kt` | Restyle + regroup into a 2-column grid |
| 9 | Generating placeholder (bounded shimmer) | new `ui/components/GeneratingSkeleton.kt` · `ui/autocard/AutoCardScreen.kt` (`:250`; current progress row at `:108`) | Replace the bare progress row with an aspect-bounded skeleton |
| 10 | Result destination: before/after, variants, save/share/export, cost line | `ui/PlayKilnApp.kt` (`AutoCardResultScreen`, `:609`; invoked `:276`) · `ui/autocard/AutoCardScreen.kt` · `ui/autocard/AutoCardResultsViewModel.kt` | Promote the result to its own destination with the handoff actions |
| 11 | Before/After comparison control | new `ui/components/BeforeAfterSlider.kt` | New composable |
| 12 | Export pack screen reached from result and Library | `ui/export/ExportScreen.kt` (`:49`) · assets `assets/marketplaces.json` | Wire the two entry points |
| 13 | Settings as the third tab (app settings only) | `ui/settings/SettingsScreen.kt` (`:98`) | Reachable from the shell; internals are `docs/settings-ia.md`'s decision — **not edited here** |
| 14 | Icons for tabs, cost chip, actions | `res/drawable/ic_lucide_*.xml` (the vendored Lucide set) | Reuse the existing licence-cleared family; no new icon set |
| 15 | Nav/tab labels in both locales | `res/values/strings_home_nav.xml` · `res/values-ru/strings_home_nav.xml` | Rename/replace `home_nav_*` nav strings to the three tab names |
| 16 | Palette + accent discipline (verdigris = guarantee, clay = charged) | `ui/theme/Color.kt` · `ui/theme/Theme.kt` · `store/studio.json` | Hold the existing palette; do not add a third accent for AI |

---

## 6. Claim ledger

`CLAIM / LABEL / EVIDENCE`. `MEASURED` rows name a command or a `path:line` read this session; external rows are `ON RECORD` with their source; `UNVERIFIED` is stated as such.

```
CLAIM                                                                                              LABEL      EVIDENCE
-----------------------------------------------------------------------------------------------    --------   --------------------------------------------------------------
Photoroom's top-level screens are Home / AI Tools / Content / Batch                                 ON RECORD  help.photoroom.com "Get started with Photoroom" (read 2026-10-16)
Photoroom Home is documented as the place to access AI Tools, Batch and Content                      ON RECORD  same help page, quoted
Photoroom carries a bottom navigation bar (Home/Content/AI Tools)                                    ON RECORD  web-search synthesis of Photoroom help articles, 2026-10-16 (secondary; help text does not print the control)
Photoroom's Home drops the user on a search bar + tool grid + "Start from Photo" CTA                 ON RECORD  botsi.com blog, "best free product-photo apps", 2026
Photoroom's AI Tools editor offers "Preview for generated images" and per-image variant menus        ON RECORD  help.photoroom.com "The AI Tools tab"
Photoroom exposes a usage panel with AI images %, AI videos %, Exports 0/250 and a reset date        ON RECORD  help.photoroom.com search synthesis, 2026-10-16
Photoroom's export offers platform resize presets                                                    ON RECORD  help.photoroom.com export articles
Photoroom's exact colours, radii and type scale                                                      NOT VERIFIED  no screenshot observed this session
Pixelcut has a bottom navigation bar; tab labels reported as Home/Ideas/Projects/Profile             ON RECORD  lazyweb.ai teardown 2026-10-16 ("plus bottom navigation"); search synthesis for labels (secondary)
Pixelcut's home is a horizontally scrolling list of large visually rich tool cards                   ON RECORD  screensdesign.com, 2026-10-16
Pixelcut shows each model's credit cost in the model picker before generating                        ON RECORD  help.pixelcut.app "Pixelcut Credits", quoted
Pixelcut uses one credit balance replenished by subscription, with on-demand extra credit purchase   ON RECORD  help.pixelcut.app "Pixelcut Credits" / "Credit FAQ"
Pixelcut extra-credit purchase is reached from the Settings page                                     ON RECORD  help.pixelcut.app "Credit FAQ"
Pixelcut results can be assembled into collages/templates and saved or shared                        ON RECORD  help.pixelcut.app collage articles; App Store listing read 2026-10-16
Pebblely's documented screens are Home, Recent, Profile, with Templates/Custom tabs in the flow      ON RECORD  help.pebblely.com "Getting started", "Manage billing", "Viewing and downloading images"
Pebblely's Home carries a set of sample products                                            ON RECORD  help.pebblely.com "Getting started", quoted
Pebblely makes the user crop/refine the product (zoom + brush) before generating                     ON RECORD  help.pebblely.com "Getting started"
Pebblely offers 40+ named preset backgrounds (Cafe, Studio, Kitchen, Water, Nature, Bathroom)        ON RECORD  help.pebblely.com "AI Product Photos" / "Getting started"
Pebblely generates 1/2/4 images at a time; results take ~10 s; no background removal required        ON RECORD  help.pebblely.com "AI Product Photos"
Pebblely's Recent screen filters by resolution (All/HD/Standard) and supports bulk download           ON RECORD  help.pebblely.com "Viewing and downloading images"
Pebblely's on-screen balance placement                                                               UNVERIFIED  not documented; no screenshot observed
Flair.ai is a web drag-and-drop canvas with a Custom Models tab and an Image Editor suite            ON RECORD  flair.ai product pages; shotkit.com review 2026
Flair.ai's entry is a template gallery of "more than a dozen options" plus a custom prompt           ON RECORD  shotkit.com review 2026, quoted
Flair.ai's Image Editor exposes Magic Erase, variations, extend, upscale, regenerate, pose/angle      ON RECORD  flair.ai product pages; shotkit.com review 2026
Flair.ai has a native Android app                                                                   UNVERIFIED  none found this session
Booth.ai is web + WhatsApp; left menu = Workflows, Products, AI Scene Generator, Gallery             ON RECORD  geekdashboard.com review 2026
Booth.ai prices ~$0.50/photo with 10 free photos on signup                                           ON RECORD  geekdashboard.com review 2026
Booth.ai's WhatsApp flow returns an HD result in ~30 s                                              ON RECORD  geekdashboard.com review 2026
Claid.ai prices every operation in credits in one published table (Remove bg 1; Upscale 2-8;
  Outpaint 2; AI Shadows 2; AI Photoshoot 4; AI Fashion 4; AI Edit 4; AI Video 60)                   ON RECORD  claid.ai pricing page, read 2026-10-16
Claid.ai's consumer-facing mobile shell                                                              NOT VERIFIED  web/API product
Canva mobile has five bottom tabs: Canva AI, Create, Templates, Your Designs, More                   ON RECORD  canva.com/help "Understanding the Canva mobile app tabs", read 2026-10-16
Canva moved Profile from the top corner into More                                                    ON RECORD  same Canva help page
Canva's Create replaced Home and lands on Quick Create shortcuts                                     ON RECORD  same Canva help page
Canva's Print Cart was moved into More during the same restructure                                   ON RECORD  same Canva help page
SellerPic's loudest reviewer complaint is credits spent on ill-fitting output, with refund requests  ON RECORD  appsumo.com review thread 2026
SellerPic has a native Android app                                                                   UNVERIFIED  not verified this session
AI Studio Plus' design doctrine specifies an always-visible header credit counter that opens
  purchase options on tap                                                                            ON RECORD  aistudioplus.app app-design article, read 2026-10-16
Intangible.ai places its credit counter top-right in the toolbar row                                 ON RECORD  intangible.ai product page, read 2026-10-16
Photo AI shows the balance top-right, large number = subscription credits, small = extra credits     ON RECORD  photoai.com help/FAQ, read 2026-10-16
Apple's generative-AI guidance defines the shimmer as a process indicator that fades when the
  result is ready                                                                                    ON RECORD  Apple HIG, "Generative AI" / "Machine learning", read 2026-10-16
Midjourney's design system renders progress as a shimmer placeholder (#1A1A1A -> #2A2A2A sweep)
  on a true-black canvas with #0E0E0E/#1A1A1A surfaces and a #2A2A2A hairline                        ON RECORD  docs/design/midjourney-DESIGN.md (committed in this repo)
Plinth's PlayKilnApp.kt has no bottom bar, no tab row and no navigation rail; only NavDisplay        MEASURED   ui/PlayKilnApp.kt:183 (`NavDisplay(`); repo-wide grep found no NavigationBar/NavigationRail/TabRow in the slot
Each Plinth screen builds its own Scaffold + TopAppBar                                               MEASURED   ui/home/HomeScreen.kt:139; ui/wallet/WalletScreen.kt:117
Plinth renders the wallet balance on exactly one screen (Cutout), and only when non-null             MEASURED   ui/cutout/CutoutScreen.kt:314; grep for `wallet_balance` returned only that call site
Plinth prints a per-tool price list and tells the user to confirm the quote before running a tool    MEASURED   res/values/strings_home.xml:15
Plinth has per-result, split and "priced separately" price strings                                   MEASURED   res/values/strings_home.xml:16,17,34
Plinth's Wallet renders the developer's pricing arithmetic into the shipped UI                       MEASURED   ui/wallet/WalletScreen.kt:177,187,197; res/values/strings_wallet.xml:14,16,18
Plinth's Wallet is a dead end in some states ("not connected in this build")                        MEASURED   res/values/strings_home_nav.xml:45
Several Plinth tool entries have no execution route                                                 MEASURED   res/values/strings_home_nav.xml:46
Plinth's Home hero copy is "One photo. A whole studio." and the promise line                         MEASURED   res/values/strings_home.xml:3-4
Plinth's Home hero is followed by a recent rail and seven StudioToolCards with descriptions          MEASURED   ui/home/HomeScreen.kt:171,199,244,263,272,281,295,304,313
Plinth's AutoCard result tells the user to go and find the output elsewhere                          MEASURED   res/values/strings_home_nav.xml:32
Plinth has a PlinthScaffold with a bottomBar slot, and only three shared components                 MEASURED   glob of ui/components/*.kt -> PlinthScaffold.kt, ProofRing.kt, PlinthStage.kt
Plinth's settings screen exists as a single composable                                               MEASURED   ui/settings/SettingsScreen.kt:98
The owner rejected the app as a "notepad with buttons" and asked for the competitor class,
  and stated the one-balance principle                                                               ON RECORD  owner's words quoted in this task's brief (2026-10-16)
Placing the balance in the header rather than in a tab is the better choice for Plinth             INFERRED   follows from ON RECORD rows for AI Studio Plus / Intangible / Photo AI (none tab-devotes the balance); not measured in Plinth
Proposed tab names perform well as user-facing labels; the exact wording is a free choice            UNVERIFIED  no usability test; the owner approves naming at checkpoint 1
A three-tab bar will improve retention or revenue at Plinth                                         UNVERIFIED  no measurement; this is a design proposal, not a result
```

---

## 7. Sources

Read 2026-10-16 unless noted.

- Photoroom Help Center (GB) — *Get started with Photoroom*: <https://help.photoroom.com/en/articles/7251200-get-started-with-photoroom>
- Photoroom Help Center (GB) — *The AI Tools tab*: <https://help.photoroom.com/en/articles/9855741-the-ai-tools-tab>
- Botsi — *best free AI product-photo apps* (2026): <https://www.botsi.com/blog/ai-product-photo-apps/>
- Pixelcut Help Center — *Pixelcut Credits*: <https://help.pixelcut.app/en/articles/10520083-pixelcut-credits>
- Pixelcut Help Center — *Credit FAQ*: <https://help.pixelcut.app/en/articles/10520085-credit-faq>
- Pixelcut — Apple App Store listing: <https://apps.apple.com/us/app/pixelcut-ai-photo-editor/id1446784264>
- Lazyweb — Pixelcut sign-in / navigation teardown: <https://www.lazyweb.com/teardowns/pixelcut>
- screensdesign — Pixelcut app redesign article: <https://screensdesign.com/app/pixelcut>
- Pebblely Help Center — *Getting started*: <https://help.pebblely.com/en/article/getting-started-1ih72y6/>
- Pebblely Help Center — *AI Product Photos*: <https://help.pebblely.com/en/article/ai-product-photos-1a5n8ab/>
- Pebblely Help Center — *Viewing and downloading images*: <https://help.pebblely.com/en/article/viewing-and-downloading-images-1s5fhy4/>
- Pebblely Help Center — *Manage billing*: <https://help.pebblely.com/en/article/manage-billing-19ikokb/>
- Flair.ai — product photography: <https://flair.ai/product-photography> · fashion: <https://flair.ai/fashion-photography>
- Shotkit — *Flair.ai review* (2026): <https://shotkit.com/flair-ai-review/>
- GeekDashboard — *Booth.ai review* (2026): <https://www.geekdashboard.com/booth-ai-review/>
- Claid.ai — pricing / credits: <https://claid.ai/pricing>
- Canva Help Center (GB) — *Understanding the Canva mobile app tabs*: <https://www.canva.com/help/canva-mobile-app-tabs/>
- SellerPic — product pages: <https://sellerpic.ai/> · AppSumo review thread: <https://appsumo.com/products/sellerpic/reviews/>
- AI Studio Plus — app design for AI/credit-based apps: <https://aistudioplus.app/app-development/ai-app-design>
- Intangible.ai — product page (credit counter placement): <https://www.intangible.ai/>
- Photo AI — help / credits: <https://photoai.com/help>
- Apple Human Interface Guidelines — *Generative AI* and *Machine learning*: <https://developer.apple.com/design/human-interface-guidelines/generative-ai>
- In-repo design reference: `docs/design/midjourney-DESIGN.md`
