#!/usr/bin/env python3
"""Measurable prerequisites of a good icon.

"Beautiful" is not machine-checkable, and this module does not pretend otherwise. What it can do is
measure the properties whose absence produced the icon the owner rejected: a field made of texture
instead of tone, strokes below the width a launcher can show, counters that fill in, a mark that is
not centred, a mark that is lost in its frame or bursting out of it, and ink a launcher mask would
shave. Every one of those is a number, and every number has a threshold with the reason for it
written beside it in ``ICON_GATES``.

The measurements are taken from pixels, not from the generator's intentions, so they survive a
rewrite of the renderer: `scripts/test-icon-design.py` renders the *pre-change* composition and
shows these sensors failing on it.

Three implementation notes that matter for reading the numbers:

* **Feature width** is measured on the shape's medial axis, where a feature's local thickness is
  twice its distance to the background. Granulometry - opening the shape with growing discs and
  watching where the area collapses - was the first implementation and it was replaced, because a
  long thin feature keeps almost all of its area when opened by a disc slightly wider than itself,
  so the collapse point measured the shape's *bulk*, not its thinnest stroke. A 24px ring measured
  2px under granulometry and 24px on the ridge; the ring is 24px.
* **One role region** is separated from another by nearest palette colour, and the antialiased
  pixels where two roles meet can group into a sliver a pixel or two wide. Such a sliver is not a
  stroke, so components below ``SLIVER_AREA_PX`` are dropped before measuring; the floor is stated
  and justified at its definition. The *silhouette* - the union of every role - is measured
  unfiltered, so a genuinely thin mark is still caught by the leg that matters.
* **The field** is measured twice: high-frequency energy (which the old 32px drafting grid supplied
  and which is invisible at 48dp, measured 1.40 there against 0.10 in the new field) and the
  residual against the best-fit linear ramp (a single tonal field is a plane, so it fits to 0.22;
  two radial blooms are curvature, so they fit to 2.49). Both are computed only on pixels the mark
  does not occupy, and the exclusion is a square dilation rather than the default cross, because a
  cross reaches its radius along the axes and only ~70% of it on a diagonal, which let the mark's
  resampling halo leak into a measurement of the field.
"""

from __future__ import annotations

import math
import sys
from pathlib import Path
from typing import Iterable, Mapping

import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from lib import colours  # noqa: E402  the one colour-maths implementation, shared with visual-qa
from lib import icon_design as design  # noqa: E402

# These names are kept as aliases of the shared primitives so `store-studio.py`, `visual-qa.py` and
# `test-icon-design.py` can keep importing them from here, while the body lives in one module.
LUMA = colours.LUMA
luminance = colours.relative_luminance
srgb_to_linear = colours.srgb_to_linear
contrast_ratio = colours.contrast_ratio

CANVAS = design.CANVAS
MARK_DELTA = 12.0  # a pixel this far from the field is mark; below it is compression noise
# A role region's antialiased seam with its neighbour is a component a pixel or two wide. 64px is
# 8x8: larger than any seam observed on a rendered mark (the pipe's dimension line meets the ring
# wall over 24px, i.e. ~48px of seam), and far smaller than the smallest *legal* feature, which is
# a declared 16px stroke of any real length (16x16 = 256px). A floor here therefore cannot hide a
# stroke that the declared stroke scale allows.
SLIVER_AREA_PX = 64

# A stroke is a width that *runs*; a taper is a width that ends. The medial axis of a pointed tip
# is thin over only the last few pixels of its length, so requiring the reported width to be
# sustained over this fraction of the ridge removes tips from the sample without removing a
# genuinely narrow long feature, which sustains its width along its whole length.
RIDGE_SUSTAINED_FRACTION = 0.25

# The composite's coverage is inverted exactly rather than thresholded, so the 50% boundary is the
# definition of "mark ink" rather than a tuned tolerance. See `mark_mask`.
COVERAGE_THRESHOLD = 0.5
# A role colour this close (L2 over 8-bit RGB) to the local field is invisible there, so the
# comparison carries no information: the whole canvas is at most 441 units from any colour, and any
# separation below ~2 units is rounding noise in the field.
MARK_FIELD_SEPARATION = 2.0


# --------------------------------------------------------------------------------------
# Thresholds
#
# Every number here is a decision, and the decision is written down. Where a threshold gates,
# `evaluate_icon` reports it as a FAIL leg; where it is only reported, the key says so.
# --------------------------------------------------------------------------------------

_KEYLINE_DP = design.KEYLINE_CIRCLE * design.ADAPTIVE_VIEWPORT_DP  # 31.0dp
_PX_IN_DP = design.ADAPTIVE_VIEWPORT_DP / CANVAS  # one 512px canvas pixel, in dp

