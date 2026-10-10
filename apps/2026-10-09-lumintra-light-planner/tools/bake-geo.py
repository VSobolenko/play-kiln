"""Bake ``app/src/main/assets/geo/coastline_110m.bin`` from the Natural Earth 1:110m coastline.

Source
------
Natural Earth, *Coastline*, 1:110m physical vectors,
``https://naciscdn.org/naturalearth/110m/physical/ne_110m_coastline.zip``.
Natural Earth is **public domain** ("no permission needed ... no attribution required", though it is
credited here anyway): <https://www.naturalearthdata.com/about/terms-of-use/>.

Transformation
--------------
The shapefile's PolyLine records are read directly (a minimal SHP reader, no third-party
dependency), each part becomes one polyline, a Douglas-Peucker pass removes vertices closer than
``--tolerance`` degrees to the simplified line, and the result is written in the compact format
tabulated in ``docs/ASSET-FORMATS.md`` §9.

    python tools/bake-geo.py [--refresh] [--tolerance 0.05]
"""
from __future__ import annotations

import argparse
import io
import json
import struct
import sys
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import common  # noqa: E402

SOURCE_URL = "https://naciscdn.org/naturalearth/110m/physical/ne_110m_coastline.zip"
MEMBERS = {"shp": "ne_110m_coastline.shp", "dbf": "ne_110m_coastline.dbf",
           "prj": "ne_110m_coastline.prj", "version": "ne_110m_coastline.VERSION"}
OUT = "geo/coastline_110m.bin"
MAGIC = b"LUMGEO01"


def read_shp_polylines(blob: bytes) -> list[list[tuple[float, float]]]:
    """Minimal ESRI shapefile reader for PolyLine (shape type 3) and Polygon (5) records."""
    (file_code,) = struct.unpack(">i", blob[0:4])
    if file_code != 9994:
        raise SystemExit(f"not a shapefile: file code {file_code}")
    (shape_type,) = struct.unpack("<i", blob[32:36])
    if shape_type not in (3, 5):
        raise SystemExit(f"unexpected shape type {shape_type}, expected PolyLine(3)/Polygon(5)")
    lines: list[list[tuple[float, float]]] = []
    off = 100
    n = len(blob)
    while off + 8 <= n:
        rec_num, rec_words = struct.unpack(">ii", blob[off:off + 8])
        off += 8
        rec_bytes = rec_words * 2
        body = blob[off:off + rec_bytes]
        off += rec_bytes
        if not body:
            break
        (st,) = struct.unpack("<i", body[0:4])
        if st == 0:                       # null shape
            continue
        if st not in (3, 5):
            raise SystemExit(f"record {rec_num}: unexpected shape type {st}")
        num_parts, num_points = struct.unpack("<ii", body[36:44])
        parts = struct.unpack(f"<{num_parts}i", body[44:44 + 4 * num_parts])
        p_off = 44 + 4 * num_parts
        pts = struct.unpack(f"<{2 * num_points}d", body[p_off:p_off + 16 * num_points])
        bounds = list(parts) + [num_points]
        for i in range(num_parts):
            seg = [(pts[2 * j], pts[2 * j + 1]) for j in range(bounds[i], bounds[i + 1])]
            if len(seg) >= 2:
                lines.append(seg)
    return lines


def douglas_peucker(pts, tol: float):
    """Iterative Douglas-Peucker; returns the kept vertices in order."""
    if len(pts) < 3 or tol <= 0:
        return list(pts)
    keep = [False] * len(pts)
    keep[0] = keep[-1] = True
    stack = [(0, len(pts) - 1)]
    tol2 = tol * tol
    while stack:
        i, j = stack.pop()
        ax, ay = pts[i]
        bx, by = pts[j]
        dx, dy = bx - ax, by - ay
        den = dx * dx + dy * dy
        best, best_k = -1.0, -1
        for k in range(i + 1, j):
            px, py = pts[k]
            if den == 0.0:
                d2 = (px - ax) ** 2 + (py - ay) ** 2
            else:
                t = ((px - ax) * dx + (py - ay) * dy) / den
                t = 0.0 if t < 0.0 else (1.0 if t > 1.0 else t)
                d2 = (px - (ax + t * dx)) ** 2 + (py - (ay + t * dy)) ** 2
            if d2 > best:
                best, best_k = d2, k
        if best > tol2:
            keep[best_k] = True
            stack.append((i, best_k))
            stack.append((best_k, j))
    return [p for p, k in zip(pts, keep) if k]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--refresh", action="store_true")
    ap.add_argument("--tolerance", type=float, default=0.05)
    args = ap.parse_args()

    raw = common.fetch(SOURCE_URL, suffix=".zip", refresh=args.refresh, timeout=300)
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        names = z.namelist()
        version = {k: z.read(v).decode("utf-8", "replace").strip()
                   for k, v in MEMBERS.items() if k == "version" and v in names}
        shp = z.read(MEMBERS["shp"])

    raw_lines = read_shp_polylines(shp)
    simp = [douglas_peucker(ln, args.tolerance) for ln in raw_lines]
    simp = [ln for ln in simp if len(ln) >= 2]
    total_points = sum(len(ln) for ln in simp)

    out_bytes = bytearray()
    out_bytes += MAGIC
    out_bytes += struct.pack("<IIII", 1, len(simp), total_points, 0)
    for ln in simp:
        out_bytes += struct.pack("<I", len(ln))
        for lon, lat in ln:
            out_bytes += struct.pack("<ff", lon, lat)

    out = common.ASSETS / OUT
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(bytes(out_bytes))

    all_pts = [p for ln in simp for p in ln]
    common.write_json(common.SCRATCH / "geo-bake-report.json", {
        "source": SOURCE_URL,
        "archiveMembers": names,
        "naturalEarthVersion": version,
        "rawLines": len(raw_lines),
        "rawPoints": sum(len(ln) for ln in raw_lines),
        "lines": len(simp),
        "points": total_points,
        "toleranceDeg": args.tolerance,
        "bounds": {
            "lon": [min(p[0] for p in all_pts), max(p[0] for p in all_pts)],
            "lat": [min(p[1] for p in all_pts), max(p[1] for p in all_pts)],
        },
        "output": str(out),
        "bytes": len(out_bytes),
        "sha256": common.sha256_bytes(bytes(out_bytes)),
    })
    print(f"wrote {out}  {len(simp)} lines  {total_points} points  "
          f"{len(out_bytes)} B  sha256 {common.sha256_bytes(bytes(out_bytes))}")
    print(f"  raw: {len(raw_lines)} lines / {sum(len(ln) for ln in raw_lines)} points"
          f"  simplified at {args.tolerance} deg")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
