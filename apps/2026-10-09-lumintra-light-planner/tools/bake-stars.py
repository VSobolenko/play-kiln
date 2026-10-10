"""Bake ``app/src/main/assets/catalog/stars.bin`` from the fetched FK6 rows plus the supplement.

Layout (docs/ASSET-FORMATS.md §3, design.md § "The catalogue"): 11 bytes per star, little-endian,
no header, no footer::

    uint32-le  ?            -- not used; see below
    per star, LITTLE ENDIAN:
        0   float32  raDeg        degrees, [0, 360), ICRS J2000.0
        4   float32  decDeg       degrees, [-90, +90], ICRS J2000.0
        8   float16  vmag         V magnitude
       10   uint8    colourBucket 0..5

The reader is ``data/SkyAssetSource.kt:321`` — it wraps the bytes in
``ByteBuffer.wrap(bytes).order(LITTLE_ENDIAN)`` and reads float, float, short(half), byte.

Run::

    python tools/bake-stars.py

Asserts the row count and the byte size, and prints the bucket histogram so a bake that lost
its colour (everything in bucket 2) is visible in the output rather than in the app.
"""
from __future__ import annotations

import argparse
import json
import math
import struct
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import common  # noqa: E402
import stars_common  # noqa: E402

RECORD_BYTES = 11
FK6_ROWS_EXPECTED = 4150
SUPPLEMENT_IDS = ("sirius", "arcturus", "capella", "betelgeuse")
TOLERANCE_ARCMIN = 1.0     # sizeMajArcmin is irrelevant for a star: the tight tolerance applies
TOLERANCE_TIGHT = 0.2      # the design's unresolved-object tolerance — a star is unresolved
MAG_DISPUTE = 0.50


def pick_spectral_class(rows: list[dict]) -> dict | None:
    """The most recently published classification wins; ties broken by the string itself.

    ``bibcode`` sorts chronologically because the year is its first four characters.
    """
    usable = [r for r in rows if (r.get("sp") or "").strip()]
    if not usable:
        return None
    return max(usable, key=lambda r: (r["bibcode"], r["sp"]))


def build_fk6_rows(fk6: list[dict], sptypes: dict[str, list[dict]]) -> tuple[list[dict], dict]:
    out: list[dict] = []
    stats = {"noSpectralClass": 0, "unmappableSpectralClass": 0, "noVmag": [],
             "bucketHistogram": [0] * 6, "spectralClassesUsed": {}}
    for row in fk6:
        cand = pick_spectral_class(sptypes.get(str(row["fk6"]), []))
        bucket = None
        if cand is None:
            stats["noSpectralClass"] += 1
        else:
            bucket, _bv = stars_common.bucket_for_spectral_class(cand["sp"])
            if bucket is None:
                stats["unmappableSpectralClass"] += 1
            else:
                stats["spectralClassesUsed"][cand["sp"]] = \
                    stats["spectralClassesUsed"].get(cand["sp"], 0) + 1
        if bucket is None:
            bucket = stars_common.DEFAULT_BUCKET
        if row["vmag"] is None:
            stats["noVmag"].append(row["fk6"])
        stats["bucketHistogram"][bucket] += 1
        out.append({"raDeg": row["raDeg"], "decDeg": row["decDeg"],
                    "vmag": row["vmag"] if row["vmag"] is not None else 0.0,
                    "colourBucket": bucket, "spType": cand["sp"] if cand else None})
    return out, stats


