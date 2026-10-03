#!/usr/bin/env python3
"""PlayKiln store studio - the composed visual identity for a slot's store presence.

The generic renderer (`scripts/store/render.py`) draws a gradient with the device screen pasted into a
rounded rectangle. That is honest but plain: it is a screencap with a caption, and a store
listing made of screencaps does not attract anyone. This module renders the assets the way a
modern Play listing does it - a branded composition with a benefit-led headline, a real device
mock-up, and a mark that survives being shrunk to a 48dp launcher icon.

It is deterministic and re-runnable, and it reads only from the slot: the raw captures under
`store/captures/raw/`, the shot copy in `store/journey.json`, and the palette in
`store/studio.json`. Nothing here invents UI - the screen inside every mock-up is the capture the
app actually produced.

The mark itself is not drawn here. `scripts/lib/icon_design.py` owns the construction grid, the
stroke scale and the primitives, and renders them twice - as pixels for the 512px listing icon and
as Android path data for the launcher - while `scripts/lib/icon_qa.py` measures the result and
`render_icon` refuses to return an icon that fails a gated measurement. Both the launcher icon and
the store icon therefore come out of one geometry, which is the property this module lost when the
mark was hand-nudged pixel constants and the raster and the vector scaled it by two different
numbers.

    python scripts/store-studio.py --slot pipe-sizing
    python scripts/store-studio.py --slot pipe-sizing --preview-circle
"""

from __future__ import annotations

import argparse
import importlib.util
import json
import math
import re
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

# The mark's construction grid, stroke scale and primitives live in the design system, and the
# measurements that decide whether the rendered mark is a designed mark live beside it. Both are
# imported rather than restated: a second copy of the keyline is a second thing to drift.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from lib import colours  # noqa: E402  the one colour-maths implementation, shared with visual-qa
from lib import icon_design  # noqa: E402
from lib import icon_qa  # noqa: E402

# The colour maths was a byte-for-byte copy in this module until 2026-10-02; `scripts/lib/colours.py`
# now owns `hex_of`/`parse_hex` and this module imports them ("one source of truth, imported - never
# restated", `.cursor/rules/verification-discipline.mdc`). The name stays a module-level *alias*
# because `scripts/test-icon-design.py` reads `studio.hex_of` directly: the name survives, the second
# implementation does not.
hex_of = colours.hex_of

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

# `lane` is deliberately not one of the `PALETTE_DEFAULTS` roles: it is a *domain* colour a slot may
# declare (the lit water under the launcher field's waterline), not a role every pack draws with, so
# a slot that omits it is not painted with another app's colour and is not reported as inheriting.
# `None` is the honest state "this slot declared no such role", and it is what withholds
# `icon_design.FIELD_OVERLAYS` from a palette that cannot colour it - a partial waterline in an
# undeclared colour is a third picture, not a cheaper one.
LANE = None

# The reference slot's own artwork, kept as the historic fallback only. `apply_palette` overwrites
# the module globals above with the slot being rendered, so these constants are never *the* palette
# of a pack; they are what `resolve_palette` reaches for when a slot omits a role, which is a
# different app's colours. That fallback is now reported, and on a slot the baseline gates refused
# (`.cursor/rules/visual-identity.mdc`: two slots never share a palette).
PALETTE_DEFAULTS = {
    "ink": INK, "ink_soft": INK_SOFT, "ink_lift": INK_LIFT,
    "amber": AMBER, "amber_deep": AMBER_DEEP, "amber_pale": AMBER_PALE,
    "accent_deep": CYAN_DEEP, "paper": PAPER, "muted": MUTED,
    "bezel": BEZEL, "bezel_rim": BEZEL_RIM,
}

# `FACTORY_BASELINE` is parsed from its one definition rather than copied: a second literal date
# here would be the "one source of truth, never restated" defect, and the two could drift apart.
_FACTORY_BASELINE_RE = re.compile(r"FACTORY_BASELINE\s*=\s*'(\d{4}-\d{2}-\d{2})'")


def factory_baseline() -> str:
    """The date a factory baseline starts gating newly created slots, from `core/baseline.mjs`."""
    path = Path(__file__).resolve().parent / "core" / "baseline.mjs"
    match = _FACTORY_BASELINE_RE.search(path.read_text(encoding="utf-8"))
    if match is None:
        raise SystemExit(f"BLOCKED: cannot read FACTORY_BASELINE from {path}")
    return match.group(1)


def _registry_entry(slot: Path) -> dict | None:
    """The registry row for a slot, matched by `dir` exactly as `core/baseline.mjs` matches it."""
    root = Path(__file__).resolve().parent.parent
    try:
        relative = slot.resolve().relative_to(root).as_posix()
    except ValueError:
        return None
    registry = root / "registry" / "apps.json"
    if not registry.is_file():
        return None
    apps = json.loads(registry.read_text(encoding="utf-8")).get("apps")
    if not isinstance(apps, list):
        return None
    return next((app for app in apps if app.get("dir") == relative), None)


def palette_baseline_applies(slot: Path | None) -> tuple[bool, str]:
    """Whether the palette-completeness baseline gates this slot, and the reason either way.

    This mirrors `scripts/core/baseline.mjs` rather than inventing a second date rule: a proof slot
    (no `economics.json`), a killed slot and a slot created before `FACTORY_BASELINE` are exempt and
    carry the finding as a note; a slot born into the baseline is gated.
    """
    if slot is None:
        return False, "the caller did not name the slot"
    if not (slot / "economics.json").is_file():
        return False, "not a product slot: it declares no economics.json, so it is a pipeline proof"
    entry = _registry_entry(slot)
    if entry is not None and entry.get("status") == "killed":
        return False, "the slot is killed; its evidence is archived rather than gated"
    created_at = entry.get("createdAt") if isinstance(entry, dict) else None
    created_at = created_at[:10] if isinstance(created_at, str) else None
    if not created_at:
        return True, ""
    baseline = factory_baseline()
    if created_at < baseline:
        return False, f"created {created_at}, before the {baseline} baseline"
    return True, ""


