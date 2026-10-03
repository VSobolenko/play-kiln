#!/usr/bin/env python3
"""Regression suite for the billing console steps in the generated `store/checklist.md`.

The defect this exists against: `write_documents` generated a checklist that named screenshots,
declarations, signing and the first AAB upload, and never named the three things that actually
block a first launch of a billing app - the one-time product, its purchase option, and the tester's
license-testing entry. The owner hit it for real: an uploaded `.aab`, nothing rolled out, an opt-in
link that answered "not found", and a purchase test with no product created. Every one of those is
Play *console* state, so no pack document described it and no gate could see it.

The legs below hold two things at once, and the second matters as much as the first:

- **A billing slot names the real ID.** The checklist must name the exact `ProductId("...")` string
  the slot's Kotlin declares, so an ID cannot be invented at upload time. The extractor is exercised
  on a real Kotlin file, both quote styles, a de-duplicated multi-product slot, a non-literal
  reference, the constant form (`const val ..._PRODUCT_ID = "..."`) with and without a `: String`
  annotation, a string constant *not* named like a product id, and a generated copy under `build/`
  that must NOT parse.
- **A non-billing slot names nothing.** A generator that sprayed these steps at every slot would
  teach the human to skim the list, and would tell a slot about products it does not sell.

This is a separate file rather than another section of `test-release-notes.py`: the two suites own
different documents (`checklist.md` vs `release-notes.md`) with independent defect histories, and a
failure should name the file that owns the rule instead of blurring a shared verdict line.

An earlier change in this repository let a suite print FAIL rows while exiting 0, and a verifier
caught it. This suite exits non-zero the moment a leg fails, and the verifier reads that
exit code. `validate_store` prints its own PASS/FAIL rows, so its output is captured instead of
leaked - the rows this process writes are only its own verdicts.

Usage: python scripts/test-checklist-products.py
"""

import contextlib
import importlib.util
import io
import sys
import tempfile
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from lib.scratch import SCRATCH_DIR

# Keep the import of `scripts/store/render.py` from writing a `scripts/__pycache__` entry.
sys.dont_write_bytecode = True

ROOT = Path(__file__).resolve().parents[1]

spec = importlib.util.spec_from_file_location("store_render", ROOT / "scripts" / "store" / "render.py")
assert spec and spec.loader
kit = importlib.util.module_from_spec(spec)
# Registered before execution: the renderer declares dataclasses, and Python 3.13 cannot create one
# whose module is absent from `sys.modules` (`dataclasses._is_type` looks up `cls.__module__`).
sys.modules.setdefault("store_render", kit)
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
BILLING = {
    "enabled": ["billing"],
    "requiresConsent": False,
    "requiresPurchaseAuthority": False,
    "purchaseAuthority": None,
}
NON_BILLING = {
    "enabled": [],
    "requiresConsent": False,
    "requiresPurchaseAuthority": False,
    "purchaseAuthority": None,
}
SOURCE_RELATIVE = Path("app/src/main/java/com/gexetr/testapp/monetization/MonetizationConfig.kt")


def literal(product_id: str, quote: str = '"') -> str:
    """A Kotlin declaration line, as a slot would write it."""
    return f"val PRODUCT = ProductId({quote}{product_id}{quote})"


def constant_literal(product_id: str, *, annotated: bool = True) -> str:
    """The other shape a slot uses: the id named once as a constant a later reader can grep.

    The name carries `PRODUCT_ID` on purpose - that name, not the string value, is what tells this
    reader the constant is a product id.  `annotated` flips the optional `: String` annotation.
    """
    annotation = ": String " if annotated else " "
    return f'const val GAME_PRODUCT_ID{annotation}= "{product_id}"'


def make_slot(declarations: list[str], *, billing: bool, extra_sources: dict | None = None):
    """A temp slot whose Kotlin declares `declarations`, with a generated store pack.

    `declarations` are Kotlin property lines, so a leg can write a literal `ProductId("x")` or a
    reference that must not parse. Returns ``(slot, store, listing)``. The Kotlin file always
    exists, so "no declaration" is a real absence rather than a missing app directory.
    """
    slot = Path(tempfile.mkdtemp(prefix="checklist-products-", dir=SCRATCH_DIR))
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
    for relative, content in (extra_sources or {}).items():
        other = slot / relative
        other.parent.mkdir(parents=True, exist_ok=True)
        if isinstance(content, bytes):
            other.write_bytes(content)
        else:
            other.write_text(content, encoding="utf-8")
    store = slot / "store"
    store.mkdir(parents=True)
    compliance = BILLING if billing else NON_BILLING
    listing = kit.default_listing(META, ["en-US"], compliance)
    kit.apply_capability_compliance(listing, compliance)
    kit.write_documents(store, listing, ["en-US"], False, META)
    return slot, store, listing


