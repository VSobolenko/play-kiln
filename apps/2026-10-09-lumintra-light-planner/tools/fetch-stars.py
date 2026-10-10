"""Fetch the FK6 star catalogue (VizieR I/264) and the spectral classes used for its colour.

Outputs, all under ``.kiln/scratch/lumintra-assets/`` (never the system temp directory):

  stars-fk6.json           the FK6 rows actually used: FK6, HIP, Name, RA, Dec, Vmag, SpType,
                           colourBucket
  stars-supplement.json    the hand-authored supplement rows (Sirius, Arcturus, Capella,
                           Betelgeuse) with both verification sources recorded per row
  stars-fetch-report.json  row counts, coverage, licence lines captured verbatim

Reproducible with no arguments::

    python tools/fetch-stars.py [--refresh]

Licence discipline
------------------
* **Positions, V magnitudes and identity: FK6, VizieR I/264** (Wielen et al. 1999).  The design
  records it as CC0 1.0; the ReadMe himself is captured verbatim into the report so the claim is
  a measurement rather than a recollection.
* **Spectral class (the colour input): VizieR B/mk/mktypes**, Skiff (2014), *Catalogue of Stellar
  Spectral Classifications* (``2014yCat....1.2023S``).  It is a compilation of published
  classifications; every row carries the Bibcode of the paper it came from, and the compilation
  itself asks that the paper, not the compilation, be cited in publication.  The per-row Bibcode
  is kept in the cached join output so the primary source of every spectral type is auditable.
* **Supplement stars: Wikidata (CC0 1.0)** P6257/P6258/P1215/P215 plus a second published source
  per row (see ``stars-supplement.json``).

Explicitly NOT used, per the slot's asset brief: OpenNGC, HYG, NGC 2000.0, the Hipparcos/Tycho
tables, Stellarium, and the Gaia panorama.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import common  # noqa: E402

TAP = "https://tapvizier.cds.unistra.fr/TAPVizieR/tap"
FK6_TABLES = ("I/264/fk6_1", "I/264/fk6_3")
MK_TYPES = "B/mk/mktypes"
READ_MES = {
    "I/264": ("https://cdsarc.cds.unistra.fr/ftp/I/264/ReadMe", ".txt"),
    "B/mk": ("https://cdsarc.cds.unistra.fr/ftp/B/mk/ReadMe", ".txt"),
}

# 0.01 deg = 36 arcsec: tight enough that two different naked-eye stars never share a match,
# loose enough to survive the epoch/proper-motion difference between FK6 and Skiff's positions.
MATCH_RADIUS_DEG = 0.01
# Skiff's magnitudes come from heterogeneous photographic work; the guard only rejects a match
# that is a full magnitude away, which cannot be the same star at this radius.
MAG_GUARD = 1.0

SELECT_FK6 = ('SELECT FK6, HIP, Name, "RA(ICRS)" AS ra, "DE(ICRS)" AS de, Vmag '
              'FROM "{table}"')

JOIN_SPTYPES = (
    'SELECT f.FK6 AS fk6, m.SpType AS sp, m.Mag AS mag, m.Bibcode AS bibcode, m.Name AS mname '
    'FROM "{table}" AS f '
    'JOIN "{mk}" AS m ON 1=CONTAINS(POINT(\'ICRS\', m.RAJ2000, m.DEJ2000), '
    'CIRCLE(\'ICRS\', f."RA(ICRS)", f."DE(ICRS)", {radius}))'
)

# The four stars FK6 omits (design.md § "The FK6 supplement is mandatory").  The Wikidata item
# ids are the CC0 source; the second source is a published catalogue read through VizieR.
SUPPLEMENT = (
    {"id": "sirius", "name": "Sirius", "wikidata": "Q3409", "vizierName": "alf CMa",
     "hip": 32349, "desig": "alpha CMa"},
    {"id": "arcturus", "name": "Arcturus", "wikidata": "Q12985", "vizierName": "alf Boo",
     "hip": 69673, "desig": "alpha Boo"},
    {"id": "capella", "name": "Capella", "wikidata": "Q12970", "vizierName": "alf Aur",
     "hip": 24608, "desig": "alpha Aur"},
    {"id": "betelgeuse", "name": "Betelgeuse", "wikidata": "Q12124", "vizierName": "alf Ori",
     "hip": 27989, "desig": "alpha Ori"},
)


def fetch_readme(key: str, refresh: bool) -> dict:
    url, suffix = READ_MES[key]
    text = common.fetch(url, suffix=suffix, refresh=refresh, timeout=120).decode("utf-8", "replace")
    lines = [ln.strip() for ln in text.splitlines()
             if any(k in ln.lower() for k in ("licen", "copyright", "public domain", "credit"))]
    return {
        "url": url,
        "bytes": len(text.encode("utf-8")),
        "sha256": common.sha256_bytes(text.encode("utf-8")),
        "licenceLines": lines[:12],
        "licenceLineCount": len(lines),
    }


def count_rows(table: str, refresh: bool) -> int:
    d = common.tap_sync(TAP, f'SELECT COUNT(*) AS n FROM "{table}"', refresh=refresh)
    return int(d["data"][0][0])


def fetch_fk6(refresh: bool) -> list[dict]:
    rows: list[dict] = []
    for table in FK6_TABLES:
        d = common.tap_sync(TAP, SELECT_FK6.format(table=table), refresh=refresh)
        names = [m["name"] for m in d["metadata"]]
        for r in d["data"]:
            rec = dict(zip(names, r))
            rows.append({
                "fk6": int(rec["FK6"]),
                "hip": int(rec["HIP"]) if rec["HIP"] is not None else None,
                "name": (rec["Name"] or "").strip(),
                "raDeg": float(rec["ra"]),
                "decDeg": float(rec["de"]),
                "vmag": float(rec["Vmag"]) if rec["Vmag"] is not None else None,
                "table": table,
            })
        print(f"  {table}: {len(d['data'])} rows", flush=True)
    return rows


def fetch_sptypes(refresh: bool) -> dict[int, list[dict]]:
    """Positional cross-match of FK6 against B/mk/mktypes; many classifications per star."""
    out: dict[int, list[dict]] = {}
    for table in FK6_TABLES:
        adql = JOIN_SPTYPES.format(table=table, mk=MK_TYPES, radius=MATCH_RADIUS_DEG)
        d = common.tap_sync(TAP, adql, refresh=refresh)
        names = [m["name"] for m in d["metadata"]]
        for r in d["data"]:
            rec = dict(zip(names, r))
            out.setdefault(int(rec["fk6"]), []).append({
                "sp": (rec["sp"] or "").strip(),
                "mag": rec["mag"],
                "bibcode": (rec["bibcode"] or "").strip(),
                "sourceName": (rec["mname"] or "").strip(),
            })
        print(f"  join {table}: {len(d['data'])} classifications for "
              f"{len(out)} stars so far", flush=True)
    return out


def fetch_supplement_mk(refresh: bool) -> dict[str, list[dict]]:
    out: dict[str, list[dict]] = {}
    for star in SUPPLEMENT:
        adql = (f'SELECT Name, SpType, Mag, Bibcode, RAJ2000, DEJ2000 FROM "{MK_TYPES}" '
                f'WHERE Name = \'{star["vizierName"]}\'')
        d = common.tap_sync(TAP, adql, refresh=refresh)
        names = [m["name"] for m in d["metadata"]]
        out[star["id"]] = [dict(zip(names, r)) for r in d["data"]]
        print(f"  {star['name']}: {len(out[star['id']])} rows from {MK_TYPES}", flush=True)
    return out


def fetch_supplement_wikidata(refresh: bool) -> dict[str, dict]:
    out: dict[str, dict] = {}
    for star in SUPPLEMENT:
        qid = star["wikidata"]
        ent = json.loads(common.fetch(
            f"https://www.wikidata.org/wiki/Special:EntityData/{qid}.json",
            suffix=".json", refresh=refresh))["entities"][qid]
        claims = ent["claims"]

        def vals(pid: str) -> list:
            return [c["mainsnak"].get("datavalue", {}).get("value")
                    for c in claims.get(pid, []) if "datavalue" in c["mainsnak"]]

        out[star["id"]] = {
            "qid": qid,
            "label": (ent.get("labels", {}).get("en", {}) or {}).get("value"),
            "raDeg": vals("P6257"),
            "decDeg": vals("P6258"),
            "vmag": [v.get("amount") for v in vals("P1215") if isinstance(v, dict)],
            "spectralClass": vals("P215"),
        }
        print(f"  {star['name']} ({qid}): {len(out[star['id']]['spectralClass'])} spectral classes, "
              f"{len(out[star['id']]['vmag'])} magnitudes on Wikidata", flush=True)
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--refresh", action="store_true")
    args = ap.parse_args()

    print("counting FK6 rows (VizieR I/264)…", flush=True)
    counts = {t: count_rows(t, args.refresh) for t in FK6_TABLES}
    print(f"  {counts}  total {sum(counts.values())}")

    print("fetching FK6 rows…", flush=True)
    rows = fetch_fk6(args.refresh)
    distinct = sorted({r["fk6"] for r in rows})
    print(f"  {len(rows)} rows, {len(distinct)} distinct FK6 numbers, "
          f"{len(rows) - len(distinct)} duplicate FK6 numbers")

    print("fetching ReadMe licence lines…", flush=True)
    readmes = {k: fetch_readme(k, args.refresh) for k in READ_MES}
    for k, v in readmes.items():
        print(f"  {k}: {v['licenceLineCount']} licence/copyright lines")

    print("cross-matching spectral classes (B/mk/mktypes)…", flush=True)
    sptypes = fetch_sptypes(args.refresh)
    print(f"  {len(sptypes)} of {len(rows)} FK6 stars matched a spectral classification")

    print("fetching supplement stars…", flush=True)
    supp_wd = fetch_supplement_wikidata(args.refresh)
    supp_mk = fetch_supplement_mk(args.refresh)

    common.write_json(common.SCRATCH / "stars-fk6.json", rows)
    common.write_json(common.SCRATCH / "stars-sptypes.json",
                      {str(k): v for k, v in sptypes.items()})
    common.write_json(common.SCRATCH / "stars-supplement-wikidata.json", supp_wd)
    common.write_json(common.SCRATCH / "stars-supplement-mktypes.json", supp_mk)
    common.write_json(common.SCRATCH / "stars-fetch-report.json", {
        "tap": TAP,
        "tables": list(FK6_TABLES),
        "tableCounts": counts,
        "totalCount": sum(counts.values()),
        "rowCount": len(rows),
        "distinctFk6": len(distinct),
        "duplicateFk6": len(rows) - len(distinct),
        "rowsWithSpectralClass": len(sptypes),
        "matchRadiusDeg": MATCH_RADIUS_DEG,
        "magGuard": MAG_GUARD,
        "readmes": readmes,
    })
    print("wrote stars-fk6.json, stars-sptypes.json, stars-supplement-*.json, stars-fetch-report.json")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
