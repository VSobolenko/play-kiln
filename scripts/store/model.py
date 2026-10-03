#!/usr/bin/env python3
"""The store pack's data model: the only place a pack path and a Play limit are resolved.

Every numeric window, byte cap and character limit comes from
``scripts/lib/play-assets.json`` -- the single source of truth shared with
``scripts/lib/play-assets.mjs`` -- and is loaded here, never restated.  A value
with two definitions drifts away from itself; see
``.cursor/rules/verification-discipline.mdc`` ("One source of truth, imported -
never restated").

``scripts/store/validate.py`` reads a pack exclusively through this module, so a
path or a cap has exactly one definition in the factory.

Run ``python scripts/store/model.py`` to print every limit this module resolved.
"""

from __future__ import annotations

import json
import re
import sys
from collections.abc import Mapping as ABCMapping
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterator, Sequence


class PackModelError(RuntimeError):
    """A pack cannot be modelled: a required input is missing or unreadable."""


# Derived from this file's own location, so the model resolves the same paths from any cwd.
REPO_ROOT = Path(__file__).resolve().parents[2]
PLAY_ASSETS_PATH = REPO_ROOT / "scripts" / "lib" / "play-assets.json"
APPS_DIR = REPO_ROOT / "apps"


def _load_play_assets() -> dict[str, Any]:
    if not PLAY_ASSETS_PATH.is_file():
        raise PackModelError(
            f"{PLAY_ASSETS_PATH} is absent, so no Play limit has a source of truth"
        )
    try:
        loaded = json.loads(PLAY_ASSETS_PATH.read_text(encoding="utf-8-sig"))
    except (OSError, json.JSONDecodeError) as error:
        raise PackModelError(f"{PLAY_ASSETS_PATH} is unreadable: {error}") from error
    if not isinstance(loaded, dict):
        raise PackModelError(f"{PLAY_ASSETS_PATH} must hold a JSON object")
    return loaded


PLAY_ASSETS = _load_play_assets()

ICON: dict[str, Any] = PLAY_ASSETS["icon"]
FEATURE_GRAPHIC: dict[str, Any] = PLAY_ASSETS["featureGraphic"]
SCREENS: dict[str, Any] = PLAY_ASSETS["screens"]
ASPECT_RATIOS: tuple[tuple[int, int], ...] = tuple(
    (int(pair[0]), int(pair[1])) for pair in PLAY_ASSETS["aspectRatios"]
)
LISTING_LIMITS: dict[str, Any] = PLAY_ASSETS["listing"]
RELEASE_NOTE: dict[str, Any] = PLAY_ASSETS["releaseNote"]
NON_DEGENERATE: dict[str, Any] = PLAY_ASSETS["nonDegenerate"]
LOCALES: dict[str, Any] = PLAY_ASSETS["locales"]
IMAGE_TYPES: dict[str, str] = PLAY_ASSETS["imageTypes"]
PLACEHOLDER_HOSTS: tuple[str, ...] = tuple(PLAY_ASSETS["placeholderHosts"])
DEFAULT_CONTACT_EMAIL: str = PLAY_ASSETS["contactEmail"]

# The two kinds that ship at the pack root and once per locale, each a fixed canvas.
SHARED_KINDS: tuple[str, ...] = ("icon", "feature-graphic")
# The kinds whose ship count, size window and ratio cap live under `screens` in the limits file.
SCREENSHOT_KINDS: tuple[str, ...] = tuple(SCREENS)
ASSET_KINDS: tuple[str, ...] = SHARED_KINDS + SCREENSHOT_KINDS

# The limits file names one Play Developer API image type per kind; a kind it declares that this
# model does not inventory is a drift between the two, and modelled as an error rather than ignored.
_UNMODELLED_KINDS = sorted(set(IMAGE_TYPES) - set(ASSET_KINDS))
if _UNMODELLED_KINDS:
    raise PackModelError(
        f"{PLAY_ASSETS_PATH} declares image types this model does not inventory: "
        f"{', '.join(_UNMODELLED_KINDS)}"
    )