def resolve_palette(
    values: dict | None,
    slot: Path | None = None,
    *,
    applies: bool | None = None,
) -> dict[str, tuple[int, int, int]]:
    """Resolve a slot's declared palette, refusing a silent inheritance on a gated slot.

    `PALETTE_DEFAULTS` are the reference slot's artwork, not a neutral fallback, so a slot that
    omits a role is otherwise painted in a different app's colours with nothing to say so - the
    shared-identity failure `.cursor/rules/visual-identity.mdc` forbids. A slot the baseline gates
    is refused; a slot that predates the rule keeps the defaults but is told, in the output, which
    roles it inherited and from where. `slot` supplies the identity the applicability rule reads;
    `applies` supplies an already-evaluated answer when the caller has one.
    """
    declared = values or {}
    inherited = [role for role in PALETTE_DEFAULTS if role not in declared]
    if inherited:
        if applies is None:
            applies, reason = palette_baseline_applies(slot)
        else:
            reason = "applicability supplied by the caller"
        if applies and slot is not None:
            raise SystemExit(
                "BLOCKED: "
                + slot.as_posix()
                + " declares no palette value for role(s) "
                + ", ".join(inherited)
                + " in store/studio.json, and it is a slot the "
                + factory_baseline()
                + " baseline gates. Every palette role must be declared, or the omitted role is "
                "silently painted with the historic reference palette in scripts/store-studio.py "
                "- another app's colours, which .cursor/rules/visual-identity.mdc forbids."
            )
        print(
            "NOTE studio - "
            + (f"{slot.as_posix()}: " if slot is not None else "")
            + "palette role(s) "
            + ", ".join(inherited)
            + " are inherited from the historic reference palette in scripts/store-studio.py "
            "(the reference slot's own artwork colours)"
            + (f"; {reason}" if reason else "")
            + ". Declare them in store/studio.json so the pack is painted in this slot's colours."
        )
    resolved = dict(PALETTE_DEFAULTS)
    for role, value in declared.items():
        # Every declared role is kept, not only the ones `PALETTE_DEFAULTS` happens to name. The
        # previous version skipped the rest, so a slot's own domain colour - `lane`, the waterline's
        # lit water - was silently dropped by the resolver and could not reach `write_vectors`, which
        # is how the launcher field and the store icon came to be two different pictures.
        try:
            resolved[role] = colours.parse_hex(value)
        except ValueError:
            raise SystemExit(
                f"BLOCKED: palette role '{role}' must be a #RRGGBB colour, got {value!r}"
            ) from None
    return resolved


def apply_palette(palette: dict[str, tuple[int, int, int]]) -> None:
    """Install one slot's resolved palette for the single-process render pipeline."""
    global INK, INK_SOFT, INK_LIFT, AMBER, AMBER_DEEP, AMBER_PALE
    global CYAN_DEEP, PAPER, MUTED, BEZEL, BEZEL_RIM, LANE
    INK, INK_SOFT, INK_LIFT = palette["ink"], palette["ink_soft"], palette["ink_lift"]
    AMBER, AMBER_DEEP, AMBER_PALE = palette["amber"], palette["amber_deep"], palette["amber_pale"]
    CYAN_DEEP, PAPER, MUTED = palette["accent_deep"], palette["paper"], palette["muted"]
    BEZEL, BEZEL_RIM = palette["bezel"], palette["bezel_rim"]
    # A slot that declares no `lane` gets `None`, not a default: the field's waterline is drawn only
    # in a colour the slot chose (see the `LANE` definition above).
    LANE = palette.get("lane")

def _load_pack_model():
    """The screenshot inventory, imported from the pack's model rather than restated here.

    `scripts/store/model.py` is the pack's single limit resolver: it loads every window and byte cap
    from `scripts/lib/play-assets.json`, the file `scripts/lib/play-assets.mjs` reads too, and it is
    what both the renderer and the validator import. A second literal copy of the sizes here could
    only drift: two lists of sizes are two answers to one question, and the copy no gate reads is
    the one that silently keeps composing the wrong canvases. The store directory is not a package,
    so the module is loaded by path - and an already-loaded copy is reused, because
    `scripts/store/render.py` loads the same file and executing it twice would build a second,
    silently divergent module state.
    """
    path = (Path(__file__).resolve().parent / "store" / "model.py").resolve()
    for module in list(sys.modules.values()):
        module_file = getattr(module, "__file__", None)
        if module_file and Path(module_file).resolve() == path:
            return module
    spec = importlib.util.spec_from_file_location("pack_model", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path} for the screenshot inventory")
    module = importlib.util.module_from_spec(spec)
    # Register before executing: `model.py` declares frozen dataclasses, and a dataclass whose
    # module is absent from `sys.modules` cannot be created on Python 3.13 — `dataclasses._is_type`
    # does `sys.modules.get(cls.__module__).__dict__` for a `KW_ONLY` field and raises
    # `AttributeError: 'NoneType' object has no attribute '__dict__'`. Registering also makes the
    # `__file__` de-duplication above find this module on a second call instead of loading a copy.
    sys.modules.setdefault("pack_model", module)
    try:
        spec.loader.exec_module(module)
    except BaseException:
        sys.modules.pop("pack_model", None)
        raise
    return module


_MODEL = _load_pack_model()
SCREEN_TARGETS = _MODEL.screenshot_targets()
SCREENSHOT_LIMITS = _MODEL.screenshot_windows()

# One raw capture per journey shot. The studio composes every screenshot target from exactly these,
# so a missing one is a hard stop rather than a target to skip.
REQUIRED_RAW_CAPTURES = 6
# How many files a run writes, as a function of the declared locale count: two shared assets at the
# pack root, plus one full set per locale - that locale's icon, its feature graphic and, unless this
# is an identity-only run, the complete screenshot inventory. Derived from the same `SCREEN_TARGETS`
# the render loop walks, so the count and the render cannot drift and a partial pack still refuses to
# write.
def expected_composed_assets(locale_count: int, *, identity_only: bool) -> int:
    per_locale = 2 if identity_only else 2 + sum(count for count, _ in SCREEN_TARGETS.values())
    return 2 + per_locale * max(locale_count, 0)


# The one-locale total, kept as a named constant because `scripts/test-store-studio-completeness.py`
# reads it; a run over a multi-locale slot derives its own count from the function above.
EXPECTED_COMPOSED_ASSETS = expected_composed_assets(1, identity_only=False)

# A capture that is (near) uniform cannot be a photograph of this app's UI. Every gate in the# factory reads dimensions, mode and byte size, which accept a solid colour rectangle of the right
# size - and one was accepted as `en-US/phone-01.png` by both `store-kit --validate-only` and
# `verify.mjs`. The thresholds sit far below any real screen (the six committed
# captures carry 1101-1395 distinct colours with a luminance spread of 252), so they reject a blank
# or solid frame without putting a real one at risk.
MIN_CAPTURE_COLOURS = 64
MIN_CAPTURE_LUMA_SPREAD = 64

