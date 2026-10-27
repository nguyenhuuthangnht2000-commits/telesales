package com.nhakhoaquangninh.telesales

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OwnPhoneNumberResolverTest {
    @Test fun simTakesPriorityWithoutReadingUser() {
        val result = OwnPhoneNumberResolver.selectPhoneNumber(
            simNumber = { " 0378404007 " },
            userNumber = { error("Không được đọc số tài khoản khi đã có số SIM") }
        )
        assertEquals("0378404007", result)
    }

    @Test fun missingSimFallsBackToUser() {
        assertEquals("0336846553", OwnPhoneNumberResolver.selectPhoneNumber(
            simNumber = { null }, userNumber = { " 0336846553 " }))
    }

    @Test fun blankSimFallsBackToUser() {
        assertEquals("0336846553", OwnPhoneNumberResolver.selectPhoneNumber(
            simNumber = { "   " }, userNumber = { "0336846553" }))
    }

    @Test fun bothUnavailableReturnsNull() {
        assertNull(OwnPhoneNumberResolver.selectPhoneNumber(
            simNumber = { null }, userNumber = { " " }))
    }

    @Test fun dualSimMatchesSim2BySubscriptionId() {
        val sims = listOf(
            SimSubscriptionCandidate(slotIndex = 0, subscriptionId = 1, iccId = "898401", phoneNumber = "0368133444"),
            SimSubscriptionCandidate(slotIndex = 1, subscriptionId = 2, iccId = "898402", phoneNumber = "0352141182")
        )
        val selection = OwnPhoneNumberResolver.resolveSelection(
            sims = sims,
            phoneAccountId = "2",
            viaNumber = null,
            userNumber = { "0368133444" }
        )
        assertEquals("0352141182", selection.phoneNumber)
        assertEquals("call_sim", selection.source)
        assertEquals(2, selection.matchedSim?.subscriptionId)
    }

    @Test fun dualSimMatchesSim2ByIccIdWithTrailingF() {
        val sims = listOf(
            SimSubscriptionCandidate(slotIndex = 0, subscriptionId = 1, iccId = "898404111122223333", phoneNumber = "0368133444"),
            SimSubscriptionCandidate(slotIndex = 1, subscriptionId = 2, iccId = "898404999988887777", phoneNumber = "0352141182")
        )
        val selection = OwnPhoneNumberResolver.resolveSelection(
            sims = sims,
            phoneAccountId = "898404999988887777F",
            viaNumber = null,
            userNumber = { "0368133444" }
        )
        assertEquals("0352141182", selection.phoneNumber)
        assertEquals("call_sim", selection.source)
        assertEquals(1, selection.matchedSim?.slotIndex)
    }

    @Test fun dualSimMatchesSim2ByTelecomAccountHandleWhenIccIdHidden() {
        val sims = listOf(
            SimSubscriptionCandidate(
                slotIndex = 0,
                subscriptionId = 1,
                iccId = null,
                phoneNumber = "0368133444",
                accountHandleIds = setOf("898404111122223333F")
            ),
            SimSubscriptionCandidate(
                slotIndex = 1,
                subscriptionId = 2,
                iccId = null,
                phoneNumber = "0352141182",
                accountHandleIds = setOf("898404999988887777F")
            )
        )
        val selection = OwnPhoneNumberResolver.resolveSelection(
            sims = sims,
            phoneAccountId = "898404999988887777",
            viaNumber = null,
            userNumber = { "0368133444" }
        )
        assertEquals("0352141182", selection.phoneNumber)
        assertEquals("call_sim", selection.source)
        assertEquals(2, selection.matchedSim?.subscriptionId)
    }

    @Test fun dualSimWithoutAccountIdPrefersCompanySimOverPersonalAccountSim() {
        val sims = listOf(
            SimSubscriptionCandidate(slotIndex = 0, subscriptionId = 1, phoneNumber = "+84368133444"),
            SimSubscriptionCandidate(slotIndex = 1, subscriptionId = 2, phoneNumber = "0352141182")
        )
        val selection = OwnPhoneNumberResolver.resolveSelection(
            sims = sims,
            phoneAccountId = null,
            viaNumber = null,
            userNumber = { "0368133444" }
        )
        assertEquals("0352141182", selection.phoneNumber)
        assertEquals("non_account_sim", selection.source)
    }

    @Test fun viaNumberUsedWhenMatchedSimHasNoStoredNumber() {
        val sims = listOf(
            SimSubscriptionCandidate(slotIndex = 0, subscriptionId = 1, phoneNumber = "0368133444"),
            SimSubscriptionCandidate(slotIndex = 1, subscriptionId = 2, phoneNumber = null)
        )
        val selection = OwnPhoneNumberResolver.resolveSelection(
            sims = sims,
            phoneAccountId = "2",
            viaNumber = "0352141182",
            userNumber = { "0368133444" }
        )
        assertEquals("0352141182", selection.phoneNumber)
        assertEquals("call_via_number", selection.source)
        assertEquals(2, selection.matchedSim?.subscriptionId)
    }
}