STORE_DIRNAME = "store"
ASSETS_DIRNAME = "assets"
LISTING_FILENAME = "listing.json"
PROVENANCE_FILENAME = "provenance.json"
STUDIO_FILENAME = "studio.json"

# The pack's documents, as `store/`-relative POSIX paths.
#
# `console-answers.md` is deliberately absent: the reduce-factory-to-core change drops the sheet
# generator (`requirements-preservation.md` section 14.7), so requiring it would contradict the
# refactor.  `provenance.json` is conditional on a pack composing its own media (see
# `composes_media`), and `README.md` is not part of the store-pack contract, so neither is required
# here.  A pack that carries extra documents is not an error.
PACK_DOCUMENTS: tuple[str, ...] = (
    "store/listing.json",
    "store/listing.md",
    "store/privacy.md",
    "store/data-safety.md",
    "store/review-notes.md",
    "store/release-notes.md",
    "store/graphics.md",
    "store/checklist.md",
)


# --------------------------------------------------------------------------------------------
# Limits, one accessor per shape a caller reads
# --------------------------------------------------------------------------------------------
#
# The module constants above are the values; these functions are the shapes a caller wants them in.
# A caller asking for "the icon's canvas" or "the release-note limit" gets it from here rather than
# reaching into a nested dict, so the limits file stays the only place the number is written.


def placeholder_hosts() -> tuple[str, ...]:
    """The reserved documentation hosts a finished listing must not point at."""
    return PLACEHOLDER_HOSTS


def aspect_ratios() -> tuple[tuple[int, int], ...]:
    """The console's aspect sentence as integer pairs: a screenshot must match one of them."""
    return ASPECT_RATIOS


def release_note_limit() -> int:
    """Play's per-language release-note character cap, on the string a human pastes."""
    return int(RELEASE_NOTE["maxChars"])


def icon_canvas() -> tuple[int, int]:
    """The store icon's exact canvas, ``(width, height)``."""
    return (int(ICON["width"]), int(ICON["height"]))


def icon_max_bytes() -> int:
    """The store icon's byte cap."""
    return int(ICON["maxBytes"])


def feature_graphic_canvas() -> tuple[int, int]:
    """The feature graphic's exact canvas, ``(width, height)``."""
    return (int(FEATURE_GRAPHIC["width"]), int(FEATURE_GRAPHIC["height"]))


def feature_graphic_max_bytes() -> int:
    """The feature graphic's byte cap."""
    return int(FEATURE_GRAPHIC["maxBytes"])


# --------------------------------------------------------------------------------------------
# Release notes: Play's own multi-language paste block
# --------------------------------------------------------------------------------------------
#
# Play's release screen takes every language in a **single text box**, separated by a tag whose name
# is the language and which occupies a line of its own.  The block is the paste rather than a
# rendering of it, so the characters the renderer writes, the characters the validator measures and
# the characters a human pastes are the same characters -- which is the whole reason the format and
# the per-language cap live here instead of in one of the two programs that read them.  Play shows
# release notes on the store page and **truncates silently** rather than refusing, so a note nobody
# measured is a note nobody can see was cut.
#
# Both readers import from this module: `scripts/store/render.py` assembles the block, and
# `scripts/store/validate.py` measures every tag in it -- including a language the listing does not
# declare, because text in the block is text a human may paste.

RELEASE_NOTE_FENCE_OPEN = "```text"
RELEASE_NOTE_FENCE_CLOSE = "```"

# A fenced block, exactly as `store/release-notes.md` carries it.
RELEASE_NOTE_FENCED_BLOCK = re.compile(r"^```[^\n]*\n(.*?)^```", re.MULTILINE | re.DOTALL)
# One `<locale>` tag pair, the tags on their own lines and the note between them.  The backreference
# makes an opening tag and its closer a matched pair, so a stray `<en-US>` cannot borrow another
# language's closing tag.
RELEASE_NOTE_LOCALE_TAG = re.compile(
    r"^<([A-Za-z][A-Za-z0-9-]*)>[ \t]*$\n(.*?)^</\1>[ \t]*$", re.MULTILINE | re.DOTALL
)


