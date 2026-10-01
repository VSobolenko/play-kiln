"""The pack's documents: listing.md, privacy, data safety, review notes, release notes, checklist.

Every generated document ends with a ``kiln:generated ...`` marker line, and a freshly generated release
note carries ``TODO-HUMAN``: deleting the marker keeps an author's own wording, and an unfinished note
reads as unfinished rather than as finished text (``requirements-preservation.md`` sections 7.5 and 13).
The markers live here because this module is the only producer of those documents. The compliance text
the documents carry -- per-capability credentials, the review-access paragraph, the product IDs read from
the slot's own Kotlin -- lives here too, because the documents are its only reader.
"""

from __future__ import annotations

import json
import re
import subprocess
import sys
import time
from pathlib import Path
from typing import Any, Callable

# The sibling modules are files beside this one, not an installed package: the store directory goes
# on `sys.path` before they are imported, the line `scripts/store/validate.py` also uses.
sys.path.insert(0, str(Path(__file__).parent))

from render_limits import (
    FEATURE_GRAPHIC_CANVAS,
    ICON_CANVAS,
    MAX_RELEASE_NOTE_CHARS,
    ROOT,
    SCREEN_TARGETS,
    pack_model,
)
from render_support import (
    RenderError,
    is_placeholder_email,
    is_placeholder_url,
    read_text,
)




# --------------------------------------------------------------------------------------------
# Capability compliance, read from the slot's own source
# --------------------------------------------------------------------------------------------

# The Kotlin declaration that tells Play which products this app buys.  The string literal in the
# slot's own `monetization/MonetizationConfig.kt` is the ID the Play Billing Library is actually
# handed, so the pack reads it from source rather than carrying a second copy that can drift away
# from what the app requests.  Both quote styles are accepted because Kotlin does not care which one
# a slot used; the backreference makes them a matched pair so a stray apostrophe cannot start a
# false match.
PRODUCT_ID_DECLARATION = re.compile(r"""\bProductId\(\s*(["'])([^"']+)\1\s*\)""")



# Only the shipped source set is read.  A `ProductId("fixture")` in `src/test/` is a fixture the
# shipped app never hands to the Play Billing Library, so counting it would put a product on the
# console checklist that does not exist -- a false instruction on the money path.
SOURCE_SCAN_RELATIVE = Path("app") / "src" / "main"



# Directories under the shipped source set that hold build output or IDE state rather than source.
# A generated or machine-local copy of a Kotlin file must not be read as a second declaration.
SOURCE_SCAN_SKIP_DIRS = frozenset({"build", ".gradle", ".kotlin", ".idea", ".cxx"})




def slot_kotlin_sources(slot: Path) -> list[str]:
    """The slot's own shipped Kotlin, as text, for the readers that read the app's declarations."""
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

    An empty list from a billing-enabled slot is a defect rather than a normal state: a purchase
    flow that references an unknown product fails in the reviewer's session.
    """
    found: set[str] = set()
    for text in slot_kotlin_sources(slot):
        found.update(match.group(2) for match in PRODUCT_ID_DECLARATION.finditer(text))
    return sorted(found)




def capability_compliance(slot: Path) -> dict[str, Any]:
    """The slot's enabled capabilities and what they imply, from the shared Node aggregator."""
    helper = ROOT / "scripts" / "lib" / "capabilities.mjs"
    result = subprocess.run(
        ["node", str(helper), "--root", str(ROOT), "--slot", str(slot)],
        capture_output=True,
        text=True,
        check=False,
    )
    if result.returncode != 0:
        detail = (result.stderr or result.stdout or "capability aggregator failed").strip()
        raise RenderError(detail.removeprefix("FAIL: ").strip())
    try:
        value = json.loads(result.stdout)
    except json.JSONDecodeError as exc:
        raise RenderError(f"capability aggregator returned invalid JSON: {exc.msg}") from exc
    if not isinstance(value, dict):
        raise RenderError("capability aggregator must return a JSON object")
    return value




# --------------------------------------------------------------------------------------------
# Credentials: what the reviewer needs, and the paragraph that says so
# --------------------------------------------------------------------------------------------
#
# The App access declaration and the «Access» paragraph of `store/review-notes.md` are one answer
# printed in two places, so they are one rule: a slot that sells an unlock gets the promo-code
# branch, a slot that gates content behind a sign-in gets the demo-account branch, and a slot with
# neither gets the branch that says nothing is required.  The rules are DATA read from the archived
# console reading (`docs/play-console-observations.json`, `credentials.rules` and
# `credentials.reviewAccess.paragraphsEn`); the archive is read by no program, so the vocabulary it
# established is kept here, where the document that carries it is written.
#
# `+` exists because two gates need their own words: a slot that both sells an unlock and requires a
# sign-in is not the union of the two single-gate instructions, and either one alone would assert
# something false about the other gate.

