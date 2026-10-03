#!/usr/bin/env python3
"""Regression suite for the two authored reasons in `store/listing.json` (`render_listing.py`).

`store/listing.json` is not generator output: it is one of the authored pack inputs the owner's
release approval is bound to (`scripts/lib/pack-approval.mjs`, read by
`scripts/lib/release-approval.mjs`).  `apply_capability_compliance` used to overwrite
`ads.reason` and `dataSafety.reason` unconditionally, so any render destroyed a hand-written
reason -- and because the file is digest-covered, the renderer voided the owner's own recorded
approval for the slot.  That happened for real on 2026-10-02 in `apps/2026-10-01-lawn-round-log`.

The rule this suite pins down: a reason is generated only when the existing value is absent, not a
string, or blank.  The two strings below are the values that were lost, kept verbatim so the exact
defect cannot recur unnoticed.

The generator sentences are quoted here on purpose: the suite must be able to fail when the
renderer's wording changes, which a value imported from the module under test could not do.

    python scripts/test-listing-reasons.py     # exits 0 when every expectation holds
"""

from __future__ import annotations

import importlib.util
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
RENDER_LISTING = HERE / "store" / "render_listing.py"

# The two authored reasons that were overwritten in `apps/2026-10-01-lawn-round-log`.
LAWN_ADS_REASON = "The app contains no advertising SDK, and the plan enables no ads capability."
LAWN_DATA_SAFETY_REASON = (
    "The billing capability bundled for the unlock declares Financial info (purchase history) "
    "as collected by Google Play Billing. Nothing the operator types leaves the device."
)

# The generator sentences, as the renderer writes them.  These are the fallback, never the value
# that replaces an authored one.
ADS_GENERATED = "Enabled capabilities include advertising SDKs."
ADS_GENERATED_NONE = "Enabled capabilities declare no advertising SDKs."
DATA_SAFETY_GENERATED = "Derived from the enabled capabilities and their bundled SDK declarations."
DATA_SAFETY_GENERATED_NONE = "Enabled capabilities declare no collected data."

# Compliance readings, shaped like the dict `render_documents.capability_compliance` returns.  The
# three differ in the branch each would take, so preservation cannot pass by agreeing with one of
# them by accident.
NO_ADS_NO_DATA = {
    "enabled": [],
    "permissions": [],
    "dataCollected": [],
    "sdkDependencies": [],
    "adsPresent": False,
    "requiresConsent": False,
    "requiresPurchaseAuthority": False,
    "purchaseAuthority": None,
}
ADS_PRESENT = {
    "enabled": ["ads"],
    "permissions": ["com.google.android.gms.permission.AD_ID"],
    "dataCollected": [],
    "sdkDependencies": ["com.google.android.gms:play-services-ads"],
    "adsPresent": True,
    "requiresConsent": True,
    "requiresPurchaseAuthority": False,
    "purchaseAuthority": None,
}
BILLING_DATA_COLLECTED = {
    "enabled": ["billing"],
    "permissions": ["com.android.vending.BILLING"],
    "dataCollected": ["Financial info"],
    "sdkDependencies": ["com.android.billingclient:billing"],
    "adsPresent": False,
    "requiresConsent": False,
    "requiresPurchaseAuthority": True,
    "purchaseAuthority": "https://playkiln.example/authority",
}

failures: list[str] = []


def check(ok: bool, message: str) -> None:
    print(f"{'PASS' if ok else 'FAIL'}     {message}")
    if not ok:
        failures.append(message)


def load_module():
    spec = importlib.util.spec_from_file_location("store_render_listing", RENDER_LISTING)
    if spec is None or spec.loader is None:
        raise SystemExit(f"BLOCKED: cannot load {RENDER_LISTING}")
    module = importlib.util.module_from_spec(spec)
    # Registered before execution so the sibling `render_limits`/`render_support` imports resolve
    # against this file's directory (the module does the `sys.path` surgery itself).
    sys.modules.setdefault("store_render_listing", module)
    spec.loader.exec_module(module)
    return module


