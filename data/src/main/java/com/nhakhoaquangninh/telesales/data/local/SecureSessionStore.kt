package com.nhakhoaquangninh.telesales.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.Base64
import com.nhakhoaquangninh.telesales.domain.model.CareTypeOption
import com.nhakhoaquangninh.telesales.domain.model.UserSession
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import androidx.core.content.edit

internal interface SessionCipher {
    fun encrypt(plainText: ByteArray): ByteArray
    fun decrypt(cipherText: ByteArray): ByteArray
}

internal class AndroidKeystoreSessionCipher : SessionCipher {
    override fun encrypt(plainText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(AAD)
        val encrypted = cipher.doFinal(plainText)
        return ByteBuffer.allocate(1 + cipher.iv.size + encrypted.size)
            .put(cipher.iv.size.toByte())
            .put(cipher.iv)
            .put(encrypted)
            .array()
    }

    override fun decrypt(cipherText: ByteArray): ByteArray {
        require(cipherText.isNotEmpty())
        val buffer = ByteBuffer.wrap(cipherText)
        val ivSize = buffer.get().toInt() and 0xFF
        require(ivSize in 12..16 && buffer.remaining() > ivSize)
        val iv = ByteArray(ivSize).also(buffer::get)
        val encrypted = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        cipher.updateAAD(AAD)
        return cipher.doFinal(encrypted)
    }

    fun deleteKey() {
        runCatching {
            KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }.deleteEntry(KEY_ALIAS)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "telesales_session_aes_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        val AAD = "com.nhakhoaquangninh.telesales.session.v1".encodeToByteArray()
    }
}

internal class SecureSessionStore(
    context: Context,
    private val cipher: SessionCipher
) {
    private val securePrefs = context.getSharedPreferences(SECURE_PREF_NAME, Context.MODE_PRIVATE)
    private val legacyPrefs = context.getSharedPreferences(LEGACY_PREF_NAME, Context.MODE_PRIVATE)

    fun save(session: UserSession) {
        try {
            persistEncrypted(session)
            legacyPrefs.edit(commit = true) { clear() }
        } catch (error: Exception) {
            if (error.isUnrecoverableKeyFailure()) {
                clearAfterUnrecoverableKeyFailure()
            }
            throw IllegalStateException("Không thể bảo vệ phiên đăng nhập", error)
        }
    }

    fun read(): UserSession? {
        val encrypted = securePrefs.getString(KEY_ENCRYPTED_SESSION, null)
        if (encrypted != null) {
            try {
                val plainText = cipher.decrypt(Base64.decode(encrypted, Base64.NO_WRAP))
                return JSONObject(plainText.decodeToString()).toSession()
            } catch (error: Exception) {
                if (error.isUnrecoverableKeyFailure()) {
                    clearAfterUnrecoverableKeyFailure()
                    return null
                }
                securePrefs.edit(commit = true) { clear() }
            }
        }

        val legacy = readLegacy() ?: return null
        return try {
            persistEncrypted(legacy)
            legacyPrefs.edit(commit = true) { clear() }
            legacy
        } catch (error: Exception) {
            if (error.isUnrecoverableKeyFailure()) {
                clearAfterUnrecoverableKeyFailure()
                null
            } else {
                legacy
            }
        }
    }

    private fun persistEncrypted(session: UserSession) {
        val encrypted = cipher.encrypt(session.toJson().toString().encodeToByteArray())
        val encoded = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        check(securePrefs.edit().putString(KEY_ENCRYPTED_SESSION, encoded).commit())
    }
    fun clear() {
        securePrefs.edit(commit = true) { clear() }
        legacyPrefs.edit(commit = true) { clear() }
    }

    private fun readLegacy(): UserSession? {
        val token = legacyPrefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return null
        val userId = legacyPrefs.getInt(KEY_USER_ID, -1).takeIf { it > 0 } ?: return null
        return UserSession(
            userId = userId,
            token = token,
            userName = legacyPrefs.getString(KEY_USER_NAME, null),
            email = legacyPrefs.getString(KEY_USER_EMAIL, null),
            phoneNumber = legacyPrefs.getString(KEY_USER_PHONE, null)
        )
    }

    private fun clearAfterUnrecoverableKeyFailure() {
        clear()
        (cipher as? AndroidKeystoreSessionCipher)?.deleteKey()
    }

    private fun Throwable.isUnrecoverableKeyFailure(): Boolean =
        generateSequence(this) { it.cause }.any {
            it is KeyPermanentlyInvalidatedException || it is UnrecoverableKeyException
        }

    private fun UserSession.toJson() = JSONObject().apply {
        put("userId", userId)
        put("token", token)
        putNullable("userName", userName)
        putNullable("email", email)
        putNullable("phoneNumber", phoneNumber)
        if (careTypeOptions.isNotEmpty()) {
            val careArray = JSONArray()
            careTypeOptions.forEach { opt ->
                careArray.put(JSONObject().apply {
                    put("value", opt.value)
                    put("label", opt.label)
                })
            }
            put("careTypeOptions", careArray)
        }
    }

    private fun JSONObject.toSession(): UserSession {
        val userId = getInt("userId")
        val token = getString("token")
        require(userId > 0 && token.isNotBlank())
        val careOptions = mutableListOf<CareTypeOption>()
        val careArray = optJSONArray("careTypeOptions")
        if (careArray != null) {
            for (i in 0 until careArray.length()) {
                val item = careArray.optJSONObject(i)
                if (item != null) {
                    val value = item.optInt("value")
                    val label = item.optString("label")
                    if (label.isNotBlank()) {
                        careOptions.add(CareTypeOption(value = value, label = label))
                    }
                }
            }
        }
        return UserSession(
            userId = userId,
            token = token,
            userName = nullableString("userName"),
            email = nullableString("email"),
            phoneNumber = nullableString("phoneNumber"),
            careTypeOptions = careOptions
        )
    }

    private fun JSONObject.putNullable(key: String, value: String?) {
        put(key, value ?: JSONObject.NULL)
    }

    private fun JSONObject.nullableString(key: String): String? =
        takeUnless { isNull(key) }?.optString(key)?.takeIf { it.isNotBlank() }

    fun saveSelectedCareTypeValue(value: Int) {
        securePrefs.edit { putInt(KEY_SELECTED_CARE_TYPE, value) }
    }

    fun getSelectedCareTypeValue(): Int? {
        return if (securePrefs.contains(KEY_SELECTED_CARE_TYPE)) {
            securePrefs.getInt(KEY_SELECTED_CARE_TYPE, -1).takeIf { it >= 0 }
        } else {
            null
        }
    }

    private companion object {
        const val SECURE_PREF_NAME = "TelesalesSecureSession"
        const val LEGACY_PREF_NAME = "TelesalesSession"
        const val KEY_ENCRYPTED_SESSION = "encrypted_session"
        const val KEY_TOKEN = "bearer_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_USER_PHONE = "user_phone"
        const val KEY_SELECTED_CARE_TYPE = "selected_care_type_value"
    }
}