ICON_GATES: dict[str, dict] = {
    "field_high_frequency_rms": {
        "max": 0.5,
        "unit": "luma levels (RMS of the icon minus a sigma=2 low-pass, mark excluded)",
        "why": "texture in the field. The rejected icon carried a 32px drafting grid at alpha 12; "
               "at 48dp that is a sub-pixel weave that reads as dirt. Measured: one exact tonal "
               "ramp reads 0.10 with its mark present, a bare ramp of the widest palette possible "
               "(black to white) reads 0.28 - that floor is 8-bit quantisation of a smooth ramp and "
               "cannot be lower - and the rejected field reads 1.40, grid and blooms together. 0.5 "
               "is 1.8x above the worst field a slot could legitimately declare and 2.8x below the "
               "field that was rejected.",
        "gates": True,
    },
    "field_linear_ramp_rms": {
        "max": 2.0,
        "unit": "luma levels (RMS residual of the field against its best-fit linear ramp)",
        "why": "how far the field is from one tonal field. The rejected icon composited a gradient "
               "plus two radial blooms, so it was not a ramp; one gradient plus at most one "
               "restrained tonal field is. Measured: the new field reads 0.22 (quantisation alone), "
               "the rejected field 2.49, because a bloom is curvature and a plane cannot fit it.",
        "gates": True,
    },
    "thinnest_stroke_px": {
        "min": design.MIN_FEATURE * CANVAS,
        "unit": "canvas px at 512 (thin roles and the silhouette together)",
        "why": f"a stroke below {design.MIN_FEATURE * CANVAS:.0f}px is below "
               f"{design.MIN_FEATURE * 48:.2f}px at 48dp, where a launcher cell shows a grey smear "
               "instead of a line.",
        "gates": True,
    },
    "smallest_counter_px": {
        "min": design.MIN_COUNTER * CANVAS,
        "unit": "canvas px at 512 (smallest enclosed negative space)",
        "why": "a counter that fills in changes the mark's shape, not just its weight. Same floor as "
               "a stroke.",
        "gates": True,
    },
    "optical_offset_px": {
        "max": design.OPTICAL_TOLERANCE * CANVAS,
        "unit": "canvas px at 512 (silhouette centroid vs the mark's declared optical anchor)",
        "why": "a mark that is off its optical centre looks crooked, which is the first word the "
               "owner used. 10.24px is 2% of the canvas: below the threshold at which the eye "
               "reliably reads an imbalance at 48dp.",
        "gates": True,
    },
    "ink_coverage_of_safe_zone": {
        "min": 0.15,
        "max": 0.85,
        "unit": "fraction of the 66dp safe square covered by mark ink",
        "why": "below the floor the mark is a speck; above the ceiling it has burst out of the zone "
               "a launcher guarantees, which is the defect the rejected pack had (0.99). The band "
               "has to hold both a filled page (0.42, the book) and a measured ring (0.30, the "
               "pipe).",
        "gates": True,
    },
    "mark_reach_dp": {
        "min": round(design.MIN_REACH_RATIO * _KEYLINE_DP, 3),
        "max": round(_KEYLINE_DP + _PX_IN_DP, 3),
        "unit": "dp from the optical anchor to the mark's outermost ink, in the 108dp viewport",
        "why": "the construction grid gives a mark the 31dp circle keyline. Under it the mark looks "
               "lost in its cell; over it a launcher mask will crop it. The ceiling allows one "
               "antialiased canvas pixel, because the mask includes the edge's partial coverage.",
        "gates": True,
    },
    "silhouette_extent": {
        "min": 0.30,
        "max": round(float(design.GRID["safe"]), 6),
        "unit": "canvas fractions (longest silhouette side)",
        "why": "the safe zone is 0.611 of the canvas wide. A silhouette below 0.30 looks lost at "
               "48dp; above 0.611 it is wider than the zone every launcher guarantees, so an "
               "aggressive mask crops it.",
        "gates": True,
    },
    "mark_outside_safe_circle_px": {
        "max": 0,
        "unit": "canvas px of mark ink outside the 66dp safe circle",
        "why": "the mask survival rule, as a number: the shipped mark must sit inside the safe "
               "circle, not merely be reported against it. The rejected pack put 28% of the book "
               "and 63% of the pipe outside it.",
        "gates": True,
    },
    "alpha_min": {
        "min": 255,
        "unit": "8-bit alpha (minimum anywhere, including the four corners)",
        "why": "Play rejects a store icon with transparency, and a corner that is not opaque is how "
               "a masked icon is smuggled in.",
        "gates": True,
    },
    "downscale_contrast_48px": {
        "min": 4.5,
        "unit": "WCAG contrast ratio between mark ink and its field at 48px",
        "why": "the icon has to be recognisable in a launcher cell. 4.5:1 is WCAG AA for normal "
               "text and a stricter floor than the 3:1 WCAG asks of a non-text graphical object, "
               "which is what an icon is.",
        "gates": True,
    },
    "min_feature_at_48dp_px": {
        "min": 1.5,
        "unit": "px at a 48px render",
        "why": "16px at 512 is exactly 1.5px at 48; this leg is the same quantity as "
               "thinnest_stroke_px expressed at the size the user actually sees.",
        "gates": True,
    },
    "mark_halo_fraction": {
        "max": None,
        "unit": "fraction of the canvas within 2px of the mark",
        "why": "reported only: context for whether a coverage figure is dominated by edge pixels.",
        "gates": False,
    },
    "field_grid_periodicity": {
        "max": None,
        "unit": "ratio, strongest grid harmonic over its control band (reported, not gated)",
        "why": "reported only, and deliberately so. It answers a sharper question than the RMS above "
               "- is the field's texture a *periodic grid* rather than tone - and it answers it "
               "unmistakably for the field that was rejected (3.03 against the new field's 0.007, "
               "and 1.34 with the blooms removed) but it cannot be thresholded tightly: a one-pixel "
               "line is a delta, a delta's spectrum is flat, so a grid's harmonics tower over a clean "
               "ramp and are only 1.1x above a legitimate radial bloom's control band. Both numbers "
               "are printed so a reviewer can read the shape of the field; the leg that gates "
               "texture is `field_high_frequency_rms`. A 2-D lattice probe was tried first and "
               "discarded: with the mark's pixels zeroed the hole's edges put energy on the lattice "
               "and the measurement read 46.9 on a pure ramp against 37.4 on the rejected field.",
        "gates": False,
    },
}

LEG_ORDER = tuple(ICON_GATES)


# --------------------------------------------------------------------------------------
# Small numeric helpers
# --------------------------------------------------------------------------------------


def _plane_fit(pixels_xy: np.ndarray, values: np.ndarray) -> np.ndarray:
    """Least-squares coefficients of value ~ a + b*x + c*y."""
    return np.linalg.lstsq(pixels_xy, values, rcond=None)[0]


