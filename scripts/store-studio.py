#!/usr/bin/env python3
"""PlayKiln store studio - the composed visual identity for a slot's store presence.

The generic `store-kit.py` renderer draws a gradient with the device screen pasted into a
rounded rectangle. That is honest but plain: it is a screencap with a caption, and a store
listing made of screencaps does not attract anyone. This module renders the assets the way a
modern Play listing does it - a branded composition with a benefit-led headline, a real device
mock-up, and a mark that survives being shrunk to a 48dp launcher icon.

It is deterministic and re-runnable, and it reads only from the slot: the raw captures under
`store/captures/raw/`, the shot copy in `store/journey.json`, and the palette in
`store/studio.json`. Nothing here invents UI - the screen inside every mock-up is the capture the
app actually produced.

    python scripts/store-studio.py --slot pipe-sizing
    python scripts/store-studio.py --slot pipe-sizing --preview-circle
"""

from __future__ import annotations

import argparse
import json
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

# --------------------------------------------------------------------------------------
# Design tokens
#
# One deliberate palette, used by every asset so the icon, the feature graphic and the
# screenshots read as one product: an ink-navy field (the engineer's drawing board), one warm
# amber (gas, and the single high-energy accent), and one cool cyan (measurement, depth).
# --------------------------------------------------------------------------------------

INK = (7, 16, 30)
INK_SOFT = (15, 36, 64)
INK_LIFT = (26, 62, 102)
AMBER = (255, 170, 40)
AMBER_DEEP = (238, 118, 18)
AMBER_PALE = (255, 206, 128)
CYAN = (88, 198, 246)
CYAN_DEEP = (24, 116, 176)
PAPER = (248, 250, 252)
MUTED = (170, 188, 210)
BEZEL = (13, 18, 27)
BEZEL_RIM = (126, 148, 176)

SCREEN_TARGETS = {
    "phone": (6, (1080, 1920)),
    "tablet-7": (4, (1200, 1920)),
    "tablet-10": (4, (1600, 2560)),
}

FONT_CANDIDATES = (
    "C:/Windows/Fonts/segoeuib.ttf",
    "C:/Windows/Fonts/arialbd.ttf",
    "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "DejaVuSans-Bold.ttf",
)
FONT_CANDIDATES_REGULAR = (
    "C:/Windows/Fonts/segoeui.ttf",
    "C:/Windows/Fonts/arial.ttf",
    "/System/Library/Fonts/Supplemental/Arial.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "DejaVuSans.ttf",
)

_FONT_STATE: dict[str, bool] = {"warned": False}


def font(size: int, *, bold: bool = False) -> ImageFont.ImageFont:
    candidates = FONT_CANDIDATES if bold else FONT_CANDIDATES_REGULAR
    for candidate in candidates:
        try:
            return ImageFont.truetype(candidate, size=size)
        except OSError:
            continue
    if not _FONT_STATE["warned"]:
        print("WARN studio - no TrueType font found; falling back to the Pillow default face")
        _FONT_STATE["warned"] = True
    return ImageFont.load_default(size=max(10, min(size, 32)))


def mix(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    t = max(0.0, min(1.0, t))
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))  # type: ignore[return-value]


# --------------------------------------------------------------------------------------
# Paint helpers
# --------------------------------------------------------------------------------------


def diagonal_gradient(
    size: tuple[int, int],
    start: tuple[int, int, int],
    end: tuple[int, int, int],
    angle_deg: float = 118.0,
) -> Image.Image:
    """A smooth linear gradient along `angle_deg`.

    Computed on a small grid and scaled up: a per-pixel loop over a 1080x1920 canvas costs
    seconds in pure Python, and a gradient has no high-frequency detail to lose.
    """
    grid = 96
    small = Image.new("RGB", (grid, grid))
    pixels = small.load()
    radians = math.radians(angle_deg)
    dx, dy = math.cos(radians), math.sin(radians)
    corners = [(0, 0), (grid - 1, 0), (0, grid - 1), (grid - 1, grid - 1)]
    projections = [x * dx + y * dy for x, y in corners]
    low, high = min(projections), max(projections)
    span = max(1e-6, high - low)
    for y in range(grid):
        for x in range(grid):
            pixels[x, y] = mix(start, end, ((x * dx + y * dy) - low) / span)
    return small.resize(size, Image.Resampling.BICUBIC)


