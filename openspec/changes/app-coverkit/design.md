# Design: Coverkit

## Context

Creators need sharp social covers for YouTube, Instagram, TikTok and WhatsApp. Play leaders already teach the vocabulary: **pick size → template or photo → text/stickers → save/share**. Coverkit ships that vocabulary offline with a **one-time unlock** and a **multi-size PNG pack** wedge — not a new chrome invention.

**Normative competitor assembly:** `docs/research/2026-10-21-coverkit-competitor-matrix.md`  
**Behavior contract:** `specs/social-cover-studio/spec.md`  
**Do not invent** screens, tools or control nouns outside those two documents.

## Goals / Non-Goals

**Goals:**

- Happy path in three beats: Destination → Templates/Photo → Export (Thumbnails4U shape).
- Exact pixels per destination; native PNG render (not Compose preview screenshot).
- System Photo Picker; free text scale; replaceable template photos.
- Durable Projects with duplicate; optional Brand kit; unlocked Sizes pack.
- Purchase-led unlock; draft Save free; no subscription SKU; no AI key.

**Non-Goals (v1 refuse — see matrix REFUSE/DEFER):**

- Canva-class cloud account, AI, video timelines, feed planner, bio site, scheduler.
- Mid-edit interstitial ads; subscription-to-save; app-private gallery.
- Primary tools: Paint, Filters, Music, AI remove-bg.
- Marketing hub instead of Destinations.

## Decisions

### 1. Screen map (only these)

| Screen | Role | Pattern source |
| --- | --- | --- |
| Home / Destinations | Network size cards | Social Media Post Maker; Thumbnails4U “select image type” |
| Templates | Grid for current destination | All makers |
| Editor | Canvas + tool row | Thumbnail Maker / Ultimate |
| Projects | Open / duplicate / delete | Social Media Post Maker |
| Brand | Logo + colors + fonts (local) | Unfold Pro, offline |
| Settings | Locale, theme, restore, about | Factory baseline |

Entry: Destinations. Brand and Sizes are **after** first success.

### 2. Control nouns (string keys must match product nouns)

`Templates`, `Photo`, `Text`, `Stickers`, `Crop`, `Layers`, `Export`, `Share`, `Save`, `Brand`, `Sizes`, `Projects`, `Safe zone` — from matrix. Do not rename to clever synonyms in v1.

### 3. Editor tool row

Primary: **Photo | Text | Stickers | Crop**  
Secondary: Layers, Safe zone  
Gestures: drag / pinch / rotate  
Text: fill + stroke (InstaSize “text border” praise)

### 4. Destination catalog

| Id | Pixels |
| --- | --- |
| yt_thumb | 1280×720 |
| yt_shorts | 1080×1920 |
| ig_post | 1080×1080 |
| ig_story | 1080×1920 |
| tiktok_cover | 1080×1920 |
| wa_status | 1080×1920 |

### 5. Export pipeline

Render layer tree → bitmap at catalog size → PNG → Share / Save. Watermark only when locked. Never PixelCopy-of-preview as sole path.

### 6. Monetization UX

Paywall on watermark-free Export, Sizes pack, locked templates/Brand gates — **not** on draft Save. One SKU. Restore in Settings. Unlock same session.

### 7. Visual identity

Slot-owned `store/studio.json` + `Color.kt` (factory visual-identity). Distinct from Plinth. Remix icons for chrome; mark from studio.

### 8. Architecture sketch

```
ui/ (Destinations, Templates, Editor, Projects, Brand, Settings)
domain/ (Project, Layer, DestinationCatalog, BrandKit, ExportRenderer)
data/ (ProjectsStore, BrandStore, Preferences, Billing)
```

Offline-first; Billing only for unlock.

## Risks / Trade-offs

| Risk | Mitigation |
| --- | --- |
| Builder invents tools | Spec refuse list + matrix ABSORB only |
| Export blur | Native renderer + QA dimensions |
| Unlock lag | Same-session apply; restore path |
| Scope creep to Canva | Refuse list in spec; OpenSpec delta required to add |

## Migration Plan

Greenfield. No migration.

## Open Questions

None blocking scaffold. Deferred product adds (filters, paint) need a new change + owner words.
