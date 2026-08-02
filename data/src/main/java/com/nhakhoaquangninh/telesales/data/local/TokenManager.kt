package com.nhakhoaquangninh.telesales.data.local

import android.content.Context
import android.content.SharedPreferences
import com.nhakhoaquangninh.telesales.domain.model.UserSession
import androidx.core.content.edit

class TokenManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "TelesalesSession"
        private const val KEY_TOKEN = "bearer_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"

        @Volatile
        private var INSTANCE: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun saveSession(session: UserSession) {
        prefs.edit().apply {
            putString(KEY_TOKEN, session.token)
            putInt(KEY_USER_ID, session.userId)
            putString(KEY_USER_NAME, session.userName)
            putString(KEY_USER_EMAIL, session.email)
            apply()
        }
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)

    fun getSession(): UserSession? {
        val token = getToken() ?: return null
        val userId = getUserId()
        if (userId <= 0) return null
        return UserSession(
            userId = userId,
            token = token,
            userName = prefs.getString(KEY_USER_NAME, null),
            email = prefs.getString(KEY_USER_EMAIL, null)
        )
    }

    fun isLoggedIn(): Boolean {
        val token = getToken()
        val userId = getUserId()
        return !token.isNullOrEmpty() && userId > 0
    }

    fun clearSession() {
        prefs.edit { clear() }
    }
}
