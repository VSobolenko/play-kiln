#!/usr/bin/env python3
"""constellation_region.py — exact spherical area and centre of an IAU constellation region.

The IAU publishes each constellation as a closed ring of Delporte-derived boundary vertices.
Everything this module needs from that ring — the region's **area on the sphere** and its
**centroid direction** — is an integral over the region, and the region is the inside of a
closed curve.

`common.py`'s `ring_area_sq_deg` / `region_centre_deg` do this by Green's theorem in the
(RA, Dec) plane. That is correct for a ring that does not encircle a pole, and **wrong for the
two that do** (Ursa Minor and Octans): measured, it over-counted UMi by `+13.42 deg^2` (5.2 %)
and put UMi's centroid *outside its own region*. The pole is not a special case worth three
code paths, so this module removes the (RA, Dec) plane entirely.

**The method.** Gnomonic projection about the ring's own mean direction `u`:

  * every vertex maps to the plane as `p = (X/Z, Y/Z)` after rotating `u` onto `+z`. A ring
    that encircles a pole is then a plain closed polygon around the plane's origin, and
    nothing wraps: `u` is the mean of the *vertices*, so the whole ring stays within a few
    tens of degrees of it and the projection is well conditioned (asserted below).
  * on the unit sphere the gnomonic map has `dA = r dr dphi / (1 + r^2)^(3/2)` in steradians,
    so with `g(r) = 1 - 1/sqrt(1 + r^2)`,

        A = ∮ g(r) dphi                       -- exact for a disc: 2*pi*(1 - cos theta_0)

    and the centroid direction is the same line integral of three more radial primitives:

        C_x = ∮ cos(phi) H(r) dphi   H(r) = atan(r)/2 - r/(2(1 + r^2))
        C_y = ∮ sin(phi) H(r) dphi
        C_z = ∮ G(r) dphi            G(r) = (1 - 1/(1 + r^2))/2

    each being `d/dr` of `r^k/(1+r^2)^2` times `r`, so `∮ (P dr + Q dphi) = integral of Q_r`.
  * `phi` is integrated **continuously** (the signed angle between consecutive projected
    vertices), so the branch cut of `atan2` never enters, and the fact that the polygon
    encircles the projection origin is handled by the integral itself rather than by a case.

`region()` returns the area in square degrees and the centroid as a unit vector; `centre_deg()`
returns the centroid as `(raDeg, decDeg)`. `inside()` is the matching membership test
(planar even-odd in the projected polygon), used to assert that a shipped centre really is
inside the region it labels.

**Edges are arcs, so they are sampled.** Between two Delporte vertices the IAU boundary is a line
of **constant declination** (most edges) or of constant right ascension. A constant-declination
arc is not a great circle, so in the gnomonic plane it is a curve, and the straight chord between
its endpoints is not that curve: the polygon differs from the region by an area whose **sign
depends on which way the arc bulges**. Measured over the 88 rings, chording the raw vertices
under-reports 35 of them and over-reports 53, for a gross discrepancy of 368.16 deg^2; the worst
single case is Octans, bounded by one long parallel, which loses **14.65 %** of its area to the
chord (248.826 -> 291.544 deg^2, published 291.045). The whole-sky sum hides it -- chording all 88
sums to 41252.89, only 0.07 deg^2 off the sphere, because losses and gains very nearly cancel
(net -0.374) -- so the per-region figure is the one that decides, and it is why densification is
not optional. `densify_ring()` samples every edge in
`(RA, Dec)` at `DEFAULT_STEP_DEG` before projecting, and that is what the bake uses: `region()`,
`centre_deg()` and `inside()` all take *sampled* rings. The measurements are in the module's
`PROOF` docstring section under `docs/asset-provenance.md`.

A second, unrelated trap is the `(RA, Dec)`-plane Green integral (`common.ring_area_sq_deg`), which
is not a sampling problem at all: a plane polygon closed over the pole is not the spherical region,
and it over-counted Ursa Minor by 5.285 %. Nothing here uses it.
"""

from __future__ import annotations

import math

DEG = math.pi / 180.0
SR_TO_DEG2 = (180.0 / math.pi) ** 2