def glow(
    size: tuple[int, int],
    center: tuple[float, float],
    radius: float,
    color: tuple[int, int, int],
    peak_alpha: int,
) -> Image.Image:
    """A soft radial bloom, built small and scaled up, then blurred."""
    steps = 72
    small = Image.new("L", (96, 96), 0)
    draw = ImageDraw.Draw(small)
    for step in range(steps, 0, -1):
        fraction = step / steps
        alpha = round(peak_alpha * (1.0 - fraction) ** 2.1)
        if alpha <= 0:
            continue
        extent = 48 * fraction
        draw.ellipse((48 - extent, 48 - extent, 48 + extent, 48 + extent), fill=alpha)
    small = small.filter(ImageFilter.GaussianBlur(6))
    diameter = max(2, round(radius * 2))
    mask = small.resize((diameter, diameter), Image.Resampling.LANCZOS)
    layer = Image.new("RGBA", size, (0, 0, 0, 0))
    layer.paste(Image.new("RGBA", (diameter, diameter), color + (255,)),
                (round(center[0] - radius), round(center[1] - radius)), mask)
    return layer


def blueprint_grid(size: tuple[int, int], spacing: int, alpha: int) -> Image.Image:
    """The faint drafting grid that carries the 'engineering instrument' read."""
    layer = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    for x in range(0, size[0], spacing):
        draw.line((x, 0, x, size[1]), fill=(150, 190, 230, alpha), width=1)
    for y in range(0, size[1], spacing):
        draw.line((0, y, size[0], y), fill=(150, 190, 230, alpha), width=1)
    return layer


def spaced_text(
    draw: ImageDraw.ImageDraw,
    xy: tuple[int, int],
    text: str,
    face: ImageFont.ImageFont,
    fill: tuple[int, int, int],
    tracking: int,
) -> int:
    """Draw text with letter spacing (Pillow has no tracking) and return the end x."""
    x, y = xy
    for character in text:
        draw.text((x, y), character, font=face, fill=fill)
        x += draw.textlength(character, font=face) + tracking
    return x


def wrap(draw: ImageDraw.ImageDraw, text: str, face: ImageFont.ImageFont, width: int) -> list[str]:
    words = text.split()
    if not words:
        return [""]
    lines: list[str] = []
    current = words[0]
    for word in words[1:]:
        trial = f"{current} {word}"
        if draw.textlength(trial, font=face) <= width:
            current = trial
        else:
            lines.append(current)
            current = word
    lines.append(current)
    return lines


# --------------------------------------------------------------------------------------
# The mark
#
# A pipe cross-section with a caliper across it: the annulus says "pipe", the dimension line
# says "sizing". It is built from three primitives so the same geometry can drive the raster
# icon and the adaptive-icon vector without the two drifting apart.
# --------------------------------------------------------------------------------------


# The mark is defined once, in fractions of its own half-extent, and each output scales that shape
# to its own extent: the raster icon uses the full canvas, while the adaptive-icon vectors use the
# 66dp safe zone, because a launcher mask crops outside it. Sharing the fractions keeps the *shape*
# identical, which is the part that must not drift.
#
# The scale differs by design and that is not drift: an exact match would put the raster mark's
# half-extent at 0.358 * 108 = 38.7dp, well outside the 33dp safe radius, so the caliper tips would
# be clipped by a circular mask. `adaptive_mark_half_extent_dp()` enforces the fit so a future
# geometry edit cannot silently reintroduce that.
MARK_HALF_EXTENT_FRACTION = 0.358  # of the raster canvas; set by the caliper arrow tips
MARK_SHAPE = {
    "ring_radius": 0.205 / MARK_HALF_EXTENT_FRACTION,
    "ring_width": 0.108 / MARK_HALF_EXTENT_FRACTION,
    "span": 0.335 / MARK_HALF_EXTENT_FRACTION,
    "line_width": 0.023 / MARK_HALF_EXTENT_FRACTION,
    "tick_half": 0.082 / MARK_HALF_EXTENT_FRACTION,
}
ADAPTIVE_VIEWPORT_DP = 108.0
ADAPTIVE_SAFE_RADIUS_DP = 33.0
# Deliberately inside the safe radius: the caliper tips sit at the mark's half-extent, and some
# launcher masks follow the safe circle closely, so a tangent mark would be shaved.
MARK_FIT_DP = 30.0


