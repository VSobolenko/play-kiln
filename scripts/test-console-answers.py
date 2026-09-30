#!/usr/bin/env python3
"""Regression suite for the generated `store/console-answers.md` sheet.

The defect this exists against: the owner performed the first Play Console upload of a real slot by
hand, answering page-by-page questions in chat. Every question was a stall — what goes in the
privacy-policy URL field, yes/no for each of the ten App-content declarations, what to put in the
credentials dialog, why the «Create release» button asked for the bundle a second time, why the
opt-in link 404s. None of it was in the generated pack, so the next slot would stall in the same
places and a human would re-derive every answer. The pack carried `listing.json`, the copy, the
assets and the release notes, and nothing carried *the console form*.

Three halves, and the negative ones are the point:

- **Generation.** `write_documents` must print a sheet that names every screen in the model's console
  order with a per-field table (`поле | лимит | что ввести | почему`), fills each literal from the
  slot's own pack, measures the English review instructions against the credentials dialog's
  500-character cap, states the release name and the release-notes paste block, and — for a billing
  slot only — names the exact product IDs the slot's own Kotlin declares.
- **Validation.** A sheet that has drifted from the pack must fail. The claims block is compared
  field by field, so a sheet left behind by an older pack cannot keep a superseded answer, and text
  carrying a reserved documentation host is rejected outright.
- **The model itself.** The screens, their fields, their caps and the wording the console shows live
  in one file (`scripts/lib/console-form.json`) that both this generator and
  `scripts/console-walkthrough.mjs` read. Legs below prove `store-kit.py` actually reads it, rather
  than that the file merely exists, and the 2026-09-29 observations are recorded as that file's data
  rather than as prose in a renderer.

Usage: python scripts/test-console-answers.py
"""

import contextlib
import copy
import importlib.util
import io
import json
import re
import sys
import tempfile
from pathlib import Path

# A Windows console defaults to a legacy codepage — cp1251 on the machine that found this — which
# cannot represent every character the console model prints. On 2026-09-29 the `→` inside a Russian
# page path raised UnicodeEncodeError while this suite was printing a label, so a content-PASS left
# the process with a non-zero exit and `scripts/selftest.mjs` went red for a reason that had nothing
# to do with the pack it grades. A verdict must not depend on the terminal's glyph set: an
# unencodable character degrades to '?' instead of killing the run.
for _stream in (sys.stdout, sys.stderr):
    with contextlib.suppress(AttributeError, ValueError):
        _stream.reconfigure(errors="replace")

# Keep the import of `store-kit.py` from writing a `scripts/__pycache__` entry.
sys.dont_write_bytecode = True

ROOT = Path(__file__).resolve().parents[1]

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
REAL_PRIVACY_URL = "https://telegra.ph/test-app-privacy-policy-09-29"
SOURCE_RELATIVE = Path("app/src/main/java/com/gexetr/testapp/monetization/MonetizationConfig.kt")
LOCALES = ["en-US", "ru-RU"]


def literal(product_id: str, quote: str = '"') -> str:
    return f"val PRODUCT = ProductId({quote}{product_id}{quote})"


def make_slot(
    declarations: list[str],
    *,
    enabled: list[str],
    privacy_url: str | None = REAL_PRIVACY_URL,
    locales: list[str] | None = None,
    price_usd: float | None = 6.99,
    tags: list[str] | None = None,
    economics: bool = True,
    data_collected: list[dict] | None = None,
    sdks: list[dict] | None = None,
    audience: str = "adults-only",
):
    """A temp slot with a generated store pack. Returns ``(slot, store, listing)``.

    The compliance dict mirrors what `scripts/lib/capabilities.mjs` emits, so `default_listing` and
    `apply_capability_compliance` produce the same fields a real slot gets without shelling out to
    node — the pack under test is the pack the generator would write. `economics.json` is written the
    way a real slot keeps it, price and all, because the console price is rendered from that file and
    a hand-written price in a test would prove nothing about the generator.

    `data_collected` and `sdks` default to empty, which is the slot that declares nothing; a leg that
    needs the collecting branch hands in the rows the capability registry itself declares
    (`capabilities/<id>/capability.json`), so the pack's data-safety answer is derived from the same
    declaration a real slot ships rather than from a second copy written here.

    `audience` is a parameter rather than an inherited default because `kit.default_listing` seeds
    none: the factory cannot know a slot's audience, so the slot declares it, and this fixture
    declares the one its leg is about. A leg that passes `audience=""` is the slot that declared
    nothing, and it must read as work to do rather than as the adult branch.
    """
    locales = locales or list(LOCALES)
    slot = Path(tempfile.mkdtemp(prefix="console-answers-"))
    if economics:
        plan = {"schemaVersion": 1, "archetype": "purchase_led_single_unlock"}
        if price_usd is not None:
            plan["priceUsd"] = price_usd
        (slot / "economics.json").write_text(json.dumps(plan, indent=2) + "\n", encoding="utf-8")
    source = slot / SOURCE_RELATIVE
    source.parent.mkdir(parents=True)
    source.write_text(
        "package com.gexetr.testapp.monetization\n\n"
        "import com.gexetr.capability.billing.ProductId\n\n"
        "object MonetizationConfig {\n"
        + "".join(f"    {line}\n" for line in declarations)
        + "}\n",
        encoding="utf-8",
    )
    store = slot / "store"
    store.mkdir(parents=True)
    compliance = {
        "enabled": list(enabled),
        "adsPresent": "ads" in enabled,
        "dataCollected": copy.deepcopy(list(data_collected or [])),
        "sdkDependencies": copy.deepcopy(list(sdks or [])),
        "permissions": [],
        "requiresConsent": False,
        "requiresPurchaseAuthority": False,
        "purchaseAuthority": None,
    }
    listing = kit.default_listing(META, locales, compliance)
    kit.apply_capability_compliance(listing, compliance)
    # The audience is set after `default_listing`, never by it: the factory seeds none.
    listing["contentRating"]["targetAudience"] = audience
    if privacy_url is not None:
        listing["privacyPolicyUrl"] = privacy_url
    if tags is not None:
        listing["tags"] = list(tags)
    kit.write_documents(store, listing, locales, False, META)
    return slot, store, listing


def sheet(store: Path) -> str:
    """The generated sheet, with its absence recorded as a failing row.

    Generation is the contract under test, so a missing file has to be a verdict rather than a
    traceback. It is also why every comparison against the text asserts non-emptiness first: a
    reader comparing an absent sheet to a string would otherwise be comparing two empties, and two
    empties are equal.
    """
    path = store / "console-answers.md"
    if not path.is_file():
        check(False, f"write_documents must generate {path.name}; there is nothing to read")
        return ""
    return path.read_text(encoding="utf-8")


def validated(store: Path, listing: dict) -> tuple[bool, str]:
    """`validate_store`'s verdict and its captured stdout - the rows a human actually sees."""
    buffer = io.StringIO()
    with contextlib.redirect_stdout(buffer):
        ok = kit.validate_store(store, listing, list(LOCALES), skip_provenance=True)
    return ok, buffer.getvalue()


def screen_by_id(screen_id: str) -> dict:
    """One model screen, top-level or not, so a leg can name a sub-screen without walking it."""
    stack = list(kit.console_screens())
    while stack:
        screen = stack.pop(0)
        if screen.get("id") == screen_id:
            return screen
        stack.extend(screen.get("subscreens") or [])
    raise AssertionError(f"scripts/lib/console-form.json has no screen {screen_id!r}")


def field_by_id(screen_id: str, field_id: str) -> dict:
    for field in kit.console_screen_fields(screen_by_id(screen_id)):
        if field.get("id") == field_id:
            return field
    raise AssertionError(f"screen {screen_id!r} has no field {field_id!r}")


def field_by_probe_id(probe: dict, screen_id: str, field_id: str) -> dict:
    """One field of a *copy* of the model, so a leg can break a row without editing the file.

    `field_by_id` reads whatever `_CONSOLE_FORM` happens to be, which is the model under test rather
    than the probe a leg is about to damage.
    """
    stack = list(probe.get("screens") or [])
    while stack:
        screen = stack.pop(0)
        if str(screen.get("id")) == screen_id:
            for field in kit.console_screen_fields(screen):
                if field.get("id") == field_id:
                    return field
            raise AssertionError(f"probe screen {screen_id!r} has no field {field_id!r}")
        stack.extend(screen.get("subscreens") or [])
    raise AssertionError(f"probe has no screen {screen_id!r}")


def field_label(field: dict) -> str:
    """How the sheet labels a field: once, never the same word twice.

    The label is the console's own row, resolved the same way the generator resolves it - a field may
    name the model path that carries its label instead of storing a second copy - so a leg asserting a
    label asserts what the sheet actually prints rather than what the model happens to store.
    """
    name_ru = kit.console_field_name(field)
    name_en = kit.console_field_name(field, lang="en")
    if name_en and name_en != name_ru:
        return f"{name_ru} ({name_en})"
    return str(name_ru)


def paste_block(text: str, label: str) -> str | None:
    """The block the sheet prints under one field's label, or None when it prints none."""
    match = re.search(
        rf"\*\*{re.escape(label)} — paste:\*\*\n\n```text\n(.*?)\n```",
        text,
        re.DOTALL,
    )
    return match.group(1) if match else None


# --- The model is the single source of truth -----------------------------------------------------


model = kit.console_form()
declarations = kit.console_declarations()
check(len(declarations) == 10, f"the model carries the ten App-content declarations (got {len(declarations)})")
check(
    [entry["declarationOrder"] for entry in declarations] == list(range(1, 11)),
    "every declaration carries a distinct console order, 1..10",
)
check(
    len({entry["id"] for entry in declarations}) == 10,
    "declaration ids are unique, so a leg can name one",
)
check(
    all(entry.get("slotKey") for entry in declarations),
    "every declaration names the slot-data key that fills its literal",
)

# --- A row the pack cannot answer is work, not a value -------------------------------------------
# The promotions screen carried `нужное число кодов (владелец выпускал 100)` in the third column - the
# column a human pastes from. One session's batch size is not this app's batch size, and the count
# cannot be changed once the promotion is saved, so the model marks such a row `ownerDecision` and
# both guides print work naming the field. The rule below is what makes the *next* row of this kind
# fail instead of shipping: every row of every screen has to say where its answer comes from, and
# "the words look like an answer" stops being evidence - it is the defect.
#
# These legs run before the first fixture is generated, deliberately. A model that fails this rule
# makes `write_documents` refuse, so a suite whose first fixture comes first would stop with the
# generator's message and never print the leg that names the broken row: a check whose failure hides
# its own report is half a check.
OWNER_ROWS = [
    ("billing-promotions", "codeCount"),
    ("billing-promotions", "startDate"),
    ("billing-license", "licenseTester"),
]


def source_errors_with(probe: dict) -> list[str]:
    """The reader errors of a *copy* of the model, so a leg can break a row without editing the file."""
    saved = kit._CONSOLE_FORM
    try:
        kit._CONSOLE_FORM = probe
        return kit.console_model_reader_errors()
    finally:
        kit._CONSOLE_FORM = saved


def sheet_cell(row: str, index: int) -> str:
    """One cell of a generated markdown table row, so a leg asserts the cell and not the whole line."""
    parts = row.split("|")
    return parts[index].strip() if len(parts) > index else ""


model_text = (ROOT / "scripts" / "lib" / "console-form.json").read_text(encoding="utf-8")
check(
    "владелец выпускал 100" not in model_text and "the owner issued 100" not in model_text,
    "no row of the model states the observed session's batch size as an answer",
)
for owner_screen, owner_field in OWNER_ROWS:
    check(
        bool(field_by_id(owner_screen, owner_field).get("ownerDecision")),
        f"{owner_screen} → {owner_field} is the owner's own decision rather than a value the pack sets",
    )
check(
    source_errors_with(copy.deepcopy(model)) == [],
    "every row of the model names where its answer comes from: a reader, a console default, the owner, "
    "a screen state, a listing field, or a product branch",
)
naked_probe = copy.deepcopy(model)
naked_screen = next(entry for entry in naked_probe["screens"] if entry["id"] == "billing-promotions")
naked_screen["fields"].append(
    {
        "id": "nakedRow",
        "nameRu": "Новая строка",
        "nameEn": "New row",
        "kind": "text",
        "valueRu": "владелец выпускал 100",
        "valueEn": "the owner issued 100",
    }
)
naked_errors = source_errors_with(naked_probe)
check(
    len(naked_errors) == 1 and "nakedRow" in naked_errors[0] and "no answer source" in naked_errors[0],
    f"a row whose words came from a session and says nothing else is a failure naming it (got {naked_errors})",
)
reasonless_probe = copy.deepcopy(model)
reasonless = field_by_probe_id(reasonless_probe, "billing-promotions", "codeCount")
reasonless.pop("ownerDecisionEn")
reasonless_errors = source_errors_with(reasonless_probe)
check(
    len(reasonless_errors) == 1
    and "codeCount" in reasonless_errors[0]
    and "ownerDecisionEn" in reasonless_errors[0],
    f"the owner's-decision marker with no statement beside it is a failure (got {reasonless_errors})",
)
both_probe = copy.deepcopy(model)
both = field_by_probe_id(both_probe, "billing-promotions", "codeCount")
both["literal"] = "derived.promoEndDate"
both_errors = source_errors_with(both_probe)
check(
    len(both_errors) == 1 and "both" in both_errors[0] and "codeCount" in both_errors[0],
    f"a row cannot be read from the pack and decided by the owner at once (got {both_errors})",
)
# The validator and the cell reader are two lines of defence, and the second one is the one a slot
# meets at generation time: a marker whose sentence is missing has to refuse there too. The `ctx` is
# minimal on purpose - the refusal happens before any of it is read.
saved_source_form = kit._CONSOLE_FORM
try:
    kit._CONSOLE_FORM = reasonless_probe
    reader_refused = False
    try:
        kit.console_field_cell(
            field_by_probe_id(reasonless_probe, "billing-promotions", "codeCount"),
            screen_by_id("billing-promotions"),
            {"enabled": [], "listing": {}, "compliance": {}, "credentials": {}, "creds": {}, "literals": {}},
        )
    except kit.StoreKitError:
        reader_refused = True
    check(
        reader_refused,
        "the cell reader refuses such a row too, instead of printing the value it has no reason for",
    )
finally:
    kit._CONSOLE_FORM = saved_source_form

# `validate_listing` must read the caps from the model instead of restating them. A copy is the
# failure mode: the sheet prints the cap it enforced, so a second number would print a limit the
# validator does not use.
limits = kit.listing_limits()
check(
    limits == {key: model["listing"][key] for key in ("title", "shortDescription", "fullDescription")},
    f"the listing character limits come from the model (got {limits})",
)
over_long = kit.default_listing(META, ["en-US"], {"enabled": [], "adsPresent": False, "dataCollected": []})
over_long["locales"]["en-US"]["title"] = "x" * (limits["title"] + 1)
check(
    any(str(limits["title"]) in error for error in kit.validate_listing(over_long, ["en-US"])),
    "an over-long listing field fails naming the model's limit, not a hardcoded one",
)

# Which radio the App access declaration takes is a rule in the model, not a branch in Python.
check(
    kit.credentials_rule({"enabled": ["billing"]})["answer"] == "yes",
    "a slot that sells something answers the App access declaration «yes»",
)
check(
    kit.credentials_rule({"enabled": []})["answer"] == "no",
    "a slot that sells nothing answers the App access declaration «no»",
)
check(
    kit.credentials_rule({"enabled": []})["when"] == "default",
    "the fallback rule is the model's own `default` entry",
)


# --- A billing slot's sheet is complete and ordered ----------------------------------------------


slot, store, listing = make_slot(
    [literal("recordbook_pro")],
    enabled=["billing"],
    tags=["offline", "teacher tools", "Productivity", "grades"],
)
text = sheet(store)
check((store / "console-answers.md").is_file(), "write_documents generates store/console-answers.md")

# Every screen the slot needs is a numbered heading of its own, in the order the console presents
# them. A screen without a heading is a screen the human walks blind.
screen_positions = [text.find(f"### {screen['order']}. {screen['consoleNameRu']}") for screen in kit.console_screens()]
check(
    all(position >= 0 for position in screen_positions),
    "every screen appears as a numbered heading under its Russian console name",
)
check(
    screen_positions == sorted(screen_positions) and len(set(screen_positions)) == len(screen_positions),
    "the screens appear in console order, once each, because that is the order they are shown in",
)
sub_positions = [
    text.find(f"#### {sub['titleRu']}")
    for screen in kit.console_screens()
    for sub in screen.get("subscreens") or []
]
check(
    all(position >= 0 for position in sub_positions),
    f"every sub-screen appears under its own title (missing: {[p for p in sub_positions if p < 0]})",
)

positions = [text.find(f"### {entry['order']}. {entry['consoleNameRu']}") for entry in declarations]
check(
    all(position >= 0 for position in positions) and positions == sorted(positions),
    "a sheet names every declaration in the console's own order",
)
check(
    declarations[0]["consoleNameRu"] == "Политика конфиденциальности"
    and declarations[-1]["consoleNameRu"] == "Приложения для здоровья"
    and f"### {declarations[0]['order']}. Политика конфиденциальности" in text
    and f"### {declarations[-1]['order']}. Приложения для здоровья" in text,
    "the sheet opens the declarations with the first and closes them with the tenth",
)

check(REAL_PRIVACY_URL in text, "the privacy-policy URL is the slot's own listing value")
# The listing labels are read off the model rather than typed here: the 2026-09-29 session read them
# in the console again, and a test holding its own copy of a console label is a test that will
# disagree with the model the next time the console is read. What is asserted is the pair itself -
# the console's Russian label with the model's English gloss - and that no mistyped variant survives.
store_screen = screen_by_id("store-listing")
contact_field = kit.console_field(store_screen, "contactEmail")
title_field = kit.console_field(store_screen, "title")
short_field = kit.console_field(store_screen, "shortDescription")
check(
    "Kонтактный" not in text
    and contact_field["nameRu"] in text
    and f"{contact_field['nameRu']} ({contact_field['nameEn']})" in text,
    "the listing fields are named with the console's own Russian labels",
)
check(
    f"| {contact_field['nameRu']} ({contact_field['nameEn']}) | — | {listing['contactEmail']} |" in text,
    "the store-listing contact row prints the pack's own address, not a description of it",
)
check(
    "`listing.privacyPolicyUrl`" in text,
    "each answer names the slot-data key it was filled from",
)
check(
    f"| {title_field['nameRu']} ({title_field['nameEn']}) — en-US | 30 | "
    f"{listing['locales']['en-US']['title']} |" in text,
    "a pack-localized listing field is one row per locale, with the console's own cap and the text itself",
)
check(
    f"| {short_field['nameRu']} ({short_field['nameEn']}) — ru-RU | 80 | "
    f"{listing['locales']['ru-RU']['shortDescription']} |" in text,
    "the ru-RU row prints the ru-RU text, not the default locale's",
)
check(
    f"{len(listing['locales']['en-US']['title'])}/30 characters" in text,
    "a localized row states the measured length against the console's cap",
)

# The product ID has to come out of the slot's shipped Kotlin, never out of a document: an invented
# ID is the false instruction on the money path that the billing checklist was written to prevent.
check(
    "```text\nrecordbook_pro\n```" in text,
    "a billing slot's sheet names the exact product ID its Kotlin declares",
)
check(
    model["billing"]["productPagePathEn"] in text and model["billing"]["licenseTestingPathEn"] in text,
    "a billing slot's sheet names where in the console the product and the license testers live",
)


# --- The one-time-product form: literals a human can paste, and the three traps --------------------
#
# The owner walked the wizard by hand and hit three stalls that no document carried: the price field
# rejected the dot-decimal `economics.json` holds, the option's Save-as-draft button left a state the
# app cannot buy, and a second purchase option would break the flow because the slot's controller
# never passes an offer token. Each is a leg here, and each leg is written to fail on the old output.

form = model["billing"]["productForm"]
billing_claims = kit.parse_console_claims(store / "console-answers.md")
check(bool(billing_claims), "the billing sheet carries a claims block to compare against")
check(
    form["pagePathObserved"] in text,
    f"the sheet names the product wizard's own path ({form['pagePathObserved']!r})",
)
for title in form["stepTitlesRu"]:
    check(title in text, f"the sheet names the wizard step {title!r} as the console shows it")

for step_id in ("step1", "step2"):
    for field in form[step_id]["fields"]:
        if not field.get("cap"):
            continue
        check(
            f"| {field_label(field)} | {field['cap']} |" in text,
            f"the sheet carries the observed cap for {field['nameRu']!r} ({field['cap']})",
        )

check(
    "| Имя (Name) | 55 | Test App |" in text,
    "the product name is the slot's own listing title, not a re-typed string",
)
check(
    "| Описание (Description) | 200 | A focused offline tool. |" in text,
    "the product description is the slot's own listing short description",
)
check(
    model["billing"]["icon"]["slotKey"] in text,
    "the sheet points the product icon at the slot's own store icon",
)

# The price: rendered from the slot's plan in the form the Russian console accepts. A dot here is
# the exact defect the sheet exists to prevent — the owner could not type the price at all.
price_label = field_label(field_by_id("billing-price", "price"))
price_block = paste_block(text, price_label)
check(
    price_block is not None,
    f"a billing slot's sheet carries the price under the console's own label ({price_label!r})",
)
if price_block is not None:
    check(price_block == "6,99", f"the price is rendered in the console's comma form (got {price_block!r})")
    check("." not in price_block, f"the pasted price contains no dot (got {price_block!r})")
    check(
        price_block == billing_claims.get("productPriceDisplay"),
        "the pasted price equals the price the claims block records",
    )
check(
    billing_claims.get("productPriceDisplay") == "6,99"
    and "." not in str(billing_claims.get("productPriceDisplay")),
    f"the claims price carries a comma and no dot (got {billing_claims.get('productPriceDisplay')!r})",
)
plan = json.loads((slot / "economics.json").read_text(encoding="utf-8"))
check(
    plan["priceUsd"] == 6.99,
    "economics.json keeps its dot-decimal price: the rendering changed, not the plan's data format",
)

# The option ID may not contain what the product ID may: the two fields' alphabets differ, and the
# difference is invisible until one of them rejects the paste.
check(
    "```text\nrecordbook-pro\n```" in text,
    "the purchase-option ID is derived from the product ID in the console's own alphabet",
)
offer_rule = next(field["whyEn"] for field in form["step2"]["fields"] if field["id"] == "offerId")
check(
    offer_rule in text,
    "the sheet states the option ID's rule, including that underscores and dots are not allowed there",
)
check(
    "underscores and dots are not allowed" in offer_rule.lower(),
    "the option-ID rule text itself forbids underscores and dots, rather than a second copy saying so",
)
check(
    "may contain digits, lowercase letters, underscores and dots" in form["step1"]["fields"][0]["whyEn"],
    "the product-ID rule text itself allows underscores and dots, so the asymmetry is stated on both sides",
)

