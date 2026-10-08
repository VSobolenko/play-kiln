#!/usr/bin/env python3
"""Validate a Play store pack against the console's own windows, and print a verdict.

Every path and every limit comes from ``scripts/store/model.py``, which loads them from
``scripts/lib/play-assets.json`` -- the single source of truth this script may not restate.

**This script reads and never writes.**  It creates no temporary file, no cache and no report
inside the slot: it opens the pack's files for reading, prints rows to stdout, and that is all.
Byte-compiled modules are disabled below so even the import of the sibling model leaves nothing
behind in the slot's tree.

Exit codes:

* ``0`` -- every check passed.
* ``1`` -- at least one check failed, or a declared URL is reported ``UNFINISHED``.
* ``2`` -- a check could not be evaluated at all (the reason is printed with the row).

A check that could not run is never printed as a pass: a row is ``PASS`` only when the measurement
it names actually happened.  A PNG the dependency-free decoder cannot read is a **failure**, not a
pass -- a file that decodes to nothing must not slip through looking valid.
"""

from __future__ import annotations

import argparse
import hashlib
import math
import re
import sys
import zlib
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Sequence
from urllib.parse import urlparse

# Nothing this script does may leave a file behind, so no import writes a `.pyc` either.
sys.dont_write_bytecode = True
# The sibling model is a file next to this one, not an installed package.
sys.path.insert(0, str(Path(__file__).resolve().parent))
import model  # noqa: E402  (scripts/store/model.py, the pack's only path and limit resolver)


# --------------------------------------------------------------------------------------------
# Verdicts and rows
# --------------------------------------------------------------------------------------------

PASS = "PASS"
FAIL = "FAIL"
SKIP = "SKIP"
UNFINISHED = "UNFINISHED"
UNVERIFIED = "UNVERIFIED"

# A `FAIL` is a rule violation; `UNFINISHED` is the pack-contract word for a placeholder the owner
# still has to replace. Both mean "this pack may not ship", so both block.
BLOCKING = frozenset({FAIL, UNFINISHED})

_CHECK_WIDTH = 24
_VERDICT_WIDTH = 10


@dataclass(frozen=True)
class Row:
    check: str
    subject: str
    verdict: str
    detail: str = ""


def format_row(row: Row) -> str:
    head = f"{row.verdict:<{_VERDICT_WIDTH}} {row.check:<{_CHECK_WIDTH}} {row.subject}"
    return f"{head} - {row.detail}" if row.detail else head


class Report:
    """Collects rows, printing each one as it is added."""

    def __init__(self) -> None:
        self.rows: list[Row] = []

    def add(self, check: str, subject: str, verdict: str, detail: str = "") -> None:
        row = Row(check, subject, verdict, detail)
        self.rows.append(row)
        print(format_row(row))

    def count(self, verdict: str) -> int:
        return sum(1 for row in self.rows if row.verdict == verdict)


# --------------------------------------------------------------------------------------------
# Image probing: dimensions, format and alpha, without a dependency
# --------------------------------------------------------------------------------------------

PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
_PNG_ALPHA_COLOR_TYPES = frozenset({4, 6})  # greyscale+alpha, truecolour+alpha
_PNG_CHANNELS = {0: 1, 2: 3, 4: 2, 6: 4}
_JPEG_START_OF_FRAME = frozenset(range(0xC0, 0xD0)) - {0xC4, 0xC8, 0xCC}


class PngDecodeError(RuntimeError):
    """The bytes are not an 8-bit, non-interlaced PNG this decoder can measure."""


@dataclass(frozen=True)
class ImageProbe:
    exists: bool
    size_bytes: int
    fmt: str
    width: int | None
    height: int | None
    has_alpha: bool | None

    @property
    def dimensions(self) -> tuple[int, int] | None:
        if self.width is None or self.height is None:
            return None
        return (self.width, self.height)


def sniff_format(data: bytes) -> str:
    if data.startswith(PNG_SIGNATURE):
        return "png"
    if data[:2] == b"\xff\xd8":
        return "jpeg"
    return "unknown"


def read_head(path: Path, length: int = 8) -> bytes:
    """The first ``length`` bytes of a file, without reading the whole of it."""
    try:
        with path.open("rb") as handle:
            return handle.read(length)
    except OSError:
        return b""


def png_header(data: bytes) -> tuple[int, int, int, int, int] | None:
    """``(width, height, bit_depth, color_type, interlace)`` or ``None``."""
    if len(data) < 24 or not data.startswith(PNG_SIGNATURE):
        return None
    if data[12:16] != b"IHDR":
        return None
    width = int.from_bytes(data[16:20], "big")
    height = int.from_bytes(data[20:24], "big")
    if width <= 0 or height <= 0:
        return None
    return (width, height, data[24], data[25], data[28])


