"""Spectral-type handling shared by tools/fetch-stars.py and tools/bake-stars.py.

The star table carries a six-value ``colourBucket``.  It is derived, at bake time, from the
star's spectral class by way of the standard main-sequence B-V calibration
(Pecaut & Mamajek 2013, "Modern Mean Dwarf Stellar Color and Effective Temperature
Sequence", https://www.pas.rochester.edu/~emamajek/EEM_dwarf_UBVIJHK_colors_Teff.txt),
with a luminosity-class offset applied for giants and supergiants.

This is an INFERRED step and is labelled as such in docs/asset-provenance.md: the measured
input is the spectral class; the B-V value and the bucket are a calibration, and the bucket
is only a sprite selector (design.md: 8 magnitude bins x 6 colour buckets = 48 sprites).

Bucket thresholds are the ones tabulated in docs/ASSET-FORMATS.md §3:

    0  B-V < 0.00            blue   (O/B)
    1  0.00 <= B-V < 0.30    blue-white (A)
    2  0.30 <= B-V < 0.60    white  (F)
    3  0.60 <= B-V < 0.90    yellow (G)
    4  0.90 <= B-V < 1.40    orange (K)
    5  1.40 <= B-V           red    (M)

Carbon, S-type and late-type stars (labels C, R, N, S, L, T, Y) have no B-V in the dwarf
calibration at all; they are the reddest objects in the sky and are mapped straight to bucket 5.
Wolf-Rayet (W) stars are mapped to 0.  Every other non-calibrated label keeps the documented
neutral default, and the row is counted in the bake report.
"""
from __future__ import annotations

import re

DEFAULT_BUCKET = 2

# Main-sequence B-V per (class letter, subclass).  Values follow the Pecaut & Mamajek
# calibration; the table is deliberately coarse (0.5 subclass steps) because a six-value
# bucket cannot resolve better than that.
BV_MS = {
    "O": {-1: -0.33, 0: -0.33, 5: -0.32, 9: -0.31},
    "B": {0: -0.30, 2: -0.24, 5: -0.17, 8: -0.11, 9: -0.06},
    "A": {0: 0.00, 2: 0.06, 5: 0.15, 7: 0.21, 9: 0.28},
    "F": {0: 0.30, 2: 0.35, 5: 0.44, 8: 0.53, 9: 0.57},
    "G": {0: 0.60, 2: 0.63, 5: 0.68, 8: 0.74, 9: 0.77},
    "K": {0: 0.82, 2: 0.94, 5: 1.15, 7: 1.32, 9: 1.40},
    "M": {0: 1.41, 2: 1.49, 5: 1.64, 8: 1.92, 9: 2.00},
}

# B-V offset added for a luminosity class, main sequence = 0.  A giant of the same
# temperature is redder than the dwarf because its surface gravity is lower.
LUMA_OFFSET = (
    ("VI", -0.10),   # subdwarf
    ("IV", 0.05),    # subgiant
    ("III", 0.20),   # giant
    ("II", 0.40),    # bright giant
    ("I", 0.65),     # supergiant (I, Iab, Ib, Ia, Iab-Ib, ...)
)

# Labels outside the dwarf calibration, mapped by their own spectral character.
EXOTIC_BUCKET = {"W": 0, "C": 5, "R": 5, "N": 5, "S": 5, "L": 5, "T": 5, "Y": 5}

_CLASS_RE = re.compile(r"([OBAFGKMLTYWCSRN])\s*(\d(?:\.\d)?)?")
_LUMA_RE = re.compile(r"\b(I{1,3}V?|IV|V|VI)[a-z]*\b")


def _nearest_subclass(letter: str, sub: float) -> float:
    table = BV_MS[letter]
    keys = sorted(table)
    best = min(keys, key=lambda k: (abs(k - sub), k))
    return table[best]


def parse_spectral_class(sptype: str) -> tuple[str, float, str]:
    """Return (class letter, subclass, luminosity class) from a raw spectral-type string.

    Handles the compound forms the literature uses: ``A0mA1 Va``, ``M0/2III``, ``K0III+...``,
    ``K1+ IIIb CN-1``, ``G1III + K0III``, ``F8V``, ``G2 V``, ``B5Ia``, ``M2Ia-Iab``, ``K1.5III``.
    The first classification in the string is the subject; the luminosity class may follow it
    directly (``G2V``) or be separated by a pecularity marker (``K1+ IIIb CN-1``).
    """
    s = (sptype or "").strip()
    m = _CLASS_RE.search(s)
    if not m:
        return "", -1.0, ""
    letter = m.group(1)
    sub = float(m.group(2)) if m.group(2) else -1.0
    rest = s[m.end():]
    lm = _LUMA_RE.search(rest)
    luma = lm.group(1) if lm else ""
    return letter, sub, luma


def bucket_for_spectral_class(sptype: str) -> tuple[int | None, float | None]:
    """Return (colourBucket, B-V) for a spectral-type string, or (None, None) if unmappable."""
    letter, sub, luma = parse_spectral_class(sptype)
    if not letter:
        return None, None
    if letter not in BV_MS:
        return EXOTIC_BUCKET.get(letter), None
    bv = _nearest_subclass(letter, sub)
    for token, offset in LUMA_OFFSET:
        if luma == token:
            bv += offset
            break
    return bucket_for_bv(bv), bv


def bucket_for_bv(bv: float) -> int:
    if bv < 0.00:
        return 0
    if bv < 0.30:
        return 1
    if bv < 0.60:
        return 2
    if bv < 0.90:
        return 3
    if bv < 1.40:
        return 4
    return 5
