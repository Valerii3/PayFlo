package dev.valerii.payflo.model

import kotlinx.serialization.Serializable

@Serializable
data class OrderAnalysisResponse(
    val matchedItemIds: List<String>
)