# Play crops the feature graphic's outer 10% on some surfaces, so no important element may sit
# there. `render_feature` records what it places and `assert_feature_safe_area` enforces it.
FEATURE_SAFE_FRACTION = 0.10

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
# Three marks are registered: `pipe` is a section through a bore with a dimension line across it,
# `mark-book` is a bound, marked page, and `mark-stripes` is the mown-stripe fan (four strokes
# converging toward a vanishing point). All three are built on the shared keyline system from a
# small set of primitives, and each is rendered twice from that one description - as pixels for the
# listing icon and as Android path data for the launcher - so the two cannot drift apart.
# The registry is the single source of truth for this list; nothing else enumerates the marks.
# --------------------------------------------------------------------------------------


# The mark, the construction grid and the stroke scale live in `scripts/lib/icon_design.py`, and
# the measurements that say whether the result is a designed mark live in `scripts/lib/icon_qa.py`.
#
# They are not here, and that is the fix for this module's worst defect. The geometry used to be
# per-mark absolute pixel constants (`MARK_SHAPE`, `BOOK_SHAPE`) nudged by hand, each output had its
# own scale factor (`MARK_FIT_DP` for the vector, `MARK_HALF_EXTENT_FRACTION` for the raster), and
# nothing checked either of them: the owner's verdict on the shipped icon was "bad and crooked, a
# naive silly thing, not a designed mark". The mark is now built on one declared keyline system and
# rendered by two renderers that read the same primitive list.
#
# Because this module renders the assets, this is also where the icon gate runs: `render_icon`
# refuses to write an icon that fails a gated measurement. numpy and scipy arrive with that import,
# as they already did for `scripts/visual-qa.py`'s icon section.

ADAPTIVE_VIEWPORT_DP = icon_design.ADAPTIVE_VIEWPORT_DP
ADAPTIVE_SAFE_RADIUS_DP = icon_design.ADAPTIVE_SAFE_RADIUS_DP
# The launcher's *visible* mask: 72dp across inside the 108dp viewport. A preview that masks the
# full 108dp canvas answers a question no launcher asks; see `adaptive_previews()`.
ADAPTIVE_VISIBLE_RADIUS_DP = icon_design.ADAPTIVE_VISIBLE_DP / 2

CURRENT_MARK = icon_design.DEFAULT_MARK


def resolve_mark(studio: dict) -> str:
    """The slot's mark, validated against the design system's registry."""
    mark = studio.get("mark") or icon_design.DEFAULT_MARK
    icon_design.mark_primitives(mark)  # raises on an unknown mark
    return mark


def mark_reach_dp(mark: str | None = None) -> float:
    """How far the mark reaches from its optical anchor, in adaptive-viewport dp."""
    return icon_design.mark_reach_dp(mark or CURRENT_MARK)


def render_icon(listing: dict, studio: dict) -> Image.Image:
    """512x512, full bleed and opaque: Play rejects a masked or transparent icon.

    The listing icon *is* the adaptive icon flattened: 512px of the 108dp layer, so the store
    artwork and the launcher artwork are the same mark at the same size, drawn from the same
    primitives. There is no second composition to keep in step.

    The field is the one the adaptive background drawable declares - the same ramp at the same angle
    plus `icon_design.FIELD_OVERLAYS`, the declared waterline - with no drafting grid and no radial
    blooms. Measured on the icon the owner rejected: the 32px grid and the two glows left 1.40 luma
    of high-frequency energy in the field and 2.49 luma of curvature against a best-fit plane, and at
    48dp that is dirt.

    The overlays are handed to `icon_field` through the slot's own palette, so the 512px listing
    icon and the launcher background are one field rather than two blue squares that resemble each
    other: the launcher used to draw the waterline while this icon drew the bare ramp, and the
    studio's own `write_vectors` flattened the drawable to match the ramp - three answers to one
    question. This is the single consumer that rasterises the store icon, and it now consumes the
    one declaration.

    Every rendered icon is measured before it is returned, and an icon that fails a gated
    measurement stops the render with the failing legs printed: this is where the threshold gates.
    """
    mark = CURRENT_MARK
    palette = {role: colour for role, colour in _palette_roles()}
    field = icon_design.icon_field(icon_design.CANVAS, INK, INK_LIFT, palette=palette)
    image = field.convert("RGBA")
    image.alpha_composite(icon_design.render_mark(icon_design.CANVAS, mark, palette))
    array = np.asarray(image, dtype=np.uint8)
    result = icon_qa.evaluate_icon(array, palette, mark=mark,
                                  field=np.asarray(field.convert("RGB"), dtype=np.float64))
    _print_icon_legs(mark, result, label="icon-512")
    if result["verdict"] != "PASS":
        raise SystemExit(
            f"BLOCKED: the icon for mark {mark!r} failed "
            f"{len(result['failed_legs'])} measurement(s): {', '.join(result['failed_legs'])}. "
            "An icon that does not measure as a designed mark is not written into the pack; fix "
            "the mark's geometry in scripts/lib/icon_design.py - its keyline, its stroke scale, its "
            "optical centre - rather than the threshold."
        )
    return image


def _palette_roles() -> list[tuple[str, tuple[int, int, int]]]:
    """The resolved palette as (role, colour), which is what the design system draws with.

    `lane` appears only when the slot declared it. It is what tells `icon_design.field_overlays`
    whether this palette can colour the field's waterline at all: a slot without it keeps the bare
    ramp it was designed with rather than a repainted one.
    """
    roles = [("ink", INK), ("ink_soft", INK_SOFT), ("ink_lift", INK_LIFT),
             ("amber", AMBER), ("amber_deep", AMBER_DEEP), ("amber_pale", AMBER_PALE),
             ("accent_deep", CYAN_DEEP), ("paper", PAPER), ("muted", MUTED),
             ("bezel", BEZEL), ("bezel_rim", BEZEL_RIM)]
    if LANE is not None:
        roles.append(("lane", LANE))
    return roles


