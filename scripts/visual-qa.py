#!/usr/bin/env python3
"""Independent visual QA for a PlayKiln slot's store assets.

A verifier, not a generator: it never writes to a slot.  Every claim it makes is measured
from the pixels on disk, and every claim states what would have falsified it.

Sections
--------
rules        the Play limits, parsed out of ``scripts/lib/play-assets.mjs`` and imported
             from ``render.py``, plus the divergences between the two
attribution  which renderer produced each shipped asset (byte comparison of both candidates)
inventory    size, mode, alpha, byte budget, locale-copy identity, sha256 revision
fidelity     is the screen region inside each mock-up the real raw capture?  The region is
             predicted from the studio's own declared geometry, then (a) the residual against
             a Lanczos rescale of the matching raw is measured, (b) the prediction is checked
             by a local search that must find its minimum at the prediction, and (c) controls
             (other raws, bilinear, cover-crop, the synthetic screen) are measured.
composition  non-blank screen, caption ink, edge clipping, outer-margin intrusion,
             pairwise similarity of the six phone shots, caption string identity
thumbnails   ink coverage / stroke width of the caption at 1080, 360, 180 and 90 px wide
contrast     WCAG ratios for the caption and the icon mark against their local fields
palette      shipped pixels vs the app's Kotlin palette and the listing's declared colours
icon         adaptive-mask clipping (72dp circle, squircle, 66dp safe zone) and a 48px render

Usage
-----
    python scripts/visual-qa.py --slot pipe-sizing
    python scripts/visual-qa.py --slot pipe-sizing --json .kiln/visual-qa.json
    python scripts/visual-qa.py --slot pipe-sizing --only fidelity,contrast

Candidate renders used for the attribution test are written to a temporary directory
outside the repository and deleted afterwards.
"""

from __future__ import annotations

import argparse
import hashlib
import importlib.util
import io
import contextlib
import json
import re
import shutil
import sys
import tempfile
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT / "scripts") not in sys.path:
    sys.path.insert(0, str(ROOT / "scripts"))
from lib import colours  # noqa: E402  the one colour-maths implementation, shared with icon_qa
from lib import icon_design as design  # noqa: E402  the keyline grid, the stroke scale, the marks
from lib import icon_qa  # noqa: E402  the icon measurements this report publishes
sys.path.insert(0, str(Path(__file__).resolve().parent))
from lib.scratch import SCRATCH_DIR

# The names this module has always exported are kept as aliases of the shared primitives, so there
# is one implementation while every existing call site keeps reading the name it already used.
LUMA = colours.LUMA
luminance = colours.relative_luminance
srgb_to_linear = colours.srgb_to_linear
contrast_ratio = colours.contrast_ratio
hex_of = colours.hex_of
parse_hex = colours.parse_hex


# ----------------------------------------------------------------------------- helpers

def load_module(name: str, path: Path):
    spec = importlib.util.spec_from_file_location(name, path)
    if spec is None or spec.loader is None:
        raise SystemExit(f"BLOCKED: cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module
    spec.loader.exec_module(module)
    return module


def sha256(path: Path) -> str:
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def rounded_mask(size, radius: int) -> np.ndarray:
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, size[0] - 1, size[1] - 1),
                                           radius=max(1, radius), fill=255)
    return np.asarray(mask) > 127


def resize_lanczos(raw: np.ndarray, size: tuple[int, int]) -> np.ndarray:
    """Exactly the operation the studio performs on a capture: RGB Lanczos, no crop."""
    return np.asarray(Image.fromarray(np.ascontiguousarray(raw[..., :3]).astype(np.uint8),
                                      "RGB").resize(size, Image.Resampling.LANCZOS),
                      dtype=np.float32)


def resize_bilinear(raw: np.ndarray, size: tuple[int, int]) -> np.ndarray:
    return np.asarray(Image.fromarray(np.ascontiguousarray(raw[..., :3]).astype(np.uint8),
                                      "RGB").resize(size, Image.Resampling.BILINEAR),
                      dtype=np.float32)


def cover_transform(raw: np.ndarray, target: tuple[int, int]) -> np.ndarray:
    """render.cover(): scale to fill, then centre-crop."""
    height, width = raw.shape[:2]
    target_w, target_h = target
    scale = max(target_w / width, target_h / height)
    resized = Image.fromarray(np.ascontiguousarray(raw[..., :3]).astype(np.uint8), "RGB").resize(
        (round(width * scale), round(height * scale)), Image.Resampling.LANCZOS)
    left = (resized.width - target_w) // 2
    top = (resized.height - target_h) // 2
    return np.asarray(resized.crop((left, top, left + target_w, top + target_h)), dtype=np.float32)


def run_lengths(bits: np.ndarray) -> np.ndarray:
    idx = np.flatnonzero(np.diff(np.concatenate(([False], bits, [False])).astype(np.int8)))
    return idx[1::2] - idx[0::2]


def load_rgb(path: Path) -> np.ndarray:
    with Image.open(path) as image:
        return np.asarray(image.convert("RGB"), dtype=np.uint8)


def theme_dir(slot: Path) -> Path:
    """The slot's own ui/theme directory, discovered instead of assumed.

    This path used to be written out as app/src/main/java/com/gexetr/pipesizing/ui/theme - the
    first slot's namespace, baked into the tool. Every later slot therefore died with a
    FileNotFoundError inside the palette section rather than being measured, so the rule telling
    an operator to run this gate could not be obeyed by any slot but the one it was written for.
    A slot names its own package (`.cursor/rules/project-conventions.mdc`), so the file is found
    by looking for it. Exactly one match is required: two would mean the measurement could grade
    a theme the app does not use, and that is a failure, not a coin toss.
    """
    source_root = slot / "app" / "src" / "main"
    patterns = ("java/**/ui/theme/Color.kt", "kotlin/**/ui/theme/Color.kt")
    matches = sorted(path for pattern in patterns for path in source_root.glob(pattern))
    if len(matches) != 1:
        raise SystemExit(
            "BLOCKED: expected exactly one Color.kt under app/src/main/{java,kotlin}/**/ui/theme "
            "under " + str(slot) + ", "
            "found " + str(len(matches)) + ": "
            + str([p.relative_to(slot).as_posix() for p in matches])
            + " - the palette measurement needs the slot's own theme, so name it unambiguously.")
    return matches[0].parent


# ----------------------------------------------------------------------------- rules

def input_files(root: Path, slot: Path) -> list[Path]:
    """Every file whose content a measurement in this tool depends on."""
    store = slot / "store"
    files = [root / "scripts" / "store-studio.py", root / "scripts" / "render.py",
             root / "scripts" / "lib" / "play-assets.mjs",
             store / "studio.json", store / "journey.json", store / "listing.json"]
    files += sorted((store / "captures" / "raw").glob("*.png"))
    files += sorted((store / "assets").rglob("*.png"))
    theme = theme_dir(slot)
    files += sorted(theme.glob("*.kt"))
    files += sorted((slot / "app" / "src" / "main" / "res" / "drawable").glob("ic_launcher_*.xml"))
    return [p for p in files if p.is_file()]


def hash_manifest(root: Path, slot: Path) -> dict:
    return {p.relative_to(root).as_posix(): sha256(p) for p in input_files(root, slot)}


def section_rules(kit, out: dict) -> dict:
    js = (ROOT / "scripts" / "lib" / "play-assets.mjs").read_text(encoding="utf-8")

    def num(pattern: str):
        match = re.search(pattern, js)
        return float(match.group(1)) if match else None

    def text(pattern: str):
        match = re.search(pattern, js)
        return match.group(1) if match else None

    mjs = {
        "APP_ICON": {"size": [num(r"APP_ICON = Object\.freeze\(\{ width: (\d+)"),
                              num(r"APP_ICON = Object\.freeze\(\{ width: \d+, height: (\d+)")],
                    "maxBytes": num(r"APP_ICON = Object\.freeze\(\{[\s\S]{0,120}?maxBytes: (\d+)"),
                    "alpha": text(r"APP_ICON = Object\.freeze\(\{[\s\S]{0,120}?alpha: (\w+)")},
        "FEATURE_GRAPHIC": {"size": [num(r"FEATURE_GRAPHIC = Object\.freeze\(\{ width: (\d+)"),
                                     num(r"FEATURE_GRAPHIC = Object\.freeze\(\{ width: \d+, height: (\d+)")],
                            "alpha": text(r"FEATURE_GRAPHIC[\s\S]{0,160}?alpha: (\w+)"),
                            "format": text(r"FEATURE_GRAPHIC[\s\S]{0,160}?format: '(\w+)'")},
        "PHONE_SCREENSHOTS": {
            "min": num(r"PHONE_SCREENSHOTS = Object\.freeze\(\{\s*min: (\d+)"),
            "max": num(r"PHONE_SCREENSHOTS = Object\.freeze\(\{\s*min: \d+,\s*max: (\d+)"),
            "target": num(r"PHONE_SCREENSHOTS[\s\S]{0,120}?target: (\d+)"),
            "minSide": num(r"PHONE_SCREENSHOTS[\s\S]{0,220}?min(?:Short)?Side: (\d+)"),
            "maxSide": num(r"PHONE_SCREENSHOTS[\s\S]{0,220}?max(?:Long)?Side: (\d+)"),
            "maxAspectRatio": num(r"PHONE_SCREENSHOTS[\s\S]{0,220}?maxAspectRatio: (\d+)"),
            "maxBytes": num(r"PHONE_SCREENSHOTS[\s\S]{0,240}?maxBytes: (\d+)"),
            "recommended": [num(r"recommended: Object\.freeze\(\{ width: (\d+)"),
                            num(r"recommended: Object\.freeze\(\{ width: \d+, height: (\d+)")],
            "largeFormatMinCount": num(r"largeFormat: Object\.freeze\(\{ minCount: (\d+)"),
        },
        "TABLET_7": {
            "min": num(r"TABLET_7 = Object\.freeze\(\{ min: (\d+)"),
            "max": num(r"TABLET_7[\s\S]{0,80}?max: (\d+)"),
            "minSide": num(r"TABLET_7[\s\S]{0,220}?min(?:Short)?Side: (\d+)"),
            "maxSide": num(r"TABLET_7[\s\S]{0,220}?max(?:Long)?Side: (\d+)"),
            "maxBytes": num(r"TABLET_7[\s\S]{0,240}?maxBytes: (\d+)"),
        },
        "TABLET_10": {
            "min": num(r"TABLET_10 = Object\.freeze\(\{ min: (\d+)"),
            "max": num(r"TABLET_10[\s\S]{0,80}?max: (\d+)"),
            "minSide": num(r"TABLET_10[\s\S]{0,220}?min(?:Short)?Side: (\d+)"),
            "maxSide": num(r"TABLET_10[\s\S]{0,220}?max(?:Long)?Side: (\d+)"),
            "maxBytes": num(r"TABLET_10[\s\S]{0,240}?maxBytes: (\d+)"),
        },
    }
    py = {
        "SCREEN_TARGETS": {k: {"count": v[0], "size": list(v[1])} for k, v in kit.SCREEN_TARGETS.items()},
        "SCREENSHOT_LIMITS": {k: dict(v) for k, v in kit.SCREENSHOT_LIMITS.items()},
        "MAX_PNG_BYTES": kit.MAX_PNG_BYTES,
    }

    kind_to_mjs = {"phone": "PHONE_SCREENSHOTS", "tablet-7": "TABLET_7", "tablet-10": "TABLET_10"}

    # The console's own per-field windows, measured live in the RU console on 2026-09-29 from each
    # screenshot field's inline text (`Скриншоты для смартфона` / `... семидюймовых планшетов` /
    # `... 10-дюймового планшета`, "каждая сторона ..."). These are the check's expectation - a
    # check that cannot state what it expects cannot fail - and NOT a second source of truth for
    # what the factory renders: `SCREEN_TARGETS`/`SCREENSHOT_LIMITS`/`SCREENSHOT_ASPECT_RATIOS` in
    # render.py remain that.
    console_windows = {"phone": (320, 3840), "tablet-7": (320, 3840), "tablet-10": (1080, 7680)}
    aspects = kit.SCREENSHOT_ASPECT_RATIOS

    divergences = []
    for kind, key in kind_to_mjs.items():
        table = py["SCREENSHOT_LIMITS"][kind]
        if mjs[key]["minSide"] != table["min_short"]:
            divergences.append(f"{kind}: play-assets.mjs min side {mjs[key]['minSide']} "
                               f"vs render.py min_short {table['min_short']}")
        if mjs[key]["maxSide"] != table["max_long"]:
            divergences.append(f"{kind}: play-assets.mjs max side {mjs[key]['maxSide']} "
                               f"vs render.py max_long {table['max_long']} "
                               f"(render.py is the stricter, documented intersection with Play's 3840px cap)")
        if (table["min_short"], table["max_long"]) != console_windows[kind]:
            divergences.append(
                f"{kind}: render.py window is {table['min_short']}..{table['max_long']} but the "
                f"console's own field accepts {console_windows[kind][0]}..{console_windows[kind][1]}")
        width, height = py["SCREEN_TARGETS"][kind]["size"]
        if not any(width * down == height * across or width * across == height * down
                   for across, down in aspects):
            divergences.append(
                f"{kind}: factory target {width}x{height} is not "
                + " or ".join(f"{across}:{down}" for across, down in aspects)
                + " (the console's aspect rule for every screenshot field)")

    # What the slot gate actually enforces, read out of verify.mjs rather than assumed.
    gate_source = (ROOT / "scripts" / "verify.mjs").read_text(encoding="utf-8")
    gate_lower = "PHONE_SCREENSHOTS.largeFormat.minCount" in gate_source
    enforced = {
        "phone": {
            "play_assets_min": int(mjs["PHONE_SCREENSHOTS"]["min"]),
            "play_assets_max": int(mjs["PHONE_SCREENSHOTS"]["max"]),
            "large_format_min_count": int(mjs["PHONE_SCREENSHOTS"]["largeFormatMinCount"]),
            "verify_slot_gate_lower_bound": ("largeFormat.minCount (4)" if gate_lower else None),
            "factory_target": py["SCREEN_TARGETS"]["phone"]["count"],
        },
    }
    for kind, key in (("tablet-7", "TABLET_7"), ("tablet-10", "TABLET_10")):
        enforced[kind] = {"play_assets_min": int(mjs[key]["min"]), "play_assets_max": int(mjs[key]["max"]),
                          "large_format_min_count": None, "verify_slot_gate_lower_bound": None,
                          "factory_target": py["SCREEN_TARGETS"][kind]["count"]}
    if gate_lower:
        divergences.append("phone: play-assets.mjs allows PHONE_SCREENSHOTS.min = "
                           f"{int(mjs['PHONE_SCREENSHOTS']['min'])} but scripts/verify.mjs enforces "
                           "PHONE_SCREENSHOTS.largeFormat.minCount = "
                           f"{int(mjs['PHONE_SCREENSHOTS']['largeFormatMinCount'])}; the two disagree and "
                           "the gate is the stricter, so 4-8 is the rule the factory ships against")

    out["rules"] = {"play_assets_mjs": mjs, "render_py": py, "divergences": divergences,
                    "enforced": enforced}
    return out["rules"]


