package com.yu_morph.smartbudget.data.local

import android.content.SharedPreferences
import java.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class StoredAnalysisSurvey(
    val gender: String = "",
    val birthDate: String = "",
    val job: String = "",
    val workplace: String = "",
    val education: String = "",
    val hasLoan: Boolean = false,
    val loanDetails: String = "",
    val managementDirection: String = "save",
    val interest: String = "",
)

interface AnalysisSurveyStore {
    fun read(username: String): StoredAnalysisSurvey?

    fun write(username: String, survey: StoredAnalysisSurvey)

    fun remove(username: String)
}

class EncryptedPreferencesAnalysisSurveyStore(
    private val preferences: SharedPreferences,
    private val json: Json = analysisSurveyJson(),
) : AnalysisSurveyStore {
    override fun read(username: String): StoredAnalysisSurvey? {
        val rawValue = preferences.getString(analysisSurveyKey(username), null) ?: return null
        return decodeAnalysisSurvey(rawValue, json)
    }

    override fun write(username: String, survey: StoredAnalysisSurvey) {
        if (username.isBlank()) return
        preferences.edit().putString(analysisSurveyKey(username), json.encodeToString(survey)).apply()
    }

    override fun remove(username: String) {
        if (username.isBlank()) return
        preferences.edit().remove(analysisSurveyKey(username)).apply()
    }
}

internal fun analysisSurveyKey(username: String): String = Base64
    .getUrlEncoder()
    .withoutPadding()
    .encodeToString(username.toByteArray(Charsets.UTF_8))

internal fun analysisSurveyJson(): Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

internal fun decodeAnalysisSurvey(rawValue: String, json: Json = analysisSurveyJson()): StoredAnalysisSurvey? = try {
    json.decodeFromString<StoredAnalysisSurvey>(rawValue)
} catch (_: SerializationException) {
    null
} catch (_: IllegalArgumentException) {
    null
}
