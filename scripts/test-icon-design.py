#!/usr/bin/env python3
"""Regression suite for the icon design system, its measurement gate, and both renderers.

What this guards
----------------
The first icons this factory shipped were a diagonal gradient, two radial glows, a 32px drafting
grid at alpha 12, and a glyph placed by hand-tuned pixel constants. The owner rejected the result
("bad and crooked, a naive silly thing, not a designed mark"), which is not a cosmetic judgement:
there was no construction grid, no optical alignment, no declared stroke weight, no negative-space
rule and no small-size test, so nothing about the icon was a *decision* that could be checked.

The replacement is three files, and this suite is the part that can fail:

* ``scripts/lib/icon_design.py`` - the keyline grid, the stroke scale, the negative-space floor and
  the primitives, rendered twice (pixels and Android path data) from one description.
* ``scripts/lib/icon_qa.py`` - the measurements that turn those properties into FAIL rows.
* ``scripts/store-studio.py`` - renders the pack and refuses to write an icon that fails the gate.

Three kinds of leg, and the distinction matters:

1. **The design passes** every gated leg, for every mark in the registry, deterministically, with the
   raster and the vector agreeing about how big the mark is.
2. **Every sensor can fail.** A gate that cannot be shown failing is not a gate, so each leg is
   driven with an icon built to break exactly it - the pre-change field composition, an oversized
   mark, a mark off its optical centre, a mark scaled below the negative-space floor, a mark drawn
   in the field's own colour, a transparent corner. The renderer *refuses* most of that geometry by
   design (``icon_design.assert_mark_on_grid``), so the injected marks bypass that refusal on
   purpose: the sensors have to catch what the grid check would have.
3. **The pre-change renderer fails the same sensors.** ``--legacy-rev <rev>`` runs the real thing
   from git (``HEAD`` while the rewrite is uncommitted, which is how the recorded counts below were
   produced) and reports its failing legs; a reproduction of the old field composition always runs,
   so the demonstration does not depend on git history.

Nothing here writes into a slot: renders go to ``--out`` (default ``.kiln/icon-design/suite``),
which is gitignored, because rewriting a committed ``store/assets/*.png`` would break the
``store/provenance.json`` record that binds it to a build.

    python scripts/test-icon-design.py                       # exits 0 when every leg holds
    python scripts/test-icon-design.py --legacy-rev HEAD      # + the pre-change renderer's failures
    python scripts/test-icon-design.py --json .kiln/icon-design/suite/measurements.json
"""

from __future__ import annotations

import argparse
import hashlib
import importlib.util
import io
import json
import subprocess
import sys
from contextlib import contextmanager
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
if str(HERE) not in sys.path:
    sys.path.insert(0, str(HERE))
from lib import icon_design as design  # noqa: E402
from lib import icon_qa as qa  # noqa: E402

CANVAS = design.CANVAS

# ---------------------------------------------------------------------------------------
# The pre-change composition, quoted from `git show HEAD:scripts/store-studio.py`
#
# The field is reproduced exactly: the same gradient, the same two blooms at the same centres,
# radii, colours and alphas, and the same 32px grid at alpha 12 - that is the part the owner's
# complaint names, and the part the field legs measure. The pre-change *glyph* is not reproduced
# (its per-mark pixel constants are what the rewrite replaced); its size is, through
# `LEGACY_MARK_REACH_FRACTION`, which is the reach `scripts/lib/icon_design.py` records for the old
# renderer. So the mark legs below carry the old scale and the new silhouettes, and their job is to
# show the sensors firing, not to be a museum copy.
# ---------------------------------------------------------------------------------------
LEGACY_GRADIENT_SMALL_PX = 128  # the old field was rendered small and resampled up
LEGACY_BLOOMS = (
    {"centre": (0.74, 0.20), "radius": 0.72, "colour": (24, 116, 176), "alpha": 96},
    {"centre": (0.22, 0.88), "radius": 0.78, "colour": (238, 118, 18), "alpha": 88},
)
LEGACY_GRID_PERIOD_PX = 32
LEGACY_GRID_ALPHA = 12
LEGACY_MARK_REACH_FRACTION = 0.358  # the old listing icon's reach, from `icon_design`'s record

failures: list[str] = []
measured: dict = {}


