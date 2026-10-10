#!/usr/bin/env python3
"""fetch-constellations.py — collect the two constellation references.

Three independent, licence-clean sources, each cached under
`.kiln/scratch/lumintra-assets/` so a later bake needs no network:

  1. **IAU constellation pages** — `https://iauarchive.eso.org/public/themes/constellations/`
     The IAU's own table of the 88 constellations (Latin name, three-letter abbreviation,
     English name) and, per constellation, the **boundary coordinate file** it publishes
     (`/static/public/constellations/txt/<abbr>.txt`), the Delporte-derived boundary
     vertices in `HH MM SS.SSSS| DD.DDDDDDD|ABBR` form. The IAU states the boundary data
     is public domain; the chart images are CC BY 4.0 and are not used here.
  2. **Sky & Telescope figure designs**, machine-readable as `constellation.figures.txt`
     from Martin Krzywinski's constellation resource (`https://mk.bcgsc.ca/constellations`),
     which transcribes into RA/Dec (degrees, J2000) the figures Alan MacRobert designed
     for Sky & Telescope and which appear on the IAU charts. The `conline` records are
     pairwise star connections with both endpoints' coordinates.
  3. **Wikidata** (CC0 1.0) for the Russian constellation names (`rdfs:label` @ `ru`).

The dcf21 `constellation_lines_iau.dat` source is deliberately **not** used: its README
says CC BY 4.0 while its own footer says GPL v3, so its licence cannot be recorded
unambiguously. `constellation.figures.txt` states its own credits in its header.

Writes `constellation-evidence.json` (the parsed, minimal facts) and the raw HTML.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from common import SCRATCH, fetch, sparql, write_json  # noqa: E402

IAU_PAGE = "https://iauarchive.eso.org/public/themes/constellations/"
IAU_BOUNDARY = "https://iauarchive.eso.org/static/public/constellations/txt/{abbr}.txt"
FIGURES_URL = "https://mk.bcgsc.ca/constellations/data/constellation.figures.txt"

RETRIEVED = "2026-10-09"

# --------------------------------------------------------------------------------------------
# The published IAU constellation area table, fetched rather than transcribed.
#
# The IAU archive does not publish area as a machine-readable column on either the index page
# or a per-constellation page (measured: `/constellations/<abbr>.html` is a 404 and the index
# table carries Name / Abbr. / English Name / Genitive / Downloads only). The table below is
# therefore read from the encyclopaedia article that reproduces the IAU values verbatim --
# rank, area in square degrees, and the constellation's centre as RA (h m) and Dec (deg arcmin).
# It is used three ways, and never copied by hand:
#
#   * `areaRank` in `constellations.json` comes from this table's `rank` column, because the
#     spec requires the *published* IAU rank, not a rank we derived;
#   * the area column is the independent reading our own Green's-theorem area is checked
#     against, which is what proves the boundary geometry was assembled correctly -- Serpens
#     in particular, where concatenating `ser1` and `ser2` into one ring gave 560 deg^2 against
#     the published 637 (measured, and the reason the two halves stay separate rings);
#   * the centre column is the independent reading our computed centre is checked against.
#
# Only the numeric facts (which are the IAU's) are shipped; the page is a cross-check source,
# not a bundled asset. `tools/bake-constellations.py` states the tolerance and reports every
# row that exceeds it.
# --------------------------------------------------------------------------------------------
AREA_TABLE_URL = "https://en.wikipedia.org/wiki/List_of_constellations_by_area"
_AREA_ROW = re.compile(
    r"<tr[^>]*>(.*?)</tr>", re.S)
_AREA_VAL = re.compile(r"(-?\d+)\s+(\d+\.\d+)")


def published_area_table() -> dict:
    """{abbrev: {rank, areaSqDeg, raHours, decDeg, name}} from the IAU-reproducing table."""
    html = fetch(AREA_TABLE_URL, suffix=".html").decode("utf-8", "replace")
    out = {}
    for row in _AREA_ROW.findall(html):
        cells = [re.sub(r"\s+", " ",
                        re.sub(r"<[^>]+>", " ", c)).replace("\u2212", "-").strip()
                 for c in re.findall(r"<t[dh][^>]*>(.*?)</t[dh]>", row, re.S)]
        if len(cells) < 8 or not cells[0].isdigit():
            continue
        ra, dec = _AREA_VAL.search(cells[6]), _AREA_VAL.search(cells[7])
        if not ra or not dec:
            continue
        sign = -1.0 if cells[7].strip().startswith("-") else 1.0
        out[cells[1]] = {
            "rank": int(cells[0]),
            "name": cells[2],
            "areaSqDeg": float(cells[3]),
            "raHours": int(ra.group(1)) + float(ra.group(2)) / 60.0,
            "decDeg": sign * (int(dec.group(1).lstrip("-")) + float(dec.group(2)) / 60.0),
        }
    if len(out) != 88:
        raise SystemExit(f"published area table parsed {len(out)} rows, expected 88")
    ranks = sorted(v["rank"] for v in out.values())
    if ranks != list(range(1, 89)):
        raise SystemExit("published area table ranks are not 1..88")
    return out

# Wikidata items for the two constellations whose `P31 Q8928` `rdfs:label` query returns no
# `ru` row (measured: neither "Aries" nor "Cancer" appears in the bulk result set, so the
# SPARQL leaves them nameless). They are looked up by QID instead, so the record is still
# sourced rather than hand-typed.
CONSTELLATION_QIDS = {"Aries": "Q10584", "Cancer": "Q8849"}


# --------------------------------------------------------------------------- IAU table
def iau_table() -> list[dict]:
    """Parse the IAU page's own 88-row table: Latin name, abbreviation, English name."""
    html = fetch(IAU_PAGE, suffix=".html").decode("utf-8", "replace")
    out = []
    for row in re.findall(r"<tr[^>]*>(.*?)</tr>", html, re.S):
        cells = [re.sub(r"\s+", " ",
                        re.sub(r"<[^>]+>", " ", c)).strip()
                 for c in re.findall(r"<t[dh][^>]*>(.*?)</t[dh]>", row, re.S)]
        if len(cells) < 4 or cells[1] in ("Abbr.", ""):
            continue
        # "Andromeda an-DRAH-mih-duh" -> the Latin name is the leading words, up to the
        # first token that is a pronunciation (lower-case or containing a hyphen/digit).
        head = cells[0]
        latin = []
        for token in head.split(" "):
            if token[:1].isupper() and not token.isupper() and "-" not in token:
                latin.append(token)
            else:
                break
        if not latin:
            continue
        english = re.sub(r"^the\s+", "", cells[2]).strip()
        out.append({"latin": " ".join(latin), "abbrev": cells[1], "english": english})
    return out


