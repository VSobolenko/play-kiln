"""Rasterise the shipped ic_launcher_foreground.xml and assert the mark survives every
adaptive-icon mask.

Android defines: 108dp viewport, central 72dp "visible" square, and a 66dp safe zone that no mask
may crop. A circular mask uses the full 72dp diameter, which is the worst case here because the
caliper arms reach furthest along the horizontal axis.

Reads the path data out of the actual file rather than retyping it, so the check cannot pass on a
stale copy of the geometry.
"""
from __future__ import annotations

import math
import re
from pathlib import Path

FOREGROUND = Path(
    "apps/2026-09-25-pipe-sizing/app/src/main/res/drawable/ic_launcher_foreground.xml"
)
VIEWPORT_DP = 108.0
VISIBLE_MASK_RADIUS_DP = 36.0  # 72dp diameter
SAFE_RADIUS_DP = 33.0  # 66dp diameter

TOKEN = re.compile(r"([MmLlHhVvAaZz])|(-?\d*\.?\d+)")


def points_from(path_data: str) -> list[tuple[float, float]]:
    """Flatten the subset of SVG path commands our generator emits."""
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
        if command in "Mm":
            x, y = float(tokens[i]), float(tokens[i + 1])
            start = (x, y)
            points.append((x, y))
            i += 2
            command = "L" if command == "M" else "l"
            continue
        if command in "Hh":
            x = float(token)
            points.append((x, y))
            i += 1
            continue
        if command in "Vv":
            y = float(token)
            points.append((x, y))
            i += 1
            continue
        if command in "Ll":
            x, y = float(tokens[i]), float(tokens[i + 1])
            points.append((x, y))
            i += 2
            continue
        if command in "Aa":
            rx, _ry, _rot, large, sweep, dx, dy = (float(t) for t in tokens[i : i + 7])
            i += 7
            x1, y1 = x, y
            x2, y2 = x + dx, y + dy
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
            for step in range(1, 65):
                angle = a1 + (a2 - a1) * step / 64
                points.append((cx + rx * math.cos(angle), cy + rx * math.sin(angle)))
            x, y = x2, y2
            continue
        i += 1
    return points


def main() -> int:
    if not FOREGROUND.is_file():
        raise SystemExit(f"BLOCKED: {FOREGROUND} does not exist")

    text = FOREGROUND.read_text(encoding="utf-8")
    path_data = re.findall(r'android:pathData="([^"]+)"', text)
    if not path_data:
        raise SystemExit(f"BLOCKED: no android:pathData found in {FOREGROUND}")

    points = [p for data in path_data for p in points_from(data)]
    if not points:
        raise SystemExit(f"BLOCKED: {FOREGROUND} declares paths but none rasterised")

    centre = VIEWPORT_DP / 2
    reach = max(math.hypot(px - centre, py - centre) for px, py in points)
    xs = [p[0] for p in points]
    ys = [p[1] for p in points]

    print(f"{FOREGROUND.name}: {len(path_data)} path element(s), {len(points)} sampled points")
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
    print(f"  PASS: mark sits {margin:.2f}dp inside the safe zone")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