def jpeg_size(data: bytes) -> tuple[int, int] | None:
    """The JPEG's pixel size, read from its first Start-Of-Frame marker."""
    if len(data) < 4 or data[0] != 0xFF or data[1] != 0xD8:
        return None
    offset = 2
    while offset + 3 < len(data):
        while offset < len(data) and data[offset] != 0xFF:
            offset += 1
        while offset < len(data) and data[offset] == 0xFF:
            offset += 1
        if offset >= len(data):
            break
        marker = data[offset]
        offset += 1
        if marker == 0xD8 or marker == 0xD9 or marker == 0x01 or 0xD0 <= marker <= 0xD7:
            continue
        if offset + 1 >= len(data):
            return None
        length = int.from_bytes(data[offset : offset + 2], "big")
        if length < 2 or offset + length > len(data):
            return None
        if marker in _JPEG_START_OF_FRAME:
            if length < 7:
                return None
            height = int.from_bytes(data[offset + 3 : offset + 5], "big")
            width = int.from_bytes(data[offset + 5 : offset + 7], "big")
            return (width, height) if width > 0 and height > 0 else None
        offset += length
    return None


def probe_image(path: Path) -> ImageProbe:
    """Read an image's format, size and alpha presence without decoding its pixels."""
    try:
        data = path.read_bytes()
    except OSError:
        return ImageProbe(False, 0, "missing", None, None, None)
    fmt = sniff_format(data)
    width = height = None
    has_alpha: bool | None = None
    if fmt == "png":
        header = png_header(data)
        if header is not None:
            width, height, _bit_depth, color_type, _interlace = header
            has_alpha = color_type in _PNG_ALPHA_COLOR_TYPES or b"tRNS" in data
    elif fmt == "jpeg":
        size = jpeg_size(data)
        if size is not None:
            width, height = size
        has_alpha = False
    return ImageProbe(True, len(data), fmt, width, height, has_alpha)


# --------------------------------------------------------------------------------------------
# The dependency-free pixel measurement, ported from `pngContentStats` in play-assets.mjs
# --------------------------------------------------------------------------------------------


@dataclass(frozen=True)
class PngStats:
    width: int
    height: int
    luminance_std_dev: float
    distinct_colours: int


def _unfilter_row(filter_type: int, row: bytearray, previous: bytearray, bpp: int) -> None:
    length = len(row)
    if filter_type == 0:
        return
    if filter_type == 1:
        for i in range(bpp, length):
            row[i] = (row[i] + row[i - bpp]) & 0xFF
        return
    if filter_type == 2:
        for i in range(length):
            row[i] = (row[i] + previous[i]) & 0xFF
        return
    if filter_type == 3:
        for i in range(length):
            left = row[i - bpp] if i >= bpp else 0
            row[i] = (row[i] + ((left + previous[i]) >> 1)) & 0xFF
        return
    if filter_type == 4:
        for i in range(length):
            left = row[i - bpp] if i >= bpp else 0
            above = previous[i]
            upper_left = previous[i - bpp] if i >= bpp else 0
            row[i] = (row[i] + _paeth(left, above, upper_left)) & 0xFF
        return
    raise PngDecodeError(f"unsupported PNG filter type {filter_type}")


def _paeth(left: int, above: int, upper_left: int) -> int:
    estimate = left + above - upper_left
    left_distance = abs(estimate - left)
    above_distance = abs(estimate - above)
    upper_left_distance = abs(estimate - upper_left)
    if left_distance <= above_distance and left_distance <= upper_left_distance:
        return left
    if above_distance <= upper_left_distance:
        return above
    return upper_left