def _plane_field(shape: tuple[int, int], coefficients: np.ndarray) -> np.ndarray:
    height, width = shape
    ys, xs = np.mgrid[0:height, 0:width]
    return (coefficients[0] + coefficients[1] * xs + coefficients[2] * ys)


def outer_band(shape: tuple[int, int], fraction: float = 0.05) -> np.ndarray:
    """A boolean mask of the canvas frame: the region no mark built on the grid may occupy."""
    height, width = shape
    margin_x = max(1, round(width * fraction))
    margin_y = max(1, round(height * fraction))
    band = np.zeros((height, width), dtype=bool)
    band[:margin_y, :] = True
    band[-margin_y:, :] = True
    band[:, :margin_x] = True
    band[:, -margin_x:] = True
    return band


def estimate_field(icon: np.ndarray, band_fraction: float = 0.05) -> np.ndarray:
    """The field a mark is drawn on, estimated as the plane through the canvas frame.

    A mark built on the grid lives inside the 18dp margin, so the outer band is always field.
    Fitting there and extrapolating is exact for a linear-gradient field and is the fallback when
    the caller cannot hand over the field it rendered.

    Each channel is fitted separately: a field that ramps in colour as well as in tone is a plane in
    every channel, and fitting one luminance plane would hand the segmentation a field that is the
    right brightness and the wrong hue at every pixel off the fit's axis.
    """
    height, width = icon.shape[:2]
    band = outer_band((height, width), band_fraction)
    ys, xs = np.nonzero(band)
    design_matrix = np.stack([np.ones_like(xs), xs, ys], axis=1).astype(np.float64)
    channels = []
    for channel in range(min(3, icon.shape[2])):
        coefficients = _plane_fit(design_matrix, icon[ys, xs, channel].astype(np.float64))
        channels.append(_plane_field((height, width), coefficients))
    return np.stack(channels, axis=2)


# --------------------------------------------------------------------------------------
# The mark
# --------------------------------------------------------------------------------------


def mark_mask(icon: np.ndarray, field: np.ndarray | None = None,
              palette: Mapping[str, Iterable[int]] | None = None,
              mark: str | None = None) -> np.ndarray:
    """Boolean mask of the mark's ink, at exactly 50% coverage.

    The composite is known, so the coverage can be inverted instead of thresholded. A mark layer is
    drawn over the field with alpha `a`, so at every pixel

        pixel = field * (1 - a) + ink * a   ==>   |pixel - ink| = (1 - a) * |field - ink|

    and the 50% boundary is therefore ``|pixel - ink| <= 0.5 * |field - ink|`` - no tuned threshold
    anywhere, and the antialiased edge lands on the half-way line by construction. The role taken is
    the one whose ink is nearest the pixel, i.e. the one the renderer actually drew there.

    Two earlier formulations were wrong here and are worth recording, because both looked right:

    * ``to_ink <= to_field``, comparing the nearest ink distance against the nearest *field*
      distance. It ties whenever the local field colour is itself a mark role - the reference
      palette's ramp runs to ``ink_lift`` and the book's spine is drawn in ``ink_lift`` - and the
      whole canvas then reads as ink, which is a 2.67 "fraction" of a zone that cannot exceed 1.
    * the pre-change delta-from-the-field threshold, which admitted pixels at a few percent coverage
      and whose one-pixel fringe made a 24px ring measure as a 2px stroke.

    A role whose colour is within ``MARK_FIELD_SEPARATION`` of the local field is skipped: at that
    pixel the mark would be invisible whatever the renderer did, so the comparison carries no
    information and would only introduce a division by ~zero. If no role is decisive, the pixel is
    field.

    `field` may be the rendered field as RGB (HxWx3) or as a single plane (HxW).
    """
    rendered = estimate_field(icon) if field is None else np.asarray(field, dtype=np.float64)
    if rendered.ndim == 2:
        rendered = rendered[..., None]
    if rendered.shape[2] == 1:
        rendered = np.repeat(rendered, 3, axis=2)
    if palette is None:
        raise SystemExit("BLOCKED: mark_mask needs the palette the mark was drawn with")
    roles = list(dict.fromkeys(primitive["role"] for primitive in
                               design.mark_primitives(mark)))
    pixels = icon[..., :3].astype(np.float64)
    unreachable = 1e9  # deliberately not inf: `inf <= 0.5 * inf` is True and would read as ink
    best_ink = np.full(pixels.shape[:2], float(unreachable))
    best_field = np.full(pixels.shape[:2], float(unreachable))
    for role in roles:
        colour = _rgb_of(palette, role)
        to_ink = np.linalg.norm(pixels - colour, axis=2)
        to_field = np.linalg.norm(rendered - colour, axis=2)
        take = (to_ink < best_ink) & (to_field > MARK_FIELD_SEPARATION)
        best_ink = np.where(take, to_ink, best_ink)
        best_field = np.where(take, to_field, best_field)
    mask = (best_ink < unreachable) & (best_ink <= COVERAGE_THRESHOLD * best_field)
    return close_pinholes(mask)


def close_pinholes(mask: np.ndarray) -> np.ndarray:
    """Remove the one-pixel seams a 50%-coverage boundary can leave along a role boundary.

    Where two roles meet, a pixel that is a blend of both is below 50% coverage for *either*, so a
    hair-thin line of "no ink" can run through the middle of a mark. On the book that line separates
    the spine from the page, which closes a loop and turns it into a 2px "counter" that a gate then
    fails for a defect that does not exist. Closing with a 3x3 element removes gaps narrower than a
    pixel and nothing else; the smallest legal feature is a declared 16px stroke, so no real counter
    or stroke is at risk.
    """
    return ndimage.binary_closing(mask, structure=np.ones((3, 3), dtype=bool))

