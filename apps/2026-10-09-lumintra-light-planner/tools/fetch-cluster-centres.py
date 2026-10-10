#!/usr/bin/env python3
"""fetch-cluster-centres.py — second-reference positions for the extended targets.

Why this exists
---------------
The target-catalogue spec requires every authored row to be checked against **two
independent sources** and to be re-sourced when the pair disagrees by more than the stated
tolerance. Measured on 2026-10-09, the two sources the spec names — Wikidata (`P6257`/
`P6258`) and NASA HEASARC's `messier` table — are **not sufficient for the large open
clusters**: an open cluster 12'–120' across has no single physical centre, so independent
compilations adopt different member sets and place its centre 1'–22' apart. The HEASARC
`messier` table was measured to be the coarsest of them (its Messier positions disagree
with Wikidata by up to 21.8'), which left several Messier rows with no passing pair.

This fetcher adds the modern, homogeneous, licence-clean cluster-centre catalogues so a
row can be *re-sourced* rather than dropped:

===================  ==========================================  ==================
key                  catalogue                                   reference
===================  ==========================================  ==================
``mwsc``             VizieR J/A+A/558/A53                        Kharchenko+ 2013
``cg20``             VizieR J/A+A/633/A99                        Cantat-Gaudin & Anders 2020
``hr23``             VizieR J/A+A/673/A114                       Hunt & Reffert 2023
===================  ==========================================  ==================

They are **not** the forbidden catalogues: none of OpenNGC, HYG, NGC 2000.0,
Hipparcos/Tycho, Stellarium or the Gaia panorama is involved. Each is a published
catalogue read through CDS/VizieR, and only its *cluster centre* (a fact) is copied.

Route
-----
The fetch uses VizieR's ``asu-tsv`` output rather than its TAP endpoint, because on
2026-10-09 the TAP endpoint (`tapvizier.cds.unistra.fr`) answered **HTTP 500** for every
query including the plain ``SELECT * FROM "B/ocl/clusters"`` that had succeeded earlier in
the session, while ``viz-bin/asu-tsv`` answered 200 throughout. The output is cached under
``.kiln/scratch/lumintra-assets/`` and re-used, so a later re-run does not need the network.
"""
from __future__ import annotations

import json
import sys
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from common import SCRATCH, USER_AGENT, write_json  # noqa: E402

BASE = "https://vizier.cds.unistra.fr/viz-bin/asu-tsv"

#: key -> (VizieR source, column list, RA column, Dec column, row cap)
CATALOGUES = {
    "mwsc": ("J/A+A/558/A53", "MWSC,RAJ2000,DEJ2000", "RAJ2000", "DEJ2000", 4000),
    "cg20": ("J/A+A/633/A99", "Cluster,RA_ICRS,DE_ICRS", "RA_ICRS", "DE_ICRS", 4000),
    "hr23": ("J/A+A/673/A114", "Name,RA_ICRS,DE_ICRS", "RA_ICRS", "DE_ICRS", 8000),
}

#: The catalogue's own names, URLs and licences, carried into the provenance record.
PROVENANCE = {
    "mwsc": {
        "source": "VizieR J/A+A/558/A53",
        "title": "Milky Way global survey of star clusters. II. (Kharchenko+, 2013)",
        "bibcode": "2013A&A...558A..53K",
        "url": "https://cdsarc.cds.unistra.fr/viz-bin/cat/J/A+A/558/A53",
        "route": BASE + "?-source=J/A+A/558/A53",
        "licence": "CDS/VizieR catalogue terms (https://cds.unistra.fr/vizier-org/licences_vizier.html)",
        "columnsUsed": ["MWSC (A4) identifier", "RAJ2000", "DEJ2000"],
        "field": "adopted cluster centre",
    },
    "cg20": {
        "source": "VizieR J/A+A/633/A99",
        "title": "Clusters in Gaia DR2 (Cantat-Gaudin & Anders, 2020)",
        "bibcode": "2020A&A...633A..99C",
        "url": "https://cdsarc.cds.unistra.fr/viz-bin/cat/J/A+A/633/A99",
        "route": BASE + "?-source=J/A+A/633/A99",
        "licence": "CDS/VizieR catalogue terms (https://cds.unistra.fr/vizier-org/licences_vizier.html)",
        "columnsUsed": ["Cluster identifier", "RA_ICRS", "DE_ICRS"],
        "field": "cluster centre from Gaia DR2 members",
    },
    "hr23": {
        "source": "VizieR J/A+A/673/A114",
        "title": "A Gaia DR3-based catalogue of open clusters (Hunt & Reffert, 2023)",
        "bibcode": "2023A&A...673A.114H",
        "url": "https://cdsarc.cds.unistra.fr/viz-bin/cat/J/A+A/673/A114",
        "route": BASE + "?-source=J/A+A/673/A114",
        "licence": "CDS/VizieR catalogue terms (https://cds.unistra.fr/vizier-org/licences_vizier.html)",
        "columnsUsed": ["Name identifier", "RA_ICRS", "DE_ICRS"],
        "field": "cluster centre from Gaia DR3 members",
    },
}


def fetch(source: str, out_cols: str, max_rows: int) -> str:
    url = f"{BASE}?-source={source}&-out={out_cols}&-out.max={max_rows}"
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=900) as response:
        return response.read().decode("utf-8", "replace")


def parse(text: str, ra_name: str, dec_name: str) -> list[dict]:
    """Read an ``asu-tsv`` payload by column **name**, never by position.

    The header block ends with a names row, a units row and a dashed rule. The names row
    is the one that carries the requested column names; the unit strings above the rule
    do not. Matching is exact, because `RAJ2000` and `DEJ2000` end in digits and a
    digit-stripping comparison would silently look for `RAJ`/`DEJ` instead.
    """
    lines = [ln for ln in text.splitlines() if ln.strip() and not ln.startswith("#")]
    sep = next((i for i, ln in enumerate(lines)
                if set(ln.replace("\t", "").replace(" ", "")) <= set("-|")), None)
    if sep is None:
        raise SystemExit("no column rule found in the VizieR payload")
    header = None
    for cand in reversed(lines[:sep]):
        cols = [c.strip() for c in cand.split("\t")]
        if ra_name in cols and dec_name in cols:
            header = cols
            break
    if header is None:
        raise SystemExit(f"columns {ra_name}/{dec_name} not found in {lines[:sep]!r}")
    ra_i, dec_i = header.index(ra_name), header.index(dec_name)
    out = []
    for line in lines[sep + 1:]:
        cells = line.split("\t")
        if len(cells) <= max(ra_i, dec_i):
            continue
        try:
            out.append({"name": cells[0].strip(),
                        "ra": float(cells[ra_i]), "dec": float(cells[dec_i])})
        except ValueError:
            continue
    return out


def main() -> int:
    result: dict[str, list] = {}
    for key, (source, cols, ra_name, dec_name, mx) in CATALOGUES.items():
        text = fetch(source, cols, mx)
        rows = parse(text, ra_name, dec_name)
        if not rows:
            raise SystemExit(f"{key}: fetched {len(text):,} B but parsed no rows")
        result[key] = rows
        print(f"{key:5s} {source:16s} {len(rows):6d} rows  ({len(text):,} B)")

    write_json(SCRATCH / "cluster-centres.json",
               {"retrieved": "2026-10-09", "route": "VizieR asu-tsv",
                "provenance": PROVENANCE, "catalogues": result})
    print(f"wrote {SCRATCH / 'cluster-centres.json'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
