package dev.valerii.payflo.server.llm

class LLMRequestFailed(message: String, cause: Throwable? = null) : Exception(message, cause)