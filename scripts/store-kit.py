#!/usr/bin/env python3
"""Generate and validate a complete, deterministic Google Play store pack.

listing.json schemaVersion 1 is the machine-readable source of truth.  The
required top-level fields and their types are documented in the generated
listing.md as well as validated here before assets are rendered.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import textwrap
import time
from typing import Any

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
APPS = ROOT / "apps"
SUPPORTED_LOCALES = ("en-US", "ru-RU")
MAX_PNG_BYTES = 8 * 1024 * 1024
SCREEN_TARGETS = {
    "phone": (6, (1080, 1920)),
    "tablet-7": (4, (1200, 1920)),
    "tablet-10": (4, (1600, 2560)),
}
FONT_CANDIDATES = (
    "DejaVuSans.ttf",
    "C:/Windows/Fonts/arial.ttf",
    "C:/Windows/Fonts/segoeui.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "/System/Library/Fonts/Supplemental/Arial.ttf",
)
_FONT_WARNING_PRINTED = False


class StoreKitError(RuntimeError):
    pass


def parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        description="Generate Play listing copy, graphics, screenshots, and validation reports."
    )
    target = p.add_mutually_exclusive_group(required=True)
    target.add_argument("--slot", help="Slot slug resolved beneath apps/.")
    target.add_argument(
        "--slot-dir", type=Path, help="Explicit slot directory (absolute or relative to cwd)."
    )
    p.add_argument("--init", action="store_true", help="Write draft listing.json and stop.")
    p.add_argument(
        "--locales", default=None, help="Comma-separated locales (supported: en-US,ru-RU)."
    )
    p.add_argument(
        "--capture", action="store_true", help="Drive an attached device with ADB before rendering."
    )
    p.add_argument(
        "--render-only", action="store_true", help="Render from existing captures or synthetic screens."
    )
    p.add_argument(
        "--validate-only", action="store_true", help="Validate existing copy and assets without writing."
    )
    return p


def resolve_slot(args: argparse.Namespace) -> Path:
    if args.slot_dir:
        slot = args.slot_dir.expanduser()
        if not slot.is_absolute():
            slot = (Path.cwd() / slot).resolve()
        else:
            slot = slot.resolve()
        if not slot.is_dir():
            raise StoreKitError(f"slot directory does not exist: {slot}")
        return slot
    assert args.slot
    if not APPS.is_dir():
        raise StoreKitError(f"apps directory does not exist: {APPS}")
    matches = sorted(
        p for p in APPS.iterdir() if p.is_dir() and (p.name == args.slot or p.name.endswith(f"-{args.slot}"))
    )
    if not matches:
        raise StoreKitError(f"no slot under apps/ equals or ends with '-{args.slot}'")
    if len(matches) > 1:
        names = ", ".join(p.name for p in matches)
        raise StoreKitError(f"slot '{args.slot}' is ambiguous: {names}")
    return matches[0]


def parse_locales(value: str | None, listing: dict[str, Any] | None = None) -> list[str]:
    if value:
        locales = [item.strip() for item in value.split(",") if item.strip()]
    elif listing:
        locales = list(listing.get("locales", {}).keys()) or [listing.get("defaultLocale", "en-US")]
    else:
        locales = ["en-US"]
    locales = list(dict.fromkeys(locales))
    bad = [locale for locale in locales if locale not in SUPPORTED_LOCALES]
    if bad:
        raise StoreKitError(
            f"unsupported locale(s): {', '.join(bad)}; supported: {', '.join(SUPPORTED_LOCALES)}"
        )
    if not locales:
        raise StoreKitError("at least one locale is required")
    return locales


def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except OSError as exc:
        raise StoreKitError(f"cannot read {path}: {exc}") from exc


def match_gradle(text: str, name: str, *, required: bool = True) -> str:
    match = re.search(rf"\b{re.escape(name)}\s*=\s*[\"']([^\"']+)[\"']", text)
    if match:
        return match.group(1).strip()
    if required:
        raise StoreKitError(f"app/build.gradle.kts is missing {name} = \"...\"")
    return ""


def prose_lines(path: Path) -> list[str]:
    if not path.is_file():
        return []
    lines = []
    fenced = False
    for raw in read_text(path).splitlines():
        line = raw.strip()
        if line.startswith("```"):
            fenced = not fenced
            continue
        if fenced or not line or line.startswith(("#", ">", "-", "*", "|", "<")):
            continue
        clean = re.sub(r"[`*_\[\]]", "", line).strip()
        if clean:
            lines.append(clean)
    return lines


def heading(path: Path) -> str:
    if not path.is_file():
        return ""
    for raw in read_text(path).splitlines():
        match = re.match(r"^#\s+(.+?)\s*$", raw)
        if match:
            return re.sub(r"[`*_]", "", match.group(1)).strip()
    return ""


def slot_metadata(slot: Path) -> dict[str, str]:
    gradle_path = slot / "app" / "build.gradle.kts"
    if not gradle_path.is_file():
        raise StoreKitError(f"missing {gradle_path}")
    gradle = read_text(gradle_path)
    application_id = match_gradle(gradle, "applicationId")
    version_name = match_gradle(gradle, "versionName")
    namespace = match_gradle(gradle, "namespace", required=False)
    version_code_match = re.search(r"\bversionCode\s*=\s*(\d+)", gradle)
    version_code = version_code_match.group(1) if version_code_match else ""

    explicit_name = match_gradle(gradle, "appName", required=False)
    strings_path = slot / "app" / "src" / "main" / "res" / "values" / "strings.xml"
    strings_name = ""
    if strings_path.is_file():
        found = re.search(r'<string\s+name=["\']app_name["\']>(.*?)</string>', read_text(strings_path), re.S)
        if found:
            strings_name = re.sub(r"<[^>]+>", "", found.group(1)).strip()
    readme = slot / "README.md"
    agents = slot / "AGENTS.md"
    app_name = explicit_name or strings_name or heading(readme) or heading(agents)
    if not app_name:
        stem = (namespace or application_id).split(".")[-1]
        app_name = " ".join(word.capitalize() for word in re.split(r"[-_]", stem) if word)
    sources = prose_lines(readme) + prose_lines(agents)
    idea = sources[0] if sources else f"A focused offline tool for {app_name}."
    idea = re.sub(r"\s+", " ", idea).strip().rstrip()
    if not idea.endswith((".", "!", "?")):
        idea += "."
    return {
        "app_name": app_name,
        "application_id": application_id,
        "namespace": namespace,
        "version_name": version_name,
        "version_code": version_code,
        "idea": idea,
        "slug": re.sub(r"^\d{4}-\d{2}-\d{2}-", "", slot.name),
    }


def truncate_chars(text: str, limit: int) -> str:
    text = re.sub(r"\s+", " ", text).strip()
    if len(text) <= limit:
        return text
    clipped = text[: max(1, limit - 1)].rstrip(" ,.;:-")
    return clipped + "…"


def english_copy(meta: dict[str, str]) -> dict[str, Any]:
    name = truncate_chars(meta["app_name"], 30)
    idea = truncate_chars(meta["idea"], 80)
    bullets = [
        "Stay focused with a clear, distraction-free workflow",
        "See meaningful progress at a glance",
        "Keep your experience private and fully offline",
    ]
    full = (
        f"{name} helps you turn intention into steady progress.\n\n"
        f"{meta['idea']}\n\n"
        "What you can do:\n"
        + "\n".join(f"• {item}" for item in bullets)
        + "\n\nDesigned for a calm, direct experience without accounts, ads, analytics, or cloud tracking."
    )
    return {
        "title": name,
        "shortDescription": idea,
        "fullDescription": full,
        "screenshotCaptions": [
            "Start with a clear overview",
            "Choose what matters now",
            "Take the next focused step",
            "See your progress clearly",
            "Build a steady daily rhythm",
            "Finish with confidence",
        ],
        "featureBullets": bullets,
    }


def russian_copy(meta: dict[str, str]) -> dict[str, Any]:
    name = truncate_chars(meta["app_name"], 30)
    bullets = [
        "Спокойный и понятный рабочий процесс",
        "Наглядный прогресс без лишних действий",
        "Полностью автономная и приватная работа",
    ]
    short = truncate_chars(f"{name}: простой способ сосредоточиться и видеть свой прогресс", 80)
    full = (
        f"{name} помогает превращать намерения в устойчивый прогресс.\n\n"
        f"{meta['idea']}\n\n"
        "Возможности:\n"
        + "\n".join(f"• {item}" for item in bullets)
        + "\n\nПриложение работает без аккаунтов, рекламы, аналитики и облачного отслеживания."
    )
    return {
        "title": name,
        "shortDescription": short,
        "fullDescription": full,
        "screenshotCaptions": [
            "Начните с понятного обзора",
            "Выберите главное на сегодня",
            "Сделайте следующий шаг",
            "Следите за прогрессом",
            "Создайте устойчивый ритм",
            "Завершайте дела уверенно",
        ],
        "featureBullets": bullets,
    }


def default_listing(meta: dict[str, str], locales: list[str]) -> dict[str, Any]:
    locale_data = {}
    for locale in locales:
        locale_data[locale] = russian_copy(meta) if locale == "ru-RU" else english_copy(meta)
    digest = hashlib.sha256(meta["application_id"].encode("utf-8")).hexdigest()
    brand = f"#{digest[:6]}"
    accent = f"#{digest[6:12]}"
    return {
        "schemaVersion": 1,
        "appName": meta["app_name"],
        "slug": meta["slug"],
        "applicationId": meta["application_id"],
        "idea": truncate_chars(meta["idea"], 240),
        "defaultLocale": locales[0],
        "category": "PRODUCTIVITY",
        "tags": ["offline", "focus", "productivity"],
        "contactEmail": "support@example.com",
        "privacyPolicyUrl": "https://example.com/privacy",
        "locales": locale_data,
        "dataSafety": {
            "collectsData": False,
            "reason": "Fully offline. No accounts, no analytics, no ads.",
            "dataTypes": [],
            "thirdPartySdks": [],
            "encryptedInTransit": True,
            "hasAccountDeletion": False,
        },
        "permissions": [],
        "ads": {"containsAds": False, "reason": "The app contains no advertising SDKs."},
        "contentRating": {"questionnaire": "notes", "targetAudience": "18+"},
        "graphicsSource": {"iconGlyph": "focus", "brandColor": brand, "accentColor": accent},
    }


def merge_missing_locales(listing: dict[str, Any], meta: dict[str, str], locales: list[str]) -> None:
    locale_map = listing.setdefault("locales", {})
    for locale in locales:
        if locale not in locale_map:
            locale_map[locale] = russian_copy(meta) if locale == "ru-RU" else english_copy(meta)
    listing["defaultLocale"] = listing.get("defaultLocale") if listing.get("defaultLocale") in locales else locales[0]


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def load_listing(path: Path) -> dict[str, Any]:
    if not path.is_file():
        raise StoreKitError(f"missing {path}; run with --init first")
    try:
        value = json.loads(read_text(path))
    except json.JSONDecodeError as exc:
        raise StoreKitError(f"invalid JSON in {path}: line {exc.lineno}: {exc.msg}") from exc
    if not isinstance(value, dict):
        raise StoreKitError(f"{path} must contain a JSON object")
    return value


def validate_listing(listing: dict[str, Any], locales: list[str]) -> list[str]:
    errors: list[str] = []
    required = {
        "schemaVersion": int,
        "appName": str,
        "slug": str,
        "applicationId": str,
        "idea": str,
        "defaultLocale": str,
        "category": str,
        "tags": list,
        "contactEmail": str,
        "privacyPolicyUrl": str,
        "locales": dict,
        "dataSafety": dict,
        "permissions": list,
        "ads": dict,
        "contentRating": dict,
        "graphicsSource": dict,
    }
    for field, kind in required.items():
        if not isinstance(listing.get(field), kind):
            errors.append(f"listing.{field} must be {kind.__name__}")
    if listing.get("schemaVersion") != 1:
        errors.append("listing.schemaVersion must be 1")
    locale_map = listing.get("locales") if isinstance(listing.get("locales"), dict) else {}
    for locale in locales:
        entry = locale_map.get(locale)
        if not isinstance(entry, dict):
            errors.append(f"listing.locales.{locale} is missing")
            continue
        for field, limit in (("title", 30), ("shortDescription", 80), ("fullDescription", 4000)):
            value = entry.get(field)
            if not isinstance(value, str) or not value.strip():
                errors.append(f"listing.locales.{locale}.{field} must be a non-empty string")
            elif len(value) > limit:
                errors.append(
                    f"listing.locales.{locale}.{field} is {len(value)} chars; Play limit is {limit}"
                )
        captions = entry.get("screenshotCaptions")
        if not isinstance(captions, list) or len(captions) != 6 or not all(isinstance(x, str) and x for x in captions):
            errors.append(f"listing.locales.{locale}.screenshotCaptions must contain 6 strings")
        bullets = entry.get("featureBullets")
        if not isinstance(bullets, list) or len(bullets) < 3 or not all(isinstance(x, str) and x for x in bullets):
            errors.append(f"listing.locales.{locale}.featureBullets must contain at least 3 strings")
    graphics = listing.get("graphicsSource", {})
    for field in ("brandColor", "accentColor"):
        if not re.fullmatch(r"#[0-9A-Fa-f]{6}", str(graphics.get(field, ""))):
            errors.append(f"listing.graphicsSource.{field} must be #RRGGBB")
    return errors


def rgb(hex_color: str) -> tuple[int, int, int]:
    value = hex_color.lstrip("#")
    return tuple(int(value[i : i + 2], 16) for i in (0, 2, 4))  # type: ignore[return-value]


def mix(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))  # type: ignore[return-value]


def gradient(size: tuple[int, int], start: tuple[int, int, int], end: tuple[int, int, int]) -> Image.Image:
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


def wrap_pixels(draw: ImageDraw.ImageDraw, text: str, face: ImageFont.ImageFont, width: int) -> list[str]:
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


def draw_abstract_mark(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], color: tuple[int, int, int, int]) -> None:
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
    base = gradient((512, 512), start, end).convert("RGBA")
    mask = Image.new("L", base.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((16, 16, 496, 496), radius=112, fill=255)
    canvas = Image.new("RGBA", base.size, (0, 0, 0, 0))
    canvas.paste(base, (0, 0), mask)
    draw = ImageDraw.Draw(canvas)
    draw_abstract_mark(draw, (146, 146, 366, 366), (255, 255, 255, 235))
    path.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(path, format="PNG", optimize=True, compress_level=9)


def render_feature(path: Path, listing: dict[str, Any], locale: str) -> None:
    graphics = listing["graphicsSource"]
    image = gradient((1024, 500), rgb(graphics["brandColor"]), rgb(graphics["accentColor"]))
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


def synthetic_screen(size: tuple[int, int], index: int, caption: str, listing: dict[str, Any]) -> Image.Image:
    width, height = size
    image = Image.new("RGB", size, (247, 248, 252))
    draw = ImageDraw.Draw(image)
    brand = rgb(listing["graphicsSource"]["brandColor"])
    accent = rgb(listing["graphicsSource"]["accentColor"])
    draw.rectangle((0, 0, width, max(80, height // 12)), fill=brand)
    draw.text((width // 16, height // 32), listing["appName"], font=font(max(20, width // 24), bold=True), fill="white")
    margin = width // 12
    top = height // 5
    draw.rounded_rectangle((margin, top, width - margin, top + height // 4), radius=width // 22, fill=(255, 255, 255), outline=(220, 224, 234), width=max(2, width // 250))
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
    draw.ellipse((width // 2 - radius, center_y - radius, width // 2 + radius, center_y + radius), fill=accent)
    draw.rounded_rectangle((width // 2 - radius // 4, center_y - radius // 2, width // 2 + radius // 4, center_y + radius // 2), radius=radius // 6, fill=(255, 255, 255))
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
    resized = image.resize((round(image.width * scale), round(image.height * scale)), Image.Resampling.LANCZOS)
    left = (resized.width - target_w) // 2
    top = (resized.height - target_h) // 2
    return resized.crop((left, top, left + target_w, top + target_h))


def frame_screen(
    source: Image.Image,
    size: tuple[int, int],
    caption: str,
    listing: dict[str, Any],
) -> Image.Image:
    width, height = size
    brand, accent = rgb(listing["graphicsSource"]["brandColor"]), rgb(listing["graphicsSource"]["accentColor"])
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
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, fitted.width, fitted.height), radius=radius, fill=255)
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
                out.save(locale_dir / f"{kind}-{index:02d}.png", format="PNG", optimize=True, compress_level=9)
    return not has_all_raw


def markdown_escape(value: Any) -> str:
    return str(value).replace("|", "\\|")


def write_documents(
    store: Path,
    listing: dict[str, Any],
    locales: list[str],
    synthetic: bool,
    meta: dict[str, str],
) -> None:
    schema = """`listing.json` schema version 1 requires identity fields (`appName`, `slug`,
`applicationId`, `idea`), publishing fields (`defaultLocale`, `category`, `tags`,
`contactEmail`, `privacyPolicyUrl`), locale copy, safety declarations, permissions,
ads/content-rating declarations, and `graphicsSource`. Each locale contains `title`,
`shortDescription`, `fullDescription`, six `screenshotCaptions`, and at least three
`featureBullets`."""
    listing_parts = [f"# Google Play listing — {listing['appName']}", "", "## Schema", "", schema]
    for locale in locales:
        entry = listing["locales"][locale]
        listing_parts += [
            "",
            f"## {locale}",
            "",
            f"### Title ({len(entry['title'])}/30)",
            "",
            entry["title"],
            "",
            f"### Short description ({len(entry['shortDescription'])}/80)",
            "",
            entry["shortDescription"],
            "",
            f"### Full description ({len(entry['fullDescription'])}/4000)",
            "",
            entry["fullDescription"],
            "",
            "### Screenshot captions",
            "",
        ] + [f"{i}. {caption}" for i, caption in enumerate(entry["screenshotCaptions"], 1)]
    (store / "listing.md").write_text("\n".join(listing_parts) + "\n", encoding="utf-8")

    safety = listing["dataSafety"]
    permissions = listing.get("permissions", [])
    permission_lines = (
        [f"- `{item['name']}` — {item['why']}" for item in permissions]
        if permissions
        else ["- None declared beyond platform-normal capabilities."]
    )
    privacy = f"""# Privacy policy — {listing['appName']}

