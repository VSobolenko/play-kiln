# Store graphics — Lumintra: Light Planner

The asset inventory the release render must produce, with the exact canvas and the Play cap for each.
Every number is the value in `scripts/lib/play-assets.json` (measured from the RU Play Console on
2026-09-29), and `python scripts/store/validate.py --slot lumintra-light-planner` measures the shipped
files against it. The identity the images are rendered from is `store/studio.json` — the mark
`mark-lumintra`, the field `#0B0E14`, the accent `#FF8F6B` — and the icon and adaptive-icon vectors are
rendered from that same declaration, so the pack cannot drift from the app.

> This file is an inventory, not a substitute for capture. Real device screenshots come from
> `node scripts/media-capture.mjs --slot lumintra-light-planner` in phase 2, after the owner's recorded
> release approval. A mocked-up frame around a plain screen is a false claim, not a marketing choice.

## Required assets

| Asset | Count | Exact canvas | Format | Cap |
|---|---|---|---|---|
| App icon | 1 (+1 per locale) | **512×512** | 32-bit PNG **with alpha**, sRGB | ≤ 1024 KB |
| Feature graphic | 1 (+1 per locale) | **exactly 1024×500** | JPEG or 24-bit PNG **without alpha** | ≤ 15 MB |
| Phone screenshots | **6** per locale | **1080×1920** (9:16) | JPEG or 24-bit PNG without alpha | ≤ 8 MB each |
| 7-inch tablet screenshots | **4** per locale | **1080×1920** (9:16) | JPEG or 24-bit PNG without alpha | ≤ 8 MB each |
| 10-inch tablet screenshots | **4** per locale | **1440×2560** (9:16) | JPEG or 24-bit PNG without alpha | ≤ 8 MB each |

**Every screenshot set is 9:16.** The console states the aspect in **every** screenshot field
("Соотношение сторон изображения должно составлять 16:9 или 9:16"), so phone and 7-inch ship at
`1080×1920` and 10-inch at `1440×2560` — inside both the 9:16 ratio and each field's own size window
(phone/7-inch sides 320–3840, 10-inch sides 1080–7680). The counts are the console's `ship` values:
6 phone (min 4, max 8) and 4 for each tablet field (min 4, max 8).

## Deliberately NOT produced

- **Desktop** (`Скриншоты на компьютере`) — **not produced.** The field carries no required marker (it
  is optional) and the owner's instruction of 2026-09-29 is that the pack does not fill it.
- **Android XR** (`Скриншоты для Android XR`) — **not produced**, same reason.

No target, no asset and no check exists for either, and adding one is a product decision rather than a
completeness fix.

## Locales

Locale sets are produced for the pack's declared locales:

- `en-US` (default)
- `ru-RU`

Each locale carries its own icon, feature graphic, 6 phone shots, 4 seven-inch shots and 4 ten-inch
shots, under `store/assets/<locale>/`. The captions rendered into the shots are the locale's own
`screenshotCaptions` from `store/listing.json`, in the recorded order:

| # | en-US | ru-RU |
|---|---|---|
| 1 | Tonight, read in one glance | Вся ночь с одного взгляда |
| 2 | When the sky is truly dark | Когда небо по-настоящему тёмное |
| 3 | Golden and blue hour, to the minute | Золотой и синий час до минуты |
| 4 | Plan the shot before you leave | Спланируйте кадр заранее |
| 5 | Satellite passes you can catch | Пролёты спутников, которые успеть |
| 6 | Red mode keeps your night eyes | Красный режим бережёт глаза |

## Layout, per the pack contract

```text
store/assets/icon-512.png
store/assets/feature-graphic-1024x500.png
store/assets/<locale>/icon-512.png
store/assets/<locale>/feature-graphic-1024x500.png
store/assets/<locale>/phone-01.png ... phone-06.png
store/assets/<locale>/tablet-7-01.png ... tablet-7-04.png
store/assets/<locale>/tablet-10-01.png ... tablet-10-04.png
store/provenance.json
```

## Icon rules (Play rejects the alternatives)

- 512×512, **32-bit PNG with alpha**, sRGB, ≤ 1024 KB.
- **No drawn rounded mask and no baked-in shadow** — the launcher and Play apply their own. The icon is
  rendered from `studio.json`'s `mark`, read at 48 dp, and must survive the adaptive-icon masks
  (72 dp circle, squircle, 66 dp safe zone). `python scripts/check-adaptive-icon.py` measures the fit.
- The mark is **drawn, not borrowed**: no stock library glyph, no emoji, no clipart as the identity.

## Non-degeneracy (why a flat image is refused)

`scripts/lib/play-assets.json` records `minLuminanceStdDev: 6` and `minScreenshotColours: 12`,
calibrated 2026-09-25 against 86 committed assets. A solid-colour or near-flat asset measures far below
this and is refused — the shots must show the real scene, not a placeholder fill.

## Provenance

`store/provenance.json` is written by `media-capture.mjs` and is committed beside the assets (unlike
`captures/` and `dist/`, which are not). It binds each committed image to the recorded build; editing an
image without a fresh capture fails `python scripts/store/validate.py`, because every asset must hash to
what the record says.

## Human check before upload

`python scripts/visual-qa.py --slot lumintra-light-planner` reports pixel measurements — palette against
the app's own Kotlin palette, caption ink and edge clipping, WCAG contrast, pairwise similarity of the
six phone shots, adaptive-mask clipping, and caption legibility at 360/180/90 px. **It is a report, not a
gate**: it exits 0 whenever it finishes, so read it rather than treating a clean run as approval. The
judgement that the pack is beautiful stays with the owner.
