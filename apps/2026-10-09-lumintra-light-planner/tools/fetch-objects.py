#!/usr/bin/env python3
"""fetch-objects.py — gather the two-source evidence behind `catalog/objects.json`.

Nothing here authors a number. This script only *collects what independent services
publish*, keyed to a designation, and writes one evidence document that
`bake-objects.py` then merges, tests and renders into the shipped asset.

Sources
  A  SIMBAD TAP (CDS, Strasbourg) — ICRS J2000 coordinates, object class, angular size
     (`galdim_majaxis` / `galdim_minaxis` / `galdim_angle`) and the aggregated V and B
     magnitudes. This is srcA for every row.
  B  NASA HEASARC — the W3Browse `messier` table, read through HEASARC's TAP service.
     It carries an independent position, a V magnitude, a `26X14`-style major/minor
     axis and the NGC designation, for 109 of the 110 Messier objects. It is srcB for
     every Messier row, which is the second source `specs/target-catalogue/spec.md`
     names first ("NASA HEASARC (freely usable, US government)").
  C  Wikidata SPARQL — CC0 1.0 structured data. Matched to a SIMBAD designation through
     `P528` (catalog code) or `P3083` (SIMBAD ID); supplies `P6257` right ascension,
     `P6258` declination and `P1215` apparent magnitude, plus the en/ru labels, the
     English aliases and the full catalogue-code set. It is srcB for the non-Messier
     rows, where no HEASARC table reaches.

A third lookup is recorded, never averaged: the row's Wikidata item id, its aliases and
its other catalogue codes go into `sourceIds` / `aliases`; a magnitude pair more than
0.50 apart becomes `magDisputed` and both numbers are written into `notes` by the baker.

Measured behaviour that shaped the code (all 2026-10-09):
  * HEASARC's TAP answers every query as a VOTable document and ignores `FORMAT` /
    `RESPONSEFORMAT` — `FORMAT=json` is refused outright and `FORMAT=csv` still returns
    the VOTable. The rows are therefore decoded from `<BINARY><STREAM>` by
    `common.votable_rows`.
  * HEASARC's `messier` table holds **109** rows, not 110.
  * SIMBAD writes NGC identifiers padded (`NGC  4664`); Wikidata's `P528` does not
    (`NGC 4664`). Designations are normalised before either is queried.
  * SIMBAD's `allfluxes.V` is null for some objects whose brightness is published only in
    B (M1 among them); the baker keeps `magB` and `magSystem="B"` for exactly that case.

Output (all under <repo>/.kiln/scratch/lumintra-assets/, never the system temp):
  simbad-deepsky.json   every NGC/IC deep-sky object with a V magnitude <= POOL_LIMIT
  heasarc-messier.json  the HEASARC messier table, decoded
  wikidata-objects.json the Wikidata rows matched to those designations
  objects-evidence.json the merged document bake-objects.py consumes
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from common import SCRATCH, fetch, sha256_bytes, sparql, tap_sync, votable_rows, write_json  # noqa: E402

SIMBAD_TAP = "https://simbad.cds.unistra.fr/simbad/sim-tap"
HEASARC_TAP = "https://heasarc.gsfc.nasa.gov/xamin/vo/tap/sync"
RETRIEVED = "2026-10-09"

#: The candidate pool is opened wide (10.5) on purpose: the selection rule in the spec
#: caps at 10.0 and only raises to 10.5 if fewer than 140 non-Messier rows qualify. The
#: pool must therefore contain everything the raise could reach, or the fallback would be
#: decided by the fetch rather than by the data.
POOL_LIMIT = 10.5

#: SIMBAD object classes that are deep-sky objects rather than stars. Anything outside
#: this set is excluded at the query, so the pool is not polluted by the 484 field stars
#: that carry an NGC designation in SIMBAD's identifier table.
DEEP_SKY_OTYPES = (
    "G", "GiC", "GiG", "Sy1", "Sy2", "AGN", "EmG", "rG", "LSB", "bCG", "IG", "SBG",
    "PaG", "LIN", "GPair", "GTrpl", "GGroup", "Cl*", "OpC", "GlC", "GNe", "RNe", "PN",
    "PN?", "HII", "SNR", "Neb", "DNe", "Cld", "ISM", "cor", "bub", "MoC", "As*",
)

#: SIMBAD's short class codes -> the closed `type` enum of the target-catalogue spec.
SIMBAD_TYPE_MAP = {
    "G": "galaxy", "GiC": "galaxy", "GiG": "galaxy", "Sy1": "galaxy", "Sy2": "galaxy",
    "AGN": "galaxy", "EmG": "galaxy", "rG": "galaxy", "LSB": "galaxy", "bCG": "galaxy",
    "IG": "galaxy", "SBG": "galaxy", "PaG": "galaxy", "LIN": "galaxy",
    "GPair": "galaxy_group", "GTrpl": "galaxy_group", "GGroup": "galaxy_group",
    "Cl*": "open_cluster", "OpC": "open_cluster",
    "GlC": "globular_cluster",
    "GNe": "emission_nebula", "HII": "emission_nebulahii",  # placeholder, corrected below
    "RNe": "reflection_nebula",
    "PN": "planetary_nebula", "PN?": "planetary_nebula",
    "SNR": "supernova_remnant",
    "Neb": "emission_nebula", "DNe": "dark_nebula", "Cld": "dark_nebula",
    "ISM": "dark_nebula", "cor": "dark_nebula", "bub": "dark_nebula",
    "MoC": "dark_nebula",
    "As*": "asterism",
}
SIMBAD_TYPE_MAP["HII"] = "emission_nebula"

#: HEASARC `object_type` -> the same enum, used only as a cross-check on SIMBAD's.
HEASARC_TYPE_MAP = {
    "S": "galaxy", "SB": "galaxy", "E": "galaxy", "I": "galaxy", "P": "galaxy",
    "IR": "galaxy", "GAL": "galaxy",
    "OC": "open_cluster", "GB": "globular_cluster", "GC": "globular_cluster",
    "PN": "planetary_nebula", "SNR": "supernova_remnant",
    "DN": "dark_nebula", "RN": "reflection_nebula", "EN": "emission_nebula",
    "NEB": "emission_nebula", "HII": "emission_nebula",
    "AST": "asterism", "DS": "double_star", "**": "double_star",
}

_SPACE = re.compile(r"\s+")


def normalise_designation(text: str) -> str:
    """`NGC  4664` / `M   31` -> `NGC 4664` / `M 31` — the one form both sources use."""
    return _SPACE.sub(" ", (text or "").strip())


def _f(value):
    """SIMBAD hands back floats, ints or None; JSON nulls and empties become None."""
    if value is None or value == "":
        return None
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


# --------------------------------------------------------------------------- source A
def fetch_simbad() -> tuple[list[dict], list[dict]]:
    """The Messier set and the whole deep-sky NGC/IC pool, both straight from SIMBAD."""
    fields = ("b.main_id, b.ra, b.dec, b.otype, b.otype_txt, b.galdim_majaxis, "
              "b.galdim_minaxis, b.galdim_angle, b.morph_type, f.V, f.B")

    messier_q = (
        f"SELECT i.id AS messier, {fields} FROM ident AS i "
        "JOIN basic AS b ON i.oidref = b.oid "
        "LEFT JOIN allfluxes AS f ON f.oidref = b.oid "
        "WHERE i.id LIKE 'M %'"
    )
    messier = tap_sync(SIMBAD_TAP, messier_q)
    rows_m = [dict(zip([c["name"] for c in messier["metadata"]], r)) for r in messier["data"]]

    otypes = ", ".join(f"'{o}'" for o in DEEP_SKY_OTYPES)
    pool_q = (
        f"SELECT i.id AS desig, {fields} FROM ident AS i "
        "JOIN basic AS b ON i.oidref = b.oid "
        "JOIN allfluxes AS f ON f.oidref = b.oid "
        f"WHERE (i.id LIKE 'NGC %' OR i.id LIKE 'IC %') AND f.V <= {POOL_LIMIT} "
        f"AND b.otype IN ({otypes})"
    )
    pool = tap_sync(SIMBAD_TAP, pool_q)
    rows_p = [dict(zip([c["name"] for c in pool["metadata"]], r)) for r in pool["data"]]

    # One object can carry several NGC/IC identifiers; the brightest-designation row wins,
    # and a true duplicate (same main_id) is collapsed.
    seen, deduped = {}, []
    for row in rows_p:
        row["desig"] = normalise_designation(row["desig"])
        row["main_id"] = normalise_designation(row["main_id"])
        row["messier"] = None
        if row["main_id"] in seen:
            continue
        seen[row["main_id"]] = True
        deduped.append(row)

    for row in rows_m:
        row["messier"] = normalise_designation(row["messier"])
        row["main_id"] = normalise_designation(row["main_id"])

    # The Messier set's own NGC/IC designations, so a Messier row can carry `ngc`/`ic`
    # and a non-Messier row can be excluded from the supplement.
    mapping_q = (
        "SELECT i1.id AS messier, i2.id AS desig FROM ident AS i1 "
        "JOIN ident AS i2 ON i1.oidref = i2.oidref "
        "WHERE i1.id LIKE 'M %' AND (i2.id LIKE 'NGC %' OR i2.id LIKE 'IC %')"
    )
    mapping = tap_sync(SIMBAD_TAP, mapping_q)
    by_messier: dict[str, list[str]] = {}
    for m, d in mapping["data"]:
        by_messier.setdefault(normalise_designation(m), []).append(normalise_designation(d))
    for row in rows_m:
        row["designations"] = sorted(by_messier.get(row["messier"], []))

    return rows_m, deduped


# --------------------------------------------------------------------------- source B
def fetch_heasarc_messier() -> list[dict]:
    """NASA HEASARC's `messier` table. VOTable-BINARY only; see the module docstring."""
    raw = fetch(HEASARC_TAP,
                data={"REQUEST": "doQuery", "LANG": "ADQL", "FORMAT": "votable",
                      "QUERY": "SELECT * FROM messier"},
                suffix=".xml")
    rows = votable_rows(raw)
    for row in rows:
        row["name"] = normalise_designation(row.get("name"))
        row["alt_name"] = normalise_designation(row.get("alt_name"))
    return rows


