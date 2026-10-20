#!/usr/bin/env python3
"""PlayKiln icon design system: the construction grid, the primitive set, and two renderers.

Why this module exists
----------------------
The first icons this factory shipped were assembled from a diagonal gradient, two radial glows, a
32px drafting grid and a glyph placed by hand-tuned absolute pixel constants (``MARK_SHAPE``,
``BOOK_SHAPE``). The owner rejected the result in his own words - "bad and crooked, a naive silly
thing, not a designed mark" - and the cause is structural rather than a matter of taste: there was
no construction grid, no optical alignment, no declared stroke weight and no negative-space rule,
so every mark was nudged into place instead of built, and the field carried a texture that
dissolves into mush in a 48dp launcher cell.

This module states the vocabulary that was missing:

* ``GRID`` - one keyline system, in fractions of the *108dp adaptive viewport*, which is the one
  space both the store icon and the launcher layer are drawn in: the 18dp band Android reserves,
  the 72dp visible mask, the 66dp safe zone, the circle and portrait keylines a mark is built on,
  and the optical centre its mass balances on. Every vertex of every mark is a fraction of this
  grid; the absolute pixel constants are gone, and so is the second scale factor that made the
  512px listing icon and the launcher drawable disagree about the mark's size.
* ``STROKE`` - the declared stroke scale. A mark draws with ``regular`` or ``bold`` and with
  nothing else, so the marks read as a family instead of as accidents.
* ``MIN_FEATURE`` - the smallest stroke or counter, in canvas fractions, that must survive a 48dp
  render. 16px at 512 is 1.5px at 48dp, the floor at which a launcher cell still shows a line
  rather than a grey smear.
* six primitives - ``ring``, ``bar``, ``plate``, ``disc``, ``stroke``, ``polygon`` - declared once in
  canonical fractions and rendered twice: as pixels (``render_mark``) and as Android vector path data
  (``mark_layers``). The two renderers are separate implementations on purpose, and
  ``scripts/test-icon-design.py`` measures their agreement rather than trusting it, because a store
  icon and a launcher icon that drifted apart is exactly the defect this replaces.
* ``icon_field`` - one restrained tonal field: a single exact linear ramp, no drafting grid and no
  radial bloom.

Measurement lives in ``scripts/lib/icon_qa.py``, which turns the properties above into numbers a
machine can fail on.

Units: every constant is a fraction of the 108dp viewport unless its name ends in ``_dp`` or
``_px``.
"""

from __future__ import annotations

import math
from typing import Iterable, Mapping, Sequence

from PIL import Image, ImageDraw

# --------------------------------------------------------------------------------------
# The canvas and the Android adaptive-icon contract
#
# One canvas for both outputs. The store icon is the flattened adaptive icon: 512px of an 108dp
# layer, which is what makes the listing icon and the installed icon the same mark at the same
# size. The previous renderer drew the mark at 0.358 of the 512px canvas for the listing and at
# 0.268 of the launcher layer, so the two were different compositions of the same shape.
# --------------------------------------------------------------------------------------

CANVAS = 512  # the store icon edge
ADAPTIVE_VIEWPORT_DP = 108.0  # the adaptive-icon layer, i.e. what 1.0 of this canvas means
ADAPTIVE_VISIBLE_DP = 72.0  # what a launcher mask shows
ADAPTIVE_SAFE_DP = 66.0  # the guaranteed-visible safe zone (a circle of this diameter)
ADAPTIVE_SAFE_RADIUS_DP = ADAPTIVE_SAFE_DP / 2
# The mark's silhouette stops this far inside the safe circle, so a mask that follows the safe zone
# closely cannot shave an edge. 2dp is 9.5px at 512: comfortably larger than one antialiased pixel.
ADAPTIVE_MARK_CLEARANCE_DP = 2.0

# Supersampling factor for the raster mark. Pillow's ImageDraw has no antialiasing, so a straight
# 512px draw leaves staircase edges on every curve. Drawing the mark at 4x and downsampling is
# deterministic and is what makes the 48dp render smooth. 4 also makes the declared stroke scale
# land on whole supersampled pixels (16px -> 64, 24px -> 96), which `assert_mark_on_grid` requires
# so the drawn stroke is exactly the declared one.
SUPERSAMPLE = 4


def dp(value_dp: float) -> float:
    """A length in adaptive-viewport dp, as a fraction of the canvas."""
    return value_dp / ADAPTIVE_VIEWPORT_DP


def px_at(canvas_px: int, fraction: float) -> float:
    """A canvas fraction, in pixels at ``canvas_px``."""
    return fraction * canvas_px


# --------------------------------------------------------------------------------------
# The construction grid (keylines)
# --------------------------------------------------------------------------------------

# The circle keyline is the space a round or square mark is built in: the safe circle less the
# clearance, so a mark whose outermost ink lands on this keyline is 2dp inside the guarantee.
_CIRCLE_KEYLINE = dp(ADAPTIVE_SAFE_RADIUS_DP - ADAPTIVE_MARK_CLEARANCE_DP)  # 31dp -> 0.287037
# The portrait keyline: the 3:4 rectangle inscribed in the circle keyline, so a page-shaped mark
# fills the safe zone without its corners crossing it. 3:4 is the proportion of a bound page and of
# a phone screenshot, which is why it is the second keyline rather than an arbitrary rectangle.
_PORTRAIT_K = _CIRCLE_KEYLINE / 2.5  # half-diagonal of a 3k x 4k rectangle is 2.5k

GRID: dict[str, object] = {
    # named keylines, all fractions of the 108dp viewport
    "trim": 0.0,  # the viewport edge itself; no ink may cross it
    "margin": dp(18.0),  # 0.166667: the band Android reserves for masks and parallax
    "visible": dp(ADAPTIVE_VISIBLE_DP),  # 0.666667: the launcher mask edge
    "safe": dp(ADAPTIVE_SAFE_DP),  # 0.611111: the guaranteed-visible square
    "safe_radius": dp(ADAPTIVE_SAFE_RADIUS_DP),  # 0.305556: the guaranteed-visible circle
    "clearance": dp(ADAPTIVE_MARK_CLEARANCE_DP),  # 0.018519: the 2dp the ink stays inside
    "circle": _CIRCLE_KEYLINE,  # 0.287037: the keyline a round mark's ink reaches
    "portrait": (3 * _PORTRAIT_K, 4 * _PORTRAIT_K),  # 0.344444 x 0.459259, inscribed in `circle`
    "optical_center": (0.5, 0.5),  # the point a mark's visual mass is balanced on
}

OPTICAL_CENTER: tuple[float, float] = GRID["optical_center"]  # type: ignore[assignment]
KEYLINE_CIRCLE = _CIRCLE_KEYLINE
KEYLINE_PORTRAIT: tuple[float, float] = GRID["portrait"]  # type: ignore[assignment]

