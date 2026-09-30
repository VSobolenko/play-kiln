#!/usr/bin/env python3
"""Generate and validate a complete, deterministic Google Play store pack.

listing.json schemaVersion 1 is the machine-readable source of truth.  The
required top-level fields and their types are documented in the generated
listing.md as well as validated here before assets are rendered.
"""

from __future__ import annotations

import argparse
import contextlib
from datetime import datetime
import hashlib
import html
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import textwrap
import time
from typing import Any, Callable
from urllib.parse import urlparse

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
APPS = ROOT / "apps"
SUPPORTED_LOCALES = ("en-US", "ru-RU")
# The Play Console's own byte caps, per asset kind, measured live in the RU console on 2026-09-29
# (`Значок приложения` <= 1 MB, `Картинка для описания` <= 15 MB, every screenshot field <= 8 MB).
# They are stated per kind because a single blanket cap either rejects a legal feature graphic or
# accepts a screenshot Play will refuse.  `MAX_PNG_BYTES` stays the screenshot default.
MAX_PNG_BYTES = 8 * 1024 * 1024
MAX_ICON_BYTES = 1024 * 1024
MAX_FEATURE_BYTES = 15 * 1024 * 1024

# Play's release screen asks for two things the rest of the pack does not carry: a release *name*,
# which is an internal console label never shown to a user, and *release notes* ("What's new"),
# which ARE shown on the store page and which Play caps at 500 characters per language.
#
# This limit was missing from the pack contract, so the first real upload hit the wall with no
# prepared text and no way to count.  The notes are prose a human pastes, which means the only
# defence against a truncated paste is to measure them here, per locale, the same way the listing's
# own title/short/full description limits are measured.
MAX_RELEASE_NOTE_CHARS = 500

# The developer account's monitored support address, recorded once here because the generated
# `listing.json` default and the generated "human checks" TODO both read it, and a second copy
# drifts.  A slot may still override `listing.json.contactEmail`, and a hand-authored value is
# never rewritten (see `write_preserving`).
#
# It is deliberately NOT a secret and NOT a `.env` value: Play publishes the contact address on the
# store listing, so treating it as key material would hide the one field a reviewer must be able to
# read.  The `.env` rule governs credentials, not a public support address.
DEFAULT_CONTACT_EMAIL = "v.sobolenko.vs@gmail.com"

# Hosts reserved for documentation by RFC 2606 / RFC 6761.  A pack that still points at one of these
# has not been finished, and must say so out loud rather than let it reach a store listing.
PLACEHOLDER_HOSTS = ("example.com", "example.org", "example.net", "example.edu", "localhost", "invalid")


def _is_placeholder_host(host: str) -> bool:
    host = host.lower().strip(".")
    return not host or any(host == entry or host.endswith(f".{entry}") for entry in PLACEHOLDER_HOSTS)


def is_placeholder_email(value: str) -> bool:
    """True when an address still uses a reserved documentation domain."""
    return _is_placeholder_host(value.rpartition("@")[2])


def is_placeholder_url(value: str) -> bool:
    """True when a URL still points at a reserved documentation host, or is not an http(s) URL."""
    parsed = urlparse(value if "//" in value else f"//{value}")
    return parsed.scheme not in ("http", "https") or _is_placeholder_host(parsed.hostname or "")
SCREEN_TARGETS = {
    "phone": (6, (1080, 1920)),
    "tablet-7": (4, (1080, 1920)),
    "tablet-10": (4, (1440, 2560)),
}

# The aspect rule the Play Console states for EVERY screenshot field, in the console's own inline
# words (measured live, RU console, 2026-09-29, on the app's `Стандартная страница приложения`
# page): "Соотношение сторон изображения должно составлять 16:9 или 9:16". It is one constant
# rather than a per-kind key because the sentence is identical in the phone, 7-inch and 10-inch
# fields, and all three targets above are exactly 9:16 (1080x1920, 1080x1920, 1440x2560).
#
# `screenshot_rule_failures` asserts it for every kind, so a `max_ratio` of None means only "no
# extra 2x cap" - it no longer means the ratio went unchecked.
SCREENSHOT_ASPECT_RATIOS: tuple[tuple[int, int], ...] = ((16, 9), (9, 16))

# Screenshot limits, keyed by asset kind.  The rule is per kind because the console states a
# *different* size window for each screenshot field; a single blanket predicate got one of them
# wrong and rejected a legal tablet asset.
#
# Primary source - the console's own inline strings, not the documentation (measured live, RU
# console, 2026-09-29, `Стандартная страница приложения`):
#   `Скриншоты для смартфона`                2..8 files, each side 320..3840, <= 8 MB
#   `Скриншоты для семидюймовых планшетов`   <= 8 files, each side 320..3840, <= 8 MB
#   `Скриншоты для 10-дюймового планшета`    <= 8 files, each side 1080..7680, <= 8 MB
# Each of those fields also carries the same aspect sentence in SCREENSHOT_ASPECT_RATIOS.  The
# 7-inch and 10-inch windows therefore differ, exactly as the console states: 320..3840 against
# 1080..7680.
#
# The same page also carries Play's general Requirements text and its Large screens section
# (https://support.google.com/googleplay/android-developer/answer/9866151?hl=en), and those two
# disagree about the long edge for tablets (3,840 against 7,680).  The factory follows the field
# windows the console itself enforces, because those are the strings that decide whether an upload
# is accepted; a 1080x1920 asset sits inside every one of them.
#
# "max_ratio" is the Requirements "no more than twice as long as the minimum dimension" cap, kept
# as an extra phone-side guard.  It is NOT the aspect rule - the console's 16:9 / 9:16 sentence
# applies to all three kinds and is asserted separately from SCREENSHOT_ASPECT_RATIOS.
SCREENSHOT_LIMITS: dict[str, dict[str, int | None]] = {
    "phone": {"min_short": 320, "max_long": 3840, "max_ratio": 2},
    "tablet-7": {"min_short": 320, "max_long": 3840, "max_ratio": None},
    "tablet-10": {"min_short": 1080, "max_long": 7680, "max_ratio": None},
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
    target.add_argument(
        "--observations",
        nargs="?",
        metavar="PATH",
        const=CONSOLE_OBSERVATIONS_RELATIVE.as_posix(),
        help=(
            "Validate the live-transcription files for this factory, and report the model edits each "
            f"one implies. PATH is a directory (default {CONSOLE_OBSERVATIONS_RELATIVE.as_posix()}) or "
            "a single file. Reads the model; writes nothing without --apply-observations."
        ),
    )
    p.add_argument(
        "--apply-observations",
        action="store_true",
        help=(
            "With --observations: write the foldable edits back into the console form. Only what "
            "`observationFold` declares is written; everything else is printed as work for a reader."
        ),
    )
    p.add_argument("--init", action="store_true", help="Write draft listing.json and stop.")
    p.add_argument(
        "--locales", default=None, help="Comma-separated locales (supported: en-US,ru-RU)."
    )
    p.add_argument(
        "--capture",
        action="store_true",
        help="Retired; refuses. Use 'node scripts/media-capture.mjs --slot <slug>' instead.",
    )
    p.add_argument(
        "--render-only", action="store_true", help="Render from existing captures or synthetic screens."
    )
    p.add_argument(
        "--validate-only", action="store_true", help="Validate existing copy and assets without writing."
    )
    p.add_argument(
        "--skip-provenance",
        action="store_true",
        help=(
            "Skip the provenance.json check. Only for scripts/media-capture.mjs, which publishes new "
            "captures and only afterwards writes the record that describes them, so the record on disk "
            "during its render is the previous run's by definition."
        ),
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


def release_approval_block(slot: Path) -> str | None:
    """The reason asset rendering must wait, or None when the owner has approved.

    Rendering the icon, the feature graphic and the screenshot sets is release work: the
    assets are the app's public claim about itself, and the owner approves the app before
    they are produced rather than after. The decision itself lives in
    `scripts/lib/release-approval.mjs` — the single source of truth every release-side script
    imports — so this shells out to its `--check` mode instead of re-deriving the rule in
    Python. `--init` (draft copy) and `--validate-only` (reading) stay ungated, because they
    are how an unapproved slot is developed.

    That check now covers **both** halves of an approval, and the reason it returns names the
    one that moved: the app (the build inputs `scripts/lib/release-approval.mjs` hashes) and
    the authored store pack (`store/listing.json`, `store/journey.json`, `store/studio.json`
    and `store/review-notes.md`, hashed by `scripts/lib/pack-approval.mjs`). Rendering is
    exactly where the second half matters: the assets are composed from `studio.json`'s mark,
    palette and headlines and from the listing's copy, so a listing edited after approval would
    otherwise be rendered into the pack that gets uploaded. Nothing is re-derived here — this
    function holds no copy of either rule — so the refusal a developer sees cannot describe a
    different contract than the one it enforces.
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


# The Play Console form itself — the ordered declarations, their page paths, their questions, the
# slot-data key that fills each literal, and the credentials-dialog field caps — lives in ONE file,
# because two consumers read it: this module prints `store/console-answers.md` for a slot, and
# `scripts/console-walkthrough.mjs` renders `docs/play-console-walkthrough.md` for the owner. A
# second copy in either place is exactly the drift this exists to prevent, so a missing or malformed
# model is fatal here rather than a fallback: an unreadable form would silently print a sheet with
# holes in it, which is the defect the sheet itself was written to remove.
CONSOLE_FORM_RELATIVE = Path("scripts") / "lib" / "console-form.json"
_CONSOLE_FORM: dict[str, Any] | None = None


def console_form() -> dict[str, Any]:
    """The console form model, loaded once and validated enough to fail closed."""
    global _CONSOLE_FORM
    if _CONSOLE_FORM is None:
        path = ROOT / CONSOLE_FORM_RELATIVE
        relative = CONSOLE_FORM_RELATIVE.as_posix()
        if not path.is_file():
            raise StoreKitError(
                f"missing {relative}; it is the single source of truth for the Play Console answers, "
                "and both store-kit.py and scripts/console-walkthrough.mjs read it"
            )
        try:
            value = json.loads(read_text(path))
        except json.JSONDecodeError as exc:
            raise StoreKitError(f"invalid JSON in {relative}: line {exc.lineno}: {exc.msg}") from exc
        if not isinstance(value, dict) or value.get("schemaVersion") != 1:
            raise StoreKitError(f"{relative} must be a schemaVersion 1 JSON object")
        _CONSOLE_FORM = value
    return _CONSOLE_FORM


def listing_limits() -> dict[str, int]:
    """The listing character limits, read from the console-form model rather than restated here.

    `validate_listing` and the generated `store/console-answers.md` both need these, and a limit
    stated twice is a limit that will disagree with itself the first time Play changes one.
    """
    limits: dict[str, int] = {}
    section = console_form().get("listing") or {}
    for field in ("title", "shortDescription", "fullDescription"):
        value = section.get(field)
        if not isinstance(value, int) or isinstance(value, bool) or value <= 0:
            raise StoreKitError(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} listing.{field} must be a positive integer"
            )
        limits[field] = value
    return limits


def pack_documents() -> list[dict[str, Any]]:
    """The documents a pack must contain, and what each one's absence means, from the model.

    The inventory lives in ONE place because two gates read it: this module's `validate_store` and
    `scripts/verify-slot.mjs`. While each carried its own copy they disagreed, and the disagreement
    was invisible: `store/console-answers.md` was missing from a shipped slot's pack and
    `--validate-only` still printed `STORE-KIT: OK`, because the Python list did not name it. An
    absent deliverable no check can see is the same as one that was never required, so absence is a
    FAIL — except for a document the model itself describes as a warned absence, which has to carry
    the reason it is not a failure, or an exemption nobody can read is indistinguishable from a check
    nobody wrote.
    """
    section = console_form().get("packDocuments") or {}
    documents = section.get("documents")
    if not isinstance(documents, list) or not documents:
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} packDocuments.documents must list the documents "
            "`write_documents` writes: the pack's inventory is what this validator and the store gate "
            "both read, and a slot whose sheet is missing has to be told so by name"
        )
    seen: set[str] = set()
    for entry in documents:
        if not isinstance(entry, dict) or not str(entry.get("name") or "").strip():
            raise StoreKitError(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} packDocuments.documents has an entry without a name"
            )
        name = str(entry["name"])
        if name in seen:
            raise StoreKitError(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} packDocuments lists {name!r} twice; one document is "
                "one entry, or the verdicts can disagree about it"
            )
        seen.add(name)
        absence = str(entry.get("absence") or "")
        if absence not in ("fail", "warn"):
            raise StoreKitError(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} packDocuments names {name!r} with absence "
                f"{absence!r}; it is 'fail' or 'warn', so every document states what its absence means"
            )
        if absence == "warn" and not str(entry.get("absenceEn") or "").strip():
            raise StoreKitError(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} packDocuments lets {name!r} be absent with no "
                "reason to read; a warned absence without a stated reason reads as a check nobody wrote"
            )
        if not str(entry.get("whyEn") or "").strip() or not str(entry.get("whyRu") or "").strip():
            raise StoreKitError(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} packDocuments lists {name!r} without saying what "
                "the document is for; a failure that names a missing document has to be able to say "
                "what the pack lost, or the reader is told to restore a file nobody can identify"
            )
    return documents


def credentials_rule(compliance: dict[str, Any]) -> dict[str, Any]:
    """Which radio the App access declaration takes, decided by the model's own rules.

    The model states the conditions (`capability.<id>`, several joined with `+`, or `default`); this
    function only evaluates them, so which slot gets which answer is a declaration in the model rather
    than a branch here. A rule names the instruction template that goes with its answer, so the words
    and the answer cannot be decided in two places.

    `+` exists because two gates need their own words: a slot that both sells an unlock and requires a
    sign-in is not the union of the two single-gate instructions, and either one alone would assert
    something false about the other gate.
    """
    rules = (console_form().get("credentials") or {}).get("rules")
    if not isinstance(rules, list) or not rules:
        raise StoreKitError(f"{CONSOLE_FORM_RELATIVE.as_posix()} credentials.rules must be a non-empty list")
    enabled = {str(item) for item in (compliance.get("enabled") or [])}
    fallback: dict[str, Any] | None = None
    for rule in rules:
        if not isinstance(rule, dict) or "when" not in rule or "answer" not in rule:
            raise StoreKitError(f"{CONSOLE_FORM_RELATIVE.as_posix()} credentials.rules has an entry without when/answer")
        when = str(rule["when"])
        # `default` is remembered rather than returned on sight: a rule that matches several
        # capabilities has to be reachable wherever it sits in the list.
        if when == "default":
            fallback = rule
            continue
        conditions = [part.strip() for part in when.split("+") if part.strip()]
        for condition in conditions:
            if not condition.startswith("capability."):
                raise StoreKitError(
                    f"{CONSOLE_FORM_RELATIVE.as_posix()} credentials.rules has an unknown condition "
                    f"{condition!r}; use 'capability.<id>', several joined with '+', or 'default'"
                )
        if all(condition.split(".", 1)[1] in enabled for condition in conditions):
            return rule
    if fallback is None:
        raise StoreKitError(f"{CONSOLE_FORM_RELATIVE.as_posix()} credentials.rules has no 'default' entry")
    return fallback


def review_access_paragraph(compliance: dict[str, Any]) -> str:
    """The «Access» paragraph of `store/review-notes.md`, from the rule that answered the console.

    The paragraph is one answer printed in two places - the reviewer notes a human reads before
    uploading, and the App access declaration the console stores - so it comes from the same
    `credentials.rules` entry through the same `answer` field. It used to be a constant here, which
    made one app's situation every app's: a slot that sells an unlock shipped reviewer notes stating
    that no payment or code was required, while its own console declaration held a promo code. The
    words live in the model beside the answer; this function only chooses between them, and a branch
    the model has no paragraph for is a refusal rather than another branch's text.
    """
    credentials = console_form().get("credentials") or {}
    paragraphs = (credentials.get("reviewAccess") or {}).get("paragraphsEn")
    rule = credentials_rule(compliance)
    answer = str(rule.get("answer") or "")
    if not isinstance(paragraphs, dict) or not str(paragraphs.get(answer) or "").strip():
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} credentials.reviewAccess.paragraphsEn carries no "
            f"paragraph for the answer {answer!r} that credentials.rules gives this slot's enabled "
            "capabilities; the reviewer notes would have to state an access rule this app does not "
            "have, which is the one thing that paragraph exists to refuse"
        )
    text = str(paragraphs[answer]).replace("{reason}", str(rule.get("reasonEn") or "").strip())
    if "{" in text or "}" in text:
        # A token the rule cannot fill is printed verbatim by every f-string that would carry it, and
        # a reviewer reading `{reason}` learns nothing about how to reach the app's content.
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} credentials.reviewAccess.paragraphsEn[{answer!r}] "
            "carries a placeholder this slot's rule cannot fill: only `{reason}` is substituted, from "
            "the answer's own `reasonEn`"
        )
    return text


# The Kotlin declaration that tells Play which products this app buys. The string literal in the
# slot's own `monetization/MonetizationConfig.kt` is the ID the Play Billing Library is actually
# handed, so the pack reads it from source rather than carrying a second copy that can drift away
# from what the app requests. Both quote styles are accepted because Kotlin does not care which one
# a slot used; the backreference makes them a matched pair so a stray apostrophe cannot start a
# false match.
PRODUCT_ID_DECLARATION = re.compile(r"""\bProductId\(\s*(["'])([^"']+)\1\s*\)""")

# Only the shipped source set is read. This is what the docstring below claims and what the rule
# needs: a `ProductId("fixture")` in `src/test/` or `src/androidTest/` is a fixture the shipped app
# never hands to the Play Billing Library, so counting it would put a product on the console
# checklist that does not exist — a false instruction on the money path, which is the one place a
# wrong instruction is expensive. Restricting the scan cannot hide a real product either: a
# declaration outside `src/main/` means the shipped app declares none, and that now fails loudly
# instead of passing on a test's fixture.
SOURCE_SCAN_RELATIVE = Path("app") / "src" / "main"

# Directories under the shipped source set that hold build output or IDE state rather than source. A
# generated or machine-local copy of a Kotlin file must not be read as a second declaration.
SOURCE_SCAN_SKIP_DIRS = frozenset({"build", ".gradle", ".kotlin", ".idea", ".cxx"})


def slot_kotlin_sources(slot: Path) -> list[str]:
    """The slot's own shipped Kotlin, as text, for the readers that read the app's declarations.

    One walk, shared by every reader that scans source: a second copy of this loop is how the third
    reader comes to disagree with the first two about which files count as the shipped app.
    """
    if not (slot / "app").is_dir():
        return []
    source_root = slot / SOURCE_SCAN_RELATIVE
    if not source_root.is_dir():
        return []
    sources: list[str] = []
    for path in source_root.rglob("*.kt"):
        if not path.is_file() or SOURCE_SCAN_SKIP_DIRS & set(path.parts):
            continue
        try:
            raw = path.read_bytes()
        except OSError:
            continue
        if b"\x00" in raw:
            # A "Kotlin source" containing NUL bytes is not source; never parse a binary as text.
            continue
        try:
            sources.append(raw.decode("utf-8"))
        except UnicodeDecodeError:
            continue
    return sources


def declared_product_ids(slot: Path) -> list[str]:
    """Every Play product ID the slot's own shipped Kotlin declares to the billing capability.

    `ProductId("recordbook_pro")` is the whole declaration: it is what the app hands the Play
    Billing Library, so it is the ID the console product must carry. The result is sorted and
    de-duplicated - declaration sites repeat (a constant can be named in more than one file), and the
    console needs the set, not the sites.

    An empty list from a billing-enabled slot is a defect rather than a normal state: a purchase
    flow that references an unknown product fails in the reviewer's session, which is the comment
    the slot's own `MonetizationConfig.kt` already carries.
    """
    found: set[str] = set()
    for text in slot_kotlin_sources(slot):
        found.update(match.group(2) for match in PRODUCT_ID_DECLARATION.finditer(text))
    return sorted(found)


# The named arguments of the slot's own `BillingCatalog(...)`: the app's declaration of *which kind*
# of product it sells, and therefore of which Play Console page its products are created on. Play
# keeps in-app products (one-time purchases and consumables) on `Monetize with Play -> Products ->
# In-app products` and subscriptions on a separate page, so a form read on one of them is not the
# form a slot on the other walks. The names are read off the capability's own constructor rather than
# listed here, so a fourth list added to `BillingCatalog.kt` cannot be a product this factory never
# classifies: the model check compares its branches with that constructor and fails on the difference.
BILLING_CATALOG_CALL = re.compile(r"\bBillingCatalog\s*\(")
CATALOG_NAMED_ARG = re.compile(
    r"\b([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)(?=,\s*[A-Za-z_][A-Za-z0-9_]*\s*=|\Z)", re.DOTALL
)
CAPABILITY_CATALOG_RELATIVE = (
    Path("capabilities") / "billing" / "src" / "main" / "kotlin" / "com" / "gexetr" / "capability"
    / "billing" / "BillingCatalog.kt"
)
CATALOG_CONSTRUCTOR_ARG = re.compile(r"\bval\s+([A-Za-z_][A-Za-z0-9_]*)\s*:")


def catalog_constructor_args() -> set[str]:
    """The product lists `BillingCatalog` itself declares - the vocabulary this model must classify.

    A slot's catalog can only name arguments the capability has, and the console can only show a page
    per kind of product; reading the constructor is what keeps those two from drifting. A missing file
    is a failure rather than an empty set: "no lists" would make every slot's branch unreadable and
    the check below would pass on a file nobody read.
    """
    path = ROOT / CAPABILITY_CATALOG_RELATIVE
    if not path.is_file():
        raise StoreKitError(
            f"{CAPABILITY_CATALOG_RELATIVE.as_posix()} is missing, so the product kinds a slot may "
            "declare cannot be read from the capability that defines them"
        )
    text = read_text(path)
    match = re.search(r"class\s+BillingCatalog\s*\(", text)
    if not match:
        raise StoreKitError(
            f"{CAPABILITY_CATALOG_RELATIVE.as_posix()} declares no `class BillingCatalog(...)`, so the "
            "product kinds have no definition to read"
        )
    depth = 0
    for index in range(match.end(), len(text)):
        char = text[index]
        if char == "(":
            depth += 1
        elif char == ")":
            if depth == 0:
                body = text[match.end():index]
                break
            depth -= 1
    else:
        raise StoreKitError(f"{CAPABILITY_CATALOG_RELATIVE.as_posix()} has an unterminated constructor")
    return set(CATALOG_CONSTRUCTOR_ARG.findall(body))


def catalog_call_bodies(text: str) -> list[str]:
    """The argument text of every `BillingCatalog(...)` in one source file, by balanced parens.

    A regex cannot count parentheses, and the argument list contains them: `listOf(ProductId("x"))`
    closes twice inside the call. The body is therefore taken by depth, so the named arguments of a
    real catalog are read as they were written and an inner call is not mistaken for the end of the
    outer one.
    """
    bodies: list[str] = []
    for match in BILLING_CATALOG_CALL.finditer(text):
        depth = 0
        start = match.end()
        for index in range(start, len(text)):
            char = text[index]
            if char == "(":
                depth += 1
            elif char == ")":
                if depth == 0:
                    bodies.append(text[start:index])
                    break
                depth -= 1
    return bodies


def declared_catalog_kind_args(slot: Path) -> set[str]:
    """Which of the catalog's lists the slot fills with products, read off its own Kotlin.

    A named argument that is present and empty (`consumables = emptyList()`) declares nothing, so it
    is not a kind the slot sells; only a non-empty list is. Every named argument is returned, not only
    the ones this model knows: an argument nobody has classified has to surface as an unread branch
    rather than be silently skipped, which is how a slot would be told a form applies to it.
    """
    kinds: set[str] = set()
    for text in slot_kotlin_sources(slot):
        for body in catalog_call_bodies(text):
            for match in CATALOG_NAMED_ARG.finditer(body):
                # Kotlin allows a trailing comma after the last argument, and that comma is part of
                # the text a lazy match reaches: `emptyList(),` is an empty list, not a filled one.
                value = match.group(2).strip().rstrip(",").strip()
                if not re.fullmatch(r"(emptyList|listOf)\s*\(\s*\)", value):
                    kinds.add(match.group(1))
    return kinds


def capability_compliance(slot: Path) -> dict[str, Any]:
    helper = ROOT / "scripts" / "lib" / "capabilities.mjs"
    result = subprocess.run(
        ["node", str(helper), "--root", str(ROOT), "--slot", str(slot)],
        capture_output=True,
        text=True,
        check=False,
    )
    if result.returncode != 0:
        detail = (result.stderr or result.stdout or "capability aggregator failed").strip()
        raise StoreKitError(detail.removeprefix("FAIL: ").strip())
    try:
        value = json.loads(result.stdout)
    except json.JSONDecodeError as exc:
        raise StoreKitError(f"capability aggregator returned invalid JSON: {exc.msg}") from exc
    if not isinstance(value, dict):
        raise StoreKitError("capability aggregator must return a JSON object")
    return value


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


def recorded_idea(slot: Path, slug: str, strings_path: Path) -> str:
    state_path = slot / ".kiln-state.json"
    if state_path.is_file():
        try:
            state = json.loads(read_text(state_path))
        except json.JSONDecodeError as exc:
            raise StoreKitError(
                f"invalid JSON in {state_path}: line {exc.lineno}: {exc.msg}"
            ) from exc
        if isinstance(state, dict) and isinstance(state.get("idea"), str):
            idea = state["idea"].strip()
            if idea:
                return idea

    registry_path = ROOT / "registry" / "apps.json"
    if registry_path.is_file():
        try:
            registry = json.loads(read_text(registry_path))
        except json.JSONDecodeError as exc:
            raise StoreKitError(
                f"invalid JSON in {registry_path}: line {exc.lineno}: {exc.msg}"
            ) from exc
        apps = registry.get("apps", []) if isinstance(registry, dict) else []
        for app in apps if isinstance(apps, list) else []:
            if isinstance(app, dict) and app.get("id") == slug and isinstance(app.get("idea"), str):
                idea = app["idea"].strip()
                if idea:
                    return idea

    if strings_path.is_file():
        found = re.search(
            r'<string\s+name=["\']app_idea["\'](?:\s+[^>]*)?>(.*?)</string>',
            read_text(strings_path),
            re.S,
        )
        if found:
            idea = html.unescape(re.sub(r"<[^>]+>", "", found.group(1))).strip()
            if idea:
                return idea

    return ""


def normalize_idea(text: str) -> str:
    text = re.sub(r"\s+", " ", text).strip().rstrip()
    if text and not text.endswith((".", "!", "?")):
        text += "."
    return text


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
    slug = re.sub(r"^\d{4}-\d{2}-\d{2}-", "", slot.name)
    recorded = recorded_idea(slot, slug, strings_path)
    if recorded:
        idea = recorded
    else:
        sources = prose_lines(readme) + prose_lines(agents)
        idea = sources[0] if sources else f"A focused offline tool for {app_name}."
    idea = normalize_idea(idea)
    return {
        "app_name": app_name,
        "application_id": application_id,
        "namespace": namespace,
        "version_name": version_name,
        "version_code": version_code,
        "idea": idea,
        "recordedIdea": normalize_idea(recorded),
        "slug": slug,
    }


def truncate_chars(text: str, limit: int) -> str:
    text = re.sub(r"\s+", " ", text).strip()
    if limit <= 0:
        return ""
    if len(text) <= limit:
        return text
    if limit == 1:
        return "…"
    clipped = text[: limit - 1]
    if " " in clipped:
        clipped = clipped.rsplit(" ", 1)[0]
    clipped = clipped.rstrip(" ,.;:-")
    if not clipped:
        clipped = text[: limit - 1]
    return clipped + "…"


def english_copy(
    meta: dict[str, str], compliance: dict[str, Any] | None = None
) -> dict[str, Any]:
    name = truncate_chars(meta["app_name"], 30)
    idea = truncate_chars(meta["idea"], 80)
    bullets = [
        "Stay focused with a clear, distraction-free workflow",
        "See meaningful progress at a glance",
        "Keep your experience private and fully offline",
    ]
    compliance = compliance or {}
    if compliance.get("adsPresent"):
        disclosure = "Advertising and data use are disclosed in the Data safety section."
    elif compliance.get("dataCollected"):
        disclosure = "Data use is disclosed in the Data safety section."
    else:
        disclosure = "No enabled capability declares ads, analytics, or cloud tracking."
    full = (
        f"{name} helps you turn intention into steady progress.\n\n"
        f"{meta['idea']}\n\n"
        "What you can do:\n"
        + "\n".join(f"• {item}" for item in bullets)
        + f"\n\n{disclosure}"
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


def russian_copy(
    meta: dict[str, str], compliance: dict[str, Any] | None = None
) -> dict[str, Any]:
    name = truncate_chars(meta["app_name"], 30)
    bullets = [
        "Спокойный и понятный рабочий процесс",
        "Наглядный прогресс без лишних действий",
        "Полностью автономная и приватная работа",
    ]
    short = truncate_chars(f"{name}: простой способ сосредоточиться и видеть свой прогресс", 80)
    compliance = compliance or {}
    if compliance.get("adsPresent"):
        disclosure = "Приложение содержит рекламу; использование данных раскрыто в разделе безопасности данных."
    elif compliance.get("dataCollected"):
        disclosure = "Использование данных раскрыто в разделе безопасности данных."
    else:
        disclosure = "Подключенные возможности не декларируют рекламу, аналитику или облачное отслеживание."
    full = (
        f"{name} помогает превращать намерения в устойчивый прогресс.\n\n"
        f"{meta['idea']}\n\n"
        "Возможности:\n"
        + "\n".join(f"• {item}" for item in bullets)
        + f"\n\n{disclosure}"
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


def default_listing(
    meta: dict[str, str], locales: list[str], compliance: dict[str, Any]
) -> dict[str, Any]:
    locale_data = {}
    for locale in locales:
        locale_data[locale] = (
            russian_copy(meta, compliance)
            if locale == "ru-RU"
            else english_copy(meta, compliance)
        )
    digest = hashlib.sha256(meta["application_id"].encode("utf-8")).hexdigest()
    brand = f"#{digest[:6]}"
    accent = f"#{digest[6:12]}"
    listing = {
        "schemaVersion": 1,
        "appName": meta["app_name"],
        "slug": meta["slug"],
        "applicationId": meta["application_id"],
        "idea": truncate_chars(meta["idea"], 240),
        "defaultLocale": locales[0],
        "category": "PRODUCTIVITY",
        "tags": ["offline", "focus", "productivity"],
        "contactEmail": DEFAULT_CONTACT_EMAIL,
        # No default privacy URL exists: the policy has to be hosted somewhere the owner controls,
        # so this stays a reserved-host placeholder and the generated human-checks list says so.
        "privacyPolicyUrl": "https://example.com/privacy",
        "locales": locale_data,
        "dataSafety": {
            "collectsData": False,
            "reason": "Fully offline. No accounts, no analytics, no ads.",
            "dataTypes": [],
            "thirdPartySdks": [],
            "encryptedInTransit": False,
            "hasAccountDeletion": False,
        },
        "permissions": [],
        "ads": {"containsAds": False, "reason": "The app contains no advertising SDKs."},
        # The audience is left empty on purpose, and this is the one seeded field that is deliberately
        # not a value. It used to be `adults-only`, which is the branch *one* slot declared: every slot
        # scaffolded after it inherited an answer nobody made, and a slot built for children would have
        # ticked the 18+ box in the console unless its builder happened to notice the token. What the
        # factory can know at init time is the field's shape, not the app's audience, so the sheet and
        # the walkthrough print `TODO-HUMAN` naming the field - the model's own
        # `targetAudience.emptyChoiceRuleEn`, read by `target_audience_branch` - until the slot's own
        # `listing.json` names a branch. `--validate-only` stays green while the token is empty, because
        # an unmade decision is work rather than a defect in a pack under construction.
        "contentRating": {"questionnaire": "notes", "targetAudience": ""},
        "graphicsSource": {"iconGlyph": "focus", "brandColor": brand, "accentColor": accent},
    }
    apply_capability_compliance(listing, compliance)
    return listing


def apply_capability_compliance(
    listing: dict[str, Any], compliance: dict[str, Any]
) -> None:
    data_types = compliance.get("dataCollected", [])
    sdks = compliance.get("sdkDependencies", [])
    listing["permissions"] = compliance.get("permissions", [])
    listing["ads"] = {
        "containsAds": bool(compliance.get("adsPresent")),
        "reason": (
            "Enabled capabilities include advertising SDKs."
            if compliance.get("adsPresent")
            else "Enabled capabilities declare no advertising SDKs."
        ),
    }
    listing["dataSafety"] = {
        "collectsData": bool(data_types),
        "reason": (
            "Derived from the enabled capabilities and their bundled SDK declarations."
            if data_types
            else "Enabled capabilities declare no collected data."
        ),
        "dataTypes": data_types,
        "thirdPartySdks": sdks,
        # Only claimable when there is something to transmit. Asserting "encrypted in transit" for an
        # app that collects nothing describes a transmission that does not exist, and the Data safety
        # doc renders this verbatim as "Is collected data encrypted in transit? Yes".
        "encryptedInTransit": bool(data_types),
        "hasAccountDeletion": False,
    }
    listing["capabilityCompliance"] = {
        "enabled": compliance.get("enabled", []),
        "requiresConsent": bool(compliance.get("requiresConsent")),
        "requiresPurchaseAuthority": bool(compliance.get("requiresPurchaseAuthority")),
        "purchaseAuthority": compliance.get("purchaseAuthority"),
    }
    if compliance.get("adsPresent") or data_types:
        replacement = (
            "Advertising and data use are disclosed in the Data safety section."
            if compliance.get("adsPresent")
            else "Data use is disclosed in the Data safety section."
        )
        for locale in listing.get("locales", {}).values():
            if isinstance(locale, dict) and isinstance(locale.get("fullDescription"), str):
                locale["fullDescription"] = locale["fullDescription"].replace(
                    "Designed for a calm, direct experience without accounts, ads, analytics, or cloud tracking.",
                    replacement,
                )


def merge_missing_locales(
    listing: dict[str, Any],
    meta: dict[str, str],
    locales: list[str],
    compliance: dict[str, Any],
) -> None:
    locale_map = listing.setdefault("locales", {})
    for locale in locales:
        if locale not in locale_map:
            locale_map[locale] = (
                russian_copy(meta, compliance)
                if locale == "ru-RU"
                else english_copy(meta, compliance)
            )
    listing["defaultLocale"] = listing.get("defaultLocale") if listing.get("defaultLocale") in locales else locales[0]


IDEA_DERIVED_FIELDS = (
    "shortDescription",
    "fullDescription",
    "featureBullets",
    "screenshotCaptions",
)


def generator_localized_copy(
    meta: dict[str, str], compliance: dict[str, Any], locale: str
) -> dict[str, Any]:
    return russian_copy(meta, compliance) if locale == "ru-RU" else english_copy(meta, compliance)


def is_idea_derived_output(value: Any, prior_value: Any, previous_idea: str) -> bool:
    """Provenance test for one locale copy field against the listing's PREVIOUS idea.

    A field counts as generator output when either:

    * it is byte-identical to what the current generator would have produced for
      the previous idea (``prior_value``), or
    * it is an ellipsis-truncated *prefix* of the previous idea.  This is how the
      short description renders the idea, and it covers both the current
      word-boundary ``truncate_chars`` and the older mid-word truncation
      (``idea[:limit-1] + "…"``), so packs written by an earlier revision of the
      generator are still recognised as generator output rather than human copy.

    Anything else is treated as human-authored and preserved.  Limit: a human
    string that happens to end in ``…`` with a body equal to a prefix of the
    stored idea would be misread as generator output; that is implausible for a
    distinct authored sentence.
    """
    if value == prior_value:
        return True
    return (
        isinstance(value, str)
        and isinstance(previous_idea, str)
        and bool(previous_idea)
        and value.endswith("…")
        and previous_idea.startswith(value[:-1])
    )


def refresh_idea_from_record(
    listing: dict[str, Any],
    meta: dict[str, str],
    compliance: dict[str, Any],
) -> None:
    """Refresh ``idea`` and the copy the generator derives from it, from the slot record.

    The slot record (``.kiln-state.json`` / ``registry/apps.json`` /
    ``res/values/strings.xml`` ``app_idea``) is the source of truth for the idea.
    An existing store pack used to keep its scaffold boilerplate forever because
    generation preserved the existing ``listing.json`` untouched, so this refresh
    happens at the source instead.

    Each derived field is rewritten only when ``is_idea_derived_output`` proves it
    is still generator output for the listing's PREVIOUS ``idea``; a human edit
    fails that test, is preserved, and is reported.  ``contactEmail``,
    ``privacyPolicyUrl``, ``category``, ``tags``, the price fields, ``appName``,
    ``title``, and every other non-derived field are never touched here.  Limits:
    the previous idea is reconstructed from ``listing["idea"]`` (capped at 240
    chars), so a >240-char original idea leaves the full description unmatched and
    therefore preserved; and any change to ``app_name`` or to the compliance
    disclosure also makes the exact-match branch fail (preserved).  Failing safe
    means preserving, never clobbering.
    """
    recorded = meta.get("recordedIdea", "")
    if not recorded:
        return
    previous_idea = listing.get("idea") if isinstance(listing.get("idea"), str) else ""
    capped = truncate_chars(recorded, 240)
    if capped == previous_idea:
        return
    listing["idea"] = capped
    print(f"REFRESH listing.idea — {len(previous_idea)} -> {len(capped)} chars")
    prior_meta = {**meta, "idea": previous_idea}
    for locale, entry in list(listing.get("locales", {}).items()):
        if locale not in SUPPORTED_LOCALES or not isinstance(entry, dict):
            continue
        prior = generator_localized_copy(prior_meta, compliance, locale)
        fresh = generator_localized_copy(meta, compliance, locale)
        refreshed: list[str] = []
        preserved: list[str] = []
        for field in IDEA_DERIVED_FIELDS:
            if field not in prior or field not in fresh:
                continue
            current = entry.get(field)
            if is_idea_derived_output(current, prior[field], previous_idea):
                if current != fresh[field]:
                    entry[field] = fresh[field]
                    refreshed.append(field)
            else:
                preserved.append(field)
        if refreshed:
            print(f"REFRESH {locale} — refreshed {', '.join(refreshed)} from the slot record")
        if preserved:
            print(
                f"REFRESH {locale} — preserved {', '.join(preserved)} "
                "(not provably generator output for the previous idea)"
            )


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
    limits = listing_limits()
    for locale in locales:
        entry = locale_map.get(locale)
        if not isinstance(entry, dict):
            errors.append(f"listing.locales.{locale} is missing")
            continue
        for field in ("title", "shortDescription", "fullDescription"):
            limit = limits[field]
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
    # Keep the source artwork rectangular: the factory contract forbids a baked-in
    # rounded mask or transparent corners.
    canvas = gradient((512, 512), start, end).convert("RGBA")
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


REVIEW_NOTES_MARKER = "<!-- kiln:generated notes - delete this line to keep your own wording -->"
PRIVACY_MARKER = "<!-- kiln:generated policy - delete this line to keep your own wording -->"
DATA_SAFETY_MARKER = "<!-- kiln:generated safety answers - delete this line to keep your own wording -->"
RELEASE_NOTES_MARKER = "<!-- kiln:generated release notes - delete this line to keep your own wording -->"

# A freshly generated release note is a prompt, not text, and the prompt has to be visible as
# unfinished rather than reading like a finished sentence — the whole failure being fixed here is a
# pack that looked complete while a field Play demands had no prepared text at all.
RELEASE_NOTE_PROMPTS = {
    "en-US": "Describe what changed for the user in this release. Play shows this text on the store page; the limit is 500 characters.",
    "ru-RU": "Опишите, что изменилось для пользователя в этом выпуске. Play показывает этот текст на странице приложения; лимит — 500 символов.",
}
RELEASE_NOTE_UNFINISHED = "TODO-HUMAN"


def write_preserving(path: Path, content: str, marker: str) -> None:
    """Write generator output, but never over a file that was written by hand.

    A run regenerates the reviewer notes, the privacy policy and the Data safety answers as a side
    effect, which gives no hint that it is about to replace prose someone wrote. The generated
    wording is necessarily generic - it cannot know how this particular app is reviewed, what a
    teacher's record actually contains, or which SDK contributed a permission - so app-specific text
    is the expected outcome rather than an anomaly. Content without the generator marker is
    therefore treated as a human edit and kept, the same way the listing's copy is preserved rather
    than clobbered. Delete the file, or restore the marker line, to get generator output back.

    This matters most for `privacy.md` and `data-safety.md`: a policy that does not match the
    merged manifest is a compliance defect, and the generator cannot read the manifest, so the
    hand-written version is the one that has to survive. Observed once already - a hand-written
    policy was silently replaced by the capability-derived stub, and `verify-slot` passed the stub.
    """
    if path.is_file() and marker not in read_text(path):
        print(f"PRESERVE {path.name} - hand-written, not generator output")
        return
    path.write_text(content, encoding="utf-8")


def billing_checklist_lines(product_ids: list[str]) -> list[str]:
    """Console steps that block a first launch of a billing app, with the real IDs substituted.

    Each of the three is invisible until a purchase is attempted for real, which is why none of them
    was in the checklist: a product that was never created, a purchase option left in Draft - Play's
    own wording is that it "isn't returned by the Play Billing Library", so the app behaves as if
    the product does not exist - and a test account that is not a *license tester*, whose purchase
    is charged real money. The IDs come from the slot's own source (`declared_product_ids`), not
    from a constant here, so the console is told exactly what the app asks for.
    """
    ids = ", ".join(f"`{product_id}`" for product_id in product_ids)
    return [
        "",
        "## Billing — create and activate what the app asks for",
        "",
        "A purchase flow that references an unknown product fails in the reviewer's session, so every",
        "step below has to be done, and confirmed with a real test purchase, before review submits.",
        "",
        f"- [ ] Create each of these in Play Console → Monetize with Play → Products → In-app products: {ids}.",
        "- [ ] Activate each product's purchase option. An option left in Draft *isn't returned by the Play Billing Library*, so the app behaves as if the product does not exist.",
        "- [ ] Add the account that will test purchases to Play Console → Settings → License testing. An account added only as an internal tester on a track is **not** a license tester, and a purchase made by it is charged **real money**.",
    ]


CONSOLE_CLAIMS_FENCE = "kiln-console-claims"
CONSOLE_ANSWERS_MARKER = (
    "<!-- kiln:generated console answers - regenerated on every store-kit.py write; "
    "edit the pack, not this file -->"
)


def release_notes_block(path: Path) -> str:
    """The fenced block of `release-notes.md`, verbatim — the same characters a human pastes.

    The sheet embeds this rather than re-deriving the notes from `listing.json`: the block in
    `release-notes.md` is Play's own multi-language format and is what `validate_release_notes`
    measures, and a second rendering would be a second chance for the pasted text and the measured
    text to differ.
    """
    if not path.is_file():
        return ""
    match = re.search(r"^```[^\n]*\n(.*?)^```", read_text(path), re.MULTILINE | re.DOTALL)
    return match.group(1).rstrip("\n") if match else ""


def slot_economics(slot: Path) -> dict[str, Any]:
    """The slot's `economics.json`, or {} when it is absent or unreadable.

    The sheet needs one number out of it — `priceUsd` — and it must come from the slot's own plan
    rather than from a hand-written string in a document. A slot with no plan has no price to print,
    which the sheet reports instead of inventing one.
    """
    path = slot / "economics.json"
    if not path.is_file():
        return {}
    try:
        value = json.loads(read_text(path))
    except (json.JSONDecodeError, OSError):
        return {}
    return value if isinstance(value, dict) else {}


def price_locale_rule() -> dict[str, Any]:
    """How the console wants a price typed, read from the console-form model."""
    rule = ((console_form().get("billing") or {}).get("productForm") or {}).get("priceRule") or {}
    separator = str(rule.get("decimalSeparator") or "")
    decimals = rule.get("decimals")
    if not separator or not isinstance(decimals, int) or isinstance(decimals, bool) or decimals < 0:
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} billing.productForm.priceRule must carry a "
            "decimalSeparator and a non-negative integer `decimals`; the console price is rendered "
            "from them, and guessing the format is what sends a human into a dead field"
        )
    return {"decimalSeparator": separator, "decimals": decimals}


def console_price_display(economics: dict[str, Any]) -> str:
    """`priceUsd` 6.99 rendered in the form the console's price field accepts — `6,99`, not `6.99`.

    The owner physically could not type the price as `economics.json` wrote it: the Russian console's
    price field wants a comma. The data file keeps its dot-decimal format, because that is the format
    every other consumer of the plan reads; only the rendering changes, and it changes in one place.
    """
    raw = economics.get("priceUsd")
    if isinstance(raw, bool) or not isinstance(raw, (int, float)):
        return ""
    rule = price_locale_rule()
    return f"{float(raw):.{rule['decimals']}f}".replace(".", rule["decimalSeparator"])


def console_tag_literals(listing: dict[str, Any]) -> tuple[list[str], list[str]]:
    """The slot's own tags in the console's tag form, plus a note for every tag that had to change.

    The owner's decision was that the field is used rather than skipped, so a human needs the literal
    strings, not a rule to apply by eye. The console accepts a tag that starts with a lowercase letter
    and contains only digits, lowercase letters and hyphens — `teacher tools` is not one — so the
    conversion is deterministic and quiet about nothing: every transform and every drop is reported
    next to the result, and an empty result is reported rather than filled with an invention.
    """
    fields = (((console_form().get("billing") or {}).get("productForm") or {}).get("tags") or {})
    cap = int(fields.get("cap") or 20)
    literals: list[str] = []
    notes: list[str] = []
    for raw in listing.get("tags") or []:
        source = str(raw).strip()
        if not source:
            continue
        candidate = re.sub(r"[^a-z0-9-]+", "-", source.lower()).strip("-")
        candidate = re.sub(r"-{2,}", "-", candidate)[:cap].strip("-")
        if not candidate:
            notes.append(f"TODO-HUMAN: the pack's tag {source!r} becomes empty in the console's tag form; replace it.")
            continue
        if not re.match(r"^[a-z]", candidate):
            notes.append(
                f"TODO-HUMAN: the pack's tag {source!r} becomes {candidate!r}, which the console rejects "
                "because a tag must start with a lowercase letter; replace it."
            )
            continue
        if candidate != source:
            notes.append(f"{source!r} is entered as {candidate!r} to satisfy the console's tag rule.")
        if candidate not in literals:
            literals.append(candidate)
    return literals, notes


def console_purchase_option_ids(product_ids: list[str]) -> tuple[list[str], list[str]]:
    """The purchase-option IDs to type, derived from the product IDs, plus what had to change.

    The two fields have *different* rules and the difference is invisible until the field rejects the
    paste: a product ID may contain underscores and dots, and a purchase-option ID may not. The option
    ID is therefore derived from the product ID by the console's own alphabet rather than copied, and
    the change is reported so the human is not left comparing two strings that look almost equal.
    """
    fields = ((console_form().get("billing") or {}).get("productForm") or {}).get("step2") or {}
    cap = 63
    for field in fields.get("fields") or []:
        if field.get("id") == "offerId":
            cap = int(field.get("cap") or cap)
    options: list[str] = []
    notes: list[str] = []
    for product_id in product_ids:
        candidate = re.sub(r"[^a-z0-9-]+", "-", str(product_id).lower()).strip("-")
        candidate = re.sub(r"-{2,}", "-", candidate)[:cap].strip("-")
        while candidate and not re.match(r"^[a-z0-9]", candidate):
            candidate = candidate[1:]
        if not candidate:
            notes.append(
                f"TODO-HUMAN: the product ID {product_id!r} has no purchase-option ID in the console's "
                "alphabet; name the option by hand."
            )
            continue
        if candidate != product_id:
            notes.append(
                f"Product {product_id!r} takes the purchase-option ID {candidate!r}: unlike a product ID, "
                "an option ID may not contain underscores or dots."
            )
        if candidate not in options:
            options.append(candidate)
    return options, notes


def console_product_literals(
    store: Path, listing: dict[str, Any], locales: list[str]
) -> dict[str, Any]:
    """Every literal the one-time-product form asks for, derived from the pack it is printed beside."""
    product_ids = console_claims_products(store, listing)
    option_ids, option_notes = console_purchase_option_ids(product_ids)
    tags, tag_notes = console_tag_literals(listing)
    economics = slot_economics(store.parent)
    price = console_price_display(economics)
    default_locale = str(listing.get("defaultLocale") or (locales[0] if locales else ""))
    entry = (listing.get("locales") or {}).get(default_locale) or {}
    return {
        "productIds": product_ids,
        "purchaseOptionIds": option_ids,
        "purchaseOptionNotes": option_notes,
        "name": str(entry.get("title") or ""),
        "description": str(entry.get("shortDescription") or ""),
        "tags": tags,
        "tagNotes": tag_notes,
        "price": price,
        "priceUsd": economics.get("priceUsd"),
        "priceTodo": ""
        if price
        else "TODO-HUMAN: `economics.json` declares no numeric `priceUsd`, so there is no price to paste.",
        "defaultLocale": default_locale,
    }


def console_claims_products(store: Path, listing: dict[str, Any]) -> list[str]:
    """The product IDs the slot's own Kotlin declares, for a slot that actually sells something."""
    enabled = list((listing.get("capabilityCompliance") or {}).get("enabled") or [])
    return declared_product_ids(store.parent) if "billing" in enabled else []


def console_claims(
    store: Path, listing: dict[str, Any], locales: list[str], release_name: str
) -> dict[str, Any]:
    """The machine-readable half of the sheet: what it claims, so validation can compare it.

    Generation and validation both call this function, so the sheet cannot be generated with one
    set of answers and validated against another. A sheet left behind by an earlier pack is caught
    by comparing these values with the pack rather than by a hash of the file, which means the
    failure names the field that disagrees.

    An unfinished value is recorded as `None` rather than as the placeholder it still holds, for two
    reasons that point the same way. The sheet *reports* a placeholder field ("TODO-HUMAN: set it to
    a monitored inbox") instead of offering it to paste, so the file must not be failed for the
    value it refuses to emit. And the invariant the validator enforces is absolute - this file
    contains no reserved documentation host anywhere, including in this block - because a rule with
    a carve-out is the rule that gets argued with at the moment it matters.
    """
    compliance = listing.get("capabilityCompliance") or {}
    safety = listing.get("dataSafety") or {}
    url = str(listing.get("privacyPolicyUrl") or "").strip()
    claims: dict[str, Any] = {
        "schemaVersion": 1,
        "appName": listing.get("appName"),
        "slug": listing.get("slug"),
        "locales": list(locales),
        "contactEmail": None
        if is_placeholder_email(str(listing.get("contactEmail") or ""))
        else listing.get("contactEmail"),
        "privacyPolicyUrl": None if (not url or is_placeholder_url(url)) else url,
        "ads": bool((listing.get("ads") or {}).get("containsAds")),
        "collectsData": bool(safety.get("collectsData")),
        "dataTypes": [
            str(item.get("type"))
            for item in (safety.get("dataTypes") or [])
            if isinstance(item, dict) and item.get("type")
        ],
        "credentialsAnswer": str(credentials_rule(compliance)["answer"]),
        # The two declarations that resolve through a branch of the model's vocabulary: recording the
        # pack's own token makes a sheet generated for one branch fail once the pack names another,
        # which is the only way the derived cells can be checked at all.
        "contentRatingQuestionnaire": str((listing.get("contentRating") or {}).get("questionnaire") or ""),
        "targetAudience": str((listing.get("contentRating") or {}).get("targetAudience") or ""),
        "productIds": console_claims_products(store, listing),
        "releaseName": release_name,
    }
    if "billing" in list(compliance.get("enabled") or []):
        # The product form's claims exist only where the form exists. A slot that sells nothing has no
        # product, no option and no price, and adding empty keys would put product vocabulary into a
        # pack's answer sheet for a field that pack will never see.
        claims["productPriceDisplay"] = console_price_display(slot_economics(store.parent))
        claims["purchaseOptionIds"] = console_purchase_option_ids(claims["productIds"])[0]
        claims["productTags"] = console_tag_literals(listing)[0]
    return claims


def target_audience_choice_refusal(branches: str) -> str:
    """The sheet's sentence for a slot that has named no audience, composed from the model.

    One sentence lives in the model (`targetAudience.emptyChoiceRefusalEn` here, the Russian of the
    same sentence in `targetAudience.emptyChoiceRefusalRu`) and both documents print it: this sheet in
    English, `docs/play-console-walkthrough.md` in Russian. The field the sentence names and the
    branches it offers are both filled in from the place that owns them - the field from the screen's
    own `slotKey`, the branches from the dictionary's own keys - so neither document can drift from
    the other, and the sentence is not restated in a renderer. This is also the one place a *default*
    audience would have come from: the model seeds none, so a slot that has not chosen gets this
    sentence instead of the branch another slot declared.
    """
    table = console_form().get("targetAudience") or {}
    template = str(table.get("emptyChoiceRefusalEn") or "").strip()
    if not template:
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} targetAudience.emptyChoiceRefusalEn is missing, so a "
            "slot that has chosen no audience has no sentence to print"
        )
    for placeholder in ("{field}", "{branches}"):
        if placeholder not in template:
            raise StoreKitError(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} targetAudience.emptyChoiceRefusalEn carries no "
                f"`{placeholder}` placeholder, so a slot that has chosen no audience cannot be told "
                f"which {placeholder.strip('{}')} it is missing"
            )
    field = str(console_screen("declaration-target-audience").get("slotKey") or "").strip()
    if not field:
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} screen declaration-target-audience names no slotKey, "
            "so the empty-audience sentence cannot say which field is unanswered"
        )
    return template.replace("{branches}", branches).replace("{field}", field)


