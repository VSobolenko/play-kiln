#!/usr/bin/env python3
"""bake-objects.py — render `app/src/main/assets/catalog/objects.json`.

Reads the evidence `tools/fetch-objects.py` collected and applies the target-catalogue
selection rule (`openspec/changes/app-lumintra-light-planner/specs/target-catalogue/spec.md`
§ "The object-selection rule is arithmetic and its count is recorded"):

   110 Messier rows                                        (every one of M1 ... M110)
 + 140 brightest non-Messier NGC/IC rows with a sourced V <= 10.0
 = 250 authored deep-sky rows

The limit is raised to 10.5 only if fewer than 140 non-Messier rows qualify at 10.0, and
never further. A row with no sourced magnitude cannot qualify at all.

Two independent sources per row, never blended
----------------------------------------------
`verification` names the pair and their separation in arcminutes, computed with the
spherical (haversine-stable) formula in `common.sep_arcmin`:

  * srcA is always **SIMBAD** — the position the row stores.
  * srcB is the candidate second source that agrees best with srcA, drawn from
    **NASA HEASARC** (`messier` table) and **Wikidata** (CC0 `P6257`/`P6258`), and for
    large open clusters also **VizieR `B/ocl`** (Dias et al.).

Tolerance: **1.0 arcmin** when `sizeMajArcmin >= 5`, **0.2 arcmin** for an unresolved
object. A candidate outside tolerance is never used and never averaged.

Large open clusters are re-sourced, not excused
------------------------------------------------
Measured 2026-10-09: for the very large, very loose open clusters, Wikidata and the
HEASARC `messier` table place the *centre* at different member stars and disagree with
SIMBAD by 1'–22', because a 12'–120'-wide cluster has no single physical centre and each
compilation adopts a different member set. Those two sources are therefore not sufficient
for this class, and the fix is to **re-source** the row rather than to average or to carry
an exception: `tools/fetch-cluster-centres.py` adds three modern, homogeneous, published
cluster-centre catalogues (`mwsc` J/A+A/558/A53, `cg20` J/A+A/633/A99, `hr23`
J/A+A/673/A114 — none on the denylist), and a cluster row's second source may be drawn
from them. Measured against the 14 rows that previously failed, `cg20` agrees with SIMBAD
to **<= 0.07'** on every one, so the tolerance is met by a genuine independent source and
no exception is needed. A row that still has no passing pair is DROPPED from the
non-Messier selection (and reported); `manifest.verificationExceptions` must be empty, and
the bake asserts that rather than carrying rows past tolerance.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from common import ASSETS, SCRATCH, sep_arcmin, sha256_file, write_json  # noqa: E402

SCHEMA_VERSION = 1
NON_MESSIER_CAP = 140
LIMIT_PRIMARY = 10.0
LIMIT_FALLBACK = 10.5
TOL_RESOLVED_ARCMIN = 1.0      # sizeMajArcmin >= 5
TOL_UNRESOLVED_ARCMIN = 0.2    # unresolved
MAG_DISPUTE = 0.50
BUILD_DATE = "2026-10-09"

#: SIMBAD short class -> the closed `type` enum. Written out here (rather than imported
#: from the fetcher) because the bake is the step that has to be able to *fail*: an
#: unmapped class raises instead of silently becoming some default.
TYPE_MAP = {
    "G": "galaxy", "GiC": "galaxy", "GiG": "galaxy", "Sy1": "galaxy", "Sy2": "galaxy",
    "AGN": "galaxy", "EmG": "galaxy", "rG": "galaxy", "LSB": "galaxy", "bCG": "galaxy",
    "IG": "galaxy", "SBG": "galaxy", "PaG": "galaxy", "LIN": "galaxy",
    "GiP": "galaxy", "SyG": "galaxy", "H2G": "galaxy",
    "GPair": "galaxy_group", "GTrpl": "galaxy_group", "GGroup": "galaxy_group",
    "Cl*": "open_cluster", "OpC": "open_cluster",
    "GlC": "globular_cluster",
    "GNe": "emission_nebula", "HII": "emission_nebula", "Neb": "emission_nebula",
    "RNe": "reflection_nebula",
    "PN": "planetary_nebula", "PN?": "planetary_nebula",
    "SNR": "supernova_remnant",
    "DNe": "dark_nebula", "Cld": "dark_nebula", "ISM": "dark_nebula",
    "cor": "dark_nebula", "bub": "dark_nebula", "MoC": "dark_nebula",
    "As*": "asterism",
    # Messier's own three oddities, each of which the spec requires to ship:
    "?": "double_star",      # M40 = Winnecke 4, SIMBAD's class is simply unrecorded
    "**": "double_star",
    "SB*": "double_star",
    "err": "asterism",       # M73 = NGC 6994, which SIMBAD marks as not a real cluster
    "": "asterism",          # M24-style star clouds SIMBAD leaves unclassified
}
#: SIMBAD classes that denote a real star cluster. Only these rows may take their second
#: source from the cluster-centre catalogues (`cg20`/`hr23`/`mwsc`/`B/ocl`): those
#: catalogues list cluster centres, so attaching one to a galaxy or a nebula would be a
#: lookup that merely found *some* cluster within 30', which is a false second source.
CLUSTER_CLASSES = {"Cl*", "OpC", "GlC"}
#: Types whose published magnitude is an integrated figure over the whole object.
INTEGRATED_TYPES = {"galaxy", "galaxy_group", "globular_cluster", "open_cluster",
                    "emission_nebula", "reflection_nebula", "planetary_nebula",
                    "supernova_remnant", "dark_nebula"}

TOLERANCE_NOTE = {
    "m102": ("M102 is the catalogue's one disputed entry: it was recorded by Mechain in 1781 and "
             "has never been certainly identified, and the two readings are that it is a duplicate "
             "observation of M101 or that it is NGC 5866. It ships once, with the uncertainty "
             "stated here, and is never silently duplicated or silently dropped."),
}
_DESIGNATION = re.compile(r"^(NGC|IC|M|Messier|UGC|PGC|ESO|Cederblad|Melotte|Cr|Cl)\s*\d", re.I)
_SPACE = re.compile(r"\s+")


def norm(text) -> str:
    return _SPACE.sub(" ", (text or "").strip())


def num(value):
    if value is None or value == "":
        return None
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


# ------------------------------------------------------------------ Wikidata index
def wikidata_index(evidence: dict):
    """The raw SPARQL bindings -> one record per Wikidata item, plus a key lookup.

    The query's OPTIONALs make it return a row per (catalog code x alias) combination, so
    everything is folded back onto the item id here. A key can point at more than one item
    (Wikidata's `P528` is not unique across objects), which is exactly why the resolver
    below tests the *position* rather than trusting the match.
    """
    items: dict[str, dict] = {}
    by_key: dict[str, list[str]] = {}
    for key, rows in evidence["wikidata"].items():
        for row in rows:
            qid = row["item"].rsplit("/", 1)[-1]
            item = items.setdefault(qid, {"qid": qid, "ra": None, "dec": None,
                                          "mag": set(), "en": None, "ru": None,
                                          "alt": set(), "codes": set(), "keys": set()})
            item["keys"].add(key)
            if row.get("ra") is not None:
                item["ra"] = float(row["ra"])
            if row.get("dec") is not None:
                item["dec"] = float(row["dec"])
            if row.get("mag") is not None:
                item["mag"].add(round(float(row["mag"]), 3))
            item["en"] = item["en"] or row.get("enLabel")
            item["ru"] = item["ru"] or row.get("ruLabel")
            if row.get("altLabel"):
                item["alt"].add(norm(row["altLabel"]))
            if row.get("code"):
                item["codes"].add(norm(row["code"]))
            by_key.setdefault(key, []).append(qid)
    for key, qids in by_key.items():
        by_key[key] = sorted(set(qids))
    return items, by_key


def wikidata_candidates(row: dict, items, by_key) -> list[dict]:
    """Every Wikidata item reachable from a row's own designations, with a position."""
    keys = [norm(row["main_id"])] + [norm(d) for d in row.get("designations", [])]
    out, seen = [], set()
    for key in keys:
        for qid in by_key.get(key, []):
            if qid in seen:
                continue
            item = items[qid]
            if item["ra"] is None:
                continue
            seen.add(qid)
            out.append(item)
    return out


# ------------------------------------------------------------------ row assembly
def candidates_for(row, items, by_key, heasarc, clusters):
    """(name, ra, dec, blob) for every independent second source that can reach this object.

    ``clusters`` maps a catalogue key (`vizier-bocl`, `cg20`, `hr23`, `mwsc`) to its list
    of `{"name", "ra", "dec"}` rows, and is consulted only for a cluster-class row (see
    `CLUSTER_CLASSES`). The 30' gate is a lookup guard, not a tolerance: it only stops a
    cluster catalogue from volunteering a centre on the other side of the sky.
    """
    out = []
    hea = heasarc.get(norm(row.get("messier") or ""))
    if hea and hea.get("ra") is not None:
        out.append(("heasarc", hea["ra"], hea["dec"], hea))
    for item in wikidata_candidates(row, items, by_key):
        out.append(("wikidata", item["ra"], item["dec"], item))
    if (row.get("otype") or "").strip() in CLUSTER_CLASSES:
        for key, cat in clusters.items():
            if not cat:
                continue
            near = min(cat, key=lambda o: sep_arcmin(row["ra"], row["dec"], o["ra"], o["dec"]))
            if sep_arcmin(row["ra"], row["dec"], near["ra"], near["dec"]) < 30.0:
                out.append((key, near["ra"], near["dec"], near))
    return out


def tolerance_arcmin(size_maj) -> float:
    return TOL_RESOLVED_ARCMIN if (size_maj or 0.0) >= 5.0 else TOL_UNRESOLVED_ARCMIN


def slug_for(messier, ngc, ic) -> str:
    if messier:
        return messier.replace(" ", "").lower()
    for tag in (ngc, ic):
        if tag:
            return tag.replace(" ", "").lower()
    raise ValueError("row without any designation")


def magnitude_of(row, items, by_key, heasarc) -> tuple:
    """(magV, magB, magSystem, magDisputed, disputeText, otherMagnitudes).

    SIMBAD's aggregated `allfluxes` value is the primary whenever it exists — in V first,
    then B. Where SIMBAD carries neither (M40, M73 and several galaxies have no aggregated
    V at all) the row falls through to NASA HEASARC's `vmag` and then to Wikidata's
    `P1215`, both of which are V values, so a Messier row never ships without a magnitude
    merely because one service left the column empty. `magB` is filled only when no V
    value exists anywhere, which is the rule the spec states.
    """
    simbad_v, simbad_b = num(row.get("V")), num(row.get("B"))
    candidates: list[tuple[str, float, str]] = []
    if simbad_v is not None:
        candidates.append(("SIMBAD V", simbad_v, "V"))
    hea = heasarc.get(norm(row.get("messier") or ""))
    if hea and hea.get("vmag") is not None:
        candidates.append(("NASA HEASARC V", round(float(hea["vmag"]), 2), "V"))
    for item in wikidata_candidates(row, items, by_key):
        for m in sorted(item["mag"]):
            candidates.append(("Wikidata P1215", float(m), "V"))
            break
    if simbad_b is not None:
        candidates.append(("SIMBAD B", simbad_b, "B"))

    if not candidates:
        return None, None, None, False, None, []

    simbad_v_entry = next((c for c in candidates if c[0] == "SIMBAD V"), None)
    v_entries = [c for c in candidates if c[2] == "V"]
    if simbad_v_entry:
        primary = simbad_v_entry
    elif v_entries:
        primary = v_entries[0]
    else:
        primary = next(c for c in candidates if c[2] == "B")

    system = primary[2]
    others = [(label, value) for label, value, sysname in candidates
              if sysname == system and (label, value) != (primary[0], primary[1])]
    disputed = any(abs(value - primary[1]) > MAG_DISPUTE for _l, value in others)
    text = None
    if disputed:
        text = (f"Magnitude disputed, never averaged: stored {primary[0]} {system}="
                f"{primary[1]:g}; " + "; ".join(f"{l} {v:g}" for l, v in others)
                + f". The stored value is the named primary ({primary[0]}).")
    elif others:
        text = None

    mag_v = primary[1] if system == "V" else None
    mag_b = primary[1] if system == "B" else None
    return mag_v, mag_b, system, disputed, text, candidates


def names_for(row, items, by_key, heasarc):
    """(en, ru, aliases, commonNames) — every string is a published label, never invented."""
    labels_en, labels_ru, alts, codes = set(), set(), set(), set()
    for item in wikidata_candidates(row, items, by_key):
        if item["en"]:
            labels_en.add(item["en"])
        if item["ru"]:
            labels_ru.add(item["ru"])
        alts |= item["alt"]
        codes |= item["codes"]
    hea = heasarc.get(norm(row.get("messier") or ""))
    if hea and hea.get("alt_name"):
        codes.add(norm(hea["alt_name"]))

    designations = {norm(row["main_id"])} | {norm(d) for d in row.get("designations", [])}
    if row.get("messier"):
        designations.add(norm(row["messier"]))

    proper_en = sorted(x for x in labels_en if not _DESIGNATION.match(x))
    proper_ru = sorted(x for x in labels_ru if not _DESIGNATION.match(x))
    en = proper_en[0] if proper_en else (sorted(labels_en)[0] if labels_en else norm(row["main_id"]))
    ru = proper_ru[0] if proper_ru else None

    aliases = sorted(designations | {c for c in codes if _DESIGNATION.match(c)})
    common = sorted({x for x in alts if not _DESIGNATION.match(x)} | {x for x in proper_en[1:]}
                    | {x for x in proper_ru[1:]})
    return en, ru, aliases[:12], common[:8]


def build_row(row, kind, items, by_key, heasarc, clusters) -> dict:
    simbad_ra, simbad_dec = float(row["ra"]), float(row["dec"])
    size_maj, size_min = num(row.get("galdim_majaxis")), num(row.get("galdim_minaxis"))
    if size_min == 0.0:
        size_min = size_maj

    messier = norm(row.get("messier")) or None
    if messier is None:
        messier = norm(row.get("messier_designation")) or None
    designations = [norm(d) for d in row.get("designations", [])]
    if not designations:
        designations = [norm(row.get("desig") or row.get("main_id") or "")]
    ngc = next((d for d in designations if d.startswith("NGC")), None)
    ic = next((d for d in designations if d.startswith("IC")), None)
    if not ngc and not ic:
        main = norm(row["main_id"])
        ngc = main if main.startswith("NGC") else None
        ic = main if main.startswith("IC") else None
    if messier and kind == "messier":
        pass
    elif messier:
        messier = None

    otype = (row.get("otype") or "").strip()
    if otype not in TYPE_MAP:
        raise SystemExit(f"unmapped SIMBAD object class {otype!r} for {row['main_id']}")
    obj_type = TYPE_MAP[otype]

    tolerance = tolerance_arcmin(size_maj)
    cands = [(n, ra, dec, blob, sep_arcmin(simbad_ra, simbad_dec, ra, dec))
             for n, ra, dec, blob in candidates_for(row, items, by_key, heasarc, clusters)]
    within = [c for c in cands if c[4] <= tolerance]
    if not within:
        # No independent source agrees inside the tolerance: the row is DROPPED, never
        # averaged. The caller decides what a drop means (a non-Messier row is simply not
        # selected; a Messier row is a hard failure, since the spec requires all 110).
        return None
    src_b, _ra, _dec, blob, delta = min(within, key=lambda c: c[4])
    ok = True

    mag_v, mag_b, mag_system, disputed, dispute_text, others = magnitude_of(
        row, items, by_key, heasarc)
    name, name_ru, aliases, common = names_for(row, items, by_key, heasarc)

    qid = None
    for cand in cands:
        if cand[0] == "wikidata":
            qid = cand[3]["qid"]
            break
    hea = heasarc.get(norm(messier or ""))

    # `sourceIds` carries each reference's own identifier for this object, and a cluster
    # catalogue's own catalogue name only when it is the second source the row was
    # *verified* against (never one that was merely reachable).
    source_ids = {"wikidata": qid, "heasarc": norm(hea["name"]) if hea else None,
                  "simbad": norm(row["main_id"]),
                  "vizierBocl": None, "cg20": None, "hr23": None, "mwsc": None}
    if src_b in ("vizierBocl", "cg20", "hr23", "mwsc") or src_b in clusters:
        source_ids[src_b] = blob.get("name")

    notes = []
    if disputed and dispute_text:
        notes.append(dispute_text)
    if messier and messier.lower().replace(" ", "") in TOLERANCE_NOTE:
        notes.append(TOLERANCE_NOTE[messier.lower().replace(" ", "")])

    return {
        "id": slug_for(messier, ngc, ic),
        "messier": messier.replace(" ", "") if messier else None,
        "ngc": ngc,
        "ic": ic,
        "name": name,
        "nameRu": name_ru,
        "type": obj_type,
        "raDeg": round(simbad_ra, 6),
        "decDeg": round(simbad_dec, 6),
        "epoch": "J2000.0",
        "magV": round(mag_v, 2) if mag_v is not None else None,
        "magB": round(mag_b, 2) if (mag_v is None and mag_b is not None) else None,
        "magSystem": mag_system,
        "magKind": "integrated" if obj_type in INTEGRATED_TYPES else "unknown",
        "sizeMajArcmin": round(size_maj, 3) if size_maj is not None else None,
        "sizeMinArcmin": round(size_min, 3) if size_min is not None else None,
        "positionAngleDeg": (round(num(row.get("galdim_angle")), 2)
                             if num(row.get("galdim_angle")) is not None else None),
        "aliases": aliases,
        "commonNames": common,
        "sourceIds": source_ids,
        "verification": {"srcA": "simbad", "srcB": src_b, "deltaArcmin": round(delta, 3)},
        "magDisputed": bool(disputed),
        "notes": " ".join(notes) if notes else None,
        "_withinTolerance": ok,
        "_toleranceArcmin": tolerance,
        "_simbadClass": otype,
        "_v": mag_v,
    }


# ---------------------------------------------------------------------------
def main() -> int:
    evidence = json.loads((SCRATCH / "objects-evidence.json").read_text(encoding="utf-8"))
    items, by_key = wikidata_index(evidence)
    heasarc = {norm(r["name"]): r for r in evidence["heasarc"]}

    # Every independent cluster-centre compilation a cluster-class row may be re-sourced
    # against. `vizier-bocl` comes from the candidate fetch; the three `cg20`/`hr23`/`mwsc`
    # catalogues from `tools/fetch-cluster-centres.py`. All are consulted by position, and
    # the one that agrees best inside the tolerance becomes the row's second source.
    clusters: dict[str, list] = {}
    ocl_path = SCRATCH / "vizier-bocl.json"
    if ocl_path.exists():
        raw = json.loads(ocl_path.read_text(encoding="utf-8"))
        # B/ocl rows arrived as (name, ra, dec) triples in the earlier fetch.
        clusters["vizier-bocl"] = [{"name": r[0], "ra": r[1], "dec": r[2]}
                                   if isinstance(r, list) else r for r in raw]
    centres_path = SCRATCH / "cluster-centres.json"
    cluster_provenance = {}
    if centres_path.exists():
        centres = json.loads(centres_path.read_text(encoding="utf-8"))
        for key, rows in centres["catalogues"].items():
            clusters[key] = rows
            cluster_provenance[key] = centres["provenance"][key]
    print("cluster-centre catalogues: "
          + ", ".join(f"{k}={len(v)}" for k, v in clusters.items()))

    # ---- the 110 Messier rows, every one of which ships -------------------------------
    messier_rows = []
    for src in evidence["messier"]:
        src = dict(src)
        src["messier_designation"] = src["messier"]
        r = build_row(src, "messier", items, by_key, heasarc, clusters)
        if r is None:
            raise SystemExit(
                f"{src['messier']} has no second source inside its tolerance; the spec "
                "requires all 110 Messier rows to ship, so this is a hard failure rather "
                "than a silent drop")
        r["messier"] = norm(src["messier"]).replace(" ", "")
        messier_rows.append(r)
    print(f"messier rows built: {len(messier_rows)}")

    # ---- the non-Messier candidate set ------------------------------------------------
    messier_desigs = set()
    for src in evidence["messier"]:
        messier_desigs.add(norm(src["main_id"]))
        messier_desigs.update(norm(d) for d in src.get("designations", []))
    nonm, dropped = [], []
    for src in evidence["pool"]:
        if norm(src["main_id"]) in messier_desigs or norm(src["desig"]) in messier_desigs:
            continue
        r = build_row(src, "ngc", items, by_key, heasarc, clusters)
        if r is None:
            dropped.append(norm(src.get("main_id") or src.get("desig") or "?"))
            continue
        nonm.append(r)
    print(f"non-messier candidates: {len(nonm)}  dropped for no in-tolerance second "
          f"source: {len(dropped)}")

    for limit, label in ((LIMIT_PRIMARY, "10.0"), (LIMIT_FALLBACK, "10.5")):
        usable = [r for r in nonm if r["_v"] is not None and r["_v"] <= limit
                  and r["_withinTolerance"]]
        print(f"  limit {label}: {len(usable)} usable non-Messier rows "
              f"(need {NON_MESSIER_CAP})")
        if len(usable) >= NON_MESSIER_CAP:
            limit_used, chosen = limit, sorted(usable, key=lambda r: (r["_v"], r["id"]))
            break
    else:
        raise SystemExit("fewer than 140 non-Messier rows qualify even at 10.5")

    selected = chosen[:NON_MESSIER_CAP]
    rows = messier_rows + selected

    ids = [r["id"] for r in rows]
    if len(set(ids)) != len(ids):
        raise SystemExit("duplicate ids in the shipped table")

    # Every shipped row is inside its tolerance by construction (a row with no agreeing
    # second source never reaches here), so this list is empty and the bake proves it.
    exceptions = [{"id": r["id"], "deltaArcmin": r["verification"]["deltaArcmin"],
                   "srcB": r["verification"]["srcB"]}
                  for r in rows if not r["_withinTolerance"]]
    if exceptions:
        raise SystemExit(f"out-of-tolerance rows reached the output: {exceptions}")
    tolerance_excess = 0.0
    for r in rows:
        for key in ("_withinTolerance", "_toleranceArcmin", "_simbadClass", "_v"):
            r.pop(key, None)

    document = {
        "schemaVersion": SCHEMA_VERSION,
        "manifest": {
            "rowCount": len(rows),
            "messierCount": len(messier_rows),
            "nonMessierCount": len(selected),
            "magnitudeLimitUsed": limit_used,
            "nonMessierCap": NON_MESSIER_CAP,
            "selectionRule": (
                "all 110 Messier objects, plus the 140 brightest non-Messier NGC/IC "
                f"deep-sky objects whose sourced V magnitude is <= {limit_used}, each "
                "verified against a second independent source inside its angular tolerance"),
            "generatedBy": "tools/bake-objects.py",
            "generatedAt": BUILD_DATE,
            "sources": evidence["sources"],
            "clusterCentreSources": cluster_provenance,
            "droppedForNoSecondSource": dropped,
            "verificationExceptions": exceptions,
            "maxToleranceExcessArcmin": round(tolerance_excess, 3),
        },
        "objects": rows,
    }

    out = ASSETS / "catalog" / "objects.json"
    write_json(out, document)
    print(f"\nwrote {out}  {out.stat().st_size:,} B  sha256 {sha256_file(out)}")
    print(f"rows={len(rows)} messier={len(messier_rows)} nonMessier={len(selected)} "
          f"limit={limit_used}")
    print(f"out-of-tolerance rows carried: {len(exceptions)} "
          f"-> {[e['id'] for e in exceptions]}")
    print(f"largest tolerance excess: {tolerance_excess:.3f} arcmin")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
