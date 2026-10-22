# Social cover studio

## Purpose

Let a creator open Coverkit, pick a social destination the way working makers do, finish a cover or post graphic with familiar tools (template or photo → text/stickers → export), and leave with a sharp PNG — without an account, without an AI key, and without a subscription to save. Depth (brand kit, layers, multi-size pack) exists after that first success. Every primary screen, tool and label below is **assembled from measured Play leaders** (`docs/research/2026-10-21-coverkit-competitor-matrix.md`); inventing alternate chrome is a defect.

## Normative sources (do not invent past these)

| Kind | Source |
| --- | --- |
| Flow shape | Thumbnails4U how-to; Thumbnail Maker; Social Media Post Maker network-first IA |
| Tools | Thumbnail Maker / Ultimate (text, stickers, shapes); InstaSize (crop/resize, text border); StoryArt (story size fit) |
| Brand consistency | Unfold Pro brand colors/logo/fonts — **offline only** |
| Failures to forbid | Blur export; lost projects; sub-to-save; Pro not activating; tiny locked text; non-system gallery |
| Our wedge | One design → multi-network PNG pack + one-time unlock (StoryArt users asked for lifetime) |

## ADDED Requirements

### Requirement: Screen map — only these primary destinations

The app SHALL expose exactly this primary navigation (names are product nouns; UI chrome follows Material 3 + maker patterns, not a novel IA):

1. **Home (Destinations)** — list of network sizes (Social Media Post Maker / Thumbnails4U “select image type” pattern).  
2. **Templates** — grid for the chosen destination (all makers).  
3. **Editor** — canvas + bottom tool row.  
4. **Projects** — saved drafts; open / duplicate / delete (Social Media Post Maker draft+duplicate praise).  
5. **Brand** — optional kit (Unfold Pro idea, local).  
6. **Settings** — language, theme, restore purchase, about/privacy/rate.

The app SHALL NOT open on a multi-feature marketing dashboard, a blank canvas with no destination, or an onboarding carousel that blocks Home.

#### Scenario: Cold start lands on Destinations

- **WHEN** the user launches with no project open
- **THEN** Home shows destination cards (YouTube, Instagram, TikTok, WhatsApp families) as the primary content

#### Scenario: No invented hub tiles

- **WHEN** Home is inspected
- **THEN** there is no equal-weight tile row inventing features outside Destinations / Projects / Brand / Settings entry points

### Requirement: Happy path — three beats, shelf vocabulary

The happy path SHALL be:

**Destination → Templates or Photo → Export**

Matching Thumbnails4U: select type → photo/template → customize → save/share. First export SHALL NOT require Brand, Sizes pack, Layers panel, or Settings.

Primary actions SHALL use shelf nouns as string resources: `Templates`, `Photo`, `Text`, `Stickers`, `Crop`, `Export`, `Share`, `Save` (see competitor matrix “Control nouns”).

#### Scenario: First YouTube cover

- **WHEN** a new user taps YouTube thumbnail → picks a free template or Photo → optionally edits one Text → taps Export
- **THEN** a PNG of 1280×720 is offered via Share and/or Save

#### Scenario: First vertical cover

- **WHEN** the user picks Instagram story, TikTok cover, or WhatsApp status
- **THEN** the same Destination → Templates/Photo → Export path works at 1080×1920

#### Scenario: First square post

- **WHEN** the user picks Instagram post
- **THEN** export is 1080×1080 (StoryArt users asked for 1:1; catalog must include it)

### Requirement: Destination catalog (InstaSize / Social Post Maker sizes)

The app SHALL ship this catalog (pixels exact):

| Id | Label (en concept) | Pixels | Absorbed from |
| --- | --- | --- | --- |
| `yt_thumb` | YouTube thumbnail | 1280×720 | Thumbnail Maker, Thumbnails4U |
| `yt_shorts` | YouTube Shorts | 1080×1920 | Thumbnail Maker Shorts claim |
| `ig_post` | Instagram post | 1080×1080 | InstaSize / StoryArt request |
| `ig_story` | Instagram story | 1080×1920 | StoryArt / Unfold |
| `tiktok_cover` | TikTok cover | 1080×1920 | vertical shelf |
| `wa_status` | WhatsApp status | 1080×1920 | StoryArt WhatsApp status claim |