def release_note_locale_tag(locale: str) -> str:
    """The opening tag Play expects for ``locale``, on a line of its own."""
    return f"<{locale}>"


def release_notes_paste_block(notes: Mapping[str, str]) -> str:
    """Assemble Play's paste block: one ``<locale>`` tag pair per language, in ``notes``' order."""
    body = "\n".join(
        f"{release_note_locale_tag(locale)}\n{note}\n</{locale}>" for locale, note in notes.items()
    )
    return f"{RELEASE_NOTE_FENCE_OPEN}\n{body}\n{RELEASE_NOTE_FENCE_CLOSE}"


def release_notes_fenced_block(text: str) -> str | None:
    """The inside of the first fenced block of ``text``, or ``None`` when it carries none."""
    match = RELEASE_NOTE_FENCED_BLOCK.search(text)
    return match.group(1) if match else None


def parse_release_notes_text(text: str) -> dict[str, str]:
    """``{locale: note}`` from the fenced paste block inside ``text``.

    An absent block is an empty mapping rather than an error: whether a pack *must* carry a block is
    a question for the validator, and this function only says what the text holds.
    """
    block = release_notes_fenced_block(text)
    if block is None:
        return {}
    return {locale: note.strip() for locale, note in RELEASE_NOTE_LOCALE_TAG.findall(block)}


def release_note_overflow(locale: str, note: str) -> str | None:
    """The failure sentence for an oversized note, or ``None``.

    The sentence is built from the limit the check evaluates, so the wording cannot drift away from
    the number; see `.cursor/rules/verification-discipline.mdc` ("never a hand-written sentence").
    """
    limit = release_note_limit()
    if len(note) <= limit:
        return None
    return f"the {locale} note is {len(note)} code points, over the {limit} code point limit"


def release_note_prompts() -> dict[str, str]:
    """The unfinished-note prompt per supported locale, built from the measured cap.

    A freshly generated note is a prompt rather than text, and it has to read as unfinished -- the
    failure being prevented is a pack that looked complete while a field Play demands had no
    prepared text at all.
    """
    limit = release_note_limit()
    defaults = {
        "en-US": (
            "Describe what changed for the user in this release. Play shows this text on the store "
            f"page; the limit is {limit} characters."
        ),
        "ru-RU": (
            "Опишите, что изменилось для пользователя в этом выпуске. Play показывает этот текст на "
            f"странице приложения; лимит — {limit} символов."
        ),
    }
    ordered: dict[str, str] = {}
    for locale in supported_locales():
        ordered[locale] = defaults.get(locale, defaults["en-US"])
    return ordered


# --------------------------------------------------------------------------------------------
# The reference slot's identity vocabulary
# --------------------------------------------------------------------------------------------
#
# `store/studio.json` is a declaration, not documentation: `mark`, the full `palette`, the per-shot
# `headlines` and `kickers`, the `featureTagline` and the `featurePill` are what the launcher icon,
# the adaptive-icon vectors, the feature graphic and the store compositions are rendered from, and
# the renderer refuses rather than silently falling back to a generic pack.  The keys and the
# palette role names below are **DATA** -- read off
# `apps/2026-09-26-teacher-gradebook/store/studio.json` and recorded in
# `openspec/changes/reduce-factory-to-core/requirements-preservation.md` section 6.10 -- so they are
# a vocabulary the renderer reads by name rather than a rule anything can violate.  A slot derives
# its own palette; `requirements-preservation.md` section 6.7 is why two slots never share one.

STUDIO_IDENTITY_KEYS: tuple[str, ...] = (
    "schemaVersion",
    "mark",
    "palette",
    "headlines",
    "kickers",
    "featureTagline",
    "featurePill",
)

