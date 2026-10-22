# Coverkit competitor matrix — Play listing synthesis (2026-10-21)

**Purpose:** Normative input for `openspec/changes/app-coverkit`. Every UI noun and feature
decision below is taken from measured shelf leaders or measured complaints — not invented.

Storefront reads: signed-out Play pages (`hl=en`, `gl=US`) for the packages named. Install/IAP
floors also from `node scripts/enrich-market.mjs` the same day.

## Leaders studied

| Package | Title | Installs (enrich) | Rating | Money |
| --- | --- | --- | --- | --- |
| `com.covermaker.thumbnail.maker` | Thumbnail Maker - Channel art | 10M+ | 4.2 | ads+IAP |
| `com.cutewallpaperstudio.thumbnail.maker.thumbnail.creator` | Thumbnails4U | 5M+ | 4.8 | ads+IAP |
| `com.sk.thumbnailmaker` | Ultimate Thumbnail Maker | 5M+ | 4.6 | ads+IAP |
| `com.ryzenrise.storyart` | StoryArt | 10M+ | 4.6 | ads+IAP |
| `com.moonlab.unfold` | Unfold | 10M+ | 3.8 | IAP (sub) |
| `com.jsdev.instasize` | InstaSize | 50M+ | 4.7 | IAP (sub) |
| `com.bg.socialcardmaker` | Social Media Post Maker | 1M+ | 4.8 | ads+IAP |
| `com.canva.editor` | Canva | 500M+ | 4.8 | IAP |

## Why they “took off” (ON RECORD from their own About + install floors)

| App | What they claim as the job | Why volume exists (INFERRED from claim + installs) |
| --- | --- | --- |
| Thumbnail Maker / Thumbnails4U / Ultimate | Minutes to a YT thumbnail/banner; templates; text; stickers; save/share | Creators need a clickable cover every upload; path is short |
| StoryArt / Unfold | Story/Reels templates; perfect IG size; fonts; one-tap share | Vertical story is a daily habit; templates remove blank-canvas fear |
| InstaSize | Resize/crop to fit each network without wrong crop | Size anxiety is universal; they own “fit the platform” |
| Social Media Post Maker | Pick Instagram/Facebook/Pinterest/YouTube/LinkedIn templates first | Network-first IA matches how marketers think |
| Canva | Brand kit + multi-format + huge template library | Consistency across formats; we take **brand kit idea offline only** |

## Feature claims → Coverkit decision

| Feature (claimed by ≥1 leader) | Coverkit v1 |
| --- | --- |
| Choose **type/size first** (Thumbnails4U: Thumbnail vs Banner; Social Post Maker: per-network) | **ABSORB** — home = destination list |
| Templates then edit (all makers) | **ABSORB** |
| Blank + photo from gallery/camera (Thumbnails4U how-to) | **ABSORB** — Photo Picker + optional camera |
| Crop to size (Thumbnails4U, InstaSize) | **ABSORB** — crop on place / before edit |
| Text + fonts + color + stroke/border (Thumbnail Maker, InstaSize, StoryArt) | **ABSORB** |
| Stickers / shapes / overlays (Thumbnail Maker, Ultimate) | **ABSORB** |
| Backgrounds / template backgrounds | **ABSORB** via templates |
| Save + Share (all) | **ABSORB** |
| Drafts / duplicate draft (Social Media Post Maker praise) | **ABSORB** — projects + duplicate |
| Multi-platform sizes in one app (Thumbnail Maker About lists FB/IG/YT/Twitter/LinkedIn; Social Post Maker) | **ABSORB** — YT/IG/TikTok/WA catalog; pack export is our wedge |
| Brand colors / logo / fonts (Unfold Pro) | **ABSORB offline** — Brand kit, no account |
| Watch ad to remove watermark (Thumbnail Maker review reply) | **REFUSE mid-edit ads**; free watermark OK; unlock removes it |
| Subscription / Unfold+ / Instasize Premium | **REFUSE** — one-time unlock (StoryArt users asked for lifetime) |
| Video / Reels timeline / animated templates (StoryArt, Unfold) | **DEFER** — still PNG covers only in v1 |
| Filters / glitter / film packs (StoryArt, InstaSize) | **DEFER** — not required for first cover |
| Paint / draw (Thumbnails4U) | **DEFER** |
| AI background remove (Unfold) | **REFUSE** v1 — no AI keys |
| Feed planner / IG login (Unfold) | **REFUSE** |
| Bio / link-in-bio site (Unfold) | **REFUSE** |
| Post scheduler / calendar (Social Media Post Maker) | **REFUSE** |
| Poster/flyer/print (Social Media Post Maker) | **DEFER** |
| Highlight-cover icon packs (StoryArt) | **DEFER** — story canvas covered; icon packs later |

## Praise vocabulary to keep (MEASURED reviews / About)

- “Easy to work with” / “comfortable immediately” (Thumbnail Maker)
- Ads “don’t interrupt your work… never while I’m clicking” (Thumbnail Maker) → **no interstitial during canvas gestures**
- “Advanced but simple” / make your own without templates (Thumbnails4U)
- “Saves a draft” / “duplicate to edit” (Social Media Post Maker)
- Perfect-fit size so upload without re-crop (StoryArt guarantee)
- Lifetime one-time payment asked explicitly (StoryArt review 2020)

## Complaint vocabulary that is a shipping defect if we repeat it

| Complaint | Source | Spec rule |
| --- | --- | --- |
| Blurry/distorted export | Thumbnail Maker 3★ | Native-resolution render, not preview capture |
| Projects inaccessible / lose work if leave | Ultimate / Thumbnails4U / 2.1★ maker | Durable projects; warn before discard |
| Subscription / Pro to save or watermark forever | Banner Studio, Unfold, StoryArt | Free draft save; unlock for watermark-free + pack |
| Paid but Pro not activated | StoryArt, Unfold | Restore purchases + same-session unlock |
| Tiny text / can’t scale text box | Social Media Post Maker | Pinch/scale text freely |
| Can’t replace template image | Social Media Post Maker | Replace photo on template layers |
| Predatory subscribe walls | InstaSize | No forced trial; free happy path |
| Gallery picker hard / not system picker | Unfold | System Photo Picker |
| Want 1:1 posts not only stories | StoryArt | IG post 1080×1080 in catalog |

## Control nouns allowed on primary chrome (from shelf language)

Use these resource labels (translate; do not invent synonyms on the primary bar):

`Templates` · `Photo` · `Text` · `Stickers` · `Layers` · `Crop` · `Export` · `Share` · `Save` · `Brand` · `Sizes` · `Projects`

Secondary / settings only: `Settings` · `Restore purchase` · `Safe zone`
