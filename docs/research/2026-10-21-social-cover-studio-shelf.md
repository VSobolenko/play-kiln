# Social cover / thumbnail studio shelf — measured 2026-10-21

Owner direction: expand YouTube thumbnails into a **multi-platform cover studio**
(YouTube, Instagram, TikTok, WhatsApp, …), no AI provider keys, substantial product.

Query set: `.kiln/scratch/forge-2026-10-21-social-covers.json`.  
Storefront: signed-out Play `hl=en`, `gl=US`.

## Ratings screen

**MEASURED** — `node scripts/scan-market.mjs --query-set .kiln/scratch/forge-2026-10-21-social-covers.json`:

| query | verdict | notes |
| --- | --- | --- |
| youtube thumbnail maker | mixed | money shelf; weak AI mid-tier |
| tiktok cover maker | mixed | polluted by official TikTok apps |
| instagram post maker size | mixed | partial relevance; InstaSize / Canva / resizers |
| social media post maker templates | mixed | Canva, Unfold, 1M+ post makers |
| story template maker instagram | mixed | Graphionica / Unfold / Storybeat / StoryLab / StoryArt all **10M+** |
| thumbnail maker all platforms | mixed | still YouTube-shaped cards |
| whatsapp status maker | fortress | strong incumbents |
| canva alternative android | mixed / POLLUTED | do not trust as Canva-competitor count |

## Money on display (enrich)

**MEASURED** — `node scripts/enrich-market.mjs` (six queries, top 8 each):

| Shelf | Standouts | installs | ads | IAP |
| --- | --- | --- | --- | --- |
| YouTube thumbnails | Thumbnail Maker - Channel art | **10M+** | yes | yes |
| | Thumbnails4U, Ultimate Thumbnail Maker | **5M+** | yes | yes |
| Stories / Reels | Graphionica, Unfold, Storybeat, StoryLab, StoryArt | **10M+** each | mix | **all IAP** |
| | InStories | **5M+** | no | yes |
| Social posts | Canva | **500M+** | no | yes |
| | Social Media Post Maker (DesignWiz) | **1M+** | yes | yes |
| | Desygner Social Post | **1M+** | yes | yes |
| | Unfold | **10M+** | no | yes |
| Size / crop | InstaSize | **50M+** | no | yes |
| | Photo resizers | **10M+** | yes | yes |

Largest floors seen this run: **500M+** (Canva), **50M+** (InstaSize), **10M+** (thumbnail + story makers). Monetization pattern on mid-tier: **ads + IAP**; story/Canva class often **IAP without ads**.

## Competitor strengths to absorb (product synthesis)

| Incumbent class | What they do well | What we take |
| --- | --- | --- |
| YouTube Thumbnail Maker (10M+) | Stickers, arrows, bold text, channel-art mindset | YouTube / Shorts size presets + creator sticker pack |
| StoryArt / StoryLab / Graphionica / Unfold (10M+) | Vertical story beauty, templates, reels framing | Story / Reels / TikTok vertical canvases + template rows |
| InstaSize (50M+) | Correct social sizes without wrecking the photo | First-class size catalog + safe crop (no stretch) |
| Canva (500M+) | Brand kit, multi-format, templates | **Offline** brand kit (colors, fonts, logo) + one design → many sizes |
| Social Media Post Maker (1M+) | Explicit multi-network templates | Network picker as the home, not a buried setting |
| Weak thumbnail apps (2.1–3.5★) | — | Negative: blurry export, broken save, subscription to save |

## Shared complaints (vocabulary sample)

**MEASURED** — `node scripts/scan-complaints.mjs` on youtube thumbnail / social post / story shelves:

Recurring themes across pockets:

1. **Subscription / paywall to save or remove watermark** (thumbnail Banner Studio; story Unfold/InStories).
2. **Blurry / distorted export** despite sharp source photos (Thumbnail Maker - Channel art).
3. **Projects inaccessible after save** / forced screenshots (Thumbnail Maker for YouTubers 2.1★).
4. **Pro templates lock old work** after update (story makers).
5. **Payment confirmed but Pro not activated** (story makers).

## Honest limits

- TikTok-named search is polluted by official TikTok clients; dedicated cover-maker demand is inferred from album/cover makers + vertical story apps, not from those official cards.
- Canva is a fortress of capability; the pocket is **not** “beat Canva”, it is **faster offline multi-size cover pack for creators who already bounce between YT/IG/TikTok/WA**.
- No AI provider keys in v1 — templates + local canvas only.