def png_content_stats(path: Path) -> PngStats:
    """Luminance standard deviation and distinct quantised colours, using only the stdlib.

    This is a port of ``pngContentStats`` in ``scripts/lib/play-assets.mjs``; ``zlib`` plus the
    PNG chunk format is all it needs, deliberately, so the gate does not depend on Pillow being
    installed to look at a pixel.  It raises :class:`PngDecodeError` for anything it cannot measure.
    A caller must treat that as a failure rather than a pass: a truncated or unsupported file
    decodes to nothing, and nothing must not read as real content.
    """
    try:
        data = path.read_bytes()
    except OSError as error:
        raise PngDecodeError(f"unreadable: {error}") from error
    header = png_header(data)
    if header is None:
        raise PngDecodeError("not a PNG (no readable IHDR)")
    width, height, bit_depth, color_type, interlace = header
    channels = _PNG_CHANNELS.get(color_type)
    if bit_depth != 8 or interlace != 0 or channels is None:
        raise PngDecodeError(
            f"unsupported PNG encoding (bit depth {bit_depth}, colour type {color_type}, "
            f"interlace {interlace}); only 8-bit non-interlaced images can be measured"
        )

    idat: list[bytes] = []
    offset = 8
    while offset + 12 <= len(data):
        length = int.from_bytes(data[offset : offset + 4], "big")
        chunk_type = data[offset + 4 : offset + 8]
        if offset + 12 + length > len(data):
            raise PngDecodeError("truncated PNG chunk")
        if chunk_type == b"IDAT":
            idat.append(data[offset + 8 : offset + 8 + length])
        offset += 12 + length
        if chunk_type == b"IEND":
            break
    if not idat:
        raise PngDecodeError("no IDAT data")

    try:
        raw = zlib.decompress(b"".join(idat))
    except zlib.error as error:
        raise PngDecodeError(f"undecodable IDAT stream: {error}") from error

    stride = width * channels
    if len(raw) < height * (stride + 1):
        raise PngDecodeError("decoded data is shorter than the declared image")

    previous = bytearray(stride)
    row = bytearray(stride)
    # 4 bits per channel keeps the colour histogram at 16^3 slots: small enough to allocate once,
    # large enough that a photo-like frame fills hundreds of them.
    seen: set[int] = set()
    total = 0
    total_squared = 0
    index = 0
    for _y in range(height):
        filter_type = raw[index]
        index += 1
        row[:] = raw[index : index + stride]
        index += stride
        _unfilter_row(filter_type, row, previous, channels)
        reds = row[0::channels]
        if channels >= 3:
            greens = row[1::channels]
            blues = row[2::channels]
            for red, green, blue in zip(reds, greens, blues):
                luminance = (red * 77 + green * 150 + blue * 29) >> 8
                total += luminance
                total_squared += luminance * luminance
                seen.add(((red >> 4) << 8) | ((green >> 4) << 4) | (blue >> 4))
        else:
            for grey in reds:
                luminance = (grey * 77 + grey * 150 + grey * 29) >> 8
                total += luminance
                total_squared += luminance * luminance
                seen.add(((grey >> 4) << 8) | ((grey >> 4) << 4) | (grey >> 4))
        previous[:] = row

    pixels = width * height
    mean = total / pixels
    variance = max(0.0, total_squared / pixels - mean * mean)
    return PngStats(width, height, math.sqrt(variance), len(seen))


# --------------------------------------------------------------------------------------------
# Checks
# --------------------------------------------------------------------------------------------


def aspect_text() -> str:
    return " or ".join(f"{across}:{down}" for across, down in model.ASPECT_RATIOS)


def allowed_aspect(width: int, height: int) -> bool:
    """16:9 or 9:16, decided by integer cross-multiplication so an exact canvas never near-misses."""
    for across, down in model.ASPECT_RATIOS:
        if width * down == height * across or width * across == height * down:
            return True
    return False


def check_inventory(report: Report, slot: Path, locales: Sequence[str]) -> None:
    required_files = model.required_pack_files(locales)
    missing = [rel for rel in required_files if not (slot / rel).is_file()]
    for rel in missing:
        report.add("inventory:missing", rel, FAIL, "required by the pack contract but absent")

    counts_ok = True
    for locale in locales:
        directory = model.locale_assets_dir(slot, locale)
        for kind in model.SCREENSHOT_KINDS:
            expected = model.shipped_count(kind)
            present = sum(
                1
                for index in range(1, expected + 1)
                if (directory / model.screenshot_asset_name(kind, index)).is_file()
            )
            if present == expected:
                report.add(
                    "inventory:count", f"{locale}/{kind}", PASS, f"{present}/{expected} present"
                )
                continue
            counts_ok = False
            report.add(
                "inventory:count",
                f"{locale}/{kind}",
                FAIL,
                f"the pack declares {expected} {kind} screenshots but ships {present}",
            )

    required_set = set(model.required_assets(locales))
    assets_root = model.assets_dir(slot)
    on_disk = (
        sorted(path.relative_to(slot).as_posix() for path in assets_root.rglob("*") if path.is_file())
        if assets_root.is_dir()
        else []
    )
    unexpected = [rel for rel in on_disk if rel not in required_set]
    for rel in unexpected:
        report.add("inventory:unexpected", rel, FAIL, "not part of the declared pack inventory")

    counts_text = ", ".join(
        f"{kind}={count}"
        for kind, count in model.asset_counts(locales).items()
        if kind != "total"
    )
    detail = (
        f"{len(required_files) - len(missing)}/{len(required_files)} required files present; "
        f"{counts_text}; {len(unexpected)} unexpected"
    )
    report.add(
        "inventory",
        f"{len(locales)} locale(s)",
        PASS if not missing and not unexpected and counts_ok else FAIL,
        detail,
    )