def mark_geometry(canvas: int) -> dict[str, float]:
    """Mark geometry for a canvas, scaled to its full extent."""
    unit = canvas * MARK_HALF_EXTENT_FRACTION
    return {"center": 0.5 * canvas, **{name: value * unit for name, value in MARK_SHAPE.items()}}


def adaptive_mark_half_extent_dp() -> float:
    """How far the mark reaches from its centre, in adaptive-viewport dp."""
    return (MARK_SHAPE["span"] + MARK_SHAPE["line_width"]) * MARK_FIT_DP


def assert_adaptive_mark_fits() -> None:
    """A mark that reaches outside the safe radius loses its tips to a circular mask."""
    reach = adaptive_mark_half_extent_dp()
    if reach > ADAPTIVE_SAFE_RADIUS_DP:
        raise SystemExit(
            f"BLOCKED: the adaptive icon mark reaches {reach:.1f}dp from centre, outside the "
            f"{ADAPTIVE_SAFE_RADIUS_DP:.0f}dp safe radius; a circular launcher mask would clip the "
            "caliper tips. Reduce MARK_FIT_DP or the shape fractions."
        )


def draw_mark(draw: ImageDraw.ImageDraw, canvas: int, *, ring: tuple[int, int, int],
              ink: tuple[int, int, int]) -> None:
    geometry = mark_geometry(canvas)
    center = geometry["center"]
    radius = geometry["ring_radius"]
    width = geometry["ring_width"]

    # The annulus. Drawn as a stroked ellipse so the stroke straddles `radius`.
    draw.ellipse(
        (center - radius, center - radius, center + radius, center + radius),
        outline=ring,
        width=round(width),
    )
    # A single sheen arc on the upper left reads as a cylindrical surface.
    sheen_radius = radius - width * 0.18
    draw.arc(
        (center - sheen_radius, center - sheen_radius, center + sheen_radius, center + sheen_radius),
        start=182, end=268, fill=AMBER_PALE, width=max(2, round(width * 0.16)),
    )

    # The caliper: a dimension line through the bore with end ticks and outward arrowheads.
    span = geometry["span"]
    line_width = round(geometry["line_width"])
    tick_half = geometry["tick_half"]
    draw.line((center - span, center, center + span, center), fill=ink, width=line_width)
    for direction in (-1, 1):
        edge = center + direction * radius - direction * width / 2
        draw.line((edge, center - tick_half, edge, center + tick_half), fill=ink, width=line_width)
        tip = center + direction * (span + line_width)
        base = center + direction * (span - line_width * 2.6)
        half = tick_half * 0.72
        draw.polygon(
            [(tip, center), (base, center - half), (base, center + half)],
            fill=ink,
        )


def render_icon(listing: dict, studio: dict) -> Image.Image:
    """512x512, full bleed and opaque: Play rejects a masked or transparent icon."""
    canvas = 512
    image = diagonal_gradient((canvas, canvas), INK, INK_LIFT, 122).convert("RGBA")
    image = Image.alpha_composite(image, glow((canvas, canvas), (canvas * 0.74, canvas * 0.20),
                                              canvas * 0.72, CYAN_DEEP, 96))
    image = Image.alpha_composite(image, glow((canvas, canvas), (canvas * 0.22, canvas * 0.88),
                                              canvas * 0.78, AMBER_DEEP, 88))
    image = Image.alpha_composite(image, blueprint_grid((canvas, canvas), 32, 12))
    draw = ImageDraw.Draw(image)
    draw_mark(draw, canvas, ring=AMBER, ink=PAPER)
    return image