def target_audience_branch(listing: dict[str, Any]) -> tuple[str | None, dict[str, Any], list[str]]:
    """Resolve `listing.contentRating.targetAudience` into its branch of the console's wizard.

    The field is a **token of the model's vocabulary**, not free text, because three things are
    derived from it and all three have to agree: which of the console's six age-group boxes to tick,
    whether the optional minor-restriction box is part of the answer, and which wizard steps the
    console will skip. Written as prose, the same audience was spelled `18+` in three slots and
    `18 and over` in a fourth, and the generated sheet printed the value verbatim - a string a human
    cannot find on the screen, since the console's own label is `От 18 лет`.

    Aliases exist for the same reason they exist at all here: those packs are committed with their own
    approvals, so the equivalence is declared once, in the model, beside the branch it belongs to,
    instead of rewriting four slots' metadata. An unknown value is a refusal rather than a guess -
    a branch nobody has read in the console has no observed labels, and inventing them is the defect
    this function exists to prevent.
    """
    table = console_form().get("targetAudience") or {}
    audiences = table.get("audiences") or {}
    if not audiences:
        raise StoreKitError(f"{CONSOLE_FORM_RELATIVE.as_posix()} targetAudience.audiences is empty")
    rating = listing.get("contentRating") if isinstance(listing.get("contentRating"), dict) else {}
    raw = str((rating or {}).get("targetAudience") or "").strip()
    known = ", ".join(sorted(audiences))
    if not raw:
        return None, {}, [target_audience_choice_refusal(known)]
    for token, branch in audiences.items():
        names = [token] + [str(alias) for alias in (branch.get("aliases") or [])]
        if raw.casefold() in {name.casefold() for name in names}:
            return token, branch, []
    return None, {}, [
        f"TODO-HUMAN: `listing.contentRating.targetAudience` = {raw!r} is not a branch of "
        f"`targetAudience.audiences` ({known}). An unobserved branch is not a guess: the age-group "
        "boxes it ticks and the steps the console skips have never been read for it, so the branch is "
        "added to the model after it is observed in the console, not filled in here."
    ]


def target_audience_labels(branch: dict[str, Any]) -> list[str]:
    """The console's own labels for the boxes one branch ticks, in the console's order."""
    table = console_form().get("targetAudience") or {}
    by_id = {str(box["id"]): str(box["labelRu"]) for box in table.get("checkboxes") or []}
    labels = [by_id[token] for token in (branch.get("checkboxes") or []) if token in by_id]
    if not labels:
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} targetAudience.audiences has a branch whose "
            "checkboxes name nothing in targetAudience.checkboxes"
        )
    return labels


def target_audience_skipped_steps(branch: dict[str, Any]) -> list[str]:
    """The wizard steps the console greys out for one branch, named as the console names them."""
    table = console_form().get("targetAudience") or {}
    titles = {str(step["id"]): str(step["titleRu"]) for step in table.get("steps") or []}
    return [titles.get(str(step), str(step)) for step in (branch.get("skippedSteps") or [])]


def target_audience_skipped_note(branch: dict[str, Any], lang: str = "en") -> str:
    """What a human *sees* where the console greys a step out, taken from the model's own sentence.

    "Those steps are not filled in at all" is true and insufficient: the owner read an inactive
    `Шаг 2` tab as work left undone, because nothing said the tab is *supposed* to be inert for the
    branch. The model's sentence names the two observations that settle it - tabs carrying
    `aria-disabled="true"` / `is-disabled`, and `Далее` on step 1 opening `Шаг 5 из 5: Сводка`
    directly - and this reader substitutes this branch's step titles into it, so the note cannot
    describe one branch's steps while the row answers for another.
    """
    table = console_form().get("targetAudience") or {}
    suffix = lang[:1].upper() + lang[1:].lower()
    template = str(table.get(f"skippedStepsNote{suffix}") or "")
    if not template:
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} targetAudience.skippedStepsNote{suffix} is missing: a "
            "branch that skips wizard steps has to say what the console shows for them, or an "
            "inactive tab reads as a failure to complete."
        )
    titles = target_audience_skipped_steps(branch)
    if "{steps}" not in template:
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} targetAudience.skippedStepsNote{suffix} carries no "
            "`{steps}` placeholder, so the note cannot name the steps this branch skips"
        )
    return template.replace("{steps}", ", ".join(f"«{title}»" for title in titles))


def product_kind_rule() -> dict[str, Any]:
    """The model's statement of which product branch the observed console form belongs to."""
    rule = ((console_form().get("billing") or {}).get("productForm") or {}).get("productKind")
    if not isinstance(rule, dict) or not rule:
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} billing.productForm.productKind must declare the "
            "branch the observed product form was read on (`observedArgs`, `noteRu/En`, "
            "`rowRefusalRu/En`): a form that names no branch is a form printed for every slot."
        )
    return rule


def console_product_branch(slot: Path, lang: str = "ru") -> tuple[bool, str, str]:
    """(is this slot on the observed product branch, the branch's short label, why not).

    Play keeps in-app products and subscriptions on **different pages of the console**, so the
    product wizard that was read on 2026-09-29 - its path, both steps, its fields and every default -
    is the in-app-product form. A slot whose `BillingCatalog(...)` declares a subscription sells on a
    page no session has read, and every row of that form is then an instruction nobody wrote for that
    page. So the branch is read off the slot's own Kotlin and an unobserved branch *refuses*: the
    sheet prints `TODO-HUMAN` naming the field, and the model gains the page only once it is read.

    The kinds themselves are not guessed here: `inAppProducts` and `consumables` are both INAPP
    products (`BillingCatalog.kt` feeds both to `BillingClient.ProductType.INAPP`), so the model's
    `observedArgs` names the two lists that share the observed page, and `unobservedArgs` names the
    one that does not.
    """
    rule = product_kind_rule()
    suffix = suffix_of(lang)
    observed = {str(arg) for arg in rule.get("observedArgs") or []}
    unobserved = {str(arg): str(label) for arg, label in (rule.get("unobservedArgs") or {}).items()}
    short = str(rule.get(f"observedShort{suffix}") or "")
    args = declared_catalog_kind_args(slot)
    if not args:
        return False, short, str(rule.get(f"unknownKind{suffix}") or "")
    strange = sorted(args & set(unobserved))
    if strange:
        return False, short, str(rule.get(f"unobservedKind{suffix}") or "").replace(
            "{kind}", ", ".join(unobserved[arg] for arg in strange)
        )
    unclassified = sorted(args - observed - set(unobserved))
    if unclassified:
        # An argument this model has never classified is an unread branch, not a match: the console
        # page it is created on has not been compared with the observed one.
        return False, short, str(rule.get(f"unclassifiedArgs{suffix}") or "").replace(
            "{kind}", ", ".join(unclassified)
        )
    return True, short, ""


def console_product_state(ctx: dict[str, Any], lang: str = "en") -> tuple[bool, str, str]:
    """The slot's product branch, read once per generated sheet rather than once per row."""
    cached = ctx.get(f"productBranch{suffix_of(lang)}")
    if cached is None:
        cached = console_product_branch(ctx["slot"], lang)
        ctx[f"productBranch{suffix_of(lang)}"] = cached
    return cached


def suffix_of(lang: str) -> str:
    """`ru` -> `Ru`, so a model key and its language agree wherever the pair is read."""
    return lang[:1].upper() + lang[1:].lower()


def console_product_row_cell(
    screen: dict[str, Any], field: dict[str, Any], ctx: dict[str, Any], label: str
) -> str | None:
    """The `TODO-HUMAN` cell for a row of an unobserved product branch, or None when it applies.

    A screen may carry the gate for every row it shows (`productKindGate`, because the whole observed
    form belongs to one branch) and a single field may carry it for itself (`productKindBound`,
    because one dropdown of the promotions page answers differently on another branch). Both are the
    same claim, so both are read here - and the cell is composed from the model's own sentence with
    the *field's label* substituted, because a refusal that does not name the field is a row the human
    cannot come back to.
    """
    if not (screen.get("productKindGate") or field.get("productKindBound")):
        return None
    on_branch, short, why = console_product_state(ctx, "en")
    if on_branch:
        return None
    template = str(product_kind_rule().get("rowRefusalEn") or "")
    if "{field}" not in template:
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} billing.productForm.productKind.rowRefusalEn carries no "
            "`{field}` placeholder, so a refused row cannot name the field it is about"
        )
    return template.replace("{field}", label).replace("{observed}", short).replace("{kind}", why)