def _rgb_of(palette: Mapping[str, Iterable[int]], role: str) -> np.ndarray:
    if role not in palette:
        raise SystemExit(
            f"BLOCKED: the mark draws with palette role {role!r}, which the resolved palette does "
            f"not define (it has {', '.join(sorted(palette))})."
        )
    return np.array([float(v) for v in tuple(palette[role])[:3]])


def _drop_slivers(mask: np.ndarray, floor_px: int = SLIVER_AREA_PX) -> np.ndarray:
    """Remove components smaller than ``floor_px``: antialiased seams, not strokes."""
    labels, count = ndimage.label(mask, structure=np.ones((3, 3), dtype=bool))
    if count == 0:
        return mask
    areas = np.bincount(labels.ravel())
    keep = areas >= floor_px
    keep[0] = False  # background label
    return keep[labels]


def role_masks(icon: np.ndarray, palette: dict, mark: str,
               mask: np.ndarray) -> dict[str, np.ndarray]:
    """Split the mark's pixels into one mask per palette role it draws with.

    Assignment is by nearest palette colour rather than by a tolerance, so the antialiased pixels
    where two roles meet belong to the nearer of the two instead of shrinking both. Those seam
    pixels group into slivers a pixel or two wide, which ``_drop_slivers`` removes before anything
    measures their width.
    """
    roles: dict[str, np.ndarray] = {}
    colours = []
    for primitive in design.mark_primitives(mark):
        role = primitive["role"]
        if role in roles:
            continue
        if role not in palette:
            raise SystemExit(f"BLOCKED: mark {mark!r} draws with palette role {role!r}, absent")
        colours.append(np.array([float(v) for v in tuple(palette[role])[:3]]))
        roles[role] = None  # type: ignore[assignment]
    if not colours:
        return {}
    stack = np.stack(colours).astype(np.float64)
    pixels = icon[..., :3].astype(np.float64)[mask]
    nearest = np.argmin(np.linalg.norm(pixels[:, None, :] - stack[None, :, :], axis=2), axis=1)
    labels = -np.ones(mask.shape, dtype=np.int32)
    labels[mask] = nearest
    names = list(roles)
    return {name: _drop_slivers(labels == index) for index, name in enumerate(names)}


# --------------------------------------------------------------------------------------
# Feature geometry
# --------------------------------------------------------------------------------------


def _ridge(distance: np.ndarray) -> np.ndarray:
    """The discrete medial axis: the pixels whose thickness is a local maximum.

    `>=` rather than `>` on purpose: the medial axis of a rectangle is a flat band one 2px disc
    thick, and `>` would leave only its ragged centre line, which is a smaller and noisier sample
    of the same width.
    """
    local_max = ndimage.maximum_filter(distance, size=3, mode="constant")
    return distance >= local_max - 1e-9


def feature_width_px(shape: np.ndarray) -> float:
    """The thinnest sustained stroke width in a shape, in px.

    Measured on the medial axis, where a shape's local thickness is exactly twice its distance to
    the background, so a 24px ring measures 24px however long its perimeter is. Two details are
    what make it a measurement rather than a guess:

    * only ridge pixels are sampled, so the taper of an edge is not mistaken for a stroke;
    * the answer is the thinnest width sustained over at least ``RIDGE_SUSTAINED_FRACTION`` of the
      ridge's *length*, so a pointed tip - which is thin over a few pixels and is a taper, not a
      stroke - does not decide the verdict, while a genuinely thin long feature still does.
    """
    if shape.size == 0 or not shape.any():
        return 0.0
    distance = ndimage.distance_transform_edt(shape)
    ridge = shape & _ridge(distance) & (distance > 0)
    if not ridge.any():
        return 0.0
    widths = np.sort(2.0 * distance[ridge])
    counts = np.ones(widths.shape[0])
    sustained = RIDGE_SUSTAINED_FRACTION * widths.shape[0]
    index = int(np.searchsorted(np.cumsum(counts), sustained, side="left"))
    index = min(index, widths.shape[0] - 1)
    return float(widths[index])


def thinnest_stroke_px(masks: dict[str, np.ndarray]) -> dict:
    """The thinnest stroke across every role region and the mark's silhouette."""
    results = {}
    for name, mask in masks.items():
        if mask.any():
            results[name] = round(feature_width_px(mask), 2)
    if not results:
        return {"value": 0.0, "per_role": {}}
    narrowest = min(results, key=results.get)
    return {"value": results[narrowest], "role": narrowest, "per_role": results}


def _enclosed_components(shape: np.ndarray) -> list[np.ndarray]:
    """The negative-space regions a shape fully encloses, as sub-masks of its bounding box."""
    rows, cols = np.nonzero(shape)
    if rows.size == 0:
        return []
    sub = np.ascontiguousarray(shape[rows.min():rows.max() + 1, cols.min():cols.max() + 1])
    background = ~sub
    # 4-connectivity for the background is the correct pairing with an 8-connected foreground: a
    # hole that only touches the outside through a diagonal is still a counter.
    labels, count = ndimage.label(background, structure=np.array([[0, 1, 0], [1, 1, 1], [0, 1, 0]]))
    if count == 0:
        return []
    border = set(np.unique(np.concatenate([
        labels[0, :], labels[-1, :], labels[:, 0], labels[:, -1]])))
    return [labels == index for index in range(1, count + 1) if index not in border]


def smallest_counter_px(masks: dict[str, np.ndarray]) -> dict:
    """The smallest enclosed negative space across every role region and the silhouette.

    Counters are the negative-space regions a shape *encloses*. A rounded corner's wedge is not one:
    it is open to the outside, and treating it as a counter would fail every rounded rectangle ever
    drawn.
    """
    results = {}
    for name, mask in masks.items():
        widths = [feature_width_px(component)
                  for component in _enclosed_components(mask) if component.any()]
        if widths:
            results[name] = round(min(widths), 2)
    if not results:
        return {"value": None, "per_role": {}}
    smallest = min(results, key=results.get)
    return {"value": results[smallest], "role": smallest, "per_role": results}