def check(ok: bool, message: str, detail: str = "") -> bool:
    print(f"  {'PASS' if ok else 'FAIL'} {message}" + (f"  [{detail}]" if detail else ""))
    if not ok:
        failures.append(message)
    return ok


def record(key: str, value) -> None:
    measured[key] = value


# ---------------------------------------------------------------------------------------
# Rendering
# ---------------------------------------------------------------------------------------


def new_icon(mark: str, palette: dict, canvas: int = CANVAS) -> np.ndarray:
    """The shipped 512px icon: the declared field with the declared mark on it.

    The palette is handed through so the field carries `icon_design.FIELD_OVERLAYS` when the slot
    declares the waterline roles - exactly as `store-studio.render_icon` now does - rather than the
    bare ramp. A slot without those roles (every slot but laneset) renders the same ramp as before.
    """
    field = design.icon_field(canvas, palette["ink"], palette["ink_lift"],
                              palette=palette).convert("RGBA")
    field.alpha_composite(design.render_mark(canvas, mark, palette))
    return np.asarray(field, dtype=np.uint8)


def new_icon_png(mark: str, palette: dict, canvas: int = CANVAS) -> bytes:
    buffer = io.BytesIO()
    Image.fromarray(new_icon(mark, palette, canvas)).save(buffer, format="PNG")
    return buffer.getvalue()


def legacy_field(canvas: int = CANVAS) -> Image.Image:
    """The pre-change field: a resampled ramp, two radial blooms, and a 32px drafting grid."""
    ramp = design.icon_field(LEGACY_GRADIENT_SMALL_PX, (7, 16, 30), (26, 62, 102))
    image = ramp.resize((canvas, canvas), Image.Resampling.BILINEAR).convert("RGBA")
    ys, xs = np.mgrid[0:canvas, 0:canvas]
    for bloom in LEGACY_BLOOMS:
        centre = (bloom["centre"][0] * canvas, bloom["centre"][1] * canvas)
        distance = np.sqrt((xs - centre[0]) ** 2 + (ys - centre[1]) ** 2) / (bloom["radius"] * canvas)
        layer = np.zeros((canvas, canvas, 4), dtype=np.uint8)
        layer[..., 0], layer[..., 1], layer[..., 2] = bloom["colour"]
        layer[..., 3] = np.clip(1.0 - distance, 0.0, 1.0) ** 2 * bloom["alpha"]
        image = Image.alpha_composite(image, Image.fromarray(layer))
    grid = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    pixels = grid.load()
    for position in range(0, canvas, LEGACY_GRID_PERIOD_PX):
        for along in range(canvas):
            pixels[position, along] = (255, 255, 255, LEGACY_GRID_ALPHA)
            pixels[along, position] = (255, 255, 255, LEGACY_GRID_ALPHA)
    return Image.alpha_composite(image, grid)


def transform(primitives: tuple[dict, ...], *, scale: float = 1.0,
              shift: tuple[float, float] = (0.0, 0.0)) -> tuple[dict, ...]:
    """Scale and offset a mark about the canvas centre, for the falsification legs.

    Stroke weights are tokens and stay tokens: that is the point of a declared scale, so the
    injected marks move geometry and leave the stroke scale alone.
    """
    def point(value: tuple[float, float]) -> tuple[float, float]:
        return (0.5 + (value[0] - 0.5) * scale + shift[0],
                0.5 + (value[1] - 0.5) * scale + shift[1])

    out = []
    for primitive in primitives:
        moved = dict(primitive)
        if "center" in moved:
            moved["center"] = point(moved["center"])
        if "radius" in moved:
            moved["radius"] = moved["radius"] * scale
        if "size" in moved:
            moved["size"] = (moved["size"][0] * scale, moved["size"][1] * scale)
        if "points" in moved:
            moved["points"] = tuple(point(value) for value in moved["points"])
        out.append(moved)
    return tuple(out)


@contextmanager
def injected(label: str, primitives: tuple[dict, ...]):
    """Register a mark the grid check would refuse, so a sensor can be shown failing on it."""
    name = f"test-{label}"
    design.MARK_REGISTRY[name] = {"primitives": primitives, "keyline": "circle",
                                  "note": f"falsification control: {label}"}
    original = design.assert_mark_on_grid
    design.assert_mark_on_grid = lambda mark=None: None  # noqa: ARG005 - the refusal is the point
    try:
        yield name
    finally:
        design.assert_mark_on_grid = original
        design.MARK_REGISTRY.pop(name, None)