# ----------------------------------------------------------------------- boundaries
_BOUNDARY = re.compile(r"^(\d{2}) (\d{2}) (\d{2}\.\d+)\|\s*(-?\d+\.\d+)\|(\S+)")


def boundary_polys(abbrev: str) -> list[list[tuple[float, float]]]:
    """The IAU boundary vertices for one constellation, as one polygon per boundary file.

    Serpens is the one constellation the IAU publishes as **two** boundary files
    (`ser1` caput and `ser2` cauda), because its two halves are separated by Ophiuchus.
    They are kept as **two separate closed polygons** rather than concatenated: a single
    concatenated ring would close a spurious edge from the end of the head to the start of
    the tail and report an area of 560 deg² instead of the published 637 deg² (measured).
    Every other constellation is one polygon.
    """
    names = ["ser1", "ser2"] if abbrev.lower() == "ser" else [abbrev.lower()]
    polys = []
    for name in names:
        raw = fetch(IAU_BOUNDARY.format(abbr=name), suffix=".txt")
        text = raw.decode("utf-8", "replace")
        pts = []
        for line in text.splitlines():
            m = _BOUNDARY.match(line.strip())
            if not m:
                continue
            hh, mm, ss, dec, _tag = m.groups()
            ra_deg = (int(hh) + int(mm) / 60.0 + float(ss) / 3600.0) * 15.0
            pts.append((ra_deg % 360.0, float(dec)))
        polys.append(pts)
    return polys


