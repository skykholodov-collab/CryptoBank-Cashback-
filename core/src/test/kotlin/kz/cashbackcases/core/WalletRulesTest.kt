package kz.cashbackcases.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WalletRulesTest {
    @Test
    fun acceptsPresetAmountsInsideLimits() {
        WalletRules.presetsKzt.forEach { amount ->
            assertNull(WalletRules.topupError(amount))
        }
        assertNull(WalletRules.topupError(WalletRules.MIN_TOPUP_KZT))
        assertNull(WalletRules.topupError(WalletRules.MAX_TOPUP_KZT))
    }

    @Test
    fun rejectsOutOfRange() {
        assertNotNull(WalletRules.topupError(WalletRules.MIN_TOPUP_KZT - 1))
        assertNotNull(WalletRules.topupError(WalletRules.MAX_TOPUP_KZT + 1))
    }

    @Test
    fun presetsStayInsideDemoPurchaseBandStyleLimits() {
        assertEquals(1_000L, WalletRules.MIN_TOPUP_KZT)
        assertEquals(100_000L, WalletRules.MAX_TOPUP_KZT)
    }
}
