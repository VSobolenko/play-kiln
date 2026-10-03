"""The authored listing: locale copy, defaults, and the pre-flight that refuses a broken one.

The listing is the input a human is allowed to edit, so this module writes it with the same restraint as
the documents: generator wording fills a missing field, and a hand-written value is never overwritten
(``is_idea_derived_output`` / ``merge_missing_locales``).
"""

from __future__ import annotations

import hashlib
import html
import json
import re
import sys
from pathlib import Path
from typing import Any, Callable

# The sibling modules are files beside this one, not an installed package: the store directory goes
# on `sys.path` before they are imported, the line `scripts/store/validate.py` also uses.
sys.path.insert(0, str(Path(__file__).parent))

from render_limits import (
    DEFAULT_CONTACT_EMAIL,
    ROOT,
    SUPPORTED_LOCALES,
    pack_model,
)
from render_support import (
    RenderError,
    heading,
    match_gradle,
    prose_lines,
    read_text,
    truncate_chars,
)




def load_listing(path: Path) -> dict[str, Any]:
    if not path.is_file():
        raise RenderError(f"missing {path}; run with --init first")
    try:
        value = json.loads(read_text(path))
    except json.JSONDecodeError as exc:
        raise RenderError(f"invalid JSON in {path}: line {exc.lineno}: {exc.msg}") from exc
    if not isinstance(value, dict):
        raise RenderError(f"{path} must contain a JSON object")
    return value




def recorded_idea(slot: Path, slug: str, strings_path: Path) -> str:
    state_path = slot / ".kiln-state.json"
    if state_path.is_file():
        try:
            state = json.loads(read_text(state_path))
        except json.JSONDecodeError as exc:
            raise RenderError(f"invalid JSON in {state_path}: line {exc.lineno}: {exc.msg}") from exc
        if isinstance(state, dict) and isinstance(state.get("idea"), str):
            idea = state["idea"].strip()
            if idea:
                return idea

    registry_path = ROOT / "registry" / "apps.json"
    if registry_path.is_file():
        try:
            registry = json.loads(read_text(registry_path))
        except json.JSONDecodeError as exc:
            raise RenderError(f"invalid JSON in {registry_path}: line {exc.lineno}: {exc.msg}") from exc
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
        raise RenderError(f"missing {gradle_path}")
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
        found = re.search(
            r'<string\s+name=["\']app_name["\']>(.*?)</string>', read_text(strings_path), re.S
        )
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




def english_copy(meta: dict[str, str], compliance: dict[str, Any] | None = None) -> dict[str, Any]:
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




def russian_copy(meta: dict[str, str], compliance: dict[str, Any] | None = None) -> dict[str, Any]:
    name = truncate_chars(meta["app_name"], 30)
    bullets = [
        "Спокойный и понятный рабочий процесс",
        "Наглядный прогресс без лишних действий",
        "Полностью автономная и приватная работа",
    ]
    short = truncate_chars(f"{name}: простой способ сосредоточиться и видеть свой прогресс", 80)
    compliance = compliance or {}
    if compliance.get("adsPresent"):
        disclosure = (
            "Приложение содержит рекламу; использование данных раскрыто в разделе безопасности данных."
        )
    elif compliance.get("dataCollected"):
        disclosure = "Использование данных раскрыто в разделе безопасности данных."
    else:
        disclosure = (
            "Подключенные возможности не декларируют рекламу, аналитику или облачное отслеживание."
        )
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




def generator_localized_copy(
    meta: dict[str, str], compliance: dict[str, Any], locale: str
) -> dict[str, Any]:
    return russian_copy(meta, compliance) if locale == "ru-RU" else english_copy(meta, compliance)




def authored_reason(prior: Any, generated: str) -> str:
    """The reason to keep: a hand-written value, or the generator sentence when there is none.

    ``store/listing.json`` is an authored pack input the owner's release approval is bound to
    (``scripts/lib/pack-approval.mjs``), so a render must never replace a reason a human wrote --
    doing so both destroys the text and, because the file is digest-covered, voids the approval
    itself.  A reason is generated only when the existing value is absent, not a string, or blank;
    this is the same restraint ``merge_missing_locales`` and ``write_preserving`` apply to the
    documents.  The generated sentence is passed in so each call site keeps its own wording and no
    rule is restated here.
    """
    if isinstance(prior, str) and prior.strip():
        return prior
    return generated