# Optical, not arithmetic, centring. A mark whose silhouette is centrally symmetric - a circle, an
# ellipse, a centred rectangle - has its area centroid at its geometric centre, so for those the
# arithmetic centre *is* the optical centre and no nudge is applied; that is why both marks below
# sit on (0.5, 0.5) and why `icon_qa.optical_offset_px` measuring ~0 is the proof rather than a
# coincidence. A triangular or otherwise unbalanced silhouette does NOT work that way (a triangle's
# optical centre sits a third of the way up from its base), so a future mark declares
# `optical_anchor` in its registry entry and the gate measures against it instead of the arithmetic
# centre.
OPTICAL_ANCHOR: dict[str, tuple[float, float]] = {}
OPTICAL_TOLERANCE = 0.02  # canvas fractions, i.e. 10.24px at 512

# A mark that cannot reach this much of the circle keyline looks lost in its cell. The book, which
# is fitted on its diagonal rather than on its width, reaches 0.957 of it.
MIN_REACH_RATIO = 0.75

# --------------------------------------------------------------------------------------
# The stroke scale and the negative-space floor
# --------------------------------------------------------------------------------------

# Two weights, and no others. Declared in px at 512, which is the unit the rest of the factory
# speaks: 16px is 3.375dp in the 108dp viewport and 1.5px in a 48dp launcher cell; 24px is 5.0625dp
# and 2.25px.
STROKE: dict[str, float] = {
    "regular": 16.0 / CANVAS,
    "bold": 24.0 / CANVAS,
}
MIN_FEATURE = STROKE["regular"]  # the smallest stroke or counter a mark may declare
# A counter - the enclosed negative space inside a letterform or a glyph - must clear the same
# floor as a stroke, because a counter that fills in is a stroke that changed shape.
MIN_COUNTER = MIN_FEATURE

# --------------------------------------------------------------------------------------
# The field
# --------------------------------------------------------------------------------------

# One linear gradient, top-left to bottom-right. This is the same direction the adaptive background
# drawable declares as android:angle="315" (Android measures the angle counter-clockwise with 0 =
# left-to-right, Pillow measures it clockwise with 0 = right, so 315 and 45 are the same
# direction), which is what makes the store icon and the launcher icon read as one field.
FIELD_ANGLE_DEG = 45.0
# The Android XML equivalent of FIELD_ANGLE_DEG, for `write_vectors`.
FIELD_ANDROID_ANGLE = 315

# The field's declared overlays: the waterline.
#
# The field is not one bare ramp. One translucent polygon darkens everything above the waterline in
# `ink` and a second lightens everything below it in `lane`, so the lower half reads as lit water
# and the pair of bands read as one surface seen across a lane - a darker field above, where the
# `amber` minute hand needs the contrast. The two polygons share the diagonal from (0, 74) to
# (108, 30), so together they tile the viewport exactly and neither overlaps the other.
#
# The geometry is declared once, here, in the 108-unit viewport the vector renderer speaks, and both
# renderers consume this one list: `icon_field` composites it into the store raster and
# `store-studio.write_vectors` emits it as `<path>` data, so the 512 listing icon and the launcher
# background cannot be two different fields - which is exactly the drift this file was written from
# (the launcher drew the waterline while the listing icon drew the bare ramp).
#
# A slot draws the waterline by declaring the roles it names. `field_overlays` returns nothing to a
# palette that cannot resolve them all - the factory's earlier slots declare no `lane` - so such a
# slot keeps the ramp it was designed with rather than half a waterline in a colour it never chose.
FIELD_OVERLAYS: tuple[dict, ...] = (
    {"role": "lane", "alpha": 0.24,
     "points": ((0.0, 74.0), (108.0, 30.0), (108.0, 108.0), (0.0, 108.0))},
    {"role": "ink", "alpha": 0.20,
     "points": ((0.0, 0.0), (108.0, 0.0), (108.0, 30.0), (0.0, 74.0))},
)


def _field_overlay_path_data(points: Sequence[tuple[float, float]]) -> str:
    """A polygon as Android path data, in the 108-unit viewport.

    The first segment is always an explicit ``L`` and later segments use ``V``/``H`` where a
    coordinate does not change; that is the form the shipped drawables carry, and reproducing it
    keeps the emitted vector byte-identical to the file it replaces rather than merely equivalent.
    """
    x, y = points[0]
    command = [f"M{x:g},{y:g}"]
    for index, (px, py) in enumerate(points[1:]):
        if index == 0 or (px != x and py != y):
            command.append(f"L{px:g},{py:g}")
        elif px == x:
            command.append(f"V{py:g}")
        else:
            command.append(f"H{px:g}")
        x, y = px, py
    command.append("Z")
    return "".join(command)


def field_overlays(present_roles: Iterable[str] | None = None) -> tuple[dict, ...]:
    """The declared field overlays, in write order, each with its role, alpha and path data.

    `present_roles` is the palette roles a caller can resolve. When it is given and omits any role
    the waterline names, the whole waterline is withheld rather than half drawn: a partial field is a
    third picture, not a cheaper one. `None` returns the full declaration for a caller that has
    already resolved the roles.
    """
    if present_roles is not None:
        have = set(present_roles)
        if any(overlay["role"] not in have for overlay in FIELD_OVERLAYS):
            return ()
    return tuple({**overlay, "data": _field_overlay_path_data(overlay["points"])}
                 for overlay in FIELD_OVERLAYS)


# --------------------------------------------------------------------------------------
# Primitives
#
# A primitive is a dict in canonical canvas fractions. `role` names a palette role.
#   {"kind": "ring",   "role", "center", "radius", "weight"}          stroked circle
#   {"kind": "bar",    "role", "center", "size"}                      filled rectangle
#   {"kind": "plate",  "role", "center", "size", "radius", "corners"} filled rounded rect
#   {"kind": "disc",   "role", "center", "radius"}                    filled circle
#   {"kind": "stroke", "role", "points", "weight"}                    stroked polyline
#   {"kind": "polygon","role", "points"}                              filled polygon
# --------------------------------------------------------------------------------------


def ring(role: str, center: tuple[float, float], radius: float, weight: str) -> dict:
    return {"kind": "ring", "role": role, "center": center, "radius": radius, "weight": weight}


def bar(role: str, center: tuple[float, float], size: tuple[float, float]) -> dict:
    return {"kind": "bar", "role": role, "center": center, "size": size}


def plate(role: str, center: tuple[float, float], size: tuple[float, float], radius: float,
          corners: object = "all") -> dict:
    return {"kind": "plate", "role": role, "center": center, "size": size,
            "radius": radius, "corners": corners}


def disc(role: str, center: tuple[float, float], radius: float) -> dict:
    return {"kind": "disc", "role": role, "center": center, "radius": radius}


def stroke(role: str, points: Sequence[tuple[float, float]], weight: str) -> dict:
    return {"kind": "stroke", "role": role, "points": tuple(points), "weight": weight}


def polygon(role: str, points: Sequence[tuple[float, float]]) -> dict:
    """A filled polygon whose vertices are canonical canvas fractions."""
    return {"kind": "polygon", "role": role, "points": tuple(points)}