def check_icon(report: Report, slot: Path, locales: Sequence[str]) -> None:
    limits = model.ICON
    for asset in model.composed_assets(locales):
        if asset.kind != "icon":
            continue
        path = slot / asset.relpath
        image = probe_image(path)
        problems: list[str] = []
        if not image.exists:
            problems.append("absent")
        else:
            if image.size_bytes > limits["maxBytes"]:
                problems.append(f"{image.size_bytes} bytes exceeds the {limits['maxBytes']}-byte cap")
            if image.fmt != limits["format"]:
                problems.append(f"format {image.fmt}, expected {limits['format']}")
            expected = (limits["width"], limits["height"])
            if image.dimensions != expected:
                problems.append(f"dimensions {image.width}x{image.height}, expected {expected[0]}x{expected[1]}")
            if image.has_alpha is not True:
                problems.append("no alpha channel, but a 32-bit PNG with alpha is required")
        report.add(
            "icon",
            asset.relpath,
            PASS if not problems else FAIL,
            "; ".join(problems)
            if problems
            else f"{image.width}x{image.height} {image.fmt} with alpha, {image.size_bytes} bytes",
        )


def check_feature_graphic(report: Report, slot: Path, locales: Sequence[str]) -> None:
    limits = model.FEATURE_GRAPHIC
    formats = tuple(limits["formats"])
    for asset in model.composed_assets(locales):
        if asset.kind != "feature-graphic":
            continue
        path = slot / asset.relpath
        image = probe_image(path)
        problems: list[str] = []
        if not image.exists:
            problems.append("absent")
        else:
            if image.size_bytes > limits["maxBytes"]:
                problems.append(f"{image.size_bytes} bytes exceeds the {limits['maxBytes']}-byte cap")
            if image.fmt not in formats:
                problems.append(f"format {image.fmt}, expected one of {', '.join(formats)}")
            expected = (limits["width"], limits["height"])
            if image.dimensions != expected:
                problems.append(
                    f"dimensions {image.width}x{image.height}, expected "
                    f"{expected[0]}x{expected[1]}"
                )
            if image.has_alpha is not False:
                problems.append("carries an alpha channel, but a 24-bit PNG without alpha is required")
        report.add(
            "feature-graphic",
            asset.relpath,
            PASS if not problems else FAIL,
            "; ".join(problems)
            if problems
            else f"{image.width}x{image.height} {image.fmt} opaque, {image.size_bytes} bytes",
        )


def check_screenshot_windows(report: Report, slot: Path, locales: Sequence[str]) -> None:
    for asset in model.composed_assets(locales):
        if not asset.is_screenshot:
            continue
        limits = model.screenshot_limits(asset.kind)
        image = probe_image(slot / asset.relpath)
        problems: list[str] = []
        if not image.exists:
            problems.append("absent")
        elif image.dimensions is None:
            problems.append(f"dimensions unreadable from a {image.fmt} file")
        else:
            width, height = image.dimensions
            short_side, long_side = min(width, height), max(width, height)
            if image.size_bytes > limits["maxBytes"]:
                problems.append(
                    f"{image.size_bytes} bytes exceeds the {limits['maxBytes']}-byte cap"
                )
            if short_side < limits["minShortSide"]:
                problems.append(
                    f"short side {short_side}px is below the {limits['minShortSide']}px minimum"
                )
            if long_side > limits["maxLongSide"]:
                problems.append(
                    f"long side {long_side}px exceeds the {limits['maxLongSide']}px maximum"
                )
            ratio = limits["maxRatio"]
            if ratio is not None and long_side > ratio * short_side:
                problems.append(
                    f"long side {long_side}px is more than {ratio}x the short side {short_side}px"
                )
        report.add(
            "screenshot:window",
            asset.relpath,
            PASS if not problems else FAIL,
            "; ".join(problems)
            if problems
            else f"{image.width}x{image.height} inside the {asset.kind} window, {image.size_bytes} bytes",
        )


def check_screenshot_aspect(report: Report, slot: Path, locales: Sequence[str]) -> None:
    for asset in model.composed_assets(locales):
        if not asset.is_screenshot:
            continue
        image = probe_image(slot / asset.relpath)
        if not image.exists or image.dimensions is None:
            report.add(
                "screenshot:aspect",
                asset.relpath,
                FAIL,
                f"cannot read dimensions from a {image.fmt} file, so the aspect rule is unmet",
            )
            continue
        width, height = image.dimensions
        if allowed_aspect(width, height):
            report.add("screenshot:aspect", asset.relpath, PASS, f"{width}x{height} is {aspect_text()}")
        else:
            report.add(
                "screenshot:aspect",
                asset.relpath,
                FAIL,
                f"{width}x{height} is not {aspect_text()}",
            )


