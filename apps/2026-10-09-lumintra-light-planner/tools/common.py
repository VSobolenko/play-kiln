"""Shared helpers for the Lumintra asset bakers.

Every temporary or cached download lives under <repo>/.kiln/scratch/lumintra-assets/
(.cursor/rules/workspace-hygiene.mdc); nothing is written to the system temp directory.
"""
from __future__ import annotations

import hashlib
import json
import math
import time
import urllib.parse
from pathlib import Path

import requests

TOOLS_DIR = Path(__file__).resolve().parent
SLOT_DIR = TOOLS_DIR.parent
REPO_DIR = SLOT_DIR.parent.parent
SCRATCH = REPO_DIR / ".kiln" / "scratch" / "lumintra-assets"
CACHE = SCRATCH / "cache"
ASSETS = SLOT_DIR / "app" / "src" / "main" / "assets"

USER_AGENT = "LumintraAssetBaker/1.0 (PlayKiln factory; v.sobolenko.vs@gmail.com)"

SCRATCH.mkdir(parents=True, exist_ok=True)
CACHE.mkdir(parents=True, exist_ok=True)


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest().upper()


def sha256_bytes(blob: bytes) -> str:
    return hashlib.sha256(blob).hexdigest().upper()


def _cache_path(key: str, suffix: str) -> Path:
    return CACHE / (hashlib.sha1(key.encode("utf-8")).hexdigest()[:20] + suffix)


def fetch(url: str, *, data: dict | None = None, suffix: str = ".bin", refresh: bool = False,
          timeout: int = 180, retries: int = 4, headers: dict | None = None) -> bytes:
    """GET (or POST form `data`) with an on-disk cache under SCRATCH/cache."""
    key = url + ("?" + urllib.parse.urlencode(data) if data else "")
    path = _cache_path(key, suffix)
    if path.exists() and not refresh:
        return path.read_bytes()
    hdrs = {"User-Agent": USER_AGENT}
    if headers:
        hdrs.update(headers)
    last = None
    for attempt in range(retries):
        try:
            if data is None:
                r = requests.get(url, headers=hdrs, timeout=timeout)
            else:
                r = requests.post(url, data=data, headers=hdrs, timeout=timeout)
            if r.status_code == 200:
                path.write_bytes(r.content)
                return r.content
            last = f"HTTP {r.status_code}: {r.text[:300]}"
            if r.status_code in (429, 500, 502, 503, 504):
                time.sleep(5 * (attempt + 1))
                continue
            break
        except requests.RequestException as e:  # network error: retry
            last = repr(e)
            time.sleep(5 * (attempt + 1))
    raise RuntimeError(f"fetch failed for {url}: {last}")


def tap_sync(endpoint: str, adql: str, *, fmt: str = "json", refresh: bool = False) -> dict:
    """Run an ADQL query against a TAP /sync endpoint; returns parsed JSON (VOTable-JSON)."""
    raw = fetch(endpoint.rstrip("/") + "/sync",
                data={"REQUEST": "doQuery", "LANG": "ADQL", "FORMAT": fmt, "QUERY": adql},
                suffix=".json", refresh=refresh)
    return json.loads(raw)


def sparql(query: str, *, refresh: bool = False) -> list[dict]:
    raw = fetch("https://query.wikidata.org/sparql", data={"query": query, "format": "json"},
                suffix=".json", refresh=refresh,
                headers={"Accept": "application/sparql-results+json"})
    return json.loads(raw)["results"]["bindings"]


def sep_arcmin(ra1: float, de1: float, ra2: float, de2: float) -> float:
    """Spherical-cosine separation (design.md § The catalogue, step 4), degrees in, arcmin out.

    Uses the haversine form for numerical stability at sub-arcsecond separations; it is
    algebraically identical to acos(sin d1 sin d2 + cos d1 cos d2 cos(a1-a2)).
    """
    a1, d1, a2, d2 = map(math.radians, (ra1, de1, ra2, de2))
    h = math.sin((d2 - d1) / 2) ** 2 + math.cos(d1) * math.cos(d2) * math.sin((a2 - a1) / 2) ** 2
    return math.degrees(2 * math.asin(min(1.0, math.sqrt(h)))) * 60.0


# ---------------------------------------------------------------------------
# VOTable, in its BINARY serialization
# ---------------------------------------------------------------------------
# NASA HEASARC's TAP service answers every query as VOTable, and ignores FORMAT /
# RESPONSEFORMAT: measured 2026-10-09, FORMAT=json is refused ("Unsupported format:
# json") and FORMAT=csv|tsv|votable all return the same document with the rows in a
# base64 <BINARY><STREAM> block rather than in <TABLEDATA>. So a HEASARC reader needs
# a VOTable reader, and this is it: VOTable 1.3/1.4 binary serialization is
# big-endian, each row is the concatenation of its FIELDs in declaration order, and a
# field's width follows from its datatype and arraysize. There is no null bitmask in
# <BINARY> (that is <BINARY2>): IEEE NaN marks a null float and the type's minimum
# value marks a null integer, which is how the two are decoded below.

