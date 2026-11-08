package com.yu_morph.smartbudget.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OcrItemData(val name: String? = null, val amount: Int? = null)

@Serializable
data class OcrResultData(
    @SerialName("file_name") val fileName: String? = null,
    val status: String? = null,
    @SerialName("error_message") val errorMessage: String? = null,
    val items: List<OcrItemData> = emptyList(),
    @SerialName("file_index") val fileIndex: Int? = null,
    @SerialName("store_name") val storeName: String? = null,
    val date: String? = null,
    val time: String? = null,
    val amount: Int? = null,
)