# ----------------------------------------------------------------------------- attribution

def section_attribution(kit, studio, slot: Path, out: dict) -> dict:
    store = slot / "store"
    listing = json.loads((store / "listing.json").read_text(encoding="utf-8-sig"))
    spec = json.loads((store / "studio.json").read_text(encoding="utf-8-sig"))
    journey = json.loads((store / "journey.json").read_text(encoding="utf-8-sig"))
    captions = listing["locales"]["en-US"]["screenshotCaptions"]
    raw1 = kit.load_raw_capture(store / "captures" / "raw", 1)

    delegation = {
        "store_studio_py_exists": (ROOT / "scripts" / "store_studio.py").is_file(),
        "store_studio_hyphen_exists": (ROOT / "scripts" / "store-studio.py").is_file(),
        "store_studio_declared": (store / "studio.json").is_file(),
        "load_studio_renderer_returned": None,
        "stdout": [],
    }
    buffer = io.StringIO()
    with contextlib.redirect_stdout(buffer):
        renderer = kit.load_studio_renderer(store)
    delegation["load_studio_renderer_returned"] = "renderer" if renderer is not None else None
    delegation["stdout"] = [line for line in buffer.getvalue().splitlines() if line.strip()]

    tmp = Path(tempfile.mkdtemp(prefix="pk-visual-qa-", dir=SCRATCH_DIR))
    try:
        kit.render_icon(tmp / "icon-generic.png", listing)
        kit.render_feature(tmp / "feature-generic.png", listing, listing.get("defaultLocale", "en-US"))
        studio.render_icon(listing, spec).save(tmp / "icon-studio.png", format="PNG",
                                               optimize=True, compress_level=9)
        studio.render_feature(listing, spec, raw1.convert("RGB")).save(
            tmp / "feature-studio.png", format="PNG", optimize=True, compress_level=9)
        candidates = {
            "icon": {"generic": tmp / "icon-generic.png", "studio": tmp / "icon-studio.png"},
            "feature": {"generic": tmp / "feature-generic.png", "studio": tmp / "feature-studio.png"},
        }
        items = compose_items(spec, journey)
        for kind, (count, size) in kit.SCREEN_TARGETS.items():
            for index in range(1, count + 1):
                raw = kit.load_raw_capture(store / "captures" / "raw", index)
                name = f"{kind}-{index:02d}"
                entry = candidates.setdefault("screenshot:" + name, {})
                kit.frame_screen(raw, size, captions[index - 1], listing).convert("RGB").save(
                    tmp / f"{name}-generic.png", format="PNG", optimize=True, compress_level=9)
                entry["generic"] = tmp / f"{name}-generic.png"
                studio.render_screenshot(size, items[(index - 1) % len(items)], raw).save(
                    tmp / f"{name}-studio.png", format="PNG", optimize=True, compress_level=9)
                entry["studio"] = tmp / f"{name}-studio.png"

        rows = []
        for name, entry in sorted(candidates.items()):
            if name == "icon":
                shipped = store / "assets" / "icon-512.png"
            elif name == "feature":
                shipped = store / "assets" / "feature-graphic-1024x500.png"
            else:
                shipped = store / "assets" / "en-US" / f"{name.split(':')[1]}.png"
            row = {"component": name, "shipped": shipped.relative_to(slot).as_posix()}
            for renderer_name, candidate in entry.items():
                cand = load_rgb(candidate)
                ship = load_rgb(shipped)
                if cand.shape != ship.shape:
                    row[renderer_name] = {"shape_mismatch": [list(cand.shape), list(ship.shape)]}
                    continue
                diff = np.abs(cand.astype(np.int16) - ship.astype(np.int16))
                row[renderer_name] = {
                    "bytes_equal": sha256(candidate) == sha256(shipped),
                    "pixels_equal": bool((diff == 0).all()),
                    "mae": round(float(diff.mean()), 4),
                    "max_abs_diff": int(diff.max()),
                }
            rows.append(row)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)

    out["attribution"] = {"studio_delegation": delegation, "components": rows}
    return out["attribution"]


def compose_items(spec: dict, journey: dict) -> list[dict]:
    """Reproduce store-studio.render_slot's per-shot copy, in its declared order."""
    shots = journey.get("shots", [])[:6]
    headlines = spec.get("headlines") or []
    kickers = spec.get("kickers") or []
    items = []
    for index, shot in enumerate(shots):
        items.append({
            "headline": headlines[index] if index < len(headlines) else shot.get("title", ""),
            "body": shot.get("body", ""),
            "kicker": kickers[index] if index < len(kickers) else "",
        })
    return [item for item in items if item["headline"]]


# ----------------------------------------------------------------------------- inventory

def section_inventory(kit, slot: Path, out: dict) -> dict:
    store = slot / "store"
    assets = store / "assets"
    expected = {
        (assets / "icon-512.png"): ("icon", (512, 512)),
        (assets / "feature-graphic-1024x500.png"): ("feature", (1024, 500)),
        (assets / "en-US" / "icon-512.png"): ("icon", (512, 512)),
        (assets / "en-US" / "feature-graphic-1024x500.png"): ("feature", (1024, 500)),
    }
    for kind, (count, size) in kit.SCREEN_TARGETS.items():
        for index in range(1, count + 1):
            expected[assets / "en-US" / f"{kind}-{index:02d}.png"] = (kind, size)

    rows = []
    for path, (kind, size) in expected.items():
        rel = path.relative_to(slot).as_posix()
        row = {"path": rel, "kind": kind, "expected": list(size)}
        if not path.is_file():
            row["verdict"] = "MISSING"
            rows.append(row)
            continue
        row["bytes"] = path.stat().st_size
        row["sha256"] = sha256(path)
        row["sha256_16"] = row["sha256"][:16]
        with Image.open(path) as image:
            image.load()
            row["size"] = [image.width, image.height]
            row["mode"] = image.mode
            row["format"] = image.format
            array = np.asarray(image.convert("RGBA"), dtype=np.uint8)
        alpha = array[..., 3]
        row["alpha_min"] = int(alpha.min())
        row["alpha_min_corner"] = int(min(alpha[0, 0], alpha[0, -1], alpha[-1, 0], alpha[-1, -1]))
        row["alpha_all_opaque"] = bool((alpha == 255).all())

        failures = []
        if kind in ("phone", "tablet-7", "tablet-10"):
            failures += kit.screenshot_rule_failures(kind, (row["size"][0], row["size"][1]))
            if row["bytes"] > kit.MAX_PNG_BYTES:
                failures.append(f"bytes {row['bytes']} over {kit.MAX_PNG_BYTES}")
            if row["mode"] != "RGB":
                failures.append(f"mode {row['mode']} is not RGB (Play wants 24-bit PNG, no alpha)")
        elif kind == "icon":
            if row["size"] != [512, 512]:
                failures.append("not 512x512")
            if row["bytes"] > 1024 * 1024:
                failures.append("over 1024 KB")
            if row["mode"] != "RGBA":
                failures.append(f"mode {row['mode']} is not RGBA (Play: 32-bit PNG with alpha)")
            if row["alpha_min"] == 0:
                failures.append("has fully transparent pixels")
        else:
            if row["size"] != [1024, 500]:
                failures.append("not 1024x500")
            if row["mode"] != "RGB":
                failures.append(f"mode {row['mode']} carries alpha")
            if row["alpha_min"] != 255:
                failures.append("has transparent pixels")
        row["failures"] = failures
        row["verdict"] = "PASS" if not failures else "FAIL"
        rows.append(row)

    # How many of each kind actually shipped, against Play's range and the pack target.
    counts = {}
    for kind, (count, size) in kit.SCREEN_TARGETS.items():
        present = sum(1 for r in rows if r["kind"] == kind and r.get("size"))
        limits = kit.SCREENSHOT_LIMITS[kind]
        counts[kind] = {
            "present": present,
            "factory_target": count,
            "play_min": 2 if kind == "phone" else 4,
            "play_max": 8,
            "within_play_range": (2 if kind == "phone" else 4) <= present <= 8,
            "within_4_to_8": 4 <= present <= 8,
            "matches_factory_target": present == count,
            "min_short_side_required": limits["min_short"],
            "max_long_side_allowed": limits["max_long"],
        }

    out["inventory"] = {
        "rows": rows,
        "counts": counts,
        "locale_copies_identical": {
            name: sha256(assets / name) == sha256(assets / "en-US" / name)
            for name in ("icon-512.png", "feature-graphic-1024x500.png")
        },
        "extra_png": sorted(p.relative_to(slot).as_posix() for p in assets.rglob("*.png")
                            if p not in expected),
    }
    return out["inventory"]


