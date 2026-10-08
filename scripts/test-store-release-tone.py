#!/usr/bin/env python3
"""Regression suite for finished-product store-copy tone (release notes + listing descriptions).

The defect this exists against: packs shipped "First release…" / "Первый выпуск…" while the factory
claims a finished product, and nothing failed the pack.  Ban list and prompts live in
``scripts/store/model.py``; ``scripts/store/validate.py`` emits ``release-notes:tone`` and
``listing:tone`` FAIL rows.  This suite proves the sensor can fail, then restores it.

python scripts/test-store-release-tone.py     # exits 0 when every expectation holds
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

sys.path.insert(0, str(ROOT / "scripts" / "store"))
import model  # noqa: E402
import validate  # noqa: E402

_CREATED: list[Path] = []
atexit.register(lambda: [shutil.rmtree(path, ignore_errors=True) for path in _CREATED])

results: list[tuple[bool, str]] = []


def check(ok: bool, label: str) -> None:
    results.append((ok, label))
    print(f"{'PASS' if ok else 'FAIL'}\t{label}")


def tone_rows_for_notes(body_by_locale: dict[str, str]) -> list[validate.Row]:
    """Drive ``check_release_notes`` over a synthetic slot whose fenced block is ``body_by_locale``."""
    slot = Path(tempfile.mkdtemp(prefix="release-tone-", dir=SCRATCH_DIR))
    _CREATED.append(slot)
    store = slot / "store"
    store.mkdir(parents=True)
    block = model.release_notes_paste_block(body_by_locale)
    (store / "release-notes.md").write_text(
        f"# Release notes\n\n**Use:** `1.0.0 (1)`\n\n{block}\n",
        encoding="utf-8",
    )
    report = validate.Report()
    with contextlib.redirect_stdout(io.StringIO()):
        validate.check_release_notes(report, slot)
    return [row for row in report.rows if row.check == "release-notes:tone"]


# --- Predicate: banned phrases and clean copy ---------------------------------------------------

for phrase in (
    "First release of the app.",
    "This is the initial release.",
    "Первый выпуск приложения.",
    "Это первый релиз.",
    "Features coming soon.",
    "Still in development.",
    "Under development right now.",
    "WIP build for testers.",
    "We will add more screens later.",
    "Скоро добавим отчёты.",
    "Приложение в разработке.",
):
    reason = model.banned_store_copy_tone(phrase)
    check(reason is not None, f"banned tone detected in {phrase!r}")
    if reason is not None:
        check("banned unfinished-product tone" in reason, f"remedy named for {phrase!r}")

check(
    model.banned_store_copy_tone(
        "Ready to use. Export CSV offline. Leave wishes in a Play review."
    )
    is None,
    "finished-product copy is clean",
)
check(
    model.banned_store_copy_tone("The swipe gesture is smooth.") is None,
    "unrelated prose mentioning no ban phrase is clean",
)
check(
    model.banned_store_copy_tone("equipment") is None,
    "token WIP does not fire inside unrelated words (equipment)",
)

# --- Prompts teach posture without containing a ban substring -----------------------------------

prompts = model.release_note_prompts()
en = prompts.get("en-US", "")
ru = prompts.get("ru-RU", "")
check("can do now" in en.casefold(), "EN prompt requires finished-product posture (what works now)")
check(
    "play reviews" in en.casefold() or "play review" in en.casefold(),
    "EN prompt invites Play review feedback",
)
check("сейчас" in ru.casefold(), "RU prompt requires finished-product posture (сейчас)")
check("отзыв" in ru.casefold(), "RU prompt invites Play review feedback (отзыв)")
for locale, prompt in prompts.items():
    hit = model.banned_store_copy_tone(prompt)
    check(hit is None, f"{locale} prompt itself is free of banned tone substrings")

# --- Validator FAIL on a synthetic bad note -----------------------------------------------------

bad_rows = tone_rows_for_notes({"en-US": "First release. Everything works offline."})
check(len(bad_rows) == 1, f"one release-notes:tone row for a bad note (got {bad_rows!r})")
check(
    bad_rows[0].verdict == validate.FAIL if bad_rows else False,
    "synthetic 'First release' note FAILs release-notes:tone",
)
check(
    bad_rows[0].detail and "first release" in bad_rows[0].detail.casefold() if bad_rows else False,
    "FAIL detail names the matched phrase",
)

good_rows = tone_rows_for_notes(
    {
        "en-US": (
            "Ready to use. Build a set, share it, work offline. "
            "Have a wish? Leave it in a Play review."
        )
    }
)
check(
    good_rows and good_rows[0].verdict == validate.PASS,
    "finished-product note PASSes release-notes:tone",
)

ru_bad = tone_rows_for_notes({"ru-RU": "Первый выпуск. Всё работает офлайн."})
check(
    ru_bad and ru_bad[0].verdict == validate.FAIL,
    "synthetic 'Первый выпуск' note FAILs release-notes:tone",
)

# --- Neutered predicate: empty ban list → miss, then restore → catch again ----------------------

saved_bans = model.STORE_COPY_TONE_BANS
saved_tokens = model.STORE_COPY_TONE_BAN_TOKENS
saved_re = model._STORE_COPY_TOKEN_RE
try:
    model.STORE_COPY_TONE_BANS = ()
    model.STORE_COPY_TONE_BAN_TOKENS = ()
    model._STORE_COPY_TOKEN_RE = __import__("re").compile(r"(?!)")  # never matches
    check(
        model.banned_store_copy_tone("First release of anything.") is None,
        "neutered ban list misses 'First release' (sensor depends on the list)",
    )
finally:
    model.STORE_COPY_TONE_BANS = saved_bans
    model.STORE_COPY_TONE_BAN_TOKENS = saved_tokens
    model._STORE_COPY_TOKEN_RE = saved_re

check(
    model.banned_store_copy_tone("First release of anything.") is not None,
    "restored ban list catches 'First release' again",
)

# --- Measured slots: rewritten notes stay under the cap and pass tone ---------------------------

limit = model.release_note_limit()
for slug in ("2026-10-01-lawn-round-log", "2026-10-02-laneset"):
    path = ROOT / "apps" / slug / "store" / "release-notes.md"
    if not path.is_file():
        check(False, f"{slug} release-notes.md exists")
        continue
    notes = model.parse_release_notes_text(path.read_text(encoding="utf-8-sig"))
    check(bool(notes), f"{slug} has a parseable fenced note block")
    for locale, note in notes.items():
        check(len(note) <= limit, f"{slug} {locale} note {len(note)}/{limit} code points")
        check(
            model.banned_store_copy_tone(note) is None,
            f"{slug} {locale} note has finished-product tone",
        )
        check(
            "play review" in note.casefold() or "отзыв" in note.casefold(),
            f"{slug} {locale} note invites Play review feedback",
        )

failed = sum(1 for ok, _ in results if not ok)
print(f"\n{len(results) - failed}/{len(results)} checks passed")
print(f"VERDICT: {'PASS' if failed == 0 else f'FAIL ({failed})'}")
sys.exit(1 if failed else 0)