# Exactly one option, and the reason is in the shipped controller rather than in advice.
single = form["singleOptionRule"]
check(single["ruleEn"] in text, "the sheet states that exactly one purchase option may exist")
check(
    "setOfferToken" in text and "PlayBillingController.kt" in single["evidence"],
    "the sheet gives the single-option rule's reason in code: the flow is built without an offer token",
)
check(
    single["evidence"].split(",")[0] in text,
    "the sheet names the controller the rule's reason comes from",
)

# Activate versus save-as-draft: the same trap the release screen has, on the money path.
check(
    "Активировать" in text and "Сохранить как проект" in text,
    "the sheet names both buttons of the option step, so the contrast can be read",
)
check(
    form["draftTrapEn"] in text and "only one makes the option live" in form["draftTrapEn"],
    "the sheet contrasts the activating button with the draft button and says what the draft costs",
)
check(
    form["step2"]["buttonsRu"][0] == "Активировать" and "Активировать" in text,
    "the activating button the model names first is the one the sheet tells the human to press",
)
check(
    model["billing"]["activationRuleEn"] in text,
    "the sheet states what an activated option means in Play's own words, not in a summary of them",
)

# `Set prices` stays untranslated in the Russian console, and the bulk-edit order is what worked.
check("`Set prices`" in text, "the sheet quotes the price dropdown by the label the console shows")
price_order = [text.find(step) for step in form["priceRule"]["orderEn"]]
check(
    all(position >= 0 for position in price_order) and price_order == sorted(price_order),
    "the sheet gives the price steps in the observed order: open, bulk-edit, select all, type",
)

# The certification checkbox can only be ticked once a promo code reaches the paid content, so the
# declaration comes last in the order and the sheet says so with the console's own wording.
check(
    model["credentials"]["certificationCheckboxRu"] in text,
    "the sheet quotes the certification checkbox in the console's own words",
)
check(
    model["credentials"]["reviewerCannotDoEn"] in text,
    "the sheet says the reviewer cannot create accounts or buy, so the code is the only mechanism",
)
order_positions = [text.find(step) for step in model["credentials"]["billingOrderRu"]]
check(
    all(position >= 0 for position in order_positions) and order_positions == sorted(order_positions),
    "the sheet orders the money path: create the product, activate it, create the code, then declare",
)
check(
    model["billing"]["promotions"]["infoBoxEn"] in text,
    "the sheet repeats the promotions page's own limit, which is why the product comes first",
)

# Tags are used, and the literal is printed rather than described.
tag_label = field_label(field_by_id("billing-tags", "tags"))
tag_values = paste_block(text, tag_label)
check(tag_values is not None, f"a billing slot's sheet carries the tags under {tag_label!r}")
if tag_values is not None:
    printed_tags = tag_values.split("\n")
    check(bool(printed_tags) and all(printed_tags), f"the tag block lists literal tags (got {printed_tags})")
    check(
        all(re.match(r"^[a-z][a-z0-9-]*$", tag) and len(tag) <= 20 for tag in printed_tags),
        f"every printed tag satisfies the console's own tag rule (got {printed_tags})",
    )
    check(
        billing_claims.get("productTags") == printed_tags,
        "the claims block records the same tags the paste block prints",
    )

# The wizard asks for the same tags on both steps, and the sheet prints the block once: a second
# copy of a three-line block is exactly the repetition the owner asked to have removed.
tag_blocks = re.findall(rf"\*\*{re.escape(tag_label)} — paste:\*\*", text)
check(len(tag_blocks) == 1, f"the tag block is printed once, not per field (got {len(tag_blocks)})")
check(
    text.count(f"paste the same block as «{tag_label}» above") >= 1,
    "the later fields point at the block instead of repeating it",
)
check(
    "teacher tools" in text and "teacher-tools" in text,
    "a tag the console would reject is printed in its console form together with the change that made it",
)

# The plan is the only source of the number, so the number has to be re-readable from it.
for price, expected in ((12.5, "12,50"), (5, "5,00")):
    _slot_p, store_p, _listing_p = make_slot(
        [literal("recordbook_pro")], enabled=["billing"], price_usd=price
    )
    text_p = sheet(store_p)
    rendered_p = paste_block(text_p, price_label)
    check(
        rendered_p == expected,
        f"a plan priced {price} renders as {expected!r} (got {rendered_p!r})",
    )

# A plan with no price must not become an invented one.
_slot_q, store_q, _listing_q = make_slot(
    [literal("recordbook_pro")], enabled=["billing"], price_usd=None
)
text_q = sheet(store_q)
rendered_q = paste_block(text_q, price_label)
check(
    rendered_q is None and "TODO-HUMAN" in text_q,
    "a slot whose plan declares no price gets a TODO for a human, never a made-up price",
)
check(
    kit.parse_console_claims(store_q / "console-answers.md").get("productPriceDisplay") == "",
    "the claims block records the absent price as empty rather than as a number",
)

# A pack with no tags is a question for a human, not an empty field.
_slot_t, store_t, _listing_t = make_slot([literal("recordbook_pro")], enabled=["billing"], tags=[])
text_t = sheet(store_t)
check(
    paste_block(text_t, tag_label) is None and "TODO-HUMAN" in text_t,
    "a billing slot whose pack declares no tags is told to fill the field, not to skip it",
)


# --- The credentials dialog: English, measured, and honest about paid content ---------------------


instructions = re.search(
    r"requires them in \*\*English\*\*.*?```text\n(.*?)\n```",
    text,
    re.DOTALL,
)
check(instructions is not None, "the sheet carries a paste block for the review instructions")
if instructions:
    body = instructions.group(1)
    check(
        re.search(r"[\u0400-\u04FF]", body) is None,
        "the review instructions are English, which is what the dialog demands",
    )
    check(
        "promo code" in body,
        "a slot with paid content hands the reviewer a promo code, which the certification checkbox requires",
    )
    # Measured 2026-09-29: the template put «Issue the code in Play Console ... then paste it in
    # place of the note» between the reviewer's own sentences, so following the sheet literally
    # handed Google's reviewer a note about the owner's workflow. The block is read by the
    # reviewer; the owner's guidance belongs outside it.
    check(
        "Play Console" not in body and "paste it in place" not in body,
        "the paste block never addresses the owner, because the reviewer reads everything inside it",
    )
    check(
        model["credentials"]["promoCodePlaceholder"] in body,
        "the block prints the model's placeholder, so an un-replaced code is visible rather than silent",
    )
    check(
        model["credentials"]["instructionsOwnerNoteEn"] not in body,
        "the owner's guidance stays out of the block Google's reviewer reads",
    )
measured = re.search(r"caps the field at (\d+) characters; measured: \*\*(\d+)/\d+\*\*", text)
check(
    measured is not None and int(measured.group(1)) == model["credentials"]["instructionsCap"],
    "the instructions are measured against the model's own 500-character field cap",
)
check(
    measured is not None
    and int(measured.group(2)) <= int(measured.group(1))
    and f"**{measured.group(2)}/{measured.group(1)}**" in text,
    "the generated instructions fit the field they are pasted into",
)

# The dialog's fields that carry a cap, each named as the console names it and measured against its
# own limit. The two sign-in fields are listed on purpose and left empty: the console shows them on
# every dialog, so a sheet that omits them sends the human looking for a field it never mentioned.
# The dialog's other rows are controls rather than text (`full_access`, `google_devices_switch`) and a
# hint (`hint_other_data`); the console states no cap for those, so the loop reads the capped rows.
capped_dialog_fields = [
    field for field in model["credentials"]["dialogFields"] if field.get("cap") is not None
]
for field in capped_dialog_fields:
    check(
        f"| {field_label(field)} | {field['cap']} |" in text,
        f"the dialog field «{field['nameRu']}» is listed with its own cap ({field['cap']})",
    )
    check(
        field["nameRu"] in text,
        f"the dialog field «{field['nameRu']}» is named exactly as the console names it",
    )
check(
    model["credentials"]["dialogTitleRu"] in text,
    "the sheet names the credentials dialog by the title the console shows, so it can be found",
)
name_field = next(field for field in model["credentials"]["dialogFields"] if field["id"] == "name")
check(
    name_field["required"] is True and name_field["cap"] == 60,
    "only the Name field is mandatory, with the dialog's own 60-character cap",
)
for field_id in ("username", "password"):
    field = next(item for item in model["credentials"]["dialogFields"] if item["id"] == field_id)
    check(
        field["required"] is False and str(field["valueEn"]) in text,
        f"«{field['nameRu']}» is declared optional and left empty on purpose for {field_id!r}",
    )
check(
    "carries the console's own asterisk" in next(
        item["whyEn"] for item in model["credentials"]["dialogFields"] if item["id"] == "name"
    ),
    "the sheet says which field is mandatory by the console's own asterisk, not by guessing",
)
check(
    model["credentials"]["certificationObservationEn"] in text
    and "not a documented Google rule" in text,
    "the cleared-checkbox observation is carried into the sheet with its caveat intact",
)
check(
    "Не удалось сохранить изменения" in model["credentials"]["certificationObservationRu"],
    "the answer the console actually gave when the box was cleared is recorded verbatim in the model",
)
check(
    "the instructions carry a code" in model["credentials"]["certificationGatedEn"] and "promo code" in text,
    "the checkbox is explained as reachable only through a code, not as a formality to tick",
)


# --- Content rating step 1, as observed on 2026-09-29 ---------------------------------------------

rating = screen_by_id("declaration-content-rating")
check(
    rating["stripRu"] == ["1 Категория", "2 Анкета", "3 Сводка"],
    f"the rating walk records the three steps the console shows (got {rating['stripRu']})",
)
category = field_by_id("declaration-content-rating", "category")
check(
    category["optionsRu"] == ["Игра", "Социальные сети или связь", "Все другие типы приложений"],
    f"the category field carries exactly the three radios the console shows (got {category['optionsRu']})",
)
# The radio the sheet names is the *branch's*, read off the field that shows it: the three labels live
# once, on this field, and a branch names one by its `optionIds` id. A leg that asserted the field's
# own `valueRu` was the label was a leg for the copy this replaced - it passed only while the model
# said one app's answer twice.
observed_category, category_notes = kit.content_rating_category(model["contentRating"]["questionnaires"]["notes"])
check(
    category_notes == []
    and observed_category == category["optionsRu"][category["optionIds"].index("other")],
    f"the category the observed branch names is printed as the console's own label (got {observed_category!r})",
)
rating_ctx = {
    "listing": listing,
    "compliance": {"enabled": ["billing"]},
    "credentials": kit.credentials_rule({"enabled": ["billing"]}),
    "enabled": ["billing"],
    "literals": {},
    "store": store,
    "locales": list(LOCALES),
    "release_name": "1.2.3 (7)",
    "blocks": {},
}
category_cell = kit.console_field_cell(category, rating, rating_ctx)[0]
check(
    category_cell == observed_category,
    f"the category row answers with the console's label itself, not a token or a gloss (got {category_cell!r})",
)
check(
    all(
        option not in text
        for option in category["optionsRu"]
        if option != observed_category
    ),
    "the sheet names the radio to choose and not the ones it must not choose, so no radio is picked by accident",
)
check(
    "questionnaire" in str(category["valueRu"]) and "optionsRu" in str(category["valueRu"]),
    "the category row describes the rule that fills it - the pack's branch and this field's own labels",
)
email_field = field_by_id("declaration-content-rating", "email")
check(
    email_field["nameRu"] in text and "may be shared with rating bodies and IARC" in text,
    "the address field is named with the console's own label and its stated purpose, IARC included",
)
check(
    email_field["required"] is True and str(listing["contactEmail"]) in text,
    "the address the console demands is the pack's own contact address",
)
iarc = field_by_id("declaration-content-rating", "iarc")
check(
    iarc["nameRu"] in text and iarc["nameRu"].startswith("Я принимаю условия использования"),
    f"the IARC checkbox is quoted as the console words it (got {iarc['nameRu']!r})",
)
check(
    rating["actionRu"] == "Далее" and "`Далее`" in text,
    "the button that leaves step 1 is the button the console shows there",
)
step_two = screen_by_id("content-rating-step-2")
check(
    bool(step_two["fields"]) and all(field.get("valueRu") for field in step_two["fields"]),
    "step 2's questionnaire is modelled question by question, each with the answer the console takes",
)
check(
    all(field["nameRu"] in text for field in step_two["fields"]),
    "every questionnaire question is named in the sheet as the console names it",
)
check(
    all(
        field.get("optionsRu") == ["Да", "Нет"] or field.get("optionsEn") == ["Yes", "No"]
        for field in step_two["fields"]
    ),
    "the questionnaire's radios are the console's own two words, so no answer is invented",
)
step_three = screen_by_id("content-rating-step-3")
check(
    step_three.get("needsVerification") is True and step_three["titleRu"] in text,
    "the unobserved step 3 is still listed, so the walk has no silent hole",
)
# The marker has to be on the unobserved step itself: a NEEDS-VERIFICATION somewhere else in the
# walk leaves step 3 reading as an answer, which is the invention this convention exists to prevent.
step_three_parts = text.split(f"#### {step_three['titleRu']}", 1)
step_three_text = re.split(r"\n#{3,4} ", step_three_parts[1], 1)[0] if len(step_three_parts) == 2 else ""
check(
    "NEEDS-VERIFICATION" in step_three_text,
    "the sheet marks step 3 itself as unobserved, rather than answering it from memory",
)
check(
    bool(step_three.get("needsVerificationRu")) and step_three["needsVerificationRu"] in text,
    "the marker carries the model's own note about what was not read",
)
check(
    "Шаг 2 прочитан в консоли 2026-09-29" in str(rating["needsVerificationRu"]),
    "the model says which steps were read in the console and which were not, rather than one blanket note",
)


# --- Promotions: the limits, the file and the redemption path, as data ----------------------------

promotions_model = model["billing"]["promotions"]
check(
    "Период между датой начала и датой окончания не должен превышать 365 дней." in promotions_model["durationRu"],
    "the console's own refusal text is recorded verbatim in the model",
)
check(
    "Период между датой начала и датой окончания не должен превышать 365 дней." in text
    and "The period between the start and end date must not exceed 365 days." in text,
    "the sheet quotes the refusal the human will see, with the translation beside it",
)
check(
    promotions_model["durationEn"] in text and "at most one year" in promotions_model["durationEn"],
    "the one-year ceiling is stated before the human picks an end date, not after the refusal",
)
check(
    promotions_model["quarterlyLimitEn"] in text and "500 promo codes per quarter" in text,
    "the 500-per-quarter ceiling is stated where the count is decided",
)
check(
    promotions_model["immutableEn"] in text and "cannot be changed" in promotions_model["immutableEn"],
    "the sheet says the count and the type are final before the promotion is saved",
)
check(
    "promotion_codes.csv" in text and "promo-codes.csv" in text,
    "the file the codes come in is named, together with the repository's own name for it",
)
check(
    promotions_model["csvEn"] in text and "never committed" in promotions_model["csvEn"],
    "the sheet says the code file is never committed, because a code unlocks what the slot sells",
)
check(
    "https://play.google.com/redeem?code=<CODE>" in text,
    "the documented redemption link is printed, so the reviewer's instruction can be checked by hand",
)
check(
    "restore()" in text
    and "acknowledgePendingPurchases()" in text
    and "PlayBillingController.kt" in text,
    "the sheet names the code path that picks up an out-of-app redemption in the shipped controller",
)
check(
    promotions_model["draftAppObservationEn"] in text,
    "the closed question - a code does work for an unreleased app - is carried as an observation",
)


# --- The release screen and the machine-readable claims -------------------------------------------


release_name, _release_notes = kit.parse_release_notes(store / "release-notes.md")
check(release_name in text, f"the sheet states the release name read back off disk ({release_name!r})")
check(
    kit.release_notes_block(store / "release-notes.md") in text,
    "the sheet embeds the release-notes paste block character for character",
)

claims = kit.parse_console_claims(store / "console-answers.md")
check(bool(claims), "the sheet carries a machine-readable claims block")
check(
    claims.get("productIds") == ["recordbook_pro"],
    f"the claims block names the declared product IDs (got {claims.get('productIds')})",
)
check(
    claims.get("credentialsAnswer") == "yes",
    "the claims block records the App access answer that was actually generated",
)
check(
    claims.get("ads") is False and claims.get("collectsData") is False,
    "the claims block records the pack's ads and data-safety answers",
)
check(
    kit.validate_console_answers(store, listing, list(LOCALES), release_name) == [],
    "a freshly generated sheet corroborates itself against the pack it came from",
)


# --- A non-billing slot is told nothing about products -------------------------------------------


slot_nb, store_nb, listing_nb = make_slot([literal("harbour_unlock")], enabled=[])
text_nb = sheet(store_nb)
for marker in model["billing"]["markersEn"]:
    check(
        marker not in text_nb,
        f"a non-billing slot's sheet does not mention {marker!r}",
    )
# The product icon is the one billing instruction that names a file every pack ships, because the
# store page's own icon row is answered by the same asset. The path is therefore no longer a marker
# of a selling slot; what belongs to the billing page is its rule, and that is what is asserted here.
check(
    model["billing"]["icon"]["ruleEn"] not in text_nb,
    "a non-billing slot's sheet carries no product-icon instruction, although the store page names the same file",
)
check(
    "harbour_unlock" not in text_nb,
    "a non-billing slot's sheet names no product, even when its Kotlin declares one",
)
check(
    kit.credentials_rule({"enabled": []})["reasonEn"].split(",")[0] in text_nb,
    "a non-billing slot's App access answer is the model's own «default» reason",
)
check(
    "no sign-in, purchase, invitation, or code is required" in text_nb,
    "a non-billing slot's instructions say that nothing is gated",
)
check(
    model["billing"]["productForm"]["pagePathObserved"] not in text_nb,
    "a non-billing slot's sheet does not walk the product wizard at all",
)


# --- A missing privacy-policy URL is reported, never emitted as a placeholder ---------------------


slot_ph, store_ph, listing_ph = make_slot([], enabled=[], privacy_url=None)
text_ph = sheet(store_ph)
check(
    "example.com" not in text_ph and "localhost" not in text_ph,
    "a slot whose privacy URL is still a reserved host gets no placeholder in its sheet",
)
check(
    "TODO-HUMAN" in text_ph and "`listing.privacyPolicyUrl`" in text_ph,
    "the missing privacy URL is reported as work, naming the field to fix",
)
check(
    "| URL политики конфиденциальности (Privacy policy URL) | — | TODO-HUMAN: `listing.privacyPolicyUrl` is missing"
    in text_ph,
    "the missing privacy URL is reported on its own row, where the human types it",
)
check(
    kit.parse_console_claims(store_ph / "console-answers.md").get("privacyPolicyUrl") is None,
    "the claims block records the absent URL as null rather than as a value",
)
check(
    kit.validate_console_answers(store_ph, listing_ph, list(LOCALES), "1.2.3 (7)") == [],
    "an unfinished privacy URL is reported, not failed: absence makes no claim false",
)

# A pack whose contact address is still on a reserved host is the case the whole placeholder rule
# exists for, and it has to keep working in both directions at once: the sheet must report the field
# instead of offering it to paste, *and* the file must still validate. The first version recorded the
# raw address in the claims block, so the sheet failed the very check that forbids a placeholder -
# a gate that could only be satisfied by a pack that was already finished.
slot_email, store_email, listing_email = make_slot([], enabled=[], privacy_url=None)
listing_email["contactEmail"] = "human-authored@example.org"
kit.write_documents(store_email, listing_email, list(LOCALES), False, META)
text_email = sheet(store_email)
check(
    "example.org" not in text_email,
    "a pack whose contact address is a placeholder produces no reserved host in the sheet, claims block included",
)
check(
    "TODO-HUMAN" in text_email and "`listing.contactEmail`" in text_email,
    "the placeholder address is reported as work, naming the field to fix",
)
check(
    f"| {contact_field['nameRu']} ({contact_field['nameEn']}) | — | TODO-HUMAN: `listing.contactEmail` is a "
    "reserved documentation host; set it to a monitored inbox |" in text_email,
    "the placeholder address is reported on its own row, where the human types it",
)
check(
    kit.parse_console_claims(store_email / "console-answers.md").get("contactEmail") is None,
    "the claims block records the placeholder address as null rather than as a value",
)
check(
    kit.validate_console_answers(store_email, listing_email, list(LOCALES), "1.2.3 (7)") == [],
    "an unfinished contact address is reported by its own sheet rather than failing it",
)
check(
    "example.com" not in text and "localhost" not in text,
    "a finished slot's sheet carries no reserved documentation host",
)


# --- An ads-enabled slot answers the ads declaration from its capabilities ------------------------


slot_ads, store_ads, listing_ads = make_slot([], enabled=["ads"])
text_ads = sheet(store_ads)
check(
    "`listing.ads.containsAds` = true" in text_ads,
    "an ads-enabled slot's ads answer reads `true` out of listing.json",
)
check(
    kit.parse_console_claims(store_ads / "console-answers.md").get("ads") is True,
    "the claims block records the ads answer the pack actually declares",
)


# --- Regeneration: written, not accumulated, and never preserved ---------------------------------


before = sheet(store)
kit.write_documents(store, listing, list(LOCALES), False, META)
check(before != "" and sheet(store) == before, "regenerating an unchanged pack produces the same sheet")
(store / "console-answers.md").write_text("hand edited\n", encoding="utf-8")
kit.write_documents(store, listing, list(LOCALES), False, META)
check(
    before != "" and sheet(store) == before,
    "a hand-edited sheet is overwritten: it is a copy of the pack, so preserving it could only make it disagree",
)


# --- Validation: a drifted sheet must fail -------------------------------------------------------


drift_path = store / "console-answers.md"
drift_path.write_text(before.replace('"ads": false', '"ads": true'), encoding="utf-8")
_, output = validated(store, listing)
check(
    any(
        row.startswith("FAIL console-answers.md") and "ads" in row
        for row in output.splitlines()
    ),
    "a sheet whose ads answer contradicts listing.json is a FAIL row naming the field",
)
check(
    any(
        row.startswith("FAIL console-answers.md") and "True" in row and "False" in row
        for row in output.splitlines()
    ),
    "the drift message prints both the sheet's value and the pack's value",
)

drift_path.write_text(before.replace('"collectsData": false', '"collectsData": true'), encoding="utf-8")
_, output = validated(store, listing)
check(
    any(
        row.startswith("FAIL console-answers.md") and "collectsData" in row
        for row in output.splitlines()
    ),
    "a sheet whose data-safety answer contradicts listing.json is a FAIL row naming the field",
)

drift_path.write_text(before.replace('"recordbook_pro"', '"invented_product"'), encoding="utf-8")
_, output = validated(store, listing)
check(
    any(
        row.startswith("FAIL console-answers.md") and "productIds" in row
        for row in output.splitlines()
    ),
    "a sheet naming a product the slot's Kotlin does not declare is a FAIL row",
)