# --------------------------------------------------------------------------------------
# The marks
#
# Every mark is built on a keyline and on the declared stroke scale, and every mark below is
# centrally symmetric, so its arithmetic centre is its optical centre.
#
# `pipe` - a pipe cross-section measured across its bore: an annulus (the pipe wall) on the circle
# keyline with a dimension line spanning the inner diameter and a tick at each end, the tick
# standing one `regular` stroke proud of the line on each side. The wall's outer edge lies exactly
# on the circle keyline, which is what makes the silhouette a circle of a declared size.
#
# `mark-book` - a bound, marked page on the portrait keyline: a darker spine, three binding rings
# punched through it, and an amber check in the page's own content area. The page is the
# silhouette; the spine and the check are interior detail and are therefore derived from the page
# rectangle rather than from the canvas, so a page that moves takes its detail with it.
#
# `mark-stripes` - the mown-stripe fan: four bold strokes converging toward a vanishing point above
# them, the pattern a mower leaves on a lawn. It is the one mark here whose silhouette is neither a
# disc nor a page, so it declares the circle keyline it is *measured against* rather than a keyline
# it is inscribed in: its outermost ink reaches 91% of that bound (beside `pipe`'s 100% and
# `mark-book`'s 96%) and every reach gate reads the same bound.
# --------------------------------------------------------------------------------------

# pipe
_PIPE_WALL = STROKE["bold"]  # 0.046875 -> 24px: the wall is one declared bold stroke
_PIPE_RADIUS = _CIRCLE_KEYLINE - _PIPE_WALL / 2  # centreline; +bold/2 reaches the circle keyline
_PIPE_BORE = _PIPE_RADIUS - _PIPE_WALL / 2  # where the wall's inner edge sits
_PIPE_TICK_HALF = _PIPE_WALL / 2 + STROKE["regular"]  # the tick stands one stroke proud each side

PIPE: tuple[dict, ...] = (
    ring("amber", OPTICAL_CENTER, _PIPE_RADIUS, "bold"),
    bar("paper", OPTICAL_CENTER, (_PIPE_BORE * 2, _PIPE_WALL)),
    bar("paper", (0.5 - _PIPE_BORE, 0.5), (_PIPE_WALL, _PIPE_TICK_HALF * 2)),
    bar("paper", (0.5 + _PIPE_BORE, 0.5), (_PIPE_WALL, _PIPE_TICK_HALF * 2)),
)

# mark-book
_BOOK_W, _BOOK_H = KEYLINE_PORTRAIT
_BOOK_CORNER = STROKE["regular"]  # 16px: a page corner, and the page's smallest radius
# The spine is four regular strokes wide and a punched binding ring takes one bold stroke across it,
# which leaves one and a quarter regular strokes (20px) of binding margin on each side of every ring.
# Both the ring's counter (24px) and the margin beside it (20px) therefore clear the 16px floor with
# room to spare; the first version of this mark left 8px walls, half the floor, which the icon gate
# caught as `thinnest_stroke_px 8.0 >= 16.0` before this file was allowed to render it.
_BOOK_SPINE_W = 4 * STROKE["regular"]  # 64px
_BOOK_LEFT = 0.5 - _BOOK_W / 2
_BOOK_TOP = 0.5 - _BOOK_H / 2
_BOOK_SPINE_X = _BOOK_LEFT + _BOOK_SPINE_W / 2
# 12px radius -> a 24px punched ring, one bold stroke across and half again the negative-space floor.
_BOOK_RING_RADIUS = STROKE["bold"] / 2
_BOOK_RING_FRACTIONS = (0.26, 0.50, 0.74)  # of the page height: the binding rings
# The check, in page-relative fractions (x, y) of the page rectangle. Interior detail, so it is
# derived from the page rather than from the canvas - but it is also *negative space* the moment it
# lands near an edge, because the paper left beside it is a feature like any other. Every point
# therefore keeps the content area's clearance: the paper between the check's outer edge and the
# spine, and between it and the page's top, right and bottom edges, is at least one regular stroke.
# The pre-change check crossed onto the spine and left 9.4px of paper at the page edge; both measured
# below the floor.
_CHECK_FRACTIONS = ((0.56, 0.60), (0.65, 0.70), (0.83, 0.42))


def _book_point(x_fraction: float, y_fraction: float) -> tuple[float, float]:
    return (_BOOK_LEFT + x_fraction * _BOOK_W, _BOOK_TOP + y_fraction * _BOOK_H)


def _book_ring_ys() -> tuple[float, ...]:
    return tuple(_BOOK_TOP + fraction * _BOOK_H for fraction in _BOOK_RING_FRACTIONS)


BOOK: tuple[dict, ...] = (
    plate("paper", OPTICAL_CENTER, (_BOOK_W, _BOOK_H), _BOOK_CORNER, "all"),
    plate("ink_lift", (_BOOK_SPINE_X, 0.5), (_BOOK_SPINE_W, _BOOK_H), _BOOK_CORNER,
          (True, False, False, True)),
    *[disc("paper", (_BOOK_SPINE_X, y), _BOOK_RING_RADIUS) for y in _book_ring_ys()],
    stroke("amber", [_book_point(x, y) for x, y in _CHECK_FRACTIONS], "bold"),
)

# mark-stripes
# Four strokes at the one declared bold weight, each a straight two-point line from topY to bottomY
# with round caps, and each stripe's top point nearer the vertical axis than its bottom point so the
# four converge upward toward a vanishing point. The two outer stripes are `paper` and the two inner
# ones `amber`, which is the mown-stripe pattern: one chalk pass beside one cut accent.
#
# The fan does not sit on a keyline the way `pipe` sits on the circle or `mark-book` on the portrait
# rectangle - its silhouette is a trapezoid, and there is no keyline for one. It is therefore placed
# to the geometry the slot declared and bounded by the *reach* gate: the outermost ink lands at
# 0.2616 of the canvas, 91% of the circle keyline (beside `pipe`'s 100% and `mark-book`'s 96%), and
# the top/bottom pairs mirror about x = 0.5, which puts the silhouette's centre exactly on the
# optical anchor with no nudge.
_STRIPES_TOP_Y = 0.350
_STRIPES_BOTTOM_Y = 0.650
# (topX, bottomX, role): the two chalk outer stripes outside the two cut-accent inner ones.
_STRIPES_AXES: tuple[tuple[float, float, str], ...] = (
    (0.385, 0.315, "paper"),
    (0.462, 0.438, "amber"),
    (0.538, 0.562, "amber"),
    (0.615, 0.685, "paper"),
)

STRIPES: tuple[dict, ...] = tuple(
    stroke(role, ((top_x, _STRIPES_TOP_Y), (bottom_x, _STRIPES_BOTTOM_Y)), "bold")
    for top_x, bottom_x, role in _STRIPES_AXES
)

# mark-clock
# A pace clock: one bold ring and two bold hands meeting at the pivot. The minute hand runs to 60
# degrees and the hour hand back to 200, so the pair never settles into one straight diameter the
# way an opposed pair would - at 48dp the two arms are what say "a time to swim, not a gauge",
# beside `pipe`'s full-width bore. The ring's outer edge lands exactly on the circle keyline (the
# same construction `pipe` uses) and each hand's round cap stops a stroke and a half short of the
# dial's inner edge, so the face never fills in.
_CLOCK_HALF = STROKE["bold"] / 2
_CLOCK_RING_R = _CIRCLE_KEYLINE - _CLOCK_HALF  # centreline: the outer edge lands on the keyline
_CLOCK_DIAL_R = _CLOCK_RING_R - _CLOCK_HALF  # the ring's inner edge - where a hand must not reach
# One stroke has to stay open between a hand's ink and the dial, and the round cap is itself half a
# bold stroke past the tip, so the tip's centreline stops one bold stroke plus one and a half regular
# strokes inside the dial.
_CLOCK_HAND_CLEARANCE = _CLOCK_HALF + 1.5 * STROKE["regular"]
_CLOCK_MINUTE_R = _CLOCK_DIAL_R - _CLOCK_HAND_CLEARANCE
_CLOCK_HOUR_R = _CLOCK_MINUTE_R * 0.68  # shorter than the minute hand, the way a clock reads


