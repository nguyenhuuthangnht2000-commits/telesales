package com.nhakhoaquangninh.telesales.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CallIdentityTest {
    @Test
    fun sameInputs_sameId() {
        val id1 = CallIdentity.create(1, 1000L, CallType.INCOMING, "0912345678")
        val id2 = CallIdentity.create(1, 1000L, CallType.INCOMING, "0912345678")
        assertThat(id1).isEqualTo(id2)
    }

    @Test
    fun differentInputs_differentId() {
        val id1 = CallIdentity.create(1, 1000L, CallType.INCOMING, "0912345678")
        val id2 = CallIdentity.create(2, 1000L, CallType.INCOMING, "0912345678")
        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun noPiiInOutput() {
        val phone = "0912345678"
        val id = CallIdentity.create(1, 1000L, CallType.INCOMING, phone)
        assertThat(id).doesNotContain(phone)
    }

    @Test
    fun lengthIs16() {
        val id = CallIdentity.create(1, 1000L, CallType.INCOMING, "0912345678")
        assertThat(id.length).isEqualTo(16)
    }
}
