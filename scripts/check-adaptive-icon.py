"""Rasterise the shipped ic_launcher_foreground.xml and assert the mark survives every
adaptive-icon mask.

Android defines: 108dp viewport, central 72dp "visible" square, and a 66dp safe zone that no mask
may crop. A circular mask uses the full 72dp diameter, which is the worst case here because the
caliper arms reach furthest along the horizontal axis.

Reads the path data out of the actual file rather than retyping it, so the check cannot pass on a
stale copy of the geometry.

Supported path subset: `M/m`, `L/l`, `H/h`, `V/v`, `A/a` and `Z/z`, with relative forms, repeated
coordinate shorthand (`M x,y x2,y2` implies lineto; `l 1,2 3,4` repeats) and implicit linetos.

Known limitations, stated rather than hidden:

- Arcs are measured as *circular* arcs: `ry` and `x-axis-rotation` are ignored. The generator only
  emits `a rx,rx 0 …`, so an arc with `rx != ry` is rejected loudly instead of being measured as if
  it were circular - a silently wrong reach is worse than a refused check.
- Only straight lines and circular arcs contribute sampled points; there are no beziers in the
  subset, so a curve command would be skipped rather than approximated.

Run `python scripts/check-adaptive-icon.py --selftest` to exercise the parser itself.
"""
from __future__ import annotations

import math
import re
import sys
import argparse
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FOREGROUND: Path | None = None  # Legacy verifier shim overrides this until its concurrent edit lands.
VIEWPORT_DP = 108.0
VISIBLE_MASK_RADIUS_DP = 36.0  # 72dp diameter
SAFE_RADIUS_DP = 33.0  # 66dp diameter

TOKEN = re.compile(r"([MmLlHhVvAaZz])|(-?\d*\.?\d+)")
ARC_RADIUS_TOLERANCE_DP = 1e-6
ARC_SAMPLES = 64
ANDROID_DRAWABLE = "{http://schemas.android.com/apk/res/android}drawable"


def find_slot(slug: str) -> Path:
    apps = ROOT / "apps"
    matches = sorted(
        path for path in apps.iterdir()
        if path.is_dir() and (path.name == slug or path.name.endswith(f"-{slug}"))
    )
    if len(matches) == 0:
        raise SystemExit(f"BLOCKED: no slot under apps/ matches '{slug}'")
    if len(matches) > 1:
        raise SystemExit(
            f"BLOCKED: slot '{slug}' is ambiguous: {[path.name for path in matches]}"
        )
    return matches[0]


def resolve_foreground(slot: Path) -> Path:
    """Resolve the shipped foreground reference and refuse uncertain resource selection."""
    res = slot / "app" / "src" / "main" / "res"
    launcher_files = sorted(
        path for directory in res.glob("mipmap*")
        for name in ("ic_launcher.xml", "ic_launcher_round.xml")
        if (path := directory / name).is_file()
    )
    if not launcher_files:
        raise SystemExit(f"BLOCKED: no adaptive ic_launcher XML found under {res}")

    references: dict[Path, str] = {}
    for launcher in launcher_files:
        try:
            root = ET.parse(launcher).getroot()
        except ET.ParseError as error:
            raise SystemExit(f"BLOCKED: cannot parse {launcher}: {error}") from error
        foreground = root.find("foreground")
        reference = foreground.get(ANDROID_DRAWABLE) if foreground is not None else None
        if not reference:
            raise SystemExit(f"BLOCKED: {launcher} has no adaptive-icon foreground reference")
        references[launcher] = reference

    distinct = sorted(set(references.values()))
    if len(distinct) != 1:
        detail = ", ".join(f"{path} -> {reference}" for path, reference in references.items())
        raise SystemExit(f"BLOCKED: adaptive-icon foreground references disagree: {detail}")
    reference = distinct[0]
    match = re.fullmatch(r"@(drawable|mipmap)/([A-Za-z0-9_]+)", reference)
    if not match:
        raise SystemExit(
            f"BLOCKED: unsupported adaptive-icon foreground reference {reference!r}; "
            "expected a local @drawable or @mipmap XML resource"
        )
    resource_type, resource_name = match.groups()
    candidates = sorted(
        path for directory in res.glob(f"{resource_type}*")
        if (path := directory / f"{resource_name}.xml").is_file()
    )
    if len(candidates) != 1:
        raise SystemExit(
            f"BLOCKED: {reference} resolved to {len(candidates)} files: "
            f"{[str(path) for path in candidates]}"
        )
    return candidates[0].resolve()


