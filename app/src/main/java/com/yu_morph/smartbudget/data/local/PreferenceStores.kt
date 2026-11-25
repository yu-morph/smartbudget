package com.yu_morph.smartbudget.data.local

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.OffsetDateTime

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

interface UserIdentityStore {
    fun readUsername(): String?

    fun writeUsername(username: String)

    fun clear()
}

interface ProfileImageStore {
    fun readUri(): String?

    fun writeBase64(encodedImage: String?): String?

    fun encodeUri(uri: String): String?

    fun clear()
}

class FileProfileImageStore(private val context: Context) : ProfileImageStore {
    private val imageFile = File(context.filesDir, PROFILE_IMAGE_NAME)

    override fun readUri(): String? = imageFile.takeIf(File::isFile)?.versionedUri()

    override fun writeBase64(encodedImage: String?): String? = when {
        encodedImage.isNullOrBlank() -> {
            clear()
            null
        }

        else -> {
            val bytes = runCatching { Base64.decode(encodedImage, Base64.DEFAULT) }.getOrNull()
            if (bytes == null || bytes.isEmpty() || bytes.size > MAX_PROFILE_IMAGE_BYTES) {
                readUri()
            } else {
                storeImage(bytes)
            }
        }
    }

    private fun storeImage(bytes: ByteArray): String? {
        val temporaryFile = File(context.filesDir, "$PROFILE_IMAGE_NAME.tmp")
        return runCatching {
            temporaryFile.writeBytes(bytes)
            if (!temporaryFile.renameTo(imageFile)) {
                temporaryFile.copyTo(imageFile, overwrite = true)
                temporaryFile.delete()
            }
            imageFile.versionedUri()
        }.getOrElse {
            temporaryFile.delete()
            readUri()
        }
    }

    override fun encodeUri(uri: String): String? = runCatching {
        val bytes = context.contentResolver.openInputStream(Uri.parse(uri))?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > MAX_PROFILE_IMAGE_BYTES) return null
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: return null
        if (bytes.isEmpty() || bytes.size > MAX_PROFILE_IMAGE_BYTES) return null
        Base64.encodeToString(bytes, Base64.NO_WRAP)
    }.getOrNull()

    override fun clear() {
        imageFile.delete()
        File(context.filesDir, "$PROFILE_IMAGE_NAME.tmp").delete()
    }

    private fun File.versionedUri(): String =
        Uri.fromFile(this).buildUpon().fragment(lastModified().toString()).build().toString()

    private companion object {
        const val PROFILE_IMAGE_NAME = "profile_image.png"
        const val MAX_PROFILE_IMAGE_BYTES = 5 * 1024 * 1024
    }
}

class SharedPreferencesUserIdentityStore(private val preferences: SharedPreferences) : UserIdentityStore {
    override fun readUsername(): String? = preferences.getString(KEY_USERNAME, null)

    override fun writeUsername(username: String) {
        preferences.edit().putString(KEY_USERNAME, username).apply()
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val KEY_USERNAME = "username"
    }
}

interface AuthTokenStore {
    fun read(): StoredAuthToken?

    fun write(token: StoredAuthToken)

    fun clear()
}

enum class DisplayMode { LIGHT, DARK, SYSTEM }

data class AppSettings(
    val languageTag: String = "ko",
    val budgetApproachingNotification: Boolean = true,
    val budgetExceededNotification: Boolean = true,
    val analysisNotification: Boolean = true,
    val displayMode: DisplayMode = DisplayMode.SYSTEM,
)

interface AppSettingsStore {
    fun read(): AppSettings

    fun write(settings: AppSettings)
}

