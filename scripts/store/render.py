"""Write a slot's Play store pack: the listing documents and the icon, feature and screenshot assets.

This is the **write** path of the factory's store work, split out of the 5,617-line
``scripts/store-kit.py`` (see ``openspec/changes/reduce-factory-to-core/design.md`` section D5), and it is
deliberately not a program that grades one:

* **It never validates its own output.** The pack's verdict is ``scripts/store/validate.py``, which reads
  the committed bytes and the provenance record and writes nothing, and the per-run gate is the Node core
  (``node scripts/verify.mjs``, checks in ``scripts/core/``). This module does not import
  ``scripts/store/validate.py``. A program that writes a pack and grades it is grading itself.
* **Every limit comes from the model**, through ``render_limits`` -- never restated here.
* **Nothing is rendered before the owner approves.** ``scripts/lib/release-approval.mjs`` owns that
  decision and ``scripts/lib/pack-approval.mjs`` owns the authored-pack half of it; this module shells out
  to ``scripts/approve-release.mjs --check`` instead of re-deriving either. ``--init`` and a read-only run
  stay open, because they are how an unapproved slot is developed.

``--skip-provenance`` exists for ``scripts/media-capture.mjs``: the record on disk during its render
describes the run being replaced by definition, so the flag reports **``UNVERIFIED`` and exits 2** rather
than letting a deferred state read as a pass (``requirements-preservation.md`` section 13.4). ``0`` or
``2`` are the only statuses that caller accepts, and every other one blocks.

The work itself lives in five sibling modules -- ``render_limits``, ``render_support``,
``render_listing``, ``render_documents`` and ``render_assets`` -- and this module is their entry point:
the approval gate, the CLI, and the orchestration that writes a whole pack. Their public names are
re-exported below, because ``scripts/visual-qa.py``, ``scripts/store-studio.py`` and
``scripts/test-checklist-products.py`` load *this file by path* and read those attributes off it.
"""

from __future__ import annotations

import argparse
import contextlib
import importlib.util
import shutil
import subprocess
import sys
from pathlib import Path
from typing import Any, Callable

# The sibling modules are files beside this one, not an installed package: the store directory goes
# on `sys.path` before they are imported, the line `scripts/store/validate.py` also uses.
sys.path.insert(0, str(Path(__file__).parent))

from render_limits import (
    ROOT,
    SCREEN_TARGETS,
    SUPPORTED_LOCALES,
    pack_model,
)
from render_support import RenderError, write_json
from render_listing import (
    apply_capability_compliance,
    default_listing,
    listing_preflight_problems,
    load_listing,
    merge_missing_locales,
    refresh_idea_from_record,
    slot_metadata,
)
from render_documents import capability_compliance, write_documents
from render_assets import render_feature, render_icon, render_screenshots
# Loaded by path by `visual-qa.py`, `store-studio.py` and `test-checklist-products.py`, which read
# these names off *this* module, so every public sibling name is re-exported here.
from render_limits import (
    APPS,
    DEFAULT_CONTACT_EMAIL,
    FEATURE_GRAPHIC_CANVAS,
    ICON_CANVAS,
    MAX_FEATURE_BYTES,
    MAX_ICON_BYTES,
    MAX_PNG_BYTES,
    MAX_RELEASE_NOTE_CHARS,
    PLACEHOLDER_HOSTS,
    ROOT,
    SCREENSHOT_ASPECT_RATIOS,
    SCREENSHOT_LIMITS,
    SCREEN_TARGETS,
    SUPPORTED_LOCALES,
    aspect_ratio_failures,
    pack_model,
    screenshot_rule_failures,
)
from render_documents import (
    CREDENTIALS_RULES,
    DATA_SAFETY_MARKER,
    PRIVACY_MARKER,
    PRODUCT_ID_DECLARATION,
    RELEASE_NAME_LINE,
    RELEASE_NOTES_MARKER,
    RELEASE_NOTE_PROMPTS,
    RELEASE_NOTE_UNFINISHED,
    REVIEW_ACCESS_PARAGRAPHS,
    REVIEW_NOTES_MARKER,
    SOURCE_SCAN_RELATIVE,
    SOURCE_SCAN_SKIP_DIRS,
    billing_checklist_lines,
    capability_compliance,
    credentials_rule,
    declared_product_ids,
    markdown_escape,
    parse_release_notes,
    read_release_manifest,
    review_access_paragraph,
    slot_kotlin_sources,
    validate_release_notes,
    write_documents,
    write_preserving,
)
from render_assets import (
    FONT_CANDIDATES,
    cover,
    draw_abstract_mark,
    font,
    frame_screen,
    gradient,
    load_raw_capture,
    mix,
    render_feature,
    render_icon,
    render_screenshots,
    rgb,
    synthetic_screen,
    wrap_pixels,
)
from render_listing import (
    IDEA_DERIVED_FIELDS,
    apply_capability_compliance,
    default_listing,
    english_copy,
    generator_localized_copy,
    is_idea_derived_output,
    listing_preflight_problems,
    load_listing,
    merge_missing_locales,
    normalize_idea,
    recorded_idea,
    refresh_idea_from_record,
    russian_copy,
    slot_metadata,
)
from render_support import (
    RenderError,
    StoreKitError,
    heading,
    is_placeholder_email,
    is_placeholder_url,
    match_gradle,
    prose_lines,
    read_text,
    truncate_chars,
    write_json,
)