def _print_icon_legs(mark: str, result: dict, *, label: str) -> None:
    """Print the icon's measurements once per process, then stay quiet.

    `render_icon` runs three times per pack (the icon, and once per feature graphic), and the same
    inputs produce byte-identical output by construction, so repeating the table three times only
    makes the log harder to read. The verdict is recomputed every time either way - the FAIL below
    is not cached, only the printing of a PASS is.
    """
    key = (mark, tuple(sorted(_palette_roles())))
    if _REPORTED_ICONS.get(key):
        return
    _REPORTED_ICONS[key] = True
    print(f"PASS studio - {label} measurements (mark {mark}): verdict {result['verdict']}")
    for line in icon_qa.format_legs(result):
        print(f"  studio{line}")


_REPORTED_ICONS: dict[tuple, bool] = {}


def render_feature(listing: dict, studio: dict, source: Image.Image | None) -> Image.Image:
    """1024x500. No important element inside the outer 10% - Play crops there.

    The 10% inset is enforced, not merely intended. An adversarial review measured the device
    mock-up 18px (3.6%) from the bottom edge and the text column 76px (7.4%) from the left, both
    inside the band this docstring claims to avoid, so a surface that crops the graphic could cut
    the device. Every element placed below is recorded in `important` and asserted against the safe
    box after composition, which turns the promise into a check.
    """
    width, height = 1024, 500
    image = diagonal_gradient((width, height), INK, INK_SOFT, 104).convert("RGBA")
    image = Image.alpha_composite(image, glow((width, height), (width * 0.80, height * 0.42),
                                              width * 0.55, CYAN_DEEP, 84))
    image = Image.alpha_composite(image, glow((width, height), (width * 0.05, height * 0.95),
                                              width * 0.40, AMBER_DEEP, 70))
    image = Image.alpha_composite(image, blueprint_grid((width, height), 46, 11))

    margin_x = round(width * FEATURE_SAFE_FRACTION)
    margin_y = round(height * FEATURE_SAFE_FRACTION)
    important: list[tuple[str, tuple[int, int, int, int]]] = []

    # A real screen, not a drawing: the mock-up is the app's own capture.
    device = None
    device_x = width - margin_x
    if source is not None:
        device = device_frame(source, height - 2 * margin_y - 10, radius=40, bezel=13)
        device_x = width - margin_x - device.width
        device_y = height - margin_y - device.height
        shadow = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        ImageDraw.Draw(shadow).rounded_rectangle(
            (device_x + 12, device_y + 18, device_x + device.width + 12, device_y + device.height + 18),
            radius=44, fill=(0, 0, 0, 130))
        image = Image.alpha_composite(image, shadow.filter(ImageFilter.GaussianBlur(26)))
        image.alpha_composite(device, (device_x, device_y))
        # The shadow is decoration and may bleed; the device itself may not.
        important.append(("device", (device_x, device_y, device_x + device.width,
                                     device_y + device.height)))

    draw = ImageDraw.Draw(image)
    left = margin_x
    text_width = (device_x - 40 - left) if device else (width - 2 * left)
    icon = render_icon(listing, studio)
    image.alpha_composite(icon.resize((92, 92), Image.Resampling.LANCZOS).convert("RGBA"),
                          (left, margin_y + 24))
    important.append(("icon", (left, margin_y + 24, left + 92, margin_y + 24 + 92)))

    name = listing.get("appName", "PlayKiln")
    name_face = font(46, bold=True)
    y = margin_y + 146
    for line in wrap(draw, name, name_face, text_width)[:2]:
        draw.text((left + 2, y), line, font=name_face, fill=PAPER)
        important.append(("name", (left + 2, y, left + 2 + round(draw.textlength(line, font=name_face)),
                                   y + 46)))
        y += 54

    tagline = studio.get("featureTagline") or listing["locales"]["en-US"]["shortDescription"]
    body_face = font(24)
    y += 6
    for line in wrap(draw, tagline, body_face, text_width)[:2]:
        draw.text((left + 2, y), line, font=body_face, fill=MUTED)
        important.append(("tagline", (left + 2, y, left + 2 + round(draw.textlength(line, font=body_face)),
                                      y + 24)))
        y += 34

    # A small honest positioning pill: both claims are true of this app.
    pill_face = font(17, bold=True)
    pill_text = studio.get("featurePill", "OFFLINE  ·  NO CODE TABLES")
    pill_width = round(draw.textlength(pill_text, font=pill_face)) + 34
    pill_y = y + 14
    draw.rounded_rectangle((left, pill_y, left + pill_width, pill_y + 38), radius=19,
                           fill=(AMBER[0], AMBER[1], AMBER[2], 40), outline=AMBER, width=2)
    draw.text((left + 17, pill_y + 10), pill_text, font=pill_face, fill=AMBER_PALE)
    important.append(("pill", (left, pill_y, left + pill_width, pill_y + 38)))

    assert_feature_safe_area(image.size, important)
    return image.convert("RGB")


def assert_feature_safe_area(
    size: tuple[int, int], important: list[tuple[str, tuple[int, int, int, int]]]
) -> None:
    """Fail if any important element leaves the outer 10% band Play may crop."""
    width, height = size
    margin_x = round(width * FEATURE_SAFE_FRACTION)
    margin_y = round(height * FEATURE_SAFE_FRACTION)
    for label, (x0, y0, x1, y1) in important:
        # Rounding to whole pixels means an element may land exactly on the boundary; allow 1px.
        if x0 < margin_x - 1 or y0 < margin_y - 1 or x1 > width - margin_x + 1 or y1 > height - margin_y + 1:
            raise SystemExit(
                f"BLOCKED: the feature graphic's {label} spans x=[{x0}, {x1}] y=[{y0}, {y1}], "
                f"outside the safe box x=[{margin_x}, {width - margin_x}] "
                f"y=[{margin_y}, {height - margin_y}]. Play crops the outer "
                f"{FEATURE_SAFE_FRACTION:.0%} on some surfaces, so this element could be cut."
            )


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
# Emitted from `icon_design.mark_layers()`, the same primitive list the raster mark is drawn from,
# so the vector cannot drift from the raster icon: there is no second scale factor left to drift
# with. The viewport is the 108dp adaptive canvas and the mark is fitted to the circle keyline,
# which is what keeps it inside the 66dp safe zone under every launcher mask.
# --------------------------------------------------------------------------------------

BACKGROUND_GRADIENT_ANGLE = icon_design.FIELD_ANDROID_ANGLE


def vector_layers(mark: str | None = None) -> list[dict]:
    """The mark as path data, in the 108dp viewport, one entry per drawn path."""
    return icon_design.mark_layers(mark or CURRENT_MARK)