def console_product_path_cell(screen: dict[str, Any], ctx: dict[str, Any], page: str) -> str:
    """The page-path line of a gated screen: the observed path, or the refusal naming the real one."""
    if not screen.get("productKindGate"):
        return page
    on_branch, _short, _why = console_product_state(ctx, "en")
    if on_branch:
        return page
    refusal = str(product_kind_rule().get("pathRefusalEn") or "")
    return f"TODO-HUMAN — {refusal} The observed path: {page}"


def console_choice_label(field: dict[str, Any], token: str) -> tuple[str | None, list[str]]:
    """The label the console shows for one answer token, taken from the field that shows it.

    A radio is chosen by the word printed beside it and this console is Russian: `да` and
    `Нет, мое приложение не содержит рекламу` are on the screen, `Yes` and `No` are not. A field that
    carries the console's own `optionsRu` therefore owns the labels - `optionIds` states which option
    each token means, positionally - and a field whose options nobody has read falls back to the
    model's `choices` gloss, which is an English gloss rather than a claim about the console's words.
    An answer whose option the field does not carry is a refusal, not the nearest match.
    """
    labels = [str(item) for item in (field.get("optionsRu") or [])]
    ids = [str(item) for item in (field.get("optionIds") or [])] if field.get("optionIds") else []
    if labels and not ids:
        # The field carries the console's own labels and nothing says which token each one means, so
        # this reader cannot print them and the answer would fall through to the `choices` gloss - an
        # English gloss on a screen that is not marked unobserved, i.e. a row that reads as the answer
        # while no human can find it on the screen. Refusing here is the second line of defence:
        # `console_model_reader_errors` fails the same field before a sheet is rendered.
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} field {field.get('id')!r} carries {len(labels)} "
            "`optionsRu` labels and no `optionIds`, so nothing says which answer token each label is. "
            "The labels and the tokens are positional: name them, or the cell prints the model's gloss "
            "instead of the words the console shows."
        )
    if ids:
        if len(ids) != len(labels):
            raise StoreKitError(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} field {field.get('id')!r} declares {len(ids)} "
                f"optionIds and {len(labels)} optionsRu labels, and the two are positional, so they "
                "must be the same length"
            )
        if token in ids:
            return labels[ids.index(token)], []
        return None, [
            f"TODO-HUMAN: the answer is {token!r} and «"
            f"{console_field_name(field) or field.get('id')}» "
            f"carries no such option (it has {ids}). The label to click has never been read, so add "
            f"the observation to `{CONSOLE_FORM_RELATIVE.as_posix()}` rather than guess it."
        ]
    choice = (console_form().get("choices") or {}).get(token)
    gloss = str((choice or {}).get("glossEn") or "")
    if not gloss:
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} choices has no token {token!r}, and field "
            f"{field.get('id')!r} has no optionsRu to resolve it against"
        )
    return gloss, []


def content_rating_branch(listing: dict[str, Any]) -> tuple[str | None, dict[str, Any], list[str]]:
    """Resolve `listing.contentRating.questionnaire` into the branch of answers it names.

    The same discipline as `target_audience_branch`, for the same reason: the category radio and the
    ten answers of step 2 are the slot's own decisions, and the pack names *which set of decisions*
    this app takes rather than the sheet restating them for whoever ships next. Written as prose the
    whole questionnaire was one app's answers - a gradebook's - printed in every slot's guide, down to
    the product ID of a different app. An unknown token is a refusal: another branch's answers have
    never been read in the console, and inventing them is the defect this function exists to prevent.
    """
    table = console_form().get("contentRating") or {}
    branches = table.get("questionnaires") or {}
    if not branches:
        raise StoreKitError(f"{CONSOLE_FORM_RELATIVE.as_posix()} contentRating.questionnaires is empty")
    rating = listing.get("contentRating") if isinstance(listing.get("contentRating"), dict) else {}
    raw = str((rating or {}).get("questionnaire") or "").strip()
    known = ", ".join(sorted(branches))
    if not raw:
        return None, {}, [
            "TODO-HUMAN: `listing.contentRating.questionnaire` is empty. Set it to one of "
            f"{known}: the category radio and the ten answers of step 2 are derived from it."
        ]
    for token, branch in branches.items():
        names = [token] + [str(alias) for alias in (branch.get("aliases") or [])]
        if raw.casefold() in {name.casefold() for name in names}:
            return token, branch, []
    return None, {}, [
        f"TODO-HUMAN: `listing.contentRating.questionnaire` = {raw!r} is not a branch of "
        f"`contentRating.questionnaires` ({known}). An unobserved branch is not a guess: the category "
        "it chooses and the answers it takes have never been read in the console, so the branch is "
        "added to the model after it is observed, not filled in here."
    ]


def content_rating_plan(
    branch: dict[str, Any], token: str, enabled: list[str]
) -> dict[str, tuple[str, str]]:
    """One branch's answers, resolved for this slot: question id -> (answer token, why).

    An answer is usually a bare token, and a `when` answer is gated on an enabled capability - the
    digital-goods question is `Да` only where the slot actually sells something, and the same branch
    answers `Нет` in a slot that sells nothing. The `otherwise` token is mandatory on a gated answer,
    because a gate with an unstated fallback is a question left unanswered.

    A gated answer carries one reason per direction (`tokenWhyEn` / `otherwiseWhyEn`), because the
    reason for «Да» is false in the slot that answers «Нет»: the row's why column is read beside the
    answer it explains, and a why that covers both directions explains neither.
    """
    plan: dict[str, tuple[str, str]] = {}
    for question_id, entry in (branch.get("answers") or {}).items():
        why = str(branch.get("whyEn") or "")
        if isinstance(entry, dict):
            why = str(entry.get("whyEn") or why)
            condition = str(entry.get("when") or "")
            if condition:
                if not str(entry.get("otherwise") or ""):
                    raise StoreKitError(
                        f"{CONSOLE_FORM_RELATIVE.as_posix()} contentRating.questionnaires.{token}."
                        f"answers.{question_id} is gated on {condition!r} without an `otherwise`; the "
                        "branch must say what the other slots answer"
                    )
                capability = condition.split(".", 1)[1] if condition.startswith("capability.") else ""
                if capability and capability in enabled:
                    answer = str(entry.get("token") or "")
                    why = str(entry.get("tokenWhyEn") or why)
                else:
                    answer = str(entry.get("otherwise"))
                    why = str(entry.get("otherwiseWhyEn") or why)
            else:
                answer = str(entry.get("token") or "")
        else:
            answer = str(entry)
        if not answer:
            raise StoreKitError(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} contentRating.questionnaires.{token}.answers."
                f"{question_id} names no answer"
            )
        plan[str(question_id)] = (answer, why)
    return plan


def content_rating_category(branch: dict[str, Any]) -> tuple[str | None, list[str]]:
    """The console's own label for the category radio a branch takes.

    The three radios live where the console shows them - on the `category` field of the Content
    rating screen - and a branch names one by its `optionIds` id. Writing the labels a second time in
    the branch would be two copies of one console fact, and this model's whole discipline is that a
    console fact appears once.
    """
    table = console_form().get("contentRating") or {}
    ref = table.get("categoryField") or {}
    field = console_field(console_screen(str(ref.get("screen") or "")), str(ref.get("field") or ""))
    ids = [str(item) for item in (field.get("optionIds") or [])]
    labels = [str(item) for item in (field.get("optionsRu") or [])]
    category = str(branch.get("category") or "")
    if not ids or len(ids) != len(labels):
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} {ref.get('screen')}.{ref.get('field')} must carry "
            "optionIds and optionsRu of the same length: the branch names its radio by id"
        )
    if category not in ids:
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} contentRating.questionnaires has a branch whose "
            f"category {category!r} names no radio of {ref.get('field')!r} ({ids})"
        )
    return labels[ids.index(category)], []


# --- Data safety: the pack's own answers, and the one row no pack field answers -------------------
# The wizard asks in Russian and the pack answers with English tokens (`Financial info`, `App
# functionality`), so the model's two vocabularies are what turn a token into the label a human finds
# on the screen. A token neither vocabulary carries is a refusal: the console's own words for an
# unread category have never been observed, and translating one by eye produces a string that is not
# in the console.
#
# The wizard also has branches this session never opened - an app that shares what it collects, one
# whose data is optional, one that has accounts - and every one of them is refused rather than
# answered from the branch that was read. One row has no pack answer at all: «Эти данные
# обрабатываются временно?» is a claim about how Google Play processes a purchase inside its own
# systems, the pack describes no retention window, and a plausible default would be a claim nobody
# checked. That row names itself and stays `TODO-HUMAN` for the owner.
def data_safety_table() -> dict[str, Any]:
    """The Data safety vocabularies and wordings, refused as a whole when one of them is missing.

    Half a table is worse than none: a category with no Russian pair prints an English token as an
    answer, which is a value the human cannot find on the screen, so the reader refuses rather than
    printing what it happens to have.
    """
    table = console_form().get("dataSafety")
    if not isinstance(table, dict) or not table:
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} dataSafety is missing: the Data safety screen's rows "
            "are resolved through its category and purpose vocabularies"
        )
    for key in (
        "surveyAnswersRu",
        "dataTypeVocabulary",
        "purposeVocabulary",
        "categoryCellEn",
        "temporaryRefusalEn",
    ):
        if not table.get(key):
            raise StoreKitError(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} dataSafety.{key} is missing, and the Data safety "
                "rows are rendered from it"
            )
    return table


def data_safety_answers(listing: dict[str, Any]) -> dict[str, Any]:
    """The pack's own Data safety answers, or a failure naming the file that has none."""
    safety = listing.get("dataSafety")
    if not isinstance(safety, dict) or not safety:
        raise StoreKitError(
            "listing.json carries no dataSafety object, and the Data safety screen is answered from "
            "it; run store-kit.py once so the slot's capabilities fill it"
        )
    return safety


def data_safety_vocabulary(name: str, token: str, label_key: str) -> tuple[str | None, list[str]]:
    """The console's own Russian label for one English token of a model vocabulary.

    An unknown token is a refusal naming the vocabulary and the tokens it has, for the same reason
    every other unknown token in this model is: the label printed in its place would be a translation
    nobody read in the console, and a human sent looking for a row that is not there.
    """
    vocabulary = data_safety_table().get(name) or {}
    entry = vocabulary.get(token)
    if not isinstance(entry, dict):
        return None, [
            f"TODO-HUMAN: the pack declares {token!r}, and `dataSafety.{name}` of "
            f"{CONSOLE_FORM_RELATIVE.as_posix()} carries no such entry (it has "
            f"{', '.join(sorted(vocabulary))}); the console's own wording for an unread "
            "category is an observation to add, not a translation to guess."
        ]
    label = str(entry.get(label_key) or "")
    if not label:
        return None, [
            f"TODO-HUMAN: `dataSafety.{name}.{token}` carries no {label_key}, so the row has no label "
            "to click; record the console's own wording before the sheet can print an answer."
        ]
    return label, []


def data_safety_label(
    field: dict[str, Any], token: str, notes: list[str], why: str, block: str | None
) -> tuple[str, str, str | None, list[str]]:
    """One Data safety radio resolved to the console's label, refusals included.

    The label comes from the field the console shows it on (`optionIds`/`optionsRu`), never from an
    English gloss: the sheet's cell has to be a word that is on the screen.
    """
    label, label_notes = console_choice_label(field, token)
    if label is None:
        return "TODO-HUMAN (see the note below)", why, block, notes + label_notes
    return label, why, block, notes + label_notes


def console_declaration_answer(
    entry: dict[str, Any],
    listing: dict[str, Any],
    compliance: dict[str, Any],
    credentials: dict[str, Any],
) -> tuple[str | None, list[str]]:
    """The literal for one console declaration, plus the notes explaining it.

    Dispatch is on the `slotKey` the model names, so the model decides which slot field fills a
    console field and this function only knows how to read that field. An unknown key is a failure
    rather than a blank line: the sheet exists to stop a field being answered from memory, and an
    empty answer would read as one.

    A radio declaration returns a **token** (`yes`/`no`) rather than a word: the label to click is the
    console's, and `console_choice_label` resolves it against the field that shows it, so a
    Russian-language radio is never answered with an English gloss.
    """
    key = str(entry.get("slotKey") or "")
    console_name = entry.get("consoleNameRu") or entry.get("id") or "?"
    ads = listing.get("ads") if isinstance(listing.get("ads"), dict) else {}
    safety = listing.get("dataSafety") if isinstance(listing.get("dataSafety"), dict) else {}
    if key == "listing.privacyPolicyUrl":
        value = str(listing.get("privacyPolicyUrl") or "").strip()
        if not value:
            return None, [
                f"TODO-HUMAN: «{console_name}» takes `listing.privacyPolicyUrl`, and `listing.json` has none. "
                "Host `store/privacy.md` over HTTPS and set the real URL before pasting anything here."
            ]
        if is_placeholder_url(value):
            return None, [
                f"TODO-HUMAN: «{console_name}» takes `listing.privacyPolicyUrl`, and the value in "
                "`listing.json` is still one of the reserved documentation hosts this factory refuses to "
                "ship (the placeholder rule is `PLACEHOLDER_HOSTS` in `scripts/store-kit.py`). Host "
                "`store/privacy.md` at a real HTTPS URL and set the field."
            ]
        return value, [f"`listing.privacyPolicyUrl` — the same URL `store/privacy.md` is hosted at."]
    if key == "listing.ads.containsAds":
        present = bool(ads.get("containsAds"))
        reason = str(ads.get("reason") or "no reason recorded in listing.json")
        return ("yes" if present else "no"), [
            f"`listing.ads.containsAds` = {str(present).lower()} — {reason}"
        ]
    if key == "derived.credentials":
        answer = "yes" if str(credentials.get("answer")) == "yes" else "no"
        return answer, [
            f"Model rule «{credentials.get('when')}»: {credentials.get('reasonEn')}",
            "The dialog fields and the instructions block are in the next section.",
        ]
    if key == "listing.contentRating.questionnaire":
        token, branch, notes = content_rating_branch(listing)
        if token is None:
            return None, notes
        category, _ = content_rating_category(branch)
        notes.append(
            f"`listing.contentRating.questionnaire` = {token!r} → the step-1 category is «{category}» "
            "and step 2 takes the branch's answers; both are derived, so nothing here has to agree with "
            "the console by hand."
        )
        return "tick " + f"«{category}»", notes
    if key == "listing.contentRating.targetAudience":
        token, branch, notes = target_audience_branch(listing)
        if token is None:
            return None, notes
        labels = target_audience_labels(branch)
        notes.append(
            f"`listing.contentRating.targetAudience` = {token!r} → tick "
            + ", ".join(f"«{label}»" for label in labels)
        )
        skipped = target_audience_skipped_steps(branch)
        if skipped:
            notes.append(target_audience_skipped_note(branch))
        return "tick " + ", ".join(f"«{label}»" for label in labels), notes
    if key == "listing.dataSafety":
        # A form of twenty-odd toggles, and the pack answers it as one line here plus the per-type and
        # per-SDK detail below: a table cell carrying line breaks would break the generated table, and
        # a cell that describes *where to look* - which is what this screen shipped with - is not an
        # answer at all.
        collects = bool(safety.get("collectsData"))
        types = [item for item in (safety.get("dataTypes") or []) if isinstance(item, dict)]
        cell = f"Collects or shares required user data: {'Yes' if collects else 'No'}"
        if collects:
            cell += f"; encrypted in transit: {'Yes' if safety.get('encryptedInTransit') else 'No'}"
        cell += "; deletion: " + ("Yes" if safety.get("hasAccountDeletion") else "no account exists")
        notes = [
            f"`listing.dataSafety` — {safety.get('reason')}; the same answers are written out in `store/data-safety.md`."
        ]
        for item in types:
            notes.append(
                f"Data type: {item.get('type')} — collected, shared {'yes' if item.get('shared') else 'no'}, "
                f"purpose {item.get('purpose')}"
            )
        for sdk in safety.get("thirdPartySdks") or []:
            if isinstance(sdk, dict):
                notes.append(
                    f"Bundled SDK to reconcile: {sdk.get('name')} {sdk.get('version')} — {sdk.get('policyUrl')}"
                )
        if types:
            notes.append(
                "Each of these is declared in the Data safety form itself, not here: this row is the "
                "pack's answer, the form is where a human enters it."
            )
        return cell, notes
    if key == "constant.no":
        # An answer no slot decides, declared as a constant of the catalogue rather than as a value in
        # the field's own prose. Three declarations answer «no» for every slot this factory ships (no
        # government affiliation, no financial product, no health data), and each of them used to
        # carry the word in its `valueEn` - a lowercase English token where the console shows a label.
        # Routing it here puts the answer through the same vocabulary as every other radio, so the
        # cell is a label resolved by `console_choice_label` and an unread radio is marked instead of
        # spelled.
        return "no", [
            f"`constant.no` — the answer is a property of the catalogue, not of this slot, so the "
            f"model declares it once and «{console_name}» reads it from there. The console's own "
            "wording for this radio has not been read in any session: this screen carries "
            "NEEDS-VERIFICATION for that reason, and the cell prints the model's gloss of the token "
            "rather than a string pretending to be the console's label."
        ]
    if key == "capability.ads":
        # The Advertising ID declaration is a claim about the contents of the bundle, and the model
        # can only answer it in one direction. A slot with no advertising capability has no SDK that
        # reads the identifier, which the pack already asserts. A slot *with* it has been declared to
        # bundle an advertising SDK, and whether that SDK reads the identifier is a fact about the
        # built AAB - so this is work for a human naming the field, not an inference from the word
        # "ads" that would silently mark every ads slot the same way.
        present = "ads" in (compliance.get("enabled") or [])
        if present:
            return None, [
                f"TODO-HUMAN: «{console_name}» cannot be answered from the pack for a slot whose `ads` "
                "capability is enabled. Read the built AAB (`com.google.android.gms.permission.AD_ID`, "
                "or the advertising SDK's own manifest) and answer from the artifact, then record the "
                "answer in `listing.advertisingId` so the next sheet can print it."
            ]
        return "no", [
            "`capability.ads` is not enabled — no advertising or analytics SDK in the build reads the "
            "identifier, which is what `listing.ads.containsAds` already declares on the Ads screen."
        ]
    raise StoreKitError(
        f"{CONSOLE_FORM_RELATIVE.as_posix()} declares slotKey {key!r} for the “{console_name}” declaration, "
        "and store-kit.py has no reader for it; add one before generating the sheet"
    )


def console_screens() -> list[dict[str, Any]]:
    """The model's screens in console order — the walk this sheet and the walkthrough both follow."""
    screens = console_form().get("screens")
    if not isinstance(screens, list) or not screens:
        raise StoreKitError(f"{CONSOLE_FORM_RELATIVE.as_posix()} screens must be a non-empty list")
    return sorted(screens, key=lambda item: item["order"])


def console_declarations() -> list[dict[str, Any]]:
    """The App-content declarations of the model, in the console's own order.

    Derived from `screens` rather than stored a second time: a declaration is a screen with
    `declaration: true`, and its `declarationOrder` is the position the console gives it. A copy of
    the ten entries would be a second place to edit, which is the drift this model exists to remove.
    """
    entries = [screen for screen in console_form().get("screens") or [] if screen.get("declaration")]
    if len(entries) != 10:
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} must mark exactly ten screens as declarations, "
            f"and it marks {len(entries)}"
        )
    return sorted(entries, key=lambda item: item["declarationOrder"])


def console_literal_readers() -> set[str]:
    """The literals this module can fill, read off its own dispatch.

    The model's `literalReaders` is the list a human reads; this is the list the code actually
    implements, and the two are compared by the test suite rather than trusted, because a drift
    between them is invisible: a field naming a literal with no reader falls back to a `valueRu`
    written for one app, which reads as an answer.

    The search is scoped to `console_field_cell`'s own body rather than the whole file: written over
    the file it matched any `if literal == "..."` anywhere, including this function's own pattern, so
    the sensor could report a reader that is not one. Scoping by the function that does the dispatch
    makes the reading and the thing read the same code.
    """
    return set(re.findall(r'^\s*if literal == "([^"]+)":', python_function_source("console_field_cell"), re.MULTILINE))


def python_function_source(name: str) -> str:
    """The source of one top-level function, so a check reads the code it grades and nothing else."""
    source = read_text(Path(__file__))
    match = re.search(rf"^def {re.escape(name)}\(", source, re.MULTILINE)
    if not match:
        raise StoreKitError(f"store-kit.py has no function {name!r}; a reader check names it by name")
    start = match.start()
    following = re.search(r"^def ", source[match.end():], re.MULTILINE)
    return source[start:] if not following else source[start:match.end() + following.start()]


def console_declaration_readers() -> set[str]:
    """The slot keys `console_declaration_answer` can actually answer, read off its own dispatch.

    A declaration screen names the pack path that fills it (`slotKey`), and a `declaration` field is
    filled by `console_declaration_answer`, which dispatches on that key. A screen naming a key
    nobody reads used to be discovered only when a slot generated its sheet - a crash at the last
    step, on the machine of whoever was shipping - so the model checks it instead: the answer must
    come from the slot's own pack *through a reader that exists*, and a model that names a key
    without one is a model that cannot be answered at all.
    """
    return set(re.findall(r'^\s*if key == "([^"]+)":', python_function_source("console_declaration_answer"), re.MULTILINE))


# The separators the model uses to *annotate* a row - a breadcrumb into the screen, or the group a
# question sits under. They belong in `whyRu`, which explains a field, and never in the label, which
# the sheet's first column promises is the row the console prints.
CONSOLE_LABEL_ANNOTATIONS = (" · ", " → ")
# A label that leads with English and brackets the Russian is the guide's own gloss rather than the
# console's label: on the Russian console the label leads, and a translation is what this model puts
# in `nameEn` instead.
CONSOLE_LABEL_GLOSS = re.compile(r"^[A-Za-z][A-Za-z' ]*\([^)]*[А-Яа-я]")


def console_label_errors(screen: dict[str, Any], field: dict[str, Any], *, label_not_quoted: bool) -> list[str]:
    """Failures for one field's label: is it the console's own row, and is it stored once?

    The first column of both generated guides is titled "the field, as the console shows it", so a
    label carrying the guide's own annotation is a row a human cannot find - and, worse, a label a
    *truthful* transcription of the console fails to match, because the handoff compares what the
    console printed against what the model stores. Two copies of one label are the same defect in the
    other direction: the optional minor-restriction box's sentence lives on the audience branch that
    answers it, and the field's own copy had already drifted from it.

    `label_not_quoted` is the screen's own verdict that its first column is not verbatim - the one
    screen where the row wording was never quoted, marked `nameNotQuoted` and disclosed in both
    generated guides rather than excused silently. It is a marker of its own rather than the screen's
    `needsVerification`, which is a statement about an unverified *claim*: one screen can carry read
    labels and an unverified linkage, and borrowing that flag here would excuse a label nothing
    checked.
    """
    errors: list[str] = []
    conflict = False
    for lang in ("ru", "en"):
        path_key, flat_key = CONSOLE_LABEL_LANGS[lang]
        path = field.get(path_key)
        if path and field.get(flat_key):
            errors.append(
                f"{screen.get('id')} → {field.get('id')} carries both {path_key} and {flat_key}; the "
                f"label is read from the model path or stored on the field, never both"
            )
            conflict = True
            continue
        if path and not str(console_model_value(str(path)) or "").strip():
            errors.append(
                f"{screen.get('id')} → {field.get('id')} names {path_key} {path!r}, which resolves to "
                "nothing in the model, so the field has no label at all"
            )
            conflict = True
    if label_not_quoted or conflict:
        return errors
    label = console_field_name(field)
    if not label:
        errors.append(f"{screen.get('id')} → {field.get('id')} has no label for the console's row")
        return errors
    annotation = next((mark for mark in CONSOLE_LABEL_ANNOTATIONS if mark in label), None)
    if annotation or "«" in label:
        errors.append(
            f"{screen.get('id')} → {field.get('id')} labels the row {label!r}, which is the guide's own "
            "annotation rather than the console's label (the mark is "
            f"{annotation or '«…»'}); the first column is the row a human finds on the screen, and the "
            "context belongs in `whyRu`"
        )
    elif CONSOLE_LABEL_GLOSS.match(label):
        errors.append(
            f"{screen.get('id')} → {field.get('id')} labels the row {label!r}, which leads with English "
            "and brackets the Russian: that is this model's gloss, not the console's label. Put the "
            "console's own wording in `nameRu` and the gloss in `nameEn`/`whyRu`"
        )
    return errors


# The two field kinds a human picks by reading a word on the screen. A `text` field is typed and a
# `note` field is read, so neither needs a label; a radio and a checkbox have no answer without one.
LABEL_CHOOSING_KINDS = frozenset({"radio", "checkbox"})

# The field kinds whose third column is not a value to type but a state to look at: `read` states what
# the console shows (`Черновик` before the first rollout, `Опубликовано` after it) and what an action
# does, so no pack field answers it and no decision is being avoided.
CONSOLE_STATE_KINDS = frozenset({"read"})

# The markers a row uses to say where its answer comes from, in the order the checks read them. The
# words are the contract: a row naming none of them is a row whose cell was written from memory.
CONSOLE_ANSWER_SOURCES = (
    ("literal", "a reader fills this row from the pack"),
    ("modelFormality", "the console's own default or a formality of the form"),
    ("ownerDecision", "the owner's own decision, which no pack field sets"),
    ("state", "a state to read off the screen rather than a value to type"),
    ("listing", "a localized listing field, printed one row per locale"),
    ("branch", "a value read on one branch of a product form, refused on another"),
)


def console_field_answer_source(field: dict[str, Any]) -> str:
    """Which kind of answer one row carries, or `""` when the model does not say.

    The sheet's third column is what a human types into the console, so a row whose words came from
    the session that observed the form - `нужное число кодов (владелец выпускал 100)` - is the defect
    the model exists against, wherever the row sits. The model has to name the kind rather than rely
    on the words looking plausible, because the defect is exactly a cell that looks plausible.
    """
    if field.get("literal"):
        return "literal"
    if field.get("modelFormality"):
        return "formality"
    if field.get("ownerDecision"):
        return "owner"
    if str(field.get("kind") or "") in CONSOLE_STATE_KINDS:
        return "state"
    if str(field.get("slotKey") or "") in LISTING_TEXT_LITERALS:
        return "listing"
    if field.get("productKindBound"):
        return "branch"
    return ""