REFERENCE_PALETTE_ROLES: tuple[str, ...] = (
    "ink",
    "ink_soft",
    "ink_lift",
    "amber",
    "amber_deep",
    "amber_pale",
    "accent_deep",
    "paper",
    "muted",
    "bezel",
    "bezel_rim",
)


# --------------------------------------------------------------------------------------------
# File names
# --------------------------------------------------------------------------------------------


def shared_asset_name(kind: str) -> str:
    """The file name a shared (locale-independent) kind ships under, derived from its canvas."""
    if kind == "icon":
        return f"icon-{ICON['width']}.png"
    if kind == "feature-graphic":
        return f"feature-graphic-{FEATURE_GRAPHIC['width']}x{FEATURE_GRAPHIC['height']}.png"
    raise PackModelError(f"{kind!r} is not a shared asset kind; expected one of {SHARED_KINDS}")


def screenshot_asset_name(kind: str, index: int) -> str:
    """The file name the ``index``-th screenshot of ``kind`` ships under (one-based)."""
    if kind not in SCREENSHOT_KINDS:
        raise PackModelError(
            f"{kind!r} is not a screenshot kind; expected one of {SCREENSHOT_KINDS}"
        )
    return f"{kind}-{index:02d}.png"


def shipped_count(kind: str) -> int:
    """How many screenshots of ``kind`` the factory ships -- the declared pack inventory count."""
    return int(SCREENS[kind]["ship"])


def screenshot_limits(kind: str) -> dict[str, Any]:
    """The console's own window for ``kind``: ``minShortSide``/``maxLongSide``/``maxRatio``/``maxBytes``."""
    if kind not in SCREENS:
        raise PackModelError(f"unknown screenshot kind: {kind!r}; known kinds are {SCREENSHOT_KINDS}")
    return SCREENS[kind]


def aspect_ratio_failures(kind: str, size: tuple[int, int]) -> list[str]:
    """The console's aspect sentence for a screenshot of ``kind`` at ``size``, or no failures.

    Every screenshot field in the console carries the same inline rule --
    "Соотношение сторон изображения должно составлять 16:9 или 9:16" -- so this applies to phone,
    7-inch and 10-inch alike.  The comparison cross-multiplies, so an exactly 9:16 canvas is decided
    by integers and a float can never turn a pixel-perfect canvas of an allowed ratio into a near
    miss.
    """
    width, height = size
    for across, down in ASPECT_RATIOS:
        if width * down == height * across or width * across == height * down:
            return []
    allowed = " or ".join(f"{across}:{down}" for across, down in ASPECT_RATIOS)
    return [f"{kind}: {width}x{height} is not {allowed}"]


def screenshot_rule_failures(kind: str, size: tuple[int, int]) -> list[str]:
    """Every Play rule a screenshot of ``kind`` and ``size`` breaks, by name, or no failures.

    The limits are per kind because the console states a different size window in each screenshot
    field, and the aspect rule is asserted here rather than left to the per-kind ``maxRatio`` so a
    kind whose ratio is ``None`` cannot skip it.

    This is the *canvas* rule -- the shape the renderer must draw and the report
    (``scripts/visual-qa.py``) prints.  The pack's own verdict is ``scripts/store/validate.py``'s
    job and is measured independently there, from the committed bytes rather than from this model.
    """
    limits = screenshot_limits(kind)
    short, long = sorted(size)
    failures: list[str] = list(aspect_ratio_failures(kind, size))
    min_short = limits["minShortSide"]
    max_long = limits["maxLongSide"]
    if min_short is not None and short < int(min_short):
        failures.append(f"{kind}: short side {short}px is below the {min_short}px minimum")
    if max_long is not None and long > int(max_long):
        failures.append(f"{kind}: long side {long}px exceeds the {max_long}px maximum")
    ratio = limits["maxRatio"]
    if ratio is not None and long > ratio * short:
        failures.append(f"{kind}: long side {long}px is more than {ratio}x the short side {short}px")
    return failures