**Last updated:** {time.strftime('%Y-%m-%d')}

## Summary

{safety['reason']}

## Data collection and sharing

The app does not collect, transmit, sell, or share personal data. Data created in the app remains on the user's device and is subject to the device's backup settings.

## Permissions

{chr(10).join(permission_lines)}

## Third-party services

No third-party SDKs that collect user data are declared.

## Data retention and deletion

The app has no account system. Users can delete locally stored data by clearing app storage or uninstalling the app.

## Security

Because the app does not transmit user data, data-in-transit exposure is not applicable. Platform security and encrypted transport are used for any operating-system services.

## Contact

Privacy questions: {listing['contactEmail']}
"""
    (store / "privacy.md").write_text(privacy, encoding="utf-8")

    type_rows = safety.get("dataTypes", [])
    data_rows = (
        [f"| {markdown_escape(item)} | Yes | No | As declared in Play Console |" for item in type_rows]
        if type_rows
        else ["| None | No | No | Not applicable |"]
    )
    sdk_rows = safety.get("thirdPartySdks", [])
    data_safety = f"""# Play Console data safety answers — {listing['appName']}

## Data collection and sharing

- Does the app collect or share required user data? **{'Yes' if safety['collectsData'] else 'No'}**
- Reason: {safety['reason']}