def console_model_reader_errors() -> list[str]:
    """The model's own declaration of its readers, checked against what the model then uses.

    Every field of an App-content declaration has to be filled by a reader: the sheet exists to stop a
    console field being answered from memory, and a field carrying only a `valueRu` is answered from
    the memory of whichever slot it was written for. A console box that is a fixed formality rather
    than a slot-data answer says so (`modelFormality` + `modelFormalityEn`) instead of being silent,
    so "I did not think about this field" and "this field is a formality" cannot look alike.

    A radio or a checkbox is clicked by a *word*, and a word the model does not carry is a word the
    human cannot find on the screen. So a label-choosing field is answered one of three ways and none
    of them is silence: the console's own labels sit on the field (`optionsRu`), the reader that fills
    the field prints labels read elsewhere in the model (`labelSources`), or the screen says its
    wording was never read (`needsVerification`). The three are checked here rather than trusted,
    because "the answer is a token" and "the label is on the screen" are different claims and only the
    second one can be pasted.

    Sub-screens are walked as well, because a declaration's answer is not only on the screen the
    console names: the Content rating declaration carries its category radio and its ten questionnaire
    answers one level down, and those were exactly the rows that shipped one app's answers to every
    other app.

    Two further claims are checked here because they are the same claim in different clothes. A
    declaration field answered through the literal `declaration` is filled by the reader that
    dispatches on the screen's own `slotKey`, so that key has to have a reader: a screen naming a pack
    path nothing reads is a screen nobody can answer, and it used to be found only by crashing on the
    machine of whoever was shipping. And every field's *label* has to be the console's own row, read
    from the one place the model stores it, because the handoff compares a live transcription against
    that label and a label carrying the guide's own annotation rejects the truth.
    """
    readers = console_form().get("literalReaders")
    if not isinstance(readers, dict) or not readers:
        return [f"{CONSOLE_FORM_RELATIVE.as_posix()} literalReaders must list the readers a field may name"]
    # The model's list of readers and the ones this module implements are compared rather than
    # trusted, in both directions: a declared reader with no dispatch is documentation that describes
    # code nobody wrote, and an implemented reader nobody declares is a reader no field can name. The
    # docstring claimed this comparison long before any check made it, which is the same defect as a
    # message that restates a rule instead of running it.
    implemented = console_literal_readers()
    declared = {str(key) for key in readers}
    if declared - implemented:
        return [
            f"{CONSOLE_FORM_RELATIVE.as_posix()} literalReaders declares {', '.join(sorted(declared - implemented))}, "
            "and store-kit.py's `console_field_cell` has no dispatch for them: a list of readers the code "
            "does not implement is a list a field may name and be answered from `valueRu` instead"
        ]
    if implemented - declared:
        return [
            f"store-kit.py fills the literal(s) {', '.join(sorted(implemented - declared))}, and "
            f"{CONSOLE_FORM_RELATIVE.as_posix()} literalReaders does not declare them, so no field can "
            "name them and no reader of the model knows they exist"
        ]
    label_sources = console_form().get("labelSources") or {}
    declaration_readers = console_declaration_readers()
    errors: list[str] = []
    # Every field's label, on every screen: both guides head their first column "the field, as the
    # console shows it", so the rule is not a rule about declarations. A screen whose row wording was
    # never quoted says so (`nameNotQuoted`, disclosed in both guides) and a sub-screen inherits that
    # from the screen that owns it; a marker with no reason to read is a silent exemption.
    for screen in console_screens():
        if screen.get("nameNotQuoted") and not str(screen.get("nameNotQuotedRu") or "").strip():
            errors.append(
                f"{screen.get('id')} is marked nameNotQuoted without a reason to read; an exemption "
                "with no statement beside it is indistinguishable from a label nobody checked"
            )
        label_not_quoted = bool(screen.get("nameNotQuoted"))
        for field in console_screen_fields(screen):
            errors.extend(console_label_errors(screen, field, label_not_quoted=label_not_quoted))
        for sub in screen.get("subscreens") or []:
            if sub.get("nameNotQuoted") and not str(sub.get("nameNotQuotedRu") or "").strip():
                errors.append(
                    f"{sub.get('id')} is marked nameNotQuoted without a reason to read; an exemption "
                    "with no statement beside it is indistinguishable from a label nobody checked"
                )
            sub_not_quoted = bool(label_not_quoted or sub.get("nameNotQuoted"))
            for field in console_screen_fields(sub):
                errors.extend(console_label_errors(sub, field, label_not_quoted=sub_not_quoted))
    for declaration in console_declarations():
        slot_key = str(declaration.get("slotKey") or "")
        if slot_key not in declaration_readers:
            errors.append(
                f"{declaration.get('id')} names slotKey {slot_key!r}, and `console_declaration_answer` "
                f"has no branch for it (it reads {', '.join(sorted(declaration_readers))}); a declaration "
                "whose pack path nothing reads is a field no slot can answer, so add the reader before "
                "the model names the key"
            )
        for screen in [declaration] + list(declaration.get("subscreens") or []):
            for field in console_screen_fields(screen):
                label = f"{screen.get('id')} → {field.get('id')}"
                literal = field.get("literal")
                if literal:
                    if str(literal) not in readers:
                        errors.append(
                            f"{label} names literal {literal!r}, which is not in the model's literalReaders "
                            f"({', '.join(sorted(str(key) for key in readers))})"
                        )
                elif field.get("modelFormality"):
                    if not str(field.get("modelFormalityEn") or "").strip():
                        errors.append(f"{label} is declared a model formality without a reason to read")
                else:
                    errors.append(
                        f"{label} has no literal, so nothing in the slot's pack fills it: every declaration "
                        "field is either read through a literal or declares itself a modelFormality"
                    )
                    continue
                if str(field.get("kind") or "") not in LABEL_CHOOSING_KINDS:
                    continue
                labels = field.get("optionsRu")
                if labels:
                    # A field that carries the console's own option labels has to say which token each
                    # one means: `console_choice_label` matches the two positionally, and a field with
                    # labels and no `optionIds` prints the `choices` gloss instead of the labels it
                    # stores - an unmarked gloss on a screen that is not marked unobserved either, so
                    # nothing in either guide says the wording was never read.
                    ids = field.get("optionIds")
                    if not isinstance(ids, list) or len(ids) != len(labels):
                        errors.append(
                            f"{label} carries {len(labels)} `optionsRu` labels and "
                            f"{len(ids) if isinstance(ids, list) else 0} `optionIds`; the two are "
                            "positional, so a field holding the console's labels without their tokens "
                            "prints the model's English gloss instead of the console's own words, and "
                            "no screen marker covers it. Name which token each label answers."
                        )
                    continue
                if literal and str(literal) in label_sources:
                    continue
                if screen.get("needsVerification"):
                    continue
                errors.append(
                    f"{label} is a {field.get('kind')} whose labels the model does not carry: it has no "
                    f"`optionsRu`, its literal {literal!r} is not in `labelSources`, and screen "
                    f"{screen.get('id')!r} is not marked `needsVerification`. Put the observed labels on "
                    "the field, point it at the model path that reads them, or mark the screen unobserved "
                    "— an answer with no label on the screen is an instruction a human cannot follow."
                )
    for literal, source in sorted(label_sources.items()):
        if str(literal) not in {str(field.get("literal")) for field in _all_console_fields()}:
            errors.append(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} labelSources names {literal!r}, and no field of the "
                f"model declares that literal, so nothing reads {source!r}"
            )
    # --- Every row of every screen, not only the declarations -------------------------------------
    # The sheet's third column is the string a human types into the console, so the "answer written
    # for one app" defect is not a defect of declaration screens: it is a defect of any row whose
    # words came from the session that observed the console instead of from this slot's pack. The
    # model therefore has to say which kind of row each one is, and these are the kinds - read
    # through a literal, the console's own default or a formality of the form, the owner's own
    # decision, a state to read off the screen, a localized listing field, or a value that belongs to
    # the product branch the row was read on. A row that names none of them prints its `valueRu` as
    # this app's answer, and nothing in either guide says the words were never this slot's.
    for screen in console_screens():
        if screen.get("declaration"):
            # A declaration screen's rows answer a different question - whether this slot's pack fills
            # them at all - and the loop above states that rule with its own message. Two messages for
            # one defect are two places to keep in agreement.
            continue
        for section in [screen] + list(screen.get("subscreens") or []):
            for field in console_screen_fields(section):
                label = f"screens[{screen.get('id')}] → {section.get('id')} → {field.get('id')}"
                source = console_field_answer_source(field)
                if not source:
                    errors.append(
                        f"{label} carries no answer source: it prints {field.get('valueRu')!r} as the "
                        "string to type, and nothing says whether that is the console's own default "
                        "(`modelFormality`), the owner's own decision (`ownerDecision`), a state to read "
                        "off the screen (`kind: read`), or a value this slot's pack sets (`literal`). A "
                        "row whose words came from one session is the next app's wrong answer."
                    )
                    continue
                if field.get("ownerDecision") and (field.get("literal") or field.get("modelFormality")):
                    second = "literal" if field.get("literal") else "modelFormality"
                    errors.append(
                        f"{label} is marked the owner's own decision and also names `{second}`: a row is "
                        "read from the pack, or it is the console's own default, or the owner chooses it - "
                        "never both, and a row that says both has two answers to print"
                    )
                    continue
                if source == "owner":
                    for key in ("ownerDecisionRu", "ownerDecisionEn"):
                        if not str(field.get(key) or "").strip():
                            errors.append(
                                f"{label} is declared the owner's own decision without `{key}`: the "
                                "refusal prints that sentence, so a marker with no statement beside it "
                                "is a row nobody can act on"
                            )
                if source == "formality" and not str(field.get("modelFormalityEn") or "").strip():
                    # The declaration loop above states this rule for a declaration field; this one
                    # states it for every other screen, so a formality is never a silent marker.
                    errors.append(f"{label} is declared a model formality without a reason to read")
    # A screen whose primary button is the owner's own irreversible step says so rather than inviting a
    # reader to press it: the sheet's action line is an instruction, so a screen that carries one and
    # forbids the press somewhere lower prints two answers to one question. The marker and its wording
    # are checked together, for the same reason every other marker in this loop is - and the label of
    # the button is read from `actionRu` by both renderers rather than restated in the sentence.
    for screen in console_screens():
        for node in [screen] + list(screen.get("subscreens") or []):
            if not node.get("ownerAction"):
                continue
            where = f"screens[{node.get('id')}].ownerAction"
            if not str(node.get("actionRu") or "").strip():
                errors.append(
                    f"{where} is set and the screen names no `actionRu`, so nothing says which button "
                    "must not be pressed"
                )
            for key in ("ownerActionRu", "ownerActionEn"):
                if not str(node.get(key) or "").strip():
                    errors.append(
                        f"{where} is set without `{key}`: the marker would print no reason, and a "
                        "marker with no statement beside it cannot be acted on"
                    )
    errors.extend(console_product_kind_model_errors())
    errors.extend(console_unreachable_model_errors())
    errors.extend(console_observed_label_errors())
    errors.extend(console_counter_row_errors())
    return errors


def console_counter_row_errors() -> list[str]:
    """The rows the model carries as the console's own counters, checked against what it prints.

    A mapping from an observation's row id to a counter is a permission: the report stops naming that
    row as a field to add. A permission nobody can review is how a row the model really is missing
    disappears, so the mapping is checked in the two ways it can be wrong - a counter id the model does
    not print, which makes the permission name nothing, and a mapping with no rule beside it, which is a
    silent exemption rather than a decision.
    """
    fold = console_form().get("observationFold") or {}
    rows = fold.get("counterRows") or {}
    if not rows:
        return []
    for key in ("counterRowsRuleRu", "counterRowsRuleEn"):
        if not str(fold.get(key) or "").strip():
            return [
                f"{CONSOLE_FORM_RELATIVE.as_posix()} observationFold.{key} is empty: a row the model "
                "refuses to add needs the rule that says which counter already carries it"
            ]
    printed = {
        str(counter.get("id"))
        for counter in ((console_form().get("declarationCompletion") or {}).get("counters") or [])
        if isinstance(counter, dict) and counter.get("id")
    }
    errors: list[str] = []
    for raw_id, counter in rows.items():
        row_id = str(raw_id)
        if not row_id.strip():
            # `console_counter_row_sources` drops a key with no id, so such an entry grants no
            # permission - and an entry that grants nothing is not an entry: it is a mapping nobody
            # wrote down, and it is reported rather than ignored.
            errors.append(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} observationFold.counterRows carries an empty row id, "
                "so nothing an observation names can match it and no counter is being pointed at"
            )
        elif str(counter) not in printed:
            errors.append(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} observationFold.counterRows maps {row_id!r} to the "
                f"counter {counter!r}, which `declarationCompletion.counters` does not print "
                f"({', '.join(sorted(printed)) or 'none'}): the row is then refused a place in the model "
                "on the strength of a counter that does not exist"
            )
    return errors


def console_unreachable_model_errors() -> list[str]:
    """The negative findings of the model, checked against what they claim.

    A finding is an item that is on no reachable page, and the model records it instead of a screen
    because nothing was seen. Two things then have to hold, and neither is checked by the markdown
    renderer alone. A finding nobody can check is the first: a refusal with no blocker, no searched
    page and no next action is indistinguishable from an item nobody looked for, so the three are
    required here as well as in the renderer.     The second is the contradiction: a finding whose id is
    also the name of a screen the model carries says the item cannot be read from any reachable page
    while the model documents its page - one of the two is wrong, and both are read by the same reader.
    A sub-screen counts as a screen here for the same reason: a dialog the model documents row by row
    and an item saying that dialog was never readable are one answer too many, and the id is what says so.
    """
    form = console_form()
    section = form.get("unreachable")
    if section is None:
        return []
    if not isinstance(section, dict) or not (section.get("items") or []):
        return [
            f"{CONSOLE_FORM_RELATIVE.as_posix()} unreachable carries no items: a negative-finding "
            "section with nothing in it is a heading that says nothing was searched"
        ]
    screen_names = {
        str(node.get("id"))
        for screen in console_screens()
        for node in [screen] + list(screen.get("subscreens") or [])
    }
    errors: list[str] = []
    for item in section["items"]:
        if not isinstance(item, dict):
            errors.append(f"{CONSOLE_FORM_RELATIVE.as_posix()} unreachable.items must hold objects")
            continue
        item_id = str(item.get("id") or "")
        label = f"unreachable.items[{item_id or '?'}]"
        if not item_id:
            errors.append(f"{label} carries no id, so no observation file can be matched against it")
        elif item_id in screen_names:
            errors.append(
                f"{label} names the item {item_id!r}, and the model also carries a screen with that id: "
                "an item that cannot be read from any reachable page and a documented screen for it are "
                "two answers to one question, so one of them is wrong"
            )
        for key in ("titleRu", "blockerRu", "searchedRu", "nextActionRu"):
            if not item.get(key):
                errors.append(
                    f"{label} carries no {key}: a refusal has to name the item, why it could not be "
                    "read, where the search happened and what to do instead, or nobody can check it"
                )
    return errors


def console_product_kind_model_errors() -> list[str]:
    """The product-branch statement, checked against the branches the code can actually read.

    Two failures are worth catching before a slot ever generates a sheet. One is a marker with no
    wording: a screen or field that declares itself branch-bound while the model carries no sentence
    to print renders a silent empty cell, which is the same as printing the other branch's value. The
    other is a key list that has drifted from the reader: `store-kit.py` reads the three named
    arguments of `BillingCatalog(...)`, so a branch the model knows and the code does not (or the
    reverse) is a branch that can never be resolved - and a slot declaring it would be told its kind
    was unreadable rather than what it is.
    """
    form = console_form()
    markers = [
        f"screens[{screen.get('id')}].productKindGate"
        for screen in console_screens()
        if screen.get("productKindGate")
    ] + [
        f"{field.get('id')}.productKindBound"
        for field in _all_console_fields()
        if field.get("productKindBound")
    ]
    rule = ((form.get("billing") or {}).get("productForm") or {}).get("productKind")
    if not isinstance(rule, dict) or not rule:
        if markers:
            return [
                f"{CONSOLE_FORM_RELATIVE.as_posix()} marks {', '.join(markers)} as bound to a product "
                "branch, and billing.productForm.productKind is missing: the row has no branch statement "
                "to print, so it would print a value read on another page"
            ]
        return []
    errors: list[str] = []
    for key in ("observedShortRu", "observedShortEn", "noteRu", "unobservedKindRu", "unobservedKindEn",
                "unknownKindRu", "unknownKindEn", "unclassifiedArgsRu", "unclassifiedArgsEn",
                "rowRefusalEn", "pathRefusalEn"):
        if not str(rule.get(key) or "").strip():
            errors.append(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} billing.productForm.productKind.{key} is empty: a "
                "branch statement with a hole in it prints a row nobody can act on"
            )
    for key, placeholder in (
        ("rowRefusalEn", "{field}"),
        ("unobservedKindRu", "{kind}"),
        ("unobservedKindEn", "{kind}"),
        ("unclassifiedArgsRu", "{kind}"),
        ("unclassifiedArgsEn", "{kind}"),
    ):
        if str(rule.get(key) or "").strip() and placeholder not in str(rule[key]):
            errors.append(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} billing.productForm.productKind.{key} carries no "
                f"`{placeholder}` placeholder, so it cannot name what it is about"
            )
    observed = {str(arg) for arg in rule.get("observedArgs") or []}
    unobserved = {str(arg) for arg in (rule.get("unobservedArgs") or {})}
    if not observed:
        errors.append(f"{CONSOLE_FORM_RELATIVE.as_posix()} billing.productForm.productKind.observedArgs is empty")
    if observed & unobserved:
        errors.append(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} billing.productForm.productKind lists "
            f"{', '.join(sorted(observed & unobserved))} as both observed and unobserved"
        )
    declared_args = observed | unobserved
    implemented = catalog_constructor_args()
    if declared_args - implemented:
        errors.append(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} billing.productForm.productKind names "
            f"{', '.join(sorted(declared_args - implemented))}, which `BillingCatalog` does not declare "
            f"({CAPABILITY_CATALOG_RELATIVE.as_posix()}): a branch no slot can be on is a branch no slot "
            "is ever told about"
        )
    if implemented - declared_args:
        errors.append(
            f"`BillingCatalog` declares {', '.join(sorted(implemented - declared_args))}, and "
            f"{CONSOLE_FORM_RELATIVE.as_posix()} billing.productForm.productKind does not say whether a slot "
            "filling that list shares the observed console page - a slot on it would be told the observed "
            "form is its own"
        )
    for screen in console_screens():
        path = str(screen.get("productKindNoteFrom") or "")
        if path and not str(console_model_value(path) or "").strip():
            errors.append(
                f"screens[{screen.get('id')}].productKindNoteFrom names {path!r}, which resolves to nothing "
                "in the model, so the screen states no branch above its table"
            )
    return errors


def console_observed_label_errors() -> list[str]:
    """The extra console labels a screen records, checked against the rule that lets them match.

    An observation file names a screen as the browser printed it, which is not always the model's id
    or even its page label: the console appends the record a dialog was opened from. A screen may
    therefore record the other labels it was read under (`observedLabelsRu`) or, where the console
    appends the observer's own record name, a label prefix (`observedLabelPrefixesRu`). Both are
    checked here because the permission would otherwise be a place where a screen quietly claims
    another page - an empty label matches every observation that reaches the screen, and a rule
    nobody wrote is a match nobody can review.
    """
    fold = console_form().get("observationFold") or {}
    errors: list[str] = []
    for key in ("labelAliasRuleRu", "labelAliasRuleEn"):
        if not str(fold.get(key) or "").strip():
            errors.append(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} observationFold.{key} is empty: a screen that "
                "records an extra console label needs the rule that says why such a label may match"
            )
    for screen in console_screens():
        for node in [screen] + list(screen.get("subscreens") or []):
            for key in ("observedLabelsRu", "observedLabelPrefixesRu"):
                values = node.get(key)
                if values is None:
                    continue
                where = f"screens[{node.get('id')}].{key}"
                if not isinstance(values, list) or not values:
                    errors.append(
                        f"{CONSOLE_FORM_RELATIVE.as_posix()} {where} is empty: a screen records the "
                        "labels it was read under, or it records none"
                    )
                    continue
                for index, value in enumerate(values):
                    if not str(value or "").strip():
                        errors.append(
                            f"{CONSOLE_FORM_RELATIVE.as_posix()} {where}[{index}] is empty, and an "
                            "empty label matches every observation that reaches this screen"
                        )
    return errors


def _all_console_fields() -> list[dict[str, Any]]:
    """Every field of every screen and sub-screen, so a model-wide rule can be stated once."""
    fields: list[dict[str, Any]] = []
    for screen in console_screens():
        fields.extend(console_screen_fields(screen))
        for sub in screen.get("subscreens") or []:
            fields.extend(console_screen_fields(sub))
    return fields



def console_screen(screen_id: str) -> dict[str, Any]:
    """One model screen by id, top-level or nested - so a rule can name the screen it reads."""
    pending = list(console_screens())
    while pending:
        screen = pending.pop(0)
        if str(screen.get("id")) == screen_id:
            return screen
        pending.extend(screen.get("subscreens") or [])
    raise StoreKitError(
        f"{CONSOLE_FORM_RELATIVE.as_posix()} has no screen {screen_id!r}; a rule names it by id"
    )


def console_field(screen: dict[str, Any], field_id: str) -> dict[str, Any]:
    """One field of a screen by id, so a rule reads the field the console shows rather than a copy."""
    for field in console_screen_fields(screen):
        if str(field.get("id")) == field_id:
            return field
    raise StoreKitError(
        f"{CONSOLE_FORM_RELATIVE.as_posix()} screen {screen.get('id')!r} has no field {field_id!r}"
    )


def console_screen_fields(screen: dict[str, Any]) -> list[dict[str, Any]]:
    """A screen's fields: inline, or the array its `fieldsFrom` path names.

    The path is resolved against the model so a screen a human adds carries its own fields, while a
    screen whose fields are also read by a rule (the wizard's caps, the dialog's caps) names that one
    array instead of copying it. Copying is the defect this indirection exists to prevent: the sheet
    would then print a cap the rule does not enforce.
    """
    fields = screen.get("fields")
    if isinstance(fields, list):
        return fields
    path = screen.get("fieldsFrom")
    if path:
        node: Any = console_form()
        for key in str(path).split("."):
            node = node.get(key) if isinstance(node, dict) else None
        if not isinstance(node, list):
            raise StoreKitError(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} screen {screen.get('id')!r} names fieldsFrom "
                f"{path!r}, which is not an array of fields"
            )
        return node
    return []


def console_review_instructions(
    listing: dict[str, Any], creds: dict[str, Any], credentials: dict[str, Any]
) -> str:
    """The text for the dialog's instructions field: English, and measured against its cap.

    Which template is used is decided by the rule that produced the declaration's answer, not by the
    answer alone: `yes` behind a purchase and `yes` behind a sign-in need opposite instructions, and
    one template for both told a sign-in-only slot's reviewer about a paid unlock that slot does not
    sell. The rule names its template (`templateKey`), so the same table that decides the answer
    decides the words that come with it, and a rule without a template is a failure rather than a
    fallback to somebody else's text.
    """
    templates = creds.get("instructionsTemplates") or {}
    key = str(credentials.get("templateKey") or "")
    template = (templates.get(key) or {}).get("templateEn")
    if not template:
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} credentials.rules names templateKey {key!r}, and "
            f"credentials.instructionsTemplates has no template for it "
            f"({', '.join(sorted(templates)) or 'none'})"
        )
    return str(template).format(
        appName=listing["appName"], promoCode=str(creds["promoCodePlaceholder"])
    )


# The pack-localized listing texts, so the sheet prints one row per locale instead of one per field.
# Keyed by the model's `slotKey`, not by `literal`: these fields describe a pack path whose value is
# per locale, and the model declares that path once rather than a reader id per language.
LISTING_TEXT_LITERALS = {
    "listing.locales.title": "title",
    "listing.locales.shortDescription": "shortDescription",
    "listing.locales.fullDescription": "fullDescription",
}


def console_model_value(path: str) -> Any:
    """Resolve a dotted path against the model, for the `bodyFrom` lines the sheet prints verbatim."""
    node: Any = console_form()
    for key in str(path).split("."):
        node = node.get(key) if isinstance(node, dict) else None
    return node


CONSOLE_LABEL_LANGS = {"ru": ("nameFrom", "nameRu"), "en": ("nameEnFrom", "nameEn")}


def console_field_name(field: dict[str, Any], *, lang: str = "ru") -> str:
    """The console's own label for one field, read from the model path it names when it names one.

    A field carries its label (`nameRu`/`nameEn`) or points at the model path that carries it
    (`nameFrom`/`nameEnFrom`). The second form exists because a label the model already holds must be
    read rather than restated: the optional minor-restriction box's sentence lives on the audience
    branch that answers the box, and the field's own copy of it had already drifted by wrapping it in
    «» - which then made a *truthful* transcription of the console fail the handoff's label
    comparison, the handoff being an equality test between what the console printed and what the model
    carries. Carrying both a path and the value is a second copy, and a path that resolves to nothing
    is a field with no label: both are failures, and neither is silent.
    """
    path_key, flat_key = CONSOLE_LABEL_LANGS.get(lang, CONSOLE_LABEL_LANGS["ru"])
    path = field.get(path_key)
    if path:
        if field.get(flat_key):
            raise StoreKitError(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} field {field.get('id')!r} carries both "
                f"{path_key} and {flat_key}; the label is read from one of them or it is a second copy"
            )
        return str(console_model_value(str(path)) or "")
    return str(field.get(flat_key) or "")


def console_paste_block(
    values: list[str], todo: str, why: str
) -> tuple[str, str, str | None, list[str]]:
    """A field whose value is pasted from a block: (cell, why, block, notes).

    The unit of correctness here is the exact string the console receives - a product ID, a tag, a
    price. A block keeps the string byte-identical to what the cell describes, so a copy cannot pick
    up a separator a table cell added.
    """
    if not values:
        return (todo, why, None, [])
    return ("paste the block below", why, "\n".join(values), [])


