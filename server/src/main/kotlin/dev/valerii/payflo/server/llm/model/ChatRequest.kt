package dev.valerii.payflo.server.llm.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(val model: String, val messages: List<Message>, val max_tokens: Int)