| Data category | Collected | Shared | Purpose |
|---|---:|---:|---|
{chr(10).join(data_rows)}

## Third-party SDKs

{'None.' if not sdk_rows else chr(10).join(f'- {sdk}' for sdk in sdk_rows)}

## Security practices

- Is collected data encrypted in transit? **{'Yes' if safety['encryptedInTransit'] else 'No'}**
- Can users request data deletion? **{'Yes' if safety['hasAccountDeletion'] else 'Not applicable — no accounts'}**

## Account deletion

- Does the app support accounts? **No**
- Is an account-deletion mechanism required? **No**
"""
    (store / "data-safety.md").write_text(data_safety, encoding="utf-8")

    review = f"""# Reviewer notes — {listing['appName']}

## Access

No account, payment, invitation, or demo credentials are required.

## Review journey

1. Launch the app and review the main overview.
2. Start the primary workflow described by the first action.
3. Complete an item or session and inspect the progress state.
4. Close and reopen the app to confirm local persistence where applicable.

## Permissions

{chr(10).join(permission_lines)}

## Human checks required before upload

- TODO-HUMAN: Replace `{listing['contactEmail']}` with a monitored support address if it is still a placeholder.
- TODO-HUMAN: Host `store/privacy.md` publicly and replace `{listing['privacyPolicyUrl']}` with its final HTTPS URL.
- TODO-HUMAN: Confirm the data-safety answers and permission list against the release AAB and all bundled SDKs.
- TODO-HUMAN: {'Replace synthetic screenshots with real captures and regenerate the pack.' if synthetic else 'Inspect every captured screen for personal or test data.'}
- TODO-HUMAN: Complete the Play Console legal declarations and first-app setup.

