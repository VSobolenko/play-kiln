#!/usr/bin/env python3
"""Regression suite for the release-name and release-note fields (`store/release-notes.md`).

The defect this exists against: the store pack carried every other Play field and neither of these,
so the first real upload reached the release screen with no prepared text and no way to count it —
and Play's 500-character cap is enforced by silently truncating the paste, not by refusing it.

Two halves, and the second is the one that can fail:

- **Generation.** `write_documents` must produce a file that parses, states a release name taken
  from the Gradle version, and carries one prompt per listing language. It must also refuse to
  overwrite notes a human wrote, because a regenerated pack silently replacing real release text
  with a prompt is worse than having no file.
- **Validation.** Every rule the parser enforces is exercised by mutating a real generated file and
  requiring a failure that names the rule. A check that cannot fail is not a check.

Usage: python scripts/test-release-notes.py
"""

import importlib.util
import json
import re
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LIMIT = 500

spec = importlib.util.spec_from_file_location("store_kit", ROOT / "scripts" / "store-kit.py")
assert spec and spec.loader
kit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(kit)

results: list[tuple[bool, str]] = []


def check(ok: bool, label: str) -> None:
    results.append((ok, label))
    print(f"{'PASS' if ok else 'FAIL'}\t{label}")


META = {
    "app_name": "Test App",
    "application_id": "com.gexetr.testapp",
    "namespace": "com.gexetr.testapp",
    "version_name": "1.2.3",
    "version_code": "7",
    "idea": "A focused offline tool.",
    "recordedIdea": "",
    "slug": "testapp",
}
COMPLIANCE = {
    "enabled": [],
    "requiresConsent": False,
    "requiresPurchaseAuthority": False,
    "purchaseAuthority": None,
}


def build_slot(locales: list[str], manifest: dict | None) -> tuple[Path, Path, Path]:
    """A temp slot with a generated store pack, returning (slot, store, notes path)."""
    slot = Path(tempfile.mkdtemp(prefix="release-notes-"))
    store = slot / "store"
    store.mkdir(parents=True)
    listing = kit.default_listing(META, locales, COMPLIANCE)
    kit.apply_capability_compliance(listing, COMPLIANCE)
    kit.write_documents(store, listing, locales, False, META)
    if manifest is not None:
        dist = slot / "dist"
        dist.mkdir()
        (dist / "release-manifest.json").write_text(json.dumps(manifest), encoding="utf-8")
    return slot, store, store / "release-notes.md"


# --- Generation ---------------------------------------------------------------------------------

slot, store, notes_path = build_slot(["en-US", "ru-RU"], {"versionName": "1.2.3", "versionCode": 7})
check(notes_path.is_file(), "write_documents generates store/release-notes.md")

generated_name, generated_notes = kit.parse_release_notes(notes_path)
check(generated_name == "1.2.3 (7)", f"the generated release name comes from the Gradle version (got {generated_name!r})")
check(
    sorted(generated_notes) == ["en-US", "ru-RU"],
    f"one tagged note per listing language (got {sorted(generated_notes)})",
)
check(
    all(kit.RELEASE_NOTE_UNFINISHED in body for body in generated_notes.values()),
    "a generated note is marked unfinished rather than reading as finished text",
)
check(
    not kit.validate_release_notes(store, generated_name, generated_notes, ["en-US", "ru-RU"]),
    "a freshly generated pack validates (the name agrees with the bundle it will name)",
)

# The generated block must be Play's own format, since it is pasted verbatim: tags on their own
# lines with the locale as the tag name. A generator that emitted `### en-US` headings would leave
# the human to translate the structure into Play's format by hand.
generated_text = notes_path.read_text(encoding="utf-8")
fence = re.search(r"^```[^\n]*\n(.*?)^```", generated_text, re.MULTILINE | re.DOTALL)
check(fence is not None, "the generated notes live in a fenced block that can be copied whole")
if fence:
    for locale in ("en-US", "ru-RU"):
        check(
            re.search(rf"^<{locale}>$", fence.group(1), re.MULTILINE) is not None
            and re.search(rf"^</{locale}>$", fence.group(1), re.MULTILINE) is not None,
            f"the generated block opens and closes <{locale}> on their own lines",
        )

# The prompt must not be pasted as if it were the note.
unfinished_warn = kit.validate_release_notes(store, generated_name, generated_notes, ["en-US"])
check(unfinished_warn == [], "an unfinished note is not a validation failure (it is a human's next action)")

# Regeneration must not clobber real release text: this is the difference between a helpful
# generator and one that silently replaces a written release note with a TODO.
hand_written = (
    "# Release notes\n\n**Use:** `1.2.3 (7)`\n\n"
    "```text\n<en-US>\nThe first release.\n</en-US>\n```\n"
)
notes_path.write_text(hand_written, encoding="utf-8")
listing = kit.default_listing(META, ["en-US"], COMPLIANCE)
kit.apply_capability_compliance(listing, COMPLIANCE)
kit.write_documents(store, listing, ["en-US"], False, META)
check(notes_path.read_text(encoding="utf-8") == hand_written, "a hand-written release note is preserved, not replaced")

# --- Validation: every rule must be able to fail -------------------------------------------------