CREDENTIALS_RULES: tuple[dict[str, Any], ...] = (
    {
        "when": "capability.billing+capability.auth",
        "answer": "yes",
        "reasonEn": (
            "The slot both sells an unlock and gates content behind a sign-in, so the reviewer needs "
            "the code *and* a demo account, and a single-gate instruction would be false about the "
            "other gate."
        ),
    },
    {
        "when": "capability.billing",
        "answer": "yes",
        "reasonEn": (
            "The slot sells an unlock, so some content is behind a purchase. The console's «no» "
            "branch asserts that paying for content is not required, which this slot cannot assert "
            "truthfully; the reviewer therefore needs access given to them, which is a promo code in "
            "the instructions."
        ),
    },
    {
        "when": "capability.auth",
        "answer": "yes",
        "reasonEn": "The slot gates content behind a sign-in, so the reviewer needs demo credentials.",
    },
    {
        "when": "default",
        "answer": "no",
        "reasonEn": (
            "No enabled capability gates content behind a sign-in, a purchase, an invitation, or a "
            "code, so nothing has to be given to the reviewer."
        ),
    },
)



REVIEW_ACCESS_PARAGRAPHS: dict[str, str] = {
    "no": (
        "No account, payment, invitation, or demo credentials are required: no enabled capability "
        "gates content behind a sign-in, a purchase, an invitation, or a code."
    ),
    "yes": (
        "{reason}\n\nThe access itself is recorded in the console's App access declaration "
        "(`Контент приложения → Учетные данные`); the credential is deliberately not reproduced "
        "here, because it is access the slot hands out rather than text the pack publishes."
    ),
}




def credentials_rule(compliance: dict[str, Any]) -> dict[str, Any]:
    """Which radio the App access declaration takes, decided by the rule list above.

    The list states the conditions (`capability.<id>`, several joined with `+`, or `default`); this
    function only evaluates them, so which slot gets which answer is a declaration rather than a
    branch hidden in code.
    """
    enabled = {str(item) for item in (compliance.get("enabled") or [])}
    fallback: dict[str, Any] | None = None
    for rule in CREDENTIALS_RULES:
        when = str(rule["when"])
        # `default` is remembered rather than returned on sight: a rule that matches several
        # capabilities has to be reachable wherever it sits in the list.
        if when == "default":
            fallback = rule
            continue
        conditions = [part.strip() for part in when.split("+") if part.strip()]
        for condition in conditions:
            if not condition.startswith("capability."):
                raise RenderError(
                    f"CREDENTIALS_RULES has an unknown condition {condition!r}; use "
                    "'capability.<id>', several joined with '+', or 'default'"
                )
        if all(condition.split(".", 1)[1] in enabled for condition in conditions):
            return rule
    if fallback is None:
        raise RenderError("CREDENTIALS_RULES has no 'default' entry")
    return fallback




def review_access_paragraph(compliance: dict[str, Any]) -> str:
    """The «Access» paragraph of `store/review-notes.md`, from the rule that answered the console.

    A branch with no paragraph is a refusal rather than another branch's text: the «no» paragraph
    printed for a slot that gates something is a claim about a different app, and that is exactly
    the defect this replaced -- a slot that sold an unlock shipped reviewer notes stating that no
    payment or code was required while its own console declaration held a promo code.
    """
    rule = credentials_rule(compliance)
    answer = str(rule.get("answer") or "")
    paragraph = REVIEW_ACCESS_PARAGRAPHS.get(answer, "")
    if not paragraph.strip():
        raise RenderError(
            f"REVIEW_ACCESS_PARAGRAPHS carries no paragraph for the answer {answer!r} that "
            "CREDENTIALS_RULES gives this slot's enabled capabilities; the reviewer notes would "
            "have to state an access rule this app does not have"
        )
    text = paragraph.replace("{reason}", str(rule.get("reasonEn") or "").strip())
    if "{" in text or "}" in text:
        raise RenderError(
            f"REVIEW_ACCESS_PARAGRAPHS[{answer!r}] carries a placeholder this slot's rule cannot "
            "fill: only `{reason}` is substituted, from the answer's own `reasonEn`"
        )
    return text




# --------------------------------------------------------------------------------------------
# Generated documents
# --------------------------------------------------------------------------------------------