# --------------------------------------------------------------------------------------------
# The approval boundary
# --------------------------------------------------------------------------------------------


def release_approval_block(slot: Path) -> str | None:
    """The reason asset rendering must wait, or ``None`` when the owner has approved.

    Rendering the icon, the feature graphic and the screenshot sets is release work: the assets are
    the app's public claim about itself, and the owner approves the app before they are produced
    rather than after.  The decision itself lives in ``scripts/lib/release-approval.mjs`` -- the
    single source of truth every release-side script imports -- so this shells out to its ``--check``
    mode instead of re-deriving the rule in Python.

    That check covers **both** halves of an approval and the reason it returns names the one that
    moved: the app (the build inputs ``release-approval.mjs`` hashes) and the authored store pack
    (``store/listing.json``, ``store/journey.json``, ``store/studio.json`` and
    ``store/review-notes.md``, hashed by ``pack-approval.mjs``).  Rendering is exactly where the
    second half matters: the assets are composed from ``studio.json``'s mark, palette and headlines
    and from the listing's copy.
    """
    script = ROOT / "scripts" / "approve-release.mjs"
    if not script.is_file():
        return (
            f"release approval cannot be checked because {script.relative_to(ROOT)} is missing; "
            "restore the factory harness before rendering store assets"
        )
    node = shutil.which("node")
    if not node:
        return (
            "release approval cannot be checked because node was not found on PATH; the "
            "approval record is validated by scripts/approve-release.mjs"
        )
    result = subprocess.run(
        [node, str(script), "--check", "--slot-dir", str(slot)],
        cwd=ROOT,
        capture_output=True,
        text=True,
    )
    if result.returncode == 0:
        return None
    output = f"{result.stdout or ''}{result.stderr or ''}"
    for row in output.splitlines():
        stripped = row.strip()
        if stripped.startswith("BLOCKED"):
            reason = stripped[len("BLOCKED"):].strip()
            if reason:
                return reason
    return output.strip() or f"approve-release.mjs --check exited {result.returncode}"




# --------------------------------------------------------------------------------------------
# Slot resolution and metadata
# --------------------------------------------------------------------------------------------


def resolve_slot(args: argparse.Namespace) -> Path:
    if args.slot_dir:
        slot = args.slot_dir.expanduser()
        slot = (Path.cwd() / slot).resolve() if not slot.is_absolute() else slot.resolve()
        if not slot.is_dir():
            raise RenderError(f"slot directory does not exist: {slot}")
        return slot
    assert args.slot
    return pack_model.find_slot(args.slot).resolve()




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
        raise RenderError(
            f"unsupported locale(s): {', '.join(bad)}; supported: {', '.join(SUPPORTED_LOCALES)}"
        )
    if not locales:
        raise RenderError("at least one locale is required")
    return locales




# --------------------------------------------------------------------------------------------
# Rendering
# --------------------------------------------------------------------------------------------


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
            {
                "name": f"shot-{index:02d}",
                "title": caption,
                "body": f"Journey step {index}",
                "steps": actions[index - 1],
            }
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




