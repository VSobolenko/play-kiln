package com.gexetr.teachergradebook.monetization

import com.gexetr.capability.billing.BillingCatalog
import com.gexetr.capability.billing.ProductId

/**
 * The one thing this app sells.
 *
 * A single non-consumable unlock, and no subscription at all. That is a deliberate reading of the
 * measured shelf: the 500K-install incumbent's most emphatic critical review is a paying user
 * asking for "one time purchase" and being told by the developer that "one time lifetime payment
 * for apps is reaching the end of life". The wedge is the opposite of that answer.
 *
 * The product must exist in Play Console with exactly this ID before review, because a purchase
 * flow that references an unknown product fails in the reviewer's session.
 */
object MonetizationConfig {

    val PRO_UNLOCK = ProductId("recordbook_pro")

    /** A catalog of one permanent entitlement: no consumables, so no server confirmation path. */
    val catalog: BillingCatalog = BillingCatalog(inAppProducts = listOf(PRO_UNLOCK))
}
