# Coverkit UI rewrite v2 — libraries, not emoji chrome (2026-10-21 evening)

**Owner rejection (MEASURED on device):** the “redesign” is still unusable.
Screenshot `.kiln/scratch/device-coverkit/broken-now.png`: bottom bar **overlaps system
nav**, destination grid is a broken masonry of mismatched aspect cards, previews are
fake colour blocks, editor chrome is **Unicode glyphs** (`‹` `↗` `▣` `✦`) instead of
icons, and the canvas is a hand-rolled Box stack — not an editor.

**Rule this rewrite obeys:** do not invent photo-editor behaviour. Use a maintained
library for the canvas job; vendor real Tabler icons; Compose owns shell only.

## Stack (ABSORB)

| Job | Package | Why |
| --- | --- | --- |
| Edit canvas (text / sticker / pinch / export bitmap) | `com.burhanrashid52:photoeditor:3.1.1` (Maven Central) | Mature stories-style editor; wrap `PhotoEditorView` in `AndroidView` |
| Load gallery bitmaps into the editor | Coil Compose (catalog / latest stable) | Same path Plinth already uses for photos |
| Crop to destination ratio before edit | `com.github.yalantis:ucrop:2.2.10` **or** PhotoEditor’s own scale + destination letterbox on export | Do not hand-roll crop maths |
| UI glyphs | Tabler Icons v3.48 (already MIT in slot) — **many** outline icons, not one `info-circle` | visual-identity.mdc |

Keep Coverkit domain for destinations catalog, projects store, brand kit, unlock SKU,
and **ExportRenderer only as a pack/multi-size path** that takes the PhotoEditor bitmap
and re-rasterises into each selected destination size. Single-size export can use
PhotoEditor’s save directly.

## Screen contracts

### Destinations
- **Uniform 2-column grid cells** of equal height (not masonry of 16:9 vs 9:16 cards).
- Inside each cell: **letterboxed** aspect preview (frame in correct ratio, empty
  margins), caption under.
- Bottom bar: `NavigationBar` / custom bar with `WindowInsets.navigationBars` padding
  so labels never sit under system buttons.
- Icons: Tabler `layout-grid` / `folder` / `palette` / `settings` — tinted Cyan.

### Templates
- Rendered thumbs via Coil or Canvas from template seed; blank tile uses Tabler photo
  icon. No text-only cards.

### Editor (product)
- Full-screen `PhotoEditorView` (dark).
- Top: back icon, Export (cyan), Share, Save — **Icon + optional short label**, never
  emoji Text.
- Bottom: PhotoEditor’s own brush/text/emoji/sticker tools **or** a thin Coverkit rail
  that calls `photoEditor.addText` / `addImage` / `addEmoji` APIs.
- Unlock: watermark overlay drawn after capture when locked; same SKU gate.

### Projects
- Thumbnail grid from saved PNG cache or last export preview; overflow menu with
  Tabler icons.

## Banned
- Emoji / symbol characters as toolbar “icons”.
- Hand-built transform canvas that duplicates PhotoEditor.
- Destination cards whose height is raw aspect ratio of the network (masonry mess).
- Claiming DONE / checkpoint without a clean A40 screenshot that does not overlap
  system UI.

## Proof
A40: home bottom bar clear of system nav; destination cells aligned; open template;
editor shows PhotoEditor chrome; add text via library; Export writes PNG; Share opens
chooser. If any primary control is a Unicode glyph Text, FAIL.
