#!/usr/bin/env python3
"""Regression suite for the privacy-policy URL: the draft seed, the generated checks, the verdict.

The defect this exists against: the factory had a working publisher
(`scripts/publish-privacy.mjs`, which writes `store/privacy.md` to Telegraph and records the URL in
`store/listing.json`) and nothing called it, so a freshly generated pack still told the reader to
"host `store/privacy.md` publicly and replace the placeholder" -- the question the owner is tired of
being asked -- and `scripts/store/validate.py` reported a reserved-host `privacyPolicyUrl` as a
softer `UNFINISHED` note while catching an **absent** one not at all.  A pack that ships
`https://example.com/privacy` is a pack a reviewer refuses, so the read path now FAILs both states
and the generated documents name the publishing command while keeping only the residual human act.

The legs below hold the three states a real listing can be in -- absent, placeholder, real -- and
both decisions the generated text makes, so a later edit cannot quietly restore the old phrasing or
weaken the verdict to a note.

python scripts/test-store-privacy.py     # exits 0 when every expectation holds
"""

from __future__ import annotations

import atexit
import contextlib
import importlib.util
import io
import shutil
import sys
import tempfile
from pathlib import Path

sys.dont_write_bytecode = True

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
from lib.scratch import SCRATCH_DIR  # noqa: E402

# The validator, the model and the sibling renderer are files in one directory, not a package.
sys.path.insert(0, str(ROOT / "scripts" / "store"))
import model  # noqa: E402  (scripts/store/model.py, the pack's path resolver)
import validate  # noqa: E402  (scripts/store/validate.py, the pack's verdict)

_CREATED: list[Path] = []
atexit.register(lambda: [shutil.rmtree(path, ignore_errors=True) for path in _CREATED])

results: list[tuple[bool, str]] = []

PLACEHOLDER_URL = "https://example.com/privacy"
REAL_URL = "https://telegra.ph/Privacy-Fixture--privacy-policy-10-02"
SLUG = "privacyfixture"


def check(ok: bool, label: str) -> None:
    results.append((ok, label))
    print(f"{'PASS' if ok else 'FAIL'}\t{label}")


