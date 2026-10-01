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

Use `en-US` as the default locale and `ru-RU` when Russian is supported. Each locale has exactly six 1080×1920 phone screenshots, four 1200×1920 7-inch tablet screenshots, and four 1600×2560 10-inch tablet screenshots. The app icon is 512×512, 32-bit sRGB PNG with alpha, no drawn mask or shadow, and no larger than 1024 KB. The feature graphic is exactly 1024×500 and is either JPEG or 24-bit PNG without alpha. Screenshots are JPEG or 24-bit PNG without alpha and no larger than 8 MB each.

Listing titles are at most 30 characters, short descriptions at most 80 characters, and full descriptions at most 4000 characters. All copy, declarations, and screenshots must describe the shipped app; identify any synthetic captures plainly and provide usable review access when content is login-gated.

For this slot, the six phone screenshots are real captures from a physical device, tied to the
signed release build by `store/provenance.json`. The 7-inch and 10-inch tablet sets are composed
from those phone captures because no tablet device was attached; they are phone-framed assets, not
tablet captures.