# --------------------------------------------------------------------------------------
# The field
# --------------------------------------------------------------------------------------


# How far the field measurements stay away from the mark. The mask's boundary is the mark's true
# edge at 50% coverage, so what has to be cleared is the fringework on top of it: the antialiased
# partial pixel and the ringing the 4x LANCZOS downsample leaves (~2px measured). 5 is that halo
# doubled, and it is applied with a *square* structuring element - the default cross reaches 5px
# along the axes but only 3px on a diagonal, which let the mark's halo back into a measurement of
# the field (measured: 0.263 instead of 0.125 on the same palette).
FIELD_EXCLUSION_PX = 5


def _field_domain(mask: np.ndarray, size: int | None = None) -> np.ndarray:
    """The pixels a field measurement may use: everything but the mark and its halo."""
    radius = FIELD_EXCLUSION_PX if size is None else size
    grown = ndimage.binary_dilation(
        mask, structure=np.ones((3, 3), dtype=bool), iterations=radius)
    return ~grown


_DECLARED_FIELD_CACHE: dict[tuple, np.ndarray] = {}


def declared_field(shape: tuple[int, int], palette: Mapping[str, Iterable[int]],
                   angle_deg: float = design.FIELD_ANGLE_DEG) -> np.ndarray:
    """The field the design system declares for a palette: the ramp plus `FIELD_OVERLAYS`.

    Reconstructed through `icon_design.icon_field` - the very call `store-studio.render_icon` makes -
    so the reference is the renderer's own definition rather than a second description of it. A slot
    whose palette declares the waterline roles gets the waterline; one that does not gets the bare
    ramp. The field legs below measure against this, which is what lets them gate a field that is a
    declared *two-plane* surface (a ramp plus a waterline) rather than forbidding one: fitting a
    single plane through a waterline reads 4.6 luma of "curvature" that is a deliberate edge, and the
    previous threshold would have refused the design the owner chose to keep.

    Cached because a test run measures half a dozen marks on one palette: the field is deterministic
    and pure, and recomputing it per mark would only make the suite slower, not truer.
    """
    size = int(shape[0])
    ink = tuple(int(v) for v in palette["ink"][:3])
    ink_lift = tuple(int(v) for v in palette["ink_lift"][:3])
    overlays = tuple((overlay["role"], overlay["alpha"], overlay["data"])
                     for overlay in design.field_overlays(palette.keys()))
    key = (size, round(angle_deg, 3), ink, ink_lift, overlays)
    if key not in _DECLARED_FIELD_CACHE:
        image = design.icon_field(size, ink, ink_lift, angle_deg, palette=palette)
        _DECLARED_FIELD_CACHE[key] = np.asarray(image.convert("RGB"), dtype=np.float64)
    return _DECLARED_FIELD_CACHE[key]


def field_high_frequency_rms(icon: np.ndarray, mask: np.ndarray, sigma: float = 2.0,
                             reference: np.ndarray | None = None) -> float:
    """RMS of the icon minus its own low-pass, on the pixels the mark does not occupy.

    When `reference` is given - the field the design declares - the declared field is subtracted
    first, so the leg measures the energy the renderer added that the declaration does not explain
    (a drafting grid, a bloom, the wrong waterline). Without a reference it measures the icon's own
    texture, which is what the pre-change renderer is shown failing on.
    """
    luma = luminance(icon)
    if reference is not None:
        declared = luminance(reference) if reference.ndim == 3 else reference
        luma = luma - declared
    residual = luma - ndimage.gaussian_filter(luma, sigma)
    keep = _field_domain(mask)
    if not keep.any():
        return 0.0
    return float(np.sqrt(np.mean(residual[keep] ** 2)))


def field_linear_ramp_rms(icon: np.ndarray, mask: np.ndarray,
                          reference: np.ndarray | None = None) -> float:
    """RMS residual of the field against the field the design declares, mark excluded.

    A single gradient *is* a ramp, so against a ramp this is near zero. A field carrying a radial
    bloom is not (a bloom is a bump, and a bump is curvature), and the drafting grid adds to it as
    well. A declared waterline is not curvature either - it is two planes meeting at a declared edge,
    so `reference` (which carries that edge) leaves only quantisation, while the previous single-plane
    fit read the edge itself as 4.6 luma of defect.

    With no `reference` the best-fit single plane is still the fallback: that is the path the
    falsification controls take, and it is what makes the pre-change field measurable as a failure.
    """
    height, width = icon.shape[:2]
    keep = _field_domain(mask)
    ys, xs = np.nonzero(keep)
    if ys.size < 16:
        return 0.0
    values = luminance(icon)[ys, xs]
    if reference is not None:
        declared = luminance(reference) if reference.ndim == 3 else reference
        return float(np.sqrt(np.mean((values - declared[ys, xs]) ** 2)))
    coefficients = _plane_fit(np.stack([np.ones_like(xs), xs, ys], axis=1).astype(np.float64), values)
    residual = values - _plane_field((height, width), coefficients)[ys, xs]
    return float(np.sqrt(np.mean(residual ** 2)))


# The field period a drafting grid was drawn at, in canvas px. A grid of lines every 32px is a comb,
# and a comb shows up on the field's own projection as energy at the comb's harmonics and nowhere
# else - which is a far more specific question than "does the field carry high-frequency energy".
GRID_PERIOD_PX = 32.0
# Harmonics of the fundamental that are searched: order 1 is the 32px period, order 4 an 8px one.
# Below 8px a launcher cell cannot show the lines at all.
GRID_HARMONICS = 4
# Fourier modes removed from a projection before the search: DC, the ramp itself, and the gentle
# curvature a per-column average of an off-axis gradient leaves. All strictly below the fundamental
# (512/32 = 16 on a 512px canvas), so no grid harmonic is ever removed with them.
GRID_TREND_MODES = 8


