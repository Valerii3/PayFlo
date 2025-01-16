package dev.valerii.payflo.server.llm.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object JsonUtils {
    inline fun <reified T> parseResponse(responseText: String): T {
        val jsonResponse = Json.parseToJsonElement(responseText)
        val messageContent = jsonResponse
            .jsonObject["choices"]
            ?.jsonArray
            ?.get(0)
            ?.jsonObject
            ?.get("message")
            ?.jsonObject
            ?.get("content")
            ?.jsonPrimitive
            ?.content
            ?: throw IllegalArgumentException("Failed to extract message content")

        val cleanedJson = cleanJson(messageContent)
        return Json.decodeFromString(cleanedJson)
    }

    fun cleanJson(rawJson: String) = rawJson
            .removePrefix("```json\n")
            .removeSuffix("\n```")
            .trim()
}