class SharedPreferencesAppSettingsStore(private val preferences: SharedPreferences) : AppSettingsStore {
    override fun read(): AppSettings = AppSettings(
        languageTag = preferences.getString(KEY_LANGUAGE, "ko").orEmpty().ifBlank { "ko" },
        budgetApproachingNotification = preferences.getBoolean(KEY_BUDGET_APPROACHING, true),
        budgetExceededNotification = preferences.getBoolean(KEY_BUDGET_EXCEEDED, true),
        analysisNotification = preferences.getBoolean(KEY_ANALYSIS, true),
        displayMode =
            runCatching {
                DisplayMode.valueOf(preferences.getString(KEY_DISPLAY_MODE, DisplayMode.SYSTEM.name).orEmpty())
            }.getOrDefault(DisplayMode.SYSTEM),
    )

    override fun write(settings: AppSettings) {
        preferences
            .edit()
            .putString(KEY_LANGUAGE, settings.languageTag)
            .putBoolean(KEY_BUDGET_APPROACHING, settings.budgetApproachingNotification)
            .putBoolean(KEY_BUDGET_EXCEEDED, settings.budgetExceededNotification)
            .putBoolean(KEY_ANALYSIS, settings.analysisNotification)
            .putString(KEY_DISPLAY_MODE, settings.displayMode.name)
            .apply()
    }

    private companion object {
        const val KEY_LANGUAGE = "language"
        const val KEY_BUDGET_APPROACHING = "budget_approaching_notification"
        const val KEY_BUDGET_EXCEEDED = "budget_exceeded_notification"
        const val KEY_ANALYSIS = "analysis_notification"
        const val KEY_DISPLAY_MODE = "display_mode"
    }
}

data class StoredAuthToken(val accessToken: String, val expiresAt: String, val tokenType: String)

fun StoredAuthToken.isValidAt(now: OffsetDateTime = OffsetDateTime.now()): Boolean {
    val expiration = runCatching { OffsetDateTime.parse(expiresAt) }.getOrNull() ?: return false
    return accessToken.isNotBlank() && tokenType.equals("Bearer", ignoreCase = true) && expiration.isAfter(now)
}

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
    private const val USER_IDENTITY_FILE = "smartbudget_user_identity"
    private const val SECURE_AUTH_FILE = "smartbudget_secure_auth"
    private const val SECURE_ANALYSIS_SURVEY_FILE = "smartbudget_secure_analysis_survey"
    private const val APP_SETTINGS_FILE = "smartbudget_app_settings"

    fun responseCache(context: Context): ApiResponseCache = SharedPreferencesResponseCache(
        context.getSharedPreferences(RESPONSE_CACHE_FILE, Context.MODE_PRIVATE),
    )

    fun remoteResources(context: Context): RemoteResourceStore = SharedPreferencesRemoteResourceStore(
        context.getSharedPreferences(REMOTE_RESOURCE_FILE, Context.MODE_PRIVATE),
    )

    fun userIdentity(context: Context): UserIdentityStore = SharedPreferencesUserIdentityStore(
        context.getSharedPreferences(USER_IDENTITY_FILE, Context.MODE_PRIVATE),
    )

    fun profileImage(context: Context): ProfileImageStore = FileProfileImageStore(context.applicationContext)

    fun appSettings(context: Context): AppSettingsStore = SharedPreferencesAppSettingsStore(
        context.getSharedPreferences(APP_SETTINGS_FILE, Context.MODE_PRIVATE),
    )

    fun analysisSurveys(context: Context): AnalysisSurveyStore = EncryptedPreferencesAnalysisSurveyStore(
        encryptedPreferences(context, SECURE_ANALYSIS_SURVEY_FILE),
    )

    @Suppress("DEPRECATION")
    fun authTokens(context: Context): AuthTokenStore = EncryptedPreferencesAuthTokenStore(
        encryptedPreferences(context, SECURE_AUTH_FILE),
    )

    @Suppress("DEPRECATION")
    private fun encryptedPreferences(context: Context, fileName: String): SharedPreferences {
        val masterKey =
            MasterKey
                .Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        val preferences =
            EncryptedSharedPreferences.create(
                context,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        return preferences
    }
}