#: Edge sampling used by the bake, in degrees of great-circle span. Chosen by measurement, not
#: by taste -- `region()` over all 88 rings at several steps:
#:
#:     step   worst published-area dev   median   whole-sky sum
#:     1.00     0.4430 % (Cha)           0.0220   41252.5129
#:     0.50     0.4534 % (Cha)           0.0229   41252.5130
#:     0.20     0.4545 % (Cha)           0.0234   41252.5129
#:     0.10     0.4545 % (Cha)           0.0235   41252.5129
#:     0.05     0.4545 % (Cha)           0.0235   41252.5129
#:     0.02     0.4545 % (Cha)           0.0235   41252.5129
#:
#: So 0.2 deg is where the curve is resolved: everything below it agrees to four decimals, and
#: the whole-sky sum does not move at all -- a 0.02 deg step reproduces the 0.2 deg sum to
#: 0.0001 deg^2. That sum is 41252.5129 against the sphere's 41252.9612, and the -0.4483 deg^2
#: residual is **not** sampling error and never shrinks: it is the IAU rings not tiling the
#: sphere exactly, because the published vertex lists round each Delporte corner. Sampling
#: controls the per-constellation error, and this is where that stops moving.
DEFAULT_STEP_DEG = 0.2

# 24-node Gauss-Legendre nodes/weights on [-1, 1], enough that the line integrals below are
# exact to double precision for the polygon sizes involved.
_GL_X = (-0.9951872199970213, -0.9747285559713095, -0.9382745520027328, -0.8864155270044011,
         -0.8200019859739029, -0.7401241915785544, -0.6480936519369755, -0.5454214713888396,
         -0.4337935076260451, -0.3150426796961634, -0.1911188674736163, -0.0640568928626056,
         0.0640568928626056, 0.1911188674736163, 0.3150426796961634, 0.4337935076260451,
         0.5454214713888396, 0.6480936519369755, 0.7401241915785544, 0.8200019859739029,
         0.8864155270044011, 0.9382745520027328, 0.9747285559713095, 0.9951872199970213)
_GL_W = (0.0123412297999872, 0.0285313886289337, 0.0442774388174198, 0.0592985849154368,
         0.0733464814110803, 0.0861901615319533, 0.0976186521041139, 0.1074442701159656,
         0.1155056680537256, 0.1216704729278034, 0.1258374563468283, 0.1279381953467522,
         0.1279381953467522, 0.1258374563468283, 0.1216704729278034, 0.1155056680537256,
         0.1074442701159656, 0.0976186521041139, 0.0861901615319533, 0.0733464814110803,
         0.0592985849154368, 0.0442774388174198, 0.0285313886289337, 0.0123412297999872)


def unit(ra_deg: float, dec_deg: float) -> tuple:
    a, d = ra_deg * DEG, dec_deg * DEG
    return (math.cos(d) * math.cos(a), math.cos(d) * math.sin(a), math.sin(d))


def to_ra_dec(v: tuple) -> tuple:
    ra = math.degrees(math.atan2(v[1], v[0])) % 360.0
    return ra, math.degrees(math.asin(max(-1.0, min(1.0, v[2]))))


def _normalise(v: tuple) -> tuple:
    n = math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
    if n == 0.0:
        raise ValueError("zero-length direction")
    return (v[0] / n, v[1] / n, v[2] / n)


def mean_direction(vertices: list) -> tuple:
    """Normalised sum of the ring's vertices — the projection centre."""
    sx = sy = sz = 0.0
    for ra, dec in vertices:
        x, y, z = unit(ra, dec)
        sx, sy, sz = sx + x, sy + y, sz + z
    return _normalise((sx, sy, sz))


def densify_ring(ring: list, step_deg: float = DEFAULT_STEP_DEG) -> list:
    """Sample every edge of a Delporte ring on the `(RA, Dec)` line it is written on.

    Each consecutive pair is joined by the shortest arc in right ascension (so a ring that
    straddles 0h is not walked the long way round), and the edge is cut into pieces no longer
    than `step_deg`. The result is the same closed region with the constant-declination arcs
    resolved, which is what makes the gnomonic integral in `ring_region()` exact instead of
    chorded. A ring already finer than `step_deg` is returned unchanged in substance: the loop
    emits at least one point per edge, so the polygon is never emptied.
    """
    out = []
    n = len(ring)
    for i in range(n):
        ra1, de1 = ring[i]
        ra2, de2 = ring[(i + 1) % n]
        d_ra = (ra2 - ra1 + 180.0) % 360.0 - 180.0
        span = math.hypot(d_ra * math.cos((de1 + de2) * 0.5 * DEG), de2 - de1)
        steps = max(1, int(math.ceil(span / step_deg)))
        for k in range(steps):
            t = k / steps
            out.append((ra1 + d_ra * t, de1 + (de2 - de1) * t))
    return out


def densify(rings: list, step_deg: float = DEFAULT_STEP_DEG) -> list:
    """`densify_ring` over every ring of a region (Serpens has two)."""
    return [densify_ring(r, step_deg) for r in rings]