drift_path.write_text(before.replace('"credentialsAnswer": "yes"', '"credentialsAnswer": "no"'), encoding="utf-8")
_, output = validated(store, listing)
check(
    any(
        row.startswith("FAIL console-answers.md") and "credentialsAnswer" in row
        for row in output.splitlines()
    ),
    "a sheet whose App access answer contradicts the model's rule is a FAIL row",
)

drift_path.write_text("# Play Console answers\n\nno claims block here\n", encoding="utf-8")
_, output = validated(store, listing)
check(
    any(row.startswith("FAIL console-answers.md") and "claims block" in row for row in output.splitlines()),
    "a sheet that is not generator output fails rather than passing on an empty comparison",
)

drift_path.write_text(
    before.replace(
        REAL_PRIVACY_URL,
        REAL_PRIVACY_URL.replace("telegra.ph", "example.com"),
    ),
    encoding="utf-8",
)
_, output = validated(store, listing)
check(
    any(
        row.startswith("FAIL console-answers.md") and "reserved documentation host" in row
        for row in output.splitlines()
    ),
    "a sheet carrying a reserved documentation host fails, whatever field it came from",
)

# Restore a good sheet and confirm the same gate can pass: a check that can never pass is not a check.
drift_path.write_text(before, encoding="utf-8")
ok, output = validated(store, listing)
check(
    any(row.startswith("PASS console-answers.md") for row in output.splitlines()),
    "a corroborated sheet is reported as a PASS row",
)
check(
    not any(row.startswith("FAIL console-answers.md") for row in output.splitlines()),
    "no console-answers failure remains once the sheet matches the pack",
)


# --- The pack's documents are an inventory, and an absent one is a failure ------------------------
# `store/console-answers.md` was missing from a shipped slot's pack while `store-kit.py
# --validate-only` printed `STORE-KIT: OK`: the validator carried its own list of document names and
# the sheet was not on it, so the gate could not see the deliverable it was supposed to require. The
# inventory now lives in the model (`packDocuments`), which both gates read, and these legs drive the
# two ways a document can be absent - gone, and present but empty - plus the one case where absence
# is legitimate, which is a `warn` entry carrying the reason it makes no claim false.
#
# The empty case is a separate leg because the two are different defects: a file can be created by a
# half-finished run, and "the path exists" would then pass a check whose whole job is the content.

inventory = {entry["name"]: entry for entry in model["packDocuments"]["documents"]}
check(
    inventory.get("console-answers.md", {}).get("absence") == "fail",
    "the model's pack inventory requires store/console-answers.md of every pack",
)
written_documents = sorted(path.name for path in store.glob("*.md"))
check(
    sorted(inventory) == written_documents,
    "the inventory and `write_documents` name the same documents, so neither can grow alone "
    f"(inventory {sorted(inventory)} vs written {written_documents})",
)
check(
    all(entry.get("whyEn") and entry.get("whyRu") for entry in inventory.values()),
    "every document in the inventory states what it is for, so a failure can say what is missing",
)

sheet_file = store / "console-answers.md"
sheet_file.unlink()
ok, output = validated(store, listing)
check(
    ok is False and "FAIL console-answers.md — missing or empty" in output.splitlines(),
    "a pack whose generated sheet is gone is a FAIL row naming the document, not a silent OK",
)
sheet_file.write_text("", encoding="utf-8")
ok, output = validated(store, listing)
check(
    ok is False and "FAIL console-answers.md — missing or empty" in output.splitlines(),
    "a sheet that exists but is empty fails the same way, because an empty answer is no answer",
)
check(
    not any(row.startswith("PASS console-answers.md") for row in output.splitlines()),
    "an empty sheet is never reported as a PASS row, whatever else the pack contains",
)
sheet_file.write_text(before, encoding="utf-8")
ok, output = validated(store, listing)
check(
    any(row.startswith("PASS console-answers.md") for row in output.splitlines())
    and not any(row.startswith("FAIL console-answers.md") for row in output.splitlines()),
    "restoring the sheet reopens the same gate: the check reports the absence, not always a failure",
)

release_notes_file = store / "release-notes.md"
saved_release_notes = release_notes_file.read_text(encoding="utf-8")
release_notes_file.unlink()
ok, output = validated(store, listing)
warned = [row for row in output.splitlines() if row.startswith("WARN release-notes.md — absent; ")]
check(
    len(warned) == 1 and str(inventory["release-notes.md"]["absenceEn"]) in warned[0],
    f"a document the model lets be absent warns with the model's own reason (got {warned})",
)
check(
    not any(row.startswith("FAIL release-notes.md") for row in output.splitlines()),
    "the legitimate absence is not turned into a failure: the inventory has two verdicts, not one",
)
release_notes_file.write_text(saved_release_notes, encoding="utf-8")

# --- The target-audience guide is derived per slot, not copied from this app ----------------------
# The wizard's first step is a fixed set of six age boxes, and the sheet has to name the ones the
# console will show. `listing.contentRating.targetAudience` is therefore a token of the model's
# vocabulary, not free text: printed verbatim it is a string the human cannot find on the screen
# ("18 and over" is not a console label), and answered for a branch nobody has read it is an
# instruction invented from nothing. Both failures are legs below.


audience_table = kit.console_form()["targetAudience"]
adults = audience_table["audiences"]["adults-only"]
check(
    kit.target_audience_labels(adults) == ["От 18 лет"],
    "the adults-only branch ticks the console's own label, which is what the sheet prints",
)
check(
    kit.target_audience_skipped_steps(adults) == ["О приложении", "Реклама", "Страница приложения"],
    "the branch names the three wizard steps the console greys out, as the console names them",
)
check(
    audience_table["steps"] == [
        {"id": "step-1", "titleRu": "Возраст целевой группы", "titleEn": "Target age group"},
        {"id": "step-2", "titleRu": "О приложении", "titleEn": "About the app"},
        {"id": "step-3", "titleRu": "Реклама", "titleEn": "Ads"},
        {"id": "step-4", "titleRu": "Страница приложения", "titleEn": "Store listing"},
        {"id": "step-5", "titleRu": "Сводка", "titleEn": "Summary"},
    ],
    "the model carries the wizard's five steps, so a sub-screen can name the step the console shows",
)
check(
    "tick " + ", ".join(f"«{label}»" for label in kit.target_audience_labels(adults)) in text,
    "the age-group row tells the human which boxes to tick, in the console's own labels",
)
# The cell the human reads must be the console's label and nothing else: a token printed as an answer
# is a string that is not on the screen, which is what "18 and over" was in four committed packs.
age_label = field_label(field_by_id("declaration-target-audience", "ageGroups"))
age_rows = [line for line in text.splitlines() if line.startswith("|") and age_label in line]
check(
    len(age_rows) == 1
    and age_rows[0].split("|")[3].strip()
    == "tick " + ", ".join(f"«{label}»" for label in kit.target_audience_labels(adults)),
    "the age-group answer cell is the console's label for the branch, not the pack's token",
)

# An alias has to resolve to the same branch, or the vocabulary silently splits the factory in two.
# Three slots were committed spelling this audience `18+` and a fourth `18 and over`; declaring the
# equivalence beside the branch keeps those packs readable without rewriting their metadata.
for spelling in ("adults-only", "18+", "18 and over", "18+ only"):
    probe = {**listing, "contentRating": {**listing["contentRating"], "targetAudience": spelling}}
    token, probe_branch, notes = kit.target_audience_branch(probe)
    check(
        (token, notes) == ("adults-only", []) and kit.target_audience_labels(probe_branch) == ["От 18 лет"],
        f"the spelling {spelling!r} resolves to the adults-only branch, not to a second one",
    )

unobserved = {**listing, "contentRating": {**listing["contentRating"], "targetAudience": "13+"}}
token, _, notes = kit.target_audience_branch(unobserved)
check(
    token is None and bool(notes) and "TODO-HUMAN" in notes[0] and "adults-only" in notes[0],
    "a branch nobody has read is refused with the vocabulary named, rather than answered",
)
blank = {**listing, "contentRating": {**listing["contentRating"], "targetAudience": ""}}
token, _, notes = kit.target_audience_branch(blank)
check(
    token is None and bool(notes) and "TODO-HUMAN" in notes[0],
    "an empty audience is work to do, named as such, rather than silently the adult branch",
)
# The audience is the slot's decision, so the *default* listing cannot carry one: a pack seeded with
# a branch would ship another slot's audience, ticking age groups this app never declared and skipping
# wizard steps it never skipped. The factory therefore seeds none, and both documents print the model's
# own sentence for a slot that has not chosen - `targetAudience.emptyChoiceRefusalEn` in the sheet,
# `targetAudience.emptyChoiceRefusalRu` in the walkthrough. The two are one sentence, so they cannot
# disagree; the placeholders are filled from the field's own home (the screen's `slotKey`) and the
# vocabulary's own keys, never restated in a renderer.
check(
    kit.default_listing(META, list(LOCALES), {"enabled": []})["contentRating"]["targetAudience"] == "",
    "a new slot inherits no audience at all: the factory seeds the field empty, never a branch",
)
refusal_template = str(audience_table["emptyChoiceRefusalEn"])
check(
    "{field}" in refusal_template and "{branches}" in refusal_template,
    "the no-audience sentence names the field and the branches as placeholders, so both documents fill in the same two things",
)
known_branches = ", ".join(sorted(audience_table["audiences"]))
check(
    kit.target_audience_choice_refusal(known_branches)
    == refusal_template.replace("{field}", "listing.contentRating.targetAudience").replace(
        "{branches}", known_branches
    ),
    "the sheet's sentence is the model's, with the field read off the screen and the branches off the dictionary",
)
_, store_blank, listing_blank = make_slot([], enabled=["billing"], audience="")
blank_sheet = sheet(store_blank)
blank_rows = [
    line for line in blank_sheet.splitlines() if line.startswith("|") and age_label in line
]
check(
    len(blank_rows) == 1
    and "TODO-HUMAN" in blank_rows[0]
    and "listing.contentRating.targetAudience" in blank_rows[0],
    "a slot that declared no audience is asked to choose one, on the row where the human is answering",
)
check(
    "От 18 лет" not in blank_rows[0] and "От 18 лет" in age_rows[0],
    "the same row carries the branch's boxes for a slot that chose one and none for a slot that did not",
)
_, rows_blank = validated(store_blank, listing_blank)
check(
    any(row.startswith("PASS console-answers.md") for row in rows_blank.splitlines())
    and not any(row.startswith("FAIL console-answers.md") for row in rows_blank.splitlines()),
    "a sheet that has not chosen an audience still passes its own check: the pack is incomplete, not wrong",
)
_, store_unobserved, listing_unobserved = make_slot([], enabled=["billing"])
listing_unobserved["contentRating"]["targetAudience"] = "13+"
kit.write_documents(store_unobserved, listing_unobserved, list(LOCALES), False, META)
text_unobserved = sheet(store_unobserved)
check(
    "TODO-HUMAN" in text_unobserved and "'13+'" in text_unobserved,
    "the refusal lands on the age-group row itself, where the human is answering",
)

# The minor-restriction box is a branch decision too, and its console label is a whole sentence: a
# slot whose audience includes children must not be told to tick it. A branch is injected here
# because the factory has no such slot yet - the point is that the sheet follows the branch.
saved_model = kit._CONSOLE_FORM
try:
    probe_model = copy.deepcopy(saved_model)
    probe_model["targetAudience"]["audiences"]["teens"] = {
        "checkboxes": ["13-15", "16-17"],
        "minorGate": "leave",
        "skippedSteps": [],
        "aliases": [],
    }
    kit._CONSOLE_FORM = probe_model
    _, store_teens, listing_teens = make_slot([], enabled=["billing"])
    listing_teens["contentRating"]["targetAudience"] = "teens"
    kit.write_documents(store_teens, listing_teens, list(LOCALES), False, META)
    text_teens = sheet(store_teens)
    teens_branch = probe_model["targetAudience"]["audiences"]["teens"]
    teens_labels = kit.target_audience_labels(teens_branch)
    check(
        len(teens_labels) == 2
        and all(f"«{label}»" in text_teens for label in teens_labels)
        and "tick " in text_teens,
        f"a branch that includes children ticks those boxes and prints their own labels ({teens_labels})",
    )
    check(
        "leave «Запретить доступ" in text_teens,
        "a branch that does not answer the minor-restriction box is told to leave it empty",
    )
    check(
        all(label not in text_teens for label in kit.target_audience_labels(adults)),
        "no other branch's age group leaks into a branch that does not include it",
    )
    # The publishing-overview counter names the age group in the branch's own words, so a branch whose
    # row nobody has read gets work naming the branch - not the observed branch's `18 и старше`, which
    # is a string this slot does not have on its screen. The rule sentence may *illustrate* the observed
    # branch; the counter row may not borrow its wording.
    publishing_counter = next(
        (
            counter
            for counter in (probe_model["declarationCompletion"]["counters"])
            if "{audienceStoreRow}" in str(counter["shows"])
        ),
        {},
    )
    publishing_row_teens = next(
        (
            line
            for line in text_teens.splitlines()
            if line.startswith("- **") and str(publishing_counter.get("whereEn") or "") in line
        ),
        "",
    )
    check(
        "{audienceStoreRow}" not in publishing_row_teens
        and "TODO-HUMAN: nobody has read what the publishing-overview row prints" in text_teens
        and "branch `teens`" in text_teens
        and "18 и старше" not in publishing_row_teens,
        f"the publishing counter refuses for a branch nobody has read, naming it (got {publishing_row_teens!r})",
    )
finally:
    kit._CONSOLE_FORM = saved_model


# --- Data safety: the wizard is answered from this slot's own pack, or refused ---------------------
# The `Безопасность данных` wizard was read whole on 2026-09-29, and one of its six rows could not be
# answered from the pack at all. The screen is modelled here for the same reason every other
# declaration is: the human walks it field by field, and a guide that does not name a row is a guide
# that stops at that row. Four things are legs below, and each of them is a failure mode rather than
# a decoration:
#
# - **The row's truth is the pack's, not this repo's.** `listing.dataSafety` is derived by
#   `scripts/lib/capabilities.mjs` from the capability registry, so the collecting row for this slot
#   is the Play Billing SDK's own `dataCollected` entry. The legs hand in the registry's own bytes and
#   assert the cell the sheet prints, so a row written for one app cannot pass for a derivation.
# - **A token is not an answer.** The pack stores `Financial info` and `App functionality`; the screen
#   shows `Финансовые сведения` → `История покупок` and `Функции приложения`, and the model's two
#   vocabularies are the only place that pair exists. A token the vocabulary does not carry is a
#   refusal naming the token, because the Russian label of an unread category is a string nobody can
#   find on screen.
# - **The directions nobody read are refused.** The session saw one branch: collecting, not shared,
#   not optional, no accounts. `shared`, `optional` and the accounts branch are other screens of the
#   same wizard, so the sheet names the work instead of repeating the branch it saw.
# - **One row has no answer, and it says so.** `Эти данные обрабатываются временно?` is a claim about
#   how Google Play processes a purchase, and `listing.dataSafety` carries no retention window; the
#   cell is a `TODO-HUMAN` carrying the model's own sentence and naming the field, because a plausible
#   default here is an assertion nobody checked.

ds_screen = screen_by_id("declaration-data-safety")
ds_fields = {field["id"]: field for field in ds_screen["fields"]}
ds_table = model["dataSafety"]


def ds_cell(text: str, field_id: str) -> str:
    """The `what to type` cell of one Data safety row, or '' when the sheet prints no row for it.

    Reading the cell rather than asking whether a string occurs somewhere in the sheet is the
    difference between a leg about the row a human pastes and a leg about a word that happens to
    appear in the document. The row is `поле | лимит | что ввести | почему`, so the cell is what sits
    between the cap column and the reason, and the reason is dropped by splitting off the last column.
    """
    prefix = f"| {field_label(ds_fields[field_id])} | — | "
    for entry in text.splitlines():
        if entry.startswith(prefix) and entry.endswith(" |"):
            return entry[len(prefix):-2].rsplit(" | ", 1)[0]
    return ""


def ds_fixture(data_collected, sdks=(), **safety):
    """A generated sheet for a slot whose `listing.dataSafety` differs in one named way."""
    _, store_ds, listing_ds = make_slot(
        [literal("recordbook_pro")],
        enabled=["billing"],
        data_collected=list(data_collected),
        sdks=list(sdks),
    )
    if safety:
        listing_ds["dataSafety"].update(safety)
        kit.write_documents(store_ds, listing_ds, list(LOCALES), False, META)
    return sheet(store_ds), listing_ds


def ds_option(field_id: str, option_id: str) -> str:
    """The console's own label for one option of one row, taken from the model's field."""
    field = ds_fields[field_id]
    return field["optionsRu"][field["optionIds"].index(option_id)]


check(
    ds_screen.get("declaration") is True
    and ds_screen.get("kind") == "declaration"
    and 1 <= int(ds_screen.get("declarationOrder", 0)) <= 10,
    "Data safety is one of the model's ten App-content declarations, in the console's own order",
)
check(
    ds_table.get("wizardSteps") == 5 and "2026-09-29" in str(ds_table.get("observed")),
    f"the model records the five-step wizard the session walked (got {ds_table.get('wizardSteps')})",
)
check(
    kit.console_field_name(ds_fields["temporaryProcessing"]) == "Эти данные обрабатываются временно?",
    "the one row whose wording was read carries the console's own question, not a gloss",
)
check(
    "{field}" in str(ds_table["temporaryRefusalEn"]),
    "the refusal is a sentence with a placeholder, so a cell cannot print it without naming its row",
)

# The slot that declares nothing: the console asks its opening questions of every app, and the pack's
# own answer decides which of them the human even sees.
quiet_text, quiet_listing = ds_fixture([])
check(
    kit.data_safety_answers(quiet_listing)["collectsData"] is False
    and ds_cell(quiet_text, "collectsOrShares") == ds_option("collectsOrShares", "no"),
    f"a slot that collects nothing answers the first question with the console's own «no» label "
    f"(got {ds_cell(quiet_text, 'collectsOrShares')!r})",
)
check(
    ds_cell(quiet_text, "accountCreation") == ds_option("accountCreation", "no-accounts"),
    f"the accounts row prints the branch the pack's own document declares "
    f"(got {ds_cell(quiet_text, 'accountCreation')!r})",
)
check(
    ds_cell(quiet_text, "dataDeletion") == ds_option("dataDeletion", "no"),
    f"the deletion row prints the model's own «no» label (got {ds_cell(quiet_text, 'dataDeletion')!r})",
)
check(
    ds_cell(quiet_text, "encryptedInTransit").startswith("not asked")
    and ds_cell(quiet_text, "encryptedInTransit") not in ds_fields["encryptedInTransit"]["optionsRu"],
    "a slot with nothing to transmit is told the encryption row is not asked, never that it is «no»",
)
check(
    ds_cell(quiet_text, "dataCategories").startswith("not asked"),
    f"the category step is not asked of a pack with no data type (got {ds_cell(quiet_text, 'dataCategories')!r})",
)

# The slot that collects: the row is the bundled SDK's own declaration, taken from the registry.
billing_capability = json.loads(
    (ROOT / "capabilities" / "billing" / "capability.json").read_text(encoding="utf-8")
)
registry_types = billing_capability["dataCollected"]
registry_type = registry_types[0]["type"] if registry_types else ""
check(
    len(registry_types) == 1 and registry_type in ds_table["dataTypeVocabulary"],
    "the type the Play Billing registry declares is one the model's vocabulary can name "
    f"(got {[item['type'] for item in registry_types]})",
)
collect_text, collect_listing = ds_fixture(registry_types, billing_capability["sdkDependencies"])
# Every read below is defensive on purpose: the legs that follow are the verdict for a vocabulary
# that lost this pair, and a suite that raises `KeyError` on the defect it was written against
# reports no verdict at all. A missing entry composes a cell nothing can equal, so the row legs fail
# while naming what the model is missing.
financial = ds_table["dataTypeVocabulary"].get(registry_type) or {}
purpose_entry = ds_table["purposeVocabulary"].get(str(registry_types[0]["purpose"])) if registry_types else {}
purpose_label = str((purpose_entry or {}).get("labelRu") or "")
expected_category = ds_table["categoryCellEn"].format(
    category=financial.get("categoryRu"),
    subcategory=financial.get("subcategoryRu"),
    collected=ds_table["surveyAnswersRu"]["collected"],
    required=ds_table["surveyAnswersRu"]["required"],
    purpose=purpose_label,
)
check(
    ds_cell(collect_text, "collectsOrShares") == ds_option("collectsOrShares", "yes")
    and ds_cell(collect_text, "encryptedInTransit") == ds_option("encryptedInTransit", "yes"),
    "a pack with a bundled transmitting SDK answers both opening questions with the console's «Да»",
)
check(
    ds_cell(collect_text, "dataCategories") == expected_category,
    "the category row is the model's vocabulary composing the registry's own type and purpose "
    f"(got {ds_cell(collect_text, 'dataCategories')!r})",
)
check(
    f"{financial.get('categoryRu')} → {financial.get('subcategoryRu')}"
    in ds_cell(collect_text, "dataCategories")
    and ds_table["surveyAnswersRu"]["required"] in ds_cell(collect_text, "dataCategories"),
    "the row names the console's category, its subcategory, and that the collection is mandatory",
)
check(
    any(
        "bundled" in line and str(sdk["name"]) in line
        for sdk in billing_capability["sdkDependencies"]
        for line in collect_text.splitlines()
    ),
    "the sheet attributes the «Да» to the bundled SDK by name, so the row is not read as the app's own code",
)
check(
    purpose_label != "" and purpose_label in collect_text,
    "the purpose is printed in the console's own Russian wording, never as the pack's English token",
)

# Tokens the vocabulary does not carry, and directions nobody has read: work named, never a guess.
# A fixture row carries `shared` and `optional` because the pack's own `store/data-safety.md` prints
# both columns from them; a row missing either is not a row this pack can carry.
unknown_type_text, _ = ds_fixture(
    [{"type": "Fitness info", "purpose": "App functionality", "shared": False, "optional": False}]
)
check(
    ds_cell(unknown_type_text, "dataCategories")
    == "TODO-HUMAN: data type 'Fitness info' is not in the model's vocabulary",
    f"a data type the vocabulary does not carry is refused by name (got "
    f"{ds_cell(unknown_type_text, 'dataCategories')!r})",
)
unknown_purpose_text, _ = ds_fixture(
    [
        {
            "type": registry_types[0]["type"],
            "purpose": "Advertising or marketing",
            "shared": False,
            "optional": False,
        }
    ]
)
check(
    "TODO-HUMAN" in ds_cell(unknown_purpose_text, "dataCategories")
    and "Advertising or marketing" in ds_cell(unknown_purpose_text, "dataCategories"),
    "a purpose the vocabulary does not carry is refused by name rather than translated by eye",
)
shared_text, _ = ds_fixture([dict(registry_types[0], shared=True)])
check(
    "TODO-HUMAN" in ds_cell(shared_text, "dataCategories")
    and "shared" in ds_cell(shared_text, "dataCategories"),
    "a type the pack declares shared names the screen nobody has read instead of the branch that was",
)
optional_text, _ = ds_fixture([dict(registry_types[0], optional=True)])
check(
    "TODO-HUMAN" in ds_cell(optional_text, "dataCategories")
    and "optional" in ds_cell(optional_text, "dataCategories"),
    "a type the pack declares optional names the same unread screen, for the same reason",
)
accounts_text, _ = ds_fixture([], hasAccountDeletion=True)
check(
    ds_cell(accounts_text, "accountCreation").startswith("TODO-HUMAN")
    and ds_cell(accounts_text, "dataDeletion").startswith("TODO-HUMAN")
    and ds_option("accountCreation", "no-accounts") not in accounts_text,
    "a pack that declares a deletion mechanism gets work on both rows, not the no-accounts branch",
)