def _clock_point(angle_deg: float, radius: float) -> tuple[float, float]:
    """A point `radius` from the optical centre, `angle_deg` counter-clockwise from three o'clock."""
    radians = math.radians(angle_deg)
    return (OPTICAL_CENTER[0] + radius * math.cos(radians),
            OPTICAL_CENTER[1] - radius * math.sin(radians))


CLOCK: tuple[dict, ...] = (
    ring("paper", OPTICAL_CENTER, _CLOCK_RING_R, "bold"),
    stroke("amber", (OPTICAL_CENTER, _clock_point(60.0, _CLOCK_MINUTE_R)), "bold"),
    stroke("paper", (OPTICAL_CENTER, _clock_point(200.0, _CLOCK_HOUR_R)), "bold"),
)

# mark-unit-plan
# UnitWalk's mark: a rental unit read in plan - the walls as one closed bold stroke, and the walk's
# tick inside them. The room is wider than it is deep, because a unit in plan is a rectangle and not
# a page; the round join of the stroke is the corner radius, so the plan sits on the same 24px bold
# weight as every other mark here rather than on a hand-chosen curve.
#
# The tick is interior detail and is therefore derived from the room rectangle rather than from the
# canvas, exactly as `mark-book` derives its check from the page: a room that moves takes its tick
# with it. The wall's centreline stops half a bold stroke inside the circle keyline, which is what
# puts the outermost ink on the keyline rather than past it - the reach gate reads 30.71dp of the
# 31dp bound, the same construction `pipe` and `mark-clock` use.
_UNIT_HALF_W = 0.205  # half the wall centreline's width: 0.41 of the canvas
_UNIT_HALF_H = 0.160  # half its depth: the room is wider than it is deep
_UNIT_LEFT = OPTICAL_CENTER[0] - _UNIT_HALF_W
_UNIT_TOP = OPTICAL_CENTER[1] - _UNIT_HALF_H
# The tick, in room-relative fractions (x, y) of the wall rectangle. It sits clear of every wall:
# the paper between the tick's outer edge and the nearest wall is more than one regular stroke, so
# the negative space inside the room stays a feature rather than a smear at 48dp.
_UNIT_TICK_FRACTIONS = ((0.30, 0.494), (0.427, 0.675), (0.70, 0.281))


def _unit_point(x_fraction: float, y_fraction: float) -> tuple[float, float]:
    return (_UNIT_LEFT + x_fraction * 2 * _UNIT_HALF_W,
            _UNIT_TOP + y_fraction * 2 * _UNIT_HALF_H)


UNIT_PLAN: tuple[dict, ...] = (
    stroke("paper", ((_UNIT_LEFT, _UNIT_TOP),
                     (_UNIT_LEFT + 2 * _UNIT_HALF_W, _UNIT_TOP),
                     (_UNIT_LEFT + 2 * _UNIT_HALF_W, _UNIT_TOP + 2 * _UNIT_HALF_H),
                     (_UNIT_LEFT, _UNIT_TOP + 2 * _UNIT_HALF_H),
                     (_UNIT_LEFT, _UNIT_TOP)), "bold"),
    stroke("amber", [_unit_point(x, y) for x, y in _UNIT_TICK_FRACTIONS], "bold"),
)

# mark-lumintra
# A light's path over the horizon: one bold horizon line, the body's arc above it as a bold
# half-ring broken where the body sits, the body itself as an ember disc on the arc at 55°, and
# one small star in the dark upper left. Geometry is the slot's store/studio.json `_mark` note,
# in the same fractions every other mark here uses. The arc is two stroked polylines (not a ring)
# because the body breaks the path: left piece 180°→83°, right piece 27°→0°, both sampled on the
# circle whose centre sits on the horizon and whose centreline radius is 19.5dp. Outermost ink is
# the horizon's round cap at ~30.58dp of the 31dp circle keyline.
_LUMINTRA_HORIZON_Y = 0.601852
_LUMINTRA_ARC_CENTER = (0.5, _LUMINTRA_HORIZON_Y)
_LUMINTRA_ARC_R = 0.180556  # 19.5dp centreline
_LUMINTRA_BODY = (0.603611, 0.453981)  # on the arc at 55°
_LUMINTRA_BODY_R = 0.046296  # 5dp
_LUMINTRA_STAR = (0.356481, 0.384259)
_LUMINTRA_STAR_R = 0.018519  # 2dp radius → 4dp diameter, above the 16px MIN_FEATURE


def _lumintra_point(angle_deg: float, radius: float = _LUMINTRA_ARC_R) -> tuple[float, float]:
    """A point on the lumintra arc: `angle_deg` counter-clockwise from three o'clock, y up."""
    radians = math.radians(angle_deg)
    cx, cy = _LUMINTRA_ARC_CENTER
    return (cx + radius * math.cos(radians), cy - radius * math.sin(radians))


def _lumintra_arc(start_deg: float, end_deg: float, steps: int) -> tuple[tuple[float, float], ...]:
    """Sample a circular arc from `start_deg` to `end_deg` (inclusive) in `steps` vertices."""
    count = max(2, steps)
    return tuple(
        _lumintra_point(start_deg + (end_deg - start_deg) * i / (count - 1))
        for i in range(count)
    )


LUMINTRA: tuple[dict, ...] = (
    stroke("paper", ((0.259259, _LUMINTRA_HORIZON_Y), (0.740741, _LUMINTRA_HORIZON_Y)), "bold"),
    stroke("paper", _lumintra_arc(180.0, 83.0, 16), "bold"),
    stroke("paper", _lumintra_arc(27.0, 0.0, 8), "bold"),
    disc("amber", _LUMINTRA_BODY, _LUMINTRA_BODY_R),
    disc("paper", _LUMINTRA_STAR, _LUMINTRA_STAR_R),
)

