package com.yu_morph.smartbudget.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
enum class TransactionType {
    @SerialName("expense")
    EXPENSE,

    @SerialName("income")
    INCOME,
}

@Serializable
enum class TransactionSource {
    @SerialName("financial_sync")
    FINANCIAL_SYNC,

    @SerialName("manual")
    MANUAL,

    @SerialName("ocr")
    OCR,
}

@Serializable
data class TransactionData(
    val id: Int,
    val date: String,
    val type: TransactionType,
    val source: TransactionSource,
    val category: String,
    @SerialName("store_name") val storeName: String? = null,
    val note: String? = null,
    val amount: Int,
    val time: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class TransactionPageData(
    val items: List<TransactionData>,
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable
data class CreateTransactionRequest(
    val date: String,
    val type: TransactionType,
    val source: TransactionSource,
    val category: String,
    @SerialName("store_name") val storeName: String? = null,
    val note: String? = null,
    val amount: Int,
    val time: String? = null,
)

sealed interface PatchValue<out T> {
    data object Omitted : PatchValue<Nothing>
    data class Present<T>(val value: T?) : PatchValue<T>
}

data class UpdateTransactionRequest(
    val date: String? = null,
    val type: TransactionType? = null,
    val category: String? = null,
    val storeName: PatchValue<String> = PatchValue.Omitted,
    val note: PatchValue<String> = PatchValue.Omitted,
    val amount: Int? = null,
    val time: PatchValue<String> = PatchValue.Omitted,
) {
    fun toJson(json: Json): JsonObject = buildMap {
        date?.let { put("date", JsonPrimitive(it)) }
        type?.let { put("type", json.parseToJsonElement(json.encodeToString(it))) }
        category?.let { put("category", JsonPrimitive(it)) }
        putPatch("store_name", storeName)
        putPatch("note", note)
        amount?.let { put("amount", JsonPrimitive(it)) }
        putPatch("time", time)
    }.let(::JsonObject)

    private fun MutableMap<String, JsonElement>.putPatch(key: String, field: PatchValue<String>) {
        if (field is PatchValue.Present) put(key, field.value?.let(::JsonPrimitive) ?: JsonNull)
    }
}