import base64 as _base64
import xml.etree.ElementTree as _ET

_VOTABLE_NS = "http://www.ivoa.net/xml/VOTable/v1.3"
_FIXED_WIDTH = {"short": 2, "int": 4, "long": 8, "unsignedByte": 1, "boolean": 1,
                "float": 4, "double": 8}
_INT_NULL = {"short": 0x8000, "int": -2 ** 31, "long": -2 ** 63, "unsignedByte": 0xFF}


_STRUCT_CODE = {"short": "h", "int": "i", "long": "q", "float": "f", "double": "d",
                "unsignedByte": "B", "boolean": "?"}


def _field_is_variable(field) -> bool:
    return field.get("arraysize") == "*"


def votable_rows(raw: bytes) -> list[dict]:
    """Parse a VOTable document (BINARY or TABLEDATA) into a list of dicts.

    BINARY is decoded from the base64 <STREAM>; TABLEDATA from its <TR>/<TD>. HEASARC
    declares every character field as `arraysize="*"`, which the VOTable 1.3 spec
    serializes in BINARY as a **4-byte big-endian element count followed by the
    bytes** — so a BINARY row has no fixed width and must be walked field by field.
    Measured on the real document: the first row begins `00 00 00 01 31` (`"1"`, the
    `__row` field) then `00 00 00 08 4e 47 43 20 36 34 37 35` (`"NGC 6475"`), which is
    the layout this decoder implements.
    """
    import struct

    root = _ET.fromstring(raw)
    fields = list(root.iter(f"{{{_VOTABLE_NS}}}FIELD"))
    names = [f.get("name") for f in fields]

    stream = next(iter(root.iter(f"{{{_VOTABLE_NS}}}STREAM")), None)
    if stream is not None:
        blob = _base64.b64decode("".join((stream.text or "").split()))
        out, cursor = [], 0
        while cursor < len(blob):
            row = {}
            for field in fields:
                dt = field.get("datatype")
                if _field_is_variable(field):
                    n = struct.unpack(">i", blob[cursor:cursor + 4])[0]
                    cursor += 4
                    chunk, cursor = blob[cursor:cursor + n], cursor + n
                    row[field.get("name")] = chunk.decode("ascii", "replace").strip()
                    continue
                arr = field.get("arraysize")
                n = int(arr.split("x")[0]) if arr else 1
                if dt == "char":
                    chunk, cursor = blob[cursor:cursor + n], cursor + n
                    row[field.get("name")] = chunk.decode("ascii", "replace").strip()
                    continue
                width = n * _FIXED_WIDTH[dt]
                chunk, cursor = blob[cursor:cursor + width], cursor + width
                vals = list(struct.unpack(">" + _STRUCT_CODE[dt] * n, chunk))
                if dt in ("float", "double"):
                    vals = [None if v != v else v for v in vals]  # NaN = the null
                else:
                    null = _INT_NULL.get(dt)
                    vals = [None if v == null else v for v in vals]
                row[field.get("name")] = vals[0] if n == 1 else vals
            out.append(row)
        return out

    out = []
    for tr in root.iter(f"{{{_VOTABLE_NS}}}TR"):
        cells = [(td.text or "").strip() for td in tr.iter(f"{{{_VOTABLE_NS}}}TD")]
        out.append(dict(zip(names, cells)))
    return out


def write_json(path: Path, obj, *, compact: bool = False) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if compact:
        text = json.dumps(obj, ensure_ascii=False, separators=(",", ":"))
    else:
        text = json.dumps(obj, ensure_ascii=False, indent=1)
    path.write_text(text + "\n", encoding="utf-8", newline="\n")


# ---------------------------------------------------------------------------
# Spherical geometry for the constellation REGIONS
# ---------------------------------------------------------------------------
# A constellation boundary file is a closed ring of (RA, Dec) vertices; between two
# consecutive vertices the boundary is one edge. The area of such a region on the unit
# sphere is the cos(Dec)-weighted integral of the ring, and Green's theorem turns it into
# a sum over edges -- the derivation is written out in `bake-constellations.py`. The three
# functions below are the whole of it, kept here because the bake and any future region
# tool both need them.
_DEG = math.pi / 180.0


