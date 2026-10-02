#!/usr/bin/env python3
"""The one implementation of the colour maths the factory had copied into two modules.

``sRGB -> linear -> WCAG relative luminance -> contrast ratio`` and the ``#RRGGBB`` string form
were byte-for-byte duplicates in ``scripts/visual-qa.py`` and ``scripts/lib/icon_qa.py``. That is
the "one source of truth, imported - never restated" defect
`.cursor/rules/verification-discipline.mdc` names: the copies agree only until one of them is
sharpened, and then the report and the gate are measuring different colours.

This module owns the primitives and nothing else. Every value here is a pure extraction of the
existing bodies, so the numerics are unchanged by construction; the extraction is validated by
comparing this module's output against the deleted implementations.

``scripts/store-studio.py`` still carries its own ``hex_of``: it is deliberately out of scope for
this change.
"""

from __future__ import annotations

import re

import numpy as np

# Rec. 709 / WCAG luminance weights. They are applied to *linearised* channels by
# `contrast_ratio`, and to the raw 0..255 samples by `relative_luminance`, whose historic name in
# the call sites is `luminance`.
LUMA = np.array([0.2126, 0.7152, 0.0722])

_HEX = re.compile(r"#[0-9A-Fa-f]{6}")


def srgb_to_linear(channel) -> np.ndarray:
    """The IEC 61966-2-1 sRGB transfer function, applied to 0..255 samples."""
    value = np.asarray(channel, dtype=np.float64) / 255.0
    return np.where(value <= 0.04045, value / 12.92, ((value + 0.055) / 1.055) ** 2.4)


def relative_luminance(rgb) -> np.ndarray:
    """Weighted sRGB luminance: ``rgb[..., :3] @ LUMA`` (the old ``luminance()``)."""
    return np.asarray(rgb)[..., :3].astype(np.float64) @ LUMA


def contrast_ratio(rgb_a, rgb_b) -> float:
    """The WCAG 2.x contrast ratio between two colours, linearised first."""
    la = float(srgb_to_linear(np.asarray(rgb_a, dtype=np.float64)) @ LUMA)
    lb = float(srgb_to_linear(np.asarray(rgb_b, dtype=np.float64)) @ LUMA)
    hi, lo = max(la, lb), min(la, lb)
    return (hi + 0.05) / (lo + 0.05)


def hex_of(rgb) -> str:
    """``(r, g, b)`` (or any length-3 slice) as an uppercase ``#RRGGBB`` string."""
    return "#%02X%02X%02X" % tuple(int(round(float(v))) for v in np.asarray(rgb)[:3])


def parse_hex(text) -> tuple[int, int, int]:
    """Parse ``#RRGGBB`` into an ``(r, g, b)`` tuple; a malformed value raises ``ValueError``.

    The message keeps the shape the existing call sites produce (``store-studio.py``'s
    ``BLOCKED: ... must be a #RRGGBB colour, got <repr>``): a malformed value names the input that
    was rejected. A caller that tolerates a malformed colour catches ``ValueError`` - it must not
    accidentally treat a parse failure as black.
    """
    if not isinstance(text, str) or _HEX.fullmatch(text) is None:
        raise ValueError(f"BLOCKED: colour must be a #RRGGBB value, got {text!r}")
    return (int(text[1:3], 16), int(text[3:5], 16), int(text[5:7], 16))
