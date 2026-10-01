# Store pack

Run `scripts/store/render.py` from the PlayKiln repository root to generate the complete Google Play store pack for a slot, and `scripts/store/validate.py` to validate it. Agents edit `store/listing.json` as the single source of truth; generated listing Markdown and graphics metadata must not become competing sources.

The finished pack contains:

```text
store/listing.json
store/listing.md
store/privacy.md
store/data-safety.md
store/review-notes.md
store/graphics.md
store/checklist.md
store/assets/icon-512.png
store/assets/feature-graphic-1024x500.png
store/assets/<locale>/icon-512.png
store/assets/<locale>/feature-graphic-1024x500.png
store/assets/<locale>/phone-01.png ... phone-06.png
store/assets/<locale>/tablet-7-01.png ... tablet-7-04.png
store/assets/<locale>/tablet-10-01.png ... tablet-10-04.png
```

Use `en-US` as the default locale and `ru-RU` when Russian is supported. Every screenshot set is 9:16, because that is the aspect the Play Console states in **every** screenshot field ("Соотношение сторон изображения должно составлять 16:9 или 9:16", measured live in the RU console on 2026-09-29): six 1080×1920 phone screenshots, four 1080×1920 7-inch tablet screenshots, and four 1440×2560 10-inch tablet screenshots. The app icon is 512×512, 32-bit sRGB PNG with alpha, no drawn mask or shadow, and no larger than 1024 KB. The feature graphic is exactly 1024×500 and is either JPEG or 24-bit PNG without alpha, no larger than 15 MB. Screenshots are JPEG or 24-bit PNG without alpha and no larger than 8 MB each.

The console's `Скриншоты на компьютере` (Desktop) and `Скриншоты для Android XR` fields are optional — they carry no required marker — and the pack deliberately does **not** produce them. That is the owner's instruction of 2026-09-29, not an omission.

Listing titles are at most 30 characters, short descriptions at most 80 characters, and full descriptions at most 4000 characters. All copy, declarations, and screenshots must describe the shipped app; mark synthetic captures as synthetic and provide usable review access when content is login-gated.