def unwrap_ra_ring(ring: list) -> list:
    """The same ring with RA made continuous, so a ring crossing 0h does not wrap.

    A ring that **encircles a pole** (Ursa Minor, Octans) unwraps to a spiral that is
    360 degrees wide; that is expected and is closed by `close_over_pole`.
    """
    out = [[ring[0][0], ring[0][1]]]
    for ra, dec in ring[1:]:
        prev = out[-1][0]
        while ra - prev > 180.0:
            ra -= 360.0
        while ra - prev < -180.0:
            ra += 360.0
        out.append([ra, dec])
    return out


def ring_winding(ring: list) -> float:
    """Net RA turn around the ring: 0 normally, +/-360 when the ring encircles a pole."""
    total, n = 0.0, len(ring)
    for i in range(n):
        d = ring[(i + 1) % n][0] - ring[i][0]
        while d > 180.0:
            d -= 360.0
        while d < -180.0:
            d += 360.0
        total += d
    return total


def close_over_pole(ring: list) -> list:
    """A ring that winds around a pole, closed along the seam and over the pole."""
    p = unwrap_ra_ring(ring)
    if abs(ring_winding(p)) <= 180.0:
        return p
    pole = 90.0 if max(q[1] for q in p) > 0.0 else -90.0
    return p + [[p[-1][0], pole], [p[0][0], pole]]


def ring_area_sq_deg(ring: list) -> float:
    """Signed cos(Dec)-weighted area of a closed ring, in square degrees.

    For one edge `(a1,d1) -> (a2,d2)` the Green integral `∮ a cos d dd` is
    `a1 (sin d2 - sin d1) + da (sin d2 + (cos d2 - cos d1)/dd)`, which is exact for the
    bilinear-in-(RA,Dec) polygon and continuous as `dd -> 0` (where it is 0).
    """
    acc, n = 0.0, len(ring)
    for i in range(n):
        a1, d1 = ring[i]
        a2, d2 = ring[(i + 1) % n]
        a1, d1, a2, d2 = a1 * _DEG, d1 * _DEG, a2 * _DEG, d2 * _DEG
        dd, da = d2 - d1, a2 - a1
        if abs(dd) < 1e-12:
            continue
        acc += a1 * (math.sin(d2) - math.sin(d1)) \
            + da * (math.sin(d2) + (math.cos(d2) - math.cos(d1)) / dd)
    return abs(acc) / _DEG / _DEG


def inside_ring(ring: list, ra: float, dec: float) -> bool:
    """Even-odd point-in-ring test in the (RA, Dec) plane, for an unwrapped ring."""
    c, n = False, len(ring)
    for i in range(n):
        x1, y1 = ring[i]
        x2, y2 = ring[(i + 1) % n]
        if (y1 > dec) != (y2 > dec):
            if ra < x1 + (dec - y1) * (x2 - x1) / (y2 - y1):
                c = not c
    return c


def region_area_sq_deg(rings: list) -> float:
    """Total area of a constellation, summing its separate rings (Serpens has two)."""
    return sum(ring_area_sq_deg(close_over_pole(r)) for r in rings)


def region_centre_deg(rings: list, step: float = 0.2) -> tuple:
    """Area-weighted centroid direction of a region, as `(raDeg, decDeg)` J2000.

    The centroid is the direction of `integral(r_hat cos d dRA dDec)`; it is computed by
    sampling the region on a `step`-degree grid, which handles a ring that crosses 0h
    (unwrapped, with the RA range allowed past 360) and one that encircles a pole (closed
    over the pole first, so its unwrapped RA range is a full 360 degrees).
    """
    closed = [close_over_pole(r) for r in rings]
    pts = [p for r in closed for p in r]
    a_lo, a_hi = min(p[0] for p in pts), max(p[0] for p in pts)
    d_lo = max(-90.0, min(p[1] for p in pts))
    d_hi = min(90.0, max(p[1] for p in pts))
    sx = sy = sz = weight = 0.0
    dec = d_lo + step / 2.0
    while dec < d_hi:
        ra = a_lo + step / 2.0
        cd = math.cos(dec * _DEG)
        while ra < a_hi:
            if any(inside_ring(r, ra, dec) for r in closed):
                w = cd * step * step * _DEG
                sx += w * cd * math.cos(ra * _DEG)
                sy += w * cd * math.sin(ra * _DEG)
                sz += w * math.sin(dec * _DEG)
                weight += w
            ra += step
        dec += step
    if weight == 0.0:
        raise ValueError("region contains no sampled cell")
    norm = math.sqrt(sx * sx + sy * sy + sz * sz)
    ra_out = math.degrees(math.atan2(sy, sx)) % 360.0
    dec_out = math.degrees(math.asin(max(-1.0, min(1.0, sz / norm))))
    return ra_out, dec_out