def _arc_points(
    x1: float, y1: float, x2: float, y2: float, rx: float, large: float, sweep: float
) -> list[tuple[float, float]]:
    """Sample a circular arc from (x1,y1) to (x2,y2). `rx` is also used as `ry`."""
    mx, my = (x1 + x2) / 2.0, (y1 + y2) / 2.0
    dxr, dyr = (x1 - x2) / 2.0, (y1 - y2) / 2.0
    chord = math.hypot(dxr, dyr)
    h = math.sqrt(max(0.0, rx * rx - chord * chord))
    if large == sweep:
        h = -h
    cx = mx + h * dyr / chord if chord else mx
    cy = my - h * dxr / chord if chord else my
    a1 = math.atan2(y1 - cy, x1 - cx)
    a2 = math.atan2(y2 - cy, x2 - cx)
    if sweep == 1 and a2 < a1:
        a2 += 2 * math.pi
    if sweep == 0 and a2 > a1:
        a2 -= 2 * math.pi
    return [
        (cx + rx * math.cos(a1 + (a2 - a1) * step / ARC_SAMPLES),
         cy + rx * math.sin(a1 + (a2 - a1) * step / ARC_SAMPLES))
        for step in range(1, ARC_SAMPLES + 1)
    ]


def points_from(path_data: str) -> list[tuple[float, float]]:
    """Flatten the subset of SVG path commands our generator emits.

    Relative commands (`m`, `l`, `h`, `v`, `a`) are resolved against the current point. Treating
    them as absolute - the original defect - returned a plausible-looking point list measured from
    the origin, so a relative-path generator change would have kept this check reporting PASS while
    the shipped mark was clipped.
    """
    tokens: list[str | float] = []
    for command, number in TOKEN.findall(path_data):
        tokens.append(command if command else float(number))

    points: list[tuple[float, float]] = []
    x = y = 0.0
    start = (0.0, 0.0)
    command = ""
    i = 0
    while i < len(tokens):
        token = tokens[i]
        if isinstance(token, str):
            command = token
            i += 1
            if command in "Zz":
                x, y = start
            continue
        relative = command.islower()
        kind = command.upper()
        if kind == "M":
            nx, ny = float(tokens[i]), float(tokens[i + 1])
            x, y = (x + nx, y + ny) if relative else (nx, ny)
            start = (x, y)
            points.append((x, y))
            i += 2
            # Extra coordinate pairs after a moveto are implicit linetos, relative to the same
            # command's case (`M x,y x2,y2` is `M` then `L`; `m x,y x2,y2` is `m` then `l`).
            command = "l" if relative else "L"
            continue
        if kind == "L":
            nx, ny = float(tokens[i]), float(tokens[i + 1])
            x, y = (x + nx, y + ny) if relative else (nx, ny)
            points.append((x, y))
            i += 2
            continue
        if kind == "H":
            nx = float(tokens[i])
            x = x + nx if relative else nx
            points.append((x, y))
            i += 1
            continue
        if kind == "V":
            ny = float(tokens[i])
            y = y + ny if relative else ny
            points.append((x, y))
            i += 1
            continue
        if kind == "A":
            rx, ry, _rotation, large, sweep, ax, ay = (float(t) for t in tokens[i : i + 7])
            i += 7
            if abs(rx - ry) > ARC_RADIUS_TOLERANCE_DP:
                raise ValueError(
                    f"elliptical arc (rx={rx}, ry={ry}) is outside this checker's subset: it "
                    "treats ry and x-axis-rotation as ignored, so it cannot measure this arc. "
                    "Refusing to report a reach rather than reporting a wrong one."
                )
            x2, y2 = (x + ax, y + ay) if relative else (ax, ay)
            points.extend(_arc_points(x, y, x2, y2, rx, large, sweep))
            x, y = x2, y2
            continue
        # An unknown command token with numeric operands: skip the number.
        i += 1
    return points


def furthest_reach(points: list[tuple[float, float]], centre: float) -> float:
    """How far the sampled mark reaches from the viewport centre, in dp."""
    return max(math.hypot(px - centre, py - centre) for px, py in points)