def screenshot_targets() -> dict[str, tuple[int, tuple[int, int]]]:
    """Every screenshot kind's shipped count and canvas, as ``{kind: (count, (width, height))}``.

    This is the shape the renderer writes and the validator measures against, so one canvas in the
    limits file moves both.
    """
    return {
        kind: (
            shipped_count(kind),
            (int(SCREENS[kind]["targetWidth"]), int(SCREENS[kind]["targetHeight"])),
        )
        for kind in SCREENSHOT_KINDS
    }


def screenshot_windows() -> dict[str, dict[str, int | None]]:
    """Every screenshot kind's size window, keyed ``min_short``/``max_long``/``max_ratio``.

    The window is per kind because the console states a different one per screenshot field; a single
    blanket predicate got one of them wrong and rejected a legal tablet asset.
    """
    return {
        kind: {
            "min_short": int(SCREENS[kind]["minShortSide"]),
            "max_long": int(SCREENS[kind]["maxLongSide"]),
            "max_ratio": SCREENS[kind]["maxRatio"],
        }
        for kind in SCREENSHOT_KINDS
    }


def screenshot_max_bytes() -> int:
    """The byte cap the screenshot fields share.

    The console states the same cap in every screenshot field, so the pack ships one default.  A
    limits file whose kinds disagree has no single default, and picking one silently is how a legal
    asset gets rejected, so that is an error rather than a choice.
    """
    caps = sorted({int(SCREENS[kind]["maxBytes"]) for kind in SCREENSHOT_KINDS})
    if len(caps) != 1:
        raise PackModelError(
            f"{PLAY_ASSETS_PATH} declares {len(caps)} distinct screenshot byte caps ({caps}); "
            "there is no single default, so a per-kind cap has to be read per kind"
        )
    return caps[0]


# --------------------------------------------------------------------------------------------
# Paths
# --------------------------------------------------------------------------------------------


def store_dir(slot_dir: Path) -> Path:
    return Path(slot_dir) / STORE_DIRNAME


def assets_dir(slot_dir: Path) -> Path:
    return store_dir(slot_dir) / ASSETS_DIRNAME


def locale_assets_dir(slot_dir: Path, locale: str) -> Path:
    return assets_dir(slot_dir) / locale


def asset_relpath(locale: str | None, name: str) -> str:
    """A `store/`-relative POSIX path.  ``locale=None`` is the shared copy at the pack root."""
    if locale is None:
        return f"{STORE_DIRNAME}/{ASSETS_DIRNAME}/{name}"
    return f"{STORE_DIRNAME}/{ASSETS_DIRNAME}/{locale}/{name}"


def pack_exists(slot_dir: Path) -> bool:
    return store_dir(slot_dir).is_dir()


def composes_media(slot_dir: Path) -> bool:
    """True when the pack ships composed media under ``store/assets/``, which a record must attest.

    Composed media is what ``scripts/store/render.py`` writes into ``store/assets/`` -- the icon,
    the feature graphic and the screenshot sets -- so its presence on disk, not the *declaration*
    in ``store/studio.json``, is what makes ``store/provenance.json`` required.

    The two files belong to different phases of this factory.  ``store/studio.json`` is authored
    before the UI is styled (``.cursor/rules/visual-identity.mdc``) while ``store/provenance.json``
    is written only by ``scripts/media-capture.mjs`` in phase 2, which refuses to run without the
    owner's recorded release approval; keying the requirement on the declaration therefore demanded
    a phase-2 artifact of every phase-1 slot.  Keying on the media is also strictly stronger than the
    line it replaces: composed images whose ``store/studio.json`` was deleted still require the
    record, so removing the declaration cannot switch the check off.

    An empty ``store/assets/`` directory is not composed media: no image on disk claims a build, so
    there is nothing to attest.

    This is the Python counterpart of ``hasComposedMedia`` in ``scripts/core/assets.mjs``, the one
    other implementation of this definition; a reader compares the two there.  Unlike the limits pair
    -- where both sides are derived from one shared declaration, ``limits.agreement`` in that file
    holding the Node constants to ``scripts/lib/play-assets.json`` while
    ``scripts/store/render_limits.py`` reads the same JSON for Python -- these two predicates have no
    shared declaration, so no program compares them and this comment is the only place the two are
    joined.
    """
    root = assets_dir(slot_dir)
    if not root.is_dir():
        return False
    return any(entry.is_file() and not entry.is_symlink() for entry in root.rglob("*"))