# --------------------------------------------------------------------------- figures
# A `conline` record is positional. It is documented in the file's own header (2.2), and the
# fields are, per endpoint: HR number, designation, constellation, magnitude, magnitude band,
# RA (deg), Dec (deg), unit vector to the other end (uRA, uDec), end-gap flag.
#
#   conline <con> <thickness> <hr1-hr2>
#       <hr1> <desig1> <con1> <mag1> <band1> <ra1> <dec1> <ura1> <udec1> <gap1>
#       <hr2> <desig2> <con2> <mag2> <band2> <ra2> <dec2> <ura2> <udec2> <gap2>
#
# Two traps this pattern is written around, both measured:
#   * the `band` field is any spectral band letter (V, K, B, ...) and, for a segment that
#     ends on a midpoint between two stars, its concatenation (`VV`). Anchoring on `[VB]`
#     dropped 34 real segments -- Car, Cen and Cep among them -- before this was fixed;
#   * the designation may be empty, written `-`.
_REAL = r"-?\d+\.\d+"
_CONLINE = re.compile(
    r"conline\s+(\S+)\s+(\d+)\s+(\S+)\s+"                       # con, thickness, HR pair
    r"(\S+)\s+(\S+)\s+(\S+)\s+(" + _REAL + r")\s+([A-Za-z]{1,3})\s+"
    r"(" + _REAL + r")\s+(" + _REAL + r")\s+(" + _REAL + r")\s+(" + _REAL + r")\s+(\d)\s+"
    r"(\S+)\s+(\S+)\s+(\S+)\s+(" + _REAL + r")\s+([A-Za-z]{1,3})\s+"
    r"(" + _REAL + r")\s+(" + _REAL + r")\s+(" + _REAL + r")\s+(" + _REAL + r")\s+(\d)")


def figures() -> tuple[dict, int]:
    """`({abbrev: [segment, ...]}, total)` where a segment is `{a: [hr, ra, dec], b: [...]}`.

    The HR number is kept because it, not the float coordinate, is the star's identity: a
    segment ending on a midpoint carries `hr` of the form `6938/6934`, and two segments that
    meet at that midpoint carry the *same string*, which is what lets `bake-constellations.py`
    join segments into polylines without guessing at float equality.
    """
    text = fetch(FIGURES_URL, suffix=".txt").decode("utf-8", "replace")
    out: dict[str, list] = {}
    total = unparsed = 0
    for line in text.splitlines():
        if not line.startswith("conline"):
            continue
        total += 1
        m = _CONLINE.match(line.strip())
        if not m:
            unparsed += 1
            continue
        con = m.group(1)
        a = [m.group(4), float(m.group(9)) % 360.0, float(m.group(10))]
        b = [m.group(14), float(m.group(19)) % 360.0, float(m.group(20))]
        out.setdefault(con, []).append({"a": a, "b": b})
    if unparsed:
        raise SystemExit(f"{unparsed} of {total} conline records did not parse")
    return out, total


# ------------------------------------------------------------------------- Wikidata
Q_CONSTELLATION = "Q8928"


def wikidata_names() -> dict:
    """English label -> its Russian label, for every Wikidata constellation item."""
    rows = sparql(
        f'SELECT ?en ?ru WHERE {{ ?item wdt:P31 wd:{Q_CONSTELLATION} . '
        '?item rdfs:label ?en FILTER(LANG(?en)="en") . '
        '?item rdfs:label ?ru FILTER(LANG(?ru)="ru") . }')
    return {r["en"]["value"].strip(): r["ru"]["value"].strip() for r in rows}


