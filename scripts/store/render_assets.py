"""The drawn assets: the launcher icon, the feature graphic, and every screenshot set.

Pillow drawing helpers and the three writers. A slot that declares ``store/studio.json`` is composed by
``scripts/store-studio.py`` instead (``render_all`` routes it); this module is the generic renderer those
slots deliberately fall back to, never to.
"""

from __future__ import annotations

import sys
from pathlib import Path
from typing import Any, Callable
from PIL import Image, ImageDraw, ImageFont

# The sibling modules are files beside this one, not an installed package: the store directory goes
# on `sys.path` before they are imported, the line `scripts/store/validate.py` also uses.
sys.path.insert(0, str(Path(__file__).parent))

from render_limits import FEATURE_GRAPHIC_CANVAS, ICON_CANVAS, SCREEN_TARGETS



FONT_CANDIDATES = (
    "DejaVuSans.ttf",
    "C:/Windows/Fonts/arial.ttf",
    "C:/Windows/Fonts/segoeui.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "/System/Library/Fonts/Supplemental/Arial.ttf",
)


_FONT_WARNING_PRINTED = False




# --------------------------------------------------------------------------------------------
# Drawing helpers
# --------------------------------------------------------------------------------------------


def rgb(hex_color: str) -> tuple[int, int, int]:
    value = hex_color.lstrip("#")
    return tuple(int(value[i : i + 2], 16) for i in (0, 2, 4))  # type: ignore[return-value]




def mix(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))  # type: ignore[return-value]




def gradient(
    size: tuple[int, int], start: tuple[int, int, int], end: tuple[int, int, int]
) -> Image.Image:
    width, height = size
    image = Image.new("RGB", size)
    draw = ImageDraw.Draw(image)
    span = max(1, height - 1)
    for y in range(height):
        draw.line((0, y, width, y), fill=mix(start, end, y / span))
    return image




def font(size: int, *, bold: bool = False) -> ImageFont.ImageFont:
    global _FONT_WARNING_PRINTED
    candidates = list(FONT_CANDIDATES)
    if bold:
        candidates = [
            "DejaVuSans-Bold.ttf",
            "C:/Windows/Fonts/arialbd.ttf",
            "C:/Windows/Fonts/seguisb.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
            "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
        ] + candidates
    for candidate in candidates:
        try:
            return ImageFont.truetype(candidate, size=size)
        except OSError:
            continue
    if not _FONT_WARNING_PRINTED:
        print("WARN: DejaVuSans/system fonts unavailable; using Pillow default font")
        _FONT_WARNING_PRINTED = True
    return ImageFont.load_default(size=max(10, min(size, 32)))




def wrap_pixels(
    draw: ImageDraw.ImageDraw, text: str, face: ImageFont.ImageFont, width: int
) -> list[str]:
    words = text.split()
    if not words:
        return [""]
    lines: list[str] = []
    current = words[0]
    for word in words[1:]:
        trial = f"{current} {word}"
        if draw.textbbox((0, 0), trial, font=face)[2] <= width:
            current = trial
        else:
            lines.append(current)
            current = word
    lines.append(current)
    return lines