def load_render_documents():
    """`scripts/store/render_documents.py` by path, the way `test-listing-reasons.py` loads a sibling."""
    path = ROOT / "scripts" / "store" / "render_documents.py"
    spec = importlib.util.spec_from_file_location("store_render_documents", path)
    if spec is None or spec.loader is None:
        raise SystemExit(f"BLOCKED: cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    # Registered before execution so its own `render_limits`/`render_support` imports resolve.
    sys.modules.setdefault("store_render_documents", module)
    spec.loader.exec_module(module)
    return module


def listing_model(*, privacy: object, raw_extra: dict | None = None) -> model.Listing:
    """A `model.Listing` with `privacy` under `privacyPolicyUrl` (omitted when `privacy is _ABSENT`)."""
    raw: dict[str, object] = {"slug": SLUG}
    if privacy is not _ABSENT:
        raw["privacyPolicyUrl"] = privacy
    if raw_extra:
        raw.update(raw_extra)
    return model.Listing(
        path=Path("listing.json"),
        raw=raw,
        default_locale="en-US",
        locales={},
        contact_email="v.sobolenko.vs@gmail.com",
        contact_email_is_default=False,
    )


_ABSENT = object()


def privacy_row(listing: model.Listing) -> validate.Row:
    report = validate.Report()
    with contextlib.redirect_stdout(io.StringIO()):
        validate.check_privacy_policy(report, listing)
    rows = [row for row in report.rows if row.check == "privacy-policy"]
    assert len(rows) == 1, f"expected one privacy-policy row, got {rows!r}"
    return rows[0]


def placeholder_rows(listing: model.Listing) -> list[validate.Row]:
    report = validate.Report()
    with contextlib.redirect_stdout(io.StringIO()):
        validate.check_placeholder_hosts(report, listing)
    return list(report.rows)


# --- The publisher is the baseline the wording and the verdict both rest on ---------------------

check(
    validate.unfinished_url_reason(PLACEHOLDER_URL) is not None,
    "the reserved-host placeholder is recognised as unfinished",
)
check(
    validate.unfinished_url_reason(REAL_URL) is None,
    "a real https host is not unfinished",
)

# --- The verdict: absent, placeholder and real, with the remedy named ---------------------------

row = privacy_row(listing_model(privacy=_ABSENT))
check(
    row.verdict == validate.FAIL and "declares no privacy policy URL" in row.detail,
    f"an absent privacyPolicyUrl FAILs (got {row.verdict}: {row.detail})",
)
check(
    f"publish-privacy.mjs --slot {SLUG}" in row.detail,
    "the absent case names the exact publishing command and slug",
)

row = privacy_row(listing_model(privacy=PLACEHOLDER_URL))
check(
    row.verdict == validate.FAIL and PLACEHOLDER_URL in row.detail,
    f"a reserved-host privacyPolicyUrl FAILs and names the URL (got {row.verdict}: {row.detail})",
)
check(
    "reserved documentation host" in row.detail and f"publish-privacy.mjs --slot {SLUG}" in row.detail,
    "the placeholder case states the reason and the remedy",
)

row = privacy_row(listing_model(privacy=REAL_URL))
check(
    row.verdict == validate.PASS and REAL_URL in row.detail,
    f"a real URL PASSes (got {row.verdict}: {row.detail})",
)

# A pack with no resolvable slug still names a runnable remedy rather than an empty command.
row = privacy_row(
    model.Listing(
        path=Path("listing.json"),
        raw={"privacyPolicyUrl": PLACEHOLDER_URL},
        default_locale="en-US",
        locales={},
        contact_email="v.sobolenko.vs@gmail.com",
        contact_email_is_default=False,
    )
)
check(
    "publish-privacy.mjs --slot <slug>" in row.detail,
    "with no slug in the listing the remedy falls back to the literal <slug> placeholder",
)

# --- privacyPolicyUrl is measured once, not folded into the softer roll-up ----------------------

rows = placeholder_rows(listing_model(privacy=PLACEHOLDER_URL, raw_extra={"websiteUrl": PLACEHOLDER_URL}))
check(
    not any(row.subject == "privacyPolicyUrl" for row in rows),
    "check_placeholder_hosts does not double-report privacyPolicyUrl",
)
check(
    any(row.subject == "websiteUrl" and row.verdict == validate.UNFINISHED for row in rows),
    "another placeholder URL still gets the softer UNFINISHED verdict",
)
rollup = rows[-1]
check(
    "besides privacyPolicyUrl" in rollup.detail,
    f"the roll-up says privacyPolicyUrl is measured separately (got {rollup.detail!r})",
)

# --- The generated documents tell the reader what the factory does, not what it must do ---------

render_documents = load_render_documents()


def generated_documents(privacy_url: str) -> tuple[str, str]:
    """`(review-notes, checklist)` for a minimal pack whose policy URL is `privacy_url`."""
    store = Path(tempfile.mkdtemp(prefix="store-privacy-", dir=SCRATCH_DIR)) / "store"
    _CREATED.append(store.parent)
    store.mkdir(parents=True)
    listing = {
        "schemaVersion": 1,
        "appName": "Privacy Fixture",
        "slug": SLUG,
        "applicationId": "com.gexetr.privacyfixture",
        "idea": "A focused offline tool.",
        "defaultLocale": "en-US",
        "category": "PRODUCTIVITY",
        "tags": ["offline"],
        "contactEmail": "v.sobolenko.vs@gmail.com",
        "privacyPolicyUrl": privacy_url,
        "locales": {
            "en-US": {
                "title": "Privacy Fixture",
                "shortDescription": "A focused offline tool.",
                "fullDescription": "A focused offline tool.",
                "screenshotCaptions": [f"caption {index}" for index in range(1, 7)],
                "featureBullets": ["a", "b", "c"],
            }
        },
        "dataSafety": {
            "collectsData": False,
            "reason": "Enabled capabilities declare no collected data.",
            "dataTypes": [],
            "thirdPartySdks": [],
            "encryptedInTransit": False,
            "hasAccountDeletion": False,
        },
        "permissions": [],
        "ads": {"containsAds": False, "reason": "No advertising SDK."},
        "contentRating": {"questionnaire": "notes", "targetAudience": ""},
        "graphicsSource": {
            "iconGlyph": "focus",
            "brandColor": "#123456",
            "accentColor": "#654321",
        },
        "capabilityCompliance": {
            "enabled": [],
            "requiresConsent": False,
            "requiresPurchaseAuthority": False,
            "purchaseAuthority": None,
        },
    }
    meta = {"version_name": "1.0", "version_code": "1"}
    with contextlib.redirect_stdout(io.StringIO()):
        render_documents.write_documents(store, listing, ["en-US"], False, meta)
    return (
        (store / "review-notes.md").read_text(encoding="utf-8"),
        (store / "checklist.md").read_text(encoding="utf-8"),
    )


notes, checklist = generated_documents(PLACEHOLDER_URL)
check(
    f"node scripts/publish-privacy.mjs --slot {SLUG}" in notes,
    "the placeholder review-notes names the publishing command",
)
check(
    "Host `store/privacy.md` publicly" not in notes,
    "the placeholder review-notes no longer asks the owner to host the policy",
)
check(
    f"node scripts/publish-privacy.mjs --slot {SLUG}" in checklist,
    "the placeholder checklist names the publishing command",
)
check(
    "Replace placeholder contact email and privacy-policy URL" not in checklist,
    "the checklist no longer asks the owner to replace both fields",
)
check(
    "is published at" not in notes,
    "the placeholder case does not claim a policy that was never published",
)

notes, checklist = generated_documents(REAL_URL)
check(
    REAL_URL in notes and REAL_URL in checklist,
    "a real URL is named in both generated documents",
)
check(
    "still describes the shipped app" in notes and "still describes the shipped app" in checklist,
    "both documents keep the residual human act: the live policy still describing the app",
)
check(
    f"node scripts/publish-privacy.mjs --slot {SLUG}" in notes,
    "even a published policy names the command that updates it in place",
)
check(
    "is recorded" in notes
    and "is recorded" in checklist
    and "is published" not in notes
    and "is published" not in checklist,
    "the real-URL documents state what the read path measured (a recorded URL), not that the publisher ran",
)

failed = sum(1 for ok, _ in results if not ok)
print(f"\n{len(results) - failed}/{len(results)} checks passed")
print(f"VERDICT: {'PASS' if failed == 0 else f'FAIL ({failed})'}")
sys.exit(1 if failed else 0)
