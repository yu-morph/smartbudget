package com.yu_morph.smartbudget.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VersionData(
    val version: String,
    val channel: String,
    @SerialName("build_timestamp") val buildTimestamp: String,
)

@Serializable
data class MessageData(val message: String? = null)
