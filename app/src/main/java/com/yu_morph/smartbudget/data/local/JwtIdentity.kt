package com.yu_morph.smartbudget.data.local

import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal fun readSubjectFromJwt(token: String): String? = runCatching {
    val segments = token.split('.')
    require(segments.size == JWT_SEGMENT_COUNT)
    val payload = String(Base64.getUrlDecoder().decode(segments[1]), StandardCharsets.UTF_8)
    Json.parseToJsonElement(payload).jsonObject[SUBJECT_CLAIM]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
}.getOrNull()

private const val JWT_SEGMENT_COUNT = 3
private const val SUBJECT_CLAIM = "sub"