# ---------------------------------------------------------------------------------------
# Loading the factory modules and the real pre-change renderer
# ---------------------------------------------------------------------------------------


def load_module(name: str, path: Path):
    spec = importlib.util.spec_from_file_location(name, path)
    if spec is None or spec.loader is None:
        raise SystemExit(f"BLOCKED: cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module
    spec.loader.exec_module(module)
    return module


def slot_palette(slot: Path) -> dict:
    studio = json.loads((slot / "store" / "studio.json").read_text(encoding="utf-8"))
    resolved = studio_default_palette()
    for role, value in (studio.get("palette") or {}).items():
        resolved[role] = tuple(int(value[index:index + 2], 16) for index in (1, 3, 5))
    return resolved


def studio_default_palette() -> dict:
    """The slot-independent palette `store-studio.py` declares when a slot overrides nothing."""
    return {
        "ink": (7, 16, 30), "ink_soft": (15, 36, 64), "ink_lift": (26, 62, 102),
        "amber": (255, 170, 40), "amber_deep": (238, 118, 18), "amber_pale": (255, 206, 128),
        "accent_deep": (24, 116, 176), "paper": (248, 250, 252), "muted": (170, 188, 210),
        "bezel": (13, 18, 27), "bezel_rim": (126, 148, 176),
    }


def legacy_renderer(rev: str):
    """The pre-change `store-studio.py` from git, or None when the revision does not have one."""
    source = subprocess.run(["git", "-C", str(ROOT), "show", f"{rev}:scripts/store-studio.py"],
                            capture_output=True, text=True)
    if source.returncode != 0:
        return None, f"git show {rev}:scripts/store-studio.py failed: {source.stderr.strip()}"
    if "icon_design" in source.stdout:
        return None, f"{rev} already contains the design system, so it is not a pre-change revision"
    scratch = out_dir() / f"legacy-store-studio-{rev.replace('/', '_')}.py"
    scratch.write_text(source.stdout, encoding="utf-8")
    return load_module("pk_legacy_studio", scratch), None


def out_dir() -> Path:
    return Path(measured["_out"])


# ---------------------------------------------------------------------------------------
# 1. The design system passes
# ---------------------------------------------------------------------------------------


def verify_design(mark: str, palette: dict) -> dict:
    icon = new_icon(mark, palette)
    result = qa.evaluate_icon(icon, palette, mark=mark)
    record(f"gate_{mark}", {"verdict": result["verdict"], "failed": result["failed_legs"],
                            "measured": result["measured"],
                            "survival": result["context"]["mask_survival"]})
    print(f"\n-- {mark}: the gate --")
    check(result["verdict"] == "PASS",
          f"{mark}: every gated leg passes ({result['gated_legs']} legs)",
          f"failed={result['failed_legs'] or 'none'}")
    for line in qa.format_legs(result):
        print(f"  {line.strip()}")
    return result


def verify_mask_survival(mark: str, result: dict) -> None:
    print(f"\n-- {mark}: the safe zone under each mask --")
    for name, data in result["context"]["mask_survival"].items():
        check(data["mark_px_clipped"] == 0,
              f"{mark}: no ink clipped by {name}",
              f"clipped={data['mark_px_clipped']}px of {data['mark_fraction_clipped']}")
    check(result["measured"]["mark_reach_dp"] <= design.mark_reach_dp(mark) + 1.0,
          f"{mark}: the raster reaches no further than the declaration",
          f"{result['measured']['mark_reach_dp']}dp measured vs "
          f"{round(design.mark_reach_dp(mark), 3)}dp declared")


def verify_geometry_legs(mark: str, result: dict) -> None:
    print(f"\n-- {mark}: strokes, counters and centring --")
    floor_px = design.MIN_FEATURE * CANVAS
    check(result["measured"]["thinnest_stroke_px"] >= floor_px,
          f"{mark}: thinnest stroke clears the declared {floor_px:.0f}px floor",
          f"{result['measured']['thinnest_stroke_px']}px "
          f"({result['measured']['thinnest_stroke_role']})")
    check(result["measured"]["min_feature_at_48dp_px"] >= qa.ICON_GATES["min_feature_at_48dp_px"]["min"],
          f"{mark}: thinnest stroke is at least 1.5px at 48dp",
          f"{result['measured']['min_feature_at_48dp_px']}px")
    counter = result["measured"]["smallest_counter_px"]
    check(counter is None or counter >= design.MIN_COUNTER * CANVAS,
          f"{mark}: smallest counter clears the same {floor_px:.0f}px floor",
          f"{counter}px ({result['measured']['smallest_counter_role']})")
    anchor = design.optical_anchor(mark)
    check(tuple(anchor) == tuple(design.OPTICAL_CENTER),
          f"{mark}: declared on the optical centre, not nudged on to it",
          f"anchor={anchor}")
    check(result["measured"]["optical_offset_px"] <= qa.ICON_GATES["optical_offset_px"]["max"],
          f"{mark}: silhouette centroid sits on the optical centre",
          f"offset={result['measured']['optical_offset_px']}px")


def verify_opacity(mark: str, icon: np.ndarray, palette: dict) -> None:
    print(f"\n-- {mark}: the store icon is opaque and unmasked --")
    alpha = icon[..., 3]
    corners = [int(alpha[0, 0]), int(alpha[0, -1]), int(alpha[-1, 0]), int(alpha[-1, -1])]
    check(int(alpha.min()) == 255, f"{mark}: every pixel is opaque", f"min alpha={int(alpha.min())}")
    check(all(value == 255 for value in corners),
          f"{mark}: the four corners are opaque, so no rounded mask is baked in",
          f"corners={corners}")
    # A masked icon keeps its corners transparent; a full-bleed one keeps the field there. Compare
    # each corner against the field the design declares at that pixel - the ramp plus the declared
    # waterline, the same call `new_icon` makes.
    ramp = np.asarray(design.icon_field(CANVAS, palette["ink"], palette["ink_lift"],
                                        palette=palette).convert("RGB"),
                      dtype=np.int16)
    worst = int(max(abs(int(icon[y, x, channel]) - int(ramp[y, x, channel]))
                    for y, x in ((0, 0), (0, CANVAS - 1), (CANVAS - 1, 0), (CANVAS - 1, CANVAS - 1))
                    for channel in range(3)))
    check(worst <= 2, f"{mark}: each corner is the declared field, not a mask edge",
          f"max channel deviation from the ramp={worst}")
    band = qa.outer_band((CANVAS, CANVAS))
    ink_in_band = int((qa.mark_mask(icon, None, palette, mark) & band).sum())
    check(ink_in_band == 0, f"{mark}: no mark ink in the reserved outer 5% band",
          f"ink in band={ink_in_band}px")


def verify_determinism(mark: str, palette: dict) -> None:
    print(f"\n-- {mark}: determinism --")
    first, second = new_icon_png(mark, palette), new_icon_png(mark, palette)
    check(first == second, f"{mark}: two renders are byte-identical",
          f"sha256={hashlib.sha256(first).hexdigest()[:16]}")
    layers_a = json.dumps(design.mark_layers(mark), sort_keys=True)
    layers_b = json.dumps(design.mark_layers(mark), sort_keys=True)
    check(layers_a == layers_b, f"{mark}: two vector layer sets are identical",
          f"sha256={hashlib.sha256(layers_a.encode()).hexdigest()[:16]}")
    record(f"determinism_{mark}", {"icon_sha256": hashlib.sha256(first).hexdigest(),
                                   "vector_sha256": hashlib.sha256(layers_a.encode()).hexdigest()})


def verify_renderers_agree(mark: str, result: dict) -> None:
    print(f"\n-- {mark}: the raster and the vector are the same mark at the same size --")
    checker = load_module("pk_check_adaptive", HERE / "check-adaptive-icon.py")
    points = []
    for layer in design.mark_layers(mark):
        points.extend(checker.points_from(layer["data"]))
    vector_reach = checker.furthest_reach(points, design.ADAPTIVE_VIEWPORT_DP / 2)
    declared = design.mark_reach_dp(mark)
    check(abs(vector_reach - declared) <= 0.25,
          f"{mark}: the emitted path data reaches the declared distance",
          f"path data={round(vector_reach, 3)}dp declared={round(declared, 3)}dp")
    raster = result["measured"]["mark_reach_dp"]
    check(abs(raster - vector_reach) <= 1.0,
          f"{mark}: the raster mark reaches the same distance as the vector",
          f"raster={raster}dp vector={round(vector_reach, 3)}dp "
          f"delta={round(raster - vector_reach, 3)}dp")
    record(f"reach_{mark}", {"declared_dp": round(declared, 4),
                             "vector_dp": round(vector_reach, 4),
                             "raster_dp": raster,
                             "raster_minus_vector_dp": round(raster - vector_reach, 4)})


def verify_identity(mark: str, palette: dict) -> None:
    """A palette change must repaint the icon, and two marks must not be the same silhouette."""
    print(f"\n-- {mark}: identity is per slot and per mark --")
    other = dict(palette)
    other["ink"] = (16, 8, 30)
    other["ink_lift"] = (58, 26, 84)
    other["amber"] = (255, 138, 214)
    check(palette["ink"] != other["ink"], "sanity: the alternative palette differs")
    check(not np.array_equal(new_icon(mark, palette), new_icon(mark, other)),
          f"{mark}: a different slot palette repaints the icon")
    others = [name for name in design.MARK_REGISTRY if name != mark]
    if others:
        first = qa.mark_mask(new_icon(mark, palette), None, palette, mark)
        second = qa.mark_mask(new_icon(others[0], palette), None, palette, others[0])
        intersection = float((first & second).sum()) / max(1, int(first.sum() + second.sum()))
        check(intersection < 0.5, f"{mark}: its silhouette differs from {others[0]}'s",
              f"overlap={round(intersection, 3)}")
        record(f"silhouette_overlap_{mark}_vs_{others[0]}", round(intersection, 4))


# ---------------------------------------------------------------------------------------
# 2. Every sensor can fail
# ---------------------------------------------------------------------------------------


def verify_field_legs(palette: dict) -> None:
    print("\n-- falsification: the field --")
    mark = design.DEFAULT_MARK
    good = new_icon(mark, palette)
    legacy = legacy_field()
    legacy.alpha_composite(design.render_mark(CANVAS, mark, palette))
    legacy_array = np.asarray(legacy, dtype=np.uint8)
    good_result = qa.evaluate_icon(good, palette, mark=mark)
    legacy_result = qa.evaluate_icon(legacy_array, palette, mark=mark)
    record("field", {"new": good_result["measured"]["field_high_frequency_rms"],
                     "legacy_reproduction": legacy_result["measured"]["field_high_frequency_rms"],
                     "new_ramp": good_result["measured"]["field_linear_ramp_rms"],
                     "legacy_reproduction_ramp": legacy_result["measured"]["field_linear_ramp_rms"],
                     "new_periodicity": good_result["measured"]["field_grid_periodicity"],
                     "legacy_reproduction_periodicity":
                         legacy_result["measured"]["field_grid_periodicity"]})
    check("field_high_frequency_rms" not in good_result["failed_legs"],
          "the declared field carries no high-frequency texture",
          f"rms={good_result['measured']['field_high_frequency_rms']} "
          f"<= {qa.ICON_GATES['field_high_frequency_rms']['max']}")
    check("field_high_frequency_rms" in legacy_result["failed_legs"],
          "the pre-change field (two blooms + a 32px grid at alpha 12) fails that leg",
          f"rms={legacy_result['measured']['field_high_frequency_rms']}")
    check("field_linear_ramp_rms" not in good_result["failed_legs"],
          "the declared field is one ramp",
          f"rms={good_result['measured']['field_linear_ramp_rms']} "
          f"<= {qa.ICON_GATES['field_linear_ramp_rms']['max']}")
    check("field_linear_ramp_rms" in legacy_result["failed_legs"],
          "the pre-change field fails the ramp leg",
          f"rms={legacy_result['measured']['field_linear_ramp_rms']}")
    ratio = (legacy_result["measured"]["field_grid_periodicity"]
             / max(1e-9, good_result["measured"]["field_grid_periodicity"]))
    check(ratio >= 10.0,
          "the report-only grid-periodicity reading separates the grid from the ramp",
          f"pre-change={legacy_result['measured']['field_grid_periodicity']} vs "
          f"new={good_result['measured']['field_grid_periodicity']} ({round(ratio, 1)}x)")


def verify_mark_legs(palette: dict) -> None:
    print("\n-- falsification: the mark --")
    book = "mark-book"

    # Oversized: the pre-change listing icon's reach, which a launcher mask crops.
    factor = LEGACY_MARK_REACH_FRACTION / design.mark_reach(book)
    with injected("oversized", transform(design.mark_primitives(book), scale=factor)) as mark:
        icon = new_icon(mark, palette)
        result = qa.evaluate_icon(icon, palette, mark=mark)
        record("oversized", {"measured": result["measured"],
                             "failed": result["failed_legs"],
                             "survival": result["context"]["mask_survival"]})
        check("mark_reach_dp" in result["failed_legs"],
              "a mark at the pre-change scale fails the reach leg",
              f"reach={result['measured']['mark_reach_dp']}dp")
        check("mark_outside_safe_circle_px" in result["failed_legs"],
              "and it fails the safe-circle leg",
              f"outside={result['measured']['mark_outside_safe_circle_px']}px")
        clipped = {name: data["mark_px_clipped"]
                   for name, data in result["context"]["mask_survival"].items()}
        check(clipped["launcher_circle_72dp"] > 0 and clipped["safe_zone_circle_66dp"] > 0,
              "and the round launcher masks clip it", f"clipped={clipped}")
        check(all(value == 0 for name, value in clipped.items() if "square" in name
                  or "squircle" in name),
              "while the square masks do not, because a portrait page fits their corners",
              f"clipped={clipped}")
        Image.fromarray(icon).save(out_dir() / f"legacy-scale-{book}.png")

    # Off the optical centre: a mark moved 20px right and 14px down.
    shift = (20 / CANVAS, 14 / CANVAS)
    with injected("offcentre", transform(design.mark_primitives(book), shift=shift)) as mark:
        result = qa.evaluate_icon(new_icon(mark, palette), palette, mark=mark)
        record("offcentre", {"offset_px": result["measured"]["optical_offset_px"],
                             "failed": result["failed_legs"]})
        check("optical_offset_px" in result["failed_legs"],
              "a mark moved off the optical centre fails the centring leg",
              f"offset={result['measured']['optical_offset_px']}px "
              f"(tolerance {qa.ICON_GATES['optical_offset_px']['max']}px)")

    # Below the negative-space floor: the book at 0.6 of its size, whose punched rings close up.
    with injected("tiny", transform(design.mark_primitives(book), scale=0.6)) as mark:
        result = qa.evaluate_icon(new_icon(mark, palette), palette, mark=mark)
        record("tiny", {"measured": result["measured"], "failed": result["failed_legs"]})
        check("ink_coverage_of_safe_zone" in result["failed_legs"]
              or "silhouette_extent" in result["failed_legs"],
              "a mark far below the keyline fails the size legs",
              f"coverage={result['measured']['ink_coverage_of_safe_zone']} "
              f"extent={result['measured']['silhouette_extent']}")
        counter = result["measured"]["smallest_counter_px"]
        check(counter is not None and counter < design.MIN_COUNTER * CANVAS,
              "and its counters fall below the declared floor",
              f"smallest counter={counter}px < {design.MIN_COUNTER * CANVAS:.0f}px")

    # A feature thinner than the declared stroke scale: a 10px bar, which the grid check cannot
    # catch because the floor is declared in stroke tokens and a bar is filled geometry.
    with injected("thin", (design.bar("paper", (0.5, 0.5), (10 / CANVAS, 0.40)),)) as mark:
        result = qa.evaluate_icon(new_icon(mark, palette), palette, mark=mark)
        record("thin", {"measured": result["measured"], "failed": result["failed_legs"]})
        check("thinnest_stroke_px" in result["failed_legs"],
              "a 10px feature fails the stroke floor",
              f"thinnest={result['measured']['thinnest_stroke_px']}px "
              f"< {design.MIN_FEATURE * CANVAS:.0f}px")
        check("min_feature_at_48dp_px" in result["failed_legs"],
              "and the same feature fails at 48dp",
              f"{result['measured']['min_feature_at_48dp_px']}px at 48dp")

    # Drawn in the field's own colour: legible in the pack, invisible in a launcher cell.
    washed = dict(palette)
    washed["paper"] = palette["ink_lift"]
    washed["ink_lift"] = palette["ink_lift"]
    result = qa.evaluate_icon(new_icon(book, washed), washed, mark=book)
    record("washed", {"contrast": result["measured"]["downscale_contrast_48px"],
                      "failed": result["failed_legs"]})
    check("downscale_contrast_48px" in result["failed_legs"],
          "a mark drawn in the field's colour fails the 48dp contrast leg",
          f"contrast={result['measured']['downscale_contrast_48px']}:1")

    # A transparent, rounded corner: how a masked icon is smuggled into a store pack.
    masked = new_icon(book, palette).copy()
    keep = Image.new("L", (CANVAS, CANVAS), 0)
    ImageDraw.Draw(keep).rounded_rectangle((0, 0, CANVAS - 1, CANVAS - 1), radius=96, fill=255)
    alpha = np.asarray(keep)
    masked[..., 3] = np.minimum(masked[..., 3], alpha)
    result = qa.evaluate_icon(masked, palette, mark=book)
    record("rounded_mask", {"alpha_min": result["measured"]["alpha_min"],
                            "failed": result["failed_legs"]})
    check("alpha_min" in result["failed_legs"],
          "an icon with a rounded transparent corner fails the opacity leg",
          f"min alpha={result['measured']['alpha_min']} corner={result['measured']['alpha_min_corner']}")


# ---------------------------------------------------------------------------------------
# 3. The pre-change renderer
# ---------------------------------------------------------------------------------------


def verify_legacy_renderer(rev: str) -> None:
    print(f"\n-- the pre-change renderer from git ({rev}) --")
    module, problem = legacy_renderer(rev)
    if module is None:
        check(False, f"the pre-change renderer could not be loaded: {problem}")
        return
    listing = {"title": "icon-design probe"}
    counts = {}
    for mark in design.MARK_REGISTRY:
        if mark.startswith("test-"):
            continue
        module.CURRENT_MARK = mark
        module.apply_palette(module.resolve_palette(None))
        image = module.render_icon(listing, {"mark": mark})
        palette = module.resolve_palette(None)
        array = np.asarray(image.convert("RGBA"), dtype=np.uint8)
        result = qa.evaluate_icon(array, palette, mark=mark)
        counts[mark] = result["failed_legs"]
        image.save(out_dir() / f"legacy-{mark}-512.png")
        print(f"    {mark}: {len(result['failed_legs'])} gated legs fail -> "
              f"{result['failed_legs']}")
    record("legacy_renderer", {"rev": rev, "failed": counts})
    worst = max(len(value) for value in counts.values())
    check(worst >= 5, "the pre-change renderer fails the design gate on every mark",
          f"{counts}")


def verify_slot_render_round_trip(slot: Path) -> None:
    """Drive the studio's own renderer end to end, into a scratch slot - never into a real one."""
    print("\n-- the studio's own renderer, into a scratch slot --")
    studio = load_module("pk_studio_suite", HERE / "store-studio.py")
    studio_spec = json.loads((slot / "store" / "studio.json").read_text(encoding="utf-8"))
    palette = studio.resolve_palette(studio_spec.get("palette"))
    studio.apply_palette(palette)
    colours = {role: studio.hex_of(colour) for role, colour in palette.items()}
    checks: dict[str, dict] = {}
    for mark in design.MARK_REGISTRY:
        if mark.startswith("test-"):
            continue
        studio.CURRENT_MARK = mark
        image = studio.render_icon({"title": "probe"}, {"mark": mark})
        array = np.asarray(image.convert("RGBA"), dtype=np.uint8)
        result = qa.evaluate_icon(array, palette, mark=mark)
        checks[mark] = {"verdict": result["verdict"], "failed": result["failed_legs"]}
        image.save(out_dir() / f"studio-icon-512-{mark}.png")
        image.convert("RGBA").resize((48, 48), Image.Resampling.LANCZOS).save(
            out_dir() / f"studio-icon-48px-{mark}.png")
        digs = []
        for attempt in ("first", "second"):
            res = out_dir() / f"scratch-slot-{mark}-{attempt}"
            studio.write_vectors(res, colours)
            digs.append(hashlib.sha256(
                (res / "drawable" / "ic_launcher_foreground.xml").read_bytes()).hexdigest())
        check(digs[0] == digs[1], f"{mark}: the launcher foreground drawable is deterministic",
              f"sha256={digs[0][:16]}")
        for name, preview in studio.adaptive_previews().items():
            preview.save(out_dir() / f"preview-{mark}-{Path(name).stem}.png")
    record("studio_render", checks)
    check(all(value["verdict"] == "PASS" for value in checks.values()),
          "the studio renders and gates every mark without a failing leg",
          f"{ {name: value['failed'] for name, value in checks.items()} }")

    # The enforcement, not just the report: a mark the gate rejects must stop the render, because a
    # gate that only prints is a comment. This is the same oversized geometry the sensors were shown
    # catching above, pushed through the studio's own entry point.
    print(f"\n-- {slot.name}: the studio refuses a failing icon --")
    factor = LEGACY_MARK_REACH_FRACTION / design.mark_reach(book := "mark-book")
    with injected("oversized-refused", transform(design.mark_primitives(book), scale=factor)) as mark:
        studio.CURRENT_MARK = mark
        try:
            studio.render_icon({"title": "probe"}, {"mark": mark})
        except SystemExit as error:
            message = str(error)
            check("BLOCKED" in message and "failed" in message,
                  "render_icon raises instead of writing an icon that fails the gate",
                  " ".join(message.split())[:150])
        else:
            check(False, "render_icon raises instead of writing an icon that fails the gate",
                  "it returned an image")
        finally:
            studio.CURRENT_MARK = design.DEFAULT_MARK


# ---------------------------------------------------------------------------------------
# main
# ---------------------------------------------------------------------------------------


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--slot", default="2026-09-26-teacher-gradebook",
                        help="slot whose palette the renders use (read-only)")
    parser.add_argument("--out", default=str(ROOT / ".kiln" / "icon-design" / "suite"))
    parser.add_argument("--legacy-rev", default=None,
                        help="git revision holding the pre-change store-studio.py (e.g. HEAD)")
    parser.add_argument("--json", default=None, help="write the measurements here")
    options = parser.parse_args()

    matches = sorted(path for path in (ROOT / "apps").iterdir()
                     if path.is_dir() and (path.name == options.slot
                                           or path.name.endswith(f"-{options.slot}")))
    if len(matches) != 1:
        raise SystemExit(f"BLOCKED: slot '{options.slot}' resolved to {[m.name for m in matches]}")
    slot = matches[0]
    output = Path(options.out)
    output.mkdir(parents=True, exist_ok=True)
    measured["_out"] = str(output)
    palette = slot_palette(slot)
    marks = [name for name in design.MARK_REGISTRY if not name.startswith("test-")]
    print(f"slot: {slot.name}  palette roles: {len(palette)}  marks: {', '.join(marks)}")
    print(f"scratch renders: {output}")

    for mark in marks:
        design.assert_mark_on_grid(mark)  # raises if the mark is off the grid at all
        check(True, f"{mark}: declared on the grid (weights, keyline, optical centre, margin)")
        result = verify_design(mark, palette)
        verify_mask_survival(mark, result)
        verify_geometry_legs(mark, result)
        verify_opacity(mark, new_icon(mark, palette), palette)
        verify_determinism(mark, palette)
        verify_renderers_agree(mark, result)
        verify_identity(mark, palette)
        Image.fromarray(new_icon(mark, palette)).save(output / f"icon-{mark}-512.png")
        Image.fromarray(new_icon(mark, palette)).resize((48, 48), Image.Resampling.LANCZOS).save(
            output / f"icon-{mark}-48px.png")

    verify_field_legs(palette)
    verify_mark_legs(palette)
    verify_slot_render_round_trip(slot)
    if options.legacy_rev:
        verify_legacy_renderer(options.legacy_rev)

    if options.json:
        Path(options.json).parent.mkdir(parents=True, exist_ok=True)
        Path(options.json).write_text(json.dumps(measured, indent=2, sort_keys=True), encoding="utf-8")
        print(f"\nmeasurements written to {options.json}")
    print(f"\nVERDICT: {'FAIL' if failures else 'PASS'}"
          f" ({len(failures)} failing leg{'s' if len(failures) != 1 else ''})")
    for failure in failures:
        print(f"  failed: {failure}")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