def console_field_cell(
    field: dict[str, Any], screen: dict[str, Any], ctx: dict[str, Any]
) -> tuple[str, str, str | None, list[str]]:
    """One console field filled from the pack: (what to type, why, paste block, notes).

    Dispatch is on the `literal` the model names, never on the field's position, so a new screen
    picks an existing reader or carries a plain value. `declaration` is the one reader that needs the
    screen itself, because the literal there is derived from the slot's capabilities by
    `console_declaration_answer`.
    """
    why = str(field.get("whyEn") or field.get("whyRu") or "")
    # A rule that two fields share (the tag alphabet, the icon's format) lives once in this model and
    # is named here, so editing the canon cannot leave a second copy behind in a field row.
    if field.get("whyFrom"):
        why = str(console_model_value(str(field["whyFrom"])) or why)
    slot_key = str(field.get("slotKey") or "")
    if slot_key and not slot_key.startswith("constant."):
        why = f"{why} (from `{slot_key}`)"
    # A field's own rule belongs beside the value it constrains: the wizard's two ID fields accept
    # different alphabets, and reading the rule anywhere but in the row is how a paste gets refused.
    rule = str(field.get("ruleEn") or "")
    if rule:
        why = f"{why} {rule}".strip()
    notes: list[str] = []
    block: str | None = None
    literal = field.get("literal")
    if field.get("ownerDecision"):
        # The value is the owner's own decision and no field of the slot's pack carries it, so the
        # cell is work rather than a value: a plausible-looking number here is the worst kind of
        # wrong answer, because the count of a promotion cannot be changed once the promotion is
        # saved. The reason is the model's, and it must name what the human is deciding.
        reason = str(field.get("ownerDecisionEn") or "").strip()
        if not reason:
            raise StoreKitError(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} field {field.get('id')!r} is declared the owner's "
                "own decision and carries no `ownerDecisionEn`, so there is nothing to print in place "
                "of the value it does not have"
            )
        label = console_field_name(field) or str(field.get("id") or "")
        return f"TODO-HUMAN: «{label}» — {reason}", why, block, notes
    if not literal:
        value = str(field.get("valueEn") or field.get("valueRu") or "")
        # A radio or a checkbox is chosen by the word the console shows, and this console is
        # Russian: an English gloss in the cell is a value the human cannot find on the screen.
        if field.get("optionsRu"):
            value = str(field.get("valueRu") or value)
        when = field.get("valueWhen") or {}
        if when and str(when.get("capability") or "") in ctx["enabled"]:
            # The override is the same kind of value as the one it replaces: a field the console
            # shows as a radio keeps the console's own wording (`Да`, not the English gloss `Yes`)
            # when its answer changes with a capability.
            value = str(
                (when.get("valueRu") if field.get("optionsRu") else when.get("valueEn"))
                or when.get("valueEn")
                or when.get("valueRu")
                or value
            )
        return value, why, block, notes

    literals = ctx["literals"]
    if literal == "derived.listingCategory":
        # The console prints its own word for the pack's token, and the dictionary holds only the
        # pairs read in the console. A token it does not hold is a refusal naming the token: a raw
        # `EDUCATION` in the cell is a word no human can find on the screen, and the model must not
        # guess the console's vocabulary.
        token = str(ctx["listing"].get("category") or "").strip()
        table = console_form().get("listing") or {}
        if not token:
            return (
                "TODO-HUMAN (see the note below)",
                why,
                block,
                notes
                + [
                    "TODO-HUMAN: this slot's `store/listing.json` carries no `category`, so the console "
                    "would be asked to choose one with nothing to paste. Add the token Play's category "
                    "list gives it (`listing.category`), after reading its label in the console."
                ],
            )
        pairs = table.get("categoryLabels") or {}
        known = pairs.get(str(token).upper()) or pairs.get(token)
        if not isinstance(known, dict):
            template = str(table.get("categoryRefusalEn") or "")
            if not template:
                raise StoreKitError(
                    f"{CONSOLE_FORM_RELATIVE.as_posix()} listing.categoryRefusalEn is missing, so a "
                    "token the dictionary does not hold cannot be refused by name"
                )
            return (
                "TODO-HUMAN (see the note below)",
                why,
                block,
                notes + [
                    "TODO-HUMAN: "
                    + template.format(
                        token=str(token), known=", ".join(sorted(str(key) for key in pairs)) or "none"
                    )
                ],
            )
        return str(known.get("labelRu") or known.get("labelEn") or ""), why, block, notes
    if literal == "derived.storeAsset":
        # A graphics row of the store page is answered by the pack's own files, named by the inventory
        # `expected_composed_assets` already enforces, so a count cannot drift from the renderer. The
        # row says which kind it takes (`assetKind`); a kind the pack does not ship is stated as such
        # rather than printed as an empty cell.
        kind = str(field.get("assetKind") or "")
        if not kind:
            raise StoreKitError(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} field {field.get('id')!r} uses the "
                "`derived.storeAsset` reader without an `assetKind`, so nothing says which part of the "
                "pack inventory answers it"
            )
        inventory = expected_composed_assets(list(ctx.get("locales") or []))
        if kind == "icon":
            wanted = [path for path in inventory if path.endswith("icon-512.png")]
        elif kind == "feature":
            wanted = [path for path in inventory if path.endswith("feature-graphic-1024x500.png")]
        elif kind in SCREEN_TARGETS:
            wanted = [path for path in inventory if f"/{kind}-" in path]
        else:
            raise StoreKitError(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} field {field.get('id')!r} names assetKind "
                f"{kind!r}; the pack inventories {', '.join(sorted(['icon', 'feature'] + list(SCREEN_TARGETS)))}"
            )
        if not wanted:
            return (
                "TODO-HUMAN (see the note below)",
                why,
                block,
                notes + [f"TODO-HUMAN: the pack ships no {kind} asset, so this row has nothing to upload."],
            )
        return ", ".join(f"`{path}`" for path in wanted), why, block, notes
    if literal == "declaration":
        answer, report = console_declaration_answer(
            screen, ctx["listing"], ctx["compliance"], ctx["credentials"]
        )
        notes.extend(report)
        if answer is None:
            return "TODO-HUMAN (see the note below)", why, block, notes
        # A radio declaration answers with a token of the model's own vocabulary, and the cell has to
        # be the label the console prints beside it. The token is resolved against the field showing
        # it, so a Russian radio never gets an English gloss and a token never reaches the human.
        if str(answer) in (console_form().get("choices") or {}):
            label, label_notes = console_choice_label(field, str(answer))
            if label is None:
                return "TODO-HUMAN (see the note below)", why, block, notes + label_notes
            return label, why, block, notes + label_notes
        return str(answer), why, block, notes
    if literal == "contentRating.answer":
        question_id = str(field.get("questionId") or "")
        if not question_id:
            raise StoreKitError(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} field {field.get('id')!r} uses the "
                "`contentRating.answer` reader without a `questionId`, so nothing says which question "
                "of step 2 it answers"
            )
        token, branch, notes = content_rating_branch(ctx["listing"])
        if token is None:
            return "TODO-HUMAN (see the note below)", why, block, notes
        plan = content_rating_plan(branch, token, ctx["enabled"])
        if question_id not in plan:
            return (
                "TODO-HUMAN (see the note below)",
                why,
                block,
                notes
                + [
                    f"TODO-HUMAN: the questionnaire branch {token!r} answers {len(plan)} questions and "
                    f"not {question_id!r}. Add the answer to `contentRating.questionnaires.{token}"
                    ".answers` in the model once it has been read in the console."
                ],
            )
        answer, answer_why = plan[question_id]
        label, label_notes = console_choice_label(field, answer)
        if label is None:
            return "TODO-HUMAN (see the note below)", why, block, notes + label_notes
        # The why of a row is the reason for *this answer*, and it belongs to the branch: the branch's
        # own `whyEn` covers it, and an answer that needs its own reason carries one next to itself.
        return label, answer_why, block, notes + label_notes
    if literal == "contentRating.category":
        # The category radio and the ten answers of step 2 are one decision, so the pack names it
        # once - the questionnaire branch - and the label comes from the field that shows the radio.
        token, branch, cat_notes = content_rating_branch(ctx["listing"])
        if token is None:
            return "TODO-HUMAN (see the note below)", why, block, cat_notes
        label, _ = content_rating_category(branch)
        return (
            label,
            why,
            block,
            cat_notes
            + [
                f"`listing.contentRating.questionnaire` = {token!r} → the branch's own `category`, and "
                f"the console's label «{label}» is read off this field. Nothing here is a value written "
                "for another app."
            ],
        )
    if literal == "listing.contactEmail":
        email = str(ctx["listing"].get("contactEmail") or "").strip()
        if not email or is_placeholder_email(email):
            return (
                "TODO-HUMAN: `listing.contactEmail` is a reserved documentation host; set it to a "
                "monitored inbox",
                why,
                block,
                notes,
            )
        return email, why, block, notes
    if literal == "listing.privacyPolicyUrl":
        url = str(ctx["listing"].get("privacyPolicyUrl") or "").strip()
        if not url or is_placeholder_url(url):
            return (
                "TODO-HUMAN: `listing.privacyPolicyUrl` is missing or on a reserved documentation "
                "host; host `store/privacy.md` over HTTPS and set it",
                why,
                block,
                notes,
            )
        return url, why, block, notes
    if literal == "listing.contentRating.targetAudience":
        token, branch, age_notes = target_audience_branch(ctx["listing"])
        if token is None:
            return "TODO-HUMAN (see the note below)", why, block, age_notes
        labels = target_audience_labels(branch)
        notes = age_notes + [
            f"`listing.contentRating.targetAudience` = {token!r}. The console's own label is "
            + ", ".join(f"«{label}»" for label in labels)
            + "; the value in `store/listing.json` is a token of `targetAudience.audiences`, not the "
            "label, so nothing here has to agree with the console by hand."
        ]
        skipped = target_audience_skipped_steps(branch)
        if skipped:
            notes.append(target_audience_skipped_note(branch))
        return "tick " + ", ".join(f"«{label}»" for label in labels), why, block, notes
    if literal == "derived.minorGate":
        gate = (console_form().get("targetAudience") or {}).get("minorGate") or {}
        label = str(gate.get("labelRu") or "")
        token, branch, gate_notes = target_audience_branch(ctx["listing"])
        if token is None:
            return "TODO-HUMAN (see the note below)", why, block, gate_notes
        if str(branch.get("minorGate") or "") != "tick":
            return (
                f"leave «{label}» empty",
                why,
                block,
                gate_notes + [f"Branch {token!r} does not answer this box, so it stays empty."],
            )
        return (
            f"tick «{label}»",
            why,
            block,
            gate_notes
            + [
                f"`{token}` ticks it. The console's own wording: «{label}».",
                "It closes both the download and the in-app purchase for accounts marked as children, "
                "and the console adds that Play may not apply the restriction to users who never "
                "declared an age under 18 — an availability limit, not a guarantee.",
            ],
        )
    if literal == "derived.reviewCredentials":
        # The console shows the two sign-in fields on every first upload, so the sheet lists them even
        # where nothing is behind a sign-in. What goes in them is decided by whether the app gates
        # anything at all, and a demo account is a credential rather than app data: nothing in the pack
        # can carry one, so a slot that needs it gets work named instead of an invented login.
        empty_cell = str(field.get("valueEn") or field.get("valueRu") or "")
        if "auth" in ctx["enabled"]:
            return (
                "TODO-HUMAN: the reviewer's demo sign-in is not in the pack",
                why,
                block,
                notes
                + [
                    f"TODO-HUMAN: «{console_field_name(field)}» needs the demo account a reviewer will sign "
                    "in with. Create it in the app, paste its value here, and record the same access in "
                    "`store/review-notes.md`; the pack holds app data, and a credential is not app data."
                ],
            )
        return empty_cell, why, block, notes
    if literal == "derived.advertisingId":
        answer, report = console_declaration_answer(
            console_screen("declaration-advertising-id"),
            ctx["listing"],
            ctx["compliance"],
            ctx["credentials"],
        )
        notes.extend(report)
        if answer is None:
            return "TODO-HUMAN (see the note below)", why, block, notes
        label, label_notes = console_choice_label(field, str(answer))
        if label is None:
            return "TODO-HUMAN (see the note below)", why, block, notes + label_notes
        return label, why, block, notes + label_notes
    if literal == "derived.dataSafetyCollects":
        # The form's first question decides whether the other four are asked at all, so it is answered
        # from the pack's own `collectsData` and the reason beside it. "Collect" means transmitting
        # data off the device: a bundled SDK that sends it is declared even when the app's own code
        # has no network, which is why a slot that sells something answers «Да» here.
        safety = data_safety_answers(ctx["listing"])
        collects = bool(safety.get("collectsData"))
        notes.append(f"`listing.dataSafety.collectsData` = {str(collects).lower()} — {safety.get('reason')}")
        sdks = [str(item.get("name")) for item in safety.get("thirdPartySdks") or [] if isinstance(item, dict)]
        if collects and sdks:
            notes.append(
                "The transmission that makes this «Да» comes from the bundled "
                + ", ".join(sdks)
                + ", not from the app's own code; `listing.dataSafety.dataTypes` is the row it needs."
            )
        return data_safety_label(field, "yes" if collects else "no", notes, why, block)
    if literal == "derived.dataSafetyEncrypted":
        # Asked only of an app that collects. The pack ties the two together by construction - it sets
        # `encryptedInTransit` from whether any data type exists - so a slot that collects nothing has
        # no answer to give and none is asked, and the cell says that rather than printing «Нет» for a
        # transmission that does not exist.
        safety = data_safety_answers(ctx["listing"])
        if not safety.get("collectsData"):
            return (
                "not asked — `listing.dataSafety.collectsData` is false",
                why,
                block,
                notes
                + [
                    "This row belongs to the branch that collects: the observed wizard asked its two "
                    "opening questions of an app that transmits data, and `listing.dataSafety` sets "
                    "`encryptedInTransit` only when a data type exists. `store/data-safety.md` prints "
                    "the same thing in prose — «Not applicable - no data is collected».",
                ],
            )
        token = "yes" if safety.get("encryptedInTransit") else "no"
        notes.append(
            f"`listing.dataSafety.encryptedInTransit` = {str(bool(safety.get('encryptedInTransit'))).lower()}"
            " — the same answer `store/data-safety.md` writes out as «Is collected data encrypted in "
            "transit?»"
        )
        return data_safety_label(field, token, notes, why, block)
    if literal == "derived.dataSafetyAccounts":
        # The pack carries no "has accounts" field, and the console reads the answer off the deletion
        # branch instead. One direction is already written into the pack - its own document prints
        # «Does the app support accounts? No» from `hasAccountDeletion` = false - and the other is a
        # branch of the wizard nobody has opened, which is work for a human rather than a second label.
        safety = data_safety_answers(ctx["listing"])
        if safety.get("hasAccountDeletion"):
            return (
                "TODO-HUMAN (see the note below)",
                why,
                block,
                notes
                + [
                    f"TODO-HUMAN: «{console_field_name(field)}» — the pack declares a deletion mechanism "
                    "(`listing.dataSafety.hasAccountDeletion` = true), which is the branch where the app "
                    "has accounts. That branch of this wizard was never read: its questions and its "
                    "labels are not in the model, so read them in the console and record the "
                    "observation rather than answering from the branch that was read.",
                ],
            )
        notes.append(
            "`listing.dataSafety.hasAccountDeletion` = false, and the pack's own `store/data-safety.md` "
            "prints «Does the app support accounts? No» from the same field: the sheet and the document "
            "cannot say different things about accounts."
        )
        return data_safety_label(field, "no-accounts", notes, why, block)
    if literal == "derived.dataSafetyDeletion":
        safety = data_safety_answers(ctx["listing"])
        if safety.get("hasAccountDeletion"):
            return (
                "TODO-HUMAN (see the note below)",
                why,
                block,
                notes
                + [
                    f"TODO-HUMAN: «{console_field_name(field)}» — the pack declares that a deletion "
                    "mechanism exists, and the «Да» direction of this radio was never read in the "
                    "console: it asks for the deletion route itself (a URL, or steps inside the app), "
                    "which the pack does not carry. Read the branch and record it.",
                ],
            )
        notes.append(
            "`listing.dataSafety.hasAccountDeletion` = false — the same pack field as the account row, "
            "for the same reason: a deletion mechanism is needed only where accounts exist and the "
            "developer stores what they hold."
        )
        return data_safety_label(field, "no", notes, why, block)
    if literal == "derived.dataSafetyCategories":
        # One console row per collected type. The row is the bundled SDK rather than the app's code -
        # with `billing` enabled the Play Billing library creates it, and under-declaring it is a
        # policy violation - so the cell reads the pack's own list and never invents an entry.
        table = data_safety_table()
        safety = data_safety_answers(ctx["listing"])
        types = [item for item in (safety.get("dataTypes") or []) if isinstance(item, dict)]
        if not types:
            return (
                "not asked — `listing.dataSafety.dataTypes` is empty",
                why,
                block,
                notes
                + [
                    "The category step belongs to the branch that collects: a pack whose "
                    "`listing.dataSafety` declares no data type has nothing to declare here, and the "
                    "observed wizard answered its opening questions for an app that does.",
                ],
            )
        survey = table["surveyAnswersRu"]
        cells: list[str] = []
        for item in types:
            token = str(item.get("type") or "")
            purpose_token = str(item.get("purpose") or "")
            category, category_notes = data_safety_vocabulary("dataTypeVocabulary", token, "categoryRu")
            subcategory, subcategory_notes = data_safety_vocabulary(
                "dataTypeVocabulary", token, "subcategoryRu"
            )
            purpose, purpose_notes = data_safety_vocabulary("purposeVocabulary", purpose_token, "labelRu")
            if category is None or subcategory is None or purpose is None:
                # Which vocabulary failed is the whole content of the message: an unknown *purpose*
                # reported against the type's token sends the reader to a token the model already
                # names, and the string they cannot find on screen is the other one.
                missing = []
                if category is None or subcategory is None:
                    missing.append(("data type", token))
                if purpose is None:
                    missing.append(("purpose", purpose_token))
                notes.extend(category_notes + subcategory_notes + purpose_notes)
                cells.append(
                    "; ".join(
                        f"TODO-HUMAN: {kind} {value!r} is not in the model's vocabulary"
                        for kind, value in missing
                    )
                )
                continue
            row = f"`{category}` → `{subcategory}`"
            # `shared` and `optional` are the two directions this session never saw, and either one
            # changes which wizard screen follows: a shared type is asked a second purpose, an
            # optional one is not required. Both are refused by name instead of being printed as the
            # branch that was read.
            if item.get("shared"):
                notes.append(
                    f"TODO-HUMAN: {row} is declared `shared` = true, and the wizard's sharing screen was "
                    "never read — it asks the purpose of the sharing separately, so its answers are not "
                    "in the model. Read it in the console and record it."
                )
                cells.append(f"TODO-HUMAN: {row} is declared shared")
                continue
            if item.get("optional"):
                notes.append(
                    f"TODO-HUMAN: {row} is declared `optional` = true, and the wizard's optional-data "
                    "screen was never read. Read it in the console and record it."
                )
                cells.append(f"TODO-HUMAN: {row} is declared optional")
                continue
            cells.append(
                table["categoryCellEn"].format(
                    category=category,
                    subcategory=subcategory,
                    collected=survey["collected"],
                    required=survey["required"],
                    purpose=purpose,
                )
            )
            detail = f"{row}: {item.get('notes')}" if item.get("notes") else row
            notes.append(detail)
        return "; ".join(cells), why, block, notes
    if literal == "derived.dataSafetyTemporary":
        # The one row with no pack answer at all. «Processed ephemerally» is a claim about how Google
        # Play handles a purchase inside its own systems; the pack describes what is collected and no
        # retention rule, so a plausible default here would be an assertion nobody checked. The cell
        # names the console's own row and the model's sentence explains why it is not answered.
        table = data_safety_table()
        sentence = str(table["temporaryRefusalEn"]).format(field=console_field_name(field))
        notes.append(sentence)
        notes.append(
            "This is an open question for the owner rather than a missing reading: the row was observed, "
            "and it was recorded with no slot key because no pack field could answer it."
        )
        return f"TODO-HUMAN: {sentence}", why, block, notes
    if literal == "derived.requiredActions":
        # "Ten declarations still to fill" is the count of *this* model's declarations, not a fact
        # about the app: a slot whose declarations are done shows none, and a model that grows an
        # eleventh declaration would otherwise keep printing ten at the human.
        count = len(console_declarations())
        template = str((console_form().get("declarationsSummary") or {}).get("cellEn") or "")
        if not template:
            raise StoreKitError(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} declarationsSummary.cellEn is the wording the "
                "count is printed with; a count with no wording is a row with no value"
            )
        return template.format(count=count), why, block, notes
    if literal == "derived.promoEndDate":
        # The one-year ceiling is documented once, in `billing.promotions.maxDays`, and the field
        # prints it: the same number written beside the field as well is a second copy that the first
        # edit to the rule leaves behind.
        rule = console_form().get("billing", {}).get("promotions", {})
        max_days = rule.get("maxDays")
        template = str(rule.get("endDateCellEn") or "")
        if not isinstance(max_days, int) or not template:
            raise StoreKitError(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} billing.promotions must carry maxDays and "
                "endDateCellEn; the end date is derived from them"
            )
        return template.format(maxDays=max_days), why, block, notes
    if literal == "productIds":
        return console_paste_block(literals["productIds"], literals["priceTodo"], why)
    if literal == "purchaseOptionId":
        block_tuple = console_paste_block(literals["purchaseOptionIds"], literals["priceTodo"], why)
        return block_tuple[0], block_tuple[1], block_tuple[2], list(literals["purchaseOptionNotes"])
    if literal == "productName":
        return (literals["name"] or literals["priceTodo"], why, block, notes)
    if literal == "productDescription":
        return (literals["description"] or literals["priceTodo"], why, block, notes)
    if literal == "productIcon":
        return str(console_form()["billing"]["icon"]["slotKey"]), why, block, notes
    if literal == "tags":
        if literals["tags"]:
            cell, why, block, _ = console_paste_block(literals["tags"], literals["priceTodo"], why)
            return cell, why, block, list(literals["tagNotes"])
        return f"TODO-HUMAN: {console_form()['billing']['tags']['blankFillEn']}", why, block, notes
    if literal == "price":
        if literals["price"]:
            return console_paste_block([literals["price"]], literals["priceTodo"], why)
        return (literals["priceTodo"], why, block, notes)
    if literal == "releaseName":
        return str(ctx["release_name"]), why, block, notes
    if literal == "releaseNotes":
        block = release_notes_block(ctx["store"] / "release-notes.md")
        return "paste the block below", why, block, notes
    if literal == "instructions":
        cap = int(ctx["creds"]["instructionsCap"])
        instructions = str(ctx["instructions"])
        why = (
            f"The console requires them in **English** and caps the field at {cap} characters; "
            f"measured: **{len(instructions)}/{cap}**"
        )
        if str(ctx["credentials"].get("answer")) == "yes":
            # The owner's paragraph travels with the template rather than with the answer: a `yes`
            # earned by a sign-in needs the demo-account note, and the promo-code note would be false
            # in that slot.
            template = (ctx["creds"].get("instructionsTemplates") or {}).get(
                str(ctx["credentials"].get("templateKey") or "")
            ) or {}
            note_from = str(template.get("ownerNoteFrom") or "")
            if note_from:
                notes.append(
                    str(console_model_value(note_from) or "").format(
                        promoCodePlaceholder=ctx["creds"]["promoCodePlaceholder"]
                    )
                )
        block = instructions
        return "paste the block below", why, block, notes
    raise StoreKitError(
        f"{CONSOLE_FORM_RELATIVE.as_posix()} screen {screen.get('id')!r} declares literal {literal!r} "
        "for field "
        f"{field.get('id')!r}, and store-kit.py has no reader for it; add one before generating the sheet"
    )


def console_localized_rows(
    field: dict[str, Any], ctx: dict[str, Any], blocks: list[tuple[str, str]]
) -> list[tuple[str, str, str, str]]:
    """One row per locale for a pack-localized listing field, with the measured length in the why."""
    key = LISTING_TEXT_LITERALS[str(field["slotKey"])]
    caps = console_form()["listing"]
    label = f"{console_field_name(field)} ({console_field_name(field, lang='en')})"
    rows: list[tuple[str, str, str, str]] = []
    for locale in ctx["locales"]:
        entry = (ctx["listing"].get("locales") or {}).get(locale) or {}
        text = str(entry.get(key) or "")
        measured = f"{len(text)}/{caps[key]} characters" if caps.get(key) else f"{len(text)} characters"
        why = f"{field.get('whyEn')} {measured}".strip()
        if field.get("block"):
            blocks.append((f"{label} — {locale}", text))
            rows.append((f"{label} — {locale}", str(caps.get(key) or "—"), "paste the block below", why))
        else:
            rows.append((f"{label} — {locale}", str(caps.get(key) or "—"), text, why))
    return rows


def console_cell(value: Any) -> str:
    """One table cell of the generated sheet: a value a table can hold.

    A reader that returns a line break or a pipe would otherwise end the row it was printed in - the
    whole table after it becomes prose, and the fault is invisible in the source that produced it. The
    Data safety answer is the one declaration that is naturally many lines, and it is emitted as a
    one-line cell with the detail in the notes below the table for exactly this reason.
    """
    text = str(value if value is not None else "")
    text = re.sub(r"\s*\n+\s*", "; ", text)
    return text.replace("|", "\\|").strip()


def console_screen_sheet(screen: dict[str, Any], ctx: dict[str, Any], level: int = 3) -> list[str]:
    """One console screen as the sheet shows it: heading, path, a per-field table, the paste blocks.

    Everything below the heading is derived from the screen's own data - there is no branch per
    screen here, so a new screen is added by editing the model.
    """
    out: list[str] = []
    order = screen.get("order")
    prefix = f"{order}. " if isinstance(order, int) else ""
    # A top-level screen is named as the console names it (that is what the human has to find);
    # a sub-screen is one step of it and is named by its own title.
    heading_ru = screen.get("consoleNameRu") or screen.get("titleRu") or screen.get("id")
    heading_en = screen.get("consoleNameEn") or screen.get("titleEn") or ""
    out.append(f"{'#' * level} {prefix}{heading_ru}" + (f" — {heading_en}" if heading_en else ""))
    out.append("")
    if screen.get("capability"):
        out.append(f"Only for slots whose `{screen['capability']}` capability is enabled.")
        out.append("")
    if screen.get("pagePathRu"):
        # The path is part of the observed branch: a slot on another product branch (a subscription,
        # whose products live on their own console page) is told so rather than sent to a page its
        # products are not on.
        page = f"`{screen['pagePathRu']}`"
        if screen.get("pagePathObserved"):
            page += f" (`{screen['pagePathObserved']}`)"
        out.append(f"Console path: {console_product_path_cell(screen, ctx, page)}")
        out.append("")
    if isinstance(screen.get("stripRu"), list) and screen["stripRu"]:
        out.append(f"Screen steps: {', '.join(f'`{step}`' for step in screen['stripRu'])}.")
        out.append("")
    # The first column of this table is headed "the console field, as shown", so a screen whose labels
    # are a gloss rather than the console's own row says so above the table it appears in.
    if screen.get("nameNotQuoted"):
        out.append(f"**The label column of this screen is not verbatim:** {screen.get('nameNotQuotedEn') or ''}")
        out.append("")

    rows: list[tuple[str, str, str, str]] = []
    blocks: list[tuple[str, str]] = []
    notes: list[str] = []
    for field in console_screen_fields(screen):
        if field.get("slotKey") in LISTING_TEXT_LITERALS:
            rows.extend(console_localized_rows(field, ctx, blocks))
            continue
        value, why, block, field_notes = console_field_cell(field, screen, ctx)
        cap = field.get("cap")
        # One label per field, never the same word twice: `Set prices` is its own translation. The
        # label is the console's own, read from the field or from the model path it names, because
        # the sheet's first column is contracted to be the row a human finds on the screen.
        name_ru = console_field_name(field)
        name_en = console_field_name(field, lang="en")
        label = f"{name_ru} ({name_en})" if name_en and name_en != name_ru else str(name_ru)
        # A row whose answer was read on another product branch is work, not a value: the cell names
        # the field and the branch before any reader gets to print somebody else's instruction.
        refusal = console_product_row_cell(screen, field, ctx, label)
        if refusal is not None:
            rows.append((label, str(cap) if cap else "—", refusal, why))
            continue
        rows.append((label, str(cap) if cap else "—", value, why))
        if block is not None:
            # A value pasted into two fields of the same wizard is one block and one pointer: the
            # wizard asks for the same tags twice, and a second copy of a 3-line block is noise the
            # human has to read twice. The first field to need it owns it.
            key = str(field.get("literal") or label)
            owner = ctx["blocks"].get(key)
            if owner:
                rows[-1] = (label, str(cap) if cap else "—", f"paste the same block as «{owner}» above", why)
                # The pointer already carries the value, so the notes that explain how this value was
                # derived were printed where the value is, and repeating them would be repetition.
                field_notes = []
            else:
                ctx["blocks"][key] = label
                blocks.append((label, block))
        notes.extend(field_notes)
    if rows:
        out.append("| Console field (as shown) | Limit | What to type | Why |")
        out.append("|---|---:|---|---|")
        for label, cap, value, why in rows:
            out.append(
                f"| {console_cell(label)} | {console_cell(cap)} | {console_cell(value)} | {console_cell(why)} |"
            )
        out.append("")
    for label, block in blocks:
        out.append(f"**{label} — paste:**")
        out.append("")
        out.append("```text")
        out.append(block)
        out.append("```")
        out.append("")
    for ref in screen.get("bodyFrom") or []:
        path = ref.get("from") if isinstance(ref, dict) else ref
        label = ref.get("label") if isinstance(ref, dict) else None
        # A line can belong to one capability only - the sentence about a promo code reaching the
        # paid content is false in a slot that sells nothing, and a `markersEn` check would catch it
        # as product vocabulary in a non-billing pack's sheet.
        needed = str(ref.get("capability") or "") if isinstance(ref, dict) else ""
        if needed and needed not in ctx["enabled"]:
            continue
        value = console_model_value(str(path))
        if isinstance(value, list):
            if not value:
                continue
            if label:
                out.append(f"{label}:")
            out.extend(f"- {item}" for item in value)
            out.append("")
            continue
        rendered = str(value if value is not None else "").strip()
        if not rendered:
            continue
        out.append(f"{label}: {rendered}" if label else rendered)
        out.append("")
    if screen.get("actionRu"):
        label = f"`{screen['actionRu']}`"
        english = str(screen.get("actionEn") or "")
        if screen.get("ownerAction"):
            # The action line is an instruction, so a screen whose press belongs to the owner prints the
            # refusal in its place: the label stays read from `actionRu`, and the reason comes from the
            # model rather than from this renderer - a screen that says "press" here and "never press" a
            # paragraph lower has two answers to one question.
            reason = str(screen.get("ownerActionEn") or "").strip()
            out.append(f"Do NOT press {label} ({english}): {reason}".rstrip())
        else:
            out.append(f"Press {label} ({english}) to save this screen.".rstrip())
        out.append("")
    for note in notes:
        out.append(note)
        out.append("")
    if screen.get("needsVerification"):
        out.append(
            "NEEDS-VERIFICATION: the console's own wording here was not observed; confirm it on the "
            "page before submitting."
        )
        out.append("")
        specific = str(screen.get("needsVerificationEn") or screen.get("needsVerificationRu") or "")
        if specific:
            out.append(specific)
            out.append("")
    for sub in screen.get("subscreens") or []:
        out.extend(console_screen_sheet(sub, ctx, level + 1))
    return out


def console_audience_store_row(listing: dict[str, Any]) -> tuple[str, str]:
    """(the wording the publishing-overview row prints, the work when nobody has read it).

    That row names the age group in the *branch's own words* (`Целевая возрастная группа: 18 и
    старше.` on `adults-only`), so the counter table substitutes the branch this slot declares instead
    of printing the observed branch's wording at every slot - the defect this function removes. The
    wording is model data on the branch (`targetAudience.audiences.<branch>.storeRowAgeRu`), and a
    branch nobody has read refuses rather than borrowing another branch's words, because those are
    words the slot does not have on its screen.
    """
    section = console_form().get("declarationCompletion") or {}
    template = str(section.get("audienceStoreRowRefusalEn") or "").strip()
    if "{branch}" not in template:
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} declarationCompletion.audienceStoreRowRefusalEn carries "
            "no `{branch}` placeholder, so a branch nobody has read cannot be named by the refusal"
        )
    token, branch, notes = target_audience_branch(listing)
    if token is None:
        return "", " ".join(notes) or template.replace("{branch}", "?")
    wording = str(branch.get("storeRowAgeRu") or "").strip()
    if wording:
        return wording, ""
    return "", template.replace("{branch}", token)


def console_completion_lines(listing: dict[str, Any]) -> list[str]:
    """The model's statement of how a *saved* declaration is read, printed above the screens.

    The owner read the wizard's `Шаг N из M` strip as a completion counter and took a saved
    declaration for an unfinished one. Completion is reported by the console's own counters -
    `Требуется действие (N)`, `Обработано`, `Выполнено: N из M`, the row in `Обзор публикации`
    and a disabled `Сохранить` - and they live in the model so this sheet and the Russian
    walkthrough carry the same sentence. A model that does not carry them is a refusal rather than
    a sheet that quietly omits the one paragraph the owner needed.

    Two of the counters print slot values rather than shapes, and both are handled here instead of
    being written into the model: the numbers are this slot's own and are read off the screen
    (`numbersRuleEn` says so), and the publishing-overview row names the age group in its branch's
    words, which comes from the branch the slot declares. The rest of the row is the console's
    wording and is printed verbatim.
    """
    section = console_form().get("declarationCompletion") or {}
    if not isinstance(section, dict) or not section.get("counters"):
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} declarationCompletion.counters must name the "
            "counters that report a finished declaration: the wizard's step strip is not one of them, "
            "and a sheet without this statement is how a saved declaration reads as unfinished."
        )
    _require_keys(
        section,
        ("titleEn", "leadEn", "warningEn", "citation", "numbersRuleEn", "audienceStoreRowRuleEn"),
        "declarationCompletion",
    )
    audience_row, audience_work = console_audience_store_row(listing)
    out = [f"## {section['titleEn']}", "", str(section["leadEn"]), ""]
    for counter in section["counters"]:
        _require_keys(counter, ("whereEn", "shows", "meansEn"), "declarationCompletion.counters")
        shows = str(counter["shows"]).replace("{audienceStoreRow}", audience_row or "TODO-HUMAN")
        out.append(f"- **{counter['whereEn']}** → {shows} — {counter['meansEn']}")
    out.append("")
    out.append(str(section["numbersRuleEn"]))
    out.append("")
    out.append(str(section["audienceStoreRowRuleEn"]))
    out.append("")
    if audience_work:
        out.append(f"**TODO-HUMAN:** {audience_work}")
        out.append("")
    out.append(f"**{section['warningEn']}**")
    out.append("")
    out.append(f"Source: {section['citation']}.")
    out.append("")
    return out


def _require_keys(node: Any, keys: tuple[str, ...], where: str) -> None:
    missing = [key for key in keys if not isinstance(node, dict) or not str(node.get(key) or "")]
    if missing:
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} {where} is missing {', '.join(missing)}; the "
            "document is rendered from the model, so a half-written entry cannot be rendered"
        )


def console_answers_document(
    store: Path,
    listing: dict[str, Any],
    locales: list[str],
    compliance: dict[str, Any],
    release_name: str,
) -> str:
    """The Ctrl-C/Ctrl-V sheet: every first-upload console screen, field by field."""
    form = console_form()
    reader_errors = console_model_reader_errors()
    if reader_errors:
        raise StoreKitError(
            "the console form names a declaration field no reader fills, so the sheet would print a "
            "value written for one app: " + "; ".join(reader_errors)
        )
    credentials = credentials_rule(compliance)
    creds = form["credentials"]
    enabled = [str(item) for item in (compliance.get("enabled") or [])]
    claims = console_claims(store, listing, locales, release_name)
    ctx: dict[str, Any] = {
        "store": store,
        "slot": store.parent,
        "listing": listing,
        "locales": locales,
        "compliance": compliance,
        "credentials": credentials,
        "creds": creds,
        "enabled": enabled,
        "literals": console_product_literals(store, listing, locales),
        "instructions": console_review_instructions(listing, creds, credentials),
        "release_name": release_name,
        "blocks": {},
    }
    parts: list[str] = []
    parts.append(f"# Play Console answers — {listing['appName']}")
    parts.append("")
    parts.append(CONSOLE_ANSWERS_MARKER)
    parts.append("")
    parts.append(
        "Every screen the Play Console asks for on a first upload, in console order, with the exact "
        "string to put in each field and the reason behind it. Regenerated from this slot's own pack "
        "on every `store-kit.py` run: editing this file changes nothing, so fix the pack and regenerate."
    )
    parts.append("")
    parts.append(
        f"`{form['docPath']}` is the same walk in Russian, rendered from the same model "
        f"(`{form['model']}`), so the two cannot disagree about the form."
    )
    parts.append("")
    parts.append(
        "- Console labels are the Russian ones you will see in the console; the values are what to "
        "type. Review instructions are English because the console demands English for them."
    )
    parts.append("- A line marked `TODO-HUMAN` is not pasteable yet: it names what the pack is missing.")
    parts.append("")
    parts.extend(console_completion_lines(listing))
    for screen in console_screens():
        capability = str(screen.get("capability") or "")
        if capability and capability not in enabled:
            continue
        parts.extend(console_screen_sheet(screen, ctx))

    # --- Machine-readable claims ------------------------------------------------------------------
    parts.append("## Machine-readable claims — do not paste")
    parts.append("")
    parts.append(
        "`store-kit.py --validate-only` reads this block and fails when it disagrees with "
        "`listing.json`, so a sheet left behind by an older pack cannot pass as current."
    )
    parts.append("")
    parts.append(f"```{CONSOLE_CLAIMS_FENCE}")
    parts.append(json.dumps(claims, ensure_ascii=False, indent=2, sort_keys=True))
    parts.append("```")
    parts.append("")
    return "\n".join(parts)

def parse_console_claims(path: Path) -> dict[str, Any]:
    """The claims block of a `console-answers.md`, or {} when the file is not generator output."""
    if not path.is_file():
        return {}
    match = re.search(
        rf"^```{CONSOLE_CLAIMS_FENCE}[ \t]*\n(.*?)^```",
        read_text(path),
        re.MULTILINE | re.DOTALL,
    )
    if not match:
        return {}
    try:
        value = json.loads(match.group(1))
    except json.JSONDecodeError:
        return {}
    return value if isinstance(value, dict) else {}


def validate_console_answers(
    store: Path, listing: dict[str, Any], locales: list[str], release_name: str
) -> list[str]:
    """Check the sheet against the pack it was generated from.

    Three independent things are measured, and they fail for different reasons: the sheet must carry
    a claims block at all (a file without one is not generator output), each claim must equal what
    the pack says now (so a stale sheet cannot keep a superseded answer), and the text must not
    contain a reserved documentation host (the field-level equivalent of pasting a placeholder into
    the console).
    """
    path = store / "console-answers.md"
    if not path.is_file() or not path.stat().st_size:
        return []
    failures: list[str] = []
    expected = console_claims(store, listing, locales, release_name)
    actual = parse_console_claims(path)
    if not actual:
        return [
            f"console-answers.md carries no ```{CONSOLE_CLAIMS_FENCE} claims block, so it was not written by "
            "store-kit.py and nothing in it can be checked against the pack"
        ]
    for key in sorted(expected):
        if key not in actual:
            failures.append(f"console-answers.md claims block is missing {key!r}")
        elif actual[key] != expected[key]:
            failures.append(
                f"console-answers.md claims {key} = {actual[key]!r} but the pack says {expected[key]!r}"
            )
    text = read_text(path)
    for host in PLACEHOLDER_HOSTS:
        if host in text:
            failures.append(
                f"console-answers.md contains the reserved documentation host {host!r}; the sheet must never "
                "hand the console a placeholder value"
            )
    return failures