def check_pixels(report: Report, slot: Path, locales: Sequence[str]) -> None:
    """Every composed image must carry structure; screenshots must additionally carry colour."""
    luminance_floor = model.NON_DEGENERATE["minLuminanceStdDev"]
    colour_floor = model.NON_DEGENERATE["minScreenshotColours"]
    for asset in model.composed_assets(locales):
        path = slot / asset.relpath
        if not path.is_file():
            report.add("pixels", asset.relpath, FAIL, "absent, so its pixels cannot be measured")
            continue
        fmt = sniff_format(read_head(slot / asset.relpath))
        if fmt != "png":
            # The dependency-free decoder reads PNG only. A JPEG feature graphic is a legal asset
            # whose structure this measurement cannot see, so the honest verdict is UNVERIFIED --
            # not a pass, and not a failure of a file that no rule forbids.
            report.add(
                "pixels",
                asset.relpath,
                UNVERIFIED,
                f"a {fmt} file cannot be measured by the dependency-free PNG decoder, so the "
                f"non-degenerate floor is unconfirmed",
            )
            continue
        try:
            stats = png_content_stats(path)
        except PngDecodeError as error:
            report.add(
                "pixels",
                asset.relpath,
                FAIL,
                f"undecodable PNG ({error}); a PNG that cannot be measured is a failure, never a pass",
            )
            continue

        problems: list[str] = []
        if stats.luminance_std_dev < luminance_floor:
            problems.append(
                f"luminance standard deviation {stats.luminance_std_dev:.2f} is below the "
                f"{luminance_floor} floor"
            )
        if asset.is_screenshot and stats.distinct_colours < colour_floor:
            problems.append(
                f"{stats.distinct_colours} distinct quantised colours is below the {colour_floor} floor"
            )
        if problems:
            report.add("pixels", asset.relpath, FAIL, "; ".join(problems))
        else:
            detail = f"luminance sd {stats.luminance_std_dev:.2f}, {stats.distinct_colours} colours"
            if not asset.is_screenshot:
                detail += " (colour floor does not apply to this kind)"
            report.add("pixels", asset.relpath, PASS, detail)


def check_listing_text(report: Report, listing: model.Listing) -> None:
    limits = model.LISTING_LIMITS
    for locale in listing.declared_locales():
        text = listing.locales.get(locale)
        if text is None:
            report.add(
                "listing:text", f"{locale}", FAIL, "the locale is declared but carries no text block"
            )
            continue
        for field in ("title", "shortDescription", "fullDescription"):
            limit = limits[field]
            value = text.field(field)
            if not value.strip():
                report.add(
                    "listing:text",
                    f"{locale}.{field}",
                    FAIL,
                    "empty after trimming, but the field is required",
                )
                continue
            # `len()` on a `str` counts code points, which is how Play counts these fields.
            length = len(value)
            report.add(
                "listing:text",
                f"{locale}.{field}",
                PASS if length <= limit else FAIL,
                f"{length}/{limit} code points"
                if length <= limit
                else f"{length} code points exceeds the {limit} code point limit",
            )
            # Titles are names; unfinished-product tone is judged on the descriptions users read.
            if field in ("shortDescription", "fullDescription"):
                tone = model.banned_store_copy_tone(value)
                report.add(
                    "listing:tone",
                    f"{locale}.{field}",
                    FAIL if tone else PASS,
                    tone
                    or "finished-product tone (no early/WIP/roadmap-apology language)",
                )
    if not listing.declared_locales():
        report.add(
            "listing:text",
            "listing",
            UNVERIFIED,
            "no locale is declared in store/listing.json, so no field could be measured",
        )


_FENCED_BLOCK = re.compile(r"^```[^\n]*\n(.*?)^```", re.MULTILINE | re.DOTALL)
_LOCALE_TAG = re.compile(r"^<([A-Za-z][A-Za-z0-9-]*)>[ \t]*$\n(.*?)^</\1>[ \t]*$", re.MULTILINE | re.DOTALL)


def parse_release_notes(path: Path) -> dict[str, str]:
    """`{locale: note}` from the fenced paste block, the way Play reads the field.

    Play's release screen takes every language in one text box, separated by locale tags that
    occupy their own lines.  The block is the paste rather than a rendering of it, so the string
    measured here and the string a human pastes are the same characters.
    """
    fenced = _FENCED_BLOCK.search(path.read_text(encoding="utf-8-sig"))
    if not fenced:
        return {}
    return {
        locale: body.strip()
        for locale, body in _LOCALE_TAG.findall(fenced.group(1))
    }