# Lightfold: a print standing on a lit plinth - the exhibit, its lit top plane and its shaded front.
#
# Every shape here is a `plate` (a filled rounded rectangle) or a `disc`, and that is a measured
# decision rather than a stylistic one. The gate's stroke sensor reads a shape's medial axis, so a
# filled polygon tapers to nothing at each corner and a 53-degree vertex alone reports a 2px stroke;
# the first faceted version of this mark failed `thinnest_stroke_px`, `smallest_counter_px` and
# `min_feature_at_48dp_px` on exactly those tapers, and it shipped a silhouette split into two
# disconnected pieces because the object floated above the slab. A rounded rectangle with a radius
# at most half its height has no taper at all: its medial axis is a plain segment, so its measured
# width is its own height - 28dp for the print, 8dp for the cap and the body, 13dp for the disc.
#
# The three masses are not decoration. `optical_offset_px` measures the silhouette centroid against
# the canvas centre, and an object on a base sits high by construction; the plinth is therefore a
# full 8dp cap plus an 8dp body, whose weight below the centre balances the print above it, leaving
# the offset at 8.9px against a 10.24px tolerance.
#
# The bounding box is x 36..72, y 33..75, centred on (54,54) to the unit, and the outermost ink is
# (72,75) at 27.7dp of the 31dp circle keyline. The print overlaps the cap by 1dp and the cap its
# body by 1dp, so the three read as one glyph rather than three floating bars.
LIGHTFOLD: tuple[dict, ...] = (
    plate("amber_deep", (dp(54), dp(71)), (dp(34), dp(8)), dp(4)),
    plate("amber", (dp(54), dp(64)), (dp(36), dp(8)), dp(4)),
    plate("paper", (dp(54), dp(47)), (dp(24), dp(28)), dp(7)),
    disc("amber_deep", (dp(54), dp(46)), dp(6.5)),
)

MARK_REGISTRY: dict[str, dict] = {
    "pipe": {"primitives": PIPE, "keyline": "circle",
             "note": "a pipe cross-section measured across its bore"},
    "mark-book": {"primitives": BOOK, "keyline": "portrait",
                  "note": "a bound, marked page with three binding rings and an amber check"},
    "mark-stripes": {"primitives": STRIPES, "keyline": "circle",
                     "note": "four mown stripes converging toward a vanishing point above them"},
    "mark-clock": {"primitives": CLOCK, "keyline": "circle",
                   "note": "a deck pace clock: a bold ring with a minute hand and an hour hand"},
    "mark-unit-plan": {"primitives": UNIT_PLAN, "keyline": "circle",
                       "note": "a rental unit in plan: the walls as one closed bold stroke with "
                               "the walk's tick inside them"},
    "mark-lumintra": {"primitives": LUMINTRA, "keyline": "circle",
                      "note": "a light on its arc over the horizon: horizon, broken arc, ember "
                              "body and one star"},
    "mark-plinth": {"primitives": LIGHTFOLD, "keyline": "circle",
                    "note": "Lightfold: a print standing on a lit plinth, in four material tones"},
}
DEFAULT_MARK = "pipe"


def mark_primitives(mark: str | None = None) -> tuple[dict, ...]:
    name = mark or DEFAULT_MARK
    if name not in MARK_REGISTRY:
        raise SystemExit(
            f"BLOCKED: unknown studio mark {name!r}; choose one of {', '.join(MARK_REGISTRY)}"
        )
    return MARK_REGISTRY[name]["primitives"]


def stroke_weight(weight: str) -> float:
    if weight not in STROKE:
        raise SystemExit(
            f"BLOCKED: undeclared stroke weight {weight!r}; the declared scale is "
            f"{', '.join(f'{k}={v * CANVAS:.0f}px' for k, v in STROKE.items())}. Every mark draws "
            "at a declared weight so a set of marks reads as one family."
        )
    return STROKE[weight]


# --------------------------------------------------------------------------------------
# Geometry
# --------------------------------------------------------------------------------------


def _stroke_outline(points: Sequence[tuple[float, float]], half: float
                    ) -> list[tuple[tuple[float, float], ...]]:
    """A stroked polyline as axis-free quads plus a disc at every vertex.

    The union of the segment rectangles and the vertex discs *is* the stroked polyline with round
    joins and round caps, and emitting it as separate subpaths means both renderers get the same
    shape without either of them implementing a stroke offsetter: the raster draws the polygons,
    and the vector emits them under the default non-zero fill rule.

    Both the quads and the vertex discs are wound the same way on purpose. Under the non-zero rule
    an overlap of two subpaths wound in opposite directions cancels to empty, so a mixed winding
    would punch a hole at every vertex of the vector - visible only in the shipped launcher icon.
    """
    pts = [(float(x), float(y)) for x, y in points]
    subpaths: list[tuple[tuple[float, float], ...]] = []
    half = float(half)
    for (ax, ay), (bx, by) in zip(pts, pts[1:]):
        dx, dy = bx - ax, by - ay
        length = math.hypot(dx, dy)
        if length == 0:
            continue
        nx, ny = -dy / length * half, dx / length * half
        subpaths.append(((ax + nx, ay + ny), (bx + nx, by + ny),
                         (bx - nx, by - ny), (ax - nx, ay - ny)))
    for px, py in pts:
        subpaths.append(tuple((px + half * math.cos(-step * math.pi / 24),
                               py + half * math.sin(-step * math.pi / 24))
                              for step in range(48)))
    return subpaths


def _rounded_corner_points(cx: float, cy: float, half_w: float, half_h: float, radius: float,
                           corners: object) -> list[tuple[float, float]]:
    """The outline of a rounded rectangle, sampled fine enough for a reach measurement."""
    if corners == "all":
        tl = tr = br = bl = True
    else:
        tl, tr, br, bl = corners  # type: ignore[misc]
    radius = min(radius, half_w, half_h)
    points: list[tuple[float, float]] = []
    # (corner centre, start angle, end angle), counter-clockwise in screen terms is irrelevant
    # here: this is a reach measurement, not a winding.
    arcs = (
        (tl, (cx - half_w + radius, cy - half_h + radius), math.pi, 1.5 * math.pi),
        (tr, (cx + half_w - radius, cy - half_h + radius), 1.5 * math.pi, 2.0 * math.pi),
        (br, (cx + half_w - radius, cy + half_h - radius), 0.0, 0.5 * math.pi),
        (bl, (cx - half_w + radius, cy + half_h - radius), 0.5 * math.pi, math.pi),
    )
    for rounded, (ax, ay), start, end in arcs:
        if rounded and radius > 0:
            points.extend(
                (ax + radius * math.cos(start + (end - start) * step / 8),
                 ay + radius * math.sin(start + (end - start) * step / 8))
                for step in range(9)
            )
        else:
            points.append((ax, ay))
    return points


def primitive_extent(primitive: dict) -> tuple[float, float, float, float]:
    """The primitive's bounding box in canonical fractions."""
    kind = primitive["kind"]
    if kind == "ring":
        cx, cy = primitive["center"]
        reach = primitive["radius"] + stroke_weight(primitive["weight"]) / 2
        return (cx - reach, cy - reach, cx + reach, cy + reach)
    if kind in ("bar", "plate"):
        cx, cy = primitive["center"]
        half_w, half_h = primitive["size"][0] / 2, primitive["size"][1] / 2
        return (cx - half_w, cy - half_h, cx + half_w, cy + half_h)
    if kind == "disc":
        cx, cy = primitive["center"]
        radius = primitive["radius"]
        return (cx - radius, cy - radius, cx + radius, cy + radius)
    if kind == "stroke":
        half = stroke_weight(primitive["weight"]) / 2
        xs = [x for x, _ in primitive["points"]]
        ys = [y for _, y in primitive["points"]]
        return (min(xs) - half, min(ys) - half, max(xs) + half, max(ys) + half)
    if kind == "polygon":
        xs = [x for x, _ in primitive["points"]]
        ys = [y for _, y in primitive["points"]]
        return (min(xs), min(ys), max(xs), max(ys))
    raise SystemExit(f"BLOCKED: unknown icon primitive kind {kind!r}")