def _vector_path_xml(layers: list[dict], colors: dict[str, str]) -> str:
    """The `<path>` elements for a set of layers, tinted with one colour each."""
    return "\n".join(
        f'    <path\n'
        f'        android:fillColor="{colors.get(layer["role"], colors["paper"])}"\n'
        + (f'        android:fillType="evenOdd"\n' if layer["fill_type"] == "evenOdd" else "")
        + f'        android:pathData="{layer["data"]}" />'
        for layer in layers
    )


def _field_overlay_xml(overlays: tuple[dict, ...], colors: dict[str, str]) -> str:
    """The background's waterline as `<path>` elements, in the order `icon_design` declares them.

    The geometry is not restated here: `overlays` is `icon_design.field_overlays(...)`, the one
    definition `icon_field` also rasterises, so the drawable and the 512px listing icon cannot drift.
    `android:fillAlpha` is emitted rather than a 8-digit `fillColor`, because that is the form the
    design was authored in and the two are not interchangeable to a human reading the diff.
    """
    return "\n".join(
        f'    <path\n'
        f'        android:fillAlpha="{overlay["alpha"]:.2f}"\n'
        f'        android:fillColor="{colors[overlay["role"]]}"\n'
        f'        android:pathData="{overlay["data"]}" />'
        for overlay in overlays
    )


def write_vectors(res: Path, colors: dict[str, str]) -> list[Path]:
    """Write the adaptive background, foreground and monochrome drawables.

    The background is one linear ramp plus `icon_design.FIELD_OVERLAYS` - the declared waterline -
    and the two renderers emit it from that single definition, which is what makes the launcher icon
    the same picture as the 512px store icon. It used to be flattened to the ramp alone (with a
    stray translucent white polygon, `M0,92L58,34L86,62L108,40V108H0Z` at alpha 0.16, the listing
    icon never had); the owner chose the opposite resolution - keep the richer field and produce it
    in both places - so the ramp is not flattened and the drawable carries the waterline.

    A slot whose palette does not declare every role the waterline names gets no overlay paths at
    all: `field_overlays` withholds the whole waterline rather than half drawing it, so such a slot
    keeps the ramp it was designed with.

    The monochrome layer draws in the palette's `paper` role, never a literal white: the launcher
    reads the layer's alpha and tints it, so `paper` masks exactly as white would while keeping the
    mark inside the slot's own declared palette (`icons.launcher` fails a literal `#FFFFFF`).
    """
    layers = vector_layers()
    overlays = icon_design.field_overlays(colors.keys())
    overlay_paths = _field_overlay_xml(overlays, colors)
    # A slot without the waterline gets the byte-identical ramp-only body it had before this change.
    overlay_block = f"{overlay_paths}\n" if overlay_paths else ""
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
                android:angle="{BACKGROUND_GRADIENT_ANGLE}"
                android:endColor="{colors['ink_lift']}"
                android:startColor="{colors['ink']}"
                android:type="linear" />
        </aapt:attr>
    </path>
{overlay_block}</vector>
"""
    foreground_paths = _vector_path_xml(layers, colors)
    monochrome_colors = {role: colors["paper"] for role in colors}
    monochrome_paths = _vector_path_xml(layers, monochrome_colors)
    foreground = f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by scripts/store-studio.py. Do not hand-edit: re-run the studio. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
{foreground_paths}
</vector>
"""
    monochrome = f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by scripts/store-studio.py. Do not hand-edit: re-run the studio. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
{monochrome_paths}
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


# `hex_of` is imported from `scripts/lib/colours.py` at the top of this module; there is no local
# definition, so the colour maths has one implementation (see the import alias above).


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


def assert_capture_is_plausible(path: Path, image: Image.Image) -> None:
    """Reject a frame that cannot be a screen of this app.

    A PNG that is one flat colour has the right dimensions, the right mode and a plausible byte
    size, so every gate in the factory accepts it. Composing is the cheapest place to stop it.
    """
    colours = image.getcolors(maxcolors=MIN_CAPTURE_COLOURS + 1)
    distinct = len(colours) if colours is not None else MIN_CAPTURE_COLOURS + 1
    low, high = image.convert("L").getextrema()
    spread = high - low
    if distinct < MIN_CAPTURE_COLOURS or spread < MIN_CAPTURE_LUMA_SPREAD:
        raise SystemExit(
            f"BLOCKED: raw capture {path} is (near) uniform - {distinct} distinct colour(s), "
            f"luminance spread {spread}. A real screen of this app carries at least "
            f"{MIN_CAPTURE_COLOURS} colours with a spread of at least {MIN_CAPTURE_LUMA_SPREAD}, "
            "so this file is not a screenshot. Re-run the capture."
        )


def load_raw(store: Path, index: int) -> Image.Image | None:
    """Load raw capture `index`, or `None` when the file is simply absent.

    An existing-but-unreadable file is fatal rather than skipped. A truncated PNG is the ordinary
    result of an interrupted copy, and treating it as "missing" is how a pack once got composed
    from two different capture runs while every gate still reported OK: the skipped target kept the
    PREVIOUS run's file, and the result passed both `store-kit --validate-only` and
    `verify.mjs`.
    """
    for candidate in (f"{index:02d}.png", f"phone-{index:02d}.png"):
        path = store / "captures" / "raw" / candidate
        if path.is_file():
            try:
                with Image.open(path) as source:
                    image = source.convert("RGB")
            except OSError as error:
                raise SystemExit(
                    f"BLOCKED: raw capture {path} exists but cannot be read ({error}). Delete it "
                    "and re-run the capture; composing around it would mix two capture runs into "
                    "one store pack."
                ) from error
            assert_capture_is_plausible(path, image)
            return image
    return None


def default_studio(listing: dict) -> dict:
    return {
        "headlines": [],
        "kickers": [],
        "featureTagline": listing.get("locales", {}).get("en-US", {}).get("shortDescription", ""),
        "featurePill": "OFFLINE  ·  NO CODE TABLES",
    }


def declared_locales(listing: dict) -> list[str]:
    """The locales the pack declares, the default first and each once.

    The studio must ship one asset set per locale `scripts/store/validate.py` requires, and that
    reader derives them from the same listing with the same order (`pack_model.Listing.declared_locales`).
    Reading the declaration here, rather than the literal `en-US` this module used to hardcode, is what
    lets a two-locale slot render both sets; the two readers stay in step because both walk
    `listing["locales"]` with the default prepended.
    """
    raw = listing.get("locales")
    names = [str(name) for name in raw] if isinstance(raw, dict) else []
    default = listing.get("defaultLocale")
    if not isinstance(default, str) or not default:
        fallback = _MODEL.LOCALES.get("default")
        default = fallback if isinstance(fallback, str) else ""
    ordered: list[str] = []
    for locale in (default, *names):
        if locale and locale not in ordered:
            ordered.append(locale)
    return ordered


