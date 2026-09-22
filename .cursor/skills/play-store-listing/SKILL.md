---
name: play-store-listing
description: Produce Google Play listing assets and copy for a PlayKiln slot (title, descriptions, privacy, icons, screenshots). Use during /ship Store role or when preparing publish.
---

# Play Store listing pack

Create `apps/<slot>/store/`:

| File | Content |
|------|---------|
| `listing.md` | App title (≤30), short description (≤80), full description, category, tags |
| `privacy.md` | Privacy policy text (data collected, permissions, contact) |
| `graphics.md` | Specs for icon 512, feature graphic 1024×500, phone screenshots |
| `assets/` | Generated images (or prompts + regenerated binaries) |
| `checklist.md` | Pre-upload checklist (content rating, target audience, etc.) |

## Rules

- No trademarked third-party brand names in titles unless the human owns them
- Descriptions must match actual app behavior (no vaporware claims)
- Prefer Codex peer for image generation; Cursor peer for accuracy vs code
- Locales: start with `en`; add `ru` when the idea is Russian-first
