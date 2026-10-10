"""Bake ``app/src/main/assets/sky/milky_way_eso0932a.webp`` from the ESO panorama eso0932a.

Source
------
ESO, *The Milky Way panorama*, Serge Brunier, ``eso0932a``,
``https://www.eso.org/public/images/eso0932a/`` — downloaded from the ESO CDN at
``https://cdn.eso.org/images/large/eso0932a.jpg``.  Licence **CC BY 4.0**; the credit
**`ESO/S. Brunier`** must appear *beside the image*, on the scene and on the shared postcard,
with the licence link and with no implied ESO endorsement (design.md § "Sky assets and attribution").

Transformation
--------------
Decoded, converted to sRGB, resized so the width is at most 4096 and the height keeps the
source's 2:1 equirectangular ratio (never upscaled), encoded as WebP.  The bake is **not**
AI-modified: nothing is painted, in-painted, extended or synthesised — the modification is the
resize and the WebP re-encode, and that is declared with the image.

    python tools/bake-milkyway.py [--refresh] [--max-width 4096] [--quality 78]
"""
from __future__ import annotations

import argparse
import io
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import common  # noqa: E402

from PIL import Image  # noqa: E402

SOURCE_URL = "https://cdn.eso.org/images/large/eso0932a.jpg"
CREDIT = "ESO/S. Brunier"
LICENCE = "CC BY 4.0"
LICENCE_URL = "https://www.eso.org/public/images/eso0932a/"
OUT = "sky/milky_way_eso0932a.webp"


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--refresh", action="store_true")
    ap.add_argument("--max-width", type=int, default=4096)
    ap.add_argument("--quality", type=int, default=78)
    args = ap.parse_args()

    raw = common.fetch(SOURCE_URL, suffix=".jpg", refresh=args.refresh, timeout=600)
    im = Image.open(io.BytesIO(raw))
    im.load()
    src_size = im.size
    im = im.convert("RGB")

    max_w = min(args.max_width, im.width)
    if im.width != max_w:
        im = im.resize((max_w, max(1, round(im.height * max_w / im.width))), Image.LANCZOS)

    buf = io.BytesIO()
    im.save(buf, format="WEBP", quality=args.quality, method=6)
    blob = buf.getvalue()
    if len(blob) >= 4 * 1024 * 1024:
        raise SystemExit(f"milky-way webp is {len(blob)} B, over the 4 MB budget")

    out = common.ASSETS / OUT
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(blob)

    common.write_json(common.SCRATCH / "milkyway-bake-report.json", {
        "source": SOURCE_URL,
        "credit": CREDIT,
        "licence": LICENCE,
        "licenceUrl": LICENCE_URL,
        "sourceBytes": len(raw),
        "sourceSize": list(src_size),
        "output": str(out),
        "outputSize": list(im.size),
        "outputBytes": len(blob),
        "sha256": common.sha256_bytes(blob),
        "quality": args.quality,
        "aiModified": False,
        "modification": "resized to 4096 wide and re-encoded as WebP; no pixel content changed",
    })
    print(f"wrote {out}  {im.size[0]}x{im.size[1]}  {len(blob)} B  "
          f"sha256 {common.sha256_bytes(blob)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