# --------------------------------------------------------------------------- source C
WIKIDATA_QUERY = """SELECT ?item ?key ?code ?ra ?dec ?mag ?enLabel ?ruLabel ?altLabel WHERE {
  VALUES ?key { %s }
  { ?item wdt:P528 ?key } UNION { ?item wdt:P3083 ?key }
  OPTIONAL { ?item wdt:P528 ?code . }
  OPTIONAL { ?item p:P6257 ?s1 . ?s1 psv:P6257 ?v1 . ?v1 wikibase:quantityAmount ?ra . }
  OPTIONAL { ?item p:P6258 ?s2 . ?s2 psv:P6258 ?v2 . ?v2 wikibase:quantityAmount ?dec . }
  OPTIONAL { ?item wdt:P1215 ?mag . }
  OPTIONAL { ?item rdfs:label ?enLabel . FILTER(lang(?enLabel) = "en") }
  OPTIONAL { ?item rdfs:label ?ruLabel . FILTER(lang(?ruLabel) = "ru") }
  OPTIONAL { ?item skos:altLabel ?altLabel . FILTER(lang(?altLabel) = "en") }
}"""


def fetch_wikidata(keys: list[str], chunk: int = 80) -> dict:
    """Wikidata CC0 rows keyed by the designation they matched on."""
    out: dict[str, list[dict]] = {}
    failures: list[str] = []
    for i in range(0, len(keys), chunk):
        part = keys[i:i + chunk]
        values = " ".join('"%s"' % k.replace('"', '') for k in part)
        try:
            bindings = sparql(WIKIDATA_QUERY % values)
        except Exception as exc:  # a chunk that times out must be visible, not silent
            failures.append(f"chunk {i // chunk} ({part[0]}..{part[-1]}): {exc!r}")
            continue
        for b in bindings:
            row = {name: b[name]["value"] for name in b}
            key = row.get("key")
            out.setdefault(key, []).append(row)
        print(f"  wikidata chunk {i // chunk + 1}: {len(bindings)} bindings", flush=True)
    if failures:
        print("  WIKIDATA CHUNK FAILURES:", *failures, sep="\n    ", flush=True)
    return out


