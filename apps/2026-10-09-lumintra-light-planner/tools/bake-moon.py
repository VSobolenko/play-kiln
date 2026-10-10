"""Bake ``app/src/main/assets/sky/moon_svs.webp`` from the NASA SVS Moon Kit.

Source
------
NASA Scientific Visualization Studio, *CGI Moon Kit* (SVS 4720),
``https://svs.gsfc.nasa.gov/4720/`` → ``lroc_color_poles_2k.tif`` (2048x1024 equirectangular,
LRO/LROC colour mosaic).  NASA SVS imagery is in the **public domain**; the obligation is an
acknowledgement and **no agency insignia**, and it is **never** credited to JPL-Caltech
(design.md § "Sky assets and attribution").

Transformation
--------------
Decoded, converted to sRGB, resized to a 2:1 equirectangular canvas no larger than 2048x1024
(never upscaled), encoded as lossy WebP.  **No pixel is painted, composited or synthesised** —
the modification is the resize and the WebP re-encode only.

    python tools/bake-moon.py [--refresh] [--max-width 2048] [--quality 82]
"""
from __future__ import annotations

import argparse
import io
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import common  # noqa: E402

from PIL import Image  # noqa: E402

SOURCE_URL = "https://svs.gsfc.nasa.gov/vis/a000000/a004700/a004720/lroc_color_poles_2k.tif"
OUT = "sky/moon_svs.webp"


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--refresh", action="store_true")
    ap.add_argument("--max-width", type=int, default=2048)
    ap.add_argument("--quality", type=int, default=82)
    args = ap.parse_args()

    raw = common.fetch(SOURCE_URL, suffix=".tif", refresh=args.refresh, timeout=300)
    im = Image.open(io.BytesIO(raw))
    im.load()
    src_size = im.size
    im = im.convert("RGB")

    max_w = min(args.max_width, im.width)
    if im.width != max_w:
        im = im.resize((max_w, max_w // 2), Image.LANCZOS)

    buf = io.BytesIO()
    im.save(buf, format="WEBP", quality=args.quality, method=6)
    blob = buf.getvalue()
    if len(blob) >= 4 * 1024 * 1024:
        raise SystemExit(f"moon webp is {len(blob)} B, over the 4 MB budget")

    out = common.ASSETS / OUT
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(blob)

    common.write_json(common.SCRATCH / "moon-bake-report.json", {
        "source": SOURCE_URL,
        "sourceBytes": len(raw),
        "sourceSize": list(src_size),
        "output": str(out),
        "outputSize": list(im.size),
        "outputBytes": len(blob),
        "sha256": common.sha256_bytes(blob),
        "quality": args.quality,
        "modification": "resized and re-encoded as lossy WebP; no pixel content changed",
    })
    print(f"wrote {out}  {im.size[0]}x{im.size[1]}  {len(blob)} B  "
          f"sha256 {common.sha256_bytes(blob)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