# The one row with no pack answer. It has to be a refusal that names itself: a `Да` or a `Нет` here is
# an assertion about how Google Play processes a purchase, which is exactly what nobody has checked.
temporary_field = ds_fields["temporaryProcessing"]
expected_refusal = f"TODO-HUMAN: {str(ds_table['temporaryRefusalEn']).format(field=kit.console_field_name(temporary_field))}"
check(
    ds_cell(collect_text, "temporaryProcessing") == expected_refusal,
    f"the transient-processing row prints the model's refusal, naming itself "
    f"(got {ds_cell(collect_text, 'temporaryProcessing')[:60]!r}...)",
)
# The model carries no options for this row - nobody read its radios - so the leg the row needs is not
# "none of its own labels leaked" (a set that is empty cannot fail) but "no label the wizard uses
# elsewhere was guessed into it". «Да» and «Нет» are the labels of the rows above it, and one of them
# is exactly the plausible default this cell must never print.
wizard_radios = {
    label
    for field_id in ("collectsOrShares", "encryptedInTransit")
    for label in ds_fields[field_id]["optionsRu"]
}
check(
    wizard_radios != set()
    and ds_cell(collect_text, "temporaryProcessing").startswith("TODO-HUMAN: ")
    and ds_cell(collect_text, "temporaryProcessing") not in wizard_radios,
    "the row is a refusal and not one of the wizard's own radio labels, which is the plausible default it must never print",
)
check(
    kit.data_safety_answers(collect_listing).get("temporaryProcessing") is None,
    "the pack carries no value for that row, which is why the cell refuses instead of answering",
)

# Both guides are rendered from the one model, so the owner-facing walkthrough has to carry the same
# open question: a sheet that refuses while the Russian walkthrough stays silent would leave the
# human with a TODO and no explanation of what it is waiting for.
ds_walkthrough = (ROOT / model["docPath"]).read_text(encoding="utf-8")
data_safety_screen = screen_by_id("declaration-data-safety")
check(
    f"## {data_safety_screen['order']}. Безопасность данных" in ds_walkthrough
    and kit.console_field_name(temporary_field) in ds_walkthrough
    and str(ds_fields["temporaryProcessing"]["whyRu"]) in ds_walkthrough,
    "the walkthrough carries the Data safety screen and the reason the transient row is open",
)


# --- How completion is read: the console's own counters, never the wizard's step strip -------------
# The owner read `Шаг 1 из 5 … Шаг 5 из 5` as "one item closed out of five" and took a declaration
# that was already saved for an unfinished one. The strip is a map of the wizard: it counts nothing,
# the console carries no `N из 5` counter anywhere on the page, and a step's tick does not survive a
# reload. What does report a saved declaration is the console's own counters, and both documents have
# to say so - a guide that lists which fields to fill in without saying how completion is read is
# exactly the gap that produced the misreading. These legs fail on a statement that drops the warning,
# on counters that stop being named, and on a skipped step that stops being described as it appears.


completion = model["declarationCompletion"]
check(
    len(completion["counters"]) >= 4 and all(counter.get("shows") for counter in completion["counters"]),
    f"the model names the console's completion counters with the literal each one shows ({len(completion['counters'])})",
)


def counter_segments(shows: str) -> list[str]:
    """The parts of a counter's `shows` that are not a per-slot substitution.

    One counter prints the age group in the branch's own words, and that is not a shape the model can
    state: it has to hold what surrounds the placeholder and let the reader fill the rest, so a leg
    asserting the counter was printed asserts the segments rather than a string the model cannot carry
    without stating one app's answer.
    """
    return [part for part in str(shows).split("{audienceStoreRow}") if part]


check(
    all(all(part in text for part in counter_segments(counter["shows"])) for counter in completion["counters"]),
    "the sheet reads completion off the console's own counters, not off the wizard's step strip",
)
check(
    "not a completion counter" in completion["warningEn"] and "not a completion counter" in text,
    "the sheet says in so many words that the `Шаг N из M` strip is not a completion counter",
)
check(
    "until the page is reloaded" in text,
    "the sheet says a step's tick is transient, so a missing tick is not a missing declaration",
)
check(
    all(name in text for name in ("Требуется действие", "Обработано", "Выполнено:", "Сохранить")),
    "the counters are quoted by the names the console gives them, so the human can find them",
)

walkthrough = (ROOT / model["docPath"]).read_text(encoding="utf-8")
check(
    all(
        all(part in walkthrough for part in counter_segments(counter["shows"]))
        for counter in completion["counters"]
    )
    and completion["warningRu"] in walkthrough,
    "the generated walkthrough carries the same completion statement as the sheet, from the same model",
)

# The counters used to print one app's numbers and one app's age group as if they were every slot's:
# `9 обработанных деклараций`, `Выполнено: 5 из 11` and `Целевая возрастная группа: 18 и старше.` are
# readings of the 2026-09-29 session on a billing gradebook, and a slot that is not that app was being
# told to look for numbers it does not have. The numbers are now the console's own shapes, and the age
# group is a substitution the reader fills from the slot's branch.
publishing = next(
    (counter for counter in completion["counters"] if "{audienceStoreRow}" in str(counter["shows"])),
    None,
)
check(
    publishing is not None
    and "Обзор публикации" in str(publishing["whereRu"])
    and "Publishing overview" in str(publishing["whereEn"]),
    f"the publishing-overview counter prints the age group through a placeholder (got {publishing})",
)
check(
    all("18 и старше" not in str(counter["shows"]) for counter in completion["counters"]),
    "no counter states the observed app's age group as if it were every slot's",
)
check(
    all(
        token not in str(counter["shows"])
        for counter in completion["counters"]
        for token in ("9 обработанных", "5 из 11", "(10)")
    )
    and str(completion["numbersRuleEn"]) in text,
    "the counters carry the console's shapes and the rule that `N` is the slot's own number, so no counter "
    "sends a human looking for another app's count",
)
check(
    "18 и старше" in text
    and all("{audienceStoreRow}" not in line for line in text.splitlines() if line.startswith("- **")),
    "the sheet substitutes the wording of the branch this slot declares and leaves no placeholder behind in the table "
    "(the rule sentence that explains the placeholder is allowed to name it)",
)

# A skipped step has to be described by what the console shows there, or an inactive tab reads as work
# the human failed to finish. The sentence is the model's and `{steps}` is filled per branch, so the
# note cannot describe one branch's steps beside another branch's answer.
skipped_note = kit.target_audience_skipped_note(adults)
check(
    len(kit.target_audience_skipped_steps(adults)) == 3
    and all(f"«{step}»" in skipped_note for step in kit.target_audience_skipped_steps(adults)),
    "the skipped-steps note names every step this branch's console greys out, in the console's titles",
)
check(
    "aria-disabled" in skipped_note and "is-disabled" in skipped_note and "`Далее`" in skipped_note,
    "the note says what the human sees on those steps: inactive tabs, and `Далее` opening the Summary",
)
check(
    skipped_note in text,
    "the per-slot sheet carries the model's skipped-steps note for the branch this slot declares",
)
illustration_branch = audience_table["audiences"][audience_table["skippedStepsNoteBranch"]]
illustration_note = str(audience_table["skippedStepsNoteRu"]).replace(
    "{steps}", ", ".join(f"«{step}»" for step in kit.target_audience_skipped_steps(illustration_branch))
)
check(
    illustration_note in walkthrough,
    "the walkthrough renders the same note for the branch it illustrates, with the steps filled in",
)
saved_note_model = kit._CONSOLE_FORM
try:
    probe_model = copy.deepcopy(saved_note_model)
    probe_model["targetAudience"]["skippedStepsNoteEn"] = "those tabs are inert"
    kit._CONSOLE_FORM = probe_model
    try:
        kit.target_audience_skipped_note(adults)
        note_refused = False
    except kit.StoreKitError:
        note_refused = True
    check(
        note_refused,
        "a note that stops naming the branch's steps is refused, not printed as a sentence about nothing",
    )
finally:
    kit._CONSOLE_FORM = saved_note_model

# --- Every declaration field is answered by a reader, and every radio has a label -----------------
# The sensor that fails a model whose declaration field is filled from the memory of whichever slot
# wrote it. Three states are legal - a reader, a declared formality, and a label-choosing field whose
# labels are read somewhere the model names - and silence is not one of them. Each leg below breaks
# one of the three and expects the message that names the field.


def reader_errors_with(probe: dict) -> list[str]:
    saved_form = kit._CONSOLE_FORM
    try:
        kit._CONSOLE_FORM = probe
        return kit.console_model_reader_errors()
    finally:
        kit._CONSOLE_FORM = saved_form


check(
    kit.console_model_reader_errors() == [],
    "every declaration field of the model is filled by a reader, a formality, or a read label",
)

probe = copy.deepcopy(model)
for entry in probe["screens"]:
    if entry["id"] == "declaration-data-safety":
        entry.pop("needsVerification", None)
        entry.pop("needsVerificationRu", None)
probe_errors = reader_errors_with(probe)
check(
    len(probe_errors) == 1
    and "declaration-data-safety" in probe_errors[0]
    and "labels the model does not carry" in probe_errors[0],
    f"a radio whose labels are nowhere and whose screen claims no gap is a failure (got {probe_errors})",
)

probe = copy.deepcopy(model)
probe.pop("labelSources", None)
probe_errors = reader_errors_with(probe)
check(
    len(probe_errors) == 2
    and all("declaration-target-audience" in row for row in probe_errors),
    f"a label supplied elsewhere must be declared there, or the fields that rely on it fail (got {probe_errors})",
)

probe = copy.deepcopy(model)
probe["labelSources"]["no.such.literal"] = "nothing reads this"
probe_errors = reader_errors_with(probe)
check(
    len(probe_errors) == 1 and "nothing reads" in probe_errors[0],
    f"a label source nothing declares is a dangling pointer, reported as one (got {probe_errors})",
)

probe = copy.deepcopy(model)
for entry in probe["screens"]:
    if entry["id"] == "declaration-ads":
        entry["fields"][0].pop("literal")
        entry["fields"][0].pop("slotKey")
probe_errors = reader_errors_with(probe)
check(
    len(probe_errors) == 1 and "has no literal" in probe_errors[0],
    f"a declaration field with neither reader nor formality is a failure (got {probe_errors})",
)

# A declaration's answer comes from its own `slotKey`, and the literal `declaration` is filled by the
# reader that dispatches on that key. Until this leg existed the two were only related at generation
# time: a screen naming a pack path nobody reads produced a `StoreKitError` on the machine of whoever
# was shipping, so the model could hold a declaration no slot can answer and every check stayed green.
check(
    {str(entry.get("slotKey")) for entry in kit.console_declarations()} <= kit.console_declaration_readers(),
    "every declaration's slotKey is answered by a branch of console_declaration_answer",
)

probe = copy.deepcopy(model)
for entry in probe["screens"]:
    if entry["id"] == "declaration-health-apps":
        entry["slotKey"] = "listing.healthData"
probe_errors = reader_errors_with(probe)
check(
    len(probe_errors) == 1 and "'listing.healthData'" in probe_errors[0] and "no branch for it" in probe_errors[0],
    f"a declaration whose pack path no reader reads is a failure, not a crash at generation (got {probe_errors})",
)

# The model's own list of readers and the readers this module implements. The docstring of
# `console_literal_readers` claimed the test suite compared the two; nothing did, so a reader the model
# declared but the code never wrote was a field naming a literal that would fall back to `valueRu` -
# this app's value, printed in the next app's guide.
probe = copy.deepcopy(model)
probe["literalReaders"]["declaration.totallyNew"] = "a reader nobody wrote"
probe_errors = reader_errors_with(probe)
check(
    len(probe_errors) == 1 and "declaration.totallyNew" in probe_errors[0] and "no dispatch" in probe_errors[0],
    f"a declared reader with no dispatch is a failure (got {probe_errors})",
)

probe = copy.deepcopy(model)
probe["literalReaders"].pop("derived.minorGate")
probe_errors = reader_errors_with(probe)
check(
    len(probe_errors) == 1 and "derived.minorGate" in probe_errors[0] and "does not declare" in probe_errors[0],
    f"an implemented reader the model does not declare is a failure (got {probe_errors})",
)

# --- A field's label is the console's row, stored once ---------------------------------------------
# Both guides head their first column "the field, as the console shows it", and the handoff compares a
# live transcription against that label. So a label carrying the guide's own annotation is not merely
# untidy: it is a string the human cannot find, and it makes the handoff reject a file that quoted the
# console correctly. A label the model already holds is read from the one place it lives - the field
# had a second copy of the minor-restriction box's sentence, already drifted by adding «».


def label_errors_with(probe: dict) -> list[str]:
    saved_form = kit._CONSOLE_FORM
    try:
        kit._CONSOLE_FORM = probe
        return kit.console_model_reader_errors()
    finally:
        kit._CONSOLE_FORM = saved_form


resolved_gate = kit.console_field_name(field_by_id("declaration-target-audience", "minorGate"))
check(
    resolved_gate == model["targetAudience"]["minorGate"]["labelRu"] and "«" not in resolved_gate,
    f"the optional gate's label is read from the branch that answers the box, not restated (got {resolved_gate!r})",
)

probe = copy.deepcopy(model)
for entry in probe["screens"]:
    if entry["id"] == "declaration-target-audience":
        for item in entry["fields"]:
            if item["id"] == "minorGate":
                item["nameRu"] = probe["targetAudience"]["minorGate"]["labelRu"]
probe_errors = label_errors_with(probe)
check(
    len(probe_errors) == 1 and "never both" in probe_errors[0],
    f"a label stored on the field and read from the model is a second copy, reported as one (got {probe_errors})",
)

probe = copy.deepcopy(model)
for entry in probe["screens"]:
    if entry["id"] == "declaration-target-audience":
        for item in entry["fields"]:
            if item["id"] == "minorGate":
                item["nameFrom"] = "targetAudience.noSuchLabel"
probe_errors = label_errors_with(probe)
check(
    len(probe_errors) == 1 and "resolves to nothing" in probe_errors[0],
    f"a label path that resolves to nothing is a field with no label, reported as one (got {probe_errors})",
)

ANNOTATED_LABELS = [
    ("app-status", "appStatus", "App status (Состояние приложения)"),
    ("app-status", "requiredActions", "Контент приложения → «Требуются действия» (счётчик)"),
    ("release", "releaseNotes", "Заметки о выпуске («What's new»)"),
    ("declaration-target-audience", "ageGroups", "Целевая возрастная группа · «Какие целевые возрастные группы у вашего приложения?» (шесть чекбоксов)"),
]
for screen_id, field_id, annotated in ANNOTATED_LABELS:
    probe = copy.deepcopy(model)
    for entry in probe["screens"]:
        if entry["id"] == screen_id:
            for item in entry["fields"]:
                if item["id"] == field_id:
                    item["nameRu"] = annotated
    probe_errors = label_errors_with(probe)
    check(
        len(probe_errors) == 1 and "labels the row" in probe_errors[0] and annotated in probe_errors[0],
        f"{screen_id} → {field_id}: the annotated label this audit removed is refused again (got {probe_errors})",
    )

probe = copy.deepcopy(model)
for entry in probe["screens"]:
    for sub in entry.get("subscreens") or []:
        if sub["id"] == "content-rating-step-2":
            sub.pop("nameNotQuotedRu")
probe_errors = label_errors_with(probe)
check(
    len(probe_errors) == 1 and "without a reason to read" in probe_errors[0],
    f"an exemption with no statement beside it is a failure (got {probe_errors})",
)
check(
    "**The label column of this screen is not verbatim:**" in text,
    "the screen whose labels are a gloss says so in the generated sheet, above its own table",
)
check(
    "**Подпись строки здесь не дословная:**" in walkthrough,
    "the same disclosure reaches the Russian walkthrough, so neither guide presents a gloss as a row",
)


# --- An answer no slot decides is a token of the catalogue, not a word in a field ------------------
# Three declarations answer «no» for every slot this factory ships. They used to carry that word in
# their own `valueEn`, which is the "answer written for one app" pattern in its purest form: a
# lowercase English token in a cell whose whole point is to be a label the human can find. The
# property is now a `constant.no` the reader resolves, and the cell is the same label a radio shows.

CATALOGUE_CONSTANTS = [
    ("declaration-government-apps", "government"),
    ("declaration-financial-features", "financial"),
    ("declaration-health-apps", "health"),
]
for screen_id, field_id in CATALOGUE_CONSTANTS:
    entry = field_by_id(screen_id, field_id)
    check(
        entry.get("literal") == "declaration" and entry.get("slotKey") == "constant.no",
        f"{screen_id} answers through a reader, so the token it names has a reader to print it",
    )
    label, label_notes = kit.console_choice_label(entry, "no")
    check(
        entry["valueEn"] not in text and f"| {field_label(entry)} | — | {label} |" in text,
        f"{screen_id} prints the label of the `no` token ({label!r}), not the field's own prose",
    )

saved_form = kit._CONSOLE_FORM
try:
    probe_form = copy.deepcopy(saved_form)
    for entry in probe_form["screens"]:
        if entry["id"] == "declaration-health-apps":
            entry["fields"][0]["valueEn"] = "no"
            entry["fields"][0].pop("literal")
    kit._CONSOLE_FORM = probe_form
    health = kit.console_screen("declaration-health-apps")
    cell = kit.console_field_cell(kit.console_field(health, "health"), health, rating_ctx)[0]
    check(
        cell == "no",
        f"without the reader the field's prose becomes the answer, which is what the reader replaced (got {cell!r})",
    )
finally:
    kit._CONSOLE_FORM = saved_form


# --- The questionnaire's reason belongs to the branch, not to the row ------------------------------
# Step 2 of the Content rating form is one app's answers until the pack names which branch it takes.
# The row used to carry the reason for *this* slot as well as the branch carrying it, so the two would
# drift the first time a branch was corrected - and the digital-goods row stated this slot's own
# capability as if it were the rule every slot follows.

step_two_fields = list(step_two["fields"])
check(
    all(entry.get("whyRuFrom") for entry in step_two_fields),
    "every questionnaire row points at the branch that holds its reason",
)
check(
    all(not entry.get("whyRu") for entry in step_two_fields),
    "no questionnaire row carries a reason of its own, so a branch edit cannot leave a second copy",
)
unresolved = [entry["whyRuFrom"] for entry in step_two_fields if kit.console_model_value(entry["whyRuFrom"]) is None]
check(
    unresolved == [],
    f"every pointer a questionnaire row uses resolves to the branch's own reason (unresolved: {unresolved})",
)
check(
    all("listing.contentRating.questionnaire" in str(entry.get("whyEn")) for entry in step_two_fields),
    "every questionnaire row states the rule - the pack's branch decides the answer",
)
check(
    "capability" not in str(field_by_id("content-rating-step-2", "q-digital-goods")["whyEn"]),
    "the digital-goods row no longer claims this slot's capability as the form's rule",
)
observed_branch = model["contentRating"]["questionnaires"]["notes"]
branch_reasons = observed_branch["answers"]
check(
    all(str(branch_reasons[entry["questionId"]].get("whyRu", "")).strip() for entry in step_two_fields),
    "the observed branch carries one reason per question, which is where the walkthrough reads it",
)
walkthrough = ROOT / "docs" / "play-console-walkthrough.md"
walkthrough_text = walkthrough.read_text(encoding="utf-8") if walkthrough.is_file() else ""
check(
    bool(walkthrough_text) and branch_reasons["q-digital-goods"]["whyRu"] in walkthrough_text,
    "the generated walkthrough prints the branch's reason in the questionnaire table, not the row's",
)
check(
    all(
        str(branch_reasons[entry["questionId"]]["whyRu"]) in walkthrough_text
        for entry in step_two_fields
    ),
    "every questionnaire row of the walkthrough carries its reason from the branch",
)


# --- The questionnaire branch is a token of the vocabulary, and an unknown one is a refusal --------
# The same discipline as the audience: the pack names *which set of answers* this app takes, the
# category radio and the ten answers are derived from it, and a branch nobody has read in the console
# is refused rather than invented. The refusal is the point - an invented questionnaire passes every
# check on paper.

for spelling in ("notes", "Notes", "NOTES"):
    probe_listing = {**listing, "contentRating": {**listing["contentRating"], "questionnaire": spelling}}
    token, probe_branch, notes = kit.content_rating_branch(probe_listing)
    check(
        (token, notes) == ("notes", []) and probe_branch["answers"] == branch_reasons,
        f"the spelling {spelling!r} resolves to the observed branch instead of a second one",
    )

unknown_questionnaire = {**listing, "contentRating": {**listing["contentRating"], "questionnaire": "scores"}}
token, _, notes = kit.content_rating_branch(unknown_questionnaire)
check(
    token is None and bool(notes) and "TODO-HUMAN" in notes[0] and "notes" in notes[0],
    "a questionnaire branch nobody has read is refused with the vocabulary named, rather than answered",
)
blank_questionnaire = {**listing, "contentRating": {**listing["contentRating"], "questionnaire": ""}}
token, _, notes = kit.content_rating_branch(blank_questionnaire)
check(
    token is None and bool(notes) and "TODO-HUMAN" in notes[0],
    "an empty questionnaire is work to do, named as such, rather than silently the observed branch",
)

# The digital-goods answer is gated on the slot's own capabilities, and the two directions carry their
# own reasons: the reason for «Да» is false in the slot that answers «Нет», so one reason for both
# directions explains neither.
gated_plan = kit.content_rating_plan(observed_branch, "notes", ["billing"])
check(
    gated_plan["q-digital-goods"][0] == "yes"
    and gated_plan["q-digital-goods"][1] == branch_reasons["q-digital-goods"]["tokenWhyEn"],
    "a slot that sells something answers the digital-goods question «Да», with that direction's reason",
)
ungated_plan = kit.content_rating_plan(observed_branch, "notes", [])
check(
    ungated_plan["q-digital-goods"][0] == "no"
    and ungated_plan["q-digital-goods"][1] == branch_reasons["q-digital-goods"]["otherwiseWhyEn"],
    "a slot that sells nothing answers the same question «Нет», with the other direction's reason",
)
check(
    all(answer == "no" for question, (answer, _) in ungated_plan.items() if question != "q-digital-goods"),
    "the observed branch's nine unconditional answers do not depend on a capability",
)