# Every generated document carries one of these on its last line.  Deleting the marker keeps an
# author's own wording: a run regenerates these files as a side effect, with no hint that it is about
# to replace prose someone wrote, and app-specific text is the expected outcome rather than an
# anomaly.  The markers live here because this file is the only producer of those documents.
REVIEW_NOTES_MARKER = "<!-- kiln:generated notes - delete this line to keep your own wording -->"


PRIVACY_MARKER = "<!-- kiln:generated policy - delete this line to keep your own wording -->"


DATA_SAFETY_MARKER = "<!-- kiln:generated safety answers - delete this line to keep your own wording -->"


RELEASE_NOTES_MARKER = "<!-- kiln:generated release notes - delete this line to keep your own wording -->"



# A freshly generated release note is a prompt, not text, and the prompt has to be visible as
# unfinished rather than reading like a finished sentence: the failure being fixed is a pack that
# looked complete while a field Play demands had no prepared text at all.  The prompts and the
# per-language cap come from the model.
RELEASE_NOTE_PROMPTS: dict[str, str] = pack_model.release_note_prompts()


RELEASE_NOTE_UNFINISHED = "TODO-HUMAN"




def markdown_escape(value: Any) -> str:
    return str(value).replace("|", "\\|")




def write_preserving(path: Path, content: str, marker: str) -> None:
    """Write generator output, but never over a file that was written by hand.

    The generated wording is necessarily generic -- it cannot know how this particular app is
    reviewed, what a teacher's record actually contains, or which SDK contributed a permission -- so
    app-specific text is the expected outcome.  Content without the generator marker is therefore
    treated as a human edit and kept, the same way the listing's copy is preserved rather than
    clobbered.  Delete the file, or restore the marker line, to get generator output back.

    This matters most for ``privacy.md`` and ``data-safety.md``: a policy that does not match the
    merged manifest is a compliance defect, and the generator cannot read the manifest, so the
    hand-written version is the one that has to survive.
    """
    if path.is_file() and marker not in read_text(path):
        print(f"PRESERVE {path.name} - hand-written, not generator output")
        return
    path.write_text(content, encoding="utf-8")