def check_release_notes(report: Report, slot: Path) -> None:
    path = model.store_dir(slot) / "release-notes.md"
    if not path.is_file():
        report.add("release-notes", "store/release-notes.md", SKIP, "the pack ships no release notes")
        return
    notes = parse_release_notes(path)
    if not notes:
        report.add(
            "release-notes:block",
            "store/release-notes.md",
            FAIL,
            "no fenced block of `<locale>`-tagged notes was found, tags on their own lines",
        )
        return
    report.add(
        "release-notes:block",
        "store/release-notes.md",
        PASS,
        f"{len(notes)} locale tag(s): {', '.join(sorted(notes))}",
    )
    limit = model.RELEASE_NOTE["maxChars"]
    for locale, note in sorted(notes.items()):
        # Every tag in the block is measured, including a locale the listing does not declare:
        # text in the block is text a human may paste, and Play truncates it silently.
        length = len(note)
        report.add(
            "release-notes:note",
            locale,
            PASS if length <= limit else FAIL,
            f"{length}/{limit} code points"
            if length <= limit
            else f"the {locale} note is {length} code points, over the {limit} code point limit",
        )
        tone = model.banned_store_copy_tone(note)
        report.add(
            "release-notes:tone",
            locale,
            FAIL if tone else PASS,
            tone
            or "finished-product tone (no early/WIP/roadmap-apology language)",
        )


def is_placeholder_host(host: str) -> bool:
    """True for a reserved documentation host, or no host at all (mirrors the retired `store-kit.py`)."""
    host = (host or "").lower().strip(".")
    return not host or any(
        host == entry or host.endswith(f".{entry}") for entry in model.PLACEHOLDER_HOSTS
    )


def unfinished_url_reason(value: str) -> str | None:
    """The reason a declared URL is unfinished, or ``None`` when it is a usable https/http URL."""
    parsed = urlparse(value if "//" in value else f"//{value}")
    if parsed.scheme not in ("http", "https"):
        return f"scheme {parsed.scheme or '(none)'} is not http or https"
    if is_placeholder_host(parsed.hostname or ""):
        return f"host {parsed.hostname or '(none)'} is a reserved documentation host"
    return None


def check_privacy_policy(report: Report, listing: model.Listing) -> None:
    """The privacy-policy URL, as a FAIL rather than the generic placeholder roll-up's softer note.

    This is deliberately a **FAIL**, not the ``UNFINISHED`` verdict `check_placeholder_hosts`
    prints for an unfinished URL, and the reasoning that made ``example.com/privacy`` merely
    unfinished has changed rather than the label merely tightened.  ``UNFINISHED`` is the pack
    contract's word for a value the owner still has to supply; that was right while the factory had
    no host, so the placeholder was honest pending work.  The factory now publishes the policy
    itself -- `node scripts/publish-privacy.mjs --slot <slug>` writes `store/privacy.md` to a public
    HTTPS URL and records it here -- so a reserved-host or absent `privacyPolicyUrl` is no longer a
    task waiting on a person.  It is a pack the pipeline did not finish, Play refuses to publish
    without a URL anyone can open, and the strongest blocking label is the truthful one.

    Two verdicts about one field must not disagree: the Node core's ``pack.placeholders`` check
    (`checkPlaceholderHosts` in `scripts/core/assets.mjs`) already FAILs both an empty and a
    reserved-host `privacyPolicyUrl`, so a read path that called the same pack merely unfinished
    would be the drift `.cursor/rules/verification-discipline.mdc` forbids.  What this measures is
    the **value** -- a real host passes even if no command was ever typed, and a reserved host fails
    even if one was -- never whether the publisher ran.
    """
    value = listing.raw.get("privacyPolicyUrl")
    slug = listing.raw.get("slug")
    remedy = (
        f"node scripts/publish-privacy.mjs --slot {slug}"
        if isinstance(slug, str) and slug.strip()
        else "node scripts/publish-privacy.mjs --slot <slug>"
    )
    if not isinstance(value, str) or not value.strip():
        report.add(
            "privacy-policy",
            "privacyPolicyUrl",
            FAIL,
            "the listing declares no privacy policy URL, and Play requires a public HTTPS one; "
            f"run `{remedy}` to publish store/privacy.md and record it",
        )
        return
    reason = unfinished_url_reason(value)
    if reason is None:
        report.add(
            "privacy-policy",
            "privacyPolicyUrl",
            PASS,
            f"{value} is a real host, not a reserved documentation host",
        )
        return
    report.add(
        "privacy-policy",
        "privacyPolicyUrl",
        FAIL,
        f"{reason} ({value}); run `{remedy}` to publish store/privacy.md and record the real URL",
    )