# --- The live-transcription handoff --------------------------------------------------------------
# One worker reads a screen in the live Play Console and writes one file per screen under
# `.kiln/console-observations/`. Those files are observations, not model edits: a program cannot
# paraphrase the console's own labels into the model, and a branch nobody has read cannot be invented.
# So this half of the tool decides whether a file may be believed at all - failing loudly on a
# paraphrase, an unknown schema version, or a field id that belongs to another screen - and then names
# the model edits the file implies, applying only those the model itself declares foldable. The rest
# are printed as work, because a silently applied observation is how a guide becomes a copy of one
# session.
#
# The schema below is the shape the handoff *agreed*, widened 2026-09-29 to the shape the first real
# handoff was written in. The first version demanded `branch`, `fields` and `steps` of every file and
# rejected all four transcriptions on disk: a page that lists the declarations has no fields of its
# own, the app-access screen is not a wizard, and a screen the observer searched seven pages for and
# did not find cannot honestly report either. The fix is the agreed richer shape - the observed keys
# named, a shape for a negative finding, and a key for the evidence `persisted` is claimed on - and not
# a more tolerant validator: a key the schema does not name is still reported, a label that paraphrases
# the console is still a refusal, and a file that leaves out what its own shape has to say is still
# rejected.
CONSOLE_OBSERVATIONS_RELATIVE = Path(".kiln") / "console-observations"
OBSERVATION_SCHEMA_VERSION = 1

# Every file carries these, whatever it is a report about: which schema it is in, which screen it is
# about, what the console calls that screen, when it was read, where the reading is cited from, and the
# reader's own notes. A file that cannot say all six is not a transcription of anything: it cannot be
# compared with the model and it cannot be found again.
OBSERVATION_REQUIRED_KEYS = (
    "schemaVersion",
    "screenId",
    "consoleNameRu",
    "observedAt",
    "citation",
    "notesRu",
)

# A file that searched for a screen and did not find it is an honest report, and what it must supply
# instead of a field transcription is where it searched and what it could not read. The shape is
# recognised by the finding itself (`foundOnAnyReachableScreen: false`) rather than by a shape name, so
# a transcriber cannot pick whichever shape demands least: the marker *is* the finding, and a file that
# carries it may not also claim a save.
OBSERVATION_NEGATIVE_MARKER = "foundOnAnyReachableScreen"
OBSERVATION_NEGATIVE_REQUIRED_KEYS = ("searchedScreens", "blockerRu")

# A file about a screen that *was* found has to say what the observer did on it and whether the console
# kept what it read. A report of a screen carrying neither is a screen name and a list of nothing.
OBSERVATION_FOUND_REQUIRED_KEYS = ("actions", "saveObserved", "persisted")

# The whole schema, so a key that does not exist is a reported difference rather than a key that is
# quietly ignored while the file looks accepted. Which keys are *required* is deliberately not decided
# here: the base keys above are required of every file and the two shapes add their own, because a
# declaration screen, a page that lists declarations and a screen nobody could reach do not carry the
# same rows - and one required-key list for all three is what rejected four honest files on disk.
OBSERVATION_KEYS: dict[str, str] = {
    "schemaVersion": "int",
    "screenId": "str",
    "consoleNameRu": "str",
    "consoleNameEn": "none-or-str",
    "observedAt": "str",
    "observedBy": "none-or-str",
    # Provenance of the *reading* rather than of the model: the branch that was read, the source that
    # named it, and the tree the observer was looking at, so a claim can be re-checked later.
    "branch": "none-or-str",
    "branchSource": "none-or-str",
    "gitBranch": "none-or-str",
    "gitHead": "none-or-str",
    # Where the page is, and what it printed above its rows.
    "pagePathObserved": "none-or-str",
    "pageHeadingRu": "none-or-str",
    "pageIntroRu": "none-or-str",
    "pageLegacyNameRu": "none-or-str",
    # The app the screen was read on, and the blocks a page prints. Named as a whole: they are the
    # page's own layout, nothing in the model is folded out of them, and their inner keys are the
    # transcriber's own record of what it saw.
    "app": "object",
    "tabs": "list",
    "dashboardCard": "object",
    "publishingOverview": "object",
    "completionCounter": "object",
    # The rows, in the shapes the screens have them: a declaration's fields, a wizard's steps.
    "fields": "list",
    "steps": "list",
    # What the observer did, and whether the console kept what it read.
    "actions": "list",
    "saveObserved": "bool",
    "saveObservedNoteRu": "none-or-str",
    "persisted": "bool",
    "persistedEvidenceRu": "none-or-str",
    "persistedEvidence": "none-or-str",
    # The negative finding: the item nobody could reach, the pages that were searched, and the reader's
    # next action.
    "foundOnAnyReachableScreen": "bool",
    "searchedScreens": "list",
    "certifications": "list",
    "blockerRu": "none-or-str",
    "bestNextActionRu": "none-or-str",
    # The reader's notes, and the citation the whole file is believed on.
    "notesRu": "list",
    "citation": "str",
}

# A declaration's row. There are two label keys because the console prints two kinds of row and a
# transcription may only quote what it saw: `nameRu` is the label above a control («В вашем приложении
# есть закрытые разделы?»), `literalQuestionRu` is the question a wizard step asks («Какие целевые
# возрастные группы у вашего приложения?»). A row whose question could not be read carries that key
# with a null - the honest statement that the step never rendered - which is why the label keys are
# `none-or-str` and why the rule below is about the key being *present*.
OBSERVATION_FIELD_KEYS: dict[str, str] = {
    "id": "str",
    "nameRu": "none-or-str",
    "nameEn": "none-or-str",
    "literalQuestionRu": "none-or-str",
    "kind": "str",
    "optionsRu": "list",
    "literalOptionsRu": "list",
    "valueRu": "none-or-str",
    "answerRu": "none-or-str",
    "answerEn": "none-or-str",
    "slotKey": "none-or-str",
    "whyRu": "str",
    "whyEn": "none-or-str",
}
OBSERVATION_FIELD_LABEL_KEYS = ("nameRu", "literalQuestionRu")
OBSERVATION_FIELD_REQUIRED_KEYS = frozenset({"id", "whyRu"})

# What the observer did on the screen, in either of the two shapes that reached this tool: the schema's
# own `labelRu`/`observed` pair and the live transcription's `order`/`actionRu`/`observedEffectRu` - the
# click and what it did. Either shape has to name the action, because an action with no label is a
# click nobody can read.
OBSERVATION_ACTION_KEYS: dict[str, str] = {
    "order": "int",
    "labelRu": "none-or-str",
    "observed": "bool",
    "actionRu": "none-or-str",
    "observedEffectRu": "none-or-str",
}
OBSERVATION_ACTION_LABEL_KEYS = ("labelRu", "actionRu")

# A wizard's step. The model folds its `id` and `titleRu` - that is what `observationFold` names - and
# the rest is what the reading saw of the step: whether the console offered it at all, what it rendered
# when it did, and what the observer clicked. A step's nested `fields` is named and not walked: those
# rows are the declaration's own, the file lists them once at the top level, and that is where a label
# is compared against the model's.
OBSERVATION_STEP_KEYS: dict[str, str] = {
    "id": "str",
    "index": "int",
    "titleRu": "str",
    "titleEn": "none-or-str",
    "reachable": "bool",
    "requiredForThisBranch": "bool",
    "pageHeadingRu": "none-or-str",
    "questionHeadingRu": "none-or-str",
    "questionRu": "none-or-str",
    "supportingTextRu": "list",
    "fields": "list",
    "fieldsNoteRu": "none-or-str",
    "buttonsRu": "none-or-list",
    "buttonStateRu": "none-or-str",
    "actionTakenRu": "none-or-str",
    "reachedBy": "none-or-str",
    "observedStateRu": "none-or-str",
    "noteRu": "none-or-str",
}

# A page the observer searched, and one of the items it was looking for. The negative finding is only
# worth believing because of these two: `searchedScreens` is where it looked and `certifications` is
# what it could not read, including the names the owner gave the items - which is the only name a file
# may carry for a checkbox nobody saw.
OBSERVATION_SEARCHED_SCREEN_KEYS: dict[str, str] = {
    "pathObserved": "str",
    "resultRu": "str",
}
OBSERVATION_CERTIFICATION_KEYS: dict[str, str] = {
    "id": "str",
    "nameRuAsGivenByOwner": "str",
    "observedCheckboxLabelRu": "none-or-str",
    "ticked": "none-or-bool",
    "stateRu": "str",
    "slotKey": "none-or-str",
    "whyRu": "str",
}
OBSERVATION_CERTIFICATION_REQUIRED_KEYS = frozenset({"id", "nameRuAsGivenByOwner", "stateRu", "whyRu"})

# The control kinds a transcription may name. It is the model's own vocabulary (`read` and `date` are
# kinds the model's rows carry) plus `note`, the transcriber's word for a row the console prints
# without a control; a kind outside it is a refusal rather than a guess, because the model's `kind`
# decides whether a row needs the console's option labels.
OBSERVATION_FIELD_KINDS = frozenset({"radio", "checkbox", "text", "select", "date", "read", "note"})

# Which keys are optional, per shape: everything the shape does not name as required. Computed from the
# tables rather than restated, so a key added to a table cannot silently become required or optional.
OBSERVATION_OPTIONAL_KEYS = frozenset(set(OBSERVATION_KEYS) - set(OBSERVATION_REQUIRED_KEYS))
OBSERVATION_FIELD_OPTIONAL_KEYS = frozenset(set(OBSERVATION_FIELD_KEYS) - OBSERVATION_FIELD_REQUIRED_KEYS)
OBSERVATION_ACTION_OPTIONAL_KEYS = frozenset(OBSERVATION_ACTION_KEYS)
OBSERVATION_STEP_OPTIONAL_KEYS = frozenset(set(OBSERVATION_STEP_KEYS) - {"id", "titleRu"})
OBSERVATION_SEARCHED_SCREEN_OPTIONAL_KEYS = frozenset(set(OBSERVATION_SEARCHED_SCREEN_KEYS) - {"pathObserved", "resultRu"})
OBSERVATION_CERTIFICATION_OPTIONAL_KEYS = frozenset(
    set(OBSERVATION_CERTIFICATION_KEYS) - OBSERVATION_CERTIFICATION_REQUIRED_KEYS
)

# `persisted` is a claim about the console, and the model's own completion rule
# (`declarationCompletion`) says saved-ness is *read* off the console's counters - the `Обработано` row
# with its date, the line in `Обзор публикации`, or a reload that kept the values - so a transcription
# that established it that way has to be able to say so. Without a key for that statement an honest
# file must either claim `saveObserved: true` for a save it did not watch, or under-report a
# declaration it saw saved. The live handoff wrote `persistedEvidenceRu`, following the model's own
# `...Ru`/`...En` convention; the schema's earlier single-language name stays readable for the same
# reason the audience vocabulary carries aliases - a file written against a shipped key is not refused
# for the name alone. Either way the key states the observation, and never substitutes for one:
# `validate_console_observation` still refuses `persisted` on a screen the console never confirmed.
OBSERVATION_EVIDENCE_KEYS = ("persistedEvidenceRu", "persistedEvidence")


def _observation_type_ok(value: Any, kind: str) -> bool:
    if kind == "int":
        return isinstance(value, int) and not isinstance(value, bool)
    if kind == "str":
        return isinstance(value, str) and bool(value.strip())
    if kind == "none-or-str":
        return value is None or isinstance(value, str)
    if kind == "bool":
        return isinstance(value, bool)
    if kind == "none-or-bool":
        return value is None or isinstance(value, bool)
    if kind == "list":
        return isinstance(value, list)
    if kind == "none-or-list":
        return value is None or isinstance(value, list)
    if kind == "object":
        return isinstance(value, dict)
    return False


def _observation_key_failures(
    node: Any, schema: dict[str, str], where: str, optional: frozenset[str] = frozenset()
) -> tuple[list[str], list[str]]:
    """(failures, keys that do not exist in the schema) for one object of an observation file."""
    if not isinstance(node, dict):
        return [f"{where} must be an object"], []
    failures: list[str] = []
    for key, kind in sorted(schema.items()):
        if key not in node:
            if key not in optional:
                failures.append(f"{where} is missing the required key {key!r}")
        elif not _observation_type_ok(node[key], kind):
            failures.append(f"{where}.{key} must be {kind}")
    return failures, _observation_unknown_keys(node, schema, where)


def _observation_unknown_keys(node: Any, schema: dict[str, str], where: str) -> list[str]:
    if not isinstance(node, dict):
        return []
    return [f"{where}.{key}" for key in sorted(node) if key not in schema]


def console_slot_keys() -> set[str]:
    """Every pack path the model reads, gathered from the model rather than restated here.

    An observation may name the slot key that should fill a field. The model already knows which pack
    paths exist - they are its `slotKey` values - so a key outside that set is a key no reader can
    fill, which is a report about the model and not a value to print.
    """
    found: set[str] = set()

    def walk(node: Any) -> None:
        if isinstance(node, dict):
            key = node.get("slotKey")
            if isinstance(key, str) and key:
                found.add(key)
            for value in node.values():
                walk(value)
        elif isinstance(node, list):
            for item in node:
                walk(item)

    walk(console_form())
    return found


def observation_fold_table() -> dict[str, Any]:
    """The model's declaration of what a fold may write.

    A fold is a program writing the model, which is the one action this whole handoff exists to keep
    out of an unobserved branch's path: the table names the field a fold may stamp and the screens it
    may reach, and the tool refuses to guess beyond it.
    """
    table = console_form().get("observationFold")
    if not isinstance(table, dict) or not table:
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} observationFold must declare what a fold may write "
            "(`provenanceField` and `targets`); a tool that writes the model by its own judgement is "
            "the drift the model exists to prevent"
        )
    if not str(table.get("provenanceField") or ""):
        raise StoreKitError(f"{CONSOLE_FORM_RELATIVE.as_posix()} observationFold.provenanceField is empty")
    if not isinstance(table.get("targets"), dict):
        raise StoreKitError(f"{CONSOLE_FORM_RELATIVE.as_posix()} observationFold.targets must be an object")
    return table


def observation_fold_targets() -> dict[str, dict[str, Any]]:
    """Which screens the model allows a fold into, and where a wizard's steps land.

    Declared in the model rather than in this file: a fold that writes model content is a decision,
    and a decision taken by the tool rather than declared by the model is the drift the model exists
    to prevent. A screen with no entry still accepts provenance - being observed is a fact - but its
    steps and fields are only reported.
    """
    return dict(observation_fold_table()["targets"])


# The containers a file may carry lists of objects in, so the "which of your keys do not exist"
# report reaches the same shapes whichever kind of screen the file is about.
OBSERVATION_CONTAINER_SCHEMAS: tuple[tuple[str, dict[str, str]], ...] = (
    ("fields", OBSERVATION_FIELD_KEYS),
    ("steps", OBSERVATION_STEP_KEYS),
    ("actions", OBSERVATION_ACTION_KEYS),
    ("searchedScreens", OBSERVATION_SEARCHED_SCREEN_KEYS),
    ("certifications", OBSERVATION_CERTIFICATION_KEYS),
)


def observation_field_label(field: dict[str, Any]) -> str:
    """The console's own label for one transcribed row, whichever shape the row was written in.

    `nameRu` is the label above a control; `literalQuestionRu` is the question a wizard step asks. A
    row that carries the key with a null says in the file itself that the step never rendered, and
    gives back an empty label - which is not a paraphrase, so it is reported as a model edit rather
    than as a refusal.
    """
    for key in OBSERVATION_FIELD_LABEL_KEYS:
        if key in field:
            return str(field.get(key) or "")
    return ""


def observation_persisted_evidence(observation: dict[str, Any]) -> str:
    """The statement a `persisted: true` is believed on, under either name the schema reads."""
    for key in OBSERVATION_EVIDENCE_KEYS:
        evidence = str(observation.get(key) or "").strip()
        if evidence:
            return evidence
    return ""


def console_unreachable_ids() -> set[str]:
    """The items the model states as unreadable from any reachable page, by id.

    A file that searched and found nothing is not a request to add a screen: what the model owes is the
    refusal, and a model that already carries it owes nothing. This set is what tells the two apart, so
    the report cannot print a sentence the model already carries as work for a reader.
    """
    items = (console_form().get("unreachable") or {}).get("items") or []
    return {str(item.get("id")) for item in items if isinstance(item, dict) and item.get("id")}


def console_counter_row_sources() -> dict[str, str]:
    """The observed rows the model carries as the console's own counters, by row id.

    A counter is not a row of a form: the console prints `Выполнено: 10 из 11` as a summary, and the
    model carries the *shape* of that summary (`declarationCompletion.counters`, with the rule that its
    numbers are read off the screen) rather than one app's numbers. A transcription that names such a
    row is therefore not describing a field the model is missing, and printing it as a field to add
    sends a reader looking for a console row that does not exist.

    The mapping is declared in the model (`observationFold.counterRows`: the file's row id -> the id of
    the counter that carries it) and checked there, because a permission the tool grants itself is the
    drift this handoff exists to stop.
    """
    rows = observation_fold_table().get("counterRows") or {}
    if not isinstance(rows, dict):
        raise StoreKitError(
            f"{CONSOLE_FORM_RELATIVE.as_posix()} observationFold.counterRows must be an object mapping "
            "an observation's row id to the counter that carries it"
        )
    return {str(row_id): str(counter) for row_id, counter in rows.items() if str(row_id).strip()}


def console_observation_instant(value: Any) -> datetime | None:
    """One observation timestamp as an instant, or None when it is not a timestamp this tool reads.

    Two spellings of one reading appear in the model and in the files - `2026-09-29T18:12:00+03:00` and
    `2026-09-29T18:43+03:00` - so comparing the strings would call one reading of a screen two different
    readings. A value that is not a timestamp (a date alone, or prose) gives None rather than raising:
    what the model carries is compared, not repaired.
    """
    text = str(value or "").strip()
    if not text:
        return None
    try:
        return datetime.fromisoformat(text)
    except ValueError:
        return None


def console_records_reading(screen: dict[str, Any], observed_at: Any) -> bool:
    """Whether the screen already records this exact reading of itself.

    The provenance field is the model's own statement that a screen was read, and a file that reports
    the reading the model already carries implies no edit: printing it as work is how a landed edit
    keeps reading as a fold a reader must still make. The comparison is between two instants rather than
    two strings, so the two spellings of one moment are one reading - and a file whose timestamp the
    model does not carry (a later session, or a date alone) is still named as the edit it implies.
    """
    recorded = screen.get(str(observation_fold_table()["provenanceField"]))
    instant = console_observation_instant(observed_at)
    return instant is not None and console_observation_instant(recorded) == instant


def console_missing_observed_steps(steps_path: str, observed: list[Any]) -> list[str]:
    """The observed steps the model's own list at `steps_path` does not carry, by id.

    The model declares where a wizard's steps live (`observationFold.targets[].stepsPath`), so whether
    the model "carries none" is a question the model answers and not one the report may assume. It
    assumed it: an observation of a wizard whose five titles the model already held was printed as
    "the 5 observed step title(s), where the model carries none", which is a claim about the model that
    was never compared with it, and a reader following that row would look for work that does not
    exist. The comparison is the fix - a step the model already carries, with the title the console
    printed, implies no edit.
    """
    node: Any = console_form()
    for key in steps_path.split("."):
        node = node.get(key) if isinstance(node, dict) else None
    carried = {
        str(step.get("id")): str(step.get("titleRu") or "")
        for step in (node if isinstance(node, list) else [])
        if isinstance(step, dict)
    }
    missing: list[str] = []
    for step in observed:
        if not isinstance(step, dict):
            continue
        step_id = str(step.get("id") or "")
        if step_id and carried.get(step_id) == str(step.get("titleRu") or ""):
            continue
        missing.append(step_id or str(step.get("titleRu") or ""))
    return missing


def observed_model_screen(
    observation: dict[str, Any], *, negative: bool = False
) -> tuple[dict[str, Any] | None, str, list[str]]:
    """(the model screen a file is about, how it was found, the edit the question implies).

    The id is what the model names a screen by, and it is what a file should use. The console's own
    label is the fallback, because the console is what decides a screen's identity and a transcription
    may name its file after the *page* it was read on rather than the screen the model carries.
    Resolving by label is what keeps such a file from being reported as a screen to add: the screen is
    in the model, and a second screen with the same console label would be the same declaration counted
    twice. Which way a screen was found is returned as well, because a fold may only write provenance
    under an id the model itself uses.

    The fallback is a last resort rather than the arrangement: on 2026-09-29 a file named
    `declaration-app-access` resolved this way to the model's `declaration-credentials`, and the model
    then took the file's name (`screenNaming` states the rule) so that the next transcription of that
    screen resolves by id and nobody has to decide which of two names to keep.

    `negative` is for a file that searched and found nothing: it adds no screen, because nothing was
    seen. What the model owes then is a statement that the item cannot be read from a reachable page -
    named by the caller, which knows what was searched - rather than a screen with rows nobody saw.
    """
    screen_id = str(observation.get("screenId") or "")
    label = str(observation.get("consoleNameRu") or "")
    screens = console_screens()
    for screen in screens:
        if str(screen.get("id")) == screen_id:
            return screen, "id", []
    for screen in screens:
        if label and str(screen.get("consoleNameRu") or "") == label:
            return screen, "label", [
                f"`{CONSOLE_FORM_RELATIVE.as_posix()}` screen {screen_id!r} is not the id of a screen "
                f"the model carries, and «{label}» is the console label of {screen.get('id')!r}; the "
                "observation is about that screen, so either the file's `screenId` is corrected or the "
                "model's id changes - a second screen with the same console label would be the same "
                "declaration counted twice"
            ]
    # A screen may record the extra labels the console was seen printing for it (`observedLabelsRu`)
    # and, where the console appends the observer's own record name, a label prefix
    # (`observedLabelPrefixesRu`). The rule is stated once in the model
    # (`observationFold.labelAliasRuleRu/En`) and implemented here, so a screen that has already been
    # read under a second name is not reported as a screen to add - which is exactly the second copy
    # of one declaration the exact-label pass above exists to prevent. Sub-screens are searched too,
    # because a dialog is one of those names: it is a page the console opens inside its declaration
    # and it carries rows of its own.
    aliases: list[tuple[dict[str, Any], str]] = []
    for screen in screens:
        for node in [screen] + list(screen.get("subscreens") or []):
            aliases.append((node, str(node.get("id"))))
    for node, _node_id in aliases:
        for recorded in node.get("observedLabelsRu") or []:
            if label and str(recorded) == label:
                return node, "observed-label", []
        for prefix in node.get("observedLabelPrefixesRu") or []:
            if label and prefix and label.startswith(str(prefix)):
                return node, "observed-label", []
    if negative:
        return None, "none", []
    return None, "none", [
        f"{CONSOLE_FORM_RELATIVE.as_posix()} must add a screen {screen_id!r}: it carries no screen with "
        f"that id and none whose console label is «{label}», so nothing in the model is being observed"
    ]


def observation_shape_failures(observation: dict[str, Any], label: str) -> list[str]:
    """The keys a file owes for what it says it is, checked against the finding it reports.

    A file that says the screen was not found anywhere is a negative finding, and it reports where it
    searched and what it could not read instead of rows. A file about a screen that *was* found reports
    what the observer did and whether the console kept it. Neither shape may claim the other's facts,
    because a file that reports nothing but also lacks the keys of both shapes is a screen name claiming
    to be evidence.
    """
    failures: list[str] = []
    if observation.get(OBSERVATION_NEGATIVE_MARKER) is False:
        for key in OBSERVATION_NEGATIVE_REQUIRED_KEYS:
            if key not in observation:
                failures.append(
                    f"{label} is a negative finding ({OBSERVATION_NEGATIVE_MARKER}: false) and is "
                    f"missing the required key {key!r}: a screen nobody found still has to say where it "
                    f"was looked for ({OBSERVATION_NEGATIVE_REQUIRED_KEYS[0]}) and what could not be "
                    f"read ({OBSERVATION_NEGATIVE_REQUIRED_KEYS[1]})"
                )
        if not observation.get("searchedScreens"):
            failures.append(
                f"{label} is a negative finding and names no screen it searched; where it looked is the "
                "only evidence that the search happened"
            )
        if not str(observation.get("blockerRu") or "").strip():
            failures.append(
                f"{label} is a negative finding and states no blocker; what could not be read is what a "
                "reader has to know before believing the item is not on the console"
            )
        for key in OBSERVATION_FOUND_REQUIRED_KEYS:
            if observation.get(key):
                failures.append(
                    f"{label} is a negative finding and claims {key}: {key!r} is a fact about a screen "
                    "that was found, and this file says there was nothing to find"
                )
    else:
        for key in OBSERVATION_FOUND_REQUIRED_KEYS:
            if key not in observation:
                failures.append(
                    f"{label} is missing the required key {key!r}: a report of a screen that was found "
                    "says what the observer did (`actions`) and whether the console kept what it read "
                    "(`saveObserved`, `persisted`)"
                )
        if "actions" in observation and not observation["actions"]:
            failures.append(
                f"{label} reports no action taken: a screen that was found was reached by doing "
                "something, and `actions` is what the file did there"
            )
    return failures