# ----------------------------------------------------------------------------- geometry

def predict_geometry(studio, item: dict, size: tuple[int, int], raw: Image.Image) -> dict:
    """Where the studio's own declared geometry puts the device and its screen.

    This is a *prediction*, not evidence: it is derived from the constants in
    store-studio.render_screenshot.  The measurement below (a bit-exact residual) is what
    proves it right, and the local search is what proves the residual is not an accident of
    a mislocated region.
    """
    width, height = size
    scale = height / 1920
    scratch = ImageDraw.Draw(Image.new("L", (8, 8)))
    margin = round(84 * scale)
    cursor = round(96 * scale)
    if item.get("kicker"):
        # the amber tick is drawn at cursor+5..cursor+9 and the kicker text at margin+42
        cursor += round(58 * scale)
    headline_face = studio.font(round(72 * scale), bold=True)
    headline_lines = studio.wrap(scratch, item.get("headline", ""), headline_face, width - 2 * margin)[:2]
    cursor += round(86 * scale) * len(headline_lines)
    cursor += round(10 * scale)
    body_face = studio.font(round(32 * scale))
    body_lines = studio.wrap(scratch, item.get("body", ""), body_face, width - 2 * margin)[:2]
    cursor += round(45 * scale) * len(body_lines)

    bezel = max(10, round(17 * scale))
    radius = round(52 * scale)
    available = height - cursor - round(52 * scale) - round(56 * scale)
    aspect = raw.width / raw.height
    clamped = False
    outer_height = available
    if (available - 2 * bezel) * aspect + 2 * bezel > width - 2 * margin:
        clamped = True
        outer_width = width - 2 * margin
        outer_height = round((outer_width - 2 * bezel) / aspect) + 2 * bezel
    inner_height = outer_height - 2 * bezel
    inner_width = round(inner_height * aspect)
    device_width = inner_width + 2 * bezel
    device_height = outer_height
    x = (width - device_width) // 2
    y = cursor + round(52 * scale)
    return {
        "scale": scale,
        "margin": margin,
        "cursor_after_copy": cursor,
        "headline_lines": len(headline_lines),
        "body_lines": len(body_lines),
        "bezel": bezel,
        "corner_radius": radius,
        "screen_mask_radius": max(4, radius - bezel),
        "width_clamped": clamped,
        "available_height": available,
        "device_rect": [x, y, device_width, device_height],
        "screen_rect": [x + bezel, y + bezel, inner_width, inner_height],
        "capture_aspect": round(aspect, 6),
        "screen_scale_x": round(inner_width / raw.width, 6),
        "screen_scale_y": round(inner_height / raw.height, 6),
    }


def edge_localisation(canvas: np.ndarray, rect: list[int], span: int = 14) -> dict:
    """Locate the device's four edges from pixels alone, independently of the studio's maths.

    The device is an opaque rounded rectangle of BEZEL over a smooth gradient field, so the
    largest first difference across each edge marks the true boundary.  For each edge the
    search scans ``-span..span`` around the predicted coordinate and reports the argmax and the
    edge magnitude, so a wrong prediction shows up as a non-zero offset.
    """
    x, y, w, h = rect
    gray = canvas[..., :3].astype(np.float32)
    result = {}
    # vertical edges: average |I(x+d+1) - I(x+d)| down the middle 60% of the device
    y0, y1 = y + round(h * 0.2), y + round(h * 0.8)
    left = []
    right = []
    for d in range(-span, span + 1):
        column = x + d
        neighbour = x + w + d - 1
        if 1 <= column < gray.shape[1] - 1:
            left.append((float(np.abs(gray[y0:y1, column + 1] - gray[y0:y1, column]).mean()), d))
        if 1 <= neighbour < gray.shape[1] - 1:
            right.append((float(np.abs(gray[y0:y1, neighbour + 1] - gray[y0:y1, neighbour]).mean()), d))
    x0, x1 = x + round(w * 0.2), x + round(w * 0.8)
    top = []
    bottom = []
    for d in range(-span, span + 1):
        row = y + d
        neighbour = y + h + d - 1
        if 1 <= row < gray.shape[0] - 1:
            top.append((float(np.abs(gray[row + 1, x0:x1] - gray[row, x0:x1]).mean()), d))
        if 1 <= neighbour < gray.shape[0] - 1:
            bottom.append((float(np.abs(gray[neighbour + 1, x0:x1] - gray[neighbour, x0:x1]).mean()), d))
    for name, samples, predicted in (("left", left, x), ("right", right, x + w - 1),
                                     ("top", top, y), ("bottom", bottom, y + h - 1)):
        if not samples:
            continue
        ranked = sorted(samples, key=lambda item: -item[0])[:3]
        result[name] = {
            "predicted_coordinate": predicted,
            "strongest_offsets_px": [offset for _, offset in ranked],
            "strongest_magnitudes": [round(magnitude, 2) for magnitude, _ in ranked],
            "magnitude_at_prediction": round(float(np.mean([m for m, d in samples if d == 0])), 2),
            "profile": {str(d): round(m, 2) for m, d in samples},
        }
    return result


def bezel_probe(canvas: np.ndarray, device_rect: list[int], inset: int = 6) -> dict:
    """Sample the BEZEL colour just inside the predicted device border and the field just
    outside it: a correct boundary must show the bezel inside and the gradient outside."""
    x, y, w, h = device_rect
    inside = np.concatenate([
        canvas[y + inset:y + inset + 2, x + inset:x + w - inset].reshape(-1, 3),
        canvas[y + h - inset - 2:y + h - inset, x + inset:x + w - inset].reshape(-1, 3),
        canvas[y + inset:y + h - inset, x + inset:x + inset + 2].reshape(-1, 3),
        canvas[y + inset:y + h - inset, x + w - inset - 2:x + w - inset].reshape(-1, 3)])
    outside = np.concatenate([
        canvas[max(0, y - inset - 2):max(1, y - inset), x:x + w].reshape(-1, 3),
        canvas[y + h + inset:y + h + inset + 2, x:x + w].reshape(-1, 3),
        canvas[y:y + h, max(0, x - inset - 2):max(1, x - inset)].reshape(-1, 3),
        canvas[y:y + h, x + w + inset:x + w + inset + 2].reshape(-1, 3)])
    return {"inside_median_rgb": [int(v) for v in np.median(inside, axis=0)],
            "inside_stdev": round(float(inside.std()), 2),
            "outside_median_rgb": [int(v) for v in np.median(outside, axis=0)],
            "outside_stdev": round(float(outside.std()), 2),
            "inside_median_distance_from_studio_bezel": round(float(np.abs(
                np.median(inside, axis=0) - np.array([13, 18, 27])).max()), 2)}


def corner_confinement(per_pixel: np.ndarray, mask: np.ndarray, radius: int) -> dict:
    """Every pixel that differs from the raw *outside* the rounded-corner mask must sit in one
    of the four corner squares.  If a differing pixel appears on a straight edge, the pasted
    screen is not the rescaled raw everywhere."""
    outside = per_pixel > 0
    outside &= ~mask
    rows, cols = np.nonzero(outside)
    if rows.size == 0:
        return {"differing_outside_mask": 0, "confined_to_corners": True, "bbox": None}
    height, width = per_pixel.shape
    in_corner = ((rows < radius) | (rows >= height - radius)) & \
                ((cols < radius) | (cols >= width - radius))
    return {"differing_outside_mask": int(rows.size),
            "differing_outside_but_not_in_corner": int((~in_corner).sum()),
            "confined_to_corners": bool(in_corner.all()),
            "bbox": [int(cols.min()), int(rows.min()), int(cols.max()) + 1, int(rows.max()) + 1],
            "first_not_in_corner": ([int(cols[~in_corner][0]), int(rows[~in_corner][0])]
                                    if (~in_corner).any() else None)}


def partial_crop_control(raw: np.ndarray, size: tuple[int, int], keep: float = 0.8) -> np.ndarray:
    """A composition that cropped the capture instead of showing all of it: crop the raw to a
    centred ``keep`` band and rescale.  Unlike cover(), this differs from a fit by construction."""
    height, width = raw.shape[:2]
    band_h = max(8, round(height * keep))
    band_w = max(8, round(width * keep))
    top = (height - band_h) // 2
    left = (width - band_w) // 2
    cropped = raw[top:top + band_h, left:left + band_w]
    return np.asarray(Image.fromarray(np.ascontiguousarray(cropped[..., :3]).astype(np.uint8), "RGB")
                      .resize(size, Image.Resampling.LANCZOS), dtype=np.float32)


# ----------------------------------------------------------------------------- fidelity