def check_placeholder_hosts(report: Report, listing: model.Listing) -> None:
    """Every other declared URL, and the contact email, against the reserved documentation hosts.

    `privacyPolicyUrl` is deliberately measured by `check_privacy_policy` instead of here: it is a
    Play-required field and is reported as a `FAIL`, while these rows keep the softer `UNFINISHED`
    verdict because another URL field may be optional or still being filled in.  The roll-up below
    therefore counts only the fields this function measured, and says so.
    """
    problems = 0
    hosts = ", ".join(model.PLACEHOLDER_HOSTS)
    other_fields = [
        (field, value) for field, value in listing.url_fields() if field != "privacyPolicyUrl"
    ]
    for field, value in other_fields:
        reason = unfinished_url_reason(value)
        if reason is None:
            continue
        problems += 1
        report.add("placeholder-host", field, UNFINISHED, f"{reason} ({value})")

    email = listing.contact_email
    email_host = email.rpartition("@")[2]
    if is_placeholder_host(email_host):
        problems += 1
        report.add(
            "placeholder-host",
            "contactEmail",
            UNFINISHED,
            f"{email} uses the reserved documentation host {email_host} ({hosts})",
        )
    else:
        source = (
            "default applied because listing.json declares no contactEmail"
            if listing.contact_email_is_default
            else "declared in listing.json"
        )
        report.add("placeholder-host", "contactEmail", PASS, f"{email} ({source})")

    report.add(
        "placeholder-host",
        "declared URLs",
        PASS if problems == 0 else UNFINISHED,
        f"{len(other_fields)} URL field(s) besides privacyPolicyUrl checked against {hosts}",
    )