def validate_console_observation(
    observation: Any, label: str
) -> tuple[list[str], list[str], list[str]]:
    """Check one observation file. Returns (failures, implied edits, keys outside the schema).

    A failure is a file that must not be folded *or* believed: a label paraphrased away from the
    console's, a schema version this tool does not know, a field id that already belongs to a
    different screen, a shape that leaves out what it would have to say, or a claim that a screen
    persisted when the console has never confirmed it. Those are the ways an observation becomes an
    invented model entry.
    """
    failures, unknown = _observation_key_failures(
        observation, OBSERVATION_KEYS, label, OBSERVATION_OPTIONAL_KEYS
    )
    if failures:
        # A file missing a required key is reported on its own, and the keys it carries that the schema
        # has never heard of are reported too: "which of its keys do not exist in the schema" is the
        # question a transcriber has to answer to correct the file, and it is invisible from "missing
        # key 'citation'" - the nested keys were the part this early return used to swallow.
        if isinstance(observation, dict):
            for container, schema in OBSERVATION_CONTAINER_SCHEMAS:
                items = observation.get(container)
                if isinstance(items, list):
                    for index, item in enumerate(items):
                        unknown.extend(
                            _observation_unknown_keys(item, schema, f"{label}.{container}[{index}]")
                        )
        return failures, [], unknown
    if observation["schemaVersion"] != OBSERVATION_SCHEMA_VERSION:
        return (
            [
                f"{label}.schemaVersion is {observation['schemaVersion']!r}, and this tool understands "
                f"{OBSERVATION_SCHEMA_VERSION}; a newer file may carry keys whose meaning has changed"
            ],
            [],
            unknown,
        )

    screen_id = str(observation["screenId"])
    implied: list[str] = []

    # What the file is, from what it says about itself, before anything is compared with the model: a
    # negative finding reports a search rather than rows, and a file about a screen that was found has
    # to say what it did there and whether the console kept it.
    failures.extend(observation_shape_failures(observation, label))

    # Which model screen this is about, resolved by id and then by the console's own label.
    negative = observation.get(OBSERVATION_NEGATIVE_MARKER) is False
    screen, matched_by, screen_notes = observed_model_screen(observation, negative=negative)
    implied.extend(screen_notes)
    resolved_id = str(screen.get("id")) if screen is not None else ""

    # A screen the model already carries must be named as the console names it. A paraphrase is how a
    # screen that is not this one gets folded into it, and the model's own label is the only one the
    # console was observed to print. This is only checkable when the file named the screen by id: a file
    # resolved *by* its label has already quoted the model's own wording, or it would not have matched.
    if screen is not None and matched_by == "id":
        model_name = str(screen.get("consoleNameRu") or "")
        if model_name and model_name != str(observation["consoleNameRu"]):
            failures.append(
                f"{label} calls the screen «{observation['consoleNameRu']}» and "
                f"`{CONSOLE_FORM_RELATIVE.as_posix()}` calls it «{model_name}»; a paraphrase is not the "
                "console's label, so the file is not about a screen the model knows"
            )
        elif not model_name:
            implied.append(f"{CONSOLE_FORM_RELATIVE.as_posix()} screens[{screen_id}].consoleNameRu <- observed label")

    if observation.get(OBSERVATION_NEGATIVE_MARKER) is False:
        # Nothing was found, so there is nothing to compare a row against: what the file carries is where
        # it searched and what the console showed instead, and the model's stake in that is a statement
        # rather than a row. The search itself is still checked key by key, because a search is only
        # evidence when each page says which page it was and what it showed there.
        searched = len(observation.get("searchedScreens") or [])
        for container, schema, optional in (
            ("searchedScreens", OBSERVATION_SEARCHED_SCREEN_KEYS, OBSERVATION_SEARCHED_SCREEN_OPTIONAL_KEYS),
            ("certifications", OBSERVATION_CERTIFICATION_KEYS, OBSERVATION_CERTIFICATION_OPTIONAL_KEYS),
        ):
            for index, item in enumerate(observation.get(container) or []):
                where = f"{label}.{container}[{index}]"
                item_failures, item_unknown = _observation_key_failures(item, schema, where, optional)
                failures.extend(item_failures)
                unknown.extend(item_unknown)
        # The model may already state this refusal - `unreachable` is where a finding the console does
        # not show lives - and a sentence the model carries implies no edit. Printing it as work is how
        # a satisfied statement reads as a fold a reader must still make.
        if screen_id not in console_unreachable_ids():
            implied.append(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} must state {screen_id!r} as something nobody can "
                f"read from a reachable page: the file searched {searched} page(s) and found nothing, so "
                "what the model owes is the refusal and the reader's next action rather than rows it "
                "never saw"
            )
        if matched_by == "id":
            implied.append(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} screens[{screen_id}] is carried as an observed "
                "screen and this file says no reachable page shows it: the model owes a refusal there "
                "rather than the fields it claims"
            )
        return failures, implied, unknown

    # Field ids: unique inside the file, and never the id of a field that already belongs to another
    # screen. Two screens answering to one field id is a model where a leg can name neither.
    seen: dict[str, int] = {}
    for index, field in enumerate(observation.get("fields") or []):
        where = f"{label}.fields[{index}]"
        field_failures, field_unknown = _observation_key_failures(
            field, OBSERVATION_FIELD_KEYS, where, OBSERVATION_FIELD_OPTIONAL_KEYS
        )
        failures.extend(field_failures)
        unknown.extend(field_unknown)
        if isinstance(field, dict) and isinstance(field.get("id"), str):
            seen[field["id"]] = seen.get(field["id"], 0) + 1
    for field_id, count in sorted(seen.items()):
        if count > 1:
            failures.append(
                f"{label} declares the field id {field_id!r} {count} times; a field id identifies one "
                "field, so the duplicates would fold into each other"
            )
    for index, step in enumerate(observation.get("steps") or []):
        where = f"{label}.steps[{index}]"
        unknown.extend(_observation_unknown_keys(step, OBSERVATION_STEP_KEYS, where))
        step_failures, _ = _observation_key_failures(
            step, OBSERVATION_STEP_KEYS, where, OBSERVATION_STEP_OPTIONAL_KEYS
        )
        failures.extend(step_failures)
    for index, action in enumerate(observation.get("actions") or []):
        where = f"{label}.actions[{index}]"
        unknown.extend(_observation_unknown_keys(action, OBSERVATION_ACTION_KEYS, where))
        action_failures, _ = _observation_key_failures(
            action, OBSERVATION_ACTION_KEYS, where, OBSERVATION_ACTION_OPTIONAL_KEYS
        )
        failures.extend(action_failures)
        if isinstance(action, dict) and not any(action.get(key) for key in OBSERVATION_ACTION_LABEL_KEYS):
            failures.append(
                f"{where} names no action: a report of a screen says what the observer did, so each "
                "action carries the label it clicked (`actionRu`), or the schema's own `labelRu`"
            )

    known_keys = console_slot_keys()
    counter_rows = console_counter_row_sources()
    for index, field in enumerate(observation.get("fields") or []):
        if not isinstance(field, dict):
            continue
        field_id = str(field.get("id") or "")
        where = f"{label}.fields[{index}]"
        observed_label = observation_field_label(field)
        if not any(key in field for key in OBSERVATION_FIELD_LABEL_KEYS):
            failures.append(
                f"{where} carries no console label: a row has to quote the label the console prints "
                "(`nameRu`) or the question a wizard step asks (`literalQuestionRu`), or carry that key "
                "with a null to say the step never rendered"
            )
        if "kind" in field and str(field["kind"]) not in OBSERVATION_FIELD_KINDS:
            failures.append(
                f"{where}.kind is {field['kind']!r}; the schema's kinds are "
                f"{', '.join(sorted(OBSERVATION_FIELD_KINDS))}"
            )
        if screen is not None:
            model_fields = {str(item.get("id")): item for item in console_screen_fields(screen)}
            if field_id in counter_rows:
                # The row is the console's own counter, which the model carries as a *shape* rather than
                # as a field: `declarationCompletion.counters` prints it, and the numbers in it are read
                # off the screen. Comparing its label with a field here would compare `Выполнено: N из M`
                # with the same counter carrying one app's numbers, and adding it as a field would make
                # the model store what its own rule refuses to store.
                pass
            elif field_id in model_fields:
                # The comparison is against the label the model *resolves*, not the one it stores:
                # a field may read its label from the model path that carries it, and comparing a
                # transcription against the raw `nameRu` key would compare it against `` for exactly
                # the fields whose label the model states once.
                model_name = console_field_name(model_fields[field_id])
                if model_name and observed_label and model_name != observed_label:
                    failures.append(
                        f"{where} calls the field «{observed_label}» and the model calls {field_id!r} "
                        f"«{model_name}»; the console's own label is what the model carries"
                    )
                model_kind = str(model_fields[field_id].get("kind") or "")
                observed_kind = str(field.get("kind") or "")
                if model_kind and observed_kind and model_kind != observed_kind:
                    # A transcription is evidence about the console, and the kind of control a field
                    # is shown as is part of what was looked at. It is not a failure - the file is
                    # still what the console printed - it is an edit the model owes a reader.
                    implied.append(
                        f"{CONSOLE_FORM_RELATIVE.as_posix()} screen {resolved_id!r} field {field_id!r} "
                        f"is shown as {observed_kind!r} in the console and the model calls it "
                        f"{model_kind!r}"
                    )
            elif observed_label:
                implied.append(
                    f"{CONSOLE_FORM_RELATIVE.as_posix()} screen {resolved_id!r} must gain the field "
                    f"{field_id!r}" + (f" ({field['kind']})" if field.get("kind") else "")
                )
            else:
                # The row was named and its question could not be read - the wizard step never
                # rendered. That is a fact about the console, not a field to add: a field the model
                # gained here would carry a label nobody has seen.
                implied.append(
                    f"{where} names the row {field_id!r} and its console label was not read, so the "
                    "model gains no field from it"
                )
            # A field id the model does not already carry on this screen, and that belongs to another
            # screen, is a collision: the model identifies a field by its screen and its id, and a row
            # *added* under another screen's id makes both ambiguous. A row the target screen already
            # carries is not being added, and the console's own element ids are page-scoped - the
            # store page and the content-rating page both print a row whose DOM id is `category` - so
            # the collision is only reported for the id an observation would introduce.
            if field_id and field_id not in model_fields:
                for other in console_screens():
                    if resolved_id and str(other.get("id")) == resolved_id:
                        continue
                    if field_id in {str(item.get("id")) for item in console_screen_fields(other)}:
                        failures.append(
                            f"{where} reuses the field id {field_id!r}, which already belongs to screen "
                            f"{other.get('id')!r}; a field id identifies one field"
                        )
        slot_key = field.get("slotKey")
        if slot_key and str(slot_key) not in known_keys:
            implied.append(
                f"store-kit.py must gain a reader for the slot key {slot_key!r} named by {where}: no "
                "declaration in the model reads it, so a cell filled from it would be invented"
            )
        for observed_key in ("valueRu", "answerRu", "literalOptionsRu"):
            if field.get(observed_key):
                implied.append(
                    f"{where}.{observed_key} must not become a model value: a field's cell comes from a "
                    "reader (a literal, `declaration`, or a `derived.*` path), and an observed cell is a "
                    "value written for one app"
                )

    # `persisted` is the claim that the console saved the declaration. It needs an observation behind
    # it - the save watched, or the saved state read back the way the model itself reads completion -
    # and it cannot be true of a screen the console never confirmed: the model marks such a screen
    # `needsVerification`, and a file that overrides that mark is exactly the unobserved branch this
    # handoff refuses to guess at.
    if observation.get("persisted"):
        evidence = observation_persisted_evidence(observation)
        if not observation.get("saveObserved") and not evidence:
            failures.append(
                f"{label} claims persisted: true with saveObserved: false and no persisting evidence "
                "stated; nothing was watched saving, so name the observation that shows it stayed "
                "saved (a reload that kept the values, or the declaration's row in «Обработано» with "
                "its date) in `persistedEvidenceRu`"
            )
        elif screen is not None and screen.get("needsVerification"):
            failures.append(
                f"{label} claims persisted: true for {screen_id!r}, and the model marks that screen "
                "NEEDS-VERIFICATION: the console never confirmed it, so the claim is not an observation"
            )
    # A screen the model does not carry is not a reason to refuse the claim. The file transcribed a
    # console page the model has not learned yet - its `pagePathObserved`, heading and rows are what the
    # console printed - and `observed_model_screen` has already named the screen the model owes as an
    # implied edit. What must not happen is a *write*: the fold touches only a screen the model itself
    # names, so a claim about a screen nobody can re-read in the model stays printed as work.

    if screen is not None:
        # The provenance of an observation is a fact about the model's own screen, so it is named
        # against the id the model uses. A file that matched by its console label is believed - the
        # label is what the console decides a screen's identity by - but its provenance is not written:
        # which of the two names the model keeps is a reader's decision, and folding it here would take
        # that decision silently.
        provenance_field = str(observation_fold_table()["provenanceField"])
        if console_records_reading(screen, observation["observedAt"]):
            # The reading is already recorded, so the model owes nothing: naming it would print a
            # landed edit as work a reader still has to do, which is the same defect as a step the
            # model already carries. A file whose timestamp the model does not carry - a later session
            # of the same page, or a date where the model records a time - is still named below.
            pass
        elif matched_by == "id":
            implied.append(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} screens[{screen_id}].{provenance_field} <- "
                f"{observation['observedAt']}"
            )
        else:
            implied.append(
                f"{CONSOLE_FORM_RELATIVE.as_posix()} screens[{resolved_id}].{provenance_field} <- "
                f"{observation['observedAt']} (the file calls the screen {screen_id!r})"
            )
        target = observation_fold_targets().get(resolved_id) or {}
        steps_path = str(target.get("stepsPath") or "")
        if steps_path and observation.get("steps"):
            missing = console_missing_observed_steps(steps_path, observation["steps"])
            if missing:
                implied.append(
                    f"{CONSOLE_FORM_RELATIVE.as_posix()} {steps_path} <- "
                    + ", ".join(f"`{step}`" for step in missing)
                    + f": {len(missing)} of the {len(observation['steps'])} observed step(s) are not in "
                    "the model, and a wizard's steps are what a reader walks, so a step the model does "
                    "not carry is a screen the document never mentions"
                )
    return failures, implied, unknown


def console_observation_rows(path: Path, *, apply: bool) -> tuple[int, list[str]]:
    """Validate one observation file, report what it implies, and fold what the model allows."""
    label = f"{CONSOLE_OBSERVATIONS_RELATIVE.as_posix()}/{path.name}"
    rows: list[str] = []
    if not path.is_file():
        return 1, [f"FAIL {label} — no such observation file"]
    try:
        observation = json.loads(read_text(path))
    except json.JSONDecodeError as exc:
        return 1, [f"FAIL {label} — not JSON: line {exc.lineno}: {exc.msg}"]
    failures, implied, unknown = validate_console_observation(observation, label)
    for key in unknown:
        rows.append(f"NOTE {label} — {key} does not exist in the schema and was ignored")
    if failures:
        rows.extend(f"FAIL {label} — {failure}" for failure in failures)
        rows.append(
            f"REJECTED {label} — {len(failures)} problem(s); nothing was folded and nothing may be "
            "believed from this file until they are answered on the console"
        )
        return 1, rows

    applied: list[str] = []
    remaining: list[str] = []
    if apply:
        applied, remaining = fold_console_observation(observation, implied)
    else:
        remaining = implied
    rows.extend(f"IMPLIED EDIT {row}" for row in applied)
    rows.extend(f"IMPLIED EDIT (not applied) {row}" for row in remaining)
    rows.append(
        f"ACCEPTED {label} — {len(implied)} implied model edit(s), {len(applied)} applied, "
        f"{len(remaining)} left for a reader"
    )
    return 0, rows


def fold_console_observation(observation: dict[str, Any], implied: list[str]) -> tuple[list[str], list[str]]:
    """Apply the model edits a validated observation proves, and list the rest.

    Only what the model declares foldable is written: the screen's provenance, a console name the
    model does not have yet, and a wizard's step titles where `observationFold` names the path. Every
    other implied edit is left printed, because a field's answer, its options and its reason are
    decisions a reader takes with the model in front of them.

    A file that was believed because its console label matched a model screen is reported and not
    folded. Its provenance belongs to the screen the *model* calls that, and writing it there under the
    file's own `screenId` would make the model carry a screen it does not have; writing it into the
    screen the label names decides which of the two names the model keeps, which is a reader's question -
    the implied edit prints the answer to be chosen.
    """
    form = console_form()
    targets = observation_fold_targets()
    provenance_field = str(observation_fold_table()["provenanceField"])
    screen_id = str(observation["screenId"])
    applied: list[str] = []
    remaining: list[str] = []
    screen, matched_by, _notes = observed_model_screen(observation)
    if observation.get(OBSERVATION_NEGATIVE_MARKER) is False:
        # A negative finding is not provenance: the screen was not reached, so a file that says so must
        # not leave an `observedAt` on it - that would record the opposite of what it reports.
        return [], list(implied)
    # A file that names the model's own id, or a console label the model itself records for that
    # screen (`observedLabelsRu`/`observedLabelPrefixesRu`), is about a screen the model carries, and
    # its provenance belongs to that screen. A file resolved only by the console's *page* label is a
    # reader's question and stays printed: the model has no screen under that name.
    if screen is None or matched_by not in ("id", "observed-label"):
        return [], list(implied)
    if console_records_reading(screen, observation["observedAt"]):
        # Nothing to write: the model already records this reading, and a write here would report an
        # applied edit the report never named. The remaining rows are still returned, so a fold that
        # finds nothing to stamp cannot swallow the edits a reader owes.
        return [], list(implied)
    screen[provenance_field] = observation["observedAt"]
    applied.append(
        f"screens[{screen.get('id')}].{provenance_field} <- {observation['observedAt']}"
        + (f" (the file calls the screen {screen_id!r})" if str(screen.get("id")) != screen_id else "")
    )
    if not str(screen.get("consoleNameRu") or ""):
        screen["consoleNameRu"] = observation["consoleNameRu"]
        applied.append(f"screens[{screen_id}].consoleNameRu <- {observation['consoleNameRu']}")
    target = targets.get(screen_id) or {}
    steps_path = str(target.get("stepsPath") or "")
    observed_steps = [step for step in (observation.get("steps") or []) if isinstance(step, dict)]
    steps_row = ""
    # `steps` is optional in the schema - a page that lists declarations has no wizard - so the fold
    # reads it the same way the report does, or a file without one would crash the writer.
    if steps_path and observed_steps:
        node: Any = form
        for key in steps_path.split("."):
            node = node.get(key) if isinstance(node, dict) else None
        steps_row = (
            f"{CONSOLE_FORM_RELATIVE.as_posix()} {steps_path} <- the "
            f"{len(observed_steps)} observed step title(s), where the model carries none"
        )
        if isinstance(node, list) and not node:
            node.extend({"id": step["id"], "titleRu": step["titleRu"]} for step in observed_steps)
            applied.append(steps_row)
    # Everything the report named and the fold did not write stays printed: an implied edit that
    # silently disappears between the report and the fold is how an observation gets forgotten.
    for row in implied:
        if row not in applied:
            remaining.append(row)
    applied_rows = list(applied)
    return applied_rows, remaining


