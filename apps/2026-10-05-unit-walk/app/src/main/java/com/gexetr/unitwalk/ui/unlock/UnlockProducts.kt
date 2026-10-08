package com.gexetr.unitwalk.ui.unlock

import com.gexetr.capability.billing.BillingCatalog
import com.gexetr.capability.billing.Entitlements
import com.gexetr.capability.billing.ProductId

/**
 * The one product this slot sells, declared once and imported everywhere else.
 *
 * `economics.json` describes a **single non-consumable unlock** at $9.99 with no renewal, so the
 * catalog below holds exactly one `inAppProducts` entry: no consumable pack, no subscription, and
 * therefore no `PurchaseAuthority` — a non-consumable needs no server confirmation, and
 * `store/review-notes.md` records that no server is required.
 *
 * The product ID literal lives here and nowhere else. A second copy of `"unitwalk_unlock"` in a
 * screen, a test or a pack file is the drift this file exists to prevent: if the ID changed, only
 * this line would, and a restated string elsewhere would silently address a product Play does not
 * sell.
 */
val UNIT_WALK_UNLOCK: ProductId = ProductId("unitwalk_unlock")

/** The catalog the controller connects with: one non-consumable, and nothing else. */
val UNIT_WALK_CATALOG: BillingCatalog = BillingCatalog(inAppProducts = listOf(UNIT_WALK_UNLOCK))

/** The one question the unlock surface asks of an [Entitlements] value. */
fun Entitlements.ownsUnitWalkUnlock(): Boolean = owns(UNIT_WALK_UNLOCK.key())

/**
 * The unlock as a gating feature needs it: the entitlement the capability reports, plus the state
 * *before* it has reported anything at all.
 *
 * [Unknown] is a state of its own rather than a collapsed `false`. While the entitlement is
 * unresolved the paid perk must not be handed out — a free-tier PDF carries its watermark — and the
 * user's export must not be blocked either, because export is never gated. Modelling the two apart
 * keeps that boundary visible at the one place it is asked.
 */
enum class UnlockState {
    /** Billing has not resolved yet; treat free and paid alike as "not owned, watermark it". */
    Unknown,

    /** The one-time unlock is owned: unlimited units, and PDF reports with no watermark. */
    Owned,

    /** The unlock is not owned: one unit, and PDF reports carry the watermark. Export still works. */
    NotOwned,
}

/**
 * The one question, answered once for the whole app: the capability's own [Entitlements] mapped to
 * the [UnlockState] the units and report features gate on.
 *
 * The units and report features import this rather than re-deriving it, so the product key lives in
 * one file and a change to the boundary cannot leave one feature behind.
 */
fun Entitlements.unlockState(): UnlockState =
    if (ownsUnitWalkUnlock()) UnlockState.Owned else UnlockState.NotOwned

/**
 * Whether this state hands out the paid perk — unlimited units, and a report without the watermark.
 *
 * Only a **confirmed** [UnlockState.Owned] does. [UnlockState.Unknown] is not owned: while billing has
 * not answered, the free tier's own rules apply, because handing the perk out on an unresolved
 * entitlement is the one direction a later `restore()` cannot take back. This is also the single place
 * that decides which states count as paid, so the units cap and the report watermark cannot drift.
 */
val UnlockState.isOwned: Boolean get() = this == UnlockState.Owned