# --------------------------------------------------------------------------------------------
# The pack inventory
# --------------------------------------------------------------------------------------------


@dataclass(frozen=True)
class ComposedAsset:
    """One composed image the pack ships."""

    kind: str
    locale: str | None
    relpath: str

    @property
    def is_screenshot(self) -> bool:
        return self.kind in SCREENSHOT_KINDS


def composed_assets(locales: Sequence[str]) -> tuple[ComposedAsset, ...]:
    """Every composed asset the pack ships for ``locales``, in a stable order.

    The shared icon and feature graphic ship once at the pack root and once per locale, because the
    listing's own fields are per-locale and Play wants an icon and feature graphic for each.  Every
    screenshot kind ships its declared count per locale.
    """
    assets: list[ComposedAsset] = []
    for kind in SHARED_KINDS:
        assets.append(ComposedAsset(kind, None, asset_relpath(None, shared_asset_name(kind))))
    for locale in locales:
        for kind in SHARED_KINDS:
            assets.append(
                ComposedAsset(kind, locale, asset_relpath(locale, shared_asset_name(kind)))
            )
        for kind in SCREENSHOT_KINDS:
            for index in range(1, shipped_count(kind) + 1):
                assets.append(
                    ComposedAsset(
                        kind, locale, asset_relpath(locale, screenshot_asset_name(kind, index))
                    )
                )
    return tuple(assets)


def required_assets(locales: Sequence[str]) -> tuple[str, ...]:
    """Every composed asset the pack is required to ship, as `store/...` POSIX paths."""
    return tuple(asset.relpath for asset in composed_assets(locales))


def required_pack_files(locales: Sequence[str]) -> tuple[str, ...]:
    """The documents plus the composed assets: the complete required file list for the pack."""
    return PACK_DOCUMENTS + required_assets(locales)


def asset_counts(locales: Sequence[str]) -> dict[str, int]:
    """The expected inventory counts, for a human-readable summary row."""
    counts = {"shared": len(SHARED_KINDS) * (1 + len(locales))}
    for kind in SCREENSHOT_KINDS:
        counts[kind] = shipped_count(kind) * len(locales)
    counts["total"] = len(required_assets(locales))
    return counts


# --------------------------------------------------------------------------------------------
# Store JSON inputs
# --------------------------------------------------------------------------------------------


def read_json_object(path: Path) -> dict[str, Any]:
    """A JSON object read from ``path``, tolerating a UTF-8 BOM.

    Raises :class:`PackModelError` rather than returning an empty object, so a caller never
    mistakes "could not read" for "read and found nothing".
    """
    if not path.is_file():
        raise PackModelError(f"{path} is absent")
    try:
        value = json.loads(path.read_text(encoding="utf-8-sig"))
    except (OSError, json.JSONDecodeError) as error:
        raise PackModelError(f"{path} is unreadable: {error}") from error
    if not isinstance(value, dict):
        raise PackModelError(f"{path} must hold a JSON object")
    return value


def _text(value: Any) -> str:
    return value.strip() if isinstance(value, str) else ""


def _walk_url_fields(value: Any, path: str) -> Iterator[tuple[str, str]]:
    """Yield ``(dotted field path, value)`` for every string field whose name reads as a URL."""
    if isinstance(value, ABCMapping):
        for key, sub in value.items():
            child = f"{path}.{key}" if path else str(key)
            if isinstance(sub, str) and "url" in str(key).lower():
                yield child, sub
            else:
                yield from _walk_url_fields(sub, child)
    elif isinstance(value, list):
        for index, sub in enumerate(value):
            yield from _walk_url_fields(sub, f"{path}[{index}]")