def sha256_of(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def check_provenance(report: Report, slot: Path, locales: Sequence[str]) -> None:
    record = model.load_provenance(slot)
    # The trigger is composed media on disk (``model.composes_media``), never the
    # ``store/studio.json`` declaration: the declaration is phase 1 and the record is phase 2, so
    # keying on it demanded a phase-2 artifact of every slot in development.  The Node counterpart
    # is ``hasComposedMedia`` in ``scripts/core/assets.mjs``.
    required = model.composes_media(slot)
    if record is None:
        if required:
            report.add(
                "provenance",
                "store/provenance.json",
                FAIL,
                "the pack ships composed media under store/assets/ but ships no record binding its "
                "committed assets to a build; run node scripts/media-capture.mjs --slot <slug>",
            )
        else:
            report.add(
                "provenance",
                "store/provenance.json",
                SKIP,
                "absent, and no composed media under store/assets to attest",
            )
        return

    # Counted rather than assumed: the roll-up row below is a PASS only when nothing in this check
    # failed, so a failing asset can never sit beside a green summary.
    problems = 0

    for entry in record.composed_assets:
        if not entry.file:
            problems += 1
            report.add("provenance", "store/provenance.json", FAIL, "an entry names no file")
            continue
        path = slot / entry.file
        if not path.is_file():
            problems += 1
            report.add(
                "provenance", entry.file, FAIL, "named in provenance.json but missing from the pack"
            )
            continue
        actual = sha256_of(path)
        if actual != entry.sha256:
            problems += 1
            report.add(
                "provenance",
                entry.file,
                FAIL,
                f"hashes to {actual[:12]}... but provenance.json records {entry.sha256[:12] or '(nothing)'}...",
            )
        else:
            report.add("provenance", entry.file, PASS, f"sha256 {actual[:12]}... as recorded")

    # Raw captures live under the ignored `captures/` tree, so their absence is expected on a clean
    # checkout; a mismatch on one that is present is still a finding about the bytes.
    for entry in record.raw_captures:
        path = slot / entry.file if entry.file else None
        if path is None or not path.is_file():
            continue
        actual = sha256_of(path)
        if actual != entry.sha256:
            problems += 1
            report.add(
                "provenance",
                entry.file,
                FAIL,
                f"raw capture hashes to {actual[:12]}... but provenance.json records "
                f"{entry.sha256[:12] or '(nothing)'}...",
            )

    # The record's own hashes only prove it agrees with itself; the entry list is trimmable, so it
    # must also cover exactly the assets the pack is required to ship, in both directions.
    expected = set(model.required_assets(locales))
    recorded = {entry.file for entry in record.composed_assets}
    for rel in sorted(expected - recorded):
        problems += 1
        report.add(
            "provenance", rel, FAIL, "a required asset that provenance.json does not record"
        )
    for rel in sorted(recorded - expected):
        problems += 1
        report.add(
            "provenance", rel, FAIL, "recorded in provenance.json but not part of the expected pack"
        )

    report.add(
        "provenance",
        f"{len(record.composed_assets)} composed asset(s)",
        PASS if problems == 0 else FAIL,
        "every recorded digest matches the committed bytes and the inventory is covered"
        if problems == 0
        else f"{problems} problem(s) reported above",
    )


# --------------------------------------------------------------------------------------------
# CLI
# --------------------------------------------------------------------------------------------


def parse_args(argv: Sequence[str] | None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Validate a Play store pack. Reads only; writes nothing."
    )
    target = parser.add_mutually_exclusive_group(required=True)
    target.add_argument("--slot", help="Slot slug resolved beneath apps/.")
    target.add_argument(
        "--slot-dir", type=Path, help="Explicit slot directory (absolute or relative to cwd)."
    )
    return parser.parse_args(argv)


def resolve_slot(args: argparse.Namespace) -> Path:
    if args.slot_dir is not None:
        slot = args.slot_dir.expanduser()
        slot = (Path.cwd() / slot).resolve() if not slot.is_absolute() else slot.resolve()
        if not slot.is_dir():
            raise model.PackModelError(f"slot directory does not exist: {slot}")
        return slot
    assert args.slot is not None
    return model.find_slot(args.slot).resolve()


def run(args: argparse.Namespace) -> int:
    try:
        slot = resolve_slot(args)
    except model.PackModelError as error:
        print(f"ERROR slot - {error}", file=sys.stderr)
        return 2
    if not model.pack_exists(slot):
        print(f"ERROR pack - {model.store_dir(slot)} does not exist", file=sys.stderr)
        return 2

    print(f"pack       {slot}")
    print(f"limits     {model.PLAY_ASSETS_PATH} (measured {model.PLAY_ASSETS.get('_measuredAt')})")
    print("writes     nothing")
    print()

    report = Report()
    try:
        listing: model.Listing | None = model.load_listing(slot)
        listing_error = ""
    except model.PackModelError as error:
        listing, listing_error = None, str(error)
    locales = listing.declared_locales() if listing is not None else ()

    if listing is None:
        report.add("listing", "store/listing.json", FAIL, listing_error)

    check_icon(report, slot, locales)
    check_feature_graphic(report, slot, locales)

    if locales:
        check_inventory(report, slot, locales)
        check_screenshot_windows(report, slot, locales)
        check_screenshot_aspect(report, slot, locales)
        check_pixels(report, slot, locales)
    else:
        reason = listing_error or "store/listing.json declares no locale"
        for check in ("inventory", "screenshot:window", "screenshot:aspect", "pixels"):
            report.add(check, "pack", UNVERIFIED, f"the pack's locale set is unknown: {reason}")

    if listing is not None:
        check_listing_text(report, listing)
        check_placeholder_hosts(report, listing)
        check_privacy_policy(report, listing)
    else:
        report.add("listing:text", "pack", UNVERIFIED, listing_error)
        report.add("placeholder-host", "pack", UNVERIFIED, listing_error)
        report.add("privacy-policy", "pack", UNVERIFIED, listing_error)

    check_release_notes(report, slot)

    if locales:
        check_provenance(report, slot, locales)
    else:
        report.add(
            "provenance",
            "pack",
            UNVERIFIED,
            f"the pack's locale set is unknown: {listing_error or 'no locale declared'}",
        )

    counts = Counter(row.verdict for row in report.rows)
    blocking = [row for row in report.rows if row.verdict in BLOCKING]
    print()
    print(
        "checks: "
        + ", ".join(
            f"{verdict} {counts.get(verdict, 0)}"
            for verdict in (PASS, FAIL, UNFINISHED, SKIP, UNVERIFIED)
        )
    )
    for label, verdict in (("FAIL", FAIL), ("UNFINISHED", UNFINISHED)):
        named = [row for row in report.rows if row.verdict == verdict]
        if named:
            print(f"{label} rows ({len(named)}):")
            for row in named:
                print(f"  {row.check} {row.subject} - {row.detail}" if row.detail else f"  {row.check} {row.subject}")

    if blocking:
        print(f"RESULT: not shippable ({len(blocking)} blocking row(s)) - exit 1")
        return 1
    if counts.get(UNVERIFIED, 0):
        print("RESULT: UNVERIFIED (a check could not be evaluated) - exit 2")
        return 2
    print("RESULT: every check passed - exit 0")
    return 0


def main(argv: Sequence[str] | None = None) -> int:
    return run(parse_args(argv))


if __name__ == "__main__":
    sys.exit(main())