def checklist_text(store: Path) -> str:
    return (store / "checklist.md").read_text(encoding="utf-8")


# --- A recorded loss: the pack validator's `billing` row has no successor -------------------------
#
# Six legs used to live here, and they were not about the checklist: they asserted the **pack
# validator's** `PASS billing` / `FAIL billing` rows (`validate_store(billing enabled but no
# declared product ID)`), and that the failure sentence was built from `PRODUCT_ID_DECLARATION`
# rather than restated by hand. That rule existed in exactly one place, `scripts/store-kit.py`,
# which `reduce-factory-to-core` §5.3 deletes, and the replacement validator
# (`scripts/store/validate.py`) reads the **pack** — assets, listing text, provenance — and prints
# no billing row at all. The legs are therefore removed rather than re-pointed, because a leg that
# asserts a row no program prints is a leg that cannot fail.
#
# What survives and is still checked below: every checklist leg, including the two that name the
# real product IDs a billing slot's Kotlin declares (`render.declared_product_ids`) and the legs
# that prove a non-billing slot is handed no selling steps. `scripts/store/render.py` also carries
# the invariant the FAIL row encoded — a billing-enabled slot with no declared ID produces a
# checklist whose product list is empty, which is visible in the generated text.
#
# The missing rule needs a home with the review it deserves: as a further row in
# `scripts/store/validate.py` (which moves that program's measured verdict, 73 PASS / 8 FAIL on
# `teacher-gradebook`, so the pin has to be re-measured with it) or as a check in `scripts/core/`,
# which is where the per-run gate lives. Until then the rule is unenforced, and this note is the
# record of it rather than a silent deletion.


# --- The extractor reads real Kotlin, not a guess ------------------------------------------------


slot, store, _ = make_slot([literal("recordbook_pro")], billing=True)
found = kit.declared_product_ids(slot)
check(
    found == ["recordbook_pro"],
    f"the extractor reads a ProductId declared in a real Kotlin file (got {found})",
)

slot, store, _ = make_slot([literal("coins_100", "'")], billing=True)
found = kit.declared_product_ids(slot)
check(found == ["coins_100"], f"a single-quoted declaration parses too (got {found})")

slot, store, _ = make_slot(
    [literal("zeta_unlock"), literal("alpha_pack"), literal("zeta_unlock")],
    billing=True,
)
found = kit.declared_product_ids(slot)
check(
    found == ["alpha_pack", "zeta_unlock"],
    f"ids are sorted and de-duplicated across declaration sites (got {found})",
)

slot, store, _ = make_slot(['val NOTE = "no product here"'], billing=True)
check(
    kit.declared_product_ids(slot) == [],
    "a slot whose Kotlin declares no ProductId( returns nothing",
)

slot, store, _ = make_slot(["val PRODUCT = ProductId(configuredId)"], billing=True)
check(
    kit.declared_product_ids(slot) == [],
    "a non-literal ProductId(...) reference is not parsed as a product ID",
)

# The other shape: the id named once as a constant. `laneset` declares its product this way, so the
# scanner has to read it or a billing slot is reported as selling nothing. Both typed and inferred
# declarations must be found.
slot, store, _ = make_slot([constant_literal("laneset_unlock")], billing=True)
found = kit.declared_product_ids(slot)
check(
    found == ["laneset_unlock"],
    f"a const val PRODUCT_ID with a String annotation is read as a product ID (got {found})",
)

slot, store, _ = make_slot([constant_literal("coins_pack", annotated=False)], billing=True)
found = kit.declared_product_ids(slot)
check(
    found == ["coins_pack"],
    f"a const val PRODUCT_ID without a type annotation is read too (got {found})",
)

# ... and the name is the whole predicate, so a constant that is not named like a product id must
# not fire - even when its value looks like one.
slot, store, _ = make_slot(['val GREETING = "hello"'], billing=True)
found = kit.declared_product_ids(slot)
check(
    found == [],
    f"a string constant not named like a product id is not a product (got {found})",
)

slot, store, _ = make_slot(['val UNLOCK = "laneset_unlock"'], billing=True)
found = kit.declared_product_ids(slot)
check(
    found == [],
    f"a product-looking value under a non-product name is not a product (got {found})",
)

# A generated copy is not source: reading `app/build/` would let stale code add a console product.
slot, store, _ = make_slot(
    [literal("real_unlock")],
    billing=True,
    extra_sources={
        "app/build/generated/MonetizationConfig.kt": 'val X = ProductId("generated_only")\n'
    },
)
found = kit.declared_product_ids(slot)
check(
    found == ["real_unlock"],
    f"a declaration under app/build is not read as a product ID (got {found})",
)