saved_form = kit._CONSOLE_FORM
try:
    gate_probe = copy.deepcopy(saved_form)
    gate_probe["contentRating"]["questionnaires"]["notes"]["answers"]["q-digital-goods"].pop("otherwise")
    kit._CONSOLE_FORM = gate_probe
    raised = ""
    try:
        kit.content_rating_plan(
            gate_probe["contentRating"]["questionnaires"]["notes"], "notes", []
        )
    except kit.StoreKitError as exc:
        raised = str(exc)
    check(
        "otherwise" in raised and "q-digital-goods" in raised,
        f"a capability-gated answer with no fallback is a refusal, not a question left unanswered (got {raised!r})",
    )
finally:
    kit._CONSOLE_FORM = saved_form


# --- The live-transcription handoff: a file is believed only when it says what the console said -----
# A second worker transcribes the console in a browser and writes one JSON file per screen, so the
# factory learns the console's own wording instead of deriving it. The file is the input a program
# folds, which is exactly why it is the most dangerous input here: a paraphrase, an unknown schema
# version, a colliding field id or a `persisted: true` nobody watched are the four ways an invented
# model entry would get in. Everything below is a fixture - no file under `.kiln/console-observations/`
# is read, and nothing under it is written.

audience_screen = screen_by_id("declaration-target-audience")
audience_model_fields = {entry["id"]: entry for entry in kit.console_screen_fields(audience_screen)}

# The labels the live console printed on 2026-09-29 (`.kiln/console-observations/`), written here as
# fixtures rather than read out of the model. A leg that copies `model["nameRu"]` into the file it
# then validates proves only that the model agrees with itself: it passes whether the model stores the
# console's row or the guide's own annotation of it, and an annotation is a string the human cannot
# find on the screen. These are the console's words, so a model that drifts from them fails.
LIVE_AGE_QUESTION = "Какие целевые возрастные группы у вашего приложения?"
LIVE_MINOR_GATE = (
    "Запретить доступ к моему приложению пользователям, которые указали в аккаунте Google, "
    "что они несовершеннолетние (необязательно)"
)


def observation(**overrides) -> dict:
    """One observation file in the schema the transcriber was given, valid unless a leg says otherwise."""
    base = {
        "schemaVersion": 1,
        "screenId": "declaration-target-audience",
        "consoleNameRu": audience_screen["consoleNameRu"],
        "observedAt": "2026-09-29",
        "branch": "adults-only",
        "steps": [{"id": "step-1", "titleRu": "Возраст целевой группы"}],
        "fields": [
            {
                "id": "ageGroups",
                "nameRu": LIVE_AGE_QUESTION,
                "nameEn": None,
                "kind": "checkbox",
                "optionsRu": ["От 18 лет"],
                "valueRu": None,
                "slotKey": None,
                "whyRu": "наблюдено в консоли",
                "whyEn": "observed in the console",
            }
        ],
        "actions": [{"labelRu": "Далее", "observed": True}],
        "saveObserved": True,
        "persisted": True,
        "notesRu": ["одна заметка"],
        "citation": "observed-console-2026-09-29",
    }
    base.update(overrides)
    return base


def observation_file(name: str, payload: dict) -> Path:
    path = Path(tempfile.mkdtemp(prefix="observation-")) / name
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return path


def fold_applied_rows(payload: dict) -> list[str]:
    """The model edits a fold really writes for one fixture, with the model restored afterwards.

    The report prints an implied edit whether or not it was written, so a leg about a fold has to look
    at the rows the fold itself returned - and run against a copy of the model, because the point of the
    leg is that some files must not change it.
    """
    saved = kit._CONSOLE_FORM
    try:
        kit._CONSOLE_FORM = copy.deepcopy(saved)
        _code, rows = kit.console_observation_rows(observation_file("folded.json", payload), apply=True)
        return [
            row
            for row in rows
            if row.startswith("IMPLIED EDIT ") and not row.startswith("IMPLIED EDIT (not applied)")
        ]
    finally:
        kit._CONSOLE_FORM = saved


valid_observation = observation()
failures, implied, unknown = kit.validate_console_observation(valid_observation, "fixture")
check(
    (failures, unknown) == ([], []),
    f"a file in the published schema is accepted with no failures and no stray keys (got {failures + unknown})",
)
check(
    any("observedAt <- 2026-09-29" in row for row in implied)
    and not any("targetAudience.steps" in row for row in implied),
    f"an accepted file names the model edits it implies, and a step the model already carries implies none (got {implied})",
)
# The step-title comparison is the model's own answer rather than an assumption: on 2026-09-29 the
# report claimed "the 5 observed step title(s), where the model carries none" about a wizard whose
# titles the model already held, so a reader was sent to look for work that did not exist. A step the
# model does not carry is still an edit - the two legs below are the same file with and without it.
new_step = observation(steps=[{"id": "step-9", "titleRu": "Шаг 9, которого в модели нет"}])
_failures, implied_new_step, _unknown = kit.validate_console_observation(new_step, "fixture")
check(
    any("targetAudience.steps" in row and "step-9" in row for row in implied_new_step),
    f"a step title the model does not carry is still named as the edit it implies (got {implied_new_step})",
)
check(
    all("valueRu" not in row for row in implied),
    f"an observation never implies a model value: a field's cell comes from a reader (got {implied})",
)

# The screen's own name and the field's own name are the console's, and a paraphrase is the way a
# *different* screen would get folded into this one - the model would then carry the guide's gloss
# where the observed row belongs, and both guides would print a string no human can find on the page.
# Both comparisons read the label the model *resolves*, so a field whose label the model states once
# (through `nameFrom`) is not mistaken for a paraphrase.
paraphrase_screen = observation(consoleNameRu="Целевая аудитория")
failures, _implied, _unknown = kit.validate_console_observation(paraphrase_screen, "fixture")
check(
    len(failures) == 1 and "a paraphrase is not the console's label" in failures[0],
    f"a screen named in the transcriber's own words is rejected, not folded in (got {failures})",
)
paraphrase_field = observation()
paraphrase_field["fields"][0]["nameRu"] = "Возрастные группы (пересказ)"
failures, _implied, _unknown = kit.validate_console_observation(paraphrase_field, "fixture")
check(
    len(failures) == 1 and "the console's own label is what the model carries" in failures[0],
    f"a field label that paraphrases the console's row is rejected too (got {failures})",
)

code, rows = kit.console_observation_rows(observation_file("valid.json", valid_observation), apply=False)
check(
    code == 0 and any(row.startswith("ACCEPTED ") for row in rows),
    f"a valid file is ACCEPTED through the file-level entry point (got {rows[-1:]})",
)
check(
    any(row.startswith("IMPLIED EDIT (not applied) ") for row in rows),
    "without --apply-observations every implied edit is printed as work, not written",
)

code, rows = kit.console_observation_rows(
    observation_file("v2.json", observation(schemaVersion=2)), apply=False
)
check(
    code == 1
    and any("schemaVersion is 2" in row for row in rows)
    and any(row.startswith("REJECTED ") for row in rows),
    f"an unknown schema version is rejected by its number (got {rows[-1:]})",
)

missing_key = observation()
del missing_key["citation"]
code, rows = kit.console_observation_rows(observation_file("missing.json", missing_key), apply=False)
check(
    code == 1 and any("missing the required key 'citation'" in row for row in rows),
    f"a missing schema key is rejected by name, not ignored (got {rows[-1:]})",
)

missing_field_key = observation()
# `whyRu` rather than one of the label keys: the 2026-09-29 handoff showed why the label keys cannot be
# required - a row of a wizard step that never rendered carries `literalQuestionRu: null`, which is the
# honest statement of what was seen, and `nameRu` is not a key such a row has. The leg is about the rule
# the two shapes share: a field states which row it is and why it is in the file, and one that does not
# is rejected by the name of the key it left out.
missing_field_key["fields"][0].pop("whyRu")
code, rows = kit.console_observation_rows(observation_file("missingfield.json", missing_field_key), apply=False)
check(
    code == 1 and any("fields[0] is missing the required key 'whyRu'" in row for row in rows),
    "a field missing one of the schema's keys is rejected the same way",
)

duplicate = observation(
    fields=[
        dict(valid_observation["fields"][0]),
        dict(valid_observation["fields"][0], nameRu=kit.console_field_name(audience_model_fields["minorGate"])),
    ]
)
code, rows = kit.console_observation_rows(observation_file("duplicate.json", duplicate), apply=False)
check(
    code == 1 and any("declares the field id 'ageGroups' 2 times" in row for row in rows),
    f"a field id declared twice is rejected: the two would fold into each other (got {rows[-1:]})",
)

collision = observation(
    fields=[dict(valid_observation["fields"][0], id="ads")]
)
code, rows = kit.console_observation_rows(observation_file("collision.json", collision), apply=False)
check(
    code == 1 and any("reuses the field id 'ads'" in row and "declaration-ads" in row for row in rows),
    f"a field id another screen already owns is rejected, naming the screen (got {rows[-1:]})",
)

paraphrase = observation(consoleNameRu="Целевая аудитория")
code, rows = kit.console_observation_rows(observation_file("paraphrase.json", paraphrase), apply=False)
check(
    code == 1
    and any("a paraphrase is not the console's label" in row for row in rows)
    and any(audience_screen["consoleNameRu"] in row for row in rows),
    f"a paraphrased screen name is rejected against the model's own label (got {rows[-1:]})",
)

paraphrase_field = observation(
    fields=[dict(valid_observation["fields"][0], nameRu="Возрастные группы (примерно)")]
)
code, rows = kit.console_observation_rows(observation_file("paraphrasefield.json", paraphrase_field), apply=False)
check(
    code == 1 and any("the console's own label is what the model carries" in row for row in rows),
    f"a paraphrased field name is rejected against the model's own label (got {rows[-1:]})",
)

# The other half of the same rule, and the defect this audit removed: the model stored the row as an
# annotation - a heading joined to the question, and the gate's sentence wrapped in «» - so a file that
# quoted the console *correctly* was rejected as a paraphrase, while a file echoing the annotation
# passed. The labels below are the console's own, taken from the live session's transcription.
check(
    kit.console_field_name(audience_model_fields["ageGroups"]) == LIVE_AGE_QUESTION
    and kit.console_field_name(audience_model_fields["minorGate"]) == LIVE_MINOR_GATE,
    "the model stores the console's own wording for the audience rows",
)

gated = observation(
    fields=[
        dict(valid_observation["fields"][0]),
        dict(
            valid_observation["fields"][0],
            id="minorGate",
            nameRu=LIVE_MINOR_GATE,
            kind="checkbox",
            optionsRu=["отмечен", "снят"],
            whyRu="аудитория взрослая",
        ),
    ]
)
code, rows = kit.console_observation_rows(observation_file("gated.json", gated), apply=False)
check(
    code == 0 and any(row.startswith("ACCEPTED ") for row in rows),
    f"a file that quotes the console's own wording for a label the model reads elsewhere is accepted (got {rows[-1:]})",
)

annotated = observation(
    fields=[
        dict(
            valid_observation["fields"][0],
            nameRu="Целевая возрастная группа · «Какие целевые возрастные группы у вашего приложения?» (шесть чекбоксов)",
        )
    ]
)
code, rows = kit.console_observation_rows(observation_file("annotated.json", annotated), apply=False)
check(
    code == 1 and any("the console's own label is what the model carries" in row for row in rows),
    f"the annotated label the model used to store is no longer a label a file can match (got {rows[-1:]})",
)

kind_mismatch = observation(fields=[dict(valid_observation["fields"][0], kind="radio")])
code, rows = kit.console_observation_rows(observation_file("kindmismatch.json", kind_mismatch), apply=False)
check(
    code == 0
    and any("shown as 'radio' in the console and the model calls it 'checkbox'" in row for row in rows),
    f"a control the transcription saw as a radio is an edit the model owes, not a failure (got {rows})",
)

unsaved = observation(persisted=True, saveObserved=False)
code, rows = kit.console_observation_rows(observation_file("unsaved.json", unsaved), apply=False)
check(
    code == 1 and any("saveObserved: false" in row for row in rows),
    f"a claim that the console saved something nobody watched saving is rejected (got {rows[-1:]})",
)

unconfirmed_screen = screen_by_id("declaration-data-safety")
unconfirmed = observation(
    screenId="declaration-data-safety",
    consoleNameRu=unconfirmed_screen["consoleNameRu"],
    fields=[],
)
code, rows = kit.console_observation_rows(observation_file("unconfirmed.json", unconfirmed), apply=False)
check(
    code == 1
    and any("NEEDS-VERIFICATION" in row and "not an observation" in row for row in rows),
    f"`persisted: true` for a screen the console never confirmed is rejected (got {rows[-1:]})",
)

# The other half of the same discipline: a file about a screen the model has not learned yet is work,
# not a refusal. The console page it transcribed is real - the file carries its path, its heading and
# its rows - so what the model owes is the screen, and the file's own claim must not reach the model,
# because there is nothing in the model to write it into.
unknown_screen = observation(screenId="declaration-no-such-screen", consoleNameRu="Не существует")
code, rows = kit.console_observation_rows(observation_file("unknownscreen.json", unknown_screen), apply=False)
check(
    code == 0 and any("must add a screen 'declaration-no-such-screen'" in row for row in rows),
    f"a file about a screen the model does not carry is accepted with the screen it owes (got {rows[-1:]})",
)
check(
    fold_applied_rows(unknown_screen) == [],
    "and a screen the model cannot look up is never written: the fold touches only its own screens",
)


# --- The shapes the first real handoff was written in ---------------------------------------------
# The four transcriptions of 2026-09-29 were written from the live console, and the schema they were
# checked against named neither the keys they used nor a way to report a screen nobody could reach: all
# four were rejected, three of them for being honest about what the console showed. The legs below are
# those shapes. The rules that must stay strict are beside them, because a shape that is wider is not a
# shape that believes anything: a key the schema has not named is still reported, a row that does not
# quote the console is still refused, and a claim with nothing behind it is still rejected.
live_actions = observation(
    actions=[
        {"order": 1, "actionRu": "«Далее» → «Шаг 5 из 5: Сводка»", "observedEffectRu": "переход"},
        {"order": 2, "actionRu": "Перезагрузка страницы", "observedEffectRu": "значения сохранились"},
    ]
)
code, rows = kit.console_observation_rows(observation_file("actionru.json", live_actions), apply=False)
check(
    code == 0 and not any("actions[" in row for row in rows),
    f"what the observer clicked, in the live transcription's own words, is a shape the schema names (got {rows})",
)

unlabelled_action = observation(actions=[{"order": 1, "observedEffectRu": "переход"}])
code, rows = kit.console_observation_rows(observation_file("nolabelaction.json", unlabelled_action), apply=False)
check(
    code == 1 and any("actions[0] names no action" in row for row in rows),
    f"an action that says what was clicked nowhere is still rejected (got {rows[-1:]})",
)

question_row = observation(
    fields=[
        {
            "id": "ageGroups",
            "literalQuestionRu": LIVE_AGE_QUESTION,
            "literalOptionsRu": ["От 18 лет"],
            "answerRu": "«От 18 лет» отмечено",
            "whyRu": "строка «Сводки», шаг 1",
        }
    ]
)
code, rows = kit.console_observation_rows(observation_file("questionrow.json", question_row), apply=False)
check(
    code == 0
    and not any(row.startswith("NOTE ") for row in rows)
    and any("must not become a model value" in row for row in rows),
    f"the question a wizard step asks is a named label, and its answer is refused a place in the model (got {rows})",
)

unlabelled_row = observation(fields=[{"id": "someRow", "kind": "note", "whyRu": "метки нет"}])
code, rows = kit.console_observation_rows(observation_file("nolabelrow.json", unlabelled_row), apply=False)
check(
    code == 1 and any("carries no console label" in row for row in rows),
    f"a row that quotes no console label is refused rather than matched loosely (got {rows[-1:]})",
)

unrendered_row = observation(
    fields=[{"id": "noQuestionRead", "literalQuestionRu": None, "answerRu": None, "whyRu": "шаг не отрисовался"}],
)
code, rows = kit.console_observation_rows(observation_file("unrenderedrow.json", unrendered_row), apply=False)
check(
    code == 0
    and not any(row.startswith("NOTE ") for row in rows)
    and any("its console label was not read" in row for row in rows),
    f"a row whose question could not be read is a fact, not a field the model may gain (got {rows})",
)

step_shapes = observation(
    steps=[
        {
            "id": "step-1",
            "index": 1,
            "titleRu": "Возраст целевой группы",
            "reachable": True,
            "requiredForThisBranch": True,
            "buttonsRu": ["Далее", "Назад"],
            "buttonStateRu": "«Далее» — enabled; «Назад» — disabled",
            "actionTakenRu": "Нажата «Далее»",
        },
        {
            "id": "step-5",
            "index": 5,
            "titleRu": "Сводка",
            "reachable": True,
            "requiredForThisBranch": True,
            "buttonsRu": None,
            "actionTakenRu": "«Сохранить» не нажималась: кнопка неактивна",
        },
    ]
)
code, rows = kit.console_observation_rows(observation_file("stepshapes.json", step_shapes), apply=False)
check(
    code == 0 and not any(row.startswith("NOTE ") for row in rows),
    f"a step's own record - its tab, its buttons, what they did - is a shape the schema names (got {rows})",
)

step_without_title = observation(steps=[{"id": "step-1", "reachable": True}])
code, rows = kit.console_observation_rows(observation_file("steptitless.json", step_without_title), apply=False)
check(
    code == 1 and any("steps[0] is missing the required key 'titleRu'" in row for row in rows),
    f"a step without the title the model folds is still rejected (got {rows[-1:]})",
)

overview = observation(
    screenId="app-content-overview",
    consoleNameRu="Контент приложения",
    pagePathObserved="play.google.com/console/u/0/developers/1/app/1/app-content/overview",
    pageIntroRu="Предоставьте информацию о контенте в своем приложении",
    app={"packageName": "com.gexetr.teacher-gradebook"},
    tabs=[{"nameRu": "Декларации", "selected": True}],
    dashboardCard={"titleRu": "Контент приложения", "statusRu": "Требуются действия"},
    publishingOverview={"statusRu": "Готово к публикации"},
    completionCounter={"totalRu": "0 из 10"},
    persisted=False,
    saveObserved=False,
)
for key in ("fields", "steps", "branch"):
    overview.pop(key, None)
code, rows = kit.console_observation_rows(observation_file("overviewshape.json", overview), apply=False)
check(
    code == 0
    and not any(row.startswith("NOTE ") for row in rows)
    and not any("must add a screen" in row for row in rows),
    f"a page that lists declarations instead of carrying fields is a shape the model now carries (got {rows})",
)
check(
    fold_applied_rows(overview) == ["IMPLIED EDIT screens[app-content-overview].observedAt <- 2026-09-29"],
    "the page the model names is folded for its provenance and for nothing that was read on it",
)

# The other half of the same rule: a page the model really does not carry still owes the screen it
# names, and a fold must not write a screen by itself. The file's label is unknown as well, because a
# label the model already carries resolves the file by name - which is the point of the leg above.
unmapped_page = observation(
    screenId="app-content-overview-of-an-unread-app",
    consoleNameRu="Контент приложения (страница, которой модель не знает)",
    tabs=[{"nameRu": "Декларации", "selected": True}],
    persisted=False,
    saveObserved=False,
)
for key in ("fields", "steps", "branch"):
    unmapped_page.pop(key, None)
code, rows = kit.console_observation_rows(observation_file("unmappedpage.json", unmapped_page), apply=False)
check(
    code == 0
    and not any(row.startswith("NOTE ") for row in rows)
    and any("must add a screen 'app-content-overview-of-an-unread-app'" in row for row in rows),
    f"a page the model does not carry is accepted with the screen it owes (got {rows})",
)
check(
    fold_applied_rows(unmapped_page) == [],
    "and a page the model does not carry writes nothing: the fold touches only its own screens",
)

# A screen the observer searched for and did not find is the one report the old schema could not accept
# at all: it has no fields, no actions and no branch, because there was nothing to read. What makes it
# evidence is where it looked and what the console showed there instead.
#
# The model carries no such finding any more, and that is the record of the 2026-09-29 walk: the dialog
# the finding used to name was opened and read, so it is a sub-screen of the flow that owns the button
# (`screens[publishing] → submit-dialog`). The machinery still has to be checked - a refusal the model
# states is not work, and a refusal it does not state is an edit - so the legs below run against a
# *probe* that carries one, exactly as the model did before the walk. Pinning them to live data would
# have meant keeping a finding the console no longer supports.
FINDING_FIXTURE_ID = "first-publication-declarations"
FINDING_FIXTURE_LABEL = "Требования первой публикации, которых нет ни на одной доступной странице"


def model_with_finding(item_id: str = FINDING_FIXTURE_ID) -> dict:
    """A probe whose `unreachable` section carries one finding, for the legs about that machinery.

    The shape is the schema's rather than one session's: a finding names the item, why it could not be
    read, where the search happened and what to do instead, and it carries no rows.
    """
    probe = copy.deepcopy(model)
    probe["unreachable"] = {
        "observed": "probe",
        "titleRu": "Чего нет ни на одной доступной странице",
        "titleEn": "Not on any reachable page",
        "ruleRu": "пункт, которого нет ни на одной прочитанной странице, — это отрицательная находка, "
        "а не экран",
        "ruleEn": "an item that is not a row of the page where it was expected is a negative finding "
        "rather than a screen",
        "items": [
            {
                "id": item_id,
                "titleRu": FINDING_FIXTURE_LABEL,
                "titleEn": "A first-publication requirement on no reachable page",
                "namesRu": ["Соблюдение правил программы для разработчиков"],
                "namesEn": ["Developer Program Policies compliance"],
                "namesNotReadRu": "имя дано владельцем, а не прочитано на странице",
                "namesNotReadEn": "the name is the owner's rather than a row read on a page",
                "blockerRu": "не отрисовано ни на одной доступной странице консоли",
                "blockerEn": "not rendered on any reachable page of the console",
                "searchedRu": [".../app-content/overview", ".../app-content/declarations"],
                "searchedEn": [".../app-content/overview", ".../app-content/declarations"],
                "nextActionRu": "спросить владельца, где он их принимал",
                "nextActionEn": "ask the owner where they were accepted",
            }
        ],
    }
    return probe


def observation_rows_with(probe: dict, name: str, payload: dict, apply: bool = False):
    """`console_observation_rows` against a probe model, restored afterwards."""
    saved_form = kit._CONSOLE_FORM
    try:
        kit._CONSOLE_FORM = probe
        return kit.console_observation_rows(observation_file(name, payload), apply=apply)
    finally:
        kit._CONSOLE_FORM = saved_form