def primitive_reach(primitive: dict, anchor: tuple[float, float]) -> float:
    """How far the primitive's outermost ink lies from ``anchor``, in canvas fractions.

    Measured on the actual outline rather than on the bounding box: a ring's box corner is empty,
    and measuring it would shrink a round mark by 30% to protect a corner that holds no ink.
    """
    kind = primitive["kind"]
    if kind == "ring":
        cx, cy = primitive["center"]
        return (math.hypot(cx - anchor[0], cy - anchor[1]) + primitive["radius"]
                + stroke_weight(primitive["weight"]) / 2)
    if kind == "disc":
        cx, cy = primitive["center"]
        return math.hypot(cx - anchor[0], cy - anchor[1]) + primitive["radius"]
    if kind == "bar":
        cx, cy = primitive["center"]
        half_w, half_h = primitive["size"][0] / 2, primitive["size"][1] / 2
        return max(math.hypot(cx + sx * half_w - anchor[0], cy + sy * half_h - anchor[1])
                   for sx in (-1, 1) for sy in (-1, 1))
    if kind == "plate":
        cx, cy = primitive["center"]
        half_w, half_h = primitive["size"][0] / 2, primitive["size"][1] / 2
        return max(math.hypot(x - anchor[0], y - anchor[1])
                   for x, y in _rounded_corner_points(
                       cx, cy, half_w, half_h, primitive["radius"], primitive["corners"]))
    if kind == "stroke":
        half = stroke_weight(primitive["weight"]) / 2
        return max(math.hypot(x - anchor[0], y - anchor[1]) + half for x, y in primitive["points"])
    if kind == "polygon":
        return max(math.hypot(x - anchor[0], y - anchor[1])
                   for x, y in primitive["points"])
    raise SystemExit(f"BLOCKED: unknown icon primitive kind {kind!r}")


def silhouette_bounds(mark: str | None = None) -> tuple[float, float, float, float]:
    """The mark's silhouette bounding box in canonical fractions."""
    boxes = [primitive_extent(primitive) for primitive in mark_primitives(mark)]
    return (min(b[0] for b in boxes), min(b[1] for b in boxes),
            max(b[2] for b in boxes), max(b[3] for b in boxes))


def optical_anchor(mark: str | None = None) -> tuple[float, float]:
    """The point this mark's visual mass must balance on."""
    return OPTICAL_ANCHOR.get(mark or DEFAULT_MARK, OPTICAL_CENTER)


def mark_reach(mark: str | None = None) -> float:
    """How far the mark's outermost ink lies from its optical anchor, in canvas fractions."""
    anchor = optical_anchor(mark)
    return max(primitive_reach(primitive, anchor) for primitive in mark_primitives(mark))


def mark_reach_dp(mark: str | None = None) -> float:
    """The same reach in adaptive-viewport dp, the unit `check-adaptive-icon.py` measures in."""
    return mark_reach(mark) * ADAPTIVE_VIEWPORT_DP


def mark_silhouette_extent(mark: str | None = None) -> float:
    """The mark's longest silhouette side, in canvas fractions."""
    x0, y0, x1, y1 = silhouette_bounds(mark)
    return max(x1 - x0, y1 - y0)


def assert_mark_on_grid(mark: str | None = None) -> None:
    """Refuse a mark that is not built on the grid.

    Checked at render time, so a mark cannot be added with an undeclared stroke weight, an
    off-centre silhouette, a silhouette that overruns the live keyline, or ink outside the safe
    circle a launcher masks to.
    """
    name = mark or DEFAULT_MARK
    for primitive in mark_primitives(name):
        if "weight" not in primitive:
            continue
        weight = stroke_weight(primitive["weight"])
        if weight < MIN_FEATURE - 1e-9:
            raise SystemExit(
                f"BLOCKED: mark {name!r} draws a {primitive['weight']} stroke of "
                f"{weight * CANVAS:.1f}px, below the {MIN_FEATURE * CANVAS:.0f}px minimum "
                f"feature ({MIN_FEATURE * 48:.2f}px at 48dp): it would smear out in a launcher "
                "cell. Use `regular` or wider."
            )
        drawn = weight * CANVAS * SUPERSAMPLE
        if abs(drawn - round(drawn)) > 1e-6:
            raise SystemExit(
                f"BLOCKED: mark {name!r}'s {primitive['weight']} stroke is "
                f"{weight * CANVAS:.3f}px at 512, which is {drawn:.3f}px at the {SUPERSAMPLE}x "
                "supersampled draw. A stroke that cannot land on a whole supersampled pixel is "
                "rounded by the renderer, so the drawn stroke would not be the declared one."
            )
    x0, y0, x1, y1 = silhouette_bounds(name)
    anchor = optical_anchor(name)
    centre = ((x0 + x1) / 2, (y0 + y1) / 2)
    offset = math.hypot(centre[0] - anchor[0], centre[1] - anchor[1])
    if offset > OPTICAL_TOLERANCE:
        raise SystemExit(
            f"BLOCKED: mark {name!r}'s silhouette centre {tuple(round(v, 4) for v in centre)} "
            f"sits {offset * CANVAS:.1f}px from its optical anchor "
            f"{tuple(round(v, 4) for v in anchor)}; the tolerance is "
            f"{OPTICAL_TOLERANCE * CANVAS:.1f}px. Move the mark, or declare the anchor its shape "
            "actually has (a triangle is not centred on its bounding box)."
        )
    reach = mark_reach(name)
    keyline = KEYLINE_CIRCLE
    if reach > keyline + 1e-9:
        raise SystemExit(
            f"BLOCKED: mark {name!r} reaches {reach * ADAPTIVE_VIEWPORT_DP:.2f}dp from its optical "
            f"anchor, outside the {keyline * ADAPTIVE_VIEWPORT_DP:.2f}dp circle keyline, which is "
            f"the {ADAPTIVE_SAFE_RADIUS_DP:.0f}dp safe radius less the "
            f"{ADAPTIVE_MARK_CLEARANCE_DP:.0f}dp clearance. A launcher mask would crop it; build "
            "the mark on the keyline instead of enlarging it."
        )
    if reach < MIN_REACH_RATIO * keyline:
        raise SystemExit(
            f"BLOCKED: mark {name!r} reaches only {reach * ADAPTIVE_VIEWPORT_DP:.2f}dp from its "
            f"optical anchor, {reach / keyline:.0%} of the "
            f"{keyline * ADAPTIVE_VIEWPORT_DP:.2f}dp circle keyline. Below {MIN_REACH_RATIO:.0%} "
            "the mark looks lost inside its cell; grow it onto the keyline."
        )
    margin = float(GRID["margin"])  # type: ignore[arg-type]
    if x0 < margin - 1e-9 or y0 < margin - 1e-9 or x1 > 1 - margin + 1e-9 or y1 > 1 - margin + 1e-9:
        raise SystemExit(
            f"BLOCKED: mark {name!r} spans x=[{x0:.4f}, {x1:.4f}] y=[{y0:.4f}, {y1:.4f}], crossing "
            f"the {margin:.4f} margin keyline (the {18}dp band Android reserves on each side)."
        )
    visible = float(GRID["visible"])  # type: ignore[arg-type]
    if mark_silhouette_extent(name) > visible + 1e-9:
        raise SystemExit(
            f"BLOCKED: mark {name!r}'s silhouette is {mark_silhouette_extent(name):.4f} of the "
            f"canvas, wider than the {visible:.4f} visible mask. Its silhouette would be cropped "
            "by every launcher mask, not just the aggressive ones."
        )


