package com.nhakhoaquangninh.telesales.call

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CallSessionTrackerTest {

    private class FakeSharedPreferences : SharedPreferences {
        private val map = mutableMapOf<String, Any?>()

        override fun getAll(): Map<String, *> = map
        override fun getString(key: String, defValue: String?): String? = map[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = null
        override fun getInt(key: String, defValue: Int): Int = map[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long): Long = map[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = map[key] as? Float ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
        override fun contains(key: String): Boolean = map.containsKey(key)

        override fun edit(): SharedPreferences.Editor = FakeEditor()
        
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        inner class FakeEditor : SharedPreferences.Editor {
            private val changes = mutableMapOf<String, Any?>()
            private var clear = false

            override fun putString(key: String, value: String?): SharedPreferences.Editor = apply { changes[key] = value }
            override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor = this
            override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply { changes[key] = value }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply { changes[key] = value }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply { changes[key] = value }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply { changes[key] = value }
            override fun remove(key: String): SharedPreferences.Editor = apply { changes[key] = null }
            override fun clear(): SharedPreferences.Editor = apply { clear = true }

            override fun commit(): Boolean {
                if (clear) map.clear()
                for ((k, v) in changes) {
                    if (v == null) map.remove(k) else map[k] = v
                }
                return true
            }

            override fun apply() { commit() }
        }
    }

    @Test
    fun outgoingCall_lifecycle() {
        var currentTime = 1000L
        val tracker = CallSessionTracker(FakeSharedPreferences(), { 1 }, { "01234" }, { currentTime })
        
        val t1 = tracker.onState(PhoneCallState.OFFHOOK, "0987")
        assertThat(t1).isEqualTo(CallTransition.None)

        currentTime = 2000L
        val t2 = tracker.onState(PhoneCallState.IDLE, null)
        assertThat(t2).isInstanceOf(CallTransition.ConnectedEnded::class.java)
        
        val snapshot = (t2 as CallTransition.ConnectedEnded).snapshot
        assertThat(snapshot.answered).isTrue()
        assertThat(snapshot.incoming).isFalse()
        assertThat(snapshot.ownerUserId).isEqualTo(1)
        assertThat(snapshot.ownPhoneNumber).isEqualTo("01234")
    }

    @Test
    fun incomingCall_lifecycle() {
        var currentTime = 1000L
        val tracker = CallSessionTracker(FakeSharedPreferences(), { 1 }, { "01234" }, { currentTime })
        
        val t1 = tracker.onState(PhoneCallState.RINGING, "0987")
        assertThat(t1).isEqualTo(CallTransition.None)

        val t2 = tracker.onState(PhoneCallState.OFFHOOK, null)
        assertThat(t2).isEqualTo(CallTransition.None)

        currentTime = 2000L
        val t3 = tracker.onState(PhoneCallState.IDLE, null)
        assertThat(t3).isInstanceOf(CallTransition.ConnectedEnded::class.java)
        
        val snapshot = (t3 as CallTransition.ConnectedEnded).snapshot
        assertThat(snapshot.answered).isTrue()
        assertThat(snapshot.incoming).isTrue()
    }

    @Test
    fun missedIncoming_lifecycle() {
        var currentTime = 1000L
        val tracker = CallSessionTracker(FakeSharedPreferences(), { 1 }, { "01234" }, { currentTime })
        
        val t1 = tracker.onState(PhoneCallState.RINGING, "0987")
        assertThat(t1).isEqualTo(CallTransition.None)

        currentTime = 2000L
        val t2 = tracker.onState(PhoneCallState.IDLE, null)
        assertThat(t2).isInstanceOf(CallTransition.MissedIncomingEnded::class.java)
        
        val snapshot = (t2 as CallTransition.MissedIncomingEnded).snapshot
        assertThat(snapshot.answered).isFalse()
        assertThat(snapshot.incoming).isTrue()
    }
}