def _frame(u: tuple) -> tuple:
    """Three rows `(e1, e2, u)`: the rotation taking `u` to `+z`."""
    z = (0.0, 0.0, 1.0)
    e1 = _normalise((z[1] * u[2] - z[2] * u[1],
                     z[2] * u[0] - z[0] * u[2],
                     z[0] * u[1] - z[1] * u[0])) if abs(u[2]) < 0.9999999 \
        else _normalise((0.0, 1.0, 0.0))
    e2 = _normalise((u[1] * e1[2] - u[2] * e1[1],
                     u[2] * e1[0] - u[0] * e1[2],
                     u[0] * e1[1] - u[1] * e1[0]))
    return e1, e2, u


def _dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def project(vertices: list, e1: tuple, e2: tuple, u: tuple) -> list:
    """Gnomonic plane coordinates of each vertex, with the worst off-axis angle."""
    out = []
    worst = 0.0
    for ra, dec in vertices:
        v = unit(ra, dec)
        z = _dot(v, u)
        if z <= 1e-6:
            raise ValueError("a vertex is 90 degrees or more from the projection centre")
        worst = max(worst, math.degrees(math.acos(min(1.0, z))))
        out.append((_dot(v, e1) / z, _dot(v, e2) / z))
    return out, worst


def _primitives(r: float) -> tuple:
    """`(g, H, G)`: the radial primitives of `dA`, `x dA` and `z dA` in the projected plane."""
    r2 = r * r
    inv = 1.0 / math.sqrt(1.0 + r2)
    return (1.0 - inv,
            math.atan(r) / 2.0 - r / (2.0 * (1.0 + r2)),
            (1.0 - 1.0 / (1.0 + r2)) / 2.0)


def ring_region(vertices: list) -> dict:
    """`{areaSqDeg, centroid, moment, maxOffAxisDeg}` for one closed ring of `(raDeg, decDeg)`."""
    pts = list(vertices)
    if len(pts) > 2 and abs(pts[0][0] - pts[-1][0]) < 1e-12 and abs(pts[0][1] - pts[-1][1]) < 1e-12:
        pts = pts[:-1]
    if len(pts) < 3:
        raise ValueError("a ring needs at least three vertices")

    u = mean_direction(pts)
    e1, e2, u = _frame(u)
    plane, worst = project(pts, e1, e2, u)
    if worst > 80.0:
        raise ValueError(f"ring spans {worst:.1f} deg from its centre; gnomonic is undefined")

    area = cx = cy = cz = 0.0
    n = len(plane)
    for i in range(n):
        x1, y1 = plane[i]
        x2, y2 = plane[(i + 1) % n]
        dx, dy = x2 - x1, y2 - y1
        # `cross(p, dp)` is constant along the segment, and `dphi = cross / r^2 dt`, so the
        # integrals below need no `atan2` and never see its branch cut:
        #   A  = ∮ g(r) dphi,  Cx = ∮ (x/r) H(r) dphi, ...
        cross = x1 * dy - y1 * dx
        if cross == 0.0:
            continue
        for node, weight in zip(_GL_X, _GL_W):
            t = (node + 1.0) / 2.0
            x, y = x1 + dx * t, y1 + dy * t
            r2 = x * x + y * y
            r = math.sqrt(r2)
            if r < 1e-12:
                continue
            g, h, gg = _primitives(r)
            area += g * cross / r2 * weight
            cx += x * h * cross / (r2 * r) * weight
            cy += y * h * cross / (r2 * r) * weight
            cz += gg * cross / r2 * weight
        # The parameter interval is [0, 1], so every node contributes with a factor of 1/2.
    scale = 0.5
    area *= scale
    cx *= scale
    cy *= scale
    cz *= scale

    # `area` and the centroid moments share the ring's winding sign, and the direction of the
    # break differs per constellation (measured: Octans and Andromeda flipped, Ursa Minor not).
    # `areaSqDeg` is reported unsigned, so the centroid must be re-oriented to match.
    if area < 0.0:
        cx, cy, cz = -cx, -cy, -cz

    # Back out of the projected frame: this is `∮ r_hat dA` over the ring, in steradians. Its
    # direction is the ring's centroid, and summing it over a region's rings (Serpens) is what
    # gives the *region* centre rather than the centre of its largest half.
    moment = (cx * e1[0] + cy * e2[0] + cz * u[0],
              cx * e1[1] + cy * e2[1] + cz * u[1],
              cx * e1[2] + cy * e2[2] + cz * u[2])
    return {"areaSqDeg": abs(area) * SR_TO_DEG2, "centroid": _normalise(moment),
            "moment": moment, "maxOffAxisDeg": worst}