def apply_capability_compliance(listing: dict[str, Any], compliance: dict[str, Any]) -> None:
    data_types = compliance.get("dataCollected", [])
    sdks = compliance.get("sdkDependencies", [])
    # Read the reasons before either dict is replaced: the assignment below rebuilds `ads` and
    # `dataSafety` wholesale, so a prior value reaches this function only through these two reads.
    prior_ads = listing.get("ads") if isinstance(listing.get("ads"), dict) else {}
    prior_data_safety = (
        listing.get("dataSafety") if isinstance(listing.get("dataSafety"), dict) else {}
    )
    listing["permissions"] = compliance.get("permissions", [])
    listing["ads"] = {
        "containsAds": bool(compliance.get("adsPresent")),
        "reason": authored_reason(
            prior_ads.get("reason"),
            "Enabled capabilities include advertising SDKs."
            if compliance.get("adsPresent")
            else "Enabled capabilities declare no advertising SDKs.",
        ),
    }
    listing["dataSafety"] = {
        "collectsData": bool(data_types),
        "reason": authored_reason(
            prior_data_safety.get("reason"),
            "Derived from the enabled capabilities and their bundled SDK declarations."
            if data_types
            else "Enabled capabilities declare no collected data.",
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




def default_listing(
    meta: dict[str, str], locales: list[str], compliance: dict[str, Any]
) -> dict[str, Any]:
    locale_data = {
        locale: generator_localized_copy(meta, compliance, locale) for locale in locales
    }
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
        # A draft seed, and deliberately a reserved-host placeholder: a policy URL cannot be invented
        # and the preflight below requires the field to be a string. It is no longer a question put
        # to the owner, because the factory hosts the policy itself -- `node scripts/publish-privacy.mjs
        # --slot <slug>` publishes `store/privacy.md` to a public HTTPS URL (Telegraph; no account, no
        # domain, no deployment) and writes the real value here, updating the same URL on every later
        # edit. A pack that still carries this seed cannot ship: the Node `pack.placeholders` check
        # (`scripts/core/assets.mjs`) and this store's own read path (`scripts/store/validate.py`)
        # both FAIL a `privacyPolicyUrl` on a reserved host, so the seed is visibly unfinished rather
        # than silently accepted.
        "privacyPolicyUrl": "https://example.com/privacy",
        "locales": locale_data,
        # The two `reason`s are deliberately absent from these seeds: `apply_capability_compliance`
        # rebuilds both dicts below, and now that it preserves an authored value a seeded sentence
        # would be kept as though a human had written it.  A fresh draft has no author, so the
        # generator sentence is the only value it may carry.  The remaining keys are placeholders
        # the same call replaces.
        "dataSafety": {
            "collectsData": False,
            "dataTypes": [],
            "thirdPartySdks": [],
            "encryptedInTransit": False,
            "hasAccountDeletion": False,
        },
        "permissions": [],
        "ads": {"containsAds": False},
        # The audience is left empty on purpose, and this is the one seeded field that is deliberately
        # not a value: the factory can know the field's shape at init time, not the app's audience, so
        # the checklist prints `TODO-HUMAN` naming the field until the slot's own `listing.json`
        # names a branch. An unmade decision is work rather than a defect in a pack under
        # construction.
        "contentRating": {"questionnaire": "notes", "targetAudience": ""},
        "graphicsSource": {"iconGlyph": "focus", "brandColor": brand, "accentColor": accent},
    }
    apply_capability_compliance(listing, compliance)
    return listing




def merge_missing_locales(
    listing: dict[str, Any],
    meta: dict[str, str],
    locales: list[str],
    compliance: dict[str, Any],
) -> None:
    locale_map = listing.setdefault("locales", {})
    for locale in locales:
        if locale not in locale_map:
            locale_map[locale] = generator_localized_copy(meta, compliance, locale)
    listing["defaultLocale"] = (
        listing.get("defaultLocale") if listing.get("defaultLocale") in locales else locales[0]
    )




IDEA_DERIVED_FIELDS = (
    "shortDescription",
    "fullDescription",
    "featureBullets",
    "screenshotCaptions",
)




def is_idea_derived_output(value: Any, prior_value: Any, previous_idea: str) -> bool:
    """Provenance test for one locale copy field against the listing's PREVIOUS idea.

    A field counts as generator output when either:

    * it is byte-identical to what the current generator would have produced for the previous idea
      (``prior_value``), or
    * it is an ellipsis-truncated *prefix* of the previous idea.  This is how the short description
      renders the idea, and it covers both the current word-boundary ``truncate_chars`` and the
      older mid-word truncation, so packs written by an earlier revision of the generator are still
      recognised as generator output rather than human copy.

    Anything else is treated as human-authored and preserved.
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

    The slot record (``.kiln-state.json`` / ``registry/apps.json`` / ``res/values/strings.xml``
    ``app_idea``) is the source of truth for the idea.  Each derived field is rewritten only when
    ``is_idea_derived_output`` proves it is still generator output for the listing's PREVIOUS
    ``idea``; a human edit fails that test, is preserved, and is reported.  Failing safe means
    preserving, never clobbering.
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




def listing_preflight_problems(listing: dict[str, Any], locales: list[str]) -> list[str]:
    """Why the listing about to be rendered is not the shape the documents are written from.

    This is a precondition on the renderer's **input**, not a verdict on its output: the pack's
    verdict is ``scripts/store/validate.py``'s job, and this module never imports it.  Refusing
    before a byte is written is what keeps a malformed input from producing a pack that describes
    nothing.
    """
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
    for locale in locales:
        entry = locale_map.get(locale)
        if not isinstance(entry, dict):
            errors.append(f"listing.locales.{locale} is missing")
            continue
        for field in ("title", "shortDescription", "fullDescription"):
            limit = int(pack_model.LISTING_LIMITS[field])
            value = entry.get(field)
            if not isinstance(value, str) or not value.strip():
                errors.append(f"listing.locales.{locale}.{field} must be a non-empty string")
            elif len(value) > limit:
                errors.append(
                    f"listing.locales.{locale}.{field} is {len(value)} chars; Play limit is {limit}"
                )
        captions = entry.get("screenshotCaptions")
        if (
            not isinstance(captions, list)
            or len(captions) != 6
            or not all(isinstance(x, str) and x for x in captions)
        ):
            errors.append(f"listing.locales.{locale}.screenshotCaptions must contain 6 strings")
        bullets = entry.get("featureBullets")
        if (
            not isinstance(bullets, list)
            or len(bullets) < 3
            or not all(isinstance(x, str) and x for x in bullets)
        ):
            errors.append(f"listing.locales.{locale}.featureBullets must contain at least 3 strings")
    graphics = listing.get("graphicsSource", {})
    for field in ("brandColor", "accentColor"):
        if not re.fullmatch(r"#[0-9A-Fa-f]{6}", str(graphics.get(field, ""))):
            errors.append(f"listing.graphicsSource.{field} must be #RRGGBB")
    return errors