def _projection(icon: np.ndarray, keep: np.ndarray, axis: int) -> np.ndarray | None:
    """The field's mean luma along one axis: one value per row (`axis=0`) or per column (`axis=1`).

    Averaging the field along an axis is what makes this robust where a 2-D transform is not: a mark
    is a *hole* in the field, and a hole has hard edges whose broadband energy lands on every bin
    including the grid's. A projection never sees the hole's edge, only the pixels that are field,
    and the few lines a mark does cut through are recovered by interpolation.
    """
    aside = 1 - axis  # the axis averaged over; `axis` is the one the profile varies along
    luma = luminance(icon)
    counts = keep.sum(axis=aside)
    total = np.where(keep, luma, 0.0).sum(axis=aside)
    usable = counts >= max(8, round(0.15 * keep.shape[aside]))
    if usable.sum() < 0.5 * usable.shape[0]:
        return None
    profile = np.zeros(usable.shape[0], dtype=np.float64)
    profile[usable] = total[usable] / counts[usable]
    if not usable.all():  # interpolate the lines the mark and its halo pass through
        index = np.arange(usable.shape[0])
        profile = np.interp(index, index[usable], profile[usable])
    return profile


def field_grid_periodicity(icon: np.ndarray, mask: np.ndarray,
                           period_px: float = GRID_PERIOD_PX) -> float:
    """How much of the field's structure is a periodic grid, against how much is anything else.

    A field of one tone has no periodic structure; a field of one ramp has none either (the ramp is
    removed as a trend); a bloom has none. A drafting grid is a comb, and a comb is *the* shape this
    leg names: the strongest of its first four harmonics on either axis, over the median bin between
    them. The answer is dimensionless, so it does not depend on the palette's contrast or on the
    field's angle - only on whether the field repeats at a fixed spacing.
    """
    if icon.ndim != 3:
        return 0.0
    size = icon.shape[1]
    keep = _field_domain(mask)
    fundamental = size / period_px
    best = 0.0
    for axis in (0, 1):
        profile = _projection(icon, keep, axis)
        if profile is None:
            continue
        spectrum = np.fft.rfft(profile)
        spectrum[:GRID_TREND_MODES] = 0.0
        detrended = np.fft.irfft(spectrum, n=profile.shape[0])
        magnitudes = np.abs(np.fft.rfft(detrended * np.hanning(profile.shape[0])))
        bins = magnitudes.shape[0]
        harmonic_bins = {int(round(order * fundamental)) for order in range(1, GRID_HARMONICS + 1)}
        harmonic_bins = {b for b in harmonic_bins if GRID_TREND_MODES < b < bins}
        if not harmonic_bins:
            continue
        # The control is the same number of bins, half-way between the harmonics: one period apart
        # is where a grid's lines put their energy, half a period away is where the same window puts
        # none of it. Comparing against a *control band* rather than a median over all bins is what
        # keeps the answer stable when the field is a clean ramp, where every bin off the lattice
        # holds nothing but quantisation noise and a median would approach zero.
        control_bins = {int(round((order + 0.5) * fundamental))
                        for order in range(GRID_HARMONICS)}
        control_bins = {b + offset for b in control_bins for offset in (-1, 0, 1)
                        if 0 <= b + offset < bins}
        peak = max(float(magnitudes[b]) for b in harmonic_bins)
        control = max((float(magnitudes[b]) for b in control_bins), default=0.0)
        if control <= 0.0:
            rest = [float(magnitudes[b]) for b in range(GRID_TREND_MODES, bins)
                    if b not in harmonic_bins]
            control = float(np.median(rest)) if rest else 0.0
        best = max(best, peak / control if control > 0 else 0.0)
    return best


# --------------------------------------------------------------------------------------
# Presentation
# --------------------------------------------------------------------------------------


def _safe_circle_mask(shape: tuple[int, int], fraction: float) -> np.ndarray:
    """The 66dp safe circle, as a boolean mask over a canvas-fraction viewport."""
    size = shape[0]
    centre = (size - 1) / 2
    radius = size * fraction / 2
    ys, xs = np.mgrid[0:size, 0:size]
    return ((xs - centre) ** 2 + (ys - centre) ** 2) <= radius ** 2


def silhouette_stats(icon: np.ndarray, mask: np.ndarray, mark: str) -> dict:
    """Where the mark sits, how big it is, and whether a launcher mask would shave it."""
    height, width = mask.shape
    ys, xs = np.nonzero(mask)
    if ys.size == 0:
        return {"present": False}
    x0, y0, x1, y1 = int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1
    safe = float(design.GRID["safe"]) * width
    safe_area = safe * safe
    anchor = design.optical_anchor(mark)
    centroid = (float(xs.mean()), float(ys.mean()))
    anchor_px = (anchor[0] * width, anchor[1] * height)
    reach_px = float(np.sqrt(((xs - anchor_px[0]) ** 2 + (ys - anchor_px[1]) ** 2).max()))
    safe_keep = _safe_circle_mask(mask.shape, design.GRID["safe"])  # type: ignore[arg-type]
    outside = int((mask & ~safe_keep).sum())
    return {
        "present": True,
        "bbox": [x0, y0, x1, y1],
        "extent_fraction": round(max(x1 - x0, y1 - y0) / width, 4),
        "ink_area_px": int(ys.size),
        "ink_coverage_of_safe_zone": round(float(ys.size) / safe_area, 4),
        "centroid_px": [round(centroid[0], 2), round(centroid[1], 2)],
        "optical_anchor_px": [round(anchor_px[0], 2), round(anchor_px[1], 2)],
        "optical_offset_px": round(math.hypot(centroid[0] - anchor_px[0],
                                             centroid[1] - anchor_px[1]), 2),
        "reach_px": round(reach_px, 2),
        "reach_dp": round(reach_px * design.ADAPTIVE_VIEWPORT_DP / width, 3),
        "reach_fraction": round(reach_px / width, 5),
        "outside_safe_circle_px": outside,
        "inside_safe_circle": outside == 0,
    }