def render_feature(listing: dict, studio: dict, source: Image.Image | None) -> Image.Image:
    """1024x500. No important element inside the outer 10% - Play crops there."""
    width, height = 1024, 500
    image = diagonal_gradient((width, height), INK, INK_SOFT, 104).convert("RGBA")
    image = Image.alpha_composite(image, glow((width, height), (width * 0.80, height * 0.42),
                                              width * 0.55, CYAN_DEEP, 84))
    image = Image.alpha_composite(image, glow((width, height), (width * 0.05, height * 0.95),
                                              width * 0.40, AMBER_DEEP, 70))
    image = Image.alpha_composite(image, blueprint_grid((width, height), 46, 11))

    # A real screen, not a drawing: the mock-up is the app's own capture.
    device = None
    if source is not None:
        device = device_frame(source, 388, radius=40, bezel=13)
        device_x = width - 66 - device.width
        device_y = height - device.height - 18
        shadow = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        ImageDraw.Draw(shadow).rounded_rectangle(
            (device_x + 12, device_y + 18, device_x + device.width + 12, device_y + device.height + 18),
            radius=44, fill=(0, 0, 0, 130))
        image = Image.alpha_composite(image, shadow.filter(ImageFilter.GaussianBlur(26)))
        image.alpha_composite(device, (device_x, device_y))

    draw = ImageDraw.Draw(image)
    left = 76
    text_width = (device_x - 40 - left) if device else (width - 2 * left)
    icon = render_icon(listing, studio)
    image.alpha_composite(icon.resize((92, 92), Image.Resampling.LANCZOS).convert("RGBA"), (left, 74))

    name = listing.get("appName", "PlayKiln")
    name_face = font(46, bold=True)
    y = 196
    for line in wrap(draw, name, name_face, text_width)[:2]:
        draw.text((left + 2, y), line, font=name_face, fill=PAPER)
        y += 54

    tagline = studio.get("featureTagline") or listing["locales"]["en-US"]["shortDescription"]
    body_face = font(24)
    y += 6
    for line in wrap(draw, tagline, body_face, text_width)[:2]:
        draw.text((left + 2, y), line, font=body_face, fill=MUTED)
        y += 34

    # A small honest positioning pill: both claims are true of this app.
    pill_face = font(17, bold=True)
    pill_text = studio.get("featurePill", "OFFLINE  ·  NO CODE TABLES")
    pill_width = round(draw.textlength(pill_text, font=pill_face)) + 34
    pill_y = y + 14
    draw.rounded_rectangle((left, pill_y, left + pill_width, pill_y + 38), radius=19,
                           fill=(AMBER[0], AMBER[1], AMBER[2], 40), outline=AMBER, width=2)
    draw.text((left + 17, pill_y + 10), pill_text, font=pill_face, fill=AMBER_PALE)
    return image.convert("RGB")


