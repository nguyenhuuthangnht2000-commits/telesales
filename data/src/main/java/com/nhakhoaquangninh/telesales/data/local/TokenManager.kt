package com.nhakhoaquangninh.telesales.data.local

import android.content.Context
import com.nhakhoaquangninh.telesales.domain.model.UserSession

class TokenManager private constructor(private val secureStore: SecureSessionStore) {

    constructor(context: Context) : this(
        SecureSessionStore(context.applicationContext, AndroidKeystoreSessionCipher())
    )

    companion object {
        @Volatile
        private var INSTANCE: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        internal fun createForTest(context: Context, cipher: SessionCipher): TokenManager =
            TokenManager(SecureSessionStore(context.applicationContext, cipher))
    }

    fun saveSession(session: UserSession) = secureStore.save(session)

    fun getToken(): String? = getSession()?.token

    fun getUserId(): Int = getSession()?.userId ?: -1

    fun getSession(): UserSession? = secureStore.read()

    fun isLoggedIn(): Boolean = getSession() != null

    fun saveSelectedCareTypeValue(value: Int) = secureStore.saveSelectedCareTypeValue(value)

    fun getSelectedCareTypeValue(): Int? = secureStore.getSelectedCareTypeValue()

    fun clearSession() = secureStore.clear()
}