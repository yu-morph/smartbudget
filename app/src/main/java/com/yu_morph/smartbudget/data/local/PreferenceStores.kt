package com.yu_morph.smartbudget.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface ApiResponseCache {
    fun read(key: String): String?

    fun write(key: String, value: String)

    fun remove(key: String)

    fun clear()
}

class SharedPreferencesResponseCache(private val preferences: SharedPreferences) : ApiResponseCache {
    override fun read(key: String): String? = preferences.getString(key, null)

    override fun write(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }
}

interface RemoteResourceStore {
    fun read(key: String): String?

    fun write(key: String, value: String)

    fun remove(key: String)
}

class SharedPreferencesRemoteResourceStore(private val preferences: SharedPreferences) : RemoteResourceStore {
    override fun read(key: String): String? = preferences.getString(key, null)

    override fun write(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }
}

interface AuthTokenStore {
    fun read(): StoredAuthToken?

    fun write(token: StoredAuthToken)

    fun clear()
}

data class StoredAuthToken(val accessToken: String, val expiresAt: String, val tokenType: String)

class EncryptedPreferencesAuthTokenStore(private val preferences: SharedPreferences) : AuthTokenStore {
    override fun read(): StoredAuthToken? {
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null)
        val expiresAt = preferences.getString(KEY_EXPIRES_AT, null)
        val tokenType = preferences.getString(KEY_TOKEN_TYPE, null)
        return if (accessToken != null && expiresAt != null && tokenType != null) {
            StoredAuthToken(accessToken, expiresAt, tokenType)
        } else {
            null
        }
    }

    override fun write(token: StoredAuthToken) {
        preferences
            .edit()
            .putString(KEY_ACCESS_TOKEN, token.accessToken)
            .putString(KEY_EXPIRES_AT, token.expiresAt)
            .putString(KEY_TOKEN_TYPE, token.tokenType)
            .apply()
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_TOKEN_TYPE = "token_type"
    }
}

object PreferenceStoreFactory {
    private const val RESPONSE_CACHE_FILE = "smartbudget_response_cache"
    private const val REMOTE_RESOURCE_FILE = "smartbudget_remote_resources"
    private const val SECURE_AUTH_FILE = "smartbudget_secure_auth"

    fun responseCache(context: Context): ApiResponseCache = SharedPreferencesResponseCache(
        context.getSharedPreferences(RESPONSE_CACHE_FILE, Context.MODE_PRIVATE),
    )

    fun remoteResources(context: Context): RemoteResourceStore = SharedPreferencesRemoteResourceStore(
        context.getSharedPreferences(REMOTE_RESOURCE_FILE, Context.MODE_PRIVATE),
    )

    @Suppress("DEPRECATION")
    fun authTokens(context: Context): AuthTokenStore {
        val masterKey =
            MasterKey
                .Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        val preferences =
            EncryptedSharedPreferences.create(
                context,
                SECURE_AUTH_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        return EncryptedPreferencesAuthTokenStore(preferences)
    }
}