def load_studio_renderer(store: Path) -> Callable[[Path], None] | None:
    """Return the studio's slot renderer when this slot opted in, else ``None``.

    The studio lives in ``scripts/store-studio.py``, whose hyphen makes it un-importable by name, so
    it is loaded from its path.  A failure here is raised rather than reported: a slot that declares
    ``studio.json`` and silently got the generic renderer back would get generic assets while its
    ``graphics.md`` describes composed ones, which is a claim rendered untrue.  Failing closed leaves
    the previous assets in place instead.
    """
    if not (store / "studio.json").is_file():
        return None
    studio_path = Path(__file__).resolve().parent.parent / "store-studio.py"
    if not studio_path.is_file():
        raise RenderError(f"{store / 'studio.json'} is declared but {studio_path} does not exist")
    spec = importlib.util.spec_from_file_location("store_studio", studio_path)
    if spec is None or spec.loader is None:
        raise RenderError(f"cannot load {studio_path}")
    module = importlib.util.module_from_spec(spec)
    # Registered before execution for the same reason as `_load_pack_model` in store-studio.py: the
    # module declares dataclasses, and Python 3.13 cannot create one whose module is not in
    # `sys.modules` (`dataclasses._is_type` does `sys.modules.get(cls.__module__).__dict__`).
    sys.modules.setdefault("store_studio", module)
    try:
        spec.loader.exec_module(module)
    except Exception as error:  # noqa: BLE001 - any load failure must stop the render
        sys.modules.pop("store_studio", None)
        raise RenderError(f"{studio_path} failed to load: {error!r}") from error

    def render(slot_dir: Path) -> None:
        module.render_slot(slot_dir)

    return render




def render_all(
    store: Path, listing: dict[str, Any], locales: list[str], meta: dict[str, str]
) -> None:
    """Write every composed asset and every document for ``store``.

    The approval gate is re-checked here rather than only in ``main``, so a caller that reaches this
    function directly cannot render an unapproved revision: the assets are the app's public claim
    about itself.
    """
    problem = release_approval_block(store.parent)
    if problem:
        raise RenderError(problem)
    # A slot can opt into the composed visual identity by adding store/studio.json. The generic
    # renderer below stays the default so the other slots keep working unchanged, and it stays in
    # place as the fallback because media-capture re-renders through this function after every
    # capture run - a fresh capture must still produce fresh assets.
    studio = load_studio_renderer(store)
    if studio is not None:
        # The studio takes the slot directory (it also writes the app's adaptive-icon vectors),
        # while render_all is handed the store directory.
        studio(store.parent)
        write_documents(store, listing, locales, False, meta)
        return
    assets = store / "assets"
    icon = assets / pack_model.shared_asset_name("icon")
    feature = assets / pack_model.shared_asset_name("feature-graphic")
    render_icon(icon, listing)
    render_feature(feature, listing, listing["defaultLocale"])
    for locale in locales:
        locale_dir = assets / locale
        locale_dir.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(icon, locale_dir / icon.name)
        render_feature(locale_dir / feature.name, listing, locale)
    synthetic = render_screenshots(store, listing, locales)
    write_documents(store, listing, locales, synthetic, meta)




PROVENANCE_DEFERRED_NOTE = (
    "UNVERIFIED provenance — this run skipped the provenance check, so the assets are NOT attested "
    "here and this is not a passing result. scripts/media-capture.mjs passes --skip-provenance because "
    "the record on disk describes the run it is about to replace, and it follows with a real "
    "validate.py pass. Invoking the flag directly leaves the pack unverified."
)




# --------------------------------------------------------------------------------------------
# CLI
# --------------------------------------------------------------------------------------------


def parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        description="Render a Play store pack. Writes documents, the icon and the screenshot sets."
    )
    target = p.add_mutually_exclusive_group(required=True)
    target.add_argument("--slot", help="Slot slug resolved beneath apps/.")
    target.add_argument(
        "--slot-dir", type=Path, help="Explicit slot directory (absolute or relative to cwd)."
    )
    p.add_argument("--init", action="store_true", help="Write draft listing.json and stop.")
    p.add_argument(
        "--generate",
        action="store_true",
        help="Render the pack. This is the default action when no other mode is named.",
    )
    p.add_argument(
        "--render-only",
        action="store_true",
        help="Retired alias of --generate; kept so an existing pipeline invocation still renders.",
    )
    p.add_argument(
        "--locales", default=None, help=f"Comma-separated locales (supported: {','.join(SUPPORTED_LOCALES)})."
    )
    p.add_argument(
        "--capture",
        action="store_true",
        help="Retired; refuses. Use 'node scripts/media-capture.mjs --slot <slug>' instead.",
    )
    p.add_argument(
        "--skip-provenance",
        action="store_true",
        help=(
            "Report UNVERIFIED and exit 2 after rendering. Only for scripts/media-capture.mjs, which "
            "publishes new captures and only afterwards writes the record that describes them."
        ),
    )
    return p




def harden_streams() -> None:
    """Make a printed message survive the terminal's codepage.

    The console's own labels are Russian, and on Windows the default stream encoding is a legacy
    codepage (cp1251 on the machine that found this), which raised UnicodeEncodeError on a `→`. An
    unencodable character now degrades to a replacement character instead of killing the run.
    """
    for stream in (sys.stdout, sys.stderr):
        with contextlib.suppress(AttributeError, ValueError):
            stream.reconfigure(errors="replace")




def main(argv: list[str] | None = None) -> int:
    harden_streams()
    args = parser().parse_args(argv)
    if args.capture:
        raise RenderError(
            "--capture is retired: it photographed the device without verifying the result. Use "
            "'node scripts/media-capture.mjs --slot <slug>', which asserts each shot's expected "
            "readings before capturing."
        )
    slot = resolve_slot(args)
    if not args.init:
        # Rendering is a release action. Check before anything is written, so a refusal leaves the
        # slot exactly as it was; `render_all` re-checks, so a caller that reaches it directly cannot
        # render an unapproved revision either.
        problem = release_approval_block(slot)
        if problem:
            raise RenderError(problem)
    meta = slot_metadata(slot)
    capability_config = slot / "capabilities.json"
    if not capability_config.is_file():
        write_json(capability_config, {"schemaVersion": 1, "enabled": []})
    compliance = capability_compliance(slot)
    store = slot / "store"
    listing_path = store / "listing.json"
    store.mkdir(parents=True, exist_ok=True)

    existing = load_listing(listing_path) if listing_path.is_file() else None
    locales = parse_locales(args.locales, existing)
    if args.init:
        # `--init` is the unapproved-slot route: it writes a draft copy and no asset, so it needs no
        # approval.
        listing = default_listing(meta, locales, compliance)
        write_json(listing_path, listing)
        review = store / "review-notes.md"
        review.write_text(
            f"# Reviewer notes — {listing['appName']}\n\n"
            "- TODO-HUMAN: Review listing.json claims against the finished app.\n"
            f"- TODO-HUMAN: Run `node scripts/publish-privacy.mjs --slot {listing['slug']}` to publish "
            "`store/privacy.md` and record its public HTTPS URL in listing.json.\n"
            "- TODO-HUMAN: Confirm the support email in listing.json is a monitored address.\n"
            "- TODO-HUMAN: Run the store renderer after review.\n",
            encoding="utf-8",
        )
        print(f"PASS: initialized draft {listing_path}")
        return 0

    if existing is None:
        existing = default_listing(meta, locales, compliance)
        write_json(listing_path, existing)
        print("WARN: listing.json was missing; initialized draft before generation")
    listing = existing
    refresh_idea_from_record(listing, meta, compliance)
    merge_missing_locales(listing, meta, locales, compliance)
    apply_capability_compliance(listing, compliance)
    preflight = listing_preflight_problems(listing, locales)
    if preflight:
        raise RenderError("listing validation failed:\n  - " + "\n  - ".join(preflight))
    write_json(listing_path, listing)

    render_all(store, listing, locales, meta)
    if args.skip_provenance:
        print(PROVENANCE_DEFERRED_NOTE)
        return 2
    print(
        f"RENDER: OK apps/{slot.name}/store "
        f"(locales={','.join(locales)}; {len(SCREEN_TARGETS)} screenshot kind(s); "
        "the pack's verdict is scripts/store/validate.py's, not this program's)"
    )
    return 0




if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RenderError as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        raise SystemExit(1)