Optional safe-zone overlay toggle (`Safe zone`) MAY preview network UI margins; it SHALL NOT bake into export unless the user places content under it.

#### Scenario: Owner networks present

- **WHEN** Destinations is shown
- **THEN** YouTube, Instagram, TikTok and WhatsApp entries exist covering the rows above

### Requirement: Templates are editable starters

Each destination SHALL provide a non-empty offline template set. Opening a template SHALL create editable layers (photo slots + text), not a flat locked image. The user SHALL be able to **replace the template photo** (Social Media Post Maker complaint) and edit text. Free users get a subset sufficient for happy-path Export; unlock expands the set — without stripping templates already used in an owned project.

#### Scenario: Replace template photo

- **WHEN** a template has a photo slot and the user picks a new Photo
- **THEN** that slot updates and remains editable

#### Scenario: Free path still exports

- **WHEN** unlock is false
- **THEN** at least one template or blank+Photo path still reaches Export

### Requirement: Editor tools — familiar maker bar only

The Editor SHALL provide these tools on the primary tool row (order MAY match common makers: Photo / Text / Stickers / Crop; Layers and Safe zone as secondary):

| Tool | Behavior | Absorbed from |
| --- | --- | --- |
| Photo | System Photo Picker; optional camera | Thumbnails4U, Unfold complaint→system picker |
| Text | Add/edit; font; fill; stroke/border; free scale/rotate | Thumbnail Maker, InstaSize text border |
| Stickers | YT callouts (arrows/ribbons) + story frames | Thumbnail Maker + StoryArt |
| Crop | Crop/reframe background to canvas | Thumbnails4U, InstaSize |
| Layers | Select, reorder, delete | standard makers |

Gestures: drag, pinch-scale, rotate on the selected layer. Text boxes SHALL scale freely (forbid Social Media Post Maker “tiny locked text box”).

**v1 SHALL NOT** put on the primary bar: Paint, Filters, Video, AI remove background, Music, Schedule, Feed planner, Bio site (matrix DEFER/REFUSE).

#### Scenario: System Photo Picker

- **WHEN** the user taps Photo
- **THEN** the system Photo Picker opens (not an app-private gallery that hides albums)

#### Scenario: Text scales

- **WHEN** the user pinches a text layer
- **THEN** the text scales and exports at that size

#### Scenario: No AI tool on the bar

- **WHEN** the Editor primary tool row is listed
- **THEN** it contains no generative-AI or background-remove-AI control

### Requirement: Export / Share / Save — sharp native PNG

Export SHALL render the layer tree to a bitmap at the **exact catalog pixels** and write PNG. Export SHALL NOT use screenshot/`PixelCopy` of the Compose preview as the only path (Thumbnail Maker blur defect).  

- Free: MAY apply watermark.  
- Unlocked: no watermark.  
After render: **Share** (system sheet) and **Save** (gallery or `ACTION_CREATE_DOCUMENT`) — nouns from all makers’ save/share claims.

#### Scenario: Dimensions exact

- **WHEN** exporting `yt_thumb`
- **THEN** PNG is 1280×720

#### Scenario: No mid-gesture ads

- **WHEN** the user is dragging/pinching on the canvas
- **THEN** no full-screen interstitial appears (Thumbnail Maker praise: ads not while clicking)

### Requirement: Multi-size pack (wedge; after first success)

Unlocked users SHALL open **Sizes** to select additional catalog ids and Export a pack of PNGs from the same layer tree with non-stretching fit (InstaSize-class). Single-size Export remains the default happy path; pack SHALL NOT be required first.

#### Scenario: Pack from one project

- **WHEN** unlocked user selects `yt_thumb` + `ig_post` and exports a pack
- **THEN** two PNGs at 1280×720 and 1080×1080 are produced