Version reviewed: `{meta['version_name']}` (code `{meta['version_code'] or 'not declared'}`)
"""
    (store / "review-notes.md").write_text(review, encoding="utf-8")

    status = "synthetic — replace with real captures before publish" if synthetic else "framed from real device captures"
    graphics = f"""# Graphics inventory — {listing['appName']}

Screenshot source status: **{status}**.

| Asset | Dimensions | Format / purpose |
|---|---:|---|
| `assets/icon-512.png` | 512×512 | RGBA Play store icon; transparent corners |
| `assets/feature-graphic-1024x500.png` | 1024×500 | RGB feature graphic |
| `assets/<locale>/phone-01..06.png` | 1080×1920 | RGB phone screenshots |
| `assets/<locale>/tablet-7-01..04.png` | 1200×1920 | RGB 7-inch tablet screenshots |
| `assets/<locale>/tablet-10-01..04.png` | 1600×2560 | RGB 10-inch tablet screenshots |

Every locale folder also contains its own icon and feature graphic. The feature graphic keeps titles and taglines inside a generous safe area because Play may crop edges on some surfaces.

Four screenshots are supplied for both tablet formats. This satisfies the large-format eligibility asset count; eligibility also depends on the app's actual large-screen quality and Play requirements at submission time.
"""
    (store / "graphics.md").write_text(graphics, encoding="utf-8")

    checklist = f"""# Google Play upload checklist — {listing['appName']}

