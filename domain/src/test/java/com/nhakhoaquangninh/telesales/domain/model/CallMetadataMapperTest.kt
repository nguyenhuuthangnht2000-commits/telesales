package com.nhakhoaquangninh.telesales.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CallMetadataMapperTest {

    @Test
    fun normalizePhone_validFormats() {
        val call1 = CallMetadataMapper.create(
            1,
            1L,
            null,
            CallType.INCOMING,
            "+84912345678",
            "0912345678",
            0,
            null
        )
        assertThat(call1.phoneNumberFrom).isEqualTo("0912345678")

        val call2 = CallMetadataMapper.create(
            1,
            1L,
            null,
            CallType.INCOMING,
            "84912345678",
            "0912345678",
            0,
            null
        )
        assertThat(call2.phoneNumberFrom).isEqualTo("0912345678")

        val call3 = CallMetadataMapper.create(
            1,
            1L,
            null,
            CallType.INCOMING,
            "0912345678",
            "0912345678",
            0,
            null
        )
        assertThat(call3.phoneNumberFrom).isEqualTo("0912345678")
    }

    @Test
    fun normalizePhone_nullOrBlank() {
        val call1 = CallMetadataMapper.create(1, 1L, null, CallType.INCOMING, null, null, 0, null)
        assertThat(call1.phoneNumberFrom).isNull()
        assertThat(call1.phoneNumberTo).isNull()

        val call2 = CallMetadataMapper.create(1, 1L, null, CallType.INCOMING, "   ", "", 0, null)
        assertThat(call2.phoneNumberFrom).isNull()
        assertThat(call2.phoneNumberTo).isNull()
    }

    @Test
    fun incomingCall() {
        val call = CallMetadataMapper.create(
            1,
            1L,
            null,
            CallType.INCOMING,
            "0912345678",
            "0987654321",
            0,
            null
        )
        assertThat(call.phoneNumberFrom).isEqualTo("0912345678") // other
        assertThat(call.phoneNumberTo).isEqualTo("0987654321") // own
    }

    @Test
    fun outgoingCall() {
        val call = CallMetadataMapper.create(
            1,
            1L,
            null,
            CallType.OUTGOING,
            "0912345678",
            "0987654321",
            0,
            null
        )
        assertThat(call.phoneNumberFrom).isEqualTo("0987654321") // own
        assertThat(call.phoneNumberTo).isEqualTo("0912345678") // other
    }

    @Test
    fun metadataFields_areSet() {
        val call = CallMetadataMapper.create(
            1,
            1000L,
            "uri",
            CallType.OUTGOING,
            "0912345678",
            "0987654321",
            10,
            null
        )
        assertThat(call.callId).isNotEmpty()
        assertThat(call.ownerUserId).isEqualTo(1)
        assertThat(call.startedAtMillis).isEqualTo(1000L)
    }
}