def device_frame(source: Image.Image, outer_height: int, *, radius: int, bezel: int) -> Image.Image:
    """Wrap a capture in a bezel, keeping the capture's own aspect ratio exactly."""
    aspect = source.width / source.height
    inner_height = outer_height - 2 * bezel
    inner_width = round(inner_height * aspect)
    outer_width = inner_width + 2 * bezel
    shot = source.convert("RGB").resize((inner_width, inner_height), Image.Resampling.LANCZOS)

    device = Image.new("RGBA", (outer_width, outer_height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(device)
    draw.rounded_rectangle((0, 0, outer_width - 1, outer_height - 1), radius=radius, fill=BEZEL)
    draw.rounded_rectangle((1, 1, outer_width - 2, outer_height - 2), radius=radius - 1,
                           outline=BEZEL_RIM + (110,), width=2)
    mask = Image.new("L", (inner_width, inner_height), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, inner_width - 1, inner_height - 1),
                                           radius=max(4, radius - bezel), fill=255)
    device.paste(shot, (bezel, bezel), mask)
    return device


def render_screenshot(size: tuple[int, int], shot: dict, source: Image.Image) -> Image.Image:
    """A composed marketing image: branded field, benefit-led headline, real screen."""
    width, height = size
    scale = height / 1920
    image = diagonal_gradient(size, INK, INK_SOFT, 116).convert("RGBA")
    image = Image.alpha_composite(image, glow(size, (width * 0.5, height * 0.80),
                                              max(width, height) * 0.58, CYAN_DEEP, 74))
    image = Image.alpha_composite(image, glow(size, (width * 0.06, height * 0.06),
                                              max(width, height) * 0.36, AMBER_DEEP, 58))
    image = Image.alpha_composite(image, blueprint_grid(size, round(58 * scale), 12))

    draw = ImageDraw.Draw(image)
    margin = round(84 * scale)
    cursor = round(96 * scale)

    kicker_face = font(round(27 * scale), bold=True)
    if shot.get("kicker"):
        draw.rectangle((margin, cursor + round(5 * scale), margin + round(26 * scale),
                        cursor + round(9 * scale)), fill=AMBER)
        spaced_text(draw, (margin + round(42 * scale), cursor), shot["kicker"].upper(),
                    kicker_face, AMBER, round(5 * scale))
        cursor += round(58 * scale)

    headline_face = font(round(72 * scale), bold=True)
    for line in wrap(draw, shot.get("headline", ""), headline_face, width - 2 * margin)[:2]:
        draw.text((margin, cursor), line, font=headline_face, fill=PAPER)
        cursor += round(86 * scale)

    body_face = font(round(32 * scale))
    cursor += round(10 * scale)
    for line in wrap(draw, shot.get("body", ""), body_face, width - 2 * margin)[:2]:
        draw.text((margin, cursor), line, font=body_face, fill=MUTED)
        cursor += round(45 * scale)

    bezel = max(10, round(17 * scale))
    radius = round(52 * scale)
    available = height - cursor - round(52 * scale) - round(56 * scale)
    outer_width = 0
    outer_height = available
    aspect = source.width / source.height
    if (available - 2 * bezel) * aspect + 2 * bezel > width - 2 * margin:
        # Tall, narrow targets (tablets) can leave the device too wide; fit it to the margins.
        outer_width = width - 2 * margin
        outer_height = round((outer_width - 2 * bezel) / aspect) + 2 * bezel
    device = device_frame(source, outer_height, radius=radius, bezel=bezel)
    x = (width - device.width) // 2
    y = cursor + round(52 * scale)

    shadow = Image.new("RGBA", size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        (x + round(10 * scale), y + round(16 * scale),
         x + device.width + round(10 * scale), y + device.height + round(16 * scale)),
        radius=radius, fill=(0, 0, 0, 140))
    image = Image.alpha_composite(image, shadow.filter(ImageFilter.GaussianBlur(round(24 * scale))))
    image.alpha_composite(device, (x, y))
    return image.convert("RGB")


# --------------------------------------------------------------------------------------
# Adaptive-icon vectors
#
# Written from `mark_geometry` so the vector cannot drift from the raster icon. The viewport is
# the 108dp adaptive canvas and the mark is held inside the 66dp safe zone.
# --------------------------------------------------------------------------------------


def _ring_path(center: float, outer: float, inner: float) -> str:
    return (
        f"M{center - outer:.2f},{center:.2f}a{outer:.2f},{outer:.2f} 0 1,0 {outer * 2:.2f},0"
        f"a{outer:.2f},{outer:.2f} 0 1,0 {-outer * 2:.2f},0Z"
        f"M{center - inner:.2f},{center:.2f}a{inner:.2f},{inner:.2f} 0 1,1 {inner * 2:.2f},0"
        f"a{inner:.2f},{inner:.2f} 0 1,1 {-inner * 2:.2f},0Z"
    )


def lasercut_paths() -> dict[str, str]:
    """Mark paths in the 108dp adaptive viewport, fitting the 66dp safe zone.

    Derived from the same `MARK_SHAPE` fractions as the raster icon, so the outline is the same
    shape; only the scale differs, and `assert_adaptive_mark_fits` bounds it.
    """
    center = ADAPTIVE_VIEWPORT_DP / 2
    unit = MARK_FIT_DP
    ring_radius = MARK_SHAPE["ring_radius"] * unit
    ring_width = MARK_SHAPE["ring_width"] * unit
    ring_outer = ring_radius + ring_width / 2
    ring_inner = ring_radius - ring_width / 2
    span = MARK_SHAPE["span"] * unit
    line = MARK_SHAPE["line_width"] * unit
    tick = MARK_SHAPE["tick_half"] * unit
    arrow = 7.0
    paths = {
        "ring": _ring_path(center, ring_outer, ring_inner),
        "line": (f"M{center - span:.2f},{center - line:.2f}H{center + span:.2f}"
                 f"V{center + line:.2f}H{center - span:.2f}Z"),
    }
    ticks = []
    for direction in (-1, 1):
        edge = center + direction * ring_radius
        ticks.append(f"M{edge - line:.2f},{center - tick:.2f}H{edge + line:.2f}"
                     f"V{center + tick:.2f}H{edge - line:.2f}Z")
    paths["ticks"] = "".join(ticks)
    arrows = []
    for direction in (-1, 1):
        tip = center + direction * (span + line)
        base = center + direction * (span - line * 2.6)
        half = tick * 0.72
        arrows.append(f"M{tip:.2f},{center:.2f}L{base:.2f},{center - half:.2f}"
                      f"L{base:.2f},{center + half:.2f}Z")
    paths["arrows"] = "".join(arrows)
    _ = arrow
    return paths


def write_vectors(res: Path, colors: dict[str, str]) -> list[Path]:
    assert_adaptive_mark_fits()
    paths = lasercut_paths()
    background = f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by scripts/store-studio.py. Do not hand-edit: re-run the studio. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:angle="304"
                android:endColor="{colors['ink_lift']}"
                android:startColor="{colors['ink']}"
                android:type="linear" />
        </aapt:attr>
    </path>
    <path
        android:fillAlpha="0.16"
        android:fillColor="#FFFFFFFF"
        android:pathData="M0,92L58,34L86,62L108,40V108H0Z" />
</vector>
"""
    foreground = f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by scripts/store-studio.py. Do not hand-edit: re-run the studio. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="{colors['amber']}"
        android:fillType="evenOdd"
        android:pathData="{paths['ring']}" />
    <path
        android:fillColor="{colors['paper']}"
        android:pathData="{paths['line']}{paths['ticks']}{paths['arrows']}" />
</vector>
"""
    monochrome = f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by scripts/store-studio.py. Do not hand-edit: re-run the studio. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFFFF"
        android:fillType="evenOdd"
        android:pathData="{paths['ring']}" />
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="{paths['line']}{paths['ticks']}{paths['arrows']}" />
</vector>
"""
    written = []
    drawable = res / "drawable"
    drawable.mkdir(parents=True, exist_ok=True)
    for name, content in (
        ("ic_launcher_background.xml", background),
        ("ic_launcher_foreground.xml", foreground),
        ("ic_launcher_monochrome.xml", monochrome),
    ):
        target = drawable / name
        target.write_text(content, encoding="utf-8")
        written.append(target)
    return written


def hex_of(color: tuple[int, int, int]) -> str:
    return "#{:02X}{:02X}{:02X}".format(*color)


# --------------------------------------------------------------------------------------
# Driver
# --------------------------------------------------------------------------------------


def resolve_slot(root: Path, slug: str) -> Path:
    apps = root / "apps"
    matches = sorted(
        candidate for candidate in apps.iterdir()
        if candidate.is_dir() and (candidate.name == slug or candidate.name.endswith(f"-{slug}"))
    )
    if not matches:
        raise SystemExit(f"BLOCKED: slot '{slug}' was not found under {apps}")
    if len(matches) > 1:
        raise SystemExit(f"BLOCKED: slot '{slug}' is ambiguous: {', '.join(p.name for p in matches)}")
    return matches[0]


def load_shots(store: Path) -> list[dict]:
    """Headlines come from journey.json: the journey already knows what each shot shows."""
    journey_path = store / "journey.json"
    if not journey_path.is_file():
        return []
    journey = json.loads(journey_path.read_text(encoding="utf-8"))
    return journey.get("shots", [])[:6]


def load_raw(store: Path, index: int) -> Image.Image | None:
    for candidate in (f"{index:02d}.png", f"phone-{index:02d}.png"):
        path = store / "captures" / "raw" / candidate
        if path.is_file():
            try:
                with Image.open(path) as source:
                    return source.convert("RGB")
            except OSError as error:
                print(f"WARN studio - ignoring unreadable capture {path.name}: {error}")
    return None


def default_studio(listing: dict) -> dict:
    return {
        "headlines": [],
        "kickers": [],
        "featureTagline": listing.get("locales", {}).get("en-US", {}).get("shortDescription", ""),
        "featurePill": "OFFLINE  ·  NO CODE TABLES",
    }


def render_slot(slot: Path, *, preview_circle: bool = False) -> None:
    store = slot / "store"
    listing = json.loads((store / "listing.json").read_text(encoding="utf-8"))
    studio_path = store / "studio.json"
    studio = default_studio(listing)
    if studio_path.is_file():
        studio.update(json.loads(studio_path.read_text(encoding="utf-8")))

    shots = load_shots(store)
    headlines = studio.get("headlines") or []
    kickers = studio.get("kickers") or []
    composed = []
    for index, shot in enumerate(shots):
        composed.append({
            "headline": (headlines[index] if index < len(headlines) else shot.get("title", "")),
            "body": shot.get("body", ""),
            "kicker": (kickers[index] if index < len(kickers) else ""),
        })

    icon = render_icon(listing, studio)
    assets = store / "assets"
    assets.mkdir(parents=True, exist_ok=True)

    icon_targets = [assets / "icon-512.png", assets / "en-US" / "icon-512.png"]
    for target in icon_targets:
        target.parent.mkdir(parents=True, exist_ok=True)
        # Saved as RGBA to match the mode store-kit validates and Play's expectation. The icon is
        # full-bleed, so every pixel is opaque and the alpha channel is uniform - it carries no
        # transparency, it just declares the channel.
        icon.convert("RGBA").save(target, format="PNG", optimize=True, compress_level=9)
        print(f"PASS studio - {target.relative_to(slot)} (512x512 RGBA)")

    sources = [load_raw(store, index) for index in range(1, 7)]
    feature_source = next((source for source in sources if source is not None), None)
    feature_targets = [assets / "feature-graphic-1024x500.png",
                       assets / "en-US" / "feature-graphic-1024x500.png"]
    for target in feature_targets:
        render_feature(listing, studio, feature_source).save(target, format="PNG",
                                                            optimize=True, compress_level=9)
        print(f"PASS studio - {target.relative_to(slot)} (1024x500)")

    composed_only = [item for item in composed if item["headline"]]
    for kind, (count, dimensions) in SCREEN_TARGETS.items():
        for index in range(1, count + 1):
            source = sources[(index - 1) % 6] if sources else None
            if source is None:
                print(f"WARN studio - no raw capture for {kind}-{index:02d}; skipped")
                continue
            item = composed_only[(index - 1) % len(composed_only)] if composed_only else {
                "headline": "", "body": "", "kicker": ""}
            target = assets / "en-US" / f"{kind}-{index:02d}.png"
            render_screenshot(dimensions, item, source).save(target, format="PNG",
                                                             optimize=True, compress_level=9)
            print(f"PASS studio - {target.relative_to(slot)} ({dimensions[0]}x{dimensions[1]})")

    colors = {
        "ink": hex_of(INK),
        "ink_lift": hex_of(INK_LIFT),
        "amber": hex_of(AMBER),
        "paper": hex_of(PAPER),
    }
    for path in write_vectors(slot / "app" / "src" / "main" / "res", colors):
        print(f"PASS studio - {path.relative_to(slot)}")

    if preview_circle:
        preview_dir = store / "captures" / "preview"
        preview_dir.mkdir(parents=True, exist_ok=True)
        for name, mask in preview_masks(icon).items():
            preview = Image.alpha_composite(icon.copy(), mask)
            preview.convert("RGB").save(preview_dir / f"icon-{name}.png")
            print(f"PASS studio - preview icon-{name}.png")


def preview_masks(icon: Image.Image) -> dict[str, Image.Image]:
    """Composite the icon under the two launcher masks, which is how a user actually sees it."""
    size = icon.size
    layers: dict[str, Image.Image] = {}
    shapes = {
        "circle": lambda draw: draw.ellipse((0, 0, size[0] - 1, size[1] - 1), fill=255),
        "squircle": lambda draw: draw.rounded_rectangle((0, 0, size[0] - 1, size[1] - 1),
                                                        radius=size[0] // 4, fill=255),
    }
    for name, shape in shapes.items():
        keep = Image.new("L", size, 0)
        shape(ImageDraw.Draw(keep))
        # Outside the mask the icon is hidden, so a corner that escapes the safe zone is visible.
        layer = Image.new("RGBA", size, (12, 14, 18, 255))
        layer.paste(icon, (0, 0), keep)
        layers[name] = layer
    return layers


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--slot", required=True, help="slot slug, e.g. pipe-sizing")
    parser.add_argument("--root", default=".", help="repository root (default: cwd)")
    parser.add_argument("--preview-circle", action="store_true",
                        help="also render the icon under the circle and squircle launcher masks")
    options = parser.parse_args()
    slot = resolve_slot(Path(options.root).resolve(), options.slot)
    render_slot(slot, preview_circle=options.preview_circle)
    print(f"STUDIO: OK {slot}")


if __name__ == "__main__":
    main()
