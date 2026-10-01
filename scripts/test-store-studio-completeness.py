"""Failure-injection tests for the studio's all-or-nothing pack rule.

The defect these exist for: `render_slot` used to `continue` past a missing or unreadable raw
capture, leaving the PREVIOUS run's composed asset in place. The resulting pack mixed two capture
runs, yet `store-kit --validate-only` and `verify.mjs` both reported OK because
neither looks at where the pixels came from. A solid red frame at the right dimensions was accepted
as a real screenshot the same way.

Every case runs against a throwaway copy of the slot under a temp directory, so the committed pack
is never touched.
"""
from __future__ import annotations

import atexit
import importlib.util
import json
import shutil
import sys
import tempfile
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from lib.scratch import SCRATCH_DIR

# Every copy `sandbox()` has made this run, removed when the process ends.
#
# This is not tidiness: each copy is the whole slot, measured at 0.41 GB on 2026-10-01, and the suite
# makes several per run. Left behind, repeated runs accumulated 20 copies and 8.2 GB — and until
# 2026-10-01 they were written to the machine's *system* temp directory, which is what the owner
# reported as a filled temp folder and a machine that took 30 seconds to five minutes to become
# usable. That measurement is why `.cursor/rules/workspace-hygiene.mdc` exists, and why the copies now
# live under the repository's own scratch directory.
#
# `atexit` rather than a `finally` around each leg: a leg that raises must still clean up, and the
# sandbox is referenced from several call sites, so the process, not the call site, owns the lifetime.
_SANDBOXES: list[Path] = []


def _remove_sandboxes() -> None:
    for parent in _SANDBOXES:
        shutil.rmtree(parent, ignore_errors=True)


atexit.register(_remove_sandboxes)

ROOT = Path(__file__).resolve().parent.parent
SLOT = ROOT / "apps" / "2026-09-25-pipe-sizing"

CHECKS = 0
FAILED = 0


def check(name: str, ok: bool, detail: str = "") -> None:
    global CHECKS, FAILED
    CHECKS += 1
    if ok:
        print(f"PASS {name}")
        return
    FAILED += 1
    print(f"FAIL {name}{(' - ' + detail) if detail else ''}")


def load_studio():
    spec = importlib.util.spec_from_file_location("store_studio", ROOT / "scripts" / "store-studio.py")
    assert spec and spec.loader
    module = importlib.util.module_from_spec(spec)
    # Registered before execution: store-studio.py declares dataclasses, and Python 3.13 cannot
    # create one whose module is not in `sys.modules` (see `scripts/store-studio.py`'s loader).
    sys.modules.setdefault("store_studio", module)
    spec.loader.exec_module(module)
    return module


def sandbox() -> Path:
    """A copy of the real slot, so a mutation here cannot reach the committed pack.

    The copy is validated because this suite may run while another writer is editing the slot's
    manifests. A torn read would otherwise surface as a render failure and read as a defect in the
    studio rather than in the harness, so retry until the copied manifests parse.
    """
    for _ in range(5):
        target = Path(tempfile.mkdtemp(prefix="studio-sandbox-", dir=SCRATCH_DIR)) / SLOT.name
        _SANDBOXES.append(target.parent)
        shutil.copytree(SLOT, target)
        try:
            for name in ("journey.json", "studio.json", "listing.json"):
                json.loads((target / "store" / name).read_text(encoding="utf-8"))
        except (json.JSONDecodeError, OSError):
            shutil.rmtree(target.parent, ignore_errors=True)
            continue
        return target
    raise SystemExit(
        "BLOCKED: the slot's manifests did not parse on five consecutive copies; another process "
        "is editing the slot. Re-run once it has stopped."
    )


def run(studio, slot: Path):
    """Run render_slot, returning (ok, message)."""
    try:
        studio.render_slot(slot)
        return True, ""
    except SystemExit as error:  # the studio fails closed with SystemExit
        return False, str(error)
    except Exception as error:  # noqa: BLE001 - any escape is a failed check below
        return False, f"{type(error).__name__}: {error}"


def hashes(store: Path) -> dict[str, str]:
    import hashlib

    assets = store / "assets"
    return {
        str(path.relative_to(assets)): hashlib.sha256(path.read_bytes()).hexdigest()
        for path in sorted(assets.rglob("*.png"))
    }