def section_fidelity(kit, studio, slot: Path, out: dict) -> dict:
    store = slot / "store"
    listing = json.loads((store / "listing.json").read_text(encoding="utf-8-sig"))
    spec = json.loads((store / "studio.json").read_text(encoding="utf-8-sig"))
    journey = json.loads((store / "journey.json").read_text(encoding="utf-8-sig"))
    captions = listing["locales"]["en-US"]["screenshotCaptions"]
    items = compose_items(spec, journey)

    raws = {}
    for index in range(1, 7):
        path = store / "captures" / "raw" / f"{index:02d}.png"
        if path.is_file():
            with Image.open(path) as image:
                raws[index] = image.convert("RGB")

    rows = []
    for kind, (count, size) in kit.SCREEN_TARGETS.items():
        prototype = kit.load_raw_capture(store / "captures" / "raw", 1)
        for index in range(1, count + 1):
            path = store / "assets" / "en-US" / f"{kind}-{index:02d}.png"
            expected_raw = ((index - 1) % 6) + 1
            raw_image = raws[expected_raw]
            item = items[(index - 1) % len(items)]
            canvas = load_rgb(path).astype(np.float32)
            geometry = predict_geometry(studio, item, size, prototype)
            x, y, w, h = geometry["screen_rect"]
            mask = rounded_mask((w, h), geometry["screen_mask_radius"])
            eroded = ndimage.binary_erosion(mask, iterations=3)

            reference = resize_lanczos(np.asarray(raw_image, dtype=np.uint8), (w, h))
            patch = canvas[y:y + h, x:x + w]
            diff = np.abs(patch - reference)
            per_pixel = diff.max(axis=2)
            row = {
                "path": path.relative_to(slot).as_posix(),
                "kind": kind,
                "item_headline": item["headline"],
                "expected_raw": expected_raw,
                "geometry": geometry,
                "reference_is_matching_raw": True,
                "mae_masked": round(float(diff[eroded].mean()), 5),
                "max_abs_diff_masked": float(per_pixel[eroded].max()),
                "pixels_differing_masked": int((per_pixel[eroded] > 0).sum()),
                "masked_pixel_count": int(eroded.sum()),
                "mae_all_pixels": round(float(diff.mean()), 5),
                "max_abs_diff_all": float(per_pixel.max()),
                "corner_confinement": corner_confinement(per_pixel, mask,
                                                         geometry["screen_mask_radius"]),
                "edge_localisation": edge_localisation(canvas, geometry["device_rect"]),
                "bezel_probe": bezel_probe(canvas, geometry["device_rect"]),
            }

            # local search: the prediction must be the minimum, or the prediction is wrong
            best = {"mae": row["mae_masked"], "dx": 0, "dy": 0}
            for dy in (-2, -1, 1, 2):
                for dx in (-2, -1, 1, 2):
                    shifted = canvas[y + dy:y + dy + h, x + dx:x + dx + w]
                    if shifted.shape[:2] != (h, w):
                        continue
                    value = float(np.abs(shifted - reference)[eroded].mean())
                    if value < best["mae"]:
                        best = {"mae": round(value, 5), "dx": dx, "dy": dy}
            row["local_search"] = {
                "predicted_mae": row["mae_masked"], "best_mae": best["mae"],
                "best_offset": [best["dx"], best["dy"]],
                "prediction_is_minimum": best["dx"] == 0 and best["dy"] == 0,
            }

            # scale sweep: rescale the raw, compare the centred overlap. The minimum should sit
            # at the predicted scale, and the sweep width shows how sharp the alignment is.
            sweep = {}
            for factor in (0.90, 0.95, 0.98, 1.00, 1.02, 1.05, 1.10):
                tw, th = round(w * factor), round(h * factor)
                resized = resize_lanczos(np.asarray(raw_image, dtype=np.uint8), (tw, th))
                oh, ow = min(h, th), min(w, tw)
                oy, ox = (h - oh) // 2, (w - ow) // 2
                ry, rx = (th - oh) // 2, (tw - ow) // 2
                window = resized[ry:ry + oh, rx:rx + ow]
                core = patch[oy:oy + oh, ox:ox + ow]
                sub = eroded[oy:oy + oh, ox:ox + ow]
                sweep[f"{factor:.2f}"] = round(float(np.abs(window - core)[sub].mean()), 4)
            row["scale_sweep_mae"] = sweep
            row["sweep_argmin"] = min(sweep, key=sweep.get)

            # controls: same geometry, different resampler / geometry / source
            row["control_bilinear_mae"] = round(
                float(np.abs(resize_bilinear(np.asarray(raw_image, dtype=np.uint8), (w, h)) - patch)[eroded].mean()), 4)
            row["control_cover_crop_mae"] = round(
                float(np.abs(cover_transform(np.asarray(raw_image, dtype=np.uint8), (w, h)) - patch)[eroded].mean()), 4)
            row["control_partial_crop_mae"] = round(
                float(np.abs(partial_crop_control(np.asarray(raw_image, dtype=np.uint8), (w, h)) - patch)[eroded].mean()), 4)
            row["cover_control_is_degenerate"] = bool(
                abs(w / raw_image.width - h / raw_image.height) < 1e-3)
            synthetic = kit.synthetic_screen((w, h), index, captions[index - 1], listing)
            row["control_synthetic_screen_mae"] = round(float(np.abs(
                np.asarray(synthetic.convert("RGB"), dtype=np.float32) - patch)[eroded].mean()), 4)
            others = {}
            for other, other_raw in raws.items():
                if other == expected_raw:
                    continue
                others[str(other)] = round(float(np.abs(
                    resize_lanczos(np.asarray(other_raw, dtype=np.uint8), (w, h)) - patch)[eroded].mean()), 4)
            row["control_mae_other_raws"] = others
            row["best_control_mae"] = min(others.values())
            row["argmin_control_raw"] = int(min(others, key=others.get))
            row["separation_ratio_vs_best_control"] = (
                round(row["best_control_mae"] / row["mae_masked"], 1) if row["mae_masked"] > 0
                else "infinite (residual is exactly 0)")
            row["fidelity_verdict"] = (
                "REAL_CAPTURE" if row["mae_masked"] == 0.0 and row["pixels_differing_masked"] == 0
                else "RESIDUAL_NEEDS_EXPLANATION")
            rows.append(row)
    out["fidelity"] = {"rows": rows}
    return out["fidelity"]


# ----------------------------------------------------------------------------- composition

def text_groups(canvas: np.ndarray, row_limit: int, threshold: float = 140.0,
                gap: int = 10, min_height: int = 6):
    """Contiguous row bands of ink above the device, found from the pixels alone."""
    ink = luminance(canvas[:max(1, row_limit)]) >= threshold
    present = ink.any(axis=1)
    groups = []
    start = None
    for position, value in enumerate(present):
        if value and start is None:
            start = position
        elif not value and start is not None:
            groups.append([start, position])
            start = None
    if start is not None:
        groups.append([start, len(present)])
    merged = []
    for group in groups:
        if merged and group[0] - merged[-1][1] < gap:
            merged[-1][1] = group[1]
        else:
            merged.append(list(group))
    return [(a, b) for a, b in merged if b - a >= min_height]


def band_stats(canvas: np.ndarray, rows: tuple[int, int], tokens: dict,
               threshold: float = 140.0) -> dict:
    top, bottom = rows
    strip = canvas[top:bottom]
    luma = luminance(strip)
    ink = luma >= threshold
    if not ink.any():
        return {"rows": [top, bottom], "ink_px": 0}
    fg = strip[ink].mean(axis=0)
    nearest = None
    for name, rgb in tokens.items():
        delta = [round(float(a) - float(b), 1) for a, b in zip(fg, rgb)]
        distance = max(abs(d) for d in delta)
        if nearest is None or distance < nearest["max_channel_delta"]:
            nearest = {"token": name, "hex": hex_of(rgb), "delta": delta,
                       "max_channel_delta": round(distance, 1)}
    runs = []
    for row_bits in ink:
        if row_bits.any():
            lengths = run_lengths(row_bits)
            if lengths.size:
                runs.append(float(np.median(lengths)))
    ys, xs = np.nonzero(ink)
    return {
        "rows": [top, bottom],
        "ink_rows": [top + int(ys.min()), top + int(ys.max()) + 1],
        "height_px": bottom - top,
        "ink_px": int(ink.sum()),
        "ink_coverage": round(float(ink.mean()), 5),
        "foreground_rgb": [round(float(v), 1) for v in fg],
        "nearest_studio_token": nearest,
        "median_stroke_run_px": round(float(np.median(runs)), 2) if runs else 0.0,
        "ink_bbox": [int(xs.min()), top + int(ys.min()), int(xs.max()), top + int(ys.max())],
    }


def classify_bands(bands: list[dict]) -> dict:
    """Split the caption bands into kicker / headline / body by their measured foreground."""
    kicker = next((b for b in bands if b["nearest_studio_token"]["token"] in ("AMBER", "AMBER_PALE")), None)
    paper = [b for b in bands if b["nearest_studio_token"]["token"] == "PAPER"]
    muted = [b for b in bands if b["nearest_studio_token"]["token"] in ("MUTED", "PAPER")]
    return {"kicker": kicker,
            "headline": max(paper, key=lambda b: b["ink_px"]) if paper else None,
            "body": max((b for b in muted if b is not (max(paper, key=lambda b: b["ink_px"]) if paper else None)),
                        key=lambda b: b["ink_px"], default=None)}