def _selftest() -> int:
    """Prove the parser's relative and repeated-coordinate semantics. Returns a process exit code."""
    cases: list[tuple[str, str, list[tuple[float, float]]]] = [
        ("relative l", "M50,50l10,0l0,10", [(50.0, 50.0), (60.0, 50.0), (60.0, 60.0)]),
        ("relative m then l", "M50,50m10,0l0,10", [(50.0, 50.0), (60.0, 50.0), (60.0, 60.0)]),
        ("relative h and v", "M10,10h5v5", [(10.0, 10.0), (15.0, 10.0), (15.0, 15.0)]),
        ("repeated moveto pairs", "M0,0 10,10 20,20", [(0.0, 0.0), (10.0, 10.0), (20.0, 20.0)]),
        ("repeated relative lineto", "M0,0l1,2 3,4", [(0.0, 0.0), (1.0, 2.0), (4.0, 6.0)]),
        ("absolute vs relative reach", "M50,50L60,50L60,60", []),
    ]
    passed = 0
    for label, path, expected in cases:
        if label == "absolute vs relative reach":
            # The same shape written the two ways must measure the same reach: this is the
            # property the old absolute-only parser broke.
            reach_absolute = furthest_reach(points_from("M50,50L60,50L60,60"), 54.0)
            reach_relative = furthest_reach(points_from("M50,50l10,0l0,10"), 54.0)
            ok = math.isclose(reach_absolute, reach_relative, abs_tol=1e-9)
            detail = f"absolute {reach_absolute:.4f}dp vs relative {reach_relative:.4f}dp"
        else:
            actual = points_from(path)
            ok = actual == expected
            detail = f"{actual} vs {expected}"
        passed += 1 if ok else 0
        print(f"  {'PASS' if ok else 'FAIL'} selftest - {label}: {detail}")
    # Prove the elliptical-arc guard can actually fire, so the documented limitation is enforced
    # rather than merely written down.
    try:
        points_from("M0,0a10,5 0 0,1 10,0")
    except ValueError:
        rejected = True
    else:
        rejected = False
    passed += 1 if rejected else 0
    print(f"  {'PASS' if rejected else 'FAIL'} selftest - elliptical arc refused: "
          f"{'rx != ry raised' if rejected else 'measured as circular (bug)'}")
    total = len(cases) + 1
    print(f"SELFTEST: {passed}/{total} passed")
    return 0 if passed == total else 1


def main(foreground: Path | None = None) -> int:
    foreground = foreground or FOREGROUND
    if foreground is None:
        raise SystemExit("BLOCKED: no adaptive-icon foreground was selected")
    foreground = foreground.resolve()
    if not foreground.is_file():
        raise SystemExit(f"BLOCKED: {foreground} does not exist")

    print(f"resolved foreground: {foreground}")
    text = foreground.read_text(encoding="utf-8")
    path_data = re.findall(r'android:pathData="([^"]+)"', text)
    if not path_data:
        raise SystemExit(f"BLOCKED: no android:pathData found in {foreground}")

    points = [p for data in path_data for p in points_from(data)]
    if not points:
        raise SystemExit(f"BLOCKED: {foreground} declares paths but none rasterised")

    centre = VIEWPORT_DP / 2
    reach = furthest_reach(points, centre)
    xs = [p[0] for p in points]
    ys = [p[1] for p in points]

    print(f"{foreground.name}: {len(path_data)} path element(s), {len(points)} sampled points")
    print(f"  mark bounding box : x=[{min(xs):.2f}, {max(xs):.2f}]  y=[{min(ys):.2f}, {max(ys):.2f}] dp")
    print(f"  furthest reach    : {reach:.2f} dp from centre")
    print(f"  72dp visible mask : radius {VISIBLE_MASK_RADIUS_DP:.1f}dp -> {'OK' if reach <= VISIBLE_MASK_RADIUS_DP else 'CLIPPED'}")
    print(f"  66dp safe zone    : radius {SAFE_RADIUS_DP:.1f}dp -> {'OK' if reach <= SAFE_RADIUS_DP else 'OUTSIDE'}")

    problems = []
    if reach > SAFE_RADIUS_DP:
        problems.append(
            f"the mark reaches {reach:.2f}dp, outside the {SAFE_RADIUS_DP:.0f}dp safe zone; "
            "a launcher mask will crop it"
        )
    if reach > VISIBLE_MASK_RADIUS_DP:
        problems.append(
            f"the mark reaches {reach:.2f}dp, outside the {VISIBLE_MASK_RADIUS_DP:.0f}dp "
            "visible mask; the tips will be cut off in circular masks"
        )
    if problems:
        for problem in problems:
            print(f"  BLOCKED: {problem}")
        return 1

    margin = SAFE_RADIUS_DP - reach
    print(f"  PASS: {foreground} - mark sits {margin:.2f}dp inside the safe zone")
    return 0


if __name__ == "__main__":
    if "--selftest" in sys.argv[1:]:
        raise SystemExit(_selftest())
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--slot", required=True)
    options = parser.parse_args()
    raise SystemExit(main(resolve_foreground(find_slot(options.slot))))