def _release_approval_problem(slot: Path) -> str | None:
    """The reason identity rendering must wait, imported from ``render.py`` rather than restated.

    Rendering the mark, the feature graphic and the screenshot sets is release work: the assets are
    the app's public claim about itself, and the owner approves the app before they are produced.
    This module also rewrites the app's own launcher vectors, so it is not merely store output.

    The decision lives in ``scripts/lib/release-approval.mjs``, which ``render.py`` already wraps in
    ``release_approval_block``. The wrapper is imported rather than re-implemented because a second
    copy of the rule is a second answer to one question. ``render.py`` loads this file by path, so the
    lookup reuses an already-loaded copy instead of re-executing its loader — the same reason and the
    same shape as ``_load_pack_model`` above, which exists for exactly this hazard.
    """
    path = (Path(__file__).resolve().parent / "store" / "render.py").resolve()
    for module in list(sys.modules.values()):
        module_file = getattr(module, "__file__", None)
        if module_file and Path(module_file).resolve() == path:
            return module.release_approval_block(slot)
    spec = importlib.util.spec_from_file_location("store_render", path)
    if spec is None or spec.loader is None:
        raise SystemExit(f"BLOCKED: cannot load {path} to check the release approval")
    module = importlib.util.module_from_spec(spec)
    sys.modules["store_render"] = module
    try:
        spec.loader.exec_module(module)
    except Exception:
        sys.modules.pop("store_render", None)
        raise
    return module.release_approval_block(slot)


def render_slot(slot: Path, *, preview_circle: bool = False, identity_only: bool = False) -> None:
    global CURRENT_MARK
    # Before anything is read or written. Invoked directly this command used to have no gate at all,
    # which made `release-gate.mdc`'s own claim — that release work cannot be reached by calling a
    # helper instead of the documented entry point — untrue for the one script that composes the
    # entire store identity and rewrites the launcher vectors.
    problem = _release_approval_problem(slot)
    if problem is not None:
        raise SystemExit(f"BLOCKED: {problem}")
    store = slot / "store"
    listing = json.loads((store / "listing.json").read_text(encoding="utf-8"))
    studio_path = store / "studio.json"
    if not studio_path.is_file():
        raise SystemExit(
            f"BLOCKED: {studio_path} is missing. It supplies the headlines, kickers and palette "
            "every composed asset is built from, so rendering without it silently falls back to "
            "the journey titles: the whole pack changes while `graphics.md` keeps claiming the "
            "media is composed to this slot's identity. Restore the file, or move the slot to the "
            "generic renderer deliberately instead of downgrading it by deletion."
        )
    studio = default_studio(listing)
    studio.update(json.loads(studio_path.read_text(encoding="utf-8")))
    palette = resolve_palette(studio.get("palette"), slot)
    apply_palette(palette)
    CURRENT_MARK = resolve_mark(studio)

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

    # Every capture must be present and readable BEFORE anything is written, and the whole set is
    # rendered in memory before a single file is replaced. The old code wrote each target as it
    # went and `continue`d past a missing capture, so an incomplete capture set produced a pack
    # that was half this run and half the previous one - and no gate noticed, because every gate
    # checks dimensions and mode, not provenance of the pixels.
    sources = ([] if identity_only else
               [load_raw(store, index) for index in range(1, REQUIRED_RAW_CAPTURES + 1)])
    absent = [index for index, source in enumerate(sources, start=1) if source is None]
    if absent:
        raise SystemExit(
            "BLOCKED: no readable raw capture for "
            + ", ".join(f"{index:02d}.png" for index in absent)
            + f" under {store / 'captures' / 'raw'}. All "
            f"{sum(count for count, _ in SCREEN_TARGETS.values())} screenshots are composed from "
            f"the {REQUIRED_RAW_CAPTURES} captures, so rendering with one missing would ship a "
            "pack that mixes two capture runs. Re-run the capture first: "
            "node scripts/media-capture.mjs --slot <slug>"
        )

    sizes = {source.size for source in sources if source is not None}
    if len(sizes) > 1:
        raise SystemExit(
            "BLOCKED: the raw captures disagree on size ("
            + ", ".join(f"{w}x{h}" for w, h in sorted(sizes))
            + "). One journey runs on one device, so a mixed set means these files came from "
            "different capture runs; re-run the capture to replace the whole set."
        )

    icon = render_icon(listing, studio)
    assets = store / "assets"
    assets.mkdir(parents=True, exist_ok=True)
    locales = declared_locales(listing)
    # A set per declared locale, plus the two shared root copies. `declared_locales` is the same
    # decision `scripts/store/validate.py` reads, so the studio writes exactly the sets the pack's
    # independent reader requires.
    icon_targets = [assets / "icon-512.png"]
    feature_targets = [assets / "feature-graphic-1024x500.png"]
    for locale in locales:
        icon_targets.append(assets / locale / "icon-512.png")
        feature_targets.append(assets / locale / "feature-graphic-1024x500.png")
    feature_source = None if identity_only else sources[0]
    composed_items = [item for item in composed if item["headline"]]
    # (target, image, label) in write order. Nothing touches disk until the whole list is built.
    #
    # Every locale's set is composed from the SAME rendered image as the shared root pair. The studio
    # holds no per-locale headline, kicker or body copy, so localising the composition would mean
    # inventing translations it must not invent; identical pixels across locales is the deliberate
    # decision for this change, and only the path set differs per locale. Each image is rendered once
    # and reused, so a locale's pixels cannot drift from the default locale's.
    renders: list[tuple[Path, object, str]] = []
    icon_render = icon.convert("RGBA")
    # Saved as RGBA to match the mode store-kit validates and Play's expectation. The icon is
    # full-bleed, so every pixel is opaque and the alpha channel is uniform - it carries no
    # transparency, it just declares the channel.
    for target in icon_targets:
        renders.append((target, icon_render, "512x512 RGBA"))
    feature_render = render_feature(listing, studio, feature_source)
    for target in feature_targets:
        renders.append((target, feature_render, "1024x500"))
    if not identity_only:
        shots_rendered: list[tuple[str, object, str]] = []
        for kind, (count, dimensions) in SCREEN_TARGETS.items():
            for index in range(1, count + 1):
                source = sources[(index - 1) % len(sources)]
                item = composed_items[(index - 1) % len(composed_items)] if composed_items else {
                    "headline": "", "body": "", "kicker": ""}
                shots_rendered.append(
                    (f"{kind}-{index:02d}.png", render_screenshot(dimensions, item, source),
                     f"{dimensions[0]}x{dimensions[1]}")
                )
        for locale in locales:
            for name, image, label in shots_rendered:
                renders.append((assets / locale / name, image, label))

    # A pack is all-or-nothing, so the count is asserted before anything is written. The expected
    # total is derived from the declared locale count, so a pack that declares two locales cannot be
    # written as a one-locale set, and a partial set still refuses to write. A mismatch means this
    # function and the inventory that describes it have diverged, which is exactly the failure mode
    # above.
    expected = expected_composed_assets(len(locales), identity_only=identity_only)
    if len(renders) != expected:
        raise SystemExit(
            f"BLOCKED: the studio rendered {len(renders)} assets but the pack must contain "
            f"{expected}. Refusing to write a partial pack."
        )

    for target, image, label in renders:
        target.parent.mkdir(parents=True, exist_ok=True)
        image.save(target, format="PNG", optimize=True, compress_level=9)  # type: ignore[attr-defined]
        print(f"PASS studio - {target.relative_to(slot)} ({label})")

    colors = {role: hex_of(value) for role, value in palette.items()}
    for path in write_vectors(slot / "app" / "src" / "main" / "res", colors):
        print(f"PASS studio - {path.relative_to(slot)}")

    if preview_circle:
        preview_dir = store / "captures" / "preview"
        preview_dir.mkdir(parents=True, exist_ok=True)
        previews = adaptive_previews()
        for name, preview in previews.items():
            preview.save(preview_dir / name, format="PNG", optimize=True)
            print(f"PASS studio - preview {name}")
        # `captures/` is gitignored evidence, and a stale preview is worse than none: the previous
        # misleading `icon-circle.png`/`icon-squircle.png` names must not survive this run and be
        # read as current.
        for stale in preview_dir.glob("icon-*.png"):
            if stale.name not in previews:
                stale.unlink()
                print(f"PASS studio - removed stale preview {stale.name}")
    if identity_only:
        print("UNVERIFIED studio - screenshot targets were not rendered because --identity-only skips raw captures")