# A note over the limit is the defect the whole file exists for.
over = "x" * (LIMIT + 1)
failures = kit.validate_release_notes(store, "1.2.3 (7)", {"en-US": over}, ["en-US"])
check(len(failures) == 1 and str(LIMIT) in failures[0], "an over-long note fails naming the limit")
check(len(kit.validate_release_notes(store, "1.2.3 (7)", {"en-US": "x" * LIMIT}, ["en-US"])) == 0, "a note exactly at the limit passes")

# A section that exists is measured even when the listing does not declare that language, because a
# human may still paste it.
failures = kit.validate_release_notes(store, "1.2.3 (7)", {"en-US": "ok", "ru-RU": over}, ["en-US"])
check(any("ru-RU" in failure for failure in failures), "an over-long note in an undeclared language still fails")

# A listing language with no section means the console box would be pasted empty.
failures = kit.validate_release_notes(store, "1.2.3 (7)", {"en-US": "ok"}, ["en-US", "ru-RU"])
check(any("ru-RU" in failure for failure in failures), "a listing language with no note section fails")
# The release name must agree with the bundle, or the console list misnames the build.
slot2, store2, _ = build_slot(["en-US"], {"versionName": "1.2.3", "versionCode": 7})
failures = kit.validate_release_notes(store2, "1.2.2 (6)", {"en-US": "ok"}, ["en-US"])
check(any("1.2.3 (7)" in failure for failure in failures), "a release name disagreeing with the bundle fails")
check(
    len(kit.validate_release_notes(store2, "1.2.3 (7)", {"en-US": "ok"}, ["en-US"])) == 0,
    "a release name agreeing with the bundle passes",
)

# With no build yet there is no bundle to agree with, so the name is not held against a manifest.
slot3, store3, _ = build_slot(["en-US"], None)
check(
    len(kit.validate_release_notes(store3, "9.9.9", {"en-US": "ok"}, ["en-US"])) == 0,
    "an unbuilt slot is not failed for disagreeing with a manifest that does not exist",
)

# A missing name is a failure: the console field would be filled from memory.
failures = kit.validate_release_notes(store3, "", {"en-US": "ok"}, ["en-US"])
check(any("no release name" in failure for failure in failures), "a missing release name fails")

# A file with no sections at all is a failure, not a silently empty pack.
check(len(kit.validate_release_notes(store3, "1.2.3 (7)", {}, [])) > 0, "release notes with no sections fail")

# --- The parser itself ---------------------------------------------------------------------------
#
# The format is Play's, because the block is pasted verbatim into the console. These legs pin the
# format down: tags name the locale, they sit on their own lines, and the body may span lines.

plain = Path(tempfile.mkdtemp(prefix="release-notes-parse-")) / "release-notes.md"
plain.write_text(
    "# Notes\n\n**Use:** `4.5.6 (8)`\n\n## What's new\n\n"
    "```text\n"
    "<en-US>\nLine one.\n\nLine two.\n</en-US>\n"
    "<ru-RU>\nСтрока.\n</ru-RU>\n"
    "```\n",
    encoding="utf-8",
)
name, notes = kit.parse_release_notes(plain)
check(name == "4.5.6 (8)", f"the parser reads a backticked release name (got {name!r})")
check(notes.get("en-US") == "Line one.\n\nLine two.", "the parser keeps a multi-line note body intact")
check(notes.get("ru-RU") == "Строка.", "the parser reads a non-ASCII note body")
check(
    "</en-US>" not in notes.get("en-US", "") and "<ru-RU>" not in notes.get("en-US", ""),
    "a note body does not absorb the surrounding tags",
)

# Play's documented requirement is that the tags sit on their own lines. An inline tag is therefore
# not recognised, and the locale is reported missing rather than silently mis-parsed — which is what
# makes this a real rule rather than a formatting preference.
inline = Path(tempfile.mkdtemp(prefix="release-notes-inline-")) / "release-notes.md"
inline.write_text(
    "# Notes\n\n**Use:** `4.5.6 (8)`\n\n```text\n<en-US>The first release.</en-US>\n```\n",
    encoding="utf-8",
)
inline_name, inline_notes = kit.parse_release_notes(inline)
check(inline_notes == {}, "a tag on the same line as its note is not recognised")
check(
    any("en-US" in failure for failure in kit.validate_release_notes(Path("/nonexistent"), inline_name, inline_notes, ["en-US"])),
    "an inline tag is reported as a missing note for that locale",
)

# Text outside any fence is prose about the notes, not the notes. Only the pasted block counts, so a
# stray mention of a locale in the surrounding explanation cannot be mistaken for a note.
stray = Path(tempfile.mkdtemp(prefix="release-notes-stray-")) / "release-notes.md"
stray.write_text(
    "# Notes\n\n**Use:** `4.5.6 (8)`\n\nNever write English under a `<fr-FR>` tag.\n\n"
    "```text\n<en-US>\nThe first release.\n</en-US>\n```\n",
    encoding="utf-8",
)
_, stray_notes = kit.parse_release_notes(stray)
check(list(stray_notes) == ["en-US"], f"prose mentioning a locale tag is not a note (got {sorted(stray_notes)})")

failed = sum(1 for ok, _ in results if not ok)
print(f"\n{len(results) - failed}/{len(results)} checks passed")
print(f"VERDICT: {'PASS' if failed == 0 else f'FAIL ({failed})'}")
sys.exit(1 if failed else 0)