def billing_checklist_lines(product_ids: list[str]) -> list[str]:
    """Console steps that block a first launch of a billing app, with the real IDs substituted.

    Each of the three is invisible until a purchase is attempted for real: a product that was never
    created, a purchase option left in Draft -- Play's own wording is that it "isn't returned by the
    Play Billing Library", so the app behaves as if the product does not exist -- and a test account
    that is not a *license tester*, whose purchase is charged real money.  The IDs come from the
    slot's own source, not from a constant here.
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




# The release screen's two fields, as the document contract that names them.  Play's own format and
# the per-language cap come from `scripts/store/model.py`, so the reader here and the pack verdict in
# `scripts/store/validate.py` cannot measure different characters.  The *name* is checked against the
# bundle the console will show it beside (`dist/release-manifest.json`), because a name that
# disagrees with the upload makes the console's release list lie about which build is which.
RELEASE_NAME_LINE = re.compile(r"^\*\*Use:\*\*\s*`([^`]+)`\s*$", re.MULTILINE)




def parse_release_notes(path: Path) -> tuple[str, dict[str, str]]:
    """``(release name, {locale: note})`` from `store/release-notes.md`."""
    if not path.is_file():
        return "", {}
    text = read_text(path)
    match = RELEASE_NAME_LINE.search(text)
    return (match.group(1).strip() if match else ""), pack_model.parse_release_notes_text(text)




def read_release_manifest(slot: Path) -> dict[str, Any] | None:
    path = slot / "dist" / "release-manifest.json"
    if not path.is_file():
        return None
    try:
        value = json.loads(read_text(path))
    except (json.JSONDecodeError, OSError) as exc:
        raise RenderError(f"{path} is unreadable: {exc}") from exc
    return value if isinstance(value, dict) else None




def validate_release_notes(
    store: Path, release_name: str, notes: dict[str, str], locales: list[str]
) -> list[str]:
    """Why the release fields disagree with the bundle or with Play's limits, or no failures.

    This is the **document contract** -- the release name the console list will show beside the
    bundle, and Play's per-language cap -- not the pack's verdict, which is
    ``scripts/store/validate.py``'s independent measurement of the committed pack.  Both read the
    format and the cap from the model.
    """
    failures: list[str] = []
    slot = store.parent
    manifest = read_release_manifest(slot)
    if manifest is not None:
        expected = f"{manifest.get('versionName', '')} ({manifest.get('versionCode', '')})"
        if release_name != expected:
            failures.append(
                f"the release name is {release_name or 'empty'!r} but the uploaded bundle is "
                f"{expected!r}; the console list would misname the build"
            )
    elif not release_name:
        failures.append("no release name is stated, so the console field would be filled from memory")
    for locale, note in sorted(notes.items()):
        # Every tag in the block is measured, including a locale the listing does not declare: text
        # in the block is text a human may paste, and Play truncates it silently.
        overflow = pack_model.release_note_overflow(locale, note)
        if overflow:
            failures.append(overflow)
    for locale in locales:
        if locale not in notes:
            failures.append(
                f"the listing declares {locale} and the paste block carries no note for it, so the "
                f"console's box would be pasted empty for that language"
            )
    if not notes:
        failures.append("no `<locale>`-tagged note was found, tags on their own lines")
    return failures




def write_documents(
    store: Path,
    listing: dict[str, Any],
    locales: list[str],
    synthetic: bool,
    meta: dict[str, str],
) -> None:
    """Write every pack document: listing, privacy, data safety, review notes, release notes, checklist."""
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
    # replace a placeholder is itself wrong once the value is real, and it teaches the reader to skim
    # the list.
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

    # Play asks for the release screen's two fields at upload time, which is the worst moment to be
    # composing them: the release name must agree with the bundle already uploaded, and each note has
    # a character limit that is only visible when the paste is rejected. Seeding the name from the
    # Gradle version means it agrees by construction, and the tagged block is Play's own
    # multi-language format, copied whole into the one text box the console shows.
    notes = {
        locale: f"{RELEASE_NOTE_UNFINISHED}: {RELEASE_NOTE_PROMPTS.get(locale, RELEASE_NOTE_PROMPTS['en-US'])}"
        for locale in locales
    }
    tagged = pack_model.release_notes_paste_block(notes)
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

{tagged}

Select everything inside the block above and paste it into Play's release-notes box as one piece —
this is Play's own format, not a representation of it, so there is nothing to reassemble. Play caps
each language at {MAX_RELEASE_NOTE_CHARS} characters and truncates silently, so every note in the
block is measured when the pack is validated. Replace each `{RELEASE_NOTE_UNFINISHED}` line with the
real note, keeping its `{pack_model.release_note_locale_tag(locales[0])}` and `</{locales[0]}>` lines
exactly where they are; a language with no real note yet can simply keep a short one or be left out of
the block.

{RELEASE_NOTES_MARKER}
"""
    write_preserving(store / "release-notes.md", release_notes, RELEASE_NOTES_MARKER)

    status = (
        "synthetic — replace with real captures before publish"
        if synthetic
        else "framed from real device captures"
    )
    # A slot with store/studio.json has its media rendered by scripts/store-studio.py, which composes
    # marketing images rather than pasting the capture into a rounded rectangle. The generic
    # inventory below would describe a pack that no longer exists, so the inventory is written by the
    # same condition that decides which renderer runs.
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
canvas and the 7-inch canvas are the same size. That follows the console's own rule rather than
declining it.
"""
    else:
        provenance = """Every locale folder also contains its own icon and feature graphic. The feature graphic keeps titles and taglines inside a generous safe area because Play may crop edges on some surfaces.
"""
    # The rows are derived from SCREEN_TARGETS rather than typed beside it: a table listing sizes that
    # the renderer no longer writes is a document that lies about the pack.
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
| `assets/icon-{ICON_CANVAS[0]}.png` | {ICON_CANVAS[0]}×{ICON_CANVAS[1]} | RGBA Play store icon; full-bleed and opaque, no transparent padding |
| `assets/{pack_model.shared_asset_name('feature-graphic')}` | {FEATURE_GRAPHIC_CANVAS[0]}×{FEATURE_GRAPHIC_CANVAS[1]} | RGB feature graphic |
{screenshot_rows}

{provenance}
Four screenshots are supplied for both tablet formats. This satisfies the large-format eligibility asset count; eligibility also depends on the app's actual large-screen quality and Play requirements at submission time.
"""
    (store / "graphics.md").write_text(graphics, encoding="utf-8")

    checklist_lines = [
        f"# Google Play upload checklist — {listing['appName']}",
        "",
        f"- [ ] Run `python scripts/store/validate.py --slot {listing['slug']}` and resolve every FAIL.",
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
    # generated from the IDs the app actually declares, and only for a slot that sells something - a
    # non-billing slot must not be handed steps about products it does not sell.
    if "billing" in capability_compliance.get("enabled", []):
        checklist_lines += billing_checklist_lines(declared_product_ids(store.parent))
    checklist_lines += [
        f"- [ ] Use `node scripts/play-upload.mjs --slot {listing['slug']} --track internal --dry-run` before any live upload.",
        "- [ ] Obtain explicit human approval before committing a Play edit.",
    ]
    (store / "checklist.md").write_text("\n".join(checklist_lines) + "\n", encoding="utf-8")