- [ ] Run `python scripts/store-kit.py --slot {listing['slug']} --validate-only` and resolve every FAIL.
- [ ] Replace synthetic screenshots with real captures when `graphics.md` marks them synthetic.
- [ ] Compare listing claims and captions with the release build.
- [ ] Replace placeholder contact email and privacy-policy URL in `listing.json`.
- [ ] Host `privacy.md` at the HTTPS URL declared in `listing.json`.
- [ ] Audit permissions and third-party SDK behavior against `data-safety.md`.
- [ ] Build and locally test the signed release AAB from `dist/`.
- [ ] Enrol in Play App Signing and keep upload credentials outside the repository.
- [ ] Complete Ads, Content rating, Target audience, App access, and Data safety declarations.
- [ ] Upload phone, 7-inch tablet, and 10-inch tablet assets for every locale.
- [ ] Verify edge-to-edge, orientation, text scale, and tablet layouts on representative devices.
- [ ] For a brand-new package, upload the first AAB and complete legal declarations manually in Play Console.
- [ ] Use `node scripts/play-upload.mjs --slot {listing['slug']} --track internal --dry-run` before any live upload.
- [ ] Obtain explicit human approval before committing a Play edit.
"""
    (store / "checklist.md").write_text(checklist, encoding="utf-8")


def journey_example(listing: dict[str, Any]) -> dict[str, Any]:
    captions = listing["locales"][listing["defaultLocale"]]["screenshotCaptions"]
    actions = [
        [],
        [{"type": "tap", "x": 540, "y": 1600}],
        [{"type": "wait", "ms": 800}, {"type": "tap", "x": 540, "y": 1450}],
        [{"type": "swipe", "x1": 540, "y1": 1500, "x2": 540, "y2": 600, "durationMs": 350}],
        [{"type": "back"}],
        [{"type": "tap", "x": 950, "y": 120}],
    ]
    return {
        "schemaVersion": 1,
        "_comment": "Edit coordinates/text for the real app. Each shot runs its steps, then captures one raw PNG.",
        "shots": [
            {"name": f"shot-{index:02d}", "title": caption, "body": f"Journey step {index}", "steps": actions[index - 1]}
            for index, caption in enumerate(captions, 1)
        ],
        "supportedStepTypes": {
            "tap": {"x": 540, "y": 1200},
            "swipe": {"x1": 540, "y1": 1500, "x2": 540, "y2": 500, "durationMs": 350},
            "wait": {"ms": 750},
            "text": {"value": "example text"},
            "back": {},
        },
    }


def adb_path() -> str | None:
    found = shutil.which("adb")
    if found:
        return found
    android_home = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if android_home:
        candidate = Path(android_home) / "platform-tools" / ("adb.exe" if os.name == "nt" else "adb")
        if candidate.is_file():
            return str(candidate)
    return None


def blocked_capture(reason: str) -> None:
    print(f"BLOCKED: capture unavailable — {reason}")
    print("BLOCKED: start an emulator with:")
    print("  $ANDROID_HOME/emulator/emulator -list-avds")
    print("  $ANDROID_HOME/emulator/emulator -avd <AVD_NAME>")
    print("  adb wait-for-device")
    print("Falling back to --render-only synthetic screenshots.")


def run_adb(adb: str, serial: str, args: list[str], *, binary: bool = False) -> bytes | str:
    command = [adb, "-s", serial, *args]
    result = subprocess.run(command, capture_output=True, text=not binary, check=False)
    if result.returncode != 0:
        stderr = result.stderr.decode("utf-8", errors="replace") if binary else result.stderr
        raise StoreKitError(f"ADB command failed ({' '.join(args)}): {stderr.strip()}")
    return result.stdout


def capture_with_adb(store: Path, listing: dict[str, Any]) -> bool:
    journey_path = store / "journey.json"
    if not journey_path.is_file():
        write_json(journey_path, journey_example(listing))
        print(f"PASS: wrote documented journey example {journey_path}")
    adb = adb_path()
    if not adb:
        blocked_capture("adb was not found on PATH or under ANDROID_HOME/platform-tools")
        return False
    devices = subprocess.run([adb, "devices"], capture_output=True, text=True, check=False)
    serials = []
    if devices.returncode == 0:
        serials = [line.split()[0] for line in devices.stdout.splitlines()[1:] if line.strip().endswith("\tdevice")]
    if not serials:
        blocked_capture("adb found no attached device in 'device' state")
        return False
    serial = serials[0]
    journey = json.loads(read_text(journey_path))
    shots = journey.get("shots", [])
    if not isinstance(shots, list) or len(shots) < 6:
        raise StoreKitError(f"{journey_path} must define at least 6 shots")
    raw_dir = store / "captures" / "raw"
    raw_dir.mkdir(parents=True, exist_ok=True)
    run_adb(adb, serial, ["shell", "monkey", "-p", listing["applicationId"], "-c", "android.intent.category.LAUNCHER", "1"])
    time.sleep(1)
    for index, shot in enumerate(shots[:6], 1):
        for step in shot.get("steps", []):
            kind = step.get("type")
            if kind == "tap":
                run_adb(adb, serial, ["shell", "input", "tap", str(step["x"]), str(step["y"])])
            elif kind == "swipe":
                run_adb(adb, serial, ["shell", "input", "swipe", str(step["x1"]), str(step["y1"]), str(step["x2"]), str(step["y2"]), str(step.get("durationMs", 350))])
            elif kind == "wait":
                time.sleep(max(0, int(step.get("ms", 0))) / 1000)
            elif kind == "text":
                value = str(step.get("value", "")).replace(" ", "%s")
                run_adb(adb, serial, ["shell", "input", "text", value])
            elif kind == "back":
                run_adb(adb, serial, ["shell", "input", "keyevent", "4"])
            else:
                raise StoreKitError(f"unsupported journey step type: {kind!r}")
        payload = run_adb(adb, serial, ["exec-out", "screencap", "-p"], binary=True)
        assert isinstance(payload, bytes)
        (raw_dir / f"{index:02d}.png").write_bytes(payload)
        print(f"PASS: captured raw screenshot {index:02d} from {serial}")
    return True


def render_all(store: Path, listing: dict[str, Any], locales: list[str], meta: dict[str, str]) -> None:
    assets = store / "assets"
    icon = assets / "icon-512.png"
    feature = assets / "feature-graphic-1024x500.png"
    render_icon(icon, listing)
    render_feature(feature, listing, listing["defaultLocale"])
    for locale in locales:
        locale_dir = assets / locale
        locale_dir.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(icon, locale_dir / icon.name)
        render_feature(locale_dir / feature.name, listing, locale)
    synthetic = render_screenshots(store, listing, locales)
    write_documents(store, listing, locales, synthetic, meta)


def inspect_image(
    path: Path,
    expected: tuple[int, int],
    mode: str,
    *,
    max_bytes: int = MAX_PNG_BYTES,
    screenshot_rules: bool = False,
) -> list[str]:
    failures = []
    if not path.is_file():
        return ["missing"]
    size_bytes = path.stat().st_size
    if size_bytes > max_bytes:
        failures.append(f"{size_bytes} bytes exceeds {max_bytes}")
    try:
        with Image.open(path) as image:
            image.load()
            if image.size != expected:
                failures.append(f"dimensions {image.width}x{image.height}, expected {expected[0]}x{expected[1]}")
            if image.mode != mode:
                failures.append(f"mode {image.mode}, expected {mode}")
            if image.format != "PNG":
                failures.append(f"format {image.format}, expected PNG")
            if screenshot_rules:
                short, long = sorted(image.size)
                if short < 320 or long > 3840 or long > 2 * short:
                    failures.append("violates Play screenshot side/aspect limits")
    except OSError as exc:
        failures.append(f"unreadable image: {exc}")
    return failures


def validate_store(store: Path, listing: dict[str, Any], locales: list[str]) -> bool:
    failures = validate_listing(listing, locales)
    if failures:
        for failure in failures:
            print(f"FAIL listing.json — {failure}")
    else:
        print("PASS listing.json — schema=1 and Play character limits satisfied")

    required_docs = ("listing.md", "privacy.md", "data-safety.md", "review-notes.md", "graphics.md", "checklist.md")
    for name in required_docs:
        path = store / name
        if path.is_file() and path.stat().st_size:
            print(f"PASS {name} — {path.stat().st_size} bytes")
        else:
            print(f"FAIL {name} — missing or empty")
            failures.append(f"missing or empty {name}")

    specs: list[tuple[Path, tuple[int, int], str, bool]] = [
        (store / "assets" / "icon-512.png", (512, 512), "RGBA", False),
        (store / "assets" / "feature-graphic-1024x500.png", (1024, 500), "RGB", False),
    ]
    for locale in locales:
        base = store / "assets" / locale
        specs += [
            (base / "icon-512.png", (512, 512), "RGBA", False),
            (base / "feature-graphic-1024x500.png", (1024, 500), "RGB", False),
        ]
        for kind, (count, dimensions) in SCREEN_TARGETS.items():
            for index in range(1, count + 1):
                specs.append((base / f"{kind}-{index:02d}.png", dimensions, "RGB", True))
    for path, dimensions, mode, screenshot_rules in specs:
        image_failures = inspect_image(
            path,
            dimensions,
            mode,
            max_bytes=(1024 * 1024 if path.name == "icon-512.png" else MAX_PNG_BYTES),
            screenshot_rules=screenshot_rules,
        )
        relative = path.relative_to(store).as_posix()
        if image_failures:
            print(f"FAIL {relative} — {'; '.join(image_failures)}")
            failures.extend(f"{relative}: {failure}" for failure in image_failures)
        else:
            size_bytes = path.stat().st_size
            print(f"PASS {relative} — {dimensions[0]}x{dimensions[1]} {mode}, {size_bytes} bytes")
    return not failures


def summary(slot: Path, locales: list[str]) -> str:
    return (
        f"STORE-KIT: OK apps/{slot.name}/store "
        f"(icon=512x512 feature=1024x500 phone=6 tablet7=4 tablet10=4 locales={','.join(locales)})"
    )


def main(argv: list[str] | None = None) -> int:
    args = parser().parse_args(argv)
    if args.init and (args.capture or args.render_only or args.validate_only):
        raise StoreKitError("--init cannot be combined with --capture, --render-only, or --validate-only")
    if args.capture and args.render_only:
        raise StoreKitError("--capture and --render-only are mutually exclusive")
    slot = resolve_slot(args)
    meta = slot_metadata(slot)
    store = slot / "store"
    listing_path = store / "listing.json"

    existing = load_listing(listing_path) if listing_path.is_file() else None
    locales = parse_locales(args.locales, existing)
    if args.init:
        listing = default_listing(meta, locales)
        write_json(listing_path, listing)
        review = store / "review-notes.md"
        review.write_text(
            f"# Reviewer notes — {listing['appName']}\n\n"
            "- TODO-HUMAN: Review listing.json claims against the finished app.\n"
            "- TODO-HUMAN: Replace the support email and privacy-policy URL before upload.\n"
            "- TODO-HUMAN: Run the full store-kit generator after review.\n",
            encoding="utf-8",
        )
        print(f"PASS: initialized draft {listing_path}")
        return 0

    if existing is None:
        existing = default_listing(meta, locales)
        write_json(listing_path, existing)
        print(f"WARN: listing.json was missing; initialized draft before generation")
    listing = existing
    merge_missing_locales(listing, meta, locales)
    preflight = validate_listing(listing, locales)
    if preflight:
        raise StoreKitError("listing validation failed:\n  - " + "\n  - ".join(preflight))
    if args.locales:
        write_json(listing_path, listing)

    if args.validate_only:
        ok = validate_store(store, listing, locales)
        if ok:
            print(summary(slot, locales))
            return 0
        return 1

    if args.capture:
        capture_with_adb(store, listing)
    render_all(store, listing, locales, meta)
    ok = validate_store(store, listing, locales)
    if not ok:
        return 1
    print(summary(slot, locales))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except StoreKitError as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        raise SystemExit(1)
