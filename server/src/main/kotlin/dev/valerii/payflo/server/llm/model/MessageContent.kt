package dev.valerii.payflo.server.llm.model

import kotlinx.serialization.Serializable

@Serializable
data class MessageContent(val type: String, val text: String? = null, val image_url: Map<String, String>? = null)