# --------------------------------------------------------------------------------------
# Launcher-mask previews
#
# `store/captures/preview/` is evidence for a reviewer, not a Play asset, so it has to show what a
# launcher actually draws. The shipped icon is the adaptive vector from `vector_layers()`, fitted to
# the circle keyline so its furthest ink sits inside the safe radius, and the real mask is the 72dp
# circle (36dp radius) inside the 108dp viewport. The previous previews masked the 512px listing
# raster across the full canvas, which measured 0.358 * 108 = 38.7dp of reach - so they drew a mark a
# real mask WOULD clip and implied it was safe. These previews rasterise the same path data
# `write_vectors` writes.
# --------------------------------------------------------------------------------------

PREVIEW_PX_PER_DP = 10
PREVIEW_CANVAS_PX = round(ADAPTIVE_VIEWPORT_DP * PREVIEW_PX_PER_DP)  # 1080px for the 108dp canvas
PREVIEW_OUTSIDE = (12, 14, 18, 255)  # what the launcher shows outside the mask
PREVIEW_ARC_SAMPLES = 64
_PREVIEW_TOKEN = re.compile(r"([MmLlHhVvAaZz])|(-?\d*\.?\d+)")


def _preview_arc(
    x1: float, y1: float, x2: float, y2: float, radius: float, large: float, sweep: float
) -> list[tuple[float, float]]:
    """Sample a circular arc. `mark_layers()` only emits `a rx,rx`, so ry is `radius`."""
    mx, my = (x1 + x2) / 2.0, (y1 + y2) / 2.0
    dxr, dyr = (x1 - x2) / 2.0, (y1 - y2) / 2.0
    chord = math.hypot(dxr, dyr)
    h = math.sqrt(max(0.0, radius * radius - chord * chord))
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
        (cx + radius * math.cos(a1 + (a2 - a1) * step / PREVIEW_ARC_SAMPLES),
         cy + radius * math.sin(a1 + (a2 - a1) * step / PREVIEW_ARC_SAMPLES))
        for step in range(1, PREVIEW_ARC_SAMPLES + 1)
    ]


def _preview_subpaths(path_data: str, scale: float) -> list[list[tuple[float, float]]]:
    """Turn the studio's own vector path data into closed polygons, in canvas pixels.

    Understands the command subset `mark_layers()` emits - `M/m`, `L/l`, `H/h`, `V/v`, `a` and
    `Z/z` - including relative forms and repeated coordinates, so the preview cannot drift from the
    XML it mirrors. A non-circular arc raises instead of being drawn as the wrong shape.
    """
    tokens: list[str | float] = []
    for command, number in _PREVIEW_TOKEN.findall(path_data):
        tokens.append(command if command else float(number))

    subpaths: list[list[tuple[float, float]]] = []
    current: list[tuple[float, float]] = []
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
                if len(current) >= 3:
                    current.append(current[0])
                    subpaths.append(current)
                current = []
                x, y = start
            continue
        relative = command.islower()
        kind = command.upper()
        if kind == "M":
            nx, ny = float(tokens[i]), float(tokens[i + 1])
            x, y = (x + nx, y + ny) if relative else (nx, ny)
            if len(current) >= 3:
                subpaths.append(current)
            current = [(x * scale, y * scale)]
            start = (x, y)
            i += 2
            command = "l" if relative else "L"
            continue
        if kind == "L":
            nx, ny = float(tokens[i]), float(tokens[i + 1])
            x, y = (x + nx, y + ny) if relative else (nx, ny)
            current.append((x * scale, y * scale))
            i += 2
            continue
        if kind == "H":
            nx = float(tokens[i])
            x = x + nx if relative else nx
            current.append((x * scale, y * scale))
            i += 1
            continue
        if kind == "V":
            ny = float(tokens[i])
            y = y + ny if relative else ny
            current.append((x * scale, y * scale))
            i += 1
            continue
        if kind == "A":
            rx, ry, _rotation, large, sweep, ax, ay = (float(t) for t in tokens[i : i + 7])
            i += 7
            if abs(rx - ry) > 1e-6:
                raise SystemExit(
                    f"BLOCKED: the preview cannot draw the elliptical arc rx={rx} ry={ry}; "
                    "mark_layers() only emits circular arcs."
                )
            x2, y2 = (x + ax, y + ay) if relative else (ax, ay)
            current.extend((px * scale, py * scale) for px, py in _preview_arc(x, y, x2, y2, rx, large, sweep))
            x, y = x2, y2
            continue
        i += 1
    if len(current) >= 3:
        subpaths.append(current)
    return subpaths