def unreachable_ids_with(probe: dict) -> set:
    """`console_unreachable_ids` against a probe model, so a leg can see the finding it supplies."""
    saved_form = kit._CONSOLE_FORM
    try:
        kit._CONSOLE_FORM = probe
        return kit.console_unreachable_ids()
    finally:
        kit._CONSOLE_FORM = saved_form


negative = observation(
    screenId=FINDING_FIXTURE_ID,
    consoleNameRu=FINDING_FIXTURE_LABEL,
    foundOnAnyReachableScreen=False,
    searchedScreens=[
        {"pathObserved": ".../app-content/overview", "resultRu": "ссылки на эти пункты нет"},
        {"pathObserved": ".../app-content/declarations", "resultRu": "пункта «Первая публикация» нет"},
    ],
    certifications=[
        {
            "id": "developer-program-policies",
            "nameRuAsGivenByOwner": "Соблюдение правил программы для разработчиков",
            "observedCheckboxLabelRu": None,
            "ticked": None,
            "stateRu": "не найдено ни на одной странице",
            "slotKey": None,
            "whyRu": "дважды искали в семи местах живой консоли",
        }
    ],
    blockerRu="чекбоксы не отрисованы ни на одной доступной странице консоли",
    bestNextActionRu="спросить владельца, где он их принимал",
)
for key in ("fields", "actions", "persisted", "saveObserved", "steps", "branch"):
    negative.pop(key, None)
code, rows = observation_rows_with(model_with_finding(), "negative.json", negative)
check(
    code == 0
    and not any(f"must state {FINDING_FIXTURE_ID!r}" in row for row in rows)
    and not any("must add a screen" in row for row in rows),
    f"a refusal the model already states is not printed as work, and is never a screen to add (got {rows})",
)
check(
    FINDING_FIXTURE_ID in unreachable_ids_with(model_with_finding())
    and not any(
        node.get("id") == FINDING_FIXTURE_ID
        for screen in kit.console_screens()
        for node in [screen] + list(screen.get("subscreens") or [])
    )
    and kit.console_unreachable_ids() == set()
    and "unreachable" not in model,
    "the refusal is carried as a finding rather than as a screen with rows nobody saw, and the model "
    "itself carries none: everything the walk searched for was read",
)

# The same file about an item the model says nothing about is the edit it implies - and still a refusal
# rather than a screen, which is the part a tolerant schema must not lose.
unrecorded_negative = dict(
    negative,
    screenId="an-item-no-page-shows",
    consoleNameRu="Пункт, которого нет ни на одной прочитанной странице",
)
code, rows = kit.console_observation_rows(
    observation_file("unrecordednegative.json", unrecorded_negative), apply=False
)
check(
    code == 0
    and any("must state 'an-item-no-page-shows'" in row for row in rows)
    and not any("must add a screen" in row for row in rows),
    f"a refusal the model does not state is the edit it implies (got {rows})",
)

negative_unsearched = dict(negative)
del negative_unsearched["searchedScreens"]
code, rows = kit.console_observation_rows(observation_file("negativesearch.json", negative_unsearched), apply=False)
check(
    code == 1 and any("missing the required key 'searchedScreens'" in row for row in rows),
    f"a negative finding that does not say where it searched is rejected (got {rows[-1:]})",
)

negative_claiming = dict(negative, persisted=True)
code, rows = kit.console_observation_rows(observation_file("negativeclaim.json", negative_claiming), apply=False)
check(
    code == 1 and any("is a negative finding and claims persisted" in row for row in rows),
    f"a negative finding may not also claim the console kept something (got {rows[-1:]})",
)

negative_unreported_page = dict(negative, searchedScreens=[{"pathObserved": ".../app-content/overview"}])
code, rows = kit.console_observation_rows(
    observation_file("negativepage.json", negative_unreported_page), apply=False
)
check(
    code == 1 and any("searchedScreens[0] is missing the required key 'resultRu'" in row for row in rows),
    f"a searched page that does not say what it showed is not a search (got {rows[-1:]})",
)

# A negative finding about a screen the model *does* carry is the sharpest case: it would be easy to
# read "not found anywhere" as provenance for the screen it names, and writing it would record the
# opposite of what the file reports.
modelled_negative = dict(
    negative, screenId="declaration-target-audience", consoleNameRu=audience_screen["consoleNameRu"]
)
check(
    fold_applied_rows(modelled_negative) == [],
    "a screen that was searched for and not found keeps no provenance from the search",
)
# The screen the model does carry already has provenance from the reading that *did* see it, so the
# leg asserts on a screen with none: a file that reports a search and no rows must not turn emptiness
# into a reading, and the check runs the fold instead of only printing it.
ads_negative = dict(negative, screenId="declaration-ads", consoleNameRu="Реклама")
saved_form = kit._CONSOLE_FORM
try:
    kit._CONSOLE_FORM = copy.deepcopy(saved_form)
    kit.console_observation_rows(observation_file("adsnegative.json", ads_negative), apply=True)
    check(
        kit.console_screen("declaration-ads").get("observedAt") is None,
        "a negative finding about a screen the model carries gives it no provenance, even when the fold runs",
    )
finally:
    kit._CONSOLE_FORM = saved_form

# A saved state nobody watched being saved is believed when the file says what it read instead. The
# statement is the only thing that makes `persisted: true` an observation rather than a hope, and it is
# required by name - the reader has to know which key to fill.
evidence_claim = observation(
    persisted=True,
    saveObserved=False,
    persistedEvidenceRu="страница перезагружена: «От 18 лет» и запрет несовершеннолетним сохранились",
)
code, rows = kit.console_observation_rows(observation_file("evidence.json", evidence_claim), apply=False)
check(
    code == 0
    and not any(row.startswith("NOTE ") for row in rows)
    and not any(row.startswith("FAIL ") for row in rows),
    f"a declaration seen saved after a reload is believed without a save being watched (got {rows})",
)
check(
    kit.OBSERVATION_EVIDENCE_KEYS[0] == "persistedEvidenceRu",
    "the key the schema asks for by name is the one the live handoff wrote, so the correction is fillable",
)
legacy_evidence = observation(persisted=True, saveObserved=False, persistedEvidence="reloaded, values kept")
code, rows = kit.console_observation_rows(observation_file("legacyevidence.json", legacy_evidence), apply=False)
check(
    code == 0,
    "the schema's earlier name for the same statement is still read, so a file written before it is not refused",
)

# Two of the four transcription files were named after the *page* they were read on rather than the
# screen the model carries. A paraphrase is refused; this is not a paraphrase - the file quotes the
# console's own label - so the file is believed and the naming question is printed for a reader.
# `declaration-testing-credentials` is the console's own slug for that page (`/app-content/testing-credentials`),
# and the model's screen for it takes its id from the console's English label (`declaration-app-access`).
page_named = observation(
    screenId="declaration-testing-credentials",
    consoleNameRu="Учетные данные",
    fields=[
        {
            "id": "hasRestrictedSections",
            "nameRu": "В вашем приложении есть закрытые разделы?",
            "valueRu": "нет",
            "slotKey": None,
            "whyRu": "первая строка страницы «Учетные данные»",
        },
        {
            "id": "brand-new-row",
            "nameRu": "Строка, которой в модели нет",
            "valueRu": "нет",
            "slotKey": None,
            "whyRu": "строка, которую модель ещё не знает",
        },
    ],
    actions=[{"order": 1, "actionRu": "Открыта страница «Учетные данные»", "observedEffectRu": "показана"}],
    persisted=True,
    saveObserved=False,
    persistedEvidenceRu="значения сохранены консолью до этой сессии",
)
for key in ("steps", "branch"):
    page_named.pop(key, None)
code, rows = kit.console_observation_rows(observation_file("pagenamed.json", page_named), apply=False)
check(
    code == 0
    and any("is the console label of 'declaration-app-access'" in row for row in rows)
    and any("must gain the field 'brand-new-row'" in row for row in rows)
    and not any("must gain the field 'hasRestrictedSections'" in row for row in rows),
    f"a file named after its page resolves to the screen the model carries, and its rows are compared with that screen (got {rows})",
)
check(
    fold_applied_rows(page_named) == [],
    "and which of the two names the model keeps stays a reader's question, so nothing is written",
)
check(
    not any(screen.get("id") == "declaration-testing-credentials" for screen in kit.console_screens())
    and kit.console_screen("declaration-app-access").get("observedAt") == "2026-09-29T18:46+03:00",
    "the file's own name creates no second screen and writes no provenance: the model's name keeps the reading recorded under it",
)

bad_kind = observation(fields=[dict(valid_observation["fields"][0], kind="checkbox-group")])
code, rows = kit.console_observation_rows(observation_file("badkind.json", bad_kind), apply=False)
check(
    code == 1 and any("the schema's kinds are" in row for row in rows),
    f"a field kind outside the schema is rejected rather than guessed at (got {rows[-1:]})",
)

unknown_keys = observation(extraTopLevel="kept", fields=[dict(valid_observation["fields"][0], extraFieldKey=1)])
path = observation_file("unknownkeys.json", unknown_keys)
code, rows = kit.console_observation_rows(path, apply=False)
check(
    code == 0 and any(row.startswith("NOTE ") and "extraTopLevel" in row for row in rows),
    f"a key the schema does not carry is reported as a NOTE and ignored, never silently dropped (got {rows[:3]})",
)
check(
    any(row.startswith("NOTE ") and "extraFieldKey" in row for row in rows),
    "the same report reaches the keys of a nested field",
)
check(
    any(row.startswith("ACCEPTED ") for row in rows),
    "an extended file is still accepted: a richer transcriber is not a rejected one",
)

unread_key = observation(fields=[dict(valid_observation["fields"][0], slotKey="listing.somethingNew")])
code, rows = kit.console_observation_rows(observation_file("unreadkey.json", unread_key), apply=False)
check(
    code == 0 and any("must gain a reader for the slot key 'listing.somethingNew'" in row for row in rows),
    f"a slot key no declaration reads is reported as a reader to write (got {rows[-1:]})",
)

prose_value = observation(fields=[dict(valid_observation["fields"][0], valueRu="«От 18 лет»")])
code, rows = kit.console_observation_rows(observation_file("prosevalue.json", prose_value), apply=False)
check(
    code == 0 and any("valueRu must not become a model value" in row for row in rows),
    f"a value written in the file is refused a place in the model (got {rows[-1:]})",
)

# The fold writes only what the model declares foldable. Everything else stays printed, because a
# field's answer, its options and its reason are decisions a reader takes with the model in front of
# them - and a tool that writes the model by its own judgement is the drift the model exists to stop.
saved_form = kit._CONSOLE_FORM
try:
    kit._CONSOLE_FORM = copy.deepcopy(saved_form)
    folded_path = observation_file("fold.json", valid_observation)
    code, rows = kit.console_observation_rows(folded_path, apply=True)
    applied = [row for row in rows if row.startswith("IMPLIED EDIT ") and not row.startswith("IMPLIED EDIT (not applied)")]
    remaining = [row for row in rows if row.startswith("IMPLIED EDIT (not applied)")]
    check(
        code == 0 and applied == [
            "IMPLIED EDIT screens[declaration-target-audience].observedAt <- 2026-09-29"
        ],
        f"a fold writes the screen's provenance and nothing else it was not told to write (got {applied})",
    )
    check(
        remaining != [] and all("valueRu" not in row for row in remaining),
        f"every other implied edit is left printed for a reader (got {remaining})",
    )
    check(
        kit.console_screen("declaration-target-audience").get("observedAt") == "2026-09-29",
        "the folded provenance is on the screen the file named",
    )
finally:
    kit._CONSOLE_FORM = saved_form

# The directory entry point: an empty handoff is a state of the work, a rejected file is not.
empty_dir = Path(tempfile.mkdtemp(prefix="observations-"))
buffer = io.StringIO()
with contextlib.redirect_stdout(buffer):
    code = kit.observation_report(empty_dir, apply=False)
check(
    code == 0 and "VERDICT: PASS — 0 file(s), 0 rejected" in buffer.getvalue(),
    f"a handoff with no file yet is a PASS with a note, not a failure (got {buffer.getvalue().strip()!r})",
)
buffer = io.StringIO()
with contextlib.redirect_stdout(buffer):
    code = kit.observation_report(empty_dir, apply=True)
check(
    code == 0,
    "an empty handoff is not a reason to rewrite the model",
)
buffer = io.StringIO()
with contextlib.redirect_stdout(buffer):
    code = kit.observation_report(observation_file("rejected.json", observation(schemaVersion=9)), apply=False)
check(
    code == 1 and "VERDICT: FAIL — 1 file(s), 1 rejected" in buffer.getvalue(),
    f"a rejected file fails the directory run even when a sibling would be accepted (got {buffer.getvalue().strip()[-80:]!r})",
)
check(
    kit.validate_console_observation(valid_observation, "fixture")[0] == [],
    "the fixture suite left the model's validation unchanged: no leg above wrote to it",
)

# A file missing a required key is still worth reading for the keys the schema has never heard of.
# The transcriber has to correct the file either way, and "missing key 'observedAt'" says nothing about
# the `actionRu`/`observedEffectRu` shape a live transcription used instead of the schema's `labelRu`/
# `observed` - which is exactly how the 2026-09-29 handoff failed on disk, with the schema's own
# top-level keys absent *and* its nested keys renamed.
partial_keys = observation(
    fields=[dict(valid_observation["fields"][0], annotRu="annotation")],
    actions=[{"actionRu": "Далее", "observedEffectRu": "шаг 2"}],
)
del partial_keys["observedAt"]
code, rows = kit.console_observation_rows(observation_file("partialkeys.json", partial_keys), apply=False)
check(
    code == 1 and any("missing the required key 'observedAt'" in row for row in rows),
    f"a file missing a required top-level key is rejected on that key (got {rows[-1:]})",
)
check(
    any(row.startswith("NOTE ") and "fields[0].annotRu" in row for row in rows),
    f"the same file still has its unknown nested keys reported, so the correction is complete (got {rows})",
)

# A radio that carries the console's own option labels has to say which answer token each one means.
# `console_choice_label` matches the two positionally, so a field with `optionsRu` and no `optionIds`
# printed the model's English gloss while the model claimed to hold the console's words - and the
# screen was not marked unobserved either, so nothing in either guide said the wording was unread.
def option_id_errors_with(probe: dict) -> list[str]:
    saved = kit._CONSOLE_FORM
    try:
        kit._CONSOLE_FORM = probe
        return kit.console_model_reader_errors()
    finally:
        kit._CONSOLE_FORM = saved


check(
    option_id_errors_with(model) == [],
    "every radio of the model that carries the console's labels also carries their answer tokens",
)
ads_probe = copy.deepcopy(model)
ads_field = next(
    field for screen in ads_probe["screens"] if screen["id"] == "declaration-ads" for field in screen["fields"]
)
ads_field.pop("optionIds")
ads_errors = option_id_errors_with(ads_probe)
check(
    len(ads_errors) == 1
    and "declaration-ads → ads" in ads_errors[0]
    and "optionIds" in ads_errors[0],
    f"a field holding the console's labels without their tokens is a failure naming the field (got {ads_errors})",
)
saved_form = kit._CONSOLE_FORM
try:
    kit._CONSOLE_FORM = ads_probe
    reader_refused = False
    try:
        kit.console_choice_label(ads_field, "no")
    except kit.StoreKitError:
        reader_refused = True
    check(
        reader_refused,
        "the reader refuses such a field too, instead of printing the gloss the model never asked for",
    )
finally:
    kit._CONSOLE_FORM = saved_form

# --- The two guides refuse the same row, from the same model -------------------------------------
# The rule is read above; these legs read what a human actually sees. The sheet is English and the
# walkthrough is Russian, so a refusal that names the field in one and offers a number in the other
# is exactly the drift the shared model exists to prevent - and the paste sheet is a *slot's* file, so
# what it prints has to come from that slot's pack or refuse.
owner_sheet = sheet(store_t)
for owner_screen, owner_field in OWNER_ROWS:
    owner = field_by_id(owner_screen, owner_field)
    owner_row = next(
        (
            line
            for line in owner_sheet.splitlines()
            if f"| {owner['nameRu']} (" in line and "TODO-HUMAN" in line
        ),
        "",
    )
    check(
        f"TODO-HUMAN: «{owner['nameRu']}»" in owner_row
        and str(owner.get("ownerDecisionEn") or "")[:40] in owner_row,
        f"the sheet refuses that row and names the field instead of printing a value (got {owner_row[:200]!r})",
    )
    check(
        owner_row != ""
        and f"TODO-HUMAN: «{owner['nameRu']}»" in sheet_cell(owner_row, 3)
        and "100" not in sheet_cell(owner_row, 3),
        f"the refusal is the cell a human reads, with no batch size to copy (got {sheet_cell(owner_row, 3)[:120]!r})",
    )

check(
    "TODO-HUMAN: «Количество кодов»" in walkthrough_text
    and "владелец выпускал 100" not in walkthrough_text
    and "партия из 100" not in walkthrough_text,
    "the Russian walkthrough refuses the same row from the same model, so the two guides agree",
)

# The walkthrough names the branch that supplied the publishing-overview wording *beside* the quoted
# row. The counter's `shows` is what the console printed, so an annotation between its backticks is a
# string a human cannot find on the screen - the same rule the field labels follow.
publishing_counter_model = next(
    (
        counter
        for counter in model["declarationCompletion"]["counters"]
        if "{audienceStoreRow}" in str(counter["shows"])
    ),
    {},
)
check(
    bool(publishing_counter_model),
    f"the model still carries the publishing-overview row the walkthrough annotates (got {publishing_counter_model})",
)
branch_row_wording = str(
    model["targetAudience"]["audiences"]["adults-only"]["storeRowAgeRu"]
)
counter_row = str(publishing_counter_model.get("shows", "")).replace(
    "{audienceStoreRow}", branch_row_wording
)
counter_line = next(
    (
        line
        for line in walkthrough_text.splitlines()
        if line.startswith("- **") and str(publishing_counter_model.get("whereRu")) in line
    ),
    "",
)
check(
    bool(counter_line) and counter_row in counter_line,
    f"the walkthrough substitutes the branch's wording into the quoted row (got {counter_line!r})",
)
check(
    f"{counter_row} · ветка" not in counter_line and "(ветка `adults-only`)" in counter_line,
    "the walkthrough names the branch outside the backticks that quote the console row, never inside it",
)

# --- The reviewer notes carry the same access the console declaration does -------------------------
# `store/review-notes.md`'s «Access» paragraph was a constant in the generator, and a constant is one
# app's answer printed for every app: a purchase-led slot shipped reviewer notes reading «No account,
# payment, invitation, or demo credentials are required» while its own console declaration held a
# promo code and its own app sold an unlock. The words now live beside the answer in
# `credentials.reviewAccess`, chosen by the same rule that answers the declaration, and these legs
# read them from the model instead of asserting a sentence - a leg that quotes prose passes for the
# wrong branch as soon as the prose changes.
review_model = (model.get("credentials") or {}).get("reviewAccess") or {}
review_paragraphs = review_model.get("paragraphsEn") or {}
check(
    set(review_paragraphs) == {str(rule["answer"]) for rule in model["credentials"]["rules"]},
    f"the model carries one reviewer-notes paragraph per answer its rules can give (got {sorted(review_paragraphs)})",
)
gated_rule = kit.credentials_rule(_listing_t["capabilityCompliance"])
gated_access = kit.review_access_paragraph(_listing_t["capabilityCompliance"])
check(
    gated_rule["answer"] == "yes"
    and gated_access == str(review_paragraphs["yes"]).replace("{reason}", str(gated_rule["reasonEn"])),
    f"a slot that gates content gets the rule's own paragraph with the rule's own reason (got {gated_access[:160]!r})",
)
gated_notes = (store_t / "review-notes.md").read_text(encoding="utf-8")
check(
    gated_access in gated_notes and str(review_paragraphs["no"]) not in gated_notes,
    "the generated reviewer notes carry that paragraph and not the other branch's",
)
ungated_access = kit.review_access_paragraph(listing_nb["capabilityCompliance"])
check(
    ungated_access == str(review_paragraphs["no"])
    and ungated_access != gated_access
    and ungated_access in (store_nb / "review-notes.md").read_text(encoding="utf-8"),
    f"an ungated slot's reviewer notes carry the other branch instead (got {ungated_access[:160]!r})",
)

# All four rules are four branches, and two of them are slots the factory has not built yet: the
# sign-in gate, and the slot that both sells and signs in. They are reached here by injecting the
# capabilities rather than by waiting for such a slot to exist, because the branch nobody exercises
# is the one whose paragraph drifts. The trap is the shared «yes» paragraph: a tail naming a promo
# code is true of the purchase branch and false of the sign-in one, so the branch-specific fact has
# to be the rule's own `{reason}`, and the shared tail may say only where the access is recorded.
# Collapsing `paragraphsEn["yes"]` back to one purchase-flavoured sentence reintroduces that defect
# and fails the tail leg below.
REVIEW_ACCESS_BRANCHES = (
    (["billing", "auth"], "gated-content-sign-in"),
    (["billing"], "gated-content"),
    (["auth"], "sign-in"),
    ([], "no-gate"),
)
for branch_caps, branch_template in REVIEW_ACCESS_BRANCHES:
    _branch_slot, branch_store, branch_listing = make_slot(
        [literal("recordbook_pro")], enabled=list(branch_caps), tags=[]
    )
    branch_compliance = branch_listing["capabilityCompliance"]
    branch_rule = kit.credentials_rule(branch_compliance)
    branch_text = kit.review_access_paragraph(branch_compliance)
    branch_notes = (branch_store / "review-notes.md").read_text(encoding="utf-8")
    check(
        str(branch_rule.get("templateKey")) == branch_template,
        f"enabled={branch_caps} resolves to the {branch_template!r} rule (got {branch_rule.get('templateKey')!r})",
    )
    check(
        branch_text
        == str(review_paragraphs[str(branch_rule["answer"])]).replace(
            "{reason}", str(branch_rule["reasonEn"])
        ),
        f"enabled={branch_caps} prints the {branch_template!r} rule's own paragraph with its own reason",
    )
    check(
        branch_text in branch_notes,
        f"the generated reviewer notes carry the {branch_template!r} paragraph for enabled={branch_caps}",
    )
    if branch_rule["answer"] == "yes":
        check(
            branch_text != review_paragraphs["no"],
            f"the {branch_template!r} branch does not print the «no access required» paragraph",
        )
        shared_tail = branch_text.split(str(branch_rule["reasonEn"]), 1)[-1]
        check(
            re.search(r"purchase|promo|sells|unlock", shared_tail, re.IGNORECASE) is None,
            "the shared «yes» tail names only where the access is recorded, never what the access is, so it "
            f"stays true of the {branch_template!r} branch too (got {shared_tail.strip()[:160]!r})",
        )