def draw_abstract_mark(
    draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], color: tuple[int, int, int, int]
) -> None:
    left, top, right, bottom = box
    width = right - left
    stroke = max(10, width // 15)
    draw.ellipse(box, outline=color, width=stroke)
    inset = width // 4
    draw.rounded_rectangle(
        (left + inset, top + inset, right - inset, bottom - inset),
        radius=width // 10,
        fill=color,
    )




def render_icon(path: Path, listing: dict[str, Any]) -> None:
    graphics = listing["graphicsSource"]
    start, end = rgb(graphics["brandColor"]), rgb(graphics["accentColor"])
    # Keep the source artwork rectangular: the factory contract forbids a baked-in rounded mask or
    # transparent corners, and it forbids a drawn shadow.
    canvas = gradient(ICON_CANVAS, start, end).convert("RGBA")
    draw = ImageDraw.Draw(canvas)
    draw_abstract_mark(draw, (146, 146, 366, 366), (255, 255, 255, 235))
    path.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(path, format="PNG", optimize=True, compress_level=9)




def render_feature(path: Path, listing: dict[str, Any], locale: str) -> None:
    graphics = listing["graphicsSource"]
    image = gradient(
        FEATURE_GRAPHIC_CANVAS, rgb(graphics["brandColor"]), rgb(graphics["accentColor"])
    )
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((70, 62, 954, 438), radius=48, fill=(12, 18, 32, 96))
    title_font = font(64, bold=True)
    body_font = font(30)
    entry = listing["locales"][locale]
    title_lines = wrap_pixels(draw, entry["title"], title_font, 760)[:2]
    y = 145
    for line in title_lines:
        draw.text((126, y), line, font=title_font, fill=(255, 255, 255))
        y += 76
    tagline = wrap_pixels(draw, entry["shortDescription"], body_font, 760)[:2]
    y += 15
    for line in tagline:
        draw.text((128, y), line, font=body_font, fill=(240, 244, 255))
        y += 42
    path.parent.mkdir(parents=True, exist_ok=True)
    image.convert("RGB").save(path, format="PNG", optimize=True, compress_level=9)




def synthetic_screen(
    size: tuple[int, int], index: int, caption: str, listing: dict[str, Any]
) -> Image.Image:
    width, height = size
    image = Image.new("RGB", size, (247, 248, 252))
    draw = ImageDraw.Draw(image)
    brand = rgb(listing["graphicsSource"]["brandColor"])
    accent = rgb(listing["graphicsSource"]["accentColor"])
    draw.rectangle((0, 0, width, max(80, height // 12)), fill=brand)
    draw.text(
        (width // 16, height // 32),
        listing["appName"],
        font=font(max(20, width // 24), bold=True),
        fill="white",
    )
    margin = width // 12
    top = height // 5
    draw.rounded_rectangle(
        (margin, top, width - margin, top + height // 4),
        radius=width // 22,
        fill=(255, 255, 255),
        outline=(220, 224, 234),
        width=max(2, width // 250),
    )
    title_face = font(max(24, width // 19), bold=True)
    body_face = font(max(18, width // 30))
    y = top + height // 22
    for line in wrap_pixels(draw, caption, title_face, width - margin * 3)[:3]:
        draw.text((margin + width // 18, y), line, font=title_face, fill=(32, 38, 52))
        y += max(35, width // 14)
    body = f"Step {index}: a preview of the described journey."
    draw.text((margin + width // 18, top + height // 5), body, font=body_face, fill=(88, 94, 108))
    center_y = int(height * 0.62)
    radius = width // 7
    draw.ellipse(
        (width // 2 - radius, center_y - radius, width // 2 + radius, center_y + radius), fill=accent
    )
    draw.rounded_rectangle(
        (
            width // 2 - radius // 4,
            center_y - radius // 2,
            width // 2 + radius // 4,
            center_y + radius // 2,
        ),
        radius=radius // 6,
        fill=(255, 255, 255),
    )
    return image




def load_raw_capture(captures: Path, index: int) -> Image.Image | None:
    candidates = (
        captures / f"{index:02d}.png",
        captures / f"phone-{index:02d}.png",
        captures / f"step-{index:02d}.png",
    )
    for candidate in candidates:
        if candidate.is_file():
            try:
                with Image.open(candidate) as source:
                    return source.convert("RGB")
            except OSError as exc:
                print(f"WARN: ignoring unreadable capture {candidate}: {exc}")
    return None




def cover(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    target_w, target_h = size
    scale = max(target_w / image.width, target_h / image.height)
    resized = image.resize(
        (round(image.width * scale), round(image.height * scale)), Image.Resampling.LANCZOS
    )
    left = (resized.width - target_w) // 2
    top = (resized.height - target_h) // 2
    return resized.crop((left, top, left + target_w, top + target_h))




def frame_screen(source: Image.Image, size: tuple[int, int], caption: str, listing: dict[str, Any]) -> Image.Image:
    width, height = size
    brand = rgb(listing["graphicsSource"]["brandColor"])
    accent = rgb(listing["graphicsSource"]["accentColor"])
    output = gradient(size, brand, accent)
    draw = ImageDraw.Draw(output)
    margin = round(width * 0.075)
    caption_top = round(height * 0.055)
    caption_face = font(max(30, round(width * 0.052)), bold=True)
    line_height = max(42, round(width * 0.065))
    for line in wrap_pixels(draw, caption, caption_face, width - 2 * margin)[:3]:
        draw.text((margin, caption_top), line, font=caption_face, fill=(255, 255, 255))
        caption_top += line_height
    screen_top = max(round(height * 0.23), caption_top + round(height * 0.035))
    screen_bottom = height - round(height * 0.055)
    screen_width = width - 2 * margin
    screen_height = screen_bottom - screen_top
    fitted = cover(source.convert("RGB"), (screen_width, screen_height))
    radius = max(28, width // 28)
    mask = Image.new("L", fitted.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, fitted.width, fitted.height), radius=radius, fill=255
    )
    shadow = Image.new("RGBA", size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        (margin + 10, screen_top + 14, width - margin + 10, screen_bottom + 14),
        radius=radius,
        fill=(0, 0, 0, 65),
    )
    output = Image.alpha_composite(output.convert("RGBA"), shadow)
    output.paste(fitted, (margin, screen_top), mask)
    return output.convert("RGB")




def render_screenshots(store: Path, listing: dict[str, Any], locales: list[str]) -> bool:
    """Write every locale's screenshot set; ``True`` when any frame had to be synthesised."""
    captures = store / "captures" / "raw"
    has_all_raw = all(load_raw_capture(captures, index) is not None for index in range(1, 7))
    for locale in locales:
        locale_dir = store / "assets" / locale
        locale_dir.mkdir(parents=True, exist_ok=True)
        captions = listing["locales"][locale]["screenshotCaptions"]
        sources = []
        for index in range(1, 7):
            raw = load_raw_capture(captures, index)
            sources.append(raw or synthetic_screen((720, 1280), index, captions[index - 1], listing))
        for kind, (count, target) in SCREEN_TARGETS.items():
            for index in range(1, count + 1):
                out = frame_screen(sources[index - 1], target, captions[index - 1], listing)
                out.save(
                    locale_dir / f"{kind}-{index:02d}.png", format="PNG", optimize=True, compress_level=9
                )
    return not has_all_raw