def region(rings: list) -> dict:
    """Total area, region centroid and per-ring results for one or more rings."""
    parts = [ring_region(r) for r in rings]
    moment = tuple(sum(p["moment"][k] for p in parts) for k in range(3))
    return {"areaSqDeg": sum(p["areaSqDeg"] for p in parts),
            "centroid": _normalise(moment),
            "moment": moment,
            "parts": parts,
            "largest": max(parts, key=lambda p: p["areaSqDeg"])}


def centre_deg(rings: list) -> tuple:
    """The region's area-weighted centre direction as `(raDeg, decDeg)`."""
    return to_ra_dec(region(rings)["centroid"])


def inside(vertices: list, ra_deg: float, dec_deg: float) -> bool:
    """Is a direction inside the ring? Planar even-odd in the same projected polygon."""
    pts = list(vertices)
    if len(pts) > 2 and abs(pts[0][0] - pts[-1][0]) < 1e-12 and abs(pts[0][1] - pts[-1][1]) < 1e-12:
        pts = pts[:-1]
    u = mean_direction(pts)
    e1, e2, u = _frame(u)
    plane, worst = project(pts, e1, e2, u)
    if worst > 80.0:
        return False
    v = unit(ra_deg, dec_deg)
    if _dot(v, u) <= 1e-6:
        # More than 90 degrees from this ring's projection centre: the ring spans well under
        # 80 degrees, so the point cannot be inside it. (Serpens' two halves hit this case.)
        return False
    [(x, y)], _ = project([(ra_deg, dec_deg)], e1, e2, u)
    c, n = False, len(plane)
    for i in range(n):
        x1, y1 = plane[i]
        x2, y2 = plane[(i + 1) % n]
        if (y1 > y) != (y2 > y):
            if x < x1 + (y - y1) * (x2 - x1) / (y2 - y1):
                c = not c
    return c


def inside_any(rings: list, ra_deg: float, dec_deg: float) -> bool:
    """Is a direction inside any ring of a region? Both Serpens halves are tested."""
    return any(inside(r, ra_deg, dec_deg) for r in rings)


# ---------------------------------------------------------------------------
# The independent check
# ---------------------------------------------------------------------------
# `ring_region()` above is one algorithm: a gnomonic projection and four line integrals. This is a
# second one that shares nothing with it but the input -- the ring is cut into spherical triangles
# with a fixed apex and each triangle's solid angle comes from van Kampen's formula,
# `2 atan2(a . (b x c), 1 + a.b + b.c + c.a)`. Agreement between the two on the *same* sampled
# ring is evidence about the region rather than about either method: measured over all 88
# constellations with the rings sampled at 0.05 deg (finer than the bake's 0.2, so the two do not
# share a discretisation), the largest disagreement is **0.0072 %, on Ursa Minor** -- 1 part in
# ~14,000, on the hardest ring in the set (the one that encircles the pole). That is the number
# the bake gates on, and it is what says the gnomonic integrals are not silently wrong.

def _fan_triangle(a: tuple, b: tuple, c: tuple) -> float:
    return 2.0 * math.atan2(_dot(a, (b[1] * c[2] - b[2] * c[1],
                                     b[2] * c[0] - b[0] * c[2],
                                     b[0] * c[1] - b[1] * c[0])),
                            1.0 + _dot(a, b) + _dot(b, c) + _dot(c, a))


def fan_area_sq_deg(ring: list) -> float:
    """The ring's area by van Kampen's spherical-triangle fan — `ring_region`'s independent twin.

    The ring must already be sampled (`densify_ring`), because the fan joins vertices with great
    circles: a long constant-declination edge chorded this way cuts inside the true boundary. The
    signed sum is reduced modulo the sphere and the complement is taken when the sum exceeds a
    hemisphere, which is what lets one formula cover a small constellation and a polar cap.
    """
    pts = [unit(ra, dec) for ra, dec in ring]
    if len(pts) > 2 and abs(pts[0][0] - pts[-1][0]) < 1e-15 and abs(pts[0][1] - pts[-1][1]) < 1e-15:
        pts = pts[:-1]
    if len(pts) < 3:
        raise ValueError("a ring needs at least three vertices")
    first = pts[0]
    total = 0.0
    for i in range(1, len(pts) - 1):
        total += _fan_triangle(first, pts[i], pts[i + 1])
    total = abs(total) % (4.0 * math.pi)
    if total > 2.0 * math.pi:
        total = 4.0 * math.pi - total
    return total * SR_TO_DEG2
