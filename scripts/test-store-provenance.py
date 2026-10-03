#!/usr/bin/env python3
"""Regression suite for the pack's ``store/provenance.json`` trigger.

The defect this exists against: ``scripts/store/model.py``'s ``composes_media`` keyed the
requirement on ``store/studio.json`` -- the identity declaration authored in **phase 1** -- while
the record it demands is written only by ``scripts/media-capture.mjs`` in **phase 2**, which refuses
to run without the owner's recorded release approval.  Every slot in development was therefore
permanently red on the provenance row, and a gate that cannot be green in the state the pipeline
puts a slot in is one the operator learns to read past.

The predicate now keys on the composed media itself, matching ``hasComposedMedia`` in
``scripts/core/assets.mjs`` term for term: ``store/assets/`` holding at least one file (recursive)
is composed media, and an absent or empty directory is not.  The legs below hold both directions --
the phase-1 SKIP that was the regression, and the anti-evasion half, so deleting ``studio.json``
cannot switch the check off while images remain.

There is no other Python test of this predicate, so this file is its only falsifiable check; the
Node counterpart carries its own fixture legs in ``scripts/core/assets.mjs``.

Usage: python scripts/test-store-provenance.py
"""

from __future__ import annotations

import atexit
import contextlib
import io
import shutil
import sys
import tempfile
from pathlib import Path

sys.dont_write_bytecode = True

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
from lib.scratch import SCRATCH_DIR  # noqa: E402

# The model and the validator are files in one directory, not an installed package.
sys.path.insert(0, str(ROOT / "scripts" / "store"))
import model  # noqa: E402  (scripts/store/model.py, the pack's path resolver)
import validate  # noqa: E402  (scripts/store/validate.py, the pack's verdict)

_CREATED: list[Path] = []
atexit.register(lambda: [shutil.rmtree(path, ignore_errors=True) for path in _CREATED])

results: list[tuple[bool, str]] = []


def check(ok: bool, label: str) -> None:
    results.append((ok, label))
    print(f"{'PASS' if ok else 'FAIL'}\t{label}")


def make_slot(*, studio: bool, assets: dict[str, bytes] | None = None) -> Path:
    """A scratch slot whose pack carries ``studio.json`` and the named ``store/assets/`` files.

    ``assets=None`` leaves ``store/assets`` absent; ``assets={}`` creates it empty; a populated
    mapping writes one file per entry (a key may name a nested path such as ``en-US/phone-01.png``).
    """
    slot = Path(tempfile.mkdtemp(prefix="store-provenance-", dir=SCRATCH_DIR))
    _CREATED.append(slot)
    store = slot / "store"
    store.mkdir(parents=True)
    (store / "listing.json").write_text("{}", encoding="utf-8")
    if studio:
        (store / "studio.json").write_text("{}\n", encoding="utf-8")
    if assets is not None:
        (store / "assets").mkdir(parents=True, exist_ok=True)
        for relpath, data in assets.items():
            target = store / "assets" / relpath
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
    return slot


def provenance_row(slot: Path) -> validate.Row:
    """The one ``provenance`` row ``check_provenance`` prints for this slot."""
    report = validate.Report()
    with contextlib.redirect_stdout(io.StringIO()):
        validate.check_provenance(report, slot, ())
    rows = [row for row in report.rows if row.check == "provenance"]
    assert len(rows) == 1, f"expected one provenance row, got {rows!r}"
    return rows[0]


# --- The predicate: composed media on disk, never the phase-1 declaration ------------------------

check(
    model.composes_media(make_slot(studio=False)) is False,
    "no store/assets is not composed media",
)

check(
    model.composes_media(make_slot(studio=True)) is False,
    "store/studio.json alone is not composed media (the phase-1 regression)",
)

check(
    model.composes_media(make_slot(studio=True, assets={"icon-512.png": b"png"})) is True,
    "a file under store/assets is composed media",
)

check(
    model.composes_media(make_slot(studio=False, assets={"icon-512.png": b"png"})) is True,
    "composed media with studio.json deleted is still composed media (anti-evasion)",
)

check(
    model.composes_media(make_slot(studio=True, assets={"en-US/phone-01.png": b"png"})) is True,
    "composed media nested under a locale directory is still composed media",
)

check(
    model.composes_media(make_slot(studio=True, assets={})) is False,
    "an empty store/assets directory is not composed media",
)

# --- The row the gate prints --------------------------------------------------------------------

row = provenance_row(make_slot(studio=True))
check(
    row.verdict == validate.SKIP and "no composed media" in row.detail,
    f"a phase-1 slot is SKIP, not FAIL (got {row.verdict}: {row.detail})",
)

row = provenance_row(make_slot(studio=True, assets={"icon-512.png": b"png"}))
check(
    row.verdict == validate.FAIL and "store/assets" in row.detail,
    f"composed media with no record is FAIL (got {row.verdict}: {row.detail})",
)

failed = sum(1 for ok, _ in results if not ok)
print(f"\n{len(results) - failed}/{len(results)} checks passed")
print(f"VERDICT: {'PASS' if failed == 0 else f'FAIL ({failed})'}")
sys.exit(1 if failed else 0)