def main() -> int:
    kit = load_module()

    # --- An authored reason survives, whichever generator branch would have applied -------------
    listing = {"ads": {"containsAds": False, "reason": LAWN_ADS_REASON}}
    kit.apply_capability_compliance(listing, ADS_PRESENT)
    check(
        listing["ads"]["reason"] == LAWN_ADS_REASON,
        "an authored ads.reason survives a render that would otherwise declare ads present",
    )
    check(
        listing["ads"]["containsAds"] is True,
        "preserving the reason does not preserve a stale containsAds",
    )

    listing = {"dataSafety": {"reason": LAWN_DATA_SAFETY_REASON}}
    kit.apply_capability_compliance(listing, BILLING_DATA_COLLECTED)
    check(
        listing["dataSafety"]["reason"] == LAWN_DATA_SAFETY_REASON,
        "an authored dataSafety.reason survives a render that would otherwise derive one",
    )

    # --- A missing reason gets the generator sentence ------------------------------------------
    listing = {"ads": {"containsAds": False}}
    kit.apply_capability_compliance(listing, NO_ADS_NO_DATA)
    check(
        listing["ads"]["reason"] == ADS_GENERATED_NONE,
        "a missing ads.reason gets the no-ads generator sentence",
    )

    listing = {}
    kit.apply_capability_compliance(listing, ADS_PRESENT)
    check(
        listing["ads"]["reason"] == ADS_GENERATED,
        "a missing ads.reason gets the ads-present generator sentence",
    )

    listing = {"dataSafety": {"collectsData": False}}
    kit.apply_capability_compliance(listing, NO_ADS_NO_DATA)
    check(
        listing["dataSafety"]["reason"] == DATA_SAFETY_GENERATED_NONE,
        "a missing dataSafety.reason gets the no-collected-data generator sentence",
    )

    listing = {}
    kit.apply_capability_compliance(listing, BILLING_DATA_COLLECTED)
    check(
        listing["dataSafety"]["reason"] == DATA_SAFETY_GENERATED,
        "a missing dataSafety.reason gets the derived generator sentence",
    )

    # --- A blank reason is a missing reason, and so is a non-string -----------------------------
    for blank in ("", "   ", "\t\n"):
        listing = {"ads": {"reason": blank}, "dataSafety": {"reason": blank}}
        kit.apply_capability_compliance(listing, NO_ADS_NO_DATA)
        check(
            listing["ads"]["reason"] == ADS_GENERATED_NONE
            and listing["dataSafety"]["reason"] == DATA_SAFETY_GENERATED_NONE,
            f"a whitespace-only reason ({blank!r}) is treated as missing",
        )

    listing = {"ads": {"reason": 7}, "dataSafety": {"reason": {"text": "written"}}}
    kit.apply_capability_compliance(listing, NO_ADS_NO_DATA)
    check(
        listing["ads"]["reason"] == ADS_GENERATED_NONE
        and listing["dataSafety"]["reason"] == DATA_SAFETY_GENERATED_NONE,
        "a non-string reason is treated as missing rather than copied through",
    )

    # --- Everything else the call writes is unchanged ------------------------------------------
    listing = {
        "ads": {"reason": LAWN_ADS_REASON},
        "dataSafety": {"reason": LAWN_DATA_SAFETY_REASON},
        "capabilityCompliance": {"stale": True},
    }
    kit.apply_capability_compliance(listing, BILLING_DATA_COLLECTED)
    check(listing["permissions"] == ["com.android.vending.BILLING"], "permissions is written as before")
    check(
        listing["ads"] == {"containsAds": False, "reason": LAWN_ADS_REASON},
        "ads carries only containsAds and the preserved reason",
    )
    check(
        listing["dataSafety"]
        == {
            "collectsData": True,
            "reason": LAWN_DATA_SAFETY_REASON,
            "dataTypes": ["Financial info"],
            "thirdPartySdks": ["com.android.billingclient:billing"],
            "encryptedInTransit": True,
            "hasAccountDeletion": False,
        },
        "dataSafety is written exactly as before apart from the preserved reason",
    )
    check(
        listing["capabilityCompliance"]
        == {
            "enabled": ["billing"],
            "requiresConsent": False,
            "requiresPurchaseAuthority": True,
            "purchaseAuthority": "https://playkiln.example/authority",
        },
        "capabilityCompliance is written exactly as before",
    )

    # `encryptedInTransit` stays false with nothing to transmit, and ads presence still tracks the
    # reading -- the two keys a preservation bug could plausibly swallow.
    listing = {"ads": {"reason": LAWN_ADS_REASON}, "dataSafety": {"reason": LAWN_DATA_SAFETY_REASON}}
    kit.apply_capability_compliance(listing, NO_ADS_NO_DATA)
    check(
        listing["dataSafety"]["encryptedInTransit"] is False
        and listing["dataSafety"]["collectsData"] is False
        and listing["ads"]["containsAds"] is False,
        "with nothing collected, encryptedInTransit/collectsData/containsAds stay false",
    )

    # --- The two real strings, together, in one listing ----------------------------------------
    listing = {
        "ads": {"containsAds": False, "reason": LAWN_ADS_REASON},
        "dataSafety": {"reason": LAWN_DATA_SAFETY_REASON},
    }
    kit.apply_capability_compliance(listing, BILLING_DATA_COLLECTED)
    check(
        listing["ads"]["reason"] == LAWN_ADS_REASON
        and listing["dataSafety"]["reason"] == LAWN_DATA_SAFETY_REASON,
        "both authored reasons survive together in one listing",
    )

    # A second call must be idempotent: the render may apply compliance more than once.
    before = (listing["ads"]["reason"], listing["dataSafety"]["reason"])
    kit.apply_capability_compliance(listing, BILLING_DATA_COLLECTED)
    check(
        (listing["ads"]["reason"], listing["dataSafety"]["reason"]) == before,
        "a second apply preserves the reasons it just kept",
    )

    # --- `default_listing` still yields generator sentences, never its own seeds ----------------
    # The scaffold seeds these two dicts, and before the preservation rule its seeds were dead
    # because the call below overwrote them.  They must stay dead: a fresh `--init` draft has no
    # author yet, so a seeded reason would now be preserved as if a human had written it.
    meta = {
        "app_name": "Reason Test",
        "application_id": "com.gexetr.reasontest",
        "namespace": "com.gexetr.reasontest",
        "version_name": "1.0",
        "version_code": "1",
        "idea": "A focused offline tool.",
        "recordedIdea": "",
        "slug": "reasontest",
    }
    generated = kit.default_listing(meta, ["en-US"], NO_ADS_NO_DATA)
    check(
        generated["ads"]["reason"] == ADS_GENERATED_NONE
        and generated["dataSafety"]["reason"] == DATA_SAFETY_GENERATED_NONE,
        "a freshly generated listing carries generator sentences, not scaffold seeds",
    )

    print(f"\nVERDICT: {'FAIL' if failures else 'PASS'}")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
