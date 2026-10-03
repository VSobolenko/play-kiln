"""The pack's documents: listing.md, privacy, data safety, review notes, release notes, checklist.

Every generated document ends with a ``kiln:generated ...`` marker line, and a freshly generated release
note carries ``TODO-HUMAN``: deleting the marker keeps an author's own wording, and an unfinished note
reads as unfinished rather than as finished text (``requirements-preservation.md`` sections 7.5 and 13).
The markers live here because this module is the only producer of those documents. The compliance text
the documents carry -- per-capability credentials, the review-access paragraph, the product IDs read from
the slot's own Kotlin -- lives here too, because the documents are its only reader.
"""

from __future__ import annotations

import argparse
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

# The Kotlin declarations that tell Play which products this app buys.  The pack reads them from the
# slot's own source rather than carrying a second copy that can drift away from what the app
# requests.  There are two shapes, because a slot writes its id in whichever one keeps it greppable:
#
#   * `ProductId("some_id")` -- the id handed straight to the Play Billing Library at its call site,
#     so the string literal is the id and is read where it is used.  Both quote styles are accepted
#     because Kotlin does not care which one a slot used; the backreference makes them a matched pair
#     so a stray apostrophe cannot start a false match.
#   * `const val UNLOCK_PRODUCT_ID: String = "some_id"` -- the id named once as a constant, so one
#     value feeds the purchase flow, the restore path and the listing and a typo is greppable rather
#     than an `ITEM_UNAVAILABLE` in front of a paying user.  What makes it a product id is the name,
#     not the value: the constant has to be named `PRODUCT_ID`, so this reader does not mistake every
#     string constant in the app for a product.
#
# The constant form's type annotation is optional (`: String`), because Kotlin infers it from the
# literal.
PRODUCT_ID_DECLARATION = re.compile(r"""\bProductId\(\s*(["'])([^"']+)\1\s*\)""")
PRODUCT_ID_CONSTANT = re.compile(
    r"""\b(?:const\s+)?val\s+\w*PRODUCT_ID\w*\s*(?::\s*String\s*)?=\s*(["'])([^"'\r\n]+)\1""",
    re.IGNORECASE,
)



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

    Both shapes above are read: the `ProductId("...")` handed to the billing library, and the
    `PRODUCT_ID`-named constant a slot declares once.  An empty list from a billing-enabled slot is a
    defect rather than a normal state: a purchase flow that references an unknown product fails in
    the reviewer's session.
    """
    found: set[str] = set()
    for text in slot_kotlin_sources(slot):
        found.update(match.group(2) for match in PRODUCT_ID_DECLARATION.finditer(text))
        found.update(match.group(2) for match in PRODUCT_ID_CONSTANT.finditer(text))
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




# --------------------------------------------------------------------------------------------
# The console runbook: this slot's own click-by-click submission document
# --------------------------------------------------------------------------------------------
#
# The factory has one global console reference (`docs/play-console-checklist.md`) and, until now,
# every app read it and re-decided every answer from scratch at release time.  This document is the
# opposite: the sequence of controls, with this slot's answer already in place, so neither an agent
# nor the owner re-derives anything.  It is deliberately **not** the retired `console-answers.md`,
# which was a copy of what the pack already said (`PACK_DOCUMENTS` in `scripts/store/model.py` and
# `docs/factory-checks.md` record the retirement): its value is the action order plus the concrete
# value, and where a value cannot be derived it says so and names the declaration that would settle
# it rather than inventing one.
#
# The answers that are a property of the **catalogue** rather than of one slot -- the three
# declarations every factory app answers «no» -- are carried here as DATA, read off the archived
# console reading (`docs/play-console-observations.json`, the `government` / `financial` / `health`
# declarations' own `whyEn`), which no program reads.  This module is where the document that prints
# them is written, the same reason `CREDENTIALS_RULES` lives here.

CONSOLE_RUNBOOK_MARKER = (
    "<!-- kiln:generated console runbook - delete this line to keep your own wording -->"
)


# Declarations whose answer is the catalogue's, not this slot's, in the console's own screen order.
# `page` is the RU console breadcrumb; `answer` is the console's own Russian token.
CATALOGUE_DECLARATIONS: tuple[dict[str, str], ...] = (
    {
        "name": "Приложения для здоровья",
        "page": "Контент приложения → Приложения для здоровья",
        "answer": "Нет",
        "reason": (
            "Factory slots are work tools: they measure no health data and handle no medical "
            "records, so the «no» is a property of the catalogue rather than of this app "
            "(the archived console reading's `health` declaration)."
        ),
    },
    {
        "name": "Финансовые функции",
        "page": "Контент приложения → Финансовые функции",
        "answer": "Нет",
        "reason": (
            "A factory slot sells at most an in-app unlock and Google Play handles the payment "
            "itself, which does not make the app a financial product (the archived console "
            "reading's `financial` declaration)."
        ),
    },
    {
        "name": "Приложения государственных учреждений",
        "page": "Контент приложения → Приложения государственных учреждений",
        "answer": "Нет",
        "reason": (
            "A «yes» asserts a government affiliation and moves the app into a different review "
            "path, and this factory ships no such app (the archived console reading's `government` "
            "declaration)."
        ),
    },
)


# The content-rating questionnaire's step-1 category radio, from the archived reading's own
# `optionsRu` and `optionIds`.  The branch named by `listing.contentRating.questionnaire` picks one.
CONTENT_RATING_CATEGORIES: dict[str, str] = {
    "game": "Игра",
    "social": "Социальные сети или связь",
    "other": "Все другие типы приложений",
}


# Step 2 of the questionnaire, in the console's own question order, with the observed `notes`
# branch's answers and its one-line reason per row (the archived reading's
# `contentRating.questionnaires.notes.answers`).  `capability` names the one row whose answer is not
# a constant: a slot that enables that capability sells a digital good and the console requires
# «Да».
CONTENT_RATING_QUESTIONNAIRE: tuple[dict[str, str], ...] = (
    {
        "label": "Скачанное приложение · содержимое с возрастными ограничениями",
        "token": "Нет",
        "why": "The package ships the interface and nothing else: no sex, violence, profanity, drugs or gambling content.",
    },
    {
        "label": "Общий доступ к пользовательскому контенту · обмен сообщениями, файлами, медиа",
        "token": "Нет",
        "why": "The app has no accounts and no transport, so what the user types stays in local storage.",
    },
    {
        "label": "Интернет-содержимое · продвижение или потоковая передача контента извне",
        "token": "Нет",
        "why": "Nothing is streamed or fetched: the app is offline by construction.",
    },
    {
        "label": "Продвижение или продажа товаров и видов деятельности с возрастными ограничениями",
        "token": "Нет",
        "why": "The app neither sells nor promotes alcohol, tobacco, firearms or gambling; its only product is a digital feature unlock.",
    },
    {
        "label": "Другое · передача точного физического местоположения пользователя",
        "token": "Нет",
        "why": "The app declares no location permission and has no recipient to share a location with.",
    },
    {
        "label": "Другое · покупка цифровых товаров внутри приложения",
        "capability": "billing",
        "token": "Да",
        "otherwise": "Нет",
        "why": "The `billing` capability sells a one-time unlock through Google Play Billing, which is a digital good, so the console requires «Да».",
        "otherwise_why": "No enabled capability sells anything, so there is no digital good to declare.",
    },
    {
        "label": "Другое · покупки со случайными элементами (лутбоксы)",
        "token": "Нет",
        "why": "A purchase is deterministic: the buyer receives exactly what the purchase screen describes.",
    },
    {
        "label": "Другое · денежные вознаграждения, подарочные карты, криптовалюта, NFT",
        "token": "Нет",
        "why": "The app pays the user nothing and issues no tokens, gift cards or digital assets.",
    },
    {
        "label": "Другое · веб-браузер или поисковая система",
        "token": "Нет",
        "why": "The app opens no arbitrary address and searches nothing.",
    },
    {
        "label": "Другое · приложение главным образом новостное или обучающее",
        "token": "Нет",
        "why": "It is a record-keeping tool rather than a source of knowledge or news.",
    },
)


# The target-audience wizard branch, from the archived reading's `targetAudience.audiences`.  The
# value of `listing.contentRating.targetAudience` names the branch, and the branch decides both the
# age groups the console ticks and the steps it skips -- neither is guessed here.
TARGET_AUDIENCE_BRANCHES: dict[str, dict[str, Any]] = {
    "adults-only": {
        "checkboxes": ["От 18 лет"],
        "minorGate": True,
        "skippedSteps": ["О приложении", "Реклама", "Страница приложения"],
        "why": "The app is not intended for children; entering Families pulls in a separate policy set.",
    },
}


def _slot_declaration(slot: Path, name: str) -> dict[str, Any]:
    """A slot-level JSON declaration (`naming.json`, `economics.json`), or ``{}`` when absent."""
    path = slot / name
    if not path.is_file():
        return {}
    try:
        value = json.loads(read_text(path))
    except (json.JSONDecodeError, OSError):
        return {}
    return value if isinstance(value, dict) else {}


def _runbook_paths(kind: str) -> str:
    """The `store/`-relative asset paths the pack ships for one screenshot ``kind``."""
    count = pack_model.shipped_count(kind)
    first = pack_model.screenshot_asset_name(kind, 1)
    last = pack_model.screenshot_asset_name(kind, count)
    if count == 1:
        return f"`store/assets/<locale>/{first}`"
    return f"`store/assets/<locale>/{first}` … `{last}`"


def console_runbook(
    store: Path,
    listing: dict[str, Any],
    locales: list[str],
    meta: dict[str, str],
) -> str:
    """This slot's own Play Console runbook: the click sequence with the answers already in place.

    Every answer is derived from the slot's own declarations -- `store/listing.json`,
    `capabilities.json` and the enabled `capabilities/<id>/capability.json`, `naming.json`,
    `economics.json`, `store/release-notes.md`, `store/data-safety.md` and the shipped Kotlin -- and
    a value that cannot be derived is named as a decision a human has to make, with the declaration
    that would settle it.  Nothing is typed in here: the caps and limits live in
    `scripts/store/model.py` / `scripts/lib/play-assets.json`, and the global reference
    (`docs/play-console-checklist.md`) is cross-referenced rather than duplicated.
    """
    slot = store.parent
    app_name = str(listing.get("appName") or meta.get("app_name") or slot.name)
    application_id = str(listing.get("applicationId") or meta.get("application_id") or "")
    version_name = str(meta.get("version_name") or "")
    version_code = str(meta.get("version_code") or "")
    version = f"{version_name} ({version_code})" if version_code else version_name or "not declared"
    idea = str(listing.get("idea") or meta.get("idea") or "").strip()
    slug = str(listing.get("slug") or slot.name)

    compliance = listing.get("capabilityCompliance")
    if not isinstance(compliance, dict):
        compliance = {}
    enabled = [str(item) for item in (compliance.get("enabled") or [])]
    enabled_text = ", ".join(f"`{item}`" for item in enabled) if enabled else "none"
    safety = listing.get("dataSafety") if isinstance(listing.get("dataSafety"), dict) else {}
    ads = listing.get("ads") if isinstance(listing.get("ads"), dict) else {}
    content_rating = (
        listing.get("contentRating") if isinstance(listing.get("contentRating"), dict) else {}
    )
    contact_email = str(listing.get("contactEmail") or "")
    privacy_url = str(listing.get("privacyPolicyUrl") or "")
    category_token = str(listing.get("category") or "")
    category_labels = (
        listing.get("categoryLabels") if isinstance(listing.get("categoryLabels"), dict) else {}
    )

    naming = _slot_declaration(slot, "naming.json")
    economics = _slot_declaration(slot, "economics.json")
    release_name, notes = parse_release_notes(store / "release-notes.md")
    product_ids = declared_product_ids(slot)
    credentials = credentials_rule(compliance)

    # Every value this document cannot derive is collected here and printed as its own section, so a
    # reader sees the questions rather than a plausible-looking guess in the middle of the answers.
    decisions: list[str] = []

    out: list[str] = []
    add = out.append

    add(f"# Play Console runbook — {app_name}")
    add("")
    add(f"**{app_name}** · `{application_id}` · **{version}**")
    if idea:
        add("")
        add(f"_{idea}_")
    add("")
    add(
        "This is this app's own submission document: the console controls to press, in order, with "
        "the answer already derived from the slot's declarations. It is not a copy of the pack — the "
        "pack's prose lives in `store/listing.md`, `store/data-safety.md` and `store/review-notes.md`. "
        "The global reference for every field and option is `docs/play-console-checklist.md`; this file "
        "does not restate its caps, and every character cap it names is resolved in "
        "`scripts/store/model.py` / `scripts/lib/play-assets.json`."
    )
    add("")
    add(f"- Enabled capabilities: {enabled_text} (`capabilities.json`)")
    add(
        f"- Reward path: `{economics.get('archetype')}` (`economics.json`)"
        if economics.get("archetype")
        else "- Reward path: no `economics.json` archetype in this slot"
    )
    add(f"- Recorded name decision: `{naming.get('title') or app_name}` (`naming.json`)")
    add(
        "- Page markers: `[console]` a console action, `[verify]` a read-only check, `[human]` the "
        "owner's own act."
    )
    add("")
    add("---")

    # ------------------------------------------------------------------ preconditions
    add("")
    add("## 1. Preconditions — clear these or the submit control stays disabled")
    add("")
    add(
        "While any dashboard task is open, the publishing overview's only control stays disabled and "
        "prints `Чтобы отправить изменения на рассмотрение, выполните необходимые действия на панели "
        "управления приложением.`"
    )
    add("")
    add(
        "- [ ] `[human]` **The bundle file is in the app's library.** `Выпуски` → the release form's "
        "`Наборы App Bundle` → the neighbouring `Загрузить` control takes the AAB file itself, so if this "
        "version is not yet listed, the owner uploads it once — see §6."
    )
    add(
        "- [ ] `[console]` **The two first-publication declarations.** `Панель управления` → the `Заявки` "
        "dialog → tick `Подтвердить, что приложение соответствует Правилам программы для разработчиков` "
        "and `Принять экспортное законодательство США`. The agent ticks them **only when the slot carries "
        "a recorded release approval**, because the approval *is* the human checkpoint and the "
        "attestation is the owner's, not the click (`docs/play-console-checklist.md` §1.A — the owner's "
        "decision, recorded 2026-10-03). Accepted once per app; nothing in the pack carries them."
    )
    add(
        "- [ ] `[human]` **The five store-graphic fields.** `Магазин → Основная информация о магазине` → "
        "upload the icon, the feature graphic and the phone / 7-inch / 10-inch screenshot sets — see §3. "
        "The sandboxed browser cannot set a `file` input, so this upload is the owner's."
    )
    add(
        f"- [ ] `[verify]` **The privacy-policy URL is real and reachable.** The listing carries "
        f"`{privacy_url}` (`store/listing.json.privacyPolicyUrl`); the same URL must open from inside the "
        "app (`scripts/check-in-app-policy.mjs`)."
    )
    add(
        "- [ ] `[verify]` The dashboard counter reads `Выполнено: N из 11` — nine `Контент приложения` "
        "rows plus the two store rows (`Выберите категорию приложения и укажите контактную информацию`, "
        "`Настройте страницу приложения`), and it deliberately omits `Рекламный идентификатор`. It is "
        "**not** the declaration count (§2)."
    )
    add("")
    add("---")

    # ------------------------------------------------------------------ declarations
    add("")
    add("## 2. App-content declarations — `Контент приложения → Обзор`")
    add("")
    add(
        "Two tabs: `Требуются действия` (default) and `Обработано`, whose heading prints `N обработанных "
        "деклараций`. Ten declarations, each row's action `Настроить`, listed below in the console's own "
        "screen order."
    )
    add("")

    # 1. Data safety
    collects = bool(safety.get("collectsData"))
    type_rows = safety.get("dataTypes") if isinstance(safety.get("dataTypes"), list) else []
    add("### Безопасность данных — `Контент приложения → Безопасность данных`")
    add("")
    add("| Form field | Answer | Source / reason |")
    add("|---|---|---|")
    add(
        f"| `Сбор и передача данных` (`collectsOrShares`) | **{'Да' if collects else 'Нет'}** | "
        f"`store/listing.json.dataSafety.collectsData` — {safety.get('reason') or 'no reason declared'} |"
    )
    transit = "Не применимо — данные не собираются" if not collects else (
        "Да" if safety.get("encryptedInTransit") else "Нет"
    )
    add(
        f"| `Шифрование при передаче` (`encryptedInTransit`) | **{transit}** | "
        "`store/listing.json.dataSafety.encryptedInTransit` |"
    )
    add(
        "| `Аккаунты` (`accountCreation`) | **Нет** | `store/review-notes.md` — the app creates no "
        "account and has no sign-in |"
    )
    add(
        "| `Удаление данных` (`dataDeletion`) | **Механизм не требуется** | There is no app-side account "
        "to delete; stored records are editable and deletable in the app |"
    )
    add(
        "| `Эти данные обрабатываются временно?` (`temporaryProcessing`) | **Нет** | The entitlement is "
        "kept on the device so the unlock survives a reinstall (`store/data-safety.md`) |"
    )
    add("")
    if type_rows:
        add("`Категории данных` (`dataCategories`) — one row per declared type:")
        add("")
        add("| Category | Collected | Shared | Required | Purpose |")
        add("|---|---|---|---|---|")
        for item in type_rows:
            required = "No" if item.get("optional") else "Yes"
            add(
                f"| {markdown_escape(item.get('type', ''))} | Yes | "
                f"{'Yes' if item.get('shared') else 'No'} | {required} | "
                f"{markdown_escape(item.get('purpose', ''))} |"
            )
        add("")
        sdk_rows = (
            safety.get("thirdPartySdks") if isinstance(safety.get("thirdPartySdks"), list) else []
        )
        if sdk_rows:
            add(
                "The rows above are the bundled SDKs, not the app's own code: "
                + ", ".join(f"{sdk.get('name', '?')} {sdk.get('version', '?')}" for sdk in sdk_rows)
                + ". Under-declaring a bundled SDK is a policy defect, so this list and the built AAB "
                "must agree."
            )
    else:
        add("`Категории данных` (`dataCategories`) — **no data category is declared**.")
    add("")
    add("- [ ] `[console]` Answer the form sections above, then save the declaration.")
    add("")

    # 2-4. The catalogue declarations
    for declaration in CATALOGUE_DECLARATIONS:
        add(f"### {declaration['name']} — `{declaration['page']}`")
        add("")
        add(f"- [ ] `[console]` Answer **{declaration['answer']}** — {declaration['reason']}")
        add("")

    # 5. Advertising ID
    ads_present = bool(ads.get("containsAds"))
    add("### Рекламный идентификатор — `Контент приложения → Рекламный идентификатор`")
    add("")
    if ads_present:
        decisions.append(
            "`Рекламный идентификатор` — the slot declares `listing.ads.containsAds = true`, and whether "
            "the built AAB reads the advertising ID is a fact about the artifact the pack does not "
            "describe. Read the merged release manifest / AAB and answer it by hand."
        )
        add(
            "- [ ] `[console]` **NEEDS-DECISION** — this slot declares advertising "
            "(`store/listing.json.ads.containsAds`), and whether its bundle reads the advertising ID is a "
            "fact about the built AAB. See §7."
        )
    else:
        add(
            "- [ ] `[console]` Answer **Нет** — `store/listing.json.ads.containsAds` is false and no "
            "enabled capability ships an advertising SDK. Answering «no» is a claim about the contents of "
            "the AAB: adding such an SDK later means changing this declaration with the build."
        )
    add("")

    # 6. Target audience
    audience_token = str(content_rating.get("targetAudience") or "")
    add("### Целевая аудитория и возрастные ограничения — `Контент приложения → Целевая аудитория`")
    add("")
    audience = TARGET_AUDIENCE_BRANCHES.get(audience_token)
    if audience:
        boxes = ", ".join(f"`{box}`" for box in audience["checkboxes"])
        add(f"- [ ] `[console]` `Какие целевые возрастные группы у вашего приложения?` → tick {boxes}.")
        if audience.get("minorGate"):
            add(
                "- [ ] `[console]` Tick the optional minor-access checkbox `Запретить доступ к моему "
                "приложению пользователям, которые указали в аккаунте Google, что они несовершеннолетние` "
                "— the branch ticks it because the declared audience is adults. It closes both the "
                "download and the in-app purchase to accounts marked as children."
            )
        skipped = ", ".join(f"`{step}`" for step in audience["skippedSteps"])
        add(
            f"- [ ] `[verify]` On this branch the wizard **skips** {skipped}: `Далее` opens `Сводка` "
            "directly. Two screens are filled, not five — a run that walks four steps has answered for a "
            "different app."
        )
        add(f"  - Branch source: `store/listing.json.contentRating.targetAudience = \"{audience_token}\"`.")
        add(f"  - {audience['why']}")
    elif not audience_token:
        decisions.append(
            "`Целевая аудитория и возрастные ограничения` — `listing.contentRating.targetAudience` is "
            "empty. Name a branch in `store/listing.json`; the branch decides both the age groups the "
            "console ticks and the steps it skips, so neither is guessed here."
        )
        add(
            "- [ ] `[console]` **NEEDS-DECISION** — `listing.contentRating.targetAudience` is empty. "
            "See §7."
        )
    else:
        decisions.append(
            f"`Целевая аудитория` — the token `{audience_token}` is not in the observed branch table "
            f"({', '.join(sorted(TARGET_AUDIENCE_BRANCHES)) or 'none'}); the branch is unobserved, so its "
            "age groups and skipped steps are not printed as if they were read."
        )
        add(
            f"- [ ] `[console]` **NEEDS-DECISION** — `contentRating.targetAudience = \"{audience_token}\"` "
            "is a branch nobody read. See §7."
        )
    add(
        f"- [ ] `[verify]` Step 1 field `Электронный адрес` = `{contact_email}` "
        "(`store/listing.json.contactEmail`)."
    )
    add("")

    # 7. Content rating
    branch = str(content_rating.get("questionnaire") or "")
    add("### Возрастные ограничения — `Контент приложения → Возрастные ограничения`")
    add("")
    add("Strip: `1 Категория` / `2 Анкета` / `3 Сводка`.")
    add("")
    add(
        f"- [ ] `[console]` Step 1 `Электронный адрес` = `{contact_email}`; tick the IARC checkbox `Я "
        "принимаю условия использования Международной коалиции возрастной классификации (IARC)` — without "
        "it the console does not open step 2."
    )
    if branch == "notes":
        # The observed `notes` branch is the only one read; its category and its ten answers travel
        # with it, so a slot on that branch gets a concrete answer for every row.
        add(
            "- [ ] `[console]` Step 1 category radio → "
            f"**`{CONTENT_RATING_CATEGORIES['other']}`** (the `notes` branch, "
            "`store/listing.json.contentRating.questionnaire`)."
        )
        add("- [ ] `[console]` Step 2 `Анкета` — answer the ten questions:")
        add("")
        add("  | Question | Answer | Reason |")
        add("  |---|---|---|")
        for question in CONTENT_RATING_QUESTIONNAIRE:
            capability = question.get("capability")
            if capability and capability in enabled:
                token, why = question["token"], question["why"]
            elif capability:
                token = question.get("otherwise", "Нет")
                why = question.get("otherwise_why", "")
            else:
                token, why = question["token"], question["why"]
            add(f"  | {markdown_escape(question['label'])} | **{token}** | {markdown_escape(why)} |")
        add("")
        add(
            "  - `q-random-elements` appears only after «Да» on the digital-goods row; answer it **Нет** "
            "— a purchase here is deterministic."
        )
        add(
            "- [ ] `[verify]` Step 3 `Сводка` is what `store/listing.json.contentRating` is checked "
            "against; confirm it matches the table above before saving."
        )
    elif branch:
        decisions.append(
            f"`Возрастные ограничения` — the questionnaire branch `{branch}` was never read, so neither "
            "its step-1 category radio nor its ten step-2 answers are printed as if they were."
        )
        add(
            f"- [ ] `[console]` **NEEDS-DECISION** — `contentRating.questionnaire = \"{branch}\"` is a "
            "branch nobody read. See §7."
        )
    else:
        decisions.append(
            "`Возрастные ограничения` — `listing.contentRating.questionnaire` is empty, so neither the "
            "step-1 category radio nor the ten step-2 answers are known. Name a branch in "
            "`store/listing.json`."
        )
        add("- [ ] `[console]` **NEEDS-DECISION** — `contentRating.questionnaire` is empty. See §7.")
    add("")

    # 8. App access
    add("### Учетные данные — `Контент приложения → Учетные данные`")
    add("")
    add("Radio `В вашем приложении есть закрытые разделы?` (`hasRestrictedSections`).")
    if str(credentials.get("answer")) == "yes":
        add(
            "- [ ] `[console]` Choose **`Да`** and tick the line the credential dialog explains *this* "
            "app through — `Платежи, например за контент для однократных покупок, подписки или уровни "
            "доступа.` The «no» branch asserts no payment is required, which would be false here."
        )
        add(f"  - Rule (`CREDENTIALS_RULES`): {credentials.get('reasonEn')}")
        add(
            "- [ ] `[console]` Open `Добавить данные для входа` and put the **promo code** in `Другие "
            "данные, которые требуются для входа` (not in the username/password fields). The code itself "
            "is harvested from the console, never from the pack."
        )
        add(
            "- [ ] `[console]` Tick the certification checkbox `Учетные данные, указанные в этой "
            "декларации, обеспечивают полный доступ ко всем функциям и контенту приложения, включая "
            "платный` — instructions and the checkbox save **together**; without it the dialog answers "
            "`Не удалось сохранить изменения`."
        )
        add(
            "- [ ] `[verify]` Create the promo code first (§4): `Монетизация с помощью Play → Promotions` "
            "refuses while the product list is empty."
        )
    else:
        add(f"- [ ] `[console]` Choose **`Нет`** — {credentials.get('reasonEn')}")
    add(
        "- [ ] `[console]` The reviewer's own instructions are `store/review-notes.md` § \"Access\"; it is "
        "generated from the same rule, so the page and the pack state one answer."
    )
    add("")

    # 9. Ads
    add("### Реклама — `Контент приложения → Реклама`")
    add("")
    ads_answer = (
        "Да, мое приложение содержит рекламу"
        if ads_present
        else "Нет, мое приложение не содержит рекламу"
    )
    ads_reason = f"; {ads.get('reason').rstrip('.')}" if ads.get("reason") else ""
    add(
        f"- [ ] `[console]` Radio `Реклама` → **`{ads_answer}`** — `store/listing.json.ads.containsAds` "
        f"is `{str(ads_present).lower()}`{ads_reason}."
    )
    add("")

    # 10. Privacy policy
    add("### Политика конфиденциальности — `Контент приложения → Политика конфиденциальности`")
    add("")
    add(f"- [ ] `[console]` Field `URL политики конфиденциальности` = `{privacy_url}`.")
    add(
        "- [ ] `[verify]` The same URL must be reachable from inside the app; the console's own page "
        f"states the link is required in both places. `node scripts/check-in-app-policy.mjs --slot {slug}` "
        "measures the in-app half."
    )
    add("")
    add("---")

    # ------------------------------------------------------------------ store listing
    add("")
    add("## 3. Store listing — `Магазин → Основная информация о магазине`")
    add("")
    add("The text fields an agent pastes, with the value and where it comes from:")
    add("")
    add("| Console field | Value for this app | Source |")
    add("|---|---|---|")
    recorded_label = category_labels.get(category_token)
    category_label = (
        str(recorded_label.get("labelRu") or "").strip()
        if isinstance(recorded_label, dict)
        else ""
    )
    if category_label:
        category_cell = f"`{category_label}`"
    else:
        category_cell = f"token `{category_token}` — **NEEDS-DECISION** (§7)"
        known = ", ".join(sorted(category_labels)) if category_labels else "none"
        decisions.append(
            f"`Категория приложения` — `listing.category` carries the token `{category_token}`, which "
            f"`store/listing.json.categoryLabels` does not hold (it holds: {known}). Read the category's "
            "label in the console and record the pair."
        )
    add(f"| `Категория приложения` (`category`) | {category_cell} | `store/listing.json.category` |")
    add(
        f"| `Контактный адрес электронной почты` (`contactEmail`) | `{contact_email}` | "
        "`store/listing.json.contactEmail` |"
    )
    add(
        f"| `URL политики конфиденциальности` (`privacyPolicyUrl`) | `{privacy_url}` | "
        "`store/listing.json.privacyPolicyUrl` |"
    )
    entries = listing.get("locales") if isinstance(listing.get("locales"), dict) else {}
    for locale in locales:
        entry = entries.get(locale) if isinstance(entries.get(locale), dict) else {}
        title = str(entry.get("title") or "")
        short = str(entry.get("shortDescription") or "")
        add(
            f"| `Название приложения *` (`title`) [{locale}] | `{title}` | "
            f"`store/listing.json.locales.{locale}.title`; measured against the cap in `store/listing.md` |"
        )
        add(
            f"| `Краткое описание *` (`shortDescription`) [{locale}] | {short} | "
            f"`store/listing.json.locales.{locale}.shortDescription` |"
        )
        add(
            f"| `Полное описание *` (`fullDescription`) [{locale}] | paste the `### Full description` "
            f"block of `store/listing.md` | `store/listing.json.locales.{locale}.fullDescription`; the cap "
            "is resolved in `scripts/store/model.py` |"
        )
    add("")
    add(
        "**The five image fields are the owner's `[human]` uploads** (Preconditions §1); the pack already "
        "carries every file:"
    )
    add("")
    add("| Console field | Pack file |")
    add("|---|---|")
    add(
        f"| `Значок приложения *` (`icon`) | `store/assets/{pack_model.shared_asset_name('icon')}` and "
        f"`store/assets/<locale>/{pack_model.shared_asset_name('icon')}` |"
    )
    add(
        f"| `Картинка для описания *` (`featureGraphic`) | "
        f"`store/assets/{pack_model.shared_asset_name('feature-graphic')}` and its per-locale copy |"
    )
    add(f"| `Скриншоты для смартфона *` (`phoneShots`) | {_runbook_paths('phone')} |")
    add(f"| `Скриншоты для семидюймовых планшетов *` (`tablet7Shots`) | {_runbook_paths('tablet-7')} |")
    add(f"| `Скриншоты для 10-дюймового планшета *` (`tablet10Shots`) | {_runbook_paths('tablet-10')} |")
    add("")
    add(
        "- [ ] `[verify]` The optional fields stay empty on purpose: `URL проморолика на YouTube`, "
        "`Скриншоты на компьютере`, `Скриншоты для Android XR` and both XR video fields "
        "(`docs/play-console-checklist.md` §8)."
    )
    add("")
    add("---")

    # ------------------------------------------------------------------ monetization
    has_billing = "billing" in enabled
    add("")
    add("## 4. Monetization" + (" — the in-app product" if has_billing else ""))
    add("")
    if has_billing:
        ids_text = (
            ", ".join(f"`{product_id}`" for product_id in product_ids)
            if product_ids
            else "**none declared — the purchase flow would reference an unknown product**"
        )
        add(
            "The app's own billing code expects these product IDs — read from the shipped Kotlin by "
            f"`declared_product_ids()`, not typed here: {ids_text}."
        )
        add("")
        add("- [ ] `[console]` `Монетизация с помощью Play → Встроенные продукты` → create the product:")
        add("")
        add("  | Step | Field | Value |")
        add("  |---|---|---|")
        add(f"  | 1 | `Идентификатор товара` (`productId`) | {ids_text} |")
        add(f"  | 1 | `Имя` (`name`) | `{app_name}` |")
        add("  | 1 | `Тип покупки` | leave the default `Купить` |")
        add("  | 1 | `Теги` / `Значок` / `Категория налогообложения` | optional — leave or fill freely |")
        add(
            "  | 2 | `Идентификатор способа покупки` (`offerId`) | exactly one option "
            "(digits, lowercase letters and hyphens) |"
        )
        price = economics.get("priceUsd")
        if isinstance(price, (int, float)):
            price_cell = (
                f"`{str(price).replace('.', ',')}` — enter it **with a comma** in the RU console; "
                f"`economics.json.priceUsd` holds `{price}` with a dot"
            )
        else:
            price_cell = "**NEEDS-DECISION** — no numeric `priceUsd` in `economics.json`"
            decisions.append(
                "`Цена` — `economics.json` carries no numeric `priceUsd`, so the console's price field has "
                "no value to show."
            )
        add(f"  | 2 | Price | {price_cell} |")
        add("  | 2 | `Доступность` | leave all countries and regions |")
        add("")
        add(
            "- [ ] `[console]` **Exactly one purchase option.** A second option breaks the purchase: the "
            "app's billing controller builds the flow without a `setOfferToken`, which Play Billing accepts "
            "only when the product has a single option."
        )
        add(
            "- [ ] `[console]` Press **`Активировать`**, not `Сохранить как проект`. A draft option *is "
            "not returned by the Play Billing Library*, so the app behaves as if the product does not "
            "exist and the reviewer's price sheet fails — the failure mode `store/review-notes.md` names."
        )
        add(
            "- [ ] `[console]` Add the purchase-testing account at `Настройки → Тестирование лицензий`. An "
            "account that is only an internal track tester is charged **real money**."
        )
        add(
            "- [ ] `[console]` `Монетизация с помощью Play → Promotions` → create the promo code **after** "
            "the product exists (the page shows no create button while the product list is empty), then "
            "paste the code into the App-access declaration (§2 `Учетные данные`). The promo count and type "
            "are final once saved; the `promotion_codes.csv` file is never committed."
        )
    else:
        add(
            f"This slot enables no selling capability ({enabled_text} in `capabilities.json`), so there is "
            "no in-app product and no monetization console step. If the money plan "
            "(`economics.json.archetype`) names one, that is a disagreement to resolve before release."
        )
    add("")
    add("---")

    # ------------------------------------------------------------------ ladder
    add("")
    add("## 5. The submission ladder — press these, in this order")
    add("")
    add("The release form first:")
    add("")
    resolved_release_name = release_name or (version if version != "not declared" else "")
    if resolved_release_name:
        add(
            f"- `[console]` `Имя выпуска` (`releaseName`) — **`{resolved_release_name}`** "
            "(`store/release-notes.md`; Play prefills it from the bundle, so it must not be retyped)."
        )
    else:
        decisions.append(
            "`Имя выпуска` — `store/release-notes.md` states no release name and Gradle declares no "
            "version, so the console field has no value to show."
        )
        add("- `[console]` `Имя выпуска` (`releaseName`) — **NEEDS-DECISION**. See §7.")
    if notes:
        add(
            "- `[console]` `Заметки о выпуске` (`releaseNotes`) — paste the **whole** fenced `<locale>` "
            "block of `store/release-notes.md` into the single box, tags on their own lines. After pasting "
            "the console answers `Примечания к выпуску предоставлены на N языке`; `на 0 языке` means the "
            "tags are inline with the text."
        )
        add(
            f"  - Each language is capped at {pack_model.release_note_limit()} characters and Play "
            "truncates silently, so the block is measured at pack validation."
        )
    else:
        decisions.append(
            "`Заметки о выпуске` — `store/release-notes.md` carries no `<locale>` paste block, so there is "
            "nothing to paste. Re-run the pack renderer or write the block."
        )
        add("- `[console]` `Заметки о выпуске` (`releaseNotes`) — **NEEDS-DECISION**. See §7.")
    add(
        "- `[console]` `Страны/регионы выпуска` (`releaseCountries`) — open `Все страны и регионы` → "
        "`Изменить список стран/регионов` → tick `Выбрать все строки` → `Сохранить`. It is the one "
        "required field this step raises an error for when skipped."
    )
    add(
        "- `[console]` `Наборы App Bundle` → `Добавить из библиотеки` → tick the version → `Добавить в "
        "выпуск`. Do not re-upload the file."
    )
    add("")
    add("Then the ladder itself, in order:")
    add("")
    add(
        "1. `[console]` **Save.** On `.../tracks/<trackId>/releases/<releaseId>/review`, heading `Ошибки, "
        "предупреждения и сообщения`, press `Сохранить`. A banner `Обнаружены проблемы с выпуском` over a "
        "*warning* does not block — only an `Ошибка` does."
    )
    add(
        "2. `[console]` **The save modal.** `Перейти на страницу \"Обзор публикации\"?` — the save has "
        "**already happened**. Press `Перейти к обзору`. There is no second save."
    )
    add(
        "3. `[console]` **The publishing overview.** `.../publishing` → press the one enabled control "
        "`Отправить N изменений на проверку`. If it is disabled and the page prints the `...выполните "
        "необходимые действия на панели управления...` line, a §1 precondition is still open."
    )
    add(
        "4. `[console]` **The confirm dialog.** `Отправить N изменений на проверку?` → press `Отправка "
        "изменений на проверку`. There are **no checkboxes and no certifications** in this dialog; the two "
        "first-publication declarations live in §1 and are not re-asked."
    )
    add(
        "5. `[verify]` **The pre-review check.** `Проверка на наличие распространенных проблем` promises "
        "`Если на этом этапе не будет найдено проблем, изменения будут отправлены сразу.` — when it finds "
        "nothing the changes go to review immediately, with no second confirm control."
    )
    add(
        "6. `[verify]` **The terminal state.** `.../publishing` reads `Изменения на проверке`, with the one "
        "remaining control `Удалить изменения`. That is the end of the ladder."
    )
    add("")
    add("---")

    # ------------------------------------------------------------------ human
    add("")
    add("## 6. What stays human — the file inputs, and where the owner's words are")
    add("")
    add(
        "Everything else above is a console action an agent may perform when the owner has directed it. "
        "Two controls are file inputs, which the sandboxed browser cannot set, so the owner performs them "
        "(`docs/play-console-checklist.md` §10):"
    )
    add("")
    add(
        "1. `[human]` **Uploading the bundle file.** `Выпуски` → `Загрузить` takes the AAB itself and is "
        "the one act that puts the version into the app's library for §5's `Добавить из библиотеки`. The "
        "release form itself never re-uploads the file."
    )
    add(
        "2. `[human]` **Uploading the five store-graphic fields** (`Магазин → Основная информация о "
        "магазине`): the icon, the feature graphic and the phone / 7-inch / 10-inch screenshot sets. The "
        "field is a hidden `input[type=file]` that appears after `Добавить объекты`, and "
        "`DOM.setFileInputFiles` is denied — an unsolved tooling limitation, recorded as a limitation and "
        "not as a policy."
    )
    add("")
    add(
        "**The two first-publication declarations are not on that list.** They are the owner's *attestation* "
        "and the agent's click: `docs/play-console-checklist.md` §1.A records his decision of 2026-10-03 "
        "that the boxes may be ticked automatically once the slot carries the recorded approval, because "
        "«если я разрешил публикацию, значит эти галочки можно уже автоматически нажимать». The "
        "attestation is discharged by the approval, not by the click."
    )
    add("")
    add(
        "That approval precedes this runbook: the agent has already installed and exercised the app on the "
        "phone, and the owner's *product* decision is recorded with "
        f"`node scripts/approve-release.mjs --slot {slug} --note \"<the owner's own words>\"`. The annual "
        "BIS self-classification question stays an open obligation."
    )
    add("")

    # ------------------------------------------------------------------ decisions
    add("---")
    add("")
    add("## 7. Decisions this runbook could not derive")
    add("")
    if decisions:
        add(
            "Questions for a human, not guesses: each names the declaration that would settle it. Until "
            "they are answered, the console field named in each is **NEEDS-DECISION**."
        )
        add("")
        for item in decisions:
            add(f"- {item}")
    else:
        add("None — every value in this runbook was derived from the slot's own declarations.")
    add("")
    add("---")
    add("")
    add(
        "**The global reference** for every field, option and cap is `docs/play-console-checklist.md` "
        "(longer narrative: `docs/play-publishing-playbook.md`). This file is the app-specific click "
        "sequence; the two are meant to be read together, and this one is the shorter path."
    )
    add("")
    add(CONSOLE_RUNBOOK_MARKER)
    add("")
    return "\n".join(out)


def write_console_runbook(
    store: Path, listing: dict[str, Any], locales: list[str], meta: dict[str, str]
) -> None:
    """Render this slot's console runbook, preserving a hand-written one as every other document is."""
    content = console_runbook(store, listing, locales, meta)
    write_preserving(store / "console-runbook.md", content, CONSOLE_RUNBOOK_MARKER)


