package dev.valerii.payflo.model

import kotlinx.serialization.Serializable

@Serializable
data class OrderAnalysisRequest(
    val orderDescription: String,
    val billItems: List<BillItem>
)