def wikidata_label_by_qid(qids: list) -> dict:
    """{QID: Russian label} for an explicit QID list -- the by-QID fallback above."""
    values = " ".join(f"wd:{q}" for q in qids)
    rows = sparql(f'SELECT ?item ?ru WHERE {{ VALUES ?item {{ {values} }} '
                  '?item rdfs:label ?ru FILTER(LANG(?ru)="ru") . }')
    return {r["item"]["value"].rsplit("/", 1)[-1]: r["ru"]["value"].strip() for r in rows}


def main() -> int:
    table = iau_table()
    if len(table) != 88:
        raise SystemExit(f"IAU table parsed {len(table)} rows, expected 88")
    print(f"IAU table: {len(table)} rows")

    boundaries, missing = {}, []
    for row in table:
        polys = boundary_polys(row["abbrev"])
        if not polys or any(len(p) < 3 for p in polys):
            missing.append(row["abbrev"])
        boundaries[row["abbrev"]] = polys
    if missing:
        raise SystemExit(f"no usable boundary data for {missing}")
    print("boundaries: " + ", ".join(f"{k}={[len(p) for p in v]}"
                                     for k, v in list(boundaries.items())[:5]) + ", ...")

    figs, total_conline = figures()
    print(f"figures: {len(figs)} constellations, {total_conline} conline segments, "
          f"0 unparsed")

    names = wikidata_names()
    print(f"wikidata constellation items with en+ru labels: {len(names)}")
    by_qid = wikidata_label_by_qid(list(CONSTELLATION_QIDS.values()))
    for english, qid in CONSTELLATION_QIDS.items():
        if english not in names and qid in by_qid:
            names[english] = by_qid[qid]
            print(f"  + {english} ({qid}) -> {by_qid[qid]}")

    pub = published_area_table()
    print(f"published area table: {len(pub)} rows, ranks 1..88")
    missing_area = sorted(set(pub) ^ {r["abbrev"] for r in table})
    if missing_area:
        raise SystemExit(f"published-area table disagrees on {missing_area}")

    write_json(SCRATCH / "constellation-evidence.json", {
        "retrieved": RETRIEVED,
        "sources": {
            "iauPage": {"url": IAU_PAGE,
                        "use": "88 rows: Latin name, IAU abbreviation, English name",
                        "licence": "IAU constellation information (public domain boundary "
                                   "data; IAU WGSN names CC BY 4.0)"},
            "iauBoundary": {"url": IAU_BOUNDARY.format(abbr="<abbr>"),
                            "use": "Delporte-derived constellation boundary vertices",
                            "licence": "public domain (IAU)"},
            "figures": {"url": FIGURES_URL,
                        "use": "Sky & Telescope / Alan MacRobert figure designs, pairwise "
                               "RA/Dec (degrees, J2000)",
                        "credit": "Constellation figures: IAU and Sky & Telescope "
                                  "(Roger Sinnott, Rick Fienberg; patterns by Alan MacRobert) "
                                  "— CC BY 4.0",
                        "licence": "CC BY 4.0",
                        "compiledBy": "Martin Krzywinski, http://mkweb.bcgsc.ca/constellations"},
            "wikidata": {"url": "https://query.wikidata.org/sparql",
                         "use": "Russian constellation names (rdfs:label @ ru)",
                         "licence": "CC0 1.0"},
            "publishedArea": {
                "url": AREA_TABLE_URL,
                "use": "published IAU constellation area, area rank, and centre, used as the "
                       "independent reading the computed area and computed centre are checked "
                       "against; the shipped `areaRank` comes from this table's rank column",
                "licence": "the tabulated numbers are IAU facts (public domain); the page is a "
                           "cross-check source and is not bundled"},
        },
        "table": table,
        "boundaries": boundaries,
        "figures": figs,
        "published": pub,
        "wikidataNames": names,
        "constellationQids": CONSTELLATION_QIDS,
    })
    print(f"wrote {SCRATCH / 'constellation-evidence.json'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