def main() -> int:
    studio = load_studio()

    # 1. The happy path still works: a complete capture set renders the full pack.
    slot = sandbox()
    before = hashes(slot / "store")
    ok, message = run(studio, slot)
    check("a complete capture set renders", ok, message)
    after = hashes(slot / "store")
    check("the render is deterministic (re-render reproduces every asset)", before == after,
          f"{len(before)} before, {len(after)} after")
    check("the pack contains every composed asset",
          len(after) == studio.EXPECTED_COMPOSED_ASSETS,
          f"{len(after)} files, expected {studio.EXPECTED_COMPOSED_ASSETS}")

    # 2. A deleted capture must stop the render, not skip a target.
    slot = sandbox()
    before = hashes(slot / "store")
    (slot / "store" / "captures" / "raw" / "03.png").unlink()
    ok, message = run(studio, slot)
    check("a missing raw capture blocks the render", not ok, message)
    check("the missing capture is named", "03.png" in message, message)
    check("nothing was written when the render was blocked", hashes(slot / "store") == before)

    # 3. A TRUNCATED capture (the realistic interruption) must block, not be treated as absent.
    slot = sandbox()
    before = hashes(slot / "store")
    truncated = slot / "store" / "captures" / "raw" / "04.png"
    truncated.write_bytes(truncated.read_bytes()[:200])
    ok, message = run(studio, slot)
    check("a truncated raw capture blocks the render", not ok, message)
    check("the truncated file is named", "04.png" in message, message)
    check("nothing was written when the render was blocked", hashes(slot / "store") == before)

    # 4. Deleting studio.json must not silently downgrade every asset to the generic fallback.
    slot = sandbox()
    before = hashes(slot / "store")
    (slot / "store" / "studio.json").unlink()
    ok, message = run(studio, slot)
    check("a missing studio.json blocks the render", not ok, message)
    check("nothing was written when the render was blocked", hashes(slot / "store") == before)

    # 5. The count assertion is wired to the real inventory, so drift cannot pass silently.
    slot = sandbox()
    ok, _ = run(studio, slot)
    check("the rendered count matches the declared inventory", ok)
    check("the declared inventory is 4 shared assets + every screenshot target",
          studio.EXPECTED_COMPOSED_ASSETS == 4 + sum(
              count for count, _ in studio.SCREEN_TARGETS.values()),
          str(studio.EXPECTED_COMPOSED_ASSETS))

    # 6. The exact attack from the review: a solid colour rectangle of the right dimensions, which
    #    both store-kit --validate-only and verify.mjs accepted as a real shot.
    from PIL import Image as PILImage

    slot = sandbox()
    before = hashes(slot / "store")
    source = PILImage.open(SLOT / "store" / "captures" / "raw" / "01.png")
    PILImage.new("RGB", source.size, (255, 0, 0)).save(
        slot / "store" / "captures" / "raw" / "01.png", format="PNG")
    ok, message = run(studio, slot)
    check("a solid red frame is rejected as a capture", not ok, message)
    check("the solid frame is named", "01.png" in message, message)
    check("nothing was written when the render was blocked", hashes(slot / "store") == before)

    # 7. A capture set that disagrees on size is a mixed run and must not compose.
    slot = sandbox()
    before = hashes(slot / "store")
    small = PILImage.open(SLOT / "store" / "captures" / "raw" / "05.png")
    small.resize((small.width // 2, small.height // 2)).save(
        slot / "store" / "captures" / "raw" / "05.png", format="PNG")
    ok, message = run(studio, slot)
    check("a capture set that disagrees on size is rejected", not ok, message)
    check("nothing was written when the render was blocked", hashes(slot / "store") == before)

    # 8. The plausibility guard has a wide margin against the real captures.
    for index in range(1, studio.REQUIRED_RAW_CAPTURES + 1):
        image = PILImage.open(SLOT / "store" / "captures" / "raw" / f"{index:02d}.png").convert("RGB")
        colours = image.getcolors(maxcolors=studio.MIN_CAPTURE_COLOURS + 1)
        distinct = len(colours) if colours is not None else studio.MIN_CAPTURE_COLOURS + 1
        low, high = image.convert("L").getextrema()
        check(f"real capture {index:02d} clears the plausibility guard with margin",
              distinct >= studio.MIN_CAPTURE_COLOURS and high - low >= studio.MIN_CAPTURE_LUMA_SPREAD,
              f"{distinct} colours, spread {high - low}")

    # 9. The manifest the studio reads is real JSON, so the failure paths above are not vacuous.
    journey = json.loads((SLOT / "store" / "journey.json").read_text(encoding="utf-8"))
    check("the journey declares the captures the studio requires",
          len(journey.get("shots", [])) >= studio.REQUIRED_RAW_CAPTURES,
          f"{len(journey.get('shots', []))} shots")

    # 10. The feature graphic keeps every important element out of the outer 10% Play crops. An
    #     adversarial review measured the device mock-up 18px from the bottom edge and the text
    #     column 76px from the left - both inside the band the docstring already forbade - so this
    #     is asserted against the rendered pixels, not merely intended by the layout code.
    listing = json.loads((SLOT / "store" / "listing.json").read_text(encoding="utf-8"))
    studio_conf = json.loads((SLOT / "store" / "studio.json").read_text(encoding="utf-8"))
    capture = PILImage.open(SLOT / "store" / "captures" / "raw" / "01.png")
    feature = studio.render_feature(listing, studio_conf, capture)
    width, height = feature.size
    margin_x = round(width * studio.FEATURE_SAFE_FRACTION)
    margin_y = round(height * studio.FEATURE_SAFE_FRACTION)
    ink = feature.convert("L").point(lambda value: 255 if value > 110 else 0)
    bbox = ink.getbbox()
    check("the feature graphic renders", bbox is not None, "no content pixels")
    if bbox:
        x0, y0, x1, y1 = bbox
        inside = (x0 >= margin_x and y0 >= margin_y
                  and x1 <= width - margin_x and y1 <= height - margin_y)
        check(
            "the feature graphic's content stays inside the outer-10% safe box",
            inside,
            f"content x=[{x0}, {x1}] y=[{y0}, {y1}] vs safe x=[{margin_x}, {width - margin_x}] "
            f"y=[{margin_y}, {height - margin_y}]",
        )

    # 11. The safe-area assertion must actually fire, or the check above is decorative.
    try:
        studio.assert_feature_safe_area(
            (width, height),
            [("probe", (0, margin_y + 1, 10, margin_y + 11))],
        )
        fired = False
        detail = "the assertion accepted an element on the left edge"
    except SystemExit:
        fired = True
        detail = ""
    check("the safe-area assertion rejects an element outside the box", fired, detail)

    print(f"\n{CHECKS - FAILED}/{CHECKS} checks passed")
    return 1 if FAILED else 0


if __name__ == "__main__":
    raise SystemExit(main())