### Requirement: Projects durable (fix “lose work”)

Projects SHALL autosave layer trees + asset refs to durable storage. Projects list SHALL support open, **duplicate** (Social Media Post Maker), delete. Process kill SHALL restore the open project. Leaving Editor with unsaved destructive replace SHALL confirm. The app SHALL NOT tell users to screenshot as the keep-work method.

#### Scenario: Kill mid-edit

- **WHEN** two layers exist and the process is killed
- **THEN** reopen restores both layers

#### Scenario: Duplicate

- **WHEN** the user duplicates a project
- **THEN** a second editable copy appears without destroying the original

### Requirement: Brand kit optional (Unfold Pro, local)

Brand kit SHALL store: logo image, up to four colors, two font choices. New Text/logo layers default to kit values when set. Brand kit SHALL NOT block happy path. No account/sync.

#### Scenario: Export with empty Brand

- **WHEN** Brand is unset
- **THEN** happy-path Export still succeeds

### Requirement: One-time unlock — never subscription-to-save

Play Billing **non-consumable** unlock ($9.99 per economics) SHALL enable: watermark-free Export, Sizes pack, full templates, Brand kit gates if any. Draft **Save** into Projects SHALL work while locked. **No subscription SKU.** Restore purchase in Settings. Unlock MUST apply in the same session after purchase confirmation (StoryArt/Unfold activation failures).

#### Scenario: Draft free

- **WHEN** unlock is false and user leaves Editor
- **THEN** project remains in Projects

#### Scenario: No subscription

- **WHEN** purchase products are listed
- **THEN** only the one-time unlock is offered

### Requirement: No account, no AI key, offline create

Create/edit/save/export (watermarked) SHALL work in airplane mode. No login/sign-up UI. No paid generative AI SDK or key in the client (`check-client-secrets`).

#### Scenario: Airplane export

- **WHEN** offline and user completes free Export
- **THEN** PNG is produced without network error

### Requirement: Explicit refuse list (so development cannot “add later” quietly)

The following SHALL NOT ship in v1 (matrix REFUSE/DEFER). Adding any requires a new OpenSpec delta and owner approval:

- Subscription or forced free-trial paywall  
- Mid-edit full-screen ads  
- AI generation / AI background remove  
- Video/Reels timeline, animated template playback as core  
- Instagram feed planner / social login  
- Link-in-bio / Bio site  
- Post scheduler / content calendar  
- Invented primary tools beyond the Editor table  
- Invented Home marketing copy blocks instead of Destinations  

#### Scenario: Refuse list audit

- **WHEN** the shipped primary Editor tools and Home are compared to this requirement
- **THEN** none of the refuse-list items appear as primary chrome

### Requirement: Complaint absorption checklist

Shipping SHALL treat these measured failures as bugs (each row is a FAIL if reproduced on device):

1. Blurry export from preview capture  
2. Projects inaccessible after save  
3. Paywall only to save draft  
4. Purchase confirmed but unlock inactive  
5. System gallery/albums unreachable (non-Picker)  
6. Text cannot be scaled  
7. Template photo cannot be replaced  

#### Scenario: Checklist is regression criteria

- **WHEN** phase-1 device verification runs the happy path and unlock restore
- **THEN** none of the seven complaint rows above is observed; any one observed blocks checkpoint 1.5

## Competitor → Coverkit map (summary)

| Leader strength | Coverkit |
| --- | --- |
| Thumbnails4U type→photo→text/sticker→save | Happy path |
| Thumbnail Maker fonts/stickers/shapes + multi-cover claim | Editor + catalog |
| StoryArt/Unfold templates + perfect vertical size | Story destinations + templates |
| InstaSize fit/crop per network | Crop + catalog pixels |
| Social Media Post Maker network-first + drafts/duplicate | Home + Projects |
| Unfold Pro brand colors/logo/fonts | Brand kit offline |
| Canva multi-format idea | Sizes pack (no cloud) |
| StoryArt “lifetime one-time” ask | Unlock, not sub |