def _preview_mask(size: int, subpaths: list[list[tuple[float, float]]], *, even_odd: bool) -> Image.Image:
    """Fill subpaths into an 8-bit mask: even-odd (XOR) for the ring, union for the ink group."""
    mask = Image.new("L", (size, size), 0)
    for points in subpaths:
        layer = Image.new("L", (size, size), 0)
        ImageDraw.Draw(layer).polygon(points, fill=255)
        mask = ImageChops.difference(mask, layer) if even_odd else ImageChops.lighter(mask, layer)
    return mask


def render_adaptive_icon() -> Image.Image:
    """The shipped adaptive icon, rasterised from the vector paths `write_vectors` writes.

    The layer is the 108dp viewport, its field is the one declared ramp plus the declared waterline
    (`icon_design.FIELD_OVERLAYS`, coloured from this slot's palette), and the mark comes from
    `icon_design.mark_layers()` - the same path data that goes into the drawable. Nothing here is
    re-derived from a second set of constants, which is what the previous version of this function
    did: it hardcoded the old two-branch mark (a ring group or a page group) and drew the
    translucent polygon the background drawable had, so the preview and the store icon could show
    two different fields.
    """
    size = PREVIEW_CANVAS_PX
    image = icon_design.icon_field(
        size, INK, INK_LIFT, icon_design.FIELD_ANGLE_DEG,
        palette=dict(_palette_roles())).convert("RGBA")
    image.alpha_composite(vector_raster(size))
    return image


def _preview_points(path_data: str) -> list[tuple[float, float]]:
    """Every outline point of a path, as (x, y) in viewport units."""
    return [
        (px, py)
        for subpath in _preview_subpaths(path_data, 1.0)
        for px, py in subpath
    ]


def vector_raster(size: int = PREVIEW_CANVAS_PX, mark: str | None = None) -> Image.Image:
    """Rasterise `mark_layers()` alone, on a transparent layer, at `size` px.

    Separate from `render_adaptive_icon` because the regression suite measures the raster and the
    vector renderers against each other: this is the vector renderer's own output, with nothing
    added, so a difference between the two cannot be excused as a field or a highlight.
    """
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    scale = size / ADAPTIVE_VIEWPORT_DP
    for entry in icon_design.mark_layers(mark or CURRENT_MARK):
        mask = _preview_mask(size, _preview_subpaths(entry["data"], scale),
                             even_odd=entry["fill_type"] == "evenOdd")
        colour = dict(_palette_roles())[entry["role"]]
        layer.paste(Image.new("RGBA", (size, size), colour + (255,)), (0, 0), mask)
    return layer


def _under_mask(icon: Image.Image, keep: Image.Image) -> Image.Image:
    layer = Image.new("RGBA", icon.size, PREVIEW_OUTSIDE)
    layer.paste(icon, (0, 0), keep)
    return layer.convert("RGB")


def adaptive_previews() -> dict[str, Image.Image]:
    """What a launcher shows, plus the safe zone the checker asserts.

    The mask is the real 72dp circle / 72dp rounded square, not the 108dp canvas, and the mark is
    the shipped adaptive foreground on its circle keyline, which reaches 31dp of the 33dp safe
    radius.
    """
    size = PREVIEW_CANVAS_PX
    icon = render_adaptive_icon()
    half = size / 2
    visible = ADAPTIVE_VISIBLE_RADIUS_DP * PREVIEW_PX_PER_DP

    circle_keep = Image.new("L", (size, size), 0)
    ImageDraw.Draw(circle_keep).ellipse(
        (half - visible, half - visible, half + visible, half + visible), fill=255)

    squircle_keep = Image.new("L", (size, size), 0)
    ImageDraw.Draw(squircle_keep).rounded_rectangle(
        (half - visible, half - visible, half + visible, half + visible),
        radius=round(visible / 2), fill=255)

    # The 66dp safe zone: tint the band between it and the visible mask, so the margin the checker
    # reports is visible in the image.
    safe_radius = ADAPTIVE_SAFE_RADIUS_DP * PREVIEW_PX_PER_DP
    safe_disc = Image.new("L", (size, size), 0)
    ImageDraw.Draw(safe_disc).ellipse(
        (half - safe_radius, half - safe_radius, half + safe_radius, half + safe_radius), fill=255)
    band = ImageChops.difference(circle_keep, safe_disc)
    safe = _under_mask(icon, circle_keep).convert("RGBA")
    tint = Image.new("RGBA", (size, size), (255, 96, 96, 0))
    tint.putalpha(band.point(lambda value: 72 if value else 0))
    safe = Image.alpha_composite(safe, tint)
    ImageDraw.Draw(safe).ellipse(
        (half - safe_radius, half - safe_radius, half + safe_radius, half + safe_radius),
        outline=CYAN + (255,), width=3)

    return {
        "icon-visible-72dp-circle.png": _under_mask(icon, circle_keep),
        "icon-visible-72dp-squircle.png": _under_mask(icon, squircle_keep),
        "icon-safe-66dp.png": safe.convert("RGB"),
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--slot", required=True, help="slot slug, e.g. pipe-sizing")
    parser.add_argument("--root", default=".", help="repository root (default: cwd)")
    parser.add_argument("--preview-circle", action="store_true",
                        help="also render the 72dp launcher-mask previews and the 66dp safe-zone preview")
    parser.add_argument("--identity-only", action="store_true",
                        help="render only icons, feature graphics, and adaptive vectors; skip captures and exit 2")
    options = parser.parse_args()
    slot = resolve_slot(Path(options.root).resolve(), options.slot)
    render_slot(slot, preview_circle=options.preview_circle, identity_only=options.identity_only)
    if options.identity_only:
        raise SystemExit(2)
    print(f"STUDIO: OK {slot}")


if __name__ == "__main__":
    main()