def section_composition(kit, studio, slot: Path, out: dict) -> dict:
    store = slot / "store"
    listing = json.loads((store / "listing.json").read_text(encoding="utf-8-sig"))
    spec = json.loads((store / "studio.json").read_text(encoding="utf-8-sig"))
    journey = json.loads((store / "journey.json").read_text(encoding="utf-8-sig"))
    items = compose_items(spec, journey)
    prototype = kit.load_raw_capture(store / "captures" / "raw", 1)
    tokens = {"PAPER": studio.PAPER, "AMBER": studio.AMBER, "AMBER_PALE": studio.AMBER_PALE,
              "MUTED": studio.MUTED, "CYAN": studio.CYAN}

    rows = []
    thumbs = {}
    for index in range(1, 7):
        path = store / "assets" / "en-US" / f"phone-{index:02d}.png"
        with Image.open(path) as image:
            image.load()
            canvas = load_rgb(path).astype(np.float32)
            thumbs[index] = np.asarray(image.convert("L").resize((135, 240), Image.Resampling.LANCZOS),
                                       dtype=np.float64)
        height, width = canvas.shape[:2]
        geometry = predict_geometry(studio, items[(index - 1) % len(items)], (width, height), prototype)
        dx, dy, dw, dh = geometry["device_rect"]
        x, y, w, h = geometry["screen_rect"]
        screen = canvas[y:y + h, x:x + w]
        luma_screen = luminance(screen)
        unique = len(np.unique((screen // 16).astype(np.uint8).reshape(-1, 3), axis=0))

        bands = [band_stats(canvas, tuple(group), tokens) for group in text_groups(canvas, dy)]
        bands = [b for b in bands if b.get("ink_px")]
        classified = classify_bands(bands)
        headline = classified["headline"]

        ink = luminance(canvas) >= 140
        inside_screen = np.zeros_like(ink)
        inside_screen[y:y + h, x:x + w] = True
        outside_ink = ink & ~inside_screen
        mx, my = round(0.05 * width), round(0.05 * height)
        edge = np.zeros_like(ink)
        edge[:my, :] = True
        edge[-my:, :] = True
        edge[:, :mx] = True
        edge[:, -mx:] = True
        border = np.zeros_like(ink)
        border[:2, :] = True
        border[-2:, :] = True
        border[:, :2] = True
        border[:, -2:] = True

        # copy identity: redraw the declared headline / body with the studio's own font and wrap
        # helpers and compare ink masks.  Not OCR: it proves the pixels match the declared string.
        item = items[(index - 1) % len(items)]
        expected = Image.new("L", (width, height), 0)
        edraw = ImageDraw.Draw(expected)
        scale = height / 1920
        margin = round(84 * scale)
        cursor = round(96 * scale)
        if item["kicker"]:
            cursor += round(58 * scale)
        head_face = studio.font(round(72 * scale), bold=True)
        for line in studio.wrap(edraw, item["headline"], head_face, width - 2 * margin)[:2]:
            edraw.text((margin, cursor), line, font=head_face, fill=255)
            cursor += round(86 * scale)
        cursor += round(10 * scale)
        body_face = studio.font(round(32 * scale))
        for line in studio.wrap(edraw, item["body"], body_face, width - 2 * margin)[:2]:
            edraw.text((margin, cursor), line, font=body_face, fill=255)
            cursor += round(45 * scale)
        rows_below = np.arange(height)[:, None] < dy
        expected_mask = (np.asarray(expected) > 229) & rows_below
        observed_mask = (luminance(canvas) >= 150) & rows_below
        inter = int((expected_mask & observed_mask).sum())
        union = int((expected_mask | observed_mask).sum())

        row = {
            "index": index,
            "declared_kicker": item["kicker"],
            "declared_headline": item["headline"],
            "declared_body": item["body"],
            "device_rect": [dx, dy, dw, dh],
            "screen_rect": [x, y, w, h],
            "geometry": geometry,
            "screen_flush_with_device": [x - dx, y - dy, (dx + dw) - (x + w), (dy + dh) - (y + h)],
            "screen_blank": bool(luma_screen.std() < 3.0),
            "screen_luma_std": round(float(luma_screen.std()), 2),
            "screen_luma_mean": round(float(luma_screen.mean()), 2),
            "screen_unique_quant16_colors": int(unique),
            "screen_frac_dark": round(float((luma_screen < 128).mean()), 4),
            "bands_above_device": bands,
            "band_count": len(bands),
            "headline_present": headline is not None,
            "headline_ink_coverage_of_band": headline["ink_coverage"] if headline else None,
            "headline_rows": headline["ink_rows"] if headline else None,
            "total_text_ink_above_device_px": int(observed_mask.sum()),
            "copy_rerender_iou": round(inter / union, 4) if union else 0.0,
            "copy_extra_ink_px": int((observed_mask & ~expected_mask).sum()),
            "copy_missing_ink_px": int((expected_mask & ~observed_mask).sum()),
            "ink_inside_outer_5pct_margin_px": int((outside_ink & edge).sum()),
            "ink_touching_image_border_px": int((ink & border).sum()),
            "caption_gaps_to_image_edge_px": [geometry["margin"], None, None, None],
            "device_gaps_to_image_edge_px": [dx, dy, width - (dx + dw), height - (dy + dh)],
            "screen_margin_fraction_of_device": round(min(dx, width - (dx + dw)) / dw, 4),
            "screen_area_fraction_of_canvas": round((w * h) / (width * height), 4),
        }
        rows.append(row)

    pairs = []
    for a in range(1, 7):
        for b in range(a + 1, 7):
            ta, tb = thumbs[a], thumbs[b]
            pairs.append({"a": a, "b": b, "mae_135x240": round(float(np.abs(ta - tb).mean()), 3),
                          "pearson_r": round(float(np.corrcoef(ta.ravel(), tb.ravel())[0, 1]), 4)})
    out["composition"] = {
        "rows": rows,
        "pairwise_phone": pairs,
        "pairwise_mae_min": min(p["mae_135x240"] for p in pairs),
        "pairwise_mae_max": max(p["mae_135x240"] for p in pairs),
        "pairwise_corr_max": max(p["pearson_r"] for p in pairs),
        "most_similar_pairs": sorted(pairs, key=lambda p: p["mae_135x240"])[:3],
        "least_similar_pairs": sorted(pairs, key=lambda p: -p["mae_135x240"])[:3],
    }
    return out["composition"]


# ----------------------------------------------------------------------------- thumbnails

def section_thumbnails(kit, studio, slot: Path, out: dict) -> dict:
    store = slot / "store"
    spec = json.loads((store / "studio.json").read_text(encoding="utf-8-sig"))
    journey = json.loads((store / "journey.json").read_text(encoding="utf-8-sig"))
    items = compose_items(spec, journey)
    prototype = kit.load_raw_capture(store / "captures" / "raw", 1)
    tokens = {"PAPER": studio.PAPER, "AMBER": studio.AMBER, "AMBER_PALE": studio.AMBER_PALE,
              "MUTED": studio.MUTED, "CYAN": studio.CYAN}

    rows = []
    for index in range(1, 7):
        path = store / "assets" / "en-US" / f"phone-{index:02d}.png"
        with Image.open(path) as image:
            full = image.convert("RGB")
        height, width = full.size[::-1]
        canvas = np.asarray(full, dtype=np.float32)
        geometry = predict_geometry(studio, items[(index - 1) % len(items)], (width, height), prototype)
        bands = [band_stats(canvas, tuple(g), tokens) for g in text_groups(canvas, geometry["device_rect"][1])]
        bands = [b for b in bands if b.get("ink_px")]
        headline = classify_bands(bands)["headline"]
        body = classify_bands(bands)["body"]
        if headline is None:
            rows.append({"index": index, "error": "no headline band found", "bands": bands})
            continue
        top, bottom = headline["ink_rows"]
        entry = {"index": index,
                 "declared_headline": items[(index - 1) % len(items)]["headline"],
                 "headline_rows_full_size": [top, bottom],
                 "headline_ink_px_full_size": headline["ink_px"],
                 "headline_cap_height_px_full_size": bottom - top,
                 "sizes": {}}
        for target_width in (1080, 360, 180, 90):
            target_height = round(height * target_width / width)
            scaled = np.asarray(full.resize((target_width, target_height), Image.Resampling.LANCZOS),
                                dtype=np.float32)
            factor = target_width / width
            band_top = max(0, int(np.floor(top * factor)) - 1)
            band_bottom = min(target_height, max(band_top + 1, int(np.ceil(bottom * factor)) + 1))
            left = max(0, int(round(geometry["margin"] * factor)))
            right = min(target_width, target_width - left)
            strip = scaled[band_top:band_bottom, left:right]
            strip_luma = luminance(strip)
            fixed = strip_luma > 200
            peak = float(np.percentile(strip_luma, 99.5)) if strip_luma.size else 0.0
            field = float(np.percentile(strip_luma, 50)) if strip_luma.size else 0.0
            adaptive = strip_luma > field + 0.35 * max(0.0, peak - field)
            runs = []
            for row_bits in adaptive:
                if row_bits.any():
                    lengths = run_lengths(row_bits)
                    if lengths.size:
                        runs.append(float(np.median(lengths)))
            entry["sizes"][str(target_width)] = {
                "scale_factor": round(factor, 5),
                "band_px": [int(strip.shape[1]), int(strip.shape[0])],
                "cap_height_px": round((bottom - top) * factor, 2),
                "ink_coverage_fixed_gt200": round(float(fixed.mean()), 5) if fixed.size else 0.0,
                "ink_coverage_adaptive": round(float(adaptive.mean()), 5) if adaptive.size else 0.0,
                "ink_pixels_adaptive": int(adaptive.sum()),
                "median_stroke_run_px": round(float(np.median(runs)), 2) if runs else 0.0,
                "max_stroke_run_px": int(np.max(runs)) if runs else 0,
                "stroke_rows_present": int(len(runs)),
                "expected_stroke_rows": int(round((bottom - top) * factor)),
                "peak_luma": round(peak, 1),
                "field_luma_median": round(field, 1),
                "peak_minus_field": round(peak - field, 1),
                "ink_share_of_full_size": round(float(adaptive.sum()) / max(1, headline["ink_px"]), 5),
            }
        if body is not None:
            btop, bbottom = body["ink_rows"]
            factor = 180 / width
            scaled = np.asarray(full.resize((180, round(height * factor)), Image.Resampling.LANCZOS),
                                dtype=np.float32)
            strip = scaled[max(0, int(btop * factor) - 1):min(scaled.shape[0], int(bbottom * factor) + 2)]
            lum = luminance(strip)
            entry["body_at_180px"] = {
                "cap_height_px": round((bbottom - btop) * factor, 2),
                "peak_luma": round(float(np.percentile(lum, 99.5)), 1) if lum.size else 0.0,
                "field_luma_median": round(float(np.percentile(lum, 50)), 1) if lum.size else 0.0,
                "peak_minus_field": round(float(np.percentile(lum, 99.5)) - float(np.percentile(lum, 50)), 1)
                if lum.size else 0.0,
            }
        rows.append(entry)
    out["thumbnails"] = {"rows": rows}
    return out["thumbnails"]


# ----------------------------------------------------------------------------- contrast

def band_contrast(canvas: np.ndarray, rows: tuple[int, int], pad: int = 6,
                  fg_percentile: float = 97.0, bg_percentile: float = 25.0,
                  floor: float = 150.0) -> dict | None:
    """WCAG ratio between the ink of a text band and the field it is drawn on."""
    top, bottom = rows
    strip = canvas[max(0, top - pad):bottom + pad]
    luma = luminance(strip)
    if luma.size == 0:
        return None
    threshold = max(floor, float(np.percentile(luma, fg_percentile)))
    ink = luma >= threshold
    field = luma <= float(np.percentile(luma, bg_percentile))
    if not ink.any() or not field.any():
        return None
    fg = strip[ink].mean(axis=0)
    bg = strip[field].mean(axis=0)
    return {"fg_rgb": [round(float(v), 1) for v in fg], "bg_rgb": [round(float(v), 1) for v in bg],
            "fg_hex": hex_of(fg), "bg_hex": hex_of(bg),
            "ratio": round(contrast_ratio(fg, bg), 2),
            "fg_px": int(ink.sum()), "bg_px": int(field.sum()),
            "fg_luma": round(float(luminance(fg[None, None, :])[0, 0]), 1),
            "bg_luma": round(float(luminance(bg[None, None, :])[0, 0]), 1)}


def section_contrast(kit, studio, slot: Path, out: dict) -> dict:
    store = slot / "store"
    spec = json.loads((store / "studio.json").read_text(encoding="utf-8-sig"))
    journey = json.loads((store / "journey.json").read_text(encoding="utf-8-sig"))
    items = compose_items(spec, journey)
    prototype = kit.load_raw_capture(store / "captures" / "raw", 1)
    tokens = {"PAPER": studio.PAPER, "AMBER": studio.AMBER, "AMBER_PALE": studio.AMBER_PALE,
              "MUTED": studio.MUTED, "CYAN": studio.CYAN}

    rows = []
    for index in range(1, 7):
        path = store / "assets" / "en-US" / f"phone-{index:02d}.png"
        canvas = load_rgb(path).astype(np.float32)
        height, width = canvas.shape[:2]
        geometry = predict_geometry(studio, items[(index - 1) % len(items)], (width, height), prototype)
        bands = [band_stats(canvas, tuple(g), tokens)
                 for g in text_groups(canvas, geometry["device_rect"][1])]
        bands = [b for b in bands if b.get("ink_px")]
        classified = classify_bands(bands)
        row = {"index": index, "sections": {}}
        for name, band in classified.items():
            if band is None:
                row[name] = None
                continue
            measurement = band_contrast(canvas, tuple(band["ink_rows"]))
            if measurement:
                measurement["token"] = band["nearest_studio_token"]["token"]
                measurement["declared"] = items[(index - 1) % len(items)].get(
                    "kicker" if name == "kicker" else name, "")
                measurement["threshold_normal_text"] = 4.5
                measurement["threshold_large_text"] = 3.0
                measurement["passes_normal_text"] = measurement["ratio"] >= 4.5
                measurement["passes_large_text"] = measurement["ratio"] >= 3.0
            row[name] = measurement
            if measurement:
                row["sections"][name] = measurement
        rows.append(row)

    # Icon mark against the field it is drawn on. The parts are the palette roles the slot's *own*
    # declared mark draws with, read from the mark registry through `icon_qa.role_masks` - not the
    # row bands and colour predicates of the first slot's artwork (`caliper_*` belongs to
    # `pipe-sizing`). Those constants selected arbitrary pixels in every other slot's icon and the
    # report still printed a confident PASS about them, which is the defect this replaces.
    mark_name = spec.get("mark") or design.DEFAULT_MARK
    icon_palette = studio.resolve_palette(spec.get("palette"))
    with Image.open(store / "assets" / "icon-512.png") as image:
        icon_rgba = np.asarray(image.convert("RGBA"), dtype=np.uint8)
    icon = icon_rgba[..., :3].astype(np.float64)
    icon_alpha = icon_rgba[..., 3]
    icon_size = icon.shape[0]
    mark = icon_qa.mark_mask(icon_rgba, None, icon_palette, mark_name)
    contrasts = []

    def part_contrast(part: str, sel: np.ndarray, source: str) -> None:
        """One region's mean ink against the field in the same rows, as the report always did."""
        if not sel.any():
            return
        fg = icon[sel].mean(axis=0)
        rows_with_sel = np.flatnonzero(sel.any(axis=1))
        row_band = np.zeros((icon_size, 1), dtype=bool)
        row_band[rows_with_sel, 0] = True
        field = np.broadcast_to(row_band, mark.shape) & ~mark
        if not field.any():
            return
        bg = icon[field].mean(axis=0)
        contrasts.append({"part": part, "source": source,
                          "fg_rgb": [round(float(v), 1) for v in fg],
                          "bg_rgb": [round(float(v), 1) for v in bg],
                          "fg_hex": hex_of(fg), "bg_hex": hex_of(bg),
                          "contrast": round(contrast_ratio(fg, bg), 2),
                          "passes_3_to_1_nontext": contrast_ratio(fg, bg) >= 3.0,
                          "passes_4_5_to_1": contrast_ratio(fg, bg) >= 4.5,
                          "fg_pixels": int(sel.sum()), "field_pixels": int(field.sum())})

    # The overall reading is generic and stays: the whole mark's ink against the whole field.
    if mark.any() and (~mark).any():
        part_contrast("mark_vs_field", mark, f"declared mark {mark_name!r}")
    # The per-part reading is the mark's own declared roles, so it can only describe this slot.
    for role, sel in icon_qa.role_masks(icon_rgba, icon_palette, mark_name, mark).items():
        part_contrast(f"role:{role}", sel, f"declared mark {mark_name!r}")
    headline_rows = [r for r in rows if r.get("headline")]
    body_rows = [r for r in rows if r.get("body")]
    out["contrast"] = {
        "phone_rows": rows,
        "headline_min_ratio": min(r["headline"]["ratio"] for r in headline_rows) if headline_rows else None,
        "headline_max_ratio": max(r["headline"]["ratio"] for r in headline_rows) if headline_rows else None,
        "body_min_ratio": min(r["body"]["ratio"] for r in body_rows) if body_rows else None,
        "body_max_ratio": max(r["body"]["ratio"] for r in body_rows) if body_rows else None,
        "wcag_normal_text_threshold": 4.5,
        "wcag_large_text_threshold": 3.0,
        "icon_alpha_on_mark": {"min": int(icon_alpha[mark].min()), "max": int(icon_alpha[mark].max()),
                               "mark_px": int(mark.sum()),
                               "frac_of_icon": round(float(mark.mean()), 4)} if mark.any() else None,
        "icon_mark_contrasts": contrasts,
        "icon_mark_contrasts_source": (
            f"regions derived from this slot's declared mark {mark_name!r} through the palette roles "
            "its primitives draw with, never from another slot's pixel constants"),
    }
    return out["contrast"]


# ----------------------------------------------------------------------------- palette

# `val NAME = Color(0xAARRGGBB)`, with any visibility and any indentation: the first slot declared
# these at file scope as `internal val`, and a slot that groups them in an `internal object Chalk`
# with plain `val` (the shape `.cursor/rules/visual-identity.mdc` asks for - one named palette
# object) matched nothing, which emptied the table and crashed the report. Kotlin's own style also
# allows `private val` inside a composable, so the visibility prefix is optional rather than fixed.
_KOTLIN_COLOR_PATTERN = re.compile(r"\bval (\w+)\s*=\s*Color\(0x([0-9A-Fa-f]{8})\)")
_KOTLIN_COLOR_DECLARATION = re.compile(
    r"(?m)^\s*(?:(?:public|private|internal|protected)\s+)?(?:val|var)\s+\w+\s*=\s*Color\([^\n]*"
)


def strip_kotlin_non_code(source: str) -> str:
    """Replace Kotlin comments and literals with spaces while preserving lines and offsets."""
    chars = list(source)
    i = 0
    state = "code"
    block_depth = 0
    while i < len(source):
        if state == "code":
            if source.startswith("//", i):
                chars[i] = chars[i + 1] = " "
                i += 2
                state = "line_comment"
            elif source.startswith("/*", i):
                chars[i] = chars[i + 1] = " "
                i += 2
                block_depth = 1
                state = "block_comment"
            elif source.startswith('\"\"\"', i):
                chars[i:i + 3] = "   "
                i += 3
                state = "raw_string"
            elif source[i] == '"':
                chars[i] = " "
                i += 1
                state = "string"
            elif source[i] == "'":
                chars[i] = " "
                i += 1
                state = "char"
            else:
                i += 1
        elif state == "line_comment":
            if source[i] == "\n":
                state = "code"
            else:
                chars[i] = " "
            i += 1
        elif state == "block_comment":
            if source.startswith("/*", i):
                chars[i] = chars[i + 1] = " "
                block_depth += 1
                i += 2
            elif source.startswith("*/", i):
                chars[i] = chars[i + 1] = " "
                block_depth -= 1
                i += 2
                if block_depth == 0:
                    state = "code"
            else:
                if source[i] != "\n":
                    chars[i] = " "
                i += 1
        elif state == "raw_string":
            if source.startswith('\"\"\"', i):
                chars[i:i + 3] = "   "
                i += 3
                state = "code"
            else:
                if source[i] != "\n":
                    chars[i] = " "
                i += 1
        else:
            if source[i] == "\\":
                chars[i] = " "
                if i + 1 < len(source):
                    if source[i + 1] != "\n":
                        chars[i + 1] = " "
                    i += 2
                else:
                    i += 1
            elif (state == "string" and source[i] == '"') or (state == "char" and source[i] == "'"):
                chars[i] = " "
                i += 1
                state = "code"
            else:
                if source[i] != "\n":
                    chars[i] = " "
                i += 1
    return "".join(chars)


class _KotlinColorMatcher:
    def finditer(self, source: str):
        return _KOTLIN_COLOR_PATTERN.finditer(strip_kotlin_non_code(source))


KOTLIN_COLOR = _KotlinColorMatcher()


def unparsed_kotlin_color_declarations(source: str) -> list[dict]:
    clean_source = strip_kotlin_non_code(source)
    parsed_starts = {match.start() for match in KOTLIN_COLOR.finditer(source)}
    unreadable = []
    for declaration in _KOTLIN_COLOR_DECLARATION.finditer(clean_source):
        if not any(declaration.start() <= start < declaration.end() for start in parsed_starts):
            unreadable.append({
                "line": clean_source.count("\n", 0, declaration.start()) + 1,
                "declaration": declaration.group(0).strip(),
            })
    return unreadable


def section_palette(kit, studio, slot: Path, out: dict) -> dict:
    color_kt = theme_dir(slot) / "Color.kt"
    theme_kt = color_kt.with_name("Theme.kt")
    source = color_kt.read_text(encoding="utf-8")
    matches = list(KOTLIN_COLOR.finditer(source))
    unreadable = unparsed_kotlin_color_declarations(source)
    kotlin = {}
    for match in matches:
        value = int(match.group(2), 16)
        kotlin[match.group(1)] = {"hex": "#%02X%02X%02X" % ((value >> 16) & 255, (value >> 8) & 255, value & 255),
                                  "rgb": [(value >> 16) & 255, (value >> 8) & 255, value & 255]}
    theme_text = theme_kt.read_text(encoding="utf-8")
    if not kotlin:
        raise SystemExit(
            "BLOCKED: no 'val NAME = Color(0xAARRGGBB)' declaration was found in "
            + (theme_dir(slot) / "Color.kt").relative_to(slot).as_posix()
            + " - this section compares the shipped pixels against the app's own Kotlin palette, so "
            "an unreadable palette is a failure, not an empty comparison.")
    scheme = {}
    for match in re.finditer(r"^\s*(\w+) = (\w+),", theme_text, flags=re.MULTILINE):
        scheme.setdefault(match.group(1), []).append(match.group(2))

    listing = json.loads((slot / "store" / "listing.json").read_text(encoding="utf-8-sig"))
    graphics = listing.get("graphicsSource", {})
    studio_tokens = {"INK": studio.INK, "INK_SOFT": studio.INK_SOFT, "INK_LIFT": studio.INK_LIFT,
                     "AMBER": studio.AMBER, "AMBER_DEEP": studio.AMBER_DEEP,
                     "AMBER_PALE": studio.AMBER_PALE, "CYAN": studio.CYAN, "CYAN_DEEP": studio.CYAN_DEEP,
                     "PAPER": studio.PAPER, "MUTED": studio.MUTED, "BEZEL": studio.BEZEL}

    def nearest(rgb, table):
        best = None
        for key, value in table.items():
            delta = [int(a) - int(b) for a, b in zip(rgb, value["rgb"])]
            distance = max(abs(d) for d in delta)
            if best is None or distance < best["max_channel_delta"]:
                best = {"name": key, "hex": value["hex"], "rgb": value["rgb"], "delta_rgb": delta,
                        "max_channel_delta": distance}
        return best

    declared = {}
    for key in ("brandColor", "accentColor"):
        value = graphics.get(key, "")
        try:
            rgb = list(parse_hex(value))
        except ValueError:
            continue
        declared[key] = {"hex": value, "rgb": rgb, "nearest_kotlin": nearest(rgb, kotlin),
                         "exact_kotlin_match": any(v["rgb"] == rgb for v in kotlin.values())}
    studio_vs_kotlin = {name: {"studio_hex": hex_of(token), "rgb": list(token),
                               "nearest_kotlin": nearest(token, kotlin)}
                        for name, token in studio_tokens.items()}

    measured = {}
    for name in ("icon-512.png", "feature-graphic-1024x500.png", "en-US/phone-01.png",
                 "en-US/tablet-10-01.png"):
        path = slot / "store" / "assets" / name
        array = load_rgb(path)
        exact = {k: int(np.all(array == v["rgb"], axis=-1).sum()) for k, v in kotlin.items()}
        studio_exact = {k: int(np.all(array == np.array(v, dtype=np.uint8), axis=-1).sum())
                        for k, v in studio_tokens.items()}
        declared_exact = {}
        for key, value in declared.items():
            rgb = np.array(value["rgb"], dtype=np.uint8)
            declared_exact[key] = int(np.all(array == rgb, axis=-1).sum())
        quant = (array // 16) * 16
        values, counts = np.unique(quant.reshape(-1, 3), axis=0, return_counts=True)
        order = np.argsort(-counts)[:6]
        measured[name] = {
            "exact_kotlin_matches": {k: v for k, v in exact.items() if v},
            "exact_studio_token_matches": {k: v for k, v in studio_exact.items() if v},
            "exact_declared_graphics_source_matches": {k: v for k, v in declared_exact.items() if v},
            "top_quant16_colors": [{"hex": hex_of(values[i]), "rgb": [int(v) for v in values[i]],
                                    "share": round(float(counts[i]) / float(array.shape[0] * array.shape[1]), 4)}
                                   for i in order],
        }
    out["palette"] = {
        "unparsed_color_declarations": unreadable,
        "kotlin_color_kt": kotlin,
        "kotlin_theme_mapping": scheme,
        "studio_tokens": {k: {"hex": hex_of(v), "rgb": list(v)} for k, v in studio_tokens.items()},
        "studio_token_nearest_kotlin": studio_vs_kotlin,
        "listing_graphics_source": declared,
        "measured": measured,
    }
    return out["palette"]


# ----------------------------------------------------------------------------- icon

def section_icon(kit, studio, slot: Path, out: dict) -> dict:
    """The listing icon: what the mark declares, what the pixels measure, and the difference.

    The icon is built on the keyline system in `scripts/lib/icon_design.py` and measured by
    `scripts/lib/icon_qa.py`, which is also the gate `store-studio.render_icon` refuses to write
    past. This section publishes that one measurement together with the *declaration* the renderer
    read, rather than deriving a second opinion from the same pixels - two implementations of one
    measurement is how a report begins disagreeing with its gate, and the declaration is the half
    the old section could not state at all: its geometry table was computed from hand-tuned pixel
    constants that no longer exist, and it labelled ink with `luma > 215`, which misses every dark
    role (the book's `ink_lift` spine) and so measured a mark the renderer never drew.

    This stays a report: `visual-qa.py` exits 0 whenever it finishes, and the icon gate is
    `store-studio.render_icon`, with `scripts/test-icon-design.py` as the regression suite.
    """
    studio_config = json.loads((slot / "store" / "studio.json").read_text(encoding="utf-8"))
    mark_name = studio_config.get("mark") or design.DEFAULT_MARK
    palette = studio.resolve_palette(studio_config.get("palette"))
    path = slot / "store" / "assets" / "icon-512.png"
    with Image.open(path) as image:
        icon = image.convert("RGBA")
    array = np.asarray(icon, dtype=np.uint8)
    canvas = int(array.shape[0])
    alpha = array[..., 3]
    result = icon_qa.evaluate_icon(array, palette, mark=mark_name)
    measured = result["measured"]
    mark = icon_qa.mark_mask(array, None, palette, mark_name)
    role_masks = icon_qa.role_masks(array, palette, mark_name, mark)
    amber = role_masks.get("amber", np.zeros_like(mark))
    ys, xs = np.nonzero(mark)
    mark_bbox = ([int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1]
                 if xs.size else None)
    center = (canvas - 1) / 2

    def dp(px: float) -> float:
        return round(px / canvas * design.ADAPTIVE_VIEWPORT_DP, 3)

    # The declaration: the geometry the renderer was handed, before any pixel existed. Everything
    # here comes from `icon_design`, the same module the raster and the vector renderer read, so
    # this cannot drift from what was drawn - it is the thing that was drawn.
    viewport = design.ADAPTIVE_VIEWPORT_DP
    declaration = {
        "mark": mark_name,
        "keyline": design.MARK_REGISTRY[mark_name]["keyline"],
        "note": design.MARK_REGISTRY[mark_name]["note"],
        "canvas_px": canvas,
        "optical_anchor": [round(v, 5) for v in design.optical_anchor(mark_name)],
        "circle_keyline_dp": round(design.KEYLINE_CIRCLE * viewport, 3),
        "safe_radius_dp": design.ADAPTIVE_SAFE_RADIUS_DP,
        "reach_dp": round(design.mark_reach_dp(mark_name), 3),
        "silhouette_extent": round(design.mark_silhouette_extent(mark_name), 4),
        "silhouette_bounds": [round(v, 5) for v in design.silhouette_bounds(mark_name)],
        "stroke_scale_px": {weight: round(scale * canvas, 1)
                            for weight, scale in design.STROKE.items()},
        "min_feature_px": round(design.MIN_FEATURE * canvas, 1),
        "min_feature_at_48dp_px": round(design.MIN_FEATURE * 48, 3),
        "primitives": [
            {"kind": primitive["kind"], "role": primitive["role"],
             "reach_dp": round(design.primitive_reach(
                 primitive, design.optical_anchor(mark_name)) * viewport, 3),
             "extent_dp": [round(v * viewport, 2)
                           for v in design.primitive_extent(primitive)],
             **({"stroke": primitive["weight"],
                 "stroke_px": round(design.stroke_weight(primitive["weight"]) * canvas, 1)}
                if "weight" in primitive else {})}
            for primitive in design.mark_primitives(mark_name)
        ],
    }
    # Raster against declaration: the renderer's fidelity, in the units the gate speaks. A nonzero
    # difference here is the antialiased edge and the half-pixel the mask boundary sits on, not a
    # design error - but if it ever grows past a pixel the pixels have stopped being the mark.
    agreement = {
        "reach_dp_delta": round(measured["mark_reach_dp"] - declaration["reach_dp"], 3),
        "silhouette_extent_delta": round(measured["silhouette_extent"]
                                        - declaration["silhouette_extent"], 4),
        "optical_anchor_offset_px_delta": round(measured["optical_offset_px"], 3),
        "inside_circle_keyline": measured["mark_reach_dp"] <= declaration["circle_keyline_dp"],
    }

    # Mask survival comes from `icon_qa`, which is also what the gate reads: the 72dp launcher
    # circle and squircle, and the 66dp safe zone as a circle and as a square. The gate is the
    # 66dp circle - the zone Android guarantees - and the rest is context for a reviewer.
    masks = {}
    for name, data in result["context"]["mask_survival"].items():
        masks[name] = {
            "mask_extent_dp": round(data["mask_extent_fraction"] * design.ADAPTIVE_VIEWPORT_DP, 2),
            **data,
        }

    # The mark's own pixel geometry, so the raster and the adaptive-icon vector can be compared.
    geometry = {
        "mark": mark_name,
        "mark_px": int(mark.sum()),
        "amber_px": int(amber.sum()),
        "mark_bbox_px": mark_bbox,
        "mark_halfwidth_px": round(max(center - mark_bbox[0], mark_bbox[2] - 1 - center), 1)
        if mark_bbox else None,
        "mark_halfwidth_dp": dp(max(center - mark_bbox[0], mark_bbox[2] - 1 - center))
        if mark_bbox else None,
        "mark_halfheight_dp": dp(max(center - mark_bbox[1], mark_bbox[3] - 1 - center))
        if mark_bbox else None,
        "raster_reach_dp": measured["mark_reach_dp"],
        "declared_reach_dp": declaration["reach_dp"],
    }
    # Per-primitive geometry lives in `declaration["primitives"]`: the declared extents in dp, which
    # is the same table the old hand-written per-mark branches here reported - except that it is
    # read off the primitive list the renderers consume instead of being restated by hand, so a mark
    # whose geometry changes cannot leave this section describing the previous shape.

    # Legibility at the size a launcher actually draws. The mask decides which pixels are the mark
    # (NEAREST, because a resampled boolean edge is neither a mark nor a field), and the numbers
    # are the ones a reviewer needs: does the mark still separate from its field, and is its
    # thinnest stroke still a line rather than a smear.
    small = {}
    for target in (48, 96):
        scaled = np.asarray(icon.convert("RGB").resize((target, target), Image.Resampling.LANCZOS),
                            dtype=np.float64)
        scaled_mask = np.asarray(
            Image.fromarray((mark * 255).astype(np.uint8)).resize(
                (target, target), Image.Resampling.NEAREST)) > 127
        scaled_luma = luminance(scaled)
        inside = scaled_luma[scaled_mask]
        outside = scaled_luma[~scaled_mask]
        rows, cols = np.nonzero(scaled_mask)
        small[target] = {
            "mark_px": int(scaled_mask.sum()),
            "mark_fraction": round(float(scaled_mask.mean()), 4),
            "mark_median_luma": round(float(np.median(inside)), 1) if inside.size else None,
            "field_median_luma": round(float(np.median(outside)), 1) if outside.size else None,
            "mark_extent_px": int(max(np.ptp(rows), np.ptp(cols))) + 1 if rows.size else 0,
            "min_feature_px": round(measured["thinnest_stroke_px"] * target / canvas, 3),
            "contrast_ratio": icon_qa.downscale_contrast(array, mark, target)["ratio"],
        }
    out["icon"] = {
        "path": path.relative_to(slot).as_posix(),
        "design_gate": {
            "verdict": result["verdict"],
            "gated_legs": result["gated_legs"],
            "failed_legs": result["failed_legs"],
            "measured": measured,
            "legs": result["legs"],
            "gates": icon_qa.ICON_GATES,
        },
        "declaration": declaration,
        "raster_vs_declaration": agreement,
        "gradient_probe_rgb": {"top": [int(v) for v in array[2, canvas // 2, :3]],
                               "left_middle": [int(v) for v in array[canvas // 2, 8, :3]],
                               "bottom": [int(v) for v in array[canvas - 3, canvas // 2, :3]]},
        "mark_alpha": {"min": int(alpha[mark].min()), "max": int(alpha[mark].max())}
        if mark.any() else None,
        "geometry_dp": geometry,
        "masks": masks,
        "renders": small,
    }
    return out["icon"]


# ----------------------------------------------------------------------------- main

SECTIONS = ("rules", "attribution", "inventory", "fidelity", "composition", "thumbnails",
            "contrast", "palette", "icon")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--slot", required=True)
    parser.add_argument("--root", default=str(ROOT))
    parser.add_argument("--json", default=None)
    parser.add_argument("--only", default=None, help=f"comma-separated subset of {','.join(SECTIONS)}")
    options = parser.parse_args()

    root = Path(options.root).resolve()
    matches = sorted(p for p in (root / "apps").iterdir()
                     if p.is_dir() and (p.name == options.slot or p.name.endswith(f"-{options.slot}")))
    if len(matches) != 1:
        raise SystemExit(f"BLOCKED: slot '{options.slot}' resolved to {[m.name for m in matches]}")
    slot = matches[0]

    kit = load_module("pk_store_render", root / "scripts" / "store" / "render.py")
    studio = load_module("pk_store_studio", root / "scripts" / "store-studio.py")
    manifest_before = hash_manifest(root, slot)
    result: dict = {"slot": slot.name, "root": str(root),
                    "manifest": {"inputs_hashed": len(manifest_before), "hashes": manifest_before}}
    wanted = set(options.only.split(",")) if options.only else set(SECTIONS)
    print(f"== slot {slot.name} ==")
    print(f"sections: {', '.join(s for s in SECTIONS if s in wanted)}")
    print(f"manifest: {len(manifest_before)} files hashed")

    if "rules" in wanted:
        rules = section_rules(kit, result)
        print("\n-- rules --")
        for kind, data in rules["enforced"].items():
            print(f"{kind:9} play-allowed {data['play_assets_min']}-{data['play_assets_max']} "
                  f"gate-lower-bound={data['verify_slot_gate_lower_bound']} "
                  f"factory target {data['factory_target']}")
        print("render limits:", rules["render_py"]["SCREENSHOT_LIMITS"])
        for line in rules["divergences"]:
            print("divergence:", line)

    if "attribution" in wanted:
        attribution = section_attribution(kit, studio, slot, result)
        print("\n-- attribution --")
        print("studio delegation:", attribution["studio_delegation"])
        for row in attribution["components"]:
            print(f"{row['component']:20} {row['shipped']:40} "
                  f"studio={row.get('studio')} generic={row.get('generic')}")

    if "inventory" in wanted:
        inventory = section_inventory(kit, slot, result)
        print("\n-- inventory --")
        for row in inventory["rows"]:
            print(f"{row.get('verdict', '?'):5} {row['path']:48} {row.get('size')} "
                  f"{row.get('mode'):5} {row.get('bytes')}B alpha_min={row.get('alpha_min')} "
                  f"sha={row.get('sha256_16')} {row.get('failures')}")
        for kind, data in inventory["counts"].items():
            print(f"count {kind:9} present={data['present']} play={data['play_min']}-{data['play_max']} "
                  f"ok={data['within_play_range']} four_to_eight={data['within_4_to_8']} "
                  f"target_ok={data['matches_factory_target']}")
        print("locale copies identical:", inventory["locale_copies_identical"])
        print("extra png:", inventory["extra_png"])

    if "fidelity" in wanted:
        fidelity = section_fidelity(kit, studio, slot, result)
        print("\n-- fidelity --")
        for row in fidelity["rows"]:
            geometry = row["geometry"]
            print(f"{row['path']:32} raw={row['expected_raw']} "
                  f"device={geometry['device_rect']} screen={geometry['screen_rect']} "
                  f"scale=({geometry['screen_scale_x']:.6f},{geometry['screen_scale_y']:.6f}) "
                  f"clamped={geometry['width_clamped']}")
            print(f"    MAE masked={row['mae_masked']} maxdiff={row['max_abs_diff_masked']} "
                  f"differing={row['pixels_differing_masked']}/{row['masked_pixel_count']} "
                  f"all_pixels_MAE={row['mae_all_pixels']} verdict={row['fidelity_verdict']}")
            print(f"    corners={row['corner_confinement']}")
            edges = row["edge_localisation"]
            print(f"    edges: " + " | ".join(
                f"{name} pred={edges[name]['predicted_coordinate']} "
                f"top3={edges[name]['strongest_offsets_px']}@{edges[name]['strongest_magnitudes']} "
                f"at_prediction={edges[name]['magnitude_at_prediction']}"
                for name in ("left", "right", "top", "bottom") if name in edges))
            print(f"    bezel={row['bezel_probe']}")
            print(f"    search={row['local_search']} sweep_min={row['sweep_argmin']}")
            print(f"    controls: bilinear={row['control_bilinear_mae']} "
                  f"cover={row['control_cover_crop_mae']} (degenerate={row['cover_control_is_degenerate']}) "
                  f"partial_crop={row['control_partial_crop_mae']} "
                  f"synthetic={row['control_synthetic_screen_mae']} other_raws={row['control_mae_other_raws']} "
                  f"best_control={row['best_control_mae']} separation={row['separation_ratio_vs_best_control']}")

    if "composition" in wanted:
        composition = section_composition(kit, studio, slot, result)
        print("\n-- composition --")
        for row in composition["rows"]:
            print(f"phone-{row['index']:02d} device={row['device_rect']} screen={row['screen_rect']} "
                  f"std={row['screen_luma_std']} colors={row['screen_unique_quant16_colors']} "
                  f"blank={row['screen_blank']}")
            print(f"    bands={row['band_count']} headline={row['headline_present']} "
                  f"cov={row['headline_ink_coverage_of_band']} text_ink={row['total_text_ink_above_device_px']} "
                  f"iou={row['copy_rerender_iou']} extra={row['copy_extra_ink_px']} "
                  f"missing={row['copy_missing_ink_px']}")
            print(f"    margin_ink={row['ink_inside_outer_5pct_margin_px']} "
                  f"border_ink={row['ink_touching_image_border_px']} "
                  f"gaps={row['device_gaps_to_image_edge_px']} "
                  f"screen_area={row['screen_area_fraction_of_canvas']}")
            for band in row["bands_above_device"]:
                print(f"      band rows={band['rows']} ink_rows={band['ink_rows']} h={band['height_px']} "
                      f"ink={band['ink_px']} cov={band['ink_coverage']} fg={band['foreground_rgb']} "
                      f"token={band['nearest_studio_token']['token']} "
                      f"delta={band['nearest_studio_token']['delta']} stroke={band['median_stroke_run_px']}")
        print("pairwise phone mae min/max:", composition["pairwise_mae_min"],
              composition["pairwise_mae_max"], "corr max:", composition["pairwise_corr_max"])
        print("most similar pairs:", composition["most_similar_pairs"])
        print("least similar pairs:", composition["least_similar_pairs"])

    if "thumbnails" in wanted:
        thumbnails = section_thumbnails(kit, studio, slot, result)
        print("\n-- thumbnails --")
        for row in thumbnails["rows"]:
            if "error" in row:
                print(f"phone-{row['index']:02d} ERROR {row['error']}")
                continue
            print(f"phone-{row['index']:02d} headline={row['declared_headline']!r} "
                  f"rows={row['headline_rows_full_size']} cap_h={row['headline_cap_height_px_full_size']} "
                  f"ink={row['headline_ink_px_full_size']}")
            for width in ("1080", "360", "180", "90"):
                data = row["sizes"][width]
                print(f"        {width:>4}px strip={data['band_px']} cap_h={data['cap_height_px']} "
                      f"cov_fixed={data['ink_coverage_fixed_gt200']:.4f} "
                      f"cov_adaptive={data['ink_coverage_adaptive']:.4f} "
                      f"stroke={data['median_stroke_run_px']} rows={data['stroke_rows_present']}"
                      f"/{data['expected_stroke_rows']} peak-field={data['peak_minus_field']}")
            if "body_at_180px" in row:
                print(f"        body@180 {row['body_at_180px']}")

    if "contrast" in wanted:
        contrast = section_contrast(kit, studio, slot, result)
        print("\n-- contrast --")
        for row in contrast["phone_rows"]:
            print(f"phone-{row['index']:02d}")
            for name in ("kicker", "headline", "body"):
                data = row.get(name)
                if data:
                    print(f"        {name:9} fg={data['fg_hex']} bg={data['bg_hex']} "
                          f"ratio={data['ratio']}:1 normal_ok={data['passes_normal_text']} "
                          f"large_ok={data['passes_large_text']} fg_px={data['fg_px']}")
                else:
                    print(f"        {name:9} NOT MEASURED")
        print("headline ratio range:", contrast["headline_min_ratio"], "..", contrast["headline_max_ratio"])
        print("body ratio range:", contrast["body_min_ratio"], "..", contrast["body_max_ratio"])
        print("icon mark alpha:", contrast["icon_alpha_on_mark"])
        for part in contrast["icon_mark_contrasts"]:
            print("icon", part)

    if "palette" in wanted:
        palette = section_palette(kit, studio, slot, result)
        print("\n-- palette --")
        for item in palette["unparsed_color_declarations"]:
            color_path = (theme_dir(slot) / "Color.kt").relative_to(slot).as_posix()
            print(f"NOTE palette - unparsed Color declaration at {color_path}:{item['line']}: "
                  f"{item['declaration']}")
        for key, data in palette["listing_graphics_source"].items():
            print(f"listing {key} {data['hex']} exact_kotlin={data['exact_kotlin_match']} "
                  f"nearest={data['nearest_kotlin']['name']} {data['nearest_kotlin']['hex']} "
                  f"delta={data['nearest_kotlin']['delta_rgb']}")
        for name, data in palette["studio_token_nearest_kotlin"].items():
            print(f"studio {name:10} {data['studio_hex']} nearest={data['nearest_kotlin']['name']:16} "
                  f"{data['nearest_kotlin']['hex']} delta={data['nearest_kotlin']['delta_rgb']}")
        for name, data in palette["measured"].items():
            print(f"measured {name}:")
            print(f"    exact kotlin={data['exact_kotlin_matches']}")
            print(f"    exact studio={data['exact_studio_token_matches']}")
            print(f"    exact declared graphicsSource={data['exact_declared_graphics_source_matches']}")
            print(f"    top={data['top_quant16_colors'][:4]}")

    if "icon" in wanted:
        icon = section_icon(kit, studio, slot, result)
        print("\n-- icon --")
        gate = icon["design_gate"]
        declaration = icon["declaration"]
        print(f"design gate: verdict {gate['verdict']} gated={gate['gated_legs']} "
              f"failed={gate['failed_legs'] or 'none'}")
        for line in icon_qa.format_legs(gate):
            print(line)
        print(f"declaration: mark={declaration['mark']} keyline={declaration['keyline']} "
              f"reach={declaration['reach_dp']}dp extent={declaration['silhouette_extent']} "
              f"anchor={declaration['optical_anchor']} circle_keyline={declaration['circle_keyline_dp']}dp")
        print(f"declaration strokes {declaration['stroke_scale_px']}px "
              f"min_feature={declaration['min_feature_px']}px "
              f"({declaration['min_feature_at_48dp_px']}px at 48dp)")
        for primitive in declaration["primitives"]:
            print(f"    {primitive['kind']:7} {primitive['role']:9} "
                  f"reach={primitive['reach_dp']}dp extent=[{primitive['extent_dp'][0]}, "
                  f"{primitive['extent_dp'][1]} .. {primitive['extent_dp'][2]}, "
                  f"{primitive['extent_dp'][3]}]dp "
                  f"{primitive.get('stroke', '')}{' ' + str(primitive.get('stroke_px')) + 'px' if primitive.get('stroke_px') else ''}")
        print("raster vs declaration:", json.dumps(icon["raster_vs_declaration"]))
        print("gradient probes:", icon["gradient_probe_rgb"], "mark alpha:", icon["mark_alpha"])
        for name, data in icon["masks"].items():
            print(f"mask {name:24} extent={data['mask_extent_dp']}dp "
                  f"clipped_px={data['mark_px_clipped']} frac={data['mark_fraction_clipped']}")
        for target, data in icon["renders"].items():
            print(f"render {target}px {data}")

    # Did anything the measurements depend on change while we measured?  A PASS on a
    # moving artifact is not a PASS: it is a PASS on an unknown revision.
    manifest_after = hash_manifest(root, slot)
    appeared = sorted(set(manifest_after) - set(manifest_before))
    disappeared = sorted(set(manifest_before) - set(manifest_after))
    changed = {rel: {"before": manifest_before[rel], "after": manifest_after[rel]}
               for rel in sorted(set(manifest_before) & set(manifest_after))
               if manifest_before[rel] != manifest_after[rel]}
    result["stability"] = {
        "inputs_hashed": len(manifest_before),
        "changed_during_run": changed,
        "changed_count": len(changed),
        "appeared_during_run": appeared,
        "disappeared_during_run": disappeared,
        "changed_inputs": [rel for rel in changed if "/assets/" not in rel],
        "changed_assets": [rel for rel in changed if "/assets/" in rel],
        "verdict": "PINNED" if not (changed or appeared or disappeared) else "ARTIFACTS_MOVED_DURING_MEASUREMENT",
    }
    print("\n-- stability --")
    print(f"inputs hashed: {len(manifest_before)}; changed during run: {len(changed)}")
    for rel, digests in changed.items():
        print(f"    CHANGED {rel}: {digests['before'][:16]} -> {digests['after'][:16]}")
    for rel in appeared:
        print(f"    APPEARED {rel}")
    for rel in disappeared:
        print(f"    DISAPPEARED {rel}")
    print("stability verdict:", result["stability"]["verdict"])
    if changed or appeared or disappeared:
        print("WARNING: measurements above may span more than one revision of the artifacts; "
              "every row in the JSON is keyed by the sha256 recorded in inventory[].sha256, "
              "which was taken at the time that row was measured.")

    if options.json:
        target = Path(options.json)
        if not target.is_absolute():
            target = root / target
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(result, indent=2, sort_keys=True), encoding="utf-8")
        print(f"\nwrote {target}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