def review_access_with(probe: dict, compliance: dict) -> str:
    saved = kit._CONSOLE_FORM
    try:
        kit._CONSOLE_FORM = probe
        return kit.review_access_paragraph(compliance)
    finally:
        kit._CONSOLE_FORM = saved


missing_paragraph_probe = copy.deepcopy(model)
missing_paragraph_probe["credentials"]["reviewAccess"]["paragraphsEn"].pop("yes")
missing_paragraph = None
try:
    review_access_with(missing_paragraph_probe, _listing_t["capabilityCompliance"])
except kit.StoreKitError as exc:
    missing_paragraph = str(exc)
check(
    missing_paragraph is not None and "'yes'" in missing_paragraph,
    f"an answer whose branch has no paragraph is a refusal naming that answer (got {missing_paragraph!r})",
)
token_probe = copy.deepcopy(model)
token_probe["credentials"]["reviewAccess"]["paragraphsEn"]["yes"] = "Reach the unlock at {secretUrl}."
token_paragraph = None
try:
    review_access_with(token_probe, _listing_t["capabilityCompliance"])
except kit.StoreKitError as exc:
    token_paragraph = str(exc)
check(
    token_paragraph is not None and "placeholder" in token_paragraph,
    f"a paragraph carrying a token the rule cannot fill is refused, not printed (got {token_paragraph!r})",
)

# --- The screens the 2026-09-29 handoff landed, and what each one now answers ----------------------
# A transcription that is validated and ACCEPTED but never landed is the failure these legs exist
# against. The fold may write only what `observationFold` declares, and writing an observed *answer*
# would be the tool inventing one app's value for every app - so the durable facts a session read (the
# screen that exists, its rows, its literals, its branch structure, the item nobody could reach) reach
# the model only when a reader lands them. Until then every future slot's guide is generated without
# them and the next reader re-reads the same console windows.
#
# The legs read the model and the two rendered documents, never `.kiln/console-observations/`: a leg
# that reads the raw evidence passes on a machine that never ran the session, which is the opposite of
# what the model is for. The console's own words are quoted here as literals for the same reason the
# fixtures above quote them - a leg that copies `model["nameRu"]` into itself proves only that the
# model agrees with itself.

overview_screen = screen_by_id("app-content-overview")
check(
    overview_screen["kind"] == "read"
    and overview_screen["consoleNameRu"] == "Контент приложения"
    and str(overview_screen.get("pagePathObserved") or "").endswith("/app-content/overview"),
    f"the App-content overview is a screen of the model, with the console's own name and page path (got "
    f"{overview_screen.get('id')!r} / {overview_screen.get('consoleNameRu')!r})",
)
map_rows = [field for field in kit.console_screen_fields(overview_screen) if field.get("declarationRow")]
check(
    {str(field["declarationRow"]) for field in map_rows} == {str(entry["id"]) for entry in declarations},
    f"the map names the ten declarations and no other row (got {sorted(str(field['declarationRow']) for field in map_rows)})",
)
check(
    [str(field["declarationRow"]) for field in map_rows]
    == [
        "declaration-data-safety",
        "declaration-health-apps",
        "declaration-financial-features",
        "declaration-government-apps",
        "declaration-advertising-id",
        "declaration-target-audience",
        "declaration-content-rating",
        "declaration-app-access",
        "declaration-ads",
        "declaration-privacy-policy",
    ],
    "the map keeps the console's own row order, which is the page's work list rather than this document's order "
    f"(got {[str(field['declarationRow']) for field in map_rows]})",
)

needs_action_row = field_by_id("app-content-overview", "map-needs-action")
processed_row = field_by_id("app-content-overview", "map-processed")
check(
    "Все декларации заполнены." in str(needs_action_row["valueRu"])
    and "Заполнить декларацию" in str(needs_action_row["valueRu"])
    and "Все декларации заполнены." in walkthrough_text,
    "the empty tab's own two sentences are carried verbatim and printed, so a reader can recognise the tab that says nothing is left",
)
check(
    "Здесь показаны обработанные декларации правил." in str(processed_row["valueRu"])
    and "Последнее изменение" in str(processed_row["valueRu"])
    and "Последнее изменение" in walkthrough_text,
    "the processed list is carried with the body and the column a human reads a declaration's saved-ness off",
)
check(
    str(model["appContentMap"]["processedHeadingFormRu"]) == "N обработанных деклараций"
    and "N обработанных деклараций" in walkthrough_text
    and not any("10 обработанных" in str(field.get("valueRu") or "") for field in kit.console_screen_fields(overview_screen)),
    "the count of processed declarations is carried as the console's shape rather than as the observed app's number",
)

app_access_screen = screen_by_id("declaration-app-access")
app_access_fields = {field["id"]: field for field in kit.console_screen_fields(app_access_screen)}
check(
    app_access_screen["consoleNameRu"] == "Учетные данные"
    and str(app_access_screen.get("pagePathObserved") or "").endswith("/app-content/testing-credentials")
    and not any(screen.get("id") == "declaration-credentials" for screen in kit.console_screens()),
    "the App-access page is one screen under the console's own label, and the name it was carried under before names nothing",
)
check(
    set(app_access_fields)
    == {
        "hasRestrictedSections",
        "yes-branch-access-types",
        "credentials-entry",
        "reviewer-access-toggle",
        "no-branch-text",
    },
    f"the screen carries the rows the console shows, under the ids the transcription used (got {sorted(app_access_fields)})",
)
check(
    app_access_fields["hasRestrictedSections"]["kind"] == "radio"
    and app_access_fields["hasRestrictedSections"]["optionsRu"] == ["Да", "Нет"]
    and app_access_fields["hasRestrictedSections"]["nameRu"] == "В вашем приложении есть закрытые разделы?",
    "the page's own question and the two words of its radio are carried verbatim, because those are the words the human clicks",
)
ACCESS_TYPE_LINES = (
    "Учетные данные (например, адрес электронной почты), имя пользователя, вход с аккаунтом Google или "
    "система единого входа (SSO).",
    "Платежи, например за контент для однократных покупок, подписки или уровни доступа.",
    "Реферальные коды или QR-коды.",
    "Одноразовые PIN-коды или двухэтапная аутентификация.",
    "Биометрическая аутентификация.",
    "Действия, которые нужно выполнить на другом устройстве.",
)
access_types = app_access_fields["yes-branch-access-types"]
check(
    access_types["kind"] == "note"
    and access_types["modelFormality"] is True
    and all(line in str(access_types["valueRu"]) for line in ACCESS_TYPE_LINES),
    "the six lines the console lists under «Да» are carried verbatim, as the explanation they are rather than as six boxes to tick",
)
check(
    "Платежи, например за контент для однократных покупок, подписки или уровни доступа." in walkthrough_text,
    "the one line that puts a slot which sells an unlock on the «Да» branch is printed where the branch is chosen",
)
reviewer_row = app_access_fields["reviewer-access-toggle"]
check(
    reviewer_row["kind"] == "note"
    and reviewer_row["modelFormality"] is True
    and "done" in str(reviewer_row["valueRu"])
    and "Разрешить использовать эти учетные данные для тестирования вашего приложения" in str(reviewer_row["nameRu"]),
    "the reviewer-permission row is carried under the transcription's own id, and says the page draws a state icon rather than a control",
)
check(
    all(
        line in str(app_access_fields["no-branch-text"]["valueRu"])
        for line in (
            "В приложении нет разделов с ограниченным доступом.",
            "Вход в аккаунт не требуется ни в одной стране или регионе.",
            "Входить в аккаунт и платить за контент не требуется.",
        )
    ),
    "the branch that is not selected is carried with its three assertions, so the two answers can be told apart before one is chosen",
)

# The fold's own view of that page: the ids above are the transcription's, so a file named after the
# page and its rows lands on the screen the model carries. A row the model gained under a *different*
# id is what this leg is against - the consumer would print it as work and the next reader would add a
# second copy of a fact the model already holds.
observed_app_access = observation(
    screenId="declaration-app-access",
    consoleNameRu="Учетные данные",
    fields=[
        {
            "id": "hasRestrictedSections",
            "nameRu": "В вашем приложении есть закрытые разделы?",
            "kind": "radio",
            "optionsRu": ["Да", "Нет"],
            "whyRu": "первый вопрос страницы",
        },
        {
            "id": "yes-branch-access-types",
            "nameRu": "Что из перечисленного ниже требуется для доступа к приложению:",
            "kind": "note",
            "whyRu": "ветка «Да»",
        },
        {"id": "credentials-entry", "nameRu": "Учетные данные", "kind": "note", "whyRu": "таблица записей"},
        {
            "id": "reviewer-access-toggle",
            "nameRu": "Разрешить использовать эти учетные данные для тестирования вашего приложения на "
            "устройствах Google и надежных партнеров, а также получения отзывов о его работе",
            "kind": "note",
            "whyRu": "строка состояния с иконкой `done`",
        },
        {
            "id": "no-branch-text",
            "nameRu": "Что из перечисленного ниже верно для вашего приложения:",
            "kind": "note",
            "whyRu": "ветка «Нет»",
        },
    ],
    actions=[{"order": 1, "actionRu": "Открыта страница «Учетные данные»", "observedEffectRu": "показана ветка «Да»"}],
    persisted=True,
    saveObserved=False,
    persistedEvidenceRu="обе кнопки сохранения disabled, а строка числится в «Обработано» панели управления",
)
for key in ("steps", "branch"):
    observed_app_access.pop(key, None)
code, rows = kit.console_observation_rows(observation_file("appaccess.json", observed_app_access), apply=False)
check(
    code == 0
    and not any("must add a screen" in row for row in rows)
    and not any("must gain the field" in row for row in rows)
    and any("screens[declaration-app-access].observedAt" in row for row in rows),
    f"the reading of that page lands on the screen the model carries: no screen to add and no row to gain (got {rows})",
)
check(
    fold_applied_rows(observed_app_access)
    == ["IMPLIED EDIT screens[declaration-app-access].observedAt <- 2026-09-29"],
    "and the fold writes that screen's provenance and nothing that was read off it",
)

# The dialog the submit button opens is a screen, not a refusal. The model used to state the search
# instead of a screen, because on 2026-09-29 18:11 the button was disabled and the dialog could not be
# opened; the same day's walk pressed it, read the dialog and transcribed it, so the refusal became the
# screen below. Both halves are asserted: the three strings are carried and printed, the dialog has no
# owner decision in it (there are no checkboxes and no certifications at all), and it names the page and
# the file it was read from.
#
# Every leg below reads the *row* that owns a console sentence rather than searching a dump of the whole
# sub-screen, because a dump is satisfied by the node's own prose: the notes quote the same sentences,
# so a paraphrased row still matched and the leg could not fail. Observed by injection - paraphrasing
# `dialog-title.nameRu` left the suite green at 474/474 before the legs were pointed at the row
# (`.kiln/addendum-inject.py --defect dialog-title-paraphrased`).


def subscreen(screen_id: str, sub_id: str) -> dict:
    """One sub-screen, or `{}` so the legs about it print FAIL rows instead of killing the suite.

    This used to be a bare `next(...)`. The defect these legs exist to catch - a model that puts the
    dialog back where the refusal had it - removes the sub-screen entirely, and a `StopIteration` out
    of the middle of the run reports less than a FAIL row does: the suite dies before it prints a
    verdict, so an injection proof cannot even read the failure it provoked. Observed by injection
    (`.kiln/addendum-inject.py --defect dialog-unreachable`).
    """
    nodes = [
        sub
        for sub in screen_by_id(screen_id).get("subscreens") or []
        if sub.get("id") == sub_id
    ]
    return nodes[0] if nodes else {}


def row_of(owner: dict, field_id: str) -> dict:
    """One row of a sub-screen, or `{}`: the same refusal-as-a-FAIL-row rule as `subscreen` above."""
    for item in owner.get("fields") or []:
        if item.get("id") == field_id:
            return item
    return {}


submit_dialog = subscreen("publishing", "submit-dialog")
submit_dialog_json = json.dumps(submit_dialog, ensure_ascii=False)
dialog_title = row_of(submit_dialog, "dialog-title")
dialog_body = row_of(submit_dialog, "dialog-body")
dialog_buttons = row_of(submit_dialog, "dialog-buttons")
submit_dialog_strings = (
    "Отправить N изменений на проверку?",
    "Изменения будут отправлены на проверку в Google. Обычно она занимает до семи дней, но иногда "
    "может потребоваться больше времени.",
    "Отправка изменений на проверку",
)
check(
    submit_dialog.get("id") == "submit-dialog",
    "the dialog the submit button opens is a sub-screen of the screen that owns the button rather than a "
    "refusal (got "
    f"{[sub.get('id') for sub in screen_by_id('publishing').get('subscreens') or []]})",
)
check(
    kit.console_field_name(dialog_title) == submit_dialog_strings[0]
    and kit.console_field_name(dialog_body) == submit_dialog_strings[1]
    and "Отмена" in kit.console_field_name(dialog_buttons)
    and submit_dialog_strings[2] in kit.console_field_name(dialog_buttons),
    "the submit dialog is carried as a screen with the three strings the walk read - its title, its body "
    "and its own pair of buttons - each on the row that owns it (observed: "
    f"title {kit.console_field_name(dialog_title)!r}, body {kit.console_field_name(dialog_body)!r}, "
    f"buttons {kit.console_field_name(dialog_buttons)!r})",
)
check(
    all(needle in walkthrough_text for needle in submit_dialog_strings),
    "and the walkthrough prints all three, so the human meets the dialog the console shows instead of a "
    "refusal (missing: "
    f"{[n for n in submit_dialog_strings if n not in walkthrough_text]})",
)
check(
    str(submit_dialog.get("pagePathRu") or "").strip()
    and str(submit_dialog.get("pagePathObserved") or "").strip()
    and str(submit_dialog.get("citation") or "").strip()
    and str(submit_dialog.get("pagePathObserved") or "") in walkthrough_text
    and str(submit_dialog.get("pagePathRu") or "") in walkthrough_text,
    "the dialog names the page it was read on and the file that recorded it, and the guide prints that "
    "path beside the strings",
)
check(
    submit_dialog.get("id") == "submit-dialog"
    and not any(
        kit.console_field_answer_source(item) == "owner"
        for item in kit.console_screen_fields(submit_dialog)
    )
    and "Заявки" in submit_dialog_json,
    "and nothing in it is the owner's own decision: the dialog re-asks no certification, and it points "
    "at the dashboard dialog that owns the two first-publication ticks",
)
check(
    "submit-dialog" not in kit.console_unreachable_ids()
    and any(
        node.get("id") == "submit-dialog"
        for screen in kit.console_screens()
        for node in [screen] + list(screen.get("subscreens") or [])
    ),
    "the dialog the button opens is a documented screen rather than a refusal, which is the half the "
    "falsified finding got wrong",
)

# What follows the press is the second half of the ladder, and the trap that cost the owner a whole
# session lives in it: after the pre-review check there is no second confirmation button. The console
# says so itself, so the model carries the console's sentence, and both guides print it.
post_submit = subscreen("publishing", "post-submit-states")
pre_review = row_of(post_submit, "pre-review-check")
no_second = row_of(post_submit, "no-second-confirmation")
terminal_state = row_of(post_submit, "terminal-state")
no_second_confirmation = "Если на этом этапе не будет найдено проблем, изменения будут отправлены сразу."
ladder_tail_facts = (
    "Проверка на наличие распространенных проблем",
    "Осталось: максимум N минут",
    "Изменения на проверке",
    "Удалить изменения",
    "На рассмотрении",
)
check(
    post_submit.get("id") == "post-submit-states",
    "what follows the press is a sub-screen of the flow that owns the button (got "
    f"{[sub.get('id') for sub in screen_by_id('publishing').get('subscreens') or []]})",
)
check(
    kit.console_field_name(no_second) == no_second_confirmation
    and "Второй кнопки подтверждения после проверки нет" in str(no_second.get("valueRu") or ""),
    "the model carries the console's own sentence that the changes go to review when the check finds "
    "nothing, and states in so many words that there is no second confirmation button - the trap the "
    f"walk paid for (observed: {kit.console_field_name(no_second)!r})",
)
check(
    kit.console_field_name(pre_review) == ladder_tail_facts[0]
    and ladder_tail_facts[1] in str(pre_review.get("valueRu") or "")
    and kit.console_field_name(terminal_state) == ladder_tail_facts[2]
    and ladder_tail_facts[3] in str(terminal_state.get("valueRu") or "")
    and ladder_tail_facts[4] in str(terminal_state.get("valueRu") or ""),
    "the pre-review block, its estimate and the terminal state are carried on the rows that own them, "
    f"as the console prints them (observed: {kit.console_field_name(pre_review)!r}, "
    f"{kit.console_field_name(terminal_state)!r}, "
    f"{str(terminal_state.get('valueRu') or '')[:80]!r})",
)
check(
    no_second_confirmation in walkthrough_text
    and all(needle in walkthrough_text for needle in ladder_tail_facts),
    "and the guide prints the sentence and the terminal state, so the next reader stops looking for a "
    "second confirmation (missing: "
    f"{[n for n in (no_second_confirmation,) + ladder_tail_facts if n not in walkthrough_text]})",
)

# The modal the release save raises is the ladder's other trap: the release is already saved when it is
# on screen, and the modal is what hid that from the previous session. It hangs off the release it is
# raised by.
save_modal = subscreen("release", "release-saved-modal")
save_modal_row = row_of(save_modal, "save-modal")
save_modal_strings = (
    'Перейти на страницу "Обзор публикации"?',
    'Изменение сохранено. Чтобы отправить его на проверку, перейдите в "Обзор публикации".',
    "Перейти к обзору",
)
check(
    save_modal.get("id") == "release-saved-modal",
    "the modal the release's save raises is a sub-screen of the release that raises it (got "
    f"{[sub.get('id') for sub in screen_by_id('release').get('subscreens') or []]})",
)
# The model's half is read off the row's own cells rather than searched in `json.dumps(save_modal)`: two
# of the three strings carry quotes of their own, and a dump escapes them - the first run of this leg
# reported a string missing from a model that carries it verbatim. The document's half is a plain search
# in the printed text, where the quotes are the console's own.
save_modal_model_text = kit.console_field_name(save_modal_row) + str(save_modal_row.get("valueRu") or "")
save_modal_missing_model = [needle for needle in save_modal_strings if needle not in save_modal_model_text]
save_modal_missing_doc = [needle for needle in save_modal_strings if needle not in walkthrough_text]
check(
    not save_modal_missing_model and not save_modal_missing_doc,
    "the modal the release save raises is carried and printed with its own strings, so nobody hunts for "
    f"a second save (missing from the model: {save_modal_missing_model}, missing from the printed "
    f"document: {save_modal_missing_doc})",
)

