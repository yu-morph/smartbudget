package com.yu_morph.smartbudget.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignUpRequest(
    val username: String,
    val password: String,
    @SerialName("display_name") val displayName: String,
)

@Serializable
data class SignInRequest(val username: String, val password: String)

@Serializable
data class RefreshRequest(val token: String)

@Serializable
data class AuthTokenData(
    val token: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("token_type") val tokenType: String,
)

@Serializable
data class AccountData(@SerialName("display_name") val displayName: String)

@Serializable
data class UpdateAccountRequest(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("current_password") val currentPassword: String? = null,
    val password: String? = null,
)
