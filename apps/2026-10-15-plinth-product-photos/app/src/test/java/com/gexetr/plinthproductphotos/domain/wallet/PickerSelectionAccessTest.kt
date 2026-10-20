package com.gexetr.plinthproductphotos.domain.wallet

import org.junit.Assert.assertEquals
import org.junit.Test

class PickerSelectionAccessTest {
    @Test
    fun pickerAndClampUseTheSameResolvedEntitlementTier() {
        val selectedItems = (1..51).toList()

        assertEquals(5, WalletAccessPolicy.pickerSelectionLimit(serverConfirmedUnlock = false))
        assertEquals((1..5).toList(), WalletAccessPolicy.clampSelection(selectedItems, serverConfirmedUnlock = false))

        assertEquals(50, WalletAccessPolicy.pickerSelectionLimit(serverConfirmedUnlock = true))
        assertEquals((1..50).toList(), WalletAccessPolicy.clampSelection(selectedItems, serverConfirmedUnlock = true))
    }
}