# --- The flow's own sub-screens, in the order the walk met them -----------------------------------
#
# A finding and a screen that document the same place are two answers to one question, and the two
# readings of 2026-09-29 could only be settled one way: the reading that *found* the dialog wins, the
# dialog is documented row by row, and the refusal it used to be is gone. The list below is the flow's
# own map, and it names every step of it rather than a subset.
certifications_dialog = screen_by_id("declarations-dialog")
check(
    [sub["id"] for sub in screen_by_id("publishing").get("subscreens") or []]
    == [
        "app-dashboard-goal",
        "submit-dialog",
        "eu-geo-blocking-notice",
        "post-submit-states",
        "declarations-dialog",
    ],
    "the goal, the dialog that was read, the notice, what follows the press and the declarations dialog "
    "hang off the screen that owns the flow, in the order the walk met them",
)
policy_row = field_by_id("declarations-dialog", "developer-program-policies-row")
export_row = field_by_id("declarations-dialog", "us-export-laws-row")
dialog_buttons = field_by_id("declarations-dialog", "dialog-buttons")
check(
    kit.console_field_name(policy_row) == "Правила программы для разработчиков"
    and kit.console_field_name(export_row) == "Экспортное законодательство США"
    and kit.console_field_answer_source(policy_row) == "owner"
    and kit.console_field_answer_source(export_row) == "owner",
    f"both certifications are rows under the console's own section names and their tick is the owner's own decision (got {kit.console_field_answer_source(policy_row)}, {kit.console_field_answer_source(export_row)})",
)
check(
    kit.console_field_answer_source(dialog_buttons) == "state"
    and "Сохранить" in kit.console_field_name(dialog_buttons),
    "and the dialog's own pair of buttons is carried as a state a human reads rather than as something to type",
)
check(
    "Соблюдение правил программы для разработчиков" in json.dumps(certifications_dialog, ensure_ascii=False),
    "the name the owner gave the first certification survives as a search trail rather than as the row's label",
)
check(
    kit.console_screen_fields(certifications_dialog) != kit.console_screen_fields(screen_by_id("store-listing"))
    and all(
        kit.console_field_answer_source(field) in {"owner", "state"}
        for field in kit.console_screen_fields(certifications_dialog)
    ),
    "every row of the dialog says where its answer comes from: the owner, or the state of the screen",
)
# The two certifications' own wording is the whole reason those transcriptions were made: a guide
# that documents the dialog but not the strings a human has to find on it leaves the next reader
# searching the screen by eye. The leg searches the rendered document, not the model, so it fails
# when the row is in the model and the document the human actually reads does not print it.
certification_strings = (
    "Правила программы для разработчиков",
    "Подтвердить, что приложение соответствует Правилам программы для разработчиков",
    "Экспортное законодательство США",
    "Принять экспортное законодательство США",
)
check(
    all(needle in json.dumps(model, ensure_ascii=False) for needle in certification_strings)
    and all(needle in walkthrough_text for needle in certification_strings),
    "the console's own words for the two first-publication certifications are carried and printed, so the reader can find those "
    f"rows on the screen (missing from the printed document: "
    f"{[needle for needle in certification_strings if needle not in walkthrough_text]})",
)
# The release's country/region list is the one field whose absence makes `Сохранить` refuse on the
# `Посмотреть и подтвердить` step, and it belongs to the *release* rather than to the listing - so a
# model organised around the listing carried no row for it and the next reader hits the same wall.
# The leg requires the row to exist and the console's own error sentence to be carried and printed.
release_countries = field_by_id("release", "releaseCountries")
release_facts = (
    "не выбраны страны и регионы",
    "Выбрать все строки",
    "Создайте и опубликуйте выпуск",
)
check(
    kit.console_field_answer_source(release_countries) == "state"
    and all(needle in json.dumps(screen_by_id("release"), ensure_ascii=False) for needle in release_facts),
    "the release carries the country/region list as a row of its own, beside the console's own error "
    "sentence that its absence raises (got source "
    f"{kit.console_field_answer_source(release_countries)})",
)
check(
    all(needle in walkthrough_text for needle in release_facts),
    "and the printed guide names that row, the console's error and the dashboard goal it unblocks, so "
    "the next reader does not rediscover the wall (missing: "
    f"{[needle for needle in release_facts if needle not in walkthrough_text]})",
)
# --- EU-NOTICE-LEGS-START (the legs between here and END are the EU geo-blocking notice and the
# --- provenance of the two places it appears; the markers let .kiln/eu-notice-inject.py remove exactly
# --- them to observe the pre-change count) ---
# --- The notice was transcribed on one page and only reported on the other -------------------------
#
# Two places carry this notice and only one of them was transcribed. On `Обзор публикации` the
# orchestrator read it live on 2026-09-29 - heading, body, the `Подробнее` link and the `Понятно`
# dismiss button - so its wording is asserted as an observation. On the release's country/region tab the
# owner saw the same notice and passed it on as a summary: nobody transcribed that page, so that claim
# alone carries `NEEDS-VERIFICATION`. Attributing the transcription to both would be the same defect as
# paraphrasing a label - a string the human cannot find on the screen it is credited to.
eu_banner = subscreen("publishing", "eu-geo-blocking-notice")
geoblocking_row = row_of(eu_banner, "geoblocking-banner")
eu_banner_json = json.dumps(eu_banner, ensure_ascii=False)
eu_banner_ru = (
    "Регламент о необоснованной геоблокировке",
    "Если вы распространяете приложения на территории Европейского союза, ознакомьтесь с Регламентом "
    "ЕС 2018/302.",
)
check(
    eu_banner.get("id") == "eu-geo-blocking-notice",
    "the transcribed notice is a sub-screen of the publishing overview, the page it was read on (got "
    f"{[sub.get('id') for sub in screen_by_id('publishing').get('subscreens') or []]})",
)
check(
    kit.console_field_name(geoblocking_row) == eu_banner_ru[0]
    and str(geoblocking_row.get("valueRu") or "").strip() == eu_banner_ru[1]
    and not eu_banner.get("needsVerification"),
    "the transcribed banner hangs off the publishing overview with the console's own heading and body, "
    "on the row that owns them, and carries no NEEDS-VERIFICATION because it is an observation rather "
    f"than a guess (observed: {kit.console_field_name(geoblocking_row)!r}, "
    f"{str(geoblocking_row.get('valueRu') or '')[:60]!r})",
)
check(
    all(needle in walkthrough_text for needle in eu_banner_ru),
    "and the guide prints both strings, so a reader can find the notice they will see on the page "
    f"(missing: {[n for n in eu_banner_ru if n not in walkthrough_text]})",
)
check(
    "Подробнее" in json.dumps(geoblocking_row, ensure_ascii=False)
    and "Понятно" in json.dumps(geoblocking_row, ensure_ascii=False),
    "the notice's own two controls are carried as well, on the row that transcribes it: a link and a "
    "dismiss button, and nothing to tick",
)
eu_notice = field_by_id("country-availability", "eu-geo-blocking-notice")
country_tab = screen_by_id("country-availability")
release_subscreens = [sub.get("id") for sub in screen_by_id("release").get("subscreens") or []]
check(
    "country-availability" in release_subscreens
    and country_tab.get("needsVerification") is True
    and kit.console_field_answer_source(eu_notice) == "formality",
    "the release's own tab keeps the notice as the owner's report, marked NEEDS-VERIFICATION because its "
    f"own string was never transcribed (got {country_tab.get('needsVerification')!r}, source "
    f"{kit.console_field_answer_source(eu_notice)!r})",
)
check(
    "Обзор публикации" in str(country_tab.get("needsVerificationRu") or "")
    and "пересказ" in str(country_tab.get("needsVerificationRu") or "")
    and "пересказ" in json.dumps(country_tab.get("notesRu"), ensure_ascii=False)
    and country_tab.get("nameNotQuoted") is True,
    "and that row says where the transcribed copy is and that its own label is the owner's summary, so "
    "neither claim borrows the other's provenance",
)
# The sheet is the document a slot's operator works from, and it prints the model's English cells, so
# the English wording is asserted separately: a model carrying only the Russian half would leave the
# sheet quoting nothing, and the rule is what keeps the notice from being read as work to do.
eu_notice_en = "please do not use unjustified geo-blocking to discriminate against users"
eu_rule_en = "the notice is then closed by itself"
check(
    eu_notice_en in text and eu_rule_en in text,
    "and the per-slot sheet prints the same notice in English with the rule that a dismissed answer is "
    "complete for a slot that is not distributed in the EU (missing: "
    f"{[needle for needle in (eu_notice_en, eu_rule_en) if needle not in text]})",
)
# --- EU-NOTICE-LEGS-END ---
#
# The Desktop and Android XR rows are on the console's page and are deliberately left empty. A model
# that stayed silent about them left the next reader to decide, per app, whether they were forgotten -
# the owner decided once, on 2026-09-29, and the decision is what the model carries.
xr_decision_rows = ("desktopShots", "androidXrShots", "androidXrSpatialVideo", "androidXrNonSpatialVideo")
check(
    all(
        kit.console_field_answer_source(field_by_id("store-listing", row)) == "owner"
        for row in xr_decision_rows
    ),
    "every Desktop and Android XR row is carried as the owner's decision rather than as an open question "
    f"(got {[kit.console_field_answer_source(field_by_id('store-listing', row)) for row in xr_decision_rows]})",
)
check(
    "Скриншоты для Android XR" in walkthrough_text and "решено 2026-09-29" in walkthrough_text,
    "and the printed guide states that decision in so many words instead of leaving the rows unmentioned",
)


def unreachable_errors_with(probe: dict) -> list:
    saved_form = kit._CONSOLE_FORM
    try:
        kit._CONSOLE_FORM = probe
        return kit.console_unreachable_model_errors()
    finally:
        kit._CONSOLE_FORM = saved_form


# The contradiction the reader refuses: a finding whose id is also a screen's. The model carries no
# finding any more, so the probe supplies one - which makes the leg stronger than it was, because it no
# longer needs live data to be exercised.
probe = model_with_finding(item_id="declarations-dialog")
probe_errors = unreachable_errors_with(probe)
check(
    len(probe_errors) == 1
    and "declarations-dialog" in probe_errors[0]
    and "two answers to one question" in probe_errors[0],
    f"a finding whose id is also a sub-screen's id is refused, because the model would then document a dialog and deny it (got {probe_errors})",
)
probe = model_with_finding(item_id="")
probe_errors = unreachable_errors_with(probe)
check(
    len(probe_errors) == 1 and "carries no id" in probe_errors[0],
    f"a finding with no id names nothing an observation can match (got {probe_errors})",
)
check(
    unreachable_errors_with(copy.deepcopy(model)) == [] and "unreachable" not in model,
    "and this model carries no finding at all: the section is gone rather than left as a heading that "
    "says nothing was searched",
)

# --- The one control that submits, named as the console names it ----------------------------------
#
# The model used to say that sending is the owner's own step and to forbid both guides to invite the
# press. The 2026-09-29 walk falsified that: the press was performed from the console, and AGENTS.md
# stage 10 records the submission as a console action performed by whoever drives the console under the
# owner's observation, with only the graphics upload and the two first-publication declarations left
# human. So the model names the control the console prints and states what enables it, and the
# owner-action marker is gone: a marker forbidding a step the ladder performs is the same defect as a
# label the human cannot find.
HUMAN_ACTION_SENTENCES = (
    "Сама отправка остаётся человеческим действием.",
    "Сам шаг отправки остаётся человеческим.",
    "Кнопку этого экрана нажимает владелец, а не фабрика",
    "Sending stays the owner's action.",
    "The owner presses this screen's button rather than the factory",
)
model_json = json.dumps(model, ensure_ascii=False)
publishing_screen = screen_by_id("publishing")
submit_field = field_by_id("publishing", "submit_button")
check(
    submit_field["nameRu"] == "Отправить N изменений на проверку"
    and publishing_screen.get("actionRu") == "Отправить N изменений на проверку"
    and publishing_screen.get("actionObserved") is True,
    "the one control that submits is named as the console names it, on the screen that owns it (got "
    f"{submit_field['nameRu']!r}, {publishing_screen.get('actionRu')!r})",
)
check(
    not publishing_screen.get("ownerAction")
    and not any(sentence in model_json for sentence in HUMAN_ACTION_SENTENCES),
    "and no screen of the model claims the submission is a human step any more: the sentence that said "
    f"so is gone (found: {[s for s in HUMAN_ACTION_SENTENCES if s in model_json]})",
)
check(
    "когда у цели не осталось незакрытой строки" in json.dumps(submit_field, ensure_ascii=False)
    and "goal has no unfinished row" in json.dumps(submit_field, ensure_ascii=False),
    "the enable condition is stated with the control rather than left to be guessed, in both languages",
)
check(
    "Отправить N изменений на проверку" in text
    and "Отправить 12 изменений" not in model_json
    and "Отправить 12 изменений" not in text,
    "the per-slot sheet prints the control by that name, and no reading's own count is stored as this "
    "slot's value",
)
check(
    "Do NOT press" not in text and "**Действие владельца:**" not in walkthrough_text,
    "neither guide keeps the refusal that forbade the step the ladder performs",
)
# The marker's own machinery is still checked, on a probe: the fact that this model carries no such
# marker is the leg above, and the rule a model must satisfy when it does carry one is this one.
probe = copy.deepcopy(model)
for entry in probe["screens"]:
    if entry["id"] == "publishing":
        entry["ownerAction"] = True
        entry["ownerActionEn"] = "the owner presses this one"
probe_errors = reader_errors_with(probe)
check(
    len(probe_errors) == 1 and "ownerAction" in probe_errors[0] and "ownerActionRu" in probe_errors[0],
    f"a marker that keeps the factory off a step is refused without a statement beside it (got {probe_errors})",
)
dashboard_goal = screen_by_id("app-dashboard-goal")
check(
    [kit.console_field_name(field) for field in kit.console_screen_fields(dashboard_goal)]
    == [
        "Выберите категорию приложения и укажите контактную информацию",
        "Настройте страницу приложения",
    ]
    and all(
        kit.console_field_answer_source(field) == "state"
        for field in kit.console_screen_fields(dashboard_goal)
    ),
    "the dashboard's own two goal rows are carried as states to read, in the console's own words",
)

# --- The counter of the goal is not a field of a screen ------------------------------------------
#
# `submission-flow.json` transcribes the dashboard's counter as a row (`dashboard_counter`, whose label
# is the counter with this app's numbers in it: `Выполнено: 10 из 11`). The model carries that counter
# as a *shape* in `declarationCompletion.counters`, with its own rule that the numbers are read off the
# screen - so the row is neither a value to store (one app's `10 из 11` is not this slot's) nor a field
# the model is missing, and adding it would make the model hold what its own rule refuses to hold. The
# report therefore stops naming it as a row to gain, and the mapping that lets it do so is declared in
# the model and checked here: it has to name a counter the model actually prints.
counter_rows = kit.console_counter_row_sources()
check(
    counter_rows == {"dashboard_counter": "dashboard"},
    f"the row the file writes for the dashboard's counter is declared against the counter that carries it (got {counter_rows})",
)
counter_field = dict(valid_observation["fields"][0])
counter_field.update(
    {"id": "dashboard_counter", "nameRu": "Выполнено: 10 из 11", "kind": "read", "valueRu": "10 из 11"}
)
counter_file = observation(
    screenId="publishing",
    consoleNameRu=screen_by_id("publishing")["consoleNameRu"],
    observedAt="2026-09-30T09:10:00+03:00",
    fields=[counter_field],
)
_failures, implied_counter, _unknown = kit.validate_console_observation(counter_file, "fixture")
check(
    _failures == []
    and not any("must gain the field 'dashboard_counter'" in row for row in implied_counter)
    and any("fields[0].valueRu must not become a model value" in row for row in implied_counter),
    f"a row that names the console's own counter is not a field to add, while its value is still refused a place in the model (got {_failures + implied_counter})",
)
probe = copy.deepcopy(model)
probe["observationFold"]["counterRows"] = {"dashboard_counter": "goal-counter-nobody-prints"}
probe_errors = reader_errors_with(probe)
check(
    len(probe_errors) == 1
    and "counterRows" in probe_errors[0]
    and "goal-counter-nobody-prints" in probe_errors[0]
    and "does not print" in probe_errors[0],
    f"a row refused a place in the model on the strength of a counter nothing prints is a failure (got {probe_errors})",
)
probe = copy.deepcopy(model)
probe["observationFold"].pop("counterRowsRuleRu")
probe_errors = reader_errors_with(probe)
check(
    len(probe_errors) == 1 and "counterRowsRuleRu" in probe_errors[0],
    f"the mapping prints no exemption without the rule that says which counter carries the row (got {probe_errors})",
)
probe = copy.deepcopy(model)
probe["observationFold"]["counterRows"] = {"": "dashboard"}
saved_form = kit._CONSOLE_FORM
try:
    kit._CONSOLE_FORM = probe
    empty_key_errors = kit.console_counter_row_errors()
    empty_key_permissions = kit.console_counter_row_sources()
finally:
    kit._CONSOLE_FORM = saved_form
check(
    len(empty_key_errors) == 1
    and "empty row id" in empty_key_errors[0]
    and empty_key_permissions == {},
    f"an entry with no row id grants no permission and is reported rather than ignored (got {empty_key_errors}, {empty_key_permissions})",
)
check(
    all(
        token not in str(completion[key])
        for key in ("numbersRuleRu", "numbersRuleEn", "citation")
        for token in ("9 обработанных", "5 из 11")
    )
    and "10 обработанных деклараций" in str(completion["numbersRuleRu"]),
    "the rule that `N` is the slot's own number does not print a count no reading produced, in either language",
)

naming_rule = model["screenNaming"]
named_in_rule = set(re.findall(r"`(declaration-[a-z-]+)`", str(naming_rule["ruleEn"])))
check(
    str(naming_rule["ruleRu"]).strip()
    and str(naming_rule["ruleEn"]).strip()
    and naming_rule["titleRu"] in walkthrough_text
    and str(naming_rule["ruleRu"]) in walkthrough_text,
    "the model states how a screen is named and the walkthrough prints it, so a file named after its page has a rule to resolve against",
)
check(
    named_in_rule
    and all(
        any(screen.get("id") == screen_id for screen in kit.console_screens())
        for screen_id in named_in_rule
    ),
    f"every screen the naming rule names by id is one the model carries, so the rule cannot outlive a rename (got {sorted(named_in_rule)})",
)

# --- The console labels a screen was read under, as the 2026-09-29 handoff named them ------------
#
# A transcription names its file and its screen as the browser printed them, which is neither the
# model's id nor even its page label: the store page was read under
# «Панель управления → Настройте приложение; Страницы приложения → Стандартная страница приложения»
# and the credentials dialog under a label the console appends the record's own name to. Before this
# rule the report answered all three of those files with "must add a screen", which is the one answer
# the model must not give: a second screen with the same console label is the same declaration
# counted twice. The two legs below are the same fixture with a recorded label and with a label no
# screen records - so the rule cannot pass by resolving everything it is shown.
store_screen = screen_by_id("store-listing")
recorded_label = (store_screen.get("observedLabelsRu") or [""])[0]
check(
    bool(recorded_label) and recorded_label != store_screen["consoleNameRu"],
    "the model records a console label for the store page that its own page label does not already carry",
)
aliased = observation(
    screenId="store-setup",
    consoleNameRu=recorded_label,
    # A reading the model does not carry: the leg above is about resolving the *label*, and provenance
    # is only named when there is something to write. A file reporting the reading the model already
    # records implies nothing, which is the leg further down.
    observedAt="2026-09-30T09:00:00+03:00",
    fields=[],
)
_failures, implied_alias, _unknown = kit.validate_console_observation(aliased, "fixture")
check(
    _failures == []
    and not any("must add a screen" in row for row in implied_alias)
    and any("screens[store-listing].observedAt" in row for row in implied_alias),
    f"a file named after the page it was read on resolves to the screen that records that label (got {_failures + implied_alias})",
)
applied_alias = fold_applied_rows(aliased)
check(
    applied_alias
    and "screens[store-listing].observedAt" in applied_alias[0]
    and "store-setup" in applied_alias[0],
    f"and the provenance is written on the screen the model carries, naming the file's own id (got {applied_alias})",
)

# --- A reading the model already records implies no edit -----------------------------------------
#
# The handoff names an edit on every file it accepts, and provenance was named unconditionally: six of
# the seven accepted files of 2026-09-29 reported "screens[...].observedAt <- <the reading>" for screens
# that already carried exactly that reading, so the report printed landed edits as work a reader still
# owed. The rule is the same one the wizard's steps and the negative findings already follow - an edit
# the model carries is not an edit - and the two legs below are what keeps it from being an exemption:
# the recorded reading implies nothing, a reading the model does not carry is still named, and the
# comparison is between instants so the two spellings of one moment (`18:16:00+03:00` and `18:16+03:00`)
# are one reading rather than two.
recorded_reading = observation(
    screenId="store-setup",
    consoleNameRu=recorded_label,
    observedAt=store_screen["observedAt"],
    fields=[],
)
_failures, implied_recorded, _unknown = kit.validate_console_observation(recorded_reading, "fixture")
check(
    _failures == []
    and not any("observedAt" in row for row in implied_recorded)
    and fold_applied_rows(recorded_reading) == [],
    f"a reading the model already records implies no edit and nothing to fold (got {implied_recorded})",
)
same_instant = observation(
    screenId="store-setup",
    consoleNameRu=recorded_label,
    observedAt=str(store_screen["observedAt"]).replace(":00+03:00", "+03:00"),
    fields=[],
)
_failures, implied_same_instant, _unknown = kit.validate_console_observation(same_instant, "fixture")
check(
    _failures == [] and not any("observedAt" in row for row in implied_same_instant),
    f"two spellings of one reading are one reading, so the shorter form implies no edit either (got {implied_same_instant})",
)
earlier_reading = observation(
    screenId="store-setup",
    consoleNameRu=recorded_label,
    observedAt="2026-09-28T10:00:00+03:00",
    fields=[],
)
_failures, implied_earlier, _unknown = kit.validate_console_observation(earlier_reading, "fixture")
check(
    _failures == []
    and any("screens[store-listing].observedAt" in row for row in implied_earlier),
    f"a reading the model does not carry is still named, older or not: the model is not silently updated (got {implied_earlier})",
)

unrecorded = observation(
    screenId="store-setup",
    consoleNameRu="Страница, которую никто не читал",
    observedAt="2026-09-29T18:12:00+03:00",
    fields=[],
)
_failures, implied_unrecorded, _unknown = kit.validate_console_observation(unrecorded, "fixture")
check(
    any("must add a screen" in row for row in implied_unrecorded)
    and fold_applied_rows(unrecorded) == [],
    f"a label no screen records is still a screen to add rather than a match (got {implied_unrecorded})",
)

dialog_subscreen = next(
    sub for sub in screen_by_id("declaration-app-access").get("subscreens") or [] if sub.get("id") == "credentials-dialog"
)
prefix = (dialog_subscreen.get("observedLabelPrefixesRu") or [""])[0]
check(
    bool(prefix) and prefix not in (dialog_subscreen.get("observedLabelsRu") or []),
    "the credentials dialog records the label prefix the console appends a record's own name to",
)
dialog_file = observation(
    screenId="app-access-credentials-dialog",
    consoleNameRu=f"{prefix}: Reviewer promo code - full unlock",
    # A reading the model does not carry: the dialog already records 18:16, and a file reporting that
    # same reading implies nothing (the leg above states that rule).
    observedAt="2026-09-30T09:05:00+03:00",
    fields=[],
)
_failures, implied_dialog, _unknown = kit.validate_console_observation(dialog_file, "fixture")
check(
    _failures == []
    and not any("must add a screen" in row for row in implied_dialog)
    and any("screens[credentials-dialog].observedAt" in row for row in implied_dialog),
    f"a dialog read under a label carrying the record's own name resolves by its recorded prefix (got {_failures + implied_dialog})",
)
check(
    "Reviewer promo code - full unlock" not in json.dumps(kit.console_form(), ensure_ascii=False),
    "and the record's own name stays out of the model: only the prefix is recorded",
)

# --- The store page's own rows: the category label and the pack's own graphics files --------------
#
# The category is the pack's token against the console's word, and the console's word is a dictionary
# entry read in the console: a token the dictionary does not hold is a refusal naming it, because
# printing `PRODUCTIVITY` at a human would be a word that is nowhere on the screen. The graphics rows
# are the pack's own inventory, counted from `SCREEN_TARGETS`, so a renderer change cannot leave the
# sheet naming six phone shots while the pack ships eight.
slot_cat, store_cat, listing_cat = make_slot([literal("recordbook_pro")], enabled=["billing"])
store_screen_model = screen_by_id("store-listing")
category_field = kit.console_field(store_screen_model, "category")
listing_cat["category"] = "EDUCATION"
kit.write_documents(store_cat, listing_cat, list(LOCALES), False, META)
text_cat = sheet(store_cat)
check(
    model["listing"]["categoryLabels"]["EDUCATION"]["labelRu"] in text_cat
    and "| Категория приложения (App category) | — | Образование |" in text_cat,
    "the category row prints the console's own word for the pack's token",
)
listing_cat["category"] = "PRODUCTIVITY"
kit.write_documents(store_cat, listing_cat, list(LOCALES), False, META)
text_unknown_cat = sheet(store_cat)
check(
    "TODO-HUMAN" in text_unknown_cat
    and "PRODUCTIVITY" in text_unknown_cat
    and model["listing"]["categoryLabels"]["EDUCATION"]["labelRu"] not in text_unknown_cat.split("Категория приложения")[1],
    "a token the dictionary does not hold is refused by name rather than printed as an answer",
)

graphics_expected = {
    "icon": "store/assets/icon-512.png",
    "featureGraphic": "store/assets/feature-graphic-1024x500.png",
    "phoneShots": "store/assets/en-US/phone-06.png",
    "tablet7Shots": "store/assets/en-US/tablet-7-04.png",
    "tablet10Shots": "store/assets/en-US/tablet-10-04.png",
}
for field_id, expected_path in graphics_expected.items():
    graph_field = kit.console_field(store_screen_model, field_id)
    check(
        graph_field["assetKind"] and graph_field["literal"] == "derived.storeAsset",
        f"the {field_id} row is answered by the pack's own files rather than by a value written here",
    )
    check(
        expected_path in text_cat,
        f"the {field_id} row names the pack's own file for that row ({expected_path})",
    )
phone_paths = [
    path for path in kit.expected_composed_assets(list(LOCALES)) if "/phone-" in path
]
check(
    all(path in text_cat for path in phone_paths) and "phone-07.png" not in text_cat,
    "the phone row names every frame the pack validator expects and no frame it does not",
)
check(
    "store/assets/ru-RU/icon-512.png" in text_cat,
    "the icon row names the file for every declared locale, not only the default one",
)

failed = sum(1 for ok_row, _ in results if not ok_row)
print(f"\n{len(results) - failed}/{len(results)} checks passed")
print(f"VERDICT: {'PASS' if failed == 0 else f'FAIL ({failed})'}")
sys.exit(1 if failed else 0)