def downscale_contrast(icon: np.ndarray, mask: np.ndarray, size: int = 48) -> dict:
    """The mark's contrast against its own field at launcher size."""
    image = Image.fromarray(np.ascontiguousarray(icon[..., :3]).astype(np.uint8))
    small_mask = Image.fromarray((mask * 255).astype(np.uint8))
    # NEAREST for the mask: a resampled boolean edge is not a mark, and the contrast of a
    # half-covered pixel is not a contrast.
    scaled = np.asarray(image.resize((size, size), Image.Resampling.LANCZOS), dtype=np.float64)
    scaled_mask = np.asarray(small_mask.resize((size, size), Image.Resampling.NEAREST)) > 127
    if not scaled_mask.any() or scaled_mask.all():
        return {"ratio": 0.0, "mark_px": int(scaled_mask.sum()), "size": size}
    foreground = scaled[scaled_mask].mean(axis=0)
    field = scaled[~scaled_mask].mean(axis=0)
    ratio = contrast_ratio(foreground, field)
    return {
        "size": size,
        "ratio": round(ratio, 2),
        "mark_rgb": [round(float(v), 1) for v in foreground],
        "field_rgb": [round(float(v), 1) for v in field],
        "mark_px": int(scaled_mask.sum()),
        "mark_extent_px": int(max(
            np.ptp(np.nonzero(scaled_mask)[0]) if scaled_mask.any() else 0,
            np.ptp(np.nonzero(scaled_mask)[1]) if scaled_mask.any() else 0)) + 1,
    }


def mask_survival(icon: np.ndarray, mask: np.ndarray) -> dict:
    """How much of the mark each Android adaptive-icon mask would cut.

    Layers are 108dp and the launcher mask is the 72dp shape inside it; 66dp is the
    guaranteed-visible safe zone. ``evaluate_icon`` gates the 66dp circle leg, because it is the
    zone Android guarantees; the rest is context for a reviewer.
    """
    size = icon.shape[0]
    centre = (size - 1) / 2
    keep_shapes = {
        "launcher_circle_72dp": ("ellipse", design.ADAPTIVE_VISIBLE_DP / design.ADAPTIVE_VIEWPORT_DP),
        "launcher_squircle_72dp": ("rounded", design.ADAPTIVE_VISIBLE_DP / design.ADAPTIVE_VIEWPORT_DP),
        "safe_zone_circle_66dp": ("ellipse", design.ADAPTIVE_SAFE_DP / design.ADAPTIVE_VIEWPORT_DP),
        "safe_zone_square_66dp": ("square", design.ADAPTIVE_SAFE_DP / design.ADAPTIVE_VIEWPORT_DP),
    }
    total = int(mask.sum())
    report = {}
    for name, (shape, fraction) in keep_shapes.items():
        keep = Image.new("L", (size, size), 0)
        draw = ImageDraw.Draw(keep)
        side = size * fraction
        if shape == "ellipse":
            draw.ellipse((centre - side / 2, centre - side / 2,
                          centre + side / 2, centre + side / 2), fill=255)
        elif shape == "rounded":
            draw.rounded_rectangle((centre - side / 2, centre - side / 2,
                                    centre + side / 2, centre + side / 2),
                                   radius=side / 4, fill=255)
        else:
            draw.rectangle((centre - side / 2, centre - side / 2,
                            centre + side / 2, centre + side / 2), fill=255)
        keep_array = np.asarray(keep) > 127
        clipped = int((mask & ~keep_array).sum())
        report[name] = {
            "mask_extent_fraction": round(fraction, 4),
            "mark_px_clipped": clipped,
            "mark_fraction_clipped": round(clipped / max(1, total), 5),
        }
    return report


# --------------------------------------------------------------------------------------
# The verdict
# --------------------------------------------------------------------------------------


def _leg(name: str, value, ok: bool, threshold: dict) -> dict:
    return {"leg": name, "value": value, "ok": bool(ok),
            "gates": threshold["gates"], "threshold": threshold, "unit": threshold["unit"]}