# A "Kotlin source" containing NUL bytes is binary and must never be parsed as text.
slot, store, _ = make_slot(
    [literal("real_unlock")],
    billing=True,
    extra_sources={
        "app/src/main/java/com/gexetr/testapp/blob.kt": b'\x00val X = ProductId("binary_only")\n'
    },
)
found = kit.declared_product_ids(slot)
check(
    found == ["real_unlock"],
    f"a binary .kt file is skipped rather than parsed (got {found})",
)

# Only the shipped source set counts. A fixture in a test source set is not a product the shipped app
# hands to the Play Billing Library, and listing it would tell the human to create a product in Play
# Console that does not exist - a false instruction on the money path.
slot, store, _ = make_slot(
    [literal("real_unlock")],
    billing=True,
    extra_sources={
        "app/src/test/java/com/gexetr/testapp/Fixture.kt": 'val F = ProductId("test_only")\n',
        "app/src/androidTest/java/com/gexetr/testapp/Probe.kt": 'val P = ProductId("instrumented_only")\n',
    },
)
found = kit.declared_product_ids(slot)
check(
    found == ["real_unlock"],
    f"a declaration in a test source set is not read as a product ID (got {found})",
)

# A product declared nowhere in the shipped source set is a defect, so the restriction above must
# fail loudly rather than return a test's fixture as a substitute.
slot, store, _ = make_slot(
    [],
    billing=True,
    extra_sources={
        "app/src/test/java/com/gexetr/testapp/Fixture.kt": 'val F = ProductId("test_only")\n',
    },
)
found = kit.declared_product_ids(slot)
check(found == [], f"a test-only declaration leaves the shipped set empty (got {found})")

# --- A billing slot's checklist names the exact ID -----------------------------------------------


slot, store, _ = make_slot([literal("harbour_unlock")], billing=True)
text = checklist_text(store)
check(
    "`harbour_unlock`" in text,
    "a billing slot's checklist names the exact product ID found in its Kotlin",
)
check(
    "Monetize with Play \u2192 Products \u2192 In-app products" in text,
    "a billing slot's checklist names where in the console the product is created",
)
check(
    "Settings \u2192 License testing" in text and "real money" in text,
    "a billing slot's checklist requires a license tester and says a track-only tester is charged",
)
check(
    "isn't returned by the Play Billing Library" in text,
    "a billing slot's checklist requires activating the purchase option, in Play's own words",
)

slot, store, _ = make_slot([literal("alpha_pack"), literal("zeta_unlock")], billing=True)
text = checklist_text(store)
check(
    "`alpha_pack`" in text and "`zeta_unlock`" in text,
    "a multi-product slot's checklist names every declared ID",
)

# The constant shape reaches the checklist too, and both shapes de-duplicate into one set.
slot, store, _ = make_slot([constant_literal("laneset_unlock")], billing=True)
text = checklist_text(store)
check(
    "`laneset_unlock`" in text,
    "a constant-form product ID is named in the checklist",
)

slot, store, _ = make_slot(
    [constant_literal("harbour_unlock"), literal("harbour_unlock")],
    billing=True,
)
found = kit.declared_product_ids(slot)
check(
    found == ["harbour_unlock"],
    f"a constant and a ProductId naming one id are de-duplicated (got {found})",
)

# The generator is deterministic: the same slot regenerated produces the same checklist.
slot, store, listing = make_slot([literal("harbour_unlock")], billing=True)
before = checklist_text(store)
kit.write_documents(store, listing, ["en-US"], False, META)
check(
    checklist_text(store) == before,
    "regenerating a billing pack produces the same checklist (it is written, not accumulated)",
)

# --- A non-billing slot is told nothing about products -------------------------------------------


# The Kotlin deliberately declares an ID: the gate is the capability state, not the presence of a
# declaration, so a slot that sells nothing must not be handed selling steps.
slot, store, listing = make_slot([literal("harbour_unlock")], billing=False)
text = checklist_text(store)
check(
    "harbour_unlock" not in text
    and "In-app products" not in text
    and "License testing" not in text,
    "a non-billing slot's checklist carries no product or license-tester step, even when its source declares one",
)
# The pack validator's billing rows are recorded as a loss above; these three legs asserted them.
# Their subject — a non-billing slot is handed no products it does not sell — is still covered by
# the checklist legs that follow.

failed = sum(1 for ok, _ in results if not ok)
print(f"\n{len(results) - failed}/{len(results)} checks passed")
print(f"VERDICT: {'PASS' if failed == 0 else f'FAIL ({failed})'}")
sys.exit(1 if failed else 0)