def console_runbook_main(argv: list[str] | None = None) -> int:
    """Write one slot's `store/console-runbook.md`, and only that document.

    `scripts/store/render.py` writes it as part of a full pack render, which is a release action gated
    on the owner's approval. This entry point exists so the runbook can be regenerated during
    development: it is derived entirely from the slot's own declarations, writes one document, and
    touches no asset, so it belongs beside `render.py --init` rather than behind the approval gate.
    """
    parser = argparse.ArgumentParser(
        description="Write a slot's store/console-runbook.md from its own declarations."
    )
    target = parser.add_mutually_exclusive_group(required=True)
    target.add_argument("--slot", help="Slot slug resolved beneath apps/.")
    target.add_argument("--slot-dir", type=Path, help="Explicit slot directory.")
    args = parser.parse_args(argv)

    if args.slot_dir:
        slot = args.slot_dir.expanduser()
        slot = (Path.cwd() / slot).resolve() if not slot.is_absolute() else slot.resolve()
    else:
        slot = pack_model.find_slot(args.slot).resolve()
    if not slot.is_dir():
        raise RenderError(f"slot directory does not exist: {slot}")

    # Imported lazily because `render_listing` is a sibling that `render.py` normally supplies;
    # importing it at module load would add a second import graph for one entry point.
    from render_listing import load_listing, slot_metadata

    listing = load_listing(slot / "store" / "listing.json")
    meta = slot_metadata(slot)
    locales = list(listing.get("locales", {}).keys()) or ["en-US"]
    write_console_runbook(slot / "store", listing, locales, meta)
    print(f"RENDER: OK apps/{slot.name}/store/console-runbook.md")
    return 0


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
    # Hosting is the factory's job, not a question for the owner: `node scripts/publish-privacy.mjs
    # --slot <slug>` publishes `store/privacy.md` to a public HTTPS URL and records it in
    # `listing.json`. What no program can do is confirm the live page still describes the shipped
    # app after the app changed -- a new SDK, permission or data type can make a previously true
    # policy false -- so that reading is the whole residual human act, and each branch names it.
    # The check is the value being real, never a claim that a command was typed: the branch that
    # knows the URL is real says it is **recorded** (what this read path measured), never that the
    # publisher ran, because `is_placeholder_url` reads the host and cannot see a process.
    privacy_todo = (
        f"- TODO-HUMAN: The policy is not published yet - `{privacy_url}` is a reserved-host "
        f"placeholder. Run `node scripts/publish-privacy.mjs --slot {listing['slug']}` to publish "
        "`store/privacy.md` and record its public HTTPS URL in `listing.json`, then open the live "
        "page and confirm it describes the shipped app."
        if is_placeholder_url(privacy_url)
        else f"- TODO-HUMAN: `{privacy_url}` is recorded as the policy URL and is a real host, not a "
        f"reserved placeholder. Re-run `node scripts/publish-privacy.mjs --slot {listing['slug']}` "
        "after editing `store/privacy.md` (it updates the same URL), and open the live page to "
        "confirm it still describes the shipped app."
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

    # The checklist used to tell the reader to replace a placeholder contact email and privacy URL
    # and to host the policy themselves -- the very questions the owner is tired of being asked.
    # The factory now does the hosting: `publish-privacy.mjs` publishes `store/privacy.md` and
    # records the URL, and the contact email has a real default. Each row below is conditional on
    # the value being real, so it never asserts a defect the pack does not have; the row that knows
    # the URL is real says it is recorded rather than that the publisher ran, and the residual human
    # act it keeps is the one no program can do -- the live policy still describing the app.
    contact_check = (
        f"- [ ] Replace `{contact}` in `listing.json` with a monitored support address."
        if is_placeholder_email(contact)
        else f"- [ ] Confirm `{contact}` is monitored; Play publishes it on the listing."
    )
    privacy_check = (
        f"- [ ] Run `node scripts/publish-privacy.mjs --slot {listing['slug']}` to publish "
        f"`store/privacy.md` and record its public HTTPS URL in `listing.json` - `{privacy_url}` is "
        "still a reserved-host placeholder."
        if is_placeholder_url(privacy_url)
        else f"- [ ] `store/privacy.md` is recorded at `{privacy_url}` (a real host); re-run "
        f"`node scripts/publish-privacy.mjs --slot {listing['slug']}` after editing it, and open the "
        "live page to confirm it still describes the shipped app."
    )
    checklist_lines = [
        f"# Google Play upload checklist — {listing['appName']}",
        "",
        f"- [ ] Run `python scripts/store/validate.py --slot {listing['slug']}` and resolve every FAIL.",
        f"- [ ] {'Replace synthetic screenshots with real captures and regenerate the pack.' if synthetic else 'Inspect every captured screen for personal or test data.'}",
        "- [ ] Compare listing claims and captions with the release build.",
        contact_check,
        privacy_check,
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
    # The app's own console runbook: the click sequence with this slot's answers already in place.
    # It is written here, last, so a full pack render always carries a current one, and it is
    # regenerable alone with `python scripts/store/render_documents.py --slot <slug>`.
    write_console_runbook(store, listing, locales, meta)


if __name__ == "__main__":
    try:
        raise SystemExit(console_runbook_main())
    except RenderError as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        raise SystemExit(1)