def write_console_form(form: dict[str, Any]) -> None:
    """Write the model back, one key per line, so a fold shows up as a small readable diff."""
    path = ROOT / CONSOLE_FORM_RELATIVE
    path.write_text(json.dumps(form, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def observation_report(target: Path, *, apply: bool) -> int:
    """Validate every observation under a path and report the model edits they imply.

    An empty directory is a PASS with a note rather than a failure: the transcriber works in the
    browser and writes its files when it reaches each screen, so "nothing has been written yet" is a
    state of the handoff and not a fault in it. A rejected file is the opposite - it is a file that
    exists and must not be believed - and it exits non-zero even when a sibling file was accepted.
    """
    files: list[Path]
    if target.is_dir():
        files = sorted(path for path in target.glob("*.json") if path.is_file())
        if not files:
            print(f"NOTE {target.as_posix()} — no observation file yet; nothing to fold and nothing claimed")
            status = target / "STATUS.md"
            if status.is_file():
                print(
                    f"NOTE {status.as_posix()} — the transcriber's status note is not an observation: "
                    "only the per-screen `<screen-id>.json` files are read"
                )
            print("VERDICT: PASS — 0 file(s), 0 rejected")
            return 0
    elif target.is_file():
        files = [target]
    else:
        print(f"FAIL {target.as_posix()} — no such file or directory")
        return 1

    rejected = 0
    folded = 0
    for path in files:
        code, rows = console_observation_rows(path, apply=apply)
        rejected += 1 if code else 0
        for row in rows:
            print(row)
            if row.startswith("IMPLIED EDIT ") and not row.startswith("IMPLIED EDIT (not applied)"):
                folded += 1
    if apply and folded:
        write_console_form(console_form())
        print(f"WROTE {CONSOLE_FORM_RELATIVE.as_posix()} — {folded} foldable edit(s) applied")
    print(f"VERDICT: {'FAIL' if rejected else 'PASS'} — {len(files)} file(s), {rejected} rejected")
    return 1 if rejected else 0


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
    sdk_rows = safety.get("thirdPartySdks", [])
    collects_data = bool(safety.get("collectsData"))
    privacy = f"""# Privacy policy — {listing['appName']}

**Last updated:** {time.strftime('%Y-%m-%d')}

## Summary

{safety['reason']}

## Data collection and sharing

{('The app and its enabled SDKs collect the data types documented in store/data-safety.md.' if collects_data else 'The enabled capabilities declare no collection, transmission, sale, or sharing of user data. Data created in the app remains on the user\'s device and is subject to the device\'s backup settings.')}

## Permissions

{chr(10).join(permission_lines)}

## Third-party services

{('None declared.' if not sdk_rows else chr(10).join(f"- {sdk['name']} {sdk['version']} — {sdk['policyUrl']}" for sdk in sdk_rows))}

## Data retention and deletion

The app has no account system. Users can delete locally stored data by clearing app storage or uninstalling the app.

## Security

Platform security and encrypted transport are required for networked capabilities and operating-system services.

## Contact

Privacy questions: {listing['contactEmail']}

{PRIVACY_MARKER}
"""
    write_preserving(store / "privacy.md", privacy, PRIVACY_MARKER)

    type_rows = safety.get("dataTypes", [])
    data_rows = (
        [
            f"| {markdown_escape(item['type'])} | Yes | {'Yes' if item['shared'] else 'No'} | {markdown_escape(item['purpose'])} |"
            for item in type_rows
        ]
        if type_rows
        else ["| None | No | No | Not applicable |"]
    )
    data_safety = f"""# Play Console data safety answers — {listing['appName']}

## Data collection and sharing

- Does the app collect or share required user data? **{'Yes' if safety['collectsData'] else 'No'}**
- Reason: {safety['reason']}

| Data category | Collected | Shared | Purpose |
|---|---:|---:|---|
{chr(10).join(data_rows)}

## Third-party SDKs

{'None.' if not sdk_rows else chr(10).join(f"- {sdk['name']} {sdk['version']} — {sdk['policyUrl']}" for sdk in sdk_rows)}

## Security practices

- Is collected data encrypted in transit? **{'Not applicable - no data is collected' if not safety.get('collectsData') else ('Yes' if safety['encryptedInTransit'] else 'No')}**
- Can users request data deletion? **{'Yes' if safety['hasAccountDeletion'] else 'Not applicable — no accounts'}**

## Account deletion

- Does the app support accounts? **No**
- Is an account-deletion mechanism required? **No**

{DATA_SAFETY_MARKER}
"""
    write_preserving(store / "data-safety.md", data_safety, DATA_SAFETY_MARKER)

    capability_compliance = listing["capabilityCompliance"]
    authority_note = (
        capability_compliance.get("purchaseAuthority")
        if capability_compliance.get("requiresPurchaseAuthority")
        else "Not required — no SERVER_COST consumables are declared."
    )

    # Generated TODOs must not assert a defect the pack does not have. A line telling a human to
    # replace a placeholder is itself wrong once the value is real, and it teaches the reader to
    # skim the list. The checklist generator already makes its one such line conditional; these are
    # the review-notes equivalent, so both documents agree about what is actually unfinished.
    contact = listing["contactEmail"]
    privacy_url = listing["privacyPolicyUrl"]
    contact_todo = (
        f"- TODO-HUMAN: Replace `{contact}` with a monitored support address - it is still a placeholder."
        if is_placeholder_email(contact)
        else f"- TODO-HUMAN: Confirm `{contact}` is monitored; Play publishes it on the listing."
    )
    privacy_todo = (
        f"- TODO-HUMAN: Host `store/privacy.md` publicly and replace `{privacy_url}` with its final HTTPS URL."
        if is_placeholder_url(privacy_url)
        else f"- TODO-HUMAN: Confirm `{privacy_url}` still serves this `store/privacy.md`."
    )

    review = f"""# Reviewer notes — {listing['appName']}

## Access

{review_access_paragraph(capability_compliance)}

## Review journey

1. Launch the app and review the main overview.
2. Start the primary workflow described by the first action.
3. Complete an item or session and inspect the progress state.
4. Close and reopen the app to confirm local persistence where applicable.

## Permissions

{chr(10).join(permission_lines)}

## Capability compliance

- Enabled: {', '.join(capability_compliance['enabled']) or 'none'}
- Consent required: **{'Yes' if capability_compliance['requiresConsent'] else 'No'}**
- Purchase authority: {authority_note or 'MISSING — release is blocked'}

## Human checks required before upload

{contact_todo}
{privacy_todo}
- TODO-HUMAN: Confirm the data-safety answers and permission list against the release AAB and all bundled SDKs.
- TODO-HUMAN: {'Replace synthetic screenshots with real captures and regenerate the pack.' if synthetic else 'Inspect every captured screen for personal or test data.'}
- TODO-HUMAN: Complete the Play Console legal declarations and first-app setup.

Version reviewed: `{meta['version_name']}` (code `{meta['version_code'] or 'not declared'}`)

{REVIEW_NOTES_MARKER}
"""
    write_preserving(store / "review-notes.md", review, REVIEW_NOTES_MARKER)

    # The release screen's two fields, seeded into the pack. Play asks for them at upload time,
    # which is the worst moment to be composing them: the release name must agree with the bundle
    # already uploaded, and each note has a 500-character limit that is only visible when the paste
    # is rejected. Seeding the name from the Gradle version means it agrees by construction, and the
    # tagged block below is Play's own multi-language format, copied whole into the one text box the
    # console shows — so the human pastes rather than transcribes.
    tagged = "\n".join(
        f"<{locale}>\n{RELEASE_NOTE_UNFINISHED}: "
        f"{RELEASE_NOTE_PROMPTS.get(locale, RELEASE_NOTE_PROMPTS['en-US'])}\n</{locale}>"
        for locale in locales
    )
    release_notes = f"""# Release notes — {listing['appName']}

The release screen asks for two things no other pack document carries.

## Release name

An internal Play Console label. It is never shown to a user; it exists so the console's release
list can tell one build from another. Play prefills it from the bundle's `versionName`, so it must
agree with what was uploaded, and this line is checked against `dist/release-manifest.json`.

**Use:** `{meta['version_name']} ({meta['version_code'] or 'not declared'})`

## What's new

Unlike the release name, this **is** shown on the store page. Play puts every language in a **single
text box** and separates them with a tag whose name is the language, the tags on their own lines:

```text
{tagged}
```

Select everything inside the block above and paste it into Play's release-notes box as one piece —
this is Play's own format, not a representation of it, so there is nothing to reassemble. Play caps
each language at {MAX_RELEASE_NOTE_CHARS} characters and truncates silently, so every note in the
block is measured when the pack is validated. Replace each `{RELEASE_NOTE_UNFINISHED}` line with the
real note, keeping its `<{locales[0]}>` and `</{locales[0]}>` lines exactly where they are; a
language with no real note yet can simply keep a short one or be left out of the block.

{RELEASE_NOTES_MARKER}
"""
    write_preserving(store / "release-notes.md", release_notes, RELEASE_NOTES_MARKER)

    # The console sheet is written rather than preserved: it is a copy of what the pack already says,
    # so a hand edit could only make it disagree with the pack it came from. It has to be written
    # *after* release-notes.md, because it embeds that file's paste block and its release name, and
    # the name it states is the one `validate_release_notes` reads back off disk.
    written_name, _written_notes = parse_release_notes(store / "release-notes.md")
    sheet_name = written_name or f"{meta['version_name']} ({meta['version_code'] or 'not declared'})"
    (store / "console-answers.md").write_text(
        console_answers_document(store, listing, locales, capability_compliance, sheet_name),
        encoding="utf-8",
    )

    status = "synthetic — replace with real captures before publish" if synthetic else "framed from real device captures"
    # A slot with store/studio.json has its media rendered by scripts/store-studio.py, which
    # composes marketing images rather than pasting the capture into a rounded rectangle. The
    # generic inventory below would describe a pack that no longer exists - it was describing
    # RGBA-vs-RGB and "framed captures" for an asset set that is composed - so the inventory is
    # written by the same condition that decides which renderer runs.
    studio_rendered = (store / "studio.json").is_file()
    if studio_rendered:
        status = "real device captures, composed by `scripts/store-studio.py`"
        provenance = f"""## How these are produced

`scripts/store-studio.py` renders all of them. The screen inside every mock-up is a real capture, taken
by `scripts/media-capture.mjs` from `journey.json` and stored under `captures/raw/`; the studio adds the
branded field, the device bezel, and the headline. It invents no UI and never redraws the captured
screen. Re-run it after changing the palette, or after a new capture:
`python scripts/store-studio.py --slot {listing['slug']}`.

## What build these captures show

`scripts/media-capture.mjs` will not publish unless the installed package is the release artifact. It
walks HEAD -> `dist/release-manifest.json` -> the recorded sha256 of the release APK -> that APK file on
disk -> the installed `base.apk`, and requires every link to match; any missing or mismatched link stops
the run before anything is published or rendered.

The result is recorded in `store/provenance.json`, which is committed and lists the sha256 of each
composed asset above. That record is the durable evidence, because both `captures/` and `dist/` are
excluded from version control: the images here survive in git, their inputs do not. If
`store/provenance.json` is absent, or names a different commit, these images are **not attested to any
build** - re-run `node scripts/media-capture.mjs --slot {listing['slug']}` against a built release to
restore the link.

## Large-screen assets are framed phone captures

The tablet assets are framed from the **phone** captures rather than captured on a tablet device: no
tablet AVD exists on this machine. Play treats large-screen assets as a claim about the large-screen
experience, so the distinction is stated here rather than passed off as a tablet capture.

The tablet canvases are 9:16, the portrait ratio the Play Console states in the 7-inch and 10-inch
screenshot fields ("Соотношение сторон изображения должно составлять 16:9 или 9:16"), so the phone
canvas and the 7-inch canvas are now the same size. That follows the console's own rule rather than
declining it, and the reason the old 5:8 canvases changed is recorded in the repository `AGENTS.md`.
"""
    else:
        provenance = """Every locale folder also contains its own icon and feature graphic. The feature graphic keeps titles and taglines inside a generous safe area because Play may crop edges on some surfaces.
"""
    # The rows are derived from SCREEN_TARGETS rather than typed beside it: a table listing sizes
    # that the renderer no longer writes is a document that lies about the pack.
    screenshot_labels = {"phone": "phone", "tablet-7": "7-inch tablet", "tablet-10": "10-inch tablet"}
    screenshot_rows = "\n".join(
        f"| `assets/<locale>/{kind}-01..{count:02d}.png` | {dimensions[0]}×{dimensions[1]} | "
        f"RGB {screenshot_labels[kind]} screenshots |"
        for kind, (count, dimensions) in SCREEN_TARGETS.items()
    )
    graphics = f"""# Graphics inventory — {listing['appName']}

Screenshot source status: **{status}**.

| Asset | Dimensions | Format / purpose |
|---|---:|---|
| `assets/icon-512.png` | 512×512 | RGBA Play store icon; full-bleed and opaque, no transparent padding |
| `assets/feature-graphic-1024x500.png` | 1024×500 | RGB feature graphic |
{screenshot_rows}

{provenance}
Four screenshots are supplied for both tablet formats. This satisfies the large-format eligibility asset count; eligibility also depends on the app's actual large-screen quality and Play requirements at submission time.
"""
    (store / "graphics.md").write_text(graphics, encoding="utf-8")

    checklist_lines = [
        f"# Google Play upload checklist — {listing['appName']}",
        "",
        f"- [ ] Run `python scripts/store-kit.py --slot {listing['slug']} --validate-only` and resolve every FAIL.",
        f"- [ ] {'Replace synthetic screenshots with real captures and regenerate the pack.' if synthetic else 'Inspect every captured screen for personal or test data.'}",
        "- [ ] Compare listing claims and captions with the release build.",
        "- [ ] Replace placeholder contact email and privacy-policy URL in `listing.json`.",
        "- [ ] Host `privacy.md` at the HTTPS URL declared in `listing.json`.",
        "- [ ] Audit permissions and third-party SDK behavior against `data-safety.md`.",
        "- [ ] Build and locally test the signed release AAB from `dist/`.",
        "- [ ] Enrol in Play App Signing and keep upload credentials outside the repository.",
        "- [ ] Complete Ads, Content rating, Target audience, App access, and Data safety declarations.",
        "- [ ] Upload phone, 7-inch tablet, and 10-inch tablet assets for every locale.",
        "- [ ] Verify edge-to-edge, orientation, text scale, and tablet layouts on representative devices.",
        "- [ ] For a brand-new package, upload the first AAB and complete legal declarations manually in Play Console.",
    ]
    # A billing app is blocked at first launch by console state no other document mentions: the
    # product itself, its purchase option, and the tester's license-testing entry. The steps are
    # generated from the IDs the app actually declares, and only for a slot that sells something -
    # a non-billing slot must not be handed steps about products it does not sell.
    if "billing" in capability_compliance.get("enabled", []):
        checklist_lines += billing_checklist_lines(declared_product_ids(store.parent))
    checklist_lines += [
        f"- [ ] Use `node scripts/play-upload.mjs --slot {listing['slug']} --track internal --dry-run` before any live upload.",
        "- [ ] Obtain explicit human approval before committing a Play edit.",
    ]
    (store / "checklist.md").write_text("\n".join(checklist_lines) + "\n", encoding="utf-8")


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
    """Refuse: this used to be a second, unverified capture path.

    It drove the device itself and wrote straight into `captures/raw/` with no `pm clear`, no IME
    handling, no `clear` step support, and - decisively - no verification of what it had
    photographed, after which `main` rendered the pack from those captures unconditionally. That
    made it possible to ship a store pack built from a screen nobody had checked, which is the
    exact failure `scripts/media-capture.mjs` exists to prevent. Two implementations of one
    journey meant the verified one could be bypassed, so this one is gone.
    """
    raise StoreKitError(
        "--capture is retired: it photographed the device without verifying the result. Use "
        f"'node scripts/media-capture.mjs --slot {listing.get('slug', '<slug>')}', which asserts "
        "each shot's expected readings before capturing and refuses to re-render the assets when "
        "a shot does not match."
    )
    return False  # unreachable; keeps the historic signature honest


def render_all(store: Path, listing: dict[str, Any], locales: list[str], meta: dict[str, str]) -> None:
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


def load_studio_renderer(store: Path) -> Callable[[Path], None] | None:
    """Return the studio's slot renderer when this slot opted in, else ``None``.

    The studio lives in `scripts/store-studio.py`, whose hyphen makes it un-importable by name, so
    it is loaded from its path. A failure here is raised rather than reported: a slot that declares
    `studio.json` and silently got the generic renderer back would get generic assets while its
    `graphics.md` describes composed ones, which is a claim rendered untrue. Failing closed leaves
    the previous assets in place instead.
    """
    if not (store / "studio.json").is_file():
        return None
    studio_path = Path(__file__).resolve().parent / "store-studio.py"
    if not studio_path.is_file():
        raise StoreKitError(f"{store / 'studio.json'} is declared but {studio_path} does not exist")
    spec = importlib.util.spec_from_file_location("store_studio", studio_path)
    if spec is None or spec.loader is None:
        raise StoreKitError(f"cannot load {studio_path}")
    module = importlib.util.module_from_spec(spec)
    try:
        spec.loader.exec_module(module)
    except Exception as error:  # noqa: BLE001 - any load failure must stop the render
        raise StoreKitError(f"{studio_path} failed to load: {error!r}") from error

    def render(slot_dir: Path) -> None:
        module.render_slot(slot_dir)

    return render


def aspect_ratio_failures(kind: str, size: tuple[int, int]) -> list[str]:
    """The console's aspect sentence for screenshot ``kind``, or no failures.

    Every screenshot field in the console carries the same inline rule -
    "Соотношение сторон изображения должно составлять 16:9 или 9:16" - so this applies to phone,
    7-inch and 10-inch alike. It is what makes a ``max_ratio`` of None mean "no extra 2x cap"
    instead of "unchecked".

    The comparison cross-multiplies, so an exactly 9:16 canvas is decided by integers and a float
    can never turn a pixel-perfect 1080x1920 into a near miss.
    """
    width, height = size
    for across, down in SCREENSHOT_ASPECT_RATIOS:
        if width * down == height * across or width * across == height * down:
            return []
    allowed = " or ".join(f"{across}:{down}" for across, down in SCREENSHOT_ASPECT_RATIOS)
    return [f"{kind}: {width}x{height} is not {allowed}"]


def screenshot_rule_failures(kind: str, size: tuple[int, int]) -> list[str]:
    """Return the Play rule violations for a screenshot of ``kind`` and ``size``.

    The limits are per kind because the console states a different size window per screenshot
    field; the aspect rule is the same for every field and is asserted here so it cannot be
    skipped by a kind whose ``max_ratio`` is None. See ``SCREENSHOT_LIMITS``.
    """
    limits = SCREENSHOT_LIMITS.get(kind)
    if limits is None:
        raise StoreKitError(f"unknown screenshot kind: {kind!r}")
    short, long = sorted(size)
    failures: list[str] = list(aspect_ratio_failures(kind, size))
    min_short = limits["min_short"]
    max_long = limits["max_long"]
    if min_short is not None and short < min_short:
        failures.append(f"{kind}: short side {short}px is below the {min_short}px minimum")
    if max_long is not None and long > max_long:
        failures.append(f"{kind}: long side {long}px exceeds the {max_long}px maximum")
    ratio = limits["max_ratio"]
    if ratio is not None and long > ratio * short:
        failures.append(
            f"{kind}: long side {long}px is more than {ratio}x the short side {short}px"
        )
    return failures


def inspect_image(
    path: Path,
    expected: tuple[int, int],
    mode: str,
    *,
    max_bytes: int = MAX_PNG_BYTES,
    screenshot_kind: str | None = None,
    require_nontransparent_corners: bool = False,
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
            if require_nontransparent_corners and image.mode == "RGBA":
                alpha = image.getchannel("A")
                corners = ((0, 0), (image.width - 1, 0), (0, image.height - 1), (image.width - 1, image.height - 1))
                if any(alpha.getpixel(corner) == 0 for corner in corners):
                    failures.append("contains fully transparent corner pixels; drawn rounded masks are forbidden")
            if screenshot_kind is not None:
                failures.extend(screenshot_rule_failures(screenshot_kind, image.size))
    except OSError as exc:
        failures.append(f"unreadable image: {exc}")
    return failures


PROVENANCE_DEFERRED_NOTE = (
    "UNVERIFIED provenance — this run skipped the provenance check, so the assets are NOT attested "
    "here and this is not a passing result. scripts/media-capture.mjs passes --skip-provenance because "
    "the record on disk describes the run it is about to replace, and it follows with a real "
    "--validate-only pass. Invoking the flag directly leaves the pack unverified."
)


_COMMIT_RE = re.compile(r"^[0-9a-fA-F]{40}$")
_SHA256_RE = re.compile(r"^[0-9a-fA-F]{64}$")


def _asset_key(relative: str) -> str:
    """Normalise a slot-relative asset path so set comparison is separator- and prefix-stable."""
    return relative.replace("\\", "/").lstrip("./")


def expected_composed_assets(locales: list[str]) -> list[str]:
    """Every composed asset the pack is required to ship, as slot-relative paths.

    Derived from `SCREEN_TARGETS` (the single source of truth for the screenshot inventory) plus the
    shared icon and feature graphic at the pack root and once per locale, so a hardcoded count here
    cannot drift from the renderer.
    """
    expected = ["store/assets/icon-512.png", "store/assets/feature-graphic-1024x500.png"]
    for locale in locales:
        expected += [
            f"store/assets/{locale}/icon-512.png",
            f"store/assets/{locale}/feature-graphic-1024x500.png",
        ]
        for kind, (count, _dimensions) in SCREEN_TARGETS.items():
            for index in range(1, count + 1):
                expected.append(f"store/assets/{locale}/{kind}-{index:02d}.png")
    return expected


def expected_asset_counts(locales: list[str]) -> str:
    """Human-readable `shared=4 phone=6 tablet7=4 tablet10=4 total=18` for the expected pack."""
    shared = 2 + 2 * len(locales)
    parts = [f"shared={shared}"]
    parts += [f"{kind.replace('-', '')}={count}" for kind, (count, _dimensions) in SCREEN_TARGETS.items()]
    total = shared + sum(count for count, _dimensions in SCREEN_TARGETS.values()) * len(locales)
    parts.append(f"total={total}")
    return " ".join(parts)


def commit_exists(commit: str) -> bool:
    """True when `commit` names a real commit object in this repository.

    `git cat-file -e` is authoritative for existence and rejects a well-formed but nonexistent sha,
    which a plain format check cannot. It runs against the factory repository (`ROOT`) because the
    record's commit refers to PlayKiln, not to whatever directory a slot copy lives in.
    """
    try:
        result = subprocess.run(
            ["git", "cat-file", "-e", f"{commit}^{{commit}}"],
            cwd=ROOT,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
    except OSError:
        return False
    return result.returncode == 0


def verify_provenance_identity(
    slot_dir: Path, record: dict[str, Any]
) -> tuple[list[str], list[str], list[str]]:
    """Authenticate the record's build-identity fields to the extent a device-less run can.

    Returns `(failures, checked, unchecked)`. `checked` names the links confirmed in this run;
    `unchecked` names the links that only a capture can verify, because they need the release artifact
    tree (not version-controlled) or `adb`. No device-less substitute is invented for
    `installedSha256`: an installed `base.apk` cannot be re-hashed without `adb`, so it is reported
    rather than faked, and a reader can never mistake a partial local check for a full one.
    """
    failures: list[str] = []
    checked: list[str] = []
    unchecked: list[str] = []

    commit = str(record.get("gitCommit") or "").strip()
    if not _COMMIT_RE.match(commit):
        failures.append(
            f"gitCommit is not a full 40-hex commit sha (recorded {commit[:12]!r}), so the record is "
            "not tied to any source revision"
        )
    elif not commit_exists(commit):
        failures.append(f"gitCommit {commit[:12]} does not exist in this repository")
    else:
        checked.append(f"gitCommit {commit[:12]}")

    artifact_rel = str(record.get("artifactPath") or "").strip()
    recorded_artifact = str(record.get("artifactSha256") or "").lower()
    if not _SHA256_RE.match(recorded_artifact):
        failures.append(
            f"artifactSha256 is not a full 64-hex sha256 (recorded {recorded_artifact[:12]!r}), so the "
            "record claims no hashed artifact"
        )
    elif not artifact_rel:
        failures.append("artifactPath is absent, so artifactSha256 names nothing to compare against")
    else:
        artifact_path = slot_dir / artifact_rel
        if artifact_path.is_file():
            actual = hashlib.sha256(artifact_path.read_bytes()).hexdigest()
            if actual != recorded_artifact:
                failures.append(
                    f"{artifact_rel} hashes to {actual[:12]}… but provenance.json records "
                    f"{recorded_artifact[:12]}…"
                )
            else:
                checked.append(f"artifactSha256 {recorded_artifact[:12]}… == {artifact_rel}")
        else:
            unchecked.append(
                f"artifactSha256 (dist/ is not version-controlled and {artifact_rel} is absent here)"
            )

    installed = str(record.get("installedSha256") or "").lower()
    if not _SHA256_RE.match(installed):
        failures.append(
            f"installedSha256 is not a full 64-hex sha256 (recorded {installed[:12]!r}), so the "
            "installed-APK link is not even claimed"
        )
    elif _SHA256_RE.match(recorded_artifact) and installed != recorded_artifact:
        # `verifyArtifactChain` refuses any capture where these differ, so a record whose two hashes
        # disagree contradicts itself. Comparing them needs no device and catches a forged
        # installedSha256, which a hash-string-only reading of the record would accept.
        failures.append(
            f"installedSha256 {installed[:12]}… disagrees with artifactSha256 "
            f"{recorded_artifact[:12]}…; the capture chain requires the installed base.apk to be "
            "byte-identical to the signed artifact"
        )
    else:
        unchecked.append(
            f"installedSha256 {installed[:12]}… (needs adb to re-hash the installed base.apk)"
        )

    return failures, checked, unchecked


def repo_relative(path: Path) -> str | None:
    """The path as git would name it, or None when it lies outside the repository."""
    try:
        return path.resolve().relative_to(ROOT).as_posix()
    except ValueError:
        return None


def path_is_tracked(path: Path) -> bool:
    """True when a path is COMMITTED, even if it is missing from the working tree.

    A composed slot declares itself with `store/studio.json` and records its evidence in
    `store/provenance.json`, and both are committed. Keying the provenance requirement on those
    files merely being present let `rm store/studio.json store/provenance.json` drop the entire
    check while still reporting OK - the declaration was unprotected. Git remembers what the pack
    is supposed to contain, so a tracked-but-absent file is a deletion to report, not a redesign.
    """
    relative = repo_relative(path)
    if relative is None:
        return False
    result = subprocess.run(
        ["git", "ls-files", "--error-unmatch", relative],
        cwd=ROOT, capture_output=True, text=True,
    )
    return result.returncode == 0


def validate_provenance(store: Path, locales: list[str], required: bool = False) -> list[str]:
    """Verify the committed screenshots still match the record of the build they came from.

    `store/provenance.json` is the only durable link between the committed images and the release APK,
    because `captures/` and `dist/` are excluded from version control. Checking the recorded hashes
    turns that link into something falsifiable: an image edited, replaced, or re-rendered without a
    fresh capture no longer matches, instead of silently keeping the previous claim.

    The check is not merely self-consistency between the record and itself. Hashing only the entries
    the record happens to list would let an attacker trim the list, so the record must also cover
    EXACTLY the assets the pack is required to ship (derived from `SCREEN_TARGETS`), in both
    directions: a missing expected asset, an unexpected extra entry, and an on-disk asset the record
    omits are each failures. The record's identity fields are authenticated too, as far as a
    device-less run allows; a link that cannot be re-checked here is named rather than assumed.

    `required` is set for slots that compose their media (`store/studio.json`), which are exactly the
    slots whose inventory claims captured screenshots. For those, an absent record is a failure rather
    than a note: the pack asserts something about a build and has nothing to show for it. A slot with
    no captures is not required to have a record.

    A row this check prints is a statement about bytes, not about the renderer: `<file> does not match
    its recorded sha256` means the image on disk is not the image the record describes. The way that
    happens without anyone editing an image is a **studio re-render**, and the way out is not to relax
    this check but to re-capture: `scripts/store-studio.py` writes the same assets `media-capture.mjs`
    records, so a re-render replaces the bytes while `store/provenance.json` keeps the previous run's
    hashes until a capture re-records them. That renderer also writes the adaptive-icon vectors into
    `app/src/main/res/**`, which are build inputs — see the note above
    `EXCLUDED_ROOT_DIRECTORY_NAMES` in `scripts/lib/release-approval.mjs` for the approval half of the
    same hazard. Run the render after the capture, never between the capture and the approval.
    """
    path = store / "provenance.json"
    if not path.is_file():
        if not required:
            print("NOTE provenance.json — absent; this pack makes no capture-attestation claim")
            return []
        message = (
            "provenance.json is absent, so these composed assets are not attested to any release build; "
            "run node scripts/media-capture.mjs --slot <slug> against a built release"
        )
        print(f"FAIL provenance.json — {message}")
        return [message]
    try:
        record = json.loads(path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError) as error:
        print(f"FAIL provenance.json - unreadable: {error}")
        return [f"provenance.json unreadable: {error}"]

    slot_dir = store.parent
    failures: list[str] = []
    for group in ("composedAssets", "rawCaptures"):
        entries = record.get(group)
        if not isinstance(entries, list) or not entries:
            failures.append(f"provenance.json has no usable {group} list")
            print(f"FAIL provenance.json - no usable {group} list")
            continue
        for entry in entries:
            relative = str(entry.get("file") or "")
            target = slot_dir / relative
            if not target.is_file():
                # Raw captures live under the ignored captures/ tree, so their absence is expected on a
                # clean checkout; the composed assets are committed and must be present.
                if group == "rawCaptures":
                    continue
                failures.append(f"{relative} is named in provenance.json but missing")
                print(f"FAIL provenance.json - {relative} missing")
                continue
            actual = hashlib.sha256(target.read_bytes()).hexdigest()
            if actual != str(entry.get("sha256") or "").lower():
                failures.append(f"{relative} does not match its recorded sha256")
                print(f"FAIL provenance.json - {relative} does not match its recorded sha256 ({actual[:12]}…)")

    # Completeness. The hashes above only prove the record agrees with itself; the entry list is
    # attacker-trimmable, so an asset swapped while its entry was deleted passed. The record must cover
    # EXACTLY the assets the pack is required to ship, in both directions.
    composed_entries = record.get("composedAssets")
    if isinstance(composed_entries, list) and composed_entries:
        expected = {_asset_key(item) for item in expected_composed_assets(locales)}
        recorded = {_asset_key(str(entry.get("file") or "")) for entry in composed_entries}
        for missing in sorted(expected - recorded):
            failures.append(f"required asset {missing} is not recorded in provenance.json")
            print(f"FAIL provenance.json - required asset {missing} is not recorded")
        for extra in sorted(recorded - expected):
            failures.append(f"provenance.json lists {extra}, which is not part of the expected pack")
            print(f"FAIL provenance.json - {extra} is not part of the expected pack")
        assets_root = store / "assets"
        if assets_root.is_dir():
            for on_disk in sorted(p for p in assets_root.rglob("*.png") if p.is_file()):
                key = _asset_key(on_disk.relative_to(slot_dir).as_posix())
                if key not in recorded:
                    failures.append(f"{key} is present on disk but is not recorded in provenance.json")
                    print(f"FAIL provenance.json - {key} is on disk but is not recorded")

    identity_failures, checked, unchecked = verify_provenance_identity(slot_dir, record)
    for failure in identity_failures:
        print(f"FAIL provenance.json - {failure}")
    failures.extend(identity_failures)

    if not failures:
        composed = len(composed_entries or [])
        counts = expected_asset_counts(locales)
        detail = f"checked {'; '.join(checked) if checked else 'none'}"
        if unchecked:
            detail += f"; not checked this run: {'; '.join(unchecked)}"
        print(
            f"PASS provenance.json - {composed} committed asset(s) cover the expected pack "
            f"({counts}) for commit {str(record.get('gitCommit'))[:12]} — {detail}"
        )
    return failures


def parse_release_notes(path: Path) -> tuple[str, dict[str, str]]:
    """Read `store/release-notes.md` into (release name, {locale: note text}).

    The parser reads the field the way **Play itself reads it**, which matters more than elegance:
    Play's release screen puts every language in a single text box and separates them with tags whose
    name is the locale, tags on their own lines —

        <en-US>
        First release.
        </en-US>

    Play's own instruction is "Add the description for your app's release between the relevant
    language tags ... make sure the language tags are on separate lines from the release notes"
    (<https://support.google.com/googleplay/android-developer/answer/9859348?hl=en>). So the fenced
    block in this file is not a rendering of the notes; it *is* the notes, character for character,
    and what is measured here is what gets pasted. An earlier revision of this parser read
    `### <locale>` sections instead, which meant a human had to reassemble Play's format by hand —
    the exact friction this file exists to remove, and a chance for the measured text and the pasted
    text to differ.

    Exactly one copy of each note lives in the file, so the count cannot disagree with the paste.
    """
    text = read_text(path)
    name = ""
    if match := re.search(r"^\*\*Use:\*\*\s*`([^`]+)`", text, re.MULTILINE):
        name = match.group(1).strip()
    notes: dict[str, str] = {}
    fenced = re.search(r"^```[^\n]*\n(.*?)^```", text, re.MULTILINE | re.DOTALL)
    if fenced:
        # A tag must occupy its own line, which is what Play requires and what the anchors enforce.
        # An inline tag therefore does not silently pass: it is simply not found, and the locale is
        # reported missing with the expected format in the message.
        notes = {
            locale: body.strip()
            for locale, body in re.findall(
                r"^<([A-Za-z][A-Za-z0-9-]*)>[ \t]*$\n(.*?)^</\1>[ \t]*$",
                fenced.group(1),
                re.MULTILINE | re.DOTALL,
            )
        }
    return name, notes


def validate_release_notes(store: Path, release_name: str, notes: dict[str, str], locales: list[str]) -> list[str]:
    """Check the release-name and release-note fields against Play's rules.

    Two independent things are measured, and they fail for different reasons:

    - **The name agrees with the bundle.** Play prefills the release name from `versionName`, and
      the console list is how a human tells one build from another, so a name that disagrees with
      `dist/release-manifest.json` makes the list lie — which is how the wrong rollout gets promoted.
      Checked only when a manifest exists, because during development there is no build yet.
    - **The notes fit.** 500 characters per language, measured on the exact string a human will
      paste, because a silently truncated release note is a claim the pack did not make.
    """
    failures: list[str] = []
    if not release_name:
        failures.append("release-notes.md states no release name (expected a `**Use:** ``...`` line)")
    if not notes:
        failures.append(
            "release-notes.md carries no pasted release notes (expected a fenced block of "
            "`<locale>`-tagged notes, tags on their own lines)"
        )
    for locale in locales:
        if not notes.get(locale):
            failures.append(
                f"release-notes.md has no `{locale}` note in the paste block "
                f"(expected a `<{locale}>` line, the note, then `</{locale}>`)"
            )
    # Every section the file states is measured, not only the listing's own locales: a section that
    # exists is text a human may paste, so an over-long one would hand them a note Play truncates.
    for locale, note in sorted(notes.items()):
        if len(note) > MAX_RELEASE_NOTE_CHARS:
            failures.append(
                f"release-notes.md {locale} note is {len(note)} chars; Play limit is {MAX_RELEASE_NOTE_CHARS}"
            )
    # A note still carrying the generator's prompt is not a release note. Reported rather than
    # failed, matching how an unfinished placeholder address is reported: the human's next action is
    # to write the text, and failing here would make a freshly scaffolded pack unusable for no gain.
    unfinished = sorted(locale for locale, note in notes.items() if RELEASE_NOTE_UNFINISHED in note)
    if unfinished:
        print(
            f"WARN release-notes.md — {', '.join(unfinished)} still carry the generated prompt; "
            "write the real note before upload"
        )
    manifest_path = store.parent / "dist" / "release-manifest.json"
    if release_name and manifest_path.is_file():
        try:
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        except (OSError, ValueError):
            manifest = {}
        version_name = str(manifest.get("versionName") or "")
        version_code = str(manifest.get("versionCode") or "")
        if version_name and version_code:
            expected = f"{version_name} ({version_code})"
            if release_name != expected:
                failures.append(
                    f"release-notes.md release name is {release_name!r} but the built bundle is "
                    f"{expected!r} ({manifest_path.parent.name}/release-manifest.json)"
                )
    return failures


def validate_store(
    store: Path, listing: dict[str, Any], locales: list[str], skip_provenance: bool = False
) -> bool:
    failures = validate_listing(listing, locales)
    if failures:
        for failure in failures:
            print(f"FAIL listing.json — {failure}")
    else:
        print("PASS listing.json — schema=1 and Play character limits satisfied")

    # --- The pack's documents ----------------------------------------------------------------------
    # The inventory is the model's (`packDocuments`), not a tuple written here, because two gates read
    # it: this one and `scripts/verify-slot.mjs`. They disagreed while each carried its own copy —
    # `store/console-answers.md` was absent from a shipped slot's pack and this function printed
    # `OK` anyway, because the tuple above did not name it. A contract-listed deliverable that no
    # check can see is the same as one that was never required, so absence is a FAIL unless the model
    # itself says why that document may be missing.
    #
    # Two of the documents carry claims of their own — the release screen's two fields, and the
    # console sheet's answers — and those are held to what the pack says now rather than merely
    # counted, because a file that exists and contradicts the pack is the defect the check is for.
    release_notes_path = store / "release-notes.md"
    sheet_name = ""
    if release_notes_path.is_file() and release_notes_path.stat().st_size:
        sheet_name, _sheet_notes = parse_release_notes(release_notes_path)
    for entry in pack_documents():
        name = str(entry["name"])
        path = store / name
        if not path.is_file() or not path.stat().st_size:
            if str(entry.get("absence")) == "fail":
                print(f"FAIL {name} — missing or empty")
                failures.append(f"missing or empty {name}")
            else:
                print(f"WARN {name} — absent; {entry['absenceEn']}")
            continue
        if name == "release-notes.md":
            # An over-long note is text a human pastes and Play silently truncates, and a release name
            # that disagrees with the bundle makes the console list lie about which build is which.
            release_name, notes = parse_release_notes(path)
            notes_failures = validate_release_notes(store, release_name, notes, locales)
            if notes_failures:
                for failure in notes_failures:
                    print(f"FAIL release-notes.md — {failure}")
                failures.extend(notes_failures)
            else:
                measured = ", ".join(
                    f"{locale} {len(notes[locale])}/{MAX_RELEASE_NOTE_CHARS}" for locale in sorted(notes)
                )
                print(f"PASS release-notes.md — release name {release_name!r}, notes {measured} chars")
        elif name == "console-answers.md":
            # The sheet is a copy of the pack, so a sheet left behind by an older pack cannot pass as
            # current: every claim in its block is compared against what the pack says now.
            answers_failures = validate_console_answers(store, listing, locales, sheet_name)
            if answers_failures:
                for failure in answers_failures:
                    print(f"FAIL console-answers.md — {failure}")
                failures.extend(answers_failures)
            else:
                sheet_claims = parse_console_claims(path)
                print(
                    "PASS console-answers.md — "
                    f"locales {', '.join(sheet_claims.get('locales') or []) or 'none'}, "
                    f"credentials answer {sheet_claims.get('credentialsAnswer')!r}, "
                    f"{len(sheet_claims.get('productIds') or [])} product ID(s) corroborated against the pack"
                )
        else:
            print(f"PASS {name} — {path.stat().st_size} bytes")

    # A billing app is blocked at first launch by console state no other check reads, and the
    # checklist is generated from the IDs the slot's own Kotlin declares. A billing slot that
    # declares none therefore ships a checklist that cannot name the product the reviewer's session
    # needs, and the purchase flow that references an unknown product fails there - which is the
    # comment the slot's own `MonetizationConfig.kt` already carries. Absence is a failure, not a
    # note: this is the money path, and the fix is to declare the product in the terminal.
    compliance = listing.get("capabilityCompliance") or {}
    if "billing" in compliance.get("enabled", []):
        product_ids = declared_product_ids(store.parent)
        if product_ids:
            print(f"PASS billing — slot declares {', '.join(product_ids)} to the Play Billing Library")
        else:
            failure = (
                f"billing is enabled but no {PRODUCT_ID_DECLARATION.pattern} declaration exists under "
                f"apps/{store.parent.name}/app, so no purchase can succeed and the checklist cannot "
                "name the product(s) Play Console must have"
            )
            print(f"FAIL billing — {failure}")
            failures.append(failure)

    specs: list[tuple[Path, tuple[int, int], str, str | None, bool, int]] = [
        (store / "assets" / "icon-512.png", (512, 512), "RGBA", None, True, MAX_ICON_BYTES),
        (store / "assets" / "feature-graphic-1024x500.png", (1024, 500), "RGB", None, False,
         MAX_FEATURE_BYTES),
    ]
    for locale in locales:
        base = store / "assets" / locale
        specs += [
            (base / "icon-512.png", (512, 512), "RGBA", None, True, MAX_ICON_BYTES),
            (base / "feature-graphic-1024x500.png", (1024, 500), "RGB", None, False,
             MAX_FEATURE_BYTES),
        ]
        for kind, (count, dimensions) in SCREEN_TARGETS.items():
            for index in range(1, count + 1):
                specs.append(
                    (base / f"{kind}-{index:02d}.png", dimensions, "RGB", kind, False, MAX_PNG_BYTES)
                )
    for path, dimensions, mode, screenshot_kind, require_nontransparent_corners, max_bytes in specs:
        image_failures = inspect_image(
            path,
            dimensions,
            mode,
            max_bytes=max_bytes,
            screenshot_kind=screenshot_kind,
            require_nontransparent_corners=require_nontransparent_corners,
        )
        relative = path.relative_to(store).as_posix()
        if image_failures:
            print(f"FAIL {relative} — {'; '.join(image_failures)}")
            failures.extend(f"{relative}: {failure}" for failure in image_failures)
        else:
            size_bytes = path.stat().st_size
            print(f"PASS {relative} — {dimensions[0]}x{dimensions[1]} {mode}, {size_bytes} bytes")
    if not skip_provenance:
        studio_path = store / "studio.json"
        record_path = store / "provenance.json"
        # Composed is a property of the SLOT, not of which files happen to survive on disk. A
        # committed declaration or record keeps the check required, so deleting them cannot quietly
        # downgrade the pack to "no captures to attest".
        composed = (
            studio_path.is_file()
            or record_path.is_file()
            or path_is_tracked(studio_path)
            or path_is_tracked(record_path)
        )
        if path_is_tracked(studio_path) and not studio_path.is_file():
            failures.append("store/studio.json is committed but missing from the working tree")
            print(
                "FAIL studio.json - committed but missing; the pack's declared visual identity "
                "was deleted, and `graphics.md` still describes composed assets"
            )
        failures.extend(validate_provenance(store, locales, required=composed))
    return not failures


def summary(slot: Path, locales: list[str]) -> str:
    # The sizes are derived from SCREEN_TARGETS so this verdict line cannot keep advertising a
    # canvas the renderer no longer writes.
    canvases = " ".join(
        f"{kind.replace('-', '')}={dimensions[0]}x{dimensions[1]}"
        for kind, (_count, dimensions) in SCREEN_TARGETS.items()
    )
    counts = " ".join(
        f"{kind.replace('-', '')}={count}shot" for kind, (count, _dimensions) in SCREEN_TARGETS.items()
    )
    return (
        f"STORE-KIT: OK apps/{slot.name}/store "
        f"(icon=512x512 feature=1024x500 {counts} {canvases} locales={','.join(locales)})"
    )


def harden_streams() -> None:
    """Make a printed verdict survive the terminal's codepage.

    The rejection reasons this tool prints quote the console's own Russian labels - a paraphrase is
    caught by comparing the label the model carries with the label the file claims, and the message
    has to show both. On Windows the default stream encoding is a legacy codepage (cp1251 on the
    machine that found this), which raised UnicodeEncodeError on a `→` and printed `?????` for the
    Russian text: a rejection reason nobody can read is not a reason. An unencodable character now
    degrades to a replacement character instead of killing the run, and the same call keeps
    `scripts/test-console-answers.py` consistent with the CLI it exercises.
    """
    for stream in (sys.stdout, sys.stderr):
        with contextlib.suppress(AttributeError, ValueError):
            stream.reconfigure(errors="replace")


def main(argv: list[str] | None = None) -> int:
    harden_streams()
    args = parser().parse_args(argv)
    if args.observations is not None:
        # The observation report is a factory-wide tool: the files describe the console, not one slot,
        # and it reads the model rather than a pack, so no slot is resolved and no pack is touched.
        if args.init or args.capture or args.render_only or args.validate_only:
            raise StoreKitError(
                "--observations cannot be combined with --init, --capture, --render-only, or --validate-only"
            )
        target = Path(args.observations)
        if not target.is_absolute():
            target = ROOT / target
        return observation_report(target, apply=args.apply_observations)
    if args.apply_observations:
        raise StoreKitError("--apply-observations is only read together with --observations")
    if args.init and (args.capture or args.render_only or args.validate_only):
        raise StoreKitError("--init cannot be combined with --capture, --render-only, or --validate-only")
    if args.capture and args.render_only:
        raise StoreKitError("--capture and --render-only are mutually exclusive")
    slot = resolve_slot(args)
    # Producing the store assets is a release action. Check before anything is written, so a
    # refusal leaves the slot exactly as it was; `--init` and `--validate-only` are the
    # unapproved-slot routes and stay open.
    if not (args.init or args.validate_only or args.capture):
        approval_problem = release_approval_block(slot)
        if approval_problem:
            raise StoreKitError(approval_problem)
    meta = slot_metadata(slot)
    capability_config = slot / "capabilities.json"
    if not args.validate_only and not capability_config.is_file():
        write_json(capability_config, {"schemaVersion": 1, "enabled": []})
    compliance = capability_compliance(slot)
    store = slot / "store"
    listing_path = store / "listing.json"

    existing = load_listing(listing_path) if listing_path.is_file() else None
    locales = parse_locales(args.locales, existing)
    if args.init:
        listing = default_listing(meta, locales, compliance)
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
        existing = default_listing(meta, locales, compliance)
        write_json(listing_path, existing)
        print(f"WARN: listing.json was missing; initialized draft before generation")
    listing = existing
    if not args.validate_only:
        refresh_idea_from_record(listing, meta, compliance)
    merge_missing_locales(listing, meta, locales, compliance)
    apply_capability_compliance(listing, compliance)
    preflight = validate_listing(listing, locales)
    if preflight:
        raise StoreKitError("listing validation failed:\n  - " + "\n  - ".join(preflight))
    if not args.validate_only:
        write_json(listing_path, listing)

    if args.validate_only:
        ok = validate_store(store, listing, locales, skip_provenance=args.skip_provenance)
        if not ok:
            return 1
        if args.skip_provenance:
            print(PROVENANCE_DEFERRED_NOTE)
            return 2
        print(summary(slot, locales))
        return 0

    if args.capture:
        capture_with_adb(store, listing)
    render_all(store, listing, locales, meta)
    ok = validate_store(store, listing, locales, skip_provenance=args.skip_provenance)
    if not ok:
        return 1
    if args.skip_provenance:
        print(PROVENANCE_DEFERRED_NOTE)
        return 2
    print(summary(slot, locales))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except StoreKitError as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        raise SystemExit(1)