def build_supplement(wd: dict, mk: dict) -> list[dict]:
    rows: list[dict] = []
    for sid in SUPPLEMENT_IDS:
        w = wd[sid]
        if not w["raDeg"] or not w["decDeg"]:
            raise SystemExit(f"supplement {sid}: Wikidata has no RA/Dec; refusing to invent one")
        ra1 = float(w["raDeg"][0]["amount"])
        de1 = float(w["decDeg"][0]["amount"])
        picks = mk[sid]
        if not picks:
            raise SystemExit(f"supplement {sid}: no second source; refusing to invent one")
        # The second source must be the same star: take the published row whose position is
        # closest to the CC0 position, then require the separation to be inside tolerance.
        ra2 = min(picks, key=lambda p: common.sep_arcmin(ra1, de1, p["RAJ2000"], p["DEJ2000"]))
        delta = common.sep_arcmin(ra1, de1, ra2["RAJ2000"], ra2["DEJ2000"])
        if delta > TOLERANCE_ARCMIN:
            raise SystemExit(f"supplement {sid}: the two sources disagree by {delta:.3f} arcmin")
        latest = max(picks, key=lambda p: (p["Bibcode"], p["SpType"]))
        bucket, bv = stars_common.bucket_for_spectral_class(latest["SpType"])
        if bucket is None:
            raise SystemExit(f"supplement {sid}: spectral class {latest['SpType']!r} is unmappable")
        # V: among the CC0 magnitudes that agree with the second source within MAG_DISPUTE, the
        # one the CC0 source states most often wins; ties break toward the second source and then
        # toward the brighter value.  Magnitudes outside the agreement window are not averaged —
        # a disagreement past MAG_DISPUTE sets the flag and the published value is shipped.
        v2 = float(ra2["Mag"])
        wd_mags = [float(a) for a in w["vmag"]]
        near = [v for v in wd_mags if abs(v - v2) <= MAG_DISPUTE]
        if near:
            v1 = max(near, key=lambda v: (near.count(v), -abs(v - v2), -v))
            disputed = False
        else:
            v1, disputed = v2, True
        rows.append({
            "id": sid, "name": w["label"],
            "raDeg": ra1, "decDeg": de1, "vmag": v1,
            "colourBucket": bucket, "bvUsed": bv,
            "spType": latest["SpType"], "spTypeBibcode": latest["Bibcode"],
            "secondSource": {"catalogue": "VizieR B/mk/mktypes (Skiff 2014)",
                             "name": ra2["Name"], "raDeg": ra2["RAJ2000"],
                             "decDeg": ra2["DEJ2000"], "vmag": v2},
            "deltaArcmin": delta, "magDisputed": disputed,
        })
    return rows


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=None)
    args = ap.parse_args()

    fk6 = json.loads((common.SCRATCH / "stars-fk6.json").read_text(encoding="utf-8"))
    sptypes = json.loads((common.SCRATCH / "stars-sptypes.json").read_text(encoding="utf-8"))
    wd = json.loads((common.SCRATCH / "stars-supplement-wikidata.json").read_text(encoding="utf-8"))
    mk = json.loads((common.SCRATCH / "stars-supplement-mktypes.json").read_text(encoding="utf-8"))

    rows, stats = build_fk6_rows(fk6, sptypes)
    supp = build_supplement(wd, mk)
    for s in supp:
        stats["bucketHistogram"][s["colourBucket"]] += 1
    rows.extend(supp)

    assert len(fk6) == FK6_ROWS_EXPECTED, f"FK6 row count {len(fk6)} != {FK6_ROWS_EXPECTED}"
    assert len(rows) == FK6_ROWS_EXPECTED + len(SUPPLEMENT_IDS), \
        f"row count {len(rows)} != {FK6_ROWS_EXPECTED + len(SUPPLEMENT_IDS)}"

    blob = bytearray()
    for r in rows:
        blob += struct.pack("<ff", r["raDeg"], r["decDeg"])
        blob += struct.pack("<e", r["vmag"])
        blob += struct.pack("<B", r["colourBucket"])

    expected = len(rows) * RECORD_BYTES
    assert len(blob) == expected, f"size {len(blob)} != {expected}"
    assert expected == (FK6_ROWS_EXPECTED + len(SUPPLEMENT_IDS)) * RECORD_BYTES

    out = Path(args.out) if args.out else common.ASSETS / "catalog" / "stars.bin"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(bytes(blob))

    report = {
        "rows": len(rows),
        "fk6Rows": len(fk6),
        "supplementRows": len(supp),
        "recordBytes": RECORD_BYTES,
        "sizeBytes": len(blob),
        "sha256": common.sha256_bytes(bytes(blob)),
        "fk6RowsWithoutSpectralClass": stats["noSpectralClass"],
        "fk6RowsWithUnmappableSpectralClass": stats["unmappableSpectralClass"],
        "fk6RowsWithSpectralClass": len(fk6) - stats["noSpectralClass"],
        "fk6RowsWithNullVmag": len(stats["noVmag"]),
        "bucketHistogram": stats["bucketHistogram"],
        "distinctSpectralClasses": len(stats["spectralClassesUsed"]),
        "topSpectralClasses": sorted(stats["spectralClassesUsed"].items(),
                                     key=lambda kv: -kv[1])[:25],
        "supplement": supp,
        "output": str(out),
    }
    common.write_json(common.SCRATCH / "stars-bake-report.json", report)

    print(f"wrote {out}")
    print(f"  rows {len(rows)} = {len(fk6)} FK6 + {len(supp)} supplement"
          f"   size {len(blob)} B   sha256 {report['sha256']}")
    print(f"  spectral class matched for {report['fk6RowsWithSpectralClass']}/{len(fk6)} FK6 rows;"
          f" {stats['unmappableSpectralClass']} unmappable; {len(stats['noVmag'])} null Vmag")
    print(f"  bucket histogram (0..5): {stats['bucketHistogram']}")
    for s in supp:
        print(f"  supplement {s['id']:<11} {s['spType']:<12} bucket {s['colourBucket']}"
              f"  delta {s['deltaArcmin']:.4f} arcmin  V {s['vmag']}"
              f"{'  MAG-DISPUTED' if s['magDisputed'] else ''}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