# --------------------------------------------------------------------------- source D
#: VizieR's `B/ocl` (Dias et al., *Optically visible open clusters and candidates*) is the
#: compilation the loose-cluster centres actually come from. It is fetched because it is the
#: only way to tell a *disagreement between sources* from a *mis-matched designation*: when
#: Wikidata's cluster position sits on `B/ocl`'s to within a hundredth of an arcminute, the
#: two are not independent and the resolver must not treat their agreement as verification.
VIZIER_TAP = "https://tapvizier.cds.unistra.fr/TAPVizieR/tap"


def fetch_bocl() -> list:
    rows = tap_sync(VIZIER_TAP,
                    'SELECT "Cluster", "RAJ2000", "DEJ2000", "Diam" FROM "B/ocl/clusters"')
    return [[r[0], float(r[1]), float(r[2]), r[3]] for r in rows["data"]]


def main() -> int:
    print("source A: SIMBAD", flush=True)
    messier, pool = fetch_simbad()
    print(f"  messier={len(messier)} pool={len(pool)}", flush=True)

    print("source B: NASA HEASARC messier", flush=True)
    heasarc = fetch_heasarc_messier()
    print(f"  heasarc rows={len(heasarc)}", flush=True)

    print("source C: Wikidata", flush=True)
    keys: list[str] = []
    for row in messier:
        keys.append(row["main_id"])
        keys.extend(row["designations"])
    for row in pool:
        keys.append(row["main_id"])
        keys.append(row["desig"])
    keys = sorted({normalise_designation(k) for k in keys if k})
    wikidata = fetch_wikidata(keys)
    print(f"  wikidata keys={len(keys)} matched={len(wikidata)}", flush=True)

    write_json(SCRATCH / "simbad-deepsky.json",
               {"retrieved": RETRIEVED, "poolLimit": POOL_LIMIT,
                "messier": messier, "pool": pool})
    write_json(SCRATCH / "heasarc-messier.json",
               {"retrieved": RETRIEVED, "rows": heasarc})
    write_json(SCRATCH / "wikidata-objects.json",
               {"retrieved": RETRIEVED, "keys": keys, "rows": wikidata})

    print("source D: VizieR B/ocl open-cluster centres", flush=True)
    ocl = fetch_bocl()
    print(f"  B/ocl rows={len(ocl)}", flush=True)
    write_json(SCRATCH / "vizier-bocl.json", ocl)

    evidence = {
        "retrieved": RETRIEVED,
        "poolLimit": POOL_LIMIT,
        "sources": {
            "simbad": {
                "label": "SIMBAD (CDS, Strasbourg) TAP — basic / allfluxes / ident",
                "url": SIMBAD_TAP,
            },
            "heasarc": {
                "label": "NASA HEASARC W3Browse `messier` table, via HEASARC TAP",
                "url": HEASARC_TAP,
            },
            "wikidata": {
                "label": "Wikidata structured data (CC0 1.0): P528/P3083 match, "
                         "P6257 RA, P6258 Dec, P1215 magnitude",
                "url": "https://query.wikidata.org/sparql",
            },
            "vizier-bocl": {
                "label": "VizieR B/ocl — Dias et al., optically visible open clusters",
                "url": VIZIER_TAP,
            },
        },
        "messier": messier,
        "pool": pool,
        "heasarc": heasarc,
        "wikidata": wikidata,
    }
    blob = json.dumps(evidence, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    (SCRATCH / "objects-evidence.json").write_bytes(blob)
    print(f"\nobjects-evidence.json  {len(blob):,} B  sha256 {sha256_bytes(blob)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