@dataclass(frozen=True)
class LocaleText:
    """The three text fields Play caps, for one locale."""

    locale: str
    title: str
    short_description: str
    full_description: str

    def field(self, name: str) -> str:
        return {
            "title": self.title,
            "shortDescription": self.short_description,
            "fullDescription": self.full_description,
        }[name]


@dataclass(frozen=True)
class Listing:
    """`store/listing.json`, typed, with the per-locale shape resolved."""

    path: Path
    raw: dict[str, Any]
    default_locale: str
    locales: dict[str, LocaleText]
    contact_email: str
    contact_email_is_default: bool

    def declared_locales(self) -> tuple[str, ...]:
        """The locales the pack declares, the default first and each locale once."""
        ordered: list[str] = []
        for locale in (self.default_locale, *self.locales):
            if locale and locale not in ordered:
                ordered.append(locale)
        return tuple(ordered)

    def url_fields(self) -> tuple[tuple[str, str], ...]:
        """Every declared URL in the listing, as ``(field path, value)``."""
        return tuple(_walk_url_fields(self.raw, ""))


def load_listing(slot_dir: Path) -> Listing:
    path = store_dir(slot_dir) / LISTING_FILENAME
    raw = read_json_object(path)

    default_locale = _text(raw.get("defaultLocale")) or _text(LOCALES.get("default"))
    entries: dict[str, LocaleText] = {}
    locales_raw = raw.get("locales")
    if isinstance(locales_raw, ABCMapping):
        for locale, value in locales_raw.items():
            if isinstance(value, ABCMapping):
                entries[str(locale)] = LocaleText(
                    locale=str(locale),
                    title=_text(value.get("title") or value.get("name")),
                    short_description=_text(
                        value.get("shortDescription") or value.get("short_description")
                    ),
                    full_description=_text(
                        value.get("fullDescription") or value.get("full_description")
                    ),
                )
    if default_locale and default_locale not in entries:
        entries[default_locale] = LocaleText(default_locale, "", "", "")

    declared = raw.get("contactEmail")
    if isinstance(declared, str) and declared.strip():
        contact_email, is_default = declared.strip(), False
    else:
        contact_email, is_default = DEFAULT_CONTACT_EMAIL, True

    return Listing(
        path=path,
        raw=raw,
        default_locale=default_locale,
        locales=entries,
        contact_email=contact_email,
        contact_email_is_default=is_default,
    )


@dataclass(frozen=True)
class ProvenanceEntry:
    """One recorded asset: the slot-relative path, its recorded digest and byte size."""

    file: str
    sha256: str
    bytes: int | None


@dataclass(frozen=True)
class Provenance:
    """`store/provenance.json`: the durable link between committed images and a recorded build."""

    path: Path
    raw: dict[str, Any]
    composed_assets: tuple[ProvenanceEntry, ...]
    raw_captures: tuple[ProvenanceEntry, ...]


def _provenance_entries(raw: dict[str, Any], key: str) -> tuple[ProvenanceEntry, ...]:
    value = raw.get(key)
    if not isinstance(value, list):
        return ()
    entries: list[ProvenanceEntry] = []
    for item in value:
        if not isinstance(item, ABCMapping):
            continue
        entries.append(
            ProvenanceEntry(
                # Normalised the way the retired `store-kit.py` normalised it, so a Windows-recorded
                # path and a POSIX-recorded path compare equal.
                file=str(item.get("file") or "").replace("\\", "/").lstrip("./"),
                sha256=str(item.get("sha256") or "").lower(),
                bytes=item.get("bytes") if isinstance(item.get("bytes"), int) else None,
            )
        )
    return tuple(entries)


