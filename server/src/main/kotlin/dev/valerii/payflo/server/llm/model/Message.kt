package dev.valerii.payflo.server.llm.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(val role: String, val content: List<MessageContent>)