def evaluate_icon(icon: np.ndarray, palette: dict, mark: str | None = None,
                  field: np.ndarray | None = None,
                  gates: dict[str, dict] | None = None) -> dict:
    """Measure one icon and return every number plus the pass/fail of each gated leg.

    `icon` is HxWx4 (or HxWx3) RGB(A), `field` the field it was drawn on when the caller knows it,
    and `mark` the registry key whose roles and optical anchor apply.
    """
    gates = ICON_GATES if gates is None else gates
    name = mark or design.DEFAULT_MARK
    alpha = (icon[..., 3] if icon.shape[-1] == 4 else np.full(icon.shape[:2], 255)).astype(np.int16)

    # The field the design declares: the caller's own render when it has one (`render_icon` passes
    # it), otherwise reconstructed from the palette through `icon_design.icon_field` - the renderer's
    # own definition. `None` only when the palette cannot name the ramp ends, which is the legacy
    # path the falsification controls take.
    if field is not None:
        reference = np.asarray(field, dtype=np.float64)
        if reference.ndim == 2:
            reference = np.repeat(reference[..., None], 3, axis=2)
    elif "ink" in palette and "ink_lift" in palette:
        reference = declared_field(icon.shape[:2], palette)
    else:
        reference = None
    mask = mark_mask(icon, reference, palette, name)
    masks = {"silhouette": mask, **role_masks(icon, palette, name, mask)}
    silhouette = silhouette_stats(icon, mask, name)
    stroke = thinnest_stroke_px(masks)
    counter = smallest_counter_px(masks)
    texture = field_high_frequency_rms(icon, mask, reference=reference)
    linearity = field_linear_ramp_rms(icon, mask, reference=reference)
    periodicity = field_grid_periodicity(icon, mask)
    downscaled = downscale_contrast(icon, mask)
    survival = mask_survival(icon, mask)
    alpha_min = int(alpha.min())
    alpha_min_corner = int(min(alpha[0, 0], alpha[0, -1], alpha[-1, 0], alpha[-1, -1]))
    min_at_48 = round(stroke["value"] * 48.0 / icon.shape[1], 3) if stroke["value"] else 0.0
    halo = float(ndimage.binary_dilation(mask, iterations=2).mean())

    measured = {
        "mark": name,
        "field_high_frequency_rms": round(texture, 4),
        "field_linear_ramp_rms": round(linearity, 4),
        "field_grid_periodicity": round(periodicity, 4),
        "thinnest_stroke_px": stroke["value"],
        "thinnest_stroke_role": stroke.get("role"),
        "thinnest_stroke_per_role": stroke["per_role"],
        "smallest_counter_px": counter["value"],
        "smallest_counter_role": counter.get("role"),
        "smallest_counter_per_role": counter["per_role"],
        "optical_offset_px": silhouette.get("optical_offset_px"),
        "ink_coverage_of_safe_zone": silhouette.get("ink_coverage_of_safe_zone"),
        "mark_reach_dp": silhouette.get("reach_dp"),
        "mark_reach_fraction": silhouette.get("reach_fraction"),
        "mark_outside_safe_circle_px": silhouette.get("outside_safe_circle_px"),
        "silhouette_extent": silhouette.get("extent_fraction"),
        "min_feature_at_48dp_px": min_at_48,
        "downscale_contrast_48px": downscaled["ratio"],
        "alpha_min": alpha_min,
        "alpha_min_corner": alpha_min_corner,
        "mark_halo_fraction": round(halo, 4),
    }
    context = {
        "silhouette": silhouette,
        "downscale_48px": downscaled,
        "mask_survival": survival,
        "field": {"high_frequency_rms": measured["field_high_frequency_rms"],
                  "best_fit_ramp_rms": measured["field_linear_ramp_rms"]},
    }

    legs: list[dict] = []

    def gate(leg: str, value, ok: bool) -> None:
        legs.append(_leg(leg, value, ok, gates[leg]))

    mark_max = gates["field_high_frequency_rms"]["max"]
    gate("field_high_frequency_rms", measured["field_high_frequency_rms"],
         measured["field_high_frequency_rms"] <= mark_max)
    gate("field_linear_ramp_rms", measured["field_linear_ramp_rms"],
         measured["field_linear_ramp_rms"] <= gates["field_linear_ramp_rms"]["max"])
    gate("thinnest_stroke_px", measured["thinnest_stroke_px"],
         measured["thinnest_stroke_px"] >= gates["thinnest_stroke_px"]["min"])
    counter_value = measured["smallest_counter_px"]
    gate("smallest_counter_px", counter_value,
         counter_value is None or counter_value >= gates["smallest_counter_px"]["min"])
    offset = measured["optical_offset_px"]
    gate("optical_offset_px", offset,
         offset is not None and offset <= gates["optical_offset_px"]["max"])
    coverage = measured["ink_coverage_of_safe_zone"]
    coverage_gate = gates["ink_coverage_of_safe_zone"]
    gate("ink_coverage_of_safe_zone", coverage,
         coverage is not None and coverage_gate["min"] <= coverage <= coverage_gate["max"])
    reach = measured["mark_reach_dp"]
    reach_gate = gates["mark_reach_dp"]
    gate("mark_reach_dp", reach,
         reach is not None and reach_gate["min"] <= reach <= reach_gate["max"])
    extent = measured["silhouette_extent"]
    extent_gate = gates["silhouette_extent"]
    gate("silhouette_extent", extent,
         extent is not None and extent_gate["min"] <= extent <= extent_gate["max"])
    outside = measured["mark_outside_safe_circle_px"]
    gate("mark_outside_safe_circle_px", outside,
         outside is not None and outside <= gates["mark_outside_safe_circle_px"]["max"])
    gate("alpha_min", alpha_min, alpha_min >= gates["alpha_min"]["min"])
    gate("downscale_contrast_48px", measured["downscale_contrast_48px"],
         measured["downscale_contrast_48px"] >= gates["downscale_contrast_48px"]["min"])
    gate("min_feature_at_48dp_px", min_at_48,
         min_at_48 >= gates["min_feature_at_48dp_px"]["min"])
    gate("mark_halo_fraction", measured["mark_halo_fraction"], True)
    gate("field_grid_periodicity", measured["field_grid_periodicity"], True)

    gated = [leg for leg in legs if leg["gates"]]
    return {
        "measured": measured,
        "context": context,
        "legs": legs,
        "gated_legs": len(gated),
        "failed_legs": [leg["leg"] for leg in gated if not leg["ok"]],
        "verdict": "PASS" if all(leg["ok"] for leg in gated) else "FAIL",
    }


def format_legs(result: dict) -> list[str]:
    """One line per leg, in the declared order: the report both visual-qa and the suite print."""
    lines = []
    for leg in result["legs"]:
        threshold = leg["threshold"]
        if not leg["gates"]:
            bound = "report"
        elif "min" in threshold and "max" in threshold:
            bound = f"[{threshold['min']}, {threshold['max']}]"
        elif "min" in threshold:
            bound = f">= {threshold['min']}"
        else:
            bound = f"<= {threshold['max']}"
        lines.append(f"  {'PASS' if leg['ok'] else 'FAIL'} {leg['leg']:32} "
                     f"{leg['value']!s:>12}  {bound}")
    return lines
