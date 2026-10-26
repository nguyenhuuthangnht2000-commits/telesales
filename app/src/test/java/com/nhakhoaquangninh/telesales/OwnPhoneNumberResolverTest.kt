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
}