def load_provenance(slot_dir: Path) -> Provenance | None:
    """The record, or ``None`` when the pack ships none."""
    path = store_dir(slot_dir) / PROVENANCE_FILENAME
    if not path.is_file():
        return None
    raw = read_json_object(path)
    return Provenance(
        path=path,
        raw=raw,
        composed_assets=_provenance_entries(raw, "composedAssets"),
        raw_captures=_provenance_entries(raw, "rawCaptures"),
    )


# --------------------------------------------------------------------------------------------
# Slot resolution
# --------------------------------------------------------------------------------------------


def find_slot(slug: str) -> Path:
    """Resolve a slug beneath ``apps/`` the way the retired ``store-kit.py`` did.

    A directory whose name equals the slug or ends with ``-<slug>`` matches; more than one match is
    refused rather than guessed, because choosing between two slots is not the resolver's call.
    """
    if not APPS_DIR.is_dir():
        raise PackModelError(f"{APPS_DIR} does not exist")
    matches = sorted(
        path
        for path in APPS_DIR.iterdir()
        if path.is_dir() and (path.name == slug or path.name.endswith(f"-{slug}"))
    )
    if not matches:
        raise PackModelError(f"no slot under {APPS_DIR} equals or ends with '-{slug}'")
    if len(matches) > 1:
        names = ", ".join(path.name for path in matches)
        raise PackModelError(f"slot '{slug}' is ambiguous: {names}")
    return matches[0]


def supported_locales() -> tuple[str, ...]:
    """The locales the factory declares support for, default first."""
    additional = LOCALES.get("additional") if isinstance(LOCALES.get("additional"), list) else []
    ordered: list[str] = []
    for locale in (LOCALES.get("default"), *additional):
        text = _text(locale)
        if text and text not in ordered:
            ordered.append(text)
    return tuple(ordered)


def resolved_limits() -> dict[str, Any]:
    """Every limit and the inventory this module actually loaded, for a human to confirm."""
    locales = supported_locales()
    return {
        "_source": str(PLAY_ASSETS_PATH.relative_to(REPO_ROOT).as_posix()),
        "_measuredAt": PLAY_ASSETS.get("_measuredAt"),
        "assetKinds": list(ASSET_KINDS),
        "sharedKinds": list(SHARED_KINDS),
        "screenshotKinds": list(SCREENSHOT_KINDS),
        "shippedCounts": {kind: shipped_count(kind) for kind in SCREENSHOT_KINDS},
        "imageTypes": IMAGE_TYPES,
        "aspectRatios": [list(pair) for pair in ASPECT_RATIOS],
        "icon": ICON,
        "featureGraphic": FEATURE_GRAPHIC,
        "screens": SCREENS,
        "listing": LISTING_LIMITS,
        "releaseNote": RELEASE_NOTE,
        "nonDegenerate": NON_DEGENERATE,
        "placeholderHosts": list(PLACEHOLDER_HOSTS),
        "contactEmail": DEFAULT_CONTACT_EMAIL,
        "locales": LOCALES,
        "supportedLocales": list(locales),
        "packDocuments": list(PACK_DOCUMENTS),
        "releaseNoteFormat": {
            "fenceOpen": RELEASE_NOTE_FENCE_OPEN,
            "fenceClose": RELEASE_NOTE_FENCE_CLOSE,
            "localeTag": "<locale> ... </locale>, each tag on a line of its own",
            "maxChars": release_note_limit(),
            "prompts": release_note_prompts(),
        },
        "studioIdentityKeys": list(STUDIO_IDENTITY_KEYS),
        "referencePaletteRoles": list(REFERENCE_PALETTE_ROLES),
        "sharedAssetNames": {kind: shared_asset_name(kind) for kind in SHARED_KINDS},
        "assetCounts": asset_counts(locales),
        "requiredAssets": list(required_assets(locales)),
    }


if __name__ == "__main__":
    print(json.dumps(resolved_limits(), indent=2, ensure_ascii=False))
    sys.exit(0)