# --------------------------------------------------------------------------------------
# The field
# --------------------------------------------------------------------------------------


def _composite_field_overlays(image: Image.Image, palette: Mapping[str, Iterable[int]],
                              overlays: Sequence[dict]) -> Image.Image:
    """Draw the declared field overlays over a rendered ramp, through the slot's own palette.

    Each overlay is alpha-composited as its own whole layer rather than drawn straight on to the
    field, because Pillow's ``ImageDraw`` writes an RGBA fill's alpha into the pixel instead of
    blending it - drawing on the field directly would replace the ramp with a flat colour, and
    drawing two polygons on to one layer would replace the first where they overlap. One layer per
    overlay keeps the raster's stacking the same as the vector's, which emits one ``<path>`` per
    overlay in this order.
    """
    scale = image.size[0] / ADAPTIVE_VIEWPORT_DP
    result = image.convert("RGBA")
    for overlay in overlays:
        layer = Image.new("RGBA", image.size, (0, 0, 0, 0))
        ImageDraw.Draw(layer).polygon(
            [(x * scale, y * scale) for x, y in overlay["points"]],
            fill=_rgb(palette, overlay["role"]) + (round(overlay["alpha"] * 255),))
        result = Image.alpha_composite(result, layer)
    return result.convert("RGB")


def icon_field(size: int, low: tuple[int, int, int], high: tuple[int, int, int],
               angle_deg: float = FIELD_ANGLE_DEG,
               palette: Mapping[str, Iterable[int]] | None = None) -> Image.Image:
    """The icon's field: one exact linear ramp, plus the declared waterline when a palette is given.

    The ramp is computed per pixel in floating point rather than on a small grid and scaled up. The
    scaled version (the previous implementation, and the reason the old field measured 0.18 luma of
    high-frequency residual) rounds twice and interpolates once, which is a texture in the field
    for no benefit: the exact ramp is one plane, and a plane has no high-frequency energy at all.

    `palette` is the slot's resolved palette. When it is supplied the field also carries
    `FIELD_OVERLAYS` - the declared waterline - drawn in that palette's own roles, which is what
    makes the store icon and the launcher background the same field. A palette that cannot resolve
    every role the waterline names gets the ramp alone: that is a slot which never declared the
    waterline, and it must not be handed a repainted one.
    """
    if size <= 0:
        raise SystemExit(f"BLOCKED: icon_field needs a positive size, got {size}")
    radians = math.radians(angle_deg)
    dx, dy = math.cos(radians), math.sin(radians)
    corners = [(0.0, 0.0), (float(size - 1), 0.0), (0.0, float(size - 1)),
               (float(size - 1), float(size - 1))]
    projections = [x * dx + y * dy for x, y in corners]
    low_projection, high_projection = min(projections), max(projections)
    span = max(1e-6, high_projection - low_projection)
    deltas = [float(h) - float(l) for l, h in zip(low, high)]
    rows = bytearray()
    for y in range(size):
        base = (y * dy - low_projection) / span
        for x in range(size):
            t = base + x * dx / span
            if t <= 0.0:
                rows += bytes(int(l) for l in low)
                continue
            if t >= 1.0:
                rows += bytes(int(h) for h in high)
                continue
            rows += bytes(int(l + d * t) for l, d in zip(low, deltas))
    image = Image.frombytes("RGB", (size, size), bytes(rows))
    if palette is not None:
        overlays = field_overlays(palette.keys())
        if overlays:
            image = _composite_field_overlays(image, palette, overlays)
    return image


# --------------------------------------------------------------------------------------
# Raster
# --------------------------------------------------------------------------------------


def _rgb(palette: Mapping[str, Iterable[int]], role: str) -> tuple[int, int, int]:
    if role not in palette:
        raise SystemExit(
            f"BLOCKED: the mark draws with palette role {role!r}, which the resolved palette "
            f"does not define (it has {', '.join(sorted(palette))})."
        )
    value = tuple(int(v) for v in palette[role])[:3]
    return (value[0], value[1], value[2])  # type: ignore[return-value]


def render_mark(canvas: int, mark: str | None = None,
                palette: Mapping[str, Iterable[int]] | None = None,
                supersample: int = SUPERSAMPLE) -> Image.Image:
    """The mark as a transparent RGBA layer at ``canvas`` px, antialiased by supersampling."""
    if palette is None:
        raise SystemExit("BLOCKED: render_mark needs a palette")
    assert_mark_on_grid(mark)
    scale = canvas * supersample
    layer = Image.new("RGBA", (scale, scale), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    for primitive in mark_primitives(mark):
        colour = _rgb(palette, primitive["role"]) + (255,)
        kind = primitive["kind"]
        if kind == "ring":
            cx, cy = primitive["center"]
            # Pillow insets an outline from its bounding box: `ellipse(bbox, width=w)` fills the
            # annulus between the bbox and the bbox shrunk by w. Passing the centreline radius
            # would therefore draw the wall *inside* the declared reach, which is a 12px error at
            # a bold stroke - measured before this was fixed: the raster ring's outer edge sat at
            # 134.96px while the vector's sat at 146.96px. Expanding the box by half the stroke
            # puts the outer edge exactly on the declared radius + weight/2, which is what the
            # vector path and `primitive_reach` both mean.
            weight = stroke_weight(primitive["weight"])
            outer = (primitive["radius"] + weight / 2) * scale
            width = round(weight * scale)
            draw.ellipse((cx * scale - outer, cy * scale - outer,
                          cx * scale + outer, cy * scale + outer), outline=colour, width=width)
        elif kind == "bar":
            cx, cy = primitive["center"]
            half_w, half_h = primitive["size"][0] * scale / 2, primitive["size"][1] * scale / 2
            draw.rectangle((cx * scale - half_w, cy * scale - half_h,
                            cx * scale + half_w, cy * scale + half_h), fill=colour)
        elif kind == "plate":
            cx, cy = primitive["center"]
            half_w, half_h = primitive["size"][0] * scale / 2, primitive["size"][1] * scale / 2
            corners = primitive["corners"]
            if corners == "all":
                # Pillow indexes `corners` (so the string "all" is an IndexError); it also wants a
                # 4-sequence, not the sentinel this module uses.
                corners = (True, True, True, True)
            draw.rounded_rectangle(
                (cx * scale - half_w, cy * scale - half_h,
                 cx * scale + half_w, cy * scale + half_h),
                radius=min(primitive["radius"] * scale, half_w, half_h), fill=colour,
                corners=corners)
        elif kind == "disc":
            cx, cy = primitive["center"]
            radius = primitive["radius"] * scale
            draw.ellipse((cx * scale - radius, cy * scale - radius,
                          cx * scale + radius, cy * scale + radius), fill=colour)
        elif kind == "stroke":
            half = stroke_weight(primitive["weight"]) * scale / 2
            for subpath in _stroke_outline(
                    [(x * scale, y * scale) for x, y in primitive["points"]], half):
                draw.polygon(subpath, fill=colour)
        elif kind == "polygon":
            draw.polygon([(x * scale, y * scale) for x, y in primitive["points"]],
                         fill=colour)
        else:
            raise SystemExit(f"BLOCKED: unknown icon primitive kind {kind!r}")
    if supersample > 1:
        layer = layer.resize((canvas, canvas), Image.Resampling.LANCZOS)
    return layer


# --------------------------------------------------------------------------------------
# Android vector path data
#
# The same primitives, in the 108dp adaptive viewport. The canvas fraction *is* the viewport
# fraction, so there is no second scale factor and the two renderers cannot disagree about how big
# the mark is. Every element is emitted as a *filled outline* rather than as `strokeWidth`, because
# `scripts/check-adaptive-icon.py` measures the mark's reach from the path data: a stroked path
# would be measured on its centreline and under-report the reach by half a stroke.
# --------------------------------------------------------------------------------------


def _round_rect_path(cx: float, cy: float, half_w: float, half_h: float, radius: float,
                     corners: object) -> str:
    """A rounded rectangle; `corners` is a 4-tuple (tl, tr, br, bl) or the string "all"."""
    if corners == "all":
        tl = tr = br = bl = True
    else:
        tl, tr, br, bl = corners  # type: ignore[misc]
    radius = min(radius, half_w, half_h)
    if radius <= 1e-9:
        # A square corner is not a zero-radius arc: an arc command with r=0 is degenerate and the
        # path samplers in `check-adaptive-icon.py` and this module's preview would both have to
        # special-case it.
        tl = tr = br = bl = False
    left, right = cx - half_w, cx + half_w
    top, bottom = cy - half_h, cy + half_h
    command = [f"M{left + (radius if tl else 0):.2f},{top:.2f}"]
    command.append(f"H{right - (radius if tr else 0):.2f}")
    if tr:
        command.append(f"A{radius:.2f},{radius:.2f} 0 0,1 {right:.2f},{top + radius:.2f}")
    command.append(f"V{bottom - (radius if br else 0):.2f}")
    if br:
        command.append(f"A{radius:.2f},{radius:.2f} 0 0,1 {right - radius:.2f},{bottom:.2f}")
    command.append(f"H{left + (radius if bl else 0):.2f}")
    if bl:
        command.append(f"A{radius:.2f},{radius:.2f} 0 0,1 {left:.2f},{bottom - radius:.2f}")
    command.append(f"V{top + (radius if tl else 0):.2f}")
    if tl:
        command.append(f"A{radius:.2f},{radius:.2f} 0 0,1 {left + radius:.2f},{top:.2f}")
    command.append("Z")
    return "".join(command)


def _circle_path(cx: float, cy: float, radius: float) -> str:
    return (f"M{cx - radius:.2f},{cy:.2f}"
            f"a{radius:.2f},{radius:.2f} 0 1,0 {radius * 2:.2f},0"
            f"a{radius:.2f},{radius:.2f} 0 1,0 {-radius * 2:.2f},0Z")


def _polygon_path_data(points: Sequence[tuple[float, float]]) -> str:
    """A closed polygon path, using compact axis-aligned commands where possible."""
    def coordinate(value: float) -> str:
        nearest = round(value)
        return str(nearest) if abs(value - nearest) < 1e-9 else f"{value:.12g}"

    x, y = points[0]
    command = [f"M{coordinate(x)},{coordinate(y)}"]
    for index, (next_x, next_y) in enumerate(points[1:]):
        if index == 0 or (next_x != x and next_y != y):
            command.append(f"L{coordinate(next_x)},{coordinate(next_y)}")
        elif next_x == x:
            command.append(f"V{coordinate(next_y)}")
        else:
            command.append(f"H{coordinate(next_x)}")
        x, y = next_x, next_y
    command.append("Z")
    return "".join(command)


def mark_layers(mark: str | None = None,
                viewport_dp: float = ADAPTIVE_VIEWPORT_DP) -> list[dict]:
    """The mark as Android vector path data, one entry per drawn path, in the viewport.

    Entries carry the palette ``role`` and the fill rule the shape needs. The annulus is the only
    even-odd shape: two concentric circles cut a bore. Everything else is a union under the default
    non-zero rule, which is what lets the stroked polyline be a set of quads plus vertex discs
    rather than an offset curve.
    """
    assert_mark_on_grid(mark)
    scale = viewport_dp
    centre = viewport_dp / 2
    anchor = optical_anchor(mark)

    def px(x: float) -> float:
        return centre + (x - anchor[0]) * scale

    def py(y: float) -> float:
        return centre + (y - anchor[1]) * scale

    layers: list[dict] = []
    for primitive in mark_primitives(mark):
        kind = primitive["kind"]
        if kind == "ring":
            radius = primitive["radius"] * scale
            width = stroke_weight(primitive["weight"]) * scale
            cx, cy = px(primitive["center"][0]), py(primitive["center"][1])
            layers.append({"role": primitive["role"], "fill_type": "evenOdd",
                           "data": _circle_path(cx, cy, radius + width / 2)
                           + _circle_path(cx, cy, radius - width / 2)})
        elif kind == "bar":
            cx, cy = px(primitive["center"][0]), py(primitive["center"][1])
            half_w, half_h = primitive["size"][0] * scale / 2, primitive["size"][1] * scale / 2
            layers.append({"role": primitive["role"], "fill_type": "nonZero",
                           "data": _round_rect_path(cx, cy, half_w, half_h, 0.0, "all")})
        elif kind == "plate":
            cx, cy = px(primitive["center"][0]), py(primitive["center"][1])
            half_w, half_h = primitive["size"][0] * scale / 2, primitive["size"][1] * scale / 2
            layers.append({"role": primitive["role"], "fill_type": "nonZero",
                           "data": _round_rect_path(cx, cy, half_w, half_h,
                                                    primitive["radius"] * scale,
                                                    primitive["corners"])})
        elif kind == "disc":
            layers.append({"role": primitive["role"], "fill_type": "nonZero",
                           "data": _circle_path(px(primitive["center"][0]),
                                                py(primitive["center"][1]),
                                                primitive["radius"] * scale)})
        elif kind == "stroke":
            half = stroke_weight(primitive["weight"]) * scale / 2
            points = [(px(x), py(y)) for x, y in primitive["points"]]
            data = "".join(
                "M" + "L".join(f"{x:.2f},{y:.2f}" for x, y in subpath) + "Z"
                for subpath in _stroke_outline(points, half)
            )
            layers.append({"role": primitive["role"], "fill_type": "nonZero", "data": data})
        elif kind == "polygon":
            points = [(px(x), py(y)) for x, y in primitive["points"]]
            layers.append({"role": primitive["role"], "fill_type": "nonZero",
                           "data": _polygon_path_data(points)})
        else:
            raise SystemExit(f"BLOCKED: unknown icon primitive kind {kind!r}")
    return layers
